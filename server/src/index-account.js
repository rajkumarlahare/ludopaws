import baseWorker, {
  ApiGate,
  FriendConversation,
  FriendDirectory,
  FriendRecentRoom,
  LeaderboardRoom,
  MatchmakerQueue,
  MatchReceiptArchive,
  MatchRoom,
  StorePurchaseLedger,
  TeamMatchmakerQueue,
} from "./index-forfeit.js";
import { AccountDirectory } from "./account-directory.js";
import { PlayerAccount } from "./player-account.js";
import {
  billingAccountId,
  hashGoogleSubject,
  verifyAccessToken,
  verifyGoogleIdToken,
} from "./account-security.js";
import { GOOGLE_PLAY_PRODUCTS, googlePlayBillingEnabled, verifyAndRedeemGooglePlayPurchase } from "./google-play-billing.js";
import { verifyAdMobSsvRequest } from "./admob-ssv.js";
import { bearerToken, httpError, readJsonObject, sha256Hex } from "./crypto.js";

export {
  ApiGate, FriendConversation, FriendDirectory, FriendRecentRoom, LeaderboardRoom,
  MatchmakerQueue, MatchReceiptArchive, MatchRoom, StorePurchaseLedger, TeamMatchmakerQueue,
  AccountDirectory, PlayerAccount,
};

const MAX_BODY_BYTES = 8 * 1024;
const RATE_WINDOW_MS = 60_000;
const ACCOUNT_RATE_LIMITS = Object.freeze({
  guest: 5, challenge: 20, googleLogin: 10, googleLink: 10, sessionRefresh: 30,
  sessionLogout: 20, profile: 60, wallet: 120, walletMutation: 30, friendLink: 15, leaderboardLink: 15,
  storePurchase: 10,
});

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    try {
      if (url.pathname === "/ready" && request.method === "GET") {
        const base = await baseWorker.fetch(request, env);
        let body;
        try { body = await base.clone().json(); }
        catch { return base; }
        const accountDirectoryConfigured = Boolean(env?.LUDOPROOF_ACCOUNT_DIRECTORY);
        const playerAccountsConfigured = Boolean(env?.LUDOPROOF_PLAYER_ACCOUNTS);
        const googleSignInClientIdConfigured = typeof env?.GOOGLE_SIGN_IN_WEB_CLIENT_ID === "string" &&
          env.GOOGLE_SIGN_IN_WEB_CLIENT_ID.trim().length > 0;
        const sessionHmacConfigured = typeof env?.LUDOPROOF_SESSION_HMAC_KEY === "string" &&
          new TextEncoder().encode(env.LUDOPROOF_SESSION_HMAC_KEY).byteLength >= 32;
        const ready = base.ok && body?.ready === true && accountDirectoryConfigured &&
          playerAccountsConfigured && googleSignInClientIdConfigured && sessionHmacConfigured;
        return json(ready ? 200 : 503, {
          ...body,
          ok: ready,
          ready,
          productionClaim: ready && body?.productionClaim === true,
          checks: {
            ...(body?.checks ?? {}),
            accountDirectoryConfigured,
            playerAccountsConfigured,
            googleSignInClientIdConfigured,
            sessionHmacConfigured,
            playBillingEnabled: googlePlayBillingEnabled(env),
            admobSsvEnabled: env?.LUDOPROOF_ADMOB_SSV_ENABLED === "true" &&
              Boolean(env?.LUDOPROOF_ADMOB_REWARDED_AD_UNIT_ID),
          },
        });
      }

      if (url.pathname === "/api/account/guest" && request.method === "POST") {
        await rateLimit(env, request, "account:guest", ACCOUNT_RATE_LIMITS.guest);
        const body = await readJson(request);
        const guest = await directoryRequest(env, "/guest", body);
        const accountId = requireText(guest.accountId, "accountId");
        return await playerResponse(env, accountId, "/session/create", {
          accountId,
          displayName: body.displayName,
        });
      }

      if (url.pathname === "/api/account/google/challenge" && request.method === "POST") {
        requireMethod(request, "POST");
        await rateLimit(env, request, "account:google:challenge", ACCOUNT_RATE_LIMITS.challenge);
        return await directoryResponse(env, "/nonce/create", {});
      }

      if (url.pathname === "/api/account/google/link" && request.method === "POST") {
        await rateLimit(env, request, "account:google:link", ACCOUNT_RATE_LIMITS.googleLink);
        const session = await authenticate(request, env);
        const body = await readJson(request);
        const google = await verifyGoogleIdToken(env, body.idToken, body.nonce);
        await directoryRequest(env, "/nonce/consume", { nonce: body.nonce });
        const subjectHash = await hashGoogleSubject(google.subject);
        const linked = await directoryRequest(env, "/google/link", {
          accountId: session.accountId,
          subjectHash,
        });
        let profile = session.profile;
        // The default guest label is a placeholder, not a player choice. Use the server-verified
        // Google display name once, but never overwrite an existing custom account name.
        if (
          String(profile?.displayName ?? "") === "Guest User" &&
          typeof google.displayName === "string" &&
          google.displayName.trim()
        ) {
          const updated = await playerRequest(env, session.accountId, "/profile/update", {
            accountId: session.accountId,
            sessionId: session.sessionId,
            displayName: google.displayName,
          });
          profile = updated.profile ?? profile;
        }
        return json(200, {
          ok: true,
          accountId: session.accountId,
          linked: linked.linked === true,
          profile,
        });
      }

      if (url.pathname === "/api/account/google/login" && request.method === "POST") {
        await rateLimit(env, request, "account:google:login", ACCOUNT_RATE_LIMITS.googleLogin);
        const body = await readJson(request);
        const google = await verifyGoogleIdToken(env, body.idToken, body.nonce);
        await directoryRequest(env, "/nonce/consume", { nonce: body.nonce });
        const subjectHash = await hashGoogleSubject(google.subject);
        // Login restores only an already-linked account. Creating a fresh guest here would
        // silently switch identities and orphan the current guest's inventory/social state.
        const existing = await directoryRequest(env, "/google/lookup", { subjectHash });
        const accountId = requireAccountId(existing.accountId);
        return await playerResponse(env, accountId, "/session/create", {
          accountId,
          displayName: google.displayName,
        });
      }

      if (url.pathname === "/api/account/refresh" && request.method === "POST") {
        await rateLimit(env, request, "account:session:refresh", ACCOUNT_RATE_LIMITS.sessionRefresh);
        const body = await readJson(request);
        const accountId = requireAccountId(body.accountId);
        const sessionId = requireAccountId(body.sessionId);
        return await playerResponse(env, accountId, "/session/refresh", { ...body, accountId, sessionId });
      }

      if (url.pathname === "/api/account/logout" && request.method === "POST") {
        await rateLimit(env, request, "account:session:logout", ACCOUNT_RATE_LIMITS.sessionLogout);
        const session = await authenticate(request, env);
        return await playerResponse(env, session.accountId, "/session/logout", {
          accountId: session.accountId,
          sessionId: session.sessionId,
        });
      }

      if (url.pathname === "/api/account/me" && request.method === "GET") {
        await rateLimit(env, request, "account:profile:read", ACCOUNT_RATE_LIMITS.profile);
        const session = await authenticate(request, env);
        return await playerResponse(env, session.accountId, "/profile/get", {
          accountId: session.accountId,
          sessionId: session.sessionId,
        });
      }

      if (url.pathname === "/api/account/profile" && request.method === "POST") {
        await rateLimit(env, request, "account:profile:update", ACCOUNT_RATE_LIMITS.profile);
        const session = await authenticate(request, env);
        const body = await readJson(request);
        return await playerResponse(env, session.accountId, "/profile/update", {
          accountId: session.accountId,
          sessionId: session.sessionId,
          displayName: body.displayName,
        });
      }

      if (url.pathname === "/api/account/links" && request.method === "GET") {
        await rateLimit(env, request, "account:links:read", ACCOUNT_RATE_LIMITS.profile);
        const session = await authenticate(request, env);
        return await directoryResponse(env, "/identity/list", { accountId: session.accountId });
      }

      if (url.pathname === "/api/account/identities/recover" && request.method === "GET") {
        await rateLimit(env, request, "account:identity:recover", 10);
        const session = await authenticate(request, env);
        const links = await directoryRequest(env, "/identity/list", { accountId: session.accountId });
        let friend = null;
        let leaderboard = null;
        if (links.friendId) {
          if (!env?.LUDOPROOF_FRIENDS) throw httpError(503, "FRIENDS_NOT_CONFIGURED", "friend directory is not configured");
          const target = env.LUDOPROOF_FRIENDS.get(env.LUDOPROOF_FRIENDS.idFromName("GLOBAL:V1"));
          friend = await parseInternalResponse(await target.fetch(new Request("https://friends/account/recover", {
            method: "POST",
            headers: { "content-type": "application/json" },
            body: JSON.stringify({ friendId: links.friendId }),
          })), "FRIEND_IDENTITY_RECOVERY_FAILED");
        }
        if (links.leaderboardProfileId) {
          if (!env?.LUDOPROOF_LEADERBOARD) throw httpError(503, "LEADERBOARD_NOT_CONFIGURED", "leaderboard is not configured");
          const target = env.LUDOPROOF_LEADERBOARD.get(env.LUDOPROOF_LEADERBOARD.idFromName("global"));
          leaderboard = await parseInternalResponse(await target.fetch(new Request("https://leaderboard/profile/recover", {
            method: "POST",
            headers: { "content-type": "application/json" },
            body: JSON.stringify({ profileId: links.leaderboardProfileId }),
          })), "LEADERBOARD_IDENTITY_RECOVERY_FAILED");
        }
        return json(200, {
          ok: true,
          accountId: session.accountId,
          googleLinked: links.googleLinked === true,
          friend,
          leaderboard,
        });
      }

      if (url.pathname === "/api/account/link/friend" && request.method === "POST") {
        await rateLimit(env, request, "account:identity:friend-link", ACCOUNT_RATE_LIMITS.friendLink);
        const session = await authenticate(request, env);
        const body = await readJson(request);
        const friendToken = requireOpaqueToken(body.friendToken, /^lf_[A-Za-z0-9_-]{32,}$/);
        const friend = await authenticatedIdentity(env, "friends", "/identity", friendToken, "friendId");
        return await directoryResponse(env, "/identity/link", {
          accountId: session.accountId,
          identityType: "friend",
          externalId: friend.friendId,
        });
      }

      if (url.pathname === "/api/account/link/leaderboard" && request.method === "POST") {
        await rateLimit(env, request, "account:identity:leaderboard-link", ACCOUNT_RATE_LIMITS.leaderboardLink);
        const session = await authenticate(request, env);
        const body = await readJson(request);
        const profileToken = requireOpaqueToken(body.profileToken, /^lpp_[A-Za-z0-9_-]{32,}$/);
        const profile = await authenticatedIdentity(env, "leaderboard", "/profile/identity", profileToken, "profileId");
        return await directoryResponse(env, "/identity/link", {
          accountId: session.accountId,
          identityType: "leaderboard",
          externalId: profile.profileId,
        });
      }

      if (url.pathname === "/api/wallet" && request.method === "GET") {
        await rateLimit(env, request, "wallet:read", ACCOUNT_RATE_LIMITS.wallet);
        const session = await authenticate(request, env);
        return await playerResponse(env, session.accountId, "/wallet/get", {
          accountId: session.accountId,
          sessionId: session.sessionId,
        });
      }

      if (url.pathname === "/api/wallet/cosmetics/purchase" && request.method === "POST") {
        await rateLimit(env, request, "wallet:cosmetic:purchase", ACCOUNT_RATE_LIMITS.walletMutation);
        const session = await authenticate(request, env);
        const body = await readJson(request);
        return await playerResponse(env, session.accountId, "/wallet/cosmetic/purchase", {
          accountId: session.accountId,
          sessionId: session.sessionId,
          cosmeticId: body.cosmeticId,
          clientRequestId: body.clientRequestId,
        });
      }

      if (url.pathname === "/api/wallet/cosmetics/select" && request.method === "POST") {
        await rateLimit(env, request, "wallet:cosmetic:select", ACCOUNT_RATE_LIMITS.walletMutation);
        const session = await authenticate(request, env);
        const body = await readJson(request);
        return await playerResponse(env, session.accountId, "/wallet/cosmetic/select", {
          accountId: session.accountId,
          sessionId: session.sessionId,
          cosmeticId: body.cosmeticId,
        });
      }

      if (url.pathname === "/api/admob/ssv") {
        if (env?.LUDOPROOF_ADMOB_SSV_ENABLED !== "true" || !env?.LUDOPROOF_ADMOB_REWARDED_AD_UNIT_ID) {
          throw httpError(503, "ADMOB_SSV_NOT_ENABLED", "server-verified ad rewards are not enabled");
        }
        // Signed callbacks still consume CPU and may refresh Google's public verification keys.
        // Apply the shared IP rate gate before parsing/cryptographic verification to limit abuse.
        await rateLimit(env, request, "admob:ssv", 120);
        const verified = await verifyAdMobSsvRequest(request, env);
        return await playerResponse(env, verified.accountId, "/wallet/reward-ad", {
          accountId: verified.accountId,
          transactionId: verified.transactionId,
          kind: verified.kind,
          cosmeticId: verified.cosmeticId,
        });
      }

      if (url.pathname === "/api/store/status" && request.method === "GET") {
        const billingReady = googlePlayBillingEnabled(env) &&
          Boolean(env?.LUDOPROOF_ACCOUNT_DIRECTORY) &&
          Boolean(env?.LUDOPROOF_PLAYER_ACCOUNTS);
        const adsReady = env?.LUDOPROOF_ADMOB_SSV_ENABLED === "true" &&
          Boolean(env?.LUDOPROOF_ADMOB_REWARDED_AD_UNIT_ID);
        return json(200, { ok: true, billingAvailable: billingReady, rewardedAdsAvailable: adsReady });
      }

      if (url.pathname === "/api/store/purchases/verify" && request.method === "POST") {
        if (!googlePlayBillingEnabled(env)) {
          return await baseWorker.fetch(request, env);
        }
        await rateLimit(env, request, "store:purchases:verify", ACCOUNT_RATE_LIMITS.storePurchase);
        const session = await authenticate(request, env);
        const body = await readJson(request);
        const productId = requireText(body.productId, "productId");
        const purchaseToken = requireOpaqueToken(body.purchaseToken, /^.{8,2048}$/);
        const obfuscatedAccountId = await billingAccountId(session.accountId);
        const verified = await verifyAndRedeemGooglePlayPurchase(env, {
          productId,
          purchaseToken,
          accountId: obfuscatedAccountId,
        });
        const purchaseTokenHash = await sha256Hex("ludoproof:google-play-wallet-token:v1:" + purchaseToken);
        const grant = await playerRequest(env, session.accountId, "/wallet/play-purchase", {
          accountId: session.accountId,
          sessionId: session.sessionId,
          productId,
          purchaseTokenHash,
        });
        return json(200, {
          ok: true,
          verified: true,
          productId,
          gemAmount: GOOGLE_PLAY_PRODUCTS[productId]?.gemAmount ?? verified.gemAmount,
          removeAds: GOOGLE_PLAY_PRODUCTS[productId]?.removeAds === true,
          replay: verified.replay === true || grant.replay === true,
          wallet: grant.wallet,
        });
      }

      return await baseWorker.fetch(request, env);
    } catch (error) {
      return errorResponse(error);
    }
  },
};

async function authenticate(request, env) {
  const token = bearerToken(request);
  const session = await verifyAccessToken(env, token);
  const checked = await playerRequest(env, session.accountId, "/session/check", {
    accountId: session.accountId,
    sessionId: session.sessionId,
  });
  return { ...session, profile: checked.profile };
}

async function authenticatedIdentity(env, kind, path, token, field) {
  const namespace = kind === "friends" ? env?.LUDOPROOF_FRIENDS : env?.LUDOPROOF_LEADERBOARD;
  if (!namespace) throw httpError(503, kind === "friends" ? "FRIENDS_NOT_CONFIGURED" : "LEADERBOARD_NOT_CONFIGURED", "identity service is not configured");
  const target = namespace.get(namespace.idFromName(kind === "friends" ? "GLOBAL:V1" : "global"));
  const response = await target.fetch(new Request("https://identity" + path, {
    method: "GET",
    headers: { authorization: "Bearer " + token },
  }));
  const value = await parseInternalResponse(response, "IDENTITY_SERVICE_UNAVAILABLE");
  if (typeof value[field] !== "string") throw httpError(503, "IDENTITY_RESPONSE_INVALID", "identity service returned an invalid response");
  return value;
}

async function directoryResponse(env, path, body) {
  return json(200, await directoryRequest(env, path, body));
}

async function playerResponse(env, accountId, path, body) {
  return json(200, await playerRequest(env, accountId, path, body));
}

async function directoryRequest(env, path, body) {
  const namespace = env?.LUDOPROOF_ACCOUNT_DIRECTORY;
  if (!namespace) throw httpError(503, "ACCOUNT_DIRECTORY_NOT_CONFIGURED", "account directory is not configured");
  const target = namespace.get(namespace.idFromName("GLOBAL:V1"));
  const response = await target.fetch(new Request("https://account-directory" + path, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify(body),
  }));
  return parseInternalResponse(response, "ACCOUNT_DIRECTORY_UNAVAILABLE");
}

async function playerRequest(env, accountId, path, body) {
  const namespace = env?.LUDOPROOF_PLAYER_ACCOUNTS;
  if (!namespace) throw httpError(503, "PLAYER_ACCOUNT_STORE_NOT_CONFIGURED", "player account storage is not configured");
  const target = namespace.get(namespace.idFromName(accountId.toLowerCase()));
  const response = await target.fetch(new Request("https://player-account" + path, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify(body),
  }));
  return parseInternalResponse(response, "PLAYER_ACCOUNT_UNAVAILABLE");
}

async function parseInternalResponse(response, fallbackCode) {
  let value;
  try { value = await response.json(); }
  catch { throw httpError(503, fallbackCode, "account service returned an invalid response"); }
  if (!response.ok) {
    throw httpError(response.status, value?.error ?? fallbackCode, value?.message ?? "account request failed");
  }
  return value;
}

async function rateLimit(env, request, scope, limit) {
  if (!env?.LUDOPROOF_API_GATE) throw httpError(503, "RATE_LIMITER_NOT_CONFIGURED", "API rate limiter is not configured");
  const ip = request.headers.get("cf-connecting-ip") ?? "unknown";
  const key = await sha256Hex("ludoproof:api-rate:v1:" + scope + ":" + ip);
  const id = env.LUDOPROOF_API_GATE.idFromName(key);
  const target = env.LUDOPROOF_API_GATE.get(id);
  let response;
  try {
    response = await target.fetch(new Request("https://gate/check", {
      method: "POST", headers: { "content-type": "application/json" },
      body: JSON.stringify({ limit, periodMs: RATE_WINDOW_MS }),
    }));
  } catch {
    throw httpError(503, "RATE_LIMITER_UNAVAILABLE", "API rate limiter is unavailable");
  }
  let result;
  try { result = await response.json(); }
  catch { throw httpError(503, "RATE_LIMITER_BAD_RESPONSE", "API rate limiter returned invalid data"); }
  if (response.status === 429 || result?.allowed === false) {
    const error = httpError(429, "RATE_LIMITED", "too many requests");
    error.retryAfter = response.headers.get("retry-after") ?? "60";
    throw error;
  }
  if (!response.ok || result?.allowed !== true) throw httpError(503, "RATE_LIMITER_UNAVAILABLE", "API rate limiter rejected the check");
}
async function readJson(request) {
  return readJsonObject(request, MAX_BODY_BYTES);
}
function requireText(value, field) {
  if (typeof value !== "string" || !value.trim()) throw httpError(400, "INVALID_" + field.toUpperCase(), field + " is required");
  return value.trim();
}
function requireAccountId(value) {
  if (typeof value !== "string" || !/^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(value)) throw httpError(400, "INVALID_ACCOUNT_ID", "account identity is invalid");
  return value.toLowerCase();
}
function requireOpaqueToken(value, regex) {
  if (typeof value !== "string" || !regex.test(value)) throw httpError(400, "INVALID_TOKEN", "token is invalid");
  return value;
}
function requireMethod(request, expected) {
  if (request.method !== expected) {
    const error = httpError(405, "METHOD_NOT_ALLOWED", "method not allowed");
    error.allow = expected;
    throw error;
  }
}
function json(status, body) {
  const headers = new Headers({
    "content-type": "application/json; charset=utf-8",
    "cache-control": "no-store",
    "x-content-type-options": "nosniff",
    "x-frame-options": "DENY",
    "referrer-policy": "no-referrer",
  });
  if (status === 429) headers.set("retry-after", "60");
  return new Response(JSON.stringify(body), { status, headers });
}
function errorResponse(error) {
  const status = Number.isInteger(error?.status) && error.status >= 400 && error.status <= 599 ? error.status : 500;
  const headers = new Headers({
    "content-type": "application/json; charset=utf-8",
    "cache-control": "no-store",
    "x-content-type-options": "nosniff",
    "x-frame-options": "DENY",
    "referrer-policy": "no-referrer",
  });
  if (error?.allow) headers.set("allow", error.allow);
  if (error?.retryAfter) headers.set("retry-after", String(error.retryAfter));
  return new Response(JSON.stringify({
    error: error?.code ?? "INTERNAL_ERROR",
    message: status === 500 && !error?.code ? "internal server error" : String(error?.message ?? "request failed"),
  }), { status, headers });
}
