/** OurChat's isolated Cloudflare backend. No Supabase/Vercel connections. */
export interface Env {
  DB: D1Database;
  ASSETS: Fetcher;
  MEMBER_1_EMAIL?: string;
  MEMBER_1_PASSWORD?: string;
  MEMBER_2_EMAIL?: string;
  MEMBER_2_PASSWORD?: string;
}

export const RETENTION_MS = 168 * 60 * 60 * 1000;
export const MAX_REQUEST_BYTES = 4 * 1024 * 1024;
export const MAX_PAGE_BYTES = 4_400_000;
export const SESSION_MS = 30 * 24 * 60 * 60 * 1000;
const COOKIE_NAME = '__Host-ourchat-session';
const encoder = new TextEncoder();
const PASSWORD_CONTEXT = encoder.encode('OurChat Cloudflare password verification v1');
const MESSAGE_LIMIT = 50;
const CHUNK_CODE_UNITS = 196_608;
const MAX_ID = BigInt(Number.MAX_SAFE_INTEGER);

type MemberId = 'member-1' | 'member-2';
interface Member { id: MemberId; display_name: string }
interface MemberConfig extends Member { email: string; password: string }
interface Config { members: [MemberConfig, MemberConfig] }
interface Auth { member: MemberConfig; peer: MemberConfig; tokenHash: string; config: Config }
interface MessageRow {
  id: string;
  sender_id: MemberId;
  client_nonce: string;
  created_at: number;
  body_bytes: number;
  body_sha256: string;
}
export interface ChatMessage {
  id: string;
  sender_id: MemberId;
  body: string;
  client_nonce: string;
  created_at: string;
}

class HttpError extends Error {
  constructor(public readonly status: number, public readonly publicMessage: string) {
    super(publicMessage);
  }
}

function json(value: unknown, status = 200, extraHeaders?: HeadersInit): Response {
  const headers = new Headers(extraHeaders);
  headers.set('Content-Type', 'application/json; charset=utf-8');
  headers.set('Cache-Control', 'private, no-store');
  headers.set('Vary', 'Cookie');
  headers.set('X-Content-Type-Options', 'nosniff');
  headers.set('Referrer-Policy', 'no-referrer');
  return new Response(JSON.stringify(value), { status, headers });
}

function publicMember(member: Member): Member {
  return { id: member.id, display_name: member.display_name };
}

export function base64url(bytes: Uint8Array): string {
  let binary = '';
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/g, '');
}

export async function sha256(value: string): Promise<string> {
  const bytes = new Uint8Array(await crypto.subtle.digest('SHA-256', encoder.encode(value)));
  return Array.from(bytes, (byte) => byte.toString(16).padStart(2, '0')).join('');
}

async function hmac(key: string, value: Uint8Array): Promise<Uint8Array> {
  const imported = await crypto.subtle.importKey(
    'raw', encoder.encode(key), { name: 'HMAC', hash: 'SHA-256' }, false, ['sign'],
  );
  return new Uint8Array(await crypto.subtle.sign('HMAC', imported, value));
}

/** WebCrypto performs the MAC comparison; JS never compares password hashes. */
export async function passwordMatches(configured: string, candidate: string): Promise<boolean> {
  const [key, signature] = await Promise.all([
    crypto.subtle.importKey('raw', encoder.encode(configured), { name: 'HMAC', hash: 'SHA-256' }, false, ['verify']),
    hmac(candidate || '\u0000', PASSWORD_CONTEXT),
  ]);
  return crypto.subtle.verify('HMAC', key, signature, PASSWORD_CONTEXT);
}

function normalizedEmail(value: string): string {
  return value.trim().toLowerCase();
}

async function configuration(env: Env): Promise<Config> {
  const email1 = normalizedEmail(env.MEMBER_1_EMAIL || '');
  const email2 = normalizedEmail(env.MEMBER_2_EMAIL || '');
  const password1 = env.MEMBER_1_PASSWORD || '';
  const password2 = env.MEMBER_2_PASSWORD || '';
  const emailValid = (email: string) => email.length <= 254 && /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
  if (!emailValid(email1) || !emailValid(email2) || email1 === email2
      || password1.length < 8 || password2.length < 8 || password1 === password2
      || encoder.encode(password1).length > 1024 || encoder.encode(password2).length > 1024) {
    throw new HttpError(503, 'Chat is not configured yet.');
  }
  return {
    members: [
      { id: 'member-1', display_name: 'Husband', email: email1, password: password1 },
      { id: 'member-2', display_name: 'Wife', email: email2, password: password2 },
    ],
  };
}

function sameOrigin(request: Request): void {
  const url = new URL(request.url);
  const origin = request.headers.get('Origin');
  if (origin !== url.origin || request.headers.get('Sec-Fetch-Site') === 'cross-site') {
    throw new HttpError(403, 'Request not allowed.');
  }
  if (request.headers.get('Content-Type')?.split(';', 1)[0].trim().toLowerCase() !== 'application/json') {
    throw new HttpError(415, 'Use application/json.');
  }
}

export async function readJson(request: Request): Promise<Record<string, unknown>> {
  const contentLength = request.headers.get('Content-Length');
  if (contentLength && (!/^\d+$/.test(contentLength) || Number(contentLength) > MAX_REQUEST_BYTES)) {
    throw new HttpError(413, 'Message is too large for one request.');
  }
  if (!request.body) throw new HttpError(400, 'Invalid request.');
  const reader = request.body.getReader();
  const chunks: Uint8Array[] = [];
  let length = 0;
  try {
    while (true) {
      const next = await reader.read();
      if (next.done) break;
      length += next.value.byteLength;
      if (length > MAX_REQUEST_BYTES) {
        await reader.cancel();
        throw new HttpError(413, 'Message is too large for one request.');
      }
      chunks.push(next.value);
    }
  } finally {
    reader.releaseLock();
  }
  const bytes = new Uint8Array(length);
  let offset = 0;
  for (const chunk of chunks) { bytes.set(chunk, offset); offset += chunk.length; }
  try {
    const value: unknown = JSON.parse(new TextDecoder('utf-8', { fatal: true, ignoreBOM: false }).decode(bytes));
    if (!value || typeof value !== 'object' || Array.isArray(value)) throw new Error();
    return value as Record<string, unknown>;
  } catch {
    throw new HttpError(400, 'Invalid request.');
  }
}

function parseId(value: unknown, required = true): string | null {
  if ((value === null || value === undefined || value === '') && !required) return null;
  if (typeof value !== 'string' || !/^[1-9]\d{0,15}$/.test(value) || BigInt(value) > MAX_ID) {
    throw new HttpError(400, 'Invalid message ID.');
  }
  return value;
}

function sessionCookie(token: string, maxAge = SESSION_MS / 1000): string {
  return `${COOKIE_NAME}=${token}; Path=/; Max-Age=${maxAge}; Secure; HttpOnly; SameSite=Lax`;
}

async function authentication(request: Request, env: Env): Promise<Auth> {
  const config = await configuration(env);
  const cookie = request.headers.get('Cookie') || '';
  const tokens = cookie.split(';').map((piece) => piece.trim()).filter((piece) => piece.startsWith(`${COOKIE_NAME}=`));
  if (tokens.length !== 1) throw new HttpError(401, 'Please sign in.');
  const token = tokens[0].slice(COOKIE_NAME.length + 1);
  if (!/^[A-Za-z0-9_-]{43}$/.test(token)) throw new HttpError(401, 'Please sign in.');
  const tokenHash = await sha256(token);
  const row = await env.DB.prepare(
    'SELECT user_id FROM sessions WHERE token_hash = ? AND expires_at > ?',
  ).bind(tokenHash, Date.now()).first<{ user_id: MemberId }>();
  const member = config.members.find((entry) => entry.id === row?.user_id);
  if (!member) throw new HttpError(401, 'Please sign in.');
  return { member, peer: config.members[member.id === 'member-1' ? 1 : 0], tokenHash, config };
}

async function consumeBuckets(env: Env, buckets: { key: string; limit: number; duration: number }[]): Promise<void> {
  const now = Date.now();
  const results = await env.DB.batch(buckets.map(({ key, duration }) => env.DB.prepare(`
    INSERT INTO rate_limits(bucket, count, expires_at) VALUES (?, 1, ?)
    ON CONFLICT(bucket) DO UPDATE SET
      count = CASE WHEN rate_limits.expires_at <= ? THEN 1 ELSE rate_limits.count + 1 END,
      expires_at = CASE WHEN rate_limits.expires_at <= ? THEN excluded.expires_at ELSE rate_limits.expires_at END
    RETURNING count
  `).bind(key, now + duration, now, now)));
  if (results.some((result, index) => Number((result.results[0] as { count: number }).count) > buckets[index].limit)) {
    throw new HttpError(429, 'Too many attempts. Please try again later.');
  }
}

async function rateIdentifierKey(env: Env): Promise<string> {
  const existing = await env.DB.prepare("SELECT value FROM app_settings WHERE key = 'rate-key-v1'").first<{ value: string }>();
  if (existing) return existing.value;
  const generated = base64url(crypto.getRandomValues(new Uint8Array(32)));
  await env.DB.prepare("INSERT OR IGNORE INTO app_settings(key, value) VALUES ('rate-key-v1', ?)").bind(generated).run();
  const winner = await env.DB.prepare("SELECT value FROM app_settings WHERE key = 'rate-key-v1'").first<{ value: string }>();
  if (!winner) throw new Error('Rate identifier key unavailable');
  return winner.value;
}

async function signin(request: Request, env: Env): Promise<Response> {
  const config = await configuration(env);
  const body = await readJson(request);
  if (typeof body.email !== 'string' || body.email.length > 254
      || typeof body.password !== 'string' || encoder.encode(body.password).length > 1024) {
    throw new HttpError(401, 'Email or password is incorrect.');
  }
  const email = normalizedEmail(body.email);
  const ip = request.headers.get('CF-Connecting-IP') || 'unavailable';
  const identifier = base64url(await hmac(await rateIdentifierKey(env), encoder.encode(`login-ip:${ip}`)));
  const account = config.members.find((member) => member.email === email)?.id || 'unknown';
  await consumeBuckets(env, [
    { key: 'login:global', limit: 200, duration: 15 * 60 * 1000 },
    { key: `login:ip:${identifier}`, limit: 20, duration: 15 * 60 * 1000 },
    { key: `login:account:${account}`, limit: 60, duration: 15 * 60 * 1000 },
  ]);
  const comparisons = await Promise.all(config.members.map((member) => passwordMatches(member.password, body.password as string)));
  const index = config.members.findIndex((member, memberIndex) => member.email === email && comparisons[memberIndex]);
  if (index === -1) throw new HttpError(401, 'Email or password is incorrect.');
  const member = config.members[index];
  const token = base64url(crypto.getRandomValues(new Uint8Array(32)));
  const tokenHash = await sha256(token);
  const now = Date.now();
  await env.DB.batch([
    env.DB.prepare('INSERT INTO sessions(token_hash, user_id, created_at, expires_at) VALUES (?, ?, ?, ?)')
      .bind(tokenHash, member.id, now, now + SESSION_MS),
    env.DB.prepare(`DELETE FROM sessions WHERE token_hash IN (
      SELECT token_hash FROM sessions WHERE user_id = ? AND token_hash <> ?
      ORDER BY created_at DESC, token_hash DESC LIMIT -1 OFFSET 9
    )`).bind(member.id, tokenHash),
  ]);
  return json({ member: publicMember(member), peer: publicMember(config.members[index === 0 ? 1 : 0]), serverTime: now }, 200,
    { 'Set-Cookie': sessionCookie(token) });
}

export function splitMessage(body: string): string[] {
  const parts: string[] = [];
  for (let offset = 0; offset < body.length;) {
    let end = Math.min(offset + CHUNK_CODE_UNITS, body.length);
    const last = body.charCodeAt(end - 1);
    if (end < body.length && last >= 0xd800 && last <= 0xdbff) end -= 1;
    parts.push(body.slice(offset, end));
    offset = end;
  }
  return parts;
}

async function loadMessage(env: Env, row: MessageRow): Promise<ChatMessage> {
  const chunks = await env.DB.prepare('SELECT body FROM message_chunks WHERE message_id = ? ORDER BY chunk_index')
    .bind(row.id).all<{ body: string }>();
  const body = chunks.results.map((part) => part.body).join('');
  if (encoder.encode(body).length !== row.body_bytes) throw new Error('Stored message incomplete');
  return { id: row.id, sender_id: row.sender_id, body, client_nonce: row.client_nonce, created_at: new Date(row.created_at).toISOString() };
}

async function loadPageBodies(env: Env, rows: MessageRow[]): Promise<Map<string, string>> {
  if (!rows.length) return new Map();
  const placeholders = rows.map(() => '?').join(', ');
  const chunks = await env.DB.prepare(`SELECT CAST(message_id AS TEXT) AS message_id, body FROM message_chunks
    WHERE message_id IN (${placeholders}) ORDER BY message_id, chunk_index`)
    .bind(...rows.map((row) => row.id)).all<{ message_id: string; body: string }>();
  const pieces = new Map<string, string[]>();
  for (const chunk of chunks.results) {
    const message = pieces.get(chunk.message_id) || [];
    message.push(chunk.body);
    pieces.set(chunk.message_id, message);
  }
  return new Map(Array.from(pieces, ([id, parts]) => [id, parts.join('')]));
}

const MESSAGE_COLUMNS = 'CAST(id AS TEXT) AS id, sender_id, client_nonce, created_at, body_bytes, body_sha256';

async function listMessages(request: Request, env: Env, auth: Auth): Promise<Response> {
  const url = new URL(request.url);
  const before = parseId(url.searchParams.get('before'), false);
  // An empty conversation starts polling at zero; other IDs stay positive.
  const rawAfter = url.searchParams.get('after');
  const after = rawAfter === '0' ? '0' : parseId(rawAfter, false);
  if (before && after) throw new HttpError(400, 'Use only one page cursor.');
  const now = Date.now();
  const direction = after ? 'ASC' : 'DESC';
  const cursorCondition = before ? 'AND messages.id < CAST(? AS INTEGER)' : after ? 'AND messages.id > CAST(? AS INTEGER)' : '';
  const parameters: (number | string)[] = [now - RETENTION_MS];
  if (before || after) parameters.push((before || after)!);
  const rows = await env.DB.prepare(`SELECT ${MESSAGE_COLUMNS} FROM messages
    WHERE created_at > ? ${cursorCondition} ORDER BY messages.id ${direction} LIMIT ${MESSAGE_LIMIT + 1}`)
    .bind(...parameters).all<MessageRow>();
  const messages: ChatMessage[] = [];
  const bodyRows: MessageRow[] = [];
  let rawBudget = 0;
  for (const row of rows.results.slice(0, MESSAGE_LIMIT)) {
    if (rawBudget + row.body_bytes > MAX_PAGE_BYTES - 2048 && bodyRows.length) break;
    bodyRows.push(row);
    rawBudget += row.body_bytes;
  }
  // One bounded query, not one subrequest for every message on the free plan.
  const bodies = await loadPageBodies(env, bodyRows);
  let totalBytes = 1024;
  for (const row of bodyRows) {
    const body = bodies.get(row.id);
    if (body === undefined || encoder.encode(body).length !== row.body_bytes) throw new Error('Stored message incomplete');
    const message: ChatMessage = {
      id: row.id, sender_id: row.sender_id, body, client_nonce: row.client_nonce,
      created_at: new Date(row.created_at).toISOString(),
    };
    const bytes = encoder.encode(JSON.stringify(message)).length + 1;
    if (totalBytes + bytes > MAX_PAGE_BYTES && messages.length > 0) break;
    // Escaped control characters can expand JSON beyond the body byte count.
    // A single message remains retrievable within the original request budget.
    if (totalBytes + bytes > MAX_PAGE_BYTES) throw new HttpError(413, 'Message cannot be transferred.');
    messages.push(message);
    totalBytes += bytes;
  }
  const hasMore = rows.results.length > messages.length;
  if (!after) messages.reverse();
  const [receiptRows, peerPresence] = await Promise.all([
    env.DB.prepare(`SELECT r.user_id, CAST(m.id AS TEXT) AS last_read_message_id
      FROM read_receipts r LEFT JOIN messages m ON m.id = r.last_read_message_id AND m.created_at > ?`)
      .bind(now - RETENTION_MS).all<{ user_id: MemberId; last_read_message_id: string | null }>(),
    env.DB.prepare('SELECT online, last_seen FROM presence WHERE user_id = ?')
      .bind(auth.peer.id).first<{ online: number; last_seen: number }>(),
  ]);
  const receipts = auth.config.members.map(({ id }) => ({
    user_id: id,
    last_read_message_id: receiptRows.results.find((receipt) => receipt.user_id === id)?.last_read_message_id ?? null,
  }));
  return json({ messages, hasMore, receipts, peerOnline: !!peerPresence?.online && peerPresence.last_seen > now - 45_000, serverTime: now });
}

async function createMessage(request: Request, env: Env, auth: Auth, ctx: ExecutionContext): Promise<Response> {
  const input = await readJson(request);
  if (typeof input.body !== 'string' || !input.body.trim() || /[\uD800-\uDFFF]/u.test(input.body)
      || typeof input.client_nonce !== 'string' || !/^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(input.client_nonce)) {
    throw new HttpError(400, 'Enter a message and a valid client nonce.');
  }
  const body = input.body;
  const nonce = input.client_nonce.toLowerCase();
  const bodyBytes = encoder.encode(body).length;
  if (bodyBytes > MAX_REQUEST_BYTES) throw new HttpError(413, 'Message is too large for one request.');
  const hash = await sha256(body);
  const old = await env.DB.prepare(`SELECT ${MESSAGE_COLUMNS} FROM messages WHERE sender_id = ? AND client_nonce = ?`)
    .bind(auth.member.id, nonce).first<MessageRow>();
  if (old) {
    if (old.body_sha256 !== hash) throw new HttpError(409, 'This send identifier has already been used.');
    if (old.created_at <= Date.now() - RETENTION_MS) throw new HttpError(410, 'This message has expired.');
    return json({ message: await loadMessage(env, old) });
  }
  await consumeBuckets(env, [{ key: `send:${auth.member.id}`, limit: 120, duration: 60_000 }]);
  const now = Date.now();
  const statements = [env.DB.prepare(`INSERT INTO messages(sender_id, client_nonce, created_at, body_bytes, body_sha256)
    VALUES (?, ?, ?, ?, ?) ON CONFLICT(sender_id, client_nonce) DO NOTHING`)
    .bind(auth.member.id, nonce, now, bodyBytes, hash)];
  for (const [index, chunk] of splitMessage(body).entries()) {
    // The hash condition prevents concurrent different bodies sharing a nonce
    // from filling another request's message with their own chunks.
    statements.push(env.DB.prepare(`INSERT INTO message_chunks(message_id, chunk_index, body)
      SELECT id, ?, ? FROM messages WHERE sender_id = ? AND client_nonce = ? AND body_sha256 = ?
      ON CONFLICT(message_id, chunk_index) DO NOTHING`).bind(index, chunk, auth.member.id, nonce, hash));
  }
  await env.DB.batch(statements);
  const saved = await env.DB.prepare(`SELECT ${MESSAGE_COLUMNS} FROM messages WHERE sender_id = ? AND client_nonce = ?`)
    .bind(auth.member.id, nonce).first<MessageRow>();
  if (!saved) throw new Error('Message insert failed');
  if (saved.body_sha256 !== hash) throw new HttpError(409, 'This send identifier has already been used.');
  const message = await loadMessage(env, saved);
  // A raced retry may send a duplicate generic push, but never creates a
  // duplicate stored message or exposes message text in a notification.
  ctx.waitUntil(notifyPeer(env, auth.peer.id, new URL(request.url).origin).catch(() => undefined));
  return json({ message }, 201);
}

async function acknowledge(request: Request, env: Env, auth: Auth): Promise<Response> {
  const input = await readJson(request);
  const id = parseId(input.message_id)!;
  const now = Date.now();
  const results = await env.DB.prepare(`INSERT INTO read_receipts(user_id, last_read_message_id, updated_at)
    SELECT ?, id, ? FROM messages WHERE id = CAST(? AS INTEGER) AND created_at > ?
    ON CONFLICT(user_id) DO UPDATE SET
      last_read_message_id = CASE WHEN read_receipts.last_read_message_id IS NULL
        OR read_receipts.last_read_message_id < excluded.last_read_message_id
        THEN excluded.last_read_message_id ELSE read_receipts.last_read_message_id END,
      updated_at = excluded.updated_at
    RETURNING user_id`).bind(auth.member.id, now, id, now - RETENTION_MS).all();
  if (!results.results.length) throw new HttpError(404, 'Message is no longer available.');
  return json({ ok: true });
}

interface VapidSettings { publicKey: string; privateKey: JsonWebKey }

async function vapidSettings(env: Env): Promise<VapidSettings> {
  const existing = await env.DB.prepare("SELECT value FROM app_settings WHERE key = 'vapid-v1'").first<{ value: string }>();
  if (existing) return JSON.parse(existing.value) as VapidSettings;
  const key = await crypto.subtle.generateKey({ name: 'ECDSA', namedCurve: 'P-256' }, true, ['sign', 'verify']) as CryptoKeyPair;
  const [privateKey, rawPublic] = await Promise.all([
    crypto.subtle.exportKey('jwk', key.privateKey),
    crypto.subtle.exportKey('raw', key.publicKey),
  ]);
  const settings: VapidSettings = { privateKey: privateKey as JsonWebKey, publicKey: base64url(new Uint8Array(rawPublic as ArrayBuffer)) };
  await env.DB.prepare("INSERT OR IGNORE INTO app_settings(key, value) VALUES ('vapid-v1', ?)")
    .bind(JSON.stringify(settings)).run();
  // Concurrent first requests all receive the persisted winning key.
  const winner = await env.DB.prepare("SELECT value FROM app_settings WHERE key = 'vapid-v1'").first<{ value: string }>();
  if (!winner) throw new Error('Push key unavailable');
  return JSON.parse(winner.value) as VapidSettings;
}

export function allowedPushEndpoint(value: unknown): value is string {
  if (typeof value !== 'string' || value.length > 4096) return false;
  try {
    const endpoint = new URL(value);
    const host = endpoint.hostname.toLowerCase();
    return endpoint.protocol === 'https:' && !endpoint.username && !endpoint.password
      && !endpoint.hash && (!endpoint.port || endpoint.port === '443') && endpoint.pathname.length > 1
      && (host === 'fcm.googleapis.com' || host === 'updates.push.services.mozilla.com'
        || host === 'push.services.mozilla.com' || host === 'web.push.apple.com'
        || (host.endsWith('.notify.windows.com') && host.length > '.notify.windows.com'.length));
  } catch { return false; }
}

async function pushSubscription(request: Request, env: Env, auth: Auth): Promise<Response> {
  const input = await readJson(request);
  if (!allowedPushEndpoint(input.endpoint)) throw new HttpError(400, 'Unsupported push service.');
  const id = await sha256(input.endpoint);
  if (request.method === 'DELETE') {
    await env.DB.prepare('DELETE FROM push_subscriptions WHERE id = ? AND user_id = ?').bind(id, auth.member.id).run();
    return json({ ok: true });
  }
  await consumeBuckets(env, [{ key: `push:${auth.member.id}`, limit: 30, duration: 60_000 }]);
  await vapidSettings(env);
  await env.DB.batch([
    env.DB.prepare(`INSERT INTO push_subscriptions(id, user_id, session_hash, endpoint, created_at) VALUES (?, ?, ?, ?, ?)
      ON CONFLICT(id) DO UPDATE SET user_id = excluded.user_id, session_hash = excluded.session_hash, created_at = excluded.created_at`)
      .bind(id, auth.member.id, auth.tokenHash, input.endpoint, Date.now()),
    env.DB.prepare(`DELETE FROM push_subscriptions WHERE id IN (
      SELECT id FROM push_subscriptions WHERE user_id = ? ORDER BY created_at DESC, id DESC LIMIT -1 OFFSET 10
    )`).bind(auth.member.id),
  ]);
  return json({ ok: true });
}

async function vapidToken(settings: VapidSettings, audience: string, subject: string): Promise<string> {
  const header = base64url(encoder.encode(JSON.stringify({ typ: 'JWT', alg: 'ES256' })));
  const payload = base64url(encoder.encode(JSON.stringify({ aud: audience, exp: Math.floor(Date.now() / 1000) + 12 * 60 * 60, sub: subject })));
  const unsigned = `${header}.${payload}`;
  const key = await crypto.subtle.importKey('jwk', settings.privateKey, { name: 'ECDSA', namedCurve: 'P-256' }, false, ['sign']);
  const signature = new Uint8Array(await crypto.subtle.sign({ name: 'ECDSA', hash: 'SHA-256' }, key, encoder.encode(unsigned)));
  return `${unsigned}.${base64url(signature)}`;
}

async function notifyPeer(env: Env, peer: MemberId, origin: string): Promise<void> {
  const subscriptions = await env.DB.prepare(`SELECT p.id, p.endpoint FROM push_subscriptions p
    JOIN sessions s ON s.token_hash = p.session_hash
    WHERE p.user_id = ? AND s.expires_at > ? ORDER BY p.created_at DESC LIMIT 10`)
    .bind(peer, Date.now()).all<{ id: string; endpoint: string }>();
  if (!subscriptions.results.length) return;
  const settings = await vapidSettings(env);
  await Promise.all(subscriptions.results.map(async ({ id, endpoint }) => {
    if (!allowedPushEndpoint(endpoint)) return;
    try {
      const token = await vapidToken(settings, new URL(endpoint).origin, origin);
      const response = await fetch(endpoint, {
        method: 'POST', redirect: 'manual', signal: AbortSignal.timeout(8000),
        headers: { Authorization: `vapid t=${token},k=${settings.publicKey}`, TTL: '60', Urgency: 'normal' },
        body: '',
      });
      await response.body?.cancel();
      if (response.status === 404 || response.status === 410) {
        await env.DB.prepare('DELETE FROM push_subscriptions WHERE id = ? AND user_id = ?').bind(id, peer).run();
      }
    } catch { /* Delivery is best effort; never log subscription credentials. */ }
  }));
}

export async function cleanup(env: Env, now = Date.now()): Promise<void> {
  // Bounded work keeps a large overdue backlog from monopolizing the free
  // worker. Reads hide expired messages immediately even while it is cleared.
  await env.DB.batch([
    env.DB.prepare(`DELETE FROM messages WHERE id IN (
      SELECT id FROM messages WHERE created_at <= ? ORDER BY created_at, id LIMIT 100
    )`).bind(now - RETENTION_MS),
    env.DB.prepare('DELETE FROM sessions WHERE token_hash IN (SELECT token_hash FROM sessions WHERE expires_at <= ? LIMIT 1000)').bind(now),
    env.DB.prepare('DELETE FROM rate_limits WHERE bucket IN (SELECT bucket FROM rate_limits WHERE expires_at <= ? LIMIT 1000)').bind(now),
    env.DB.prepare('DELETE FROM presence WHERE last_seen <= ?').bind(now - 24 * 60 * 60 * 1000),
  ]);
}

async function api(request: Request, env: Env, ctx: ExecutionContext): Promise<Response> {
  const path = new URL(request.url).pathname;
  if (path === '/api/health' && request.method === 'GET') return json({ ok: true });
  if (!['GET', 'POST', 'DELETE'].includes(request.method)) throw new HttpError(405, 'Method not allowed.');
  if (request.method !== 'GET') sameOrigin(request);
  if (path === '/api/auth/signin' && request.method === 'POST') return signin(request, env);
  const auth = await authentication(request, env);
  if (path === '/api/session' && request.method === 'GET') {
    return json({ member: publicMember(auth.member), peer: publicMember(auth.peer), serverTime: Date.now() });
  }
  if (path === '/api/auth/signout' && request.method === 'POST') {
    await readJson(request);
    await env.DB.batch([
      env.DB.prepare('DELETE FROM sessions WHERE token_hash = ?').bind(auth.tokenHash),
      env.DB.prepare('UPDATE presence SET online = 0, last_seen = ? WHERE user_id = ?').bind(Date.now(), auth.member.id),
    ]);
    return json({ ok: true }, 200, { 'Set-Cookie': sessionCookie('', 0) });
  }
  if (path === '/api/messages') {
    if (request.method === 'GET') return listMessages(request, env, auth);
    if (request.method === 'POST') return createMessage(request, env, auth, ctx);
  }
  if (path === '/api/read' && request.method === 'POST') return acknowledge(request, env, auth);
  if (path === '/api/presence' && request.method === 'POST') {
    const input = await readJson(request);
    if (typeof input.online !== 'boolean') throw new HttpError(400, 'Invalid presence.');
    await env.DB.prepare(`INSERT INTO presence(user_id, online, last_seen) VALUES (?, ?, ?)
      ON CONFLICT(user_id) DO UPDATE SET online = excluded.online, last_seen = excluded.last_seen`)
      .bind(auth.member.id, input.online ? 1 : 0, Date.now()).run();
    return json({ ok: true });
  }
  if (path === '/api/push/key' && request.method === 'GET') return json({ publicKey: (await vapidSettings(env)).publicKey });
  if (path === '/api/push/subscribe' && (request.method === 'POST' || request.method === 'DELETE')) return pushSubscription(request, env, auth);
  throw new HttpError(404, 'Not found.');
}

export default {
  async fetch(request: Request, env: Env, ctx: ExecutionContext): Promise<Response> {
    if (new URL(request.url).pathname.startsWith('/api/')) {
      try { return await api(request, env, ctx); }
      catch (error) {
        if (error instanceof HttpError) return json({ error: error.publicMessage }, error.status);
        // Do not serialize provider errors: they can contain SQL, cookies,
        // message contents or credential-bearing subscription endpoints.
        return json({ error: 'Chat is temporarily unavailable. Please try again.' }, 503);
      }
    }
    const response = await env.ASSETS.fetch(request);
    const headers = new Headers(response.headers);
    headers.set('X-Content-Type-Options', 'nosniff');
    headers.set('Referrer-Policy', 'no-referrer');
    headers.set('X-Frame-Options', 'DENY');
    headers.set('Permissions-Policy', 'camera=(), microphone=(), geolocation=()');
    headers.set('Content-Security-Policy', "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; connect-src 'self'; img-src 'self' data:; font-src 'self'; worker-src 'self'; frame-ancestors 'none'; base-uri 'none'; form-action 'self'");
    // App assets may contain cached shells, but authenticated responses and
    // message bodies are never cached by this worker or its service worker.
    return new Response(response.body, { status: response.status, statusText: response.statusText, headers });
  },
  async scheduled(_event: ScheduledController, env: Env, _ctx: ExecutionContext): Promise<void> {
    try { await cleanup(env); }
    catch { throw new Error('OurChat scheduled cleanup failed'); }
  },
} satisfies ExportedHandler<Env>;
