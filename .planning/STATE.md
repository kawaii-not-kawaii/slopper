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

- **Kotlin/KSP: minimal 2.2.21 patch bump (REVISED 2026-05-30, supersedes the original "no bump")** — the original "no Kotlin/KSP bump" assumed 2.2.20 was sufficient for AGP 9; B-08-02 proved KSP 2.2.20-2.0.4 is incompatible with AGP-9 **built-in Kotlin**. User-approved minimal fix: Kotlin 2.2.20→**2.2.21** + KSP 2.2.20-2.0.4→**2.2.21-2.0.5** (same minor, the patch line KSP supports for built-in Kotlin; AGP 9 needs only KGP ≥ 2.2.10). Still NO 2.3.x. compose-compiler plugin auto-tracks Kotlin via `version.ref`.
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

## Accepted Risks

- **AR-08-01 (resolves B-08-01) — baselineprofile `1.5.0-alpha06` scoped stable-only exception.** User decision 2026-05-30 (option a). The `androidx.baselineprofile` Gradle plugin's newest stable (`1.4.1`) hard-rejects AGP 9; the only AGP-9-compatible build is `1.5.0-alpha06`. Adopted as a **scoped, accepted-risk exception** to the global stable-only policy. **Justification:** it is build-time-only tooling (generates baseline profiles consumed at build time; it never ships in the APK), so the policy's runtime-safety intent is preserved; landing AGP-9 is the entire purpose of v1.1. **Scope:** limited to `baselineProfilePlugin` only — detekt `2.0.0-alpha` and all runtime libraries remain stable-only. **Exit:** revert to a stable `androidx.baselineprofile` once one supports AGP 9.

## Blockers (resolved)

- **B-08-02 (RESOLVED 2026-05-30 — user decision: option a, minimal 2.2.21 patch bump; see revised Kotlin/KSP locked decision above)** — KSP 2.2.20-2.0.4 × AGP-9 built-in Kotlin incompatibility. Resolution: bump Kotlin 2.2.20→2.2.21 + KSP 2.2.20-2.0.4→2.2.21-2.0.5 (both verified published on Maven Central), keeping AGP-9 built-in Kotlin (no `builtInKotlin=false` crutch, no `kotlin("android")` re-add). Re-verify Hilt 2.59.2 / Compose-compiler / Apollo on 2.2.21 during the gate. After B-08-01 was resolved (baselineprofile 1.5.0-alpha06, commit `631b2b0`), the `:core:common:compileDebugKotlin` choke-point smoke fails at **`:app` configuration** with: `KSP is not compatible with Android Gradle Plugin's built-in Kotlin. Please disable by adding android.builtInKotlin=false to gradle.properties and apply kotlin("android") plugin` (`com.google.devtools.ksp.gradle.AndroidPluginIntegration.tryUpdateKspWithAndroidSourceSets`, AndroidPluginIntegration.kt:84). It originates in any module applying the Hilt convention plugin (which applies `com.google.devtools.ksp` — AndroidHiltConventionPlugin.kt:11). **Both suggested fixes are LOCKED-FORBIDDEN:** (a) `android.builtInKotlin=false` is a D-09 forbidden crutch flag (the gate is only done with it ABSENT); (b) re-applying `kotlin("android")` reverses D-06/Task-3 and re-triggers "extension already registered". **The only compatible KSP** (`2.2.21-2.0.5`, also `2.2.21-2.0.4`) lives on the Kotlin **2.2.21** line — adopting it requires bumping Kotlin 2.2.20→2.2.21 AND KSP 2.2.20-2.0.4→2.2.21-2.0.5, which is **forbidden by D-04b** ("Do NOT bump Kotlin/KSP"). No KSP exists on the 2.2.20 line that supports built-in Kotlin (`2.2.20-2.0.4` is newest). This was NOT anticipated by 08-RESEARCH (Pattern 2 de-risked `KotlinAndroidProjectExtension` resolution, but never tested KSP-subplugin × built-in-Kotlin). **Last-good commit: `631b2b0`** (Gradle 9.4.1, AGP 9.2.1, built-in Kotlin, bare CommonExtension + AGP-9 DSL forms, Hilt 2.59.2, baselineprofile 1.5.0-alpha06 — all landed and bisectable; build configures cleanly up to the KSP/built-in-Kotlin collision). **Decision needed (planner/user):** (a) lift the D-04b lock and adopt the minimal Kotlin 2.2.21 + KSP 2.2.21-2.0.5 bump (smallest patch on the same minor; re-verify Hilt/Compose/Apollo compat); (b) accept the `android.builtInKotlin=false` crutch + re-apply `kotlin("android")` as a scoped exception to D-09/D-06 (defeats the built-in-Kotlin goal of AGP9-02 — not recommended); or (c) defer the AGP-9 landing until KSP ships built-in-Kotlin support on the 2.2.20 line (may never). Recommendation: **(a)** — it is the minimal, in-spirit fix (AGP-9 needs only KGP ≥ 2.2.10; 2.2.21 is a same-minor patch, lower churn than feared, and is the path the KSP team explicitly supports).

- **B-08-01 (RESOLVED via AR-08-01)** — `androidx.baselineprofile` Gradle plugin `1.4.1` (the newest **stable** release, currently pinned via `baselineProfilePlugin = "1.4.1"`) **hard-rejects AGP 9**: applying it to `:app` throws `Module :app is not a supported android module` (`androidx.baselineprofile.gradle.utils.AgpPlugin.configureWithAndroidPlugin`, AgpPlugin.kt:202). The ONLY AGP-9-compatible release is `1.5.0-alpha06` (pre-release). Adopting it violates the ADR-0001 / global **stable-only** policy. This plugin is applied at `:app` AND is the entire `:baselineprofile` module's reason to exist, so it blocks whole-build configuration → blocks the Task-5 `:core:common` smoke and the Task-10 full gate. It is **out of plan 08.1's declared scope** (the 7-commit set never touched the baselineprofile plugin version). **Decision needed (planner/user):** (a) accept `1.5.0-alpha06` as a scoped exception to stable-only for the macrobenchmark toolchain; (b) temporarily remove/disable the `androidx.baselineprofile` plugin + `:baselineprofile` module for this milestone and re-add when a stable AGP-9 release ships; or (c) defer the AGP-9 landing until a stable baselineprofile plugin supports AGP 9. **Last-good commit: `fcd1499`.** Everything else in the migration (Gradle 9.4.1, AGP 9.2.1, built-in Kotlin, bare CommonExtension + AGP-9 DSL forms, Hilt 2.59.2) is landed and bisectable.

## Next Step

**Phase 08 plan 08.1 RESUMING — both blockers resolved by user decision (2026-05-30/31).** B-08-01 → baselineprofile 1.5.0-alpha06 (`631b2b0`, AR-08-01); B-08-02 → minimal Kotlin/KSP 2.2.21 bump (approved, supersedes the original no-bump decision). Last-good `631b2b0`. Resuming: bump Kotlin 2.2.20→2.2.21 + KSP 2.2.20-2.0.4→2.2.21-2.0.5, re-run the `:core:common:compileDebugKotlin` smoke, then Tasks 6 (kotlinOptions→compilerOptions), 8 (compileSdk 36), 9 (CI cache-key agp8→agp9 + targetSdk×2/no-crutch/compileSdk guards), 10 (full gate, incl. empirical detekt-1.23.8 + Hilt/Compose/Apollo on 2.2.21). **Active goal: fix all Dependabot debt properly — landing this toolchain migration unblocks the deferred AGP/Gradle/Kotlin/KSP/Hilt/benchmark/compileSdk deps in `.github/dependabot.yml`'s ignore list; Phase 9 then unblocks Media3/nextlib/activity/core; the obsolete ignore entries are pruned afterward.**

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
