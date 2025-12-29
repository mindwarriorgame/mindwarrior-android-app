package com.mindwarrior.app

import android.content.Context

object SettingsPreferences {
    private const val PREFS_NAME = "mindwarrior_prefs"
    private const val KEY_TIMER_FOREGROUND_ENABLED = "timer_foreground_enabled"

    fun isForegroundEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_TIMER_FOREGROUND_ENABLED, false)
    }

    fun setForegroundEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_TIMER_FOREGROUND_ENABLED, enabled).apply()
    }
}
