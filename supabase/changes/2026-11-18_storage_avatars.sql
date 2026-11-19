-- =============================================================================
-- Cambio: perfil-configuracion — bucket de fotos de perfil (HU-005 / HU-009)
-- Fecha : 2026-11-18
-- Autor : David Chicaiza (Groovy Cow)
--
-- Qué hace
--   1. Crea (o actualiza) el bucket público `avatars`: máximo 2 MB (2097152 bytes)
--      y solo `image/jpeg`.
--   2. Políticas sobre `storage.objects`: lectura pública del bucket; insertar y
--      actualizar únicamente dentro de la carpeta propia (`{uid}/foto.jpg`).
--
-- Idempotencia: se puede ejecutar más de una vez (on conflict / drop policy if exists).
-- Reversa: ver `2026-11-18_perfil_rollback.sql`.
-- Ejecución: manual en el SQL editor de Supabase (no se usa Supabase CLI).
-- =============================================================================

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('avatars', 'avatars', true, 2097152, array['image/jpeg'])
on conflict (id) do update
  set public = true,
      file_size_limit = 2097152,
      allowed_mime_types = array['image/jpeg'];

drop policy if exists "avatars_select_public" on storage.objects;
create policy "avatars_select_public" on storage.objects
  for select using (bucket_id = 'avatars');

drop policy if exists "avatars_insert_own_folder" on storage.objects;
create policy "avatars_insert_own_folder" on storage.objects
  for insert to authenticated
  with check (bucket_id = 'avatars' and (storage.foldername(name))[1] = auth.uid()::text);

drop policy if exists "avatars_update_own_folder" on storage.objects;
create policy "avatars_update_own_folder" on storage.objects
  for update to authenticated
  using (bucket_id = 'avatars' and (storage.foldername(name))[1] = auth.uid()::text)
  with check (bucket_id = 'avatars' and (storage.foldername(name))[1] = auth.uid()::text);
