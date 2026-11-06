# Proposal: Modalidad, publicación de misión y aviso de recompensas (HU-015, HU-016, HU-017, HU-020, HU-021, HU-126)

## Intent

El flujo de publicación ya existe en el menú (`MainMenuDestination.PUBLISH_MISSION`) pero hoy cae en `FeaturePlaceholderScreen`. La base de datos ya soporta `modality`, `date_time`, `location` y los estados `Borrador`/`Publicada`; el esquema declara políticas RLS para que el publicador inserte y actualice sus misiones, pero el insert autenticado aún debe verificarse. `MissionRepository` **solo leía** (`getPublishedMissions`, `getMissionById`).

Este change habilita la selección de modalidad de HU-015, el flujo de borrador/fecha/hora/publicación de misiones y el aviso de recompensas no monetarias.

## Scope

### In Scope
- HU-015: selector previo de modalidad (campo/remota), campos físicos condicionales y posibilidad de volver a cambiar la modalidad antes de guardar.
- Formulario de borrador de misión de campo: título, descripción, requisitos, categoría, punto de encuentro, coordenadas iniciales latitud/longitud, fecha, hora y cupos. Una selección futura en mapa poblará el mismo par de coordenadas.
- Validaciones de dominio: campos obligatorios, ubicación en Cali, fecha no pasada, cupos válidos.
- `CreateMissionUseCase` + `MissionRepository.createMission()` contra Supabase con `status = 'Borrador'`.
- `DatePicker` y `TimePicker` (Material 3) y combinación de fecha + hora en `date_time`.
- `PublishMissionUseCase` + `MissionRepository.publishMission()` con transición `Borrador -> Publicada`.
- Navegación: `AppDestination.PublishMission` cableado desde el menú principal.
- Tarjeta informativa de ausencia de pasarela de pago en el detalle de misión.
- Tests unitarios de validadores, casos de uso y ViewModel (TDD estricto).

### Out of Scope
- Flujos remotos adicionales no descritos por HU-015 (por ejemplo, capacidades nuevas de colaboración digital o mapa remoto).
- Foro de misión, mapa interactivo, geocodificación/geolocalización automática, subida de imágenes.
- Recompensas reales, pasarela de pago o monetización (HU-126 solo informa).
- Notificaciones, recordatorios y edición posterior de una misión publicada.
- Room/cache offline.

## Capabilities

### New Capabilities
- `crear-borrador-mision`: alta de un borrador de misión de campo con validaciones y persistencia en Supabase (HU-016).
- Selección de modalidad previa al formulario y campos condicionales (HU-015; integrada con `crear-borrador-mision`).
- `fecha-hora-inicio`: selección y validación de fecha y hora de inicio, combinadas en un único `date_time` (HU-020, HU-021).
- `publicar-mision`: transición de borrador a publicada con verificación de requisitos previos (HU-017).
- `aviso-recompensas`: aviso fijo y visible de que la plataforma no procesa pagos (HU-126).

### Modified Capabilities
- None

## Approach

Se respeta la arquitectura por capas vigente y el patrón del equipo:

- `domain/model` + `domain/validation`: `CreateMissionDraft` y validadores puros (con `java.time.Clock` inyectado para tests deterministas, igual que `BirthDateValidator`).
- `domain/repository`: se extiende `MissionRepository` con `createMission` y `publishMission`. El repositorio **lanza**; el caso de uso envuelve en `Result<T>`.
- `data/repository`: `MissionRepositoryImpl` usa `postgrest-kt` (`insert`, `update`) y mapea fecha/hora y ubicación.
- `ui/missions/create`: `CreateMissionScreen` + `CreateMissionViewModel` con `StateFlow<CreateMissionUiState>`.
- `ui/navigation`: nuevo destino `PublishMission`; `MainMenuDestination.PUBLISH_MISSION` deja de caer en `Placeholder`.
- `di/RepositoryModule`: registro de casos de uso y ViewModel.
- Textos de UI en español con acentos, centralizados en un archivo de mensajes de la feature (patrón `ui/auth/AuthMessages.kt`).

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `domain/model/Mission.kt`, `CreateMissionDraft.kt` | Modify/New | Punto de encuentro y coordenadas de ubicación |
| `domain/validation/MissionDraftValidator.kt` | New | Obligatorios, Cali, fecha, cupos |
| `domain/repository/MissionRepository.kt` | Modify | `createMission`, `publishMission` |
| `domain/usecase/CreateMissionUseCase.kt`, `PublishMissionUseCase.kt` | New | `Result<T>` |
| `data/repository/MissionRepositoryImpl.kt` | Modify | `insert`/`update` en Supabase |
| `ui/missions/create/**` | New | Pantalla + ViewModel |
| `ui/missions/MissionDetailScreen.kt` | Modify | Aviso de recompensas (HU-126) |
| `ui/navigation/AppNavigation.kt`, `MainMenuScreen.kt` | Modify | Destino `PublishMission` |
| `di/RepositoryModule.kt` | Modify | Casos de uso + ViewModel |
| `app/src/test/**` | New | Validadores, casos de uso, ViewModel |
| `supabase/schema.sql` + `supabase/changes/2026-11-05_missions_meeting_point.sql`, `2026-11-05_missions_publish_draft_policy.sql` | Modify/New | Columna `meeting_point` (D1) y lectura RLS limitada a borradores propios (D14) |

## Slicing (PRs a `develop`)

1. **`hu/HU-016`** — fundación, selector HU-015, modelo, validador, `createMission`, pantalla de borrador, navegación y **fecha/hora** (commits separados que referencian HU-016, HU-020 y HU-021). Mergea primero.
2. **`hu/HU-015`** — tests instrumentados y actualización de alcance/Jira; se apila sobre HU-016 porque el selector ya está implementado en ese flujo.
3. **`hu/HU-017`** — `publishMission`, acción "Publicar Misión" y validación previa.
4. **`hu/HU-126`** — tarjeta de aviso en el detalle (independiente, se puede mergear en cualquier momento).

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| `location GEOGRAPHY(POINT)` no mapea como `String` en supabase-kt | Low en lectura; pendiente en escritura | Lectura por PostgREST y decodificación `Mission.location: String?` verificadas; spike de `insert`/EWKT y RLS pendiente |
| `date_time NOT NULL` impide guardar borradores sin fecha | High | Exigir fecha y hora en el borrador (decisión D2) o migrar a nullable |
| Sin dirección textual para "punto de encuentro" | Resuelto | D1: se agrega `meeting_point TEXT NULL` (cambio aditivo, coordinado) |
| Sprint cierra el sáb 7-nov 22:00 (COL) | High | Priorizar HU-016 + fecha/hora; HU-126 al final |
| La columna `meeting_point` falta en la BD compartida | High | Migración idempotente preparada; coordinar su aplicación con el Líder Técnico antes de probar escrituras |
| RLS no permite leer los borradores propios | High | Política adicional limitada a `auth.uid() = publisher_id AND status = 'Borrador'`; coordinar y aplicar antes de la validación integrada |

## Rollback Plan

- App: revertir los PRs en `develop` en orden inverso (HU-126, HU-017, fecha/hora, HU-016).
- BD: si se aplicaron cambios, revertir coordinadamente: eliminar `meeting_point` solo tras comprobar que no hay consumidores y quitar la política `Publishers can view own drafts`. Los datos existentes no se tocan.
- No hay cambios destructivos sobre tablas ni políticas RLS.

## Dependencies

- Proyecto Supabase compartido (`epmjmintacuzjyfmfduu`) accesible con la `anon key`.
- Coordinación con el Líder Técnico para cualquier cambio de esquema.
- HU-016 reutiliza el selector y el comportamiento de campos condicionales de HU-015.

## Success Criteria

- [ ] Todos los escenarios Gherkin de HU-015, HU-016, HU-017, HU-020, HU-021 y HU-126 se verifican en dispositivo.
- [ ] `./gradlew ktlintCheck detekt testDebugUnitTest assembleDebug` en verde (local o CI).
- [ ] Un borrador se guarda con `status = 'Borrador'` y no aparece en la búsqueda pública.
- [ ] Publicar cambia el estado a `Publicada` y la misión aparece en la lista.
- [ ] El aviso de recompensas se muestra con el texto exacto exigido por HU-126.
