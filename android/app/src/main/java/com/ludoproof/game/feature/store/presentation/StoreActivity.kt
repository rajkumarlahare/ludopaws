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
import com.ludoproof.game.LudoProofTheme
import com.ludoproof.game.feature.store.data.ads.RewardedAdsController
import com.ludoproof.game.feature.store.data.billing.GooglePlayBillingGateway
import com.ludoproof.game.feature.store.data.billing.StoreInstallAccountId
import com.ludoproof.game.feature.store.data.billing.PendingStorePurchase
import com.ludoproof.game.feature.store.data.billing.PendingStorePurchaseStore
import com.ludoproof.game.feature.store.data.billing.StorePurchaseVerifier
import com.ludoproof.game.feature.store.data.local.CosmeticInventoryStore
import com.ludoproof.game.feature.store.data.local.GemWalletStore
import com.ludoproof.game.feature.store.domain.StoreCatalog
import com.ludoproof.game.feature.store.domain.StoreCosmeticCatalog
import com.ludoproof.game.feature.store.domain.StorePurchaseFulfillment
import com.ludoproof.game.feature.store.domain.model.VerifiedStorePurchase
import com.ludoproof.game.feature.store.domain.model.StoreBillingProduct
import com.ludoproof.game.feature.store.domain.model.StoreTab
import com.ludoproof.game.feature.store.presentation.components.storeContent
import com.ludoproof.game.feature.store.presentation.components.storeHeader
import com.ludoproof.game.feature.store.presentation.components.storeTabs
import java.util.concurrent.Executors

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
    private val billingExecutor =
        Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "LudoPawsStoreBilling")
        }
    private val purchaseVerifier by lazy {
        StorePurchaseVerifier()
    }
    private val pendingPurchases by lazy {
        PendingStorePurchaseStore(this)
    }
    private val installAccountId by lazy {
        StoreInstallAccountId(this).value
    }
    internal val adsEnabledForBuild: Boolean
        get() =
            BuildConfig.LUDOPROOF_ADS_ENABLED &&
                (
                    !BuildConfig.LUDOPROOF_ADS_USE_TEST_IDS ||
                        BuildConfig.DEBUG
                    )
    private val rewardedAds by lazy {
        RewardedAdsController(
            activity = this,
            enabled = adsEnabledForBuild,
            adUnitId = BuildConfig.ADMOB_REWARDED_AD_UNIT_ID,
            onMessage = ::showStoreMessage,
        )
    }

    private val billing by lazy {
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
        rewardedAds.prepare()
        if (!BuildConfig.LUDOPROOF_BILLING_ENABLED) {
            val hasPending =
                try {
                    pendingPurchases.all().isNotEmpty()
                } catch (error: Exception) {
                    Log.e(TAG, "Could not read pending Play purchases", error)
                    showStoreMessage(
                        "A pending Play purchase could not be recovered from this device. No items were granted.",
                    )
                    return
                }
            if (!hasPending) return
        }
        billingAvailable = false
        billingExecutor.execute {
            var available = false
            var availabilityError: Exception? = null
            var recoveredPurchases = 0
            var recoveryError: Exception? = null
            try {
                available = purchaseVerifier.isBillingAvailable()
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
                Log.w(TAG, "Play billing verification service is unavailable", error)
            }
            runOnUiThread {
                if (!canRenderStoreUi()) return@runOnUiThread
                billingAvailable = available
                if (
                    BuildConfig.LUDOPROOF_BILLING_ENABLED &&
                    billingAvailable
                ) {
                    billing.connect()
                } else if (BuildConfig.LUDOPROOF_BILLING_ENABLED) {
                    val message = availabilityError?.message
                        ?: "Google Play payments are not configured yet."
                    showStoreMessage(message)
                }
                if (recoveredPurchases > 0) {
                    showStoreMessage(
                        "Recovered $recoveredPurchases verified Play purchase${if (recoveredPurchases == 1) "" else "s"} from this device.",
                    )
                } else if (recoveryError != null) {
                    showStoreMessage(
                        "A previous Play payment could not be fully finalized on this device. Any saved grant is protected from duplication; we will retry when you open the store again.",
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        billing.close()
        rewardedAds.close()
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
        rewardedAds.showRewardedAd {
            try {
                if (cosmeticId == null) {
                    val balance =
                        wallet.applyRewardedAdGems(
                            REWARDED_AD_GEMS,
                        )
                    renderStore()
                    showStoreMessage(
                        "$REWARDED_AD_GEMS gems added. Your balance is $balance.",
                    )
                } else {
                    val progress =
                        cosmetics.recordVerifiedRewardedAd(
                            cosmeticId,
                        )
                    val cosmetic =
                        requireNotNull(
                            StoreCosmeticCatalog.find(cosmeticId),
                        )
                    renderStore()
                    showStoreMessage(
                        cosmetic.title +
                            " ad progress: $progress/" +
                            cosmetic.requiredAdViews +
                            ".",
                    )
                }
            } catch (error: Exception) {
                Log.e(TAG, "Could not save rewarded-ad reward", error)
                showStoreMessage(
                    "The ad reward could not be saved on this device. Please keep the app installed and contact support before watching another ad.",
                )
            }
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
        val verified =
            purchaseVerifier.verifyPurchase(
                productId = pending.productId,
                purchaseToken = pending.purchaseToken,
                accountId = pending.accountId,
            )
        check(verified) {
            "Google Play did not confirm the purchase."
        }
        return StorePurchaseFulfillment(this)
            .applyVerifiedPurchase(
                VerifiedStorePurchase(
                    productId = pending.productId,
                    purchaseToken = pending.purchaseToken,
                ),
            )
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
            ),
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
        private const val REWARDED_AD_GEMS = 5
        private const val TAG = "StoreBilling"
    }
}
