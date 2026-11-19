-- =============================================================================
-- Cambio: auth-registro-login — límite de intentos de login por celular (HU-002)
-- Fecha : 2026-11-03
-- Autor : David Chicaiza (Groovy Cow)
--
-- Qué hace
--   Crea `login_attempts`, usada por la Edge Function `login-telefono` para
--   bloquear fuerza bruta: 5 fallos en 15 minutos por celular o por IP -> 429.
--   Solo accede el service role (RLS activa y sin políticas, sin grants).
--   Incluye `purge_login_attempts()` para borrar intentos de más de 1 día.
--
-- Idempotencia: se puede ejecutar más de una vez.
-- Reversa: ver `2026-11-03_auth_rollback.sql`.
-- Ejecución: manual en el SQL editor de Supabase.
-- =============================================================================

begin;

-- Un registro por intento FALLIDO (los bloqueados no se registran)
create table if not exists public.login_attempts (
  id           bigint generated always as identity primary key,
  phone        text not null,
  client_ip    inet,
  attempted_at timestamptz not null default now()
);

comment on table public.login_attempts is
  'Intentos fallidos de login por celular (Edge Function login-telefono). Solo service role.';
comment on column public.login_attempts.phone is 'Celular normalizado con el que se intentó iniciar sesión.';
comment on column public.login_attempts.client_ip is 'IP del cliente según la Edge Function; puede ser nula.';
comment on column public.login_attempts.attempted_at is 'Momento del intento fallido (UTC).';

-- Índices para contar fallos recientes por celular y por IP
create index if not exists login_attempts_phone_idx on public.login_attempts (phone, attempted_at desc);
create index if not exists login_attempts_ip_idx    on public.login_attempts (client_ip, attempted_at desc);

-- RLS activa y sin políticas: nadie salvo el service role (que la omite) accede
alter table public.login_attempts enable row level security;
revoke all on public.login_attempts from anon, authenticated;

-- Limpieza de intentos de más de 1 día. La invoca la Edge Function en cada llamada
-- (oportunista); opcionalmente con pg_cron:
--   select cron.schedule('purge-login-attempts', '0 * * * *', 'select public.purge_login_attempts()');
create or replace function public.purge_login_attempts()
returns void
language sql
security definer
set search_path = ''
as $$
  delete from public.login_attempts where attempted_at < now() - interval '1 day';
$$;

comment on function public.purge_login_attempts() is
  'Borra intentos de login de más de 1 día. Solo ejecutable por service role.';

revoke execute on function public.purge_login_attempts() from public, anon, authenticated;

commit;
