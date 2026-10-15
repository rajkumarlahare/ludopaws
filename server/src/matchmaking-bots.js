import { LUDO_PAWS_CHARACTER_IDS } from "./ludo-paws-characters.js";

const BOT_NAMES = Object.freeze([
  "Aarav", "Maya", "Leo", "Zara", "Arjun", "Ava",
  "Noah", "Anaya", "Ishaan", "Mia", "Kabir", "Riya",
  "Ethan", "Meera", "Ayaan", "Sofia", "Reyansh", "Ira",
  "Vihaan", "Nora", "Dev", "Tara", "Rohan", "Kiara",
]);

const DEFAULT_FILL_DELAY_MS = 15_000;
const DEFAULT_TURN_DELAY_MS = 1_100;
const DEFAULT_RESULT_HOLD_MS = 950;
const DEFAULT_RETRY_DELAY_MS = 3_000;

function isEnabled(value, fallback = true) {
  if (value == null || String(value).trim() === "") return fallback;
  return !["0", "false", "off", "no", "disabled"].includes(
    String(value).trim().toLowerCase(),
  );
}

function boundedMilliseconds(value, fallback, min, max) {
  const number = Number(value);
  return Number.isSafeInteger(number) && number >= min && number <= max
    ? number
    : fallback;
}

export function botMatchFillEnabled(env) {
  return isEnabled(env?.LUDOPROOF_BOT_MATCH_FILL_ENABLED);
}

export function botTurnsEnabled(env) {
  return isEnabled(env?.LUDOPROOF_BOT_TURNS_ENABLED);
}

export function botFillDelayMs(env) {
  return boundedMilliseconds(
    env?.LUDOPROOF_BOT_FILL_DELAY_MS,
    DEFAULT_FILL_DELAY_MS,
    250,
    120_000,
  );
}

export function botTurnDelayMs(env) {
  return boundedMilliseconds(
    env?.LUDOPROOF_BOT_TURN_DELAY_MS,
    DEFAULT_TURN_DELAY_MS,
    1_100,
    10_000,
  );
}

export function botResultHoldMs(env) {
  return boundedMilliseconds(
    env?.LUDOPROOF_BOT_RESULT_HOLD_MS,
    DEFAULT_RESULT_HOLD_MS,
    950,
    5_000,
  );
}

export function botRetryDelayMs(env) {
  return boundedMilliseconds(
    env?.LUDOPROOF_BOT_RETRY_DELAY_MS,
    DEFAULT_RETRY_DELAY_MS,
    250,
    30_000,
  );
}

/**
 * Creates server-owned bot seats, not fake authenticated users or queue tickets.
 * Names are natural display names; the public playerType remains BOT so UI can
 * label them transparently. IDs are ephemeral per match and never have profiles.
 */
export function createBotParticipants({
  count,
  playerCount,
  matchMode,
  existingNames = [],
  now = Date.now(),
}) {
  if (!Number.isInteger(count) || count < 0 || count > 3) {
    throw new RangeError("bot seat count must be between zero and three");
  }
  if (playerCount !== 2 && playerCount !== 4) {
    throw new RangeError("bot player count must be two or four");
  }
  if (matchMode !== "ONLINE" && matchMode !== "TEAM_UP") {
    throw new RangeError("bots are supported only in public Online and Team Up matches");
  }
  if (matchMode === "TEAM_UP" && playerCount !== 4) {
    throw new RangeError("Team Up bots require four seats");
  }

  const usedNames = new Set(existingNames.map((name) => String(name).trim().toLocaleLowerCase("en")));
  const bots = [];
  for (let index = 0; index < count; index += 1) {
    const availableNames = BOT_NAMES.filter(
      (name) => !usedNames.has(name.toLocaleLowerCase("en")),
    );
    const pool = availableNames.length > 0 ? availableNames : BOT_NAMES;
    const random = new Uint8Array(2);
    crypto.getRandomValues(random);
    const displayName = pool[((random[0] << 8) | random[1]) % pool.length];
    usedNames.add(displayName.toLocaleLowerCase("en"));
    const characterId = LUDO_PAWS_CHARACTER_IDS[random[1] % LUDO_PAWS_CHARACTER_IDS.length];

    bots.push({
      clientRequestId: crypto.randomUUID(),
      displayName,
      characterId,
      profileId: null,
      playerType: "BOT",
      playerCount,
      matchMode,
      joinedAt: now,
      lastSeenAt: now,
    });
  }
  return bots;
}
