package com.ludoproof.game.core.ui.components

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.StateListDrawable
import android.view.Gravity
import android.widget.FrameLayout
import com.ludoproof.game.LudoProofTheme
import com.ludoproof.game.core.ui.icons.AppIconName
import com.ludoproof.game.core.ui.icons.AppIconView
import com.ludoproof.game.core.ui.theme.AppUiTokens

/**
 * A button-sized accessible parent with a decorative shared icon child.
 * The visible label belongs on this parent; AppIconView remains hidden from
 * accessibility traversal to prevent duplicate/unlabeled announcements.
 */
internal object AppActionIconButton {
    fun create(
        context: Context,
        iconName: AppIconName,
        contentDescription: CharSequence,
        iconTint: Int = Color.WHITE,
        iconSizeDp: Int = AppUiTokens.Icon.STANDARD_DP,
        onClick: () -> Unit,
    ): FrameLayout {
        require(contentDescription.isNotBlank()) {
            "An action icon button requires a non-blank content description."
        }

        val button = FrameLayout(context)
        val normal = LudoProofTheme.rounded(
            0xCC0D4F9C.toInt(),
            AppUiTokens.Shape.CONTROL_RADIUS_DP,
            LudoProofTheme.CYAN_BORDER,
            1f,
            context,
        )
        val pressed = LudoProofTheme.rounded(
            0xFF062B68.toInt(),
            AppUiTokens.Shape.CONTROL_RADIUS_DP,
            LudoProofTheme.GOLD,
            1.5f,
            context,
        )

        button.background = StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_pressed), pressed)
            addState(intArrayOf(android.R.attr.state_focused), pressed)
            addState(intArrayOf(), normal)
        }
        button.contentDescription = contentDescription
        button.isClickable = true
        button.isFocusable = true
        button.minimumWidth = LudoProofTheme.dp(
            context,
            AppUiTokens.Icon.MIN_TOUCH_TARGET_DP,
        )
        button.minimumHeight = LudoProofTheme.dp(
            context,
            AppUiTokens.Icon.MIN_TOUCH_TARGET_DP,
        )
        button.setOnClickListener { onClick() }

        val iconSize = iconSizeDp.coerceIn(
            AppUiTokens.Icon.SMALL_DP,
            AppUiTokens.Icon.LARGE_DP,
        )
        button.addView(
            AppIconView(
                context = context,
                iconName = iconName,
                iconTint = iconTint,
            ),
            FrameLayout.LayoutParams(
                LudoProofTheme.dp(context, iconSize),
                LudoProofTheme.dp(context, iconSize),
                Gravity.CENTER,
            ),
        )
        return button
    }
}
