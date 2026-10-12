import test from "node:test";
import assert from "node:assert/strict";
import { FriendDirectory } from "../src/friend-directory.js";
import { LeaderboardRoom } from "../src/leaderboard-room.js";

const SECRET = "lp_test_session_hmac_key_1234567890abcdef";
const REQUEST_ID = "00000000-0000-4000-8000-000000000001";

function request(path, body) {
  return new Request("https://unit.test" + path, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify(body),
  });
}

function namespace() {
  return {
    idFromName(name) { return name; },
    get() {
      return { async fetch() { return Response.json({ ok: true }); } };
    },
  };
}

function makeContext(sql) {
  return {
    storage: {
      sql,
      transactionSync(callback) { return callback(); },
    },
    blockConcurrencyWhile(callback) { return callback(); },
  };
}

class FriendSql {
  profiles = new Map();
  deletedProfiles = new Set();
  deletedRegistrationHashes = new Set();

  exec(statement, ...params) {
    const sql = String(statement).replace(/\s+/g, " ").trim().toLowerCase();
    if (sql.startsWith("create table") || sql.startsWith("create index")) return [];
    if (sql.startsWith("select friend_id from deleted_friend_profiles")) {
      return this.deletedProfiles.has(params[0]) ? [{ friend_id: params[0] }] : [];
    }
    if (sql.startsWith("select request_hash from deleted_friend_registration_requests")) {
      return this.deletedRegistrationHashes.has(params[0]) ? [{ request_hash: params[0] }] : [];
    }
    if (sql.startsWith("select friend_id, registration_request_id from friend_profiles")) {
      return [...this.profiles.values()]
        .filter(row => row.friend_id === params[0] || row.registration_request_id === params[1] || row.token_hash === params[2])
        .slice(0, 1)
        .map(row => ({ friend_id: row.friend_id, registration_request_id: row.registration_request_id }));
    }
    if (sql.startsWith("select registration_request_id from friend_profiles")) {
      const row = this.profiles.get(params[0]);
      return row ? [{ registration_request_id: row.registration_request_id }] : [];
    }
    if (sql.startsWith("select friend_id from friend_profiles")) {
      return this.profiles.has(params[0]) ? [{ friend_id: params[0] }] : [];
    }
    if (sql.startsWith("insert into friend_profiles")) {
      const [friendId, tokenHash, registrationRequestId, displayName, createdAt, lastSeenAt] = params;
      this.profiles.set(friendId, { friend_id: friendId, token_hash: tokenHash, registration_request_id: registrationRequestId, display_name: displayName, created_at: createdAt, last_seen_at: lastSeenAt });
      return [{ rowsWritten: 1 }];
    }
    if (sql.startsWith("update friend_profiles")) {
      const [tokenHash, displayName, lastSeenAt, friendId] = params;
      const row = this.profiles.get(friendId);
      if (row) Object.assign(row, { token_hash: tokenHash, display_name: displayName, last_seen_at: lastSeenAt });
      return [{ rowsWritten: row ? 1 : 0 }];
    }
    if (sql.startsWith("insert or ignore into deleted_friend_profiles")) {
      this.deletedProfiles.add(params[0]);
      return [];
    }
    if (sql.startsWith("insert or ignore into deleted_friend_registration_requests")) {
      this.deletedRegistrationHashes.add(params[0]);
      return [];
    }
    if (sql.startsWith("delete from friend_profiles")) {
      this.profiles.delete(params[0]);
      return [];
    }
    return [];
  }
}

class LeaderboardSql {
  profiles = new Map();
  deletedProfiles = new Set();
  deletedRegistrationHashes = new Set();

  exec(statement, ...params) {
    const sql = String(statement).replace(/\s+/g, " ").trim().toLowerCase();
    if (sql.startsWith("create table") || sql.startsWith("create index")) return [];
    if (sql.startsWith("select profile_id from deleted_leaderboard_profiles")) {
      return this.deletedProfiles.has(params[0]) ? [{ profile_id: params[0] }] : [];
    }
    if (sql.startsWith("select request_hash from deleted_leaderboard_registration_requests")) {
      return this.deletedRegistrationHashes.has(params[0]) ? [{ request_hash: params[0] }] : [];
    }
    if (sql.startsWith("select profile_id, registration_request_id from leaderboard_profiles_v2")) {
      return [...this.profiles.values()]
        .filter(row => row.profile_id === params[0] || row.registration_request_id === params[1] || row.token_hash === params[2])
        .slice(0, 1)
        .map(row => ({ profile_id: row.profile_id, registration_request_id: row.registration_request_id }));
    }
    if (sql.startsWith("select registration_request_id from leaderboard_profiles_v2")) {
      const row = this.profiles.get(params[0]);
      return row ? [{ registration_request_id: row.registration_request_id }] : [];
    }
    if (sql.startsWith("insert into leaderboard_profiles_v2")) {
      const [profileId, tokenHash, registrationRequestId, createdAt, lastSeenAt] = params;
      this.profiles.set(profileId, { profile_id: profileId, token_hash: tokenHash, registration_request_id: registrationRequestId, created_at: createdAt, last_seen_at: lastSeenAt });
      return [{ rowsWritten: 1 }];
    }
    if (sql.startsWith("update leaderboard_profiles_v2")) {
      const [tokenHash, lastSeenAt, profileId] = params;
      const row = this.profiles.get(profileId);
      if (row) Object.assign(row, { token_hash: tokenHash, last_seen_at: lastSeenAt });
      return [{ rowsWritten: row ? 1 : 0 }];
    }
    if (sql.startsWith("insert or ignore into deleted_leaderboard_profiles")) {
      this.deletedProfiles.add(params[0]);
      return [];
    }
    if (sql.startsWith("insert or ignore into deleted_leaderboard_registration_requests")) {
      this.deletedRegistrationHashes.add(params[0]);
      return [];
    }
    if (sql.startsWith("delete from leaderboard_profiles_v2")) {
      this.profiles.delete(params[0]);
      return [];
    }
    return [];
  }
}

test("a deleted Friend identity and its registration key cannot recreate the identity", async () => {
  const sql = new FriendSql();
  const env = {
    LUDOPROOF_SESSION_HMAC_KEY: SECRET,
    LUDOPROOF_FRIEND_CONVERSATIONS: namespace(),
    LUDOPROOF_FRIEND_RECENT: namespace(),
  };
  const directory = new FriendDirectory(makeContext(sql), env);
  const createdResponse = await directory.fetch(request("/register", { clientRequestId: REQUEST_ID, displayName: "Player One" }));
  assert.equal(createdResponse.status, 200);
  const created = await createdResponse.json();

  const deletedResponse = await directory.fetch(request("/account/delete", { friendId: created.friendId }));
  assert.equal(deletedResponse.status, 200);
  assert.equal((await deletedResponse.json()).deleted, true);
  assert.equal(sql.profiles.has(created.friendId), false);
  assert.equal(sql.deletedProfiles.has(created.friendId), true);

  const replay = await directory.fetch(request("/register", { clientRequestId: REQUEST_ID, displayName: "Player One" }));
  assert.equal(replay.status, 410);
  assert.equal((await replay.json()).error, "FRIEND_IDENTITY_DELETED");
});

test("a deleted leaderboard profile and its registration key cannot recreate the profile", async () => {
  const sql = new LeaderboardSql();
  const env = { LUDOPROOF_SESSION_HMAC_KEY: SECRET };
  const leaderboard = new LeaderboardRoom(makeContext(sql), env);
  const createdResponse = await leaderboard.fetch(request("/profile/register", { clientRequestId: REQUEST_ID }));
  assert.equal(createdResponse.status, 200);
  const created = await createdResponse.json();

  const deletedResponse = await leaderboard.fetch(request("/profile/delete", { profileId: created.profileId }));
  assert.equal(deletedResponse.status, 200);
  assert.equal((await deletedResponse.json()).deleted, true);
  assert.equal(sql.profiles.has(created.profileId), false);
  assert.equal(sql.deletedProfiles.has(created.profileId), true);

  const replay = await leaderboard.fetch(request("/profile/register", { clientRequestId: REQUEST_ID }));
  assert.equal(replay.status, 410);
  assert.equal((await replay.json()).error, "LEADERBOARD_PROFILE_DELETED");
});
