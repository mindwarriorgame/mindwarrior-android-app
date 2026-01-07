package com.mindwarrior.app.badges.counters

import com.mindwarrior.app.badges.BadgeAdvice
import com.mindwarrior.app.badges.BadgeProgress
import kotlin.math.roundToInt

class CatBadgeCounter : BadgeCounter {
    private fun calculateIntervalSecs(difficulty: Int): Long {
        val koef = listOf(0.5, 0.75, 1.0, 1.25, 1.5)
        return (INTERVAL_SECS * koef[difficulty]).roundToInt().toLong()
    }

    override fun onGameStarted(
        activePlayTimeSecs: Long,
        state: String?,
        difficulty: Int,
        badgesLockedOnBoard: List<String>
    ): BadgeAdvice {
        return BadgeAdvice(null, generateState(0, activePlayTimeSecs, "game_started"))
    }

    private fun getCumulativeCounterSecs(state: String): Long {
        return state.split(",")[0].split("=")[1].toDouble().toLong()
    }

    private fun getCounterLastUpdated(state: String): Long {
        return state.split(",")[1].split("=")[1].toDouble().toLong()
    }

    private fun getUpdateReason(state: String): String {
        return state.split(",")[2].split("=")[1]
    }

    private fun generateState(
        cumulativeCounterSecs: Long,
        counterLastUpdated: Long,
        updateReason: String
    ): String {
        return "cumulative_counter_secs=$cumulativeCounterSecs," +
            "counter_last_updated=$counterLastUpdated," +
            "update_reason=$updateReason"
    }

    override fun onFormulaUpdated(
        activePlayTimeSecs: Long,
        state: String?,
        difficulty: Int,
        badgesLockedOnBoard: List<String>
    ): BadgeAdvice {
        return BadgeAdvice(null, state)
    }

    override fun onPrompt(
        activePlayTimeSecs: Long,
        state: String?,
        difficulty: Int,
        badgesLockedOnBoard: List<String>
    ): BadgeAdvice {
        var nextState = state
        if (nextState == null) {
            nextState = onGameStarted(activePlayTimeSecs, state, difficulty, badgesLockedOnBoard).state
        }
        requireNotNull(nextState) { "should never happen, for typecasting" }

        if (badgesLockedOnBoard.contains("c1")) {
            return BadgeAdvice(null, nextState)
        }

        if (badgesLockedOnBoard.contains("c2")) {
            return BadgeAdvice(
                null,
                generateState(getCumulativeCounterSecs(nextState), activePlayTimeSecs, "prompt")
            )
        }

        return BadgeAdvice(null, nextState)
    }

    override fun onPenalty(
        activePlayTimeSecs: Long,
        state: String?,
        difficulty: Int,
        badgesLockedOnBoard: List<String>
    ): BadgeAdvice {
        var nextState = state
        if (nextState == null) {
            nextState = onGameStarted(activePlayTimeSecs, state, difficulty, badgesLockedOnBoard).state
        }
        requireNotNull(nextState) { "should never happen, for typecasting" }

        val badge = if (difficulty >= 1) "c0" else null
        return BadgeAdvice(
            badge,
            generateState(getCumulativeCounterSecs(nextState), activePlayTimeSecs, "penalty")
        )
    }

    override fun onReview(
        activePlayTimeSecs: Long,
        state: String?,
        difficulty: Int,
        badgesLockedOnBoard: List<String>
    ): BadgeAdvice {
        if (state == null) {
            return onGameStarted(activePlayTimeSecs, state, difficulty, badgesLockedOnBoard)
        }

        var cumulativeCounterSecs = getCumulativeCounterSecs(state)
        val counterLastUpdated = getCounterLastUpdated(state)
        val lastUpdateReason = getUpdateReason(state)

        if (badgesLockedOnBoard.contains("c1")) {
            if (lastUpdateReason == "penalty") {
                return BadgeAdvice(
                    null,
                    generateState(cumulativeCounterSecs, activePlayTimeSecs, "review")
                )
            }

            cumulativeCounterSecs += activePlayTimeSecs - counterLastUpdated
            if (cumulativeCounterSecs >= calculateIntervalSecs(difficulty)) {
                return BadgeAdvice(
                    "c1",
                    generateState(0, activePlayTimeSecs, "review")
                )
            }

            return BadgeAdvice(
                null,
                generateState(cumulativeCounterSecs, activePlayTimeSecs, "review")
            )
        }

        if (badgesLockedOnBoard.contains("c2")) {
            if (lastUpdateReason == "prompt" || lastUpdateReason == "penalty") {
                return BadgeAdvice(
                    null,
                    generateState(cumulativeCounterSecs, activePlayTimeSecs, "review")
                )
            }

            cumulativeCounterSecs += activePlayTimeSecs - counterLastUpdated
            if (cumulativeCounterSecs >= calculateIntervalSecs(difficulty)) {
                return BadgeAdvice(
                    "c2",
                    generateState(0, activePlayTimeSecs, "review")
                )
            }

            return BadgeAdvice(
                null,
                generateState(cumulativeCounterSecs, activePlayTimeSecs, "review")
            )
        }

        return BadgeAdvice(null, state)
    }

    override fun progress(
        forBadge: String,
        activePlayTimeSecs: Long,
        state: String?,
        difficulty: Int,
        badgesLockedOnBoard: List<String>
    ): BadgeProgress? {
        if (forBadge != "c1" && forBadge != "c2") {
            return null
        }

        var nextState = state
        if (nextState == null) {
            nextState = onGameStarted(activePlayTimeSecs, state, difficulty, badgesLockedOnBoard).state
        }
        requireNotNull(nextState) { "should never happen (called onGameStarted above), for typecasting" }

        val cumulativeCounterSecs = getCumulativeCounterSecs(nextState)

        if (badgesLockedOnBoard.contains("c1")) {
            val remainingTimeSecs =
                (calculateIntervalSecs(difficulty) - cumulativeCounterSecs).coerceAtLeast(0L)
            val progressPct =
                100 - (100 * remainingTimeSecs / calculateIntervalSecs(difficulty)).toInt()

            if (forBadge == "c1") {
                return BadgeProgress(
                    badge = "c1",
                    challenge = "review_regularly_no_penalty",
                    progressPct = progressPct,
                    remainingTimeSecs = remainingTimeSecs
                )
            }
            return null
        }

        if (badgesLockedOnBoard.contains("c2")) {
            val remainingTimeSecs =
                (calculateIntervalSecs(difficulty) - cumulativeCounterSecs).coerceAtLeast(0L)
            val progressPct =
                100 - (100 * remainingTimeSecs / calculateIntervalSecs(difficulty)).toInt()

            if (forBadge == "c2") {
                return BadgeProgress(
                    badge = "c2",
                    challenge = "review_regularly_no_prompt",
                    progressPct = progressPct,
                    remainingTimeSecs = remainingTimeSecs
                )
            }
            return null
        }

        return null
    }

    companion object {
        private const val INTERVAL_SECS = 16 * 3600L
    }
}
