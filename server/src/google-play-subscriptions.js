import { httpError, sha256Hex } from "./crypto.js";
import { googlePlayBillingEnabled, googleAccessToken } from "./google-play-billing.js";

export const GOOGLE_PLAY_SUBSCRIPTION_PRODUCT_ID = "paws_plus";
export const GOOGLE_PLAY_SUBSCRIPTION_BASE_PLAN_ID = "monthly-auto";
export const PAWS_PLUS_MONTHLY_GEMS = 300;

const PACKAGE_NAME = "com.rekixo.ludopaws";
const PUBLISHER_URL = "https://androidpublisher.googleapis.com/androidpublisher/v3";
const GOOGLE_OAUTH_ISSUERS = new Set(["accounts.google.com", "https://accounts.google.com"]);
const KNOWN_SUBSCRIPTION_STATES = new Set([
  "SUBSCRIPTION_STATE_PENDING", "SUBSCRIPTION_STATE_ACTIVE", "SUBSCRIPTION_STATE_PAUSED",
  "SUBSCRIPTION_STATE_IN_GRACE_PERIOD", "SUBSCRIPTION_STATE_ON_HOLD", "SUBSCRIPTION_STATE_CANCELED",
  "SUBSCRIPTION_STATE_EXPIRED", "SUBSCRIPTION_STATE_PENDING_PURCHASE_CANCELED",
]);
let cachedRtdnKeys = { expiresAt: 0, keys: [] };

export function googlePlaySubscriptionsEnabled(env) {
  if (
    !googlePlayBillingEnabled(env) ||
    env?.GOOGLE_PLAY_SUBSCRIPTION_ENABLED !== "true" ||
    env?.GOOGLE_PLAY_SUBSCRIPTION_PRODUCT_ID !== GOOGLE_PLAY_SUBSCRIPTION_PRODUCT_ID ||
    env?.GOOGLE_PLAY_SUBSCRIPTION_BASE_PLAN_ID !== GOOGLE_PLAY_SUBSCRIPTION_BASE_PLAN_ID
  ) return false;
  const audience = parseHttpsUrl(env?.GOOGLE_PLAY_RTDN_AUDIENCE);
  const email = String(env?.GOOGLE_PLAY_RTDN_PUSH_EMAIL ?? "").trim();
  return Boolean(audience && /^[^@\s]+@[^@\s]+\.iam\.gserviceaccount\.com$/.test(email));
}

export async function verifyGooglePlaySubscription(env, { productId, purchaseToken, expectedBillingAccountId }, fetchImpl = fetch, nowMs = Date.now()) {
  if (productId !== GOOGLE_PLAY_SUBSCRIPTION_PRODUCT_ID) throw httpError(400, "UNKNOWN_SUBSCRIPTION_PRODUCT", "subscription product is not recognized");
  if (typeof purchaseToken !== "string" || purchaseToken.length < 8 || purchaseToken.length > 2048 ||
      !/^[a-f0-9]{64}$/.test(String(expectedBillingAccountId ?? ""))) {
    throw httpError(400, "INVALID_SUBSCRIPTION", "subscription purchase information is invalid");
  }
  if (!googlePlaySubscriptionsEnabled(env)) throw httpError(503, "PLAY_SUBSCRIPTION_NOT_ENABLED", "Google Play subscription verification is not enabled");
  const accessToken = await googleAccessToken(env, fetchImpl);
  const url = PUBLISHER_URL + "/applications/" + encodeURIComponent(PACKAGE_NAME) +
    "/purchases/subscriptionsv2/tokens/" + encodeURIComponent(purchaseToken);
  let response;
  try {
    response = await fetchImpl(url, { method: "GET", headers: { authorization: "Bearer " + accessToken }, signal: AbortSignal.timeout(10_000) });
  } catch {
    throw httpError(503, "PLAY_SUBSCRIPTION_UNAVAILABLE", "Google Play subscription verification is temporarily unavailable");
  }
  if (!response.ok) {
    if (response.status === 404) throw httpError(400, "PLAY_SUBSCRIPTION_NOT_FOUND", "Google Play subscription was not found");
    throw httpError(response.status === 401 || response.status === 403 ? 503 : 502, "PLAY_SUBSCRIPTION_REJECTED", "Google Play could not verify this subscription");
  }
  let payload;
  try { payload = await response.json(); }
  catch { throw httpError(502, "PLAY_SUBSCRIPTION_BAD_RESPONSE", "Google Play returned an invalid subscription response"); }
  const state = payload?.subscriptionState;
  if (!KNOWN_SUBSCRIPTION_STATES.has(state)) throw httpError(502, "PLAY_SUBSCRIPTION_STATE_INVALID", "Google Play returned an unsupported subscription state");
  if (payload?.externalAccountIdentifiers?.obfuscatedExternalAccountId !== expectedBillingAccountId) {
    throw httpError(403, "PLAY_SUBSCRIPTION_ACCOUNT_MISMATCH", "Google Play subscription does not belong to the signed-in app account");
  }
  const lineItems = Array.isArray(payload?.lineItems) ? payload.lineItems : [];
  const matchingItems = lineItems.filter((item) => item?.productId === GOOGLE_PLAY_SUBSCRIPTION_PRODUCT_ID &&
    item?.offerDetails?.basePlanId === GOOGLE_PLAY_SUBSCRIPTION_BASE_PLAN_ID);
  if (matchingItems.length !== 1 || lineItems.length !== 1) {
    throw httpError(400, "PLAY_SUBSCRIPTION_PRODUCT_MISMATCH", "Google Play subscription product or base plan does not match this app");
  }
  const item = matchingItems[0];
  const expiryAt = typeof item.expiryTime === "string" ? Date.parse(item.expiryTime) : NaN;
  if (!Number.isSafeInteger(expiryAt) || expiryAt <= 0) throw httpError(502, "PLAY_SUBSCRIPTION_EXPIRY_INVALID", "Google Play subscription expiry is invalid");
  const tokenHash = await sha256Hex("ludopaws:google-play-subscription-token:v1:" + purchaseToken);
  const linkedPurchaseTokenHash = typeof payload.linkedPurchaseToken === "string" &&
    payload.linkedPurchaseToken.length >= 8 && payload.linkedPurchaseToken.length <= 2048
    ? await sha256Hex("ludopaws:google-play-subscription-token:v1:" + payload.linkedPurchaseToken) : null;
  const orderId = typeof item.latestSuccessfulOrderId === "string" ? item.latestSuccessfulOrderId.trim() : "";
  const latestOrderIdHash = orderId ? await sha256Hex("ludopaws:google-play-order:v1:" + orderId) : null;
  const activeState = ["SUBSCRIPTION_STATE_ACTIVE", "SUBSCRIPTION_STATE_IN_GRACE_PERIOD", "SUBSCRIPTION_STATE_CANCELED"].includes(state);
  const active = activeState && expiryAt > nowMs;
  if (active && payload.acknowledgementState === "ACKNOWLEDGEMENT_STATE_PENDING") {
    await acknowledgeGooglePlaySubscription(env, purchaseToken, fetchImpl);
  } else if (!["ACKNOWLEDGEMENT_STATE_PENDING", "ACKNOWLEDGEMENT_STATE_ACKNOWLEDGED"].includes(payload.acknowledgementState)) {
    throw httpError(502, "PLAY_SUBSCRIPTION_ACK_STATE_INVALID", "Google Play subscription acknowledgement state is invalid");
  }
  return {
    productId: GOOGLE_PLAY_SUBSCRIPTION_PRODUCT_ID,
    basePlanId: GOOGLE_PLAY_SUBSCRIPTION_BASE_PLAN_ID,
    subscriptionState: state,
    expiryAt,
    autoRenewEnabled: item?.autoRenewingPlan?.autoRenewEnabled === true,
    active,
    tokenHash,
    linkedPurchaseTokenHash,
    latestOrderIdHash,
    checkedAt: nowMs,
  };
}

async function acknowledgeGooglePlaySubscription(env, purchaseToken, fetchImpl) {
  const accessToken = await googleAccessToken(env, fetchImpl);
  const url = PUBLISHER_URL + "/applications/" + encodeURIComponent(PACKAGE_NAME) +
    "/purchases/subscriptions/" + encodeURIComponent(GOOGLE_PLAY_SUBSCRIPTION_PRODUCT_ID) +
    "/tokens/" + encodeURIComponent(purchaseToken) + ":acknowledge";
  let response;
  try {
    response = await fetchImpl(url, {
      method: "POST",
      headers: { authorization: "Bearer " + accessToken, "content-type": "application/json" },
      body: "{}",
      signal: AbortSignal.timeout(10_000),
    });
  } catch {
    throw httpError(503, "PLAY_SUBSCRIPTION_ACK_UNAVAILABLE", "Google Play could not acknowledge this subscription yet");
  }
  if (!response.ok) throw httpError(response.status === 401 || response.status === 403 ? 503 : 502,
    "PLAY_SUBSCRIPTION_ACK_FAILED", "Google Play could not acknowledge this subscription yet");
}

export async function verifyGooglePlayRtdnPush(request, env, fetchImpl = fetch, nowMs = Date.now()) {
  if (!googlePlaySubscriptionsEnabled(env)) throw httpError(503, "PLAY_SUBSCRIPTION_NOT_ENABLED", "Google Play subscription notifications are not enabled");
  if (request.method !== "POST") throw httpError(405, "RTDN_METHOD_NOT_ALLOWED", "subscription notifications require POST");
  const token = String(request.headers.get("authorization") ?? "").replace(/^Bearer\s+/i, "").trim();
  if (!token || token.length > 16_384) throw httpError(401, "RTDN_AUTH_REQUIRED", "Google Play notification authentication is required");
  await verifyGooglePushIdentityToken(token, env, fetchImpl, nowMs);
  const contentLength = Number(request.headers.get("content-length") ?? 0);
  if (Number.isFinite(contentLength) && contentLength > 128 * 1024) throw httpError(413, "RTDN_BODY_TOO_LARGE", "subscription notification is too large");
  let bodyText;
  try { bodyText = await request.text(); if (bodyText.length > 128 * 1024) throw new Error("oversized"); }
  catch { throw httpError(400, "RTDN_BODY_INVALID", "subscription notification body is invalid"); }
  let body;
  try { body = JSON.parse(bodyText); }
  catch { throw httpError(400, "RTDN_BODY_INVALID", "subscription notification body is invalid"); }
  const encodedData = body?.message?.data;
  if (typeof encodedData !== "string" || encodedData.length > 100 * 1024) throw httpError(400, "RTDN_DATA_INVALID", "subscription notification data is invalid");
  let decoded;
  try {
    const bytes = Uint8Array.from(atob(encodedData), (character) => character.charCodeAt(0));
    decoded = JSON.parse(new TextDecoder().decode(bytes));
  } catch { throw httpError(400, "RTDN_DATA_INVALID", "subscription notification data is invalid"); }
  if (decoded?.packageName !== PACKAGE_NAME) throw httpError(403, "RTDN_PACKAGE_MISMATCH", "subscription notification belongs to a different application");
  if (decoded?.testNotification) return { kind: "test" };
  const notification = decoded?.subscriptionNotification;
  if (!notification) return { kind: "ignore" };
  if (notification.subscriptionId !== GOOGLE_PLAY_SUBSCRIPTION_PRODUCT_ID) return { kind: "ignore" };
  if (typeof notification.purchaseToken !== "string" || notification.purchaseToken.length < 8 ||
      notification.purchaseToken.length > 2048 || !Number.isSafeInteger(Number(notification.notificationType))) {
    throw httpError(400, "RTDN_SUBSCRIPTION_INVALID", "subscription notification fields are invalid");
  }
  return {
    kind: "subscription",
    purchaseToken: notification.purchaseToken,
    productId: notification.subscriptionId,
    notificationType: Number(notification.notificationType),
    eventTimeMillis: Number.isSafeInteger(Number(decoded.eventTimeMillis)) ? Number(decoded.eventTimeMillis) : null,
  };
}

async function verifyGooglePushIdentityToken(token, env, fetchImpl, nowMs) {
  const parts = token.split(".");
  if (parts.length !== 3 || parts.some((part) => !part)) throw httpError(401, "RTDN_TOKEN_INVALID", "Google Play notification token is invalid");
  let header, claims;
  try {
    header = JSON.parse(new TextDecoder().decode(decodeBase64Url(parts[0])));
    claims = JSON.parse(new TextDecoder().decode(decodeBase64Url(parts[1])));
  } catch { throw httpError(401, "RTDN_TOKEN_INVALID", "Google Play notification token is invalid"); }
  if (
    header?.alg !== "RS256" || typeof header?.kid !== "string" ||
    !GOOGLE_OAUTH_ISSUERS.has(String(claims?.iss ?? "")) ||
    !(claims?.aud === env.GOOGLE_PLAY_RTDN_AUDIENCE ||
      (Array.isArray(claims?.aud) && claims.aud.includes(env.GOOGLE_PLAY_RTDN_AUDIENCE))) ||
    claims?.email !== String(env.GOOGLE_PLAY_RTDN_PUSH_EMAIL) ||
    !(claims?.email_verified === true || claims?.email_verified === "true") ||
    !Number.isSafeInteger(Number(claims?.exp)) || Number(claims.exp) <= Math.floor(nowMs / 1000) ||
    !Number.isSafeInteger(Number(claims?.iat)) || Number(claims.iat) > Math.floor(nowMs / 1000) + 60
  ) throw httpError(401, "RTDN_TOKEN_INVALID", "Google Play notification token is invalid");
  const key = await findGooglePushKey(header.kid, fetchImpl, nowMs);
  if (!key) throw httpError(401, "RTDN_KEY_UNKNOWN", "Google Play notification signing key is unknown");
  let publicKey;
  try { publicKey = await crypto.subtle.importKey("jwk", key, { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" }, false, ["verify"]); }
  catch { throw httpError(503, "RTDN_KEYS_INVALID", "Google notification verification keys are invalid"); }
  let valid = false;
  try {
    valid = await crypto.subtle.verify("RSASSA-PKCS1-v1_5", publicKey, decodeBase64Url(parts[2]),
      new TextEncoder().encode(parts[0] + "." + parts[1]));
  } catch { valid = false; }
  if (!valid) throw httpError(401, "RTDN_SIGNATURE_INVALID", "Google Play notification signature is invalid");
}

async function findGooglePushKey(kid, fetchImpl, nowMs) {
  if (cachedRtdnKeys.expiresAt <= nowMs || !cachedRtdnKeys.keys.some((key) => key?.kid === kid)) {
    let response;
    try { response = await fetchImpl("https://www.googleapis.com/oauth2/v3/certs", { method: "GET", signal: AbortSignal.timeout(10_000) }); }
    catch { throw httpError(503, "RTDN_KEYS_UNAVAILABLE", "Google notification verification keys are temporarily unavailable"); }
    if (!response.ok) throw httpError(503, "RTDN_KEYS_UNAVAILABLE", "Google notification verification keys are temporarily unavailable");
    let payload;
    try { payload = await response.json(); }
    catch { throw httpError(503, "RTDN_KEYS_INVALID", "Google notification verification keys are invalid"); }
    if (!Array.isArray(payload?.keys) || payload.keys.length < 1 || payload.keys.length > 100) {
      throw httpError(503, "RTDN_KEYS_INVALID", "Google notification verification keys are invalid");
    }
    const match = String(response.headers.get("cache-control") ?? "").match(/max-age=(\d+)/i);
    const seconds = match ? Math.min(Number(match[1]), 3600) : 300;
    cachedRtdnKeys = { expiresAt: nowMs + Math.max(60, seconds) * 1000, keys: payload.keys };
  }
  return cachedRtdnKeys.keys.find((key) => key?.kid === kid && key?.kty === "RSA" && key?.use === "sig") ?? null;
}

function parseHttpsUrl(value) {
  try {
    const parsed = new URL(String(value ?? ""));
    return parsed.protocol === "https:" && !parsed.username && !parsed.password && !parsed.hash ? parsed : null;
  } catch { return null; }
}
function decodeBase64Url(value) {
  if (typeof value !== "string" || !/^[A-Za-z0-9_-]+$/.test(value)) throw new TypeError("invalid base64url");
  const data = value.replace(/-/g, "+").replace(/_/g, "/") + "=".repeat((4 - value.length % 4) % 4);
  return Uint8Array.from(atob(data), (character) => character.charCodeAt(0));
}
