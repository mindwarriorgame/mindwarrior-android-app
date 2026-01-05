package com.mindwarrior.app.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.appcompat.content.res.AppCompatResources
import com.mindwarrior.app.engine.Difficulty
import com.mindwarrior.app.MainActivity
import com.mindwarrior.app.R
import com.mindwarrior.app.TimeHelperObject
import com.mindwarrior.app.UserStorage
import com.mindwarrior.app.engine.DifficultyHelper
import kotlin.math.max

object OneOffAlertController {
    private const val PREFS_NAME = "mindwarrior_prefs"
    private const val KEY_NEXT_TRIGGER = "battle_next_trigger"
    private const val KEY_NOTIFICATION_COUNT = "battle_notification_count"
    private const val KEY_PAUSED_REMAINING = "battle_timer_remaining"

    const val CHANNEL_ID = "battle_timer_channel"
    private const val CHANNEL_NAME = "Battle Timer"
    private const val NOTIFICATION_ID = 1002

    fun ensureScheduled(context: Context) {
        if (UserStorage.getUser(context).pausedTimerSerialized.isPresent) {
            return
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val now = TimeHelperObject.currentTimeMillis()
        val current = prefs.getLong(KEY_NEXT_TRIGGER, 0L)
        val next = if (current == 0L || current <= now) {
            now + DifficultyHelper.getReviewFrequencyMillis(UserStorage.getUser(context).difficulty)
        } else {
            current
        }
        prefs.edit().putLong(KEY_NEXT_TRIGGER, next).apply()
        cancelAlarm(context)
        scheduleAlarm(context, next)
    }

    fun restart(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val next = TimeHelperObject.currentTimeMillis() +
            DifficultyHelper.getReviewFrequencyMillis(UserStorage.getUser(context).difficulty)
        prefs.edit()
            .putLong(KEY_NEXT_TRIGGER, next)
            .remove(KEY_PAUSED_REMAINING)
            .apply()
        cancelAlarm(context)
        scheduleAlarm(context, next)
    }

    fun getRemainingMillis(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (UserStorage.getUser(context).pausedTimerSerialized.isPresent) {
            return prefs.getLong(KEY_PAUSED_REMAINING, 0L)
        }
        val next = prefs.getLong(KEY_NEXT_TRIGGER, 0L)
        if (next == 0L) return 0L
        return max(0L, next - TimeHelperObject.currentTimeMillis())
    }

    fun pauseTimer(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val remaining = getRemainingMillis(context)
        prefs.edit()
            .putLong(KEY_PAUSED_REMAINING, remaining)
            .apply()
        cancelAlarm(context)
        cancelNotification(context)
    }

    fun resumeTimer(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val remaining = prefs.getLong(KEY_PAUSED_REMAINING, 0L)
        val next = TimeHelperObject.currentTimeMillis() + remaining
        prefs.edit()
            .putLong(KEY_NEXT_TRIGGER, next)
            .remove(KEY_PAUSED_REMAINING)
            .apply()
        cancelAlarm(context)
        scheduleAlarm(context, next)
    }

    fun handleAlarm(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val count = prefs.getInt(KEY_NOTIFICATION_COUNT, 0) + 1
        prefs.edit().putInt(KEY_NOTIFICATION_COUNT, count).apply()

        showNotification(context)

        val next = TimeHelperObject.currentTimeMillis() +
            DifficultyHelper.getReviewFrequencyMillis(UserStorage.getUser(context).difficulty)
        prefs.edit().putLong(KEY_NEXT_TRIGGER, next).apply()
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

    private fun cancelNotification(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun showNotification(context: Context) {
        ensureNotificationChannel(context)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val contentIntent = PendingIntent.getActivity(
            context,
            1002,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(loadAppIcon(context))
            .setContentTitle("Time's up")
            .setContentText("Your battle timer has finished.")
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)

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

    private fun loadAppIcon(context: Context): Bitmap? {
        val drawable = AppCompatResources.getDrawable(context, R.drawable.ic_launcher) ?: return null
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 108
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 108
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
