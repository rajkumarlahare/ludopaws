import { httpError, readJsonObject, sha256Hex } from "./crypto.js";
import { hashGoogleNonce, isAccountId } from "./account-security.js";

const FRIEND_ID = /^LPF-[A-Z2-9]{4}-[A-Z2-9]{4}-[A-Z2-9]{4}$/;
const PROFILE_ID = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
const UUID_V4 = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
const NONCE_TTL_MS = 5 * 60 * 1000;

export class AccountDirectory {
  constructor(ctx) {
    this.ctx = ctx;
    ctx.blockConcurrencyWhile(async () => {
      const sql = ctx.storage.sql;
      sql.exec(`CREATE TABLE IF NOT EXISTS account_registry (account_id TEXT PRIMARY KEY, account_kind TEXT NOT NULL, created_at INTEGER NOT NULL)`);
      sql.exec(`CREATE TABLE IF NOT EXISTS guest_requests (client_request_id TEXT PRIMARY KEY, account_id TEXT NOT NULL, created_at INTEGER NOT NULL)`);
      sql.exec(`CREATE TABLE IF NOT EXISTS deleted_guest_requests (client_request_id TEXT PRIMARY KEY, deleted_at INTEGER NOT NULL)`);
      sql.exec(`CREATE TABLE IF NOT EXISTS google_identities (subject_hash TEXT PRIMARY KEY, account_id TEXT NOT NULL UNIQUE, linked_at INTEGER NOT NULL)`);
      sql.exec(`CREATE TABLE IF NOT EXISTS google_nonces (nonce_hash TEXT PRIMARY KEY, created_at INTEGER NOT NULL, expires_at INTEGER NOT NULL, consumed_at INTEGER)`);
      sql.exec(`CREATE TABLE IF NOT EXISTS account_identity_links (identity_type TEXT NOT NULL, external_id TEXT NOT NULL, account_id TEXT NOT NULL, linked_at INTEGER NOT NULL, PRIMARY KEY(identity_type, external_id), UNIQUE(identity_type, account_id))`);
      sql.exec("CREATE INDEX IF NOT EXISTS idx_account_links_account ON account_identity_links(account_id, identity_type)");
    });
  }

  async fetch(request) {
    try {
      const url = new URL(request.url);
      if (request.method === "POST" && url.pathname === "/guest") return await this.#createGuest(request);
      if (request.method === "POST" && url.pathname === "/account/delete") return await this.#deleteAccount(request);
      if (request.method === "POST" && url.pathname === "/nonce/create") return await this.#createNonce();
      if (request.method === "POST" && url.pathname === "/nonce/consume") return await this.#consumeNonce(request);
      if (request.method === "POST" && url.pathname === "/google/link") return await this.#linkGoogle(request);
      if (request.method === "POST" && url.pathname === "/google/lookup") return await this.#lookupGoogle(request);
      if (request.method === "POST" && url.pathname === "/identity/link") return await this.#linkExternalIdentity(request);
      if (request.method === "POST" && url.pathname === "/identity/list") return await this.#listExternalIdentities(request);
      return json(404, { error: "NOT_FOUND", message: "route not found" });
    } catch (error) { return errorResponse(error); }
  }

  async #createGuest(request) {
    const body = await readJson(request);
    const requestId = requireUuid(body?.clientRequestId, "clientRequestId");
    // This key is a guest-account recovery credential. Never persist the raw bearer-like value.
    const requestKey = await sha256Hex("ludoproof:guest-recovery-key:v1:" + requestId);
    const deleted = [...this.ctx.storage.sql.exec("SELECT client_request_id FROM deleted_guest_requests WHERE client_request_id = ? LIMIT 1", requestKey)];
    if (deleted.length) throw httpError(410, "GUEST_ACCOUNT_DELETED", "this guest account has already been deleted");
    const existing = [...this.ctx.storage.sql.exec("SELECT account_id FROM guest_requests WHERE client_request_id = ? LIMIT 1", requestKey)];
    if (existing.length === 1) return json(200, { ok: true, accountId: String(existing[0].account_id), replay: true });

    const generatedId = crypto.randomUUID().toLowerCase();
    const now = Date.now();
    this.ctx.storage.transactionSync(() => {
      const raced = [...this.ctx.storage.sql.exec("SELECT account_id FROM guest_requests WHERE client_request_id = ? LIMIT 1", requestKey)];
      if (raced.length > 0) return;
      this.ctx.storage.sql.exec("INSERT INTO account_registry (account_id, account_kind, created_at) VALUES (?, 'GUEST', ?)", generatedId, now);
      this.ctx.storage.sql.exec("INSERT INTO guest_requests (client_request_id, account_id, created_at) VALUES (?, ?, ?)", requestKey, generatedId, now);
    });
    const saved = [...this.ctx.storage.sql.exec("SELECT account_id FROM guest_requests WHERE client_request_id = ? LIMIT 1", requestKey)];
    if (saved.length !== 1) throw httpError(503, "ACCOUNT_CREATE_FAILED", "guest account could not be created safely");
    return json(200, { ok: true, accountId: String(saved[0].account_id), replay: String(saved[0].account_id) !== generatedId });
  }

  async #deleteAccount(request) {
    const body = await readJson(request);
    const accountId = requireAccountId(body?.accountId);
    const keys = [...this.ctx.storage.sql.exec("SELECT client_request_id FROM guest_requests WHERE account_id = ?", accountId)]
      .map((row) => String(row.client_request_id));
    let existed = false;
    const now = Date.now();
    this.ctx.storage.transactionSync(() => {
      existed = [...this.ctx.storage.sql.exec("SELECT account_id FROM account_registry WHERE account_id = ? LIMIT 1", accountId)].length === 1;
      for (const key of keys) this.ctx.storage.sql.exec(
        "INSERT OR IGNORE INTO deleted_guest_requests (client_request_id, deleted_at) VALUES (?, ?)", key, now,
      );
      this.ctx.storage.sql.exec("DELETE FROM guest_requests WHERE account_id = ?", accountId);
      this.ctx.storage.sql.exec("DELETE FROM google_identities WHERE account_id = ?", accountId);
      this.ctx.storage.sql.exec("DELETE FROM account_identity_links WHERE account_id = ?", accountId);
      this.ctx.storage.sql.exec("DELETE FROM account_registry WHERE account_id = ?", accountId);
    });
    return json(200, { ok: true, deleted: true, replay: !existed });
  }

  async #createNonce() {
    const nonce = [...crypto.getRandomValues(new Uint8Array(32))].map((byte) => byte.toString(16).padStart(2, "0")).join("");
    const nonceHash = await hashGoogleNonce(nonce);
    const now = Date.now();
    this.ctx.storage.sql.exec("INSERT INTO google_nonces (nonce_hash, created_at, expires_at, consumed_at) VALUES (?, ?, ?, NULL)", nonceHash, now, now + NONCE_TTL_MS);
    this.ctx.storage.sql.exec("DELETE FROM google_nonces WHERE expires_at < ?", now - NONCE_TTL_MS);
    return json(200, { ok: true, nonce, expiresAt: now + NONCE_TTL_MS });
  }

  async #consumeNonce(request) {
    const body = await readJson(request);
    if (typeof body?.nonce !== "string" || body.nonce.length < 16 || body.nonce.length > 512) throw httpError(400, "GOOGLE_NONCE_INVALID", "Google sign-in challenge is invalid");
    const hash = await hashGoogleNonce(body.nonce);
    const now = Date.now();
    let consumed = false;
    this.ctx.storage.transactionSync(() => {
      const update = this.ctx.storage.sql.exec("UPDATE google_nonces SET consumed_at = ? WHERE nonce_hash = ? AND consumed_at IS NULL AND expires_at > ?", now, hash, now);
      consumed = Number(update.rowsWritten ?? 0) === 1;
    });
    if (!consumed) throw httpError(401, "GOOGLE_NONCE_EXPIRED_OR_REPLAYED", "Google sign-in challenge expired or was already used");
    return json(200, { ok: true });
  }

  async #linkGoogle(request) {
    const body = await readJson(request);
    const accountId = requireAccountId(body?.accountId);
    const subjectHash = requireHash(body?.subjectHash, "subjectHash");
    if ([...this.ctx.storage.sql.exec("SELECT account_id FROM account_registry WHERE account_id = ? LIMIT 1", accountId)].length !== 1) throw httpError(404, "ACCOUNT_NOT_FOUND", "account was not found");
    const rows = [...this.ctx.storage.sql.exec("SELECT subject_hash, account_id FROM google_identities WHERE subject_hash = ? OR account_id = ? LIMIT 2", subjectHash, accountId)];
    if (rows.length) {
      const exact = rows.some((row) => String(row.subject_hash) === subjectHash && String(row.account_id) === accountId);
      const conflict = rows.some((row) => String(row.subject_hash) !== subjectHash || String(row.account_id) !== accountId);
      if (exact && !conflict) return json(200, { ok: true, accountId, linked: true, replay: true });
      throw httpError(409, "GOOGLE_IDENTITY_ALREADY_LINKED", "this Google account is already linked to another Ludo Paws account, or this account already has a different Google identity");
    }
    this.ctx.storage.transactionSync(() => {
      this.ctx.storage.sql.exec("INSERT INTO google_identities (subject_hash, account_id, linked_at) VALUES (?, ?, ?)", subjectHash, accountId, Date.now());
      this.ctx.storage.sql.exec("UPDATE account_registry SET account_kind = 'GOOGLE_LINKED' WHERE account_id = ?", accountId);
    });
    return json(200, { ok: true, accountId, linked: true, replay: false });
  }

  async #lookupGoogle(request) {
    const body = await readJson(request);
    const subjectHash = requireHash(body?.subjectHash, "subjectHash");
    const rows = [...this.ctx.storage.sql.exec("SELECT account_id FROM google_identities WHERE subject_hash = ? LIMIT 1", subjectHash)];
    if (!rows.length) throw httpError(404, "GOOGLE_IDENTITY_NOT_LINKED", "this Google account has not been linked to a Ludo Paws account yet");
    return json(200, { ok: true, accountId: String(rows[0].account_id) });
  }

  async #linkExternalIdentity(request) {
    const body = await readJson(request);
    const accountId = requireAccountId(body?.accountId);
    const identityType = body?.identityType;
    const externalId = String(body?.externalId ?? "").trim();
    if (!["friend", "leaderboard"].includes(identityType)) throw httpError(400, "IDENTITY_TYPE_INVALID", "identity type is invalid");
    if ((identityType === "friend" && !FRIEND_ID.test(externalId)) || (identityType === "leaderboard" && !PROFILE_ID.test(externalId))) throw httpError(400, "EXTERNAL_ID_INVALID", "linked identity is invalid");
    if ([...this.ctx.storage.sql.exec("SELECT account_id FROM account_registry WHERE account_id = ? LIMIT 1", accountId)].length !== 1) throw httpError(404, "ACCOUNT_NOT_FOUND", "account was not found");
    const rows = [...this.ctx.storage.sql.exec("SELECT identity_type, external_id, account_id FROM account_identity_links WHERE identity_type = ? AND (external_id = ? OR account_id = ?) LIMIT 2", identityType, externalId, accountId)];
    if (rows.length) {
      const exact = rows.some((row) => String(row.external_id) === externalId && String(row.account_id) === accountId);
      const conflict = rows.some((row) => String(row.external_id) !== externalId || String(row.account_id) !== accountId);
      if (exact && !conflict) return json(200, { ok: true, identityType, externalId, replay: true });
      throw httpError(409, "ACCOUNT_IDENTITY_CONFLICT", "this profile or friend identity is already linked to another account");
    }
    this.ctx.storage.sql.exec("INSERT INTO account_identity_links (identity_type, external_id, account_id, linked_at) VALUES (?, ?, ?, ?)", identityType, externalId, accountId, Date.now());
    return json(200, { ok: true, identityType, externalId, replay: false });
  }

  async #listExternalIdentities(request) {
    const body = await readJson(request);
    const accountId = requireAccountId(body?.accountId);
    const rows = [...this.ctx.storage.sql.exec("SELECT identity_type, external_id FROM account_identity_links WHERE account_id = ?", accountId)];
    const links = {};
    for (const row of rows) links[String(row.identity_type)] = String(row.external_id);
    const googleRows = [...this.ctx.storage.sql.exec("SELECT subject_hash FROM google_identities WHERE account_id = ? LIMIT 1", accountId)];
    return json(200, { ok: true, accountId, googleLinked: googleRows.length === 1, friendId: links.friend ?? null, leaderboardProfileId: links.leaderboard ?? null });
  }
}

function requireUuid(value, field) {
  if (typeof value !== "string" || !UUID_V4.test(value)) throw httpError(400, "INVALID_REQUEST_ID", field + " must be a UUID v4");
  return value.toLowerCase();
}
function requireAccountId(value) {
  if (!isAccountId(value)) throw httpError(400, "INVALID_ACCOUNT_ID", "account identity is invalid");
  return value.toLowerCase();
}
function requireHash(value, field) {
  if (typeof value !== "string" || !/^[a-f0-9]{64}$/.test(value)) throw httpError(400, "INVALID_" + field.toUpperCase(), field + " is invalid");
  return value;
}
async function readJson(request) {
  return readJsonObject(request, 8 * 1024);
}
function errorResponse(error) {
  return json(Number.isInteger(error?.status) && error.status >= 400 && error.status <= 599 ? error.status : 500, {
    error: error?.code ?? "INTERNAL_ERROR",
    message: Number.isInteger(error?.status) ? String(error?.message ?? "request failed") : "internal server error",
  });
}
function json(status, body) {
  return Response.json(body, { status, headers: { "cache-control": "no-store", "content-type": "application/json; charset=utf-8" } });
}
