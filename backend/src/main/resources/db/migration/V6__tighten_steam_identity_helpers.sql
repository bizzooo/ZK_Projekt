-- Keep Steam identity changes backend-only and deterministic.
-- V3-V5 may already be applied in shared environments, so fixes go forward here.

create or replace function private.sync_primary_steam_to_profile()
returns trigger
language plpgsql
security definer
set search_path = pg_catalog, public, private
as $$
declare
  replacement_account_id uuid;
  replacement_steam_id text;
begin
  if tg_op = 'DELETE' then
    if old.provider <> 'steam'::public.dotaops_external_account_provider then
      return old;
    end if;

    if old.is_primary then
      select pea.id, pea.provider_account_id
      into replacement_account_id, replacement_steam_id
      from public.profile_external_accounts pea
      where pea.profile_id = old.profile_id
        and pea.provider = 'steam'::public.dotaops_external_account_provider
      order by pea.verified_at desc nulls last, pea.created_at desc
      limit 1;

      if replacement_account_id is null then
        update public.profiles
        set steam_id = null,
            updated_at = now()
        where id = old.profile_id;
      else
        update public.profile_external_accounts
        set is_primary = true,
            updated_at = now()
        where id = replacement_account_id
          and is_primary is distinct from true;

        update public.profiles
        set steam_id = replacement_steam_id,
            updated_at = now()
        where id = old.profile_id;
      end if;
    end if;

    return old;
  end if;

  if tg_op = 'UPDATE' then
    if old.provider = 'steam'::public.dotaops_external_account_provider
       and old.is_primary
       and (
         new.provider <> 'steam'::public.dotaops_external_account_provider
         or new.profile_id is distinct from old.profile_id
         or new.is_primary is distinct from true
       ) then
      select pea.id, pea.provider_account_id
      into replacement_account_id, replacement_steam_id
      from public.profile_external_accounts pea
      where pea.profile_id = old.profile_id
        and pea.provider = 'steam'::public.dotaops_external_account_provider
        and pea.id <> new.id
      order by pea.is_primary desc, pea.verified_at desc nulls last, pea.created_at desc
      limit 1;

      if replacement_account_id is null then
        update public.profiles
        set steam_id = null,
            updated_at = now()
        where id = old.profile_id;
      else
        update public.profile_external_accounts
        set is_primary = true,
            updated_at = now()
        where id = replacement_account_id
          and is_primary is distinct from true;

        update public.profiles
        set steam_id = replacement_steam_id,
            updated_at = now()
        where id = old.profile_id;
      end if;
    end if;
  end if;

  if new.provider <> 'steam'::public.dotaops_external_account_provider then
    return new;
  end if;

  if new.is_primary then
    update public.profile_external_accounts
    set is_primary = false,
        updated_at = now()
    where profile_id = new.profile_id
      and provider = 'steam'::public.dotaops_external_account_provider
      and id <> new.id
      and is_primary;

    update public.profiles
    set steam_id = new.provider_account_id,
        updated_at = now()
    where id = new.profile_id;
  end if;

  return new;
end;
$$;

create or replace function private.set_primary_external_account(
  p_profile_id uuid,
  p_external_account_id uuid
)
returns void
language plpgsql
security definer
set search_path = pg_catalog, public, private
as $$
declare
  account_provider public.dotaops_external_account_provider;
  account_subject text;
begin
  if p_profile_id is null or p_external_account_id is null then
    raise exception 'Profile and external account are required.';
  end if;

  perform 1
  from public.profiles p
  where p.id = p_profile_id
  for update;

  if not found then
    raise exception 'Profile does not exist.';
  end if;

  select pea.provider, pea.provider_account_id
  into account_provider, account_subject
  from public.profile_external_accounts pea
  where pea.id = p_external_account_id
    and pea.profile_id = p_profile_id
  for update;

  if not found then
    raise exception 'External account does not belong to the requested profile.';
  end if;

  update public.profile_external_accounts
  set is_primary = false,
      updated_at = now()
  where profile_id = p_profile_id
    and provider = account_provider
    and id <> p_external_account_id
    and is_primary;

  update public.profile_external_accounts
  set is_primary = true,
      updated_at = now()
  where id = p_external_account_id
    and is_primary is distinct from true;

  if account_provider = 'steam'::public.dotaops_external_account_provider then
    update public.profiles
    set steam_id = account_subject,
        updated_at = now()
    where id = p_profile_id;
  end if;
end;
$$;

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

  if p_make_primary then
    perform private.set_primary_external_account(target_profile_id, target_account_id);
  end if;

  return query select target_profile_id, target_account_id, created_profile, created_account;
end;
$$;

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
set search_path = pg_catalog, public, private
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

  perform pg_catalog.pg_advisory_xact_lock(
    pg_catalog.hashtext('dotaops:steam'),
    pg_catalog.hashtext(normalized_steam_id)
  );

  perform 1
  from public.profiles p
  where p.id = p_profile_id
  for update;

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

create or replace function private.unlink_steam_account_from_profile(
  p_profile_id uuid,
  p_external_account_id uuid,
  p_allow_unlinked_profile boolean default false
)
returns void
language plpgsql
security definer
set search_path = pg_catalog, public, private
as $$
declare
  account_provider public.dotaops_external_account_provider;
  steam_account_count integer;
begin
  if p_profile_id is null or p_external_account_id is null then
    raise exception 'Profile and external account are required.';
  end if;

  perform 1
  from public.profiles p
  where p.id = p_profile_id
  for update;

  if not found then
    raise exception 'Profile does not exist.';
  end if;

  select pea.provider
  into account_provider
  from public.profile_external_accounts pea
  where pea.id = p_external_account_id
    and pea.profile_id = p_profile_id
  for update;

  if not found then
    raise exception 'External account does not belong to the requested profile.';
  end if;

  if account_provider <> 'steam'::public.dotaops_external_account_provider then
    raise exception 'Only Steam accounts can be unlinked with this helper.';
  end if;

  select count(*)::integer
  into steam_account_count
  from public.profile_external_accounts pea
  where pea.profile_id = p_profile_id
    and pea.provider = 'steam'::public.dotaops_external_account_provider;

  if steam_account_count <= 1 and not p_allow_unlinked_profile then
    raise exception 'Cannot unlink the last Steam account from the profile.';
  end if;

  delete from public.profile_external_accounts
  where id = p_external_account_id
    and profile_id = p_profile_id
    and provider = 'steam'::public.dotaops_external_account_provider;
end;
$$;

create or replace function private.create_steam_login_state(
  p_state_hash text,
  p_return_to text default null,
  p_profile_id uuid default null,
  p_auth_user_id uuid default null,
  p_requested_ip inet default null,
  p_user_agent text default null,
  p_expires_at timestamptz default null
)
returns uuid
language plpgsql
security definer
set search_path = pg_catalog, public, private
as $$
declare
  normalized_state_hash text;
  resolved_expires_at timestamptz;
  created_state_id uuid;
begin
  normalized_state_hash = btrim(coalesce(p_state_hash, ''));
  resolved_expires_at = coalesce(p_expires_at, now() + interval '10 minutes');

  if char_length(normalized_state_hash) < 32 or char_length(normalized_state_hash) > 256 then
    raise exception 'Steam login state hash must be between 32 and 256 characters.';
  end if;

  if resolved_expires_at <= now() or resolved_expires_at > now() + interval '30 minutes' then
    raise exception 'Steam login state expiry must be within the next 30 minutes.';
  end if;

  insert into private.steam_login_states (
    state_hash,
    return_to,
    profile_id,
    auth_user_id,
    requested_ip,
    user_agent,
    expires_at
  )
  values (
    normalized_state_hash,
    nullif(btrim(p_return_to), ''),
    p_profile_id,
    p_auth_user_id,
    p_requested_ip,
    nullif(btrim(p_user_agent), ''),
    resolved_expires_at
  )
  returning id into created_state_id;

  return created_state_id;
end;
$$;

create or replace function private.consume_steam_login_state(
  p_state_hash text
)
returns table (
  out_id uuid,
  out_return_to text,
  out_profile_id uuid,
  out_auth_user_id uuid,
  out_requested_ip inet,
  out_user_agent text
)
language plpgsql
security definer
set search_path = pg_catalog, public, private
as $$
declare
  normalized_state_hash text;
begin
  normalized_state_hash = btrim(coalesce(p_state_hash, ''));

  return query
  update private.steam_login_states sls
  set consumed_at = now()
  where sls.state_hash = normalized_state_hash
    and sls.consumed_at is null
    and sls.expires_at > now()
  returning
    sls.id,
    sls.return_to,
    sls.profile_id,
    sls.auth_user_id,
    sls.requested_ip,
    sls.user_agent;
end;
$$;

revoke all on function private.set_primary_external_account(uuid, uuid) from public, anon, authenticated;
revoke all on function private.upsert_steam_profile(text, uuid, text, text, text, text, jsonb, boolean) from public, anon, authenticated;
revoke all on function private.link_steam_account_to_profile(uuid, text, text, text, text, jsonb, boolean) from public, anon, authenticated;
revoke all on function private.unlink_steam_account_from_profile(uuid, uuid, boolean) from public, anon, authenticated;
revoke all on function private.create_steam_login_state(text, text, uuid, uuid, inet, text, timestamptz) from public, anon, authenticated;
revoke all on function private.consume_steam_login_state(text) from public, anon, authenticated;

grant execute on function private.set_primary_external_account(uuid, uuid) to service_role;
grant execute on function private.upsert_steam_profile(text, uuid, text, text, text, text, jsonb, boolean) to service_role;
grant execute on function private.link_steam_account_to_profile(uuid, text, text, text, text, jsonb, boolean) to service_role;
grant execute on function private.unlink_steam_account_from_profile(uuid, uuid, boolean) to service_role;
grant execute on function private.create_steam_login_state(text, text, uuid, uuid, inet, text, timestamptz) to service_role;
grant execute on function private.consume_steam_login_state(text) to service_role;

comment on function private.upsert_steam_profile(text, uuid, text, text, text, text, jsonb, boolean) is
  'Backend/service helper that creates or links a DotaOps profile after Steam OpenID validation.';
comment on function private.link_steam_account_to_profile(uuid, text, text, text, text, jsonb, boolean) is
  'Backend/service helper for linking additional SteamID64 accounts to an existing DotaOps profile.';
comment on function private.unlink_steam_account_from_profile(uuid, uuid, boolean) is
  'Backend/service helper for unlinking Steam accounts while protecting profiles from losing their last Steam login identity by default.';
comment on function private.create_steam_login_state(text, text, uuid, uuid, inet, text, timestamptz) is
  'Creates short-lived hashed Steam OpenID state for backend login flows.';
comment on function private.consume_steam_login_state(text) is
  'Atomically consumes a non-expired Steam OpenID state hash and returns its login context.';
