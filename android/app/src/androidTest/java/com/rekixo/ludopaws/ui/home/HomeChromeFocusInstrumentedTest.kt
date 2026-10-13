package com.rekixo.ludopaws.ui.home

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rekixo.ludopaws.LudoProofTheme
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeChromeFocusInstrumentedTest {
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun glassControlsShowAVisibleFocusTreatment() {
        assertFocusStateChangesPixels(
            homeGlassBackground(context, HomeGlassShape.PILL),
        )
    }

    @Test
    fun modeCardsShowAVisibleFocusTreatment() {
        assertFocusStateChangesPixels(
            homeModeButtonBackground(context, HomeModeTone.ONLINE),
        )
    }

    private fun assertFocusStateChangesPixels(drawable: Drawable) {
        val width = LudoProofTheme.dp(context, 150)
        val height = LudoProofTheme.dp(context, 60)
        drawable.setBounds(0, 0, width, height)

        val normal = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val focused = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        try {
            drawable.setState(intArrayOf())
            drawable.draw(Canvas(normal))

            assertTrue(
                "The drawable should respond to focus state",
                drawable.setState(intArrayOf(android.R.attr.state_focused)),
            )
            drawable.draw(Canvas(focused))

            var changedPixels = 0
            for (y in 0 until height) {
                for (x in 0 until width) {
                    if (normal.getPixel(x, y) != focused.getPixel(x, y)) {
                        changedPixels++
                    }
                }
            }

            assertTrue(
                "Focus should visibly change the chrome, changedPixels=$changedPixels",
                changedPixels > 0,
            )
            assertTrue("The normal surface should render", containsPaintedPixels(normal))
            assertTrue("The focused surface should render", containsPaintedPixels(focused))
        } finally {
            normal.recycle()
            focused.recycle()
        }
    }

    private fun containsPaintedPixels(bitmap: Bitmap): Boolean {
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                if (Color.alpha(bitmap.getPixel(x, y)) > 0) return true
            }
        }
        return false
    }
}
