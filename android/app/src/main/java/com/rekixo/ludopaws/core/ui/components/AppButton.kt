package com.rekixo.ludopaws.core.ui.components

import android.content.Context
import android.widget.Button
import com.rekixo.ludopaws.LudoProofTheme
import com.rekixo.ludopaws.core.ui.theme.AppUiTokens

/** Semantic button treatments backed by the existing app theme. */
internal enum class AppButtonVariant {
    PRIMARY,
    SECONDARY,
    POSITIVE,
    DANGER,
}

/**
 * Creates a consistently sized native button without taking ownership of its
 * layout params or click behavior. Feature screens remain responsible for
 * placement, navigation, loading state and domain behavior.
 */
internal object AppButton {
    fun create(
        context: Context,
        label: CharSequence,
        variant: AppButtonVariant = AppButtonVariant.PRIMARY,
        enabled: Boolean = true,
        contentDescription: CharSequence? = null,
        onClick: (() -> Unit)? = null,
    ): Button =
        Button(context).apply {
            text = label
            isAllCaps = false
            isEnabled = enabled
            contentDescription?.let { this.contentDescription = it }

            when (variant) {
                AppButtonVariant.PRIMARY -> LudoProofTheme.primary(this)
                AppButtonVariant.SECONDARY -> LudoProofTheme.secondary(this)
                AppButtonVariant.POSITIVE -> LudoProofTheme.positive(this)
                AppButtonVariant.DANGER -> LudoProofTheme.danger(this)
            }

            val targetHeight = LudoProofTheme.dp(
                context,
                AppUiTokens.Icon.MIN_TOUCH_TARGET_DP,
            )
            minHeight = maxOf(minHeight, targetHeight)
            onClick?.let { callback -> setOnClickListener { callback() } }
        }
}
