alter table public.match_slots
  add column if not exists is_locked boolean not null default false;

create index if not exists match_slots_source_match_type_idx
  on public.match_slots(source_match_id, source_type)
  where source_match_id is not null;

create index if not exists match_slots_locked_idx
  on public.match_slots(is_locked)
  where is_locked;

create table if not exists public.match_advancement_audit_logs (
  id uuid primary key default gen_random_uuid(),
  tournament_id uuid not null references public.tournaments(id) on delete cascade,
  source_match_id uuid not null references public.matches(id) on delete cascade,
  target_match_id uuid not null references public.matches(id) on delete cascade,
  target_slot public.dotaops_match_slot not null,
  source_type public.dotaops_match_slot_source not null,
  advanced_team_id uuid references public.teams(id) on delete set null,
  previous_team_id uuid references public.teams(id) on delete set null,
  reason text not null,
  message text not null,
  created_by uuid references public.profiles(id) on delete set null,
  created_at timestamptz not null default now(),
  constraint match_advancement_audit_source_type check (source_type in ('winner', 'loser')),
  constraint match_advancement_audit_reason_length check (char_length(reason) between 1 and 80),
  constraint match_advancement_audit_message_length check (char_length(message) between 1 and 500)
);

alter table public.match_advancement_audit_logs enable row level security;

create index if not exists match_advancement_audit_tournament_idx
  on public.match_advancement_audit_logs(tournament_id, created_at desc);

create index if not exists match_advancement_audit_source_match_idx
  on public.match_advancement_audit_logs(source_match_id, created_at desc);

create index if not exists match_advancement_audit_target_match_idx
  on public.match_advancement_audit_logs(target_match_id, created_at desc);

drop policy if exists "admins read match advancement audit logs" on public.match_advancement_audit_logs;
create policy "admins read match advancement audit logs"
on public.match_advancement_audit_logs for select
to authenticated
using ((select private.is_admin()));

drop policy if exists "officials insert match advancement audit logs" on public.match_advancement_audit_logs;
create policy "officials insert match advancement audit logs"
on public.match_advancement_audit_logs for insert
to authenticated
with check ((select private.is_organizer_or_admin()));

revoke all on public.match_advancement_audit_logs from anon;
grant select, insert on public.match_advancement_audit_logs to authenticated;
revoke update, delete on public.match_advancement_audit_logs from authenticated;
grant all on public.match_advancement_audit_logs to service_role;

comment on column public.match_slots.is_locked is 'Prevents automatic bracket advancement from overwriting this match slot.';
comment on table public.match_advancement_audit_logs is 'Append-only audit trail for automatic winner and loser advancement between bracket matches.';
