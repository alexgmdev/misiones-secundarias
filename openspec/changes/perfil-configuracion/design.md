# Design: Perfiles, configuración y onboarding (HU-005..HU-011, HU-013)

> Supera las 800 palabras: 8 HU, esquema SQL, contratos y diagramas de secuencia (regla `design` de `openspec/config.yaml`). Base: `develop` @ `779946b`.

## Technical Approach

Mismo patrón que `auth-registro-login`: capas `domain/{model,repository,validation,usecase}` → `data/repository` → `ui/<feature>`; el repositorio **lanza** `ProfileException(ProfileError)`, el caso de uso devuelve `Result<T>` vía `authCatching {}`, el ViewModel expone `StateFlow<XUiState>` con errores como enums y los textos viven en `ui/profile/ProfileMessages.kt`. Navegación manual: los destinos nuevos se agrupan en un sub-tipo sellado que se enruta desde **una sola rama** de `AppNavigation`. Dependencias nuevas: `storage-kt:2.6.0` y `coil-compose:2.7.0` (AGP 8.5.0 intacto). Specs: `specs/{cambio-contrasena-ajustes,perfil-inicial,onboarding,perfil-propio,perfil-publico,historial-misiones}/spec.md`.

## Architecture Decisions

| # | Decisión | Rechazado | Rationale |
|---|----------|-----------|-----------|
| D1 | `ChangeCurrentPasswordUseCase` nuevo: valida localmente (vacíos, `PasswordValidator`, coincidencia, nueva ≠ actual), reautentica con `signIn(currentUserEmail(), actual)` y luego `updatePassword`; **no** cierra sesión | Reusar `ChangePasswordUseCase` (cierra sesión, flujo de recuperación); nonce `reauthenticate()` (requiere correo con OTP) | La reautenticación crea una sesión fresca, por lo que funciona incluso con "Secure password change" activo (exige login reciente); igualmente se recomienda dejarlo OFF |
| D2 | `AuthError.IncorrectCurrentPassword` nuevo; el `InvalidCredentials` del `signIn` de reautenticación se traduce a él en el caso de uso | Reusar `InvalidCredentials` ("Credenciales incorrectas") | La spec exige "La contraseña actual es incorrecta" |
| D3 | `profile_completed` **columna generada** `STORED` (nickname, gender, avatar_id y photo_url no nulos) | Flag escrito por la app; vista | Imposible desincronizar; no requiere GRANT de escritura |
| D4 | `GRANT UPDATE (nickname, gender, description, avatar_id, photo_url)` a `authenticated`; RLS "update own" existente | `GRANT UPDATE` de tabla; RPC `update_profile` | Mínimo privilegio; `full_name`/`role` siguen intocables |
| D5 | Unicidad: índice único `lower(nickname)` + RPC `nickname_disponible(p_nickname)` que excluye `auth.uid()`; la violación `23505` se traduce a `NicknameTaken` | Solo índice; solo RPC | RPC da el error amable antes de subir la foto; el índice cierra la carrera. Excluir al propio usuario cubre "mi nickname no cuenta como en uso" (HU-008) |
| D6 | Foto: `AndroidPhotoCompressor` (data) lee el `Uri`, acepta solo `image/jpeg`/`image/png` (`ContentResolver.getType`), decodifica con `inSampleSize`, escala ≤ 1080 px y prueba calidad 85→45 (pasos de 10) hasta ≤ 2 MB; si no baja → `PhotoTooLarge` | Subir original; validar solo tamaño | Fotos de cámara superan 2 MB; el bucket solo acepta JPEG |
| D7 | Bucket `avatars` público, `file_size_limit = 2097152`, `allowed_mime_types = {image/jpeg}`, ruta fija `{uid}/foto.jpg` con `upsert = true`; 3 políticas (select público, insert/update en carpeta propia, sin delete: la foto se sobrescribe); `photo_url = publicUrl + "?v=<epochMillis>"` | Nombre de archivo único por subida | Ruta fija = sin huérfanos; el query param invalida la caché de Coil |
| D8 | CHECK `photo_url` debe contener `/storage/v1/object/public/avatars/<id>/` | Sin restricción | Con GRANT UPDATE cualquiera podría poner una URL arbitraria vía API |
| D9 | Avatares: `enum class AvatarId(val key: String)` (8 entradas, colores de `NaturePalette` + ícono Material) dibujados por `ProfileAvatar` con `Canvas` al estilo `GameAvatar`; BD solo valida formato `^[a-z_]{1,30}$` | Imágenes en assets; CHECK con lista cerrada | Sin assets; agregar avatares no requiere migración |
| D10 | Gate: `SessionViewModel` recibe `IsProfileCompleteUseCase`. Arranque en frío: con sesión resuelve `MAIN_MENU` o `PROFILE_SETUP`. Login: destino transitorio `AppDestination.CheckingProfile` (spinner) + `sessionViewModel.checkProfileGate(onResult)`. Registro: directo a `InitialProfile`. **Cualquier** fallo de la consulta deja pasar al menú | Gate en cada pantalla; bloquear sin red | Spec "Gate sin conexión"; si el SQL aún no está aplicado (columna inexistente) la app sigue usable |
| D11 | Se elimina el `BackHandler` de `InitialProfile` → Atrás finaliza la Activity; al volver, el gate de arranque en frío la reabre | Consumir Atrás sin acción | Spec "Sin salida del gate" sin dejar al usuario atrapado |
| D12 | `sealed interface ProfileFlowDestination : AppDestination` con `MyProfile`, `EditProfile`, `MissionHistory`, `ChangePassword`, `Onboarding(fromSettings)`; se enrutan en `ui/navigation/ProfileNavigation.kt` | 5 ramas más en `AppNavigation` | `AppNavigation` ya suprime `CyclomaticComplexMethod`; solo suma una rama. `shouldRedirectToWelcome` los protege sin cambios (`else -> true`) |
| D13 | HU-007 como `ModalBottomSheet` (`PublicProfileSheet`) superpuesto desde `AppNavigation` con `var publicProfileUserId`; `PostulantesScreen` y `MissionDetailRoute` reciben callbacks opcionales `onApplicantClick(solverId)` / `onPublisherClick()` (default `{}`) | Pantalla dedicada; ampliar el `select` de postulantes | La spec dice "despliega una tarjeta"; cambios mínimos en archivos de Alex; no se tocan DTOs con `full_name` (D11 del plan fuera de alcance) |
| D14 | Columnas públicas en constante `ProfileRepositoryImpl.PUBLIC_COLUMNS = listOf("id","nickname","gender","description","avatar_id","photo_url")` usada con `Columns.list(...)` | `select()` sin columnas | Spec "Columnas consultadas" verificable en prueba unitaria sin red |
| D15 | Saludo del menú: `ProfileSummaryViewModel` (nickname, avatar, foto) con `refresh()` en `LaunchedEffect` al entrar a `MainMenu`; `MainMenuScreen` ya acepta `username` | Pasar el perfil por la navegación | Spec "Reflejo inmediato" (el VM vive en la Activity, se refresca en cada entrada) |
| D16 (revisada) | HU-011: sin repositorio nuevo; `GetMissionHistoryUseCase` reutiliza `MissionRepository.getMyMissions()` (filtra `Borrador`) y `ApplicationRepository.getMyApplications()` (`Aceptado`, detalle con `getMissionById`), deduplica por `missionId` (gana publicador) y ordena por `dateTime` desc. `GetMissionLogbookSectionsUseCase` reutiliza `LogbookRepository.getLogbooksForMission` (rol `publicador` = bitácora oficial; `participante` = palabras) al expandir, con caché por misión | Vista SQL; RPC | Sin cambios de BD; RLS actual ya lo permite |
| D17 | Onboarding sin persistencia: se muestra tras guardar `InitialProfile` y desde Ajustes → Ayuda; `HorizontalPager` de 4 páginas | DataStore "ya visto" | El perfil inicial ocurre una vez (plan D6) |
| D18 | `ui/theme/NaturePalette.kt` (`object NaturePalette`) en las pantallas nuevas de familia B (Mi perfil, Editar, Historial, Ficha pública, Cambiar contraseña); `InitialProfileScreen` y `OnboardingScreen` usan familia A (`AuthVisualStyle`) | Refactorizar `private val` de compañeros | Evita conflictos |

## Data Flow

Gate (login / arranque en frío):

    LoginScreen ─onSignedIn→ AppNavigation: destination = CheckingProfile
        └→ SessionViewModel.checkProfileGate ─→ IsProfileCompleteUseCase ─→ ProfileRepository.isProfileComplete()
              select profile_completed where id = uid
        ←─ Result.success(true)  → MainMenu
        ←─ Result.success(false) → InitialProfile
        ←─ Result.failure(*)     → MainMenu (no bloquea)

Guardar perfil inicial / foto:

    picker(Uri) → InitialProfileVM.onPhotoPicked → PhotoCompressor.compress(uri) → ByteArray ≤ 2 MB (o error)
    "Guardar Perfil" → CompleteInitialProfileUseCase
        validar campos ─✗→ errores por campo (sin backend)
        rpc nickname_disponible ─false→ NicknameTaken
        storage avatars.upload("{uid}/foto.jpg", bytes, upsert) → publicUrl + ?v=ts
        profiles.update(nickname, gender, description, avatar_id, photo_url)
    → Onboarding(fromSettings = false) → MainMenu

Cambio de contraseña (HU-010):

    ChangePasswordVM → ChangeCurrentPasswordUseCase
        local: requeridos / reglas / coincidencia / nueva ≠ actual ─✗→ error (sin backend)
        AuthRepository.currentUserEmail() ─null→ ServerError
        signIn(email, actual) ─InvalidCredentials→ IncorrectCurrentPassword
        updatePassword(nueva) → Result.success (sesión intacta) → diálogo "Contraseña actualizada"

## File Changes

Base `app/src/main/java/com/groovycow/misionessecundarias/`. PR entre paréntesis.

| Archivo | Acción | Descripción |
|---------|--------|-------------|
| `domain/model/AuthError.kt` | Modify (A) | `IncorrectCurrentPassword` |
| `domain/repository/AuthRepository.kt`, `data/repository/AuthRepositoryImpl.kt` | Modify (A) | `currentUserEmail(): String?` |
| `domain/usecase/ChangeCurrentPasswordUseCase.kt` | Create (A) | D1/D2 |
| `domain/validation/FieldError.kt` | Modify (A,B) | `NicknameLength`, `NicknameInvalidChars`, `DescriptionTooLong`, `SelectionRequired` |
| `ui/auth/AuthMessages.kt` | Modify (A) | Texto de `IncorrectCurrentPassword` |
| `ui/settings/ChangePasswordScreen.kt`, `ChangePasswordViewModel.kt` | Create (A) | 3 campos con Mostrar/Ocultar, diálogo de éxito |
| `ui/settings/SettingsScreen.kt` | Modify (A,C) | Scroll, tarjetas "Seguridad" y "Ayuda"; params `onChangePassword`, `onOpenGuide` |
| `di/AuthModule.kt` | Modify (A) | Caso de uso y VM de HU-010 |
| `gradle/libs.versions.toml`, `app/build.gradle.kts` | Modify (B) | `supabase-storage`, `coil-compose` |
| `di/SupabaseModule.kt` | Modify (B) | `install(Storage)` |
| `domain/model/{Profile,PublicProfile,Gender,AvatarId,ProfileError,MissionHistoryEntry}.kt` | Create (B,F) | Ver contratos |
| `domain/repository/{ProfileRepository,MissionHistoryRepository,PhotoCompressor}.kt` | Create (B,F) | Interfaces |
| `domain/validation/{NicknameValidator,DescriptionValidator}.kt` | Create (B) | Puros |
| `data/repository/{ProfileRepositoryImpl,ProfileErrorMapper,MissionHistoryRepositoryImpl}.kt` | Create (B,F) | DTOs `@Serializable` privados, `withContext(Dispatchers.IO)` |
| `data/image/AndroidPhotoCompressor.kt` | Create (B) | D6 |
| `domain/usecase/{IsProfileComplete,CompleteInitialProfile,GetMyProfile,UpdateProfile,UploadProfilePhoto,GetPublicProfile,GetMissionHistory}UseCase.kt` | Create (B–F) | `Result<T>` |
| `ui/theme/NaturePalette.kt` | Create (B) | D18 |
| `ui/profile/components/{ProfileAvatar,AvatarPicker,ProfilePhoto,GenderSelector}.kt`, `ui/profile/ProfileMessages.kt` | Create (B) | Reutilizables; `ProfilePhoto` usa Coil `AsyncImage` con placeholder `Icons.Default.Person` |
| `di/ProfileModule.kt`, `MisionesApp.kt` | Create/Modify (B) | Registrar `profileModule` |
| `ui/profile/setup/InitialProfile{Screen,ViewModel}.kt` | Create (C) | "Crea tu perfil" |
| `ui/onboarding/OnboardingScreen.kt` | Create (C) | 4 páginas, Omitir/Siguiente/Empezar |
| `ui/auth/SessionViewModel.kt` | Modify (C) | `PROFILE_SETUP`, `checkProfileGate` (D10) |
| `ui/navigation/AppNavigation.kt` | Modify (C–E) | `InitialProfile` real, `CheckingProfile`, rama `is ProfileFlowDestination`, mapeo `PROFILE/AVATAR/HISTORY`, `PublicProfileSheet` superpuesto |
| `ui/navigation/ProfileNavigation.kt` | Create (C) | D12, D15 |
| `ui/profile/me/MyProfile{Screen,ViewModel}.kt`, `ui/profile/edit/EditProfile{Screen,ViewModel}.kt`, `ui/profile/ProfileSummaryViewModel.kt` | Create (D) | HU-006/008/009 |
| `ui/navigation/MainMenuScreen.kt` | Modify (D) | Param opcional `avatar: (@Composable () -> Unit)?` en lugar del ícono genérico (avisar a Jojhan) |
| `ui/profile/public/PublicProfile{Sheet,ViewModel}.kt` | Create (E) | HU-007 |
| `ui/missions/PostulantesScreen.kt`, `ui/missions/MissionDetailScreen.kt` | Modify (E) | Callbacks opcionales (D13); avisar a Alex |
| `ui/profile/history/MissionHistory{Screen,ViewModel}.kt` | Create (F) | Timeline, tarjetas expandibles, secciones vacías |
| `supabase/changes/2026-11-18_profiles_public_fields.sql` | Create (B) | Columnas, CHECKs, índice, GRANT, RPC |
| `supabase/changes/2026-11-18_storage_avatars.sql` | Create (B) | Bucket + políticas |
| `supabase/changes/2026-11-18_perfil_rollback.sql` | Create (B) | Reversa de ambos |
| `supabase/schema.sql` | Modify (B) | Espejo del estado final |

## Interfaces / Contracts

```kotlin
enum class Gender(val dbValue: String) { FEMENINO("femenino"), MASCULINO("masculino"), PREFIERO_NO_DECIR("prefiero_no_decir") }
data class Profile(val id: String, val nickname: String?, val gender: Gender?, val description: String?,
    val avatarId: AvatarId?, val photoUrl: String?, val isComplete: Boolean)
data class PublicProfile(val id: String, val nickname: String?, val gender: Gender?, val description: String?,
    val avatarId: AvatarId?, val photoUrl: String?)
data class ProfileUpdate(val nickname: String, val gender: Gender, val description: String?, val avatarId: AvatarId, val photoUrl: String?)
sealed interface ProfileError { NicknameTaken; UnsupportedPhotoFormat; PhotoTooLarge; SessionRequired; NoConnection; ServerError } // data objects
class ProfileException(val error: ProfileError) : Exception()

interface ProfileRepository {
    suspend fun isProfileComplete(): Boolean
    suspend fun getMyProfile(): Profile
    suspend fun isNicknameAvailable(nickname: String): Boolean      // rpc nickname_disponible
    suspend fun uploadPhoto(jpeg: ByteArray): String                // devuelve photo_url con ?v=
    suspend fun updateProfile(update: ProfileUpdate)                // photoUrl null = no tocar
    suspend fun getPublicProfile(userId: String): PublicProfile     // solo PUBLIC_COLUMNS
}
interface PhotoCompressor { suspend fun compressToJpeg(uri: String): ByteArray } // lanza ProfileException
enum class HistoryRole { PUBLISHER, SOLVER }
data class MissionHistoryEntry(val missionId: String, val title: String, val description: String,
    val requirements: String?, val dateTime: String, val status: String, val role: HistoryRole)
```

SQL (idempotente, encabezado Cambio/Fecha/Autor como `2026-11-03_*`):

```sql
alter table public.profiles add column if not exists nickname text
  constraint profiles_nickname_format check (nickname ~ '^[A-Za-z0-9_]{3,20}$');
-- gender check in (...), description char_length <= 280, avatar_id ~ '^[a-z_]{1,30}$', photo_url (D8)
alter table public.profiles add column if not exists profile_completed boolean
  generated always as (nickname is not null and gender is not null and avatar_id is not null and photo_url is not null) stored;
create unique index if not exists profiles_nickname_lower_key on public.profiles (lower(nickname));
grant update (nickname, gender, description, avatar_id, photo_url) on public.profiles to authenticated;
-- nickname_disponible: security definer, search_path '', excluye auth.uid(); execute solo a authenticated
-- storage: insert into storage.buckets ... on conflict (id) do update; 3 políticas (select público, insert/update en carpeta propia; sin delete)
--   en storage.objects con bucket_id = 'avatars' and (storage.foldername(name))[1] = auth.uid()::text
notify pgrst, 'reload schema';
```

## Testing Strategy

| Layer | Qué | Cómo |
|-------|-----|------|
| Unit | `NicknameValidator`, `DescriptionValidator` | Tablas de casos (límites 2/3/20/21, caracteres, 280/281) |
| Unit | Casos de uso (`ChangeCurrentPassword`, `CompleteInitialProfile`, `UpdateProfile`, `IsProfileComplete`, `GetPublicProfile`, `GetMissionHistory`) | MockK; orden de llamadas (`coVerify(exactly = 0)` cuando falla validación), dedupe/orden del historial |
| Unit | `ProfileErrorMapper`, `PUBLIC_COLUMNS` | Puros; `23505` → `NicknameTaken`; columnas exactas |
| Unit | ViewModels (`ChangePassword`, `InitialProfile`, `EditProfile`, `MyProfile`, `PublicProfile`, `MissionHistory`) y `SessionViewModel` (gate: completo, incompleto, fallo) | `MainDispatcherRule` + Turbine-less `StateFlow.value` (patrón actual) |
| Manual | Compresión de fotos, Coil, bucket, pager, BackHandler | Matriz en dispositivo (sin pruebas Compose en JVM) |

## Migration / Rollout

1. PR A no depende de BD. 2. Juan aplica el SQL pendiente de Sprint 1, luego `2026-11-18_profiles_public_fields.sql` y `2026-11-18_storage_avatars.sql`. 3. Usuarios existentes quedan incompletos y pasan por HU-005 (avisar a QA). Rollback: `2026-11-18_perfil_rollback.sql` (quita RPC, grants, índice, columnas, políticas y bucket; borrar antes los objetos del bucket) y revertir PRs F→A.

## Open Questions

- [ ] Confirmar en el dashboard el estado de "Secure password change" (D1 funciona en ambos casos; se recomienda OFF).
- [ ] Confirmar con supabase-kt 2.6.0 que `upload(path, bytes, upsert = true)` infiere `image/jpeg` por extensión; si no, pasar `contentType` explícito.
- [ ] D11 del plan (exposición de `full_name` a `anon`) queda pendiente con el equipo.
