package com.mindwarrior.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            BattleTimerScheduler.ensureScheduled(context)
            if (SettingsPreferences.isForegroundEnabled(context)) {
                TimerServiceController.start(context)
            }
        }
    }
}
