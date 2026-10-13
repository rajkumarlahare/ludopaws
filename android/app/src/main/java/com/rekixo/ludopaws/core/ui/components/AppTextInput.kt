package com.rekixo.ludopaws.core.ui.components

import android.content.Context
import android.text.InputType
import android.widget.EditText
import com.rekixo.ludopaws.LudoProofTheme
import com.rekixo.ludopaws.core.ui.theme.AppUiTokens

/** Standard text entry styling; feature code still configures validation and text listeners. */
internal object AppTextInput {
    fun create(
        context: Context,
        hint: CharSequence? = null,
        inputType: Int = InputType.TYPE_CLASS_TEXT,
        singleLine: Boolean = true,
        contentDescription: CharSequence? = null,
    ): EditText =
        EditText(context).apply {
            this.hint = hint
            this.inputType = inputType
            setSingleLine(singleLine)
            contentDescription?.let { this.contentDescription = it }
            LudoProofTheme.input(this)
            minHeight = maxOf(
                minHeight,
                LudoProofTheme.dp(context, AppUiTokens.Icon.MIN_TOUCH_TARGET_DP),
            )
        }
}
