-- Remove only the message character cap. Run as the Supabase database owner.
-- The original migration remains immutable; existing installations apply this one.
begin;
set local lock_timeout = '5s';
set local search_path = pg_catalog;

-- Serialize the catalog checks and replacement with writes and concurrent DDL.
-- A busy room fails within five seconds instead of waiting indefinitely.
lock table public.messages in access exclusive mode;

do $$
declare
  v_body_attnum smallint;
  v_constraint record;
  v_original constant text := 'CHECK (((body = btrim(body)) AND ((char_length(body) >= 1) AND (char_length(body) <= 2000))))';
  v_unlimited constant text := 'CHECK (((body = btrim(body)) AND (char_length(body) >= 1)))';
begin
  if not exists (
    select 1 from pg_catalog.pg_class
    where oid = 'public.messages'::pg_catalog.regclass
      and relkind = 'r' and not relispartition and relrowsecurity
  ) or exists (
    select 1 from pg_catalog.pg_inherits
    where inhrelid = 'public.messages'::pg_catalog.regclass
       or inhparent = 'public.messages'::pg_catalog.regclass
  ) then
    raise exception 'Refusing character-limit migration: unexpected public.messages table schema'
      using errcode = '55000';
  end if;

  select attnum into v_body_attnum
  from pg_catalog.pg_attribute
  where attrelid = 'public.messages'::pg_catalog.regclass and attname = 'body'
    and not attisdropped and atttypid = 'pg_catalog.text'::pg_catalog.regtype
    and atttypmod = -1 and attnotnull and not atthasdef
    and attidentity = '' and attgenerated = '';
  if v_body_attnum is null then
    raise exception 'Refusing character-limit migration: expected body text NOT NULL without a default'
      using errcode = '55000';
  end if;

  select c.*, pg_catalog.pg_get_constraintdef(c.oid, false) as definition
    into v_constraint
  from pg_catalog.pg_constraint c
  where c.conrelid = 'public.messages'::pg_catalog.regclass
    and c.conname = 'messages_body_check';
  if not found then
    raise exception 'Refusing character-limit migration: missing messages_body_check'
      using errcode = '55000';
  end if;
  if v_constraint.contype <> 'c' or not v_constraint.convalidated
    or v_constraint.connoinherit or not v_constraint.conislocal
    or v_constraint.coninhcount <> 0 or v_constraint.conparentid <> 0
    or v_constraint.conkey <> array[v_body_attnum]::smallint[]
    or v_constraint.definition not in (v_original, v_unlimited)
    or exists (
      select 1 from pg_catalog.pg_constraint c
      where c.conrelid = 'public.messages'::pg_catalog.regclass
        and c.contype = 'c' and v_body_attnum = any(c.conkey)
        and c.oid <> v_constraint.oid
    ) then
    raise exception 'Refusing character-limit migration: unexpected body constraint; inspect before changing it'
      using errcode = '55000';
  end if;

  -- Replaying this exact migration is a no-op; unknown states always fail closed.
  if v_constraint.definition = v_unlimited then
    return;
  end if;

  alter table public.messages drop constraint messages_body_check;
  alter table public.messages add constraint messages_body_check
    check (body = pg_catalog.btrim(body) and pg_catalog.char_length(body) >= 1);
end;
$$;

commit;
