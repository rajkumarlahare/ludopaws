package com.ludoproof.game.feature.store.presentation.components

import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import com.ludoproof.game.core.ui.art.plywoodBackground
import com.ludoproof.game.core.ui.art.recessedPlywoodBackground
import com.ludoproof.game.feature.store.presentation.StoreActivity

/**
 * Store card surface that keeps the shared plywood rim visible around a tinted
 * arcade face, preserving readable, category-specific card colours.
 */
internal fun StoreActivity.storePlywoodCardBackground(
    cornerRadiusDp: Float,
    fillColors: IntArray,
    strokeColor: Int,
    strokeWidthDp: Int = 1,
    insetDp: Int = 5,
): Drawable {
    val wood = plywoodBackground(this, cornerRadiusDp)
    val density = resources.displayMetrics.density
    val face =
        GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            fillColors,
        ).apply {
            cornerRadius =
                (cornerRadiusDp - insetDp).coerceAtLeast(4f) * density
            setStroke(dp(strokeWidthDp), strokeColor)
        }
    val inset = dp(insetDp)
    return LayerDrawable(arrayOf(wood, face)).apply {
        setLayerInset(1, inset, inset, inset, inset)
    }
}

/**
 * Recessed plywood pocket for item artwork. A translucent colour wash may be
 * layered inside the cavity so each cosmetic keeps its own palette and the
 * wooden bevel and grain remain visible.
 */
internal fun StoreActivity.storeRecessedSurfaceBackground(
    cornerRadiusDp: Float,
    fillColors: IntArray? = null,
    strokeColor: Int = 0x885FE1FF.toInt(),
    strokeWidthDp: Int = 1,
    insetDp: Int = 7,
): Drawable {
    val wood = recessedPlywoodBackground(this, cornerRadiusDp)
    if (fillColors == null) return wood
    val density = resources.displayMetrics.density
    val face =
        GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            fillColors,
        ).apply {
            cornerRadius =
                (cornerRadiusDp - insetDp).coerceAtLeast(4f) * density
            setStroke(dp(strokeWidthDp), strokeColor)
        }
    val inset = dp(insetDp)
    return LayerDrawable(arrayOf(wood, face)).apply {
        setLayerInset(1, inset, inset, inset, inset)
    }
}
