package com.arxes.elysium

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.arxes.elysium.alarm.AlarmScheduler
import com.arxes.elysium.voice.CommandParser

class MainActivity : AppCompatActivity() {
    private val scheduler by lazy { AlarmScheduler(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestNotificationPermissionIfNeeded()

        val input = findViewById<EditText>(R.id.commandInput)
        val status = findViewById<TextView>(R.id.statusText)
        val run = findViewById<Button>(R.id.runButton)
        val sample = findViewById<Button>(R.id.sampleButton)

        run.setOnClickListener {
            val cmd = input.text.toString().trim()
            val parsed = CommandParser.parse(cmd)
            if (parsed == null) {
                status.text = "Komut anlaşılamadı. Örn: '20:30 su iç hatırlat'"
                return@setOnClickListener
            }
            scheduler.schedule(parsed)
            status.text = "Alarm kuruldu: ${parsed.label} @ ${parsed.hour.toString().padStart(2, '0')}:${parsed.minute.toString().padStart(2, '0')}"
        }

        sample.setOnClickListener {
            val parsed = CommandParser.parse("5 dk sonra su iç")
            if (parsed != null) {
                scheduler.schedule(parsed)
                status.text = "Demo alarm kuruldu: 5 dk sonra"
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
    }
}
