# Phase 8: AGP-9 — Atomic Build-Logic Migration + compileSdk 36 - Context

**Gathered:** 2026-05-30
**Status:** Ready for planning
**Source:** discuss-phase `--auto` (autonomous, research/STATE/ADR-aligned recommended defaults)

<domain>
## Phase Boundary

This is the **one indivisible phase** of the v1.1 milestone. It lands the atomic AGP-9 critical-path change-set so the `build-logic/convention` choke point configures all ~14–16 modules and the build is green again on AGP 9. The change-set is: **Gradle 9.4.1 wrapper activation (commit 1, folded forward from Phase 7) → AGP 9.2.1 → drop `org.jetbrains.kotlin.android` (AGP-9 built-in Kotlin) → `CommonExtension<*,…>` generics fix → `kotlinOptions{}`→`compilerOptions{}` (baselineprofile only) → Hilt 2.59.2 → compileSdk 36** — with `targetSdk = 35` held explicit everywhere.

**In scope:** the version pins + DSL surgery above, across the convention plugins + root/baselineprofile build scripts + version catalog + Gradle wrapper + CI cache key, ending with the full phase gate (`compileDebugSources + detekt + ktlintCheck + test`) green and the version-isolation opt-out flags removed.

**Out of scope (each is its own phase/decision):** Media3/nextlib + leaf-lib bumps (Phase 9), CI assemble/signing probe (Phase 10), any Kotlin/KSP bump, any `targetSdk` bump, any new dependency or feature, module-graph restructure, and the deferred lint re-enables (DEPS-07).

</domain>

<decisions>
## Implementation Decisions

### Commit sequencing (bisectability)
- **D-01:** Migrate **one concern per commit** in this exact order, even though the build is only green again at the *end* of the sequence — this keeps `git bisect` able to isolate which concern broke configuration:
  1. Gradle wrapper `8.11.1` → `9.4.1` + fresh `distributionSha256Sum` (fetched live from the official `.sha256`, never hardcoded from memory). [folded forward from Phase 7]
  2. AGP `8.7.3` → `9.2.1` (`gradle/libs.versions.toml:3`).
  3. Drop `org.jetbrains.kotlin.android` from all application sites (built-in Kotlin) — see D-05.
  4. `CommonExtension<*,*,*,*,*,*>` generics fix in `KotlinAndroid.kt:16` (the generics arity changed in AGP 9).
  5. `kotlinOptions{}` → `kotlin{ compilerOptions{} }` in `baselineprofile/build.gradle.kts:15` (the ONE lagging site — the convention plugins already use `compilerOptions`).
  6. Hilt/Dagger `2.56.2` → `2.59.2` (`gradle/libs.versions.toml:6`).
  7. compileSdk `35` → `36` (`KotlinAndroid.kt:21` + `baselineprofile/build.gradle.kts:9`).
- **D-02:** Validate the fast choke-point smoke `./gradlew :core:common:compileDebugKotlin` after the AGP + built-in-Kotlin + generics commits, before attempting a full multi-module build. Full gate runs at the end.

### Version pins (resolve the 3 research-flagged disagreements)
- **D-03:** **AGP = `9.2.1`** (latest stable patch; CONFIRM against live Maven metadata at plan time — fall back to `9.2.0` only if `9.2.1` is not published). Gradle floor: AGP 9.2.x's required minimum is satisfied by the pinned `9.4.1`.
- **D-04:** **Hilt/Dagger = `2.59.2`** — pinned exactly, **never bare `2.59+`** (a floating range re-introduces the resolution nondeterminism this milestone is eliminating). 2.59 drops AGP 8 entirely, so it lands strictly *after* AGP 9 in the sequence.
- **D-04b:** **Kotlin `2.2.20` / KSP `2.2.20-2.0.4` UNCHANGED** — already satisfy AGP-9's KGP `2.2.10` floor (confirmed in the Phase-7 ADR). No Kotlin/KSP bump; verify `KotlinAndroidProjectExtension` still resolves under AGP-9 built-in Kotlin.

### targetSdk safety guard (highest-consequence silent failure)
- **D-05a:** `targetSdk = 35` MUST stay **explicit** in all 3 sites (`AndroidApplicationConventionPlugin.kt:17`, `AndroidLibraryConventionPlugin.kt:18`, `baselineprofile/build.gradle.kts:21`). AGP 9's `defaultTargetSdkToCompileSdkIfUnset` will silently flip `targetSdk` to 36 (an Android-16 runtime opt-in — **forbidden** this milestone) if any site omits it. Only `compileSdk` moves to 36.
- **D-05b:** Add an explicit verification step asserting `targetSdk = 35` is present in every site AND `compileSdk = 36` is in both compileSdk sites — grep-checkable, not "looks right".

### Built-in Kotlin removal (completeness)
- **D-06:** Remove `org.jetbrains.kotlin.android` from **all 5 sites** — AGP 9 registers the Kotlin Android plugin itself, so any leftover `apply`/`alias` causes an "extension already registered" / plugin-conflict failure:
  1. `build.gradle.kts:5` (root `alias(libs.plugins.kotlin.android) apply false`)
  2. `AndroidApplicationConventionPlugin.kt:13` (`apply("org.jetbrains.kotlin.android")`)
  3. `AndroidLibraryConventionPlugin.kt:14` (`apply("org.jetbrains.kotlin.android")`)
  4. `baselineprofile/build.gradle.kts:3` (`alias(libs.plugins.kotlin.android)`)
  5. `gradle/libs.versions.toml:150` (`kotlin-android` catalog alias)
- **D-06b:** KEEP `kotlin.compose` and `kotlin.serialization` plugins — they are separate Kotlin Gradle plugins still required and NOT bundled by AGP's built-in Kotlin.

### CI cache key + phase gate
- **D-07:** Bump the CI cache key token `agp8` → `agp9` at both sites (`.github/workflows/ci.yml:39` and `:41`) — a stale AGP-8 Gradle home cache poisons AGP-9 configuration.
- **D-08:** Phase gate (definition of done): `./gradlew compileDebugSources detekt ktlintCheck test` green across ALL modules with the Phase-7/early-v1.1 version-isolation opt-out flags **removed**. detekt-on-Gradle-9 is the first empirical test of the Phase-7 ADR decision (keep stable `1.23.8`); if detekt hard-fails under Gradle 9 + AGP 9, handle it as a planned deviation (document + minimal fallback) — do NOT pre-bump to the forbidden `2.0.0-alpha`.

### Forbidden crutches (locked — do NOT add)
- **D-09:** Do NOT add `android.enableLegacyVariantApi=true` or `android.newDsl=false` — both are no-op/doomed crutches and the repo is already clean (zero `applicationVariants`/`buildDir` usage; `KotlinAndroid` uses `layout.buildDirectory`).

### Claude's Discretion
- Exact commit messages and which sub-edits group into each of the D-01 commits (as long as the one-concern-per-commit bisect invariant holds).
- Whether the `CommonExtension` generics fix is expressed as a changed type parameter list, a type alias, or a star-projection adjustment — whatever compiles cleanly under AGP-9's `com.android.build.api.dsl.CommonExtension` while keeping `configureKotlinAndroid` callable from both Application/Library extensions.
- Whether to run the full gate once at the end or also after the Hilt commit.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase-8 decision sources (locked)
- `docs/adr/0001-gradle-9-readiness-and-agp8-fold-forward.md` — Phase-7 ADR: pinned Gradle `9.4.1` target (sha256 fetched-at-exec), the four plugin verdicts, the **detekt 1.23.8 keep-stable / reject-alpha** decision, the KGP `2.2.10` floor confirmation, and the AGP-8.7.3 fold-forward that makes the wrapper flip **Phase-8 commit 1**.
- `.planning/STATE.md` §"Target Version Matrix" + §"Key Locked Decisions (v1.1)" — the authoritative version pins and the non-negotiable constraints (no targetSdk bump, no Kotlin/KSP bump, no crutch flags, Media3 cap deferred).
- `.planning/ROADMAP.md` §"Phase 8" — goal, 5 success criteria (the gate contract), and the research flag (confirm AGP 9.2.1/9.2.0, Gradle floor, Hilt 2.59.2/2.59.1 at plan time).
- `.planning/REQUIREMENTS.md` — AGP9-02, AGP9-03, SDK-01 (this phase's REQ-IDs) + the green-on-Gradle-9 clause of AGP9-01 folded forward from Phase 7.

### Milestone research (read for pitfalls + exact patterns)
- `.planning/research/PITFALLS.md` — the AGP-9 migration traps (silent targetSdk flip, extension-already-registered, CommonExtension generics, Hilt-2.59-drops-AGP-8).
- `.planning/research/STACK.md` — pinned target versions with rationale.
- `.planning/research/ARCHITECTURE.md` — the convention-plugin choke-point analysis.
- `.planning/phases/07-gradle-9-core-version-bump-deprecation-sweep/07-RESEARCH.md` — the AGP-8.7.3×Gradle-9 incompatibility finding and the migration sequencing rationale.
- `.planning/phases/07-gradle-9-core-version-bump-deprecation-sweep/gradle9-deprecations.log` — Phase-7 evidence: ZERO repo-authored Gradle-9 deprecations (so no surprise own-script migration items in Phase 8).

</canonical_refs>

<code_context>
## Existing Code Insights

### Migration surface (exact sites — verified live)
- `build-logic/convention/src/main/kotlin/io/stashapp/android/buildlogic/KotlinAndroid.kt` — the shared `configureKotlinAndroid(commonExtension: CommonExtension<*,*,*,*,*,*>)` choke point: `compileSdk = 35` (→36) at L21; the `CommonExtension` 6-arg generics at L16 (the fix target); already uses `KotlinAndroidProjectExtension { compilerOptions {} }` (so NO kotlinOptions migration needed here).
- `AndroidApplicationConventionPlugin.kt` / `AndroidLibraryConventionPlugin.kt` — each `apply("org.jetbrains.kotlin.android")` (drop) and `defaultConfig.targetSdk = 35` (KEEP explicit).
- `AndroidHiltConventionPlugin.kt` — applies `com.google.devtools.ksp` + `com.google.dagger.hilt.android`; pulls `hilt-android` + `hilt-compiler` from the catalog (so the Hilt bump is a single catalog edit, `gradle/libs.versions.toml:6`).
- `baselineprofile/build.gradle.kts` — the only standalone module build script in scope: `alias(libs.plugins.kotlin.android)` (drop, L3), `kotlinOptions{ jvmTarget = "17" }` (→ `kotlin{compilerOptions{ jvmTarget.set(JvmTarget.JVM_17) }}`, L15), `compileSdk = 35` (→36, L9), `targetSdk = 35` (KEEP, L21).
- `gradle/libs.versions.toml` — single source for `agp` (L3), `hilt` (L6), and the `kotlin-android` alias (L150, remove).
- `.github/workflows/ci.yml` — cache key `agp8` token at L39 + L41 (→ `agp9`).

### Established patterns
- AGP-style convention-plugin architecture (Now-in-Android lineage): all module config flows through `configureKotlinAndroid`, so the generics fix + compileSdk bump fix every module at once. This is why the phase is atomic — the choke point fails all modules until it lands whole.

### Integration points
- The Gradle wrapper flip (commit 1) gates everything: AGP 9 requires Gradle 9, confirmed in Phase 7.

</code_context>

<specifics>
## Specific Ideas

- Confirm `KotlinAndroidProjectExtension` (and `JvmTarget`) still resolve under AGP-9 built-in Kotlin BEFORE committing the `kotlin.android` removal — if AGP-9's bundled Kotlin changes the extension type/import, the `configureKotlinAndroid` Kotlin block needs adjusting in the same commit.
- The repo declares a pre-existing duplicate `include(":baselineprofile")` in `settings.gradle.kts` (lines 38 & 50, surfaced in the Phase-7 code review). Harmless (Gradle dedups) and OUT of this phase's atomic scope — see Deferred.

</specifics>

<deferred>
## Deferred Ideas

- **`settings.gradle.kts` duplicate `include(":baselineprofile")` cleanup** — a 1-line dedupe; deferred to keep the AGP-9 atomic change-set clean and bisect-pure. Capture as a trivial follow-up (or fold into Phase 9/10 housekeeping), not Phase 8.
- **Media3 1.10.0 / nextlib pair + activity-compose 1.13 + core-ktx 1.18** — Phase 9 (green-gated on the compileSdk-36 build this phase produces).
- **CI assemble/signing probe + EdEC/bcprov spike** — Phase 10.
- **DEPS-07 lint re-enables** (the `NullSafeMutableLiveData` / `FrequentlyChangingValue` / `RememberInComposition` disables in `KotlinAndroid.kt`) — explicitly deferred; only revisit if AGP-9 changes lint behavior and breaks the gate.

</deferred>

---

*Phase: 08-agp-9-atomic-build-logic-migration-compilesdk-36*
*Context gathered: 2026-05-30 via discuss-phase --auto*
