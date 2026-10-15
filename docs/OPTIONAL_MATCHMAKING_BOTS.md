# Optional Online and Team Up bots

The matchmaking fallback and its turn controller are server-side features. Bots are created only by the Cloudflare Durable Object matchmaking queues after the configured real-player grace period. They do not receive a Google identity, Friend ID, leaderboard profile, or fake human queue ticket.

## Runtime controls

These are Worker runtime variables, not Android build flags:

| Variable | Default | Valid range / behavior |
| --- | ---: | --- |
| `LUDOPROOF_BOT_MATCH_FILL_ENABLED` | `true` | Set `false`, `0`, `off`, or `no` to stop creating new bot-filled matches. |
| `LUDOPROOF_BOT_TURNS_ENABLED` | `true` | Set `false`, `0`, `off`, or `no` to stop automated bot turns. |
| `LUDOPROOF_BOT_FILL_DELAY_MS` | `15000` | 250–120000 ms. |
| `LUDOPROOF_BOT_TURN_DELAY_MS` | `650` | 100–10000 ms. |
| `LUDOPROOF_BOT_RETRY_DELAY_MS` | `3000` | 250–30000 ms. |

Missing, invalid, or out-of-range delays use the documented defaults. Test and staging environments can use a shorter fill delay without modifying source code.

## Match and identity rules

- Online Find Match supports the existing 2-seat and 4-seat queue modes. Team Up remains exactly four seats and preserves A/B/A/B assignments.
- Real-player queue entries always take precedence. Bots only fill seats still vacant when the oldest eligible real-player request reaches the grace deadline.
- Bot display names are natural names, but every bot carries the server-authored `playerType: "BOT"` value and the Android roster shows a BOT label. Bot status is not inferred from a display name and cannot be set by an Android request.
- Bot identities have ephemeral player/request IDs and no leaderboard profile or social identity. Bot-filled matches are casual/unranked and are not submitted to the competitive leaderboard.
- Bot dice actions go through the regular MatchRoom commit/reveal, proof verification, and legal move endpoints. The bot controller does not create its own dice outcomes or bypass game rules.
- Bot-turn metadata is stored on the existing match room and serialized with authoritative mutations. There is no new Durable Object binding or separate bot database.

## Safe shutdown and removal

To stop creating new bot-filled matches, set `LUDOPROOF_BOT_MATCH_FILL_ENABLED=false`. Keep bot turns enabled while already-started bot matches finish. After active bot matches are drained or safely ended, set `LUDOPROOF_BOT_TURNS_ENABLED=false`. Do not disable automated turns while users are still in active bot-filled matches: their bot seats would stop taking turns.

To remove the implementation later:

1. Set `LUDOPROOF_BOT_MATCH_FILL_ENABLED=false`, drain or safely finish bot-filled matches, then set `LUDOPROOF_BOT_TURNS_ENABLED=false`.
2. Remove the timeout fallback blocks and bot imports from `matchmaker-queue.js` and `team-matchmaker-queue.js`; remove the signed `playerType` claim extension in `crypto.js`, the bot-type resolver/leaderboard guard in `match-room.js`, and the bot type field from `game-v1-core.js`.
3. Restore the `MatchRoom` export in `index-forfeit.js` to `match-room-lifecycle.js`; delete `matchmaking-bots.js`, `match-room-bots.js`, and `matchmaking-bots.test.js`.
4. Revert the bot label/count handling in `LudoPawsPlayerCardView.kt` and `PublicMatchmakingActions.kt`. The `GameModels.kt` `playerType` field and its tolerant parser can be left in place for backward-compatible snapshots or removed with the corresponding `GameJsonAuthoritativeSnapshotTest.kt` cases.
5. Remove the bot-specific additions from `crypto.test.js` and `match-room.integration.test.js`, delete this document, and rerun the relevant CI gates.

Keep snapshot parsing tolerant of older payloads during the migration; do not remove the bot turn controller while active bot-filled matches still require it.

## Release verification

Run the focused bot tests with `node --test test/matchmaking-bots.test.js` from `server/`, then the authoritative MatchRoom integration tests with `node --test test/match-room.integration.test.js`. Android snapshot parsing is covered by `GameJsonAuthoritativeSnapshotTest`. The final PR CI still validates the regular server suite, Android unit tests, lint, builds, and Worker dry-run. Do not enable these controls in production until that CI passes and the release owner has reviewed the BOT disclosure and unranked-results policy.
