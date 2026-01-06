package com.mindwarrior.app.engine

object SleepUtils {

    fun isNowInsideSleepInterval(sleepStartMinutes: Int, sleepEndMinutes: Int): Boolean {
        val minutesInDay = 24 * 60
        val calendar = java.util.Calendar.getInstance()
        val nowMinutes = calendar.get(java.util.Calendar.HOUR_OF_DAY) * 60 +
            calendar.get(java.util.Calendar.MINUTE)
        return isInsideSleepInterval(nowMinutes, sleepStartMinutes, sleepEndMinutes)
    }

    fun isInsideSleepInterval(
        nowMinutes: Int,
        sleepStartMinutes: Int,
        sleepEndMinutes: Int
    ): Boolean {
        val minutesInDay = 24 * 60
        val normalizedNow = ((nowMinutes % minutesInDay) + minutesInDay) % minutesInDay
        val normalizedStart = ((sleepStartMinutes % minutesInDay) + minutesInDay) % minutesInDay
        val normalizedEnd = ((sleepEndMinutes % minutesInDay) + minutesInDay) % minutesInDay
        if (normalizedStart == normalizedEnd) {
            return false
        }
        return if (normalizedStart < normalizedEnd) {
            normalizedNow in normalizedStart until normalizedEnd
        } else {
            normalizedNow >= normalizedStart || normalizedNow < normalizedEnd
        }
    }

    fun calculateNextSleepEventMillis(sleepStartMinutes: Int,
                                      sleepEndMinutes: Int): Long {
        return calculateNextSleepEventMillisAt(
            java.util.Calendar.getInstance().timeInMillis,
            sleepStartMinutes,
            sleepEndMinutes
        )
    }

    fun calculateNextSleepEventMillisAt(
        nowMillis: Long,
        sleepStartMinutes: Int,
        sleepEndMinutes: Int
    ): Long {
        val minutesInDay = 24 * 60
        val normalizedStart = ((sleepStartMinutes % minutesInDay) + minutesInDay) % minutesInDay
        val normalizedEnd = ((sleepEndMinutes % minutesInDay) + minutesInDay) % minutesInDay
        val now = java.util.Calendar.getInstance().apply { timeInMillis = nowMillis }
        val nextStartMillis = nextOccurrenceMillis(now, normalizedStart)
        val nextEndMillis = nextOccurrenceMillis(now, normalizedEnd)
        return kotlin.math.min(nextStartMillis, nextEndMillis)
    }

    private fun nextOccurrenceMillis(now: java.util.Calendar, minutesOfDay: Int): Long {
        val candidate = now.clone() as java.util.Calendar
        candidate.set(java.util.Calendar.HOUR_OF_DAY, minutesOfDay / 60)
        candidate.set(java.util.Calendar.MINUTE, minutesOfDay % 60)
        candidate.set(java.util.Calendar.SECOND, 0)
        candidate.set(java.util.Calendar.MILLISECOND, 0)
        if (candidate.timeInMillis < now.timeInMillis) {
            candidate.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        return candidate.timeInMillis
    }
}
