alter table public.profiles
  add column if not exists opendota_account_id bigint,
  add column if not exists steam_profile_synced_at timestamptz,
  add column if not exists opendota_profile_synced_at timestamptz,
  add column if not exists opendota_last_failure_at timestamptz;

alter table public.profiles drop constraint if exists profiles_opendota_account_id_range;
alter table public.profiles add constraint profiles_opendota_account_id_range
  check (
    opendota_account_id is null
    or (opendota_account_id between 0 and 4294967295)
  );

create unique index if not exists profiles_opendota_account_id_unique_idx
  on public.profiles(opendota_account_id)
  where opendota_account_id is not null;

comment on column public.profiles.opendota_account_id is
  'Dota/OpenDota 32-bit account_id derived from the primary SteamID64.';
comment on column public.profiles.steam_profile_synced_at is
  'Last successful backend Steam profile bootstrap timestamp.';
comment on column public.profiles.opendota_profile_synced_at is
  'Last successful backend OpenDota player profile bootstrap timestamp.';
comment on column public.profiles.opendota_last_failure_at is
  'Last failed backend OpenDota bootstrap attempt timestamp. Raw external responses are not stored here.';
