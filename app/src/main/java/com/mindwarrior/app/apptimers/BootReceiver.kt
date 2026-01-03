package com.mindwarrior.app.apptimers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mindwarrior.app.UserStorage

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            OneOffTimerController.ensureScheduled(context)
            if (UserStorage.getUser(context).timerForegroundEnabled) {
                StickyTimerController.start(context)
            }
        }
    }
}