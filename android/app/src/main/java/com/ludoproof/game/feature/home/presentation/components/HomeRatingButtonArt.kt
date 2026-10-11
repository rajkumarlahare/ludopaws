package com.ludoproof.game.ui.home

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Crisp, high-contrast four-colour Google Play mark for the Home rating action. */
internal class HomeGooglePlayIconView(
    context: Context,
) : View(context) {
    private val paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val size = min(width, height).toFloat()
        if (size <= 0f) return

        val scale = size / 100f
        canvas.save()
        canvas.translate((width - size) / 2f, (height - size) / 2f)
        canvas.scale(scale, scale)

        // Flat, fully opaque brand colours keep the small mark sharp and readable.
        // A broad rounded silhouette avoids a narrow, samosa-like triangle.
        val blue =
            Path().apply {
                moveTo(22f, 11f)
                quadTo(14f, 11f, 14f, 21f)
                lineTo(14f, 79f)
                quadTo(14f, 89f, 22f, 89f)
                lineTo(52f, 50f)
                close()
            }
        paint.color = 0xFF18A8F4.toInt()
        canvas.drawPath(blue, paint)

        val green =
            Path().apply {
                moveTo(22f, 11f)
                quadTo(24f, 11f, 28f, 14f)
                lineTo(57f, 38f)
                lineTo(52f, 50f)
                close()
            }
        paint.color = 0xFF00D66F.toInt()
        canvas.drawPath(green, paint)

        val red =
            Path().apply {
                moveTo(22f, 89f)
                quadTo(24f, 89f, 28f, 86f)
                lineTo(57f, 62f)
                lineTo(52f, 50f)
                close()
            }
        paint.color = 0xFFFF3B49.toInt()
        canvas.drawPath(red, paint)

        val yellow =
            Path().apply {
                // Keep the yellow segment tight around the Play mark's right-facing point.
                // The previous wide diamond made the tip read like a yellow smudge.
                moveTo(52f, 50f)
                lineTo(57f, 38f)
                lineTo(72f, 34f)
                lineTo(88f, 50f)
                lineTo(72f, 66f)
                lineTo(57f, 62f)
                close()
            }
        paint.color = 0xFFFFC928.toInt()
        canvas.drawPath(yellow, paint)

        // Tiny opaque centre join removes muddy overlapping gradients at this size.
        paint.color = 0xFF20B8E8.toInt()
        canvas.drawCircle(52f, 50f, 2.2f, paint)

        canvas.restore()
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
