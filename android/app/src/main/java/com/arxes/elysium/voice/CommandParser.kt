package com.arxes.elysium.voice

import com.arxes.elysium.alarm.AlarmSpec
import java.util.Calendar
import kotlin.math.max

object CommandParser {
    private val timeRegex = Regex("""(\d{1,2})[:.](\d{2})\s+(.+)""")
    private val minsLaterRegex = Regex("""(\d{1,3})\s*d(k|akika)?\s*sonra\s+(.+)""", RegexOption.IGNORE_CASE)

    fun parse(command: String): AlarmSpec? {
        if (command.isBlank()) return null

        val m1 = timeRegex.find(command)
        if (m1 != null) {
            val h = m1.groupValues[1].toIntOrNull() ?: return null
            val min = m1.groupValues[2].toIntOrNull() ?: return null
            val label = m1.groupValues[3].trim()
            if (h !in 0..23 || min !in 0..59 || label.isBlank()) return null
            return AlarmSpec(hour = h, minute = min, label = label)
        }

        val m2 = minsLaterRegex.find(command)
        if (m2 != null) {
            val mins = max(1, m2.groupValues[1].toIntOrNull() ?: return null)
            val label = m2.groupValues[3].trim()
            val cal = Calendar.getInstance().apply { add(Calendar.MINUTE, mins) }
            return AlarmSpec(hour = cal.get(Calendar.HOUR_OF_DAY), minute = cal.get(Calendar.MINUTE), label = label)
        }

        return null
    }
}
package com.arxes.elysium.voice

import com.arxes.elysium.alarm.AlarmSpec
import java.util.Calendar
import kotlin.math.max

object CommandParser {
    private val timeRegex = Regex("""(\d{1,2})[:.](\d{2})\s+(.+)""")
    private val minsLaterRegex = Regex("""(\d{1,3})\s*d(k|akika)?\s*sonra\s+(.+)""", RegexOption.IGNORE_CASE)

    fun parse(command: String): AlarmSpec? {
        if (command.isBlank()) return null

        val m1 = timeRegex.find(command)
        if (m1 != null) {
            val h = m1.groupValues[1].toIntOrNull() ?: return null
            val min = m1.groupValues[2].toIntOrNull() ?: return null
            val label = m1.groupValues[3].trim()
            if (h !in 0..23 || min !in 0..59 || label.isBlank()) return null
            return AlarmSpec(hour = h, minute = min, label = label)
        }

        val m2 = minsLaterRegex.find(command)
        if (m2 != null) {
            val mins = max(1, m2.groupValues[1].toIntOrNull() ?: return null)
            val label = m2.groupValues[3].trim()
            val cal = Calendar.getInstance().apply { add(Calendar.MINUTE, mins) }
            return AlarmSpec(hour = cal.get(Calendar.HOUR_OF_DAY), minute = cal.get(Calendar.MINUTE), label = label)
        }

        return null
    }
}
