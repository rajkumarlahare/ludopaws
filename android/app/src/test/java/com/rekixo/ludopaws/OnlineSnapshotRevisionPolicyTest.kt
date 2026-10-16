package com.rekixo.ludopaws

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnlineSnapshotRevisionPolicyTest {
    @Test
    fun olderAuthoritativeReplyCannotRollBackCurrentMatch() {
        assertFalse(
            OnlineSnapshotRevisionPolicy.shouldApply(
                currentMatchId = "LPABCDEFGH",
                currentRevision = 12L,
                currentIsAuthoritative = true,
                incomingMatchId = "LPABCDEFGH",
                incomingRevision = 11L,
            ),
        )
    }

    @Test
    fun newerAndEqualAuthoritativeRevisionsAreAccepted() {
        assertTrue(
            OnlineSnapshotRevisionPolicy.shouldApply(
                "LPABCDEFGH", 12L, true, "LPABCDEFGH", 13L,
            ),
        )
        assertTrue(
            OnlineSnapshotRevisionPolicy.shouldApply(
                "LPABCDEFGH", 12L, true, "LPABCDEFGH", 12L,
            ),
        )
    }

    @Test
    fun differentMatchAndCachedCurrentStateDoNotBlockAuthoritativeReply() {
        assertTrue(
            OnlineSnapshotRevisionPolicy.shouldApply(
                "LPABCDEFGH", 20L, true, "LP12345678", 1L,
            ),
        )
        assertTrue(
            OnlineSnapshotRevisionPolicy.shouldApply(
                "LPABCDEFGH", 20L, false, "LPABCDEFGH", 1L,
            ),
        )
    }
}
