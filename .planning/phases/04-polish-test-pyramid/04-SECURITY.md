---
phase: 4
slug: polish-test-pyramid
status: verified
threats_open: 0
threats_total: 8
threats_closed: 8
asvs_level: 1
created: 2026-05-19
audited: 2026-05-19
register_authored_at_plan_time: true
---

# Phase 4 — POLISH: Security Threat Verification

> Per-phase security contract.
> register_authored_at_plan_time: true (all 3 PLAN.md files contain formal <threat_model> blocks)

## Threat Register

| Threat ID | Category | Component | Disposition | Mitigation | Status |
|-----------|----------|-----------|-------------|------------|--------|
| T-04-01 | Tampering | PlayerScreen split breaks Phase 3 optimizations | mitigate | Build green + explicit grep checks for PERF-04/09/COMPLY-01/02 all pass. Verified in VERIFICATION.md 14/14 truths. | closed |
| T-04-02 | Denial of Service | ConnectionResult consumers missed | mitigate | `grep -rn 'ConnectionResult' feature/ core/` → 0 hits confirmed. | closed |
| T-04-03 | Tampering | JUnit5 wiring breaks :baselineprofile module | accept | `:baselineprofile` uses JUnit4 and does NOT apply `stash.android.library` convention plugin. JUnit4 catalog entry preserved. | closed |
| T-04-04 | Denial of Service | Robolectric incompatibility in test wiring | mitigate | `./gradlew test` → BUILD SUCCESSFUL with 17 tests passing. | closed |
| T-04-05 | Tampering | local.properties removal breaks builds | mitigate | `local.properties` contains only `sdk.dir`; all builds use `ANDROID_SDK_ROOT` env var. `git ls-files local.properties` → 0. | closed |
| T-04-06 | Information Disclosure | CI workflow exposes secrets | accept | Workflow uses no `secrets.*` references; runs `assembleDebug detekt ktlintCheck` only. | closed |
| T-04-07 | Denial of Service | CancellationException swallowed in catch narrowing | mitigate | All 13 narrowed catch sites confirmed to rethrow `CancellationException` first. `grep -c 'CancellationException' core/data/src/` → ≥ 13. | closed |
| T-04-08 | Tampering | Stale detekt baseline IDs cause false violations | mitigate | `feature/player/detekt-baseline.xml` and `core/data/detekt-baseline.xml` regenerated. `./gradlew detekt` exits 0. | closed |

## Accepted Risks Log

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|-------------|------|
| AR-04-01 | T-04-03 | `:baselineprofile` uses JUnit4; not affected by `stash.android.library` convention plugin changes | Plan author (04.2-PLAN.md T-04-03 accept) | 2026-05-19 |
| AR-04-02 | T-04-06 | Self-hosted Forgejo instance; no external secret exposure | Plan author (04.3-PLAN.md T-04-06 accept) | 2026-05-19 |
| AR-04-LINT | N/A | Coil 3.0.4/AGP 8.7.3 lint incompatibility prevents `lintDebug`. Pre-existing from Phase 1. CI intentionally excludes lint. Revisit on Coil 3.1.x+ upgrade. | 04.3-SUMMARY.md | 2026-05-19 |

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-05-19 | 8 | 8 | 0 | Claude (gsd-secure-phase, register_authored_at_plan_time: true) |

## Sign-Off

- [x] All 8 threats have a disposition
- [x] `threats_open: 0` confirmed
- [x] `status: verified` set
