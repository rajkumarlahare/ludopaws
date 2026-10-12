package com.ludoproof.game.feature.store.presentation.components

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.ludoproof.game.core.ui.art.playerCountSelectionBackground
import com.ludoproof.game.core.ui.art.plywoodBackground
import com.ludoproof.game.core.ui.components.AppFocusRing
import com.ludoproof.game.core.ui.components.playSelectionPopAnimation
import com.ludoproof.game.feature.store.domain.model.StoreCosmetic
import com.ludoproof.game.feature.store.presentation.StoreActivity
import com.ludoproof.game.feature.store.presentation.actions.handleCosmeticTap

private const val LOCKED_BOARD_TITLE = "LOCKED BOARD"

internal fun StoreActivity.boardCatalogContent(): LinearLayout =
    LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL

        val catalogBoards = StoreCosmeticCatalog.boards
        val classicBoard =
            catalogBoards.first {
                it.id == StoreCosmeticCatalog.DEFAULT_BOARD
            }
        // Keep Classic first; the other eight cards are placeholders until their artwork and names
        // are decided. Their only visible title/status is LOCKED BOARD.
        val panels =
            listOf(classicBoard) +
                catalogBoards.filterNot {
                    it.id == StoreCosmeticCatalog.DEFAULT_BOARD
                }

        panels.chunked(3).forEachIndexed { rowIndex, rowBoards ->
            val row =
                LinearLayout(this@boardCatalogContent).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.TOP
                }

            rowBoards.forEach { board ->
                row.addView(
                    boardPanel(board),
                    LinearLayout.LayoutParams(
                        0,
                        dp(132),
                        1f,
                    ).apply {
                        setMargins(dp(4), 0, dp(4), 0)
                    },
                )
            }

            repeat(3 - rowBoards.size) {
                row.addView(
                    TextView(this@boardCatalogContent),
                    LinearLayout.LayoutParams(0, dp(132), 1f),
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

private fun StoreActivity.boardPanel(
    board: StoreCosmetic,
): LinearLayout =
    LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        setPadding(dp(6), dp(8), dp(6), dp(8))

        val locked = board.id != StoreCosmeticCatalog.DEFAULT_BOARD
        val selected =
            !locked &&
                cosmetics.selectedId(board.category) == board.id

        // Only the timber panel is used. No blue/purple arcade face or coloured icon well.
        background =
            if (locked) {
                plywoodBackground(this@boardPanel, cornerRadiusDp = 13f)
            } else {
                playerCountSelectionBackground(
                    context = this@boardPanel,
                    selected = selected,
                    outerCornerRadiusDp = 13f,
                    checkBadgeAtTopEnd = true,
                    emphasizeSelected = true,
                    showCheckBadge = selected,
                    checkMarkScale = 1.7f,
                    useRecessedPlywood = false,
                )
            }
        elevation = dp(if (selected) 4 else 2).toFloat()
        isClickable = !locked
        isFocusable = !locked
        contentDescription =
            if (locked) {
                "$LOCKED_BOARD_TITLE, locked, coming soon"
            } else {
                "Classic board, unlocked" + if (selected) ", selected" else ""
            }

        addView(
            TextView(this@boardPanel).apply {
                text = if (locked) LOCKED_BOARD_TITLE else board.title
                textSize = 10.5f
                setTypeface(Typeface.DEFAULT_BOLD)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                includeFontPadding = false
                maxLines = 1
                setShadowLayer(2f, 0f, dp(1).toFloat(), 0xAA3B2414.toInt())
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        // Leave this area unpainted so the board icon can be added later without another BG.
        addView(
            View(this@boardPanel).apply {
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(42),
            ),
        )

        addView(
            boardStatusBadge(locked = locked, selected = selected),
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        if (locked) {
            addView(
                TextView(this@boardPanel).apply {
                    text = "COMING SOON"
                    textSize = 7.5f
                    setTypeface(Typeface.DEFAULT_BOLD)
                    setTextColor(0xFF65421E.toInt())
                    gravity = Gravity.CENTER
                    includeFontPadding = false
                    maxLines = 1
                    letterSpacing = 0.02f
                    setPadding(0, dp(3), 0, 0)
                },
            )
        } else {
            setOnClickListener { clicked ->
                clicked.playSelectionPopAnimation {
                    handleCosmeticTap(board)
                }
            }
            AppFocusRing.apply(this, cornerRadiusDp = 13f)
        }
    }

private fun StoreActivity.boardStatusBadge(
    locked: Boolean,
    selected: Boolean,
): TextView =
    TextView(this).apply {
        text = if (locked) "🔒 LOCKED" else "🔓 UNLOCKED"
        textSize = 8.5f
        setTypeface(Typeface.DEFAULT_BOLD)
        setTextColor(
            when {
                locked -> 0xFF65421E.toInt()
                selected -> 0xFF67430F.toInt()
                else -> 0xFF104B29.toInt()
            },
        )
        gravity = Gravity.CENTER
        includeFontPadding = false
        maxLines = 1
        letterSpacing = 0.015f
        setPadding(dp(7), dp(4), dp(7), dp(4))
        background =
            GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                when {
                    locked ->
                        intArrayOf(0xFFE5D2B5.toInt(), 0xFFC4A47B.toInt())
                    selected ->
                        intArrayOf(0xFFFFF0AD.toInt(), 0xFFFFCB45.toInt())
                    else ->
                        intArrayOf(0xFFBDF5CB.toInt(), 0xFF7CE69A.toInt())
                },
            ).apply {
                cornerRadius = dp(999).toFloat()
                setStroke(
                    dp(1),
                    when {
                        locked -> 0xFF987044.toInt()
                        selected -> 0xFFFFF5C9.toInt()
                        else -> 0xFF47B86C.toInt()
                    },
                )
            }
    }
