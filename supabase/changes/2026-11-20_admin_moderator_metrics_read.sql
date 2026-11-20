-- Permite a moderadores consultar los registros que alimentan el panel operativo.
-- Las políticas son solo de lectura; no amplían permisos de escritura.
DROP POLICY IF EXISTS "Moderators can view all missions" ON public.missions;
CREATE POLICY "Moderators can view all missions" ON public.missions FOR SELECT TO authenticated
    USING (
        EXISTS (
            SELECT 1
            FROM public.profiles p
            WHERE p.id = auth.uid()
              AND p.role = 'moderator'
        )
    );

DROP POLICY IF EXISTS "Moderators can view all applications" ON public.applications;
CREATE POLICY "Moderators can view all applications" ON public.applications FOR SELECT TO authenticated
    USING (
        EXISTS (
            SELECT 1
            FROM public.profiles p
            WHERE p.id = auth.uid()
              AND p.role = 'moderator'
        )
    );
