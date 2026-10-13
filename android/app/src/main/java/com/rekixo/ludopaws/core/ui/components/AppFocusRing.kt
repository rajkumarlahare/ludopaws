package com.rekixo.ludopaws.core.ui.components

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.view.View
import com.rekixo.ludopaws.core.ui.theme.AppUiTokens
import kotlin.math.min

/**
 * Adds a visible gold outline when a custom-styled control receives keyboard or
 * accessibility focus, while preserving its existing background and foreground
 * (including ripple feedback).
 */
internal object AppFocusRing {
    fun apply(
        view: View,
        cornerRadiusDp: Float = AppUiTokens.Shape.CONTROL_RADIUS_DP,
        strokeWidthDp: Float = AppUiTokens.Icon.NORMAL_STROKE_DP,
        color: Int = 0xFFFFD45E.toInt(),
    ) {
        val ring = AppFocusRingDrawable(
            context = view.context,
            cornerRadiusDp = cornerRadiusDp,
            strokeWidthDp = strokeWidthDp,
            color = color,
        )
        val previousForeground = view.foreground
        view.foreground = if (previousForeground == null) {
            ring
        } else {
            LayerDrawable(arrayOf(previousForeground, ring))
        }
        view.refreshDrawableState()
    }
}

/** Stateful overlay drawable used by [AppFocusRing] and pixel-level UI tests. */
internal class AppFocusRingDrawable(
    context: android.content.Context,
    cornerRadiusDp: Float = AppUiTokens.Shape.CONTROL_RADIUS_DP,
    strokeWidthDp: Float = AppUiTokens.Icon.NORMAL_STROKE_DP,
    color: Int = 0xFFFFD45E.toInt(),
) : Drawable() {
    private val density = context.resources.displayMetrics.density
    private val cornerRadius = cornerRadiusDp.coerceAtLeast(0f) * density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = strokeWidthDp.coerceIn(0.5f, 8f) * density
        this.color = color
    }
    private var focused = false
    private var drawableAlpha = 255

    override fun draw(canvas: Canvas) {
        if (!focused) return
        val bounds = bounds
        val inset = paint.strokeWidth / 2f
        val rect = RectF(
            bounds.left + inset,
            bounds.top + inset,
            bounds.right - inset,
            bounds.bottom - inset,
        )
        if (rect.width() <= 0f || rect.height() <= 0f) return
        val radius = min(cornerRadius, min(rect.width(), rect.height()) / 2f)
        paint.alpha = drawableAlpha
        canvas.drawRoundRect(rect, radius, radius, paint)
    }

    override fun isStateful(): Boolean = true

    override fun onStateChange(stateSet: IntArray): Boolean {
        val nextFocused = stateSet.contains(android.R.attr.state_focused)
        if (nextFocused == focused) return false
        focused = nextFocused
        invalidateSelf()
        return true
    }

    override fun setAlpha(alpha: Int) {
        drawableAlpha = alpha.coerceIn(0, 255)
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {
        paint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
