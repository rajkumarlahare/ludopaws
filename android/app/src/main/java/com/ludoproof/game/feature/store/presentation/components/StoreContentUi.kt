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

        addView(
            gemIntroCard(),
        )
        addView(
            TextView(this@gemsContent).apply {
                text = "GEM POUCHES"
                textSize = 13f
                setTypeface(Typeface.DEFAULT_BOLD)
                setTextColor(0xFFEAF7FF.toInt())
                letterSpacing = 0.14f
                setPadding(dp(6), dp(14), dp(6), dp(2))
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
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

        addView(
            rewardedAdCard(),
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(10)
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

private fun StoreActivity.gemIntroCard(): LinearLayout =
    LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(14), dp(10), dp(14), dp(10))
        background =
            GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xFF124F91.toInt(), 0xFF34246F.toInt()),
            ).apply {
                cornerRadius = dp(16).toFloat()
                setStroke(dp(1), 0xFF53DFFF.toInt())
            }

        addView(
            GemClusterView(this@gemIntroCard, gemCount = 3).apply {
                contentDescription = "A cluster of shining gems"
            },
            LinearLayout.LayoutParams(dp(76), dp(72)),
        )

        addView(
            LinearLayout(this@gemIntroCard).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(10), 0, 0, 0)
                addView(
                    TextView(this@gemIntroCard).apply {
                        text = "A LITTLE SPARK GOES A LONG WAY"
                        textSize = 14f
                        setTypeface(Typeface.DEFAULT_BOLD)
                        setTextColor(Color.WHITE)
                    },
                )
                addView(
                    TextView(this@gemIntroCard).apply {
                        text = "Pick a gem pouch for your next style upgrade."
                        textSize = 12f
                        setTextColor(0xFFD4E6FF.toInt())
                        setPadding(0, dp(5), 0, 0)
                    },
                )
            },
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
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
            GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(
                    0xFF4A156F.toInt(),
                    0xFF2A0C54.toInt(),
                ),
            ).apply {
                cornerRadius =
                    dp(15)
                        .toFloat()
                setStroke(
                    dp(2),
                    0xFF8939A8.toInt(),
                )
            }

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
                    LudoProofTheme
                        .rounded(
                            0xFF1067B8.toInt(),
                            16f,
                            0xFF64E0FF.toInt(),
                            2f,
                            this@featuredRemoveAdsCard,
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
            dp(12),
            dp(10),
            dp(12),
            dp(10),
        )
        background =
            LudoProofTheme
                .rounded(
                    0xE9144C9D.toInt(),
                    13f,
                    0xFFA7429B.toInt(),
                    2f,
                    this@rewardedAdCard,
                )

        addView(
            TextView(
                this@rewardedAdCard,
            ).apply {
                text =
                    "5 ◆"
                textSize =
                    18f
                setTypeface(
                    Typeface.DEFAULT_BOLD,
                )
                setTextColor(
                    LudoProofTheme.GOLD,
                )
            },
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f,
            ),
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
                dp(112),
                dp(42),
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
        background =
            GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(
                    0xFF1A65AA.toInt(),
                    0xFF123D82.toInt(),
                    0xFF111D55.toInt(),
                ),
            ).apply {
                cornerRadius =
                    dp(16)
                        .toFloat()
                setStroke(
                    dp(1),
                    0xFF55DFFF.toInt(),
                )
            }
        elevation = dp(3).toFloat()

        product.title
            .substringAfter("·", "")
            .trim()
            .takeIf(String::isNotBlank)
            ?.let { badge ->
                addView(
                    TextView(this@gemProductCard).apply {
                        text = badge.uppercase()
                        textSize = 9f
                        setTypeface(Typeface.DEFAULT_BOLD)
                        setTextColor(0xFF082B57.toInt())
                        gravity = Gravity.CENTER
                        setPadding(dp(8), dp(4), dp(8), dp(4))
                        background =
                            LudoProofTheme.rounded(
                                0xFFFFD96A.toInt(),
                                999f,
                                0xFFFFF0A8.toInt(),
                                1f,
                                this@gemProductCard,
                            )
                    },
                )
            }

        addView(
            GemClusterView(
                this@gemProductCard,
                gemCount = gemClusterSize(product.gemAmount),
            ).apply {
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
