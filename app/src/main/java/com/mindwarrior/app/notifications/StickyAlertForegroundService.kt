package com.mindwarrior.app.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.LocaleList
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import android.app.PendingIntent
import com.mindwarrior.app.MainActivity
import com.mindwarrior.app.LanguageManager
import com.mindwarrior.app.R
import com.mindwarrior.app.NowProvider
import com.mindwarrior.app.UserStorage
import com.mindwarrior.app.engine.Counter
import com.mindwarrior.app.engine.GameManager
import java.util.Locale

class StickyAlertForegroundService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            updateNotification()
            handler.postDelayed(this, 1000L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification())
        handler.post(ticker)
    }

    override fun onDestroy() {
        handler.removeCallbacks(ticker)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        updateNotification()
        return START_STICKY
    }

    private fun buildNotification(): Notification {
        val channelId = STICKY_CHANNEL_ID_V2
        ensureStickyNotificationChannel()
        val localizedContext = getLocalizedContext()

        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setContentIntent(createContentIntent())
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentTitle(localizedContext.getString(R.string.timer_notification_review_title))
            .setContentText(localizedContext.getString(R.string.timer_notification_loading))
            .setSound(null)
            .setVibrate(null)
            .setDefaults(0)
            .build()
    }

    private fun updateNotification() {
        val localizedContext = getLocalizedContext()
        val user = UserStorage.getUser(this)
        val isPaused = user.pausedTimerSerialized.isPresent
        val contentText = if (isPaused) {
            localizedContext.getString(R.string.timer_notification_paused) + " ⏸"
        } else {
            val activePlaySeconds = Counter(user.activePlayTimerSerialized).getTotalSeconds()
            val deltaSeconds =
                (activePlaySeconds - user.lastRewardAtActivePlayTime).coerceAtLeast(0L)
            val freezeSuffix = if (deltaSeconds < FREEZE_WINDOW_SECONDS) " ❄️" else ""
            val remaining = (GameManager.calculateNextDeadlineAtMillis(user) - NowProvider.nowMillis())
                .coerceAtLeast(0L)
            formatRemaining(localizedContext, remaining) + freezeSuffix
        }

        val notification = NotificationCompat.Builder(this, STICKY_CHANNEL_ID_V2)
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setContentIntent(createContentIntent())
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentTitle(localizedContext.getString(R.string.timer_notification_review_title))
            .setContentText(contentText)
            .setSound(null)
            .setVibrate(null)
            .setDefaults(0)
            .build()

        ContextCompat.getSystemService(this, NotificationManager::class.java)
            ?.notify(NOTIFICATION_ID, notification)
    }

    private fun formatRemaining(localizedContext: Context, remainingMillis: Long): String {
        val totalSeconds = remainingMillis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return localizedContext.getString(R.string.time_format_hms, hours, minutes, seconds)
    }

    private fun createContentIntent(): PendingIntent {
        return PendingIntent.getActivity(
            this,
            2001,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private const val NOTIFICATION_ID = 2002
        private const val STICKY_CHANNEL_ID_V2 = "battle_timer_sticky_v2"
        private const val FREEZE_WINDOW_SECONDS = 5 * 60L
    }

    private fun ensureStickyNotificationChannel() {
        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val existing = notificationManager.getNotificationChannel(STICKY_CHANNEL_ID_V2)
        if (existing != null) return

        val localizedContext = getLocalizedContext()
        val channel = NotificationChannel(
            STICKY_CHANNEL_ID_V2,
            localizedContext.getString(R.string.notification_channel_review_timer_sticky),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        channel.setSound(null, null)
        channel.enableVibration(false)
        channel.enableLights(false)
        notificationManager.createNotificationChannel(channel)
    }

    private fun getLocalizedContext(): Context {
        val tag = LanguageManager.getCurrentLanguageTag(this)
        val locale = Locale.forLanguageTag(tag)
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(locale))
        }
        return createConfigurationContext(config)
    }
}
