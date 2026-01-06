package com.mindwarrior.app.engine

import androidx.annotation.StringRes
import com.mindwarrior.app.R

enum class Difficulty(val id: String, @StringRes val labelRes: Int) {
    BEGINNER("beginner", R.string.difficulty_beginner),
    EASY("easy", R.string.difficulty_easy),
    MEDIUM("medium", R.string.difficulty_medium),
    HARD("hard", R.string.difficulty_hard),
    EXPORT("export", R.string.difficulty_export)
}

object DifficultyHelper {
    fun getReviewFrequencyMillis(difficulty: Difficulty): Long {
        val minutes = when (difficulty) {
            Difficulty.BEGINNER -> 6 * 60
            Difficulty.EASY -> 3 * 60
            Difficulty.MEDIUM -> 90
            Difficulty.HARD -> 60
            Difficulty.EXPORT -> 45
        }
        return minutes * 60_000L
    }

    fun hasNudge(difficulty: Difficulty): Boolean {
        return when (difficulty) {
            Difficulty.BEGINNER -> true
            Difficulty.EASY -> true
            Difficulty.MEDIUM -> true
            Difficulty.HARD -> false
            Difficulty.EXPORT -> false
        }
    }
}