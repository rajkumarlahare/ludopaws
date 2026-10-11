package com.ludoproof.game.feature.profile.presentation.components

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.ludoproof.game.LudoProofTheme
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
            GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(
                    0xF20B6DD2.toInt(),
                    0xF0074EAE.toInt(),
                ),
            ).apply {
                cornerRadius =
                    dp(15)
                        .toFloat()
                setStroke(
                    dp(1),
                    0xFF27BDF6.toInt(),
                )
            }
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
