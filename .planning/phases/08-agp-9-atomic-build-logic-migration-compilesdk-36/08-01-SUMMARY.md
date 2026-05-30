---
phase: 08-agp-9-atomic-build-logic-migration-compilesdk-36
plan: 08.1
subsystem: build-toolchain
status: COMPLETE (full phase gate GREEN)
tags: [agp9, gradle9, kotlin, ksp, ksp2, built-in-kotlin, hilt, apollo, detekt, compilesdk, build-logic]
requires:
  - Gradle 9.4.1 wrapper (Phase 7 fold-forward)
  - AGP 9.2.1 (Google Maven)
  - Kotlin 2.3.20 + KSP 2.3.9 (KSP2, bare scheme — B-08-04 option a)
  - Hilt/Dagger 2.59.2 (Maven Central, KSP2-host compatible)
  - Apollo 5.0.0 (Gratatouille codegen, off the KSP axis)
  - baselineprofile 1.5.0-alpha06 (AR-08-01 scoped exception)
  - detekt 2.0.0-alpha.3 / dev.detekt (AR-08-02 scoped exception)
provides:
  - AGP-9 built-in Kotlin landed green across all ~14 modules on Gradle 9.4.1
  - compileSdk 36 (targetSdk held explicit at 35)
  - Full phase gate GREEN — compileDebugSources + detekt + ktlintCheck + test
  - Hilt 2.59.2 DI codegen empirically confirmed under the KSP2 2.3.9 host
  - CI Gradle cache key bumped agp8 -> agp9
affects:
  - all ~14 modules (via build-logic/convention choke point)
tech-stack:
  added:
    - "Kotlin 2.3.20 (was 2.2.20)"
    - "KSP 2.3.9 KSP2 bare scheme (was 2.2.20-2.0.4)"
    - "detekt 2.0.0-alpha.3 dev.detekt (was io.gitlab.arturbosch.detekt 1.23.8)"
  patterns: [built-in-kotlin, ksp2, version-catalog-pinning, bisectable-atomic-commits, detekt-2.0-schema, baseline-absorbed-findings]
key-files:
  created: []
  modified:
    - gradle/libs.versions.toml (kotlin 2.3.20; ksp 2.3.9; detekt 2.0.0-alpha.3 + plugin id dev.detekt)
    - gradle.properties (android.builtInKotlin=false removed)
    - build.gradle.kts (detekt plugin id + DetektExtension import -> dev.detekt)
    - config/detekt/detekt.yml (complexity keys migrated to detekt 2.0 allowed* schema)
    - "12 per-module detekt-baseline.xml (regenerated for detekt 2.0 ID format)"
    - build-logic/convention/.../KotlinAndroid.kt (bare CommonExtension, compileSdk 36)
    - baselineprofile/build.gradle.kts (kotlin{compilerOptions}, compileSdk 36)
    - .github/workflows/ci.yml (cache key agp8 -> agp9)
decisions:
  - "B-08-04 option (a): lift D-04b 2.3.x ceiling — adopt Kotlin 2.3.20 + KSP 2.3.9 + AGP-9 built-in Kotlin (the ONLY green path)"
  - "AR-08-01: baselineprofile 1.5.0-alpha06 scoped build-time-only exception"
  - "AR-08-02: detekt 2.0.0-alpha.3 (dev.detekt) scoped build-time-only exception — only 2.3.x-capable detekt"
metrics:
  duration: ~12m (this resume/landing session)
  completed: 2026-05-31
---

# Phase 08 Plan 08.1: AGP-9 Atomic Build-Logic Migration Summary

**AGP-9 lands green.** The full phase gate `./gradlew compileDebugSources detekt ktlintCheck test` is **BUILD SUCCESSFUL** across all ~14 modules on **Gradle 9.4.1 + AGP 9.2.1 + Kotlin 2.3.20 + KSP 2.3.9 (KSP2) + AGP-9 built-in Kotlin + Hilt 2.59.2 + Apollo 5.0.0 + compileSdk 36 (targetSdk 35) + detekt 2.0.0-alpha.3 (`dev.detekt`)**, with all version-isolation opt-out flags (`enableLegacyVariantApi`, `newDsl`, `builtInKotlin`) ABSENT. This resolves the entire B-08-01 → B-08-02 → B-08-03 → B-08-04 blocker saga: the Kotlin-2.2.x line is empirically dead under AGP 9, and the research-verified Kotlin-2.3.20/KSP2 set is the sole coherent green path (08-RESEARCH-2.3.x.md, VIABLE).

## Final Resolved Versions

| Component | Version | Source / Note |
|-----------|---------|---------------|
| Gradle wrapper | **9.4.1** (validateDistributionUrl=true) | sha256 `2ab2958f2a1e51120c326cad6f385153bb11ee93b3c216c5fccebfdfbb7ec6cb` (live-fetched) |
| AGP | **9.2.1** | Google Maven |
| Kotlin (KGP) | **2.3.20** | KSP-matched (not 2.3.21 — no KSP for it) |
| KSP | **2.3.9** | bare KSP2 scheme; id unchanged `com.google.devtools.ksp` |
| compose-compiler / serialization plugins | **2.3.20** | auto-track `version.ref="kotlin"` |
| Hilt / Dagger | **2.59.2** (exact, never `2.59+`) | Maven Central; KSP2-host codegen CONFIRMED |
| Apollo Kotlin | **5.0.0** | Gratatouille codegen (off the KSP axis) |
| ktlint plugin | **14.2.0** (engine 1.6.0) | unchanged |
| detekt | **2.0.0-alpha.3** (`dev.detekt`) | AR-08-02 scoped exception |
| baselineprofile plugin | **1.5.0-alpha06** | AR-08-01 scoped exception |
| compileSdk | **36** (targetSdk held at **35**) | KotlinAndroid.kt + baselineprofile |

## Gate Results

| Gate | Command | Result |
|------|---------|--------|
| Choke-point smoke (Task 5) | `:core:common:compileDebugKotlin` | **BUILD SUCCESSFUL** (Gradle 9.4.1) |
| Hilt/KSP2 early check (A1) | `:feature:connection:kspDebugKotlin` | **BUILD SUCCESSFUL** — Dagger 2.59.2 codegen runs under KSP2 2.3.9 |
| Full phase gate (Task 10) | `compileDebugSources detekt ktlintCheck test` | **BUILD SUCCESSFUL** — 497 actionable tasks |

**MEDIUM-risk assumption A1 (Dagger-under-KSP2) is now CONFIRMED PASS** — the one item research flagged as the only thing that could still block. Hilt is verified resolving to `com.google.dagger:hilt-android:2.59.2` on `debugRuntimeClasspath`.

## Commit Ledger (bisect provenance)

This landing session (newest last):

| Commit | Message |
|--------|---------|
| `baee519` | build(kotlin): re-adopt AGP-9 built-in Kotlin (reverse falsified KSP1 path, B-08-04) |
| `c796d32` | build(kotlin): bump Kotlin 2.2.20→2.3.20 + KSP →2.3.9 (KSP2, AGP-9 built-in Kotlin) [B-08-04] |
| `e1aa6b4` | build(detekt): migrate detekt 1.23.8 → 2.0.0-alpha.3 (dev.detekt) for Kotlin 2.3.x [AR-08-02] |
| `21251a2` | build(baselineprofile): migrate kotlinOptions → kotlin{compilerOptions} |
| `52afc8a` | build(sdk): compileSdk 35 → 36 (targetSdk stays 35) |
| `817ff8b` | ci(agp9): bump cache key agp8 → agp9; assert no AGP-9 crutch flags |
| `a3e6568` | build(detekt): migrate complexity config keys to detekt 2.0 schema (gate fix) |
| `7f6c0df` | build(detekt): regenerate baselines for detekt 2.0 ID format (gate fix) |

(Provenance: the falsified B-08-03 KSP1 commits `35748ff`+`cbf9689` were reversed by `baee519`; HEAD before this resume was `817ff8b` with all source-level tasks already committed but the build never driven to GREEN.)

## Full Saga: B-08-01 → B-08-04 (resolutions)

- **B-08-01 (AR-08-01):** `androidx.baselineprofile` stable 1.4.1 hard-rejects AGP 9 (`Module :app is not a supported android module`). Only `1.5.0-alpha06` is AGP-9-compatible → scoped build-time-only exception.
- **B-08-02 → B-08-03:** AGP-9 built-in Kotlin requires a **KSP2-only** plugin (KSP ≥ 2.3.0). Every KSP on the Kotlin-2.2.x line (incl. 2.2.21-2.0.5) is the unified KSP1 plugin whose `AndroidPluginIntegration` rejects built-in Kotlin at plugin-apply time. The KSP-team opt-in `ksp.useKSP2=true` does NOT bypass it (fires before task routing).
- **B-08-04:** The traditional `kotlin("android")` + KSP1 escape ALSO crashes — KGP 2.2.x unconditionally casts the AGP extension to the AGP-9-**removed** `BaseExtension` (`ApplicationExtensionImpl cannot be cast to BaseExtension`). The entire 2.2.x line is dead under AGP 9. **Resolution = option (a):** lift D-04b, adopt the research-verified Kotlin 2.3.20 + KSP 2.3.9 + built-in-Kotlin quartet. The earlier "no Kotlin/KSP bump" (D-04b) and "defer built-in Kotlin" (D-BIK) decisions were **voided by empirical reality** — their premise (2.2.x works under AGP 9) is false.

**AGP9-02 success-criterion #2 (adopt built-in Kotlin) is now FULLY MET** — not deferred. `org.jetbrains.kotlin.android` is gone from all 5 sites; `android.builtInKotlin=false` is removed.

## Two Alpha Exceptions (FLAG FOR VERIFIER)

Both are **scoped, build-time-only** exceptions to the global stable-only policy. Neither ships in the APK, so the policy's runtime-safety intent is preserved. They are the ONLY AGP-9/Kotlin-2.3.x-compatible builds of their respective tools.

1. **AR-08-01 — baselineprofile `1.5.0-alpha06`** (macrobenchmark toolchain; generates profiles at build time).
2. **AR-08-02 — detekt `2.0.0-alpha.3` (`dev.detekt`)** (static analysis; stable 1.23.8 hard-fails on Kotlin 2.3.x metadata #8865).

## Deviations from Plan

### Auto-fixed Issues (Rule 3 — blocking gate failures)

**1. [Rule 3 — Blocking] detekt 2.0 config-key migration**
- **Found during:** Task 10 (full gate, first run). detekt 2.0.0-alpha.3 rejected 7 complexity config keys at config-validation time (`InvalidConfig: 7 invalid config properties`).
- **Issue:** detekt 2.0 (changelog-2.0.0 "Breaking Changes") renamed every complexity `threshold` key to a descriptive `allowed*` key. The prior session's detekt migration bumped the plugin/version but left `config/detekt/detekt.yml` on the 1.x schema.
- **Fix:** Renamed in `config/detekt/detekt.yml`: `LongMethod.threshold→allowedLines`, `LongParameterList.functionThreshold/constructorThreshold→allowedFunctionParameters/allowedConstructorParameters`, `TooManyFunctions.thresholdInFiles/thresholdInClasses→allowedFunctionsPerFile/allowedFunctionsPerClass`, `CyclomaticComplexMethod.threshold→allowedComplexity`, `LargeClass.threshold→allowedLines`. Verified via detekt 2.0 docs (Context7) + the runtime error's "Allowed properties" lists.
- **Commit:** `a3e6568`

**2. [Rule 3 — Blocking] detekt 2.0 baseline regeneration**
- **Found during:** Task 10 (second run). After the config validated, detekt RAN and the gate failed with pre-existing findings (`IssuesFound`). detekt 2.0 changed the baseline issue-ID delimiter (`.kt$Class` → `.kt:Class`), so the existing per-module `detekt-baseline.xml` files no longer matched the **identical** already-accepted findings.
- **Issue:** 42 pre-existing code smells (TooGenericExceptionCaught, TooManyFunctions, UnusedParameter, MatchingDeclarationName) — all already absorbed by the 1.x baselines — re-surfaced because the baseline IDs were format-mismatched.
- **Fix:** Regenerated all 12 baselines via `./gradlew detektBaseline`. Diff is a pure ID-format migration — same files, same findings, only the delimiter changed. No new smells masked (this plan added zero source code).
- **Commit:** `7f6c0df`

### Accepted-Risk Exceptions

3. **[AR-08-01]** baselineprofile 1.4.1 → 1.5.0-alpha06 (B-08-01 resolution; build-time-only).
4. **[AR-08-02]** detekt 1.23.8 (`io.gitlab.arturbosch.detekt`) → 2.0.0-alpha.3 (`dev.detekt`); group + plugin id + extension import (`dev.detekt.gradle.extensions.DetektExtension`) + `toolVersion` all migrated. Build-time-only.

### Structural deviations carried from the migration

5. **[CommonExtension form larger than original RESEARCH predicted]** AGP 9.2.1 removed the `Action<T>` lambda-block overloads from `CommonExtension`; the choke point uses property-access + `.apply{}` forms (`defaultConfig.minSdk = …`, `compileOptions.apply{…}`, `lint.apply{…}`), plus bare (non-generic) `CommonExtension`.
6. **[AGP 9 removed `targetSdk` from the library DSL]** — the targetSdk guard is **2 sites** (`AndroidApplicationConventionPlugin` + `baselineprofile`), not 3; libraries resolve targetSdk from the consuming app. `targetSdk = 35` guard count is 3 in build-logic+baselineprofile (app + library convention-plugin call sites + baselineprofile) and `targetSdk = 36` count is 0 — no silent Android-16 opt-in.

### detekt empirical question — RESOLVED

The ADR's deferred question ("does detekt run on Gradle 9 + AGP 9?") is now answered: **stable detekt 1.23.8 cannot** (hard-fails on Kotlin 2.3.x metadata, #8865); **detekt 2.0.0-alpha.3 (`dev.detekt`) runs cleanly and GATES** (its decoupled 2.3.21 analyzer reads 2.3.20 metadata). detekt is **gating** (on the gate command), not the AR-08-02 non-gating fallback — the alpha behaved correctly.

## Final Grep Gate (all pass)

```
agp8 in ci.yml:                 0   (expect 0)
agp9 in ci.yml:                 2   (expect 2)
kotlin.android refs:            0   (expect 0)
forbidden flags:                0   (enableLegacyVariantApi/newDsl/builtInKotlin)
compileSdk = 36:                2   (expect 2)
targetSdk = 35:                 3   (expect 3)
targetSdk = 36:                 0   (expect 0)
agp=9.2.1 kotlin=2.3.20 ksp=2.3.9 hilt=2.59.2 apollo=5.0.0 detekt=2.0.0-alpha.3
wrapper: gradle-9.4.1-bin.zip
```

## Requirements Discharged

- **AGP9-02** — AGP-9 atomic build-logic migration; built-in Kotlin ADOPTED (criterion #2 fully met).
- **AGP9-03** — Hilt+KSP DI graph compiles (now on Kotlin 2.3.20 / KSP2 2.3.9; confirmed at `:feature:connection` + full gate).
- **SDK-01** — compileSdk 36, targetSdk held 35.
- **AGP9-01 (folded-forward clause)** — green on Gradle 9.4.1 asserted at the gate.

## TDD Gate Compliance

N/A — build-toolchain migration plan (no `tdd="true"` tasks; no application behavior changed).

## Self-Check: PASSED

- `08-01-SUMMARY.md` exists: FOUND.
- All 8 landing commits (`baee519`, `c796d32`, `e1aa6b4`, `21251a2`, `52afc8a`, `817ff8b`, `a3e6568`, `7f6c0df`) in git log: FOUND.
- Key modified files (`gradle/libs.versions.toml`, `config/detekt/detekt.yml`, `build.gradle.kts`, regenerated `detekt-baseline.xml`): FOUND.
- Full phase gate `compileDebugSources detekt ktlintCheck test` reproduced BUILD SUCCESSFUL (497 tasks): CONFIRMED.
- All grep guards pass (agp9×2, kotlin.android×0, forbidden flags×0, compileSdk 36×2, targetSdk 35×3, targetSdk 36×0): CONFIRMED.
