package com.ludoproof.game.feature.store.presentation.art

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.view.View
import kotlin.math.min

class GemClusterView(
    context: Context,
    private val gemCount: Int = 1,
) : View(context) {
    private val fillPaint =
        Paint(Paint.ANTI_ALIAS_FLAG)
    private val facetPaint =
        Paint(Paint.ANTI_ALIAS_FLAG)
    private val outlinePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(1.4f)
            color = 0xBFFFFFFF.toInt()
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val side = min(width, height).toFloat()
        if (side <= 0f) return

        val gemSize = when (gemCount.coerceIn(1, 5)) {
            1 -> side * 0.70f
            2 -> side * 0.61f
            3 -> side * 0.52f
            4 -> side * 0.46f
            else -> side * 0.42f
        }
        val centerX = width / 2f
        val centerY = height / 2f
        val placements =
            when (gemCount.coerceIn(1, 5)) {
                1 -> listOf(GemPlacement(0f, 0f, 1f))
                2 -> listOf(
                    GemPlacement(-0.19f, 0.08f, 0.92f),
                    GemPlacement(0.19f, -0.08f, 0.92f),
                )
                3 -> listOf(
                    GemPlacement(0f, -0.18f, 0.88f),
                    GemPlacement(-0.20f, 0.14f, 0.76f),
                    GemPlacement(0.20f, 0.14f, 0.76f),
                )
                4 -> listOf(
                    GemPlacement(-0.18f, -0.16f, 0.72f),
                    GemPlacement(0.18f, -0.16f, 0.72f),
                    GemPlacement(-0.18f, 0.16f, 0.72f),
                    GemPlacement(0.18f, 0.16f, 0.72f),
                )
                else -> listOf(
                    GemPlacement(0f, -0.20f, 0.70f),
                    GemPlacement(-0.21f, -0.02f, 0.70f),
                    GemPlacement(0.21f, -0.02f, 0.70f),
                    GemPlacement(-0.12f, 0.20f, 0.70f),
                    GemPlacement(0.12f, 0.20f, 0.70f),
                )
            }

        placements.forEach { placement ->
            drawGem(
                canvas = canvas,
                centerX = centerX + placement.x * side,
                centerY = centerY + placement.y * side,
                size = gemSize * placement.scale,
            )
        }
    }

    private fun drawGem(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        size: Float,
    ) {
        val halfWidth = size * 0.46f
        val crownY = centerY - size * 0.24f
        val top = centerY - size * 0.48f
        val bottom = centerY + size * 0.50f
        val left = centerX - halfWidth
        val right = centerX + halfWidth
        val shoulderLeft = centerX - size * 0.31f
        val shoulderRight = centerX + size * 0.31f

        val body =
            Path().apply {
                moveTo(left, crownY)
                lineTo(shoulderLeft, top)
                lineTo(shoulderRight, top)
                lineTo(right, crownY)
                lineTo(centerX, bottom)
                close()
            }
        fillPaint.shader =
            LinearGradient(
                left,
                top,
                right,
                bottom,
                intArrayOf(0xFF9BFFF1.toInt(), 0xFF27D8CF.toInt(), 0xFF087DC4.toInt()),
                null,
                Shader.TileMode.CLAMP,
            )
        canvas.drawPath(body, fillPaint)

        facetPaint.shader = null
        facetPaint.color = 0xBFFFFFFF.toInt()
        canvas.drawPath(
            Path().apply {
                moveTo(left, crownY)
                lineTo(shoulderLeft, top)
                lineTo(centerX, crownY)
                lineTo(centerX, bottom)
                close()
            },
            facetPaint,
        )
        facetPaint.color = 0x5539F4E5
        canvas.drawPath(
            Path().apply {
                moveTo(shoulderLeft, top)
                lineTo(shoulderRight, top)
                lineTo(right, crownY)
                lineTo(centerX, crownY)
                close()
            },
            facetPaint,
        )
        facetPaint.color = 0x8A0569AB.toInt()
        canvas.drawPath(
            Path().apply {
                moveTo(centerX, crownY)
                lineTo(right, crownY)
                lineTo(centerX, bottom)
                close()
            },
            facetPaint,
        )
        canvas.drawPath(body, outlinePaint)
        fillPaint.shader = null

        fillPaint.color = Color.WHITE
        canvas.drawCircle(
            centerX - size * 0.17f,
            centerY - size * 0.23f,
            size * 0.035f,
            fillPaint,
        )
    }

    private fun dp(value: Float): Float =
        value * resources.displayMetrics.density

    private data class GemPlacement(
        val x: Float,
        val y: Float,
        val scale: Float,
    )
}
