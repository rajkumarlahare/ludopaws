/**
 * Matchmaking character preference helpers.
 *
 * Character identity is cosmetic: it may improve a match's visual variety, but
 * it must never prevent a match when the short preference window expires.
 */
export const CHARACTER_PREFERENCE_WINDOW_MS = 2_000;

function joinedAt(candidate) {
  const value = Number(candidate?.joinedAt ?? 0);
  return Number.isFinite(value) ? value : 0;
}

function requestId(candidate) {
  return String(candidate?.clientRequestId ?? "");
}

function characterId(candidate) {
  // Queue participants are normalized at the request boundary. Treat older
  // persisted records without a character as Dog for backward compatibility.
  const value = String(candidate?.characterId ?? "dog").trim().toLowerCase();
  return value || "dog";
}

/**
 * Selects an oldest-player-anchored group with maximum available character
 * diversity. The oldest queued player always remains in the group; other seats
 * prefer a character not already used before falling back to duplicate IDs.
 */
export function selectCharacterDiverseGroup(candidates, playerCount) {
  if (!Number.isInteger(playerCount) || playerCount < 1) return [];
  const sorted = [...candidates].sort(
    (left, right) =>
      joinedAt(left) - joinedAt(right) ||
      requestId(left).localeCompare(requestId(right)),
  );
  if (sorted.length < playerCount) return [];

  const selected = [sorted[0]];
  const selectedRequestIds = new Set([requestId(sorted[0])]);
  const usedCharacters = new Set([characterId(sorted[0])]);

  for (const candidate of sorted.slice(1)) {
    if (selected.length >= playerCount) break;
    const id = requestId(candidate);
    const character = characterId(candidate);
    if (selectedRequestIds.has(id) || usedCharacters.has(character)) continue;
    selected.push(candidate);
    selectedRequestIds.add(id);
    usedCharacters.add(character);
  }

  // If the queue has fewer unique characters than seats, fill from the oldest
  // remaining participants rather than holding players indefinitely.
  for (const candidate of sorted.slice(1)) {
    if (selected.length >= playerCount) break;
    const id = requestId(candidate);
    if (selectedRequestIds.has(id)) continue;
    selected.push(candidate);
    selectedRequestIds.add(id);
  }

  return selected.sort(
    (left, right) =>
      joinedAt(left) - joinedAt(right) ||
      requestId(left).localeCompare(requestId(right)),
  );
}

export function distinctCharacterCount(participants) {
  return new Set(participants.map(characterId)).size;
}

export function characterPreferenceKey(playerCount, rulesetKey) {
  return `${playerCount}:${String(rulesetKey)}`;
}
