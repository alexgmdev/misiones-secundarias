-- HU-054 / HU-055: pausas de la misión en ejecución (socialización, hidratación y pausa breve).
-- El publicador las activa o desactiva; los solucionadores aceptados ven que la misión está en pausa.
-- Son independientes del checklist de progreso (mission_checklist_items).
CREATE TABLE IF NOT EXISTS public.mission_pauses (
    mission_id UUID REFERENCES public.missions(id) ON DELETE CASCADE NOT NULL,
    kind TEXT NOT NULL CHECK (kind IN ('socializacion', 'hidratacion', 'pausa_breve')),
    is_active BOOLEAN DEFAULT false NOT NULL,
    PRIMARY KEY (mission_id, kind)
);

ALTER TABLE public.mission_pauses ENABLE ROW LEVEL SECURITY;

DO $migration$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE schemaname = 'public' AND tablename = 'mission_pauses' AND policyname = 'Publishers manage own pauses') THEN
        CREATE POLICY "Publishers manage own pauses"
            ON public.mission_pauses
            FOR ALL
            TO authenticated
            USING (auth.uid() IN (SELECT publisher_id FROM public.missions WHERE id = mission_id))
            WITH CHECK (auth.uid() IN (SELECT publisher_id FROM public.missions WHERE id = mission_id));
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE schemaname = 'public' AND tablename = 'mission_pauses' AND policyname = 'Accepted solvers read pauses') THEN
        CREATE POLICY "Accepted solvers read pauses"
            ON public.mission_pauses
            FOR SELECT
            TO authenticated
            USING (EXISTS (
                SELECT 1 FROM public.applications a
                WHERE a.mission_id = mission_pauses.mission_id
                  AND a.solver_id = auth.uid()
                  AND a.status = 'Aceptado'
            ));
    END IF;
END
$migration$;

GRANT SELECT, INSERT ON public.mission_pauses TO authenticated;
GRANT UPDATE (is_active) ON public.mission_pauses TO authenticated;
NOTIFY pgrst, 'reload schema';
