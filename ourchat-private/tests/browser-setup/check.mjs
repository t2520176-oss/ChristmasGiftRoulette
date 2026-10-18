import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { randomUUID, createECDH } from "node:crypto";
import { resolve } from "node:path";
import { createServer } from "node:http";
import { chromium } from "playwright";
import { PGlite } from "@electric-sql/pglite";

const setup = resolve(process.argv[2] || "/tmp/OurChat-Start-test.html");
// The cloud Chromium policy disallows file: navigation. Serve the exact files on
// loopback for behavior checks; actual Windows file opening remains unverified.
const server = createServer(async (request, response) => {
  const path = request.url === "/setup" ? setup : request.url === "/vapid" ? resolve("tools/generate-vapid.html") : null;
  if (!path) { response.writeHead(404).end(); return; }
  response.setHeader("Content-Type", "text/html; charset=utf-8");
  response.end(await readFile(path));
});
await new Promise((done) => server.listen(0, "127.0.0.1", done));
const base = `http://127.0.0.1:${server.address().port}`;
const browser = await chromium.launch({ executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || "/usr/bin/chromium", headless: true });
const first = randomUUID();
const second = randomUUID();
let checks = 0;
async function test(name, run) {
  await run(); checks++; console.log(`ok ${checks} - ${name}`);
}
let sql;
try {
  const page = await browser.newPage();
  const errors = [];
  const outbound = [];
  page.on("pageerror", (error) => errors.push(error.message));
  page.on("request", (request) => { if (!request.url().startsWith(base + "/")) outbound.push(request.url()); });
  await page.goto(base + "/setup");
  await test("setup is a helper with no automatic deployment or SQL execution", async () => {
    assert.match(await page.locator("body").innerText(), /설치가 완료되지는 않습니다/);
    assert.equal(await page.locator("#copy-sql").isDisabled(), true);
    assert.equal(await page.locator("#open-app").isDisabled(), true);
    const deploy = new URL(await page.locator("#deploy").getAttribute("href"));
    assert.equal(deploy.origin, "https://vercel.com");
    assert.equal(deploy.pathname, "/new/clone");
    assert.match(deploy.searchParams.get("repository-url"), /\/tree\/[a-f0-9]{40}\/ourchat-private$/);
    assert.equal(deploy.searchParams.get("env"), "NEXT_PUBLIC_SUPABASE_URL,NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY");
  });
  await test("SQL requires valid distinct UUIDs and new-project confirmation", async () => {
    await page.locator("#first-uuid").fill("bad'; drop table public.members; --");
    await page.locator("#second-uuid").fill(second);
    await page.locator("#new-project").check();
    assert.equal(await page.locator("#copy-sql").isDisabled(), true);
    await page.locator("#first-uuid").fill(second);
    assert.equal(await page.locator("#copy-sql").isDisabled(), true);
    await page.locator("#first-uuid").fill(first);
    await page.locator("#new-project").uncheck();
    assert.equal(await page.locator("#copy-sql").isDisabled(), true);
    await page.locator("#new-project").check();
    assert.equal(await page.locator("#copy-sql").isDisabled(), false);
    sql = await page.locator("#sql").inputValue();
    assert.ok(sql.includes(`first_auth_uuid_text text := '${first}'`));
    assert.ok(sql.includes("first_auth_uuid_text = 'REPLACE_FIRST_AUTH_UUID'"));
  });
  await test("copy fallback exposes selected SQL when clipboard is unavailable", async () => {
    await page.evaluate(() => Object.defineProperty(navigator, "clipboard", { value: { writeText: async () => { throw new Error("unsupported"); } }, configurable: true }));
    await page.locator("#copy-sql").click();
    assert.equal(await page.locator("#sql").isVisible(), true);
    assert.match(await page.locator("#status").innerText(), /Ctrl\+C/);
  });
  await test("application URL rejects insecure, unrelated, credential and path targets", async () => {
    for (const invalid of ["javascript:alert(1)", "http://new.vercel.app", "https://new.vercel.app.evil.example", "https://user:pass@new.vercel.app", "https://new.vercel.app/other", "https://new.vercel.app/?secret=value"]) {
      await page.locator("#app-url").fill(invalid);
      assert.equal(await page.locator("#open-app").isDisabled(), true);
    }
    await page.locator("#app-url").fill("https://ourchat-new.vercel.app/login");
    assert.equal(await page.locator("#open-app").isDisabled(), false);
    await page.locator("#copy-origin").click();
    assert.equal(await page.locator("#app-url").inputValue(), "https://ourchat-new.vercel.app");
  });
  await test("helper makes no network requests and retains no inputs after reload", async () => {
    await page.reload();
    assert.equal(await page.locator("#first-uuid").inputValue(), "");
    assert.equal(await page.locator("#app-url").inputValue(), "");
    assert.deepEqual(outbound, []);
    assert.deepEqual(errors, []);
  });

  const keyPage = await browser.newPage();
  const keyOutbound = [];
  keyPage.on("request", (request) => { if (!request.url().startsWith(base + "/")) keyOutbound.push(request.url()); });
  await keyPage.goto(base + "/vapid");
  await test("offline push helper generates matching keys without network or storage", async () => {
    assert.equal(await keyPage.locator("#private-key").inputValue(), "");
    await keyPage.locator("#generate").click();
    await keyPage.waitForFunction(() => document.getElementById("private-key").value.length > 0);
    const publicKey = await keyPage.locator("#public-key").inputValue();
    const privateKey = await keyPage.locator("#private-key").inputValue();
    const ecdh = createECDH("prime256v1");
    ecdh.setPrivateKey(Buffer.from(privateKey, "base64url"));
    assert.ok(ecdh.getPublicKey().equals(Buffer.from(publicKey, "base64url")));
    assert.equal(await keyPage.locator("#private-key").getAttribute("type"), "password");
    await keyPage.evaluate(() => { window.confirm = () => false; });
    await keyPage.locator("#generate").click();
    assert.equal(await keyPage.locator("#private-key").inputValue(), privateKey);
    assert.deepEqual(keyOutbound, []);
    await keyPage.reload();
    assert.equal(await keyPage.locator("#private-key").inputValue(), "");
  });
} finally { await browser.close(); server.close(); }

async function database(users = [first, second]) {
  const db = new PGlite();
  await db.exec(`create role anon nologin; create role authenticated nologin;
    create role service_role nologin bypassrls;
    create schema auth; create table auth.users(id uuid primary key);
    create function auth.uid() returns uuid language sql stable as $$select null::uuid$$;
    grant usage on schema public,auth to anon,authenticated,service_role;
    grant execute on function auth.uid() to anon,authenticated,service_role;`);
  for (const user of users) await db.query("insert into auth.users(id) values ($1)", [user]);
  return db;
}
async function rejected(db) {
  await assert.rejects(db.exec(sql));
  await db.exec("rollback");
}
await test("combined setup creates tables and exactly two supplied members atomically", async () => {
  const db = await database();
  try {
    await db.exec(sql);
    const result = await db.query("select user_id from public.members order by user_id");
    assert.deepEqual(result.rows.map((row) => row.user_id), [first, second].sort());
    await rejected(db);
    assert.equal((await db.query("select count(*)::int as n from public.members")).rows[0].n, 2);
  } finally { await db.close(); }
});
await test("existing application table, row, RLS and policy remain untouched", async () => {
  const db = await database();
  try {
    await db.exec("create table public.existing_work(id int); insert into public.existing_work values(7); alter table public.existing_work enable row level security; create policy existing_policy on public.existing_work for select using(true)");
    await rejected(db);
    assert.equal((await db.query("select id from public.existing_work")).rows[0].id, 7);
    assert.equal((await db.query("select relrowsecurity from pg_class where oid='public.existing_work'::regclass")).rows[0].relrowsecurity, true);
    assert.equal((await db.query("select count(*)::int as n from pg_policies where policyname='existing_policy'")).rows[0].n, 1);
    assert.equal((await db.query("select to_regclass('public.members')::text as name")).rows[0].name, null);
  } finally { await db.close(); }
});
await test("existing application function or private schema prevents setup", async () => {
  for (const statement of ["create function public.existing_work() returns int language sql as $$select 8$$", "create schema private; create table private.existing_work(id int)"]) {
    const db = await database();
    try {
      await db.exec(statement);
      await rejected(db);
      assert.equal((await db.query("select to_regclass('public.members')::text as name")).rows[0].name, null);
    } finally { await db.close(); }
  }
});
await test("missing Auth user rolls back schema and membership creation together", async () => {
  const db = await database([first]);
  try {
    await rejected(db);
    assert.equal((await db.query("select to_regclass('public.members')::text as name")).rows[0].name, null);
    assert.equal((await db.query("select count(*)::int as n from pg_namespace where nspname='private'")).rows[0].n, 0);
    assert.equal((await db.query("select count(*)::int as n from auth.users")).rows[0].n, 1);
  } finally { await db.close(); }
});
await test("project with more than two Auth users is rejected before any setup", async () => {
  const db = await database([first, second, randomUUID()]);
  try {
    await rejected(db);
    assert.equal((await db.query("select to_regclass('public.members')::text as name")).rows[0].name, null);
    assert.equal((await db.query("select count(*)::int as n from auth.users")).rows[0].n, 3);
  } finally { await db.close(); }
});
assert.ok((await readFile(setup, "utf8")).includes("connect-src 'none'"));
console.log(`PASS: ${checks} browser-helper and database preservation scenarios. No hosted accounts were accessed.`);
