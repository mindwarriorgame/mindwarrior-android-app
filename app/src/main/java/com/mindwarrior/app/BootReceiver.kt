package com.mindwarrior.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            BattleTimerScheduler.ensureScheduled(context)
            if (android.os.Build.VERSION.SDK_INT < 33 ||
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.content.ContextCompat.startForegroundService(
                    context,
                    Intent(context, TimerForegroundService::class.java)
                )
            }
        }
    }
}
