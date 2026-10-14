# Ludo Paws — Configurable Store & Monetization Roadmap

Status: planning baseline plus an initial Phase 2 catalog API implementation on feat/ludopaws-production-readiness. The Worker is not production-deployed, the catalog starts empty, and Android does not consume this API yet. This roadmap does not enable billing, rewarded ads, or production deployment.

## Objective

Build a server-authoritative, editable catalogue for Paws, dice, boards, and future cosmetics. Each item must have a stable ID and independently configurable eligibility, unlock channels, pricing, asset references, regional publishing, and availability. Adding a new item should normally require an authenticated Admin Studio change and asset upload—not an Android rebuild. A new renderer or game behavior may still require a client release.

## Non-negotiable compatibility and safety constraints

- Preserve the existing Android application ID `com.rekixo.ludopaws`.
- Preserve deployed Worker/Durable Object names, namespaces, persisted schema semantics, match/proof identifiers, and protocol identifiers unless a separately reviewed migration explicitly authorizes a change.
- Keep `GOOGLE_PLAY_BILLING_ENABLED=false`, `LUDOPROOF_BILLING_ENABLED=false`, `LUDOPROOF_ADMOB_SSV_ENABLED=false`, and `LUDOPROOF_ADS_ENABLED=false` until internal end-to-end acceptance passes.
- Never trust client-supplied price, gem cost, ad progress, ownership, purchase state, subscription state, or entitlement expiry.
- Do not publish incomplete assets or catalog entries. A disabled/unavailable item must not accept purchase, consume gems, or become equipable.
- Existing four starter Paws (Dog, Cat, Goat, Duck) remain free and unlocked. Do not remove ownership already granted.
- Keep catalog changes separate from production deployment. Production Worker deployment remains manual workflow dispatch only.
- Do not add secrets, OAuth private credentials, Google Play service-account keys, or AdMob signatures to Git.

## Phases and acceptance criteria

### Phase 0 — Baseline and release safety
1. Preserve current main and record the current Store, wallet, purchase-verification and SSV behavior.
2. Keep deployment and purchase flags disabled.
3. Identify all authoritative and cached state boundaries.
4. Acceptance: tests establish no client-only currency grants and current IDs/flows remain compatible.

### Phase 1 — Canonical catalog schema
Each item uses:
- `id`: immutable stable ID;
- `category`: PAW, DICE, BOARD, or future cosmetic category;
- `name`, `description`, `localizationKey`;
- `thumbnail`, `assetManifest`, `assetVersion`, `supportedFormats`;
- `status`: DRAFT, PREVIEW, SCHEDULED, PUBLISHED, ARCHIVED;
- `contentReady`, `createdAt`, `updatedAt`;
- `unlock.channels`: independently enabled FREE, REWARDED_ADS, GEMS, PLAY_PRODUCT, LEVEL, EVENT;
- each channel has its own validated rule/configuration;
- `playProductId` must map to a provisioned Google Play one-time product; it is never invented dynamically during checkout;
- `availability`: country allow/deny list, start/end timestamps, and store placement;
- `catalogVersion` and an audit record for every publication.
Acceptance: schema validation and tests reject duplicate IDs, invalid prices, invalid ad counts, unknown product IDs, invalid assets and unsupported renderer formats.

### Phase 2 — Server-authoritative catalog API
- Add authenticated catalog read and admin write/publish endpoints.
- The API returns stable item IDs and the allowed unlock channels, not authority to grant ownership.
- All purchase/unlock requests re-check item status, content readiness, eligibility and prices on the server.
- Use ETags/version numbers, caching and rollback of catalog versions.
- Acceptance: stale clients cannot unlock archived/unavailable items; changing metadata does not delete ownership.

Initial feature-branch implementation adds authenticated catalog reads with ETag/country availability filtering, account-allowlisted draft/publish/archive/rollback routes, optimistic revision checks, audit events, and a versioned Durable Object store. It remains undeployed. The new API is not yet wired into the legacy PlayerAccount cosmetic purchase/equip authority or Android UI, so the Phase 2 acceptance criterion for every purchase/unlock request remains open. Direct Play-product cosmetic publication is blocked until the verifier can grant the specific item entitlement. See docs/STORE_CATALOG_API.md.

### Phase 3 — Admin Studio and asset pipeline
Target host: `studio.ludopaws.rekixo.com`, only after DNS/TLS and authentication setup.
- Restricted admin authentication; initially only the authorized Rekixo administrator account.
- Draft, preview, publish, schedule, archive, rollback and audit trail.
- Upload thumbnails and game assets to Cloudflare R2 with server-side validation of content type, size, dimensions, archive structure, manifest and hashes.
- Upload to a versioned object path; never overwrite a published asset in place. Publish the manifest only after the complete bundle is validated.
- Serve public game assets with cache headers; keep admin upload APIs authenticated.
- Acceptance: unauthorized users cannot upload or publish; incomplete/invalid bundles fail closed; rollback restores the earlier manifest.

### Phase 4 — Android dynamic store
- Tabs for Paws, Dice, Boards and Gems.
- Render item states from the server catalogue: free, owned, locked, progress, unavailable, selected.
- Use Google Play-returned localized formatted prices for all Play checkout UI; never treat fallback prices as checkout authority.
- Cache catalogue safely and refresh by version; network errors never grant value.
- Acceptance: a new content-ready catalog item appears after publishing without an Android release when its format is already supported.

### Phase 5 — Three unlock channels for cosmetics
A published item can independently enable one or more:
1. Rewarded ads: unlock after the configured count of distinct, valid server-verified SSV transactions.
2. Gems: atomic wallet deduction and ownership grant in one Durable Object transaction.
3. Direct purchase: Google Play one-time product verification and ledger idempotency, then permanent ownership.
Optional LEVEL/EVENT methods require server-authoritative progression/event evidence.
- Each transaction is idempotent and is bound to account + stable item/product ID.
- Direct cosmetic purchases must be modeled as non-consumable one-time products unless a separate clearly defined consumable makes commercial sense.
- Acceptance: duplicate callbacks/purchase retries cannot double-charge or double-grant; already-owned items do not charge again.

### Phase 6 — Paws Plus monthly subscription
- Create a new subscription product ID and monthly auto-renewing base plan. Keep legacy `remove_ads` semantics intact; do not silently reinterpret an existing one-time product ID as a subscription.
- Proposed subscription ID: `paws_plus`; base plan: `monthly-auto`.
- Active entitlement: suppress forced/post-win interstitial ads and other explicitly documented forced ads; keep opt-in rewarded ads available so players can still earn rewards. This does not mean all advertisements are disabled if the player deliberately chooses a reward ad.
- Add recurring value that is real and delivered every cycle, such as a monthly cosmetic rental/perk, rotating subscribers-only cosmetics, or a fixed monthly Gem allowance. Any bonus must be supplemental to the core ongoing benefits and must be granted once per verified billing period.
- Verify subscription state through Google Play Developer API subscription endpoints. Store current state/expiry server-side; process renewal, grace period, account hold, pause where offered, cancellation, expiry, revocation/refund and replacement/upgrade. Integrate Real-time Developer Notifications or an equally reliable reconciliation process.
- Provide restore/refresh status and an easy Google Play subscription-management/cancellation entry in the app.
- Clearly disclose price, billing period, auto-renewal, cancellation and what remains accessible without subscribing.
- Acceptance: sandbox/license-tester flows pass initial purchase, renewal, cancellation, expiry, restore, hold/grace period and refund/revocation; expired subscriptions do not keep benefits.

### Phase 7 — Stable Google Play product catalogue and pricing
- Existing one-time IDs remain stable: `gems_300`, `gems_1500`, `gems_3500`, `gems_20000`, `gems_40000`, `gems_200000`, and the existing `remove_ads` one-time product for compatibility.
- New product IDs must be added only after server verifier, Android Billing query types, and tests support them.
- Use a new ID such as `paws_plus` for subscription. Do not create Play Console products until code and test contracts agree.
- Currency prices are configured in Play Console by region. Use the localized Play price from ProductDetails at runtime. Any admin-authored direct-purchase price must be tied to an actual provisioned product/offer.
- Initial pricing is a hypothesis to test, not a promise of profitable unit economics: test low-friction India tiers and regional Play Console defaults first; review conversion, refund rate, payer retention and net revenue after fees/taxes. Avoid equal numeric prices across countries.
- Acceptance: every production-visible SKU exists in the correct product type, has regional availability/pricing, is queryable by the app and verifiable on the backend.

### Phase 8 — Security and automated quality gates
- Unit/integration tests for catalog validation, admin authorization, Gem atomicity, ad SSV duplicates, purchase-token replay, account deletion races, asset validation and version rollback.
- Google Play Billing license testers and internal testing track.
- Subscription lifecycle tests.
- CI gates: server tests, Wrangler dry-run, Android JVM tests, lint, build/AAB generation, asset/schema tests and security checks.
- Acceptance: no failed required CI checks; no secret or production flag is introduced in source.

### Phase 9 — Cost, telemetry and staged rollout
Track:
- DAU/MAU, sessions per user, concurrent matches;
- Worker requests/CPU, Durable Object requests/duration/storage;
- R2 storage, Class A/Class B operations and asset bundle sizes;
- upstream EntroNex requests and actual provider cost/latency/failure rate;
- AdMob impressions, fill, completed rewarded views, eCPM and net revenue;
- purchase conversion, average revenue per paying user, refunds, churn and subscription retention.
Use actual billing dashboards and observed request counts to calculate unit economics. Do not claim a precise cost per user from MAU alone.
Rollout: internal license testers → small closed/limited test cohort → staged public rollout → worldwide availability after policy, privacy, payment, ads and reliability checks.

## Subscription and monetization recommendation

A monthly plan that only hides one set of ads can be commercially fragile. Make Paws Plus an honest recurring-value plan with ongoing forced-ad suppression and at least one transparent recurring benefit. Proposed initial experiment: ₹29/month in India, then evaluate ₹39/month as an A/B candidate only if retention and conversion data support it. This is a pricing experiment, not a verified market benchmark. Keep optional rewarded ads usable for users who want ad rewards.

## Initial cosmetics price bands (test hypotheses; India starting point)

- Simple dice/board tint or basic paw variant: 100–300 Gems, or 3–5 completed rewarded ads where permitted.
- More detailed theme/character pack: 300–700 Gems, or 6–10 completed rewarded ads.
- Premium, high-production-quality character/board bundle: direct Play product around ₹49–₹149, localized per market.
- Avoid setting every item to all methods by default. Admin Studio should let the operator choose one or multiple methods per item.

These are product-design hypotheses. Adjust after measuring ad eCPM, reward completion, gem sink rates, payer conversion and production asset costs.

## Operational cost model

Use USD as the provider-billing baseline and convert only when reviewing the actual invoice. The current published Cloudflare Workers Paid plan starts at $5/month, includes 10 million Worker requests and 30 million CPU milliseconds, and charges beyond included usage; Durable Objects have separate request/duration/storage meters. R2 Standard currently lists 10 GB-month, 1 million Class A and 10 million Class B monthly free allocations, with no internet egress fee. Verify pricing in the Cloudflare billing console before launch.

EntroNex usage is an important unknown: the repository identifies the current v4 integration as an evaluation candidate but does not provide the contract price or real per-roll charge. Do not invent it. Measure billed provider calls and use:
`monthly upstream cost = verified billable API calls × contract price per call`.
A robust monthly operating model must include Workers, Durable Objects, R2, EntroNex, store fees/tax, customer support, privacy/legal operations and release monitoring. Advertising revenue is variable and should not be assumed to cover API expense until actual eCPM and fill rates are measured.

## Main-branch audit findings recorded for implementation
- `StoreCatalog.kt` currently defines `remove_ads` as `NON_CONSUMABLE_REMOVE_ADS`; the Worker grants a persistent `ads_removed` boolean. Keep that ID's existing behavior for compatibility and create a new subscription ID for monthly Paws Plus.
- `server/src/google-play-billing.js` currently verifies one-time `INAPP` purchases via products APIs and its hard-coded `GOOGLE_PLAY_PRODUCTS` map. Subscription purchase tokens, subscription status, acknowledgement, renewals and expiry are not implemented in that verifier yet.
- `server/src/player-account.js` has a hard-coded `COSMETICS` map and wallet/ad progress tables. Existing channels include FREE, GEMS and REWARDED_ADS, but new server-owned catalogue entries and arbitrary direct-purchase cosmetics require a catalog authority and SKU-to-item mapping.
- `server/src/index-account.js` exposes wallet, cosmetic purchase/select, one-time purchase verification, store status and SSV routes, but no catalog-management/admin routes or subscription lifecycle routes.
- Current ad SSV grants 5 Gems on a cosmetic-targeted rewarded view as well as one unit of that item's ad progress. Product design must preserve this explicitly or migrate it only with tests and clear UI disclosure.
- `server/wrangler.json` currently binds no R2 bucket. R2 upload/delivery requires adding a new explicit binding and deployment configuration; this plan does not modify Worker routing yet.
- The repository contains a Store tab and category catalog, but its local cosmetic metadata is not yet a remote, operator-editable published catalog. The proposed admin workflow must not claim dynamic publishing until catalog persistence/API and Android fetch/cache are implemented.

## Not in scope for this planning baseline

No code behavior, Google Play product, feature flag, secret, production deployment, production billing or ad activation has been changed by this plan. These require the phase-specific implementation and acceptance checks above.
