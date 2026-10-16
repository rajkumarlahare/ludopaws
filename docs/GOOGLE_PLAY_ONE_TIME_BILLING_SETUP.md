# Google Play One-Time Billing Setup — Ludo Paws

This document covers the six Gem-pack INAPP products and the non-consumable Remove Ads product. The separately documented Paws Plus monthly subscription has a different Play API product type, acknowledgement/lifecycle and RTDN requirements. Do not enable a billing flag merely because code is present.

## Product contract

- Android application ID: `com.rekixo.ludopaws`
- Permanent ad-removal product ID: `remove_ads` (keep this existing ID; do not delete/recreate it as a different SKU)
- `remove_ads` is a non-consumable one-time purchase. A server-verified purchase sets the account's permanent `ads_removed` entitlement and grants the configured one-time 5-gem bonus.
- Gem packs remain consumable one-time products: `gems_300`, `gems_1500`, `gems_3500`, `gems_20000`, `gems_40000`, and `gems_200000`.
- Rewarded ads remain opt-in and may still be watched for rewards after Remove Ads has been purchased.

## Worldwide pricing and availability

1. In Google Play Console, open the existing Ludo Paws app whose package is `com.rekixo.ludopaws`.
2. Keep the existing `remove_ads` product ID and configure it as a permanent non-consumable one-time purchase. Use the current Play Console one-time-product UI for the product's purchase option and regional availability.
3. The proposed source/base price for the Remove Ads purchase option is **US$1.00**. This is a planning recommendation, not a verified live Play Console price. Before launch, confirm the saved purchase option price, every intended country/region price, taxes and regional restrictions in Play Console. If you choose a different price, keep the app and website free of hard-coded currency amounts. Apply Play's regional pricing tools and review the generated prices before saving.
4. Confirm the app's own distribution availability also includes the intended countries. Product availability cannot make the app purchasable in a country where the app itself is unavailable.
5. Review each Gem product's availability and regional price independently. Do not assume the Remove Ads price config automatically configures Gem packs.
6. Verify the product list after saving. Google Play's local prices are configured price points based on a one-time conversion, local pricing patterns, and applicable taxes; they are not a live foreign-exchange conversion at each checkout. Where a currency is unsupported, Play may show a supported fallback currency. Do not promise an identical live-FX equivalent in every country.
7. Never add currency conversion rates or a hard-coded INR/USD checkout amount to Android. The app must show the formatted price returned by Google Play Billing (`ProductDetails`). If no localized price is available yet, the app shows `Price unavailable` rather than inventing a regional price.

## Secure purchase verification prerequisites

1. In Google Cloud, enable the Google Play Android Developer API for the project used by the Play Console app.
2. Create/use a dedicated service account. Grant it the appropriate app access in Play Console and verify that its permissions allow reading and finalizing in-app purchases. The app package and product IDs must match this repository exactly.
3. Store the two parts in their correct places:
   - `GOOGLE_PLAY_SERVICE_ACCOUNT_EMAIL`: a non-secret GitHub Actions variable in **Settings → Environments → production → Environment variables**. The production deployment workflow reads this value from the `production` environment.
   - `GOOGLE_PLAY_SERVICE_ACCOUNT_PRIVATE_KEY`: a **Cloudflare Worker secret** containing the service-account JSON `private_key` PEM value, including the BEGIN/END PRIVATE KEY lines. Never place this private key in GitHub variables, Android properties, committed files, or chat.
4. The Worker configuration already declares the `LUDOPROOF_STORE_PURCHASES` Durable Object binding used by the idempotent purchase ledger. Confirm the binding is present in the active Cloudflare deployment/environment.
5. The Worker deployment job declares `environment: production`. Set `GOOGLE_PLAY_BILLING_ENABLED` and `LUDOPROOF_BILLING_ENABLED` together under **Settings → Environments → production → Environment variables**. Environment-scoped variables take precedence over repository-level variables of the same name, so changing only **Settings → Secrets and variables → Actions → Variables** may not change the value consumed by the production workflow. Keep both flags false until service-account access, product availability, regional prices, the ledger binding, and license-tester purchase/restore checks are ready.
6. Set `LUDOPROOF_BILLING_ENABLED=true` for a controlled internal/license-tester Android build after the server is ready. This build flag is separate from the Worker-side `GOOGLE_PLAY_BILLING_ENABLED` gate, and release workflow validation requires them to match.
8. Keep `GOOGLE_PLAY_SUBSCRIPTIONS_ENABLED=false` and Android `LUDOPROOF_SUBSCRIPTIONS_ENABLED=false` until the separate subscription setup in `docs/GOOGLE_PLAY_SUBSCRIPTIONS_SETUP.md` has been completed and independently checked.
7. Do not place service-account private keys in `gradle.properties`, `local.properties`, GitHub source, APK resources, or client logs. Rotate a key if it is ever exposed.

## Required acceptance before any public rollout

- The app queries the six Gem packs and `remove_ads` as `INAPP` products and receives localized prices for each available SKU.
- The Play purchase dialog price matches the price shown in the app for the testing country/region.
- A successful purchase is independently verified by the server before any gems or entitlement are granted.
- Gem packs are consumed only after verification; Remove Ads is acknowledged and remains owned across app restarts and account restore.
- Purchase retries and duplicate callbacks do not double-grant. Pending or invalid purchases never grant content.
- Remove Ads suppresses eligible non-rewarded ads, while the explicitly disclosed opt-in rewarded-ad reward feature remains available.
- Refund/revocation behavior is checked with appropriate Play test facilities and the entitlement policy before rollout.
- Run the full Android and backend test suites after the implementation batch is complete, then verify the purchase flow with Play license testers on an internal testing track.
- Production deployment, monetization activation, and public rollout require a separate explicit approval. Passing unit tests alone is not proof of real Play Console configuration or end-to-end purchase readiness.
- The one-time purchase verifier must never accept the `paws_plus` subscription SKU; its verification and lifecycle belong only to `/api/store/subscriptions/verify` and the separately gated subscription API.
