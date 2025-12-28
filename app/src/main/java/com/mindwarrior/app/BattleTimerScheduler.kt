package com.mindwarrior.app

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import kotlin.math.max

object BattleTimerScheduler {
    private const val PREFS_NAME = "mindwarrior_prefs"
    private const val KEY_NEXT_TRIGGER = "battle_next_trigger"
    private const val KEY_NOTIFICATION_COUNT = "battle_notification_count"

    private const val CHANNEL_ID = "battle_timer_channel"
    private const val CHANNEL_NAME = "Battle Timer"
    private const val NOTIFICATION_ID = 1002

    fun ensureScheduled(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val current = prefs.getLong(KEY_NEXT_TRIGGER, 0L)
        val next = if (current == 0L || current <= now) {
            now + getIntervalMillis(DifficultyPreferences.getDifficulty(context))
        } else {
            current
        }
        prefs.edit().putLong(KEY_NEXT_TRIGGER, next).apply()
        scheduleAlarm(context, next)
    }

    fun restart(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val next = System.currentTimeMillis() + getIntervalMillis(DifficultyPreferences.getDifficulty(context))
        prefs.edit().putLong(KEY_NEXT_TRIGGER, next).apply()
        scheduleAlarm(context, next)
    }

    fun getRemainingMillis(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val next = prefs.getLong(KEY_NEXT_TRIGGER, 0L)
        if (next == 0L) return 0L
        return max(0L, next - System.currentTimeMillis())
    }

    fun handleAlarm(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val count = prefs.getInt(KEY_NOTIFICATION_COUNT, 0) + 1
        prefs.edit().putInt(KEY_NOTIFICATION_COUNT, count).apply()

        showNotification(context)

        val next = System.currentTimeMillis() + getIntervalMillis(DifficultyPreferences.getDifficulty(context))
        prefs.edit().putLong(KEY_NEXT_TRIGGER, next).apply()
        scheduleAlarm(context, next)
    }

    private fun getIntervalMillis(difficulty: Difficulty): Long {
        val minutes = when (difficulty) {
            Difficulty.BEGINNER -> 6 * 60
            Difficulty.EASY -> 3 * 60
            Difficulty.MEDIUM -> 90
            Difficulty.HARD -> 60
            Difficulty.EXPORT -> 45
        }
        return minutes * 60_000L
    }

    private fun scheduleAlarm(context: Context, triggerAtMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, TimerAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (android.os.Build.VERSION.SDK_INT >= 31) {
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

    private fun showNotification(context: Context) {
        ensureNotificationChannel(context)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Time's up")
            .setContentText("Your battle timer has finished.")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)

        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val ringtone = RingtoneManager.getRingtone(context, uri)
        ringtone.play()
    }

    private fun ensureNotificationChannel(context: Context) {
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
