# Proposal: Perfiles, configuración y onboarding (HU-005..HU-011, HU-013)

## Intent

No existe dominio de perfil: `InitialProfile` y los destinos del menú `PROFILE`, `AVATAR` y `HISTORY` caen en placeholders, `profiles` solo tiene `GRANT SELECT` y no hay almacenamiento de fotos. Login y arranque en frío saltan el perfil inicial. Este change formaliza `plan.md` (D1..D10) para dar identidad pública al usuario, cambio de contraseña e historial.

## Scope

### In Scope
- HU-010: cambio de contraseña en Ajustes → Seguridad; valida la actual reautenticando (correo + actual), nueva distinta y confirmada; **mantiene la sesión**.
- Fundación: columnas `nickname`, `gender` (`femenino`, `masculino`, `prefiero_no_decir`), `description` (≤ 280), `avatar_id`, `photo_url`, `profile_completed` generada; `GRANT UPDATE` por columna; RPC `nickname_disponible`; bucket `avatars` (solo `image/jpeg`, 2 MB).
- HU-005 + gate de perfil completo en login, registro y arranque en frío; HU-013 onboarding de 4 páginas (Omitir/Siguiente/Empezar), reabrible desde Ajustes → Ayuda.
- HU-006 Mi perfil ("Nivel 1" fijo, sin marco), HU-008 edición, HU-009 foto (JPG/PNG de entrada, comprimida a JPEG ≤ 2 MB).
- Catálogo de avatares predefinidos dibujados con Canvas al estilo `GameAvatar`, guardado como `avatar_id`.
- HU-007 perfil público de solo lectura (solo columnas públicas) desde postulantes y publicador.
- HU-011 línea temporal (publicadas + postulaciones aceptadas); "Bitácora oficial" y "Palabras de participantes" en estado vacío.

### Out of Scope
- HU-012; gamificación (nivel real, marcos).
- D11 privacidad (`profiles` legible por `anon` con `full_name`): **pendiente de discusión con el equipo**; no se cambian los usos de `full_name` de compañeros.
- Bitácora y palabras reales, lista de participantes del detalle, persistencia local.

## Capabilities

### New Capabilities
- `cambio-contrasena-ajustes`: HU-010.
- `perfil-inicial`: configuración obligatoria y gate (HU-005).
- `onboarding`: guía inicial y reapertura (HU-013).
- `perfil-propio`: consulta, edición y foto (HU-006, HU-008, HU-009).
- `perfil-publico`: ficha de terceros sin datos privados (HU-007).
- `historial-misiones`: línea temporal (HU-011).

### Modified Capabilities
- None (no hay specs en `openspec/specs/`).

## Approach

Capas vigentes (`domain` → `data` → `ui`), repositorios que lanzan y casos de uso con `Result<T>`, ViewModel + `StateFlow`, Koin (`ProfileModule`). Dependencias nuevas: `storage-kt:2.6.0`, `coil-compose:2.7.0`. SQL idempotente en `supabase/changes/` con espejo en `schema.sql`. TDD estricto, KDoc y strings en español.

**Orden de PR:** A HU-010 → B fundación → C HU-005+HU-013 → D HU-006/008/009 → E HU-007 → F HU-011.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `domain/{model,repository,validation,usecase}` | New/Modified | Perfil, historial, validadores, `AuthRepository.currentUserEmail()` |
| `data/repository/` | New | `ProfileRepositoryImpl`, `MissionHistoryRepositoryImpl` |
| `ui/profile/**`, `ui/onboarding/`, `ui/theme/NaturePalette.kt` | New | Pantallas y componentes |
| `ui/settings/`, `ui/navigation/`, `SessionViewModel` | Modified | Seguridad, Ayuda, destinos, gate |
| `PostulantesScreen`, `MissionDetailScreen` | Modified | Abrir perfil público |
| `di/`, `libs.versions.toml` | Modified | Módulo y dependencias |
| `supabase/changes/`, `schema.sql` | New/Modified | Columnas, grants, RPC, bucket + rollback |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| SQL no aplicado en BD compartida | High | PR A sin BD; coordinar con el tech lead |
| "Secure password change" activo | Med | Verificar en dashboard |
| Usuarios existentes pasan por HU-005 | High | Esperado; avisar a QA |
| Sin red al consultar el gate | Med | Dejar pasar con aviso |
| Conflictos en archivos de compañeros | Med | Cambios mínimos, avisar |

## Rollback Plan

Revertir PRs en orden inverso (F→A). BD: scripts de rollback eliminan columnas, RPC, grants y bucket; columnas nullable, sin cambios destructivos.

## Dependencies

- Tech lead aplica SQL pendiente del Sprint 1 y luego el de PR B.

## Success Criteria

- [ ] Escenarios Gherkin de HU-005..HU-011 y HU-013 verificados en dispositivo.
- [ ] `ktlintCheck detekt testDebugUnitTest assembleDebug` en verde.
- [ ] El perfil público nunca selecciona columnas privadas (prueba del `select`).
