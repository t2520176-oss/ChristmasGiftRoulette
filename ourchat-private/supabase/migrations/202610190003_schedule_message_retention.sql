-- Supabase scheduling, intentionally separate from the portable retention core.
-- Requires pg_cron support; never substitutes browser traffic for scheduled cleanup.
begin;
set local lock_timeout = '5s';
set local search_path = pg_catalog;

do $schedule$
declare
  v_job record;
  v_name constant text := 'ourchat-prune-messages-7d';
  v_command constant text := 'SELECT private.prune_expired_messages();';
begin
  if not exists (
    select 1 from pg_catalog.pg_proc where oid = pg_catalog.to_regprocedure('private.prune_expired_messages()')
      and proowner = (select oid from pg_catalog.pg_roles where rolname = current_user)
      and prosecdef and pronargs = 0 and prorettype = 'int8'::regtype
      and proconfig = array['search_path=""', 'lock_timeout=5s']::text[]
      and pg_catalog.md5(prosrc) = '8e255647d26cc66ae0d8d5f09274db23'
      and not exists (select 1 from pg_catalog.aclexplode(coalesce(proacl,
        pg_catalog.acldefault('f', proowner))) where grantee <> proowner)
  ) then
    raise exception 'Refusing retention schedule: install the owner-only retention core first' using errcode = '55000';
  end if;
  perform pg_catalog.pg_advisory_xact_lock(17439021, 4);
  if not exists (select 1 from pg_catalog.pg_extension where extname = 'pg_cron') then
    if not exists (select 1 from pg_catalog.pg_available_extensions where name = 'pg_cron') then
      raise exception 'pg_cron is unavailable; automatic message deletion is not configured' using errcode = '55000';
    end if;
    create extension pg_cron with schema pg_catalog;
  elsif not exists (
    select 1 from pg_catalog.pg_extension where extname = 'pg_cron'
      and extnamespace = 'pg_catalog'::regnamespace
  ) then
    raise exception 'Refusing retention schedule: unexpected pg_cron extension schema' using errcode = '55000';
  end if;

  select * into v_job from cron.job where jobname = v_name;
  if not found then
    perform cron.schedule(v_name, '* * * * *', v_command);
    select * into v_job from cron.job where jobname = v_name;
  end if;
  if not found or (select pg_catalog.count(*) from cron.job where jobname = v_name) <> 1
    or v_job.username <> current_user or v_job.database <> pg_catalog.current_database()
    or v_job.command <> v_command or v_job.schedule <> '* * * * *'
    or v_job.active is distinct from true
    or v_job.nodename <> 'localhost' or v_job.nodeport <> pg_catalog.current_setting('port')::integer then
    raise exception 'Refusing retention schedule: existing job name has different settings or ownership' using errcode = '55000';
  end if;
end;
$schedule$;

commit;
