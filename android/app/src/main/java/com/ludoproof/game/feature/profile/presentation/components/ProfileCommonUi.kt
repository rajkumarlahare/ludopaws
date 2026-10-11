package com.ludoproof.game.feature.profile.presentation.components

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.ludoproof.game.LudoProofTheme
import com.ludoproof.game.R
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
                    0xE81D72D1.toInt(),
                    0xE50D4CAA.toInt(),
                ),
            ).apply {
                cornerRadius =
                    dp(15)
                        .toFloat()
                setStroke(
                    dp(1),
                    0x8856D9FF.toInt(),
                )
            }
        elevation =
            dp(5)
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
            18f
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

internal fun ProfileActivity.profilePlayGamesPanel(): LinearLayout =
    profilePanel().apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(14), dp(12), dp(14), dp(12))

        val accountMark =
            TextView(this@profilePlayGamesPanel).apply {
                text = "G"
                textSize = 22f
                setTypeface(Typeface.DEFAULT_BOLD)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                background =
                    LudoProofTheme.rounded(
                        0xFF4285F4.toInt(),
                        999f,
                        Color.WHITE,
                        1f,
                        this@profilePlayGamesPanel,
                    )
            }
        addView(
            accountMark,
            LinearLayout.LayoutParams(dp(42), dp(42)),
        )

        val copy =
            LinearLayout(this@profilePlayGamesPanel).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), 0, 0, 0)
                addView(
                    TextView(this@profilePlayGamesPanel).apply {
                        text = getString(R.string.profile_play_games_title)
                        textSize = 13f
                        setTypeface(Typeface.DEFAULT_BOLD)
                        setTextColor(Color.WHITE)
                    },
                )
                addView(
                    TextView(this@profilePlayGamesPanel).apply {
                        text =
                            playGamesDisplayName
                                ?.takeIf { playGamesAuthenticated }
                                ?.let { "Signed in as $it" }
                                ?: playGamesStatus
                        textSize = 11f
                        setTextColor(0xFFD4E6FF.toInt())
                        maxLines = 2
                    },
                )
            }
        addView(
            copy,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
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
