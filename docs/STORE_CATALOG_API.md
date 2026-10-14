# Ludo Paws — Store Catalog API (Phase 2)

Status: feature-branch implementation only. This API is not production-deployed, and the Android Store UI does not consume it yet. The catalog starts empty. Do not enable billing, rewarded ads, or production deployment based on the presence of these endpoints.

## Authentication and administrative access

Every endpoint below requires an authenticated Ludo Paws account access token in the Authorization: Bearer <access-token> header. The normal account session is checked against PlayerAccount on each request.

Admin endpoints additionally require the authenticated account UUID to be listed in the Cloudflare Worker variable LUDOPROOF_CATALOG_ADMIN_ACCOUNT_IDS. Configure a comma-separated list of exact account UUIDs only after the operator account is identified. If the variable is missing or has no valid UUID, admin access fails closed with 503 CATALOG_ADMIN_NOT_CONFIGURED. A logged-in non-admin receives 403 CATALOG_ADMIN_FORBIDDEN. Do not put bearer tokens in that variable.

Mutating admin requests must supply:
- expectedRevision: revision returned by the admin-state endpoint. A stale revision receives 409 CATALOG_REVISION_CONFLICT; fetch the latest state before retrying.
- reason: a 3–200 character operator reason recorded in the audit trail.
- Each operation records the authenticated server-derived admin account ID. Clients cannot select the audit actor.

## Endpoints

| Method | Path | Purpose |
|---|---|---|
| GET | /api/store/catalog | Authenticated client read of currently available, published items |
| GET | /api/admin/store/catalog | Admin state: published/archived items, drafts, revision and audit |
| GET | /api/admin/store/catalog/versions?limit=30 | Recent immutable published snapshots and audit records |
| PUT | /api/admin/store/catalog/items/{itemId} | Create or replace a DRAFT, PREVIEW, or SCHEDULED item |
| POST | /api/admin/store/catalog/publish | Validate and publish a saved draft |
| POST | /api/admin/store/catalog/archive | Archive a currently published item without deleting ownership data |
| POST | /api/admin/store/catalog/rollback | Restore a retained published snapshot as a new monotonically increasing version |

### Save a draft

    PUT /api/admin/store/catalog/items/paw_safari_01
    Authorization: Bearer <access-token>
    Content-Type: application/json

    {
      "expectedRevision": 0,
      "reason": "Create the first Safari Paw draft",
      "item": {
        "id": "paw_safari_01",
        "category": "PAW",
        "name": "Safari Paw",
        "description": "Safari-themed Paw cosmetic",
        "status": "DRAFT",
        "contentReady": false,
        "asset": {
          "thumbnail": "r2://staging/paw_safari_01/v1/thumbnail.png",
          "manifest": "r2://staging/paw_safari_01/v1/manifest.json",
          "version": 1,
          "sha256": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
          "format": "GLB"
        },
        "unlock": {
          "channels": ["GEMS"],
          "gems": { "enabled": true, "price": 250 }
        },
        "availability": {
          "worldwide": true,
          "allowedCountries": [],
          "blockedCountries": [],
          "startsAt": null,
          "endsAt": null
        },
        "catalogVersion": 1
      }
    }

Drafts are not returned to game clients. The example is illustrative only; r2:// metadata does not upload or validate the real asset bundle. The Phase 3 R2 upload/manifest pipeline remains unimplemented.

### Publish, archive, or roll back

Publish a previously saved draft:

    { "expectedRevision": 1, "itemId": "paw_safari_01", "reason": "Assets and gameplay review passed" }

Archive an item:

    { "expectedRevision": 2, "itemId": "paw_safari_01", "reason": "Retire the item from sale" }

Restore snapshot version 1, creating a new published version:

    { "expectedRevision": 3, "targetVersion": 1, "reason": "Rollback after catalog regression" }

Each object is sent as the request body to its matching /api/admin/store/catalog/... endpoint. Publication and rollback create snapshots. The API retains the latest 100 published snapshots and up to 250 audit events.

## Read caching and availability

The client read returns a quoted ETag derived from the published catalog version, Cloudflare country code, and visible item IDs. Clients can send If-None-Match; an unchanged catalog returns 304 Not Modified. Responses are private-cacheable for 60 seconds and vary by CF-IPCountry. Only PUBLISHED, content-ready items inside their start/end schedule and country rules are returned. The response also contains `suppressedIds`: opaque stable IDs for canonical catalog entries that are currently not visible (for example, archived, scheduled for the future, or region-blocked). This ID-only list lets Android hide matching bundled legacy offers without disclosing hidden item metadata.

The ETag version changes only when the published catalog changes. Draft edits advance the admin revision but do not change the public catalog version.

## Explicit implementation boundaries

- The catalog is now consulted by PlayerAccount on each purchase, equip, and cosmetic-targeted rewarded-ad callback. If the ID exists in current catalog state, its status, content readiness, schedule, country eligibility, enabled unlock channel and server-configured Gem/ad price take precedence over the legacy map. An archived/unavailable item cannot fall back to a same-named legacy entry; catalog read failures fail closed when the binding is configured. Legacy IDs absent from catalog state continue to use the existing map during migration.
- The Android Store still uses its current UI/data. Dynamic catalog display, multi-channel purchase choice in the client, safe catalog caching, and renderer/asset integration remain Phase 4; do not launch dynamic items until that client integration and device tests are complete.
- PLAY_PRODUCT drafts are allowed for planning, but publishing them is blocked with 409 CATALOG_PLAY_PURCHASE_NOT_READY until the Google Play verifier can grant the specific cosmetic entitlement. This prevents the existing Gem / Remove Ads verifier from being misused as a cosmetic purchase.
- BUNDLE publication is blocked until an atomic multi-item entitlement grant exists. LEVEL and EVENT entries may be saved as drafts, but publication is blocked until server-authoritative progression/event verification is implemented. Currently publishable unlock methods are FREE, GEMS, and REWARDED_ADS.
- This phase does not add asset uploads, a web Admin Studio, entitlement writes, wallet deductions, rewarded-ad rewards, subscriptions, or client UI.
- A Durable Object migration is included in Wrangler config. It takes effect only when the Worker is deliberately deployed through the protected manual deployment workflow.
