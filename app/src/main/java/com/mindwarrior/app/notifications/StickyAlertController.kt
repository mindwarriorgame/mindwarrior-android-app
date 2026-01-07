package com.mindwarrior.app.notifications

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.mindwarrior.app.UserStorage
import android.util.Log

object StickyAlertController {
    fun start(context: Context) {
        Log.i(TAG, "StickyAlertController.start")
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                Log.w(TAG, "Notifications permission not granted; disabling timerForegroundEnabled")
                Handler(Looper.getMainLooper()).post {
                    val user = UserStorage.getUser(context)
                    if (user.timerForegroundEnabled) {
                        UserStorage.upsertUser(context, user.copy(timerForegroundEnabled = false))
                    }
                }
                return
            }
        }
        ContextCompat.startForegroundService(context, Intent(context, StickyAlertForegroundService::class.java))
    }

    fun stop(context: Context) {
        Log.i(TAG, "StickyAlertController.stop")
        context.stopService(Intent(context, StickyAlertForegroundService::class.java))
    }

    private const val TAG = "MindWarriorSticky"
}
