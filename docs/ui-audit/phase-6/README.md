# Phase 6 — Profile, Store and Leaderboard UI Consistency

## Intent

Extend the shared UI focus and selected-state treatment into Profile, Store and Leaderboard. Preserve each screen's existing visual identity and leave navigation, domain state and service behavior owned by the screen.

## Applied changes

- Profile back, Play Games action and profile-name edit action display the shared gold focus ring without replacing their existing backgrounds.
- Store back action, tab controls, purchase buttons, rewarded-ad action and ad-privacy action display the shared focus ring.
- Store tabs expose their selected state through `isSelected` and an explicit selected accessibility description.
- Leaderboard back and refresh actions display the shared focus ring. Existing refresh enable/disable state and loading feedback are unchanged.
- Existing feature-specific gradients, tab colors, cards, avatars and leaderboard ranking treatments are retained.

## Safety boundaries

- No ProfileStore schema, Play Games authentication flow, purchase request, billing verification, ad consent, rewarded-ad fulfillment, inventory/wallet persistence or purchase entitlement behavior is changed.
- No leaderboard API, cached snapshot, credential, score, ranking or refresh behavior is changed.
- No navigation destinations, networking/session code, game rules, dice/proof logic or gameplay rendering are changed.

## Validation

Required CI gates for this PR:
- Android unit tests and release lint.
- Debug/release APK assembly and release AAB packaging.
- Emulator instrumentation and existing shared focus-ring/icon tests.
- CodeQL and release readiness.

Manual device QA remains required:
- Profile: Back, Play Games states, edit-name action and keyboard/TalkBack focus.
- Store: each tab selected state, purchase buttons with long prices, rewarded-ad/privacy focus, and unchanged touch/click behavior.
- Leaderboard: Back, Refresh disabled/loading state, long player names and narrow-screen layout.
- Compare compact and standard phone screenshots; test font scaling and keyboard navigation.

Automated CI does not replace device-level screenshot or TalkBack review. Do not merge until the PR's required checks are green and manual QA is recorded.

## Completion checklist

- [x] Shared focus feedback applied to Profile actions.
- [x] Store tabs expose selection semantics and shared focus feedback.
- [x] Shared focus feedback applied to Store purchase, rewarded-ad and privacy actions.
- [x] Shared focus feedback applied to Leaderboard top actions.
- [x] Safety boundaries and manual QA checklist documented.
- [ ] Required CI checks green on the Phase 6 PR commit.
- [ ] Compact/standard device screenshot and TalkBack review complete.
