package com.mindwarrior.app

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mindwarrior.app.engine.AlertType
import com.mindwarrior.app.engine.Difficulty
import com.mindwarrior.app.engine.User
import java.util.Optional
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserStorageTest {

    @Before
    fun clearPrefs() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    @Test
    fun upsertAndLoadRoundTripWithPausedTimer() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val user = User(
            pausedTimerSerialized = Optional.of("{\"is_active\":false}"),
            activePlayTimerSerialized = "{\"is_active\":true}",
            lastRewardAtActivePlayTime = 1234L,
            reviewTimerSerialized = "{\"is_active\":false}",
            nextAlertType = AlertType.Reminder,
            timerForegroundEnabled = true,
            sleepEnabled = true,
            sleepStartMinutes = 22 * 60,
            sleepEndMinutes = 6 * 60,
            difficulty = Difficulty.HARD
        )

        UserStorage.upsertUser(context, user)
        val loaded = UserStorage.getUser(context)

        assertEquals(user, loaded)
    }

    @Test
    fun upsertAndLoadRoundTripWithoutPausedTimer() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val user = User(
            pausedTimerSerialized = Optional.empty(),
            activePlayTimerSerialized = "{\"is_active\":true}",
            lastRewardAtActivePlayTime = 42L,
            reviewTimerSerialized = "{\"is_active\":false}",
            nextAlertType = AlertType.WakeUp,
            timerForegroundEnabled = false,
            sleepEnabled = false,
            sleepStartMinutes = 23 * 60,
            sleepEndMinutes = 7 * 60,
            difficulty = Difficulty.BEGINNER
        )

        UserStorage.upsertUser(context, user)
        val loaded = UserStorage.getUser(context)

        assertEquals(user, loaded)
    }

    @Test
    fun getUserReturnsDefaultsWhenMissingRequiredFields() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_ACTIVE_PLAY_TIMER_SERIALIZED, "{\"is_active\":true}")
            .apply()

        val loaded = UserStorage.getUser(context)

        assertTrue(loaded.activePlayTimerSerialized.isNotBlank())
        assertTrue(loaded.reviewTimerSerialized.isNotBlank())
        assertEquals(0L, loaded.lastRewardAtActivePlayTime)
        assertEquals(AlertType.Reminder, loaded.nextAlertType)
        assertTrue(!loaded.pausedTimerSerialized.isPresent)
        assertTrue(!loaded.timerForegroundEnabled)
        assertTrue(!loaded.sleepEnabled)
        assertEquals(23 * 60, loaded.sleepStartMinutes)
        assertEquals(7 * 60, loaded.sleepEndMinutes)
        assertEquals(Difficulty.BEGINNER, loaded.difficulty)
    }

    companion object {
        private const val PREFS_NAME = "mindwarrior_user"
        private const val KEY_ACTIVE_PLAY_TIMER_SERIALIZED = "active_play_timer_serialized"
    }
}
