<!-- generated-by: gsd-doc-writer -->
# Architecture

Slopper (Gradle root project `StashAndroid`) is a single-activity Jetpack Compose
client for a self-hosted [Stash](https://github.com/stashapp/stash) GraphQL server.
The codebase follows the Now-in-Android module layout: Gradle convention plugins
in `build-logic/`, layered `:core:*` and `:feature:*` modules, and a thin `:app`
shell that owns the navigation graph.

This document is the contributor map. Start here, then drill into the module you
care about.

## High-level module graph

```
                 ┌─────────────────────────────┐
                 │            :app             │
                 │  MainActivity + AppNavHost  │
                 │  RootViewModel (start dest) │
                 └──────────┬──────────────────┘
                            │ depends on every feature
              ┌─────────────┴─────────────┬──────────────┬──────────────┐
              ▼                           ▼              ▼              ▼
   ┌──────────────────┐         ┌──────────────────┐  ┌──────────┐  ┌──────────┐
   │ :feature:home    │  ...    │ :feature:player  │  │ :feature │  │ :feature │
   │ :feature:library │         │ :feature:detail  │  │ :browse  │  │ :settings│
   │ :feature:connection                                                       │
   └──────────┬───────┘         └────────┬─────────┘  └────┬─────┘  └────┬─────┘
              │ via stash.android.feature plugin                          │
              └─────────────────────┬─────────────────────────────────────┘
                                    ▼
                  ┌──────────────────────────────────────┐
                  │  :core:ui    │  :core:designsystem   │
                  │  (Routes, BottomNav, image loader)   │
                  └──────────┬───────────────────────────┘
                             ▼
                  ┌──────────────────────────────────────┐
                  │            :core:domain              │
                  │  Repository interfaces, query types  │
                  └──────────┬───────────────────────────┘
                             │ Hilt @Binds in :core:data
                             ▼
                  ┌──────────────────────────────────────┐
                  │            :core:data                │
                  │  Default*Repository, paging, prefs   │
                  └──────────┬───────────────────────────┘
                             ▼
                  ┌──────────────────────────────────────┐
                  │           :core:network              │
                  │   Apollo + OkHttp + auth interceptor │
                  └──────────┬───────────────────────────┘
                             ▼
                  ┌──────────────────────────────────────┐
                  │       Stash GraphQL server (LAN)     │
                  └──────────────────────────────────────┘

Cross-cutting (depended on by most layers):
  :core:model    — Plain Kotlin domain types
  :core:common   — AppResult, AppError, dispatcher qualifiers
```

The composition root is `:app`. The terminal node (depended on by nothing) is
also `:app`. Everything else fans out from `:core:common` and `:core:model`.

## Module catalog

The canonical module list lives in `settings.gradle.kts`.

| Module | Role | Key public types | Depends on (project) |
|---|---|---|---|
| `:app` | Single-activity host, root `NavHost`, `RootViewModel`, signing/build types, ABI splits, baseline-profile consumer | `MainActivity`, `StashApp`, `RootViewModel`, `AppNavHost` | every `:core:*` + every `:feature:*` |
| `:core:common` | Result/error taxonomy and coroutine dispatcher qualifiers | `AppResult`, `AppError`, `@IoDispatcher`, `@DefaultDispatcher`, `@MainDispatcher` | — |
| `:core:model` | Plain Kotlin domain entities, `@Serializable` where needed | `Scene`, `SceneSummary`, `SceneDetail`, `Connection`, `Queue`, `BrowseEntity` | — |
| `:core:domain` | Repository **interfaces** and immutable query/filter/sort types — no Android, no Apollo | `SceneRepository`, `ConnectionRepository`, `BrowseRepository`, `SceneQuery`, `SceneFilter`, `SceneSort` | `:core:model`, `:core:common` |
| `:core:network` | Apollo 4 client, OkHttp stack, `StashAuthInterceptor`, endpoint provider interface, Apollo-generated GraphQL code under `io.stashapp.android.graphql` | `StashEndpoint`, `StashEndpointProvider`, `NetworkModule` | `:core:common` |
| `:core:data` | Repository **implementations**, paging sources, mappers, DataStore preference stores, Hilt `@Binds` module | `DefaultSceneRepository`, `DefaultConnectionRepository`, `DefaultBrowseRepository`, `EndpointStateHolder`, `ConnectionStore`, `UiPreferences`, `PlayerPreferences`, `DataModule` | `:core:domain` (api), `:core:network` (api) |
| `:core:designsystem` | Material3 theme, color, typography, reusable design-system primitives | `StashTheme`, `SceneCard` | — |
| `:core:ui` | Shared Compose UI utilities: route registry, bottom-nav chrome, sheet UI, Coil image loader factory | `Routes`, `MainBottomBar`, `NavCustomizeSheet`, `StashImageLoader`. Note: `MoreSheet` is a **private composable inside `BottomNav.kt:179`**, not a standalone file. | `:core:designsystem`, `:core:model`, `:core:domain`, `:core:network`, **`:core:data`** (see Layering rules) |
| `:feature:connection` | Sign-in / endpoint setup | `ConnectionScreen`, `ConnectionViewModel` | core modules via `stash.android.feature` |
| `:feature:home` | Home rails (Continue watching, Recently released, Recently added, Most played) | `HomeScreen`, `HomeViewModel`, `HomeUiState` | core modules via `stash.android.feature` |
| `:feature:library` | Paged scene grid with filter sheet and presets | `LibraryScreen`, `LibraryViewModel`, `FilterSheet` | + `:core:data` (UiPreferences) |
| `:feature:browse` | Performers / studios / tags paged listings | `BrowseScreen`, `BrowseViewModel` | core modules via `stash.android.feature` |
| `:feature:detail` | Single-scene detail + playback launch | `DetailScreen`, `DetailViewModel` | core modules via `stash.android.feature` |
| `:feature:player` | Media3 ExoPlayer surface, queue handling, codec/capability checks | `PlayerScreen`, `PlayerViewModel`, `PlayerQueue`, `CodecCapabilities`, `StashPlayerFactory` | + `:core:data` (PlayerPreferences); Media3 ExoPlayer + nextlib FFmpeg |
| `:feature:settings` | Settings UI, disconnect/reconnect, browse entry points, per-app language picker | `SettingsScreen`, `SettingsViewModel`, `LanguageRow` (private — fires `ACTION_APP_LOCALE_SETTINGS`; guarded by `Build.VERSION.SDK_INT >= 33`) | + `:core:data` (UiPreferences); + `feature/settings/src/main/res/values/strings.xml` (co-located due to module-graph R-class constraint) |
| `:baselineprofile` | Macrobenchmark module that generates the baseline profile installed by `:app`'s `androidx.profileinstaller`. Not part of a normal build; invoked via `./gradlew :app:generateBaselineProfile`. | — | drives `:app` |
| `build-logic:convention` | Included build providing the `stash.android.*` Gradle convention plugins | `AndroidApplicationConventionPlugin`, `AndroidLibraryConventionPlugin`, `AndroidComposeConventionPlugin`, `AndroidHiltConventionPlugin`, `AndroidFeatureConventionPlugin` | — |

## Layering rules

The intended dependency direction is one-way and shallow:

```
:app  →  :feature:*  →  :core:ui / :core:designsystem  →  :core:domain  →  :core:model / :core:common
                                                              ▲
                                                    @Binds in :core:data
                                                              │
                                                          :core:network
```

Hard rules:

- `:app` is the only module allowed to depend on every other module.
- `:feature:*` modules must not depend on each other. (The convention plugin
  does not wire any feature→feature edge; if you need shared feature code,
  extract a `:core:*` module.)
- `:feature:*` modules talk to data through `:core:domain` interfaces.
  Hilt resolves the impl at runtime from `:core:data`.
- `:core:domain` must remain pure Kotlin — no Android, no Apollo, no DataStore.

Known violations of the "feature → domain only" rule (do not extend; do not
add new ones without a domain interface):

- `:core:ui` declares `implementation(project(":core:data"))`. This is a known
  layering wart tracked under DEPS-07 / WR-01 in `.planning/phases/01-deps-foundation-bump/01-REVIEW.md`.
  The follow-up plan is to lift the affected preference surfaces into
  `:core:domain` interfaces so `:core:ui` no longer needs the concrete store.
- `:feature:library`, `:feature:player`, and `:feature:settings` each pull in
  `:core:data` directly to read `UiPreferences` / `PlayerPreferences`. Same
  remediation: define a small interface in `:core:domain` (e.g. `PlayerSettings`)
  and bind the DataStore-backed impl in `DataModule`.

## Convention plugins

The `build-logic/` included build keeps every module's `build.gradle.kts` to
roughly ten lines. There are five plugins, all under
`io.stashapp.android.buildlogic`.

| Plugin id | Source | Applies | Configures |
|---|---|---|---|
| `stash.android.application` | `AndroidApplicationConventionPlugin.kt` | `com.android.application`, `org.jetbrains.kotlin.android` | `compileSdk=35`, `targetSdk=35`, `minSdk=26`, JVM 17 toolchain, opt-ins, lint detector exclusions |
| `stash.android.library` | `AndroidLibraryConventionPlugin.kt` | `com.android.library`, `org.jetbrains.kotlin.android` | Same Kotlin/Android baseline as above, plus `consumerProguardFiles("consumer-rules.pro")` |
| `stash.android.compose` | `AndroidComposeConventionPlugin.kt` | `org.jetbrains.kotlin.plugin.compose` | Sets `buildFeatures.compose = true`, wires the Compose BOM, `ui`, `ui-graphics`, `ui-tooling-preview`, `material3`, `material-icons-extended`, `foundation`, and debug `ui-tooling` |
| `stash.android.hilt` | `AndroidHiltConventionPlugin.kt` | `com.google.devtools.ksp`, `com.google.dagger.hilt.android` | Adds `hilt-android` impl and `hilt-compiler` KSP dependencies |
| `stash.android.feature` | `AndroidFeatureConventionPlugin.kt` | `stash.android.library` + `stash.android.hilt` + `stash.android.compose` | Adds the standard `:core:ui`, `:core:designsystem`, `:core:domain`, `:core:model`, `:core:common` dependencies, plus AndroidX lifecycle/navigation/coroutines so feature modules only declare feature-specific extras |

Shared Kotlin/Android setup lives in `KotlinAndroid.kt::configureKotlinAndroid`:
JDK 17 toolchain, `JvmTarget.JVM_17`, `compileSdk=35`, `minSdk=26`,
`isCoreLibraryDesugaringEnabled=false`, and a small list of disabled lint
detectors that crash under the current AGP 8.7.3 + Kotlin 2.2.20 combination.

When a module's `build.gradle.kts` looks like

```kotlin
plugins { alias(libs.plugins.stash.android.feature) }
android { namespace = "io.stashapp.android.feature.home" }
```

that is the entire wiring — every base dep comes from the feature plugin.

## Data flow

End-to-end path for a screen that reads from the Stash server:

1. **Activity boot.** `MainActivity.onCreate` calls `installSplashScreen()`
   (BEFORE `super.onCreate`) and sets a `setKeepOnScreenCondition` gate on
   `RootViewModel.start` with a 3-second ANR safety-timeout. It then enables
   edge-to-edge (`enableEdgeToEdge()`), requests the highest available display
   refresh rate, and sets the Compose content to
   `StashTheme { StashAppContent(rootViewModel) }`.
2. **Start destination.** `RootViewModel.init` collects
   `ConnectionRepository.activeServer()`. The first emission is either
   `Routes.Connection` (no server configured) or `Routes.Home`.
3. **Navigation host.** `AppNavHost` registers one composable per route:
   `connection`, `home`, `library/{preset}`, `browse/{kind}`,
   `detail/{sceneId}`, `player/...`, `settings`. The `MainBottomBar` is shown
   only on the four top-level tabs.
4. **Screen + ViewModel.** Each route composable calls `hiltViewModel()` to get
   its `@HiltViewModel`. The VM holds `MutableStateFlow<UiState>` and exposes
   the read-only `StateFlow<UiState>`. The composable consumes it via
   `collectAsStateWithLifecycle()`.
5. **Repository call.** The VM calls a `:core:domain` interface
   (e.g. `SceneRepository.scenes(query, limit)`). Hilt has bound the interface
   to its `Default*Repository` impl in `:core:data` via `DataModule.@Binds`.
6. **Apollo query.** The repository invokes an Apollo-generated operation
   class — `FindScenesQuery`, `FindSceneQuery`, `SceneUpdateMutation`, etc. —
   from `core/network/src/main/graphql/io/stashapp/android/graphql/operations/`.
7. **Endpoint rewrite.** Apollo's call goes through `StashAuthInterceptor`,
   which reads the current endpoint from `StashEndpointProvider`, rewrites
   `request.url` to `endpoint.graphqlUrl`, and attaches the `ApiKey` header.
   The Apollo client itself is built once at a placeholder URL — endpoint
   switching never recreates it.
8. **Mapping.** The repository maps Apollo's generated types into
   `:core:model` types (e.g. `SceneMapper.toSummary(...)`) and wraps the
   result in `AppResult<T>`.
9. **State update.** VM updates its `MutableStateFlow`; the composable
   recomposes off the new value.

Paged lists (Library, Browse) take a parallel path: the repository exposes
`Flow<PagingData<T>>` built from `Pager(PagingConfig(...))` over a custom
`PagingSource`. UI consumes via `collectAsLazyPagingItems()` inside a
`LazyVerticalGrid`/`LazyColumn`. Streams are cached with
`cachedIn(viewModelScope)` so config changes do not refetch.

## Cross-cutting concerns

**Dependency injection — Hilt + KSP.** Every repository binding is in
`core/data/src/main/java/io/stashapp/android/core/data/di/DataModule.kt`
(`@Module @InstallIn(SingletonComponent::class)`). Every ViewModel is
`@HiltViewModel`. Application entry is `@HiltAndroidApp` (`StashApp`),
activity entry is `@AndroidEntryPoint` (`MainActivity`). KSP runs the Hilt
compiler — there is no kapt. `SingletonComponent` is the only scope used.

**Preferences — DataStore + EncryptedSharedPreferences.**
`core/data/.../prefs/` holds three stores:

- `ConnectionStore` — persists known servers; the API key is stored in
  `EncryptedSharedPreferences` (AndroidX `security-crypto`).
- `UiPreferences` — bottom-nav customization, library filter defaults.
- `PlayerPreferences` — seek sensitivity, codec preferences, etc.

`EndpointStateHolder` (singleton) owns the in-memory active endpoint and
implements `StashEndpointProvider` for `:core:network`. It is deliberately
separate from `DefaultConnectionRepository` to break a would-be cycle:
`ApolloClient` needs the endpoint, the repository needs the Apollo client.

**Networking — Apollo Kotlin 4 + OkHttp.** `core/network/build.gradle.kts`
declares a single `service("stash")` with package
`io.stashapp.android.graphql` and pass-through scalar mappings for Stash's
custom scalars (`Time`, `Timestamp`, `Map`, `BoolMap`, `PluginConfigMap`,
`Any`, `Int64`, `Upload` — all to `String`). The schema is vendored at
`core/network/src/main/graphql/io/stashapp/android/graphql/schema.graphqls`.
Cleartext HTTP is allowed in `AndroidManifest.xml` / `network_security_config.xml`
because Stash on a LAN typically runs plain HTTP.

**Image loading — Coil 3.** `StashApp` implements
`coil3.SingletonImageLoader.Factory` and supplies a Hilt-built
`StashImageLoaderFactory` (`core/ui/src/main/java/io/stashapp/android/core/ui/image/StashImageLoader.kt`).
This routes image fetches through the same OkHttp stack as Apollo so the
auth header is reused.

**Playback — Media3 ExoPlayer + nextlib FFmpeg.** `:feature:player` pulls
`androidx.media3` (`exoplayer`, `exoplayer-hls`, `exoplayer-dash`, `ui`,
`session`, `datasource-okhttp`) plus the nextlib `media3ext` artifact for
prebuilt FFmpeg software decoders covering codecs ExoPlayer's default
decoders do not (AC3/EAC3/DTS/TrueHD, plus H.264/HEVC/VP8/VP9 fallback).
The build file also picks up any `media3-decoder-ffmpeg*.aar` dropped in
`feature/player/libs/` for users running custom decoder sets.

**Edge-to-edge and insets.** `themes.xml` does not set `statusBarColor` or
`navigationBarColor` (they were removed in COMPLY-01). All three
`ModalBottomSheet` composables (`FilterSheet`, `NavCustomizeSheet`,
`BottomNav.MoreSheet`) set `contentWindowInsets = { WindowInsets.navigationBars }`.
`PlayerScreen` wraps its chrome (controls + locked-overlay) in
`Box(Modifier.safeDrawingPadding())` while the `AndroidView` ExoPlayer surface
stays full-bleed. Top-level `Scaffold`s use Material3 defaults
(`ScaffoldDefaults.contentWindowInsets`, equivalent to `WindowInsets.systemBars`).

**Predictive back.** `AndroidManifest.xml` opts in via
`android:enableOnBackInvokedCallback="true"`. `PlayerScreen.kt:188` uses
`PredictiveBackHandler` (from `androidx.activity`) instead of the removed
`BackHandler`, with `try { progress.collect { … }; onExit() } catch (e: CancellationException) { throw e }`
cancel semantics. No other back handlers exist in the repo.

**Per-app language picker.** `app/build.gradle.kts` sets
`androidResources { generateLocaleConfig = true }`. AGP produces
`_generated_res_locale_config.xml` (referenced via `android:localeConfig` in
the merged manifest). `SettingsScreen.LanguageRow` fires
`Intent(Settings.ACTION_APP_LOCALE_SETTINGS)` guarded by
`Build.VERSION.SDK_INT >= 33 (TIRAMISU)`.

**State management.** ViewModels expose `StateFlow<UiState>`; composables
consume via `collectAsStateWithLifecycle()`. Repositories return
`Flow<PagingData<T>>` for paged data and `suspend AppResult<T>` for one-shot
calls. Exceptions never cross the repository boundary — they are caught and
converted to `AppError` variants (`Network`, `Auth`, `NotFound`, `Server`,
`Unknown`).

## What is deliberately not here yet

These are explicit deferrals, not oversights — do not add them ad hoc; they
are scheduled work.

- **No automated test suite.** There are no JVM unit tests, no Robolectric
  tests, and no instrumented/Espresso tests. The test infrastructure work
  is tracked as POLISH-04.
- **No CI pipeline.** The repository has no `.github/workflows/` directory.
  Lint, detekt, ktlint, and the OWASP dependency-check task run only on
  developer machines today. CI bring-up is tracked as SEC-CI-01 in the
  backlog.
- **No background `MediaSessionService`.** `:feature:player` uses Media3
  but does not expose a foreground media session for system controls or
  background playback. That work is tracked as BG-MEDIA-01.

When you add tests, CI, or a media service, do not invent a new pattern —
revisit the corresponding backlog item first.

## Where to make common changes

**Add a new feature module.**

1. Create `feature/<name>/build.gradle.kts` containing only:
   ```kotlin
   plugins { alias(libs.plugins.stash.android.feature) }
   android { namespace = "io.stashapp.android.feature.<name>" }
   ```
2. Add `include(":feature:<name>")` to `settings.gradle.kts`.
3. Create `<Name>Screen.kt` (Compose) and `<Name>ViewModel.kt`
   (`@HiltViewModel`, injects `:core:domain` interfaces only).
4. Wire a new route into `core/ui/src/main/java/io/stashapp/android/core/ui/nav/Routes.kt`
   and add a `composable(...)` for it in `AppNavHost` inside
   `app/src/main/java/io/stashapp/android/MainActivity.kt`.
5. Add `implementation(project(":feature:<name>"))` to `app/build.gradle.kts`.

**Add a new Stash GraphQL query.**

1. Drop a `.graphql` file under
   `core/network/src/main/graphql/io/stashapp/android/graphql/operations/`.
   Apollo's KSP step generates the operation class on next build.
2. If the response uses a scalar not already mapped in
   `core/network/build.gradle.kts`'s `apollo { service("stash") { ... } }`
   block, add a `mapScalarToKotlinString(...)` line.
3. Call the generated query from a method on a `Default*Repository` in
   `:core:data`, mapping into a `:core:model` type and wrapping in
   `AppResult`. Add the corresponding signature to the matching interface
   in `:core:domain`.

**Add or change a preference.**

1. Pick the right store in `core/data/src/main/java/io/stashapp/android/core/data/prefs/`:
   `UiPreferences` for chrome / library defaults, `PlayerPreferences` for
   playback knobs, `ConnectionStore` for connection metadata (and only
   `ConnectionStore` for secrets — it uses `EncryptedSharedPreferences`).
2. Expose the new value as a `Flow` plus a `suspend` setter on the store.
3. For preference reads from a feature module, prefer adding a `:core:domain`
   interface and binding the DataStore-backed impl in `DataModule` rather
   than importing `:core:data` directly (see Layering rules → DEPS-07 / WR-01).

**Add a new repository.**

1. Define the interface in `core/domain/src/main/java/io/stashapp/android/core/domain/`,
   returning `Flow<PagingData<T>>` or `suspend AppResult<T>`.
2. Implement it as `Default<Name>Repository` in
   `core/data/src/main/java/io/stashapp/android/core/data/<area>/`.
3. Add a `@Binds @Singleton` line in `DataModule`.
4. Inject the interface into the ViewModel that needs it — never the impl.
