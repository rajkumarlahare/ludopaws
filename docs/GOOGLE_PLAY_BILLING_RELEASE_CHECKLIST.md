# Google Play Billing — Production Readiness and Release Checklist

**Scope:** Current production implementation supports Google Play **one-time in-app products** only. This checklist does not enable deployment, change flags, or add subscription behavior. Keep the one-time `remove_ads` SKU semantics unchanged.

## 1. Supported product IDs

These IDs are hard-coded in the server verifier and must match products provisioned in Play Console exactly:

| Product ID | Type | Server fulfillment |
| --- | --- | --- |
| `remove_ads` | One-time, non-consumable | Persistent Remove Ads entitlement (legacy-compatible behavior) |
| `gems_300` | One-time, consumable | 300 Gems |
| `gems_1500` | One-time, consumable | 1,200 Gems |
| `gems_3500` | One-time, consumable | 3,000 Gems |
| `gems_20000` | One-time, consumable | 7,500 Gems |
| `gems_40000` | One-time, consumable | 16,000 Gems |
| `gems_200000` | One-time, consumable | 35,000 Gems |

Do not infer a purchase SKU from a display label, create a different ID in Play Console, or convert `remove_ads` to a subscription. Verify that each product exists, has the correct product type, is active/available for the test region, and has a price configured. Only show prices returned by Google Play as checkout prices.

## 2. Configure the Google Play Developer API identity

1. In Google Cloud Console, select the project that owns the service account, enable **Google Play Developer API**, and create/identify the service account.
2. In Play Console, open **Users and permissions**, invite that exact service-account email, and grant the Play Billing permissions needed to view financial data/orders and manage orders/subscriptions.
3. Generate/download a service-account key only if one is not already available. Never commit the JSON key, private key, or credentials to Git, Android resources, Gradle properties committed to source, or chat.

Official setup guide: https://developers.google.com/android-publisher/getting_started

## 3. Configure runtime values without exposing secrets

In the Cloudflare dashboard, open the `ludoproof-game-api` Worker and add/update the secret:

- `GOOGLE_PLAY_SERVICE_ACCOUNT_PRIVATE_KEY`: the service-account JSON's `private_key` PEM value, including the BEGIN/END PRIVATE KEY lines.

In GitHub repository settings, open **Settings → Environments → production → Environment variables** and set:

- `GOOGLE_PLAY_SERVICE_ACCOUNT_EMAIL`: exact service-account email.
- `GOOGLE_PLAY_BILLING_ENABLED`: `true` only after the internal purchase tests below pass.
- `LUDOPROOF_BILLING_ENABLED`: same value as `GOOGLE_PLAY_BILLING_ENABLED`.

The production workflow checks that the Cloudflare private-key secret exists, that the email is correctly formatted, and that the Android/Worker flags match. These preflight checks do **not** prove that Google accepts the key or that the service account has permission to read/consume/acknowledge the products. The credential remains a Cloudflare secret; do not add it as a repository variable or print it in logs.

Official permission/setup guide: https://developers.google.com/android-publisher/getting_started

## 4. Build an explicitly Billing-enabled test candidate

Billing is deliberately disabled by default in Gradle. Keep the default disabled; enable it only for the candidate being tested.

From PowerShell:

```powershell
Set-Location D:\dev\ludoproof\android

.\gradlew.bat -PLUDOPROOF_BILLING_ENABLED=true :app:testDebugUnitTest
.\gradlew.bat -PLUDOPROOF_BILLING_ENABLED=true :app:lintRelease
.\gradlew.bat -PLUDOPROOF_BILLING_ENABLED=true :app:assembleRelease :app:bundleRelease
```

Before uploading an AAB to a track, verify the release signing configuration and inspect the package ID: `com.rekixo.ludopaws`. A CI artifact is not necessarily signed with the Play upload key.

## 5. Test using Play Console's internal test track and license testers

Use a Google account registered as a license tester. Install the candidate from the Play test-track opt-in link and confirm the Play Store account on the test device is the intended tester. Do not use a real-paying account for initial validation.

Required checks:

- Product query: all expected SKUs appear with Google Play localized prices; missing/unavailable SKUs are not offered for purchase.
- Consumable purchase: buy `gems_300`; the balance changes by exactly 300 after server verification.
- Idempotency: repeat the verification/relaunch/reconnect path for the same purchase token; the wallet must not receive the Gems twice.
- Non-consumable purchase: buy `remove_ads`; server verification must acknowledge it and the account must receive the existing Remove Ads entitlement.
- Reconciliation: restore/query purchases after force-stop and restart; legitimate prior non-consumable purchases must be recoverable without granting a purchase to a different account.
- Pending and cancelled payment: never grant Gems or Remove Ads until Google confirms `PURCHASED`.
- Error paths: disable network or make the API unreachable during verification; do not grant unverified content. A retry must recover safely without a duplicate grant.
- SKU mismatch / wrong account: verification must fail closed and must not grant value.
- Verify that consumable products are consumed and non-consumable products are acknowledged server-side. Google requires purchases to be processed/acknowledged within its specified window.

Google's billing test guide: https://developer.android.com/google/play/billing/test

Purchase-processing guidance: https://developer.android.com/google/play/billing/integrate

## 6. Deploy and verify only after test acceptance

1. Confirm all source/CI checks pass on the exact commit being released.
2. In **Actions → LudoProof Cloudflare Production → Run workflow**, select the reviewed release commit on `main`. Production deployment is manual; merging a pull request does not itself deploy.
3. Confirm the workflow's production readiness, store status, and live game-flow checks succeed.
4. From PowerShell, query:

   ```powershell
   Invoke-RestMethod "https://api.ludopaws.rekixo.com/ready" | ConvertTo-Json -Depth 8
   Invoke-RestMethod "https://api.ludopaws.rekixo.com/api/store/status" | ConvertTo-Json -Depth 8
   ```

   Confirm `checks.playBillingEnabled = true` and `billingAvailable = true`. The optional `billingChecks` breakdown identifies whether the flag, service-account email, private-key PEM presence, purchase-ledger binding, account directory, or player-account binding is absent. It reports booleans only, not the private key.

5. Install the same Billing-enabled, correctly signed release candidate from the internal test track and repeat the purchase/restore checks against the deployed Worker.

## Release gate

Do not describe Billing as production-ready merely because the flag is true or `/ready` returns 200. Release acceptance requires: matching Play Console SKUs, service-account authorization against the real Developer API, a Billing-enabled signed candidate, a successful license-tester purchase and restore, no double fulfillment, server consume/acknowledge success, passing CI, and an explicitly run successful production deployment.

Subscription billing is outside this one-time purchase checklist.
