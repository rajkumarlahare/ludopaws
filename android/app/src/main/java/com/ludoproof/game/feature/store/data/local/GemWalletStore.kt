package com.ludoproof.game.feature.store.data.local

import android.content.Context

class GemWalletStore(
    context: Context,
) {
    private val prefs =
        context.getSharedPreferences(
            StorePreferences.PREFS_NAME,
            Context.MODE_PRIVATE,
        )

    fun balance(): Int =
        prefs.getInt(
            StorePreferences.KEY_BALANCE,
            0,
        )
            .coerceAtLeast(
                0,
            )

    fun adsRemoved(): Boolean =
        prefs.getBoolean(
            StorePreferences.KEY_ADS_REMOVED,
            false,
        )

    @Synchronized
    fun applyRewardedAdGems(
        gemAmount: Int,
    ): Int {
        require(gemAmount > 0) {
            "Rewarded-ad gem amount must be positive."
        }
        val nextBalance =
            (
                balance().toLong() +
                    gemAmount
                )
                .coerceAtMost(
                    Int.MAX_VALUE.toLong(),
                )
                .toInt()
        check(
            prefs.edit()
                .putInt(
                    StorePreferences.KEY_BALANCE,
                    nextBalance,
                )
                .commit(),
        ) {
            "Could not safely save the rewarded-ad gem reward."
        }
        return nextBalance
    }

    @Synchronized
    fun applyVerifiedPurchase(
        productId: String,
        purchaseToken: String,
        gemAmount: Int,
        removeAds: Boolean,
    ): Boolean {
        if (
            purchaseToken.isBlank()
        ) {
            return false
        }

        val applied =
            prefs.getStringSet(
                StorePreferences.KEY_APPLIED_TOKENS,
                emptySet(),
            )
                ?.toMutableSet()
                ?: mutableSetOf()

        if (
            purchaseToken in
                applied
        ) {
            return false
        }

        val nextBalance =
            (
                balance()
                    .toLong() +
                    gemAmount
                        .coerceAtLeast(
                            0,
                        )
                )
                .coerceAtMost(
                    Int.MAX_VALUE
                        .toLong(),
                )
                .toInt()

        applied +=
            purchaseToken

        val saved =
            prefs
            .edit()
            .putInt(
                StorePreferences.KEY_BALANCE,
                nextBalance,
            )
            .putBoolean(
                StorePreferences.KEY_ADS_REMOVED,
                adsRemoved() ||
                    removeAds,
            )
            .putStringSet(
                StorePreferences.KEY_APPLIED_TOKENS,
                applied,
            )
            .commit()
        check(saved) {
            "Could not safely save the verified Play purchase."
        }
        return true
    }

}
