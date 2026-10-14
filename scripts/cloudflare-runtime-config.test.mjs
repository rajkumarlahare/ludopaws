import test from "node:test";
import assert from "node:assert/strict";
import { ACCOUNT_WEB_ORIGIN, GOOGLE_PLAY_RTDN_AUDIENCE, buildCloudflareRuntimeVars } from "./cloudflare-runtime-config.mjs";

const valid = {
  GOOGLE_SIGN_IN_WEB_CLIENT_ID: "1234567890-exampleclient.apps.googleusercontent.com",
  LUDOPROOF_ACCOUNT_WEB_ORIGIN: ACCOUNT_WEB_ORIGIN,
};
const adUnit = "ca-app-pub-1234567890123456/1234567890";

test("runtime vars require a valid Google Web OAuth client and the account-site origin", () => {
  const vars = buildCloudflareRuntimeVars({ ENTRONEX_TENANT_ID: "cloudflare_v4_eval" }, valid);
  assert.equal(vars.GOOGLE_SIGN_IN_WEB_CLIENT_ID, valid.GOOGLE_SIGN_IN_WEB_CLIENT_ID);
  assert.equal(vars.LUDOPROOF_ACCOUNT_WEB_ORIGIN, ACCOUNT_WEB_ORIGIN);
  assert.equal(vars.ENTRONEX_TENANT_ID, "cloudflare_v4_eval");
});

test("runtime store flags fail closed when GitHub variables are unset", () => {
  const vars = buildCloudflareRuntimeVars({}, valid);
  assert.equal(vars.GOOGLE_PLAY_BILLING_ENABLED, "false");
  assert.equal(vars.GOOGLE_PLAY_SUBSCRIPTIONS_ENABLED, "false");
  assert.equal("GOOGLE_PLAY_RTDN_AUDIENCE" in vars, false);
  assert.equal("GOOGLE_PLAY_RTDN_EMAIL" in vars, false);
  assert.equal(vars.LUDOPROOF_ADMOB_SSV_ENABLED, "false");
  assert.equal(vars.LUDOPROOF_ADMOB_REWARD_ITEM, "gems");
  assert.equal("GOOGLE_PLAY_SERVICE_ACCOUNT_EMAIL" in vars, false);
  assert.equal("LUDOPROOF_ADMOB_REWARDED_AD_UNIT_ID" in vars, false);
});

test("invalid OAuth client ID is rejected", () => {
  assert.throws(
    () => buildCloudflareRuntimeVars({}, { ...valid, GOOGLE_SIGN_IN_WEB_CLIENT_ID: "not-a-web-client-id" }),
    /valid Google Web OAuth client ID/,
  );
});

test("account-site origin mismatch is rejected", () => {
  assert.throws(
    () => buildCloudflareRuntimeVars({}, { ...valid, LUDOPROOF_ACCOUNT_WEB_ORIGIN: "https://example.com" }),
    /must be exactly/,
  );
});

test("Paws Plus subscriptions default off and require matching Android/server flags", () => {
  const vars = buildCloudflareRuntimeVars({}, valid);
  assert.equal(vars.GOOGLE_PLAY_SUBSCRIPTIONS_ENABLED, "false");
  assert.throws(
    () => buildCloudflareRuntimeVars({}, {
      ...valid,
      GOOGLE_PLAY_SUBSCRIPTIONS_ENABLED: "true",
    }),
    /must match before deployment/,
  );
});

test("Paws Plus cannot be enabled without Billing and authenticated RTDN configuration", () => {
  const base = {
    ...valid,
    GOOGLE_PLAY_BILLING_ENABLED: "true",
    LUDOPROOF_BILLING_ENABLED: "true",
    GOOGLE_PLAY_SUBSCRIPTIONS_ENABLED: "true",
    LUDOPROOF_SUBSCRIPTIONS_ENABLED: "true",
    GOOGLE_PLAY_SERVICE_ACCOUNT_EMAIL: "ludoproof-play@ludoproof-prod.iam.gserviceaccount.com",
  };
  assert.throws(
    () => buildCloudflareRuntimeVars({}, base),
    /RTDN_AUDIENCE must exactly match/,
  );
  assert.throws(
    () => buildCloudflareRuntimeVars({}, {
      ...base,
      GOOGLE_PLAY_RTDN_AUDIENCE: GOOGLE_PLAY_RTDN_AUDIENCE,
      GOOGLE_PLAY_RTDN_EMAIL: "not-an-authentication-identity",
    }),
    /GOOGLE_PLAY_RTDN_EMAIL must be/,
  );
  const vars = buildCloudflareRuntimeVars({}, {
    ...base,
    GOOGLE_PLAY_RTDN_AUDIENCE,
    GOOGLE_PLAY_RTDN_EMAIL: "ludo-rtdn@ludoproof-prod.iam.gserviceaccount.com",
  });
  assert.equal(vars.GOOGLE_PLAY_SUBSCRIPTIONS_ENABLED, "true");
  assert.equal(vars.GOOGLE_PLAY_RTDN_AUDIENCE, GOOGLE_PLAY_RTDN_AUDIENCE);
  assert.equal(vars.GOOGLE_PLAY_RTDN_EMAIL, "ludo-rtdn@ludoproof-prod.iam.gserviceaccount.com");
});

test("Billing cannot be enabled without the Google Play service-account email", () => {
  assert.throws(
    () => buildCloudflareRuntimeVars({}, { ...valid, GOOGLE_PLAY_BILLING_ENABLED: "true", LUDOPROOF_BILLING_ENABLED: "true" }),
    /cannot be enabled until GOOGLE_PLAY_SERVICE_ACCOUNT_EMAIL/,
  );
});

test("Billing accepts a correctly shaped service-account email", () => {
  const vars = buildCloudflareRuntimeVars({}, {
    ...valid,
    GOOGLE_PLAY_BILLING_ENABLED: "true",
    LUDOPROOF_BILLING_ENABLED: "true",
    GOOGLE_PLAY_SERVICE_ACCOUNT_EMAIL: "ludoproof-play@ludoproof-prod.iam.gserviceaccount.com",
  });
  assert.equal(vars.GOOGLE_PLAY_BILLING_ENABLED, "true");
  assert.equal(vars.GOOGLE_PLAY_SERVICE_ACCOUNT_EMAIL, "ludoproof-play@ludoproof-prod.iam.gserviceaccount.com");
});

test("AdMob SSV cannot be enabled without a real rewarded-unit ID", () => {
  assert.throws(
    () => buildCloudflareRuntimeVars({}, { ...valid, LUDOPROOF_ADMOB_SSV_ENABLED: "true", LUDOPROOF_ADS_ENABLED: "true" }),
    /cannot be enabled until LUDOPROOF_ADMOB_REWARDED_AD_UNIT_ID/,
  );
});

test("AdMob SSV only permits the configured gems reward and validates the unit ID", () => {
  const vars = buildCloudflareRuntimeVars({}, {
    ...valid,
    LUDOPROOF_ADMOB_SSV_ENABLED: "true",
    LUDOPROOF_ADS_ENABLED: "true",
    LUDOPROOF_ADMOB_REWARDED_AD_UNIT_ID: adUnit,
    LUDOPROOF_ADMOB_REWARD_ITEM: "gems",
    ADMOB_APP_ID: "ca-app-pub-1234567890123456~1234567890",
    ADMOB_REWARDED_AD_UNIT_ID: adUnit,
  });
  assert.equal(vars.LUDOPROOF_ADMOB_SSV_ENABLED, "true");
  assert.equal(vars.LUDOPROOF_ADMOB_REWARDED_AD_UNIT_ID, adUnit);
  assert.throws(
    () => buildCloudflareRuntimeVars({}, { ...valid, LUDOPROOF_ADMOB_REWARD_ITEM: "coins" }),
    /must be "gems"/,
  );
  assert.throws(
    () => buildCloudflareRuntimeVars({}, { ...valid, LUDOPROOF_ADMOB_REWARDED_AD_UNIT_ID: "ca-app-pub-123/456" }),
    /must be a real AdMob rewarded-unit ID/,
  );
});

test("Worker and Android store flags must match before deployment", () => {
  assert.throws(
    () => buildCloudflareRuntimeVars({}, { ...valid, GOOGLE_PLAY_BILLING_ENABLED: "true" }),
    /must match before deployment/,
  );
  assert.throws(
    () => buildCloudflareRuntimeVars({}, { ...valid, LUDOPROOF_ADMOB_SSV_ENABLED: "true" }),
    /must match before deployment/,
  );
});

test("feature flags reject ambiguous or non-canonical values instead of interpreting them as true", () => {
  assert.throws(
    () => buildCloudflareRuntimeVars({}, { ...valid, GOOGLE_PLAY_BILLING_ENABLED: "yes" }),
    /must be exactly "true" or "false"/,
  );
  assert.throws(
    () => buildCloudflareRuntimeVars({}, { ...valid, GOOGLE_PLAY_BILLING_ENABLED: "TRUE", LUDOPROOF_BILLING_ENABLED: "TRUE" }),
    /must be exactly "true" or "false"/,
  );
});
