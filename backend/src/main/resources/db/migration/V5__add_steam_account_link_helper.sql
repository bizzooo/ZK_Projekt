create or replace function private.link_steam_account_to_profile(
  p_profile_id uuid,
  p_steam_id text,
  p_display_name text default null,
  p_avatar_url text default null,
  p_profile_url text default null,
  p_metadata jsonb default '{}'::jsonb,
  p_make_primary boolean default false
)
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
  normalized_steam_id text;
  target_account_id uuid;
  existing_profile_id uuid;
  has_primary_steam boolean;
begin
  normalized_steam_id = btrim(coalesce(p_steam_id, ''));

  if normalized_steam_id !~ '^[0-9]{17}$' then
    raise exception 'Steam ID must be a SteamID64 value.';
  end if;

  perform 1
  from public.profiles p
  where p.id = p_profile_id;

  if not found then
    raise exception 'Profile does not exist.';
  end if;

  select pea.id, pea.profile_id
  into target_account_id, existing_profile_id
  from public.profile_external_accounts pea
  where pea.provider = 'steam'::public.dotaops_external_account_provider
    and pea.provider_account_id = normalized_steam_id
  for update;

  if target_account_id is not null and existing_profile_id <> p_profile_id then
    raise exception 'Steam account is already linked to a different profile.';
  end if;

  select exists (
    select 1
    from public.profile_external_accounts pea
    where pea.profile_id = p_profile_id
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
      p_profile_id,
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
    perform private.set_primary_external_account(p_profile_id, target_account_id);
  end if;

  return target_account_id;
end;
$$;

revoke all on function private.link_steam_account_to_profile(uuid, text, text, text, text, jsonb, boolean) from public, anon, authenticated;
grant execute on function private.link_steam_account_to_profile(uuid, text, text, text, text, jsonb, boolean) to service_role;

comment on function private.link_steam_account_to_profile(uuid, text, text, text, text, jsonb, boolean) is
  'Backend/service helper for linking additional SteamID64 accounts to an existing DotaOps profile.';
