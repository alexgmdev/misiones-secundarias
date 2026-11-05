# Tasks: Registro, login y recuperación (HU-001..HU-004)

Tamaños: S (<1 h), M (1-3 h), L (>3 h). Base de código: `app/src/main/java/com/groovycow/misionessecundarias/` (abreviado `…/`); tests en `app/src/test/java/com/groovycow/misionessecundarias/` (`T/`).
Reglas globales: TDD estricto (RED -> GREEN -> REFACTOR) con `./gradlew :app:testDebugUnitTest`; KDoc en español en todo lo público es criterio de "hecho" de cada tarea; ktlint + detekt deben pasar; sin SDK local, verificación = CI en `dev/*`.
Trazabilidad: AR = auth-registro, AL = auth-login, AC = auth-recuperacion (requisito entre paréntesis).

## PR 0: Fundación (rama `dev/<nombre>` -> develop; HU-001..004)

- [ ] 0.1 (S) Coordinar con Alex: mover `AppDestination` a `ui/navigation/AppNavigation.kt`, borrar `.env.example`; acordar orden de merge.
- [x] 0.2 (S) `AndroidManifest.xml`: `<uses-permission INTERNET>`. `.gitignore`: `/release/`, `/debug/`, quitar comentario en línea de `schemas/`.
- [x] 0.3 (S) `gradle/libs.versions.toml` + `app/build.gradle.kts`: `supabase-functions` (functions-kt, `version.ref = "supabase"`); `di/SupabaseModule.kt`: `install(Functions)`.
- [x] 0.4 (S) `README.md`: setup con `local.properties` (no `.env`) + checklist de dashboard; borrar `.env.example`.
- [x] 0.5 (S) Crear `domain/model/AuthError.kt` (`AuthError` sellado + `AuthException`), `SessionState.kt`, `RegistrationData.kt`, con KDoc.
- [x] 0.6 (M) RED `T/data/repository/AuthErrorMapperTest.kt`: `AuthRestException` (`user_already_exists`, `email_exists`, `weak_password`, `invalid_credentials`, `otp_expired`, `same_password`, 5xx), `IOException`/timeout, por operación (LOGIN/VERIFY/otras). GREEN `data/repository/AuthErrorMapper.kt` (`mapRest`/`mapTransport`, `when` expresión). [AR Unicidad correo, Contraseña robusta, Errores red; AL Error genérico, Errores red; AC Código inválido, Errores red]
- [x] 0.7 (S) RED `T/domain/usecase/AuthCatchingTest.kt` (éxito, `AuthException` -> failure, `CancellationException` se relanza) -> GREEN `domain/usecase/AuthCatching.kt`.
- [x] 0.8 (M) `domain/repository/AuthRepository.kt`: `observeSession()` (+ `signIn`/`signOut` conservados). `data/repository/AuthRepositoryImpl.kt` con `observeSession` (LoadingFromStorage/Authenticated/NetworkError/NotAuthenticated) + `signOut`. [AL Sesión persistida]
- [x] 0.9 (S) RED `T/testutil/MainDispatcherRule.kt` + `T/domain/usecase/ObserveSessionUseCaseTest.kt` -> GREEN `domain/usecase/ObserveSessionUseCase.kt`.
- [x] 0.10 (M) RED `T/ui/auth/SessionViewModelTest.kt`: LOADING -> Login (sin sesión) o MainMenu (con sesión), decidido una sola vez. GREEN `ui/auth/SessionViewModel.kt`. [AL Sesión persistida: Reapertura/Sin sesión]
- [x] 0.11 (M) `di/AuthModule.kt` (Clock, mapper, repo, VM de sesión) + `MisionesApp.kt` registra `authModule`.
- [x] 0.12 (L) `ui/navigation/AppNavigation.kt` (mover `AppDestination` + `when`; destinos `Login`, `InitialProfile` = `FeaturePlaceholderScreen`, `MainMenu`; spinner mientras LOADING; `BackHandler`). `MainActivity.kt` -> `setContent { …AppNavigation() }`. Verificar que misiones/postulaciones se comportan igual. [AL Sesión persistida]
- [x] 0.13 (M) SQL: `supabase/schema.sql` (estado final) + `supabase/changes/2026-11-03_auth_profiles_privacy.sql` (`profile_private`, first/last_name, drop `phone`, `handle_new_user`, `telefono_disponible`, RLS, `COMMENT ON`) + `supabase/changes/2026-11-03_login_attempts.sql` (tabla, índices, `purge_login_attempts`). Verificación manual: SQL editor en proyecto de prueba, correr dos veces (idempotencia). [AR Unicidad celular, Registro exitoso; AL Límite]
- [ ] 0.14 (S) Push a `dev/*` y confirmar CI verde (ktlintCheck, detekt, testDebugUnitTest, assembleDebug); abrir PR a develop.
- [ ] 0.15 (S) Coordinar con Juan (credenciales Supabase): correr ambos `supabase/changes/*.sql` en la BD compartida (borra `profiles.phone`; recrear usuarios de prueba), con aviso a Alex.
- [ ] 0.16 (S) Checklist de dashboard con Juan: Confirm email OFF; clave min 8 + lower/upper/digits; plantilla Reset Password con `{{ .Token }}`; OTP 6 dígitos y expiración (900 s); SMTP propio.

## PR 1: `hu/HU-001` Registro (depende de PR 0)

- [x] 1.1 (S) RED `T/domain/validation/{Email,Name}ValidatorTest.kt` (vacío, solo espacios, trim, <=60, formato) -> GREEN `domain/validation/{ValidationResult,FieldError,EmailValidator,NameValidator}.kt`. [AR Campos obligatorios, Formato de correo]
- [x] 1.2 (S) RED `PhoneValidatorTest` (`normalize`: espacios, guiones, `+57`, `^3\d{9}$`, null) -> GREEN `PhoneValidator.kt`. [AR Formato de celular x3]
- [x] 1.3 (S) RED `PasswordValidatorTest` (`unmetRules`: <8, sin mayús/minús/dígito, válida) -> GREEN `PasswordValidator.kt`. [AR Contraseña robusta x3, Confirmación]
- [x] 1.4 (S) RED `BirthDateValidatorTest` con `Clock.fixed(2026-11-03)`: menor, futura, exactamente 18 -> GREEN `BirthDateValidator.kt`. [AR Edad mínima x3]
- [x] 1.5 (M) RED `T/domain/usecase/RegisterUseCaseTest.kt`: `isPhoneAvailable=false` -> `PhoneInUse` sin `signUp`; EmailInUse; NoConnection; ServerError; éxito. GREEN `RegisterUseCase.kt`. [AR Unicidad x4, Registro exitoso, Errores x2]
- [x] 1.6 (M) `AuthRepository`: `isPhoneAvailable`, `signUp(RegistrationData)`; `AuthRepositoryImpl` (rpc `telefono_disponible`, `signUpWith(Email){data=…}`; DTOs privados). [AR Unicidad celular, Registro exitoso]
- [x] 1.7 (L) RED `T/ui/auth/register/RegisterViewModelTest.kt`: errores por campo, correo trim, no llama caso de uso si hay errores, `isLoading` evita doble envío, `isRegistered`, `error`. GREEN `RegisterViewModel.kt` + `RegisterUiState`. [AR todos los requisitos de validación, Estado de carga, Errores]
- [x] 1.8 (L) `ui/auth/register/RegisterScreen.kt` + `ui/auth/AuthMessages.kt` (textos de la spec) + destino `Register` y enlace desde Login; `onRegistered` -> `InitialProfile`; registrar VM/validadores en `AuthModule`. Manual: Gherkin HU-001 en dispositivo.
- [ ] 1.9 (S) Push, CI verde, PR a develop.

## PR 2: `hu/HU-002` Login + Edge Function (depende de PR 0; reusa PhoneValidator de PR 1 -> mergear después de PR 1)

- [x] 2.1 (M) RED `T/domain/usecase/SignInUseCaseTest.kt`: sin `@` -> normaliza celular; `normalize==null` -> InvalidCredentials sin red; reduce errores a {InvalidCredentials, NoConnection, ServerError}. GREEN `SignInUseCase.kt`. [AL Login correo/celular, Celular formato libre, Error genérico, Celular desconocido, Errores red]
- [x] 2.2 (M) `AuthRepository.signInWithPhone` + Impl: `functions.invoke("login-telefono")`, `importSession(user=null)` + `retrieveUserForCurrentSession(updateSession=true)`; fallback `importAuthToken`. [AL Login con celular, Correo no expuesto]
- [x] 2.3 (M) RED `T/ui/auth/login/LoginViewModelTest.kt` (campos vacíos, `isLoading`, `isSignedIn`, `error`) -> GREEN `LoginViewModel.kt`. [AL Campos obligatorios, Estado de carga]
- [x] 2.4 (M) `ui/auth/login/LoginScreen.kt`, destino Login conectado (`onSignedIn` -> `MainMenu`, enlaces a Register/RequestCode); VM en `AuthModule`.
- [x] 2.5 (L) `supabase/functions/login-telefono/index.ts` (JSDoc): normaliza, límite 5/15 min por celular o IP (`x-forwarded-for`), service role -> `getUserById` -> `signInWithPassword`, respuesta sin `user`, 401/429 mismo cuerpo, limpieza oportunista. [AL Correo no expuesto, Límite x5, Celular desconocido]
- [ ] 2.6 (S) Verificación manual con `curl`: 5 fallos y 6.º con clave correcta = 429; cuerpo idéntico a 401; sin `user`/correo; login por correo sigue OK; ventana vencida.
- [ ] 2.7 (S) Coordinar con Juan: `supabase functions deploy login-telefono` con secretos (service role, anon key).
- [ ] 2.8 (S) Push, CI verde (confirmar deserialización `UserSession` sin `user`; si falla, `importAuthToken`), PR. Manual: Gherkin HU-002.

## PR 3: `hu/HU-003` Solicitar código (depende de PR 0; Email validator de PR 1)

- [ ] 3.1 (M) RED `T/domain/usecase/RequestPasswordResetUseCaseTest.kt`: éxito neutro para todo salvo NoConnection. GREEN `RequestPasswordResetUseCase.kt`. [AC Sin divulgación, Errores red]
- [ ] 3.2 (S) `AuthRepository.sendPasswordResetCode` + Impl (`resetPasswordForEmail`). [AC Solicitud de código]
- [ ] 3.3 (M) RED `T/ui/auth/recovery/RequestCodeViewModelTest.kt` (vacío, formato, `isCodeSent`, `isLoading`, NoConnection) -> GREEN `RequestCodeViewModel.kt`. [AC Solicitud x3, Estado de carga]
- [ ] 3.4 (M) `RequestCodeScreen.kt`, destino `RequestCode`, `BackHandler`, DI.
- [ ] 3.5 (S) Push, CI verde, PR. Manual: Gherkin HU-003 (correo real y no registrado = misma confirmación).

## PR 4: `hu/HU-004` Cambiar clave (depende de PR 3)

- [ ] 4.1 (S) RED `RecoveryCodeValidatorTest` (`^\d{6,10}$`, vacío, no numérico) -> GREEN `RecoveryCodeValidator.kt`. [AC Código vacío o no numérico]
- [ ] 4.2 (M) RED `T/domain/usecase/{VerifyRecoveryCode,ChangePassword}UseCaseTest.kt`: InvalidOrExpiredCode, SamePassword, WeakPassword, éxito + `signOut`. GREEN ambos casos de uso. [AC Código inválido, Restablecimiento, Errores]
- [ ] 4.3 (M) `AuthRepository.verifyRecoveryCode` (`verifyEmailOtp(RECOVERY)`) y `updatePassword` (`updateUser{password}`) + Impl.
- [ ] 4.4 (L) RED `T/ui/auth/recovery/ResetPasswordViewModelTest.kt`: código inválido, clave débil/confirmación, fallo tras verificar -> `codeVerified=true` solo reintenta clave, reenvío 60 s con `advanceTimeBy`, `isPasswordChanged`. GREEN `ResetPasswordViewModel.kt`. [AC Reenvío, Nueva contraseña x2, Restablecimiento, Estado de carga]
- [ ] 4.5 (M) `ResetPasswordScreen.kt`, destino `ResetPassword(email)`, `koinViewModel { parametersOf(email) }`, éxito -> Login; DI.
- [ ] 4.6 (S) Push, CI verde, PR. Manual: Gherkin HU-004 de extremo a extremo.

## PR 5 (cierre)

- [ ] 5.1 (S) Verificación completa contra los 55 escenarios de las specs en dispositivo; actualizar README si cambió algo; `/sdd-verify`.

**Total: 45 tareas** (PR0 16: 10 S, 5 M, 1 L · PR1 9 · PR2 8 · PR3 5 · PR4 6 · cierre 1). Ruta crítica: 0.5-0.13 -> 0.14/0.15 -> 1.x -> 2.x -> 3.x -> 4.x. Las tareas de coordinación (0.1, 0.15, 0.16, 2.7) bloquean el merge, no el código.
