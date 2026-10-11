# Phase 3 — Shared UI Components

## Intent

Introduce small native-View factories for repeated UI patterns. The app remains Kotlin Views/Canvas/Drawable-based; this phase does not add Jetpack Compose or replace the existing theme.

## Added components

- `AppButton`: semantic primary, secondary, positive and danger buttons backed by the current `LudoProofTheme` styles.
- `AppPanel`: standard and dark panel surfaces with configurable non-negative padding and optional elevation.
- `AppBadgeView`: informational, success, warning, danger and neutral status tones. It is not intended to replace branded gems, character badges or other feature artwork.
- `AppSectionHeader`: a title with optional supporting text.
- `AppTextInput`: shared themed text input; validation and text listeners remain with the feature.
- `AppActionIconButton`: a minimum-touch-target icon button. The actionable parent owns its non-empty accessibility description; the shared icon stays decorative.

## Compatibility / safety boundaries

- Components are additive; no existing feature screen is migrated in this phase.
- Existing `LudoProofTheme` colors and themed button/panel/input treatments remain the source of truth.
- Factories do not own navigation, business state, persistence, asynchronous work or domain callbacks. Those remain at the call site.
- Callers continue to own positioning and layout params.
- Feature-specific glass chrome, selected mode cards, mascot/character art, board rendering and intentional screen-specific surfaces remain feature-owned.
- No gameplay rules, dice/proof calculations, networking/session logic, purchase flow, billing or advertising behavior is changed.

## Validation

- `AppUiComponentsInstrumentedTest` covers enabled/click behavior, minimum touch-target dimensions, decorative icon accessibility, panel orientation/padding and visible input/header/badge text.
- Existing `AppIconViewRenderingInstrumentedTest` remains part of the Phase 2 base.
- Run the Android unit suite, release lint, debug APK/AAB build gates, and emulator instrumentation in CI. Device-level screenshot, TalkBack and compact/expanded layout review remain required before a broader screen migration.

## Suggested usage

Use these components for repeated generic structures. Do not migrate a custom component only because it looks superficially similar: compare its interaction states, semantics, and visual intent first. Phase 4 is the controlled Home-screen pilot and should migrate a small set at a time with before/after screenshots.
