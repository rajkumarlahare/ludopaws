package com.ludoproof.game.feature.store.data.local

import android.content.Context
import com.ludoproof.game.feature.account.data.AccountSessionStore
import com.ludoproof.game.feature.store.domain.StoreCosmeticCatalog
import com.ludoproof.game.feature.store.domain.model.CosmeticAcquireResult
import com.ludoproof.game.feature.store.domain.model.CosmeticCategory
import com.ludoproof.game.feature.store.domain.model.CosmeticUnlockKind
import com.ludoproof.game.feature.store.domain.model.StoreCosmetic
import org.json.JSONObject

/**
 * Read-through cache for store UI only. Final ownership, purchase, selection and reward decisions
 * are made by the authenticated server wallet. These methods never grant local entitlements.
 */
class CosmeticInventoryStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(StorePreferences.PREFS_NAME, Context.MODE_PRIVATE)

    fun applyServerSnapshot(snapshot: JSONObject, expectedAccountId: String): Boolean =
        GemWalletStore(appContext).applyServerSnapshot(snapshot, expectedAccountId)

    fun isOwned(cosmeticId: String): Boolean =
        hasCurrentAccountSnapshot() && cosmeticId in ownedIds()

    fun selectedId(category: CosmeticCategory): String {
        if (!hasCurrentAccountSnapshot()) return StoreCosmeticCatalog.defaultId(category)
        return prefs.getString(selectedKey(category), null)
            ?.takeIf { id ->
                val item = StoreCosmeticCatalog.find(id)
                item != null && item.category == category && item.contentAvailable && isOwned(id)
            }
            ?: StoreCosmeticCatalog.defaultId(category)
    }

    fun rewardedAdProgress(cosmeticId: String): Int {
        if (!hasCurrentAccountSnapshot()) return 0
        return prefs.getInt(StorePreferences.KEY_AD_PROGRESS_PREFIX + cosmeticId, 0).coerceAtLeast(0)
    }

    fun hasVerifiedEventEntitlement(cosmeticId: String): Boolean {
        // Event entitlements are server-controlled; local preferences are never proof.
        return false
    }

    @Synchronized
    fun acquireOrSelect(cosmetic: StoreCosmetic, currentLevel: Int): CosmeticAcquireResult {
        if (!cosmetic.contentAvailable) return CosmeticAcquireResult.CONTENT_UNAVAILABLE
        if (currentLevel < cosmetic.requiredLevel) return CosmeticAcquireResult.NEED_LEVEL
        if (isOwned(cosmetic.id)) {
            // Selection must go through /api/wallet/cosmetics/select.
            return CosmeticAcquireResult.INVALID
        }
        return when (cosmetic.unlockKind) {
            CosmeticUnlockKind.GEMS ->
                if (GemWalletStore(appContext).balance() < cosmetic.gemPrice) {
                    CosmeticAcquireResult.NEED_GEMS
                } else {
                    // The server performs the debit and ownership update atomically.
                    CosmeticAcquireResult.INVALID
                }
            CosmeticUnlockKind.REWARDED_ADS ->
                if (rewardedAdProgress(cosmetic.id) < cosmetic.requiredAdViews) {
                    CosmeticAcquireResult.NEED_REWARDED_ADS
                } else {
                    CosmeticAcquireResult.INVALID
                }
            CosmeticUnlockKind.EVENT -> CosmeticAcquireResult.NEED_EVENT
            CosmeticUnlockKind.FREE, CosmeticUnlockKind.LEVEL -> CosmeticAcquireResult.INVALID
        }
    }

    @Synchronized
    fun recordVerifiedRewardedAd(cosmeticId: String): Int {
        // Ad callbacks on the device are not authoritative. Only a server-verified SSV snapshot
        // may update the progress that this method reads.
        return rewardedAdProgress(cosmeticId)
    }

    @Synchronized
    fun recordVerifiedEventEntitlement(cosmeticId: String): Boolean = false

    private fun hasCurrentAccountSnapshot(): Boolean {
        val accountId = AccountSessionStore(appContext).load()?.accountId ?: return false
        return prefs.getString(StorePreferences.KEY_WALLET_ACCOUNT_ID, null) == accountId
    }

    private fun ownedIds(): Set<String> =
        prefs.getStringSet(StorePreferences.KEY_OWNED_COSMETICS, emptySet())?.toSet() ?: emptySet()

    private fun selectedKey(category: CosmeticCategory): String =
        when (category) {
            CosmeticCategory.BOARD -> StorePreferences.KEY_SELECTED_BOARD
            CosmeticCategory.DICE -> StorePreferences.KEY_SELECTED_DICE
            CosmeticCategory.AVATAR -> StorePreferences.KEY_SELECTED_AVATAR
            CosmeticCategory.CHARACTER_PACK -> StorePreferences.KEY_SELECTED_CHARACTER_PACK
        }
}
