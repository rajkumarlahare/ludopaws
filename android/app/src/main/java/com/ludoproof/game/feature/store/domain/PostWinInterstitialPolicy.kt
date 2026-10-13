package com.ludoproof.game.feature.store.domain

/**
 * Keeps the initial monetization placement limited to exiting a completed win result.
 * Active-match exits, unfinished games, and losses never qualify.
 */
internal object PostWinInterstitialPolicy {
    fun shouldShowOffline(
        matchStatus: String?,
        winnerPlayerId: String?,
        humanPlayerId: String?,
        isComputerMode: Boolean,
    ): Boolean {
        if (matchStatus != FINISHED_STATUS || winnerPlayerId.isNullOrBlank()) return false
        return !isComputerMode ||
            (!humanPlayerId.isNullOrBlank() && winnerPlayerId == humanPlayerId)
    }

    fun shouldShowRemote(
        matchStatus: String?,
        teamBased: Boolean,
        winnerPlayerId: String?,
        playerId: String?,
        winnerTeamId: String?,
        playerTeamId: String?,
    ): Boolean {
        if (matchStatus != FINISHED_STATUS) return false
        return if (teamBased) {
            !winnerTeamId.isNullOrBlank() && winnerTeamId == playerTeamId
        } else {
            !winnerPlayerId.isNullOrBlank() && winnerPlayerId == playerId
        }
    }

    private const val FINISHED_STATUS = "FINISHED"
}
