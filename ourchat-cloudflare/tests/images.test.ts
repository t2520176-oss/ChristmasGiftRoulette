import { afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest';
import {
  applyD1Migrations, createExecutionContext, env, waitOnExecutionContext,
  type D1Migration,
} from 'cloudflare:test';
import worker, { cleanup, IMAGE_CHUNK_BYTES, MAX_IMAGE_BYTES, MAX_PAGE_BYTES, RETENTION_MS, sha256, type Env } from '../worker/app';

const bindings = env as unknown as Env & { TEST_MIGRATIONS: D1Migration[] };
const db = bindings.DB;
const ORIGIN = 'https://ourchat.example.test';
const IMAGE_QUOTA = 64 * 1024 * 1024;
// Actual 2 x 1 JPEGs generated with sharp; the browser sends this normalized format.
const RED_JPEG = '/9j/2wBDAAMCAgMCAgMDAwMEAwMEBQgFBQQEBQoHBwYIDAoMDAsKCwsNDhIQDQ4RDgsLEBYQERMUFRUVDA8XGBYUGBIUFRT/2wBDAQMEBAUEBQkFBQkUDQsNFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBT/wAARCAABAAIDASIAAhEBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAj/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/8QAFQEBAQAAAAAAAAAAAAAAAAAABwn/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oADAMBAAIRAxEAPwCdAAYqm//Z';
const BLUE_JPEG = '/9j/2wBDAAMCAgMCAgMDAwMEAwMEBQgFBQQEBQoHBwYIDAoMDAsKCwsNDhIQDQ4RDgsLEBYQERMUFRUVDA8XGBYUGBIUFRT/2wBDAQMEBAUEBQkFBQkUDQsNFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBT/wAARCAABAAIDASIAAhEBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAn/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/8QAFQEBAQAAAAAAAAAAAAAAAAAABgn/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oADAMBAAIRAxEAPwCe4CqYO//Z';
const decode = (value: string) => Uint8Array.from(atob(value), (character) => character.charCodeAt(0));
function encode(bytes: Uint8Array): string {
  let binary = '';
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary);
}
const image = (data = RED_JPEG) => ({ mimeType: 'image/jpeg', data });

interface Message {
  id: string;
  body: string;
  client_nonce: string;
  sender_id: string;
  image?: { url: string; width: number; height: number; bytes: number; mimeType: string };
}

async function call(path: string, options: { cookie?: string; body?: unknown; headers?: HeadersInit } = {}): Promise<Response> {
  const headers = new Headers(options.headers);
  if (options.cookie) headers.set('Cookie', options.cookie);
  if (options.body !== undefined) {
    headers.set('Origin', ORIGIN);
    headers.set('Content-Type', 'application/json');
  }
  const context = createExecutionContext();
  const response = await worker.fetch(new Request(`${ORIGIN}${path}`, {
    method: options.body === undefined ? 'GET' : 'POST', headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
  }), bindings, context);
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

async function send(cookie: string, body = '', data = RED_JPEG, nonce = crypto.randomUUID()): Promise<Message> {
  const response = await call('/api/messages', { cookie, body: { body, client_nonce: nonce, image: image(data) } });
  expect(response.status).toBe(201);
  return (await response.json() as { message: Message }).message;
}

// Insert valid application metadata segments before the normal JPEG data. Each
// segment is bounded by JPEG's 16-bit segment length, and leaves decoding intact.
function paddedJpeg(targetBytes: number): string {
  const original = decode(RED_JPEG);
  const result = new Uint8Array(targetBytes);
  result.set(original.subarray(0, 2));
  let offset = 2;
  let remaining = targetBytes - original.length;
  while (remaining) {
    let segmentBytes = Math.min(remaining, 65_537);
    if (remaining - segmentBytes > 0 && remaining - segmentBytes < 4) segmentBytes -= 4 - (remaining - segmentBytes);
    if (segmentBytes < 4) throw new Error('Padding needs a complete APP segment');
    const segmentLength = segmentBytes - 2;
    result.set([0xff, 0xef, segmentLength >>> 8, segmentLength & 255], offset);
    offset += segmentBytes;
    remaining -= segmentBytes;
  }
  result.set(original.subarray(2), offset);
  return encode(result);
}

function dimensions(width: number, height: number): string {
  const bytes = decode(RED_JPEG);
  const start = bytes.findIndex((byte, index) => byte === 0xff && bytes[index + 1] === 0xc0);
  if (start < 0) throw new Error('Fixture lacks SOF');
  bytes.set([height >>> 8, height & 255, width >>> 8, width & 255], start + 5);
  return encode(bytes);
}

beforeAll(async () => { await applyD1Migrations(db, bindings.TEST_MIGRATIONS); });
beforeEach(async () => {
  vi.restoreAllMocks();
  vi.spyOn(globalThis, 'fetch').mockRejectedValue(new Error('External network is disabled in image tests'));
  await db.batch([
    db.prepare('DELETE FROM push_subscriptions'), db.prepare('DELETE FROM sessions'),
    db.prepare('DELETE FROM messages'), db.prepare('DELETE FROM read_receipts'),
    db.prepare('DELETE FROM rate_limits'), db.prepare('DELETE FROM presence'), db.prepare('DELETE FROM app_settings'),
    db.prepare('UPDATE image_storage SET bytes_used = 0 WHERE id = 1'),
  ]);
});
afterEach(async () => {
  // Quota tests temporarily reserve capacity without allocating 64 MiB of data.
  await db.prepare('UPDATE image_storage SET bytes_used = (SELECT COALESCE(SUM(byte_length), 0) FROM message_images) WHERE id = 1').run();
  vi.restoreAllMocks();
});

describe('private JPEG messages in native D1', () => {
  it('stores image-only messages and exact plaintext captions without inline image data', async () => {
    const husband = await login();
    const wife = await login(2);
    const photo = await send(husband);
    const caption = '  A private caption 🎄\nCopy this exactly.  ';
    const captioned = await send(wife, caption, BLUE_JPEG);
    for (const message of [photo, captioned]) {
      expect(message.image).toEqual({ url: `/api/images/${message.id}`, width: 2, height: 1, bytes: 270, mimeType: 'image/jpeg' });
      expect(JSON.stringify(message)).not.toContain(RED_JPEG);
      expect(JSON.stringify(message)).not.toContain(BLUE_JPEG);
    }
    expect(photo.body).toBe('');
    expect(captioned.body).toBe(caption);
    const rows = await db.prepare('SELECT CAST(id AS TEXT) AS id, body_bytes FROM messages ORDER BY id').all();
    expect(rows.results).toEqual([
      { id: photo.id, body_bytes: 270 },
      { id: captioned.id, body_bytes: 270 + new TextEncoder().encode(caption).length },
    ]);
    expect((await db.prepare('SELECT body FROM message_chunks WHERE message_id = ? ORDER BY chunk_index').bind(captioned.id).all()).results)
      .toEqual([{ body: caption }]);
    const result = await call('/api/messages', { cookie: husband });
    const serialized = await result.text();
    expect(new TextEncoder().encode(serialized).length).toBeLessThan(MAX_PAGE_BYTES);
    const page = JSON.parse(serialized);
    expect(page.messages.map((message: Message) => message.id)).toEqual([photo.id, captioned.id]);
    expect(page.messages[0].body).toBe('');
    expect(page.messages[1].body).toBe(caption);
    expect(page.messages[1].image).toEqual(captioned.image);
    expect(serialized).not.toContain(RED_JPEG);
    expect(serialized).not.toContain(BLUE_JPEG);
  });

  it('returns identical private JPEG bytes to either authenticated member and prevents anonymous access', async () => {
    const husband = await login();
    const wife = await login(2);
    const message = await send(husband);
    const url = message.image!.url;
    expect((await call(url)).status).toBe(401);
    expect((await call(url, { cookie: '__Host-ourchat-session=bad' })).status).toBe(401);
    for (const cookie of [husband, wife]) {
      const response = await call(url, { cookie });
      expect(response.status).toBe(200);
      expect(response.headers.get('Content-Type')).toBe('image/jpeg');
      expect(response.headers.get('X-Content-Type-Options')).toBe('nosniff');
      expect(response.headers.get('Cache-Control')).toBe('private, no-store');
      expect(response.headers.get('Vary')).toContain('Cookie');
      expect(response.headers.get('Access-Control-Allow-Origin')).toBeNull();
      expect(new Uint8Array(await response.arrayBuffer())).toEqual(decode(RED_JPEG));
    }
    expect((await call('/api/images/9007199254740991', { cookie: husband })).status).toBe(404);
    expect((await call('/api/images/1%20OR%201=1', { cookie: husband })).status).toBe(400);
  });

  it('keeps legacy text SHA behavior while binding image nonces to bytes and captions', async () => {
    const cookie = await login();
    const nonce = crypto.randomUUID();
    const payload = { body: 'same caption', client_nonce: nonce, image: image() };
    const message = await send(cookie, payload.body, RED_JPEG, nonce);
    const duplicate = await call('/api/messages', { cookie, body: payload });
    expect(duplicate.status).toBe(200);
    expect((await duplicate.json() as { message: Message }).message).toEqual(message);
    for (const changed of [
      { ...payload, image: image(BLUE_JPEG) },
      { ...payload, body: 'changed caption' },
      { body: payload.body, client_nonce: nonce },
    ]) expect((await call('/api/messages', { cookie, body: changed })).status).toBe(409);
    expect(await db.prepare('SELECT COUNT(*) AS count FROM messages').first('count')).toBe(1);
    expect(await db.prepare('SELECT bytes_used FROM image_storage WHERE id = 1').first('bytes_used')).toBe(270);
    const textNonce = crypto.randomUUID();
    const text = 'A message saved before image attachments';
    const textPayload = { body: text, client_nonce: textNonce };
    const first = await call('/api/messages', { cookie, body: textPayload });
    expect(first.status).toBe(201);
    const saved = (await first.json() as { message: Message }).message;
    expect(saved.body).toBe(text);
    expect(saved.image).toBeUndefined();
    expect(await db.prepare('SELECT body_sha256 FROM messages WHERE id = ?').bind(saved.id).first('body_sha256')).toBe(await sha256(text));
    expect((await call('/api/messages', { cookie, body: textPayload })).status).toBe(200);
    expect((await call('/api/messages', { cookie, body: { ...textPayload, image: image() } })).status).toBe(409);
    expect((await call(`/api/images/${saved.id}`, { cookie })).status).toBe(404);
  });

  it('strictly rejects SVG, URLs, invalid base64, unsupported types and malformed images', async () => {
    const cookie = await login();
    const padded = paddedJpeg(274);
    for (const invalid of [
      { mimeType: 'image/svg+xml', data: btoa('<svg xmlns="http://www.w3.org/2000/svg"><script>alert(1)</script></svg>') },
      image(btoa('<svg><script>alert(1)</script></svg>')),
      { mimeType: 'image/png', data: RED_JPEG },
      image('https://example.test/private.jpg'),
      image(`data:image/jpeg;base64,${RED_JPEG}`),
      image(`${RED_JPEG}\n`), image(`${RED_JPEG.slice(0, 10)} ${RED_JPEG.slice(10)}`),
      image(padded.replace(/=+$/, '')), image('!!!!'), image(''),
      image(RED_JPEG.slice(0, -4)),
      { mimeType: 'image/jpeg', data: 123 }, null, [],
    ]) {
      const response = await call('/api/messages', { cookie, body: { body: '', client_nonce: crypto.randomUUID(), image: invalid } });
      expect(response.status).toBe(400);
      const error = await response.text();
      expect(error).not.toMatch(/D1_ERROR|INSERT INTO|SELECT |token_hash|app_settings/);
    }
    expect((await call('/api/messages', { cookie, body: { body: '', client_nonce: crypto.randomUUID() } })).status).toBe(400);
    expect(await db.prepare('SELECT COUNT(*) AS count FROM messages').first('count')).toBe(0);
    expect(await db.prepare('SELECT bytes_used FROM image_storage WHERE id = 1').first('bytes_used')).toBe(0);
  });

  it('checks actual JPEG frame dimensions and rejects zero or oversized dimensions', async () => {
    const cookie = await login();
    for (const [width, height] of [[0, 1], [1, 0], [1601, 1], [1, 1601], [65_535, 65_535]]) {
      const response = await call('/api/messages', { cookie, body: {
        body: '', client_nonce: crypto.randomUUID(), image: image(dimensions(width, height)),
      } });
      expect([400, 413]).toContain(response.status);
    }
    expect(await db.prepare('SELECT COUNT(*) AS count FROM messages').first('count')).toBe(0);
  });

  it('round-trips the exact 128 KiB limit in bounded BLOB chunks and rejects the next byte', async () => {
    const cookie = await login();
    const data = paddedJpeg(MAX_IMAGE_BYTES);
    const message = await send(cookie, 'An image split into safe D1 rows', data);
    expect(message.image!.bytes).toBe(MAX_IMAGE_BYTES);
    const chunks = await db.prepare('SELECT chunk_index, length(data) AS bytes FROM image_chunks WHERE message_id = ? ORDER BY chunk_index')
      .bind(message.id).all<{ chunk_index: number; bytes: number }>();
    expect(chunks.results).toHaveLength(4);
    expect(chunks.results.map((chunk) => chunk.chunk_index)).toEqual([0, 1, 2, 3]);
    expect(chunks.results.every((chunk) => chunk.bytes > 0 && chunk.bytes <= IMAGE_CHUNK_BYTES)).toBe(true);
    expect(chunks.results.reduce((sum, chunk) => sum + chunk.bytes, 0)).toBe(MAX_IMAGE_BYTES);
    expect(new Uint8Array(await (await call(message.image!.url, { cookie })).arrayBuffer())).toEqual(decode(data));
    const response = await call('/api/messages', { cookie, body: {
      body: 'too large', client_nonce: crypto.randomUUID(), image: image(paddedJpeg(MAX_IMAGE_BYTES + 1)),
    } });
    expect(response.status).toBe(413);
    expect(await db.prepare('SELECT COUNT(*) AS count FROM messages').first('count')).toBe(1);
    expect(await db.prepare('SELECT bytes_used FROM image_storage WHERE id = 1').first('bytes_used')).toBe(MAX_IMAGE_BYTES);
  });

  it('hides images at exactly 168 hours and cascades scheduled deletion while retaining fresh data', async () => {
    const now = 1_800_000_000_000;
    vi.spyOn(Date, 'now').mockReturnValue(now);
    const husband = await login();
    const wife = await login(2);
    const boundary = await send(husband, 'expired caption');
    const fresh = await send(wife, 'fresh caption', BLUE_JPEG);
    await db.batch([
      db.prepare('UPDATE messages SET created_at = ? WHERE id = ?').bind(now - RETENTION_MS, boundary.id),
      db.prepare('UPDATE messages SET created_at = ? WHERE id = ?').bind(now - RETENTION_MS + 1, fresh.id),
    ]);
    for (const cookie of [husband, wife]) {
      expect((await call(boundary.image!.url, { cookie })).status).toBe(404);
      expect((await call(fresh.image!.url, { cookie })).status).toBe(200);
      const page = await (await call('/api/messages', { cookie })).json() as { messages: Message[] };
      expect(page.messages.map((message) => message.id)).toEqual([fresh.id]);
    }
    expect((await call('/api/messages', { cookie: husband, body: {
      body: boundary.body, client_nonce: boundary.client_nonce, image: image(),
    } })).status).toBe(410);
    expect(await db.prepare('SELECT COUNT(*) AS count FROM image_chunks').first('count')).toBe(2);
    await cleanup(bindings, now);
    for (const table of ['messages', 'message_images']) {
      const column = table === 'messages' ? 'id' : 'message_id';
      expect((await db.prepare(`SELECT CAST(${column} AS TEXT) AS id FROM ${table}`).all()).results).toEqual([{ id: fresh.id }]);
    }
    expect((await db.prepare('SELECT CAST(message_id AS TEXT) AS id FROM image_chunks').all()).results).toEqual([{ id: fresh.id }]);
    expect((await db.prepare('SELECT body FROM message_chunks').all()).results).toEqual([{ body: 'fresh caption' }]);
    expect(await db.prepare('SELECT bytes_used FROM image_storage WHERE id = 1').first('bytes_used')).toBe(270);
    await db.prepare('DELETE FROM messages WHERE id = ?').bind(fresh.id).run();
    expect(await db.prepare('SELECT COUNT(*) AS count FROM image_chunks').first('count')).toBe(0);
    expect(await db.prepare('SELECT bytes_used FROM image_storage WHERE id = 1').first('bytes_used')).toBe(0);
  });

  it('rolls back all caption/image rows when the free image quota is full and permits exact retries', async () => {
    const cookie = await login();
    const existing = await send(cookie, 'already stored');
    await db.prepare('UPDATE image_storage SET bytes_used = ? WHERE id = 1').bind(IMAGE_QUOTA).run();
    const retry = await call('/api/messages', { cookie, body: {
      body: existing.body, client_nonce: existing.client_nonce, image: image(),
    } });
    expect(retry.status).toBe(200);
    const nonce = crypto.randomUUID();
    const rejected = await call('/api/messages', { cookie, body: { body: 'must roll back', client_nonce: nonce, image: image(BLUE_JPEG) } });
    expect(rejected.status).toBe(413);
    expect(await db.prepare('SELECT COUNT(*) AS count FROM messages WHERE client_nonce = ?').bind(nonce).first('count')).toBe(0);
    expect(await db.prepare('SELECT COUNT(*) AS count FROM messages').first('count')).toBe(1);
    expect(await db.prepare('SELECT COUNT(*) AS count FROM message_chunks').first('count')).toBe(1);
    expect(await db.prepare('SELECT COUNT(*) AS count FROM message_images').first('count')).toBe(1);
    expect(await db.prepare('SELECT COUNT(*) AS count FROM image_chunks').first('count')).toBe(1);
    expect(await db.prepare('SELECT bytes_used FROM image_storage WHERE id = 1').first('bytes_used')).toBe(IMAGE_QUOTA);
    const textOnly = await call('/api/messages', { cookie, body: { body: 'text still works', client_nonce: crypto.randomUUID() } });
    expect(textOnly.status).toBe(201);
  });

  it('enforces the shared quota atomically when image sends race for the remaining capacity', async () => {
    const cookie = await login();
    await db.prepare('UPDATE image_storage SET bytes_used = ? WHERE id = 1').bind(IMAGE_QUOTA - 270).run();
    const results = await Promise.all([RED_JPEG, BLUE_JPEG].map((data) => call('/api/messages', {
      cookie, body: { body: 'one available slot', client_nonce: crypto.randomUUID(), image: image(data) },
    })));
    expect(results.map((response) => response.status).sort()).toEqual([201, 413]);
    expect(await db.prepare('SELECT COUNT(*) AS count FROM messages').first('count')).toBe(1);
    expect(await db.prepare('SELECT COUNT(*) AS count FROM message_chunks').first('count')).toBe(1);
    expect(await db.prepare('SELECT COUNT(*) AS count FROM message_images').first('count')).toBe(1);
    expect(await db.prepare('SELECT COUNT(*) AS count FROM image_chunks').first('count')).toBe(1);
    expect(await db.prepare('SELECT bytes_used FROM image_storage WHERE id = 1').first('bytes_used')).toBe(IMAGE_QUOTA);
  });

  it('accepts concurrent identical retries once when they fill the last available image quota', async () => {
    const cookie = await login();
    await db.prepare('UPDATE image_storage SET bytes_used = ? WHERE id = 1').bind(IMAGE_QUOTA - 270).run();
    const payload = { body: 'retry the same send', client_nonce: crypto.randomUUID(), image: image() };
    const results = await Promise.all([
      call('/api/messages', { cookie, body: payload }),
      call('/api/messages', { cookie, body: payload }),
    ]);
    for (const response of results) expect([200, 201]).toContain(response.status);
    const messages = await Promise.all(results.map(async (response) => (await response.json() as { message: Message }).message));
    expect(messages[0]).toEqual(messages[1]);
    expect(await db.prepare('SELECT COUNT(*) AS count FROM messages').first('count')).toBe(1);
    expect(await db.prepare('SELECT COUNT(*) AS count FROM message_chunks').first('count')).toBe(1);
    expect(await db.prepare('SELECT COUNT(*) AS count FROM message_images').first('count')).toBe(1);
    expect(await db.prepare('SELECT COUNT(*) AS count FROM image_chunks').first('count')).toBe(1);
    expect(await db.prepare('SELECT bytes_used FROM image_storage WHERE id = 1').first('bytes_used')).toBe(IMAGE_QUOTA);
  });
});
