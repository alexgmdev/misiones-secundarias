# Tasks: Publicación de misión de campo y aviso de recompensas

Tamaños: **S** (<1 h), **M** (1-3 h), **L** (>3 h). Base de código: `app/src/main/java/com/groovycow/misionessecundarias/` (abreviado `…/`); tests en `app/src/test/java/com/groovycow/misionessecundarias/` (`T/`).

Reglas globales: TDD estricto (RED -> GREEN -> REFACTOR) con `./gradlew :app:testDebugUnitTest`; KDoc en español en toda API pública; ktlint y detekt en verde; verificar localmente con JDK 17 y Android SDK instalados (2026-11-05), además de CI al abrir PR.

Trazabilidad: **CBM** = `crear-borrador-mision`, **FHI** = `fecha-hora-inicio`, **PM** = `publicar-mision`, **AVR** = `aviso-recompensas`.

Decisiones aplicadas (2026-11-05): D1 `meeting_point TEXT NULL`; D2 fecha/hora obligatorias; D4 selector de modalidad mínimo en HU-016 y HU-015 descopeada; D13 coordenadas manuales compatibles con futura selección en mapa; 3 PRs; verificación local habilitada.

## PR 0: Fundación (rama `dev/juan-pablo` -> develop)

- [x] 0.1 (S) Decisiones de diseño tomadas y registradas en `design.md` (D1, D2, D4, PRs, entorno).
- [ ] 0.2 (M) **Spike**: insertar una misión de prueba vía `supabase-kt` y verificar (a) que RLS permite el `insert`, (b) cómo viaja `location GEOGRAPHY(POINT)` en PostgREST y si `Mission.location: String?` lo decodifica, (c) el formato de `date_time`. Documentar el resultado en `design.md`. [CBM Persistencia] Lectura/decodificación y formato completados; inserción/RLS pendiente: falta `meeting_point`, la política para leer borradores no está verificada en la BD compartida y no hay una sesión autenticada de prueba verificable.
- [x] 0.3 (S) `domain/model/MissionModality.kt`: enum `CAMPO`/`REMOTA` con valor de BD y KDoc.
- [x] 0.4 (S) `domain/model/CreateMissionDraft.kt` y `domain/model/MissionError.kt` con KDoc.
- [x] 0.5 (S) `domain/model/Mission.kt`: agregar `meetingPoint` y ajustar el mapeo de `location` según 0.2.
- [x] 0.6 (S) `domain/repository/MissionRepository.kt`: firmas `createMission` y `publishMission` (sin implementación todavía). [CBM Persistencia; PM Transición]
- [ ] 0.7 (S) SQL de D1: `supabase/changes/2026-11-05_missions_meeting_point.sql` (idempotente) + reflejarlo en `supabase/schema.sql`. Coordinar la aplicación con el Líder Técnico. [CBM Ubicación en Cali] Evidencia: la columna aún no existe en el proyecto compartido; no aplicar la migración sin coordinación.

## PR 1: `hu/HU-016` Crear borrador + fecha/hora (depende de PR 0)

- [x] 1.1 (M) RED `T/domain/validation/MissionDraftValidatorTest.kt`: título vacío, descripción vacía, requisitos vacíos, punto de encuentro vacío, ubicación fuera de Cali, cupos < 2, modalidad remota sin ubicación, fecha ausente/pasada/de hoy, hora ausente, `Clock.fixed`. GREEN `domain/validation/MissionDraftValidator.kt`. [CBM Campos obligatorios, Ubicación en Cali, Cupos; FHI Fecha/Hora obligatorias, Fecha no anterior a hoy]
- [x] 1.2 (S) RED test de combinación: `2026-11-15` + `10:00` -> `2026-11-15T10:00:00-05:00` (zona `America/Bogota`). GREEN helper de fecha/hora en `domain`. [FHI Combinación fecha + hora]
- [x] 1.3 (M) RED `T/domain/usecase/CreateMissionUseCaseTest.kt`: éxito, excepción del repositorio, `CancellationException` relanzada. GREEN `domain/usecase/CreateMissionUseCase.kt`. [CBM Persistencia, Errores]
- [x] 1.4 (M) `data/repository/MissionRepositoryImpl.createMission()`: `insert` con `status='Borrador'`, mapeo de `date_time`, `meeting_point` y ubicación; DTOs privados `@Serializable` en el mismo archivo (estilo del equipo). [CBM Persistencia como borrador, Categoría opcional]
- [x] 1.5 (L) RED `T/ui/missions/create/CreateMissionViewModelTest.kt`: errores por campo, no invoca el caso de uso con errores, `isLoading` evita doble envío, éxito navega, error de red conserva datos. GREEN `ui/missions/create/CreateMissionViewModel.kt` + `CreateMissionUiState`. [CBM Estado de carga, Errores, Campos obligatorios]
- [x] 1.6 (L) `ui/missions/create/CreateMissionScreen.kt` + `ui/missions/MissionMessages.kt`: selector de modalidad, campos del formulario, `DatePickerDialog` y selector de hora Material 3, validación visual; registrar en `di/RepositoryModule.kt`. [CBM Selección de modalidad, Campos obligatorios; FHI Selección de hora válida]
- [x] 1.7 (M) `ui/navigation/AppNavigation.kt` y `MainMenuScreen.kt`: `AppDestination.PublishMission` + `MainMenuDestination.PUBLISH_MISSION.toAppDestination()`. Verificar que el resto de destinos no cambie. [CBM Selección de modalidad]
- [x] 1.8 (S) Formato legible de `date_time` en español para el detalle de la misión y la lista, convertido a `America/Bogota`. [FHI Persistencia y visualización]
- [ ] 1.9 (S) Manual: recorrer el Gherkin de HU-016, HU-020 y HU-021 en dispositivo/emulador con el proyecto Supabase compartido. Pendiente: la columna `meeting_point` no existe aún en la BD compartida y no se ha coordinado la migración.
- [ ] 1.10 (S) Push a `hu/HU-016`, CI verde y PR a `develop` (commits separados: `HU-016`, `HU-020`, `HU-021`).

## PR 2: `hu/HU-017` Publicar misión (depende de PR 1)

- [x] 2.1 (M) RED `T/domain/usecase/PublishMissionUseCaseTest.kt`: borrador incompleto, punto de encuentro ausente, fecha vencida, éxito, error de red. GREEN `domain/usecase/PublishMissionUseCase.kt`. [PM Verificación de requisitos previos, Errores]
- [x] 2.2 (M) `MissionRepositoryImpl.publishMission()`: `update({ status = "Publicada" })` filtrando por `id` y publicador; devolver la misión actualizada e idempotencia. [PM Transición a publicada, Idempotencia]
- [x] 2.3 (M) UI: acción "Publicar Misión" en la confirmación del borrador, estado de carga, confirmación "Misión publicada correctamente" y bloqueo de doble envío. [PM Acción de publicar, Estado de carga, Transición]
- [ ] 2.4 (S) Verificar manualmente que la misión publicada aparece en "Buscar misiones" y que el borrador no. [PM Visibilidad pública] Pendiente de aplicar `meeting_point` y la política SELECT de borradores en la BD compartida.
- [ ] 2.5 (S) Push, CI verde y PR a `develop`.
- [ ] 2.6 (S) Coordinar con el Líder Técnico y, si se aprueba, aplicar `supabase/changes/2026-11-05_missions_publish_draft_policy.sql`; la política permite SELECT únicamente al publicador del borrador. No aplicarla unilateralmente. [PM Transición; CBM Persistencia]

## PR 3: `hu/HU-126` Aviso de recompensas (independiente)

- [x] 3.1 (S) `ui/missions/MissionDetailScreen.kt`: tarjeta informativa en la sección de recompensas con el texto exacto de la spec. [AVR Aviso fijo y visible, Texto exacto]
- [x] 3.2 (S) Test de UI que verifique la presencia del texto exacto y la ausencia de acciones transaccionales. [AVR Sin acciones, Alcance del aviso]
- [ ] 3.3 (S) Verificar accesibilidad con TalkBack (texto estático completo). [AVR Accesibilidad] Compose expone el texto completo como estático (test instrumentado); falta la comprobación manual con TalkBack.
- [ ] 3.4 (S) Push, CI verde y PR a `develop`.

## Cierre del sprint

- [ ] 4.1 Mover en Jira a "En curso" cada historia al iniciar y comentar el PR correspondiente.
- [ ] 4.2 Proponer al equipo sacar **SCRUM-38 (HU-015)** del sprint por estar marcada Post-MVP (decisión D4). No modificar Jira sin confirmación.
- [x] 4.3 Verificar los 5 pasos (`ktlintCheck`, `detekt`, `testDebugUnitTest`, `jacocoTestReport`, `assembleDebug`) localmente; todos pasan con el entorno instalado.
- [ ] 4.4 Cerrar HU-016, HU-017, HU-020, HU-021 y HU-126 al mergear a `develop`.
