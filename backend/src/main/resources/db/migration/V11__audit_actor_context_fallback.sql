create or replace function private.write_audit_log()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  audit_record_id uuid;
  audit_actor_auth_user_id uuid;
  audit_actor_profile_id uuid;
begin
  audit_record_id = nullif(coalesce(to_jsonb(new)->>'id', to_jsonb(old)->>'id'), '')::uuid;
  audit_actor_auth_user_id = nullif(coalesce(current_setting('request.dotaops.auth_user_id', true), ''), '')::uuid;

  if audit_actor_auth_user_id is null then
    audit_actor_auth_user_id = (select auth.uid());
  end if;

  audit_actor_profile_id = nullif(coalesce(current_setting('request.dotaops.profile_id', true), ''), '')::uuid;

  if audit_actor_profile_id is null then
    audit_actor_profile_id = private.current_profile_id();
  end if;

  insert into public.audit_log (
    actor_auth_user_id,
    actor_profile_id,
    table_name,
    record_id,
    action,
    previous_row,
    new_row
  )
  values (
    audit_actor_auth_user_id,
    audit_actor_profile_id,
    tg_table_schema || '.' || tg_table_name,
    audit_record_id,
    lower(tg_op)::public.dotaops_audit_action,
    case when tg_op in ('UPDATE', 'DELETE') then to_jsonb(old) else null end,
    case when tg_op in ('INSERT', 'UPDATE') then to_jsonb(new) else null end
  );

  return coalesce(new, old);
end;
$$;
