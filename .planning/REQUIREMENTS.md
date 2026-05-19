# Requirements: Slopper Modernization

**Defined:** 2026-05-16
**Core Value:** The app continues to install and run on every device it currently supports, but is measurably faster, leaner, and aligned with current Android platform guidance — with a dependency tree the team can keep alive.

## Milestone Requirements

Requirements for the modernization milestone. Each maps to one of the 4 roadmap phases (DEPS / COMPLY / PERF / POLISH).

### Dependencies & Build Infrastructure (DEPS)

- [ ] **DEPS-01**: `app/lint-baseline.xml` generated and committed before any other bump — establishes a baseline so subsequent bumps surface only *new* issues
- [ ] **DEPS-02**: JDK toolchain pinned to 17 via `kotlin { jvmToolchain(17) }` and `org.gradle.java.installations.auto-download=false`
- [ ] **DEPS-03**: Gradle wrapper bumped to 9.4.1 with configuration cache enabled; `gradlew --configuration-cache assembleDebug` succeeds
- [ ] **DEPS-04**: AGP bumped to 9.2.0; `compileSdk = 36`, `targetSdk = 36`, `minSdk` unchanged (26); R8 keep rules audited per AGP 9 semantics
- [ ] **DEPS-05**: Kotlin bumped to 2.2.20 with matching KSP and Compose Compiler plugin versions; `-Xcontext-receivers` migrated to `-Xcontext-parameters`; `-Xskip-metadata-version-check` removed if no longer needed
- [ ] **DEPS-06**: Compose BOM bumped to 2026.05.00; explicit version overrides for Compose libs removed
- [ ] **DEPS-07**: AndroidX sweep — Lifecycle 2.10.0, Activity Compose 1.13.0, Navigation Compose 2.9.6, DataStore 1.2.1, Coil 3.4.0
- [ ] **DEPS-08**: Hilt 2.57.1, coroutines 1.11.0, kotlinx.serialization 1.9.0 — KSP/Hilt aggregating-processor regression check passes (`./gradlew :app:kspDebugKotlin` clean)
- [ ] **DEPS-09**: Apollo Kotlin stays on 4.4.3 (deferred to future milestone); OkHttp stays on 4.12.0 (shared-transitive stability)
- [ ] **DEPS-10**: Media3 bumped to 1.10.0 only if `io.github.anilbeesetti:nextlib-media3ext:1.10.x` exists on Maven Central; otherwise stays on current pin with explicit rationale
- [ ] **DEPS-11**: Version catalog (`gradle/libs.versions.toml`) cleaned — unused Room artifacts removed, `kotlinx.collections.immutable` added, BOM-controlled libs use BOM-style refs
- [ ] **DEPS-12**: Detekt 1.23.8 + ktlint plugin 13.1.0 wired; existing detekt/ktlint baselines refreshed after format churn
- [ ] **DEPS-13**: Build green end-to-end: `./gradlew assembleDebug assembleRelease assembleBenchmark check` passes; `./gradlew :app:dependencies` reviewed for skews
- [ ] **DEPS-14**: Baseline profile regenerated in the same PR series as the dep bumps (stale profile is slower than no profile)

### Platform Compliance — Android Guideline Mandates (COMPLY)

- [ ] **COMPLY-01**: Edge-to-edge audit complete — `themes.xml` system-bar colors removed, `enableEdgeToEdge()` retained, every screen tested with `WindowInsets`-aware composables on gesture-nav *and* 3-button-nav devices
- [ ] **COMPLY-02**: Predictive back enabled — `android:enableOnBackInvokedCallback="true"` in manifest; all custom `BackHandler` / `OnBackPressedCallback` call sites audited; `PredictiveBackHandler` adopted on `FilterSheet`, `MoreSheet`, `NavCustomizeSheet`
- [ ] **COMPLY-03**: `POST_NOTIFICATIONS` runtime permission requested at the right moment (not at app start); graceful fallback when denied
- [ ] **COMPLY-04**: Splash Screen API migrated — `androidx.core:core-splashscreen` adopted, `installSplashScreen()` in `MainActivity`, `keepOnScreenCondition` driven by `RootViewModel.start`
- [ ] **COMPLY-05**: Orphan `FOREGROUND_SERVICE_MEDIA_PLAYBACK` permission removed from manifest (MediaSessionService deferred — see Out of Scope)
- [ ] **COMPLY-06**: Per-app language preference enabled — `generateLocaleConfig = true`, `locale_config.xml` generated, in-app language picker exposes the system per-app language dialog
- [ ] **COMPLY-07**: Manual `DEVICE_TESTING.md` checklist re-run on both a Pixel (gesture nav) and an OEM 3-button-nav device; all flows pass

### Performance — Measured Wins (PERF)

- [ ] **PERF-01**: Gradle-managed device wired (`Pixel 6 API 34`) for repeatable macrobench runs
- [ ] **PERF-02**: Compose Compiler stability reports wired into the `stash.android.library.compose` convention plugin; reports written to `build/compose-reports/`
- [ ] **PERF-03**: All `UiState` types audited; collection fields migrated to `kotlinx.collections.immutable.ImmutableList<T>` where strong-skipping benefits apply
- [ ] **PERF-04**: `feature/player` strong-skipping audit — every `LaunchedEffect` / `DisposableEffect` key list verified; auto-memoized lambdas not regressing effect re-launch behavior
- [ ] **PERF-05**: Baseline profile *expanded* beyond cold start to cover Home rails, Library scroll + filter, Detail open, Player start
- [ ] **PERF-06**: Cold-start macrobench shows **p50 ≥ 5% improvement with baseline profile vs without** on the GMD; reproducible across 3 back-to-back runs
- [ ] **PERF-07**: Library scroll macrobench shows **≥ 95% frames on time at p95** on the GMD
- [ ] **PERF-08**: **Shuffle / random consecutive playback fix** — diagnose and resolve the hang/slowdown after several videos; root cause documented (likely candidates: leaked `ExoPlayer` instance, accumulating `Player.Listener`s, retained `MediaSource` references, or `AndroidView.update` running `applyVideoFrameRate` per frame); macrobench / steady-state memory profile shows no growth across a 10-video shuffle session
- [ ] **PERF-09**: `applyVideoFrameRate` moved out of `AndroidView.update` into `LaunchedEffect(targetFps)` (per `CONCERNS.md`)
- [ ] **PERF-10**: Every perf claim in PR descriptions is backed by a checked-in macrobench output or baseline-profile delta — no unmeasured "feels faster" claims

### Polish, Test Pyramid & Documentation (POLISH)

- [ ] **POLISH-01**: `feature/player/PlayerScreen.kt` (1122 lines) split into `PlayerControls.kt` / `PlayerGestures.kt` / `PlayerSurface.kt`; behavior unchanged
- [ ] **POLISH-02**: Lint baseline shrunk — at least 30% of baselined issues resolved or explicitly documented as deferred
- [ ] **POLISH-03**: Detekt + ktlint re-baselined after upgrades; zero new violations in CI
- [ ] **POLISH-04**: JUnit5 + Turbine + MockK + Robolectric wired into `stash.android.library` convention plugin so every library module has a working test source-set out of the box
- [ ] **POLISH-05**: Seed test suites added — `core/common`, `core/model`, `core/domain` get baseline unit tests; one ViewModel per feature gets a state-machine test; one Compose smoke test per feature
- [ ] **POLISH-06**: `:core:domain` exposes `PlayerSettings` / `UiSettings` interfaces so `feature/*` modules no longer import `:core:data` directly for prefs (within-module refactor only)
- [ ] **POLISH-07**: `ConnectionResult` retired in favor of unified `AppResult`; `catch (e: Throwable)` blocks in repos narrowed and `CancellationException` rethrown
- [ ] **POLISH-08**: CI cache keys composed correctly (Gradle wrapper + JDK + AGP + libs.versions.toml hash); GitHub Actions workflow refreshed
- [ ] **POLISH-09**: Docs refreshed — `README.md`, `DEVICE_TESTING.md`, `build-logic/` conventions documented; `.planning/codebase/*` re-mapped at milestone end
- [ ] **POLISH-10**: Repository hygiene — `local.properties` removed from VCS, `.gitignore` audited, `keystore.properties` workflow documented via the existing `keystore.properties.example`

## Deferred to Future Milestones

### Phase 1 trim (DEPS-17, auto-logged 2026-05-16)

Recorded via plan 01.2 Task 4. Source of truth for the why: `.planning/phases/01-deps-foundation-bump/01.1-SUMMARY.md` (Option A unblock). Phase 1 stays on Gradle 8.11.1 / AGP 8.7.3 / compileSdk 35 because no published Hilt supports AGP 9 yet (latest is 2.56.2; the plan referenced a nonexistent 2.57.1).

- **DEPS-03** — Gradle 9.4.1 — blocked on Hilt AGP-9 support. Revisit once Dagger publishes a Hilt release that supports AGP 9.
- **DEPS-04** — AGP 9.2.0 + compileSdk/targetSdk 36 — same blocker. AGP 9 also broke `CommonExtension` generics, Action-taking DSL overloads, `LibraryExtension.targetSdk`, and the `org.jetbrains.kotlin.android` plugin; the migration deserves a dedicated phase.
- **DEPS-10 (1.10.0 upgrade path)** — Media3 1.10.0 + nextlibMedia3Ext 1.10.0-0.12.1 — requires compileSdk 36 (blocked by DEPS-04). Phase 1 keeps Media3 1.9.1 / nextlibMedia3Ext 1.9.1-0.11.0 (CASE B forced).

### Bump deferral (auto-logged 2026-05-16)
- **DEPS-07**: Lifecycle 2.10.0 and Activity Compose 1.13.0 require AGP 8.9.1+/compileSdk 36; Phase 1 stays on AGP 8.7.3/compileSdk 35 per Option A unblock (Hilt AGP-9 incompat). Full sweep deferred.
  - WR-01 (carry from Phase 1 01-REVIEW): :core:ui → :core:data dep added by cf5f1f0 introduces layering inversion. When DEPS-07 lands, relocate UiPreferences.imageCacheSizeMb accessor (or define an interface in :core:domain) and remove the dep edge from core/ui/build.gradle.kts.
- **DEPS-16**: Baseline profile regen requires a connected device or running emulator; this dev host has none and provisioning one is out of dev-box scope (REVIEWS C4 hard-fails the task without one). User-accepted deferral 2026-05-16 (see CONTEXT.md `## Deferred Ideas`). Existing committed `baseline-prof.txt` is retained; resume when a device/emulator is available.

### Performance (POLISH)
- **POLISH-CACHE-01** (carry from Phase 1 01-REVIEW WR-02): StashImageLoader.kt:33 uses runBlocking { ... } in Coil's newImageLoader() factory to read imageCacheSizeMb from DataStore. Pre-existing on master; surfaces under load when first image paint races DataStore first-emit. Replace with a Coil ImageLoader configured asynchronously via a Hilt-provided Provider<ImageLoader>.

Tracked but explicitly out of this modernization milestone.

### Background Media (BG-MEDIA)

- **BG-MEDIA-01**: Real `MediaSessionService` + `MediaController` binding with lock-screen / headphone / Bluetooth controls and background-audio survival
- **BG-MEDIA-02**: Notification-channel work for media playback once a service exists

### Security (SEC)

- **SEC-01**: Migrate `androidx.security:security-crypto` (EncryptedSharedPreferences) off the deprecated artifact once a stable replacement ships; design re-key/re-enter-credentials flow
- **SEC-02**: Tighten `network_security_config.xml` cleartext scope (currently global)
- **SEC-CI-01** (carry from Phase 1 01-SECURITY T-T-04 ACCEPT, 2026-05-17): Wire a Forgejo Actions workflow `.forgejo/workflows/dependency-check.yml` that runs `./gradlew dependencyCheckAggregate --no-configuration-cache` weekly (cron) on a CI runner with `NVD_API_KEY` provisioned via repo secrets. Local-host first-run took 2h 47m and faulted on `Region [POM] : Not alive` H2-lifecycle error without the API key. CI gives reproducible scans, archived HTML/JSON reports, and break-the-build on HIGH+ CVE (`failBuildOnCVSS=7.0f` already configured in `build.gradle.kts`). Also: add `dependencyCheckAggregate` to PR checks on `phase-*`/`release/*` branches.
- **SEC-VERIFY-01** (carry from Phase 1 01-SECURITY T-T-02 ACCEPT, 2026-05-17): Generate `gradle/verification-metadata.xml` via `./gradlew --write-verification-metadata sha256 help` to lock every transitive dependency hash. File will be several thousand lines of XML and deserves its own phase (review impact: every dep bump requires re-generation; team must agree on policy). Best landed alongside the AGP-9 migration (DEPS-03/04) when the catalog churns anyway.

### Networking (NET)

- **NET-01**: Apollo Kotlin 4.x → 5.x migration (normalized-cache module restructure, warnings-to-errors)

### Navigation (NAV)

- **NAV-01**: Migrate from Navigation Compose 2.x to Navigation 3 (deep-link audit, route restructure)

### Architecture (ARCH)

- **ARCH-01**: Extract `:core:player-capabilities` to break the `feature/settings` → `feature/player` direct dep (module-graph change — out of current scope)

## Out of Scope (this milestone)

Explicitly excluded; reasons documented to prevent scope creep.

| Item | Reason |
|------|--------|
| New end-user features | Modernization-only milestone — no behavior change beyond perf + compliance |
| `minSdk` bump | Must stay installable on existing devices |
| New third-party SDKs | No vendor additions without explicit approval |
| Module-graph restructure (`app/` / `core/` / `feature/` / `build-logic/`) | Frozen — refactors stay within modules |
| Migrating off Compose / Hilt / Gradle Kotlin DSL | Existing architecture preserved |
| Real `MediaSessionService` (background playback) | New capability — deferred (see BG-MEDIA above) |
| EncryptedSharedPreferences replacement | Replacement is alpha; modernization is stable-only |
| Apollo 5 / Nav3 migrations | Each deserves its own milestone |
| "Feels faster" perf claims | Every perf change must show baseline-profile or macrobench delta |
| Sentry / Crashlytics / Firebase | No new vendor dependencies |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| DEPS-01 … DEPS-14 | Phase 1 (DEPS) | Pending |
| COMPLY-01 … COMPLY-07 | Phase 2 (COMPLY) | Pending |
| PERF-01 … PERF-10 | Phase 3 (PERF) | Pending |
| POLISH-01 … POLISH-10 | Phase 4 (POLISH) | Pending |

**Coverage:**
- Milestone requirements: 41 total
- Mapped to phases: 41
- Unmapped: 0

---
*Requirements defined: 2026-05-16*
*Last updated: 2026-05-16 after initialization*
