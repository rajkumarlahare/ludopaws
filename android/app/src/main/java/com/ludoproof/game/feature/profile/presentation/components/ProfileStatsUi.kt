package com.ludoproof.game.feature.profile.presentation.components

import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.ludoproof.game.core.ui.art.classicModeIconBackground
import com.ludoproof.game.core.ui.art.classicStatsProgressTrackBackground
import com.ludoproof.game.core.ui.art.classicStatsProgressFillBackground
import com.ludoproof.game.core.ui.art.transparentDarkBlueStrokeBackground
import com.ludoproof.game.feature.profile.domain.model.ProfileModeStats
import com.ludoproof.game.feature.profile.domain.model.ProfileSnapshot
import com.ludoproof.game.feature.profile.presentation.ProfileActivity

internal fun ProfileActivity.profileModeStatsPanel(profile: ProfileSnapshot): LinearLayout =
    profilePanel().apply {
        val heading = LinearLayout(this@profileModeStatsPanel).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        heading.addView(
            profileTitle("Game Stats"),
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
        )
        heading.addView(
            TextView(this@profileModeStatsPanel).apply {
                text = "›"
                textSize = 34f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                contentDescription = "Classic game statistics"
            },
            LinearLayout.LayoutParams(dp(28), dp(34)),
        )
        addView(heading)

        addView(
            classicRow(profile.classic),
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(10) },
        )
    }

private fun ProfileActivity.classicRow(stats: ProfileModeStats): LinearLayout =
    LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(10), dp(10), dp(10), dp(10))
        background = transparentDarkBlueStrokeBackground(this@classicRow)

        addView(
            TextView(this@classicRow).apply {
                text = "▦"
                textSize = 28f
                setTypeface(Typeface.DEFAULT_BOLD)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                background = classicModeIconBackground(this@classicRow)
                contentDescription = "Classic"
            },
            LinearLayout.LayoutParams(dp(48), dp(48)),
        )

        val details = LinearLayout(this@classicRow).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
        }
        val top = LinearLayout(this@classicRow).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        top.addView(
            TextView(this@classicRow).apply {
                text = "Classic"
                textSize = 17f
                setTypeface(Typeface.DEFAULT_BOLD)
                setTextColor(Color.WHITE)
                maxLines = 1
            },
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
        )
        top.addView(
            TextView(this@classicRow).apply {
                text = "${stats.wins} wins · ${stats.games} games ›"
                textSize = 12.5f
                setTypeface(Typeface.DEFAULT_BOLD)
                setTextColor(Color.WHITE)
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
                maxLines = 1
            },
        )
        details.addView(top)

        val ratio = if (stats.games > 0) {
            (stats.wins.toFloat() / stats.games.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
        val track = FrameLayout(this@classicRow).apply {
            background = classicStatsProgressTrackBackground(this@classicRow)
            clipToOutline = true
        }
        val fill = View(this@classicRow).apply {
            background = classicStatsProgressFillBackground(this@classicRow)
        }
        track.addView(
            fill,
            FrameLayout.LayoutParams(0, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.START),
        )
        track.post {
            if (track.width > 0) {
                val params = fill.layoutParams as FrameLayout.LayoutParams
                params.width = (track.width * ratio).toInt().coerceIn(0, track.width)
                fill.layoutParams = params
            }
        }
        details.addView(
            track,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(10),
            ).apply { topMargin = dp(8) },
        )

        addView(
            details,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
        )
    }
