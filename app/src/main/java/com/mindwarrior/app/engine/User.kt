package com.mindwarrior.app.engine

import java.util.Optional

enum class AlertType {
    Reminder,
    Penalty,
    WakeUp
}

data class User(
    val pausedTimerSerialized: Optional<String>,
    val activePlayTimerSerialized: String,
    val lastRewardAtActivePlayTime: Long,
    val reviewTimerSerialized: String,
    val nextAlertType: AlertType
)
