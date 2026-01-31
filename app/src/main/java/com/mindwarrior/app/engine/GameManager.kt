package com.mindwarrior.app.engine

import com.mindwarrior.app.LogMessageCodec
import com.mindwarrior.app.LogMessageKeys
import com.mindwarrior.app.NowProvider
import com.mindwarrior.app.badges.BadgesManager
import com.mindwarrior.app.badges.BoardSerializer
import java.util.Optional
import org.json.JSONObject
import java.net.URLEncoder

object GameManager {
    const val MAX_UNSEEN_LOGS = 10
    const val MAX_OLD_LOGS = 100
    private val SHOP_BASE_PRICES = listOf(10, 20, 30, 40, 50, 60)

    fun onDifficultyChanged(
        user: User,
        newDifficulty: Difficulty
    ): User {
        val logMessage = LogMessageCodec.encode(
            LogMessageKeys.DIFFICULTY_CHANGED,
            user.difficulty.id,
            newDifficulty.id
        )
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
        draftEndMinutes: Int
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
        val logMessage = if (draftEnabled) {
            LogMessageCodec.encode(
                LogMessageKeys.SLEEP_SCHEDULE_ENABLED,
                draftStartMinutes.toString(),
                draftEndMinutes.toString()
            )
        } else {
            LogMessageCodec.encode(LogMessageKeys.SLEEP_SCHEDULE_DISABLED)
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

    fun onPaused(user: User): User {
        if (user.pausedTimerSerialized.isPresent) {
            return user;
        }
        val nowMillis = NowProvider.nowMillis()
        return user.copy(
            pausedTimerSerialized = Optional.of(Counter(null).resume().serialize()),
            nextPenaltyTimerSerialized = Counter(user.nextPenaltyTimerSerialized).pause().serialize(),
            activePlayTimerSerialized = Counter(user.activePlayTimerSerialized).pause().serialize(),
            unseenLogsNewestFirst = trimUnseenLogs(
                listOf(Pair(LogMessageCodec.encode(LogMessageKeys.GAME_PAUSED), nowMillis)) +
                    user.unseenLogsNewestFirst
            )
        )
    }

    fun onResume(user: User): User {
        if (!user.pausedTimerSerialized.isPresent) {
            return user
        }
        val nowMillis = NowProvider.nowMillis()
        val pauseIntervalHistory = addEntityToPausedInterval(user, nowMillis)
        return user.copy(
            pausedTimerSerialized = Optional.empty(),
            nextPenaltyTimerSerialized = Counter(user.nextPenaltyTimerSerialized).resume().serialize(),
            activePlayTimerSerialized = Counter(user.activePlayTimerSerialized).resume().serialize(),
            unseenLogsNewestFirst = trimUnseenLogs(
                listOf(Pair(LogMessageCodec.encode(LogMessageKeys.GAME_RESUMED), nowMillis)) +
                    user.unseenLogsNewestFirst
            ),
            pauseIntervalHistory = pauseIntervalHistory
        )
    }

    fun onLocalStorageUpdated(
        user: User,
        localStorate: Optional<String>,
        triggeredByFormulaEditor: Boolean
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
            LogMessageCodec.encode(LogMessageKeys.GAME_STARTED)
        } else {
            LogMessageCodec.encode(LogMessageKeys.FORMULA_UPDATED)
        }
        val mergedLog = if (newBadge != null) {
            listOf(
                Pair(
                    LogMessageCodec.encode(LogMessageKeys.NEW_BADGE) + "\n\n" + baseLogMessage,
                    nowMillis
                )
            )
        } else if (manager.countActiveGrumpyCatsOnBoard() > 0) {
            listOf(
                Pair(
                    LogMessageCodec.encode(LogMessageKeys.GRUMPY_BLOCKING) +
                        "\n\n" +
                        baseLogMessage,
                    nowMillis
                )
            )
        } else {
            listOf(Pair(baseLogMessage, nowMillis))
        }
        updatedUser = updatedUser.copy(unseenLogsNewestFirst = trimUnseenLogs(mergedLog + user.unseenLogsNewestFirst))
        if (isGameStarted) {
            updatedUser = updatedUser.copy(
                pausedTimerSerialized = Optional.empty(),
                nextPenaltyTimerSerialized = Counter(user.nextPenaltyTimerSerialized).resume()
                    .serialize(),
                activePlayTimerSerialized = Counter(user.activePlayTimerSerialized).resume()
                    .serialize()
            )
        }
        return updatedUser
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

    fun onUnseenLogsObserved(user: User, observedLogs: List<Pair<String, Long>>): User {
        if (user.unseenLogsNewestFirst.isEmpty() || observedLogs.isEmpty()) {
            return user
        }

        val remainingObserved = observedLogs.toMutableList()
        val toMove = mutableListOf<Pair<String, Long>>()
        val remainingUnseen = mutableListOf<Pair<String, Long>>()

        for (entry in user.unseenLogsNewestFirst) {
            val idx = remainingObserved.indexOf(entry)
            if (idx >= 0) {
                remainingObserved.removeAt(idx)
                toMove.add(entry)
            } else {
                remainingUnseen.add(entry)
            }
        }

        if (toMove.isEmpty()) {
            return user
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
        user: User
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
                    LogMessageCodec.encode(LogMessageKeys.GRUMPY_SNEAKED_IN) + "\n\n"
                } else {
                    ""
                }
                return user.copy(
                    nextAlertType = AlertType.Penalty,
                    badgesSerialized = badgesManager.serialize(),
                    pendingNotificationLogsNewestFirst = listOf(
                        Pair(
                            prefix + LogMessageCodec.encode(LogMessageKeys.PROMPT_REMINDER),
                            NowProvider.nowMillis()
                        )
                    ) + user.pendingNotificationLogsNewestFirst
                )
            }
        }

        if (user.nextAlertType == AlertType.Penalty
            && (penaltyTimerStartedAtMillis + penaltyThreshold) < NowProvider.nowMillis()
        ) {
            var updatedUser = user

            val nowMillis = NowProvider.nowMillis()
            var badgesManager = BadgesManager(user.difficulty.ordinal, user.badgesSerialized)

            var newBadge = badgesManager.onPenalty(activePlaySeconds)

            if (newBadge == "c0" && user.hasRepeller) {
                badgesManager = BadgesManager(user.difficulty.ordinal, user.badgesSerialized)
                updatedUser = user.copy(
                    hasRepeller = false
                )
                newBadge = "repeller"
            }
            val prefix = if (newBadge == "c0") {
                LogMessageCodec.encode(LogMessageKeys.GRUMPY_SNEAKED_IN) + "\n\n"
            } else if (newBadge == "repeller") {
                LogMessageCodec.encode(LogMessageKeys.REPELLER_USED) + "\n\n"
            } else {
                ""
            }
            val nextUser = updatedUser.copy(
                nextAlertType = if (DifficultyHelper.hasNudge(user.difficulty)) {
                    AlertType.Reminder
                } else {
                    AlertType.Penalty
                },
                nextPenaltyTimerSerialized = Counter(null).resume().serialize(),
                badgesSerialized = badgesManager.serialize(),
                pendingNotificationLogsNewestFirst = listOf(
                    Pair(prefix + LogMessageCodec.encode(LogMessageKeys.PROMPT_PENALTY), nowMillis)
                ) + updatedUser.pendingNotificationLogsNewestFirst
            )
            return if (newBadge == "repeller") {
                onPaused(nextUser)
            } else {
                nextUser
            }
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
        reviewHours: Int,
        reviewMinutes: Int
    ): User {
        val wasPaused = user.pausedTimerSerialized.isPresent
        val resetCounter = Counter(null).resume()
        val nextAlertType = if (DifficultyHelper.hasNudge(user.difficulty)) {
            AlertType.Reminder
        } else {
            AlertType.Penalty
        }
        val activePlaySeconds = Counter(user.activePlayTimerSerialized).getTotalSeconds()
        val deltaSeconds  = (activePlaySeconds - user.lastRewardAtActivePlayTime).coerceAtLeast(0L)
        val isFreeze = deltaSeconds < FREEZE_WINDOW_SECONDS
        val nowMillis = NowProvider.nowMillis()
        val activePlayMillis =
            Counter(user.activePlayTimerSerialized).getTotalSeconds() * 1000L
        val reviewAtMillisActivePlayTimeHistory =
            addReviewDurationHistory(user, nowMillis, activePlayMillis)

        val updatedPausedTimerSerialized = if (wasPaused) Optional.empty() else user.pausedTimerSerialized
        val updatedActivePlayTimerSerialized = if (wasPaused) {
            Counter(user.activePlayTimerSerialized).resume().serialize()
        } else {
            user.activePlayTimerSerialized
        }
        val updatedNextPenaltyTimerSerialized = resetCounter.serialize()
        val pauseIntervalHistory = if (wasPaused) {
            addEntityToPausedInterval(user, nowMillis)
        } else {
            user.pauseIntervalHistory
        }

        if (isFreeze) {
            val baseMessage = LogMessageCodec.encode(
                LogMessageKeys.REVIEW_COMPLETED,
                reviewHours.toString(),
                reviewMinutes.toString()
            ) + "\n\n" + LogMessageCodec.encode(LogMessageKeys.REVIEW_NO_REWARD)
            val newMessage = if (wasPaused) {
                baseMessage + "\n\n" + LogMessageCodec.encode(LogMessageKeys.GAME_RESUMED)
            } else {
                baseMessage
            }
            val newLogs = listOf(Pair(newMessage, nowMillis)) + user.unseenLogsNewestFirst
            return user.copy(
                pausedTimerSerialized = updatedPausedTimerSerialized,
                activePlayTimerSerialized = updatedActivePlayTimerSerialized,
                nextPenaltyTimerSerialized = updatedNextPenaltyTimerSerialized,
                nextAlertType = nextAlertType,
                unseenLogsNewestFirst = trimUnseenLogs(newLogs),
                reviewAtMillisActivePlayTimeHistory = reviewAtMillisActivePlayTimeHistory,
                pauseIntervalHistory = pauseIntervalHistory
            )
        }

        val badgesManager = BadgesManager(user.difficulty.ordinal, user.badgesSerialized)
        val newBadge = badgesManager.onReview(activePlaySeconds)
        val activeGrumpyCats = badgesManager.countActiveGrumpyCatsOnBoard()
        val isNewDiamond = activeGrumpyCats == 0;
        val messages = mutableListOf<String>(
            LogMessageCodec.encode(
                LogMessageKeys.REVIEW_COMPLETED,
                reviewHours.toString(),
                reviewMinutes.toString()
            )
        )
        when {
            newBadge == "c0_removed" && activeGrumpyCats > 0 ->
                messages.add(
                    LogMessageCodec.encode(
                        LogMessageKeys.GRUMPY_REMOVED_WITH_REMAINING,
                        activeGrumpyCats.toString()
                    )
                )
            newBadge == "c0_removed" && activeGrumpyCats == 0 ->
                messages.add(LogMessageCodec.encode(LogMessageKeys.GRUMPY_REMOVED_WITH_UNBLOCKED))
            activeGrumpyCats > 0 ->
                messages.add(LogMessageCodec.encode(LogMessageKeys.GRUMPY_BLOCKING))
            newBadge != null ->
                messages.add(LogMessageCodec.encode(LogMessageKeys.NEW_BADGE))
        }
        if (isNewDiamond) {
            messages.add(LogMessageCodec.encode(LogMessageKeys.REVIEW_REWARD))
        }

        val baseMessage = messages.joinToString("\n\n")
        val newMessage = if (wasPaused) {
            baseMessage + "\n\n" + LogMessageCodec.encode(LogMessageKeys.GAME_RESUMED)
        } else {
            baseMessage
        }

        val newLogs = listOf(Pair(newMessage, nowMillis)) + user.unseenLogsNewestFirst
        return user.copy(
            pausedTimerSerialized = updatedPausedTimerSerialized,
            activePlayTimerSerialized = updatedActivePlayTimerSerialized,
            nextPenaltyTimerSerialized = updatedNextPenaltyTimerSerialized,
            nextAlertType = nextAlertType,
            diamonds = if (isNewDiamond) user.diamonds + 1 else user.diamonds,

            // Doesn't matter if isNewDiamond or expelling the grumpy cat, because
            // "expelling" is also an action that should be prevented from abuse, therefore bump
            // lastRewardAt in any case (only if freeze then don't to avoid resetting freeze timer)
            lastRewardAtActivePlayTime = activePlaySeconds,
            unseenLogsNewestFirst = trimUnseenLogs(newLogs),
            badgesSerialized = badgesManager.serialize(),
            reviewAtMillisActivePlayTimeHistory = reviewAtMillisActivePlayTimeHistory,
            pauseIntervalHistory = pauseIntervalHistory
        )
    }

    fun onShooGrumpyCat(
        user: User
    ): User {
        val activePlaySeconds = Counter(user.activePlayTimerSerialized).getTotalSeconds()
        val badgesManager = BadgesManager(user.difficulty.ordinal, user.badgesSerialized)
        badgesManager.onShooCat(activePlaySeconds)
        val nowMillis = NowProvider.nowMillis()
        val updated = user.copy(
            badgesSerialized = badgesManager.serialize(),
            unseenLogsNewestFirst = trimUnseenLogs(
                listOf(
                    Pair(LogMessageCodec.encode(LogMessageKeys.GRUMPY_REMOVED), nowMillis)
                ) + user.unseenLogsNewestFirst
            )
        )
        return applyShopPurchase(updated, shopBasePriceFor(user))
    }

    fun onForceNextAchievement(
        user: User
    ): User {
        val activePlaySeconds = Counter(user.activePlayTimerSerialized).getTotalSeconds()
        val badgesManager = BadgesManager(user.difficulty.ordinal, user.badgesSerialized)
        badgesManager.onForceBadgeOpen(activePlaySeconds)
        val nowMillis = NowProvider.nowMillis()
        val updated = user.copy(
            badgesSerialized = badgesManager.serialize(),
            unseenLogsNewestFirst = trimUnseenLogs(
                listOf(Pair(LogMessageCodec.encode(LogMessageKeys.NEW_BADGE), nowMillis)) +
                    user.unseenLogsNewestFirst
            )
        )
        return applyShopPurchase(updated, shopBasePriceFor(user))
    }

    fun onBuyRepeller(user: User): User {
        if (user.hasRepeller) {
            return user
        }
        val updated = user.copy(hasRepeller = true)
        return applyShopPurchase(updated, shopRepellerPriceFor(user))
    }

    private fun applyShopPurchase(user: User, price: Int): User {
        val charge = price.coerceAtLeast(0)
        return user.copy(
            diamonds = (user.diamonds - charge).coerceAtLeast(0),
            diamondsSpent = user.diamondsSpent + charge
        )
    }

    private fun shopBasePriceFor(user: User): Int {
        return SHOP_BASE_PRICES.getOrElse(user.difficulty.ordinal) {
            SHOP_BASE_PRICES.last()
        }
    }

    private fun shopRepellerPriceFor(user: User): Int {
        return (shopBasePriceFor(user) * 1.5f).toInt()
    }

    private fun addEntityToPausedInterval(user: User, nowMillis: Long): List<Pair<Long, Long>> {
        val pausedTimerSerialized = user.pausedTimerSerialized
        if (!pausedTimerSerialized.isPresent) {
            return user.pauseIntervalHistory
        }
        val elapsedSeconds = Counter(pausedTimerSerialized.get()).getTotalSeconds()
        val startMillis = nowMillis - elapsedSeconds * 1000L
        val cutoffMillis = nowMillis - WEEK_MILLIS
        val trimmed = user.pauseIntervalHistory.filter { it.second >= cutoffMillis }
        return trimmed + Pair(startMillis, nowMillis)
    }

    private fun addReviewDurationHistory(
        user: User,
        nowMillis: Long,
        durationMillis: Long
    ): List<Pair<Long, Long>> {
        val cutoffMillis = nowMillis - WEEK_MILLIS
        val trimmed = user.reviewAtMillisActivePlayTimeHistory.filter { it.first >= cutoffMillis }
        return trimmed + Pair(nowMillis, durationMillis)
    }

    private const val WEEK_MILLIS = 7L * 24 * 60 * 60 * 1000

    private fun handleAutoSleepEvent(
        user: User
    ): User {
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
                    Pair(
                        LogMessageCodec.encode(LogMessageKeys.SLEEP_STARTED),
                        NowProvider.nowMillis()
                    )
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
                    Pair(
                        LogMessageCodec.encode(LogMessageKeys.SLEEP_RESUMED),
                        NowProvider.nowMillis()
                    )
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
