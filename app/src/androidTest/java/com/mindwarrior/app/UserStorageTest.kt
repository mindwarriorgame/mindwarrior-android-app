package com.mindwarrior.app

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.os.SystemClock
import com.mindwarrior.app.engine.AlertType
import com.mindwarrior.app.engine.Counter
import com.mindwarrior.app.engine.Difficulty
import com.mindwarrior.app.engine.GameManager
import com.mindwarrior.app.engine.User
import com.mindwarrior.app.engine.UserFactory
import java.util.Optional
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
            // Penalty is issued when it reaches review threshold, then it got reset
            nextPenaltyTimerSerialized = "{\"is_active\":false}",
            nextAlertType = AlertType.Reminder,
            timerForegroundEnabled = true,
            nextSleepEventAtMillis = Optional.of(1234567899876543L),
            sleepStartMinutes = 22 * 60,
            sleepEndMinutes = 6 * 60,
            difficulty = Difficulty.HARD,
            localStorageSnapshot = Optional.of("{\"a\":\"b\"}"),
            pendingNotificationLogsNewestFirst = listOf(
                Pair("{\"log\":\"pending1\"}", 1712300000L),
                Pair("{\"log\":\"pending2\"}", 1712300001L)
            ),
            unseenLogsNewestFirst = listOf(
                Pair("{\"log\":\"unseen\"}", 1712311111L)
            ),
            oldLogsNewestFirst = listOf(
                Pair("{\"log\":\"old1\"}", 1712322222L),
                Pair("{\"log\":\"old2\"}", 1712323333L)
            )
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
            nextPenaltyTimerSerialized = "{\"is_active\":false}",
            nextAlertType = AlertType.Penalty,
            timerForegroundEnabled = false,
            nextSleepEventAtMillis = Optional.empty(),
            sleepStartMinutes = 23 * 60,
            sleepEndMinutes = 7 * 60,
            difficulty = Difficulty.BEGINNER,
            localStorageSnapshot = Optional.empty(),
            pendingNotificationLogsNewestFirst = emptyList(),
            unseenLogsNewestFirst = emptyList(),
            oldLogsNewestFirst = emptyList()
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
        assertTrue(loaded.nextPenaltyTimerSerialized.isNotBlank())
        assertTrue(Counter(loaded.pausedTimerSerialized.get()).isActive())
        assertTrue(Counter(loaded.activePlayTimerSerialized).isActive())
        assertFalse(Counter(loaded.nextPenaltyTimerSerialized).isActive())
        assertEquals(-100_000L, loaded.lastRewardAtActivePlayTime)
        assertEquals(AlertType.Reminder, loaded.nextAlertType)
        assertTrue(loaded.pausedTimerSerialized.isPresent)
        assertTrue(!loaded.timerForegroundEnabled)
        assertTrue(!loaded.nextSleepEventAtMillis.isPresent)
        assertEquals(23 * 60, loaded.sleepStartMinutes)
        assertEquals(7 * 60, loaded.sleepEndMinutes)
        assertEquals(Difficulty.EASY, loaded.difficulty)
        assertTrue(!loaded.localStorageSnapshot.isPresent)
        assertEquals(emptyList<Pair<String, Long>>(), loaded.pendingNotificationLogsNewestFirst)
        assertEquals(emptyList<Pair<String, Long>>(), loaded.unseenLogsNewestFirst)
        assertEquals(1, loaded.oldLogsNewestFirst.size)
        assertEquals(WELCOME_MESSAGE, loaded.oldLogsNewestFirst.first().first)
    }

    @Test
    fun pausedTimerDefaultsWhenPreferenceMissing() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        val loaded = UserStorage.getUser(context)

        assertTrue(loaded.pausedTimerSerialized.isPresent)
        assertTrue(Counter(loaded.pausedTimerSerialized.get()).isActive())
    }

    @Test
    fun pausedTimerEmptyWhenPreferenceIsEmptyString() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_PAUSED_TIMER_SERIALIZED, "empty")
            .apply()

        val loaded = UserStorage.getUser(context)

        assertFalse(loaded.pausedTimerSerialized.isPresent)
    }

    @Test
    fun pausedTimerRestoresWhenPreferenceHasValue() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val paused = Counter(null).apply { resume() }.serialize()
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_PAUSED_TIMER_SERIALIZED, paused)
            .apply()

        val loaded = UserStorage.getUser(context)

        assertTrue(loaded.pausedTimerSerialized.isPresent)
        assertTrue(Counter(loaded.pausedTimerSerialized.get()).isActive())
    }

    @Test
    fun pausedTimerEmptyRoundTripStoresEmptyMarker() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val user = UserStorage.getUser(context).copy(pausedTimerSerialized = Optional.empty())

        UserStorage.upsertUser(context, user)

        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        assertEquals("empty", prefs.getString(KEY_PAUSED_TIMER_SERIALIZED, null))
        val loaded = UserStorage.getUser(context)
        assertFalse(loaded.pausedTimerSerialized.isPresent)
    }

    @Test
    fun getUserPersistsEvaluatedAlertsWhenChanged() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val elapsedMinutes = 1700
        val timerSerialized = Counter(null).resume().apply {
            moveTimeBack(elapsedMinutes.toLong())
            getTotalSeconds()
        }.serialize()
        val user = UserFactory.createUser(Difficulty.EASY).copy(
            pausedTimerSerialized = Optional.empty(),
            nextPenaltyTimerSerialized = timerSerialized,
            nextAlertType = AlertType.Reminder,
            difficulty = Difficulty.EASY,
            pendingNotificationLogsNewestFirst = emptyList(),
            nextSleepEventAtMillis = Optional.empty()
        )
        UserStorage.upsertUser(context, user)

        val updated = UserStorage.getUser(context)

        assertTrue(updated.pendingNotificationLogsNewestFirst.isNotEmpty())
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val updatedPersisted = waitForAlertType(prefs, AlertType.Penalty.name)
        assertTrue(updatedPersisted)
        assertTrue(waitForPendingLogs(prefs))
    }

    companion object {
        private const val PREFS_NAME = "mindwarrior_user"
        private const val KEY_ACTIVE_PLAY_TIMER_SERIALIZED = "active_play_timer_serialized"
        private const val KEY_PAUSED_TIMER_SERIALIZED = "paused_timer_serialized"
        private const val KEY_NEXT_ALERT_TYPE = "next_alert_type"
        private const val KEY_PENDING_NOTIFICATION_LOGS_NEWEST_FIRST =
            "pending_notification_logs_newest_first"
        private const val WELCOME_MESSAGE =
            "👋 Welcome to MindWarrior game! 🥷 Tap 🧪 to enter your Formula of Firm Resolution and start playing!"
    }

    private fun waitForAlertType(
        prefs: android.content.SharedPreferences,
        expected: String
    ): Boolean {
        val deadline = SystemClock.uptimeMillis() + 2_000L
        while (SystemClock.uptimeMillis() < deadline) {
            val current = prefs.getString(KEY_NEXT_ALERT_TYPE, null)
            if (current == expected) {
                return true
            }
            SystemClock.sleep(25)
        }
        return false
    }

    private fun waitForPendingLogs(prefs: android.content.SharedPreferences): Boolean {
        val deadline = SystemClock.uptimeMillis() + 2_000L
        while (SystemClock.uptimeMillis() < deadline) {
            if (prefs.getString(KEY_PENDING_NOTIFICATION_LOGS_NEWEST_FIRST, "[]") != "[]") {
                return true
            }
            SystemClock.sleep(25)
        }
        return false
    }
}
