package com.ludoproof.game.ui.online

import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.ludoproof.game.LudoProofTheme
import com.ludoproof.game.ModeArtView
import com.ludoproof.game.core.ui.art.plywoodBackground
import com.ludoproof.game.core.ui.art.playGamesSignInBackground
import com.ludoproof.game.core.ui.art.recessedPlywoodBackground
import com.ludoproof.game.core.ui.components.AppFocusRing
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
                    plywoodBackground(
                        context = this@onlineLobbyPanel,
                        cornerRadiusDp = 24f,
                    )

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
                                gold = false,
                            )
                        setTextColor(Color.WHITE)
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
                        AppFocusRing.apply(this, cornerRadiusDp = 20f, strokeWidthDp = 3f)
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
                        AppFocusRing.apply(this, cornerRadiusDp = 20f, strokeWidthDp = 3f)
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
                    LinearLayout(
                        this@onlineLobbyPanel,
                    ).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER
                        isClickable = true
                        isFocusable = true
                        contentDescription = "Play"
                        setPadding(dp(16), 0, dp(16), 0)
                        // Reuse the exact Google Play sign-in fill and stroke from Profile.
                        background =
                            playGamesSignInBackground(
                                this@onlineLobbyPanel,
                            )

                        addView(
                            TextView(this@onlineLobbyPanel).apply {
                                text = "PLAY"
                                textSize =
                                    if (isCompactOnline()) 16f else 18f
                                setTypeface(Typeface.DEFAULT_BOLD)
                                setTextColor(Color.WHITE)
                                gravity = Gravity.CENTER
                                includeFontPadding = false
                                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                            },
                            LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                            ),
                        )
                        addView(
                            ImageView(this@onlineLobbyPanel).apply {
                                setImageDrawable(
                                    context.getDrawable(com.ludoproof.game.R.drawable.ic_play_triangle_white)?.mutate(),
                                )
                                scaleType = ImageView.ScaleType.FIT_CENTER
                                contentDescription = null
                                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                            },
                            LinearLayout.LayoutParams(dp(18), dp(18)).apply {
                                marginStart = dp(6)
                            },
                        )

                        AppFocusRing.apply(this, cornerRadiusDp = 13f, strokeWidthDp = 3f)
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
                // The complete searching board keeps the same regular plywood skin as setup.
                background =
                    plywoodBackground(
                        context = this@onlineLobbyPanel,
                        cornerRadiusDp = 24f,
                    )

                val ownAvatar =
                    FrameLayout(
                        this@onlineLobbyPanel,
                    ).apply {
                        // Reuse the carved plywood pocket instead of the old blue avatar plate.
                        background =
                            recessedPlywoodBackground(
                                context = this@onlineLobbyPanel,
                                outerCornerRadiusDp = 14f,
                            )
                        setPadding(dp(3), dp(3), dp(3), dp(3))
                        addView(
                            LudoPawsCharacterAvatarView(
                                this@onlineLobbyPanel,
                            ).apply {
                                bind(
                                    characterId =
                                        CharacterSelectionStore(
                                            this@onlineLobbyPanel,
                                        ).load().characterId,
                                    showCircularChrome = false,
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
                        setTextColor(Color.WHITE)
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

                // Only the inner opponent/searching lane uses the recessed plywood pocket.
                val matchmakingOpponentArea =
                    FrameLayout(this@onlineLobbyPanel).apply {
                        background =
                            recessedPlywoodBackground(
                                context = this@onlineLobbyPanel,
                                outerCornerRadiusDp = 16f,
                            )
                        setPadding(dp(7), dp(6), dp(7), dp(6))
                        clipToPadding = true
                        clipChildren = true
                    }
                matchmakingOpponentRail =
                    MatchmakingOpponentRailView(
                        this@onlineLobbyPanel,
                    )
                matchmakingOpponentArea.addView(
                    matchmakingOpponentRail,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                    ),
                )
                addView(
                    matchmakingOpponentArea,
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
                                gold = false,
                            )
                        setTextColor(Color.WHITE)
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
                        contentDescription = "Cancel online matchmaking"
                        AppFocusRing.apply(this, cornerRadiusDp = 12f)
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
            ).apply {
                // Keep the searching box aligned with the setup card's vertical position.
                topMargin = dp(if (isCompactOnline()) 92 else 112)
            },
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
    val playerNumber = if (isTwoPlayer) "2" else "4"
    val playerLabel = "$playerNumber PLAYERS"
    button.text =
        SpannableString("$playerNumber\nPLAYERS").apply {
            setSpan(
                RelativeSizeSpan(1.65f),
                0,
                playerNumber.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }
    button.isSelected = selected
    button.contentDescription = if (selected) "$playerLabel, selected" else playerLabel

    // Keep the label compact below the large number so icon and caption fit together.
    button.textSize =
        if (isCompactOnline()) 11.5f else 13f
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
    button.compoundDrawablePadding = dp(if (isCompactOnline()) 6 else 7)
    button.setCompoundDrawables(
        null,
        HomeModePlayerCountDrawable(
            context = button.context,
            count = if (isTwoPlayer) 2 else 4,
            selected = selected,
        ).apply {
            val iconWidth = dp(if (isCompactOnline()) 92 else 108)
            val iconHeight = dp(if (isCompactOnline()) 82 else 94)
            setBounds(0, 0, iconWidth, iconHeight)
        },
        null,
        null,
    )
    // Keep the recessed plywood pocket for both states. The selected card gets
    // a translucent gold wash, a fine inset highlight, and a compact check badge.
    val baseBackground =
        recessedPlywoodBackground(
            context = button.context,
            outerCornerRadiusDp = 20f,
        )
    button.background =
        if (selected) {
            android.graphics.drawable.LayerDrawable(
                arrayOf(
                    baseBackground,
                    PlayerCountSelectionOverlayDrawable(button.context),
                ),
            )
        } else {
            baseBackground
        }
}

/**
 * Visible-only overlay for the active 2-player/4-player card.
 *
 * It is painted above the plywood background but below the card's art and text,
 * so the player colours remain unchanged and legible.
 */
private class PlayerCountSelectionOverlayDrawable(
    context: android.content.Context,
) : Drawable() {
    private val density = context.resources.displayMetrics.density
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
    private var drawableAlpha = 255

    override fun draw(canvas: Canvas) {
        val boundsRect = android.graphics.RectF(bounds)
        if (boundsRect.width() <= 0f || boundsRect.height() <= 0f) return

        // Soft gold tint within the carved pocket; the timber rim stays visible.
        val inset = dp(7.5f)
        val overlayRect = android.graphics.RectF(boundsRect).apply {
            inset(inset, inset)
        }
        val radius = dp(12f).coerceAtMost(
            minOf(overlayRect.width(), overlayRect.height()) / 2f,
        )
        fillPaint.style = Paint.Style.FILL
        fillPaint.color = 0xFFFFD45E.toInt()
        fillPaint.alpha = 46 * drawableAlpha / 255
        canvas.drawRoundRect(overlayRect, radius, radius, fillPaint)

        // Thin inset edge gives selection a clear boundary without an outside stroke.
        val strokeRect = android.graphics.RectF(overlayRect).apply {
            inset(dp(0.8f), dp(0.8f))
        }
        strokePaint.color = 0xFFFFD45E.toInt()
        strokePaint.alpha = 220 * drawableAlpha / 255
        strokePaint.strokeWidth = dp(1.35f)
        val strokeRadius = (radius - dp(0.8f)).coerceAtLeast(1f)
        canvas.drawRoundRect(strokeRect, strokeRadius, strokeRadius, strokePaint)

        // Small check badge sits in the lower-right wood area, clear of the player art.
        val badgeRadius = dp(7.2f)
        val badgeX = boundsRect.right - dp(18f)
        val badgeY = boundsRect.bottom - dp(19f)
        fillPaint.alpha = drawableAlpha
        canvas.drawCircle(badgeX, badgeY, badgeRadius, fillPaint)

        strokePaint.color = 0xFF63451B.toInt()
        strokePaint.alpha = drawableAlpha
        strokePaint.strokeWidth = dp(1.8f)
        val check = android.graphics.Path().apply {
            moveTo(badgeX - badgeRadius * 0.43f, badgeY + badgeRadius * 0.02f)
            lineTo(badgeX - badgeRadius * 0.10f, badgeY + badgeRadius * 0.34f)
            lineTo(badgeX + badgeRadius * 0.44f, badgeY - badgeRadius * 0.36f)
        }
        canvas.drawPath(check, strokePaint)
    }

    override fun setAlpha(alpha: Int) {
        drawableAlpha = alpha.coerceIn(0, 255)
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {
        fillPaint.colorFilter = colorFilter
        strokePaint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT

    private fun dp(value: Float): Float = value * density
}

/**
 * Reuses the exact character illustration renderer from Home mode tiles for the
 * 2-player and 4-player choices instead of maintaining a second, divergent drawing.
 */
private class HomeModePlayerCountDrawable(
    context: android.content.Context,
    count: Int,
    selected: Boolean,
) : Drawable() {
    private val art =
        ModeArtView(context).apply {
            playerCount = count
            playerCountSelected = selected
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }

    override fun onBoundsChange(bounds: android.graphics.Rect) {
        val width = bounds.width().coerceAtLeast(1)
        val height = bounds.height().coerceAtLeast(1)
        art.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
        )
        art.layout(0, 0, width, height)
    }

    override fun draw(canvas: Canvas) {
        val b = bounds
        if (b.width() <= 0 || b.height() <= 0) return
        canvas.save()
        canvas.translate(b.left.toFloat(), b.top.toFloat())
        art.draw(canvas)
        canvas.restore()
    }

    override fun setAlpha(alpha: Int) {
        art.alpha = alpha.coerceIn(0, 255) / 255f
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {
        // The Home artwork defines its own palette; don't tint it here.
        invalidateSelf()
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
