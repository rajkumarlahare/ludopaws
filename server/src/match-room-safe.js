import {
  MatchRoom as ForfeitMatchRoom,
} from "./match-room-forfeit.js";
import {
  bearerToken,
  sha256Hex,
} from "./crypto.js";
import {
  publicState,
} from "./game.js";
import {
  fairnessSummary,
} from "./fairness.js";

const STATE_KEY =
  "match-state";
const PLAYER_TOKEN_HASH_DOMAIN =
  "ludoproof:player-token:v1:";
const DIGEST =
  /^[0-9a-f]{64}$/i;

/**
 * Safety layer that preserves pending-roll deadlines while wrapping each
 * request in the authoritative room's serialized mutation queue.
 */
export class MatchRoom extends ForfeitMatchRoom {
  async fetch(request) {
    const response = await super.fetch(request);
    await this.#enqueue(() => this.#preservePendingRevealAlarm());
    return response;
  }

  #enqueue(work) {
    const run =
      this.mutationTail.then(
        work,
        work,
      );
    this.mutationTail =
      run.catch(
        () => {},
      );
    return run;
  }

  async #preservePendingRevealAlarm() {
    if (
      typeof this.ctx.storage
        .setAlarm !==
      "function"
    ) {
      return;
    }

    const state =
      await this.ctx.storage.get(
        STATE_KEY,
      );
    const deadline =
      state?.pendingRoll
        ?.revealDeadlineAt;

    if (
      Number.isSafeInteger(
        deadline,
      ) &&
      deadline >
        0
    ) {
      await this.ctx.storage
        .setAlarm(
          deadline,
        );
    }
  }


}

function decoratedState(
  state,
) {
  const history =
    (
      state.history ??
      []
    ).slice(
      -100,
    );
  const base =
    publicState(
      state,
    );

  return {
    ...base,
    players:
      base.players.map(
        (
          player,
          seat,
        ) => ({
          ...player,
          forfeited:
            Number.isSafeInteger(
              state.players?.[
                seat
              ]?.forfeitedAt,
            ),
          forfeitedAt:
            Number.isSafeInteger(
              state.players?.[
                seat
              ]?.forfeitedAt,
            )
              ? state.players[
                  seat
                ].forfeitedAt
              : null,
        }),
      ),
    history,
    fairness:
      fairnessSummary(
        history,
      ),
  };
}

function json(
  status,
  body,
) {
  return new Response(
    JSON.stringify(
      body,
    ),
    {
      status,
      headers: {
        "content-type":
          "application/json; charset=utf-8",
        "cache-control":
          "no-store",
        "x-content-type-options":
          "nosniff",
      },
    },
  );
}
