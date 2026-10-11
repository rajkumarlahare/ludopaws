package com.ludoproof.game.ui.offline.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.ludoproof.game.*
import com.ludoproof.game.core.ui.components.appIconActionButton
import com.ludoproof.game.core.ui.icons.AppIconName
import com.ludoproof.game.ui.dialogs.showSettingsDialog
import com.ludoproof.game.ui.offline.gameplay.showGame
import com.ludoproof.game.ui.offline.setup.showSetup

internal fun OfflineGameActivity.backHeader(label: String?): View {
    fun openSettings() {
        showSettingsDialog(this@backHeader) {
            val current = engine.snapshot()
            if (current != null) showGame(current) else showSetup()
        }
    }

    if (isComputerMode) {
        return FrameLayout(this).apply {
            minimumHeight = dp(54)
            clipChildren = false
            clipToPadding = false

            addView(
                appIconActionButton(
                    context = this@backHeader,
                    contentDescription = "Back",
                    iconName = AppIconName.CHEVRON_LEFT,
                    sizeDp = 44,
                ) {
                    requestOfflineExit()
                },
                FrameLayout.LayoutParams(
                    dp(44),
                    dp(44),
                    Gravity.START or Gravity.CENTER_VERTICAL,
                ).apply { marginStart = dp(4) },
            )

            val badge =
                LinearLayout(this@backHeader).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER
                    setPadding(dp(12), dp(4), dp(12), dp(4))
                    background =
                        GradientDrawable(
                            GradientDrawable.Orientation.TOP_BOTTOM,
                            intArrayOf(0xFF0C55B5.toInt(), 0xFF082963.toInt()),
                        ).apply {
                            cornerRadius = dp(999).toFloat()
                            setStroke(dp(2), LudoProofTheme.GOLD)
                        }
                    elevation = dp(6).toFloat()
                    addView(
                        SetupPawMarkView(this@backHeader),
                        LinearLayout.LayoutParams(dp(22), dp(22)),
                    )
                    addView(
                        TextView(this@backHeader).apply {
                            text = label.orEmpty()
                            LudoProofTheme.title(this, 16f)
                            setPadding(dp(8), 0, dp(8), 0)
                        },
                    )
                    addView(
                        SetupPawMarkView(this@backHeader),
                        LinearLayout.LayoutParams(dp(22), dp(22)),
                    )
                }
            addView(
                badge,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    dp(42),
                    Gravity.CENTER,
                ),
            )

            addView(
                appIconActionButton(
                    context = this@backHeader,
                    contentDescription = "Game settings",
                    iconName = AppIconName.SETTINGS,
                    sizeDp = 44,
                ) {
                    openSettings()
                },
                FrameLayout.LayoutParams(
                    dp(44),
                    dp(44),
                    Gravity.END or Gravity.CENTER_VERTICAL,
                ).apply { marginEnd = dp(4) },
            )
        }
    }

    return LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        addView(
            appIconActionButton(
                context = this@backHeader,
                contentDescription = "Back",
                iconName = AppIconName.CHEVRON_LEFT,
                sizeDp = 52,
            ) {
                requestOfflineExit()
            },
            LinearLayout.LayoutParams(dp(52), dp(52)),
        )
        if (label != null) {
            addView(
                TextView(this@backHeader).apply {
                    text = label
                    LudoProofTheme.body(this, 14f, centered = true, bright = true)
                    setPadding(dp(14), dp(8), dp(14), dp(8))
                    background =
                        LudoProofTheme.darkPanelDrawable(
                            this@backHeader,
                            goldBorder = true,
                        )
                },
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f,
                ).apply {
                    marginStart = dp(12)
                    marginEnd = dp(10)
                },
            )
        } else {
            addView(
                View(this@backHeader),
                LinearLayout.LayoutParams(
                    0,
                    1,
                    1f,
                ).apply {
                    marginStart = dp(12)
                    marginEnd = dp(10)
                },
            )
        }
        addView(
            appIconActionButton(
                context = this@backHeader,
                contentDescription = "Game settings",
                iconName = AppIconName.SETTINGS,
                sizeDp = 52,
            ) {
                openSettings()
            },
            LinearLayout.LayoutParams(dp(52), dp(52)),
        )
    }
}

internal fun OfflineGameActivity.isCompactSetup():
    Boolean =
    LudoProofTheme
        .isCompactWidth(this)

internal fun OfflineGameActivity.dp(value: Int): Int =
    LudoProofTheme.dp(this, value)

private class SetupPawMarkView(context: Context) : View(context) {
    private val pawPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF55C9FF.toInt()
            style = Paint.Style.FILL
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = minOf(width, height).toFloat()
        if (size <= 0f) return
        val left = (width - size) / 2f
        val top = (height - size) / 2f
        canvas.save()
        canvas.translate(left, top)
        canvas.scale(size / 100f, size / 100f)
        canvas.drawOval(RectF(23f, 48f, 77f, 91f), pawPaint)
        canvas.drawOval(RectF(12f, 29f, 30f, 50f), pawPaint)
        canvas.drawOval(RectF(33f, 15f, 51f, 39f), pawPaint)
        canvas.drawOval(RectF(57f, 15f, 75f, 39f), pawPaint)
        canvas.drawOval(RectF(75f, 29f, 93f, 50f), pawPaint)
        canvas.restore()
    }
}
