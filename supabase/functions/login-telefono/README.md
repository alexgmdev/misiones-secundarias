# Edge Function `login-telefono` (HU-002)

Inicio de sesión con celular y contraseña. La app envía `POST {"telefono","clave"}`; la función resuelve el correo
internamente y devuelve solo la sesión (`access_token`, `refresh_token`, `expires_in`, `expires_at`, `token_type`),
sin `user` ni correo. Errores de credenciales: siempre `{"error":"credenciales_invalidas"}` (400, 401 y 429 idénticos).
Límite: 5 fallos en 15 minutos por celular o por IP (`login_attempts`).

## Despliegue (tech lead)

Requisitos previos: haber corrido `supabase/changes/2026-11-03_auth_profiles_privacy.sql` y
`supabase/changes/2026-11-03_login_attempts.sql` en la base de datos.

```bash
supabase login
supabase link --project-ref <PROJECT_REF>
supabase functions deploy login-telefono
```

`SUPABASE_URL`, `SUPABASE_ANON_KEY` y `SUPABASE_SERVICE_ROLE_KEY` los inyecta Supabase automáticamente en las Edge
Functions: no hay que crearlos con `supabase secrets set` (y la clave service role nunca va en la app ni en el repo).

Verificación de JWT: la app envía la anon key como `apikey`/`Authorization`; con la anon key en formato JWT el
despliegue por defecto funciona. Si el proyecto usa las nuevas claves `sb_publishable_...` (no son JWT), desplegar con
`--no-verify-jwt`: la función no necesita identidad del llamador y se protege con el límite de intentos.

Opcional: programar la limpieza con pg_cron (la función también limpia de forma oportunista en cada llamada):
`select cron.schedule('purge-login-attempts', '0 * * * *', 'select public.purge_login_attempts()');`

## Verificación local con Deno (opcional)

```bash
deno check supabase/functions/login-telefono/index.ts
deno lint supabase/functions/login-telefono/index.ts
```

## Pruebas manuales con curl (tarea 2.6)

```bash
URL="https://<PROJECT_REF>.supabase.co/functions/v1/login-telefono"
ANON="<SUPABASE_ANON_KEY>"
call() { curl -s -w '\n%{http_code}\n' -X POST "$URL" -H "apikey: $ANON" -H "Authorization: Bearer $ANON" \
  -H 'Content-Type: application/json' -d "{\"telefono\":\"$1\",\"clave\":\"$2\"}"; }
```

1. Éxito: `call 3001234567 '<clave correcta>'` -> 200 con los 5 campos de sesión; no debe aparecer `user` ni correo.
2. Clave incorrecta: `call 3001234567 'mala'` -> 401 `{"error":"credenciales_invalidas"}`.
3. Celular sin cuenta: `call 3109999999 'cualquiera'` -> 401 con el mismo cuerpo exacto que el punto 2.
4. Formato libre: `call '300 123 4567' '<clave correcta>'` y `call '+57 300-123-4567' ...` -> 200.
5. Bloqueo: repetir el punto 2 cinco veces; el 6.º intento con la clave CORRECTA -> 429 con el mismo cuerpo, sin sesión.
6. Bloqueo por IP: desde la misma IP, 5 fallos con celulares distintos y luego un celular nuevo -> 429.
7. Login por correo no afectado: con el celular bloqueado, iniciar sesión en la app con el correo -> OK.
8. Ventana vencida: en el SQL editor
   `update public.login_attempts set attempted_at = now() - interval '16 minutes' where phone = '3001234567';`
   y luego `call 3001234567 '<clave correcta>'` -> 200 (y los fallos de ese celular quedan borrados).
9. Entrada inválida: `call 12345 x` o cuerpo no JSON -> 400 con el mismo cuerpo genérico.
10. CORS: `curl -i -X OPTIONS "$URL"` -> 204 con `Access-Control-Allow-*`.
