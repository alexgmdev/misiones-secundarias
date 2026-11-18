-- HU-053: checklist de ejecución. El publicador crea las tareas al iniciar la misión y las marca
-- como cumplidas; el porcentaje de progreso se calcula como tareas cumplidas / tareas totales.
-- Es independiente de las pausas (socialización, hidratación, pausa breve).
CREATE TABLE IF NOT EXISTS public.mission_checklist_items (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    mission_id UUID REFERENCES public.missions(id) ON DELETE CASCADE NOT NULL,
    title TEXT NOT NULL CONSTRAINT checklist_title_length CHECK (char_length(btrim(title)) BETWEEN 1 AND 80),
    is_done BOOLEAN DEFAULT false NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

CREATE INDEX IF NOT EXISTS mission_checklist_items_mission_idx ON public.mission_checklist_items (mission_id, created_at);

ALTER TABLE public.mission_checklist_items ENABLE ROW LEVEL SECURITY;

DO $migration$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE schemaname = 'public' AND tablename = 'mission_checklist_items' AND policyname = 'Publishers manage own checklist') THEN
        CREATE POLICY "Publishers manage own checklist"
            ON public.mission_checklist_items
            FOR ALL
            TO authenticated
            USING (auth.uid() IN (SELECT publisher_id FROM public.missions WHERE id = mission_id))
            WITH CHECK (auth.uid() IN (SELECT publisher_id FROM public.missions WHERE id = mission_id));
    END IF;
END
$migration$;

GRANT SELECT, INSERT, DELETE ON public.mission_checklist_items TO authenticated;
GRANT UPDATE (is_done) ON public.mission_checklist_items TO authenticated;
NOTIFY pgrst, 'reload schema';
