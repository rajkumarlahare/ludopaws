# LudoProof Android 1.0.0-rc2

## Purpose

This release candidate prepares the Android client for the Ruleset V2 rollout without activating Ruleset V2 on production matches.

- `versionCode`: `2`
- `versionName`: `1.0.0-rc2`
- production backend default: Ruleset V1
- Android authoritative snapshot parser: V1 + V2 compatible
- billing default: disabled unless explicitly enabled by build property and the Worker reports purchase verification ready

## Compatibility contract

RC2 must continue to accept all four authoritative ruleset IDs:

- `ludoproof-standard-v1`
- `ludoproof-standard-v2`
- `ludoproof-team-v1`
- `ludoproof-team-v2`

The backend must remain pinned to V1 for newly-created production matches until a V2-capable Android build is distributed to users. Existing V1 proof hashes remain immutable.

## Release signing

Do not commit a keystore or passwords to the repository. Release signing is read only from environment variables:

- `LUDOPROOF_RELEASE_STORE_FILE`
- `LUDOPROOF_RELEASE_STORE_PASSWORD`
- `LUDOPROOF_RELEASE_KEY_ALIAS`
- `LUDOPROOF_RELEASE_KEY_PASSWORD`

The manual `LudoProof Android Signed Release` GitHub Actions workflow expects these repository secrets:

- `LUDOPROOF_ANDROID_KEYSTORE_BASE64`
- `LUDOPROOF_ANDROID_STORE_PASSWORD`
- `LUDOPROOF_ANDROID_KEY_ALIAS`
- `LUDOPROOF_ANDROID_KEY_PASSWORD`

The workflow decodes the keystore only into the ephemeral runner directory, builds the release APK/AAB, verifies their signatures, hashes release evidence, and uploads the signed artifacts. Missing signing secrets cause the workflow to fail instead of falling back to a debug key.

## Google Play Billing

Store checkout must remain disabled until the one-time Play Console products are active and the Worker has its Google Play Developer API service-account email/private-key configuration. The private key is a Cloudflare Worker secret and must never be compiled into the APK/AAB. The app queries `/api/store/status` before connecting Billing, verifies purchase tokens through `/api/store/purchases/verify`, and grants gems or remove-ads only after the backend confirms them. Follow the [Google Play Billing setup instructions](../README.md#google-play-billing) before enabling `-PLUDOPROOF_BILLING_ENABLED=true`.

## Google AdMob rewarded ads

Rewarded ads use the Google Mobile Ads SDK and UMP consent flow. Production ads remain disabled unless explicitly enabled with real AdMob IDs; Google's sample IDs are allowed only for debug testing. Reward grants are server-verified by the Worker through AdMob SSV; the client reward callback only triggers a wallet refresh and never grants value locally.

The currently configured AdMob reward is **5 gems**. A normal Gems-tab ad grants 5 gems; an ad watched toward an eligible cosmetic grants the same 5 gems **plus one server-tracked cosmetic-progress step**. Both effects are idempotent on the signed AdMob transaction ID. Complete the AdMob app/ad-unit and privacy-message setup in the [Google AdMob rewarded ads instructions](../README.md#google-admob-rewarded-ads) before enabling production ads.

## Required gates before distribution

1. Server tests and repository security gate pass.
2. Android RC2 release-readiness gate passes.
3. Android unit tests pass.
4. Release lint passes.
5. Debug/release APK and release AAB build successfully.
6. Android 16 target verification passes.
7. Emulator instrumentation tests pass.
8. CodeQL JavaScript and Android Kotlin analyses pass.
9. Signed workflow verifies APK and AAB signatures.
10. AdMob UMP consent/privacy options and the signed SSV callback are tested end-to-end for both Gems-tab rewards and cosmetic-progress rewards; each successful callback grants the configured 5 gems, cosmetic progress increments only for an eligible target, and duplicate callbacks do not duplicate either reward.

## Ruleset V2 rollout sequence

1. Merge RC2 client compatibility and release-signing preparation.
2. Produce and distribute a signed V2-capable Android build.
3. Confirm the deployed client population is compatible.
4. Use a separate, minimal backend rollout PR to make new matches opt into `CLASSIC_V2` / `TEAM_UP_V2`.
5. Deploy the backend through the production workflow and run live `/health`, `/ready`, and real-game smoke checks.
6. Keep V1 verification support permanently for historical match receipts.
