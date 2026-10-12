package com.ludoproof.game.feature.store.presentation.components

import android.graphics.Color
import android.graphics.Typeface
import com.ludoproof.game.core.ui.art.plywoodBackground
import android.view.Gravity
import com.ludoproof.game.core.ui.components.appIconActionButton
import com.ludoproof.game.core.ui.icons.AppIconName
import com.ludoproof.game.core.ui.icons.AppIconView
import android.widget.LinearLayout
import android.widget.TextView
import com.ludoproof.game.LudoProofTheme
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

        val compactCurrencyPill =
            LudoProofTheme.isCompactWidth(this@storeHeader)
        val currencyIconSizeDp =
            if (compactCurrencyPill) 26 else 29
        val currencyPillWidthDp =
            if (compactCurrencyPill) 96 else 107
        val currencyPillHeightDp =
            if (compactCurrencyPill) 35 else 38

        val gemPill =
            LinearLayout(
                this@storeHeader,
            ).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER_VERTICAL
                background =
                    plywoodBackground(
                        context = this@storeHeader,
                        cornerRadiusDp =
                            if (compactCurrencyPill) 17.5f else 19f,
                    )
                elevation = dp(5).toFloat()
                setPadding(
                    dp(4),
                    dp(3),
                    dp(3),
                    dp(3),
                )
            }

        gemPill.addView(
            GemClusterView(
                this@storeHeader,
            ).apply {
                // Let the gems sit directly on the same plywood pill used on Home.
                contentDescription =
                    "Gems"
            },
            LinearLayout.LayoutParams(
                dp(currencyIconSizeDp),
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
                    if (compactCurrencyPill) 14f else 15f
                setTypeface(
                    Typeface.DEFAULT_BOLD,
                )
                setTextColor(
                    Color.WHITE,
                )
                gravity =
                    Gravity.CENTER
                setShadowLayer(
                    2.4f,
                    0f,
                    dp(1).toFloat(),
                    0xB0000000.toInt(),
                )
            },
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f,
            ),
        )

        gemPill.addView(
            AppIconView(
                context = this@storeHeader,
                iconName = AppIconName.ADD,
                iconTint = Color.WHITE,
                strokeWidthDp = 2.8f,
            ).apply {
                background =
                    plywoodBackground(
                        context = this@storeHeader,
                        cornerRadiusDp =
                            if (compactCurrencyPill) 13f else 14.5f,
                    )
            },
            LinearLayout.LayoutParams(
                dp(currencyIconSizeDp),
                dp(currencyIconSizeDp),
            ),
        )

        addView(
            gemPill,
            LinearLayout.LayoutParams(
                dp(currencyPillWidthDp),
                dp(currencyPillHeightDp),
            ),
        )
    }

internal fun StoreActivity.storefrontAwning():
    StoreAwningView =
    StoreAwningView(this)
