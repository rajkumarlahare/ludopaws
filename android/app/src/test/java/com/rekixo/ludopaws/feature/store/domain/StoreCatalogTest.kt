package com.rekixo.ludopaws.feature.store.domain

import com.rekixo.ludopaws.feature.store.domain.model.StoreProductKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreCatalogTest {
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
            "Price set by Google Play",
            product.fallbackPrice,
        )
    }

    @Test
    fun gemPacksMatchTheRecommendedLaunchLadder() {
        val expected =
            listOf(
                StoreCatalog.GEMS_300 to (300 to "₹29.00"),
                StoreCatalog.GEMS_1500 to (1_200 to "₹79.00"),
                StoreCatalog.GEMS_3500 to (3_000 to "₹179.00"),
                StoreCatalog.GEMS_20000 to (7_500 to "₹399.00"),
                StoreCatalog.GEMS_40000 to (16_000 to "₹799.00"),
                StoreCatalog.GEMS_200000 to (35_000 to "₹1,499.00"),
            )

        expected.forEach { (productId, offer) ->
            val product =
                requireNotNull(
                    StoreCatalog.find(productId),
                )
            assertEquals(offer.first, product.gemAmount)
            assertEquals(offer.second, product.fallbackPrice)
        }
    }
}
