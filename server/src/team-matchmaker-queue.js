import {
  deterministicMatchId,
  httpError,
  issueLeaderboardProfileAssertion,
  normalizeDisplayName,
  requireClientRequestId,
} from "./crypto.js";
import {
  normalizeLeaderboardProfileId,
} from "./leaderboard-core.js";
import {
  normalizeCharacterId,
} from "./ludo-paws-characters.js";
import {
  botFillDelayMs,
  botMatchFillEnabled,
  botRetryDelayMs,
  createBotParticipants,
} from "./matchmaking-bots.js";

const STATE_KEY = "team-matchmaking-state-v1";
const QUEUE_TTL_MS = 2 * 60 * 1000;
const ASSIGNMENT_TTL_MS = 10 * 60 * 1000;
const PROFILE_ASSERTION_TTL_MS = 15 * 60 * 1000;
const TARGET_PLAYER_COUNT = 4;

export class TeamMatchmakerQueue {
  constructor(ctx, env) {
    this.ctx = ctx;
    this.env = env;
    this.mutationTail = Promise.resolve();
  }

  async fetch(request) {
    try {
      const url = new URL(request.url);
      const body = request.method === "POST" ? await readJson(request) : null;
      if (request.method === "POST" && url.pathname === "/search") {
        return await this.#mutate(() => this.#search(request, body));
      }
      if (request.method === "POST" && url.pathname === "/status") {
        return await this.#mutate(() => this.#status(request, body));
      }
      if (request.method === "POST" && url.pathname === "/cancel") {
        return await this.#mutate(() => this.#cancel(request, body));
      }
      return json(404, { error: "NOT_FOUND", message: "route not found" });
    } catch (error) {
      return errorResponse(error);
    }
  }

  async alarm() {
    return this.#mutate(async () => {
      const now = Date.now();
      let state = this.#cleanup(await this.#load(), now);

      if (state.pendingMatch) {
        if (Number(state.pendingMatch.retryAt ?? 0) > now) {
          await this.#persist(state);
          return;
        }
        const completed = await this.#resumePendingMatch(state, now);
        if (!completed) return;
      }

      if (botMatchFillEnabled(this.env)) {
        state = this.#cleanup(await this.#load(), Date.now());
        if (!state.pendingMatch) {
          const humans = [...state.queue].sort((left, right) => left.joinedAt - right.joinedAt);
          const oldest = humans[0];
          const fallbackAt = oldest
            ? Number(oldest.botFallbackAt ?? oldest.joinedAt + botFillDelayMs(this.env))
            : Number.POSITIVE_INFINITY;

          if (oldest && fallbackAt <= now) {
            const realPlayers = humans.slice(0, TARGET_PLAYER_COUNT);
            const bots = await createBotParticipants({
              count: TARGET_PLAYER_COUNT - realPlayers.length,
              playerCount: TARGET_PLAYER_COUNT,
              matchMode: "TEAM_UP",
              existingNames: realPlayers.map((candidate) => candidate.displayName),
              now,
            });
            state.pendingMatch = {
              matchMode: "TEAM_UP",
              playerCount: TARGET_PLAYER_COUNT,
              participants: [...realPlayers, ...bots],
              retryAt: now + botRetryDelayMs(this.env),
            };
            await this.#persist(state);
            const completed = await this.#resumePendingMatch(state, now);
            if (!completed) return;
          }
        }
      }

      state = this.#cleanup(await this.#load(), Date.now());
      await this.#persist(state);
    });
  }

  #mutate(work) {
    const run = this.mutationTail.then(work, work);
    this.mutationTail = run.catch(() => {});
    return run;
  }

  async #search(request, body) {
    const profileId = await this.#profileIdentity(request);
    const participant = normalizeParticipant(body, profileId);
    const now = Date.now();
    let state = this.#cleanup(await this.#load(), now);

    const assigned = state.assignments[participant.clientRequestId];
    if (assigned) {
      return this.#matchedResponse(
        {
          ...participant,
          characterId: normalizeCharacterId(assigned.characterId ?? participant.characterId),
        },
        assigned,
      );
    }

    const existingIndex = state.queue.findIndex(
      (candidate) => candidate.clientRequestId === participant.clientRequestId,
    );
    if (existingIndex >= 0) {
      const existing = state.queue[existingIndex];
      if (existing.profileId !== participant.profileId) {
        throw httpError(
          403,
          "TEAM_MATCHMAKING_PROFILE_MISMATCH",
          "team matchmaking request belongs to another authenticated profile",
        );
      }
      state.queue[existingIndex] = {
        ...existing,
        displayName: participant.displayName,
        characterId: participant.characterId,
        lastSeenAt: now,
      };
      await this.#persist(state);
      return json(200, searchingPayload(state, now, participant.clientRequestId));
    }

    if (
      state.queue.some((candidate) => candidate.profileId === participant.profileId) ||
      Object.values(state.assignments).some(
        (assignment) => assignment.profileId === participant.profileId,
      )
    ) {
      throw httpError(
        409,
        "PROFILE_ALREADY_TEAM_QUEUED",
        "this profile is already searching for a team match",
      );
    }

    state.queue.push({
      ...participant,
      joinedAt: now,
      lastSeenAt: now,
    });

    const eligible = [...state.queue].sort(
      (left, right) => left.joinedAt - right.joinedAt,
    );

    if (eligible.length >= TARGET_PLAYER_COUNT && !state.pendingMatch) {
      const group = eligible.slice(0, TARGET_PLAYER_COUNT);
      state.pendingMatch = {
        matchMode: "TEAM_UP",
        playerCount: TARGET_PLAYER_COUNT,
        participants: group,
        retryAt: now + botRetryDelayMs(this.env),
      };
      await this.#persist(state);
      const completed = await this.#resumePendingMatch(state, now);
      if (!completed) {
        throw httpError(
          503,
          "TEAM_MATCHMAKER_RECOVERY_PENDING",
          "team match setup is being retried; keep searching",
        );
      }

      const ownAssignment = state.assignments[participant.clientRequestId];
      if (!ownAssignment) {
        throw httpError(
          503,
          "TEAM_MATCHMAKER_ASSIGNMENT_MISSING",
          "team match was created but the player assignment is not available yet",
        );
      }
      return this.#matchedResponse(
        {
          ...participant,
          characterId: normalizeCharacterId(ownAssignment.characterId),
        },
        ownAssignment,
      );
    }

    await this.#persist(state);
    return json(202, searchingPayload(state, now, participant.clientRequestId));
  }

  async #status(request, body) {
    const profileId = await this.#profileIdentity(request);
    const clientRequestId = requireClientRequestId(body?.clientRequestId);
    const now = Date.now();
    const state = this.#cleanup(await this.#load(), now);

    const assignment = state.assignments[clientRequestId];
    if (assignment) {
      assertOwner(assignment, profileId);
      await this.#persist(state);
      return this.#matchedResponse(
        {
          clientRequestId,
          displayName: assignment.displayName,
          characterId: normalizeCharacterId(assignment.characterId),
          profileId,
        },
        assignment,
      );
    }

    const reserved = state.pendingMatch?.participants.some(
      (candidate) =>
        candidate.playerType !== "BOT" &&
        candidate.clientRequestId === clientRequestId,
    );
    if (reserved) {
      await this.#persist(state);
      return json(200, {
        status: "MATCHED",
        matchMode: "TEAM_UP",
        cancelled: false,
        pending: true,
        targetPlayerCount: TARGET_PLAYER_COUNT,
      });
    }

    const queued = state.queue.find(
      (candidate) => candidate.clientRequestId === clientRequestId,
    );
    if (!queued) {
      await this.#persist(state);
      return json(200, {
        status: "IDLE",
        matchMode: "TEAM_UP",
        playerCount: TARGET_PLAYER_COUNT,
        queuedPlayers: 0,
        targetPlayerCount: TARGET_PLAYER_COUNT,
      });
    }
    if (queued.profileId !== profileId) {
      throw httpError(
        403,
        "TEAM_MATCHMAKING_PROFILE_MISMATCH",
        "team matchmaking request belongs to another authenticated profile",
      );
    }
    queued.lastSeenAt = now;
    await this.#persist(state);
    return json(200, searchingPayload(state, now, clientRequestId));
  }

  async #cancel(request, body) {
    const profileId = await this.#profileIdentity(request);
    const clientRequestId = requireClientRequestId(body?.clientRequestId);
    const now = Date.now();
    const state = this.#cleanup(await this.#load(), now);

    const assignment = state.assignments[clientRequestId];
    if (assignment) {
      assertOwner(assignment, profileId);
      await this.#persist(state);
      return json(200, {
        status: "MATCHED",
        matchMode: "TEAM_UP",
        cancelled: false,
        targetPlayerCount: TARGET_PLAYER_COUNT,
        matchId: assignment.matchId,
      });
    }

    const queued = state.queue.find(
      (candidate) => candidate.clientRequestId === clientRequestId,
    );
    if (queued && queued.profileId !== profileId) {
      throw httpError(
        403,
        "TEAM_MATCHMAKING_PROFILE_MISMATCH",
        "team matchmaking request belongs to another authenticated profile",
      );
    }

    const before = state.queue.length;
    state.queue = state.queue.filter(
      (candidate) => candidate.clientRequestId !== clientRequestId,
    );
    await this.#persist(state);
    return json(200, {
      status: "CANCELLED",
      matchMode: "TEAM_UP",
      cancelled: state.queue.length < before,
      targetPlayerCount: TARGET_PLAYER_COUNT,
    });
  }

  async #materialize(participants) {
    const host = participants[0];
    for (let attempt = 0; attempt < 4; attempt += 1) {
      // ONLINE public matchmaking reserves attempts 0..3. Team Up uses 4..7,
      // remaining inside deterministicMatchId's reviewed attempt bounds.
      const matchId = await deterministicMatchId(
        host.clientRequestId,
        4 + attempt,
      );
      const target = room(this.env, matchId);
      const createResponse = await target.fetch(
        new Request("https://room/create", {
          method: "POST",
          headers: {
            "content-type": "application/json",
            "x-ludoproof-profile-assertion": await issueAssertion(
              this.env,
              matchId,
              host,
            ),
          },
          body: JSON.stringify({
            matchId,
            displayName: host.displayName,
            characterId: normalizeCharacterId(host.characterId),
            clientRequestId: host.clientRequestId,
            matchmaking: true,
            targetPlayerCount: TARGET_PLAYER_COUNT,
            matchMode: "TEAM_UP",
          }),
        }),
      );
      if (createResponse.status === 409) continue;
      const created = await requireJsonResponse(createResponse);

      for (let index = 1; index < participants.length; index += 1) {
        const participant = participants[index];
        const joinResponse = await target.fetch(
          new Request("https://room/join", {
            method: "POST",
            headers: {
              "content-type": "application/json",
              "x-ludoproof-profile-assertion": await issueAssertion(
                this.env,
                matchId,
                participant,
              ),
            },
            body: JSON.stringify({
              displayName: participant.displayName,
              characterId: normalizeCharacterId(participant.characterId),
              clientRequestId: participant.clientRequestId,
            }),
          }),
        );
        await requireJsonResponse(joinResponse);
      }

      const startResponse = await target.fetch(
        new Request("https://room/start", {
          method: "POST",
          headers: {
            "content-type": "application/json",
            authorization: "Bearer " + created.playerToken,
          },
          body: JSON.stringify({}),
        }),
      );
      await requireJsonResponse(startResponse);
      return { matchId };
    }

    throw httpError(
      503,
      "TEAM_MATCH_ID_EXHAUSTED",
      "could not allocate a team match",
    );
  }

  async #resumePendingMatch(state, now) {
    const pending = state.pendingMatch;
    if (!pending) return true;

    let assignment;
    try {
      assignment = await this.#materialize(pending.participants);
    } catch (error) {
      pending.retryAt = Date.now() + botRetryDelayMs(this.env);
      state.pendingMatch = pending;
      await this.#persist(state);
      console.error("Team Up reserved match could not be materialized; retry scheduled", {
        code: error?.code ?? "TEAM_MATCH_MATERIALIZE_FAILED",
      });
      return false;
    }

      const realParticipants = pending.participants.filter(
        (candidate) => candidate.playerType !== "BOT",
      );
      const realRequestIds = new Set(
        realParticipants.map((candidate) => candidate.clientRequestId),
      );
      state.queue = state.queue.filter(
        (candidate) => !realRequestIds.has(candidate.clientRequestId),
      );

      const assignedAt = Date.now();
      pending.participants.forEach((candidate, index) => {
        if (candidate.playerType === "BOT") return;
        state.assignments[candidate.clientRequestId] = {
          matchId: assignment.matchId,
          role: index === 0 ? "HOST" : "GUEST",
          seat: index,
          teamId: index % 2 === 0 ? "A" : "B",
          assignedAt,
          expiresAt: assignedAt + ASSIGNMENT_TTL_MS,
          displayName: candidate.displayName,
          characterId: normalizeCharacterId(candidate.characterId),
          profileId: candidate.profileId,
        };
      });
      delete state.pendingMatch;
      await this.#persist(state);
      return true;
  }

  async #matchedResponse(participant, assignment) {
    assertOwner(assignment, participant.profileId);
    const target = room(this.env, assignment.matchId);
    const host = assignment.role === "HOST";
    const body = {
      displayName: participant.displayName,
      characterId: normalizeCharacterId(
        assignment.characterId ?? participant.characterId,
      ),
      clientRequestId: participant.clientRequestId,
      ...(host
        ? {
            matchId: assignment.matchId,
            matchmaking: true,
            targetPlayerCount: TARGET_PLAYER_COUNT,
            matchMode: "TEAM_UP",
          }
        : {}),
    };
    const response = await target.fetch(
      new Request("https://room/" + (host ? "create" : "join"), {
        method: "POST",
        headers: {
          "content-type": "application/json",
          "x-ludoproof-profile-assertion": await issueAssertion(
            this.env,
            assignment.matchId,
            participant,
          ),
        },
        body: JSON.stringify(body),
      }),
    );
    const session = await requireJsonResponse(response);
    return json(200, {
      status: "MATCHED",
      matchMode: "TEAM_UP",
      targetPlayerCount: TARGET_PLAYER_COUNT,
      matchId: assignment.matchId,
      seat: assignment.seat,
      teamId: assignment.teamId,
      playerId: session.playerId,
      playerToken: session.playerToken,
      state: session.state,
    });
  }

  async #profileIdentity(request) {
    const authorization = request.headers.get("authorization");
    if (
      typeof authorization !== "string" ||
      !/^Bearer\s+lpp_[A-Za-z0-9_-]{32,}$/i.test(authorization)
    ) {
      throw httpError(
        401,
        "PROFILE_AUTH_REQUIRED",
        "authenticated leaderboard profile is required for Team Up",
      );
    }
    if (!this.env.LUDOPROOF_LEADERBOARD) {
      throw httpError(
        503,
        "LEADERBOARD_NOT_CONFIGURED",
        "leaderboard storage is not configured",
      );
    }
    const id = this.env.LUDOPROOF_LEADERBOARD.idFromName("global");
    const target = this.env.LUDOPROOF_LEADERBOARD.get(id);
    const response = await target.fetch(
      new Request("https://leaderboard/profile/identity", {
        method: "GET",
        headers: { authorization },
      }),
    );
    let value;
    try {
      value = await response.json();
    } catch {
      throw httpError(
        503,
        "PROFILE_AUTH_BAD_RESPONSE",
        "leaderboard profile authorization returned invalid data",
      );
    }
    const profileId = normalizeLeaderboardProfileId(value?.profileId);
    if (!response.ok || value?.ok !== true || profileId == null) {
      throw httpError(
        response.status === 401 ? 401 : 403,
        value?.error ?? "PROFILE_AUTH_INVALID",
        value?.message ?? "leaderboard profile credential is invalid",
      );
    }
    return profileId;
  }

  async #load() {
    return (
      (await this.ctx.storage.get(STATE_KEY)) ?? {
        schemaVersion: 1,
        queue: [],
        assignments: {},
      }
    );
  }

  #cleanup(state, now) {
    const next = structuredClone(state);
    next.queue = (next.queue ?? []).filter(
      (participant) =>
        Number(participant.lastSeenAt ?? participant.joinedAt ?? 0) + QUEUE_TTL_MS > now,
    );
    next.assignments = next.assignments ?? {};
    for (const [requestId, assignment] of Object.entries(next.assignments)) {
      if (Number(assignment.expiresAt ?? 0) <= now) {
        delete next.assignments[requestId];
      }
    }
    return next;
  }

  async #persist(state) {
    await this.ctx.storage.put(STATE_KEY, state);
    if (typeof this.ctx.storage.setAlarm !== "function") return;
    const deadlines = [
      ...(state.pendingMatch
        ? [Number(state.pendingMatch.retryAt ?? Date.now() + botRetryDelayMs(this.env))]
        : []),
      ...state.queue.flatMap((participant) => {
        const queueExpiry = Number(
          participant.lastSeenAt ?? participant.joinedAt ?? Date.now(),
        ) + QUEUE_TTL_MS;
        const values = [queueExpiry];
        if (botMatchFillEnabled(this.env) && !state.pendingMatch) {
          const fallbackAt = Number(
            participant.botFallbackAt ?? participant.joinedAt + botFillDelayMs(this.env),
          );
          values.push(Math.max(Date.now() + 1, fallbackAt));
        }
        return values;
      }),
      ...Object.values(state.assignments).map(
        (assignment) => Number(assignment.expiresAt ?? Date.now() + ASSIGNMENT_TTL_MS),
      ),
    ].filter((deadline) => Number.isFinite(deadline) && deadline > Date.now());
    if (deadlines.length > 0) {
      await this.ctx.storage.setAlarm(Math.min(...deadlines));
    } else if (typeof this.ctx.storage.deleteAlarm === "function") {
      await this.ctx.storage.deleteAlarm();
    }
  }
}

function normalizeParticipant(body, profileId) {
  return {
    clientRequestId: requireClientRequestId(body?.clientRequestId),
    displayName: normalizeDisplayName(body?.displayName),
    characterId: normalizeCharacterId(body?.characterId),
    profileId,
    playerType: "HUMAN",
  };
}

function searchingPayload(state, now, requesterRequestId) {
  const queued = [...state.queue].sort((a, b) => a.joinedAt - b.joinedAt);
  const requester = queued.find(
    (candidate) => candidate.clientRequestId === requesterRequestId,
  );
  const orderedForRequester = requester
    ? [
        requester,
        ...queued.filter(
          (candidate) => candidate.clientRequestId !== requesterRequestId,
        ),
      ]
    : queued;
  const visibleParticipants = orderedForRequester.slice(0, TARGET_PLAYER_COUNT);
  return {
    status: "SEARCHING",
    matchMode: "TEAM_UP",
    playerCount: TARGET_PLAYER_COUNT,
    queuedPlayers: Math.min(queued.length, TARGET_PLAYER_COUNT),
    targetPlayerCount: TARGET_PLAYER_COUNT,
    queuedCharacterIds: visibleParticipants.map((participant) =>
      normalizeCharacterId(participant.characterId),
    ),
    searchStartedAt: queued[0]?.joinedAt ?? now,
  };
}

function assertOwner(assignment, profileId) {
  if (assignment.profileId !== profileId) {
    throw httpError(
      403,
      "TEAM_MATCHMAKING_PROFILE_MISMATCH",
      "team matchmaking request belongs to another authenticated profile",
    );
  }
}

async function issueAssertion(env, matchId, participant) {
  return issueLeaderboardProfileAssertion(env, {
    matchId,
    profileId: participant.profileId,
    playerType: participant.playerType ?? "HUMAN",
    clientRequestId: participant.clientRequestId,
    expiresAt: Date.now() + PROFILE_ASSERTION_TTL_MS,
  });
}

function room(env, matchId) {
  if (!env.LUDOPROOF_MATCHES) {
    throw httpError(
      503,
      "MATCH_STORE_NOT_CONFIGURED",
      "match storage is not configured",
    );
  }
  const id = env.LUDOPROOF_MATCHES.idFromName(matchId);
  return env.LUDOPROOF_MATCHES.get(id);
}

async function requireJsonResponse(response) {
  let body;
  try {
    body = await response.json();
  } catch {
    throw httpError(
      503,
      "TEAM_MATCHMAKER_BAD_RESPONSE",
      "match storage returned invalid data",
    );
  }
  if (!response.ok) {
    throw httpError(
      response.status,
      body?.error ?? "TEAM_MATCHMAKER_MATCH_ERROR",
      body?.message ?? "match storage rejected team matchmaking",
    );
  }
  return body;
}

async function readJson(request) {
  const type = request.headers.get("content-type") ?? "";
  if (!type.toLowerCase().startsWith("application/json")) {
    throw httpError(
      415,
      "UNSUPPORTED_MEDIA_TYPE",
      "content-type must be application/json",
    );
  }
  const text = await request.text();
  if (new TextEncoder().encode(text).byteLength > 8 * 1024) {
    throw httpError(413, "REQUEST_TOO_LARGE", "request body is too large");
  }
  try {
    const value = text.length === 0 ? {} : JSON.parse(text);
    if (!value || typeof value !== "object" || Array.isArray(value)) {
      throw new Error("object required");
    }
    return value;
  } catch {
    throw httpError(400, "INVALID_JSON", "request body must be a JSON object");
  }
}

function errorResponse(error) {
  const status =
    Number.isInteger(error?.status) && error.status >= 400 && error.status <= 599
      ? error.status
      : 500;
  return json(status, {
    error: error?.code ?? "INTERNAL_ERROR",
    message:
      status >= 500 && !error?.code
        ? "internal server error"
        : String(error?.message ?? "internal server error"),
  });
}

function json(status, body) {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      "content-type": "application/json; charset=utf-8",
      "cache-control": "no-store",
      "x-content-type-options": "nosniff",
    },
  });
}
