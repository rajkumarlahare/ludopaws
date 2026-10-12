import test from "node:test";
import assert from "node:assert/strict";
import accountWorker from "../src/index-account.js";
import { issueAccessToken } from "../src/account-security.js";

const ACCOUNT_A = "0a2c3a83-e0fb-4abc-8ac4-581a6d994fe7";
const ACCOUNT_B = "1b3d4b94-e1f0-4bc9-9cd5-592b7e0a1f21";
const SESSION_A = "6382a0ee-f216-46f8-b10d-51411e1f38f2";
const SECRET = "test-session-hmac-key-that-is-long-enough-123456";

function makeRateGate(allowed = true) {
  return {
    idFromName(name) { return name; },
    get(name) {
      return {
        async fetch(request) {
          assert.equal(new URL(request.url).pathname, "/check");
          return allowed
            ? Response.json({ allowed: true, remaining: 119 })
            : Response.json(
                { allowed: false, remaining: 0 },
                { status: 429, headers: { "retry-after": "60" } },
              );
        },
      };
    },
  };
}

function makePlayerNamespace() {
  const calls = [];
  return {
    calls,
    idFromName(name) { return name; },
    get(accountId) {
      return {
        async fetch(request) {
          const url = new URL(request.url);
          const body = await request.json();
          calls.push({ accountId, path: url.pathname, body });
          if (url.pathname === "/session/check") {
            return Response.json({
              ok: true,
              accountId,
              profile: { accountId, displayName: "Ludo Player" },
            });
          }
          if (url.pathname === "/wallet/get") {
            return Response.json({
              ok: true,
              wallet: {
                accountId,
                balance: 42,
                adsRemoved: false,
                ownedCosmetics: ["dice_classic"],
                selectedCosmetics: { DICE: "dice_classic" },
                rewardedAdProgress: {},
              },
            });
          }
          return Response.json({ error: "NOT_FOUND", message: "unknown test route" }, { status: 404 });
        },
      };
    },
  };
}

test("account wallet API derives the account from the verified bearer token, not URL input", async () => {
  const player = makePlayerNamespace();
  const env = {
    LUDOPROOF_SESSION_HMAC_KEY: SECRET,
    LUDOPROOF_API_GATE: makeRateGate(),
    LUDOPROOF_PLAYER_ACCOUNTS: player,
  };
  const now = Date.now();
  const token = await issueAccessToken(env, ACCOUNT_A, SESSION_A, now);
  const response = await accountWorker.fetch(
    new Request(
      "https://api.example.test/api/wallet?accountId=" + ACCOUNT_B,
      { method: "GET", headers: { authorization: "Bearer " + token } },
    ),
    env,
  );

  assert.equal(response.status, 200);
  const body = await response.json();
  assert.equal(body.wallet.accountId, ACCOUNT_A);
  assert.equal(body.wallet.balance, 42);
  assert.ok(player.calls.length >= 2);
  assert.ok(player.calls.every((call) => call.accountId === ACCOUNT_A));
  assert.ok(player.calls.every((call) => call.body.accountId === ACCOUNT_A));
  assert.ok(player.calls.some((call) => call.path === "/session/check"));
  assert.ok(player.calls.some((call) => call.path === "/wallet/get"));
});

test("account wallet API rejects requests without a verified bearer token", async () => {
  const env = {
    LUDOPROOF_API_GATE: makeRateGate(),
    LUDOPROOF_PLAYER_ACCOUNTS: makePlayerNamespace(),
  };
  const response = await accountWorker.fetch(
    new Request("https://api.example.test/api/wallet"),
    env,
  );
  assert.equal(response.status, 401);
  assert.equal((await response.json()).error, "ACCOUNT_SESSION_REQUIRED");
});


test("AdMob SSV rate limiting runs before signature/key verification", async () => {
  const env = {
    LUDOPROOF_API_GATE: makeRateGate(false),
    LUDOPROOF_ADMOB_SSV_ENABLED: "true",
    LUDOPROOF_ADMOB_REWARDED_AD_UNIT_ID: "ca-app-pub-123/456",
  };
  const response = await accountWorker.fetch(
    new Request("https://api.example.test/api/admob/ssv?invalid=signature"),
    env,
  );
  assert.equal(response.status, 429);
  assert.equal((await response.json()).error, "RATE_LIMITED");
  assert.equal(response.headers.get("retry-after"), "60");
});
