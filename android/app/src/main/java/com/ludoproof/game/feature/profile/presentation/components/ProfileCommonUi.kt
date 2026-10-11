package com.ludoproof.game.feature.profile.presentation.components

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.ludoproof.game.core.ui.art.PlywoodTextureDrawable
import com.ludoproof.game.feature.profile.presentation.ProfileActivity

internal fun ProfileActivity.profilePanel():
    LinearLayout =
    LinearLayout(this).apply {
        orientation =
            LinearLayout.VERTICAL
        setPadding(
            dp(14),
            dp(14),
            dp(14),
            dp(14),
        )
        background =
            PlywoodTextureDrawable(resources.displayMetrics.density)
        elevation =
            dp(4)
                .toFloat()
    }

internal fun ProfileActivity.profileTitle(
    text: String,
):
    TextView =
    TextView(this).apply {
        this.text =
            text
        textSize =
            20f
        setTypeface(
            Typeface.DEFAULT_BOLD,
        )
        setTextColor(
            Color.WHITE,
        )
    }

internal fun ProfileActivity.profileBody(
    text: String,
    size: Float = 13f,
):
    TextView =
    TextView(this).apply {
        this.text =
            text
        textSize =
            size
        setTextColor(
            0xFFE8F5FF.toInt(),
        )
    }

internal fun ProfileActivity.showNameEditor() {
    val current =
        profileStore
            .snapshot()
    val input =
        EditText(this).apply {
            setText(
                current.displayName,
            )
            selectAll()
            isSingleLine =
                true
            setPadding(
                dp(14),
                dp(8),
                dp(14),
                dp(8),
            )
        }

    AlertDialog
        .Builder(this)
        .setTitle(
            "Profile name",
        )
        .setView(
            input,
        )
        .setNegativeButton(
            "CANCEL",
            null,
        )
        .setPositiveButton(
            "SAVE",
        ) {
                _,
                _ ->
            profileStore
                .updateDisplayName(
                    input.text
                        .toString(),
                )
            renderProfile()
        }
        .show()
}

internal fun ProfileActivity.viewAllBadges(
    body: String,
) {
    AlertDialog
        .Builder(this)
        .setTitle(
            "Badges",
        )
        .setMessage(
            body,
        )
        .setPositiveButton(
            "CLOSE",
            null,
        )
        .show()
}

internal fun ProfileActivity.viewPurchases(
    body: String,
) {
    AlertDialog
        .Builder(this)
        .setTitle(
            "Purchased Products",
        )
        .setMessage(
            body,
        )
        .setPositiveButton(
            "CLOSE",
            null,
        )
        .show()
}
