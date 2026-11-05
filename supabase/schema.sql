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
    full_name TEXT NOT NULL,
    phone TEXT UNIQUE,
    avatar_url TEXT,
    role TEXT DEFAULT 'user' CHECK (role IN ('user', 'moderator')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

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
    max_participants INTEGER DEFAULT 2,
    status TEXT DEFAULT 'Publicada' CHECK (status IN ('Borrador', 'Publicada', 'Asignada', 'En progreso', 'Completada', 'Cancelada', 'Reportada', 'Suspendida')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,

    CONSTRAINT check_location_by_modality CHECK (
        (modality = 'campo' AND location IS NOT NULL) OR
        (modality = 'remota' AND location IS NULL)
    )
);

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

-- Perfiles
CREATE POLICY "Public profiles viewable" ON profiles FOR SELECT USING (true);
CREATE POLICY "Users can insert own profile" ON profiles FOR INSERT WITH CHECK (auth.uid() = id);
CREATE POLICY "Users can update own profile" ON profiles FOR UPDATE USING (auth.uid() = id);

-- Misiones
CREATE POLICY "Published missions viewable" ON missions FOR SELECT USING (status != 'Borrador');
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


--reglas para la tabla de profiles
GRANT SELECT ON public.profiles TO anon, authenticated;
GRANT SELECT ON public.applications TO anon, authenticated;
-- ==========================================
-- 4. TRIGGER PARA AUTO-CREAR PERFILES (David - Auth)
-- ==========================================
-- Esta función se ejecuta cuando Supabase Auth registra un nuevo usuario
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

-- El trigger en sí
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute procedure public.handle_new_user();
