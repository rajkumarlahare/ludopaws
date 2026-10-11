package com.ludoproof.game.core.ui.components

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.widget.TextView
import com.ludoproof.game.LudoProofTheme
import com.ludoproof.game.core.ui.theme.AppUiTokens

/** Semantic status tones; use feature-owned artwork for branded badges. */
internal enum class AppBadgeTone {
    INFO,
    SUCCESS,
    WARNING,
    DANGER,
    NEUTRAL,
}

internal class AppBadgeView(
    context: Context,
    label: CharSequence,
    tone: AppBadgeTone = AppBadgeTone.INFO,
) : TextView(context) {
    init {
        text = label
        textSize = 12f
        setTypeface(typeface, Typeface.BOLD)
        gravity = Gravity.CENTER
        maxLines = 1
        setPadding(
            LudoProofTheme.dp(context, AppUiTokens.Spacing.SM_DP),
            LudoProofTheme.dp(context, AppUiTokens.Spacing.XXS_DP),
            LudoProofTheme.dp(context, AppUiTokens.Spacing.SM_DP),
            LudoProofTheme.dp(context, AppUiTokens.Spacing.XXS_DP),
        )
        minimumHeight = LudoProofTheme.dp(context, 24)

        val (fill, foreground, border) = when (tone) {
            AppBadgeTone.INFO -> Triple(0x3320C9FF, 0xFFBCEFFF.toInt(), LudoProofTheme.CYAN_BORDER)
            AppBadgeTone.SUCCESS -> Triple(0x3335C83D, 0xFFBDF8B8.toInt(), LudoProofTheme.GREEN)
            AppBadgeTone.WARNING -> Triple(0x33FFC62E, 0xFFFFE59B.toInt(), LudoProofTheme.GOLD)
            AppBadgeTone.DANGER -> Triple(0x33F22E35, 0xFFFFC1C4.toInt(), LudoProofTheme.RED)
            AppBadgeTone.NEUTRAL -> Triple(0x33D4E6FF, LudoProofTheme.TEXT_MUTED, 0x669CBDEB)
        }
        setTextColor(foreground)
        background = LudoProofTheme.rounded(
            fill.toInt(),
            AppUiTokens.Shape.PILL_RADIUS_DP,
            border,
            1f,
            context,
        )
        isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
    }
}
