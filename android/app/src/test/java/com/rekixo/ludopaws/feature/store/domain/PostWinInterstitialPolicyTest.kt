package com.rekixo.ludopaws.feature.store.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PostWinInterstitialPolicyTest {
    @Test
    fun removeAdsSuppressesOnlyTheFillerInterstitialPlacement() {
        assertFalse(PostWinInterstitialPolicy.shouldShowFillerInterstitial(adsRemoved = true))
        assertTrue(PostWinInterstitialPolicy.shouldShowFillerInterstitial(adsRemoved = false))
        // Rewarded Ads do not use this policy: watching one for the user-requested +5 Gems reward
        // remains a separate, explicitly initiated flow.
    }

    @Test
    fun offlineInterstitialOnlyFollowsFinishedLocalWin() {
        assertTrue(
            PostWinInterstitialPolicy.shouldShowOffline(
                matchStatus = "FINISHED",
                winnerPlayerId = "player-2",
                humanPlayerId = "player-1",
                isComputerMode = false,
            ),
        )
        assertFalse(
            PostWinInterstitialPolicy.shouldShowOffline(
                matchStatus = "ACTIVE",
                winnerPlayerId = null,
                humanPlayerId = "player-1",
                isComputerMode = false,
            ),
        )
    }

    @Test
    fun computerModeRequiresTheHumanPlayerToWin() {
        assertTrue(
            PostWinInterstitialPolicy.shouldShowOffline(
                matchStatus = "FINISHED",
                winnerPlayerId = "human-1",
                humanPlayerId = "human-1",
                isComputerMode = true,
            ),
        )
        assertFalse(
            PostWinInterstitialPolicy.shouldShowOffline(
                matchStatus = "FINISHED",
                winnerPlayerId = "computer-1",
                humanPlayerId = "human-1",
                isComputerMode = true,
            ),
        )
    }

    @Test
    fun remoteClassicInterstitialOnlyFollowsCurrentPlayersWin() {
        assertTrue(
            PostWinInterstitialPolicy.shouldShowRemote(
                matchStatus = "FINISHED",
                teamBased = false,
                winnerPlayerId = "player-1",
                playerId = "player-1",
                winnerTeamId = null,
                playerTeamId = null,
            ),
        )
        assertFalse(
            PostWinInterstitialPolicy.shouldShowRemote(
                matchStatus = "FINISHED",
                teamBased = false,
                winnerPlayerId = "player-2",
                playerId = "player-1",
                winnerTeamId = null,
                playerTeamId = null,
            ),
        )
    }

    @Test
    fun teamUpInterstitialOnlyFollowsWinningTeam() {
        assertTrue(
            PostWinInterstitialPolicy.shouldShowRemote(
                matchStatus = "FINISHED",
                teamBased = true,
                winnerPlayerId = null,
                playerId = "player-1",
                winnerTeamId = "A",
                playerTeamId = "A",
            ),
        )
        assertFalse(
            PostWinInterstitialPolicy.shouldShowRemote(
                matchStatus = "FINISHED",
                teamBased = true,
                winnerPlayerId = null,
                playerId = "player-1",
                winnerTeamId = "B",
                playerTeamId = "A",
            ),
        )
        assertFalse(
            PostWinInterstitialPolicy.shouldShowRemote(
                matchStatus = "ACTIVE",
                teamBased = true,
                winnerPlayerId = null,
                playerId = "player-1",
                winnerTeamId = "A",
                playerTeamId = "A",
            ),
        )
    }
}
