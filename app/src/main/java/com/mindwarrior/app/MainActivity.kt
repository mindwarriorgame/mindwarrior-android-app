package com.mindwarrior.app

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.mindwarrior.app.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.random.Random
import android.view.Gravity
import android.graphics.Rect

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val handler = Handler(Looper.getMainLooper())
    private val logAdapter = LogAdapter()
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
    private var tickCount = 0

    private val snowflakeBlink = object : Runnable {
        private var visible = true
        override fun run() {
            visible = !visible
            if (visible) {
                binding.snowflake.visibility = android.view.View.VISIBLE
                binding.pauseRow.gravity = Gravity.CENTER_VERTICAL
            } else {
                binding.snowflake.visibility = android.view.View.GONE
                binding.pauseRow.gravity = Gravity.CENTER
            }
            handler.postDelayed(this, 1000L)
        }
    }

    private val logTicker = object : Runnable {
        override fun run() {
            tickCount += 1
            val newLogs = mutableListOf<LogItem>()
            newLogs.add(buildLogItem())
            if (tickCount % 2 == 0) {
                newLogs.add(buildLogItem())
                newLogs.add(buildLogItem())
            }
            logAdapter.prependLogs(newLogs)
            logAdapter.trimTo(MAX_LOG_ITEMS)
            binding.logsRecycler.scrollToPosition(0)
            handler.postDelayed(this, 15_000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupMenu()
        setupLogs()
        setupControls()

        seedInitialLogs()
        handler.post(snowflakeBlink)
        handler.postDelayed(logTicker, 15_000L)
    }

    override fun onResume() {
        super.onResume()
        updateDifficultyLabel()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
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
            if (binding.pauseButton.isSelected) return@setOnClickListener
            binding.pauseButton.isSelected = true
            binding.pauseButton.alpha = 1f
        }

        binding.progressButton.setOnClickListener {
            startActivity(android.content.Intent(this, BoardWebViewActivity::class.java))
        }
    }

    private fun updateDifficultyLabel() {
        val difficulty = DifficultyPreferences.getDifficulty(this)
        binding.menuDifficulty.text =
            getString(R.string.menu_difficulty, getString(difficulty.labelRes))
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
        val seededItems = seeds.map { seed ->
            LogItem(formatTimeLabel(seed.timestamp), seed.message)
        }
        logAdapter.prependLogs(seededItems)
        logAdapter.trimTo(MAX_LOG_ITEMS)
    }

    private fun buildLogItem(): LogItem {
        val message = sampleMessages.random()
        val timeOffsetMinutes = Random.nextInt(1, 180)
        val timeMillis = System.currentTimeMillis() - timeOffsetMinutes * 60_000L
        return LogItem(formatTimeLabel(timeMillis), message)
    }

    private fun formatTimeLabel(timeMillis: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timeMillis
        return if (diff < DAY_MILLIS) {
            val relative = when {
                diff < 60_000L -> "${diff / 1000}s ago"
                diff < 3_600_000L -> "${diff / 60_000}m ago"
                else -> "${diff / 3_600_000}h ago"
            }
            "$relative · ${timeFormat.format(timeMillis)}"
        } else {
            dayFormat.format(timeMillis)
        }
    }

    data class LogSeed(val timestamp: Long, val message: String)

    companion object {
        private const val MAX_LOG_ITEMS = 20
        private const val DAY_MILLIS = 24 * 60 * 60 * 1000L
    }
}
