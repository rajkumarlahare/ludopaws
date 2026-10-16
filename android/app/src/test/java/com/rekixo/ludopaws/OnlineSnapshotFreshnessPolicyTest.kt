package com.rekixo.ludopaws

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnlineSnapshotFreshnessPolicyTest {
    @Test
    fun olderAuthoritativeRevisionIsRejectedForCurrentMatch() {
        assertFalse(
            OnlineSnapshotFreshnessPolicy.shouldApply(
                sessionMatchId = MATCH_ID,
                incomingMatchId = MATCH_ID,
                incomingSource = OnlineStateSource.AUTHORITATIVE,
                currentStateSource = OnlineStateSource.AUTHORITATIVE,
                incomingRevision = 11L,
                lastAppliedAuthoritativeRevision = 12L,
            ),
        )
    }

    @Test
    fun equalOrNewerAuthoritativeRevisionIsAccepted() {
        for (revision in listOf(12L, 13L)) {
            assertTrue(
                OnlineSnapshotFreshnessPolicy.shouldApply(
                    sessionMatchId = MATCH_ID,
                    incomingMatchId = MATCH_ID,
                    incomingSource = OnlineStateSource.AUTHORITATIVE,
                    currentStateSource = OnlineStateSource.AUTHORITATIVE,
                    incomingRevision = revision,
                    lastAppliedAuthoritativeRevision = 12L,
                ),
            )
        }
    }

    @Test
    fun staleRevisionFromAnotherMatchIsNotComparedToCurrentMatchRevision() {
        assertTrue(
            OnlineSnapshotFreshnessPolicy.shouldApply(
                sessionMatchId = "LPQRSTUV23",
                incomingMatchId = MATCH_ID,
                incomingSource = OnlineStateSource.AUTHORITATIVE,
                currentStateSource = OnlineStateSource.AUTHORITATIVE,
                incomingRevision = 1L,
                lastAppliedAuthoritativeRevision = 99L,
            ),
        )
    }

    @Test
    fun cacheCannotReplaceAuthoritativeStateForSameMatch() {
        assertFalse(
            OnlineSnapshotFreshnessPolicy.shouldApply(
                sessionMatchId = MATCH_ID,
                incomingMatchId = MATCH_ID,
                incomingSource = OnlineStateSource.CACHE,
                currentStateSource = OnlineStateSource.AUTHORITATIVE,
                incomingRevision = 7L,
                lastAppliedAuthoritativeRevision = 12L,
            ),
        )
    }

    @Test
    fun initialCacheAndNewAuthoritativeStateCanBeApplied() {
        assertTrue(
            OnlineSnapshotFreshnessPolicy.shouldApply(
                sessionMatchId = MATCH_ID,
                incomingMatchId = MATCH_ID,
                incomingSource = OnlineStateSource.CACHE,
                currentStateSource = null,
                incomingRevision = 7L,
                lastAppliedAuthoritativeRevision = -1L,
            ),
        )
        assertTrue(
            OnlineSnapshotFreshnessPolicy.shouldApply(
                sessionMatchId = MATCH_ID,
                incomingMatchId = MATCH_ID,
                incomingSource = OnlineStateSource.AUTHORITATIVE,
                currentStateSource = OnlineStateSource.CACHE,
                incomingRevision = 8L,
                lastAppliedAuthoritativeRevision = -1L,
            ),
        )
    }

    private companion object {
        const val MATCH_ID = "LPABCDEFGH"
    }
}
