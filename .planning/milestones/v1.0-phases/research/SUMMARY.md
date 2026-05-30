# Project Research Summary — Slopper Android Modernization

**Project:** Slopper (Android Compose multi-module client)
**Domain:** Brownfield Android modernization (Kotlin/Compose, Gradle multi-module)
**Researched:** 2026-05-16
**Confidence:** HIGH on stack/pitfalls/architecture; MEDIUM on project-specific inferences (PlayerScreen strong-skipping behavior, `nextlib-media3ext` lockstep availability).

## Executive Summary

- Modernization-only milestone per `PROJECT.md`: no new features, frozen `minSdk` (26), frozen module graph, stable releases only, every perf claim measured.
- Natural fault line: **AGP 9 / Kotlin 2.2 / Compose BOM 2026.05 / `compileSdk = 36`**. Everything downstream (predictive back, edge-to-edge, MediaSessionService, dynamic color, baseline-profile regen) is either *forced* by `targetSdk = 36` or only becomes *safe* once the toolchain is in place (see `STACK.md §"Migration Plan"`).
- Biggest cross-cutting risk is `feature/player/PlayerScreen.kt` (1122 lines, Compose + AndroidView + Activity manipulation, zero tests). Strong-skipping (`PITFALLS.md §5`), edge-to-edge enforcement (`PITFALLS.md §7`), predictive back (`PITFALLS.md §8`), and the still-missing `MediaSessionService` (`FEATURES.md §"Foreground-service type"`, `CONCERNS.md §"No foreground service yet"`) all converge here.
- Three open scope decisions block roadmap finalization: (1) `MediaSessionService` scope, (2) `androidx.security:security-crypto` deprecation, (3) Apollo 5 vs 4.4.3. Conservative defaults: defer 1, defer 2, stay on 4.4.3.
- Measurement is non-negotiable. Baseline profile is currently shallow (cold start only — `CONCERNS.md`) and must be regenerated in the same PR as every BOM/Media3/Apollo/Coil/Kotlin/Hilt bump (`PITFALLS.md §6`). A Gradle-managed device is required for macrobench claims to be more than noise (`PITFALLS.md §13`).

## Recommended Phase Shape (4 phases, coarse-grained)

### Phase 1 — Foundation Bump (Dependencies & Build Infra)

**Goal:** Land the toolchain and dependency floor; keep the build green.

**Deliverables:** Generate `app/lint-baseline.xml` *first* (`PITFALLS.md §18`); sequenced bumps lint-baseline → JDK toolchain pin → Gradle 9.4.1 → AGP 9.2.0 → Kotlin 2.2.20 + matching KSP + matching Compose Compiler → `compileSdk = 36`; Compose/AndroidX sweep; Hilt/coroutines/serialization sweep; Apollo 4.4.3 + Coil 3.4.0 + Media3 1.10 (conditional on `nextlib-media3ext:1.10.x` availability); catalog hygiene (drop unused Room, audit BOM-controlled `version.ref`, add `kotlinx.collections.immutable`); R8 keep-rule re-spec for AGP 9 + serialization 1.9 (`PITFALLS.md §9`); `-Xcontext-receivers` → `-Xcontext-parameters` (`PITFALLS.md §12`).

**Verification gate:** `assembleDebug + assembleRelease + assembleBenchmark` green; `./gradlew :app:dependencies` diff reviewed for skews (`PITFALLS.md §10`); detekt/ktlint green; benchmark APK smoke on every primary screen; baseline profile regenerated in this PR series.

### Phase 2 — Platform Compliance (Guidelines / Mandates)

**Goal:** Resolve every framework-enforced contract that `targetSdk = 36` makes mandatory.

**Deliverables:** Edge-to-edge audit + `themes.xml` cleanup (`FEATURES.md §"Edge-to-edge"`, `PITFALLS.md §7`); predictive back manifest flag + `BackHandler` audit (`PITFALLS.md §8`); `POST_NOTIFICATIONS` runtime request; Splash Screen API migration (`core-splashscreen`, `installSplashScreen()`, `keepOnScreenCondition` on `RootViewModel.start`); **conditional `MediaSessionService` scaffolding pending user decision** (Open Decision #1).

**Verification gate:** Manual `DEVICE_TESTING.md` checklist on both a Pixel gesture-nav device *and* a 3-button-nav OEM device.

### Phase 3 — Modern Citizen + Performance Measurement

**Goal:** User-visible polish + measurement work backing every perf claim.

**Deliverables:** Material You dynamic color (gated off for player route); WindowSizeClass + `NavigationSuiteScaffold`; `generateLocaleConfig = true`; `PredictiveBackHandler` on `FilterSheet` / `MoreSheet` / `NavCustomizeSheet`; Compose Compiler reports wired into convention plugin; UiState audit + `ImmutableList` migration; strong-skipping `LaunchedEffect` audit in `feature/player` (`PITFALLS.md §5`); baseline profile expansion (cold start + Home rails + Library scroll/filter + Detail open + Player start); Gradle-managed device (`Pixel 6 API 34`) wired (`PITFALLS.md §13`); macrobench scroll-jank for Library (p50 + p95); move `applyVideoFrameRate` out of `AndroidView.update` into `LaunchedEffect(targetFps)` (`CONCERNS.md`).

**Verification gate:** Macrobench p50 reproducible across 3 back-to-back runs; profile-installed cold-start ≥15% faster than no-profile.

### Phase 4 — Polish / Test Pyramid Bootstrap

**Goal:** Shrink lint baseline, wire test infrastructure, refactor highest-risk file, close doc drift.

**Deliverables:** Split `PlayerScreen.kt` into `PlayerControls.kt` / `PlayerGestures.kt` / `PlayerSurface.kt` (`CONCERNS.md`); lint baseline shrink; detekt + ktlint re-baseline (`PITFALLS.md §12`); JUnit5/Turbine/MockK/Robolectric wiring via `stash.android.library` convention plugin (`ARCHITECTURE.md §9`); seed tests `core/common` + `core/model` + `core/domain` → ViewModel state-machine tests → per-feature Compose smokes; introduce `PlayerSettings`/`UiSettings` interfaces in `:core:domain` to fix prefs-import anti-pattern; retire `ConnectionResult` → `AppResult`; narrow `catch (e: Throwable)` swallowing in repos (rethrow `CancellationException`); CI cache key composition + GitHub Actions setup (`PITFALLS.md §15`); doc refresh; `.gitignore` + remove `local.properties` from VCS (`CONCERNS.md` restoration gaps).

**Verification gate:** `./gradlew check` green; zero new lint/detekt; coverage report exists (floor TBD).

## Cross-Cutting Risks (Top 5 from `PITFALLS.md`)

1. **Stale baseline profile after dep bump** — slower than no profile; CI freshness check + same-PR regen rule. (`§6`)
2. **Strong-skipping regression in `PlayerScreen`** — auto-memoized lambdas stop re-triggering `LaunchedEffect`; not catchable by lint. (`§5`)
3. **R8 keep-rule invalidation** under AGP 9 + serialization 1.9 + Apollo 4.4.x — crashes only in release. (`§9`)
4. **Edge-to-edge breaks player gestures + filter-sheet insets** on 3-button-nav OEM devices. (`§7`)
5. **KSP / Kotlin / Compose-Compiler version drift** — silent miscompilation or stale Hilt codegen → `MissingBindingException`. (`§§3, 4`)

Honorable mentions: JDK toolchain pin (`§1`), config-cache from OWASP carve-out (`§2`), Hilt aggregating-processor NPE (`§11`), `nextlib-media3ext` lockstep coupling (`§16`).

## Open Decisions for the User (block roadmap finalization)

### Decision 1 — `MediaSessionService` scope (HIGH IMPACT)

Manifest declares `FOREGROUND_SERVICE_MEDIA_PLAYBACK` but no `<service>` exists. `FEATURES.md` calls this "the single biggest modernization win," but it is HIGH complexity and arguably a *new capability* (today's app has no background audio). Options: **(A)** in scope — full service + `MediaController` binding; **(B)** out of scope — remove orphan permissions, defer; **(C)** stub scaffold without wiring. *Recommendation:* **(B)** if scope-disciplined, **(A)** if the manifest/code gap matters more.

### Decision 2 — `androidx.security:security-crypto` migration

Officially deprecated; replacement (DataStore-encrypted) is alpha. *Recommendation:* **defer.** Pin 1.1.0, leave TODO in `ConnectionStore.kt`, file follow-up milestone.

### Decision 3 — Apollo 5 vs 4.4.3

Apollo 5 reshuffles normalized-cache modules, promotes warnings to errors, bumps KGP requirements. *Recommendation:* **stay on 4.4.3** — Apollo 5 is its own milestone.

(Minor) OkHttp 4.12 vs 5.x: stay on 4.12.0 for shared-transitive stability across Apollo / Media3 / Coil.

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack target versions | HIGH | Cross-checked against developer.android.com / GitHub / JetBrains 2026-05-16. |
| Feature mandates | HIGH | Cross-referenced against Slopper's manifest/`MainActivity.kt`/`themes.xml`. |
| Architecture deltas | HIGH | Google 2026 guide + corroborated by codebase map. |
| Pitfalls | HIGH (upstream) / MEDIUM (project-specific inference, esp. PlayerScreen). |
| Nav3 timing | LOW–MEDIUM | Out of scope this milestone. |
| `nextlib-media3ext:1.10.x` availability | MEDIUM | Re-verify on Maven Central at phase-execution time. |

**Known gaps to resolve during planning:**
- Verify current `targetSdk` in `build-logic/convention/.../AndroidApplicationConventionPlugin.kt` (affects predictive-back vs FGS-type sequencing).
- `PROJECT.md` doesn't define a numeric floor for "measurable perf win" — Phase 3 should set one (suggest p50 cold-start ≥ 5%, scroll-jank ≥ 95% frames on time on Pixel 6).
- Test-coverage target for Phase 4 unresolved (global rule is 80%, milestone starts from ~0%).

## Required Reading for Downstream Agents

| Question | Read |
|----------|------|
| Exact target version for any library/tool | `STACK.md §"Recommended Stack — Target Versions"` |
| What `compileSdk = 36` forces in code | `FEATURES.md §"Table Stakes"` + `PITFALLS.md §§7, 8` |
| Is feature X in scope at all | `FEATURES.md §"Anti-Features"` + `PROJECT.md §"Out of Scope"` |
| Phase-ordering rationale | `ARCHITECTURE.md §"Build / Dependency Order"` |
| Per-phase verification checklist | `PITFALLS.md §"Looks Done But Isn't"` + `§"Pitfall-to-Phase Mapping"` |
| Risk register for a specific bump | `PITFALLS.md §§1–18` |
| `PlayerScreen` modernization specifics | `CONCERNS.md §"Large player file"`, `PITFALLS.md §§5, 7` |
| Existing code state | `.planning/codebase/STACK.md`, `CONCERNS.md`, `ARCHITECTURE.md` |
| Constraints from product owner | `PROJECT.md §"Constraints"` |

---
*Generated 2026-05-16 from STACK.md / FEATURES.md / ARCHITECTURE.md / PITFALLS.md.*
