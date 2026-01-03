package com.mindwarrior.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class OneOffAlertReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        OneOffAlertController.handleAlarm(context)
    }
}
