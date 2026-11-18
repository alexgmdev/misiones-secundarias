-- HU-044 / HU-045: permite que el publicador acepte o rechace postulaciones.
-- La política RLS existente sigue limitando la actualización al dueño de la misión.
GRANT UPDATE (status) ON public.applications TO authenticated;
NOTIFY pgrst, 'reload schema';
