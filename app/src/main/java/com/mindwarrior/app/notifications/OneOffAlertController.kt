package com.mindwarrior.app.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import com.mindwarrior.app.NowProvider
import com.mindwarrior.app.UserStorage
import com.mindwarrior.app.engine.GameManager
import com.mindwarrior.app.engine.User
import kotlin.math.max

object OneOffAlertController {
    private const val PREFS_NAME = "mindwarrior_prefs"
    private const val KEY_NEXT_TRIGGER = "battle_next_trigger"
    private const val KEY_PAUSED_REMAINING = "battle_timer_remaining"

    const val CHANNEL_ID = "battle_timer_channel"
    private const val CHANNEL_NAME = "Battle Timer"

    fun ensureScheduled(context: Context) {
        scheduleNextAlert(context, UserStorage.getUser(context))
    }

    fun restart(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_PAUSED_REMAINING)
            .apply()
        scheduleNextAlert(context, UserStorage.getUser(context))
    }

    fun getRemainingMillis(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (UserStorage.getUser(context).pausedTimerSerialized.isPresent) {
            return prefs.getLong(KEY_PAUSED_REMAINING, 0L)
        }
        val next = prefs.getLong(KEY_NEXT_TRIGGER, 0L)
        if (next == 0L) return 0L
        return max(0L, next - NowProvider.nowMillis())
    }

    fun pauseTimer(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val remaining = getRemainingMillis(context)
        prefs.edit()
            .putLong(KEY_PAUSED_REMAINING, remaining)
            .apply()
        cancelAlarm(context)
    }

    fun resumeTimer(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val remaining = prefs.getLong(KEY_PAUSED_REMAINING, 0L)
        val next = NowProvider.nowMillis() + remaining
        prefs.edit()
            .putLong(KEY_NEXT_TRIGGER, next)
            .remove(KEY_PAUSED_REMAINING)
            .apply()
        cancelAlarm(context)
        scheduleAlarm(context, next)
    }

    fun scheduleNextAlert(context: Context, user: User) {
        val next = GameManager.calculateNextAlertMillis(user)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_NEXT_TRIGGER, next).apply()
        cancelAlarm(context)
        scheduleAlarm(context, next)
    }

    private fun scheduleAlarm(context: Context, triggerAtMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = createAlarmPendingIntent(context)
        if (Build.VERSION.SDK_INT >= 31) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    private fun cancelAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = createAlarmPendingIntent(context)
        alarmManager.cancel(pendingIntent)
    }

    private fun createAlarmPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, OneOffAlertReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun ensureNotificationChannel(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val existing = notificationManager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val attributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .build()
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
        channel.setSound(soundUri, attributes)
        channel.enableVibration(true)
        notificationManager.createNotificationChannel(channel)
    }
}
