package com.rekixo.ludopaws

import android.app.Activity
import android.content.Intent
import com.rekixo.ludopaws.feature.mode.presentation.RemoteModeEntryActivity
import com.rekixo.ludopaws.feature.friends.presentation.FriendsActivity
import com.rekixo.ludopaws.feature.team.presentation.TeamUpActivity
import com.rekixo.ludopaws.feature.safety.AgeSafetyPolicy
import com.rekixo.ludopaws.feature.safety.AgeSafetyUi

fun Activity.openGameMode(
    mode: GameMode,
    resumeSavedMatch: Boolean = false,
) {
    if (!AgeSafetyPolicy.hasSelectedAgeRange(this)) {
        AgeSafetyUi.showAgeRangeIfNeeded(this) {
            (application as? LudoProofApplication)?.onAgeRangeSelected()
            if (mode.isRemote && !AgeSafetyPolicy.canUseOnlineFeatures(this)) return@showAgeRangeIfNeeded
            openGameMode(mode, resumeSavedMatch)
        }
        return
    }
    if (mode.isRemote && !AgeSafetyPolicy.canUseOnlineFeatures(this)) {
        AgeSafetyUi.showFamilySafetyInfo(this)
        return
    }

    val target =
        when (
            GameModeRouting
                .destination(
                    mode,
                )
        ) {
            GameModeDestination.ONLINE_ACTIVITY ->
                MainActivity::class.java

            GameModeDestination.REMOTE_MODE_ENTRY ->
                TeamUpActivity::class.java

            GameModeDestination.FRIENDS_ACTIVITY ->
                FriendsActivity::class.java

            GameModeDestination.OFFLINE_ACTIVITY ->
                OfflineGameActivity::class.java
        }

    startActivity(
        Intent(
            this,
            target,
        )
            .putExtra(
                GameModeIntent.EXTRA_GAME_MODE,
                mode.wireValue,
            )
            .putExtra(
                GameModeIntent.EXTRA_RESUME_SAVED_MATCH,
                resumeSavedMatch,
            ),
    )
}
