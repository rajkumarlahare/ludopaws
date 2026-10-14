package com.rekixo.ludopaws.feature.store.data

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreCatalogParserTest {
    @Test
    fun overlaysPublishedMetadataForAnExistingRenderer() {
        val snapshot = StoreCatalogParser.parse(
            catalogResponse(
                items = arrayOf(
                    catalogItem(
                        id = "dice_heart",
                        category = "DICE",
                        name = "HEART FESTIVAL",
                        channels = arrayOf("GEMS"),
                        gemPrice = 175,
                    ),
                ),
                version = 7,
            ),
            fetchedAtMillis = 1_800_000_000_000L,
        )

        val item = snapshot.items.single()
        assertEquals("dice_heart", item.id)
        assertEquals("HEART FESTIVAL", item.title)
        assertEquals(175, item.gemPrice)
        assertEquals(7, item.catalogVersion)
        assertTrue(item.remoteManaged)
        assertTrue(item.contentAvailable)
        assertEquals("DICE", item.backendCategory)
    }

    @Test
    fun unknownPublishedItemsAreDiscoverableButRemainLockedWithoutALocalRenderer() {
        val snapshot = StoreCatalogParser.parse(
            catalogResponse(
                items = arrayOf(
                    catalogItem(
                        id = "dice_safari_01",
                        category = "DICE",
                        name = "SAFARI DICE",
                        channels = arrayOf("GEMS", "REWARDED_ADS"),
                        gemPrice = 80,
                        requiredCompletedAds = 3,
                    ),
                ),
            ),
        )

        val item = snapshot.items.single()
        assertEquals("SAFARI DICE", item.title)
        assertTrue(item.remoteManaged)
        assertFalse(item.contentAvailable)
        assertEquals(2, item.unlockChannels.size)
        assertEquals(80, item.gemPrice)
        assertEquals(3, item.requiredAdViews)
    }

    @Test
    fun suppressionIdsHideBundledLegacyOffersAndIgnoreMalformedIds() {
        val snapshot = StoreCatalogParser.parse(
            catalogResponse(
                items = arrayOf(
                    catalogItem("dice_safari_01", "DICE", "SAFARI DICE", arrayOf("FREE")),
                ),
                suppressedIds = arrayOf("dice_pumpkin", "not an id", "../dice_heart", "dice_safari_01"),
            ),
        )

        assertEquals(setOf("dice_pumpkin"), snapshot.suppressedIds)
    }

    @Test
    fun mergesDynamicItemsAndSuppressesKnownLegacyItems() {
        val snapshot = StoreCatalogParser.parse(
            catalogResponse(
                items = arrayOf(
                    catalogItem("dice_safari_01", "DICE", "SAFARI DICE", arrayOf("GEMS"), gemPrice = 80),
                ),
                suppressedIds = arrayOf("dice_pumpkin"),
            ),
        )

        try {
            com.rekixo.ludopaws.feature.store.domain.StoreCosmeticCatalog
                .replaceRemoteCatalog(snapshot.items, snapshot.suppressedIds)

            val dynamicItem = com.rekixo.ludopaws.feature.store.domain.StoreCosmeticCatalog
                .dice.firstOrNull { it.id == "dice_safari_01" }
            assertNotNull(dynamicItem)
            assertFalse(requireNotNull(dynamicItem).contentAvailable)
            assertFalse(com.rekixo.ludopaws.feature.store.domain.StoreCosmeticCatalog.find("dice_pumpkin") != null)
            assertNotNull(com.rekixo.ludopaws.feature.store.domain.StoreCosmeticCatalog.find("dice_classic"))
        } finally {
            com.rekixo.ludopaws.feature.store.domain.StoreCosmeticCatalog.clearRemoteCatalog()
        }
    }

    @Test
    fun rejectsPublishedResponsesWithInvalidOrDuplicateItemIds() {
        val invalid = catalogItem(id = "INVALID", category = "DICE", name = "BAD", channels = arrayOf("FREE"))
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            StoreCatalogParser.parse(catalogResponse(items = arrayOf(invalid)))
        }

        val duplicate = catalogItem("dice_heart", "DICE", "HEART", arrayOf("GEMS"), gemPrice = 20)
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            StoreCatalogParser.parse(catalogResponse(items = arrayOf(duplicate, duplicate)))
        }
    }

    @Test
    fun staleCacheCanBeMarkedDisplayOnlyByCaller() {
        val parsed = StoreCatalogParser.parse(
            catalogResponse(
                items = arrayOf(
                    catalogItem("dice_heart", "DICE", "HEART", arrayOf("GEMS"), gemPrice = 100),
                ),
            ),
        )
        val stale = parsed.copy(items = parsed.items.map { it.copy(contentAvailable = false) }, stale = true)

        assertTrue(stale.stale)
        assertFalse(stale.items.single().contentAvailable)
    }

    private fun catalogResponse(
        items: Array<JSONObject> = emptyArray(),
        suppressedIds: Array<String> = emptyArray(),
        version: Int = 4,
    ) = JSONObject()
        .put("ok", true)
        .put("catalogVersion", version)
        .put("updatedAt", "2026-10-14T12:00:00.000Z")
        .put("items", JSONArray().apply { items.forEach { item -> put(item) } })
        .put("suppressedIds", JSONArray().apply { suppressedIds.forEach { id -> put(id) } })

    private fun catalogItem(
        id: String,
        category: String,
        name: String,
        channels: Array<String>,
        gemPrice: Int = 0,
        requiredCompletedAds: Int = 0,
    ) = JSONObject()
        .put("id", id)
        .put("category", category)
        .put("name", name)
        .put("description", "Catalog test item")
        .put("status", "PUBLISHED")
        .put("contentReady", true)
        .put(
            "asset",
            JSONObject()
                .put("thumbnail", "r2://published/$id/v1/thumbnail.png")
                .put("manifest", "r2://published/$id/v1/manifest.json")
                .put("version", 1)
                .put("sha256", "a".repeat(64))
                .put("format", "BUILT_IN"),
        )
        .put(
            "unlock",
            JSONObject()
                .put("channels", JSONArray().apply { channels.forEach { channel -> put(channel) } })
                .apply {
                    if ("GEMS" in channels) {
                        put("gems", JSONObject().put("enabled", true).put("price", gemPrice))
                    }
                    if ("REWARDED_ADS" in channels) {
                        put(
                            "rewardedAds",
                            JSONObject().put("enabled", true).put("requiredCompletedAds", requiredCompletedAds),
                        )
                    }
                },
        )
        .put(
            "availability",
            JSONObject()
                .put("worldwide", true)
                .put("allowedCountries", JSONArray())
                .put("blockedCountries", JSONArray())
                .put("startsAt", JSONObject.NULL)
                .put("endsAt", JSONObject.NULL),
        )
        .put("catalogVersion", 1)
}
