package com.ludoproof.game.feature.store.presentation.components

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.ludoproof.game.DiceView
import com.ludoproof.game.core.ui.art.playerCountSelectionBackground
import com.ludoproof.game.core.ui.art.plywoodBackground
import com.ludoproof.game.feature.store.domain.StoreCosmeticCatalog
import com.ludoproof.game.feature.store.domain.model.CosmeticUnlockKind
import com.ludoproof.game.feature.store.domain.model.StoreCosmetic
import com.ludoproof.game.feature.store.presentation.StoreActivity
import com.ludoproof.game.feature.store.presentation.actions.handleCosmeticTap

private const val DICE_CARD_HEIGHT_DP = 188
private const val DICE_PREVIEW_SIZE_DP = 104

internal fun StoreActivity.diceCatalogContent(): LinearLayout =
    LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL

        StoreCosmeticCatalog.dice.chunked(2).forEachIndexed { rowIndex, rowDice ->
            val row =
                LinearLayout(this@diceCatalogContent).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.TOP
                }

            rowDice.forEach { dice ->
                row.addView(
                    dicePanel(dice),
                    LinearLayout.LayoutParams(
                        0,
                        dp(DICE_CARD_HEIGHT_DP),
                        1f,
                    ).apply {
                        setMargins(dp(4), 0, dp(4), 0)
                    },
                )
            }

            repeat(2 - rowDice.size) {
                row.addView(
                    TextView(this@diceCatalogContent),
                    LinearLayout.LayoutParams(0, dp(DICE_CARD_HEIGHT_DP), 1f),
                )
            }

            addView(
                row,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply {
                    if (rowIndex > 0) topMargin = dp(10)
                },
            )
        }
    }

private fun StoreActivity.dicePanel(
    dice: StoreCosmetic,
): LinearLayout =
    LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        setPadding(dp(6), dp(8), dp(6), dp(8))

        val selected = cosmetics.selectedId(dice.category) == dice.id
        val owned = cosmetics.isOwned(dice.id)

        // Match the Board Store's plywood card and selected check treatment.
        background =
            if (selected) {
                playerCountSelectionBackground(
                    context = this@dicePanel,
                    selected = true,
                    outerCornerRadiusDp = 13f,
                    checkBadgeAtTopEnd = true,
                    emphasizeSelected = true,
                    showCheckBadge = true,
                    checkMarkScale = 2.0f,
                    useRecessedPlywood = false,
                )
            } else {
                plywoodBackground(this@dicePanel, cornerRadiusDp = 13f)
            }
        elevation = dp(if (selected) 4 else 2).toFloat()
        contentDescription =
            "${dice.title}, ${when {
                selected -> "selected"
                owned -> "unlocked"
                !dice.contentAvailable -> "coming soon"
                else -> "locked"
            }}"

        addView(
            TextView(this@dicePanel).apply {
                text = dice.title
                textSize = 14.5f
                setTypeface(Typeface.DEFAULT_BOLD)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                includeFontPadding = false
                maxLines = 1
                setShadowLayer(2f, 0f, dp(1).toFloat(), 0xAA3B2414.toInt())
                setPadding(0, 0, 0, dp(8))
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        val preview: View =
            if (dice.id == StoreCosmeticCatalog.DEFAULT_DICE) {
                // Use the actual gameplay die renderer for Classic, frozen at a clear six face.
                DiceView(this@dicePanel).apply {
                    showOutcome(6, animate = false)
                    setPlayerTeamColor(null)
                    isClickable = false
                    isFocusable = false
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    contentDescription = "Classic gameplay dice preview"
                }
            } else {
                StoreThemedDicePreviewView(
                    context = this@dicePanel,
                    primaryColor = dice.previewPrimary,
                    secondaryColor = dice.previewSecondary,
                    faceValue = dice.previewSymbol.toIntOrNull()?.coerceIn(1, 6) ?: 5,
                ).apply {
                    isClickable = false
                    isFocusable = false
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    contentDescription = "${dice.title.lowercase()} dice preview"
                }
            }
        addView(
            preview,
            LinearLayout.LayoutParams(
                dp(DICE_PREVIEW_SIZE_DP),
                dp(DICE_PREVIEW_SIZE_DP),
            ).apply {
                topMargin = dp(1)
                bottomMargin = dp(1)
            },
        )

        addView(
            diceActionBadge(dice, owned, selected),
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(8)
            },
        )
    }

private fun StoreActivity.diceActionBadge(
    dice: StoreCosmetic,
    owned: Boolean,
    selected: Boolean,
): TextView =
    TextView(this).apply {
        text =
            when {
                !dice.contentAvailable -> "SOON"
                selected -> "✓"
                owned -> "USE"
                dice.unlockKind == CosmeticUnlockKind.GEMS -> "${dice.gemPrice} ◆"
                dice.unlockKind == CosmeticUnlockKind.REWARDED_ADS ->
                    "AD ${cosmetics.rewardedAdProgress(dice.id)}/${dice.requiredAdViews}"
                dice.unlockKind == CosmeticUnlockKind.LEVEL -> "UNLOCK"
                dice.unlockKind == CosmeticUnlockKind.EVENT -> "EVENT"
                else -> "FREE"
            }
        textSize = 9.5f
        setTypeface(Typeface.DEFAULT_BOLD)
        gravity = Gravity.CENTER
        includeFontPadding = false
        maxLines = 1
        letterSpacing = 0.015f
        setPadding(dp(9), dp(5), dp(9), dp(5))
        setTextColor(
            when {
                !dice.contentAvailable -> 0xFF65421E.toInt()
                selected -> 0xFF67430F.toInt()
                owned -> 0xFF104B29.toInt()
                else -> Color.WHITE
            },
        )
        background =
            GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                when {
                    !dice.contentAvailable ->
                        intArrayOf(0xFFE5D2B5.toInt(), 0xFFC4A47B.toInt())
                    selected ->
                        intArrayOf(0xFFFFF0AD.toInt(), 0xFFFFCB45.toInt())
                    owned ->
                        intArrayOf(0xFFBDF5CB.toInt(), 0xFF7CE69A.toInt())
                    else ->
                        intArrayOf(0xFFF05A50.toInt(), 0xFFC92E2E.toInt())
                },
            ).apply {
                cornerRadius = dp(999).toFloat()
                setStroke(
                    dp(1),
                    when {
                        !dice.contentAvailable -> 0xFF987044.toInt()
                        selected -> 0xFFFFF5C9.toInt()
                        owned -> 0xFF47B86C.toInt()
                        else -> 0xFFFF9A87.toInt()
                    },
                )
            }
        setOnClickListener {
            handleCosmeticTap(dice)
        }
    }

/**
 * Dice-themed preview for catalog items that don't yet have their own gameplay renderer.
 * The rounded body, angled gloss, outline and pip layout follow the production DiceView style.
 */
private class StoreThemedDicePreviewView(
    context: android.content.Context,
    private val primaryColor: Int,
    private val secondaryColor: Int,
    private val faceValue: Int,
) : View(context) {
    private val facePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(2.4f)
    }
    private val pipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        setShadowLayer(dp(1.2f), 0f, dp(0.8f), 0x26000000)
    }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0x33000000
        setShadowLayer(dp(3f), 0f, dp(1.5f), 0x26000000)
    }
    private val glossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0x42FFFFFF
    }

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = minOf(width, height).toFloat()
        if (size <= 0f) return

        val shadow = RectF(
            size * 0.09f,
            size * 0.11f,
            size * 0.91f,
            size * 0.92f,
        )
        canvas.drawRoundRect(shadow, size * 0.16f, size * 0.16f, shadowPaint)

        val inset = size * 0.07f
        val rect = RectF(inset, inset, size - inset, size - inset)
        val edgeColor = darken(secondaryColor, 0.72f)
        facePaint.shader =
            LinearGradient(
                rect.left,
                rect.top,
                rect.right,
                rect.bottom,
                intArrayOf(brighten(primaryColor, 1.16f), primaryColor, secondaryColor),
                null,
                Shader.TileMode.CLAMP,
            )
        canvas.drawRoundRect(rect, size * 0.17f, size * 0.17f, facePaint)
        facePaint.shader = null

        canvas.drawRoundRect(
            RectF(
                rect.left + size * 0.035f,
                rect.top + size * 0.035f,
                rect.right - size * 0.035f,
                rect.top + rect.height() * 0.43f,
            ),
            size * 0.13f,
            size * 0.13f,
            glossPaint,
        )

        edgePaint.color = edgeColor
        canvas.drawRoundRect(rect, size * 0.17f, size * 0.17f, edgePaint)
        pipPaint.color =
            if (relativeLuminance(primaryColor) > 0.52) {
                0xFF292929.toInt()
            } else {
                Color.WHITE
            }

        val left = size * 0.31f
        val center = size * 0.50f
        val right = size * 0.69f
        val top = size * 0.31f
        val middle = size * 0.50f
        val bottom = size * 0.69f
        val radius = size * 0.055f
        val pips =
            when (faceValue.coerceIn(1, 6)) {
                1 -> listOf(center to middle)
                2 -> listOf(left to top, right to bottom)
                3 -> listOf(left to top, center to middle, right to bottom)
                4 -> listOf(left to top, right to top, left to bottom, right to bottom)
                5 -> listOf(left to top, right to top, center to middle, left to bottom, right to bottom)
                else -> listOf(
                    left to top,
                    right to top,
                    left to middle,
                    right to middle,
                    left to bottom,
                    right to bottom,
                )
            }
        pips.forEach { (x, y) -> canvas.drawCircle(x, y, radius, pipPaint) }
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    private fun brighten(color: Int, factor: Float): Int =
        Color.rgb(
            (Color.red(color) * factor).toInt().coerceIn(0, 255),
            (Color.green(color) * factor).toInt().coerceIn(0, 255),
            (Color.blue(color) * factor).toInt().coerceIn(0, 255),
        )

    private fun darken(color: Int, factor: Float): Int =
        Color.rgb(
            (Color.red(color) * factor).toInt().coerceIn(0, 255),
            (Color.green(color) * factor).toInt().coerceIn(0, 255),
            (Color.blue(color) * factor).toInt().coerceIn(0, 255),
        )

    private fun relativeLuminance(color: Int): Double =
        (0.2126 * Color.red(color) + 0.7152 * Color.green(color) + 0.0722 * Color.blue(color)) / 255.0

}
