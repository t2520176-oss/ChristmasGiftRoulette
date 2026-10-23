import { afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest';
import {
  applyD1Migrations, createExecutionContext, env, waitOnExecutionContext,
  type D1Migration,
} from 'cloudflare:test';
import worker, {
  allowedPushEndpoint, cleanup, MAX_PAGE_BYTES, MAX_REQUEST_BYTES, passwordMatches,
  RETENTION_MS, SESSION_MS, sha256, splitMessage, type Env,
} from '../worker/app';

const bindings = env as unknown as Env & { TEST_MIGRATIONS: D1Migration[] };
const ORIGIN = 'https://ourchat.example.test';
const db = bindings.DB;

interface CallOptions {
  method?: string;
  cookie?: string;
  body?: unknown;
  origin?: string | null;
  headers?: Record<string, string>;
  env?: Env;
  rawBody?: string;
}

async function call(path: string, options: CallOptions = {}): Promise<Response> {
  const method = options.method || (options.body === undefined && options.rawBody === undefined ? 'GET' : 'POST');
  const headers = new Headers(options.headers);
  if (options.cookie) headers.set('Cookie', options.cookie);
  if (method !== 'GET') {
    headers.set('Content-Type', headers.get('Content-Type') || 'application/json');
    if (options.origin !== null) headers.set('Origin', options.origin || ORIGIN);
  }
  const context = createExecutionContext();
  const response = await worker.fetch(new Request(`${ORIGIN}${path}`, {
    method, headers, body: options.rawBody ?? (options.body === undefined ? undefined : JSON.stringify(options.body)),
  }), options.env || bindings, context);
  await waitOnExecutionContext(context);
  return response;
}

async function login(member: 1 | 2 = 1): Promise<string> {
  const response = await call('/api/auth/signin', { body: {
    email: member === 1 ? bindings.MEMBER_1_EMAIL : bindings.MEMBER_2_EMAIL,
    password: member === 1 ? bindings.MEMBER_1_PASSWORD : bindings.MEMBER_2_PASSWORD,
  } });
  expect(response.status).toBe(200);
  return response.headers.get('Set-Cookie')!.split(';', 1)[0];
}

async function send(cookie: string, body: string, nonce = crypto.randomUUID()) {
  const response = await call('/api/messages', { cookie, body: { body, client_nonce: nonce } });
  expect(response.status).toBe(201);
  return (await response.json() as { message: { id: string; body: string; client_nonce: string; sender_id: string; created_at: string } }).message;
}

async function seed(body: string, createdAt = Date.now(), member = 'member-1'): Promise<string> {
  const nonce = crypto.randomUUID();
  const result = await db.prepare(`INSERT INTO messages(sender_id, client_nonce, created_at, body_bytes, body_sha256)
    VALUES (?, ?, ?, ?, ?) RETURNING CAST(id AS TEXT) AS id`)
    .bind(member, nonce, createdAt, new TextEncoder().encode(body).length, await sha256(body)).first<{ id: string }>();
  await db.batch(splitMessage(body).map((part, index) => db.prepare(
    'INSERT INTO message_chunks(message_id, chunk_index, body) VALUES (?, ?, ?)',
  ).bind(result!.id, index, part)));
  return result!.id;
}

beforeAll(async () => {
  await applyD1Migrations(db, bindings.TEST_MIGRATIONS);
});

beforeEach(async () => {
  vi.restoreAllMocks();
  vi.spyOn(globalThis, 'fetch').mockRejectedValue(new Error('External network is disabled in backend tests'));
  await db.batch([
    db.prepare('DELETE FROM push_subscriptions'), db.prepare('DELETE FROM sessions'),
    db.prepare('DELETE FROM messages'), db.prepare('DELETE FROM read_receipts'),
    db.prepare('DELETE FROM rate_limits'), db.prepare('DELETE FROM presence'), db.prepare('DELETE FROM app_settings'),
  ]);
});

afterEach(() => { vi.restoreAllMocks(); });

describe('private authentication and request protection', () => {
  it('exposes only a generic health result while every private route needs authentication', async () => {
    const health = await call('/api/health');
    expect(await health.json()).toEqual({ ok: true });
    expect(health.headers.get('Cache-Control')).toContain('no-store');
    for (const path of ['/api/session', '/api/messages', '/api/push/key']) {
      const result = await call(path);
      expect(result.status).toBe(401);
      expect(result.headers.get('Cache-Control')).toBe('private, no-store');
      expect(result.headers.get('Access-Control-Allow-Origin')).toBeNull();
    }
  });

  it('fails closed for missing, short, identical passwords or identical accounts', async () => {
    for (const overrides of [
      { MEMBER_1_PASSWORD: '' },
      { MEMBER_1_PASSWORD: 'seven07' }, { MEMBER_2_PASSWORD: 'seven07' },
      { MEMBER_1_PASSWORD: 'equal008', MEMBER_2_PASSWORD: 'equal008' },
      { MEMBER_2_EMAIL: bindings.MEMBER_1_EMAIL },
    ]) {
      const result = await call('/api/session', { env: { ...bindings, ...overrides } });
      expect(result.status).toBe(503);
      expect(await result.json()).toEqual({ error: 'Chat is not configured yet.' });
    }
  });

  it('lets both members sign in and use their session with different eight-character passwords', async () => {
    const minimumPasswordEnv = { ...bindings, MEMBER_1_PASSWORD: 'first008', MEMBER_2_PASSWORD: 'second08' };
    for (const index of [1, 2] as const) {
      const response = await call('/api/auth/signin', { env: minimumPasswordEnv, body: {
        email: index === 1 ? minimumPasswordEnv.MEMBER_1_EMAIL : minimumPasswordEnv.MEMBER_2_EMAIL,
        password: index === 1 ? minimumPasswordEnv.MEMBER_1_PASSWORD : minimumPasswordEnv.MEMBER_2_PASSWORD,
      } });
      expect(response.status).toBe(200);
      const cookie = response.headers.get('Set-Cookie')!.split(';', 1)[0];
      const session = await call('/api/session', { env: minimumPasswordEnv, cookie });
      expect(session.status).toBe(200);
      expect(await session.json()).toMatchObject({ member: { id: `member-${index}` } });
    }
  });

  it('compares exact passwords through native HMAC verification', async () => {
    expect(await passwordMatches('An exact long password 🎄', 'An exact long password 🎄')).toBe(true);
    expect(await passwordMatches('An exact long password 🎄', 'An exact long password 🎄 ')).toBe(false);
    expect(await passwordMatches('An exact long password 🎄', '')).toBe(false);
  });

  it('gives the same generic error for unknown email and incorrect password', async () => {
    const first = await call('/api/auth/signin', { body: { email: 'unknown@example.test', password: 'wrong' } });
    const second = await call('/api/auth/signin', { body: { email: bindings.MEMBER_1_EMAIL, password: 'wrong' } });
    expect(first.status).toBe(401);
    expect(second.status).toBe(401);
    expect(await first.text()).toBe(await second.text());
    expect(await db.prepare('SELECT COUNT(*) AS count FROM sessions').first('count')).toBe(0);
  });

  it('creates only hashed random sessions, secure host cookies, and public profile fields', async () => {
    const response = await call('/api/auth/signin', { body: {
      email: `  ${bindings.MEMBER_1_EMAIL!.toUpperCase()} `, password: bindings.MEMBER_1_PASSWORD,
    } });
    expect(response.status).toBe(200);
    const cookie = response.headers.get('Set-Cookie')!;
    expect(cookie).toMatch(/^__Host-ourchat-session=[A-Za-z0-9_-]{43};/);
    for (const flag of ['Secure', 'HttpOnly', 'SameSite=Lax', 'Path=/', 'Max-Age=2592000']) expect(cookie).toContain(flag);
    expect(cookie).not.toContain('Domain=');
    const session = await db.prepare('SELECT * FROM sessions').first<Record<string, unknown>>();
    expect(Object.keys(session!).sort()).toEqual(['created_at', 'expires_at', 'token_hash', 'user_id']);
    const token = cookie.split(';')[0].split('=')[1];
    expect(session!.token_hash).toBe(await sha256(token));
    expect(JSON.stringify(session)).not.toContain(token);
    expect(Number(session!.expires_at) - Number(session!.created_at)).toBe(SESSION_MS);
    const profile = await call('/api/session', { cookie: cookie.split(';')[0] });
    expect(await profile.json()).toMatchObject({ member: { id: 'member-1', display_name: 'Husband' }, peer: { id: 'member-2', display_name: 'Wife' } });
  });

  it('rejects forged/expired/duplicate cookies and revokes signout sessions', async () => {
    const cookie = await login();
    for (const value of ['__Host-ourchat-session=forged', `${cookie}; ${cookie}`]) {
      expect((await call('/api/session', { cookie: value })).status).toBe(401);
    }
    expect((await call('/api/auth/signout', { cookie, body: {} })).status).toBe(200);
    expect((await call('/api/session', { cookie })).status).toBe(401);
    const fresh = await login();
    await db.prepare('UPDATE sessions SET expires_at = ?').bind(Date.now() - 1).run();
    expect((await call('/api/session', { cookie: fresh })).status).toBe(401);
  });

  it('keeps the newest sign-in usable when many devices sign in in the same millisecond', async () => {
    vi.spyOn(Date, 'now').mockReturnValue(1_800_000_000_000);
    let newest = '';
    for (let index = 0; index < 12; index++) newest = await login();
    expect((await call('/api/session', { cookie: newest })).status).toBe(200);
    expect(await db.prepare('SELECT COUNT(*) AS count FROM sessions').first('count')).toBe(10);
  });

  it('rate limits sign-in using HMAC network identifiers without storing raw addresses', async () => {
    let response: Response | undefined;
    for (let index = 0; index < 21; index++) response = await call('/api/auth/signin', {
      headers: { 'CF-Connecting-IP': '203.0.113.37' }, body: { email: bindings.MEMBER_1_EMAIL, password: 'wrong' },
    });
    expect(response!.status).toBe(429);
    const rows = await db.prepare('SELECT * FROM rate_limits').all();
    const saved = JSON.stringify(rows.results);
    expect(saved).not.toContain('203.0.113.37');
    expect(saved).not.toContain(bindings.MEMBER_1_EMAIL);
    expect(saved).not.toContain(bindings.MEMBER_1_PASSWORD);
  });

  it('requires exact same origin and JSON for writes, including sign-in', async () => {
    const body = { email: bindings.MEMBER_1_EMAIL, password: bindings.MEMBER_1_PASSWORD };
    for (const origin of [null, 'null', 'https://evil.example.test', `${ORIGIN}.evil.test`]) {
      expect((await call('/api/auth/signin', { origin, body })).status).toBe(403);
    }
    expect((await call('/api/auth/signin', { body, headers: { 'Sec-Fetch-Site': 'cross-site' } })).status).toBe(403);
    expect((await call('/api/auth/signin', { body, headers: { 'Content-Type': 'text/plain' } })).status).toBe(415);
    const cookie = await login();
    expect((await call('/api/read', { cookie, body: { message_id: '1' }, origin: 'https://evil.test' })).status).toBe(403);
    expect((await call('/api/messages', { cookie, method: 'PUT', body: {} })).status).toBe(405);
  });

  it('rejects malformed JSON and bounds streamed/declared request size without a product character cap', async () => {
    const cookie = await login();
    expect((await call('/api/messages', { cookie, rawBody: '{broken' })).status).toBe(400);
    expect((await call('/api/messages', { cookie, body: [] })).status).toBe(400);
    expect((await call('/api/messages', { cookie, body: {}, headers: { 'Content-Length': String(MAX_REQUEST_BYTES + 1) } })).status).toBe(413);
    expect((await call('/api/messages', { cookie, rawBody: ' '.repeat(MAX_REQUEST_BYTES + 1) })).status).toBe(413);
  });

  it('sanitizes provider failures instead of returning queries or credentials', async () => {
    const broken = { ...bindings, DB: { prepare() { throw new Error('private SQL, message and secret must stay private'); } } } as unknown as Env;
    const cookie = await login();
    const result = await call('/api/messages', { cookie, env: broken });
    expect(result.status).toBe(503);
    expect(await result.json()).toEqual({ error: 'Chat is temporarily unavailable. Please try again.' });
  });
});

describe('messages, chunk storage and bounded pagination', () => {
  it('stores exact Unicode, spacing and multiline text, deduplicating sends per member', async () => {
    const husband = await login();
    const wife = await login(2);
    const nonce = crypto.randomUUID();
    const text = '  안녕 🎄\nline 2\t"quotes" \\ dollar $\n ';
    const message = await send(husband, text, nonce);
    expect(message.body).toBe(text);
    expect(message.sender_id).toBe('member-1');
    const retry = await call('/api/messages', { cookie: husband, body: { body: text, client_nonce: nonce } });
    expect(retry.status).toBe(200);
    expect((await retry.json() as any).message.id).toBe(message.id);
    const collision = await call('/api/messages', { cookie: husband, body: { body: 'different', client_nonce: nonce } });
    expect(collision.status).toBe(409);
    await send(wife, 'Same nonce is independent for the other member.', nonce);
    expect(await db.prepare('SELECT COUNT(*) AS count FROM messages').first('count')).toBe(2);
    const listed = await call('/api/messages', { cookie: wife });
    expect((await listed.json() as any).messages[0]).toEqual(message);
  });

  it('keeps concurrent retries atomic for message and chunks', async () => {
    const cookie = await login();
    const nonce = crypto.randomUUID();
    const body = 'A🎄'.repeat(100_000);
    const results = await Promise.all(Array.from({ length: 3 }, () => call('/api/messages', { cookie, body: { body, client_nonce: nonce } })));
    expect(results.every((result) => [200, 201].includes(result.status))).toBe(true);
    const messages = await Promise.all(results.map(async (response) => (await response.json() as any).message));
    expect(new Set(messages.map((message) => message.id)).size).toBe(1);
    expect(messages.every((message) => message.body === body)).toBe(true);
    expect(await db.prepare('SELECT COUNT(*) AS count FROM messages').first('count')).toBe(1);
  });

  it('rejects invalid message shapes/nonces and leaves storage unchanged', async () => {
    const cookie = await login();
    for (const input of [
      { body: '', client_nonce: crypto.randomUUID() }, { body: ' \n\t ', client_nonce: crypto.randomUUID() },
      { body: 42, client_nonce: crypto.randomUUID() }, { body: 'text', client_nonce: 'bad' },
      { body: '\ud800', client_nonce: crypto.randomUUID() },
    ]) expect((await call('/api/messages', { cookie, body: input })).status).toBe(400);
    expect(await db.prepare('SELECT COUNT(*) AS count FROM messages').first('count')).toBe(0);
  });

  it('stores text far beyond D1 single-row limits as intact ordered chunks', async () => {
    const cookie = await login();
    const body = 'A한🎄\n'.repeat(300_000); // 2.7 MB UTF-8, far beyond the old 2,000-character cap.
    expect(new TextEncoder().encode(body).length).toBeGreaterThan(2_000_000);
    const message = await send(cookie, body);
    expect(message.body).toBe(body);
    const chunks = await db.prepare('SELECT length(CAST(body AS BLOB)) AS bytes FROM message_chunks WHERE message_id = ?').bind(message.id).all<{ bytes: number }>();
    expect(chunks.results.length).toBeGreaterThan(1);
    expect(chunks.results.every((chunk) => chunk.bytes < 1_000_000)).toBe(true);
    const page = await call('/api/messages', { cookie });
    expect((await page.json() as any).messages[0].body).toBe(body);
  });

  it('does not split surrogate pairs at storage boundaries', () => {
    const body = `${'a'.repeat(196_607)}🎄${'나'.repeat(300_000)}`;
    const chunks = splitMessage(body);
    expect(chunks.join('')).toBe(body);
    expect(chunks.every((chunk) => !/[\uD800-\uDFFF]/u.test(chunk))).toBe(true);
  });

  it('paginates newest history and ascending updates without omission', async () => {
    const cookie = await login();
    const ids: string[] = [];
    for (let index = 0; index < 53; index++) ids.push(await seed(`synthetic ${index}`));
    const page1 = await (await call('/api/messages', { cookie })).json() as any;
    expect(page1.messages.map((message: any) => message.id)).toEqual(ids.slice(3));
    expect(page1.hasMore).toBe(true);
    const page2 = await (await call(`/api/messages?before=${page1.messages[0].id}`, { cookie })).json() as any;
    expect(page2.messages.map((message: any) => message.id)).toEqual(ids.slice(0, 3));
    expect(page2.hasMore).toBe(false);
    const update1 = await (await call(`/api/messages?after=${ids[0]}`, { cookie })).json() as any;
    expect(update1.messages.map((message: any) => message.id)).toEqual(ids.slice(1, 51));
    expect(update1.hasMore).toBe(true);
    const update2 = await (await call(`/api/messages?after=${ids[50]}`, { cookie })).json() as any;
    expect(update2.messages.map((message: any) => message.id)).toEqual(ids.slice(51));
    expect(update2.hasMore).toBe(false);
    const fromEmpty = await (await call('/api/messages?after=0', { cookie })).json() as any;
    expect(fromEmpty.messages.map((message: any) => message.id)).toEqual(ids.slice(0, 50));
    for (const query of ['before=0', 'after=-1', 'before=9007199254740992', 'before=1&after=2', 'after=1.5', 'after=1%20OR%201=1']) {
      expect((await call(`/api/messages?${query}`, { cookie })).status).toBe(400);
    }
  });

  it('limits serialized pages by bytes while preserving a valid next cursor', async () => {
    const cookie = await login();
    const first = await seed('a'.repeat(2_300_000));
    const second = await seed('b'.repeat(2_300_000));
    const third = await seed('c'.repeat(2_300_000));
    const result = await call('/api/messages', { cookie });
    const serialized = await result.text();
    expect(new TextEncoder().encode(serialized).length).toBeLessThan(MAX_PAGE_BYTES);
    const page = JSON.parse(serialized);
    expect(page.messages.map((message: any) => message.id)).toEqual([third]);
    expect(page.hasMore).toBe(true);
    const before = await (await call(`/api/messages?before=${third}`, { cookie })).json() as any;
    expect(before.messages.map((message: any) => message.id)).toEqual([second]);
    const after = await (await call(`/api/messages?after=${first}`, { cookie })).json() as any;
    expect(after.messages.map((message: any) => message.id)).toEqual([second]);
    expect(after.hasMore).toBe(true);
  });

  it('accounts for JSON escaping when determining the page byte budget', async () => {
    const cookie = await login();
    const text = '\u0001'.repeat(600_000);
    const first = await send(cookie, text);
    const second = await send(cookie, text);
    const result = await call('/api/messages', { cookie });
    const serialized = await result.text();
    expect(new TextEncoder().encode(serialized).length).toBeLessThan(MAX_PAGE_BYTES);
    const page = JSON.parse(serialized);
    expect(page.messages.map((message: any) => message.id)).toEqual([second.id]);
    expect(page.hasMore).toBe(true);
    const earlier = await (await call(`/api/messages?before=${second.id}`, { cookie })).json() as any;
    expect(earlier.messages.map((message: any) => message.id)).toEqual([first.id]);
    expect(earlier.messages[0].body).toBe(text);
  });

  it('moves read receipts only forward and reports the other member presence briefly', async () => {
    const husband = await login();
    const wife = await login(2);
    const first = await send(husband, 'one');
    const second = await send(husband, 'two');
    expect((await call('/api/read', { cookie: wife, body: { message_id: second.id } })).status).toBe(200);
    expect((await call('/api/read', { cookie: wife, body: { message_id: first.id } })).status).toBe(200);
    await call('/api/presence', { cookie: wife, body: { online: true } });
    let page = await (await call('/api/messages', { cookie: husband })).json() as any;
    expect(page.receipts).toContainEqual({ user_id: 'member-2', last_read_message_id: second.id });
    expect(page.peerOnline).toBe(true);
    await db.prepare('UPDATE presence SET last_seen = ?').bind(Date.now() - 46_000).run();
    page = await (await call('/api/messages', { cookie: husband })).json() as any;
    expect(page.peerOnline).toBe(false);
    expect((await call('/api/presence', { cookie: wife, body: { online: 'true' } })).status).toBe(400);
  });
});

describe('held update requests deliver news within about a second', () => {
  const sleep = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));
  const timed = async (path: string, cookie: string) => {
    const started = Date.now();
    const body = await (await call(path, { cookie })).json() as any;
    return { body, ms: Date.now() - started };
  };

  it('answers at once when something newer than the cursor already exists', async () => {
    const husband = await login();
    const first = await seed('already here');
    const second = await seed('also here');
    const { body, ms } = await timed(`/api/messages?after=${first}&wait=10&po=0`, husband);
    expect(body.messages.map((message: any) => message.id)).toEqual([second]);
    expect(ms).toBeLessThan(1500);
  });

  it('holds an idle request and releases it when the other member sends', async () => {
    const husband = await login();
    const wife = await login(2);
    const known = await seed('earlier');
    const started = Date.now();
    const held = timed(`/api/messages?after=${known}&wait=10&since=${Date.now()}`, husband);
    await sleep(1500);
    const sent = await send(wife, 'hello while you wait');
    const { body, ms } = await held;
    expect(body.messages.map((message: any) => message.id)).toEqual([sent.id]);
    expect(ms).toBeGreaterThanOrEqual(1400);
    expect(Date.now() - started).toBeLessThan(5000);
  });

  it('gives up empty after the requested wait and reports the server time to continue from', async () => {
    const husband = await login();
    const known = await seed('quiet');
    const { body, ms } = await timed(`/api/messages?after=${known}&wait=2&since=${Date.now()}&po=0`, husband);
    expect(body.messages).toEqual([]);
    expect(body.hasMore).toBe(false);
    expect(ms).toBeGreaterThanOrEqual(1800);
    expect(ms).toBeLessThan(4500);
    expect(Math.abs(body.serverTime - Date.now())).toBeLessThan(1000);
  });

  it('wakes for the other member read receipt but not for the caller own', async () => {
    const husband = await login();
    const wife = await login(2);
    const mine = await send(husband, 'did you see this');
    const held = timed(`/api/messages?after=${mine.id}&wait=10&since=${Date.now()}`, husband);
    await sleep(1200);
    expect((await call('/api/read', { cookie: wife, body: { message_id: mine.id } })).status).toBe(200);
    const { body, ms } = await held;
    expect(body.messages).toEqual([]);
    expect(body.receipts).toContainEqual({ user_id: 'member-2', last_read_message_id: mine.id });
    expect(ms).toBeLessThan(4500);
    const own = timed(`/api/messages?after=${mine.id}&wait=2&since=${Date.now()}`, husband);
    await sleep(500);
    expect((await call('/api/read', { cookie: husband, body: { message_id: mine.id } })).status).toBe(200);
    expect((await own).ms).toBeGreaterThanOrEqual(1800);
  });

  it('wakes when the other member goes online or away compared with what the caller shows', async () => {
    const husband = await login();
    const wife = await login(2);
    const known = await seed('presence');
    const online = timed(`/api/messages?after=${known}&wait=10&po=0`, husband);
    await sleep(1200);
    await call('/api/presence', { cookie: wife, body: { online: true } });
    const arrived = await online;
    expect(arrived.body.peerOnline).toBe(true);
    expect(arrived.ms).toBeLessThan(4500);
    const away = timed(`/api/messages?after=${known}&wait=10&po=1`, husband);
    await sleep(1200);
    await call('/api/presence', { cookie: wife, body: { online: false } });
    expect((await away).body.peerOnline).toBe(false);
  });

  it('ignores the wait for first loads and older pages, and rejects malformed wait values', async () => {
    const husband = await login();
    const only = await seed('only message');
    expect((await timed('/api/messages?wait=10', husband)).ms).toBeLessThan(1500);
    expect((await timed(`/api/messages?before=${only}&wait=10`, husband)).ms).toBeLessThan(1500);
    for (const query of ['wait=abc', 'wait=100', 'wait=-1', 'wait=1&since=abc', 'wait=1&po=2', 'wait=1.5']) {
      expect((await call(`/api/messages?after=${only}&${query}`, { cookie: husband })).status).toBe(400);
    }
    expect((await timed(`/api/messages?after=${only}&wait=0`, husband)).ms).toBeLessThan(1500);
  });

  it('still requires a valid session while waiting', async () => {
    expect((await call('/api/messages?after=0&wait=10')).status).toBe(401);
  });
});

describe('rolling seven-day visibility and scheduled physical deletion', () => {
  it('hides at exactly 168 hours for both accounts and rejects expired read acknowledgements', async () => {
    const now = 1_800_000_000_000;
    vi.spyOn(Date, 'now').mockReturnValue(now);
    const husband = await login();
    const wife = await login(2);
    await seed('one millisecond too old', now - RETENTION_MS - 1);
    const boundary = await seed('exact boundary', now - RETENTION_MS);
    const visible = await seed('one millisecond fresh', now - RETENTION_MS + 1);
    await db.prepare('INSERT INTO read_receipts(user_id, last_read_message_id, updated_at) VALUES (?, ?, ?)')
      .bind('member-2', boundary, now).run();
    for (const cookie of [husband, wife]) {
      const page = await (await call('/api/messages', { cookie })).json() as any;
      expect(page.messages.map((message: any) => message.id)).toEqual([visible]);
      expect(page.receipts.find((receipt: any) => receipt.user_id === 'member-2').last_read_message_id).toBeNull();
      expect(page.serverTime).toBe(now);
      expect((await call('/api/read', { cookie, body: { message_id: boundary } })).status).toBe(404);
    }
    expect(await db.prepare('SELECT COUNT(*) AS count FROM messages').first('count')).toBe(3);
  });

  it('deletes expired messages/chunks and safely nulls receipts while preserving fresh data', async () => {
    const now = 1_800_000_000_000;
    const expired = await seed('expired body', now - RETENTION_MS);
    const fresh = await seed('fresh body', now - RETENTION_MS + 1);
    await db.batch([
      db.prepare('INSERT INTO read_receipts VALUES (?, ?, ?)').bind('member-1', expired, now),
      db.prepare('INSERT INTO read_receipts VALUES (?, ?, ?)').bind('member-2', fresh, now),
      db.prepare('INSERT INTO sessions VALUES (?, ?, ?, ?)').bind('expired-hash', 'member-1', now - 1000, now),
      db.prepare('INSERT INTO sessions VALUES (?, ?, ?, ?)').bind('fresh-hash', 'member-2', now, now + 1),
      db.prepare('INSERT INTO rate_limits VALUES (?, ?, ?)').bind('old', 1, now),
      db.prepare('INSERT INTO rate_limits VALUES (?, ?, ?)').bind('new', 1, now + 1),
      db.prepare('INSERT INTO presence VALUES (?, ?, ?)').bind('member-1', 1, now - 86_400_000),
      db.prepare('INSERT INTO presence VALUES (?, ?, ?)').bind('member-2', 1, now - 1),
    ]);
    await cleanup(bindings, now);
    expect((await db.prepare('SELECT CAST(id AS TEXT) AS id FROM messages').all()).results).toEqual([{ id: fresh }]);
    expect((await db.prepare('SELECT CAST(message_id AS TEXT) AS id FROM message_chunks').all()).results).toEqual([{ id: fresh }]);
    expect(await db.prepare("SELECT last_read_message_id FROM read_receipts WHERE user_id='member-1'").first('last_read_message_id')).toBeNull();
    expect(String(await db.prepare("SELECT last_read_message_id FROM read_receipts WHERE user_id='member-2'").first('last_read_message_id'))).toBe(fresh);
    expect((await db.prepare('SELECT token_hash FROM sessions').all()).results).toEqual([{ token_hash: 'fresh-hash' }]);
    expect((await db.prepare('SELECT bucket FROM rate_limits').all()).results).toEqual([{ bucket: 'new' }]);
    expect((await db.prepare('SELECT user_id FROM presence').all()).results).toEqual([{ user_id: 'member-2' }]);
  });

  it('bounds each overdue cleanup batch and scheduled handler can clear the rest', async () => {
    const now = Date.now();
    for (let index = 0; index < 105; index++) await seed(`expired ${index}`, now - RETENTION_MS - 1000);
    const fresh = await seed('fresh', now);
    await cleanup(bindings, now);
    expect(await db.prepare('SELECT COUNT(*) AS count FROM messages').first('count')).toBe(6);
    const context = createExecutionContext();
    await worker.scheduled({ scheduledTime: now, cron: '* * * * *', noRetry() {} }, bindings, context);
    await waitOnExecutionContext(context);
    expect((await db.prepare('SELECT CAST(id AS TEXT) AS id FROM messages').all()).results).toEqual([{ id: fresh }]);
  });

  it('does not resurrect an expired nonce during cleanup backlog', async () => {
    const now = 1_800_000_000_000;
    vi.spyOn(Date, 'now').mockReturnValue(now);
    const cookie = await login();
    const id = await seed('already expired', now - RETENTION_MS);
    const nonce = await db.prepare('SELECT client_nonce FROM messages WHERE id = ?').bind(id).first<string>('client_nonce');
    const result = await call('/api/messages', { cookie, body: { body: 'already expired', client_nonce: nonce } });
    expect(result.status).toBe(410);
    expect(await db.prepare('SELECT COUNT(*) AS count FROM messages').first('count')).toBe(1);
  });
});

describe('private push registration and generic native Web Push', () => {
  it('allows only known HTTPS push providers and rejects SSRF/redirect-style addresses', () => {
    for (const endpoint of [
      'https://fcm.googleapis.com/fcm/send/token', 'https://web.push.apple.com/Qtoken',
      'https://updates.push.services.mozilla.com/wpush/v2/token', 'https://wns.notify.windows.com/w/?token=abc',
    ]) expect(allowedPushEndpoint(endpoint)).toBe(true);
    for (const endpoint of [
      'http://fcm.googleapis.com/token', 'https://fcm.googleapis.com.evil.test/token',
      'https://localhost/token', 'https://127.0.0.1/token', 'https://169.254.169.254/latest',
      'https://evil@fcm.googleapis.com/token', 'https://fcm.googleapis.com:444/token',
      'https://fcm.googleapis.com/token#fragment', 'https://fcm.googleapis.com/', 'not a url',
    ]) expect(allowedPushEndpoint(endpoint)).toBe(false);
  });

  it('generates one durable key and never exposes its private component', async () => {
    const cookie = await login();
    const first = await (await call('/api/push/key', { cookie })).json() as any;
    const second = await (await call('/api/push/key', { cookie })).json() as any;
    expect(second).toEqual(first);
    expect(Object.keys(first)).toEqual(['publicKey']);
    expect(first.publicKey).toMatch(/^[A-Za-z0-9_-]{87}$/);
    expect(await db.prepare("SELECT COUNT(*) AS count FROM app_settings WHERE key = 'vapid-v1'").first('count')).toBe(1);
    const privateSettings = JSON.parse(await db.prepare("SELECT value FROM app_settings WHERE key = 'vapid-v1'").first<string>('value') as string);
    expect(privateSettings.privateKey.d).toBeTruthy();
    expect(JSON.stringify(first)).not.toContain(privateSettings.privateKey.d);
  });

  it('registers only authenticated vendor endpoints and scopes removal to the owning user', async () => {
    const husband = await login();
    const wife = await login(2);
    const endpoint = 'https://fcm.googleapis.com/fcm/send/private-token';
    expect((await call('/api/push/subscribe', { body: { endpoint } })).status).toBe(401);
    expect((await call('/api/push/subscribe', { cookie: wife, body: { endpoint: 'https://evil.test/token' } })).status).toBe(400);
    expect((await call('/api/push/subscribe', { cookie: wife, body: { endpoint, keys: { auth: 'not-stored', p256dh: 'not-stored' } } })).status).toBe(200);
    await call('/api/push/subscribe', { method: 'DELETE', cookie: husband, body: { endpoint } });
    const rows = await db.prepare('SELECT * FROM push_subscriptions').all();
    expect(rows.results).toHaveLength(1);
    expect(JSON.stringify(rows)).not.toContain('not-stored');
    await call('/api/push/subscribe', { method: 'DELETE', cookie: wife, body: { endpoint } });
    expect(await db.prepare('SELECT COUNT(*) AS count FROM push_subscriptions').first('count')).toBe(0);
  });

  it('revokes device push automatically on logout through the session foreign key', async () => {
    const cookie = await login(2);
    await call('/api/push/subscribe', { cookie, body: { endpoint: 'https://fcm.googleapis.com/fcm/send/token' } });
    expect(await db.prepare('SELECT COUNT(*) AS count FROM push_subscriptions').first('count')).toBe(1);
    await call('/api/auth/signout', { cookie, body: {} });
    expect(await db.prepare('SELECT COUNT(*) AS count FROM push_subscriptions').first('count')).toBe(0);
  });

  it('sends an empty generic push to the peer provider and removes gone subscriptions', async () => {
    const husband = await login();
    const wife = await login(2);
    const endpoint = 'https://fcm.googleapis.com/fcm/send/gone-token';
    await call('/api/push/subscribe', { cookie: wife, body: { endpoint } });
    let authorization = '';
    let observedBody: unknown;
    vi.mocked(globalThis.fetch).mockImplementation(async (url, options) => {
      expect(url).toBe(endpoint);
      expect(options?.method).toBe('POST');
      expect(options?.redirect).toBe('manual');
      authorization = new Headers(options?.headers).get('Authorization') || '';
      observedBody = options?.body;
      return new Response('', { status: 410 });
    });
    await send(husband, 'Private text must never appear in a push payload.');
    expect(authorization).toMatch(/^vapid t=[A-Za-z0-9_.-]+,k=[A-Za-z0-9_-]{87}$/);
    const jwt = authorization.split('t=')[1].split(',k=')[0];
    const publicPart = authorization.split(',k=')[1];
    const [header, claims, signature] = jwt.split('.');
    const decode = (value: string) => Uint8Array.from(atob(value.replace(/-/g, '+').replace(/_/g, '/')), (character) => character.charCodeAt(0));
    const decodedClaims = JSON.parse(new TextDecoder().decode(decode(claims)));
    expect(decodedClaims.aud).toBe('https://fcm.googleapis.com');
    expect(decodedClaims.sub).toBe(ORIGIN);
    expect(decodedClaims.exp).toBeGreaterThan(Date.now() / 1000);
    expect(decodedClaims.exp).toBeLessThan(Date.now() / 1000 + 86400);
    const verifyingKey = await crypto.subtle.importKey('raw', decode(publicPart), { name: 'ECDSA', namedCurve: 'P-256' }, false, ['verify']);
    expect(await crypto.subtle.verify({ name: 'ECDSA', hash: 'SHA-256' }, verifyingKey, decode(signature), new TextEncoder().encode(`${header}.${claims}`))).toBe(true);
    expect(observedBody === undefined || observedBody === null || observedBody === '').toBe(true);
    expect(await db.prepare('SELECT COUNT(*) AS count FROM push_subscriptions').first('count')).toBe(0);
  });
});
