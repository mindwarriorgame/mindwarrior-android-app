package com.mindwarrior.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.mindwarrior.app.databinding.ActivityMainBinding
import android.graphics.Rect
import android.Manifest
import android.content.pm.PackageManager
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AlertDialog
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.mindwarrior.app.viewmodel.MainViewModel
import com.mindwarrior.app.engine.Counter
import com.mindwarrior.app.engine.GameManager
import com.mindwarrior.app.notifications.OneOffAlertController
import com.mindwarrior.app.notifications.StickyAlertController
import com.mindwarrior.app.notifications.StickyAlertForegroundService
import java.util.Optional

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val logAdapter = LogAdapter()
    private lateinit var viewModel: MainViewModel

    private var timerFlagDialogShowing = false
    private var lastTimerFlagEvent = 0L
    private var unseenLogDialogShowing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        setupMenu()
        setupLogs()
        setupControls()
        OneOffAlertController.restart(this)
        requestNotificationPermission()
        if (UserStorage.getUser(this).timerForegroundEnabled) {
            StickyAlertController.start(this)
        }

        bindViewModel()
    }

    override fun onResume() {
        super.onResume()
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
        binding.menuDebug.setOnClickListener {
            binding.menuPanel.visibility = android.view.View.GONE
            showTimeTravelDialog()
        }
        binding.menuSleep.setOnClickListener {
            binding.menuPanel.visibility = android.view.View.GONE
            startActivity(android.content.Intent(this, SleepSchedulerActivity::class.java))
        }
        binding.menuPanel.setOnClickListener { }
        binding.menuPanel.setOnTouchListener { _, event ->
            if (event.action != android.view.MotionEvent.ACTION_UP) {
                return@setOnTouchListener false
            }
            val items = listOf(
                binding.menuSleep,
                binding.menuProgress,
                binding.menuDifficulty,
                binding.menuSettings,
                binding.menuDebug
            )
            val panelLocation = IntArray(2)
            binding.menuPanel.getLocationOnScreen(panelLocation)
            val localY = (event.rawY - panelLocation[1]).toInt()
            items.forEach { item ->
                val itemRect = Rect()
                item.getHitRect(itemRect)
                if (localY in itemRect.top..itemRect.bottom) {
                    item.performClick()
                    return@setOnTouchListener true
                }
            }
            false
        }

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
            intent.putExtra(WebViewActivity.EXTRA_IS_REVIEW, true)
            startActivity(intent)
        }
    }

    private fun showTimeTravelDialog() {
        val input = EditText(this)
        input.hint = getString(R.string.debug_time_travel_hint)
        input.inputType = InputType.TYPE_CLASS_NUMBER
        AlertDialog.Builder(this)
            .setTitle(R.string.debug_time_travel_title)
            .setView(input)
            .setPositiveButton(R.string.debug_time_travel_button) { _, _ ->
                val minutes = input.text.toString().trim().toLongOrNull()
                if (minutes == null || minutes <= 0L) {
                    Toast.makeText(this, R.string.debug_time_travel_invalid, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                NowProvider.moveTimeForwardMins(minutes)
                val user = UserStorage.getUser(this)
                OneOffAlertController.scheduleNextAlert(this, user)
                viewModel.refreshTimerDisplay()
                Toast.makeText(
                    this,
                    "Time moved forward by $minutes minutes",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
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
        viewModel.freezeTimerText.observe(this) { text ->
            binding.snowflakeTimer.text = text
        }
        viewModel.freezeTimerActive.observe(this) { active ->
            binding.snowflakeRow.visibility = if (active) View.VISIBLE else View.GONE
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
        viewModel.unseenLogsEvent.observe(this) { logs ->
            if (logs.isEmpty() || unseenLogDialogShowing) return@observe
            showUnseenLogDialog(logs)
        }
        viewModel.isPaused.observe(this) { paused ->
            updatePauseUi(paused)
        }
        viewModel.reviewEnabled.observe(this) { enabled ->
            binding.reviewButton.isEnabled = enabled
            binding.reviewButton.alpha = if (enabled) 1f else 0.5f
        }
        viewModel.difficultyLabel.observe(this) { label ->
            binding.menuDifficulty.text = label
        }
    }

    private fun updateUserPausedState(paused: Boolean) {
        val user = UserStorage.getUser(this)
        if (paused) {
            UserStorage.upsertUser(this, GameManager.onPaused(user))
        } else {
            UserStorage.upsertUser(this, GameManager.onResume(user))
        }
        OneOffAlertController.restart(this)
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

    private fun showUnseenLogDialog(logs: List<Pair<String, Long>>) {
        if (unseenLogDialogShowing) return
        unseenLogDialogShowing = true
        val message = logs.joinToString("\n") { it.first }
        AlertDialog.Builder(this)
            .setTitle("New events")
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                viewModel.markUnseenLogsObserved()
                unseenLogDialogShowing = false
            }
            .setOnDismissListener { unseenLogDialogShowing = false }
            .setCancelable(false)
            .show()
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
                if (UserStorage.getUser(this).timerForegroundEnabled) {
                    StickyAlertController.start(this)
                }
            }
        }
    }

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST = 1003
    }
}
