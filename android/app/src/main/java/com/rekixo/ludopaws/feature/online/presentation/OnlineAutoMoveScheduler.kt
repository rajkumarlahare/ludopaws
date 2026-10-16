package com.rekixo.ludopaws.feature.online

import android.os.SystemClock
import com.rekixo.ludopaws.MainActivity
import com.rekixo.ludopaws.MatchSnapshot
import com.rekixo.ludopaws.OnlineStateSource

internal fun MainActivity.scheduleOnlineAutoMove(snapshot: MatchSnapshot) {
    val authoritative =
        uiStateHolder.value.currentStateSource == OnlineStateSource.AUTHORITATIVE
    val decision = OnlineAutoMovePolicy.resolve(
        state = snapshot,
        localPlayerId = playerId,
        isOnline = isOnline,
        hasAuthoritativeState = authoritative,
    )
    if (decision == null) {
        cancelScheduledOnlineAutoMove()
        return
    }
    if (scheduledAutoMoveEventKey == decision.eventKey) return
    if (
        lastAutoMoveAttemptEventKey == decision.eventKey &&
        autoMoveAttemptCount >= 2
    ) {
        cancelScheduledOnlineAutoMove()
        return
    }

    cancelScheduledOnlineAutoMove()
    scheduledAutoMoveEventKey = decision.eventKey
    val task = object : Runnable {
        override fun run() {
            if (scheduledAutoMoveEventKey != decision.eventKey) return
            if (!canRenderUi() || !screenStarted) {
                cancelScheduledOnlineAutoMove()
                return
            }
            val current = currentState
            val latestDecision = current?.let {
                OnlineAutoMovePolicy.resolve(
                    state = it,
                    localPlayerId = playerId,
                    isOnline = isOnline,
                    hasAuthoritativeState =
                        uiStateHolder.value.currentStateSource == OnlineStateSource.AUTHORITATIVE,
                )
            }
            if (
                latestDecision == null ||
                latestDecision.eventKey != decision.eventKey ||
                latestDecision.tokenIndex != decision.tokenIndex
            ) {
                cancelScheduledOnlineAutoMove()
                return
            }

            val delay = (
                onlineGameplayActionBlockedUntilMillis - SystemClock.uptimeMillis()
            ).coerceAtLeast(0L)
            if (delay > 0L) {
                mainHandler.postDelayed(this, delay)
                return
            }
            if (onlineRollRequestInFlight || onlineMoveRequestInFlight) {
                cancelScheduledOnlineAutoMove()
                return
            }
            if (!canSubmitOnlineGameplayAction()) {
                cancelScheduledOnlineAutoMove()
                return
            }

            if (lastAutoMoveAttemptEventKey != decision.eventKey) {
                lastAutoMoveAttemptEventKey = decision.eventKey
                autoMoveAttemptCount = 0
            }
            if (autoMoveAttemptCount >= 2) {
                cancelScheduledOnlineAutoMove()
                return
            }
            autoMoveAttemptCount += 1
            cancelScheduledOnlineAutoMove()
            moveToken(decision.tokenIndex)
        }
    }
    scheduledAutoMoveRunnable = task
    val delay = (
        onlineGameplayActionBlockedUntilMillis - SystemClock.uptimeMillis()
    ).coerceAtLeast(0L)
    mainHandler.postDelayed(task, delay)
}

internal fun MainActivity.cancelScheduledOnlineAutoMove() {
    scheduledAutoMoveRunnable?.let(mainHandler::removeCallbacks)
    scheduledAutoMoveRunnable = null
    scheduledAutoMoveEventKey = null
}
