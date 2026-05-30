# Architecture Research

**Domain:** Modern Android app (Kotlin/Compose, multi-module, GraphQL client)
**Researched:** 2026-05-16
**Confidence:** HIGH (Google's official architecture guide, Nav3 stable docs, Compose stability docs cross-verified)

> **Scope contract:** Slopper's module graph (`app/`, `core/*`, `feature/*`, `build-logic/`, `baselineprofile/`) is **frozen** per `PROJECT.md`. This document maps Google's current (2026) recommended Android architecture to **within-module** modernization changes Slopper should adopt. Module boundaries do not move.

---

## Standard Architecture (Google, 2026)

Google's "Guide to App Architecture" (developer.android.com/topic/architecture) prescribes three layers — **UI**, **Domain (optional)**, **Data** — with strict unidirectional data flow. Slopper already implements this; the modernization deltas are within each layer.

```
┌──────────────────────────────────────────────────────────────┐
│                          UI LAYER                            │
│   ┌─────────────────────┐    ┌──────────────────────────┐    │
│   │ @Composable Screen  │◄───┤ StateFlow<ImmutableUi>   │    │
│   │  (stateless)        │    │  (ViewModel-owned)       │    │
│   └────────┬────────────┘    └────────────┬─────────────┘    │
│            │ events (lambdas)              ▲                 │
│            ▼                                │                 │
│   ┌─────────────────────────────────────────┴────────┐       │
│   │   ViewModel (StateFlow + onAction(Event))        │       │
│   │   Nav3: NavBackStack ownership lifts here        │       │
│   └─────────────────────┬────────────────────────────┘       │
└─────────────────────────┼────────────────────────────────────┘
                          │ calls suspend fun / Flow
                          ▼
┌──────────────────────────────────────────────────────────────┐
│                     DOMAIN LAYER (optional)                  │
│   UseCase / Interactor — only when logic is reused or        │
│   crosses multiple repositories. Pure Kotlin, main-safe.     │
└─────────────────────────┬────────────────────────────────────┘
                          ▼
┌──────────────────────────────────────────────────────────────┐
│                        DATA LAYER                            │
│   Repository (interface in :core:domain, impl in :core:data) │
│   Returns Result<T> / Flow<T>. Owns conflict resolution,     │
│   caching, offline policy. Single source of truth per area.  │
│                                                              │
│   DataSource(s): network (Apollo), local (DataStore/Room)    │
└──────────────────────────────────────────────────────────────┘
```

### Component Responsibilities (mapped to Slopper)

| Component | Google's responsibility (2026) | Slopper module(s) affected |
|-----------|--------------------------------|----------------------------|
| Compose screen | Stateless, hoists state, accepts `UiState` + lambdas | `feature/*` |
| ViewModel | Owns `StateFlow<UiState>`, exposes `onAction(Event)`, holds `NavBackStack` (Nav3) | `feature/*` |
| UseCase | Reusable / cross-repo business logic, main-safe | `core/domain` (currently absent — add only when needed) |
| Repository interface | Domain contract, `AppResult`/`Flow` returns | `core/domain` |
| Repository impl | Coordinates data sources, applies policies | `core/data` |
| DataSource | Single source (network / local) | `core/network`, `core/data/prefs` |
| DI graph | `@Binds` interface→impl in `SingletonComponent`; `@ViewModelScoped` for VM-scoped state | `core/data/di`, `core/network/di` |
| Convention plugins | One plugin per module archetype | `build-logic/convention` |
| Baseline profile / Macrobench | `com.android.test` module driving CUJ-based profile generation | `baselineprofile/` |

---

## Within-Module Modernization Deltas

### 1. UI Layer — Compose-first patterns

**Current Google guidance (HIGH confidence):**
- **Strong Skipping Mode** is enabled by default in Compose Compiler 1.5.4+; all restartable composables are skippable. Unstable params compared by `===`. Hand-rolled `@Stable`/`@Immutable` annotations and `compose_compiler_config.conf` stability files are mostly obsolete — `kotlinx.collections.immutable` is the recommended escape hatch for `List`/`Map` parameters. Source: developer.android.com/develop/ui/compose/performance/stability/strongskipping (2026-01).
- **UDF + immutable `UiState`:** ViewModel owns `MutableStateFlow<UiState>`, exposes `StateFlow<UiState>`, UI collects via `collectAsStateWithLifecycle()`. Single state class per screen.
- **`collectAsStateWithLifecycle()`** is the only correct collector for screen state (lifecycle-aware; pauses in STOPPED).
- **Navigation 3 (`androidx.navigation3`) is stable** (1.0, late 2025) — type-safe via `@Serializable` route classes, `NavBackStack` lives in caller code (often hoisted into a ViewModel), supports adaptive multi-pane via `Scene`/`SceneStrategy`. Source: developer.android.com/guide/navigation/navigation-3.
- **Predictive back** is fully supported via `BackHandler` + `PredictiveBackHandler` (Compose 1.7+).
- **Edge-to-edge is mandatory** when targeting Android 15+ (SDK 35).

**Slopper module impact:**
| Change | Module |
|---|---|
| Adopt `collectAsStateWithLifecycle()` everywhere (already partly done — audit `core/ui`, all `feature/*`) | `feature/*`, `core/ui` |
| Replace any `List<T>` in `UiState` with `ImmutableList<T>` (kotlinx.collections.immutable) | `feature/*` (`*UiState.kt` files) |
| Migrate `androidx.navigation:navigation-compose` → `androidx.navigation3` if upgrading nav; otherwise keep Nav2 and document why. Type-safe routes (`@Serializable` data classes) instead of `Routes` string registry | `core/ui/nav/Routes.kt`, `app/MainActivity.kt`, every `feature/*` (call sites) |
| Predictive back handlers on player + detail screens | `feature/player`, `feature/detail` |
| Confirm edge-to-edge + `WindowInsets` handling (already in `MainActivity`) survives upgrade | `app/MainActivity.kt`, `feature/player` (player UI insets) |

**Nav3 caveat (MEDIUM confidence):** Migration is non-trivial — Nav3 lifts back-stack ownership to caller code, which is a real refactor of `MainActivity.kt`'s `NavHost`. The `Routes` central registry pattern in `core/ui/nav/Routes.kt` maps cleanly to a sealed hierarchy of `@Serializable` route classes, but every `composable(Routes.X)` call site changes. Recommend treating Nav3 as **opt-in within this milestone** — adopt only if remaining Nav2 issues (deep links, tab state loss noted in `ARCHITECTURE.md:232`) justify the churn.

---

### 2. Domain Layer — when to keep it, use-case patterns

**Current Google guidance (HIGH confidence):**
- Domain layer is **optional**. Add a `UseCase` only when (a) logic is reused across ViewModels, (b) logic combines multiple repositories, or (c) ViewModels are becoming complex. Source: developer.android.com/topic/architecture/domain-layer.
- Use cases must be **main-safe** (move blocking work to appropriate dispatcher).
- Single responsibility per use case; named `<Verb><Noun>UseCase`; expose `operator fun invoke(...)`.
- Use cases must **not hold mutable state** — state lives in UI or data layers.

**Slopper module impact:**
| Change | Module |
|---|---|
| **Do not retroactively introduce use cases** — Slopper currently has ViewModels calling repos directly, which the official guide explicitly endorses when there's no reuse. Add use cases only where `HomeViewModel`'s rail-fanout logic or `PlayerViewModel`'s queue+codec logic crosses multiple repos | `core/domain` (new `usecase/` subpackage if added) |
| Audit `PlayerViewModel.kt` (~414 lines per `CONVENTIONS.md`) — candidate for extracting `BuildPlayerSourceUseCase` or `ResolveCodecUseCase` if logic is reused by `feature/settings` | `core/domain`, `feature/player`, `feature/settings` |

---

### 3. Data Layer — repository, error modeling, offline-first

**Current Google guidance (HIGH confidence):**
- Repositories are **the only entry points** to the data layer. UI/Domain never touch DataSources directly. Source: developer.android.com/topic/architecture/data-layer.
- One repository per **data type/feature**, not per data source.
- **Error modeling:** Google's samples (Now-in-Android) use either Kotlin's `Result<T>` or a custom sealed `Result`/`AppResult` with typed errors. Slopper's `AppResult` + `AppError` already matches this pattern — keep it.
- **Offline-first:** Single Source of Truth = local store; network refreshes local; UI observes local `Flow`. Use `NetworkBoundResource`-style or Room+Apollo cache + Paging 3 `RemoteMediator` for paged data.
- **Coroutines best practice:** repositories are `suspend`/`Flow`; do **not** create their own scopes — they receive `viewModelScope` via the caller. Use `withContext(ioDispatcher)` only when work is CPU/IO bound; trust structured concurrency.

**Slopper module impact:**
| Change | Module |
|---|---|
| Wire the unused dispatcher qualifiers (`@IoDispatcher`, `@DefaultDispatcher`) — add a `DispatchersModule` and apply `withContext(io)` in `Default*Repository` where Apollo's dispatcher isn't sufficient (file mappers, JSON serialization) | `core/common/di`, `core/data/*` |
| Replace `feature/library`/`feature/player`/`feature/settings` direct imports of `:core:data` `UiPreferences`/`PlayerPreferences` with `:core:domain` interfaces (e.g. `PlayerSettings`, `UiSettings`) + `@Binds` in `DataModule` (already flagged as anti-pattern in `ARCHITECTURE.md:243`) | `core/domain`, `core/data/prefs`, `feature/library`, `feature/player`, `feature/settings` |
| Migrate legacy `ConnectionResult` (`core/model/Connection.kt`) to `AppResult` so the codebase has **one** result type | `core/model`, `core/data/connection`, `feature/connection` |
| Consider Apollo's normalized cache + `watch()` for offline-first scene/browse data — currently Slopper goes network-on-every-query. Low-risk win because Apollo handles it; gate behind a perf measurement | `core/network` (Apollo config), `core/data/scene`, `core/data/browse` |
| Apollo client built once with placeholder URL + per-request rewrite (`NetworkModule.kt:52`) — keep, but document that this prevents normalized-cache key-collisions across servers (cache key must include endpoint) if cache is enabled | `core/network/di/NetworkModule.kt` |

---

### 4. DI — Hilt module conventions, scoping, multi-module wiring

**Current Google guidance (HIGH confidence):**
- Prefer `@Binds` over `@Provides` for interface→impl (more efficient — generates fewer factories). Source: developer.android.com/training/dependency-injection/hilt-android.
- **Scoping ladder:** `@Singleton` (whole app) → `@ActivityRetainedScoped` (survives config) → `@ViewModelScoped` (ViewModel lifetime) → `@ActivityScoped` → `@FragmentScoped`. Use the narrowest scope that works.
- `@ViewModelScoped` is the right choice for state that's per-screen but shared across collaborators injected into a single VM.
- Multi-module: Hilt requires the **application module** to see all `@Module`-annotated modules transitively. Feature modules can declare `@Module @InstallIn(SingletonComponent::class)` freely; they're picked up via Gradle's transitive graph. Source: developer.android.com/training/dependency-injection/hilt-multi-module.
- **Inversion caveat:** "Dynamic feature modules" (DFM) reverse the dependency direction and require manual Dagger entry points. Slopper does **not** use DFM, so this doesn't apply.

**Slopper module impact:**
| Change | Module |
|---|---|
| Audit `DataModule.kt` and `NetworkModule.kt` — confirm all bindings are `@Binds` (already true per `CONVENTIONS.md:202`). Keep `SingletonComponent` as default | `core/data/di`, `core/network/di` |
| Introduce `@ViewModelScoped` only if a screen has multiple injected collaborators that need to share state (e.g. `PlayerViewModel` + a hypothetical `PlayerSourceFactory`) | `feature/player` |
| Hilt module conventions are codified in `AndroidHiltConventionPlugin` — confirm KSP version pinned to current stable Hilt (2.52+) | `build-logic/convention/AndroidHiltConventionPlugin.kt`, `gradle/libs.versions.toml` |

---

### 5. Async — coroutines / StateFlow / SharedFlow / channels

**Current best practice (HIGH confidence, cross-verified with Google's coroutines guidance + JetBrains docs):**
- **`StateFlow`** for screen UI state (always has a value, conflates, lifecycle-friendly).
- **`SharedFlow`** for one-shot events (snackbars, nav events) — `replay = 0`, `extraBufferCapacity = 1`, `onBufferOverflow = DROP_OLDEST`. **But** the increasingly recommended alternative for one-shot events is to **fold them into `UiState`** (e.g. `UiState.snackbar: SnackbarMessage?` cleared via `onAction(SnackbarShown)`) — simpler and survives config changes.
- **Channels** only for hot producer↔single-consumer pipelines that aren't UI state. Rare in app code.
- **`viewModelScope`** is the default; never `GlobalScope`, never hand-rolled `CoroutineScope` unless you can tie its cancellation to something.
- `Dispatchers.Main.immediate` is the default for `viewModelScope`; offload only when work is genuinely blocking.
- `flatMapLatest` for query-driven Paging (Slopper already does this in `LibraryViewModel`).
- `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)` is the canonical conversion of cold flow→`StateFlow` (5s grace lets config change complete).

**Slopper module impact:**
| Change | Module |
|---|---|
| Replace any `Channel`-based event buses with `UiState`-folded events or `SharedFlow(0, 1, DROP_OLDEST)` | `feature/*` (audit ViewModels) |
| Standardize on `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Loading)` where ViewModels currently use eager `MutableStateFlow` + manual `update` | `feature/*` |
| Move `PlayerViewModel`'s `positionTicker` / `periodicSync` ticker pattern (per `CONVENTIONS.md:196`) to `flow { while(isActive) { emit(...); delay(...) } }.stateIn(...)` — cleaner cancellation, no manual `Job?` plumbing | `feature/player/PlayerViewModel.kt` |

---

### 6. Multi-module conventions — `build-logic/` + version catalog

**Current Google guidance (HIGH confidence):**
- Now-in-Android's `build-logic/convention` composite build is the canonical pattern (Slopper already follows it). Source: developer.android.com/topic/modularization/patterns + github.com/android/nowinandroid.
- Convention plugin per archetype: `application`, `library`, `compose`, `hilt`, `feature`, `test`. Slopper already has all five.
- **Version catalog** (`gradle/libs.versions.toml`) is the single source of truth — `[versions]`, `[libraries]`, `[plugins]`, `[bundles]`. Slopper already uses this.
- **`api()` vs `implementation()`:** prefer `implementation()` to keep build graphs small; use `api()` only when the dependency's types appear in your module's public API (Slopper's `:core:data` uses `api(project(":core:domain"))` because `AppResult`/repo interfaces leak through — correct).

**Slopper module impact:**
| Change | Module |
|---|---|
| Audit `build-logic/convention/*.kt` plugins — bump targets (`compileSdk=36` for Android 16 / API 36, keep `minSdk=26` per constraint), align Kotlin/AGP/Compose BOM/Hilt/KSP versions | `build-logic/convention/KotlinAndroid.kt`, `gradle/libs.versions.toml` |
| Consider a `stash.android.test` convention plugin to standardize the unit-test setup (JUnit 5, Turbine, MockK/Mockito-kotlin, Truth) when tests are introduced | `build-logic/convention` |
| Add `kotlin.collections.immutable` to version catalog + Compose convention so feature modules can use `ImmutableList` for UiState stability | `gradle/libs.versions.toml`, `build-logic/convention/AndroidComposeConventionPlugin.kt` |

---

### 7. Build performance — config cache, K2, parallelism, isolated projects

**Current Google + Gradle + JetBrains guidance (HIGH confidence):**
- **Kotlin K2 compiler is default since Kotlin 2.0** (stable JVM). Slopper inherits this on Kotlin 2.x. Source: kotlinlang.org/docs/k2-compiler-migration-guide.html.
- **Configuration cache is stable**; enable with `org.gradle.configuration-cache=true`. KSP 2 + Hilt 2.52+ + Apollo 4 + AGP 8.7+ all support it. OWASP `dependency-check` does **not** (Slopper already works around this via `--no-configuration-cache` per `ARCHITECTURE.md:222`).
- **Project isolation** (incubating in Gradle 8.x, stabilizing through 2026): `org.gradle.unsafe.isolated-projects=true`. KSP enables `ksp.project.isolation` by default when isolated-projects is on. Strong parallelism win but requires every plugin to be project-isolation-safe — check each plugin's release notes before enabling project-wide.
- **Parallel builds** + **build cache** + **`org.gradle.workers.max`** tuning.
- **AGP 8.7+** required for some current Compose + K2 features; AGP 8.9+ for latest profile-installer integrations.

**Slopper module impact:**
| Change | Module |
|---|---|
| Enable `org.gradle.configuration-cache=true` in `gradle.properties` once OWASP carve-out is confirmed (already documented) | `gradle.properties`, root `build.gradle.kts` |
| Validate KSP/Hilt/Apollo/AGP versions against current stable matrix; bump in version catalog | `gradle/libs.versions.toml` |
| Trial `org.gradle.unsafe.isolated-projects=true` on a branch — flag any plugin that breaks (ktlint, detekt, OWASP) | `gradle.properties` |
| Confirm `KotlinAndroid.kt` does not pass deprecated compiler args under K2 (e.g. `-Xskip-metadata-version-check` may no longer be needed if nextlib upgrades) | `build-logic/convention/KotlinAndroid.kt` |
| Add `--scan` or `develocity` build scans to CI to track regression | root `build.gradle.kts`, CI config |

---

### 8. Baseline Profiles + Macrobenchmark wiring

**Current Google guidance (HIGH confidence):**
- Macrobenchmark module is a **`com.android.test`** module separate from app. Slopper has this (`baselineprofile/`).
- Required versions (early 2026): `androidx.benchmark:benchmark-macro-junit4:1.4.x`, `androidx.profileinstaller:profileinstaller:1.4.x`, AGP 8.0+. Source: developer.android.com/topic/performance/baselineprofiles.
- Generation: `./gradlew :app:generateBaselineProfile` — drives `:baselineprofile` against the `benchmark` build type (release-like but profileable + debuggable=false + minified).
- **Critical User Journeys (CUJs):** cover startup + every screen reachable in <5 taps. For Slopper: cold start → Home rails scroll → Library scroll/filter → Detail open → Player start.
- Macrobenchmark for **regression detection** (separate from baseline-profile generation) — `@MacrobenchmarkRule` with `CompilationMode.Partial(BaselineProfileMode.Require)`.

**Slopper module impact:**
| Change | Module |
|---|---|
| Audit `baselineprofile/src/main/` — confirm CUJs cover Home, Library scroll+filter, Detail, Player cold-start | `baselineprofile/` |
| Add scroll-jank Macrobenchmark for Library (paged grid is the most jank-prone surface) | `baselineprofile/` |
| Ensure `:app`'s `benchmark` build type stays in sync with release (R8 on, baseline profile installed) | `app/build.gradle.kts` |
| Bump `benchmark-macro-junit4` + `profileinstaller` to current stable; remove deprecated `ProfileVerifier` calls if any | `gradle/libs.versions.toml`, `baselineprofile/` |

---

### 9. Testing pyramid

**Current Google guidance (HIGH confidence):**

| Tier | Tool | Scope | Slopper module |
|---|---|---|---|
| Unit (JVM) | JUnit 5 + Turbine + MockK | Pure Kotlin: mappers, `AppResult` paths, `SceneFilter` logic, ViewModel state transitions (with `kotlinx-coroutines-test` `runTest`) | `core/common`, `core/model`, `core/domain`, `core/data` (with fakes), `feature/*` (ViewModel tests) |
| Robolectric | `org.robolectric:robolectric` | Android-dependent unit tests (DataStore, EncryptedSharedPreferences round-trip) without an emulator | `core/data/prefs` |
| Compose UI | `androidx.compose.ui:ui-test-junit4` + Hilt test rules | Screen-level behavior with fake repos via Hilt test modules | `feature/*` |
| Instrumented benchmark | `androidx.benchmark:benchmark-macro-junit4` | Real device, real APK — startup, scroll, frame timing | `baselineprofile/` |

Slopper currently has **no `src/test/` or `src/androidTest/`** anywhere except `:baselineprofile` (per `STRUCTURE.md:181`). This is the **biggest gap** versus modern guidance.

**Slopper module impact:**
| Change | Module |
|---|---|
| Bootstrap unit tests in `core/common`, `core/model`, `core/domain` first (no Android deps — cheapest) | `core/common`, `core/model`, `core/domain` |
| ViewModel tests with Turbine + fake repos (define fake-repo flavor in `:core:data` testFixtures or sibling `:core:testing` module *— but `:core:testing` would be a new module, out of scope*; use in-module `@TestInstallIn` Hilt modules instead) | `feature/*`, `core/data` (testFixtures) |
| Compose UI smoke test per feature (at minimum: "screen renders empty state without crashing") | `feature/*` |
| Wire JUnit 5 + Turbine + Robolectric via `stash.android.library` plugin defaults (not a new module) | `build-logic/convention/AndroidLibraryConventionPlugin.kt` |

---

## Architectural Patterns

### Pattern 1: Immutable `UiState` data class

**What:** One `data class XxxUiState(...)` per screen with default values; ViewModel exposes `StateFlow<UiState>`; UI reads with `collectAsStateWithLifecycle()`.
**When to use:** Every screen.
**Trade-offs:** Cheap recomposition only if `UiState` is fully stable — collections must be `ImmutableList`/`ImmutableMap`, no `var` fields. Slopper already follows this pattern; the delta is adopting `kotlinx.collections.immutable`.

```kotlin
data class LibraryUiState(
    val items: ImmutableList<SceneSummary> = persistentListOf(),
    val filter: SceneFilter = SceneFilter(),
    val isLoading: Boolean = false,
    val error: String? = null,
)
```

### Pattern 2: Repository returns `AppResult<T>` / `Flow<T>`

**What:** Suspend functions return typed `AppResult<T>`; streams return `Flow<T>` (or `Flow<PagingData<T>>` for paged). No exceptions cross the boundary.
**When to use:** All data-layer entry points.
**Trade-offs:** Forces ViewModels to handle every error case explicitly. Slopper does this already.

### Pattern 3: Hilt `@Binds` interface→impl in `SingletonComponent`

**What:** `abstract class DataModule { @Binds abstract fun bindX(impl: DefaultX): X }`. Repositories `@Singleton`.
**When to use:** Default for repositories and app-wide services.
**Trade-offs:** `@Singleton` leaks state across nav graph if misused — keep repository state minimal.

### Pattern 4: Convention plugin per module archetype

**What:** `build-logic/convention/` registers `stash.android.{application,library,compose,hilt,feature}` plugins; module `build.gradle.kts` is ~10 lines.
**When to use:** Always in multi-module projects ≥3 modules.
**Trade-offs:** New module archetype = new plugin (small cost, big payoff).

### Pattern 5: Single Activity + NavHost (Nav2 today, Nav3 candidate)

**What:** One `MainActivity` hosts a `NavHost`; each `composable(route)` is a screen.
**When to use:** Standard Android pattern since Compose Nav.
**Trade-offs:** Deep linking requires explicit work; tab state retention needs `launchSingleTop` + `popUpTo` discipline (Slopper deliberately opts out, see `ARCHITECTURE.md:232`).

---

## Data Flow

### Primary Request Flow (already correct in Slopper)

```
User taps → Composable emits Event (lambda)
         → ViewModel.onAction(Event)
         → viewModelScope.launch { repo.suspendCall() }
         → Repository.impl → DataSource (Apollo)
         → AppResult<T> back
         → _state.update { it.copy(...) }
         → StateFlow re-emits → Compose recomposes
```

### State Management Flow

```
DataStore prefs ──Flow──► Repository ──Flow──► ViewModel.stateIn(...) ──StateFlow──► Compose
                                                       ▲
                                              user actions fold here
```

---

## Build / Dependency Order (drives roadmap phases)

The downstream roadmap should follow this dependency order — **each phase must keep the build green**:

1. **Phase A — Dependency floor + build infra** (touches: `build-logic/convention`, `gradle/libs.versions.toml`, `gradle.properties`)
   - Bump AGP, Kotlin, KSP, Hilt, Compose BOM, Apollo to current stable.
   - Enable `org.gradle.configuration-cache=true` (keep OWASP carve-out).
   - K2 verification (likely no-op if already on Kotlin 2.x).
   - **Why first:** every later phase depends on these.

2. **Phase B — Guideline migrations within existing modules** (touches: `feature/*`, `core/ui`, `core/domain`, `core/data`)
   - `collectAsStateWithLifecycle()` everywhere.
   - `ImmutableList` in `UiState`.
   - `stateIn(WhileSubscribed(5_000))` standardization.
   - Resolve `:feature → :core:data` direct-imports anti-pattern (introduce `PlayerSettings`/`UiSettings` interfaces in `:core:domain`).
   - Retire `ConnectionResult` → `AppResult`.
   - Wire `DispatchersModule`, apply `withContext(io)` where Apollo isn't already off-main.
   - **Optional within Phase B:** Nav2 → Nav3 migration (treat as an independent sub-phase; high churn).
   - **Why second:** uses Phase A's deps but does not yet touch perf/measurement.

3. **Phase C — Performance + measurement** (touches: `baselineprofile/`, `app/`)
   - Refresh baseline profile CUJs (Home, Library scroll, Detail, Player start).
   - Add Macrobenchmark regression tests (startup + Library scroll-jank).
   - Apollo normalized cache + `watch()` for offline-first scenes (gated on benchmark delta).
   - Record before/after baseline-profile + macrobench deltas to satisfy `PROJECT.md` perf-claim rule.
   - **Why third:** needs guideline migrations done so measurements reflect the new baseline.

4. **Phase D — Test pyramid bootstrap** (touches: `build-logic/convention/AndroidLibraryConventionPlugin.kt`, `core/*`, `feature/*`)
   - Wire JUnit5/Turbine/Robolectric into library convention plugin.
   - Unit tests for `core/common`, `core/model`, `core/domain` first.
   - ViewModel tests with fake repos.
   - Compose UI smoke tests per feature.
   - **Why last:** tests guard against future regressions but aren't strictly required for the modernization to ship; can run in parallel with Phase C if capacity allows.

5. **Phase E — Polish** (touches: docs, CI, lint baseline)
   - Refresh `README.md`, `DEVICE_TESTING.md`, `build-logic/` plugin comments.
   - Re-baseline lint after upgrades; ensure `warningsAsErrors=true` in CI passes.
   - Clean any new detekt findings.

---

## Anti-Patterns (domain-specific, beyond those already in codebase ARCHITECTURE.md)

### Anti-Pattern 1: Hand-rolled stability annotations after Strong Skipping

**What people do:** Add `@Immutable`/`@Stable` to every data class "just in case."
**Why it's wrong:** Strong Skipping makes all restartable composables skippable regardless; the annotation is noise. Real problem is `List`/`Map` parameters — fix with `ImmutableList`.
**Do this instead:** Use `kotlinx.collections.immutable`. Reserve `@Immutable` for cases where the compiler genuinely cannot infer (rare).

### Anti-Pattern 2: `MutableSharedFlow` for UI events when state-folded works

**What people do:** Emit `Snackbar`/`Navigate` events through a `SharedFlow` collected in the screen.
**Why it's wrong:** Events lost on config change; complicates testing.
**Do this instead:** Fold events into `UiState` (`val snackbar: SnackbarMessage? = null`); UI clears via `onAction(SnackbarShown)`.

### Anti-Pattern 3: Introducing a use case per repository call

**What people do:** `GetScenesUseCase`, `GetSceneByIdUseCase` for every repo method.
**Why it's wrong:** Adds indirection without reuse benefit. Google's domain-layer guide explicitly says use cases are optional and should exist only for reuse / cross-repo logic.
**Do this instead:** ViewModel calls repository directly. Add use case when, and only when, the second consumer arrives.

### Anti-Pattern 4: `GlobalScope.launch` from a ViewModel

**What people do:** Fire-and-forget work in `GlobalScope` to "outlive the screen."
**Why it's wrong:** No cancellation; leaks; impossible to test.
**Do this instead:** If work must survive the screen, expose a `@Singleton` worker via Hilt that owns its own `CoroutineScope(SupervisorJob + Dispatchers.IO)`; tie cancellation to a lifecycle you control.

### Anti-Pattern 5: Putting Compose stability `compose_compiler_config.conf` in `build-logic/` without measurement

**What people do:** Copy-paste a long stability config from a blog post.
**Why it's wrong:** Strong Skipping made most entries obsolete; stale config can mask real stability problems.
**Do this instead:** Trust Strong Skipping; only add a stability config entry after seeing the offending class flagged by Compose compiler metrics.

---

## Integration Points (Slopper-specific)

### External Services

| Service | Integration pattern | Notes |
|---|---|---|
| Stash GraphQL server | Apollo Kotlin 4 + OkHttp interceptor | Endpoint rewritten per-request via `StashEndpointProvider` — cache key must include endpoint if normalized cache is enabled |
| EncryptedSharedPreferences | `androidx.security:security-crypto` | Library is in maintenance mode; verify it still receives security updates or plan migration to AndroidX Crypto Tink (HIGH-confidence guidance pending Crypto Tink stable release) |

### Internal Boundaries

| Boundary | Communication | Notes |
|---|---|---|
| `feature/*` ↔ `core/domain` | Repository interfaces + `AppResult` | Correct — keep |
| `feature/library`, `feature/player`, `feature/settings` ↔ `core/data` (direct prefs imports) | Direct class import | **Anti-pattern** — fix by exposing `:core:domain` interfaces |
| `core/data` ↔ `core/network` | `api()` dep + `StashEndpointProvider` interface | Correct — keep, broken-cycle pattern documented |
| `feature/settings` ↔ `feature/player` (`CodecCapabilities`) | Direct module dep | **Anti-pattern** — extract to `:core:player-capabilities` is **out of scope** (module-graph change); for this milestone, copy the file into `core/common` or duplicate-and-leave-comment, with extraction deferred to a future milestone |

---

## Sources

- [Guide to app architecture](https://developer.android.com/topic/architecture) — Google, current
- [UI layer](https://developer.android.com/topic/architecture/ui-layer) — Google, current
- [Domain layer](https://developer.android.com/topic/architecture/domain-layer) — Google, current
- [Data layer](https://developer.android.com/topic/architecture/data-layer) — Google, current
- [Navigation 3](https://developer.android.com/guide/navigation/navigation-3) — Google, stable 1.0 (late 2025)
- [Migrate from Navigation 2 to Navigation 3](https://developer.android.com/guide/navigation/navigation-3/migration-guide) — Google
- [Strong Skipping Mode](https://developer.android.com/develop/ui/compose/performance/stability/strongskipping) — Google, updated 2026-01-16
- [Fix Compose stability issues](https://developer.android.com/develop/ui/compose/performance/stability/fix) — Google
- [Dependency injection with Hilt](https://developer.android.com/training/dependency-injection/hilt-android) — Google
- [Hilt in multi-module apps](https://developer.android.com/training/dependency-injection/hilt-multi-module) — Google
- [Common modularization patterns](https://developer.android.com/topic/modularization/patterns) — Google
- [Baseline Profiles overview](https://developer.android.com/topic/performance/baselineprofiles/overview) — Google
- [Write a Macrobenchmark](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview) — Google
- [Create Baseline Profiles](https://developer.android.com/topic/performance/baselineprofiles/create-baselineprofile) — Google
- [K2 compiler migration guide](https://kotlinlang.org/docs/k2-compiler-migration-guide.html) — JetBrains
- [Kotlin Gradle compilation and caches](https://kotlinlang.org/docs/gradle-compilation-and-caches.html) — JetBrains
- [Now in Android sample](https://github.com/android/nowinandroid) — Google's reference multi-module app
- [Navigation Compose 3 Is Finally Stable](https://proandroiddev.com/navigation-compose-3-is-finally-stable-heres-why-it-matters-for-android-developers-6f2b59e4f022) — ProAndroidDev, Dec 2025 (corroborating Nav3 stable)

---

*Architecture research for: Slopper Android app modernization (2026)*
*Researched: 2026-05-16*
