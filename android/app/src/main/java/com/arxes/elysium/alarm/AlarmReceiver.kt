package com.arxes.elysium.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val label = intent.getStringExtra(EXTRA_LABEL) ?: "Hatırlatma"
        val key = intent.getStringExtra(EXTRA_ALARM_KEY) ?: "0"
        val id = key.hashCode() and 0x1FFFFFFF
        NotificationHelper.show(context, id, "⏰ Elysium", label)
        AlarmScheduler(context).rescheduleNextDay(key)
    }

    companion object {
        const val EXTRA_LABEL = "label"
        const val EXTRA_ALARM_KEY = "alarm_key"
    }
}
