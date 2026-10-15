package com.rekixo.ludopaws

import com.rekixo.ludopaws.feature.settings.data.local.GameSpeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnlineGameplayPacingPolicyTest {
    @Test
    fun resolvedOnlineRollUsesTheSameDiceHoldAsOfflinePlay() {
        val previous = snapshot(randomEventIndex = 4)
        val current = snapshot(
            randomEventIndex = 5,
            pending = pending("RESOLVED", eventIndex = 4, outcome = 3),
        )

        val transition = OnlineGameplayPacingPolicy.resolve(previous, current, GameSpeed.NORMAL)

        assertEquals("LPABCDEFGH:roll:4", transition?.key)
        assertEquals(700L, transition?.delayMillis)
    }

    @Test
    fun duplicateResolvedSnapshotDoesNotScheduleAnotherHold() {
        val state = snapshot(
            randomEventIndex = 5,
            pending = pending("RESOLVED", eventIndex = 4, outcome = 3),
        )

        assertNull(OnlineGameplayPacingPolicy.resolve(state, state, GameSpeed.NORMAL))
    }

    @Test
    fun rollThatPassesTurnWithoutLegalMoveStillGetsReadableHold() {
        val previous = snapshot(randomEventIndex = 4)
        val current = snapshot(
            turnSeat = 1,
            randomEventIndex = 5,
            history = listOf(event(eventIndex = 4, outcome = 2)),
        )

        val transition = OnlineGameplayPacingPolicy.resolve(previous, current, GameSpeed.NORMAL)

        assertEquals("LPABCDEFGH:roll:4", transition?.key)
        assertEquals(700L, transition?.delayMillis)
    }

    @Test
    fun completedMoveUsesAnimationDurationAndPostMoveBreath() {
        val previous = snapshot(
            randomEventIndex = 5,
            pending = pending("RESOLVED", eventIndex = 4, outcome = 3),
        )
        val current = snapshot(
            randomEventIndex = 5,
            turnSeat = 1,
            players = makePlayers(firstTokens = listOf(3, -1, -1, -1)),
            history = listOf(event(eventIndex = 4, outcome = 3, moveTokenIndex = 0)),
        )
        val motion = LudoPawsPawnAnimationPolicy.plans(previous, current).single()
        val expected =
            LudoPawsGameplayPacingPolicy.forwardTravelDurationMillis(
                GameSpeed.NORMAL,
                motion.visualSteps,
            ) + LudoPawsGameplayPacingPolicy.postMoveBreathMillis(
                GameSpeed.NORMAL,
                extraTurn = false,
            )

        val transition = OnlineGameplayPacingPolicy.resolve(previous, current, GameSpeed.NORMAL)

        assertEquals("LPABCDEFGH:move:5:1", transition?.key)
        assertEquals(expected, transition?.delayMillis)
        assertTrue(expected > 450L)
    }

    @Test
    fun teamUpPartnerHandoffIsNotMistakenForAnExtraTurn() {
        val previous = snapshot(
            matchMode = "TEAM_UP",
            turnSeat = 0,
            actingSeat = 0,
            randomEventIndex = 5,
            pending = pending("RESOLVED", eventIndex = 4, outcome = 3),
        )
        val current = snapshot(
            matchMode = "TEAM_UP",
            turnSeat = 0,
            actingSeat = 2,
            randomEventIndex = 5,
            players = makePlayers(firstTokens = listOf(3, -1, -1, -1), teamUp = true),
            history = listOf(event(eventIndex = 4, outcome = 3, moveTokenIndex = 0)),
        )
        val motion = LudoPawsPawnAnimationPolicy.plans(previous, current).single()
        val expected =
            LudoPawsGameplayPacingPolicy.forwardTravelDurationMillis(
                GameSpeed.NORMAL,
                motion.visualSteps,
            ) + LudoPawsGameplayPacingPolicy.postMoveBreathMillis(
                GameSpeed.NORMAL,
                extraTurn = false,
            )

        val transition = OnlineGameplayPacingPolicy.resolve(previous, current, GameSpeed.NORMAL)

        assertEquals(expected, transition?.delayMillis)
    }

    private fun snapshot(
        turnSeat: Int = 0,
        randomEventIndex: Int,
        pending: PendingRollSnapshot? = null,
        history: List<HistoryEventSnapshot> = emptyList(),
        players: List<PlayerSnapshot> = makePlayers(),
        matchMode: String = "ONLINE",
        actingSeat: Int? = null,
    ): MatchSnapshot =
        MatchSnapshot(
            matchId = "LPABCDEFGH",
            status = "ACTIVE",
            hostPlayerId = "p0",
            targetPlayerCount = players.size,
            matchMode = matchMode,
            players = players,
            turnSeat = turnSeat,
            randomEventIndex = randomEventIndex,
            pendingRoll = pending,
            winnerPlayerId = null,
            rulesetId = if (matchMode == "TEAM_UP") "ludoproof-team-v3" else "ludoproof-standard-v3",
            history = history,
            actingSeat = actingSeat,
            teamAssignments = if (matchMode == "TEAM_UP") listOf("A", "B", "A", "B") else emptyList(),
        )

    private fun makePlayers(
        firstTokens: List<Int> = listOf(-1, -1, -1, -1),
        teamUp: Boolean = false,
    ): List<PlayerSnapshot> =
        listOf(
            PlayerSnapshot("p0", "Player 0", "RED", 0, firstTokens, if (teamUp) "A" else null),
            PlayerSnapshot("p1", "Player 1", "GREEN", 1, listOf(-1, -1, -1, -1), if (teamUp) "B" else null),
            PlayerSnapshot("p2", "Player 2", "YELLOW", 2, listOf(-1, -1, -1, -1), if (teamUp) "A" else null),
            PlayerSnapshot("p3", "Player 3", "BLUE", 3, listOf(-1, -1, -1, -1), if (teamUp) "B" else null),
        )

    private fun pending(
        status: String,
        eventIndex: Int,
        outcome: Int? = null,
    ): PendingRollSnapshot =
        PendingRollSnapshot(
            status = status,
            seat = 0,
            eventIndex = eventIndex,
            eventId = "roll:$eventIndex",
            roundId = "round-$eventIndex",
            serverCommitment = "server-$eventIndex",
            clientCommitment = "client-$eventIndex",
            revealDeadlineAt = 123456789L,
            proofDigest = if (status == "RESOLVED") "proof-$eventIndex" else null,
            outcome = outcome,
            legalTokenIndexes = if (status == "RESOLVED") setOf(0) else emptySet(),
        )

    private fun event(
        eventIndex: Int,
        outcome: Int,
        moveTokenIndex: Int? = null,
    ): HistoryEventSnapshot =
        HistoryEventSnapshot(
            eventIndex = eventIndex,
            playerId = "p0",
            roundId = "round-$eventIndex",
            proofDigest = "proof-$eventIndex",
            outcome = outcome,
            moveTokenIndex = moveTokenIndex,
            captures = 0,
            effectiveOutcome = outcome,
        )
}
