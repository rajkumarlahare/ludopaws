package com.ludoproof.game.ui.offline.setup

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import com.ludoproof.game.LudoPawsCharacterAvatarView
import com.ludoproof.game.core.ui.art.recessedPlywoodBackground
import com.ludoproof.game.LudoProofTheme
import com.ludoproof.game.OfflineGameActivity
import com.ludoproof.game.feature.characters.domain.catalog.LudoPawsCharacterCatalog
import com.ludoproof.game.feature.characters.domain.model.AnimalCharacter
import com.ludoproof.game.feature.characters.domain.selection.StarterPawsAssignmentPolicy
import com.ludoproof.game.feature.offline.data.local.OfflineCharacterSetupSnapshot
import com.ludoproof.game.ui.offline.common.*

internal fun OfflineGameActivity.initializeCharacterSetup() {
    val preferred = characterSelectionStore.load()
    val saved =
        offlineCharacterSetupStore.loadSetup(
            mode = gameMode,
            playerCount = selectedPlayers,
            preferredCharacterId = preferred.characterId,
        )
    selectedCharacterSlot = saved.selectedSlot
    selectedCharacterIds = saved.characterIds
}

internal fun OfflineGameActivity.updateSelectedPlayerCount(playerCount: Int) {
    require(gameMode.supportsPlayerCount(playerCount)) {
        "Unsupported local player count: $playerCount"
    }
    selectedPlayers = playerCount
    selectedCharacterSlot =
        if (isComputerMode) {
            0
        } else {
            selectedCharacterSlot.coerceIn(0, playerCount - 1)
        }
    selectedCharacterIds =
        StarterPawsAssignmentPolicy.normalize(
            playerCount = playerCount,
            requestedCharacterIds = selectedCharacterIds,
            preferredCharacterId = characterSelectionStore.load().characterId,
            computerMode = isComputerMode,
        )
    persistCharacterSetup()
    rebuildCharacterSlotButtons()
    refreshSetupSelections()
}

internal fun OfflineGameActivity.selectCharacterSlot(slot: Int) {
    if (isComputerMode) {
        selectedCharacterSlot = 0
    } else if (slot in 0 until selectedPlayers) {
        selectedCharacterSlot = slot
    }
    persistCharacterSetup()
    refreshCharacterSelectionUi()
}

internal fun OfflineGameActivity.selectCharacterForActiveSlot(characterId: String) {
    selectedCharacterIds =
        StarterPawsAssignmentPolicy.select(
            playerCount = selectedPlayers,
            currentCharacterIds = selectedCharacterIds,
            selectedSlot = selectedCharacterSlot,
            requestedCharacterId = characterId,
            computerMode = isComputerMode,
        )

    selectedCharacterIds
        .firstOrNull()
        ?.let { playerOneCharacterId ->
            characterSelectionStore.select(
                packId = LudoPawsCharacterCatalog.STARTER_PACK_ID,
                characterId = playerOneCharacterId,
            )
        }

    persistCharacterSetup()
    refreshCharacterSelectionUi()
}

internal fun OfflineGameActivity.persistCharacterSetup() {
    val repaired =
        offlineCharacterSetupStore.saveSetup(
            mode = gameMode,
            snapshot =
                OfflineCharacterSetupSnapshot(
                    playerCount = selectedPlayers,
                    selectedSlot = selectedCharacterSlot,
                    characterIds = selectedCharacterIds,
                ),
        )
    selectedCharacterSlot = repaired.selectedSlot
    selectedCharacterIds = repaired.characterIds
}

internal fun OfflineGameActivity.persistActiveCharacterSetup() {
    persistCharacterSetup()
    offlineCharacterSetupStore.saveActive(
        mode = gameMode,
        playerCount = selectedPlayers,
        preferredColor = selectedColor,
        characterIds = selectedCharacterIds,
    )
}

internal fun OfflineGameActivity.characterPanel(): LinearLayout =
    selectionPanel(
        "CHOOSE YOUR PAW",
        if (isComputerMode) {
            "Pick your animal. Your computer rivals get the remaining paws."
        } else {
            "Give every player a different animal."
        },
    ).apply {
        characterSlotsRow =
            LinearLayout(this@characterPanel).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }.also(::addView)
        rebuildCharacterSlotButtons()

        val scroll =
            HorizontalScrollView(this@characterPanel).apply {
                isHorizontalScrollBarEnabled = false
                isFillViewport = false
                overScrollMode = HorizontalScrollView.OVER_SCROLL_NEVER
            }
        val cardsRow =
            LinearLayout(this@characterPanel).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                setPadding(dp(4), dp(10), dp(4), dp(4))
            }
        val cardMap = linkedMapOf<String, LinearLayout>()

        LudoPawsCharacterCatalog
            .charactersForPack(LudoPawsCharacterCatalog.STARTER_PACK_ID)
            .forEach { character ->
                val card = characterChoiceCard(character)
                cardMap[character.id] = card
                cardsRow.addView(
                    card,
                    LinearLayout.LayoutParams(
                        dp(if (isCompactSetup()) 100 else 112),
                        dp(if (isCompactSetup()) 140 else 152),
                    ).apply {
                        setMargins(dp(4), 0, dp(4), 0)
                    },
                )
            }
        characterCards = cardMap
        scroll.addView(cardsRow)
        addView(scroll)

        characterSummaryText =
            TextView(this@characterPanel).apply {
                LudoProofTheme.body(
                    this,
                    if (isCompactSetup()) 10.5f else 11.5f,
                    centered = true,
                    bright = true,
                )
                setTypeface(Typeface.DEFAULT_BOLD)
                setPadding(dp(12), dp(8), dp(12), dp(8))
                background = LudoProofTheme.rounded(
                    0xC8062861.toInt(),
                    999f,
                    0xFF267ACF.toInt(),
                    1f,
                    this@characterPanel,
                )
            }.also(::addView)

        refreshCharacterSelectionUi()
    }

private fun OfflineGameActivity.rebuildCharacterSlotButtons() {
    val slotsRow = characterSlotsRow ?: return
    slotsRow.removeAllViews()

    val slotMap = linkedMapOf<Int, Button>()
    val visibleSlots =
        if (isComputerMode) {
            listOf(0)
        } else {
            (0 until selectedPlayers).toList()
        }

    visibleSlots.forEach { slot ->
        val button =
            tileButton(
                if (isComputerMode) {
                    "YOU"
                } else {
                    "P${slot + 1}"
                },
            ) {
                selectCharacterSlot(slot)
            }
        slotMap[slot] = button
        slotsRow.addView(
            button,
            if (isComputerMode) {
                LinearLayout.LayoutParams(dp(126), dp(54)).apply {
                    setMargins(dp(4), 0, dp(4), 0)
                }
            } else {
                setupTileParams(
                    if (isCompactSetup()) 52 else 56,
                    marginDp = 4,
                )
            },
        )
    }
    characterSlotButtons = slotMap
}

private fun OfflineGameActivity.characterChoiceCard(
    character: AnimalCharacter,
): LinearLayout =
    LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        clipChildren = false
        clipToPadding = false
        setPadding(dp(5), dp(7), dp(5), dp(7))

        // Keep the existing production 3D portrait, but show it as a clean
        // cut-out inside the selection card rather than a white circular badge.
        LudoPawsCharacterAvatarView(this@characterChoiceCard).also { avatar ->
            avatar.tag = CHARACTER_AVATAR_TAG
            avatar.bind(
                characterId = character.id,
                ringColor = 0xFF5BE0FF.toInt(),
                showCircularChrome = false,
            )
            addView(
                avatar,
                LinearLayout.LayoutParams(
                    dp(if (isCompactSetup()) 70 else 80),
                    dp(if (isCompactSetup()) 70 else 80),
                ),
            )
        }

        addView(
            TextView(this@characterChoiceCard).apply {
                text = character.displayName
                textSize = if (isCompactSetup()) 14f else 15f
                setTypeface(Typeface.DEFAULT_BOLD)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                includeFontPadding = false
                setShadowLayer(2f, 0f, dp(1).toFloat(), 0xAA00112D.toInt())
                tag = CHARACTER_NAME_TAG
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(1) },
        )

        addView(
            TextView(this@characterChoiceCard).apply {
                text =
                    character.personality.name
                        .lowercase()
                        .replaceFirstChar(Char::uppercase)
                textSize = if (isCompactSetup()) 9.5f else 10.5f
                setTextColor(0xFFBCD5FF.toInt())
                gravity = Gravity.CENTER
                includeFontPadding = false
                setPadding(dp(8), dp(4), dp(8), dp(4))
                background = LudoProofTheme.rounded(
                    0xB803225C.toInt(),
                    999f,
                    0xFF178CD6.toInt(),
                    1f,
                    this@characterChoiceCard,
                )
                tag = CHARACTER_PERSONALITY_TAG
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(3) },
        )

        contentDescription = "Choose ${character.displayName}"
        setOnClickListener {
            selectCharacterForActiveSlot(character.id)
        }
    }

internal fun OfflineGameActivity.refreshCharacterSelectionUi() {
    if (selectedCharacterIds.size != selectedPlayers) {
        selectedCharacterIds =
            StarterPawsAssignmentPolicy.normalize(
                playerCount = selectedPlayers,
                requestedCharacterIds = selectedCharacterIds,
                preferredCharacterId = characterSelectionStore.load().characterId,
                computerMode = isComputerMode,
            )
    }

    val expectedSlots =
        if (isComputerMode) {
            setOf(0)
        } else {
            (0 until selectedPlayers).toSet()
        }
    if (
        characterSlotsRow != null &&
        characterSlotButtons.keys != expectedSlots
    ) {
        rebuildCharacterSlotButtons()
    }

    characterSlotButtons.forEach { (slot, button) ->
        if (slot == selectedCharacterSlot) {
            LudoProofTheme.selectedTile(button)
        } else {
            LudoProofTheme.normalTile(button)
        }
        val characterName =
            selectedCharacterIds
                .getOrNull(slot)
                ?.let(LudoPawsCharacterCatalog::character)
                ?.displayName
                .orEmpty()
        button.text =
            when {
                isComputerMode -> "YOU\n$characterName"
                else -> "P${slot + 1}\n$characterName"
            }
        button.setTextColor(
            if (slot == selectedCharacterSlot) {
                0xFF052A68.toInt()
            } else {
                Color.WHITE
            },
        )
    }

    val activeCharacterId = selectedCharacterIds.getOrNull(selectedCharacterSlot)
    characterCards.forEach { (characterId, card) ->
        val selected = characterId == activeCharacterId
        if (selected) {
            LudoProofTheme.selectedTile(card)
        } else {
            LudoProofTheme.normalTile(card)
            if (isComputerMode) {
                card.background = recessedPlywoodBackground(
                    context = this@refreshCharacterSelectionUi,
                    outerCornerRadiusDp = 12f,
                )
            }
        }
        card.findViewWithTag<LudoPawsCharacterAvatarView>(CHARACTER_AVATAR_TAG)
            ?.bind(
                characterId = characterId,
                ringColor =
                    if (selected) {
                        0xFFFFC72C.toInt()
                    } else {
                        0xFF5BE0FF.toInt()
                    },
                highlighted = selected,
                showCircularChrome = false,
            )
        card.findViewWithTag<TextView>(CHARACTER_NAME_TAG)
            ?.setTextColor(
                if (selected) {
                    0xFF052A68.toInt()
                } else {
                    Color.WHITE
                },
            )
        card.findViewWithTag<TextView>(CHARACTER_PERSONALITY_TAG)?.apply {
            setTextColor(
                if (selected) 0xFF174A78.toInt() else 0xFFBCD5FF.toInt(),
            )
            background = LudoProofTheme.rounded(
                if (selected) 0xB5FFE59A.toInt() else 0xB803225C.toInt(),
                999f,
                if (selected) 0xCCFFB51D.toInt() else 0xFF178CD6.toInt(),
                1f,
                this@refreshCharacterSelectionUi,
            )
        }
    }

    characterSummaryText?.text =
        selectedCharacterIds
            .mapIndexed { index, characterId ->
                val characterName =
                    LudoPawsCharacterCatalog
                        .character(characterId)
                        ?.displayName
                        ?: characterId
                val owner =
                    if (isComputerMode) {
                        if (index == 0) {
                            "YOU"
                        } else {
                            "CPU $index"
                        }
                    } else {
                        "P${index + 1}"
                    }
                "$owner $characterName"
            }
            .joinToString("  •  ")
}

private const val CHARACTER_AVATAR_TAG =
    "ludo_paws_character_avatar"
private const val CHARACTER_NAME_TAG =
    "ludo_paws_character_name"
private const val CHARACTER_PERSONALITY_TAG =
    "ludo_paws_character_personality"
