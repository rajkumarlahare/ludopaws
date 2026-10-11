package com.ludoproof.game.feature.store.data.billing

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class PendingStorePurchase(
    val productId: String,
    val purchaseToken: String,
    val accountId: String,
)

class PendingStorePurchaseStore(
    context: Context,
) {
    private val preferences =
        context.applicationContext.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        )

    @Synchronized
    fun all(): List<PendingStorePurchase> {
        val raw = preferences.getString(KEY_PENDING_PURCHASES, null) ?: return emptyList()
        val values = JSONArray(raw)
        return List(values.length()) { index ->
            val value = values.getJSONObject(index)
            PendingStorePurchase(
                productId = value.getString("productId"),
                purchaseToken = value.getString("purchaseToken"),
                accountId = value.getString("accountId"),
            )
        }
    }

    @Synchronized
    fun add(purchase: PendingStorePurchase) {
        require(purchase.productId.isNotBlank())
        require(purchase.purchaseToken.isNotBlank())
        require(purchase.accountId.matches(ACCOUNT_ID_REGEX))
        val current = all()
        val existing =
            current.firstOrNull { it.purchaseToken == purchase.purchaseToken }
        if (existing != null) {
            check(existing == purchase) {
                "A pending Play purchase token conflicts with its saved product details."
            }
            return
        }
        persist(current + purchase)
    }

    @Synchronized
    fun remove(purchaseToken: String) {
        val current = all()
        persist(current.filterNot { it.purchaseToken == purchaseToken })
    }

    private fun persist(purchases: List<PendingStorePurchase>) {
        val values =
            JSONArray().apply {
                purchases.forEach { purchase ->
                    put(
                        JSONObject()
                            .put("productId", purchase.productId)
                            .put("purchaseToken", purchase.purchaseToken)
                            .put("accountId", purchase.accountId),
                    )
                }
            }
        check(
            preferences.edit()
                .putString(KEY_PENDING_PURCHASES, values.toString())
                .commit(),
        ) {
            "Could not save pending Play purchase verification."
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "ludoproof_wallet"
        const val KEY_PENDING_PURCHASES = "pending_play_purchases"
        val ACCOUNT_ID_REGEX = Regex("[a-f0-9]{64}")
    }
}
