import assert from "node:assert/strict";
import { readFileSync } from "node:fs";

function read(path) {
  return readFileSync(new URL(path, import.meta.url), "utf8");
}

function has(source, snippet, label) {
  assert.ok(source.includes(snippet), label + " is missing");
}

function matches(source, pattern, label) {
  assert.match(source, pattern, label + " is missing or malformed");
}

const page = read("../docs/account-deletion.html");
const policy = read("../docs/privacy-policy.html");
const worker = read("../server/src/index-account.js");
const directory = read("../server/src/account-directory.js");
const player = read("../server/src/player-account.js");
const pagesWorkflow = read("../.github/workflows/account-deletion-pages.yml");

has(page, 'id="guest-deletion-code" type="password" maxlength="64"', "guest delete-only credential input");
has(page, 'id="confirm-guest-delete"', "guest deletion confirmation checkbox");
has(page, 'id="guest-delete-button"', "guest deletion submit button");
has(page, 'post("/api/account/guest/delete"', "guest deletion API request");
has(page, 'post("/api/account/delete"', "Google-linked deletion API request");
has(page, 'credentials: "omit"', "credential-free cross-origin fetch mode");
has(page, 'cache: "no-store"', "no-store fetch caching");
has(page, 'connect-src \'self\' https://ludoproof-game-api.ai-8f3.workers.dev', "account API Content Security Policy allowlist");
assert.doesNotMatch(page, /localStorage|sessionStorage/, "the page must not persist session or deletion credentials in browser storage");
assert.doesNotMatch(page, /console\.(log|debug|info|warn|error)\s*\(/, "the page must not log credential-bearing UI state");

has(worker, '"/api/account/guest/delete"', "guest deletion CORS allowlist");
has(worker, '"/guest-deletion/register"', "authenticated guest code registration route");
has(worker, '"/guest-deletion/claim"', "guest code claim route");
has(worker, 'ludoproof:guest-deletion-credential:v1:', "domain-separated guest code hash");
has(directory, "deleted_guest_deletion_credentials", "guest deletion replay tombstone");
has(directory, "ACCOUNT_DELETION_IN_PROGRESS", "guest deletion claim lock");
has(player, "body?.guestRecoveryVerified !== true", "guest deletion server-owned verification boundary");
has(policy, "delete-only code", "guest deletion disclosure in privacy-policy draft");
has(policy, "POLICY_RELEASE_REVIEW_REQUIRED", "publisher/legal review marker");

has(pagesWorkflow, "LUDOPROOF_POLICY_RELEASE_APPROVED", "publisher approval gate");
has(pagesWorkflow, "GOOGLE_SIGN_IN_WEB_CLIENT_ID", "Google OAuth Web Client ID release gate");
has(pagesWorkflow, "LUDOPROOF_SUPPORT_EMAIL", "monitored support contact release gate");
has(pagesWorkflow, "LUDOPROOF_ACCOUNT_WEB_ORIGIN", "account-site CORS origin release gate");
has(pagesWorkflow, "actions/deploy-pages", "explicit Pages publishing step");

console.log("Account-deletion source checks passed. Public publication remains gated by publisher/legal, OAuth, support and origin configuration.");
