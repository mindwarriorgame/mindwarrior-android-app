package com.mindwarrior.app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class GameViewModel : ViewModel() {
    private val _countdownText = MutableLiveData("00:24:18")
    val countdownText: LiveData<String> = _countdownText

    private val _pauseEnabled = MutableLiveData(false)
    val pauseEnabled: LiveData<Boolean> = _pauseEnabled

    private val _logs = MutableLiveData<List<LogEntry>>(emptyList())
    val logs: LiveData<List<LogEntry>> = _logs

    fun enablePause() {
        if (_pauseEnabled.value != true) {
            _pauseEnabled.value = true
        }
    }

    fun addLogs(newEntries: List<LogEntry>) {
        val current = _logs.value.orEmpty()
        val merged = (newEntries + current).take(20)
        _logs.value = merged
    }
}
