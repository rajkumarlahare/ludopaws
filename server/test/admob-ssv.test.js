import test from "node:test";
import assert from "node:assert/strict";
import { verifyAdMobSsvRequest } from "../src/admob-ssv.js";

function base64(value) { return Buffer.from(value).toString("base64"); }
function rawToDer(raw) {
  const part = (bytes) => {
    let value = Buffer.from(bytes);
    while (value.length > 1 && value[0] === 0) value = value.subarray(1);
    if (value[0] & 0x80) value = Buffer.concat([Buffer.from([0]), value]);
    return Buffer.concat([Buffer.from([0x02, value.length]), value]);
  };
  const body = Buffer.concat([part(raw.subarray(0, raw.length / 2)), part(raw.subarray(raw.length / 2))]);
  return Buffer.concat([Buffer.from([0x30, body.length]), body]);
}
async function signedRequest({ privateKey, keyId, accountId, adUnit, customData, timestamp, transactionId }) {
  const params = new URLSearchParams();
  params.append("ad_network", "5450213213286189855");
  params.append("ad_unit", adUnit);
  params.append("custom_data", customData);
  params.append("reward_amount", "5");
  params.append("reward_item", "gems");
  params.append("timestamp", String(timestamp));
  params.append("transaction_id", transactionId);
  params.append("user_id", accountId);
  const content = params.toString();
  const raw = new Uint8Array(await crypto.subtle.sign({ name: "ECDSA", hash: "SHA-256" }, privateKey, new TextEncoder().encode(content)));
  return new Request("https://api.example.test/api/admob/ssv?" + content +
    "&signature=" + rawToDer(raw).toString("base64url") + "&key_id=" + keyId);
}

test("AdMob SSV requires valid signature, matching ad unit and configured reward", async () => {
  const pair = await crypto.subtle.generateKey({ name: "ECDSA", namedCurve: "P-256" }, true, ["sign", "verify"]);
  const spki = await crypto.subtle.exportKey("spki", pair.publicKey);
  const keys = { keys: [{ keyId: 129, base64: base64(spki) }] };
  const fetchImpl = async () => Response.json(keys, { headers: { "cache-control": "public, max-age=3600" } });
  const now = Date.now();
  const accountId = "0a2c3a83-e0fb-4abc-8ac4-581a6d994fe7";
  const tx = "a1b2c3d4e5f60718293a4b5c6d7e8f90";
  const request = await signedRequest({
    privateKey: pair.privateKey, keyId: 129, accountId, adUnit: "ca-app-pub-123/456",
    customData: JSON.stringify({ kind: "gems" }), timestamp: now, transactionId: tx,
  });
  const result = await verifyAdMobSsvRequest(
    request, { LUDOPROOF_ADMOB_REWARDED_AD_UNIT_ID: "ca-app-pub-123/456" }, fetchImpl, now,
  );
  assert.deepEqual(result, { accountId, transactionId: tx, kind: "gems", cosmeticId: null, timestamp: now });

  await assert.rejects(
    verifyAdMobSsvRequest(new Request(request.url.replace("reward_amount=5", "reward_amount=50")),
      { LUDOPROOF_ADMOB_REWARDED_AD_UNIT_ID: "ca-app-pub-123/456" }, fetchImpl, now),
  );
});

test("AdMob SSV refuses callback from a different configured ad unit", async () => {
  const pair = await crypto.subtle.generateKey({ name: "ECDSA", namedCurve: "P-256" }, true, ["sign", "verify"]);
  const spki = await crypto.subtle.exportKey("spki", pair.publicKey);
  const fetchImpl = async () => Response.json({ keys: [{ keyId: 130, base64: base64(spki) }] });
  const now = Date.now();
  const request = await signedRequest({
    privateKey: pair.privateKey, keyId: 130,
    accountId: "0a2c3a83-e0fb-4abc-8ac4-581a6d994fe7",
    adUnit: "ca-app-pub-123/456", customData: JSON.stringify({ kind: "gems" }), timestamp: now,
    transactionId: "fedcba98765432100123456789abcdef",
  });
  await assert.rejects(
    verifyAdMobSsvRequest(request, { LUDOPROOF_ADMOB_REWARDED_AD_UNIT_ID: "ca-app-pub-999/456" }, fetchImpl, now),
    (error) => error.code === "SSV_AD_UNIT_INVALID",
  );
});
