-- BE/DB-13 is deferred for Iteration 2.
-- Keep owner/organizer as the only operative tournament staff roles for writes.
-- Referee and analyst enum values remain for future iterations but must not grant write access yet.

create or replace function private.can_officiate_tournament(target_tournament_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select private.can_manage_tournament(target_tournament_id)
$$;

comment on function private.can_officiate_tournament(uuid) is
  'Iteration 2 defers referee/analyst permissions. Officiating write access is currently equivalent to owner/organizer tournament management.';

drop policy if exists "authenticated users request imports" on public.match_imports;

create policy "organizers request imports"
on public.match_imports for insert
to authenticated
with check (
  (select private.is_organizer_or_admin())
  or (
    match_id is not null
    and private.can_officiate_match(match_id)
  )
  or (
    match_game_id is not null
    and private.can_officiate_match_game(match_game_id)
  )
);

comment on policy "organizers request imports"
on public.match_imports
is 'Iteration 2 limits import writes to organizers/admins or tournament owners/organizers through match scope.';
