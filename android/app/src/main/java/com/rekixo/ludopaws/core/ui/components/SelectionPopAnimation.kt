package com.rekixo.ludopaws.core.ui.components

import android.view.View

/**
 * Reuses Online Match's short press-and-return feedback for selection controls
 * across the game setup and matchmaking screens.
 */
internal fun View.playSelectionPopAnimation(onComplete: (() -> Unit)? = null) {
    animate().cancel()
    scaleX = 1f
    scaleY = 1f
    animate()
        .scaleX(0.965f)
        .scaleY(0.965f)
        .setDuration(75L)
        .withEndAction {
            animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(135L)
                .withEndAction { onComplete?.invoke() }
                .start()
        }
        .start()
}
