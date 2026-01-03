package com.mindwarrior.app.apptimers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class OneOffTimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        OneOffTimerController.handleAlarm(context)
    }
}
