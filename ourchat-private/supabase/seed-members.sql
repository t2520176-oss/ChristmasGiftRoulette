-- ADMIN ONLY. Create both email/password users in Supabase Auth first.
-- Disable public signups and anonymous sign-in BEFORE inviting users.
-- Supply their real Auth UUIDs as psql variables; no identities are invented here.
--
-- psql "$DATABASE_URL" \
--   -v husband_user_id='COPY_AUTH_UUID_HERE' \
--   -v wife_user_id='COPY_OTHER_AUTH_UUID_HERE' \
--   -f supabase/seed-members.sql
--
-- If using Dashboard SQL Editor, replace the two :'..._user_id' expressions with
-- your actual quoted UUIDs. Keep this file free of real account information.
-- Change Husband/Wife below if desired. Member slots are assigned by the trigger.
begin;
insert into public.members (user_id, display_name)
values (:'husband_user_id'::uuid, 'Husband'), (:'wife_user_id'::uuid, 'Wife');
commit;

-- Verify exactly your two previously created users, never add a third member.
select user_id, display_name from public.members order by member_slot;
