package com.ludoproof.game.ui.game

import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import com.ludoproof.game.LudoProofTheme
import com.ludoproof.game.core.ui.components.AppFocusRing
import com.ludoproof.game.core.ui.icons.AppIconName
import com.ludoproof.game.core.ui.icons.AppIconView
import com.ludoproof.game.core.ui.theme.AppUiTokens
import com.ludoproof.game.ui.home.HomeIconView

/**
 * Fixed top-level game controls.
 *
 * These controls deliberately live in the activity viewport rather than the
 * gameplay vertical flow, so board placement cannot move when the game layout
 * changes. Online surfaces retain their glass treatment; local gameplay can opt
 * into a matte, mode-safe action surface.
 */
internal enum class GameTopActionsStyle {
    SHARED_GLASS,
    OFFLINE_GAMEPLAY,
}

internal fun addGameTopActions(
    host: FrameLayout,
    context: android.content.Context,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    style: GameTopActionsStyle = GameTopActionsStyle.SHARED_GLASS,
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
                    iconName = if (style == GameTopActionsStyle.OFFLINE_GAMEPLAY) {
                        AppIconName.BACK
                    } else {
                        AppIconName.CHEVRON_LEFT
                    },
                    iconTint = android.graphics.Color.WHITE,
                    strokeWidthDp = AppUiTokens.Icon.EMPHASIS_STROKE_DP,
                ),
            style = style,
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
                if (style == GameTopActionsStyle.OFFLINE_GAMEPLAY) {
                    AppIconView(
                        context = context,
                        iconName = AppIconName.SETTINGS,
                        iconTint = android.graphics.Color.WHITE,
                        strokeWidthDp = AppUiTokens.Icon.NORMAL_STROKE_DP,
                    )
                } else {
                    HomeIconView(context).apply {
                        kind = com.ludoproof.game.ui.home.HomeIconKind.SETTINGS
                        iconColor = android.graphics.Color.WHITE
                        importantForAccessibility =
                            View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    }
                },
            style = style,
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
    style: GameTopActionsStyle,
    onClick: () -> Unit,
): FrameLayout =
    FrameLayout(context).apply {
        isClickable = true
        isFocusable = true
        this.contentDescription = contentDescription
        background = if (style == GameTopActionsStyle.OFFLINE_GAMEPLAY) {
            offlineGameplayActionBackground(context)
        } else {
            com.ludoproof.game.ui.home.homeGlassBackground(
                context = context,
                shape = com.ludoproof.game.ui.home.HomeGlassShape.CIRCLE,
            )
        }
        elevation = LudoProofTheme.dp(context, if (style == GameTopActionsStyle.OFFLINE_GAMEPLAY) 3 else 7).toFloat()
        stateListAnimator = null
        setOnClickListener { onClick() }
        if (style == GameTopActionsStyle.OFFLINE_GAMEPLAY) {
            AppFocusRing.apply(
                this,
                cornerRadiusDp = AppUiTokens.Shape.PILL_RADIUS_DP,
                strokeWidthDp = AppUiTokens.Icon.NORMAL_STROKE_DP,
                color = LudoProofTheme.GOLD,
            )
        }

        addView(
            icon,
            FrameLayout.LayoutParams(
                LudoProofTheme.dp(context, AppUiTokens.Icon.ACTION_CONTENT_DP),
                LudoProofTheme.dp(context, AppUiTokens.Icon.ACTION_CONTENT_DP),
                Gravity.CENTER,
            ),
        )
    }



/** Matte, high-contrast action chrome reserved for Pass & Play and Computer. */
private fun offlineGameplayActionBackground(context: android.content.Context): StateListDrawable =
    StateListDrawable().apply {
        addState(
            intArrayOf(android.R.attr.state_pressed),
            offlineGameplayActionCircle(
                context,
                fill = 0xFF293447.toInt(),
                stroke = LudoProofTheme.GOLD,
                strokeWidthDp = 2,
            ),
        )
        addState(
            intArrayOf(android.R.attr.state_focused),
            offlineGameplayActionCircle(
                context,
                fill = 0xFF172C3D.toInt(),
                stroke = LudoProofTheme.GOLD,
                strokeWidthDp = 3,
            ),
        )
        addState(
            intArrayOf(),
            offlineGameplayActionCircle(
                context,
                fill = 0xE5112638.toInt(),
                stroke = 0xFF5DD9F2.toInt(),
                strokeWidthDp = 1,
            ),
        )
    }

private fun offlineGameplayActionCircle(
    context: android.content.Context,
    fill: Int,
    stroke: Int,
    strokeWidthDp: Int,
): GradientDrawable =
    GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(fill)
        setStroke(LudoProofTheme.dp(context, strokeWidthDp), stroke)
    }
