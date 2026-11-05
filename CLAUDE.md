# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

**Misiones Secundarias** — Android MVP (academic project, Universidad del Valle 2026, by Groovy Cow) that connects **Publicadores** (people who need help with a task) with **Solucionadores** (people willing to help) in Cali, via an interactive map of "missions". Docs, KDoc and user-facing strings are in Spanish (with correct accents); code identifiers are in English.

## Commands

Requires JDK 17 (toolchain enforced in `app/build.gradle.kts`) and an Android SDK (`sdk.dir` in `local.properties`).

```bash
./gradlew assembleDebug                 # build debug APK
./gradlew testDebugUnitTest             # JVM unit tests (app/src/test)
./gradlew :app:testDebugUnitTest --tests "com.groovycow.misionessecundarias.domain.usecase.GetMissionsUseCaseTest"   # single class
./gradlew :app:testDebugUnitTest --tests "*GetMissionsUseCaseTest.someMethod"                                         # single method
./gradlew ktlintCheck                   # lint (ktlintFormat to auto-fix); Gradle files are excluded
./gradlew detekt                        # static analysis, config in detekt.yml
./gradlew connectedAndroidTest          # instrumented tests (device/emulator)
```

**CI** (`.github/workflows/ci.yml`) runs ktlintCheck → detekt → testDebugUnitTest → assembleDebug on push to `develop`/`dev/*` and on PRs to `develop`/`main`. All four must pass. When no local Android SDK is available, pushing to the `dev/*` branch and checking CI is the verification path.

Test stack: JUnit 4, MockK, kotlinx-coroutines-test. Style: `kotlin.code.style=official`, `.editorconfig` (Composable functions exempt from naming rule, wildcard imports allowed), detekt max line length 150 and LongMethod 250.

## Architecture

Single module `:app`, package `com.groovycow.misionessecundarias`, **layered** (not per-feature):

- `domain/model`, `domain/repository` (interfaces), `domain/usecase` — use cases wrap repository calls and return `Result<T>`; repositories throw.
- `data/repository` — Supabase-backed implementations (`*RepositoryImpl`) using `postgrest-kt` / `gotrue-kt`.
- `ui/<feature>/` — Compose screens + ViewModels exposing `StateFlow<XUiState>`.
- `di/` — Koin modules (`supabaseModule`, `repositoryModule`), started in `MisionesApp` (Application class).
- Navigation is **manual**: `MainActivity` holds a `mutableStateOf<AppDestination>` and switches screens with `when` (no navigation-compose). `ui/navigation/MainMenuScreen.kt` is the main menu.

Stack: Compose + Material 3, Koin, supabase-kt 2.6.0 (`gotrue-kt` module name, Ktor OkHttp 2.3.12 engine, kotlinx.serialization), Room via KSP (declared, not yet used), MapLibre. Versions in `gradle/libs.versions.toml` — add libraries there.

## Configuration & Backend

- Supabase credentials: `SUPABASE_URL` and `SUPABASE_ANON_KEY` in **`local.properties`** (git-ignored), injected as `BuildConfig` fields. Ask the tech lead for them. (`README.md` still mentions `.env`; that is outdated.)
- Database schema lives in `supabase/schema.sql` (single file, applied manually by the tech lead; no Supabase CLI migrations). It includes the `handle_new_user` trigger that creates a `profiles` row from `auth.users.raw_user_meta_data`. Schema changes to the shared DB must be coordinated with the team.

## Spec-Driven Development

Planned changes live in `openspec/` (config in `openspec/config.yaml`, strict TDD enabled). Each change under `openspec/changes/<name>/` has `proposal.md`, `specs/*/spec.md` (Given/When/Then), `design.md` and `tasks.md` — read them before implementing that change. Active change: `auth-registro-login` (HU-001..004).

## Product Requirements (untracked reference files at repo root)

- `Product_Backlog_Misiones_Secundarias.xlsx - Product Backlog.tsv` — user stories `HU-XXX` with business rules and Gherkin acceptance criteria.
- `Misiones secundarias.md` — project charter (scope, exclusions, team roles).
- `prototipar` — draw.io (mxfile XML) UI prototype (missions/forum screens only).

## Git Workflow

- `main`: stable releases. `develop`: sprint integration branch (PRs target `develop`).
- `dev/<nombre>`: personal dev branches. `hu/HU-XXX`: per-user-story branches.
