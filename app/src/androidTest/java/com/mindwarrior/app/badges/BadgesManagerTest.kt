package com.mindwarrior.app.badges

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BadgesManagerTest {
    @Test
    fun testStartGameStartsFromLevel0() {
        val badgesManager = BadgesManager(2, null)
        val badge = badgesManager.onGameStarted(0)
        assertEquals("f0", badge)
        assertEquals(0, badgesManager.getLevel())
        assertEquals(
            listOf(
                BoardCell("f0", true, true),
                BoardCell("s0", false, false),
                BoardCell("s1", false, false),
                BoardCell("c0", false, false)
            ),
            badgesManager.getBoard()
        )

        assertEquals(
            mapOf(
                "c0" to BadgeProgress(
                    badge = "c0",
                    challenge = "review",
                    progressPct = 100,
                    remainingReviews = 0
                ),
                "s0" to BadgeProgress(
                    badge = "s0",
                    challenge = "review_regularly_no_penalty",
                    progressPct = 0,
                    remainingReviews = 5
                )
            ),
            badgesManager.progress(1000)
        )

        assertEquals(false, badgesManager.isLevelCompleted())
        assertJsonEquals(
            "{\"badges_state\": {\"CatBadgeCounter\": \"cumulative_counter_secs=0,counter_last_updated=0,update_reason=game_started\", \"TimeBadgeCounter\": \"86400\", \"StarBadgeCounter\": \"0,5\", \"FeatherBadgeCounter\": \"86400\"}, \"board\": [{\"badge\": \"f0\", \"is_active\": true, \"is_last_modified\": true}, {\"badge\": \"s0\", \"is_active\": false, \"is_last_modified\": false}, {\"badge\": \"s1\", \"is_active\": false, \"is_last_modified\": false}, {\"badge\": \"c0\", \"is_active\": false, \"is_last_modified\": false}], \"level\": 0, \"c0_hp\": 0, \"c0_hp_next_delta\": 3, \"last_badge\": \"f0\", \"last_badge_at\": 0, \"c0_active_time_penalty\": 0, \"c0_lock_started_at\": 0}",
            badgesManager.serialize()
        )
    }

    @Test
    fun testGrumpyCatGetsIn() {
        val badgesManager = BadgesManager(
            2,
            "{\"badges_state\": {\"CatBadgeCounter\": \"cumulative_counter_secs=0,counter_last_updated=0,update_reason=game_started\", \"TimeBadgeCounter\": \"86400\", \"StarBadgeCounter\": \"0,5\", \"FeatherBadgeCounter\": \"86400\"}, \"board\": [{\"badge\": \"f0\", \"is_active\": true, \"is_last_modified\": true}, {\"badge\": \"s0\", \"is_active\": false, \"is_last_modified\": false}, {\"badge\": \"s1\", \"is_active\": false, \"is_last_modified\": false}, {\"badge\": \"c0\", \"is_active\": false, \"is_last_modified\": false}], \"level\": 0, \"c0_hp\": 0, \"c0_hp_next_delta\": 3, \"last_badge\": \"f0\", \"last_badge_at\": 0, \"c0_active_time_penalty\": 0, \"c0_lock_started_at\": 0}"
        )
        val badge = badgesManager.onPenalty(61000)
        assertEquals("c0", badge)
        assertEquals(0, badgesManager.getLevel())
        assertEquals(
            listOf(
                BoardCell("f0", true, false),
                BoardCell("s0", false, false),
                BoardCell("s1", false, false),
                BoardCell("c0", true, true)
            ),
            badgesManager.getBoard()
        )

        assertEquals(false, badgesManager.isLevelCompleted())
        assertJsonEquals(
            "{\"badges_state\": {\"CatBadgeCounter\": \"cumulative_counter_secs=0,counter_last_updated=61000,update_reason=penalty\", \"TimeBadgeCounter\": \"86400\", \"StarBadgeCounter\": \"0,5,skip_next\", \"FeatherBadgeCounter\": \"86400\"}, \"board\": [{\"badge\": \"f0\", \"is_active\": true, \"is_last_modified\": false}, {\"badge\": \"s0\", \"is_active\": false, \"is_last_modified\": false}, {\"badge\": \"s1\", \"is_active\": false, \"is_last_modified\": false}, {\"badge\": \"c0\", \"is_active\": true, \"is_last_modified\": true}], \"level\": 0, \"c0_hp\": 15, \"c0_hp_next_delta\": 1, \"last_badge\": \"c0\", \"last_badge_at\": 61000, \"c0_active_time_penalty\": 0, \"c0_lock_started_at\": 61000}",
            badgesManager.serialize()
        )
    }

    @Test
    fun testNoGrumpyCatSmallLevels() {
        val badgesManager = BadgesManager(
            0,
            "{\"badges_state\": {\"CatBadgeCounter\": \"cumulative_counter_secs=0,counter_last_updated=0,update_reason=game_started\", \"TimeBadgeCounter\": \"86400\", \"StarBadgeCounter\": \"0,5\", \"FeatherBadgeCounter\": \"86400\"}, \"board\": [{\"badge\": \"f0\", \"is_active\": true, \"is_last_modified\": true}, {\"badge\": \"s0\", \"is_active\": false, \"is_last_modified\": false}, {\"badge\": \"s1\", \"is_active\": false, \"is_last_modified\": false}, {\"badge\": \"c0\", \"is_active\": false, \"is_last_modified\": false}, {\"badge\": \"c1\", \"is_active\": false, \"is_last_modified\": false}], \"level\": 0, \"c0_hp\": 0, \"c0_hp_next_delta\": 3, \"last_badge\": \"f0\", \"last_badge_at\": null, \"c0_active_time_penalty\": 0, \"c0_lock_started_at\": 0}"
        )
        val badge = badgesManager.onPenalty(61000)
        assertEquals(null, badge)
        assertJsonEquals(
            "{\"badges_state\": {\"CatBadgeCounter\": \"cumulative_counter_secs=0,counter_last_updated=61000,update_reason=penalty\", \"TimeBadgeCounter\": \"86400\", \"StarBadgeCounter\": \"0,5,skip_next\", \"FeatherBadgeCounter\": \"86400\"}, \"board\": [{\"badge\": \"f0\", \"is_active\": true, \"is_last_modified\": false }, {\"badge\": \"s0\", \"is_active\": false, \"is_last_modified\": false}, {\"badge\": \"s1\", \"is_active\": false, \"is_last_modified\": false}, {\"badge\": \"c0\", \"is_active\": false, \"is_last_modified\": false}, {\"badge\": \"c1\", \"is_active\": false, \"is_last_modified\": false}], \"level\": 0, \"c0_hp\": 0, \"c0_hp_next_delta\": 1, \"last_badge\": null, \"last_badge_at\": null, \"c0_active_time_penalty\": 0, \"c0_lock_started_at\": 0}",
            badgesManager.serialize()
        )
        badgesManager.onReview(100000)
        assertJsonEquals(
            "{\"badges_state\": {\"CatBadgeCounter\": \"cumulative_counter_secs=0,counter_last_updated=100000,update_reason=review\", \"TimeBadgeCounter\": \"86400\", \"StarBadgeCounter\": \"0,5\", \"FeatherBadgeCounter\": \"86400\"}, \"board\": [{\"badge\": \"f0\", \"is_active\": true, \"is_last_modified\": false}, {\"badge\": \"s0\", \"is_active\": false, \"is_last_modified\": false}, {\"badge\": \"s1\", \"is_active\": false, \"is_last_modified\": false}, {\"badge\": \"c0\", \"is_active\": false, \"is_last_modified\": false}, {\"badge\": \"c1\", \"is_active\": false, \"is_last_modified\": false}], \"level\": 0, \"c0_hp\": 0, \"c0_hp_next_delta\": 3, \"last_badge\": null, \"last_badge_at\": null, \"c0_active_time_penalty\": 0, \"c0_lock_started_at\": 0}",
            badgesManager.serialize()
        )
        badgesManager.onReview(120000)
        assertJsonEquals(
            "{\"badges_state\": {\"CatBadgeCounter\": \"cumulative_counter_secs=20000,counter_last_updated=120000,update_reason=review\", \"TimeBadgeCounter\": \"86400\", \"StarBadgeCounter\": \"1,5\", \"FeatherBadgeCounter\": \"86400\"}, \"board\": [{\"badge\": \"f0\", \"is_active\": true, \"is_last_modified\": false}, {\"badge\": \"s0\", \"is_active\": false, \"is_last_modified\": false}, {\"badge\": \"s1\", \"is_active\": false, \"is_last_modified\": false}, {\"badge\": \"c0\", \"is_active\": false, \"is_last_modified\": false}, {\"badge\": \"c1\", \"is_active\": false, \"is_last_modified\": false}], \"level\": 0, \"c0_hp\": 0, \"c0_hp_next_delta\": 3, \"last_badge\": null, \"last_badge_at\": null, \"c0_active_time_penalty\": 0, \"c0_lock_started_at\": 0}",
            badgesManager.serialize()
        )
    }

    @Test
    fun testGrumpySpoilsEverything() {
        val badgesState = "{\"CatBadgeCounter\": \"cumulative_counter_secs=0,counter_last_updated=61000,update_reason=penalty\", \"TimeBadgeCounter\": \"86400\", \"StarBadgeCounter\": \"0,5,skip_next\", \"FeatherBadgeCounter\": \"86400\"}"
        val badgesManager = BadgesManager(
            2,
            "{\"badges_state\": $badgesState, \"board\": [{\"badge\": \"f0\", \"is_active\": true, \"is_last_modified\": false}, {\"badge\": \"s0\", \"is_active\": false, \"is_last_modified\": false}, {\"badge\": \"s1\", \"is_active\": false, \"is_last_modified\": false}, {\"badge\": \"c0\", \"is_active\": true, \"is_last_modified\": true}], \"level\": 0, \"c0_hp\": 15, \"c0_hp_next_delta\": 1, \"last_badge\": \"c0\", \"last_badge_at\": 61000, \"c0_active_time_penalty\": 0, \"c0_lock_started_at\": 61000}"
        )
        val badge = badgesManager.onReview(1000000)
        assertEquals(null, badge)
        assertEquals(1, badgesManager.countActiveGrumpyCatsOnBoard())
        assertEquals(14, badgesManager.getGrumpyCatHealthpoints())
        assertJsonEquals(
            "{\"badges_state\": $badgesState, \"board\": [{\"badge\": \"f0\", \"is_active\": true, \"is_last_modified\": false}, {\"badge\": \"s0\", \"is_active\": false, \"is_last_modified\": false}, {\"badge\": \"s1\", \"is_active\": false, \"is_last_modified\": false}, {\"badge\": \"c0\", \"is_active\": true, \"is_last_modified\": false}], \"level\": 0, \"c0_hp\": 14, \"c0_hp_next_delta\": 3, \"last_badge\": null, \"last_badge_at\": 61000, \"c0_active_time_penalty\": 0, \"c0_lock_started_at\": 61000}",
            badgesManager.serialize()
        )
    }

    @Test
    fun testKickingOutGrumpyCat() {
        val badgesState = "{\"CatBadgeCounter\": \"cumulative_counter_secs=0,counter_last_updated=61000,update_reason=penalty\", \"TimeBadgeCounter\": \"86400\", \"StarBadgeCounter\": \"0,5,skip_next\", \"FeatherBadgeCounter\": \"86400\"}"
        val badgesManager = BadgesManager(
            2,
            "{\"badges_state\": $badgesState, \"board\": [{\"badge\": \"f0\", \"is_active\": true, \"is_last_modified\": false}, {\"badge\": \"s0\", \"is_active\": false, \"is_last_modified\": false}, {\"badge\": \"s1\", \"is_active\": false, \"is_last_modified\": false}, {\"badge\": \"c0\", \"is_active\": true, \"is_last_modified\": false}], \"level\": 0, \"c0_hp\": 14, \"c0_hp_next_delta\": 3, \"last_badge\": null, \"last_badge_at\": 61000, \"c0_active_time_penalty\": 0, \"c0_lock_started_at\": 61000}"
        )
        assertEquals(null, badgesManager.onReview(10))
        assertEquals(11, badgesManager.getGrumpyCatHealthpoints())
        assertEquals(null, badgesManager.onReview(11))
        assertEquals(8, badgesManager.getGrumpyCatHealthpoints())
        assertEquals(null, badgesManager.onReview(12))
        assertEquals(5, badgesManager.getGrumpyCatHealthpoints())
        assertEquals(null, badgesManager.onReview(13))
        assertEquals(2, badgesManager.getGrumpyCatHealthpoints())
        assertEquals("c0_removed", badgesManager.onReview(14))
        assertEquals(
            listOf(
                BoardCell("f0", true, false),
                BoardCell("s0", false, false),
                BoardCell("s1", false, false),
                BoardCell("c0", false, true)
            ),
            badgesManager.getBoard()
        )
        assertEquals(
            mapOf(
                "c0" to BadgeProgress(
                    badge = "c0",
                    challenge = "review",
                    progressPct = 100,
                    remainingReviews = 0
                ),
                "s0" to BadgeProgress(
                    badge = "s0",
                    challenge = "review_regularly_no_penalty",
                    progressPct = 0,
                    remainingReviews = 5
                )
            ),
            badgesManager.progress(1000)
        )
    }

    @Test
    fun testOutsideOfBoard() {
        val badgesManager = BadgesManager(
            2,
            "{\"badges_state\": {}, \"board\": [{\"badge\": \"s0\", \"is_active\": false, \"is_last_modified\": false}, {\"badge\": \"c0\", \"is_active\": false, \"is_last_modified\": false}], \"level\": 0, \"last_badge\": \"f0\"}"
        )
        val badge = badgesManager.onGameStarted(0)
        assertEquals(null, badge)
        assertEquals(0, badgesManager.getLevel())
        assertEquals(null, badgesManager.getLastBadge())
        assertEquals(
            listOf(
                BoardCell("s0", false, false),
                BoardCell("c0", false, false)
            ),
            badgesManager.getBoard()
        )
    }

    @Test
    fun testLevelUp() {
        val badgesManager = BadgesManager(
            2,
            "{\"badges_state\": {}, \"board\": [{\"badge\": \"f0\", \"is_active\": true, \"is_last_modified\": true}, {\"badge\": \"s0\", \"is_active\": false, \"is_last_modified\": false}, {\"badge\": \"c0\", \"is_active\": false, \"is_last_modified\": false}], \"level\": 0, \"last_badge\": \"f0\"}"
        )
        repeat(4) {
            val badge = badgesManager.onReview(100000)
            assertEquals(null, badge)
            assertEquals(false, badgesManager.isLevelCompleted())
        }

        val badge = badgesManager.onReview(100000)
        assertEquals("s0", badge)
        assertEquals(true, badgesManager.isLevelCompleted())

        assertEquals(
            listOf(
                BoardCell("s1", false, false),
                BoardCell("t0", false, false),
                BoardCell("c0", false, false),
                BoardCell("c1", false, false),
                BoardCell("c0", false, false)
            ),
            badgesManager.getNextLevelBoard()
        )
        val startedBadge = badgesManager.onReview(102600 - 1200)
        assertEquals(
            mapOf(
                "c0" to BadgeProgress(
                    badge = "c0",
                    challenge = "review",
                    progressPct = 100,
                    remainingReviews = 0
                ),
                "c1" to BadgeProgress(
                    badge = "c1",
                    challenge = "review_regularly_no_penalty",
                    progressPct = 3,
                    remainingTimeSecs = 56200
                ),
                "s1" to BadgeProgress(
                    badge = "s1",
                    challenge = "review_regularly_no_penalty",
                    progressPct = 10,
                    remainingReviews = 9
                ),
                "t0" to BadgeProgress(
                    badge = "t0",
                    challenge = "play_time",
                    progressPct = 1,
                    remainingTimeSecs = 85000
                )
            ),
            badgesManager.progress(102600 - 1200)
        )
        assertEquals(null, startedBadge)
        val badgeLater = badgesManager.onReview(1100001)
        assertEquals("c1", badgeLater)
        assertEquals(1, badgesManager.getLevel())
        assertEquals("c1", badgesManager.getLastBadge())
        assertEquals(
            listOf(
                BoardCell("s1", false, false),
                BoardCell("t0", false, false),
                BoardCell("c0", false, false),
                BoardCell("c1", true, true),
                BoardCell("c0", false, false)
            ),
            badgesManager.getBoard()
        )
        assertEquals(false, badgesManager.isLevelCompleted())
    }

    @Test
    fun testC0CanBlockTwoCells() {
        val badgesManager = BadgesManager(
            2,
            "{\"badges_state\": {}, \"board\": [{\"badge\": \"t0\", \"is_active\": false, \"is_last_modified\": true}, {\"badge\": \"c0\", \"is_active\": false, \"is_last_modified\": false}, {\"badge\": \"c0\", \"is_active\": false, \"is_last_modified\": false}], \"level\": 0, \"last_badge\": \"f0\"}"
        )
        val badge = badgesManager.onPenalty(50)
        assertEquals("c0", badge)
        assertEquals(
            mapOf(
                "c0" to BadgeProgress(
                    badge = "c0",
                    challenge = "review",
                    progressPct = 0,
                    remainingReviews = 15
                ),
                "t0" to BadgeProgress(
                    badge = "t0",
                    challenge = "play_time",
                    progressPct = 0,
                    remainingTimeSecs = 86400
                )
            ),
            badgesManager.progress(50)
        )
        badgesManager.onReview(600)
        val badgeSecond = badgesManager.onPenalty(800)
        assertEquals("c0", badgeSecond)
        assertEquals(2, badgesManager.countActiveGrumpyCatsOnBoard())
        assertEquals(
            listOf(
                BoardCell("t0", false, false),
                BoardCell("c0", true, false),
                BoardCell("c0", true, true)
            ),
            badgesManager.getBoard()
        )
        val badgeThird = badgesManager.onPenalty(70)
        assertEquals(null, badgeThird)

        assertEquals(14, badgesManager.getGrumpyCatHealthpoints())
        badgesManager.onReview(800)
        assertEquals(13, badgesManager.getGrumpyCatHealthpoints())
        assertEquals(
            mapOf(
                "c0" to BadgeProgress(
                    badge = "c0",
                    challenge = "review",
                    progressPct = 14,
                    remainingReviews = 13
                ),
                "t0" to BadgeProgress(
                    badge = "t0",
                    challenge = "play_time",
                    progressPct = 0,
                    remainingTimeSecs = 86400
                )
            ),
            badgesManager.progress(800)
        )
        badgesManager.onReview(900)
        badgesManager.onReview(1000)
        badgesManager.onReview(1100)
        badgesManager.onReview(1200)
        assertEquals(1, badgesManager.getGrumpyCatHealthpoints())
        assertEquals("c0_removed", badgesManager.onReview(2000))
        assertEquals(15, badgesManager.getGrumpyCatHealthpoints())
        assertEquals(
            mapOf(
                "c0" to BadgeProgress(
                    badge = "c0",
                    challenge = "review",
                    progressPct = 0,
                    remainingReviews = 15
                ),
                "t0" to BadgeProgress(
                    badge = "t0",
                    challenge = "play_time",
                    progressPct = 0,
                    remainingTimeSecs = 86400
                )
            ),
            badgesManager.progress(2000)
        )
        assertEquals(
            listOf(
                BoardCell("t0", false, false),
                BoardCell("c0", false, true),
                BoardCell("c0", true, false)
            ),
            badgesManager.getBoard()
        )
        badgesManager.onReview(2030)
        badgesManager.onReview(2040)
        badgesManager.onReview(2050)
        badgesManager.onReview(2060)
        badgesManager.onReview(2070)
        assertEquals(
            listOf(
                BoardCell("t0", false, false),
                BoardCell("c0", false, false),
                BoardCell("c0", false, true)
            ),
            badgesManager.getBoard()
        )
        assertEquals(
            mapOf(
                "c0" to BadgeProgress(
                    badge = "c0",
                    challenge = "review",
                    progressPct = 100,
                    remainingReviews = 0
                ),
                "t0" to BadgeProgress(
                    badge = "t0",
                    challenge = "play_time",
                    progressPct = 0,
                    remainingTimeSecs = 86400
                )
            ),
            badgesManager.progress(3000)
        )
        badgesManager.onReview(3500)
        assertEquals(
            listOf(
                BoardCell("t0", false, false),
                BoardCell("c0", false, false),
                BoardCell("c0", false, false)
            ),
            badgesManager.getBoard()
        )
        assertEquals(
            mapOf(
                "c0" to BadgeProgress(
                    badge = "c0",
                    challenge = "review",
                    progressPct = 100,
                    remainingReviews = 0
                ),
                "t0" to BadgeProgress(
                    badge = "t0",
                    challenge = "play_time",
                    progressPct = 1,
                    remainingTimeSecs = 85150
                )
            ),
            badgesManager.progress(4750)
        )
        assertEquals("t0", badgesManager.onReview(100000))
    }

    @Test
    fun testNewLevelEmptyProgress() {
        val badgesManager = BadgesManager(
            2,
            "{\"badges_state\": {}, \"board\": [{\"badge\": \"f0\", \"is_active\": true, \"is_last_modified\": true}, {\"badge\": \"s0\", \"is_active\": true, \"is_last_modified\": false}, {\"badge\": \"s1\", \"is_active\": true, \"is_last_modified\": false}, {\"badge\": \"c0\", \"is_active\": false, \"is_last_modified\": false}], \"level\": 0, \"c0_hp\": 0, \"c0_hp_next_delta\": 3, \"last_badge\": \"f0\", \"c0_active_time_penalty\": 32951, \"c0_lock_started_at\": 116774}"
        )
        assertEquals(
            mapOf(
                "c1" to BadgeProgress(
                    badge = "c1",
                    challenge = "review_regularly_no_penalty",
                    progressPct = 0,
                    remainingTimeSecs = 57600
                ),
                "s1" to BadgeProgress(
                    badge = "s1",
                    challenge = "review_regularly_no_penalty",
                    progressPct = 0,
                    remainingReviews = 10
                ),
                "t0" to BadgeProgress(
                    badge = "t0",
                    challenge = "play_time",
                    progressPct = 0,
                    remainingTimeSecs = 86400
                )
            ),
            badgesManager.newLevelEmptyProgress()
        )
    }

    private fun assertJsonEquals(expected: String, actual: String) {
        assertJsonValueEquals("<root>", JSONObject(expected), JSONObject(actual))
    }

    private fun assertJsonValueEquals(path: String, expected: Any?, actual: Any?) {
        val exp = normalizeJsonValue(expected)
        val act = normalizeJsonValue(actual)
        when {
            exp is JSONObject && act is JSONObject -> {
                val expKeys = exp.keys().asSequence().toSet()
                val actKeys = act.keys().asSequence().toSet()
                assertEquals(expKeys, actKeys)
                expKeys.forEach { key ->
                    assertJsonValueEquals(path +"/" + key, exp.get(key), act.get(key))
                }
            }
            exp is JSONArray && act is JSONArray -> {
                assertEquals(exp.length(), act.length())
                for (idx in 0 until exp.length()) {
                    assertJsonValueEquals(path + "[" + idx + "]", exp.get(idx), act.get(idx))
                }
            }
            exp is Number && act is Number -> {
                assertEquals(path, exp.toDouble(), act.toDouble(), 0.0)
            }
            else -> assertEquals(path, exp, act)
        }
    }

    private fun normalizeJsonValue(value: Any?): Any? {
        return if (value == JSONObject.NULL) null else value
    }
}
