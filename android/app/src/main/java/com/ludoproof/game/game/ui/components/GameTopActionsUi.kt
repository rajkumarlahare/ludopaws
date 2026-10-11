package com.ludoproof.game.ui.game

import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import com.ludoproof.game.LudoProofTheme
import com.ludoproof.game.core.ui.icons.AppIconName
import com.ludoproof.game.core.ui.icons.AppIconView
import com.ludoproof.game.core.ui.theme.AppUiTokens
import com.ludoproof.game.ui.home.HomeGlassShape
import com.ludoproof.game.ui.home.homeGlassBackground
import com.ludoproof.game.ui.home.HomeIconKind
import com.ludoproof.game.ui.home.HomeIconView

/**
 * Fixed top-level game controls.
 *
 * These controls deliberately live in the activity viewport rather than the
 * gameplay vertical flow, so board placement cannot move when the game layout
 * changes. The visual chrome reuses the Home glass treatment.
 */
internal fun addGameTopActions(
    host: FrameLayout,
    context: android.content.Context,
    onBack: () -> Unit,
    onSettings: () -> Unit,
): FrameLayout {
    val bar =
        FrameLayout(context).apply {
            clipChildren = false
            clipToPadding = false
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        }

    bar.addView(
        gameTopButton(
            context = context,
            contentDescription = "Back",
            icon =
                AppIconView(
                    context = context,
                    iconName = AppIconName.CHEVRON_LEFT,
                    iconTint = android.graphics.Color.WHITE,
                    strokeWidthDp = AppUiTokens.Icon.EMPHASIS_STROKE_DP,
                ),
            onClick = onBack,
        ),
        FrameLayout.LayoutParams(
            LudoProofTheme.dp(context, 52),
            LudoProofTheme.dp(context, 52),
            Gravity.TOP or Gravity.START,
        ).apply {
            leftMargin = LudoProofTheme.dp(context, 16)
            topMargin = LudoProofTheme.dp(context, 14)
        },
    )

    bar.addView(
        gameTopButton(
            context = context,
            contentDescription = "Game settings",
            icon =
                HomeIconView(context).apply {
                    kind = HomeIconKind.SETTINGS
                    iconColor = android.graphics.Color.WHITE
                    importantForAccessibility =
                        View.IMPORTANT_FOR_ACCESSIBILITY_NO
                },
            onClick = onSettings,
        ),
        FrameLayout.LayoutParams(
            LudoProofTheme.dp(context, 52),
            LudoProofTheme.dp(context, 52),
            Gravity.TOP or Gravity.END,
        ).apply {
            rightMargin = LudoProofTheme.dp(context, 16)
            topMargin = LudoProofTheme.dp(context, 14)
        },
    )

    host.addView(
        bar,
        FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        ),
    )
    return bar
}

private fun gameTopButton(
    context: android.content.Context,
    contentDescription: String,
    icon: View,
    onClick: () -> Unit,
): FrameLayout =
    FrameLayout(context).apply {
        isClickable = true
        isFocusable = true
        this.contentDescription = contentDescription
        background =
            homeGlassBackground(
                context = context,
                shape = HomeGlassShape.CIRCLE,
            )
        elevation = LudoProofTheme.dp(context, 7).toFloat()
        setOnClickListener { onClick() }

        addView(
            icon,
            FrameLayout.LayoutParams(
                LudoProofTheme.dp(context, AppUiTokens.Icon.ACTION_CONTENT_DP),
                LudoProofTheme.dp(context, AppUiTokens.Icon.ACTION_CONTENT_DP),
                Gravity.CENTER,
            ),
        )
    }

