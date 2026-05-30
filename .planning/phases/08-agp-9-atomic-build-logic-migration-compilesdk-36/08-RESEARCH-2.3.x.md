# Phase 8: AGP-9 via Kotlin 2.3.x + KSP2 — Coherent Version-Set Research

**Researched:** 2026-05-31
**Domain:** AGP 9.2.1 + AGP-9 built-in Kotlin + Kotlin 2.3.x + KSP2 (bare scheme) toolchain coherence for Slopper
**Confidence:** HIGH (every version live-verified against Maven Central / Google Maven POMs + maven-metadata.xml this session; ecosystem compat cross-checked against detekt's official compatibility table, Apollo 5 changelog, Dagger 2.59 notes, and KSP docs)

---

## Summary

The 2.2.x line is **empirically dead under AGP 9** (B-08-04, this project): AGP-9 built-in Kotlin requires a KSP2-only plugin (KSP ≥ 2.3.0 ⇒ Kotlin 2.3.x), and the traditional `kotlin("android")` KSP1 path crashes at apply-time on the whole KGP 2.2.x line (`ApplicationExtensionImpl cannot be cast to BaseExtension`, because AGP 9 removed `BaseExtension`). So **Kotlin 2.3.x + KSP2 + AGP-9 built-in Kotlin is the sole remaining path** — exactly the question this research answers.

**A coherent, working version set DOES exist.** Every coupled component has a 2.3.x-compatible build:

- **Kotlin 2.3.20** is the correct target (not 2.3.21) — it is the Kotlin version that the **bare-scheme KSP2 top (2.3.9)** is built against. `[VERIFIED: KSP 2.3.7/2.3.8/2.3.9 POMs all depend on kotlin-stdlib 2.3.20]`
- **compose-compiler-gradle-plugin 2.3.20** is published (auto-tracks Kotlin via `version.ref`). `[VERIFIED: Maven Central]`
- **Hilt/Dagger 2.59.2** is KSP-API-based; its processor runs under the KSP2 2.3.x host. Dagger 2.59's library + annotation processor were updated to target Kotlin 2.0+ specifically to support the newer toolchain including KSP2. `[CITED: Dagger 2.59 notes / search corroboration]` — MEDIUM-HIGH.
- **Apollo Kotlin 5.0.0** is a **Kotlin 2.3 release** (built on KGP 2.3.10, JVM/Android consumers compatible at 2.1+). Fully compatible with Kotlin 2.3.20. Its Gradle plugin uses Gratatouille wiring (not KSP) for codegen. `[VERIFIED: Apollo CHANGELOG + 5.0.0 POM]` — HIGH.
- **detekt** is the ONE component with **no stable 2.3.x build**. detekt 1.23.8 (current stable, and the newest on Maven Central) **hard-fails on Kotlin 2.3.x metadata** (#8865) — a DIRECT hit on Kotlin 2.3.20. The only build that reads Kotlin 2.3.x metadata is **detekt 2.0.0-alpha.3** (built against Kotlin 2.3.21, published under the new `dev.detekt` group/plugin-id). detekt runs its OWN bundled Kotlin analysis (decoupled from the project compile Kotlin), so alpha.3's 2.3.21 analyzer reads 2.3.20 metadata fine. `[VERIFIED: detekt compatibility table + dev.detekt Maven Central metadata]` — HIGH.

**Verdict: VIABLE.** No component is a hard wall. The single new cost is a **detekt 2.0.0-alpha.3 build-time-tool exception** (precedented by AR-08-01 for baselineprofile 1.5.0-alpha06) — OR decoupling detekt from the gate. detekt is build-time-only static analysis; it never ships in the APK, so the stable-only policy's runtime-safety intent is preserved.

**Primary recommendation:** Adopt **Kotlin 2.3.20 / KSP 2.3.9 / Dagger-Hilt 2.59.2 / Apollo 5.0.0 / compose-compiler 2.3.20 / ktlint 14.2.0**, and resolve detekt by adopting **detekt 2.0.0-alpha.3** (`dev.detekt` group + `dev.detekt` plugin id) under a scoped, AR-08-01-style build-tool exception. Re-drop `kotlin("android")` (AGP-9 built-in Kotlin), remove `android.builtInKotlin=false`. This is the only configuration that yields a green AGP-9 build.

---

## User Constraints (from STATE.md / CONTEXT.md)

### Locked decisions this research operates under
- AGP **9.2.1** (locked, applied). Gradle **9.4.1** (applied). Hilt **2.59.2** (locked exactly, never bare `2.59+`).
- `targetSdk = 35` stays explicit at all 3 sites; only `compileSdk` → 36.
- Forbidden crutches: `android.enableLegacyVariantApi=true`, `android.newDsl=false`. (`android.builtInKotlin=false` is NOT used in the 2.3.x path — built-in Kotlin is adopted.)
- **Stable-only policy** in force, with a precedented **scoped build-time-tool exception** (AR-08-01: baselineprofile `1.5.0-alpha06`). A detekt-alpha exception is precedented IF stable has no 2.3.x path — and it does not.

### The constraint this research is explicitly chartered to revisit
- **D-04b ("Kotlin/KSP UNCHANGED at 2.2.20")** — proven impossible under AGP 9 (B-08-04). This research determines the replacement quartet. Lifting D-04b for the toolchain set is B-08-04 option (a), the only green path.

---

## The Coherent Version Set (live-verified 2026-05-31)

| Component | Catalog key | FROM | TO (recommended) | Verification | Confidence |
|-----------|-------------|------|------------------|--------------|------------|
| Kotlin (KGP) | `kotlin` | `2.2.20` | **`2.3.20`** | KSP 2.3.7/2.3.8/2.3.9 all depend on `kotlin-stdlib 2.3.20` → 2.3.20 is the KSP2-matched Kotlin; 2.3.21 has NO published KSP yet `[VERIFIED: symbol-processing-api-2.3.9.pom → kotlin-stdlib 2.3.20]` | HIGH |
| KSP (bare KSP2 scheme) | `ksp` | `2.2.20-2.0.4` | **`2.3.9`** | Bare scheme top; `<latest>2.3.9</latest>`; KSP2-only, AGP-9-compatible; same artifact id `com.google.devtools.ksp` + `symbol-processing-api` (no id change for KSP2) `[VERIFIED: Maven Central maven-metadata.xml]` | HIGH |
| compose-compiler plugin | `plugin-compose` (`version.ref = kotlin`) | tracks `2.2.20` | **`2.3.20`** (auto) | `compose-compiler-gradle-plugin` 2.3.20 published; tracks `kotlin` ref automatically `[VERIFIED: Maven Central — 2.3.0/2.3.10/2.3.20/2.3.21 all present]` | HIGH |
| Hilt / Dagger | `hilt` | `2.56.2` | **`2.59.2`** (unchanged from locked) | KSP-API-based processor runs under KSP2 host; 2.59 lib+processor target Kotlin 2.0+ for newer toolchain incl. KSP2; `<latest>2.59.2</latest>` (no 2.60) `[VERIFIED: Maven Central]` `[CITED: Dagger 2.59 / search]` | MEDIUM-HIGH |
| Apollo Kotlin | `apollo` | `5.0.0` | **`5.0.0`** (unchanged) | Apollo 5 is a Kotlin-2.3 release (KGP 2.3.10; JVM/Android consumers 2.1+ compatible); Gradle plugin uses Gratatouille wiring, not KSP `[VERIFIED: Apollo CHANGELOG + apollo-gradle-plugin-5.0.0.pom]` | HIGH |
| ktlint plugin | `ktlint` | `14.2.0` | **`14.2.0`** (unchanged) | Configures/runs via its own ktlint engine (root build pins `version.set("1.6.0")`); Kotlin-compile-version-independent; low risk | MEDIUM |
| detekt | `detekt` | `1.23.8` | **`2.0.0-alpha.3`** (group `dev.detekt`, plugin id `dev.detekt`) | 1.23.8 hard-fails on 2.3.x metadata (#8865); alpha.3 built against Kotlin 2.3.21, reads 2.3.20 metadata; `<latest>2.0.0-alpha.3</latest>` `[VERIFIED: dev.detekt Maven Central + detekt compatibility table]` | HIGH (compat) / requires policy exception |

**No HARD blocker exists.** Every coupled component has a published 2.3.x-compatible release. The only non-stable artifact is detekt (alpha) — and detekt is build-time-only.

---

## Per-Question Findings

### Q1 — Kotlin 2.3.x choice: **2.3.20** (NOT 2.3.21)

Stable 2.3.x line: `2.3.0, 2.3.10, 2.3.20, 2.3.21` (2.4.0 is RC2-only). `[VERIFIED: kotlin-gradle-plugin maven-metadata.xml]`

The deciding constraint is **KSP**: the bare KSP2 scheme tops at **2.3.9**, and 2.3.7/2.3.8/2.3.9 are ALL built against **kotlin-stdlib 2.3.20**. There is **no published KSP for Kotlin 2.3.21**. Since KSP2 must match the compile Kotlin, **Kotlin 2.3.20 is the correct, KSP-matched target.** Choosing 2.3.21 would leave KSP unmatched (no 2.3.21-built KSP) — a coherence break. `[VERIFIED: symbol-processing-api-{2.3.7,2.3.8,2.3.9}.pom]`

**AGP 9.2.1 ↔ Kotlin ceiling:** AGP 9.2's KGP floor is 2.2.10 (well below 2.3.20); no documented Kotlin *ceiling* in AGP 9.2.x. Built-in Kotlin uses whatever KGP the catalog pins, as long as ≥ floor. Kotlin 2.3.20 is fine. `[CITED: agp-9-2-0-release-notes; no upper-bound clause]` — HIGH.

**Recommendation: Kotlin `2.3.20`.**

### Q2 — KSP2 ↔ Kotlin mapping: **KSP `2.3.9`** pairs with Kotlin 2.3.20

- The bare scheme `2.3.0…2.3.9` IS the KSP2-only line (KSP1 is gone from this scheme). `<latest>2.3.9</latest>`. `[VERIFIED: maven-metadata.xml]`
- KSP **2.3.9 → kotlin-stdlib 2.3.20**, so KSP 2.3.9 + Kotlin 2.3.20 is the exact-matched pair.
- **Artifact/plugin id UNCHANGED for KSP2:** still `com.google.devtools.ksp` (plugin id), `com.google.devtools.ksp:symbol-processing-api` (library), `com.google.devtools.ksp.gradle.plugin` (plugin marker). The catalog's existing coordinates stay; only the version string changes `2.2.20-2.0.4` → `2.3.9`. `[VERIFIED: Maven Central paths resolve]` — HIGH.
- KSP2 + AGP-9 built-in Kotlin is the *intended* combination: "KSP1 will not support Kotlin 2.3.0+ and AGP 9.0+; migrate to KSP2." KSP2 has been default since KSP 2.0.0. `[CITED: KSP docs / google/ksp]` — HIGH. This directly clears the B-08-02/B-08-03 collision: the bare-scheme KSP is KSP2-only and accepts AGP-9 built-in Kotlin.

**Recommendation: KSP `2.3.9`.** (Operational note: KSP2 uses materially more daemon memory than KSP1 — the repo's `org.gradle.workers.max=2` + 12GB host should hold, but watch for OOM on CI; bump `-Xmx` if needed.)

### Q3 — Hilt/Dagger 2.59.2 × Kotlin 2.3.x + KSP2: **supported, no version change needed**

- `dagger-compiler-2.59.2.pom` declares compile deps on `symbol-processing-api 2.2.20-2.0.3` and `kotlin-stdlib/kotlin-metadata-jvm 2.2.20`. `[VERIFIED: POM]` These are the versions Dagger was **built** against — they are NOT a runtime ceiling. A KSP **processor** is written against the KSP *API*; the **KSP2 host** (2.3.9) loads and runs it. Dagger uses stable KSP API surfaces present in 2.3.x.
- Dagger 2.59's library + annotation processor were explicitly updated to **target Kotlin 2.0 to support the newer Kotlin toolchain including KSP2**. `[CITED: search corroboration of Dagger 2.59 notes]`
- Hilt 2.59 is the FIRST Hilt whose Gradle plugin supports AGP 9.0 (dropped AGP 8). 2.59.2 fixes the 2.59.0 `ComponentTreeDeps` regression (#5099) and the jetifier compile error. `[VERIFIED: Maven Central <latest>2.59.2</latest>; CITED: Dagger #5099]`
- No Dagger 2.60+ exists; **2.59.2 is both the locked version AND the newest** — there is nothing higher to fall back to, and nothing lower works on AGP 9.

**Not a hard blocker.** Residual risk: the Dagger-KSP2 path is newer than the Dagger-KSP1 path; verify a `:feature:*` Hilt DI graph compiles under KSP2 at the gate. **Confidence MEDIUM-HIGH** — the POM evidence is HIGH for "no version bump available/needed"; the "runs cleanly under KSP2 2.3.9" claim is MEDIUM pending the empirical gate (the standard processor/host contract makes it very likely).

### Q4 — Apollo 5.0.0 × Kotlin 2.3.x + KSP2: **fully supported, no version change needed**

- Apollo Kotlin 5 IS a Kotlin-2.3 release: *"Apollo Kotlin 5 uses KGP 2.3, with 2.1 compatibility for JVM and Android consumers"* and *"Bump to 2.3.10."* `[VERIFIED: Apollo CHANGELOG]` Our JVM/Android-only usage (the `com.apollographql.apollo` plugin + `apollo-runtime`) is comfortably within the 2.1+ consumer floor, and 2.3.20 is within Apollo's own 2.3 build line.
- `apollo-gradle-plugin-5.0.0.pom` depends on `kotlin-stdlib 2.0.0` + **`gratatouille-wiring-runtime 0.1.2`** — Apollo 5's Gradle codegen uses **Gratatouille**, NOT KSP. `[VERIFIED: POM]` So Apollo's codegen does not even sit on the KSP×AGP-9 axis; the only KSP consumers are Hilt/Dagger. This REMOVES Apollo from the KSP2 risk surface entirely.
- Apollo 5 also lists Gradle 9 support in its 5.0.0 infrastructure notes. `[VERIFIED: CHANGELOG]`

**Not a hard blocker. HIGH confidence.** Apollo 5.0.0 stays exactly as pinned.

### Q5 — compose-compiler-gradle-plugin for Kotlin 2.3.20: **published**

`org.jetbrains.kotlin:compose-compiler-gradle-plugin` has `2.3.0, 2.3.10, 2.3.20, 2.3.21` all on Maven Central. `[VERIFIED]` It ships in lockstep with Kotlin and the catalog routes `plugin-compose` through `version.ref = "kotlin"`, so bumping `kotlin = "2.3.20"` auto-selects compose-compiler 2.3.20. No separate action. **HIGH.**

### Q6 — detekt on Kotlin 2.3.x: **THE one non-stable component — use detekt 2.0.0-alpha.3**

This is the critical risk, and it resolves to a build-tool exception (not a hard wall):

- **(i) Is there a 1.23.x patch for 2.3.x?** **NO.** detekt-gradle-plugin on Maven Central tops at **1.23.8** (`<latest>1.23.8</latest>`; newest is 1.23.8). 1.23.8 bundles Kotlin compiler 2.0.0 (reads metadata ≤ 2.1.0) and **hard-fails on Kotlin 2.3.x metadata** — `metadata version is 2.3.0, but compiler 2.0.0 can read up to 2.1.0` (#8865). Moving to Kotlin 2.3.20 is a **DIRECT hit**. No 1.23.9+ exists. `[VERIFIED: Maven Central + detekt compatibility table + #8865]`
- **(ii) Does detekt 2.0.0 support 2.3.x, and how stable?** **YES, but alpha only.** Published under the **new group `dev.detekt`** (and new plugin id **`dev.detekt`**), tops at **`2.0.0-alpha.3`**. Official compatibility table: alpha.1 → Kotlin 2.2.20, alpha.2 → 2.3.0, **alpha.3 → 2.3.21**. `[VERIFIED: detekt.dev/docs/introduction/compatibility]` The maintainer (BraisGabin) states the alpha line supports Kotlin 2.3.20 and that alpha2 did NOT — **so alpha.3 is the required version**. The team explicitly is NOT ready to mark 2.0 stable (API still changing). `[CITED: detekt #9170, #8865]`
- **(iii) Can detekt run decoupled from the compile Kotlin version?** **Yes — and this is why alpha.3 works for a 2.3.20 project.** detekt bundles its OWN Kotlin analysis engine. alpha.3's analyzer is built on Kotlin 2.3.21, so it reads 2.3.20 metadata (2.3.20 ≤ 2.3.21) without the #8865 ceiling error. Slopper's detekt config is also **parse-only** (no `jvmTarget`/classpath/type-resolution wiring in the root `subprojects{}` block) — lower metadata-read pressure still. The detekt version is independent of `kotlin = 2.3.20`; you pin it separately.
- **(iv) Worst case — drop detekt from the gate?** Available as a fallback but **not necessary**. Preference ladder:
  1. **RECOMMENDED — adopt detekt `2.0.0-alpha.3`** (`dev.detekt:detekt-gradle-plugin:2.0.0-alpha.3` + plugin id `dev.detekt`) under a scoped AR-08-01-style build-tool exception. Update BOTH the catalog `detekt` ref AND the root `build.gradle.kts subprojects{}` `toolVersion` (the second source of truth — Pitfall 5 from 08-RESEARCH). Note the plugin id changes `io.gitlab.arturbosch.detekt` → `dev.detekt`, and config import packages may move (`io.gitlab.arturbosch.detekt.*` → `dev.detekt.*`) — budget a small DSL-migration in the root build script.
  2. If the alpha proves unstable on the repo's ruleset: run detekt **non-gating** (separate `--continue` task, off the `compileDebugSources detekt ktlintCheck test` gate) and track as a deviation, keeping ktlint as the gating linter.
  3. Last resort: temporarily drop detekt from the gate entirely, file tech debt, restore when detekt 2.0 ships stable.

**Concrete recommendation:** Path (1) — **detekt 2.0.0-alpha.3 under a scoped build-time-tool exception**, mirroring AR-08-01 exactly (build-time-only tooling, never in the APK, the only AGP-9/Kotlin-2.3.x-compatible build). This is a **NEW required policy exception** and must be called out and approved.

### Q7 — ktlint 14.2.0 × Kotlin 2.3.x: **low risk, unchanged**

The `org.jlleitschuh.gradle.ktlint` plugin (14.2.0) wraps the standalone ktlint engine, pinned in the root build to `version.set("1.6.0")`. ktlint formats/lints via its own parser, largely independent of the project's compile Kotlin version. Expected to configure/run on Gradle 9 + AGP 9 + Kotlin 2.3.20. **MEDIUM** (no live failure found; verify at gate). If ktlint 1.6.0's parser chokes on a 2.3 language feature, bump the engine `version.set(...)`, not the plugin. No change recommended pre-emptively.

### Q8 — Other coupled components on Kotlin 2.3.x / KSP2

| Component | Catalog | Coupling | Verdict |
|-----------|---------|----------|---------|
| **kotlinx-serialization compiler plugin** | `plugin-kotlin-serialization` (`version.ref = kotlin`) | The serialization *compiler plugin* tracks Kotlin EXACTLY (ships in the Kotlin distribution). | **Auto-tracks to 2.3.20** via `version.ref = "kotlin"`. No action — bumping `kotlin` bumps it. The runtime lib `kotlinx-serialization-json 1.7.3` is consumer-compatible (runtime lib, not compiler-coupled). LOW risk. `[VERIFIED: catalog routes plugin id through kotlin ref]` |
| **kotlinx-coroutines 1.9.0** | `kotlinxCoroutines` | Pure runtime lib; not compiler-version-welded. | No change needed. 1.9.0 is consumer-compatible with Kotlin 2.3.x (Kotlin stdlib forward-compat). LOW risk. |
| **kotlinx-collections-immutable 0.4.0** | runtime lib | No compiler coupling. | No change. LOW. |
| **Coil 3.4.0, OkHttp 5.3.2, Media3 1.9.1** | runtime libs | No Kotlin-compiler coupling. | No change (Media3 1.10 is Phase 9). LOW. |
| **baselineprofile 1.5.0-alpha06** | already exception (AR-08-01) | Build-tool, AGP-9. | Unchanged; already accepted-risk. |

**No other compiler-coupled component is a blocker.** The serialization compiler plugin is the only other lockstep item, and it auto-tracks `kotlin`.

---

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary | Rationale |
|------------|-------------|-----------|-----------|
| Compile Kotlin version | catalog `kotlin = 2.3.20` | compose-compiler + serialization plugins (auto via `version.ref`) | One bump drives three compiler-welded plugins |
| Annotation processing (Hilt DI) | catalog `ksp = 2.3.9` (KSP2 host) + `hilt = 2.59.2` (processor) | `AndroidHiltConventionPlugin` | KSP2 host runs the Dagger processor; built-in Kotlin no longer rejected |
| GraphQL codegen | `apollo = 5.0.0` (Gratatouille, NOT KSP) | — | Off the KSP×AGP-9 axis entirely; Kotlin-2.3 native |
| Static-analysis gate | catalog `detekt = 2.0.0-alpha.3` + root `subprojects{} toolVersion` | ktlint 14.2.0 (engine 1.6.0) | detekt is the only non-stable; decoupled analyzer; build-time-only |
| Built-in Kotlin DSL | AGP 9.2.1 (drop `kotlin("android")`, remove `builtInKotlin=false`) | `KotlinAndroidProjectExtension` (still registered) | Reverses commits A/B (`35748ff`+`cbf9689`); re-applies the original B-08-01-era built-in-Kotlin migration |

---

## Required Reversal of Commits A/B (B-08-04 provenance)

If option (a) is chosen, the current bisectable commits `35748ff` (revert to 2.2.20) + `cbf9689` (`builtInKotlin=false` + `kotlin("android")`) must be reversed:

1. **Re-drop `kotlin("android")`** at all 5 sites (the original D-06 removal) — built-in Kotlin owns Kotlin.
2. **Remove `android.builtInKotlin=false`** from `gradle.properties` — built-in Kotlin is ON (the AGP-9 default).
3. **Bump the quartet:** `kotlin 2.2.20 → 2.3.20`, `ksp 2.2.20-2.0.4 → 2.3.9` (compose-compiler + serialization plugin auto-follow).
4. **Swap detekt:** `io.gitlab.arturbosch.detekt 1.23.8` → `dev.detekt 2.0.0-alpha.3` (catalog plugin id + `version` AND root `subprojects{}` `toolVersion` + config import packages).
5. Keep Hilt 2.59.2, Apollo 5.0.0, ktlint 14.2.0, compileSdk 36, targetSdk 35 as already decided.

The `CommonExtension` bare-generics fix and `KotlinAndroidProjectExtension` survival (08-RESEARCH Patterns 1-2) STILL apply unchanged — those were verified against Now-in-Android `main`, which runs **Kotlin 2.3.0 + AGP 9.0** (i.e. NiA was always on the 2.3.x line, making it an even closer match now).

---

## Common Pitfalls (2.3.x-specific, additive to 08-RESEARCH)

### Pitfall A: Choosing Kotlin 2.3.21 (newest stable) instead of 2.3.20
**What goes wrong:** No KSP is published for 2.3.21; KSP 2.3.9 is built for 2.3.20. A 2.3.21 + KSP-2.3.9 pairing is a version mismatch (KSP will warn/fail or auto-downgrade Kotlin).
**Avoid:** Pin Kotlin to **2.3.20** to match KSP 2.3.9. Re-check at execution — if a `2.3.21`-suffixed KSP appears later, 2.3.21 becomes viable.

### Pitfall B: detekt plugin id + group both changed for 2.0
**What goes wrong:** detekt 2.0 moved from `io.gitlab.arturbosch.detekt` (group + plugin id) to **`dev.detekt`** (group + plugin id). A version-only bump leaves the old id/group → unresolved plugin. Config import packages also move (`io.gitlab.arturbosch.detekt.extensions.DetektExtension` → `dev.detekt.*`).
**Avoid:** Update the catalog plugin id, the root `build.gradle.kts subprojects{}` `apply(plugin = "dev.detekt")` + `configure<...DetektExtension>` import, AND the `toolVersion`. Two sources of truth (Pitfall 5 from 08-RESEARCH).

### Pitfall C: detekt config in type-resolution mode would re-trip metadata ceiling
**What goes wrong:** If detekt is configured WITH a classpath/`jvmTarget` (type resolution), it reads project `.class` metadata; an analyzer older than the compile Kotlin would re-hit #8865.
**Avoid:** alpha.3's analyzer (2.3.21) is ≥ compile Kotlin (2.3.20), so even type-resolution mode is safe. Slopper's config is parse-only anyway. Keep it parse-only.

### Pitfall D: KSP2 daemon memory
**What goes wrong:** KSP2 uses materially more memory than KSP1; ubiquitous KSP (Hilt across many modules) can OOM the Gradle daemon, especially on CI.
**Avoid:** Watch the gate; bump `org.gradle.jvmargs -Xmx` if OOM appears. Not a blocker, an operational tuning. `[CITED: KSP docs]`

---

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Dagger 2.59.2's KSP processor runs cleanly under the KSP2 2.3.9 host on Kotlin 2.3.20 (processor/host contract; built against KSP 2.2.20-2.0.3 API which is forward-stable into 2.3.x) | Q3 | If Dagger's processor uses a KSP API surface changed between 2.2.x and 2.3.x KSP2, DI codegen fails. MEDIUM. Mitigation: no higher Dagger exists (2.59.2 is newest); if it fails, AGP-9 is genuinely blocked until Dagger ships a 2.3.x-built release. Verify FIRST at the gate with a `:feature:*` smoke. |
| A2 | detekt 2.0.0-alpha.3 (analyzer Kotlin 2.3.21) reads Kotlin 2.3.20 metadata without #8865 (2.3.20 ≤ 2.3.21) | Q6 | LOW — metadata is backward-readable within a minor; the compat table lists alpha.3 against 2.3.21. If wrong, fall to non-gating detekt (Q6 ladder step 2). |
| A3 | ktlint 14.2.0 + engine 1.6.0 parses Kotlin 2.3.20 source | Q7 | LOW — ktlint parser is largely version-tolerant. If wrong, bump engine `version.set(...)`. |
| A4 | Kotlin 2.3.20 has no AGP 9.2.1 upper-bound conflict (floor 2.2.10, no documented ceiling) | Q1 | LOW — no ceiling clause in AGP 9.2 notes; built-in Kotlin uses the pinned KGP. |
| A5 | Apollo 5.0.0 Gradle codegen (Gratatouille) needs no KSP and is Kotlin-2.3-native for JVM/Android consumers | Q4 | LOW — POM shows gratatouille-wiring, not KSP; changelog states KGP 2.3 build + 2.1 consumer floor. |

**Empirical-verification priority at the gate:** A1 (Dagger/KSP2) FIRST — it is the only MEDIUM-risk item that could still block. Run `./gradlew :feature:<any>:kspDebugKotlin` (a Hilt module) before the full gate.

---

## Sources

### Primary (HIGH — live this session)
- Maven Central `maven-metadata.xml`: `kotlin-gradle-plugin` (2.3.0/2.3.10/2.3.20/2.3.21; latest 2.4.0-RC2), `compose-compiler-gradle-plugin` (2.3.20 present), `com.google.devtools.ksp.gradle.plugin` + `symbol-processing-api` (latest 2.3.9), `com/google/dagger/dagger` (latest 2.59.2), `apollo-runtime` (latest 5.0.0), `dev/detekt/detekt-gradle-plugin` (latest 2.0.0-alpha.3), `io/gitlab/arturbosch/detekt/detekt-gradle-plugin` (latest 1.23.8).
- POMs: `symbol-processing-api-{2.3.7,2.3.8,2.3.9}.pom` → kotlin-stdlib 2.3.20; `dagger-compiler-2.59.2.pom` → KSP 2.2.20-2.0.3 + kotlin 2.2.20 (build deps); `apollo-gradle-plugin-5.0.0.pom` → kotlin-stdlib 2.0.0 + gratatouille-wiring-runtime 0.1.2 (NOT KSP).
- `detekt.dev/docs/introduction/compatibility` — detekt 2.0.0-alpha.1→2.2.20, alpha.2→2.3.0, alpha.3→2.3.21; 1.23.8→2.0.21.
- `raw.githubusercontent.com/apollographql/apollo-kotlin/main/CHANGELOG.md` — "Apollo Kotlin 5 uses KGP 2.3, 2.1 compat for JVM/Android; bump to 2.3.10."

### Secondary (MEDIUM)
- detekt #8865 (1.23.8 fails Kotlin 2.3.0 metadata) + #9170 (maintainer: alpha supports 2.3.20; alpha2 did not → alpha.3).
- Dagger #5099 (2.59.0 ComponentTreeDeps regression → 2.59.2); Dagger 2.59 release / search corroboration ("library + processor target Kotlin 2.0 to support KSP2").
- google/ksp docs: KSP1 unsupported on Kotlin 2.3.0+/AGP 9.0+; KSP2 default since 2.0.0; KSP2 higher memory.
- `brendanlong/lion-otter-recipes#250` — independent real-world "AGP 9 + Kotlin 2.3 + Hilt 2.59+ + KSP" upgrade in flight (corroborates the path).

### Project sources
- `.planning/STATE.md` (B-08-04 — 2.2.x line proven dead under AGP 9), `gradle/libs.versions.toml`, `build.gradle.kts` (detekt/ktlint root pins), `.planning/research/PITFALLS.md`, `08-RESEARCH.md` (Patterns 1-2 still valid; NiA is on Kotlin 2.3), `08-CONTEXT.md` (D-04b — the lifted ceiling).

## Metadata
- Stack coherence: HIGH — every version live-verified; the KSP→Kotlin 2.3.20 pin is exact.
- Apollo / compose-compiler / serialization / KSP2-id: HIGH.
- detekt resolution: HIGH on the compat facts (alpha.3 is the only 2.3.x-capable build); requires a policy exception.
- Hilt/Dagger-under-KSP2: MEDIUM-HIGH (POM-confirmed no-higher-version; runtime-under-KSP2 is the one gate-time empirical).
- **Research date:** 2026-05-31. **Valid until:** ~2026-06-14 (re-confirm KSP 2.3.x top + detekt alpha + Dagger latest at execution).

---

## RESEARCH COMPLETE

**VIABLE.**

A coherent, working version set exists. **No coupled component is a hard wall** — every one has a published 2.3.x-compatible build.

**The minimal coherent version set:**

| Component | Version | Note |
|-----------|---------|------|
| Kotlin (KGP) | **2.3.20** | KSP-matched (NOT 2.3.21 — no KSP for 2.3.21) |
| KSP | **2.3.9** | bare KSP2 scheme; id unchanged (`com.google.devtools.ksp`); pairs with 2.3.20 |
| compose-compiler plugin | **2.3.20** | auto-tracks `kotlin` ref |
| kotlinx-serialization plugin | **2.3.20** | auto-tracks `kotlin` ref |
| Hilt / Dagger | **2.59.2** | unchanged; KSP2-host-compatible processor; newest available |
| Apollo Kotlin | **5.0.0** | unchanged; Kotlin-2.3 native; uses Gratatouille not KSP (off the KSP axis) |
| ktlint plugin | **14.2.0** | unchanged; engine 1.6.0 |
| **detekt** | **2.0.0-alpha.3** | group + plugin id → `dev.detekt`; the ONLY 2.3.x-capable detekt build |

**detekt handling:** detekt 1.23.8 (current stable, and newest on Maven Central) HARD-FAILS on Kotlin 2.3.x metadata (#8865) — a direct hit. There is **no stable detekt build for Kotlin 2.3.x**. Adopt **detekt 2.0.0-alpha.3** (`dev.detekt` group, `dev.detekt` plugin id), whose bundled analyzer (Kotlin 2.3.21) reads 2.3.20 metadata cleanly. Update BOTH the catalog AND the root `build.gradle.kts subprojects{}` `toolVersion` + the `dev.detekt.*` config imports. Fallback if the alpha is unstable: run detekt non-gating, or temporarily drop it from the gate (ktlint remains gating).

**Required NEW policy exception (must be approved):** a **scoped, AR-08-01-style build-time-tool exception** for **detekt `2.0.0-alpha.3`**. Justification mirrors AR-08-01 exactly — detekt is build-time-only static analysis, never ships in the APK (runtime-safety intent of stable-only preserved), and the alpha is the ONLY detekt that works on the Kotlin-2.3.x line that AGP-9 forces. Scope: detekt only; all runtime libraries remain stable-only. Exit: revert to stable detekt 2.0 once released.

**One MEDIUM-risk item to verify FIRST at the gate (not a blocker, but the only thing that could still fail):** Dagger/Hilt 2.59.2's KSP processor running under the KSP2 2.3.9 host (`./gradlew :feature:<any>:kspDebugKotlin`). 2.59.2 is the newest Dagger — if it failed, there would be no higher version to fall to, so confirm it before the full migration. POM + Dagger's "target Kotlin 2.0 for KSP2" statement make it very likely to pass.

**Net:** Lift D-04b for the toolchain quartet, adopt the set above, reverse provenance commits `35748ff`+`cbf9689`, land AGP-9 with built-in Kotlin. This is the sole green path; the 2.2.x ceiling is proven closed.
