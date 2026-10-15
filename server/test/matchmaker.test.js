import test from "node:test";
import assert from "node:assert/strict";

import {
  MatchmakerQueue,
} from "../src/matchmaker-queue.js";
import {
  MatchRoom,
} from "../src/match-room.js";

const SESSION_KEY =
  "lp_test_session_hmac_key_1234567890abcdef";

class FakeStorage {
  constructor() {
    this.values =
      new Map();
    this.alarmAt =
      null;
  }

  async get(key) {
    const value =
      this.values.get(
        key,
      );
    return value ===
      undefined
      ? undefined
      : structuredClone(
          value,
        );
  }

  async put(
    key,
    value,
  ) {
    this.values.set(
      key,
      structuredClone(
        value,
      ),
    );
  }

  async setAlarm(
    timestamp,
  ) {
    this.alarmAt =
      timestamp;
  }
}

class MatchNamespace {
  constructor(env) {
    this.env =
      env;
    this.rooms =
      new Map();
  }

  idFromName(name) {
    return name;
  }

  get(id) {
    if (
      !this.rooms.has(
        id,
      )
    ) {
      this.rooms.set(
        id,
        new MatchRoom(
          {
            storage:
              new FakeStorage(),
          },
          this.env,
        ),
      );
    }
    return this.rooms.get(
      id,
    );
  }
}

function makeQueue() {
  const env = {
    LUDOPROOF_SESSION_HMAC_KEY:
      SESSION_KEY,
  };
  env.LUDOPROOF_MATCHES =
    new MatchNamespace(
      env,
    );
  env.LUDOPROOF_LEADERBOARD = {
    idFromName(name) {
      assert.equal(
        name,
        "global",
      );
      return name;
    },
    get() {
      return {
        async fetch(request) {
          const authorization =
            request.headers.get(
              "authorization",
            ) ?? "";
          const match =
            authorization.match(
              /^Bearer\s+(lpp_[A-Za-z0-9_-]{32,})$/,
            );
          if (
            !match
          ) {
            return Response.json(
              {
                error:
                  "PROFILE_AUTH_INVALID",
                message:
                  "leaderboard profile credential is invalid",
              },
              {
                status:
                  401,
              },
            );
          }

          const suffix =
            match[1]
              .slice(
                -12,
              );
          return Response.json({
            ok: true,
            profileId:
              "10000000-0000-4000-8000-" +
              suffix,
          });
        },
      };
    },
  };

  return new MatchmakerQueue(
    {
      storage:
        new FakeStorage(),
    },
    env,
  );
}

function request(
  path,
  body,
  profileToken =
    profileTokenFor(
      body
        ?.clientRequestId,
    ),
) {
  const headers = {
    "content-type":
      "application/json",
  };
  if (
    profileToken
  ) {
    headers.authorization =
      "Bearer " +
      profileToken;
  }

  return new Request(
    "https://matchmaker" +
      path,
    {
      method:
        "POST",
      headers,
      body:
        JSON.stringify(
          body,
        ),
    },
  );
}

function profileTokenFor(
  clientRequestId,
) {
  const suffix =
    String(
      clientRequestId ??
        "",
    )
      .replaceAll(
        "-",
        "",
      )
      .slice(
        -12,
      )
      .padStart(
        12,
        "0",
      );
  return (
    "lpp_" +
    "A".repeat(32) +
    suffix
  );
}

async function read(
  response,
) {
  return {
    status:
      response.status,
    body:
      await response.json(),
  };
}

function player(
  suffix,
  playerCount,
  characterId = "dog",
) {
  return {
    displayName:
      "Player " +
      suffix,
    clientRequestId:
      "00000000-0000-4000-8000-" +
      String(
        suffix,
      ).padStart(
        12,
        "0",
      ),
    playerCount,
    characterId,
  };
}

test(
  "2P public queue creates and auto-starts one authoritative match",
  async () => {
    const queue =
      makeQueue();
    const first =
      player(
        1,
        2,
        "dog",
      );
    const second =
      player(
        2,
        2,
        "goat",
      );

    const waiting =
      await read(
        await queue.fetch(
          request(
            "/search",
            first,
          ),
        ),
      );
    assert.equal(
      waiting.status,
      202,
      JSON.stringify(
        waiting.body,
      ),
    );
    assert.equal(
      waiting.body.status,
      "SEARCHING",
    );
    assert.equal(
      waiting.body.queuedPlayers,
      1,
    );

    const matchedSecond =
      await read(
        await queue.fetch(
          request(
            "/search",
            second,
          ),
        ),
      );
    assert.equal(
      matchedSecond.status,
      200,
    );
    assert.equal(
      matchedSecond.body.status,
      "MATCHED",
    );
    assert.equal(
      matchedSecond.body.state
        .status,
      "ACTIVE",
    );
    assert.equal(
      matchedSecond.body.state
        .players.length,
      2,
    );

    const matchedFirst =
      await read(
        await queue.fetch(
          request(
            "/status",
            {
              clientRequestId:
                first.clientRequestId,
              playerCount:
                2,
            },
          ),
        ),
      );

    assert.equal(
      matchedFirst.body.status,
      "MATCHED",
    );
    assert.equal(
      matchedFirst.body.matchId,
      matchedSecond.body.matchId,
    );
    assert.notEqual(
      matchedFirst.body.playerToken,
      matchedSecond.body.playerToken,
    );
    assert.equal(
      matchedFirst.body.state
        .status,
      "ACTIVE",
    );
  },
);

test(
  "4P queue waits for all four real players before starting",
  async () => {
    const queue =
      makeQueue();

    for (
      let index = 1;
      index <= 3;
      index += 1
    ) {
      const result =
        await read(
          await queue.fetch(
            request(
              "/search",
              player(
                index,
                4,
                ["dog", "goat", "duck", "cat"][index - 1],
              ),
            ),
          ),
        );
      assert.equal(
        result.body.status,
        "SEARCHING",
      );
      assert.equal(
        result.body.queuedPlayers,
        index,
      );
    }

    const fourth =
      await read(
        await queue.fetch(
          request(
            "/search",
            player(
              4,
              4,
              "cat",
            ),
          ),
        ),
      );

    assert.equal(
      fourth.body.status,
      "MATCHED",
    );
    assert.equal(
      fourth.body.state
        .status,
      "ACTIVE",
    );
    assert.equal(
      fourth.body.state
        .players.length,
      4,
    );
  },
);

test(
  "cancel removes a queued player but never discards an assigned match",
  async () => {
    const queue =
      makeQueue();
    const first =
      player(
        1,
        2,
        "dog",
      );
    const second =
      player(
        2,
        2,
        "goat",
      );

    await queue.fetch(
      request(
        "/search",
        first,
      ),
    );

    const cancelled =
      await read(
        await queue.fetch(
          request(
            "/cancel",
            {
              clientRequestId:
                first.clientRequestId,
              playerCount:
                2,
            },
          ),
        ),
      );
    assert.equal(
      cancelled.body.status,
      "CANCELLED",
    );
    assert.equal(
      cancelled.body.cancelled,
      true,
    );

    await queue.fetch(
      request(
        "/search",
        first,
      ),
    );
    await queue.fetch(
      request(
        "/search",
        second,
      ),
    );

    const raced =
      await read(
        await queue.fetch(
          request(
            "/cancel",
            {
              clientRequestId:
                first.clientRequestId,
              playerCount:
                2,
            },
          ),
        ),
      );

    assert.equal(
      raced.body.status,
      "MATCHED",
    );
    assert.equal(
      raced.body.cancelled,
      false,
    );
    assert.match(
      raced.body.matchId,
      /^LP[A-Z2-9]{8}$/,
    );
  },
);


test(
  "legacy public matchmaking remains playable but unranked",
  async () => {
    const queue =
      makeQueue();
    const first =
      player(
        21,
        2,
      );
    const second =
      player(
        22,
        2,
        "goat",
      );

    const waiting =
      await read(
        await queue.fetch(
          request(
            "/search",
            first,
            null,
          ),
        ),
      );
    assert.equal(
      waiting.status,
      202,
      JSON.stringify(
        waiting.body,
      ),
    );
    assert.equal(
      waiting.body.status,
      "SEARCHING",
    );

    const matched =
      await read(
        await queue.fetch(
          request(
            "/search",
            second,
            null,
          ),
        ),
      );
    assert.equal(
      matched.status,
      200,
      JSON.stringify(
        matched.body,
      ),
    );
    assert.equal(
      matched.body.status,
      "MATCHED",
    );
    assert.equal(
      matched.body.state
        .status,
      "ACTIVE",
    );

    const room =
      queue.env
        .LUDOPROOF_MATCHES
        .rooms.get(
          matched.body
            .matchId,
        );
    const stored =
      await room.ctx.storage.get(
        "match-state",
      );
    assert.equal(
      stored.players.length,
      2,
    );
    assert.ok(
      stored.players.every(
        (candidate) =>
          candidate.profileId ==
          null,
      ),
      "legacy matchmaking seats must not receive leaderboard profiles",
    );
  },
);

test(
  "matchmaking request IDs cannot be replayed by another authenticated profile",
  async () => {
    const queue =
      makeQueue();
    const first =
      player(
        7,
        2,
      );

    const waiting =
      await read(
        await queue.fetch(
          request(
            "/search",
            first,
          ),
        ),
      );
    assert.equal(
      waiting.status,
      202,
    );

    const attackerToken =
      "lpp_" +
      "B".repeat(32) +
      "000000000999";
    const replay =
      await read(
        await queue.fetch(
          request(
            "/search",
            first,
            attackerToken,
          ),
        ),
      );

    assert.equal(
      replay.status,
      403,
    );
    assert.equal(
      replay.body.error,
      "MATCHMAKING_PROFILE_MISMATCH",
    );
  },
);

test(
  "public status reports a reserved room as FINALIZING until assignment is materialized",
  async () => {
    const queue = makeQueue();
    const first = player(1, 2, "dog");
    const now = Date.now();
    const profileId = "10000000-0000-4000-8000-000000000001";
    const human = {
      ...first,
      profileId,
      playerType: "HUMAN",
      rulesetKey: "CLASSIC_V3",
      joinedAt: now,
      lastSeenAt: now,
    };
    const bot = {
      clientRequestId: "bot-pending-000000000001",
      displayName: "Bot Seat",
      characterId: "goat",
      profileId: null,
      playerType: "BOT",
      playerCount: 2,
      matchMode: "ONLINE",
      rulesetKey: "CLASSIC_V3",
      joinedAt: now,
      lastSeenAt: now,
    };
    await queue.ctx.storage.put("matchmaking-state", {
      schemaVersion: 1,
      queue: [human],
      assignments: {},
      characterPreferences: {},
      pendingMatch: {
        matchMode: "ONLINE",
        playerCount: 2,
        rulesetKey: "CLASSIC_V3",
        participants: [human, bot],
        retryAt: now + 3_000,
      },
    });

    const result = await read(
      await queue.fetch(
        request("/status", {
          clientRequestId: first.clientRequestId,
          playerCount: 2,
        }),
      ),
    );

    assert.equal(result.status, 200);
    assert.equal(result.body.status, "FINALIZING");
    assert.equal(result.body.pending, true);
    assert.equal(result.body.targetPlayerCount, 2);
    assert.equal(result.body.queuedPlayers, 2);
    assert.equal(result.body.retryAt, now + 3_000);
    assert.equal(result.body.matchId, undefined);
    assert.equal(result.body.playerToken, undefined);
  },
);

