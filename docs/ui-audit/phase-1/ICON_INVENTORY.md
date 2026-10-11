# Icon Inventory — Phase 1

This is a source inventory, not a pixel-perfect visual comparison. It separates common action glyphs from branded art and renderers.

## Existing icon sources

| Source | Current icon families | Rendering behavior | Candidate next step |
|---|---|---|---|
| [HomeIconView.kt](https://github.com/rajkumarlahare/ludoproof/blob/main/android/app/src/main/java/com/ludoproof/game/feature/home/presentation/components/HomeIconView.kt#L18-L95) | GEM, PROFILE, EDIT, ADD, LEADERBOARD, SHOP, BADGE, SHARE, RATING, AD_BLOCKER, SETTINGS | Canvas with 100-unit logical coordinates; base stroke 5.5f, detail stroke 3.5f, plus icon-specific widths; mix of filled and outlined paths and brand colors. | Classify each drawing as action glyph, stateful/filled icon or branded illustration before migration. |
| [FriendsVisualKit.kt](https://github.com/rajkumarlahare/ludoproof/blob/main/android/app/src/main/java/com/ludoproof/game/feature/friends/presentation/FriendsVisualKit.kt#L18-L45) and [FriendsIconView](https://github.com/rajkumarlahare/ludoproof/blob/main/android/app/src/main/java/com/ludoproof/game/feature/friends/presentation/FriendsVisualKit.kt#L318-L360) | BACK, PEOPLE, ID_CARD, ADD_FRIEND, HOUSE, REQUESTS, INVITES, SEARCH, COPY, SHARE, PLAY, INVITE, MESSAGE, CLOCK, MORE | Separate Canvas view; base width 3dp; helpers change Paint between STROKE and FILL. INVITES currently shares PEOPLE art and INVITE shares ADD_FRIEND art. | Review whether aliases are intentional; good candidates include BACK, SEARCH, COPY, SHARE, PLAY, MESSAGE, CLOCK and MORE. |
| [SettingsVisualKit.kt](https://github.com/rajkumarlahare/ludoproof/blob/main/android/app/src/main/java/com/ludoproof/game/feature/settings/presentation/SettingsVisualKit.kt#L102-L145) | CLOSE | A separate Canvas cross with 2.5dp stroke inside an orange custom close-button background. | Share the glyph only; retain its specific button chrome and accessible parent. |
| [GameTopActionsUi.kt](https://github.com/rajkumarlahare/ludoproof/blob/main/android/app/src/main/java/com/ludoproof/game/game/ui/components/GameTopActionsUi.kt#L113-L134) | BACK chevron | Standalone Canvas path, 4dp stroke; button already uses shared Home glass treatment and accessible action parent. | Replace the glyph as a small controlled pilot, not the button container. |
| [LeaderboardActivity.kt](https://github.com/rajkumarlahare/ludoproof/blob/main/android/app/src/main/java/com/ludoproof/game/feature/leaderboard/presentation/LeaderboardActivity.kt#L419-L535) | BACK arrow and REFRESH text glyphs | Platform text glyphs with content descriptions. | Normalize glyph appearance while preserving descriptions, click handlers and disabled/loading states. |

## Illustrations to keep outside the generic icon registry

- ModeArtView.kt and HomePetsHeroView.kt: home/mode and character illustration.
- HomeModeButtonChrome.kt: mode-card rim/glow; it is component chrome, not a glyph.
- OnlineLobbyUi.kt: player groups, lobby and matchmaking visuals.
- TeamUpActivity.kt: Team Up Canvas art.
- LudoBoardView.kt, LudoPaws3DSceneView.kt and LudoPaws3DSceneRenderer.kt: board, pawns and 3D rendering.
- android/app/src/main/res/drawable/ic_ludo_paws.xml and ic_ludoproof.xml: application resources, not per-screen action glyphs.

## Proposed shared API (planning only; no code added)

- Semantic name: AppIconName (examples: BACK, CHEVRON_LEFT, CLOSE, ADD, SEARCH, COPY, SHARE, PLAY, REFRESH, SETTINGS, PROFILE, LEADERBOARD).
- Variant: OUTLINE, FILLED, with BRAND only where a tiny glyph truly is brand-specific; large mascot/feature art remains a separate illustration type.
- Size: define a few dp slots after screenshot review; do not pick a universal width based solely on existing logical canvas coordinates.
- Stroke: normalize common outline icons using dp-based geometry and rounded caps/joins where appropriate. Filled/brand exceptions should be explicit.
- State: tint, selected/enabled state and touch target belong to component/view state, not an icon enum side effect.
- Accessibility: icon-only controls expose one stable label on the clickable parent; decorative glyph views must not become redundant focus stops.

## Migration order

1. Back / chevron / close.
2. Search / add / copy / share / play / message.
3. Refresh / settings / profile / leaderboard.
4. Evaluate GEM, SHOP, BADGE, AD_BLOCKER, rating star and mascot visuals individually; do not automatically redraw them as generic outlines.
