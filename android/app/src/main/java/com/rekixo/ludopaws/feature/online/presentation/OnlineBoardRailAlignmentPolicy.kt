package com.rekixo.ludopaws.feature.online

import kotlin.math.roundToInt

/**
 * Computes the rail translation from the rail's un-translated screen bounds.
 *
 * The same measured bounds always produce the same result. Callers must set this
 * absolute value instead of accumulating deltas, otherwise rerenders/relayouts
 * can make profile and dice rails drift away from the board.
 */
internal object OnlineBoardRailAlignmentPolicy {
    data class Translation(
        val top: Float,
        val bottom: Float,
    )

    fun calculate(
        boardTopOnScreen: Int,
        boardHeight: Int,
        topRailTopOnScreen: Int,
        topRailHeight: Int,
        bottomRailTopOnScreen: Int,
        gapPx: Int,
    ): Translation =
        Translation(
            top = (boardTopOnScreen - gapPx - topRailTopOnScreen - topRailHeight).toFloat(),
            bottom = (boardTopOnScreen + boardHeight + gapPx - bottomRailTopOnScreen).toFloat(),
        )

    fun dpToPx(dp: Int, density: Float): Int =
        (dp * density).roundToInt()
}
