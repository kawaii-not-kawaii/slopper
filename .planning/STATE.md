---
gsd_state_version: 1.0
milestone: v1.1
milestone_name: AGP-9 Toolchain Modernization
current_phase: 08
status: Executing Phase 08
last_updated: "2026-05-30T11:45:28.612Z"
progress:
  total_phases: 4
  completed_phases: 1
  total_plans: 3
  completed_plans: 2
  percent: 25
---

# Project State

**Project:** Slopper (Android Compose multi-module app)
**Milestone:** v1.1 — AGP-9 Toolchain Modernization (DEPS-17) — pure build-toolchain upgrade, no end-user features
**Initialized:** 2026-05-30 (v1.0 shipped 2026-05-29, tag `v1.0`)

## Where Things Stand

- **Milestone defined:** YES (`PROJECT.md`, `REQUIREMENTS.md` updated for v1.1)
- **Research complete:** YES (`.planning/research/` — STACK, FEATURES, ARCHITECTURE, PITFALLS, SUMMARY; HIGH confidence)
- **Roadmap created:** YES (Phases 7–10 appended to `ROADMAP.md`)
- **Current phase:** 08
- **Plans created:** 2
- **Plans executed:** 2 (07.1 — deprecation sweep; 07.2 — Gradle-9 readiness ADR + fold-forward)

## Roadmap Snapshot (v1.1)

| # | Phase | Status | Plans | Requirements |
|---|-------|--------|-------|--------------|
| 7 | **GRADLE-9** — Core Version Bump + Deprecation Sweep | Complete | 2/2 | AGP9-01 |
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

## Blockers (open)

- **B-08-01 (Phase 08 HALT — Rule 4, policy decision required)** — `androidx.baselineprofile` Gradle plugin `1.4.1` (the newest **stable** release, currently pinned via `baselineProfilePlugin = "1.4.1"`) **hard-rejects AGP 9**: applying it to `:app` throws `Module :app is not a supported android module` (`androidx.baselineprofile.gradle.utils.AgpPlugin.configureWithAndroidPlugin`, AgpPlugin.kt:202). The ONLY AGP-9-compatible release is `1.5.0-alpha06` (pre-release). Adopting it violates the ADR-0001 / global **stable-only** policy. This plugin is applied at `:app` AND is the entire `:baselineprofile` module's reason to exist, so it blocks whole-build configuration → blocks the Task-5 `:core:common` smoke and the Task-10 full gate. It is **out of plan 08.1's declared scope** (the 7-commit set never touched the baselineprofile plugin version). **Decision needed (planner/user):** (a) accept `1.5.0-alpha06` as a scoped exception to stable-only for the macrobenchmark toolchain; (b) temporarily remove/disable the `androidx.baselineprofile` plugin + `:baselineprofile` module for this milestone and re-add when a stable AGP-9 release ships; or (c) defer the AGP-9 landing until a stable baselineprofile plugin supports AGP 9. **Last-good commit: `fcd1499`.** Everything else in the migration (Gradle 9.4.1, AGP 9.2.1, built-in Kotlin, bare CommonExtension + AGP-9 DSL forms, Hilt 2.59.2) is landed and bisectable.

## Next Step

**Phase 08 plan 08.1 is HALTED at the Task-5 smoke** — resolve blocker B-08-01 (baselineprofile-plugin × AGP-9 stable-only conflict) before resuming. Commits `888c65a`→`fcd1499` are in place and clean. Once the policy decision is made, resume from the choke-point smoke (`./gradlew --stop && ./gradlew :core:common:compileDebugKotlin`), then continue with the remaining commits (compileSdk 36, CI cache-key agp8→agp9, crutch-flag guard) and the Task-10 full gate.

## Decisions (accumulated)

- **07.1** — Wrapper stays on `gradle-8.11.1-bin.zip`; the 9.4.1 flip folds forward to Phase 8 commit 1 (AGP 8.7.3 hard-fails on Gradle 9).
- **07.1** — detekt 1.23.8 retained; its Gradle-9 warn-vs-fail behavior is untestable until AGP 9 (Phase 8). Do NOT adopt the 2.0.0-alpha line.
- **07.1** — Repo-authored build scripts independently confirmed Gradle-9-clean (only `layout.buildDirectory`; no `buildDir`/`Project.exec`/`getConvention`/`JavaPluginConvention`/`archivesBaseName`).
- **07.1** — `gradle9-deprecations.log` force-added past the global `*.log` gitignore (scoped single-path `-f`; `.gitignore` untouched).
- **07.2** — Gradle 9.4.1 target pinned in ADR 0001 with sha256 fetched-at-Phase-8-execution (never hard-coded); live wrapper stays 8.11.1.
- **07.2** — detekt 1.23.8 kept (stable); accept/track Gradle-9 warnings, defer empirical run-vs-fail test to Phase 8, reject 2.0.0-alpha (stable-only policy).
- **07.2** — KGP 2.2.20 / KSP 2.2.20-2.0.4 confirmed ≥ AGP-9's 2.2.10 floor — no Kotlin/KSP bump in Phase 8.
- **07.2** — AGP 8.7.3 hard-fails on Gradle 9 → wrapper flip folds forward to Phase 8 commit 1; AGP9-01 PARTIALLY delivered (green-on-Gradle-9 asserted at the Phase-8 gate). `docs/adr/` ADR convention established.

---
*Last updated: 2026-05-30 — plan 07.2 executed (Gradle-9 readiness ADR + fold-forward); Phase 7 COMPLETE at 2/2.*
