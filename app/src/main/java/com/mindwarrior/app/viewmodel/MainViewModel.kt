package com.mindwarrior.app.viewmodel

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.mindwarrior.app.BattleTimerScheduler
import com.mindwarrior.app.LogItem
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.random.Random

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val handler = Handler(Looper.getMainLooper())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dayFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val sampleMessages = listOf(
        "⚔️ Clash triggered: Warden +25% focus boost",
        "🧠 Formula locked\n- Focus 8m\n- Recall 4m\n- Break 2m",
        "💥 Combo x3 landed. Stamina drops to 62%",
        "🧭 New waypoint unlocked: Quiet Zone",
        "🧪 Potion brewed: Calm Serum (1 use)",
        "🏆 Level check: 55% 😾",
        "🛡️ Shield restored by ally protocol",
        "📜 Journal sync complete\nMindMap updated"
    )
    private val logItems = mutableListOf<LogItem>()
    private var tickCount = 0
    private var tickersRunning = false
    private var timerFlagNotified = false
    private var logIdSeed = System.currentTimeMillis()
    private var lastLogLabelUpdateMillis = 0L

    private val _timerText = MutableLiveData<String>()
    val timerText: LiveData<String> = _timerText

    private val _timerWarning = MutableLiveData<Boolean>()
    val timerWarning: LiveData<Boolean> = _timerWarning

    private val _snowflakeVisible = MutableLiveData<Boolean>(true)
    val snowflakeVisible: LiveData<Boolean> = _snowflakeVisible

    private val _logs = MutableLiveData<List<LogItem>>(emptyList())
    val logs: LiveData<List<LogItem>> = _logs

    private val _timerFlagEvent = MutableLiveData<Long>()
    val timerFlagEvent: LiveData<Long> = _timerFlagEvent

    private val snowflakeBlink = object : Runnable {
        private var visible = true
        override fun run() {
            visible = !visible
            _snowflakeVisible.value = visible
            handler.postDelayed(this, 1000L)
        }
    }

    private val logTicker = object : Runnable {
        override fun run() {
            tickCount += 1
            val now = System.currentTimeMillis()
            val newLogs = mutableListOf<LogItem>()
            newLogs.add(buildLogItem(now))
            if (tickCount % 2 == 0) {
                newLogs.add(buildLogItem(now))
                newLogs.add(buildLogItem(now))
            }
            logItems.addAll(0, newLogs)
            logItems.sortByDescending { it.timestampMillis }
            if (logItems.size > MAX_LOG_ITEMS) {
                logItems.subList(MAX_LOG_ITEMS, logItems.size).clear()
            }
            _logs.value = logItems.toList()
            handler.postDelayed(this, 15_000L)
        }
    }

    private val timerTicker = object : Runnable {
        override fun run() {
            refreshTimerDisplay()
            val now = System.currentTimeMillis()
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
        seedInitialLogs()
        refreshTimerDisplay()
    }

    fun startTickers() {
        if (tickersRunning) return
        tickersRunning = true
        handler.removeCallbacks(snowflakeBlink)
        handler.removeCallbacks(logTicker)
        handler.removeCallbacks(timerTicker)
        handler.post(snowflakeBlink)
        handler.postDelayed(logTicker, 15_000L)
        handler.post(timerTicker)
    }

    fun stopTickers() {
        if (!tickersRunning) return
        tickersRunning = false
        handler.removeCallbacks(snowflakeBlink)
        handler.removeCallbacks(logTicker)
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
        val remainingMillis = BattleTimerScheduler.getRemainingMillis(getApplication())
        _timerText.value = formatRemaining(remainingMillis)
        _timerWarning.value = remainingMillis in 1..WARNING_THRESHOLD_MILLIS
    }

    fun addLog(message: String) {
        val now = System.currentTimeMillis()
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
        _timerFlagEvent.value = System.currentTimeMillis()
    }

    fun clearTimerFlag() {
        val prefs = getApplication<Application>()
            .getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_TIMER_FLAG).apply()
        timerFlagNotified = false
    }

    override fun onCleared() {
        super.onCleared()
        handler.removeCallbacksAndMessages(null)
    }

    private fun seedInitialLogs() {
        val now = System.currentTimeMillis()
        val seeds = listOf(
            LogSeed(now - 2 * 60 * 1000L, sampleMessages[0]),
            LogSeed(now - 12 * 60 * 1000L, sampleMessages[1]),
            LogSeed(now - 42 * 60 * 1000L, sampleMessages[2]),
            LogSeed(now - 3 * 60 * 60 * 1000L, sampleMessages[3]),
            LogSeed(now - 28 * 60 * 60 * 1000L, sampleMessages[4])
        )
        logItems.clear()
        logItems.addAll(seeds.map { seed ->
            LogItem(newLogId(), seed.timestamp, formatTimeLabel(seed.timestamp), seed.message)
        })
        logItems.sortByDescending { it.timestampMillis }
        _logs.value = logItems.toList()
    }

    private fun buildLogItem(timestampMillis: Long): LogItem {
        val message = sampleMessages.random()
        return LogItem(newLogId(), timestampMillis, formatTimeLabel(timestampMillis), message)
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

    private fun formatTimeLabel(timeMillis: Long): String {
        val now = System.currentTimeMillis()
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

    data class LogSeed(val timestamp: Long, val message: String)

    companion object {
        private const val MAX_LOG_ITEMS = 20
        private const val DAY_MILLIS = 24 * 60 * 60 * 1000L
        private const val WARNING_THRESHOLD_MILLIS = 15 * 60 * 1000L
        private const val TIMER_FLAG_POLL_MS = 100L
        private const val PREFS_NAME = "mindwarrior_prefs"
        private const val KEY_TIMER_FLAG = "timer_flag"
        private const val LOG_LABEL_UPDATE_INTERVAL_MS = 10_000L
    }
}
