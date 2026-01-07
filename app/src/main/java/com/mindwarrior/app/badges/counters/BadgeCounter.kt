package com.mindwarrior.app.badges.counters

import com.mindwarrior.app.badges.BadgeAdvice
import com.mindwarrior.app.badges.BadgeProgress

interface BadgeCounter {
    fun onGameStarted(
        activePlayTimeSecs: Long,
        state: String?,
        difficulty: Int,
        badgesLockedOnBoard: List<String>
    ): BadgeAdvice

    fun onFormulaUpdated(
        activePlayTimeSecs: Long,
        state: String?,
        difficulty: Int,
        badgesLockedOnBoard: List<String>
    ): BadgeAdvice

    fun onPrompt(
        activePlayTimeSecs: Long,
        state: String?,
        difficulty: Int,
        badgesLockedOnBoard: List<String>
    ): BadgeAdvice

    fun onPenalty(
        activePlayTimeSecs: Long,
        state: String?,
        difficulty: Int,
        badgesLockedOnBoard: List<String>
    ): BadgeAdvice

    fun onReview(
        activePlayTimeSecs: Long,
        state: String?,
        difficulty: Int,
        badgesLockedOnBoard: List<String>
    ): BadgeAdvice

    fun progress(
        forBadge: String,
        activePlayTimeSecs: Long,
        state: String?,
        difficulty: Int,
        badgesLockedOnBoard: List<String>
    ): BadgeProgress?
}
