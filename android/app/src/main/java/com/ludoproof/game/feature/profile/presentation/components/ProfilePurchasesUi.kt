package com.ludoproof.game.feature.profile.presentation.components

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.ludoproof.game.LudoProofTheme
import com.ludoproof.game.core.ui.art.PlywoodTextureDrawable
import com.ludoproof.game.feature.profile.domain.model.ProfileSnapshot
import com.ludoproof.game.feature.profile.presentation.ProfileActivity

internal fun ProfileActivity.profilePurchasesPanel(profile: ProfileSnapshot): LinearLayout =
    profilePanel().apply {
        val heading = LinearLayout(this@profilePurchasesPanel).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        heading.addView(
            profileTitle("Purchased Products"),
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
        )
        heading.addView(
            Button(this@profilePurchasesPanel).apply {
                text = "VIEW ALL  ›"
                textSize = 11f
                setTypeface(Typeface.DEFAULT_BOLD)
                setTextColor(0xFFFFD43B.toInt())
                minWidth = 0
                minHeight = 0
                setPadding(0, 0, 0, 0)
                background = null
                contentDescription = "View all purchased products"
                setOnClickListener {
                    viewPurchases(
                        if (profile.purchasedProducts.isEmpty()) {
                            "No purchases yet."
                        } else {
                            profile.purchasedProducts.joinToString("\n") { "• $it" }
                        },
                    )
                }
            },
        )
        addView(heading)

        if (profile.purchasedProducts.isEmpty()) {
            val emptyState = LinearLayout(this@profilePurchasesPanel).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(0, dp(10), 0, 0)
            }
            emptyState.addView(
                TextView(this@profilePurchasesPanel).apply {
                    text = "🛒"
                    textSize = 32f
                    gravity = Gravity.CENTER
                    setTextColor(0xFFBDE5FF.toInt())
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(0x1C3DBEFF)
                        setStroke(dp(1), 0x663EABF3)
                    }
                },
                LinearLayout.LayoutParams(dp(60), dp(60)),
            )
            emptyState.addView(
                TextView(this@profilePurchasesPanel).apply {
                    text = "No purchases yet"
                    textSize = 15f
                    setTypeface(Typeface.DEFAULT_BOLD)
                    setTextColor(Color.WHITE)
                    gravity = Gravity.CENTER
                },
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = dp(8) },
            )
            emptyState.addView(
                TextView(this@profilePurchasesPanel).apply {
                    text = "Explore the store for exciting items\nand upgrades!"
                    textSize = 12f
                    setTextColor(0xFFD0E7FF.toInt())
                    gravity = Gravity.CENTER
                    setLineSpacing(dp(2).toFloat(), 1f)
                    setPadding(dp(4), dp(4), dp(4), 0)
                },
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ),
            )
            addView(emptyState)
        } else {
            addView(
                TextView(this@profilePurchasesPanel).apply {
                    text = profile.purchasedProducts.joinToString("  •  ")
                    textSize = 13f
                    setTextColor(0xFFE8F5FF.toInt())
                    gravity = Gravity.CENTER
                    setPadding(0, dp(24), 0, dp(20))
                },
            )
        }
    }
