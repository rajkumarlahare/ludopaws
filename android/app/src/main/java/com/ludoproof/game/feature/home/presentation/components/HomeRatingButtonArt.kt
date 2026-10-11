package com.ludoproof.game.ui.home

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Smooth, four-colour Google Play mark used by the Home rating action. */
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

        // Keep the Play mark broad and give the two left corners a soft radius,
        // instead of rendering it as one hard, samosa-like triangle.
        val outer =
            Path().apply {
                moveTo(22f, 11f)
                quadTo(14f, 11f, 14f, 21f)
                lineTo(14f, 79f)
                quadTo(14f, 89f, 22f, 89f)
                lineTo(83f, 56f)
                quadTo(93f, 50f, 83f, 44f)
                close()
            }

        paint.shader = null
        paint.color = 0x66000000
        canvas.save()
        canvas.translate(0f, 3f)
        canvas.drawPath(outer, paint)
        canvas.restore()

        // Blue forms the rounded back of the mark.
        val blue =
            Path().apply {
                moveTo(22f, 11f)
                quadTo(14f, 11f, 14f, 21f)
                lineTo(14f, 79f)
                quadTo(14f, 89f, 22f, 89f)
                lineTo(51f, 50f)
                close()
            }
        paint.shader =
            LinearGradient(
                16f, 15f, 52f, 86f,
                intArrayOf(0xFF39C9F5.toInt(), 0xFF1687E4.toInt()),
                null,
                Shader.TileMode.CLAMP,
            )
        canvas.drawPath(blue, paint)

        // Green and red split the upper and lower arms of the Play symbol.
        val green =
            Path().apply {
                moveTo(22f, 11f)
                quadTo(24f, 11f, 28f, 14f)
                lineTo(56f, 39f)
                lineTo(51f, 50f)
                lineTo(22f, 11f)
                close()
            }
        paint.shader =
            LinearGradient(
                22f, 12f, 63f, 44f,
                intArrayOf(0xFF42E477.toInt(), 0xFF08B96A.toInt()),
                null,
                Shader.TileMode.CLAMP,
            )
        canvas.drawPath(green, paint)

        val red =
            Path().apply {
                moveTo(22f, 89f)
                quadTo(24f, 89f, 28f, 86f)
                lineTo(56f, 61f)
                lineTo(51f, 50f)
                lineTo(22f, 89f)
                close()
            }
        paint.shader =
            LinearGradient(
                22f, 88f, 63f, 56f,
                intArrayOf(0xFFFF7467.toInt(), 0xFFEA3348.toInt()),
                null,
                Shader.TileMode.CLAMP,
            )
        canvas.drawPath(red, paint)

        // Yellow makes the recognisable forward-facing tip; curved joins keep
        // the four colours reading as one continuous Play Store symbol.
        val yellow =
            Path().apply {
                moveTo(51f, 50f)
                lineTo(56f, 39f)
                lineTo(70f, 31f)
                lineTo(83f, 44f)
                quadTo(93f, 50f, 83f, 56f)
                lineTo(70f, 69f)
                lineTo(56f, 61f)
                close()
            }
        paint.shader =
            LinearGradient(
                56f, 34f, 85f, 66f,
                intArrayOf(0xFFFFE36A.toInt(), 0xFFFFB82F.toInt()),
                null,
                Shader.TileMode.CLAMP,
            )
        canvas.drawPath(yellow, paint)

        // A restrained gloss softens the finish without a harsh white divider.
        val gloss =
            Path().apply {
                moveTo(21f, 20f)
                quadTo(20f, 17f, 24f, 19f)
                lineTo(45f, 40f)
                lineTo(40f, 43f)
                close()
            }
        paint.shader =
            LinearGradient(
                20f, 18f, 47f, 44f,
                intArrayOf(0x9EFFFFFF.toInt(), 0x18FFFFFF, Color.TRANSPARENT),
                floatArrayOf(0f, 0.62f, 1f),
                Shader.TileMode.CLAMP,
            )
        canvas.drawPath(gloss, paint)
        paint.shader = null

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
