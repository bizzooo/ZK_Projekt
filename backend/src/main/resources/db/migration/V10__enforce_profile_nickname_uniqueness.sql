do $$
begin
  if exists (
    select 1
    from public.profiles
    group by lower(nickname)
    having count(*) > 1
  ) then
    raise exception 'Cannot enforce unique profile nicknames: duplicate nicknames already exist case-insensitively.';
  end if;
end $$;

create unique index if not exists profiles_nickname_ci_unique_idx
  on public.profiles (lower(nickname));

comment on index public.profiles_nickname_ci_unique_idx is
  'Enforces case-insensitive uniqueness for public profile nickname lookups.';
