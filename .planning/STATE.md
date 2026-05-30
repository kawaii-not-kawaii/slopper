---
gsd_state_version: 1.0
milestone: v1.1
milestone_name: AGP-9 Toolchain Modernization
current_phase: 07
status: Executing Phase 07
last_updated: "2026-05-30T10:09:00.000Z"
progress:
  total_phases: 4
  completed_phases: 0
  total_plans: 2
  completed_plans: 1
  percent: 13
---

# Project State

**Project:** Slopper (Android Compose multi-module app)
**Milestone:** v1.1 — AGP-9 Toolchain Modernization (DEPS-17) — pure build-toolchain upgrade, no end-user features
**Initialized:** 2026-05-30 (v1.0 shipped 2026-05-29, tag `v1.0`)

## Where Things Stand

- **Milestone defined:** YES (`PROJECT.md`, `REQUIREMENTS.md` updated for v1.1)
- **Research complete:** YES (`.planning/research/` — STACK, FEATURES, ARCHITECTURE, PITFALLS, SUMMARY; HIGH confidence)
- **Roadmap created:** YES (Phases 7–10 appended to `ROADMAP.md`)
- **Current phase:** 07
- **Plans created:** 2
- **Plans executed:** 1 (07.1 — Gradle-9 readiness + deprecation sweep)

## Roadmap Snapshot (v1.1)

| # | Phase | Status | Plans | Requirements |
|---|-------|--------|-------|--------------|
| 7 | **GRADLE-9** — Core Version Bump + Deprecation Sweep | In progress | 1/2 | AGP9-01 |
| 8 | **AGP-9** — Atomic Build-Logic Migration + compileSdk 36 (the indivisible core) | Not started | 0/? | AGP9-02, AGP9-03, SDK-01 |
| 9 | **LIBS** — Green-Gated Library Bumps (Media3/nextlib pair + leaf libs) | Not started | 0/? | LIB-01, LIB-02 |
| 10 | **CI-SIGNING** — Isolated Assemble/Signing Probe (last, non-gating) | Not started | 0/? | CI-01 |

**Coverage:** 7/7 requirements mapped. No orphans.

### Phase Ordering Rationale

- **Phase 8 is the one atomic phase** — Gradle→AGP→drop-`kotlin.android`→`CommonExtension` fix→`compilerOptions`→Hilt 2.59.2 is an indivisible change-set; the convention plugins fail all ~14 modules until it lands whole. compileSdk 36 rides here.
- **Versions before DSL** (7 before 8) isolates version-resolution breakage from DSL-migration breakage (enables `git bisect`).
- **Hilt 2.59.2 strictly after AGP 9** — 2.59 drops AGP 8 entirely.
- **compileSdk 36 before Media3/leaf libs** — AAR `minCompileSdk` consumer rule rejects SDK-35 modules consuming SDK-36 libs.
- **CI signing isolated and LAST** — must never gate the toolchain landing; EdEC/bcprov is not AGP-fixed.

### Per-Phase Research Flags

- **Phase 7:** OWASP dependency-check 12.2.2 Gradle-9 compatibility UNKNOWN — confirm or budget `--no-configuration-cache`.
- **Phase 8:** Confirm 3 version disagreements (AGP 9.2.1/9.2.0, Gradle floor, Hilt 2.59.2/2.59.1) at plan time; verify `KotlinAndroidProjectExtension` resolves under built-in Kotlin.
- **Phase 9:** Lighter — mechanical bumps; verify the `1.10.0-0.12.1` pair + device software-codec smoke test.
- **Phase 10:** EdEC/bcprov spike — capture the full stack trace on an AGP-9 runner before deciding the gate.

## Target Version Matrix (from STACK.md — confirm exact patches at plan time)

| Key | FROM | TO |
|-----|------|----|
| AGP | 8.7.3 | 9.2.1 |
| Gradle wrapper | 8.11.1 | 9.4.1 (+ new sha256) |
| Hilt/Dagger | 2.56.2 | 2.59.2 (never bare "2.59+") |
| Kotlin (KGP) | 2.2.20 | unchanged |
| KSP | 2.2.20-2.0.4 | unchanged |
| compileSdk | 35 | 36 (targetSdk stays 35 explicit) |
| Media3 | 1.9.1 | 1.10.0 (HARD CAP — not 1.10.1) |
| nextlib-media3ext | 1.9.1-0.11.0 | 1.10.0-0.12.1 |
| activity-compose | current | 1.13 |
| core-ktx | current | 1.18 |

## Key Locked Decisions (v1.1)

- **No Kotlin/KSP bump** — AGP 9 needs only KGP ≥ 2.2.10; 2.2.20 satisfies. Chasing 2.3.x re-triggers KSP/Hilt churn for no gain.
- **No `targetSdk` bump** — must stay explicit at 35 in all 3 sites or AGP 9 silently flips it via `defaultTargetSdkToCompileSdkIfUnset`. Highest-consequence silent failure.
- **Do NOT add `android.enableLegacyVariantApi=true` or `newDsl=false`** — no-op/doomed crutches; repo is already clean (zero `applicationVariants`/`buildDir`).
- **Media3 hard cap at 1.10.0** — nextlib has no 1.10.1 pairing; mismatch causes runtime `UnsatisfiedLinkError`.
- **CI signing: compile-only gate stays the contract** — `assembleDebug` is a `continue-on-error` probe only; never restored on faith.

## Carried Tech Debt (from v1.0)

- Macrobenchmark execution (PERF-MB-01), formal screenshot audit, COMPLY-07-3BTN, COMPLY-02-NAV-EVENT, WR-02 ViewModel refactor, APOLLO-CACHE-01 — all deferred, none in this milestone.

## Workflow Config

- Mode: YOLO
- Granularity: Coarse (4 phases)
- Parallel plans: enabled
- Commit docs: yes
- Model profile: Quality (Opus for research / roadmap)
- Workflow agents enabled: Research, Plan Check, Verifier

## Next Step

Plan 07.1 (Gradle-9 readiness + deprecation sweep) executed: deprecation inventory captured (ZERO emitted on 8.11.1), own-script audit clean, CI gate green, wrapper unchanged. Proceed with plan 07.2 (detekt-on-Gradle-9 governance decision).

## Decisions (accumulated)

- **07.1** — Wrapper stays on `gradle-8.11.1-bin.zip`; the 9.4.1 flip folds forward to Phase 8 commit 1 (AGP 8.7.3 hard-fails on Gradle 9).
- **07.1** — detekt 1.23.8 retained; its Gradle-9 warn-vs-fail behavior is untestable until AGP 9 (Phase 8). Do NOT adopt the 2.0.0-alpha line.
- **07.1** — Repo-authored build scripts independently confirmed Gradle-9-clean (only `layout.buildDirectory`; no `buildDir`/`Project.exec`/`getConvention`/`JavaPluginConvention`/`archivesBaseName`).
- **07.1** — `gradle9-deprecations.log` force-added past the global `*.log` gitignore (scoped single-path `-f`; `.gitignore` untouched).

---
*Last updated: 2026-05-30 — plan 07.1 executed (Gradle-9 readiness + deprecation sweep); Phase 7 at 1/2.*
