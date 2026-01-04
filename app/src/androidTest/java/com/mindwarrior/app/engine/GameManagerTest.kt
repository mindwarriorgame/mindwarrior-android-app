package com.mindwarrior.app.engine

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.Optional
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

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
            reviewTimerSerialized = review
        )

        val updated = GameManager.onPaused(user)

        assertTrue(updated.pausedTimerSerialized.isPresent)
        assertFalse(Counter(updated.activePlayTimerSerialized).isActive())
        assertFalse(Counter(updated.reviewTimerSerialized).isActive())
    }

    @Test
    fun onResumeUnpausesWhenPaused() {
        val user = UserFactory.createUser(Difficulty.EASY)

        val updated = GameManager.onResume(user)

        assertFalse(updated.pausedTimerSerialized.isPresent)
        assertTrue(Counter(updated.activePlayTimerSerialized).isActive())
        assertTrue(Counter(updated.reviewTimerSerialized).isActive())
    }

    @Test
    fun onLocalStorageUpdatedStartsGameWhenFormulaPresentAndNearZero() {
        val user = UserFactory.createUser(Difficulty.EASY)
        val storage = Optional.of("{\"formula\":\"hello\"}")

        val updated = GameManager.onLocalStorageUpdated(user, storage)

        assertTrue(updated.localStorageSnapshot.isPresent)
        assertTrue(updated.pausedTimerSerialized.isEmpty)
        assertTrue(Counter(updated.activePlayTimerSerialized).isActive())
        assertTrue(Counter(updated.reviewTimerSerialized).isActive())
    }

    @Test
    fun onLocalStorageUpdatedKeepsPausedWhenFormulaMissing() {
        val user = UserFactory.createUser(Difficulty.EASY)
        val storage = Optional.of("{\"formula\":\"\"}")

        val updated = GameManager.onLocalStorageUpdated(user, storage)

        assertTrue(updated.localStorageSnapshot.isPresent)
        assertTrue(updated.pausedTimerSerialized.isPresent)
        assertFalse(Counter(updated.activePlayTimerSerialized).isActive())
        assertFalse(Counter(updated.reviewTimerSerialized).isActive())
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

        val updated = GameManager.onDifficultyChanged(user, Difficulty.HARD)

        assertEquals(Difficulty.HARD, updated.difficulty)
        assertTrue(updated.pausedTimerSerialized.isPresent)
    }

    @Test
    fun onDifficultyChangedKeepsPausedWhenPaused() {
        val user = UserFactory.createUser(Difficulty.EASY)

        val updated = GameManager.onDifficultyChanged(user, Difficulty.HARD)

        assertEquals(Difficulty.HARD, updated.difficulty)
        assertTrue(updated.pausedTimerSerialized.isPresent)
        assertFalse(Counter(updated.activePlayTimerSerialized).isActive())
        assertFalse(Counter(updated.reviewTimerSerialized).isActive())
    }

    @Test
    fun onSleepScheduleChangedUpdatesFields() {
        val user = UserFactory.createUser(Difficulty.EASY)

        val updated = GameManager.onSleepScheduleChanged(user, true, 22 * 60, 6 * 60)

        assertTrue(updated.sleepEnabled)
        assertEquals(22 * 60, updated.sleepStartMinutes)
        assertEquals(6 * 60, updated.sleepEndMinutes)
    }
}
