package com.mindwarrior.app.engine

import java.util.Optional

enum class AlertType {
    Reminder,
    Penalty
}

// Do not modify the fields directly! Use GameManager for that!
data class User(
    var pausedTimerSerialized: Optional<String>,
    var activePlayTimerSerialized: String,
    var lastRewardAtActivePlayTime: Long,
    var nextPenaltyTimerSerialized: String,
    var nextAlertType: AlertType,
    var timerForegroundEnabled: Boolean,
    var nextSleepEventAtMillis: Optional<Long>,
    var sleepStartMinutes: Int,
    var sleepEndMinutes: Int,
    var difficulty: Difficulty,
    var localStorageSnapshot: Optional<String>,

    val pendingNotificationLogsNewestFirst: List<Pair<String, Long>>,
    val unseenLogsNewestFirst: List<Pair<String, Long>>,
    val oldLogsNewestFirst: List<Pair<String, Long>>
)

object UserFactory {
    fun createUser(difficulty: Difficulty): User {
        val pausedTimer = Counter(null)
        pausedTimer.resume()
        val activePlayTimer = Counter(null)
        activePlayTimer.pause()
        val reviewTimerSerialized = Counter(null)
        reviewTimerSerialized.pause()
        return User(
            pausedTimerSerialized = Optional.of(pausedTimer.serialize()),
            activePlayTimerSerialized = activePlayTimer.serialize(),
            lastRewardAtActivePlayTime = 0L,
            nextPenaltyTimerSerialized = reviewTimerSerialized.serialize(),
            nextAlertType = AlertType.Reminder,
            timerForegroundEnabled = false,
            nextSleepEventAtMillis = Optional.empty(),
            sleepStartMinutes = 23 * 60,
            sleepEndMinutes = 7 * 60,
            difficulty = difficulty,
            localStorageSnapshot = Optional.empty(),
            pendingNotificationLogsNewestFirst = emptyList(),
            unseenLogsNewestFirst = emptyList(),
            oldLogsNewestFirst = emptyList()
        )
    }
}
