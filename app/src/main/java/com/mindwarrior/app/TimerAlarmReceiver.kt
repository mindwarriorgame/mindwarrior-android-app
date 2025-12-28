package com.mindwarrior.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TimerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        BattleTimerScheduler.handleAlarm(context)
    }
}
