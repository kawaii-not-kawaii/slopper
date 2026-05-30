# Migration Surface Research (AGP 9 / Gradle 9 / compileSdk 36)

**Domain:** Pure build-toolchain upgrade — Android Compose multi-module app
**Researched:** 2026-05-30
**Confidence:** HIGH (verified against AGP 9.0/9.1/9.2 release notes + built-in-Kotlin migration guide; MEDIUM on Apollo/baseline-profile plugin specifics)

> This milestone ships **no end-user features**. The "feature landscape" below is repurposed
> into the **migration work surface**: the concrete BUILD/CONFIG/API changes AGP 9 + Gradle 9 +
> compileSdk 36 force on *this* repo. Every item maps to a real file. Classification:
> **REQUIRED** (build breaks without it) / **RECOMMENDED** (works but should change) / **OPTIONAL**.

## Target toolchain (verified current, 2026-05-30)

| Component | This repo today | Target | Floor enforced by AGP 9 |
|-----------|-----------------|--------|--------------------------|
| AGP | 8.7.3 | **9.2.0** (GA Apr 2026; latest stable) | — |
| Gradle | 8.11.1 | **9.1.0+** | AGP 9.1+ requires Gradle **9.1.0** minimum |
| Kotlin (KGP) | 2.2.20 | 2.2.20 OK | AGP 9 runtime-depends on KGP **2.2.10**; auto-upgrades anything lower |
| compileSdk | 35 | **36** | Build Tools **36.0.0**, max compileSdk **36.1** |
| JDK | 17 (toolchain) | 17 | AGP 9 minimum **JDK 17** — already satisfied |
| KSP | 2.2.20-2.0.4 | realign to KGP | exact-Kotlin lockstep (existing PITFALL §3) |
| Hilt/Dagger | 2.56.2 | **2.59+** | the unblocker — adds AGP-9 support |

Decision: target **AGP 9.2.0 + Gradle 9.1.x**. 9.2 is the latest stable and the only line that
supports API 36.1; staying on 9.0/9.1 buys nothing and re-incurs a later bump.

---

## REQUIRED Work Items (build breaks without these)

These map to question (1) built-in Kotlin, (2) legacy variant API, (3) compileSdk 36, (4) Gradle 9,
(5) gradle.properties, (6) convention-plugin DSL, (7) KSP/Apollo/Hilt.

| # | Work Item | File(s) | How (one line) | Maps to Q |
|---|-----------|---------|----------------|-----------|
| R1 | Bump AGP → 9.2.0 | `gradle/libs.versions.toml` (`agp`) | `agp = "9.2.0"` | core |
| R2 | Bump Gradle wrapper → 9.1.x | `gradle/wrapper/gradle-wrapper.properties` | `distributionUrl=…gradle-9.1.1-bin.zip` (run `./gradlew wrapper` twice) | 4 |
| R3 | Bump Hilt/Dagger → 2.59+ | `gradle/libs.versions.toml` (`hilt`) | the AGP-9 unblocker | 7 |
| R4 | Realign KSP to Kotlin | `gradle/libs.versions.toml` (`ksp`) | match KSP build to `kotlin` version exactly (lockstep) | 7 |
| R5 | **Remove `org.jetbrains.kotlin.android` from convention plugins** | `AndroidApplicationConventionPlugin.kt` L13, `AndroidLibraryConventionPlugin.kt` L14 | delete the `apply("org.jetbrains.kotlin.android")` line — AGP 9 applies Kotlin itself (`android.builtInKotlin=true` default) | 1 |
| R6 | Remove `kotlin.android` from root + module plugin blocks | root `build.gradle.kts` L5, `baselineprofile/build.gradle.kts` L4 | drop `alias(libs.plugins.kotlin.android)` / `apply false` entries | 1 |
| R7 | Remove `kotlin-android` alias from catalog | `gradle/libs.versions.toml` L150 + `plugin-kotlin` classpath L143 | delete `kotlin-android = …`; KGP no longer applied by us | 1 |
| R8 | **Fix `CommonExtension` parameterization** | `KotlinAndroid.kt` L18 (`CommonExtension<*, *, *, *, *, *>`) | AGP 9 removed the type params → `CommonExtension` (no generics). Compilation error otherwise. | 6 |
| R9 | **Migrate `KotlinAndroidProjectExtension` config** | `KotlinAndroid.kt` L47-56 | built-in Kotlin: configure via the `kotlin {}`/`KotlinAndroidProjectExtension` still resolvable, but verify `getByType` import + `compilerOptions` still applies; `jvmTarget` now defaults to `compileOptions.targetCompatibility` (can drop explicit set) | 1,6 |
| R10 | **Replace `kotlinOptions {}`** | `baselineprofile/build.gradle.kts` L15-17 | `kotlinOptions{}` DSL removed → move `jvmTarget` to `kotlin { compilerOptions { jvmTarget.set(JVM_17) } }` (or omit — defaults to targetCompatibility) | 1 |
| R11 | **Bump `compileSdk` 35 → 36** (3 touchpoints) | `KotlinAndroid.kt` L21, `baselineprofile/build.gradle.kts` L9 | `compileSdk = 36` everywhere; convention plugin covers app+libs+features, baseline module is standalone | 3 |
| R12 | Verify `targetSdk` stays 35 (no runtime opt-in) | `AndroidApplicationConventionPlugin.kt` L17, `AndroidLibraryConventionPlugin.kt` L18, `baselineprofile` L21 | keep `targetSdk = 35` — bumping to 36 triggers Android 16 runtime behaviors we must NOT enable; `android.sdk.defaultTargetSdkToCompileSdkIfUnset=true` is now default, so **targetSdk must stay explicitly set** or it silently becomes 36 | 3 |
| R13 | Apollo plugin AGP-9 / newDsl compat | `core/network/build.gradle.kts`, catalog `apollo`=5.0.0 | Apollo 5 supports AGP 9; verify `android.newDsl=true` doesn't trip issue #6693 ("KotlinSourceSet 'main' not found"). Bump to latest 5.x if it does. | 7 |
| R14 | Baseline-profile plugin AGP-9 bump | `baselineprofile/build.gradle.kts`, catalog `baselineProfilePlugin`=1.4.1 / `benchmark`=1.3.3 | bump `androidx.benchmark` + baseline-profile plugin to AGP-9-aware release; uses `com.android.test` which AGP 9 supports | 2,7 |

### Notes on the legacy-variant-API question (Q2)

**Verified: this repo does NOT use the removed legacy variant API.**
- Grep for `applicationVariants` / `libraryVariants` / `variantFilter` / `buildDir` across all
  `*.gradle.kts` + `*.kt`: **zero hits** outside `/build/`.
- `baselineprofile/build.gradle.kts` L52 already uses the **new** `androidComponents.onVariants {}` API.
- `app/build.gradle.kts` `signingConfigs`/`buildTypes` use the DSL, not the variant callback API.

Therefore: **`android.enableLegacyVariantApi` is a no-op as of AGP 9.0** (the property was
neutralized — `android.newDsl` is the real switch). We do **not** need it, and must **not** add it
as a crutch. The legacy API removal is a non-event here. **The only DSL break is R8** (the
`CommonExtension<*,...>` generics removal). This is the single highest-risk migration item because
it lives in shared build-logic and fails *every* module at once.

### Notes on Gradle 9 (Q4)

- No `Project.buildDir` usage — the compose plugin already uses `layout.buildDirectory` (Gradle 9-safe).
- Configuration cache already **on** (`org.gradle.configuration-cache=true`) — Gradle 9 makes CC the
  path of least resistance; existing setup is forward-compatible.
- `org.gradle.java.installations.auto-download=false` + JDK-17 toolchain already satisfy AGP 9's
  JDK-17 floor.
- Known CC incompatibility: OWASP `dependencyCheck` (root `build.gradle.kts`) already runs with
  `--no-configuration-cache`; unchanged by Gradle 9.

---

## RECOMMENDED Work Items

| # | Work Item | File(s) | How | Maps to Q |
|---|-----------|---------|-----|-----------|
| C1 | Set `android.builtInKotlin=true` explicitly | `gradle.properties` | it's the default in AGP 9, but pinning documents intent and survives an accidental AGP 10 default flip | 1,5 |
| C2 | Drop `compile`-classpath `kotlin` plugin dep in build-logic | `build-logic/convention/build.gradle.kts` L15 (`compileOnly(libs.plugin.kotlin)`) | once R5–R7 land, build-logic no longer references KGP for `apply()`. Keep `compileOnly` only if `KotlinAndroidProjectExtension` types are still imported (they are, in KotlinAndroid.kt) — verify before removing | 1,6 |
| C3 | Verify R8 repackaging change vs ProGuard rules | `app/proguard-rules.pro`, `consumer-rules.pro` | AGP 9.1+ R8 repackages to unnamed package by default (adds `-repackageclasses`); if reflection/Hilt keep-rules assume package names, add `-dontrepackage`. Likely safe (Hilt uses KSP-generated keep rules) but verify release build | 3,4 |
| C4 | Lockstep-bump Media3 1.9.1 → 1.10+ | catalog `media3`, `nextlibMedia3Ext` | Media3 1.10 requires compileSdk 36 (now satisfied); nextlib version-locked | core |
| C5 | Lockstep leaf bumps now unblocked | catalog `coreKtx`→1.18, `activityCompose`→1.13 | both required AGP 8.9.1 + compileSdk 36 floor (per dependabot ignore list) — now landable | core |
| C6 | Re-evaluate CI `assembleDebug` signing gate | CI workflow | BouncyCastle EdEC blocker may now lift under new toolchain — restore a real assemble gate | core |
| C7 | Confirm `targetCompatibility`/`jvmTarget` coherence post-R9/R10 | `KotlinAndroid.kt`, `baselineprofile` | with built-in Kotlin, `jvmTarget` auto-follows `compileOptions.targetCompatibility=17`; explicit JVM_17 sets become redundant but harmless | 1 |

---

## OPTIONAL Work Items

| # | Work Item | File(s) | How | Maps to Q |
|---|-----------|---------|-----|-----------|
| O1 | Remove redundant explicit `jvmTarget` sets | `KotlinAndroid.kt` L52, `baselineprofile` L16 | now auto-derived; deleting reduces drift. Leave if you prefer explicitness | 1 |
| O2 | Update Dependabot ignore list | `.github/dependabot.yml` | once landed, un-ignore `com.android.*`, `androidx.media3:*`, `androidx.activity:*`, `androidx.core:*`, `com.google.dagger*`, `androidx.benchmark*` so leaf updates resume | — |
| O3 | Audit new AGP 9 default-flipped properties for behavior drift | `gradle.properties` | e.g. `android.onlyEnableUnitTestForTheTestedBuildType=true`, `android.r8.optimizedResourceShrinking=true`, `android.uniquePackageNames=true` now default — verify none change current outputs; pin to old value only if a regression appears | 5 |
| O4 | Confirm `android.nonTransitiveRClass` already default | `gradle.properties` L11 | already set `true`; AGP 9 default — line is now redundant but harmless. Namespace already set in every module (R-class non-issue) | 5 |

---

## Work-Item Dependency Graph (build order)

```
R1 (AGP 9.2) ──┬──> R2 (Gradle 9.1)            [AGP 9.1+ HARD-requires Gradle 9.1]
               ├──> R3 (Hilt 2.59) ──> R4 (KSP realign)   [Hilt is the unblocker]
               └──> R8 (CommonExtension generics)  [compiles build-logic against AGP 9 API]
                        └──> R5/R6/R7 (drop kotlin.android)
                                 └──> R9/R10 (kotlinOptions → compilerOptions)
                                          └──> R11 (compileSdk 36)
                                                   └──> R12 (pin targetSdk 35)  [CRITICAL guard]
                                                            ├──> R13 (Apollo newDsl verify)
                                                            └──> R14 (baseline-profile plugin)

After GREEN build:
   C4/C5 (Media3 + leaf bumps) ──> C3 (R8 repackage verify on release)
   C6 (CI gate) ──> O2 (un-ignore Dependabot)
```

**Critical path:** R1 → R8 → R5/R6/R7 → R9/R10 → R11 → R12. These are *atomic* — build-logic
won't compile until R8 + R5–R10 land together, because the convention plugins are shared by every
module. Treat the build-logic surgery (R5–R10) as **one indivisible commit**, not incremental.

**Highest-risk single item:** **R8** (`CommonExtension<*,*,*,*,*,*>` → `CommonExtension`). It is the
one genuine compile break in shared build logic and gates everything downstream.

**Most dangerous silent failure:** **R12**. AGP 9 flips
`android.sdk.defaultTargetSdkToCompileSdkIfUnset` to `true` by default. If any `targetSdk = 35` is
dropped during the compileSdk-36 edit, targetSdk silently becomes 36 → triggers Android 16 runtime
behavior changes the milestone forbids. **targetSdk must remain explicitly 35 in all three places.**

---

## Anti-Items (do NOT do these)

| Anti-Item | Why tempting | Why wrong | Instead |
|-----------|--------------|-----------|---------|
| Add `android.enableLegacyVariantApi=true` | Listed in old migration guides | **No-op in AGP 9.0** — property neutralized; `android.newDsl` is the real switch and we don't use legacy variants anyway | Do nothing — repo uses `androidComponents.onVariants` already |
| Set `android.newDsl=false` to dodge R8 | Avoids touching build-logic | Removed in AGP 10; just defers a 1-line fix; breaks Apollo/other plugins expecting new DSL | Do R8 (drop the `<*,*,...>` generics) |
| Bump `targetSdk` 36 "while we're here" | compileSdk is going to 36 | Opts into Android 16 runtime behaviors — milestone explicitly forbids runtime changes | Keep `targetSdk = 35` (R12) |
| Keep `org.jetbrains.kotlin.android` "to be safe" | Less churn | Allowed in AGP 9 but redundant; the whole point is built-in Kotlin; double-apply risks KGP version conflicts | Remove it (R5–R7) |
| Migrate kapt | — | N/A — repo uses **KSP only** (Hilt + Apollo). No kapt anywhere. | No action |

---

## Sources

- [AGP 9.0.1 release notes (Jan 2026)](https://developer.android.com/build/releases/agp-9-0-0-release-notes) — HIGH (breaking changes, property defaults, removed APIs)
- [AGP 9.1.1 release notes (Apr 2026)](https://developer.android.com/build/releases/agp-9-1-0-release-notes) — HIGH (Gradle 9.1 floor, R8 repackage)
- [AGP 9.2.0 release notes (Apr 2026)](https://developer.android.com/build/releases/agp-9-2-0-release-notes) — HIGH (API 36.1, latest stable)
- [Migrate to built-in Kotlin](https://developer.android.com/build/migrate-to-built-in-kotlin) — HIGH (kotlinOptions→compilerOptions, kapt, builtInKotlin property)
- [Update your Kotlin projects for AGP 9 (JetBrains)](https://blog.jetbrains.com/kotlin/2026/01/update-your-projects-for-agp9/) — MEDIUM
- [Apollo Kotlin issue #6693 (newDsl compat)](https://github.com/apollographql/apollo-kotlin/issues/6693) — MEDIUM (Apollo 5 + AGP 9 newDsl edge case)
- [Dagger issue #4979 (Hilt legacy-variant fix)](https://github.com/google/dagger/issues/4979) — MEDIUM (Hilt AGP-9 unblock context)
- Repo inspection (grep: zero `applicationVariants`/`libraryVariants`/`buildDir` hits) — HIGH
