-- =============================================================================
-- Reversa de: auth-registro-login (2026-11-03_auth_profiles_privacy.sql y
--             2026-11-03_login_attempts.sql)
-- Autor : David Chicaiza (Groovy Cow)
--
-- Restablece el esquema previo: `profiles.phone`, trigger original y política de
-- inserción propia. Los celulares se recuperan desde `profile_private` ANTES de
-- eliminarla. `first_name` / `last_name` se conservan (no estorban); si se desea,
-- descomentar el bloque final para quitarlas.
-- Idempotente. Ejecución manual en el SQL editor de Supabase.
-- =============================================================================

begin;

-- 1. Límite de intentos de login
drop function if exists public.purge_login_attempts();
drop table if exists public.login_attempts;

-- 2. Restaurar profiles.phone copiando desde profile_private (si aún existe)
alter table public.profiles add column if not exists phone text unique;

do $$
begin
  if to_regclass('public.profile_private') is not null then
    update public.profiles p
       set phone = pp.phone
      from public.profile_private pp
     where pp.id = p.id and p.phone is null;
  end if;
end;
$$;

-- 3. Restaurar el trigger previo (usa full_name y phone de la metadata)
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer set search_path = public
as $$
begin
  insert into public.profiles (id, full_name, phone, role)
  values (
    new.id,
    new.raw_user_meta_data ->> 'full_name',
    new.raw_user_meta_data ->> 'phone',
    'user'
  );
  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute procedure public.handle_new_user();

-- 4. Restaurar la política de inserción propia
drop policy if exists "Users can insert own profile" on public.profiles;
create policy "Users can insert own profile" on public.profiles
  for insert with check (auth.uid() = id);

-- 5. Eliminar RPC y tabla privada
drop function if exists public.telefono_disponible(text);
drop table if exists public.profile_private;

-- 6. (Opcional) quitar nombre/apellido separados
-- alter table public.profiles drop constraint if exists profiles_first_name_length;
-- alter table public.profiles drop constraint if exists profiles_last_name_length;
-- alter table public.profiles drop column if exists first_name, drop column if exists last_name;

commit;
