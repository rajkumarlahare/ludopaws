package com.ludoproof.game.ui.offline.setup

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.ludoproof.game.*
import com.ludoproof.game.feature.offline.*
import com.ludoproof.game.ui.offline.common.*
import com.ludoproof.game.ui.offline.gameplay.*

internal fun OfflineGameActivity.playersPanel():
    LinearLayout =
    selectionPanel(
        "SELECT PLAYERS",
        if (
            isComputerMode
        ) {
            "You + 1–3 computer players"
        } else {
            "2–4 players on this device"
        },
    ).apply {
        val playersRow =
            LinearLayout(
                this@playersPanel,
            ).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER
            }
        val playerMap =
            linkedMapOf<Int, Button>()

        listOf(
            2 to "♟♟",
            3 to "♟♟♟",
            4 to "♟♟♟♟",
        ).forEach {
                entry ->
            val button =
                tileButton(
                    entry.second +
                        "\n" +
                        entry.first +
                        " PLAYERS",
                ) {
                    updateSelectedPlayerCount(
                        entry.first,
                    )
                }
            playerMap[entry.first] =
                button
            playersRow.addView(
                button,
                setupTileParams(
                    if (isCompactSetup()) 98 else 106,
                ),
            )
        }

        playerButtons =
            playerMap
        addView(playersRow)
    }

internal fun OfflineGameActivity.colorPanel():
    LinearLayout =
    selectionPanel(
        "CHOOSE YOUR COLOR",
        "Your color will rotate to the bottom-left side",
    ).apply {
        val colorRow =
            LinearLayout(
                this@colorPanel,
            ).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER
            }

        val colors =
            linkedMapOf(
                "BLUE" to
                    0xFF298DFF.toInt(),
                "RED" to
                    0xFFF22E35.toInt(),
                "GREEN" to
                    0xFF1FB257.toInt(),
                "YELLOW" to
                    0xFFFFD324.toInt(),
            )

        val colorMap =
            linkedMapOf<String, View>()

        colors.forEach {
                entry ->
            val button: View =
                if (isComputerMode) {
                    LinearLayout(this@colorPanel).apply {
                        orientation = LinearLayout.VERTICAL
                        gravity = Gravity.CENTER
                        setPadding(dp(4), dp(5), dp(4), dp(5))
                        isClickable = true
                        isFocusable = true
                        elevation = dp(3).toFloat()
                        addView(
                            View(this@colorPanel).apply {
                                background =
                                    GradientDrawable(
                                        GradientDrawable.Orientation.TOP_BOTTOM,
                                        intArrayOf(
                                            blendWithWhite(entry.value, 0.38f),
                                            entry.value,
                                            blendWithBlack(entry.value, 0.18f),
                                        ),
                                    ).apply {
                                        shape = GradientDrawable.OVAL
                                        setStroke(dp(2), 0x99FFFFFF.toInt())
                                    }
                            },
                            LinearLayout.LayoutParams(dp(42), dp(42)),
                        )
                        addView(
                            TextView(this@colorPanel).apply {
                                text = entry.key
                                textSize = if (isCompactSetup()) 11f else 12f
                                setTypeface(android.graphics.Typeface.DEFAULT_BOLD)
                                setTextColor(entry.value)
                                gravity = Gravity.CENTER
                                setPadding(0, dp(3), 0, 0)
                            },
                        )
                        setOnClickListener {
                            selectedColor = entry.key
                            refreshSetupSelections()
                        }
                    }
                } else {
                    Button(this@colorPanel).apply {
                        text = "●\n${entry.key}"
                        textSize = if (isCompactSetup()) 13f else 14f
                        setTextColor(entry.value)
                        gravity = Gravity.CENTER
                        setPadding(dp(2), dp(6), dp(2), dp(6))
                        setOnClickListener {
                            selectedColor = entry.key
                            refreshSetupSelections()
                        }
                    }
                }
            colorMap[entry.key] =
                button
            colorRow.addView(
                button,
                setupTileParams(
                    if (isComputerMode) {
                        if (isCompactSetup()) 90 else 98
                    } else if (isCompactSetup()) 86 else 94,
                    marginDp =
                        if (isCompactSetup()) 3 else 5,
                ),
            )
        }

        colorButtons =
            colorMap
        addView(colorRow)
    }

internal fun OfflineGameActivity.localTrustStrip():
    LinearLayout =
    LinearLayout(this).apply {
        orientation =
            LinearLayout.HORIZONTAL
        gravity =
            Gravity.CENTER_VERTICAL
        setPadding(
            dp(14),
            dp(12),
            dp(14),
            dp(12),
        )
        background =
            LudoProofTheme
                .hudPanelDrawable(
                    this@localTrustStrip,
                    goldBorder = false,
                )
        elevation =
            dp(4).toFloat()

        addView(
            TextView(
                this@localTrustStrip,
            ).apply {
                text =
                    "✓"
                textSize =
                    25f
                setTextColor(
                    0xFF68F053.toInt(),
                )
                gravity =
                    Gravity.CENTER
            },
            LinearLayout.LayoutParams(
                dp(38),
                dp(38),
            ),
        )

        addView(
            LinearLayout(
                this@localTrustStrip,
            ).apply {
                orientation =
                    LinearLayout.VERTICAL

                addView(
                    TextView(
                        this@localTrustStrip,
                    ).apply {
                        text =
                            "LOCAL V4 RECOMPUTATION"
                        LudoProofTheme.body(
                            this,
                            12f,
                            bright = true,
                        )
                    },
                )
                addView(
                    TextView(
                        this@localTrustStrip,
                    ).apply {
                        text =
                            "Works offline • no remote server attestation"
                        LudoProofTheme.body(
                            this,
                            10.5f,
                        )
                        setPadding(
                            0,
                            dp(2),
                            0,
                            0,
                        )
                    },
                )
            },
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f,
            ).apply {
                marginStart =
                    dp(8)
            },
        )
    }

internal fun OfflineGameActivity.selectionPanel(
    title: String,
    subtitle: String,
): LinearLayout =
    LudoProofTheme.panel(this).apply {
        setPadding(
            dp(if (isCompactSetup()) 12 else 14),
            dp(if (isCompactSetup()) 12 else 14),
            dp(if (isCompactSetup()) 12 else 14),
            dp(if (isCompactSetup()) 12 else 14),
        )
        if (isComputerMode) {
            background = LudoProofTheme.darkPanelDrawable(this@selectionPanel)
        }

        addView(
            TextView(this@selectionPanel).apply {
                text =
                    if (isComputerMode) {
                        when (title) {
                            "CHOOSE YOUR PAW",
                            "SELECT PLAYERS",
                            "CHOOSE YOUR COLOR",
                            -> title
                            else -> title
                        }
                    } else {
                        title
                    }
                LudoProofTheme.title(
                    this,
                    if (isCompactSetup()) 17f else 19f,
                )
                if (isComputerMode) {
                    gravity = Gravity.CENTER
                    val highlightedWord =
                        when (title) {
                            "CHOOSE YOUR PAW" -> "PAW"
                            "SELECT PLAYERS" -> "PLAYERS"
                            "CHOOSE YOUR COLOR" -> "COLOR"
                            else -> null
                        }
                    val wordStart =
                        highlightedWord?.let(title::lastIndexOf) ?: -1
                    if (wordStart >= 0 && highlightedWord != null) {
                        text =
                            SpannableString(title).apply {
                                setSpan(
                                    ForegroundColorSpan(LudoProofTheme.GOLD),
                                    wordStart,
                                    wordStart + highlightedWord.length,
                                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                                )
                            }
                    }
                }
            },
        )

        addView(
            TextView(this@selectionPanel).apply {
                text =
                    subtitle
                LudoProofTheme.body(
                    this,
                    11f,
                    centered = true,
                )
                if (isComputerMode) setTextColor(0xFFC5DEFF.toInt())
                setPadding(
                    0,
                    dp(2),
                    0,
                    dp(11),
                )
            },
        )
    }

internal fun OfflineGameActivity.choiceTile(
    icon: String,
    title: String,
    subtitle: String,
    selected: Boolean,
    enabled: Boolean,
    action: () -> Unit,
): LinearLayout =
    LinearLayout(this).apply {
        orientation =
            LinearLayout.VERTICAL
        gravity =
            Gravity.CENTER
        isEnabled =
            enabled
        alpha =
            if (enabled) 1f else .52f

        if (selected) {
            LudoProofTheme.selectedTile(this)
        } else {
            LudoProofTheme.normalTile(this)
        }

        setPadding(
            dp(10),
            dp(8),
            dp(10),
            dp(8),
        )

        addView(
            TextView(this@choiceTile).apply {
                text =
                    icon
                textSize =
                    27f
                gravity =
                    Gravity.CENTER
                setTextColor(
                    if (selected) {
                        0xFF053177.toInt()
                    } else {
                        Color.WHITE
                    },
                )
            },
        )
        addView(
            TextView(this@choiceTile).apply {
                text =
                    title
                LudoProofTheme.body(
                    this,
                    16f,
                    centered = true,
                    bright = true,
                )
                if (selected) {
                    setTextColor(
                        0xFF052A68.toInt(),
                    )
                }
            },
        )
        addView(
            TextView(this@choiceTile).apply {
                text =
                    subtitle
                LudoProofTheme.body(
                    this,
                    10.5f,
                    centered = true,
                    bright = true,
                )
                if (selected) {
                    setTextColor(
                        0xFF174A78.toInt(),
                    )
                }
            },
        )
        setOnClickListener {
            if (enabled) {
                action()
            }
        }
    }

internal fun OfflineGameActivity.tileButton(
    text: String,
    action: () -> Unit,
): Button =
    Button(this).apply {
        this.text =
            text
        textSize =
            if (isCompactSetup()) 12.5f else 14f
        setTextColor(
            Color.WHITE,
        )
        gravity =
            Gravity.CENTER
        setPadding(
            dp(4),
            dp(8),
            dp(4),
            dp(8),
        )
        setOnClickListener {
            action()
        }
    }

private fun blendWithWhite(color: Int, amount: Float): Int =
    Color.rgb(
        (Color.red(color) + (255 - Color.red(color)) * amount).toInt(),
        (Color.green(color) + (255 - Color.green(color)) * amount).toInt(),
        (Color.blue(color) + (255 - Color.blue(color)) * amount).toInt(),
    )

private fun blendWithBlack(color: Int, amount: Float): Int =
    Color.rgb(
        (Color.red(color) * (1f - amount)).toInt(),
        (Color.green(color) * (1f - amount)).toInt(),
        (Color.blue(color) * (1f - amount)).toInt(),
    )

private fun setupColor(name: String): Int =
    when (name) {
        "BLUE" -> 0xFF298DFF.toInt()
        "RED" -> 0xFFF22E35.toInt()
        "GREEN" -> 0xFF1FB257.toInt()
        else -> 0xFFFFD324.toInt()
    }

internal fun OfflineGameActivity.refreshSetupSelections() {
    if (playerButtons.isNotEmpty()) {
        playerButtons.forEach { entry ->
            if (entry.key == selectedPlayers) {
                LudoProofTheme.selectedTile(entry.value)
            } else {
                LudoProofTheme.normalTile(entry.value)
            }
        }
    }
    if (colorButtons.isNotEmpty()) {
        colorButtons.forEach { entry ->
            val selected = entry.key == selectedColor
            if (selected) {
                LudoProofTheme.selectedTile(entry.value)
            } else {
                LudoProofTheme.normalTile(entry.value)
            }
            ((entry.value as? LinearLayout)?.getChildAt(1) as? TextView)
                ?.setTextColor(
                    if (selected) 0xFF052A68.toInt() else setupColor(entry.key),
                )
        }
    }

    refreshCharacterSelectionUi()
}

internal fun OfflineGameActivity.setupSectionParams(
    topMarginDp: Int,
) =
    LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT,
    ).apply {
        topMargin =
            dp(topMarginDp)
    }

internal fun OfflineGameActivity.setupTileParams(
    heightDp: Int,
    marginDp: Int = 6,
) =
    LinearLayout.LayoutParams(
        0,
        dp(heightDp),
        1f,
    ).apply {
        setMargins(
            dp(marginDp),
            0,
            dp(marginDp),
            0,
        )
    }
