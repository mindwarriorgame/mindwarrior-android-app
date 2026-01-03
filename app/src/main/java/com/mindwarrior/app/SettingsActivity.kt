package com.mindwarrior.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.mindwarrior.app.databinding.ActivitySettingsBinding
import com.mindwarrior.app.viewmodel.SettingsViewModel

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var viewModel: SettingsViewModel
    private var suppressSwitchCallback = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this)[SettingsViewModel::class.java]

        viewModel.timerForegroundEnabled.observe(this) { enabled ->
            suppressSwitchCallback = true
            binding.timerForegroundSwitch.isChecked = enabled
            suppressSwitchCallback = false
        }
        binding.timerForegroundSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (suppressSwitchCallback) return@setOnCheckedChangeListener
            viewModel.setTimerForegroundEnabled(isChecked)
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
