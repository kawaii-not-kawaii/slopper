<!-- generated-by: gsd-doc-writer -->
# Slopper Development Guide

A recipe-oriented guide for working in the Slopper Android codebase. This document
focuses on the conventions, gotchas, and common workflows specific to this project.

- New to the codebase? Read [GETTING-STARTED.md](GETTING-STARTED.md) first.
- Module layout & architecture overview: top-level `README.md`.
- Detekt rules and project-wide style: `config/detekt/detekt.yml`.

---

## Module layout

```
:app                         — single application module (DI, MainActivity, AppNavHost)
:baselineprofile             — macrobenchmark module for baseline profile generation
:core:common                 — shared utilities (AppResult, Dispatchers, ...)
:core:model                  — pure Kotlin domain models (no Android deps)
:core:designsystem           — Compose components + Material theme
:core:ui                     — shared UI infra (image loader, format helpers)
:core:network                — Apollo client + vendored Stash GraphQL schema
:core:data                   — repositories + DataStore preferences
:core:domain                 — repository interfaces + use-case-style query types
:feature:{home,library,browse,detail,player,settings,connection}
                             — one Compose screen + ViewModel per feature
:build-logic:convention      — Gradle convention plugins (see below)
```

Every Android module applies one of the convention plugins from
`:build-logic:convention` rather than configuring AGP/Kotlin directly. The
convention plugins are the single source of truth for `compileSdk`, `minSdk`,
Java/Kotlin target, lint disables, and shared dependencies.

| Plugin ID | Class | Purpose |
|-----------|-------|---------|
| `stash.android.application` | `AndroidApplicationConventionPlugin` | Used by `:app` only. Applies `com.android.application` + `kotlin-android` and sets `targetSdk = 35`. |
| `stash.android.library`     | `AndroidLibraryConventionPlugin`     | Base for every non-app Android module. Applies `com.android.library` + `kotlin-android`, sets `targetSdk = 35`, wires `consumer-rules.pro`. |
| `stash.android.compose`     | `AndroidComposeConventionPlugin`     | Enables `buildFeatures.compose`, pulls the Compose BOM, and adds the canonical Compose deps (ui, material3, foundation, icons-extended, tooling). |
| `stash.android.hilt`        | `AndroidHiltConventionPlugin`        | Applies KSP + Hilt and adds `hilt-android` + `hilt-android-compiler`. |
| `stash.android.feature`     | `AndroidFeatureConventionPlugin`     | Bundles library + hilt + compose and adds the shared `:core:*` + lifecycle + navigation dependencies every feature needs. |

Shared logic lives in `build-logic/convention/src/main/kotlin/io/stashapp/android/buildlogic/KotlinAndroid.kt`
(`configureKotlinAndroid`) — that's where `compileSdk = 35`, `minSdk = 26`,
JDK 17 toolchain, and the lint detector disables are defined.

---

## Recipe: add a library to the version catalog

The version catalog is `gradle/libs.versions.toml`. **Never** hard-code a
version in a module `build.gradle.kts` — add it to the catalog first.

1. Add the version under `[versions]`:

   ```toml
   [versions]
   awesomeLib = "1.4.0"
   ```

2. Add the library entry under `[libraries]`:

   ```toml
   [libraries]
   awesome-lib = { module = "com.example:awesome-lib", version.ref = "awesomeLib" }
   ```

3. Reference it from a module `build.gradle.kts`:

   ```kotlin
   dependencies {
       implementation(libs.awesome.lib)   // dot-segments map to dashes in the TOML key
   }
   ```

   Catalog accessors convert dashes to dots: `awesome-lib` → `libs.awesome.lib`.

4. For plugin entries, add under `[plugins]` and reference via
   `alias(libs.plugins.foo)` in `plugins { ... }`.

5. Run `./gradlew help` to confirm the catalog is well-formed. The
   `versionCatalogUpdate` workflow is not wired — bumps are done by hand.

**Policy:** stable releases only. The catalog header comments document the
explicit holds (Apollo 4.4.3, OkHttp 4.12.0, Media3 1.9.1 — see DEPS-17 / NET-01
in `.planning/`). Don't bump those without sign-off.

---

## Recipe: add a new feature module

Every feature follows the same shape. Replace `<name>` with the lowercase
feature name (e.g. `playlist`).

1. **Create the directory** under `feature/<name>/` with:

   ```
   feature/<name>/
   ├── build.gradle.kts
   ├── consumer-rules.pro            # empty file is fine
   └── src/main/
       ├── AndroidManifest.xml       # <manifest> with no application tag
       └── java/io/stashapp/android/feature/<name>/
           ├── <Name>Screen.kt
           └── <Name>ViewModel.kt
   ```

2. **`build.gradle.kts`** — minimal, the feature convention plugin does the rest:

   ```kotlin
   plugins {
       alias(libs.plugins.stash.android.feature)
   }

   android {
       namespace = "io.stashapp.android.feature.<name>"
   }
   ```

   `stash.android.feature` transitively applies `library + hilt + compose`
   and wires `:core:ui`, `:core:designsystem`, `:core:domain`, `:core:model`,
   `:core:common`, plus lifecycle + navigation-compose + coroutines. You only
   need to add module-specific deps (e.g. `:core:data` if the ViewModel
   touches repositories — features by default talk to `:core:domain` only).

3. **Register in `settings.gradle.kts`** — add an `include(":feature:<name>")`
   line in the feature-modules block.

4. **Add to `app/build.gradle.kts` dependencies** if the feature ships a screen:

   ```kotlin
   implementation(project(":feature:<name>"))
   ```

5. **`<Name>ViewModel.kt`** — Hilt + StateFlow shape used everywhere:

   ```kotlin
   @HiltViewModel
   class FooViewModel @Inject constructor(
       private val sceneRepository: SceneRepository,
   ) : ViewModel() {
       private val _state = MutableStateFlow(FooUiState.Initial)
       val state: StateFlow<FooUiState> = _state.asStateFlow()
   }
   ```

6. **`<Name>Screen.kt`** — entry composable receives navigation callbacks and
   gets the VM via `hiltViewModel()`:

   ```kotlin
   @Composable
   fun FooScreen(
       onSomethingClick: (id: String) -> Unit,
       viewModel: FooViewModel = hiltViewModel(),
   ) {
       val state by viewModel.state.collectAsStateWithLifecycle()
       // ...
   }
   ```

   See `feature/home/src/main/java/.../HomeScreen.kt` + `HomeViewModel.kt` for
   the canonical example.

7. **Wire the route** in `app/src/main/java/io/stashapp/android/MainActivity.kt`'s
   `AppNavHost`. Add a constant in `Routes` (same file) and a `composable(Routes.Foo) { ... }`
   block inside `AppNavHost`. Navigation lambdas are passed in as parameters —
   don't reach into `NavController` from inside the feature module.

---

## Recipe: add a new Stash GraphQL operation

The Stash GraphQL schema is vendored at
`core/network/src/main/graphql/io/stashapp/android/graphql/schema.graphqls`
(provenance recorded in the file header — re-vendor when upstream breaks).

1. **Create the operation file** under
   `core/network/src/main/graphql/io/stashapp/android/graphql/operations/`.
   One file per operation, named after the operation (e.g. `FindScene.graphql`).
   Use fragments (e.g. `SceneCard`) where they already exist in `operations/`
   to keep field sets consistent across queries:

   ```graphql
   query FindScene($id: ID!) {
     findScene(id: $id) {
       ...SceneCard
       scene_markers { id title seconds }
     }
   }
   ```

2. **Regenerate Apollo sources:**

   ```bash
   ./gradlew :core:network:generateStashApolloSources
   ```

   This generates `io.stashapp.android.graphql.FindSceneQuery` (and a `Data`
   class with the response shape). Operation classes share the package
   `io.stashapp.android.graphql` (configured in `core/network/build.gradle.kts`
   under `apollo.service("stash").packageName`).

3. **Consume from a repository** in `:core:data` — never call Apollo directly
   from a ViewModel. Repositories implement the interface defined in
   `:core:domain` and return the model types from `:core:model`. The custom
   scalars `Time`, `Timestamp`, `Map`, `BoolMap`, `Int64`, `Upload`, `Any`,
   and `PluginConfigMap` are mapped to `String` on the wire (see
   `core/network/build.gradle.kts`).

---

## Recipe: add a new preference

Preferences are split across two DataStore files in `:core:data` to keep
unrelated concerns from cross-contaminating:

- `PlayerPreferences` (`player_prefs`) — playback, gestures, decoder choice.
- `UiPreferences` (`ui_prefs`) — shell chrome (bottom-nav layout, default
  library filter, image cache size).

Both follow the same pattern. To add `fooBar`:

1. **Pick the right file.** Playback-affecting → `PlayerPreferences`. Anything
   else → `UiPreferences`.

2. **Add a key + default + getter Flow + setter** inside the class body:

   ```kotlin
   // In the body of the class
   val fooBar: Flow<Int> = flow(KEY_FOO_BAR, DEFAULT_FOO_BAR)

   suspend fun setFooBar(value: Int) = put(KEY_FOO_BAR, value)

   // In the companion object
   const val DEFAULT_FOO_BAR: Int = 42
   private val KEY_FOO_BAR = intPreferencesKey("foo_bar")
   ```

   `flow(key, default)` and `put(key, value)` are private helpers already
   defined in both classes — use them.

3. **Inject and consume** via Hilt. Both classes are `@Singleton` and use
   `@Inject` constructor injection:

   ```kotlin
   class MyViewModel @Inject constructor(
       private val playerPreferences: PlayerPreferences,
   ) : ViewModel() {
       val speed = playerPreferences.defaultPlaybackSpeed
           .stateIn(viewModelScope, SharingStarted.Eagerly, PlayerPreferences.DEFAULT_SPEED)
   }
   ```

4. **Never call `runBlocking` on a preference Flow from the UI thread.** There
   is an existing offender in `core/ui/src/main/java/io/stashapp/android/core/ui/image/StashImageLoader.kt`
   (line ~33) that is tracked under `POLISH-CACHE-01` — don't add new ones.

---

## Common Gradle commands

All commands run from the repo root and require Java 17.

| Command | Purpose |
|---------|---------|
| `./bootstrap.sh` | One-time setup on a fresh clone (installs `gradlew`, verifies SDK). |
| `./gradlew :app:assembleDebug` | Build the debug APK. |
| `./gradlew :app:installDebug` | Install debug APK on the attached device. |
| `./gradlew detekt ktlintCheck` | Run both linters in check-only mode (CI does this). |
| `./gradlew detektFormat ktlintFormat` | Autofix lint issues. |
| `./gradlew :app:lintDebug` | Run Android Lint on `:app` (with the 3 documented detector disables in effect). |
| `./gradlew :core:network:generateStashApolloSources` | Regenerate Apollo Kotlin from the vendored schema. |
| `./gradlew :app:dependencies --configuration releaseRuntimeClasspath` | Audit the release dep tree. |
| `./gradlew --refresh-dependencies <task>` | Force re-resolution (bypass dependency cache). |
| `./gradlew dependencyCheckAnalyze --no-configuration-cache` | OWASP CVE scan. The plugin is not configuration-cache compatible — the `--no-configuration-cache` flag is required. |
| `./gradlew :app:generateBaselineProfile` | Run the `:baselineprofile` macrobenchmark module to refresh the baseline profile. |

`./gradlew` is **gitignored** — fresh clones must run `./bootstrap.sh` first.
It will install Gradle 8.11.1 + the wrapper, then check for `ANDROID_SDK_ROOT` /
`ANDROID_HOME`.

---

## Memory tuning (`gradle.properties`)

The defaults are tuned for a 12 GB developer host. CI runners with more RAM
can override these locally (e.g. in `~/.gradle/gradle.properties` or via
`-P` flags) — do not raise them in the checked-in `gradle.properties`.

| Setting | Value | Reason |
|---------|-------|--------|
| `org.gradle.jvmargs` | `-Xmx2g` | Caps the Gradle main daemon heap. |
| `org.gradle.workers.max` | `2` | Limits parallel Gradle workers so daemon + workers + Kotlin daemon stay under ~8 GB. |
| `kotlin.daemon.jvmargs` | `-Xmx1500m` | Caps the Kotlin compiler daemon heap. |
| `org.gradle.parallel` | `true` | Parallel project execution. |
| `org.gradle.caching` | `true` | Build cache enabled. |
| `org.gradle.configuration-cache` | `true` | Configuration cache enabled (note: OWASP `dependencyCheck` is incompatible — see above). |
| `org.gradle.java.installations.auto-download` | `false` | Never let Gradle auto-download a JDK. Daemon JDK is pinned to the JDK 17 toolchain you have installed. |

If you see OOM during a clean build, the usual fix is **not** to raise these —
it's to make sure no other heavyweight processes (browsers, IDE indexers) are
running. The Kotlin 2.2.20 + KSP combination is heap-hungry; with the caps
above, `assembleDebug` peaks around 7 GB.

---

## Lint detector disables (temporary)

`build-logic/.../KotlinAndroid.kt` disables three lint detectors **project-wide**:

| Detector | Source library | Why disabled |
|----------|----------------|--------------|
| `NullSafeMutableLiveData` | androidx.lifecycle 2.8.7 | Bundled lint detector throws `IncompatibleClassChangeError` under AGP 8.7.3 lint runtime + Kotlin 2.2.20. |
| `FrequentlyChangingValue` | Compose BOM 2026.05.00 (compose-runtime) | Same root cause — old detector binary vs new Kotlin metadata. |
| `RememberInComposition` | Compose BOM 2026.05.00 (compose-runtime) | Same root cause. |

These are tracked under **DEPS-07** (AndroidX Lifecycle 2.10.x bump, currently
deferred). When DEPS-07 lands, the `disable.addAll(...)` block in
`KotlinAndroid.kt` should be removed and `:app:lintDebug` re-run to confirm
nothing regressed.

---

## detekt + ktlint baselines

Both linters run on every subproject via the `subprojects { ... }` block in the
root `build.gradle.kts`. Pinned versions:

- detekt: **1.23.8** (matches `gradle/libs.versions.toml` `detekt` and the
  `toolVersion` in the root build).
- ktlint plugin: **13.1.0** with ktlint engine version **1.6.0** (pinned in
  the root `build.gradle.kts` via `KtlintExtension.version.set("1.6.0")`).

Project-wide detekt config lives at `config/detekt/detekt.yml` (loosens
`LongMethod`, `LongParameterList`, `MagicNumber` etc. for Compose idioms; see
the file for the full list of overrides).

**Baselines** (existing violations grandfathered in) live alongside each module:

- detekt: `<module>/detekt-baseline.xml` — current locations: `app/`,
  `core/data/`, `core/designsystem/`, `core/ui/`, `feature/connection/`,
  `feature/detail/`, `feature/player/`, `feature/settings/`.
- lint: `app/lint-baseline.xml`.

To regenerate a detekt baseline after a large refactor:

```bash
./gradlew :<module>:detektBaseline
```

To regenerate the lint baseline:

```bash
./gradlew :app:updateLintBaseline
```

**Don't** regenerate a baseline to silence a new violation you just
introduced — fix the code instead. Baselines are for grandfathered debt only.

---

## Gotchas

**Bootstrap is mandatory on fresh clones.** `gradlew`, `gradlew.bat`, and
`gradle/wrapper/gradle-wrapper.jar` are all gitignored. Running `./gradlew`
before `./bootstrap.sh` will fail with "no such file". The bootstrap script
also verifies `ANDROID_SDK_ROOT` / `ANDROID_HOME` is set.

**No `runBlocking` on DataStore from the UI thread.** DataStore reads are
suspending — collect them as a Flow with `collectAsStateWithLifecycle()` or
hoist them into a ViewModel-scoped `StateFlow`. The one known offender is
`core/ui/.../StashImageLoader.kt` line 33 (tracked as `POLISH-CACHE-01`).

**Hilt requires ≥ 2.56 with Kotlin 2.2.** The catalog pins `hilt = "2.56.2"`.
Earlier Hilt releases (2.52 and below) fail KSP under Kotlin 2.2 with cryptic
`KaptGenerateStubsTask` errors. If you bump Kotlin, bump Hilt at the same time.

**AGP 9 is not supported.** Hilt does not publish an AGP-9-compatible release
yet. The toolchain is locked at **AGP 8.7.3 + Gradle 8.11.1**. Don't bump AGP
to 9.x — it's deferred under `DEPS-03` / `DEPS-04` (see `gradle/libs.versions.toml`
header comments and `.planning/REQUIREMENTS.md`).

**OWASP dependencyCheck is configuration-cache hostile.** It must be invoked
with `--no-configuration-cache`, otherwise it errors during task graph
configuration. The fail-on-CVSS threshold is `7.0` (HIGH+).

**Apollo schema is vendored.** The schema in
`core/network/src/main/graphql/io/stashapp/android/graphql/schema.graphqls`
is a snapshot of `stashapp/stash@develop` (SHA recorded in the file header).
It is **not** auto-refreshed. Re-vendor when upstream introduces a breaking
schema change or at every Slopper minor release.

**Custom scalars map to `String`.** Stash's GraphQL custom scalars (`Time`,
`Timestamp`, `Map`, `BoolMap`, `Int64`, `Upload`, `Any`, `PluginConfigMap`)
are all mapped to `kotlin.String` via `mapScalarToKotlinString` in
`core/network/build.gradle.kts`. Parse them inside repositories if you need
structured types — don't change the mapping.

**Feature modules talk to `:core:domain`, not `:core:data`.** The convention
plugin only wires `:core:domain`, `:core:model`, `:core:common`, `:core:ui`,
and `:core:designsystem`. If a feature genuinely needs to reach into
`:core:data` (rare), add it explicitly in that feature's `build.gradle.kts` —
but first check whether the abstraction it needs should live in `:core:domain`.

**Routes are central, navigation lambdas flow down.** All routes live in
`Routes` (in `app/src/main/java/io/stashapp/android/MainActivity.kt`).
Feature screens take `onSomethingClick: (id: String) -> Unit` callbacks and
remain decoupled from `NavController`. Don't inject `NavController` into a
feature.
