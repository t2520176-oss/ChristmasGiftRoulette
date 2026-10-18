-- ADMIN ONLY: run in the SQL Editor of a NEW, OurChat-only Supabase project.
-- Run supabase/migrations/202610180001_ourchat.sql in that project FIRST.
-- Never run this file or the migration in an existing application's project.
-- Disable public signups and anonymous sign-in, then create the two users in Auth.
-- Copy their Auth user UUIDs into the two placeholders below IN THE SQL EDITOR.
-- Do not commit real UUIDs, email addresses, passwords, or keys to this file.
-- Husband/Wife are defaults for NEW members. Existing display names stay unchanged.
-- Re-running with the same two UUIDs is safe; a different existing member aborts
-- the whole transaction. This script never updates or deletes existing members
-- and never changes database roles, grants, policies, or functions.

begin;

do $ourchat_seed$
declare
  first_auth_uuid_text text := 'REPLACE_FIRST_AUTH_UUID';
  second_auth_uuid_text text := 'REPLACE_SECOND_AUTH_UUID';
  first_auth_uuid uuid;
  second_auth_uuid uuid;
begin
  if nullif(btrim(first_auth_uuid_text), '') is null
    or nullif(btrim(second_auth_uuid_text), '') is null
    or first_auth_uuid_text = 'REPLACE_FIRST_AUTH_UUID'
    or second_auth_uuid_text = 'REPLACE_SECOND_AUTH_UUID' then
    raise exception 'Replace both UUID placeholders with the two existing Auth user UUIDs.'
      using errcode = '22023';
  end if;

  begin
    first_auth_uuid := btrim(first_auth_uuid_text)::uuid;
    second_auth_uuid := btrim(second_auth_uuid_text)::uuid;
  exception when invalid_text_representation then
    raise exception 'Both values must be valid UUIDs copied from Authentication > Users.'
      using errcode = '22023';
  end;

  if first_auth_uuid = second_auth_uuid then
    raise exception 'Choose two different Auth user UUIDs.' using errcode = '22023';
  end if;

  if pg_catalog.to_regclass('public.members') is null
    or pg_catalog.to_regprocedure('private.is_member()') is null then
    raise exception 'Run the OurChat migration in this NEW OurChat-only project first.'
      using errcode = '55000';
  end if;

  if not exists (select 1 from auth.users where id = first_auth_uuid)
    or not exists (select 1 from auth.users where id = second_auth_uuid) then
    raise exception 'Both UUIDs must already exist in Authentication > Users in this project.'
      using errcode = '22023';
  end if;

  -- Match the existing member-cap trigger's transaction lock. Do not disable it.
  perform pg_catalog.pg_advisory_xact_lock(17439021, 2);

  if exists (
    select 1 from public.members
    where user_id not in (first_auth_uuid, second_auth_uuid)
  ) then
    raise exception 'This project already has a different member. No members were changed.'
      using errcode = '23514';
  end if;

  -- Skip existing identities before INSERT: the max-two trigger runs before
  -- ON CONFLICT, so a blind upsert would fail when the pair already exists.
  insert into public.members (user_id, display_name)
  select candidate.user_id, candidate.display_name
  from (values
    (first_auth_uuid, 'Husband'),
    (second_auth_uuid, 'Wife')
  ) as candidate(user_id, display_name)
  where not exists (
    select 1 from public.members existing where existing.user_id = candidate.user_id
  );

  if (select count(*) from public.members) <> 2 then
    raise exception 'Expected exactly two members; all changes will be rolled back.'
      using errcode = '23514';
  end if;
end;
$ourchat_seed$;

commit;

-- Inspect the two identities locally. Never publish the result with your source.
select user_id, display_name from public.members order by member_slot;
