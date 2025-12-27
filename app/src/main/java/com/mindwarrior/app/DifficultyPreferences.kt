package com.mindwarrior.app

import android.content.Context
import androidx.annotation.StringRes

enum class Difficulty(val id: String, @StringRes val labelRes: Int) {
    BEGINNER("beginner", R.string.difficulty_beginner),
    EASY("easy", R.string.difficulty_easy),
    MEDIUM("medium", R.string.difficulty_medium),
    HARD("hard", R.string.difficulty_hard),
    EXPORT("export", R.string.difficulty_export)
}

object DifficultyPreferences {
    private const val PREFS_NAME = "mindwarrior_prefs"
    private const val KEY_DIFFICULTY = "difficulty"

    fun getDifficulty(context: Context): Difficulty {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stored = prefs.getString(KEY_DIFFICULTY, null)
        return Difficulty.values().firstOrNull { it.id == stored } ?: Difficulty.BEGINNER
    }

    fun setDifficulty(context: Context, difficulty: Difficulty) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_DIFFICULTY, difficulty.id).apply()
    }
}
