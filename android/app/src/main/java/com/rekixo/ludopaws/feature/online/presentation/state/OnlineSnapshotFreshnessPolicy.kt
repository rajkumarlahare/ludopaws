package com.rekixo.ludopaws

/**
 * Guards remote snapshot application against stale HTTP responses.
 *
 * Revisions come from the server and only order snapshots for the same match.
 * Cache state is useful for drawing a board during startup, but it must never
 * replace a newer snapshot already seen for that same match, including after
 * the Activity temporarily clears its source marker during lifecycle changes.
 */
internal object OnlineSnapshotFreshnessPolicy {
    fun shouldApply(
        sessionMatchId: String?,
        incomingMatchId: String,
        incomingSource: OnlineStateSource,
        currentStateSource: OnlineStateSource?,
        currentStateMatchId: String?,
        incomingRevision: Long,
        lastAppliedAuthoritativeRevision: Long,
    ): Boolean {
        val sameCurrentMatch = currentStateMatchId == incomingMatchId

        if (incomingSource == OnlineStateSource.CACHE) {
            // Do not roll a currently displayed match back to a cache copy.
            if (
                sameCurrentMatch &&
                (
                    currentStateSource == OnlineStateSource.AUTHORITATIVE ||
                    lastAppliedAuthoritativeRevision >= 0L
                )
            ) {
                return false
            }
            return true
        }

        if (
            sameCurrentMatch &&
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
