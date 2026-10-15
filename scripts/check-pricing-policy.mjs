import assert from "node:assert/strict";
import { readFileSync } from "node:fs";

function read(path) {
  return readFileSync(new URL(path, import.meta.url), "utf8");
}

function amount(value) {
  return Number(value.replaceAll("_", ""));
}

const android = read("../android/app/src/main/java/com/rekixo/ludopaws/feature/store/domain/StoreCatalog.kt");
const server = read("../server/src/google-play-billing.js");
const pricing = read("../docs/pricing.html");
const terms = read("../docs/terms-of-service.html");
const privacy = read("../docs/privacy-policy.html");
const inAppPolicy = read("../android/app/src/main/java/com/rekixo/ludopaws/feature/settings/presentation/PrivacyPolicyDialogUi.kt");
const pagesWorkflow = read("../.github/workflows/account-deletion-pages.yml");

const constants = Object.fromEntries(
  [...android.matchAll(/const\s+val\s+([A-Z0-9_]+)\s*=\s*\n?\s*"([^"]+)"/g)]
    .map((match) => [match[1], match[2]]),
);
const androidProducts = {};
for (const match of android.matchAll(/StoreProduct\(([\s\S]*?)\n {12}\),/g)) {
  const block = match[1];
  const constant = block.match(/productId\s*=\s*([A-Z0-9_]+)/)?.[1];
  const gemText = block.match(/gemAmount\s*=\s*([0-9_]+)/)?.[1];
  const kind = block.match(/kind\s*=\s*StoreProductKind\.([A-Z_]+)/)?.[1];
  assert.ok(constant && constants[constant], "every Android store item needs a resolvable product ID");
  assert.ok(gemText && kind, "every Android store item needs a reward amount and purchase type");
  androidProducts[constants[constant]] = {
    gemAmount: amount(gemText),
    removeAds: kind === "NON_CONSUMABLE_REMOVE_ADS",
    consumable: kind === "CONSUMABLE_GEMS",
  };
}

const serverProducts = Object.fromEntries(
  [...server.matchAll(/^\s{2}([a-z][a-z0-9_]+): Object\.freeze\(\{\s*gemAmount:\s*([0-9_]+),\s*removeAds:\s*(true|false),\s*consumable:\s*(true|false),\s*\}\),/gm)]
    .map((match) => [match[1], {
      gemAmount: amount(match[2]),
      removeAds: match[3] === "true",
      consumable: match[4] === "true",
    }]),
);

assert.ok(Object.keys(androidProducts).length > 0, "Android product catalog must not be empty");
assert.deepEqual(
  androidProducts,
  serverProducts,
  "Android product IDs, gem amounts, Remove Ads entitlement and consumable types must match server verification",
);

for (const [productId, product] of Object.entries(serverProducts)) {
  assert.ok(pricing.includes("<code>" + productId + "</code>"), "pricing page missing " + productId);
  assert.ok(pricing.includes(product.gemAmount.toLocaleString("en-US") + " Gems"), "pricing page reward amount mismatch for " + productId);
}
assert.ok(pricing.includes("localized price and currency shown in the app"), "pricing page must direct users to the localized Google Play price");
assert.ok(pricing.includes("does not take payments or offer a separate web checkout"), "pricing page must not imply web checkout");
assert.ok(pricing.includes("does not offer subscriptions or automatic renewals"), "pricing page must match the one-time purchase catalog");
assert.ok(pricing.includes("Google Play order history"), "pricing page must explain the order/refund support boundary");
assert.ok(pricing.includes("PRICING_RELEASE_REVIEW_REQUIRED"), "pricing page must retain its manual release-review gate until live Play Console validation");
assert.ok(terms.includes("TERMS_RELEASE_REVIEW_REQUIRED"), "Terms of Service must retain its publisher/legal review gate");
assert.doesNotMatch(pricing, /(?:₹|Rs\.?\s*|US\$|USD\s*)\s*\d/i, "public pricing page must not hard-code a regional currency price");

assert.ok(privacy.includes('href="./pricing.html">Pricing and in-app purchase policy</a>'), "online privacy policy must link to pricing policy");
assert.ok(privacy.includes("configured reward is five Gems"), "online privacy policy must disclose the verified rewarded-ad grant");
assert.ok(inAppPolicy.includes("configured reward is five Gems"), "in-app privacy copy must disclose the same verified rewarded-ad grant");
assert.ok(inAppPolicy.includes("https://ludopaws.rekixo.com/pricing.html"), "in-app policy must link to pricing policy");
assert.ok(pagesWorkflow.includes("cp docs/pricing.html site/pricing.html"), "Pages workflow must publish the pricing policy");
assert.ok(pricing.includes('href="./terms-of-service.html"'), "pricing page must link to Terms of Service");
assert.ok(terms.includes('href="./pricing.html"'), "Terms of Service must link to pricing policy");
assert.ok(terms.includes('href="./privacy-policy.html"'), "Terms of Service must link to privacy policy");
assert.ok(terms.includes('href="./account-deletion.html"'), "Terms of Service must link to account deletion");
assert.ok(pagesWorkflow.includes("cp docs/terms-of-service.html site/terms-of-service.html"), "Pages workflow must publish Terms of Service");
assert.ok(pagesWorkflow.includes("TERMS_RELEASE_REVIEW_REQUIRED"), "Pages workflow must block on the Terms of Service review marker");
assert.ok(pagesWorkflow.includes("fetch_public_page /terms-of-service.html"), "Pages workflow must smoke-test the published Terms of Service route");
assert.ok(pagesWorkflow.includes("PRICING_RELEASE_REVIEW_REQUIRED"), "Pages workflow must block on the pricing review marker");

console.log("Pricing and in-app purchase policy contract passed: Android, server and public disclosures match.");
