<!-- refreshed: 2026-05-19 -->
# Architecture

**Analysis Date:** 2026-05-16

## System Overview

```text
┌─────────────────────────────────────────────────────────────────────┐
│                          :app  (StashApp / MainActivity)            │
│  `app/src/main/java/io/stashapp/android/MainActivity.kt`            │
│  `app/src/main/java/io/stashapp/android/StashApp.kt`                │
│  Hosts RootViewModel + Compose NavHost (single-activity)            │
└──────────┬────────────────────────────────────────────────┬─────────┘
           │ depends on every :feature:* module             │
           ▼                                                ▼
┌──────────────────────────────────────────────────────────────────────┐
│  :feature:connection │ :feature:home  │ :feature:library │ :feature:│
│  :feature:browse     │ :feature:detail│ :feature:player  │ :settings│
│  Each = Compose screen + HiltViewModel (one VM per screen)          │
│  `feature/<name>/src/main/java/io/stashapp/android/feature/<name>/` │
└──────────┬───────────────────────────────────────────────┬──────────┘
           │ depends on core/ui, core/designsystem,        │
           │ core/domain, core/model, core/common          │
           ▼                                               ▼
┌──────────────────────────────────────────────────────────────────────┐
│                      :core:ui  /  :core:designsystem                 │
│  Shared Compose components, theme, NavHost route registry            │
│  `core/ui/src/main/java/io/stashapp/android/core/ui/nav/Routes.kt`   │
│  `core/designsystem/src/main/java/.../theme/Theme.kt`                │
└──────────┬──────────────────────────────────────────────────────────┘
           │ ViewModels inject :core:domain interfaces
           ▼
┌──────────────────────────────────────────────────────────────────────┐
│                            :core:domain                              │
│  Pure repository interfaces + domain types (no Android deps)         │
│  `core/domain/src/main/java/io/stashapp/android/core/domain/`        │
│   SceneRepository.kt, ConnectionRepository.kt, BrowseRepository.kt   │
└──────────┬──────────────────────────────────────────────────────────┘
           │ Hilt @Binds in :core:data wires impls
           ▼
┌──────────────────────────────────────────────────────────────────────┐
│                            :core:data                                │
│  Default*Repository implementations, paging sources, DataStore prefs │
│  `core/data/src/main/java/io/stashapp/android/core/data/`            │
│   di/DataModule.kt        (Hilt @Binds repo impls)                   │
│   scene/DefaultSceneRepository.kt, ScenePagingSource.kt              │
│   connection/EndpointStateHolder.kt (impl of StashEndpointProvider)  │
│   prefs/ConnectionStore.kt, UiPreferences.kt, PlayerPreferences.kt   │
└──────────┬────────────────────────────────┬─────────────────────────┘
           │                                │
           ▼                                ▼
┌─────────────────────────────┐   ┌──────────────────────────────────┐
│       :core:network         │   │   AndroidX DataStore (prefs)     │
│  Apollo GraphQL client +    │   │   EncryptedSharedPreferences     │
│  OkHttp + auth interceptor  │   │   (api key) — owned by           │
│  `core/network/src/main/`   │   │   ConnectionStore                │
│   di/NetworkModule.kt       │   └──────────────────────────────────┘
│   StashEndpointProvider.kt  │
│   graphql/ (Apollo codegen) │
└──────────┬──────────────────┘
           ▼
┌──────────────────────────────────────────────────────────────────────┐
│                  Stash GraphQL server (remote)                       │
│  URL resolved at request time by StashAuthInterceptor                │
│  `core/network/src/main/java/.../di/NetworkModule.kt`                │
└──────────────────────────────────────────────────────────────────────┘

Cross-cutting (depended on by most layers):
  :core:model    — DTOs / domain entities (no Android deps)
  :core:common   — AppResult, AppError, Dispatcher qualifiers
```

## Component Responsibilities

| Component | Responsibility | File |
|-----------|----------------|------|
| `StashApp` | `@HiltAndroidApp` entry point; supplies Coil `SingletonImageLoader.Factory` | `app/src/main/java/io/stashapp/android/StashApp.kt` |
| `MainActivity` | `@AndroidEntryPoint` single Activity; sets edge-to-edge + high-refresh-rate window; hosts Compose NavHost | `app/src/main/java/io/stashapp/android/MainActivity.kt` |
| `RootViewModel` | Decides start destination (Connection vs Home) from `ConnectionRepository.activeServer()` | `app/src/main/java/io/stashapp/android/MainActivity.kt` (same file) |
| `Routes` | Central type-safe route registry shared across feature modules | `core/ui/src/main/java/io/stashapp/android/core/ui/nav/Routes.kt` |
| `MainBottomBar` / `NavCustomizeSheet` / `MoreSheet` | Bottom-nav chrome + user-customizable visible items | `core/ui/src/main/java/io/stashapp/android/core/ui/nav/BottomNav.kt`, `.../NavCustomizeSheet.kt` |
| `StashEndpointProvider` (interface) / `EndpointStateHolder` (impl) | Owns active server endpoint; broken out to avoid Apollo↔repo cycle | `core/network/src/main/java/io/stashapp/android/core/network/StashEndpointProvider.kt`, `core/data/src/main/java/io/stashapp/android/core/data/connection/EndpointStateHolder.kt` |
| `SceneRepository` / `DefaultSceneRepository` | Paged + one-shot scene queries, mutations (rating, O-counter, play activity) | `core/domain/src/main/java/io/stashapp/android/core/domain/SceneRepository.kt`, `core/data/src/main/java/io/stashapp/android/core/data/scene/DefaultSceneRepository.kt` |
| `ConnectionRepository` / `DefaultConnectionRepository` | Connect/disconnect Stash servers, list known servers, switch active | `core/domain/.../ConnectionRepository.kt`, `core/data/.../connection/DefaultConnectionRepository.kt` |
| `BrowseRepository` / `DefaultBrowseRepository` | Paged listings of performers/studios/tags | `core/domain/.../BrowseRepository.kt`, `core/data/.../browse/DefaultBrowseRepository.kt` |
| `NetworkModule` | Provides `OkHttpClient`, `ApolloClient`, `StashAuthInterceptor` (rewrites URL + adds `ApiKey` header per request) | `core/network/src/main/java/io/stashapp/android/core/network/di/NetworkModule.kt` |
| `DataModule` | Hilt `@Binds` mapping repo interfaces to `Default*` impls | `core/data/src/main/java/io/stashapp/android/core/data/di/DataModule.kt` |
| `AppResult` / `AppError` | Sealed result + error taxonomy used in repo signatures | `core/common/src/main/java/io/stashapp/android/core/common/Result.kt` |

## Pattern Overview

**Overall:** Modularized **Clean Architecture** (Now-in-Android style) with **MVVM + Unidirectional Data Flow** at the UI layer. The whole product is one Activity (`MainActivity`) hosting a Compose `NavHost`; each route maps to one feature module with a single `HiltViewModel` exposing `StateFlow<UiState>`.

**Key Characteristics:**
- Strict three-tier layering: `feature → core:domain ← core:data → core:network` (interfaces in `:core:domain`, impls in `:core:data`; UI never imports `:core:data` types directly, only domain interfaces — except where DataStore prefs are reused, e.g. `feature/library` and `feature/player` explicitly add `implementation(project(":core:data"))`).
- Single-Activity + Compose Navigation (`androidx.navigation.compose`). No fragments.
- DI everywhere via **Hilt** with KSP; `SingletonComponent` is the only scope used.
- Networking is exclusively **Apollo Kotlin 4** GraphQL over OkHttp; the Apollo client is constructed once at `localhost/graphql` and an `HttpInterceptor` rewrites the URL + injects the `ApiKey` header per request, so endpoint switching does not require client rebuilds.
- Paging via **AndroidX Paging 3** (`Pager` + `PagingSource` in `:core:data`, `LazyPagingItems` in feature modules).
- Persistence: **DataStore Preferences** for UI prefs + connection metadata; **EncryptedSharedPreferences** (`androidx.security:security-crypto`) for the API key.
- Build conventions centralized in `build-logic/convention/` — five plugins (`stash.android.application/library/compose/hilt/feature`) keep module build files to ~10 lines.

## Layers

**`:app` (application shell):**
- Purpose: Application class, MainActivity, top-level NavHost, root nav decision.
- Location: `app/src/main/java/io/stashapp/android/`
- Contains: `StashApp.kt`, `MainActivity.kt` (also holds `RootViewModel`).
- Depends on: every `:core:*` module + every `:feature:*` module (it is the composition root).
- Used by: nothing (terminal node).

**`:feature:*` (UI features):**
- Purpose: One Compose screen + one ViewModel per user-visible destination.
- Location: `feature/<name>/src/main/java/io/stashapp/android/feature/<name>/`
- Contains: `<Name>Screen.kt`, `<Name>ViewModel.kt`, optional screen-specific helpers (e.g. `feature/player/PlayerQueue.kt`, `CodecCapabilities.kt`, `StashPlayerFactory.kt`; `feature/library/FilterSheet.kt`).
- Depends on (via `AndroidFeatureConventionPlugin`): `:core:ui`, `:core:designsystem`, `:core:domain`, `:core:model`, `:core:common`.
- May additionally depend on `:core:data` (only `feature:library`, `feature:player`, `feature:settings` do — for `UiPreferences` / `PlayerPreferences`).
- Used by: `:app` only.

**`:core:ui`:**
- Purpose: Shared Compose UI utilities — image loader, bottom-nav, route registry.
- Location: `core/ui/src/main/java/io/stashapp/android/core/ui/`
- Depends on: `:core:designsystem`, `:core:model`, `:core:domain`, `:core:network`, Coil, OkHttp.

**`:core:designsystem`:**
- Purpose: Material3 theme, typography, color, reusable design-system components (e.g. `SceneCard`).
- Location: `core/designsystem/src/main/java/io/stashapp/android/core/designsystem/`
- Depends on: Compose BOM, Coil.

**`:core:domain`:**
- Purpose: Repository **interfaces** and domain query/filter/sort types. Pure Kotlin (no Android, no Apollo).
- Location: `core/domain/src/main/java/io/stashapp/android/core/domain/`
- Depends on: `:core:model`, `:core:common`, AndroidX Paging.

**`:core:data`:**
- Purpose: Repository **implementations**, paging sources, mappers, DataStore preference stores, Hilt module that `@Binds` impls.
- Location: `core/data/src/main/java/io/stashapp/android/core/data/`
- Subpackages: `browse/`, `connection/`, `scene/`, `prefs/`, `di/`.
- Depends on: `:core:domain` (api), `:core:network` (api), DataStore, security-crypto, Paging, kotlinx.serialization.

**`:core:network`:**
- Purpose: Apollo GraphQL client + OkHttp + auth/URL interceptor + endpoint provider interface. Hosts `.graphql` operation files and Apollo-generated code under package `io.stashapp.android.graphql`.
- Location: `core/network/src/main/java/io/stashapp/android/core/network/`, GraphQL ops in `core/network/src/main/graphql/io/stashapp/android/graphql/operations/`.
- Depends on: Apollo runtime, OkHttp, `:core:common`.

**`:core:model`:**
- Purpose: Plain Kotlin domain entities (`Scene`, `SceneSummary`, `SceneDetail`, `Connection`, `Queue`, `BrowseEntity`). `@Serializable` where needed.
- Location: `core/model/src/main/java/io/stashapp/android/core/model/`

**`:core:common`:**
- Purpose: `AppResult` / `AppError` sealed types, coroutine dispatcher qualifiers (`@IoDispatcher`, `@DefaultDispatcher`, `@MainDispatcher`).
- Location: `core/common/src/main/java/io/stashapp/android/core/common/`

**`:baselineprofile`:**
- Purpose: Macrobenchmark module that generates the baseline profile installed by `app`'s `androidx.profileinstaller`. Not built normally; invoked via `./gradlew :app:generateBaselineProfile`.
- Location: `baselineprofile/src/main/`

## Data Flow

### Primary Request Path (e.g., Home screen rails)

1. `MainActivity.onCreate` → composes `StashAppContent` which creates `rememberNavController()` and a `RootViewModel` (`app/src/main/java/io/stashapp/android/MainActivity.kt:84`).
2. `RootViewModel.init` collects `ConnectionRepository.activeServer()` and emits `Routes.Home` when an endpoint exists (`MainActivity.kt:70`).
3. `NavHost` composes `HomeScreen`, which `hiltViewModel()`s a `HomeViewModel` (`feature/home/src/main/java/io/stashapp/android/feature/home/HomeViewModel.kt`).
4. `HomeViewModel.load()` launches `viewModelScope` coroutines that call `sceneRepository.scenes(query, limit=20)` per rail in parallel (`HomeViewModel.kt`).
5. `DefaultSceneRepository.scenes` builds a `FindFilterType` and calls `apollo.query(FindScenesQuery(...))` (`core/data/src/main/java/io/stashapp/android/core/data/scene/DefaultSceneRepository.kt`).
6. `StashAuthInterceptor.intercept` reads the current endpoint from `StashEndpointProvider`, rewrites `request.url` to `endpoint.graphqlUrl`, and attaches the `ApiKey` header (`core/network/src/main/java/io/stashapp/android/core/network/di/NetworkModule.kt:65`).
7. Apollo deserializes → `SceneMapper` (`core/data/.../scene/SceneMapper.kt`) maps to `SceneSummary` from `:core:model`.
8. `HomeViewModel` updates `MutableStateFlow<HomeUiState>`; `HomeScreen` re-composes off `collectAsStateWithLifecycle()`.

### Paged List Flow (Library / Browse)

1. ViewModel exposes `Flow<PagingData<SceneSummary>>` built from `Pager(PagingConfig(pageSize=40, prefetchDistance=40, initialLoadSize=40))` (`DefaultSceneRepository.pagedScenes`, `core/data/.../scene/DefaultSceneRepository.kt:35`).
2. `ScenePagingSource.load` issues an Apollo query and returns a `LoadResult.Page` (`core/data/.../scene/ScenePagingSource.kt`).
3. UI uses `collectAsLazyPagingItems()` inside the `LazyColumn`/`LazyVerticalGrid`.

### Connection / Endpoint Switching

1. `ConnectionScreen` → `ConnectionViewModel.connect(baseUrl, apiKey)`.
2. `DefaultConnectionRepository.connect` validates via `ServerInfoQuery`, persists via `ConnectionStore` (EncryptedSharedPreferences for the API key), then calls `EndpointStateHolder.set(endpoint)` (`core/data/.../connection/`).
3. Every in-flight + future Apollo request automatically picks up the new endpoint through the interceptor — the `ApolloClient` is never recreated.

**State Management:**
- ViewModel-owned `MutableStateFlow<UiState>` exposed as immutable `StateFlow<UiState>`; UI collects via `collectAsState`/`collectAsStateWithLifecycle`.
- Paging streams cached with `cachedIn(viewModelScope)` (`feature/library/.../LibraryViewModel.kt`).
- Persistent UI state (default filters, bottom-nav customization, player prefs) lives in DataStore via `UiPreferences` / `PlayerPreferences` (`core/data/src/main/java/io/stashapp/android/core/data/prefs/`).

## Key Abstractions

**Repository interfaces (`:core:domain`):**
- Purpose: hide Apollo + storage details from UI. All methods return either `Flow<PagingData<T>>` (paged) or `suspend AppResult<T>` (one-shot).
- Examples: `SceneRepository`, `ConnectionRepository`, `BrowseRepository`.
- Pattern: interface in `:core:domain`, `Default*` impl in `:core:data`, bound via `@Binds` in `DataModule`.

**`AppResult<T>` + `AppError` (`core/common/.../Result.kt`):**
- Purpose: typed error channel for repository calls; UI surfaces `AppError.Network` / `Auth` / `NotFound` / `Server` / `Unknown` distinctly.
- Pattern: sealed interface + sealed class; never throws across repo boundary.

**`StashEndpointProvider` / `StashEndpoint`:**
- Purpose: single source of truth for "where is the user's Stash server?". `StashEndpoint.resolve(path)` joins relative server paths to the base URL and **rejects cross-origin absolute URLs** (security guard against a malicious server returning attacker URLs in `paths.stream`). `core/network/.../StashEndpoint.kt:21`.

**`Routes` object (`core/ui/.../nav/Routes.kt`):**
- Purpose: single typed registry of Compose-Nav route patterns and builder helpers (`Routes.player(...)`, `Routes.sceneDetail(...)`).
- Pattern: feature modules use these helpers — never raw strings — to avoid divergence.

## Entry Points

**Process entry:**
- Class: `io.stashapp.android.StashApp` (declared in `app/src/main/AndroidManifest.xml:18`)
- `@HiltAndroidApp` — initializes the Hilt graph.
- Also implements `coil3.SingletonImageLoader.Factory`, injecting a Hilt-built `StashImageLoaderFactory` (`core/ui/src/main/java/io/stashapp/android/core/ui/image/StashImageLoader.kt`).

**Activity entry:**
- Class: `io.stashapp.android.MainActivity` (manifest `app/src/main/AndroidManifest.xml:30`, `launchMode="singleTop"`, `supportsPictureInPicture="true"`).
- Triggers: launcher icon (`MAIN` / `LAUNCHER` intent filter).
- Responsibilities: enable edge-to-edge, request highest display refresh rate, install `StashTheme` + `NavHost`.

**Build entry points:**
- `./gradlew :app:assembleDebug` / `:app:assembleRelease`
- `./gradlew :app:generateBaselineProfile` (drives `:baselineprofile`)
- `./gradlew dependencyCheckAnalyze --no-configuration-cache` (OWASP scan, root `build.gradle.kts`)

## Architectural Constraints

- **Threading:** Single UI thread (Compose); repositories use `suspend` + `Flow` and rely on the caller's `viewModelScope` (Dispatchers.Main.immediate by default). Paging sources / Apollo run on OkHttp's dispatcher. Dispatcher qualifiers are defined (`core/common/.../di/Dispatchers.kt`) but no `DispatchersModule` provides them yet — repositories do not currently `withContext(IO)`.
- **Global state:** Three `@Singleton` instances hold mutable state — `EndpointStateHolder` (current endpoint), `ConnectionStore` (DataStore-backed), and the singleton `ApolloClient`. All other state is ViewModel-scoped.
- **Circular import avoided:** Apollo client needs the endpoint, repository needs Apollo — broken by hoisting the endpoint into `EndpointStateHolder` and injecting only the `StashEndpointProvider` interface into `NetworkModule` (documented in `core/data/.../di/DataModule.kt:26`).
- **Apollo client is built once** with placeholder `http://localhost/graphql`; per-request URL rewriting is mandatory — never call Apollo with the placeholder URL outside the interceptor path (`core/network/.../di/NetworkModule.kt:52`).
- **Cleartext HTTP allowed** (`AndroidManifest.xml:26`, `app/src/main/res/xml/network_security_config.xml`) because Stash on a LAN typically runs plain HTTP. Restricted via network-security config rather than a blanket allow.
- **ABI splits:** Release builds emit per-ABI APKs for `arm64-v8a` and `armeabi-v7a` only; `isUniversalApk = false` (`app/build.gradle.kts`).
- **`launchSingleTop` deliberately OFF** on tab navigation because Browse sub-tabs share the `browse/{kind}` pattern and nav considers pattern-level identity (`MainActivity.kt:259`). Trade-off: tab scroll position is lost on switch.

## Anti-Patterns

### Reaching across feature modules for shared code

**What happens:** `:feature:settings` declares `implementation(project(":feature:player"))` just to reuse `CodecCapabilities` (`feature/settings/build.gradle.kts`).
**Why it's wrong:** Feature modules should be siblings, not depend on each other — this creates an implicit ordering and makes both modules harder to test in isolation.
**Do this instead:** Per the comment in that build file, extract a `:core:player-capabilities` module when this grows. New shared code goes in `:core:*`, not another feature.

### Importing `:core:data` from a feature module

**What happens:** `feature/library`, `feature/player`, `feature/settings` import `UiPreferences` / `PlayerPreferences` directly from `:core:data`.
**Why it's wrong:** Bypasses the `:core:domain` abstraction layer; ties UI to the concrete preference store.
**Do this instead:** For new preference surfaces, define a small interface in `:core:domain` (e.g. `PlayerSettings`) and bind the DataStore-backed impl in `DataModule`.

### Hand-built GraphQL strings in repositories

**What happens:** Repositories should always go through Apollo-generated operation classes (`FindScenesQuery`, `SceneUpdateMutation`, …) defined in `core/network/src/main/graphql/io/stashapp/android/graphql/operations/`.
**Why it's wrong:** Skipping codegen forfeits compile-time GraphQL validation.
**Do this instead:** Add a new `.graphql` file under `core/network/src/main/graphql/...`, let Apollo's KSP step generate the class, then call it from `:core:data`.

## Error Handling

**Strategy:** Repositories return `AppResult<T>` — no exceptions cross the repo boundary. ViewModels translate `AppResult.Failure(AppError.*)` into per-screen UI state (e.g. per-rail error strings in `HomeUiState`).

**Patterns:**
- `try { … apollo.query(…).execute() … } catch` inside `Default*Repository` methods → return `AppResult.Failure(AppError.Network(...))` / `AppError.Auth(...)` based on HTTP status / exception type.
- Mutations expose the new server value (e.g. `incrementO` returns `AppResult<Int>` carrying the new count) so the UI can update optimistically and reconcile.

## Cross-Cutting Concerns

**Logging:**
- OkHttp `HttpLoggingInterceptor` at `Level.BASIC` is added **only when `FLAG_DEBUGGABLE` is set** (`core/network/.../di/NetworkModule.kt:35`) — release builds never log URLs (avoids leaking scene IDs / search terms into logcat).
- Ad-hoc `android.util.Log` calls (e.g. nav debug in `MainActivity.kt:332`).

**Validation:**
- Domain types (`SceneQuery`, `SceneFilter`) are immutable `data class`es with enum constraints (`SceneSort`, `SceneResolution`, …) — invalid combinations are unrepresentable.
- `StashEndpoint.resolve` validates server-returned URLs for same-origin (`core/network/.../StashEndpoint.kt:21`).

**Authentication:**
- API key per server, stored in `EncryptedSharedPreferences` via `ConnectionStore`.
- Attached on every GraphQL request by `StashAuthInterceptor` as the `ApiKey` HTTP header.

**Image loading:**
- Coil 3 with a custom `StashImageLoaderFactory` (`core/ui/.../image/StashImageLoader.kt`) configured as `SingletonImageLoader.Factory` from `StashApp`.

**Static analysis:**
- ktlint 1.3.1 + detekt 1.23.7 applied to every subproject via root `subprojects { … }` block (`build.gradle.kts`). detekt config: `config/detekt/detekt.yml`.
- OWASP `dependency-check` plugin fails CI on CVSS ≥ 7.0; suppressions in `config/owasp-suppressions.xml`.

---

*Architecture analysis: 2026-05-16 | Last updated: 2026-05-19*

## Phase 4 Changes (POLISH — Test Pyramid & Cleanup)

### POLISH-01 — PlayerScreen split

`feature/player/PlayerScreen.kt` was split into three focused composables to reduce file size and improve testability:

| New file | Responsibility |
|----------|---------------|
| `PlayerScreen.kt` | Top-level composable, state hoisting, navigation back, PiP |
| `PlayerControls.kt` | Play/pause, skip, repeat, shuffle, volume controls overlay |
| `PlayerTimeline.kt` | Scrubber, markers timeline bar, chapter display |

Marker collections migrated from `List<Marker>` to `ImmutableList<Marker>` (Phase 3 PERF-03 pattern) — detekt baselines regenerated to match new signatures.

### POLISH-06 — PlayerSettings + UiSettings interfaces

Two new preference interfaces extracted into `:core:domain` to break direct `:feature → :core:data` dependency for settings reads:

- `PlayerSettings` — exposes player prefs (frame rate, default quality, resume behaviour)
- `UiSettings` — exposes UI prefs (bottom-nav layout, theme)

DataStore-backed implementations bound in `DataModule` via `@Binds`. Feature modules that previously imported `:core:data` for `PlayerPreferences`/`UiPreferences` now depend on the domain interfaces only.

### POLISH-07 — ErrorResult + catch narrowing

- `ConnectionResult` sealed type retired; all connection operations now return `AppResult<ServerInfo>` (consistent with the rest of the codebase).
- All `catch (e: Throwable)` blocks in `:core:data` narrowed to specific exception types (`IOException`, `ApolloException`, etc.). Six `TooGenericExceptionCaught` detekt suppressions removed.

### Anti-pattern updated

The "Importing `:core:data` from a feature module" anti-pattern (see above) is being resolved:
- `feature/player` and `feature/settings` now inject `PlayerSettings` / `UiSettings` domain interfaces instead of the concrete DataStore classes.
- `feature/library` still imports `:core:data` directly for `UiPreferences` — tracked for a future cleanup cycle.
