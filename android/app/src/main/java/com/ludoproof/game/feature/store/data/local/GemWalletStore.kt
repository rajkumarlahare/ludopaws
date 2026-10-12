package com.ludoproof.game.feature.store.data.local

import android.content.Context
import android.content.SharedPreferences
import com.ludoproof.game.feature.account.data.AccountSessionStore
import org.json.JSONObject

class GemWalletStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(StorePreferences.PREFS_NAME, Context.MODE_PRIVATE)

    fun balance(): Int {
        val accountId = AccountSessionStore(appContext).load()?.accountId ?: return 0
        if (prefs.getString(StorePreferences.KEY_WALLET_ACCOUNT_ID, null) != accountId) return 0
        return prefs.getInt(StorePreferences.KEY_BALANCE, 0).coerceAtLeast(0)
    }

    fun adsRemoved(): Boolean {
        val accountId = AccountSessionStore(appContext).load()?.accountId ?: return false
        if (prefs.getString(StorePreferences.KEY_WALLET_ACCOUNT_ID, null) != accountId) return false
        return prefs.getBoolean(StorePreferences.KEY_ADS_REMOVED, false)
    }

    /**
     * Saves only a snapshot returned by the authenticated server. This cache is for rendering;
     * no purchase/reward/spend is allowed to originate in local preferences.
     */
    @Synchronized
    fun applyServerSnapshot(snapshot: JSONObject, expectedAccountId: String): Boolean {
        if (
            snapshot.optString("accountId") != expectedAccountId ||
            AccountSessionStore(appContext).load()?.accountId != expectedAccountId
        ) {
            return false
        }
        val balance = snapshot.optLong("balance", -1L)
        if (balance !in 0..MAX_BALANCE.toLong()) return false

        val priorAccountId = prefs.getString(StorePreferences.KEY_WALLET_ACCOUNT_ID, null)
        val editor = prefs.edit()
        if (priorAccountId != expectedAccountId) {
            // Do not leak cached inventory or cosmetic progress between accounts.
            editor.remove(StorePreferences.KEY_APPLIED_TOKENS)
            editor.remove(StorePreferences.KEY_OWNED_COSMETICS)
            editor.remove(StorePreferences.KEY_EVENT_ENTITLEMENTS)
            editor.remove(StorePreferences.KEY_ADS_REMOVED)
            editor.remove(StorePreferences.KEY_SELECTED_BOARD)
            editor.remove(StorePreferences.KEY_SELECTED_DICE)
            editor.remove(StorePreferences.KEY_SELECTED_AVATAR)
            editor.remove(StorePreferences.KEY_SELECTED_CHARACTER_PACK)
            prefs.all.keys.filter { it.startsWith(StorePreferences.KEY_AD_PROGRESS_PREFIX) }
                .forEach(editor::remove)
        }

        editor.putString(StorePreferences.KEY_WALLET_ACCOUNT_ID, expectedAccountId)
            .putInt(StorePreferences.KEY_BALANCE, balance.toInt())
            .putBoolean(StorePreferences.KEY_ADS_REMOVED, snapshot.optBoolean("adsRemoved", false))
            .putStringSet(
                StorePreferences.KEY_OWNED_COSMETICS,
                snapshot.optJSONArray("ownedCosmetics").toStringSet(),
            )

        val selected = snapshot.optJSONObject("selectedCosmetics") ?: JSONObject()
        selected.optString("BOARD").takeIf(String::isNotBlank)?.let { editor.putString(StorePreferences.KEY_SELECTED_BOARD, it) }
        selected.optString("DICE").takeIf(String::isNotBlank)?.let { editor.putString(StorePreferences.KEY_SELECTED_DICE, it) }
        selected.optString("AVATAR").takeIf(String::isNotBlank)?.let { editor.putString(StorePreferences.KEY_SELECTED_AVATAR, it) }
        selected.optString("CHARACTER_PACK").takeIf(String::isNotBlank)?.let { editor.putString(StorePreferences.KEY_SELECTED_CHARACTER_PACK, it) }

        val progress = snapshot.optJSONObject("rewardedAdProgress") ?: JSONObject()
        progress.keys().forEach { cosmeticId ->
            val value = progress.optInt(cosmeticId, 0).coerceAtLeast(0)
            editor.putInt(StorePreferences.KEY_AD_PROGRESS_PREFIX + cosmeticId, value)
        }

        check(editor.commit()) { "Server wallet snapshot could not be cached safely." }
        return true
    }

    private fun org.json.JSONArray?.toStringSet(): Set<String> {
        val result = linkedSetOf<String>()
        if (this == null) return result
        for (index in 0 until length()) {
            optString(index).takeIf(String::isNotBlank)?.let(result::add)
        }
        return result
    }

    private companion object {
        const val MAX_BALANCE = 2_000_000_000
    }
}
