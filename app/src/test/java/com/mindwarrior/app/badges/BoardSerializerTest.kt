package com.mindwarrior.app.badges

import org.junit.Assert.assertEquals
import org.junit.Test

class BoardSerializerTest {
    @Test
    fun testSerializeSimpleBoard() {
        val board = listOf(
            BoardCell("f0", true, true),
            BoardCell("s0"),
            BoardCell("c0")
        )
        assertEquals("f0am_s0_c0", BoardSerializer.serializeBoard(board))
    }

    @Test
    fun testSerializeProgress() {
        val serialized = BoardSerializer.serializeProgress(
            linkedMapOf(
                "c1" to BadgeProgress(
                    badge = "c1",
                    challenge = "review_regularly_no_penalty",
                    progressPct = 1,
                    remainingTimeSecs = 56600
                ),
                "c2" to BadgeProgress(
                    badge = "c1",
                    challenge = "review_regularly_no_penalty",
                    progressPct = 1,
                    remainingTimeSecs = 56600
                ),
                "f0" to BadgeProgress(
                    badge = "f0",
                    challenge = "update_formula",
                    progressPct = 1,
                    remainingTimeSecs = 85400
                ),
                "s0" to BadgeProgress(
                    badge = "s0",
                    challenge = "review_regularly_no_penalty",
                    progressPct = 0,
                    remainingReviews = 3
                ),
                "s1" to BadgeProgress(
                    badge = "s1",
                    challenge = "review_regularly_no_penalty",
                    progressPct = 0,
                    remainingReviews = 6
                ),
                "s2" to BadgeProgress(
                    badge = "s2",
                    challenge = "review_regularly_no_penalty",
                    progressPct = 0,
                    remainingReviews = 9
                ),
                "t0" to BadgeProgress(
                    badge = "t0",
                    challenge = "play_time",
                    progressPct = 1,
                    remainingTimeSecs = 85400
                ),
                "c0" to BadgeProgress(
                    badge = "c0",
                    challenge = "review",
                    progressPct = 7,
                    remainingReviews = 14
                )
            )
        )

        assertEquals(
            "c1_56600_1--c2_56600_1--f0_85400_1--s0_3_0--s1_6_0--s2_9_0--t0_85400_1--c0_14_7",
            serialized
        )
    }

    @Test
    fun testSerializeGrumpyCat() {
        val serialized = BoardSerializer.serializeBoard(
            listOf(
                BoardCell("f0", true),
                BoardCell("s0"),
                BoardCell("c0", true, true)
            )
        )
        assertEquals("f0a_s0_c0am", serialized)
    }

    @Test
    fun testSerializeKickingOutGrumpyCat() {
        val serialized = BoardSerializer.serializeBoard(
            listOf(
                BoardCell("f0", true),
                BoardCell("s0"),
                BoardCell("c0", false, true)
            )
        )
        assertEquals("f0a_s0_c0m", serialized)
    }
}
