package com.ludoproof.game.feature.store.presentation.actions

import com.ludoproof.game.feature.profile.data.local.ProfileStore
import com.ludoproof.game.feature.profile.domain.ProfileProgression
import com.ludoproof.game.feature.store.domain.model.CosmeticAcquireResult
import com.ludoproof.game.feature.store.domain.model.CosmeticUnlockKind
import com.ludoproof.game.feature.store.domain.model.StoreCosmetic
import com.ludoproof.game.feature.store.presentation.StoreActivity

internal fun StoreActivity.handleCosmeticTap(cosmetic: StoreCosmetic) {
    // CONTENT_UNAVAILABLE is handled before any server request or value-bearing action.
    if (!cosmetic.contentAvailable) {
        showStoreMessage(
            cosmetic.title +
                " is reserved for a future complete animal-pack drop. No gems, ad progress, or event entitlement was spent.",
        )
        return
    }

    val level = ProfileProgression
        .levelProgress(ProfileStore(this).snapshot().totalXp)
        .level
    if (!cosmetics.isOwned(cosmetic.id) && level < cosmetic.requiredLevel) {
        showStoreMessage("Reach level ${cosmetic.requiredLevel} to unlock ${cosmetic.title}.")
        return
    }

    if (cosmetics.isOwned(cosmetic.id)) {
        purchaseOrSelectCosmetic(cosmetic)
        return
    }

    when (cosmetic.unlockKind) {
        CosmeticUnlockKind.REWARDED_ADS -> {
            if (cosmetics.rewardedAdProgress(cosmetic.id) < cosmetic.requiredAdViews) {
                requestRewardedAd(cosmetic.id)
            } else {
                purchaseOrSelectCosmetic(cosmetic)
            }
        }
        CosmeticUnlockKind.EVENT -> {
            when (cosmetics.acquireOrSelect(cosmetic, level)) {
                CosmeticAcquireResult.NEED_EVENT -> showStoreMessage(
                    cosmetic.title +
                        " is an event item. It can only be unlocked after the server confirms the event entitlement.",
                )
                else -> showStoreMessage(
                    cosmetic.title +
                        " cannot be unlocked until its event entitlement is confirmed by the server.",
                )
            }
        }
        CosmeticUnlockKind.FREE,
        CosmeticUnlockKind.LEVEL,
        CosmeticUnlockKind.GEMS -> purchaseOrSelectCosmetic(cosmetic)
    }
}
