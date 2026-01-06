package com.mindwarrior.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mindwarrior.app.UserStorage
import com.mindwarrior.app.engine.GameManager

class OneOffAlertReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val user = UserStorage.getUser(context)
        val updatedUser = GameManager.evaluateAlerts(user)
        if (updatedUser != user) {
            UserStorage.upsertUser(context, updatedUser)
        }
        OneOffAlertController.scheduleNextAlert(context, if (updatedUser != user) updatedUser else user)
    }
}
