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
}
