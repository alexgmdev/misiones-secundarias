-- ==========================================
-- 1. EXTENSIONES
-- ==========================================
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS postgis;

-- ==========================================
-- 2. TABLAS
-- ==========================================
-- Tabla de Perfiles
CREATE TABLE profiles (
    id UUID REFERENCES auth.users(id) ON DELETE CASCADE PRIMARY KEY,
    full_name TEXT NOT NULL, -- derivado: first_name || ' ' || last_name (lo escribe el trigger)
    first_name TEXT CONSTRAINT profiles_first_name_length CHECK (first_name IS NULL OR char_length(btrim(first_name)) BETWEEN 1 AND 60),
    last_name TEXT CONSTRAINT profiles_last_name_length CHECK (last_name IS NULL OR char_length(btrim(last_name)) BETWEEN 1 AND 60),
    avatar_url TEXT,
    role TEXT DEFAULT 'user' CHECK (role IN ('user', 'moderator')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- Datos personales del perfil (celular y fecha de nacimiento): solo visibles para su dueño.
-- Cambio: supabase/changes/2026-11-03_auth_profiles_privacy.sql
CREATE TABLE profile_private (
    id UUID REFERENCES profiles(id) ON DELETE CASCADE PRIMARY KEY,
    phone TEXT NOT NULL UNIQUE CONSTRAINT profile_private_phone_format CHECK (phone ~ '^3[0-9]{9}$'),
    birth_date DATE NOT NULL CONSTRAINT profile_private_adult CHECK (birth_date <= (current_date - interval '18 years')::date),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL
);
COMMENT ON TABLE profile_private IS 'Datos personales (celular, fecha de nacimiento). RLS: solo el dueño lee; solo el trigger handle_new_user escribe.';

-- Intentos fallidos de login por celular (Edge Function login-telefono; solo service role).
-- Cambio: supabase/changes/2026-11-03_login_attempts.sql
CREATE TABLE login_attempts (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    phone TEXT NOT NULL,
    client_ip INET,
    attempted_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL
);
COMMENT ON TABLE login_attempts IS 'Intentos fallidos de login por celular. Solo service role.';
CREATE INDEX login_attempts_phone_idx ON login_attempts (phone, attempted_at DESC);
CREATE INDEX login_attempts_ip_idx ON login_attempts (client_ip, attempted_at DESC);

-- Tabla de Misiones
CREATE TABLE missions (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    publisher_id UUID REFERENCES profiles(id) ON DELETE CASCADE NOT NULL,
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    category TEXT NULL,
    requirements TEXT NULL,
    modality TEXT CHECK (modality IN ('campo', 'remota')) NOT NULL,
    date_time TIMESTAMP WITH TIME ZONE NOT NULL,
    location GEOGRAPHY(POINT) NULL,
    meeting_point TEXT NULL,
    max_participants INTEGER DEFAULT 2,
    status TEXT DEFAULT 'Publicada' CHECK (status IN ('Borrador', 'Publicada', 'Asignada', 'En progreso', 'Completada', 'Cancelada', 'Reportada', 'Suspendida')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,

    CONSTRAINT check_location_by_modality CHECK (
        (modality = 'campo' AND location IS NOT NULL) OR
        (modality = 'remota' AND location IS NULL)
    )
);
COMMENT ON COLUMN missions.meeting_point IS 'Punto de encuentro textual (solo misiones de campo)';

-- Tabla de Postulaciones
CREATE TABLE applications (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    mission_id UUID REFERENCES missions(id) ON DELETE CASCADE NOT NULL,
    solver_id UUID REFERENCES profiles(id) ON DELETE CASCADE NOT NULL,
    status TEXT DEFAULT 'Postulado' CHECK (status IN ('Postulado', 'Aceptado', 'Rechazado')),
    applied_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    CONSTRAINT unique_application UNIQUE (mission_id, solver_id)
);

-- ==========================================
-- 3. SEGURIDAD (RLS Y POLÍTICAS)
-- ==========================================
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE missions ENABLE ROW LEVEL SECURITY;
ALTER TABLE applications ENABLE ROW LEVEL SECURITY;
ALTER TABLE profile_private ENABLE ROW LEVEL SECURITY;
ALTER TABLE login_attempts ENABLE ROW LEVEL SECURITY; -- sin políticas: solo service role

-- Perfiles
CREATE POLICY "Public profiles viewable" ON profiles FOR SELECT USING (true);
-- (Sin política de INSERT: los perfiles solo los crea el trigger handle_new_user)
CREATE POLICY "Users can update own profile" ON profiles FOR UPDATE USING (auth.uid() = id);

-- Datos privados: el dueño solo lee su propia fila
CREATE POLICY "Owner can read own private data" ON profile_private FOR SELECT TO authenticated USING (auth.uid() = id);

-- Misiones
CREATE POLICY "Published missions viewable" ON missions FOR SELECT USING (status != 'Borrador');
CREATE POLICY "Publishers can view own drafts" ON missions FOR SELECT TO authenticated USING (auth.uid() = publisher_id AND status = 'Borrador');
CREATE POLICY "Publishers can insert missions" ON missions FOR INSERT WITH CHECK (auth.uid() = publisher_id);
CREATE POLICY "Publishers can update missions" ON missions FOR UPDATE USING (auth.uid() = publisher_id);

-- Postulaciones
CREATE POLICY "View applications based on role" ON applications FOR SELECT USING (auth.uid() = solver_id OR auth.uid() IN (SELECT publisher_id FROM missions WHERE id = mission_id));
CREATE POLICY "Solvers can apply" ON applications FOR INSERT WITH CHECK (auth.uid() = solver_id);
CREATE POLICY "Solvers can delete own applications" ON applications FOR DELETE USING (auth.uid() = solver_id);
CREATE POLICY "Publishers can update application status" ON applications FOR UPDATE USING (auth.uid() IN (SELECT publisher_id FROM missions WHERE id = mission_id));


-- configuración para evitar problemas de permisos
-- Permisos explícitos para la Data API (necesario desde Nov 2026)
GRANT SELECT ON public.missions TO anon, authenticated;
GRANT SELECT ON public.profiles TO anon, authenticated;
GRANT SELECT ON public.applications TO anon, authenticated;
GRANT INSERT ON public.applications TO authenticated;
GRANT DELETE ON public.applications TO authenticated;
GRANT UPDATE (status) ON public.applications TO authenticated;

-- Datos privados y intentos de login
REVOKE ALL ON public.profile_private FROM anon, authenticated;
GRANT SELECT ON public.profile_private TO authenticated;
REVOKE ALL ON public.login_attempts FROM anon, authenticated;


--reglas para la tabla de profiles
GRANT SELECT ON public.profiles TO anon, authenticated;
GRANT SELECT ON public.applications TO anon, authenticated;
-- ==========================================
-- 4. TRIGGER PARA AUTO-CREAR PERFILES (David - Auth)
-- ==========================================
-- Esta función se ejecuta cuando Supabase Auth registra un nuevo usuario.
-- Inserta de forma atómica en profiles y profile_private (cualquier fallo aborta el signUp).
-- Lee first_name, last_name, phone y birth_date (ISO) de raw_user_meta_data.
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

-- El trigger en sí
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute procedure public.handle_new_user();

-- ==========================================
-- 5. FUNCIONES AUXILIARES DE AUTH (HU-001 / HU-002)
-- ==========================================
-- Indica si un celular normalizado está libre (true) o ya registrado (false).
create or replace function public.telefono_disponible(p_telefono text)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select not exists (select 1 from public.profile_private where phone = p_telefono);
$$;
comment on function public.telefono_disponible(text) is 'Indica si un celular está libre (true) o ya registrado (false).';
revoke execute on function public.telefono_disponible(text) from public;
grant execute on function public.telefono_disponible(text) to anon, authenticated;

-- Borra intentos de login de más de 1 día (invocada por la Edge Function login-telefono).
create or replace function public.purge_login_attempts()
returns void
language sql
security definer
set search_path = ''
as $$
  delete from public.login_attempts where attempted_at < now() - interval '1 day';
$$;
comment on function public.purge_login_attempts() is 'Borra intentos de login de más de 1 día. Solo service role.';
revoke execute on function public.purge_login_attempts() from public, anon, authenticated;
