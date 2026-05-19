# Coding Conventions

**Analysis Date:** 2026-05-16

## Tooling

**Style enforcement:**
- **ktlint** `1.3.1` (Gradle plugin `org.jlleitschuh.gradle.ktlint`) applied to every subproject from the root `build.gradle.kts` (`subprojects { ... }` block, lines 36-49).
  - `android = true` (uses Android-flavoured ruleset)
  - `ignoreFailures = false` — style violations fail the build
  - Excludes `**/build/**` and `**/generated/**` (Apollo-generated GraphQL is exempt)
- **detekt** `1.23.7` applied to every subproject (`build.gradle.kts` lines 51-58).
  - Config: `config/detekt/detekt.yml`
  - `buildUponDefaultConfig = true`, `ignoreFailures = false`
  - Source scanned: `src/main/java`, `src/main/kotlin`

**Run locally:**
```bash
./gradlew ktlintCheck       # style check
./gradlew ktlintFormat      # auto-fix
./gradlew detekt            # static analysis
./gradlew lint              # Android lint
```

**Android Lint:** `app/build.gradle.kts` lines 91-103.
- `abortOnError = true`
- `warningsAsErrors = false` (CI overrides to true)
- `checkReleaseBuilds = true`
- Disabled rules: `MutableCollectionMutableState` (noisy on LazyList items)
- Baseline file: `app/lint-baseline.xml` (declared but absent — no grandfathered issues yet)

**Kotlin global config:** `gradle.properties`
- `kotlin.code.style=official`

## Detekt Tuning (project-specific deviations)

See `config/detekt/detekt.yml`:

| Rule | Override | Why |
|------|----------|-----|
| `complexity.LongMethod.threshold` | 150 | Compose screens routinely exceed 100 lines |
| `complexity.LongParameterList.functionThreshold` | 12 | Compose components with many slots |
| `complexity.LongParameterList.constructorThreshold` | 12 | Same |
| `complexity.TooManyFunctions.thresholdInFile` | 30 | — |
| `complexity.TooManyFunctions.thresholdInClass` | 20 | — |
| `complexity.CyclomaticComplexMethod.threshold` | 20 | — |
| `complexity.LargeClass.threshold` | 800 | — |
| `naming.FunctionNaming` | Ignore `@Composable` | PascalCase for Composables is idiomatic |
| `style.MagicNumber` | Disabled | Noisy in Compose layout code |
| `style.ReturnCount.max` | 5 | — |
| `style.ForbiddenComment` | Active for `FIXME:`, `STOPSHIP:` | `TODO:` is allowed |
| `style.MaxLineLength` | 140 | Excludes package + import statements |
| `potential-bugs.LateinitUsage` | Disabled | Hilt field injection uses `lateinit` |
| `performance.SpreadOperator` | Active | — |

**Implication:** `FIXME` / `STOPSHIP` comments will fail the build. Use `TODO` for known follow-ups.

## Kotlin Compiler Configuration

`build-logic/convention/src/main/kotlin/io/stashapp/android/buildlogic/KotlinAndroid.kt`:
- `jvmTarget = JVM_17`
- `sourceCompatibility = targetCompatibility = JavaVersion.VERSION_17`
- `compileSdk = 35`, `minSdk = 26`
- Free compiler args:
  - `-Xcontext-receivers` (context receivers enabled)
  - `-opt-in=kotlin.RequiresOptIn`
  - `-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi` (project-wide — no per-file opt-in needed)
  - `-Xskip-metadata-version-check` (tolerate nextlib AAR built against newer Kotlin)

## Naming Conventions

**Files:**
- One top-level class per file, filename matches class: `DetailViewModel.kt`, `SceneRepository.kt`
- Compose screen files: `<Feature>Screen.kt` (e.g. `DetailScreen.kt`, `ConnectionScreen.kt`)
- Hilt DI modules live under `<module>/di/` and are named `<Layer>Module.kt` (`DataModule.kt`, `NetworkModule.kt`)
- Mappers: `<Entity>Mapper.kt` (`SceneMapper.kt`, `SceneFilterMapper.kt`)
- Paging sources: `<Entity>PagingSource(s).kt`

**Packages:**
- Root: `io.stashapp.android`
- Layer prefixes: `core.<layer>`, `feature.<feature>`
- Examples: `io.stashapp.android.core.data.scene`, `io.stashapp.android.feature.player`

**Classes & types:**
- `PascalCase` everywhere
- ViewModel state class: `<Feature>UiState` (e.g. `DetailUiState`, `LibraryUiState`)
- Repository interfaces in `core/domain`: `<Entity>Repository` (`SceneRepository`)
- Concrete impls in `core/data`: `Default<Entity>Repository` (`DefaultSceneRepository`)
- Sealed result hierarchies: `AppResult`, `AppError`, `ConnectionResult` — variants are nested `data class` / `data object`

**Functions:**
- `camelCase` for normal functions
- `PascalCase` for `@Composable` functions (per detekt override and Compose convention)
- Boolean properties / getters: `is<Adjective>` (`isActive`, `isMinifyEnabled`)
- Suspend repository methods: action verbs (`scene()`, `scenes()`, `setRating()`, `incrementO()`)

**Properties:**
- `camelCase` instance members
- Backing state: `_state` (private `MutableStateFlow`) exposed as `state` (`StateFlow`) — see `DetailViewModel`, `LibraryViewModel`
- Top-level constants: `UPPER_SNAKE_CASE` OR `camelCase` (detekt `TopLevelPropertyNaming.constantPattern: '[A-Z][_A-Z0-9]*|[a-z][A-Za-z0-9]*'`)
- Companion `const val`: `UPPER_SNAKE_CASE` (`TARGET_PACKAGE` in `StashBaselineProfileGenerator.kt`)

**Enums:**
- Variants `PascalCase` with a constructor carrying display + GraphQL metadata. Example: `SceneSort.DateDesc("Newest first", "date", "DESC")`.

## Import Organization

Observed import order (ktlint enforces):
1. Android / AndroidX (`androidx.*`, `android.*`)
2. Third-party (`com.apollographql.*`, `dagger.*`, `coil3.*`)
3. Project imports (`io.stashapp.android.*`)
4. `kotlinx.*`
5. `kotlin.*` (rare)
6. `javax.inject.*` at the very end

No wildcard imports. No path aliases — full FQN packages.

## Module Structure & Layering

The codebase is a multi-module Now-in-Android-style architecture (see `ARCHITECTURE.md`):

| Layer | Path | Convention |
|-------|------|------------|
| App | `app/` | `stash.android.application` + `stash.android.compose` + `stash.android.hilt` |
| Feature modules | `feature/<name>/` | `stash.android.feature` convention plugin (auto-wires hilt + compose + library) |
| Core | `core/<name>/` | `stash.android.library` (+ `stash.android.compose` for UI modules) |

Feature modules **only** depend on `core:*` modules — never on each other. The `AndroidFeatureConventionPlugin` (`build-logic/convention/src/main/kotlin/io/stashapp/android/buildlogic/AndroidFeatureConventionPlugin.kt`) hard-wires `core:ui`, `core:designsystem`, `core:domain`, `core:model`, `core:common`.

## Error Handling

**Result type (NOT exceptions across layer boundaries):**

`core/common/src/main/java/io/stashapp/android/core/common/Result.kt` defines:
```kotlin
sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
}

sealed class AppError(open val message: String) {
    data class Network(...)
    data class Auth(...)
    data class NotFound(...)
    data class Server(...)
    data class Unknown(..., val cause: Throwable? = null)
}
```

**Repository pattern** (`core/data/src/main/java/io/stashapp/android/core/data/scene/DefaultSceneRepository.kt`):
```kotlin
return try {
    val response = apollo.query(...).execute()
    if (response.hasErrors()) AppResult.Failure(AppError.Server(...))
    else AppResult.Success(...)
} catch (e: Throwable) {
    AppResult.Failure(AppError.Network(e.message ?: "Network error"))
}
```

**Rules:**
- Repository methods return `AppResult<T>` — never throw across the suspend boundary.
- ViewModels `when`-match `AppResult` and translate to `UiState` fields (`loading`, `error: String?`).
- `error("...")` is acceptable for programmer errors (invalid required nav args — see `DetailViewModel.sceneId`).
- The legacy `sealed class ConnectionResult` (`core/model/Connection.kt`) predates `AppResult`; new code should use `AppResult`.

## Sealed Hierarchies

Sealed types are the preferred way to model finite state / result variants:
- `AppResult` / `AppError` (`core/common/Result.kt`) — `sealed interface` + `data class` variants
- `ConnectionResult` (`core/model/Connection.kt`) — `sealed class` legacy
- Enums (with constructor params) for fixed-set domain values: `SceneSort`, `SceneResolution`, `SceneOrientation`, `SceneDurationBucket`, `DateBucket` (all in `core/domain/SceneRepository.kt`)

Prefer `sealed interface` over `sealed class` for new code unless you need shared mutable state.

## Coroutines & Flow

**Imports / scope:**
- `viewModelScope.launch { ... }` for VM-driven work — no manual `CoroutineScope` plumbing.
- `kotlinx.coroutines.ExperimentalCoroutinesApi` is opted-in globally; `@OptIn(ExperimentalCoroutinesApi::class)` on classes that use `flatMapLatest` (e.g. `LibraryViewModel`).

**State exposure:**
- Private mutable: `private val _state = MutableStateFlow(UiState())`
- Public read-only: `val state: StateFlow<UiState> = _state.asStateFlow()`
- Mutation: `_state.update { it.copy(...) }` (never `_state.value = ...` for partial updates)
- Optimistic updates: mutate state immediately, revert in the `Failure` branch (see `DetailViewModel.setRating` / `setOrganized`).

**Combining flows:**
- `queryFlow.flatMapLatest { repo.pagedScenes(it) }.cachedIn(viewModelScope)` for Paging 3 + query-change reactivity (`LibraryViewModel` lines 49-51).
- `first()` for one-shot reads from `DataStore`-backed `Flow`s (`LibraryViewModel` line 58).

**Dispatcher qualifiers** (`core/common/di/Dispatchers.kt`):
- `@IoDispatcher`, `@DefaultDispatcher`, `@MainDispatcher` Hilt qualifiers.
- Use when a class needs an explicit dispatcher; ViewModels rely on `viewModelScope`'s default (Main).

**Ticker pattern** (`feature/player/PlayerViewModel.kt`): long-running periodic jobs are stored in a `Job?` field, cancelled and re-launched on state transitions, and cancelled in `onCleared()`. Example field names: `positionTicker`, `periodicSync`, `bannerJob`.

## Hilt / DI Patterns

**Modules:** Live in `<module>/di/<Layer>Module.kt`.

**Bindings:** Prefer `@Binds` over `@Provides` when binding an interface to its impl:
```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds abstract fun bindSceneRepository(impl: DefaultSceneRepository): SceneRepository
    // ...
}
```

**ViewModels:**
- `@HiltViewModel class XxxViewModel @Inject constructor(...) : ViewModel()`
- Inject `SavedStateHandle` first when used: `savedState: SavedStateHandle, private val repo: ...`
- Resolve required nav args eagerly with `error("sceneId required")` for missing values.

**Singletons:** Repositories are `@Singleton` (`DefaultSceneRepository` has `@Singleton` on the class).

**Compose entry:** Use `viewModel: XxxViewModel = hiltViewModel()` as the default parameter on `Screen` composables.

## Compose Patterns

- `@Composable fun XxxScreen(onBack: () -> Unit, ..., viewModel: XxxViewModel = hiltViewModel())` — callbacks for nav, VM defaulted via Hilt.
- State collection: `val state by viewModel.state.collectAsStateWithLifecycle()` (note: lifecycle-aware variant from `androidx.lifecycle.compose`, not the base `collectAsState`).
- Experimental APIs opted in at the function/file level: `@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)`.
- Theme tokens from `core/designsystem/theme/`: `MaterialTheme.colorScheme.*`, plus a custom `LocalStashColors` `CompositionLocal` for app-specific accents.
- Material 3 only (no Material 2). Icons from `androidx.compose.material.icons.filled.*` / `outlined.*`.

## Data Class Patterns

- All UI state is an immutable `data class` with default values for every field — so `_state.update { it.copy(field = ...) }` works without naming every field.
- Domain query objects (`SceneQuery`, `SceneFilter`) are also `data class` with defaulted fields and computed properties (`SceneFilter.isActive`).
- Companion `fun` factories for parameterised constructors: `SceneFilter.withDurationBucket(bucket)`.

## Comments

**Style:** KDoc (`/** ... */`) for public API on:
- Interfaces in `core/domain` (every method on `SceneRepository` is documented)
- Sealed type members where the variant intent isn't obvious
- ViewModel public methods with non-obvious behaviour (e.g. optimistic update notes)
- `data class` fields where the semantics aren't obvious from the name

**Inline comments:** Explain the *why*, not the *what*. Frequently used for:
- Performance tuning rationale (`prefetchDistance = 40` comment in `DefaultSceneRepository`)
- Hilt / KSP gotchas
- Build/proguard quirks

**Forbidden by detekt:** `FIXME:`, `STOPSHIP:`. `TODO:` is allowed.

## File Size Targets

- Detekt caps `LargeClass` at 800 lines and `LongMethod` at 150 lines.
- Real distribution: most files are 50-300 lines; `PlayerViewModel` is the heaviest VM at ~414 lines.
- 58 Kotlin source files outside `build/` — small, focused modules are the norm.

## Build Conventions Plugins

Centralised in `build-logic/convention/`. **Always apply via the alias rather than direct plugin IDs.**

| Convention | Plugin ID | What it does |
|-----------|-----------|--------------|
| `stash.android.application` | `AndroidApplicationConventionPlugin` | App module setup |
| `stash.android.library` | `AndroidLibraryConventionPlugin` | Library module setup |
| `stash.android.compose` | `AndroidComposeConventionPlugin` | Compose BOM + UI deps |
| `stash.android.hilt` | `AndroidHiltConventionPlugin` | Hilt + KSP |
| `stash.android.feature` | `AndroidFeatureConventionPlugin` | Bundles library+compose+hilt+core deps |

When adding a new feature module, apply only `alias(libs.plugins.stash.android.feature)` — everything else is wired automatically.

## Resource Naming

- App namespace: `io.stashapp.android` (release), `.debug` suffix in debug build (`applicationIdSuffix = ".debug"`).
- `android.nonTransitiveRClass=true` — each module owns its own `R` class; reference resources via the declaring module's `R`.
- `android.defaults.buildfeatures.buildconfig=false` — no `BuildConfig` generated by default; opt-in per module if needed.

## Adding New Code

**New feature screen + VM:**
1. Create `feature/<name>/` with `build.gradle.kts` applying only `alias(libs.plugins.stash.android.feature)`.
2. Add module to `settings.gradle.kts`.
3. ViewModel: `@HiltViewModel class XxxViewModel @Inject constructor(...)` exposing `state: StateFlow<XxxUiState>`.
4. Screen: `XxxScreen(onBack: () -> Unit, ..., viewModel = hiltViewModel())`.
5. Wire into nav graph (`core/ui/nav/Routes.kt`).
6. Add `implementation(project(":feature:<name>"))` to `app/build.gradle.kts`.

**New repository:**
1. Interface in `core/domain` (`XxxRepository`), suspend methods returning `AppResult<T>` or `Flow<T>`.
2. Impl in `core/data/<entity>/DefaultXxxRepository.kt` annotated `@Singleton`.
3. `@Binds` wiring in `core/data/.../di/DataModule.kt`.

---

*Convention analysis: 2026-05-16*
