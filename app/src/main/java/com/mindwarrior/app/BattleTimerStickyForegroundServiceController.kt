package com.mindwarrior.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

object BattleTimerStickyForegroundServiceController {
    fun start(context: Context) {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        ContextCompat.startForegroundService(context, Intent(context, BattleTimerStickyForegroundService::class.java))
    }

    fun stop(context: Context) {
        context.stopService(Intent(context, BattleTimerStickyForegroundService::class.java))
    }
}
