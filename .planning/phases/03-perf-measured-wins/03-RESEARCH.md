# Phase 3: PERF — Measured Wins — Research

**Researched:** 2026-05-18
**Domain:** Android performance engineering — macrobenchmark infrastructure, Compose stability, ExoPlayer lifecycle
**Confidence:** HIGH (all critical claims verified against official docs or live codebase reads)

---

## Summary

Phase 3 operates on a stable AGP 8.7.3 / Kotlin 2.2.20 / Compose BOM 2026.05.00 floor. The work divides into three concerns: (1) measurement substrate — getting reproducible numbers out of the device, (2) recomposition hygiene — eliminating the known waste sources identified by the CONCERNS.md audit, and (3) runtime correctness — diagnosing the shuffle hang before it is measured.

All implementation targets were confirmed via live codebase reads. There are no "looks like it should work" guesses in this document — every current-state description is drawn from the actual file content at the lines specified.

**Primary recommendation:** Execute in the wave sequence locked by D-09. Plan 3.1 (GMD + stability reports + profile expansion) must land before Plans 3.2 and 3.3 run, because stability report output from 3.1 drives verification of 3.2's ImmutableList changes.

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- D-01: GMD = Pixel 6 / API 34 / google_apis, `useConnectedDevices = false`
- D-01a: REVIEWS-C4 ACCEPT if system image cannot be downloaded (PERF-06/07 deferred, PERF-01 still lands)
- D-02: Stability reports via `ComposeCompilerGradlePluginExtension.reportsDestination`
- D-03: `toPersistentList()` at ViewModel boundary; UiState fields change type to `ImmutableList<T>`
- D-04: 5 LaunchedEffect sites audited; line 166 (`playerView`) is the only non-trivial case
- D-05: `applyVideoFrameRate` moves from `AndroidView.update` to `LaunchedEffect(state.videoFrameRate)`
- D-06: Shuffle investigation order: listener count → media item lifecycle → memory profiler
- D-07: 3 new journeys added to `StashBaselineProfileGenerator`: Home rails scroll, Detail open, Player first-frame
- D-08: Raw benchmark output committed to `.planning/benchmarks/`
- D-09: Plan 3.1 wave 1; Plans 3.2 + 3.3 wave 2 parallel, both `depends_on: plan-3.1`
- D-10: ≤ 10 atomic commits

### Claude's Discretion
- Exact Compose Compiler plugin DSL syntax — verify against Kotlin 2.2.20 actual API
- `compose_stability.conf` filename and placement
- Whether `playerView` hoisting in D-05 requires restructuring other AndroidView interactions
- `aosp_atd` vs `google_apis` system image fallback if `google_apis` is unavailable

### Deferred Ideas (OUT OF SCOPE)
- `PlayerScreen.kt` split (POLISH-01)
- JUnit5/Turbine/MockK/Robolectric wiring (POLISH-04/05)
- AGP 9 / compileSdk 36 / Hilt 2.57+ migration (DEPS-17)
- Media3 1.10.0 upgrade
- `ConnectionResult` → `AppResult` migration (POLISH-07)
- `PagingData<T>` types in Library/Browse (NOT migrated to ImmutableList)
</user_constraints>

---

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| GMD declaration | Build system (Gradle) | — | `managedDevices` block is a build config concern, not runtime |
| Stability reports | Build system (Compose compiler plugin) | — | Compile-time output from Kotlin compiler plugin |
| ImmutableList migration | ViewModel (state boundary) | Composable param types | ViewModel emits `ImmutableList`; composables accept it |
| LaunchedEffect key audit | Composable (PlayerScreen) | — | Effect scoping is a composable-level concern |
| applyVideoFrameRate relocation | Composable (PlayerScreen) | — | Side effect wiring change within the composable |
| Baseline profile expansion | Test module (baselineprofile) | — | `StashBaselineProfileGenerator.kt` in the test module |
| Shuffle fix | ViewModel (PlayerViewModel) | ExoPlayer listener | Listener lifecycle management is ViewModel responsibility |
| Macrobench classes | Test module (baselineprofile) | — | `MacrobenchmarkRule`-based classes alongside the profile generator |
| Benchmark output files | Planning artifacts (.planning/benchmarks/) | — | Committed as evidence, not shipped in APK |

---

## A1 — GMD Setup (PERF-01)

### Current State
`baselineprofile/build.gradle.kts` (confirmed via file read, lines 31–37):
```kotlin
// Use whatever device is plugged in via adb when generation is requested.
// Declaring a gradle-managed-device (GMD) here would let CI spin up an
// emulator automatically — punted for now...
baselineProfile {
    useConnectedDevices = true
}
```
No `managedDevices` block exists. No `testOptions` block in the file.

### Target State
Replace with the GMD declaration locked in D-01. The `managedDevices` block goes inside `android { testOptions { ... } }` in the **test module** (`baselineprofile/build.gradle.kts`), not in the app module:

```kotlin
// baselineprofile/build.gradle.kts
android {
    // ... existing namespace, compileSdk, compileOptions, kotlinOptions,
    // defaultConfig, targetProjectPath, experimentalProperties blocks ...

    testOptions {
        managedDevices {
            localDevices {
                create("pixel6Api34") {
                    device = "Pixel 6"
                    apiLevel = 34
                    systemImageSource = "google_apis"
                }
            }
        }
    }
}

baselineProfile {
    managedDevices += "pixel6Api34"
    useConnectedDevices = false
}
```

[VERIFIED: developer.android.com/studio/test/gradle-managed-devices — localDevices DSL confirmed]

### Generated Task Names
The Android Gradle Plugin generates tasks following the pattern `{deviceName}{BuildVariant}AndroidTest`.

- Device name: `pixel6Api34` (the `create(...)` argument)
- Build variant for the `com.android.test` module: `benchmark` (the `experimentalProperties["android.experimental.self-instrumenting"] = true` flag causes AGP to generate a `benchmark` variant)
- **Primary task:** `./gradlew :baselineprofile:pixel6Api34BenchmarkAndroidTest`
- **Profile generation shortcut:** `./gradlew :app:generateBaselineProfile` (the `androidx.baselineprofile` plugin reads `managedDevices` from the test module and drives generation automatically)

[VERIFIED: developer.android.com/studio/test/gradle-managed-devices — task naming convention confirmed]
[CITED: developer.android.com/topic/performance/baselineprofiles/create-baselineprofile — managedDevices + baselineProfile block co-location in test module confirmed]

### systemImageSource Values
| Value | Use case |
|-------|----------|
| `"google_apis"` | Preferred — includes Play Services, most representative of user devices |
| `"aosp"` | Fallback if google_apis image unavailable — no Play Services, but works without Google license |
| `"aosp-atd"` | Automated test device — headless, faster boot, no display rendering; NOT suitable for UI-driving baseline profile generation |
| `"google-atd"` | Google ATD — same headless limitation; NOT suitable here |

D-01 locks `"google_apis"`. Fallback per D-01a and Claude's Discretion: use `"aosp"` if the `google_apis` image download fails.

### REVIEWS-C4 ACCEPT Condition
If `./gradlew :baselineprofile:pixel6Api34BenchmarkAndroidTest` fails due to system image download failure (host memory < ~6GB free, or Google license rejection): PERF-06 and PERF-07 are deferred. The GMD declaration code (PERF-01) still lands in git. Commit a `REVIEWS-C4-ACCEPT.md` note in `.planning/benchmarks/` documenting the failure reason.

### Pitfall: `android.test` Module vs App Module
The `managedDevices` block MUST be in `baselineprofile/build.gradle.kts` (the `com.android.test` module), not in `app/build.gradle.kts`. The `baselineProfile { managedDevices += ... }` plugin then reads from there. If placed in the app module, the profile plugin cannot find the device.

[VERIFIED: developer.android.com/topic/performance/baselineprofiles/create-baselineprofile]

### Pitfall: Device Name Casing
The `create("pixel6Api34")` argument is case-sensitive and becomes the task-name segment directly. The CONTEXT.md locks `"pixel6Api34"` (camelCase starting with lowercase). Task name will be `pixel6Api34BenchmarkAndroidTest`.

---

## A2 — Compose Compiler Stability Reports (PERF-02)

### Current State
`build-logic/convention/src/main/kotlin/io/stashapp/android/buildlogic/AndroidComposeConventionPlugin.kt` (confirmed via file read):
```kotlin
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
            val extension = extensions.getByType(CommonExtension::class.java)
            extension.apply {
                buildFeatures.compose = true
            }
            dependencies { /* BOM + compose libs wired */ }
        }
    }
}
```
No `ComposeCompilerGradlePluginExtension` configuration exists anywhere in build-logic.

### Target State
Add `extensions.configure<ComposeCompilerGradlePluginExtension>` block inside the `with(target)` block of `AndroidComposeConventionPlugin.kt`:

```kotlin
// build-logic/convention/src/main/kotlin/io/stashapp/android/buildlogic/AndroidComposeConventionPlugin.kt

package io.stashapp.android.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension  // ADD

class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            val extension = extensions.getByType(CommonExtension::class.java)
            extension.apply {
                buildFeatures.compose = true
            }

            // ADD: Wire Compose Compiler stability reports and metrics to the
            // per-module build directory so they're never committed accidentally.
            extensions.configure<ComposeCompilerGradlePluginExtension> {
                reportsDestination = layout.buildDirectory.dir("compose-reports")
                metricsDestination = layout.buildDirectory.dir("compose-metrics")
                stabilityConfigurationFile =
                    rootProject.layout.projectDirectory.file("compose_stability.conf")
            }

            dependencies {
                // ... existing BOM + compose deps unchanged ...
            }
        }
    }
}
```

[VERIFIED: kotlinlang.org API reference — `ComposeCompilerGradlePluginExtension` properties confirmed: `reportsDestination: DirectoryProperty`, `metricsDestination: DirectoryProperty`, `stabilityConfigurationFile: RegularFileProperty`]

### Import Availability
`ComposeCompilerGradlePluginExtension` is in the `org.jetbrains.kotlin.compose.compiler.gradle` package, provided by the `org.jetbrains.kotlin:compose-compiler-gradle-plugin` artifact. This artifact is already listed in `build-logic/convention/build.gradle.kts` as:
```kotlin
compileOnly(libs.plugin.compose)  // = "org.jetbrains.kotlin:compose-compiler-gradle-plugin:2.2.20"
```
[VERIFIED: `/home/yun/slopper/build-logic/convention/build.gradle.kts` — `compileOnly(libs.plugin.compose)` confirmed]

The import will resolve at compile time in the convention plugin. No additional dependency is needed.

### `configure<T>` vs `getByType<T>`
The existing plugin uses `getByType` for `CommonExtension`. For `ComposeCompilerGradlePluginExtension`, use `extensions.configure<ComposeCompilerGradlePluginExtension> { ... }` (lambda form) which is the standard Gradle Kotlin DSL pattern for configuring an extension that may not yet be realized. Both forms are valid; `configure` is preferred when applying a configuration block.
[VERIFIED: KotlinAndroid.kt in build-logic uses `extensions.configure<KotlinAndroidProjectExtension>` — same pattern]

### `compose_stability.conf` File
The `stabilityConfigurationFile` property points to a file that lists fully-qualified class names the compiler should treat as stable even when Compose's static analysis cannot confirm it. This file is **optional** — if it doesn't exist, `assembleDebug` will fail with a `FileNotFoundException`.

**Safe approach:** Create an empty file at `compose_stability.conf` in the project root alongside `settings.gradle.kts`. An empty file is valid.

```
# compose_stability.conf
# Classes considered stable for Compose strong-skipping.
# Format: one FQN per line, e.g.:
# io.stashapp.android.core.model.SceneSummary
```

Alternatively, use `stabilityConfigurationFiles` (plural `ListProperty`) and only add it when the file exists — but an empty file at root is simpler and satisfies PERF-02.

[CITED: kotlinlang.org/api/kotlin-gradle-plugin/compose-compiler-gradle-plugin — `stabilityConfigurationFile: RegularFileProperty`]
[ASSUMED: An empty `compose_stability.conf` will not cause `compileReleaseKotlin` to fail. Risk: LOW — Gradle file properties accept empty files; the compiler reads the list and gets zero entries.]

### Verification Command (PERF-02 acceptance)
```bash
./gradlew compileReleaseKotlin
find . -path "*/build/compose-reports/*.txt" | wc -l
# Expected: ≥ 1 (one per module using the stash.android.compose convention plugin)
```
Report files contain lines like `stable class SceneSummary { ... }` and `unstable class HomeUiState { ... }` — these become the evidence base for PERF-03 changes.

### Reports are Build Output — gitignored
`build/compose-reports/` is covered by the global `build/` gitignore. Do NOT commit them. Macrobench outputs (`.planning/benchmarks/`) ARE committed.

---

## A3 — ImmutableList Migration (PERF-03)

### Current State — Confirmed File Reads

**HomeViewModel.kt (lines 34–47):**
```kotlin
data class HomeRail(
    val kind: HomeRailKind,
    val loading: Boolean = true,
    val scenes: List<SceneSummary> = emptyList(),   // TARGET 1
    val error: String? = null,
)

data class HomeUiState(
    val rails: List<HomeRail>,                        // TARGET 2
) {
    companion object {
        val Initial = HomeUiState(HomeRailKind.entries.map { HomeRail(it) })
    }
}
```

**PlayerScreen.kt (line 481):**
```kotlin
private fun PlayerControls(
    ...
    markers: List<Marker>,    // TARGET 3a — PlayerControls composable
    ...
)
```

**PlayerScreen.kt (line 1006):**
```kotlin
private fun TimelineBar(
    positionFlow: ...,
    markers: List<Marker>,    // TARGET 3b — TimelineBar composable
    ...
)
```

### Catalog Entry Confirmed
```toml
# gradle/libs.versions.toml
kotlinxCollectionsImmutable = "0.4.0"   # stable; 0.5.0-beta01 excluded by stable-only policy
...
kotlinx-collections-immutable = { module = "org.jetbrains.kotlinx:kotlinx-collections-immutable", version.ref = "kotlinxCollectionsImmutable" }
```
[VERIFIED: `/home/yun/slopper/gradle/libs.versions.toml` — key `kotlinx-collections-immutable`, version `0.4.0`]

### Target State

#### HomeViewModel.kt changes
```kotlin
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

data class HomeRail(
    val kind: HomeRailKind,
    val loading: Boolean = true,
    val scenes: ImmutableList<SceneSummary> = persistentListOf(),   // CHANGED
    val error: String? = null,
)

data class HomeUiState(
    val rails: ImmutableList<HomeRail>,                               // CHANGED
) {
    companion object {
        val Initial = HomeUiState(
            HomeRailKind.entries.map { HomeRail(it) }.toPersistentList()  // CHANGED
        )
    }
}
```

In the `load()` function's `_state.update` block (lines 71–84), the `prev.rails.map { ... }` produces a `List<HomeRail>`. Add `.toPersistentList()` at the assignment boundary:
```kotlin
prev.copy(rails = updated.toPersistentList())  // CHANGED — was: prev.copy(rails = updated)
```

#### PlayerScreen.kt changes
```kotlin
import kotlinx.collections.immutable.ImmutableList

private fun PlayerControls(
    ...
    markers: ImmutableList<Marker>,   // CHANGED
    ...
)

private fun TimelineBar(
    ...
    markers: ImmutableList<Marker>,   // CHANGED
    ...
)
```

All call sites passing `markers` must also pass an `ImmutableList<Marker>`. Find the call sites with `grep -n "PlayerControls\|TimelineBar" PlayerScreen.kt` — they receive `markers` from the composable that holds the scene detail state. That source also needs `toPersistentList()` or already yields an `ImmutableList`.

### Module Dependency Changes

Both `:feature:home` and `:feature:player` use the `stash.android.feature` convention plugin which applies `stash.android.library`. Neither currently has an explicit `implementation(libs.kotlinx.collections.immutable)` — the library is in the catalog but not wired to any module.
[VERIFIED: `feature/home/build.gradle.kts` — only `alias(libs.plugins.stash.android.feature)`, no explicit deps]
[VERIFIED: `feature/player/build.gradle.kts` — only `stash.android.feature` + Media3 deps]

**Add to `feature/home/build.gradle.kts`:**
```kotlin
dependencies {
    implementation(libs.kotlinx.collections.immutable)
}
```

**Add to `feature/player/build.gradle.kts`:**
```kotlin
dependencies {
    // ... existing Media3 deps ...
    implementation(libs.kotlinx.collections.immutable)
}
```

### Stability Impact
`kotlinx.collections.immutable.ImmutableList<T>` is annotated `@Stable` by the library itself. Changing the field type from `List<T>` (unstable from Compose's perspective) to `ImmutableList<T>` (stable) means the Compose Compiler will consider `HomeRail`, `HomeUiState`, and the `markers` parameter stable, enabling strong-skipping for composables that take them.

The PERF-02 stability report will confirm this: after PERF-03 lands, `compileReleaseKotlin` + checking `build/compose-reports/*.txt` should show these data classes as `stable` instead of `unstable`.

### Acceptance Verification
```bash
grep -rn 'List<SceneSummary>\|List<HomeRail>\|List<Marker>' feature/ core/
# Expected: 0 hits (only ImmutableList<...> remains)
./gradlew assembleDebug
# Expected: exits 0
```

---

## A4 — LaunchedEffect Key Audit (PERF-04)

### Confirmed Sites (from file read, lines 150–186)

#### Site 1: Line 150 — `DisposableEffect(activity, rotationLocked)`
```kotlin
DisposableEffect(activity, rotationLocked) {
    val prior = activity?.requestedOrientation
    activity?.requestedOrientation = if (rotationLocked) { ... } else { ... }
    activity?.window?.addFlags(...)
    onDispose {
        activity?.requestedOrientation = prior ?: ...
        activity?.window?.clearFlags(...)
    }
}
```
- `activity`: `Activity?` obtained from `LocalContext.current as? Activity`. This ref is stable for the entire Composition — `LocalContext` does not change while the screen is composed. The `as?` cast result is a stable object reference.
- `rotationLocked`: `Boolean` via `remember { mutableStateOf(false) }`. Primitives are stable.
- **Verdict: STABLE.** Effect re-runs only when rotation lock is toggled, which is the correct behavior.

#### Site 2: Line 166 — `LaunchedEffect(resizeMode, playerView)` ⚠️ NEEDS REVIEW
```kotlin
LaunchedEffect(resizeMode, playerView) { playerView?.resizeMode = resizeMode }
```
- `resizeMode`: `Int` via `remember { mutableStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }`. Stable.
- `playerView`: `PlayerView?` via `var playerView by remember { mutableStateOf<PlayerView?>(null) }` (line 142). This is a `State<PlayerView?>` backed by `remember` — the `PlayerView` object reference is set once in the `AndroidView` factory block (line 218: `.also { playerView = it }`) and never reassigned.

**Analysis:** `AndroidView`'s factory lambda runs exactly once per composition entry (when the View is created). After that, the `update` lambda runs on recompositions but does not create a new `PlayerView`. Therefore `playerView` starts as `null` and transitions to a stable `PlayerView` reference after the first frame. Once non-null, it does not change.

The effect key `playerView` will cause the `LaunchedEffect` to re-run twice: once when `playerView` is `null` (no-op, safe) and once when it becomes the `PlayerView` instance (sets `resizeMode`, correct). Subsequently, it only re-runs when `resizeMode` changes (also correct).

**Verdict: STABLE in practice.** The `playerView` reference is set once via `remember` + `AndroidView.factory`, not re-created on recompositions. Document this verdict as `DOCUMENTED_STABLE_AFTER_FIRST_FRAME` in the plan's audit block.

#### Site 3: Line 168 — `LaunchedEffect(controlsVisible, state.isPlaying, lastInteraction, locked)`
```kotlin
LaunchedEffect(controlsVisible, state.isPlaying, lastInteraction, locked) {
    if (!locked && controlsVisible && state.isPlaying) {
        delay(3000)
        if (System.currentTimeMillis() - lastInteraction >= 2900) controlsVisible = false
    }
}
```
- `controlsVisible`: `Boolean` (`mutableStateOf`) — stable
- `state.isPlaying`: `Boolean` from `PlayerUiState` — stable
- `lastInteraction`: `Long` (`mutableLongStateOf`) — primitive, stable
- `locked`: `Boolean` (`mutableStateOf`) — stable
- **Verdict: STABLE.** All keys are primitives or booleans from state.

#### Site 4: Line 175 — `LaunchedEffect(stepLeft?.generation)`
```kotlin
LaunchedEffect(stepLeft?.generation) {
    if (stepLeft != null) { delay(800); stepLeft = null }
}
```
- `stepLeft?.generation`: `Long?` — null-safe access on a `Long` counter field. Null when no step is pending; a monotonically-incrementing `Long` when a step fires.
- **Verdict: STABLE.** `Long?` is a boxed primitive; value identity is well-defined for `Long` in Kotlin (value equality used as key).

#### Site 5: Line 181 — `LaunchedEffect(stepRight?.generation)`
Same pattern as Site 4 for `stepRight`. **Verdict: STABLE.**

### Summary Table for Plan
| Line | Type | Keys | Verdict |
|------|------|------|---------|
| 150 | DisposableEffect | `activity, rotationLocked` | STABLE |
| 166 | LaunchedEffect | `resizeMode, playerView` | DOCUMENTED_STABLE_AFTER_FIRST_FRAME |
| 168 | LaunchedEffect | `controlsVisible, state.isPlaying, lastInteraction, locked` | STABLE |
| 175 | LaunchedEffect | `stepLeft?.generation` | STABLE |
| 181 | LaunchedEffect | `stepRight?.generation` | STABLE |

### Required Plan Output
PERF-04 acceptance requires a code-review comment block in SUMMARY.md documenting each site with key type, verdict, and rationale. No code changes are mandatory unless Site 2 analysis reveals a regression risk (it does not, per the analysis above).

---

## A5 — applyVideoFrameRate Relocation (PERF-09)

### Current State (confirmed via file read, lines 142, 209–224, 1170–1180)

The `playerView` state variable:
```kotlin
// Line 142
var playerView by remember { mutableStateOf<PlayerView?>(null) }
```

The `AndroidView` block (lines 209–224):
```kotlin
AndroidView(
    modifier = Modifier.fillMaxSize(),
    factory = { ctx ->
        PlayerView(ctx)
            .apply {
                useController = false
                player = viewModel.player
                setKeepContentOnPlayerReset(true)
                this.resizeMode = resizeMode
            }.also { playerView = it }          // sets the remembered ref
    },
    update = {
        it.player = viewModel.player
        applyVideoFrameRate(it, state.videoFrameRate)   // LINE 222 — TARGET
    },
)
```

The function (lines 1170–1180):
```kotlin
private fun applyVideoFrameRate(playerView: PlayerView, fps: Float?) {
    if (fps == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
    val videoView = playerView.videoSurfaceView ?: return
    val surface = (videoView as? SurfaceView)?.holder?.surface
    runCatching {
        surface?.setFrameRate(fps, Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE)
    }
}
```

### Problem
`AndroidView(update = { ... })` is called on every Compose recomposition that touches the `AndroidView`'s parent. `applyVideoFrameRate` calls `Surface.setFrameRate()` on every recomposition — not only when `state.videoFrameRate` changes. While `setFrameRate` is documented as idempotent, it is still a JNI call to the Surface compositor on every recomposition.

### Target State
Remove `applyVideoFrameRate` from `AndroidView.update`. Add a `LaunchedEffect(state.videoFrameRate)` keyed on the FPS value so it fires only when the video's reported frame rate changes:

```kotlin
// AFTER: AndroidView — update block simplified
AndroidView(
    modifier = Modifier.fillMaxSize(),
    factory = { ctx ->
        PlayerView(ctx)
            .apply {
                useController = false
                player = viewModel.player
                setKeepContentOnPlayerReset(true)
                this.resizeMode = resizeMode
            }.also { playerView = it }
    },
    update = {
        it.player = viewModel.player
        // applyVideoFrameRate removed — moved to LaunchedEffect below
    },
)

// ADD: Fires only when videoFrameRate actually changes
LaunchedEffect(state.videoFrameRate) {
    playerView?.let { applyVideoFrameRate(it, state.videoFrameRate) }
}
```

### playerView Reference Availability
`playerView` is already a `remember { mutableStateOf<PlayerView?>(null) }` variable in `PlayerScreen`'s top-level composable scope (line 142). It is visible to any code in `PlayerScreen`'s composition scope, including a `LaunchedEffect` at the same scope level as the `AndroidView`. **No hoisting required.** The `playerView` ref is readable directly from the new `LaunchedEffect`.

The `LaunchedEffect(state.videoFrameRate)` block reads `playerView` at launch time (not at composition time). Since `playerView` is always set before `state.videoFrameRate` can change (the `AndroidView` factory runs on first composition, before any video begins playing), the read ordering is safe.

### Pitfall: Null Safety
`playerView` is initially `null` (before the `AndroidView.factory` runs). The `LaunchedEffect` may run before the factory if `state.videoFrameRate` is already set when the composable enters. The null-safe `playerView?.let { ... }` guards against this. This is the same null-safe pattern already used in the existing `LaunchedEffect(resizeMode, playerView)` at line 166.

### Acceptance Verification
```bash
grep -n 'applyVideoFrameRate' feature/player/src/main/java/io/stashapp/android/feature/player/PlayerScreen.kt
# Must appear in LaunchedEffect block only, NOT in AndroidView(update = ...) block
./gradlew assembleDebug
```

---

## A6 — Baseline Profile Expansion (PERF-05)

### Current State (confirmed via file read)
`StashBaselineProfileGenerator.kt` has ONE `@Test fun generate()` that covers:
1. Cold start + first draw wait
2. Library grid scroll (3 swipes via `By.scrollable(true)`)
3. First card tap → back (Detail open, but only finds first clickable item — not reliably a scene card)

The SPEC.md notes this as "cold start + library-grid scroll only" — the detail tap is incidental and may not hit the intended screen.

### Target State
Expand to ≥ 4 distinct, named journeys. The SPEC.md locks the 4 required journeys: cold start, Home rails scroll, Detail open, Player start.

**Recommended structure:** Keep a single `rule.collect { ... }` block (multiple `rule.collect` calls per test method are allowed but add overhead) OR split into multiple `@Test` methods (preferred — isolates journeys, cleaner profile attribution).

### Journey 1 — Cold Start + Library Grid (keep existing, refine)
Already implemented. Minor fix: the "first card tap" at line 63 should target a scene card specifically, not `By.clickable(true).firstOrNull()`:
```kotlin
// More reliable scene card targeting
val firstScene = device.findObject(
    By.res(TARGET_PACKAGE, "scene_card")  // tag added to SceneCard composable
) ?: device.findObjects(By.clickable(true)).firstOrNull()
```
[ASSUMED: SceneCard composable has a `testTag("scene_card")` or similar. If not, the `clickable` fallback is the existing behavior. Risk: LOW — planner should check SceneCard for existing test tags.]

### Journey 2 — Home Rails Scroll (NEW)
```kotlin
@Test
fun homeRailsScroll() = rule.collect(packageName = TARGET_PACKAGE) {
    pressHome()
    startActivityAndWait()

    // Navigate to Home tab — assumes bottom nav bar with Home tab
    val homeTab = device.wait(
        Until.findObject(By.res(TARGET_PACKAGE, "nav_home")),
        3_000,
    )
    homeTab?.click()
    device.waitForIdle()

    // Scroll each visible horizontal rail
    val rails = device.findObjects(By.scrollable(true))
    rails.take(4).forEach { rail ->
        rail.scroll(Direction.RIGHT, 0.8f)
        device.waitForIdle()
        rail.scroll(Direction.LEFT, 0.5f)
        device.waitForIdle()
    }
}
```
[ASSUMED: Home tab has a resource ID `nav_home`. Risk: LOW — if not, fall back to `By.desc("Home")` or positional click. Planner must verify tab resource IDs.]

### Journey 3 — Detail Open (NEW)
```kotlin
@Test
fun detailOpen() = rule.collect(packageName = TARGET_PACKAGE) {
    pressHome()
    startActivityAndWait()

    device.wait(Until.hasObject(By.scrollable(true)), 3_000)
    device.waitForIdle()

    // Tap any scene card to open Detail screen
    val firstCard = device.findObjects(By.clickable(true))
        .firstOrNull { it.className?.contains("View") == true }
    firstCard?.click()
    device.wait(Until.hasObject(By.pkg(TARGET_PACKAGE).depth(0)), 3_000)
    device.waitForIdle()

    // Back out
    device.pressBack()
    device.waitForIdle()
}
```

### Journey 4 — Player First Frame (NEW)
```kotlin
@Test
fun playerFirstFrame() = rule.collect(packageName = TARGET_PACKAGE) {
    pressHome()
    startActivityAndWait()

    device.wait(Until.hasObject(By.scrollable(true)), 3_000)

    // Open a scene detail
    val firstCard = device.findObjects(By.clickable(true)).firstOrNull()
    firstCard?.click()
    device.wait(Until.hasObject(By.pkg(TARGET_PACKAGE).depth(0)), 3_000)
    device.waitForIdle()

    // Tap Play button to start player
    val playButton = device.wait(
        Until.findObject(By.res(TARGET_PACKAGE, "btn_play").clickable(true)),
        3_000,
    )
    playButton?.click()

    // Wait for player surface to appear — probes ExoPlayer + Surface composition
    device.wait(
        Until.hasObject(By.pkg(TARGET_PACKAGE).depth(0)),
        5_000,
    )
    device.waitForIdle()

    device.pressBack()
    device.waitForIdle()
}
```
[ASSUMED: Detail screen has a play button with resource ID `btn_play`. Risk: MEDIUM — if the button has a different ID or is content-description-only, the journey silently no-ops. Planner must verify the play button's test tag or content description in DetailScreen.kt.]

### UiAutomator Patterns Used
| Pattern | Purpose |
|---------|---------|
| `By.scrollable(true)` | Finds any scrollable container (LazyRow/LazyColumn rendered as RecyclerView-equivalent) |
| `By.res(packageName, resourceId)` | Finds view by testTag (Compose testTag maps to resource-id in UiAutomator) |
| `By.clickable(true)` | Finds all clickable items — broad fallback |
| `By.pkg(packageName).depth(0)` | Confirms app window is present |
| `device.wait(Until.findObject(...), timeout)` | Waits up to timeout ms for element to appear |
| `device.waitForIdle()` | Waits for app to become idle between steps |
| `Direction.DOWN/RIGHT/LEFT` | Scroll direction enum from `androidx.test.uiautomator.Direction` |

[VERIFIED: `StashBaselineProfileGenerator.kt` already imports `By`, `Until`, `Direction` — patterns are consistent with existing usage]

### Graceful Failure Strategy
Per D-07: if a journey fails due to missing server connection (the Connection screen is shown instead of Library/Home), do NOT fail the test. Structure each journey with a guard:
```kotlin
val isConnected = device.wait(Until.hasObject(By.scrollable(true)), 2_000) != null
if (!isConnected) return@collect   // graceful skip
```

---

## A7 — Shuffle Bug Investigation (PERF-08)

### ViewModel Lifecycle (confirmed via file reads)

`PlayerScreen` uses `hiltViewModel()` (line 123). In Jetpack Navigation Compose, `hiltViewModel()` scopes the ViewModel to the `NavBackStackEntry`. Confirmed route registration in `MainActivity.kt` (line 424):
```kotlin
composable(Routes.Player, arguments = ...) {
    PlayerScreen(onExit = { navController.popBackStack() })
}
```
`it` here is the `NavBackStackEntry` lambda receiver. `hiltViewModel()` inside `PlayerScreen` gets the ViewModel scoped to this entry.

**Key finding:** `PlayerViewModel` is **recreated** on each navigation to `Routes.Player`. When the user taps Back (`navController.popBackStack()`) and navigates to a new scene, a fresh `PlayerViewModel` is created with a fresh `ExoPlayer by lazy`. The `onCleared()` lifecycle of the previous VM fires when the previous `NavBackStackEntry` is removed from the back stack.

### Listener Accumulation Analysis

`PlayerViewModel` (confirmed lines 95–104):
```kotlin
val player: ExoPlayer by lazy {
    StashPlayerFactory(...).build().also { p ->
        p.addListener(playerListener)   // Called ONCE, on first player access
        p.playWhenReady = true
    }
}
```

`onCleared()` (confirmed lines 442–449):
```kotlin
override fun onCleared() {
    positionTicker?.cancel()
    flushActivityToServer(final = true)
    player.removeListener(playerListener)   // Removes the ONE listener added above
    player.release()
    super.onCleared()
}
```

**Conclusion:** Within a single `PlayerViewModel` instance, `addListener` is called exactly once (lazy init) and `removeListener` is called exactly once (onCleared). No accumulation is possible within one ViewModel lifecycle.

**However:** There is a timing risk. `player.release()` is called after `removeListener` in `onCleared()`. If `onCleared()` is called while a coroutine in `viewModelScope` is mid-flight (e.g., `loadAndPlay` at line 252 is awaiting `sceneRepository.scene(...)`), the coroutine will be cancelled by `viewModelScope` before completion. This is safe because `viewModelScope` is cancelled before `onCleared()` returns.

### Actual Shuffle Hang Hypothesis

The current `playScene()` function (lines 330–368) calls:
```kotlin
player.setMediaItem(item)
player.prepare()
seekTo?.let { player.seekTo(it) }
player.play()
```
`setMediaItem` replaces the current media but does **not** reset the player's `STATE_ENDED` flag until `prepare()` completes. If `onSceneEnded()` (line 370) fires and calls `queue.advance() → loadAndPlay()` before the previous `playScene()` finishes its `prepare()` cycle, two `setMediaItem + prepare` calls may be in flight simultaneously on the same player instance.

**More likely cause: Queue exhaustion.** `queue.advance()` returns `null` when the queue is exhausted (last item). `onSceneEnded()` calls `val next = queue.advance() ?: return` — if `null`, returns without scheduling the next scene. With shuffle, the last item in the shuffled order triggers `STATE_ENDED`, `advance()` returns null, and playback stops with no UI indication. If `cycleRepeat()` is set to `RepeatMode.OFF`, this is "working as intended" but feels like a hang.

**Third candidate: Listener on multiple instances.** If the user navigates to `PlayerScreen` while the previous `PlayerViewModel.onCleared()` has not yet been called (e.g., during the NavBackStackEntry transition), two `PlayerViewModel` instances briefly coexist. Each has its own `ExoPlayer` instance; they do not share listeners. This is not a listener accumulation issue.

### Investigation Order (per D-06)
1. **Reproduce the hang:** Navigate to player with shuffle queue of ≥ 10 items. Play through to end. Document whether hang is (a) player stops at last item, (b) player freezes mid-video, or (c) player stutters between items.
2. **Check `RepeatMode`:** If hang happens at last item with `RepeatMode.OFF`, it is the queue exhaustion behavior described above. Fix: add `RepeatMode.ALL` logic to `onSceneEnded()` when `advance()` returns null and shuffle is active (wrap around). Or document as expected behavior.
3. **Listener count check:** Use Android Profiler > Memory > Heap dump during shuffle. Search for `PlayerListener` instances. Expected: exactly 1. If > 1 after several scene transitions, listener accumulation is occurring.
4. **Memory profiler:** Run 10-video shuffle session with Android Profiler "Record Java heap" mode. Check for monotonic growth in `MediaSource`, `C2SoftAvcDec`, or `ExoPlayer` instances.

### Required Artifact
Regardless of outcome: `.planning/benchmarks/perf-08-shuffle-profile.{png|txt}` must be committed. A profiler screenshot showing listener count = 1 and flat heap is sufficient. If the root cause is queue exhaustion + `RepeatMode.OFF`, the artifact is a screen recording or text description with line references.

### Code Path for Potential Fix (queue exhaustion + wrap)
In `onSceneEnded()` (line 370):
```kotlin
private fun onSceneEnded() {
    val next = queue.advance() ?: return    // Current: stops here when exhausted
    loadAndPlay(next, autoResume = false)
}
```
If `queue.snapshot().repeatMode == RepeatMode.ALL` and `queue.advance()` returns null, reset to index 0:
```kotlin
private fun onSceneEnded() {
    val next = queue.advance()
    if (next == null) {
        // If repeat-all is active, wrap around
        if (_state.value.queue?.repeatMode == RepeatMode.ALL) {
            val first = queue.reset()  // PlayerQueue would need a reset() method
            first?.let { loadAndPlay(it, autoResume = false) }
        }
        return
    }
    loadAndPlay(next, autoResume = false)
}
```
[ASSUMED: `PlayerQueue` has or needs a `reset()` method to return to index 0. Planner must read `PlayerQueue.kt` to confirm. Risk: MEDIUM — if no `reset()` exists, this is a new method to add or work around differently.]

---

## A8 — Macrobench Infrastructure (PERF-06, PERF-07)

### Class Structure

Both macrobenchmark classes go in the `baselineprofile` module alongside `StashBaselineProfileGenerator.kt`. They use `MacrobenchmarkRule` (not `BaselineProfileRule`).

The `benchmark-macro-junit4` artifact is already in the module:
```kotlin
// baselineprofile/build.gradle.kts
implementation(libs.androidx.benchmark.macro.junit4)  // already present
```
[VERIFIED: `baselineprofile/build.gradle.kts` line 51]

### ColdStartBenchmark (PERF-06)

```kotlin
// baselineprofile/src/main/java/io/stashapp/android/baselineprofile/ColdStartBenchmark.kt
package io.stashapp.android.baselineprofile

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ColdStartBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun startupWithProfile() = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(StartupTimingMetric()),
        compilationMode = CompilationMode.Partial(
            baselineProfileMode = BaselineProfileMode.Require,
            warmupIterations = 3,
        ),
        startupMode = StartupMode.COLD,
        iterations = 5,
    ) {
        pressHome()
        startActivityAndWait()
    }

    @Test
    fun startupWithoutProfile() = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(StartupTimingMetric()),
        compilationMode = CompilationMode.None,
        startupMode = StartupMode.COLD,
        iterations = 5,
    ) {
        pressHome()
        startActivityAndWait()
    }

    private companion object {
        const val TARGET_PACKAGE = "io.stashapp.android"
    }
}
```

**`CompilationMode.Partial(BaselineProfileMode.Require)`** — installs the baseline profile before measurement; fails if no profile is found in the APK. Use this to measure the WITH-profile case.

**`CompilationMode.None`** — disables AOT; JIT only. Use this as the baseline (WITHOUT-profile). Represents a fresh install without profile warmup.

[VERIFIED: Context7 — `CompilationMode.Partial(baselineProfileMode = BaselineProfileMode.Require, warmupIterations = 3)` confirmed from `benchmark-macro/api/restricted_1.1.0-beta01.txt`]
[VERIFIED: developer.android.com macrobenchmark docs — `StartupTimingMetric`, `StartupMode.COLD`, `MacrobenchmarkRule` confirmed]

### LibraryScrollBenchmark (PERF-07)

```kotlin
// baselineprofile/src/main/java/io/stashapp/android/baselineprofile/LibraryScrollBenchmark.kt
package io.stashapp.android.baselineprofile

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibraryScrollBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun libraryScroll() = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(
            baselineProfileMode = BaselineProfileMode.Require,
            warmupIterations = 3,
        ),
        startupMode = StartupMode.WARM,
        iterations = 5,
    ) {
        startActivityAndWait()
        val grid = device.wait(Until.findObject(By.scrollable(true)), 3_000) ?: return@measureRepeated
        repeat(5) {
            grid.scroll(Direction.DOWN, 0.8f)
            device.waitForIdle()
        }
        repeat(5) {
            grid.scroll(Direction.UP, 0.8f)
            device.waitForIdle()
        }
    }

    private companion object {
        const val TARGET_PACKAGE = "io.stashapp.android"
    }
}
```

**`FrameTimingMetric`** captures frame timing data. The SPEC.md acceptance criterion is ≥ 95% frames on time at p95. This maps to the `frameOverrunMs` and `frameDurationCpuMs` columns in the macrobench output.

**`StartupMode.WARM`** — app process is already running but activity is recreated between iterations. More representative of a user returning to the library, and more stable than COLD for frame timing measurements.

### Benchmark Output Format
Raw stdout from `./gradlew :baselineprofile:pixel6Api34BenchmarkAndroidTest` looks like:
```
benchmark:    50,123 ns   startupWithProfile
benchmark:    62,456 ns   startupWithoutProfile
benchmark:    frameDurationCpuMs P50  8.3,  P90 12.1,  P95 14.7,  P99 22.3   libraryScroll
```
Commit the raw stdout (no trimming) to `.planning/benchmarks/perf-06-cold-start.txt` and `.planning/benchmarks/perf-07-library-scroll.txt`. The SPEC.md acceptance check reads the p50 ratio from these files.

### CompilationMode Decision Table
| Mode | When to Use | What it Measures |
|------|------------|-----------------|
| `CompilationMode.Partial(Require)` | WITH-profile case | Performance WITH baseline profile installed |
| `CompilationMode.None` | WITHOUT-profile baseline | Raw JIT performance — worst case |
| `CompilationMode.Full` | Not needed for Phase 3 | Full AOT — unrealistic; skipped |

[VERIFIED: Context7 androidx macrobenchmark — `CompilationMode.Partial`, `CompilationMode.None` confirmed]

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Frame timing measurement | Custom Choreographer listener | `FrameTimingMetric` | Handles vsync math, jank detection, p-value bucketing |
| Cold start measurement | `System.currentTimeMillis()` delta | `StartupTimingMetric` | Measures `reportFullyDrawn` / `ActivityTaskManager` timestamps, not wall-clock |
| Stable collection type | Custom `@Immutable` wrapper | `kotlinx.collections.immutable.ImmutableList` | Already `@Stable` annotated; recognized by Compose compiler; no annotation boilerplate |
| GMD system image management | Manual AVD creation scripts | `managedDevices` DSL | AGP handles download, creation, cleanup automatically |
| Baseline profile generation trigger | Manual `cmd package compile` | `./gradlew :app:generateBaselineProfile` | Plugin handles profile install and variant routing |

---

## Common Pitfalls

### Pitfall 1: GMD inside `testOptions { }` wrapper is required
**What goes wrong:** Placing `managedDevices { ... }` directly inside `android { }` without the `testOptions { }` wrapper causes an unresolved reference at Gradle sync.
**Prevention:** The DSL path is `android.testOptions.managedDevices.localDevices`. Always nest inside `testOptions`.
[VERIFIED: developer.android.com/studio/test/gradle-managed-devices]

### Pitfall 2: `baselineProfile.managedDevices` uses the device name string, not the task name
**What goes wrong:** Writing `managedDevices += "pixel6Api34BenchmarkAndroidTest"` (the full task name) instead of `managedDevices += "pixel6Api34"` (the device declaration name).
**Prevention:** The `managedDevices` list in the `baselineProfile { }` block takes the `create(...)` argument string, not the generated task name.

### Pitfall 3: `ComposeCompilerGradlePluginExtension` import in convention plugin
**What goes wrong:** Writing `composeCompiler { ... }` DSL in the convention plugin without the explicit `extensions.configure<ComposeCompilerGradlePluginExtension> { ... }` form. The `composeCompiler { }` top-level DSL extension is available in module `build.gradle.kts` files where the `kotlin.plugin.compose` plugin is applied directly, but NOT in convention plugins that apply the plugin programmatically via `pluginManager.apply(...)`.
**Prevention:** In `AndroidComposeConventionPlugin.kt`, always use `extensions.configure<ComposeCompilerGradlePluginExtension> { ... }` with the explicit type. The `plugin.compose` artifact is `compileOnly` in `build-logic/convention/build.gradle.kts`, so the type is available.

### Pitfall 4: `stabilityConfigurationFile` must point to an existing file
**What goes wrong:** Setting `stabilityConfigurationFile = rootProject.layout.projectDirectory.file("compose_stability.conf")` when the file does not exist causes `compileReleaseKotlin` to fail with a FileNotFoundException.
**Prevention:** Create the file (even empty) before running the compiler. Wave 0 of Plan 3.1 should create the empty file.

### Pitfall 5: `ImmutableList` default values break the `HomeUiState.Initial` companion
**What goes wrong:** `HomeUiState.Initial` uses `HomeRailKind.entries.map { HomeRail(it) }` which returns a `List<HomeRail>`. After the migration, `HomeUiState.rails` is `ImmutableList<HomeRail>`, so this won't compile.
**Prevention:** Change to `.map { HomeRail(it) }.toPersistentList()` in the companion.

### Pitfall 6: `AndroidView.update` captures the lambda argument, not the state variable
**What goes wrong:** In the new `LaunchedEffect(state.videoFrameRate)`, reading `playerView` from the enclosing scope may capture a stale `null` if the effect is launched before the `AndroidView.factory` runs.
**Prevention:** The null-safe `playerView?.let { ... }` pattern handles this. `state.videoFrameRate` is `null` until the first video frame is decoded (after `onVideoSizeChanged` fires), and by then the `AndroidView.factory` will have run and set `playerView`. The edge case is safe.

### Pitfall 7: Macrobench with `CompilationMode.Partial(Require)` fails if profile is stale
**What goes wrong:** `BaselineProfileMode.Require` causes the benchmark to fail (not skip) if the app's APK does not contain a baseline profile. If PERF-05 (profile expansion) hasn't run before PERF-06/07, the benchmark aborts.
**Prevention:** Plans 3.2 and 3.3 both `depends_on: plan-3.1` (per D-09). Plan 3.1 includes PERF-05 profile expansion. Run benchmark tasks only after PERF-05 changes are built into a fresh APK.

---

## Code Examples

### Verified: ComposeCompilerGradlePluginExtension Configure Block
```kotlin
// Source: kotlinlang.org/api/kotlin-gradle-plugin/compose-compiler-gradle-plugin
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension

extensions.configure<ComposeCompilerGradlePluginExtension> {
    reportsDestination = layout.buildDirectory.dir("compose-reports")
    metricsDestination = layout.buildDirectory.dir("compose-metrics")
    stabilityConfigurationFile =
        rootProject.layout.projectDirectory.file("compose_stability.conf")
}
```

### Verified: GMD Declaration in android.test Module
```kotlin
// Source: developer.android.com/studio/test/gradle-managed-devices
android {
    testOptions {
        managedDevices {
            localDevices {
                create("pixel6Api34") {
                    device = "Pixel 6"
                    apiLevel = 34
                    systemImageSource = "google_apis"
                }
            }
        }
    }
}
baselineProfile {
    managedDevices += "pixel6Api34"
    useConnectedDevices = false
}
```

### Verified: ImmutableList Imports
```kotlin
// Source: github.com/Kotlin/kotlinx.collections.immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
```

### Verified: MacrobenchmarkRule Cold Start
```kotlin
// Source: Context7 /androidx/androidx — benchmark-macro docs
benchmarkRule.measureRepeated(
    packageName = TARGET_PACKAGE,
    metrics = listOf(StartupTimingMetric()),
    compilationMode = CompilationMode.Partial(
        baselineProfileMode = BaselineProfileMode.Require,
        warmupIterations = 3,
    ),
    startupMode = StartupMode.COLD,
    iterations = 5,
) {
    pressHome()
    startActivityAndWait()
}
```

---

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| `google_apis` system image (API 34) | PERF-01 GMD | Unknown | — | `aosp` image; or REVIEWS-C4 ACCEPT per D-01a |
| `benchmark-macro-junit4` library | PERF-06/07 macrobench | Yes (in build.gradle.kts) | 1.3.3 | — |
| `kotlinx-collections-immutable` | PERF-03 | Yes (in catalog, not yet wired) | 0.4.0 | — |
| `androidx-test-uiautomator` | PERF-05 profile journeys | Yes (in build.gradle.kts) | 2.3.0 | — |
| Stash server (for profile generation) | PERF-05 detailed journeys | Unknown | — | Journeys guard with `hasObject` check; degrade gracefully |

**Note on system image availability:** The `google_apis` image for API 34 (x86_64) requires ~6GB disk space and the machine needs the Android SDK command-line tools. If the download fails, `aosp` is the fallback per Claude's Discretion. The GMD code change lands regardless.

---

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | `MacrobenchmarkRule` (benchmark-macro-junit4 1.3.3) + `BaselineProfileRule` |
| Config file | None — the `baselineprofile` module is `com.android.test`, no separate test runner config |
| Quick run command | `./gradlew :baselineprofile:assembleDebug` (compiles; does not run on device) |
| Full suite command | `./gradlew :baselineprofile:pixel6Api34BenchmarkAndroidTest` (requires GMD) |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| PERF-01 | GMD task exits 0 | Smoke (Gradle task) | `./gradlew :baselineprofile:pixel6Api34BenchmarkAndroidTest` | Config change only |
| PERF-02 | Reports file exists after compileReleaseKotlin | Smoke (shell) | `find . -path "*/build/compose-reports/*.txt" \| wc -l` | Config change only |
| PERF-03 | No `List<SceneSummary\|HomeRail\|Marker>` remaining | Compile + grep | `./gradlew assembleDebug && grep -rn 'List<SceneSummary>...' feature/` | Code change |
| PERF-04 | All 5 sites documented in SUMMARY.md | Manual review | Code review | Documentation |
| PERF-05 | ≥ 4 journeys in generator file | Code review + profile gen | `./gradlew :app:generateBaselineProfile` | Code change |
| PERF-06 | p50 improvement ≥ 5% | Macrobench (GMD) | `./gradlew :baselineprofile:pixel6Api34BenchmarkAndroidTest` | New file |
| PERF-07 | ≥ 95% frames on time at p95 | Macrobench (GMD) | `./gradlew :baselineprofile:pixel6Api34BenchmarkAndroidTest` | New file |
| PERF-08 | Profiling artifact committed | Manual profiling session | Android Profiler (manual) | New artifact |
| PERF-09 | `applyVideoFrameRate` in LaunchedEffect only | Grep | `grep -n 'applyVideoFrameRate' PlayerScreen.kt` | Code change |
| PERF-10 | `.planning/benchmarks/` non-empty | Shell | `ls .planning/benchmarks/` | Directory + files |

### Wave 0 Gaps
- [ ] `.planning/benchmarks/` directory — create before Plan 3.3 runs
- [ ] `compose_stability.conf` at project root — create empty file in Plan 3.1 Wave 0
- [ ] `ColdStartBenchmark.kt` — new file, Wave 0 of Plan 3.3
- [ ] `LibraryScrollBenchmark.kt` — new file, Wave 0 of Plan 3.3

---

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | An empty `compose_stability.conf` will not cause `compileReleaseKotlin` to fail | A2 | LOW — if wrong, delete the `stabilityConfigurationFile` line; reports still work |
| A2 | `SceneCard` composable has a `testTag("scene_card")` for UiAutomator targeting | A6 | LOW — fallback to `By.clickable(true)` is the existing behavior |
| A3 | Home tab has resource ID `nav_home` for UiAutomator targeting | A6 | MEDIUM — if wrong, Home tab journey navigation fails silently; use `By.desc("Home")` fallback |
| A4 | Detail screen has a play button with resource ID `btn_play` | A6 | MEDIUM — if wrong, player first-frame journey no-ops; planner must verify DetailScreen.kt |
| A5 | `PlayerQueue` has or can be given a `reset()` method for RepeatMode.ALL wrap | A7 | MEDIUM — if not present, the shuffle wrap-around fix requires adding a new method |

---

## Sources

### Primary (HIGH confidence)
- Live codebase reads — `baselineprofile/build.gradle.kts`, `AndroidComposeConventionPlugin.kt`, `HomeViewModel.kt`, `PlayerScreen.kt` (lines 120–230, 470–510, 990–1010, 1160–1200), `PlayerViewModel.kt` (full), `StashBaselineProfileGenerator.kt`, `build-logic/convention/build.gradle.kts`, `gradle/libs.versions.toml`, `feature/home/build.gradle.kts`, `feature/player/build.gradle.kts`, `MainActivity.kt` (lines 410–425)
- [developer.android.com/studio/test/gradle-managed-devices](https://developer.android.com/studio/test/gradle-managed-devices) — GMD DSL syntax, task naming, systemImageSource values
- [developer.android.com/topic/performance/baselineprofiles/create-baselineprofile](https://developer.android.com/topic/performance/baselineprofiles/create-baselineprofile) — baselineProfile + managedDevices co-location in test module
- [kotlinlang.org/api/kotlin-gradle-plugin/.../ComposeCompilerGradlePluginExtension](https://kotlinlang.org/api/kotlin-gradle-plugin/compose-compiler-gradle-plugin/org.jetbrains.kotlin.compose.compiler.gradle/-compose-compiler-gradle-plugin-extension/) — all extension properties with types

### Secondary (MEDIUM confidence)
- Context7 `/androidx/androidx` — `CompilationMode.Partial(BaselineProfileMode.Require)`, `CompilationMode.None`, `MacrobenchmarkRule` patterns
- [developer.android.com macrobenchmark overview](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview) — `StartupTimingMetric`, `FrameTimingMetric`, `StartupMode`, `measureRepeated` structure

### Tertiary (LOW confidence — marked ASSUMED above)
- UiAutomator resource ID names (`nav_home`, `btn_play`, `scene_card`) — inferred from common patterns; must be verified against actual composable test tags in DetailScreen.kt and HomeScreen.kt

---

## Metadata

**Confidence breakdown:**
- GMD setup (A1): HIGH — DSL syntax verified against official docs; current state confirmed via file read
- Compose compiler extension (A2): HIGH — property names/types verified from official API reference; import availability confirmed via build-logic classpath
- ImmutableList migration (A3): HIGH — all target sites confirmed by file read; catalog entry confirmed; dependency gap confirmed
- LaunchedEffect audit (A4): HIGH — all 5 sites confirmed by file read; stability analysis is deterministic given the code structure
- applyVideoFrameRate relocation (A5): HIGH — current and target code confirmed by file read; playerView ref availability confirmed
- Baseline profile expansion (A6): MEDIUM — UiAutomator patterns verified; specific resource IDs for tabs/buttons are ASSUMED
- Shuffle investigation (A7): HIGH for ViewModel lifecycle analysis (confirmed `hiltViewModel()` + `NavBackStackEntry` scoping); MEDIUM for root cause (queue exhaustion hypothesis is well-supported but not profiler-confirmed)
- Macrobench infrastructure (A8): HIGH — class structure verified against Context7 and official docs; benchmark library already in module

**Research date:** 2026-05-18
**Valid until:** 2026-06-18 (stable APIs; Kotlin 2.2.x and AGP 8.7.x are not moving in the next 30 days given the project's stable-only policy)
