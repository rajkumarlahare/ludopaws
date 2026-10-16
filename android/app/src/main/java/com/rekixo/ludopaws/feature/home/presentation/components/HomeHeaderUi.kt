package com.rekixo.ludopaws.ui.home

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.rekixo.ludopaws.HomeActivity
import com.rekixo.ludopaws.LudoProofTheme
import com.rekixo.ludopaws.core.ui.art.plywoodBackground
import com.rekixo.ludopaws.LudoPawsCharacterAvatarView
import com.rekixo.ludopaws.feature.characters.data.local.CharacterSelectionStore
import com.rekixo.ludopaws.feature.leaderboard.presentation.LeaderboardActivity
import com.rekixo.ludopaws.feature.profile.data.local.ProfileStore
import com.rekixo.ludopaws.feature.profile.presentation.ProfileActivity
import com.rekixo.ludopaws.feature.profile.presentation.BadgesActivity
import com.rekixo.ludopaws.feature.store.data.local.GemWalletStore
import com.rekixo.ludopaws.feature.store.domain.StoreCatalog
import com.rekixo.ludopaws.feature.store.presentation.StoreActivity

internal fun HomeActivity.profileHud(): LinearLayout =
    LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(
            dp(if (isCompact()) 2 else 3),
            dp(2),
            dp(if (isCompact()) 2 else 3),
            dp(3),
        )

        val hudInsetReductionDp =
            if (isCompact()) 6 else 8
        val hudHorizontalPaddingDp =
            (
                LudoProofTheme.pageHorizontalPaddingDp(this@profileHud) -
                    hudInsetReductionDp
                ).coerceAtLeast(6)
        val availableHudWidthPx =
            minOf(
                resources.displayMetrics.widthPixels -
                    dp(hudHorizontalPaddingDp * 2),
                dp(
                    LudoProofTheme.pageMaxContentWidthDp(this@profileHud) +
                        hudInsetReductionDp * 2,
                ),
            )
        // Keep the HUD aligned to the available Home content width.
        // Only the inner controls were compacted; shrinking the row itself
        // made Gems/Profile and the four shortcuts bunch up in the center.
        val hudWidthPx = availableHudWidthPx

        addView(
            homeIdentityRow(),
            LinearLayout.LayoutParams(
                hudWidthPx,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        addView(
            homeShortcutRow(),
            LinearLayout.LayoutParams(
                hudWidthPx,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(if (isCompact()) 6 else 8)
            },
        )

        connectivityText =
            TextView(this@profileHud).apply {
                text = "● CHECKING"
                visibility = View.GONE
                importantForAccessibility =
                    View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
        addView(connectivityText)
    }

private fun HomeActivity.homeIdentityRow(): LinearLayout =
    LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = dp(if (isCompact()) 53 else 57)
        // Keep only the Gems and Profile chips comfortably inset from the screen edges.
        setPadding(dp(if (isCompact()) 8 else 10), 0, dp(if (isCompact()) 8 else 10), 0)

        addView(
            homeCurrencyPill(),
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(if (isCompact()) 53 else 57),
            ),
        )

        addView(
            View(this@homeIdentityRow),
            LinearLayout.LayoutParams(
                0,
                1,
                1f,
            ),
        )

        addView(
            homeProfilePill(),
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(if (isCompact()) 53 else 57),
            ),
        )
    }

private fun HomeActivity.homeCurrencyPill(): LinearLayout =
    LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        isClickable = true
        isFocusable = true
        contentDescription = "Open gem store"
        background =
            plywoodBackground(
                context = this@homeCurrencyPill,
                cornerRadiusDp = if (isCompact()) 26.5f else 28.5f,
            )
        elevation = dp(5).toFloat()
        setPadding(
            dp(6),
            dp(5),
            dp(5),
            dp(5),
        )
        setOnClickListener {
            startActivity(
                Intent(
                    this@homeCurrencyPill,
                    StoreActivity::class.java,
                ),
            )
        }

        addView(
            HomeIconView(this@homeCurrencyPill).apply {
                kind = HomeIconKind.GEM
                importantForAccessibility =
                    View.IMPORTANT_FOR_ACCESSIBILITY_NO
            },
            LinearLayout.LayoutParams(
                dp(if (isCompact()) 39 else 44),
                LinearLayout.LayoutParams.MATCH_PARENT,
            ),
        )

        addView(
            TextView(this@homeCurrencyPill).apply {
                homeGemBalanceText = this
                text =
                    GemWalletStore(this@homeCurrencyPill)
                        .balance()
                        .toString()
                textSize = if (isCompact()) 16f else 17.5f
                setTypeface(Typeface.DEFAULT_BOLD)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setShadowLayer(
                    2.4f,
                    0f,
                    dp(1).toFloat(),
                    0xB0000000.toInt(),
                )
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT,
            ),
        )

        addView(
            HomeIconView(this@homeCurrencyPill).apply {
                kind = HomeIconKind.ADD
                iconColor = Color.WHITE
                isClickable = true
                isFocusable = true
                contentDescription = "Add currency"
                setPadding(
                    dp(if (isCompact()) 4 else 5),
                    dp(if (isCompact()) 4 else 5),
                    dp(if (isCompact()) 4 else 5),
                    dp(if (isCompact()) 4 else 5),
                )
                background =
                    plywoodBackground(
                        context = this@homeCurrencyPill,
                        cornerRadiusDp = if (isCompact()) 9.75f else 11f,
                    )
                setOnClickListener {
                    startActivity(
                        Intent(
                            this@homeCurrencyPill,
                            StoreActivity::class.java,
                        ),
                    )
                }
            },
            LinearLayout.LayoutParams(
                dp(if (isCompact()) 20 else 22),
                dp(if (isCompact()) 20 else 22),
            ),
        )
    }

private fun HomeActivity.homeProfilePill(): LinearLayout =
    LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        isClickable = true
        isFocusable = true
        contentDescription = "Guest user profile"
        background =
            plywoodBackground(
                context = this@homeProfilePill,
                cornerRadiusDp = if (isCompact()) 26.5f else 28.5f,
            )
        elevation = dp(5).toFloat()

        setPadding(
            dp(if (isCompact()) 12 else 14),
            dp(4),
            dp(if (isCompact()) 4 else 5),
            dp(4),
        )

        setOnClickListener {
            startActivity(
                Intent(
                    this@homeProfilePill,
                    ProfileActivity::class.java,
                ),
            )
        }

        addView(
            TextView(this@homeProfilePill).apply {
                homeProfileNameText = this
                text = homeDisplayName()
                textSize = if (isCompact()) 13.5f else 15f
                setTypeface(Typeface.DEFAULT_BOLD)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER_VERTICAL
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                maxWidth = dp(if (isCompact()) 96 else 112)
                setShadowLayer(
                    2.4f,
                    0f,
                    dp(1).toFloat(),
                    0xB0000000.toInt(),
                )
                setPadding(
                    0,
                    0,
                    dp(if (isCompact()) 6 else 7),
                    0,
                )
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT,
            ),
        )

        addView(
            LudoPawsCharacterAvatarView(this@homeProfilePill).apply {
                val selectedCharacterId =
                    CharacterSelectionStore(
                        this@homeProfilePill,
                    ).load().characterId
                bind(
                    characterId = selectedCharacterId,
                    ringColor = 0xFFFFD34A.toInt(),
                    highlighted = true,
                )
                setPadding(
                    dp(6),
                    dp(6),
                    dp(6),
                    dp(6),
                )
                background =
                    plywoodBackground(
                        context = this@homeProfilePill,
                        cornerRadiusDp = if (isCompact()) 22f else 24f,
                    )
                importantForAccessibility =
                    View.IMPORTANT_FOR_ACCESSIBILITY_NO
            },
            LinearLayout.LayoutParams(
                dp(if (isCompact()) 44 else 48),
                dp(if (isCompact()) 44 else 48),
            ),
        )
    }
private fun HomeActivity.homeShortcutRow(): LinearLayout =
    LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER

        listOf(
            HeaderShortcut(
                label = "Shop",
                tint = 0xFFFFC928.toInt(),
                iconKind = HomeIconKind.SHOP,
                enabled = true,
            ),
            HeaderShortcut(
                label = "Badge",
                tint = 0xFFFFC928.toInt(),
                iconKind = HomeIconKind.BADGE,
                enabled = true,
            ),
            HeaderShortcut(
                label = "Leaderboard",
                tint = 0xFFFFC928.toInt(),
                iconKind = HomeIconKind.LEADERBOARD,
                enabled = true,
            ),
            HeaderShortcut(
                label = "Remove Ads",
                tint = 0xFFFF3B30.toInt(),
                iconKind = HomeIconKind.AD_BLOCKER,
                enabled = true,
            ),
        ).forEach { item ->
            addView(
                homeShortcut(item),
                LinearLayout.LayoutParams(
                    0,
                    dp(if (isCompact()) 63 else 69),
                    1f,
                ).apply {
                    leftMargin = dp(1)
                    rightMargin = dp(1)
                },
            )
        }
    }

private fun HomeActivity.homeShortcut(
    item: HeaderShortcut,
): LinearLayout =
    LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        isClickable = item.enabled
        isFocusable = item.enabled
        isEnabled = item.enabled
        contentDescription =
            if (item.enabled) item.label else "${item.label} unavailable"
        alpha = if (item.enabled) 1f else 0.56f
        background =
            plywoodBackground(
                context = this@homeShortcut,
                cornerRadiusDp = 18f,
            )
        elevation = dp(5).toFloat()
        setPadding(
            dp(2),
            dp(if (isCompact()) 4 else 5),
            dp(2),
            dp(4),
        )
        if (item.enabled) {
            setOnClickListener {
                when (item.iconKind) {
                    HomeIconKind.SHOP ->
                        startActivity(
                            Intent(
                                this@homeShortcut,
                                StoreActivity::class.java,
                            ),
                        )

                    HomeIconKind.LEADERBOARD ->
                        startActivity(
                            Intent(
                                this@homeShortcut,
                                LeaderboardActivity::class.java,
                            ),
                        )

                    HomeIconKind.BADGE ->
                        startActivity(
                            Intent(
                                this@homeShortcut,
                                BadgesActivity::class.java,
                            ),
                        )

                    HomeIconKind.AD_BLOCKER ->
                        startActivity(
                            Intent(
                                this@homeShortcut,
                                StoreActivity::class.java,
                            ).putExtra(
                                StoreActivity.EXTRA_PURCHASE_PRODUCT_ID,
                                StoreCatalog.REMOVE_ADS,
                            ),
                        )

                    else -> Unit
                }
            }
        }

        addView(
            HomeIconView(this@homeShortcut).apply {
                kind = item.iconKind
                iconColor = item.tint
                importantForAccessibility =
                    View.IMPORTANT_FOR_ACCESSIBILITY_NO
            },
            LinearLayout.LayoutParams(
                dp(if (isCompact()) 36 else 41),
                dp(if (isCompact()) 36 else 41),
            ),
        )

        addView(
            TextView(this@homeShortcut).apply {
                text = item.label
                textSize =
                    when {
                        isCompact() && item.label.length > 10 -> 8f
                        isCompact() -> 8.5f
                        item.label.length > 10 -> 8f
                        else -> 9f
                    }
                setTypeface(Typeface.DEFAULT_BOLD)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                maxLines = 1
                setShadowLayer(
                    2f,
                    0f,
                    dp(1).toFloat(),
                    0xC0000000.toInt(),
                )
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f,
            ),
        )
    }

private fun HomeActivity.homeDisplayName(): String {
    val name =
        ProfileStore(this)
            .snapshot()
            .displayName
            .trim()
    return when {
        name.isBlank() -> "Guest User"
        name.equals("Guest", ignoreCase = true) -> "Guest User"
        else -> name
    }
}

internal fun HomeActivity.refreshHomeProfileSummary() {
    homeProfileNameText?.text = homeDisplayName()
}

internal fun HomeActivity.refreshHomeGemBalance() {
    homeGemBalanceText?.text =
        GemWalletStore(this)
            .balance()
            .toString()
}

private data class HeaderShortcut(
    val label: String,
    val tint: Int,
    val iconKind: HomeIconKind,
    val enabled: Boolean = false,
)
