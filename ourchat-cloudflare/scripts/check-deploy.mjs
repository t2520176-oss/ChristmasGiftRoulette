#!/usr/bin/env node
/** Offline template checks only: no Cloudflare API calls, login, or deployment. */
import { readFileSync, readdirSync, lstatSync } from "node:fs";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const root = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const secretNames = ["MEMBER_1_EMAIL", "MEMBER_1_PASSWORD", "MEMBER_2_EMAIL", "MEMBER_2_PASSWORD"];
const provisioned = process.argv.length === 3 && process.argv[2] === "--provisioned";
const errors = [];
const requireCheck = (condition, message) => { if (!condition) errors.push(message); };

function jsonc(source) {
  let clean = "";
  let string = false;
  let escaped = false;
  for (let i = 0; i < source.length; i++) {
    const char = source[i];
    if (string) {
      clean += char;
      if (escaped) escaped = false;
      else if (char === "\\") escaped = true;
      else if (char === '"') string = false;
    } else if (char === '"') {
      string = true;
      clean += char;
    } else if (char === "/" && source[i + 1] === "/") {
      while (i < source.length && source[i] !== "\n") i++;
      clean += "\n";
    } else if (char === "/" && source[i + 1] === "*") {
      i += 2;
      while (i < source.length && !(source[i] === "*" && source[i + 1] === "/")) {
        clean += source[i] === "\n" ? "\n" : " ";
        i++;
      }
      if (i >= source.length) throw new Error("Unclosed configuration comment");
      i++;
    } else clean += char;
  }
  let normalized = "";
  string = false;
  escaped = false;
  for (let i = 0; i < clean.length; i++) {
    const char = clean[i];
    if (string) {
      normalized += char;
      if (escaped) escaped = false;
      else if (char === "\\") escaped = true;
      else if (char === '"') string = false;
    } else {
      if (char === '"') string = true;
      if (char === ",") {
        let next = i + 1;
        while (/\s/.test(clean[next] ?? "") && next < clean.length) next++;
        if (clean[next] === "}" || clean[next] === "]") continue;
      }
      normalized += char;
    }
  }
  return JSON.parse(normalized);
}

try {
  requireCheck(process.argv.length === 2 || provisioned, "Use no arguments for the public template, or --provisioned for the deployment build.");
  const config = jsonc(readFileSync(join(root, "wrangler.jsonc"), "utf8"));
  const pkg = JSON.parse(readFileSync(join(root, "package.json"), "utf8"));
  requireCheck((pkg.devDependencies?.wrangler ?? pkg.dependencies?.wrangler) === "4.151.2", "Pin Wrangler to exactly 4.151.2.");
  requireCheck(typeof config.name === "string" && /^[a-z][a-z0-9-]{1,62}$/.test(config.name), "Set a valid Worker name; confirm separately that it is new in the selected account.");
  if (!provisioned) requireCheck(!("account_id" in config), "Keep account_id out of the public template; select the new account in Cloudflare or set CLOUDFLARE_ACCOUNT_ID privately for CLI use.");
  requireCheck(!("routes" in config) && !("route" in config) && !("zone_id" in config), "The template must use a new workers.dev address without changing existing domains.");
  requireCheck(config.workers_dev !== false, "Keep workers.dev enabled for the new app.");
  const databases = config.d1_databases;
  requireCheck(Array.isArray(databases) && databases.length === 1, "Declare exactly one D1 binding.");
  if (Array.isArray(databases) && databases.length === 1) {
    const database = databases[0];
    requireCheck(database.binding === "DB", "Use the D1 binding name DB.");
    if (!provisioned) requireCheck(!("database_name" in database) && !("database_id" in database), "Omit database_name and database_id so the public template cannot select an existing D1 database.");
    else {
      requireCheck(database.database_id === undefined || (typeof database.database_id === "string" && /^[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}$/i.test(database.database_id)), "The provisioned D1 ID must be a UUID if present.");
      requireCheck(database.database_name === undefined || (typeof database.database_name === "string" && database.database_name.length > 0), "The provisioned D1 name must be a nonempty string if present.");
    }
    requireCheck(!("preview_database_id" in database), "Do not pin a preview D1 database in the public template.");
    requireCheck((database.migrations_dir ?? "migrations") === "migrations", "Keep D1 migrations in migrations/.");
  }
  requireCheck(config.triggers?.crons?.length === 1 && config.triggers.crons[0] === "* * * * *", "Configure the retention Cron to run every minute.");
  for (const name of secretNames) {
    requireCheck(!(name in (config.vars ?? {})), "Member credentials must be encrypted secrets, not Wrangler vars.");
  }
  const placeholders = new Map();
  for (const line of readFileSync(join(root, ".dev.vars.example"), "utf8").split(/\r?\n/)) {
    if (!line.trim() || line.trimStart().startsWith("#")) continue;
    const match = /^([A-Z][A-Z0-9_]*)\s*=(.*)$/.exec(line.trim());
    requireCheck(Boolean(match), "Use blank NAME= entries in .dev.vars.example.");
    if (match) {
      requireCheck(!placeholders.has(match[1]), "Do not duplicate secret placeholders.");
      requireCheck(["", '""', "''"].includes(match[2].trim()), "The public secret example must contain no values.");
      placeholders.set(match[1], true);
    }
  }
  requireCheck(placeholders.size === secretNames.length && secretNames.every(name => placeholders.has(name)), "Declare the four member secret names in .dev.vars.example.");
  const migrationFiles = readdirSync(join(root, "migrations")).filter(name => name.endsWith(".sql"));
  requireCheck(migrationFiles.length > 0, "Include the reviewed D1 schema migration.");
  requireCheck(migrationFiles.every(name => lstatSync(join(root, "migrations", name)).isFile()), "Migrations must be regular files.");
  const deploy = pkg.scripts?.deploy ?? "";
  const referenced = [...deploy.matchAll(/npm run ([a-zA-Z0-9:_-]+)/g)].map(match => pkg.scripts?.[match[1]] ?? "");
  const commands = [deploy, ...referenced].join("\n");
  requireCheck(/wrangler\s+d1\s+migrations\s+apply\s+DB\s+--remote\b/.test(commands), "Apply remote migrations by the DB binding name in the deploy script.");
  requireCheck(/wrangler\s+deploy\b/.test(deploy), "Include Worker deployment in the deploy script.");
  if (errors.length) {
    for (const message of [...new Set(errors)]) console.error(`Check failed: ${message}`);
    process.exitCode = 1;
  } else {
    console.log(`${provisioned ? "Deployment configuration" : "Public deployment template"} checks passed. No network calls or changes were made.`);
    console.log("Select the separate new Cloudflare account and a new Worker name before deployment. This offline check does not verify account identity, resource freshness, deployed secrets, or production behavior.");
  }
} catch {
  console.error("Deployment template check could not read or validate the required public files. No file contents or secret values were printed.");
  process.exitCode = 1;
}
