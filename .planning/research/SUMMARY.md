# Project Research Summary

**Project:** Slopper — v1.1 AGP-9 Toolchain Modernization (DEPS-17)
**Domain:** Android Compose multi-module build-toolchain upgrade (pure version bumps, no user features)
**Researched:** 2026-05-30
**Confidence:** HIGH

## Executive Summary

This milestone is a **pure build-toolchain upgrade** of an existing Now-in-Android-style multi-module Compose app: AGP 8.7.3 → 9.x, Gradle 8.11.1 → 9.x, Hilt 2.56.2 → 2.59.x, compileSdk 35 → 36, plus the now-unblocked Media3/nextlib and leaf-library bumps. There are **no end-user features** — the "feature surface" is a checklist of concrete BUILD/CONFIG/API edits that AGP 9 forces on this repo. The repo is in unusually good shape for AGP 9: it already uses `androidComponents.onVariants` (not `applicationVariants`), `layout.buildDirectory` (not `buildDir`), KSP-only (no kapt), and has config-cache on. The entire blast radius is concentrated in `build-logic/convention/` — which is both the migration's biggest simplifier (a handful of central edits propagate to ~14 modules) and its single point of failure (one broken `configure` block fails every module's configuration at once).

The recommended approach is **version-bump-first, DSL-migrate-second, with a tightly-sequenced atomic core.** All four reports converge on one indivisible change-set: **Gradle wrapper → AGP 9 → drop `org.jetbrains.kotlin.android` from both convention plugins → fix `CommonExtension` generics → migrate `kotlinOptions{}` to `kotlin{compilerOptions{}}` → Hilt 2.59.x.** These cannot land piecemeal and stay green — Gradle < 9.4.1 blocks AGP 9.2, AGP 8 blocks Hilt 2.59, Hilt 2.59 drops AGP 8, and AGP 9's built-in Kotlin + plugin removal are mutually dependent. Everything *after* a green build — compileSdk 36, Media3/nextlib, leaf libs, and the CI-signing probe — is a **separable, green-gated follow-up** with its own small blast radius.

The dominant risks are silent/centralized failures, not hard crashes: a **silent targetSdk bump** (AGP 9 flips `defaultTargetSdkToCompileSdkIfUnset=true`, so the explicit `targetSdk = 35` in three places must be preserved or the app silently opts into Android-16 runtime behaviors this milestone forbids); the **Hilt 2.59.0 broken artifact** (`ComponentTreeDeps` missing — must pin 2.59.2, never "2.59+"); a **hard Media3 cap at 1.10.0** (NOT 1.10.1 — nextlib has no matching `1.10.1-…` build); and the **BouncyCastle EdEC CI-signing failure**, which the toolchain bump does NOT fix and may worsen — keep the compile-only CI gate as the contract and treat assembly as a probe, never a milestone dependency.

## Key Findings

### Recommended Stack

The full target version matrix is in STACK.md. Where the four reports disagreed, the resolutions below favor STACK.md (every version verified against live Google Maven / Maven Central `maven-metadata.xml` rather than training data) and PITFALLS.md (verified against the Dagger issue tracker). **Three disagreements are flagged for plan-time confirmation.**

**Copy-pasteable target set (FROM → TO):**

| Key | FROM | TO (recommended) | Flag |
|-----|------|------------------|------|
| `agp` | `8.7.3` | **`9.2.1`** | DISAGREE: STACK says 9.2.1 (patch, Maven-verified 2026-05-05); FEATURES/ARCH say 9.2.0 (GA). Default **9.2.1**; confirm patch exists at plan time. |
| Gradle wrapper | `8.11.1` | **`9.4.1`** | DISAGREE: STACK says 9.4.1 (the *exact AGP-9.2* floor); FEATURES/ARCH say 9.1.x (the AGP-9.0 floor). **9.4.1 is correct for AGP 9.2.** Re-pin Gradle to whatever the chosen AGP minor's release notes mandate (9.0->9.1.0, 9.1->9.3.1, 9.2->9.4.1). New `distributionSha256Sum` required. |
| `hilt` | `2.56.2` | **`2.59.2`** | DISAGREE: STACK/PITFALLS say 2.59.2 (fixes #5099 ComponentTreeDeps + jetifier); ARCH says 2.59.1. **Use 2.59.2** — 2.59.0 and arguably 2.59.1 carry the broken artifact. Never pin bare "2.59+". |
| `kotlin` (KGP) | `2.2.20` | **`2.2.20` (NO CHANGE)** | AGP 9 needs KGP >= 2.2.10; 2.2.20 satisfies. Do NOT chase 2.3.x (forces new KSP line + churn). |
| `ksp` | `2.2.20-2.0.4` | **`2.2.20-2.0.4` (NO CHANGE)** | Locked to Kotlin 2.2.20 exactly; already the highest on that line. |
| `compileSdk` | `35` | **`36`** | Build-Tools 36.0.0; AGP 9.2 supports up to API 36.1, so 36 is well inside range. 3 touchpoints. |
| `targetSdk` | `35` | **`35` (KEEP EXPLICIT)** | Out of scope; must stay pinned in all 3 places or AGP 9 silently bumps it to 36. |
| `media3` | `1.9.1` | **`1.10.0`** | HARD CAP — **NOT 1.10.1.** nextlib has no 1.10.1 pairing. |
| `nextlibMedia3Ext` | `1.9.1-0.11.0` | **`1.10.0-0.12.1`** | Version-prefix lockstep with Media3 1.10.0. |
| `baselineProfilePlugin` / `benchmark` | `1.4.1` / `1.3.3` | **AGP-9-aware 1.4.x+ pair** | Exact GA-safe version unconfirmed; move both as one lockstep pair. |
| `activity-compose` | current | **`1.13`** | Leaf lib, unblocked by compileSdk 36; independent, last. |
| `core-ktx` | current | **`1.18`** | Leaf lib, unblocked by compileSdk 36; independent, last. |
| JDK | 17 compile / 21 daemon | **unchanged** | AGP 9.2 + Gradle 9.4.1 both need JDK 17 min; current setup is fine. |

**Do NOT touch:** Kotlin past 2.2.20, KSP past 2.2.20-2.0.4, Media3 to 1.10.1, Gradle to 9.5.x, minSdk/targetSdk, or the already-landed Apollo 5.0.0 / OkHttp 5.3.2 / Coil 3.4.0 / Compose BOM.

### Migration Work Surface (the "features")

Full REQUIRED/RECOMMENDED/OPTIONAL checklist with file-and-line references is in FEATURES.md.

**REQUIRED (build breaks without these):**
- Bump AGP, Gradle wrapper, Hilt, realign KSP (R1–R4).
- **Remove `org.jetbrains.kotlin.android`** from both convention plugins, the root + baselineprofile plugin blocks, and the catalog alias (R5–R7).
- **Fix `CommonExtension<*,*,*,*,*,*>` -> `CommonExtension`** (R8) — AGP 9 removed the generics; this is the single genuine compile break in shared build-logic and gates everything downstream.
- Migrate `kotlinOptions{}` -> `kotlin{compilerOptions{}}` (R9–R10).
- `compileSdk` 35 -> 36 in **3 touchpoints** (R11); **keep `targetSdk = 35` explicit in 3 places** (R12 — the most dangerous silent failure).
- Verify Apollo `newDsl` compat (R13) and bump baseline-profile/benchmark to an AGP-9-aware pair (R14).

**Verified non-issues:** repo uses zero `applicationVariants`/`libraryVariants`/`buildDir`; `enableLegacyVariantApi` is a no-op in AGP 9 and must NOT be added as a crutch; no kapt anywhere.

**Anti-items (do NOT do):** add `enableLegacyVariantApi=true`; set `newDsl=false` to dodge R8; bump `targetSdk` "while we're here"; keep `kotlin.android` "to be safe."

### Architecture Approach

The build graph funnels all version/variant/Kotlin wiring through `build-logic/convention` (five convention plugins applied by every module). Detail and the six integration points are in ARCHITECTURE.md.

**Major integration points:**
1. `gradle-wrapper.properties` + `gradle.properties` — Gradle bump + new sha256.
2. `gradle/libs.versions.toml` — the single source of versions.
3. `build-logic/convention` (THE CHOKE POINT) — `KotlinAndroid.kt`, Application/Library/Compose/Hilt/Feature plugins. One edit propagates to ~14 modules; one break fails all 14.
4. Per-module `build.gradle.kts` (low touch; already new-DSL).
5. `baselineprofile/build.gradle.kts` — standalone, AGP-coupled, the 4th compileSdk touchpoint footgun.
6. CI (`.github` + `.forgejo` `ci.yml`) — cache key `agp8` -> `agp9`, compile-only gate, signing probe.

### Critical Pitfalls

Top items from PITFALLS.md (10 total, mapped to phases there):

1. **Silent targetSdk bump (P2)** — AGP 9 defaults `defaultTargetSdkToCompileSdkIfUnset=true`. Keep `targetSdk = 35` explicit in all 3 sites during the compileSdk-36 edit, or the app silently opts into Android-16 runtime behavior. Highest-consequence silent failure.
2. **`kotlin.android` double-application (P2)** — built-in Kotlin registers the `kotlin` extension itself; the convention plugins also apply it -> "extension already registered" / "no longer required since AGP 9.0." Remove all five application sites.
3. **`CommonExtension` generics removal (P2)** — `CommonExtension<*,*,*,*,*,*>` -> `CommonExtension`; the one real compile break in shared build-logic, fails every module.
4. **Hilt 2.59.0 broken artifact (P3)** — `ComponentTreeDeps` missing (#5099). Pin **2.59.2**, never "2.59+". Hilt 2.59 hard-requires AGP 9 — sequence strictly after the core bump.
5. **Media3 1.10.0 hard cap (P3)** — nextlib has no 1.10.1 pairing; mismatched versions cause a runtime `UnsatisfiedLinkError` in the FFmpeg renderer (surfaces only on software-codec playback — device smoke test required).
6. **BouncyCastle EdEC CI signing (P4)** — NOT fixed by the toolchain bump and may regress. Keep the compile-only CI gate as the contract; add a `continue-on-error` `assembleDebug` probe; only promote to a real gate on a demonstrated green run; pin `bcprov-jdk18on` if forcing it.
7. **Multi-module blast radius (P1->P2 sequencing)** — version-bump-first with `builtInKotlin=false`/`newDsl=false` opt-outs ON to isolate version breakage; migrate build-logic one concern per commit; remove opt-outs last; validate against `:core:common` before a full build.
8. **Gradle 9 deprecations / dependency-check 12.2.2 (P1)** — enumerate via `./gradlew help --warning-mode=all` on Gradle 8.11.1 first; OWASP dependency-check 12.2.2 Gradle-9 compatibility is **unknown** — verify or keep `--no-configuration-cache`.

## Implications for Roadmap

The reports converge on a **6–7 stage sequence** built around one atomic core. Suggested phases (PITFALLS.md shorthand in parentheses):

### Phase 1: Core Version Bump + Gradle-9 Deprecation Sweep (P1-CORE-BUMP)
**Rationale:** Isolate "did the version bump break anything" from "did the DSL migration break anything." Gradle and KSP/Kotlin/compose lockstep must be settled before AGP-9 DSL surgery.
**Delivers:** Gradle wrapper -> 9.4.1 (+ sha256); enumerate and bump Gradle-9-incompatible plugins (detekt, ktlint, dependency-check, baseline-profile) via `--warning-mode=all`; confirm Kotlin 2.2.20 / KSP 2.2.20-2.0.4 already satisfy AGP 9.
**Uses:** STACK.md version floors.
**Avoids:** Pitfall 5 (KSP/Kotlin lockstep), Pitfall 10 (Gradle-9 deprecations + dependency-check 12.2.2 unknown).
**Note:** May run AGP 8.7.3 on Gradle 9 for one commit to isolate Gradle fallout; if AGP 8.7.3 is not Gradle-9-compatible, fold into Phase 2.

### Phase 2: AGP 9 + Built-in-Kotlin Build-Logic Migration + compileSdk 36 (P2-BUILD-LOGIC)
**Rationale:** **The atomic core.** AGP 9 + drop `kotlin.android` + `CommonExtension` fix + `kotlinOptions`->`compilerOptions` + Hilt 2.59.2 are mutually dependent — no half-state build configures. compileSdk 36 rides here (or its own sub-commit) since it must precede the Media3/leaf bumps and is the lowest-risk SDK change.
**Delivers:** AGP 9.2.1; convention-plugin surgery (R5–R10); compileSdk 36 in all 3 touchpoints; `targetSdk = 35` preserved explicit; Hilt 2.59.2; CI cache key `agp8`->`agp9`.
**Implements:** The `build-logic/convention` choke point (Integration Point 3).
**Avoids:** Pitfalls 1, 2, 3, 6, 8. Validate `:core:common:compileDebugKotlin` before a full build; one concern per commit for `git bisect`.
**Gate:** `compileDebugSources + detekt + ktlintCheck + test` green with opt-out flags REMOVED.

### Phase 3: Hilt-gated Library Bumps — Media3/nextlib + Leaf Libs (P3-HILT-MEDIA3)
**Rationale:** Green-gated follow-up; these only change *which* version resolves, not whether the build configures. Media3 1.10.0 needs compileSdk 36 (Phase 2). Each can be its own commit.
**Delivers:** Media3 1.10.0 + nextlib 1.10.0-0.12.1 (single locked pair, NOT 1.10.1); activity-compose 1.13; core-ktx 1.18; un-ignore the corresponding Dependabot entries.
**Uses:** Media3/nextlib lockstep from STACK.md.
**Avoids:** Pitfall 7 — device software-codec playback smoke test (runtime-only failure surface).

### Phase 4: CI Signing Probe (P4-CI-SIGNING)
**Rationale:** **Isolated and last** — must never block a green AGP-9 landing. The EdEC failure is a bcprov classpath skew, not AGP-gated; the toolchain bump is neutral-to-risky.
**Delivers:** A `continue-on-error` `assembleDebug`/`validateSigningDebug` probe on AGP-9 runners; either a demonstrated green run that promotes a real assemble gate, or a documented decision to keep the compile-only gate (pinning `bcprov-jdk18on` if forcing it).
**Avoids:** Pitfall 9 — do NOT restore `assembleDebug` on faith.

### Phase Ordering Rationale

- **Atomic core vs separable follow-ups** is the load-bearing structure. Gradle->AGP->drop-kotlin.android->CommonExtension-fix->compilerOptions->Hilt is one indivisible change-set (convention plugins fail all modules until complete). compileSdk 36, Media3/nextlib, leaf libs, and CI-signing are separable, each with its own green gate.
- **Versions before DSL** (Phase 1 before 2) isolates version-resolution breakage from DSL-migration breakage, enabling `git bisect`.
- **Hilt strictly after AGP 9** — 2.59 drops AGP 8 entirely; bumping it earlier fails.
- **compileSdk 36 before Media3/leaf libs** — the AAR `minCompileSdk` consumer rule rejects SDK-35 modules consuming SDK-36 libraries.
- **CI signing isolated and last** — it must not gate the toolchain landing.

### Research Flags

Phases likely needing `/gsd-research-phase` during planning:
- **Phase 1:** OWASP **dependency-check 12.2.2 Gradle-9 compatibility is unknown** — confirm a compatible release exists or budget the `--no-configuration-cache` workaround.
- **Phase 2:** Confirm the three flagged version disagreements (AGP 9.2.1 vs 9.2.0, Gradle 9.4.1 vs 9.1.x, Hilt 2.59.2 vs 2.59.1) against live metadata at plan time; verify `KotlinAndroidProjectExtension` still resolves under built-in Kotlin and that the `kotlin{compilerOptions{}}` rewrite is exact.
- **Phase 4:** EdEC/bcprov spike — capture the full stack trace on an AGP-9 runner before deciding on a gate.

Phases with standard patterns (lighter research):
- **Phase 3:** Media3/nextlib lockstep and leaf-lib bumps are well-documented mechanical bumps — the only nuance is the matching version pair (verified `1.10.0-0.12.1`) and a device smoke test.

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | Every version verified against live Google Maven / Maven Central metadata + AGP/Gradle release notes, not training data. Three exact-patch disagreements flagged for plan-time confirmation. |
| Features (migration surface) | HIGH | Verified against AGP 9.0/9.1/9.2 release notes + built-in-Kotlin guide and direct repo grep (zero legacy-variant/buildDir hits). MEDIUM on Apollo `newDsl` and exact baseline-profile/benchmark GA version. |
| Architecture | HIGH | Convention-plugin graph, integration points, and build order read directly from repo files. MEDIUM on exact baseline-profile-plugin GA version and whether AGP 9 incidentally fixes EdEC. |
| Pitfalls | HIGH | Verified against AGP 9 release notes + Dagger issue tracker (#4944/#4979/#5083/#5098/#5099/#5122) + repo build-logic inspection. MEDIUM on Media3/nextlib pairing and the bcprov classpath conflict. |

**Overall confidence:** HIGH

### Gaps to Address

- **AGP 9.2.1 vs 9.2.0 / Gradle 9.4.1 vs 9.1.x / Hilt 2.59.2 vs 2.59.1:** Confirm exact patches against live metadata when Phase 2 is planned; recommended defaults are 9.2.1 / 9.4.1 / 2.59.2. Gradle floor must match the AGP minor actually chosen.
- **dependency-check 12.2.2 + Gradle 9:** Compatibility unknown — verify in Phase 1; fallback is `--no-configuration-cache` (already used) and/or a plugin bump.
- **baseline-profile / benchmark AGP-9-GA-safe version:** Exact version uncertain (1.4.1 stable, but the AGP-9-GA variant-API fix may need 1.4.x/1.5.x) — verify the pair at Phase 2/3 plan time.
- **Apollo 5.0.0 + `android.newDsl=true`:** Verify issue #6693 ("KotlinSourceSet 'main' not found") does not trip; bump to latest 5.x if it does.
- **EdEC/bcprov CI signing:** Unresolved by design — Phase 4 spike captures the stack trace and decides; compile-only gate is the safe fallback.
- **`KotlinAndroidProjectExtension` resolvability under built-in Kotlin:** Verify the extension still resolves post-`kotlin.android` removal, or rewrite to `kotlin{compilerOptions{}}`.

## Sources

### Primary (HIGH confidence)
- Google Maven `maven-metadata.xml` — AGP 9.x list incl. 9.2.1 (POM last-modified 2026-05-05).
- Maven Central `maven-metadata.xml` — Dagger latest 2.59.2; nextlib-media3ext 1.10.0-0.12.1; KSP 2.2.20-2.0.4.
- AGP 9.0 / 9.1 / 9.2.0 release notes — Gradle/JDK/Build-Tools floors, built-in Kotlin default, legacy variant API removal, `defaultTargetSdkToCompileSdkIfUnset` flip, AAR `minCompileSdk` consumer rule, max API 36.1.
- Migrate to built-in Kotlin (developer.android.com) — plugin removal, `kotlinOptions`->`compilerOptions`, jvmTarget defaults to targetCompatibility.
- JetBrains "Update your projects for AGP 9" — KGP 2.2.10 runtime floor, `builtInKotlin=false` removed in AGP 10.
- Dagger #5099 — Hilt 2.59.0 `ComponentTreeDeps` missing, fixed in 2.59.2.
- gradle.org/releases — Gradle 9.4.1 is the AGP-9.2 floor; Media3 releases — 1.10.0 (2026-03-26) vs 1.10.1 (2026-05-12).
- Repo inspection — `libs.versions.toml`, wrapper, `gradle.properties`, all five convention plugins, `KotlinAndroid.kt`, `app/build.gradle.kts`, `baselineprofile/build.gradle.kts`, both `ci.yml`, `dependabot.yml`.

### Secondary (MEDIUM confidence)
- Dagger #4944/#4979/#5083/#5098/#5122 — Hilt AGP-9 BaseExtension/legacy-variant failures + slow-incremental watch.
- Apollo Kotlin #6693 — Apollo 5 + AGP 9 `newDsl` edge case.
- androidx Benchmark releases — baseline-profile AGP-9-GA-safe version (stale "9.0.0-alpha01" doc note).
- SonarQube/bcgit EdEC `NoClassDefFoundError` discussions — root cause is old-bcprov-shadowing-new classpath conflict, JDK-independent.

### Tertiary (LOW confidence)
- `about-agp` summary table "37" max-API cell — contradicts the 9.2.0 release notes' "36.1"; resolved in favor of release notes. No impact on this milestone (target is 36).

---
*Research completed: 2026-05-30*
*Ready for roadmap: yes*
