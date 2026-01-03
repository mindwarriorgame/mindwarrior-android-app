package com.mindwarrior.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mindwarrior.app.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val enabled = UserStorage.getUser(this).timerForegroundEnabled
        binding.timerForegroundSwitch.isChecked = enabled
        binding.timerForegroundSwitch.setOnCheckedChangeListener { _, isChecked ->
            val user = UserStorage.getUser(this)
            UserStorage.upsertUser(this, user.copy(timerForegroundEnabled = isChecked))
            if (isChecked) {
                BattleTimerStickyForegroundServiceController.start(this)
            } else {
                BattleTimerStickyForegroundServiceController.stop(this)
            }
        }

        binding.settingsClose.setOnClickListener {
            finish()
        }
    }
}
