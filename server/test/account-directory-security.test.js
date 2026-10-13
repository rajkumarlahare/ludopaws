import test from "node:test";
import assert from "node:assert/strict";
import { AccountDirectory } from "../src/account-directory.js";
import { sha256Hex } from "../src/crypto.js";

class FakeSql {
  constructor() {
    this.guestRequests = new Map();
    this.deletedGuestRequests = new Map();
    this.accounts = new Map();
    this.googleIdentities = new Map();
    this.guestDeletionCredentials = new Map();
    this.guestDeletionClaims = new Map();
    this.deletedGuestDeletionCredentials = new Map();
    this.accountDeletionClaims = new Map();
  }

  exec(statement, ...params) {
    const sql = String(statement).replace(/\s+/g, " ").trim();
    if (sql.startsWith("SELECT client_request_id FROM deleted_guest_requests")) {
      const row = this.deletedGuestRequests.get(params[0]);
      return row ? [{ client_request_id: params[0] }] : [];
    }
    if (sql.startsWith("SELECT credential_hash FROM deleted_guest_deletion_credentials")) {
      return this.deletedGuestDeletionCredentials.has(params[0]) ? [{ credential_hash: params[0] }] : [];
    }
    if (sql.startsWith("SELECT account_id FROM account_deletion_claims WHERE account_id = ?")) {
      return this.accountDeletionClaims.has(params[0]) ? [{ account_id: params[0] }] : [];
    }
    if (sql.startsWith("SELECT source, credential_hash FROM account_deletion_claims WHERE account_id = ?")) {
      const row = this.accountDeletionClaims.get(params[0]);
      return row ? [{ source: row.source, credential_hash: row.credentialHash }] : [];
    }
    if (sql.startsWith("INSERT INTO account_deletion_claims")) {
      const [accountId, source, credentialHash, claimedAt] = params;
      this.accountDeletionClaims.set(accountId, { source, credentialHash, claimedAt });
      return { rowsWritten: 1 };
    }
    if (sql.startsWith("INSERT OR IGNORE INTO account_deletion_claims")) {
      if (!this.accountDeletionClaims.has(params[0])) {
        this.accountDeletionClaims.set(params[0], {
          source: "GUEST_CODE", credentialHash: params[1], claimedAt: params[2],
        });
      }
      return { rowsWritten: 1 };
    }
    if (sql.startsWith("DELETE FROM account_deletion_claims WHERE account_id = ?")) {
      this.accountDeletionClaims.delete(params[0]);
      return { rowsWritten: 1 };
    }
    if (sql.startsWith("SELECT account_kind FROM account_registry WHERE account_id = ?")) {
      const row = this.accounts.get(params[0]);
      return row ? [{ account_kind: row.kind }] : [];
    }
    if (sql.startsWith("SELECT account_id FROM google_identities WHERE account_id = ?")) {
      return this.googleIdentities.has(params[0]) ? [{ account_id: params[0] }] : [];
    }
    if (sql.startsWith("SELECT account_id FROM guest_deletion_credentials WHERE credential_hash = ?")) {
      const row = this.guestDeletionCredentials.get(params[0]);
      return row ? [{ account_id: row.accountId }] : [];
    }
    if (sql.startsWith("SELECT credential_hash FROM guest_deletion_credentials WHERE account_id = ?")) {
      return [...this.guestDeletionCredentials.entries()]
        .filter(([, value]) => value.accountId === params[0])
        .map(([credential_hash]) => ({ credential_hash }));
    }
    if (sql.startsWith("SELECT credential_hash FROM guest_deletion_claims WHERE account_id = ?")) {
      const value = this.guestDeletionClaims.get(params[0]);
      return value ? [{ credential_hash: value.credentialHash }] : [];
    }
    if (sql.startsWith("SELECT account_id FROM guest_deletion_claims WHERE account_id = ?")) {
      return this.guestDeletionClaims.has(params[0]) ? [{ account_id: params[0] }] : [];
    }
    if (sql.startsWith("SELECT account_id FROM guest_deletion_claims WHERE account_id = ?")) {
      return this.guestDeletionClaims.has(params[0]) ? [{ account_id: params[0] }] : [];
    }
    if (sql.startsWith("SELECT client_request_id FROM guest_requests WHERE account_id = ?")) {
      return [...this.guestRequests.entries()].filter(([, value]) => value.accountId === params[0]).map(([client_request_id]) => ({ client_request_id }));
    }
    if (sql.startsWith("SELECT account_id FROM account_registry WHERE account_id = ?")) {
      return this.accounts.has(params[0]) ? [{ account_id: params[0] }] : [];
    }
    if (sql.startsWith("INSERT OR IGNORE INTO deleted_guest_deletion_credentials")) {
      if (!this.deletedGuestDeletionCredentials.has(params[0])) this.deletedGuestDeletionCredentials.set(params[0], { deletedAt: params[1] });
      return { rowsWritten: 1 };
    }
    if (sql.startsWith("INSERT INTO guest_deletion_credentials")) {
      const [credentialHash, accountId, createdAt] = params;
      const existing = this.guestDeletionCredentials.get(credentialHash);
      if (existing && existing.accountId !== accountId) throw new Error("UNIQUE constraint failed: guest_deletion_credentials.credential_hash");
      this.guestDeletionCredentials.set(credentialHash, { accountId, createdAt });
      return { rowsWritten: 1 };
    }
    if (sql.startsWith("INSERT OR IGNORE INTO guest_deletion_claims")) {
      if (!this.guestDeletionClaims.has(params[0])) this.guestDeletionClaims.set(params[0], { credentialHash: params[1], createdAt: params[2] });
      return { rowsWritten: 1 };
    }
    if (sql.startsWith("DELETE FROM guest_deletion_credentials WHERE account_id = ?")) {
      for (const [key, value] of this.guestDeletionCredentials.entries()) if (value.accountId === params[0]) this.guestDeletionCredentials.delete(key);
      return { rowsWritten: 1 };
    }
    if (sql.startsWith("DELETE FROM guest_deletion_claims WHERE account_id = ?")) {
      this.guestDeletionClaims.delete(params[0]);
      return { rowsWritten: 1 };
    }
    if (sql.startsWith("INSERT OR IGNORE INTO deleted_guest_requests")) {
      if (!this.deletedGuestRequests.has(params[0])) this.deletedGuestRequests.set(params[0], { deletedAt: params[1] });
      return { rowsWritten: 1 };
    }
    if (sql.startsWith("DELETE FROM guest_requests WHERE account_id = ?")) {
      for (const [key, value] of this.guestRequests.entries()) if (value.accountId === params[0]) this.guestRequests.delete(key);
      return { rowsWritten: 1 };
    }
    if (sql.startsWith("DELETE FROM google_identities WHERE account_id = ?")) {
      this.googleIdentities.delete(params[0]);
      return { rowsWritten: 1 };
    }
    if (sql.startsWith("DELETE FROM account_identity_links WHERE account_id = ?")) return { rowsWritten: 1 };
    if (sql.startsWith("UPDATE account_registry SET account_kind = 'GOOGLE_LINKED' WHERE account_id = ?")) {
      const row = this.accounts.get(params[0]);
      if (row) this.accounts.set(params[0], { ...row, kind: "GOOGLE_LINKED" });
      return { rowsWritten: row ? 1 : 0 };
    }
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


test("guest deletion codes are stored as hashes, block Google linking after claim, and replay only after deletion completes", async () => {
  const { directory, sql } = makeDirectory();
  const requestId = "7a4e7f90-68e5-4c30-8fb8-c5a7b0f0f8cc";
  const createdResponse = await guestRequest(directory, requestId);
  assert.equal(createdResponse.status, 200);
  const created = await createdResponse.json();

  const registered = await directory.fetch(new Request("https://account-directory/guest-deletion/register", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ accountId: created.accountId }),
  }));
  assert.equal(registered.status, 200);
  const registeredBody = await registered.json();
  const credential = registeredBody.credential;
  assert.match(credential, /^[a-f0-9]{64}$/);
  const credentialHash = await sha256Hex("ludoproof:guest-deletion-credential:v1:" + credential);
  assert.equal(sql.guestDeletionCredentials.has(credentialHash), true);
  assert.equal(sql.guestDeletionCredentials.has(credential), false);

  const secondRegistration = await directory.fetch(new Request("https://account-directory/guest-deletion/register", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ accountId: created.accountId }),
  }));
  assert.equal(secondRegistration.status, 200);
  const secondCredential = (await secondRegistration.json()).credential;
  assert.match(secondCredential, /^[a-f0-9]{64}$/);
  assert.notEqual(secondCredential, credential);
  const secondCredentialHash = await sha256Hex("ludoproof:guest-deletion-credential:v1:" + secondCredential);
  assert.equal(sql.guestDeletionCredentials.has(credentialHash), true, "registering a replacement-device code must not revoke the previously saved code");
  assert.equal(sql.guestDeletionCredentials.has(secondCredentialHash), true);

  const claimedResponse = await directory.fetch(new Request("https://account-directory/guest-deletion/claim", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ credentialHash }),
  }));
  assert.equal(claimedResponse.status, 200);
  const claimed = await claimedResponse.json();
  assert.equal(claimed.accountId, created.accountId);
  assert.equal(claimed.deleted, false);
  assert.equal(sql.guestDeletionClaims.get(created.accountId).credentialHash, credentialHash);

  const guestRecoveryReplay = await guestRequest(directory, requestId);
  assert.equal(guestRecoveryReplay.status, 409);
  assert.equal((await guestRecoveryReplay.json()).error, "ACCOUNT_DELETION_IN_PROGRESS");

  const linking = await directory.fetch(new Request("https://account-directory/google/link", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ accountId: created.accountId, subjectHash: "cd".repeat(32) }),
  }));
  assert.equal(linking.status, 409);
  assert.equal((await linking.json()).error, "ACCOUNT_DELETION_IN_PROGRESS");
  assert.equal(sql.googleIdentities.has(created.accountId), false);

  const removed = await directory.fetch(new Request("https://account-directory/account/delete", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ accountId: created.accountId }),
  }));
  assert.equal(removed.status, 200);
  assert.equal(sql.guestDeletionCredentials.has(credentialHash), false);
  assert.equal(sql.guestDeletionCredentials.has(secondCredentialHash), false);
  assert.equal(sql.guestDeletionClaims.has(created.accountId), false);
  assert.equal(sql.deletedGuestDeletionCredentials.has(credentialHash), true);
  assert.equal(sql.deletedGuestDeletionCredentials.has(secondCredentialHash), true);

  const replay = await directory.fetch(new Request("https://account-directory/guest-deletion/claim", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ credentialHash }),
  }));
  assert.equal(replay.status, 200);
  assert.deepEqual(await replay.json(), { ok: true, deleted: true, replay: true });
});



test("explicit guest deletion code rotation invalidates every previous saved code", async () => {
  const { directory, sql } = makeDirectory();
  const requestId = "8d5e7f90-68e5-4c30-8fb8-c5a7b0f0f8e1";
  const createdResponse = await guestRequest(directory, requestId);
  const created = await createdResponse.json();

  const register = (rotate) => directory.fetch(new Request("https://account-directory/guest-deletion/register", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ accountId: created.accountId, rotate }),
  }));
  const firstResponse = await register(false);
  assert.equal(firstResponse.status, 200);
  const firstCredential = (await firstResponse.json()).credential;
  const firstHash = await sha256Hex("ludoproof:guest-deletion-credential:v1:" + firstCredential);
  assert.equal(sql.guestDeletionCredentials.has(firstHash), true);

  const additionalResponse = await register(false);
  assert.equal(additionalResponse.status, 200);
  const additionalCredential = (await additionalResponse.json()).credential;
  const additionalHash = await sha256Hex("ludoproof:guest-deletion-credential:v1:" + additionalCredential);
  assert.equal(sql.guestDeletionCredentials.has(firstHash), true);
  assert.equal(sql.guestDeletionCredentials.has(additionalHash), true);

  const replacementResponse = await register(true);
  assert.equal(replacementResponse.status, 200);
  const replacementCredential = (await replacementResponse.json()).credential;
  const replacementHash = await sha256Hex("ludoproof:guest-deletion-credential:v1:" + replacementCredential);
  assert.equal(sql.guestDeletionCredentials.size, 1);
  assert.equal(sql.guestDeletionCredentials.has(firstHash), false);
  assert.equal(sql.guestDeletionCredentials.has(additionalHash), false);
  assert.equal(sql.guestDeletionCredentials.has(replacementHash), true);

  for (const oldHash of [firstHash, additionalHash]) {
    const denied = await directory.fetch(new Request("https://account-directory/guest-deletion/claim", {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ credentialHash: oldHash }),
    }));
    assert.equal(denied.status, 404);
    assert.equal((await denied.json()).error, "GUEST_DELETION_CODE_INVALID");
  }
});

test("guest deletion codes are unavailable for Google-linked accounts", async () => {
  const { directory, sql } = makeDirectory();
  const requestId = "8b5e7f90-68e5-4c30-8fb8-c5a7b0f0f8dd";
  const createdResponse = await guestRequest(directory, requestId);
  const created = await createdResponse.json();
  sql.googleIdentities.set(created.accountId, { subjectHash: "ef".repeat(32) });
  sql.accounts.set(created.accountId, { kind: "GOOGLE_LINKED", createdAt: Date.now() });
  const response = await directory.fetch(new Request("https://account-directory/guest-deletion/register", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ accountId: created.accountId }),
  }));
  assert.equal(response.status, 409);
  assert.equal((await response.json()).error, "GUEST_DELETION_CODE_NOT_AVAILABLE");
  assert.equal(sql.guestDeletionCredentials.size, 0);
});

test("AccountDirectory prepare locks identity changes and is safely idempotent", async () => {
  const { directory, sql } = makeDirectory();
  const requestId = "924e7f90-68e5-4c30-8fb8-c5a7b0f0f811";
  const createdResponse = await guestRequest(directory, requestId);
  assert.equal(createdResponse.status, 200);
  const created = await createdResponse.json();

  const prepare = () => directory.fetch(new Request("https://account-directory/account/delete-prepare", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ accountId: created.accountId, source: "SESSION" }),
  }));
  const first = await prepare();
  assert.equal(first.status, 200);
  assert.deepEqual(await first.json(), { ok: true, claimed: true, replay: false });
  assert.deepEqual(sql.accountDeletionClaims.get(created.accountId), {
    source: "SESSION", credentialHash: null, claimedAt: sql.accountDeletionClaims.get(created.accountId).claimedAt,
  });

  const repeat = await prepare();
  assert.equal(repeat.status, 200);
  assert.deepEqual(await repeat.json(), { ok: true, claimed: true, replay: true });

  const createCode = await directory.fetch(new Request("https://account-directory/guest-deletion/register", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ accountId: created.accountId, credentialHash: "12".repeat(32) }),
  }));
  assert.equal(createCode.status, 409);
  assert.equal((await createCode.json()).error, "ACCOUNT_DELETION_IN_PROGRESS");

  const linking = await directory.fetch(new Request("https://account-directory/google/link", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ accountId: created.accountId, subjectHash: "34".repeat(32) }),
  }));
  assert.equal(linking.status, 409);
  assert.equal((await linking.json()).error, "ACCOUNT_DELETION_IN_PROGRESS");

  const externalLink = await directory.fetch(new Request("https://account-directory/identity/link", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({
      accountId: created.accountId,
      identityType: "friend",
      externalId: "LPF-ABCD-EFGH-IJKL",
    }),
  }));
  assert.equal(externalLink.status, 409);
  assert.equal((await externalLink.json()).error, "ACCOUNT_DELETION_IN_PROGRESS");

  const differentSource = await directory.fetch(new Request("https://account-directory/account/delete-prepare", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ accountId: created.accountId, source: "GUEST_CODE", credentialHash: "56".repeat(32) }),
  }));
  assert.equal(differentSource.status, 409);
  assert.equal((await differentSource.json()).error, "ACCOUNT_DELETION_IN_PROGRESS");

  const deleted = await directory.fetch(new Request("https://account-directory/account/delete", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ accountId: created.accountId }),
  }));
  assert.equal(deleted.status, 200);
  assert.equal(sql.accountDeletionClaims.has(created.accountId), false);
});
