create or replace function private.upsert_steam_profile(
  p_steam_id text,
  p_auth_user_id uuid default null,
  p_nickname text default null,
  p_display_name text default null,
  p_avatar_url text default null,
  p_profile_url text default null,
  p_metadata jsonb default '{}'::jsonb,
  p_make_primary boolean default true
)
returns table (
  out_profile_id uuid,
  out_external_account_id uuid,
  out_is_new_profile boolean,
  out_is_new_external_account boolean
)
language plpgsql
security definer
set search_path = pg_catalog, public, private
as $$
declare
  normalized_steam_id text;
  target_profile_id uuid;
  target_account_id uuid;
  existing_auth_user_id uuid;
  created_profile boolean := false;
  created_account boolean := false;
  resolved_nickname text;
  has_primary_steam boolean;
begin
  normalized_steam_id = btrim(coalesce(p_steam_id, ''));

  if normalized_steam_id !~ '^[0-9]{17}$' then
    raise exception 'Steam ID must be a SteamID64 value.';
  end if;

  perform pg_catalog.pg_advisory_xact_lock(
    pg_catalog.hashtext('dotaops:steam'),
    pg_catalog.hashtext(normalized_steam_id)
  );

  select pea.id, pea.profile_id
  into target_account_id, target_profile_id
  from public.profile_external_accounts pea
  where pea.provider = 'steam'::public.dotaops_external_account_provider
    and pea.provider_account_id = normalized_steam_id
  for update;

  if target_profile_id is null and p_auth_user_id is not null then
    select p.id
    into target_profile_id
    from public.profiles p
    where p.auth_user_id = p_auth_user_id
    for update;
  end if;

  if target_profile_id is null then
    resolved_nickname = left(
      coalesce(
        nullif(btrim(p_nickname), ''),
        nullif(btrim(p_display_name), ''),
        'steam_' || right(normalized_steam_id, 8)
      ),
      40
    );

    insert into public.profiles (
      auth_user_id,
      nickname,
      display_name,
      steam_id,
      avatar_url
    )
    values (
      p_auth_user_id,
      resolved_nickname,
      nullif(btrim(p_display_name), ''),
      normalized_steam_id,
      nullif(btrim(p_avatar_url), '')
    )
    returning id into target_profile_id;

    created_profile = true;
  else
    select p.auth_user_id
    into existing_auth_user_id
    from public.profiles p
    where p.id = target_profile_id
    for update;

    if p_auth_user_id is not null and existing_auth_user_id is null then
      update public.profiles
      set auth_user_id = p_auth_user_id,
          updated_at = now()
      where id = target_profile_id;
    elsif p_auth_user_id is not null and existing_auth_user_id <> p_auth_user_id then
      raise exception 'Steam account is already linked to a different authenticated user.';
    end if;

    update public.profiles
    set display_name = coalesce(nullif(btrim(p_display_name), ''), display_name),
        avatar_url = coalesce(nullif(btrim(p_avatar_url), ''), avatar_url),
        updated_at = now()
    where id = target_profile_id;
  end if;

  select exists (
    select 1
    from public.profile_external_accounts pea
    where pea.profile_id = target_profile_id
      and pea.provider = 'steam'::public.dotaops_external_account_provider
      and pea.is_primary
  )
  into has_primary_steam;

  if target_account_id is null then
    insert into public.profile_external_accounts (
      profile_id,
      provider,
      provider_account_id,
      display_name,
      profile_url,
      avatar_url,
      is_primary,
      verified_at,
      metadata,
      last_login_at,
      last_synced_at,
      is_login_identity
    )
    values (
      target_profile_id,
      'steam'::public.dotaops_external_account_provider,
      normalized_steam_id,
      nullif(btrim(p_display_name), ''),
      nullif(btrim(p_profile_url), ''),
      nullif(btrim(p_avatar_url), ''),
      false,
      now(),
      coalesce(p_metadata, '{}'::jsonb),
      now(),
      now(),
      true
    )
    returning id into target_account_id;

    created_account = true;
  else
    update public.profile_external_accounts pea
    set display_name = coalesce(nullif(btrim(p_display_name), ''), pea.display_name),
        profile_url = coalesce(nullif(btrim(p_profile_url), ''), pea.profile_url),
        avatar_url = coalesce(nullif(btrim(p_avatar_url), ''), pea.avatar_url),
        verified_at = coalesce(pea.verified_at, now()),
        metadata = coalesce(pea.metadata, '{}'::jsonb) || coalesce(p_metadata, '{}'::jsonb),
        last_login_at = now(),
        last_synced_at = now(),
        is_login_identity = true,
        updated_at = now()
    where pea.id = target_account_id;
  end if;

  if p_make_primary or not has_primary_steam then
    perform private.set_primary_external_account(target_profile_id, target_account_id);
  end if;

  return query select target_profile_id, target_account_id, created_profile, created_account;
end;
$$;

revoke all on function private.upsert_steam_profile(text, uuid, text, text, text, text, jsonb, boolean) from public, anon, authenticated;
grant execute on function private.upsert_steam_profile(text, uuid, text, text, text, text, jsonb, boolean) to service_role;

comment on function private.upsert_steam_profile(text, uuid, text, text, text, text, jsonb, boolean) is
  'Backend/service helper that creates or links a DotaOps profile after Steam OpenID validation. The first linked Steam account is primary even when p_make_primary is false.';
