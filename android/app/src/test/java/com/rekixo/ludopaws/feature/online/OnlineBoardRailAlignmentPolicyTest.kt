package com.rekixo.ludopaws.feature.online

import org.junit.Assert.assertEquals
import org.junit.Test

class OnlineBoardRailAlignmentPolicyTest {
    @Test
    fun keepsTopAndBottomRailsAtTheSameBoardGap() {
        val result = OnlineBoardRailAlignmentPolicy.calculate(
            boardTopOnScreen = 500,
            boardHeight = 360,
            topRailTopOnScreen = 390,
            topRailHeight = 72,
            bottomRailTopOnScreen = 900,
            gapPx = 8,
        )

        assertEquals(30f, result.top, 0f)
        assertEquals(-32f, result.bottom, 0f)
    }

    @Test
    fun recalculationIsIndependentOfPreviousTranslation() {
        val first = OnlineBoardRailAlignmentPolicy.calculate(
            boardTopOnScreen = 500,
            boardHeight = 360,
            topRailTopOnScreen = 390,
            topRailHeight = 72,
            bottomRailTopOnScreen = 900,
            gapPx = 8,
        )
        val afterRelayout = OnlineBoardRailAlignmentPolicy.calculate(
            boardTopOnScreen = 500,
            boardHeight = 360,
            topRailTopOnScreen = 390,
            topRailHeight = 72,
            bottomRailTopOnScreen = 900,
            gapPx = 8,
        )

        assertEquals(first, afterRelayout)
    }

    @Test
    fun dpConversionUsesDeviceDensityAndRoundsToNearestPixel() {
        assertEquals(12, OnlineBoardRailAlignmentPolicy.dpToPx(8, 1.5f))
        assertEquals(20, OnlineBoardRailAlignmentPolicy.dpToPx(8, 2.5f))
    }
}
