package com.rekixo.ludopaws

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReferenceArray

@RunWith(AndroidJUnit4::class)
class LudoPawsCharacterPortraitBitmapCacheTest {
    @Test
    fun eachProductionAnimalProducesVisibleTransparentPortraitPixels() {
        val characterIds = listOf("dog", "goat", "duck", "cat")
        val completed = CountDownLatch(characterIds.size)
        val images = AtomicReferenceArray<Bitmap?>(characterIds.size)

        characterIds.forEachIndexed { index, characterId ->
            LudoPawsCharacterPortraitBitmapCache.request(
                characterId = characterId,
                teamColor = "BLUE",
            ) { bitmap ->
                images.set(index, bitmap)
                completed.countDown()
            }
        }

        assertTrue(
            "Timed out while rendering production 3D character portraits",
            completed.await(30, TimeUnit.SECONDS),
        )

        characterIds.forEachIndexed { index, characterId ->
            val bitmap = images.get(index)
            assertNotNull(
                "Expected a 3D portrait bitmap for $characterId",
                bitmap,
            )
            val portrait = requireNotNull(bitmap)
            assertTrue(
                "$characterId portrait should have a useful render size",
                portrait.width >= 128 && portrait.height >= 128,
            )

            val pixels = IntArray(portrait.width * portrait.height)
            portrait.getPixels(
                pixels,
                0,
                portrait.width,
                0,
                0,
                portrait.width,
                portrait.height,
            )
            val visiblePixels = pixels.count { Color.alpha(it) > 16 }
            val transparentPixels = pixels.size - visiblePixels

            assertTrue(
                "$characterId portrait rendered blank",
                visiblePixels > 128,
            )
            assertTrue(
                "$characterId portrait should preserve a transparent canvas",
                transparentPixels > 0,
            )
        }
    }
}
