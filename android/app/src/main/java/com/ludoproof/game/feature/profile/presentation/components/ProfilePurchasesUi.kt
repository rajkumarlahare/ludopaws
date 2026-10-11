package com.ludoproof.game.feature.profile.presentation.components

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.ludoproof.game.LudoProofTheme
import com.ludoproof.game.feature.profile.domain.model.ProfileSnapshot
import com.ludoproof.game.feature.profile.presentation.ProfileActivity

internal fun ProfileActivity.profilePurchasesPanel(profile: ProfileSnapshot): LinearLayout =
    profilePanel().apply {
        // Test the reference's realistic plywood look on this section only.
        // Other Profile panels continue using the standard blue profilePanel background.
        background = PlywoodTextureDrawable(resources.displayMetrics.density)

        val heading = LinearLayout(this@profilePurchasesPanel).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        heading.addView(
            profileTitle("Purchased Products"),
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
        )
        heading.addView(
            Button(this@profilePurchasesPanel).apply {
                text = "VIEW ALL  ›"
                textSize = 11f
                setTypeface(Typeface.DEFAULT_BOLD)
                setTextColor(0xFFFFD43B.toInt())
                minWidth = 0
                minHeight = 0
                setPadding(0, 0, 0, 0)
                background = null
                contentDescription = "View all purchased products"
                setOnClickListener {
                    viewPurchases(
                        if (profile.purchasedProducts.isEmpty()) {
                            "No purchases yet."
                        } else {
                            profile.purchasedProducts.joinToString("\n") { "• $it" }
                        },
                    )
                }
            },
        )
        addView(heading)

        if (profile.purchasedProducts.isEmpty()) {
            val emptyState = LinearLayout(this@profilePurchasesPanel).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(0, dp(10), 0, 0)
            }
            emptyState.addView(
                TextView(this@profilePurchasesPanel).apply {
                    text = "🛒"
                    textSize = 32f
                    gravity = Gravity.CENTER
                    setTextColor(0xFFBDE5FF.toInt())
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(0x1C3DBEFF)
                        setStroke(dp(1), 0x663EABF3)
                    }
                },
                LinearLayout.LayoutParams(dp(60), dp(60)),
            )
            emptyState.addView(
                TextView(this@profilePurchasesPanel).apply {
                    text = "No purchases yet"
                    textSize = 15f
                    setTypeface(Typeface.DEFAULT_BOLD)
                    setTextColor(Color.WHITE)
                    gravity = Gravity.CENTER
                },
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = dp(8) },
            )
            emptyState.addView(
                TextView(this@profilePurchasesPanel).apply {
                    text = "Explore the store for exciting items\nand upgrades!"
                    textSize = 12f
                    setTextColor(0xFFD0E7FF.toInt())
                    gravity = Gravity.CENTER
                    setLineSpacing(dp(2).toFloat(), 1f)
                    setPadding(dp(4), dp(4), dp(4), 0)
                },
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ),
            )
            addView(emptyState)
        } else {
            addView(
                TextView(this@profilePurchasesPanel).apply {
                    text = profile.purchasedProducts.joinToString("  •  ")
                    textSize = 13f
                    setTextColor(0xFFE8F5FF.toInt())
                    gravity = Gravity.CENTER
                    setPadding(0, dp(24), 0, dp(20))
                },
            )
        }
    }

/**
 * Code-drawn plywood texture for the Purchased Products panel test.
 * Vertical boards, subtle grain, seams and knots are painted at runtime:
 * no bitmap/image asset is used.
 */
private class PlywoodTextureDrawable(
    private val density: Float,
) : Drawable() {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private var drawableAlpha = 255

    private val boardWidthsDp = floatArrayOf(62f, 70f, 66f, 74f, 60f, 68f)
    private val boardPalettes = arrayOf(
        intArrayOf(0xFFF2D49D.toInt(), 0xFFE4B978.toInt(), 0xFFC78D4F.toInt(), 0xFFE9C58B.toInt()),
        intArrayOf(0xFFEAC38A.toInt(), 0xFFD5A263.toInt(), 0xFFB97C42.toInt(), 0xFFE0B474.toInt()),
        intArrayOf(0xFFF5DCA8.toInt(), 0xFFE9C487.toInt(), 0xFFC88C4F.toInt(), 0xFFE7BD7E.toInt()),
        intArrayOf(0xFFE4B87B.toInt(), 0xFFD09A5A.toInt(), 0xFFAC713C.toInt(), 0xFFDBAD70.toInt()),
        intArrayOf(0xFFF0D09A.toInt(), 0xFFE0B476.toInt(), 0xFFC48A4E.toInt(), 0xFFE5BC80.toInt()),
        intArrayOf(0xFFE8C087.toInt(), 0xFFD9A968.toInt(), 0xFFB67A43.toInt(), 0xFFDDB176.toInt()),
    )

    override fun draw(canvas: Canvas) {
        val b = RectF(bounds)
        if (b.width() <= 0f || b.height() <= 0f) return

        val radius = 15f * density
        val outerPath = Path().apply {
            addRoundRect(b, radius, radius, Path.Direction.CW)
        }
        val layer = canvas.saveLayerAlpha(b, drawableAlpha)
        canvas.save()
        canvas.clipPath(outerPath)

        // Warm plywood base with gentle top-to-bottom light falloff.
        fill.shader = LinearGradient(
            b.left,
            b.top,
            b.left,
            b.bottom,
            intArrayOf(
                0xFFF1D4A0.toInt(),
                0xFFDDB077.toInt(),
                0xFFC58A53.toInt(),
                0xFFE5BD82.toInt(),
            ),
            null,
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(b, fill)
        fill.shader = null

        var x = b.left
        var boardIndex = 0
        while (x < b.right) {
            val width = minOf(boardWidthsDp[boardIndex % boardWidthsDp.size] * density, b.right - x)
            val palette = boardPalettes[boardIndex % boardPalettes.size]
            val boardRect = RectF(x, b.top, x + width, b.bottom)

            fill.shader = LinearGradient(
                boardRect.left,
                boardRect.top,
                boardRect.right,
                boardRect.top,
                palette,
                floatArrayOf(0f, 0.24f, 0.70f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawRect(boardRect, fill)
            fill.shader = null

            // Dark seam and a narrow reflected edge make each plank read as wood.
            fill.color = Color.argb(88, 91, 48, 22)
            canvas.drawRect(
                x,
                b.top,
                minOf(x + 1.25f * density, b.right),
                b.bottom,
                fill,
            )
            fill.color = Color.argb(100, 255, 235, 190)
            canvas.drawRect(
                minOf(x + 2f * density, b.right),
                b.top,
                minOf(x + 2.7f * density, b.right),
                b.bottom,
                fill,
            )

            // Fine, slightly wandering vertical grain lines with restrained contrast.
            for (grainIndex in 0..4) {
                val gx = x + width * (0.14f + grainIndex * 0.17f)
                if (gx >= b.right) continue
                val wave = density * (1.1f + ((boardIndex + grainIndex) % 3) * 0.65f)
                val yShift = ((boardIndex * 17 + grainIndex * 23) % 43) * density
                val grain = Path().apply {
                    moveTo(gx, b.top - yShift * 0.15f)
                    cubicTo(
                        gx + wave,
                        b.top + b.height() * 0.22f,
                        gx - wave * 0.75f,
                        b.top + b.height() * 0.43f,
                        gx + wave * 0.25f,
                        b.top + b.height() * 0.61f,
                    )
                    cubicTo(
                        gx + wave * 0.7f,
                        b.top + b.height() * 0.77f,
                        gx - wave * 0.45f,
                        b.bottom - yShift * 0.12f,
                        gx + wave * 0.10f,
                        b.bottom,
                    )
                }
                line.color = if (grainIndex % 2 == 0) {
                    Color.argb(42, 111, 62, 31)
                } else {
                    Color.argb(35, 255, 237, 194)
                }
                line.strokeWidth = (if (grainIndex % 2 == 0) 0.7f else 0.55f) * density
                canvas.drawPath(grain, line)
            }

            // Occasional tiny knots, drawn as natural low-contrast curved rings.
            if (boardIndex % 3 == 1 && width > 28f * density && b.height() > 70f * density) {
                val knotX = x + width * 0.58f
                val knotY = b.top + b.height() * (if (boardIndex % 2 == 0) 0.38f else 0.68f)
                line.color = Color.argb(62, 119, 65, 32)
                line.strokeWidth = 0.85f * density
                canvas.drawOval(
                    RectF(knotX - 3.2f * density, knotY - 7f * density, knotX + 3.2f * density, knotY + 7f * density),
                    line,
                )
                line.color = Color.argb(42, 255, 232, 181)
                line.strokeWidth = 0.65f * density
                canvas.drawOval(
                    RectF(knotX - 1.6f * density, knotY - 4f * density, knotX + 1.6f * density, knotY + 4f * density),
                    line,
                )
            }

            x += width
            boardIndex += 1
        }

        canvas.restore()

        // Rounded warm timber edge and inset highlight; keeps the existing card shape.
        line.color = 0xFF9B622F.toInt()
        line.strokeWidth = 2f * density
        canvas.drawRoundRect(
            RectF(b).apply { inset(1f * density, 1f * density) },
            radius,
            radius,
            line,
        )
        line.color = 0x99FFF0C8.toInt()
        line.strokeWidth = 0.8f * density
        canvas.drawRoundRect(
            RectF(b).apply { inset(3f * density, 3f * density) },
            (radius - 2f * density).coerceAtLeast(0f),
            (radius - 2f * density).coerceAtLeast(0f),
            line,
        )

        canvas.restoreToCount(layer)
    }

    override fun setAlpha(alpha: Int) {
        drawableAlpha = alpha.coerceIn(0, 255)
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        fill.colorFilter = colorFilter
        line.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Android")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

