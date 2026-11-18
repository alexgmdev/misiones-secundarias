-- HU-050: permite que el publicador inicie su misión (estado "En progreso").
-- La política RLS "Publishers can update missions" sigue limitando la actualización al dueño.
-- Es idempotente: si el permiso ya existe, no cambia nada.
GRANT UPDATE (status) ON public.missions TO authenticated;
NOTIFY pgrst, 'reload schema';
