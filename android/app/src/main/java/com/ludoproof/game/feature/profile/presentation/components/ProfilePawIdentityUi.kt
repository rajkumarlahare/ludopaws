package com.ludoproof.game.feature.profile.presentation.components

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.ludoproof.game.LudoProofTheme
import com.ludoproof.game.LudoPawsCharacterAvatarView
import com.ludoproof.game.feature.characters.data.local.CharacterSelectionStore
import com.ludoproof.game.feature.characters.domain.catalog.LudoPawsCharacterCatalog
import com.ludoproof.game.feature.profile.presentation.ProfileActivity
import com.ludoproof.game.feature.store.domain.model.StoreTab
import com.ludoproof.game.feature.store.presentation.StoreActivity

internal fun ProfileActivity.profilePawIdentityPanel(): LinearLayout =
    LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(12), dp(10), dp(12), dp(10))
        background = LudoProofTheme.rounded(
            0xE9153879.toInt(),
            14f,
            0xFF55C8FF.toInt(),
            1.5f,
            this@profilePawIdentityPanel,
        )

        val selection = CharacterSelectionStore(this@profilePawIdentityPanel).load()
        val character = LudoPawsCharacterCatalog.character(selection.characterId)
            ?: LudoPawsCharacterCatalog.character(LudoPawsCharacterCatalog.DEFAULT_CHARACTER_ID)

        val header = LinearLayout(this@profilePawIdentityPanel).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(
            TextView(this@profilePawIdentityPanel).apply {
                text = "YOUR PAW"
                textSize = 12f
                setTypeface(Typeface.DEFAULT_BOLD)
                setTextColor(0xFF8DE8FF.toInt())
                letterSpacing = 0.06f
            },
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
        )
        header.addView(
            Button(this@profilePawIdentityPanel).apply {
                text = "OPEN PAW STORE"
                textSize = 10.5f
                setTypeface(Typeface.DEFAULT_BOLD)
                setTextColor(Color.WHITE)
                minWidth = 0
                minHeight = 0
                setPadding(dp(10), 0, dp(10), 0)
                contentDescription = "Open Paw character store"
                background = LudoProofTheme.rounded(
                    0xFF0AA54A.toInt(),
                    10f,
                    0xFF55E980.toInt(),
                    1f,
                    this@profilePawIdentityPanel,
                )
                setOnClickListener {
                    startActivity(
                        Intent(this@profilePawIdentityPanel, StoreActivity::class.java)
                            .putExtra(StoreActivity.EXTRA_INITIAL_TAB, StoreTab.PAWS.name),
                    )
                }
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(34),
            ),
        )
        addView(header)

        val row = LinearLayout(this@profilePawIdentityPanel).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, 0)
        }
        val portraitFrame = FrameLayout(this@profilePawIdentityPanel).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xFFF9FBFF.toInt())
                setStroke(dp(2), 0xFFFFC928.toInt())
            }
        }
        portraitFrame.addView(
            LudoPawsCharacterAvatarView(this@profilePawIdentityPanel).apply {
                setPadding(dp(4), dp(4), dp(4), dp(4))
                bind(
                    characterId = character?.id,
                    ringColor = 0xFFFFC928.toInt(),
                    highlighted = true,
                )
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
                contentDescription = character?.displayName ?: "Paw"
            },
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )
        row.addView(
            portraitFrame,
            LinearLayout.LayoutParams(dp(64), dp(64)),
        )

        row.addView(
            TextView(this@profilePawIdentityPanel).apply {
                text = character?.displayName ?: "Paw"
                textSize = 20f
                maxLines = 1
                setTypeface(Typeface.DEFAULT_BOLD)
                setTextColor(Color.WHITE)
                setPadding(dp(12), 0, 0, 0)
                gravity = Gravity.CENTER_VERTICAL
            },
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f,
            ),
        )
        addView(row)
    }
