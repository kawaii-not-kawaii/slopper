---
phase: 02-comply-platform-compliance
mode: documented-stub
nyquist_validation_enabled: false
config_source: .planning/config.json `workflow.nyquist_validation`
generated: 2026-05-18
verdict: COVERED (platform compliance phase — verification via build + device UAT)
counts:
  requirements_total: 7
  covered: 6
  partial: 1
  missing: 0
  generated_tests: 0
test_infrastructure_changes: []
---

# Phase 2 Validation: COMPLY — Platform Compliance

## TL;DR

Phase 2 is a **platform compliance phase** — manifest edits, Compose modifier additions, permission removals, and a build-flag change (`generateLocaleConfig`). It introduces **zero new business-logic behavior surface** — no new ViewModels, no new data flows, no new UI state machines, no new API calls. Every code change is either (a) a manifest attribute/permission change, (b) a Compose modifier addition to existing composables, (c) a library wiring (core-splashscreen), or (d) a build-flag + resource file addition.

The Nyquist validation gate is configured `false` in `.planning/config.json` for this project. This stub records the explicit rationale for spec-layer step 9 closure. The COMPLY requirements are validated by three complementary evidence streams:
1. **Build verification** — `./gradlew assembleDebug assembleRelease` green at commit `06b5571`
2. **Codebase grep checks** — 25 acceptance-criterion grep checks in VERIFICATION.md (all PASS)
3. **Device UAT** — 45-row gesture-nav PASS on Galaxy S23+ Android 16 (02-UAT.md)

## Disposition per requirement

| Req | Disposition | Validation evidence |
|-----|-------------|---------------------|
| COMPLY-01 | COVERED — grep + UAT | `themes.xml` bar-color lines removed (grep count = 0); 3 `ModalBottomSheet contentWindowInsets` sites verified (grep count = 3); `safeDrawingPadding` applied (grep count ≥ 2). UAT rows: edge-to-edge rendering on S23+ gesture-nav. VERIFICATION.md COMPLY-01 row: COVERED. |
| COMPLY-02 | COVERED — grep + UAT | `enableOnBackInvokedCallback="true"` in manifest (grep count = 1); `PredictiveBackHandler` at PlayerScreen.kt:188 (grep count ≥ 2); zero `BackHandler` remaining in feature/ (grep count = 0); `CancellationException` rethrow present (grep count = 1). UAT rows: predictive-back gesture on S23+. VERIFICATION.md COMPLY-02 row: COVERED. |
| COMPLY-03 | COVERED — grep | `POST_NOTIFICATIONS` removed from manifest (grep count = 0); pre-removal safety grep confirmed zero notification call sites. VERIFICATION.md COMPLY-03 row: COVERED. |
| COMPLY-04 | COVERED — grep + UAT | `installSplashScreen()` present at line 99 BEFORE `super.onCreate()` at line 102; `setKeepOnScreenCondition` present; `appReady` AtomicBoolean with LaunchedEffect gate + 3s safety-timeout; Theme.Stash.Splash declared; dedup invariant = EXACTLY 1 `collectAsState` for `start`. UAT rows: cold launch splash on S23+. VERIFICATION.md COMPLY-04 row: COVERED. |
| COMPLY-05 | COVERED — grep | `FOREGROUND_SERVICE_MEDIA_PLAYBACK` removed (grep count = 0); base `FOREGROUND_SERVICE` preserved (grep count = 1). VERIFICATION.md COMPLY-05 row: COVERED. |
| COMPLY-06 | COVERED — grep + build artifact | `generateLocaleConfig = true` in `app/build.gradle.kts`; `resources.properties` present with `unqualifiedResLocale=en`; `_generated_res_locale_config.xml` exists under `app/build/`; `LanguageRow` composable present with `Build.VERSION.SDK_INT >= TIRAMISU` gate and `ACTION_APP_LOCALE_SETTINGS` intent. VERIFICATION.md COMPLY-06 row: COVERED. |
| COMPLY-07 | PARTIAL — verbal UAT (PNG pack skipped; 3-button-nav deferred) | 45 gesture-nav PASS rows in 02-UAT.md (verbal verdict "all pass" on S23+ Android 16). 4 rows DEFERRED → COMPLY-07-3BTN (hardware-blocked). PNG screenshot pack: NOT PRODUCED (accepted risk COMPLY-07-NO-PNG, reviewer-elected skip). VERIFICATION.md COMPLY-07 row: PARTIAL (verbal-PASS path accepted). Re-shoot trigger: Phase 5 (Spine redesign). |

## Test infrastructure (unchanged)

Phase 2 made **zero changes** to test infrastructure:
- No new test source sets created.
- No test dependencies added.
- Existing test directories: none (test pyramid not yet wired — deferred to Phase 4 POLISH-04/05).

## Why automated Nyquist tests are not generated

The 7 COMPLY requirements are platform-contract verifications:
- **Manifest attributes** — validated by build (AGP validates manifest at compile time) and by grep (acceptance criteria in VERIFICATION.md).
- **Compose modifier additions** — `safeDrawingPadding`, `contentWindowInsets` — validated by visual device testing (cannot be unit-tested without device/emulator in this project's current test infrastructure state).
- **Splash Screen API** — requires Activity lifecycle; not testable without Robolectric or instrumented tests (not yet wired; deferred to Phase 4 POLISH-04).
- **PredictiveBackHandler** — requires system back dispatcher interaction; not testable without instrumented tests.
- **Permission removals** — validated by manifest grep (build-time, no runtime test needed).

The POLISH phase (Phase 4) will wire JUnit5 + Robolectric + MockK + Turbine, at which point COMPLY-01/02/04 can receive instrumented regression tests. At that point, this VALIDATION.md should be revisited.

## Backlog test coverage items

These items are NOT blocking Phase 2 closure but should be tracked for Phase 4:
- **COMPLY-01-TEST** — Compose UI test: edge-to-edge rendering on each top-level Scaffold + PlayerScreen (Robolectric + `captureRoboImage` or instrumented screenshot test)
- **COMPLY-02-TEST** — Instrumented test: `PredictiveBackHandler` fires `onExit()` on committed gesture, does NOT fire on cancelled gesture
- **COMPLY-04-TEST** — Robolectric test: `MainActivity` launches without calling `setKeepOnScreenCondition` more than once; `installSplashScreen()` returns non-null splashScreen
- **COMPLY-06-TEST** — Unit test: `LanguageRow` composable is not emitted when `Build.VERSION.SDK_INT < 33`; emitted when ≥ 33 (requires fake `LocalContext`)

## Sign-Off

- [x] All 7 requirements have dispositions
- [x] 6 COVERED, 1 PARTIAL (verbal-UAT path; no functional regressions)
- [x] `nyquist_validation_enabled: false` — documented-stub mode; explicit rationale recorded
- [x] Backlog test items documented for Phase 4 POLISH-04/05
