package com.ludoproof.game.feature.store.presentation

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import com.ludoproof.game.BuildConfig
import com.ludoproof.game.LudoProofApplication
import com.ludoproof.game.LudoProofTheme
import com.ludoproof.game.feature.store.data.ads.RewardedAdsController
import com.ludoproof.game.feature.store.data.billing.GooglePlayBillingGateway
import com.ludoproof.game.feature.store.data.billing.PendingStorePurchase
import com.ludoproof.game.feature.store.data.billing.PendingStorePurchaseStore
import com.ludoproof.game.feature.store.data.local.CosmeticInventoryStore
import com.ludoproof.game.feature.store.data.local.GemWalletStore
import com.ludoproof.game.feature.profile.data.local.ProfileStore
import com.ludoproof.game.feature.store.domain.StoreCosmeticCatalog
import com.ludoproof.game.feature.store.domain.model.StoreBillingProduct
import com.ludoproof.game.feature.store.domain.model.StoreTab
import com.ludoproof.game.feature.store.presentation.components.storeContent
import com.ludoproof.game.feature.store.presentation.components.storeHeader
import com.ludoproof.game.feature.store.presentation.components.storeTabs
import java.util.concurrent.Executors
import org.json.JSONObject

class StoreActivity :
    Activity(),
    GooglePlayBillingGateway.Listener {
    internal val wallet by lazy {
        GemWalletStore(this)
    }
    internal val cosmetics by lazy {
        CosmeticInventoryStore(this)
    }

    internal var selectedTab =
        StoreTab.GEMS
    internal var liveProducts:
        Map<
            String,
            StoreBillingProduct,
            > =
        emptyMap()
    private var billingAvailable = false
    private var rewardedAdsEnabledByServer = false
    private var billingWasCreated = false
    private var rewardedAdsWasCreated = false
    private val accountRepository by lazy {
        (application as LudoProofApplication).accountRepository
    }
    private val billingExecutor =
        Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "LudoPawsStoreBilling")
        }
    private val pendingPurchases by lazy {
        PendingStorePurchaseStore(this)
    }
    private val installAccountId by lazy {
        accountRepository.requireSession().billingAccountId
    }
    internal val adsEnabledForBuild: Boolean
        get() =
            BuildConfig.LUDOPROOF_ADS_ENABLED &&
                rewardedAdsEnabledByServer &&
                (
                    !BuildConfig.LUDOPROOF_ADS_USE_TEST_IDS ||
                        BuildConfig.DEBUG
                    )
    private val rewardedAds by lazy {
        rewardedAdsWasCreated = true
        RewardedAdsController(
            activity = this,
            enabled = adsEnabledForBuild,
            adUnitId = BuildConfig.ADMOB_REWARDED_AD_UNIT_ID,
            onMessage = ::showStoreMessage,
        )
    }

    private val billing by lazy {
        billingWasCreated = true
        GooglePlayBillingGateway(
            activity =
                this,
            enabled =
                BuildConfig
                    .LUDOPROOF_BILLING_ENABLED,
            accountId =
                installAccountId,
            listener =
                this,
        )
    }

    override fun onCreate(
        savedInstanceState: Bundle?,
    ) {
        super.onCreate(
            savedInstanceState,
        )
        LudoProofTheme
            .configureWindow(
                this,
            )
        selectedTab =
            requestedInitialTab()
                ?: StoreTab.GEMS
        renderStore()
    }

    override fun onStart() {
        super.onStart()
        billingAvailable = false
        billingExecutor.execute {
            var available = false
            var adsAvailable = false
            var availabilityError: Exception? = null
            var recoveredPurchases = 0
            var recoveryError: Exception? = null
            var activeAccountId: String? = null
            try {
                val session = accountRepository.ensureGuestSession()
                activeAccountId = session.accountId
                val status = accountRepository.storeStatus()
                available = BuildConfig.LUDOPROOF_BILLING_ENABLED &&
                    status.optBoolean("billingAvailable", false)
                adsAvailable = BuildConfig.LUDOPROOF_ADS_ENABLED &&
                    status.optBoolean("rewardedAdsAvailable", false)
                val serverWallet = accountRepository.wallet()
                wallet.applyServerSnapshot(serverWallet, session.accountId)
                cosmetics.applyServerSnapshot(serverWallet, session.accountId)

                // A purchase that belongs to another account is intentionally left pending.
                // It can only be recovered after that same Google-linked account is restored.
                if (available) {
                    pendingPurchases.all().forEach { pending ->
                        try {
                            val applied = verifyPurchaseAndFulfill(pending)
                            if (applied) recoveredPurchases += 1
                            pendingPurchases.remove(pending.purchaseToken)
                        } catch (error: Exception) {
                            Log.e(TAG, "Pending Play purchase verification failed", error)
                            if (recoveryError == null) recoveryError = error
                        }
                    }
                }
            } catch (error: Exception) {
                availabilityError = error
                Log.w(TAG, "Account/store bootstrap is unavailable", error)
            }
            runOnUiThread {
                if (!canRenderStoreUi()) return@runOnUiThread
                billingAvailable = available
                rewardedAdsEnabledByServer = adsAvailable
                if (BuildConfig.LUDOPROOF_BILLING_ENABLED && billingAvailable) {
                    billing.connect()
                } else if (BuildConfig.LUDOPROOF_BILLING_ENABLED) {
                    val message = availabilityError?.message
                        ?: "Google Play payments are not configured yet."
                    showStoreMessage(message)
                }
                if (adsEnabledForBuild) rewardedAds.prepare()
                renderStore()
                if (recoveredPurchases > 0) {
                    showStoreMessage(
                        "Recovered $recoveredPurchases verified Play purchase${if (recoveredPurchases == 1) "" else "s"} from this account.",
                    )
                } else if (recoveryError != null) {
                    showStoreMessage(
                        "A previous Play payment could not yet be verified for the currently signed-in account. It remains pending; restore the account that made the purchase and try again.",
                    )
                }
                if (
                    availabilityError == null &&
                    recoveryError == null &&
                    recoveredPurchases == 0 &&
                    wallet.markLegacyUnverifiedNoticeShown()
                ) {
                    showStoreMessage(
                        "Previous device-only store data was preserved for recovery, but it is not used as gems or an entitlement until verified. Your current balance and items come from the secure account wallet.",
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        if (billingWasCreated) billing.close()
        if (rewardedAdsWasCreated) rewardedAds.close()
        billingExecutor.shutdownNow()
        super.onDestroy()
    }

    internal fun selectTab(
        tab: StoreTab,
    ) {
        if (
            selectedTab ==
            tab
        ) {
            return
        }
        selectedTab =
            tab
        renderStore()
    }

    internal fun requestPurchase(
        productId: String,
    ) {
        if (
            !BuildConfig
                .LUDOPROOF_BILLING_ENABLED
        ) {
            showStoreMessage(
                "Purchases are prepared but intentionally disabled in this build. Enable them only after the Play Console products and secure purchase-verification backend are configured.",
            )
            return
        }
        if (!billingAvailable) {
            showStoreMessage(
                "Google Play payments are unavailable until purchase verification is configured.",
            )
            return
        }
        billing
            .launchPurchase(
                productId,
            )
    }

    internal fun requestRewardedAd(
        cosmeticId: String? = null,
    ) {
        if (!adsEnabledForBuild) {
            showStoreMessage("Server-verified rewarded ads are not configured for this build. No reward was requested.")
            return
        }
        val session = try {
            accountRepository.requireSession()
        } catch (error: Exception) {
            showStoreMessage(error.message ?: "Connect to the account service before requesting a reward.")
            return
        }
        val customData = JSONObject()
            .put("kind", if (cosmeticId == null) "gems" else "cosmetic")
            .apply {
                if (cosmeticId != null) put("cosmeticId", cosmeticId)
            }
            .toString()
        val initialBalance = wallet.balance()
        val initialProgress = cosmeticId?.let(cosmetics::rewardedAdProgress) ?: 0
        rewardedAds.showRewardedAd(
            userId = session.accountId,
            customData = customData,
        ) {
            refreshWalletAfterRewardedAd(
                accountId = session.accountId,
                initialBalance = initialBalance,
                cosmeticId = cosmeticId,
                initialProgress = initialProgress,
            )
        }
    }

    internal fun showAdPrivacyOptions() {
        rewardedAds.showPrivacyOptions()
    }

    override fun onCatalogLoaded(
        products:
            Map<
                String,
                StoreBillingProduct,
            >,
    ) {
        runOnUiThread {
            if (
                !canRenderStoreUi()
            ) {
                return@runOnUiThread
            }
            liveProducts =
                products
            renderStore()
        }
    }

    override fun onPurchaseRequiresVerification(
        productId: String,
        purchaseToken: String,
        accountId: String,
        onFinished: () -> Unit,
    ) {
        val pending =
            PendingStorePurchase(
                productId = productId,
                purchaseToken = purchaseToken,
                accountId = accountId,
            )
        try {
            pendingPurchases.add(pending)
        } catch (error: Exception) {
            Log.e(TAG, "Could not persist pending Play purchase", error)
            showStoreMessage(
                "Payment was received, but its verification could not be saved safely. No items were granted. Please keep the app installed and reopen the store.",
            )
            onFinished()
            return
        }
        billingExecutor.execute {
            var entitlementApplied = false
            try {
                entitlementApplied = verifyPurchaseAndFulfill(pending)
                pendingPurchases.remove(purchaseToken)
                runOnUiThread {
                    if (!canRenderStoreUi()) return@runOnUiThread
                    showStoreMessage(
                        if (entitlementApplied) {
                            "Purchase verified. Your store item is ready."
                        } else {
                            "Purchase verified. This item was already added to your profile."
                        },
                    )
                }
            } catch (error: Exception) {
                Log.e(TAG, "Store purchase verification failed", error)
                runOnUiThread {
                    if (!canRenderStoreUi()) return@runOnUiThread
                    val detail =
                        error.message?.takeIf(String::isNotBlank)
                            ?: "Please keep the app installed and try again."
                    showStoreMessage(
                        if (entitlementApplied) {
                            "Purchase was verified and the item was saved, but its recovery record could not be cleared. It will be safely checked again. $detail"
                        } else {
                            "Payment was received, but purchase verification is still pending. No items were granted yet. $detail"
                        },
                    )
                }
            } finally {
                onFinished()
            }
        }
    }

    private fun verifyPurchaseAndFulfill(
        pending: PendingStorePurchase,
    ): Boolean {
        val session = accountRepository.requireSession()
        check(session.billingAccountId == pending.accountId) {
            "This Play purchase belongs to another account. Restore that account before recovering it."
        }
        val response = accountRepository.verifyPlayPurchase(
            productId = pending.productId,
            purchaseToken = pending.purchaseToken,
        )
        check(
            response.optBoolean("ok") &&
                response.optBoolean("verified") &&
                response.optString("productId") == pending.productId,
        ) {
            "Google Play did not confirm the purchase for the signed-in account."
        }
        val serverWallet = response.optJSONObject("wallet")
            ?: throw IllegalStateException("Verified purchase did not return the authoritative wallet.")
        check(wallet.applyServerSnapshot(serverWallet, session.accountId)) {
            "Verified wallet snapshot did not match the signed-in account."
        }
        cosmetics.applyServerSnapshot(serverWallet, session.accountId)
        ProfileStore(this).addPurchase(pending.productId) // UI cache only; server remains authoritative.
        return !response.optBoolean("replay", false)
    }

    internal fun purchaseOrSelectCosmetic(cosmetic: com.ludoproof.game.feature.store.domain.model.StoreCosmetic) {
        billingExecutor.execute {
            try {
                val session = accountRepository.requireSession()
                val response = if (cosmetics.isOwned(cosmetic.id)) {
                    accountRepository.selectCosmetic(cosmetic.id)
                } else {
                    accountRepository.purchaseCosmetic(cosmetic.id)
                }
                val serverWallet = response.optJSONObject("wallet") ?: accountRepository.wallet()
                check(wallet.applyServerSnapshot(serverWallet, session.accountId)) {
                    "Server returned a wallet for a different account."
                }
                cosmetics.applyServerSnapshot(serverWallet, session.accountId)
                runOnUiThread {
                    if (!canRenderStoreUi()) return@runOnUiThread
                    renderStore()
                    showStoreMessage(
                        if (response.optBoolean("alreadyOwned", false)) {
                            "${cosmetic.title} is already owned."
                        } else {
                            "${cosmetic.title} was saved to your account."
                        },
                    )
                }
            } catch (error: Exception) {
                Log.e(TAG, "Server cosmetic purchase/select failed", error)
                val apiError = error as? com.ludoproof.game.feature.account.data.AccountApiException
                val message = when (apiError?.code) {
                    "INSUFFICIENT_GEMS" -> "You do not have enough gems to buy ${cosmetic.title}."
                    "COSMETIC_NOT_OWNED" -> "This item is not owned by the signed-in account."
                    "COSMETIC_UNAVAILABLE" -> "This item is not available yet."
                    "PROFILE_PROGRESSION_NOT_SERVER_AUTHORITATIVE" ->
                        "This unlock needs server-verified profile progression before it can be purchased."
                    else -> error.message ?: "The store could not update your account. Try again when connected."
                }
                runOnUiThread {
                    if (!canRenderStoreUi()) return@runOnUiThread
                    showStoreMessage(message)
                }
            }
        }
    }

    private fun refreshWalletAfterRewardedAd(
        accountId: String,
        initialBalance: Int,
        cosmeticId: String?,
        initialProgress: Int,
    ) {
        billingExecutor.execute {
            var latest: JSONObject? = null
            var changed = false
            for (attempt in 0 until 5) {
                try {
                    val session = accountRepository.requireSession()
                    if (session.accountId != accountId) break
                    val candidate = accountRepository.wallet()
                    latest = candidate
                    val currentBalance = candidate.optInt("balance", 0)
                    val progress = candidate.optJSONObject("rewardedAdProgress")
                        ?.optInt(cosmeticId ?: "", 0) ?: 0
                    changed = if (cosmeticId == null) {
                        currentBalance > initialBalance
                    } else {
                        progress > initialProgress
                    }
                    if (changed) break
                } catch (error: Exception) {
                    Log.w(TAG, "Waiting for server-verified ad reward", error)
                }
                if (attempt < 4) Thread.sleep(1500)
            }
            val snapshot = latest
            runOnUiThread {
                if (!canRenderStoreUi()) return@runOnUiThread
                if (snapshot != null) {
                    val session = accountRepository.currentSession()
                    if (session != null && session.accountId == accountId) {
                        wallet.applyServerSnapshot(snapshot, accountId)
                        cosmetics.applyServerSnapshot(snapshot, accountId)
                    }
                }
                renderStore()
                showStoreMessage(
                    if (changed) {
                        if (cosmeticId == null) "Your rewarded-ad gems were verified by the server."
                        else "The rewarded-ad progress for this item was verified by the server."
                    } else {
                        "The ad finished, but the server reward has not arrived yet. No local reward was granted; the store will update when verification completes."
                    },
                )
            }
        }
    }

    override fun onPurchasePending(
        productId: String,
    ) {
        runOnUiThread {
            if (
                !canRenderStoreUi()
            ) {
                return@runOnUiThread
            }
            showStoreMessage(
                "Your Google Play purchase for $productId is pending. Items will only be granted after Google confirms payment and the backend verifies it.",
            )
        }
    }

    override fun onBillingUnavailable(
        message: String,
    ) {
        runOnUiThread {
            if (!canRenderStoreUi()) return@runOnUiThread
            Toast.makeText(
                this,
                message.ifBlank { "Google Play payments are unavailable." },
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    internal fun priceFor(
        productId: String,
        fallback: String,
    ): String =
        liveProducts[
            productId
        ]
            ?.formattedPrice
            ?: if (
                BuildConfig
                    .LUDOPROOF_BILLING_ENABLED
            ) {
                "UNAVAILABLE"
            } else {
                fallback
            }

    internal fun dp(
        value: Int,
    ): Int =
        LudoProofTheme
            .dp(
                this,
                value,
            )

    private fun requestedInitialTab(): StoreTab? {
        val requested =
            intent
                ?.getStringExtra(
                    EXTRA_INITIAL_TAB,
                )
                ?.trim()
                ?.uppercase()
                ?: return null
        return StoreTab.entries
            .firstOrNull {
                it.name ==
                    requested
            }
    }

    private fun canRenderStoreUi(): Boolean =
        !isFinishing &&
            !isDestroyed

    internal fun renderStore() {
        if (
            !canRenderStoreUi()
        ) {
            return
        }

        val (root, host) =
            LudoProofTheme
                .arcadeRoot(
                    this,
                    gameplayBackground = true,
                )

        val scroll =
            ScrollView(this).apply {
                isFillViewport =
                    true
                overScrollMode =
                    View.OVER_SCROLL_NEVER
            }
        val contentHost =
            FrameLayout(this)
        scroll.addView(
            contentHost,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        val horizontalPadding =
            dp(
                if (
                    LudoProofTheme
                        .isCompactWidth(
                            this,
                        )
                ) {
                    10
                } else {
                    14
                },
            )
        val contentWidth =
            minOf(
                resources
                    .displayMetrics
                    .widthPixels -
                    horizontalPadding *
                        2,
                dp(
                    LudoProofTheme
                        .pageMaxContentWidthDp(
                            this,
                        ),
                ),
            )

        val content =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                setPadding(
                    0,
                    dp(12),
                    0,
                    dp(30),
                )
            }

        contentHost.addView(
            content,
            FrameLayout.LayoutParams(
                contentWidth,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or
                    Gravity.CENTER_HORIZONTAL,
            ),
        )
        host.addView(
            scroll,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )

        content.addView(
            storeHeader(),
        )
        content.addView(
            storeTabs(),
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(58),
            ).apply {
                // Separate category tabs from the back button and gem/plus pill above.
                topMargin = dp(14)
            },
        )
        content.addView(
            storeContent(),
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin =
                    dp(12)
            },
        )

        setContentView(
            root,
        )
    }

    internal fun showStoreMessage(
        message: String,
    ) {
        if (
            !canRenderStoreUi()
        ) {
            return
        }

        AlertDialog
            .Builder(this)
            .setTitle(
                "Ludo Paws Store",
            )
            .setMessage(
                message,
            )
            .setPositiveButton(
                "OK",
                null,
            )
            .show()
    }

    companion object {
        const val EXTRA_INITIAL_TAB =
            "ludo_paws_store_initial_tab"
        private const val TAG = "StoreBilling"
    }
}
