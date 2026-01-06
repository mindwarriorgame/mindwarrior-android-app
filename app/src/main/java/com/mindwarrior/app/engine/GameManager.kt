package com.mindwarrior.app.engine

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
        val nextSleepEventAtMillis = if (draftEnabled) {
            Optional.of(
                SleepUtils.calculateNextSleepEventMillisAt(
                    System.currentTimeMillis(),
                    draftStartMinutes,
                    draftEndMinutes
                )
            )
        } else {
            Optional.empty()
        }
        return user.copy(
            nextSleepEventAtMillis = nextSleepEventAtMillis,
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
                    System.currentTimeMillis()
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

    fun evaluateAlerts(user: User): User {
        if (user.nextSleepEventAtMillis.isPresent && user.nextSleepEventAtMillis.get() < System.currentTimeMillis()) {
            return handleAutoSleepEvent(user)
        }

        if (user.pausedTimerSerialized.isPresent) {
            return user
        }

        val penaltyThreshold = DifficultyHelper.getReviewFrequencyMillis(user.difficulty)
        val penaltyTimerStartedAtMillis = System.currentTimeMillis() - Counter(user.nextPenaltyTimerSerialized).getTotalSeconds() * 1000

        if (user.nextAlertType == AlertType.Reminder) {
            val nudgeThreshold = penaltyThreshold - 15 * 60 * 1000

            val nudgeThesholdAtMsecs = penaltyTimerStartedAtMillis + nudgeThreshold

            if (nudgeThesholdAtMsecs < System.currentTimeMillis()) {
                return user.copy(
                    nextAlertType = AlertType.Penalty,
                    pendingNotificationLogsNewestFirst = listOf(
                        Pair("⏰ Don't forget to review your formula!", System.currentTimeMillis())
                    ) + user.pendingNotificationLogsNewestFirst
                )
            }
        }

        if (user.nextAlertType == AlertType.Penalty
            && (penaltyTimerStartedAtMillis + penaltyThreshold) < System.currentTimeMillis()
        ) {
            return user.copy(
                nextAlertType = if (DifficultyHelper.hasNudge(user.difficulty)) {
                    AlertType.Reminder
                } else {
                    AlertType.Penalty
                },
                nextPenaltyTimerSerialized = Counter(null).serialize(),
                pendingNotificationLogsNewestFirst = listOf(
                    Pair("🟥 have missed the review!", System.currentTimeMillis())
                ) + user.pendingNotificationLogsNewestFirst
            )
        }

        return user
    }

    fun calculateNextAlertMillis(user: User): Long {
        var candidates: List<Long> = listOf()
        if (user.pausedTimerSerialized.isPresent) {
            if (!user.nextSleepEventAtMillis.isPresent) {
                return System.currentTimeMillis() + 5 * 365 * 24 * 3600 * 1000L;
            }
            candidates = candidates + user.nextSleepEventAtMillis.get()
        }
        val penaltyThreshold = DifficultyHelper.getReviewFrequencyMillis(user.difficulty)
        val penaltyTimerStartedAtMillis = System.currentTimeMillis() - Counter(user.nextPenaltyTimerSerialized).getTotalSeconds() * 1000

        if (user.nextAlertType == AlertType.Reminder) {
            val nudgeThreshold = penaltyThreshold - 15 * 60 * 1000

            candidates = candidates + (penaltyTimerStartedAtMillis + nudgeThreshold)
        } else {
            candidates = candidates + (penaltyTimerStartedAtMillis + penaltyThreshold)
        }

        return candidates.sorted().get(0)
    }

    private fun handleAutoSleepEvent(user: User): User {
        val nextSleepEventAtMillis = Optional.of(
            SleepUtils.calculateNextSleepEventMillisAt(
                System.currentTimeMillis(),
                user.sleepStartMinutes,
                user.sleepEndMinutes
            )
        )
        if (SleepUtils.isNowInsideSleepInterval(user.sleepStartMinutes, user.sleepEndMinutes)
            && !user.pausedTimerSerialized.isPresent
        ) {
            return user.copy(
                pausedTimerSerialized = Optional.of(Counter(null).resume().serialize()),
                activePlayTimerSerialized = Counter(user.activePlayTimerSerialized).pause().serialize(),
                nextPenaltyTimerSerialized = Counter(user.nextPenaltyTimerSerialized).pause().serialize(),
                nextSleepEventAtMillis = nextSleepEventAtMillis,
                pendingNotificationLogsNewestFirst = listOf(
                    Pair("💤 Time to sleep. The game is automatically paused ⏸️", System.currentTimeMillis())
                ) + user.pendingNotificationLogsNewestFirst
            )
        }
        if (!SleepUtils.isNowInsideSleepInterval(user.sleepStartMinutes, user.sleepEndMinutes)
            && user.pausedTimerSerialized.isPresent
        ) {
            return user.copy(
                pausedTimerSerialized = Optional.empty(),
                activePlayTimerSerialized = Counter(user.activePlayTimerSerialized).resume().serialize(),
                nextPenaltyTimerSerialized = Counter(user.nextPenaltyTimerSerialized).resume().serialize(),
                nextSleepEventAtMillis = nextSleepEventAtMillis,
                pendingNotificationLogsNewestFirst = listOf(
                    Pair("☀️ Good morning! The game is resumed ▶️", System.currentTimeMillis())
                ) + user.pendingNotificationLogsNewestFirst
            )
        }
        return user.copy(
            nextSleepEventAtMillis = nextSleepEventAtMillis
        )
    }

    fun calculateNextDeadlineAtMillis(user: User): Long {
        return System.currentTimeMillis() +
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
