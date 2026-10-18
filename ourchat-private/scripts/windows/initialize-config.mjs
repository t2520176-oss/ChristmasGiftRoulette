import fs from "node:fs";
import webpush from "web-push";

// Only the per-user config path crosses the process boundary; no key is printed.
const configPath = process.env.OURCHAT_CONFIG_PATH;
if (!configPath) throw new Error("Missing private config path");
let text = fs.readFileSync(configPath, "utf8");
const read = (name) => text.match(new RegExp(`^${name}=(.*)$`, "m"))?.[1].trim() ?? "";
const set = (name, value) => {
  const expression = new RegExp(`^${name}=.*$`, "m");
  text = expression.test(text) ? text.replace(expression, `${name}=${value}`) : `${text}\n${name}=${value}\n`;
};
const publicKey = read("NEXT_PUBLIC_VAPID_PUBLIC_KEY");
const privateKey = read("VAPID_PRIVATE_KEY");
if (Boolean(publicKey) !== Boolean(privateKey)) {
  throw new Error("Incomplete existing VAPID pair. Restore the matching key instead of rotating automatically.");
}
if (!publicKey && !privateKey) {
  const keys = webpush.generateVAPIDKeys();
  set("NEXT_PUBLIC_VAPID_PUBLIC_KEY", keys.publicKey);
  set("VAPID_PRIVATE_KEY", keys.privateKey);
}
if (!read("NEXT_PUBLIC_SITE_URL")) set("NEXT_PUBLIC_SITE_URL", "http://localhost:3000");
fs.writeFileSync(configPath, text, { mode: 0o600 });
