# Phase 8 — Pass & Play / Computer Gameplay Visual Refresh

## Intent

Address the in-match controls and UI surfaces that looked inconsistent in Pass & Play and Computer modes. The phase is presentation-only: make action meaning clearer, reduce glassy control chrome, and provide visible keyboard/accessibility focus while protecting board/gameplay behavior.

## Applied changes

- Offline gameplay top controls opt into a dedicated style; Online Lobby and Team Up continue using the existing shared glass style.
- Back uses the new, distinct `BACK_ARROW` glyph (separate from a plain left chevron) and Settings uses the shared `SETTINGS` icon. Both use a matte dark circular surface with a thin cyan idle border, gold pressed/focus feedback, and a visible shared focus ring.
- The offline turn/HUD panel uses an opaque-enough matte dark surface with a subtle mode-specific cyan/gold edge to improve text readability against the shared gameplay backdrop.
- Result actions are now distinct, accessible controls: REMATCH uses `REFRESH` with a green positive treatment; EXIT uses `CLOSE` with a restrained danger treatment. Both preserve their existing callbacks and click sound and have at least a 48dp layout height.
- The existing custom Quick Chat speech-bubble artwork is retained and receives a visible focus ring; the popup content and reaction behavior are unchanged.
- Adds `GameTopActionsUiInstrumentedTest` covering semantic icon selection, focusability, matte state-list backgrounds, foreground focus overlays, accessibility names and click callbacks.

## Background and artwork boundaries

- The full-screen `ludo_paws_game_background` artwork remains unchanged. A global wallpaper swap would affect setup, Store, Friends and other screens outside this phase and should only happen after a separately approved screenshot comparison.
- Board artwork, pet/thumbnail illustrations, player colors, dice rendering, pawn paths and character assets remain feature-owned and are not replaced by generic icons.
- The top control buttons, HUD surface and finished-match action surface are the specifically refreshed backgrounds in this phase.

## Safety boundaries

- No rules, legal moves, dice outcomes, turn order, game/session state, proof logic, pawn movement, sound timing, Quick Chat payloads or persistence contracts are changed.
- Online top-action visuals are unchanged; the dedicated matte style is opt-in from the offline gameplay screen only.
- Rematch and Exit actions keep their original behavior. The new icons are decorative and their parent views own accessible descriptions.

## Validation gates

- Android unit tests and release lint.
- Debug/release APK build and release AAB packaging.
- Emulator instrumentation including `GameTopActionsUiInstrumentedTest` and existing icon/focus-ring tests.
- CodeQL and Android Release Readiness.
- Manual testing in both Pass & Play and Computer modes: Back, Settings, rematch, exit confirmation, Quick Chat keyboard focus, readable HUD over the board/backdrop, compact devices and TalkBack.
- Capture before/after screenshots on the same device density/orientation and ensure the board and player rails do not move.

## Completion checklist

- [x] Semantic Back/Settings icons and matte button backgrounds on offline gameplay only.
- [x] Matte HUD treatment for Pass & Play and Computer.
- [x] Semantic REFRESH/CLOSE icons for rematch and exit.
- [x] Visible Quick Chat focus feedback.
- [x] Instrumentation test for game top actions.
- [x] Safety boundaries and device QA checklist documented.
- [ ] CI green on the exact Phase 8 PR commit.
- [ ] Manual screenshots and device/TalkBack validation complete.
