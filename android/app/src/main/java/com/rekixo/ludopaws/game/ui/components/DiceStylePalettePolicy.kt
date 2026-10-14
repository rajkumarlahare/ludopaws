package com.rekixo.ludopaws

/** Render-only palettes for store/game dice. They never influence roll outcomes or proof state. */
internal data class DicePalette(
    val faceColors: IntArray,
    val borderColor: Int,
    val pipColor: Int,
)

internal object DiceStylePalettePolicy {
    fun resolve(styleId: String, teamColor: Int?): DicePalette {
        val style = when (styleId) {
            "dice_pumpkin" -> DicePalette(
                intArrayOf(0xFFFFB65C.toInt(), 0xFFD9802F.toInt(), 0xFFB65E25.toInt()),
                0xFF6A351C.toInt(),
                0xFF3D2418.toInt(),
            )
            "dice_diwali" -> DicePalette(
                intArrayOf(0xFFFFE8A0.toInt(), 0xFFFFC832.toInt(), 0xFFB21C32.toInt()),
                0xFFB21C32.toInt(),
                0xFF7C1524.toInt(),
            )
            "dice_football" -> DicePalette(
                intArrayOf(0xFFFFFFFF.toInt(), 0xFFF5F5F5.toInt(), 0xFFD4D4D4.toInt()),
                0xFF222222.toInt(),
                0xFF222222.toInt(),
            )
            "dice_cricket" -> DicePalette(
                intArrayOf(0xFFE8F4D8.toInt(), 0xFF9DD68E.toInt(), 0xFF4F9E5F.toInt()),
                0xFF2B6C36.toInt(),
                0xFF173F23.toInt(),
            )
            "dice_summers" -> DicePalette(
                intArrayOf(0xFFFFFDF2.toInt(), 0xFFFFF1D4.toInt(), 0xFFFFD2A9.toInt()),
                0xFFE84432.toInt(),
                0xFFA62520.toInt(),
            )
            "dice_colors" -> DicePalette(
                intArrayOf(0xFFFFF08B.toInt(), 0xFFFFD63B.toInt(), 0xFFEF3F8A.toInt()),
                0xFFEF3F8A.toInt(),
                0xFF5C1645.toInt(),
            )
            "dice_heart" -> DicePalette(
                intArrayOf(0xFFFFFFFF.toInt(), 0xFFFFF8F8.toInt(), 0xFFF3C8D0.toInt()),
                0xFFD82E3D.toInt(),
                0xFFD82E3D.toInt(),
            )
            else -> DicePalette(
                intArrayOf(0xFFF4F4F4.toInt(), 0xFFF4F4F4.toInt(), 0xFFF4F4F4.toInt()),
                0xFF8A8A8A.toInt(),
                0xFF3A3A3A.toInt(),
            )
        }
        return if (teamColor == null) style else style.copy(borderColor = teamColor)
    }
}
