-- Permite al publicador leer sus propios borradores sin hacerlos visibles a otros usuarios.
DO $migration$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_policies
        WHERE schemaname = 'public'
          AND tablename = 'missions'
          AND policyname = 'Publishers can view own drafts'
    ) THEN
        CREATE POLICY "Publishers can view own drafts"
            ON public.missions
            FOR SELECT
            TO authenticated
            USING (auth.uid() = publisher_id AND status = 'Borrador');
    END IF;
END
$migration$;
