# Phase 1: DEPS (Foundation Bump) — Research

**Researched:** 2026-05-16
**Domain:** Android build-toolchain & dependency-floor migration (AGP 8.7.3 → 9.2.0, Kotlin 2.1.0 → 2.2.20, Compose BOM 2024.12.01 → 2026.05.00, compileSdk 35 → 36, Gradle 8.11.1 → 9.4.1)
**Confidence:** HIGH (all conditional gates resolved by live Maven Central probes; all 16 requirements have command-checkable acceptance)

> **Mandate:** SPEC.md (16 reqs) and CONTEXT.md (3 decisions) are locked. This file is the **last-mile, edit-level recipe** the planner uses to write per-requirement atomic-commit plans. Ecosystem rationale lives in `.planning/research/STACK.md` and `.planning/research/PITFALLS.md` — cited inline, not duplicated.

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

1. **Commit/PR shape — linear branch, atomic per-req commits.** Single branch `phase-1/deps-bump`. One commit per requirement DEPS-01..DEPS-16. Format: `chore(deps): <req-id> — <short description>`. One PR opened against `master` at phase end. Planner MUST produce per-requirement atomic commits; executor MUST NOT collapse without explicit justification.

2. **Media3 1.10 — HTTP-probe Maven Central before touching catalog.** Probe `curl -sf https://repo1.maven.org/maven2/io/github/anilbeesetti/nextlib-media3ext/maven-metadata.xml | grep -oE '<version>1\.10[^<]*</version>' | head -3` at execution time. Both branches (case A bump / case B retain+comment) must be specified in the DEPS-10 plan step. Probe date is the date written into the comment.

3. **Failure-recovery — revert-and-continue, EXCEPT lockstep chain.** Lockstep chain = lint-baseline → JDK → Gradle → AGP/SDK → Kotlin/KSP/Compose-compiler → Compose BOM (DEPS-01..06). Failure in lockstep → **stop the phase**. Failure in DEPS-07..16 → revert the offending commit, log failure, add to deferred backlog, continue. Plan log template MUST include a "Failures & Reverts" section.

### Claude's Discretion

- **R8 keep-rule audit approach:** incremental — keep existing `app/proguard-rules.pro`; for each affected library group (Apollo 4.4.3, Hilt 2.57.1, kotlinx.serialization 1.9.0, Media3 1.10, Compose BOM 2026.05.00), consult upstream notes and **append** with inline comment citing the source. Smoke-test release APK reaches the first DI-injected screen.
- **Detekt/ktlint baseline regeneration policy:** re-baseline ktlint format churn (expected from Kotlin 2.2); fail-fast on net new *detekt* findings; document each detekt finding that gets baselined with a reason.
- **dependencyCheck plugin treatment:** grep `build-logic/` and all `build.gradle.kts`. If absent → remove from catalog. If present and loads on AGP 9 → leave as-is. If present and broken on AGP 9 → revert-and-continue (Decision 3). **Probe result below confirms: plugin IS wired** — DEPS-13 treats as "leave as-is unless AGP 9 breaks it."
- **Verification cadence:** `./gradlew --configuration-cache assembleDebug` after every commit; full `assembleDebug assembleRelease assembleBenchmark check` at step-group boundaries (after DEPS-04, DEPS-06, DEPS-13) and before DEPS-16.
- **`targetSdk = 36` extraction:** if planner extracts to a shared constant in `KotlinAndroid.kt` (currently hardcoded in two convention plugins), it lands inside the DEPS-04 commit — not a separate refactor commit.

### Deferred Ideas (OUT OF SCOPE)

(none surfaced during discuss-phase — scope discipline held)
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| DEPS-01 | Lint baseline established first | Recipe §DEPS-01; PITFALLS §18 |
| DEPS-02 | JDK toolchain pinned to 17 | Recipe §DEPS-02; PITFALLS §1 |
| DEPS-03 | Gradle wrapper at 9.4.1 with configuration cache | Recipe §DEPS-03; PITFALLS §2 |
| DEPS-04 | AGP 9.2.0 with compileSdk/targetSdk = 36 | Recipe §DEPS-04; PITFALLS §9; R8 audit §4 |
| DEPS-05 | Kotlin 2.2.20 + matching KSP + Compose Compiler | Recipe §DEPS-05; PITFALLS §3, §4, §12; version probe §3 |
| DEPS-06 | Compose BOM 2026.05.00 | Recipe §DEPS-06; PITFALLS §14, §17 |
| DEPS-07 | AndroidX sweep (Lifecycle / Activity / Nav / DataStore / Coil) | Recipe §DEPS-07; PITFALLS §10 |
| DEPS-08 | Hilt 2.57.1 + coroutines 1.11.0 + serialization 1.9.0 | Recipe §DEPS-08; PITFALLS §3, §11 |
| DEPS-09 | Apollo 4.4.3, OkHttp 4.12.0 retained | Recipe §DEPS-09 |
| DEPS-10 | Media3 1.10.0 conditional | Recipe §DEPS-10; probe §3 (**case A confirmed**) |
| DEPS-11 | Catalog hygiene (Room out, kotlinx-collections-immutable in) | Recipe §DEPS-11; grep §3 |
| DEPS-12 | Detekt 1.23.8 + ktlint plugin 13.1.0 | Recipe §DEPS-12; PITFALLS §12 |
| DEPS-13 | dependencyCheck plugin handled | Recipe §DEPS-13; **wired** confirmed §3 |
| DEPS-14 | Build green end-to-end | Recipe §DEPS-14; smoke matrix §5 |
| DEPS-15 | Dependency tree clean | Recipe §DEPS-15; PITFALLS §10 |
| DEPS-16 | Baseline profile regenerated | Recipe §DEPS-16; PITFALLS §6 |
</phase_requirements>

---

## 1. Summary

Five things the planner must internalize above everything else:

1. **Both conditional gates resolve to the optimistic branch.** Maven Central probe confirms `nextlib-media3ext:1.10.0-0.12.1` exists (released 2026-04-14) — DEPS-10 takes **case A** (bump Media3 to 1.10.0). `dependencyCheck` plugin IS wired in root `build.gradle.kts` (plugin applied + `dependencyCheck { }` block at lines ~13–30) — DEPS-13 leaves it in place unless AGP 9 forces it.
2. **Two compiler flags must move together with the Kotlin bump.** Only `KotlinAndroid.kt` lines 38 and 44 contain `-Xcontext-receivers` and `-Xskip-metadata-version-check`. There are **zero call sites** in `core/*` / `feature/*` (no actual `context(Foo)` syntax in the codebase — confirmed via grep). Both flags can be **deleted outright** at DEPS-05; no call-site migration required. This is one of the lowest-risk parts of the phase.
3. **Convention plugin has three SDK touchpoints; targetSdk is in TWO files.** `compileSdk` lives in `KotlinAndroid.kt:21`. `targetSdk = 35` is duplicated in `AndroidApplicationConventionPlugin.kt:17` AND `AndroidLibraryConventionPlugin.kt:17`. `baselineprofile/build.gradle.kts` also hardcodes `compileSdk = 35` (line 9) and `targetSdk = 35` (line 21) — **this fourth touchpoint is easy to miss** and SPEC.md doesn't call it out explicitly. The planner MUST include `baselineprofile/build.gradle.kts` in the DEPS-04 file list.
4. **Compose catalog is already BOM-clean.** Grep returns one match (`androidx-compose-bom` itself); every other Compose entry already omits `version.ref`. DEPS-06 is purely a `composeBom = "2026.05.00"` one-line change + a re-check pass.
5. **Baseline profile regen needs a connected device or emulator.** `baselineprofile/build.gradle.kts` line 36 has `useConnectedDevices = true` and GMD wiring is explicitly deferred to Phase 3. For DEPS-16 the executor must have either a 120Hz Pixel plugged in via adb or an emulator with `Pixel 6 API 34` running. Task name: `./gradlew :app:generateReleaseBaselineProfile` (AGP-9 baseline-profile plugin convention). Output path: `app/src/release/generated/baselineProfiles/baseline-prof.txt`.

---

## 2. Per-Requirement Implementation Recipe (DEPS-01..16)

Recipes are **diff-grade**: file path → exact line/region → what to change → verification command.

### DEPS-01 — Lint baseline established first

**Files touched:** `app/lint-baseline.xml` (NEW, generated).

**Action:**
```bash
./gradlew :app:updateLintBaseline
git add app/lint-baseline.xml
```

**Why this order matters:** `app/build.gradle.kts:98` already declares `lint { baseline = file("lint-baseline.xml") }`; the file just doesn't exist. Running `updateLintBaseline` *against the pre-bump AGP* freezes the current AGP 8.7.3 warning surface. If we generated it after AGP 9 lands, new AGP-9-only checks (edge-to-edge, predictive back, Android-15 behavior) get masked rather than surfaced — `[CITED: PITFALLS.md §18]`.

**Verification:** `ls app/lint-baseline.xml` exists; `git log --diff-filter=A -- app/lint-baseline.xml` returns a commit; that commit's diff doesn't touch `gradle/libs.versions.toml`.

**Parallel-safe with DEPS-02** per ROADMAP Plan 1.1.

---

### DEPS-02 — JDK toolchain pinned to 17

**Files touched:**
- `build-logic/convention/src/main/kotlin/io/stashapp/android/buildlogic/KotlinAndroid.kt`
- `gradle.properties`

**Edit `KotlinAndroid.kt`:** add a `jvmToolchain` block inside `configureKotlinAndroid`. The clean placement is inside the `extensions.configure<KotlinAndroidProjectExtension>` block (line 34) since the toolchain is a Kotlin DSL feature:

```kotlin
extensions.configure<KotlinAndroidProjectExtension> {
    jvmToolchain(17)          // NEW
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.addAll(
            // -Xcontext-receivers removed at DEPS-05
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            // -Xskip-metadata-version-check removed at DEPS-05
        )
    }
}
```

Note: DEPS-02 commit keeps the two `-X` flags (they're removed in DEPS-05). Only the `jvmToolchain(17)` line is added now.

**Edit `gradle.properties`:** append at the end:
```properties
# Pin daemon JDK to the toolchain spec — never let Gradle auto-download a JDK
org.gradle.java.installations.auto-download=false
```

**Verification:**
```bash
./gradlew -q javaToolchains              # one JDK 17 resolved
./gradlew --configuration-cache assembleDebug
```

`[CITED: PITFALLS.md §1]`. `[VERIFIED: AGP 9.2.0 release notes — JDK 17 minimum + default]`.

---

### DEPS-03 — Gradle wrapper 9.4.1 with config cache

**Files touched:** `gradle/wrapper/gradle-wrapper.properties`. (`org.gradle.configuration-cache=true` is already in `gradle.properties:4` — no change there.)

**Edit `gradle-wrapper.properties:3`:**
```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-9.4.1-bin.zip
```

**Procedure (config-cache-safe):**
```bash
# Edit the property file by hand, then prime the wrapper:
./gradlew wrapper --gradle-version 9.4.1 --distribution-type bin
# Verify
./gradlew --version                                       # → Gradle 9.4.1
./gradlew --configuration-cache assembleDebug             # first run: stores
./gradlew --configuration-cache assembleDebug             # second run: "Reusing configuration cache"
```

If `dependencyCheck` reappears as the config-cache violator (it ran with `--no-configuration-cache` historically), do NOT fix it here — that's DEPS-13 scope. Phase-1 verification deliberately uses `assembleDebug`, which doesn't invoke `dependencyCheckAnalyze`.

`[CITED: STACK.md — Gradle 9.4.1 is AGP 9.2 minimum]`. `[CITED: Gradle 9 upgrade guide]`.

---

### DEPS-04 — AGP 9.2.0, compileSdk/targetSdk 36, R8 audit

**Files touched:**
- `gradle/libs.versions.toml` — `agp = "9.2.0"`
- `build-logic/convention/src/main/kotlin/io/stashapp/android/buildlogic/KotlinAndroid.kt` — `compileSdk = 36`
- `build-logic/convention/src/main/kotlin/io/stashapp/android/buildlogic/AndroidApplicationConventionPlugin.kt` — `targetSdk = 36`
- `build-logic/convention/src/main/kotlin/io/stashapp/android/buildlogic/AndroidLibraryConventionPlugin.kt` — `targetSdk = 36`
- `baselineprofile/build.gradle.kts` — `compileSdk = 36` (line 9), `targetSdk = 36` (line 21) **← do not forget this fourth file**
- `app/proguard-rules.pro` — R8 keep-rule deltas (see §4 below)

**Catalog edit:**
```toml
agp = "9.2.0"
```

**Optional `targetSdk` constant refactor (Claude's discretion, per CONTEXT.md):** add to `KotlinAndroid.kt`:
```kotlin
internal const val TARGET_SDK = 36
```
Then reference it from both convention plugins (`defaultConfig.targetSdk = TARGET_SDK`). If the planner picks this up, it lands in the same DEPS-04 commit — not a separate refactor.

**Optional plugin-block cleanup (AGP 9 introduces built-in Kotlin support):** AGP 9 no longer requires `apply("org.jetbrains.kotlin.android")` in both convention plugins (lines 13 of `AndroidApplicationConventionPlugin.kt` and `AndroidLibraryConventionPlugin.kt`). **Recommendation: leave as-is for DEPS-04** — removing the explicit plugin is a discretionary cleanup that risks subtle KGP behavior changes; planner should treat this as deferred backlog rather than land it in the AGP-bump commit. `[ASSUMED: AGP-9 built-in Kotlin is opt-in for existing projects, not auto-applied]` — confirm against AGP 9.2 release notes at execution time before deciding.

**R8 keep rules:** see §4 of this document for the full append-only delta.

**Verification:**
```bash
./gradlew --configuration-cache assembleDebug
./gradlew assembleRelease                                 # minified — proves R8 didn't strip serializer classes
./gradlew :app:assembleBenchmark                          # minified + smoke target
# Manual: install benchmark APK, launch, reach first @HiltViewModel screen → no MissingBindingException
```

`[CITED: PITFALLS.md §9 — R8 wildcard semantics changed under AGP 9]`. `[CITED: AGP 9.2.0 release notes — compileSdk 36 supported up to 36.1, BuildTools 36 required]`.

---

### DEPS-05 — Kotlin 2.2.20 + matching KSP + Compose Compiler plugin

**Files touched:**
- `gradle/libs.versions.toml` — `kotlin = "2.2.20"`, `ksp = "2.2.20-2.0.4"`
- `build-logic/convention/src/main/kotlin/io/stashapp/android/buildlogic/KotlinAndroid.kt` — remove both deprecated `-X` flags

**Catalog edit:**
```toml
kotlin = "2.2.20"            # MUST match Compose Compiler plugin alias (kotlin-compose) — same version.ref
ksp = "2.2.20-2.0.4"         # MUST match kotlin above — KSP2 for Kotlin 2.2.20 (verified on Maven Central 2026-05-16)
```

Note: `libs.versions.toml:133` declares `kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }` — already keyed to `kotlin`, so bumping `kotlin` automatically bumps the Compose Compiler plugin. **No second catalog edit needed** for the Compose Compiler plugin. `[VERIFIED: libs.versions.toml line 133]`.

**Remove deprecated `-X` flags in `KotlinAndroid.kt:37-45`:** delete lines 38 (`"-Xcontext-receivers"`) and 44 (`"-Xskip-metadata-version-check"`). Final block:

```kotlin
compilerOptions {
    jvmTarget.set(JvmTarget.JVM_17)
    freeCompilerArgs.addAll(
        "-opt-in=kotlin.RequiresOptIn",
        "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
    )
}
```

**Migration risk check (grep audit, 2026-05-16):** `grep -rE '(-Xcontext-receivers|-Xskip-metadata-version-check)' --include='*.kts' --include='*.kt' /home/yun/slopper` shows **only** the two `KotlinAndroid.kt` lines. There is no source-level usage of `context(Foo)` syntax in `core/*` or `feature/*` — so no `-Xcontext-parameters` reintroduction is needed. The flags can be deleted outright. `[VERIFIED: grep audit completed 2026-05-16]`.

`-Xskip-metadata-version-check` was tolerating nextlib AARs built against a newer Kotlin (per the inline comment at `KotlinAndroid.kt:42-43`). After bumping to Kotlin 2.2.20 with nextlib 1.10.0-0.12.1, the gap closes — the flag is no longer needed. If a `kotlin_module` metadata error appears at compile time, **temporarily** re-add the flag and document it as a deferred follow-up rather than reverting DEPS-05. `[ASSUMED: nextlib 1.10.0-0.12.1 is built against Kotlin ≤ 2.2.x — verify by checking its `kotlin_module` after the build]`.

**Verification:**
```bash
./gradlew :app:kspDebugKotlin                              # clean
./gradlew :app:compileDebugKotlin                          # no deprecated-K1-flag warnings
grep -rE '(-Xcontext-receivers|-Xskip-metadata-version-check)' --include='*.kts' --include='*.kt' /home/yun/slopper   # → no hits
./gradlew :app:dependencies --configuration releaseRuntimeClasspath | grep compose-compiler  # exactly one version
```

`[CITED: PITFALLS.md §3 KSP/Kotlin drift]`. `[CITED: PITFALLS.md §4 Compose Compiler coordinate]`. `[CITED: PITFALLS.md §12 detekt/ktlint rule drift — handled in DEPS-12]`.

---

### DEPS-06 — Compose BOM 2026.05.00

**Files touched:** `gradle/libs.versions.toml` only.

**Catalog edit:**
```toml
composeBom = "2026.05.00"
```

**Catalog hygiene check (already clean):** `grep -nE '^androidx-compose-.*version' gradle/libs.versions.toml` returns one hit — the BOM line itself (`androidx-compose-bom`). Every other Compose entry (`androidx-compose-ui`, `androidx-compose-material3`, etc.) already lacks a `version.ref` — confirmed in `libs.versions.toml:58-64`. `[VERIFIED: grep result on libs.versions.toml]`. PITFALLS §14 is therefore already satisfied; no per-library `version.ref` to remove.

**Verification:**
```bash
./gradlew :app:dependencies --configuration releaseRuntimeClasspath | grep 'androidx.compose'  # versions resolve via BOM
./gradlew :app:compileReleaseKotlin                       # zero deprecation warnings (PITFALLS §17 — ScaleToBounds rename etc.)
./gradlew --configuration-cache assembleDebug
```

Watch for: `ScaleToBounds` → `scaleToBounds()` rename, `RetainScope` → `RetainedValuesStore` rename `[CITED: PITFALLS.md §17]`. The current codebase is small enough that these surface as immediate compile errors, not silent miscompiles.

---

### DEPS-07 — AndroidX sweep

**Files touched:** `gradle/libs.versions.toml`.

**Catalog edits:**
```toml
lifecycle = "2.10.0"
activityCompose = "1.13.0"
navigationCompose = "2.9.6"
datastore = "1.2.1"
coil = "3.4.0"
# coreKtx — leave at 1.15.0 unless 1.18.0 explicitly required (SPEC.md doesn't mandate; STACK.md mentions 1.18.0).
#   Recommendation: leave alone in DEPS-07 to keep the diff minimal; bump separately if a future requirement needs it.
```

`coreKtx` is **not** in SPEC.md's exact-match list of catalog entries (SPEC.md line 147) — keep it at 1.15.0 unless it forces a transitive resolution conflict, which would surface in DEPS-15.

**Verification:**
```bash
./gradlew --configuration-cache assembleDebug
./gradlew :app:dependencyInsight --dependency androidx.lifecycle:lifecycle-runtime  # resolves to 2.10.0
./gradlew :app:dependencyInsight --dependency androidx.activity:activity-compose    # resolves to 1.13.0
```

`[CITED: PITFALLS.md §10 dependency convergence — watch for transitive bumps]`.

---

### DEPS-08 — Hilt / coroutines / serialization

**Files touched:** `gradle/libs.versions.toml`.

**Catalog edits:**
```toml
hilt = "2.57.1"
kotlinxCoroutines = "1.11.0"
kotlinxSerialization = "1.9.0"
```

**Pre-flight (recommended, not required):** before this commit, set `ksp.incremental.intermodule=false` **temporarily** if Hilt aggregating processor NPEs appear (PITFALLS §11). The current `gradle.properties:14` has `ksp.incremental=true`; the related `ksp.incremental.intermodule` is not set (defaults to true). If a Hilt NPE bites, the fallback is to add `ksp.incremental=false` to `gradle.properties` and document it as a deferred backlog item. Revert this fallback once a Hilt patch ships.

**Verification (the smoke that catches Hilt aggregating-processor regressions):**
```bash
./gradlew clean :app:kspDebugKotlin       # no NPE in dagger.hilt.processor.internal.aggregateddeps.AggregatedDepsGenerator
./gradlew assembleDebug
# Manual: edit one @HiltViewModel parameter, re-run :app:assembleDebug → incremental build < 15s
adb install -r app/build/outputs/apk/debug/app-debug.apk
# Manual: launch → reach first @HiltViewModel screen → no MissingBindingException at runtime
```

`[CITED: PITFALLS.md §11 Hilt aggregating processor]`. `[CITED: PITFALLS.md §3 KSP/Kotlin drift — already addressed in DEPS-05]`.

---

### DEPS-09 — Apollo 4.4.3, OkHttp 4.12.0 retained

**Files touched:** `gradle/libs.versions.toml`.

**Catalog edits:**
```toml
apollo = "4.4.3"
okhttp = "4.12.0"     # explicit comment: Apollo 5 / OkHttp 5 deferred to NET-01 milestone
```

**Add an inline comment block above `okhttp`:**
```toml
# Apollo 4.4.3 + OkHttp 4.12.0 are explicitly held — Apollo 5 (May 2026) split
# apollo-normalized-cache-sqlite to a separate repo and promoted warnings to
# errors; OkHttp 5 shares the client transitively with Apollo + Media3 + Coil.
# Both are deferred to NET-01 milestone (see .planning/REQUIREMENTS.md).
```

**Verification:**
```bash
./gradlew :app:dependencyInsight --dependency com.squareup.okhttp3:okhttp   # → 4.12.0 from a single source
./gradlew :app:dependencyInsight --dependency com.apollographql.apollo:apollo-runtime  # → 4.4.3
./gradlew --configuration-cache assembleDebug
```

`[CITED: STACK.md — Apollo 5 / OkHttp 5 explicit deferral rationale]`.

---

### DEPS-10 — Media3 conditional (CASE A confirmed)

**Probe result (executed 2026-05-16 14:23 GMT+9):**
```
GET https://repo1.maven.org/maven2/io/github/anilbeesetti/nextlib-media3ext/maven-metadata.xml
→ 200 OK
<latest>1.10.0-0.12.1</latest>
<release>1.10.0-0.12.1</release>
Available 1.10.x: 1.10.0-0.12.1 (released 2026-04-14)
```
`[VERIFIED: Maven Central probe 2026-05-16]`.

**This means CASE A applies.** Catalog edits:
```toml
media3 = "1.10.0"
nextlibMedia3Ext = "1.10.0-0.12.1"
```

The executor **MUST re-run the probe** at execution time to confirm 1.10.0-0.12.1 is still the latest 1.10.x — if a 1.10.1 has shipped in the interim, prefer the newer patch. Record the probe output in the plan execution log.

**If — against expectations — the probe at execution time returns empty (Maven Central outage, repo deletion):** fall through to CASE B:
```toml
# Media3 stays on 1.9.1 — nextlib-media3ext has no 1.10.x build on Maven Central
# as of YYYY-MM-DD (probe at execution time returned empty). Revisit next milestone
# (see .planning/REQUIREMENTS.md "Deferred to Future Milestones").
media3 = "1.9.1"
nextlibMedia3Ext = "1.9.1-0.11.0"
```

**R8 keep-rule check:** existing rules in `app/proguard-rules.pro:17-21` already cover `androidx.media3.**` and `io.github.anilbeesetti.nextlib.**` with broad keep. No delta needed for the Media3 bump itself — but verify the release APK launches the player without `UnsatisfiedLinkError` (PITFALLS §16).

**Verification:**
```bash
./gradlew --configuration-cache assembleDebug
./gradlew :app:assembleBenchmark                          # minified
# Manual: install benchmark APK, open the player on a video with AC3/EAC3 audio → no UnsatisfiedLinkError, AC3 plays
```

`[CITED: PITFALLS.md §16 nextlib-media3ext lockstep]`. `[CITED: STACK.md — Media3 1.10 needs compileSdk 36 — provided by DEPS-04]`.

---

### DEPS-11 — Catalog hygiene

**Files touched:** `gradle/libs.versions.toml`.

**Remove (Room is dead, confirmed via grep — see §3 audit):**
```toml
# DELETE from [versions]:
room = "2.6.1"

# DELETE from [libraries]:
androidx-room-runtime
androidx-room-ktx
androidx-room-paging
androidx-room-compiler
```

**Add (kotlinx-collections-immutable — `[VERIFIED: Maven Central 2026-05-16, latest stable 0.4.0]`):**
```toml
# [versions]
kotlinxCollectionsImmutable = "0.4.0"   # latest stable on Maven Central 2026-05-16; 0.5.0-beta01 exists but stable-only policy

# [libraries]
kotlinx-collections-immutable = { module = "org.jetbrains.kotlinx:kotlinx-collections-immutable", version.ref = "kotlinxCollectionsImmutable" }
```

Note: STACK.md and PROJECT context mention `0.3.8`; the current stable on Maven Central as of 2026-05-16 is **0.4.0** (probed; 0.5.0-beta01 is the only newer entry and violates the stable-only constraint). Use 0.4.0. `[VERIFIED: Maven Central probe 2026-05-16]`.

**Verification:**
```bash
grep -rE 'androidx\.room' /home/yun/slopper/app /home/yun/slopper/core /home/yun/slopper/feature   # → empty
grep -nE '^room|androidx-room' /home/yun/slopper/gradle/libs.versions.toml                         # → empty
./gradlew --configuration-cache assembleDebug                                                       # green
```

---

### DEPS-12 — Detekt + ktlint

**Files touched:** `gradle/libs.versions.toml`, root `build.gradle.kts` (subprojects block at lines ~36–61), `config/detekt-baseline.xml` (regenerated), per-module ktlint baselines (regenerated).

**Catalog edits:**
```toml
detekt = "1.23.8"      # latest 1.x patch — Detekt 2.0 is alpha-only on Kotlin 2.3 (PITFALLS §12)
ktlint = "13.1.0"      # latest stable plugin (verified on plugins.gradle.org 2026-05-16)
```

`[VERIFIED: Maven Central 2026-05-16 — detekt 1.23.8 is the latest 1.x patch]`.
`[VERIFIED: plugins.gradle.org 2026-05-16 — ktlint-gradle 13.1.0 is the latest stable; 13.0.0 also present]`.

**Edit root `build.gradle.kts:48`:** the hardcoded `version.set("1.3.1")` for ktlint runtime needs to bump in lockstep with the plugin. Plugin 13.x defaults to ktlint runtime 1.6.x; recommended explicit setting:
```kotlin
configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    version.set("1.6.0")          // bump from 1.3.1 — matches ktlint plugin 13.1.0
    android.set(true)
    ignoreFailures.set(false)
    // ...
}
```

**Edit root `build.gradle.kts:57`:** detekt `toolVersion`:
```kotlin
toolVersion = "1.23.8"            // was 1.23.7
```

**Regenerate baselines (in this order, after the version bumps land):**
```bash
./gradlew ktlintFormat                            # auto-fix all format churn from Kotlin 2.2 + ktlint 1.6
./gradlew detektBaseline                          # regenerate detekt baseline (per discretion: only baseline net-new findings with documented reason)
# Commit the format churn + new baselines together
```

**Discretion (from CONTEXT.md):** re-baseline ktlint format churn (expected); for detekt, the planner should fail-fast on net-new findings and require a per-finding rationale before baselining.

**Verification:**
```bash
./gradlew ktlintCheck detekt                      # exit 0
test "$(stat -c '%Y' config/detekt-baseline.xml)" -gt "$(git log -1 --format=%ct -- gradle/libs.versions.toml)"   # baseline newer than catalog bump
```

`[CITED: PITFALLS.md §12 — analyzer Kotlin compiler lag]`.

---

### DEPS-13 — dependencyCheck plugin

**Probe result:** `grep -rE 'dependency-check|dependencyCheckAnalyze' build-logic/ /home/yun/slopper/build.gradle.kts` confirms the plugin IS wired:
- `build.gradle.kts:16` — `alias(libs.plugins.dependency.check)`
- `build.gradle.kts:19–30` — `dependencyCheck { ... }` block with `failBuildOnCVSS = 7.0f`, suppression file, analyzer config

`[VERIFIED: grep on /home/yun/slopper 2026-05-16]`.

**Action:** **leave the catalog entry in place** (`dependencyCheck = "11.1.1"`). The plugin runs only when `./gradlew dependencyCheckAnalyze` is explicitly invoked; it does **not** run as part of `check`. Verify AGP 9 / Gradle 9.4.1 compatibility:
```bash
./gradlew dependencyCheckAnalyze --no-configuration-cache
```

**If the plugin breaks on Gradle 9 / AGP 9:** per CONTEXT.md Decision 3 revert-and-continue rule (DEPS-13 is NOT in the lockstep chain) — revert this attempt and add `DEPS-PLUGIN-DEPCHECK` to deferred backlog. The phase continues; the plugin is non-critical for the build green-gate.

**Verification:**
```bash
./gradlew dependencyCheckAnalyze --no-configuration-cache       # exits 0 (or with documented HIGH findings)
./gradlew --configuration-cache assembleDebug                    # config cache still green — depcheck doesn't run here
```

`[CITED: STACK.md — dependencyCheck still incompatible with config cache, keep `--no-configuration-cache`]`.

---

### DEPS-14 — Build green end-to-end

**Files touched:** none directly; this is a verification gate.

**Command (single-shot):**
```bash
./gradlew --configuration-cache assembleDebug assembleRelease assembleBenchmark check
```

**Note on `check`:** depending on what's wired, `check` invokes lint + unit tests + (detekt+ktlint via subprojects block) but **not** `dependencyCheckAnalyze`. Run depcheck separately if a release gate requires it.

**On failure:** failure here is **not** a lockstep stop — DEPS-14 is the verification gate, not a transformation step. A failure at this point means an earlier requirement's commit introduced a regression; planner should specify "git bisect across the phase-1/deps-bump branch to find the offending commit" as the remediation step.

---

### DEPS-15 — Dependency tree clean

**Files touched:** none; this is a review gate. Output saved as audit artifact (suggest `.planning/phases/01-deps-foundation-bump/deps-audit.txt`).

**Command:**
```bash
./gradlew :app:dependencies --configuration releaseRuntimeClasspath > .planning/phases/01-deps-foundation-bump/deps-audit.txt
```

**Review checklist (planner builds this as a sub-step list):**
- [ ] every `androidx.compose.*` resolves to the BOM version (1.11.1 family for BOM 2026.05.00) — `grep 'androidx.compose' deps-audit.txt | sort -u`
- [ ] every `androidx.lifecycle:*` resolves to 2.10.0 — `grep 'androidx.lifecycle' deps-audit.txt | sort -u`
- [ ] no two majors of the same AndroidX artifact (e.g. lifecycle 2.8.7 + 2.10.0 both present)
- [ ] `com.squareup.okhttp3:okhttp` resolves to 4.12.0 from one source only
- [ ] exactly one `compose-compiler` version present (per PITFALLS §4)
- [ ] `:app:dependencyInsight --dependency androidx.activity:activity-compose` → 1.13.0 (not 1.10.x bumped transitively by Compose BOM — PITFALLS §10)

**On finding a skew:** add `strictly("X.Y.Z")` to the catalog entry, document the override in the plan log, re-run the audit.

`[CITED: PITFALLS.md §10]`.

---

### DEPS-16 — Baseline profile regenerated

**Files touched:** `app/src/release/generated/baselineProfiles/baseline-prof.txt` (regenerated).

**Prerequisites (executor pre-flight):**
- Either: a connected 120Hz Pixel via `adb devices` (per `baselineprofile/build.gradle.kts:36` `useConnectedDevices = true`)
- Or: a running emulator (e.g. `Pixel 6 API 34` AVD) — GMD wiring is Phase 3 PERF-01 scope, **NOT** Phase 1.

**Command (canonical AGP-9 baseline-profile plugin task):**
```bash
./gradlew :app:generateReleaseBaselineProfile
```

(The plugin also registers `:app:generateBaselineProfile` as a non-variant-specific alias; either is acceptable.)

**Output:** the plugin writes to `app/src/release/generated/baselineProfiles/baseline-prof.txt`. SPEC.md acceptance is that this file's last-modified commit is at-or-after the Compose BOM bump (DEPS-06) commit.

**Verification:**
```bash
./gradlew :app:generateReleaseBaselineProfile
git add app/src/release/generated/baselineProfiles/baseline-prof.txt
# Commit DEPS-16 with the regenerated file
test "$(git log -1 --format=%ct -- app/src/release/generated/baselineProfiles/baseline-prof.txt)" \
  -ge "$(git log -1 --format=%ct -- gradle/libs.versions.toml)"
```

**On variance / flake:** PITFALLS §6 — a stale profile is worse than no profile. If regen produces a file but cold-start macrobench shows <5% delta vs no-profile, the profile is dead and needs a proper journey expansion — but that's PERF-05 scope, not DEPS-16. For DEPS-16, freshness of the file (not its quality) is the acceptance criterion.

`[CITED: PITFALLS.md §6 stale baseline profile]`. `[ASSUMED: AGP 9.2's `androidx.baselineprofile` plugin 1.3.3 exposes `:app:generateReleaseBaselineProfile` — confirm task name with `./gradlew :app:tasks --group="Baseline Profile"` at execution time]`.

---

## 3. Version-Availability Verification Log

Executed at 2026-05-16 14:22–14:23 GMT+9. All probes via `curl -sf` against Maven Central / plugins.gradle.org.

| Artifact / Plugin | Probed | Latest stable found | Decision |
|---|---|---|---|
| `io.github.anilbeesetti:nextlib-media3ext` | `repo1.maven.org/.../maven-metadata.xml` | **1.10.0-0.12.1** (released 2026-04-14) | **CASE A** — bump Media3 to 1.10.0, nextlib to 1.10.0-0.12.1 |
| `com.google.devtools.ksp:symbol-processing-api` for Kotlin 2.2.20 | `repo1.maven.org/.../maven-metadata.xml` | **2.2.20-2.0.4** | use `ksp = "2.2.20-2.0.4"` |
| `org.jetbrains.kotlin.plugin.compose` for Kotlin 2.2.20 | (transitive — catalog `kotlin-compose` already `version.ref = "kotlin"` per `libs.versions.toml:133`) | matches `kotlin` automatically | no separate entry to bump |
| `io.gitlab.arturbosch.detekt:detekt-gradle-plugin` | `repo1.maven.org/.../maven-metadata.xml` | **1.23.8** is latest 1.x (2.0.0+ is alpha-only on Kotlin 2.3) | use `detekt = "1.23.8"` |
| `org.jlleitschuh.gradle.ktlint` plugin | `plugins.gradle.org/m2/.../maven-metadata.xml` | **13.1.0** (13.0.0 also present; 12.3.0 is highest 12.x) | use `ktlint = "13.1.0"`; underlying ktlint runtime → 1.6.0 |
| `org.jetbrains.kotlinx:kotlinx-collections-immutable` | `repo1.maven.org/.../maven-metadata.xml` | **0.4.0** (latest stable; 0.5.0-beta01 violates stable-only) | use `kotlinxCollectionsImmutable = "0.4.0"` |
| `androidx.compose:compose-bom 2026.05.00` | not re-probed (per SPEC.md lock; STACK.md HIGH-confidence) | — | use `composeBom = "2026.05.00"` |

`[VERIFIED: all rows tagged "probed" — Maven Central / plugins.gradle.org 2026-05-16]`.

### `-Xcontext-receivers` / `-Xskip-metadata-version-check` grep audit

```
$ grep -rnE '(-Xcontext-receivers|-Xskip-metadata-version-check)' --include='*.kts' --include='*.kt' /home/yun/slopper
build-logic/convention/src/main/kotlin/io/stashapp/android/buildlogic/KotlinAndroid.kt:38: "-Xcontext-receivers",
build-logic/convention/src/main/kotlin/io/stashapp/android/buildlogic/KotlinAndroid.kt:44: "-Xskip-metadata-version-check",
```

**Both flags appear only in `KotlinAndroid.kt`. No `context(Foo)` syntax in `core/*` or `feature/*` source.** DEPS-05 deletes the flags outright; no call-site migration. `[VERIFIED: grep 2026-05-16]`.

### Room dead-code audit

```
$ grep -rE 'androidx\.room' /home/yun/slopper/app /home/yun/slopper/core /home/yun/slopper/feature
(no output)
```

Zero hits. The four Room library entries + the `room` version pin in `libs.versions.toml` are removed in DEPS-11. `[VERIFIED: grep 2026-05-16]`.

### dependencyCheck plugin wiring audit

```
$ grep -rE 'dependency-check|dependencyCheckAnalyze' build-logic/ /home/yun/slopper/build.gradle.kts
build.gradle.kts:16:    alias(libs.plugins.dependency.check)
(plus the configuration block dependencyCheck { ... } in lines 19-30)
```

**Plugin IS wired.** DEPS-13 leaves the catalog entry; treats AGP-9 breakage (if any) as revert-and-continue. `[VERIFIED: grep 2026-05-16]`.

---

## 4. R8 Keep-Rule Audit Checklist

Append-only against the existing `app/proguard-rules.pro`. Existing rules cover Apollo, Hilt, Media3 (broad), kotlinx.serialization (companion + serializer), Compose runtime, coroutines volatile fields, OkHttp dontwarn, core.model + core.domain. The deltas below are the **net-new** rules surfaced by upgrade targets.

| Library / version | Existing rules suffice? | New rule(s) to append | Source |
|---|---|---|---|
| AGP 9.2.0 wildcard semantics | **Audit required** — AGP 9 changed how `-keepattributes` wildcards treat runtime-invisible annotations | If R8 release-mode emits warnings about `RuntimeInvisibleAnnotations` referenced from kept classes, add `-keepattributes *Annotation*,RuntimeInvisibleAnnotations,RuntimeInvisibleParameterAnnotations,RuntimeInvisibleTypeAnnotations` (broad but conservative) | `[CITED: Android Developers — Configure and troubleshoot R8 Keep Rules, Nov 2025]` `[CITED: PITFALLS.md §9]` |
| kotlinx.serialization 1.9.0 | **Likely yes**, but verify with R8 warning scan — 1.9.0 revised its bundled rules and consumer-provided rules can conflict | Run `./gradlew :app:assembleRelease -Pandroid.enableR8.fullMode=true 2>&1 | tee r8-warnings.txt` and read every warning. If `Missing class kotlinx.serialization.KSerializer (referenced from: ...)` appears, the existing companion-keep rule needs `-keep,allowobfuscation @kotlinx.serialization.Serializable class * { *; }` added | `[CITED: github.com/Kotlin/kotlinx.serialization#3033]` `[CITED: github.com/Kotlin/kotlinx.serialization#2392]` |
| Apollo 4.4.3 | **Yes** — existing `-keep class io.stashapp.android.graphql.** { *; }` is broad enough; Apollo's own `consumer-rules.pro` covers runtime | none — but **consider narrowing** in a future POLISH milestone (the broad keep defeats some shrinking) | `[ASSUMED: Apollo 4.x consumer rules unchanged in 4.4.3 — verify by checking apollo-runtime AAR's consumer-rules.pro at execution time]` |
| Hilt 2.57.1 | **Yes** — existing `-keepnames class dagger.hilt.internal.aggregatedroot.codegen.*` + HiltViewModel-targeted rules are correct | none unless new `@HiltViewModel` subclasses are added in DEPS scope (they aren't) | `[ASSUMED: Hilt 2.57.1 consumer rules unchanged since 2.50 — verify]` |
| Media3 1.10.0 | **Yes** — existing broad keep `-keep class androidx.media3.** { *; }` + `io.github.anilbeesetti.nextlib.** { *; }` covers reflective loading | none — but watch for `UnsatisfiedLinkError` on player smoke (PITFALLS §16) | `[CITED: app/proguard-rules.pro:17-21]` |
| Compose BOM 2026.05.00 (runtime 1.11.1) | **Yes** — existing `-keep class androidx.compose.runtime.** { *; }` covers it | none expected; Compose runtime is reflective-free in production code | `[CITED: app/proguard-rules.pro:36]` |
| OkHttp 4.12.0 (retained) | **Yes** — existing `-dontwarn` block is correct | none | `[CITED: app/proguard-rules.pro:42-45]` |
| Coil 3.4.0 | **Yes** — Coil has no reflection-based class loading | none | `[ASSUMED: Coil 3.x ships its own consumer-rules.pro]` |

**Audit procedure (DEPS-04 plan step):**
```bash
./gradlew :app:assembleRelease -Pandroid.enableR8.fullMode=true 2>&1 | tee /tmp/r8-warnings.txt
grep -E '(Missing class|Warning:.*keep)' /tmp/r8-warnings.txt
# If non-empty: append targeted keep rule, re-run; do NOT use blanket `-keep class **` (PITFALLS technical-debt table)
```

**Smoke test (mandatory):** install the benchmark APK (`:app:assembleBenchmark` — minified, debug-signed, profileable) and reach the first `@HiltViewModel` screen. No `MissingBindingException`, no `SerializationException: Polymorphic serializer was not found`, no `UnsatisfiedLinkError`.

---

## 5. Smoke-Test Command Matrix

Per CONTEXT.md Claude's-discretion verification cadence:

| Trigger | Command | Purpose |
|---|---|---|
| After every per-req commit (DEPS-01..DEPS-16) | `./gradlew --configuration-cache assembleDebug` | Cheap green-gate; catches compile failures and config-cache regressions |
| After DEPS-04 (AGP/SDK bump) | `./gradlew --configuration-cache assembleDebug assembleRelease assembleBenchmark check` | Step-group gate — first time release + benchmark variants exercise the new toolchain |
| After DEPS-06 (Compose BOM) | same as above + `./gradlew :app:compileReleaseKotlin` | PITFALLS §17 deprecation warning surface |
| After DEPS-13 (last lib bump) | same as DEPS-04 + `./gradlew dependencyCheckAnalyze --no-configuration-cache` | Final library-level gate |
| Pre-DEPS-16 | `./gradlew --configuration-cache assembleDebug assembleRelease assembleBenchmark check` | Confirms baseline-profile regen runs against green build |
| Phase-close (post-DEPS-16) | full matrix above + `./gradlew :app:dependencies --configuration releaseRuntimeClasspath > deps-audit.txt` + manual review | DEPS-15 final dependency-tree audit |

**Per-commit (cheap):** `./gradlew --configuration-cache assembleDebug` — single-shot, configuration-cache-aware, exits in < 60s on incremental.

**Step-group gate (expensive):** the full matrix. Runs at DEPS-04, DEPS-06, DEPS-13 commit boundaries and once more before DEPS-16.

**Phase gate:** SPEC.md acceptance — `./gradlew --configuration-cache assembleDebug assembleRelease assembleBenchmark check` exits 0 on a clean clone.

---

## 6. Open Risks — Treat as "Failures & Reverts" candidates per CONTEXT.md Decision 3

Per Decision 3: DEPS-01..06 are **lockstep** (stop the phase on failure); DEPS-07..16 are **independent** (revert-and-continue). Risks below mapped to that scheme:

| # | Risk | Phase impact | Likely failure mode | Phase rule |
|---|---|---|---|---|
| R1 | `dependencyCheck` plugin breaks on Gradle 9 / AGP 9 | DEPS-13 | Plugin fails to load; `./gradlew tasks` throws | Revert-and-continue; defer plugin upgrade to a future maintenance task |
| R2 | Detekt 1.23.8 / ktlint 13.1.0 reject Kotlin 2.2.20 surface | DEPS-12 | `./gradlew detekt` throws `PsiInvalidElementAccessException` or false-positives | Revert-and-continue; document in deferred backlog; the bump is non-critical for build green |
| R3 | Hilt 2.57.1 aggregating-processor NPE on KSP 2.2.20-2.0.4 | DEPS-08 | NPE in `dagger.hilt.processor.internal.aggregateddeps.AggregatedDepsGenerator` | First fallback: set `ksp.incremental=false` in `gradle.properties` and re-attempt (document); if still failing, revert-and-continue |
| R4 | nextlib 1.10.0-0.12.1 ABI mismatch surfaces only at runtime | DEPS-10 | `UnsatisfiedLinkError: dlopen failed` on player open | Revert-and-continue; fall back to CASE B (Media3 1.9.1) and document |
| R5 | Compose BOM 2026.05.00 breaks compilation via API rename (PITFALLS §17) | DEPS-06 | `Unresolved reference: ScaleToBounds` or similar | **Lockstep — stop the phase.** DEPS-07..16 all assume the BOM bump is clean |
| R6 | Kotlin 2.2.20 K1→K2 surface incompatibility | DEPS-05 | `compileDebugKotlin` fails on a `context(Foo)` syntax we missed | **Lockstep — stop the phase.** But: grep audit (§3) shows zero `context()` call sites, so risk is near-zero in this codebase |
| R7 | AGP 9 + R8 wildcard semantic change breaks release minification | DEPS-04 | `assembleRelease` succeeds, but installed APK crashes at runtime in the first `@HiltViewModel` screen | **Lockstep — stop the phase.** Surface via `:app:assembleBenchmark` smoke (smoke matrix §5 step-group gate) before claiming DEPS-04 done |
| R8 | Baseline profile regen requires connected device, none plugged in | DEPS-16 | `:app:generateReleaseBaselineProfile` fails with "No connected devices" | Not a failure of the bump itself — executor pre-flight requirement. Planner should document the prerequisite in the DEPS-16 plan step |
| R9 | Detekt finds genuinely new semantic issues post-Kotlin-2.2 (not formatting churn) | DEPS-12 | `./gradlew detekt` exits non-zero with new findings | Per discretion: fail-fast and require per-finding rationale before baselining. Does NOT trigger revert — handled as in-flight fix |
| R10 | `targetSdk = 36` triggers new Lint deprecation warnings | DEPS-04 | `./gradlew lint` reports new warnings | Expected — DEPS-01 baseline absorbs them. POLISH-02 shrinks the baseline in Phase 4 |

**Plan log template addition (per Decision 3):** every plan MUST include a `## Failures & Reverts` section structured as:

```markdown
## Failures & Reverts

| Req ID | Attempted version | Failure mode | Resolution | Deferred? |
|--------|-------------------|--------------|------------|-----------|
| (rows added at execution time) | | | | |
```

---

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|---|---|---|
| A1 | AGP-9 built-in Kotlin is opt-in for existing projects, not auto-applied — keep explicit `apply("org.jetbrains.kotlin.android")` in convention plugins | §DEPS-04 (optional plugin cleanup) | Low — leaving explicit plugin is a no-op even if AGP auto-applies; only matters if a strict deprecation warning is emitted |
| A2 | nextlib 1.10.0-0.12.1 is built against Kotlin ≤ 2.2.x, so `-Xskip-metadata-version-check` can be removed safely | §DEPS-05 | Medium — if nextlib ships against Kotlin 2.3, compile will warn or fail; fallback is to re-add the flag temporarily, document, continue |
| A3 | `:app:generateReleaseBaselineProfile` is the correct task name under AGP 9.2 baseline-profile plugin 1.3.3 | §DEPS-16 | Low — verify with `./gradlew :app:tasks --group="Baseline Profile"` at execution time; the alias `:app:generateBaselineProfile` also exists |
| A4 | Apollo 4.4.3 / Hilt 2.57.1 / Coil 3.4.0 consumer-rules.pro files cover their reflection needs without delta keep rules | §4 R8 audit | Medium — surfaced by reading R8 warnings during `assembleRelease`; mitigation is targeted append, not blanket keep |
| A5 | Baseline-profile plugin 1.3.3 supports the AGP-9 build cleanly without bumping to 1.4.x (beta) | §DEPS-16 / catalog | Medium — if not, revert-and-continue per Decision 3 falls outside lockstep; defer profile regen to a post-phase patch |

`[ASSUMED]` rows here flag where execution may need to confirm against live behavior. None of these block planning — they shape recovery branches.

---

## Project Constraints (from CLAUDE.md / project skills)

Project-global rules from `~/.claude/rules/*.md` that the planner must honor:

- **No mutation** — N/A for build-file edits, but applies to any Kotlin source touched during deprecation cleanup
- **Conventional commit format** — already matched by CONTEXT.md Decision 1 (`chore(deps): <req-id> — <desc>`)
- **No hardcoded secrets** — N/A for this phase; release keystore handling unchanged
- **Test coverage 80%** — N/A for DEPS phase (test infrastructure is POLISH scope)
- **No `console.log`** — N/A (Android / Kotlin codebase)
- **80% min test coverage** — N/A for DEPS phase; explicitly deferred to POLISH per ROADMAP

CLAUDE.md's "Pull Request Workflow" maps directly to CONTEXT.md Decision 1 — one PR at phase end against `master`, scrolled commit-by-commit.

---

## Sources

### Primary (HIGH confidence — direct probe / official docs)
- Maven Central `nextlib-media3ext` metadata — probed 2026-05-16, 1.10.0-0.12.1 confirmed
- Maven Central KSP `symbol-processing-api` metadata — probed 2026-05-16, 2.2.20-2.0.4 confirmed
- Maven Central `kotlinx-collections-immutable` metadata — probed 2026-05-16, 0.4.0 confirmed
- Maven Central `detekt-gradle-plugin` metadata — probed 2026-05-16, 1.23.8 confirmed
- plugins.gradle.org `ktlint-gradle` metadata — probed 2026-05-16, 13.1.0 confirmed
- `.planning/research/STACK.md` — exhaustive target-version table and rationale (HIGH-cited throughout)
- `.planning/research/PITFALLS.md` — 18 pitfalls with phase mapping (HIGH-cited throughout)
- AGP 9.2.0 release notes — https://developer.android.com/build/releases/agp-9-2-0-release-notes (cited in CONTEXT.md)
- Compose BOM mapping — https://developer.android.com/develop/ui/compose/bom/bom-mapping (cited in CONTEXT.md)
- R8 Keep Rules guidance (Nov 2025) — https://android-developers.googleblog.com/2025/11/configure-and-troubleshoot-r8-keep-rules.html

### Codebase (HIGH confidence — direct file reads)
- `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties`, `gradle.properties`
- All 6 files under `build-logic/convention/src/main/kotlin/io/stashapp/android/buildlogic/`
- `app/build.gradle.kts`, `app/proguard-rules.pro`
- `baselineprofile/build.gradle.kts`
- Root `build.gradle.kts` and `settings.gradle.kts`
- Grep audits on `app/`, `core/`, `feature/` for room, context-receivers, dependency-check

### Secondary (cross-referenced)
- `.planning/codebase/STACK.md` — current-in-repo state
- `.planning/REQUIREMENTS.md` — DEPS-01..14 mapping
- `.planning/ROADMAP.md` — Phase 1 verification gate

---

## Metadata

**Confidence breakdown:**
- Per-requirement recipes (DEPS-01..16): **HIGH** — every recipe has exact file paths, exact edits, and command-checkable verification
- Version-availability log: **HIGH** — all critical versions probed live against authoritative registries
- R8 keep-rule deltas: **MEDIUM** — append-only audit checklist with execution-time confirmation step; absolute keep rules will be set only after reading R8 warning output during DEPS-04
- Smoke-test matrix: **HIGH** — directly drawn from CONTEXT.md Claude's-discretion verification cadence
- Failures & reverts mapping: **HIGH** — every risk mapped to lockstep-vs-independent rule per CONTEXT.md Decision 3

**Research date:** 2026-05-16
**Valid until:** 2026-06-15 (30 days — toolchain ecosystem is stable but Maven Central freshness probes should be re-run if execution slips)
