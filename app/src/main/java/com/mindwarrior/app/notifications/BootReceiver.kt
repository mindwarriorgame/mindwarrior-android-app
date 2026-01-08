package com.mindwarrior.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.mindwarrior.app.UserStorage
import com.mindwarrior.app.engine.GameManager

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "BootReceiver.onReceive action=${intent.action}")
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val user = UserStorage.getUser(context)
            val updatedUser = GameManager.evaluateAlerts(
                user,
                context.getString(com.mindwarrior.app.R.string.log_prompt_reminder),
                context.getString(com.mindwarrior.app.R.string.log_prompt_penalty),
                context.getString(com.mindwarrior.app.R.string.log_grumpy_sneaked_in)
            )
            if (updatedUser != user) {
                UserStorage.upsertUser(context, updatedUser)
            }
            OneOffAlertController.scheduleNextAlert(context, updatedUser)
            if (updatedUser.timerForegroundEnabled) {
                Log.i(TAG, "Starting sticky service on boot (timerForegroundEnabled=true)")
                StickyAlertController.start(context)
            } else {
                Log.i(TAG, "Sticky service not started on boot (timerForegroundEnabled=false)")
            }
            return
        }
        if (Build.VERSION.SDK_INT >= 24 &&
            intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) {
            val enabled = UserStorage.isTimerForegroundEnabledDevice(context)
            Log.i(TAG, "Locked boot completed. deviceProtected timerForegroundEnabled=$enabled")
            if (enabled) {
                Log.i(TAG, "Starting sticky service on locked boot")
                StickyAlertController.start(context)
            }
        }
    }

    companion object {
        private const val TAG = "MindWarriorBoot"
    }
}
