package com.mindwarrior.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ScaleGestureDetector
import android.widget.EdgeEffect
import com.mindwarrior.app.NowProvider
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
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
    private var rangeStartMillis: Long = 0L
    private var rangeEndMillis: Long = 0L
    private var zoomScale: Float = 1f
    private var zoomStartMillis: Long? = null
    private var zoomScaleY: Float = 1f
    private var zoomMinY: Float? = null
    private var lastChartRect: RectF? = null
    private val scaleDetector = ScaleGestureDetector(context, ScaleListener())
    private val gestureDetector = GestureDetector(context, ScrollListener())
    private val leftEdgeEffect = EdgeEffect(context)
    private val rightEdgeEffect = EdgeEffect(context)
    private val topEdgeEffect = EdgeEffect(context)
    private val bottomEdgeEffect = EdgeEffect(context)
    private var zoomedIn = false
    private var zoomChangedListener: ((Boolean) -> Unit)? = null
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun setData(
        points: List<ProgressPoint>,
        sleepIntervals: List<TimeInterval>,
        meanValue: Float,
        thresholdValue: Float,
        minY: Float,
        maxY: Float,
        rangeStartMillis: Long,
        rangeEndMillis: Long
    ) {
        this.points = points
        this.sleepIntervals = sleepIntervals
        this.meanValue = meanValue
        this.thresholdValue = thresholdValue
        this.minY = minY
        this.maxY = max(maxY, minY + 1f)
        this.rangeStartMillis = rangeStartMillis
        this.rangeEndMillis = rangeEndMillis
        zoomScale = 1f
        zoomStartMillis = null
        zoomScaleY = 1f
        zoomMinY = null
        updateZoomState()
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
        lastChartRect = chart

        drawSleepIntervals(canvas, chart)
        drawGrid(canvas, chart)
        drawAxes(canvas, chart)
        drawLine(canvas, chart)
        drawPoints(canvas, chart)
        drawThresholds(canvas, chart)
        drawLabels(canvas, chart)
        drawEdgeEffects(canvas, chart)
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
        val (minX, maxX) = currentRange()
        val (currentMinY, currentMaxY) = currentYRange()
        val path = Path()
        points.forEachIndexed { index, point ->
            val x = mapX(point.x, minX, maxX, chart)
            val y = mapY(point.y, currentMinY, currentMaxY, chart)
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        canvas.drawPath(path, linePaint)
    }

    private fun drawPoints(canvas: Canvas, chart: RectF) {
        val (minX, maxX) = currentRange()
        val (currentMinY, currentMaxY) = currentYRange()
        points.forEach { point ->
            val x = mapX(point.x, minX, maxX, chart)
            val y = mapY(point.y, currentMinY, currentMaxY, chart)
            canvas.drawCircle(x, y, 6f, pointPaint)
        }
    }

    private fun drawThresholds(canvas: Canvas, chart: RectF) {
        val (currentMinY, currentMaxY) = currentYRange()
        val meanY = mapY(meanValue, currentMinY, currentMaxY, chart)
        canvas.drawLine(chart.left, meanY, chart.right, meanY, meanPaint)

        val thresholdY = mapY(thresholdValue, currentMinY, currentMaxY, chart)
        canvas.drawLine(chart.left, thresholdY, chart.right, thresholdY, thresholdPaint)
    }

    private fun drawSleepIntervals(canvas: Canvas, chart: RectF) {
        val (minX, maxX) = currentRange()
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
        val (minX, maxX) = currentRange()
        val span = (maxX - minX).coerceAtLeast(0L)
        val startLabel = "-" + formatDuration(span)
        val endLabel = formatRelativeDayLabelWithTime(maxX)
        canvas.drawText("Minutes between reviews", chart.left, chart.top - 8f, textPaint)
        canvas.drawText(startLabel, chart.left, height - 24f, textPaint)
        val textWidth = textPaint.measureText(endLabel)
        canvas.drawText(endLabel, chart.right - textWidth, height - 24f, textPaint)

        val (currentMinY, currentMaxY) = currentYRange()
        val maxLabel = String.format("%.0f", currentMaxY)
        canvas.drawText(maxLabel, 8f, chart.top + 8f, textPaint)
        val minLabel = String.format("%.0f", currentMinY)
        canvas.drawText(minLabel, 8f, chart.bottom, textPaint)
    }

    private fun mapX(value: Long, minX: Long, maxX: Long, chart: RectF): Float {
        if (maxX == minX) return chart.left
        val ratio = (value - minX).toFloat() / (maxX - minX).toFloat()
        return chart.left + ratio * chart.width()
    }

    private fun mapY(value: Float, minY: Float, maxY: Float, chart: RectF): Float {
        val ratio = (value - minY) / (maxY - minY)
        return chart.bottom - ratio * chart.height()
    }

    private fun currentRange(): Pair<Long, Long> {
        val baseSpan = rangeEndMillis - rangeStartMillis
        if (baseSpan <= 0L) {
            return rangeStartMillis to rangeEndMillis
        }
        val span = (baseSpan / zoomScale).toLong().coerceAtLeast(1L)
        val maxStart = (rangeEndMillis - span).coerceAtLeast(rangeStartMillis)
        val defaultStart = rangeStartMillis + (baseSpan - span) / 2
        val start = (zoomStartMillis ?: defaultStart).coerceIn(rangeStartMillis, maxStart)
        val end = (start + span).coerceAtMost(rangeEndMillis)
        return start to end
    }

    private fun currentYRange(): Pair<Float, Float> {
        val baseSpan = maxY - minY
        if (baseSpan <= 0f) {
            return minY to maxY
        }
        val span = (baseSpan / zoomScaleY).coerceAtLeast(0.1f)
        val maxStart = (maxY - span).coerceAtLeast(minY)
        val defaultStart = minY + (baseSpan - span) / 2f
        val start = (zoomMinY ?: defaultStart).coerceIn(minY, maxStart)
        val end = (start + span).coerceAtMost(maxY)
        return start to end
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val scaleHandled = scaleDetector.onTouchEvent(event)
        val scrollHandled = gestureDetector.onTouchEvent(event)
        if (event.actionMasked == MotionEvent.ACTION_UP ||
            event.actionMasked == MotionEvent.ACTION_CANCEL) {
            leftEdgeEffect.onRelease()
            rightEdgeEffect.onRelease()
            topEdgeEffect.onRelease()
            bottomEdgeEffect.onRelease()
            if (leftEdgeEffect.isFinished.not() ||
                rightEdgeEffect.isFinished.not() ||
                topEdgeEffect.isFinished.not() ||
                bottomEdgeEffect.isFinished.not()) {
                postInvalidateOnAnimation()
            }
            parent?.requestDisallowInterceptTouchEvent(false)
        }
        return scaleHandled || scrollHandled || super.onTouchEvent(event)
    }

    private fun formatDuration(durationMillis: Long): String {
        val absMillis = abs(durationMillis)
        val days = absMillis / DAY_MILLIS
        val hours = (absMillis % DAY_MILLIS) / HOUR_MILLIS
        val minutes = (absMillis % HOUR_MILLIS) / MINUTE_MILLIS
        return when {
            days > 0 && hours > 0 -> "${days}d ${hours}h"
            days > 0 -> "${days}d"
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            else -> "${minutes}m"
        }
    }

    private fun formatRelativeDayLabelWithTime(millis: Long): String {
        val nowStart = startOfDayMillis(NowProvider.nowMillis())
        val targetStart = startOfDayMillis(millis)
        val diffDays = ((nowStart - targetStart) / DAY_MILLIS).coerceAtLeast(0L)
        val dayLabel = when (diffDays) {
            0L -> "today"
            1L -> "yesterday"
            else -> "${diffDays} days ago"
        }
        return dayLabel + " " + timeFormat.format(Date(millis))
    }

    private fun startOfDayMillis(millis: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = millis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun drawEdgeEffects(canvas: Canvas, chart: RectF) {
        val width = chart.width().toInt().coerceAtLeast(0)
        val height = chart.height().toInt().coerceAtLeast(0)
        var needsInvalidate = false
        if (!topEdgeEffect.isFinished) {
            topEdgeEffect.setSize(width, height)
            val restore = canvas.save()
            canvas.translate(chart.left, chart.top)
            needsInvalidate = topEdgeEffect.draw(canvas) || needsInvalidate
            canvas.restoreToCount(restore)
        }
        if (!bottomEdgeEffect.isFinished) {
            bottomEdgeEffect.setSize(width, height)
            val restore = canvas.save()
            canvas.translate(chart.left, chart.bottom)
            canvas.rotate(180f, 0f, 0f)
            needsInvalidate = bottomEdgeEffect.draw(canvas) || needsInvalidate
            canvas.restoreToCount(restore)
        }
        if (!leftEdgeEffect.isFinished) {
            leftEdgeEffect.setSize(height, width)
            val restore = canvas.save()
            canvas.translate(chart.left, chart.top)
            canvas.rotate(270f, 0f, 0f)
            canvas.translate(-chart.height(), 0f)
            needsInvalidate = leftEdgeEffect.draw(canvas) || needsInvalidate
            canvas.restoreToCount(restore)
        }
        if (!rightEdgeEffect.isFinished) {
            rightEdgeEffect.setSize(height, width)
            val restore = canvas.save()
            canvas.translate(chart.right, chart.top)
            canvas.rotate(90f, 0f, 0f)
            needsInvalidate = rightEdgeEffect.draw(canvas) || needsInvalidate
            canvas.restoreToCount(restore)
        }
        if (needsInvalidate) {
            postInvalidateOnAnimation()
        }
    }

    fun resetZoom() {
        zoomScale = 1f
        zoomStartMillis = null
        zoomScaleY = 1f
        zoomMinY = null
        updateZoomState()
        invalidate()
    }

    fun setOnZoomChangedListener(listener: ((Boolean) -> Unit)?) {
        zoomChangedListener = listener
        updateZoomState()
    }

    private fun updateZoomState() {
        val isZoomed = zoomScale > MIN_ZOOM || zoomScaleY > MIN_ZOOM
        if (isZoomed != zoomedIn) {
            zoomedIn = isZoomed
            zoomChangedListener?.invoke(isZoomed)
        }
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleX = if (detector.previousSpanX > 0f) {
                detector.currentSpanX / detector.previousSpanX
            } else {
                detector.scaleFactor
            }
            val scaleY = if (detector.previousSpanY > 0f) {
                detector.currentSpanY / detector.previousSpanY
            } else {
                detector.scaleFactor
            }
            val newScale = (zoomScale * scaleX)
                .coerceIn(MIN_ZOOM, MAX_ZOOM)
            val newScaleY = (zoomScaleY * scaleY)
                .coerceIn(MIN_ZOOM, MAX_ZOOM)
            if (newScale == zoomScale && newScaleY == zoomScaleY) {
                return true
            }
            val chart = lastChartRect
            val focusRatio = if (chart != null && chart.width() > 0f) {
                ((detector.focusX - chart.left) / chart.width()).coerceIn(0f, 1f)
            } else {
                0.5f
            }
            val (oldStart, oldEnd) = currentRange()
            val oldSpan = (oldEnd - oldStart).coerceAtLeast(1L)
            val focusMillis = oldStart + (oldSpan * focusRatio).toLong()
            val focusRatioY = if (chart != null && chart.height() > 0f) {
                ((chart.bottom - detector.focusY) / chart.height()).coerceIn(0f, 1f)
            } else {
                0.5f
            }
            val (oldMinY, oldMaxY) = currentYRange()
            val oldSpanY = (oldMaxY - oldMinY).coerceAtLeast(0.1f)
            val focusValueY = oldMinY + oldSpanY * focusRatioY
            zoomScale = newScale
            val baseSpan = (rangeEndMillis - rangeStartMillis).coerceAtLeast(1L)
            val newSpan = (baseSpan / zoomScale).toLong().coerceAtLeast(1L)
            var newStart = focusMillis - (newSpan * focusRatio).toLong()
            val maxStart = (rangeEndMillis - newSpan).coerceAtLeast(rangeStartMillis)
            newStart = newStart.coerceIn(rangeStartMillis, maxStart)
            zoomStartMillis = if (zoomScale == MIN_ZOOM) null else newStart
            zoomScaleY = newScaleY
            val baseSpanY = (maxY - minY).coerceAtLeast(1f)
            val newSpanY = (baseSpanY / zoomScaleY).coerceAtLeast(0.1f)
            var newMinY = focusValueY - newSpanY * focusRatioY
            val maxMinY = (maxY - newSpanY).coerceAtLeast(minY)
            newMinY = newMinY.coerceIn(minY, maxMinY)
            zoomMinY = if (zoomScaleY == MIN_ZOOM) null else newMinY
            updateZoomState()
            invalidate()
            return true
        }
    }

    private inner class ScrollListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            return true
        }

        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            val chart = lastChartRect ?: return false
            val baseSpan = (rangeEndMillis - rangeStartMillis).coerceAtLeast(1L)
            val span = (baseSpan / zoomScale).toLong().coerceAtLeast(1L)
            val chartWidth = chart.width()
            val chartHeight = chart.height()
            if (chartWidth <= 0f || chartHeight <= 0f) {
                return false
            }
            val canPanX = span < baseSpan
            val (currentMinY, currentMaxY) = currentYRange()
            val baseSpanY = (maxY - minY).coerceAtLeast(0.1f)
            val spanY = (currentMaxY - currentMinY).coerceAtLeast(0.1f)
            val canPanY = spanY < baseSpanY
            if (!canPanX && !canPanY) {
                return false
            }
            parent?.requestDisallowInterceptTouchEvent(true)
            var changed = false
            if (canPanX) {
                val currentStart = currentRange().first
                val millisPerPixel = span.toFloat() / chartWidth
                val deltaMillis = (distanceX * millisPerPixel).toLong()
                val maxStart = (rangeEndMillis - span).coerceAtLeast(rangeStartMillis)
                val unclampedStart = currentStart + deltaMillis
                val newStart = unclampedStart.coerceIn(rangeStartMillis, maxStart)
                if (unclampedStart != newStart && millisPerPixel > 0f) {
                    val overscrollMillis = (unclampedStart - newStart).toFloat()
                    val overscrollPx = overscrollMillis / millisPerPixel
                    val pullDistance = abs(overscrollPx) / chartWidth
                    if (overscrollMillis < 0) {
                        leftEdgeEffect.onPull(pullDistance)
                    } else {
                        rightEdgeEffect.onPull(pullDistance)
                    }
                    postInvalidateOnAnimation()
                }
                if (newStart != currentStart) {
                    zoomStartMillis = newStart
                    changed = true
                }
            }
            if (canPanY) {
                val unitsPerPixel = spanY / chartHeight
                val deltaY = -distanceY * unitsPerPixel
                val maxMinY = (maxY - spanY).coerceAtLeast(minY)
                val unclampedMinY = currentMinY + deltaY
                val newMinY = unclampedMinY.coerceIn(minY, maxMinY)
                if (unclampedMinY != newMinY && unitsPerPixel > 0f) {
                    val overscrollUnits = unclampedMinY - newMinY
                    val overscrollPx = overscrollUnits / unitsPerPixel
                    val pullDistance = abs(overscrollPx) / chartHeight
                    if (overscrollUnits < 0f) {
                        bottomEdgeEffect.onPull(pullDistance)
                    } else {
                        topEdgeEffect.onPull(pullDistance)
                    }
                    postInvalidateOnAnimation()
                }
                if (newMinY != currentMinY) {
                    zoomMinY = newMinY
                    changed = true
                }
            }
            if (changed) {
                invalidate()
            }
            return true
        }
    }

    companion object {
        private const val MIN_ZOOM = 1f
        private const val MAX_ZOOM = 4f
        private const val DAY_MILLIS = 24L * 60L * 60L * 1000L
        private const val HOUR_MILLIS = 60L * 60L * 1000L
        private const val MINUTE_MILLIS = 60L * 1000L
    }
}
