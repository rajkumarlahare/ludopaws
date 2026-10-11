# Phase 1 — UI Code Audit

## Snapshot and method

- Repository: [ludoproof](https://github.com/rajkumarlahare/ludoproof)
- Snapshot: [commit f11f9578e7f45347c34bc8f76eabe63ce829d9ee](https://github.com/rajkumarlahare/ludoproof/commit/f11f9578e7f45347c34bc8f76eabe63ce829d9ee)
- Inspection: repository tree, primary screen/component files, icon drawing paths, selected click handlers, theme/layout helpers, and latest GitHub Actions runs.
- This is static source review. The local Gradle commands were not run in this pass.

Ludo Paws is a native Android Kotlin Views/Canvas/Drawable app. This is not a Jetpack Compose migration. There is already a global theme, reusable Home glass chrome and separate mode-card chrome; the main maintainability opportunity is to define stable shared tokens and common action-icon semantics without flattening brand illustration or special-screen styling.

## Repository snapshot

At the audited commit, the tracked Git tree contained 540 blobs, including 289 Kotlin files, 70 JVM/unit-test Kotlin files and 5 Android instrumentation-test Kotlin files. The Android application identity is still com.ludoproof.game; Ludo Paws is the consumer-facing brand. Visual work must not casually rename package, persisted storage, protocol, ruleset or proof identifiers.

## Findings and priority

| ID | Priority | Finding | Evidence | Recommendation |
|---|---|---|---|---|
| UI-01 | P1 — interaction | Home Badge and Remove Ads shortcuts are clickable/focusable but their click handler reaches else -> Unit. | [HomeHeaderUi.kt, shortcut list and handler](https://github.com/rajkumarlahare/ludoproof/blob/main/android/app/src/main/java/com/ludoproof/game/feature/home/presentation/components/HomeHeaderUi.kt#L285-L359) | Confirm the intended destinations. Wire them only after confirming behavior, or remove/disable the action affordance while it is unavailable. |
| UI-02 | P2 — icon consistency | HomeIconView, FriendsIconView, the Settings close button and game back chevron draw icons independently. Their stroke rules and coordinate systems differ. | [HomeIconView.kt](https://github.com/rajkumarlahare/ludoproof/blob/main/android/app/src/main/java/com/ludoproof/game/feature/home/presentation/components/HomeIconView.kt#L18-L95), [FriendsVisualKit.kt](https://github.com/rajkumarlahare/ludoproof/blob/main/android/app/src/main/java/com/ludoproof/game/feature/friends/presentation/FriendsVisualKit.kt#L18-L45), [SettingsVisualKit.kt](https://github.com/rajkumarlahare/ludoproof/blob/main/android/app/src/main/java/com/ludoproof/game/feature/settings/presentation/SettingsVisualKit.kt#L102-L145), [GameTopActionsUi.kt](https://github.com/rajkumarlahare/ludoproof/blob/main/android/app/src/main/java/com/ludoproof/game/game/ui/components/GameTopActionsUi.kt#L113-L134) | Design a shared semantic icon API in Phase 2. Migrate common actions one at a time, preserving size, tint, content description, click parent and visual exceptions. |
| UI-03 | P2 — design-system duplication | LudoProofTheme contains global palette and layout helpers, while feature kits implement local surfaces, button treatments and palettes. | [LudoProofTheme.kt](https://github.com/rajkumarlahare/ludoproof/blob/main/android/app/src/main/java/com/ludoproof/game/core/ui/theme/LudoProofTheme.kt), [HomeGlassChrome.kt](https://github.com/rajkumarlahare/ludoproof/blob/main/android/app/src/main/java/com/ludoproof/game/feature/home/presentation/components/HomeGlassChrome.kt), FriendsVisualKit.kt, SettingsVisualKit.kt and StoreTabsUi.kt | Extract semantic tokens incrementally. Keep deliberately unique treatments (Home glass/mode cards, Store selected tab, settings texture, character/board illustrations) feature-specific until design intent is clear. |
| UI-04 | P2 — accessibility | Settings ON/OFF segments update state and change visual styling; the reviewed control does not explicitly expose selected/checked state. | [SettingsVisualKit.kt, SettingsBooleanControl](https://github.com/rajkumarlahare/ludoproof/blob/main/android/app/src/main/java/com/ludoproof/game/feature/settings/presentation/SettingsVisualKit.kt#L290-L411) | Review TalkBack label/role/state announcements, focus order and non-touch access. Treat this as an accessibility risk to validate, not a confirmed runtime failure. |
| UI-05 | P2 — responsive QA | Shared width/padding helpers exist, but some screens use their own compact/default padding rules. Team Up currently uses fixed horizontal padding in buildUi. | [Theme layout helpers](https://github.com/rajkumarlahare/ludoproof/blob/main/android/app/src/main/java/com/ludoproof/game/core/ui/theme/LudoProofTheme.kt#L552-L569), [TeamUpActivity.kt](https://github.com/rajkumarlahare/ludoproof/blob/main/android/app/src/main/java/com/ludoproof/game/feature/team/presentation/TeamUpActivity.kt#L107-L117) | Define explicit responsive expectations and verify compact, regular, expanded, landscape/short-height and larger-font layouts before changing spacing. |
| UI-06 | P2 — visual regression | CI covers compile/lint/unit/instrumentation and domain contracts, but this audit found no dedicated cross-screen screenshot/golden baseline for icon stroke and layout. | [Latest CI run](https://github.com/rajkumarlahare/ludoproof/actions/runs/38107467907) and [QA baseline](QA_BASELINE.md) | Capture deterministic before/after screenshots in the visual phase; separately test actions and accessibility. |
| UI-07 | P3 — change risk | Several UI owners are large files: FriendsActivity.kt (1,643 lines), LeaderboardActivity.kt (1,405), OnlineLobbyUi.kt (1,126), TeamUpActivity.kt (1,015). | Source line counts at the snapshot | Avoid broad rewrites. Migrate one feature and a small set of reusable components per change. |

## Architectural guardrails

- Preserve the current visual brand unless a redesign is explicitly approved.
- Preserve app/package and backend compatibility identifiers.
- Do not change dice outcomes, move legality, proof math, online/offline authority, session/recovery, billing, rewarded-ad entitlements or purchase verification as part of a UI refactor.
- Do not convert gems, shop/badge art, pawns, characters, matchmaking art or the board renderer into generic outline icons.
- All shared icons must keep accessibility labels on the actionable parent rather than create redundant TalkBack stops on decorative child views.

## Phase 1 completion

- [x] Record repository snapshot and relevant UI source structure.
- [x] Map major screen/component owners.
- [x] Inventory separate common-icon drawing implementations.
- [x] Inspect selected high-value interactions and record confirmed source-level no-ops.
- [x] Record the latest remote CI baseline.
- [ ] Capture device screenshots — not done in this static pass.
- [ ] Run Gradle tests locally — not done in this environment.
- [ ] Runtime TalkBack, compact-device and landscape checks — remain test tasks.

## Exit gate for Phase 2

Agree the shared token/icon contract and confirm what Badge and Remove Ads are meant to do before changing those behaviors. Then implement only the shared primitives and a Home pilot; validate compilation, tests, interaction destinations, screenshots and accessibility before migrating other screens.
