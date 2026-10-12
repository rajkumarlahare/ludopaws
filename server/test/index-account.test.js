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
  assert.equal((await response.json()).error, "AUTH_REQUIRED");
});


test("account deletion cannot be authorized by a caller-supplied account ID", async () => {
  const env = {
    LUDOPROOF_API_GATE: makeRateGate(),
    LUDOPROOF_PLAYER_ACCOUNTS: makePlayerNamespace(),
  };
  const response = await accountWorker.fetch(
    new Request("https://api.example.test/api/account/delete", {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({
        accountId: ACCOUNT_A,
        confirmation: "DELETE_MY_ACCOUNT",
      }),
    }),
    env,
  );
  assert.equal(response.status, 401);
  assert.equal((await response.json()).error, "AUTH_REQUIRED");
});

test("account deletion web CORS allows only the configured static-site origin and expected headers", async () => {
  const allowed = await accountWorker.fetch(
    new Request("https://api.example.test/api/account/delete", {
      method: "OPTIONS",
      headers: {
        origin: "https://rajkumarlahare.github.io",
        "access-control-request-method": "POST",
        "access-control-request-headers": "authorization,content-type",
      },
    }),
    { LUDOPROOF_ACCOUNT_WEB_ORIGIN: "https://rajkumarlahare.github.io" },
  );
  assert.equal(allowed.status, 204);
  assert.equal(allowed.headers.get("access-control-allow-origin"), "https://rajkumarlahare.github.io");
  assert.equal(allowed.headers.get("access-control-allow-methods"), "POST, OPTIONS");

  const denied = await accountWorker.fetch(
    new Request("https://api.example.test/api/account/delete", {
      method: "OPTIONS",
      headers: {
        origin: "https://attacker.example",
        "access-control-request-method": "POST",
        "access-control-request-headers": "authorization,content-type",
      },
    }),
    { LUDOPROOF_ACCOUNT_WEB_ORIGIN: "https://rajkumarlahare.github.io" },
  );
  assert.equal(denied.status, 403);
  assert.equal(denied.headers.get("access-control-allow-origin"), null);

  const unexpectedHeader = await accountWorker.fetch(
    new Request("https://api.example.test/api/account/delete", {
      method: "OPTIONS",
      headers: {
        origin: "https://rajkumarlahare.github.io",
        "access-control-request-method": "POST",
        "access-control-request-headers": "authorization,x-not-allowed",
      },
    }),
    { LUDOPROOF_ACCOUNT_WEB_ORIGIN: "https://rajkumarlahare.github.io" },
  );
  assert.equal(unexpectedHeader.status, 403);

  const unconfigured = await accountWorker.fetch(
    new Request("https://api.example.test/api/account/delete", {
      method: "OPTIONS",
      headers: {
        origin: "https://rajkumarlahare.github.io",
        "access-control-request-method": "POST",
        "access-control-request-headers": "authorization,content-type",
      },
    }),
    {},
  );
  assert.equal(unconfigured.status, 403);
  assert.equal(unconfigured.headers.get("access-control-allow-origin"), null);
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


function makeDeletionReplayEnv(deleted) {
  const calls = [];
  const player = {
    calls,
    idFromName(name) { return name; },
    get(accountId) {
      return {
        async fetch(request) {
          const url = new URL(request.url);
          const body = await request.json();
          calls.push({ accountId, path: url.pathname, body });
          if (url.pathname === "/session/check") {
            return Response.json({ error: "ACCOUNT_SESSION_INVALID" }, { status: 401 });
          }
          if (url.pathname === "/account/delete-preview") {
            return Response.json({ ok: true, deleted, purchaseLedgerIds: [] });
          }
          return Response.json({ error: "NOT_FOUND" }, { status: 404 });
        },
      };
    },
  };
  return {
    player,
    env: {
      LUDOPROOF_SESSION_HMAC_KEY: SECRET,
      LUDOPROOF_API_GATE: makeRateGate(),
      LUDOPROOF_PLAYER_ACCOUNTS: player,
    },
  };
}

test("expired signed access token can only confirm an already-deleted account", async () => {
  const { env, player } = makeDeletionReplayEnv(true);
  const expiredToken = await issueAccessToken(env, ACCOUNT_A, SESSION_A, Date.now() - 20 * 60 * 1000);
  const response = await accountWorker.fetch(
    new Request("https://api.example.test/api/account/delete", {
      method: "POST",
      headers: { authorization: "Bearer " + expiredToken, "content-type": "application/json" },
      body: JSON.stringify({}),
    }),
    env,
  );

  assert.equal(response.status, 200);
  assert.deepEqual(await response.json(), { ok: true, deleted: true, replay: true });
  assert.equal(player.calls.length, 1);
  assert.equal(player.calls[0].path, "/account/delete-preview");
  assert.equal(player.calls[0].accountId, ACCOUNT_A);
  assert.equal(player.calls[0].body.sessionId, SESSION_A);
});

test("expired signed access token cannot authorize a deletion when no deletion tombstone exists", async () => {
  const { env, player } = makeDeletionReplayEnv(false);
  const expiredToken = await issueAccessToken(env, ACCOUNT_A, SESSION_A, Date.now() - 20 * 60 * 1000);
  const response = await accountWorker.fetch(
    new Request("https://api.example.test/api/account/delete", {
      method: "POST",
      headers: { authorization: "Bearer " + expiredToken, "content-type": "application/json" },
      body: JSON.stringify({ confirmation: "DELETE_MY_ACCOUNT" }),
    }),
    env,
  );

  assert.equal(response.status, 401);
  assert.equal((await response.json()).error, "ACCOUNT_SESSION_INVALID");
  assert.equal(player.calls.length, 1);
  assert.equal(player.calls[0].path, "/account/delete-preview");
});


test("expired token replay still rejects a forged signature before checking the tombstone", async () => {
  const { env, player } = makeDeletionReplayEnv(true);
  const token = await issueAccessToken(env, ACCOUNT_A, SESSION_A, Date.now() - 20 * 60 * 1000);
  const parts = token.split(".");
  parts[2] = (parts[2][0] === "A" ? "B" : "A") + parts[2].slice(1);
  const tamperedToken = parts.join(".");

  const response = await accountWorker.fetch(
    new Request("https://api.example.test/api/account/delete", {
      method: "POST",
      headers: { authorization: "Bearer " + tamperedToken, "content-type": "application/json" },
      body: JSON.stringify({}),
    }),
    env,
  );

  assert.equal(response.status, 401);
  assert.equal((await response.json()).error, "ACCOUNT_SESSION_INVALID");
  assert.equal(player.calls.length, 0);
});
