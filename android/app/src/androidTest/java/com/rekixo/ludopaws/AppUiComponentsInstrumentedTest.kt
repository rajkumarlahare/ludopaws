package com.rekixo.ludopaws

import android.content.Context
import android.text.InputType
import android.view.View
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rekixo.ludopaws.core.ui.components.AppActionIconButton
import com.rekixo.ludopaws.core.ui.components.AppBadgeTone
import com.rekixo.ludopaws.core.ui.components.AppBadgeView
import com.rekixo.ludopaws.core.ui.components.AppButton
import com.rekixo.ludopaws.core.ui.components.AppButtonVariant
import com.rekixo.ludopaws.core.ui.components.AppPanel
import com.rekixo.ludopaws.core.ui.components.AppPanelVariant
import com.rekixo.ludopaws.core.ui.components.AppSectionHeader
import com.rekixo.ludopaws.core.ui.components.AppTextInput
import com.rekixo.ludopaws.core.ui.icons.AppIconName
import com.rekixo.ludopaws.core.ui.theme.AppUiTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppUiComponentsInstrumentedTest {
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private val minimumTouchTargetPx: Int
        get() = LudoProofTheme.dp(context, AppUiTokens.Icon.MIN_TOUCH_TARGET_DP)

    @Test
    fun buttonUsesThemeAndHonorsEnabledAndClickContracts() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            var clickCount = 0
            val button = AppButton.create(
                context = context,
                label = "Continue",
                variant = AppButtonVariant.SECONDARY,
                onClick = { clickCount++ },
            )

            assertEquals("Continue", button.text.toString())
            assertTrue(button.isEnabled)
            assertTrue(button.minHeight >= minimumTouchTargetPx)
            assertTrue(button.performClick())
            assertEquals(1, clickCount)

            val disabled = AppButton.create(
                context = context,
                label = "Unavailable",
                enabled = false,
            )
            assertFalse(disabled.isEnabled)
        }
    }

    @Test
    fun actionIconButtonExposesLabelOnParentAndKeepsIconDecorative() {
        var activated = false
        val button = AppActionIconButton.create(
            context = context,
            iconName = AppIconName.CLOSE,
            contentDescription = "Close dialog",
            onClick = { activated = true },
        )

        assertEquals("Close dialog", button.contentDescription.toString())
        assertTrue(button.isClickable)
        assertTrue(button.isFocusable)
        assertTrue(button.minimumWidth >= minimumTouchTargetPx)
        assertTrue(button.minimumHeight >= minimumTouchTargetPx)
        assertEquals(
            View.IMPORTANT_FOR_ACCESSIBILITY_NO,
            button.getChildAt(0).importantForAccessibility,
        )
        assertTrue(button.performClick())
        assertTrue(activated)
    }

    @Test
    fun panelProvidesVerticalContainerAndNonNegativePadding() {
        val panel = AppPanel.create(
            context = context,
            variant = AppPanelVariant.DARK_ACCENT,
            paddingDp = -4,
        )

        assertEquals(android.widget.LinearLayout.VERTICAL, panel.orientation)
        assertEquals(0, panel.paddingLeft)
        assertEquals(0, panel.paddingTop)
        assertEquals(0, panel.paddingRight)
        assertEquals(0, panel.paddingBottom)
    }

    @Test
    fun badgeAndSectionHeaderExposeTheirVisibleText() {
        val badge = AppBadgeView(context, "Online", AppBadgeTone.SUCCESS)
        assertEquals("Online", badge.text.toString())
        assertTrue(badge.minimumHeight > 0)

        val header = AppSectionHeader.create(
            context = context,
            title = "Friends",
            subtitle = "Manage your contacts",
        )
        assertEquals(2, header.childCount)
        assertEquals("Friends", (header.getChildAt(0) as android.widget.TextView).text)
        assertEquals(
            "Manage your contacts",
            (header.getChildAt(1) as android.widget.TextView).text,
        )
    }

    @Test
    fun textInputKeepsHintAndMeetsMinimumTouchHeight() {
        val input = AppTextInput.create(
            context = context,
            hint = "Search players",
            inputType = InputType.TYPE_CLASS_TEXT,
        )

        assertEquals("Search players", input.hint.toString())
        assertTrue(input.minHeight >= minimumTouchTargetPx)
    }
}
