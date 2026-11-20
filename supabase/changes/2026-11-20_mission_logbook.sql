-- Tabla de Bitácoras de la Misión (HU - Evaluación y Cierre: Publicador + Participantes)
CREATE TABLE IF NOT EXISTS mission_logbooks (
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

ALTER TABLE mission_logbooks ENABLE ROW LEVEL SECURITY;

-- Publicador administra su bitácora
CREATE POLICY "Publishers manage own logbooks" ON mission_logbooks FOR ALL TO authenticated
    USING (auth.uid() = author_id AND auth.uid() IN (SELECT publisher_id FROM missions WHERE id = mission_id))
    WITH CHECK (auth.uid() = author_id AND auth.uid() IN (SELECT publisher_id FROM missions WHERE id = mission_id));

-- Solucionadores aceptados pueden insertar su bitácora/comentario
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

-- Solucionadores aceptados pueden actualizar su propia bitácora
CREATE POLICY "Accepted solvers update own logbooks" ON mission_logbooks FOR UPDATE TO authenticated
    USING (auth.uid() = author_id)
    WITH CHECK (auth.uid() = author_id);

-- Lectura pública para el historial de la misión
CREATE POLICY "Public logbooks viewable" ON mission_logbooks FOR SELECT USING (true);

GRANT SELECT, INSERT, UPDATE ON public.mission_logbooks TO authenticated;
