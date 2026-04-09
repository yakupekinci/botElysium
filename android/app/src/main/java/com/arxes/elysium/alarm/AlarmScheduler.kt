package com.arxes.elysium.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import org.json.JSONArray
import java.util.Calendar

class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "elysium_native_alarms"
        private const val KEY_PAYLOAD = "alarms_json"
        private const val KEY_OLD_KEYS = "alarm_key_set"
        private const val TAG = "AlarmScheduler"
    }

    private fun requestCodeForKey(alarmKey: String): Int = alarmKey.hashCode() and 0x1FFFFFFF

    private fun pendingIntentFor(alarmKey: String, label: String, create: Boolean): PendingIntent? {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_ALARM_KEY, alarmKey)
            putExtra(AlarmReceiver.EXTRA_LABEL, label)
        }
        val flags = if (create) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        }
        return PendingIntent.getBroadcast(context, requestCodeForKey(alarmKey), intent, flags)
    }

    fun cancelAlarm(alarmKey: String) {
        val pi = pendingIntentFor(alarmKey, "", false) ?: return
        alarmManager.cancel(pi)
        pi.cancel()
    }

    fun scheduleOne(alarmKey: String, hour: Int, minute: Int, label: String) {
        cancelAlarm(alarmKey)
        val pi = pendingIntentFor(alarmKey, label, true) ?: return

        val triggerAt = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) add(Calendar.DAY_OF_MONTH, 1)
        }.timeInMillis

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerAt, pi),
                    pi
                )
            } else {
                @Suppress("DEPRECATION")
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }
        } catch (se: SecurityException) {
            Log.e(TAG, "setAlarmClock denied", se)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAt,
                        pi
                    )
                } else {
                    @Suppress("DEPRECATION")
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                }
            } catch (e: Exception) {
                Log.e(TAG, "exact fallback failed", e)
                try {
                    @Suppress("DEPRECATION")
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                } catch (e2: Exception) {
                    Log.e(TAG, "inexact fallback failed", e2)
                }
            }
        }
    }

    /**
     * Full sync from WebView JSON: `[{ "id", "time", "label", "on" }, ...]`
     */
    fun syncFromWebJson(json: String) {
        val arr = try {
            JSONArray(json)
        } catch (e: Exception) {
            Log.e(TAG, "invalid json", e)
            return
        }

        val newKeys = mutableSetOf<String>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            if (!o.optBoolean("on", true)) continue
            val id = o.optString("id", "").trim()
            if (id.isEmpty()) continue
            val time = o.optString("time", "").trim()
            val label = o.optString("label", "Hatırlatma").ifBlank { "Hatırlatma" }
            val parts = time.split(":")
            if (parts.size != 2) continue
            val h = parts[0].trim().toIntOrNull() ?: continue
            val m = parts[1].trim().toIntOrNull() ?: continue
            if (h !in 0..23 || m !in 0..59) continue
            scheduleOne(id, h, m, label)
            newKeys.add(id)
        }

        val prev = prefs.getStringSet(KEY_OLD_KEYS, emptySet()) ?: emptySet()
        for (key in prev) {
            if (key !in newKeys) cancelAlarm(key)
        }

        prefs.edit()
            .putString(KEY_PAYLOAD, json)
            .putStringSet(KEY_OLD_KEYS, HashSet(newKeys))
            .apply()
    }

    fun rescheduleAfterBoot() {
        val json = prefs.getString(KEY_PAYLOAD, "[]") ?: "[]"
        val arr = try {
            JSONArray(json)
        } catch (e: Exception) {
            return
        }
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            if (!o.optBoolean("on", true)) continue
            val id = o.optString("id", "").trim()
            if (id.isEmpty()) continue
            val time = o.optString("time", "").trim()
            val label = o.optString("label", "Hatırlatma").ifBlank { "Hatırlatma" }
            val parts = time.split(":")
            if (parts.size != 2) continue
            val h = parts[0].trim().toIntOrNull() ?: continue
            val m = parts[1].trim().toIntOrNull() ?: continue
            if (h !in 0..23 || m !in 0..59) continue
            scheduleOne(id, h, m, label)
        }
    }

    /** After an alarm fires, schedule the next occurrence (same time, next day if needed). */
    fun rescheduleNextDay(alarmKey: String) {
        val json = prefs.getString(KEY_PAYLOAD, "[]") ?: return
        val arr = try {
            JSONArray(json)
        } catch (e: Exception) {
            return
        }
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            if (o.optString("id", "").trim() != alarmKey) continue
            if (!o.optBoolean("on", true)) return
            val time = o.optString("time", "").trim()
            val label = o.optString("label", "Hatırlatma").ifBlank { "Hatırlatma" }
            val parts = time.split(":")
            if (parts.size != 2) return
            val h = parts[0].trim().toIntOrNull() ?: return
            val m = parts[1].trim().toIntOrNull() ?: return
            if (h !in 0..23 || m !in 0..59) return
            scheduleOne(alarmKey, h, m, label)
            return
        }
    }
}
