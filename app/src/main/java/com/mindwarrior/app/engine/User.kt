package com.mindwarrior.app.engine

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
    var reviewTimerSerialized: String,
    var nextAlertType: AlertType,
    var timerForegroundEnabled: Boolean,
    var sleepEnabled: Boolean,
    var sleepStartMinutes: Int,
    var sleepEndMinutes: Int,
    var difficulty: Difficulty,
    var localStorageSnapshot: Optional<String>,
    var nextReviewDeadlineAtMillis: Long
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
            reviewTimerSerialized = reviewTimerSerialized.serialize(),
            nextAlertType = AlertType.Reminder,
            timerForegroundEnabled = false,
            sleepEnabled = false,
            sleepStartMinutes = 23 * 60,
            sleepEndMinutes = 7 * 60,
            difficulty = difficulty,
            localStorageSnapshot = Optional.empty(),
            nextReviewDeadlineAtMillis = System.currentTimeMillis() +
                DifficultyHelper.getReviewFrequencyMillis(difficulty)
        )
    }
}
