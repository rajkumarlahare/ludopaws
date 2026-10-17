package com.rekixo.ludopaws.feature.online

import kotlin.math.roundToInt

/**
 * Computes absolute rail translation from visible profile/dice anchor bounds.
 *
 * Anchor bounds exclude reserved quick-chat space and refer to the square board,
 * not the flexible viewport container. Recalculation is deterministic.
 */
internal object OnlineBoardRailAlignmentPolicy {
    data class Translation(
        val top: Float,
        val bottom: Float,
    )

    fun calculate(
        boardTopOnScreen: Int,
        boardHeight: Int,
        topAnchorTopOnScreen: Int,
        topAnchorHeight: Int,
        bottomAnchorTopOnScreen: Int,
        gapPx: Int,
    ): Translation =
        Translation(
            top = (boardTopOnScreen - gapPx - topAnchorTopOnScreen - topAnchorHeight).toFloat(),
            bottom = (boardTopOnScreen + boardHeight + gapPx - bottomAnchorTopOnScreen).toFloat(),
        )

    fun dpToPx(dp: Int, density: Float): Int =
        (dp * density).roundToInt()
}
