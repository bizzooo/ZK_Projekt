-- Let active team members read their team's tournament registration status
-- without broadening registration write permissions.

create or replace function private.is_active_team_member(target_team_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1
    from public.team_members tm
    where tm.team_id = target_team_id
      and tm.profile_id = private.current_profile_id()
      and tm.is_active = true
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
        or private.is_active_team_member(tr.team_id)
        or (
          tr.status = 'approved'
          and private.can_read_tournament(tr.tournament_id)
        )
      )
  )
$$;

drop policy if exists "registrations are readable when relevant" on public.tournament_registrations;

create policy "registrations are readable when relevant"
on public.tournament_registrations for select
to anon, authenticated
using (
  private.can_manage_tournament(tournament_id)
  or private.is_team_captain(team_id)
  or private.is_active_team_member(team_id)
  or (
    status = 'approved'::public.dotaops_registration_status
    and private.can_read_tournament(tournament_id)
  )
);

comment on policy "registrations are readable when relevant"
on public.tournament_registrations
is 'Organizers, team captains, and active team members can read relevant registration statuses; approved public registrations remain public-safe.';
