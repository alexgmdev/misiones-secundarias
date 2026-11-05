## Exploration: auth-registro-login (HU-001..HU-004)

### Current State
- App es solo plantilla: `MainActivity` + `Greeting`, paquete `ui/theme`. Sin Application, Koin, cliente Supabase, navegacion ni Room.
- `app/build.gradle.kts`: declara supabase-kt, gotrue-kt, postgrest-kt, Koin, Room/KSP, pero NO hay motor Ktor, ni plugin `kotlinx.serialization`, ni navigation-compose, ni `buildConfig`. `.env` (SUPABASE_URL/ANON_KEY) no llega a BuildConfig. Sin `viewmodel-compose`.
- Backlog: HU-001 registro (nombre, apellido, correo, telefono, fecha nac., clave + confirmacion; correo y telefono unicos; exito -> pantalla "configuracion inicial de perfil" (fuera de alcance, stub)); HU-002 login correo o celular, error generico, exito -> menu principal (stub); HU-003 "Olvidaste tu contrasena" pide correo, envia codigo numerico temporal, confirma sin revelar datos; HU-004 codigo + nueva clave + confirmacion ("Guardar Nueva Contrasena"), luego login con la nueva clave.
- `prototipar` (draw.io): solo contiene pantallas de foro/mision (preguntas, respuestas, solucionadores/postulaciones). NO hay pantallas de login/registro/recuperacion: campos y flujo salen solo de las HU. Hay que disenar UI (Material 3 estandar).
- Sin Android SDK en la maquina: no se puede compilar ni correr tests (riesgo).

### Verificacion de API supabase-kt 2.6.0 (confianza media-alta; docs web no listan ejemplos de todo)
- Modulo en 2.x: `gotrue-kt` (renombrado a `auth-kt` recien en 3.0). Paquete `io.github.jan.supabase.gotrue`, plugin `Auth`, acceso `supabase.auth`. Mantener `supabase-gotrue` del catalogo. (Alta)
- Registro: `auth.signUpWith(Email) { email; password; data = buildJsonObject{...} }` (metadata va a `raw_user_meta_data`). Login: `auth.signInWith(Email) { email; password }`. (Alta)
- Recuperacion por codigo: `auth.resetPasswordForEmail(email)` (envia correo; para que traiga CODIGO hay que editar la plantilla "Reset Password" en el dashboard para usar `{{ .Token }}` en vez de `{{ .ConfirmationURL }}`), luego `auth.verifyEmailOtp(type = OtpType.Email.RECOVERY, email, token)` (inicia sesion) y `auth.updateUser { password = nuevaClave }`. La docs confirman `verifyEmailOtp(type=OtpType.Email.EMAIL,...)`; `OtpType.Email.RECOVERY` es el tipo correspondiente (confianza media: confirmar en el IDE al implementar). (Media)
- Requiere motor Ktor (ej. `io.ktor:ktor-client-android` o `okhttp`, version compatible con Ktor 2.3.x que usa supabase-kt 2.6) y plugin `org.jetbrains.kotlin.plugin.serialization` (version = kotlin). Se recomienda `io.github.jan-tennert.supabase:bom:2.6.0`. (Alta)
- Defaults Supabase: OTP de 6 digitos (configurable 6-10), expira en 1h (configurable), limite 1 correo/60 s por usuario, SMTP incorporado muy limitado (pocos correos/hora): configurar SMTP propio para uso real.
- IMPORTANTE (HU-001 vs enumeracion): con "Confirm email" activo Supabase responde exito falso al registrar correo existente (anti-enumeracion), lo que contradice el escenario "correo ya en uso". Con confirmacion DESACTIVADA devuelve error `user_already_exists`. HU-001 redirige directo al perfil (sin verificar correo) -> desactivar "Confirm email".

### Approaches y recomendaciones

1. Arquitectura / paquetes
   - A) Por capas globales (data/, domain/, ui/) — simple, escala mal. 
   - B) **Por feature (recomendada)**: `core/{config,di,validation,navigation}`, `features/auth/{data,domain,ui}`; `AuthRepository` (interfaz) + `SupabaseAuthRepository`; validadores Kotlin puros (`PasswordValidator`, `EmailValidator`, `PhoneValidator`, `BirthDateValidator`); ViewModel por pantalla (Registro, Login, SolicitarCodigo, RestablecerClave) con `StateFlow<UiState>` + eventos; resultados como `sealed AuthResult/AuthError` (sin filtrar excepciones Supabase a la UI). Pros: TDD facil con MockK, aislamiento de feature. Esfuerzo: medio. Sin casos de uso separados (YAGNI) salvo logica reusable.
   - Dependencias nuevas: `lifecycle-viewmodel-compose`, `navigation-compose`, `koin-android` ya, `kotlinx-serialization`, Ktor engine.

2. Navegacion
   - A) navigation-compose con rutas string/sealed `Destino` (recomendada, simple, 2.8.x compatible con compileSdk 34). B) rutas tipadas (`@Serializable`, nav 2.8+) — buen ajuste ya que se anade serialization, pero mas riesgo de version. Recomendacion: A con sealed class de rutas; grafo `auth` (login, registro, solicitar-codigo, restablecer) + destinos stub `perfil-inicial` y `menu-principal`; destino inicial segun `auth.sessionStatus` (sesion persistida). Un solo NavHost en MainActivity; ViewModels via `koinViewModel()`. Flujo recuperar: solicitar-codigo(correo) -> restablecer(correo, codigo, clave, confirmacion) -> login. `popUpTo` para no volver atras tras exito.

3. Datos de perfil y unicidad
   - A) Insert desde cliente en `profiles` tras signUp — no atomico, falla a medias, exige RLS de insert.
   - B) **Tabla `public.profiles` + trigger `AFTER INSERT ON auth.users` (SECURITY DEFINER)** que lee `raw_user_meta_data` (nombre, apellido, telefono, fecha_nacimiento) (recomendada): atomica, sin ventana inconsistente. Unicidad: correo por `auth.users` (error `user_already_exists`); telefono con `UNIQUE` en `profiles.telefono` (digitos normalizados, CHECK formato 10 digitos CO `^3\d{9}$`) como respaldo. Problema: si el trigger falla por telefono duplicado, signUp devuelve "Database error saving new user" generico -> agregar RPC `telefono_disponible(p_telefono)` (SECURITY DEFINER, ejecutable por anon) que el cliente llama ANTES de signUp para mostrar "telefono ya en uso" (el HU exige mensaje de duplicado; se acepta el compromiso de enumeracion), y mapear el error del trigger como fallback.
   - RLS: `profiles` con select/update solo del propio `auth.uid()`; sin insert/delete desde cliente (lo hace el trigger).

4. Login con celular
   - RPC `correo_para_login(p_identificador text)` SECURITY DEFINER (decision del usuario): si el identificador contiene `@` se usa tal cual; si no, normaliza digitos y busca `profiles.telefono` -> `auth.users.email`. Cliente luego `signInWith(Email)`. Cualquier falla (no existe / clave mala) -> mismo mensaje generico "Credenciales incorrectas".
   - RIESGO: un RPC ejecutable por anon que devuelve el correo de un telefono expone PII y permite enumerar. Mitigaciones: devolver solo cuando sea telefono (no para correos), `REVOKE ... FROM public` + `GRANT EXECUTE TO anon`, `search_path` fijo; mejor (post-MVP) mover a Edge Function que haga el sign-in server-side y no devuelva el correo, con rate limit. Quedar registrado como deuda.

5. Secretos a BuildConfig
   - A) `local.properties` — ya ignorado, pero el equipo usa `.env`. B) **Leer `.env` en `build.gradle.kts` (Properties/parser simple), `buildFeatures { buildConfig = true }`, `buildConfigField("String","SUPABASE_URL",...)`, con fallback a variables de entorno y valor vacio + `check` solo en tareas de release (recomendada)**. Entregar `.env.example` y encapsular en `SupabaseConfig` (`core/config`) para inyectar en Koin y poder testear. La anon key es publica por diseno (protegida por RLS), pero no se versiona.

6. Edad minima (SIN REGLA EN EL BACKLOG — decision pendiente del usuario)
   - Se recolecta fecha de nacimiento pero ninguna HU define edad minima ni formato. Las misiones implican encuentros presenciales con desconocidos. Propuesta por defecto: minimo 18 anos (validacion cliente + CHECK en BD), fecha no futura. Alternativa 14 (habeas data Colombia, Ley 1581 / datos de menores). Debe confirmarse antes de la spec.
   - Otros huecos: formato de telefono (propuesta: 10 digitos celular CO, sin indicativo), longitud maxima de nombres, politica de contrasena ya fijada (>=8, mayus, minus, digito; configurar tambien en Supabase: min length 8 + "requires lower, upper, digits").

7. Migraciones SQL
   - Guardar en `supabase/migrations/<timestamp>_*.sql` (el `.gitignore` solo excluye `config.toml`, `.temp`, `.branches`, `.env`; las migraciones SI se versionan). Archivos propuestos: `..._profiles.sql` (tabla, CHECKs, UNIQUE, RLS, trigger `handle_new_user`), `..._auth_rpc.sql` (`telefono_disponible`, `correo_para_login`, grants). Aplicacion manual via SQL editor o `supabase db push`; documentar en README. Ajustes de dashboard no versionables (checklist): Confirm email OFF, password policy, plantilla Reset Password con `{{ .Token }}`, expiracion OTP, SMTP propio.

### Affected Areas
- `gradle/libs.versions.toml`, `app/build.gradle.kts` — Ktor engine, serialization plugin, navigation-compose, viewmodel-compose, BuildConfig/.env.
- `app/src/main/.../MisionesApp.kt` (nuevo Application + Koin), `AndroidManifest.xml` (name, INTERNET permission — verificar), `MainActivity.kt` (NavHost).
- `core/**`, `features/auth/**` (nuevos), `app/src/test/**` (tests ViewModel/validadores/repo con MockK).
- `supabase/migrations/*.sql` (nuevos), `.env.example`, README (setup Supabase).

### Recommendation
Feature-package + ViewModel/StateFlow + `AuthRepository` interfaz; navigation-compose con rutas sealed; profiles + trigger + RPCs (`telefono_disponible`, `correo_para_login`); `.env` -> BuildConfig; confirmar edad minima (default 18). Plan de TDD: validadores -> ViewModels (repo mockeado) -> repo Supabase (capa fina, verificada manualmente / instrumentada).

### Risks
- Sin Android SDK local: no se puede compilar/probar; verificar en maquina de otro integrante / CI.
- API exacta (`OtpType.Email.RECOVERY`, import paths gotrue 2.6.0) por confirmar al compilar.
- Recuperacion depende de configuracion de dashboard no versionada (plantilla con `{{ .Token }}`, SMTP, expiracion); sin ella llega enlace en vez de codigo.
- RPC `correo_para_login` expone PII / permite enumerar; `telefono_disponible` tambien enumera (exigido por HU-001).
- "Confirm email" desactivado: cualquiera puede registrar correos ajenos sin verificar (decision implicita en HU-001); anotar.
- Limites de correo de Supabase (rate limit, SMTP por defecto) afectan HU-003.
- Edad minima y formato de telefono sin definir.
- HU-001/HU-002 dependen de pantallas stub (perfil inicial, menu principal) no especificadas.

### Ready for Proposal
Si, tras confirmar con el usuario la edad minima (default 18) y el formato de telefono (default 10 digitos CO).
