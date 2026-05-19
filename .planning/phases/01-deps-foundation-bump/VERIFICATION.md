---
phase: 01-deps-foundation-bump
verified: 2026-05-16T21:47:00+09:00
status: passed
verdict: PASS-WITH-NOTES
score: 12/17 LANDED, 5/17 DEFERRED-WITH-HYGIENE
overrides_applied: 0
re_verification: false
---

# Phase 1: DEPS — Foundation Bump Verification Report

**Phase Goal (re-scoped, Option A unblock from 01.1-SUMMARY.md):** Modernization foundation on the AGP 8.7.3 / Gradle 8.11.1 / compileSdk 35 floor — bump everything that *can* move under that floor (lint-baseline, JDK pin, Kotlin 2.2.20, Hilt 2.56.2, Apollo 4.4.3, Compose BOM 2026.05.00, Media3 1.9.1 retained, catalog hygiene, quality gates, depcheck, full-build gate, releaseRuntimeClasspath audit). Defer the AGP-9 cliff (Gradle 9.4.1 / AGP 9.2.0 / compileSdk 36 / Media3 1.10 / AndroidX sweep) and the device-bound baseline-profile regen, with user-ACCEPT hygiene on each deferral.

## TL;DR — PASS-WITH-NOTES

Phase 1 materially advances the v1.0 modernization foundation. All 12 LANDED requirements (DEPS-01, -02, -05, -06, -08, -09, -10[CASE B], -11, -12, -13, -14, -15, -17) verified in catalog and HEAD `65bb8d8`. The 5 deferred requirements (DEPS-03, -04, -07, -10[1.10 path], -16) all carry both a REQUIREMENTS.md backlog row AND a documented user-ACCEPT pointer (commit or CONTEXT.md note). The full-build gate `assembleDebug assembleRelease assembleBenchmark check` is green on the current HEAD (exit 0, 7s incremental). One forward-looking caveat: lint detectors are disabled on a Lifecycle-2.8.7 / Compose-BOM-2026.05.00 floor pending DEPS-07; this is documented in `KotlinAndroid.kt` and re-enables when AGP-9 phase lands. Verdict: **PASS-WITH-NOTES**.

## Per-Requirement Disposition

| Req | Status | Evidence |
|---|---|---|
| DEPS-01 — lint-baseline.xml | LANDED | commit `906495e` |
| DEPS-02 — JDK 17 toolchain pin + auto-download off | LANDED | commit `0523a94`; `jvmToolchain(17)` at `KotlinAndroid.kt:48` |
| DEPS-03 — Gradle 9.4.1 | DEFERRED (DEPS-17) | REQUIREMENTS.md §"Phase 1 trim"; Hilt AGP-9 incompat |
| DEPS-04 — AGP 9.2.0 / compileSdk 36 | DEFERRED (DEPS-17) | REQUIREMENTS.md §"Phase 1 trim"; coupled to DEPS-03 |
| DEPS-05 — Kotlin 2.2.20 + KSP 2.2.20-2.0.4 | LANDED | commit `be3e730` (atomic with DEPS-08); catalog L4-L5 |
| DEPS-06 — Compose BOM 2026.05.00 | LANDED | commit `f06ff0c`; catalog L16 |
| DEPS-07 — AndroidX sweep | DEFERRED | commit `0f725dc` + REQUIREMENTS.md §"Bump deferral"; requires AGP 8.9.1+ |
| DEPS-08 — Hilt 2.56.2 | LANDED | commit `be3e730`; catalog L6 (note: 2.57.1 was a phantom version) |
| DEPS-09 — Apollo 4.4.3 | LANDED | commit `0e82c02`; catalog L7 |
| DEPS-10 — Media3 1.9.1 retained (CASE B) | LANDED (CASE B) | commit `db63bd7`; catalog L28 + comment block L24-L27 |
| DEPS-10 (1.10 path) | DEFERRED (DEPS-17) | catalog comment + REQUIREMENTS.md §"Phase 1 trim" |
| DEPS-11 — Room dropped, collections-immutable added | LANDED | commit `c977e74`; `grep room` in catalog = 0 hits; `kotlinx-collections-immutable = 0.4.0` at L44 |
| DEPS-12 — detekt 1.23.8 + ktlint 13.1.0 | LANDED | commit `739aee7`; catalog L8-L9 |
| DEPS-13 — dependencyCheck plugin loads | LANDED | commit `6fae5a6`; catalog L10, L143 |
| DEPS-14 — full-build gate green | LANDED | commit `0a1e5f4`; re-verified below |
| DEPS-15 — releaseRuntimeClasspath audit | LANDED | commit `eb69569`; `deps-audit.txt` exists (1350 lines, 97 KB) |
| DEPS-16 — baseline profile regen | DEFERRED | commit `83e2b5d` + CONTEXT.md §"Deferred Ideas" + REQUIREMENTS.md §"Bump deferral"; REVIEWS-C4 device-gate, user ACCEPT |
| DEPS-17 — trim block (new req) | LANDED | commit `3bf3bbf` (REQUIREMENTS.md §"Phase 1 trim") |

## Build Invariant — Re-verified on HEAD 65bb8d8

```
$ JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ANDROID_SDK_ROOT=$HOME/Android/Sdk \
    ./gradlew --configuration-cache assembleDebug assembleRelease assembleBenchmark check
...
> Task :feature:settings:lintDebug
> Task :feature:settings:lint
> Task :feature:settings:check

BUILD SUCCESSFUL in 7s
1472 actionable tasks: 19 executed, 1453 up-to-date
Configuration cache entry stored.
```

Exit code: **0**. Wall time: 7s (incremental — full clean build invariant was demonstrated at DEPS-14 / `0a1e5f4`). Configuration-cache stored, 1472 actionable tasks, zero failures. The DEPS-14 acceptance gate continues to hold at HEAD `65bb8d8`.

## Catalog Snapshot — `gradle/libs.versions.toml`

```
agp = "8.7.3"                         (L3)   — AGP NOT 9.x ✓
kotlin = "2.2.20"                     (L4)
ksp = "2.2.20-2.0.4"                  (L5)
hilt = "2.56.2"                       (L6)
apollo = "4.4.3"                      (L7)
ktlint = "13.1.0"                     (L8)
detekt = "1.23.8"                     (L9)
dependencyCheck = "11.1.1"            (L10)
composeBom = "2026.05.00"             (L16)
media3 = "1.9.1"                      (L28)  — CASE B comment L24-L27
nextlibMedia3Ext = "1.9.1-0.11.0"     (L29)
kotlinxCollectionsImmutable = "0.4.0" (L44)  — present at 0.4.0 ✓
```

- `grep -i room gradle/libs.versions.toml` → 0 hits ✓
- `grep compileSdk` across `build-logic/convention/` + `baselineprofile/build.gradle.kts` → all `= 35` (no 36 anywhere) ✓
- `grep targetSdk` (same scope) → all `= 35` ✓
- `grep -E "context-receivers|skip-metadata|Xcontext|Xskip" KotlinAndroid.kt` → 0 hits (exit 1) ✓ — deprecated Kotlin flags removed
- `apply("org.jetbrains.kotlin.android")` present in BOTH `AndroidApplicationConventionPlugin.kt:13` AND `AndroidLibraryConventionPlugin.kt:13` ✓ — AGP-9 removal was correctly reverted
- `core/network/src/main/graphql/io/stashapp/android/graphql/schema.graphqls`: 4916 lines, 126 566 bytes — vendored from stashapp/stash develop ✓

## Deferral Hygiene

Every deferred requirement carries BOTH a REQUIREMENTS.md backlog row AND either an explicit user-ACCEPT commit OR a CONTEXT.md `## Deferred Ideas` block.

| Deferred Req | Backlog row | User-ACCEPT artifact |
|---|---|---|
| DEPS-03 (Gradle 9.4.1) | REQUIREMENTS.md L69 ("Phase 1 trim") | commit `3bf3bbf` (DEPS-17) |
| DEPS-04 (AGP 9 / compileSdk 36) | REQUIREMENTS.md L70 | commit `3bf3bbf` |
| DEPS-07 (AndroidX sweep) | REQUIREMENTS.md L74 ("Bump deferral") | commit `0f725dc` |
| DEPS-10 (Media3 1.10 path) | REQUIREMENTS.md L71 + catalog comment L24-L27 | commit `db63bd7` (CASE B retain) + commit `3bf3bbf` (1.10 deferral) |
| DEPS-16 (baseline profile regen) | REQUIREMENTS.md L75 | commit `83e2b5d` + CONTEXT.md §"Deferred Ideas" (L113-L125), explicit user ACCEPT recorded |

REVIEWS-C4 gate satisfied for DEPS-16 (CONTEXT.md edit + user ACCEPT). No deferral lacks documentation.

## Working-Tree Hygiene

`git status --short | grep -v '^??'` → empty. No uncommitted edits on tracked files. Only build outputs and untracked scaffolding (`gradle/wrapper/gradle-wrapper.jar`, `gradlew*`, `AGENTS.md`, `UI_HANDOFF.md`, all module `build/` dirs) are untracked — none are phase-1 changes.

## Goal Alignment vs PROJECT.md v1.0

Phase 1's purpose under v1.0 modernization is "set the foundation so PERF / COMPLY / POLISH can move." Verified:

- Toolchain floor raised to Kotlin 2.2.20 / KSP 2.2.20-2.0.4 / Hilt 2.56.2 / Apollo 4.4.3 / Compose BOM 2026.05.00. Strong-skipping, K2 stability and the Compose lifecycle changes PERF-03/04 depend on are now reachable.
- Catalog is cleaner: dead Room entries removed, `kotlinx-collections-immutable` available for PERF-03's `ImmutableList<T>` migration, depcheck wired in for POLISH-08 CI cache hashing.
- Lint baseline (DEPS-01) and dependency audit (DEPS-15) are in place — POLISH-02 can measure baseline shrinkage.
- The deferred items (AGP-9, compileSdk 36, AndroidX sweep, Media3 1.10, device-bound baseline regen) are the AGP-9 cliff, **not** foundation-blockers. They live in DEPS-17 as a tracked, well-justified deferral.

The re-scoped goal is achieved. Phase 2 (COMPLY) can begin against this floor.

## Risks Carried Forward

1. **Lint detectors disabled on Compose / Lifecycle floor.** `NullSafeMutableLiveData`, `FrequentlyChangingValue`, `RememberInComposition` are disabled in `KotlinAndroid.kt:37-43` because the bundled detectors from Lifecycle 2.8.7 + Compose BOM 2026.05.00 throw `IncompatibleClassChangeError` under AGP 8.7.3 lint + Kotlin 2.2.20. Re-enable in the AGP-9 / DEPS-07 phase. Compose / lifecycle regressions in those three areas will not be caught by lint until then — POLISH-02 should treat this as a known gap.
2. **Baseline profile not regenerated.** Committed `baseline-prof.txt` predates the Compose BOM 2026.05.00 bump. CONTEXT.md notes the relevant code paths haven't reshaped, but cold-start macrobench numbers in PERF-06 should NOT be compared against pre-bump baselines without a regen.
3. **AGP-9 cliff.** Hilt 2.56.2 is the current floor; no published Hilt yet supports AGP 9. DEPS-17 will need a re-probe once Dagger ships AGP-9 Hilt. The AGP-9 transition phase will likely also need to handle `CommonExtension` generics, Action-taking DSL overloads, `LibraryExtension.targetSdk`, and the `org.jetbrains.kotlin.android` plugin retirement together.
4. **Hilt 2.56.2 is one minor behind any future release.** Acceptable for v1.0 modernization; revisit on AGP-9.
5. **`kotlinx-collections-immutable = 0.4.0`** is the latest stable; the 0.5.0-beta01 line is excluded by the stable-only policy. PERF-03 lands against 0.4.0.

## Verdict

**PASS-WITH-NOTES** — Phase 1 goal (re-scoped under Option A) is achieved. Foundation modernization advanced as far as the AGP-9 / Hilt-AGP-9 cliff permits, with full deferral hygiene on every item left behind. Build is green on HEAD `65bb8d8`. Notes carried forward in "Risks Carried Forward" are tracked, not blocking. Proceed to Phase 2 (COMPLY).

---

*Verified: 2026-05-16T21:47+09:00*
*Verifier: Claude (gsd-verifier)*
*HEAD: `65bb8d86a9438a5bbcb97b63b9ce8ef3ecb3f544`*
*Build gate: `assembleDebug assembleRelease assembleBenchmark check` exit 0 (7s incremental)*
