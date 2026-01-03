package com.mindwarrior.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.mindwarrior.app.databinding.ActivityMainBinding
import android.graphics.Rect
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AlertDialog
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.mindwarrior.app.viewmodel.MainViewModel
import com.mindwarrior.app.engine.Counter
import java.util.Optional
import kotlin.math.max
import kotlin.math.min

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val logAdapter = LogAdapter()
    private lateinit var viewModel: MainViewModel

    private var quickStartStep = 0
    private var timerFlagDialogShowing = false
    private var lastTimerFlagEvent = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        setupMenu()
        setupLogs()
        setupControls()
        BattleTimerScheduler.ensureScheduled(this)
        requestNotificationPermission()
        if (SettingsPreferences.isForegroundEnabled(this)) {
            BattleTimerStickyForegroundServiceController.start(this)
        }

        bindViewModel()
        showQuickStartIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        updateDifficultyLabel()
        viewModel.startTickers()
        viewModel.refreshTimerDisplay()
        viewModel.startTimerFlagChecker()
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopTickers()
        viewModel.stopTimerFlagChecker()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onBackPressed() {
        if (binding.menuPanel.visibility == android.view.View.VISIBLE) {
            binding.menuPanel.visibility = android.view.View.GONE
        } else {
            super.onBackPressed()
        }
    }

    private fun setupMenu() {
        binding.menuButton.setOnClickListener {
            binding.menuPanel.visibility =
                if (binding.menuPanel.visibility == android.view.View.VISIBLE) {
                    android.view.View.GONE
                } else {
                    android.view.View.VISIBLE
                }
        }
        binding.menuDifficulty.setOnClickListener {
            binding.menuPanel.visibility = android.view.View.GONE
            startActivity(android.content.Intent(this, DifficultyActivity::class.java))
        }
        binding.menuProgress.setOnClickListener {
            binding.menuPanel.visibility = android.view.View.GONE
            startActivity(android.content.Intent(this, ProgressActivity::class.java))
        }
        binding.menuSettings.setOnClickListener {
            binding.menuPanel.visibility = android.view.View.GONE
            startActivity(android.content.Intent(this, SettingsActivity::class.java))
        }
        binding.menuSleep.setOnClickListener {
            binding.menuPanel.visibility = android.view.View.GONE
            startActivity(android.content.Intent(this, SleepSchedulerActivity::class.java))
        }
        updateDifficultyLabel()

        binding.root.setOnTouchListener { _, event ->
            if (event.action == android.view.MotionEvent.ACTION_DOWN &&
                binding.menuPanel.visibility == android.view.View.VISIBLE) {
                val panelRect = Rect()
                val buttonRect = Rect()
                binding.menuPanel.getGlobalVisibleRect(panelRect)
                binding.menuButton.getGlobalVisibleRect(buttonRect)
                if (!panelRect.contains(event.rawX.toInt(), event.rawY.toInt()) &&
                    !buttonRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                    binding.menuPanel.visibility = android.view.View.GONE
                }
            }
            false
        }
    }

    private fun setupLogs() {
        binding.logsRecycler.layoutManager = LinearLayoutManager(this)
        binding.logsRecycler.adapter = logAdapter
        binding.logsRecycler.itemAnimator = DefaultItemAnimator()
    }

    private fun setupControls() {
        binding.pauseButton.setOnClickListener {
            val paused = viewModel.isPaused.value == true
            if (paused) {
                BattleTimerScheduler.resumeTimer(this)
            } else {
                BattleTimerScheduler.pauseTimer(this)
            }
            updateUserPausedState(!paused)
        }

        binding.diamondsButton.setOnClickListener {
            startActivity(android.content.Intent(this, ShopActivity::class.java))
        }

        binding.progressButton.setOnClickListener {
            startActivity(android.content.Intent(this, WebViewActivity::class.java))
        }

        binding.labButton.setOnClickListener {
            val intent = android.content.Intent(this, WebViewActivity::class.java)
            intent.putExtra(
                WebViewActivity.EXTRA_BASE_URL,
                "file:///android_asset/miniapp-frontend/index.html?formula=1"
            )
            intent.putExtra(
                WebViewActivity.EXTRA_ASSET_PATH,
                "miniapp-frontend/index.html"
            )
            startActivity(intent)
        }

        binding.reviewButton.setOnClickListener {
            val intent = android.content.Intent(this, WebViewActivity::class.java)
            intent.putExtra(
                WebViewActivity.EXTRA_BASE_URL,
                "file:///android_asset/miniapp-frontend/index.html?review=1&next_review_prompt_minutes=1,2,3,4,5"
            )
            intent.putExtra(
                WebViewActivity.EXTRA_ASSET_PATH,
                "miniapp-frontend/index.html"
            )
            startActivity(intent)
        }
    }

    private fun updatePauseUi(paused: Boolean) {
        binding.pauseButton.text = getString(
            if (paused) R.string.pause_button_resume else R.string.pause_button_pause
        )
        binding.pauseButton.alpha = 1f
        binding.pauseButton.isSelected = paused
        if (paused) {
            binding.pauseIndicator.visibility = View.VISIBLE
        } else {
            binding.pauseIndicator.visibility = View.GONE
        }
    }

    private fun showQuickStartIfNeeded() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        if (prefs.getBoolean(KEY_QUICK_START_DONE, false)) return
        quickStartStep = 1
        binding.root.post { showQuickStartStep() }
    }

    private fun bindViewModel() {
        viewModel.timerText.observe(this) { text ->
            binding.timerText.text = text
        }
        viewModel.timerWarning.observe(this) { warning ->
            binding.timerText.setTextColor(
                if (warning) getColor(R.color.timer_text_warning) else getColor(R.color.timer_text)
            )
        }
        viewModel.snowflakeVisible.observe(this) { visible ->
            val visibility = if (visible) View.VISIBLE else View.GONE
            binding.snowflake.visibility = visibility
            binding.snowflakeTimer.visibility = visibility
        }
        viewModel.logs.observe(this) { logs ->
            logAdapter.submitList(logs) {
                binding.logsRecycler.scrollToPosition(0)
            }
        }
        viewModel.timerFlagEvent.observe(this) { timestamp ->
            if (timestamp <= lastTimerFlagEvent) return@observe
            lastTimerFlagEvent = timestamp
            showTimerFlagDialog()
        }
        viewModel.isPaused.observe(this) { paused ->
            updatePauseUi(paused)
        }
    }

    private fun updateUserPausedState(paused: Boolean) {
        val user = UserStorage.getUser(this)
        val pausedTimerSerialized = if (paused) {
            Optional.of(Counter(null).serialize())
        } else {
            Optional.empty()
        }
        UserStorage.upsertUser(this, user.copy(pausedTimerSerialized = pausedTimerSerialized))
    }

    private fun showQuickStartStep() {
        val (target, textRes, buttonRes) = when (quickStartStep) {
            1 -> Triple(binding.labButton, R.string.quick_start_tube, R.string.quick_start_next)
            else -> Triple(binding.reviewButton, R.string.quick_start_review, R.string.quick_start_done)
        }
        binding.quickStartOverlay.visibility = View.VISIBLE
        binding.quickStartText.text = getString(textRes)
        binding.quickStartButton.text = getString(buttonRes)
        binding.quickStartButton.setOnClickListener {
            if (quickStartStep == 1) {
                quickStartStep = 2
                showQuickStartStep()
            } else {
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .edit()
                    .putBoolean(KEY_QUICK_START_DONE, true)
                    .apply()
                binding.quickStartOverlay.visibility = View.GONE
            }
        }
        target.post { positionQuickStartHint(target) }
    }

    private fun positionQuickStartHint(target: View) {
        val overlay = binding.quickStartOverlay
        val hint = binding.quickStartHintContainer
        val arrow = binding.quickStartArrow
        val focus = binding.quickStartFocus
        val dim = binding.quickStartDim

        if (overlay.width == 0 || overlay.height == 0 || target.width == 0 || target.height == 0) {
            target.post { positionQuickStartHint(target) }
            return
        }

        val overlayLocation = IntArray(2)
        val targetLocation = IntArray(2)
        overlay.getLocationInWindow(overlayLocation)
        target.getLocationInWindow(targetLocation)

        val targetLeft = targetLocation[0] - overlayLocation[0]
        val targetTop = targetLocation[1] - overlayLocation[1]
        val targetCenterX = targetLeft + target.width / 2f

        val focusPadding = resources.displayMetrics.density * 6
        val focusWidth = target.width + (focusPadding * 2).toInt()
        val focusHeight = target.height + (focusPadding * 2).toInt()
        val focusParams = focus.layoutParams
        focusParams.width = focusWidth
        focusParams.height = focusHeight
        focus.layoutParams = focusParams
        focus.translationX = targetLeft - focusPadding
        focus.translationY = targetTop - focusPadding
        dim.setDimColor(0x99000000.toInt())
        dim.setFocusRect(
            targetLeft - focusPadding,
            targetTop - focusPadding,
            targetLeft + target.width + focusPadding,
            targetTop + target.height + focusPadding,
            resources.displayMetrics.density * 12
        )

        hint.measure(
            View.MeasureSpec.makeMeasureSpec(overlay.width, View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(overlay.height, View.MeasureSpec.AT_MOST)
        )
        val hintWidth = hint.measuredWidth
        val hintHeight = hint.measuredHeight

        val spaceAbove = targetTop
        val spaceBelow = overlay.height - (targetTop + target.height)
        val showAbove = spaceAbove > spaceBelow

        val hintX = min(
            overlay.width - hintWidth - 16f,
            max(16f, targetCenterX - hintWidth / 2f)
        )
        val hintY = if (showAbove) {
            max(16f, targetTop - hintHeight - 20f)
        } else {
            min(overlay.height - hintHeight - 16f, targetTop + target.height + 20f)
        }

        hint.translationX = hintX
        hint.translationY = hintY

        val arrowSize = arrow.layoutParams.width.toFloat()
        val arrowX = targetCenterX - arrowSize / 2f
        val arrowY = if (showAbove) {
            hintY + hintHeight - arrowSize / 2f
        } else {
            hintY - arrowSize / 2f
        }
        arrow.translationX = arrowX
        arrow.translationY = arrowY
        arrow.rotation = if (showAbove) 225f else 45f
    }

    private fun updateDifficultyLabel() {
        val difficulty = DifficultyPreferences.getDifficulty(this)
        binding.menuDifficulty.text =
            getString(R.string.menu_difficulty, getString(difficulty.labelRes))
    }

    private fun showTimerFlagDialog() {
        if (timerFlagDialogShowing) return
        timerFlagDialogShowing = true
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.timer_flag_title))
            .setMessage(getString(R.string.timer_flag_message))
            .setPositiveButton(getString(R.string.timer_flag_done)) { _, _ ->
                viewModel.clearTimerFlag()
                addTimerLog()
                timerFlagDialogShowing = false
            }
            .setOnDismissListener { timerFlagDialogShowing = false }
            .setCancelable(false)
            .show()
    }

    private fun addTimerLog() {
        viewModel.addLog(getString(R.string.timer_flag_log))
    }

    private fun requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT < 33) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            return
        }

        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            NOTIFICATION_PERMISSION_REQUEST
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST) {
            val granted = grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                finishAffinity()
            } else {
                if (SettingsPreferences.isForegroundEnabled(this)) {
                    BattleTimerStickyForegroundServiceController.start(this)
                }
            }
        }
    }

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST = 1003
        private const val PREFS_NAME = "mindwarrior_prefs"
        private const val KEY_QUICK_START_DONE = "quick_start_done5"
    }
}
