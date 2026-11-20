-- HU-124: calificaciones al publicador tras completar una misión.
-- Aplicar con coordinación del Líder Técnico sobre la base compartida.
-- Idempotente: se puede ejecutar más de una vez.

CREATE TABLE IF NOT EXISTS public.publisher_ratings (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    mission_id UUID REFERENCES public.missions(id) ON DELETE CASCADE NOT NULL,
    solver_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE NOT NULL,
    publisher_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE NOT NULL,
    stars INTEGER NOT NULL CHECK (stars BETWEEN 1 AND 5),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    CONSTRAINT unique_rating_per_solver_mission UNIQUE (mission_id, solver_id)
);

CREATE INDEX IF NOT EXISTS publisher_ratings_publisher_idx
    ON public.publisher_ratings (publisher_id);

ALTER TABLE public.publisher_ratings ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Solvers rate completed missions" ON public.publisher_ratings;
CREATE POLICY "Solvers rate completed missions" ON public.publisher_ratings
    FOR INSERT TO authenticated
    WITH CHECK (
        auth.uid() = solver_id
        AND EXISTS (
            SELECT 1
            FROM public.applications a
                     JOIN public.missions m ON m.id = a.mission_id
            WHERE a.mission_id = publisher_ratings.mission_id
              AND a.solver_id = auth.uid()
              AND a.status = 'Aceptado'
              AND m.status = 'Completada'
              AND m.publisher_id = publisher_ratings.publisher_id
        )
    );

DROP POLICY IF EXISTS "Ratings viewable by everyone" ON public.publisher_ratings;
CREATE POLICY "Ratings viewable by everyone" ON public.publisher_ratings
    FOR SELECT USING (true);

GRANT SELECT ON public.publisher_ratings TO anon, authenticated;
GRANT INSERT ON public.publisher_ratings TO authenticated;
