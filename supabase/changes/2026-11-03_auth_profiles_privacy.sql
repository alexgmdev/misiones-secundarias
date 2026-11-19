-- =============================================================================
-- Cambio: auth-registro-login — privacidad de perfiles (HU-001)
-- Fecha : 2026-11-03
-- Autor : David Chicaiza (Groovy Cow)
--
-- Qué hace
--   1. Crea `profile_private` (celular y fecha de nacimiento) con RLS de fila
--      propia: ya nadie con la anon key puede leer celulares.
--   2. Agrega `first_name` / `last_name` a `profiles` (nombre y apellido
--      separados); `full_name` se conserva y lo deriva el trigger.
--   3. Reescribe `handle_new_user()` para insertar atómicamente en `profiles`
--      y `profile_private` al registrarse (cualquier fallo aborta el signUp).
--   4. Elimina `profiles.phone` y la política "Users can insert own profile"
--      (solo el trigger inserta perfiles).
--   5. Crea la RPC `telefono_disponible(p_telefono)` para validar unicidad del
--      celular antes de registrar.
--
-- Idempotencia: se puede ejecutar más de una vez (if not exists / drop if exists).
-- ADVERTENCIA: los usuarios de prueba existentes pierden su celular y no tienen
--   fila en `profile_private` (falta `birth_date`): hay que recrearlos desde la app.
--   Coordinar con Alex y Juan antes de correrlo en la BD compartida.
-- Reversa: ver `2026-11-03_auth_rollback.sql`.
-- Ejecución: manual en el SQL editor de Supabase (no se usa Supabase CLI).
-- =============================================================================

begin;

-- -----------------------------------------------------------------------------
-- 1. profile_private: datos personales visibles solo para su dueño
-- -----------------------------------------------------------------------------
create table if not exists public.profile_private (
  id         uuid primary key references public.profiles (id) on delete cascade,
  phone      text not null unique
             constraint profile_private_phone_format check (phone ~ '^3[0-9]{9}$'),
  -- La edad solo se valida en insert/update (correcto: la edad solo crece).
  birth_date date not null
             constraint profile_private_adult check (birth_date <= (current_date - interval '18 years')::date),
  created_at timestamptz not null default now()
);

comment on table public.profile_private is
  'Datos personales (celular, fecha de nacimiento) del perfil. RLS: solo el dueño puede leer; solo el trigger handle_new_user escribe.';
comment on column public.profile_private.phone is
  'Celular colombiano normalizado: 10 dígitos que inician en 3. Único en toda la tabla.';
comment on column public.profile_private.birth_date is
  'Fecha de nacimiento; el usuario debe ser mayor de 18 años al registrarse.';

alter table public.profile_private enable row level security;

-- Política de lectura de fila propia (recreada para ser idempotente)
drop policy if exists "Owner can read own private data" on public.profile_private;
create policy "Owner can read own private data" on public.profile_private
  for select to authenticated
  using (auth.uid() = id);

-- Sin permisos para anon/authenticated salvo SELECT (vía RLS, solo su fila)
revoke all on public.profile_private from anon, authenticated;
grant select on public.profile_private to authenticated;

-- -----------------------------------------------------------------------------
-- 2. profiles: nombre y apellido separados
-- -----------------------------------------------------------------------------
alter table public.profiles
  add column if not exists first_name text,
  add column if not exists last_name  text;

-- Nulos permitidos por las filas legado; si hay valor debe tener 1-60 caracteres
alter table public.profiles drop constraint if exists profiles_first_name_length;
alter table public.profiles add constraint profiles_first_name_length
  check (first_name is null or char_length(btrim(first_name)) between 1 and 60);

alter table public.profiles drop constraint if exists profiles_last_name_length;
alter table public.profiles add constraint profiles_last_name_length
  check (last_name is null or char_length(btrim(last_name)) between 1 and 60);

comment on column public.profiles.first_name is 'Nombre del usuario (1-60 caracteres). Nulo en filas legado.';
comment on column public.profiles.last_name  is 'Apellido del usuario (1-60 caracteres). Nulo en filas legado.';
comment on column public.profiles.full_name  is 'Nombre completo derivado (first_name || '' '' || last_name); lo usa la lista de misiones.';

-- -----------------------------------------------------------------------------
-- 3. Trigger de alta de usuario (se reescribe ANTES de eliminar profiles.phone)
-- -----------------------------------------------------------------------------
-- Lee first_name, last_name, phone y birth_date (ISO) de raw_user_meta_data.
-- security definer + search_path vacío: todos los objetos van calificados.
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  insert into public.profiles (id, first_name, last_name, full_name, role)
  values (
    new.id,
    btrim(new.raw_user_meta_data ->> 'first_name'),
    btrim(new.raw_user_meta_data ->> 'last_name'),
    btrim(new.raw_user_meta_data ->> 'first_name') || ' ' || btrim(new.raw_user_meta_data ->> 'last_name'),
    'user'
  );

  insert into public.profile_private (id, phone, birth_date)
  values (
    new.id,
    new.raw_user_meta_data ->> 'phone',
    (new.raw_user_meta_data ->> 'birth_date')::date
  );

  return new;
end;
$$;

comment on function public.handle_new_user() is
  'Trigger de auth.users: crea profiles y profile_private de forma atómica. Cualquier fallo (celular duplicado, menor de edad, formato) aborta el registro.';

-- El trigger en sí (se recrea para garantizar que apunta a la función vigente)
drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute procedure public.handle_new_user();

-- -----------------------------------------------------------------------------
-- 4. Limpieza: celular fuera de profiles y sin inserción directa de perfiles
-- -----------------------------------------------------------------------------
alter table public.profiles drop column if exists phone;

drop policy if exists "Users can insert own profile" on public.profiles;

-- -----------------------------------------------------------------------------
-- 5. RPC de disponibilidad de celular
-- -----------------------------------------------------------------------------
-- Devuelve true si NO existe otro usuario con ese celular. Se expone a anon porque
-- se consulta antes de crear la cuenta; solo revela un booleano.
create or replace function public.telefono_disponible(p_telefono text)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select not exists (
    select 1 from public.profile_private where phone = p_telefono
  );
$$;

comment on function public.telefono_disponible(text) is
  'Indica si un celular normalizado está libre (true) o ya registrado (false). Ejecutable por anon y authenticated.';

revoke execute on function public.telefono_disponible(text) from public;
grant execute on function public.telefono_disponible(text) to anon, authenticated;

commit;
