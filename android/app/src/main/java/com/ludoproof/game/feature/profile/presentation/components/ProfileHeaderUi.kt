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
import com.ludoproof.game.core.ui.components.AppFocusRing
import com.ludoproof.game.core.ui.components.appIconActionButton
import com.ludoproof.game.core.ui.icons.AppIconName
import com.ludoproof.game.feature.characters.data.local.CharacterSelectionStore
import com.ludoproof.game.feature.profile.domain.ProfileProgression
import com.ludoproof.game.feature.characters.domain.catalog.LudoPawsCharacterCatalog
import com.ludoproof.game.feature.profile.domain.model.ProfileSnapshot
import com.ludoproof.game.feature.profile.presentation.ProfileActivity
import com.ludoproof.game.feature.store.domain.model.StoreTab
import com.ludoproof.game.feature.store.presentation.StoreActivity

internal fun ProfileActivity.profileTopBar(): LinearLayout =
    LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL

        addView(
            appIconActionButton(
                context = this@profileTopBar,
                contentDescription = "Back",
                iconName = AppIconName.CHEVRON_LEFT,
                sizeDp = 50,
            ) { finish() },
            LinearLayout.LayoutParams(dp(50), dp(50)),
        )

        addView(
            TextView(this@profileTopBar).apply {
                text = "Profile"
                textSize = 25f
                setTypeface(Typeface.DEFAULT_BOLD)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), 0, 0, 0)
            },
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
        )

        addView(
            Button(this@profileTopBar).apply {
                text = "⚙  SETUP"
                textSize = 13f
                setTypeface(Typeface.DEFAULT_BOLD)
                setTextColor(0xFF303846.toInt())
                minWidth = 0
                minHeight = 0
                setPadding(dp(10), 0, dp(10), 0)
                background = LudoProofTheme.rounded(
                    0xFFF3F5F8.toInt(),
                    12f,
                    0xFFDCE4EF.toInt(),
                    1f,
                    this@profileTopBar,
                )
                contentDescription = "Profile and Play Games setup"
                isEnabled = !playGamesSignInInProgress
                alpha = if (isEnabled) 1f else 0.72f
                setOnClickListener { beginPlayGamesSignIn() }
                AppFocusRing.apply(this, cornerRadiusDp = 12f)
            },
            LinearLayout.LayoutParams(dp(104), dp(42)),
        )
    }

// Character personality is presentation-only and never changes dice or legal moves.
internal fun ProfileActivity.profileIdentityPanel(profile: ProfileSnapshot): LinearLayout =
    LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(10), dp(10), dp(10), dp(10))
        background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(0xFF0876E0.toInt(), 0xFF0649A8.toInt()),
        ).apply {
            cornerRadius = dp(20).toFloat()
            setStroke(dp(2), 0xFF21C8FF.toInt())
        }
        elevation = dp(6).toFloat()

        val selection = CharacterSelectionStore(this@profileIdentityPanel).load()
        val character = LudoPawsCharacterCatalog.character(selection.characterId)
            ?: LudoPawsCharacterCatalog.character(LudoPawsCharacterCatalog.DEFAULT_CHARACTER_ID)

        val heroRow = LinearLayout(this@profileIdentityPanel).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        // One portrait only: the persisted in-game Paw selection. No separate profile-photo avatar.
        val pawColumn = LinearLayout(this@profileIdentityPanel).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        val pawFrame = FrameLayout(this@profileIdentityPanel).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(0xFF149CFF.toInt(), 0xFF0751B5.toInt()),
            ).apply {
                cornerRadius = dp(18).toFloat()
                setStroke(dp(1), 0xFF63DFFF.toInt())
            }
        }
        pawFrame.addView(
            LudoPawsCharacterAvatarView(this@profileIdentityPanel).apply {
                setPadding(dp(3), dp(3), dp(3), dp(3))
                bind(
                    characterId = selection.characterId,
                    ringColor = 0xFFFFD34A.toInt(),
                    highlighted = true,
                )
                contentDescription = "Selected Paw: ${character?.displayName ?: "Paw"}"
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            },
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )
        pawColumn.addView(
            pawFrame,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(132)),
        )
        pawColumn.addView(
            Button(this@profileIdentityPanel).apply {
                text = "✎  Change"
                textSize = 12f
                setTypeface(Typeface.DEFAULT_BOLD)
                setTextColor(Color.WHITE)
                minWidth = 0
                minHeight = 0
                setPadding(dp(6), 0, dp(6), 0)
                background = LudoProofTheme.rounded(
                    0xFF064EAE.toInt(),
                    999f,
                    0xFF2ED3FF.toInt(),
                    1.5f,
                    this@profileIdentityPanel,
                )
                contentDescription = "Change selected Paw"
                setOnClickListener {
                    startActivity(
                        Intent(this@profileIdentityPanel, StoreActivity::class.java)
                            .putExtra(StoreActivity.EXTRA_INITIAL_TAB, StoreTab.PAWS.name),
                    )
                }
                AppFocusRing.apply(this, cornerRadiusDp = 999f)
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(36),
            ).apply { topMargin = dp(6) },
        )
        heroRow.addView(
            pawColumn,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.43f),
        )

        val details = LinearLayout(this@profileIdentityPanel).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(9), 0, 0, 0)
        }
        val signInRow = LinearLayout(this@profileIdentityPanel).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
        }
        signInRow.addView(
            Button(this@profileIdentityPanel).apply {
                text = when {
                    playGamesSignInInProgress -> "CONNECTING…"
                    playGamesAuthenticated -> "✓  CONNECTED"
                    else -> "▶  SIGN IN"
                }
                textSize = 11f
                setTypeface(Typeface.DEFAULT_BOLD)
                setTextColor(Color.WHITE)
                minWidth = 0
                minHeight = 0
                setPadding(dp(8), 0, dp(8), 0)
                background = LudoProofTheme.rounded(
                    0xFF058A69.toInt(),
                    13f,
                    0xFF20E1C0.toInt(),
                    1.5f,
                    this@profileIdentityPanel,
                )
                contentDescription = "Sign in to Google Play Games"
                isEnabled = !playGamesAuthenticated && !playGamesSignInInProgress
                alpha = if (isEnabled || playGamesAuthenticated) 1f else 0.78f
                setOnClickListener { beginPlayGamesSignIn() }
                AppFocusRing.apply(this, cornerRadiusDp = 13f)
            },
            LinearLayout.LayoutParams(dp(126), dp(38)),
        )
        details.addView(signInRow)

        val nameRow = LinearLayout(this@profileIdentityPanel).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(4), 0, 0)
        }
        nameRow.addView(
            TextView(this@profileIdentityPanel).apply {
                text = profile.displayName
                textSize = 21f
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                setTypeface(Typeface.DEFAULT_BOLD)
                setTextColor(Color.WHITE)
            },
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
        )
        nameRow.addView(
            Button(this@profileIdentityPanel).apply {
                text = "✎"
                textSize = 15f
                setTextColor(0xFFE2F8FF.toInt())
                minWidth = 0
                minHeight = 0
                setPadding(dp(6), 0, dp(6), 0)
                background = LudoProofTheme.rounded(
                    0x3327C6F4,
                    8f,
                    0xAA55DFFF.toInt(),
                    1f,
                    this@profileIdentityPanel,
                )
                contentDescription = "Edit profile name"
                setOnClickListener { showNameEditor() }
                AppFocusRing.apply(this, cornerRadiusDp = 8f)
            },
            LinearLayout.LayoutParams(dp(32), dp(30)).apply { marginStart = dp(4) },
        )
        details.addView(nameRow)

        val progress = ProfileProgression.levelProgress(profile.totalXp)
        val xpRow = LinearLayout(this@profileIdentityPanel).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(5), 0, 0)
        }
        xpRow.addView(
            TextView(this@profileIdentityPanel).apply {
                text = "★${progress.level}"
                textSize = 21f
                setTypeface(Typeface.DEFAULT_BOLD)
                setTextColor(0xFFFFD43B.toInt())
                gravity = Gravity.CENTER
            },
            LinearLayout.LayoutParams(dp(43), dp(34)),
        )

        val track = FrameLayout(this@profileIdentityPanel).apply {
            background = LudoProofTheme.rounded(
                0xFF052E78.toInt(),
                999f,
                0xFF3185D5.toInt(),
                1f,
                this@profileIdentityPanel,
            )
            clipToOutline = true
        }
        val fill = View(this@profileIdentityPanel).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(0xFFFFD83D.toInt(), 0xFFFFB812.toInt()),
            ).apply { cornerRadius = dp(999).toFloat() }
        }
        track.addView(
            fill,
            FrameLayout.LayoutParams(0, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.START),
        )
        track.addView(
            TextView(this@profileIdentityPanel).apply {
                text = "${progress.xpIntoLevel} / ${progress.xpForNextLevel} XP"
                textSize = 10.5f
                setTypeface(Typeface.DEFAULT_BOLD)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                maxLines = 1
            },
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )
        track.post {
            if (track.width > 0) {
                val params = fill.layoutParams as FrameLayout.LayoutParams
                params.width = (track.width * progress.fraction).toInt()
                    .coerceIn(0, track.width)
                fill.layoutParams = params
            }
        }
        xpRow.addView(
            track,
            LinearLayout.LayoutParams(0, dp(32), 1f),
        )
        details.addView(xpRow)

        val statsStrip = LinearLayout(this@profileIdentityPanel).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(2), dp(7), dp(2), dp(7))
            background = LudoProofTheme.rounded(
                0xC9082A69.toInt(),
                14f,
                0xFF176FCC.toInt(),
                1f,
                this@profileIdentityPanel,
            )
        }
        statsStrip.addView(
            statCell("🎮", profile.totalGames.toString(), "Games"),
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
        )
        statsStrip.addView(statDivider())
        statsStrip.addView(
            statCell("🔥", profile.currentDayStreak.toString(), "Day Streak"),
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
        )
        statsStrip.addView(statDivider())
        statsStrip.addView(
            statCell("🏆", profile.totalWins.toString(), "Wins"),
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
        )
        details.addView(
            statsStrip,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(8) },
        )

        heroRow.addView(
            details,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.57f),
        )
        addView(
            heroRow,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
    }

private fun ProfileActivity.statCell(symbol: String, value: String, label: String): LinearLayout =
    LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        addView(
            TextView(this@statCell).apply {
                text = symbol
                textSize = 17f
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                contentDescription = label
            },
            LinearLayout.LayoutParams(dp(26), dp(22)),
        )
        addView(
            TextView(this@statCell).apply {
                text = value
                textSize = 17f
                setTypeface(Typeface.DEFAULT_BOLD)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
        addView(
            TextView(this@statCell).apply {
                text = label
                textSize = 9f
                maxLines = 1
                setTextColor(0xFFD5F0FF.toInt())
                gravity = Gravity.CENTER
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
    }

private fun ProfileActivity.statDivider(): View =
    View(this).apply {
        setBackgroundColor(0xFF176FC2.toInt())
        layoutParams = LinearLayout.LayoutParams(dp(1), dp(48))
    }
