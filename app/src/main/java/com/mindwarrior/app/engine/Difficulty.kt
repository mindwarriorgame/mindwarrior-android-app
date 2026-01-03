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