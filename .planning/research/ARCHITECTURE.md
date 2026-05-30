# Architecture Research — AGP-9 Build-Graph Integration

**Domain:** Android Compose multi-module build toolchain upgrade (AGP 8.7.3 → 9.x, Gradle 8.11.1 → 9.x, compileSdk 35 → 36)
**Researched:** 2026-05-30
**Confidence:** HIGH (AGP 9 release notes, Dagger 2.59 release + issue tracker, JetBrains AGP-9 migration guide all verified; one MEDIUM flag on exact baseline-profile-plugin GA version and one on whether AGP 9's apksig fixes the EdEC CI failure)

> Scope note: "Architecture" here = the **build architecture** (convention plugins, module graph wiring, CI pipeline, signing/packaging path). App runtime architecture is frozen this milestone and is not re-derived.

---

## Standard Architecture — Where the Upgrade Lands

The Slopper build graph is a classic Now-in-Android-style layout: a `build-logic` composite build publishes five convention plugins that every `app`/`core`/`feature` module applies. **All version/variant/Kotlin wiring funnels through `build-logic`** — that is both the migration's biggest simplifier and its single point of failure.

```
┌──────────────────────────────────────────────────────────────────────┐
│  gradle/wrapper/gradle-wrapper.properties   gradle.properties          │
│  Gradle 8.11.1 → 9.x                        config-cache ON, JDK pin   │  ← INTEGRATION POINT 1
├──────────────────────────────────────────────────────────────────────┤
│  gradle/libs.versions.toml  (single source of versions)                │  ← INTEGRATION POINT 2
│   agp 8.7.3→9.x · kotlin 2.2.20 · ksp · hilt 2.56.2→2.59.1 ·           │
│   media3 1.9.1→1.10 · baselineProfilePlugin 1.4.1 · benchmark 1.3.3    │
├──────────────────────────────────────────────────────────────────────┤
│  build-logic/convention  (THE CHOKE POINT)                             │  ← INTEGRATION POINT 3
│   build.gradle.kts: compileOnly(plugin.android/kotlin/compose/ksp)     │
│   ┌────────────────────────┬─────────────────────────────────────┐    │
│   │ KotlinAndroid.kt       │ compileSdk=35, jvmToolchain(17),     │    │
│   │  (shared helper)       │ KotlinAndroidProjectExtension wiring │    │
│   ├────────────────────────┼─────────────────────────────────────┤    │
│   │ AndroidApplication     │ apply("org.jetbrains.kotlin.android")│    │ ← drops in AGP 9
│   │ AndroidLibrary         │ apply("org.jetbrains.kotlin.android")│    │ ← drops in AGP 9
│   │ AndroidCompose         │ ComposeCompilerGradlePluginExtension │    │
│   │ AndroidHilt            │ apply ksp + hilt.android             │    │ ← Hilt 2.59 gate
│   │ AndroidFeature         │ bundles library+compose+hilt         │    │
│   └────────────────────────┴─────────────────────────────────────┘    │
├──────────────────────────────────────────────────────────────────────┤
│  Per-module build.gradle.kts (mostly inherit; few touch points)       │  ← INTEGRATION POINT 4
│   app/build.gradle.kts:        targetSdk via convention, signing,      │
│                                splits, baselineProfile consumer        │
│   baselineprofile/build.gradle.kts: compileSdk=35, targetSdk=35,       │  ← INTEGRATION POINT 5
│     com.android.test + androidx.baselineprofile + onVariants{} block   │
├──────────────────────────────────────────────────────────────────────┤
│  CI: .github/workflows/ci.yml  +  .forgejo/workflows/ci.yml            │  ← INTEGRATION POINT 6
│   compileDebugSources + detekt + ktlintCheck + test  (NO assemble)     │
│   cache key hardcodes "...-jdk17-agp8-..."                             │
└──────────────────────────────────────────────────────────────────────┘
```

### Integration Points (concrete to this repo)

| # | File | What changes | Inherited or per-module? |
|---|------|--------------|--------------------------|
| 1 | `gradle/wrapper/gradle-wrapper.properties` | `8.11.1` → `9.x` (+ new `distributionSha256Sum`) | Global |
| 1 | `gradle.properties` | Possibly add `android.builtInKotlin` opt-out flag (transitional); verify config-cache still valid | Global |
| 2 | `gradle/libs.versions.toml` | `agp`, `hilt`→`2.59.1`, `media3`→`1.10`, `nextlibMedia3Ext` lockstep, `baselineProfilePlugin`, `benchmark`, leaf libs (`coreKtx 1.15→1.18`, `activityCompose 1.9.3→1.13`) | Global |
| 3 | `build-logic/.../AndroidApplicationConventionPlugin.kt` | **Remove** `apply("org.jetbrains.kotlin.android")` | Choke point (1 edit → all app) |
| 3 | `build-logic/.../AndroidLibraryConventionPlugin.kt` | **Remove** `apply("org.jetbrains.kotlin.android")` | Choke point (1 edit → all libs/features) |
| 3 | `build-logic/.../KotlinAndroid.kt` | `compileSdk = 35` → `36`; verify `kotlinOptions`/`compilerOptions` DSL still valid under built-in Kotlin (`kotlin.compilerOptions{}`); revisit the 3 disabled lint detectors | Choke point |
| 3 | `AndroidApplication/LibraryConventionPlugin.kt` | `defaultConfig.targetSdk = 35` → keep 35 (runtime targetSdk frozen) **or** rely on new `defaultTargetSdkToCompileSdkIfUnset=true` default — must pin explicitly to avoid silent bump | Choke point |
| 4 | `app/build.gradle.kts` | No legacy `applicationVariants` present (already uses `buildTypes`/`signingConfigs` DSL) → low touch. Verify `splits.abi`, `androidResources.generateLocaleConfig`, `ndk.debugSymbolLevel` still valid DSL | Per-module |
| 5 | `baselineprofile/build.gradle.kts` | `compileSdk=35→36`, `targetSdk=35→36`; **`androidComponents.onVariants{}` already new-API (good)**; verify `baselineProfilePlugin` version supports AGP 9 GA | Per-module (AGP-coupled) |
| 6 | both `ci.yml` files | Bump cache key `jdk17-agp8` → `agp9`; decide compile-only vs add `assembleDebug`; consider Gradle JDK requirement | CI |

---

## Recommended Build Order (the key deliverable)

Dependency reasoning drives this. The hard constraints are: **(a)** Hilt 2.59 requires AGP 9 *and drops AGP 8* — there is no version of Hilt that straddles both, so Hilt + AGP are a **forced atomic pair**; **(b)** AGP 9 requires Gradle 9.1+ and JDK 17+ — Gradle must move *first or together*; **(c)** AGP 9 enables built-in Kotlin and removes legacy variant API by default — the convention-plugin DSL edits must land *in the same commit* as the AGP bump or every module fails to configure.

### Staged sequence

```
STAGE 0  Pre-flight (separate commit, no toolchain change)
  └─ Read compose-compiler / lint detector debt; confirm no module uses
     applicationVariants/libraryVariants (grep) → confirmed app & baselineprofile
     already on new DSL. Add android.enableLegacyVariantApi / android.newDsl
     escape-hatch knowledge to gradle.properties notes (do NOT set yet).

STAGE 1  Gradle wrapper bump  (ATOMIC commit, build still green on AGP 8.7.3)
  └─ gradle-wrapper.properties 8.11.1 → 9.x + sha256.
     Gradle 9 is backward-compatible enough to run AGP 8.7.3 for one commit;
     this isolates Gradle-9 deprecation fallout (config-cache, removed APIs)
     from the AGP fallout. Run ./gradlew compileDebugSources to surface
     Gradle-9-only breakage in isolation.
     ⚠ If AGP 8.7.3 is NOT Gradle-9-compatible, FOLD this into Stage 2.

STAGE 2  AGP + built-in-Kotlin + Hilt  (ONE ATOMIC commit — the lockstep core)
  ├─ libs.versions.toml: agp → 9.x, hilt → 2.59.1
  ├─ build-logic: remove apply("org.jetbrains.kotlin.android") from BOTH
  │   Application + Library convention plugins
  ├─ build-logic: migrate android.kotlinOptions{} → kotlin.compilerOptions{}
  │   if AGP 9 rejects the old DSL (KotlinAndroid.kt already uses
  │   KotlinAndroidProjectExtension.compilerOptions — likely fine, verify)
  └─ This MUST be atomic: built-in Kotlin + plugin removal + Hilt-AGP9 are
     mutually dependent. A partial commit leaves every module unconfigurable.
     Gate: compileDebugSources + test green.

STAGE 3  compileSdk 36  (ATOMIC commit, small surface)
  ├─ KotlinAndroid.kt compileSdk 35 → 36
  └─ baselineprofile/build.gradle.kts compileSdk 35 → 36 (+ targetSdk 36)
     compileSdk is the lowest-risk bump (compile-time only, no minSdk/targetSdk
     runtime change). Keep separate so any SDK-36 lint/API surfacing is isolated.

STAGE 4  baseline-profile + benchmark lockstep  (ATOMIC commit)
  ├─ baselineProfilePlugin → AGP-9-compatible release (verify version)
  └─ benchmark → matching 1.4.x+ (verify); nextlib/Media3 can ride here or
     get their own commit.

STAGE 5  Media3 1.9.1 → 1.10 + nextlib-media3ext lockstep  (ATOMIC commit)
  └─ Independent of AGP mechanics; sequenced last to keep blast radius small.
     Version-paired (media3 + nextlibMedia3Ext must move together — already
     documented in libs.versions.toml CASE B comment).

STAGE 6  Leaf libs (coreKtx 1.18, activityCompose 1.13)  (ATOMIC commit)
  └─ Now-unblocked by compileSdk 36; trivial, batch together.

STAGE 7  CI pipeline update  (commit alongside Stage 2, or immediately after)
  ├─ both ci.yml: cache key jdk17-agp8 → agp9
  ├─ verify Gradle 9 daemon JDK requirement vs the 17+21 setup
  └─ CI-signing decision (see below)
```

### Atomic vs staged — summary table

| Change cluster | Atomic with… | Why |
|----------------|--------------|-----|
| Gradle wrapper | itself (Stage 1) | Isolate Gradle-9 deprecations from AGP-9 |
| AGP + drop kotlin.android + Hilt 2.59 | **must be one commit** | Built-in Kotlin, plugin removal, and Hilt's AGP-9-only plugin are mutually dependent; no half-state builds |
| compileSdk 36 | itself | Lowest risk; isolate SDK-36 surfacing |
| baseline-profile + benchmark | each other | Plugin/runtime version pair |
| Media3 + nextlib | each other | FFmpeg ext is version-locked to Media3 |
| Leaf libs | batched | Independent, trivial |
| CI cache key + signing | with/after Stage 2 | Cache key references agp8; stale key just misses, not breaks |

**Rule of thumb for the roadmapper:** anything that would leave the build *unconfigurable* if split (AGP/Kotlin/Hilt) is atomic; anything that only changes *which* version resolves (Media3, leaf libs, SDK) can be its own commit with its own green gate.

---

## The Convention-Plugin Choke Point — Simplifier vs Risk

**Simplifier.** Because `KotlinAndroid.kt` is the *single* place `compileSdk` and the Kotlin compiler/toolchain are wired, the SDK-36 bump is **two edits** (`KotlinAndroid.kt` + `baselineprofile`, which is standalone). The `org.jetbrains.kotlin.android` removal is **two edits** (Application + Library convention plugins) and propagates to all ~14 modules — no per-module `build.gradle.kts` churn. The `build-logic/convention/build.gradle.kts` `compileOnly(libs.plugin.kotlin)` classpath dep can stay (KGP is still on the classpath for the Compose compiler plugin and toolchain types), so the plugin classpath is largely undisturbed.

**Risk.** The same centralization means **one broken `configure` block fails every module's configuration phase at once** — there is no "some modules still build" fallback. The specific landmines:
- `configureKotlinAndroid` calls `extensions.configure<KotlinAndroidProjectExtension>` — under built-in Kotlin AGP *registers this extension itself*; double-registration or a renamed extension type would break all modules. Verify the extension is still resolvable post-removal.
- AGP 9 `android.newDsl=true` hides old extension impl types. The convention plugins cast to `ApplicationExtension`/`LibraryExtension`/`CommonExtension` (the **new** public interfaces) — this is correct and AGP-9-safe. They do **not** touch `BaseExtension`, so the common `ClassCastException` trap is already avoided.
- `targetSdk` is set in the convention plugins (`defaultConfig.targetSdk = 35`). AGP 9 flips `defaultTargetSdkToCompileSdkIfUnset=true` — but since it's set explicitly, no silent bump. Keep it explicit (constraint: runtime targetSdk frozen).

**Recommendation:** treat `build-logic` edits as their own verification sub-gate within Stage 2 — run `./gradlew :app:help` (configuration only) before a full compile to fail fast on configuration-phase breakage.

---

## CI Pipeline & the BouncyCastle EdEC Signing Calculus

### What the failure actually is

`:app:validateSigningDebug` invokes `apksig` (AGP's APK signer), which references `org.bouncycastle.asn1.edec.EdECObjectIdentifiers`. That class lives in **bcprov ≥ 1.70** (the `org.bouncycastle.asn1.edec` package). The `NoClassDefFoundError` on CI runners is a **classpath-resolution conflict**: an older `bcprov` (lacking the `edec` package) wins on the signing classpath in the runner environment, while locally a ≥1.70 `bcprov` resolves. It is fundamentally a **bcprov version-on-classpath bug, not an AGP-version-gated bug** — confirmed by the same `EdECObjectIdentifiers` `NoClassDefFoundError` appearing in unrelated tools (SonarQube Gradle plugin) when an old bcprov shadows a new one.

### Does AGP 9 change the calculus?

**MEDIUM confidence: likely improves, not guaranteed to fix.** AGP 9 ships a newer bundled `apksig` and pulls a newer `bcprov` transitively, and the JDK floor is 17 (the runner already uses JDK 17+21). A newer bundled bcprov *raises the floor* of what apksig sees, which is exactly the direction that resolves this class of `NoClassDefFoundError`. **But** the root cause is a *conflict* (something old shadowing something new), and AGP 9 does not, per its release notes, contain a signing/apksig fix that names this issue — AGP 9.0/9.1/9.2 release notes mention no BouncyCastle or `validateSigning` changes. So AGP 9 may incidentally fix it (newer floor) or may not (if the conflicting old bcprov source is in the runner's environment classpath, not AGP's).

### Recommended CI-signing strategy for AGP 9

**Two-step, evidence-driven — do NOT assume AGP 9 fixes it:**

1. **Keep the compile-only gate as the contract.** `compileDebugSources + detekt + ktlintCheck + test` remains the authoritative green gate. Do not make the milestone depend on signing working in CI.

2. **Add a non-blocking probe job** (or a one-shot manual run) on the AGP-9 branch that runs `./gradlew :app:validateSigningDebug` (or `assembleDebug`) with `continue-on-error: true`. If it passes green on AGP 9 runners → **promote `assembleDebug` into the real gate** in a follow-up commit (this is the milestone's stated "may now permit restoring a real assembleDebug gate"). If it still throws EdEC → keep compile-only and, if a real-assembly gate is wanted, pin a `bcprov` ≥ 1.78 onto the signing classpath via a `dependencies { }` constraint as the targeted fix rather than waiting on AGP.

**Bottom line for the roadmapper:** budget one CI experiment task in the AGP-9 phase to *test* whether assembly is now viable; do not pre-commit to restoring `assembleDebug` in the plan. The compile-only gate stays the floor.

---

## baseline-profile Module — AGP-9 Lockstep

The `baselineprofile/` module is the most AGP-coupled module: it applies `com.android.test` (an AGP plugin, version-locked to `agp`) **plus** `androidx.baselineprofile` (an independent androidx plugin). Two coupling facts:

1. **`com.android.test` rides the `agp` version automatically** — it's `version.ref = "agp"` in the catalog (`android-test` plugin alias). No separate bump; it moves with Stage 2.

2. **`androidx.baselineprofile` is the lockstep risk.** Current pin: `baselineProfilePlugin = "1.4.1"`, `benchmark = "1.3.3"`. The plugin historically reached into the **legacy `applicationVariants` API** (its `BaselineProfileAppTargetPlugin`), which AGP 9 removes. The published "max recommended AGP 9.0.0-alpha01" line in the androidx benchmark release notes is **stale documentation** (predates AGP 9 GA) and should NOT be read as "only alpha works." A baseline-profile/benchmark **1.4.x release that uses the new variant API is required** for AGP 9 GA — verify at plan time which exact `benchmark` version (1.4.1 is stable as of Sep 2025; a 1.4.x or 1.5.x may be needed for the variant-API migration). **Action: bump `benchmark` to match `baselineProfilePlugin`, keep both ≥ the version that compiles against AGP 9, and treat them as one lockstep pair (Stage 4).**

The module's own `build.gradle.kts` is already on the **new** `androidComponents.onVariants{}` API (lines 51–60) — so the *consuming* code is AGP-9-ready; only the *plugin's internal* variant usage is the gate, which is fixed by the version bump, not by editing this file.

---

## Anti-Patterns to Avoid (this migration)

| Anti-pattern | Why bad | Instead |
|--------------|---------|---------|
| Bumping AGP without removing `kotlin.android` | Every module fails configuration | Atomic: AGP + plugin removal + Hilt in one commit |
| Setting `android.builtInKotlin=false` permanently | Removed in AGP 10; defers the real work | Use only as a transient bisect tool, then do the real migration |
| Restoring `assembleDebug` to CI on faith that AGP 9 fixed EdEC | Unverified; may re-break the gate | Probe with `continue-on-error` first, promote only on green |
| Bumping `agp` but leaving `hilt 2.56.2` | 2.56.2 plugin uses legacy variant API → fails on AGP 9 | Hilt 2.59.1 is mandatory and atomic with AGP |
| Bumping `baselineProfilePlugin` without `benchmark` | Runtime/plugin version skew | Move both as a pair |
| Letting Gradle auto-download a JDK | `gradle.properties` pins `auto-download=false`; AGP 9 needs JDK 17 daemon | Keep the 17(toolchain)+21(runtime) setup; verify Gradle 9 accepts it |
| Splitting Media3 from nextlib-media3ext | FFmpeg ext is version-locked | Move `media3` + `nextlibMedia3Ext` together |

---

## Known Secondary Risks (flag for plan-time research)

- **Hilt 2.59 `ComponentTreeDeps` (#5099):** 2.59.0 shipped with the runtime annotation class missing → "package dagger.hilt.internal.componenttreedeps does not exist." **Use 2.59.1** (the patch), not 2.59.0. (MEDIUM — confirm 2.59.1 fully resolves it at plan time.)
- **Hilt 2.59.1 slow incremental builds (#5122):** `hiltSyncDebug` task time doubled on AGP 9. Not a blocker, but watch CI duration; may want to tune.
- **`enableJetifier` + AGP 9:** Dagger noted an AGP-9 + `enableJetifier=true` issue. Slopper's `gradle.properties` does **not** set `enableJetifier` → not affected. (HIGH — verified absent.)
- **Gradle minimum drift:** AGP 9.0 requires Gradle 9.1+; AGP 9.1.1 requires Gradle 9.3.1. One source claimed AGP 9.2.0 needs only "Gradle 8.11+" — treat that as **likely erroneous** (contradicts the whole AGP-9 line). Pin Gradle to the version the chosen AGP's release notes mandate; verify the wrapper version against the exact AGP 9.x picked.
- **Config-cache (`org.gradle.configuration-cache=true`):** already on. AGP 9 + Gradle 9 are generally config-cache-friendlier, but convention-plugin code that reads `rootProject.file(...)` at configuration time (the `keystore.properties` read in `app/build.gradle.kts`, and `compose_stability.conf` in the Compose convention plugin) should be re-verified config-cache-clean under Gradle 9.

---

## Sources

- [Android Gradle plugin 9.0.1 release notes](https://developer.android.com/build/releases/agp-9-0-0-release-notes) — Gradle 9.1+ / JDK 17 floor, built-in Kotlin default, legacy variant API removal, compileSdk-consumer rule, `defaultTargetSdkToCompileSdkIfUnset` flip. HIGH.
- [Android Gradle plugin 9.1.1 release notes](https://developer.android.com/build/releases/agp-9-1-0-release-notes) — Gradle 9.3.1 min; no signing/apksig/BouncyCastle changes. HIGH.
- [Android Gradle plugin 9.2.0 release notes](https://developer.android.com/build/releases/agp-9-2-0-release-notes) — 9.2.0 GA April 2026, max API 36.1. HIGH (one Gradle-min figure in a secondary summary is suspect).
- [Migrate to built-in Kotlin](https://developer.android.com/build/migrate-to-built-in-kotlin) — convention-plugin removal of `org.jetbrains.kotlin.android`. HIGH.
- [Update your Kotlin projects for AGP 9.0 (JetBrains)](https://blog.jetbrains.com/kotlin/2026/01/update-your-projects-for-agp9/) — `kotlinOptions{}` → `kotlin.compilerOptions{}`, KGP 2.2.10 runtime dep. HIGH.
- [Dagger 2.59 release](https://github.com/google/dagger/releases/tag/dagger-2.59) — Hilt Gradle plugin AGP-9 support; min AGP 9.0; drops AGP 8 (fixes #4944, #4979). HIGH.
- [Dagger #5099 — ComponentTreeDeps missing in 2.59](https://github.com/google/dagger/issues/5099) — use 2.59.1. MEDIUM (fix-version confirmation pending).
- [Dagger #5122 — slow incremental builds on AGP 9 / Hilt 2.59.1](https://github.com/google/dagger/issues/5122) — perf watch. MEDIUM.
- [androidx Benchmark releases](https://developer.android.com/jetpack/androidx/releases/benchmark) — benchmark 1.4.1 stable (Sep 2025); baseline-profile plugin AGP-9 note is stale ("max recommended 9.0.0-alpha01"). MEDIUM on exact AGP-9-GA-safe version.
- [SonarQube/BouncyCastle EdEC NoClassDefFoundError discussion](https://community.sonarsource.com/t/sonarqube-gradle-plugin-6-0-1-5171-org-bouncycastle-asn1-edec-edecobjectidentifiers/131770) — confirms EdEC error is an old-bcprov-shadowing-new classpath conflict, not AGP-version-gated. MEDIUM.
- Repo files read directly (HIGH): `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties`, `gradle.properties`, `settings.gradle.kts`, all five convention plugins + `KotlinAndroid.kt`, `app/build.gradle.kts`, `baselineprofile/build.gradle.kts`, both `ci.yml`.
