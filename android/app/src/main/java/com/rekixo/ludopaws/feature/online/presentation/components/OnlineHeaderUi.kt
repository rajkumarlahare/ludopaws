package com.rekixo.ludopaws.ui.online

import android.app.AlertDialog
import android.os.Build
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.window.OnBackInvokedDispatcher
import com.rekixo.ludopaws.*
import com.rekixo.ludopaws.feature.online.*

internal fun MainActivity.registerOnlineBackCallback() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        onBackInvokedDispatcher.registerOnBackInvokedCallback(
            OnBackInvokedDispatcher.PRIORITY_DEFAULT,
        ) {
            requestRemoteExit()
        }
    }
}

internal fun MainActivity.requestRemoteExit() {
    val hasUnfinishedMatch =
        !matchId.isNullOrBlank() &&
            currentState?.status != "FINISHED"
    val activeMatch =
        currentState?.status == "ACTIVE"

    if (!hasUnfinishedMatch) {
        if (abandonRemoteSessionState()) {
            finish()
        }
        return
    }

    AlertDialog.Builder(this)
        .setTitle(
            if (gameMode == GameMode.TEAM_UP) {
                "Leave Team Up?"
            } else {
                "Leave Match?"
            },
        )
        .setMessage(
            if (activeMatch && gameMode == GameMode.TEAM_UP) {
                "Leaving an active 2 vs 2 match forfeits your whole team. " +
                    "Cancel to stay in the match."
            } else if (activeMatch) {
                "Leaving attempts a server-side forfeit. Any ranked result is saved only after the server confirms the match is finished."
            } else if (gameMode == GameMode.TEAM_UP) {
                "Leave this Team Up room? If the match is already active, leaving attempts to forfeit your whole team."
            } else {
                "Your match is not confirmed as finished. Leaving attempts a server-side forfeit if it is active."
            },
        )
        .setNegativeButton("CANCEL", null)
        .setPositiveButton(
            if (gameMode == GameMode.TEAM_UP) {
                "EXIT TEAM"
            } else {
                "LEAVE MATCH"
            },
        ) { _, _ ->
            if (abandonRemoteSessionState()) {
                finish()
            }
        }
        .setCancelable(false)
        .show()
}

internal fun MainActivity.onlineHero(): FrameLayout =
    FrameLayout(this).apply {
        val heroHeight = dp(if (isCompactOnline()) 160 else 182)

        addView(
            OnlineLobbyArtView(this@onlineHero),
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                heroHeight,
            ),
        )

        val copy =
            LinearLayout(this@onlineHero).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(
                    dp(if (isCompactOnline()) 104 else 146),
                    dp(14),
                    dp(if (isCompactOnline()) 12 else 24),
                    dp(14),
                )

                addView(
                    TextView(this@onlineHero).apply {
                        text =
                            if (gameMode == GameMode.TEAM_UP) {
                                "VERIFIED TEAM UP"
                            } else {
                                "VERIFIED ONLINE"
                            }
                        LudoProofTheme.title(
                            this,
                            if (isCompactOnline()) 21f else 27f,
                            gold = true,
                        )
                        gravity = Gravity.START or Gravity.CENTER_VERTICAL
                    },
                )

                addView(
                    TextView(this@onlineHero).apply {
                        text =
                            if (gameMode == GameMode.TEAM_UP) {
                                "Red + Yellow vs Green + Blue"
                            } else {
                                "Create a room or join with a match code"
                            }
                        LudoProofTheme.body(
                            this,
                            if (isCompactOnline()) 11f else 12.5f,
                            bright = true,
                        )
                        gravity = Gravity.START
                        setPadding(0, dp(4), 0, 0)
                    },
                )

                addView(
                    TextView(this@onlineHero).apply {
                        text =
                            if (gameMode == GameMode.TEAM_UP) {
                                "Remote EntroNex authority • exit forfeits your team"
                            } else {
                                "Remote EntroNex authority • exit abandons this match"
                            }
                        LudoProofTheme.body(
                            this,
                            if (isCompactOnline()) 9.5f else 10.5f,
                        )
                        gravity = Gravity.START
                        setPadding(0, dp(3), 0, 0)
                    },
                )
            }

        addView(
            copy,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                heroHeight,
            ),
        )
    }
