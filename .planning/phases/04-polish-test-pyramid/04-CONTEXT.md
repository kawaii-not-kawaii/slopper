# Phase 4: POLISH — Test Pyramid & Cleanup - Context

**Gathered:** 2026-05-19
**Status:** Ready for planning
**Mode:** `--auto` (single-pass, recommended defaults; no AskUserQuestion turns)

<domain>
## Phase Boundary

Clean up the technical debt accumulated across Phases 1–3: split the oversized `PlayerScreen.kt` into maintainable files, shrink the lint baseline, wire the test infrastructure, retire `ConnectionResult`, extract domain preference interfaces, add Forgejo CI, refresh docs, and close VCS hygiene gaps. All changes are behavior-preserving. No new user-visible features.

</domain>

<spec_lock>
## Requirements (locked via SPEC.md)

**10 requirements are locked.** See `04-SPEC.md` for full requirements, boundaries, and acceptance criteria.

Downstream agents MUST read `04-SPEC.md` before planning or implementing.

**In scope (from SPEC.md):**
- `PlayerScreen.kt` split into `PlayerScreen.kt` + `PlayerControls.kt` + `PlayerTimeline.kt` (≤ 600 lines each)
- Lint baseline shrink ≥ 30% (1001 → ≤ 700 lines)
- Detekt + ktlint zero new violations
- JUnit5 + Turbine + MockK + Robolectric wired in `stash.android.library`
- Seed test suites (≥ 15 test files)
- `PlayerSettings` / `UiSettings` interfaces in `:core:domain`
- `ConnectionResult` → `AppResult<ServerInfo>` + `catch (e: Throwable)` narrowing
- `.forgejo/workflows/ci.yml` with correct cache keys
- `DEVICE_TESTING.md` Phase 2/3 additions + `.planning/codebase/` re-map
- `local.properties` removed from VCS

**Out of scope (from SPEC.md):**
- Module-graph restructure
- AGP 9 / compileSdk 36 upgrade
- Apollo 5 / Nav3 / MediaSessionService
- Full 80%+ test coverage (seed only)
- POLISH-CACHE-01 (StashImageLoader runBlocking)

</spec_lock>

<decisions>
## Implementation Decisions

### Decision 1 — PlayerScreen Split Target Files
**D-01:** Split `PlayerScreen.kt` (1227 lines) into 3 files:

1. **`PlayerScreen.kt`** — Entry point composable `PlayerScreen(...)` (lines ~123–477), AndroidView ExoPlayer surface, gesture detection (`pointerInput`), `safeDrawingPadding` overlay structure, LaunchedEffects (PERF-03/09 from Phase 3), state wiring. Target: < 500 lines.

2. **`PlayerControls.kt`** — `PlayerControls(...)` composable (line ~478 and below) through the icon/button row helpers. Contains: top bar with title/controls row, bottom controls bar, speed selector, aspect ratio button, shuffle/repeat/CC/list buttons. Target: < 500 lines.

3. **`PlayerTimeline.kt`** — `TimelineBar(...)`, `BannerPill(...)`, `MarkerSeekCallout(...)` and their support composables. Contains the seek bar, marker tick marks, remaining-time toggle, and banner overlay. Target: < 350 lines.

**Why 3 files:** ROADMAP.md suggested `PlayerControls.kt` + `PlayerGestures.kt`, but the actual code structure shows the natural boundaries are entry/surface vs controls vs timeline. Gesture code is tightly woven into `PlayerScreen.kt`'s `Box` structure and would require more refactoring to extract cleanly. The `TimelineBar` cluster is already self-contained and clean to extract.

**CRITICAL:** Phase 3 additions that MUST survive the split:
- `LaunchedEffect(state.videoFrameRate) { playerView?.let { applyVideoFrameRate(...) } }` (PERF-09)
- `ImmutableList<Marker>` parameter types (PERF-03)
- Stability comment above `LaunchedEffect(resizeMode, playerView)` (PERF-04)
- `PredictiveBackHandler` (COMPLY-02)
- `safeDrawingPadding` wrapper (COMPLY-01)
- All import paths must be correct in each new file

### Decision 2 — Test Framework Versions
**D-02:** Add to `gradle/libs.versions.toml`:

```toml
[versions]
junit5 = "5.11.4"
mockk = "1.14.0"
turbine = "1.2.0"
robolectric = "4.14.1"

[libraries]
junit5-api = { module = "org.junit.jupiter:junit-jupiter-api", version.ref = "junit5" }
junit5-engine = { module = "org.junit.jupiter:junit-jupiter-engine", version.ref = "junit5" }
junit5-params = { module = "org.junit.jupiter:junit-jupiter-params", version.ref = "junit5" }
mockk = { module = "io.mockk:mockk", version.ref = "mockk" }
turbine = { module = "app.cash.turbine:turbine", version.ref = "turbine" }
robolectric = { module = "org.robolectric:robolectric", version.ref = "robolectric" }
```

Add to `AndroidLibraryConventionPlugin.kt` (inside `target.dependencies {}` block):
```kotlin
testImplementation(libs.junit5.api)
testImplementation(libs.junit5.params)
testRuntimeOnly(libs.junit5.engine)
testImplementation(libs.mockk)
testImplementation(libs.turbine)
testImplementation(libs.robolectric)
```

Add `useJUnitPlatform()` to the `testOptions { }` block in the convention plugin.

**Note:** JUnit 4 catalog entry (`junit = "4.13.2"`) is kept — it's used by `baselineprofile` and `androidx.test.ext:junit`. Do NOT remove it.

### Decision 3 — PlayerSettings / UiSettings Interface Scope
**D-03:** Minimal interfaces — expose only the fields that feature modules currently read via direct `:core:data` deps.

**`PlayerSettings` interface in `core/domain/`:**
```kotlin
interface PlayerSettings {
    val seekMsPerPx: Flow<Float>
    val doubleTapSeekSeconds: Flow<Int>
    val defaultSpeed: Flow<Float>
    val autoPlayNext: Flow<Boolean>
    val decoderPreference: Flow<String>
    val defaultAspectRatio: Flow<String>
    val resumeThresholdSeconds: Flow<Int>
    val completionThresholdPercent: Flow<Int>
    val skipIntroSeconds: Flow<Int>
    suspend fun setDecoderPreference(value: String)
    suspend fun setDefaultSpeed(value: Float)
    suspend fun setDefaultAspectRatio(value: String)
    // ... setters for other mutable prefs
}
```

**`UiSettings` interface in `core/domain/`:**
```kotlin
interface UiSettings {
    val bottomNavVisibleIds: Flow<List<String>>
    val gridColumns: Flow<String>
    val amoledBlackMode: Flow<Boolean>
    val activityTracking: Flow<Boolean>
    val autoRotatePlayer: Flow<Boolean>
    val defaultSceneFilter: Flow<SceneFilter?>
    suspend fun setGridColumns(value: String)
    suspend fun setAmoledBlack(value: Boolean)
    // ... setters for other mutable prefs
}
```

`PlayerPreferences` in `:core:data` implements `PlayerSettings`; `UiPreferences` implements `UiSettings`. Both bound in `DataModule` via `@Binds @Singleton`.

**After POLISH-06:** `:feature:player` and `:feature:settings` inject `PlayerSettings`/`UiSettings` directly (via Hilt interface binding). They drop `implementation(project(":core:data"))` if that dep was ONLY for prefs. `:feature:library` similarly drops `:core:data` dep if ONLY for `UiPreferences`.

### Decision 4 — Lint Baseline Shrink Strategy
**D-04:** Fix high-yield, low-risk categories first. Target: remove ≥ 301 lines from the 1001-line baseline:

**Fix categories (in priority order):**
1. `UnusedResources` — deleted/moved resources, safe to remove from baseline
2. `ObsoleteSdkInt` / deprecated API warnings where Phase 2 already fixed the root cause
3. `HardcodedText` where strings already exist in `strings.xml` (Phase 2 added new strings)
4. Import cleanup warnings (unused imports from Phase 2/3 refactors)
5. Any new violations from Phase 2/3 that slipped into the baseline

**Defer (leave in baseline):**
- `ContentDescription` — accessibility debt, requires design intent per element
- `TrustAllX509TrustManager` — network security, needs BG-MEDIA milestone context
- `VectorPath` / `IconPack` — auto-generated, low ROI
- Complex structural warnings requiring large refactors

Run `./gradlew :app:updateLintBaseline` after fixing, then diff to confirm ≥ 301 lines removed.

### Decision 5 — Plan Wave Structure
**D-05:** 3 plans, all wave 1 (parallel-safe — disjoint file sets):

- **Plan 4.1** — PlayerScreen split + domain interfaces + ConnectionResult (POLISH-01, POLISH-06, POLISH-07): `wave: 1`, `autonomous: true`
- **Plan 4.2** — Test infrastructure + seed tests (POLISH-04, POLISH-05): `wave: 1`, `autonomous: true`. Note: POLISH-04 (framework wiring) must be Task 1 before POLISH-05 (seed tests) as Task 2 within the same plan.
- **Plan 4.3** — Baselines + CI + docs + hygiene (POLISH-02, POLISH-03, POLISH-08, POLISH-09, POLISH-10): `wave: 1`, `autonomous: true`

**Parallelism rationale:**
- Plan 4.1 touches: `feature/player/`, `core/domain/`, `core/data/`, `core/model/`, `feature/connection/`
- Plan 4.2 touches: `build-logic/convention/`, `gradle/libs.versions.toml`, all `src/test/` source sets (additive only)
- Plan 4.3 touches: `app/lint-baseline.xml`, `config/detekt-baseline.xml`, `.forgejo/workflows/`, `DEVICE_TESTING.md`, `.planning/codebase/`, `.gitignore`, `local.properties`

No overlapping files. All 3 can run in parallel.

**UAT:** Deferred to end-of-milestone per user instruction. Plan 4.1's PlayerScreen split behavior change is the only one requiring device verification.

### Decision 6 — POLISH-05 Seed Test Depth
**D-06:** Each seed test verifies the wiring (compilation + basic assertion), not exhaustive behavior:

**Core module tests (1 file each):**
- `core/common`: `AppResultTest.kt` — Success data extraction, Failure error type check
- `core/model`: `SceneSummaryTest.kt` — Basic model construction (data class property access)
- `core/domain`: `AppErrorTest.kt` — AppError message property (or skip if no pure logic)

**ViewModel tests (1 per feature — use Turbine + Coroutine test dispatchers):**
- Pattern: `HomeViewModelTest.kt` — StateFlow emits `Initial` state; mock repository returns test data; verify state updates
- Same pattern for: Connection, Library, Browse, Detail, Player, Settings (7 ViewModels total)

**Compose smoke tests (1 per feature — use Robolectric + ComposeTestRule):**
- Pattern: `@Config(sdk = [33]) class HomeScreenSmokeTest { ... }` — verify composable renders without crash
- Same pattern for all feature screens (7 screens)

Total: 3 + 7 + 7 = 17 files (≥ 15 requirement met).

### Decision 7 — Throwable Narrowing Pattern (POLISH-07)
**D-07:** Replace all `catch (e: Throwable)` in `core/data/` repositories and paging sources with:

```kotlin
// PATTERN: narrow from broad to specific
} catch (e: CancellationException) {
    throw e  // preserve structured concurrency
} catch (e: ApolloException) {
    AppResult.Failure(AppError.Network(e.message ?: "Apollo request failed"))
} catch (e: IOException) {
    AppResult.Failure(AppError.Network(e.message ?: "IO error"))
} catch (e: HttpException) {
    AppResult.Failure(AppError.Server("HTTP ${e.statusCode}"))
} catch (e: Exception) {
    AppResult.Failure(AppError.Unknown(e.message ?: "Unexpected error", cause = e))
}
```

Files to update (all in `core/data/`):
- `DefaultSceneRepository.kt` (4 catch blocks)
- `ScenePagingSource.kt` (1 catch block)
- `BrowsePagingSources.kt` (3 catch blocks)
- `DefaultConnectionRepository.kt` already uses typed catches — verify CancellationException is rethrown

**Note:** `DefaultConnectionRepository.kt` already has typed `catch` blocks. Check whether `CancellationException` is explicitly rethrown. If not, add it. The `ConnectionResult` retirement is also in this file.

### Decision 8 — CI Workflow Content
**D-08:** `.forgejo/workflows/ci.yml` content:

```yaml
name: CI
on:
  push:
    branches: [master]
  pull_request:
    branches: [master]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Cache Gradle
        uses: actions/cache@v4
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: gradle-${{ hashFiles('gradle/wrapper/gradle-wrapper.properties') }}-jdk17-agp8-${{ hashFiles('gradle/libs.versions.toml') }}
          restore-keys: |
            gradle-${{ hashFiles('gradle/wrapper/gradle-wrapper.properties') }}-jdk17-agp8-

      - name: Bootstrap wrapper
        run: bash bootstrap.sh

      - name: Build + Lint + Static analysis
        run: ./gradlew assembleDebug detekt ktlintCheck
```

Cache key includes: wrapper SHA (`gradle-wrapper.properties` hash) + JDK version (hardcoded `jdk17`) + AGP version (hardcoded `agp8`) + `libs.versions.toml` hash. The JDK and AGP versions are embedded in the key label string since they change rarely — the `libs.versions.toml` hash captures dep bumps.

### Decision 9 — DEVICE_TESTING.md Update Scope
**D-09:** Add 2 sections to `DEVICE_TESTING.md`:

1. **Phase 2 — Platform Compliance Checks** (after existing smoke test steps):
   - Cold launch: verify splash screen appears (no white flash)
   - Player: enable Dev Options → Predictive back animations → verify preview on back swipe
   - Settings: verify Language row appears (Android 13+ only)
   - Gesture nav: verify no clipped content in Library filter sheet, MoreSheet

2. **Phase 3 — Performance Regression Check**:
   - Shuffle: play 10-scene shuffle, verify "End of queue" banner appears (not silent stop)
   - Player: verify frame rate setting applies on video start (check for jank-free first frame)

### Decision 10 — Commit Budget
**D-10:** Target ≤ 12 atomic commits across all 3 plans:
- Plan 4.1: PlayerScreen split (1), domain interfaces (1), ConnectionResult retire (1), Throwable narrowing (1) = 4
- Plan 4.2: test framework wiring (1), seed core tests (1), seed ViewModel tests (1), seed Compose smoke tests (1) = 4
- Plan 4.3: lint shrink (1), detekt re-baseline (1), CI workflow (1), docs + hygiene (1) = 4

### Accepted Risks
**AR-04-01:** PlayerScreen split may reveal hidden state-passing dependencies. If the split causes ≥ 3 deviation-class bugs (Rule 1), defer the full split and do a partial split (just `TimelineBar` extraction) per REVIEWS-C4 ACCEPT.

**AR-04-02:** Lint baseline shrink ≥ 30% may not be achievable within 1 plan if the issues require invasive fixes. If ≤ 250 lines can be removed without risk, accept at 25% reduction and document the remaining gap.

### Claude's Discretion
- Exact setters needed in `PlayerSettings`/`UiSettings` interfaces (expose all currently used by features)
- Test assertion depth for seed tests (just `assertNotNull(state)` for basic wiring; deeper only if the ViewModel has non-trivial initial state)
- Whether `:feature:library`'s `:core:data` dep was ONLY for `UiPreferences` or has other uses (read `feature/library/build.gradle.kts` to confirm before removing)
- Robolectric SDK version for smoke tests (`@Config(sdk = [33])` unless a lower version compiles cleaner)

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase 4 Spec and Context
- `.planning/phases/04-polish-test-pyramid/04-SPEC.md` — Locked requirements + boundaries (MANDATORY)
- `.planning/REQUIREMENTS.md` — POLISH-01..10 full requirement text
- `.planning/ROADMAP.md` — Phase 4 section with 3 suggested plans

### Prior Phase Artifacts (constraint inheritance)
- `.planning/phases/01-deps-foundation-bump/01-CONTEXT.md` — AGP 8.7.3 / Gradle 8.11.1 floor
- `.planning/phases/02-comply-platform-compliance/02-CONTEXT.md` — Platform compliance decisions
- `.planning/phases/03-perf-measured-wins/03-CONTEXT.md` — ImmutableList, LaunchedEffect audit, applyVideoFrameRate patterns that must survive PlayerScreen split

### Key Implementation Files
- `feature/player/src/main/java/io/stashapp/android/feature/player/PlayerScreen.kt` — 1227-line target of POLISH-01 split
- `core/model/src/main/java/io/stashapp/android/core/model/Connection.kt` — `ConnectionResult` sealed class (delete in POLISH-07)
- `core/common/src/main/java/io/stashapp/android/core/common/Result.kt` — `AppResult`/`AppError` definitions
- `core/data/src/main/java/io/stashapp/android/core/data/prefs/PlayerPreferences.kt` — source of `PlayerSettings` interface scope
- `core/data/src/main/java/io/stashapp/android/core/data/prefs/UiPreferences.kt` — source of `UiSettings` interface scope
- `core/data/src/main/java/io/stashapp/android/core/data/scene/DefaultSceneRepository.kt` — 4 `catch (e: Throwable)` sites (POLISH-07)
- `core/data/src/main/java/io/stashapp/android/core/data/scene/ScenePagingSource.kt` — 1 `catch (e: Throwable)` site
- `build-logic/convention/src/main/kotlin/io/stashapp/android/buildlogic/AndroidLibraryConventionPlugin.kt` — target of POLISH-04 test dep wiring
- `gradle/libs.versions.toml` — add JUnit5/MockK/Turbine/Robolectric versions
- `app/lint-baseline.xml` — 1001 lines, target ≤ 700

### Codebase Analysis
- `.planning/codebase/CONCERNS.md` — `catch (e: Throwable)` and `PlayerScreen.kt` large file concerns relevant to POLISH-07 and POLISH-01
- `.planning/phases/03-perf-measured-wins/03-LEARNINGS.md` — ImmutableList migration lessons apply to POLISH-01 (don't forget consumer composables)

</canonical_refs>

<code_context>
## Codebase Context

### Current PlayerScreen.kt Composable Structure (from scout)
```
line 123: fun PlayerScreen(...)         ← entry point + state wiring
line 478: private fun PlayerControls(...)  ← controls overlay
line 716-942: private fun AspectRatio*(), Speed*, Codec*, Queue*, Subtitle* helpers
line 943: private fun BannerPill(...)
line 984: private fun MarkerSeekCallout(...)
line 1009: private fun TimelineBar(...)
```

Split: `PlayerScreen.kt` keeps lines 123–477; `PlayerControls.kt` gets lines 478–942 + helpers; `PlayerTimeline.kt` gets lines 943–1227.

### ConnectionResult → AppResult Mapping
```kotlin
ConnectionResult.Success(info)    → AppResult.Success(info)
ConnectionResult.InvalidUrl(r)    → AppResult.Failure(AppError.Unknown(r))
ConnectionResult.AuthFailed(m)    → AppResult.Failure(AppError.Auth(m))
ConnectionResult.NetworkError(m)  → AppResult.Failure(AppError.Network(m))
ConnectionResult.ServerError(m)   → AppResult.Failure(AppError.Server(m))
```

### Feature → Core:Data Direct Deps (POLISH-06 scope)
- `feature/player/build.gradle.kts`: has `implementation(project(":core:data"))` — for `PlayerPreferences`
- `feature/settings/build.gradle.kts`: has `implementation(project(":core:data"))` — for `UiPreferences`
- `feature/library/build.gradle.kts`: has `implementation(project(":core:data"))` — for `UiPreferences` (verify before removing)

### Existing Test Infrastructure (for POLISH-04 baseline)
- `junit = "4.13.2"` already in catalog (keep — used by `baselineprofile`)
- No JUnit5, no MockK, no Turbine, no Robolectric in catalog yet
- `useJUnitPlatform()` NOT in any convention plugin

### Pitfalls (from prior phases)
- **ImmutableList consumer scope (Phase 3 DEV-01):** When splitting PlayerScreen.kt, ALL composables that call `PlayerControls` or `TimelineBar` must pass `ImmutableList<Marker>`. Check call sites in the entry-point file.
- **Lambda naming (Phase 3):** `grid?.let { uiObj -> ... }` pattern if any nested lambdas are in the extracted files.
- **Anti-coupling rule:** `grep -rn 'import .*Spine' feature/ core/ app/` → 0 hits must still hold after split.

</code_context>
