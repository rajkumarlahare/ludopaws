package com.ludoproof.game.core.ui.components

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import com.ludoproof.game.LudoProofTheme
import com.ludoproof.game.core.ui.icons.AppIconName
import com.ludoproof.game.core.ui.icons.AppIconView
import com.ludoproof.game.core.ui.theme.AppUiTokens
import com.ludoproof.game.ui.home.HomeGlassShape
import com.ludoproof.game.ui.home.homeGlassBackground

/**
 * Shared circular action chrome for navigation and utility actions.
 *
 * Screen-specific code supplies the semantic icon, accessible label and callback;
 * surface, icon sizing, focus treatment and hit-target geometry stay consistent.
 */
internal fun appIconActionButton(
    context: Context,
    contentDescription: String,
    iconName: AppIconName,
    sizeDp: Int = 52,
    onClick: () -> Unit,
): FrameLayout =
    FrameLayout(context).apply {
        isClickable = true
        isFocusable = true
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        this.contentDescription = contentDescription
        background = homeGlassBackground(
            context = context,
            shape = HomeGlassShape.CIRCLE,
        )
        elevation = LudoProofTheme.dp(context, 7).toFloat()
        setOnClickListener { onClick() }

        val iconSizeDp = minOf(
            AppUiTokens.Icon.ACTION_CONTENT_DP,
            sizeDp - AppUiTokens.Spacing.MD_DP,
        ).coerceAtLeast(AppUiTokens.Icon.STANDARD_DP)
        addView(
            AppIconView(
                context = context,
                iconName = iconName,
                iconTint = Color.WHITE,
                strokeWidthDp = AppUiTokens.Icon.EMPHASIS_STROKE_DP,
            ),
            FrameLayout.LayoutParams(
                LudoProofTheme.dp(context, iconSizeDp),
                LudoProofTheme.dp(context, iconSizeDp),
                Gravity.CENTER,
            ),
        )
        AppFocusRing.apply(
            this,
            cornerRadiusDp = AppUiTokens.Shape.PILL_RADIUS_DP,
            strokeWidthDp = AppUiTokens.Icon.NORMAL_STROKE_DP,
            color = LudoProofTheme.GOLD,
        )
    }
