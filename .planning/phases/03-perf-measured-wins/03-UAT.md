# Phase 3 PERF — UAT

**Phase:** 3 — PERF (Measured Wins)
**Status:** PASS-WITH-NOTES — UAT deferred to end-of-milestone
**Date:** 2026-05-19
**Mode:** Deferred (all human testing batched per user instruction)

## Verdict: PASS-WITH-NOTES

Functional code is verified via build verification + codebase grep checks (8/10 SPEC bullets COVERED). Device-dependent tests are deferred to end-of-milestone human testing session.

## Automated Verification (COVERED)

| Req | Check | Result |
|-----|-------|--------|
| PERF-01 | GMD declared in baselineprofile/build.gradle.kts | PASS |
| PERF-02 | ComposeCompilerGradlePluginExtension configured | PASS |
| PERF-03 | ImmutableList migration complete (0 List<T> in feature/) | PASS |
| PERF-04 | LaunchedEffect audit — all 5 sites STABLE | PASS |
| PERF-08 | Shuffle fix committed (onSceneEnded null return + banner) | PASS |
| PERF-09 | applyVideoFrameRate in LaunchedEffect, NOT AndroidView.update | PASS |
| PERF-10 | .planning/benchmarks/ exists; 03-UAT-DEFERRED.md committed | PASS |

## Deferred Verification (device-dependent)

| Req | What's needed | Deferred artifact |
|-----|---------------|-------------------|
| PERF-05 | Run profile generator to produce baseline-prof.txt | 03-UAT-DEFERRED.md §PERF-05 |
| PERF-06 | ColdStartBenchmark execution on GMD/device, p50 ratio ≥ 1.05 | 03-UAT-DEFERRED.md §PERF-06 |
| PERF-07 | LibraryScrollBenchmark execution, ≥ 95% frames on time at p95 | 03-UAT-DEFERRED.md §PERF-07 |
| PERF-08 | Live 10-video shuffle session, heap dump confirming fix | 03-UAT-DEFERRED.md §PERF-08 |

## Accepted Risks

### AR-03-01: GMD system image unavailable
If Pixel 6 API 34 system image cannot be downloaded:
- PERF-06 and PERF-07 formally deferred via REVIEWS-C4-ACCEPT
- PERF-01 (GMD declaration) remains SATISFIED
- Revisit trigger: CI runner with emulator capability

### AR-03-02: Shuffle fix scope
Root cause was code-level (onSceneEnded null return with RepeatMode.OFF → silent stop). Fix: emit "End of queue" banner. Live profiling deferred to confirm no secondary heap issue.
