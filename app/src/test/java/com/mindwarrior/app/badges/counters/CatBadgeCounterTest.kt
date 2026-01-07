package com.mindwarrior.app.badges.counters

import com.mindwarrior.app.badges.BadgeProgress
import org.junit.Assert.assertEquals
import org.junit.Test

class CatBadgeCounterTest {
    private data class Expectation(
        val progressAt: Long? = null,
        val reviewAt: Long? = null,
        val penaltyAt: Long? = null,
        val promptAt: Long? = null,
        val expectedBadge: String? = null,
        val expectedProgressPct: Int? = null,
        val expectedRemainingTimeSecs: Long? = null,
        val expectedChallenge: String? = null
    )

    @Test
    fun testWayToC1() {
        val expectations = listOf(
            Expectation(progressAt = 0, expectedProgressPct = 0, expectedRemainingTimeSecs = 57600, expectedChallenge = "review_regularly_no_penalty"),
            Expectation(reviewAt = 0, expectedBadge = null),
            Expectation(progressAt = 0, expectedProgressPct = 0, expectedRemainingTimeSecs = 57600, expectedChallenge = "review_regularly_no_penalty"),
            Expectation(reviewAt = 1000, expectedBadge = null),
            Expectation(progressAt = 1000, expectedProgressPct = 2, expectedRemainingTimeSecs = 56600, expectedChallenge = "review_regularly_no_penalty"),
            Expectation(reviewAt = 5000, expectedBadge = null),
            Expectation(progressAt = 5000, expectedProgressPct = 9, expectedRemainingTimeSecs = 52600, expectedChallenge = "review_regularly_no_penalty"),
            Expectation(promptAt = 5500, expectedBadge = null),
            Expectation(penaltyAt = 60000, expectedBadge = "c0"),
            Expectation(progressAt = 61000, expectedProgressPct = 9, expectedRemainingTimeSecs = 52600, expectedChallenge = "review_regularly_no_penalty"),
            Expectation(progressAt = 200000, expectedProgressPct = 9, expectedRemainingTimeSecs = 52600, expectedChallenge = "review_regularly_no_penalty"),
            Expectation(reviewAt = 500000, expectedBadge = null),
            Expectation(reviewAt = 600000, expectedBadge = "c1"),
            Expectation(reviewAt = 601000, expectedBadge = null),
            Expectation(progressAt = 601000, expectedProgressPct = 2, expectedRemainingTimeSecs = 56600, expectedChallenge = "review_regularly_no_penalty")
        )
        testRunner("c1", expectations, 2, listOf("c1", "c2", "c0", "t0"))
    }

    @Test
    fun testNoC0OnBeginner() {
        val expectations = listOf(
            Expectation(progressAt = 0, expectedProgressPct = 0, expectedRemainingTimeSecs = 28800, expectedChallenge = "review_regularly_no_penalty"),
            Expectation(reviewAt = 0, expectedBadge = null),
            Expectation(progressAt = 0, expectedProgressPct = 0, expectedRemainingTimeSecs = 28800, expectedChallenge = "review_regularly_no_penalty"),
            Expectation(penaltyAt = 60000, expectedBadge = null),
            Expectation(progressAt = 61000, expectedProgressPct = 0, expectedRemainingTimeSecs = 28800, expectedChallenge = "review_regularly_no_penalty"),
            Expectation(reviewAt = 62000, expectedBadge = null),
            Expectation(reviewAt = 63000, expectedBadge = null),
            Expectation(progressAt = 64000, expectedProgressPct = 4, expectedRemainingTimeSecs = 27800, expectedChallenge = "review_regularly_no_penalty")
        )
        testRunner("c1", expectations, 0, listOf("c1", "c2", "c0", "t0"))
    }

    @Test
    fun testWayToC2() {
        val expectations = listOf(
            Expectation(progressAt = 0, expectedProgressPct = 0, expectedRemainingTimeSecs = 72000, expectedChallenge = "review_regularly_no_prompt"),
            Expectation(reviewAt = 0, expectedBadge = null),
            Expectation(progressAt = 0, expectedProgressPct = 0, expectedRemainingTimeSecs = 72000, expectedChallenge = "review_regularly_no_prompt"),
            Expectation(reviewAt = 1000, expectedBadge = null),
            Expectation(progressAt = 1000, expectedProgressPct = 2, expectedRemainingTimeSecs = 71000, expectedChallenge = "review_regularly_no_prompt"),
            Expectation(reviewAt = 2000, expectedBadge = null),
            Expectation(progressAt = 2000, expectedProgressPct = 3, expectedRemainingTimeSecs = 70000, expectedChallenge = "review_regularly_no_prompt"),
            Expectation(promptAt = 5500, expectedBadge = null),
            Expectation(progressAt = 7100, expectedProgressPct = 3, expectedRemainingTimeSecs = 70000, expectedChallenge = "review_regularly_no_prompt"),
            Expectation(reviewAt = 7200, expectedBadge = null),
            Expectation(progressAt = 7200, expectedProgressPct = 3, expectedRemainingTimeSecs = 70000, expectedChallenge = "review_regularly_no_prompt"),
            Expectation(penaltyAt = 7400, expectedBadge = "c0"),
            Expectation(progressAt = 7400, expectedProgressPct = 3, expectedRemainingTimeSecs = 70000, expectedChallenge = "review_regularly_no_prompt"),
            Expectation(reviewAt = 100000, expectedBadge = null),
            Expectation(progressAt = 7400, expectedProgressPct = 3, expectedRemainingTimeSecs = 70000, expectedChallenge = "review_regularly_no_prompt"),
            Expectation(reviewAt = 180000, expectedBadge = "c2"),
            Expectation(progressAt = 180000, expectedProgressPct = 0, expectedRemainingTimeSecs = 72000, expectedChallenge = "review_regularly_no_prompt")
        )
        testRunner("c2", expectations, 3, listOf("c2", "s0", "c0", "t0"))
    }

    private fun testRunner(
        forBadge: String,
        expectations: List<Expectation>,
        difficulty: Int,
        boardLockedItems: List<String>
    ) {
        val counter = CatBadgeCounter()
        var state: String? = null
        for (exp in expectations) {
            when {
                exp.penaltyAt != null -> {
                    val advice = counter.onPenalty(exp.penaltyAt, state, difficulty, boardLockedItems)
                    assertEquals(exp.expectedBadge, advice.badge)
                    state = advice.state
                }
                exp.reviewAt != null -> {
                    val advice = counter.onReview(exp.reviewAt, state, difficulty, boardLockedItems)
                    assertEquals(exp.expectedBadge, advice.badge)
                    state = advice.state
                }
                exp.promptAt != null -> {
                    val advice = counter.onPrompt(exp.promptAt, state, difficulty, boardLockedItems)
                    assertEquals(exp.expectedBadge, advice.badge)
                    state = advice.state
                }
                else -> {
                    val result = counter.progress(
                        forBadge,
                        exp.progressAt ?: 0,
                        state,
                        difficulty,
                        boardLockedItems
                    )
                    assertEquals(
                        BadgeProgress(
                            badge = forBadge,
                            challenge = exp.expectedChallenge ?: "",
                            remainingTimeSecs = exp.expectedRemainingTimeSecs,
                            progressPct = exp.expectedProgressPct ?: 0
                        ),
                        result
                    )
                }
            }
        }
    }
}
