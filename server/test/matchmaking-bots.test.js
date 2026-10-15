import test from "node:test";
import assert from "node:assert/strict";

import { MatchmakerQueue } from "../src/matchmaker-queue.js";
import { TeamMatchmakerQueue } from "../src/team-matchmaker-queue.js";
import { MatchRoom } from "../src/match-room-bots.js";
import {
  botFillDelayMs,
  botMatchFillEnabled,
  botTurnsEnabled,
  createBotParticipants,
} from "../src/matchmaking-bots.js";

const SESSION_KEY = "lp_test_session_hmac_key_1234567890abcdef";

class FakeStorage {
  constructor() {
    this.values = new Map();
    this.alarmAt = null;
  }

  async get(key) {
    const value = this.values.get(key);
    return value === undefined ? undefined : structuredClone(value);
  }

  async put(key, value) {
    this.values.set(key, structuredClone(value));
  }

  async setAlarm(timestamp) {
    this.alarmAt = timestamp;
  }

  async deleteAlarm() {
    this.alarmAt = null;
  }

  async deleteAll() {
    this.values.clear();
    this.alarmAt = null;
  }
}

class MatchNamespace {
  constructor(env) {
    this.env = env;
    this.rooms = new Map();
  }

  idFromName(name) {
    return name;
  }

  get(id) {
    if (!this.rooms.has(id)) {
      this.rooms.set(
        id,
        new MatchRoom({ storage: new FakeStorage() }, this.env),
      );
    }
    return this.rooms.get(id);
  }
}

function profileTokenFor(clientRequestId) {
  const suffix = String(clientRequestId ?? "")
    .replaceAll("-", "")
    .slice(-12)
    .padStart(12, "0");
  return "lpp_" + "A".repeat(32) + suffix;
}

function makeEnv() {
  const env = {
    LUDOPROOF_SESSION_HMAC_KEY: SESSION_KEY,
    LUDOPROOF_BOT_MATCH_FILL_ENABLED: "true",
    LUDOPROOF_BOT_TURNS_ENABLED: "false",
    LUDOPROOF_BOT_FILL_DELAY_MS: "250",
    LUDOPROOF_BOT_TURN_DELAY_MS: "100",
  };
  env.LUDOPROOF_MATCHES = new MatchNamespace(env);
  env.LUDOPROOF_LEADERBOARD = {
    idFromName(name) {
      assert.equal(name, "global");
      return name;
    },
    get() {
      return {
        async fetch(request) {
          const authorization = request.headers.get("authorization") ?? "";
          const match = authorization.match(/^Bearer\s+(lpp_[A-Za-z0-9_-]{32,})$/);
          if (!match) {
            return Response.json({ ok: false, error: "PROFILE_AUTH_INVALID" }, { status: 401 });
          }
          const suffix = match[1].slice(-12);
          return Response.json({
            ok: true,
            profileId: "10000000-0000-4000-8000-" + suffix,
          });
        },
      };
    },
  };
  return env;
}

function request(path, body, profileToken = null) {
  const headers = { "content-type": "application/json" };
  if (profileToken) headers.authorization = "Bearer " + profileToken;
  return new Request("https://matchmaker" + path, {
    method: "POST",
    headers,
    body: JSON.stringify(body),
  });
}

function human(suffix, playerCount, team = false) {
  const clientRequestId =
    "00000000-0000-4000-8000-" + String(suffix).padStart(12, "0");
  return {
    clientRequestId,
    displayName: "Player " + suffix,
    ...(team ? {} : { playerCount }),
    characterId: ["dog", "cat", "goat", "duck"][suffix % 4],
    profileToken: profileTokenFor(clientRequestId),
  };
}

async function parse(response) {
  return { status: response.status, body: await response.json() };
}

async function backdateQueue(queue, storageKey, queueIndex = 0) {
  const state = await queue.ctx.storage.get(storageKey);
  state.queue[queueIndex].joinedAt = Date.now() - 10_000;
  state.queue[queueIndex].lastSeenAt = Date.now();
  await queue.ctx.storage.put(storageKey, state);
}

test("bot settings are bounded and bots receive non-profile identities", () => {
  assert.equal(botMatchFillEnabled({ LUDOPROOF_BOT_MATCH_FILL_ENABLED: "false" }), false);
  assert.equal(botTurnsEnabled({ LUDOPROOF_BOT_TURNS_ENABLED: "off" }), false);
  assert.equal(botFillDelayMs({ LUDOPROOF_BOT_FILL_DELAY_MS: "10" }), 15_000);
  const bots = createBotParticipants({
    count: 3,
    playerCount: 4,
    matchMode: "ONLINE",
    existingNames: ["Aarav"],
    now: 100,
  });
  assert.equal(bots.length, 3);
  assert.ok(bots.every((bot) => bot.playerType === "BOT" && bot.profileId === null));
  assert.ok(bots.every((bot) => /^[0-9a-f-]{36}$/i.test(bot.clientRequestId)));
  assert.equal(new Set(bots.map((bot) => bot.displayName.toLowerCase())).size, 3);
  assert.ok(bots.every((bot) => bot.displayName.toLowerCase() !== "aarav"));
});

test("Online 4-player fallback adds only the missing bots and assigns no bot client tickets", async () => {
  const env = makeEnv();
  const queue = new MatchmakerQueue({ storage: new FakeStorage() }, env);
  const first = human(1, 4);
  const second = human(2, 4);

  for (const person of [first, second]) {
    const result = await parse(await queue.fetch(
      request("/search", {
        clientRequestId: person.clientRequestId,
        displayName: person.displayName,
        playerCount: 4,
        characterId: person.characterId,
      }, person.profileToken),
    ));
    assert.equal(result.body.status, "SEARCHING");
  }

  await backdateQueue(queue, "matchmaking-state");
  await queue.alarm();

  const result = await parse(await queue.fetch(
    request("/status", {
      clientRequestId: first.clientRequestId,
      playerCount: 4,
    }, first.profileToken),
  ));
  assert.equal(result.body.status, "MATCHED");
  assert.equal(result.body.state.status, "ACTIVE");
  assert.equal(result.body.state.players.length, 4);
  assert.deepEqual(
    result.body.state.players.map((player) => player.playerType),
    ["HUMAN", "HUMAN", "BOT", "BOT"],
  );
  assert.ok(result.body.state.players.slice(2).every((player) => player.displayName.length >= 2));
  const stored = await env.LUDOPROOF_MATCHES.rooms
    .get(result.body.matchId).ctx.storage.get("match-state");
  assert.ok(stored.players.slice(2).every((player) => player.profileId == null));
  const queueState = await queue.ctx.storage.get("matchmaking-state");
  assert.deepEqual(Object.keys(queueState.assignments).sort(), [
    first.clientRequestId,
    second.clientRequestId,
  ].sort());
});

test("Team Up fallback fills remaining seats while retaining A/B/A/B teams", async () => {
  const env = makeEnv();
  const queue = new TeamMatchmakerQueue({ storage: new FakeStorage() }, env);
  const people = [human(11, 4, true), human(12, 4, true), human(13, 4, true)];

  for (const person of people) {
    const result = await parse(await queue.fetch(
      request("/search", {
        clientRequestId: person.clientRequestId,
        displayName: person.displayName,
        characterId: person.characterId,
      }, person.profileToken),
    ));
    assert.equal(result.body.status, "SEARCHING");
  }

  await backdateQueue(queue, "team-matchmaking-state-v1");
  await queue.alarm();

  const result = await parse(await queue.fetch(
    request("/status", {
      clientRequestId: people[0].clientRequestId,
    }, people[0].profileToken),
  ));
  assert.equal(result.body.status, "MATCHED");
  assert.equal(result.body.matchMode, "TEAM_UP");
  assert.equal(result.body.state.players.length, 4);
  assert.deepEqual(result.body.state.players.map((player) => player.teamId), ["A", "B", "A", "B"]);
  assert.deepEqual(result.body.state.players.map((player) => player.playerType), ["HUMAN", "HUMAN", "HUMAN", "BOT"]);
  assert.equal(result.body.teamId, "A");
  const stored = await env.LUDOPROOF_MATCHES.rooms
    .get(result.body.matchId).ctx.storage.get("match-state");
  assert.equal(stored.players[3].profileId, null);
  assert.equal(stored.players[3].playerType, "BOT");
});
