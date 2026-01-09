package com.mindwarrior.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.max
import kotlin.math.min

class ProgressGraphView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFE6F0FF.toInt()
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x33E6F0FF
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF5AC8FF.toInt()
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }
    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF5AC8FF.toInt()
        style = Paint.Style.FILL
    }
    private val meanPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF1E9E63.toInt()
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }
    private val thresholdPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFF5B5B.toInt()
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }
    private val sleepPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x336C7688
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFE6F0FF.toInt()
        textSize = 28f
    }

    private var points: List<ProgressPoint> = emptyList()
    private var sleepIntervals: List<TimeInterval> = emptyList()
    private var meanValue: Float = 0f
    private var thresholdValue: Float = 0f
    private var minY: Float = 0f
    private var maxY: Float = 1f

    fun setData(
        points: List<ProgressPoint>,
        sleepIntervals: List<TimeInterval>,
        meanValue: Float,
        thresholdValue: Float,
        minY: Float,
        maxY: Float
    ) {
        this.points = points
        this.sleepIntervals = sleepIntervals
        this.meanValue = meanValue
        this.thresholdValue = thresholdValue
        this.minY = minY
        this.maxY = max(maxY, minY + 1f)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (points.isEmpty()) return

        val leftPadding = 90f
        val rightPadding = 24f
        val topPadding = 24f
        val bottomPadding = 70f
        val chart = RectF(
            leftPadding,
            topPadding,
            width.toFloat() - rightPadding,
            height.toFloat() - bottomPadding
        )

        drawSleepIntervals(canvas, chart)
        drawGrid(canvas, chart)
        drawAxes(canvas, chart)
        drawLine(canvas, chart)
        drawPoints(canvas, chart)
        drawThresholds(canvas, chart)
        drawLabels(canvas, chart)
    }

    private fun drawAxes(canvas: Canvas, chart: RectF) {
        canvas.drawLine(chart.left, chart.top, chart.left, chart.bottom, axisPaint)
        canvas.drawLine(chart.left, chart.bottom, chart.right, chart.bottom, axisPaint)
    }

    private fun drawGrid(canvas: Canvas, chart: RectF) {
        val steps = 3
        for (i in 1..steps) {
            val y = chart.top + (chart.height() / (steps + 1)) * i
            canvas.drawLine(chart.left, y, chart.right, y, gridPaint)
        }
    }

    private fun drawLine(canvas: Canvas, chart: RectF) {
        val minX = points.first().x
        val maxX = points.last().x
        val path = Path()
        points.forEachIndexed { index, point ->
            val x = mapX(point.x, minX, maxX, chart)
            val y = mapY(point.y, chart)
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        canvas.drawPath(path, linePaint)
    }

    private fun drawPoints(canvas: Canvas, chart: RectF) {
        val minX = points.first().x
        val maxX = points.last().x
        points.forEach { point ->
            val x = mapX(point.x, minX, maxX, chart)
            val y = mapY(point.y, chart)
            canvas.drawCircle(x, y, 6f, pointPaint)
        }
    }

    private fun drawThresholds(canvas: Canvas, chart: RectF) {
        val meanY = mapY(meanValue, chart)
        canvas.drawLine(chart.left, meanY, chart.right, meanY, meanPaint)

        val thresholdY = mapY(thresholdValue, chart)
        canvas.drawLine(chart.left, thresholdY, chart.right, thresholdY, thresholdPaint)
    }

    private fun drawSleepIntervals(canvas: Canvas, chart: RectF) {
        val minX = points.first().x
        val maxX = points.last().x
        sleepIntervals.forEach { interval ->
            val startX = mapX(interval.startMillis, minX, maxX, chart)
            val endX = mapX(interval.endMillis, minX, maxX, chart)
            if (endX <= chart.left || startX >= chart.right) return@forEach
            canvas.drawRect(
                max(chart.left, startX),
                chart.top,
                min(chart.right, endX),
                chart.bottom,
                sleepPaint
            )
        }
    }

    private fun drawLabels(canvas: Canvas, chart: RectF) {
        canvas.drawText("Minutes between reviews", chart.left, chart.top - 8f, textPaint)
        canvas.drawText("7d ago", chart.left, height - 24f, textPaint)
        val todayLabel = "Today"
        val textWidth = textPaint.measureText(todayLabel)
        canvas.drawText(todayLabel, chart.right - textWidth, height - 24f, textPaint)

        val maxLabel = String.format("%.0f", maxY)
        canvas.drawText(maxLabel, 8f, chart.top + 8f, textPaint)
        val minLabel = String.format("%.0f", minY)
        canvas.drawText(minLabel, 8f, chart.bottom, textPaint)
    }

    private fun mapX(value: Long, minX: Long, maxX: Long, chart: RectF): Float {
        if (maxX == minX) return chart.left
        val ratio = (value - minX).toFloat() / (maxX - minX).toFloat()
        return chart.left + ratio * chart.width()
    }

    private fun mapY(value: Float, chart: RectF): Float {
        val ratio = (value - minY) / (maxY - minY)
        return chart.bottom - ratio * chart.height()
    }
}
