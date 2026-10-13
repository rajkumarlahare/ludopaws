package com.rekixo.ludopaws.core.ui.theme

/**
 * Semantic dimensions shared by native Views and Canvas-based UI.
 *
 * Colors continue to live in [com.rekixo.ludopaws.LudoProofTheme] so existing
 * feature palettes remain the source of truth. These values define a common
 * spacing, shape, icon-size and outline-stroke vocabulary without forcing
 * feature-specific artwork onto one visual style.
 */
internal object AppUiTokens {
    object Spacing {
        const val XXS_DP = 4
        const val XS_DP = 8
        const val SM_DP = 12
        const val MD_DP = 16
        const val LG_DP = 20
        const val XL_DP = 24
        const val XXL_DP = 32
    }

    object Shape {
        const val CONTROL_RADIUS_DP = 12f
        const val CARD_RADIUS_DP = 18f
        const val PILL_RADIUS_DP = 999f
    }

    object Icon {
        const val SMALL_DP = 16
        const val STANDARD_DP = 24
        const val LARGE_DP = 32
        const val ACTION_CONTENT_DP = 34
        const val MIN_TOUCH_TARGET_DP = 48

        const val NORMAL_STROKE_DP = 2.5f
        const val EMPHASIS_STROKE_DP = 4f
    }
}
