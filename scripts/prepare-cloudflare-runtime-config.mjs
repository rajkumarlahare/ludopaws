import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { buildCloudflareRuntimeVars } from "./cloudflare-runtime-config.mjs";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const configPath = path.join(root, "server", "wrangler.json");
const config = JSON.parse(fs.readFileSync(configPath, "utf8"));
config.vars = buildCloudflareRuntimeVars(config.vars ?? {}, process.env);
fs.writeFileSync(configPath, JSON.stringify(config, null, 2) + "\n", "utf8");

console.log("Prepared explicit Cloudflare Worker runtime variables.");
console.log("Billing runtime enabled: " + config.vars.GOOGLE_PLAY_BILLING_ENABLED);
console.log("AdMob SSV runtime enabled: " + config.vars.LUDOPROOF_ADMOB_SSV_ENABLED);
console.log("Secrets are not read, printed, or written by this configuration step.");
