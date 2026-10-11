# Phase 5 — Friends, Settings and Multiplayer UI Consistency

## Intent

Carry the shared UI foundation into Friends, Settings and multiplayer entry points without changing networking, matchmaking contracts, navigation destinations, stored settings, friend operations or game/session behavior.

## Applied UI changes

- Friends uses the shared `AppIconView` registry for common action glyphs: back, people, add/invite, search, copy, share, play, message, clock and more. Feature-specific identity-card, house and friend-request envelope artwork remains owned by Friends.
- Friends search/add inputs and enabled action buttons receive the shared gold focus outline; Friend Requests and Room Invites cards receive the same focus treatment.
- Settings' close control uses the shared `CLOSE` icon and has a 48dp minimum touch target. Music/Sound controls, dropdown fields, dropdown choice rows, ON/OFF segments, and the Privacy Policy action show a gold focus outline without changing selected values or callback behavior.
- Public multiplayer player-count choices and PLAY, plus Team Up's FIND A TEAM and CANCEL SEARCH actions, show a visible gold outline on keyboard/accessibility focus. Existing selected/pressed surfaces and matchmaking actions remain intact.
- The focus helper overlays the current foreground rather than replacing the background, so feature-owned surfaces remain unchanged and any existing foreground feedback is retained.

## Safety boundaries

- No changes to friend API payloads, Friend ID generation, requests, invites, room/session creation, matchmaking polling/cancellation, server protocol, game rules, dice/proof logic, or persisted settings.
- No new navigation destinations, network requests, billing behavior, ad behavior or settings semantics.
- Feature-owned artwork and existing backgrounds are preserved where they do not represent common action glyphs.
- The scope is intentionally limited to Friends, Settings and the public/Team Up multiplayer entry surfaces. Other screens remain in their owning future phases.

## Validation

- `AppFocusRingInstrumentedTest` verifies focused-state pixels and confirms attachment preserves the existing background/foreground.
- `FriendsSharedIconInstrumentedTest` verifies common Friends actions use the shared icon registry and special artwork stays feature-specific.
- Run the full CI gate: unit tests, release lint, debug/release APKs, AAB packaging and emulator instrumentation.
- Manual review still required on compact and regular-width devices: Friends ID copy/share/search, request/invite cards, ON/OFF and dropdown controls in Settings, Online 2/4-player selection, PLAY, Team Up search/cancel, keyboard focus and TalkBack labels. Device review has not been performed by this commit.

## Phase 5 completion checklist

- [x] Friends common action glyph adoption.
- [x] Friends input/action/card focus visibility.
- [x] Settings close icon consolidation and touch target.
- [x] Settings audio controls, dropdown choices and Privacy Policy focus visibility.
- [x] Settings dropdown and toggle-segment focus visibility.
- [x] Public multiplayer mode selection and PLAY focus visibility.
- [x] Team Up FIND A TEAM and CANCEL SEARCH focus visibility.
- [x] Instrumentation coverage and documented manual QA gate.
- [ ] CI green on this exact commit.
- [ ] User manual device test and approval before merge.
