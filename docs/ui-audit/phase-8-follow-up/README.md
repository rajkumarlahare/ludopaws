# UI follow-up — shared controls and player-count selection

## User-visible corrections

- Profile Back no longer uses the yellow/orange gradient control. It now reuses AppIconActionButton, with the same circular glass surface, semantic chevron icon, sizing vocabulary and focus ring as the game controls.
- Pass & Play and Computer in-match Back/Settings actions reuse that same component. Their icon type, surface, icon bounds, elevation and accessibility focus behavior no longer branch by mode.
- Offline setup Back/Settings actions also reuse the shared component, including the narrow Computer setup header.
- The 2/3/4-player tiles no longer use Unicode chess-pawn characters. Each tile uses a prominent numeric player count and a clear PLAYERS label, so the control does not depend on platform-specific chess glyph rendering.
- The shared action component owns the visual surface and focus treatment; feature screens only provide the icon meaning, accessible name and existing click handler.

## Behavior boundaries

- Back still invokes the same exit/navigation callback.
- Settings still opens the same settings dialog and returns to setup/gameplay through the existing snapshot logic.
- Player-count tiles still call updateSelectedPlayerCount with the same 2, 3 or 4 values.
- No matchmaking, local game rules, dice, movement, turn order, proof/session state, sound timing, store/billing, persistence or network contracts are changed.

## Validation

- CI is required on this follow-up PR: Android unit tests, release lint/build, emulator instrumentation and Release Readiness.
- GameTopActionsUiInstrumentedTest checks shared Back/Settings icon semantics, common button surfaces, accessible labels and click callbacks.
- Manual QA is still required for screenshots on the same device/resolution in Profile, Pass & Play, Computer setup and in-match screens, with keyboard focus/TalkBack and compact-width review.

## Known limitation

The setup player-count tiles intentionally prioritize a large numeric count over an extra icon. This removes the inconsistent chess-pawn font glyphs instead of introducing another screen-specific illustration.
