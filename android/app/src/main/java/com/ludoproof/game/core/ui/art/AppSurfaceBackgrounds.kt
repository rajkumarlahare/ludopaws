package com.ludoproof.game.core.ui.art

import android.content.Context
import android.graphics.drawable.GradientDrawable
import com.ludoproof.game.LudoProofTheme

/**
 * Shared translucent navy surface for cards and sections across the app.
 * Defaults match the approved UI: #33104483 fill, #FF247FCB stroke, 18dp radius.
 */
internal fun transparentDarkBlueStrokeBackground(
    context: Context,
    cornerRadiusDp: Float = 18f,
): GradientDrawable =
    LudoProofTheme.rounded(
        0x33104483,
        cornerRadiusDp,
        0xFF247FCB.toInt(),
        1f,
        context,
    )

/**
 * Shared blue icon chip. Pass fill/stroke overrides only when a specific icon already uses
 * a slightly different approved blue; its shape and drawable construction remain centralized.
 */
internal fun blueIconBackground(
    context: Context,
    cornerRadiusDp: Float = 999f,
    fillColor: Int = 0xFF073F8C.toInt(),
    strokeColor: Int = 0xFF1C8BDA.toInt(),
    strokeWidthDp: Float = 1f,
): GradientDrawable =
    LudoProofTheme.rounded(
        fillColor,
        cornerRadiusDp,
        strokeColor,
        strokeWidthDp,
        context,
    )
