package com.mindwarrior.app

import android.content.Context
import com.mindwarrior.app.engine.AlertType
import com.mindwarrior.app.engine.Difficulty
import com.mindwarrior.app.engine.User
import com.mindwarrior.app.engine.UserFactory
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
    private const val KEY_NEXT_REVIEW_DEADLINE_AT_MILLIS = "next_review_deadline_at_millis"
    private val userUpdateListeners = mutableListOf<WeakReference<UserUpdateListener>>()

    fun getUser(context: Context): User {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val defaults = UserFactory.createUser(Difficulty.EASY)
        val activePlayTimerSerialized = prefs.getString(
            KEY_ACTIVE_PLAY_TIMER_SERIALIZED,
            defaults.activePlayTimerSerialized
        ) ?: defaults.activePlayTimerSerialized
        val reviewTimerSerialized = prefs.getString(
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
        val maybePausedTimerSerialized = if (pausedTimerSerialized == null) {
            defaults.pausedTimerSerialized
        } else {
            if (pausedTimerSerialized == "empty") {
                Optional.empty()
            } else {
                Optional.of(pausedTimerSerialized)
            }
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
            defaults.localStorageSnapshot
        } else {
            Optional.of(localStorageSnapshot)
        }
        val nextReviewDeadlineAtMillis = if (prefs.contains(KEY_NEXT_REVIEW_DEADLINE_AT_MILLIS)) {
            prefs.getLong(
                KEY_NEXT_REVIEW_DEADLINE_AT_MILLIS,
                defaults.nextReviewDeadlineAtMillis
            )
        } else {
            defaults.nextReviewDeadlineAtMillis
        }
        return User(
            pausedTimerSerialized = maybePausedTimerSerialized,
            activePlayTimerSerialized = activePlayTimerSerialized,
            lastRewardAtActivePlayTime = lastRewardAtActivePlayTime,
            reviewTimerSerialized = reviewTimerSerialized,
            nextAlertType = nextAlertType,
            timerForegroundEnabled = timerForegroundEnabled,
            sleepEnabled = sleepEnabled,
            sleepStartMinutes = sleepStartMinutes,
            sleepEndMinutes = sleepEndMinutes,
            difficulty = difficulty,
            localStorageSnapshot = localStorageOptional,
            nextReviewDeadlineAtMillis = nextReviewDeadlineAtMillis
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
        editor.putLong(KEY_NEXT_REVIEW_DEADLINE_AT_MILLIS, user.nextReviewDeadlineAtMillis)
        if (user.localStorageSnapshot.isPresent) {
            editor.putString(KEY_LOCAL_STORAGE, user.localStorageSnapshot.get())
        } else {
            editor.remove(KEY_LOCAL_STORAGE)
        }
        if (user.pausedTimerSerialized.isPresent) {
            editor.putString(KEY_PAUSED_TIMER_SERIALIZED, user.pausedTimerSerialized.get())
        } else {
            editor.putString(KEY_PAUSED_TIMER_SERIALIZED, "empty")
        }
        editor.apply()
        notifyUserUpdated(user)
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
