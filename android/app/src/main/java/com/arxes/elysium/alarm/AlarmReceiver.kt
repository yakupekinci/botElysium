package com.arxes.elysium.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val label = intent.getStringExtra("label") ?: "Hatırlatma"
        val id = intent.getIntExtra("id", 1)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "elysium_alarm_channel"

        val channel = NotificationChannel(
            channelId,
            "Elysium Alarmları",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Elysium reminder alarms"
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)

        val notif = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("⏰ Elysium")
            .setContentText(label)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        manager.notify(id, notif)
    }
}
