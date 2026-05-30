---
phase: 08-agp-9-atomic-build-logic-migration-compilesdk-36
plan: 08.1
subsystem: build-toolchain
status: HALTED (partial — B-08-02 resolution applied but EMPIRICALLY INSUFFICIENT; new blocker B-08-03)
tags: [agp9, gradle9, kotlin, ksp, ksp2, hilt, compilesdk, build-logic, halt]
requires:
  - Gradle 9.4.1 wrapper (Phase 7 fold-forward)
  - AGP 9.2.1 (Google Maven)
  - Hilt 2.59.2 (Maven Central)
  - baselineprofile 1.5.0-alpha06 (Google Maven, AR-08-01 scoped exception)
provides:
  - PARTIAL — AGP-9 critical-path commits 1–6 + Kotlin/KSP 2.2.21 bump (B-08-02 resolution) landed and bisectable
  - NOT provided — green choke-point smoke, compileSdk 36, CI agp9 cache key, full phase gate (all blocked by B-08-03)
affects:
  - all ~14 modules (via build-logic/convention choke point)
tech-stack:
  added: []
  patterns: [built-in-kotlin, version-catalog-pinning, bisectable-atomic-commits, ksp1-vs-ksp2]
key-files:
  created: []
  modified:
    - gradle/libs.versions.toml (kotlin 2.2.20 -> 2.2.21; ksp 2.2.20-2.0.4 -> 2.2.21-2.0.5)
decisions:
  - "AR-08-01: baselineprofile 1.5.0-alpha06 scoped stable-only exception (B-08-01 resolution)"
  - "B-08-02 RESOLUTION APPLIED: Kotlin/KSP -> 2.2.21 / 2.2.21-2.0.5 (commit 1b17a80)"
  - "B-08-03 SURFACED: the 2.2.21 bump is necessary but NOT sufficient — KSP on the 2.2.x line is still the KSP1 unified plugin, which rejects AGP-9 built-in Kotlin. Only KSP >= 2.3.0 (KSP2-only) clears it, and that needs Kotlin 2.3.x (D-04b hard ceiling). HALT for new policy decision."
metrics:
  duration: ~25m (this resume session)
  completed: 2026-05-31
---

# Phase 08 Plan 08.1: AGP-9 Atomic Build-Logic Migration Summary (PARTIAL — HALTED at B-08-03)

The user-approved **B-08-02 resolution (Kotlin 2.2.20→2.2.21 + KSP 2.2.20-2.0.4→2.2.21-2.0.5) was applied exactly as approved and committed** (`1b17a80`). Empirical verification then proved that resolution **necessary but insufficient**: the choke-point smoke `:core:common:compileDebugKotlin` still fails at `:app` configuration with the *identical* `KSP is not compatible with Android Gradle Plugin's built-in Kotlin` error, because **every KSP release on the Kotlin 2.2.x line (newest `2.2.21-2.0.5`) is still the unified plugin that runs the legacy KSP1 `AndroidPluginIntegration`, which categorically rejects AGP-9's built-in Kotlin**. The KSP-team's documented opt-in (`ksp.useKSP2=true`) was tried and does NOT bypass it — the rejection fires during *plugin apply* (`KspSubplugin.kt:678` → `AndroidPluginIntegration.kt:84`), upstream of useKSP2 task routing. The first KSP that is genuinely KSP2-only (no KSP1 path, AGP-9-compatible) is **KSP ≥ 2.3.0**, whose new bare-version scheme is locked to **Kotlin 2.3.x** — which is the explicit **D-04b hard ceiling** ("Still NO 2.3.x"). Execution HALTED for a new planner/user policy decision; no auto-fix is possible without crossing D-04b, D-09, or D-06.

## What Was Done This Session

| Step | Task | Result | Commit |
|------|------|--------|--------|
| Task 1 | Kotlin 2.2.20→2.2.21 + KSP 2.2.20-2.0.4→2.2.21-2.0.5 (B-08-02 resolution) | DONE | `1b17a80` |
| Task 5 | Choke-point smoke `:core:common:compileDebugKotlin` | **FAILED → B-08-03** (identical KSP×built-in-Kotlin error persists on 2.2.21) | — |
| (diag) | `ksp.useKSP2=true` opt-in (Rule-3 attempt) | DID NOT clear the apply-time rejection; reverted (uncommitted) | — |
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
- `631b2b0` build(baselineprofile): adopt 1.5.0-alpha06 for AGP-9 (AR-08-01 scoped exception)

This session:
- `1b17a80` build(kotlin): bump Kotlin 2.2.20→2.2.21 + KSP →2.2.21-2.0.5 for AGP-9 built-in Kotlin (B-08-02)  ← **last-good**

## Resolved Versions

| Component | Version | Source |
|-----------|---------|--------|
| Gradle wrapper | 9.4.1 (validateDistributionUrl=true) | services.gradle.org |
| AGP | 9.2.1 | Google Maven |
| Hilt/Dagger | 2.59.2 (exact) | Maven Central |
| Kotlin (KGP) | **2.2.21** (bumped this session, B-08-02) | — |
| KSP | **2.2.21-2.0.5** (bumped this session — but still KSP1 unified plugin → the B-08-03 blocker) | — |
| baselineprofile plugin | 1.5.0-alpha06 (AR-08-01) | Google Maven |
| compileSdk | 35 (NOT YET raised — Task 8 blocked) | — |

## HALT — Blocker B-08-03 (NEW — supersedes the now-applied B-08-02)

**Exact error (`:core:common:compileDebugKotlin --stacktrace`, on Kotlin 2.2.21 + KSP 2.2.21-2.0.5):**
```
* What went wrong:
A problem occurred configuring project ':app'.
> KSP is not compatible with Android Gradle Plugin's built-in Kotlin. Please disable by adding android.builtInKotlin=false to gradle.properties and apply kotlin("android") plugin

Caused by: ... at com.google.devtools.ksp.gradle.AndroidPluginIntegration.tryUpdateKspWithAndroidSourceSets(AndroidPluginIntegration.kt:84)
           at com.google.devtools.ksp.gradle.AndroidPluginIntegration.syncSourceSets(AndroidPluginIntegration.kt:202)
           at com.google.devtools.ksp.gradle.KspGradleSubplugin.applyToCompilation(KspSubplugin.kt:678)
```

**Root cause (empirically established, not predicted):**
1. The B-08-02 fix bumped to KSP `2.2.21-2.0.5` — verified resolved on the classpath (jar present in `~/.gradle/caches`). The error is **identical** to the pre-bump 2.2.20-2.0.4 error.
2. KSP's versioning scheme `<Kotlin>-<KSP>` (the `-2.0.x` suffix) is the **unified plugin** that still contains and applies the legacy **KSP1** (K1) `KspGradleSubplugin` / `AndroidPluginIntegration`. That integration calls `tryUpdateKspWithAndroidSourceSets` at **plugin-apply time**, and AGP 9's built-in Kotlin makes it throw outright.
3. The KSP-team's documented KSP2 opt-in `ksp.useKSP2=true` (gradle.properties) was applied and **did not clear the error** — the rejection fires in the *apply* path, before useKSP2 routes the processing task. Reverted (left uncommitted; no value committing a non-working flag).
4. Maven Central metadata for `com.google.devtools.ksp.gradle.plugin`: the 2.2.21 line tops out at **`2.2.21-2.0.5`** (still unified/KSP1). The next published versions are **`2.3.0` … `2.3.9`** — a **new bare scheme that is KSP2-only and AGP-9-compatible**, but **bound to Kotlin 2.3.x**.
5. KSP docs (Context7 `/google/ksp`) confirm: *"KSP1 will not support Kotlin 2.3.0 and higher, and Android Gradle Plugin 9.0 and higher."* AGP-9 built-in Kotlin therefore requires a **KSP2-only** plugin = **KSP ≥ 2.3.0** = **Kotlin 2.3.x**.

**Why this is a locked-policy HALT, not a Rule 1–3 auto-fix:**
The only paths forward each cross a NON-NEGOTIABLE constraint:
- **Kotlin 2.3.x + KSP ≥ 2.3.0** (the real fix) → violates **D-04b** hard ceiling ("Still NO 2.3.x"). This is the decision the user must make: lift the 2.3.x ceiling.
- `android.builtInKotlin=false` + re-apply `kotlin("android")` (the error's own suggestion) → violates **D-09** (forbidden crutch flag; Task 9 asserts count==0) **and** **D-06/Task-3** (reverses built-in Kotlin, re-triggers "extension 'kotlin' already registered").
- `ksp.useKSP2=true` alone → **does not work** (proven this session).
- Drop Hilt + Apollo KSP processors → architectural (Rule 4), defeats the app's DI graph.

**Why the B-08-02 decision was insufficient:** The user-approved B-08-02 assumed bumping to Kotlin/KSP 2.2.21 (same minor) would clear the collision because 2.2.21-2.0.5 was "the patch line KSP supports for built-in Kotlin." Empirically, **no KSP on any 2.2.x patch line supports AGP-9 built-in Kotlin** — they are all the unified KSP1 plugin. Built-in-Kotlin support is a property of **KSP2-only (≥ 2.3.0)**, which the 2.2.x→2.2.21 bump cannot reach. The minimal-patch assumption was off by a minor version of Kotlin.

**Last-good commit: `1b17a80`.** Everything landed (Gradle 9.4.1, AGP 9.2.1, built-in Kotlin, bare CommonExtension + AGP-9 DSL forms, Hilt 2.59.2, baselineprofile 1.5.0-alpha06, Kotlin/KSP 2.2.21) is bisectable and correct; the build configures cleanly up to the KSP1/built-in-Kotlin collision. Nothing needs reverting.

### Decision needed (planner/user) — see STATE.md B-08-03

- **(a) RECOMMENDED — lift the D-04b 2.3.x ceiling for the toolchain quartet only.** Adopt **Kotlin 2.3.x + KSP 2.3.x (bare, KSP2-only)**. This is the path the KSP/AGP-9 teams explicitly support; it is the *only* way to keep AGP-9 built-in Kotlin without a crutch. Requires re-verifying Hilt 2.59.2, the Compose compiler plugin (auto-tracks via `version.ref="kotlin"` → will move to 2.3.x), Apollo 5.0.0 KSP codegen, and detekt 1.23.8 (whose 2.1.0 metadata ceiling becomes a HARDER risk on Kotlin 2.3.x — see detekt note below) all on 2.3.x. Higher churn than the rejected 2.2.21 patch, but it is the in-spirit AGP-9 landing.
- **(b)** Accept `android.builtInKotlin=false` + re-apply `kotlin("android")` as a scoped D-09/D-06 exception (defeats AGP9-02's built-in-Kotlin goal; keeps Kotlin 2.2.x and KSP1). Not recommended — it abandons the phase's core deliverable.
- **(c)** Defer the AGP-9 landing until a KSP1 patch ships built-in-Kotlin support on the 2.2.x line (per KSP's own deprecation notice, this will **never** happen — KSP1 is frozen for AGP 9 / Kotlin 2.3+).

## Deviations from Plan

1. **[AR-08-01 — accepted-risk exception]** baselineprofile plugin 1.4.1 → 1.5.0-alpha06 (B-08-01 resolution). Scoped stable-only exception (build-time-only tooling; never ships in APK). Commit `631b2b0`.
2. **[B-08-02 resolution applied]** Kotlin 2.2.20→2.2.21 + KSP 2.2.20-2.0.4→2.2.21-2.0.5 per the user-approved decision. Commit `1b17a80`. **It was necessary but proved insufficient (→ B-08-03).**
3. **[CommonExtension fix larger than RESEARCH predicted]** (pre-session, `f5cadbf`) — AGP 9.2.1 removed the `Action<T>` lambda-block overloads from `CommonExtension`; the choke point uses property-access + `.apply{}` (`defaultConfig.minSdk = …`, `compileOptions.apply{…}`, `lint.apply{…}`), not a one-token change. Also dropped the now-invalid `targetSdk` from the library DSL (`LibraryBaseFlavor`).
4. **[AGP 9 removed `targetSdk` from the library DSL]** — the targetSdk guard is now **2 sites** (`AndroidApplicationConventionPlugin` + `baselineprofile`), not 3; libraries have no runtime targetSdk. (Pre-staged for Task 9, which never ran.)
5. **[Hilt bump reordered ahead of compileSdk]** (pre-session) to make the build configurable for the smoke.
6. **[NEW — B-08-03 HALT]** The Kotlin/KSP 2.2.21 bump does NOT clear the AGP-9 × KSP collision; the real fix is KSP2-only (≥ 2.3.0) requiring Kotlin 2.3.x, which D-04b forbids. `ksp.useKSP2=true` was tried and does not bypass the apply-time rejection. Forced HALT for a new policy decision; no auto-fix crosses fewer than one locked constraint.

## detekt 1.23.8 Empirical Question — STILL UNRESOLVED (and now riskier)

The ADR's deferred empirical test (does detekt 1.23.8 pass on Gradle 9 + AGP 9?) **still could not be answered** — the build fails at `:app` configuration (B-08-03) before any detekt task runs. **Forward note for decision (a):** if Kotlin moves to 2.3.x, detekt 1.23.8's known hard failure (issue #8865 — bundled compiler reads metadata ≤ 2.1.0, fails on **Kotlin 2.3.0 metadata**) becomes a *direct* hit rather than the previously-bounded parse-only escape. The §Pitfall-6 fallback ladder (NOT 2.0.0-alpha) would likely need to be exercised, or detekt isolated from the compile gate. This raises the cost of decision (a) and should be weighed by the planner.

## TDD Gate Compliance

N/A — build-toolchain migration plan (no `tdd="true"` tasks; no application behavior changed).

## Self-Check: PASSED

- `1b17a80` exists in git log: FOUND.
- `gradle/libs.versions.toml` contains `kotlin = "2.2.21"` and `ksp = "2.2.21-2.0.5"`: FOUND.
- B-08-03 smoke failure reproduced deterministically (with and without config cache, and with `ksp.useKSP2=true`); exact `* What went wrong` header + `KspSubplugin.kt:678 → AndroidPluginIntegration.kt:84` stacktrace captured: CONFIRMED.
- Maven Central metadata confirming 2.2.21 line tops at `2.2.21-2.0.5` and the next KSP is bare `2.3.0+`: CONFIRMED.
- Speculative `ksp.useKSP2=true` reverted; working tree clean apart from this SUMMARY + STATE: CONFIRMED.
- STATE.md B-08-03 recorded + Next Step updated: FOUND.
