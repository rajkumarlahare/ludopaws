package com.ludoproof.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ludoproof.game.core.ui.icons.AppIconName
import com.ludoproof.game.core.ui.icons.AppIconVariant
import com.ludoproof.game.core.ui.icons.AppIconView
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppIconViewRenderingInstrumentedTest {
    @Test
    fun everySharedIconAndVariantRendersAtSmallAndLargeSizes() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val density = context.resources.displayMetrics.density
        val sizes = listOf(16, 32).map { (it * density).toInt().coerceAtLeast(1) }
        val variants = AppIconVariant.entries

        for (size in sizes) {
            val exactSize = View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY)

            for (name in AppIconName.entries) {
                for (variant in variants) {
                    val icon = AppIconView(
                        context = context,
                        iconName = name,
                        iconTint = Color.WHITE,
                        iconVariant = variant,
                    )
                    icon.measure(exactSize, exactSize)
                    icon.layout(0, 0, size, size)

                    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                    try {
                        icon.draw(Canvas(bitmap))
                        var paintedPixels = 0
                        for (y in 0 until size) {
                            for (x in 0 until size) {
                                if (Color.alpha(bitmap.getPixel(x, y)) > 0) {
                                    paintedPixels++
                                }
                            }
                        }
                        assertTrue(
                            "Expected icon drawing to have visible pixels",
                            paintedPixels > 0,
                        )
                    } finally {
                        bitmap.recycle()
                    }
                }
            }
        }
    }
}
