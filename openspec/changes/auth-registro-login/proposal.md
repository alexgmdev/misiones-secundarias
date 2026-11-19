# Proposal: Registro, login y recuperacion de contrasena (HU-001..HU-004)

## Intent

La app es solo plantilla. Sin autenticacion ninguna otra HU puede avanzar. Implementar registro, login (correo o celular) y recuperacion por codigo sobre Supabase, y montar la base tecnica (cliente, DI, navegacion) que reusaran todas las features.

## Scope

### In Scope
- Base: Ktor engine, serialization, navigation-compose, viewmodel-compose; `.env` -> BuildConfig; `SupabaseConfig`, cliente Supabase, Application + Koin, NavHost con stubs `perfil-inicial` / `menu-principal`.
- HU-001 registro con validacion cliente (clave >=8 + mayus + minus + digito; edad >=18, fecha no futura; celular `^3\d{9}$` normalizado; correo/telefono unicos).
- HU-002 login por correo (`signInWith(Email)`) o celular (Edge Function `login-telefono`); error siempre generico.
- HU-003/004: `resetPasswordForEmail` -> `verifyEmailOtp(RECOVERY)` -> `updateUser(password)`.
- `supabase/migrations/`: `profiles` + CHECKs + UNIQUE(telefono) + RLS + trigger `handle_new_user`; RPC `telefono_disponible`.
- `supabase/functions/login-telefono/` (Deno/TS, JSDoc).
- Checklist de dashboard; tests unitarios (validadores, ViewModels, mapeo de errores).

### Out of Scope
- Pantallas reales de perfil inicial (HU-005) y menu principal (HU-014): solo stubs.
- Verificacion de correo, SMTP propio, rate limiting avanzado, Room/cache offline.

## Capabilities

### New Capabilities
- `auth-registro`: alta de cuenta, validaciones, unicidad correo/telefono, perfil via trigger (HU-001).
- `auth-login`: login correo/celular, error generico, sesion persistida y destino inicial (HU-002).
- `auth-recuperacion`: solicitud de codigo y cambio de clave con codigo (HU-003, HU-004).

### Modified Capabilities
- None

## Approach

- Paquetes `core/{config,di,validation,navigation}`, `features/auth/{data,domain,ui}`. Validadores Kotlin puros; `AuthRepository` (interfaz) + `SupabaseAuthRepository` delgado; `sealed AuthResult/AuthError`; un ViewModel + `StateFlow<UiState>` por pantalla.
- **Login con celular**: la app llama la Edge Function con telefono + clave; esta resuelve el correo con service role, hace sign-in y devuelve solo la sesion; la app la importa (`auth.importSession`). El correo nunca llega al cliente.
- **`telefono_disponible`**: se mantiene como RPC que devuelve solo `boolean` (sin PII). HU-001 exige revelar el duplicado y el correo ya se enumera via `user_already_exists`; moverlo a Edge Function no reduce la exposicion y duplicaria el signUp. UNIQUE + mapeo del error del trigger como respaldo.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `gradle/libs.versions.toml`, `app/build.gradle.kts` | Modified | Deps, plugin serialization, BuildConfig |
| `AndroidManifest.xml`, `MainActivity.kt`, `MisionesApp.kt` | Modified/New | INTERNET, Application, NavHost |
| `core/**`, `features/auth/**`, `app/src/test/**` | New | Codigo y tests |
| `supabase/migrations/`, `supabase/functions/login-telefono/` | New | SQL y Edge Function |
| `.env.example`, `README.md` | Modified | Setup + checklist |

## Slicing (PRs a `develop`)

1. **Fundacion** (`dev/<nombre>` o `chore/auth-foundation`): deps, BuildConfig, cliente, Koin, NavHost + stubs, migracion `profiles`. Mergea primero.
2. `hu/HU-001`: registro + `telefono_disponible`.
3. `hu/HU-002`: login + Edge Function.
4. `hu/HU-003` -> `hu/HU-004` (04 depende de 03; pueden ir en paralelo con 2-3).

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Sin Android SDK local | High | Compilar/testear en otra maquina o CI antes de merge |
| "Confirm email" OFF: correos ajenos sin verificar | Med | Documentado; reactivar post-MVP |
| Enumeracion de telefono (RPC) | Med | Solo boolean, `search_path` fijo, grants minimos |
| Ajustes de dashboard no versionados | Med | Checklist en README |
| API gotrue 2.6.0 (`OtpType.Email.RECOVERY`, `importSession`) | Low | Confirmar al compilar |

## Rollback Plan

- App: revertir PRs en `develop` (orden inverso; fundacion al final).
- BD: nueva migracion de reversion (Supabase no tiene `down`): drop trigger, `handle_new_user`, `telefono_disponible`, `profiles`.
- `supabase functions delete login-telefono`; restaurar ajustes del dashboard segun checklist.

## Dependencies

- Proyecto Supabase con service role para la Edge Function; Supabase CLI.

## Success Criteria

- [ ] Todos los escenarios Gherkin de HU-001..HU-004 pasan manualmente en dispositivo.
- [ ] Tests unitarios verdes (validadores, ViewModels, mapeo de errores) con `./gradlew :app:testDebugUnitTest`.
- [ ] El correo nunca se expone en login por celular; errores de login genericos.
