-- Cambio aditivo para HU-016: conserva location GEOGRAPHY(POINT) para el mapa.
ALTER TABLE public.missions
    ADD COLUMN IF NOT EXISTS meeting_point TEXT NULL;

COMMENT ON COLUMN public.missions.meeting_point IS
    'Punto de encuentro textual (solo misiones de campo)';
