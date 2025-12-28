package com.mindwarrior.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt

class QuickStartOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val focusRect = RectF()
    private var focusRadius = 0f

    init {
        setWillNotDraw(false)
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        dimPaint.style = Paint.Style.FILL
        dimPaint.color = 0x99000000.toInt()
        clearPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    fun setDimColor(@ColorInt color: Int) {
        dimPaint.color = color
        invalidate()
    }

    fun setFocusRect(left: Float, top: Float, right: Float, bottom: Float, radius: Float) {
        focusRect.set(left, top, right, bottom)
        focusRadius = radius
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), dimPaint)
        if (!focusRect.isEmpty) {
            canvas.drawRoundRect(focusRect, focusRadius, focusRadius, clearPaint)
        }
    }
}
