package com.rekixo.ludopaws

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DiceTurnCountdownPolicyTest {
    @Test
    fun fullProgressAtDeadlineStart() {
        assertEquals(1f, DiceTurnCountdownPolicy.remainingFraction(15_000L, 0L)!!, 0.0001f)
    }

    @Test
    fun progressDecreasesLinearlyWithRemainingServerTime() {
        assertEquals(0.5f, DiceTurnCountdownPolicy.remainingFraction(15_000L, 7_500L)!!, 0.0001f)
        assertEquals(0.1f, DiceTurnCountdownPolicy.remainingFraction(15_000L, 13_500L)!!, 0.0001f)
    }

    @Test
    fun deadlineExpiryLeavesNoProgress() {
        assertEquals(0f, DiceTurnCountdownPolicy.remainingFraction(15_000L, 15_000L)!!, 0.0001f)
        assertEquals(0f, DiceTurnCountdownPolicy.remainingFraction(15_000L, 16_000L)!!, 0.0001f)
    }

    @Test
    fun missingDeadlineDoesNotShowCountdown() {
        assertNull(DiceTurnCountdownPolicy.remainingFraction(null, 0L))
    }
}
