# Phase 4: POLISH — Test Pyramid & Cleanup — Specification

**Created:** 2026-05-19
**Ambiguity score:** 0.168 (gate: ≤ 0.20)
**Requirements:** 10 locked

## Goal

Refactor `PlayerScreen.kt` into maintainable split files, shrink the lint baseline by ≥ 30%, wire the JUnit5/Turbine/MockK/Robolectric test infrastructure across all library modules, retire `ConnectionResult` in favor of `AppResult`, extract `PlayerSettings`/`UiSettings` interfaces into `:core:domain`, add Forgejo Actions CI, and close VCS hygiene gaps — all on the stable Phase 1–3 floor with a green build at every step.

## Background

**Current state (scouted 2026-05-19):**
- `feature/player/.../PlayerScreen.kt`: **1227 lines** (grew from original 1122 during Phase 2/3 changes — COMPLY added `PredictiveBackHandler`; PERF added `LaunchedEffect(state.videoFrameRate)` and ImmutableList)
- `app/lint-baseline.xml`: **1001 lines** (~300 findings grandfathered from Phase 1 baseline capture)
- No test source sets (`find . -name "*.kt" -path "*/test/*"` → 0 files)
- No CI directory (`.github/workflows/` and `.forgejo/workflows/` both absent)
- `local.properties` IS tracked in git (`git ls-files local.properties` → "local.properties")
- `ConnectionResult` is a `sealed class` in `core/model` with 5 subtypes (Success, InvalidUrl, AuthFailed, NetworkError, ServerError); 24 references across `feature/` and `core/`
- `AppResult`/`AppError` already defined in `core/common` (`AppError` variants: Network, Auth, NotFound, Server, Unknown)
- No `PlayerSettings`/`UiSettings` interfaces in `:core:domain` — these don't exist yet
- `feature/library`, `feature/player`, `feature/settings` each import `:core:data` directly for preferences

## Requirements

1. **PlayerScreen split (POLISH-01):** `PlayerScreen.kt` is split into at least 3 focused files; no single file exceeds 600 lines; behavior is unchanged.
   - Current: `feature/player/.../PlayerScreen.kt` at 1227 lines mixing ExoPlayer surface, gesture controls, top bar, bottom seek bar, PiP, marker timeline
   - Target: At minimum `PlayerScreen.kt` (entry point + state wiring, < 400 lines), `PlayerControls.kt` (top bar + bottom seek bar composables), `PlayerGestures.kt` (gesture detection logic, `pointerInput` blocks, `safeDrawingPadding` overlay structure); each file < 600 lines
   - Acceptance: `wc -l feature/player/.../PlayerScreen.kt` → < 600; `ls feature/player/.../PlayerControls.kt feature/player/.../PlayerGestures.kt` → both exist; `./gradlew :feature:player:assembleDebug` → BUILD SUCCESSFUL; `./gradlew :feature:player:compileReleaseKotlin` → 0 new errors

2. **Lint baseline shrink (POLISH-02):** `app/lint-baseline.xml` shrunk by ≥ 30% from the current 1001-line file.
   - Current: 1001 lines in `app/lint-baseline.xml`
   - Target: ≤ 700 lines after fixing or explicitly deferring issues. A deferred issue is recorded in the baseline file itself (existing behavior) — the goal is to fix, not just re-baseline.
   - Acceptance: `wc -l app/lint-baseline.xml` → ≤ 700; `./gradlew :app:lintDebug` → 0 new violations above the updated baseline

3. **Detekt + ktlint re-baseline (POLISH-03):** Zero new detekt/ktlint violations introduced by Phases 2/3 changes; per-module detekt baselines regenerated if needed.
   - Current: Detekt baselines exist per module (last captured Phase 1); Phase 2/3 added ~40 lines to `PlayerScreen.kt` and created new files
   - Target: `./gradlew detekt ktlintCheck` exits 0 with no new violations
   - Acceptance: `./gradlew detekt ktlintCheck 2>&1 | grep -c 'issue found'` → 0 (or all issues are in existing baselines)

4. **Test framework wiring (POLISH-04):** JUnit5, Turbine, MockK, and Robolectric added to the `stash.android.library` convention plugin so every library module has a working test source-set out of the box.
   - Current: No test dependencies in any convention plugin; no `src/test/` source sets
   - Target: `build-logic/convention/src/main/kotlin/.../AndroidLibraryConventionPlugin.kt` includes `testImplementation(libs.junit5.api)`, `testRuntimeOnly(libs.junit5.engine)`, `testImplementation(libs.mockk)`, `testImplementation(libs.turbine)`, `testImplementation(libs.robolectric)` (or equivalent catalog refs); `gradle/libs.versions.toml` has versions for all 4 frameworks
   - Acceptance: `./gradlew :core:common:test` → BUILD SUCCESSFUL (even if no tests yet); `grep -c 'junit5\|turbine\|mockk\|robolectric' gradle/libs.versions.toml` → ≥ 4

5. **Seed test suites (POLISH-05):** Baseline tests exist for `core/common`, `core/model`, `core/domain`, one ViewModel per feature, and one Compose smoke test per feature.
   - Current: Zero test files
   - Target: ≥ 1 test file per core module (`core/common`, `core/model`, `core/domain`); ≥ 1 `*ViewModelTest.kt` under `feature/{connection,home,library,browse,detail,player,settings}/src/test/`; ≥ 1 Compose smoke test per feature (may use Robolectric `@Config` to avoid full emulator)
   - Acceptance: `find . -path "*/src/test/*.kt" -not -path "*/.planning/*" | wc -l` → ≥ 15; `./gradlew test` → BUILD SUCCESSFUL with tests passing

6. **PlayerSettings/UiSettings interfaces (POLISH-06):** `:core:domain` exposes `PlayerSettings` and `UiSettings` interfaces; `feature/{player,settings}` consume them via Hilt without importing `:core:data` directly for prefs.
   - Current: `feature/library`, `feature/player`, `feature/settings` each declare `implementation(project(":core:data"))` to access `PlayerPreferences`/`UiPreferences` DataStore stores directly
   - Target: `PlayerSettings` interface in `core/domain/` exposes at minimum: `seekMsPerPx: Flow<Float>`, `doubleTapSeekSec: Flow<Int>`, `defaultSpeed: Flow<Float>`, `repeatMode: Flow<RepeatMode>` (and setters); `UiSettings` interface exposes `bottomNavVisible: Flow<String>`, `gridColumns: Flow<String>`, `amoledBlack: Flow<Boolean>`; `PlayerPreferences` and `UiPreferences` in `:core:data` implement these interfaces; Hilt `@Binds` wires them in `DataModule`; feature modules drop `:core:data` dep if it was only for prefs
   - Acceptance: `grep -rn 'implementation.*core:data' feature/player/build.gradle.kts feature/settings/build.gradle.kts` → 0 (or no `:core:data` dep); `./gradlew :feature:player:assembleDebug :feature:settings:assembleDebug` → BUILD SUCCESSFUL

7. **ConnectionResult retirement (POLISH-07):** `ConnectionResult` is replaced by `AppResult<ServerInfo>`; `catch (e: Throwable)` blocks in all repository files rethrow `CancellationException` before handling other errors.
   - Current: `core/model/Connection.kt` defines `sealed class ConnectionResult` with 5 subtypes; `DefaultConnectionRepository.kt` returns `ConnectionResult`; `ConnectionViewModel.kt` consumes it; 24 references total
   - Target: `ConnectionResult` sealed class deleted from `core/model`; `ConnectionRepository.test(server)` returns `AppResult<ServerInfo>`; mapping: `Success` → `AppResult.Success(info)`, `InvalidUrl` → `AppResult.Failure(AppError.Unknown(reason))`, `AuthFailed` → `AppResult.Failure(AppError.Auth(message))`, `NetworkError` → `AppResult.Failure(AppError.Network(message))`, `ServerError` → `AppResult.Failure(AppError.Server(message))`; all `catch (e: Throwable)` blocks in `core/data` catch and rethrow `CancellationException` first
   - Acceptance: `grep -rn 'ConnectionResult' feature/ core/` → 0 hits; `grep -rn 'catch (e: Throwable)' core/data/` → 0 hits (replaced by narrow catches); `./gradlew :core:data:assembleDebug :feature:connection:assembleDebug` → BUILD SUCCESSFUL

8. **Forgejo Actions CI (POLISH-08):** A `.forgejo/workflows/ci.yml` workflow exists with cache keys composed from Gradle wrapper + JDK + AGP + `libs.versions.toml` hash; runs on push and PR.
   - Current: No CI directory
   - Target: `.forgejo/workflows/ci.yml` with steps: checkout, Java 17 setup, Gradle cache (keyed on wrapper SHA + JDK version + AGP version + `libs.versions.toml` hash), `./gradlew assembleDebug detekt ktlintCheck`; workflow triggers on `push` to `master` and `pull_request`
   - Acceptance: `.forgejo/workflows/ci.yml` exists; `grep -c 'hashFiles.*libs.versions.toml' .forgejo/workflows/ci.yml` → ≥ 1; YAML is valid (`python3 -c "import yaml; yaml.safe_load(open('.forgejo/workflows/ci.yml'))"` → no error)

9. **Docs refresh (POLISH-09):** `DEVICE_TESTING.md` updated to reflect Phase 2/3 additions; `.planning/codebase/` re-mapped against current codebase state.
   - Current: `DEVICE_TESTING.md` predates Phase 2 (no platform compliance checks); `.planning/codebase/` was mapped at Phase 1 initialization
   - Target: `DEVICE_TESTING.md` includes Phase 2 platform compliance checks (COMPLY-01/02/04/06) and Phase 3 shuffle-fix verification step; `.planning/codebase/` has at minimum 1 updated file reflecting Phase 2/3 codebase changes
   - Acceptance: `grep -c 'COMPLY\|predictive back\|edge-to-edge\|ImmutableList\|shuffle' DEVICE_TESTING.md` → ≥ 3; `ls .planning/codebase/*.md` → at least 1 file with `last_updated` ≥ 2026-05-19

10. **VCS hygiene (POLISH-10):** `local.properties` removed from VCS; `.gitignore` audited and covers standard Android artifacts.
    - Current: `git ls-files local.properties` → "local.properties" (tracked); `.gitignore` exists but may have gaps
    - Target: `local.properties` removed from git tracking (`git rm --cached`) but left on disk; `.gitignore` explicitly covers `local.properties`, `*.keystore`, `*.jks`, `release.keystore`, `keystore.properties`, `**/build/`, `.gradle/`
    - Acceptance: `git ls-files local.properties` → empty; `grep -c 'local.properties' .gitignore` → ≥ 1; `git status` shows `local.properties` as untracked (not modified)

## Boundaries

**In scope:**
- `PlayerScreen.kt` split into `PlayerControls.kt` + `PlayerGestures.kt` (+ entry point `PlayerScreen.kt`)
- Lint baseline shrink ≥ 30% of current 1001-line file (≤ 700 lines target)
- Detekt + ktlint zero-new-violations gate
- JUnit5 + Turbine + MockK + Robolectric wired in `stash.android.library` convention plugin
- Seed test suites for core modules + one ViewModel + one Compose smoke per feature
- `PlayerSettings` + `UiSettings` interfaces in `:core:domain` with DataStore-backed impls in `:core:data`
- `ConnectionResult` → `AppResult<ServerInfo>` migration + `catch (e: Throwable)` narrowing
- `.forgejo/workflows/ci.yml` with correct cache keys
- `DEVICE_TESTING.md` Phase 2/3 additions
- `local.properties` removed from VCS

**Out of scope:**
- Module-graph restructure (`:app/`, `:core/`, `:feature/`, `:build-logic/` topology frozen) — per milestone boundary
- AGP 9 / compileSdk 36 upgrade — DEPS-17, deferred to AGP-9 milestone
- Apollo 5 / Nav3 migrations — dedicated milestones (NET-01, NAV-01)
- Real `MediaSessionService` — BG-MEDIA milestone
- EncryptedSharedPreferences replacement — SEC-01 (replacement alpha, not stable)
- New user-visible features — modernization-only milestone
- Full exhaustive test coverage for all existing code — seed tests only; coverage target is "wiring works, not 80%+" (that's a future milestone)
- POLISH-CACHE-01 (`StashImageLoader.kt` runBlocking fix) — deferred; requires async Coil factory pattern change

## Constraints

- **Toolchain floor:** AGP 8.7.3 / Kotlin 2.2.20 / compileSdk 35 — unchanged
- **Build must stay green** at every atomic commit; no intermediate broken states
- **PlayerScreen split must preserve Phase 3 optimizations:** `LaunchedEffect(state.videoFrameRate)` (PERF-09), `ImmutableList<Marker>` parameters (PERF-03), `PredictiveBackHandler` (COMPLY-02), `safeDrawingPadding` overlay structure (COMPLY-01), `PERF-04` stability comment — all must survive the refactor
- **POLISH-06 within-module only:** `PlayerSettings`/`UiSettings` interfaces are extracted from `:core:data` into `:core:domain`. No new external dependencies. No behavior change.
- **POLISH-07 `InvalidUrl` mapping:** Map to `AppError.Unknown(reason)` — no new `AppError` subtype added (smallest-diff migration; a future cleanup can add `AppError.Validation` if needed)
- **CI uses `.forgejo/workflows/`, not `.github/workflows/`** — the remote is Forgejo at `alpine-forgejo.twin-wezen.ts.net`
- **Lint shrink strategy:** Fix actual violations where practical; defer remainder with existing baseline mechanism; do NOT regenerate the entire baseline to hide new issues
- **Test seed scope:** "Working out of the box" means test compilation green and basic test infrastructure wired; coverage metrics are not gated in this phase (deferred to future milestone)

## Open Questions (resolved for planning)

| # | Question | Decision |
|---|----------|----------|
| 1 | `ConnectionResult.InvalidUrl` → which `AppError`? | `AppError.Unknown(reason)` — no new variant |
| 2 | CI platform: GitHub Actions or Forgejo Actions? | `.forgejo/workflows/ci.yml` — Forgejo is the active platform |
| 3 | Which features need `:core:data` dep only for prefs? | `feature/player` (PlayerPreferences), `feature/settings` (UiPreferences), `feature/library` (UiPreferences) — all 3 to be migrated via POLISH-06 |
| 4 | Split file names (POLISH-01)? | `PlayerScreen.kt` (entry point), `PlayerControls.kt` (controls overlays), `PlayerGestures.kt` (gesture detection + pointerInput) |
| 5 | PlayerScreen.kt line count target post-split? | No single file > 600 lines (generous to handle imports and Phase 2/3 additions) |
| 6 | Lint shrink: fix or defer? | Fix where practical; remaining deferred in baseline — target is absolute ≤ 700 lines, not "fix 30% of issues" |

## Ambiguity Report

```
Goal Clarity:       0.87 (min 0.75) ✓
Boundary Clarity:   0.84 (min 0.70) ✓
Constraint Clarity: 0.78 (min 0.65) ✓
Acceptance Criteria:0.81 (min 0.70) ✓
Ambiguity score:    0.168 (gate: ≤ 0.20) ✓ PASS
```

All 4 dimensions above minimum. Gate passed. Planner may proceed without assumptions.
