package com.ludoproof.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.View
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ludoproof.game.core.ui.components.AppFocusRing
import com.ludoproof.game.core.ui.components.AppFocusRingDrawable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppFocusRingInstrumentedTest {
    @Test
    fun ringAddsPixelsForFocusedStateOnly() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val density = context.resources.displayMetrics.density
            val width = (120 * density).toInt().coerceAtLeast(1)
            val height = (52 * density).toInt().coerceAtLeast(1)
            val ring = AppFocusRingDrawable(
                context = context,
                cornerRadiusDp = 12f,
                strokeWidthDp = 3f,
            ).apply { setBounds(0, 0, width, height) }
            val normal = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val focused = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            try {
                ring.setState(intArrayOf())
                ring.draw(Canvas(normal))
                assertTrue("A focus state must be handled", ring.setState(intArrayOf(android.R.attr.state_focused)))
                ring.draw(Canvas(focused))
                assertEquals(0, countPaintedPixels(normal))
                assertTrue("Focused controls must have a visible outline", countPaintedPixels(focused) > 0)
            } finally {
                normal.recycle()
                focused.recycle()
            }
        }
    }

    @Test
    fun attachingRingPreservesControlBackgroundAndExistingForeground() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val view = View(context)
            val originalBackground = GradientDrawable().apply {
                setColor(Color.BLUE)
                cornerRadius = 12f
            }
            val originalForeground = ColorDrawable(Color.MAGENTA)
            view.background = originalBackground
            view.foreground = originalForeground

            AppFocusRing.apply(view)

            assertSame(originalBackground, view.background)
            assertNotNull(view.foreground)
            assertNotSame(originalForeground, view.foreground)
            assertTrue(view.foreground is LayerDrawable)
        }
    }

    private fun countPaintedPixels(bitmap: Bitmap): Int {
        var count = 0
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                if (Color.alpha(bitmap.getPixel(x, y)) > 0) count++
            }
        }
        return count
    }
}
