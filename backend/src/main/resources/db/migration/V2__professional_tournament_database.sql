create schema if not exists private;

revoke all on schema private from public;
revoke all on schema private from anon;
revoke all on schema private from authenticated;
grant usage on schema private to service_role;

do $$
begin
  create type public.dotaops_external_account_provider as enum (
    'steam',
    'opendota',
    'discord',
    'email'
  );
exception
  when duplicate_object then null;
end;
$$;

do $$
begin
  create type public.dotaops_tournament_staff_role as enum (
    'owner',
    'organizer',
    'referee',
    'analyst'
  );
exception
  when duplicate_object then null;
end;
$$;

do $$
begin
  create type public.dotaops_match_slot as enum (
    'team_a',
    'team_b'
  );
exception
  when duplicate_object then null;
end;
$$;

do $$
begin
  create type public.dotaops_match_slot_source as enum (
    'manual',
    'seed',
    'winner',
    'loser',
    'bye'
  );
exception
  when duplicate_object then null;
end;
$$;

do $$
begin
  create type public.dotaops_game_side as enum (
    'radiant',
    'dire'
  );
exception
  when duplicate_object then null;
end;
$$;

do $$
begin
  create type public.dotaops_invitation_status as enum (
    'pending',
    'accepted',
    'declined',
    'cancelled',
    'expired'
  );
exception
  when duplicate_object then null;
end;
$$;

do $$
begin
  create type public.dotaops_notification_channel as enum (
    'in_app',
    'email',
    'discord'
  );
exception
  when duplicate_object then null;
end;
$$;

do $$
begin
  create type public.dotaops_delivery_status as enum (
    'queued',
    'sent',
    'failed',
    'cancelled'
  );
exception
  when duplicate_object then null;
end;
$$;

do $$
begin
  create type public.dotaops_audit_action as enum (
    'insert',
    'update',
    'delete'
  );
exception
  when duplicate_object then null;
end;
$$;

alter table public.team_members
  add column if not exists updated_at timestamptz not null default now();

alter table public.tournaments
  add column if not exists timezone text not null default 'UTC',
  add column if not exists check_in_opens_at timestamptz,
  add column if not exists check_in_closes_at timestamptz,
  add column if not exists published_at timestamptz,
  add column if not exists settings jsonb not null default '{}'::jsonb;

alter table public.tournament_registrations
  add column if not exists seed_number integer,
  add column if not exists checked_in_at timestamptz,
  add column if not exists contact_email text,
  add column if not exists metadata jsonb not null default '{}'::jsonb;

alter table public.matches
  add column if not exists stage_name text not null default 'main',
  add column if not exists series_number integer,
  add column if not exists settings jsonb not null default '{}'::jsonb,
  add column if not exists notes text;

alter table public.heroes
  add column if not exists slug text,
  add column if not exists image_url text,
  add column if not exists icon_url text;

alter table public.match_imports
  add column if not exists source text not null default 'opendota',
  add column if not exists attempt_count integer not null default 0,
  add column if not exists requested_at timestamptz not null default now(),
  add column if not exists locked_at timestamptz,
  add column if not exists metadata jsonb not null default '{}'::jsonb;

alter table public.match_players
  add column if not exists lane_role text,
  add column if not exists party_id integer,
  add column if not exists item_ids integer[] not null default '{}',
  add column if not exists benchmarks jsonb not null default '{}'::jsonb;

alter table public.team_members drop constraint if exists team_members_team_id_profile_id_key;

create unique index if not exists team_members_one_active_profile_per_team_idx
  on public.team_members(team_id, profile_id)
  where is_active;

create index if not exists team_members_team_active_idx
  on public.team_members(team_id, is_active);

alter table public.tournaments drop constraint if exists tournaments_check_in_order;
alter table public.tournaments add constraint tournaments_check_in_order
  check (
    check_in_opens_at is null
    or check_in_closes_at is null
    or check_in_closes_at >= check_in_opens_at
  );

alter table public.tournaments drop constraint if exists tournaments_registration_before_start;
alter table public.tournaments add constraint tournaments_registration_before_start
  check (
    registration_closes_at is null
    or registration_closes_at <= starts_at
  );

alter table public.matches drop constraint if exists matches_best_of_odd;
alter table public.matches add constraint matches_best_of_odd
  check (best_of between 1 and 9 and best_of % 2 = 1);

alter table public.matches drop constraint if exists matches_scores_fit_series;
alter table public.matches add constraint matches_scores_fit_series
  check (
    score_a <= ((best_of + 1) / 2)
    and score_b <= ((best_of + 1) / 2)
  );

alter table public.matches drop constraint if exists matches_time_order;
alter table public.matches add constraint matches_time_order
  check (
    (started_at is null or scheduled_at is null or started_at >= scheduled_at)
    and (finished_at is null or started_at is null or finished_at >= started_at)
  );

alter table public.tournament_registrations drop constraint if exists tournament_registrations_seed_positive;
alter table public.tournament_registrations add constraint tournament_registrations_seed_positive
  check (seed_number is null or seed_number > 0);

alter table public.heroes drop constraint if exists heroes_slug_format;
alter table public.heroes add constraint heroes_slug_format
  check (slug is null or slug ~ '^[a-z0-9]+(?:-[a-z0-9]+)*$');

alter table public.match_imports drop constraint if exists match_imports_attempt_count_non_negative;
alter table public.match_imports add constraint match_imports_attempt_count_non_negative
  check (attempt_count >= 0);

create unique index if not exists tournament_registrations_seed_idx
  on public.tournament_registrations(tournament_id, seed_number)
  where seed_number is not null;

create unique index if not exists heroes_slug_unique_idx
  on public.heroes(slug)
  where slug is not null;

create table if not exists public.profile_external_accounts (
  id uuid primary key default gen_random_uuid(),
  profile_id uuid not null references public.profiles(id) on delete cascade,
  provider public.dotaops_external_account_provider not null,
  provider_account_id text not null,
  display_name text,
  profile_url text,
  is_primary boolean not null default false,
  verified_at timestamptz,
  metadata jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (provider, provider_account_id),
  unique (profile_id, provider, provider_account_id)
);

create unique index if not exists profile_external_accounts_one_primary_idx
  on public.profile_external_accounts(profile_id, provider)
  where is_primary;

create table if not exists public.tournament_staff (
  id uuid primary key default gen_random_uuid(),
  tournament_id uuid not null references public.tournaments(id) on delete cascade,
  profile_id uuid not null references public.profiles(id) on delete cascade,
  staff_role public.dotaops_tournament_staff_role not null default 'organizer',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (tournament_id, profile_id)
);

create table if not exists public.tournament_groups (
  id uuid primary key default gen_random_uuid(),
  tournament_id uuid not null references public.tournaments(id) on delete cascade,
  name text not null,
  sort_order integer not null default 1,
  settings jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (tournament_id, name),
  unique (tournament_id, sort_order),
  constraint tournament_groups_sort_positive check (sort_order > 0),
  constraint tournament_groups_name_length check (char_length(name) between 1 and 80)
);

create table if not exists public.tournament_group_teams (
  id uuid primary key default gen_random_uuid(),
  group_id uuid not null references public.tournament_groups(id) on delete cascade,
  team_id uuid not null references public.teams(id) on delete cascade,
  registration_id uuid references public.tournament_registrations(id) on delete set null,
  seed_number integer,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (group_id, team_id),
  unique (group_id, seed_number),
  constraint tournament_group_teams_seed_positive check (seed_number is null or seed_number > 0)
);

alter table public.matches
  add column if not exists group_id uuid references public.tournament_groups(id) on delete set null;

create table if not exists public.tournament_registration_members (
  id uuid primary key default gen_random_uuid(),
  registration_id uuid not null references public.tournament_registrations(id) on delete cascade,
  profile_id uuid not null references public.profiles(id) on delete cascade,
  team_member_id uuid references public.team_members(id) on delete set null,
  member_role public.dotaops_team_member_role not null default 'support',
  is_starter boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (registration_id, profile_id)
);

create table if not exists public.team_invitations (
  id uuid primary key default gen_random_uuid(),
  team_id uuid not null references public.teams(id) on delete cascade,
  inviter_profile_id uuid references public.profiles(id) on delete set null,
  invitee_profile_id uuid references public.profiles(id) on delete cascade,
  invitee_email text,
  proposed_role public.dotaops_team_member_role not null default 'support',
  status public.dotaops_invitation_status not null default 'pending',
  token_hash text,
  expires_at timestamptz,
  accepted_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint team_invitations_invitee_present check (
    invitee_profile_id is not null
    or invitee_email is not null
  ),
  constraint team_invitations_expiry_after_create check (
    expires_at is null
    or expires_at >= created_at
  )
);

create unique index if not exists team_invitations_pending_profile_idx
  on public.team_invitations(team_id, invitee_profile_id)
  where status = 'pending' and invitee_profile_id is not null;

create unique index if not exists team_invitations_pending_email_idx
  on public.team_invitations(team_id, lower(invitee_email))
  where status = 'pending' and invitee_email is not null;

create table if not exists public.match_slots (
  id uuid primary key default gen_random_uuid(),
  match_id uuid not null references public.matches(id) on delete cascade,
  slot public.dotaops_match_slot not null,
  source_type public.dotaops_match_slot_source not null default 'manual',
  source_match_id uuid references public.matches(id) on delete set null,
  source_registration_id uuid references public.tournament_registrations(id) on delete set null,
  seed_number integer,
  display_label text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (match_id, slot),
  constraint match_slots_seed_positive check (seed_number is null or seed_number > 0),
  constraint match_slots_source_consistency check (
    (
      source_type in ('manual', 'seed', 'bye')
      and source_match_id is null
    )
    or (
      source_type in ('winner', 'loser')
      and source_match_id is not null
    )
  )
);

create table if not exists public.match_games (
  id uuid primary key default gen_random_uuid(),
  match_id uuid not null references public.matches(id) on delete cascade,
  game_number integer not null,
  status public.dotaops_match_status not null default 'scheduled',
  import_status public.dotaops_import_status not null default 'queued',
  dota_match_id text unique,
  radiant_team_id uuid references public.teams(id) on delete set null,
  dire_team_id uuid references public.teams(id) on delete set null,
  winner_team_id uuid references public.teams(id) on delete set null,
  duration_seconds integer,
  started_at timestamptz,
  finished_at timestamptz,
  raw_summary jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (match_id, game_number),
  constraint match_games_game_number_positive check (game_number > 0),
  constraint match_games_duration_positive check (duration_seconds is null or duration_seconds > 0),
  constraint match_games_dota_match_id_format check (
    dota_match_id is null
    or dota_match_id ~ '^[0-9]+$'
  ),
  constraint match_games_distinct_sides check (
    radiant_team_id is null
    or dire_team_id is null
    or radiant_team_id <> dire_team_id
  ),
  constraint match_games_winner_is_side check (
    winner_team_id is null
    or coalesce(winner_team_id = radiant_team_id, false)
    or coalesce(winner_team_id = dire_team_id, false)
  ),
  constraint match_games_time_order check (
    finished_at is null
    or started_at is null
    or finished_at >= started_at
  )
);

alter table public.match_imports
  add column if not exists match_game_id uuid references public.match_games(id) on delete cascade;

alter table public.match_players
  add column if not exists match_game_id uuid references public.match_games(id) on delete cascade;

alter table public.match_imports drop constraint if exists match_imports_match_id_key;

create unique index if not exists match_imports_match_game_id_idx
  on public.match_imports(match_game_id)
  where match_game_id is not null;

create unique index if not exists match_players_match_game_player_slot_idx
  on public.match_players(match_game_id, player_slot)
  where match_game_id is not null;

create table if not exists public.match_import_events (
  id uuid primary key default gen_random_uuid(),
  match_import_id uuid not null references public.match_imports(id) on delete cascade,
  status public.dotaops_import_status not null,
  message text,
  payload jsonb not null default '{}'::jsonb,
  created_by uuid references public.profiles(id) on delete set null,
  created_at timestamptz not null default now()
);

create table if not exists public.notification_outbox (
  id uuid primary key default gen_random_uuid(),
  recipient_profile_id uuid references public.profiles(id) on delete cascade,
  channel public.dotaops_notification_channel not null,
  subject text,
  body text,
  payload jsonb not null default '{}'::jsonb,
  status public.dotaops_delivery_status not null default 'queued',
  available_at timestamptz not null default now(),
  sent_at timestamptz,
  attempts integer not null default 0,
  last_error text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint notification_outbox_attempts_non_negative check (attempts >= 0)
);

create table if not exists public.audit_log (
  id uuid primary key default gen_random_uuid(),
  actor_auth_user_id uuid,
  actor_profile_id uuid references public.profiles(id) on delete set null,
  table_name text not null,
  record_id uuid,
  action public.dotaops_audit_action not null,
  previous_row jsonb,
  new_row jsonb,
  created_at timestamptz not null default now()
);

create index if not exists profile_external_accounts_profile_idx
  on public.profile_external_accounts(profile_id);
create index if not exists tournament_staff_profile_idx
  on public.tournament_staff(profile_id);
create index if not exists tournament_staff_tournament_role_idx
  on public.tournament_staff(tournament_id, staff_role);
create index if not exists tournament_groups_tournament_idx
  on public.tournament_groups(tournament_id, sort_order);
create index if not exists tournament_group_teams_team_idx
  on public.tournament_group_teams(team_id);
create index if not exists tournament_registration_members_profile_idx
  on public.tournament_registration_members(profile_id);
create index if not exists team_invitations_team_status_idx
  on public.team_invitations(team_id, status);
create index if not exists match_slots_match_idx
  on public.match_slots(match_id);
create index if not exists match_slots_source_match_idx
  on public.match_slots(source_match_id);
create index if not exists matches_group_id_idx
  on public.matches(group_id);
create index if not exists matches_tournament_stage_idx
  on public.matches(tournament_id, stage_name, round_number, bracket_position);
create index if not exists match_games_match_idx
  on public.match_games(match_id, game_number);
create index if not exists match_games_status_idx
  on public.match_games(status, import_status);
create index if not exists match_imports_match_game_status_idx
  on public.match_imports(match_game_id, status);
create index if not exists match_players_match_game_idx
  on public.match_players(match_game_id);
create index if not exists match_import_events_import_idx
  on public.match_import_events(match_import_id, created_at desc);
create index if not exists notification_outbox_recipient_status_idx
  on public.notification_outbox(recipient_profile_id, status, available_at);
create index if not exists audit_log_table_record_idx
  on public.audit_log(table_name, record_id, created_at desc);
create index if not exists audit_log_actor_idx
  on public.audit_log(actor_profile_id, created_at desc);

drop trigger if exists team_members_set_updated_at on public.team_members;
create trigger team_members_set_updated_at
before update on public.team_members
for each row execute function public.set_updated_at();

create trigger profile_external_accounts_set_updated_at
before update on public.profile_external_accounts
for each row execute function public.set_updated_at();

create trigger tournament_staff_set_updated_at
before update on public.tournament_staff
for each row execute function public.set_updated_at();

create trigger tournament_groups_set_updated_at
before update on public.tournament_groups
for each row execute function public.set_updated_at();

create trigger tournament_group_teams_set_updated_at
before update on public.tournament_group_teams
for each row execute function public.set_updated_at();

create trigger tournament_registration_members_set_updated_at
before update on public.tournament_registration_members
for each row execute function public.set_updated_at();

create trigger team_invitations_set_updated_at
before update on public.team_invitations
for each row execute function public.set_updated_at();

create trigger match_slots_set_updated_at
before update on public.match_slots
for each row execute function public.set_updated_at();

create trigger match_games_set_updated_at
before update on public.match_games
for each row execute function public.set_updated_at();

create trigger notification_outbox_set_updated_at
before update on public.notification_outbox
for each row execute function public.set_updated_at();

insert into public.tournament_staff (tournament_id, profile_id, staff_role)
select t.id, t.organizer_profile_id, 'owner'
from public.tournaments t
where t.organizer_profile_id is not null
on conflict (tournament_id, profile_id) do nothing;

insert into public.tournament_registration_members (
  registration_id,
  profile_id,
  team_member_id,
  member_role,
  is_starter
)
select
  tr.id,
  tm.profile_id,
  tm.id,
  tm.member_role,
  tm.member_role not in ('coach', 'substitute')
from public.tournament_registrations tr
join public.team_members tm on tm.team_id = tr.team_id
where tm.is_active
on conflict (registration_id, profile_id) do nothing;

insert into public.match_games (
  match_id,
  game_number,
  status,
  import_status,
  dota_match_id,
  radiant_team_id,
  dire_team_id,
  winner_team_id,
  started_at,
  finished_at
)
select
  m.id,
  1,
  m.status,
  case
    when m.dota_match_id is null then 'queued'::public.dotaops_import_status
    else 'ready'::public.dotaops_import_status
  end,
  m.dota_match_id,
  m.team_a_id,
  m.team_b_id,
  m.winner_team_id,
  m.started_at,
  m.finished_at
from public.matches m
where m.dota_match_id is not null
on conflict (dota_match_id) do nothing;

update public.match_imports mi
set match_game_id = mg.id
from public.match_games mg
where mi.match_game_id is null
  and mi.dota_match_id = mg.dota_match_id;

update public.match_players mp
set match_game_id = mi.match_game_id
from public.match_imports mi
where mp.match_game_id is null
  and mp.match_import_id = mi.id
  and mi.match_game_id is not null;

create or replace function private.current_profile_id()
returns uuid
language sql
stable
security definer
set search_path = public
as $$
  select p.id
  from public.profiles p
  where p.auth_user_id = (select auth.uid())
  limit 1
$$;

create or replace function private.is_admin()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1
    from public.profiles p
    where p.auth_user_id = (select auth.uid())
      and p.role = 'admin'
  )
$$;

create or replace function private.is_organizer_or_admin()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1
    from public.profiles p
    where p.auth_user_id = (select auth.uid())
      and p.role in ('organizer', 'admin')
  )
$$;

create or replace function private.is_team_captain(target_team_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1
    from public.teams t
    where t.id = target_team_id
      and t.captain_profile_id = private.current_profile_id()
  )
$$;

create or replace function private.can_manage_tournament(target_tournament_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select private.is_organizer_or_admin()
    or exists (
      select 1
      from public.tournaments t
      where t.id = target_tournament_id
        and t.organizer_profile_id = private.current_profile_id()
    )
    or exists (
      select 1
      from public.tournament_staff ts
      where ts.tournament_id = target_tournament_id
        and ts.profile_id = private.current_profile_id()
        and ts.staff_role in ('owner', 'organizer')
    )
$$;

create or replace function private.can_officiate_tournament(target_tournament_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select private.can_manage_tournament(target_tournament_id)
    or exists (
      select 1
      from public.tournament_staff ts
      where ts.tournament_id = target_tournament_id
        and ts.profile_id = private.current_profile_id()
        and ts.staff_role in ('referee', 'analyst')
    )
$$;

create or replace function private.can_read_tournament(target_tournament_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
      select 1
      from public.tournaments t
      where t.id = target_tournament_id
        and t.is_public
    )
    or private.can_officiate_tournament(target_tournament_id)
    or exists (
      select 1
      from public.tournament_registrations tr
      where tr.tournament_id = target_tournament_id
        and tr.status in ('pending', 'approved', 'waitlisted')
        and private.is_team_captain(tr.team_id)
    )
$$;

create or replace function private.can_read_match(target_match_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1
    from public.matches m
    where m.id = target_match_id
      and private.can_read_tournament(m.tournament_id)
  )
$$;

create or replace function private.can_officiate_match(target_match_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1
    from public.matches m
    where m.id = target_match_id
      and private.can_officiate_tournament(m.tournament_id)
  )
$$;

create or replace function private.can_read_match_game(target_match_game_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1
    from public.match_games mg
    where mg.id = target_match_game_id
      and private.can_read_match(mg.match_id)
  )
$$;

create or replace function private.can_officiate_match_game(target_match_game_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1
    from public.match_games mg
    where mg.id = target_match_game_id
      and private.can_officiate_match(mg.match_id)
  )
$$;

create or replace function private.can_read_registration(target_registration_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1
    from public.tournament_registrations tr
    where tr.id = target_registration_id
      and (
        private.can_manage_tournament(tr.tournament_id)
        or private.is_team_captain(tr.team_id)
        or (
          tr.status = 'approved'
          and private.can_read_tournament(tr.tournament_id)
        )
      )
  )
$$;

create or replace function private.validate_registration_starter_limit()
returns trigger
language plpgsql
set search_path = public
as $$
begin
  if new.is_starter and (
    select count(*)
    from public.tournament_registration_members trm
    where trm.registration_id = new.registration_id
      and trm.is_starter
      and trm.id <> coalesce(new.id, '00000000-0000-0000-0000-000000000000'::uuid)
  ) >= 5 then
    raise exception 'A tournament registration can have at most five starters.';
  end if;

  return new;
end;
$$;

create or replace function private.validate_match_game_team_alignment()
returns trigger
language plpgsql
set search_path = public
as $$
declare
  match_team_a uuid;
  match_team_b uuid;
begin
  select m.team_a_id, m.team_b_id
  into match_team_a, match_team_b
  from public.matches m
  where m.id = new.match_id;

  if new.radiant_team_id is not null
    and new.radiant_team_id is distinct from match_team_a
    and new.radiant_team_id is distinct from match_team_b then
    raise exception 'Radiant team must be one of the teams assigned to the match.';
  end if;

  if new.dire_team_id is not null
    and new.dire_team_id is distinct from match_team_a
    and new.dire_team_id is distinct from match_team_b then
    raise exception 'Dire team must be one of the teams assigned to the match.';
  end if;

  return new;
end;
$$;

create or replace function private.sync_match_import_match_game()
returns trigger
language plpgsql
set search_path = public
as $$
declare
  linked_match_id uuid;
  linked_dota_match_id text;
begin
  if new.match_game_id is null and new.dota_match_id is not null then
    select mg.id
    into new.match_game_id
    from public.match_games mg
    where mg.dota_match_id = new.dota_match_id
    limit 1;
  end if;

  if new.match_game_id is not null then
    select mg.match_id, mg.dota_match_id
    into linked_match_id, linked_dota_match_id
    from public.match_games mg
    where mg.id = new.match_game_id;

    if linked_match_id is null then
      raise exception 'match_game_id does not reference an existing match game.';
    end if;

    if new.match_id is not null and new.match_id <> linked_match_id then
      raise exception 'match_imports.match_id must match match_games.match_id.';
    end if;

    new.match_id = linked_match_id;

    if linked_dota_match_id is not null and linked_dota_match_id <> new.dota_match_id then
      raise exception 'match_imports.dota_match_id must match match_games.dota_match_id.';
    end if;
  end if;

  return new;
end;
$$;

create or replace function private.apply_match_import_status()
returns trigger
language plpgsql
set search_path = public
as $$
begin
  if new.match_game_id is not null then
    update public.match_games
    set import_status = new.status,
        updated_at = now()
    where id = new.match_game_id;
  end if;

  return new;
end;
$$;

create or replace function private.write_audit_log()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  audit_record_id uuid;
begin
  audit_record_id = nullif(coalesce(to_jsonb(new)->>'id', to_jsonb(old)->>'id'), '')::uuid;

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
    (select auth.uid()),
    private.current_profile_id(),
    tg_table_schema || '.' || tg_table_name,
    audit_record_id,
    lower(tg_op)::public.dotaops_audit_action,
    case when tg_op in ('UPDATE', 'DELETE') then to_jsonb(old) else null end,
    case when tg_op in ('INSERT', 'UPDATE') then to_jsonb(new) else null end
  );

  return coalesce(new, old);
end;
$$;

drop trigger if exists tournament_registration_members_validate_starters on public.tournament_registration_members;
create trigger tournament_registration_members_validate_starters
before insert or update of is_starter, registration_id
on public.tournament_registration_members
for each row execute function private.validate_registration_starter_limit();

drop trigger if exists match_games_validate_team_alignment on public.match_games;
create trigger match_games_validate_team_alignment
before insert or update of match_id, radiant_team_id, dire_team_id
on public.match_games
for each row execute function private.validate_match_game_team_alignment();

drop trigger if exists match_imports_sync_match_game on public.match_imports;
create trigger match_imports_sync_match_game
before insert or update of match_game_id, match_id, dota_match_id
on public.match_imports
for each row execute function private.sync_match_import_match_game();

drop trigger if exists match_imports_apply_status on public.match_imports;
create trigger match_imports_apply_status
after insert or update of status, match_game_id
on public.match_imports
for each row execute function private.apply_match_import_status();

drop function if exists public.refresh_dotaops_analytics();
drop materialized view if exists public.mv_player_metrics;
drop materialized view if exists public.mv_team_metrics;
drop materialized view if exists public.mv_hero_metrics;

create materialized view public.mv_player_metrics as
select
  mp.profile_id,
  m.tournament_id,
  t.is_public,
  count(*)::integer as games_played,
  count(*) filter (where mp.is_winner)::integer as wins,
  count(*) filter (where mp.is_winner is false)::integer as losses,
  sum(mp.kills)::integer as kills,
  sum(mp.deaths)::integer as deaths,
  sum(mp.assists)::integer as assists,
  round(
    case
      when sum(mp.deaths) = 0 then sum(mp.kills + mp.assists)::numeric
      else sum(mp.kills + mp.assists)::numeric / nullif(sum(mp.deaths), 0)
    end,
    2
  ) as kda,
  round((count(*) filter (where mp.is_winner)::numeric / nullif(count(*), 0)) * 100, 2) as win_rate,
  round(avg(coalesce(mg.duration_seconds, mp.duration_seconds)))::integer as avg_duration_seconds,
  now() as refreshed_at
from public.match_players mp
left join public.match_games mg on mg.id = mp.match_game_id
join public.matches m on m.id = coalesce(mg.match_id, mp.match_id)
join public.tournaments t on t.id = m.tournament_id
where mp.profile_id is not null
group by mp.profile_id, m.tournament_id, t.is_public;

create materialized view public.mv_team_metrics as
select
  mp.team_id,
  m.tournament_id,
  t.is_public,
  count(distinct coalesce('game:' || mp.match_game_id::text, 'legacy-match:' || mp.match_id::text))::integer as games_played,
  count(distinct coalesce('game:' || mp.match_game_id::text, 'legacy-match:' || mp.match_id::text)) filter (where mp.is_winner)::integer as wins,
  sum(mp.kills)::integer as kills,
  sum(mp.deaths)::integer as deaths,
  sum(mp.assists)::integer as assists,
  round(
    case
      when sum(mp.deaths) = 0 then sum(mp.kills + mp.assists)::numeric
      else sum(mp.kills + mp.assists)::numeric / nullif(sum(mp.deaths), 0)
    end,
    2
  ) as kda,
  round(
    (count(distinct coalesce('game:' || mp.match_game_id::text, 'legacy-match:' || mp.match_id::text)) filter (where mp.is_winner)::numeric
      / nullif(count(distinct coalesce('game:' || mp.match_game_id::text, 'legacy-match:' || mp.match_id::text)), 0)) * 100,
    2
  ) as win_rate,
  now() as refreshed_at
from public.match_players mp
left join public.match_games mg on mg.id = mp.match_game_id
join public.matches m on m.id = coalesce(mg.match_id, mp.match_id)
join public.tournaments t on t.id = m.tournament_id
where mp.team_id is not null
group by mp.team_id, m.tournament_id, t.is_public;

create materialized view public.mv_hero_metrics as
select
  mp.hero_id,
  m.tournament_id,
  t.is_public,
  count(*)::integer as picks,
  count(*) filter (where mp.is_winner)::integer as wins,
  round((count(*) filter (where mp.is_winner)::numeric / nullif(count(*), 0)) * 100, 2) as win_rate,
  round(
    avg(
      case
        when mp.deaths = 0 then (mp.kills + mp.assists)::numeric
        else (mp.kills + mp.assists)::numeric / nullif(mp.deaths, 0)
      end
    ),
    2
  ) as avg_kda,
  now() as refreshed_at
from public.match_players mp
left join public.match_games mg on mg.id = mp.match_game_id
join public.matches m on m.id = coalesce(mg.match_id, mp.match_id)
join public.tournaments t on t.id = m.tournament_id
where mp.hero_id is not null
group by mp.hero_id, m.tournament_id, t.is_public;

create materialized view public.mv_tournament_metrics as
select
  t.id as tournament_id,
  t.is_public,
  count(distinct m.id)::integer as series_count,
  count(distinct m.id) filter (where m.status = 'finished')::integer as finished_series_count,
  count(mg.id)::integer as games_count,
  count(mg.id) filter (where mg.status = 'finished')::integer as finished_games_count,
  round(avg(mg.duration_seconds))::integer as avg_game_duration_seconds,
  now() as refreshed_at
from public.tournaments t
left join public.matches m on m.tournament_id = t.id
left join public.match_games mg on mg.match_id = m.id
group by t.id, t.is_public;

create unique index mv_player_metrics_unique_idx
  on public.mv_player_metrics(profile_id, tournament_id);
create unique index mv_team_metrics_unique_idx
  on public.mv_team_metrics(team_id, tournament_id);
create unique index mv_hero_metrics_unique_idx
  on public.mv_hero_metrics(hero_id, tournament_id);
create unique index mv_tournament_metrics_unique_idx
  on public.mv_tournament_metrics(tournament_id);

create or replace function private.refresh_dotaops_analytics()
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  refresh materialized view public.mv_player_metrics;
  refresh materialized view public.mv_team_metrics;
  refresh materialized view public.mv_hero_metrics;
  refresh materialized view public.mv_tournament_metrics;
end;
$$;

drop view if exists public.v_player_metrics;
create view public.v_player_metrics
with (security_invoker = true)
as
select
  mp.profile_id,
  m.tournament_id,
  count(*)::integer as games_played,
  count(*) filter (where mp.is_winner)::integer as wins,
  count(*) filter (where mp.is_winner is false)::integer as losses,
  sum(mp.kills)::integer as kills,
  sum(mp.deaths)::integer as deaths,
  sum(mp.assists)::integer as assists,
  round(
    case
      when sum(mp.deaths) = 0 then sum(mp.kills + mp.assists)::numeric
      else sum(mp.kills + mp.assists)::numeric / nullif(sum(mp.deaths), 0)
    end,
    2
  ) as kda,
  round((count(*) filter (where mp.is_winner)::numeric / nullif(count(*), 0)) * 100, 2) as win_rate,
  round(avg(coalesce(mg.duration_seconds, mp.duration_seconds)))::integer as avg_duration_seconds
from public.match_players mp
left join public.match_games mg on mg.id = mp.match_game_id
join public.matches m on m.id = coalesce(mg.match_id, mp.match_id)
join public.tournaments t on t.id = m.tournament_id
where mp.profile_id is not null
  and t.is_public
group by mp.profile_id, m.tournament_id;

drop view if exists public.v_team_metrics;
create view public.v_team_metrics
with (security_invoker = true)
as
select
  mp.team_id,
  m.tournament_id,
  count(distinct coalesce('game:' || mp.match_game_id::text, 'legacy-match:' || mp.match_id::text))::integer as games_played,
  count(distinct coalesce('game:' || mp.match_game_id::text, 'legacy-match:' || mp.match_id::text)) filter (where mp.is_winner)::integer as wins,
  sum(mp.kills)::integer as kills,
  sum(mp.deaths)::integer as deaths,
  sum(mp.assists)::integer as assists,
  round(
    case
      when sum(mp.deaths) = 0 then sum(mp.kills + mp.assists)::numeric
      else sum(mp.kills + mp.assists)::numeric / nullif(sum(mp.deaths), 0)
    end,
    2
  ) as kda,
  round(
    (count(distinct coalesce('game:' || mp.match_game_id::text, 'legacy-match:' || mp.match_id::text)) filter (where mp.is_winner)::numeric
      / nullif(count(distinct coalesce('game:' || mp.match_game_id::text, 'legacy-match:' || mp.match_id::text)), 0)) * 100,
    2
  ) as win_rate
from public.match_players mp
left join public.match_games mg on mg.id = mp.match_game_id
join public.matches m on m.id = coalesce(mg.match_id, mp.match_id)
join public.tournaments t on t.id = m.tournament_id
where mp.team_id is not null
  and t.is_public
group by mp.team_id, m.tournament_id;

drop view if exists public.v_hero_metrics;
create view public.v_hero_metrics
with (security_invoker = true)
as
select
  mp.hero_id,
  m.tournament_id,
  count(*)::integer as picks,
  count(*) filter (where mp.is_winner)::integer as wins,
  round((count(*) filter (where mp.is_winner)::numeric / nullif(count(*), 0)) * 100, 2) as win_rate,
  round(
    avg(
      case
        when mp.deaths = 0 then (mp.kills + mp.assists)::numeric
        else (mp.kills + mp.assists)::numeric / nullif(mp.deaths, 0)
      end
    ),
    2
  ) as avg_kda
from public.match_players mp
left join public.match_games mg on mg.id = mp.match_game_id
join public.matches m on m.id = coalesce(mg.match_id, mp.match_id)
join public.tournaments t on t.id = m.tournament_id
where mp.hero_id is not null
  and t.is_public
group by mp.hero_id, m.tournament_id;

drop view if exists public.v_tournament_metrics;
create view public.v_tournament_metrics
with (security_invoker = true)
as
select
  t.id as tournament_id,
  count(distinct m.id)::integer as series_count,
  count(distinct m.id) filter (where m.status = 'finished')::integer as finished_series_count,
  count(mg.id)::integer as games_count,
  count(mg.id) filter (where mg.status = 'finished')::integer as finished_games_count,
  round(avg(mg.duration_seconds))::integer as avg_game_duration_seconds
from public.tournaments t
left join public.matches m on m.tournament_id = t.id
left join public.match_games mg on mg.match_id = m.id
where t.is_public
group by t.id;

drop view if exists public.v_group_standings;
create view public.v_group_standings
with (security_invoker = true)
as
select
  tg.id as group_id,
  tg.tournament_id,
  tgt.team_id,
  count(m.id) filter (where m.status = 'finished')::integer as series_played,
  count(m.id) filter (where m.status = 'finished' and m.winner_team_id = tgt.team_id)::integer as series_wins,
  count(m.id) filter (
    where m.status = 'finished'
      and m.winner_team_id is not null
      and m.winner_team_id <> tgt.team_id
  )::integer as series_losses,
  coalesce(sum(
    case
      when m.team_a_id = tgt.team_id then m.score_a
      when m.team_b_id = tgt.team_id then m.score_b
      else 0
    end
  ), 0)::integer as maps_won,
  coalesce(sum(
    case
      when m.team_a_id = tgt.team_id then m.score_b
      when m.team_b_id = tgt.team_id then m.score_a
      else 0
    end
  ), 0)::integer as maps_lost
from public.tournament_groups tg
join public.tournaments t on t.id = tg.tournament_id
join public.tournament_group_teams tgt on tgt.group_id = tg.id
left join public.matches m
  on m.group_id = tg.id
  and (m.team_a_id = tgt.team_id or m.team_b_id = tgt.team_id)
where t.is_public
group by tg.id, tg.tournament_id, tgt.team_id;

drop view if exists public.public_match_import_status;
create view public.public_match_import_status
with (security_invoker = true)
as
select
  mi.id,
  mi.match_id,
  mi.dota_match_id,
  mi.status,
  mi.created_at,
  mi.updated_at,
  mi.match_game_id
from public.match_imports mi
join public.matches m on m.id = mi.match_id
join public.tournaments t on t.id = m.tournament_id
where t.is_public;

alter table public.profile_external_accounts enable row level security;
alter table public.tournament_staff enable row level security;
alter table public.tournament_groups enable row level security;
alter table public.tournament_group_teams enable row level security;
alter table public.tournament_registration_members enable row level security;
alter table public.team_invitations enable row level security;
alter table public.match_slots enable row level security;
alter table public.match_games enable row level security;
alter table public.match_import_events enable row level security;
alter table public.notification_outbox enable row level security;
alter table public.audit_log enable row level security;

drop policy if exists "profiles are publicly readable" on public.profiles;
drop policy if exists "users create own profile" on public.profiles;
drop policy if exists "users update own profile" on public.profiles;
drop policy if exists "teams are publicly readable" on public.teams;
drop policy if exists "authenticated users create teams" on public.teams;
drop policy if exists "captains and organizers update teams" on public.teams;
drop policy if exists "team members are publicly readable" on public.team_members;
drop policy if exists "captains manage team members" on public.team_members;
drop policy if exists "public tournaments are readable" on public.tournaments;
drop policy if exists "organizers create tournaments" on public.tournaments;
drop policy if exists "organizers update tournaments" on public.tournaments;
drop policy if exists "approved registrations are readable" on public.tournament_registrations;
drop policy if exists "captains create registrations" on public.tournament_registrations;
drop policy if exists "organizers update registrations" on public.tournament_registrations;
drop policy if exists "public matches are readable" on public.matches;
drop policy if exists "organizers manage matches" on public.matches;
drop policy if exists "match imports are readable for public matches" on public.match_imports;
drop policy if exists "authenticated users request imports" on public.match_imports;
drop policy if exists "organizers update imports" on public.match_imports;
drop policy if exists "heroes are publicly readable" on public.heroes;
drop policy if exists "organizers manage heroes" on public.heroes;
drop policy if exists "match players are readable for public matches" on public.match_players;
drop policy if exists "organizers manage match players" on public.match_players;

drop function if exists public.current_profile_id();
drop function if exists public.is_organizer_or_admin();
drop function if exists public.is_team_captain(uuid);
drop function if exists public.can_manage_tournament(uuid);

create policy "profiles are publicly readable"
on public.profiles for select
to anon, authenticated
using (true);

create policy "users create own player profile"
on public.profiles for insert
to authenticated
with check (
  auth_user_id = (select auth.uid())
  and role = 'player'
);

create policy "users update own profile details"
on public.profiles for update
to authenticated
using (
  auth_user_id = (select auth.uid())
  or (select private.is_organizer_or_admin())
)
with check (
  auth_user_id = (select auth.uid())
  or (select private.is_organizer_or_admin())
);

create policy "teams are publicly readable"
on public.teams for select
to anon, authenticated
using (true);

create policy "authenticated users create own teams"
on public.teams for insert
to authenticated
with check (
  (select private.is_organizer_or_admin())
  or (
    created_by = (select auth.uid())
    and (
      captain_profile_id is null
      or captain_profile_id = (select private.current_profile_id())
    )
  )
);

create policy "captains and organizers update teams"
on public.teams for update
to authenticated
using (
  private.is_team_captain(id)
  or (select private.is_organizer_or_admin())
)
with check (
  private.is_team_captain(id)
  or (select private.is_organizer_or_admin())
);

create policy "team members are publicly readable"
on public.team_members for select
to anon, authenticated
using (true);

create policy "captains manage team members"
on public.team_members for all
to authenticated
using (
  private.is_team_captain(team_id)
  or (select private.is_organizer_or_admin())
)
with check (
  private.is_team_captain(team_id)
  or (select private.is_organizer_or_admin())
);

create policy "external accounts are owner readable"
on public.profile_external_accounts for select
to authenticated
using (
  profile_id = (select private.current_profile_id())
  or (select private.is_organizer_or_admin())
);

create policy "users manage own external accounts"
on public.profile_external_accounts for all
to authenticated
using (
  profile_id = (select private.current_profile_id())
  or (select private.is_organizer_or_admin())
)
with check (
  profile_id = (select private.current_profile_id())
  or (select private.is_organizer_or_admin())
);

create policy "public or permitted tournaments are readable"
on public.tournaments for select
to anon, authenticated
using (private.can_read_tournament(id));

create policy "organizers create tournaments"
on public.tournaments for insert
to authenticated
with check ((select private.is_organizer_or_admin()));

create policy "tournament owners update tournaments"
on public.tournaments for update
to authenticated
using (private.can_manage_tournament(id))
with check (private.can_manage_tournament(id));

create policy "tournament staff is readable with tournament"
on public.tournament_staff for select
to anon, authenticated
using (private.can_read_tournament(tournament_id));

create policy "tournament owners manage staff"
on public.tournament_staff for all
to authenticated
using (private.can_manage_tournament(tournament_id))
with check (private.can_manage_tournament(tournament_id));

create policy "registrations are readable when relevant"
on public.tournament_registrations for select
to anon, authenticated
using (private.can_read_registration(id));

create policy "captains create registrations"
on public.tournament_registrations for insert
to authenticated
with check (
  private.is_team_captain(team_id)
  or private.can_manage_tournament(tournament_id)
);

create policy "organizers update registrations"
on public.tournament_registrations for update
to authenticated
using (private.can_manage_tournament(tournament_id))
with check (private.can_manage_tournament(tournament_id));

create policy "registration members are readable when registration is readable"
on public.tournament_registration_members for select
to anon, authenticated
using (private.can_read_registration(registration_id));

create policy "captains manage registration members"
on public.tournament_registration_members for all
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

create policy "team invitations are readable by participants"
on public.team_invitations for select
to authenticated
using (
  private.is_team_captain(team_id)
  or invitee_profile_id = (select private.current_profile_id())
  or (select private.is_organizer_or_admin())
);

create policy "captains manage team invitations"
on public.team_invitations for all
to authenticated
using (
  private.is_team_captain(team_id)
  or (select private.is_organizer_or_admin())
)
with check (
  private.is_team_captain(team_id)
  or (select private.is_organizer_or_admin())
);

create policy "tournament groups are readable with tournament"
on public.tournament_groups for select
to anon, authenticated
using (private.can_read_tournament(tournament_id));

create policy "tournament organizers manage groups"
on public.tournament_groups for all
to authenticated
using (private.can_manage_tournament(tournament_id))
with check (private.can_manage_tournament(tournament_id));

create policy "group teams are readable with group"
on public.tournament_group_teams for select
to anon, authenticated
using (
  exists (
    select 1
    from public.tournament_groups tg
    where tg.id = tournament_group_teams.group_id
      and private.can_read_tournament(tg.tournament_id)
  )
);

create policy "tournament organizers manage group teams"
on public.tournament_group_teams for all
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

create policy "matches are readable with tournament"
on public.matches for select
to anon, authenticated
using (private.can_read_tournament(tournament_id));

create policy "officials manage matches"
on public.matches for all
to authenticated
using (private.can_officiate_tournament(tournament_id))
with check (private.can_officiate_tournament(tournament_id));

create policy "match slots are readable with match"
on public.match_slots for select
to anon, authenticated
using (private.can_read_match(match_id));

create policy "officials manage match slots"
on public.match_slots for all
to authenticated
using (private.can_officiate_match(match_id))
with check (private.can_officiate_match(match_id));

create policy "match games are readable with match"
on public.match_games for select
to anon, authenticated
using (private.can_read_match(match_id));

create policy "officials manage match games"
on public.match_games for all
to authenticated
using (private.can_officiate_match(match_id))
with check (private.can_officiate_match(match_id));

create policy "match imports are readable to requesters and officials"
on public.match_imports for select
to anon, authenticated
using (
  (
    match_id is not null
    and private.can_read_match(match_id)
  )
  or requested_by = (select private.current_profile_id())
);

create policy "authenticated users request imports"
on public.match_imports for insert
to authenticated
with check (
  requested_by = (select private.current_profile_id())
  or (
    match_id is not null
    and private.can_officiate_match(match_id)
  )
  or (
    match_game_id is not null
    and private.can_officiate_match_game(match_game_id)
  )
);

create policy "officials update imports"
on public.match_imports for update
to authenticated
using (
  (match_id is not null and private.can_officiate_match(match_id))
  or (match_game_id is not null and private.can_officiate_match_game(match_game_id))
  or (select private.is_organizer_or_admin())
)
with check (
  (match_id is not null and private.can_officiate_match(match_id))
  or (match_game_id is not null and private.can_officiate_match_game(match_game_id))
  or (select private.is_organizer_or_admin())
);

create policy "match import events readable with import"
on public.match_import_events for select
to authenticated
using (
  exists (
    select 1
    from public.match_imports mi
    where mi.id = match_import_events.match_import_id
      and (
        mi.requested_by = (select private.current_profile_id())
        or (mi.match_id is not null and private.can_read_match(mi.match_id))
        or (mi.match_game_id is not null and private.can_read_match_game(mi.match_game_id))
      )
  )
);

create policy "officials manage match import events"
on public.match_import_events for all
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

create policy "heroes are publicly readable"
on public.heroes for select
to anon, authenticated
using (true);

create policy "organizers manage heroes"
on public.heroes for all
to authenticated
using ((select private.is_organizer_or_admin()))
with check ((select private.is_organizer_or_admin()));

create policy "match players are readable with match"
on public.match_players for select
to anon, authenticated
using (
  (match_game_id is not null and private.can_read_match_game(match_game_id))
  or (match_id is not null and private.can_read_match(match_id))
);

create policy "officials manage match players"
on public.match_players for all
to authenticated
using (
  (match_game_id is not null and private.can_officiate_match_game(match_game_id))
  or (match_id is not null and private.can_officiate_match(match_id))
)
with check (
  (match_game_id is not null and private.can_officiate_match_game(match_game_id))
  or (match_id is not null and private.can_officiate_match(match_id))
);

create policy "users read own notifications"
on public.notification_outbox for select
to authenticated
using (
  recipient_profile_id = (select private.current_profile_id())
  or (select private.is_organizer_or_admin())
);

create policy "admins read audit log"
on public.audit_log for select
to authenticated
using ((select private.is_admin()));

revoke insert, update, delete on public.profiles from authenticated;
grant insert (auth_user_id, nickname, display_name, steam_id, avatar_url, bio, country_code)
  on public.profiles to authenticated;
grant update (nickname, display_name, steam_id, avatar_url, bio, country_code)
  on public.profiles to authenticated;

grant select on
  public.profiles,
  public.teams,
  public.team_members,
  public.tournaments,
  public.tournament_registrations,
  public.tournament_registration_members,
  public.tournament_staff,
  public.tournament_groups,
  public.tournament_group_teams,
  public.matches,
  public.match_slots,
  public.heroes
to anon, authenticated;

revoke select on public.match_imports, public.match_games, public.match_players from anon, authenticated;

grant select (
  id,
  match_id,
  match_game_id,
  dota_match_id,
  status,
  requested_by,
  error_message,
  started_at,
  completed_at,
  created_at,
  updated_at
) on public.match_imports to anon, authenticated;

grant select (
  id,
  match_id,
  game_number,
  status,
  import_status,
  dota_match_id,
  radiant_team_id,
  dire_team_id,
  winner_team_id,
  duration_seconds,
  started_at,
  finished_at,
  created_at,
  updated_at
) on public.match_games to anon, authenticated;

grant select (
  id,
  match_import_id,
  match_id,
  match_game_id,
  team_id,
  profile_id,
  hero_id,
  player_slot,
  is_radiant,
  is_winner,
  kills,
  deaths,
  assists,
  last_hits,
  denies,
  gold_per_min,
  xp_per_min,
  net_worth,
  hero_damage,
  tower_damage,
  hero_healing,
  level,
  duration_seconds,
  lane_role,
  party_id,
  item_ids,
  benchmarks,
  created_at,
  updated_at
) on public.match_players to anon, authenticated;

grant insert, update, delete on
  public.profile_external_accounts,
  public.team_members,
  public.tournament_registrations,
  public.tournament_registration_members,
  public.team_invitations,
  public.tournament_staff,
  public.tournament_groups,
  public.tournament_group_teams,
  public.matches,
  public.match_slots,
  public.match_games,
  public.match_imports,
  public.match_import_events,
  public.heroes,
  public.match_players
to authenticated;

grant insert, update on public.teams to authenticated;
grant insert, update on public.tournaments to authenticated;
grant select on public.profile_external_accounts, public.team_invitations, public.match_import_events, public.notification_outbox, public.audit_log to authenticated;

grant all on
  public.profile_external_accounts,
  public.tournament_staff,
  public.tournament_groups,
  public.tournament_group_teams,
  public.tournament_registration_members,
  public.team_invitations,
  public.match_slots,
  public.match_games,
  public.match_import_events,
  public.notification_outbox,
  public.audit_log
to service_role;

revoke select on public.mv_player_metrics from anon, authenticated;
revoke select on public.mv_team_metrics from anon, authenticated;
revoke select on public.mv_hero_metrics from anon, authenticated;
revoke select on public.mv_tournament_metrics from anon, authenticated;
grant select on
  public.mv_player_metrics,
  public.mv_team_metrics,
  public.mv_hero_metrics,
  public.mv_tournament_metrics
to service_role;

grant select on
  public.v_player_metrics,
  public.v_team_metrics,
  public.v_hero_metrics,
  public.v_tournament_metrics,
  public.v_group_standings,
  public.public_match_import_status
to anon, authenticated, service_role;

grant execute on function private.refresh_dotaops_analytics() to service_role;

drop trigger if exists audit_teams on public.teams;
create trigger audit_teams
after insert or update or delete on public.teams
for each row execute function private.write_audit_log();

drop trigger if exists audit_tournaments on public.tournaments;
create trigger audit_tournaments
after insert or update or delete on public.tournaments
for each row execute function private.write_audit_log();

drop trigger if exists audit_tournament_registrations on public.tournament_registrations;
create trigger audit_tournament_registrations
after insert or update or delete on public.tournament_registrations
for each row execute function private.write_audit_log();

drop trigger if exists audit_matches on public.matches;
create trigger audit_matches
after insert or update or delete on public.matches
for each row execute function private.write_audit_log();

drop trigger if exists audit_match_games on public.match_games;
create trigger audit_match_games
after insert or update or delete on public.match_games
for each row execute function private.write_audit_log();

drop trigger if exists audit_match_imports on public.match_imports;
create trigger audit_match_imports
after insert or update or delete on public.match_imports
for each row execute function private.write_audit_log();

drop trigger if exists audit_match_players on public.match_players;
create trigger audit_match_players
after insert or update or delete on public.match_players
for each row execute function private.write_audit_log();

comment on schema private is 'Internal helper schema for RLS, validation triggers, analytics refresh, and audit helpers. It is intentionally not exposed through Supabase Data API.';
comment on table public.match_games is 'Individual Dota 2 games/maps inside a tournament match series.';
comment on table public.match_slots is 'Bracket slot source metadata for TBD, seeded, winner, loser, and bye advancement.';
comment on table public.tournament_registration_members is 'Roster snapshot captured for a tournament registration, independent from later team roster changes.';
comment on table public.tournament_groups is 'Group-stage containers for tournaments that use groups before playoffs.';
comment on table public.audit_log is 'Append-only operational audit trail for core tournament and analytics records.';
