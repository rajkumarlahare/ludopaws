package com.rekixo.ludopaws

import com.rekixo.ludopaws.feature.online.OnlineRollActionKind
import com.rekixo.ludopaws.feature.online.OnlineRollActionPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnlineRollActionPolicyTest {
    @Test
    fun freshTurnStartsNativeRoll() {
        val result = decision()
        assertEquals(OnlineRollActionKind.NEW_ROLL, result.kind)
        assertTrue(result.enabled)
        assertEquals("ROLL DICE", result.label)
    }

    @Test
    fun cachedOrStaleStateCannotAuthorizeRoll() {
        val result = decision(hasAuthoritativeState = false)
        assertEquals(OnlineRollActionKind.DISABLED, result.kind)
        assertFalse(result.enabled)
        assertEquals("SYNCING MATCH", result.label)
    }

    @Test
    fun resolvedNativeRollRequiresMove() {
        val result = decision(pendingStatus = "RESOLVED")
        assertEquals(OnlineRollActionKind.MOVE_REQUIRED, result.kind)
        assertFalse(result.enabled)
        assertEquals("MOVE A TOKEN", result.label)
    }

    @Test
    fun incompletePreMigrationRollWaitsForLifecycleTimeout() {
        for (status in listOf("CREATING", "COMMITTED", "RESOLVING")) {
            val result = decision(pendingStatus = status)
            assertEquals(OnlineRollActionKind.WAIT_FOR_RECOVERY, result.kind)
            assertFalse(result.enabled)
        }
    }

    @Test
    fun offlineConnectionDisablesRoll() {
        val result = decision(isOnline = false)
        assertEquals(OnlineRollActionKind.DISABLED, result.kind)
        assertFalse(result.enabled)
    }

    @Test
    fun opponentTurnDisablesRoll() {
        val result = decision(myTurn = false)
        assertEquals(OnlineRollActionKind.DISABLED, result.kind)
        assertFalse(result.enabled)
    }

    private fun decision(
        isOnline: Boolean = true,
        hasAuthoritativeState: Boolean = true,
        myTurn: Boolean = true,
        pendingStatus: String? = null,
    ) = OnlineRollActionPolicy.resolve(
        isOnline = isOnline,
        hasAuthoritativeState = hasAuthoritativeState,
        matchStatus = "ACTIVE",
        myTurn = myTurn,
        pendingStatus = pendingStatus,
    )
}
