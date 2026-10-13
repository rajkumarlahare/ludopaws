package com.rekixo.ludopaws.core.ui.components

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

/** Small cyan laser-status LED; animation runs only while the view is attached. */
internal class LaserStatusDotView(context: Context) : View(context) {
    private var pulse = 0f
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val pulseAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 680L
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.REVERSE
        interpolator = AccelerateDecelerateInterpolator()
        addUpdateListener { animator ->
            pulse = animator.animatedValue as Float
            invalidate()
        }
    }

    init {
        // Software drawing enables the soft halo around the bright core.
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredSize = dp(10f).toInt()
        setMeasuredDimension(
            resolveSize(desiredSize, widthMeasureSpec),
            resolveSize(desiredSize, heightMeasureSpec),
        )
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!pulseAnimator.isStarted) pulseAnimator.start()
    }

    override fun onDetachedFromWindow() {
        pulseAnimator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = minOf(width, height).toFloat()
        if (size <= 0f) return
        val cx = width / 2f
        val cy = height / 2f
        val intensity = pulse.coerceIn(0f, 1f)
        val glowAlpha = (24f + intensity * 76f).toInt().coerceIn(0, 255)
        val shadowAlpha = (80f + intensity * 155f).toInt().coerceIn(0, 255)
        glowPaint.color = Color.argb(glowAlpha, 55, 244, 255)
        glowPaint.setShadowLayer(
            dp(1.5f + intensity * 3f),
            0f,
            0f,
            Color.argb(shadowAlpha, 55, 244, 255),
        )
        canvas.drawCircle(cx, cy, size * (0.33f + intensity * 0.06f), glowPaint)
        glowPaint.clearShadowLayer()
        corePaint.color = Color.argb(
            (175f + intensity * 80f).toInt().coerceIn(0, 255),
            207,
            255,
            255,
        )
        canvas.drawCircle(cx, cy, size * 0.20f, corePaint)
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
}
