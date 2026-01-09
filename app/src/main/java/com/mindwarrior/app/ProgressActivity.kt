package com.mindwarrior.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mindwarrior.app.databinding.ActivityProgressBinding
import com.mindwarrior.app.engine.Counter
import com.mindwarrior.app.engine.DifficultyHelper
import kotlin.math.max
import kotlin.math.min

class ProgressActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProgressBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProgressBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val now = NowProvider.nowMillis()
        val start = now - WEEK_MILLIS
        val user = UserStorage.getUser(this)

        val points = buildPointsFromHistory(user.reviewAtMillisActivePlayTimeHistory, start, now)
        val sleepIntervals = buildPauseIntervals(user.pauseIntervalHistory, start, now)
        val mean = median(points.map { it.y })
        val threshold =
            DifficultyHelper.getReviewFrequencyMillis(user.difficulty) / 60_000f
        val maxY = if (points.isEmpty()) {
            max(mean, threshold)
        } else {
            max(max(points.maxOf { it.y }, threshold), mean)
        }
        val minY = if (points.isEmpty()) 5f else min(5f, points.minOf { it.y })

        binding.progressGraph.setData(
            points = points,
            sleepIntervals = sleepIntervals,
            meanValue = mean,
            thresholdValue = threshold,
            minY = minY,
            maxY = maxY,
            rangeStartMillis = start,
            rangeEndMillis = now
        )

        val totalSeconds = Counter(user.activePlayTimerSerialized).getTotalSeconds()
        val days = totalSeconds / 86_400
        val hours = (totalSeconds % 86_400) / 3_600
        val minutes = (totalSeconds % 3_600) / 60
        val seconds = totalSeconds % 60
        binding.progressActiveTime.text = getString(
            R.string.progress_active_time,
            days,
            hours,
            minutes,
            seconds
        )
        val totalDiamonds = user.diamonds + user.diamondsSpent
        binding.progressDiamondsTotal.text = getString(
            R.string.progress_diamonds_total,
            totalDiamonds
        )

        binding.progressClose.setOnClickListener {
            finish()
        }
    }

    private fun buildPointsFromHistory(
        history: List<Pair<Long, Long>>,
        startMillis: Long,
        endMillis: Long
    ): List<ProgressPoint> {
        val filtered = history
            .filter { it.first in startMillis..endMillis }
            .sortedBy { it.first }
        if (filtered.size == 1) {
            return listOf(ProgressPoint(filtered[0].first, 0f))
        }
        if (filtered.isEmpty()) {
            return emptyList()
        }
        val points = mutableListOf<ProgressPoint>()
        for (index in 1 until filtered.size) {
            val (atMillis, activePlayMillis) = filtered[index]
            val prevActivePlayMillis = filtered[index - 1].second
            val deltaMinutes = ((activePlayMillis - prevActivePlayMillis).coerceAtLeast(0L)) / 60_000f
            points.add(ProgressPoint(atMillis, deltaMinutes))
        }
        return points
    }

    private fun buildPauseIntervals(
        history: List<Pair<Long, Long>>,
        startMillis: Long,
        endMillis: Long
    ): List<TimeInterval> {
        return history
            .mapNotNull { (start, end) ->
                if (end < startMillis || start > endMillis) {
                    null
                } else {
                    val clippedStart = max(start, startMillis)
                    val clippedEnd = min(end, endMillis)
                    TimeInterval(clippedStart, clippedEnd)
                }
            }
    }

    private fun median(values: List<Float>): Float {
        if (values.isEmpty()) {
            return 0f
        }
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) {
            sorted[mid]
        } else {
            (sorted[mid - 1] + sorted[mid]) / 2f
        }
    }

    companion object {
        private const val WEEK_MILLIS = 7L * 24 * 60 * 60 * 1000
    }
}
