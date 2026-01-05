package com.mindwarrior.app.engine

import com.mindwarrior.app.TimeHelperObject
import java.util.Optional
import org.json.JSONObject

object GameManager {

    fun onDifficultyChanged(user: User, newDifficulty: Difficulty): User {
        val newUser = UserFactory.createUser(newDifficulty).copy(
            localStorageSnapshot = user.localStorageSnapshot
        )
        return if (user.pausedTimerSerialized.isPresent) {
            newUser.copy(
                pausedTimerSerialized = Optional.of(Counter(null).resume().serialize()),
                activePlayTimerSerialized = Counter(newUser.activePlayTimerSerialized).pause().serialize(),
                nextPenaltyTimerSerialized = Counter(newUser.nextPenaltyTimerSerialized).pause().serialize()
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
            nextPenaltyTimerSerialized = Counter(user.nextPenaltyTimerSerialized).pause().serialize(),
            activePlayTimerSerialized = Counter(user.activePlayTimerSerialized).pause().serialize()
        )
    }

    fun onResume(user: User): User {
        if (!user.pausedTimerSerialized.isPresent) {
            return user
        }
        return user.copy(
            pausedTimerSerialized = Optional.empty(),
            nextPenaltyTimerSerialized = Counter(user.nextPenaltyTimerSerialized).resume().serialize(),
            activePlayTimerSerialized = Counter(user.activePlayTimerSerialized).resume().serialize()
        )
    }

    fun onLocalStorageUpdated(user: User, localStorate: Optional<String>): User {
        val userOldLocalStorage = user.localStorageSnapshot
        val updatedUser = user.copy(localStorageSnapshot = localStorate)
        if (!localStorate.isPresent) {
            return updatedUser
        }
        val isGameStarted = (!userOldLocalStorage.isPresent && localStorate.isPresent)
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
            nextPenaltyTimerSerialized = Counter(user.nextPenaltyTimerSerialized).resume().serialize(),
            activePlayTimerSerialized = Counter(user.activePlayTimerSerialized).resume().serialize(),
            unseenLogsNewestFirst = listOf(
                Pair(
                    "The game has started! Don't forget to review your Formula before the time runs out!",
                    TimeHelperObject.currentTimeMillis()
                )
            ) + user.unseenLogsNewestFirst
        )
    }

    fun onUnseenLogsObserved(user: User): User {
        if (user.unseenLogsNewestFirst.isEmpty()) {
            return user
        }
        val mergedLogs = (user.unseenLogsNewestFirst + user.oldLogsNewestFirst)
            .sortedByDescending { it.second }
        return user.copy(
            unseenLogsNewestFirst = emptyList(),
            oldLogsNewestFirst = mergedLogs
        )
    }

    fun calculateNextDeadlineAtMillis(user: User): Long {
        return TimeHelperObject.currentTimeMillis() +
            DifficultyHelper.getReviewFrequencyMillis(user.difficulty) -
                Counter(user.nextPenaltyTimerSerialized).getTotalSeconds() * 1000
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
