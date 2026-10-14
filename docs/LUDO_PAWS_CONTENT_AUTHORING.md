# Ludo Paws content authoring and store release

## Current policy

- Dog, Cat, Goat and Duck in Starter Paws remain free, owned by default and playable.
- Future characters/packs and board themes remain unavailable by default. A new offer must not be charged for until its content is registered and its availability gate is intentionally enabled.
- Dice styles use the shared procedural dice renderer with a named palette registry.
- Cosmetic purchases always go through the authenticated Cloudflare Worker. Android preferences are display caches, not ownership or currency authority.

## Adding a character

1. Add stable metadata and asset references to `android/app/src/main/assets/ludo_paws/catalog.json` (lowercase ID, species, personality, preview/pawn/full-body art, animation set and voice set).
2. Register the runtime `AnimalCharacter` and `AnimalPack` in `LudoPawsCharacterCatalog.kt`. Use a one-character pack for an individually sold character, or a multi-character pack for a bundle. Preserve `starter_paws` and existing IDs for compatibility.
3. Register the species/model in `LudoPaws3DCharacterPolicy.kt` and the game renderer if it needs a new 3D shape. The current runtime still has a four-species geometry mapping; catalog JSON alone cannot introduce a completely new 3D species yet.
4. Add a locked offer in `StoreCosmeticCatalog.kt` and a matching server record in `server/src/player-account.js`. Keep Android `contentAvailable: false` and server `available: false` until art, preview, animation, reactions and audio work.
5. Choose the release unlock method: Gems (`GEMS`) or server-verified rewarded ads (`REWARDED_ADS`). Do not add client-only level restrictions; authoritative progression is not yet implemented on the Worker.
6. Debug builds can preview registered future-pack characters in locked Paws cards. Previewing must never equip the character, grant ownership or spend Gems. Keep the offer unavailable until release approval.
7. Only after the content-ready review, change both catalog availability gates in the same PR and run the focused catalog/purchase checks plus an on-device gameplay check.

## Adding a board theme

The production board renderer maps stable IDs such as `board_diwali`, `board_denim`, `board_neon`, `board_pirate`, `board_alien`, and `board_penguin` to theme colors. Add the offer and preview first, leave it unavailable while unfinished, and enable it only after the gameplay renderer and purchase/equip path have been checked.

## Adding a dice style

Add the offer to `StoreCosmeticCatalog.kt`, the server record to `server/src/player-account.js`, and a named palette to `DiceStylePalettePolicy.kt`. The Store preview uses the same palette resolver as `DiceView`; the player/team outline is presentation-only and cannot affect roll results or proof state.

## Payment activation checklist

Gems are consumable Google Play one-time products. Keep the Android catalog and `server/src/google-play-billing.js` product IDs/grant amounts identical. Leave the Worker billing feature flag off until Play Console products, service-account API access, the Cloudflare private-key secret and the purchase-ledger binding are configured. Run licensed internal-track checkout, pending/cancelled purchase, retry/restore and duplicate-grant checks before enabling production billing.
