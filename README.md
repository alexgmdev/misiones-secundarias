# Misiones Secundarias — MVP Android

Aplicación móvil Android para facilitar la colaboración comunitaria en Cali. Conecta a **Publicadores** (personas que necesitan apoyo) con **Solucionadores** (personas dispuestas a colaborar).

**Proyecto académico** — Universidad del Valle, 2026.
**Organización ejecutora:** Groovy Cow.

## Stack Tecnológico
*   **Lenguaje:** Kotlin
*   **UI:** Jetpack Compose + Material 3
*   **Backend/BaaS:** Supabase (Auth + PostgreSQL + PostGIS)
*   **Persistencia Local:** Room
*   **Mapas:** MapLibre
*   **Inyección de Dependencias:** Koin

## Prerrequisitos
*   Android Studio (Ladybug o superior)
*   JDK 17

## Configuración Inicial
1. Clonar el repositorio.
2. Crear (o editar) `local.properties` en la raíz del proyecto (está ignorado por git) con la ruta del SDK y las credenciales de Supabase, que se solicitan al Líder Técnico:
   ```properties
   sdk.dir=/ruta/a/Android/Sdk
   SUPABASE_URL=https://tu-proyecto.supabase.co
   SUPABASE_ANON_KEY=tu_anon_key_aqui
   ```
3. Abrir el proyecto en Android Studio y sincronizar Gradle.

### Checklist del dashboard de Supabase (autenticación)
Ajustes que no se versionan y deben revisarse a mano en el proyecto:
*   **Confirm email: OFF**.
*   Política de clave: mínimo 8 caracteres con minúsculas, mayúsculas y dígitos.
*   Plantilla *Reset Password* con el código `{{ .Token }}`; OTP de 6 dígitos y expiración de 900 s.
*   SMTP propio configurado.
*   Ejecutar en el SQL editor `supabase/changes/2026-11-03_auth_profiles_privacy.sql` y `supabase/changes/2026-11-03_login_attempts.sql` (reversa: `2026-11-03_auth_rollback.sql`). Coordinar antes con el equipo: afectan la BD compartida.
*   Desplegar la Edge Function: `supabase functions deploy login-telefono`.

## Flujo de Trabajo (Git)
*   `main`: Solo releases estables.
*   `develop`: Rama de integración del sprint.
*   `dev/<nombre>`: Ramas personales de desarrollo.
*   `hu/HU-XXX`: Ramas para historias de usuario específicas.

## Licencia
© 2026 Groovy Cow — Universidad del Valle. Todos los derechos reservados. Ver [LICENSE](./LICENSE).