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
    -- Perfil público (cambio: supabase/changes/2026-11-18_profiles_public_fields.sql)
    nickname TEXT CONSTRAINT profiles_nickname_format CHECK (nickname IS NULL OR nickname ~ '^[A-Za-z0-9_]{3,20}$'),
    gender TEXT CONSTRAINT profiles_gender_values CHECK (gender IS NULL OR gender IN ('femenino', 'masculino', 'prefiero_no_decir')),
    description TEXT CONSTRAINT profiles_description_length CHECK (description IS NULL OR char_length(description) <= 280),
    avatar_id TEXT CONSTRAINT profiles_avatar_id_format CHECK (avatar_id IS NULL OR avatar_id ~ '^[a-z_]{1,30}$'),
    photo_url TEXT CONSTRAINT profiles_photo_url_origin CHECK (photo_url IS NULL OR position('/storage/v1/object/public/avatars/' || id::text || '/' in photo_url) > 0),
    profile_completed BOOLEAN GENERATED ALWAYS AS (
        nickname IS NOT NULL AND gender IS NOT NULL AND avatar_id IS NOT NULL AND photo_url IS NOT NULL
    ) STORED,
    role TEXT DEFAULT 'user' CHECK (role IN ('user', 'moderator')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

CREATE UNIQUE INDEX profiles_nickname_lower_key ON profiles (lower(nickname));

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
    reward TEXT NULL,
    max_participants INTEGER DEFAULT 2,
    status TEXT DEFAULT 'Publicada' CHECK (status IN ('Borrador', 'Publicada', 'Asignada', 'En progreso', 'Completada', 'Cancelada', 'Reportada', 'Suspendida')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,

    CONSTRAINT check_location_by_modality CHECK (
        (modality = 'campo' AND location IS NOT NULL) OR
        (modality = 'remota' AND location IS NULL)
    )
);
COMMENT ON COLUMN missions.meeting_point IS 'Punto de encuentro textual (solo misiones de campo)';
COMMENT ON COLUMN missions.reward IS 'Recompensa informativa sin pasarela de pago (HU-024)';

-- Tabla de Postulaciones
CREATE TABLE applications (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    mission_id UUID REFERENCES missions(id) ON DELETE CASCADE NOT NULL,
    solver_id UUID REFERENCES profiles(id) ON DELETE CASCADE NOT NULL,
    status TEXT DEFAULT 'Postulado' CHECK (status IN ('Postulado', 'Aceptado', 'Rechazado')),
    applied_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    CONSTRAINT unique_application UNIQUE (mission_id, solver_id)
);

-- Checklist de ejecución de la misión (HU-053): tareas creadas y marcadas por el publicador
CREATE TABLE mission_checklist_items (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    mission_id UUID REFERENCES missions(id) ON DELETE CASCADE NOT NULL,
    title TEXT NOT NULL CONSTRAINT checklist_title_length CHECK (char_length(btrim(title)) BETWEEN 1 AND 80),
    is_done BOOLEAN DEFAULT false NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);
CREATE INDEX mission_checklist_items_mission_idx ON mission_checklist_items (mission_id, created_at);

-- Pausas de la misión en ejecución (HU-054/055): las activa el publicador y las ven los aceptados
CREATE TABLE mission_pauses (
    mission_id UUID REFERENCES missions(id) ON DELETE CASCADE NOT NULL,
    kind TEXT NOT NULL CHECK (kind IN ('socializacion', 'hidratacion', 'pausa_breve')),
    is_active BOOLEAN DEFAULT false NOT NULL,
    PRIMARY KEY (mission_id, kind)
);

-- Bitácora de la misión (HU - Evaluación y Cierre): relato del publicador y vivencias de participantes
CREATE TABLE mission_logbooks (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    mission_id UUID REFERENCES missions(id) ON DELETE CASCADE NOT NULL,
    author_id UUID REFERENCES profiles(id) ON DELETE CASCADE NOT NULL,
    role TEXT NOT NULL CHECK (role IN ('publicador', 'participante')),
    entry TEXT NOT NULL CONSTRAINT logbook_entry_not_empty CHECK (char_length(btrim(entry)) >= 1),
    participant_ids UUID[] DEFAULT '{}'::UUID[] NOT NULL,
    participant_names TEXT[] DEFAULT '{}'::TEXT[] NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    CONSTRAINT unique_mission_author UNIQUE (mission_id, author_id)
);

-- Calificaciones al publicador (HU-124): un voto por solucionador y misión completada
CREATE TABLE publisher_ratings (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    mission_id UUID REFERENCES missions(id) ON DELETE CASCADE NOT NULL,
    solver_id UUID REFERENCES profiles(id) ON DELETE CASCADE NOT NULL,
    publisher_id UUID REFERENCES profiles(id) ON DELETE CASCADE NOT NULL,
    stars INTEGER NOT NULL CHECK (stars BETWEEN 1 AND 5),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    CONSTRAINT unique_rating_per_solver_mission UNIQUE (mission_id, solver_id)
);
CREATE INDEX publisher_ratings_publisher_idx ON publisher_ratings (publisher_id);

-- ==========================================
-- 3. SEGURIDAD (RLS Y POLÍTICAS)
-- ==========================================
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE missions ENABLE ROW LEVEL SECURITY;
ALTER TABLE applications ENABLE ROW LEVEL SECURITY;
ALTER TABLE mission_checklist_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE mission_pauses ENABLE ROW LEVEL SECURITY;
ALTER TABLE mission_logbooks ENABLE ROW LEVEL SECURITY;
ALTER TABLE publisher_ratings ENABLE ROW LEVEL SECURITY;
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
CREATE POLICY "Moderators can view all missions" ON missions FOR SELECT TO authenticated
    USING (EXISTS (SELECT 1 FROM profiles p WHERE p.id = auth.uid() AND p.role = 'moderator'));
CREATE POLICY "Publishers can insert missions" ON missions FOR INSERT WITH CHECK (auth.uid() = publisher_id);
CREATE POLICY "Publishers can update missions" ON missions FOR UPDATE USING (auth.uid() = publisher_id);

-- Postulaciones
CREATE POLICY "View applications based on role" ON applications FOR SELECT USING (auth.uid() = solver_id OR auth.uid() IN (SELECT publisher_id FROM missions WHERE id = mission_id));
CREATE POLICY "Moderators can view all applications" ON applications FOR SELECT TO authenticated
    USING (EXISTS (SELECT 1 FROM profiles p WHERE p.id = auth.uid() AND p.role = 'moderator'));
CREATE POLICY "Solvers can apply" ON applications FOR INSERT WITH CHECK (auth.uid() = solver_id);
CREATE POLICY "Solvers can delete own applications" ON applications FOR DELETE USING (auth.uid() = solver_id);
CREATE POLICY "Publishers can update application status" ON applications FOR UPDATE USING (auth.uid() IN (SELECT publisher_id FROM missions WHERE id = mission_id));

-- Checklist: solo el publicador de la misión lee y gestiona sus tareas
CREATE POLICY "Publishers manage own checklist" ON mission_checklist_items FOR ALL TO authenticated
    USING (auth.uid() IN (SELECT publisher_id FROM missions WHERE id = mission_id))
    WITH CHECK (auth.uid() IN (SELECT publisher_id FROM missions WHERE id = mission_id));

-- Pausas: el publicador las gestiona y los solucionadores aceptados las leen
CREATE POLICY "Publishers manage own pauses" ON mission_pauses FOR ALL TO authenticated
    USING (auth.uid() IN (SELECT publisher_id FROM missions WHERE id = mission_id))
    WITH CHECK (auth.uid() IN (SELECT publisher_id FROM missions WHERE id = mission_id));
CREATE POLICY "Accepted solvers read pauses" ON mission_pauses FOR SELECT TO authenticated
    USING (EXISTS (SELECT 1 FROM applications a WHERE a.mission_id = mission_pauses.mission_id AND a.solver_id = auth.uid() AND a.status = 'Aceptado'));

-- Bitácora: el publicador y los solucionadores aceptados gestionan sus entradas; lectura pública para el historial
CREATE POLICY "Publishers manage own logbooks" ON mission_logbooks FOR ALL TO authenticated
    USING (auth.uid() = author_id AND auth.uid() IN (SELECT publisher_id FROM missions WHERE id = mission_id))
    WITH CHECK (auth.uid() = author_id AND auth.uid() IN (SELECT publisher_id FROM missions WHERE id = mission_id));

CREATE POLICY "Accepted solvers insert logbooks" ON mission_logbooks FOR INSERT TO authenticated
    WITH CHECK (
        auth.uid() = author_id AND
        role = 'participante' AND
        EXISTS (
            SELECT 1 FROM applications
            WHERE mission_id = mission_logbooks.mission_id
              AND solver_id = auth.uid()
              AND status = 'Aceptado'
        )
    );

CREATE POLICY "Accepted solvers update own logbooks" ON mission_logbooks FOR UPDATE TO authenticated
    USING (auth.uid() = author_id)
    WITH CHECK (auth.uid() = author_id);

CREATE POLICY "Public logbooks viewable" ON mission_logbooks FOR SELECT USING (true);

-- Calificaciones: inserta solo un participante aceptado de una misión completada; lectura pública para la reputación
CREATE POLICY "Solvers rate completed missions" ON publisher_ratings FOR INSERT TO authenticated
    WITH CHECK (
        auth.uid() = solver_id
        AND EXISTS (
            SELECT 1 FROM applications a
            JOIN missions m ON m.id = a.mission_id
            WHERE a.mission_id = publisher_ratings.mission_id
              AND a.solver_id = auth.uid()
              AND a.status = 'Aceptado'
              AND m.status = 'Completada'
              AND m.publisher_id = publisher_ratings.publisher_id
        )
    );
CREATE POLICY "Ratings viewable by everyone" ON publisher_ratings FOR SELECT USING (true);


-- configuración para evitar problemas de permisos
-- Permisos explícitos para la Data API (necesario desde Nov 2026)
GRANT SELECT ON public.missions TO anon, authenticated;
GRANT SELECT ON public.profiles TO anon, authenticated;
GRANT SELECT ON public.applications TO anon, authenticated;
GRANT INSERT ON public.applications TO authenticated;
GRANT DELETE ON public.applications TO authenticated;
GRANT UPDATE (status) ON public.applications TO authenticated;
GRANT SELECT, INSERT, DELETE ON public.mission_checklist_items TO authenticated;
GRANT UPDATE (is_done) ON public.mission_checklist_items TO authenticated;
GRANT SELECT, INSERT ON public.mission_pauses TO authenticated;
GRANT UPDATE (is_active) ON public.mission_pauses TO authenticated;
GRANT SELECT, INSERT, UPDATE ON public.mission_logbooks TO authenticated;
GRANT SELECT ON public.publisher_ratings TO anon, authenticated;
GRANT INSERT ON public.publisher_ratings TO authenticated;

-- Datos privados y intentos de login
REVOKE ALL ON public.profile_private FROM anon, authenticated;
GRANT SELECT ON public.profile_private TO authenticated;
REVOKE ALL ON public.login_attempts FROM anon, authenticated;


--reglas para la tabla de profiles
GRANT SELECT ON public.profiles TO anon, authenticated;
-- Edición del perfil público: solo estas columnas (cambio 2026-11-18_profiles_public_fields.sql)
GRANT UPDATE (nickname, gender, description, avatar_id, photo_url) ON public.profiles TO authenticated;
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

-- ==========================================
-- 6. PERFIL PÚBLICO Y FOTOS (perfil-configuracion)
-- ==========================================
-- Cambios: supabase/changes/2026-11-18_profiles_public_fields.sql y 2026-11-18_storage_avatars.sql
-- Indica si un nickname está libre (true); su propio nickname cuenta como libre.
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

-- Bucket público de fotos de perfil: máximo 2 MB, solo JPEG, ruta `{uid}/foto.jpg`.
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('avatars', 'avatars', true, 2097152, array['image/jpeg'])
on conflict (id) do update
  set public = true, file_size_limit = 2097152, allowed_mime_types = array['image/jpeg'];

create policy "avatars_select_public" on storage.objects
  for select using (bucket_id = 'avatars');
create policy "avatars_insert_own_folder" on storage.objects
  for insert to authenticated
  with check (bucket_id = 'avatars' and (storage.foldername(name))[1] = auth.uid()::text);
create policy "avatars_update_own_folder" on storage.objects
  for update to authenticated
  using (bucket_id = 'avatars' and (storage.foldername(name))[1] = auth.uid()::text)
  with check (bucket_id = 'avatars' and (storage.foldername(name))[1] = auth.uid()::text);
