package com.mindwarrior.app

import android.content.Context

object NowProvider {
    private const val PREFS_NAME = "mindwarrior_time"
    private const val KEY_OFFSET_MILLIS = "now_offset_millis"

    @Volatile
    private var offsetMillis: Long = 0L
    @Volatile
    private var initialized = false
    private lateinit var prefs: android.content.SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        offsetMillis = prefs.getLong(KEY_OFFSET_MILLIS, 0L)
        initialized = true
    }

    fun nowMillis(): Long {
        val offset = if (initialized) offsetMillis else 0L
        return System.currentTimeMillis() + offset
    }

    fun moveTimeForwardMins(minutes: Long) {
        if (minutes <= 0L) {
            return
        }
        val millis = minutes * 60_000L
        offsetMillis += millis
        if (initialized) {
            prefs.edit().putLong(KEY_OFFSET_MILLIS, offsetMillis).apply()
        }
    }
}
