-- OurChat V1. Run as the Supabase database owner, not as an application user.
-- The private schema must NOT be added to Supabase's exposed API schemas.
begin;

create schema if not exists private;
revoke all on schema private from public, anon, authenticated;
grant usage on schema private to authenticated;

create table public.members (
  user_id uuid primary key references auth.users(id) on delete restrict,
  display_name text not null check (
    display_name = btrim(display_name) and char_length(display_name) between 1 and 80
  ),
  created_at timestamptz not null default now(),
  -- A unique two-value domain is a final concurrency-safe cap, even at REPEATABLE READ.
  member_slot smallint not null unique check (member_slot in (1, 2))
);

create function private.enforce_two_members()
returns trigger language plpgsql security definer set search_path = '' as $$
begin
  perform pg_catalog.pg_advisory_xact_lock(17439021, 2);
  if tg_op = 'UPDATE' then
    if new.user_id <> old.user_id or new.member_slot <> old.member_slot then
      raise exception 'Member identity and slot cannot be changed' using errcode = '23514';
    end if;
    return new;
  end if;
  if (select pg_catalog.count(*) from public.members) >= 2 then
    raise exception 'OurChat allows exactly two registered members at most' using errcode = '23514';
  end if;
  if new.member_slot is null then
    select s into new.member_slot
    from pg_catalog.generate_series(1, 2) as s
    where not exists (select 1 from public.members m where m.member_slot = s)
    order by s limit 1;
  end if;
  return new;
end;
$$;
revoke all on function private.enforce_two_members() from public, anon, authenticated;
create trigger enforce_two_members before insert or update on public.members
for each row execute function private.enforce_two_members();

-- SECURITY DEFINER intentionally bypasses members RLS without a recursive policy.
-- The caller cannot choose another identity. No table name is search-path dependent.
create function private.is_member()
returns boolean language sql stable security definer set search_path = '' as $$
  select auth.uid() is not null
    and exists (select 1 from public.members m where m.user_id = auth.uid());
$$;
revoke all on function private.is_member() from public, anon, authenticated;
grant execute on function private.is_member() to authenticated;

create table public.messages (
  id bigint generated always as identity primary key,
  sender_id uuid not null references public.members(user_id) on delete restrict,
  body text not null check (
    body = btrim(body) and char_length(body) between 1 and 2000
  ),
  client_nonce uuid not null,
  created_at timestamptz not null default now(),
  unique (sender_id, client_nonce)
);
create index messages_id_desc_idx on public.messages(id desc);

-- A sequence alone orders allocation, not commit: transaction N+1 could commit
-- before N, making an id > last_seen cursor permanently skip N. Serialize this
-- single room BEFORE the executor evaluates any row's identity/default values.
-- A BEFORE ROW trigger is too late because its NEW.id is already allocated.
create function private.serialize_message_inserts()
returns trigger language plpgsql security definer set search_path = '' as $$
begin
  perform pg_catalog.pg_advisory_xact_lock(17439021, 3);
  return null; -- Ignored for a statement trigger; the lock lasts until commit.
end;
$$;
revoke all on function private.serialize_message_inserts() from public, anon, authenticated;
create trigger serialize_message_inserts before insert on public.messages
for each statement execute function private.serialize_message_inserts();

create table public.read_receipts (
  user_id uuid primary key references public.members(user_id) on delete cascade,
  last_read_message_id bigint references public.messages(id) on delete restrict,
  updated_at timestamptz not null default now()
);

create table public.push_subscriptions (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.members(user_id) on delete cascade,
  endpoint text not null unique check (
    char_length(endpoint) between 12 and 2048 and endpoint like 'https://%'
  ),
  p256dh text not null check (p256dh ~ '^[A-Za-z0-9_-]{87}$'),
  auth text not null check (auth ~ '^[A-Za-z0-9_-]{22}$'),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index push_subscriptions_user_id_idx on public.push_subscriptions(user_id);

create table private.api_rate_limits (
  user_id uuid not null references public.members(user_id) on delete cascade,
  action text not null,
  window_start timestamptz not null,
  hits integer not null check (hits > 0),
  primary key (user_id, action)
);
revoke all on table private.api_rate_limits from public, anon, authenticated, service_role;

-- Atomic counter: a direct authenticated PostgREST insert receives the same limit
-- as a Route Handler insert. A failed transaction never consumes a message/nonce.
create function private.consume_rate_limit(p_action text, p_limit integer)
returns void language plpgsql security definer set search_path = '' as $$
declare
  v_user uuid := auth.uid();
  v_window timestamptz := pg_catalog.date_trunc('minute', pg_catalog.clock_timestamp());
  v_hits integer;
begin
  if v_user is null or not private.is_member() then
    raise exception 'Membership required' using errcode = '42501';
  end if;
  insert into private.api_rate_limits as r (user_id, action, window_start, hits)
  values (v_user, p_action, v_window, 1)
  on conflict (user_id, action) do update set
    window_start = excluded.window_start,
    hits = case when r.window_start = excluded.window_start then r.hits + 1 else 1 end
  returning hits into v_hits;
  if v_hits > p_limit then
    raise exception 'Request rate limit exceeded. Please try again shortly.' using errcode = 'P0001';
  end if;
end;
$$;
revoke all on function private.consume_rate_limit(text, integer) from public, anon, authenticated;

create function public.check_api_rate_limit(p_action text)
returns void language plpgsql security invoker set search_path = '' as $$
begin
  -- Deliberately delegate to fixed wrappers; authenticated callers cannot choose limits.
  perform private.check_api_rate_limit(p_action);
end;
$$;

create function private.check_api_rate_limit(p_action text)
returns void language plpgsql security definer set search_path = '' as $$
begin
  case p_action
    when 'read' then perform private.consume_rate_limit('read', 120);
    when 'push_subscribe' then perform private.consume_rate_limit('push_subscribe', 20);
    when 'push_delete' then perform private.consume_rate_limit('push_delete', 20);
    else raise exception 'Unsupported rate-limit action' using errcode = '22023';
  end case;
end;
$$;
revoke all on function private.check_api_rate_limit(text) from public, anon, authenticated;
grant execute on function private.check_api_rate_limit(text) to authenticated;
revoke all on function public.check_api_rate_limit(text) from public, anon, authenticated;
grant execute on function public.check_api_rate_limit(text) to authenticated;

create function private.prepare_message()
returns trigger language plpgsql security definer set search_path = '' as $$
begin
  if auth.uid() is null or new.sender_id <> auth.uid() or not private.is_member() then
    raise exception 'Messages may only be sent by their authenticated member' using errcode = '42501';
  end if;
  -- Match JavaScript String.trim(), including Unicode spaces and CR/LF/tab.
  new.body := pg_catalog.btrim(new.body,
    U&'\0009\000A\000B\000C\000D\0020\00A0\1680\2000\2001\2002\2003\2004\2005\2006\2007\2008\2009\200A\2028\2029\202F\205F\3000\FEFF');
  new.created_at := pg_catalog.clock_timestamp();
  perform private.consume_rate_limit('message', 30);
  return new;
end;
$$;
revoke all on function private.prepare_message() from public, anon, authenticated;
create trigger prepare_message before insert on public.messages
for each row execute function private.prepare_message();

-- Backward direct table writes are also clamped, so bypassing mark_read cannot
-- regress a receipt. UPSERT/UPDATE lock the row and evaluate GREATEST atomically.
create function private.prepare_receipt()
returns trigger language plpgsql security definer set search_path = '' as $$
begin
  if auth.uid() is null or new.user_id <> auth.uid() or not private.is_member() then
    raise exception 'Read receipts may only be changed by their member' using errcode = '42501';
  end if;
  if tg_op = 'UPDATE' then
    new.last_read_message_id := greatest(old.last_read_message_id, new.last_read_message_id);
    new.user_id := old.user_id;
  end if;
  new.updated_at := pg_catalog.clock_timestamp();
  return new;
end;
$$;
revoke all on function private.prepare_receipt() from public, anon, authenticated;
create trigger prepare_receipt before insert or update on public.read_receipts
for each row execute function private.prepare_receipt();

create function private.prepare_push_subscription()
returns trigger language plpgsql security definer set search_path = '' as $$
begin
  if auth.uid() is null or new.user_id <> auth.uid() or not private.is_member() then
    raise exception 'Subscriptions may only be changed by their owner' using errcode = '42501';
  end if;
  if tg_op = 'UPDATE' then
    if new.user_id <> old.user_id or new.endpoint <> old.endpoint or new.id <> old.id then
      raise exception 'Subscription identity and owner cannot be changed' using errcode = '42501';
    end if;
    new.created_at := old.created_at;
  else
    new.created_at := pg_catalog.clock_timestamp();
  end if;
  new.updated_at := pg_catalog.clock_timestamp();
  return new;
end;
$$;
revoke all on function private.prepare_push_subscription() from public, anon, authenticated;
create trigger prepare_push_subscription before insert or update on public.push_subscriptions
for each row execute function private.prepare_push_subscription();

alter table public.members enable row level security;
alter table public.messages enable row level security;
alter table public.read_receipts enable row level security;
alter table public.push_subscriptions enable row level security;
alter table private.api_rate_limits enable row level security;

revoke all on table public.members, public.messages, public.read_receipts,
  public.push_subscriptions from public, anon, authenticated, service_role;
revoke all on sequence public.messages_id_seq from public, anon, authenticated, service_role;
grant select on table public.members to authenticated;
grant select, insert on table public.messages to authenticated;
grant usage on sequence public.messages_id_seq to authenticated;
grant select, insert, update on table public.read_receipts to authenticated;
grant select, insert, update, delete on table public.push_subscriptions to authenticated;
-- Used only server-side, after the Route Handler verifies session + membership.
grant select, delete on table public.push_subscriptions to service_role;

create policy members_read_pair on public.members for select to authenticated
using ((select private.is_member()));
create policy messages_read_pair on public.messages for select to authenticated
using ((select private.is_member()));
create policy messages_insert_self on public.messages for insert to authenticated
with check ((select private.is_member()) and sender_id = (select auth.uid()));
create policy receipts_read_pair on public.read_receipts for select to authenticated
using ((select private.is_member()));
create policy receipts_insert_self on public.read_receipts for insert to authenticated
with check ((select private.is_member()) and user_id = (select auth.uid()));
create policy receipts_update_self on public.read_receipts for update to authenticated
using ((select private.is_member()) and user_id = (select auth.uid()))
with check ((select private.is_member()) and user_id = (select auth.uid()));
create policy subscriptions_read_self on public.push_subscriptions for select to authenticated
using ((select private.is_member()) and user_id = (select auth.uid()));
create policy subscriptions_insert_self on public.push_subscriptions for insert to authenticated
with check ((select private.is_member()) and user_id = (select auth.uid()));
create policy subscriptions_update_self on public.push_subscriptions for update to authenticated
using ((select private.is_member()) and user_id = (select auth.uid()))
with check ((select private.is_member()) and user_id = (select auth.uid()));
create policy subscriptions_delete_self on public.push_subscriptions for delete to authenticated
using ((select private.is_member()) and user_id = (select auth.uid()));

create function public.mark_read(p_message_id bigint)
returns table (user_id uuid, last_read_message_id text, updated_at timestamptz)
language plpgsql security invoker set search_path = '' as $$
begin
  if not private.is_member() then
    raise exception 'Membership required' using errcode = '42501';
  end if;
  if p_message_id is null or not exists (select 1 from public.messages m where m.id = p_message_id) then
    raise exception 'Message does not exist or is not visible' using errcode = '22023';
  end if;
  return query
    insert into public.read_receipts as r (user_id, last_read_message_id)
    values (auth.uid(), p_message_id)
    on conflict on constraint read_receipts_pkey do update set
      last_read_message_id = greatest(r.last_read_message_id, excluded.last_read_message_id)
    returning r.user_id, r.last_read_message_id::text, r.updated_at;
end;
$$;
revoke all on function public.mark_read(bigint) from public, anon, authenticated;
grant execute on function public.mark_read(bigint) to authenticated;

-- Postgres Changes observes table RLS. Client identity must be set before subscribe.
alter table public.messages replica identity full;
alter table public.read_receipts replica identity full;
do $$
begin
  if not exists (select 1 from pg_catalog.pg_publication where pubname = 'supabase_realtime') then
    create publication supabase_realtime;
  end if;
  if not exists (select 1 from pg_catalog.pg_publication_tables where pubname = 'supabase_realtime' and schemaname = 'public' and tablename = 'messages') then
    alter publication supabase_realtime add table public.messages;
  end if;
  if not exists (select 1 from pg_catalog.pg_publication_tables where pubname = 'supabase_realtime' and schemaname = 'public' and tablename = 'read_receipts') then
    alter publication supabase_realtime add table public.read_receipts;
  end if;
end;
$$;

-- Supabase Presence uses a PRIVATE channel. Do not allow public-channel fallback.
-- This conditional supports older/local installs lacking the Realtime schema.
do $$
begin
  if pg_catalog.to_regclass('realtime.messages') is not null then
    execute 'alter table realtime.messages enable row level security';
    execute 'create policy ourchat_presence_read on realtime.messages for select to authenticated using ((select private.is_member()) and (select realtime.topic()) = ''ourchat:couple'' and extension = ''presence'')';
    execute 'create policy ourchat_presence_write on realtime.messages for insert to authenticated with check ((select private.is_member()) and (select realtime.topic()) = ''ourchat:couple'' and extension = ''presence'')';
  end if;
end;
$$;

commit;
