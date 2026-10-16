package com.rekixo.ludopaws

/**
 * Guards authoritative UI state from delayed HTTP replies arriving after a newer
 * WebSocket-triggered refresh or gameplay mutation. A server revision is monotonic
 * within a match; cached snapshots never outrank a fresh authoritative response.
 */
internal object OnlineSnapshotRevisionPolicy {
    fun shouldApply(
        currentMatchId: String?,
        currentRevision: Long?,
        currentIsAuthoritative: Boolean,
        incomingMatchId: String,
        incomingRevision: Long,
    ): Boolean {
        if (!currentIsAuthoritative || currentMatchId != incomingMatchId) return true
        if (currentRevision == null) return true
        return incomingRevision > currentRevision
    }
}
