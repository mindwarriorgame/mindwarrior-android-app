package com.mindwarrior.app.badges.counters

import com.mindwarrior.app.badges.BadgeAdvice
import com.mindwarrior.app.badges.BadgeProgress

class StarBadgeCounter : BadgeCounter {
    private fun frequency(difficulty: Int): Int {
        return when (difficulty) {
            0, 1 -> 3
            2, 3 -> 5
            4 -> 7
            else -> 5
        }
    }

    override fun onGameStarted(
        activePlayTimeSecs: Long,
        state: String?,
        difficulty: Int,
        badgesLockedOnBoard: List<String>
    ): BadgeAdvice {
        return when {
            badgesLockedOnBoard.contains("s0") -> BadgeAdvice(null, "0,${frequency(difficulty)}")
            badgesLockedOnBoard.contains("s1") -> BadgeAdvice(null, "0,${frequency(difficulty) * 2}")
            badgesLockedOnBoard.contains("s2") -> BadgeAdvice(null, "0,${frequency(difficulty) * 3}")
            else -> BadgeAdvice(null, "0,100000")
        }
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
        var nextState = state
        if (nextState == null) {
            nextState = onGameStarted(activePlayTimeSecs, state, difficulty, badgesLockedOnBoard).state
        }
        requireNotNull(nextState) { "should never happen, onGameStarted is called above" }

        if (nextState.contains("skip_next")) {
            return BadgeAdvice(null, nextState)
        }
        return BadgeAdvice(null, "$nextState,skip_next")
    }

    override fun onReview(
        activePlayTimeSecs: Long,
        state: String?,
        difficulty: Int,
        badgesLockedOnBoard: List<String>
    ): BadgeAdvice {
        if (!badgesLockedOnBoard.contains("s0") &&
            !badgesLockedOnBoard.contains("s1") &&
            !badgesLockedOnBoard.contains("s2")
        ) {
            return onGameStarted(activePlayTimeSecs, state, difficulty, badgesLockedOnBoard)
        }

        var nextState = state
        if (nextState == null) {
            nextState = onGameStarted(activePlayTimeSecs, state, difficulty, badgesLockedOnBoard).state
        }
        requireNotNull(nextState) { "Should never happen, onGameStarted is called above" }

        if (nextState.contains("skip_next")) {
            return BadgeAdvice(null, nextState.replace(",skip_next", ""))
        }

        val parts = nextState.split(",")
        val cnt = parts[0].toInt() + 1
        val threshold = parts[1].toInt()

        if (cnt >= threshold) {
            val giveAwayStar = findSmallestStar(badgesLockedOnBoard)
            val badgesWithoutStar = removeStarFromBoard(badgesLockedOnBoard)
            val restarted = onGameStarted(activePlayTimeSecs, null, difficulty, badgesWithoutStar)
            return BadgeAdvice(giveAwayStar, restarted.state)
        }

        return BadgeAdvice(null, "$cnt,$threshold")
    }

    private fun findSmallestStar(badgesLockedOnBoard: List<String>): String? {
        for (candidate in listOf("s0", "s1", "s2")) {
            if (badgesLockedOnBoard.contains(candidate)) {
                return candidate
            }
        }
        return null
    }

    private fun removeStarFromBoard(badgesLockedOnBoard: List<String>): List<String> {
        val newBoard = badgesLockedOnBoard.toMutableList()
        for (candidate in listOf("s0", "s1", "s2")) {
            if (newBoard.remove(candidate)) {
                return newBoard
            }
        }
        return newBoard
    }

    override fun progress(
        forBadge: String,
        activePlayTimeSecs: Long,
        state: String?,
        difficulty: Int,
        badgesLockedOnBoard: List<String>
    ): BadgeProgress? {
        if (forBadge != "s0" && forBadge != "s1" && forBadge != "s2") {
            return null
        }

        var nextState = state
        if (nextState == null) {
            nextState = onGameStarted(activePlayTimeSecs, state, difficulty, badgesLockedOnBoard).state
        }
        requireNotNull(nextState) { "should never happen, onGameStarted is called above" }

        val minOnBoard = findSmallestStar(badgesLockedOnBoard)
        if (minOnBoard != forBadge) {
            return null
        }

        val cleanState = if (nextState.contains("skip_next")) {
            nextState.replace(",skip_next", "")
        } else {
            nextState
        }
        val parts = cleanState.split(",")
        val cnt = parts[0].toInt()
        val threshold = parts[1].toInt()

        return BadgeProgress(
            badge = minOnBoard,
            challenge = "review_regularly_no_penalty",
            progressPct = (cnt * 100 / threshold).coerceAtMost(100),
            remainingReviews = threshold - cnt
        )
    }
}
