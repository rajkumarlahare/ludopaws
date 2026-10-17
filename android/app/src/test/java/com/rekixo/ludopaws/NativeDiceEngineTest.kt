package com.rekixo.ludopaws

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.json.JSONObject
import org.junit.Test

class NativeDiceEngineTest {
    @Test
    fun nativeReceiptIsContextBoundAndRoundTripsThroughJson() {
        val receipt = NativeDiceEngine.createReceipt(
            matchId = "offline-native-test",
            eventIndex = 9,
            playerId = "offline-player-1",
            stateMaterial = """{"turnSeat":0,"players":[-1,-1,-1,-1]}""",
            issuedAt = 1_800_000_000_000L,
        )
        assertTrue(receipt.outcome in 1..6)
        assertTrue(receipt.verify())
        assertEquals(receipt, NativeDiceReceipt.fromJson(receipt.toJson()))
        assertFalse(receipt.copy(outcome = (receipt.outcome % 6) + 1).verify())
        assertFalse(receipt.copy(matchId = "another-match").verify())
        assertFalse(receipt.copy(drawWordHex = "ffffffff").verify())
    }

    @Test
    fun receiptBindsEventAndPlayerIdentity() {
        val one = NativeDiceEngine.createReceipt(
            matchId = "offline-native-test",
            eventIndex = 1,
            playerId = "offline-player-1",
            stateMaterial = "initial",
        )
        val anotherEvent = NativeDiceEngine.createReceipt(
            matchId = "offline-native-test",
            eventIndex = 2,
            playerId = "offline-player-1",
            stateMaterial = "initial",
        )
        val anotherPlayer = NativeDiceEngine.createReceipt(
            matchId = "offline-native-test",
            eventIndex = 1,
            playerId = "offline-player-2",
            stateMaterial = "initial",
        )
        assertNotEquals(one.proofDigest, anotherEvent.proofDigest)
        assertNotEquals(one.proofDigest, anotherPlayer.proofDigest)
        assertTrue(one.verify() && anotherEvent.verify() && anotherPlayer.verify())
    }
}
