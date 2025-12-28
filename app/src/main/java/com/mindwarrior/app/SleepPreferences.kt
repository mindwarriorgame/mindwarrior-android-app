package com.mindwarrior.app

import android.content.Context

object SleepPreferences {
    private const val PREFS_NAME = "mindwarrior_prefs"
    private const val KEY_ENABLED = "sleep_enabled"
    private const val KEY_START_MINUTES = "sleep_start_minutes"
    private const val KEY_END_MINUTES = "sleep_end_minutes"

    fun isEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_ENABLED, false)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun getStartMinutes(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_START_MINUTES, 23 * 60)
    }

    fun setStartMinutes(context: Context, minutes: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_START_MINUTES, minutes).apply()
    }

    fun getEndMinutes(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_END_MINUTES, 7 * 60)
    }

    fun setEndMinutes(context: Context, minutes: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_END_MINUTES, minutes).apply()
    }
}
