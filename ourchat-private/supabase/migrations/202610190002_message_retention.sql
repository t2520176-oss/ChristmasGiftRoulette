-- Fixed rolling seven-day retention. Run as the existing OurChat database owner.
-- Installing this migration hides expired rows but does not delete any rows.
-- Physical pruning is owner-only and scheduled separately by the next migration.
begin;
set local lock_timeout = '5s';
set local search_path = pg_catalog;
lock table public.messages, public.read_receipts in access exclusive mode;

do $migration$
declare
  v_owner oid := (select oid from pg_catalog.pg_roles where rolname = current_user);
  v_authenticated oid := (select oid from pg_catalog.pg_roles where rolname = 'authenticated');
  v_policy record;
  v_index oid;
  v_function record;
  v_original constant text := '( SELECT private.is_member() AS is_member)';
  v_retained constant text := '(( SELECT private.is_member() AS is_member) AND (created_at > (statement_timestamp() - ''168:00:00''::interval)))';
  v_cleanup_source constant text := $cleanup$
declare
  v_cutoff timestamptz := pg_catalog.clock_timestamp() - interval '168 hours';
  v_deleted bigint;
begin
  -- Fixed order prevents new message/receipt writes from racing the restrictive FK.
  -- Reads remain available; a busy writer causes a bounded failure and cron retry.
  lock table public.messages in share row exclusive mode;
  lock table public.read_receipts in share row exclusive mode;
  delete from public.read_receipts as r
  using public.messages as m
  where r.last_read_message_id = m.id and m.created_at <= v_cutoff;
  delete from public.messages as m where m.created_at <= v_cutoff;
  get diagnostics v_deleted = row_count;
  return v_deleted;
end;
$cleanup$;
begin
  -- Refuse foreign owners, partitioned/inherited tables, or a disabled RLS boundary.
  if v_owner is null or v_authenticated is null or (
    select pg_catalog.count(*) from pg_catalog.pg_class
    where oid in ('public.members'::regclass, 'public.messages'::regclass,
      'public.read_receipts'::regclass)
      and relkind = 'r' and not relispartition and relrowsecurity and not relforcerowsecurity
      and relowner = v_owner
  ) <> 3 or exists (
    select 1 from pg_catalog.pg_inherits
    where inhrelid in ('public.messages'::regclass, 'public.read_receipts'::regclass)
       or inhparent in ('public.messages'::regclass, 'public.read_receipts'::regclass)
  ) then
    raise exception 'Refusing retention migration: unexpected table ownership or RLS schema' using errcode = '55000';
  end if;
  if (select pg_catalog.count(*) from pg_catalog.pg_attribute
      where attrelid = 'public.messages'::regclass and attnum > 0 and not attisdropped) <> 5
    or (select pg_catalog.count(*) from pg_catalog.pg_attribute
      where attrelid = 'public.read_receipts'::regclass and attnum > 0 and not attisdropped) <> 3
    or not exists (
      select 1 from pg_catalog.pg_attribute where attrelid = 'public.messages'::regclass
        and attname = 'created_at' and atttypid = 'timestamptz'::regtype and attnotnull
        and not attisdropped and attgenerated = '' and attidentity = ''
    ) or not exists (
      select 1 from pg_catalog.pg_attribute where attrelid = 'public.messages'::regclass
        and attname = 'body' and atttypid = 'text'::regtype and attnotnull
        and not attisdropped and not atthasdef and atttypmod = -1
    ) then
    raise exception 'Refusing retention migration: unexpected message or receipt columns' using errcode = '55000';
  end if;

  -- Require the completed character-limit migration, nonce guard, and original FK.
  if not exists (
    select 1 from pg_catalog.pg_constraint where conrelid = 'public.messages'::regclass
      and conname = 'messages_body_check' and contype = 'c' and convalidated
      and pg_catalog.pg_get_constraintdef(oid, false) = 'CHECK (((body = btrim(body)) AND (char_length(body) >= 1)))'
  ) or (select pg_catalog.count(*) from pg_catalog.pg_constraint c
      join pg_catalog.pg_attribute a on a.attrelid = c.conrelid and a.attnum = any(c.conkey)
      where c.conrelid = 'public.messages'::regclass and c.contype = 'c' and a.attname = 'body') <> 1
    or not exists (
      select 1 from pg_catalog.pg_constraint where conrelid = 'public.messages'::regclass
        and conname = 'messages_sender_id_client_nonce_key' and contype = 'u' and convalidated
        and pg_catalog.pg_get_constraintdef(oid, false) = 'UNIQUE (sender_id, client_nonce)'
    ) or not exists (
      select 1 from pg_catalog.pg_constraint where conrelid = 'public.read_receipts'::regclass
        and conname = 'read_receipts_last_read_message_id_fkey' and contype = 'f' and convalidated
        and confrelid = 'public.messages'::regclass and confdeltype = 'r'
        and pg_catalog.pg_get_constraintdef(oid, false) = 'FOREIGN KEY (last_read_message_id) REFERENCES public.messages(id) ON DELETE RESTRICT'
    ) or (select pg_catalog.count(*) from pg_catalog.pg_constraint
      where contype = 'f' and confrelid = 'public.messages'::regclass) <> 1 then
    raise exception 'Refusing retention migration: unexpected body, nonce, or message reference constraint' using errcode = '55000';
  end if;

  -- Known helper bodies are immutable in this migration; never replace receipt auth.
  if (select pg_catalog.count(*) from pg_catalog.pg_proc p
      where p.proowner = v_owner and p.prosecdef
        and p.proconfig = array['search_path=""']::text[] and (
          (p.oid = 'private.is_member()'::regprocedure and pg_catalog.md5(p.prosrc) = '8582095b3d24dee8364ba889a811ad22')
          or (p.oid = 'private.serialize_message_inserts()'::regprocedure and pg_catalog.md5(p.prosrc) = 'e500caade78cec9d5da6ef3df7906027')
          or (p.oid = 'private.prepare_message()'::regprocedure and pg_catalog.md5(p.prosrc) = '6e242796a74e0e8bf93e7a6e9437049c')
          or (p.oid = 'private.prepare_receipt()'::regprocedure and pg_catalog.md5(p.prosrc) = 'd2dc3c64ad69c21c6f22d3f02bb1cc19')
        )) <> 4 or (
      select pg_catalog.count(*) from pg_catalog.pg_trigger
      where tgrelid in ('public.messages'::regclass, 'public.read_receipts'::regclass)
        and not tgisinternal
    ) <> 3 or (
      select pg_catalog.count(*) from pg_catalog.pg_trigger where not tgisinternal and tgenabled = 'O' and (
        (tgrelid = 'public.messages'::regclass and tgname = 'serialize_message_inserts'
          and tgfoid = 'private.serialize_message_inserts()'::regprocedure and tgtype = 6)
        or (tgrelid = 'public.messages'::regclass and tgname = 'prepare_message'
          and tgfoid = 'private.prepare_message()'::regprocedure and tgtype = 7)
        or (tgrelid = 'public.read_receipts'::regclass and tgname = 'prepare_receipt'
          and tgfoid = 'private.prepare_receipt()'::regprocedure and tgtype = 23)
      )) <> 3 then
    raise exception 'Refusing retention migration: unexpected membership, sender, or receipt trigger' using errcode = '55000';
  end if;

  select p.*, pg_catalog.pg_get_expr(p.polqual, p.polrelid, false) as expression
  into v_policy from pg_catalog.pg_policy p
  where polrelid = 'public.messages'::regclass and polname = 'messages_read_pair';
  if not found then
    raise exception 'Refusing retention migration: missing message read policy' using errcode = '55000';
  end if;
  if v_policy.polcmd <> 'r' or not v_policy.polpermissive
    or v_policy.polroles <> array[v_authenticated]::oid[] or v_policy.polwithcheck is not null
    or v_policy.expression is null or v_policy.expression not in (v_original, v_retained)
    or (select pg_catalog.count(*) from pg_catalog.pg_policy where polrelid = 'public.messages'::regclass) <> 2
    or not exists (
      select 1 from pg_catalog.pg_policy where polrelid = 'public.messages'::regclass
        and polname = 'messages_insert_self' and polcmd = 'a' and polpermissive
        and polroles = array[v_authenticated]::oid[] and polqual is null
        and pg_catalog.pg_get_expr(polwithcheck, polrelid, false) = '(( SELECT private.is_member() AS is_member) AND (sender_id = ( SELECT auth.uid() AS uid)))'
    ) then
    raise exception 'Refusing retention migration: unexpected message policies' using errcode = '55000';
  end if;

  -- Preserve the client privilege boundary; pruning never grants DELETE or RPC access.
  if not pg_catalog.has_table_privilege('authenticated', 'public.messages', 'SELECT')
    or not pg_catalog.has_table_privilege('authenticated', 'public.messages', 'INSERT')
    or pg_catalog.has_table_privilege('authenticated', 'public.messages', 'UPDATE,DELETE,TRUNCATE,REFERENCES,TRIGGER')
    or pg_catalog.has_table_privilege('authenticated', 'public.read_receipts', 'DELETE,TRUNCATE,REFERENCES,TRIGGER')
    or pg_catalog.has_table_privilege('anon', 'public.messages', 'SELECT,INSERT,UPDATE,DELETE,TRUNCATE,REFERENCES,TRIGGER')
    or pg_catalog.has_table_privilege('service_role', 'public.messages', 'SELECT,INSERT,UPDATE,DELETE,TRUNCATE,REFERENCES,TRIGGER')
    or exists (select 1 from pg_catalog.pg_class c,
      lateral pg_catalog.aclexplode(c.relacl) a
      where c.oid in ('public.messages'::regclass, 'public.read_receipts'::regclass) and a.grantee = 0) then
    raise exception 'Refusing retention migration: unexpected client message privileges' using errcode = '55000';
  end if;

  v_index := pg_catalog.to_regclass('public.messages_created_at_idx');
  if v_index is not null and not exists (
    select 1 from pg_catalog.pg_index i join pg_catalog.pg_class c on c.oid = i.indexrelid
    where i.indexrelid = v_index and i.indrelid = 'public.messages'::regclass
      and i.indisvalid and i.indisready and not i.indisunique and i.indpred is null and i.indexprs is null
      and c.relowner = v_owner and pg_catalog.pg_get_indexdef(i.indexrelid) = 'CREATE INDEX messages_created_at_idx ON public.messages USING btree (created_at)'
  ) then
    raise exception 'Refusing retention migration: conflicting cleanup index' using errcode = '55000';
  end if;

  select p.* into v_function from pg_catalog.pg_proc p
  where p.pronamespace = 'private'::regnamespace and p.proname = 'prune_expired_messages';
  if found then
    if (select pg_catalog.count(*) from pg_catalog.pg_proc p
        where p.pronamespace = 'private'::regnamespace and p.proname = 'prune_expired_messages') <> 1
      or v_function.pronargs <> 0 or v_function.pronargdefaults <> 0 or v_function.proretset
      or v_function.prokind <> 'f' or v_function.prorettype <> 'int8'::regtype
      or v_function.prolang <> (select oid from pg_catalog.pg_language where lanname = 'plpgsql')
      or not v_function.prosecdef or v_function.proowner <> v_owner
      or v_function.provolatile <> 'v' or v_function.proparallel <> 'u'
      or v_function.proleakproof or v_function.proisstrict
      or v_function.proconfig is distinct from array['search_path=""', 'lock_timeout=5s']::text[]
      or v_function.prosrc is distinct from v_cleanup_source
      or exists (select 1 from pg_catalog.aclexplode(coalesce(v_function.proacl,
        pg_catalog.acldefault('f', v_function.proowner))) where grantee <> v_owner) then
      raise exception 'Refusing retention migration: conflicting cleanup function' using errcode = '55000';
    end if;
  else
    execute 'create function private.prune_expired_messages() returns bigint language plpgsql security definer set search_path = '''' set lock_timeout = ''5s'' as '
      || pg_catalog.quote_literal(v_cleanup_source);
    revoke all on function private.prune_expired_messages() from public, anon, authenticated, service_role;
    if exists (
      select 1 from pg_catalog.pg_proc p,
        lateral pg_catalog.aclexplode(coalesce(p.proacl, pg_catalog.acldefault('f', p.proowner))) a
      where p.oid = 'private.prune_expired_messages()'::regprocedure and a.grantee <> v_owner
    ) then
      raise exception 'Refusing retention migration: unexpected default cleanup privileges' using errcode = '55000';
    end if;
  end if;

  if v_index is null then
    create index messages_created_at_idx on public.messages(created_at);
  end if;
  if v_policy.expression = v_original then
    alter policy messages_read_pair on public.messages
      using ((select private.is_member()) and created_at > pg_catalog.statement_timestamp() - interval '168 hours');
  end if;
end;
$migration$;

commit;
