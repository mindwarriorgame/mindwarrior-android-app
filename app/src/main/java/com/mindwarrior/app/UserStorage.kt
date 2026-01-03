package com.mindwarrior.app

import android.content.Context
import com.mindwarrior.app.engine.AlertType
import com.mindwarrior.app.engine.Counter
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
        return User(
            pausedTimerSerialized = pausedTimerOptional,
            activePlayTimerSerialized = activePlayTimer,
            lastRewardAtActivePlayTime = lastRewardAtActivePlayTime,
            reviewTimerSerialized = reviewTimer,
            nextAlertType = nextAlertType
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
        if (user.pausedTimerSerialized.isPresent) {
            editor.putString(KEY_PAUSED_TIMER_SERIALIZED, user.pausedTimerSerialized.get())
        } else {
            editor.remove(KEY_PAUSED_TIMER_SERIALIZED)
        }
        editor.apply()
        notifyUserUpdated(user)
    }

    private fun defaultUser(): User {
        return User(
            pausedTimerSerialized = Optional.empty(),
            activePlayTimerSerialized = Counter(null).serialize(),
            lastRewardAtActivePlayTime = 0L,
            reviewTimerSerialized = Counter(null).serialize(),
            nextAlertType = AlertType.Reminder
        )
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
