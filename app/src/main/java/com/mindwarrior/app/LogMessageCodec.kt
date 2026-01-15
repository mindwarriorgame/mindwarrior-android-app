package com.mindwarrior.app

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import com.mindwarrior.app.engine.Difficulty
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.Locale

object LogMessageKeys {
    const val DIFFICULTY_CHANGED = "difficulty_changed"
    const val SLEEP_SCHEDULE_ENABLED = "sleep_schedule_enabled"
    const val SLEEP_SCHEDULE_DISABLED = "sleep_schedule_disabled"
    const val GAME_PAUSED = "game_paused"
    const val GAME_RESUMED = "game_resumed"
    const val GAME_STARTED = "game_started"
    const val FORMULA_UPDATED = "formula_updated"
    const val NEW_BADGE = "new_badge"
    const val GRUMPY_BLOCKING = "grumpy_blocking"
    const val REVIEW_COMPLETED = "review_completed"
    const val REVIEW_REWARD = "review_reward"
    const val REVIEW_NO_REWARD = "review_no_reward"
    const val GRUMPY_REMOVED = "grumpy_removed"
    const val GRUMPY_REMOVED_WITH_REMAINING = "grumpy_removed_with_remaining"
    const val GRUMPY_REMOVED_WITH_UNBLOCKED = "grumpy_removed_with_unblocked"
    const val GRUMPY_REMAINING = "grumpy_remaining"
    const val ACHIEVEMENTS_UNBLOCKED = "achievements_unblocked"
    const val GRUMPY_EXPELLING = "grumpy_expelling"
    const val PROMPT_REMINDER = "prompt_reminder"
    const val PROMPT_PENALTY = "prompt_penalty"
    const val GRUMPY_SNEAKED_IN = "grumpy_sneaked_in"
    const val SLEEP_STARTED = "sleep_started"
    const val SLEEP_RESUMED = "sleep_resumed"
    const val REPELLER_USED = "repeller_used"
    const val WELCOME_MESSAGE = "welcome_message"
}

object LogMessageCodec {
    private const val PREFIX = "log:"
    private const val SEPARATOR = "|"
    private const val ENTRY_SEPARATOR = "\n\n"

    fun encode(key: String, vararg args: String): String {
        val encodedArgs = args.map { URLEncoder.encode(it, "UTF-8") }
        return buildString {
            append(PREFIX)
            append(key)
            encodedArgs.forEach { arg ->
                append(SEPARATOR)
                append(arg)
            }
        }
    }

    fun translate(context: Context, message: String): String {
        val parts = message.split(ENTRY_SEPARATOR)
        val translated = parts.map { part ->
            translateSingle(context, part) ?: part
        }
        return translated.joinToString(ENTRY_SEPARATOR)
    }

    private fun translateSingle(context: Context, message: String): String? {
        if (!message.startsWith(PREFIX)) return null
        val decoded = decode(message) ?: return null
        val localized = getLocalizedContext(context)
        val args = decoded.second
        return when (decoded.first) {
            LogMessageKeys.DIFFICULTY_CHANGED -> {
                val oldLabel = difficultyLabel(localized, args.getOrNull(0))
                val newLabel = difficultyLabel(localized, args.getOrNull(1))
                localized.getString(R.string.log_difficulty_changed, oldLabel, newLabel)
            }
            LogMessageKeys.SLEEP_SCHEDULE_ENABLED -> {
                val start = formatMinutes(localized, args.getOrNull(0))
                val end = formatMinutes(localized, args.getOrNull(1))
                localized.getString(R.string.log_sleep_schedule_enabled, start, end)
            }
            LogMessageKeys.SLEEP_SCHEDULE_DISABLED ->
                localized.getString(R.string.log_sleep_schedule_disabled)
            LogMessageKeys.GAME_PAUSED -> localized.getString(R.string.log_game_paused)
            LogMessageKeys.GAME_RESUMED -> localized.getString(R.string.log_game_resumed)
            LogMessageKeys.GAME_STARTED -> localized.getString(R.string.log_game_started)
            LogMessageKeys.FORMULA_UPDATED -> localized.getString(R.string.log_formula_updated)
            LogMessageKeys.NEW_BADGE -> localized.getString(R.string.log_new_badge)
            LogMessageKeys.GRUMPY_BLOCKING -> localized.getString(R.string.log_grumpy_blocking)
            LogMessageKeys.REVIEW_COMPLETED -> {
                val hours = args.getOrNull(0)?.toIntOrNull() ?: 0
                val minutes = args.getOrNull(1)?.toIntOrNull() ?: 0
                localized.getString(R.string.log_review_completed, hours, minutes)
            }
            LogMessageKeys.REVIEW_REWARD -> localized.getString(R.string.log_review_reward)
            LogMessageKeys.REVIEW_NO_REWARD -> localized.getString(R.string.log_review_no_reward)
            LogMessageKeys.GRUMPY_REMOVED -> localized.getString(R.string.log_grumpy_removed)
            LogMessageKeys.GRUMPY_REMOVED_WITH_REMAINING -> {
                val remaining = args.getOrNull(0)?.toIntOrNull() ?: 0
                localized.getString(R.string.log_grumpy_removed) + " " +
                    localized.getString(R.string.log_grumpy_remaining, remaining)
            }
            LogMessageKeys.GRUMPY_REMOVED_WITH_UNBLOCKED ->
                localized.getString(R.string.log_grumpy_removed) + " " +
                    localized.getString(R.string.log_achievements_unblocked)
            LogMessageKeys.GRUMPY_REMAINING -> {
                val remaining = args.getOrNull(0)?.toIntOrNull() ?: 0
                localized.getString(R.string.log_grumpy_remaining, remaining)
            }
            LogMessageKeys.ACHIEVEMENTS_UNBLOCKED ->
                localized.getString(R.string.log_achievements_unblocked)
            LogMessageKeys.GRUMPY_EXPELLING -> localized.getString(R.string.log_grumpy_expelling)
            LogMessageKeys.PROMPT_REMINDER -> localized.getString(R.string.log_prompt_reminder)
            LogMessageKeys.PROMPT_PENALTY -> localized.getString(R.string.log_prompt_penalty)
            LogMessageKeys.GRUMPY_SNEAKED_IN -> localized.getString(R.string.log_grumpy_sneaked_in)
            LogMessageKeys.SLEEP_STARTED -> localized.getString(R.string.log_sleep_started)
            LogMessageKeys.SLEEP_RESUMED -> localized.getString(R.string.log_sleep_resumed)
            LogMessageKeys.REPELLER_USED -> localized.getString(R.string.log_repeller_used)
            LogMessageKeys.WELCOME_MESSAGE -> localized.getString(R.string.log_welcome_message)
            else -> null
        }
    }

    private fun decode(message: String): Pair<String, List<String>>? {
        val parts = message.split(SEPARATOR)
        if (parts.isEmpty()) return null
        val key = parts[0].removePrefix(PREFIX)
        val args = parts.drop(1).map { URLDecoder.decode(it, "UTF-8") }
        return Pair(key, args)
    }

    private fun difficultyLabel(context: Context, id: String?): String {
        val difficulty = Difficulty.values().firstOrNull { it.id == id }
            ?: Difficulty.BEGINNER
        return context.getString(difficulty.labelRes)
    }

    private fun formatMinutes(context: Context, value: String?): String {
        val minutes = value?.toIntOrNull() ?: 0
        val hour = minutes / 60
        val minute = minutes % 60
        return String.format(getLocale(context), "%02d:%02d", hour, minute)
    }

    private fun getLocale(context: Context): Locale {
        val tag = LanguageManager.getCurrentLanguageTag(context)
        return Locale.forLanguageTag(tag)
    }

    private fun getLocalizedContext(context: Context): Context {
        val locale = getLocale(context)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(locale))
        }
        return context.createConfigurationContext(config)
    }
}
