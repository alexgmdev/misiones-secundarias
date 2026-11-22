-- Tabla de Reportes de Moderación (HU - Moderación y Seguridad)
CREATE TABLE IF NOT EXISTS moderation_reports (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    reporter_id UUID REFERENCES profiles(id) ON DELETE CASCADE NOT NULL,
    target_type TEXT NOT NULL CHECK (target_type IN ('Misión', 'Usuario', 'Comentario')),
    target_id TEXT NOT NULL,
    reason TEXT NOT NULL CONSTRAINT report_reason_not_empty CHECK (char_length(btrim(reason)) >= 1),
    details TEXT,
    priority TEXT DEFAULT 'Media' CHECK (priority IN ('Alta', 'Media', 'Baja')),
    status TEXT DEFAULT 'Pendiente' CHECK (status IN ('Pendiente', 'En revisión', 'Resuelto', 'Desestimado')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

ALTER TABLE moderation_reports ENABLE ROW LEVEL SECURITY;

-- Moderadores leen y gestionan todos los reportes
CREATE POLICY "Moderators read all reports" ON moderation_reports FOR SELECT TO authenticated
    USING (EXISTS (SELECT 1 FROM profiles WHERE id = auth.uid() AND role = 'moderator'));

CREATE POLICY "Moderators update reports" ON moderation_reports FOR UPDATE TO authenticated
    USING (EXISTS (SELECT 1 FROM profiles WHERE id = auth.uid() AND role = 'moderator'))
    WITH CHECK (EXISTS (SELECT 1 FROM profiles WHERE id = auth.uid() AND role = 'moderator'));

-- Usuarios autenticados crean reportes
CREATE POLICY "Authenticated users create reports" ON moderation_reports FOR INSERT TO authenticated
    WITH CHECK (auth.uid() = reporter_id);

GRANT SELECT, INSERT, UPDATE ON public.moderation_reports TO authenticated;
