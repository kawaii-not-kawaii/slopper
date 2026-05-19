# Codebase Structure

**Analysis Date:** 2026-05-16

## Directory Layout

```
slopper/
├── settings.gradle.kts        # Module inclusion; rootProject.name = "StashAndroid"
├── build.gradle.kts           # Root: applies ktlint/detekt/OWASP to all subprojects
├── gradle.properties
├── gradle/
│   └── libs.versions.toml     # Centralized version catalog (libs.* + plugins.stash.*)
├── bootstrap.sh               # First-clone setup script
├── keystore.properties.example
├── local.properties           # SDK location (untracked)
├── README.md
├── DEVICE_TESTING.md
├── app/                       # :app — Android Application module (composition root)
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/io/stashapp/android/
│       │   ├── StashApp.kt          # @HiltAndroidApp
│       │   └── MainActivity.kt      # Single Activity + NavHost + RootViewModel
│       └── res/                     # Launcher icon, theme, network_security_config
├── build-logic/               # Included build — Gradle convention plugins
│   ├── settings.gradle.kts
│   └── convention/
│       ├── build.gradle.kts
│       └── src/main/kotlin/io/stashapp/android/buildlogic/
│           ├── AndroidApplicationConventionPlugin.kt   # id "stash.android.application"
│           ├── AndroidLibraryConventionPlugin.kt       # id "stash.android.library"
│           ├── AndroidComposeConventionPlugin.kt       # id "stash.android.compose"
│           ├── AndroidHiltConventionPlugin.kt          # id "stash.android.hilt"
│           ├── AndroidFeatureConventionPlugin.kt       # id "stash.android.feature"
│           └── KotlinAndroid.kt                        # shared Kotlin/Android config
├── core/                      # Cross-feature library modules
│   ├── common/                # :core:common — AppResult, AppError, dispatcher qualifiers
│   ├── model/                 # :core:model  — domain entities (Scene, Connection, Queue, …)
│   ├── network/               # :core:network — Apollo GraphQL + OkHttp + auth interceptor
│   │   └── src/main/
│   │       ├── java/io/stashapp/android/core/network/
│   │       │   ├── StashEndpoint.kt
│   │       │   ├── StashEndpointProvider.kt
│   │       │   └── di/NetworkModule.kt
│   │       └── graphql/io/stashapp/android/graphql/operations/   # *.graphql operations
│   ├── data/                  # :core:data — repo impls, paging sources, DataStore prefs
│   │   └── src/main/java/io/stashapp/android/core/data/
│   │       ├── di/DataModule.kt
│   │       ├── browse/  connection/  scene/  prefs/
│   ├── domain/                # :core:domain — repo interfaces + query/filter/sort types
│   ├── designsystem/          # :core:designsystem — Material3 theme + reusable components
│   └── ui/                    # :core:ui — shared Compose UI (nav registry, image loader)
├── feature/                   # User-visible screens (one module per top-level destination)
│   ├── connection/            # Server connect / API-key entry
│   ├── home/                  # Home rails (continue watching, recently released, …)
│   ├── library/               # Paged scene browsing with filter sheet
│   ├── browse/                # Performers / Studios / Tags listings
│   ├── detail/                # Scene detail
│   ├── player/                # ExoPlayer-based playback (+ FFmpeg decoders via nextlib)
│   └── settings/              # App + player settings (depends on :feature:player for codec caps)
├── baselineprofile/           # :baselineprofile — macrobenchmark module
│   └── src/main/              # Generates baseline-prof.txt consumed by :app
├── config/
│   ├── detekt/detekt.yml      # Detekt config shared across subprojects
│   └── owasp-suppressions.xml # OWASP dependency-check suppressions
├── tools/
│   └── ffmpeg-extension/      # Optional custom Media3 FFmpeg AAR builder
└── .planning/                 # GSD planning artifacts (this directory)
```

## Module Dependency Graph

```
                            :app
                              │
       ┌──────────────────────┼──────────────────────────────┐
       │                      │                              │
:feature:home  :feature:library  :feature:browse  …  :feature:player
       │                      │                              │
       └──────────┬───────────┴──────────────────────────────┘
                  │ (via AndroidFeatureConventionPlugin)
                  ▼
       :core:ui ──► :core:designsystem
            │            (Material3 + Coil)
            ▼
       :core:domain  ◄── :core:data ──► :core:network ──► (Apollo runtime)
            │                │                  │
            └──► :core:model ◄┘                 ▼
                     │              (Apollo codegen: io.stashapp.android.graphql)
                     ▼
                 :core:common  (AppResult, dispatcher qualifiers)

:baselineprofile ──instruments──► :app
```

Notes:
- Every `:feature:*` automatically gets `:core:ui`, `:core:designsystem`, `:core:domain`, `:core:model`, `:core:common` via `AndroidFeatureConventionPlugin` (`build-logic/convention/src/main/kotlin/io/stashapp/android/buildlogic/AndroidFeatureConventionPlugin.kt`).
- `:feature:library`, `:feature:player`, `:feature:settings` additionally pull `:core:data` for DataStore preference classes.
- `:feature:settings` depends on `:feature:player` for `CodecCapabilities` — flagged for extraction to `:core:player-capabilities` in `feature/settings/build.gradle.kts`.
- `:core:data` uses `api(project(":core:domain"))` and `api(project(":core:network"))` so consumers see domain types transitively.

## Directory Purposes

**`app/`:**
- Purpose: Android Application module — composition root, single Activity, NavHost.
- Contains: `StashApp` (`@HiltAndroidApp`), `MainActivity` (`@AndroidEntryPoint`), `RootViewModel`, app resources, ABI-split / signing / lint / proguard config.
- Key files: `app/src/main/java/io/stashapp/android/MainActivity.kt`, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `app/proguard-rules.pro`, `app/lint-baseline.xml`.

**`build-logic/convention/`:**
- Purpose: Gradle included build that hosts five reusable convention plugins. Every module's `build.gradle.kts` is ~10 lines because the plugins encode SDK levels, Compose setup, Hilt+KSP wiring, and core-module dependencies.
- Plugin IDs registered (see `build-logic/convention/build.gradle.kts`): `stash.android.application`, `stash.android.library`, `stash.android.compose`, `stash.android.hilt`, `stash.android.feature`.

**`core/common/`:**
- Purpose: Universal Kotlin utilities with zero Android deps.
- Key files: `core/common/src/main/java/io/stashapp/android/core/common/Result.kt`, `.../di/Dispatchers.kt`.

**`core/model/`:**
- Purpose: Plain domain entities. `@Serializable` enables persistence in DataStore JSON.
- Key files: `core/model/src/main/java/io/stashapp/android/core/model/Scene.kt`, `Connection.kt`, `Queue.kt`, `BrowseEntity.kt`.

**`core/network/`:**
- Purpose: Apollo Kotlin 4 client + OkHttp + auth/URL-rewrite interceptor. Hosts `.graphql` operations and Apollo-generated code under package `io.stashapp.android.graphql`.
- Key files: `core/network/src/main/java/io/stashapp/android/core/network/di/NetworkModule.kt`, `.../StashEndpoint.kt`, `.../StashEndpointProvider.kt`, `core/network/src/main/graphql/io/stashapp/android/graphql/operations/*.graphql`.

**`core/data/`:**
- Purpose: Repository implementations, paging sources, server-DTO ↔ domain mappers, DataStore-backed preference stores, Hilt `@Binds` module.
- Subpackages: `browse/`, `connection/`, `scene/`, `prefs/`, `di/`.
- Key files: `core/data/src/main/java/io/stashapp/android/core/data/di/DataModule.kt`, `.../scene/DefaultSceneRepository.kt`, `.../scene/ScenePagingSource.kt`, `.../connection/EndpointStateHolder.kt`, `.../prefs/ConnectionStore.kt`.

**`core/domain/`:**
- Purpose: Repository interfaces + immutable query/filter/sort types. Pure Kotlin + AndroidX Paging only.
- Key files: `core/domain/src/main/java/io/stashapp/android/core/domain/SceneRepository.kt`, `ConnectionRepository.kt`, `BrowseRepository.kt`.

**`core/designsystem/`:**
- Purpose: Material3 theme, color, typography, and reusable Compose visual components (e.g. `SceneCard`).
- Key files: `core/designsystem/src/main/java/io/stashapp/android/core/designsystem/theme/Theme.kt`, `Color.kt`, `Type.kt`, `component/SceneCard.kt`.

**`core/ui/`:**
- Purpose: Shared non-design-system Compose utilities — image loader, bottom nav, central `Routes` registry.
- Key files: `core/ui/src/main/java/io/stashapp/android/core/ui/nav/Routes.kt`, `nav/BottomNav.kt`, `nav/NavCustomizeSheet.kt`, `image/StashImageLoader.kt`.

**`feature/<name>/`:**
- Purpose: One destination = one module. Each contains a Compose `<Name>Screen.kt` plus a `<Name>ViewModel.kt`.
- Subpackages: kept flat — feature-specific helpers live alongside (e.g. `feature/player/PlayerQueue.kt`, `CodecCapabilities.kt`, `StashPlayerFactory.kt`; `feature/library/FilterSheet.kt`).

**`baselineprofile/`:**
- Purpose: AndroidX Macrobenchmark module that records a baseline profile against the installed `:app`. Not part of normal builds — invoked only by `./gradlew :app:generateBaselineProfile`.

**`config/`:**
- Purpose: Shared static-analysis config consumed by the root `subprojects { … }` block.
- Files: `config/detekt/detekt.yml`, `config/owasp-suppressions.xml`.

**`tools/ffmpeg-extension/`:**
- Purpose: Optional NDK builder for a custom Media3 FFmpeg decoder AAR. Resulting `media3-decoder-ffmpeg*.aar` files can be dropped into `feature/player/libs/` and are auto-picked up by `feature/player/build.gradle.kts`. Normal builds use the prebuilt `nextlib-media3ext` artifact instead.

## Key File Locations

**Entry Points:**
- `app/src/main/java/io/stashapp/android/StashApp.kt` — `@HiltAndroidApp` Application
- `app/src/main/java/io/stashapp/android/MainActivity.kt` — single Activity + `NavHost` + `RootViewModel`
- `app/src/main/AndroidManifest.xml` — declares `StashApp`, `MainActivity`, permissions

**Configuration:**
- `settings.gradle.kts` — module inclusion list, `pluginManagement { includeBuild("build-logic") }`
- `build.gradle.kts` (root) — applies ktlint + detekt + OWASP dependency-check across subprojects
- `gradle/libs.versions.toml` — version catalog (`libs.*`, plugins `stash.android.*`)
- `app/build.gradle.kts` — signing configs, build types (debug/benchmark/release), ABI splits, lint baseline
- `app/src/main/res/xml/network_security_config.xml` — cleartext-HTTP allowlist
- `gradle.properties`, `keystore.properties.example`, `local.properties`, `app/proguard-rules.pro`

**Core Logic:**
- Repository interfaces — `core/domain/src/main/java/io/stashapp/android/core/domain/`
- Repository implementations — `core/data/src/main/java/io/stashapp/android/core/data/` (`browse/`, `connection/`, `scene/`)
- DI graph — `core/data/src/main/java/io/stashapp/android/core/data/di/DataModule.kt` and `core/network/src/main/java/io/stashapp/android/core/network/di/NetworkModule.kt`
- GraphQL operations — `core/network/src/main/graphql/io/stashapp/android/graphql/operations/*.graphql`
- Generated Apollo code — `core/network/build/generated/source/apollo/` (package `io.stashapp.android.graphql`)
- Route registry — `core/ui/src/main/java/io/stashapp/android/core/ui/nav/Routes.kt`

**Testing:**
- No dedicated `src/test/` or `src/androidTest/` directories exist yet beyond the `:baselineprofile` macrobenchmark module (`baselineprofile/src/main/`).

## Naming Conventions

**Modules:**
- Colon-prefixed Gradle paths: `:core:<name>`, `:feature:<name>`, `:app`, `:baselineprofile`.
- Namespace mirrors the path: `io.stashapp.android.core.<name>` / `io.stashapp.android.feature.<name>`.

**Files:**
- Kotlin file = primary class name in PascalCase: `MainActivity.kt`, `DefaultSceneRepository.kt`, `ScenePagingSource.kt`, `HomeViewModel.kt`.
- Compose screens: `<Feature>Screen.kt` (e.g. `HomeScreen.kt`, `LibraryScreen.kt`).
- ViewModels: `<Feature>ViewModel.kt`.
- Repository impls: `Default<Interface>.kt` (e.g. `DefaultSceneRepository` implements `SceneRepository`).
- Hilt modules: `<Area>Module.kt` (`DataModule.kt`, `NetworkModule.kt`).
- GraphQL ops: `<OperationName>.graphql` matches the generated Kotlin class name 1:1 (`FindScenes.graphql` → `FindScenesQuery`).

**Packages:**
- Root: `io.stashapp.android.*`
- Core: `io.stashapp.android.core.<module>.*`
- Feature: `io.stashapp.android.feature.<module>.*`
- Apollo-generated: `io.stashapp.android.graphql.*` (configured in `core/network/build.gradle.kts`)

**Build conventions:**
- Convention plugin IDs use the `stash.android.<kind>` scheme; module build files alias them as `libs.plugins.stash.android.<kind>`.

## Where to Add New Code

**New user-facing screen (top-level destination):**
1. Create `feature/<name>/build.gradle.kts` with only `plugins { alias(libs.plugins.stash.android.feature) }` and `android { namespace = "io.stashapp.android.feature.<name>" }`.
2. Add `include(":feature:<name>")` in `settings.gradle.kts`.
3. Add files under `feature/<name>/src/main/java/io/stashapp/android/feature/<name>/`:
   - `<Name>Screen.kt` (Composable + state hoisting)
   - `<Name>ViewModel.kt` (`@HiltViewModel`)
4. Register route in `core/ui/src/main/java/io/stashapp/android/core/ui/nav/Routes.kt`.
5. Add `composable(Routes.<Name>) { … }` and `implementation(project(":feature:<name>"))` in `app/build.gradle.kts` + `MainActivity.kt`.

**New repository / data source:**
1. Define interface + domain types in `core/domain/src/main/java/io/stashapp/android/core/domain/<Name>Repository.kt`.
2. Implement in `core/data/src/main/java/io/stashapp/android/core/data/<area>/Default<Name>Repository.kt` as `@Singleton class @Inject constructor(...)`.
3. Add a `@Binds` line to `core/data/src/main/java/io/stashapp/android/core/data/di/DataModule.kt`.
4. Add any new GraphQL operations under `core/network/src/main/graphql/io/stashapp/android/graphql/operations/<Name>.graphql`.

**New shared UI component:**
- Visual / themed primitive → `core/designsystem/src/main/java/io/stashapp/android/core/designsystem/component/`.
- Stateful or feature-spanning Compose helper → `core/ui/src/main/java/io/stashapp/android/core/ui/`.
- Never put cross-feature UI inside a `feature/<name>/` module.

**New domain entity / DTO:**
- `core/model/src/main/java/io/stashapp/android/core/model/<Name>.kt` (no Android imports; `@Serializable` if persisted).

**New preference / persisted setting:**
- Add the DataStore key + flow to an existing store under `core/data/src/main/java/io/stashapp/android/core/data/prefs/` (`UiPreferences.kt`, `PlayerPreferences.kt`, `ConnectionStore.kt`) or create a sibling file there.
- If consumers should not see DataStore directly, expose a `:core:domain` interface and `@Binds` it in `DataModule.kt`.

**New GraphQL operation:**
- Drop `*.graphql` into `core/network/src/main/graphql/io/stashapp/android/graphql/operations/`. Apollo's KSP step generates the Kotlin class on the next build into package `io.stashapp.android.graphql`.

**New shared dependency version:**
- Add to `gradle/libs.versions.toml` under `[versions]` + `[libraries]`; reference via `libs.<name>` in build files. Never hardcode versions in module `build.gradle.kts`.

## Special Directories

**`core/network/src/main/graphql/`:**
- Purpose: Apollo operation sources (`.graphql`) — compiled by the Apollo Gradle plugin into Kotlin classes under `io.stashapp.android.graphql`.
- Generated: No (source) → generated outputs land in `core/network/build/generated/source/apollo/`.
- Committed: Yes.

**`build/` (any module):**
- Purpose: Gradle/AGP/Apollo/KSP build outputs.
- Generated: Yes.
- Committed: No (ignored).

**`feature/player/libs/`:**
- Purpose: Optional drop-in directory for hand-built `media3-decoder-ffmpeg*.aar` files; `feature/player/build.gradle.kts` auto-includes any AAR matching that glob.
- Generated: Yes (by `tools/ffmpeg-extension/build.sh`).
- Committed: No.

**`.planning/`:**
- Purpose: GSD planning artifacts (codebase docs, phase plans). This directory.
- Generated: Yes (by GSD commands).
- Committed: Yes.

**`local.properties`:**
- Purpose: Android SDK location.
- Committed: No.

**`keystore.properties` (not present, only `*.example`):**
- Purpose: Release signing credentials, picked up by `app/build.gradle.kts` `signingConfigs`. Falls back to env vars `STASH_KEYSTORE_*`, then debug keystore.
- Committed: No.

---

*Structure analysis: 2026-05-16*
