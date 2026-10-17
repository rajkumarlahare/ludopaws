import test from "node:test";
import assert from "node:assert/strict";

import {
  FAST_DICE_ALGORITHM,
  FAST_DICE_PROTOCOL,
  createFastDiceReceipt,
  outcomeFromUint32,
  verifyFastDiceReceipt,
} from "../src/dice-fast.js";

test("uint32 rejection sampling excludes the biased upper tail", () => {
  assert.equal(outcomeFromUint32(0), 1);
  assert.equal(outcomeFromUint32(5), 6);
  assert.equal(outcomeFromUint32(6), 1);
  assert.equal(outcomeFromUint32(0xffff_fffb), 6);
  assert.equal(outcomeFromUint32(0xffff_fffc), null);
  assert.equal(outcomeFromUint32(0xffff_ffff), null);
  assert.equal(outcomeFromUint32(-1), null);
  assert.equal(outcomeFromUint32(1.5), null);
});

test("fast dice receipt can be reproduced and tampering is detected", async () => {
  const receipt = await createFastDiceReceipt({
    matchId: "LPABCDEFGH",
    eventIndex: 7,
    playerId: "player-fast-dice-test",
    clientCommitment: "a".repeat(64),
    issuedAt: 1_800_000_000_000,
  });

  assert.equal(receipt.protocol, FAST_DICE_PROTOCOL);
  assert.equal(receipt.algorithm, FAST_DICE_ALGORITHM);
  assert.ok(receipt.outcome >= 1 && receipt.outcome <= 6);
  assert.equal(await verifyFastDiceReceipt(receipt), true);
  assert.equal(
    await verifyFastDiceReceipt({ ...receipt, outcome: (receipt.outcome % 6) + 1 }),
    false,
  );
  assert.equal(
    await verifyFastDiceReceipt({ ...receipt, serverSeed: "0".repeat(64) }),
    false,
  );
});
