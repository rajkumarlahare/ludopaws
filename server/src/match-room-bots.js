import { derivePlayerIdentity, sha256Hex } from "./crypto.js";
import {
  RULESET,
  actingSeatForTurn,
  applyMove,
  globalCellFor,
  isTeamUp,
} from "./game.js";
import { botRetryDelayMs, botTurnDelayMs, botTurnsEnabled } from "./matchmaking-bots.js";
import { MatchRoom as LifecycleMatchRoom } from "./match-room-lifecycle.js";

const STATE_KEY = "match-state";
const BOT_TURN_RECOVERY_MS = 25_000;

/**
 * Optional server-side bot controller layered above the ordinary MatchRoom.
 * Bot turns use the same authenticated commit/reveal/move handlers as humans;
 * only the server-created bot seat has access to its derived player token.
 */
export class MatchRoom extends LifecycleMatchRoom {
  constructor(ctx, env) {
    super(ctx, env);
    this.botRunPromise = null;
  }

  async fetch(request) {
    const response = await super.fetch(request);
    try {
      await this.#withBotMetadata(() => this.#scheduleBotTurn());
    } catch (error) {
      console.error("Could not schedule Ludo Paws bot turn", {
        code: error?.code ?? "BOT_TURN_SCHEDULE_FAILED",
      });
    }
    return response;
  }

  async alarm() {
    await super.alarm();
    await this.#runScheduledBotTurn();
  }

  #withBotMetadata(work) {
    // Bot metadata shares the MatchRoom mutation queue so stale bot-turn writes
    // cannot overwrite a concurrent authoritative game mutation.
    const run = this.mutationTail.then(work, work);
    this.mutationTail = run.catch(() => {});
    return run;
  }

  async #scheduleBotTurn() {
    if (!botTurnsEnabled(this.env) || typeof this.ctx.storage.setAlarm !== "function") {
      return;
    }

    const state = await this.ctx.storage.get(STATE_KEY);
    if (!state || state.status !== "ACTIVE") return;

    const pending = state.pendingRoll;
    if (pending) {
      const existing = state.botTurn;
      if (
        existing &&
        existing.playerId === pending.playerId &&
        existing.inProgress !== true
      ) {
        const currentRunAt = Number(existing.runAt ?? 0);
        // State polling must not keep pushing a failed bot action further out.
        const retryAt = currentRunAt > Date.now()
          ? currentRunAt
          : Date.now() + botRetryDelayMs(this.env);
        existing.runAt = Math.min(
          retryAt,
          Number(pending.revealDeadlineAt ?? retryAt),
        );
        await this.ctx.storage.put(STATE_KEY, state);
        await this.ctx.storage.setAlarm(existing.runAt);
      }
      return;
    }

    const seat = actingSeatForTurn(state);
    const player = state.players?.[seat];
    if (player?.playerType !== "BOT") {
      if (state.botTurn) {
        delete state.botTurn;
        await this.ctx.storage.put(STATE_KEY, state);
      }
      return;
    }

    const eventIndex = Number(state.randomEventIndex ?? 0);
    let turn = state.botTurn;
    if (
      !turn ||
      turn.playerId !== player.playerId ||
      turn.eventIndex !== eventIndex
    ) {
      const clientSeed = randomSeedHex();
      const clientCommitment = await sha256Hex(
        "entronex:v4:client-commit:" + clientSeed,
      );
      turn = {
        playerId: player.playerId,
        eventIndex,
        clientSeed,
        clientCommitment,
        runAt: Date.now() + botTurnDelayMs(this.env),
        inProgress: false,
      };
      state.botTurn = turn;
      await this.ctx.storage.put(STATE_KEY, state);
    }

    if (turn.inProgress) return;
    await this.ctx.storage.setAlarm(Math.max(Date.now(), Number(turn.runAt ?? Date.now())));
  }

  async #runScheduledBotTurn() {
    if (!botTurnsEnabled(this.env)) return;
    if (this.botRunPromise) return this.botRunPromise;

    this.botRunPromise = this.#executeBotTurn();
    try {
      await this.botRunPromise;
    } finally {
      this.botRunPromise = null;
    }
  }

  async #executeBotTurn() {
    let turn = null;
    let state = null;
    try {
      const prepared = await this.#withBotMetadata(async () => {
        const current = await this.ctx.storage.get(STATE_KEY);
        if (!current || current.status !== "ACTIVE") return null;

        let activeTurn = current.botTurn;
        const pending = current.pendingRoll;
        if (pending) {
          if (!activeTurn || pending.playerId !== activeTurn.playerId) return null;
        } else {
          const seat = actingSeatForTurn(current);
          const player = current.players?.[seat];
          if (player?.playerType !== "BOT") {
            if (activeTurn) {
              delete current.botTurn;
              await this.ctx.storage.put(STATE_KEY, current);
            }
            return null;
          }
          const eventIndex = Number(current.randomEventIndex ?? 0);
          if (
            !activeTurn ||
            activeTurn.playerId !== player.playerId ||
            activeTurn.eventIndex !== eventIndex
          ) {
            if (activeTurn) {
              delete current.botTurn;
              await this.ctx.storage.put(STATE_KEY, current);
            }
            return { reschedule: true };
          }
        }

        if (Number(activeTurn.runAt ?? 0) > Date.now()) {
          await this.ctx.storage.setAlarm(activeTurn.runAt);
          return null;
        }
        if (activeTurn.inProgress === true) {
          // A prior invocation may have been interrupted by a runtime restart.
          activeTurn.inProgress = false;
        }
        activeTurn.inProgress = true;
        await this.ctx.storage.put(STATE_KEY, current);
        await this.ctx.storage.setAlarm(Date.now() + BOT_TURN_RECOVERY_MS);
        return { turn: structuredClone(activeTurn), state: current };
      });
      if (!prepared) return;
      if (prepared.reschedule) {
        await this.#withBotMetadata(() => this.#scheduleBotTurn());
        return;
      }
      turn = prepared.turn;
      state = prepared.state;

      const bot = state.players.find((player) => player.playerId === turn.playerId);
      if (!bot || bot.playerType !== "BOT" || !bot.joinRequestId) {
        await this.#clearBotTurn(turn);
        return;
      }
      const identity = await derivePlayerIdentity(
        this.env,
        state.matchId,
        bot.joinRequestId,
      );

      if (!state.pendingRoll) {
        await this.#internalPlayerRequest(
          "/roll/commit",
          identity.playerToken,
          { clientCommitment: turn.clientCommitment },
        );
        state = await this.ctx.storage.get(STATE_KEY);
      }

      let pending = state?.pendingRoll;
      if (!pending || pending.playerId !== bot.playerId) {
        await this.#clearBotTurn(turn);
        return;
      }

      if (pending.status !== "RESOLVED") {
        await this.#internalPlayerRequest(
          "/roll/reveal",
          identity.playerToken,
          { clientSeed: turn.clientSeed },
        );
        state = await this.ctx.storage.get(STATE_KEY);
        pending = state?.pendingRoll;
      }

      if (
        pending?.status === "RESOLVED" &&
        Array.isArray(pending.legalTokenIndexes) &&
        pending.legalTokenIndexes.length > 0
      ) {
        const tokenIndex = chooseBotMove(state, bot.playerId, pending.legalTokenIndexes);
        await this.#internalPlayerRequest(
          "/move",
          identity.playerToken,
          {
            tokenIndex,
            eventIndex: pending.eventIndex,
          },
        );
      }

      await this.#clearBotTurn(turn);
      await this.#withBotMetadata(() => this.#scheduleBotTurn());
    } catch (error) {
      await this.#retryBotTurn(turn).catch(() => {});
      console.error("Ludo Paws bot turn failed; it will retry", {
        code: error?.code ?? "BOT_TURN_FAILED",
      });
    }
  }

  async #internalPlayerRequest(path, playerToken, body) {
    const response = await super.fetch(
      new Request("https://match-room-bot" + path, {
        method: "POST",
        headers: {
          authorization: "Bearer " + playerToken,
          "content-type": "application/json",
        },
        body: JSON.stringify(body),
      }),
    );
    let payload;
    try {
      payload = await response.json();
    } catch {
      const error = new Error("MatchRoom returned invalid bot action response");
      error.code = "BOT_ACTION_BAD_RESPONSE";
      throw error;
    }
    if (!response.ok) {
      const error = new Error(String(payload?.message ?? "Bot action was rejected"));
      error.code = String(payload?.error ?? "BOT_ACTION_REJECTED");
      throw error;
    }
    return payload;
  }

  async #clearBotTurn(turn) {
    await this.#withBotMetadata(async () => {
      const current = await this.ctx.storage.get(STATE_KEY);
      if (
        !current ||
        !current.botTurn ||
        current.botTurn.playerId !== turn.playerId ||
        current.botTurn.eventIndex !== turn.eventIndex
      ) {
        return;
      }
      delete current.botTurn;
      await this.ctx.storage.put(STATE_KEY, current);
    });
  }

  async #retryBotTurn(turn) {
    await this.#withBotMetadata(async () => {
      const current = await this.ctx.storage.get(STATE_KEY);
      if (
        !current ||
        current.status !== "ACTIVE" ||
        !current.botTurn ||
        (turn &&
          (current.botTurn.playerId !== turn.playerId ||
            current.botTurn.eventIndex !== turn.eventIndex))
      ) {
        return;
      }
      current.botTurn.inProgress = false;
      const retryAt = Date.now() + botRetryDelayMs(this.env);
      const deadline = Number(current.pendingRoll?.revealDeadlineAt ?? retryAt);
      current.botTurn.runAt = Math.min(retryAt, deadline);
      await this.ctx.storage.put(STATE_KEY, current);
      if (typeof this.ctx.storage.setAlarm === "function") {
        await this.ctx.storage.setAlarm(current.botTurn.runAt);
      }
    });
  }
}

function chooseBotMove(state, playerId, legalTokenIndexes) {
  const player = state.players.find((candidate) => candidate.playerId === playerId);
  const seat = state.players.findIndex((candidate) => candidate.playerId === playerId);
  const ranked = legalTokenIndexes.map((tokenIndex) => {
    let score = 0;
    try {
      const before = Number(player.tokens[tokenIndex] ?? -1);
      const simulated = applyMove(state, { playerId, tokenIndex, now: Date.now() });
      const after = Number(simulated.state.players[seat]?.tokens[tokenIndex] ?? before);
      if (simulated.state.status === "FINISHED" || simulated.winnerTeamId != null) score += 10_000;
      if (after === RULESET.homePosition) score += 300;
      if (simulated.captures > 0) score += 150 * simulated.captures;
      if (simulated.extraTurn) score += 35;
      if (before === -1 && after === 0) score += 24;
      score += Math.max(0, after - Math.max(0, before)) * 0.8;

      if (after >= 0 && after <= 50) {
        const globalCell = globalCellFor(player.color, after);
        if (RULESET.safeGlobalCells.includes(globalCell)) score += 16;
        if (isTeamUp(state)) {
          const opponentSeats = state.players
            .map((candidate, index) => ({ candidate, index }))
            .filter(({ candidate }) => candidate.teamId !== player.teamId);
          if (opponentSeats.some(({ candidate }) =>
            candidate.tokens.some((position) => position >= 0 && position <= 50 &&
              globalCellFor(candidate.color, position) === globalCell))) {
            score += 90;
          }
        }
      }
    } catch {
      score = Number.NEGATIVE_INFINITY;
    }

    const tieBreaker = new Uint8Array(1);
    crypto.getRandomValues(tieBreaker);
    return { tokenIndex, score: score + tieBreaker[0] / 1000 };
  });

  ranked.sort((left, right) => right.score - left.score);
  return ranked[0]?.tokenIndex ?? legalTokenIndexes[0];
}

function randomSeedHex() {
  const bytes = new Uint8Array(32);
  crypto.getRandomValues(bytes);
  return [...bytes].map((byte) => byte.toString(16).padStart(2, "0")).join("");
}
