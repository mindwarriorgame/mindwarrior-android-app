package com.mindwarrior.app.badges.counters

import com.mindwarrior.app.badges.BadgeAdvice
import com.mindwarrior.app.badges.BadgeProgress
import kotlin.math.roundToInt

class TimeBadgeCounter : BadgeCounter {
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
        return BadgeAdvice(null, (activePlayTimeSecs + calculateIntervalSecs(difficulty)).toString())
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
        return BadgeAdvice(null, state)
    }

    override fun onPenalty(
        activePlayTimeSecs: Long,
        state: String?,
        difficulty: Int,
        badgesLockedOnBoard: List<String>
    ): BadgeAdvice {
        return BadgeAdvice(null, state)
    }

    override fun onReview(
        activePlayTimeSecs: Long,
        state: String?,
        difficulty: Int,
        badgesLockedOnBoard: List<String>
    ): BadgeAdvice {
        if (!badgesLockedOnBoard.contains("t0")) {
            return BadgeAdvice(null, state)
        }

        if (state == null) {
            return BadgeAdvice(null, (activePlayTimeSecs + calculateIntervalSecs(difficulty)).toString())
        }

        val pendingAt = state.toDouble().toLong()
        if (pendingAt < activePlayTimeSecs) {
            return BadgeAdvice("t0", (activePlayTimeSecs + calculateIntervalSecs(difficulty)).toString())
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
        if (forBadge != "t0") {
            return null
        }
        if (!badgesLockedOnBoard.contains("t0")) {
            return null
        }

        val effectiveState = state
            ?: (activePlayTimeSecs + calculateIntervalSecs(difficulty)).toString()

        val pendingAt = effectiveState.toDouble().toLong()
        val timeLeft = (pendingAt - activePlayTimeSecs).coerceAtLeast(0L)
        val timePassed = (calculateIntervalSecs(difficulty) - timeLeft).coerceAtLeast(0L)
        val progressPct = (100 * timePassed / calculateIntervalSecs(difficulty)).coerceAtMost(100L)

        return BadgeProgress(
            badge = "t0",
            challenge = "play_time",
            progressPct = progressPct.toInt(),
            remainingTimeSecs = timeLeft
        )
    }

    companion object {
        private const val INTERVAL_SECS = 24 * 3600L
    }
}
