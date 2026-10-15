# Ludo Paws Admin Studio — Acceptance Checklist

This defines the planned acceptance contract. It is not a claim that the Admin Studio already exists.

## Administration and access
- [ ] Hosted under the Rekixo-owned Ludo Paws subdomain after DNS/TLS review.
- [ ] Only explicitly authorized accounts can create, edit, upload, publish, schedule, archive or rollback.
- [ ] Server validates authorization on every admin API call; hiding buttons is not authorization.
- [ ] Admin changes have an append-only audit event with actor, item ID, operation, catalog version and timestamp. Never log credentials or secret upload URLs.

## Asset upload
- [ ] Upload target is a private staging prefix/bucket path; publication occurs only after validation.
- [ ] Validate extension and sniffed MIME type; never trust file extension alone.
- [ ] Validate file/archive size, dimensions, item count, compression ratio, manifest paths and SHA-256.
- [ ] Reject path traversal, duplicate archive entries, executable content and formats unsupported by the client renderer.
- [ ] Use immutable, versioned R2 object keys for published assets.
- [ ] Preview assets before publication; rollback to a previously validated version.
- [ ] Ensure R2 public delivery exposes only published assets, never private staging uploads.

## Catalogue and publication
- [ ] Immutable stable IDs; unique by category namespace and validated globally.
- [ ] Independent unlock channels: rewarded ads, Gems, direct Play purchase, level and event.
- [ ] Channel price/progress is server-validated; at least one configured channel is required for publishing.
- [ ] `PUBLISHED` requires `contentReady=true`, verified asset manifest and supported format.
- [ ] If content becomes unavailable, block new unlocks but preserve ownership of already-owned items.
- [ ] Archive never deletes item ownership or reuses the ID.
- [ ] Country and date restrictions are enforced server-side. The Play product must also be available in the user's Play country.
- [ ] Every publication increments a version, validates all product references, and supports rollback.

## Purchase security
- [ ] Gems debit and ownership insert commit atomically and are idempotent.
- [ ] Rewarded progress counts only unique, valid SSV transactions accepted by the server.
- [ ] One-time Play purchases are verified with the Google Play Developer API and reserved in the existing purchase ledger.
- [ ] Server maps SKU to item; client cannot supply the amount, category or entitlement.
- [ ] Items purchased by one route stay owned when catalog prices or channels change.

## Admin UX
- [ ] Draft, preview, save, publish, schedule, archive and rollback actions have explicit confirmation for destructive/unpublishing changes.
- [ ] Form validates channel combinations and prices before saving.
- [ ] Catalog list supports filtering by category, status, unlock method and country.
- [ ] Upload progress/errors and asset previews are visible.
- [ ] A pre-publish report lists missing assets, unknown SKUs, invalid regional availability and unsupported formats.

## Release gates
- [ ] Server tests, Android tests, release lint/build and security checks pass.
- [ ] Billing and SSV remain disabled unless the complete internal-test plan is signed off.
- [ ] Admin Studio hosting is deployed separately and does not alter unrelated Rekixo/AR3D Workers/routes.
