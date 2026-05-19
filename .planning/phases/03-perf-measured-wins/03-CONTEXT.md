# Phase 3: PERF — Measured Wins - Context

**Gathered:** 2026-05-18
**Status:** Ready for planning
**Mode:** `--auto` (single-pass, recommended defaults; no AskUserQuestion turns)

<domain>
## Phase Boundary

Wire reproducible macrobenchmark infrastructure (GMD), fix the shuffle-playback hang, eliminate the top three recomposition waste sources (`applyVideoFrameRate` in AndroidView.update, unstable `List<T>` UiState fields, unaudited `LaunchedEffect` keys), expand the baseline profile, and demonstrate measurable cold-start and scroll-frame-rate improvements backed by checked-in benchmark outputs.

This phase does NOT split `PlayerScreen.kt` (POLISH-01 deferred), wire the test pyramid (POLISH-04/05 deferred), or bump AGP/compileSdk (DEPS-17 deferred). It operates entirely on the AGP 8.7.3 / Kotlin 2.2.20 / compileSdk 35 floor established by Phases 1–2.

</domain>

<spec_lock>
## Requirements (locked via SPEC.md)

**10 requirements are locked.** See `03-SPEC.md` for full requirements, boundaries, and acceptance criteria.

Downstream agents MUST read `03-SPEC.md` before planning or implementing. Requirements are not duplicated here.

**In scope (from SPEC.md):**
- Gradle Managed Device (Pixel 6 API 34) declared and runnable (PERF-01)
- Compose Compiler stability reports in `AndroidComposeConventionPlugin.kt` (PERF-02)
- `ImmutableList<T>` migration for `HomeRail.scenes`, `HomeUiState.rails`, `markers: List<Marker>` (PERF-03)
- PlayerScreen LaunchedEffect/DisposableEffect key audit — 5 sites at lines 150, 166, 168, 175, 181 (PERF-04)
- Baseline profile expanded to ≥ 4 journeys: cold-start, Home-rails scroll, Detail open, Player start (PERF-05)
- Cold-start macrobench p50 ≥ 5% improvement with profile vs without on GMD (PERF-06)
- Library scroll ≥ 95% frames on time at p95 on GMD (PERF-07)
- Shuffle/consecutive playback diagnosis + fix (investigative; profiling artifact mandatory) (PERF-08)
- `applyVideoFrameRate` relocated from `AndroidView.update` to `LaunchedEffect(targetFps)` (PERF-09)
- All perf claims backed by `.planning/benchmarks/` files (PERF-10)

**Out of scope (from SPEC.md):**
- `PlayerScreen.kt` split — deferred to Phase 4 (POLISH-01)
- JUnit5/Turbine/MockK/Robolectric wiring — deferred to Phase 4 (POLISH-04/05)
- AGP 9 / compileSdk 36 / Hilt 2.57+ migration — DEPS-17 successor
- Media3 1.10.0 upgrade — blocked on compileSdk 36
- `ConnectionResult` → `AppResult` migration — POLISH-07
- `PagingData<T>` types in Library/Browse — NOT migrated to ImmutableList

</spec_lock>

<decisions>
## Implementation Decisions

### Decision 1 — GMD Configuration
**D-01:** Pixel 6 API 34 (`Pixel6Api34` device name in Gradle DSL) using `google_apis` system image. API 34 is the ROADMAP.md-specified target. Device RAM: 4096MB (Pixel 6 real-device spec); cpuCores: 4.

```kotlin
// baselineprofile/build.gradle.kts
managedDevices {
    localDevices {
        create("pixel6Api34") {
            device = "Pixel 6"
            apiLevel = 34
            systemImageSource = "google_apis"
        }
    }
}
baselineProfile {
    managedDevices += "pixel6Api34"
    useConnectedDevices = false
}
```

**D-01a GMD fallback:** If the Pixel 6 API 34 system image fails to download on the dev host (e.g., host memory constraint), accept `REVIEWS-C4 ACCEPT` for PERF-06 and PERF-07 — GMD declaration still lands (PERF-01 satisfied by code); benchmark requirements deferred with profiling artifacts as evidence.

### Decision 2 — Compose Compiler Stability Reports
**D-02:** Add `composeCompiler { reportsDestination = layout.buildDirectory.dir("compose-reports") }` to `AndroidComposeConventionPlugin.kt`. For Kotlin Compose Compiler Plugin 2.x (2.2.20 is the floor), the DSL is:

```kotlin
// In AndroidComposeConventionPlugin.kt apply block
extensions.configure<ComposeCompilerGradlePluginExtension> {
    reportsDestination = layout.buildDirectory.dir("compose-reports")
    stabilityConfigurationFile = layout.projectDirectory.file("compose_stability.conf")
}
```

The stability config file is optional — create an empty one at `app/compose_stability.conf` if the extension requires it. Report files are gitignored (build output) but macrobench outputs are committed.

### Decision 3 — ImmutableList Migration Pattern
**D-03:** Apply `toPersistentList()` at the ViewModel/repository boundary (where `List<T>` is returned from a repo call and assigned to UiState). The UiState data class fields change type from `List<T>` to `ImmutableList<T>`. No `@Immutable` annotation needed on the data class itself — `ImmutableList<T>` is already `@Stable` by the library.

Affected files (confirmed by scout):
- `HomeViewModel.kt` — `HomeRail(scenes: ImmutableList<SceneSummary>)` and `HomeUiState(rails: ImmutableList<HomeRail>)`
- `PlayerScreen.kt` composable params — `markers: ImmutableList<Marker>` (2 call sites at lines 481 and 1006)

Catalog entry already present: `kotlinx-collections-immutable = 0.4.0`. Add `implementation(libs.kotlinx.collections.immutable)` to:
- `:feature:home` (for HomeRail/HomeUiState)
- `:feature:player` (for Marker list composable params)

Default value change: `emptyList<X>()` → `persistentListOf<X>()`.

### Decision 4 — LaunchedEffect Key Audit Approach
**D-04:** Document each of the 5 sites in the Plan task and verify stability inline:

| Line | Type | Keys | Stability verdict |
|------|------|------|-------------------|
| 150 | DisposableEffect | `activity, rotationLocked` | STABLE — `Activity` ref stable for VM lifetime; `Boolean` primitive |
| 166 | LaunchedEffect | `resizeMode, playerView` | NEEDS REVIEW — `playerView: PlayerView?` is an object ref; check if it changes across recompositions (if AndroidView's factory runs once, the ref is stable) |
| 168 | LaunchedEffect | `controlsVisible, state.isPlaying, lastInteraction, locked` | STABLE — all Boolean/Long primitives from state |
| 175 | LaunchedEffect | `stepLeft?.generation` | STABLE — Long? counter |
| 181 | LaunchedEffect | `stepRight?.generation` | STABLE — Long? counter |

Line 166 is the only non-trivial case. Plan task must confirm `playerView` reference stability. If it changes on every AndroidView recomposition, scope it with a `remember` or restructure the effect.

### Decision 5 — applyVideoFrameRate Relocation
**D-05:** Move from `AndroidView(update = { applyVideoFrameRate(it, state.videoFrameRate) })` to a separate `LaunchedEffect(state.videoFrameRate)`. The AndroidView `update` block should only call methods that are safe to call on every recomposition; `applyVideoFrameRate` touches ExoPlayer internals and should be rate-limited to actual fps changes.

```kotlin
// BEFORE (remove from AndroidView update block)
AndroidView(
    update = { pv ->
        applyVideoFrameRate(it, state.videoFrameRate)  // ← remove this line
    }
)
// AFTER (add alongside the AndroidView)
val playerView = remember { ... }  // need to hoist playerView ref
LaunchedEffect(state.videoFrameRate) {
    playerView?.let { applyVideoFrameRate(it, state.videoFrameRate) }
}
```

This requires hoisting the `PlayerView` reference out of the `AndroidView` factory if it isn't already accessible. Use `remember { Ref<PlayerView?>(null) }` or a local `var` captured in the closure.

### Decision 6 — Shuffle Bug Investigation Strategy
**D-06:** Investigation order:
1. **Listener accumulation** — Verify `addListener` call count vs `removeListener` call count across a 10-video shuffle session using Profiler "Record heap dump". Check: does `PlayerViewModel` get recreated for each scene, or is it reused? If reused, the `addListener` at line 101 runs multiple times but `removeListener` only in `onCleared()`. Hypothesis: if the `player: ExoPlayer by lazy` fires once but `addListener` is called each time `viewModelScope.launch` completes (unlikely given the code structure), listeners accumulate.
2. **ExoPlayer media item lifecycle** — Check if `setMediaItem` at line 353 properly clears previous MediaSource state between songs.
3. **Memory profiler** — If listener count is stable, run 10-video shuffle session with Android Profiler in "Record memory" mode; look for monotonic heap growth in `MediaSource`, `C2SoftAvcDec` (decoder), or `WeakReference` growth.

Committed artifact: `.planning/benchmarks/perf-08-shuffle-profile.{png|txt}` — required regardless of whether a fix lands.

### Decision 7 — Baseline Profile Journey Expansion
**D-07:** Expand `StashBaselineProfileGenerator.kt` with 3 additional journeys beyond the existing cold-start + library-scroll:
1. **Home rails scroll** — navigate to Home (already on it after cold start), scroll through all 4 rails
2. **Detail open** — tap first library item, wait for DetailScreen to render
3. **Player first-frame** — tap Play on a scene detail, wait for first ExoPlayer frame (use `Until.findObject(By.res("player_surface"))` or equivalent UiAutomator probe)

All journeys gate-checked with `device.waitForIdle()` between steps. If a journey fails due to missing server connection, skip gracefully (not a test failure — baseline profile is best-effort).

### Decision 8 — Benchmark Output Format
**D-08:** Committed benchmark outputs under `.planning/benchmarks/`:
- `perf-06-cold-start.txt` — raw macrobench stdout for ColdStartBenchmark (CompilationMode.Partial WITH profile vs CompilationMode.None WITHOUT)
- `perf-07-library-scroll.txt` — raw macrobench stdout for LibraryScrollBenchmark
- `perf-08-shuffle-profile.{png|txt}` — Android Profiler screenshot or heap analysis notes
- Format: raw macrobench output preserved as-is (no summarizing), so future readers can verify numbers

### Decision 9 — Plan Wave Structure
**D-09:** 3 plans, matching ROADMAP.md §Suggested Plans:
- **Plan 3.1** — Measurement substrate (PERF-01 GMD, PERF-02 stability reports, PERF-05 profile expansion): `wave: 1`, `autonomous: true`
- **Plan 3.2** — Compose hygiene audit (PERF-03 ImmutableList, PERF-04 LaunchedEffect audit, PERF-09 applyVideoFrameRate): `wave: 2`, `autonomous: true`, `depends_on: plan-3.1`
- **Plan 3.3** — Shuffle fix + numeric floors (PERF-08 shuffle, PERF-06 cold-start bench, PERF-07 scroll bench, PERF-10 claims): `wave: 2`, `autonomous: false` (human checkpoint for profiling session and macrobench execution), `depends_on: plan-3.1`

**Wave sequencing rationale:** Plan 3.1 must land first so the GMD and stability reports exist before Plans 3.2 and 3.3 run. Plans 3.2 and 3.3 can run in parallel after 3.1 (they touch disjoint surfaces).

**Human checkpoint in Plan 3.3:** The macrobench runs (PERF-06/07) and profiling session (PERF-08) require device/GMD interaction. UAT for Phase 3 is deferred to end of milestone per user instruction — Plan 3.3's human checkpoint will be handled then.

### Decision 10 — Commit Budget
**D-10:** Target ≤ 10 atomic COMPLY-style commits:
- Plan 3.1: GMD config commit + stability reports commit + baseline profile expansion commit = 3 commits
- Plan 3.2: ImmutableList migration commit + LaunchedEffect audit commit + applyVideoFrameRate commit = 3 commits
- Plan 3.3: Shuffle fix commit (if applicable) + benchmark output commit = 1–2 commits
- Total target: 7–8 code commits + docs/planning commits outside budget

### Accepted Risks (carried forward and new)
**AR-03-01:** PERF-06 / PERF-07 macrobench require GMD. If Pixel 6 API 34 system image cannot be downloaded/spun on dev host: REVIEWS-C4 ACCEPT — benchmark requirements deferred, code changes (GMD declaration, profile expansion) still land. Revisit trigger: CI runner with emulator capability.

**AR-03-02:** PERF-08 shuffle root cause may require AGP-9 migration or large architectural refactor. If so: diagnosis artifact committed, fix deferred via accepted-risk entry with revisit trigger.

### Claude's Discretion
- Exact Compose Compiler plugin DSL syntax — verify against Kotlin 2.2.20 actual API (may differ from the snippets above)
- `compose_stability.conf` filename and placement — follow official docs for AGP 8.7.3
- Whether `playerView` hoisting in D-05 requires restructuring other AndroidView interactions — planner decides based on full PlayerScreen.kt read
- `aosp_atd` vs `google_apis` system image — prefer `google_apis` for widest Play Services compat; fall back to `aosp_atd` if `google_apis` is unavailable

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase 3 Spec and Context
- `.planning/phases/03-perf-measured-wins/03-SPEC.md` — Locked requirements, boundaries, acceptance criteria (MANDATORY)
- `.planning/REQUIREMENTS.md` — PERF-01..10 full requirement text
- `.planning/ROADMAP.md` — Phase 3 section with suggested plans (3.1/3.2/3.3) and risk register

### Prior Phase Artifacts (constraint inheritance)
- `.planning/phases/01-deps-foundation-bump/01-CONTEXT.md` — AGP 8.7.3 / Gradle 8.11.1 / compileSdk 35 floor decisions
- `.planning/phases/02-comply-platform-compliance/02-CONTEXT.md` — Platform compliance decisions; anti-coupling rule (no Spine pre-painting)

### Key Implementation Files
- `baselineprofile/build.gradle.kts` — Current GMD-less config (target of PERF-01)
- `baselineprofile/src/main/java/io/stashapp/android/baselineprofile/StashBaselineProfileGenerator.kt` — Current 2-journey generator (target of PERF-05)
- `feature/player/src/main/java/io/stashapp/android/feature/player/PlayerScreen.kt` — PERF-04 (LaunchedEffect sites: lines 150, 166, 168, 175, 181), PERF-09 (applyVideoFrameRate at line 222)
- `feature/player/src/main/java/io/stashapp/android/feature/player/PlayerViewModel.kt` — PERF-08 (addListener at line 101, onCleared at line 442)
- `feature/home/src/main/java/io/stashapp/android/feature/home/HomeViewModel.kt` — PERF-03 (HomeRail.scenes, HomeUiState.rails)
- `build-logic/convention/src/main/kotlin/io/stashapp/android/buildlogic/AndroidComposeConventionPlugin.kt` — PERF-02 (stability reports target)
- `gradle/libs.versions.toml` — `kotlinxCollectionsImmutable = "0.4.0"` already present (PERF-03)

### Codebase Analysis
- `.planning/codebase/CONCERNS.md` — Tech debt register; `applyVideoFrameRate`/`PlayerScreen.kt` concerns relevant to PERF-09
- `.planning/phases/01-deps-foundation-bump/01-LEARNINGS.md` — Phase 1 learnings on Compose BOM + baseline profile
- `.planning/phases/02-comply-platform-compliance/02-LEARNINGS.md` — Phase 2 learnings (cross-AI wave sequencing pattern applies to Phase 3 3.2+3.3 parallel plans)

</canonical_refs>

<code_context>
## Codebase Context

### Reusable Assets
- `kotlinx-collections-immutable 0.4.0` — already in `gradle/libs.versions.toml`; add `implementation(libs.kotlinx.collections.immutable)` to `:feature:home` and `:feature:player`
- `BaselineProfileRule` — already used in `StashBaselineProfileGenerator.kt`; expand journeys in the same file
- `PlayerUiState` — already `data class`; adding `ImmutableList` does not require structural changes to the ViewModel

### Integration Points
- `AndroidComposeConventionPlugin.kt` — where stability reports extension goes; keep changes minimal (2–3 lines)
- `baselineprofile/build.gradle.kts` — GMD block replaces `baselineProfile { useConnectedDevices = true }` (currently on line ~29)
- `PlayerScreen.kt:209` — `AndroidView(factory = ..., update = { ... })` block; `applyVideoFrameRate` is at the end of the `update` lambda

### Pitfalls (from Phase 1/2 LEARNINGS.md)
- **Wave sequencing:** Plans 3.2 and 3.3 must be `wave: 2` (not parallel with 3.1) — they require GMD and stability reports from 3.1. Planner should declare `depends_on: plan-3.1` for both.
- **Cross-AI review convergence pattern:** 3/3 reviewers caught the wave-1 conflict in Phase 2. Apply the same cross-plan file-conflict check before finalizing wave assignments.
- **No Spine pre-painting:** Anti-coupling rule carries forward; `grep -rn 'import .*Spine' feature/ core/ app/` must return empty.

### Known Deferred Items Relevant to This Phase
- DEPS-16 (baseline profile regen deferred in Phase 1) — PERF-05 supersedes this; new profile generated via PERF-01 GMD
- DEPS-17 (AGP 9 deferred) — Phase 3 stays on AGP 8.7.3; no scope change
- COMPLY-07-3BTN (3-button-nav deferred) — not relevant to Phase 3

</code_context>

## Open Questions (resolved for planning)

| # | Question | Decision |
|---|----------|----------|
| 1 | GMD API level? | API 34 / Pixel 6 / google_apis |
| 2 | Stability reports DSL for Kotlin 2.2.20? | `ComposeCompilerGradlePluginExtension.reportsDestination` — verify exact API against Kotlin 2.2.20 Compose Compiler plugin |
| 3 | ImmutableList at ViewModel or repo boundary? | ViewModel boundary (`toPersistentList()` in update block) |
| 4 | How many baseline profile journeys? | 4: cold-start, Home rails, Detail open, Player first-frame |
| 5 | Shuffle investigation tool? | LeakCanary first, then Profiler |
| 6 | Plan wave structure? | 3.1 wave 1; 3.2 + 3.3 wave 2 (parallel, depends_on 3.1) |
| 7 | Benchmark output location? | `.planning/benchmarks/*.txt` committed |
| 8 | Human testing timing? | Deferred to end-of-milestone per user instruction; Plan 3.3 checkpoint documents this |
