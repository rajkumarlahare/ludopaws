package com.rekixo.ludopaws

/**
 * Guards remote snapshot application against stale HTTP responses.
 *
 * Revisions come from the server and only order snapshots for the same match.
 * Cache state is useful for drawing a board during startup, but it must never
 * replace a live authoritative snapshot for that same match.
 */
internal object OnlineSnapshotFreshnessPolicy {
    fun shouldApply(
        sessionMatchId: String?,
        incomingMatchId: String,
        incomingSource: OnlineStateSource,
        currentStateSource: OnlineStateSource?,
        incomingRevision: Long,
        lastAppliedAuthoritativeRevision: Long,
    ): Boolean {
        if (incomingSource == OnlineStateSource.CACHE) {
            if (
                currentStateSource == OnlineStateSource.AUTHORITATIVE &&
                sessionMatchId == incomingMatchId
            ) {
                return false
            }
            return true
        }

        if (
            sessionMatchId == incomingMatchId &&
            incomingRevision >= 0L &&
            lastAppliedAuthoritativeRevision >= 0L &&
            incomingRevision < lastAppliedAuthoritativeRevision
        ) {
            return false
        }
        return true
    }
}
