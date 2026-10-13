-- Run after Flyway migrations, not inside Flyway.
-- Flyway locks public.flyway_schema_history while executing migrations, so
-- changing this table from a Flyway migration can hit statement timeouts.

revoke all on table public.flyway_schema_history from anon, authenticated;
grant all on table public.flyway_schema_history to service_role;
alter table public.flyway_schema_history enable row level security;
