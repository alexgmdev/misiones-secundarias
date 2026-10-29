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
2. Copiar el archivo de variables de entorno: `cp .env.example .env`
3. Solicitar las credenciales de Supabase al Líder Técnico e ingresarlas en el `.env`.
4. Abrir el proyecto en Android Studio y sincronizar Gradle.

## Flujo de Trabajo (Git)
*   `main`: Solo releases estables.
*   `develop`: Rama de integración del sprint.
*   `dev/<nombre>`: Ramas personales de desarrollo.
*   `hu/HU-XXX`: Ramas para historias de usuario específicas.

## Licencia
© 2026 Groovy Cow — Universidad del Valle. Todos los derechos reservados. Ver [LICENSE](./LICENSE).