package com.mindwarrior.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mindwarrior.app.databinding.ActivityDifficultyBinding
import com.mindwarrior.app.notifications.OneOffAlertController
import com.mindwarrior.app.engine.Difficulty
import com.mindwarrior.app.engine.GameManager

class DifficultyActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDifficultyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDifficultyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        var current = UserStorage.getUser(this).difficulty
        when (current) {
            Difficulty.BEGINNER -> binding.difficultyBeginner.isChecked = true
            Difficulty.EASY -> binding.difficultyEasy.isChecked = true
            Difficulty.MEDIUM -> binding.difficultyMedium.isChecked = true
            Difficulty.HARD -> binding.difficultyHard.isChecked = true
            Difficulty.EXPORT -> binding.difficultyExport.isChecked = true
        }
        updateDifficultyInfo(current)

        binding.difficultyGroup.setOnCheckedChangeListener { _, checkedId ->
            val selected = when (checkedId) {
                R.id.difficulty_beginner -> Difficulty.BEGINNER
                R.id.difficulty_easy -> Difficulty.EASY
                R.id.difficulty_medium -> Difficulty.MEDIUM
                R.id.difficulty_hard -> Difficulty.HARD
                R.id.difficulty_export -> Difficulty.EXPORT
                else -> Difficulty.BEGINNER
            }
            if (selected != current) {
                val user = UserStorage.getUser(this)
                UserStorage.upsertUser(this, GameManager.onDifficultyChanged(user, selected))
                OneOffAlertController.restart(this)
                current = selected
            }
            updateDifficultyInfo(selected)
        }

        binding.doneButton.setOnClickListener {
            finish()
        }
    }

    private fun updateDifficultyInfo(difficulty: Difficulty) {
        val infoRes = when (difficulty) {
            Difficulty.BEGINNER -> R.string.difficulty_info_beginner
            Difficulty.EASY -> R.string.difficulty_info_easy
            Difficulty.MEDIUM -> R.string.difficulty_info_medium
            Difficulty.HARD -> R.string.difficulty_info_hard
            Difficulty.EXPORT -> R.string.difficulty_info_export
        }
        binding.difficultyInfo.text = getString(infoRes)
    }
}
