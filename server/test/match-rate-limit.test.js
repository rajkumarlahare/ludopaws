import test from "node:test";
import assert from "node:assert/strict";

import { checkFastMatchRateLimit } from "../src/match-rate-limit.js";

const policies = [
  ["create", "LUDOPROOF_MATCH_CREATE_LIMITER"],
  ["join", "LUDOPROOF_MATCH_JOIN_LIMITER"],
  ["state", "LUDOPROOF_MATCH_STATE_LIMITER"],
  ["mutation", "LUDOPROOF_MATCH_MUTATION_LIMITER"],
];

test("fast match rate limit uses the policy-specific binding and scoped key", async () => {
  for (const [policy, name] of policies) {
    let received;
    const env = {
      [name]: {
        async limit(args) {
          received = args;
          return { success: true };
        },
      },
    };

    assert.equal(
      await checkFastMatchRateLimit(env, policy, "match-and-client-scope"),
      true,
    );
    assert.deepEqual(received, { key: "match-and-client-scope" });
  }
});

test("fast match rate limit distinguishes an explicit denial", async () => {
  const env = {
    LUDOPROOF_MATCH_MUTATION_LIMITER: {
      async limit() {
        return { success: false };
      },
    },
  };

  assert.equal(
    await checkFastMatchRateLimit(env, "mutation", "match:client"),
    false,
  );
});

test("missing, malformed, and failed bindings request a conservative fallback", async () => {
  assert.equal(await checkFastMatchRateLimit({}, "state", "key"), null);
  assert.equal(
    await checkFastMatchRateLimit(
      { LUDOPROOF_MATCH_STATE_LIMITER: {} },
      "state",
      "key",
    ),
    null,
  );
  assert.equal(
    await checkFastMatchRateLimit(
      {
        LUDOPROOF_MATCH_STATE_LIMITER: {
          async limit() {
            throw new Error("temporary binding failure");
          },
        },
      },
      "state",
      "key",
    ),
    null,
  );
  assert.equal(await checkFastMatchRateLimit({}, "unknown-policy", "key"), null);
});
