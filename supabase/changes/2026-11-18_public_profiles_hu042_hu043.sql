
-- =============================================================================
-- Cambio: HU-042 / HU-043 — datos públicos del perfil
-- Fecha : 2026-11-18
-- Autor : Groovy Cow
--
-- Qué hace:
--   1. Agrega nickname a profiles.
--   2. Agrega gender a profiles.
--   3. Agrega description a profiles.
--   4. Agrega level a profiles.
--
-- Estos campos se utilizan para consultar los participantes aceptados
-- y visualizar sus perfiles públicos.
--
-- Ejecución: manual en SQL Editor de Supabase.
-- =============================================================================

begin;

alter table public.profiles
  add column if not exists nickname text,
  add column if not exists gender text,
  add column if not exists description text,
  add column if not exists level text;

comment on column public.profiles.nickname is
  'Apodo público del usuario.';

comment on column public.profiles.gender is
  'Género mostrado en el perfil público.';

comment on column public.profiles.description is
  'Descripción pública del usuario.';

comment on column public.profiles.level is
  'Nivel del usuario en la plataforma.';

commit;
