package com.ludoproof.game.feature.store.domain

import android.content.Context
import com.ludoproof.game.feature.profile.data.local.ProfileStore
import com.ludoproof.game.feature.store.data.local.GemWalletStore
import com.ludoproof.game.feature.store.domain.model.VerifiedStorePurchase
import org.json.JSONObject

/**
 * Updates display caches only after the authenticated backend returns its authoritative wallet.
 * This class cannot grant gems from a client-side "verified" boolean or purchase token.
 */
class StorePurchaseFulfillment(context: Context) {
    private val appContext = context.applicationContext
    private val wallet = GemWalletStore(appContext)
    private val profile = ProfileStore(appContext)

    fun applyVerifiedPurchase(
        purchase: VerifiedStorePurchase,
        serverWalletSnapshot: JSONObject,
        accountId: String,
    ): Boolean {
        val product = StoreCatalog.find(purchase.productId) ?: return false
        val applied = wallet.applyServerSnapshot(serverWalletSnapshot, accountId)
        if (applied) {
            // Profile's purchase list is presentation cache only, never an entitlement authority.
            profile.addPurchase(product.productId)
        }
        return applied
    }
}
