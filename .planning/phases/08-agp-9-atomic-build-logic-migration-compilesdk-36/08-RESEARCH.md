# Phase 8: AGP-9 — Atomic Build-Logic Migration + compileSdk 36 - Research

**Researched:** 2026-05-30
**Domain:** AGP 8.7.3→9.2.1 / Gradle 8.11.1→9.4.1 / built-in-Kotlin DSL migration of a convention-plugin multi-module Compose app (Slopper)
**Confidence:** HIGH (every version live-verified against Google Maven / Maven Central metadata; every DSL mechanic confirmed against the AGP 9.0 release notes, the Android built-in-Kotlin migration guide, AND the Now-in-Android `main` branch which already runs the identical `configureKotlinAndroid(CommonExtension)` pattern on AGP 9 + compileSdk 36)

---

<user_constraints>
## User Constraints (from CONTEXT.md / STATE.md)

### Locked Decisions
- **D-01 / one-concern-per-commit order** (bisectability): (1) Gradle wrapper 8.11.1→9.4.1 + fresh `distributionSha256Sum` fetched live, (2) AGP 8.7.3→9.2.1, (3) drop `org.jetbrains.kotlin.android` from all sites, (4) `CommonExtension<*,…>` generics fix in `KotlinAndroid.kt:18`, (5) `kotlinOptions{}`→`kotlin{compilerOptions{}}` in `baselineprofile/build.gradle.kts:15`, (6) Hilt 2.56.2→2.59.2, (7) compileSdk 35→36. Build is green only at the END of the sequence.
- **D-02:** Validate `./gradlew :core:common:compileDebugKotlin` after the AGP + built-in-Kotlin + generics commits, before a full build.
- **D-03:** AGP = `9.2.1` (fall back to `9.2.0` only if `9.2.1` unpublished). Gradle floor satisfied by `9.4.1`.
- **D-04:** Hilt/Dagger = `2.59.2` exactly — never bare `2.59+`. Lands strictly after AGP 9.
- **D-04b:** Kotlin `2.2.20` / KSP `2.2.20-2.0.4` UNCHANGED.
- **D-05a:** `targetSdk = 35` MUST stay explicit in all 3 sites (`AndroidApplicationConventionPlugin.kt:17`, `AndroidLibraryConventionPlugin.kt:18`, `baselineprofile/build.gradle.kts:21`). Only `compileSdk` moves to 36.
- **D-05b:** Add a grep-checkable verification step asserting `targetSdk = 35` in every site and `compileSdk = 36` in both compileSdk sites.
- **D-06:** Remove `org.jetbrains.kotlin.android` from all 5 sites (root `build.gradle.kts:5`, both convention plugins, `baselineprofile/build.gradle.kts:3`, catalog alias `libs.versions.toml:150`).
- **D-06b:** KEEP `kotlin.compose` and `kotlin.serialization` plugins.
- **D-07:** Bump CI cache key token `agp8`→`agp9` at `.github/workflows/ci.yml:39` and `:41`.
- **D-08:** Phase gate: `./gradlew compileDebugSources detekt ktlintCheck test` green across ALL modules with version-isolation opt-out flags REMOVED. detekt 1.23.8 kept; if it hard-fails, planned-deviation fallback — do NOT pre-bump to `2.0.0-alpha`.
- **D-09 (forbidden crutches):** Do NOT add `android.enableLegacyVariantApi=true` or `android.newDsl=false`.

### Claude's Discretion
- Exact commit messages and which sub-edits group into each D-01 commit (bisect invariant must hold).
- Whether the `CommonExtension` generics fix is a changed type-parameter list, a type alias, or a star-projection adjustment.
- Whether to run the full gate once at the end or also after the Hilt commit.

### Deferred Ideas (OUT OF SCOPE)
- `settings.gradle.kts` duplicate `include(":baselineprofile")` cleanup (lines 38 & 50) — deferred.
- Media3 1.10.0 / nextlib pair + activity-compose 1.13 + core-ktx 1.18 — **Phase 9**.
- CI assemble/signing probe + EdEC/bcprov spike — **Phase 10**.
- DEPS-07 lint re-enables in `KotlinAndroid.kt` — deferred; only revisit if AGP-9 breaks the gate.
- No Kotlin/KSP bump, no targetSdk bump, no new dependency/feature, no module-graph restructure.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| AGP9-02 | AGP→9.2.1; `build-logic` migration complete (drop `kotlin.android`, remove `CommonExtension<*,…>` generics, `kotlinOptions{}`→`kotlin{compilerOptions{}}`); all ~14 modules configure, `compileDebugSources` green | §Standard Stack (AGP 9.2.1 live-verified), §Pattern 1 (bare `CommonExtension`), §Pattern 2 (`KotlinAndroidProjectExtension` STILL resolves under built-in Kotlin — NiA proof), §Pattern 3 (the 5 removal sites) |
| AGP9-03 | Hilt/Dagger→2.59.2 exactly; Kotlin 2.2.20 / KSP 2.2.20-2.0.4 satisfy AGP-9 KGP floor; working DI graph | §Standard Stack (Hilt 2.59.2 = `<latest>`/`<release>` live-verified; KGP 2.2.10 floor; 2.59 drops AGP 8 → lands after AGP commit) |
| SDK-01 | `compileSdk`→36 in all 3 touchpoints; `targetSdk = 35` preserved explicit in every site (no silent Android-16 opt-in); build green | §Pitfall 1 (`defaultTargetSdkToCompileSdkIfUnset` false→true confirmed), §Pattern 4, §Validation (grep guard) |
| AGP9-01 (folded fwd) | Green-on-Gradle-9 asserted at this gate; wrapper flip = commit 1 | §Pattern 0 (wrapper flip + live sha256), §Environment Availability |
</phase_requirements>

## Summary

Every flagged version disagreement is now resolved against **live registry metadata** (2026-05-30): **AGP = `9.2.1`** (Google Maven lists `…9.2.0, 9.2.1`; 9.3.0 is alpha-only — `9.2.1` is the newest stable 9.2 patch, POM HTTP 200) and **Hilt/Dagger = `2.59.2`** (Maven Central `<latest>2.59.2</latest>` / `<release>2.59.2</release>`; `hilt-android-2.59.2.pom` HTTP 200; no 2.59.3/2.60 exists). AGP 9.2.x's hard **Gradle floor is 9.4.1**, exactly the pinned wrapper target — no higher Gradle needed.

The two highest-uncertainty DSL questions both resolve cleanly and favorably, proven by the **Now-in-Android `main` branch**, which runs the *byte-for-byte identical* `configureKotlinAndroid(commonExtension: CommonExtension)` choke-point pattern on AGP 9.0.0 + `compileSdk = 36`: (Q3) `CommonExtension` **lost all its generic type parameters in AGP 9.0** — the fix is to replace `CommonExtension<*, *, *, *, *, *>` with the **bare `CommonExtension`** in `KotlinAndroid.kt:18` (one token change, imports unchanged); and (Q4) **`org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension` STILL exists and is STILL the extension registered by AGP-9 built-in Kotlin** — `extensions.configure<KotlinAndroidProjectExtension> { jvmToolchain(17); compilerOptions { … } }` in `KotlinAndroid.kt:47` needs **no rewrite** once `kotlin.android` is removed; the imports `JvmTarget` + `KotlinAndroidProjectExtension` keep resolving. The only true `kotlinOptions→compilerOptions` migration is the one lagging site, `baselineprofile/build.gradle.kts:15`.

The targetSdk landmine is confirmed: AGP 9.0 flips `android.sdk.defaultTargetSdkToCompileSdkIfUnset` **false→true**, so any site that omits `targetSdk` silently inherits `compileSdk = 36` (a forbidden Android-16 runtime opt-in). All 3 sites already set `targetSdk = 35` explicitly today — the job is to **keep** them and add a grep guard. detekt 1.23.8's Gradle-9 risk is narrower than feared: its known hard-failure (#8865) is a **Kotlin-2.3.0 metadata** error (detekt 1.23.8 bundles compiler 2.0.0, reads metadata ≤ 2.1.0); Slopper is pinned at **Kotlin 2.2.20 and is NOT bumping**, and its detekt config runs **without type resolution** (no `jvmTarget`/classpath wiring), so the metadata-read path is not exercised — detekt is expected to run, with the empirical confirmation happening at this phase's first green build per the ADR.

**Primary recommendation:** Execute D-01's 7-commit sequence as written. Commit 4 = change exactly one token (`CommonExtension<*,*,*,*,*,*>` → `CommonExtension`); the Kotlin compiler-options block above it is correct as-is. Merge-risk note: commits 2+3 are not independently green (Pitfall S1) but are independently *bisectable* — keep them separate per D-01; the "already registered" error is diagnostic, not corrupting.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Toolchain version resolution (AGP/Gradle/Hilt/KSP) | Build system (Gradle wrapper + `libs.versions.toml`) | — | Single source of truth; the version-catalog edits drive every module |
| Per-module Android config (compileSdk, targetSdk, compileOptions, lint) | `build-logic/convention` choke point (`configureKotlinAndroid`) | `baselineprofile/build.gradle.kts` (the one out-of-band module) | NiA-lineage convention plugin; one edit configures all ~14 modules — and one mistake fails all of them |
| Kotlin compiler options (jvmTarget, freeCompilerArgs, toolchain) | `KotlinAndroidProjectExtension` (KGP, still registered under AGP-9 built-in Kotlin) | `baselineprofile` module's `kotlin{compilerOptions{}}` | Built-in Kotlin keeps the KGP extension type; only the *plugin application* is removed |
| DI codegen (Hilt + KSP) | `AndroidHiltConventionPlugin` + catalog `hilt` ref | KSP plugin (unchanged) | Single catalog bump covers `hilt-android` + `hilt-compiler` + the Hilt plugin |
| Static-analysis gate (detekt/ktlint) | Root `build.gradle.kts` `subprojects{}` block | catalog `detekt`/`ktlint` refs | detekt `toolVersion`/ktlint `version` are pinned **in the root build script**, not only the catalog — see Pitfall 5 |

## Standard Stack

### Core toolchain — live-verified 2026-05-30

| Key | FROM | TO | Verification | Confidence |
|-----|------|----|--------------|------------|
| `agp` (`libs.versions.toml:3`) | `8.7.3` | **`9.2.1`** | Google Maven `maven-metadata.xml` lists `9.0.0,9.0.1,9.1.0,9.1.1,9.2.0,9.2.1` then `9.3.0-alpha…`; `gradle-9.2.1.pom` → **HTTP 200** `[VERIFIED: dl.google.com Google Maven]` | HIGH |
| Gradle wrapper | `8.11.1` | **`9.4.1`** | AGP 9.2.x min Gradle = **9.4.1** (release notes); `gradle-9.4.1-bin.zip` → HTTP 307 (CDN), `.sha256` → 301 redirect (fetch-at-exec, do not hardcode) `[VERIFIED: services.gradle.org]` + `[CITED: developer.android.com/build/releases/agp-9-2-0-release-notes]` | HIGH |
| `hilt` (`libs.versions.toml:6`) | `2.56.2` | **`2.59.2`** | Maven Central `<latest>2.59.2</latest>` / `<release>2.59.2</release>`; versions `2.59, 2.59.1, 2.59.2`; `hilt-android-2.59.2.pom` → **HTTP 200** `[VERIFIED: repo1.maven.org Maven Central]` | HIGH |
| `kotlin` (KGP) | `2.2.20` | **unchanged** | AGP 9.0 runtime-dep KGP floor = **2.2.10**; 2.2.20 > 2.2.10 `[CITED: agp-9-0-0-release-notes]` | HIGH |
| `ksp` | `2.2.20-2.0.4` | **unchanged** | Locked to Kotlin 2.2.20 exactly; AGP auto-upgrades KSP < `2.2.10-2.0.2` only — 2.2.20-2.0.4 is above `[CITED: agp-9-0-0-release-notes]` | HIGH |
| `compileSdk` | `35` | **`36`** (2 sites) | AGP 9.2 supports up to API **36.1**; Build-Tools 36.0.0 min/default `[CITED: agp-9-2-0-release-notes]` | HIGH |
| `targetSdk` | `35` | **HOLD 35** (3 sites) | `defaultTargetSdkToCompileSdkIfUnset` flips false→true in 9.0 — must stay explicit `[CITED: agp-9-0-0-release-notes]` | HIGH |

**No installation step** — all edits are version-catalog + build-script changes. No `npm`/`pip` analog.

**Sha256 mandate (commit 1):** fetch at execution, never hardcode (threat T-07.2-01):
```bash
./gradlew wrapper --gradle-version 9.4.1 \
  --gradle-distribution-sha256-sum "$(curl -sSL https://services.gradle.org/distributions/gradle-9.4.1-bin.zip.sha256)"
# keep validateDistributionUrl=true, networkTimeout=10000 unchanged
```
(The `.sha256` URL 301-redirects through Cloudflare; `curl -sSL` follows it. The current pin `f397b287…151c6` is the 8.11.1 hash and MUST be replaced.)

### Alternatives Considered
| Instead of | Could Use | Tradeoff | Verdict |
|------------|-----------|----------|---------|
| AGP 9.2.1 | 9.2.0 | Only if 9.2.1 unpublished — it IS published (HTTP 200) | Use **9.2.1** (D-03) |
| Gradle 9.4.1 | 9.5.1 | 9.5.1 exists but 9.4.1 is the exact AGP-9.2 floor | **9.4.1** — minimal-surface policy (ADR) |
| Hilt 2.59.2 | 2.59.0/2.59.1 | 2.59.0 ships broken `ComponentTreeDeps` (Dagger #5099); 2.59.1 still pre-jetifier-fix | **2.59.2** (D-04) |
| detekt 1.23.8 | 2.0.0-alpha | alpha forbidden by stable-only policy | **keep 1.23.8** (D-08); fallback in §Pitfall 6 |

## Architecture Patterns

### System Architecture Diagram (build-config dataflow — what breaks where)

```
gradle/wrapper/gradle-wrapper.properties ──[commit 1]──► Gradle 9.4.1 runtime
        │ (AGP 9 hard-fails Gradle-version check if < 9.4.1)
        ▼
gradle/libs.versions.toml  agp=9.2.1 ──[commit 2]──► AGP 9 plugin classpath
        │                                            │ enables built-in Kotlin (registers `kotlin` ext)
        │                                            │ removes CommonExtension generics + legacy variant API
        ▼                                            ▼
  [commit 3] drop org.jetbrains.kotlin.android ◄── else "extension 'kotlin' already registered"
   • root build.gradle.kts:5  • both convention plugins  • baselineprofile:3  • catalog alias:150
        ▼
  build-logic/convention/.../KotlinAndroid.kt  (THE CHOKE POINT — configures all ~14 modules)
   ├─[commit 4]─ CommonExtension<*,*,*,*,*,*> → CommonExtension   (generics removed in AGP 9.0)
   ├─ configure<KotlinAndroidProjectExtension>{ compilerOptions{} }  ← UNCHANGED (ext still resolves)
   └─[commit 7]─ compileSdk = 35 → 36
        ▼
  AndroidApplication/Library ConventionPlugin  →  defaultConfig.targetSdk = 35  ← KEEP EXPLICIT (D-05)
        ▼
  baselineprofile/build.gradle.kts  (out-of-band — NOT through convention plugin)
   ├─[commit 3]─ drop alias(libs.plugins.kotlin.android)
   ├─[commit 5]─ kotlinOptions{ jvmTarget="17" } → kotlin{ compilerOptions{ jvmTarget.set(JvmTarget.JVM_17) } }
   ├─[commit 7]─ compileSdk = 35 → 36     └─ targetSdk = 35  ← KEEP EXPLICIT
        ▼
  gradle/libs.versions.toml  hilt=2.59.2 ──[commit 6]──► Hilt plugin + hilt-android + hilt-compiler
   (2.59 DROPS AGP 8 → must land AFTER commit 2)
        ▼
  .github/workflows/ci.yml:39,41  agp8 → agp9  (stale AGP-8 Gradle-home cache poisons AGP-9 config)
        ▼
  GATE: ./gradlew compileDebugSources detekt ktlintCheck test   (all ~14 modules green)
```

### Pattern 0: Wrapper flip + live sha256 (commit 1 — folded forward from Phase 7)
**What:** Flip `distributionUrl` to `gradle-9.4.1-bin.zip` and re-pin `distributionSha256Sum` fetched live. **This commit is NOT green in isolation** — AGP 8.7.3 hard-fails the Gradle-version check on Gradle 9 (ADR 0001, HIGH confidence). It is committed separately for bisect, then immediately followed by commit 2.

### Pattern 1: `CommonExtension` generics removed (commit 4) — THE Q3 answer
**What:** AGP 9.0 **removed all type parameters from `com.android.build.api.dsl.CommonExtension`**. The new arity is **zero** — it is a plain non-generic type.
```kotlin
// Source: developer.android.com/build/releases/agp-9-0-0-release-notes
//         + android/nowinandroid main branch (live, AGP 9.0.0)
// BEFORE (Slopper KotlinAndroid.kt:17-18):
internal fun Project.configureKotlinAndroid(
    commonExtension: CommonExtension<*, *, *, *, *, *>,   // ← AGP 8 (6 star-projections)
) { … }

// AFTER (AGP 9):
internal fun Project.configureKotlinAndroid(
    commonExtension: CommonExtension,                     // ← bare, no generics
) { … }
```
`configureKotlinAndroid(this)` stays callable from both `ApplicationExtension` and `LibraryExtension` because both still extend the (now non-generic) `CommonExtension`. The `import com.android.build.api.dsl.CommonExtension` is unchanged. **`AndroidComposeConventionPlugin.kt:16`** (`extensions.getByType(CommonExtension::class.java)`) is unaffected — class literal, no generics — but re-verify at build time.
**Verification (NiA `main`, live):** signature is literally `configureKotlinAndroid(commonExtension: CommonExtension)` on AGP 9.0.0. `[VERIFIED: raw.githubusercontent.com/android/nowinandroid/main]`

### Pattern 2: `KotlinAndroidProjectExtension` survives built-in Kotlin (commit 3) — THE Q4 answer (was highest-uncertainty)
**What:** Removing `org.jetbrains.kotlin.android` does **NOT** remove or change the Kotlin extension type. Under AGP-9 built-in Kotlin, the registered extension is **still** `org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension`, and `extensions.configure<KotlinAndroidProjectExtension> { … }` still resolves and works in a convention plugin.
```kotlin
// Source: android/nowinandroid main (AGP 9.0.0) — identical pattern to Slopper:
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension   // ← unchanged, still resolves

configure<KotlinAndroidProjectExtension> {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11        // NiA uses .assign; Slopper uses .set() — both valid
        freeCompilerArgs.add(...)
    }
}
```
**Consequence for Slopper:** `KotlinAndroid.kt:47-56` (`extensions.configure<KotlinAndroidProjectExtension> { jvmToolchain(17); compilerOptions { jvmTarget.set(JvmTarget.JVM_17); freeCompilerArgs.addAll("-opt-in=…") } }`) needs **NO rewrite** beyond the plugin-application removal. `jvmToolchain(17)` and the `-opt-in` args stay as-is. **This means there is NO `kotlinOptions→compilerOptions` work in the choke point** — Slopper's choke point already uses `compilerOptions`. `[VERIFIED: NiA main]` + `[CITED: developer.android.com/build/migrate-to-built-in-kotlin]`

> **Optional simplification (Claude's discretion, NOT required):** the migration guide notes `jvmTarget` now defaults to `android.compileOptions.targetCompatibility` (already `VERSION_17`), so the explicit `jvmTarget.set(JvmTarget.JVM_17)` *could* be dropped. Recommendation: **keep it explicit** — it is harmless, self-documenting, and keeps the diff minimal/bisect-clean.

### Pattern 3: The 5 `kotlin.android` removal sites (commit 3, completeness — D-06)
NiA confirms the pattern: its `AndroidLibraryConventionPlugin` applies only `com.android.library` (+ lint) — **no** `kotlin.android`. Remove from all 5 Slopper sites:
| # | File:line | Current | Action |
|---|-----------|---------|--------|
| 1 | `build.gradle.kts:5` | `alias(libs.plugins.kotlin.android) apply false` | delete line |
| 2 | `AndroidApplicationConventionPlugin.kt:13` | `apply("org.jetbrains.kotlin.android")` | delete line |
| 3 | `AndroidLibraryConventionPlugin.kt:14` | `apply("org.jetbrains.kotlin.android")` | delete line |
| 4 | `baselineprofile/build.gradle.kts:3` | `alias(libs.plugins.kotlin.android)` | delete line |
| 5 | `libs.versions.toml:150` | `kotlin-android = { id = "org.jetbrains.kotlin.android", … }` | delete entry |
**KEEP** `kotlin.compose` (`kotlin.versions.toml:151`, applied in `AndroidComposeConventionPlugin.kt:14`) and `kotlin.serialization` (`:152`) — built-in Kotlin replaces `kotlin-android` **only** `[CITED: migrate-to-built-in-kotlin: "Built-in Kotlin replaces the kotlin-android plugin only."]`.

### Pattern 4: The one real `kotlinOptions→compilerOptions` site (commit 5)
Only `baselineprofile/build.gradle.kts:15` still uses the old android-DSL `kotlinOptions{}`:
```kotlin
// BEFORE (baselineprofile/build.gradle.kts, inside android { }):
kotlinOptions {
    jvmTarget = "17"
}
// AFTER — move OUT of android { } to a top-level kotlin { } block:
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
```
Add `import org.jetbrains.kotlin.gradle.dsl.JvmTarget` (or fully-qualify). Note the `android.test` plugin path here also gets built-in Kotlin from AGP 9, so the `kotlin {}` extension is available. `[CITED: migrate-to-built-in-kotlin]` `[VERIFIED: navczydev.medium.com AGP 9.2.1 before/after]`

### Pattern 5: compileSdk + targetSdk surgery (commit 7 — SDK-01)
```kotlin
// KotlinAndroid.kt:21        compileSdk = 35 → 36
// baselineprofile:9          compileSdk = 35 → 36
// AndroidApplicationConventionPlugin.kt:17   defaultConfig.targetSdk = 35   ← KEEP
// AndroidLibraryConventionPlugin.kt:18       defaultConfig.targetSdk = 35   ← KEEP
// baselineprofile:21         defaultConfig { targetSdk = 35 }               ← KEEP
```
NiA `main` uses `compileSdk = 36` in the identical choke point — confirms 36 is the live AGP-9 value. `[VERIFIED: NiA main]`

### Anti-Patterns to Avoid
- **`android.builtInKotlin=false` / `android.newDsl=false` / `android.enableLegacyVariantApi=true`** — forbidden (D-09). Removed in AGP 10; repo is already clean (zero `applicationVariants`/`buildDir`; uses `androidComponents.onVariants` + `layout.buildDirectory`). The gate is only "done" with these ABSENT.
- **Bumping Hilt before AGP** — 2.59 requires AGP 9 + Gradle 9.1; bumping it before commit 2 fails. Commit 6 is strictly after commit 2.
- **Floating `hilt = "2.59+"`** — re-introduces resolution nondeterminism; pin `2.59.2` exactly (D-04).
- **Hardcoding the Gradle sha256 from memory** — must fetch live (threat T-07.2-01).

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Configure Kotlin compiler under built-in Kotlin | A custom `tasks.withType<KotlinCompile>` reach-around | `extensions.configure<KotlinAndroidProjectExtension> { compilerOptions { } }` (unchanged) | The KGP extension is still registered under AGP 9; reaching into tasks is fragile and unnecessary |
| Make `CommonExtension` callable from both Application/Library | A type alias or wrapper interface | Bare `CommonExtension` (both concrete extensions extend it) | AGP removed the generics specifically so star-projections are no longer needed |
| Fetch + validate the Gradle distribution hash | `curl` + manual paste into properties | `./gradlew wrapper --gradle-version 9.4.1 --gradle-distribution-sha256-sum <fetched>` | Atomic, validates against `validateDistributionUrl=true`, no stale-hash risk |
| Keep targetSdk from drifting to 36 | A custom Gradle check task | Explicit `targetSdk = 35` at each site + a grep assertion | AGP's own default is the hazard; the only fix is explicitness |

**Key insight:** The convention-plugin choke point already does the heavy lifting — the AGP-9 migration is a handful of *token-level* edits (one generics token, five plugin-application deletions, one `kotlinOptions` block, two compileSdk numbers, one Hilt version), not new machinery. The temptation to "modernize" the Kotlin block is a trap: NiA proves the existing `KotlinAndroidProjectExtension` block is the supported AGP-9 path.

## Runtime State Inventory

> Pure build-toolchain phase. No stored data, services, or OS registrations carry a renamed string. Inventory below for completeness.

| Category | Items Found | Action Required |
|----------|-------------|------------------|
| Stored data | None — no DB/datastore keys reference AGP/Gradle/Hilt versions. | None — verified by grep of `build-logic`, `libs.versions.toml`. |
| Live service config | **CI Gradle-home cache** (GitHub Actions `actions/cache@v5`) keyed on token `agp8` (`.github/workflows/ci.yml:39,41`). A stale AGP-8 Gradle home poisons AGP-9 configuration. | Bump key token `agp8`→`agp9` (D-07). The cache key also hashes `gradle-wrapper.properties` + `libs.versions.toml`, so it self-invalidates on the version edits too — but the explicit token bump is the belt-and-braces guard. |
| OS-registered state | None — no Task Scheduler / launchd / pm2 registration of build versions. | None — verified. |
| Secrets/env vars | None — no env var encodes a toolchain version. | None — verified. |
| Build artifacts | **Gradle wrapper dist cache** (`~/.gradle/wrapper/dists/gradle-8.11.1-…`) and the **AGP-8 transformed-classes cache** become stale after the flip. | None in-repo; the wrapper auto-downloads 9.4.1 on first run and CI cache-key bump (above) handles the runner. Local: a stale daemon may need `--stop` if it clings to 8.11.1. |

**The canonical question — after every file is updated, what still has the old toolchain cached?** Only Gradle caches (local `~/.gradle` + CI `actions/cache`). The CI side is covered by D-07; the local side self-heals on wrapper re-download.

## Common Pitfalls

### Pitfall 1: Silent `targetSdk` flip to 36 (SDK-01 — highest consequence)
**What goes wrong:** AGP 9.0 flips `android.sdk.defaultTargetSdkToCompileSdkIfUnset` **false→true**. Any module whose `targetSdk` is *unset* silently inherits `compileSdk = 36` → an Android-16 runtime opt-in (forbidden).
**Why:** The default itself is the hazard; the convention plugins set `targetSdk = 35` per-module, so a missed site doesn't error — it silently changes runtime behavior.
**How to avoid:** Keep `targetSdk = 35` explicit at all 3 sites (D-05a). Add the grep guard (D-05b, §Validation).
**Warning signs:** No build error — only a behavior change. Catch it only with the explicit grep assert or by inspecting the merged manifest. `[CITED: agp-9-0-0-release-notes]`

### Pitfall 2: "extension 'kotlin' already registered" (commit 3 dependency)
**What goes wrong:** AGP 9 registers the `kotlin` extension itself; any leftover `apply("org.jetbrains.kotlin.android")` collides: `Cannot add extension with name 'kotlin', as there is an extension already registered with that name.`
**Why:** Two code paths register `kotlin`; the convention-plugin indirection hides the duplicate from a root-`build.gradle.kts` grep.
**How to avoid:** Remove all 5 sites (Pattern 3). This is why commits 2 and 3 cannot both be green in isolation (see Sequencing below).
**Warning signs:** First module (`:app` or `:core:common`) fails at configuration, naming the `kotlin` extension. `[VERIFIED: navczydev.medium.com]`

### Pitfall 3: Hilt 2.59 drops AGP 8 / 2.59.0 broken (commit 6 ordering)
**What goes wrong:** Hilt 2.59 requires AGP 9 + Gradle 9.1 and 2.59.0 ships a broken `ComponentTreeDeps` (Dagger #5099 → `package dagger.hilt.internal.componenttreedeps does not exist`).
**How to avoid:** Pin `2.59.2` (fixed) and land commit 6 strictly after commit 2. One catalog edit (`libs.versions.toml:6`) covers all three Hilt artifacts.
**Warning signs:** `ComponentTreeDeps` missing-package error (wrong patch) or `NoClassDefFoundError: …ScopedArtifact$POST_COMPILATION_CLASSES` (Hilt 2.59 on too-old AGP). `[CITED: github.com/google/dagger/issues/5099]`

### Pitfall 4: Multi-module blast radius — choke-point failure fails ALL modules
**What goes wrong:** A mistake in `KotlinAndroid.kt` or either convention plugin fails configuration for every module at once; you can't tell which change broke what.
**How to avoid:** D-02 — run `./gradlew :core:common:compileDebugKotlin` (the choke point applies there too) after the AGP + built-in-Kotlin + generics commits, before a full build. One-concern-per-commit keeps `git bisect` precise.
**Warning signs:** "All modules failed configuration" with a `io.stashapp.android.buildlogic.*` stack frame.

### Pitfall 5: detekt/ktlint versions are pinned in the ROOT build script, not just the catalog
**What goes wrong:** Root `build.gradle.kts` `subprojects{}` hardcodes `detekt { toolVersion = "1.23.8" }` (`:53`) and `ktlint { version.set("1.6.0") }` (`:42`) — separate from the catalog `detekt`/`ktlint` plugin versions. A catalog-only audit misses these. (Not changed this phase, but the planner must know they exist if detekt needs a fallback — see Pitfall 6.)
**How to avoid:** Treat the root `subprojects{}` block as a second source of truth for detekt/ktlint. `[VERIFIED: build.gradle.kts:40-63]`

### Pitfall 6: detekt 1.23.8 on Gradle 9 + AGP 9 (Q7 — the empirical unknown, de-risked)
**What goes wrong (feared):** detekt 1.23.8 is built against Gradle 8.12.1 / Kotlin 2.0.21 and bundles Kotlin compiler **2.0.0** (reads metadata ≤ **2.1.0**). Its known hard failure (detekt #8865) is `class 'kotlin.Unit' … metadata version is 2.3.0, but compiler 2.0.0 can read up to 2.1.0`.
**Why it likely does NOT bite Slopper:** (a) that failure is triggered by **Kotlin 2.3.0** metadata — Slopper is pinned at **2.2.20 and is NOT bumping** (D-04b); (b) Slopper's detekt config (root `subprojects{}`) sets `source.setFrom(...)` with **no `jvmTarget`/classpath** wiring → detekt runs in its default **parse-only** mode (no type resolution), so it does not read project `.class` metadata at all; (c) detekt's Gradle plugin applies Kotlin/AGP as `compileOnly`, so it tolerates a newer Gradle with deprecation warnings rather than a hard classpath conflict.
**Residual risk (MEDIUM):** Kotlin 2.2.20 source *metadata* is 2.2 — above detekt's 2.1.0 ceiling. IF detekt is ever pushed into type-resolution mode (it is not, today) OR if it chokes parsing a 2.2 language feature, it could fail. This is the ADR's deferred empirical test, resolved at this phase's first green build.
**Concrete fallback if detekt 1.23.8 hard-fails (without the forbidden 2.0.0-alpha):** in order of preference — (1) ensure no type-resolution classpath is configured (confirm it isn't); (2) scope detekt off the failing surface via the existing `baseline`/`config` mechanism; (3) run detekt with `--continue` isolated from the compile gate and track as a deviation; (4) last resort — temporarily exclude the offending module's detekt task and file tech debt. Do NOT adopt detekt 2.0.0-alpha (D-08, ADR stable-only policy). `[CITED: github.com/detekt/detekt/issues/8865]` `[CITED: detekt.dev/docs/introduction/compatibility]`

## Code Examples

### The complete migrated choke point (`KotlinAndroid.kt`) — post-commits-4&7
```kotlin
// Source: synthesized from android/nowinandroid main (AGP 9.0.0, compileSdk 36)
//         applied to Slopper's existing block. Imports UNCHANGED.
import com.android.build.api.dsl.CommonExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

internal fun Project.configureKotlinAndroid(
    commonExtension: CommonExtension,                    // ← was <*,*,*,*,*,*>
) {
    commonExtension.apply {
        compileSdk = 36                                  // ← was 35
        defaultConfig { minSdk = 26 }
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
            isCoreLibraryDesugaringEnabled = false
        }
        lint { disable.addAll(listOf(/* DEPS-07 disables — unchanged */)) }
    }
    extensions.configure<KotlinAndroidProjectExtension> {   // ← STILL resolves under built-in Kotlin
        jvmToolchain(17)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            freeCompilerArgs.addAll("-opt-in=kotlin.RequiresOptIn",
                "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi")
        }
    }
}
```

## State of the Art

| Old Approach (AGP 8.7.3) | Current (AGP 9.2.x) | When Changed | Impact on Slopper |
|--------------------------|---------------------|--------------|-------------------|
| `CommonExtension<*,*,*,*,*,*>` (6 generics) | bare `CommonExtension` | AGP 9.0 | 1-token edit, `KotlinAndroid.kt:18` |
| Apply `org.jetbrains.kotlin.android` | built-in Kotlin (default-on) | AGP 9.0 | drop 5 sites; KGP extension type unchanged |
| android `kotlinOptions {}` DSL | `kotlin { compilerOptions {} }` | AGP 9.0 | only `baselineprofile:15` (choke point already on `compilerOptions`) |
| `targetSdk` defaults to `minSdk` | defaults to `compileSdk` | AGP 9.0 | must keep `targetSdk = 35` explicit |
| legacy variant API (`applicationVariants`) | `androidComponents.onVariants` | AGP 9.0 (removed) | repo already clean — no change |
| Hilt 2.56.2 (AGP 8) | Hilt 2.59.2 (AGP 9 only) | Dagger 2.59 | one catalog bump; lands after AGP |

**Deprecated/removed:** `enableLegacyVariantApi` never existed in AGP 9 (the original framing was wrong — confirmed in PITFALLS.md Pitfall 3). `android.newDsl=false`/`android.builtInKotlin=false` exist but are removed in AGP 10 and forbidden here.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | detekt 1.23.8 RUNS (warns, not fails) on Gradle 9 + AGP 9 with Kotlin 2.2.20 because Slopper's config is parse-only (no type resolution) | Pitfall 6 | If it hard-fails, the gate blocks — but the fallback ladder (no 2.0.0-alpha) is pre-specified; ADR already flags this as the deferred empirical test. MEDIUM risk, bounded. |
| A2 | Kotlin 2.2.20 metadata (2.2) does not trip detekt 1.23.8's 2.1.0 ceiling in parse-only mode | Pitfall 6 | Same as A1 — surfaces at first green build, fallback ready. |
| A3 | `AndroidComposeConventionPlugin.kt:16` `getByType(CommonExtension::class.java)` is unaffected by the generics removal (class literal) | Pattern 1 | LOW — class literals carry no type args; re-verify at build time per D-02. |

**Note:** A1/A2 are the *only* non-HIGH-confidence items. Everything about AGP/Hilt versions, the `CommonExtension` arity, and the `KotlinAndroidProjectExtension` survival is HIGH (live-verified against NiA `main` + official release notes).

## Open Questions

1. **Does detekt 1.23.8 actually pass the gate on the live AGP-9 build?**
   - What we know: parse-only config, Kotlin 2.2.20 (not 2.3.0), `compileOnly` Gradle wiring → expected to run.
   - What's unclear: empirically untested until the first green build (ADR-acknowledged).
   - Recommendation: plan the gate to run detekt; if it fails, apply the §Pitfall 6 fallback ladder and document as a planned deviation. Do NOT pre-bump.

2. **NiA `main` runs Kotlin 2.3.0 + AGP 9.0.0; Slopper runs Kotlin 2.2.20 + AGP 9.2.1 — any divergence in the `KotlinAndroidProjectExtension` API between those points?**
   - What we know: the extension type and `compilerOptions`/`jvmTarget`/`freeCompilerArgs` API are stable across KGP 2.2.x→2.3.x; AGP 9.0→9.2 did not change built-in-Kotlin extension wiring (no release-note mention).
   - Recommendation: HIGH confidence it's identical; the `:core:common:compileDebugKotlin` smoke (D-02) confirms in seconds.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Gradle 9.4.1 distribution | commit 1 wrapper flip | ✓ | `gradle-9.4.1-bin.zip` HTTP 307 (CDN) | none — hard requirement |
| Gradle 9.4.1 `.sha256` | commit 1 hash re-pin | ✓ | `.sha256` 301-redirect (follow with `curl -sSL`) | none — fetch at exec |
| AGP 9.2.1 (`com.android.tools.build:gradle`) | commit 2 | ✓ | `9.2.1` POM HTTP 200 (Google Maven) | `9.2.0` (also published) per D-03 |
| Hilt/Dagger 2.59.2 | commit 6 | ✓ | `2.59.2` POM HTTP 200; `<latest>`/`<release>` | none — 2.59.2 is the only fixed patch |
| Build-Tools 36.0.0 | compileSdk 36 (commit 7) | ✓ (assumed — AGP 9.2 default) | `36.0.0` min+default | AGP auto-provisions if SDK manager configured |
| JDK 17+ | AGP 9.2 + Gradle 9.4.1 | ✓ (repo: 17 toolchain / 21 daemon) | unchanged | none needed |

**Missing dependencies with no fallback:** none — all live-verified reachable.

## Validation Architecture

> `workflow.nyquist_validation` is **false** in `.planning/config.json` — the formal Requirements→Test map is omitted. The phase gate below is the validation contract (build-config phase: "tests" = the compile/static-analysis/unit gate, which is the D-08 definition of done).

### Smoke + gate commands
| Stage | Command | Purpose |
|-------|---------|---------|
| Choke-point smoke (D-02) | `./gradlew :core:common:compileDebugKotlin --stacktrace` | Fast single-module reproduction of any convention-plugin break (generics / built-in-Kotlin / extension). Run AFTER commits 2-4. |
| targetSdk guard (D-05b) | `grep -rn "targetSdk" --include=*.kts --include=*.kt build-logic baselineprofile` → assert `= 35` at all 3 sites; `grep -rn "compileSdk" …` → assert `= 36` at both sites | Grep-checkable, catches the silent flip (no build error otherwise). |
| Full phase gate (D-08) | `./gradlew compileDebugSources detekt ktlintCheck test --stacktrace` | All ~14 modules green with opt-out flags REMOVED. detekt empirical test happens here. |
| CI cache key (D-07) | `grep -n "agp8" .github/workflows/ci.yml` → expect ZERO hits after edit | Confirms `agp8`→`agp9` at `:39` and `:41`. |

### "Looks done but isn't" gate checklist (from PITFALLS.md, narrowed to Phase 8)
- [ ] AGP 9 green with `builtInKotlin`/`newDsl`/`enableLegacyVariantApi` opt-outs ABSENT (not merely defaulted off).
- [ ] `grep -rn "compileSdk"` → all sites 36; `grep -rn "targetSdk"` → all sites 35.
- [ ] Catalog `hilt = "2.59.2"` exactly (not `2.59`/`2.59.1`/floating); a `:feature:*` Hilt+KSP DI graph compiles.
- [ ] `grep -rn "org.jetbrains.kotlin.android"` → ZERO hits across repo (all 5 sites removed).
- [ ] `grep -n "agp8" .github/workflows/ci.yml` → ZERO hits.
- [ ] detekt either passes OR a documented §Pitfall-6 fallback is applied (NOT 2.0.0-alpha).

## Sequencing Landmines (Q6 — the safest bisectable grouping)

**Per-commit green-in-isolation analysis of the D-01 order:**

| Commit | Concern | Green in isolation? | Bisectable? | Note |
|--------|---------|--------------------|-------------|------|
| 1 | Gradle 9.4.1 wrapper | **NO** (AGP 8.7.3 hard-fails Gradle-9 check) | YES | Expected — ADR fold-forward. Commit alone, then immediately commit 2. |
| 2 | AGP 9.2.1 | **NO** (built-in Kotlin collides with still-applied `kotlin.android` → "extension already registered") | YES | The collision error is diagnostic, not corrupting. |
| 3 | drop `kotlin.android` (5 sites) | **NO** (generics still `<*,…>` → `build-logic` compile error) | YES | |
| 4 | `CommonExtension` generics fix | **likely YES at choke point** | YES | After 2-4, `:core:common:compileDebugKotlin` (D-02) should pass. |
| 5 | `kotlinOptions→compilerOptions` (baselineprofile) | YES (isolated module) | YES | |
| 6 | Hilt 2.59.2 | YES (requires AGP 9 from commit 2) | YES | |
| 7 | compileSdk 36 | YES | YES | Full gate here. |

**Verdict on the "must commits 2+3 merge?" question:** **NO merge required.** Commits 2 and 3 are each non-green but each produces a *distinct, attributable* failure (commit 2 → "extension already registered"; commit 3 → generics compile error). `git bisect` lands on the precise concern. This is the explicit intent of D-01 ("green only at the end, but bisect can isolate which concern broke configuration"). Keep all 7 separate. The build first goes green after commit 4 at the **choke-point smoke** (D-02) and after commit 7 at the **full gate**.

**Only hard ordering constraints (everything else is for bisect clarity):**
1. Commit 1 (Gradle) **before** commit 2 (AGP) — AGP 9 refuses Gradle < 9.4.1.
2. Commit 3 (drop `kotlin.android`) **with-or-after** commit 2 — built-in Kotlin only collides once AGP is 9.x; doing it before commit 2 would remove Kotlin support on AGP 8.7.3 and break compilation. **Safest: 2 then 3.**
3. Commit 6 (Hilt 2.59.2) **after** commit 2 — 2.59 drops AGP 8.

## Project Constraints (from CLAUDE.md + global rules)
- **CLAUDE.md (project):** Spec-Layer session protocol — no Phase-8 code directives; defers to `.planning/`.
- **Global stable-only policy** (ADR-codified): no pre-release plugins → reinforces detekt 1.23.8 (not 2.0.0-alpha).
- **Global immutability/file-size/security rules:** apply to app code, not to version-catalog/build-script edits; no new app code this phase. Security: no secrets touched (the only secret-adjacent surface, CI signing, is Phase 10).

## Sources

### Primary (HIGH confidence)
- Google Maven `com/android/tools/build/gradle/maven-metadata.xml` + `gradle-9.2.1.pom` (HTTP 200) — AGP `9.2.1` is newest stable 9.2.x.
- Maven Central `com/google/dagger/dagger/maven-metadata.xml` (`<latest>2.59.2</latest>`) + `hilt-android-2.59.2.pom` (HTTP 200) — Hilt `2.59.2`.
- `developer.android.com/build/releases/agp-9-0-0-release-notes` — CommonExtension generics removal, `defaultTargetSdkToCompileSdkIfUnset` false→true, `android.newDsl`, legacy-variant removal, built-in Kotlin default, KGP 2.2.10 floor, compileSdk 36.1 max.
- `developer.android.com/build/releases/agp-9-2-0-release-notes` — AGP 9.2 Gradle floor 9.4.1, JDK 17, Build-Tools 36.0.0, API 36.1 max.
- `developer.android.com/build/migrate-to-built-in-kotlin` — remove `kotlin-android` ONLY; `kotlin{compilerOptions{}}` DSL; jvmTarget defaults to targetCompatibility.
- **`raw.githubusercontent.com/android/nowinandroid/main` `KotlinAndroid.kt` + `AndroidLibraryConventionPlugin.kt` + `libs.versions.toml`** — live proof on AGP 9.0.0: bare `CommonExtension`, `configure<KotlinAndroidProjectExtension>{compilerOptions}`, `compileSdk = 36`, no `kotlin.android` application.
- `services.gradle.org/distributions/gradle-9.4.1-bin.zip[.sha256]` (HTTP 307 / 301) — distribution live, hash fetch-at-exec.

### Secondary (MEDIUM confidence)
- `navczydev.medium.com` "Migrating to AGP 9.2.1" — confirms "extension already registered" error + `kotlinOptions→kotlin{compilerOptions}` before/after.
- `github.com/google/dagger/issues/5099` — Hilt 2.59.0 broken `ComponentTreeDeps`, fixed 2.59.2.
- `github.com/detekt/detekt/issues/8865` + `detekt.dev/docs/introduction/compatibility` — detekt 1.23.8 bundles compiler 2.0.0 (metadata ≤ 2.1.0); fails on Kotlin 2.3.0.
- `.planning/research/{STACK,PITFALLS}.md`, `docs/adr/0001-…md`, `07-RESEARCH.md` — prior milestone research (cross-confirmed against live registries above).

## Metadata
**Confidence breakdown:**
- Standard stack (versions): HIGH — every version live-verified against Google Maven / Maven Central + POM 200s.
- Architecture / DSL mechanics: HIGH — `CommonExtension` arity and `KotlinAndroidProjectExtension` survival proven against NiA `main` + official release notes (the two highest-uncertainty items resolved).
- Pitfalls: HIGH except detekt-on-Gradle-9 (MEDIUM — empirical, but bounded with a pre-specified fallback ladder).

**Research date:** 2026-05-30
**Valid until:** ~2026-06-13 (14 days — AGP/Hilt patch cadence; re-confirm 9.2.x/2.59.x latest at plan execution per D-03/D-04).
