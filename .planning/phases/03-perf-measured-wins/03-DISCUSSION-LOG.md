# Phase 3 Discussion Log

**Phase:** 3 — PERF (Measured Wins)
**Mode:** `--auto` (single-pass autonomous; no AskUserQuestion)
**Date:** 2026-05-18

## Gray Areas Auto-Resolved

| Area | Question | Selected Option | Rationale |
|------|----------|-----------------|-----------|
| GMD Configuration | Which device/API for GMD? | Pixel 6 API 34, google_apis | ROADMAP.md specifies this device; google_apis for Play Services compat |
| Stability Reports DSL | Which Compose Compiler extension DSL? | `ComposeCompilerGradlePluginExtension.reportsDestination` | Standard Kotlin 2.x Compose Compiler plugin API |
| ImmutableList Boundary | Where to call `toPersistentList()`? | ViewModel update block | Closest to UI consumption; keeps repo types clean |
| Baseline Profile Journeys | Which 4 journeys to add? | cold-start + Home rails + Detail + Player first-frame | Matches ROADMAP.md suggested scope; covers all 4 major user flows |
| Shuffle Investigation | Which tool first? | LeakCanary then Profiler heap | Listener accumulation is the primary hypothesis; LeakCanary is lowest overhead |
| Plan Wave Structure | How to sequence 3 plans? | 3.1 wave 1; 3.2+3.3 wave 2 parallel | 3.1 provides GMD + reports needed by 3.2/3.3; 3.2/3.3 touch disjoint surfaces |

## Deferred Ideas

None — all discussed items are within Phase 3 scope per ROADMAP.md.

## Claude's Discretion Items

- Exact Compose Compiler plugin DSL syntax (may differ slightly in Kotlin 2.2.20)
- `compose_stability.conf` placement
- `playerView` hoisting strategy in PERF-09 relocation
- System image choice (`google_apis` vs `aosp_atd`) if google_apis unavailable
