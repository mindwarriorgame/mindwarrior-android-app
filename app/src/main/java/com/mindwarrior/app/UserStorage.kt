package com.mindwarrior.app

import android.content.Context
import com.mindwarrior.app.engine.AlertType
import com.mindwarrior.app.engine.Counter
import com.mindwarrior.app.engine.Difficulty
import com.mindwarrior.app.engine.User
import java.util.Optional
import java.lang.ref.WeakReference

object UserStorage {
    private const val PREFS_NAME = "mindwarrior_user"
    private const val KEY_PAUSED_TIMER_SERIALIZED = "paused_timer_serialized"
    private const val KEY_ACTIVE_PLAY_TIMER_SERIALIZED = "active_play_timer_serialized"
    private const val KEY_LAST_REWARD_AT_ACTIVE_PLAY_TIME = "last_reward_at_active_play_time"
    private const val KEY_REVIEW_TIMER_SERIALIZED = "review_timer_serialized"
    private const val KEY_NEXT_ALERT_TYPE = "next_alert_type"
    private const val KEY_TIMER_FOREGROUND_ENABLED = "timer_foreground_enabled"
    private const val KEY_SLEEP_ENABLED = "sleep_enabled"
    private const val KEY_SLEEP_START_MINUTES = "sleep_start_minutes"
    private const val KEY_SLEEP_END_MINUTES = "sleep_end_minutes"
    private const val KEY_DIFFICULTY = "difficulty"
    private const val KEY_LOCAL_STORAGE = "local_storage_snapshot"
    private val userUpdateListeners = mutableListOf<WeakReference<UserUpdateListener>>()

    fun getUser(context: Context): User {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val defaults = defaultUser()
        val activePlayTimer = prefs.getString(
            KEY_ACTIVE_PLAY_TIMER_SERIALIZED,
            defaults.activePlayTimerSerialized
        ) ?: defaults.activePlayTimerSerialized
        val reviewTimer = prefs.getString(
            KEY_REVIEW_TIMER_SERIALIZED,
            defaults.reviewTimerSerialized
        ) ?: defaults.reviewTimerSerialized
        val lastRewardAtActivePlayTime = if (prefs.contains(KEY_LAST_REWARD_AT_ACTIVE_PLAY_TIME)) {
            prefs.getLong(KEY_LAST_REWARD_AT_ACTIVE_PLAY_TIME, defaults.lastRewardAtActivePlayTime)
        } else {
            defaults.lastRewardAtActivePlayTime
        }
        val nextAlertRaw = prefs.getString(KEY_NEXT_ALERT_TYPE, defaults.nextAlertType.name)
        val nextAlertType = try {
            AlertType.valueOf(nextAlertRaw ?: defaults.nextAlertType.name)
        } catch (ex: IllegalArgumentException) {
            defaults.nextAlertType
        }
        val pausedTimerSerialized = prefs.getString(KEY_PAUSED_TIMER_SERIALIZED, null)
        val pausedTimerOptional = if (pausedTimerSerialized == null) {
            Optional.empty()
        } else {
            Optional.of(pausedTimerSerialized)
        }
        val timerForegroundEnabled = prefs.getBoolean(
            KEY_TIMER_FOREGROUND_ENABLED,
            defaults.timerForegroundEnabled
        )
        val sleepEnabled = prefs.getBoolean(KEY_SLEEP_ENABLED, defaults.sleepEnabled)
        val sleepStartMinutes = prefs.getInt(KEY_SLEEP_START_MINUTES, defaults.sleepStartMinutes)
        val sleepEndMinutes = prefs.getInt(KEY_SLEEP_END_MINUTES, defaults.sleepEndMinutes)
        val difficultyId = prefs.getString(KEY_DIFFICULTY, defaults.difficulty.id)
        val difficulty = difficultyFromId(difficultyId) ?: defaults.difficulty
        val localStorageSnapshot = prefs.getString(KEY_LOCAL_STORAGE, null)
        val localStorageOptional = if (localStorageSnapshot == null) {
            Optional.empty()
        } else {
            Optional.of(localStorageSnapshot)
        }
        return User(
            pausedTimerSerialized = pausedTimerOptional,
            activePlayTimerSerialized = activePlayTimer,
            lastRewardAtActivePlayTime = lastRewardAtActivePlayTime,
            reviewTimerSerialized = reviewTimer,
            nextAlertType = nextAlertType,
            timerForegroundEnabled = timerForegroundEnabled,
            sleepEnabled = sleepEnabled,
            sleepStartMinutes = sleepStartMinutes,
            sleepEndMinutes = sleepEndMinutes,
            difficulty = difficulty,
            localStorageSnapshot = localStorageOptional
        )
    }

    fun observeUserChanges(context: Context, listener: UserUpdateListener) {
        userUpdateListeners.add(WeakReference(listener))
        pruneListeners()
        listener.onUserUpdated(getUser(context))
    }

    fun upsertUser(context: Context, user: User) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString(KEY_ACTIVE_PLAY_TIMER_SERIALIZED, user.activePlayTimerSerialized)
        editor.putString(KEY_REVIEW_TIMER_SERIALIZED, user.reviewTimerSerialized)
        editor.putLong(KEY_LAST_REWARD_AT_ACTIVE_PLAY_TIME, user.lastRewardAtActivePlayTime)
        editor.putString(KEY_NEXT_ALERT_TYPE, user.nextAlertType.name)
        editor.putBoolean(KEY_TIMER_FOREGROUND_ENABLED, user.timerForegroundEnabled)
        editor.putBoolean(KEY_SLEEP_ENABLED, user.sleepEnabled)
        editor.putInt(KEY_SLEEP_START_MINUTES, user.sleepStartMinutes)
        editor.putInt(KEY_SLEEP_END_MINUTES, user.sleepEndMinutes)
        editor.putString(KEY_DIFFICULTY, user.difficulty.id)
        if (user.localStorageSnapshot.isPresent) {
            editor.putString(KEY_LOCAL_STORAGE, user.localStorageSnapshot.get())
        } else {
            editor.remove(KEY_LOCAL_STORAGE)
        }
        if (user.pausedTimerSerialized.isPresent) {
            editor.putString(KEY_PAUSED_TIMER_SERIALIZED, user.pausedTimerSerialized.get())
        } else {
            editor.remove(KEY_PAUSED_TIMER_SERIALIZED)
        }
        editor.apply()
        notifyUserUpdated(user)
    }

    private fun defaultUser(): User {
        val pausedTimer = Counter(null)
        pausedTimer.resume()
        val activePlayTimer = Counter(null)
        activePlayTimer.pause()
        val reviewTimerSerialized = Counter(null)
        reviewTimerSerialized.pause()
        val difficulty = Difficulty.EASY
        return User(
            pausedTimerSerialized = Optional.of(pausedTimer.serialize()),
            activePlayTimerSerialized = activePlayTimer.serialize(),
            lastRewardAtActivePlayTime = 0L,
            reviewTimerSerialized = reviewTimerSerialized.serialize(),
            nextAlertType = AlertType.Reminder,
            timerForegroundEnabled = false,
            sleepEnabled = false,
            sleepStartMinutes = 23 * 60,
            sleepEndMinutes = 7 * 60,
            difficulty = difficulty,
            localStorageSnapshot = Optional.empty()
        )
    }

    private fun difficultyFromId(id: String?): Difficulty? {
        return Difficulty.values().firstOrNull { it.id == id }
    }

    private fun notifyUserUpdated(user: User) {
        pruneListeners()
        val iterator = userUpdateListeners.iterator()
        while (iterator.hasNext()) {
            val listener = iterator.next().get()
            if (listener == null) {
                iterator.remove()
            } else {
                listener.onUserUpdated(user)
            }
        }
    }

    private fun pruneListeners() {
        val iterator = userUpdateListeners.iterator()
        while (iterator.hasNext()) {
            if (iterator.next().get() == null) {
                iterator.remove()
            }
        }
    }

    interface UserUpdateListener {
        fun onUserUpdated(user: User)
    }

}
