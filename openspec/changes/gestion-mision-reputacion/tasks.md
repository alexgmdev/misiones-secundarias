# Tasks: Gestión de misión, privacidad y reputación

Reglas globales: TDD estricto, KDoc en español, `ktlintCheck`, `detekt`, `testDebugUnitTest` y `assembleDebug` en verde.

## HU-024: Recompensa informativa

- [x] (S) `Mission.reward` y `CreateMissionDraft.reward` con KDoc.
- [x] (M) `CreateMissionViewModel`: campo `reward`, envío al caso de uso y aviso de recompensas.
- [x] (S) `CreateMissionScreen`: campo de texto y nota de ausencia de pasarela de pago.
- [x] (S) `MissionRepositoryImpl`: persistir `reward` y exponerlo en la vista pública.
- [x] (S) `MissionDetailScreen`: sección "Recompensa informativa".
- [ ] (S) Aplicar `supabase/changes/2026-11-21_missions_reward.sql` con el Líder Técnico.

## HU-023: Previsualización

- [x] (M) `CreateMissionViewModel.onPreview` valida y expone `previewDraft` sin persistir.
- [x] (M) `MissionPreviewScreen` con tarjeta, detalle y aviso de recompensas.
- [x] (S) Acciones "Volver a editar" y "Guardar borrador" desde la previsualización.
- [x] (S) Tests de previsualización válida e inválida.

## HU-032: Editar misión publicada

- [x] (M) `MissionDetailsUpdate`, `UpdateMissionDetailsUseCase` y regla de participantes aceptados.
- [x] (M) `MissionRepository.updateMissionDetails` filtrando por publicador y estado `Publicada`.
- [x] (M) `EditMissionViewModel` + `EditMissionScreen`.
- [x] (S) Acción "Editar misión" en el detalle para el publicador.
- [x] (S) Navegación `EditMission` y refresco del detalle con la misión actualizada.

## HU-122: Políticas de privacidad

- [x] (S) `PrivacyPolicyScreen` con secciones de datos, ubicación, visibilidad y control.
- [x] (S) Entrada "Políticas de Privacidad" en Ajustes y destino de navegación.
- [x] (S) Test del contenido informativo.

## HU-124: Calificar publicador

- [x] (M) Tabla `publisher_ratings`, RLS y permisos en `supabase/changes/2026-11-21_publisher_ratings.sql`.
- [x] (M) `PublisherRating`, `RatingRepository`, `RatePublisherUseCase` y `GetPublisherReputationUseCase`.
- [x] (M) `RatePublisherViewModel` y diálogo de estrellas en "Mis postulaciones".
- [x] (S) Reputación promedio visible en el detalle de la misión.
- [ ] (S) Aplicar la migración de calificaciones con el Líder Técnico.

## Cierre

- [ ] Ejecutar `ktlintCheck`, `detekt`, `testDebugUnitTest` y `assembleDebug`.
- [ ] Push a `dev/juan-pablo`, CI verde y PR a `develop`.
- [ ] Mover SCRUM-45, SCRUM-46, SCRUM-56, SCRUM-144 y SCRUM-146 en Jira.
