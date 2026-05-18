# Phase 3: PERF — Measured Wins — Specification

**Created:** 2026-05-18
**Ambiguity score:** 0.17 (gate: ≤ 0.20)
**Requirements:** 10 locked

## Goal

Wire reproducible macrobenchmark infrastructure (GMD), fix the shuffle-playback hang, eliminate the top three recomposition waste sources, and demonstrate measurable cold-start and scroll-frame-rate improvements backed by checked-in benchmark outputs.

## Background

Phase 1 landed the toolchain floor (AGP 8.7.3, Kotlin 2.2.20, Compose BOM 2026.05.00). Phase 2 cleared all platform compliance regressions. Phase 3 now has a stable floor to measure against.

**Current state (scouted 2026-05-18):**

- **Baseline profile:** `StashBaselineProfileGenerator.kt` covers cold start + library-grid scroll only. No Home rails, no Detail open, no Player start. Profile is stale (DEPS-16 regen was deferred).
- **GMD:** `baselineprofile/build.gradle.kts` uses `useConnectedDevices = true` — no Gradle Managed Device declared. Regeneration requires a physically connected device.
- **Compose Compiler stability reports:** Not enabled in any convention plugin.
- **ImmutableList:** `kotlinx-collections-immutable 0.4.0` is in `gradle/libs.versions.toml` but zero UiState types use it. Confirmed List fields: `HomeRail.scenes: List<SceneSummary>` (HomeViewModel.kt:37), `HomeUiState.rails: List<HomeRail>` (HomeViewModel.kt:42), `markers: List<Marker>` (PlayerScreen.kt:481, 1006). PagingData-backed types (Library, Browse) are NOT candidates.
- **LaunchedEffect/DisposableEffect in PlayerScreen:** 5 sites — lines 150 (DisposableEffect keyed on `activity, rotationLocked`), 166 (`LaunchedEffect(resizeMode, playerView)`), 168 (`LaunchedEffect(controlsVisible, state.isPlaying, lastInteraction, locked)`), 175 (`LaunchedEffect(stepLeft?.generation)`), 181 (`LaunchedEffect(stepRight?.generation)`).
- **applyVideoFrameRate:** Called at `AndroidView.update` block (PlayerScreen.kt:222) — fires on every Compose recomposition that touches the AndroidView, not just on targetFps changes.
- **Shuffle/consecutive playback:** `PlayerViewModel.onCleared()` has `removeListener` + `release`. The hang likely surfaces during consecutive scene transitions (new `addListener` call at line 101 on each `ExoPlayer` build; if the ViewModel is reused across navigation back-stacks, accumulated listeners may not be cleared between scenes).

## Requirements

1. **GMD wired (PERF-01):** A Gradle Managed Device `Pixel 6 API 34` is declared in `baselineprofile/build.gradle.kts` and the macrobenchmark task runs reproducibly from CI with no connected physical device.
   - Current: `baselineprofile { useConnectedDevices = true }` — requires connected phone
   - Target: `managedDevices { devices { create("pixel6Api34") { ... } } }` declared; `./gradlew :baselineprofile:pixel6Api34BenchmarkAndroidTest` exits 0 without a physical device
   - Acceptance: `./gradlew :baselineprofile:pixel6Api34BenchmarkAndroidTest` exits 0; task output references "pixel6Api34" emulator; no "No connected devices" error

2. **Compose Compiler stability reports (PERF-02):** The `stash.android.library.compose` convention plugin enables stability reports in every Compose library module.
   - Current: No stability report configuration in any convention plugin
   - Target: `composeCompiler { reportsDestination = layout.buildDirectory.dir("compose-reports") }` (or equivalent for Kotlin Compose Compiler plugin 2.x) added to `AndroidComposeConventionPlugin.kt`; reports generated for all `stash.android.compose` modules on `compileReleaseKotlin`
   - Acceptance: `find . -path "*/build/compose-reports/*.txt" | wc -l` → ≥ 1 after `./gradlew compileReleaseKotlin`; file content contains `stable` / `unstable` classifications

3. **ImmutableList migration (PERF-03):** The three identified `List<T>` UiState fields are migrated to `ImmutableList<T>` from `kotlinx.collections.immutable`.
   - Current: `HomeRail.scenes: List<SceneSummary>`, `HomeUiState.rails: List<HomeRail>`, `markers: List<Marker>` (PlayerScreen) use `kotlin.collections.List`; `kotlinx-collections-immutable 0.4.0` is in the catalog but unused
   - Target: All three fields use `ImmutableList<T>` (`persistentListOf()` / `toPersistentList()`); `@Stable` or `@Immutable` annotation confirms stable classification in Compose Compiler reports (PERF-02 verifies)
   - Acceptance: `grep -rn 'List<SceneSummary>\|List<HomeRail>\|List<Marker>' feature/ core/` → 0 hits (only `ImmutableList<...>` remains); `./gradlew assembleDebug` exits 0

4. **Player LaunchedEffect key audit (PERF-04):** All 5 LaunchedEffect/DisposableEffect sites in `PlayerScreen.kt` have correct key sets; no effect key captures an unstable lambda or an always-different reference.
   - Current: 5 sites (lines 150, 166, 168, 175, 181); line 168 keys on `state.isPlaying` (stable — Boolean), `lastInteraction` (Long — stable), `controlsVisible` (Boolean — stable), `locked` (Boolean — stable); line 166 keys on `playerView` (AndroidView ref — potentially unstable)
   - Target: Every effect key is either a stable primitive/enum, a State-read value, or explicitly documented if an object reference is unavoidable; no `LaunchedEffect(Unit)` exists outside of one-shot effects where re-launch must be prevented
   - Acceptance: A code-review comment block in the SUMMARY.md documents each of the 5 sites with: key type, stability verdict (STABLE / DOCUMENTED_UNSTABLE), and rationale; `./gradlew compileReleaseKotlin` exits 0

5. **Baseline profile expanded (PERF-05):** `StashBaselineProfileGenerator.kt` covers at least 4 user journeys: cold start → library grid, Home rails scroll, Detail screen open, Player start (first-frame).
   - Current: Only cold start + library-grid scroll covered
   - Target: Generator journey covers ≥ 4 flows; generated `baseline-prof.txt` is ≥ 20% larger (line count) than the current stale file
   - Acceptance: `wc -l app/src/release/generated/baselineProfiles/baseline-prof.txt` → ≥ 20% more lines than the committed baseline before this phase; journey source file references at least Home, Library, Detail, and Player destinations

6. **Cold-start macrobench floor (PERF-06):** p50 cold-start time with baseline profile is ≥ 5% faster than without, on the GMD, across 3 back-to-back runs within variance budget.
   - Current: No macrobench output committed; profile is stale
   - Target: Benchmark output file committed under `.planning/benchmarks/perf-06-cold-start.txt`; p50 WITH_PROFILE / WITHOUT_PROFILE ratio ≥ 1.05 (5% improvement)
   - Acceptance: `.planning/benchmarks/perf-06-cold-start.txt` exists; contains p50 values for both `CompilationMode.Partial(BaselineProfileMode.Require)` and `CompilationMode.None`; ratio ≥ 1.05; variance across 3 runs ≤ 10% of median value
   - **Device dependency:** Requires GMD (PERF-01). If GMD cannot be spun up, this requirement is deferred via REVIEWS-C4 ACCEPT.

7. **Library scroll frame-rate floor (PERF-07):** Library scroll macrobench shows ≥ 95% frames on time at p95 on the GMD.
   - Current: No macrobench output committed
   - Target: Benchmark output file committed under `.planning/benchmarks/perf-07-library-scroll.txt`; p95 frame time ≤ 16.67ms (60fps budget) or ≤ 8.33ms (120fps budget) depending on GMD display refresh rate; percentage of frames on time ≥ 95% at p95
   - Acceptance: `.planning/benchmarks/perf-07-library-scroll.txt` exists and contains p95 frame time and on-time percentage; value ≥ 95%
   - **Device dependency:** Requires GMD (PERF-01). If GMD cannot be spun up, this requirement is deferred via REVIEWS-C4 ACCEPT.

8. **Shuffle/consecutive playback fix (PERF-08):** Diagnose the hang/slowdown after several consecutive shuffled videos; implement a fix if root cause is actionable without AGP-9.
   - Current: `PlayerViewModel.onCleared()` calls `removeListener` + `release`; however the ViewModel's `player: ExoPlayer by lazy` may survive across navigation back-stack (ViewModel is reused when the NavBackStackEntry is retained); `addListener(playerListener)` is called on every `LaunchedEffect(player)` in `PlayerScreen` — if multiple screen instances overlap, listeners accumulate
   - Target: 10-video shuffle session shows no monotonic heap growth and no accumulated listener count (verified via Android Profiler or LeakCanary run); root cause documented in SUMMARY.md with evidence (Profiler screenshot or LeakCanary trace committed under `.planning/benchmarks/perf-08-shuffle-profile.*`)
   - Acceptance: At least one profiling artifact committed under `.planning/benchmarks/perf-08-*`; SUMMARY.md documents the root cause with specific class/line; if fix was applied: `./gradlew assembleDebug` exits 0 and the fix is in a committed atomic feat commit; if root cause requires AGP-9 migration: accepted-risk entry in CONTEXT.md with revisit trigger
   - **Note:** Investigative requirement — if the root cause is outside Phase 3's scope (e.g. requires AGP-9 or a large arch refactor), a documented diagnosis + deferred accept is the valid outcome.

9. **applyVideoFrameRate relocation (PERF-09):** `applyVideoFrameRate` is moved out of `AndroidView.update` into `LaunchedEffect(targetFps)`.
   - Current: `applyVideoFrameRate(it, state.videoFrameRate)` called at `PlayerScreen.kt:222` inside `AndroidView(update = { … })` — fires on every recomposition touching the AndroidView, not only on targetFps changes
   - Target: `applyVideoFrameRate` call removed from the `AndroidView` update block; replaced by `LaunchedEffect(state.videoFrameRate) { playerView?.let { applyVideoFrameRate(it, state.videoFrameRate) } }` (or equivalent); function is called exactly once per `videoFrameRate` change
   - Acceptance: `grep -n 'applyVideoFrameRate' PlayerScreen.kt` → appears only in a `LaunchedEffect` block, NOT inside an `AndroidView(update = …)` block; `./gradlew assembleDebug` exits 0

10. **Measured-only claims (PERF-10):** Every performance-related statement in Phase 3 PR description and SUMMARY.md is backed by a checked-in file under `.planning/benchmarks/` or a Compose Compiler stability report.
    - Current: No benchmarks directory exists; no macrobench outputs
    - Target: `.planning/benchmarks/` directory exists and contains at minimum one file per macrobench claim; no PR bullet point says "feels faster" or "should be faster" without a file reference
    - Acceptance: `ls .planning/benchmarks/` → ≥ 1 file; PR description cites specific file paths; any GMD-dependent benchmark that was deferred has an explicit accepted-risk entry (not an unsupported claim)

## Boundaries

**In scope:**
- Gradle Managed Device declaration for `Pixel 6 API 34`
- Compose Compiler stability reports configuration
- `ImmutableList<T>` migration for `HomeRail.scenes`, `HomeUiState.rails`, and `markers: List<Marker>`
- LaunchedEffect/DisposableEffect key audit for all 5 sites in `PlayerScreen.kt`
- Baseline profile expansion to cover ≥ 4 user journeys
- Cold-start macrobench (PERF-06) and library-scroll macrobench (PERF-07) with committed output files
- Shuffle/consecutive playback investigation + fix (or documented diagnosis + defer)
- `applyVideoFrameRate` relocation from AndroidView.update to LaunchedEffect(targetFps)
- `.planning/benchmarks/` directory with committed macrobench output files

**Out of scope:**
- `PlayerScreen.kt` split into `PlayerControls.kt`/`PlayerGestures.kt`/`PlayerSurface.kt` — deferred to Phase 4 (POLISH-01); split must happen AFTER perf fixes are in place so the refactor preserves measured wins
- JUnit5/Turbine/MockK/Robolectric wiring — deferred to Phase 4 (POLISH-04/05)
- AGP 9 / compileSdk 36 / Hilt 2.57+ migration — deferred (DEPS-17 successor); no Hilt supports AGP 9 yet
- Media3 1.10.0 upgrade — blocked on compileSdk 36 (DEPS-17)
- `ConnectionResult` → `AppResult` migration — deferred to Phase 4 (POLISH-07)
- Lint baseline shrink — deferred to Phase 4 (POLISH-02)
- `StashImageLoader.kt` runBlocking fix (POLISH-CACHE-01) — deferred to Phase 4
- New user-visible features or behavior changes — this phase is measurement and optimization only

## Constraints

- **Toolchain floor:** AGP 8.7.3 / Kotlin 2.2.20 / compileSdk 35 / Compose BOM 2026.05.00 (unchanged from Phase 1/2 — no AGP 9 bump in this phase)
- **Device dependency for PERF-06/07:** GMD declaration (PERF-01) is required before PERF-06 and PERF-07 can execute. If the GMD emulator image cannot be downloaded or spun up on the dev host, PERF-06 and PERF-07 are deferred via REVIEWS-C4 ACCEPT with explicit accepted-risk entries.
- **ImmutableList scope:** Only `List<T>` fields in non-paged UiState types. `PagingData<T>` types in `LibraryViewModel` and `BrowseViewModel` are NOT migrated (PagingData is incompatible with ImmutableList).
- **PERF-08 investigative cap:** If diagnosing the shuffle bug requires > 4 hours of profiling, document the investigation findings and defer the fix via REVIEWS-C4 ACCEPT. A committed profiling artifact is mandatory regardless of whether a fix lands.
- **No "feels faster" claims:** Every performance statement must cite a `.planning/benchmarks/` file or a Compose Compiler report. The PERF-10 gate blocks the PR if any claim is unsupported.
- **Anti-coupling rule from Phase 2 carries forward:** `grep -rn 'import .*Spine' feature/ core/ app/` must return empty after all Phase 3 commits.

## Open Questions (resolved for planning)

| # | Question | Decision |
|---|----------|----------|
| 1 | Can PERF-06/07 run without GMD? | No — must be deferred via REVIEWS-C4 ACCEPT if GMD unavailable |
| 2 | Which UiState `List<T>` fields migrate? | `HomeRail.scenes`, `HomeUiState.rails`, `markers: List<Marker>` only |
| 3 | Is PERF-08 blocking? | No — diagnosis artifact is blocking; fix is conditional on root cause actionability |
| 4 | Where do benchmark outputs land? | `.planning/benchmarks/` directory, committed |
| 5 | What if shuffle bug root cause requires AGP-9? | Accepted-risk entry in CONTEXT.md + profiling artifact committed |

## Ambiguity Report

```
Goal Clarity:       0.88 (min 0.75) ✓
Boundary Clarity:   0.82 (min 0.70) ✓
Constraint Clarity: 0.79 (min 0.65) ✓
Acceptance Criteria:0.83 (min 0.70) ✓
Ambiguity score:    0.17 (gate: ≤ 0.20) ✓ PASS
```

All 4 dimensions above minimum. Gate passed. No dimensions below minimum — planner may proceed without assumptions.
