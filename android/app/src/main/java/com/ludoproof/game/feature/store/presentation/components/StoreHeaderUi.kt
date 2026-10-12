package com.ludoproof.game.feature.store.presentation.components

import android.graphics.Color
import android.graphics.Typeface
import com.ludoproof.game.core.ui.art.plywoodBackground
import android.view.Gravity
import com.ludoproof.game.core.ui.components.appIconActionButton
import com.ludoproof.game.core.ui.icons.AppIconName
import android.widget.LinearLayout
import android.widget.TextView
import com.ludoproof.game.LudoProofTheme
import com.ludoproof.game.core.ui.art.recessedPlywoodBackground
import com.ludoproof.game.core.ui.components.AppFocusRing
import com.ludoproof.game.feature.store.presentation.StoreActivity
import com.ludoproof.game.feature.store.presentation.art.GemClusterView
import com.ludoproof.game.feature.store.presentation.art.StoreAwningView

internal fun StoreActivity.storeHeader():
    LinearLayout =
    LinearLayout(this).apply {
        orientation =
            LinearLayout.HORIZONTAL
        gravity =
            Gravity.CENTER_VERTICAL

        addView(
            appIconActionButton(
                context = this@storeHeader,
                contentDescription = "Back",
                iconName = AppIconName.CHEVRON_LEFT,
                sizeDp = 50,
            ) { finish() }.apply {
                // Match the shared Profile back button: circular plywood with a white chevron.
                background = plywoodBackground(this@storeHeader, cornerRadiusDp = 25f)
            },
            LinearLayout.LayoutParams(
                dp(50),
                dp(50),
            ),
        )

        addView(
            TextView(
                this@storeHeader,
            ),
            LinearLayout.LayoutParams(
                0,
                1,
                1f,
            ),
        )

        val gemPill =
            LinearLayout(
                this@storeHeader,
            ).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER_VERTICAL
                background =
                    LudoProofTheme
                        .rounded(
                            0xF307173C.toInt(),
                            999f,
                            0x44000000,
                            1f,
                            this@storeHeader,
                        )
            }

        gemPill.addView(
            GemClusterView(
                this@storeHeader,
            ).apply {
                background = recessedPlywoodBackground(this@storeHeader, outerCornerRadiusDp = 12f)
                contentDescription =
                    "Gems"
            },
            LinearLayout.LayoutParams(
                dp(42),
                LinearLayout.LayoutParams.MATCH_PARENT,
            ),
        )

        gemPill.addView(
            TextView(
                this@storeHeader,
            ).apply {
                text =
                    wallet
                        .balance()
                        .toString()
                textSize =
                    19f
                setTypeface(
                    Typeface.DEFAULT_BOLD,
                )
                setTextColor(
                    Color.WHITE,
                )
                gravity =
                    Gravity.CENTER
            },
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f,
            ),
        )

        gemPill.addView(
            TextView(
                this@storeHeader,
            ).apply {
                text =
                    "+"
                textSize =
                    27f
                setTypeface(
                    Typeface.DEFAULT_BOLD,
                )
                setTextColor(
                    0xFF7B5510.toInt(),
                )
                gravity =
                    Gravity.CENTER
                background =
                    LudoProofTheme
                        .rounded(
                            0xFFFFCA28.toInt(),
                            9f,
                            0xFFFFE37C.toInt(),
                            1f,
                            this@storeHeader,
                        )
            },
            LinearLayout.LayoutParams(
                dp(42),
                LinearLayout.LayoutParams.MATCH_PARENT,
            ),
        )

        addView(
            gemPill,
            LinearLayout.LayoutParams(
                dp(132),
                dp(48),
            ),
        )
    }

internal fun StoreActivity.storefrontAwning():
    StoreAwningView =
    StoreAwningView(this)
