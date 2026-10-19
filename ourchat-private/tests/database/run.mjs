import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { randomUUID } from 'node:crypto';
import { fileURLToPath } from 'node:url';
import { PGlite } from '@electric-sql/pglite';

// In-memory synthetic identities only. Never connects to a live database.
const db = new PGlite();
const husband = randomUUID();
const wife = randomUUID();
const outsider = randomUUID();
let checks = 0;
async function test(name, fn) {
  await fn();
  checks++;
  console.log(`ok ${checks} - ${name}`);
}
async function as(user, role = 'authenticated') {
  await db.exec('reset role');
  await db.query("select set_config('request.jwt.claim.sub', $1, false)", [user ?? '']);
  await db.exec(`set role ${role}`);
}
async function admin() { await db.exec('reset role'); }
async function denied(sql, params = [], code = '42501') {
  await assert.rejects(db.query(sql, params), (e) => e.code === code, `Expected SQLSTATE ${code}`);
}
async function insertMessage(body = 'Hello', user = husband, nonce = randomUUID()) {
  const result = await db.query(
    'insert into public.messages(sender_id,body,client_nonce) values ($1,$2,$3) returning id::text,body,created_at',
    [user, body, nonce],
  );
  return result.rows[0];
}
async function schemaSnapshot(includeBodyConstraint = false) {
  const queries = {
    relations: `select c.oid, n.nspname, c.relname, c.relkind, c.relacl,
      c.relrowsecurity, c.relforcerowsecurity, c.relreplident
      from pg_class c join pg_namespace n on n.oid=c.relnamespace
      where n.nspname in ('public','private','realtime') order by c.oid`,
    columns: `select a.attrelid,a.attnum,a.attname,a.atttypid,a.atttypmod,
      a.attnotnull,a.attidentity,a.attgenerated,pg_get_expr(d.adbin,d.adrelid) as default_value
      from pg_attribute a join pg_class c on c.oid=a.attrelid
      join pg_namespace n on n.oid=c.relnamespace
      left join pg_attrdef d on d.adrelid=a.attrelid and d.adnum=a.attnum
      where n.nspname in ('public','private','realtime') and a.attnum>0
        and not a.attisdropped order by a.attrelid,a.attnum`,
    policies: `select * from pg_policies where schemaname in ('public','private','realtime')
      order by schemaname,tablename,policyname`,
    constraints: `select oid,conrelid,conname,contype,convalidated,pg_get_constraintdef(oid,false) as definition
      from pg_constraint where connamespace in ('public'::regnamespace,'private'::regnamespace,'realtime'::regnamespace)
      ${includeBodyConstraint ? '' : "and not (conrelid='public.messages'::regclass and conname='messages_body_check')"}
      order by oid`,
    triggers: `select t.oid,pg_get_triggerdef(t.oid) as definition from pg_trigger t
      join pg_class c on c.oid=t.tgrelid join pg_namespace n on n.oid=c.relnamespace
      where n.nspname in ('public','private','realtime') order by t.oid`,
    functions: `select p.oid,p.proacl,pg_get_functiondef(p.oid) as definition from pg_proc p
      join pg_namespace n on n.oid=p.pronamespace
      where n.nspname in ('public','private','realtime') order by p.oid`,
    publications: `select * from pg_publication_tables order by pubname,schemaname,tablename`,
    messages: `select id::text,sender_id,body,client_nonce,created_at from public.messages order by id`,
  };
  const snapshot = {};
  for (const [name, sql] of Object.entries(queries)) snapshot[name] = (await db.query(sql)).rows;
  return snapshot;
}

try {
  await db.exec(`
    create role anon nologin;
    create role authenticated nologin;
    create role service_role nologin bypassrls;
    create schema auth;
    create table auth.users(id uuid primary key);
    create function auth.uid() returns uuid language sql stable as $$
      select nullif(current_setting('request.jwt.claim.sub', true), '')::uuid;
    $$;
    grant usage on schema public, auth to anon, authenticated, service_role;
    grant execute on function auth.uid() to anon, authenticated, service_role;
    create schema realtime;
    create table realtime.messages(id bigint generated always as identity, extension text);
    create function realtime.topic() returns text language sql stable as $$
      select current_setting('realtime.topic', true);
    $$;
    grant usage on schema realtime to authenticated;
    grant select, insert on realtime.messages to authenticated;
    grant usage on sequence realtime.messages_id_seq to authenticated;
  `);
  const migration = fileURLToPath(new URL('../../supabase/migrations/202610180001_ourchat.sql', import.meta.url));
  await db.exec(await readFile(migration, 'utf8'));
  await db.query('insert into auth.users(id) values ($1),($2),($3)', [husband, wife, outsider]);
  await db.query('insert into public.members(user_id,display_name) values ($1,$2),($3,$4)', [husband, 'Husband', wife, 'Wife']);
  const characterLimitMigration = await readFile(new URL('../../supabase/migrations/202610190001_remove_message_character_limit.sql', import.meta.url), 'utf8');
  await as(husband);
  await insertMessage('Existing conversation survives the character-limit migration');
  await admin();
  await test('character-limit migration preserves existing rows, ACLs, policies, triggers, functions and other constraints', async () => {
    const before = await schemaSnapshot();
    await db.exec(characterLimitMigration);
    assert.deepEqual(await schemaSnapshot(), before);
  });
  await test('character-limit migration replay is a no-op including constraint identity', async () => {
    const before = await schemaSnapshot(true);
    await db.exec(characterLimitMigration);
    assert.deepEqual(await schemaSnapshot(true), before);
  });
  for (const [name, change, restore] of [
    ['unexpected body check',
      'alter table public.messages drop constraint messages_body_check; alter table public.messages add constraint messages_body_check check (char_length(body) between 1 and 1999)',
      'alter table public.messages drop constraint messages_body_check; alter table public.messages add constraint messages_body_check check (body = btrim(body) and char_length(body) >= 1)'],
    ['missing body check',
      'alter table public.messages drop constraint messages_body_check',
      'alter table public.messages add constraint messages_body_check check (body = btrim(body) and char_length(body) >= 1)'],
    ['extra body check',
      'alter table public.messages add constraint unexpected_body_check check (char_length(body) <= 2000)',
      'alter table public.messages drop constraint unexpected_body_check'],
    ['nullable body schema',
      'alter table public.messages alter column body drop not null',
      'alter table public.messages alter column body set not null'],
  ]) {
    await test(`character-limit migration refuses ${name} without changing state`, async () => {
      await db.exec(change);
      const before = await schemaSnapshot(true);
      await assert.rejects(db.exec(characterLimitMigration), (e) => e.code === '55000');
      await db.exec('rollback');
      assert.deepEqual(await schemaSnapshot(true), before);
      await db.exec(restore);
    });
  }

  await test('all client tables enforce RLS and default-deny writes', async () => {
    const { rows } = await db.query(`select relname, relrowsecurity from pg_class
      where oid in ('public.members'::regclass,'public.messages'::regclass,
      'public.read_receipts'::regclass,'public.push_subscriptions'::regclass)`);
    assert.equal(rows.length, 4);
    assert.ok(rows.every((r) => r.relrowsecurity));
  });
  await test('third member rejected by database cap', async () => {
    await denied('insert into public.members(user_id,display_name) values ($1,$2)', [outsider, 'Outsider'], '23514');
    assert.equal((await db.query('select count(*)::integer as n from public.members')).rows[0].n, 2);
  });
  await test('member slot domain and uniqueness guard cap independently', async () => {
    const { rows } = await db.query(`select pg_get_constraintdef(oid) as def from pg_constraint
      where conrelid='public.members'::regclass`);
    assert.ok(rows.some((r) => /UNIQUE \(member_slot\)/.test(r.def)));
    assert.ok(rows.some((r) => /member_slot.*ANY.*1.*2/.test(r.def)));
  });
  await as(husband);
  await test('member can read both profiles without recursive policy', async () => {
    assert.equal((await db.query('select * from public.members')).rows.length, 2);
  });
  await test('member cannot create, edit, or remove members', async () => {
    await denied('insert into public.members(user_id,display_name) values ($1,$2)', [outsider, 'Outsider']);
    await denied('update public.members set display_name=$1 where user_id=$2', ['Other', husband]);
    await denied('delete from public.members where user_id=$1', [husband]);
  });
  await test('message serialization is BEFORE STATEMENT and precedes identity allocation', async () => {
    await admin();
    const trigger = await db.query(`select tgtype::integer as type from pg_trigger
      where tgrelid='public.messages'::regclass and tgname='serialize_message_inserts'`);
    // PostgreSQL tgtype bitmask: BEFORE=2, INSERT=4, ROW=1 absent.
    assert.equal(trigger.rows[0].type, 6);
    // Observe the REAL identity sequence from a second BEFORE STATEMENT trigger.
    // Trigger names order it after the advisory lock but before row projection.
    await db.exec(`
      create table private.test_insert_observation(sequence_last bigint, sequence_called boolean, lock_held boolean);
      create function private.test_observe_identity_order() returns trigger
      language plpgsql security definer set search_path='' as $$
      begin
        insert into private.test_insert_observation
        select s.last_value, s.is_called, exists (
          select 1 from pg_catalog.pg_locks l where l.locktype='advisory'
          and (l.pid=pg_catalog.pg_backend_pid() or l.pid is null) and l.classid=17439021
          and l.objid=3 and l.objsubid=2 and l.granted
        ) from public.messages_id_seq s;
        return null;
      end;
      $$;
      revoke all on function private.test_observe_identity_order() from public,anon,authenticated;
      create trigger zz_test_identity_order before insert on public.messages
      for each statement execute function private.test_observe_identity_order();
    `);
    await as(husband);
    await db.exec('begin');
    const message = await insertMessage('Identity ordering probe');
    await admin();
    const observed = (await db.query(`select sequence_last::text,sequence_called,lock_held
      from private.test_insert_observation`)).rows[0];
    // PGlite's single-process backend exposes NULL pg_locks.pid; native PG has PID.
    assert.equal(observed.lock_held, true);
    const expected = BigInt(observed.sequence_last) + (observed.sequence_called ? 1n : 0n);
    assert.equal(BigInt(message.id), expected, 'identity must allocate after BEFORE STATEMENT observation');
    const held = await db.query(`select exists(select 1 from pg_locks where locktype='advisory'
      and (pid=pg_backend_pid() or pid is null) and classid=17439021 and objid=3 and objsubid=2 and granted) as held`);
    assert.equal(held.rows[0].held, true, 'lock must survive the insert statement until commit');
    await db.exec('commit');
    const released = await db.query(`select exists(select 1 from pg_locks where locktype='advisory'
      and (pid=pg_backend_pid() or pid is null) and classid=17439021 and objid=3 and objsubid=2 and granted) as held`);
    assert.equal(released.rows[0].held, false, 'transaction completion must release the lock');
    await db.exec(`drop trigger zz_test_identity_order on public.messages;
      drop function private.test_observe_identity_order();
      drop table private.test_insert_observation;`);
    await as(husband);
  });
  let first;
  let second;
  const nonce = randomUUID();
  await test('self message insert trims Unicode whitespace and preserves Unicode', async () => {
    first = await insertMessage('\t\n \u00a0Hello 한국어 👋\u3000\r\n', husband, nonce);
    assert.equal(first.body, 'Hello 한국어 👋');
    assert.match(first.id, /^\d+$/);
  });
  await test('message insert cannot forge other sender', async () => {
    await denied('insert into public.messages(sender_id,body,client_nonce) values ($1,$2,$3)', [wife, 'Forged', randomUUID()]);
  });
  await test('server stamps time instead of trusting caller timestamp', async () => {
    const result = await db.query(`insert into public.messages(sender_id,body,client_nonce,created_at)
      values($1,'New',$2,'2000-01-01') returning id::text,created_at`, [husband, randomUUID()]);
    second = result.rows[0];
    assert.ok(new Date(second.created_at).getUTCFullYear() >= 2026);
  });
  await test('message nonce unique for a sender and existing row remains readable', async () => {
    await denied('insert into public.messages(sender_id,body,client_nonce) values ($1,$2,$3)', [husband, 'Retry', nonce], '23505');
    const result = await db.query('select id::text,body from public.messages where sender_id=$1 and client_nonce=$2', [husband, nonce]);
    assert.equal(result.rows[0].id, first.id);
    assert.equal(result.rows[0].body, first.body);
  });
  await test('empty and blank Unicode whitespace messages still rejected', async () => {
    for (const body of ['', '   ', '\n\t\u00a0\u3000']) {
      await denied('insert into public.messages(sender_id,body,client_nonce) values ($1,$2,$3)', [husband, body, randomUUID()], '23514');
    }
  });
  await test('messages above 2000 ASCII and Unicode characters are stored intact', async () => {
    for (const body of ['x'.repeat(2001), '😀'.repeat(2001), '한😀a'.repeat(16667)]) {
      const message = await insertMessage(`\t\u3000${body}\n\u00a0`);
      assert.equal(message.body, body);
      assert.equal((await db.query('select body from public.messages where id=$1', [message.id])).rows[0].body, body);
    }
  });
  await test('a 50000-character text is readable by both members with normal nonce protection', async () => {
    const body = 'Long text\n'.repeat(4999) + 'Long text!';
    assert.equal(body.length, 50000);
    const longNonce = randomUUID();
    const message = await insertMessage(body, husband, longNonce);
    await denied('insert into public.messages(sender_id,body,client_nonce) values ($1,$2,$3)', [husband, body, longNonce], '23505');
    await as(wife);
    assert.equal((await db.query('select body from public.messages where id=$1', [message.id])).rows[0].body, body);
    await as(husband);
  });
  await test('members cannot UPDATE or DELETE messages', async () => {
    await denied('update public.messages set body=$1 where id=$2', ['Changed', first.id]);
    await denied('delete from public.messages where id=$1', [first.id]);
  });
  await as(wife);
  await test('other member reads the same chat and can reuse a different sender nonce', async () => {
    assert.ok((await db.query('select id from public.messages')).rows.length >= 3);
    await insertMessage('Reply', wife, nonce);
  });
  await test('mark_read returns text ID and rejects unknown IDs', async () => {
    const result = await db.query('select * from public.mark_read($1)', [second.id]);
    assert.equal(result.rows[0].last_read_message_id, second.id);
    assert.equal(result.rows[0].user_id, wife);
    await denied('select * from public.mark_read(9223372036854775807)', [], '22023');
    await denied('select * from public.mark_read(null)', [], '22023');
  });
  await test('receipt cannot regress through RPC, direct UPDATE, or UPSERT', async () => {
    await db.query('select * from public.mark_read($1)', [first.id]);
    await db.query('update public.read_receipts set last_read_message_id=$1 where user_id=$2', [first.id, wife]);
    await db.query(`insert into public.read_receipts(user_id,last_read_message_id) values($1,$2)
      on conflict(user_id) do update set last_read_message_id=excluded.last_read_message_id`, [wife, first.id]);
    await db.query('update public.read_receipts set last_read_message_id=null where user_id=$1', [wife]);
    assert.equal((await db.query('select last_read_message_id::text as id from public.read_receipts where user_id=$1', [wife])).rows[0].id, second.id);
  });
  await test('receipt cannot target another member or nonexistent message', async () => {
    await denied('insert into public.read_receipts(user_id,last_read_message_id) values ($1,$2)', [husband, first.id]);
    await denied('update public.read_receipts set last_read_message_id=9223372036854775807 where user_id=$1', [wife], '23503');
    await denied('delete from public.read_receipts where user_id=$1', [wife]);
  });
  await as(husband);
  await test('pair can read each other receipt but cannot modify it', async () => {
    assert.equal((await db.query('select user_id from public.read_receipts')).rows[0].user_id, wife);
    const result = await db.query('update public.read_receipts set last_read_message_id=$1 where user_id=$2 returning user_id', [first.id, wife]);
    assert.equal(result.rows.length, 0);
  });
  const endpoint = 'https://push.example.test/subscription-1';
  const key = 'A'.repeat(87);
  const secret = 'B'.repeat(22);
  await test('self push registration and key refresh work', async () => {
    await db.query('insert into public.push_subscriptions(user_id,endpoint,p256dh,auth) values($1,$2,$3,$4)', [husband, endpoint, key, secret]);
    const result = await db.query('update public.push_subscriptions set p256dh=$1 where endpoint=$2 returning id', ['C'.repeat(87), endpoint]);
    assert.equal(result.rows.length, 1);
  });
  await test('owner cannot transfer endpoint or subscription ownership', async () => {
    await denied('update public.push_subscriptions set user_id=$1 where endpoint=$2', [wife, endpoint]);
    await denied('update public.push_subscriptions set endpoint=$1 where endpoint=$2', ['https://push.example.test/changed', endpoint]);
  });
  await as(wife);
  await test('other member cannot read/delete a subscription', async () => {
    assert.equal((await db.query('select * from public.push_subscriptions')).rows.length, 0);
    assert.equal((await db.query('delete from public.push_subscriptions where endpoint=$1 returning id', [endpoint])).rows.length, 0);
  });
  await test('global unique endpoint cannot be stolen through upsert', async () => {
    await denied(`insert into public.push_subscriptions(user_id,endpoint,p256dh,auth) values($1,$2,$3,$4)
      on conflict(endpoint) do update set user_id=excluded.user_id,p256dh=excluded.p256dh`, [wife, endpoint, key, secret]);
  });
  await test('forged push user rejected', async () => {
    await denied('insert into public.push_subscriptions(user_id,endpoint,p256dh,auth) values($1,$2,$3,$4)', [husband, 'https://push.example.test/forged', key, secret]);
  });
  await test('private counter and arbitrary limit helper inaccessible', async () => {
    await denied('select * from private.api_rate_limits');
    await denied("select private.consume_rate_limit('message', 99999)");
    await denied("select public.check_api_rate_limit('message')", [], '22023');
  });
  await admin();
  await db.exec('delete from private.api_rate_limits');
  await as(husband);
  await test('database defends direct authenticated inserts at 30/minute', async () => {
    for (let i = 0; i < 30; i++) await insertMessage(`Rate check ${i}`);
    await denied('insert into public.messages(sender_id,body,client_nonce) values($1,$2,$3)', [husband, 'Too many', randomUUID()], 'P0001');
  });
  await test('atomic API rate wrapper enforces fixed action budgets', async () => {
    for (let i = 0; i < 20; i++) await db.query("select public.check_api_rate_limit('push_subscribe')");
    await denied("select public.check_api_rate_limit('push_subscribe')", [], 'P0001');
    await db.query("select public.check_api_rate_limit('push_delete')");
    await db.query("select public.check_api_rate_limit('read')");
  });
  await as(outsider);
  await test('authenticated outsider reads no profiles, chat, receipts, or pushes', async () => {
    for (const table of ['members', 'messages', 'read_receipts', 'push_subscriptions']) {
      assert.equal((await db.query(`select * from public.${table}`)).rows.length, 0);
    }
  });
  await test('authenticated outsider cannot write or invoke useful RPCs', async () => {
    await denied('insert into public.messages(sender_id,body,client_nonce) values($1,$2,$3)', [husband, 'Attack', randomUUID()]);
    await denied('select * from public.mark_read($1)', [first.id]);
    await denied("select public.check_api_rate_limit('read')");
    await denied('insert into public.push_subscriptions(user_id,endpoint,p256dh,auth) values($1,$2,$3,$4)', [husband, 'https://push.example.test/outsider', key, secret]);
  });
  await as(null, 'anon');
  await test('anonymous role has no chat data or write/RPC privileges', async () => {
    for (const table of ['members', 'messages', 'read_receipts', 'push_subscriptions']) {
      await denied(`select * from public.${table}`);
    }
    await denied('insert into public.messages(sender_id,body,client_nonce) values($1,$2,$3)', [husband, 'Attack', randomUUID()]);
    await denied('select * from public.mark_read($1)', [first.id]);
    await denied("select public.check_api_rate_limit('read')");
  });
  await as(husband);
  await test('private Presence policies restrict membership, topic, and extension', async () => {
    await db.query("select set_config('realtime.topic','ourchat:couple',false)");
    await db.query("insert into realtime.messages(extension) values('presence')");
    assert.equal((await db.query('select * from realtime.messages')).rows.length, 1);
    await denied("insert into realtime.messages(extension) values('broadcast')");
    await db.query("select set_config('realtime.topic','another-room',false)");
    assert.equal((await db.query('select * from realtime.messages')).rows.length, 0);
    await denied("insert into realtime.messages(extension) values('presence')");
    await as(outsider);
    await db.query("select set_config('realtime.topic','ourchat:couple',false)");
    assert.equal((await db.query('select * from realtime.messages')).rows.length, 0);
    await denied("insert into realtime.messages(extension) values('presence')");
  });
  await as(null, 'service_role');
  await test('server role can read/delete push endpoints, not write messages or members', async () => {
    assert.equal((await db.query('select * from public.push_subscriptions')).rows.length, 1);
    await denied('insert into public.messages(sender_id,body,client_nonce) values($1,$2,$3)', [husband, 'Admin bypass', randomUUID()]);
    await denied('select * from public.members');
    assert.equal((await db.query('delete from public.push_subscriptions where endpoint=$1 returning id', [endpoint])).rows.length, 1);
  });
  await admin();
  await test('publication includes exactly both live-data tables', async () => {
    const result = await db.query("select tablename from pg_publication_tables where pubname='supabase_realtime' order by tablename");
    assert.deepEqual(result.rows.map((r) => r.tablename), ['messages', 'read_receipts']);
  });
  await test('all privileged helpers have empty search_path and restricted execute', async () => {
    const result = await db.query(`select p.proname, p.prosecdef, p.proconfig,
      has_function_privilege('anon',p.oid,'execute') as anon_exec
      from pg_proc p join pg_namespace n on n.oid=p.pronamespace where n.nspname='private'`);
    assert.ok(result.rows.length >= 7);
    assert.ok(result.rows.every((r) => r.prosecdef && r.proconfig.includes('search_path=""') && !r.anon_exec));
  });

  const retentionMigration = await readFile(new URL('../../supabase/migrations/202610190002_message_retention.sql', import.meta.url), 'utf8');
  await test('retention installation changes only its read policy, index and owner helper without pruning any rows', async () => {
    const before = await schemaSnapshot(true);
    await db.exec(retentionMigration);
    const after = await schemaSnapshot(true);
    const indexId = (await db.query("select 'public.messages_created_at_idx'::regclass::oid as id")).rows[0].id;
    const functionId = (await db.query("select 'private.prune_expired_messages()'::regprocedure::oid as id")).rows[0].id;
    after.relations = after.relations.filter((row) => row.oid !== indexId);
    after.columns = after.columns.filter((row) => row.attrelid !== indexId);
    after.functions = after.functions.filter((row) => row.oid !== functionId);
    const previousPolicy = before.policies.find((row) => row.tablename === 'messages' && row.policyname === 'messages_read_pair');
    const nextPolicy = after.policies.find((row) => row.tablename === 'messages' && row.policyname === 'messages_read_pair');
    assert.match(nextPolicy.qual, /private\.is_member\(\).*created_at > .*statement_timestamp\(\).*168:00:00/s);
    nextPolicy.qual = previousPolicy.qual;
    assert.deepEqual(after, before);
  });
  await test('retention replay preserves all row, function, policy, constraint and index identities', async () => {
    const before = await schemaSnapshot(true);
    await db.exec(retentionMigration);
    assert.deepEqual(await schemaSnapshot(true), before);
  });
  const cleanupDefinition = (await db.query("select pg_get_functiondef('private.prune_expired_messages()'::regprocedure) as definition")).rows[0].definition;
  for (const [name, change, restore] of [
    ['weakened message read policy',
      'alter policy messages_read_pair on public.messages using (true)',
      "alter policy messages_read_pair on public.messages using ((select private.is_member()) and created_at > statement_timestamp() - interval '168 hours')"],
    ['missing read policy expression',
      'drop policy messages_read_pair on public.messages; create policy messages_read_pair on public.messages for select to authenticated',
      "alter policy messages_read_pair on public.messages using ((select private.is_member()) and created_at > statement_timestamp() - interval '168 hours')"],
    ['different retention duration',
      "alter policy messages_read_pair on public.messages using ((select private.is_member()) and created_at > statement_timestamp() - interval '169 hours')",
      "alter policy messages_read_pair on public.messages using ((select private.is_member()) and created_at > statement_timestamp() - interval '168 hours')"],
    ['additional permissive message policy',
      'create policy unexpected_retention_read on public.messages for select to authenticated using (true)',
      'drop policy unexpected_retention_read on public.messages'],
    ['restored character cap',
      'alter table public.messages drop constraint messages_body_check; alter table public.messages add constraint messages_body_check check (body = btrim(body) and char_length(body) between 1 and 1000000)',
      'alter table public.messages drop constraint messages_body_check; alter table public.messages add constraint messages_body_check check (body = btrim(body) and char_length(body) >= 1)'],
    ['changed receipt foreign key action',
      'alter table public.read_receipts drop constraint read_receipts_last_read_message_id_fkey; alter table public.read_receipts add constraint read_receipts_last_read_message_id_fkey foreign key(last_read_message_id) references public.messages(id) on delete set null',
      'alter table public.read_receipts drop constraint read_receipts_last_read_message_id_fkey; alter table public.read_receipts add constraint read_receipts_last_read_message_id_fkey foreign key(last_read_message_id) references public.messages(id) on delete restrict'],
    ['changed receipt trigger security',
      'alter function private.prepare_receipt() security invoker',
      'alter function private.prepare_receipt() security definer'],
    ['client message DELETE grant',
      'grant delete on public.messages to authenticated',
      'revoke delete on public.messages from authenticated'],
    ['conflicting cleanup index',
      'drop index public.messages_created_at_idx; create index messages_created_at_idx on public.messages(id)',
      'drop index public.messages_created_at_idx; create index messages_created_at_idx on public.messages(created_at)'],
    ['conflicting cleanup implementation',
      "create or replace function private.prune_expired_messages() returns bigint language plpgsql security definer set search_path='' set lock_timeout='5s' as $$begin return 0;end;$$",
      cleanupDefinition],
    ['client execution grant on cleanup',
      'grant execute on function private.prune_expired_messages() to authenticated',
      'revoke execute on function private.prune_expired_messages() from authenticated'],
  ]) {
    await test(`retention migration refuses ${name} atomically`, async () => {
      await db.exec(change);
      const before = await schemaSnapshot(true);
      await assert.rejects(db.exec(retentionMigration), (e) => e.code === '55000');
      await db.exec('rollback');
      assert.deepEqual(await schemaSnapshot(true), before);
      await db.exec(restore);
    });
  }
  await test('cleanup function is owner-only with empty search_path, fixed timeout and no configurable age', async () => {
    const { rows } = await db.query(`select p.pronargs,p.prorettype='int8'::regtype as returns_count,
      p.prosecdef,p.proconfig,
      has_function_privilege('anon',p.oid,'execute') as anon_exec,
      has_function_privilege('authenticated',p.oid,'execute') as member_exec,
      has_function_privilege('service_role',p.oid,'execute') as server_exec
      from pg_proc p where p.oid='private.prune_expired_messages()'::regprocedure`);
    assert.deepEqual(rows[0], { pronargs: 0, returns_count: true, prosecdef: true,
      proconfig: ['search_path=""', 'lock_timeout=5s'], anon_exec: false, member_exec: false, server_exec: false });
    for (const [user, role] of [[husband, 'authenticated'], [wife, 'authenticated'], [outsider, 'authenticated'], [null, 'anon'], [null, 'service_role']]) {
      await as(user, role);
      await denied('select private.prune_expired_messages()');
    }
    await admin();
  });

  await db.exec('delete from private.api_rate_limits');
  await as(husband);
  const boundaryBefore = await insertMessage('One millisecond expired');
  const boundaryExact = await insertMessage('Exactly seven days old');
  const boundaryAfter = await insertMessage('One millisecond retained');
  await admin();
  await test('both members lose exact-168-hour and older rows immediately before physical cleanup', async () => {
    // One DO command fixes statement_timestamp across the writes and policy reads.
    // Fixture timestamps are owner-only updates and are rolled back afterwards.
    await db.exec('begin');
    try {
      await db.exec(`do $$declare v_user uuid; v_count integer; begin
        update public.messages set created_at=statement_timestamp()-interval '168 hours'-interval '1 millisecond' where id=${boundaryBefore.id};
        update public.messages set created_at=statement_timestamp()-interval '168 hours' where id=${boundaryExact.id};
        update public.messages set created_at=statement_timestamp()-interval '168 hours'+interval '1 millisecond' where id=${boundaryAfter.id};
        foreach v_user in array array['${husband}'::uuid,'${wife}'::uuid] loop
          perform set_config('request.jwt.claim.sub',v_user::text,true);
          set local role authenticated;
          select count(*) into v_count from public.messages where id in (${boundaryBefore.id},${boundaryExact.id},${boundaryAfter.id});
          if v_count<>1 or not exists(select 1 from public.messages where id=${boundaryAfter.id}) then
            raise exception 'Seven-day member visibility boundary failed';
          end if;
          begin
            perform public.mark_read(${boundaryExact.id});
            raise exception 'Expired message accepted by mark_read';
          exception when invalid_parameter_value then null;
          end;
          reset role;
        end loop;
        perform set_config('request.jwt.claim.sub','${outsider}',true);
        set local role authenticated;
        if exists(select 1 from public.messages) then raise exception 'Outsider retention policy bypass';end if;
        reset role;
        set local role anon;
        begin
          perform 1 from public.messages;
          raise exception 'Anonymous retention read unexpectedly allowed';
        exception when insufficient_privilege then null;
        end;
        reset role;
        if (select count(*) from public.messages where id in (${boundaryBefore.id},${boundaryExact.id},${boundaryAfter.id}))<>3 then
          raise exception 'Visibility filtering deleted a stored row';
        end if;
      end;$$;`);
    } finally { await db.exec('rollback'); }
  });
  await test('fixed 168 hours is seven elapsed days even across a daylight-saving change', async () => {
    await db.exec("set time zone 'America/New_York'");
    try {
      const { rows } = await db.query(`select extract(epoch from ('2026-11-03T00:00:00-05:00'::timestamptz -
        ('2026-11-03T00:00:00-05:00'::timestamptz - interval '168 hours')))::integer as elapsed,
        extract(epoch from ('2026-11-03T00:00:00-05:00'::timestamptz -
        ('2026-11-03T00:00:00-05:00'::timestamptz - interval '7 days')))::integer as calendar_elapsed`);
      assert.equal(rows[0].elapsed, 604800);
      assert.notEqual(rows[0].calendar_elapsed, 604800);
    } finally { await db.exec("set time zone 'UTC'"); }
  });

  await db.exec('delete from public.read_receipts');
  await as(husband);
  const expiredA = await insertMessage('Expired A', husband);
  const expiredB = await insertMessage('Expired B', husband);
  const retained = await insertMessage('Retained long body ' + '한글 👋'.repeat(2000), husband);
  await db.query('select * from public.mark_read($1)', [expiredA.id]);
  await as(wife);
  await db.query('select * from public.mark_read($1)', [retained.id]);
  await admin();
  await db.query("update public.messages set created_at=clock_timestamp()-interval '192 hours' where id in ($1,$2)", [expiredA.id, expiredB.id]);
  await test('expired IDs cannot be read or marked even while their stored rows await cron', async () => {
    for (const user of [husband, wife]) {
      await as(user);
      assert.equal((await db.query('select id from public.messages where id in ($1,$2)', [expiredA.id, expiredB.id])).rows.length, 0);
      await denied('select * from public.mark_read($1)', [expiredA.id], '22023');
      await denied('delete from public.messages where id=$1', [expiredA.id]);
    }
    await admin();
    assert.equal((await db.query('select count(*)::integer as n from public.messages where id in ($1,$2)', [expiredA.id, expiredB.id])).rows[0].n, 2);
  });
  await test('owner cleanup deletes expired rows and only their referencing receipts atomically', async () => {
    await db.query("select set_config('request.jwt.claim.sub','',false)");
    const before = await schemaSnapshot(true);
    const memberRows = (await db.query('select * from public.members order by user_id')).rows;
    const freshReceipt = (await db.query('select * from public.read_receipts where user_id=$1', [wife])).rows;
    const result = await db.query('select private.prune_expired_messages()::text as deleted');
    assert.equal(result.rows[0].deleted, '2');
    assert.equal((await db.query('select count(*)::integer as n from public.messages where id in ($1,$2)', [expiredA.id, expiredB.id])).rows[0].n, 0);
    assert.equal((await db.query('select * from public.read_receipts where user_id=$1', [husband])).rows.length, 0);
    assert.deepEqual((await db.query('select * from public.read_receipts where user_id=$1', [wife])).rows, freshReceipt);
    assert.deepEqual((await db.query('select * from public.members order by user_id')).rows, memberRows);
    const after = await schemaSnapshot(true);
    before.messages = before.messages.filter((row) => ![expiredA.id, expiredB.id].includes(row.id));
    assert.deepEqual(after, before);
    await as(husband);
    assert.equal((await db.query('select body from public.messages where id=$1', [retained.id])).rows[0].body, retained.body);
    const next = await insertMessage('Normal sending and receipt recreation after pruning');
    await db.query('select * from public.mark_read($1)', [next.id]);
    await admin();
  });
  await test('repeated cleanup is a no-op and preserves a null read receipt', async () => {
    await db.query('delete from public.read_receipts where user_id=$1', [husband]);
    await as(husband);
    await db.query('insert into public.read_receipts(user_id,last_read_message_id) values($1,null)', [husband]);
    await admin();
    const before = await schemaSnapshot(true);
    const receipts = (await db.query('select * from public.read_receipts order by user_id')).rows;
    assert.equal((await db.query('select private.prune_expired_messages()::text as deleted')).rows[0].deleted, '0');
    assert.deepEqual(await schemaSnapshot(true), before);
    assert.deepEqual((await db.query('select * from public.read_receipts order by user_id')).rows, receipts);
  });
  await test('scheduler fails closed when pg_cron is unavailable instead of claiming automatic cleanup', async () => {
    const scheduleMigration = await readFile(new URL('../../supabase/migrations/202610190003_schedule_message_retention.sql', import.meta.url), 'utf8');
    assert.equal((await db.query("select name from pg_available_extensions where name='pg_cron'")).rows.length, 0);
    const before = await schemaSnapshot(true);
    await assert.rejects(db.exec(scheduleMigration), (error) => error.code === '55000' && /pg_cron is unavailable/.test(error.message));
    await db.exec('rollback');
    assert.deepEqual(await schemaSnapshot(true), before);
  });
  console.log(`\nPASS: ${checks} database ACL/RLS/trigger/RPC checks against the actual migration.`);
} finally {
  await db.close();
}
