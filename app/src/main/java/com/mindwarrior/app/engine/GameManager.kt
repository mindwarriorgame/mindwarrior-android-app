package com.mindwarrior.app.engine

import java.util.Optional
import org.json.JSONObject
import kotlin.jvm.optionals.getOrNull

object GameManager {

    fun onDifficultyChanged(user: User, newDifficulty: Difficulty): User {
        val newUser = UserFactory.createUser(newDifficulty)
        return if (user.pausedTimerSerialized.isPresent) {
            newUser.copy(
                pausedTimerSerialized = Optional.of(Counter(null).resume().serialize()),
                activePlayTimerSerialized = Counter(newUser.activePlayTimerSerialized).pause().serialize(),
                reviewTimerSerialized = Counter(newUser.reviewTimerSerialized).pause().serialize()
            )
        } else {
            newUser
        }
    }

    fun onSleepScheduleChanged(
        user: User,
        draftEnabled: Boolean,
        draftStartMinutes: Int,
        draftEndMinutes: Int
    ): User {
        return user.copy(
            sleepEnabled = draftEnabled,
            sleepStartMinutes = draftStartMinutes,
            sleepEndMinutes = draftEndMinutes
        )

    }

    fun onPaused(user: User): User {
        if (user.pausedTimerSerialized.isPresent) {
            return user;
        }
        return user.copy(
            pausedTimerSerialized = Optional.of(Counter(null).resume().serialize()),
            reviewTimerSerialized = Counter(user.reviewTimerSerialized).pause().serialize(),
            activePlayTimerSerialized = Counter(user.activePlayTimerSerialized).pause().serialize()
        )
    }

    fun onResume(user: User): User {
        if (user.pausedTimerSerialized.isEmpty) {
            return user
        }
        return user.copy(
            pausedTimerSerialized = Optional.empty(),
            reviewTimerSerialized = Counter(user.reviewTimerSerialized).resume().serialize(),
            activePlayTimerSerialized = Counter(user.activePlayTimerSerialized).resume().serialize()
        )
    }

    fun onLocalStorageUpdated(user: User, localStorate: Optional<String>): User {
        val updatedUser = user.copy(localStorageSnapshot = localStorate)
        if (!localStorate.isPresent) {
            return updatedUser
        }
        if (!user.pausedTimerSerialized.isPresent) {
            return updatedUser
        }
        val activePlaySeconds = Counter(user.activePlayTimerSerialized).getTotalSeconds()
        if (activePlaySeconds > ACTIVE_PLAY_NEAR_ZERO_SECONDS) {
            return updatedUser
        }
        if (!hasFormula(localStorate.get())) {
            return updatedUser
        }
        return updatedUser.copy(
            pausedTimerSerialized = Optional.empty(),
            reviewTimerSerialized = Counter(user.reviewTimerSerialized).resume().serialize(),
            activePlayTimerSerialized = Counter(user.activePlayTimerSerialized).resume().serialize()
        )
    }

    fun calculateNextDeadlineAtMillis(user: User): Long {
        return System.currentTimeMillis() + DifficultyHelper.getReviewFrequencyMillis(user.difficulty) -
                Counter(user.reviewTimerSerialized).getTotalSeconds() * 1000
    }

    private fun hasFormula(localStorageJson: String): Boolean {
        return try {
            val value = JSONObject(localStorageJson).optString("formula", "")
            value.isNotBlank()
        } catch (_: Exception) {
            false
        }
    }

    private const val ACTIVE_PLAY_NEAR_ZERO_SECONDS = 10L
}
