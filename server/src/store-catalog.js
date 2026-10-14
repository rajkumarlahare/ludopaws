// Keep this validator usable in both Node-based tests and the Cloudflare Worker runtime.
// The canonical JSON Schema lives in docs/STORE_CATALOG_SCHEMA.json; its ID is mirrored here.
const SCHEMA_ID = "https://ludopaws.rekixo.com/schemas/store-catalog-item.schema.json";

const VALID_CHANNELS = new Set(["FREE", "REWARDED_ADS", "GEMS", "PLAY_PRODUCT", "LEVEL", "EVENT"]);
const VALID_CATEGORIES = new Set(["PAW", "DICE", "BOARD", "AVATAR", "BUNDLE"]);
const VALID_STATUSES = new Set(["DRAFT", "PREVIEW", "SCHEDULED", "PUBLISHED", "ARCHIVED"]);
const VALID_ASSET_FORMATS = new Set(["GLB", "GLTF", "PNG_SET", "SPRITE_ATLAS", "BUILT_IN"]);
const COUNTRY_CODE = /^[A-Z]{2}$/;
const ITEM_ID = /^[a-z0-9][a-z0-9_-]{2,63}$/;
const PRODUCT_ID = /^[a-z][a-z0-9_]{2,63}$/;
const SHA256 = /^[a-f0-9]{64}$/;

const ISO_DATE_TIME =
  /^(\d{4})-(0[1-9]|1[0-2])-(0[1-9]|[12]\d|3[01])T([01]\d|2[0-3]):[0-5]\d:[0-5]\d(?:\.\d+)?(?:Z|[+-](?:[01]\d|2[0-3]):[0-5]\d)$/;

function parseAvailabilityDateTime(value) {
  if (typeof value !== "string") return Number.NaN;
  const match = value.match(ISO_DATE_TIME);
  if (!match) return Number.NaN;
  const year = Number(match[1]);
  const month = Number(match[2]);
  const day = Number(match[3]);
  const leapYear = year % 4 === 0 && (year % 100 !== 0 || year % 400 === 0);
  const daysByMonth = [31, leapYear ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];
  if (day > daysByMonth[month - 1]) return Number.NaN;
  const timestamp = Date.parse(value);
  return Number.isFinite(timestamp) ? timestamp : Number.NaN;
}

/**
 * Lightweight dependency-free validator for the operational invariants in
 * STORE_CATALOG_SCHEMA.json. Publication rules are deliberately stricter than
 * draft rules. This module is an implementation foundation; it does not yet
 * replace the existing PlayerAccount catalog or expose admin HTTP endpoints.
 */
export function validateCatalogItem(item, { playProducts = [], requirePublished = false } = {}) {
  const errors = [];
  const add = (condition, message) => { if (!condition) errors.push(message); };
  const isRecord = (value) => value !== null && typeof value === "object" && !Array.isArray(value);
  const text = (value, max = Infinity) => typeof value === "string" && value.trim().length > 0 && value.length <= max;

  if (!isRecord(item)) return { valid: false, errors: ["item must be an object"] };
  add(text(item.id, 64) && ITEM_ID.test(item.id), "id must be a stable 3–64 character lowercase identifier");
  add(VALID_CATEGORIES.has(item.category), "category is unsupported");
  add(text(item.name, 80), "name is required and must be at most 80 characters");
  add(typeof item.description === "string" && item.description.length <= 500, "description must be at most 500 characters");
  add(VALID_STATUSES.has(item.status), "status is unsupported");
  add(typeof item.contentReady === "boolean", "contentReady must be boolean");
  add(Number.isSafeInteger(item.catalogVersion) && item.catalogVersion >= 1, "catalogVersion must be a positive integer");

  if (!isRecord(item.asset)) {
    errors.push("asset must be an object");
  } else {
    add(text(item.asset.thumbnail), "asset.thumbnail is required");
    add(text(item.asset.manifest), "asset.manifest is required");
    add(Number.isSafeInteger(item.asset.version) && item.asset.version >= 1, "asset.version must be a positive integer");
    add(typeof item.asset.sha256 === "string" && SHA256.test(item.asset.sha256), "asset.sha256 must be a lowercase SHA-256 digest");
    add(VALID_ASSET_FORMATS.has(item.asset.format), "asset.format is unsupported");
  }

  const unlock = item.unlock;
  if (!isRecord(unlock) || !Array.isArray(unlock.channels) || unlock.channels.length < 1) {
    errors.push("unlock.channels must be a non-empty array");
  } else {
    add(unlock.channels.every((channel) => VALID_CHANNELS.has(channel)), "unlock.channels contains an unsupported channel");
    add(new Set(unlock.channels).size === unlock.channels.length, "unlock.channels must not contain duplicates");
    if (unlock.channels.includes("REWARDED_ADS")) {
      add(isRecord(unlock.rewardedAds) && unlock.rewardedAds.enabled === true &&
        Number.isSafeInteger(unlock.rewardedAds.requiredCompletedAds) &&
        unlock.rewardedAds.requiredCompletedAds >= 1 && unlock.rewardedAds.requiredCompletedAds <= 100,
      "REWARDED_ADS requires an enabled, bounded completed-ad threshold");
    }
    if (unlock.channels.includes("GEMS")) {
      add(isRecord(unlock.gems) && unlock.gems.enabled === true &&
        Number.isSafeInteger(unlock.gems.price) && unlock.gems.price >= 1 && unlock.gems.price <= 2_000_000_000,
      "GEMS requires an enabled positive integer price");
    }
    if (unlock.channels.includes("PLAY_PRODUCT")) {
      const playProduct = unlock.playProduct;
      add(isRecord(playProduct) && playProduct.enabled === true &&
        typeof playProduct.productId === "string" && PRODUCT_ID.test(playProduct.productId) &&
        playProduct.productKind === "NON_CONSUMABLE",
      "PLAY_PRODUCT requires an enabled, valid non-consumable product mapping");
      if (isRecord(playProduct) && typeof playProduct.productId === "string") {
        add(playProducts.includes(playProduct.productId), "PLAY_PRODUCT references a SKU not present in the verified server allowlist");
      }
    }
    if (unlock.channels.includes("LEVEL")) {
      add(isRecord(unlock.level) && unlock.level.enabled === true &&
        Number.isSafeInteger(unlock.level.minimumLevel) && unlock.level.minimumLevel >= 1,
      "LEVEL requires an enabled positive minimum level");
    }
    if (unlock.channels.includes("EVENT")) {
      add(isRecord(unlock.event) && unlock.event.enabled === true &&
        text(unlock.event.eventKey, 100) && unlock.event.eventKey.length >= 3,
      "EVENT requires an enabled stable event key");
    }
    add(unlock.channels.includes("FREE") || unlock.channels.includes("REWARDED_ADS") ||
      unlock.channels.includes("GEMS") || unlock.channels.includes("PLAY_PRODUCT") ||
      unlock.channels.includes("LEVEL") || unlock.channels.includes("EVENT"),
    "at least one supported unlock channel is required");
  }

  const availability = item.availability;
  if (!isRecord(availability)) {
    errors.push("availability must be an object");
  } else {
    add(typeof availability.worldwide === "boolean", "availability.worldwide must be boolean");
    for (const field of ["allowedCountries", "blockedCountries"]) {
      add(Array.isArray(availability[field]) && availability[field].every((c) => typeof c === "string" && COUNTRY_CODE.test(c)) &&
        new Set(availability[field]).size === availability[field].length,
      `availability.${field} must contain unique ISO-style uppercase two-letter country codes`);
    }
    const startsAt = availability.startsAt == null ? null : parseAvailabilityDateTime(availability.startsAt);
    const endsAt = availability.endsAt == null ? null : parseAvailabilityDateTime(availability.endsAt);
    add(availability.startsAt == null || Number.isFinite(startsAt),
      "availability.startsAt must be a valid ISO 8601 date-time or null");
    add(availability.endsAt == null || Number.isFinite(endsAt),
      "availability.endsAt must be a valid ISO 8601 date-time or null");
    if (availability.startsAt != null && availability.endsAt != null &&
        Number.isFinite(startsAt) && Number.isFinite(endsAt)) {
      add(startsAt < endsAt, "availability end must be later than start");
    }
    add(!(availability.worldwide && Array.isArray(availability.allowedCountries) && availability.allowedCountries.length > 0),
      "worldwide availability cannot be combined with an allowlist; use blockedCountries instead");
  }

  if (item.status === "PUBLISHED" || requirePublished) {
    add(item.contentReady === true, "published items require contentReady=true");
    add(isRecord(item.asset) && VALID_ASSET_FORMATS.has(item.asset.format), "published items require a supported asset format");
    add(isRecord(availability), "published items require valid availability");
    add(isRecord(unlock) && Array.isArray(unlock.channels) && unlock.channels.length > 0, "published items require at least one unlock channel");
  }

  return { valid: errors.length === 0, errors, schemaId: SCHEMA_ID };
}

export function validateCatalog(items, options = {}) {
  if (!Array.isArray(items)) return { valid: false, errors: ["catalog must be an array"] };
  const errors = [];
  const seen = new Set();
  for (const [index, item] of items.entries()) {
    const result = validateCatalogItem(item, options);
    for (const error of result.errors) errors.push(`items[${index}]: ${error}`);
    if (item && typeof item.id === "string") {
      if (seen.has(item.id)) errors.push(`items[${index}]: duplicate stable item ID ${item.id}`);
      seen.add(item.id);
    }
  }
  return { valid: errors.length === 0, errors, count: items.length };
}

export function assertCatalogCanPublish(item, options = {}) {
  const result = validateCatalogItem(item, { ...options, requirePublished: true });
  if (!result.valid) {
    const error = new Error("Catalog item cannot be published: " + result.errors.join("; "));
    error.code = "CATALOG_ITEM_INVALID";
    error.details = result.errors;
    throw error;
  }
  return { ...item, status: "PUBLISHED" };
}
