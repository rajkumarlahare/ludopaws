# QA Baseline — Phase 1

## Snapshot

- Repository: [rajkumarlahare/ludoproof](https://github.com/rajkumarlahare/ludoproof)
- Branch/commit: main / [f11f9578e7f45347c34bc8f76eabe63ce829d9ee](https://github.com/rajkumarlahare/ludoproof/commit/f11f9578e7f45347c34bc8f76eabe63ce829d9ee)
- Date in the repository commit metadata: 11 October 2026.

## Latest GitHub Actions baseline before this audit branch

| Workflow | Result | Evidence |
|---|---|---|
| Ludo Paws CI | Success | [Run 38107467907](https://github.com/rajkumarlahare/ludoproof/actions/runs/38107467907) |
| Android Release Readiness | Success | [Run 38107467862](https://github.com/rajkumarlahare/ludoproof/actions/runs/38107467862) |
| CodeQL | Success | [Run 38107467920](https://github.com/rajkumarlahare/ludoproof/actions/runs/38107467920) |
| Offline v4 Conformance | Success | [Run 38107467958](https://github.com/rajkumarlahare/ludoproof/actions/runs/38107467958) |
| Cloudflare Production | Skipped | [Run 38107467830](https://github.com/rajkumarlahare/ludoproof/actions/runs/38107467830); deployment was skipped, not reported as a failed UI test. |

The Ludo Paws CI run included Android unit tests, release lint, debug/release APK assembly, release AAB bundling, emulator instrumentation, server tests, script checks and SBOM generation. These are remote Actions results, not evidence of a local Gradle run in this session.

## Test inventory at the snapshot

- 70 JVM/unit-test Kotlin files under android/app/src/test/.
- 5 Android instrumentation-test Kotlin files under android/app/src/androidTest/.
- Existing coverage includes game rules/proof contracts, online/offline recovery, rendering policies and several user flows.
- No screenshot/golden suite dedicated to a full-screen, cross-device visual comparison was identified in this static inventory. This does not prove that no manual screenshot evidence exists elsewhere.

## Screenshot status

No new device screenshots were captured as part of this audit. The emulator instrumentation run is not a screenshot baseline. Capture and compare deterministic screenshots before and after actual visual changes with the same API level, density, font scale, orientation, insets/system-bar state, account/game state and animation settings.

Suggested coverage:
- Compact portrait (around 320dp width): Home, Settings, Friends, Store, Leaderboard; truncation/collisions.
- Standard phone portrait (around 360–393dp width): all main screen families and selected/pressed/disabled states.
- Short-height or landscape (around 800 × 360dp): Home, Settings dialog, Store, lobby and in-game top actions.
- Expanded width (600–720dp): centered content/max-width and large-screen spacing.
- Font scale 1.3x and 2.0x: clipping, wrapping, target size, labels and focus order.

## Local verification commands

Run from a local checkout (not executed in this audit pass):

```bash
gradle -p android :app:testDebugUnitTest
gradle -p android :app:lintRelease
gradle -p android :app:assembleDebug
gradle -p android :app:assembleRelease
gradle -p android :app:bundleRelease
gradle -p android :app:connectedDebugAndroidTest
```

## Phase 1 exit state

- Completed: static inventory, screen/component matrix, icon-source inventory, selected source-handler audit and remote CI baseline.
- Pending: runtime screenshots, local Gradle runs, TalkBack checks and compact/landscape manual UI review.
- Phase 2 must not proceed to broad migration until the shared icon/token contract is clear and the intended Home Badge/Remove Ads destinations are confirmed.
