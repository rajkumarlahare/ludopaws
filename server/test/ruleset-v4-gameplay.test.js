import test from "node:test";
import assert from "node:assert/strict";

import {
  RULESET_V4,
  RULESET_V4_KEY,
  TEAM_RULESET_V4,
  TEAM_RULESET_V4_KEY,
  actingSeatForTurn,
  addPlayer,
  applyMove,
  forfeitTimedOutTurn,
  newMatch,
  publicState,
  rulesetForState,
  startMatch,
} from "../src/game.js";

function activeMatch({ matchId, mode = "ONLINE", playerCount = 2, rulesetKey = RULESET_V4_KEY }) {
  let state = newMatch({
    matchId, hostPlayerId: "p0", hostDisplayName: "P0", now: 1_000,
    targetPlayerCount: playerCount, matchMode: mode, rulesetKey,
  });
  for (let seat = 1; seat < playerCount; seat += 1) {
    state = addPlayer(state, {
      playerId: "p" + seat, displayName: "P" + seat,
      tokenAuthHash: "auth-" + seat, now: 1_000 + seat,
    });
  }
  return startMatch(state, "p0", 2_000);
}

test("v4 rulesets are explicit and existing versions remain available", () => {
  assert.equal(RULESET_V4_KEY, "CLASSIC_V4");
  assert.equal(TEAM_RULESET_V4_KEY, "TEAM_UP_V4");
  assert.equal(RULESET_V4.id, "ludoproof-standard-v4");
  assert.equal(TEAM_RULESET_V4.id, "ludoproof-team-v4");
  assert.equal(rulesetForState(activeMatch({ matchId: "LPV4TEST01" })), RULESET_V4);
  const active = activeMatch({
    matchId: "LPV4TEAM01", mode: "TEAM_UP", playerCount: 4, rulesetKey: TEAM_RULESET_V4_KEY,
  });
  assert.equal(rulesetForState(active), TEAM_RULESET_V4);
  assert.equal(publicState(active).rulesetId, "ludoproof-team-v4");
  assert.equal(Number.isSafeInteger(active.turnDeadlineAt), true);
});

test("a missed v4 roll advances the turn and establishes a new deadline", () => {
  const active = activeMatch({ matchId: "LPV4TIME01" });
  const firstDeadline = active.turnDeadlineAt;
  const missed = forfeitTimedOutTurn(active, { now: firstDeadline });
  assert.equal(missed.count, 1);
  assert.equal(missed.forfeited, false);
  assert.equal(missed.state.turnSeat, 1);
  assert.equal(missed.state.turnTimeoutCounts.p0, 1);
  assert.equal(missed.state.turnTimeoutNotice.playerId, "p0");
  assert.equal(missed.state.turnDeadlineAt, firstDeadline + RULESET_V4.turnRollTimeoutMs);
});

test("four missed classic turns forfeit that player and finish a two-player match", () => {
  const active = activeMatch({ matchId: "LPV4FOUR01" });
  const state = { ...active, turnTimeoutCounts: { p0: 3 }, turnDeadlineAt: 3_000, turnDeadlineSeat: 0 };
  const result = forfeitTimedOutTurn(state, { now: 3_000 });
  assert.equal(result.count, 4);
  assert.equal(result.forfeited, true);
  assert.equal(result.state.players[0].forfeitReason, "TURN_TIMEOUT");
  assert.equal(result.state.status, "FINISHED");
  assert.equal(result.state.winnerPlayerId, "p1");
  assert.equal(result.state.turnDeadlineAt, null);
});

test("two opposing Team-Up tokens on one cell protect the whole stack", () => {
  let state = activeMatch({
    matchId: "LPV4STAK01", mode: "TEAM_UP", playerCount: 4, rulesetKey: TEAM_RULESET_V4_KEY,
  });
  state.players[0].tokens = [5, -1, -1, -1];
  state.players[2].tokens = [31, -1, -1, -1];

  let scheduledSeat = -1;
  let movingSeat = -1;
  for (let candidate = 0; candidate < 4; candidate += 1) {
    const actor = actingSeatForTurn({ ...state, turnSeat: candidate });
    if (actor === 1 || actor === 3) { scheduledSeat = candidate; movingSeat = actor; break; }
  }
  assert.notEqual(scheduledSeat, -1);
  const movingPlayer = state.players[movingSeat];
  movingPlayer.tokens = [movingPlayer.color === "GREEN" ? 43 : 17, -1, -1, -1];
  state.turnSeat = scheduledSeat;
  state.pendingRoll = {
    status: "RESOLVED", seat: movingSeat, scheduledSeat, playerId: movingPlayer.playerId,
    eventIndex: state.randomEventIndex, eventId: "roll:" + state.randomEventIndex,
    roundId: "test-round", clientCommitment: "client-commitment",
    serverCommitment: "server-commitment", proofDigest: "test-proof", outcome: 1,
    legalTokenIndexes: [0], createdAt: 2_100, resolvedAt: 2_200,
  };

  const moved = applyMove(state, { playerId: movingPlayer.playerId, tokenIndex: 0, now: 2_300 });
  assert.equal(moved.captures, 0);
  assert.equal(moved.state.players[0].tokens[0], 5);
  assert.equal(moved.state.players[2].tokens[0], 31);
});
