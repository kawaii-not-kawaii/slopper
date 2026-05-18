# Roadmap: Slopper Android Modernization

**Created:** 2026-05-16
**Granularity:** Coarse (4 phases)
**Mode:** YOLO + parallel plans
**Status:** Phase 1 pending

## Milestone Overview

**Core Value:** The app continues to install and run on every device it currently supports, but is measurably faster, leaner, and aligned with current Android platform guidance — with a dependency tree the team can keep alive.

**Phase count:** 4 (DEPS / COMPLY / PERF / POLISH)

**Ordering rationale:** AGP 9 / Kotlin 2.2 / Compose BOM 2026.05 / `compileSdk = 36` is the natural fault line — everything downstream (predictive back, edge-to-edge, Splash API, baseline-profile regen, strong-skipping audit) is either *forced* by `targetSdk = 36` or only becomes *safe* once the toolchain is in place. Therefore: bump the floor first (DEPS), absorb the platform mandates that floor unlocks (COMPLY), then measure and improve on the stable floor (PERF), then clean up what the prior phases churned (POLISH). Test-pyramid bootstrap and the `PlayerScreen.kt` split are deferred to POLISH so the refactor lands against a known-green build with re-baselined lint/detekt.

**Coverage:** 41 / 41 requirements mapped (DEPS-01..14, COMPLY-01..07, PERF-01..10, POLISH-01..10).

**Current Status:**

| Phase | Plans | Status | Completed |
|-------|-------|--------|-----------|
| 1. DEPS (Foundation Bump) | 0/3 | Not started | — |
| 2. COMPLY (Platform Compliance) | 2/2 | Planned | — |
| 3. PERF (Measured Wins) | 2/3 | In Progress|  |
| 4. POLISH (Test Pyramid & Cleanup) | 2/3 | In Progress|  |

## Phases

- [ ] **Phase 1: DEPS** — Land toolchain + dependency floor (lint baseline → JDK → Gradle → AGP → Kotlin → Compose BOM → AndroidX/Hilt/Coil sweep) with the build green
- [ ] **Phase 2: COMPLY** — Resolve every framework-enforced contract that `targetSdk = 36` makes mandatory (edge-to-edge, predictive back, POST_NOTIFICATIONS, Splash API, per-app locales, orphan FGS permission)
- [ ] **Phase 3: PERF** — Wire GMD, expand baseline profile, hit numeric perf floors, fix the shuffle-playback bug
- [ ] **Phase 4: POLISH** — Split `PlayerScreen.kt`, shrink lint baseline, wire JUnit5/Turbine/MockK/Robolectric, seed tests, retire `ConnectionResult`, refresh docs

## Phase Details

### Phase 1: DEPS (Foundation Bump)

**Goal:** Land the toolchain and dependency floor so every subsequent phase runs against current-stable AGP/Kotlin/Compose with a green build.

**Depends on:** Nothing (first phase).

**Requirements:** DEPS-01, DEPS-02, DEPS-03, DEPS-04, DEPS-05, DEPS-06, DEPS-07, DEPS-08, DEPS-09, DEPS-10, DEPS-11, DEPS-12, DEPS-13, DEPS-14

**Success Criteria** (what must be TRUE):
  1. `app/lint-baseline.xml` exists on disk and is committed *before* any version bump lands (PITFALLS §18).
  2. `./gradlew --configuration-cache assembleDebug assembleRelease assembleBenchmark check` is green on JDK 17, Gradle 9.4.1, AGP 9.2.0, Kotlin 2.2.20, Compose BOM 2026.05.00.
  3. `./gradlew :app:dependencies` review shows no surprise version skews — every AndroidX artifact resolves to the version intended in `gradle/libs.versions.toml` (PITFALLS §10).
  4. `./gradlew -q javaToolchains` reports a single resolved JDK 17 (PITFALLS §1).
  5. Baseline profile under `app/src/release/generated/baselineProfiles/baseline-prof.txt` is regenerated in the same PR series as the dep bumps and is newer than `gradle/libs.versions.toml` (PITFALLS §6).

**Suggested Plans** (3 plans, sequenced + parallel-safe where independent):

- **Plan 1.1 — Pre-bump safety net** *(parallel-safe: YES, run first, blocks 1.2 + 1.3)*
  - **Goal:** Freeze the current warning surface and pin the JDK so subsequent bumps surface only *new* issues.
  - **Scope:** DEPS-01 (lint baseline), DEPS-02 (JDK toolchain pin).
  - **Files likely touched:** `app/lint-baseline.xml` (new), `build-logic/convention/src/main/kotlin/.../KotlinAndroid.kt`, `build-logic/convention/src/main/kotlin/.../AndroidApplicationConventionPlugin.kt`, `build-logic/convention/src/main/kotlin/.../AndroidLibraryConventionPlugin.kt`, `gradle.properties` (`org.gradle.java.installations.auto-download=false`).
  - **Dependency:** None. Must complete before Plan 1.2 or 1.3.

- **Plan 1.2 — Toolchain + AGP/Kotlin/Compose bumps** *(parallel-safe: NO — strictly sequential within itself)*
  - **Goal:** Walk the toolchain from current pins to target stable, one bump per step, build green at each step.
  - **Scope:** DEPS-03 (Gradle 9.4.1 + config cache), DEPS-04 (AGP 9.2.0, `compileSdk=36`/`targetSdk=36`, R8 keep-rule audit per PITFALLS §9), DEPS-05 (Kotlin 2.2.20 + KSP coordinate + Compose Compiler plugin + `-Xcontext-receivers` → `-Xcontext-parameters` migration per PITFALLS §12), DEPS-06 (Compose BOM 2026.05.00 + drop explicit version overrides per PITFALLS §14), DEPS-12 (Detekt 1.23.8 + ktlint 13.1.0 re-baselined).
  - **Files likely touched:** `gradle/wrapper/gradle-wrapper.properties`, `gradle/libs.versions.toml`, `build-logic/convention/src/main/kotlin/.../AndroidComposeConventionPlugin.kt`, `build-logic/convention/src/main/kotlin/.../KotlinAndroid.kt`, `app/proguard-rules.pro`, `config/detekt/detekt.yml`, `config/detekt-baseline.xml`.
  - **Dependency:** Plan 1.1.

- **Plan 1.3 — Library sweep + catalog hygiene** *(parallel-safe: YES, after Plan 1.2 lands the floor)*
  - **Goal:** Bring every non-toolchain dependency to its target version with a clean catalog and a fresh baseline profile.
  - **Scope:** DEPS-07 (AndroidX sweep — Lifecycle 2.10.0, Activity Compose 1.13.0, Navigation Compose 2.9.6, DataStore 1.2.1, Coil 3.4.0), DEPS-08 (Hilt 2.57.1, coroutines 1.11.0, kotlinx.serialization 1.9.0 + KSP regression check per PITFALLS §§3, 11), DEPS-09 (Apollo pinned 4.4.3, OkHttp 4.12.0), DEPS-10 (conditional Media3 1.10.0 only if `nextlib-media3ext:1.10.x` exists per PITFALLS §16), DEPS-11 (catalog hygiene: drop unused Room, add `kotlinx.collections.immutable`, BOM-controlled refs per PITFALLS §14), DEPS-13 (final assemble + check end-to-end), DEPS-14 (baseline profile regen).
  - **Files likely touched:** `gradle/libs.versions.toml`, `app/src/release/generated/baselineProfiles/baseline-prof.txt`, `baselineprofile/build.gradle.kts`, `app/proguard-rules.pro`.
  - **Dependency:** Plan 1.2.

**Created plans** (executable PLAN.md files in `.planning/phases/01-deps-foundation-bump/`):
- [ ] `01.1-PLAN.md` — Preflight + Toolchain Floor (DEPS-01..06, lockstep, stop-on-failure)
- [ ] `01.2-PLAN.md` — Library Sweep (DEPS-07..10, revert-and-continue)
- [ ] `01.3-PLAN.md` — Catalog hygiene, quality gates, verification, baseline profile (DEPS-11..16, revert-and-continue; DEPS-15/16 added beyond original REQUIREMENTS.md DEPS-01..14)

**Verification Gate:**

```
./gradlew --no-daemon -q javaToolchains              # one JDK 17 resolved
./gradlew --configuration-cache assembleDebug         # config cache clean (PITFALLS §2)
./gradlew assembleRelease assembleBenchmark check     # release + benchmark + tests + lint green
./gradlew :app:dependencies --configuration releaseRuntimeClasspath > deps.txt  # diff for skews
./gradlew :app:assembleRelease -Pandroid.enableR8.fullMode=true 2>&1 | tee r8-warnings.txt  # zero serialization/Apollo warnings (PITFALLS §9)
ls app/lint-baseline.xml                              # baseline exists
find app/src/release/generated/baselineProfiles/baseline-prof.txt -newer gradle/libs.versions.toml  # profile fresher than catalog
```

**Risks** (cite PITFALLS.md):
- §1 JDK toolchain mismatch (High) — mitigated by Plan 1.1.
- §3 KSP/Kotlin version drift (High) — `mismatch=fail` in convention plugin.
- §4 Compose Compiler plugin coordinate split (High) — verify single `compose-compiler` in deps report.
- §9 R8 keep-rule invalidation under serialization 1.9 (High) — release smoke on `benchmark` build type.
- §10 Dependency convergence (Medium) — `:app:dependencies` diff per bump.
- §11 Hilt aggregating processor NPE (Medium) — stay on Hilt 2.57.1 latest stable; have `ksp.incremental=false` fallback ready.
- §16 `nextlib-media3ext` lockstep (High if attempted) — DEPS-10 is conditional, scope-checked.
- §17 Compose API removals (Medium) — `compileReleaseKotlin` zero-deprecations gate.

---

### Phase 2: COMPLY (Platform Compliance)

**Goal:** Resolve every framework-enforced contract that `targetSdk = 36` makes mandatory, without changing user-visible behavior.

**Depends on:** Phase 1 (needs `compileSdk = 36`, Activity Compose 1.13.0, Compose BOM with predictive-back APIs).

**Requirements:** COMPLY-01, COMPLY-02, COMPLY-03, COMPLY-04, COMPLY-05, COMPLY-06, COMPLY-07

**Success Criteria** (what must be TRUE):
  1. Every screen renders correctly edge-to-edge on **both** a Pixel gesture-nav device and an OEM 3-button-nav device — no clipped buttons, no phantom shade-pulls, no unreadable status-bar text on player overlays.
  2. Back swipe on Android 14+ shows the correct predictive preview from `PlayerScreen`, `FilterSheet`, `MoreSheet`, and `NavCustomizeSheet`; `BackHandler` callbacks fire exactly once.
  3. `POST_NOTIFICATIONS` is requested at the moment a feature first needs it (not at app start); denial is handled gracefully.
  4. Cold launch shows the `installSplashScreen()`-driven splash until `RootViewModel.start` resolves; no white flash.
  5. The orphan `FOREGROUND_SERVICE_MEDIA_PLAYBACK` permission is gone from `AndroidManifest.xml`; per-app language picker exposes the system dialog.

**Suggested Plans** (2 plans, parallel-safe — they touch disjoint surfaces):

- **Plan 2.1 — Insets + Back + Splash** *(parallel-safe: YES)*
  - **Goal:** Land the three platform contracts that touch UI plumbing.
  - **Scope:** COMPLY-01 (edge-to-edge audit per PITFALLS §7), COMPLY-02 (predictive back per PITFALLS §8), COMPLY-04 (Splash Screen API migration).
  - **Files likely touched:** `app/src/main/AndroidManifest.xml`, `app/src/main/res/values/themes.xml`, `app/src/main/java/.../MainActivity.kt`, `feature/player/src/main/kotlin/.../PlayerScreen.kt`, `feature/library/src/main/kotlin/.../FilterSheet.kt`, `feature/.../MoreSheet.kt`, `feature/.../NavCustomizeSheet.kt`, `core/ui` Scaffold wrappers.
  - **Dependency:** Phase 1.

- **Plan 2.2 — Permissions + Locales + Manifest hygiene** *(parallel-safe: YES, independent of 2.1)*
  - **Goal:** Land the manifest-and-permission contracts that don't touch UI layout.
  - **Scope:** COMPLY-03 (`POST_NOTIFICATIONS` runtime request), COMPLY-05 (remove orphan `FOREGROUND_SERVICE_MEDIA_PLAYBACK`), COMPLY-06 (per-app language `generateLocaleConfig = true` + picker), COMPLY-07 (manual `DEVICE_TESTING.md` re-run on Pixel + OEM 3-button device).
  - **Files likely touched:** `app/src/main/AndroidManifest.xml`, `app/build.gradle.kts` (`generateLocaleConfig`), `app/src/main/res/xml/locale_config.xml` (generated), `feature/settings/.../LanguageSettings.kt`, permission-request call sites in whichever feature first needs notifications, `DEVICE_TESTING.md`.
  - **Dependency:** Phase 1. Final manual `DEVICE_TESTING.md` run gates Phase 2 exit and depends on Plan 2.1 also being merged.

**Verification Gate:**

```
./gradlew assembleDebug assembleRelease check
adb install -r app-debug.apk
# Manual: DEVICE_TESTING.md full run on Pixel gesture-nav + OEM 3-button-nav
# Manual: enable Developer Options → "Predictive back animations"; verify previews on all sheets
grep -c FOREGROUND_SERVICE_MEDIA_PLAYBACK app/src/main/AndroidManifest.xml   # → 0
grep -c 'android:enableOnBackInvokedCallback="true"' app/src/main/AndroidManifest.xml  # → 1
```

**Risks** (cite PITFALLS.md):
- §7 Edge-to-edge breaks `PlayerScreen` gestures + `FilterSheet` insets on 3-button-nav OEMs (High) — mitigated by mandatory dual-device test.
- §8 Predictive back regression on conditional `BackHandler(enabled=…)` (Medium) — gate `enabled` flag stability mid-gesture; never mutate during the gesture.
- Permission UX: requesting `POST_NOTIFICATIONS` at app start is a known anti-pattern — gate behind first-use.

**Created plans** (executable PLAN.md files in `.planning/phases/02-comply-platform-compliance/`):
- [ ] `02.1-PLAN.md` — Insets + Back + Splash (COMPLY-01, COMPLY-02, COMPLY-04)
- [ ] `02.2-PLAN.md` — Permissions + Locales + Manifest hygiene + UAT (COMPLY-03, COMPLY-05, COMPLY-06, COMPLY-07)

---

### Phase 3: PERF (Measured Wins)

**Goal:** Wire reproducible perf measurement, expand the baseline profile, hit the numeric floors, and fix the shuffle-playback bug.

**Depends on:** Phase 1 (toolchain + profile freshness) and Phase 2 (no compliance regressions during measurement).

**Requirements:** PERF-01, PERF-02, PERF-03, PERF-04, PERF-05, PERF-06, PERF-07, PERF-08, PERF-09, PERF-10

**Success Criteria** (what must be TRUE):
  1. A Gradle-managed `Pixel 6 API 34` device runs `./gradlew :baselineprofile:pixel6Api34BenchmarkBenchmark` reproducibly — three back-to-back runs land within tight variance.
  2. Cold-start macrobench shows **p50 ≥ 5% faster with baseline profile vs without** on the GMD; result is checked into the repo (PERF-06).
  3. Library scroll macrobench shows **≥ 95% frames on time at p95** on the GMD (PERF-07).
  4. Shuffle / random consecutive video playback no longer hangs or slows after several videos; a 10-video shuffle session shows no steady-state memory growth and no listener / `MediaSource` accumulation; root cause is documented in the PR (PERF-08).
  5. Every perf claim landed in this milestone has a checked-in macrobench output or baseline-profile delta backing it — no "feels faster" claims (PERF-10).

**Suggested Plans** (3 plans, parallelizable after the GMD lands):

- **Plan 3.1 — Measurement substrate** *(parallel-safe: must run first, blocks 3.2 + 3.3)*
  - **Goal:** Make perf claims reproducible before anyone makes them.
  - **Scope:** PERF-01 (GMD `Pixel 6 API 34` wired per PITFALLS §13), PERF-02 (Compose Compiler stability reports into `stash.android.library.compose`), PERF-05 (baseline-profile expansion to Home rails / Library scroll+filter / Detail open / Player start).
  - **Files likely touched:** `baselineprofile/build.gradle.kts`, `baselineprofile/src/main/kotlin/.../StashBaselineProfileGenerator.kt`, `build-logic/convention/src/main/kotlin/.../AndroidLibraryComposeConventionPlugin.kt`, `build-logic/convention/src/main/kotlin/.../AndroidApplicationConventionPlugin.kt`.
  - **Dependency:** Phase 1 + Phase 2.

- **Plan 3.2 — Strong-skipping + Compose hygiene audit** *(parallel-safe: YES, after 3.1)*
  - **Goal:** Eliminate recomposition pitfalls that would mask or invert the perf measurements.
  - **Scope:** PERF-03 (`UiState` → `ImmutableList<T>` migration via `kotlinx.collections.immutable`), PERF-04 (`feature/player` `LaunchedEffect`/`DisposableEffect` key audit per PITFALLS §5), PERF-09 (move `applyVideoFrameRate` out of `AndroidView.update` into `LaunchedEffect(targetFps)` per CONCERNS.md).
  - **Files likely touched:** All `*UiState.kt` under `feature/*`, `feature/player/src/main/kotlin/.../PlayerScreen.kt` (lambda/key sites only — full split is POLISH), `core/model` and `core/domain` types where collection fields appear.
  - **Dependency:** Plan 3.1.

- **Plan 3.3 — Shuffle bug fix + macrobench numeric floors** *(parallel-safe: YES, after 3.1; can run alongside 3.2)*
  - **Goal:** Diagnose and fix the shuffle/consecutive-playback hang; demonstrate the numeric perf floors.
  - **Scope:** PERF-06 (cold-start p50 ≥ 5% with profile vs without), PERF-07 (Library scroll ≥ 95% frames on time at p95), PERF-08 (shuffle bug — investigate ExoPlayer leak / listener accumulation / `MediaSource` retention / `applyVideoFrameRate` per-frame as root-cause candidates; document the actual cause; verify with steady-state memory profile across a 10-video shuffle), PERF-10 (every PR carries macrobench output).
  - **Files likely touched:** `feature/player/src/main/kotlin/.../PlayerScreen.kt`, `feature/player/src/main/kotlin/.../PlayerViewModel.kt`, `feature/player/src/main/kotlin/.../ExoPlayer*.kt`, queue/shuffle handling code, macrobench journey sources under `baselineprofile/`.
  - **Dependency:** Plan 3.1. Shares files with Plan 3.2 — coordinate via PR ordering or share a feature branch.

**Verification Gate:**

```
./gradlew :baselineprofile:pixel6Api34BenchmarkBenchmark   # GMD path exists & green
# Repeat 3 times back-to-back, p50 stable within variance budget
./gradlew :baselineprofile:generateBaselineProfile         # profile regenerates against GMD
# Compose stability reports present:
ls build-logic/.../build/compose-reports/*.txt || ls **/build/compose-reports
# Shuffle session: launch app, shuffle 10 videos, observe steady-state heap via Android Profiler — no monotonic growth
# Every PR in this phase has a macrobench output committed under .planning/benchmarks/ or similar
```

**Risks** (cite PITFALLS.md):
- §5 Strong-skipping regression — `LaunchedEffect`/`DisposableEffect` keyed on unstable captures stop re-firing (High, not catchable by lint).
- §6 Stale baseline profile after dep bumps slows startup *more* than no profile (High) — gated by Phase 1 regen and CI freshness check.
- §13 Macrobench variance masks real signal (Medium) — GMD pin, `CompilationMode.Partial(BaselineProfileMode.Require)`, ≥5 iterations, p50+p95 reported.
- PERF-08 root-cause is hypothesis-driven: be prepared to swap candidates if the first instrumentation pass doesn't reproduce.

---

### Phase 4: POLISH (Test Pyramid & Cleanup)

**Goal:** Refactor the highest-risk file, shrink the lint/detekt baselines, wire test infrastructure, retire `ConnectionResult`, and close documentation drift.

**Depends on:** Phase 1 (test infra wires against bumped Kotlin/AGP/Hilt), Phase 2 (no behavior drift mid-refactor), Phase 3 (split `PlayerScreen.kt` only after strong-skipping + frame-rate fixes land so the refactor preserves measured wins).

**Requirements:** POLISH-01, POLISH-02, POLISH-03, POLISH-04, POLISH-05, POLISH-06, POLISH-07, POLISH-08, POLISH-09, POLISH-10

**Success Criteria** (what must be TRUE):
  1. `feature/player/PlayerScreen.kt` no longer exists as a 1122-line file; behavior is unchanged, split into `PlayerControls.kt` / `PlayerGestures.kt` / `PlayerSurface.kt` (POLISH-01).
  2. Lint baseline is at least 30% smaller than the file committed in Phase 1 (POLISH-02); detekt + ktlint re-baselined; CI shows zero new violations (POLISH-03).
  3. Every `:core:*` and `feature/*` module has a working test source-set (JUnit5 + Turbine + MockK + Robolectric); seed tests exist for `core/common`, `core/model`, `core/domain`, one ViewModel per feature, one Compose smoke per feature (POLISH-04, POLISH-05).
  4. `feature/*` modules no longer import `:core:data` directly for prefs — `PlayerSettings` / `UiSettings` interfaces live in `:core:domain` (POLISH-06); `ConnectionResult` is retired in favor of `AppResult`; repo `catch (e: Throwable)` blocks narrow and rethrow `CancellationException` (POLISH-07).
  5. GitHub Actions workflow exists with cache keys composed from wrapper + JDK + AGP + `libs.versions.toml`; `README.md` / `DEVICE_TESTING.md` / `build-logic/` docs refreshed; `local.properties` no longer in VCS; `.gitignore` audited (POLISH-08, POLISH-09, POLISH-10).

**Suggested Plans** (3 plans, parallelizable across disjoint surfaces):

- **Plan 4.1 — PlayerScreen split + internal refactors** *(parallel-safe: YES — file-local)*
  - **Goal:** Make the highest-risk file maintainable without changing behavior.
  - **Scope:** POLISH-01 (split `PlayerScreen.kt`), POLISH-06 (`PlayerSettings`/`UiSettings` interfaces in `:core:domain`), POLISH-07 (retire `ConnectionResult` → `AppResult`; narrow `catch (e: Throwable)` and rethrow `CancellationException`).
  - **Files likely touched:** `feature/player/src/main/kotlin/.../PlayerScreen.kt` → split into `PlayerControls.kt`, `PlayerGestures.kt`, `PlayerSurface.kt`; `core/domain/src/main/kotlin/.../PlayerSettings.kt`, `core/domain/src/main/kotlin/.../UiSettings.kt` (new interfaces); `core/data/src/main/kotlin/.../ConnectionStore.kt`, every repo with `catch (e: Throwable)`, `core/common/src/main/kotlin/.../AppResult.kt`, `feature/connection` `ConnectionResult` call sites.
  - **Dependency:** Phase 3.

- **Plan 4.2 — Test infrastructure + seed tests** *(parallel-safe: YES — additive)*
  - **Goal:** Make tests writeable everywhere; seed enough to prove the wiring.
  - **Scope:** POLISH-04 (JUnit5 + Turbine + MockK + Robolectric wired into `stash.android.library` convention plugin), POLISH-05 (seed suites for `core/common`, `core/model`, `core/domain` + one ViewModel state-machine test per feature + one Compose smoke per feature).
  - **Files likely touched:** `build-logic/convention/src/main/kotlin/.../AndroidLibraryConventionPlugin.kt`, `gradle/libs.versions.toml` (test deps), `core/common/src/test/`, `core/model/src/test/`, `core/domain/src/test/`, one `*ViewModelTest.kt` per `feature/*`, one Compose `*ScreenSmokeTest.kt` per `feature/*`.
  - **Dependency:** Phase 1 (Kotlin/AGP/Hilt versions need to be on target before wiring test infra).

- **Plan 4.3 — Baselines, CI, docs, repo hygiene** *(parallel-safe: YES — non-overlapping surface)*
  - **Goal:** Shrink the warning surface, harden CI caching, refresh docs, audit VCS hygiene.
  - **Scope:** POLISH-02 (lint baseline shrink ≥ 30%), POLISH-03 (detekt + ktlint re-baseline), POLISH-08 (CI cache keys: wrapper + JDK + AGP + `libs.versions.toml` hash), POLISH-09 (docs refresh — `README.md`, `DEVICE_TESTING.md`, `build-logic/` conventions, `.planning/codebase/*` re-mapped), POLISH-10 (`local.properties` removed from VCS, `.gitignore` audited, `keystore.properties` workflow documented via the existing `keystore.properties.example`).
  - **Files likely touched:** `app/lint-baseline.xml`, `config/detekt-baseline.xml`, `.github/workflows/*.yml` (new), `README.md`, `DEVICE_TESTING.md`, `build-logic/convention/README.md` (new or refreshed), `.gitignore`, removal of `local.properties` from tree, `.planning/codebase/*.md`.
  - **Dependency:** Phase 1 + Phase 3 (detekt/ktlint re-baseline runs against bumped tooling; lint shrink targets the baseline created in Phase 1).

**Verification Gate:**

```
./gradlew check                                       # all tests green, detekt+ktlint zero new violations
./gradlew :app:lint                                   # baseline shrunk ≥ 30% vs Phase 1 commit
wc -l app/lint-baseline.xml                           # numeric check
git ls-files | grep -E '(^|/)local\.properties$'      # must return empty
# Per-feature: one ViewModel test + one Compose smoke present
find feature -path '*/src/test/*ViewModelTest.kt' | wc -l
find feature -path '*/src/*Test/*ScreenSmokeTest.kt' | wc -l
# CI: trigger a fresh-cache run, then a cache-hit run; both green
```

**Risks** (cite PITFALLS.md):
- §5 Splitting `PlayerScreen` can reintroduce strong-skipping regressions if effect keys move across file boundaries (High) — re-run player section of `DEVICE_TESTING.md` post-split.
- §12 Detekt / ktlint rule drift (Medium) — handled by re-baseline against the Phase 1 versions.
- §15 CI cache invalidation cascade (Medium) — compose cache keys per the pitfall.
- §18 Lint baseline shrink can become a long tail — bound it at the 30% target; defer the rest with explicit rationale.

### Phase 5: SPINE (Compose UI Redesign)

**Goal:** [To be planned]
**Requirements**: TBD
**Depends on:** Phase 4
**Plans:** 2/3 plans executed

Plans:
- [ ] TBD (run /gsd-plan-phase 5 to break down)

---

## Cross-Phase Dependencies

| Upstream → Downstream | Why it unblocks |
|---|---|
| Phase 1 → Phase 2 | `compileSdk = 36`, Activity Compose 1.13.0, Splash + predictive-back APIs only exist on the bumped floor. |
| Phase 1 → Phase 3 | Compose Compiler reports, GMD plugin, baseline-profile DSL, stable Kotlin/Compose runtime needed before measurement. |
| Phase 1 → Phase 4 | Test infra (JUnit5/Turbine/MockK/Robolectric), detekt/ktlint re-baseline, lint baseline shrink all target the bumped tooling. |
| Phase 2 → Phase 3 | Edge-to-edge / predictive-back / Splash regressions would invalidate macrobench journeys; compliance must be settled before "measure" is meaningful. |
| Phase 2 → Phase 4 | `DEVICE_TESTING.md` checklist outcomes feed POLISH-09 doc refresh. |
| Phase 3 → Phase 4 | Split `PlayerScreen.kt` only after PERF-04/-08/-09 land — the refactor must preserve the measured fixes; lint baseline shrink targets the Phase 1 file, but the player split happens *after* perf to avoid masking root-cause attribution. |
| Plan 1.1 → Plan 1.2 → Plan 1.3 | Sequential within Phase 1: lint baseline + JDK pin must precede AGP/Kotlin bumps; library sweep follows toolchain. |
| Plan 3.1 → Plans 3.2 + 3.3 | GMD + stability reports + expanded profile must exist before audit + bug-fix work asserts numbers. |

## Risk Register (Top 5, rolled up from PITFALLS.md)

| # | Risk | Severity | Phase | Mitigation |
|---|---|---|---|---|
| 1 | Stale baseline profile after dep bump (PITFALLS §6) — slower than no profile | High | DEPS gate + PERF | Same-PR profile regen; CI freshness check `find … -newer libs.versions.toml`; macrobench validates ≥ 5% delta. |
| 2 | Strong-skipping regression in `PlayerScreen` (PITFALLS §5) — auto-memoized lambdas stop re-triggering `LaunchedEffect`; not catchable by lint | High | DEPS verify + PERF audit + POLISH refactor | Plan 3.2 audits every `LaunchedEffect`/`DisposableEffect` key list; manual `DEVICE_TESTING.md` player checklist post-BOM-bump and post-split. |
| 3 | R8 keep-rule invalidation under AGP 9 + serialization 1.9 + Apollo 4.4.x (PITFALLS §9) — release-only crashes | High | DEPS bump + PERF release smoke | `assembleRelease` with full R8 mode; read every warning; smoke-test `benchmark` build type across primary screens. |
| 4 | Edge-to-edge breaks player gestures + filter-sheet insets on 3-button-nav OEMs (PITFALLS §7) | High (player) / Med (sheet) | COMPLY | Dual-device manual test in Plan 2.1's gate. |

Honorable mentions: §1 JDK toolchain mismatch (DEPS), §13 macrobench variance (PERF), §16 `nextlib-media3ext` lockstep (DEPS — explicitly scope-checked via DEPS-10), §18 missing `lint-baseline.xml` (DEPS first task).

## Out of Scope

Copied from REQUIREMENTS.md to prevent scope creep:

| Item | Reason |
|------|--------|
| New end-user features | Modernization-only milestone — no behavior change beyond perf + compliance |
| `minSdk` bump | Must stay installable on existing devices |
| New third-party SDKs | No vendor additions without explicit approval |
| Module-graph restructure (`app/` / `core/` / `feature/` / `build-logic/`) | Frozen — refactors stay within modules |
| Migrating off Compose / Hilt / Gradle Kotlin DSL | Existing architecture preserved |
| Real `MediaSessionService` (background playback) | New capability — deferred to BG-MEDIA milestone; Phase 2 only removes the orphan `FOREGROUND_SERVICE_MEDIA_PLAYBACK` permission |
| EncryptedSharedPreferences replacement | Replacement is alpha; pin `androidx.security:security-crypto:1.1.0`, leave TODO in `ConnectionStore.kt` |
| Apollo 5 / Nav3 migrations | Each deserves its own milestone — Apollo stays on 4.4.3 |
| "Feels faster" perf claims | Every perf change must show baseline-profile or macrobench delta |
| Sentry / Crashlytics / Firebase | No new vendor dependencies |

## Glossary

| Prefix | Phase | Meaning |
|---|---|---|
| **DEPS** | Phase 1 | Dependencies & build infrastructure (toolchain, AGP/Kotlin/Compose/Hilt/AndroidX/Apollo/Media3, version catalog, lint baseline, JDK pin) |
| **COMPLY** | Phase 2 | Platform-mandated compliance (edge-to-edge, predictive back, POST_NOTIFICATIONS, Splash API, orphan FGS permission, per-app locales, device-test rerun) |
| **PERF** | Phase 3 | Measured performance work (GMD wiring, Compose stability reports, ImmutableList migration, strong-skipping audit, baseline profile expansion, cold-start + scroll macrobench floors, shuffle-playback bug fix, `applyVideoFrameRate` relocation, measured-only claims) |
| **POLISH** | Phase 4 | Test pyramid + cleanup (`PlayerScreen` split, lint/detekt baseline shrink, JUnit5/Turbine/MockK/Robolectric wiring, seed tests, `PlayerSettings`/`UiSettings` interfaces, retire `ConnectionResult`, CI cache keys, docs refresh, VCS hygiene) |
| **BG-MEDIA / SEC / NET / NAV / ARCH** | Deferred | Tracked in REQUIREMENTS.md "Deferred to Future Milestones" — explicitly *not* part of this milestone |
