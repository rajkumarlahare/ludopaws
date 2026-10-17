import { canonicalJson, sha256Hex } from "./crypto.js";

export const FAST_DICE_PROTOCOL = "ludopaws-server-csprng-v1";
export const FAST_DICE_ALGORITHM = "sha256-uint32-rejection-sampling-v1";

const DOMAIN = "ludopaws:server-csprng:v1:";
const UINT32_RANGE = 0x1_0000_0000;
const ACCEPT_LIMIT = UINT32_RANGE - (UINT32_RANGE % 6);
const SEED_HEX = /^[0-9a-f]{64}$/;
const DIGEST_HEX = /^[0-9a-f]{64}$/;
const MAX_DERIVATION_ATTEMPTS = 128;

export function outcomeFromUint32(value) {
  if (!Number.isInteger(value) || value < 0 || value >= UINT32_RANGE) {
    return null;
  }
  // Reject the top four uint32 values so every face has exactly the same
  // number of accepted samples; plain modulo without this step is biased.
  if (value >= ACCEPT_LIMIT) return null;
  return (value % 6) + 1;
}

function randomSeedHex() {
  const bytes = crypto.getRandomValues(new Uint8Array(32));
  return Array.from(bytes, (value) => value.toString(16).padStart(2, "0")).join("");
}

async function deriveSample({ serverSeed, matchId, eventIndex, playerId, clientCommitment }) {
  for (let attempt = 0; attempt < MAX_DERIVATION_ATTEMPTS; attempt += 1) {
    const digest = await sha256Hex(
      DOMAIN +
        "draw:" +
        serverSeed + ":" +
        matchId + ":" +
        eventIndex + ":" +
        playerId + ":" +
        clientCommitment + ":" +
        attempt,
    );
    const drawWordHex = digest.slice(0, 8);
    const outcome = outcomeFromUint32(Number.parseInt(drawWordHex, 16));
    if (outcome !== null) {
      return { outcome, drawAttempt: attempt, drawWordHex };
    }
  }
  throw new Error("secure dice rejection sampling did not converge");
}

/**
 * Generates a one-use, server-CSPRNG dice receipt. The result is authoritative;
 * the receipt makes the exact derivation reproducible but is not an independent
 * third-party fairness attestation.
 */
export async function createFastDiceReceipt({
  matchId,
  eventIndex,
  playerId,
  clientCommitment,
  issuedAt = Date.now(),
}) {
  if (
    typeof matchId !== "string" ||
    !matchId ||
    !Number.isSafeInteger(eventIndex) ||
    eventIndex < 0 ||
    typeof playerId !== "string" ||
    !playerId ||
    typeof clientCommitment !== "string" ||
    !DIGEST_HEX.test(clientCommitment) ||
    !Number.isSafeInteger(issuedAt)
  ) {
    throw new TypeError("invalid fast dice context");
  }

  const serverSeed = randomSeedHex();
  const serverCommitment = await sha256Hex(
    DOMAIN + "seed-commitment:" + serverSeed,
  );
  const sample = await deriveSample({
    serverSeed,
    matchId,
    eventIndex,
    playerId,
    clientCommitment,
  });
  const receipt = {
    protocol: FAST_DICE_PROTOCOL,
    algorithm: FAST_DICE_ALGORITHM,
    matchId,
    eventIndex,
    playerId,
    clientCommitment,
    serverSeed,
    serverCommitment,
    outcome: sample.outcome,
    roundId: crypto.randomUUID(),
    issuedAt,
    drawAttempt: sample.drawAttempt,
    drawWordHex: sample.drawWordHex,
  };
  const proofDigest = await sha256Hex(
    DOMAIN + "receipt:" + canonicalJson(receipt),
  );
  return { ...receipt, proofDigest };
}

export async function verifyFastDiceReceipt(receipt) {
  try {
    if (
      !receipt ||
      receipt.protocol !== FAST_DICE_PROTOCOL ||
      receipt.algorithm !== FAST_DICE_ALGORITHM ||
      typeof receipt.matchId !== "string" ||
      !Number.isSafeInteger(receipt.eventIndex) ||
      typeof receipt.playerId !== "string" ||
      !DIGEST_HEX.test(receipt.clientCommitment ?? "") ||
      !SEED_HEX.test(receipt.serverSeed ?? "") ||
      !DIGEST_HEX.test(receipt.serverCommitment ?? "") ||
      !DIGEST_HEX.test(receipt.proofDigest ?? "") ||
      !Number.isInteger(receipt.outcome) ||
      receipt.outcome < 1 ||
      receipt.outcome > 6 ||
      typeof receipt.roundId !== "string" ||
      !Number.isSafeInteger(receipt.issuedAt)
    ) {
      return false;
    }

    const expectedCommitment = await sha256Hex(
      DOMAIN + "seed-commitment:" + receipt.serverSeed,
    );
    if (expectedCommitment !== receipt.serverCommitment) return false;

    const sample = await deriveSample({
      serverSeed: receipt.serverSeed,
      matchId: receipt.matchId,
      eventIndex: receipt.eventIndex,
      playerId: receipt.playerId,
      clientCommitment: receipt.clientCommitment,
    });
    if (
      sample.outcome !== receipt.outcome ||
      sample.drawAttempt !== receipt.drawAttempt ||
      sample.drawWordHex !== receipt.drawWordHex
    ) {
      return false;
    }

    const { proofDigest, ...receiptCore } = receipt;
    return (
      (await sha256Hex(DOMAIN + "receipt:" + canonicalJson(receiptCore))) ===
      proofDigest
    );
  } catch {
    return false;
  }
}
