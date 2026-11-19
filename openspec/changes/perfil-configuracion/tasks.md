# Tasks: Perfiles, configuración y onboarding (HU-005..HU-011, HU-013)

Tamaños: **S** (<1 h), **M** (1-3 h), **L** (>3 h). Base de código: `app/src/main/java/com/groovycow/misionessecundarias/` (abreviado `…/`); tests en `app/src/test/java/com/groovycow/misionessecundarias/` (`T/`).

Reglas globales: TDD estricto (RED -> GREEN -> REFACTOR) con `./gradlew :app:testDebugUnitTest`; KDoc en español en toda API pública y textos de usuario en español con tildes; `ktlintCheck` y `detekt` en verde antes de cada commit; CI (`ktlintCheck -> detekt -> testDebugUnitTest -> assembleDebug`) al abrir cada PR. Sin pruebas Compose en JVM: lo visual se cubre en la matriz manual (sección Cierre).

Trazabilidad (specs en `specs/<capability>/spec.md`): **CCA** = `cambio-contrasena-ajustes` (HU-010), **PI** = `perfil-inicial` (HU-005), **ONB** = `onboarding` (HU-013), **PP** = `perfil-propio` (HU-006/008/009), **PU** = `perfil-publico` (HU-007), **HM** = `historial-misiones` (HU-011). Las decisiones se citan como D1..D18 del `design.md`.

Decisiones aplicadas: D1/D2 reautenticación con `signIn` y `IncorrectCurrentPassword`, sin cerrar sesión; D3 `profile_completed` generada; D4 GRANT UPDATE por columna; D5 unicidad índice + RPC; D6 compresión a JPEG <= 2 MB; D7/D8 bucket `avatars` y CHECK de `photo_url`; D9 avatares con Canvas; D10 gate fail-open; D12 `ProfileFlowDestination`; D13 ficha pública como `ModalBottomSheet`; D14 `PUBLIC_COLUMNS`; D16 historial con 2 consultas; D17 onboarding sin persistencia; D18 `NaturePalette` solo en pantallas de familia B. Fuera de alcance: D11 del plan (privacidad de `full_name`), HU-012, gamificación.

## Flujo de commits (el usuario commitea a mano, un commit por HU)

Reglas para que cada HU sea separable:

- Los archivos **nuevos** de cada HU no se comparten; van en un solo commit de esa HU.
- Los archivos **compartidos** entre HU se listan en cada PR bajo "Compartidos"; usar `git add -p` y tocar en cada commit solo los hunks de esa HU (cada tarea de edición de un compartido es pequeña y aislada para facilitarlo).
- Orden de commits sugerido por PR: primero fundación/dominio, luego UI, y por último el cableado en `AppNavigation.kt`/`SettingsScreen.kt`.
- No se hace commit desde las tareas; marcar `[x]` al terminar y commitear manualmente.

Matriz de archivos compartidos entre HU:

| Archivo | HU que lo tocan |
|---------|-----------------|
| `ui/navigation/AppNavigation.kt` | HU-010 (A), HU-005+HU-013 (C), HU-006/008/009 (D), HU-007 (E), HU-011 (F) |
| `ui/settings/SettingsScreen.kt` | HU-010 (A: Seguridad), HU-013 (C: Ayuda) |
| `domain/validation/FieldError.kt` | HU-010 (A), fundación (B) |
| `ui/auth/AuthMessages.kt`, `domain/model/AuthError.kt`, `di/AuthModule.kt` | solo HU-010 (A); mencionados porque son de Jojhan/Alex |
| `di/ProfileModule.kt` | fundación (B) y cada HU posterior (C..F agregan sus registros) |
| `ui/navigation/ProfileNavigation.kt` | HU-005/013 (C, creación), HU-006/008/009 (D), HU-011 (F) |
| `ui/auth/SessionViewModel.kt` | solo HU-005 (C) |
| `ui/navigation/MainMenuScreen.kt` | solo HU-006 (D) |
| `ui/missions/PostulantesScreen.kt`, `ui/missions/MissionDetailScreen.kt` | solo HU-007 (E) |
| `supabase/schema.sql`, `app/build.gradle.kts`, `gradle/libs.versions.toml`, `di/SupabaseModule.kt`, `MisionesApp.kt` | solo fundación (B) |

## PR A: HU-010 Cambio de contraseña en Ajustes (sin dependencia de BD)

Archivos tocados (nuevos): `…/domain/usecase/ChangeCurrentPasswordUseCase.kt`, `…/ui/settings/ChangePasswordScreen.kt`, `…/ui/settings/ChangePasswordViewModel.kt`, `T/domain/usecase/ChangeCurrentPasswordUseCaseTest.kt`, `T/ui/settings/ChangePasswordViewModelTest.kt`.
Compartidos (editar con `git add -p`): `…/domain/model/AuthError.kt`, `…/domain/repository/AuthRepository.kt`, `…/data/repository/AuthRepositoryImpl.kt`, `…/domain/validation/FieldError.kt` (solo si se necesitan errores nuevos aquí), `…/ui/auth/AuthMessages.kt`, `…/di/AuthModule.kt`, `…/ui/settings/SettingsScreen.kt`, `…/ui/navigation/AppNavigation.kt`. Todo es de HU-010, pero son archivos de compañeros: cambios mínimos y avisarles.

- [x] A.1 (S) `domain/model/AuthError.kt`: agregar `IncorrectCurrentPassword` (data object) con KDoc. [CCA Actual incorrecta; D2]
- [x] A.2 (S) `AuthRepository.currentUserEmail(): String?` + implementación en `AuthRepositoryImpl` (lee la sesión de gotrue-kt); ajustar fakes/mocks existentes de `AuthRepository` en tests. [D1]
- [x] A.3 (M) RED `T/domain/usecase/ChangeCurrentPasswordUseCaseTest.kt`: campos vacíos, contraseña corta, sin clases requeridas, confirmación distinta, nueva igual a la actual (todos con `coVerify(exactly = 0)` sobre el repositorio), correo nulo -> `ServerError`, `InvalidCredentials` del `signIn` -> `IncorrectCurrentPassword`, éxito (orden: `signIn` -> `updatePassword`, sin `signOut`), error de red, error de servidor, `CancellationException` relanzada. GREEN `domain/usecase/ChangeCurrentPasswordUseCase.kt` (valida con `PasswordValidator`, `authCatching {}`). [CCA Campos vacíos, Contraseña corta, Sin clases requeridas, Confirmación distinta, Actual incorrecta, Nueva igual a la actual, Éxito, Sin conexión, Error del servidor; D1, D2]
- [x] A.4 (S) `ui/auth/AuthMessages.kt`: texto "La contraseña actual es incorrecta" para `IncorrectCurrentPassword`. [CCA Actual incorrecta]
- [x] A.5 (M) RED `T/ui/settings/ChangePasswordViewModelTest.kt`: errores por campo sin invocar el caso de uso, `isLoading` evita doble envío, éxito muestra diálogo y no cierra sesión, errores de red/servidor conservan los datos. GREEN `ui/settings/ChangePasswordViewModel.kt` + `ChangePasswordUiState`. [CCA Campos vacíos, Éxito, Sin conexión, Error del servidor]
- [x] A.6 (L) `ui/settings/ChangePasswordScreen.kt`: 3 campos (actual, nueva, confirmación) con Mostrar/Ocultar, errores por campo, estado de carga, diálogo "Contraseña actualizada" que vuelve a Ajustes. Estilo de `SettingsScreen` (aún sin `NaturePalette`, que llega en PR B; usar los colores locales actuales y migrarlos en la fundación si se desea). [CCA Abrir la pantalla, Éxito]
- [x] A.7 (S) `di/AuthModule.kt`: registrar `ChangeCurrentPasswordUseCase` y `ChangePasswordViewModel`. [CCA]
- [x] A.8 (M) `ui/settings/SettingsScreen.kt`: hacerlo desplazable y agregar tarjeta "Seguridad" con param `onChangePassword`. `ui/navigation/AppNavigation.kt`: destino **permanente** `AppDestination.ChangePassword`, su rama y `BackHandler` hacia Ajustes (decisión del orquestador: NO se migra a `ProfileFlowDestination` en PR C; los hunks quedan en el commit HU-010). [CCA Abrir la pantalla]
- [ ] A.9 (S) Verificar en el dashboard de Supabase el estado de "Secure password change" (pregunta abierta; D1 funciona en ambos casos, se recomienda OFF). [CCA Éxito]
- [ ] A.10 (S) Manual: recorrer los 10 escenarios CCA en dispositivo (la sesión sigue activa tras el cambio).
- [ ] A.11 (S) **Coordinación**: avisar a Jojhan/Alex que se tocan `AuthError`, `AuthRepository(Impl)`, `AuthMessages`, `AuthModule`, `SettingsScreen` y `AppNavigation` (solo agregados).
- [ ] A.12 (S) Commit manual `HU-010` (separar con `git add -p` los hunks compartidos), push a la rama, CI verde y PR a `develop`.

## PR B: Fundación (SQL, dependencias, repositorio, validadores, componentes)

Sin HU propia: un solo commit "fundación perfil" (o dos: SQL/dependencias y código). Depende de que Juan aplique el SQL para probar contra BD real, pero las pruebas unitarias no lo necesitan.

Archivos tocados (nuevos): `supabase/changes/2026-11-18_profiles_public_fields.sql`, `supabase/changes/2026-11-18_storage_avatars.sql`, `supabase/changes/2026-11-18_perfil_rollback.sql`, `…/domain/model/{Profile,PublicProfile,Gender,AvatarId,ProfileError}.kt`, `…/domain/repository/{ProfileRepository,PhotoCompressor}.kt`, `…/domain/validation/{NicknameValidator,DescriptionValidator}.kt`, `…/data/repository/{ProfileRepositoryImpl,ProfileErrorMapper}.kt`, `…/data/image/AndroidPhotoCompressor.kt`, `…/domain/usecase/{IsProfileComplete,GetMyProfile,UpdateProfile,UploadProfilePhoto,CompleteInitialProfile,GetPublicProfile}UseCase.kt`, `…/ui/theme/NaturePalette.kt`, `…/ui/profile/components/{ProfileAvatar,AvatarPicker,ProfilePhoto,GenderSelector}.kt`, `…/ui/profile/ProfileMessages.kt`, `…/di/ProfileModule.kt` y sus tests en `T/`.
Compartidos: `supabase/schema.sql`, `gradle/libs.versions.toml`, `app/build.gradle.kts`, `…/di/SupabaseModule.kt`, `…/MisionesApp.kt`, `…/domain/validation/FieldError.kt` (comparte con A: solo agregar variantes nuevas).

- [x] B.1 (M) SQL `2026-11-18_profiles_public_fields.sql` (idempotente, encabezado Cambio/Fecha/Autor): columnas `nickname`, `gender`, `description`, `avatar_id`, `photo_url` con CHECKs (D8 incluido), `profile_completed` generada (D3), índice único `lower(nickname)`, `GRANT UPDATE` por columna (D4), RPC `nickname_disponible` (security definer, `search_path ''`, excluye `auth.uid()`, solo `authenticated`), `notify pgrst, 'reload schema'`. [PI Nickname único, Gate; D3-D5, D8]
- [x] B.2 (M) SQL `2026-11-18_storage_avatars.sql`: bucket `avatars` público, 2097152 bytes, solo `image/jpeg`, políticas select/insert/update/delete con `(storage.foldername(name))[1] = auth.uid()::text`. [PI Foto grande aceptada; D7]
- [x] B.3 (S) SQL `2026-11-18_perfil_rollback.sql` (quita RPC, grants, índice, columnas, políticas, bucket; nota: borrar antes los objetos del bucket) y espejo del estado final en `supabase/schema.sql`.
- [ ] B.4 (S) **Coordinación**: entregar a Juan (tech lead) los 3 scripts y pedir que aplique primero el SQL pendiente del Sprint 1 y luego B.1 y B.2 en ese orden; confirmar aplicación antes de las pruebas manuales de C..F. No aplicar unilateralmente.
- [x] B.5 (S) `gradle/libs.versions.toml` + `app/build.gradle.kts`: `supabase-storage` (`storage-kt:2.6.0`) y `coil-compose:2.7.0`; `di/SupabaseModule.kt`: `install(Storage)`. Verificar `assembleDebug` (AGP 8.5.0 intacto). [D7]
- [x] B.6 (M) Modelos de dominio con KDoc: `Gender`, `AvatarId` (8 entradas, `key`, color de `NaturePalette` + ícono Material en UI, no en dominio), `Profile`, `PublicProfile`, `ProfileUpdate`, `ProfileError`, `ProfileException`. [D9]
- [x] B.7 (M) RED `T/domain/validation/NicknameValidatorTest.kt` (límites 2/3/20/21, vacío, caracteres inválidos) y `DescriptionValidatorTest.kt` (280/281, vacío permitido). GREEN validadores puros + `FieldError`: `NicknameLength`, `NicknameInvalidChars`, `DescriptionTooLong`, `SelectionRequired`. [PI Muy corto o largo, Caracteres inválidos, Excede el límite, Sin descripción]
- [x] B.8 (M) Interfaces `ProfileRepository` y `PhotoCompressor`. RED `T/data/repository/ProfileErrorMapperTest.kt` (`23505` -> `NicknameTaken`, red -> `NoConnection`, resto -> `ServerError`) y `ProfileRepositoryImplTest.kt` que verifica `PUBLIC_COLUMNS == listOf("id","nickname","gender","description","avatar_id","photo_url")`. GREEN `ProfileErrorMapper` y `ProfileRepositoryImpl.PUBLIC_COLUMNS`. [PU Columnas consultadas; D5, D14]
- [x] B.9 (L) `ProfileRepositoryImpl`: `isProfileComplete`, `getMyProfile`, `isNicknameAvailable` (RPC), `uploadPhoto` (`{uid}/foto.jpg`, `upsert = true`, `publicUrl + "?v=<epochMillis>"`; resolver la pregunta abierta del `contentType` explícito), `updateProfile` (`photoUrl` nulo = no tocar), `getPublicProfile` con `Columns.list(PUBLIC_COLUMNS)`; DTOs `@Serializable` privados, `withContext(Dispatchers.IO)`, mapear excepciones a `ProfileException`. [D5, D7, D14]
- [x] B.10 (L) RED `T/data/image/` (lógica pura extraíble: selección de calidad 85->45 y escalado <= 1080 px) y `AndroidPhotoCompressor`: solo `image/jpeg`/`image/png` por `ContentResolver.getType` (resto -> `UnsupportedPhotoFormat`), `inSampleSize`, <= 2 MB o `PhotoTooLarge`. [PI Foto grande aceptada, Formato no soportado, Imposible reducir; D6]
- [x] B.11 (L) RED `T/domain/usecase/` para `IsProfileCompleteUseCaseTest`, `GetMyProfileUseCaseTest`, `GetPublicProfileUseCaseTest`, `UploadProfilePhotoUseCaseTest`, `UpdateProfileUseCaseTest` (valida campos y disponibilidad de nickname propio, ignora el propio) y `CompleteInitialProfileUseCaseTest` (orden: validar -> `nickname_disponible` -> `uploadPhoto` -> `updateProfile`; `coVerify(exactly = 0)` ante error de validación/nickname en uso; `CancellationException` relanzada). GREEN los casos de uso con `Result<T>`/`authCatching`. [PI Faltan obligatorios, En uso, Perfil completo, Error al guardar, Sin conexión al guardar; PP Nickname propio, Nickname de otro; D5]
- [x] B.12 (S) `ui/theme/NaturePalette.kt` (`object NaturePalette`). [D18]
- [x] B.13 (L) `ui/profile/components/`: `ProfileAvatar` (Canvas al estilo `GameAvatar`), `AvatarPicker`, `ProfilePhoto` (Coil `AsyncImage`, placeholder `Icons.Default.Person`), `GenderSelector` (3 opciones); `ui/profile/ProfileMessages.kt` (textos de errores y etiquetas). Previews opcionales. [PI Opciones disponibles; PU Sin foto; D9, D18]
- [x] B.14 (S) `di/ProfileModule.kt` (repositorio, compresor, casos de uso) y registrarlo en `MisionesApp.kt`.
- [ ] B.15 (S) Commit manual de fundación (SQL/Gradle separados del código si se prefiere), push, CI verde y PR a `develop`.

## PR C: HU-005 Perfil inicial + gate, y HU-013 Onboarding

Dos commits: `HU-005` (perfil inicial + gate) y `HU-013` (onboarding). Depende de PR B y del SQL aplicado (B.4).

Archivos tocados HU-005 (nuevos): `…/ui/profile/setup/InitialProfileScreen.kt`, `…/ui/profile/setup/InitialProfileViewModel.kt`, `T/ui/profile/setup/InitialProfileViewModelTest.kt`, `T/ui/auth/SessionViewModelTest.kt` (extender/crear), `…/ui/navigation/ProfileNavigation.kt`.
Archivos tocados HU-013 (nuevos): `…/ui/onboarding/OnboardingScreen.kt`.
Compartidos: `…/ui/auth/SessionViewModel.kt` (HU-005), `…/ui/navigation/AppNavigation.kt` (HU-005: `InitialProfile` real, `CheckingProfile`, rama `ProfileFlowDestination`, retiro del `BackHandler`; HU-013: destino `Onboarding` tras el guardado), `…/ui/settings/SettingsScreen.kt` (HU-013: tarjeta "Ayuda" y `onOpenGuide`; el hunk de "Seguridad" de PR A ya está commiteado), `…/di/ProfileModule.kt` (VMs). `ChangePassword` ya no se migra: queda como destino permanente de HU-010.

- [ ] C.1 (M) RED `T/ui/auth/SessionViewModelTest.kt`: arranque en frío con sesión completa -> `MAIN_MENU`, incompleta -> `PROFILE_SETUP`, fallo de consulta -> `MAIN_MENU`; `checkProfileGate(onResult)` con completo/incompleto/fallo. GREEN `SessionViewModel` recibe `IsProfileCompleteUseCase`. [PI Login con perfil incompleto, Arranque en frío incompleto, Perfil completo, Offline en el gate; D10]
- [ ] C.2 (L) RED `T/ui/profile/setup/InitialProfileViewModelTest.kt`: errores por campo sin backend, nickname en uso, descripción opcional, foto no soportada/demasiado grande/imposible de reducir, `isLoading` evita doble envío, éxito emite navegación, error y sin conexión conservan datos. GREEN `InitialProfileViewModel` + `InitialProfileUiState`. [PI Faltan obligatorios, Sin descripción, Muy corto o largo, Caracteres inválidos, En uso, Excede el límite, Foto grande aceptada, Formato no soportado, Imposible reducir, Perfil completo, Error al guardar, Sin conexión al guardar]
- [ ] C.3 (L) `ui/profile/setup/InitialProfileScreen.kt` ("Crea tu perfil", familia A con `AuthVisualStyle`): nickname, `GenderSelector`, descripción con contador, `AvatarPicker`, selector de foto (picker de imágenes), "Guardar Perfil". Reemplaza el placeholder. [PI Opciones disponibles, Perfil completo; D18]
- [ ] C.4 (M) `ui/navigation/AppNavigation.kt`: `AppDestination.CheckingProfile` (spinner) tras `onSignedIn`; registro va directo a `InitialProfile`; eliminar el `BackHandler` de `InitialProfile` (Atrás finaliza la Activity). [PI Login con perfil incompleto, Registro nuevo, Atrás en perfil inicial; D10, D11]
- [ ] C.5 (M) `ui/navigation/ProfileNavigation.kt` + `sealed interface ProfileFlowDestination : AppDestination` (`MyProfile`, `EditProfile`, `MissionHistory`, `Onboarding(fromSettings)`) enrutada desde una sola rama de `AppNavigation`; `ChangePassword` permanece como `AppDestination.ChangePassword` (ya no se migra; se quita de `ProfileFlowDestination`). Verificar que `shouldRedirectToWelcome` los protege (`else -> true`). [D12]
- [ ] C.6 (S) Manual HU-005: login incompleto, registro nuevo, arranque en frío (matar la app), perfil completo salta, offline pasa al menú, Atrás en perfil inicial sin atrapar al usuario (el gate lo reabre).
- [ ] C.7 (S) **Coordinación**: avisar a Jojhan/Alex (`AppNavigation`, `SessionViewModel`) y a QA que los usuarios existentes pasarán por HU-005 al ingresar (esperado).
- [ ] C.8 (S) Commit manual `HU-005`.
- [ ] C.9 (M) `ui/onboarding/OnboardingScreen.kt` (familia A): `HorizontalPager` de 4 páginas, indicadores, Omitir / Siguiente / Empezar (última página sin Omitir), param `fromSettings` para cerrar con volver a Ajustes en lugar de ir al menú. Sin persistencia. [ONB Primer ingreso, Avanzar, Última página, Empezar, Omitir tras perfil inicial, Cerrar desde Ajustes; D17]
- [ ] C.10 (S) `AppNavigation`/`ProfileNavigation`: tras guardar `InitialProfile` -> `Onboarding(fromSettings = false)` -> `MainMenu`. [ONB Aparición tras el perfil inicial, Primer ingreso]
- [ ] C.11 (S) `ui/settings/SettingsScreen.kt`: tarjeta "Ayuda" con `onOpenGuide` que abre `Onboarding(fromSettings = true)`. [ONB Reabrir]
- [ ] C.12 (S) Manual HU-013: pager, Omitir, Empezar, reapertura y cierre desde Ajustes.
- [ ] C.13 (S) Commit manual `HU-013`, push, CI verde y PR a `develop`.

## PR D: HU-006 Mi perfil, HU-008 Edición, HU-009 Foto

Tres commits (`HU-006`, `HU-008`, `HU-009`). Depende de PR C (destinos `ProfileFlowDestination`).

Archivos tocados HU-006 (nuevos): `…/ui/profile/me/MyProfileScreen.kt`, `…/ui/profile/me/MyProfileViewModel.kt`, `…/ui/profile/ProfileSummaryViewModel.kt`, `T/ui/profile/me/MyProfileViewModelTest.kt`, `T/ui/profile/ProfileSummaryViewModelTest.kt`.
Archivos tocados HU-008 (nuevos): `…/ui/profile/edit/EditProfileScreen.kt`, `…/ui/profile/edit/EditProfileViewModel.kt` (parte de datos), `T/ui/profile/edit/EditProfileViewModelTest.kt` (parte de datos).
Archivos tocados HU-009: la parte de foto vive en los mismos `EditProfile*` (ver nota en D.5) y `ProfileMessages.kt`.
Compartidos: `…/ui/navigation/MainMenuScreen.kt` (HU-006: param `avatar`; **Jojhan**), `…/ui/navigation/AppNavigation.kt` (HU-006: mapeo `PROFILE/AVATAR` y `refresh()` del saludo; HU-008: destino `EditProfile`), `…/ui/navigation/ProfileNavigation.kt`, `…/di/ProfileModule.kt`, `…/ui/profile/ProfileMessages.kt` (HU-008/HU-009).

- [ ] D.1 (M) RED `T/ui/profile/me/MyProfileViewModelTest.kt`: carga correcta, sin descripción, error de carga con reintento, nunca muestra nivel distinto de "Nivel 1". GREEN `MyProfileViewModel` + estado. [PP Visualización, Sin descripción, Error de carga]
- [ ] D.2 (L) `ui/profile/me/MyProfileScreen.kt` (familia B con `NaturePalette`): foto/avatar, nickname, género, descripción, "Nivel 1" fijo sin marco, accesos a Editar y a Historial (el acceso a Historial queda inactivo/oculto hasta PR F o apunta a destino ya definido). [PP Visualización; D18]
- [ ] D.3 (M) RED `T/ui/profile/ProfileSummaryViewModelTest.kt` (`refresh()` actualiza nickname/avatar/foto) y GREEN `ProfileSummaryViewModel`. `MainMenuScreen.kt`: param opcional `avatar: (@Composable () -> Unit)?` en lugar del ícono genérico; `AppNavigation`: `refresh()` en `LaunchedEffect` al entrar a `MainMenu` y mapeo `PROFILE`/`AVATAR` a `MyProfile`. [PP Saludo actualizado; D15]
- [ ] D.4 (S) **Coordinación**: avisar a Jojhan del cambio en `MainMenuScreen`; Commit manual `HU-006` (incluye D.1-D.3).
- [ ] D.5 (L) RED `T/ui/profile/edit/EditProfileViewModelTest.kt` (datos): precarga, edición exitosa, campo obligatorio vacío, nickname propio permitido, nickname de otro rechazado, `isLoading`, errores de red/servidor. GREEN `EditProfileViewModel` (datos) + `EditProfileScreen` (familia B, `GenderSelector`, `AvatarPicker`, contador). [PP Edición exitosa, Campo obligatorio vacío, Nickname propio, Nickname de otro]
- [ ] D.6 (S) `AppNavigation`/`ProfileNavigation`: destino `EditProfile` y regreso a Mi perfil con datos refrescados. Commit manual `HU-008`.
- [ ] D.7 (M) RED (en `EditProfileViewModelTest`, bloque de foto): foto nueva comprime y sube, formato no soportado, demasiado grande, sin conexión conserva la foto actual. GREEN lógica de foto en `EditProfileViewModel`/`UploadProfilePhotoUseCase` y selector en `EditProfileScreen`. [PP Foto nueva, Formato no soportado, Foto demasiado grande, Sin conexión]
- [ ] D.8 (S) Manual HU-006/008/009: Gherkin completo, foto de cámara > 2 MB, PNG, formato GIF, avión, saludo del menú actualizado. Commit manual `HU-009` (hunks de foto vía `git add -p` si comparte archivos con HU-008), push, CI verde y PR a `develop`.

## PR E: HU-007 Perfil público

Un commit `HU-007`. Depende de PR B (`getPublicProfile`) y de D (opcional).

Archivos tocados (nuevos): `…/ui/profile/public/PublicProfileSheet.kt`, `…/ui/profile/public/PublicProfileViewModel.kt`, `T/ui/profile/public/PublicProfileViewModelTest.kt`.
Compartidos (archivos de Alex): `…/ui/missions/PostulantesScreen.kt`, `…/ui/missions/MissionDetailScreen.kt`, `…/ui/navigation/AppNavigation.kt` (hunk de `publicProfileUserId` y `PublicProfileSheet`), `…/di/ProfileModule.kt`.

- [ ] E.1 (M) RED `T/ui/profile/public/PublicProfileViewModelTest.kt`: carga correcta, sin foto (avatar), error de carga con reintento, sin conexión, no expone campos privados (el estado solo tiene los de `PublicProfile`). GREEN `PublicProfileViewModel`. [PU Sin foto, Error de carga, Sin conexión, Sin datos privados en pantalla]
- [ ] E.2 (S) Reforzar prueba de `PUBLIC_COLUMNS` de PR B: que `getPublicProfile` use exactamente esas columnas y que ninguna sea `full_name`, `email` ni `role`. [PU Columnas consultadas; D14]
- [ ] E.3 (L) `ui/profile/public/PublicProfileSheet.kt` (`ModalBottomSheet`, familia B): foto/avatar, nickname, género, descripción, "Nivel 1", solo lectura; cerrar al deslizar/atrás. [PU Cerrar, Sin datos privados en pantalla; D13, D18]
- [ ] E.4 (M) `PostulantesScreen` y `MissionDetailRoute`: callbacks opcionales `onApplicantClick(solverId)` / `onPublisherClick()` con default `{}`; `AppNavigation`: `var publicProfileUserId` y sheet superpuesto. No tocar DTOs con `full_name`. [PU Desde postulantes, Desde el detalle; D13]
- [ ] E.5 (S) **Coordinación**: avisar a Alex de los cambios en `PostulantesScreen` y `MissionDetailScreen` (solo parámetros opcionales y `clickable`).
- [ ] E.6 (S) Manual HU-007: abrir desde postulantes y desde el detalle, perfil sin foto, sin conexión, verificar que no aparece `full_name`. Commit manual `HU-007`, push, CI verde y PR a `develop`.

## PR F: HU-011 Historial de misiones

Un commit `HU-011`. Depende de PR D (acceso desde Mi perfil).

Archivos tocados (nuevos): `…/domain/model/MissionHistoryEntry.kt` (+ `HistoryRole`), `…/domain/repository/MissionHistoryRepository.kt`, `…/data/repository/MissionHistoryRepositoryImpl.kt`, `…/domain/usecase/GetMissionHistoryUseCase.kt`, `…/ui/profile/history/MissionHistoryScreen.kt`, `…/ui/profile/history/MissionHistoryViewModel.kt`, `T/domain/usecase/GetMissionHistoryUseCaseTest.kt`, `T/ui/profile/history/MissionHistoryViewModelTest.kt`.
Compartidos: `…/ui/navigation/ProfileNavigation.kt` y `…/ui/navigation/AppNavigation.kt` (rama `MissionHistory`), `…/ui/profile/me/MyProfileScreen.kt` (enlace de HU-006, ya existente), `…/di/ProfileModule.kt`.

- [ ] F.1 (S) Modelos `MissionHistoryEntry`, `HistoryRole` e interfaz `MissionHistoryRepository` con KDoc. [D16]
- [ ] F.2 (M) RED `T/domain/usecase/GetMissionHistoryUseCaseTest.kt`: mezcla de roles, borradores excluidos, sin duplicados por `missionId` (publicador gana o regla definida), orden por `dateTime` descendente, historial vacío, error del repositorio, `CancellationException`. GREEN `GetMissionHistoryUseCase`. [HM Mezcla de roles, Borradores excluidos, Sin duplicados, Historial vacío; D16]
- [ ] F.3 (M) `MissionHistoryRepositoryImpl`: consulta de misiones propias `status <> 'Borrador'` y de postulaciones `Aceptado` con `missions(...)` embebido; DTOs privados, `withContext(Dispatchers.IO)`; registrar en `ProfileModule`. [HM Borradores excluidos; D16]
- [ ] F.4 (M) RED `T/ui/profile/history/MissionHistoryViewModelTest.kt`: carga, expandir/contraer una tarjeta, secciones "Bitácora oficial" y "Palabras de participantes" vacías, error de carga con reintento, sin conexión. GREEN `MissionHistoryViewModel`. [HM Expandir, Secciones vacías, Contraer, Error de carga, Sin conexión]
- [ ] F.5 (L) `ui/profile/history/MissionHistoryScreen.kt` (familia B): línea temporal, tarjetas expandibles (título, descripción, requisitos, rol, estado), estado vacío y error. [HM Expandir, Secciones vacías, Contraer, Historial vacío; D18]
- [ ] F.6 (S) `ProfileNavigation`/`AppNavigation`: rama `MissionHistory` y acceso desde Mi perfil y el mapeo `HISTORY` del menú. [HM Desde Mi perfil]
- [ ] F.7 (S) Manual HU-011: Gherkin completo (usuario con ambos roles, vacío, sin conexión). Commit manual `HU-011`, push, CI verde y PR a `develop`.

## Cierre

- [ ] Z.1 (M) Matriz de pruebas en dispositivo (compresión de fotos cámara/PNG/GIF, Coil con caché por `?v=`, bucket y políticas, pager, `BackHandler`/Atrás del gate, gate offline, perfil existente incompleto) con el SQL de B ya aplicado en la BD compartida. Anotar resultados en el PR o en `design.md`.
- [ ] Z.2 (S) Checklist manual de Gherkin por spec: CCA (10), PI (19), ONB (7), PP (12), PU (8), HM (10); marcar las que pasen.
- [ ] Z.3 (S) Ejecutar localmente los 4 pasos de CI (`ktlintCheck`, `detekt`, `testDebugUnitTest`, `assembleDebug`) sobre la rama integrada.
- [ ] Z.4 (S) Avisos: QA (usuarios existentes pasan por HU-005), Juan (SQL aplicado), Jojhan y Alex (archivos tocados), equipo (D11 del plan sobre `full_name` sigue pendiente).
- [ ] Z.5 (M) `/sdd-verify perfil-configuracion` contra specs, design y tasks; tratar CRITICAL antes de mergear.
- [ ] Z.6 (S) Cerrar HU-005..HU-011 y HU-013 al mergear a `develop`; luego `/sdd-archive`.
