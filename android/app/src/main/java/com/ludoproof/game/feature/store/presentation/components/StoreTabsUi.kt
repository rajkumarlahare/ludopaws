package com.ludoproof.game.feature.store.presentation.components

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import com.ludoproof.game.core.ui.art.plywoodBackground
import com.ludoproof.game.core.ui.components.AppFocusRing
import com.ludoproof.game.feature.store.domain.model.StoreTab
import com.ludoproof.game.feature.store.presentation.StoreActivity

internal fun StoreActivity.storeTabs():
    LinearLayout =
    LinearLayout(this).apply {
        orientation =
            LinearLayout.HORIZONTAL
        gravity =
            Gravity.CENTER_VERTICAL

        StoreTab
            .entries
            .forEach {
                    tab ->
                val selected =
                    tab ==
                        selectedTab
                addView(
                    TextView(
                        this@storeTabs,
                    ).apply {
                        text =
                            tab.label
                        textSize =
                            13f
                        setTypeface(
                            Typeface.DEFAULT_BOLD,
                        )
                        setTextColor(
                            if (selected) {
                                Color.WHITE
                            } else {
                                0xFF5B381F.toInt()
                            },
                        )
                        setShadowLayer(
                            2f,
                            0f,
                            dp(1).toFloat(),
                            0x99000000.toInt(),
                        )
                        gravity =
                            Gravity.CENTER
                        isClickable =
                            true
                        isFocusable =
                            true
                        isSelected = selected
                        contentDescription =
                            if (selected) "${tab.label}, selected" else tab.label
                        val woodSurface =
                            plywoodBackground(
                                context = this@storeTabs,
                                cornerRadiusDp = 10f,
                            )
                        background =
                            if (selected) {
                                // Keep the plywood visible and add an inset gold gradient
                                // so the active category stays obvious without a flat colour tile.
                                LayerDrawable(
                                    arrayOf(
                                        woodSurface,
                                        GradientDrawable(
                                            GradientDrawable.Orientation.TOP_BOTTOM,
                                            intArrayOf(
                                                0x66FFE8A3.toInt(),
                                                0x44C98722.toInt(),
                                            ),
                                        ).apply {
                                            cornerRadius = dp(5).toFloat()
                                            setStroke(
                                                dp(1),
                                                0xFFFFD45E.toInt(),
                                            )
                                        },
                                    ),
                                ).apply {
                                    val inset = dp(6)
                                    setLayerInset(1, inset, inset, inset, inset)
                                }
                            } else {
                                woodSurface
                            }
                        setOnClickListener {
                            selectTab(
                                tab,
                            )
                        }
                        AppFocusRing.apply(this, cornerRadiusDp = 10f)
                    },
                    LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        1f,
                    ).apply {
                        setMargins(
                            dp(2),
                            0,
                            dp(2),
                            0,
                        )
                    },
                )
            }
    }
