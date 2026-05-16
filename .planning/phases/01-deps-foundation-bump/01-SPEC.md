# Phase 1: DEPS (Foundation Bump) — Specification

**Created:** 2026-05-16
**Ambiguity score:** 0.12
**Requirements:** 16 locked

## Goal

Move Slopper's build toolchain and dependency floor from `AGP 8.7.3 / Gradle 8.11.1 / Kotlin 2.1.0 / Compose BOM 2024.12.01 / compileSdk 35` to `AGP 9.2.0 / Gradle 9.4.1 / Kotlin 2.2.20 / Compose BOM 2026.05.00 / compileSdk 36`, with `assembleDebug assembleRelease assembleBenchmark check` green on JDK 17 and the baseline profile regenerated in the same PR series — without bumping `minSdk` and without changing app behavior.

## Background

Current state confirmed from the repository at 2026-05-16:

- `gradle/wrapper/gradle-wrapper.properties` — `gradle-8.11.1-bin.zip`
- `gradle/libs.versions.toml` — `agp = "8.7.3"`, `kotlin = "2.1.0"`, `ksp = "2.1.0-1.0.29"`, `hilt = "2.53.1"`, `apollo = "4.1.0"`, `composeBom = "2024.12.01"`, `media3 = "1.9.1"`, `nextlibMedia3Ext = "1.9.1-0.11.0"`, `okhttp = "4.12.0"`, `coil = "3.0.4"`, `kotlinxSerialization = "1.7.3"`, `kotlinxCoroutines = "1.9.0"`, `securityCrypto = "1.1.0"`, `detekt = "1.23.7"`, `ktlint = "12.1.1"`, `dependencyCheck = "11.1.1"`
- `build-logic/convention/src/main/kotlin/io/stashapp/android/buildlogic/KotlinAndroid.kt` — `compileSdk = 35`, `minSdk = 26`
- `build-logic/convention/src/main/kotlin/io/stashapp/android/buildlogic/AndroidApplicationConventionPlugin.kt` — `targetSdk = 35`
- `app/lint-baseline.xml` — **does not exist**

This phase is the first in the modernization milestone and gates every subsequent phase. Per `.planning/research/PITFALLS.md`, the order within DEPS is non-negotiable: lint-baseline must be generated *before* any version bumps, because Lint rules ship with AGP and a fresh baseline against the new AGP would mask legitimate new findings discovered during later phases.

The two open conditional decisions are LOCKED for this spec:
- **Media3 1.10** is gated on `io.github.anilbeesetti:nextlib-media3ext:1.10.x` existing on Maven Central. If it does not exist at execution time, Media3 stays on `1.9.1` and a comment is added to `gradle/libs.versions.toml` explaining why.
- **`dependencyCheck`** is bumped only if AGP 9 / Gradle 9 force it (plugin incompatibility); otherwise left at `11.1.1`. If the plugin is not actually wired anywhere, it is removed from the catalog (catalog hygiene).

## Requirements

1. **Lint baseline established first**: `app/lint-baseline.xml` exists and is committed before any version bump.
   - Current: file does not exist
   - Target: `app/lint-baseline.xml` checked in, generated from the current (pre-bump) AGP
   - Acceptance: `git log --diff-filter=A -- app/lint-baseline.xml` returns a commit whose parent is the pre-Phase-1 HEAD; the commit precedes any change to `gradle/libs.versions.toml`'s `agp`/`gradle`/`kotlin` entries

2. **JDK toolchain pinned to 17**: Build runs on JDK 17 with no auto-download.
   - Current: no explicit `jvmToolchain(17)` declaration; relies on ambient JDK
   - Target: `kotlin { jvmToolchain(17) }` declared in `build-logic/convention/.../KotlinAndroid.kt`; `org.gradle.java.installations.auto-download=false` set in `gradle.properties`
   - Acceptance: `./gradlew -q javaToolchains` shows exactly one resolved JDK 17; build succeeds on a machine with only JDK 17 installed

3. **Gradle wrapper at 9.4.1 with configuration cache**: Wrapper bumped, configuration cache enabled and green.
   - Current: `gradle-8.11.1-bin.zip`, configuration cache status unverified
   - Target: `gradle-9.4.1-bin.zip` in `gradle-wrapper.properties`; `org.gradle.configuration-cache=true` in `gradle.properties`
   - Acceptance: `./gradlew --configuration-cache assembleDebug` succeeds; rerun reuses the configuration cache (logs `Reusing configuration cache`)

4. **AGP 9.2.0 with `compileSdk = 36` / `targetSdk = 36`**: Convention plugins updated; R8 keep rules audited under AGP 9 semantics.
   - Current: AGP 8.7.3, `compileSdk = 35`, `targetSdk = 35`
   - Target: AGP 9.2.0 in catalog; `compileSdk = 36` in `KotlinAndroid.kt`; `targetSdk = 36` in `AndroidApplicationConventionPlugin.kt` AND `AndroidLibraryConventionPlugin.kt`; R8 keep rules (`*-rules.pro`) reviewed for AGP-9 wildcard semantic changes
   - Acceptance: `assembleRelease` (minified) succeeds; APK smoke-launches without `ClassNotFoundException` / `MissingBindingException`; lint reports zero new "deprecated" warnings against compileSdk 36

5. **Kotlin 2.2.20 with matching KSP and Compose Compiler**: Three coupled versions advanced lockstep.
   - Current: Kotlin 2.1.0, KSP 2.1.0-1.0.29, Compose Compiler plugin implicit via Kotlin 2.1.0
   - Target: `kotlin = "2.2.20"` and matching KSP for Kotlin 2.2.20 in the catalog; Compose Compiler plugin coordinate explicit and matched to Kotlin 2.2.20 (per the Compose-to-Kotlin compatibility map); `-Xcontext-receivers` replaced with `-Xcontext-parameters` everywhere it appears; `-Xskip-metadata-version-check` removed if no longer needed
   - Acceptance: `./gradlew :app:kspDebugKotlin` and `:app:compileDebugKotlin` succeed clean; no compiler warnings about deprecated K1 flags; `grep -rE '(-Xcontext-receivers|-Xskip-metadata-version-check)' --include=build.gradle.kts --include=*.kts /home/yun/slopper` returns no hits

6. **Compose BOM 2026.05.00 with explicit versions removed**: BOM controls Compose libs.
   - Current: `composeBom = "2024.12.01"`; some Compose libs may carry explicit versions in the catalog
   - Target: `composeBom = "2026.05.00"`; every Compose-family entry in `libs.versions.toml` is BOM-controlled (no `version.ref`)
   - Acceptance: `grep -nE '^androidx-compose-.*version' gradle/libs.versions.toml` returns no per-library version pins; `./gradlew :app:dependencies` shows Compose libs resolving via BOM

7. **AndroidX sweep**: Lifecycle / Activity / Navigation / DataStore / Coil aligned with research targets.
   - Current: Lifecycle 2.8.7, Activity Compose 1.9.3, Navigation Compose 2.8.5, DataStore 1.1.1, Coil 3.0.4
   - Target: Lifecycle 2.10.0, Activity Compose 1.13.0, Navigation Compose 2.9.6, DataStore 1.2.1, Coil 3.4.0
   - Acceptance: catalog entries match these targets exactly; `./gradlew assembleDebug` succeeds

8. **Hilt / coroutines / serialization sweep**: KSP-coupled bumps land without aggregating-processor regressions.
   - Current: Hilt 2.53.1, kotlinx.coroutines 1.9.0, kotlinx.serialization 1.7.3
   - Target: Hilt 2.57.1, coroutines 1.11.0, kotlinx.serialization 1.9.0
   - Acceptance: `./gradlew clean :app:kspDebugKotlin` succeeds without `NullPointerException` from Hilt aggregating processor; debug APK launches and reaches the first DI-injected screen without `MissingBindingException`

9. **Apollo stays on 4.4.3, OkHttp stays on 4.12.0**: Conservative bump on Apollo, no change on OkHttp.
   - Current: Apollo 4.1.0, OkHttp 4.12.0
   - Target: Apollo 4.4.3 in catalog; OkHttp explicitly pinned to 4.12.0 in catalog (already there; verify no drift); a comment in `libs.versions.toml` notes that Apollo 5 and OkHttp 5 are deferred to a future milestone
   - Acceptance: catalog entries match; `./gradlew :app:dependencyInsight --dependency com.squareup.okhttp3:okhttp` resolves to 4.12.0 from a single source

10. **Media3 1.10.0 conditional**: Bumped only if `nextlib-media3ext:1.10.x` is published; otherwise pinned at 1.9.1 with explicit rationale.
    - Current: Media3 1.9.1, `nextlib-media3ext = "1.9.1-0.11.0"`
    - Target — case A (extension available): `media3 = "1.10.0"` and matching `nextlibMedia3Ext` entry
    - Target — case B (extension not available): `media3 = "1.9.1"` retained, and a comment in `libs.versions.toml` reading approximately: `# Media3 stays on 1.9.1 — nextlib-media3ext has no 1.10.x build on Maven Central as of YYYY-MM-DD. Revisit next milestone.`
    - Acceptance: exactly one of case A or case B is implemented; the chosen case is justified by a Maven Central lookup recorded in the phase plan execution log

11. **Version catalog hygiene**: Unused entries removed, `kotlinx.collections.immutable` added.
    - Current: unused `room = "2.6.1"` (and related libraries) in catalog; no `kotlinx.collections.immutable`
    - Target: Room entries removed if `grep -rE 'androidx\.room' app core feature` returns no hits; `kotlinxCollectionsImmutable = "0.3.8"` (or current stable) added under `[versions]` and a corresponding `[libraries]` entry
    - Acceptance: `gradle/libs.versions.toml` has no unused entries (verified by `grep` for each `[libraries]` module); `kotlinx.collections.immutable` is declared and consumable

12. **Quality gates aligned**: Detekt and ktlint plugins bumped; baselines refreshed.
    - Current: Detekt 1.23.7, ktlint plugin 12.1.1
    - Target: Detekt 1.23.8 and ktlint plugin 13.1.0 in catalog; detekt + ktlint baseline files regenerated after format churn
    - Acceptance: `./gradlew detekt ktlintCheck` returns exit 0; the detekt and ktlint baseline files are newer than the catalog change commit

13. **`dependencyCheck` plugin handled**: Bumped only if forced, removed if unused.
    - Current: `dependencyCheck = "11.1.1"` in catalog
    - Target: if the plugin is referenced anywhere (`grep -rE 'dependency-check|dependencyCheckAnalyze' build-logic/ */build.gradle.kts`), bump only when AGP/Gradle 9 force it; if not referenced, remove the entry from the catalog
    - Acceptance: either (a) plugin is wired and the build succeeds with whatever version is pinned; or (b) the catalog entry no longer appears in `[versions]`

14. **Build green end-to-end**: Full assemble + check pass.
    - Current: no enforced gate
    - Target: `./gradlew --configuration-cache assembleDebug assembleRelease assembleBenchmark check` succeeds in a clean clone
    - Acceptance: a single command invocation of the above on a clean checkout exits 0; CI workflow (if updated in DEPS-08-CI-cache scope) reports green for the merge commit that closes Phase 1

15. **Dependency tree clean**: No surprise version skews.
    - Current: unverified
    - Target: every AndroidX artifact resolves to the version intended in `libs.versions.toml`; no two majors of the same artifact in the resolved graph
    - Acceptance: `./gradlew :app:dependencies` output reviewed against the catalog; any version-mismatch warning is either documented in the phase log or resolved before merge

16. **Baseline profile regenerated in the same PR series**: Stale profile is slower than no profile.
    - Current: `app/src/release/generated/baselineProfiles/baseline-prof.txt` predates the dep bumps in this phase
    - Target: profile regenerated against the bumped toolchain in the same logical PR series that lands DEPS-04..DEPS-08
    - Acceptance: `git log -1 --format=%H -- app/src/release/generated/baselineProfiles/baseline-prof.txt` returns a commit no older than the commit that bumped `composeBom` in `libs.versions.toml`

## Boundaries

**In scope:**
- All version bumps listed in Requirements 2–13
- `app/lint-baseline.xml` generation (Requirement 1)
- Catalog hygiene (Requirement 11) — removing genuinely unused entries
- Detekt + ktlint baseline refresh (Requirement 12) — only because the bumps churn formatting
- R8 / proguard keep-rule audit triggered by AGP 9 + serialization 1.9 (Requirement 4)
- Baseline profile regeneration triggered by the bumps (Requirement 16)

**Out of scope:**
- `minSdk` bump — frozen project-wide
- `MediaSessionService` introduction — handled by Phase 2 (permission removal only)
- Edge-to-edge / predictive back / Splash API work — Phase 2 (COMPLY)
- Gradle-managed device wiring — Phase 3 (PERF)
- `PlayerScreen.kt` split — Phase 4 (POLISH)
- New test infrastructure (JUnit5/Turbine/MockK/Robolectric) — Phase 4 (POLISH)
- Apollo 5 migration — deferred to future milestone (NET-01)
- Navigation 3 migration — deferred (NAV-01)
- `androidx.security:security-crypto` replacement — deferred (SEC-01)
- Any source-code behavior change beyond the minimum required to keep the build green (API-rename follow-ups, deprecation suppressions, and codegen-mandated edits only)

## Constraints

- **Compatibility:** `minSdk = 26` must not change. Builds must remain installable on every device currently supported.
- **Risk profile:** stable releases only. No alphas / RCs / betas. The single exception path is Media3 1.10 (already a stable release) gated on nextlib availability.
- **Architecture:** module graph (`app/`, `core/*`, `feature/*`, `build-logic/`, `baselineprofile/`) is frozen. All changes are within-module.
- **Build invariants:** after every meaningful subtask, the build must remain green (`assembleDebug` minimum). No "fix it later" merges.
- **Toolchain:** JDK 17, not 21 — research confirmed 21 is unnecessary churn for this milestone.
- **Order:** lint-baseline (Req 1) → JDK toolchain (Req 2) → **[Gradle wrapper (Req 3) + AGP + SDK (Req 4) — single atomic commit; Gradle 9 cannot load AGP 8]** → Kotlin/KSP/Compose-compiler (Req 5) → Compose BOM (Req 6) → AndroidX sweep (Req 7) → Hilt/coroutines/serialization (Req 8) → Apollo/OkHttp (Req 9) → Media3 conditional (Req 10) → catalog hygiene (Req 11) → quality gates (Req 12) → dependencyCheck (Req 13) → green-build verification (Req 14) → dependency-tree review (Req 15) → baseline-profile regen (Req 16). Plans may parallelize within independent segments (e.g. lint-baseline + JDK toolchain are independent and safe to land together), but the four lockstep gates (Req 4 → 5, Req 5 → 6, Req 6 → 7, Req 14 → 16) are strict. **Exception to the atomic-per-requirement rule (CONTEXT.md Decision 1 Refinement, 2026-05-16):** Req 3 and Req 4 land in a SINGLE commit because Gradle 9.x physically cannot load AGP 8.x — there is no intermediate green state to commit between them.
- **Verification cost:** every requirement must be checkable by a command on a clean checkout — no manual "I looked at it" passes.

## Acceptance Criteria

- [ ] `app/lint-baseline.xml` exists and the commit creating it precedes every version bump in this phase (per `git log` ordering on `gradle/libs.versions.toml`)
- [ ] `./gradlew -q javaToolchains` reports JDK 17 as the resolved toolchain; `org.gradle.java.installations.auto-download=false` set in `gradle.properties`
- [ ] `./gradlew --configuration-cache assembleDebug` succeeds; second run logs `Reusing configuration cache`
- [ ] `gradle/libs.versions.toml` has: `agp = "9.2.0"`, `kotlin = "2.2.20"`, matching `ksp`, `composeBom = "2026.05.00"`, `lifecycle = "2.10.0"`, `activityCompose = "1.13.0"`, `navigationCompose = "2.9.6"`, `datastore = "1.2.1"`, `coil = "3.4.0"`, `hilt = "2.57.1"`, `kotlinxCoroutines = "1.11.0"`, `kotlinxSerialization = "1.9.0"`, `apollo = "4.4.3"`, `okhttp = "4.12.0"`, `detekt = "1.23.8"`, `ktlint = "13.1.0"` (plugin coordinate)
- [ ] `compileSdk = 36` in `KotlinAndroid.kt`; `targetSdk = 36` in both `AndroidApplicationConventionPlugin.kt` and `AndroidLibraryConventionPlugin.kt`; `minSdk = 26` unchanged
- [ ] Media3 catalog state is *one of*: (a) `media3 = "1.10.0"` with matching `nextlibMedia3Ext` 1.10.x entry, OR (b) `media3 = "1.9.1"` retained with a comment explaining the deferral and the date the Maven Central lookup was performed
- [ ] No occurrences of `-Xcontext-receivers` or `-Xskip-metadata-version-check` in any `.kts` or `.gradle.kts` file in the repo
- [ ] `kotlinx.collections.immutable` declared in `libs.versions.toml` (`[versions]` and `[libraries]`)
- [ ] Unused entries (Room, etc.) removed from the catalog after confirming with `grep`
- [ ] `./gradlew --configuration-cache assembleDebug assembleRelease assembleBenchmark check` exits 0 on a clean clone
- [ ] `./gradlew detekt ktlintCheck` exits 0; both baseline files newer than the catalog bump commit
- [ ] `./gradlew :app:dependencies` reviewed; no two majors of the same AndroidX artifact resolved; any waivers documented in the phase plan execution log
- [ ] `app/src/release/generated/baselineProfiles/baseline-prof.txt` last-modified commit is at-or-after the Compose BOM bump commit
- [ ] R8 keep-rule files (`*-rules.pro`) audited for AGP 9 wildcard semantic changes and kotlinx.serialization 1.9 keep rules; release APK launches without crash on first DI-injected screen

## Ambiguity Report

| Dimension          | Score | Min  | Status | Notes                                                                 |
|--------------------|-------|------|--------|-----------------------------------------------------------------------|
| Goal Clarity       | 0.92  | 0.75 | ✓      | Target versions are numeric and locked; conditional case (Media3) defined |
| Boundary Clarity   | 0.88  | 0.70 | ✓      | In-scope and out-of-scope explicit; cross-phase boundaries named      |
| Constraint Clarity | 0.87  | 0.65 | ✓      | minSdk frozen, stable-only, JDK 17, ordering rules locked              |
| Acceptance Criteria| 0.82  | 0.70 | ✓      | Every requirement has a command-checkable acceptance test              |
| **Ambiguity**      | 0.12  | ≤0.20| ✓      | Gate passed before interview; two clarifying decisions then locked     |

Status: ✓ = met minimum, ⚠ = below minimum (planner treats as assumption)

## Interview Log

Initial assessment already passed the gate (ambiguity 0.14). Two clarifying questions asked to sharpen the spec.

| Round | Perspective | Question summary | Decision locked |
|-------|-------------|------------------|-----------------|
| 0 | — | Pre-interview score from REQUIREMENTS.md + ROADMAP.md + research | Gate already passing at 0.14 |
| 1a | Boundary Keeper | How should Phase 1 treat `dependencyCheck` plugin? | Bump only if AGP/Gradle 9 force it; remove from catalog if unused |
| 1b | Boundary Keeper | If `nextlib-media3ext:1.10.x` not on Maven Central, what does Phase 1 do? | Stay on Media3 1.9.1, document in catalog comment, defer to next milestone |

---

*Phase: `01-deps-foundation-bump`*
*Spec created: 2026-05-16*
*Next step: `/gsd-discuss-phase 1` — implementation decisions (how to build what's specified above)*
