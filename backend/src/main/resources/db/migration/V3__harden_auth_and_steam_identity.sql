-- Harden public RLS policies and the external identity model before
-- Steam-first authentication work starts.

create or replace function public.set_updated_at()
returns trigger
language plpgsql
set search_path = pg_catalog
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

alter table public.profiles drop constraint if exists profiles_steam_id_format;
alter table public.profiles add constraint profiles_steam_id_format
  check (steam_id is null or steam_id ~ '^[0-9]{17}$');

alter table public.profile_external_accounts
  add column if not exists avatar_url text,
  add column if not exists last_login_at timestamptz,
  add column if not exists last_synced_at timestamptz,
  add column if not exists is_login_identity boolean not null default true;

alter table public.profile_external_accounts drop constraint if exists profile_external_accounts_steam_id64_format;
alter table public.profile_external_accounts add constraint profile_external_accounts_steam_id64_format
  check (provider <> 'steam'::public.dotaops_external_account_provider or provider_account_id ~ '^[0-9]{17}$');

insert into public.profile_external_accounts (
  profile_id,
  provider,
  provider_account_id,
  display_name,
  is_primary,
  verified_at,
  metadata
)
select
  p.id,
  'steam'::public.dotaops_external_account_provider,
  p.steam_id,
  p.display_name,
  not exists (
    select 1
    from public.profile_external_accounts pea
    where pea.profile_id = p.id
      and pea.provider = 'steam'::public.dotaops_external_account_provider
      and pea.is_primary
  ),
  now(),
  jsonb_build_object('source', 'legacy_profiles_steam_id')
from public.profiles p
where p.steam_id is not null
on conflict (provider, provider_account_id) do nothing;

create table if not exists private.steam_login_states (
  id uuid primary key default gen_random_uuid(),
  state_hash text not null unique,
  return_to text,
  profile_id uuid references public.profiles(id) on delete cascade,
  auth_user_id uuid references auth.users(id) on delete cascade,
  requested_ip inet,
  user_agent text,
  consumed_at timestamptz,
  expires_at timestamptz not null,
  created_at timestamptz not null default now(),
  constraint steam_login_states_expiry_after_create check (expires_at > created_at)
);

alter table private.steam_login_states enable row level security;
revoke all on table private.steam_login_states from public, anon, authenticated;
grant all on table private.steam_login_states to service_role;

create index if not exists steam_login_states_expires_idx
  on private.steam_login_states(expires_at)
  where consumed_at is null;

create index if not exists steam_login_states_profile_idx
  on private.steam_login_states(profile_id, created_at desc);

create or replace function private.sync_primary_steam_to_profile()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  replacement_account_id uuid;
  replacement_steam_id text;
  affected_profile_id uuid;
begin
  if coalesce(new.provider, old.provider) <> 'steam'::public.dotaops_external_account_provider then
    return coalesce(new, old);
  end if;

  affected_profile_id = coalesce(new.profile_id, old.profile_id);

  if tg_op in ('INSERT', 'UPDATE') and new.is_primary then
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
  elsif tg_op = 'DELETE' and old.is_primary then
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
      where id = replacement_account_id;

      update public.profiles
      set steam_id = replacement_steam_id,
          updated_at = now()
      where id = old.profile_id;
    end if;
  elsif tg_op = 'UPDATE' and old.profile_id is distinct from new.profile_id then
    update public.profiles
    set steam_id = (
          select pea.provider_account_id
          from public.profile_external_accounts pea
          where pea.profile_id = old.profile_id
            and pea.provider = 'steam'::public.dotaops_external_account_provider
            and pea.is_primary
          limit 1
        ),
        updated_at = now()
    where id = old.profile_id;
  end if;

  return coalesce(new, old);
end;
$$;

drop trigger if exists profile_external_accounts_sync_primary_steam on public.profile_external_accounts;
create trigger profile_external_accounts_sync_primary_steam
after insert or update of profile_id, provider, provider_account_id, is_primary or delete
on public.profile_external_accounts
for each row execute function private.sync_primary_steam_to_profile();

create or replace function private.set_primary_external_account(
  p_profile_id uuid,
  p_external_account_id uuid
)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  account_provider public.dotaops_external_account_provider;
  account_subject text;
begin
  select pea.provider, pea.provider_account_id
  into account_provider, account_subject
  from public.profile_external_accounts pea
  where pea.id = p_external_account_id
    and pea.profile_id = p_profile_id;

  if account_provider is null then
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
  where id = p_external_account_id;

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
set search_path = public
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

  select pea.id, pea.profile_id
  into target_account_id, target_profile_id
  from public.profile_external_accounts pea
  where pea.provider = 'steam'::public.dotaops_external_account_provider
    and pea.provider_account_id = normalized_steam_id
  limit 1;

  if target_profile_id is null and p_auth_user_id is not null then
    select p.id
    into target_profile_id
    from public.profiles p
    where p.auth_user_id = p_auth_user_id
    limit 1;
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

revoke all on function private.set_primary_external_account(uuid, uuid) from public, anon, authenticated;
revoke all on function private.upsert_steam_profile(text, uuid, text, text, text, text, jsonb, boolean) from public, anon, authenticated;
grant execute on function private.set_primary_external_account(uuid, uuid) to service_role;
grant execute on function private.upsert_steam_profile(text, uuid, text, text, text, text, jsonb, boolean) to service_role;

-- Users must not be able to claim or rewrite Steam identities from the client.
revoke insert, update on public.profiles from authenticated;
grant insert (auth_user_id, nickname, display_name, avatar_url, bio, country_code)
  on public.profiles to authenticated;
grant update (nickname, display_name, avatar_url, bio, country_code)
  on public.profiles to authenticated;

revoke insert, update, delete on public.profile_external_accounts from anon, authenticated;
revoke all on public.audit_log from anon;
revoke insert, update, delete on public.audit_log from authenticated;
revoke insert, update, delete on public.notification_outbox from anon, authenticated;

revoke select on public.team_invitations from anon, authenticated;
grant select (
  id,
  team_id,
  inviter_profile_id,
  invitee_profile_id,
  invitee_email,
  proposed_role,
  status,
  expires_at,
  accepted_at,
  created_at,
  updated_at
) on public.team_invitations to authenticated;

revoke insert, update, delete on all tables in schema public from anon;

create index if not exists match_games_radiant_team_id_idx
  on public.match_games(radiant_team_id);
create index if not exists match_games_dire_team_id_idx
  on public.match_games(dire_team_id);
create index if not exists match_games_winner_team_id_idx
  on public.match_games(winner_team_id);
create index if not exists match_import_events_created_by_idx
  on public.match_import_events(created_by);
create index if not exists match_imports_match_id_idx
  on public.match_imports(match_id);
create index if not exists match_imports_requested_by_idx
  on public.match_imports(requested_by);
create index if not exists match_slots_source_registration_idx
  on public.match_slots(source_registration_id);
create index if not exists matches_team_a_id_idx
  on public.matches(team_a_id);
create index if not exists matches_team_b_id_idx
  on public.matches(team_b_id);
create index if not exists matches_winner_team_id_idx
  on public.matches(winner_team_id);
create index if not exists team_invitations_inviter_profile_idx
  on public.team_invitations(inviter_profile_id);
create index if not exists team_invitations_invitee_profile_idx
  on public.team_invitations(invitee_profile_id);
create index if not exists teams_created_by_idx
  on public.teams(created_by);
create index if not exists tournament_group_teams_registration_idx
  on public.tournament_group_teams(registration_id);
create index if not exists tournament_registration_members_team_member_idx
  on public.tournament_registration_members(team_member_id);
create index if not exists tournament_registrations_captain_profile_idx
  on public.tournament_registrations(captain_profile_id);
create index if not exists tournament_registrations_reviewed_by_idx
  on public.tournament_registrations(reviewed_by);
create index if not exists tournaments_created_by_idx
  on public.tournaments(created_by);
create index if not exists tournaments_organizer_profile_idx
  on public.tournaments(organizer_profile_id);

drop policy if exists "organizers manage heroes" on public.heroes;
create policy "organizers insert heroes"
on public.heroes for insert
to authenticated
with check ((select private.is_organizer_or_admin()));
create policy "organizers update heroes"
on public.heroes for update
to authenticated
using ((select private.is_organizer_or_admin()))
with check ((select private.is_organizer_or_admin()));
create policy "organizers delete heroes"
on public.heroes for delete
to authenticated
using ((select private.is_organizer_or_admin()));

drop policy if exists "officials manage match games" on public.match_games;
create policy "officials insert match games"
on public.match_games for insert
to authenticated
with check (private.can_officiate_match(match_id));
create policy "officials update match games"
on public.match_games for update
to authenticated
using (private.can_officiate_match(match_id))
with check (private.can_officiate_match(match_id));
create policy "officials delete match games"
on public.match_games for delete
to authenticated
using (private.can_officiate_match(match_id));

drop policy if exists "officials manage match import events" on public.match_import_events;
create policy "officials insert match import events"
on public.match_import_events for insert
to authenticated
with check (
  exists (
    select 1
    from public.match_imports mi
    where mi.id = match_import_events.match_import_id
      and (
        (mi.match_id is not null and private.can_officiate_match(mi.match_id))
        or (mi.match_game_id is not null and private.can_officiate_match_game(mi.match_game_id))
      )
  )
);
create policy "officials update match import events"
on public.match_import_events for update
to authenticated
using (
  exists (
    select 1
    from public.match_imports mi
    where mi.id = match_import_events.match_import_id
      and (
        (mi.match_id is not null and private.can_officiate_match(mi.match_id))
        or (mi.match_game_id is not null and private.can_officiate_match_game(mi.match_game_id))
      )
  )
)
with check (
  exists (
    select 1
    from public.match_imports mi
    where mi.id = match_import_events.match_import_id
      and (
        (mi.match_id is not null and private.can_officiate_match(mi.match_id))
        or (mi.match_game_id is not null and private.can_officiate_match_game(mi.match_game_id))
      )
  )
);
create policy "officials delete match import events"
on public.match_import_events for delete
to authenticated
using (
  exists (
    select 1
    from public.match_imports mi
    where mi.id = match_import_events.match_import_id
      and (
        (mi.match_id is not null and private.can_officiate_match(mi.match_id))
        or (mi.match_game_id is not null and private.can_officiate_match_game(mi.match_game_id))
      )
  )
);

drop policy if exists "officials manage match players" on public.match_players;
create policy "officials insert match players"
on public.match_players for insert
to authenticated
with check (
  (match_game_id is not null and private.can_officiate_match_game(match_game_id))
  or (match_id is not null and private.can_officiate_match(match_id))
);
create policy "officials update match players"
on public.match_players for update
to authenticated
using (
  (match_game_id is not null and private.can_officiate_match_game(match_game_id))
  or (match_id is not null and private.can_officiate_match(match_id))
)
with check (
  (match_game_id is not null and private.can_officiate_match_game(match_game_id))
  or (match_id is not null and private.can_officiate_match(match_id))
);
create policy "officials delete match players"
on public.match_players for delete
to authenticated
using (
  (match_game_id is not null and private.can_officiate_match_game(match_game_id))
  or (match_id is not null and private.can_officiate_match(match_id))
);

drop policy if exists "officials manage match slots" on public.match_slots;
create policy "officials insert match slots"
on public.match_slots for insert
to authenticated
with check (private.can_officiate_match(match_id));
create policy "officials update match slots"
on public.match_slots for update
to authenticated
using (private.can_officiate_match(match_id))
with check (private.can_officiate_match(match_id));
create policy "officials delete match slots"
on public.match_slots for delete
to authenticated
using (private.can_officiate_match(match_id));

drop policy if exists "officials manage matches" on public.matches;
create policy "officials insert matches"
on public.matches for insert
to authenticated
with check (private.can_officiate_tournament(tournament_id));
create policy "officials update matches"
on public.matches for update
to authenticated
using (private.can_officiate_tournament(tournament_id))
with check (private.can_officiate_tournament(tournament_id));
create policy "officials delete matches"
on public.matches for delete
to authenticated
using (private.can_officiate_tournament(tournament_id));

drop policy if exists "users manage own external accounts" on public.profile_external_accounts;

drop policy if exists "captains manage team invitations" on public.team_invitations;
create policy "captains insert team invitations"
on public.team_invitations for insert
to authenticated
with check (
  private.is_team_captain(team_id)
  or (select private.is_organizer_or_admin())
);
create policy "captains update team invitations"
on public.team_invitations for update
to authenticated
using (
  private.is_team_captain(team_id)
  or (select private.is_organizer_or_admin())
)
with check (
  private.is_team_captain(team_id)
  or (select private.is_organizer_or_admin())
);
create policy "captains delete team invitations"
on public.team_invitations for delete
to authenticated
using (
  private.is_team_captain(team_id)
  or (select private.is_organizer_or_admin())
);

drop policy if exists "captains manage team members" on public.team_members;
create policy "captains insert team members"
on public.team_members for insert
to authenticated
with check (
  private.is_team_captain(team_id)
  or (select private.is_organizer_or_admin())
);
create policy "captains update team members"
on public.team_members for update
to authenticated
using (
  private.is_team_captain(team_id)
  or (select private.is_organizer_or_admin())
)
with check (
  private.is_team_captain(team_id)
  or (select private.is_organizer_or_admin())
);
create policy "captains delete team members"
on public.team_members for delete
to authenticated
using (
  private.is_team_captain(team_id)
  or (select private.is_organizer_or_admin())
);

drop policy if exists "tournament organizers manage group teams" on public.tournament_group_teams;
create policy "tournament organizers insert group teams"
on public.tournament_group_teams for insert
to authenticated
with check (
  exists (
    select 1
    from public.tournament_groups tg
    where tg.id = tournament_group_teams.group_id
      and private.can_manage_tournament(tg.tournament_id)
  )
);
create policy "tournament organizers update group teams"
on public.tournament_group_teams for update
to authenticated
using (
  exists (
    select 1
    from public.tournament_groups tg
    where tg.id = tournament_group_teams.group_id
      and private.can_manage_tournament(tg.tournament_id)
  )
)
with check (
  exists (
    select 1
    from public.tournament_groups tg
    where tg.id = tournament_group_teams.group_id
      and private.can_manage_tournament(tg.tournament_id)
  )
);
create policy "tournament organizers delete group teams"
on public.tournament_group_teams for delete
to authenticated
using (
  exists (
    select 1
    from public.tournament_groups tg
    where tg.id = tournament_group_teams.group_id
      and private.can_manage_tournament(tg.tournament_id)
  )
);

drop policy if exists "tournament organizers manage groups" on public.tournament_groups;
create policy "tournament organizers insert groups"
on public.tournament_groups for insert
to authenticated
with check (private.can_manage_tournament(tournament_id));
create policy "tournament organizers update groups"
on public.tournament_groups for update
to authenticated
using (private.can_manage_tournament(tournament_id))
with check (private.can_manage_tournament(tournament_id));
create policy "tournament organizers delete groups"
on public.tournament_groups for delete
to authenticated
using (private.can_manage_tournament(tournament_id));

drop policy if exists "captains manage registration members" on public.tournament_registration_members;
create policy "captains insert registration members"
on public.tournament_registration_members for insert
to authenticated
with check (
  exists (
    select 1
    from public.tournament_registrations tr
    where tr.id = tournament_registration_members.registration_id
      and (
        private.is_team_captain(tr.team_id)
        or private.can_manage_tournament(tr.tournament_id)
      )
  )
);
create policy "captains update registration members"
on public.tournament_registration_members for update
to authenticated
using (
  exists (
    select 1
    from public.tournament_registrations tr
    where tr.id = tournament_registration_members.registration_id
      and (
        private.is_team_captain(tr.team_id)
        or private.can_manage_tournament(tr.tournament_id)
      )
  )
)
with check (
  exists (
    select 1
    from public.tournament_registrations tr
    where tr.id = tournament_registration_members.registration_id
      and (
        private.is_team_captain(tr.team_id)
        or private.can_manage_tournament(tr.tournament_id)
      )
  )
);
create policy "captains delete registration members"
on public.tournament_registration_members for delete
to authenticated
using (
  exists (
    select 1
    from public.tournament_registrations tr
    where tr.id = tournament_registration_members.registration_id
      and (
        private.is_team_captain(tr.team_id)
        or private.can_manage_tournament(tr.tournament_id)
      )
  )
);

drop policy if exists "tournament owners manage staff" on public.tournament_staff;
create policy "tournament owners insert staff"
on public.tournament_staff for insert
to authenticated
with check (private.can_manage_tournament(tournament_id));
create policy "tournament owners update staff"
on public.tournament_staff for update
to authenticated
using (private.can_manage_tournament(tournament_id))
with check (private.can_manage_tournament(tournament_id));
create policy "tournament owners delete staff"
on public.tournament_staff for delete
to authenticated
using (private.can_manage_tournament(tournament_id));

comment on column public.profiles.steam_id is 'Legacy/public primary SteamID64 mirror. Authoritative Steam links are rows in profile_external_accounts where provider = steam.';
comment on table private.steam_login_states is 'Short-lived backend-only Steam OpenID state storage. Store hashes only, never raw session tokens.';
comment on function private.upsert_steam_profile(text, uuid, text, text, text, text, jsonb, boolean) is 'Backend/service helper that creates or links a DotaOps profile after Steam OpenID validation.';
