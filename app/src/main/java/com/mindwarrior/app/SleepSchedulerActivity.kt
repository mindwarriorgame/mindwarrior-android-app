package com.mindwarrior.app

import android.app.TimePickerDialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mindwarrior.app.databinding.ActivitySleepSchedulerBinding
import com.mindwarrior.app.engine.GameManager
import com.mindwarrior.app.engine.User
import java.util.Locale

class SleepSchedulerActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySleepSchedulerBinding
    private var draftEnabled = false
    private var draftStartMinutes = 0
    private var draftEndMinutes = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySleepSchedulerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val user = UserStorage.getUser(this)
        draftEnabled = user.sleepEnabled
        draftStartMinutes = user.sleepStartMinutes
        draftEndMinutes = user.sleepEndMinutes

        binding.sleepEnabled.isChecked = draftEnabled
        updateTimeButtons(draftEnabled)

        binding.sleepStartButton.setOnClickListener {
            showTimePicker(draftStartMinutes) { minutes ->
                draftStartMinutes = minutes
                updateTimeLabels()
            }
        }

        binding.sleepEndButton.setOnClickListener {
            showTimePicker(draftEndMinutes) { minutes ->
                draftEndMinutes = minutes
                updateTimeLabels()
            }
        }

        binding.sleepEnabled.setOnCheckedChangeListener { _, isChecked ->
            draftEnabled = isChecked
            updateTimeButtons(isChecked)
        }

        binding.sleepDoneButton.setOnClickListener {
            UserStorage.upsertUser(this, GameManager.onSleepScheduleChanged(
                UserStorage.getUser(this),
                draftEnabled,
                draftStartMinutes,
                draftEndMinutes
            ))
            finish()
        }

        updateTimeLabels()
    }

    private fun updateTimeButtons(enabled: Boolean) {
        binding.sleepStartButton.isEnabled = enabled
        binding.sleepEndButton.isEnabled = enabled
        binding.sleepStartButton.isClickable = enabled
        binding.sleepEndButton.isClickable = enabled
        val alpha = if (enabled) 1f else 0.5f
        binding.sleepStartLabel.alpha = alpha
        binding.sleepEndLabel.alpha = alpha
        binding.sleepStartButton.alpha = alpha
        binding.sleepEndButton.alpha = alpha
    }

    private fun updateTimeLabels() {
        binding.sleepStartButton.text = formatMinutes(draftStartMinutes)
        binding.sleepEndButton.text = formatMinutes(draftEndMinutes)
    }

    private fun showTimePicker(initialMinutes: Int, onSelected: (Int) -> Unit) {
        val hour = initialMinutes / 60
        val minute = initialMinutes % 60
        TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            onSelected(selectedHour * 60 + selectedMinute)
        }, hour, minute, true).show()
    }

    private fun formatMinutes(minutes: Int): String {
        val hour = minutes / 60
        val minute = minutes % 60
        return String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
    }
}