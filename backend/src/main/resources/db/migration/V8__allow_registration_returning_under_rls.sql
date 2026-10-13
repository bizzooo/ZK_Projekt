-- Keep the registration read policy row-local so INSERT ... RETURNING can
-- evaluate it against the inserted row while RLS is active.

drop policy if exists "registrations are readable when relevant" on public.tournament_registrations;

create policy "registrations are readable when relevant"
on public.tournament_registrations for select
to anon, authenticated
using (
  private.can_manage_tournament(tournament_id)
  or private.is_team_captain(team_id)
  or (
    status = 'approved'::public.dotaops_registration_status
    and private.can_read_tournament(tournament_id)
  )
);

comment on policy "registrations are readable when relevant"
on public.tournament_registrations
is 'Uses row columns directly so captains and organizers can INSERT ... RETURNING registrations under RLS.';

-- Rollback note:
-- Drop this policy and recreate the previous variant with
-- using (private.can_read_registration(id)).
