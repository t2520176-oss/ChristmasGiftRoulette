-- Read-only preflight for the browser setup helper. Run inside the setup transaction.
-- This reduces wrong-project mistakes; still verify the NEW project in Dashboard.
do $ourchat_new_project$
begin
  if exists (
    select 1 from pg_catalog.pg_class c
    join pg_catalog.pg_namespace n on n.oid = c.relnamespace
    where n.nspname = 'public' and c.relkind in ('r', 'p', 'v', 'm', 'f', 'S')
      and not exists (
        select 1 from pg_catalog.pg_depend d
        where d.classid = 'pg_catalog.pg_class'::regclass and d.objid = c.oid
          and d.deptype = 'e'
      )
  ) or exists (
    select 1 from pg_catalog.pg_proc p
    join pg_catalog.pg_namespace n on n.oid = p.pronamespace
    where n.nspname = 'public' and not exists (
      select 1 from pg_catalog.pg_depend d
      where d.classid = 'pg_catalog.pg_proc'::regclass and d.objid = p.oid
        and d.deptype = 'e'
    )
  ) or exists (
    select 1 from pg_catalog.pg_namespace where nspname = 'private'
  ) or (select count(*) from auth.users) > 2 then
    raise exception 'Stopped: this is not an empty OurChat project. No setup changes were made. Create a NEW project; do not delete existing work.'
      using errcode = '55000';
  end if;
end;
$ourchat_new_project$;
