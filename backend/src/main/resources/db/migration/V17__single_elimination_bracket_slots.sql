alter table public.match_slots
  add column if not exists team_id uuid references public.teams(id) on delete set null;

create index if not exists match_slots_team_idx
  on public.match_slots(team_id);

alter table public.matches drop constraint if exists matches_round_number_positive;
alter table public.matches add constraint matches_round_number_positive
  check (round_number > 0) not valid;

alter table public.matches drop constraint if exists matches_bracket_position_positive;
alter table public.matches add constraint matches_bracket_position_positive
  check (bracket_position is null or bracket_position > 0) not valid;

comment on column public.match_slots.team_id is 'Team assigned to this slot when the source is a known seed or manual assignment.';
