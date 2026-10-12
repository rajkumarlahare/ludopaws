package com.ludoproof.game.ui.game

import android.content.ComponentName
import android.content.Context
import android.content.pm.ActivityInfo
import com.ludoproof.game.MainActivity
import com.ludoproof.game.OfflineGameActivity
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
    fun topActionsUseSharedSemanticIconsAndCommonChrome() {
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
            )

            assertEquals(2, bar.childCount)
            val backButton = bar.getChildAt(0) as FrameLayout
            val settingsButton = bar.getChildAt(1) as FrameLayout

            assertEquals("Back", backButton.contentDescription)
            assertEquals("Game settings", settingsButton.contentDescription)
            assertTrue(backButton.isClickable)
            assertTrue(settingsButton.isClickable)
            assertTrue(backButton.isFocusable)
            assertTrue(settingsButton.isFocusable)
            assertNotNull(backButton.background)
            assertNotNull(settingsButton.background)
            assertEquals("PlywoodTextureDrawable", backButton.background.javaClass.simpleName)
            assertEquals("PlywoodTextureDrawable", settingsButton.background.javaClass.simpleName)
            assertNotNull(backButton.foreground)
            assertNotNull(settingsButton.foreground)
            assertEquals(AppIconName.CHEVRON_LEFT, (backButton.getChildAt(0) as AppIconView).iconName)
            assertEquals(AppIconName.SETTINGS, (settingsButton.getChildAt(0) as AppIconView).iconName)

            backButton.performClick()
            settingsButton.performClick()
            assertEquals(1, backClicks)
            assertEquals(1, settingsClicks)
        }
    }
    @Suppress("DEPRECATION")
    @Test
    fun gameplayActivitiesStayPortraitToPreventRotationResettingTheMatch() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val packageManager = context.packageManager

        val offlineInfo = packageManager.getActivityInfo(
            ComponentName(context, OfflineGameActivity::class.java),
            0,
        )
        val onlineInfo = packageManager.getActivityInfo(
            ComponentName(context, MainActivity::class.java),
            0,
        )

        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, offlineInfo.screenOrientation)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, onlineInfo.screenOrientation)
    }

}
