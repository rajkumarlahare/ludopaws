package com.rekixo.ludopaws.core.ui.components

import android.content.Context
import android.widget.LinearLayout
import com.rekixo.ludopaws.LudoProofTheme
import com.rekixo.ludopaws.core.ui.theme.AppUiTokens

/** Panel surfaces reuse the existing theme so feature colors stay compatible. */
internal enum class AppPanelVariant {
    STANDARD,
    DARK,
    DARK_ACCENT,
}

internal object AppPanel {
    /**
     * Returns a vertical content container. Callers own child layout params and
     * should choose a feature-specific surface when its visual treatment is intentional.
     */
    fun create(
        context: Context,
        variant: AppPanelVariant = AppPanelVariant.STANDARD,
        paddingDp: Int = AppUiTokens.Spacing.MD_DP,
        elevationDp: Float = 0f,
    ): LinearLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val padding = LudoProofTheme.dp(context, paddingDp.coerceAtLeast(0))
            setPadding(padding, padding, padding, padding)
            background = when (variant) {
                AppPanelVariant.STANDARD -> LudoProofTheme.panelDrawable(context)
                AppPanelVariant.DARK -> LudoProofTheme.darkPanelDrawable(context)
                AppPanelVariant.DARK_ACCENT -> LudoProofTheme.darkPanelDrawable(
                    context,
                    goldBorder = true,
                )
            }
            elevation = LudoProofTheme.dp(context, elevationDp.coerceAtLeast(0f)).toFloat()
        }
}
