package com.rekixo.ludopaws.core.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import com.rekixo.ludopaws.core.ui.art.recessedPlywoodBackground

/**
 * Shared close control for plywood-surfaced dialogs.
 * Matches the Settings popup's recessed timber pocket and rounded red cross.
 */
internal class PlywoodCloseButton(
    context: Context,
    contentDescription: String,
    onClose: () -> Unit,
) : View(context) {
    private val crossPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFD95757.toInt()
            style = Paint.Style.STROKE
            strokeWidth = closeDp(context, 6f).toFloat()
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

    init {
        this.contentDescription = contentDescription
        isClickable = true
        isFocusable = true
        minimumWidth = closeDp(context, 40)
        minimumHeight = closeDp(context, 40)
        background = recessedPlywoodBackground(
            context = context,
            outerCornerRadiusDp = 18f,
        )
        elevation = 0f
        setOnClickListener { onClose() }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val arm = closeDp(context, 8.4f).toFloat()
        canvas.drawLine(cx - arm, cy - arm, cx + arm, cy + arm, crossPaint)
        canvas.drawLine(cx + arm, cy - arm, cx - arm, cy + arm, crossPaint)
    }

    private fun closeDp(context: Context, value: Float): Int =
        (value * context.resources.displayMetrics.density).toInt()

    private fun closeDp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()
}
