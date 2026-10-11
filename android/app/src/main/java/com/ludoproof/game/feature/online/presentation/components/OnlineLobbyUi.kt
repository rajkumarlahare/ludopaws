package com.ludoproof.game.ui.online

import android.graphics.Color
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.ludoproof.game.LudoProofTheme
import com.ludoproof.game.LudoPawsCharacterAvatarView
import com.ludoproof.game.MainActivity
import com.ludoproof.game.feature.profile.data.local.ProfileStore
import com.ludoproof.game.feature.characters.data.local.CharacterSelectionStore
import com.ludoproof.game.feature.online.beginPublicMatchmaking
import com.ludoproof.game.feature.online.cancelPublicMatchmaking
import com.ludoproof.game.feature.online.selectPublicPlayerCount

internal fun MainActivity.onlineLobbyPanel():
    LinearLayout =
    LinearLayout(this).apply {
        orientation =
            LinearLayout.VERTICAL
        gravity =
            Gravity.CENTER_HORIZONTAL

        val profile =
            ProfileStore(
                this@onlineLobbyPanel,
            )
                .snapshot()

        nameInput =
            EditText(
                this@onlineLobbyPanel,
            ).apply {
                setText(
                    profile.displayName,
                )
                visibility =
                    View.GONE
            }

        matchInput =
            EditText(
                this@onlineLobbyPanel,
            ).apply {
                visibility =
                    View.GONE
            }
        createButton =
            Button(
                this@onlineLobbyPanel,
            ).apply {
                visibility =
                    View.GONE
            }
        joinButton =
            Button(
                this@onlineLobbyPanel,
            ).apply {
                visibility =
                    View.GONE
            }

        matchmakingSetupPanel =
            LinearLayout(
                this@onlineLobbyPanel,
            ).apply {
                orientation =
                    LinearLayout.VERTICAL
                gravity =
                    Gravity.CENTER_HORIZONTAL
                setPadding(
                    dp(
                        if (
                            isCompactOnline()
                        ) {
                            18
                        } else {
                            26
                        },
                    ),
                    dp(12),
                    dp(
                        if (
                            isCompactOnline()
                        ) {
                            18
                        } else {
                            26
                        },
                    ),
                    dp(20),
                )
                clipChildren = false
                clipToPadding = false
                background =
                    GradientDrawable(
                        GradientDrawable.Orientation.TOP_BOTTOM,
                        intArrayOf(
                            0xF20A2856.toInt(),
                            0xF2081B3D.toInt(),
                        ),
                    ).apply {
                        cornerRadius = dp(24).toFloat()
                        setStroke(dp(2), 0xFF40CFFF.toInt())
                    }

                addView(
                    MatchmakingMedallionView(this@onlineLobbyPanel),
                    LinearLayout.LayoutParams(
                        dp(if (isCompactOnline()) 104 else 120),
                        dp(if (isCompactOnline()) 104 else 120),
                    ).apply {
                        bottomMargin = dp(4)
                    },
                )

                addView(
                    TextView(
                        this@onlineLobbyPanel,
                    ).apply {
                        text =
                            "SELECT PLAYERS"
                        LudoProofTheme
                            .title(
                                this,
                                if (
                                    isCompactOnline()
                                ) {
                                    21f
                                } else {
                                    24f
                                },
                                gold = true,
                            )
                        gravity =
                            Gravity.CENTER
                    },
                )

                addView(
                    TextView(
                        this@onlineLobbyPanel,
                    ).apply {
                        text =
                            "Choose match size to begin"
                        LudoProofTheme
                            .body(
                                this,
                                11f,
                                centered = true,
                                bright = true,
                            )
                        setPadding(
                            0,
                            dp(4),
                            0,
                            dp(16),
                        )
                    },
                )

                twoPlayerButton =
                    Button(
                        this@onlineLobbyPanel,
                    ).apply {
                        minWidth =
                            0
                        minHeight =
                            0
                        setPadding(
                            dp(14),
                            0,
                            dp(14),
                            0,
                        )
                        setOnClickListener {
                            selectPublicPlayerCount(
                                2,
                            )
                        }
                    }

                fourPlayerButton =
                    Button(
                        this@onlineLobbyPanel,
                    ).apply {
                        minWidth =
                            0
                        minHeight =
                            0
                        setPadding(
                            dp(14),
                            0,
                            dp(14),
                            0,
                        )
                        setOnClickListener {
                            selectPublicPlayerCount(
                                4,
                            )
                        }
                    }
                val playerCountRow =
                    LinearLayout(this@onlineLobbyPanel).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER
                        addView(
                            twoPlayerButton,
                            LinearLayout.LayoutParams(
                                0,
                                dp(if (isCompactOnline()) 154 else 174),
                                1f,
                            ).apply {
                                rightMargin = dp(8)
                            },
                        )
                        addView(
                            fourPlayerButton,
                            LinearLayout.LayoutParams(
                                0,
                                dp(if (isCompactOnline()) 154 else 174),
                                1f,
                            ).apply {
                                leftMargin = dp(8)
                            },
                        )
                    }
                addView(
                    playerCountRow,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(if (isCompactOnline()) 154 else 174),
                    ).apply {
                        topMargin = dp(16)
                    },
                )

                findMatchButton =
                    Button(
                        this@onlineLobbyPanel,
                    ).apply {
                        text = "▶   PLAY"
                        textSize =
                            if (
                                isCompactOnline()
                            ) {
                                16f
                            } else {
                                18f
                            }
                        setTypeface(Typeface.DEFAULT_BOLD)
                        setTextColor(0xFF342000.toInt())
                        background =
                            GradientDrawable(
                                GradientDrawable.Orientation.TOP_BOTTOM,
                                intArrayOf(
                                    0xFFFFD642.toInt(),
                                    0xFFFF9D00.toInt(),
                                ),
                            ).apply {
                                cornerRadius = dp(999).toFloat()
                                setStroke(dp(2), 0xFFFFF0A0.toInt())
                            }
                        setOnClickListener {
                            beginPublicMatchmaking()
                        }
                    }
                addView(
                    findMatchButton,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(54),
                    ).apply {
                        topMargin = dp(20)
                    },
                )
            }

        addView(
            matchmakingSetupPanel,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(if (isCompactOnline()) 92 else 112)
            },
        )

        matchmakingSearchPanel =
            LinearLayout(
                this@onlineLobbyPanel,
            ).apply {
                orientation =
                    LinearLayout.VERTICAL
                gravity =
                    Gravity.CENTER_HORIZONTAL
                visibility =
                    View.GONE
                setPadding(
                    dp(
                        if (
                            isCompactOnline()
                        ) {
                            14
                        } else {
                            20
                        },
                    ),
                    dp(18),
                    dp(
                        if (
                            isCompactOnline()
                        ) {
                            14
                        } else {
                            20
                        },
                    ),
                    dp(18),
                )
                background =
                    LudoProofTheme
                        .darkPanelDrawable(
                            this@onlineLobbyPanel,
                            goldBorder = true,
                        )

                val ownAvatar =
                    FrameLayout(
                        this@onlineLobbyPanel,
                    ).apply {
                        background =
                            GradientDrawable(
                                GradientDrawable.Orientation.TOP_BOTTOM,
                                intArrayOf(
                                    0xFF168EEA.toInt(),
                                    0xFF083D91.toInt(),
                                ),
                            ).apply {
                                cornerRadius =
                                    dp(12)
                                        .toFloat()
                                setStroke(
                                    dp(2),
                                    0xFF5BE0FF.toInt(),
                                )
                            }
                        addView(
                            LudoPawsCharacterAvatarView(
                                this@onlineLobbyPanel,
                            ).apply {
                                bind(
                                    characterId =
                                        CharacterSelectionStore(
                                            this@onlineLobbyPanel,
                                        ).load().characterId,
                                    ringColor = 0xFFFFD34A.toInt(),
                                    highlighted = true,
                                )
                            },
                            FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT,
                            ),
                        )
                    }
                addView(
                    ownAvatar,
                    LinearLayout.LayoutParams(
                        dp(
                            if (
                                isCompactOnline()
                            ) {
                                72
                            } else {
                                82
                            },
                        ),
                        dp(
                            if (
                                isCompactOnline()
                            ) {
                                72
                            } else {
                                82
                            },
                        ),
                    ),
                )

                addView(
                    TextView(
                        this@onlineLobbyPanel,
                    ).apply {
                        text =
                            profile.displayName
                        LudoProofTheme
                            .title(
                                this,
                                if (
                                    isCompactOnline()
                                ) {
                                    15f
                                } else {
                                    17f
                                },
                            )
                        gravity =
                            Gravity.CENTER
                        setPadding(
                            0,
                            dp(5),
                            0,
                            0,
                        )
                    },
                )

                addView(
                    TextView(
                        this@onlineLobbyPanel,
                    ).apply {
                        text =
                            "VS"
                        textSize =
                            if (
                                isCompactOnline()
                            ) {
                                25f
                            } else {
                                29f
                            }
                        setTypeface(
                            Typeface.DEFAULT_BOLD,
                        )
                        setTextColor(
                            LudoProofTheme.GOLD,
                        )
                        gravity =
                            Gravity.CENTER
                        setPadding(
                            0,
                            dp(10),
                            0,
                            dp(8),
                        )
                    },
                )

                matchmakingOpponentRail =
                    MatchmakingOpponentRailView(
                        this@onlineLobbyPanel,
                    )
                addView(
                    matchmakingOpponentRail,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(
                            if (
                                isCompactOnline()
                            ) {
                                118
                            } else {
                                132
                            },
                        ),
                    ),
                )

                matchmakingStatusText =
                    TextView(
                        this@onlineLobbyPanel,
                    ).apply {
                        text =
                            "SEARCHING FOR PLAYERS…"
                        LudoProofTheme
                            .title(
                                this,
                                if (
                                    isCompactOnline()
                                ) {
                                    15f
                                } else {
                                    17f
                                },
                                gold = true,
                            )
                        gravity =
                            Gravity.CENTER
                        setPadding(
                            dp(6),
                            dp(12),
                            dp(6),
                            dp(4),
                        )
                    }
                addView(
                    matchmakingStatusText,
                )

                matchmakingTimerText =
                    TextView(
                        this@onlineLobbyPanel,
                    ).apply {
                        text =
                            "SEARCH  00:00"
                        LudoProofTheme
                            .body(
                                this,
                                if (
                                    isCompactOnline()
                                ) {
                                    12f
                                } else {
                                    13f
                                },
                                centered = true,
                                bright = true,
                            )
                        setTextColor(
                            0xFF70E7FF.toInt(),
                        )
                    }
                addView(
                    matchmakingTimerText,
                )

                cancelMatchmakingButton =
                    Button(
                        this@onlineLobbyPanel,
                    ).apply {
                        text =
                            "CANCEL"
                        LudoProofTheme
                            .danger(
                                this,
                            )
                        setOnClickListener {
                            cancelPublicMatchmaking()
                        }
                    }
                addView(
                    cancelMatchmakingButton,
                    LinearLayout.LayoutParams(
                        dp(
                            if (
                                isCompactOnline()
                            ) {
                                132
                            } else {
                                148
                            },
                        ),
                        dp(44),
                    ).apply {
                        gravity =
                            Gravity.CENTER_HORIZONTAL
                        topMargin =
                            dp(13)
                    },
                )
            }

        addView(
            matchmakingSearchPanel,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        matchmakingSlotsText =
            TextView(
                this@onlineLobbyPanel,
            ).apply {
                visibility =
                    View.GONE
                importantForAccessibility =
                    View.IMPORTANT_FOR_ACCESSIBILITY_YES
            }
        addView(
            matchmakingSlotsText,
            LinearLayout.LayoutParams(
                1,
                1,
            ),
        )

        stylePublicPlayerCountButton(
            twoPlayerButton,
            selected = true,
        )
        stylePublicPlayerCountButton(
            fourPlayerButton,
            selected = false,
        )
    }

internal fun MainActivity.stylePublicPlayerCountButton(
    button: Button,
    selected: Boolean,
) {
    val isTwoPlayer = button === twoPlayerButton
    button.text = (if (isTwoPlayer) "2" else "4") + " PLAYERS"

    button.textSize =
        if (isCompactOnline()) 14f else 16f
    button.setTypeface(
        Typeface.DEFAULT_BOLD,
    )
    button.setTextColor(
        if (
            selected
        ) {
            0xFFFFD45E.toInt()
        } else {
            Color.WHITE
        },
    )
    button.gravity = Gravity.CENTER
    button.isAllCaps = false
    button.compoundDrawablePadding = dp(10)
    button.setCompoundDrawables(
        null,
        PlayerGroupDrawable(
            count = if (isTwoPlayer) 2 else 4,
            color = if (selected) 0xFFFFC52E.toInt() else 0xFF29B9FF.toInt(),
            selected = selected,
        ).apply {
            val iconWidth = dp(if (isCompactOnline()) 92 else 108)
            val iconHeight = dp(if (isCompactOnline()) 82 else 94)
            setBounds(0, 0, iconWidth, iconHeight)
        },
        null,
        null,
    )
    button.background =
        GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            if (
                selected
            ) {
                intArrayOf(
                    0xFF34250C.toInt(),
                    0xFF151D31.toInt(),
                )
            } else {
                intArrayOf(
                    0xFF0C3564.toInt(),
                    0xFF092344.toInt(),
                )
            },
        ).apply {
            cornerRadius = dp(20).toFloat()
            setStroke(dp(if (selected) 3 else 1), if (selected) 0xFFFFC52E.toInt() else 0xFF1689D0.toInt())
        }
}

private class PlayerGroupDrawable(
    private val count: Int,
    private val color: Int,
    private val selected: Boolean,
) : Drawable() {
    private val paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
    private val outline =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
    private val glint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
    private val shadow =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = 0x66020A18
        }
    private val radioPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
    private val radioFill =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
    private val neckPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

    override fun draw(canvas: Canvas) {
        val bounds = bounds
        val width = bounds.width().toFloat()
        val height = bounds.height().toFloat()
        if (width <= 0f || height <= 0f) return

        val figures =
            if (count == 2) {
                listOf(
                    Figure(0.31f, 0.06f, 0.90f),
                    Figure(0.69f, 0.06f, 0.90f),
                )
            } else {
                listOf(
                    Figure(0.39f, 0.12f, 0.68f),
                    Figure(0.61f, 0.12f, 0.68f),
                    Figure(0.19f, 0.20f, 0.80f),
                    Figure(0.81f, 0.20f, 0.80f),
                )
            }
        figures.forEach { figure ->
            drawPerson(
                canvas = canvas,
                x = bounds.left + width * figure.x,
                top = bounds.top + height * figure.top,
                width = width,
                height = height * (0.62f - figure.top),
                headRadius = width * (if (count == 2) 0.12f else 0.095f) * figure.scale,
                sizeScale = figure.scale,
            )
        }

        val radioRadius = width * 0.075f
        val radioX = bounds.exactCenterX()
        val radioY = bounds.top + height * 0.91f
        radioPaint.color = if (selected) 0xFFFFE177.toInt() else 0xFFE3F0FF.toInt()
        radioPaint.strokeWidth = width * 0.025f
        canvas.drawCircle(radioX, radioY, radioRadius, radioPaint)
        if (selected) {
            radioFill.color = color
            canvas.drawCircle(radioX, radioY, radioRadius * 0.62f, radioFill)
            canvas.drawCircle(radioX - radioRadius * 0.22f, radioY - radioRadius * 0.28f, radioRadius * 0.18f, glint)
        }
    }

    private data class Figure(
        val x: Float,
        val top: Float,
        val scale: Float,
    )

    private fun drawPerson(
        canvas: Canvas,
        x: Float,
        top: Float,
        width: Float,
        height: Float,
        headRadius: Float,
        sizeScale: Float,
    ) {
        val base = color
        val light = blend(base, android.graphics.Color.WHITE, 0.32f)
        val dark = blend(base, 0xFF071B3B.toInt(), 0.24f)
        val headY = top + height * 0.22f
        val headCenterX = x
        val shoulderWidth = width * (if (count == 2) 0.15f else 0.135f) * sizeScale
        val bodyTop = top + height * 0.48f
        val bodyBottom = top + height * 0.91f
        val neckWidth = headRadius * 0.62f
        val neckBottom = bodyTop + height * 0.08f
        val bodyPath =
            android.graphics.Path().apply {
                moveTo(x - shoulderWidth * 0.46f, bodyTop)
                cubicTo(
                    x - shoulderWidth * 1.02f,
                    bodyTop + height * 0.08f,
                    x - shoulderWidth * 1.28f,
                    bodyBottom,
                    x - shoulderWidth * 1.15f,
                    bodyBottom,
                )
                lineTo(x + shoulderWidth * 1.15f, bodyBottom)
                cubicTo(
                    x + shoulderWidth * 1.28f,
                    bodyBottom,
                    x + shoulderWidth * 1.02f,
                    bodyTop + height * 0.05f,
                    x + shoulderWidth * 0.46f,
                    bodyTop,
                )
                close()
            }

        canvas.drawOval(
            x - shoulderWidth * 0.86f,
            bodyBottom - height * 0.05f,
            x + shoulderWidth * 0.86f,
            bodyBottom + height * 0.10f,
            shadow,
        )
        neckPaint.shader =
            android.graphics.LinearGradient(
                x - neckWidth,
                headY,
                x + neckWidth,
                neckBottom,
                light,
                dark,
                android.graphics.Shader.TileMode.CLAMP,
            )
        canvas.drawRoundRect(
            x - neckWidth,
            headY + headRadius * 0.62f,
            x + neckWidth,
            neckBottom,
            neckWidth * 0.35f,
            neckWidth * 0.35f,
            neckPaint,
        )
        neckPaint.shader = null

        val shadowPath =
            android.graphics.Path(bodyPath).apply {
                offset(width * 0.025f, height * 0.06f)
            }
        paint.color = dark
        canvas.drawPath(shadowPath, paint)
        paint.shader =
            android.graphics.RadialGradient(
                x - shoulderWidth * 0.55f,
                bodyTop + height * 0.1f,
                shoulderWidth * 2.2f,
                intArrayOf(light, base, dark),
                floatArrayOf(0f, 0.52f, 1f),
                android.graphics.Shader.TileMode.CLAMP,
            )
        canvas.drawPath(bodyPath, paint)
        paint.shader = null
        canvas.save()
        canvas.clipPath(bodyPath)
        glint.shader =
            android.graphics.RadialGradient(
                x - shoulderWidth * 0.28f,
                bodyTop + height * 0.10f,
                shoulderWidth * 1.4f,
                intArrayOf(0x88FFFFFF.toInt(), 0x22FFFFFF, Color.TRANSPARENT),
                null,
                android.graphics.Shader.TileMode.CLAMP,
            )
        canvas.drawRect(
            x - shoulderWidth * 1.5f,
            bodyTop,
            x + shoulderWidth * 1.5f,
            bodyBottom,
            glint,
        )
        glint.shader = null
        canvas.restore()
        outline.color = blend(base, 0xFF071327.toInt(), 0.42f)
        outline.strokeWidth = width * 0.018f
        canvas.drawPath(bodyPath, outline)

        outline.color = blend(light, android.graphics.Color.WHITE, 0.35f)
        outline.strokeWidth = width * 0.028f
        canvas.drawLine(
            x - shoulderWidth * 0.55f,
            bodyTop + height * 0.05f,
            x + shoulderWidth * 0.55f,
            bodyTop + height * 0.05f,
            outline,
        )

        paint.shader =
            android.graphics.RadialGradient(
                headCenterX - headRadius * 0.38f,
                headY - headRadius * 0.42f,
                headRadius * 1.72f,
                intArrayOf(light, base, dark),
                floatArrayOf(0f, 0.50f, 1f),
                android.graphics.Shader.TileMode.CLAMP,
            )
        canvas.drawCircle(headCenterX, headY, headRadius, paint)
        paint.shader = null
        outline.color = blend(base, 0xFF071327.toInt(), 0.36f)
        outline.strokeWidth = width * 0.018f
        canvas.drawCircle(headCenterX, headY, headRadius, outline)
        glint.color = 0x99FFFFFF.toInt()
        canvas.drawOval(
            headCenterX - headRadius * 0.58f,
            headY - headRadius * 0.66f,
            headCenterX - headRadius * 0.05f,
            headY - headRadius * 0.26f,
            glint,
        )
        glint.color = 0x55FFFFFF
        canvas.drawCircle(headCenterX + headRadius * 0.38f, headY + headRadius * 0.36f, headRadius * 0.12f, glint)
    }

    private fun blend(
        first: Int,
        second: Int,
        amount: Float,
    ): Int {
        val inverse = 1f - amount
        return android.graphics.Color.rgb(
            (android.graphics.Color.red(first) * inverse + android.graphics.Color.red(second) * amount).toInt(),
            (android.graphics.Color.green(first) * inverse + android.graphics.Color.green(second) * amount).toInt(),
            (android.graphics.Color.blue(first) * inverse + android.graphics.Color.blue(second) * amount).toInt(),
        )
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT
}

private class MatchmakingMedallionView(context: android.content.Context) : View(context) {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadow =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x66020A18
        }
    private val stroke =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
    private val glint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = 0xCCFFF6D4.toInt()
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = minOf(width, height) * 0.44f

        fill.shader =
            android.graphics.RadialGradient(
                centerX,
                centerY,
                radius,
                intArrayOf(0xFF138CFF.toInt(), 0xFF062D78.toInt()),
                null,
                android.graphics.Shader.TileMode.CLAMP,
            )
        canvas.drawCircle(centerX, centerY, radius, fill)
        fill.shader = null
        stroke.color = 0xFFFFD751.toInt()
        stroke.strokeWidth = dp(4).toFloat()
        canvas.drawCircle(centerX, centerY, radius, stroke)
        stroke.color = 0xFFFFF0A0.toInt()
        stroke.strokeWidth = dp(1.5f)
        canvas.drawCircle(centerX, centerY, radius - dp(5), stroke)

        val gold = 0xFFFFC52E.toInt()
        drawPerson(canvas, centerX - radius * 0.47f, centerY + radius * 0.08f, radius * 0.34f, radius * 0.39f, radius * 0.15f, 0xFFFFD85D.toInt())
        drawPerson(canvas, centerX + radius * 0.47f, centerY + radius * 0.08f, radius * 0.34f, radius * 0.39f, radius * 0.15f, 0xFFFFD85D.toInt())
        drawPerson(canvas, centerX, centerY, radius * 0.52f, radius * 0.60f, radius * 0.22f, gold)

        stroke.color = gold
        stroke.strokeWidth = dp(7).toFloat()
        canvas.drawLine(centerX - radius * 0.93f, centerY + radius * 0.36f, centerX - radius * 0.60f, centerY + radius * 0.36f, stroke)
        canvas.drawLine(centerX + radius * 0.60f, centerY + radius * 0.36f, centerX + radius * 0.93f, centerY + radius * 0.36f, stroke)
        stroke.color = 0xFFFFF0A0.toInt()
        stroke.strokeWidth = dp(2).toFloat()
        canvas.drawLine(centerX - radius * 0.88f, centerY + radius * 0.36f, centerX - radius * 0.63f, centerY + radius * 0.36f, stroke)
        canvas.drawLine(centerX + radius * 0.63f, centerY + radius * 0.36f, centerX + radius * 0.88f, centerY + radius * 0.36f, stroke)
        canvas.drawCircle(centerX, centerY - radius * 0.84f, dp(2.5f), glint)
    }

    private fun drawPerson(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        headRadius: Float,
        bodyWidth: Float,
        bodyHeight: Float,
        color: Int,
    ) {
        val headY = centerY - bodyHeight * 0.72f
        fill.shader =
            android.graphics.RadialGradient(
                centerX - headRadius * 0.38f,
                headY - headRadius * 0.42f,
                headRadius * 1.72f,
                intArrayOf(0xFFFFF9DC.toInt(), 0xFFFFD75A.toInt(), 0xFFB86B00.toInt()),
                floatArrayOf(0f, 0.55f, 1f),
                android.graphics.Shader.TileMode.CLAMP,
            )
        canvas.drawCircle(centerX, headY, headRadius, fill)
        fill.shader = null
        val top = centerY - bodyHeight * 0.32f
        val neckWidth = headRadius * 0.62f
        fill.shader =
            android.graphics.LinearGradient(
                centerX - neckWidth,
                headY,
                centerX + neckWidth,
                top + bodyHeight * 0.12f,
                0xFFFFE994.toInt(),
                0xFFC37A05.toInt(),
                android.graphics.Shader.TileMode.CLAMP,
            )
        canvas.drawRoundRect(
            centerX - neckWidth,
            headY + headRadius * 0.60f,
            centerX + neckWidth,
            top + bodyHeight * 0.15f,
            neckWidth * 0.35f,
            neckWidth * 0.35f,
            fill,
        )
        fill.shader = null
        val body =
            android.graphics.Path().apply {
                moveTo(centerX - bodyWidth * 0.28f, top)
                cubicTo(
                    centerX - bodyWidth * 0.78f,
                    top + bodyHeight * 0.08f,
                    centerX - bodyWidth * 0.72f,
                    top + bodyHeight * 0.25f,
                    centerX - bodyWidth * 0.68f,
                    top + bodyHeight,
                )
                lineTo(centerX + bodyWidth * 0.68f, top + bodyHeight)
                cubicTo(
                    centerX + bodyWidth * 0.72f,
                    top + bodyHeight * 0.25f,
                    centerX + bodyWidth * 0.78f,
                    top + bodyHeight * 0.08f,
                    centerX + bodyWidth * 0.28f,
                    top,
                )
                close()
            }
        val bodyShadow =
            android.graphics.Path(body).apply {
                offset(dp(1.5f), dp(3f))
            }
        canvas.drawPath(bodyShadow, shadow)
        fill.shader =
            android.graphics.LinearGradient(
                centerX - bodyWidth * 0.70f,
                top + bodyHeight * 0.10f,
                centerX + bodyWidth * 0.55f,
                top + bodyHeight,
                intArrayOf(0xFFFFF4B2.toInt(), 0xFFFFC32D.toInt(), 0xFFB96800.toInt()),
                floatArrayOf(0f, 0.52f, 1f),
                android.graphics.Shader.TileMode.CLAMP,
            )
        canvas.drawPath(body, fill)
        fill.shader = null
        stroke.color = 0xFF9D5A00.toInt()
        stroke.strokeWidth = dp(2.5f)
        canvas.drawPath(body, stroke)
        stroke.color = 0xFFFFF2B6.toInt()
        stroke.strokeWidth = dp(1.5f)
        canvas.drawPath(body, stroke)
        canvas.save()
        canvas.clipPath(body)
        fill.shader =
            android.graphics.RadialGradient(
                centerX - bodyWidth * 0.25f,
                top + bodyHeight * 0.15f,
                bodyWidth * 1.25f,
                intArrayOf(0x88FFFFFF.toInt(), 0x22FFFFFF, Color.TRANSPARENT),
                null,
                android.graphics.Shader.TileMode.CLAMP,
            )
        canvas.drawPath(body, fill)
        fill.shader = null
        canvas.restore()
        canvas.drawOval(
            centerX - headRadius * 0.58f,
            headY - headRadius * 0.66f,
            centerX - headRadius * 0.05f,
            headY - headRadius * 0.26f,
            glint,
        )
        stroke.color = 0xFF9D5A00.toInt()
        stroke.strokeWidth = dp(1.5f)
        canvas.drawCircle(centerX, headY, headRadius, stroke)
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}

internal fun MainActivity.inputLabel(
    label: String,
):
    TextView =
    TextView(this).apply {
        text =
            label
        LudoProofTheme.body(
            this,
            10.5f,
            bright = true,
        )
        setTextColor(
            0xFF70E7FF.toInt(),
        )
        setPadding(
            dp(4),
            0,
            dp(4),
            dp(5),
        )
    }
