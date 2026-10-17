package com.rekixo.ludopaws.feature.online

enum class OnlineRollActionKind {
    NEW_ROLL,
    WAIT_FOR_RECOVERY,
    MOVE_REQUIRED,
    DISABLED,
}

data class OnlineRollActionDecision(
    val kind: OnlineRollActionKind,
    val enabled: Boolean,
    val label: String,
)

/**
 * Native roll requests resolve atomically on the authoritative match server.
 * A resolved result requires a move; any stale pre-migration pending turn waits
 * for lifecycle timeout rather than calling a retired split roll endpoint.
 */
object OnlineRollActionPolicy {
    fun resolve(
        isOnline: Boolean,
        hasAuthoritativeState: Boolean,
        matchStatus: String,
        myTurn: Boolean,
        pendingStatus: String?,
    ): OnlineRollActionDecision {
        if (matchStatus != "ACTIVE" || !myTurn) return disabled()
        if (!isOnline) {
            return OnlineRollActionDecision(
                kind = OnlineRollActionKind.DISABLED,
                enabled = false,
                label = "OFFLINE • WAITING",
            )
        }
        if (!hasAuthoritativeState) {
            return OnlineRollActionDecision(
                kind = OnlineRollActionKind.DISABLED,
                enabled = false,
                label = "SYNCING MATCH",
            )
        }
        if (pendingStatus == "RESOLVED") {
            return OnlineRollActionDecision(
                kind = OnlineRollActionKind.MOVE_REQUIRED,
                enabled = false,
                label = "MOVE A TOKEN",
            )
        }
        if (pendingStatus != null) {
            return OnlineRollActionDecision(
                kind = OnlineRollActionKind.WAIT_FOR_RECOVERY,
                enabled = false,
                label = "WAITING FOR PREVIOUS TURN",
            )
        }
        return OnlineRollActionDecision(
            kind = OnlineRollActionKind.NEW_ROLL,
            enabled = true,
            label = "ROLL DICE",
        )
    }

    private fun disabled() = OnlineRollActionDecision(
        kind = OnlineRollActionKind.DISABLED,
        enabled = false,
        label = "ROLL DICE",
    )
}
