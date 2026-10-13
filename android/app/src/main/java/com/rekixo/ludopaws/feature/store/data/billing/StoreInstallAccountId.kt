package com.rekixo.ludopaws.feature.store.data.billing

import android.content.Context
import java.security.MessageDigest
import java.util.UUID

class StoreInstallAccountId(
    context: Context,
) {
    private val preferences =
        context.applicationContext.getSharedPreferences(
            "ludoproof_wallet",
            Context.MODE_PRIVATE,
        )

    val value: String by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        val existing =
            preferences.getString(KEY_INSTALL_ACCOUNT_ID, null)
                ?.takeIf { it.matches(ACCOUNT_ID_REGEX) }
        existing ?: sha256(UUID.randomUUID().toString()).also { accountId ->
            check(
                preferences.edit()
                    .putString(KEY_INSTALL_ACCOUNT_ID, accountId)
                    .commit(),
            ) {
                "Could not save the Play purchase account identifier."
            }
        }
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte ->
                (byte.toInt() and 0xff)
                    .toString(16)
                    .padStart(2, '0')
            }

    private companion object {
        const val KEY_INSTALL_ACCOUNT_ID = "play_purchase_account_id"
        val ACCOUNT_ID_REGEX = Regex("[a-f0-9]{64}")
    }
}
