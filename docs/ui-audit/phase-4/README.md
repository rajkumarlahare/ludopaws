# Phase 4 — Home Screen Pilot

## Intent

Apply a small, visible and reviewable UI improvement to Home while preserving game actions, the supplied Ludo Paws hero artwork, fixed viewport positions and feature-owned glass/mode-card drawing.

## Changes

- Mode cards now show their short supporting labels (Find match, 2 vs 2, Private, CPU, Offline) below the mode title. Text sizes adapt for compact widths and supporting labels ellipsize rather than grow outside card bounds.
- Home glass controls and mode cards draw a gold focus rim when keyboard/accessibility focus is present; pressed-state shading remains intact.
- Badge and Remove Ads shortcuts are visibly dimmed and marked unavailable because their intended destinations are not yet defined. Their prior no-op click affordance is removed. Shop and Leaderboard remain active.
- The existing Home bottom action controls (Rate, Share, Settings) benefit from the focused glass treatment without replacing the established Ludo Paws visual style.
- Instrumentation tests compare normal/focused render output for both Home glass and mode-card chrome.

## Preserved

- Home hero art, per-mode color treatments, card geometry, toolbar/navigation destinations and current fixed positioning.
- Shop and Leaderboard navigation; Share, Settings and rating actions.
- Gameplay rules, dice/proof logic, remote authority, online/session protocol, persisted stores, billing, purchase verification, ad consent and entitlement behavior.
- No broad migration to generic buttons/panels is performed where that would erase intentional Home-specific styling.

## Validation gate

Run the full Android CI gate on the stacked Phase 4 branch:
- `:app:testDebugUnitTest`
- `:app:lintRelease`
- `:app:assembleDebug`, release APK and release AAB
- `:app:connectedDebugAndroidTest`

The `HomeChromeFocusInstrumentedTest` checks that focus is a genuinely visible drawable state. Device screenshot comparison and TalkBack validation remain manual review tasks; no screenshot or device test is claimed as locally completed.

This PR is stacked on `feat/phase-3-shared-ui-components-v1`; keep the ordered dependency chain Phase 2 → Phase 3 → Phase 4, or rebase the stack before changing PR bases.
