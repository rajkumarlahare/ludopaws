package com.rekixo.ludopaws.feature.store.data.billing

import android.app.Activity
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.rekixo.ludopaws.feature.store.domain.StoreCatalog
import com.rekixo.ludopaws.feature.store.domain.model.StoreProductKind
import com.rekixo.ludopaws.feature.store.domain.model.StoreBillingProduct
import java.util.concurrent.ConcurrentHashMap

class GooglePlayBillingGateway(
    private val activity: Activity,
    private val enabled: Boolean,
    private val subscriptionsEnabled: Boolean = false,
    accountId: String,
    private val listener: Listener,
) : PurchasesUpdatedListener {
    @Volatile
    private var currentBillingAccountId: String = accountId

    private val productDetails =
        linkedMapOf<
            String,
            ProductDetails,
            >()

    @Volatile
    private var closed =
        false
    private val processingPurchaseTokens =
        ConcurrentHashMap.newKeySet<String>()

    private val billingClient =
        BillingClient
            .newBuilder(
                activity.applicationContext,
            )
            .setListener(
                this,
            )
            .enablePendingPurchases(
                PendingPurchasesParams
                    .newBuilder()
                    .enableOneTimeProducts()
                    .build(),
            )
            .enableAutoServiceReconnection()
            .build()

    /**
     * Refreshes purchase attribution when the active Ludo Paws account changes.
     * The caller must pass the account binding returned by the authenticated backend.
     */
    fun updateAccountId(newAccountId: String) {
        require(OBFUSCATED_ACCOUNT_ID_REGEX.matches(newAccountId)) {
            "Google Play account binding is invalid."
        }
        currentBillingAccountId = newAccountId
    }

    fun connect() {
        if (
            closed
        ) {
            return
        }

        if (!enabled) {
            listener
                .onBillingUnavailable(
                    "Google Play purchases are not enabled for this build yet.",
                )
            return
        }

        if (
            billingClient
                .isReady
        ) {
            queryProducts()
            queryExistingPurchases()
            return
        }

        billingClient
            .startConnection(
                object :
                    BillingClientStateListener {
                    override fun onBillingSetupFinished(
                        billingResult:
                            BillingResult,
                    ) {
                        if (
                            closed
                        ) {
                            return
                        }

                        if (
                            billingResult.responseCode ==
                            BillingClient
                                .BillingResponseCode
                                .OK
                        ) {
                            queryProducts()
                            queryExistingPurchases()
                        } else {
                            listener
                                .onBillingUnavailable(
                                    billingResult
                                        .debugMessage
                                        .ifBlank {
                                            "Google Play Billing is unavailable."
                                        },
                                )
                        }
                    }

                    override fun onBillingServiceDisconnected() {
                        if (
                            closed
                        ) {
                            return
                        }
                        listener
                            .onBillingUnavailable(
                                "Google Play Billing connection was interrupted.",
                            )
                    }
                },
            )
    }

    fun launchPurchase(
        productId: String,
    ) {
        if (
            closed
        ) {
            return
        }

        if (!enabled) {
            listener
                .onBillingUnavailable(
                    "Purchases are disabled until Play Console products and secure verification are configured.",
                )
            return
        }

        val details =
            productDetails[
                productId
            ]
        if (details == null) {
            listener
                .onBillingUnavailable(
                    "This product is not available from Google Play yet.",
                )
            queryProducts()
            return
        }

        val isSubscription = productId in StoreCatalog.subscriptionProductIds
        if (isSubscription && !subscriptionsEnabled) {
            listener.onBillingUnavailable("Paws Plus subscriptions are not enabled for this build and server yet.")
            return
        }
        val offerToken = if (isSubscription) {
            details.subscriptionOfferDetails
                ?.firstOrNull { it.basePlanId == StoreCatalog.PAWS_PLUS_BASE_PLAN_ID && it.offerId == null }
                ?.offerToken
                ?: details.subscriptionOfferDetails
                    ?.firstOrNull { it.basePlanId == StoreCatalog.PAWS_PLUS_BASE_PLAN_ID }
                    ?.offerToken
        } else {
            details.oneTimePurchaseOfferDetailsList?.firstOrNull()?.offerToken
        }
        if (offerToken.isNullOrBlank()) {
            listener.onBillingUnavailable(
                if (isSubscription) {
                    "Google Play has no eligible monthly Paws Plus offer. Check the paws_plus / monthly-auto Play Console product."
                } else {
                    "No eligible Google Play one-time offer is available for this product."
                },
            )
            return
        }

        val productParams =
            BillingFlowParams
                .ProductDetailsParams
                .newBuilder()
                .setProductDetails(
                    details,
                )
                .setOfferToken(
                    offerToken,
                )
                .build()

        val result =
            billingClient
                .launchBillingFlow(
                    activity,
                    BillingFlowParams
                        .newBuilder()
                        .setObfuscatedAccountId(currentBillingAccountId)
                        .setProductDetailsParamsList(
                            listOf(
                                productParams,
                            ),
                        )
                        .build(),
                )

        if (
            result.responseCode !=
            BillingClient
                .BillingResponseCode
                .OK
        ) {
            listener
                .onBillingUnavailable(
                    result.debugMessage
                        .ifBlank {
                            "Google Play purchase screen could not be opened."
                        },
                )
        }
    }

    fun close() {
        if (
            closed
        ) {
            return
        }
        closed =
            true
        productDetails.clear()
        runCatching {
            billingClient
                .endConnection()
        }
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?,
    ) {
        if (
            closed
        ) {
            return
        }

        when (
            billingResult.responseCode
        ) {
            BillingClient
                .BillingResponseCode
                .OK -> {
                val (subscriptionPurchases, oneTimePurchases) =
                    purchases.orEmpty().partition { purchase ->
                        StoreCatalog.PAWS_PLUS in purchase.products
                    }
                processPurchases(oneTimePurchases, isSubscription = false)
                if (subscriptionPurchases.isNotEmpty()) {
                    if (subscriptionsEnabled) {
                        processPurchases(subscriptionPurchases, isSubscription = true)
                    } else {
                        listener.onBillingUnavailable("Paws Plus subscription verification is not enabled for this build.")
                    }
                }
            }
            BillingClient
                .BillingResponseCode
                .USER_CANCELED ->
                Unit

            else ->
                listener
                    .onBillingUnavailable(
                        billingResult
                            .debugMessage
                            .ifBlank {
                                "Google Play purchase failed."
                            },
                    )
        }
    }

    private fun queryExistingPurchases() {
        queryExistingPurchases(
            productType = BillingClient.ProductType.INAPP,
            isSubscription = false,
        ) {
            if (subscriptionsEnabled && !closed) {
                queryExistingPurchases(
                    productType = BillingClient.ProductType.SUBS,
                    isSubscription = true,
                ) {}
            }
        }
    }

    private fun queryExistingPurchases(
        productType: String,
        isSubscription: Boolean,
        onFinished: () -> Unit,
    ) {
        if (closed || !billingClient.isReady) {
            onFinished()
            return
        }
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(productType).build(),
        ) { billingResult, purchases ->
            if (closed) {
                onFinished()
                return@queryPurchasesAsync
            }
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                listener.onBillingUnavailable(
                    billingResult.debugMessage.ifBlank {
                        if (isSubscription) {
                            "Google Play could not restore Paws Plus subscriptions."
                        } else {
                            "Google Play could not reload previous purchases."
                        }
                    },
                )
                onFinished()
                return@queryPurchasesAsync
            }
            processPurchases(purchases, isSubscription)
            onFinished()
        }
    }

    private fun processPurchases(
        purchases: List<Purchase>,
        isSubscription: Boolean = false,
    ) {
        purchases.forEach { purchase ->
            when (purchase.purchaseState) {
                Purchase.PurchaseState.PURCHASED -> {
                    val productId = purchase.products.singleOrNull()
                    val isPawsPlus = productId == StoreCatalog.PAWS_PLUS
                    if (purchase.products.size != 1 || (isSubscription && !isPawsPlus) || (!isSubscription && isPawsPlus)) {
                        listener.onBillingUnavailable(
                            "A Google Play purchase has an unsupported product shape; no entitlement was granted.",
                        )
                        return@forEach
                    }
                    val purchaseAccountId = purchase.accountIdentifiers?.obfuscatedAccountId
                    val restorableNonConsumable =
                        !isSubscription &&
                            productId?.let(StoreCatalog::find)?.kind ==
                            StoreProductKind.NON_CONSUMABLE_REMOVE_ADS
                    val validPurchaseAccountId = purchaseAccountId?.takeIf(OBFUSCATED_ACCOUNT_ID_REGEX::matches)
                    if (purchaseAccountId == null && restorableNonConsumable) {
                        listener.onBillingUnavailable(
                            "This Remove Ads purchase has no recoverable Ludo Paws account binding. Restore the account that made the purchase; no entitlement was granted.",
                        )
                        return@forEach
                    }
                    if (purchaseAccountId != currentBillingAccountId && !restorableNonConsumable) {
                        listener.onBillingUnavailable(
                            "A Play purchase could not be matched to this app account; no content was granted.",
                        )
                        return@forEach
                    }
                    if (validPurchaseAccountId == null || !processingPurchaseTokens.add(purchase.purchaseToken)) {
                        if (validPurchaseAccountId == null) {
                            listener.onBillingUnavailable(
                                "This Play purchase has an invalid Ludo Paws account binding. No content was granted.",
                            )
                        }
                        return@forEach
                    }
                    if (isSubscription) {
                        listener.onSubscriptionRequiresVerification(
                            productId = StoreCatalog.PAWS_PLUS,
                            purchaseToken = purchase.purchaseToken,
                            accountId = validPurchaseAccountId,
                        ) {
                            processingPurchaseTokens.remove(purchase.purchaseToken)
                        }
                    } else {
                        listener.onPurchaseRequiresVerification(
                            productId = requireNotNull(productId),
                            purchaseToken = purchase.purchaseToken,
                            accountId = validPurchaseAccountId,
                        ) {
                            processingPurchaseTokens.remove(purchase.purchaseToken)
                        }
                    }
                }

                Purchase.PurchaseState.PENDING ->
                    purchase.products.forEach { productId ->
                        listener.onPurchasePending(productId)
                    }

                else -> Unit
            }
        }
    }

    private fun queryProducts() {
        if (closed) return
        productDetails.clear()
        queryProductType(
            productIds = StoreCatalog.playProductIds,
            productType = BillingClient.ProductType.INAPP,
        ) { oneTimeProducts ->
            val combined = linkedMapOf<String, StoreBillingProduct>().apply {
                putAll(oneTimeProducts)
            }
            if (!subscriptionsEnabled || closed) {
                listener.onCatalogLoaded(combined)
                return@queryProductType
            }
            queryProductType(
                productIds = StoreCatalog.subscriptionProductIds,
                productType = BillingClient.ProductType.SUBS,
            ) { subscriptionProducts ->
                combined.putAll(subscriptionProducts)
                if (!closed) listener.onCatalogLoaded(combined)
            }
        }
    }

    private fun queryProductType(
        productIds: List<String>,
        productType: String,
        onComplete: (Map<String, StoreBillingProduct>) -> Unit,
    ) {
        if (closed || productIds.isEmpty()) {
            onComplete(emptyMap())
            return
        }
        val products = productIds.map { productId ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(productType)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(products)
            .build()
        billingClient.queryProductDetailsAsync(params) { billingResult, result ->
            if (closed) return@queryProductDetailsAsync
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                listener.onBillingUnavailable(
                    billingResult.debugMessage.ifBlank { "Google Play catalog could not be loaded." },
                )
                onComplete(emptyMap())
                return@queryProductDetailsAsync
            }
            val mapped = linkedMapOf<String, StoreBillingProduct>()
            result.productDetailsList.forEach { details ->
                val formattedPrice = if (productType == BillingClient.ProductType.SUBS) {
                    val matchingOffers = details.subscriptionOfferDetails
                        .orEmpty()
                        .filter { it.basePlanId == StoreCatalog.PAWS_PLUS_BASE_PLAN_ID }
                    val offer = matchingOffers.firstOrNull { it.offerId == null }
                        ?: matchingOffers.firstOrNull()
                    offer?.pricingPhases?.pricingPhaseList?.lastOrNull()?.formattedPrice
                } else {
                    details.oneTimePurchaseOfferDetailsList?.firstOrNull()?.formattedPrice
                } ?: return@forEach
                productDetails[details.productId] = details
                mapped[details.productId] = StoreBillingProduct(
                    productId = details.productId,
                    formattedPrice = formattedPrice,
                )
            }
            onComplete(mapped)
        }
    }

    private companion object {
        val OBFUSCATED_ACCOUNT_ID_REGEX = Regex("^[a-f0-9]{64}$")
    }

    interface Listener {
        fun onCatalogLoaded(
            products:
                Map<
                    String,
                    StoreBillingProduct,
                    >,
        )

        fun onPurchaseRequiresVerification(
            productId: String,
            purchaseToken: String,
            accountId: String,
            onFinished: () -> Unit,
        )

        fun onSubscriptionRequiresVerification(
            productId: String,
            purchaseToken: String,
            accountId: String,
            onFinished: () -> Unit,
        )

        fun onPurchasePending(
            productId: String,
        )

        fun onBillingUnavailable(
            message: String,
        )
    }
}
