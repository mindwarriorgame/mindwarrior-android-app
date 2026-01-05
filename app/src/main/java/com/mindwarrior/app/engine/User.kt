package com.mindwarrior.app.engine

import com.mindwarrior.app.TimeHelperObject
import java.util.Optional

enum class AlertType {
    Reminder,
    Penalty,
    WakeUp
}

// Do not modify the fields directly! Use GameManager for that!
data class User(
    var pausedTimerSerialized: Optional<String>,
    var activePlayTimerSerialized: String,
    var lastRewardAtActivePlayTime: Long,
    var nextPenaltyTimerSerialized: String,
    var nextAlertType: AlertType,
    var timerForegroundEnabled: Boolean,
    var sleepEnabled: Boolean,
    var sleepStartMinutes: Int,
    var sleepEndMinutes: Int,
    var difficulty: Difficulty,
    var localStorageSnapshot: Optional<String>,
    var eventsLastProcessedInclusiveEpochSecs: Long,

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
            sleepEnabled = false,
            sleepStartMinutes = 23 * 60,
            sleepEndMinutes = 7 * 60,
            difficulty = difficulty,
            localStorageSnapshot = Optional.empty(),
            eventsLastProcessedInclusiveEpochSecs = TimeHelperObject.currentTimeMillis() / 1000L,
            pendingNotificationLogsNewestFirst = emptyList(),
            unseenLogsNewestFirst = emptyList(),
            oldLogsNewestFirst = emptyList()
        )
    }
}
