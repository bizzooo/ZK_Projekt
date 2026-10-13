update public.profiles
set role = 'player'::public.dotaops_user_role,
    updated_at = now()
where role = 'captain'::public.dotaops_user_role;

alter table public.profiles drop constraint if exists profiles_role_no_global_captain;
alter table public.profiles add constraint profiles_role_no_global_captain
  check (role <> 'captain'::public.dotaops_user_role);

create or replace function private.self_selected_profile_role(
  p_user_metadata jsonb,
  p_app_metadata jsonb
)
returns public.dotaops_user_role
language plpgsql
security definer
set search_path = ''
as $$
declare
  requested_role text;
begin
  requested_role = lower(btrim(coalesce(
    p_app_metadata->>'desired_role',
    p_app_metadata->>'account_type',
    p_user_metadata->>'desired_role',
    p_user_metadata->>'account_type',
    p_user_metadata->>'role',
    'player'
  )));

  if requested_role = 'organizer' then
    return 'organizer'::public.dotaops_user_role;
  end if;

  return 'player'::public.dotaops_user_role;
end;
$$;

create or replace function private.ensure_profile_for_auth_user(
  p_auth_user_id uuid,
  p_email text,
  p_user_metadata jsonb default '{}'::jsonb,
  p_app_metadata jsonb default '{}'::jsonb
)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
  existing_profile_id uuid;
  suffix text;
  base_nickname text;
  candidate_nickname text;
  attempt integer := 0;
  resolved_display_name text;
  resolved_avatar_url text;
  resolved_bio text;
  resolved_country_code text;
begin
  if p_auth_user_id is null then
    raise exception 'Auth user id is required to create a profile.';
  end if;

  select p.id
  into existing_profile_id
  from public.profiles p
  where p.auth_user_id = p_auth_user_id
  limit 1;

  if existing_profile_id is not null then
    return existing_profile_id;
  end if;

  suffix = left(replace(p_auth_user_id::text, '-', ''), 8);
  base_nickname = btrim(coalesce(
    nullif(p_user_metadata->>'nickname', ''),
    nullif(split_part(coalesce(p_email, ''), '@', 1), ''),
    'player_' || suffix
  ));
  base_nickname = regexp_replace(base_nickname, '\s+', '_', 'g');
  base_nickname = left(base_nickname, 40);

  if char_length(base_nickname) < 2 then
    base_nickname = 'player_' || suffix;
  end if;

  candidate_nickname = base_nickname;

  while exists (
    select 1
    from public.profiles p
    where lower(p.nickname) = lower(candidate_nickname)
  ) loop
    attempt = attempt + 1;
    candidate_nickname = left(base_nickname, 40 - char_length(suffix) - char_length(attempt::text) - 2)
      || '_' || suffix || '_' || attempt::text;

    if attempt > 20 then
      raise exception 'Could not generate a unique profile nickname.';
    end if;
  end loop;

  resolved_display_name = nullif(left(btrim(coalesce(
    p_user_metadata->>'display_name',
    p_user_metadata->>'full_name',
    candidate_nickname
  )), 80), '');
  resolved_avatar_url = nullif(left(btrim(coalesce(p_user_metadata->>'avatar_url', '')), 512), '');
  resolved_bio = nullif(left(btrim(coalesce(p_user_metadata->>'bio', '')), 500), '');
  resolved_country_code = upper(btrim(coalesce(p_user_metadata->>'country_code', '')));

  if resolved_country_code !~ '^[A-Z]{2}$' then
    resolved_country_code = null;
  end if;

  insert into public.profiles (
    auth_user_id,
    nickname,
    display_name,
    role,
    avatar_url,
    bio,
    country_code
  )
  values (
    p_auth_user_id,
    candidate_nickname,
    resolved_display_name,
    private.self_selected_profile_role(
      coalesce(p_user_metadata, '{}'::jsonb),
      coalesce(p_app_metadata, '{}'::jsonb)
    ),
    resolved_avatar_url,
    resolved_bio,
    resolved_country_code
  )
  on conflict (auth_user_id) do nothing
  returning id into existing_profile_id;

  if existing_profile_id is null then
    select p.id
    into existing_profile_id
    from public.profiles p
    where p.auth_user_id = p_auth_user_id
    limit 1;
  end if;

  return existing_profile_id;
end;
$$;

create or replace function private.create_profile_for_new_auth_user()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
  auth_user jsonb;
begin
  auth_user = to_jsonb(new);

  perform private.ensure_profile_for_auth_user(
    new.id,
    auth_user->>'email',
    coalesce(auth_user->'raw_user_meta_data', '{}'::jsonb),
    coalesce(auth_user->'raw_app_meta_data', '{}'::jsonb)
  );

  return new;
end;
$$;

drop trigger if exists dotaops_create_profile_on_auth_user on auth.users;
create trigger dotaops_create_profile_on_auth_user
after insert on auth.users
for each row execute function private.create_profile_for_new_auth_user();

select private.ensure_profile_for_auth_user(
  u.id,
  to_jsonb(u)->>'email',
  coalesce(to_jsonb(u)->'raw_user_meta_data', '{}'::jsonb),
  coalesce(to_jsonb(u)->'raw_app_meta_data', '{}'::jsonb)
)
from auth.users u
where not exists (
  select 1
  from public.profiles p
  where p.auth_user_id = u.id
);

revoke all on function private.self_selected_profile_role(jsonb, jsonb) from public, anon, authenticated;
revoke all on function private.ensure_profile_for_auth_user(uuid, text, jsonb, jsonb) from public, anon, authenticated;
revoke all on function private.create_profile_for_new_auth_user() from public, anon, authenticated;
grant execute on function private.self_selected_profile_role(jsonb, jsonb) to service_role;
grant execute on function private.ensure_profile_for_auth_user(uuid, text, jsonb, jsonb) to service_role;

comment on constraint profiles_role_no_global_captain on public.profiles is
  'Team captain is a team-specific capability from teams.captain_profile_id/team membership, not a global account role.';
