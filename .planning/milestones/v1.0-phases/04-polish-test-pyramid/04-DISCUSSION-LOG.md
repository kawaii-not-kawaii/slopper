# Phase 4 Discussion Log

**Phase:** 4 — POLISH (Test Pyramid & Cleanup)
**Mode:** `--auto` (single-pass autonomous)
**Date:** 2026-05-19

## Gray Areas Auto-Resolved

| Area | Question | Selected Option | Rationale |
|------|----------|-----------------|-----------|
| PlayerScreen split | Which 3 files? | PlayerScreen.kt + PlayerControls.kt + PlayerTimeline.kt | Natural composable boundaries (entry/surface, controls, timeline); gesture code stays with entry point |
| Test framework versions | JUnit5/MockK/Turbine/Robolectric versions? | 5.11.4 / 1.14.0 / 1.2.0 / 4.14.1 | Latest stable at spec date |
| PlayerSettings/UiSettings scope | Minimal or full interface? | Expose all currently-consumed prefs | Minimal diff; no behavior change |
| Lint shrink strategy | Fix or defer? | Fix high-yield categories first | UnusedResources, ObsoleteSdkInt, import cleanup; defer ContentDescription/security |
| Plan wave structure | Parallel or sequential? | All 3 wave 1 parallel | Disjoint file sets confirmed |
| Seed test depth | Wiring or assertions? | Wiring verification (basic assertions) | Phase goal is "pyramid wired", not "80% coverage" |
| Throwable narrowing | Pattern? | CancellationException rethrow first, then typed catches | Standard Kotlin structured concurrency pattern |

## Deferred Ideas

None — all discussed items are within Phase 4 scope per ROADMAP.md.

## Claude's Discretion Items

- Exact setter methods in PlayerSettings/UiSettings interfaces
- Whether feature/library's :core:data dep is solely for UiPreferences
- Robolectric SDK level for smoke tests
- Test assertion depth (minimal wiring verification)
