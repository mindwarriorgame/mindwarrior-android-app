package com.mindwarrior.app.badges.counters

import com.mindwarrior.app.badges.BadgeProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StarBadgeCounterTest {
    private sealed class Step {
        object Penalty : Step()
        data class Progress(
            val remainingReviews: Int?,
            val progressPct: Int?,
            val receivedBadge: String?,
            val noProgress: Boolean = false
        ) : Step()
    }

    @Test
    fun testWayToS0() {
        val expectations = mutableListOf<Step>(
            Step.Progress(3, 0, null),
            Step.Progress(2, 33, null),
            Step.Penalty,
            Step.Progress(1, 66, null),
            Step.Progress(1, 66, "s0"),
            Step.Progress(null, null, null, noProgress = true)
        )
        repeat(100) { expectations.add(Step.Progress(null, null, null, noProgress = true)) }
        testRunner("s0", expectations, mutableListOf("s0", "f0"))
    }

    @Test
    fun testWayToS1() {
        val expectations = mutableListOf<Step>(
            Step.Progress(6, 0, null),
            Step.Progress(5, 16, null),
            Step.Progress(4, 33, null),
            Step.Progress(3, 50, null),
            Step.Penalty,
            Step.Progress(2, 66, null),
            Step.Progress(2, 66, null),
            Step.Progress(1, 83, "s1"),
            Step.Progress(null, null, null, noProgress = true)
        )
        repeat(100) { expectations.add(Step.Progress(null, null, null, noProgress = true)) }
        testRunner("s1", expectations, mutableListOf("s1", "f0"))
    }

    @Test
    fun testWayToS2() {
        val expectations = mutableListOf<Step>(
            Step.Progress(9, 0, null),
            Step.Progress(8, 11, null),
            Step.Progress(7, 22, null),
            Step.Progress(6, 33, null),
            Step.Penalty,
            Step.Progress(5, 44, null),
            Step.Progress(5, 44, null),
            Step.Progress(4, 55, null),
            Step.Progress(3, 66, null),
            Step.Progress(2, 77, null),
            Step.Progress(1, 88, "s2"),
            Step.Progress(null, null, null, noProgress = true)
        )
        repeat(100) { expectations.add(Step.Progress(null, null, null, noProgress = true)) }
        testRunner("s2", expectations, mutableListOf("s2", "f0"))
    }

    @Test
    fun testTwoStars() {
        val expectations = mutableListOf<Step>(
            Step.Progress(3, 0, null),
            Step.Progress(2, 33, null),
            Step.Progress(1, 66, "s0"),
            Step.Progress(3, 0, null),
            Step.Progress(2, 33, null),
            Step.Progress(1, 66, "s0"),
            Step.Progress(null, null, null, noProgress = true)
        )
        repeat(100) { expectations.add(Step.Progress(null, null, null, noProgress = true)) }
        testRunner("s0", expectations, mutableListOf("s0", "s0", "t0", "f0"))
    }

    @Test
    fun testAllStars() {
        val counter = StarBadgeCounter()
        val state = counter.onGameStarted(0, null, 1, listOf("s0", "s1", "s2", "t0", "f0")).state

        val progress = counter.progress("s0", 0, state, 1, listOf("s0", "s1", "s2", "t0", "f0"))
        assertEquals(
            BadgeProgress(
                badge = "s0",
                challenge = "review_regularly_no_penalty",
                remainingReviews = 3,
                progressPct = 0
            ),
            progress
        )
    }

    private fun testRunner(
        forBadge: String,
        expectations: List<Step>,
        boardBadges: MutableList<String>
    ) {
        val counter = StarBadgeCounter()
        var state: String? = null
        for (step in expectations) {
            when (step) {
                Step.Penalty -> {
                    val advice = counter.onPenalty(0, state, 1, boardBadges)
                    state = advice.state
                }
                is Step.Progress -> {
                    val result = counter.progress(forBadge, 0, state, 1, boardBadges)
                    if (step.noProgress) {
                        assertNull(result)
                    } else {
                        assertEquals(
                            BadgeProgress(
                                badge = forBadge,
                                challenge = "review_regularly_no_penalty",
                                remainingReviews = step.remainingReviews,
                                progressPct = step.progressPct ?: 0
                            ),
                            result
                        )
                    }
                    val advice = counter.onReview(0, state, 1, boardBadges)
                    assertEquals(step.receivedBadge, advice.badge)
                    if (advice.badge != null) {
                        boardBadges.remove(advice.badge)
                    }
                    state = advice.state
                }
            }
        }
    }
}
