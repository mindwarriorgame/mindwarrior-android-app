package com.mindwarrior.app

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.mindwarrior.app.databinding.ActivitySettingsBinding
import com.mindwarrior.app.notifications.StickyAlertController
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
            if (suppressSwitchCallback) {
                return@setOnCheckedChangeListener
            }
            viewModel.setTimerForegroundEnabled(isChecked)
            if (isChecked) {
                StickyAlertController.start(this)
            } else {
                StickyAlertController.stop(this)
            }
        }

        updateLanguageButton()
        binding.settingsLanguageButton.setOnClickListener {
            showLanguageDialog()
        }

        binding.settingsClose.setOnClickListener {
            finish()
        }
    }

    private fun updateLanguageButton() {
        val currentTag = LanguageManager.getCurrentLanguageTag(this)
        binding.settingsLanguageButton.text =
            LanguageManager.getLanguageLabel(this, currentTag)
    }

    private fun showLanguageDialog() {
        val options = LanguageManager.languageOptions
        val labels = options.map { getString(it.labelResId) }.toTypedArray()
        val currentTag = LanguageManager.getCurrentLanguageTag(this)
        val selectedIndex = options.indexOfFirst { it.tag == currentTag }.coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle(R.string.settings_language_dialog_title)
            .setSingleChoiceItems(labels, selectedIndex) { dialog, index ->
                val selectedTag = options[index].tag
                if (selectedTag != currentTag) {
                    LanguageManager.setLanguage(this, selectedTag)
                    updateLanguageButton()
                    recreate()
                }
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
