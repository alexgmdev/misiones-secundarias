# Proposal: Gestión de misión, privacidad y reputación (HU-023, HU-024, HU-032, HU-122, HU-124)

## Intent

Completar el flujo de publicación con una recompensa informativa, una previsualización previa al envío y la edición de los datos descriptivos de una misión publicada sin participantes. Además, ofrecer las políticas de privacidad desde Ajustes y permitir que un participante aceptado califique al publicador tras completar la misión.

## Scope

### In Scope
- HU-024: campo de recompensa informativa en el borrador, con aviso de ausencia de pasarela de pago, visible en tarjeta y detalle.
- HU-023: previsualización de la misión con los datos ingresados y regreso al formulario antes de guardar.
- HU-032: edición de título, descripción, requisitos, categoría y recompensa de una misión publicada sin participantes aceptados.
- HU-122: pantalla estática de políticas de privacidad y uso de datos, accesible desde Ajustes.
- HU-124: calificación de 1 a 5 estrellas al publicador por un participante aceptado de una misión completada, con promedio público de reputación.

### Out of Scope
- Pasarela de pago o recompensas transaccionales.
- Cambio de modalidad o de fecha/hora durante la edición (HU-032 se limita a datos descriptivos).
- Flujo completo de código de verificación de cierre (HU-067); la calificación se habilita cuando la misión está `Completada`.

## Approach

Se respeta la arquitectura por capas existente: modelos y casos de uso con `Result<T>`, repositorios Supabase que lanzan, ViewModels con `StateFlow` y navegación manual en `AppNavigation`. La recompensa y las calificaciones se agregan al esquema compartido mediante migraciones idempotentes coordinadas con el Líder Técnico.

## Affected Areas

| Area | Impact |
|------|--------|
| `domain/model`, `domain/repository`, `domain/usecase` | Nuevos modelos de edición/calificación y casos de uso |
| `data/repository` | `reward` en misiones y `RatingRepositoryImpl` |
| `ui/missions/create`, `preview`, `edit`, `rating` | Recompensa, previsualización, edición y calificación |
| `ui/settings` | Políticas de privacidad |
| `ui/navigation` | Destinos `PrivacyPolicy` y `EditMission` |
| `supabase/schema.sql` + `supabase/changes` | Columna `reward`, tabla `publisher_ratings` y vista pública |
