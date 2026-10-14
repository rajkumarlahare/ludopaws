package com.rekixo.ludopaws

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DiceStylePalettePolicyTest {
    @Test
    fun catalogStylesHaveDistinctFacePalettes() {
        val ids = listOf(
            "dice_classic", "dice_pumpkin", "dice_diwali", "dice_football",
            "dice_cricket", "dice_summers", "dice_colors", "dice_heart",
        )
        val palettes = ids.map { DiceStylePalettePolicy.resolve(it, null).faceColors.joinToString(",") }
        assertEquals(ids.size, palettes.toSet().size)
    }

    @Test
    fun gameplayTeamColorKeepsOutlineWhilePreservingSkin() {
        val baseline = DiceStylePalettePolicy.resolve("dice_pumpkin", null)
        val teamColored = DiceStylePalettePolicy.resolve("dice_pumpkin", 0xFF008844.toInt())
        assertEquals(0xFF008844.toInt(), teamColored.borderColor)
        assertEquals(baseline.pipColor, teamColored.pipColor)
        assertEquals(baseline.faceColors.toList(), teamColored.faceColors.toList())
        assertNotEquals(
            DiceStylePalettePolicy.resolve("dice_classic", null).faceColors.toList(),
            baseline.faceColors.toList(),
        )
    }
}
