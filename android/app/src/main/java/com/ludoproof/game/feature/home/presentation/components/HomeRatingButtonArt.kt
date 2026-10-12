package com.ludoproof.game.ui.home

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import com.ludoproof.game.R
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Google Play mark rendered from a vector path asset for clean, exact colour joins. */
internal class HomeGooglePlayIconView(
    context: Context,
) : View(context) {
    private val playMark =
        context.getDrawable(R.drawable.ic_google_play_mark)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val drawable = playMark ?: return
        val side = min(width, height).toFloat()
        if (side <= 0f) return

        // Preserve the original prism's portrait proportions instead of stretching it
        // into a triangle; all four colour segments come from the same vector asset.
        val markHeight = (side * 0.92f * 0.8f).toInt().coerceAtLeast(1)
        val markWidth = (markHeight * 38.1f / 44.1f).toInt().coerceAtLeast(1)
        val left = (width - markWidth) / 2
        val top = (height - markHeight) / 2
        drawable.setBounds(left, top, left + markWidth, top + markHeight)
        drawable.draw(canvas)
    }
}

/** Five compact gold stars with depth and a top-light reflection. */
internal class HomeRatingStarsView(
    context: Context,
) : View(context) {
    private val paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
    private val outline =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (width <= 0 || height <= 0) return

        val gap = width * 0.018f
        val starWidth = (width - gap * 4f) / 5f
        val radius = min(starWidth * 0.46f, height * 0.43f)
        val centerY = height * 0.48f

        for (index in 0 until 5) {
            val centerX =
                starWidth * index +
                    starWidth / 2f +
                    gap * index

            val shadowPath =
                starPath(
                    centerX = centerX,
                    centerY = centerY + radius * 0.16f,
                    outerRadius = radius,
                    innerRadius = radius * 0.46f,
                )
            paint.shader = null
            paint.color = 0xA25C2D00.toInt()
            canvas.drawPath(shadowPath, paint)

            val star =
                starPath(
                    centerX = centerX,
                    centerY = centerY,
                    outerRadius = radius,
                    innerRadius = radius * 0.46f,
                )
            paint.shader =
                LinearGradient(
                    0f,
                    centerY - radius,
                    0f,
                    centerY + radius,
                    intArrayOf(
                        0xFFFFF7B5.toInt(),
                        0xFFFFD957.toInt(),
                        0xFFFFB83D.toInt(),
                    ),
                    floatArrayOf(0f, 0.54f, 1f),
                    Shader.TileMode.CLAMP,
                )
            canvas.drawPath(star, paint)
            paint.shader = null

            outline.color = 0xFFE6AD3E.toInt()
            outline.strokeWidth = (radius * 0.11f).coerceAtLeast(1f)
            canvas.drawPath(star, outline)

            // A tiny upper-left highlight gives each star a toy-like 3D finish.
            paint.color = 0xB8FFFFFF.toInt()
            canvas.drawCircle(
                centerX - radius * 0.20f,
                centerY - radius * 0.36f,
                radius * 0.075f,
                paint,
            )
        }
    }

    private fun starPath(
        centerX: Float,
        centerY: Float,
        outerRadius: Float,
        innerRadius: Float,
    ): Path =
        Path().apply {
            for (index in 0 until 10) {
                val radius =
                    if (index % 2 == 0) {
                        outerRadius
                    } else {
                        innerRadius
                    }
                val angle =
                    Math.toRadians(
                        -90.0 + index * 36.0,
                    )
                val x =
                    centerX +
                        cos(angle).toFloat() * radius
                val y =
                    centerY +
                        sin(angle).toFloat() * radius
                if (index == 0) {
                    moveTo(x, y)
                } else {
                    lineTo(x, y)
                }
            }
            close()
        }
}
