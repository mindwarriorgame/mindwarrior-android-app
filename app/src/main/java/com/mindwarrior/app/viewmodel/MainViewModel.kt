package com.mindwarrior.app.viewmodel

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.LocaleList
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.mindwarrior.app.LogMessageCodec
import com.mindwarrior.app.LanguageManager
import com.mindwarrior.app.R
import com.mindwarrior.app.LogItem
import com.mindwarrior.app.NowProvider
import com.mindwarrior.app.UnseenLogItem
import com.mindwarrior.app.UserStorage
import com.mindwarrior.app.badges.BadgesManager
import com.mindwarrior.app.engine.Counter
import com.mindwarrior.app.engine.GameManager
import java.text.SimpleDateFormat
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val handler = Handler(Looper.getMainLooper())
    private var cachedTimeFormatTag: String? = null
    private var cachedTimeFormat: SimpleDateFormat? = null
    private var cachedDayFormatTag: String? = null
    private var cachedDayFormat: SimpleDateFormat? = null
    private val logItems = mutableListOf<LogItem>()
    private var tickersRunning = false
    private var logIdSeed = NowProvider.nowMillis()
    private var lastLogLabelUpdateMillis = 0L
    private var lastOldLogsSnapshot: List<Pair<String, Long>> = emptyList()
    private var lastUnseenLogsSnapshot: List<Pair<String, Long>> = emptyList()
    private var lastOldLogsLocaleTag: String? = null
    private var lastUnseenLogsLocaleTag: String? = null

    private val _timerText = MutableLiveData<String>()
    val timerText: LiveData<String> = _timerText

    private val _timerWarning = MutableLiveData<Boolean>()
    val timerWarning: LiveData<Boolean> = _timerWarning

    private val _isPaused = MutableLiveData<Boolean>()
    val isPaused: LiveData<Boolean> = _isPaused

    private val _reviewEnabled = MutableLiveData<Boolean>()
    val reviewEnabled: LiveData<Boolean> = _reviewEnabled

    private val _diamonds = MutableLiveData<Int>()
    val diamonds: LiveData<Int> = _diamonds

    private val _progressLevel = MutableLiveData<Int>()
    val progressLevel: LiveData<Int> = _progressLevel

    private val _progressHasGrumpyCat = MutableLiveData<Boolean>()
    val progressHasGrumpyCat: LiveData<Boolean> = _progressHasGrumpyCat

    private val _progressHasRepeller = MutableLiveData<Boolean>()
    val progressHasRepeller: LiveData<Boolean> = _progressHasRepeller

    private val _difficultyLabel = MutableLiveData<String>()
    val difficultyLabel: LiveData<String> = _difficultyLabel

    private val _hasFormula = MutableLiveData<Boolean>()
    val hasFormula: LiveData<Boolean> = _hasFormula

    private val _snowflakeVisible = MutableLiveData<Boolean>(true)
    val snowflakeVisible: LiveData<Boolean> = _snowflakeVisible

    private val _freezeTimerText = MutableLiveData<String>()
    val freezeTimerText: LiveData<String> = _freezeTimerText

    private val _freezeTimerActive = MutableLiveData<Boolean>(false)
    val freezeTimerActive: LiveData<Boolean> = _freezeTimerActive

    private val _logs = MutableLiveData<List<LogItem>>(emptyList())
    val logs: LiveData<List<LogItem>> = _logs

    private val _unseenLogsEvent = MutableLiveData<List<UnseenLogItem>>()
    val unseenLogsEvent: LiveData<List<UnseenLogItem>> = _unseenLogsEvent

    private val userListener = object : UserStorage.UserUpdateListener {
        override fun onUserUpdated(user: com.mindwarrior.app.engine.User) {
            _isPaused.value = user.pausedTimerSerialized.isPresent
            _reviewEnabled.value = true
            _diamonds.value = user.diamonds
            updateProgressState(user)
            _hasFormula.value = user.localStorageSnapshot.isPresent
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

    init {
        refreshTimerDisplay()
        val user = UserStorage.getUser(getApplication())
        _isPaused.value = user.pausedTimerSerialized.isPresent
        _reviewEnabled.value = true
        _diamonds.value = user.diamonds
        updateProgressState(user)
        _hasFormula.value = user.localStorageSnapshot.isPresent
        _difficultyLabel.value = formatDifficultyLabel(user.difficulty)
        updateLogsFromUser(user)
        updateUnseenLogsFromUser(user)
        UserStorage.observeUserChanges(getApplication(), userListener)
    }

    private fun formatDifficultyLabel(difficulty: com.mindwarrior.app.engine.Difficulty): String {
        val localizedContext = getLocalizedContext()
        val label = localizedContext.getString(difficulty.labelRes)
        return localizedContext.getString(R.string.menu_difficulty, label)
    }

    private fun getLocalizedContext(): Context {
        val app = getApplication<Application>()
        val tag = LanguageManager.getCurrentLanguageTag(app)
        val locale = Locale.forLanguageTag(tag)
        val config = Configuration(app.resources.configuration)
        config.setLocale(locale)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(locale))
        }
        return app.createConfigurationContext(config)
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

    fun refreshLocalizedLogs() {
        val user = UserStorage.getUser(getApplication())
        updateLogsFromUser(user)
        refreshLogLabels()
    }

    fun markUnseenLogsObserved(observedLogs: List<UnseenLogItem>) {
        val user = UserStorage.getUser(getApplication())
        val rawLogs = observedLogs.map { Pair(it.rawMessage, it.timestampMillis) }
        val updated = GameManager.onUnseenLogsObserved(user, rawLogs)
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
        val currentTag = LanguageManager.getCurrentLanguageTag(getApplication())
        if (newLogs == lastOldLogsSnapshot && currentTag == lastOldLogsLocaleTag) return
        lastOldLogsSnapshot = newLogs.toList()
        lastOldLogsLocaleTag = currentTag
        val items = newLogs.map { (message, timestampMillis) ->
            val translated = LogMessageCodec.translate(getApplication(), message)
            LogItem(
                id = generateLogId(message, timestampMillis),
                timestampMillis = timestampMillis,
                timeLabel = formatTimeLabel(timestampMillis),
                message = translated
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
        val currentTag = LanguageManager.getCurrentLanguageTag(getApplication())
        if (newLogs == lastUnseenLogsSnapshot) return
        lastUnseenLogsSnapshot = newLogs.toList()
        lastUnseenLogsLocaleTag = currentTag
        _unseenLogsEvent.value = newLogs.map { (message, timestampMillis) ->
            UnseenLogItem(
                rawMessage = message,
                translatedMessage = LogMessageCodec.translate(getApplication(), message),
                timestampMillis = timestampMillis
            )
        }
    }

    private fun generateLogId(message: String, timestampMillis: Long): Long {
        return (timestampMillis xor message.hashCode().toLong())
    }

    private fun formatTimeLabel(timeMillis: Long): String {
        val now = NowProvider.nowMillis()
        val diff = now - timeMillis
        return if (diff < DAY_MILLIS) {
            val relative = formatRelativeTime(diff)
            appString(
                R.string.relative_time_with_clock,
                relative,
                getTimeFormat().format(timeMillis)
            )
        } else {
            getDayFormat().format(timeMillis)
        }
    }

    private fun formatRemaining(remainingMillis: Long): String {
        val totalSeconds = remainingMillis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return appString(R.string.time_format_hms, hours, minutes, seconds)
    }

    private fun formatRelativeTime(diff: Long): String {
        return when {
            diff < 10_000L -> appString(R.string.relative_time_now)
            diff < 30_000L -> appString(R.string.relative_time_seconds_10)
            diff < 60_000L -> appString(R.string.relative_time_seconds_30)
            diff < 3_600_000L -> appString(
                R.string.relative_time_minutes_ago,
                (diff / 60_000).coerceAtLeast(1)
            )
            else -> appString(R.string.relative_time_hours_ago, diff / 3_600_000)
        }
    }

    private fun appString(resId: Int, vararg args: Any): String {
        val localized = getLocalizedContext()
        return localized.getString(resId, *args)
    }

    private fun getTimeFormat(): SimpleDateFormat {
        val app = getApplication<Application>()
        val tag = LanguageManager.getCurrentLanguageTag(app)
        if (tag != cachedTimeFormatTag || cachedTimeFormat == null) {
            val localized = getLocalizedContext()
            val locale = Locale.forLanguageTag(tag)
            cachedTimeFormatTag = tag
            cachedTimeFormat = SimpleDateFormat(
                localized.getString(R.string.time_format_hhmm),
                locale
            )
        }
        return cachedTimeFormat!!
    }

    private fun getDayFormat(): SimpleDateFormat {
        val app = getApplication<Application>()
        val tag = LanguageManager.getCurrentLanguageTag(app)
        if (tag != cachedDayFormatTag || cachedDayFormat == null) {
            val localized = getLocalizedContext()
            val locale = Locale.forLanguageTag(tag)
            cachedDayFormatTag = tag
            cachedDayFormat = SimpleDateFormat(
                localized.getString(R.string.time_format_yyyy_mm_dd_hhmm),
                locale
            )
        }
        return cachedDayFormat!!
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

    private fun updateProgressState(user: com.mindwarrior.app.engine.User) {
        val manager = BadgesManager(user.difficulty.ordinal, user.badgesSerialized)
        val baseLevel = manager.getLevel().coerceAtLeast(0)
        val displayLevel = if (manager.isLevelCompleted()) {
            baseLevel + 1
        } else {
            baseLevel
        }
        _progressLevel.value = displayLevel
        _progressHasGrumpyCat.value = manager.countActiveGrumpyCatsOnBoard() > 0
        _progressHasRepeller.value = user.hasRepeller
    }

    companion object {
        private const val MAX_LOG_ITEMS = 20
        private const val DAY_MILLIS = 24 * 60 * 60 * 1000L
        private const val WARNING_THRESHOLD_MILLIS = 15 * 60 * 1000L
        private const val LOG_LABEL_UPDATE_INTERVAL_MS = 10_000L
        private const val FREEZE_WINDOW_SECONDS = 5 * 60L
    }

}
