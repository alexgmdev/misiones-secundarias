-- =============================================================================
-- Cambio: perfil-configuracion — campos públicos del perfil (HU-005..HU-009)
-- Fecha : 2026-11-18
-- Autor : David Chicaiza (Groovy Cow)
--
-- Qué hace
--   1. Agrega a `profiles`: nickname, gender, description, avatar_id y photo_url
--      con sus CHECK (formato, valores permitidos, longitud y origen de la foto).
--   2. Agrega `profile_completed`, columna generada: verdadera cuando nickname,
--      gender, avatar_id y photo_url no son nulos (la descripción es opcional).
--   3. Crea el índice único `lower(nickname)` (unicidad sin distinguir mayúsculas).
--   4. Concede UPDATE por columna a `authenticated` sobre los campos editables
--      (`full_name`, `role` siguen sin poder modificarse desde la API).
--   5. Crea la RPC `nickname_disponible(p_nickname)`: excluye al usuario actual.
--
-- Idempotencia: se puede ejecutar más de una vez (if not exists / drop if exists).
-- ADVERTENCIA: los usuarios existentes quedan con perfil incompleto y pasarán por
--   la pantalla "Crea tu perfil" al ingresar (esperado). Coordinar con Juan.
-- Reversa: ver `2026-11-18_perfil_rollback.sql`.
-- Ejecución: manual en el SQL editor de Supabase (no se usa Supabase CLI).
-- =============================================================================

begin;

alter table public.profiles add column if not exists nickname text;
alter table public.profiles add column if not exists gender text;
alter table public.profiles add column if not exists description text;
alter table public.profiles add column if not exists avatar_id text;
alter table public.profiles add column if not exists photo_url text;

alter table public.profiles drop constraint if exists profiles_nickname_format;
alter table public.profiles add constraint profiles_nickname_format
  check (nickname is null or nickname ~ '^[A-Za-z0-9_]{3,20}$');

alter table public.profiles drop constraint if exists profiles_gender_values;
alter table public.profiles add constraint profiles_gender_values
  check (gender is null or gender in ('femenino', 'masculino', 'prefiero_no_decir'));

alter table public.profiles drop constraint if exists profiles_description_length;
alter table public.profiles add constraint profiles_description_length
  check (description is null or char_length(description) <= 280);

alter table public.profiles drop constraint if exists profiles_avatar_id_format;
alter table public.profiles add constraint profiles_avatar_id_format
  check (avatar_id is null or avatar_id ~ '^[a-z_]{1,30}$');

-- La foto solo puede apuntar al bucket `avatars`, a la carpeta del propio usuario.
alter table public.profiles drop constraint if exists profiles_photo_url_origin;
alter table public.profiles add constraint profiles_photo_url_origin
  check (photo_url is null or position('/storage/v1/object/public/avatars/' || id::text || '/' in photo_url) > 0);

-- Columna generada: no puede desincronizarse ni escribirse desde la API.
alter table public.profiles add column if not exists profile_completed boolean
  generated always as (
    nickname is not null and gender is not null and avatar_id is not null and photo_url is not null
  ) stored;

create unique index if not exists profiles_nickname_lower_key on public.profiles (lower(nickname));

-- Mínimo privilegio: solo estas columnas son editables por el propio usuario (RLS "update own").
grant update (nickname, gender, description, avatar_id, photo_url) on public.profiles to authenticated;

-- Indica si un nickname está libre (true) para el usuario actual; su propio nickname cuenta como libre.
create or replace function public.nickname_disponible(p_nickname text)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select not exists (
    select 1 from public.profiles
    where lower(nickname) = lower(p_nickname)
      and id is distinct from auth.uid()
  );
$$;
comment on function public.nickname_disponible(text) is 'Indica si un nickname está libre (true); excluye al usuario actual.';
revoke execute on function public.nickname_disponible(text) from public, anon;
grant execute on function public.nickname_disponible(text) to authenticated;

commit;

notify pgrst, 'reload schema';
