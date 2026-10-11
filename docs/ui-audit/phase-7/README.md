# Phase 7 — Online Lobby and Team Up UI Consistency

## Intent

Apply the shared semantic icon and focus/accessibility foundation to the Online Lobby and Team Up entry screens without changing matchmaking behavior, player-count defaults, networking, session contracts or game rules.

## Applied changes

- Team Up header now pairs the existing title with the shared semantic `PEOPLE` outline icon. The icon is decorative to accessibility; the title remains readable.
- Team Up Find and Cancel actions have explicit accessible names and retain the existing shared focus-ring treatment.
- Online Lobby's Cancel action now has an explicit accessibility label and the shared visible focus ring.
- Two-player and four-player selection controls expose `isSelected` and announce the selected player-count state while preserving their custom PlayerGroupDrawable artwork and selected colors.
- Existing custom match medallion, player-group illustration, backgrounds and team-color artwork remain feature-owned.

## Safety boundaries

- No public matchmaking request, polling/cancellation code, player-count selection logic, team formation, session or server protocol is changed.
- No game rules, dice/proof logic, persisted settings, billing, purchases or ads behavior is changed.
- Pass & Play and Computer gameplay interiors are deliberately not included in Phase 7; they have a dedicated follow-up scope in Phase 8.

## Phase 8 — requested gameplay icon/background redesign

The next phase is explicitly intended to address the requested screens rather than expanding this pass sideways:

- Pass & Play in-match screen: replace the inconsistent Back and Settings button icon treatment with a coherent custom semantic icon style.
- Computer in-match screen: apply the same focused, distinct Back/Settings treatment and audit the other small button/thumbnail icons.
- Review the in-game surfaces/backgrounds for Pass & Play and Computer, preserving board readability and the current game artwork while correcting inconsistent or poor-looking UI chrome.
- Inventory icon sources first, then distinguish semantic actions (Back, Settings, Close, Refresh, Quick Chat and related controls) from character/board artwork; avoid indiscriminate one-glyph-fits-all replacement.
- Add deterministic before/after screenshot coverage and regression checks for callbacks, hit targets, visual state and compact display layouts.

No gameplay, dice, movement, turn order, proof or sound behavior should be altered as part of that visual redesign.

## Validation gates

- Android unit tests, release lint, debug/release APK assembly and release AAB packaging.
- Emulator instrumentation and existing shared icon/focus-ring tests.
- CodeQL and Android Release Readiness.
- Manual QA for Online 2/4-player selection, Play, Cancel, Team Up Find/Cancel, keyboard focus, TalkBack labels, compact layouts and navigation.
- The Pass & Play / Computer redesign requires its own before/after device screenshots and smoke tests in Phase 8.

## Completion checklist

- [x] Shared PEOPLE glyph added to the Team Up header.
- [x] Team Up Find/Cancel actions named accessibly.
- [x] Online matchmaking Cancel uses shared focus feedback.
- [x] Online player-count selected state exposed accessibly.
- [x] Phase 8 scope for Pass & Play / Computer documented.
- [ ] Required CI checks green on the exact Phase 7 PR commit.
- [ ] Device-level visual and TalkBack review complete.
