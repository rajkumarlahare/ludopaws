package com.rekixo.ludopaws.feature.online

import android.view.View
import com.rekixo.ludopaws.*
import com.rekixo.ludopaws.feature.profile.data.local.ProfileStore
import com.rekixo.ludopaws.feature.profile.domain.model.ProfileGameMode
import com.rekixo.ludopaws.feature.profile.domain.model.ProfileMatchSource
import org.json.JSONObject

internal fun MainActivity.applyResponse(
    response: JSONObject,
    announce: Boolean = true,
    silentValidationFailure: Boolean = false,
) {
    val source =
        if (
            response.optBoolean(
                CachedMatchStore.CACHE_SOURCE_MARKER,
                false,
            )
        ) {
            OnlineStateSource.CACHE
        } else {
            OnlineStateSource.AUTHORITATIVE
        }

    val envelope =
        try {
            GameJson.envelope(response)
        } catch (error: GameSchemaException) {
            // Passive refreshes must not rerender controls or disturb a live roll.
            if (silentValidationFailure) return
            diceView.stopRolling()
            currentState?.let { safeState ->
                updateControls(safeState)
            }
            showStatus(
                "Server state validation failed. Your last verified state was kept; refresh to retry.",
            )
            updateRollButton()
            return
        }

    val state = envelope.state
    if (state == null) {
        showStatus(response.toString(2))
        updateRollButton()
        return
    }

    if (
        source == OnlineStateSource.CACHE &&
        !OnlineCachedStateRestorePolicy.shouldApply(
            sessionMatchId = matchId,
            cachedMatchId = state.matchId,
        )
    ) {
        cachedMatchStore.clear()
        updateRollButton()
        return
    }

    val previousState = currentState
    val currentStateIsAuthoritative =
        uiStateHolder.value.currentStateSource == OnlineStateSource.AUTHORITATIVE
    val incomingBehindRealtime =
        source == OnlineStateSource.AUTHORITATIVE &&
            currentStateIsAuthoritative &&
            previousState?.matchId == state.matchId &&
            state.revision < lastRealtimeRevision.toLong()
    if (incomingBehindRealtime) {
        // A WebSocket event may announce a revision before the state-fetch reply
        // arrives. Do not render the older reply; fetch again for the latest state.
        mainHandler.removeCallbacks(realtimeRefreshRunnable)
        mainHandler.postDelayed(realtimeRefreshRunnable, 120L)
        return
    }

    if (
        source == OnlineStateSource.AUTHORITATIVE &&
        !OnlineSnapshotRevisionPolicy.shouldApply(
            currentMatchId = previousState?.matchId,
            currentRevision = previousState?.revision,
            currentIsAuthoritative = currentStateIsAuthoritative,
            incomingMatchId = state.matchId,
            incomingRevision = state.revision,
        )
    ) {
        return
    }

    envelope.playerId?.let { resolvedPlayerId ->
        playerId = resolvedPlayerId
        val code = matchId
        val token = playerToken
        if (code != null && token != null) {
            persistSessionSecurely(
                code = code,
                id = resolvedPlayerId,
                token = token,
            )
        }
    }

    currentState = state
    applyOnlineGameplayPacing(previousState, state)
    LudoProofTheme.setGameplayBackground(
        root = arcadeRootView,
        enabled = state.status == "ACTIVE",
    )
    uiStateHolder.update {
        it.copy(
            currentStateSource = source,
        )
    }
    matchId = state.matchId

    // A successful matchmaking response also carries the authoritative room state.
    // Switch from the matchmaking lobby to the actual match UI here; rendering the
    // board alone is not enough because boardFrame starts GONE and the lobby remains visible.
    lobbyPanel.visibility = View.GONE
    matchStatusPanel.visibility = View.VISIBLE
    boardFrame.visibility = View.VISIBLE
    actionPanel.visibility = View.VISIBLE
    resultPanel.visibility = if (state.status == "FINISHED") View.VISIBLE else View.GONE

    val serverRevision =
        response.optJSONObject("state")
            ?.optInt("revision", -1)
            ?: response.optInt("revision", -1)
    if (serverRevision > lastRealtimeRevision) {
        lastRealtimeRevision = serverRevision
    }

    if (state.status == "FINISHED") {
        stopFriendsVoice("Voice ended with the match.")
        realtimeClient.disconnect()
        realtimeConnected = false
        updateConnectionLabel()
    }

    if (
        source == OnlineStateSource.AUTHORITATIVE &&
        state.status == "FINISHED" &&
        gameMode.ranked
    ) {
        runCatching {
            ProfileStore(this).recordCompletedMatch(
                matchId = state.matchId,
                mode = ProfileGameMode.CLASSIC,
                source = ProfileMatchSource.ONLINE,
                won = state.winnerPlayerId == playerId,
            )
        }
    }

    if (source == OnlineStateSource.AUTHORITATIVE) {
        response.optJSONObject("state")?.let { safeState ->
            cachedMatchStore.save(
                envelope.playerId ?: playerId,
                safeState,
            )
        }
    }

    reconcilePendingSecret(
        state = state,
        source = source,
    )

    boardView.bind(state, playerId)
    val presentationOverride = onlinePresentationRendererOverrideForTests
    if (presentationOverride != null) {
        presentationOverride(previousState, state)
    } else {
        OnlineLudoPawsPresentation.render(
            activity = this,
            previous = previousState,
            current = state,
        )
    }

    matchInfoText.text =
        buildString {
            append("MATCH • ")
            append(state.matchId)
            append("   •   ")
            append(state.status)
            if (state.matchMode == "TEAM_UP") {
                append("   •   2 VS 2")
            }
            if (state.rulesetId.isNotBlank()) {
                append("   •   ")
                append(state.rulesetId)
            }
        }

    playersText.text =
        state.players.joinToString(separator = "\n") { player ->
            val marker = if (player.playerId == playerId) "  •  YOU" else ""
            val team =
                if (state.matchMode == "TEAM_UP" && player.teamId != null) {
                    "  •  TEAM ${player.teamId}"
                } else {
                    ""
                }
            player.color + "  •  " + player.displayName + team + marker
        }

    updateTurnBanner(state)
    updateVerification(state)
    proofDetailsText.text = proofDetails(state)
    updateControls(state)
    updateRollButton()

    val transitionMessage =
        OnlineTurnTransitionFeedbackPolicy.message(
            previous = previousState,
            current = state,
        )

    if (secureSessionPersistenceFailed) {
        showStatus(
            "Match is connected, but secure resume could not be saved. Keep this screen open until the match is finished.",
        )
    } else if (transitionMessage != null) {
        showStatus(transitionMessage)
    } else if (announce) {
        showStatus(
            if (state.status == "WAITING") {
                "Room synced • waiting for all required players."
            } else {
                "State revision updated. Event index: " + state.randomEventIndex
            },
        )
    }

    val wireState = response.optJSONObject("state") ?: response
    val timeoutNotice = wireState.optJSONObject("turnTimeoutNotice")
    val timeoutPlayerId = timeoutNotice?.optString("playerId").orEmpty()
    val timeoutCount = timeoutNotice?.optInt("count", 0) ?: 0
    val timeoutAt = timeoutNotice?.optLong("at", 0L) ?: 0L
    if (
        source == OnlineStateSource.AUTHORITATIVE &&
        timeoutPlayerId.isNotBlank() &&
        timeoutCount > 0 &&
        timeoutAt > lastTurnTimeoutNoticeAt
    ) {
        lastTurnTimeoutNoticeAt = timeoutAt
        val displayName = state.players.firstOrNull {
            it.playerId == timeoutPlayerId
        }?.displayName ?: "Player"
        showStatus(
            if (timeoutCount >= 4) {
                "${displayName} was removed after four missed dice turns."
            } else {
                "${displayName} did not roll in 15 seconds • warning ${timeoutCount}/3 • turn passed."
            },
        )
    }

    val wirePlayers = wireState.optJSONArray("players")
    val localPlayerWasForfeited = wirePlayers != null && (0 until wirePlayers.length()).any { index ->
        val wirePlayer = wirePlayers.optJSONObject(index)
        wirePlayer != null &&
            wirePlayer.optString("playerId") == playerId &&
            wirePlayer.optBoolean("forfeited", false)
    }
    if (
        source == OnlineStateSource.AUTHORITATIVE &&
        localPlayerWasForfeited &&
        !timeoutExitScheduled
    ) {
        timeoutExitScheduled = true
        showStatus("You missed four dice turns and were removed from this match.")
        mainHandler.postDelayed(
            {
                if (canRenderUi()) {
                    secureSessionStore.clear()
                    pendingRollStore.clear()
                    cachedMatchStore.clear()
                    publicMatchmakingStore.clear()
                    matchId = null
                    playerToken = null
                    playerId = null
                    currentState = null
                    pendingSecret = null
                    realtimeConnected = false
                    realtimeClient.disconnect()
                    LudoProofTheme.setGameplayBackground(root = arcadeRootView, enabled = false)
                    finish()
                }
            },
            1_800L,
        )
    }
}

internal fun MainActivity.reconcilePendingSecret(
    state: MatchSnapshot,
    source: OnlineStateSource,
) {
    val secret = pendingSecret ?: return
    if (
        OnlinePendingRollRecoveryPolicy.action(
            source = source,
            secret = secret,
            state = state,
        ) == PendingRollSecretAction.CLEAR
    ) {
        pendingRollStore.clear()
        pendingSecret = null
    }
}

internal fun MainActivity.updateTurnBanner(
    state: MatchSnapshot,
) {
    when (state.status) {
        "WAITING" -> {
            turnText.text =
                if (
                    state.targetPlayerCount != null &&
                    state.players.size < state.targetPlayerCount
                ) {
                    "WAITING FOR PLAYERS • ${state.players.size}/${state.targetPlayerCount}"
                } else if (state.players.size < 2) {
                    "WAITING FOR PLAYER"
                } else if (state.hostPlayerId == playerId) {
                    "READY • START THE MATCH"
                } else {
                    "READY • WAITING FOR HOST"
                }
        }

        "FINISHED" -> {
            turnText.text = "MATCH COMPLETE"
            if (state.matchMode == "TEAM_UP") {
                val me = state.players.find { it.playerId == playerId }
                val winnerTeam = state.winnerTeamId
                val won = winnerTeam != null && me?.teamId == winnerTeam
                val winners =
                    state.players
                        .filter { it.teamId == winnerTeam }
                        .joinToString(" + ") { it.displayName }
                        .ifBlank { "Team ${winnerTeam ?: "—"}" }

                resultTitleText.text =
                    if (won) "YOUR TEAM WON" else "TEAM ${winnerTeam ?: "—"} WINS"
                resultSubtitleText.text =
                    "$winners • server-authoritative team result • verified history available"
            } else {
                val winner = state.players.find { it.playerId == state.winnerPlayerId }
                val winnerName = winner?.displayName ?: "Player"
                resultTitleText.text =
                    if (state.players.size == 4) "MATCH COMPLETE"
                    else if (winner?.playerId == playerId) "YOU WON"
                    else "WINNER • $winnerName"
                resultSubtitleText.text =
                    if (state.players.size == 4 && state.finishOrderPlayerIds.isNotEmpty()) {
                        "Final placements are shown on the board."
                    } else if (winner?.playerId == playerId) {
                        "$winnerName • server-authoritative result • proof history available"
                    } else {
                        "Server-authoritative result • verified history available"
                    }
            }
        }

        else -> {
            val activeSeat = state.actingSeat ?: state.turnSeat
            val active = state.players.getOrNull(activeSeat)
            val mine = active?.playerId == playerId
            val handoff =
                state.matchMode == "TEAM_UP" &&
                    state.actingSeat != null &&
                    state.actingSeat != state.turnSeat
            turnText.text =
                if (mine) {
                    if (handoff) "YOUR TEAM TURN • PARTNER HANDOFF"
                    else "YOUR TURN • ROLL OR MOVE"
                } else {
                    val prefix = if (handoff) "TEAM HANDOFF • " else "WAITING • "
                    prefix + (active?.displayName ?: "Player") + "'S TURN"
                }
        }
    }
}
