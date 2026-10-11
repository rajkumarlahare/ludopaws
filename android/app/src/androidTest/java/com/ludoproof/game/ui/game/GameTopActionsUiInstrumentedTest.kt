package com.ludoproof.game.ui.game

import android.content.Context
import android.graphics.drawable.StateListDrawable
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ludoproof.game.core.ui.icons.AppIconName
import com.ludoproof.game.core.ui.icons.AppIconView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.platform.app.InstrumentationRegistry

@RunWith(AndroidJUnit4::class)
class GameTopActionsUiInstrumentedTest {
    @Test
    fun offlineTopActionsUseSemanticIconsAndMatteFocusableChrome() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val host = FrameLayout(context)
            var backClicks = 0
            var settingsClicks = 0

            val bar = addGameTopActions(
                host = host,
                context = context,
                onBack = { backClicks += 1 },
                onSettings = { settingsClicks += 1 },
                style = GameTopActionsStyle.OFFLINE_GAMEPLAY,
            )

            assertEquals(2, bar.childCount)
            val backButton = bar.getChildAt(0) as FrameLayout
            val settingsButton = bar.getChildAt(1) as FrameLayout

            assertEquals("Back", backButton.contentDescription)
            assertEquals("Game settings", settingsButton.contentDescription)
            assertTrue(backButton.isFocusable)
            assertTrue(settingsButton.isFocusable)
            assertTrue(backButton.background is StateListDrawable)
            assertTrue(settingsButton.background is StateListDrawable)
            assertNotNull(backButton.foreground)
            assertNotNull(settingsButton.foreground)
            assertEquals(AppIconName.BACK_ARROW, (backButton.getChildAt(0) as AppIconView).iconName)
            assertEquals(AppIconName.SETTINGS, (settingsButton.getChildAt(0) as AppIconView).iconName)

            backButton.performClick()
            settingsButton.performClick()
            assertEquals(1, backClicks)
            assertEquals(1, settingsClicks)
        }
    }
}
