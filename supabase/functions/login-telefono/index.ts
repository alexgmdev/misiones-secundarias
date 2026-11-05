/**
 * Edge Function `login-telefono` (HU-002): inicio de sesión con celular y contraseña.
 *
 * Flujo:
 *  1. Valida la entrada y normaliza el celular (misma regla que `PhoneValidator` de la app).
 *  2. Aplica el límite de intentos: 5 fallos en 15 minutos para ese celular o esa IP de cliente
 *     (tabla `login_attempts`); si se supera responde 429 SIN intentar autenticar.
 *  3. Con service role busca `profile_private.id` por celular y luego el correo con `auth.admin.getUserById`.
 *  4. Con un cliente anon (`persistSession: false`) ejecuta `signInWithPassword`.
 *  5. Fallo: registra el intento. Éxito: borra los fallos de ese celular y devuelve solo la sesión.
 *
 * Privacidad: la respuesta NUNCA incluye el correo ni el objeto `user`; todos los errores de credenciales
 * (celular inexistente, clave incorrecta, bloqueo, entrada inválida) tienen el mismo cuerpo genérico.
 * Nunca se registran en logs el correo ni la contraseña.
 *
 * Variables de entorno (las inyecta Supabase; no hay secretos en el código):
 *  SUPABASE_URL, SUPABASE_ANON_KEY, SUPABASE_SERVICE_ROLE_KEY.
 */
import { createClient, type SupabaseClient } from "npm:@supabase/supabase-js@2";

/** Cantidad de fallos que bloquea el login por celular o por IP. */
const MAX_FAILED_ATTEMPTS = 5;

/** Ventana del límite de intentos, en minutos. */
const WINDOW_MINUTES = 15;

/** Cuerpo genérico de error de credenciales (idéntico en 400, 401 y 429). */
const GENERIC_BODY = { error: "credenciales_invalidas" };

/** Cuerpo de error interno (500). */
const INTERNAL_BODY = { error: "error_interno" };

/** Cabeceras CORS permitidas (las que envía supabase-js / functions-kt). */
const CORS_HEADERS: Record<string, string> = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

/** Celular colombiano de 10 dígitos que inicia en 3. */
const PHONE_REGEX = /^3\d{9}$/;

/** Forma aceptable de una IP (IPv4 o IPv6) para la columna `inet`; evita errores de inserción. */
const IP_REGEX = /^[0-9a-fA-F:.]{3,45}$/;

/**
 * Responde con JSON y las cabeceras CORS.
 *
 * @param body Objeto a serializar.
 * @param status Código HTTP.
 * @returns La respuesta HTTP.
 */
function json(body: unknown, status: number): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...CORS_HEADERS, "Content-Type": "application/json" },
  });
}

/**
 * Responde el error genérico de credenciales con el estado indicado.
 *
 * @param status 400, 401 o 429; el cuerpo es siempre el mismo.
 * @returns La respuesta HTTP.
 */
function genericError(status: 400 | 401 | 429): Response {
  return json(GENERIC_BODY, status);
}

/**
 * Normaliza un celular quitando espacios, guiones, paréntesis y el prefijo opcional `+57`/`57`.
 *
 * @param input Valor recibido.
 * @returns El celular de 10 dígitos que inicia en 3, o `null` si no cumple el formato.
 */
export function normalizePhone(input: unknown): string | null {
  if (typeof input !== "string") return null;
  const compact = input.replace(/[\s\-()]/g, "").replace(/^\+/, "");
  const local = compact.length === 12 ? compact.replace(/^57/, "") : compact;
  return PHONE_REGEX.test(local) ? local : null;
}

/**
 * Obtiene la IP del cliente: primer valor de `x-forwarded-for` o, si no existe, `x-real-ip`.
 *
 * @param req Solicitud entrante.
 * @returns La IP, o `null` si no viene o no tiene forma de IP.
 */
export function clientIp(req: Request): string | null {
  const forwarded = req.headers.get("x-forwarded-for")?.split(",")[0]?.trim();
  const candidate = forwarded || req.headers.get("x-real-ip")?.trim() || null;
  return candidate && IP_REGEX.test(candidate) ? candidate : null;
}

/**
 * Indica si el celular o la IP ya alcanzaron el máximo de fallos dentro de la ventana.
 *
 * @param admin Cliente con service role.
 * @param phone Celular normalizado.
 * @param ip IP del cliente, o `null`.
 * @returns `true` si hay que bloquear el intento.
 * @throws Error si falla la consulta.
 */
async function isBlocked(admin: SupabaseClient, phone: string, ip: string | null): Promise<boolean> {
  const since = new Date(Date.now() - WINDOW_MINUTES * 60_000).toISOString();
  const countBy = async (column: "phone" | "client_ip", value: string): Promise<number> => {
    const { count, error } = await admin
      .from("login_attempts")
      .select("id", { count: "exact", head: true })
      .eq(column, value)
      .gte("attempted_at", since);
    if (error) throw new Error("login_attempts_count_failed");
    return count ?? 0;
  };
  if ((await countBy("phone", phone)) >= MAX_FAILED_ATTEMPTS) return true;
  return ip !== null && (await countBy("client_ip", ip)) >= MAX_FAILED_ATTEMPTS;
}

/**
 * Registra un intento fallido (también para celulares inexistentes, para que no se puedan enumerar).
 *
 * @param admin Cliente con service role.
 * @param phone Celular normalizado.
 * @param ip IP del cliente, o `null`.
 */
async function recordFailure(admin: SupabaseClient, phone: string, ip: string | null): Promise<void> {
  const { error } = await admin.from("login_attempts").insert({ phone, client_ip: ip });
  if (error) console.error("login-telefono: no se pudo registrar el intento");
}

/**
 * Resuelve el correo de la cuenta asociada al celular (solo uso interno; nunca se devuelve).
 *
 * @param admin Cliente con service role.
 * @param phone Celular normalizado.
 * @returns El correo, o `null` si el celular no tiene cuenta.
 * @throws Error si falla alguna consulta.
 */
async function resolveEmail(admin: SupabaseClient, phone: string): Promise<string | null> {
  const { data: profile, error } = await admin.from("profile_private").select("id").eq("phone", phone).maybeSingle();
  if (error) throw new Error("profile_lookup_failed");
  if (!profile) return null;
  const { data, error: userError } = await admin.auth.admin.getUserById(profile.id);
  if (userError) throw new Error("user_lookup_failed");
  return data.user?.email ?? null;
}

/**
 * Atiende una solicitud de login por celular.
 *
 * @param req Solicitud HTTP.
 * @returns 200 con la sesión (sin `user`), 400/401/429 con el error genérico, o 500.
 */
export async function handle(req: Request): Promise<Response> {
  if (req.method === "OPTIONS") return new Response(null, { status: 204, headers: CORS_HEADERS });
  if (req.method !== "POST") return json({ error: "metodo_no_permitido" }, 405);

  let payload: { telefono?: unknown; clave?: unknown };
  try {
    payload = await req.json();
  } catch {
    return genericError(400);
  }
  const phone = normalizePhone(payload?.telefono);
  const password = payload?.clave;
  if (phone === null || typeof password !== "string" || password.length === 0) return genericError(400);

  const url = Deno.env.get("SUPABASE_URL");
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY");
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!url || !anonKey || !serviceKey) return json(INTERNAL_BODY, 500);

  const authOptions = { auth: { persistSession: false, autoRefreshToken: false } };
  const admin = createClient(url, serviceKey, authOptions);
  const ip = clientIp(req);

  try {
    // Limpieza oportunista de intentos de más de 1 día; un fallo aquí no afecta el login.
    await admin.rpc("purge_login_attempts").then(() => undefined, () => undefined);

    if (await isBlocked(admin, phone, ip)) return genericError(429);

    const email = await resolveEmail(admin, phone);
    if (email === null) {
      await recordFailure(admin, phone, ip);
      return genericError(401);
    }

    const anon = createClient(url, anonKey, authOptions);
    const { data, error } = await anon.auth.signInWithPassword({ email, password });
    if (error || !data.session) {
      if (error && (error.status ?? 0) >= 500) return json(INTERNAL_BODY, 500);
      await recordFailure(admin, phone, ip);
      return genericError(401);
    }

    await admin.from("login_attempts").delete().eq("phone", phone);
    const { access_token, refresh_token, expires_in, expires_at, token_type } = data.session;
    return json({ access_token, refresh_token, expires_in, expires_at, token_type }, 200);
  } catch {
    return json(INTERNAL_BODY, 500);
  }
}

if (import.meta.main) {
  Deno.serve(handle);
}
