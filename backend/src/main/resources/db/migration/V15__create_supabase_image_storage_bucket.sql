-- Images uploaded by the backend are stored in a public Supabase Storage bucket.
-- The block is guarded so plain PostgreSQL test/dev databases without Supabase Storage can still migrate.

do $$
begin
  if to_regclass('storage.buckets') is not null
     and exists (
       select 1
       from information_schema.columns
       where table_schema = 'storage'
         and table_name = 'buckets'
         and column_name = 'public'
     )
     and exists (
       select 1
       from information_schema.columns
       where table_schema = 'storage'
         and table_name = 'buckets'
         and column_name = 'file_size_limit'
     )
     and exists (
       select 1
       from information_schema.columns
       where table_schema = 'storage'
         and table_name = 'buckets'
         and column_name = 'allowed_mime_types'
     ) then
    insert into storage.buckets (
      id,
      name,
      "public",
      file_size_limit,
      allowed_mime_types
    )
    values (
      'dotaops-images',
      'dotaops-images',
      true,
      5242880,
      array['image/png', 'image/jpeg', 'image/webp', 'image/gif']::text[]
    )
    on conflict (id) do update
      set "public" = excluded."public",
          file_size_limit = excluded.file_size_limit,
          allowed_mime_types = excluded.allowed_mime_types;
  end if;
end $$;
