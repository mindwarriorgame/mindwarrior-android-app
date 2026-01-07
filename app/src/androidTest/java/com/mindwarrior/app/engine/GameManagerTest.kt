package com.mindwarrior.app.engine

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mindwarrior.app.NowProvider
import java.util.Optional
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Calendar

@RunWith(AndroidJUnit4::class)
class GameManagerTest {

    @Test
    fun onPausedKeepsPausedUserUnchanged() {
        val user = UserFactory.createUser(Difficulty.EASY)

        val updated = GameManager.onPaused(user)

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

        val updated = GameManager.onPaused(user)

        assertTrue(updated.pausedTimerSerialized.isPresent)
        assertFalse(Counter(updated.activePlayTimerSerialized).isActive())
        assertFalse(Counter(updated.nextPenaltyTimerSerialized).isActive())
    }

    @Test
    fun onResumeUnpausesWhenPaused() {
        val user = UserFactory.createUser(Difficulty.EASY)

        val updated = GameManager.onResume(user)

        assertFalse(updated.pausedTimerSerialized.isPresent)
        assertTrue(Counter(updated.activePlayTimerSerialized).isActive())
        assertTrue(Counter(updated.nextPenaltyTimerSerialized).isActive())
    }

    @Test
    fun onLocalStorageUpdatedStartsGameWhenFormulaPresentAndNearZero() {
        val user = UserFactory.createUser(Difficulty.EASY)
        val storage = Optional.of("{\"formula\":\"hello\"}")

        val updated = GameManager.onLocalStorageUpdated(user, storage)

        assertTrue(updated.localStorageSnapshot.isPresent)
        assertTrue(updated.pausedTimerSerialized.isEmpty)
        assertTrue(Counter(updated.activePlayTimerSerialized).isActive())
        assertTrue(Counter(updated.nextPenaltyTimerSerialized).isActive())
    }

    @Test
    fun onLocalStorageUpdatedKeepsPausedWhenFormulaMissing() {
        val user = UserFactory.createUser(Difficulty.EASY)
        val storage = Optional.of("{\"formula\":\"\"}")

        val updated = GameManager.onLocalStorageUpdated(user, storage)

        assertTrue(updated.localStorageSnapshot.isPresent)
        assertTrue(updated.pausedTimerSerialized.isPresent)
        assertFalse(Counter(updated.activePlayTimerSerialized).isActive())
        assertFalse(Counter(updated.nextPenaltyTimerSerialized).isActive())
    }

    @Test
    fun onLocalStorageUpdatedKeepsPausedWhenActivePlayNotNearZero() {
        val active = Counter(null).apply {
            resume()
            moveTimeBack(1)
            getTotalSeconds()
            pause()
        }.serialize()
        val user = UserFactory.createUser(Difficulty.EASY).copy(
            activePlayTimerSerialized = active
        )

        val updated = GameManager.onLocalStorageUpdated(user, Optional.of("{\"formula\":\"ok\"}"))

        assertTrue(updated.pausedTimerSerialized.isPresent)
        assertFalse(Counter(updated.activePlayTimerSerialized).isActive())
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

        val updated = GameManager.onSleepScheduleChanged(user, true, 22 * 60, 6 * 60)

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

        val updated = GameManager.evaluateAlerts(user)

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

        val updated = GameManager.evaluateAlerts(user)

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

        val updated = GameManager.evaluateAlerts(user)

        assertEquals(AlertType.Reminder, updated.nextAlertType)
        assertTrue(updated.pendingNotificationLogsNewestFirst.isNotEmpty())
        assertFalse(user.nextPenaltyTimerSerialized == updated.nextPenaltyTimerSerialized)
    }

    @Test
    fun evaluateAlertsSkipsNudgeWhenNotSupported() {
        val user = userWithElapsedMinutes(
            elapsedMinutes = 70,
            difficulty = Difficulty.HARD,
            nextAlertType = AlertType.Penalty,
            paused = false
        )

        val updated = GameManager.evaluateAlerts(user)

        assertEquals(updated.pendingNotificationLogsNewestFirst[0].first, "\uD83D\uDFE5 have missed the review!")
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

        val updated = GameManager.evaluateAlerts(user)

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

        val updated = GameManager.evaluateAlerts(user)

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
}
