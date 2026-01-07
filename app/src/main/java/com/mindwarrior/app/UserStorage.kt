package com.mindwarrior.app

import android.content.Context
import com.mindwarrior.app.engine.AlertType
import com.mindwarrior.app.engine.Difficulty
import com.mindwarrior.app.engine.GameManager
import com.mindwarrior.app.engine.User
import com.mindwarrior.app.engine.UserFactory
import java.util.Optional
import android.os.Handler
import android.os.Looper
import java.lang.ref.WeakReference
import android.os.Build
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

object UserStorage {
    private const val PREFS_NAME = "mindwarrior_user"
    private const val PREFS_NAME_DEVICE = "mindwarrior_user_device"
    private const val KEY_PAUSED_TIMER_SERIALIZED = "paused_timer_serialized"
    private const val KEY_ACTIVE_PLAY_TIMER_SERIALIZED = "active_play_timer_serialized"
    private const val KEY_LAST_REWARD_AT_ACTIVE_PLAY_TIME = "last_reward_at_active_play_time"
    private const val KEY_REVIEW_TIMER_SERIALIZED = "review_timer_serialized"
    private const val KEY_NEXT_ALERT_TYPE = "next_alert_type"
    private const val KEY_TIMER_FOREGROUND_ENABLED = "timer_foreground_enabled"
    private const val KEY_NEXT_SLEEP_EVENT_AT_MILLIS = "next_sleep_event_at_millis"
    private const val KEY_SLEEP_START_MINUTES = "sleep_start_minutes"
    private const val KEY_SLEEP_END_MINUTES = "sleep_end_minutes"
    private const val KEY_DIFFICULTY = "difficulty"
    private const val KEY_LOCAL_STORAGE = "local_storage_snapshot"
    private const val KEY_DIAMONDS = "diamonds"
    private const val KEY_DIAMONDS_SPENT = "diamonds_spent"
    private const val KEY_PENDING_NOTIFICATION_LOGS_NEWEST_FIRST =
        "pending_notification_logs_newest_first"
    private const val KEY_UNSEEN_LOGS_NEWEST_FIRST = "unseen_logs_newest_first"
    private const val KEY_OLD_LOGS_NEWEST_FIRST = "old_logs_newest_first"
    private val userUpdateListeners = mutableListOf<WeakReference<UserUpdateListener>>()

    fun getUser(context: Context): User {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val defaults = UserFactory.createUser(Difficulty.EASY)

        val hasAnyLogs = prefs.contains(KEY_OLD_LOGS_NEWEST_FIRST) ||
            prefs.contains(KEY_UNSEEN_LOGS_NEWEST_FIRST) ||
            prefs.contains(KEY_PENDING_NOTIFICATION_LOGS_NEWEST_FIRST)
        val isNewUser = !hasAnyLogs

        val activePlayTimerSerialized = prefs.getString(
            KEY_ACTIVE_PLAY_TIMER_SERIALIZED,
            defaults.activePlayTimerSerialized
        ) ?: defaults.activePlayTimerSerialized


        val reviewTimerSerialized = prefs.getString(
            KEY_REVIEW_TIMER_SERIALIZED,
            defaults.nextPenaltyTimerSerialized
        ) ?: defaults.nextPenaltyTimerSerialized
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
        val nextSleepEventAtMillis = if (prefs.contains(KEY_NEXT_SLEEP_EVENT_AT_MILLIS)) {
            Optional.of(prefs.getLong(KEY_NEXT_SLEEP_EVENT_AT_MILLIS, 0L))
        } else {
            Optional.empty()
        }
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
        val diamonds = prefs.getInt(KEY_DIAMONDS, defaults.diamonds)
        val diamondsSpent = prefs.getInt(KEY_DIAMONDS_SPENT, defaults.diamondsSpent)
        val pendingNotificationLogsNewestFirst = deserializeLogList(
            prefs.getString(KEY_PENDING_NOTIFICATION_LOGS_NEWEST_FIRST, null),
            defaults.pendingNotificationLogsNewestFirst
        )
        val unseenLogsNewestFirst = deserializeLogList(
            prefs.getString(KEY_UNSEEN_LOGS_NEWEST_FIRST, null),
            defaults.unseenLogsNewestFirst
        )
        val oldLogsNewestFirst = deserializeLogList(
            prefs.getString(KEY_OLD_LOGS_NEWEST_FIRST, null),
            defaults.oldLogsNewestFirst
        )
        val user = User(
            pausedTimerSerialized = maybePausedTimerSerialized,
            activePlayTimerSerialized = activePlayTimerSerialized,
            lastRewardAtActivePlayTime = lastRewardAtActivePlayTime,
            nextPenaltyTimerSerialized = reviewTimerSerialized,
            nextAlertType = nextAlertType,
            timerForegroundEnabled = timerForegroundEnabled,
            nextSleepEventAtMillis = nextSleepEventAtMillis,
            sleepStartMinutes = sleepStartMinutes,
            sleepEndMinutes = sleepEndMinutes,
            difficulty = difficulty,
            localStorageSnapshot = localStorageOptional,
            diamonds = diamonds,
            diamondsSpent = diamondsSpent,
            pendingNotificationLogsNewestFirst = pendingNotificationLogsNewestFirst,
            unseenLogsNewestFirst = unseenLogsNewestFirst,
            oldLogsNewestFirst = if (isNewUser) {
                listOf(Pair(WELCOME_MESSAGE, NowProvider.nowMillis()))
            } else {
                oldLogsNewestFirst
            }
        )

        val updatedUser = GameManager.evaluateAlerts(user)
        if (updatedUser == user && !isNewUser) {
            return user
        }
        if (isNewUser) {
            upsertUserInternal(context.applicationContext, updatedUser, false)
        } else {
            upsertUser(context.applicationContext, updatedUser)
        }
        return updatedUser
    }

    fun observeUserChanges(context: Context, listener: UserUpdateListener) {
        userUpdateListeners.add(WeakReference(listener))
        pruneListeners()
        removeDuplicates()
    }

    fun upsertUser(context: Context, user: User) {
        upsertUserInternal(context, user, true)
    }

    private fun upsertUserInternal(
        context: Context,
        user: User,
        shouldTriggerObservers: Boolean
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString(KEY_ACTIVE_PLAY_TIMER_SERIALIZED, user.activePlayTimerSerialized)
        editor.putString(KEY_REVIEW_TIMER_SERIALIZED, user.nextPenaltyTimerSerialized)
        editor.putLong(KEY_LAST_REWARD_AT_ACTIVE_PLAY_TIME, user.lastRewardAtActivePlayTime)
        editor.putString(KEY_NEXT_ALERT_TYPE, user.nextAlertType.name)
        editor.putBoolean(KEY_TIMER_FOREGROUND_ENABLED, user.timerForegroundEnabled)
        if (user.nextSleepEventAtMillis.isPresent) {
            editor.putLong(KEY_NEXT_SLEEP_EVENT_AT_MILLIS, user.nextSleepEventAtMillis.get())
        } else {
            editor.remove(KEY_NEXT_SLEEP_EVENT_AT_MILLIS)
        }
        editor.putInt(KEY_SLEEP_START_MINUTES, user.sleepStartMinutes)
        editor.putInt(KEY_SLEEP_END_MINUTES, user.sleepEndMinutes)
        editor.putString(KEY_DIFFICULTY, user.difficulty.id)
        if (user.localStorageSnapshot.isPresent) {
            editor.putString(KEY_LOCAL_STORAGE, user.localStorageSnapshot.get())
        } else {
            editor.remove(KEY_LOCAL_STORAGE)
        }
        editor.putInt(KEY_DIAMONDS, user.diamonds)
        editor.putInt(KEY_DIAMONDS_SPENT, user.diamondsSpent)
        editor.putString(
            KEY_PENDING_NOTIFICATION_LOGS_NEWEST_FIRST,
            serializeLogList(user.pendingNotificationLogsNewestFirst)
        )
        editor.putString(
            KEY_UNSEEN_LOGS_NEWEST_FIRST,
            serializeLogList(user.unseenLogsNewestFirst)
        )
        editor.putString(
            KEY_OLD_LOGS_NEWEST_FIRST,
            serializeLogList(user.oldLogsNewestFirst)
        )
        if (user.pausedTimerSerialized.isPresent) {
            editor.putString(KEY_PAUSED_TIMER_SERIALIZED, user.pausedTimerSerialized.get())
        } else {
            editor.putString(KEY_PAUSED_TIMER_SERIALIZED, "empty")
        }
        editor.apply()
        storeTimerForegroundEnabled(context, user.timerForegroundEnabled)
        if (shouldTriggerObservers) {
            Handler(Looper.getMainLooper()).post {
                notifyUserUpdated(user)
            }
        }
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

    private fun removeDuplicates() {
        val seen = mutableSetOf<UserUpdateListener>()
        val iterator = userUpdateListeners.iterator()
        while (iterator.hasNext()) {
            val listener = iterator.next().get()
            if (listener == null) {
                iterator.remove()
            } else if (!seen.add(listener)) {
                iterator.remove()
            }
        }
    }

    private fun serializeLogList(logs: List<Pair<String, Long>>): String {
        val array = JSONArray()
        for (log in logs) {
            val item = JSONObject()
            item.put("text", log.first)
            item.put("epochSecs", log.second)
            array.put(item)
        }
        return array.toString()
    }

    fun isTimerForegroundEnabledDevice(context: Context): Boolean {
        val deviceContext = if (Build.VERSION.SDK_INT >= 24) {
            context.createDeviceProtectedStorageContext()
        } else {
            context
        }
        return deviceContext
            .getSharedPreferences(PREFS_NAME_DEVICE, Context.MODE_PRIVATE)
            .getBoolean(KEY_TIMER_FOREGROUND_ENABLED, false)
    }

    private fun storeTimerForegroundEnabled(context: Context, enabled: Boolean) {
        if (Build.VERSION.SDK_INT < 24) {
            return
        }
        val deviceContext = context.createDeviceProtectedStorageContext()
        deviceContext
            .getSharedPreferences(PREFS_NAME_DEVICE, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_TIMER_FOREGROUND_ENABLED, enabled)
            .apply()
    }

    private fun deserializeLogList(
        raw: String?,
        fallback: List<Pair<String, Long>>
    ): List<Pair<String, Long>> {
        if (raw.isNullOrBlank()) {
            return fallback
        }
        return try {
            val array = JSONArray(raw)
            val logs = ArrayList<Pair<String, Long>>(array.length())
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val text = item.optString("text", "")
                val epochSecs = item.optLong("epochSecs", 0L)
                logs.add(Pair(text, epochSecs))
            }
            logs
        } catch (ex: JSONException) {
            fallback
        }
    }

    interface UserUpdateListener {
        fun onUserUpdated(user: User)
    }

    private const val WELCOME_MESSAGE =
        "👋 Welcome to MindWarrior game! 🥷 Tap 🧪 to enter your Formula of Firm Resolution and start playing!"

}
