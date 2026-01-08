package com.mindwarrior.app.engine

import com.mindwarrior.app.NowProvider
import com.mindwarrior.app.badges.BadgesManager
import com.mindwarrior.app.badges.BoardSerializer
import java.util.Optional
import org.json.JSONObject
import java.net.URLEncoder

object GameManager {
    const val MAX_UNSEEN_LOGS = 10
    const val MAX_OLD_LOGS = 100

    fun onDifficultyChanged(
        user: User,
        newDifficulty: Difficulty,
        logMessage: String
    ): User {
        val newUser = UserFactory.createUser(newDifficulty).copy(
            localStorageSnapshot = user.localStorageSnapshot,
            timerForegroundEnabled = user.timerForegroundEnabled,
            nextSleepEventAtMillis = user.nextSleepEventAtMillis,
            sleepStartMinutes = user.sleepStartMinutes,
            sleepEndMinutes = user.sleepEndMinutes,
            unseenLogsNewestFirst = listOf(Pair(logMessage, NowProvider.nowMillis()))
        )
        return if (user.pausedTimerSerialized.isPresent) {
            newUser.copy(
                pausedTimerSerialized = Optional.of(Counter(null).resume().serialize()),
                activePlayTimerSerialized = Counter(newUser.activePlayTimerSerialized).pause().serialize(),
                nextPenaltyTimerSerialized = Counter(newUser.nextPenaltyTimerSerialized).pause().serialize()
            )
        } else {
            newUser.copy(
                pausedTimerSerialized = Optional.empty(),
                activePlayTimerSerialized = Counter(newUser.activePlayTimerSerialized).resume().serialize(),
                nextPenaltyTimerSerialized = Counter(newUser.nextPenaltyTimerSerialized).resume().serialize()
            )
        }
    }

    fun onSleepScheduleChanged(
        user: User,
        draftEnabled: Boolean,
        draftStartMinutes: Int,
        draftEndMinutes: Int,
        logMessage: String
    ): User {
        val nextSleepEventAtMillis = if (draftEnabled) {
            Optional.of(
                SleepUtils.calculateNextSleepEventMillisAt(
                    NowProvider.nowMillis(),
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
            sleepEndMinutes = draftEndMinutes,
            unseenLogsNewestFirst = trimUnseenLogs(
                listOf(Pair(logMessage, NowProvider.nowMillis())) + user.unseenLogsNewestFirst
            )
        )

    }

    fun onPaused(user: User, logMessage: String): User {
        if (user.pausedTimerSerialized.isPresent) {
            return user;
        }
        val nowMillis = NowProvider.nowMillis()
        return user.copy(
            pausedTimerSerialized = Optional.of(Counter(null).resume().serialize()),
            nextPenaltyTimerSerialized = Counter(user.nextPenaltyTimerSerialized).pause().serialize(),
            activePlayTimerSerialized = Counter(user.activePlayTimerSerialized).pause().serialize(),
            unseenLogsNewestFirst = trimUnseenLogs(
                listOf(Pair(logMessage, nowMillis)) + user.unseenLogsNewestFirst
            )
        )
    }

    fun onResume(user: User, logMessage: String): User {
        if (!user.pausedTimerSerialized.isPresent) {
            return user
        }
        val nowMillis = NowProvider.nowMillis()
        return user.copy(
            pausedTimerSerialized = Optional.empty(),
            nextPenaltyTimerSerialized = Counter(user.nextPenaltyTimerSerialized).resume().serialize(),
            activePlayTimerSerialized = Counter(user.activePlayTimerSerialized).resume().serialize(),
            unseenLogsNewestFirst = trimUnseenLogs(
                listOf(Pair(logMessage, nowMillis)) + user.unseenLogsNewestFirst
            )
        )
    }

    fun onLocalStorageUpdated(
        user: User,
        localStorate: Optional<String>,
        triggeredByFormulaEditor: Boolean,
        newBadgeLogMessage: String = "",
        gameStartedLogMessage: String = "",
        formulaUpdatedLogMessage: String = "",
        grumpyBlockingLogMessage: String = ""
    ): User {
        val userOldLocalStorage = user.localStorageSnapshot
        var updatedUser = user.copy(localStorageSnapshot = localStorate)
        if (!localStorate.isPresent) {
            return updatedUser
        }
        val hasFormula = hasFormula(localStorate.get())
        val isGameStarted = (!userOldLocalStorage.isPresent && triggeredByFormulaEditor)
        val isFormulaUpdate = triggeredByFormulaEditor && userOldLocalStorage.isPresent
        if (!isGameStarted) {
            if (!isFormulaUpdate || !hasFormula) {
                return updatedUser
            }
        }
        val activePlaySeconds = Counter(updatedUser.activePlayTimerSerialized).getTotalSeconds()
        val manager = BadgesManager(updatedUser.difficulty.ordinal, updatedUser.badgesSerialized)
        val newBadge = if (isGameStarted) {
            manager.onGameStarted(activePlaySeconds)
        } else {
            manager.onFormulaUpdated(activePlaySeconds)
        }
        updatedUser = updatedUser.copy(badgesSerialized = manager.serialize())
        val nowMillis = NowProvider.nowMillis()
        val baseLogMessage = if (isGameStarted) {
            gameStartedLogMessage
        } else {
            formulaUpdatedLogMessage
        }
        val mergedLog = if (newBadge != null) {
            listOf(Pair("$newBadgeLogMessage\n\n$baseLogMessage", nowMillis))
        } else if (manager.countActiveGrumpyCatsOnBoard() > 0) {
            listOf(Pair("$grumpyBlockingLogMessage\n\n$baseLogMessage", nowMillis))
        } else {
            listOf(Pair(baseLogMessage, nowMillis))
        }
        return updatedUser.copy(
            pausedTimerSerialized = Optional.empty(),
            nextPenaltyTimerSerialized = Counter(user.nextPenaltyTimerSerialized).resume().serialize(),
            activePlayTimerSerialized = Counter(user.activePlayTimerSerialized).resume().serialize(),
            unseenLogsNewestFirst = trimUnseenLogs(mergedLog + user.unseenLogsNewestFirst)
        )
    }

    fun onUnseenLogsObserved(user: User, nLogsObserved: Int): User {
        if (user.unseenLogsNewestFirst.isEmpty()) {
            return user
        }
        val toMoveCount = nLogsObserved.coerceIn(0, user.unseenLogsNewestFirst.size)
        val toMove = if (toMoveCount == user.unseenLogsNewestFirst.size) {
            user.unseenLogsNewestFirst
        } else {
            user.unseenLogsNewestFirst.takeLast(toMoveCount)
        }
        val remainingUnseen = if (toMoveCount == 0) {
            user.unseenLogsNewestFirst
        } else {
            user.unseenLogsNewestFirst.dropLast(toMoveCount)
        }
        val mergedLogs = (toMove + user.oldLogsNewestFirst)
            .sortedByDescending { it.second }
            .take(MAX_OLD_LOGS)
        return user.copy(
            unseenLogsNewestFirst = remainingUnseen,
            oldLogsNewestFirst = mergedLogs
        )
    }

    private fun trimUnseenLogs(logs: List<Pair<String, Long>>): List<Pair<String, Long>> {
        return if (logs.size > MAX_UNSEEN_LOGS) logs.take(MAX_UNSEEN_LOGS) else logs
    }

    private fun trimOldLogs(logs: List<Pair<String, Long>>): List<Pair<String, Long>> {
        return if (logs.size > MAX_OLD_LOGS) logs.take(MAX_OLD_LOGS) else logs
    }

    fun evaluateAlerts(
        user: User,
        reminderMessage: String,
        penaltyMessage: String,
        grumpyCatMessage: String
    ): User {
        if (user.nextSleepEventAtMillis.isPresent &&
            user.nextSleepEventAtMillis.get() < NowProvider.nowMillis()
        ) {
            return handleAutoSleepEvent(user)
        }

        if (user.pausedTimerSerialized.isPresent) {
            return user
        }

        val penaltyThreshold = DifficultyHelper.getReviewFrequencyMillis(user.difficulty)
        val penaltyTimerStartedAtMillis =
            NowProvider.nowMillis() - Counter(user.nextPenaltyTimerSerialized).getTotalSeconds() * 1000
        val activePlaySeconds = Counter(user.activePlayTimerSerialized).getTotalSeconds()

        if (user.nextAlertType == AlertType.Reminder) {
            val nudgeThreshold = penaltyThreshold - 15 * 60 * 1000

            val nudgeThesholdAtMsecs = penaltyTimerStartedAtMillis + nudgeThreshold

            if (nudgeThesholdAtMsecs < NowProvider.nowMillis()) {
                val badgesManager = BadgesManager(user.difficulty.ordinal, user.badgesSerialized)
                val newBadge = badgesManager.onPrompt(activePlaySeconds)
                val prefix = if (newBadge == "c0") {
                    "$grumpyCatMessage\n\n"
                } else {
                    ""
                }
                return user.copy(
                    nextAlertType = AlertType.Penalty,
                    badgesSerialized = badgesManager.serialize(),
                    pendingNotificationLogsNewestFirst = listOf(
                        Pair(prefix + reminderMessage, NowProvider.nowMillis())
                    ) + user.pendingNotificationLogsNewestFirst
                )
            }
        }

        if (user.nextAlertType == AlertType.Penalty
            && (penaltyTimerStartedAtMillis + penaltyThreshold) < NowProvider.nowMillis()
        ) {
            val badgesManager = BadgesManager(user.difficulty.ordinal, user.badgesSerialized)
            val newBadge = badgesManager.onPenalty(activePlaySeconds)
            val prefix = if (newBadge == "c0") {
                "$grumpyCatMessage\n\n"
            } else {
                ""
            }
            return user.copy(
                nextAlertType = if (DifficultyHelper.hasNudge(user.difficulty)) {
                    AlertType.Reminder
                } else {
                    AlertType.Penalty
                },
                nextPenaltyTimerSerialized = Counter(null).resume().serialize(),
                badgesSerialized = badgesManager.serialize(),
                pendingNotificationLogsNewestFirst = listOf(
                    Pair(prefix + penaltyMessage, NowProvider.nowMillis())
                ) + user.pendingNotificationLogsNewestFirst
            )
        }

        return user
    }

    fun calculateNextAlertMillis(user: User): Long {
        var candidates: List<Long> = listOf()
        if (user.nextSleepEventAtMillis.isPresent) {
            candidates = candidates + user.nextSleepEventAtMillis.get()
        }
        if (user.pausedTimerSerialized.isPresent) {
            return if (candidates.size == 0) {
                NowProvider.nowMillis() + 5 * 365 * 24 * 3600 * 1000L
            } else {
                candidates.get(0)
            }
        }
        val penaltyThreshold = DifficultyHelper.getReviewFrequencyMillis(user.difficulty)
        val penaltyTimerStartedAtMillis =
            NowProvider.nowMillis() - Counter(user.nextPenaltyTimerSerialized).getTotalSeconds() * 1000

        if (user.nextAlertType == AlertType.Reminder) {
            val nudgeThreshold = penaltyThreshold - 15 * 60 * 1000

            candidates = candidates + (penaltyTimerStartedAtMillis + nudgeThreshold)
        } else {
            candidates = candidates + (penaltyTimerStartedAtMillis + penaltyThreshold)
        }

        return candidates.sorted().get(0)
    }

    fun onReviewCompleted(
        user: User,
        reviewMessage: String,
        newDiamondMessage: String,
        freezeMessage: String,
        newBadgeLogMessage: String,
        grumpyRemovedLogMessage: String,
        grumpyRemainingLogMessage: String,
        achievementsUnblockedLogMessage: String,
        grumpyBlockingLogMessage: String
    ): User {
        val resetCounter = Counter(null)
        if (user.pausedTimerSerialized.isPresent) {
            resetCounter.pause()
        } else {
            resetCounter.resume()
        }
        val nextAlertType = if (DifficultyHelper.hasNudge(user.difficulty)) {
            AlertType.Reminder
        } else {
            AlertType.Penalty
        }
        val activePlaySeconds = Counter(user.activePlayTimerSerialized).getTotalSeconds()
        val deltaSeconds  = (activePlaySeconds - user.lastRewardAtActivePlayTime).coerceAtLeast(0L)
        val isFreeze = deltaSeconds < FREEZE_WINDOW_SECONDS
        val nowMillis = NowProvider.nowMillis()

        if (isFreeze) {
            val newLogs = listOf(Pair(reviewMessage + "\n\n" + freezeMessage, nowMillis)) + user.unseenLogsNewestFirst
            return user.copy(
                nextPenaltyTimerSerialized = resetCounter.serialize(),
                nextAlertType = nextAlertType,
                unseenLogsNewestFirst = trimUnseenLogs(newLogs),
            )
        }

        val badgesManager = BadgesManager(user.difficulty.ordinal, user.badgesSerialized)
        val newBadge = badgesManager.onReview(activePlaySeconds)
        val activeGrumpyCats = badgesManager.countActiveGrumpyCatsOnBoard()
        val isNewDiamond = activeGrumpyCats == 0;
        val messages = mutableListOf<String>(reviewMessage)
        messages.add(if (newBadge == "c0_removed" && activeGrumpyCats > 0) {
                grumpyRemovedLogMessage + " " + String.format(grumpyRemainingLogMessage, activeGrumpyCats)
            } else if (newBadge == "c0_removed" && activeGrumpyCats == 0) {
                grumpyRemovedLogMessage + " " + achievementsUnblockedLogMessage
            } else if (activeGrumpyCats > 0) {
                grumpyBlockingLogMessage
            } else if (newBadge != null) {
                newBadgeLogMessage
            } else {
                ""
            })

        messages.add(if (isNewDiamond) {
                newDiamondMessage
            } else {
                ""
            })

        val filteredMessages = messages.filter { it.isNotBlank() };
        val newMessage = filteredMessages.joinToString("\n\n")

        val newLogs = listOf(Pair(newMessage, nowMillis)) + user.unseenLogsNewestFirst
        return user.copy(
            nextPenaltyTimerSerialized = resetCounter.serialize(),
            nextAlertType = nextAlertType,
            diamonds = if (isNewDiamond) user.diamonds + 1 else user.diamonds,

            // Doesn't matter if isNewDiamond or expelling the grumpy cat, because
            // "expelling" is also an action that should be prevented from abuse, therefore bump
            // lastRewardAt in any case (only if freeze then don't to avoid resetting freeze timer)
            lastRewardAtActivePlayTime = activePlaySeconds,
            unseenLogsNewestFirst = trimUnseenLogs(newLogs),
            badgesSerialized = badgesManager.serialize()
        )
    }

    private fun handleAutoSleepEvent(user: User): User {
        val nextSleepEventAtMillis = Optional.of(
            SleepUtils.calculateNextSleepEventMillisAt(
                NowProvider.nowMillis(),
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
                    Pair("💤 Time to sleep. The game is automatically paused ⏸️", NowProvider.nowMillis())
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
                    Pair("☀️ Good morning! The game is resumed ▶️", NowProvider.nowMillis())
                ) + user.pendingNotificationLogsNewestFirst
            )
        }
        return user.copy(
            nextSleepEventAtMillis = nextSleepEventAtMillis
        )
    }

    fun calculateNextDeadlineAtMillis(user: User): Long {
        return NowProvider.nowMillis() +
            DifficultyHelper.getReviewFrequencyMillis(user.difficulty) -
                Counter(user.nextPenaltyTimerSerialized).getTotalSeconds() * 1000
    }

    fun buildBoardWebViewUrl(
        baseUrl: String,
        user: User,
        langCode: String,
        env: String
    ): String {
        val activePlayTimeSecs = Counter(user.activePlayTimerSerialized).getTotalSeconds()
        val badgesManager = BadgesManager(user.difficulty.ordinal, user.badgesSerialized)
        val params = linkedMapOf(
            "lang" to langCode,
            "env" to env,
            "level" to (badgesManager.getLevel() + 1).toString(),
            "b1" to BoardSerializer.serializeBoard(badgesManager.getBoard()),
            "bp1" to BoardSerializer.serializeProgress(badgesManager.progress(activePlayTimeSecs))
        )

        val lastBadge = badgesManager.getLastBadge()
        if (lastBadge != null) {
            params["new_badge"] = lastBadge
        }

        if (badgesManager.isLevelCompleted()) {
            params["b2"] = BoardSerializer.serializeBoard(badgesManager.getNextLevelBoard())
            params["bp2"] = BoardSerializer.serializeProgress(badgesManager.newLevelEmptyProgress())
        }

        val query = params.entries.joinToString("&") { (key, value) ->
            val encoded = URLEncoder.encode(value, "UTF-8")
            "$key=$encoded"
        }

        return if (baseUrl.contains("?")) {
            "$baseUrl&$query"
        } else {
            "$baseUrl?$query"
        }
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
    private const val FREEZE_WINDOW_SECONDS = 5 * 60L
}
