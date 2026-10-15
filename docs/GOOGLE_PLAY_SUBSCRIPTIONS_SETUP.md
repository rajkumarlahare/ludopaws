# Google Play Paws Plus Subscription Setup — Ludo Paws

This checklist covers only the recurring Paws Plus plan. The six Gem packs and `remove_ads` remain one-time `INAPP` products; Paws Plus uses the separate Google Play `SUBS` product type.

## Fixed product contract

- Android package: `com.rekixo.ludopaws`
- Product ID: `paws_plus`
- Base plan ID: `monthly-auto`
- Billing: auto-renewing monthly; Google Play displays the actual localized price and billing terms.
- Active entitlement: suppress eligible forced post-win interstitial ads. Optional rewarded ads remain a separate, voluntary choice.
- Cancellation stops future renewal; access follows the server-verified Google Play subscription state and expiry. Do not infer active access from a cached local flag.
- The runtime gates `GOOGLE_PLAY_SUBSCRIPTIONS_ENABLED` and `LUDOPROOF_SUBSCRIPTIONS_ENABLED` default to false. Do not enable the feature until every prerequisite and test below is complete.

## 1. Provision the Play Console product

1. Open the correct Play Console app for package `com.rekixo.ludopaws`.
2. Create/verify a subscription whose product ID is exactly `paws_plus`.
3. Create/verify the auto-renewing monthly base plan whose ID is exactly `monthly-auto`. Do not change the identifiers to match a display name.
4. Set the supported countries/regions, billing price, taxes and availability in Play Console. This repository intentionally does not hard-code a currency price. Verify the actual price and billing period returned by ProductDetails on a real license-tester account.
5. Ensure the subscription's purchase option/base plan is active and available to the intended internal/closed-track testers. A repository constant alone does not activate the product in Google Play.

## 2. Google Play Developer API and acknowledgement

1. Enable the Google Play Android Developer API in the Cloud project linked to this app.
2. Use a dedicated service account and grant it least-privilege Play Console app access sufficient to query subscription purchases and acknowledge them. Confirm the package, product ID and base plan are correct.
3. Configure the production GitHub environment at **Settings → Environments → production → Environment variables**:
   - `GOOGLE_PLAY_SERVICE_ACCOUNT_EMAIL`: service account used by the server to call Google Play.
   - `GOOGLE_PLAY_BILLING_ENABLED=true` and `LUDOPROOF_BILLING_ENABLED=true` are the existing one-time billing gates; keep them matched.
   - `GOOGLE_PLAY_SUBSCRIPTIONS_ENABLED=true` and `LUDOPROOF_SUBSCRIPTIONS_ENABLED=true` are the additional subscription gates. Keep them false until RTDN is configured and end-to-end tests pass.
4. In the Cloudflare Worker secrets, configure `GOOGLE_PLAY_SERVICE_ACCOUNT_PRIVATE_KEY` with that service account's private-key PEM. Never place private keys in GitHub variables, Android properties, committed code, issue comments or chat.
5. Verify the account has access to the relevant subscription APIs and purchase acknowledgement operations. Newly purchased subscriptions must be acknowledged by the backend after verification; tests should confirm Play reports the purchase as acknowledged.

## 3. Set up authenticated Real-time Developer Notifications (RTDN)

Google Play subscription changes must be delivered to the server so renewals, cancellations, grace periods, account holds, expiry, refunds and revocations are reflected rather than relying on the app opening again.

1. In the Google Cloud project associated with Play Console, enable Pub/Sub and create a topic dedicated to Play subscription notifications. Configure Google Play Console Monetization setup / Real-time developer notifications to publish to that topic.
2. Create a **push** subscription for the topic whose HTTPS endpoint is exactly:

   `https://api.ludopaws.rekixo.com/api/google-play/rtdn`

3. Configure authenticated push delivery with an OIDC token. The token's audience must be that exact URL. Choose the service account that Pub/Sub uses to sign push tokens; authorize the Pub/Sub service agent to mint tokens for it, following Google's Pub/Sub authenticated-push instructions.
4. Set these non-secret variables in **GitHub Settings → Environments → production → Environment variables**:
   - `GOOGLE_PLAY_RTDN_AUDIENCE=https://api.ludopaws.rekixo.com/api/google-play/rtdn`
   - `GOOGLE_PLAY_RTDN_EMAIL`: the exact service-account email present as the verified OIDC token email on authenticated push delivery. This may be separate from the Play Developer API service account.
5. Validate a real Pub/Sub push to the endpoint in a non-production test first. The backend rejects requests when the OIDC signature/token, exact audience, email, verified-email flag, issuer or expiry do not validate. Do not expose or accept unauthenticated RTDN.
6. Ensure the actual subscription record is registered via the authenticated Android purchase-verification flow before relying on notifications. The handler deduplicates message IDs and re-queries Google Play rather than trusting the notification payload as the entitlement source.

## 4. Build configuration

For internal validation only, provide `LUDOPROOF_SUBSCRIPTIONS_ENABLED=true` through the Android Gradle property/build configuration, with the Worker subscription flag and RTDN configuration also validated. Release build automation must not silently turn this flag on. A disabled or misconfigured server must return subscription availability as false, and the store must keep the Subscribe button disabled.

The current release workflows read production variables from the `production` GitHub environment; environment-scoped variables override repository variables of the same name. Only the service-account private key belongs in Cloudflare Worker secrets. The production Worker workflow is manual and must not be run just because code has been committed.

## 5. Required license-tester acceptance

On an internal or closed Play testing track, use a real tester Google account and verify all of the following before switching either subscription flag on for a public build:

- Google Play returns the correct `paws_plus` product and eligible `monthly-auto` base-plan offer; the app displays Play's localized recurring price and the Play purchase sheet shows the same terms.
- A successful purchase is verified server-side, bound to the correct signed-in Ludo Paws account, acknowledged with Google Play and stored idempotently. Pending, invalid, mismatched and unverified purchases must not activate Paws Plus.
- Relaunch/restore and a second device recover the correct entitlement from the server. A purchase token cannot activate a different Ludo Paws account.
- Active subscription suppresses only eligible forced post-win interstitials. Voluntary rewarded ads still work and grant the configured five Gems only after server-side AdMob SSV verification.
- RTDN tests cover initial purchase, renewal, cancellation with remaining paid time, grace period, account hold, expiry, refund/revoke, duplicate notification IDs, delayed and out-of-order notifications, unknown/unregistered tokens, transient Google API failure and retry.
- App/server restart, network loss and account deletion do not leave stale active entitlement. An expired/revoked subscription is not active merely because a local cache or delayed notification says it was previously active.
- One-time Gem/Remove Ads purchase, consume/acknowledgement, restore and refund tests still pass unchanged.
- CI passes for the exact commit and a human verifies the configured Play Console and Pub/Sub resources before enabling the gates.

## 6. Release controls

- Do not hard-code localized prices or infer a renewal price from an old checkout.
- Keep the subscription flags false until Play Console activation, API permissions, authenticated RTDN delivery, end-to-end license-tester lifecycle and release review all pass.
- Keep Pricing, Privacy and Terms review markers in place until the publisher confirms the support inbox, target age/audience and child-directed obligations, refunds/consumer terms, provider/data-safety disclosures and public account-deletion workflow.
- Production deployment and public activation are separate manual decisions. Unit tests and `/api/store/status` readiness alone do not prove that a live monthly subscription is saleable or compliant.
