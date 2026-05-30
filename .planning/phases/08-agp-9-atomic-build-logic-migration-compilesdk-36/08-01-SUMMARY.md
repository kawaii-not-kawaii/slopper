---
phase: 08-agp-9-atomic-build-logic-migration-compilesdk-36
plan: 08.1
subsystem: build-toolchain
status: HALTED (partial — blocked at Task-5 smoke by B-08-02)
tags: [agp9, gradle9, kotlin, ksp, hilt, compilesdk, build-logic, halt]
requires:
  - Gradle 9.4.1 wrapper (Phase 7 fold-forward)
  - AGP 9.2.1 (Google Maven)
  - Hilt 2.59.2 (Maven Central)
  - baselineprofile 1.5.0-alpha06 (Google Maven, AR-08-01 scoped exception)
provides:
  - PARTIAL — AGP-9 critical-path commits 1–6 landed (Gradle 9.4.1, AGP 9.2.1, built-in Kotlin, bare CommonExtension + AGP-9 DSL forms, Hilt 2.59.2, baselineprofile alpha)
  - NOT provided — green choke-point smoke, compileSdk 36, CI agp9 cache key, full phase gate (all blocked by B-08-02)
affects:
  - all ~14 modules (via build-logic/convention choke point)
tech-stack:
  added: []
  patterns: [built-in-kotlin, version-catalog-pinning, bisectable-atomic-commits]
key-files:
  created: []
  modified:
    - gradle/libs.versions.toml (baselineProfilePlugin 1.4.1 -> 1.5.0-alpha06)
decisions:
  - "AR-08-01: baselineprofile 1.5.0-alpha06 scoped stable-only exception"
  - "B-08-02 surfaced: KSP 2.2.20-2.0.4 incompatible with AGP-9 built-in Kotlin; HALT"
metrics:
  duration: ~30m (this resume session)
  completed: 2026-05-30
---

# Phase 08 Plan 08.1: AGP-9 Atomic Build-Logic Migration Summary (PARTIAL — HALTED)

AGP-9 critical-path migration (Gradle 9.4.1 / AGP 9.2.1 / built-in Kotlin / bare CommonExtension / Hilt 2.59.2 / baselineprofile 1.5.0-alpha06) is fully landed and bisectable, but the first-green choke-point smoke is **blocked at `:app` configuration by a previously-unmodeled blocker (B-08-02): KSP 2.2.20-2.0.4 is incompatible with AGP-9's built-in Kotlin**, and every available fix crosses a NON-NEGOTIABLE locked constraint (D-04b no-Kotlin/KSP-bump, or D-09 no-crutch-flag). Execution HALTED for a planner/user policy decision per the plan's locked-constraints contract.

## What Was Done This Session

| Step | Task | Result | Commit |
|------|------|--------|--------|
| AR-08-01 | baselineProfilePlugin 1.4.1 → 1.5.0-alpha06 | DONE (resolves B-08-01) | `631b2b0` |
| Task 5 | Choke-point smoke `:core:common:compileDebugKotlin` | **FAILED → B-08-02** | — |
| Task 6 | baselineprofile kotlinOptions→compilerOptions | NOT STARTED (blocked) | — |
| Task 8 | compileSdk 35→36 ×2 | NOT STARTED (blocked) | — |
| Task 9 | CI agp9 cache key + guards | NOT STARTED (blocked) | — |
| Task 10 | Full phase gate | NOT STARTED (blocked) | — |

## Full Commit Ledger (bisect provenance)

Pre-existing (landed before this session):
- `888c65a` chore(gradle): flip wrapper 8.11.1 → 9.4.1 (live sha256, validateDistributionUrl kept)
- `f2d11d5` build(agp): bump AGP 8.7.3 → 9.2.1
- `fa25942` build(kotlin): drop org.jetbrains.kotlin.android (AGP-9 built-in Kotlin), 5 sites
- `8cd177f` build(agp): drop CommonExtension generics (AGP-9 removed type params)
- `f5cadbf` build(agp): drop CommonExtension generics + AGP-9 DSL form fixes
- `fcd1499` build(hilt): bump Hilt/Dagger 2.56.2 → 2.59.2 (AGP-9 required)

This session:
- `631b2b0` build(baselineprofile): adopt 1.5.0-alpha06 for AGP-9 (AR-08-01 scoped stable-only exception)  ← **last-good**

## Resolved Versions

| Component | Version | Source |
|-----------|---------|--------|
| Gradle wrapper | 9.4.1 (live sha256 `2ab2958f…7ec6cb`, validateDistributionUrl=true) | services.gradle.org |
| AGP | 9.2.1 | Google Maven |
| Hilt/Dagger | 2.59.2 (exact, no range) | Maven Central |
| Kotlin (KGP) | 2.2.20 (UNCHANGED — D-04b lock) | — |
| KSP | 2.2.20-2.0.4 (UNCHANGED — D-04b lock) — **the blocker** | — |
| baselineprofile plugin | 1.5.0-alpha06 (AR-08-01 scoped exception) | Google Maven |
| compileSdk | 35 (NOT YET raised — Task 8 blocked) | — |

## HALT — Blocker B-08-02 (NEW, distinct from B-08-01)

**Exact error (`:core:common:compileDebugKotlin --stacktrace`):**
```
* What went wrong:
A problem occurred configuring project ':app'.
> KSP is not compatible with Android Gradle Plugin's built-in Kotlin.
  Please disable by adding android.builtInKotlin=false to gradle.properties and apply kotlin("android") plugin

Caused by: java.lang.RuntimeException: KSP is not compatible with Android Gradle Plugin's built-in Kotlin...
    at com.google.devtools.ksp.gradle.AndroidPluginIntegration.tryUpdateKspWithAndroidSourceSets(AndroidPluginIntegration.kt:84)
```

**Origin:** any module applying the Hilt convention plugin, which applies `com.google.devtools.ksp` (`AndroidHiltConventionPlugin.kt:11`). `:app` is the first to configure.

**Why this is a locked-policy HALT, not a Rule 1–3 auto-fix:**
The error's two suggested fixes are both explicitly forbidden by the plan's NON-NEGOTIABLE constraints:
1. `android.builtInKotlin=false` → **D-09 forbidden crutch flag** (the gate is only "done" with `builtInKotlin`/`newDsl`/`enableLegacyVariantApi` ABSENT; Task 9 even asserts count==0).
2. re-apply `kotlin("android")` → reverses **D-06 / Task-3** (the whole point of built-in Kotlin for AGP9-02) and re-triggers the "extension 'kotlin' already registered" collision.

The only legitimate fix — a KSP that supports AGP-9 built-in Kotlin — does not exist on the Kotlin 2.2.20 line (`2.2.20-2.0.4` is the newest; verified via Maven Central metadata). The earliest compatible KSP is **`2.2.21-2.0.5`** (also `2.2.21-2.0.4`) on the Kotlin **2.2.21** line — adopting it requires bumping Kotlin 2.2.20→2.2.21 AND KSP 2.2.20-2.0.4→2.2.21-2.0.5, which is **forbidden by D-04b** ("Do NOT bump Kotlin (2.2.20) or KSP").

**Why RESEARCH missed it:** 08-RESEARCH Pattern 2 de-risked that `KotlinAndroidProjectExtension` still resolves under built-in Kotlin (it does), and Now-in-Android proved the `configureKotlinAndroid` choke point. But NiA's verified config did not exercise the **KSP subplugin × built-in-Kotlin** path on KSP 2.2.20-2.0.4; the KSP/AGP-9 built-in-Kotlin handshake landed in a newer KSP patch. This is the next-layer empirical unknown beyond the detekt one the ADR already flagged.

**Decision needed (planner/user) — see STATE.md B-08-02:**
- **(a) RECOMMENDED** — lift D-04b for the minimal **Kotlin 2.2.21 + KSP 2.2.21-2.0.5** patch bump (same minor, low churn; AGP-9 needs only KGP ≥ 2.2.10; re-verify Hilt 2.59.2 / Compose-compiler / Apollo compat on 2.2.21).
- (b) accept `android.builtInKotlin=false` + re-apply `kotlin("android")` as a scoped D-09/D-06 exception (defeats AGP9-02's built-in-Kotlin goal — not recommended).
- (c) defer the AGP-9 landing until KSP ships built-in-Kotlin support on the 2.2.20 line (may never).

**Last-good commit: `631b2b0`.** The migration configures cleanly up to the KSP/built-in-Kotlin collision; nothing landed is broken or needs reverting.

## Deviations from Plan

1. **[AR-08-01 — accepted-risk exception]** baselineprofile plugin 1.4.1 → 1.5.0-alpha06, the B-08-01 resolution. Scoped stable-only exception (build-time-only tooling, never ships in APK). detekt stays 1.23.8; all runtime libs stay stable. Commit `631b2b0`. (Confirmed published on Google Maven before pinning.)
2. **[CommonExtension fix larger than RESEARCH predicted]** (landed pre-session, `f5cadbf`) — AGP 9.2.1 removed the `Action<T>` lambda-block overloads from `CommonExtension`, so the choke point uses property-access + `.apply{}` (`defaultConfig.minSdk = …`, `compileOptions.apply{…}`, `lint.apply{…}`), not the one-token change RESEARCH Pattern 1 implied. Also dropped the now-invalid `targetSdk` from the library DSL (`LibraryBaseFlavor` — AGP 9 removed it).
3. **[AGP 9 removed `targetSdk` from the library DSL]** — the D-05a "targetSdk=35 in 3 sites" guard is now **2 sites** (`AndroidApplicationConventionPlugin` + `baselineprofile`); the library convention plugin legitimately no longer sets targetSdk (libraries have no runtime targetSdk). Security intent T-08-03 preserved: the Android-16 opt-in is governed only by app+test, both explicit at 35. Task 9's guard must therefore assert 2 sites, not 3.
4. **[Hilt bump reordered ahead of compileSdk]** (landed pre-session) to make the build configurable for the smoke.
5. **[NEW — B-08-02 HALT]** KSP 2.2.20-2.0.4 × AGP-9 built-in Kotlin incompatibility (above). Forced HALT; no auto-fix possible without crossing D-04b or D-09.

## detekt 1.23.8 Empirical Question — STILL UNRESOLVED

The ADR's deferred empirical test (does detekt 1.23.8 pass on Gradle 9 + AGP 9?) **could not be answered** — the build fails at configuration (B-08-02) before any detekt task runs. The detekt verdict moves to the next session, after B-08-02 is resolved.

## TDD Gate Compliance

N/A — this is a build-toolchain migration plan (no `tdd="true"` tasks; no application behavior changed).

## Self-Check: PASSED

- `631b2b0` exists in git log: FOUND.
- `gradle/libs.versions.toml` contains `baselineProfilePlugin = "1.5.0-alpha06"`: FOUND (×1); `profileinstaller = "1.4.1"` untouched: FOUND.
- Smoke failure reproduced deterministically with exact `* What went wrong` header captured: CONFIRMED.
- STATE.md B-08-02 recorded + Next Step updated: FOUND.
