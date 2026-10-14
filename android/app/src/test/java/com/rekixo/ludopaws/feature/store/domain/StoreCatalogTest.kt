package com.rekixo.ludopaws.feature.store.domain

import com.rekixo.ludopaws.feature.store.domain.model.StoreProductKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreCatalogTest {
    @Test
    fun storeOffersOnlyOneTimeProductsAndKeepsRemoveAdsAndGemPacks() {
        assertEquals(
            listOf(
                StoreCatalog.REMOVE_ADS,
                StoreCatalog.GEMS_300,
                StoreCatalog.GEMS_1500,
                StoreCatalog.GEMS_3500,
                StoreCatalog.GEMS_20000,
                StoreCatalog.GEMS_40000,
                StoreCatalog.GEMS_200000,
            ),
            StoreCatalog.playProductIds,
        )
        assertTrue(StoreCatalog.products.none { it.productId.contains("plus", ignoreCase = true) })
    }

    @Test
    fun productIdsAreUnique() {
        val ids =
            StoreCatalog
                .products
                .map {
                    it.productId
                }

        assertEquals(
            ids.size,
            ids.toSet()
                .size,
        )
    }

    @Test
    fun gemPacksHavePositiveAmounts() {
        val gemPacks =
            StoreCatalog
                .products
                .filter {
                    it.kind ==
                        StoreProductKind
                            .CONSUMABLE_GEMS
                }

        assertTrue(
            gemPacks.isNotEmpty(),
        )
        assertTrue(
            gemPacks.all {
                it.gemAmount >
                    0 &&
                    it.fallbackPrice
                        .isNotBlank()
            },
        )
    }

    @Test
    fun removeAdsIsNonConsumable() {
        val product =
            requireNotNull(
                StoreCatalog
                    .find(
                        StoreCatalog
                            .REMOVE_ADS,
                    ),
            )

        assertEquals(
            StoreProductKind
                .NON_CONSUMABLE_REMOVE_ADS,
            product.kind,
        )
        assertTrue(
            product.featured,
        )
        assertEquals(
            "Price unavailable",
            product.fallbackPrice,
        )
    }

    @Test
    fun gemPacksMatchTheRecommendedLaunchLadder() {
        val expected =
            listOf(
                StoreCatalog.GEMS_300 to 300,
                StoreCatalog.GEMS_1500 to 1_200,
                StoreCatalog.GEMS_3500 to 3_000,
                StoreCatalog.GEMS_20000 to 7_500,
                StoreCatalog.GEMS_40000 to 16_000,
                StoreCatalog.GEMS_200000 to 35_000,
            )

        expected.forEach { (productId, gemAmount) ->
            val product =
                requireNotNull(
                    StoreCatalog.find(productId),
                )
            assertEquals(gemAmount, product.gemAmount)
            assertEquals("Price unavailable", product.fallbackPrice)
        }
    }

    @Test
    fun everyProductUsesPlayPriceInsteadOfHardcodedRegionalFallback() {
        assertTrue(
            StoreCatalog.products.all {
                it.fallbackPrice == "Price unavailable"
            },
        )
    }
}
