package com.mindwarrior.app

import android.content.Context
import com.mindwarrior.app.engine.AlertType
import com.mindwarrior.app.engine.User
import java.util.Optional

object UserStorage {
    private const val PREFS_NAME = "mindwarrior_user"
    private const val KEY_PAUSED_TIMER_SERIALIZED = "paused_timer_serialized"
    private const val KEY_ACTIVE_PLAY_TIMER_SERIALIZED = "active_play_timer_serialized"
    private const val KEY_LAST_REWARD_AT_ACTIVE_PLAY_TIME = "last_reward_at_active_play_time"
    private const val KEY_REVIEW_TIMER_SERIALIZED = "review_timer_serialized"
    private const val KEY_NEXT_ALERT_TYPE = "next_alert_type"

    fun getUser(context: Context): Optional<User> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val activePlayTimer = prefs.getString(KEY_ACTIVE_PLAY_TIMER_SERIALIZED, null)
            ?: return Optional.empty()
        val reviewTimer = prefs.getString(KEY_REVIEW_TIMER_SERIALIZED, null)
            ?: return Optional.empty()
        if (!prefs.contains(KEY_LAST_REWARD_AT_ACTIVE_PLAY_TIME)) {
            return Optional.empty()
        }
        val lastRewardAtActivePlayTime =
            prefs.getLong(KEY_LAST_REWARD_AT_ACTIVE_PLAY_TIME, 0L)
        val nextAlertRaw = prefs.getString(KEY_NEXT_ALERT_TYPE, null)
            ?: return Optional.empty()
        val nextAlertType = try {
            AlertType.valueOf(nextAlertRaw)
        } catch (ex: IllegalArgumentException) {
            return Optional.empty()
        }
        val pausedTimerSerialized = prefs.getString(KEY_PAUSED_TIMER_SERIALIZED, null)
        val pausedTimerOptional = if (pausedTimerSerialized == null) {
            Optional.empty()
        } else {
            Optional.of(pausedTimerSerialized)
        }
        return Optional.of(
            User(
                pausedTimerSerialized = pausedTimerOptional,
                activePlayTimerSerialized = activePlayTimer,
                lastRewardAtActivePlayTime = lastRewardAtActivePlayTime,
                reviewTimerSerialized = reviewTimer,
                nextAlertType = nextAlertType
            )
        )
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
    }
}
