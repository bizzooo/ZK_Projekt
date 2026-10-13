create extension if not exists "pgcrypto";

create type public.dotaops_user_role as enum (
  'visitor',
  'player',
  'captain',
  'organizer',
  'admin'
);

create type public.dotaops_team_member_role as enum (
  'carry',
  'mid',
  'offlane',
  'support',
  'roamer',
  'coach',
  'substitute'
);

create type public.dotaops_tournament_status as enum (
  'draft',
  'registration',
  'published',
  'live',
  'finished',
  'archived'
);

create type public.dotaops_tournament_format as enum (
  'single_elimination',
  'groups_playoff',
  'round_robin',
  'best_of_three_playoff'
);

create type public.dotaops_registration_status as enum (
  'pending',
  'approved',
  'rejected',
  'waitlisted'
);

create type public.dotaops_match_status as enum (
  'scheduled',
  'ready',
  'live',
  'finished',
  'cancelled'
);

create type public.dotaops_import_status as enum (
  'queued',
  'processing',
  'ready',
  'error'
);

create table public.profiles (
  id uuid primary key default gen_random_uuid(),
  auth_user_id uuid unique references auth.users(id) on delete set null,
  nickname text not null,
  display_name text,
  steam_id text unique,
  role public.dotaops_user_role not null default 'player',
  avatar_url text,
  bio text,
  country_code char(2),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint profiles_nickname_length check (char_length(nickname) between 2 and 40)
);

create table public.teams (
  id uuid primary key default gen_random_uuid(),
  name text not null unique,
  tag text,
  slug text not null unique,
  captain_profile_id uuid references public.profiles(id) on delete set null,
  region text,
  logo_url text,
  description text,
  created_by uuid references auth.users(id) on delete set null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint teams_slug_format check (slug ~ '^[a-z0-9]+(?:-[a-z0-9]+)*$'),
  constraint teams_name_length check (char_length(name) between 2 and 80)
);

create table public.team_members (
  id uuid primary key default gen_random_uuid(),
  team_id uuid not null references public.teams(id) on delete cascade,
  profile_id uuid not null references public.profiles(id) on delete cascade,
  member_role public.dotaops_team_member_role not null default 'support',
  is_active boolean not null default true,
  joined_at timestamptz not null default now(),
  left_at timestamptz,
  unique (team_id, profile_id)
);

create table public.tournaments (
  id uuid primary key default gen_random_uuid(),
  slug text not null unique,
  title text not null,
  status public.dotaops_tournament_status not null default 'draft',
  format public.dotaops_tournament_format not null default 'single_elimination',
  organizer_profile_id uuid references public.profiles(id) on delete set null,
  description text,
  rules text,
  prize_pool text,
  max_teams integer not null default 8,
  starts_at timestamptz not null,
  ends_at timestamptz,
  registration_opens_at timestamptz,
  registration_closes_at timestamptz,
  is_public boolean not null default true,
  created_by uuid references auth.users(id) on delete set null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint tournaments_slug_format check (slug ~ '^[a-z0-9]+(?:-[a-z0-9]+)*$'),
  constraint tournaments_max_teams check (max_teams between 2 and 128),
  constraint tournaments_dates_order check (ends_at is null or ends_at >= starts_at),
  constraint tournaments_registration_order check (
    registration_opens_at is null
    or registration_closes_at is null
    or registration_closes_at >= registration_opens_at
  )
);

create table public.tournament_registrations (
  id uuid primary key default gen_random_uuid(),
  tournament_id uuid not null references public.tournaments(id) on delete cascade,
  team_id uuid not null references public.teams(id) on delete cascade,
  captain_profile_id uuid references public.profiles(id) on delete set null,
  status public.dotaops_registration_status not null default 'pending',
  message text,
  reviewed_by uuid references public.profiles(id) on delete set null,
  reviewed_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (tournament_id, team_id)
);

create table public.matches (
  id uuid primary key default gen_random_uuid(),
  tournament_id uuid not null references public.tournaments(id) on delete cascade,
  round_name text not null,
  round_number integer not null default 1,
  bracket_position integer,
  status public.dotaops_match_status not null default 'scheduled',
  scheduled_at timestamptz,
  started_at timestamptz,
  finished_at timestamptz,
  best_of integer not null default 1,
  team_a_id uuid references public.teams(id) on delete set null,
  team_b_id uuid references public.teams(id) on delete set null,
  score_a integer not null default 0,
  score_b integer not null default 0,
  winner_team_id uuid references public.teams(id) on delete set null,
  dota_match_id text unique,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint matches_scores_non_negative check (score_a >= 0 and score_b >= 0),
  constraint matches_best_of_positive check (best_of > 0),
  constraint matches_winner_is_participant check (
    winner_team_id is null
    or winner_team_id = team_a_id
    or winner_team_id = team_b_id
  )
);

create table public.match_imports (
  id uuid primary key default gen_random_uuid(),
  match_id uuid unique references public.matches(id) on delete cascade,
  dota_match_id text not null unique,
  status public.dotaops_import_status not null default 'queued',
  requested_by uuid references public.profiles(id) on delete set null,
  raw_response jsonb,
  normalized_payload jsonb,
  error_message text,
  started_at timestamptz,
  completed_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table public.heroes (
  id uuid primary key default gen_random_uuid(),
  dota_hero_id integer not null unique,
  name text not null,
  localized_name text not null,
  primary_attr text,
  attack_type text,
  roles text[] not null default '{}',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table public.match_players (
  id uuid primary key default gen_random_uuid(),
  match_import_id uuid not null references public.match_imports(id) on delete cascade,
  match_id uuid references public.matches(id) on delete cascade,
  team_id uuid references public.teams(id) on delete set null,
  profile_id uuid references public.profiles(id) on delete set null,
  hero_id uuid references public.heroes(id) on delete set null,
  steam_account_id text,
  player_slot integer not null,
  is_radiant boolean,
  is_winner boolean,
  kills integer not null default 0,
  deaths integer not null default 0,
  assists integer not null default 0,
  last_hits integer not null default 0,
  denies integer not null default 0,
  gold_per_min integer,
  xp_per_min integer,
  net_worth integer,
  hero_damage integer,
  tower_damage integer,
  hero_healing integer,
  level integer,
  duration_seconds integer,
  raw_player jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (match_import_id, player_slot),
  constraint match_players_non_negative check (
    kills >= 0
    and deaths >= 0
    and assists >= 0
    and last_hits >= 0
    and denies >= 0
  )
);

create index profiles_auth_user_id_idx on public.profiles(auth_user_id);
create index teams_captain_profile_id_idx on public.teams(captain_profile_id);
create index team_members_team_id_idx on public.team_members(team_id);
create index team_members_profile_id_idx on public.team_members(profile_id);
create index tournaments_status_idx on public.tournaments(status);
create index tournaments_starts_at_idx on public.tournaments(starts_at);
create index tournament_registrations_tournament_id_idx on public.tournament_registrations(tournament_id);
create index tournament_registrations_team_id_idx on public.tournament_registrations(team_id);
create index matches_tournament_id_idx on public.matches(tournament_id);
create index matches_dota_match_id_idx on public.matches(dota_match_id);
create index match_imports_status_idx on public.match_imports(status);
create index match_players_match_id_idx on public.match_players(match_id);
create index match_players_profile_id_idx on public.match_players(profile_id);
create index match_players_team_id_idx on public.match_players(team_id);
create index match_players_hero_id_idx on public.match_players(hero_id);

create or replace function public.set_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

create trigger profiles_set_updated_at
before update on public.profiles
for each row execute function public.set_updated_at();

create trigger teams_set_updated_at
before update on public.teams
for each row execute function public.set_updated_at();

create trigger tournaments_set_updated_at
before update on public.tournaments
for each row execute function public.set_updated_at();

create trigger tournament_registrations_set_updated_at
before update on public.tournament_registrations
for each row execute function public.set_updated_at();

create trigger matches_set_updated_at
before update on public.matches
for each row execute function public.set_updated_at();

create trigger match_imports_set_updated_at
before update on public.match_imports
for each row execute function public.set_updated_at();

create trigger heroes_set_updated_at
before update on public.heroes
for each row execute function public.set_updated_at();

create trigger match_players_set_updated_at
before update on public.match_players
for each row execute function public.set_updated_at();

create or replace function public.current_profile_id()
returns uuid
language sql
stable
security definer
set search_path = public
as $$
  select p.id
  from public.profiles p
  where p.auth_user_id = auth.uid()
  limit 1
$$;

create or replace function public.is_organizer_or_admin()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1
    from public.profiles p
    where p.auth_user_id = auth.uid()
      and p.role in ('organizer', 'admin')
  )
$$;

create or replace function public.is_team_captain(target_team_id uuid)
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
      and t.captain_profile_id = public.current_profile_id()
  )
$$;

create or replace function public.can_manage_tournament(target_tournament_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select public.is_organizer_or_admin()
    or exists (
      select 1
      from public.tournaments t
      where t.id = target_tournament_id
        and t.organizer_profile_id = public.current_profile_id()
    )
$$;

create materialized view public.mv_player_metrics as
select
  mp.profile_id,
  m.tournament_id,
  count(*)::integer as matches_played,
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
  round(avg(mp.duration_seconds))::integer as avg_duration_seconds,
  now() as refreshed_at
from public.match_players mp
join public.matches m on m.id = mp.match_id
where mp.profile_id is not null
group by mp.profile_id, m.tournament_id;

create materialized view public.mv_team_metrics as
select
  mp.team_id,
  m.tournament_id,
  count(distinct mp.match_id)::integer as matches_played,
  count(distinct mp.match_id) filter (where mp.is_winner)::integer as wins,
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
    (count(distinct mp.match_id) filter (where mp.is_winner)::numeric / nullif(count(distinct mp.match_id), 0)) * 100,
    2
  ) as win_rate,
  now() as refreshed_at
from public.match_players mp
join public.matches m on m.id = mp.match_id
where mp.team_id is not null
group by mp.team_id, m.tournament_id;

create materialized view public.mv_hero_metrics as
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
  ) as avg_kda,
  now() as refreshed_at
from public.match_players mp
join public.matches m on m.id = mp.match_id
where mp.hero_id is not null
group by mp.hero_id, m.tournament_id;

create index mv_player_metrics_profile_idx on public.mv_player_metrics(profile_id);
create index mv_team_metrics_team_idx on public.mv_team_metrics(team_id);
create index mv_hero_metrics_hero_idx on public.mv_hero_metrics(hero_id);

create or replace function public.refresh_dotaops_analytics()
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  refresh materialized view public.mv_player_metrics;
  refresh materialized view public.mv_team_metrics;
  refresh materialized view public.mv_hero_metrics;
end;
$$;

create or replace view public.public_match_import_status as
select
  mi.id,
  mi.match_id,
  mi.dota_match_id,
  mi.status,
  mi.created_at,
  mi.updated_at
from public.match_imports mi
join public.matches m on m.id = mi.match_id
join public.tournaments t on t.id = m.tournament_id
where t.is_public;

alter table public.profiles enable row level security;
alter table public.teams enable row level security;
alter table public.team_members enable row level security;
alter table public.tournaments enable row level security;
alter table public.tournament_registrations enable row level security;
alter table public.matches enable row level security;
alter table public.match_imports enable row level security;
alter table public.heroes enable row level security;
alter table public.match_players enable row level security;

create policy "profiles are publicly readable"
on public.profiles for select
using (true);

create policy "users create own profile"
on public.profiles for insert
to authenticated
with check (auth_user_id = auth.uid());

create policy "users update own profile"
on public.profiles for update
to authenticated
using (auth_user_id = auth.uid() or public.is_organizer_or_admin())
with check (auth_user_id = auth.uid() or public.is_organizer_or_admin());

create policy "teams are publicly readable"
on public.teams for select
using (true);

create policy "authenticated users create teams"
on public.teams for insert
to authenticated
with check (created_by = auth.uid() or public.is_organizer_or_admin());

create policy "captains and organizers update teams"
on public.teams for update
to authenticated
using (public.is_team_captain(id) or public.is_organizer_or_admin())
with check (public.is_team_captain(id) or public.is_organizer_or_admin());

create policy "team members are publicly readable"
on public.team_members for select
using (true);

create policy "captains manage team members"
on public.team_members for all
to authenticated
using (public.is_team_captain(team_id) or public.is_organizer_or_admin())
with check (public.is_team_captain(team_id) or public.is_organizer_or_admin());

create policy "public tournaments are readable"
on public.tournaments for select
using (
  is_public
  or public.can_manage_tournament(id)
);

create policy "organizers create tournaments"
on public.tournaments for insert
to authenticated
with check (public.is_organizer_or_admin());

create policy "organizers update tournaments"
on public.tournaments for update
to authenticated
using (public.can_manage_tournament(id))
with check (public.can_manage_tournament(id));

create policy "approved registrations are readable"
on public.tournament_registrations for select
using (
  status = 'approved'
  or public.is_team_captain(team_id)
  or public.can_manage_tournament(tournament_id)
);

create policy "captains create registrations"
on public.tournament_registrations for insert
to authenticated
with check (public.is_team_captain(team_id) or public.is_organizer_or_admin());

create policy "organizers update registrations"
on public.tournament_registrations for update
to authenticated
using (public.can_manage_tournament(tournament_id))
with check (public.can_manage_tournament(tournament_id));

create policy "public matches are readable"
on public.matches for select
using (
  exists (
    select 1
    from public.tournaments t
    where t.id = matches.tournament_id
      and t.is_public
  )
);

create policy "organizers manage matches"
on public.matches for all
to authenticated
using (public.can_manage_tournament(tournament_id))
with check (public.can_manage_tournament(tournament_id));

create policy "match imports are readable for public matches"
on public.match_imports for select
using (
  match_id is null
  or exists (
    select 1
    from public.matches m
    join public.tournaments t on t.id = m.tournament_id
    where m.id = match_imports.match_id
      and t.is_public
  )
);

create policy "authenticated users request imports"
on public.match_imports for insert
to authenticated
with check (
  requested_by = public.current_profile_id()
  or public.is_organizer_or_admin()
);

create policy "organizers update imports"
on public.match_imports for update
to authenticated
using (
  public.is_organizer_or_admin()
  or exists (
    select 1
    from public.matches m
    where m.id = match_imports.match_id
      and public.can_manage_tournament(m.tournament_id)
  )
)
with check (
  public.is_organizer_or_admin()
  or exists (
    select 1
    from public.matches m
    where m.id = match_imports.match_id
      and public.can_manage_tournament(m.tournament_id)
  )
);

create policy "heroes are publicly readable"
on public.heroes for select
using (true);

create policy "organizers manage heroes"
on public.heroes for all
to authenticated
using (public.is_organizer_or_admin())
with check (public.is_organizer_or_admin());

create policy "match players are readable for public matches"
on public.match_players for select
using (
  exists (
    select 1
    from public.matches m
    join public.tournaments t on t.id = m.tournament_id
    where m.id = match_players.match_id
      and t.is_public
  )
);

create policy "organizers manage match players"
on public.match_players for all
to authenticated
using (
  public.is_organizer_or_admin()
  or exists (
    select 1
    from public.matches m
    where m.id = match_players.match_id
      and public.can_manage_tournament(m.tournament_id)
  )
)
with check (
  public.is_organizer_or_admin()
  or exists (
    select 1
    from public.matches m
    where m.id = match_players.match_id
      and public.can_manage_tournament(m.tournament_id)
  )
);

grant usage on schema public to anon, authenticated, service_role;
grant select on
  public.profiles,
  public.teams,
  public.team_members,
  public.tournaments,
  public.tournament_registrations,
  public.matches,
  public.heroes
to anon, authenticated;
grant select on public.match_imports, public.match_players to authenticated;
grant insert, update, delete on
  public.profiles,
  public.teams,
  public.team_members,
  public.tournaments,
  public.tournament_registrations,
  public.matches,
  public.match_imports,
  public.heroes,
  public.match_players
to authenticated;
grant all on
  public.profiles,
  public.teams,
  public.team_members,
  public.tournaments,
  public.tournament_registrations,
  public.matches,
  public.match_imports,
  public.heroes,
  public.match_players
to service_role;
grant select on public.public_match_import_status to anon, authenticated, service_role;
grant select on public.mv_player_metrics to anon, authenticated, service_role;
grant select on public.mv_team_metrics to anon, authenticated, service_role;
grant select on public.mv_hero_metrics to anon, authenticated, service_role;
grant execute on function public.current_profile_id() to anon, authenticated;
grant execute on function public.is_organizer_or_admin() to anon, authenticated;
grant execute on function public.is_team_captain(uuid) to anon, authenticated;
grant execute on function public.can_manage_tournament(uuid) to anon, authenticated;
grant execute on function public.refresh_dotaops_analytics() to authenticated, service_role;
