-- =============================================================================
-- Reversa de: perfil-configuracion (2026-11-18_profiles_public_fields.sql y
--             2026-11-18_storage_avatars.sql)
-- Autor : David Chicaiza (Groovy Cow)
--
-- Quita la RPC, los grants, el índice, las columnas, las políticas y el bucket.
-- NOTA: Supabase no permite borrar un bucket con objetos; vaciar `avatars` ANTES
-- (desde el dashboard de Storage o con la API) o el último paso fallará.
-- ADVERTENCIA: se pierden nickname, género, descripción, avatar y foto de todos
-- los usuarios.
-- Idempotente. Ejecución manual en el SQL editor de Supabase.
-- =============================================================================

begin;

-- 1. Storage: políticas y bucket
drop policy if exists "avatars_select_public" on storage.objects;
drop policy if exists "avatars_insert_own_folder" on storage.objects;
drop policy if exists "avatars_update_own_folder" on storage.objects;
delete from storage.buckets where id = 'avatars';

-- 2. RPC
drop function if exists public.nickname_disponible(text);

-- 3. Grants por columna
revoke update (nickname, gender, description, avatar_id, photo_url) on public.profiles from authenticated;

-- 4. Índice, columna generada y columnas (los CHECK caen con ellas)
drop index if exists public.profiles_nickname_lower_key;
alter table public.profiles drop column if exists profile_completed;
alter table public.profiles drop column if exists photo_url;
alter table public.profiles drop column if exists avatar_id;
alter table public.profiles drop column if exists description;
alter table public.profiles drop column if exists gender;
alter table public.profiles drop column if exists nickname;

commit;

notify pgrst, 'reload schema';
