package com.mindwarrior.app.badges.counters

import com.mindwarrior.app.badges.BadgeAdvice
import com.mindwarrior.app.badges.BadgeProgress
import kotlin.math.roundToInt

class FeatherBadgeCounter : BadgeCounter {
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
        val nextAt = (activePlayTimeSecs + calculateIntervalSecs(difficulty)).toString()
        return if (badgesLockedOnBoard.contains("f0")) {
            BadgeAdvice("f0", nextAt)
        } else {
            BadgeAdvice(null, nextAt)
        }
    }

    override fun onFormulaUpdated(
        activePlayTimeSecs: Long,
        state: String?,
        difficulty: Int,
        badgesLockedOnBoard: List<String>
    ): BadgeAdvice {
        if (!badgesLockedOnBoard.contains("f0")) {
            return BadgeAdvice(null, state)
        }

        if (state == null) {
            return BadgeAdvice(null, (activePlayTimeSecs + calculateIntervalSecs(difficulty)).toString())
        }

        val timeToFire = state.toDouble().toLong()
        if (timeToFire < activePlayTimeSecs) {
            return BadgeAdvice("f0", (activePlayTimeSecs + calculateIntervalSecs(difficulty)).toString())
        }

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
        if (state == null && badgesLockedOnBoard.contains("f0")) {
            return BadgeAdvice(null, onGameStarted(activePlayTimeSecs, state, difficulty, badgesLockedOnBoard).state)
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
        if (forBadge != "f0") {
            return null
        }
        if (!badgesLockedOnBoard.contains("f0")) {
            return null
        }

        val effectiveState = state
            ?: (activePlayTimeSecs + calculateIntervalSecs(difficulty)).toString()

        val timeToFire = effectiveState.toDouble().toLong()
        val timeLeft = (timeToFire - activePlayTimeSecs).coerceAtLeast(0L)
        val timePassed = (calculateIntervalSecs(difficulty) - timeLeft).coerceAtLeast(0L)
        val progressPct = (100 * timePassed / calculateIntervalSecs(difficulty)).coerceAtMost(100L)

        return BadgeProgress(
            badge = "f0",
            challenge = "update_formula",
            progressPct = progressPct.toInt(),
            remainingTimeSecs = timeLeft
        )
    }

    companion object {
        private const val INTERVAL_SECS = 24 * 3600L
    }
}
