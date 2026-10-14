# Google Play One-Time Billing Setup — Ludo Paws

This setup keeps monetization to Google Play one-time products only. Paws Plus Monthly and recurring subscription flows are intentionally out of the app/backend design. Do not enable a billing flag merely because code is present.

## Product contract

- Android application ID: `com.rekixo.ludopaws`
- Permanent ad-removal product ID: `remove_ads` (keep this existing ID; do not delete/recreate it as a different SKU)
- `remove_ads` is a non-consumable one-time purchase. A server-verified purchase sets the account's permanent `ads_removed` entitlement and grants the configured one-time 5-gem bonus.
- Gem packs remain consumable one-time products: `gems_300`, `gems_1500`, `gems_3500`, `gems_20000`, `gems_40000`, and `gems_200000`.
- Do not create or re-enable a monthly subscription for this flow.
- Remove any legacy `Paws Plus Monthly` UI, app/backend subscription code, product query, purchase/restore routes, subscription entitlement fields, and associated subscription-only tests if present. Do not retain a purchasable monthly base plan in Play Console. This codebase is one-time-products only.
- Rewarded ads remain opt-in and may still be watched for rewards after Remove Ads has been purchased.

## Worldwide pricing and availability

1. In Google Play Console, open the existing Ludo Paws app whose package is `com.rekixo.ludopaws`.
2. Keep the existing `remove_ads` product ID and configure it as a one-time product/permanent entitlement, not as a subscription. Use the current Play Console one-time-product UI for the product's purchase option and regional availability.
3. Set the source/base price to **US$0.99** as an initial suggested Remove Ads price for the intended one-time purchase option. Select all countries/regions where this app and one-time products should be offered. Apply Play's one-time local-currency conversion to those regions, then review each generated price and any region-specific restrictions/taxes. Save the product and pricing changes.
4. Confirm the app's own distribution availability also includes the intended countries. Product availability cannot make the app purchasable in a country where the app itself is unavailable.
5. Review each Gem product's availability and regional price independently. Do not assume the Remove Ads price config automatically configures Gem packs.
6. Verify the product list after saving. Google Play's local prices are configured price points based on a one-time conversion, local pricing patterns, and applicable taxes; they are not a live foreign-exchange conversion at each checkout. Where a currency is unsupported, Play may show a supported fallback currency. Do not promise an identical live-FX equivalent in every country.
7. Never add currency conversion rates or a hard-coded INR/USD checkout amount to Android. The app must show the formatted price returned by Google Play Billing (`ProductDetails`). If no localized price is available yet, the app shows `Price unavailable` rather than inventing a regional price.

## Secure purchase verification prerequisites

1. In Google Cloud, enable the Google Play Android Developer API for the project used by the Play Console app.
2. Create/use a dedicated service account. Grant it the appropriate app access in Play Console and verify that its permissions allow reading and finalizing in-app purchases. The app package and product IDs must match this repository exactly.
3. Add these as **Cloudflare Worker secrets**, not as Android properties, committed files, or ordinary public `vars`:
   - `GOOGLE_PLAY_SERVICE_ACCOUNT_EMAIL`
   - `GOOGLE_PLAY_SERVICE_ACCOUNT_PRIVATE_KEY`
4. The Worker configuration already declares the `LUDOPROOF_STORE_PURCHASES` Durable Object binding used by the idempotent purchase ledger. Confirm the binding is present in the active Cloudflare deployment/environment.
5. Keep `GOOGLE_PLAY_BILLING_ENABLED` false until service-account access, product availability, regional prices, the ledger binding, and license-tester purchase/restore checks are ready.
6. Build Android with `LUDOPROOF_BILLING_ENABLED=true` only for a controlled internal/license-tester build after the server is ready. This is separate from the Worker-side `GOOGLE_PLAY_BILLING_ENABLED` gate.
7. Do not place service-account private keys in `gradle.properties`, `local.properties`, GitHub source, APK resources, or client logs. Rotate a key if it is ever exposed.

## Required acceptance before any public rollout

- The app queries only one-time `INAPP` products and receives localized prices for each available SKU.
- The Play purchase dialog price matches the price shown in the app for the testing country/region.
- A successful purchase is independently verified by the server before any gems or entitlement are granted.
- Gem packs are consumed only after verification; Remove Ads is acknowledged and remains owned across app restarts and account restore.
- Purchase retries and duplicate callbacks do not double-grant. Pending or invalid purchases never grant content.
- Remove Ads suppresses eligible non-rewarded ads, while the explicitly disclosed opt-in rewarded-ad reward feature remains available.
- Refund/revocation behavior is checked with appropriate Play test facilities and the entitlement policy before rollout.
- Run the full Android and backend test suites after the implementation batch is complete, then verify the purchase flow with Play license testers on an internal testing track.
- Production deployment, monetization activation, and public rollout require a separate explicit approval. Passing unit tests alone is not proof of real Play Console configuration or end-to-end purchase readiness.
