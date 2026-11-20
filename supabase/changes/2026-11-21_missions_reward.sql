-- HU-024: recompensa informativa de una misión.
-- Aplicar con coordinación del Líder Técnico sobre la base compartida.
-- Idempotente: se puede ejecutar más de una vez.

ALTER TABLE public.missions
    ADD COLUMN IF NOT EXISTS reward TEXT NULL;

COMMENT ON COLUMN public.missions.reward IS
    'Recompensa informativa (refrigerios, certificados, insumos). No implica pagos.';

-- La vista pública debe exponer la nueva columna para tarjeta y detalle.
-- IMPORTANTE: `location` se mantiene como TEXT con la coordenada aproximada
-- (2 decimales); si se devuelve como geography, Postgres rechaza el reemplazo
-- con "cannot change data type of view column location".
CREATE OR REPLACE VIEW public.public_missions
    WITH (security_invoker = true) AS
SELECT id,
       publisher_id,
       title,
       description,
       category,
       requirements,
       meeting_point,
       modality,
       date_time,
       max_participants,
       status,
       created_at,
       ST_AsText(ST_SnapToGrid(location::geometry, 0.01)) AS location,
       reward
FROM public.missions;

GRANT SELECT ON public.public_missions TO anon, authenticated;
