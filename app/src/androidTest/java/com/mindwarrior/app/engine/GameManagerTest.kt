package com.mindwarrior.app.engine

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mindwarrior.app.NowProvider
import com.mindwarrior.app.badges.BadgesManager
import java.util.Optional
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Calendar
import org.json.JSONArray
import org.json.JSONObject

@RunWith(AndroidJUnit4::class)
class GameManagerTest {

    @Test
    fun onPausedKeepsPausedUserUnchanged() {
        val user = UserFactory.createUser(Difficulty.EASY)

        val updated = GameManager.onPaused(user, "test")

        assertEquals(user, updated)
        assertTrue(updated.pausedTimerSerialized.isPresent)
    }

    @Test
    fun onPausedPausesActiveAndReviewTimers() {
        val active = Counter(null).resume().serialize()
        val review = Counter(null).resume().serialize()
        val user = UserFactory.createUser(Difficulty.EASY).copy(
            pausedTimerSerialized = Optional.empty(),
            activePlayTimerSerialized = active,
            nextPenaltyTimerSerialized = review
        )

        val updated = GameManager.onPaused(user, "test")

        assertTrue(updated.pausedTimerSerialized.isPresent)
        assertFalse(Counter(updated.activePlayTimerSerialized).isActive())
        assertFalse(Counter(updated.nextPenaltyTimerSerialized).isActive())
    }

    @Test
    fun onResumeUnpausesWhenPaused() {
        val user = UserFactory.createUser(Difficulty.EASY)

        val updated = GameManager.onResume(user, "test")

        assertFalse(updated.pausedTimerSerialized.isPresent)
        assertTrue(Counter(updated.activePlayTimerSerialized).isActive())
        assertTrue(Counter(updated.nextPenaltyTimerSerialized).isActive())
    }

    @Test
    fun onLocalStorageUpdatedStartsGameWhenFormulaPresentAndNearZero() {
        val user = UserFactory.createUser(Difficulty.EASY)
        val storage = Optional.of("{\"formula\":\"hello\"}")

        val updated = GameManager.onLocalStorageUpdated(
            user,
            storage,
            true,
            "",
            "",
            "",
            ""
        )

        assertTrue(updated.localStorageSnapshot.isPresent)
        assertTrue(updated.pausedTimerSerialized.isEmpty)
        assertTrue(Counter(updated.activePlayTimerSerialized).isActive())
        assertTrue(Counter(updated.nextPenaltyTimerSerialized).isActive())
    }

    @Test
    fun onReviewCompletedNoCatsNoNewAchievement() {
        val badgesSerialized = buildBadgesSerialized(
            board = listOf(Pair("s0", false)),
            badgesState = mapOf("StarBadgeCounter" to "0,3")
        )
        val user = UserFactory.createUser(Difficulty.BEGINNER).copy(
            badgesSerialized = badgesSerialized,
            activePlayTimerSerialized = activeTimerSerialized(minutes = 10),
            lastRewardAtActivePlayTime = 0L
        )

        val updated = GameManager.onReviewCompleted(
            user,
            "REVIEW",
            "REWARD",
            "NO_REWARD",
            "NEW_BADGE",
            "GRUMPY_REMOVED",
            "REMAINING %d",
            "UNBLOCKED",
            "GRUMPY_BLOCKING"
        )

        assertEquals(1, updated.diamonds)
        assertEquals(
            "REVIEW\n\nREWARD",
            updated.unseenLogsNewestFirst.first().first
        )
    }

    @Test
    fun onReviewCompletedNoCatsWithNewAchievement() {
        val badgesSerialized = buildBadgesSerialized(
            board = listOf(Pair("s0", false)),
            badgesState = mapOf("StarBadgeCounter" to "2,3")
        )
        val user = UserFactory.createUser(Difficulty.BEGINNER).copy(
            badgesSerialized = badgesSerialized,
            activePlayTimerSerialized = activeTimerSerialized(minutes = 10),
            lastRewardAtActivePlayTime = 0L
        )

        val updated = GameManager.onReviewCompleted(
            user,
            "REVIEW",
            "REWARD",
            "NO_REWARD",
            "NEW_BADGE",
            "GRUMPY_REMOVED",
            "REMAINING %d",
            "UNBLOCKED",
            "GRUMPY_BLOCKING"
        )

        assertEquals(
            "REVIEW\n\nNEW_BADGE\n\nREWARD",
            updated.unseenLogsNewestFirst.first().first
        )
    }

    @Test
    fun onReviewCompletedBlockingCatNotExpelled() {
        val badgesSerialized = buildBadgesSerialized(
            board = listOf(Pair("c0", true)),
            c0Hp = 5,
            c0HpNextDelta = 3
        )
        val user = UserFactory.createUser(Difficulty.BEGINNER).copy(
            badgesSerialized = badgesSerialized,
            activePlayTimerSerialized = activeTimerSerialized(minutes = 10),
            lastRewardAtActivePlayTime = 0L
        )

        val updated = GameManager.onReviewCompleted(
            user,
            "REVIEW",
            "REWARD",
            "NO_REWARD",
            "NEW_BADGE",
            "GRUMPY_REMOVED",
            "REMAINING %d",
            "UNBLOCKED",
            "GRUMPY_BLOCKING"
        )

        assertEquals(0, updated.diamonds)
        assertEquals(
            "REVIEW\n\nGRUMPY_BLOCKING",
            updated.unseenLogsNewestFirst.first().first
        )
    }

    @Test
    fun onReviewCompletedTwoBlockingCatsOneExpelled() {
        val badgesSerialized = buildBadgesSerialized(
            board = listOf(Pair("c0", true), Pair("c0", true)),
            c0Hp = 2,
            c0HpNextDelta = 3
        )
        val user = UserFactory.createUser(Difficulty.BEGINNER).copy(
            badgesSerialized = badgesSerialized,
            activePlayTimerSerialized = activeTimerSerialized(minutes = 10),
            lastRewardAtActivePlayTime = 0L
        )

        val updated = GameManager.onReviewCompleted(
            user,
            "REVIEW",
            "REWARD",
            "NO_REWARD",
            "NEW_BADGE",
            "GRUMPY_REMOVED",
            "REMAINING %d",
            "UNBLOCKED",
            "GRUMPY_BLOCKING"
        )

        assertEquals(0, updated.diamonds)
        assertEquals(
            "REVIEW\n\nGRUMPY_REMOVED REMAINING 1",
            updated.unseenLogsNewestFirst.first().first
        )
    }

    @Test
    fun onReviewCompletedSingleBlockingCatExpelled() {
        val badgesSerialized = buildBadgesSerialized(
            board = listOf(Pair("c0", true)),
            c0Hp = 2,
            c0HpNextDelta = 3
        )
        val user = UserFactory.createUser(Difficulty.BEGINNER).copy(
            badgesSerialized = badgesSerialized,
            activePlayTimerSerialized = activeTimerSerialized(minutes = 10),
            lastRewardAtActivePlayTime = 0L
        )

        val updated = GameManager.onReviewCompleted(
            user,
            "REVIEW",
            "REWARD",
            "NO_REWARD",
            "NEW_BADGE",
            "GRUMPY_REMOVED",
            "REMAINING %d",
            "UNBLOCKED",
            "GRUMPY_BLOCKING"
        )

        assertEquals(1, updated.diamonds)
        assertEquals(
            "REVIEW\n\nGRUMPY_REMOVED UNBLOCKED\n\nREWARD",
            updated.unseenLogsNewestFirst.first().first
        )
    }

    @Test
    fun onReviewCompletedTrimsUnseenLogsToTen() {
        val badgesSerialized = buildBadgesSerialized(
            board = listOf(Pair("s0", false)),
            badgesState = mapOf("StarBadgeCounter" to "0,3")
        )
        val now = NowProvider.nowMillis()
        val existingLogs = List(12) { idx -> Pair("old-$idx", now - idx) }
        val user = UserFactory.createUser(Difficulty.BEGINNER).copy(
            badgesSerialized = badgesSerialized,
            activePlayTimerSerialized = activeTimerSerialized(minutes = 10),
            lastRewardAtActivePlayTime = 0L,
            unseenLogsNewestFirst = existingLogs
        )

        val updated = GameManager.onReviewCompleted(
            user,
            "REVIEW",
            "REWARD",
            "NO_REWARD",
            "NEW_BADGE",
            "GRUMPY_REMOVED",
            "REMAINING %d",
            "UNBLOCKED",
            "GRUMPY_BLOCKING"
        )

        assertEquals(10, updated.unseenLogsNewestFirst.size)
        assertEquals("REVIEW\n\nREWARD", updated.unseenLogsNewestFirst.first().first)
    }

    @Test
    fun onReviewCompletedUpdatesLastRewardAtActivePlayTimeWhenRewarded() {
        val badgesSerialized = buildBadgesSerialized(
            board = listOf(Pair("s0", false)),
            badgesState = mapOf("StarBadgeCounter" to "0,3")
        )
        val user = UserFactory.createUser(Difficulty.BEGINNER).copy(
            badgesSerialized = badgesSerialized,
            activePlayTimerSerialized = activeTimerSerialized(minutes = 10),
            lastRewardAtActivePlayTime = 0L
        )
        val activePlaySeconds = Counter(user.activePlayTimerSerialized).getTotalSeconds()

        val updated = GameManager.onReviewCompleted(
            user,
            "REVIEW",
            "REWARD",
            "NO_REWARD",
            "NEW_BADGE",
            "GRUMPY_REMOVED",
            "REMAINING %d",
            "UNBLOCKED",
            "GRUMPY_BLOCKING"
        )

        assertEquals(activePlaySeconds, updated.lastRewardAtActivePlayTime)
    }

    @Test
    fun onUnseenLogsObservedTrimsOldLogsToHundred() {
        val now = NowProvider.nowMillis()
        val unseen = List(5) { idx -> Pair("new-$idx", now - idx) }
        val oldLogs = List(150) { idx -> Pair("old-$idx", now - 1_000 - idx) }
        val user = UserFactory.createUser(Difficulty.BEGINNER).copy(
            unseenLogsNewestFirst = unseen,
            oldLogsNewestFirst = oldLogs
        )

        val updated = GameManager.onUnseenLogsObserved(user, unseen.size)

        assertTrue(updated.unseenLogsNewestFirst.isEmpty())
        assertEquals(100, updated.oldLogsNewestFirst.size)
        assertEquals("new-0", updated.oldLogsNewestFirst.first().first)
    }

    @Test
    fun onUnseenLogsObservedMovesOldestObservedItems() {
        val now = NowProvider.nowMillis()
        val unseen = listOf(
            Pair("u0", now),
            Pair("u1", now - 1),
            Pair("u2", now - 2),
            Pair("u3", now - 3)
        )
        val oldLogs = listOf(Pair("old", now - 10))
        val user = UserFactory.createUser(Difficulty.BEGINNER).copy(
            unseenLogsNewestFirst = unseen,
            oldLogsNewestFirst = oldLogs
        )

        val updated = GameManager.onUnseenLogsObserved(user, 2)

        assertEquals(listOf("u0", "u1"), updated.unseenLogsNewestFirst.map { it.first })
        assertEquals("u2", updated.oldLogsNewestFirst.first().first)
    }

    @Test
    fun onDifficultyChangedResetsUserForNewDifficultyWhenUnpaused() {
        val user = UserFactory.createUser(Difficulty.EASY).copy(
            pausedTimerSerialized = Optional.empty()
        )

        val updated = GameManager.onDifficultyChanged(user, Difficulty.HARD, "test")

        assertEquals(Difficulty.HARD, updated.difficulty)
        assertFalse(updated.pausedTimerSerialized.isPresent)
    }

    @Test
    fun onDifficultyChangedKeepsPausedWhenPaused() {
        val user = UserFactory.createUser(Difficulty.EASY)

        val updated = GameManager.onDifficultyChanged(user, Difficulty.HARD, "test")

        assertEquals(Difficulty.HARD, updated.difficulty)
        assertTrue(updated.pausedTimerSerialized.isPresent)
        assertFalse(Counter(updated.activePlayTimerSerialized).isActive())
        assertFalse(Counter(updated.nextPenaltyTimerSerialized).isActive())
    }

    @Test
    fun onSleepScheduleChangedUpdatesFields() {
        val user = UserFactory.createUser(Difficulty.EASY)

        val updated = GameManager.onSleepScheduleChanged(user, true, 22 * 60, 6 * 60, "")

        assertTrue(updated.nextSleepEventAtMillis.isPresent)
        assertEquals(22 * 60, updated.sleepStartMinutes)
        assertEquals(6 * 60, updated.sleepEndMinutes)
    }

    @Test
    fun evaluateAlertsReturnsUnchangedWhenPaused() {
        val user = userWithElapsedMinutes(
            elapsedMinutes = 200,
            difficulty = Difficulty.EASY,
            nextAlertType = AlertType.Reminder,
            paused = true
        )

        val updated = GameManager.evaluateAlerts(
            user,
            "REMINDER",
            "PENALTY",
            "GRUMPY"
        )

        assertEquals(user, updated)
    }

    @Test
    fun evaluateAlertsAddsNudgeWhenThresholdReached() {
        val user = userWithElapsedMinutes(
            elapsedMinutes = 170,
            difficulty = Difficulty.EASY,
            nextAlertType = AlertType.Reminder,
            paused = false
        )

        val updated = GameManager.evaluateAlerts(
            user,
            "REMINDER",
            "PENALTY",
            "GRUMPY"
        )

        assertEquals(AlertType.Penalty, updated.nextAlertType)
        assertTrue(updated.pendingNotificationLogsNewestFirst.isNotEmpty())
    }

    @Test
    fun evaluateAlertsAddsPenaltyWhenOverdue() {
        val user = userWithElapsedMinutes(
            elapsedMinutes = 190,
            difficulty = Difficulty.EASY,
            nextAlertType = AlertType.Penalty,
            paused = false
        )

        val updated = GameManager.evaluateAlerts(
            user,
            "REMINDER",
            "PENALTY",
            "GRUMPY"
        )

        assertEquals(AlertType.Reminder, updated.nextAlertType)
        assertTrue(updated.pendingNotificationLogsNewestFirst.isNotEmpty())
        assertFalse(user.nextPenaltyTimerSerialized == updated.nextPenaltyTimerSerialized)
    }

    @Test
    fun evaluateAlertsReminderDoesNotPrefixWithoutNewCat() {
        val user = userWithElapsedMinutes(
            elapsedMinutes = 170,
            difficulty = Difficulty.EASY,
            nextAlertType = AlertType.Reminder,
            paused = false
        )

        val updated = GameManager.evaluateAlerts(
            user,
            "REMINDER",
            "PENALTY",
            "GRUMPY"
        )

        assertEquals(AlertType.Penalty, updated.nextAlertType)
        val message = updated.pendingNotificationLogsNewestFirst.first().first
        assertTrue(message.startsWith("REMINDER"))
    }

    @Test
    fun evaluateAlertsPenaltyPrefixesWhenCatAppears() {
        val user = userWithElapsedMinutes(
            elapsedMinutes = 190,
            difficulty = Difficulty.EASY,
            nextAlertType = AlertType.Penalty,
            paused = false
        )

        val updated = GameManager.evaluateAlerts(
            user,
            "REMINDER",
            "PENALTY",
            "GRUMPY"
        )

        val message = updated.pendingNotificationLogsNewestFirst.first().first
        assertTrue(message.startsWith("GRUMPY\n\nPENALTY"))
        val manager = BadgesManager(updated.difficulty.ordinal, updated.badgesSerialized)
        assertTrue(manager.countActiveGrumpyCatsOnBoard() > 0)
    }

    @Test
    fun evaluateAlertsSkipsNudgeWhenNotSupported() {
        val user = userWithElapsedMinutes(
            elapsedMinutes = 70,
            difficulty = Difficulty.HARD,
            nextAlertType = AlertType.Penalty,
            paused = false
        )

        val updated = GameManager.evaluateAlerts(
            user,
            "REMINDER",
            "PENALTY",
            "GRUMPY"
        )

        assertEquals(updated.pendingNotificationLogsNewestFirst[0].first, "GRUMPY\n\nPENALTY")
        assertEquals(updated.nextAlertType, AlertType.Penalty)
    }

    @Test
    fun evaluateAlertsHandlesSleepEventWhenInsideInterval() {
        val nowMinutes = currentMinutesOfDay()
        val start = normalizeMinutes(nowMinutes - 10)
        val end = normalizeMinutes(nowMinutes + 10)
        val user = userWithElapsedMinutes(
            elapsedMinutes = 1,
            difficulty = Difficulty.EASY,
            nextAlertType = AlertType.Reminder,
            paused = false
        ).copy(
            sleepStartMinutes = start,
            sleepEndMinutes = end,
            nextSleepEventAtMillis = Optional.of(System.currentTimeMillis() - 1)
        )

        val updated = GameManager.evaluateAlerts(
            user,
            "REMINDER",
            "PENALTY",
            "GRUMPY"
        )

        assertTrue(updated.pausedTimerSerialized.isPresent)
        assertTrue(updated.nextSleepEventAtMillis.isPresent)
        assertTrue(updated.pendingNotificationLogsNewestFirst.isNotEmpty())
    }

    @Test
    fun evaluateAlertsHandlesSleepEventWhenOutsideInterval() {
        val nowMinutes = currentMinutesOfDay()
        val start = normalizeMinutes(nowMinutes + 10)
        val end = normalizeMinutes(nowMinutes + 20)
        val user = userWithElapsedMinutes(
            elapsedMinutes = 1,
            difficulty = Difficulty.EASY,
            nextAlertType = AlertType.Reminder,
            paused = true
        ).copy(
            sleepStartMinutes = start,
            sleepEndMinutes = end,
            nextSleepEventAtMillis = Optional.of(System.currentTimeMillis() - 1)
        )

        val updated = GameManager.evaluateAlerts(
            user,
            "REMINDER",
            "PENALTY",
            "GRUMPY"
        )

        assertFalse(updated.pausedTimerSerialized.isPresent)
        assertTrue(updated.nextSleepEventAtMillis.isPresent)
        assertTrue(updated.pendingNotificationLogsNewestFirst.isNotEmpty())
    }

    @Test
    fun calculateNextAlertMillisReturnsNudgeForReminder() {
        val nowMillis = System.currentTimeMillis()
        val user = userWithElapsedMinutes(
            elapsedMinutes = 10,
            difficulty = Difficulty.EASY,
            nextAlertType = AlertType.Reminder,
            paused = false
        )
        val penaltyThreshold = DifficultyHelper.getReviewFrequencyMillis(user.difficulty)
        val nudgeThreshold = penaltyThreshold - 15 * 60 * 1000
        val penaltyTimerStartedAtMillis =
            nowMillis - Counter(user.nextPenaltyTimerSerialized).getTotalSeconds() * 1000
        val expected = penaltyTimerStartedAtMillis + nudgeThreshold

        val actual = GameManager.calculateNextAlertMillis(user)

        assertMillisClose(expected, actual)
    }

    @Test
    fun calculateNextAlertMillisReturnsPenaltyForPenaltyAlertType() {
        val nowMillis = System.currentTimeMillis()
        val user = userWithElapsedMinutes(
            elapsedMinutes = 10,
            difficulty = Difficulty.EASY,
            nextAlertType = AlertType.Penalty,
            paused = false
        )
        val penaltyThreshold = DifficultyHelper.getReviewFrequencyMillis(user.difficulty)
        val penaltyTimerStartedAtMillis =
            nowMillis - Counter(user.nextPenaltyTimerSerialized).getTotalSeconds() * 1000
        val expected = penaltyTimerStartedAtMillis + penaltyThreshold

        val actual = GameManager.calculateNextAlertMillis(user)

        assertMillisClose(expected, actual)
    }

    @Test
    fun calculateNextAlertMillisPrefersSleepEventWhenPaused() {
        val nowMillis = System.currentTimeMillis()
        val nowMinutes = currentMinutesOfDay()
        val start = normalizeMinutes(nowMinutes + 1)
        val end = normalizeMinutes(nowMinutes + 2)
        val user = userWithElapsedMinutes(
            elapsedMinutes = 10,
            difficulty = Difficulty.EASY,
            nextAlertType = AlertType.Reminder,
            paused = true
        ).copy(
            sleepStartMinutes = start,
            sleepEndMinutes = end,
            nextSleepEventAtMillis = Optional.of(SleepUtils.calculateNextSleepEventMillisAt(
                nowMillis,
                start,
                end
            ))
        )
        val expected = nextOccurrenceMillisAt(nowMillis, start)

        val actual = GameManager.calculateNextAlertMillis(user)

        assertMillisClose(expected, actual)
    }

    private fun userWithElapsedMinutes(
        elapsedMinutes: Int,
        difficulty: Difficulty,
        nextAlertType: AlertType,
        paused: Boolean
    ): User {
        val timer = Counter(null).resume().apply {
            moveTimeBack(elapsedMinutes.toLong())
            getTotalSeconds()
        }.serialize()
        val pausedTimer = if (paused) Optional.of(Counter(null).resume().serialize()) else Optional.empty()
        return UserFactory.createUser(difficulty).copy(
            pausedTimerSerialized = pausedTimer,
            nextPenaltyTimerSerialized = timer,
            nextAlertType = nextAlertType,
            nextSleepEventAtMillis = Optional.empty()
        )
    }

    private fun currentMinutesOfDay(): Int {
        val calendar = Calendar.getInstance()
        return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
    }

    private fun normalizeMinutes(minutes: Int): Int {
        val minutesInDay = 24 * 60
        return ((minutes % minutesInDay) + minutesInDay) % minutesInDay
    }

    private fun assertMillisClose(expected: Long, actual: Long) {
        val toleranceMillis = 2_000L
        val delta = kotlin.math.abs(expected - actual)
        assertTrue("Expected $expected but was $actual", delta <= toleranceMillis)
    }

    private fun nextOccurrenceMillisAt(nowMillis: Long, minutesOfDay: Int): Long {
        val minutesInDay = 24 * 60
        val normalized = ((minutesOfDay % minutesInDay) + minutesInDay) % minutesInDay
        val calendar = Calendar.getInstance().apply { timeInMillis = nowMillis }
        calendar.set(Calendar.HOUR_OF_DAY, normalized / 60)
        calendar.set(Calendar.MINUTE, normalized % 60)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        if (calendar.timeInMillis < nowMillis) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return calendar.timeInMillis
    }

    private fun buildBadgesSerialized(
        board: List<Pair<String, Boolean>>,
        badgesState: Map<String, String?> = emptyMap(),
        c0Hp: Int = 0,
        c0HpNextDelta: Int = 3
    ): String {
        val root = JSONObject()
        val badgesStateJson = JSONObject()
        badgesState.forEach { (key, value) ->
            if (value == null) {
                badgesStateJson.put(key, JSONObject.NULL)
            } else {
                badgesStateJson.put(key, value)
            }
        }
        root.put("badges_state", badgesStateJson)
        root.put("last_badge", JSONObject.NULL)
        root.put("last_badge_at", JSONObject.NULL)
        root.put("level", 0)
        root.put("c0_hp_next_delta", c0HpNextDelta)
        root.put("c0_hp", c0Hp)
        root.put("c0_lock_started_at", 0)
        root.put("c0_active_time_penalty", 0)

        val boardJson = JSONArray()
        for ((badge, isActive) in board) {
            val cell = JSONObject()
            cell.put("badge", badge)
            cell.put("is_active", isActive)
            cell.put("is_last_modified", false)
            boardJson.put(cell)
        }
        root.put("board", boardJson)
        return root.toString()
    }

    private fun activeTimerSerialized(minutes: Long): String {
        return Counter(null).resume().apply {
            moveTimeBack(minutes)
            getTotalSeconds()
            pause()
        }.serialize()
    }
}
