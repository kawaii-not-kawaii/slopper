# Pitfalls Research

**Domain:** AGP 8.7.3 → 9.x / Gradle 8.11.1 → 9.1+ / compileSdk 35 → 36 toolchain upgrade of a convention-plugin-based multi-module Compose app (Slopper)
**Researched:** 2026-05-30
**Confidence:** HIGH (AGP 9.0 release notes, Dagger issue tracker #4944/#4979/#5083/#5098/#5099, JetBrains AGP-9 migration guidance, repo build-logic inspection)

> **Scope note.** This is a *pure toolchain* milestone. Most pitfalls below are build-system failures, not runtime bugs. The repo is in better shape than the generic AGP-9 horror stories: it already uses `androidComponents.onVariants` (not `applicationVariants`), `layout.buildDirectory` (not `buildDir`), no `Project.exec`, and has `org.gradle.configuration-cache=true` on already. The real blast radius is concentrated in `build-logic/convention/` — which is exactly why a single break fails every module at once.

> **Suggested phase shorthand used below** (the roadmapper will name these): **P1-CORE-BUMP** (AGP/Gradle/Kotlin/KSP versions + wrapper), **P2-BUILD-LOGIC** (built-in-Kotlin migration of convention plugins, compileSdk 36), **P3-HILT-MEDIA3** (Hilt 2.59.2 + Media3 1.10 + nextlib lockstep + leaf libs), **P4-CI-SIGNING** (re-evaluate the BouncyCastle assembleDebug gate).

---

## Critical Pitfalls

### Pitfall 1: Double-application of `org.jetbrains.kotlin.android` under built-in Kotlin

**What goes wrong:**
AGP 9.0 enables built-in Kotlin by default (`android.builtInKotlin` defaults `false`→`true`) and registers the `kotlin` extension itself. Every convention plugin in this repo *also* applies the Kotlin plugin programmatically:
- `AndroidApplicationConventionPlugin.kt:13` → `apply("org.jetbrains.kotlin.android")`
- `AndroidLibraryConventionPlugin.kt:14` → `apply("org.jetbrains.kotlin.android")`
- plus the `root build.gradle.kts` and `libs.versions.toml` `kotlin-android` alias, and `baselineprofile/build.gradle.kts` `alias(libs.plugins.kotlin.android)`.

The build fails with one of:
```
Failed to apply plugin 'org.jetbrains.kotlin.android'.
  > Cannot add extension with name 'kotlin', as there is an extension already registered with that name.
```
or
```
Failed to apply plugin 'org.jetbrains.kotlin.android'
  > The 'org.jetbrains.kotlin.android' plugin is no longer required for Kotlin support since AGP 9.0.
```

**Why it happens:**
Two code paths both try to register the `kotlin` project extension. The convention-plugin indirection hides the duplicate application from a quick `build.gradle.kts` grep — you have to know the plugins are applied inside `build-logic`.

**How to avoid:**
Remove `apply("org.jetbrains.kotlin.android")` from BOTH convention plugins, remove the `kotlin-android` plugin alias from the root `build.gradle.kts` and from `baselineprofile/build.gradle.kts`, and delete the `kotlin-android` entry from `libs.versions.toml [plugins]`. Keep `kotlin = "2.2.x"` version (still needed for the compose-compiler and serialization plugins, which DO still apply separately). Escape hatch if P2 isn't ready: set `android.builtInKotlin=false` in `gradle.properties` to defer — but this is removed in AGP 10, so treat as temporary tech debt only.

**Warning signs:**
First module to configure fails at `:app` (or `:core:common`) configuration time, before any compilation. The error names the `kotlin` extension collision.

**Phase to address:** P2-BUILD-LOGIC (must land in the same commit/phase as the AGP 9 bump — they are not independently green).

---

### Pitfall 2: `KotlinAndroidProjectExtension` / `compilerOptions` configuration block breaks

**What goes wrong:**
`KotlinAndroid.kt:47` does `extensions.configure<KotlinAndroidProjectExtension> { jvmToolchain(17); compilerOptions { jvmTarget.set(JVM_17); freeCompilerArgs.addAll(...) } }`. Under built-in Kotlin the Kotlin DSL surface moves: AGP's migration guide steers config to the `kotlin { compilerOptions { } }` block, and the old `kotlinOptions {}` android DSL is gone. The `KotlinAndroidProjectExtension` import path (`org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension`) comes from KGP; with built-in Kotlin the extension may still resolve but the documented, supported path is `kotlin { compilerOptions { } }`. If the extension isn't registered as expected you get an `UnknownDomainObjectException` / `Extension of type 'KotlinAndroidProjectExtension' does not exist`.

**Why it happens:**
The convention plugin reaches into KGP's extension type directly. Built-in Kotlin is AGP-owned, not KGP-applied, so the type wiring changes.

**How to avoid:**
Rewrite `configureKotlinAndroid` to use the `kotlin { compilerOptions { } }` DSL. Note from the migration guide: `jvmTarget` now **defaults to `android.compileOptions.targetCompatibility`**, so the explicit `JVM_17` set becomes redundant once `compileOptions` is `VERSION_17` (which it already is). Keep `freeCompilerArgs` (the `-opt-in` flags) and `jvmToolchain(17)`. Verify the `import` resolves against the version pulled by AGP 9 (KGP 2.2.10 floor).

**Warning signs:**
`Extension of type 'KotlinAndroidProjectExtension' does not exist` or `Unresolved reference` on `kotlinOptions`/`compilerOptions` at convention-plugin compile time (the `build-logic` project fails to compile before any app module configures).

**Phase to address:** P2-BUILD-LOGIC.

---

### Pitfall 3: `android.newDsl=true` BaseExtension ClassCastException (the real "legacy variant" trap)

**What goes wrong:**
The original question framed this as "`enableLegacyVariantApi` safe short-term vs tech-debt." **That flag does not exist in AGP 9.0.** The legacy variant API (`applicationVariants`, `libraryVariants`, `testVariants`, `unitTestVariants`) is *fully removed*, and `android.newDsl` defaults `false`→`true`. Old DSL types like `BaseExtension` are now hidden, producing:
```
java.lang.ClassCastException: class com.android.build.gradle.internal.dsl.ApplicationExtensionImpl$AgpDecorated_Decorated
  cannot be cast to class com.android.build.gradle.BaseExtension
```
This is the exact failure class that broke the Hilt plugin (Dagger #4944: "BaseExtension wasn't created when the Hilt plugin checked for it"; #4979: legacy variant API usage).

**Why it happens:**
Third-party Gradle plugins (older Hilt, Wire, etc.) probed `BaseExtension`/`applicationVariants`. AGP 9 removed those access points.

**How to avoid:**
**Good news for this repo:** it does NOT use the legacy variant API. `app/build.gradle.kts` uses `signingConfigs {}` + `buildTypes {}` DSL (fine), and `baselineprofile/build.gradle.kts` already uses the modern `androidComponents { onVariants { variant -> ... } }` API. So the repo's *own* code is clean. The exposure is entirely via **plugins**: Hilt (see Pitfall 5) and the baseline-profile plugin. The escape hatch `android.newDsl=false` exists but is removed in AGP 10 — do NOT rely on it; instead bump the offending plugins. Audit any custom `extensions.getByType(CommonExtension::class.java)` usage (`AndroidComposeConventionPlugin.kt:16`) — `CommonExtension` is still supported in the new DSL, so that line is fine, but confirm at build time.

**Warning signs:**
`ClassCastException ... BaseExtension` or `NoClassDefFoundError: com/android/build/api/artifact/ScopedArtifact$POST_COMPILATION_CLASSES` (the Dagger #5098 symptom of a too-old plugin against AGP 9).

**Phase to address:** P3-HILT-MEDIA3 (plugin bumps) — but verify in P2 once AGP 9 is live, since the baseline-profile plugin applies to `:app` and `:baselineprofile`.

---

### Pitfall 4: Hilt 2.59 `ComponentTreeDeps` missing-class regression — pin 2.59.2, not "2.59+"

**What goes wrong:**
Dagger **2.59** is the release that adds AGP-9 support (the milestone's unblocker), **but 2.59.0 shipped broken**: the Gradle plugin generates code referencing `dagger.hilt.internal.componenttreedeps.ComponentTreeDeps`, which was **not included in the published runtime artifact** (Dagger #5099). Build fails:
```
error: package dagger.hilt.internal.componenttreedeps does not exist
  import dagger.hilt.internal.componenttreedeps.ComponentTreeDeps;
```
Fixed in **Dagger 2.59.2** (which also fixed a `jetifierEnabled=true` compile error under AGP 9).

**Why it happens:**
The milestone framing says "Hilt 2.56.2 → 2.59+". A naive `hilt = "2.59"` or `"2.59.1"` lands on the broken artifact.

**How to avoid:**
Pin `hilt = "2.59.2"` (or later) exactly in `libs.versions.toml`. Both `hilt-android` and `hilt-compiler` libraries and the `hilt` plugin share this `version.ref`, so one bump covers all three — good. Hilt 2.59 **requires** AGP 9 + Gradle 9.1 (it will NOT work on AGP 8.7.3), so this bump is locked to P1/P2 — never bump Hilt before AGP. The convention `AndroidHiltConventionPlugin.kt` applies KSP + the Hilt plugin and adds `ksp(hilt-compiler)` — that wiring stays the same; built-in Kotlin does not change KSP application.

**Warning signs:**
`package dagger.hilt.internal.componenttreedeps does not exist` during `:feature:*` or `:app` Kotlin/KSP compilation; or `NoClassDefFoundError: ...ScopedArtifact$POST_COMPILATION_CLASSES` if Hilt 2.59 is paired with an AGP < 9.

**Phase to address:** P3-HILT-MEDIA3 (but Hilt cannot go green until AGP 9 lands in P1/P2; sequence Hilt strictly after the core bump).

---

### Pitfall 5: KSP / Kotlin / compose-compiler lockstep — the exact-version pin still rules

**What goes wrong:**
The repo pins `ksp = "2.2.20-2.0.4"` to the EXACT Kotlin `2.2.20`. AGP 9.0 carries a runtime dependency on **KGP 2.2.10 (floor)** and will *auto-upgrade* any KSP below `2.2.10-2.0.2` to match KGP. The compose-compiler plugin (`org.jetbrains.kotlin.plugin.compose`, applied in `AndroidComposeConventionPlugin.kt:14`) must also match the Kotlin version exactly. If Kotlin, KSP, and compose-compiler drift apart (e.g. a Kotlin patch bump without the matching KSP suffix), KSP fails plugin application or the compose compiler errors `This version of the Compose Compiler requires Kotlin version X but you appear to be using Y`.

**Why it happens:**
Three plugins are version-welded to Kotlin. The existing Dependabot exclusion (`org.jetbrains.kotlin*`, `com.google.devtools.ksp*`) exists precisely because of this — but a manual DEPS pass can still mismatch them by hand.

**How to avoid:**
Bump Kotlin + KSP + compose-compiler-plugin as one atomic change. The repo already routes `plugin-compose` and `plugin-ksp` through `version.ref = "kotlin"`/`"ksp"` — keep them synchronized. Current `kotlin = "2.2.20"` is **above** the AGP 9 KGP floor (2.2.10), so the existing pin is compatible; you do NOT need Kotlin 2.3 for AGP 9 (Kotlin 2.3 is what *KSP 2.3.x* requires, a separate axis). Decision point: stay on Kotlin 2.2.20 (lower risk, satisfies AGP 9) for this milestone; defer Kotlin 2.3 to a later pass.

**Warning signs:**
`KSP plugin loaded for ... but the project uses Kotlin ...` mismatch; or compose-compiler Kotlin-version assertion failure at module configuration.

**Phase to address:** P1-CORE-BUMP (Kotlin/KSP) — verified atomic before anything else.

---

### Pitfall 6: compileSdk 35 → 36 hardcoded in three places; the AAR `minCompileSdk` consumer rule

**What goes wrong:**
`compileSdk = 35` is hardcoded in `KotlinAndroid.kt:21` (covers all library + app modules via the shared `configureKotlinAndroid`), and again in `baselineprofile/build.gradle.kts` (`compileSdk = 35`, which does NOT go through the convention plugin). Miss any one and you get split-SDK build inconsistency. Separately, AGP 9 adds a new default: **a library consumer must use the same or higher compileSdk than the library** (controlled by `AarMetadata.minCompileSdk`). Media3 1.10 and activity-compose 1.13 ship compiled against SDK 36, so a module still on 35 will be rejected:
```
Dependency '...' requires libraries and applications that depend on it to compile against version 36 or later of the Android APIs.
```

**Why it happens:**
The SDK level is set in code, not a catalog value, and the baselineprofile module is a separate touchpoint outside the convention plugin (a known historical 4th-touchpoint footgun — see DEPS Phase 1 notes).

**How to avoid:**
Change `compileSdk` to 36 in `KotlinAndroid.kt` AND `baselineprofile/build.gradle.kts` in the same commit. Keep `minSdk = 26` and `targetSdk = 35` unchanged (PROJECT.md constraint: no `minSdk`/runtime-behavior bump; compileSdk is build-only). Ensure build-tools 36.0.0 is available (AGP 9 default/min). Consider lifting `compileSdk`/`targetSdk` into `libs.versions.toml` to make this a single source of truth and kill the multi-touchpoint risk permanently.

**Warning signs:**
`requires ... compile against version 36 or later`; or runtime drift where one module compiles SDK 35 and another 36.

**Phase to address:** P2-BUILD-LOGIC (compileSdk 36 must precede the Media3/activity leaf bumps in P3).

---

### Pitfall 7: Media3 1.10 ↔ nextlib-media3ext exact-version lockstep (ABI/decoder breakage)

**What goes wrong:**
`nextlib-media3ext` carries a compound version `<media3>-<nextlib>` (currently `1.9.1-0.11.0`). It bundles a prebuilt FFmpeg `.so` ABI compiled against a specific Media3 decoder/renderer API. Mismatching Media3 and nextlib versions causes either a Gradle resolution conflict or — worse — a runtime `UnsatisfiedLinkError` / `NoSuchMethodError` in the FFmpeg renderer that only surfaces when a software-decoded codec actually plays (not at build time).

**Why it happens:**
The two artifacts are released in lockstep but live in different groups (`androidx.media3:*` vs `io.github.anilbeesetti:*`), and Dependabot was deliberately excluded for both — so they must be hand-bumped together.

**How to avoid:**
Bump Media3 `1.9.1 → 1.10.0` and nextlib to the matching `1.10.0-0.12.1` (verified current matching pair on Maven Central) in the same commit. Confirm the nextlib version string's Media3 prefix equals the `media3` version exactly. Media3 1.10 expects compileSdk 36 — so this is gated behind Pitfall 6. After bumping, do a real device playback smoke test of a software-decoded codec (the FFmpeg path), since the breakage is runtime-only.

**Warning signs:**
Gradle version conflict on `androidx.media3:*`; or at runtime `java.lang.UnsatisfiedLinkError` / `NoSuchMethodError` in `NextRenderersFactory`/FFmpeg decoder when a non-hardware codec plays.

**Phase to address:** P3-HILT-MEDIA3.

---

### Pitfall 8: Multi-module blast radius — a convention-plugin break fails ALL modules at once

**What goes wrong:**
Every module routes through `build-logic/convention`. A single mistake in `KotlinAndroid.kt`, `AndroidApplicationConventionPlugin`, or `AndroidLibraryConventionPlugin` fails configuration for `:app`, all `:core:*`, all `:feature:*`, AND `:baselineprofile` simultaneously. You can't tell which change broke what because nothing configures.

**Why it happens:**
The convention-plugin pattern centralizes config (its strength) but also centralizes failure (its risk). All of Pitfalls 1, 2, 6 live in `build-logic`.

**How to avoid (staging strategy):**
1. **Bump the toolchain VERSIONS first (P1)** with built-in Kotlin temporarily OFF (`android.builtInKotlin=false`) and `android.newDsl=false` — get a green build on AGP 9 + Gradle 9.1 with the *old* DSL still working. This isolates "did the version bump break anything" from "did the DSL migration break anything."
2. **Migrate build-logic next (P2)**, one concern at a time, removing the opt-out flags last: remove `kotlin.android` application → green; migrate the `kotlin{}` DSL → green; flip compileSdk 36 → green; then drop `builtInKotlin=false`/`newDsl=false`.
3. **Validate against a single small library module first** by running `./gradlew :core:common:compileDebugKotlin` before a full build — the convention plugin applies there too, so a failure reproduces fast without compiling the whole graph.
4. Keep each step a separate commit so `git bisect` works if CI goes red.

**Warning signs:**
"All modules failed configuration" with an error originating from a `io.stashapp.android.buildlogic.*` stack frame.

**Phase to address:** Cross-cutting — drives the P1→P2 sequencing (versions before DSL migration).

---

### Pitfall 9: BouncyCastle EdEC signing — the toolchain bump does NOT fix it, and may worsen it

**What goes wrong:**
The known landmine: `:app:validateSigningDebug` throws `NoClassDefFoundError: org/bouncycastle/asn1/edec/EdECObjectIdentifiers` on CI runners (GitHub/Forgejo), unreproducible locally. Current mitigation: CI is a compile-only gate (no `assembleDebug`/signing). The question: does AGP 9 make re-enabling `assembleDebug` in CI safer?

**Assessment (MEDIUM confidence):** AGP 9's apksig still depends on BouncyCastle for EdEC (Ed25519) OIDs; the root cause is a **classpath/version skew of `bcprov`** on the runner (a BouncyCastle on the classpath too old to contain `EdECObjectIdentifiers`, or a `NoClassDefFoundError` from a partially-shaded BC). AGP 9 bundles its own (newer) apksig + BC, so the bump *could* incidentally help — but it equally **could regress**: AGP 9 / Gradle 9.1 changes the plugin classpath and may surface a *different* BC version conflict. JDK is unchanged (17 toolchain / 21 runtime), so the JDK axis doesn't shift. Treat AGP 9 as **neutral-to-risky**, not a fix.

**How to avoid:**
Do NOT re-add `assembleDebug` to CI as part of the toolchain bump. Keep the compile-only gate through P1–P3. In a *dedicated* P4-CI-SIGNING spike, on the AGP-9 toolchain: (a) run `assembleDebug` on a CI runner and capture the full BC stack trace; (b) if it still fails, force a single `bcprov-jdk18on` version via a `resolutionStrategy` on the buildscript classpath, or exclude the stale transitive BC; (c) only flip CI to assemble after a green run is demonstrated. The debug keystore fallback (`signingConfig = signingConfigs.getByName("debug")`) is already wired, so the signing path is exercised — the issue is purely the BC class on the runner classpath.

**Warning signs:**
`NoClassDefFoundError: org/bouncycastle/asn1/edec/EdECObjectIdentifiers` (or `.../asn1/cms/CMSObjectIdentifiers`) during `validateSigning*`/`package*` on CI only.

**Phase to address:** P4-CI-SIGNING (isolated, last — never bundle into the toolchain bump; it must not block a green AGP-9 landing).

---

### Pitfall 10: Gradle 9 deprecated-feature removals (the existing "incompatible with Gradle 9.0" warnings)

**What goes wrong:**
The project already emits "Deprecated Gradle features ... incompatible with Gradle 9.0." Since the obvious culprits (`buildDir`, `Project.exec`) are NOT used in this repo's scripts (verified — the build uses `layout.buildDirectory` and has no `.exec(`), the warnings most likely originate from: (a) **third-party Gradle plugins** still calling deprecated Gradle APIs — `dependency-check` (OWASP) is already flagged config-cache-incompatible in the root build and is a prime suspect; ktlint/detekt plugins are others; (b) deprecated `Convention` API usage inside an applied plugin; (c) the `org.owasp.dependencycheck` plugin's incompatibility (the root build already documents `--no-configuration-cache` for it). Under Gradle 9 a warning can become a hard `BUILD FAILED`.

**Why it happens:**
Plugins authored against Gradle 8 used APIs Gradle 9 removed (e.g. the `Convention` API, mutable-project-state-at-execution).

**How to avoid:**
Before bumping the wrapper, run `./gradlew help --warning-mode=all --stacktrace` on Gradle 8.11.1 to *enumerate* the exact deprecations and the plugin each comes from. Then bump those plugins (detekt, ktlint, dependency-check, baseline-profile) to Gradle-9-compatible versions as part of P1. The OWASP dependency-check plugin is the highest-risk one: it is already config-cache-incompatible; verify a Gradle-9-compatible release exists (`dependencyCheck = "12.2.2"` may need a bump) or keep running it with `--no-configuration-cache` (it's a separate task, not in the main build path). Note: `org.gradle.configuration-cache=true` is already ON, so Gradle 9's stricter config-cache enforcement is partly pre-validated — but Gradle 9 promotes more config-cache problems from warning to error.

**Warning signs:**
`--warning-mode=all` lines naming a plugin; after the bump, `An issue was found ... incompatible with the configuration cache` as a hard failure.

**Phase to address:** P1-CORE-BUMP (enumerate + bump plugins alongside the Gradle wrapper).

---

## Technical Debt Patterns

| Shortcut | Immediate Benefit | Long-term Cost | When Acceptable |
|----------|-------------------|----------------|-----------------|
| `android.builtInKotlin=false` to defer the built-in-Kotlin migration | AGP 9 lands without touching `build-logic` | Removed in AGP 10 (mid-2026); blocks the next bump | Only as a P1 staging step to isolate version-bump failures; must be removed by end of P2 |
| `android.newDsl=false` to keep old DSL types | Old plugins that probe `BaseExtension` keep working | Removed in AGP 10; masks plugins needing upgrade | Only transiently in P1; bump the plugins instead |
| Keeping `dependencyCheck` on `--no-configuration-cache` | Avoids fighting an incompatible plugin | Slower OWASP scans; perpetual exception | Acceptable indefinitely — it's an out-of-band CI task, not in the main build |
| Leaving compileSdk in code (3 touchpoints) instead of the catalog | No refactor needed | Re-introduces the "forgot baselineprofile" footgun every bump | Never — lift to `libs.versions.toml` during P2 |
| Pinning Hilt to `2.59` exactly | Matches milestone framing | Hits the broken `ComponentTreeDeps` artifact | Never — use `2.59.2`+ |

## Integration Gotchas

| Integration | Common Mistake | Correct Approach |
|-------------|----------------|------------------|
| Hilt Gradle plugin + AGP 9 | Bumping Hilt before AGP, or to 2.59.0/2.59.1 | Bump AGP 9 first; pin Hilt **2.59.2+**; Hilt 2.59 hard-requires AGP 9 + Gradle 9.1 |
| nextlib-media3ext + Media3 | Bumping one without the other | Bump as one commit to a matching `1.10.0-0.12.1` ↔ `media3 1.10.0` pair; device-smoke a software codec |
| KSP + Kotlin + compose-compiler | Bumping Kotlin patch without matching KSP/compose suffix | Atomic 3-way bump; current 2.2.20 already satisfies AGP 9's KGP 2.2.10 floor |
| OWASP dependency-check + Gradle 9 | Expecting it to run in the config-cached main build | Keep it isolated with `--no-configuration-cache`; bump to a Gradle-9-tested release |
| baseline-profile plugin + AGP 9 | Assuming it uses legacy variant API | Repo already uses `androidComponents.onVariants`; just bump `baselineProfilePlugin`/`benchmark` to AGP-9-compatible versions |

## Performance Traps

| Trap | Symptoms | Prevention | When It Breaks |
|------|----------|------------|----------------|
| Config-cache invalidation from build-logic changes | Every convention-plugin edit re-runs full configuration | Expected during P2; not a regression — config cache re-validates after stabilization | During active build-logic migration only |
| `org.gradle.workers.max=2` + Gradle 9 parallel config | Slower CI on AGP 9's parallel project configuration | Leave as-is (12GB host cap); not a correctness issue | N/A — capacity tuning, not a blocker |

## Security Mistakes

| Mistake | Risk | Prevention |
|---------|------|------------|
| Re-enabling CI `assembleDebug` and shipping the debug-keystore fallback as if signed | A "signed" CI artifact actually carries the debug key | Keep release signing env-gated; CI artifact remains debug-only and clearly labeled |
| Suppressing the BC `NoClassDefFoundError` by broadly excluding BouncyCastle | Could disable real signature verification | Pin a *correct* `bcprov-jdk18on` version, don't blanket-exclude |

## UX Pitfalls

*(Not applicable — pure toolchain milestone, no user-facing surface changes. PROJECT.md constraint: no behavior change.)*

## "Looks Done But Isn't" Checklist

- [ ] **AGP 9 build green:** Often missing — verify with `builtInKotlin`/`newDsl` opt-outs *removed*, not just defaulted off. A build green only with the escape hatches on is not done.
- [ ] **compileSdk 36:** Often missing the `baselineprofile/build.gradle.kts` touchpoint — verify all 3 SDK locations changed (`grep -rn "compileSdk" --include=*.kts --include=*.kt`).
- [ ] **Hilt:** Often green at compile but pinned to 2.59.0 — verify the catalog says **2.59.2+** and KSP-generated Hilt code compiles in a `:feature:*` module.
- [ ] **Media3/nextlib:** Often green at build but never run — verify a **device** software-codec playback (runtime-only failure surface).
- [ ] **Gradle 9 deprecations:** Often "warnings ignored" — verify `--warning-mode=all` is clean (or each remaining warning is a known, tracked plugin).
- [ ] **CI gate:** Often silently still compile-only — confirm whether P4 re-enabled `assembleDebug` or *deliberately* kept the compile-only gate (document the decision).

## Recovery Strategies

| Pitfall | Recovery Cost | Recovery Steps |
|---------|---------------|----------------|
| Double-application error (P1) | LOW | Remove the `kotlin.android` applications from convention plugins + root + catalog; one commit |
| `KotlinAndroidProjectExtension` break (P2) | LOW | Rewrite `configureKotlinAndroid` to `kotlin { compilerOptions { } }`; drop redundant `jvmTarget` |
| Hilt ComponentTreeDeps (P4) | LOW | Bump catalog `hilt` to `2.59.2`; clean + rebuild |
| Media3/nextlib mismatch (P7) | MEDIUM | Realign both to `1.10.0`/`1.10.0-0.12.1`; device re-test |
| BC signing on CI (P9) | HIGH | Isolated spike: capture stack trace, pin `bcprov-jdk18on`, or keep compile-only gate (revert is the cheap fallback) |
| Whole-graph configuration failure (P8) | MEDIUM | `git bisect` across the per-concern commits; that's why P8 mandates one-concern-per-commit |

## Pitfall-to-Phase Mapping

| Pitfall | Prevention Phase | Verification |
|---------|------------------|--------------|
| 5: KSP/Kotlin/compose lockstep | P1-CORE-BUMP | `./gradlew :core:common:compileDebugKotlin` green on AGP 9 + Gradle 9.1 |
| 10: Gradle 9 deprecations | P1-CORE-BUMP | `./gradlew help --warning-mode=all` clean or tracked |
| 8: Multi-module blast radius | P1→P2 sequencing | Each concern is a separate green commit; small-module compile first |
| 1: kotlin.android double-application | P2-BUILD-LOGIC | Full configure succeeds with `builtInKotlin` opt-out removed |
| 2: KotlinAndroidProjectExtension DSL | P2-BUILD-LOGIC | `build-logic` compiles; opt-in args + toolchain still applied |
| 3: newDsl / legacy-variant removal | P2 (verify) / P3 (plugin bumps) | No `ClassCastException: BaseExtension`; `newDsl` opt-out removed |
| 6: compileSdk 36 (3 touchpoints) | P2-BUILD-LOGIC | All 3 SDK sites = 36; AAR `minCompileSdk` consumers resolve |
| 4: Hilt 2.59.2 pin | P3-HILT-MEDIA3 | `:feature:*` KSP/Hilt compile green; catalog ≥ 2.59.2 |
| 7: Media3/nextlib lockstep | P3-HILT-MEDIA3 | Matching version pair + device software-codec playback |
| 9: BouncyCastle CI signing | P4-CI-SIGNING (isolated) | Either a demonstrated green `assembleDebug` on CI, or a documented decision to keep the compile-only gate |

## Sources

- [Android Gradle plugin 9.0 release notes](https://developer.android.com/build/releases/agp-9-0-0-release-notes) — built-in Kotlin default, legacy variant API removal (no `enableLegacyVariantApi`), `android.newDsl` + `BaseExtension` ClassCastException, Gradle 9.1.0 min / JDK 17 min / KGP 2.2.10, compileSdk up to 36.1, AAR `minCompileSdk` consumer rule, build-tools 36.0.0 — HIGH
- [Migrate to built-in Kotlin (Android)](https://developer.android.com/build/migrate-to-built-in-kotlin) — exact plugins to remove, double-application error strings, KSP works without kotlin-android, `kotlin{compilerOptions}` replaces `kotlinOptions`, jvmTarget defaults to targetCompatibility — HIGH
- [JetBrains: Update your Kotlin projects for AGP 9.0](https://blog.jetbrains.com/kotlin/2026/01/update-your-projects-for-agp9/) — `builtInKotlin=false` temporary opt-out, removed in AGP 10 — HIGH
- [Dagger #5099 — Hilt 2.59 ComponentTreeDeps missing](https://github.com/google/dagger/issues/5099) — `package dagger.hilt.internal.componenttreedeps does not exist`, fixed in **2.59.2** — HIGH
- [Dagger #4944](https://github.com/google/dagger/issues/4944) / [#4979](https://github.com/google/dagger/issues/4979) / [#5083](https://github.com/google/dagger/issues/5083) — Hilt plugin AGP-9 legacy-variant/BaseExtension failures, fixed in 2.59 — HIGH
- [Dagger #5098](https://github.com/google/dagger/issues/5098) — `NoClassDefFoundError: ScopedArtifact$POST_COMPILATION_CLASSES` symptom of Hilt 2.59 on too-old AGP; AGP 9 hard requirement — HIGH
- [Release Dagger 2.59](https://github.com/google/dagger/releases/tag/dagger-2.59) — AGP 9 + Gradle 9.1 now required — HIGH
- [nextlib-media3ext on Maven Central](https://central.sonatype.com/artifact/io.github.anilbeesetti/nextlib-media3ext) / [Media3 1.10 is out](https://android-developers.googleblog.com/2026/03/media3-110-is-out.html) — matching pair `1.10.0-0.12.1` ↔ Media3 1.10.0 — MEDIUM
- [BouncyCastle EdECObjectIdentifiers NoClassDefFoundError discussions](https://github.com/bcgit/bc-java/issues/1502) — root cause is `bcprov` classpath/version skew, JDK-independent — MEDIUM
- Repo inspection: `build-logic/convention/*`, `app/build.gradle.kts`, `baselineprofile/build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, `.github/dependabot.yml`, `.continue-here.md` anti-patterns table — HIGH

---
*Pitfalls research for: AGP-9 toolchain upgrade of a convention-plugin multi-module Compose app (Slopper v1.1 / DEPS-17)*
*Researched: 2026-05-30*
