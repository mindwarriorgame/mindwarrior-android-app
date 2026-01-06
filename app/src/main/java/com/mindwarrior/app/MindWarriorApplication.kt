package com.mindwarrior.app

import android.app.Application
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.NotificationCompat
import com.mindwarrior.app.engine.User
import com.mindwarrior.app.notifications.OneOffAlertController

class MindWarriorApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        UserStorage.observeUserChanges(this, object : UserStorage.UserUpdateListener {
            override fun onUserUpdated(user: User) {
                if (user.pendingNotificationLogsNewestFirst.isNotEmpty()) {
                    val pending = user.pendingNotificationLogsNewestFirst
                    showNotification(pending.first().first)
                    val updated = user.copy(
                        pendingNotificationLogsNewestFirst = emptyList(),
                        unseenLogsNewestFirst = pending + user.unseenLogsNewestFirst
                    )
                    UserStorage.upsertUser(applicationContext, updated)
                    return
                }
                OneOffAlertController.scheduleNextAlert(applicationContext, user)
            }
        })
    }

    private fun showNotification(message: String) {
        if (message.isBlank()) {
            return
        }
        OneOffAlertController.ensureNotificationChannel(this)
        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val contentIntent = PendingIntent.getActivity(
            this,
            NOTIFICATION_ID,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, OneOffAlertController.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(loadAppIcon())
            .setContentTitle(getString(R.string.app_name))
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun loadAppIcon(): Bitmap? {
        val drawable = AppCompatResources.getDrawable(this, R.drawable.ic_launcher) ?: return null
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 108
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 108
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    companion object {
        private const val NOTIFICATION_ID = 1002
    }
}
