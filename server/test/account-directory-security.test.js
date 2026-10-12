import test from "node:test";
import assert from "node:assert/strict";
import { AccountDirectory } from "../src/account-directory.js";
import { sha256Hex } from "../src/crypto.js";

class FakeSql {
  constructor() {
    this.guestRequests = new Map();
    this.deletedGuestRequests = new Map();
    this.accounts = new Map();
  }

  exec(statement, ...params) {
    const sql = String(statement).replace(/\s+/g, " ").trim();
    if (sql.startsWith("SELECT client_request_id FROM deleted_guest_requests")) {
      const row = this.deletedGuestRequests.get(params[0]);
      return row ? [{ client_request_id: params[0] }] : [];
    }
    if (sql.startsWith("SELECT client_request_id FROM guest_requests WHERE account_id = ?")) {
      return [...this.guestRequests.entries()].filter(([, value]) => value.accountId === params[0]).map(([client_request_id]) => ({ client_request_id }));
    }
    if (sql.startsWith("SELECT account_id FROM account_registry WHERE account_id = ?")) {
      return this.accounts.has(params[0]) ? [{ account_id: params[0] }] : [];
    }
    if (sql.startsWith("INSERT OR IGNORE INTO deleted_guest_requests")) {
      if (!this.deletedGuestRequests.has(params[0])) this.deletedGuestRequests.set(params[0], { deletedAt: params[1] });
      return { rowsWritten: 1 };
    }
    if (sql.startsWith("DELETE FROM guest_requests WHERE account_id = ?")) {
      for (const [key, value] of this.guestRequests.entries()) if (value.accountId === params[0]) this.guestRequests.delete(key);
      return { rowsWritten: 1 };
    }
    if (sql.startsWith("DELETE FROM google_identities WHERE account_id = ?") || sql.startsWith("DELETE FROM account_identity_links WHERE account_id = ?")) return { rowsWritten: 1 };
    if (sql.startsWith("DELETE FROM account_registry WHERE account_id = ?")) {
      this.accounts.delete(params[0]);
      return { rowsWritten: 1 };
    }
    if (sql.startsWith("SELECT account_id FROM guest_requests")) {
      const row = this.guestRequests.get(params[0]);
      return row ? [{ account_id: row.accountId }] : [];
    }
    if (sql.startsWith("INSERT INTO account_registry")) {
      this.accounts.set(params[0], { kind: "GUEST", createdAt: params[1] });
      return { rowsWritten: 1 };
    }
    if (sql.startsWith("INSERT INTO guest_requests")) {
      if (this.guestRequests.has(params[0])) throw new Error("duplicate guest recovery key");
      this.guestRequests.set(params[0], { accountId: params[1], createdAt: params[2] });
      return { rowsWritten: 1 };
    }
    // The Durable Object constructor's schema declarations are intentionally no-ops.
    if (/^(CREATE TABLE|CREATE INDEX)/i.test(sql)) return { rowsWritten: 0 };
    throw new Error("Unexpected SQL in account-directory security test: " + sql);
  }
}

function makeDirectory() {
  const sql = new FakeSql();
  const storage = { sql, transactionSync: (callback) => callback() };
  const context = {
    storage,
    blockConcurrencyWhile: (callback) => callback(),
  };
  return { directory: new AccountDirectory(context), sql };
}

async function guestRequest(directory, clientRequestId) {
  return directory.fetch(new Request("https://account-directory/guest", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ clientRequestId, displayName: "Guest" }),
  }));
}

test("guest recovery IDs are stored as domain-separated hashes and remain idempotent", async () => {
  const { directory, sql } = makeDirectory();
  const requestId = "4c4e7f90-68e5-4c30-8fb8-c5a7b0f0f8aa";

  const firstResponse = await guestRequest(directory, requestId);
  assert.equal(firstResponse.status, 200);
  const first = await firstResponse.json();
  assert.match(first.accountId, /^[0-9a-f-]{36}$/);
  assert.equal(first.replay, false);

  const expectedKey = await sha256Hex("ludoproof:guest-recovery-key:v1:" + requestId);
  assert.equal(sql.guestRequests.has(requestId), false);
  assert.equal(sql.guestRequests.has(expectedKey), true);
  assert.equal(sql.guestRequests.size, 1);

  const replayResponse = await guestRequest(directory, requestId);
  assert.equal(replayResponse.status, 200);
  const replay = await replayResponse.json();
  assert.equal(replay.accountId, first.accountId);
  assert.equal(replay.replay, true);
  assert.equal(sql.accounts.size, 1);
});

test("guest account deletion tombstones the recovery key and is idempotent", async () => {
  const { directory, sql } = makeDirectory();
  const requestId = "663e7f90-68e5-4c30-8fb8-c5a7b0f0f8bb";
  const createdResponse = await guestRequest(directory, requestId);
  assert.equal(createdResponse.status, 200);
  const created = await createdResponse.json();

  const deletedResponse = await directory.fetch(new Request("https://account-directory/account/delete", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ accountId: created.accountId }),
  }));
  assert.equal(deletedResponse.status, 200);
  assert.equal((await deletedResponse.json()).deleted, true);
  assert.equal(sql.accounts.has(created.accountId), false);

  const repeated = await directory.fetch(new Request("https://account-directory/account/delete", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ accountId: created.accountId }),
  }));
  assert.equal(repeated.status, 200);
  assert.equal((await repeated.json()).replay, true);

  const recovered = await guestRequest(directory, requestId);
  assert.equal(recovered.status, 410);
  assert.equal((await recovered.json()).error, "GUEST_ACCOUNT_DELETED");
});
