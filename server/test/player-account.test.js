import test from "node:test";
import assert from "node:assert/strict";
import { PlayerAccount } from "../src/player-account.js";

class FakeAccountSql {
  constructor() {
    this.accountMeta = new Map();
    this.deletedAccounts = new Map();
    this.deletionClaims = new Map();
    this.wallet = new Map();
    this.sessions = new Map();
    this.ownedCosmetics = new Map();
    this.selectedCosmetics = new Map();
    this.adProgress = new Map();
    this.walletTransactions = new Map();
  }

  exec(statement, ...params) {
    const sql = String(statement).replace(/\s+/g, " ").trim();
    if (/^(CREATE TABLE|CREATE INDEX)/i.test(sql)) return { rowsWritten: 0 };

    if (sql.startsWith("SELECT account_id FROM account_deletions WHERE account_id = ?")) {
      return this.deletedAccounts.has(params[0]) ? [{ account_id: params[0] }] : [];
    }
    if (sql.startsWith("SELECT account_id FROM account_deletion_claims WHERE account_id = ?")) {
      return this.deletionClaims.has(params[0]) ? [{ account_id: params[0] }] : [];
    }
    if (sql.startsWith("INSERT OR IGNORE INTO account_deletion_claims")) {
      if (!this.deletionClaims.has(params[0])) this.deletionClaims.set(params[0], { claimed_at: params[1] });
      return { rowsWritten: 1 };
    }
    if (sql.startsWith("DELETE FROM account_deletion_claims WHERE account_id = ?")) {
      this.deletionClaims.delete(params[0]);
      return { rowsWritten: 1 };
    }
    if (sql.startsWith("SELECT source_ref_hash FROM wallet_transactions WHERE kind IN")) {
      return [...this.walletTransactions.values()].filter(row => ["PLAY_GEM_PURCHASE", "PLAY_REMOVE_ADS"].includes(row.kind) && row.source_ref_hash).map(row => ({ source_ref_hash: row.source_ref_hash }));
    }
    if (/^DELETE FROM account_sessions$/.test(sql)) { this.sessions.clear(); return { rowsWritten: 1 }; }
    if (/^DELETE FROM wallet_transactions$/.test(sql)) { this.walletTransactions.clear(); return { rowsWritten: 1 }; }
    if (/^DELETE FROM cosmetic_ad_progress$/.test(sql)) { this.adProgress.clear(); return { rowsWritten: 1 }; }
    if (/^DELETE FROM selected_cosmetics$/.test(sql)) { this.selectedCosmetics.clear(); return { rowsWritten: 1 }; }
    if (/^DELETE FROM owned_cosmetics$/.test(sql)) { this.ownedCosmetics.clear(); return { rowsWritten: 1 }; }
    if (/^DELETE FROM wallet$/.test(sql)) { this.wallet.clear(); return { rowsWritten: 1 }; }
    if (sql.startsWith("DELETE FROM account_meta WHERE account_id = ?")) { this.accountMeta.delete(params[0]); return { rowsWritten: 1 }; }
    if (sql.startsWith("INSERT INTO account_deletions")) { this.deletedAccounts.set(params[0], { deleted_at: params[1] }); return { rowsWritten: 1 }; }
    if (sql.startsWith("SELECT account_id FROM account_meta WHERE account_id = ?")) {
      return this.accountMeta.has(params[0]) ? [{ account_id: params[0] }] : [];
    }
    if (sql.startsWith("INSERT INTO account_meta")) {
      this.accountMeta.set(params[0], {
        account_id: params[0], display_name: params[1], created_at: params[2], updated_at: params[3],
      });
      return { rowsWritten: 1 };
    }
    if (sql.startsWith("SELECT account_id, display_name, created_at, updated_at FROM account_meta")) {
      const row = this.accountMeta.get(params[0]);
      return row ? [structuredClone(row)] : [];
    }
    if (sql.startsWith("UPDATE account_meta SET display_name = ?")) {
      const row = this.accountMeta.get(params[2]);
      if (!row) return { rowsWritten: 0 };
      row.display_name = params[0];
      row.updated_at = params[1];
      return { rowsWritten: 1 };
    }

    if (sql.startsWith("INSERT OR IGNORE INTO wallet")) {
      const accountId = params[0];
      if (this.wallet.has(accountId)) return { rowsWritten: 0 };
      this.wallet.set(accountId, {
        account_id: accountId, balance: 0, ads_removed: 0, updated_at: Number(params[1]),
      });
      return { rowsWritten: 1 };
    }
    if (sql.startsWith("SELECT balance, ads_removed, updated_at FROM wallet WHERE account_id = ?")) {
      const row = this.wallet.get(params[0]);
      return row ? [structuredClone(row)] : [];
    }
    if (sql.startsWith("SELECT balance FROM wallet WHERE account_id = ?")) {
      const row = this.wallet.get(params[0]);
      return row ? [{ balance: row.balance }] : [];
    }
    if (sql.startsWith("UPDATE wallet SET balance = ?, ads_removed = CASE")) {
      const row = this.wallet.get(params[3]);
      if (!row) return { rowsWritten: 0 };
      row.balance = params[0];
      if (params[1] === 1) row.ads_removed = 1;
      row.updated_at = params[2];
      return { rowsWritten: 1 };
    }
    if (sql.startsWith("UPDATE wallet SET balance = ?, updated_at = ? WHERE account_id = ?")) {
      const row = this.wallet.get(params[2]);
      if (!row) return { rowsWritten: 0 };
      row.balance = params[0];
      row.updated_at = params[1];
      return { rowsWritten: 1 };
    }

    if (sql.startsWith("INSERT INTO account_sessions")) {
      this.sessions.set(params[0], {
        session_id: params[0],
        refresh_token_hash: params[1],
        previous_refresh_hash: null,
        last_refresh_request_id: null,
        created_at: params[2],
        expires_at: params[3],
        revoked_at: null,
        last_seen_at: params[4],
      });
      return { rowsWritten: 1 };
    }
    if (sql.startsWith("SELECT refresh_token_hash, previous_refresh_hash, last_refresh_request_id, expires_at, revoked_at FROM account_sessions WHERE session_id = ?")) {
      const session = this.sessions.get(params[0]);
      return session ? [{
        refresh_token_hash: session.refresh_token_hash,
        previous_refresh_hash: session.previous_refresh_hash,
        last_refresh_request_id: session.last_refresh_request_id,
        expires_at: session.expires_at,
        revoked_at: session.revoked_at,
      }] : [];
    }
    if (sql.startsWith("UPDATE account_sessions SET refresh_token_hash = ?, previous_refresh_hash = ?, last_refresh_request_id = ?, expires_at = ?, last_seen_at = ? WHERE session_id = ? AND refresh_token_hash = ?")) {
      const session = this.sessions.get(params[5]);
      if (
        !session ||
        session.refresh_token_hash !== params[6] ||
        session.revoked_at != null ||
        session.expires_at <= params[7]
      ) return { rowsWritten: 0 };
      session.refresh_token_hash = params[0];
      session.previous_refresh_hash = params[1];
      session.last_refresh_request_id = params[2];
      session.expires_at = params[3];
      session.last_seen_at = params[4];
      return { rowsWritten: 1 };
    }
    if (sql.startsWith("SELECT session_id FROM account_sessions WHERE session_id = ? AND revoked_at IS NULL")) {
      const session = this.sessions.get(params[0]);
      return session && session.revoked_at == null && session.expires_at > params[1] && this.accountMeta.has(params[2])
        ? [{ session_id: session.session_id }]
        : [];
    }

    if (sql.startsWith("INSERT OR IGNORE INTO owned_cosmetics")) {
      const cosmeticId = params[0];
      if (this.ownedCosmetics.has(cosmeticId)) return { rowsWritten: 0 };
      this.ownedCosmetics.set(cosmeticId, {
        cosmetic_id: cosmeticId,
        category: params[1],
        source: "DEFAULT",
        acquired_at: params[2],
        source_ref_hash: null,
      });
      return { rowsWritten: 1 };
    }
    if (sql.startsWith("INSERT INTO owned_cosmetics")) {
      const source = sql.includes("'GEMS'") ? "GEMS" : "REWARDED_ADS";
      this.ownedCosmetics.set(params[0], {
        cosmetic_id: params[0], category: params[1], source, acquired_at: params[2], source_ref_hash: params[3] ?? null,
      });
      return { rowsWritten: 1 };
    }
    if (sql.startsWith("SELECT cosmetic_id FROM owned_cosmetics WHERE cosmetic_id = ?")) {
      return this.ownedCosmetics.has(params[0]) ? [{ cosmetic_id: params[0] }] : [];
    }
    if (sql.startsWith("SELECT cosmetic_id FROM owned_cosmetics ORDER BY cosmetic_id")) {
      return [...this.ownedCosmetics.keys()].sort().map((cosmetic_id) => ({ cosmetic_id }));
    }

    if (sql.startsWith("INSERT OR IGNORE INTO selected_cosmetics")) {
      const category = params[0];
      if (this.selectedCosmetics.has(category)) return { rowsWritten: 0 };
      this.selectedCosmetics.set(category, { category, cosmetic_id: params[1], updated_at: params[2] });
      return { rowsWritten: 1 };
    }
    if (sql.startsWith("INSERT INTO selected_cosmetics")) {
      const category = params[0];
      this.selectedCosmetics.set(category, { category, cosmetic_id: params[1], updated_at: params[2] });
      return { rowsWritten: 1 };
    }
    if (sql.startsWith("SELECT category, cosmetic_id FROM selected_cosmetics ORDER BY category")) {
      return [...this.selectedCosmetics.values()].sort((a, b) => a.category.localeCompare(b.category)).map((row) => ({
        category: row.category, cosmetic_id: row.cosmetic_id,
      }));
    }

    if (sql.startsWith("SELECT payload_hash FROM wallet_transactions WHERE idempotency_key = ?")) {
      const row = this.walletTransactions.get(params[0]);
      return row ? [{ payload_hash: row.payload_hash }] : [];
    }
    if (sql.startsWith("SELECT payload_hash, balance_after FROM wallet_transactions WHERE idempotency_key = ?")) {
      const row = this.walletTransactions.get(params[0]);
      return row ? [{ payload_hash: row.payload_hash, balance_after: row.balance_after }] : [];
    }
    if (sql.startsWith("INSERT INTO wallet_transactions")) {
      const isCosmeticInsert = sql.includes("'COSMETIC_PURCHASE'");
      const idempotencyKey = params[1];
      const payloadHash = params[2];
      const kind = isCosmeticInsert ? "COSMETIC_PURCHASE" : params[3];
      const delta = isCosmeticInsert ? params[3] : params[4];
      const balanceAfter = isCosmeticInsert ? params[4] : params[5];
      this.walletTransactions.set(idempotencyKey, {
        payload_hash: payloadHash, kind, delta, balance_after: balanceAfter,
        source_ref_hash: isCosmeticInsert ? params[5] ?? null : params[6] ?? null,
      });
      return { rowsWritten: 1 };
    }

    if (sql.startsWith("SELECT cosmetic_id, progress FROM cosmetic_ad_progress ORDER BY cosmetic_id")) {
      return [...this.adProgress.entries()].map(([cosmetic_id, progress]) => ({ cosmetic_id, progress }));
    }
    if (sql.startsWith("INSERT INTO cosmetic_ad_progress")) {
      this.adProgress.set(params[0], Number(params[1]));
      return { rowsWritten: 1 };
    }
    if (sql.startsWith("SELECT progress FROM cosmetic_ad_progress WHERE cosmetic_id = ?")) {
      return this.adProgress.has(params[0]) ? [{ progress: this.adProgress.get(params[0]) }] : [];
    }
    throw new Error("Unexpected PlayerAccount SQL in test: " + sql);
  }
}

function makeAccount() {
  const sql = new FakeAccountSql();
  const ctx = {
    storage: { sql, transactionSync: (callback) => callback() },
    blockConcurrencyWhile: (callback) => callback(),
  };
  const env = { LUDOPROOF_SESSION_HMAC_KEY: "test-session-hmac-key-that-is-long-enough-123456" };
  return { account: new PlayerAccount(ctx, env), sql };
}

async function post(account, path, body) {
  return account.fetch(new Request("https://player-account" + path, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify(body),
  }));
}

test("account deletion requires explicit confirmation, clears server-owned data, and prevents resurrection", async () => {
  const { account, sql } = makeAccount();
  const accountId = "0a2c3a83-e0fb-4abc-8ac4-581a6d994fe7";
  const created = await post(account, "/session/create", { accountId, displayName: "Delete Me" });
  assert.equal(created.status, 200);
  const session = await created.json();
  assert.equal(sql.wallet.get(accountId).balance, 0);

  const denied = await post(account, "/account/delete", {
    accountId, sessionId: session.sessionId, confirmation: "wrong",
  });
  assert.equal(denied.status, 400);
  assert.ok(sql.accountMeta.has(accountId));

  const deleted = await post(account, "/account/delete", {
    accountId, sessionId: session.sessionId, confirmation: "DELETE_MY_ACCOUNT",
  });
  assert.equal(deleted.status, 200);
  assert.equal((await deleted.json()).deleted, true);
  assert.equal(sql.accountMeta.has(accountId), false);
  assert.equal(sql.wallet.has(accountId), false);
  assert.equal(sql.sessions.size, 0);
  assert.ok(sql.deletedAccounts.has(accountId));

  const retry = await post(account, "/account/delete", {
    accountId, sessionId: session.sessionId, confirmation: "DELETE_MY_ACCOUNT",
  });
  assert.equal(retry.status, 200);
  assert.equal((await retry.json()).replay, true);

  const resurrect = await post(account, "/session/create", { accountId, displayName: "Again" });
  assert.equal(resurrect.status, 410);
  assert.equal((await resurrect.json()).error, "ACCOUNT_DELETED");
});

test("account sessions are required to read the server-owned wallet and cannot be replayed for another account", async () => {
  const { account } = makeAccount();
  const accountId = "0a2c3a83-e0fb-4abc-8ac4-581a6d994fe7";
  const createdResponse = await post(account, "/session/create", { accountId, displayName: "Test Player" });
  assert.equal(createdResponse.status, 200);
  const created = await createdResponse.json();
  assert.equal(created.accountId, accountId);
  assert.equal(created.wallet.balance, 0);
  assert.ok(created.wallet.ownedCosmetics.includes("dice_classic"));

  const walletResponse = await post(account, "/wallet/get", { accountId, sessionId: created.sessionId });
  assert.equal(walletResponse.status, 200);
  assert.equal((await walletResponse.json()).wallet.balance, 0);

  const forgedAccountResponse = await post(account, "/wallet/get", {
    accountId: "1b3d4b94-e1f0-4bc9-9cd5-592b7e0a1f21",
    sessionId: created.sessionId,
  });
  assert.equal(forgedAccountResponse.status, 401);
  assert.equal((await forgedAccountResponse.json()).error, "ACCOUNT_SESSION_INVALID");
});

test("cosmetic wallet purchase atomically debits gems, owns/selects once, and is idempotent", async () => {
  const { account, sql } = makeAccount();
  const accountId = "0a2c3a83-e0fb-4abc-8ac4-581a6d994fe7";
  const created = await (await post(account, "/session/create", { accountId, displayName: "Test Player" })).json();
  sql.wallet.get(accountId).balance = 500;

  const purchase = {
    accountId,
    sessionId: created.sessionId,
    cosmeticId: "dice_heart",
    clientRequestId: "4c4e7f90-68e5-4c30-8fb8-c5a7b0f0f8aa",
  };
  const firstResponse = await post(account, "/wallet/cosmetic/purchase", purchase);
  assert.equal(firstResponse.status, 200);
  const first = await firstResponse.json();
  assert.equal(first.wallet.balance, 250);
  assert.equal(first.wallet.ownedCosmetics.includes("dice_heart"), true);
  assert.equal(first.wallet.selectedCosmetics.DICE, "dice_heart");
  assert.equal(sql.walletTransactions.size, 1);

  const replayResponse = await post(account, "/wallet/cosmetic/purchase", purchase);
  assert.equal(replayResponse.status, 200);
  const replay = await replayResponse.json();
  assert.equal(replay.replay, true);
  assert.equal(replay.wallet.balance, 250);
  assert.equal(sql.walletTransactions.size, 1);
});


test("content-ready cosmetic purchases do not require client-only level progression", async () => {
  const { account, sql } = makeAccount();
  const accountId = "0a2c3a83-e0fb-4abc-8ac4-581a6d994fe7";
  const created = await (await post(account, "/session/create", { accountId, displayName: "Cosmetic Tester" })).json();
  sql.wallet.get(accountId).balance = 500;

  const response = await post(account, "/wallet/cosmetic/purchase", {
    accountId,
    sessionId: created.sessionId,
    cosmeticId: "dice_colors",
    clientRequestId: "8a9b7c6d-5e4f-4321-9876-123456789abc",
  });

  assert.equal(response.status, 200);
  const body = await response.json();
  assert.equal(body.wallet.balance, 300);
  assert.equal(body.wallet.ownedCosmetics.includes("dice_colors"), true);
  assert.equal(body.wallet.selectedCosmetics.DICE, "dice_colors");
});

test("refresh-token retries with the same request ID return the same rotated token", async () => {
  const { account } = makeAccount();
  const accountId = "0a2c3a83-e0fb-4abc-8ac4-581a6d994fe7";
  const session = await (await post(account, "/session/create", { accountId, displayName: "Test Player" })).json();
  const refreshRequest = {
    accountId,
    sessionId: session.sessionId,
    refreshToken: session.refreshToken,
    clientRequestId: "4c4e7f90-68e5-4c30-8fb8-c5a7b0f0f8aa",
  };

  const firstResponse = await post(account, "/session/refresh", refreshRequest);
  assert.equal(firstResponse.status, 200);
  const first = await firstResponse.json();
  assert.equal(first.replay, false);

  const retryResponse = await post(account, "/session/refresh", refreshRequest);
  assert.equal(retryResponse.status, 200);
  const retry = await retryResponse.json();
  assert.equal(retry.replay, true);
  assert.equal(retry.refreshToken, first.refreshToken);
  assert.equal(retry.sessionExpiresAt, first.sessionExpiresAt);
});

test("cosmetic-targeted AdMob view grants the configured 5 gems and progress exactly once", async () => {
  const { account, sql } = makeAccount();
  const accountId = "0a2c3a83-e0fb-4abc-8ac4-581a6d994fe7";
  const sessionResponse = await post(account, "/session/create", { accountId, displayName: "Ad Tester" });
  assert.equal(sessionResponse.status, 200);
  const event = {
    accountId,
    transactionId: "a1b2c3d4e5f60718293a4b5c6d7e8f90",
    kind: "cosmetic",
    cosmeticId: "dice_diwali",
  };

  const firstResponse = await post(account, "/wallet/reward-ad", event);
  assert.equal(firstResponse.status, 200);
  const first = await firstResponse.json();
  assert.equal(first.wallet.balance, 5);
  assert.equal(sql.wallet.get(accountId).balance, 5);
  assert.equal(sql.adProgress.get("dice_diwali"), 1);
  assert.equal(sql.walletTransactions.size, 1);
  const transaction = [...sql.walletTransactions.values()][0];
  assert.equal(transaction.kind, "REWARDED_AD_COSMETIC_PROGRESS_AND_GEMS");
  assert.equal(transaction.delta, 5);
  assert.equal(transaction.balance_after, 5);

  const replayResponse = await post(account, "/wallet/reward-ad", event);
  assert.equal(replayResponse.status, 200);
  const replay = await replayResponse.json();
  assert.equal(replay.replay, true);
  assert.equal(replay.wallet.balance, 5);
  assert.equal(sql.wallet.get(accountId).balance, 5);
  assert.equal(sql.adProgress.get("dice_diwali"), 1);
  assert.equal(sql.walletTransactions.size, 1);
});


test("deletion preview locks account mutations until the deletion saga commits or retries", async () => {
  const { account, sql } = makeAccount();
  const accountId = "0a2c3a83-e0fb-4abc-8ac4-581a6d994fe7";
  const createdResponse = await post(account, "/session/create", { accountId, displayName: "Delete Me" });
  assert.equal(createdResponse.status, 200);
  const session = await createdResponse.json();
  const before = sql.wallet.get(accountId).balance;

  const preview = await post(account, "/account/delete-preview", {
    accountId, sessionId: session.sessionId,
  });
  assert.equal(preview.status, 200);
  const previewBody = await preview.json();
  assert.equal(previewBody.deleted, false);
  assert.equal(previewBody.deletionPending, true);
  assert.equal(sql.deletionClaims.has(accountId), true);

  const wallet = await post(account, "/wallet/get", { accountId, sessionId: session.sessionId });
  assert.equal(wallet.status, 409);
  assert.equal((await wallet.json()).error, "ACCOUNT_DELETION_IN_PROGRESS");

  const profile = await post(account, "/profile/update", {
    accountId, sessionId: session.sessionId, displayName: "Changed during deletion",
  });
  assert.equal(profile.status, 409);
  assert.equal((await profile.json()).error, "ACCOUNT_DELETION_IN_PROGRESS");
  assert.equal(sql.wallet.get(accountId).balance, before);
  assert.equal(sql.accountMeta.get(accountId).display_name, "Delete Me");

  const normalSessionCheck = await post(account, "/session/check", { accountId, sessionId: session.sessionId });
  assert.equal(normalSessionCheck.status, 409);
  assert.equal((await normalSessionCheck.json()).error, "ACCOUNT_DELETION_IN_PROGRESS");

  const deletionRouteSessionCheck = await post(account, "/session/check", {
    accountId, sessionId: session.sessionId, allowDeletionPending: true,
  });
  assert.equal(deletionRouteSessionCheck.status, 200);

  const deletion = await post(account, "/account/delete", {
    accountId, sessionId: session.sessionId, confirmation: "DELETE_MY_ACCOUNT",
  });
  assert.equal(deletion.status, 200);
  assert.equal((await deletion.json()).deleted, true);
  assert.equal(sql.deletionClaims.has(accountId), false);
  assert.equal(sql.deletedAccounts.has(accountId), true);
});
