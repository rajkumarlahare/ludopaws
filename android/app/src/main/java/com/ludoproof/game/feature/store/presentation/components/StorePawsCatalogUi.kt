package com.ludoproof.game.feature.store.presentation.components

import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import com.ludoproof.game.LudoPawsCharacterAvatarView
import com.ludoproof.game.core.ui.art.playerCountSelectionBackground
import com.ludoproof.game.core.ui.art.plywoodBackground
import com.ludoproof.game.core.ui.components.AppFocusRing
import com.ludoproof.game.core.ui.components.playSelectionPopAnimation
import com.ludoproof.game.feature.characters.data.local.CharacterSelectionStore
import com.ludoproof.game.feature.characters.domain.catalog.LudoPawsCharacterCatalog
import com.ludoproof.game.feature.characters.domain.model.AnimalCharacter
import com.ludoproof.game.feature.store.domain.StoreCosmeticCatalog
import com.ludoproof.game.feature.store.domain.model.StoreCosmetic
import com.ludoproof.game.feature.store.presentation.StoreActivity

internal fun StoreActivity.pawsCatalogContent(): LinearLayout =
    LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL

        // These are the four live, selectable 3D characters from the free Starter Paws pack.
        val starterCharacters =
            listOf("cat", "dog", "goat", "duck").mapNotNull(
                LudoPawsCharacterCatalog::character,
            )
        addView(
            this@pawsCatalogContent.pawsCharacterGrid(starterCharacters),
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        // Reserved packs stay visible as plywood-only Coming Soon cards; no unlock costs/actions.
        val upcomingPacks =
            StoreCosmeticCatalog.characterPacks.filter {
                it.id != StoreCosmeticCatalog.DEFAULT_CHARACTER_PACK
            }
        if (upcomingPacks.isNotEmpty()) {
            addView(
                this@pawsCatalogContent.pawsComingSoonGrid(upcomingPacks),
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply {
                    topMargin = dp(12)
                },
            )
        }
    }

private fun StoreActivity.pawsCharacterGrid(
    characters: List<AnimalCharacter>,
): LinearLayout =
    LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        val selectedCharacterId =
            CharacterSelectionStore(this@pawsCharacterGrid).load().characterId

        characters.chunked(2).forEachIndexed { rowIndex, rowCharacters ->
            val row =
                LinearLayout(this@pawsCharacterGrid).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.TOP
                }

            rowCharacters.forEach { character ->
                row.addView(
                    pawsCharacterCard(
                        character = character,
                        selected = character.id == selectedCharacterId,
                    ),
                    LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f,
                    ).apply {
                        setMargins(dp(4), 0, dp(4), 0)
                    },
                )
            }

            repeat(2 - rowCharacters.size) {
                row.addView(
                    TextView(this@pawsCharacterGrid),
                    LinearLayout.LayoutParams(0, 1, 1f),
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

private fun StoreActivity.pawsCharacterCard(
    character: AnimalCharacter,
    selected: Boolean,
): LinearLayout =
    LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        clipChildren = false
        clipToPadding = false
        setPadding(dp(8), dp(10), dp(8), dp(10))
        background =
            playerCountSelectionBackground(
                context = this@pawsCharacterCard,
                selected = selected,
                outerCornerRadiusDp = 13f,
                checkBadgeAtTopEnd = true,
                emphasizeSelected = true,
                showCheckBadge = true,
                checkMarkScale = 1.35f,
            )
        elevation = dp(if (selected) 5 else 3).toFloat()
        isClickable = true
        isFocusable = true
        contentDescription =
            "${character.displayName}, ${if (selected) "selected" else "unlocked"}"

        addView(
            LudoPawsCharacterAvatarView(this@pawsCharacterCard).apply {
                // Reuse the same production 3D portrait as gameplay and character selection.
                bind(
                    characterId = character.id,
                    ringColor = 0xFF5BE0FF.toInt(),
                    highlighted = false,
                    showCircularChrome = false,
                )
                importantForAccessibility =
                    android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
            },
            LinearLayout.LayoutParams(
                dp(96),
                dp(96),
            ),
        )

        addView(
            TextView(this@pawsCharacterCard).apply {
                text = character.displayName.uppercase()
                textSize = 15f
                setTypeface(Typeface.DEFAULT_BOLD)
                setTextColor(0xFF57351E.toInt())
                gravity = Gravity.CENTER
                includeFontPadding = false
                maxLines = 1
                setShadowLayer(0.8f, 0f, dp(0.5f), 0x44FFFFFF)
                setPadding(0, dp(6), 0, dp(3))
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        addView(
            TextView(this@pawsCharacterCard).apply {
                text = if (selected) "SELECTED" else "UNLOCKED"
                textSize = 10f
                setTypeface(Typeface.DEFAULT_BOLD)
                setTextColor(
                    if (selected) 0xFF77501B.toInt() else 0xFF286C37.toInt(),
                )
                gravity = Gravity.CENTER
                includeFontPadding = false
                setPadding(dp(4), dp(4), dp(4), dp(4))
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        setOnClickListener { clicked ->
            clicked.playSelectionPopAnimation {
                CharacterSelectionStore(this@pawsCharacterCard).select(
                    packId = LudoPawsCharacterCatalog.STARTER_PACK_ID,
                    characterId = character.id,
                )
                renderStore()
            }
        }
        AppFocusRing.apply(this, cornerRadiusDp = 13f)
    }

private fun StoreActivity.pawsComingSoonGrid(
    packs: List<StoreCosmetic>,
): LinearLayout =
    LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL

        packs.chunked(3).forEachIndexed { rowIndex, rowPacks ->
            val row =
                LinearLayout(this@pawsComingSoonGrid).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.TOP
                }

            rowPacks.forEach { pack ->
                row.addView(
                    pawsComingSoonCard(pack),
                    LinearLayout.LayoutParams(
                        0,
                        dp(98),
                        1f,
                    ).apply {
                        setMargins(dp(4), 0, dp(4), 0)
                    },
                )
            }

            repeat(3 - rowPacks.size) {
                row.addView(
                    TextView(this@pawsComingSoonGrid),
                    LinearLayout.LayoutParams(0, dp(98), 1f),
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

private fun StoreActivity.pawsComingSoonCard(
    pack: StoreCosmetic,
): LinearLayout =
    LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        setPadding(dp(6), dp(8), dp(6), dp(8))
        // Keep only the shared plywood; discard the old tinted arcade face and colour preview.
        background =
            plywoodBackground(
                context = this@pawsComingSoonCard,
                cornerRadiusDp = 13f,
            )
        elevation = dp(2).toFloat()
        contentDescription = "${pack.title}, coming soon"

        addView(
            TextView(this@pawsComingSoonCard).apply {
                text = pack.title
                textSize = 10.5f
                setTypeface(Typeface.DEFAULT_BOLD)
                setTextColor(0xFF57351E.toInt())
                gravity = Gravity.CENTER
                includeFontPadding = false
                maxLines = 2
                setPadding(0, 0, 0, dp(8))
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f,
            ),
        )

        addView(
            TextView(this@pawsComingSoonCard).apply {
                text = "COMING SOON"
                textSize = 9f
                setTypeface(Typeface.DEFAULT_BOLD)
                setTextColor(0xFF754D2E.toInt())
                gravity = Gravity.CENTER
                includeFontPadding = false
                maxLines = 1
            },
        )
    }
