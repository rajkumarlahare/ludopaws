import test from "node:test";
import assert from "node:assert/strict";

import worker from "../src/index.js";
import { sha256Hex } from "../src/crypto.js";

function rateGate(counter) {
  return {
    idFromName(key) {
      return key;
    },
    get() {
      return {
        async fetch() {
          counter.calls += 1;
          return Response.json(
            { allowed: true, remaining: 89 },
            { status: 200 },
          );
        },
      };
    },
  };
}

test("POST /api/matches/:id/roll resolves through one fast-dice room request", async () => {
  const observed = [];
  const limiter = { calls: 0 };
  const env = {
    LUDOPROOF_API_GATE: rateGate(limiter),
    LUDOPROOF_MATCHES: {
      idFromName(id) {
        assert.equal(id, "LPABCDEFGH");
        return id;
      },
      get() {
        return {
          async fetch(request) {
            const url = new URL(request.url);
            const body = request.method === "POST" ? await request.json() : null;
            observed.push({ path: url.pathname, authorization: request.headers.get("authorization"), body });
            if (url.pathname === "/roll/fast") {
              return Response.json({
                ok: true,
                outcome: 4,
                proof: { protocol: "ludopaws-server-csprng-v1" },
                state: { pendingRoll: { status: "RESOLVED" } },
              });
            }
            return Response.json({ error: "unexpected path" }, { status: 404 });
          },
        };
      },
    },
  };

  const clientSeed = "1".repeat(64);
  const clientCommitment = await sha256Hex(
    "entronex:v4:client-commit:" + clientSeed,
  );
  const response = await worker.fetch(
    new Request("https://api.example.test/api/matches/LPABCDEFGH/roll", {
      method: "POST",
      headers: {
        authorization: "Bearer test-player-token",
        "content-type": "application/json",
        "cf-connecting-ip": "192.0.2.50",
      },
      body: JSON.stringify({ clientSeed, clientCommitment }),
    }),
    env,
  );

  assert.equal(response.status, 200);
  assert.equal((await response.json()).outcome, 4);
  assert.equal(limiter.calls, 1);
  assert.deepEqual(observed.map((entry) => entry.path), ["/roll/fast"]);
  assert.deepEqual(observed[0].body, { clientSeed, clientCommitment });
  assert.ok(observed.every((entry) => entry.authorization === "Bearer test-player-token"));
});

test("single-call roll rejects a mismatched seed and commitment before mutating the room", async () => {
  let roomCalls = 0;
  const env = {
    LUDOPROOF_API_GATE: rateGate({ calls: 0 }),
    LUDOPROOF_MATCHES: {
      idFromName(id) { return id; },
      get() {
        return {
          async fetch() {
            roomCalls += 1;
            return Response.json({ ok: true });
          },
        };
      },
    },
  };
  const response = await worker.fetch(
    new Request("https://api.example.test/api/matches/LPABCDEFGH/roll", {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({
        clientSeed: "1".repeat(64),
        clientCommitment: "2".repeat(64),
      }),
    }),
    env,
  );

  assert.equal(response.status, 400);
  assert.equal((await response.json()).error, "CLIENT_COMMITMENT_MISMATCH");
  assert.equal(roomCalls, 0);
});
