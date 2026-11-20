# Plan de implementación — Perfiles y configuración (Sprint 2)

**Responsable:** David Chicaiza · **HU:** 005, 006, 007, 008, 009, 010, 011, 013
**Base:** `develop` @ 779946b (PR #50) · **Rama:** `dev/david-chicaiza` (sub-ramas `hu/HU-XXX` opcionales)
**Estado:** borrador para revisión. Las decisiones marcadas con ❓ deben confirmarse antes de escribir las specs.

---

## 1. Punto de partida (análisis de la rama)

| Área | Estado actual | Impacto |
|---|---|---|
| Modelo de perfil | No existe `Profile` ni `ProfileRepository`. `profiles` tiene solo `id, full_name, first_name, last_name, avatar_url (sin uso), role, created_at` | Hay que crear el dominio completo |
| Permisos BD | `profiles` tiene política RLS de UPDATE propio, pero **solo `GRANT SELECT`**: cualquier edición fallará | SQL nuevo con `GRANT UPDATE (columnas)` |
| Fotos | Sin `storage-kt`, sin Coil, sin bucket ni políticas | Agregar dependencias y bucket `avatars` |
| Navegación | `AppDestination.InitialProfile` existe como placeholder. Register lo usa, pero Login y el arranque en frío van directo a `MainMenu` | Hace falta un **gate de perfil completo** en los 3 puntos de entrada |
| Menú | `MainMenuDestination.PROFILE / AVATAR / HISTORY` caen en `Placeholder`. `MainMenuScreen(username)` nunca recibe el nombre | Conectar con las pantallas nuevas |
| Ajustes | Solo la tarjeta "Cuenta" con "Cerrar sesión", contenido centrado y sin scroll | Agregar secciones Seguridad y Ayuda |
| Auth | `updatePassword` existe. `ChangePasswordUseCase` cierra sesión (diseñado para recuperación). No hay forma de validar la contraseña actual | Caso de uso nuevo para HU-010 |
| Postulantes | `ApplicantCard` en `PostulantesScreen.kt` no es clickeable. `MissionApplicant.solverId` está disponible | Punto de enganche de HU-007 |
| Historial | Solo hay `date_time` y `status`. **No existen** bitácora ni "palabras de participantes", y nada pasa misiones a `Completada` | HU-011 se implementa parcial (ver §4) |
| Persistencia local | No hay DataStore ni SharedPreferences | HU-013 se resuelve sin persistencia local (ver D6) |

**Estética vigente** (se conserva, sin refactorizar archivos de otros):
- **Familia A, "auth"** (`ui/auth/AuthVisualStyle.kt`): fondo `login_cali_background` con degradado, `AuthPanel` verde bosque con borde dorado, botón `GameGold` / `ForestInk`.
- **Familia B, "naturaleza"** (menú, misiones, ajustes): `Scaffold` en `SandBackground`, `TopAppBar` con flecha `EarthOrange`, tarjetas `CreamSurface` con borde de 3.dp en color semántico, botones de 12–14.dp de radio con texto `ExtraBold`.
- **Tipografía:** pixel fonts vía `MaterialTheme.typography` (Minecraft y Press Start 2P), se heredan gratis.

---

## 2. Decisiones de diseño propuestas

| # | Decisión | Propuesta |
|---|---|---|
| D1 | Columnas nuevas en `profiles` | `nickname` (3–20 caracteres `[A-Za-z0-9_]`, único sin distinguir mayúsculas), `gender`, `description` (≤ 280), `avatar_id`, `photo_url` y `profile_completed` como **columna generada** (`nickname`, `gender`, `avatar_id` y `photo_url` no nulos). Todas nullable para no romper filas existentes |
| D2 ❓ | Valores de género | `femenino`, `masculino`, `no_binario`, `prefiero_no_decir` (CHECK en BD) |
| D3 ❓ | Avatar "personalizable" | Catálogo de **avatares predefinidos dibujados con Canvas**, con el mismo estilo que `GameAvatar` (anillos de color + ícono), guardados como `avatar_id`. Sin assets nuevos. La personalización avanzada (marcos, nivel) queda fuera, porque no hay modelo de gamificación |
| D4 ❓ | Foto pública | Bucket `avatars` con lectura pública, ruta `{uid}/foto.jpg`, escritura solo en la carpeta propia (RLS en `storage.objects`), **límite 2 MB**, MIME `image/jpeg` o `image/png` validado en la app y en el bucket. Selección con `PickVisualMedia` (Photo Picker, no requiere permisos en minSdk 29+ vía backport) |
| D5 | Gate de perfil | `SessionViewModel` agrega `StartDestination.PROFILE_SETUP`. Después de login, de registro y en el arranque en frío se consulta `profile_completed`: si es falso, se va a `InitialProfile`. El `BackHandler` de `InitialProfile` deja de ir a `MainMenu` (sale al cerrar sesión o al minimizar) |
| D6 | Onboarding (HU-013) | Se muestra **inmediatamente después de guardar el perfil inicial**. Como la configuración ocurre una sola vez, no hace falta persistir "ya visto". Se puede reabrir desde Ajustes → Ayuda → "Ver guía". 4 páginas con botones Omitir, Siguiente y Empezar |
| D7 ❓ | Cambio de contraseña (HU-010) | `ChangeCurrentPasswordUseCase`: valida los campos, **reautentica** con `signIn(emailActual, contraseñaActual)` (error: "La contraseña actual es incorrecta"), verifica que la nueva sea distinta y llama `updatePassword`. **Mantiene la sesión**. Requiere que "Secure password change" esté OFF en el dashboard, o usar el nonce de reautenticación |
| D8 ❓ | Historial (HU-011) | Línea de tiempo con misiones **publicadas por mí** más **postulaciones aceptadas**, ordenadas por `date_time` descendente, con chip de rol (Publicador o Solucionador) y estado. Al expandir: fecha, título, descripción y requerimientos. "Bitácora" y "Palabras de participantes" se muestran como secciones vacías ("Aún no hay registros") hasta que existan sus HU, que no son de este sprint |
| D9 | Perfil de terceros (HU-007) | Pantalla o `ModalBottomSheet` de solo lectura con foto, nickname, género, descripción y avatar. Se abre desde `ApplicantCard` y desde `PublisherCard` del detalle de misión. **Nunca** se seleccionan columnas privadas |
| D10 | Paleta compartida | Crear `ui/theme/NaturePalette.kt` con los colores de la familia B y usarla **solo en pantallas nuevas**. No tocar las copias privadas de los compañeros para evitar conflictos (refactor propuesto aparte) |
| D11 ⚠️ | Privacidad | `profiles` es legible por `anon` e incluye `first_name`, `last_name` y `full_name`. HU-007 exige exponer solo datos públicos. Propuesta: mostrar `nickname` en lugar de `full_name` en las vistas públicas y revocar `SELECT` a `anon`. **Hay que coordinarlo con Alex y Jojhan**, porque sus pantallas leen `full_name` |

---

## 3. Arquitectura (capas existentes)

```
domain/model/        Profile, PublicProfile, Gender, AvatarId, MissionHistoryEntry, HistoryRole
domain/repository/   ProfileRepository, MissionHistoryRepository (o métodos en MissionRepository)
domain/validation/   NicknameValidator, DescriptionValidator, ProfilePhotoValidator (+ FieldError nuevos)
domain/usecase/      GetMyProfileUseCase, IsProfileCompleteUseCase, CompleteInitialProfileUseCase,
                     UpdateProfileUseCase, UploadProfilePhotoUseCase, GetPublicProfileUseCase,
                     ChangeCurrentPasswordUseCase, GetMissionHistoryUseCase
data/repository/     ProfileRepositoryImpl (postgrest + storage), MissionHistoryRepositoryImpl
                     + CurrentUserProvider (unifica los authenticatedUserId() duplicados; opcional)
ui/profile/setup/    InitialProfileScreen/ViewModel        (familia A, continúa desde Registro)
ui/profile/me/       MyProfileScreen/ViewModel             (familia B)
ui/profile/edit/     EditProfileScreen/ViewModel           (familia B, incluye foto)
ui/profile/public/   PublicProfileSheet/ViewModel          (familia B)
ui/profile/history/  MissionHistoryScreen/ViewModel        (familia B, timeline)
ui/profile/components/ AvatarPicker, ProfileAvatar (Canvas), ProfilePhoto (Coil), GenderSelector
ui/onboarding/       OnboardingScreen                      (familia A, 4 páginas, HorizontalPager)
ui/settings/         + sección Seguridad → ChangePasswordScreen/ViewModel; sección Ayuda → Onboarding
di/ProfileModule.kt  registrado en MisionesApp
```

**Navegación** (`AppNavigation.kt`), con destinos nuevos:
- `MyProfile`, `EditProfile`, `MissionHistory`, `ChangePassword` y `Onboarding(fromSettings: Boolean)`
- `PublicProfile(userId: String, returnTo: AppDestination)`

Todos son protegidos automáticamente por `shouldRedirectToWelcome`. Mapear `PROFILE → MyProfile`, `AVATAR → EditProfile` y `HISTORY → MissionHistory`. `MainMenuScreen` recibe `username = nickname`; es un cambio mínimo en el archivo de Jojhan, que hay que avisarle.

**Dependencias nuevas** (`libs.versions.toml`):
- `io.github.jan-tennert.supabase:storage-kt:2.6.0`
- `io.coil-kt:coil-compose:2.7.0`

Se mantiene AGP 8.5.0.

**SQL** (siguiendo la convención):
- `supabase/changes/2026-11-18_profiles_public_fields.sql` (idempotente), con espejo en `schema.sql`:
  - columnas y CHECKs de D1/D2
  - índice único `lower(nickname)`
  - `GRANT UPDATE (nickname, gender, description, avatar_id, photo_url) ON profiles TO authenticated`
  - RPC `nickname_disponible(p_nickname)` que devuelve un booleano
- `supabase/changes/2026-11-18_storage_avatars.sql` para el bucket y las políticas.
- Rollback correspondiente para ambos.

---

## 4. Plan por PR (orden recomendado)

Cada tarea sigue **TDD estricto** (RED → GREEN → REFACTOR), con KDoc en español y ktlint, detekt y pruebas en verde antes del PR.

### PR A: HU-010, cambiar contraseña desde Ajustes · *sin bloqueos de BD, se puede empezar ya*
1. `FieldError.CurrentPasswordRequired`, `NewPasswordSameAsCurrent` y mensajes en `AuthMessages`.
2. `ChangeCurrentPasswordUseCase` (MockK). Casos: actual incorrecta, nueva igual a la actual, débil, no coincide, sin conexión, éxito sin cerrar sesión.
3. `AuthRepository.currentUserEmail()` en la interfaz y la implementación.
4. `ChangePasswordViewModel` con plantilla de `ResetPasswordViewModel`.
5. `ChangePasswordScreen` (familia B):
   - 3 campos con "Mostrar/Ocultar"
   - botón "Actualizar Contraseña"
   - diálogo de éxito "Contraseña actualizada"
6. `SettingsScreen`:
   - contenido con scroll y alineado arriba
   - tarjeta **"Seguridad"** (borde `RiverTeal`) con "Cambiar contraseña"
   - la tarjeta "Cuenta" se mantiene

### PR B: fundación de perfil · *requiere que Juan aplique el SQL*
1. SQL, bucket y rollback. Actualizar `schema.sql` y README de supabase.
2. Dependencias storage-kt y Coil. Instalar `Storage` en `SupabaseModule`.
3. Modelos, `ProfileRepository` e implementación (DTO privados, `withContext(IO)`). Pruebas del mapeo.
4. Validadores: `NicknameValidator`, `DescriptionValidator` y `ProfilePhotoValidator` (MIME y ≤ 2 MB).
5. Componentes UI reutilizables:
   - `ProfileAvatar` (Canvas por `avatar_id`)
   - `AvatarPicker` (grilla)
   - `ProfilePhoto` (Coil, con placeholder `Icons.Default.Person`)
   - `GenderSelector` (chips)
   - `NaturePalette.kt`
6. `ProfileModule` en Koin.

### PR C: HU-005 configuración inicial + HU-013 onboarding
1. `CompleteInitialProfileUseCase`: valida, revisa disponibilidad del nickname, sube la foto y luego actualiza el perfil. Si la actualización falla, conserva la foto subida (ruta fija, se sobrescribe).
2. `InitialProfileViewModel` y `InitialProfileScreen` (familia A, título "Crea tu perfil"):
   - foto + avatar + nickname + género + descripción
   - botón "Guardar Perfil"
3. **Gate**:
   - `IsProfileCompleteUseCase`
   - `StartDestination.PROFILE_SETUP`
   - redirecciones en login, registro y arranque en frío
   - `BackHandler` corregido
   - pruebas de `SessionViewModel`
4. `OnboardingScreen` con 4 páginas (Misiones de campo, Misiones remotas, Postulaciones, Moderación y convivencia), indicadores de página y botones "Omitir", "Siguiente" y "Empezar". El destino final es `MainMenu`, o `Settings` si se abrió desde Ayuda.
5. Ajustes: tarjeta **"Ayuda"** (borde `SunYellow`) con "Ver guía de la app".

### PR D: HU-006 Mi perfil + HU-008 editar + HU-009 foto
1. `GetMyProfileUseCase` y `MyProfileViewModel`/`MyProfileScreen`:
   - tarjeta de cabecera con foto + avatar + nickname + "Nivel 1" (fijo, sin modelo de nivel)
   - tarjetas Género y Descripción
   - botones "Editar Perfil" e "Histórico de Misiones"
2. `UpdateProfileUseCase` y `EditProfileViewModel`/`EditProfileScreen`:
   - mismos validadores
   - el nickname propio no cuenta como "en uso"
   - botón "Guardar Cambios"
   - al guardar vuelve a Mi Perfil recargado
3. `UploadProfilePhotoUseCase` dentro de Editar con "Cambiar foto" (Photo Picker). Errores: formato no soportado, supera 2 MB, sin conexión.
4. Menú principal: saludo con el nickname y `ProfileAvatar` real en lugar del ícono genérico (coordinar con Jojhan).

### PR E: HU-007 perfil público de otro usuario
1. `GetPublicProfileUseCase(userId)`: selecciona solo columnas públicas. Hay una prueba que verifica el `select`.
2. `PublicProfileSheet` (`ModalBottomSheet`, familia B), solo lectura.
3. Enganches:
   - `ApplicantCard` clickeable (foto y nombre)
   - `PublisherCard` del detalle de misión
   - el select de postulantes amplía a `nickname, avatar_id, photo_url`
4. La lista de "Participantes" del detalle no existe todavía. Queda pendiente cuando se implemente (y su RLS solo permite que la lean el publicador y el propio solucionador).

### PR F: HU-011 histórico de misiones
1. Repositorio: `missions where publisher_id = yo and status <> 'Borrador'` más `applications where solver_id = yo and status = 'Aceptado'`, combinados.
2. `GetMissionHistoryUseCase`: une, elimina duplicados y ordena por fecha descendente.
3. `MissionHistoryScreen`:
   - timeline vertical: línea `MapLine` con nodos coloreados por estado
   - tarjetas expandibles con fecha, título, chip de rol, descripción y requerimientos
   - secciones "Bitácora oficial" y "Palabras de participantes" en estado vacío
   - estado vacío general: "Aún no tienes misiones en tu historial"

---

## 5. Dependencias y coordinación

| Con | Qué |
|---|---|
| **Juan** (BD) | Aplicar **primero** el SQL pendiente del Sprint 1 (`2026-11-03_*`, la Edge Function y la configuración del dashboard). Después, los scripts de PR B y crear el bucket. Verificar que "Secure password change" está OFF |
| **Jojhan** | PR A y C tocan `SettingsScreen`, `SessionViewModel` y `MainMenuScreen` |
| **Alex** | PR E toca `PostulantesScreen`, `MissionDetailScreen` y los selects de `ApplicationRepositoryImpl`/`MissionRepositoryImpl`. Además, D11 (`full_name` → `nickname`) |
| **PO (Esteban)** | Confirmar D2, D3, D4 (límite de foto) y D8 (bitácora y palabras fuera de alcance; nivel y marco fijos) |

## 6. Riesgos

- **Gate de perfil y usuarios existentes:** todos los usuarios actuales quedarán "incompletos" y pasarán por HU-005. Es el comportamiento esperado, pero hay que avisarlo al equipo de QA.
- **Subida de fotos:** imágenes grandes de la cámara superan 2 MB. Se puede **comprimir en el cliente** (JPEG calidad 85, máximo 1080 px) antes de validar, para no rechazar fotos normales.
- **`AppNavigation.kt`** ya tiene `@Suppress("CyclomaticComplexMethod")`. Agregar 6 destinos más lo agrava. Se puede extraer un `ProfileNavigation` con funciones `@Composable` por destino.
- **Sin pruebas de UI Compose en unitarias:** los gates y validaciones se prueban en el ViewModel y el caso de uso. Se agregan casos manuales a la matriz de pruebas.
- **Sesión con `NetworkError`:** si no se puede consultar `profile_completed` sin red, se deja pasar a `MainMenu` con aviso y no se bloquea.

## 7. Siguiente paso

Confirmar las decisiones ❓ y luego formalizar como cambio SDD `perfil-configuracion` (proposal → specs → design → tasks), con el mismo formato de `publicacion-mision-campo/tasks.md`, y arrancar por **PR A (HU-010)**, que no depende de la BD.
