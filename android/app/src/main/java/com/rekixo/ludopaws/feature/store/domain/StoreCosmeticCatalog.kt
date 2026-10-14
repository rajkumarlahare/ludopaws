package com.rekixo.ludopaws.feature.store.domain

import com.rekixo.ludopaws.feature.store.domain.model.CosmeticCategory
import com.rekixo.ludopaws.feature.store.domain.model.CosmeticUnlockKind
import com.rekixo.ludopaws.feature.store.domain.model.StoreCosmetic

object StoreCosmeticCatalog {
    const val DEFAULT_BOARD =
        "board_classic"
    const val DEFAULT_DICE =
        "dice_classic"
    const val DEFAULT_AVATAR =
        "avatar_4"
    const val DEFAULT_CHARACTER_PACK =
        "character_pack_starter_paws"

    private val builtInBoards =
        listOf(
            cosmetic(
                id = "board_classic",
                title = "CLASSIC",
                category = CosmeticCategory.BOARD,
                unlockKind = CosmeticUnlockKind.FREE,
                previewSymbol = "",
                previewPrimary = 0xFFF7F4EC.toInt(),
                previewSecondary = 0xFFF7F4EC.toInt(),
            ),
            cosmetic(
                id = "board_checkers",
                title = "LOCKED BOARD",
                category = CosmeticCategory.BOARD,
                unlockKind = CosmeticUnlockKind.FREE,
                previewSymbol = "",
                previewPrimary = 0xFFF7F4EC.toInt(),
                previewSecondary = 0xFFF7F4EC.toInt(),
                contentAvailable = false,
            ),
            cosmetic(
                id = "board_chess",
                title = "LOCKED BOARD",
                category = CosmeticCategory.BOARD,
                unlockKind = CosmeticUnlockKind.REWARDED_ADS,
                previewSymbol = "",
                previewPrimary = 0xFFF7F4EC.toInt(),
                previewSecondary = 0xFFF7F4EC.toInt(),
                ads = 5,
                contentAvailable = false,
            ),
            cosmetic(
                id = "board_diwali",
                title = "LOCKED BOARD",
                category = CosmeticCategory.BOARD,
                unlockKind = CosmeticUnlockKind.REWARDED_ADS,
                previewSymbol = "",
                previewPrimary = 0xFFF7F4EC.toInt(),
                previewSecondary = 0xFFF7F4EC.toInt(),
                ads = 10,
                contentAvailable = false,
            ),
            cosmetic(
                id = "board_denim",
                title = "LOCKED BOARD",
                category = CosmeticCategory.BOARD,
                unlockKind = CosmeticUnlockKind.REWARDED_ADS,
                previewSymbol = "",
                previewPrimary = 0xFFF7F4EC.toInt(),
                previewSecondary = 0xFFF7F4EC.toInt(),
                ads = 10,
                contentAvailable = false,
            ),
            cosmetic(
                id = "board_neon",
                title = "LOCKED BOARD",
                category = CosmeticCategory.BOARD,
                unlockKind = CosmeticUnlockKind.REWARDED_ADS,
                previewSymbol = "",
                previewPrimary = 0xFFF7F4EC.toInt(),
                previewSecondary = 0xFFF7F4EC.toInt(),
                level = 10,
                ads = 20,
                contentAvailable = false,
            ),
            cosmetic(
                id = "board_pirate",
                title = "LOCKED BOARD",
                category = CosmeticCategory.BOARD,
                unlockKind = CosmeticUnlockKind.GEMS,
                previewSymbol = "",
                previewPrimary = 0xFFF7F4EC.toInt(),
                previewSecondary = 0xFFF7F4EC.toInt(),
                gems = 200,
                level = 16,
                contentAvailable = false,
            ),
            cosmetic(
                id = "board_alien",
                title = "LOCKED BOARD",
                category = CosmeticCategory.BOARD,
                unlockKind = CosmeticUnlockKind.GEMS,
                previewSymbol = "",
                previewPrimary = 0xFFF7F4EC.toInt(),
                previewSecondary = 0xFFF7F4EC.toInt(),
                gems = 250,
                level = 22,
                contentAvailable = false,
            ),
            cosmetic(
                id = "board_penguin",
                title = "LOCKED BOARD",
                category = CosmeticCategory.BOARD,
                unlockKind = CosmeticUnlockKind.GEMS,
                previewSymbol = "",
                previewPrimary = 0xFFF7F4EC.toInt(),
                previewSecondary = 0xFFF7F4EC.toInt(),
                gems = 250,
                contentAvailable = false,
            ),
        )

    private val builtInDice =
        listOf(
            cosmetic("dice_classic","CLASSIC",CosmeticCategory.DICE,CosmeticUnlockKind.FREE,"⚄",0xFFF7F7F7.toInt(),0xFF1B2838.toInt()),
            cosmetic("dice_pumpkin","PUMPKIN",CosmeticCategory.DICE,CosmeticUnlockKind.FREE,"1",0xFFD9802F.toInt(),0xFF6A351C.toInt()),
            cosmetic("dice_diwali","DIWALI",CosmeticCategory.DICE,CosmeticUnlockKind.REWARDED_ADS,"6",0xFFB21C32.toInt(),0xFFFFC832.toInt(),ads=5),
            cosmetic("dice_football","FOOTBALL",CosmeticCategory.DICE,CosmeticUnlockKind.REWARDED_ADS,"6",0xFFF5F5F5.toInt(),0xFF222222.toInt(),ads=10),
            cosmetic("dice_cricket","CRICKET",CosmeticCategory.DICE,CosmeticUnlockKind.REWARDED_ADS,"1",0xFF4F9E5F.toInt(),0xFFE8F4D8.toInt(),ads=20),
            cosmetic("dice_summers","SUMMERS",CosmeticCategory.DICE,CosmeticUnlockKind.REWARDED_ADS,"3",0xFFFFF1D4.toInt(),0xFFE84432.toInt(),ads=30),
            cosmetic("dice_colors","COLORS",CosmeticCategory.DICE,CosmeticUnlockKind.GEMS,"3",0xFFFFD63B.toInt(),0xFFEF3F8A.toInt(),gems=200),
            cosmetic("dice_heart","HEART",CosmeticCategory.DICE,CosmeticUnlockKind.GEMS,"♥",0xFFF8F8F8.toInt(),0xFFD82E3D.toInt(),gems=250),
        )

    private val builtInAvatars =
        listOf(
            cosmetic("avatar_9","Avatar 9",CosmeticCategory.AVATAR,CosmeticUnlockKind.FREE,"A9",0xFF1E91E8.toInt(),0xFF073F97.toInt()),
            cosmetic("avatar_4","Avatar 4",CosmeticCategory.AVATAR,CosmeticUnlockKind.FREE,"A4",0xFF1E91E8.toInt(),0xFF073F97.toInt()),
            cosmetic("avatar_3","Avatar 3",CosmeticCategory.AVATAR,CosmeticUnlockKind.FREE,"A3",0xFF1E91E8.toInt(),0xFF073F97.toInt()),
            cosmetic("avatar_5","Avatar 5",CosmeticCategory.AVATAR,CosmeticUnlockKind.FREE,"A5",0xFF1E91E8.toInt(),0xFF073F97.toInt()),
            cosmetic("avatar_6","Avatar 6",CosmeticCategory.AVATAR,CosmeticUnlockKind.FREE,"A6",0xFF1E91E8.toInt(),0xFF073F97.toInt()),
            cosmetic("avatar_7","Avatar 7",CosmeticCategory.AVATAR,CosmeticUnlockKind.FREE,"A7",0xFF1E91E8.toInt(),0xFF073F97.toInt()),
            cosmetic("avatar_1","Avatar 1",CosmeticCategory.AVATAR,CosmeticUnlockKind.FREE,"A1",0xFF1E91E8.toInt(),0xFF073F97.toInt()),
            cosmetic("avatar_8","Avatar 8",CosmeticCategory.AVATAR,CosmeticUnlockKind.FREE,"A8",0xFF1E91E8.toInt(),0xFF073F97.toInt()),
            cosmetic("avatar_2","Avatar 2",CosmeticCategory.AVATAR,CosmeticUnlockKind.GEMS,"A2",0xFF7D3FC1.toInt(),0xFF33206E.toInt(),gems=500),
            cosmetic("avatar_10","Avatar 10",CosmeticCategory.AVATAR,CosmeticUnlockKind.GEMS,"A10",0xFFE14F7C.toInt(),0xFF79203D.toInt(),gems=500),
        )

    /**
     * Only Starter Paws is content-ready today. The remaining offers reserve
     * stable store IDs and demonstrate every supported progression channel,
     * but cannot consume gems/progress or become selected until their complete
     * art/reaction/animation/voice pack is registered in the character catalog.
     */
    private val builtInCharacterPacks =
        listOf(
            cosmetic(
                id = DEFAULT_CHARACTER_PACK,
                title = "STARTER PAWS",
                category = CosmeticCategory.CHARACTER_PACK,
                unlockKind = CosmeticUnlockKind.FREE,
                previewSymbol = "PAW",
                previewPrimary = 0xFF20A86B.toInt(),
                previewSecondary = 0xFF075B72.toInt(),
                characterPackId = "starter_paws",
            ),
            cosmetic(
                id = "character_pack_safari_paws",
                title = "SAFARI PAWS",
                category = CosmeticCategory.CHARACTER_PACK,
                unlockKind = CosmeticUnlockKind.LEVEL,
                previewSymbol = "SA",
                previewPrimary = 0xFFE0A12A.toInt(),
                previewSecondary = 0xFF8A4C18.toInt(),
                level = 8,
                characterPackId = "safari_paws",
                contentAvailable = false,
            ),
            cosmetic(
                id = "character_pack_farm_paws",
                title = "FARM PAWS",
                category = CosmeticCategory.CHARACTER_PACK,
                unlockKind = CosmeticUnlockKind.GEMS,
                previewSymbol = "FA",
                previewPrimary = 0xFF5EB34D.toInt(),
                previewSecondary = 0xFF2B6C36.toInt(),
                gems = 300,
                characterPackId = "farm_paws",
                contentAvailable = false,
            ),
            cosmetic(
                id = "character_pack_cozy_paws",
                title = "COZY PAWS",
                category = CosmeticCategory.CHARACTER_PACK,
                unlockKind = CosmeticUnlockKind.REWARDED_ADS,
                previewSymbol = "CO",
                previewPrimary = 0xFF7E74D9.toInt(),
                previewSecondary = 0xFF463A94.toInt(),
                ads = 10,
                characterPackId = "cozy_paws",
                contentAvailable = false,
            ),
            cosmetic(
                id = "character_pack_wild_paws",
                title = "WILD PAWS",
                category = CosmeticCategory.CHARACTER_PACK,
                unlockKind = CosmeticUnlockKind.EVENT,
                previewSymbol = "WI",
                previewPrimary = 0xFFD95A36.toInt(),
                previewSecondary = 0xFF7B2B37.toInt(),
                characterPackId = "wild_paws",
                eventKey = "wild_paws_event_v1",
                contentAvailable = false,
            ),
            cosmetic(
                id = "character_pack_pet_paws",
                title = "PET PAWS",
                category = CosmeticCategory.CHARACTER_PACK,
                unlockKind = CosmeticUnlockKind.GEMS,
                previewSymbol = "PE",
                previewPrimary = 0xFF3D9BE9.toInt(),
                previewSecondary = 0xFF3155A7.toInt(),
                gems = 450,
                characterPackId = "pet_paws",
                contentAvailable = false,
            ),
            cosmetic(
                id = "character_pack_jungle_paws",
                title = "JUNGLE PAWS",
                category = CosmeticCategory.CHARACTER_PACK,
                unlockKind = CosmeticUnlockKind.GEMS,
                previewSymbol = "JU",
                previewPrimary = 0xFF5A9E3B.toInt(),
                previewSecondary = 0xFF234D30.toInt(),
                gems = 550,
                characterPackId = "jungle_paws",
                contentAvailable = false,
            ),
        )

    @Volatile
    private var remoteItemsById: Map<String, StoreCosmetic> = emptyMap()

    @Volatile
    private var suppressedLegacyIds: Set<String> = emptySet()

    val boards: List<StoreCosmetic>
        get() = mergedForCategory(CosmeticCategory.BOARD)

    val dice: List<StoreCosmetic>
        get() = mergedForCategory(CosmeticCategory.DICE)

    val avatars: List<StoreCosmetic>
        get() = mergedForCategory(CosmeticCategory.AVATAR)

    val characterPacks: List<StoreCosmetic>
        get() = mergedForCategory(CosmeticCategory.CHARACTER_PACK)

    val all: List<StoreCosmetic>
        get() = boards + dice + avatars + characterPacks

    // Only shipped, locally renderable defaults are granted at account creation.
    // Remote FREE offers never become locally owned merely by appearing in a catalog.
    val defaultOwnedIds: Set<String>
        get() = (builtInBoards + builtInDice + builtInAvatars + builtInCharacterPacks)
            .filter { it.unlockKind == CosmeticUnlockKind.FREE && it.contentAvailable }
            .map { it.id }
            .toSet()

    fun replaceRemoteCatalog(
        items: List<StoreCosmetic>,
        suppressedIds: Set<String>,
    ) {
        val safeItems = items
            .filter { it.remoteManaged && ID_PATTERN.matches(it.id) }
            .distinctBy { it.id }
            .associateBy { it.id }
        remoteItemsById = safeItems
        // A published item always takes precedence over suppression metadata for the same ID.
        suppressedLegacyIds = suppressedIds
            .filter(ID_PATTERN::matches)
            .toSet() - safeItems.keys
    }

    fun clearRemoteCatalog() {
        remoteItemsById = emptyMap()
        suppressedLegacyIds = emptySet()
    }

    fun findBuiltIn(id: String): StoreCosmetic? =
        (builtInBoards + builtInDice + builtInAvatars + builtInCharacterPacks)
            .firstOrNull { it.id == id }

    fun find(
        id: String,
    ): StoreCosmetic? =
        all.firstOrNull {
            it.id == id
        }

    fun forCategory(
        category: CosmeticCategory,
    ): List<StoreCosmetic> =
        mergedForCategory(category)

    private fun mergedForCategory(
        category: CosmeticCategory,
    ): List<StoreCosmetic> {
        val builtIns = when (category) {
            CosmeticCategory.BOARD -> builtInBoards
            CosmeticCategory.DICE -> builtInDice
            CosmeticCategory.AVATAR -> builtInAvatars
            CosmeticCategory.CHARACTER_PACK -> builtInCharacterPacks
        }
        val hiddenIds = suppressedLegacyIds
        val remote = remoteItemsById
        val visibleBuiltIns = builtIns
            .filterNot { it.id in hiddenIds }
            .map { builtIn ->
                val serverItem = remote[builtIn.id]
                if (serverItem == null || serverItem.category != builtIn.category) {
                    builtIn
                } else {
                    serverItem.copy(
                        previewSymbol = builtIn.previewSymbol,
                        previewPrimary = builtIn.previewPrimary,
                        previewSecondary = builtIn.previewSecondary,
                        characterPackId = builtIn.characterPackId,
                        eventKey = builtIn.eventKey,
                        // Server availability is necessary, but the app must also have a
                        // matching local renderer/complete content pack before enabling taps.
                        contentAvailable = builtIn.contentAvailable && serverItem.contentAvailable,
                    )
                }
            }
        val builtInIds = builtIns.map { it.id }.toSet()
        val newRemoteItems = remote.values
            .filter { it.category == category && it.id !in builtInIds }
            .map { it.copy(contentAvailable = false) }
        return visibleBuiltIns + newRemoteItems
    }

    fun defaultId(
        category: CosmeticCategory,
    ): String =
        when (category) {
            CosmeticCategory.BOARD -> DEFAULT_BOARD
            CosmeticCategory.DICE -> DEFAULT_DICE
            CosmeticCategory.AVATAR -> DEFAULT_AVATAR
            CosmeticCategory.CHARACTER_PACK -> DEFAULT_CHARACTER_PACK
        }

    private val ID_PATTERN = Regex("^[a-z0-9][a-z0-9_-]{2,63}$")

    private fun cosmetic(
        id: String,
        title: String,
        category: CosmeticCategory,
        unlockKind: CosmeticUnlockKind,
        previewSymbol: String,
        previewPrimary: Int,
        previewSecondary: Int,
        gems: Int = 0,
        level: Int = 0,
        ads: Int = 0,
        characterPackId: String? = null,
        eventKey: String? = null,
        contentAvailable: Boolean = true,
    ) =
        StoreCosmetic(
            id = id,
            title = title,
            category = category,
            unlockKind = unlockKind,
            gemPrice = gems,
            requiredLevel = level,
            requiredAdViews = ads,
            previewSymbol = previewSymbol,
            previewPrimary = previewPrimary,
            previewSecondary = previewSecondary,
            characterPackId = characterPackId,
            eventKey = eventKey,
            contentAvailable = contentAvailable,
        )
}
