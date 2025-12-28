package com.mindwarrior.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mindwarrior.app.databinding.ActivityProgressBinding
import kotlin.math.max
import kotlin.random.Random

class ProgressActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProgressBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProgressBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val now = System.currentTimeMillis()
        val weekMillis = 7L * 24 * 60 * 60 * 1000
        val start = now - weekMillis

        val points = buildPoints(start, now)
        val sleepIntervals = buildSleepIntervals(start, now)
        val mean = points.map { it.y }.average().toFloat()
        val threshold = 30f
        val maxY = max(points.maxOf { it.y }, threshold)

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

    private fun buildPoints(start: Long, end: Long): List<ProgressPoint> {
        val count = 28
        return List(count) {
            val x = Random.nextLong(start, end)
            val y = randomYValue()
            ProgressPoint(x, y)
        }.sortedBy { it.x }
    }

    private fun buildSleepIntervals(start: Long, end: Long): List<TimeInterval> {
        val intervals = mutableListOf<TimeInterval>()
        val dayMillis = 24L * 60 * 60 * 1000
        var cursor = start
        while (cursor < end) {
            val sleepStart = cursor + 23L * 60 * 60 * 1000
            val sleepEnd = cursor + dayMillis + 7L * 60 * 60 * 1000
            intervals.add(TimeInterval(sleepStart, sleepEnd))
            cursor += dayMillis
        }
        return intervals
    }

    private fun randomYValue(): Float {
        val base = 5f
        val skew = Random.nextFloat()
        val scaled = 5f + skew * skew * 80f
        return base + scaled
    }
}
