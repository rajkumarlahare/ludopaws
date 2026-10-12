package com.ludoproof.game

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ludoproof.game.feature.account.data.AccountSession
import com.ludoproof.game.feature.account.data.AccountSessionStore
import com.ludoproof.game.feature.store.data.billing.PendingStorePurchase
import com.ludoproof.game.feature.store.data.billing.PendingStorePurchaseStore
import com.ludoproof.game.feature.store.data.local.CosmeticInventoryStore
import com.ludoproof.game.feature.store.data.local.GemWalletStore
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class AccountWalletSecurityInstrumentedTest {
    private lateinit var context: Context
    private lateinit var walletPreferences: android.content.SharedPreferences

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("ludoproof_account_session_v1", Context.MODE_PRIVATE)
            .edit().clear().commit()
        walletPreferences = context.getSharedPreferences("ludoproof_wallet", Context.MODE_PRIVATE)
        walletPreferences.edit().clear().commit()
    }

    @After
    fun tearDown() {
        context.getSharedPreferences("ludoproof_account_session_v1", Context.MODE_PRIVATE)
            .edit().clear().commit()
        walletPreferences.edit().clear().commit()
    }

    @Test
    fun pendingPurchaseTokensAreEncryptedAndSurviveStoreRecreation() {
        val purchase = pendingPurchase()
        PendingStorePurchaseStore(context).add(purchase)

        val stored = requireNotNull(walletPreferences.getString("pending_play_purchases", null))
        assertTrue(stored.startsWith("v1."))
        assertFalse(stored.contains(purchase.purchaseToken))
        assertEquals(listOf(purchase), PendingStorePurchaseStore(context).all())
    }

    @Test
    fun legacyPendingPurchaseRecordsAreMigratedWithoutLosingTheToken() {
        val purchase = pendingPurchase()
        val legacy = org.json.JSONArray().put(
            JSONObject()
                .put("productId", purchase.productId)
                .put("purchaseToken", purchase.purchaseToken)
                .put("accountId", purchase.accountId),
        )
        walletPreferences.edit().putString("pending_play_purchases", legacy.toString()).commit()

        assertEquals(listOf(purchase), PendingStorePurchaseStore(context).all())
        val encrypted = requireNotNull(walletPreferences.getString("pending_play_purchases", null))
        assertTrue(encrypted.startsWith("v1."))
        assertFalse(encrypted.contains(purchase.purchaseToken))
        assertEquals(listOf(purchase), PendingStorePurchaseStore(context).all())
    }

    @Test
    fun corruptedPendingPurchaseCiphertextNeverReturnsARewardableRecord() {
        PendingStorePurchaseStore(context).add(pendingPurchase())
        val stored = requireNotNull(walletPreferences.getString("pending_play_purchases", null))
        val replacement = if (stored.last() == 'A') 'B' else 'A'
        walletPreferences.edit()
            .putString("pending_play_purchases", stored.dropLast(1) + replacement)
            .commit()

        try {
            PendingStorePurchaseStore(context).all()
            fail("Corrupted purchase-token ciphertext must not be accepted.")
        } catch (_: Exception) {
            // Expected: authentication failed. Keep the record quarantined rather than granting.
        }
        assertTrue(walletPreferences.contains("pending_play_purchases"))
    }

    @Test
    fun convertingGuestToGoogleModeClearsTheGuestRecoveryCredential() {
        val sessionStore = AccountSessionStore(context)
        val sessionPreferences = context.getSharedPreferences(
            "ludoproof_account_session_v1",
            Context.MODE_PRIVATE,
        )
        saveTestSession()
        val guestRecoveryId = sessionStore.guestRequestId()
        val storedGuestCiphertext = requireNotNull(
            sessionPreferences.getString("guest_request_ciphertext", null),
        )
        assertFalse(storedGuestCiphertext.contains(guestRecoveryId))
        assertEquals("GUEST", sessionStore.lastAccountMode())

        val session = requireNotNull(sessionStore.load())
        sessionStore.save(session.copy(guestRecoveryEligible = false))

        assertEquals("GOOGLE", sessionStore.lastAccountMode())
        assertFalse(sessionPreferences.contains("guest_request_ciphertext"))
        assertFalse(sessionPreferences.contains("guest_request_iv"))
    }

    @Test
    fun firstServerWalletSnapshotArchivesLegacyLocalDataButNeverGrantsIt() {
        saveTestSession()
        walletPreferences.edit()
            .putInt("gem_balance", 500)
            .putBoolean("ads_removed", true)
            .putStringSet("owned_cosmetics", setOf("dice_heart"))
            .putString("selected_dice", "dice_heart")
            .putInt("ad_progress_dice_summers", 17)
            .commit()

        val expectedAccountId = ACCOUNT_ID
        val serverWallet = JSONObject()
            .put("accountId", expectedAccountId)
            .put("balance", 0)
            .put("adsRemoved", false)
            .put("ownedCosmetics", org.json.JSONArray())
            .put("selectedCosmetics", JSONObject())
            .put("rewardedAdProgress", JSONObject())

        val store = GemWalletStore(context)
        assertTrue(store.applyServerSnapshot(serverWallet, expectedAccountId))

        val archive = JSONObject(
            requireNotNull(walletPreferences.getString("legacy_wallet_unverified_archive_v1", null)),
        )
        assertFalse(archive.getBoolean("verified"))
        assertEquals(500, archive.getInt("balance"))
        assertTrue(archive.getBoolean("adsRemoved"))
        assertTrue(archive.getJSONArray("ownedCosmetics").toString().contains("dice_heart"))
        assertEquals(0, store.balance())
        assertFalse(store.adsRemoved())
        assertFalse(CosmeticInventoryStore(context).isOwned("dice_heart"))
        assertTrue(store.hasLegacyUnverifiedSnapshot())
    }

    private fun pendingPurchase() = PendingStorePurchase(
        productId = "gems_300",
        purchaseToken = "secure-purchase-token-1234567890",
        accountId = "a".repeat(64),
    )

    private fun saveTestSession() {
        AccountSessionStore(context).save(
            AccountSession(
                accountId = ACCOUNT_ID,
                sessionId = UUID.fromString("6382a0ee-f216-46f8-b10d-51411e1f38f2").toString(),
                accessToken = "a.b.c",
                refreshToken = "lpr_" + "A".repeat(43),
                billingAccountId = "a".repeat(64),
                accessExpiresAt = System.currentTimeMillis() / 1000 + 600,
                sessionExpiresAt = System.currentTimeMillis() + 86_400_000,
                displayName = "Test",
                guestRecoveryEligible = true,
            ),
        )
    }

    private companion object {
        const val ACCOUNT_ID = "0a2c3a83-e0fb-4abc-8ac4-581a6d994fe7"
    }
}
