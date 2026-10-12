package com.ludoproof.game.feature.store.presentation.components

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.ludoproof.game.LudoProofTheme
import com.ludoproof.game.core.ui.art.plywoodBackground
import com.ludoproof.game.core.ui.art.recessedPlywoodBackground
import com.ludoproof.game.core.ui.components.AppFocusRing
import com.ludoproof.game.feature.store.domain.StoreCatalog
import com.ludoproof.game.feature.store.domain.model.StoreProduct
import com.ludoproof.game.feature.store.domain.model.StoreTab
import com.ludoproof.game.feature.store.presentation.StoreActivity
import com.ludoproof.game.feature.store.presentation.art.GemClusterView

internal fun StoreActivity.storeContent():
    LinearLayout =
    when (selectedTab) {
        StoreTab.GEMS ->
            gemsContent()

        StoreTab.PAWS ->
            pawsCatalogContent()

        StoreTab.BOARD ->
            boardCatalogContent()

        StoreTab.DICE ->
            diceCatalogContent()

    }

private fun StoreActivity.gemsContent():
    LinearLayout =
    LinearLayout(this).apply {
        orientation =
            LinearLayout.VERTICAL

        val adActionRow =
            LinearLayout(this@gemsContent).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END
                addView(
                    rewardedAdCard(),
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ),
                )
            }
        addView(
            adActionRow,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                bottomMargin = dp(8)
            },
        )

        val removeAds =
            requireNotNull(
                StoreCatalog
                    .find(
                        StoreCatalog
                            .REMOVE_ADS,
                    ),
            )
        val packs =
            StoreCatalog
                .products
                .filter {
                    !it.featured
                }

        packs.chunked(
            2,
        ).forEach {
                rowProducts ->
            val row =
                LinearLayout(
                    this@gemsContent,
                ).apply {
                    orientation =
                        LinearLayout.HORIZONTAL
                    gravity =
                        Gravity.TOP
                }

            rowProducts.forEach {
                    product ->
                row.addView(
                    gemProductCard(
                        product,
                    ),
                    LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f,
                    ).apply {
                        setMargins(
                            dp(5),
                            0,
                            dp(5),
                            0,
                        )
                    },
                )
            }

            repeat(
                2 -
                    rowProducts.size,
            ) {
                row.addView(
                    TextView(
                        this@gemsContent,
                    ),
                    LinearLayout.LayoutParams(
                        0,
                        1,
                        1f,
                    ),
                )
            }

            addView(
                row,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply {
                    topMargin =
                        dp(10)
                },
            )
        }

        addView(
            featuredRemoveAdsCard(
                removeAds,
            ),
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(18)
            },
        )

        if (adsEnabledForBuild) {
            addView(
                TextView(this@gemsContent).apply {
                    text = "Ad privacy options"
                    textSize = 12f
                    setTextColor(0xFFB8D9FF.toInt())
                    gravity = Gravity.CENTER
                    setPadding(dp(8), dp(10), dp(8), dp(4))
                    isFocusable = true
                    setOnClickListener {
                        showAdPrivacyOptions()
                    }
                    AppFocusRing.apply(this, cornerRadiusDp = 8f)
                },
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ),
            )
        }

        addView(
            storeSafetyNote(),
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin =
                    dp(16)
            },
        )
    }

private fun StoreActivity.featuredRemoveAdsCard(
    product: StoreProduct,
):
    LinearLayout =
    LinearLayout(this).apply {
        orientation =
            LinearLayout.VERTICAL
        background =
            storePlywoodCardBackground(
                cornerRadiusDp = 15f,
                fillColors = intArrayOf(0xD94A156F.toInt(), 0xD92A0C54.toInt()),
                strokeColor = 0xFF8939A8.toInt(),
                strokeWidthDp = 2,
                insetDp = 5,
            )

        val body =
            LinearLayout(
                this@featuredRemoveAdsCard,
            ).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER_VERTICAL
                setPadding(
                    dp(16),
                    dp(12),
                    dp(16),
                    dp(12),
                )
            }

        body.addView(
            TextView(
                this@featuredRemoveAdsCard,
            ).apply {
                text =
                    "AD\n×"
                textSize =
                    27f
                setTypeface(
                    Typeface.DEFAULT_BOLD,
                )
                setTextColor(
                    Color.WHITE,
                )
                gravity =
                    Gravity.CENTER
                background =
                    storeRecessedSurfaceBackground(
                        cornerRadiusDp = 16f,
                        fillColors = intArrayOf(0xCC1067B8.toInt(), 0xCC094A92.toInt()),
                        strokeColor = 0xFF64E0FF.toInt(),
                        strokeWidthDp = 2,
                        insetDp = 6,
                    )
            },
            LinearLayout.LayoutParams(
                dp(118),
                dp(104),
            ),
        )

        val copy =
            LinearLayout(
                this@featuredRemoveAdsCard,
            ).apply {
                orientation =
                    LinearLayout.VERTICAL
                gravity =
                    Gravity.CENTER_VERTICAL
                setPadding(
                    dp(16),
                    0,
                    0,
                    0,
                )
            }
        copy.addView(
            TextView(
                this@featuredRemoveAdsCard,
            ).apply {
                text =
                    "Remove Ads"
                textSize =
                    19f
                setTypeface(
                    Typeface.DEFAULT_BOLD,
                )
                setTextColor(
                    Color.WHITE,
                )
            },
        )
        copy.addView(
            TextView(
                this@featuredRemoveAdsCard,
            ).apply {
                text =
                    "+" +
                        product.gemAmount +
                        " ◆"
                textSize =
                    17f
                setTypeface(
                    Typeface.DEFAULT_BOLD,
                )
                setTextColor(
                    LudoProofTheme.GOLD,
                )
                setPadding(
                    0,
                    dp(8),
                    0,
                    0,
                )
            },
        )
        body.addView(
            copy,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f,
            ),
        )
        addView(
            body,
        )

        addView(
            Button(
                this@featuredRemoveAdsCard,
            ).apply {
                text =
                    priceFor(
                        product.productId,
                        product.fallbackPrice,
                    )
                textSize =
                    18f
                setTypeface(
                    Typeface.DEFAULT_BOLD,
                )
                setTextColor(
                    Color.WHITE,
                )
                minHeight =
                    0
                background =
                    GradientDrawable(
                        GradientDrawable.Orientation.TOP_BOTTOM,
                        intArrayOf(
                            0xFF86E23C.toInt(),
                            0xFF4BAD1D.toInt(),
                        ),
                    ).apply {
                        cornerRadii =
                            floatArrayOf(
                                0f,
                                0f,
                                0f,
                                0f,
                                dp(15).toFloat(),
                                dp(15).toFloat(),
                                dp(15).toFloat(),
                                dp(15).toFloat(),
                            )
                    }
                setOnClickListener {
                    requestPurchase(
                        product.productId,
                    )
                }
                AppFocusRing.apply(this, cornerRadiusDp = 10f)
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(52),
            ),
        )
    }

private fun StoreActivity.rewardedAdCard():
    LinearLayout =
    LinearLayout(this).apply {
        orientation =
            LinearLayout.HORIZONTAL
        gravity =
            Gravity.CENTER_VERTICAL
        setPadding(
            dp(7),
            dp(5),
            dp(7),
            dp(5),
        )
        background =
            storeRecessedSurfaceBackground(
                cornerRadiusDp = 13f,
                fillColors = intArrayOf(0xD9144C9D.toInt(), 0xD90C2E66.toInt()),
                strokeColor = 0xFFA7429B.toInt(),
                strokeWidthDp = 2,
                insetDp = 6,
            )

        addView(
            TextView(
                this@rewardedAdCard,
            ).apply {
                text =
                    "5 GEMS  •  BEST"
                textSize =
                    12f
                setTypeface(
                    Typeface.DEFAULT_BOLD,
                )
                setTextColor(
                    LudoProofTheme.GOLD,
                )
                setPadding(0, 0, dp(10), 0)
            },
        )

        addView(
            Button(
                this@rewardedAdCard,
            ).apply {
                text =
                    "WATCH AD"
                textSize =
                    12f
                setTypeface(
                    Typeface.DEFAULT_BOLD,
                )
                setTextColor(
                    Color.WHITE,
                )
                minWidth =
                    0
                minHeight =
                    0
                background =
                    LudoProofTheme
                        .rounded(
                            0xFFE93434.toInt(),
                            10f,
                            0xFFFF7373.toInt(),
                            1f,
                            this@rewardedAdCard,
                        )
                setOnClickListener {
                    requestRewardedAd()
                }
                AppFocusRing.apply(this, cornerRadiusDp = 10f)
            },
            LinearLayout.LayoutParams(
                dp(108),
                dp(40),
            ),
        )
    }

private fun StoreActivity.gemProductCard(
    product: StoreProduct,
):
    LinearLayout =
    LinearLayout(this).apply {
        orientation =
            LinearLayout.VERTICAL
        gravity =
            Gravity.CENTER
        setPadding(
            dp(8),
            dp(12),
            dp(8),
            dp(10),
        )
        // Show the plywood surface directly; avoid the coloured arcade face overlay.
        background =
            plywoodBackground(
                context = this@gemProductCard,
                cornerRadiusDp = 16f,
            )
        elevation = dp(3).toFloat()

        val badge =
            product.title
                .substringAfter("·", "")
                .trim()
                .takeIf(String::isNotBlank)
                ?: when (product.productId) {
                    StoreCatalog.GEMS_20000 -> "Mega"
                    StoreCatalog.GEMS_40000 -> "Ultra"
                    else -> "Gems"
                }
        addView(
            TextView(this@gemProductCard).apply {
                text = badge.uppercase()
                textSize = 9f
                setTypeface(Typeface.DEFAULT_BOLD)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setPadding(dp(8), dp(4), dp(8), dp(4))
            },
        )

        addView(
            GemClusterView(
                this@gemProductCard,
                gemCount = gemClusterSize(product.gemAmount),
            ).apply {
                // Keep the gem art on a transparent canvas; no recessed pocket behind it.
                contentDescription = "${product.gemAmount} gems"
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(78),
            ),
        )

        addView(
            TextView(this@gemProductCard).apply {
                text = formatGemAmount(product.gemAmount)
                textSize = 19f
                setTypeface(Typeface.DEFAULT_BOLD)
                setTextColor(LudoProofTheme.GOLD)
                gravity = Gravity.CENTER
            },
        )
        addView(
            TextView(this@gemProductCard).apply {
                text = "GEMS"
                textSize = 10f
                setTypeface(Typeface.DEFAULT_BOLD)
                setTextColor(0xFFB9D8F7.toInt())
                gravity = Gravity.CENTER
                letterSpacing = 0.12f
                setPadding(0, dp(1), 0, dp(8))
            },
        )

        addView(
            Button(
                this@gemProductCard,
            ).apply {
                text =
                    priceFor(
                        product.productId,
                        product.fallbackPrice,
                    )
                textSize =
                    13f
                setTypeface(
                    Typeface.DEFAULT_BOLD,
                )
                setTextColor(
                    Color.WHITE,
                )
                minWidth =
                    0
                minHeight =
                    0
                setPadding(
                    dp(2),
                    0,
                    dp(2),
                    0,
                )
                background =
                    LudoProofTheme
                        .rounded(
                            0xFF70D72F.toInt(),
                            10f,
                            0xFF9EFF57.toInt(),
                            1f,
                            this@gemProductCard,
                        )
                setOnClickListener {
                    requestPurchase(
                        product.productId,
                    )
                }
                AppFocusRing.apply(this, cornerRadiusDp = 10f)
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(42),
            ),
        )
    }

private fun StoreActivity.storeSafetyNote():
    TextView =
    TextView(this).apply {
        text =
            "Payments are handled by Google Play. Gems and entitlements are granted only after purchase verification."
        textSize =
            10f
        setTextColor(
            0xFFC8D8EE.toInt(),
        )
        gravity =
            Gravity.CENTER
        setPadding(
            dp(14),
            dp(10),
            dp(14),
            dp(10),
        )
    }

private fun StoreActivity.formatGemAmount(
    amount: Int,
): String =
    when {
        amount >=
            1_000 &&
            amount %
                1_000 ==
                0 ->
            (
                amount /
                    1_000
                )
                .toString() +
                "K"

        else ->
            amount
                .toString()
    }

private fun StoreActivity.gemClusterSize(amount: Int): Int =
    when {
        amount >= 20_000 -> 5
        amount >= 3_500 -> 4
        amount >= 1_500 -> 3
        else -> 2
    }
