package com.mindwarrior.app.badges.counters

import com.mindwarrior.app.badges.BadgeProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimeBadgeCounterTest {
    @Test
    fun testWayToT0() {
        val counter = TimeBadgeCounter()
        var state: String? = null

        val initial = counter.progress("t0", 0, state, 2, listOf("t0", "c0"))
        assertEquals(
            BadgeProgress(
                badge = "t0",
                challenge = "play_time",
                remainingTimeSecs = 86400,
                progressPct = 0
            ),
            initial
        )

        val adviceStart = counter.onGameStarted(2000, null, 3, listOf("t0", "c0"))
        assertEquals(null, adviceStart.badge)
        assertEquals("110000", adviceStart.state)
        state = adviceStart.state

        val progressA = counter.progress("t0", 3000, state, 3, listOf("t0", "c0"))
        assertEquals(
            BadgeProgress(
                badge = "t0",
                challenge = "play_time",
                remainingTimeSecs = 107000,
                progressPct = 0
            ),
            progressA
        )

        val progressB = counter.progress("t0", 5000, state, 3, listOf("t0", "c0"))
        assertEquals(
            BadgeProgress(
                badge = "t0",
                challenge = "play_time",
                remainingTimeSecs = 105000,
                progressPct = 2
            ),
            progressB
        )

        val adviceReviewA = counter.onReview(10000, state, 3, listOf("t0", "c0"))
        assertNull(adviceReviewA.badge)
        assertEquals("110000", adviceReviewA.state)
        state = adviceReviewA.state

        val adviceReviewB = counter.onReview(200000, state, 3, listOf("t0", "c0"))
        assertEquals("t0", adviceReviewB.badge)
        assertEquals("308000", adviceReviewB.state)
        state = adviceReviewB.state

        val progressC = counter.progress("t0", 200000, state, 3, listOf("t0", "c0"))
        assertEquals(
            BadgeProgress(
                badge = "t0",
                challenge = "play_time",
                remainingTimeSecs = 108000,
                progressPct = 0
            ),
            progressC
        )
    }
}
