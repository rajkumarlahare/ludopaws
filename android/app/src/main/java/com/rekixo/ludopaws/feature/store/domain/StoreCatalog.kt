package com.rekixo.ludopaws.feature.store.domain

import com.rekixo.ludopaws.feature.store.domain.model.StoreProduct
import com.rekixo.ludopaws.feature.store.domain.model.StoreProductKind

object StoreCatalog {
    const val REMOVE_ADS =
        "remove_ads"
    const val PAWS_PLUS =
        "paws_plus"
    const val PAWS_PLUS_BASE_PLAN_ID =
        "monthly-auto"
    const val GEMS_300 =
        "gems_300"
    const val GEMS_1500 =
        "gems_1500"
    const val GEMS_3500 =
        "gems_3500"
    const val GEMS_20000 =
        "gems_20000"
    const val GEMS_40000 =
        "gems_40000"
    const val GEMS_200000 =
        "gems_200000"

    val products =
        listOf(
            StoreProduct(
                productId =
                    REMOVE_ADS,
                title =
                    "Remove Ads",
                gemAmount =
                    5,
                kind =
                    StoreProductKind.NON_CONSUMABLE_REMOVE_ADS,
                fallbackPrice =
                    "₹99.00",
                featured =
                    true,
            ),
            StoreProduct(
                productId =
                    PAWS_PLUS,
                title =
                    "Paws Plus · Monthly",
                gemAmount =
                    0,
                kind =
                    StoreProductKind.SUBSCRIPTION_PAWS_PLUS,
                fallbackPrice =
                    "Google Play pricing",
            ),
            StoreProduct(
                productId =
                    GEMS_300,
                title =
                    "300 Gems · Starter",
                gemAmount =
                    300,
                kind =
                    StoreProductKind.CONSUMABLE_GEMS,
                fallbackPrice =
                    "₹29.00",
            ),
            StoreProduct(
                productId =
                    GEMS_1500,
                title =
                    "1,200 Gems · Value",
                gemAmount =
                    1_200,
                kind =
                    StoreProductKind.CONSUMABLE_GEMS,
                fallbackPrice =
                    "₹79.00",
            ),
            StoreProduct(
                productId =
                    GEMS_3500,
                title =
                    "3,000 Gems · Popular",
                gemAmount =
                    3_000,
                kind =
                    StoreProductKind.CONSUMABLE_GEMS,
                fallbackPrice =
                    "₹179.00",
            ),
            StoreProduct(
                productId =
                    GEMS_20000,
                title =
                    "7,500 Gems",
                gemAmount =
                    7_500,
                kind =
                    StoreProductKind.CONSUMABLE_GEMS,
                fallbackPrice =
                    "₹399.00",
            ),
            StoreProduct(
                productId =
                    GEMS_40000,
                title =
                    "16,000 Gems",
                gemAmount =
                    16_000,
                kind =
                    StoreProductKind.CONSUMABLE_GEMS,
                fallbackPrice =
                    "₹799.00",
            ),
            StoreProduct(
                productId =
                    GEMS_200000,
                title =
                    "35,000 Gems · Best Value",
                gemAmount =
                    35_000,
                kind =
                    StoreProductKind.CONSUMABLE_GEMS,
                fallbackPrice =
                    "₹1,499.00",
            ),
        )

    val playProductIds:
        List<String> =
        products.filter {
            it.kind != StoreProductKind.SUBSCRIPTION_PAWS_PLUS
        }.map {
            it.productId
        }

    val subscriptionProductIds: List<String> =
        products.filter {
            it.kind == StoreProductKind.SUBSCRIPTION_PAWS_PLUS
        }.map {
            it.productId
        }

    fun find(
        productId: String,
    ): StoreProduct? =
        products.firstOrNull {
            it.productId ==
                productId
        }
}
