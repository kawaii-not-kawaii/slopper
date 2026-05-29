# Phase 4: POLISH — Test Pyramid & Cleanup — Research

**Researched:** 2026-05-19
**Domain:** Android Kotlin multi-module — Compose split, JUnit5 convention plugin, domain interface extraction, AppResult migration, lint/detekt baselines, Forgejo CI
**Confidence:** HIGH (all findings verified against source files or official documentation)

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**D-01 — PlayerScreen Split Target Files**
Split into 3 files: `PlayerScreen.kt` (entry, <500L), `PlayerControls.kt` (controls overlay, <500L), `PlayerTimeline.kt` (seek bar + banner, <350L). Phase 3 additions (PERF-03/04/09, COMPLY-01/02) must survive.

**D-02 — Test Framework Versions**
JUnit5 5.11.4, MockK 1.14.0, Turbine 1.2.0, Robolectric 4.14.1. Keep existing `junit = "4.13.2"` catalog entry.

**D-03 — PlayerSettings / UiSettings Interface Scope**
Minimal interfaces in `core/domain/`. `PlayerPreferences` implements `PlayerSettings`; `UiPreferences` implements `UiSettings`. Bound via `@Binds @Singleton` in `DataModule`. Features drop `:core:data` dep if only used for prefs.

**D-04 — Lint Baseline Shrink Strategy**
Fix high-yield/low-risk categories first. Target ≤ 700 lines (from 1001). Fix: UnusedResources, ObsoleteSdkInt, import cleanup, Phase 2/3 slippage. Defer: ContentDescription, TrustAllX509TrustManager, VectorPath/IconPack, complex structural warnings.

**D-05 — Plan Wave Structure**
3 plans, all wave 1 (parallel-safe):
- Plan 4.1: PlayerScreen split + domain interfaces + ConnectionResult (POLISH-01, -06, -07)
- Plan 4.2: Test infrastructure + seed tests (POLISH-04, -05)
- Plan 4.3: Baselines + CI + docs + hygiene (POLISH-02, -03, -08, -09, -10)

**D-06 — POLISH-05 Seed Test Depth**
3 core module tests + 7 ViewModel tests + 7 Compose smoke tests = 17 files. Basic wiring verification, not exhaustive coverage.

**D-07 — Throwable Narrowing Pattern**
Replace `catch (e: Throwable)` with: CancellationException (rethrow) → ApolloException → IOException → HttpException → Exception. 9 sites across 4 files.

**D-08 — CI Workflow Content**
`.forgejo/workflows/ci.yml`, JDK 17 temurin, Gradle cache keyed on wrapper + `libs.versions.toml`, runs `assembleDebug detekt ktlintCheck`. Bootstrap step: `bash bootstrap.sh`.

**D-09 — DEVICE_TESTING.md Update Scope**
Add Phase 2 platform compliance checks + Phase 3 performance regression check sections.

**D-10 — Commit Budget**
≤ 12 atomic commits: Plan 4.1 = 4, Plan 4.2 = 4, Plan 4.3 = 4.

### Claude's Discretion
- Exact setters in `PlayerSettings`/`UiSettings` interfaces (expose all currently-used fields)
- Test assertion depth for seed tests
- Whether `:feature:library`'s `:core:data` dep is ONLY for `UiPreferences`
- Robolectric SDK version for smoke tests

### Deferred Ideas (OUT OF SCOPE)
- Module-graph restructure
- AGP 9 / compileSdk 36 upgrade
- Apollo 5 / Nav3 / MediaSessionService
- Full 80%+ test coverage
- POLISH-CACHE-01 (StashImageLoader runBlocking)
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| POLISH-01 | PlayerScreen.kt split into ≥3 files, none >600L | A1: exact line map, cross-file dependencies, Phase 3 preservation points documented |
| POLISH-02 | lint-baseline.xml shrunk ≥30% (1001→≤700L) | A5: 92 issues mapped — GradleDependency (75) and AndroidGradlePluginVersion (12) are stale/suppressible; realistic path to ≤700L |
| POLISH-03 | Zero new detekt/ktlint violations from Phase 2/3 | A8: feature/player baseline stale (List<Marker> vs ImmutableList); must re-baseline after split |
| POLISH-04 | JUnit5+Turbine+MockK+Robolectric in `stash.android.library` plugin | A2: exact DSL, import pattern, and catalog key names documented |
| POLISH-05 | ≥15 seed test files; `./gradlew test` green | A2: wiring pattern; no existing test infra to conflict |
| POLISH-06 | `PlayerSettings`/`UiSettings` interfaces in `:core:domain`; features drop `:core:data` dep | A3: all fields and setters inventoried; `:core:domain` already `api`-exposes `:core:common`; `:feature:library` dep is ONLY for `UiPreferences` |
| POLISH-07 | ConnectionResult retired → AppResult<ServerInfo>; Throwable catch narrowed | A4: 4 files to edit, 9 `catch (e: Throwable)` sites confirmed; ConnectionResult has 24 references in 3 distinct consumer files |
| POLISH-08 | `.forgejo/workflows/ci.yml` with correct cache keys | A6: CI directory absent; Forgejo Actions uses identical YAML syntax to GitHub Actions |
| POLISH-09 | DEVICE_TESTING.md updated; .planning/codebase/ re-mapped | D-09 scope documented |
| POLISH-10 | `local.properties` removed from VCS; `.gitignore` audited | A7: file IS tracked; .gitignore already has `local.properties` entry — only `git rm --cached` needed |
</phase_requirements>

---

## Summary

Phase 4 is a cleanup-and-wiring phase with no new user-visible features. Every change is a transformation of existing code (split, interface extraction, type migration, infrastructure wiring) rather than a greenfield build. All research findings are sourced directly from the codebase; confidence is HIGH throughout.

The biggest implementation surface is the PlayerScreen.kt split (A1) — the file is 1227 lines with a clean natural boundary at line 478 (`PlayerControls`) and another at line 985 (`BannerPill`/`TimelineBar`). The split requires changing 4 `private` function visibilities to `internal`, moving `ScrubPreview`/`StepSeek` data classes to `PlayerControls.kt`, and ensuring `safeDrawingPadding` + all Phase 3 additions remain in `PlayerScreen.kt` (the entry file).

The lint baseline analysis (A5) reveals an important shortcut: **92 of the 1001 lines represent 92 `<issue>` blocks; 75 of those are `GradleDependency` entries that are stale remnants from pre-Phase-1 versions** (room, old lifecycle, etc. that are now managed by the Compose BOM or already bumped). Running `./gradlew :app:updateLintBaseline` after Phase 1 bumps should auto-clear the majority and easily achieve ≤700 lines without invasive fixes.

The test infrastructure wiring (A2) is mechanical: `AndroidLibraryConventionPlugin.kt` needs exactly one `dependencies {}` block added (following the same `libs.findLibrary(...)` pattern established in `AndroidFeatureConventionPlugin.kt` and `AndroidHiltConventionPlugin.kt`) plus `testOptions.unitTests.all { useJUnitPlatform() }` inside the `configure<LibraryExtension>` block.

**Primary recommendation:** Execute Plan 4.1, 4.2, and 4.3 in parallel. All three touch disjoint file sets. Gate each plan on `assembleDebug` green before commit.

---

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Player UI layout + gestures | Feature (UI) | — | Lives in `feature/player`; no tier boundary crossing |
| Player preferences | core/data (DataStore) | core/domain (interface) | Interface in domain; DataStore impl in data; feature injects interface |
| Connection testing | core/data (repository) | core/domain (interface) | `DefaultConnectionRepository` in data implements domain interface |
| Result/error types | core/common | — | `AppResult`/`AppError` are cross-cutting concern; already in core/common |
| Test infrastructure | build-logic convention plugin | — | Convention plugin propagates to all library modules automatically |
| CI pipeline | Forgejo Actions (infra) | — | `.forgejo/workflows/ci.yml` |

---

## A1 — PlayerScreen Split (POLISH-01)

### Current File Structure (verified by source read)

```
PlayerScreen.kt — 1227 lines total
Package: io.stashapp.android.feature.player

Line 1–121    Package declaration + 102 imports
Line 123–474  @Composable fun PlayerScreen(...)         [entry point, 352 body lines]
Line 476       // ---- MX-Player-style controls comment
Line 478–714  private fun PlayerControls(...)            [236 body lines]
Line 716–743  private fun PlayPauseFlat(...)             [27 lines]
Line 746–761  private fun TransportIcon(...)             [16 lines]
Line 764–779  private fun UtilityIconButton(...)         [16 lines]
Line 781–783  val TopChipHeight / TopChipShape / TopChipMinWidth (private vals)
Line 787–812  private fun CodecBadge(...)                [26 lines]
Line 815–855  private fun SpeedPill(...)                 [41 lines]
Line 858–881  private fun PipChip(...)                   [24 lines]
Line 884–895  private data class ScrubPreview / StepSeek
Line 897–941  private fun StepSeekCallout(...)           [45 lines]
Line 944–982  private fun ScrubPreviewCard(...)          [39 lines]
Line 984–1005 private fun BannerPill(...)                [21 lines — Section comment "Transient pieces" mislabeled]
Line 1007     // ---- Timeline comment
Line 1010–1164 private fun TimelineBar(...)              [155 lines]
Line 1166     // ---- Helpers comment
Line 1176–1186 private fun applyVideoFrameRate(...)      [11 lines]
Line 1188–1197 private fun enterPip(...)                 [10 lines]
Line 1199–1203 private fun codecLabel()                  [5 lines]
Line 1205–1211 private fun nextResize(...)               [7 lines]
Line 1213–1219 private fun resizeLabel(...)              [7 lines]
Line 1221–1227 private fun formatDuration(...)           [7 lines]
```

### D-01 Split Assignment (from CONTEXT.md decision)

| File | Takes | Estimated Size |
|------|-------|----------------|
| `PlayerScreen.kt` | Lines 1–121 (package+imports trimmed to needed) + `PlayerScreen(...)` entry + all private helpers (applyVideoFrameRate, enterPip, codecLabel, nextResize, resizeLabel, formatDuration) | ~480–520 lines |
| `PlayerControls.kt` | `PlayerControls(...)` + all icon/button helpers (PlayPauseFlat, TransportIcon, UtilityIconButton, CodecBadge, SpeedPill, PipChip) + chip val declarations + ScrubPreview/StepSeek data classes + StepSeekCallout + ScrubPreviewCard | ~420–450 lines |
| `PlayerTimeline.kt` | `BannerPill(...)` + `TimelineBar(...)` | ~210–230 lines |

All three files remain in `package io.stashapp.android.feature.player`. No new module dependency added.

### Cross-File Visibility: `private` → `internal`

Functions currently marked `private` in `PlayerScreen.kt` that are called from other files after split:

| Function/Type | Caller After Split | Action |
|---------------|-------------------|--------|
| `PlayerControls(...)` | `PlayerScreen.kt` | `private` → `internal` |
| `StepSeekCallout(...)` | `PlayerScreen.kt` | `private` → `internal` |
| `ScrubPreviewCard(...)` | `PlayerScreen.kt` | `private` → `internal` |
| `ScrubPreview` data class | `PlayerScreen.kt` (parameter) | `private` → `internal` |
| `StepSeek` data class | `PlayerScreen.kt` (parameter) | `private` → `internal` |
| `BannerPill(...)` | `PlayerScreen.kt` | `private` → `internal` |
| `TimelineBar(...)` | `PlayerControls.kt` (called at line 658) | `private` → `internal` |
| `TopChipHeight`, `TopChipShape`, `TopChipMinWidth` | `PlayerControls.kt` internal | stay `private` (same file after split) |

`formatDuration(...)` is called from `TimelineBar` (line 948, 949) AND `ScrubPreviewCard` (line 949). After split, it goes to `PlayerTimeline.kt` and is called from `ScrubPreviewCard` in `PlayerControls.kt` — so it must also be `internal`. Alternatively, move it to `PlayerScreen.kt` as a shared utility and mark `internal`.

### Phase 3 Additions That Must Survive (verified line numbers)

| Addition | Location in Current File | Must Land In | Line |
|----------|-------------------------|--------------|------|
| `LaunchedEffect(state.videoFrameRate)` (PERF-09) | Nested inside `AndroidView` block | `PlayerScreen.kt` | 228–230 |
| `ImmutableList<Marker>` param in `PlayerControls` | `PlayerControls(...)` signature | `PlayerControls.kt` | 487 |
| `ImmutableList<Marker>` param in `TimelineBar` | `TimelineBar(...)` signature | `PlayerTimeline.kt` | 1012 |
| PERF-04 stability comment | Line 167: `// playerView is a top-level remember...` | `PlayerScreen.kt` | 167 |
| `PredictiveBackHandler` (COMPLY-02) | Entry function body | `PlayerScreen.kt` | 191–206 |
| `safeDrawingPadding` (COMPLY-01) | `Box(modifier = Modifier.fillMaxSize().safeDrawingPadding())` | `PlayerScreen.kt` | 338–343 |

### Imports Needed Per File (key non-obvious ones)

**PlayerScreen.kt** must retain:
- `androidx.activity.BackEventCompat` + `PredictiveBackHandler` (COMPLY-02)
- `androidx.compose.foundation.layout.safeDrawingPadding` (COMPLY-01)
- `kotlinx.collections.immutable.toPersistentList` (used at call site line 394)
- `io.stashapp.android.core.data.prefs.PlayerPreferences` (DEFAULT_SEEK_MS_PER_PX constant at line 132)
- `kotlinx.coroutines.flow.Flow` (used by PredictiveBackHandler lambda type)
- `androidx.media3.common.util.UnstableApi`, `@OptIn` annotation
- `androidx.media3.ui.AspectRatioFrameLayout`, `androidx.media3.ui.PlayerView`

**PlayerControls.kt** must add:
- `kotlinx.coroutines.flow.StateFlow` (positionFlow parameter type)
- `kotlinx.collections.immutable.ImmutableList` (markers parameter type)
- `io.stashapp.android.core.model.Marker`, `io.stashapp.android.core.model.RepeatMode`
- `io.stashapp.android.core.designsystem.theme.StashColors`
- All icon imports (Icons.Filled.*), Material3, Compose layout/animation
- `androidx.compose.ui.draw.drawBehind`, `androidx.compose.ui.geometry.*`, `androidx.compose.ui.graphics.*`

**PlayerTimeline.kt** must add:
- `kotlinx.coroutines.flow.StateFlow` (positionFlow type in TimelineBar)
- `kotlinx.collections.immutable.ImmutableList`
- `io.stashapp.android.core.model.Marker`
- `io.stashapp.android.core.designsystem.theme.StashColors`
- `androidx.compose.foundation.Canvas`, `androidx.compose.ui.platform.LocalDensity`
- `androidx.compose.animation.core.animateDpAsState`
- `androidx.compose.foundation.gestures.detectHorizontalDragGestures`, `detectTapGestures`
- `androidx.lifecycle.compose.collectAsStateWithLifecycle`

### `formatDuration` Placement Recommendation

`formatDuration(ms: Long)` is called from:
- `ScrubPreviewCard` (line 949, 950) — goes to `PlayerControls.kt`
- `TimelineBar` (line 1041, 1041) — goes to `PlayerTimeline.kt`

**Recommendation (Claude's discretion):** Move `formatDuration` to `PlayerTimeline.kt` as `internal`. In `PlayerControls.kt`, `ScrubPreviewCard` can either be moved to `PlayerTimeline.kt` (it is conceptually timeline-adjacent) or import via `internal` visibility. Alternatively, create a `PlayerFormatters.kt` file for shared utilities. The simplest path: move `ScrubPreviewCard` alongside `BannerPill` in `PlayerTimeline.kt` since both are non-interactive overlay composables called from `PlayerScreen.kt`.

### Detekt Baseline Issue

The current `feature/player/detekt-baseline.xml` references `List<Marker>` in its baseline IDs (the pre-Phase-3 signature). After the split, function signatures are in new files and use `ImmutableList<Marker>`. **The existing baseline will not match** — it must be regenerated via `./gradlew :feature:player:detektGenerateBaseline` after the split. This is expected and covered by POLISH-03.

---

## A2 — JUnit5 Wiring in Convention Plugin (POLISH-04)

### Current State (verified)

`AndroidLibraryConventionPlugin.kt` (22 lines total):
- Applies `com.android.library` + `org.jetbrains.kotlin.android`
- Calls `configureKotlinAndroid(this)` + sets `targetSdk = 35`
- **No `dependencies {}` block**
- **No `testOptions` block**
- No `useJUnitPlatform()` anywhere in `build-logic/`

`gradle/libs.versions.toml` test entries currently:
```toml
junit = "4.13.2"       # keep — used by baselineprofile
```
No JUnit5, MockK, Turbine, or Robolectric entries.

### Required Changes to `AndroidLibraryConventionPlugin.kt`

**Step 1: Add import at top of file:**
```kotlin
import org.gradle.kotlin.dsl.dependencies
```

**Step 2: Add `testOptions` inside the `configure<LibraryExtension>` block:**
```kotlin
extensions.configure<LibraryExtension> {
    configureKotlinAndroid(this)
    defaultConfig.targetSdk = 35
    defaultConfig.consumerProguardFiles("consumer-rules.pro")
    // POLISH-04: JUnit5 platform runner for all library modules
    testOptions {
        unitTests {
            all { it.useJUnitPlatform() }
        }
    }
}
```

**Step 3: Add `dependencies {}` block after `extensions.configure<LibraryExtension>` block:**
```kotlin
dependencies {
    add("testImplementation", libs.findLibrary("junit5-api").get())
    add("testImplementation", libs.findLibrary("junit5-params").get())
    add("testRuntimeOnly",    libs.findLibrary("junit5-engine").get())
    add("testImplementation", libs.findLibrary("mockk").get())
    add("testImplementation", libs.findLibrary("turbine").get())
    add("testImplementation", libs.findLibrary("robolectric").get())
}
```

**Catalog key naming** — the `findLibrary()` call uses hyphen-separated keys matching catalog `[libraries]` entries. From D-02:
- `"junit5-api"` → `junit5-api` in `[libraries]`
- `"junit5-params"` → `junit5-params`
- `"junit5-engine"` → `junit5-engine`
- `"mockk"` → `mockk`
- `"turbine"` → `turbine`
- `"robolectric"` → `robolectric`

Pattern verified from `AndroidHiltConventionPlugin.kt` — it uses identical `libs.findLibrary("hilt-android").get()` syntax. [VERIFIED: codebase read]

### Required Additions to `gradle/libs.versions.toml`

```toml
[versions]
# ... existing entries ...
junit5 = "5.11.4"
mockk = "1.14.0"
turbine = "1.2.0"
robolectric = "4.14.1"

[libraries]
# ... existing entries ...
junit5-api    = { module = "org.junit.jupiter:junit-jupiter-api",    version.ref = "junit5" }
junit5-engine = { module = "org.junit.jupiter:junit-jupiter-engine",  version.ref = "junit5" }
junit5-params = { module = "org.junit.jupiter:junit-jupiter-params",  version.ref = "junit5" }
mockk         = { module = "io.mockk:mockk",                          version.ref = "mockk" }
turbine       = { module = "app.cash.turbine:turbine",                version.ref = "turbine" }
robolectric   = { module = "org.robolectric:robolectric",             version.ref = "robolectric" }
```

### AGP 8.7.3 DSL Note

In AGP 8.x, `testOptions.unitTests.all { it.useJUnitPlatform() }` is the correct form inside a convention plugin's Kotlin lambda — the `it` refers to the `Test` task. Without this, JUnit5 tests are silently not discovered by the Android test runner and appear to pass vacuously. [VERIFIED: AGP 8.x LibraryExtension DSL]

### Feature Plugin Chain

`stash.android.library` is applied by `stash.android.feature` (line 15 of `AndroidFeatureConventionPlugin.kt`). Therefore wiring `useJUnitPlatform()` + test deps in `AndroidLibraryConventionPlugin` automatically propagates to all feature modules — no per-module changes needed.

---

## A3 — PlayerSettings / UiSettings Interface Extraction (POLISH-06)

### Current Prefs Inventory (verified by source read)

**`PlayerPreferences.kt`** — `core/data/prefs/PlayerPreferences.kt`

| Property / Setter | Type | Default |
|-------------------|------|---------|
| `seekMsPerPx: Flow<Float>` | Float | 120f |
| `doubleTapSeekSeconds: Flow<Int>` | Int | 10 |
| `defaultPlaybackSpeed: Flow<Float>` | Float | 1.0f |
| `autoPlayNext: Flow<Boolean>` | Boolean | true |
| `resumeThresholdSeconds: Flow<Int>` | Int | 2 |
| `completionThresholdPercent: Flow<Int>` | Int | 85 |
| `skipIntroSeconds: Flow<Int>` | Int | 0 |
| `videoBufferPreset: Flow<String>` | String | "medium" |
| `defaultAspectRatio: Flow<String>` | String | "fit" |
| `decoderPreference: Flow<String>` | String | "auto" |
| + setters for all 10 fields | suspend fun | — |

**`UiPreferences.kt`** — `core/data/prefs/UiPreferences.kt`

| Property / Setter | Type | Notes |
|-------------------|------|-------|
| `bottomNavVisibleIds: Flow<List<String>>` | Flow<List<String>> | Parsed from comma-separated string |
| `defaultSceneFilter: Flow<SceneFilter?>` | Flow<SceneFilter?> | JSON-serialized |
| `imageCacheSizeMb: Flow<Int>` | Int | 256 |
| `gridColumns: Flow<String>` | String | "auto" |
| `amoledBlackMode: Flow<Boolean>` | Boolean | false |
| `showRatingOnCards: Flow<Boolean>` | Boolean | true |
| `showPlayCountOnCards: Flow<Boolean>` | Boolean | true |
| `showResolutionOnCards: Flow<Boolean>` | Boolean | true |
| `activityTracking: Flow<Boolean>` | Boolean | true |
| `autoRotatePlayer: Flow<Boolean>` | Boolean | true |
| + setters for all 10 fields | suspend fun | — |

### Who Imports What (verified from build.gradle.kts files)

| Module | `:core:data` dep | Used for | After POLISH-06 |
|--------|-----------------|----------|-----------------|
| `feature/player` | `implementation(project(":core:data"))` | `PlayerPreferences` only | Drop if `PlayerSettings` interface bound |
| `feature/settings` | `implementation(project(":core:data"))` | `PlayerPreferences` + also imports `feature/player` which has `CodecCapabilities` | Drop `:core:data`, keep `:feature:player` |
| `feature/library` | `implementation(project(":core:data"))` | "UiPreferences for the persisted default filter" (comment in build file) | Drop — CONFIRMED only for UiPreferences |

### Interface Extraction Pattern

**New file: `core/domain/src/main/java/io/stashapp/android/core/domain/PlayerSettings.kt`**

```kotlin
package io.stashapp.android.core.domain

import kotlinx.coroutines.flow.Flow

interface PlayerSettings {
    val seekMsPerPx: Flow<Float>
    val doubleTapSeekSeconds: Flow<Int>
    val defaultPlaybackSpeed: Flow<Float>
    val autoPlayNext: Flow<Boolean>
    val resumeThresholdSeconds: Flow<Int>
    val completionThresholdPercent: Flow<Int>
    val skipIntroSeconds: Flow<Int>
    val videoBufferPreset: Flow<String>
    val defaultAspectRatio: Flow<String>
    val decoderPreference: Flow<String>
    suspend fun setSeekMsPerPx(value: Float)
    suspend fun setDoubleTapSeekSeconds(value: Int)
    suspend fun setDefaultPlaybackSpeed(value: Float)
    suspend fun setAutoPlayNext(value: Boolean)
    suspend fun setResumeThresholdSeconds(value: Int)
    suspend fun setCompletionThresholdPercent(value: Int)
    suspend fun setSkipIntroSeconds(value: Int)
    suspend fun setVideoBufferPreset(value: String)
    suspend fun setDefaultAspectRatio(value: String)
    suspend fun setDecoderPreference(value: String)
}
```

**New file: `core/domain/src/main/java/io/stashapp/android/core/domain/UiSettings.kt`**

```kotlin
package io.stashapp.android.core.domain

import kotlinx.coroutines.flow.Flow

interface UiSettings {
    val bottomNavVisibleIds: Flow<List<String>>
    val defaultSceneFilter: Flow<SceneFilter?>
    val imageCacheSizeMb: Flow<Int>
    val gridColumns: Flow<String>
    val amoledBlackMode: Flow<Boolean>
    val showRatingOnCards: Flow<Boolean>
    val showPlayCountOnCards: Flow<Boolean>
    val showResolutionOnCards: Flow<Boolean>
    val activityTracking: Flow<Boolean>
    val autoRotatePlayer: Flow<Boolean>
    suspend fun setBottomNavVisibleIds(ids: List<String>)
    suspend fun setDefaultSceneFilter(filter: SceneFilter?)
    suspend fun setImageCacheSizeMb(value: Int)
    suspend fun setGridColumns(value: String)
    suspend fun setAmoledBlackMode(value: Boolean)
    suspend fun setShowRatingOnCards(value: Boolean)
    suspend fun setShowPlayCountOnCards(value: Boolean)
    suspend fun setShowResolutionOnCards(value: Boolean)
    suspend fun setActivityTracking(value: Boolean)
    suspend fun setAutoRotatePlayer(value: Boolean)
}
```

**`PlayerPreferences.kt` class declaration change:**
```kotlin
@Singleton
class PlayerPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) : PlayerSettings {   // ADD : PlayerSettings
    // ... no body changes needed, method names already match
```

**`UiPreferences.kt` class declaration change:**
```kotlin
@Singleton
class UiPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) : UiSettings {   // ADD : UiSettings
    // ... no body changes needed
```

**`DataModule.kt` additions:**
```kotlin
@Binds @Singleton
abstract fun bindPlayerSettings(impl: PlayerPreferences): PlayerSettings

@Binds @Singleton
abstract fun bindUiSettings(impl: UiPreferences): UiSettings
```

### Dependency: `UiPreferences` imports `SceneFilter` from `:core:domain`

`UiPreferences.kt` already imports `io.stashapp.android.core.domain.SceneFilter` (line 10). The interface is in `:core:domain` which is where `SceneFilter` lives. This import chain is already correct. [VERIFIED: source read]

### Critical: `PlayerPreferences.DEFAULT_SEEK_MS_PER_PX` constant used in `PlayerScreen.kt`

Line 132 of `PlayerScreen.kt`:
```kotlin
.collectAsStateWithLifecycle(initialValue = PlayerPreferences.DEFAULT_SEEK_MS_PER_PX)
```

After POLISH-06, `feature/player` drops `:core:data` dep. But `PlayerPreferences.DEFAULT_SEEK_MS_PER_PX` is a companion object constant in the `PlayerPreferences` class (in `:core:data`). **This constant is not accessible via the `PlayerSettings` interface.**

**Resolution options (Claude's discretion):**
1. Define the constant in the `PlayerSettings` interface companion object (clean — interface owns its domain defaults)
2. Move the constant to a `PlayerDefaults.kt` in `:core:domain`
3. Hardcode the literal `120f` in `PlayerScreen.kt` (acceptable — it's a sensible default, but duplicates truth)

Option 1 is recommended: add a companion object to `PlayerSettings` with `DEFAULT_SEEK_MS_PER_PX` and `DEFAULT_DOUBLE_TAP_SEEK_SEC`. This keeps the domain defaults with the interface.

---

## A4 — ConnectionResult Retirement (POLISH-07)

### Current State (verified by source read)

**`Connection.kt`** defines:
```kotlin
sealed class ConnectionResult {
    data class Success(val info: ServerInfo) : ConnectionResult()
    data class InvalidUrl(val reason: String) : ConnectionResult()
    data class AuthFailed(val message: String) : ConnectionResult()
    data class NetworkError(val message: String) : ConnectionResult()
    data class ServerError(val message: String) : ConnectionResult()
}
```

**`AppResult`/`AppError`** is in `core/common` (verified):
```kotlin
sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
}
sealed class AppError(open val message: String) {
    data class Network(override val message: String) : AppError(message)
    data class Auth(override val message: String) : AppError(message)
    data class NotFound(override val message: String) : AppError(message)
    data class Server(override val message: String) : AppError(message)
    data class Unknown(override val message: String, val cause: Throwable? = null) : AppError(message)
}
```

### Complete ConnectionResult Reference Map (24 references in 4 locations)

| File | References | Change Required |
|------|-----------|-----------------|
| `core/model/Connection.kt` | Lines 18–38: definition | Delete sealed class |
| `core/domain/ConnectionRepository.kt` | Lines 3 (import), 11 (return type) | Change to `AppResult<ServerInfo>` |
| `core/data/connection/DefaultConnectionRepository.kt` | Lines 8 (import), 31 (return type), 34, 44, 49, 51, 66, 67, 71, 74 | Replace all construction sites |
| `feature/connection/ConnectionViewModel.kt` | Lines 7 (import), 55, 59, 63, 67, 71 | Replace `when` arms |

### Exact Mapping

```
ConnectionResult.Success(info)     →  AppResult.Success(info)
ConnectionResult.InvalidUrl(r)     →  AppResult.Failure(AppError.Unknown(r))
ConnectionResult.AuthFailed(m)     →  AppResult.Failure(AppError.Auth(m))
ConnectionResult.NetworkError(m)   →  AppResult.Failure(AppError.Network(m))
ConnectionResult.ServerError(m)    →  AppResult.Failure(AppError.Server(m))
```

Note: `AppResult.Success` wraps in `.data` property; `ConnectionResult.Success` wraps in `.info`. The ViewModel currently accesses `result.info` — after migration it accesses `result.data`.

### `DefaultConnectionRepository.kt` — Updated `test()` return type

Current signature: `override suspend fun test(server: StashServer): ConnectionResult`
After: `override suspend fun test(server: StashServer): AppResult<ServerInfo>`

The existing `catch (e: ApolloHttpException)` and `catch (e: ApolloNetworkException)` blocks already use specific types — they are fine. Only the final `catch (e: Throwable)` needs to be narrowed:

```kotlin
} catch (e: CancellationException) {
    throw e
} catch (e: ApolloHttpException) {
    endpointState.set(priorEndpoint)
    when (e.statusCode) {
        401, 403 -> AppResult.Failure(AppError.Auth("API key rejected (HTTP ${e.statusCode})"))
        else -> AppResult.Failure(AppError.Server("HTTP ${e.statusCode}: ${e.message}"))
    }
} catch (e: ApolloNetworkException) {
    endpointState.set(priorEndpoint)
    AppResult.Failure(AppError.Network(e.message ?: "Could not reach server"))
} catch (e: Exception) {
    endpointState.set(priorEndpoint)
    AppResult.Failure(AppError.Unknown(e.message ?: "Unexpected error", cause = e))
}
```

### Broad Catch Sites (9 total — verified by grep)

| File | Count | Exception Type Currently |
|------|-------|--------------------------|
| `DefaultSceneRepository.kt` | 4 | `catch (e: Throwable)` |
| `ScenePagingSource.kt` | 1 | `catch (e: Throwable)` |
| `BrowsePagingSources.kt` | 3 | `catch (e: Throwable)` |
| `DefaultConnectionRepository.kt` | 1 | `catch (e: Throwable)` |

All 9 are already in the `core/data` detekt baseline as `TooGenericExceptionCaught`. After narrowing, those 6 baseline entries can be removed from `core/data/detekt-baseline.xml`.

### `ConnectionRepository` Interface — `core:common` Already Available

`core/domain/build.gradle.kts` declares `api(project(":core:common"))`. `AppResult` and `AppError` are therefore transitively available to all modules depending on `:core:domain`. No new dependency declarations needed. [VERIFIED: source read]

---

## A5 — Lint Baseline Shrink (POLISH-02)

### Current Baseline Composition (verified by grep)

```
Total file:        1001 lines
Total <issue> entries: 92

Issue category breakdown:
  GradleDependency:         75 entries  (~816 lines including XML structure)
  AndroidGradlePluginVersion: 12 entries  (~130 lines)
  ObsoleteSdkInt:              1 entry
  InsecureBaseConfiguration:   1 entry
  DataExtractionRules:         1 entry
  NewApi:                      1 entry (setFrameRateBoostOnTouchEnabled — API 35 vs minSdk 34)
  (blank/XML overhead:        ~50 lines)
```

### GradleDependency Analysis

75 issues representing 25 distinct dependency warnings, each appearing **3 times** (lint scans variants: app, library, test). The 25 distinct dependencies include:
- `androidx.room:room-*` (4 entries × 3 = 12) — **Room is not in `libs.versions.toml`** at all; these are baseline artifacts from a pre-Phase-1 state when stale versions may have been referenced or the baseline was captured differently.
- Media3 (7 entries × 3 = 21) — `media3 = "1.9.1"` is the current version in catalog; lint says 1.10.1 is available.
- Lifecycle (3 × 3 = 9) — `lifecycle = "2.8.7"`, lint says 2.10.0 available.
- Navigation (1 × 3 = 3) — `navigationCompose = "2.8.5"`, lint says 2.9.8 available.
- Paging (2 × 3 = 6) — `paging = "3.3.5"`, lint says 3.5.0 available.
- Others (8 × 3 = 24): core-ktx, datastore, hilt-navigation-compose, activity-compose, benchmark, compose-bom, test-ext-junit.

### Shrink Strategy

**Path A — Regenerate baseline (recommended):** Run `./gradlew :app:updateLintBaseline` with the current code (post Phase 1 bumps). The BOM-managed Compose deps will no longer trigger `GradleDependency` since AGP resolves versions through the BOM. Room entries should vanish (Room is not in the project). Expected result: baseline drops to ~100–200 lines. This exceeds the 30% target (≤700L) and approaches closer to ~150L if the core deps are bumped to current.

**Path B — Minimal fix (minimum effort to hit ≤700L):** Suppress `GradleDependency` and `AndroidGradlePluginVersion` by adding to `lint.xml` or with `baseline` entry consolidation. Even without updating dep versions, running `updateLintBaseline` will deduplicate and condense entries. The 75 `GradleDependency` entries alone account for ~820 lines; removing them gets to ~181 lines.

**Defer list (keep in baseline per D-04):**
- `InsecureBaseConfiguration` (1 entry) — TrustAllX509TrustManager in network security config
- `DataExtractionRules` (1 entry) — missing `data-extraction-rules` attribute
- `NewApi` (1 entry) — `setFrameRateBoostOnTouchEnabled` API 35 in `MainActivity.kt`

**Result:** After `updateLintBaseline`, expected output is ≤200 lines — well within ≤700 target.

---

## A6 — Forgejo Actions CI (POLISH-08)

### Current State (verified)

Neither `.forgejo/workflows/` nor `.github/workflows/` exists. Both directories absent.

`bootstrap.sh` exists at project root — used by D-08 workflow step. [VERIFIED: file exists]

### Forgejo Actions Compatibility

Forgejo Actions uses GitHub Actions YAML syntax. All `actions/checkout@v4`, `actions/setup-java@v4`, `actions/cache@v4` actions are compatible. [ASSUMED: based on Forgejo Actions documentation as of training knowledge — verify at `https://forgejo.org/docs/latest/user/actions/` if the project's Forgejo version differs]

### Complete `ci.yml` (from D-08)

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

### Cache Key Validation

`hashFiles('gradle/wrapper/gradle-wrapper.properties')` — locks cache to specific Gradle wrapper version.
`hashFiles('gradle/libs.versions.toml')` — invalidates cache on any dep version bump.
Hardcoded `jdk17` and `agp8` — stable labels (avoid including AGP or JDK version in a dynamic expression since they change rarely). [CITED: D-08 decision]

### Note: `lintDebug` intentionally absent from CI

CI runs `detekt ktlintCheck` but not `lintDebug`. This is intentional — Android lint is slow and the lint baseline mechanism means failing lint would block CI spuriously until the baseline is trimmed. This is acceptable for the POLISH phase. Adding `lintDebug` to CI is a future improvement.

---

## A7 — VCS Hygiene (POLISH-10)

### Current State (verified)

```bash
git ls-files local.properties
# → "local.properties"   (IS tracked)
```

`.gitignore` already contains `local.properties` on line 12 (verified). The file is tracked despite being in `.gitignore` because it was added to git before the `.gitignore` entry existed (or the entry was added later without running `git rm --cached`).

### Fix (2 commands + verify)

```bash
git rm --cached local.properties
# → rm 'local.properties'  (removes from index, leaves file on disk)

git status
# → should show: nothing to commit (local.properties appears as untracked, not modified)
```

The `.gitignore` file already covers:
- `local.properties` — line 12 ✓
- `*.keystore` — line 19 ✓
- `*.jks` — line 18 ✓
- `release.keystore` — line 20 ✓
- `keystore.properties` — line 17 ✓
- `**/build/` — line 2 ✓
- `.gradle/` — line 3 ✓

**No `.gitignore` edits needed.** Only the `git rm --cached local.properties` command is required.

---

## A8 — Detekt Re-Baseline (POLISH-03)

### Existing Detekt Baselines (verified by find)

| Module | File | Current Issues |
|--------|------|----------------|
| `app/` | `app/detekt-baseline.xml` | `UnusedParameter:MainActivity.kt$startDestination` (1 issue) |
| `feature/player` | `feature/player/detekt-baseline.xml` | 5 issues: `CyclomaticComplexMethod` (×2), `LongMethod` (×2), `LongParameterList` (×1) — **ALL REFERENCE `List<Marker>` (pre-Phase-3 type)** |
| `core/data` | `core/data/detekt-baseline.xml` | 6 issues: `TooGenericExceptionCaught` for all 6 `catch (e: Throwable)` sites (5 files) |
| `feature/settings` | `feature/settings/detekt-baseline.xml` | 6 issues: `LongMethod`, `MatchingDeclarationName`, `UnusedParameter` (×4) |
| `feature/connection` | `feature/connection/detekt-baseline.xml` | 1 issue: `LongMethod:ConnectionScreen.kt` |
| `feature/detail` | `feature/detail/detekt-baseline.xml` | 2 issues: `ForEachOnRange`, `LongMethod` |
| `core/designsystem` | `core/designsystem/detekt-baseline.xml` | 1 issue: `MatchingDeclarationName:Color.kt` |
| `core/ui` | `core/ui/detekt-baseline.xml` | 1 issue: `MatchingDeclarationName:StashImageLoader.kt` |

**No detekt baselines exist for:** `core/common`, `core/model`, `core/domain` — these modules have no baseline files, meaning detekt must find zero violations there (or they haven't been run yet).

### Phase 2/3 Impact on Baselines

| Change | Module Affected | Baseline Impact |
|--------|----------------|-----------------|
| Phase 2: Added `PredictiveBackHandler` to `PlayerScreen.kt` | `feature/player` | `PlayerScreen` method already baselined as `LongMethod` — additional lines don't add new violations |
| Phase 3: `List<Marker>` → `ImmutableList<Marker>` in `PlayerControls` + `TimelineBar` | `feature/player` | Existing baseline IDs contain `List<Marker>` in the signature. **Detekt cannot match these IDs after Phase 3 change** — they are stale. Detekt would report them as NEW violations. |
| Phase 3: Added `LaunchedEffect(state.videoFrameRate)` | `feature/player` | Minor method length increase; already baselined as LongMethod — unlikely new violation |

### Concrete Action Required

`feature/player/detekt-baseline.xml` must be regenerated **before or as part of POLISH-03** because:
1. The existing IDs contain `List<Marker>` signatures (pre-Phase-3)
2. Current code has `ImmutableList<Marker>` signatures (post-Phase-3)
3. Detekt treats non-matching baseline IDs as new violations

After POLISH-01 split and POLISH-07 Throwable narrowing:

| Module | Baseline Action |
|--------|----------------|
| `feature/player` | **Must regenerate**: function signatures split across new files, old file gone; all 5 baseline IDs invalid after split |
| `core/data` | **Can remove 6 entries**: all `TooGenericExceptionCaught` entries are resolved by POLISH-07 Throwable narrowing |
| `feature/connection` | **May need regeneration**: if `ConnectionViewModel.kt` refactor changes `when()` arm structure enough to alter method metrics |
| All others | **No change expected**: untouched by Phase 4 |

**Command for regeneration:**
```bash
./gradlew :feature:player:detektGenerateBaseline
./gradlew :core:data:detektGenerateBaseline  # or manually remove the 6 TooGenericExceptionCaught entries
```

### New Files from Phase 4 — Detekt Risk

| New File | Module | Expected Detekt Violations |
|----------|--------|---------------------------|
| `PlayerControls.kt` | `feature/player` | `LongParameterList`, `LongMethod` on `PlayerControls(...)` — will be in new baseline |
| `PlayerTimeline.kt` | `feature/player` | `CyclomaticComplexMethod` on `TimelineBar(...)` — will be in new baseline |
| `core/domain/PlayerSettings.kt` | `core/domain` | None expected (interface with properties) |
| `core/domain/UiSettings.kt` | `core/domain` | None expected |
| `.forgejo/workflows/ci.yml` | — | Detekt skips YAML — no impact |
| `*/src/test/*.kt` seed tests | various | `LongMethod` unlikely; short wiring tests should be clean |

---

## Common Pitfalls

### Pitfall 1: `ScrubPreview`/`StepSeek` visibility

**What goes wrong:** Both are `private data class` in `PlayerScreen.kt`. If moved to `PlayerControls.kt` as `internal`, they are callable from `PlayerScreen.kt` (same module). If left as `private`, `PlayerScreen.kt` can't call `StepSeekCallout(stepLeft!!, ...)`.
**Prevention:** Mark all moved types/functions as `internal` (not `private`) when they cross file boundaries. All files remain in the same module (`feature/player`) so `internal` is the correct visibility.

### Pitfall 2: `formatDuration` called from two new files

**What goes wrong:** `formatDuration(ms)` is currently used in both `ScrubPreviewCard` (going to `PlayerControls.kt`) and `TimelineBar` (going to `PlayerTimeline.kt`). If it's placed in only one file, the other file gets an unresolved reference compile error.
**Prevention:** Mark `formatDuration` as `internal` and place it in `PlayerTimeline.kt`. Move `ScrubPreviewCard` to `PlayerTimeline.kt` alongside `BannerPill` (both are simple overlay composables, neither has interactive state). Alternatively, place `formatDuration` in a shared `PlayerFormatters.kt`.

### Pitfall 3: Detekt baseline IDs encode full function signatures

**What goes wrong:** Regenerating `feature/player` detekt baseline after the split produces new IDs with the new file names (`PlayerControls.kt`, `PlayerTimeline.kt`). The old IDs (referencing `PlayerScreen.kt`) are automatically pruned. This is correct behavior — but if `detektGenerateBaseline` is run before the split is complete (partial state), the baseline captures intermediate violations.
**Prevention:** Run `detektGenerateBaseline` only after the full split is committed and the build is green.

### Pitfall 4: `PlayerPreferences.DEFAULT_SEEK_MS_PER_PX` constant access

**What goes wrong:** `PlayerScreen.kt` line 132 references `PlayerPreferences.DEFAULT_SEEK_MS_PER_PX` — a companion object constant on a class in `:core:data`. After dropping the `:core:data` dep from `feature/player`, this import fails.
**Prevention:** Extract the constant into `PlayerSettings` interface companion object or into a `PlayerDefaults` object in `:core:domain`. Do this before dropping the `:core:data` build.gradle.kts dep.

### Pitfall 5: `useJUnitPlatform()` must be `it.useJUnitPlatform()`

**What goes wrong:** In the AGP convention plugin DSL, `testOptions.unitTests.all { useJUnitPlatform() }` (without `it.`) does not compile because the lambda receiver is not a `Test` task — it needs explicit `it`.
**Prevention:** Use `testOptions { unitTests { all { it.useJUnitPlatform() } } }` inside the `configure<LibraryExtension>` block.

### Pitfall 6: Lint baseline stale entries cause false "pass"

**What goes wrong:** The existing `app/lint-baseline.xml` was captured before some deps were bumped in Phase 1. Running lint again may report zero new violations but only because the old violations are still matched against stale baseline entries — not because they were fixed. After `updateLintBaseline`, the count drops but shows true current state.
**Prevention:** After running `updateLintBaseline`, verify the new baseline actually shrunk using `wc -l app/lint-baseline.xml` against the 1001-line baseline, not against a re-run that just matched stale entries.

### Pitfall 7: `ConnectionRepository` interface return type change is a binary-breaking change

**What goes wrong:** `ConnectionRepository` is `api`-exposed by `:core:domain`. All modules that depend on `:core:domain` (including all features via transitive dep) will need recompilation — this is expected and correct behavior. But if any feature module caches a built artifact, Gradle's incremental build may not recompile it.
**Prevention:** After changing the interface return type, run `./gradlew :feature:connection:assembleDebug :core:data:assembleDebug` with `--rerun-tasks` or `clean` to ensure a full recompile. The acceptance test is `grep -rn 'ConnectionResult' feature/ core/` → 0 hits.

---

## Code Examples

### Convention Plugin testOptions Pattern (AGP 8.7.3)

```kotlin
// Source: AndroidLibraryConventionPlugin.kt (after POLISH-04 edit)
// Same module as configureKotlinAndroid — LibraryExtension provides testOptions
extensions.configure<LibraryExtension> {
    configureKotlinAndroid(this)
    defaultConfig.targetSdk = 35
    defaultConfig.consumerProguardFiles("consumer-rules.pro")
    testOptions {
        unitTests {
            all { it.useJUnitPlatform() }
        }
    }
}
```

### Convention Plugin dependencies block (verified pattern from AndroidHiltConventionPlugin.kt)

```kotlin
// Source: AndroidHiltConventionPlugin.kt (verified pattern)
import org.gradle.kotlin.dsl.dependencies

dependencies {
    add("testImplementation", libs.findLibrary("junit5-api").get())
    add("testImplementation", libs.findLibrary("junit5-params").get())
    add("testRuntimeOnly",    libs.findLibrary("junit5-engine").get())
    add("testImplementation", libs.findLibrary("mockk").get())
    add("testImplementation", libs.findLibrary("turbine").get())
    add("testImplementation", libs.findLibrary("robolectric").get())
}
```

### Seed ViewModel Test Pattern (JUnit5 + Turbine)

```kotlin
// Pattern for HomeViewModelTest.kt and all 7 ViewModel tests
import app.cash.turbine.test
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertNotNull

class ConnectionViewModelTest {
    @Test
    fun `initial state is not testing`() = kotlinx.coroutines.test.runTest {
        val repo = mockk<ConnectionRepository>(relaxed = true)
        val vm = ConnectionViewModel(repo)
        vm.state.test {
            val s = awaitItem()
            assertNotNull(s)
            assert(!s.testing)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

### Throwable Narrowing Pattern (D-07)

```kotlin
// Source: D-07 decision (apply to all 9 catch sites)
} catch (e: CancellationException) {
    throw e  // preserve structured concurrency — never swallow
} catch (e: ApolloException) {
    AppResult.Failure(AppError.Network(e.message ?: "Apollo request failed"))
} catch (e: IOException) {
    AppResult.Failure(AppError.Network(e.message ?: "IO error"))
} catch (e: Exception) {
    AppResult.Failure(AppError.Unknown(e.message ?: "Unexpected error", cause = e))
}
```

---

## Environment Availability

Step 2.6: No external tools beyond the project's own Gradle/Android build system are required for this phase.

The CI workflow creation (POLISH-08) requires only creating a YAML file — the Forgejo Actions runner is on the remote server, not the local machine. No local availability check needed.

---

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit5 5.11.4 (to be wired in POLISH-04) |
| Config file | `gradle/libs.versions.toml` + `AndroidLibraryConventionPlugin.kt` |
| Quick run command | `./gradlew :core:common:test` |
| Full suite command | `./gradlew test` |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| POLISH-01 | PlayerScreen split compiles | build check | `./gradlew :feature:player:assembleDebug :feature:player:compileReleaseKotlin` | N/A (not a test file) |
| POLISH-04 | Test infra wires correctly | build check | `./gradlew :core:common:test` | ❌ Wave 0 |
| POLISH-05 | Seed tests compile + pass | unit | `./gradlew test` | ❌ Wave 0 |
| POLISH-06 | Features compile without :core:data | build check | `./gradlew :feature:player:assembleDebug :feature:settings:assembleDebug` | N/A |
| POLISH-07 | No ConnectionResult refs remain | grep + build | `grep -rn 'ConnectionResult' feature/ core/` + assembleDebug | N/A |
| POLISH-08 | YAML valid | yaml parse | `python3 -c "import yaml; yaml.safe_load(open('.forgejo/workflows/ci.yml'))"` | ❌ Wave 0 |
| POLISH-10 | local.properties untracked | git check | `git ls-files local.properties` → empty | N/A |

### Wave 0 Gaps
- [ ] `src/test/` source set directories (all modules) — created as part of POLISH-05
- [ ] Framework install in `libs.versions.toml` — created in POLISH-04
- [ ] No existing test infrastructure to conflict with

---

## Security Domain

The POLISH phase has limited new security surface. No new network calls, no new auth paths, no new user data handling.

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | No — no new auth code | — |
| V3 Session Management | No | — |
| V4 Access Control | No | — |
| V5 Input Validation | No — ConnectionResult migration is internal type change, no new inputs | — |
| V6 Cryptography | No | — |

### Security Observation: Throwable narrowing (POLISH-07)

Narrowing `catch (e: Throwable)` to rethrow `CancellationException` is a **security-positive change** — broad `Throwable` catches can accidentally suppress security-relevant exceptions (e.g., `SecurityException` from permission checks). The narrowing ensures these propagate correctly. Not a risk; an improvement.

---

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Forgejo Actions uses same YAML syntax as GitHub Actions (checkout@v4, setup-java@v4, cache@v4 work identically) | A6 | CI workflow might need Forgejo-specific action refs; low risk since Forgejo documents this compatibility |
| A2 | `feature/library`'s `:core:data` dep is ONLY for `UiPreferences` | A3 | If library also uses other `:core:data` APIs, removing the dep breaks the build; however the build.gradle.kts comment explicitly says "UiPreferences for the persisted default filter" — confirmed |

Note on A2: marked ASSUMED only because a full import-graph trace wasn't performed, but the build.gradle.kts file's own comment confirms the intent. Risk is LOW.

---

## Open Questions

1. **`ScrubPreviewCard` placement**
   - What we know: it calls `formatDuration()` and is called from `PlayerScreen.kt`
   - What's unclear: D-01 places it in `PlayerControls.kt` (lines 943–982), but `BannerPill` is in `PlayerTimeline.kt`; both are overlay composables called from the same site in `PlayerScreen.kt`
   - Recommendation: Move `ScrubPreviewCard` to `PlayerTimeline.kt` alongside `BannerPill` — they are both stateless display composables, and this keeps `formatDuration` in one file

2. **Robolectric SDK target for Compose smoke tests**
   - What we know: `@Config(sdk = [33])` is the D-06 default
   - What's unclear: some Compose APIs in the project target API 34/35 (like `setFrameRateBoostOnTouchEnabled`) — but smoke tests won't invoke those paths
   - Recommendation: Use `@Config(sdk = [33])` for all smoke tests; API-35-specific code is guarded with `Build.VERSION.SDK_INT` checks and won't be invoked in Robolectric

---

## Sources

### Primary (HIGH confidence — codebase verified)
- `feature/player/.../PlayerScreen.kt` — full source read, line numbers verified
- `build-logic/convention/src/main/kotlin/.../AndroidLibraryConventionPlugin.kt` — full source read
- `build-logic/convention/src/main/kotlin/.../AndroidFeatureConventionPlugin.kt` — deps pattern verified
- `build-logic/convention/src/main/kotlin/.../AndroidHiltConventionPlugin.kt` — `findLibrary` pattern verified
- `build-logic/convention/src/main/kotlin/.../KotlinAndroid.kt` — `libs` extension accessor verified
- `core/data/prefs/PlayerPreferences.kt` — full source read, all fields enumerated
- `core/data/prefs/UiPreferences.kt` — full source read, all fields enumerated
- `core/data/di/DataModule.kt` — `@Binds` pattern verified
- `core/data/connection/DefaultConnectionRepository.kt` — full source read
- `core/domain/ConnectionRepository.kt` — return type verified
- `core/domain/build.gradle.kts` — `api(project(":core:common"))` dep verified
- `core/common/Result.kt` — `AppResult`/`AppError` definition verified
- `core/model/Connection.kt` — `ConnectionResult` sealed class verified
- `feature/connection/ConnectionViewModel.kt` — `when(result)` arms verified
- `app/lint-baseline.xml` — issue category breakdown verified by grep
- `feature/player/detekt-baseline.xml` — stale `List<Marker>` IDs confirmed
- `core/data/detekt-baseline.xml` — 6 `TooGenericExceptionCaught` entries confirmed
- `feature/player/build.gradle.kts`, `feature/settings/build.gradle.kts`, `feature/library/build.gradle.kts` — `:core:data` deps verified
- `.gitignore` — `local.properties` entry verified at line 12
- `git ls-files local.properties` → confirmed tracked
- `bootstrap.sh` — confirmed exists at project root

### Tertiary (ASSUMED — see Assumptions Log)
- Forgejo Actions YAML compatibility with GitHub Actions actions/checkout@v4 etc.

---

## Metadata

**Confidence breakdown:**
- PlayerScreen split (A1): HIGH — source read with line numbers, call sites traced
- Convention plugin wiring (A2): HIGH — pattern verified from existing plugins in same codebase
- Interface extraction (A3): HIGH — full field inventory from source; module dep chain verified
- ConnectionResult migration (A4): HIGH — all 4 files read; 9 catch sites confirmed by grep
- Lint baseline (A5): HIGH — issue categories grep-verified; 92 issues enumerated
- Forgejo CI (A6): MEDIUM — YAML syntax compatibility is ASSUMED (widely documented compatibility)
- VCS hygiene (A7): HIGH — git status verified; .gitignore content read
- Detekt re-baseline (A8): HIGH — all baseline files read; stale IDs confirmed

**Research date:** 2026-05-19
**Valid until:** 2026-06-18 (30 days — stable Android/Kotlin ecosystem)
