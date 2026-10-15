import test from "node:test";
import assert from "node:assert/strict";
import accountWorker from "../src/index-account.js";

const AUDIENCE = "https://api.example.test/api/google-play/rtdn";
const PUBSUB_EMAIL = "pubsub-push@ludo-project.iam.gserviceaccount.com";
const TOKEN = "test-oidc-token-" + "A".repeat(56);
const PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----\n" + "A".repeat(128) + "\n-----END PRIVATE KEY-----";

function makeEnv(overrides = {}) {
  return {
    GOOGLE_PLAY_BILLING_ENABLED: "true",
    GOOGLE_PLAY_SERVICE_ACCOUNT_EMAIL: "play-billing@ludo-project.iam.gserviceaccount.com",
    GOOGLE_PLAY_SERVICE_ACCOUNT_PRIVATE_KEY: PRIVATE_KEY,
    GOOGLE_PLAY_SUBSCRIPTIONS_ENABLED: "true",
    GOOGLE_PLAY_RTDN_AUDIENCE: AUDIENCE,
    GOOGLE_PLAY_RTDN_EMAIL: PUBSUB_EMAIL,
    LUDOPROOF_STORE_PURCHASES: {
      idFromName(name) { return name; },
      get() {
        return {
          async fetch() {
            throw new Error("The test should not reach the subscription ledger.");
          },
        };
      },
    },
    ...overrides,
  };
}

function rtdnRequest({ token = TOKEN, body = {}, includeAuthorization = true } = {}) {
  const headers = { "content-type": "application/json" };
  if (includeAuthorization) headers.authorization = "Bearer " + token;
  return new Request(AUDIENCE, {
    method: "POST",
    headers,
    body: JSON.stringify(body),
  });
}

function verifiedTokenInfo(overrides = {}) {
  return Response.json({
    aud: AUDIENCE,
    email: PUBSUB_EMAIL,
    email_verified: "true",
    exp: String(Math.floor(Date.now() / 1000) + 300),
    iss: "https://accounts.google.com",
    ...overrides,
  });
}

async function withMockFetch(mock, run) {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = mock;
  try {
    return await run();
  } finally {
    globalThis.fetch = originalFetch;
  }
}

test("Google Play RTDN fails closed when subscription processing is disabled", async () => {
  let fetched = false;
  const env = makeEnv({ GOOGLE_PLAY_SUBSCRIPTIONS_ENABLED: "false" });
  const response = await withMockFetch(async () => {
    fetched = true;
    throw new Error("Google must not be called for a disabled subscription feature.");
  }, () => accountWorker.fetch(rtdnRequest(), env));

  assert.equal(response.status, 503);
  assert.equal((await response.json()).error, "PLAY_SUBSCRIPTIONS_NOT_ENABLED");
  assert.equal(fetched, false);
});

test("Google Play RTDN rejects missing bearer authentication before calling Google", async () => {
  let fetched = false;
  const response = await withMockFetch(async () => {
    fetched = true;
    throw new Error("Token info must not be fetched without an Authorization bearer token.");
  }, () => accountWorker.fetch(rtdnRequest({ includeAuthorization: false }), makeEnv()));

  assert.equal(response.status, 401);
  assert.equal((await response.json()).error, "RTDN_AUTH_REQUIRED");
  assert.equal(fetched, false);
});

test("Google Play RTDN rejects an OIDC token with the wrong audience", async () => {
  const response = await withMockFetch(
    async (url) => {
      assert.match(String(url), /^https:\/\/oauth2\.googleapis\.com\/tokeninfo\?id_token=/);
      return verifiedTokenInfo({ aud: "https://wrong.example.test/callback" });
    },
    () => accountWorker.fetch(rtdnRequest(), makeEnv()),
  );

  assert.equal(response.status, 401);
  assert.equal((await response.json()).error, "RTDN_AUTH_INVALID");
});

test("Google Play RTDN rejects an OIDC token from the wrong service account", async () => {
  const response = await withMockFetch(
    async () => verifiedTokenInfo({ email: "untrusted@other-project.iam.gserviceaccount.com" }),
    () => accountWorker.fetch(rtdnRequest(), makeEnv()),
  );

  assert.equal(response.status, 401);
  assert.equal((await response.json()).error, "RTDN_AUTH_INVALID");
});

test("Google Play RTDN ignores valid authenticated notifications for another package", async () => {
  const payload = {
    packageName: "com.example.othergame",
    subscriptionNotification: {
      purchaseToken: "unrelated-subscription-token",
    },
  };
  const body = {
    message: {
      messageId: "test-rtdn-message-1",
      data: Buffer.from(JSON.stringify(payload), "utf8").toString("base64"),
    },
  };
  const response = await withMockFetch(
    async () => verifiedTokenInfo(),
    () => accountWorker.fetch(rtdnRequest({ body }), makeEnv()),
  );

  assert.equal(response.status, 200);
  assert.deepEqual(await response.json(), {
    ok: true,
    ignored: true,
    reason: "package_mismatch",
  });
});
