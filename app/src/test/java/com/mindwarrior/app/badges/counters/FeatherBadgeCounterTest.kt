package com.mindwarrior.app.badges.counters

import com.mindwarrior.app.badges.BadgeProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FeatherBadgeCounterTest {
    @Test
    fun testWayToF0() {
        val counter = FeatherBadgeCounter()

        val initial = counter.progress("f0", 0, null, 3, listOf("f0", "s0"))
        assertEquals(
            BadgeProgress(
                badge = "f0",
                challenge = "update_formula",
                remainingTimeSecs = 108000,
                progressPct = 0
            ),
            initial
        )

        val adviceStart = counter.onGameStarted(0, null, 3, listOf("f0", "s0"))
        assertEquals("f0", adviceStart.badge)
        assertEquals("108000", adviceStart.state)
        var state = adviceStart.state

        val progressA = counter.progress("f0", 0, state, 3, listOf("f0", "s0"))
        assertEquals(
            BadgeProgress(
                badge = "f0",
                challenge = "update_formula",
                remainingTimeSecs = 108000,
                progressPct = 0
            ),
            progressA
        )

        val progressB = counter.progress("f0", 3000, state, 3, listOf("f0", "s0"))
        assertEquals(
            BadgeProgress(
                badge = "f0",
                challenge = "update_formula",
                remainingTimeSecs = 105000,
                progressPct = 2
            ),
            progressB
        )

        val adviceUpdateA = counter.onFormulaUpdated(50000, state, 3, listOf("f0", "s0"))
        assertNull(adviceUpdateA.badge)
        assertEquals("108000", adviceUpdateA.state)
        state = adviceUpdateA.state

        val progressC = counter.progress("f0", 108000, state, 3, listOf("f0", "s0"))
        assertEquals(
            BadgeProgress(
                badge = "f0",
                challenge = "update_formula",
                remainingTimeSecs = 0,
                progressPct = 100
            ),
            progressC
        )

        val adviceUpdateB = counter.onFormulaUpdated(108001, state, 3, listOf("f0", "s0"))
        assertEquals("f0", adviceUpdateB.badge)
        assertEquals("216001", adviceUpdateB.state)
        state = adviceUpdateB.state

        val progressD = counter.progress("f0", 108001, state, 3, listOf("f0", "s0"))
        assertEquals(
            BadgeProgress(
                badge = "f0",
                challenge = "update_formula",
                remainingTimeSecs = 108000,
                progressPct = 0
            ),
            progressD
        )
    }

    @Test
    fun testNotFiringWhenNotOnBoard() {
        val counter = FeatherBadgeCounter()
        val result = counter.progress("f0", 0, null, 3, listOf("c0", "s0"))
        assertNull(result)

        val adviceStart = counter.onGameStarted(0, null, 3, listOf("c0", "s0"))
        assertNull(adviceStart.badge)
        assertEquals("108000", adviceStart.state)
        var state = adviceStart.state

        val adviceUpdate = counter.onFormulaUpdated(109000, state, 3, listOf("c0", "s0"))
        assertNull(adviceUpdate.badge)
        assertEquals("108000", adviceUpdate.state)
        state = adviceUpdate.state

        val progress = counter.progress("f0", 109001, state, 3, listOf("c0", "s0"))
        assertNull(progress)
    }
}
