package com.mindwarrior.app.engine

import com.mindwarrior.app.engine.Difficulty
import java.util.Optional

enum class AlertType {
    Reminder,
    Penalty,
    WakeUp
}

data class User(
    var pausedTimerSerialized: Optional<String>,
    var activePlayTimerSerialized: String,
    var lastRewardAtActivePlayTime: Long,
    var reviewTimerSerialized: String,
    var nextAlertType: AlertType,
    var timerForegroundEnabled: Boolean,
    var sleepEnabled: Boolean,
    var sleepStartMinutes: Int,
    var sleepEndMinutes: Int,
    var difficulty: Difficulty,
    var localStorageSnapshot: Optional<String>
)
