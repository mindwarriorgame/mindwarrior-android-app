package com.mindwarrior.app.engine

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
    var timerForegroundEnabled: Boolean
)
