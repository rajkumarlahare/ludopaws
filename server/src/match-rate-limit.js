const MATCH_RATE_LIMIT_BINDINGS = Object.freeze({
  create: "LUDOPROOF_MATCH_CREATE_LIMITER",
  join: "LUDOPROOF_MATCH_JOIN_LIMITER",
  state: "LUDOPROOF_MATCH_STATE_LIMITER",
  mutation: "LUDOPROOF_MATCH_MUTATION_LIMITER",
});

/**
 * Try the Worker Rate Limiting binding for a hot-path match request.
 *
 * Returns true/false when a healthy binding gives a clear decision. Returns null
 * when no binding exists or the binding fails, allowing the caller to fall back
 * to the durable-object rate gate rather than silently skipping rate limiting.
 *
 * Cloudflare's native limiter is an edge, best-effort limiter, not a globally
 * serialized quota. Authoritative game/session validation remains in MatchRoom.
 */
export async function checkFastMatchRateLimit(env, policyKey, key) {
  const bindingName = MATCH_RATE_LIMIT_BINDINGS[policyKey];
  if (!bindingName) return null;

  const limiter = env?.[bindingName];
  if (!limiter || typeof limiter.limit !== "function") return null;

  try {
    const result = await limiter.limit({ key });
    return typeof result?.success === "boolean"
      ? result.success
      : null;
  } catch {
    return null;
  }
}
