drop view if exists public.v_group_standings;

create view public.v_group_standings
with (security_invoker = true)
as
with team_match_results as (
  select
    tg.id as group_id,
    tg.tournament_id,
    tgt.team_id,
    tm.name as team_name,
    m.id as match_id,
    case
      when m.status = 'finished'::public.dotaops_match_status
        and (
          (m.team_a_id = tgt.team_id and m.winner_team_id = tgt.team_id)
          or (m.team_b_id = tgt.team_id and m.winner_team_id = tgt.team_id)
          or (
            m.winner_team_id is null
            and (
              (m.team_a_id = tgt.team_id and m.score_a > m.score_b)
              or (m.team_b_id = tgt.team_id and m.score_b > m.score_a)
            )
          )
        )
        then 'win'
      when m.status = 'finished'::public.dotaops_match_status
        and m.score_a = m.score_b
        and m.winner_team_id is null
        then 'draw'
      when m.status = 'finished'::public.dotaops_match_status
        and (
          (m.team_a_id = tgt.team_id and m.team_b_id is not null)
          or (m.team_b_id = tgt.team_id and m.team_a_id is not null)
        )
        then 'loss'
      else null
    end as match_result,
    case
      when m.status <> 'finished'::public.dotaops_match_status then 0
      when m.team_a_id = tgt.team_id then m.score_a
      when m.team_b_id = tgt.team_id then m.score_b
      else 0
    end as game_wins,
    case
      when m.status <> 'finished'::public.dotaops_match_status then 0
      when m.team_a_id = tgt.team_id then m.score_b
      when m.team_b_id = tgt.team_id then m.score_a
      else 0
    end as game_losses
  from public.tournament_groups tg
  join public.tournaments t on t.id = tg.tournament_id
  join public.tournament_group_teams tgt on tgt.group_id = tg.id
  join public.teams tm on tm.id = tgt.team_id
  left join public.matches m
    on m.group_id = tg.id
    and m.tournament_id = tg.tournament_id
    and (m.team_a_id = tgt.team_id or m.team_b_id = tgt.team_id)
  where t.is_public
    and t.status in ('registration', 'published', 'live', 'finished')
),
standings as (
  select
    group_id,
    tournament_id,
    team_id,
    team_name,
    count(match_id) filter (where match_result is not null)::integer as matches_played,
    count(match_id) filter (where match_result = 'win')::integer as match_wins,
    count(match_id) filter (where match_result = 'loss')::integer as match_losses,
    count(match_id) filter (where match_result = 'draw')::integer as match_draws,
    coalesce(sum(game_wins), 0)::integer as game_wins,
    coalesce(sum(game_losses), 0)::integer as game_losses
  from team_match_results
  group by group_id, tournament_id, team_id, team_name
)
select
  group_id,
  tournament_id,
  team_id,
  team_name,
  matches_played,
  match_wins,
  match_losses,
  match_draws,
  game_wins,
  game_losses,
  (game_wins - game_losses)::integer as game_diff,
  ((match_wins * 3) + match_draws)::integer as points,
  (row_number() over (
    partition by group_id
    order by
      ((match_wins * 3) + match_draws) desc,
      match_wins desc,
      (game_wins - game_losses) desc,
      game_wins desc,
      team_name asc
  ))::integer as rank
from standings;

create or replace function public.validate_tournament_group_team_assignment()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  target_tournament_id uuid;
  registration_tournament_id uuid;
  registration_team_id uuid;
  registration_status public.dotaops_registration_status;
begin
  select tg.tournament_id
  into target_tournament_id
  from public.tournament_groups tg
  where tg.id = new.group_id;

  if target_tournament_id is null then
    return new;
  end if;

  if new.registration_id is not null then
    select tr.tournament_id, tr.team_id, tr.status
    into registration_tournament_id, registration_team_id, registration_status
    from public.tournament_registrations tr
    where tr.id = new.registration_id;

    if registration_tournament_id is distinct from target_tournament_id
        or registration_team_id is distinct from new.team_id then
      raise exception 'Tournament group team registration must belong to the same tournament and team.'
        using errcode = '23514',
              constraint = 'tournament_group_teams_registration_matches_group';
    end if;

    if registration_status <> 'approved'::public.dotaops_registration_status then
      raise exception 'Tournament group team registration must be approved.'
        using errcode = '23514',
              constraint = 'tournament_group_teams_registration_approved';
    end if;
  end if;

  if exists (
    select 1
    from public.tournament_group_teams existing
    join public.tournament_groups existing_group on existing_group.id = existing.group_id
    where existing.team_id = new.team_id
      and existing_group.tournament_id = target_tournament_id
      and existing.id is distinct from new.id
  ) then
    raise exception 'Team is already assigned to a group in this tournament.'
      using errcode = '23505',
            constraint = 'tournament_group_teams_one_group_per_tournament';
  end if;

  return new;
end;
$$;

drop trigger if exists tournament_group_teams_validate_assignment on public.tournament_group_teams;
create trigger tournament_group_teams_validate_assignment
before insert or update on public.tournament_group_teams
for each row execute function public.validate_tournament_group_team_assignment();

grant select on public.v_group_standings to anon, authenticated, service_role;

comment on view public.v_group_standings is 'Calculated Dota 2 group-stage standings using tournament match and Dota game terminology.';
comment on function public.validate_tournament_group_team_assignment() is 'Validates that a tournament group team assignment uses an approved registration and only one group per tournament.';
comment on table public.match_games is 'Individual Dota 2 games inside a tournament match series.';
