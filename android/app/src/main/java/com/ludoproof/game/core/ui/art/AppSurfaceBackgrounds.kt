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


/** Shared factory for the approved code-drawn plywood skin. */
internal fun plywoodBackground(
    context: Context,
    cornerRadiusDp: Float = 15f,
): android.graphics.drawable.Drawable =
    PlywoodTextureDrawable(context.resources.displayMetrics.density, cornerRadiusDp)

/** Shared factory for a curved, recessed plywood pocket for 3D character/art panels. */
internal fun recessedPlywoodBackground(
    context: Context,
    outerCornerRadiusDp: Float = 18f,
): android.graphics.drawable.Drawable =
    RecessedPlywoodDrawable(context.resources.displayMetrics.density, outerCornerRadiusDp)

/** Approved green/cyan Google Play sign-in action skin. */
internal fun playGamesSignInBackground(context: Context): GradientDrawable =
    LudoProofTheme.rounded(
        0xFF058A69.toInt(),
        13f,
        0xFF20E1C0.toInt(),
        1.5f,
        context,
    )

/** Dark blue track used under the player's XP progress. */
internal fun experienceProgressTrackBackground(context: Context): GradientDrawable =
    LudoProofTheme.rounded(
        0xFF052E78.toInt(),
        999f,
        0xFF3185D5.toInt(),
        1f,
        context,
    )

/** Gold gradient fill used by the player's XP progress. */
internal fun experienceProgressFillBackground(context: Context): GradientDrawable =
    GradientDrawable(
        GradientDrawable.Orientation.LEFT_RIGHT,
        intArrayOf(0xFFFFD83D.toInt(), 0xFFFFB812.toInt()),
    ).apply {
        cornerRadius = LudoProofTheme.dp(context, 999f).toFloat()
    }

/** Dark blue track used by a per-mode win-rate progress bar. */
internal fun classicStatsProgressTrackBackground(context: Context): GradientDrawable =
    LudoProofTheme.rounded(
        0xFF062E70.toInt(),
        999f,
        0xFF1E73BE.toInt(),
        1f,
        context,
    )

/** Gold gradient fill used by a per-mode win-rate progress bar. */
internal fun classicStatsProgressFillBackground(context: Context): GradientDrawable =
    GradientDrawable(
        GradientDrawable.Orientation.LEFT_RIGHT,
        intArrayOf(0xFFFFD93D.toInt(), 0xFFFFB91A.toInt()),
    ).apply {
        cornerRadius = LudoProofTheme.dp(context, 999f).toFloat()
    }

/** Translucent blue circular backing for the empty Purchased Products illustration. */
internal fun emptyPurchaseIconBackground(context: Context): GradientDrawable =
    GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(0x1C3DBEFF)
        setStroke(LudoProofTheme.dp(context, 1), 0x663EABF3)
    }
