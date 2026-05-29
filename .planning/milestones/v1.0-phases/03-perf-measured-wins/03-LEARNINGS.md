---
phase: 3
phase_name: "PERF — Measured Wins"
project: "slopper"
generated: "2026-05-19"
counts:
  decisions: 7
  lessons: 9
  patterns: 5
  surprises: 6
missing_artifacts: []
---

# Phase 3 Learnings: PERF — Measured Wins

## Decisions

### Compose Compiler Extension Properties Use `.set()` Not `=` in Convention Plugins
In `AndroidComposeConventionPlugin.kt`, `ComposeCompilerGradlePluginExtension` properties are Gradle lazy types (`DirectoryProperty`, `RegularFileProperty`, `ListProperty`). Use `.set(...)` to assign them, not direct `=` assignment.

**Rationale:** Direct `=` assignment on a `DirectoryProperty` causes "Val cannot be reassigned" at compile time. The Gradle lazy configuration API requires `.set()` as the mutation method. This applies to all convention plugins that configure Kotlin/Compose compiler extensions programmatically via `extensions.configure<T>`.
**Source:** 03.1-SUMMARY.md DEV-01; 03.1-PLAN.md Task 2

---

### `compose_stability.conf` Must Be Truly Empty — Comments Are Parse Errors
Create `compose_stability.conf` as an empty file (or single newline). Do not add `#` comment lines.

**Rationale:** KGP 2.2.20's Compose Compiler plugin throws `Error parsing stability configuration file on line 0` if the file contains `#` comment lines. The file format does not support comments. An empty file is the safe default; explicit stable-class declarations are added only when needed.
**Source:** 03.1-SUMMARY.md DEV-02

---

### RepeatMode.OFF Queue Exhaustion Is Expected Behavior — Fix at the UX Layer
When a shuffled or sequential queue reaches the last item with `RepeatMode.OFF`, `PlayerQueue.advance()` returning `null` is the correct semantic. The fix is emitting user feedback ("End of queue" banner), not changing queue logic.

**Rationale:** `RepeatMode.ALL` wrap-around was already implemented correctly in `PlayerQueue.advance()`. The "hang" was a UX gap: the last video ended silently with no state update, no banner, and no navigation — leaving users uncertain whether the app had crashed. The fix (`onSceneEnded() ?: run { emitBanner(...); return }`) addresses the perception, not the behavior.
**Source:** 03.3-SUMMARY.md PERF-08; `.planning/benchmarks/perf-08-shuffle-investigation.md`

---

### ImmutableList Migration Must Cover ALL Consumers, Not Just Data Class Fields
When migrating `List<T>` → `ImmutableList<T>` in a data class, ALL composable functions that accept that type as a parameter must also be updated.

**Rationale:** Kotlin's type system enforces this at compile time — `ImmutableList<T>` is not a subtype of `List<T>` for parameter matching. `HomeScreen.kt`'s `RailScenes(scenes: List<SceneSummary>)` caused a type mismatch when called with `homeRail.scenes: ImmutableList<SceneSummary>`. Scope estimation for ImmutableList migrations must trace all call sites, not just the declaring class.
**Source:** 03.2-SUMMARY.md DEV-01

---

### GMD Task Names Are Gradle-Generated — Source File Contains 2 Occurrences, Not 3
A `grep -c 'pixel6Api34' baselineprofile/build.gradle.kts` check returns 2: the `create("pixel6Api34")` declaration and the `managedDevices += "pixel6Api34"` reference. The Gradle task names (`pixel6Api34BenchmarkAndroidTest`, `pixel6Api34Setup`) are generated at configuration time and are NOT present in the source file.

**Rationale:** Plan verification checks that count source-file occurrences of a device name must account for this. Expecting 3 occurrences (or validating task names by source grep) will always fail. Validate task existence separately with `./gradlew :baselineprofile:tasks`.
**Source:** 03-REVIEWS.md MEDIUM-1; 03.1-PLAN.md Task 1 verify block

---

### `managedDevices` Block Belongs Inside `android { testOptions { } }` — NOT at `android { }` Level
The Gradle Managed Device DSL requires `managedDevices` nested as: `android { testOptions { managedDevices { localDevices { create("...") { ... } } } } }`.

**Rationale:** Placing `managedDevices` directly inside `android { }` (without `testOptions`) causes a Gradle configuration error. The `com.android.test` module (`:baselineprofile`) uses a different DSL nesting than regular application modules for device testing configuration.
**Source:** 03.1-PLAN.md Pitfall 1; 03-RESEARCH.md A1

---

### Stability Reports Are Immediate Execution Evidence for ImmutableList Rationale
After PERF-02 lands, running `./gradlew :feature:home:compileReleaseKotlin` produces `build/compose-reports/*.txt` confirming which types are classified `unstable`. This is concrete evidence that validates the ImmutableList migration need.

**Rationale:** Wave 1 completed PERF-02 before PERF-03, and the stability reports immediately confirmed `HomeRail.scenes: List<SceneSummary>` as `unstable`. This ordering validates the wave structure: PERF-02 (stability reports) before PERF-03 (migration) provides objective evidence rather than theoretical justification.
**Source:** 03.1-SUMMARY.md; VERIFICATION.md PERF-02/03 row

---

## Lessons

### Compose Compiler Plugin in Convention Plugins Requires `extensions.configure<T>` Form
The `composeCompiler { }` top-level DSL block only works in module-level `build.gradle.kts`. In a convention plugin that programmatically applies the Kotlin Compose Compiler plugin via `pluginManager.apply(...)`, the correct form is `target.extensions.configure<ComposeCompilerGradlePluginExtension> { ... }`.

**Context:** Both forms invoke the same extension, but the convention plugin's `apply(target: Project)` context requires the `target.extensions.configure<T>` form to access the extension on the target project rather than the convention-plugin project itself.
**Source:** 03.1-PLAN.md Task 2; 03-RESEARCH.md A2

---

### `PlayerViewModel` Is Recreated Per Navigation — Listener Accumulation Is Structurally Impossible
`hiltViewModel()` scopes the ViewModel to the `NavBackStackEntry`. Each navigation to `PlayerScreen` creates a new `PlayerViewModel` instance. `addListener` at the lazy `player` initialization runs once per ViewModel instance. `onCleared()` calls `removeListener` exactly once. There is no accumulation path.

**Context:** The shuffle bug investigation's primary hypothesis (listener accumulation) was definitively ruled out by tracing the ViewModel lifecycle. Future debugging of player-related issues should eliminate listener accumulation as a hypothesis when `@HiltViewModel` scoped to `NavBackStackEntry` is the pattern.
**Source:** 03.3-SUMMARY.md PERF-08; `.planning/benchmarks/perf-08-shuffle-investigation.md`

---

### `CompilationMode.None` in benchmark-macro 1.3.3 Is an Inner Class — Requires `()`
`CompilationMode.None` is an inner class (not a Kotlin `object`), so it requires the constructor call syntax: `CompilationMode.None()` not `CompilationMode.None`.

**Context:** The IDE's type inference accepts both at completion time, but the compiler rejects the object-reference form. This would surface as a compile error only when writing macrobenchmark tests. Learned during Plan 3.3 Task 2 execution.
**Source:** 03.3-SUMMARY.md deviation (Rule 1 compile error)

---

### Lambda Shadowing: Name Outer `let` Receiver When Nesting `repeat`
`collection?.let { repeat(n) { it.doSomething() } }` — inside `repeat`, `it` is the `Int` iteration index, shadowing the outer `UiObject2 it`. Causes a type error at `.scroll(...)` call.

**Context:** The baseline profile generator's library scroll journey originally used `grid?.let { repeat(3) { it.scroll(...) } }`. The `it` inside `repeat` was the `Int` index (0, 1, 2), not the `UiObject2`. Fixed by naming the outer lambda: `grid?.let { uiObj -> repeat(3) { uiObj.scroll(...) } }`.
**Source:** 03.1-SUMMARY.md DEV-03

---

### PERF-05 Baseline Profile Output Is Implicitly Device-Dependent Despite No SPEC Annotation
The SPEC listed PERF-05 (baseline profile expansion) without explicitly marking it as device-dependent (unlike PERF-06/07 which had explicit device-dependency notes). However, generating `baseline-prof.txt` requires an actual device/GMD run, making PERF-05 implicitly device-dependent.

**Context:** The SPEC acceptance criterion was `wc -l baseline-prof.txt → ≥ 20% more lines`. This check requires the profile to have been regenerated, which requires a device. Future SPEC authoring should explicitly mark acceptance criteria that require device execution, even if the code change itself is device-agnostic.
**Source:** VERIFICATION.md PERF-05 row

---

### Stability Reports Go Under `build/compose-reports/` for ALL Compose Convention Modules
After adding `reportsDestination = target.layout.buildDirectory.dir("compose-reports")` to `AndroidComposeConventionPlugin.kt`, every module that applies `stash.android.compose` generates stability reports under its own `build/compose-reports/`. Not just `:app` — all `feature/*` and `core/*` Compose modules.

**Context:** The SPEC's acceptance criterion checked `feature/home/build/compose-reports/*.txt`. In practice, reports exist for every Compose module. The grep check should use `find . -path "*/build/compose-reports/*.txt"` for completeness.
**Source:** 03.1-SUMMARY.md PERF-02 verify; VERIFICATION.md

---

### ImmutableList Migration Requires `toPersistentList()` at ALL Assignment Sites
The `HomeViewModel.kt` migration requires `toPersistentList()` at 3 sites: (1) `HomeUiState.Initial` companion, (2) `result.data.toPersistentList()` in AppResult.Success branch, (3) `updated.toPersistentList()` in `_state.update`. Missing any one causes a compile error because `List<T>.map { }` returns `List<T>`, not `ImmutableList<T>`.

**Context:** The plan initially estimated 3 sites but the count matches exactly. Lesson: when counting `toPersistentList()` sites in a ViewModel, trace every place where a `List<T>` value is assigned to a field typed `ImmutableList<T>`.
**Source:** 03.2-SUMMARY.md

---

### Shuffle "End of Queue" UX Fix Is Separate from RepeatMode.ALL Logic
The queue exhaustion fix (`onSceneEnded() ?: run { banner; return }`) only applies to `RepeatMode.OFF`. `RepeatMode.ALL` wrap-around was already implemented in `PlayerQueue.advance()` and required no changes.

**Context:** This distinction matters for future testing: a 10-video shuffle with `RepeatMode.ALL` should wrap seamlessly; with `RepeatMode.OFF` it should stop and show the banner. Both branches need separate test coverage when POLISH-04/05 lands.
**Source:** 03.3-SUMMARY.md; `.planning/benchmarks/perf-08-shuffle-investigation.md`

---

### ImmutableList Dependency Needed in Feature Modules, NOT Core Modules
`kotlinx-collections-immutable` dependency must be added to the feature modules that USE `ImmutableList` in composable parameters (`:feature:home`, `:feature:player`). The data class fields in `core/model` are NOT migrated (per SPEC constraint), so no core module dependency is needed.

**Context:** The dependency graph flows `app → feature → core`. Adding the dependency to `core/model` would be wrong (it's not using ImmutableList). Feature modules that have `ImmutableList<T>` in their composable parameters need the dependency in their own `build.gradle.kts`.
**Source:** 03.2-PLAN.md Task 1; 03.2-SUMMARY.md

---

## Patterns

### Convention Plugin Lazy Property Assignment Pattern
When configuring a Kotlin/Android/Compose Gradle extension in a convention plugin, always use `.set()` for `Property<T>`, `DirectoryProperty`, `RegularFileProperty`, and `.add()` for `ListProperty<T>`:

```kotlin
target.extensions.configure<ComposeCompilerGradlePluginExtension> {
    reportsDestination.set(target.layout.buildDirectory.dir("compose-reports"))
    stabilityConfigurationFiles.add(
        target.rootProject.layout.projectDirectory.file("compose_stability.conf")
    )
}
```

**When to use:** Any convention plugin that configures a Gradle extension with lazy-configuration properties (anything that extends `Property<T>`, `DirectoryProperty`, or `RegularFileProperty`).
**Source:** 03.1-SUMMARY.md DEV-01; 03.1-PLAN.md Task 2

---

### Queue-Exhaustion UX Fix Pattern
When a sequential/shuffled queue reaches its end with no-repeat mode, provide explicit user feedback instead of silent stop:

```kotlin
// BEFORE
val next = queue.advance() ?: return
// AFTER
val next = queue.advance() ?: run {
    _state.update { it.copy(banner = "End of queue") }
    return
}
```

**When to use:** Any media player or sequential queue that can reach a natural end state. The silent return is semantically correct; the UX fix makes the terminal state visible.
**Source:** 03.3-SUMMARY.md PERF-08; `PlayerViewModel.kt` `onSceneEnded()`

---

### Named-Lambda Pattern for Nested Iteration on UiObject2
When calling a method on a `UiObject2` inside a `repeat()` block, always name the outer `let` lambda parameter to avoid `it` shadowing:

```kotlin
// WRONG — it inside repeat is Int, not UiObject2
scrollable?.let { repeat(3) { it.scroll(Direction.DOWN, 0.8f) } }

// CORRECT
scrollable?.let { uiObj ->
    repeat(3) { uiObj.scroll(Direction.DOWN, 0.8f) }
}
```

**When to use:** Any UiAutomator journey code with nested `repeat` blocks on a captured UI element.
**Source:** 03.1-SUMMARY.md DEV-03

---

### Stability-Reports-First ImmutableList Migration Workflow
Order of operations for ImmutableList migrations: (1) land stability reports (PERF-02), (2) run `compileReleaseKotlin` to get concrete `unstable` classifications, (3) migrate the types that are confirmed unstable.

```bash
# After landing stability reports:
./gradlew :feature:home:compileReleaseKotlin
find feature/home/build/compose-reports -name "*.txt" -exec grep "unstable" {} \;
# Use output as evidence in SUMMARY.md / PR description
```

**When to use:** Any phase that requires ImmutableList or @Stable migration. The reports turn a theoretical optimization into a measured finding.
**Source:** VERIFICATION.md; 03.2-SUMMARY.md context

---

### Macrobench Class Structure Pattern (benchmark-macro 1.3.3)
Correct syntax for macrobenchmark classes in `baselineprofile` module:

```kotlin
@RunWith(AndroidJUnit4::class)
class ColdStartBenchmark {
    @get:Rule val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun coldStartWithProfile() = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(StartupTimingMetric()),
        compilationMode = CompilationMode.Partial(BaselineProfileMode.Require),  // not Partial{}
        startupMode = StartupMode.COLD,
        iterations = 5,
    ) { pressHome(); startActivityAndWait() }
}
private const val TARGET_PACKAGE = "io.stashapp.android.debug"
```

Key: `CompilationMode.None()` requires `()` (inner class). `CompilationMode.Partial(...)` takes a `BaselineProfileMode` argument.
**When to use:** Any macrobenchmark class in a `:baselineprofile` module on AGP 8.7.x.
**Source:** 03.3-SUMMARY.md deviation; 03.3-PLAN.md Task 2

---

## Surprises

### Shuffle Bug Was NOT Listener Accumulation — It Was Silent UX
The primary hypothesis (listener count growing across shuffle sessions due to `addListener` without matching cleanup) was definitively wrong. `PlayerViewModel` is scoped to `NavBackStackEntry` and recreated on every navigation — no listener accumulates across instances. The actual cause was a silent `null` return from `onSceneEnded()` leaving users with no visual feedback at queue end.

**Impact:** The investigation was faster than expected (no Profiler heap dump needed once the lifecycle was confirmed). The fix was simpler (one-line banner emit). Future shuffle-related bugs should start by confirming whether `RepeatMode.ALL` wraps correctly before profiling.
**Source:** 03.3-SUMMARY.md PERF-08

---

### `compose_stability.conf` With Comment Lines Causes Parse Error at Line 0
KGP 2.2.20's Compose Compiler throws `Error parsing stability configuration file on line 0` if the config file contains `#` comment lines. This is not a warning — it's a build-blocking parse error. The file format silently rejects any non-class-name content.

**Impact:** The plan expected the file to accept comments (matching the mental model of `.gitignore`, `.editorconfig`, etc.). The fix was immediate (empty the file), but the diagnostic message ("line 0") was confusing — it suggests the problem is on line 0 when actually any comment line triggers it.
**Source:** 03.1-SUMMARY.md DEV-02

---

### Stability Reports Are Generated for ALL Compose Convention Modules, Not Just `:app`
After the convention plugin change, `./gradlew compileReleaseKotlin` generates reports for every module applying `stash.android.compose`. Not just `:feature:home` — all `feature/*` and `core/designsystem` modules produce reports.

**Impact:** The SPEC's acceptance criterion only checked `feature/home/build/compose-reports/*.txt`. Stability data is available project-wide. Future phases can use this data to identify recomposition opportunities in any Compose module without extra configuration.
**Source:** VERIFICATION.md PERF-02 row

---

### ImmutableList Migration Required 4 Files, Not 3
The SPEC and CONTEXT identified 3 target locations (HomeRail, HomeUiState, markers parameters). Actual execution required 4 files: HomeViewModel.kt, HomeScreen.kt (RailScenes composable parameter), PlayerScreen.kt (2 composable signatures + 2 call sites). The HomeScreen.kt change was an unplanned scope addition discovered at compile time.

**Impact:** The build-green gate caught this — the type mismatch at the HomeScreen.kt call site was a compile error, not a silent runtime issue. ImmutableList migrations that are "complete" at the data class level are NOT complete until all consumer composables compile cleanly.
**Source:** 03.2-SUMMARY.md DEV-01

---

### `CompilationMode.None` Is an Inner Class in benchmark-macro 1.3.3
`androidx.benchmark.macro.CompilationMode.None` requires constructor-call syntax `CompilationMode.None()`, not object-reference syntax `CompilationMode.None`. The IDE's autocomplete inserts the latter, which fails at compile time.

**Impact:** Low (compile error caught immediately), but surprising since most Kotlin sealed class / enum variants use object-reference syntax. The benchmark library's `None` is specifically an inner class to allow future parameterization, making it distinct from the object-style siblings like `CompilationMode.Full`.
**Source:** 03.3-SUMMARY.md deviation

---

### PERF-05 Profile Output Is Implicitly Device-Dependent
The SPEC did not annotate PERF-05 as device-dependent (unlike PERF-06/07 which had explicit `[device-dependent]` notes). However, the acceptance criterion (`wc -l baseline-prof.txt → ≥ 20% more lines`) requires an actual device/GMD run to regenerate the file. The code change (journey expansion) is device-agnostic, but the verification artifact is not.

**Impact:** PERF-05 ended up as PARTIAL in the verification report alongside the explicitly device-dependent PERF-06/07. Future SPEC authoring: any acceptance criterion that references a generated file should explicitly note whether that file requires device execution to generate.
**Source:** VERIFICATION.md PERF-05 row
