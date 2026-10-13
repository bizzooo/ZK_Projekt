alter table public.matches
  add column if not exists cancelled_at timestamptz,
  add column if not exists cancellation_reason text;

alter table public.matches drop constraint if exists matches_cancellation_reason_length;
alter table public.matches add constraint matches_cancellation_reason_length
  check (cancellation_reason is null or char_length(cancellation_reason) <= 500) not valid;

alter table public.matches drop constraint if exists matches_cancelled_at_status;
alter table public.matches add constraint matches_cancelled_at_status
  check (cancelled_at is null or status = 'cancelled'::public.dotaops_match_status) not valid;

create index if not exists matches_status_idx
  on public.matches(status);

comment on column public.matches.cancelled_at is 'Timestamp set when a scheduled or live tournament match is cancelled.';
comment on column public.matches.cancellation_reason is 'Organizer/referee-facing reason for cancelling a tournament match.';
