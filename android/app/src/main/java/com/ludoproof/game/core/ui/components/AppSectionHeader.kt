package com.ludoproof.game.core.ui.components

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.ludoproof.game.LudoProofTheme
import com.ludoproof.game.core.ui.theme.AppUiTokens

/** Standard title/subtitle stack for sections; callers can still add their own trailing actions. */
internal object AppSectionHeader {
    fun create(
        context: Context,
        title: CharSequence,
        subtitle: CharSequence? = null,
        titleColor: Int = LudoProofTheme.WHITE,
    ): LinearLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL

            addView(
                TextView(context).apply {
                    text = title
                    textSize = 20f
                    setTextColor(titleColor)
                    setTypeface(typeface, Typeface.BOLD)
                    gravity = Gravity.START or Gravity.CENTER_VERTICAL
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
                },
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ),
            )

            subtitle?.takeIf { it.isNotBlank() }?.let { supportingText ->
                val gap = LudoProofTheme.dp(context, AppUiTokens.Spacing.XXS_DP)
                addView(
                    TextView(context).apply {
                        text = supportingText
                        textSize = 13f
                        setTextColor(LudoProofTheme.TEXT_MUTED)
                        gravity = Gravity.START
                    },
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        topMargin = gap
                    },
                )
            }
        }
}
