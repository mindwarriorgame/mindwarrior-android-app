package com.mindwarrior.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mindwarrior.app.databinding.ActivityProgressBinding
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
        val mean = if (points.isEmpty()) 0f else points.map { it.y }.average().toFloat()
        val threshold = 30f
        val maxY = if (points.isEmpty()) threshold else max(points.maxOf { it.y }, threshold)

        binding.progressGraph.setData(
            points = points,
            sleepIntervals = sleepIntervals,
            meanValue = mean,
            thresholdValue = threshold,
            minY = 5f,
            maxY = maxY
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
        if (filtered.size < 2) {
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

    companion object {
        private const val WEEK_MILLIS = 7L * 24 * 60 * 60 * 1000
    }
}
