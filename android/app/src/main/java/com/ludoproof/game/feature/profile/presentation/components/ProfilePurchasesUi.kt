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
    // Multiplying RGB by ~0.6 darkens the entire plywood finish by about 40%
    // while retaining the natural tone differences between the grain and highlights.
    private val plywoodShade = android.graphics.LightingColorFilter(
        0xFF999999.toInt(),
        Color.TRANSPARENT,
    )
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        colorFilter = plywoodShade
    }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        colorFilter = plywoodShade
    }
    private var drawableAlpha = 255

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

        // Reference plywood is fairly evenly lit, with only a soft warm shift.
        // Avoid the darker band through the middle/lower part of the card.
        fill.shader = LinearGradient(
            b.left,
            b.top,
            b.left,
            b.bottom,
            intArrayOf(
                0xFFF0D0A1.toInt(),
                0xFFECC796.toInt(),
                0xFFECC796.toInt(),
                0xFFEFD0A0.toInt(),
            ),
            floatArrayOf(0f, 0.28f, 0.72f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(b, fill)
        fill.shader = null

        // Fine, restrained plywood grain: lower opacity keeps it from reading as stripes.
        // Irregular paths vary from one another, like natural veneer texture.
        val grainCount = 25
        for (index in 0 until grainCount) {
            val fraction = (index + 0.35f + (index % 4) * 0.11f) / grainCount
            val gx = b.left + b.width() * fraction
            val wave = density * (1.8f + (index % 5) * 0.8f)
            val driftA = ((index * 13) % 17 - 8) * density
            val driftB = ((index * 7) % 13 - 6) * density
            val grain = Path().apply {
                moveTo(gx + driftA * 0.15f, b.top)
                cubicTo(
                    gx - wave,
                    b.top + b.height() * 0.18f,
                    gx + wave * 0.55f + driftA,
                    b.top + b.height() * 0.34f,
                    gx + driftB,
                    b.top + b.height() * 0.51f,
                )
                cubicTo(
                    gx - wave * 0.7f + driftB,
                    b.top + b.height() * 0.70f,
                    gx + wave * 0.8f + driftA * 0.25f,
                    b.top + b.height() * 0.84f,
                    gx + driftA * 0.1f,
                    b.bottom,
                )
            }
            line.color = when (index % 4) {
                0 -> Color.argb(16, 112, 66, 32)
                1 -> Color.argb(12, 255, 241, 205)
                2 -> Color.argb(14, 128, 76, 35)
                else -> Color.argb(10, 255, 240, 205)
            }
            line.strokeWidth = (if (index % 5 == 0) 1.05f else 0.6f) * density
            canvas.drawPath(grain, line)
        }

        // A few faint, irregular grain streaks add natural plywood variation
        // without the regular spacing that made the previous version look striped.
        for (index in 0 until 8) {
            val yOffset = b.height() * (0.08f + (index * 0.113f))
            val gx = b.left + b.width() * (((index * 19) % 83) / 100f + 0.04f)
            val streak = Path().apply {
                moveTo(gx, b.top + yOffset)
                cubicTo(
                    gx + density * (4f + index % 3),
                    b.top + yOffset + density * 2f,
                    gx - density * (5f + index % 4),
                    b.top + yOffset + density * 4f,
                    gx + density * 2f,
                    b.top + yOffset + density * 6f,
                )
            }
            line.color = Color.argb(8, 119, 71, 36)
            line.strokeWidth = 0.55f * density
            canvas.drawPath(streak, line)
        }

        // Tiny low-contrast wood knots, spaced irregularly and without plank seams.
        if (b.width() > 150f * density && b.height() > 70f * density) {
            val knots = listOf(
                (b.left + b.width() * 0.24f) to (b.top + b.height() * 0.31f),
                (b.left + b.width() * 0.73f) to (b.top + b.height() * 0.72f),
            )
            knots.forEachIndexed { index, (knotX, knotY) ->
                line.color = Color.argb(11, 119, 65, 32)
                line.strokeWidth = 0.55f * density
                canvas.drawOval(
                    RectF(
                        knotX - 2.4f * density,
                        knotY - 4.8f * density,
                        knotX + 2.4f * density,
                        knotY + 4.8f * density,
                    ),
                    line,
                )
                line.color = Color.argb(10, 255, 232, 181)
                line.strokeWidth = 0.45f * density
                canvas.drawOval(
                    RectF(
                        knotX - 1.1f * density,
                        knotY - 2.4f * density,
                        knotX + 1.1f * density,
                        knotY + 2.4f * density,
                    ),
                    line,
                )
            }
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
        // Keep the requested plywood darkening and optionally compose a caller tint.
        fill.colorFilter = colorFilter ?: plywoodShade
        line.colorFilter = colorFilter ?: plywoodShade
        invalidateSelf()
    }

    @Deprecated("Deprecated in Android")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

