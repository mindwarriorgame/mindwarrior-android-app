package com.mindwarrior.app.viewmodel

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.mindwarrior.app.R
import com.mindwarrior.app.LogItem
import com.mindwarrior.app.NowProvider
import com.mindwarrior.app.UserStorage
import com.mindwarrior.app.engine.Counter
import com.mindwarrior.app.engine.GameManager
import java.text.SimpleDateFormat
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val handler = Handler(Looper.getMainLooper())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dayFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val logItems = mutableListOf<LogItem>()
    private var tickersRunning = false
    private var timerFlagNotified = false
    private var logIdSeed = NowProvider.nowMillis()
    private var lastLogLabelUpdateMillis = 0L
    private var lastOldLogsSnapshot: List<Pair<String, Long>> = emptyList()
    private var lastUnseenLogsSnapshot: List<Pair<String, Long>> = emptyList()

    private val _timerText = MutableLiveData<String>()
    val timerText: LiveData<String> = _timerText

    private val _timerWarning = MutableLiveData<Boolean>()
    val timerWarning: LiveData<Boolean> = _timerWarning

    private val _isPaused = MutableLiveData<Boolean>()
    val isPaused: LiveData<Boolean> = _isPaused

    private val _reviewEnabled = MutableLiveData<Boolean>()
    val reviewEnabled: LiveData<Boolean> = _reviewEnabled

    private val _difficultyLabel = MutableLiveData<String>()
    val difficultyLabel: LiveData<String> = _difficultyLabel

    private val _snowflakeVisible = MutableLiveData<Boolean>(true)
    val snowflakeVisible: LiveData<Boolean> = _snowflakeVisible

    private val _freezeTimerText = MutableLiveData<String>()
    val freezeTimerText: LiveData<String> = _freezeTimerText

    private val _freezeTimerActive = MutableLiveData<Boolean>(false)
    val freezeTimerActive: LiveData<Boolean> = _freezeTimerActive

    private val _logs = MutableLiveData<List<LogItem>>(emptyList())
    val logs: LiveData<List<LogItem>> = _logs

    private val _timerFlagEvent = MutableLiveData<Long>()
    val timerFlagEvent: LiveData<Long> = _timerFlagEvent

    private val _unseenLogsEvent = MutableLiveData<List<Pair<String, Long>>>()
    val unseenLogsEvent: LiveData<List<Pair<String, Long>>> = _unseenLogsEvent

    private val userListener = object : UserStorage.UserUpdateListener {
        override fun onUserUpdated(user: com.mindwarrior.app.engine.User) {
            _isPaused.value = user.pausedTimerSerialized.isPresent
            _reviewEnabled.value = true
            _difficultyLabel.value = formatDifficultyLabel(user.difficulty)
            updateLogsFromUser(user)
            updateUnseenLogsFromUser(user)
        }
    }

    private val timerTicker = object : Runnable {
        override fun run() {
            refreshTimerDisplay()
            val now = NowProvider.nowMillis()
            if (now - lastLogLabelUpdateMillis >= LOG_LABEL_UPDATE_INTERVAL_MS) {
                lastLogLabelUpdateMillis = now
                refreshLogLabels()
            }
            handler.postDelayed(this, 1000L)
        }
    }

    private val timerFlagChecker = object : Runnable {
        override fun run() {
            checkTimerFlag()
            handler.postDelayed(this, TIMER_FLAG_POLL_MS)
        }
    }

    init {
        refreshTimerDisplay()
        val user = UserStorage.getUser(getApplication())
        _isPaused.value = user.pausedTimerSerialized.isPresent
        _reviewEnabled.value = true
        _difficultyLabel.value = formatDifficultyLabel(user.difficulty)
        updateLogsFromUser(user)
        updateUnseenLogsFromUser(user)
        UserStorage.observeUserChanges(getApplication(), userListener)
    }

    private fun formatDifficultyLabel(difficulty: com.mindwarrior.app.engine.Difficulty): String {
        val label = getApplication<Application>().getString(difficulty.labelRes)
        return getApplication<Application>().getString(R.string.menu_difficulty, label)
    }

    fun startTickers() {
        if (tickersRunning) return
        tickersRunning = true
        handler.removeCallbacks(timerTicker)
        handler.post(timerTicker)
    }

    fun stopTickers() {
        if (!tickersRunning) return
        tickersRunning = false
        handler.removeCallbacks(timerTicker)
    }

    fun startTimerFlagChecker() {
        handler.removeCallbacks(timerFlagChecker)
        handler.post(timerFlagChecker)
    }

    fun stopTimerFlagChecker() {
        handler.removeCallbacks(timerFlagChecker)
        timerFlagNotified = false
    }

    fun refreshTimerDisplay() {
        val user = UserStorage.getUser(this.getApplication())
        val remainingMillis = Math.max(
            GameManager.calculateNextDeadlineAtMillis(user) / 1000 -
                NowProvider.nowMillis() / 1000,
            0
        ) * 1000
        _timerText.value = formatRemaining(remainingMillis)
        _timerWarning.value = remainingMillis in 1..WARNING_THRESHOLD_MILLIS
        refreshFreezeTimerDisplay(user)
    }

    fun addLog(message: String) {
        val now = NowProvider.nowMillis()
        logItems.add(0, LogItem(newLogId(), now, formatTimeLabel(now), message))
        logItems.sortByDescending { it.timestampMillis }
        if (logItems.size > MAX_LOG_ITEMS) {
            logItems.subList(MAX_LOG_ITEMS, logItems.size).clear()
        }
        _logs.value = logItems.toList()
    }

    private fun checkTimerFlag() {
        val prefs = getApplication<Application>()
            .getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val isSet = prefs.getBoolean(KEY_TIMER_FLAG, false)
        if (!isSet) {
            timerFlagNotified = false
            return
        }
        if (timerFlagNotified) return
        timerFlagNotified = true
        _timerFlagEvent.value = NowProvider.nowMillis()
    }

    fun clearTimerFlag() {
        val prefs = getApplication<Application>()
            .getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_TIMER_FLAG).apply()
        timerFlagNotified = false
    }

    fun markUnseenLogsObserved() {
        val user = UserStorage.getUser(getApplication())
        val updated = GameManager.onUnseenLogsObserved(user)
        if (updated != user) {
            UserStorage.upsertUser(getApplication(), updated)
        }
    }

    override fun onCleared() {
        super.onCleared()
        handler.removeCallbacksAndMessages(null)
    }

    private fun newLogId(): Long {
        logIdSeed += 1
        return logIdSeed
    }

    private fun refreshLogLabels() {
        if (logItems.isEmpty()) return
        val updated = logItems.map { item ->
            item.copy(timeLabel = formatTimeLabel(item.timestampMillis))
        }.sortedByDescending { it.timestampMillis }
        logItems.clear()
        logItems.addAll(updated)
        _logs.value = logItems.toList()
    }

    private fun updateLogsFromUser(user: com.mindwarrior.app.engine.User) {
        val newLogs = user.oldLogsNewestFirst
        if (newLogs == lastOldLogsSnapshot) return
        lastOldLogsSnapshot = newLogs.toList()
        val items = newLogs.map { (message, timestampMillis) ->
            LogItem(
                id = generateLogId(message, timestampMillis),
                timestampMillis = timestampMillis,
                timeLabel = formatTimeLabel(timestampMillis),
                message = message
            )
        }.sortedByDescending { it.timestampMillis }
        logItems.clear()
        logItems.addAll(items)
        if (logItems.size > MAX_LOG_ITEMS) {
            logItems.subList(MAX_LOG_ITEMS, logItems.size).clear()
        }
        _logs.value = logItems.toList()
    }

    private fun updateUnseenLogsFromUser(user: com.mindwarrior.app.engine.User) {
        val newLogs = user.unseenLogsNewestFirst
        if (newLogs == lastUnseenLogsSnapshot) return
        lastUnseenLogsSnapshot = newLogs.toList()
        if (newLogs.isNotEmpty()) {
            _unseenLogsEvent.value = newLogs
        }
    }

    private fun generateLogId(message: String, timestampMillis: Long): Long {
        return (timestampMillis xor message.hashCode().toLong())
    }

    private fun formatTimeLabel(timeMillis: Long): String {
        val now = NowProvider.nowMillis()
        val diff = now - timeMillis
        return if (diff < DAY_MILLIS) {
            val relative = when {
                diff < 10_000L -> "now"
                diff < 30_000L -> "10s ago"
                diff < 60_000L -> "30s ago"
                diff < 3_600_000L -> "${(diff / 60_000).coerceAtLeast(1)}m ago"
                else -> "${diff / 3_600_000}h ago"
            }
            "$relative · ${timeFormat.format(timeMillis)}"
        } else {
            dayFormat.format(timeMillis)
        }
    }

    private fun formatRemaining(remainingMillis: Long): String {
        val totalSeconds = remainingMillis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun refreshFreezeTimerDisplay(user: com.mindwarrior.app.engine.User) {
        val activePlaySeconds = Counter(user.activePlayTimerSerialized).getTotalSeconds()
        val deltaSeconds = (activePlaySeconds - user.lastRewardAtActivePlayTime).coerceAtLeast(0L)
        val remainingSeconds = FREEZE_WINDOW_SECONDS - deltaSeconds
        val isActive = remainingSeconds > 0L
        _freezeTimerActive.value = isActive
        _snowflakeVisible.value = isActive
        if (isActive) {
            _freezeTimerText.value = formatRemaining(remainingSeconds * 1000)
        } else {
            _freezeTimerText.value = ""
        }
    }

    companion object {
        private const val MAX_LOG_ITEMS = 20
        private const val DAY_MILLIS = 24 * 60 * 60 * 1000L
        private const val WARNING_THRESHOLD_MILLIS = 15 * 60 * 1000L
        private const val TIMER_FLAG_POLL_MS = 100L
        private const val PREFS_NAME = "mindwarrior_prefs"
        private const val KEY_TIMER_FLAG = "timer_flag"
        private const val LOG_LABEL_UPDATE_INTERVAL_MS = 10_000L
        private const val FREEZE_WINDOW_SECONDS = 5 * 60L
    }

}
