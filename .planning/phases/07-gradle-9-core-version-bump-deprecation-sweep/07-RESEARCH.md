# Phase 7: GRADLE-9 — Core Version Bump + Deprecation Sweep — Research

**Researched:** 2026-05-30
**Domain:** Gradle wrapper 8.11.1 → 9.4.1 on a convention-plugin-based multi-module Android Compose build (~16 modules), AGP/DSL/Hilt/compileSdk deliberately FROZEN this phase
**Confidence:** HIGH (every load-bearing version fact verified against official Android docs, Gradle docs, detekt compatibility table, plugin portals, and Maven/registry metadata — not training data)

---

## Summary

The decisive finding of this research **invalidates the phase's optimistic "isolating commit" framing**: **AGP 8.7.3 cannot run on Gradle 9.4.1.** AGP 8.7's official compatibility table lists Gradle **8.9 as both minimum and default** and does not list Gradle 9 at any tier; the Gradle compatibility matrix confirms Gradle 9 is tested **only with AGP 9.0–9.2.x**; and AGP enforces a hard Gradle-version check at configuration start. Therefore the ROADMAP's escape clause fires: **"if AGP 8.7.3 is NOT Gradle-9-compatible, this phase folds forward into Phase 8."** It is not Gradle-9-compatible. The Gradle wrapper bump cannot land as a standalone green commit on the current AGP — it can only be green once AGP is also at 9.x, which is Phase 8's atomic change-set.

This does not make Phase 7 pointless — it changes its **deliverable from "a green build on Gradle 9 + AGP 8.7.3" to "everything that CAN be done and verified independently of the AGP-9 DSL surgery."** Concretely: (1) confirm and pin Gradle 9.4.1 distribution + fresh sha256; (2) enumerate the existing Gradle-8→9 deprecations via `./gradlew help --warning-mode=all` **on the current Gradle 8.11.1** and attribute each to its source plugin; (3) confirm the Gradle-9 compatibility of every toolchain plugin and pre-stage the minimal plugin bumps Gradle 9 forces; (4) confirm Kotlin 2.2.20 / KSP 2.2.20-2.0.4 already satisfy AGP-9's KGP floor. The actual `gradle-wrapper.properties` edit then **rides into Phase 8 as step 1 of that atomic commit** — or, if the planner prefers, Phase 7 lands the wrapper bump on a branch that is knowingly red-on-AGP-8.7.3 and is only validated once Phase 8 lands. Either way, **"build runs GREEN on Gradle 9" is achievable only with AGP 9 present** — the planner must reconcile AGP9-01's success-criterion wording with this reality.

The second-most-consequential finding is a plugin-compatibility tension: **detekt 1.23.8 is NOT officially Gradle-9-compatible** (its compatibility table pins it to Gradle 8.12.1 / Kotlin 2.0.21), and the only detekt line that officially supports Gradle 9 is **2.0.0-alpha.x** — which the stable-only policy forbids. detekt's Gradle plugin applies Kotlin/AGP as `compileOnly` so it commonly *runs* on Gradle 9 with warnings, but this is an unverified-until-tested risk that the planner must surface as an explicit open decision. By contrast, **ktlint-gradle 14.2.0 is confirmed Gradle-9-ready**, **OWASP dependency-check 12.2.2 runs on Gradle 9 but only via `--no-configuration-cache`** (already the documented contract in this repo), and the **baseline-profile plugin 1.4.1** is the one to verify at plan time (its AGP-9-GA variant-API status is the soft spot, but it is not exercised by anything in Phase 7's frozen scope).

**Primary recommendation:** Treat Phase 7 as a **pre-flight + version-pinning + deprecation-enumeration phase whose `gradle-wrapper.properties` flip is sequenced as the first step of Phase 8's atomic commit** — NOT as a standalone green-on-AGP-8.7.3 build. Pin Gradle **9.4.1** (fetch fresh sha256 at execution), pre-stage **ktlint 14.2.0 (already set), dependency-check `--no-configuration-cache` (already set)**, and escalate the **detekt 1.23.8-on-Gradle-9 question** as the one open decision (run-with-warnings vs. stable-only-policy exception).

---

## User Constraints (from milestone STATE.md / REQUIREMENTS.md — no phase CONTEXT.md exists yet)

> No `CONTEXT.md` exists for Phase 7 yet (planning just initialized). These constraints are copied verbatim from the milestone-level locked decisions that bind this phase.

### Locked Decisions (do NOT research alternatives)
- **No Kotlin/KSP bump this milestone.** AGP 9 needs only KGP ≥ 2.2.10; 2.2.20 satisfies. Chasing 2.3.x re-triggers KSP/Hilt churn for no gain. → Kotlin stays `2.2.20`, KSP stays `2.2.20-2.0.4`.
- **Do NOT add `android.enableLegacyVariantApi=true` or `android.newDsl=false`** — no-op/doomed crutches; repo is already clean (zero `applicationVariants`/`buildDir`). (These are AGP-9 flags anyway — irrelevant to a Gradle-only phase, and must not appear.)
- **Stable-only dependency policy.** No alphas/betas/RCs. (This directly collides with detekt's Gradle-9 story — see Open Questions.)
- **Phase 7 scope is Gradle-only:** does NOT touch AGP (stays 8.7.3 in the catalog), the build-logic DSL, Hilt (stays 2.56.2), or compileSdk (stays 35). Those are Phase 8.
- **Gradle target is 9.4.1** (the exact AGP-9.2 floor), NOT 9.5.x — minimal-surface policy.

### Claude's Discretion
- Exact sequencing of the wrapper flip relative to Phase 8 (fold-forward vs. knowingly-red branch) — the planner decides; this research recommends fold-forward.
- Whether to lift `compileSdk`/`targetSdk` into the catalog now (a Phase-7-safe refactor) or defer to Phase 8. Research recommends deferring — it is DSL-adjacent and Phase 8 touches those sites anyway.
- How to record the detekt decision (policy exception vs. run-with-warnings-and-track).

### Deferred Ideas (OUT OF SCOPE this phase)
- AGP 8.7.3 → 9.2.1 (Phase 8), built-in-Kotlin migration (Phase 8), Hilt 2.59.2 (Phase 8), compileSdk 36 (Phase 8), Media3/nextlib + leaf libs (Phase 9), CI assemble/signing probe (Phase 10).
- Detekt 2.x adoption, Kotlin 2.3.x, Media3 1.10.1.

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| **AGP9-01** | Gradle wrapper upgraded to the AGP-9.2 floor (target 9.4.1) with `distributionSha256Sum` re-pinned; the build runs green on Gradle 9 with every plugin (detekt, ktlint, OWASP dependency-check, baseline-profile) confirmed compatible — Gradle-9 deprecations enumerated (`./gradlew help --warning-mode=all`) and resolved. | **PARTIALLY blocked by the AGP-8.7.3 finding.** The wrapper-pin + sha256 + deprecation-enumeration + per-plugin-compatibility-confirmation sub-goals are all achievable in this phase. The **"build runs green on Gradle 9"** sub-goal is NOT achievable while AGP stays 8.7.3 — green-on-Gradle-9 requires AGP 9 (Phase 8). The planner MUST reconcile this: either (a) re-scope AGP9-01's green criterion to "green on Gradle 9 once AGP 9 lands in Phase 8" (fold-forward), or (b) define Phase 7's gate as "pinned + enumerated + plugins-pre-staged" with the green build asserted at the Phase 8 gate. See Architecture Patterns → "The Fold-Forward Decision." |
</phase_requirements>

---

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Gradle distribution resolution | `gradle/wrapper/gradle-wrapper.properties` | — | The wrapper is the single source of the Gradle version + integrity pin |
| Toolchain version pinning | `gradle/libs.versions.toml` | — | All plugin versions resolve from the catalog; Phase 7 touches only Gradle-forced plugin pins |
| Plugin application + lint/quality wiring | `build.gradle.kts` (root) | `build-logic/convention` | detekt + ktlint + dependency-check are applied at the ROOT; convention plugins handle Android wiring (frozen this phase) |
| Deprecation surface | Third-party plugins (detekt/dependency-check) | Gradle itself | Repo's own scripts are clean (`layout.buildDirectory`, no `Project.exec`, no `Convention` API) — the warnings come from plugins, not project code |
| CI gate | `.github/workflows/ci.yml` + `.forgejo/workflows/ci.yml` | — | Compile-only gate; cache key hardcodes `agp8` (do NOT bump to `agp9` this phase — AGP is still 8) |

---

## Standard Stack

### Core (verified versions for THIS phase)

| Item | Current | Phase-7 Target | Purpose | Verification |
|------|---------|----------------|---------|--------------|
| Gradle wrapper | `8.11.1` | **`9.4.1`** | The AGP-9.2 Gradle floor; latest 9.4.x patch | `[CITED: developer.android.com/build/releases/about-agp]` AGP 9.2 → Gradle 9.4.1 min; `[VERIFIED: services.gradle.org]` distribution URL returns HTTP 307 (exists, CDN-redirected) |
| `distributionSha256Sum` | `f397b287…151c6` (for 8.11.1) | **fetch fresh at execution** | Wrapper integrity pin — MUST be replaced | `[ASSUMED]` exact sha — fetch at execution: `curl -sSL https://services.gradle.org/distributions/gradle-9.4.1-bin.zip.sha256` (sandbox blocked the body during research; URL confirmed live) |
| AGP | `8.7.3` | **`8.7.3` (UNCHANGED)** | Frozen this phase | `[VERIFIED: gradle/libs.versions.toml]` |
| Kotlin (KGP) | `2.2.20` | **`2.2.20` (UNCHANGED)** | Already > AGP-9 KGP floor 2.2.10 | `[CITED: blog.jetbrains.com/kotlin AGP-9 guide]` floor = 2.2.10 |
| KSP | `2.2.20-2.0.4` | **unchanged** | Locked to Kotlin exactly | `[VERIFIED: catalog]` |

### Supporting — toolchain plugins (Gradle-9 compatibility confirmation, the heart of AGP9-01)

| Plugin | Catalog version | Gradle-9 status | Action this phase | Verification |
|--------|-----------------|-----------------|-------------------|--------------|
| **ktlint** (`org.jlleitschuh.gradle.ktlint`) | `14.2.0` | ✅ **Compatible** | None — already current | `[CITED: plugins.gradle.org/plugin/org.jlleitschuh.gradle.ktlint]` 14.2.0 released 2026-03-12, supports Isolated Projects; Gradle-9 support landed in 13.1.0, 14.0.1 built for Gradle 9.1 |
| **OWASP dependency-check** (`org.owasp.dependencycheck`) | `12.2.2` | ⚠️ **Runs only with `--no-configuration-cache`** | Keep the existing `--no-configuration-cache` contract; no bump needed | `[CITED: github.com/dependency-check/dependency-check-gradle]` supports Gradle 7.6.4–9.x; 12.2.2 published 2026-05-05. Config-cache incompatibility already documented in `build.gradle.kts:19`. |
| **detekt** (`io.gitlab.arturbosch.detekt`) | `1.23.8` | ❌ **NOT officially Gradle-9-compatible** | **OPEN DECISION** — see Open Questions | `[CITED: detekt.dev/docs/introduction/compatibility]` 1.23.8 → Gradle **8.12.1** / Kotlin **2.0.21**; first Gradle-9 line is **2.0.0-alpha.1** (Kotlin 2.2.20). Stable-only policy forbids the alpha. |
| **baseline-profile** (`androidx.baselineprofile`) | `1.4.1` | 🟡 **Verify at plan time** | Confirm; not exercised in Phase 7's frozen scope | `[CITED: developer.android.com/jetpack/androidx/releases/benchmark]` 1.4.1 stable (Sep 2025); its legacy-variant-API usage is an AGP-9 (Phase 8) concern, not a Gradle-9 concern per se |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Gradle 9.4.1 | Gradle 9.5.1 (exists, 2026-05-12) | Rejected by minimal-surface + stable-only policy; 9.4.1 is the exact AGP-9.2 floor — no benefit to going higher |
| detekt 1.23.8 (run-with-warnings) | detekt 2.0.0-alpha.1 (officially Gradle-9) | **Forbidden by stable-only policy** — it is an alpha. This is the crux of the one open decision. |
| Standalone Gradle wrapper commit | Fold wrapper bump into Phase 8 atomic commit | The standalone commit CANNOT be green on AGP 8.7.3 — fold-forward is the only path to a green Gradle-9 build |

**Gradle 9.4.1 wrapper edit (the exact diff this phase pins, applied in Phase 8's commit-1):**
```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-9.4.1-bin.zip
distributionSha256Sum=<fetch at execution — see command below>
# keep validateDistributionUrl=true, networkTimeout=10000 unchanged
```

**sha256 fetch (execution-time, REQUIRED — do not reuse a training-data value):**
```bash
curl -sSL https://services.gradle.org/distributions/gradle-9.4.1-bin.zip.sha256
# or, atomically with the wrapper task (once on Gradle 9 / Phase 8):
./gradlew wrapper --gradle-version 9.4.1 --gradle-distribution-sha256-sum <sum>
```

---

## Architecture Patterns

### System Architecture Diagram — where the Gradle bump lands and where it breaks

```
  ./gradlew  ──reads──▶  gradle-wrapper.properties  (Gradle 8.11.1 → 9.4.1 + sha256)   ◀── INTEGRATION POINT 1
                                  │
                                  ▼
                         Gradle 9.4.1 runtime
                                  │
              ┌───────────────────┼───────────────────────────┐
              ▼                   ▼                            ▼
   AGP 8.7.3 version-check   build.gradle.kts (root)    build-logic/convention
   ❌ HARD FAILS:            applies: ktlint ✅          applies kotlin.android,
   "AGP 8.7 needs Gradle     detekt ❌(warns/maybe-OK)   CommonExtension wiring
    8.9; Gradle 9 not        dependency-check ⚠️          (FROZEN — Phase 8)
    in tested range"         (--no-config-cache)
              │
              ▼
   ⇒ The build cannot reach a GREEN state on Gradle 9 until AGP is also 9.x.
     Phase 7's green-build goal is therefore deferred into Phase 8's atomic flip.
              │
              ▼
   What IS green / achievable in Phase 7 (no AGP change required):
   ┌──────────────────────────────────────────────────────────────────┐
   │ ./gradlew help --warning-mode=all   (ON Gradle 8.11.1)            │ ← enumerate deprecations
   │   → list every "incompatible with Gradle 9.0" warning + its plugin│
   │ Pin gradle-wrapper.properties target + fetch sha256              │
   │ Confirm ktlint 14.2.0 / depcheck 12.2.2 / baseline-profile 1.4.1 │
   │ Resolve the detekt-on-Gradle-9 decision                          │
   │ Confirm KGP 2.2.20 / KSP 2.2.20-2.0.4 satisfy AGP-9 floor        │
   └──────────────────────────────────────────────────────────────────┘
```

### Pattern 1: The Fold-Forward Decision (the load-bearing architectural call)

**What:** Because AGP 8.7.3 hard-fails on Gradle 9, the Gradle-wrapper flip cannot be a standalone green commit. The wrapper edit becomes **step 1 of Phase 8's atomic change-set** (Gradle → AGP → drop kotlin.android → CommonExtension → compilerOptions → Hilt). Phase 7's deliverable is everything *around* the flip: the pinned target, the deprecation inventory, the plugin-compatibility verdicts, and the detekt decision.
**When to use:** Confirmed for this repo — the AGP-8.7-on-Gradle-9 finding is HIGH confidence.
**Consequence for AGP9-01:** Its "build runs green on Gradle 9" clause is satisfied at the **Phase 8 gate**, not the Phase 7 gate. Phase 7's gate is a **paper/config gate** (enumeration + pins + decisions), plus the still-green-on-Gradle-8.11.1 baseline (`compileDebugSources + detekt + ktlintCheck + test`) proving nothing regressed.

### Pattern 2: Enumerate-on-the-old-Gradle-first

**What:** Run `./gradlew help --warning-mode=all --stacktrace` (and per-task variants) **on the current Gradle 8.11.1**, BEFORE any wrapper change. Gradle 8.x already emits "Deprecated Gradle features were used … incompatible with Gradle 9.0" — this is the cheapest, AGP-safe way to enumerate exactly what Gradle 9 will reject and attribute each warning to its source plugin.
**When to use:** First task of the phase. It runs on AGP 8.7.3 + Gradle 8.11.1 (the current green state) — zero risk.
**Example:**
```bash
./gradlew help --warning-mode=all --stacktrace 2>&1 | tee gradle9-deprecations.log
./gradlew detekt ktlintCheck --warning-mode=all --dry-run 2>&1 | tee -a gradle9-deprecations.log
```
**Expected output:** Warnings attributable to detekt 1.23.8 and/or dependency-check; the repo's own scripts are clean (verified — no `buildDir`, no `Project.exec`, no `Convention`/`getConvention`, no `JavaPluginConvention`; uses `layout.buildDirectory` in `AndroidComposeConventionPlugin.kt:27-28`).

### Anti-Patterns to Avoid
- **Bumping the wrapper to 9.4.1 and expecting a green build on AGP 8.7.3.** It will hard-fail at AGP's Gradle-version check. This is the whole reason the phase folds forward.
- **Bumping the CI cache key `agp8` → `agp9` this phase.** AGP is still 8.7.3; the key must stay `agp8` until Phase 8. A premature flip just misses cache (not catastrophic, but wrong and misleading).
- **Adopting detekt 2.0.0-alpha.x to "fix" the Gradle-9 warning.** Violates stable-only policy. The decision is run-1.23.8-with-warnings vs. a documented policy exception — not a silent alpha bump.
- **Adding `android.builtInKotlin=false` / `android.newDsl=false`.** These are AGP-9 flags; irrelevant on AGP 8.7.3 and explicitly forbidden by the milestone.
- **Lifting compileSdk to the catalog "while we're here."** It is DSL-adjacent and Phase 8 rewrites those sites — defer to avoid touching frozen surface.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Computing the Gradle distribution checksum | A manual `sha256sum` of a hand-downloaded zip | `./gradlew wrapper --gradle-version 9.4.1 --gradle-distribution-sha256-sum <official>` or the published `.sha256` URL | Gradle publishes the canonical checksum; deriving your own invites a download-integrity mismatch and defeats `validateDistributionUrl` |
| Detecting Gradle-9 deprecations | A custom grep over build scripts for known-bad APIs | `./gradlew help --warning-mode=all` | Gradle's own deprecation reporter catches plugin-internal API usage that a source grep cannot see (the warnings here come from *plugins*, not project code) |
| Per-plugin Gradle-9 compatibility | Guessing from version numbers | Each plugin's official compatibility table / release notes | Version-number heuristics fail badly here (ktlint's old `9.x` line vs. current `14.x`; detekt's `1.23.8` looking "recent" but pinned to Gradle 8.12.1) |

**Key insight:** In this phase the *only* deprecation risk is in third-party plugins, not the project's own Gradle code — the repo was already audited clean in the milestone PITFALLS research. So the work is verification + pinning, not code surgery.

---

## Common Pitfalls

### Pitfall 1: Assuming AGP 8.7.3 will run on Gradle 9 for "one isolating commit"
**What goes wrong:** The build fails immediately at configuration with an AGP-enforced Gradle-version error; no module configures. The whole "version-isolation before DSL-isolation" strategy collapses.
**Why it happens:** AGP carries a hard `MIN_GRADLE_VERSION` (and a tested-range check); AGP 8.7's tested Gradle is 8.9, and Gradle 9 tests only AGP 9.0–9.2.x. There is no overlap.
**How to avoid:** Do NOT attempt the standalone wrapper-bump commit on AGP 8.7.3. Fold the wrapper flip into Phase 8's atomic commit-1; treat Phase 7 as enumeration + pinning.
**Warning signs:** `Minimum supported Gradle version is X. Current version is 9.4.1` or `Android Gradle plugin requires …` at sync/configuration time.
**Confidence:** HIGH `[CITED: about-agp compatibility table; Gradle compatibility matrix]`

### Pitfall 2: detekt 1.23.8 emits Gradle-9-incompatible deprecations (or fails) under Gradle 9
**What goes wrong:** Under Gradle 9, detekt 1.23.8 either emits "incompatible with Gradle 9.0" warnings that the strict mode escalates, or fails outright — it was compiled against Gradle 8.12.1.
**Why it happens:** detekt's Gradle plugin applies Kotlin/AGP as `compileOnly`, so it often *runs* on a newer Gradle, but its own internal API usage targets Gradle 8.x. The officially-supported Gradle-9 detekt is 2.0.0-alpha.x only.
**How to avoid:** This is the one open decision (see Open Questions). Most likely path: keep 1.23.8, run it, capture the warning set, and either accept the warnings with a tracked reason or grant a narrow policy exception. Do NOT silently adopt the alpha.
**Warning signs:** detekt-attributed lines in `--warning-mode=all`; or a `detekt`/`ktlintCheck` task failing under Gradle 9 specifically.
**Confidence:** HIGH that 1.23.8 is not *officially* Gradle-9-supported; MEDIUM on whether it *runs* with only warnings (verify by running it on Gradle 9 once AGP 9 is present — it cannot be tested on Gradle 9 in Phase 7 because nothing configures until AGP 9 lands).

### Pitfall 3: OWASP dependency-check in the config-cached main build
**What goes wrong:** `dependencyCheckAnalyze` breaks the configuration cache (which is ON: `org.gradle.configuration-cache=true`), and Gradle 9's stricter enforcement turns the existing warning into a hard failure.
**Why it happens:** The plugin is config-cache-incompatible (documented in `build.gradle.kts:19`).
**How to avoid:** Keep the existing `--no-configuration-cache` invocation contract — it is already the project's documented approach and is an out-of-band CI task, not in the main `compile/test` path. No version bump needed (12.2.2 supports Gradle 9.x in that mode).
**Warning signs:** `An issue was found … incompatible with the configuration cache` naming the dependency-check task.
**Confidence:** HIGH `[VERIFIED: build.gradle.kts:18-32]` + `[CITED: dependency-check-gradle repo]`

### Pitfall 4: Re-pinning the wrong sha256 (stale training value)
**What goes wrong:** A `distributionSha256Sum` copied from memory/training data won't match the real 9.4.1 zip; `validateDistributionUrl=true` + the checksum check abort the wrapper download.
**How to avoid:** Fetch the checksum at execution time from `…/gradle-9.4.1-bin.zip.sha256` (or via the `wrapper` task). Never hard-code a remembered hash.
**Warning signs:** `Verification of Gradle distribution failed! … expected … but was …`.
**Confidence:** HIGH (general Gradle wrapper behavior).

---

## Code Examples

### Enumerate deprecations on the current Gradle (AGP-safe, run first)
```bash
# Source: Gradle docs — warning-mode reporting. Runs on the CURRENT 8.11.1 + AGP 8.7.3.
./gradlew help --warning-mode=all --stacktrace 2>&1 | tee gradle9-deprecations.log
# Per-quality-task surface (where detekt/ktlint deprecations show):
./gradlew detekt ktlintCheck --warning-mode=all --dry-run 2>&1 | tee -a gradle9-deprecations.log
```

### Confirm the KGP/AGP-9 floor (no bump needed — proves AGP9-01 criterion 4)
```bash
# Catalog already pins kotlin=2.2.20, ksp=2.2.20-2.0.4.
# AGP 9 floor is KGP >= 2.2.10; 2.2.20 > 2.2.10  ⇒ satisfied, no change.
grep -E '^(kotlin|ksp) ' gradle/libs.versions.toml
```

### Wrapper pin (the edit Phase 8 commit-1 applies; sha fetched live)
```properties
# Source: services.gradle.org distribution layout
distributionUrl=https\://services.gradle.org/distributions/gradle-9.4.1-bin.zip
distributionSha256Sum=<curl -sSL https://services.gradle.org/distributions/gradle-9.4.1-bin.zip.sha256>
```

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| ktlint-gradle `9.x` versioning | ktlint-gradle `13.x`/`14.x` (Gradle-9 + Isolated Projects) | Gradle-9 support in 13.1.0; 14.x current | Repo's `14.2.0` is already on the modern, Gradle-9-ready line — the old `9.x` numbers in search results are a red herring |
| detekt 1.23.x (Gradle 8.x line) | detekt 2.0.0-alpha.x (first Gradle-9 line) | detekt 2.0 alpha series | Stable-only policy forbids the alpha; 1.23.8 is the latest *stable* but is Gradle-8-targeted |
| `kotlinOptions{}` android DSL | `kotlin { compilerOptions {} }` (built-in Kotlin) | AGP 9.0 | **Phase 8 concern, not Phase 7** — listed only so the planner does not pull it forward |

**Deprecated/outdated for this phase:**
- The ROADMAP's "run AGP 8.7.3 on Gradle 9 for one isolating commit" hypothesis — **disproven**; replaced by fold-forward.

---

## Runtime State Inventory

> This is a version-bump phase touching only build config, not a rename/migration. Included for completeness — no runtime-state migration applies.

| Category | Items Found | Action Required |
|----------|-------------|------------------|
| Stored data | None — no datastore keys reference Gradle/AGP versions. Verified: phase touches only `gradle-wrapper.properties` + catalog. | None |
| Live service config | CI cache key in `.github/workflows/ci.yml` + `.forgejo/workflows/ci.yml` hardcodes `…-jdk17-agp8-…`. **Do NOT change this phase** (AGP still 8). | None this phase (Phase 8 flips `agp8`→`agp9`) |
| OS-registered state | None. | None |
| Secrets/env vars | None reference Gradle version. The `keystore.properties` read in `app/build.gradle.kts` is config-cache-sensitive but unaffected by a Gradle-version-only change while AGP is frozen. | None |
| Build artifacts | Gradle wrapper dists cache (`~/.gradle/wrapper/dists`) will fetch the new 9.4.1 dist when the flip lands (in Phase 8). The `build-logic` composite build recompiles against whatever Gradle runs it — no stale artifact issue from a version-only pin. | None this phase |

**Verified clean:** No project-authored Gradle code uses Gradle-9-removed APIs (`buildDir`, `Project.exec`, `Convention`/`getConvention`, `JavaPluginConvention`, `archivesBaseName`) — grep across all `*.kts`/`*.kt` returned only `layout.buildDirectory` (the *correct* replacement) and benign `sourceCompatibility` usage.

---

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Internet → services.gradle.org | Wrapper dist + sha256 fetch | ✓ (URL returns HTTP 307 redirect to CDN) | 9.4.1 dist confirmed live | — |
| JDK 17 (toolchain) + JDK 21 (daemon) | Gradle 9.4.1 run/compile | ✓ (existing CI + local setup) | 17/21 | — |
| `./gradlew` (wrapper) | All build invocations | ✓ | currently 8.11.1 | — |
| `curl` | sha256 fetch at execution | ✓ (sandbox blocked the .sha256 *body* during research, but the tool is present) | — | `./gradlew wrapper --gradle-distribution-sha256-sum` |

**Missing dependencies with no fallback:** None.
**Note:** The research sandbox could not retrieve the `.sha256` *body* (301/307 CDN redirect returned no payload to the sandboxed curl). The distribution URL is confirmed live (HTTP 307). The exact sha256 MUST be fetched at execution time in a non-sandboxed context — it is the one `[ASSUMED]` value in this research.

---

## Validation Architecture

> `workflow.nyquist_validation` is not disabled; section included.

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit4 + JUnit5 (Jupiter 6.1.0) + Robolectric 4.16.1 + MockK + Turbine; build-verification via Gradle tasks |
| Config file | `gradle/libs.versions.toml` (test deps); no separate test runner config |
| Quick run command | `./gradlew :core:common:compileDebugKotlin` (fast convention-plugin-applies smoke) |
| Full suite command | `./gradlew compileDebugSources detekt ktlintCheck test` (the established CI gate) |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| AGP9-01 (pin) | `gradle-wrapper.properties` targets 9.4.1 with a real sha256 | config-assert | `grep '9.4.1' gradle/wrapper/gradle-wrapper.properties` (post-flip, in Phase 8) | ✅ existing file |
| AGP9-01 (deprecations) | Every Gradle-9 deprecation enumerated + attributed | manual+automated | `./gradlew help --warning-mode=all` (on Gradle 8.11.1) → `gradle9-deprecations.log` | ✅ (log is the artifact) |
| AGP9-01 (plugins) | ktlint/depcheck/baseline-profile confirmed Gradle-9-compatible; detekt decision recorded | research-assert | This RESEARCH.md + the recorded detekt decision | ✅ |
| AGP9-01 (green) | Build green on Gradle 9 | integration | `./gradlew compileDebugSources detekt ktlintCheck test` **on Gradle 9** | ❌ **deferred to Phase 8** (requires AGP 9) |
| AGP9-01 (KGP floor) | KGP 2.2.20 / KSP 2.2.20-2.0.4 ≥ AGP-9 floor | config-assert | `grep -E '^(kotlin\|ksp) ' gradle/libs.versions.toml` (2.2.20 > 2.2.10) | ✅ |
| Regression | Nothing broke on the current toolchain | integration | `./gradlew compileDebugSources detekt ktlintCheck test` **on Gradle 8.11.1** (must stay green) | ✅ existing CI gate |

### Sampling Rate
- **Per task commit:** `./gradlew :core:common:compileDebugKotlin` (still on 8.11.1 in Phase 7)
- **Per phase gate (Phase 7):** `gradle9-deprecations.log` produced + every plugin verdict recorded + detekt decision logged + the existing `compileDebugSources detekt ktlintCheck test` STILL green on 8.11.1 (no regression).
- **Green-on-Gradle-9 gate:** asserted at the **Phase 8** gate, not Phase 7.

### Wave 0 Gaps
- [ ] `gradle9-deprecations.log` — the enumeration artifact (produced by the first task; covers AGP9-01 deprecation criterion)
- [ ] A recorded **detekt-on-Gradle-9 decision** (run-with-warnings vs. policy exception) — the one open governance item
- [ ] No new test files needed — Phase 7 is config/verification; the existing CI gate is the regression net.

*(If the planner adopts fold-forward, the only Phase-7 "test" that runs green on Gradle 9 is none — that gate moves to Phase 8. Phase 7's gate is the no-regression-on-8.11.1 build + the enumeration/decision artifacts.)*

---

## Security Domain

> `security_enforcement` absent ⇒ enabled. Scope is build-toolchain only; no app-code security surface changes.

### Applicable ASVS Categories
| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V14 Configuration / Build Integrity | **yes** | `distributionSha256Sum` pin + `validateDistributionUrl=true` on the wrapper — fetch the *official* checksum; never hand-roll |
| V6 Cryptography | no (no code change) | — |
| V5 Input Validation | no | — |
| Supply-chain (dependency scanning) | **yes** | OWASP dependency-check 12.2.2 stays wired (`failBuildOnCVSS=7.0`); keep `--no-configuration-cache` so the scan still runs under Gradle 9 |

### Known Threat Patterns for this phase
| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Tampered Gradle distribution | Tampering | `distributionSha256Sum` from the official `.sha256` + `validateDistributionUrl=true` |
| Stale/disabled vuln scan after toolchain churn | Repudiation/Info-disclosure | Verify `dependencyCheckAnalyze` still executes (with `--no-configuration-cache`) post-bump; do not let the scan silently drop |

---

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Exact `distributionSha256Sum` for gradle-9.4.1-bin.zip | Standard Stack | LOW — fetched/validated at execution; a wrong value fails the download loudly, never ships silently |
| A2 | detekt 1.23.8 *runs* (with warnings) on Gradle 9 rather than hard-failing | Open Questions / Pitfall 2 | MEDIUM — if it hard-fails, the detekt decision escalates from "accept warnings" to "must change detekt version or exclude the task"; cannot be tested until AGP 9 is present (Phase 8) |
| A3 | baseline-profile plugin 1.4.1 is Gradle-9-tolerant (its AGP-9 variant-API gap is a Phase-8 issue, not a Phase-7 Gradle issue) | Supporting stack | LOW for Phase 7 (not exercised); MEDIUM for Phase 8 — flagged there already |
| A4 | AGP 8.7.3 *hard-fails* (not merely warns) on Gradle 9.4.1 | Pitfall 1 / Fold-Forward | LOW — even if it only warned, the milestone's "green" bar and the Gradle-tested-AGP-range (9.0–9.2 only) make running production on it unacceptable; fold-forward holds either way |

---

## Open Questions

1. **detekt 1.23.8 on Gradle 9 — run-with-warnings or stable-only-policy exception?**
   - What we know: 1.23.8 is the latest *stable* detekt but is officially Gradle-8.12.1-targeted; the only Gradle-9-supporting detekt is 2.0.0-alpha.x; the stable-only policy forbids alphas. detekt applies Kotlin/AGP as `compileOnly`, so it usually still *runs* on a newer Gradle with deprecation warnings.
   - What's unclear: whether 1.23.8 merely warns or actually fails under Gradle 9 — **and this cannot be empirically tested in Phase 7** because nothing configures on Gradle 9 until AGP 9 lands (Phase 8).
   - Recommendation: **Defer the empirical test to Phase 8's first green-on-Gradle-9 build.** In Phase 7, record the decision *intent*: keep detekt 1.23.8, plan to accept its Gradle-9 deprecation warnings (tracked), and only revisit a detekt-2.x policy exception if it hard-fails in Phase 8. Do NOT adopt the alpha pre-emptively.

2. **Phase 7 gate definition given the fold-forward.**
   - What we know: "build green on Gradle 9" is impossible while AGP is 8.7.3.
   - What's unclear: whether the planner re-words AGP9-01's green criterion to "asserted at Phase 8" (cleanest) or keeps Phase 7 as a knowingly-red branch.
   - Recommendation: Re-scope Phase 7's gate to **(a)** pinned wrapper target + fetched sha256 recorded, **(b)** `gradle9-deprecations.log` produced and attributed, **(c)** all four plugins' verdicts recorded + detekt decision logged, **(d)** the existing `compileDebugSources detekt ktlintCheck test` STILL green on 8.11.1 (no regression). Move the green-on-Gradle-9 assertion to the Phase 8 gate.

---

## Sources

### Primary (HIGH confidence)
- `[CITED]` developer.android.com/build/releases/about-agp — AGP↔Gradle compatibility table: AGP 8.7 → Gradle 8.9 min (no Gradle 9); AGP 9.0→9.1.0, 9.1→9.3.1, 9.2→9.4.1.
- `[CITED]` developer.android.com/build/releases/agp-9-0-0-release-notes — AGP 9.0 min Gradle 9.1.0.
- `[CITED]` docs.gradle.org/current/userguide/compatibility.html — Gradle 9 tested with AGP 9.0–9.2.x (AGP 8.7 outside tested range).
- `[CITED]` detekt.dev/docs/introduction/compatibility — detekt 1.23.8 → Gradle 8.12.1 / Kotlin 2.0.21; first Gradle-9 line = 2.0.0-alpha.1.
- `[CITED]` plugins.gradle.org/plugin/org.jlleitschuh.gradle.ktlint — ktlint-gradle 14.2.0 (2026-03-12), Gradle-9 + Isolated Projects ready.
- `[CITED]` github.com/dependency-check/dependency-check-gradle — supports Gradle 7.6.4–9.x; 12.2.2 (2026-05-05); config-cache caveat.
- `[CITED]` developer.android.com/jetpack/androidx/releases/benchmark — baseline-profile/benchmark 1.4.1 stable.
- `[VERIFIED]` Repo files: `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties`, `gradle.properties`, `build.gradle.kts`, all 6 convention plugins + `KotlinAndroid.kt`, `settings.gradle.kts`, CI workflow; grep audit for Gradle-9-removed APIs (clean).
- `[VERIFIED]` services.gradle.org/distributions/gradle-9.4.1-bin.zip — HTTP 307 (live).

### Secondary (MEDIUM confidence)
- `[CITED]` Gradle/AGP forum + Medium compatibility writeups — corroborate AGP-8.7-needs-Gradle-8.9 and AGP-version-check hard-fail behavior.

### Tertiary (LOW confidence — flagged)
- The exact 9.4.1 `distributionSha256Sum` (A1) — fetch at execution.
- Whether detekt 1.23.8 warns-vs-fails on Gradle 9 (A2) — untestable until Phase 8.

---

## Metadata

**Confidence breakdown:**
- AGP 8.7.3 × Gradle 9 incompatibility (the phase-defining finding): **HIGH** — multiple official sources agree (about-agp table, Gradle compatibility matrix, AGP version-check behavior).
- Plugin compatibility (ktlint ✅, depcheck ⚠️ config-cache, baseline-profile 🟡 verify): **HIGH** for ktlint/depcheck, MEDIUM for baseline-profile-on-AGP-9 (Phase-8 concern).
- detekt Gradle-9 status: **HIGH** that 1.23.8 is not officially supported; **MEDIUM** on runs-with-warnings vs. fails.
- Gradle 9.4.1 as the correct target + URL: **HIGH**; exact sha256: deferred to execution.

**Research date:** 2026-05-30
**Valid until:** 2026-06-29 (30 days; toolchain versions are stable releases — but re-confirm the detekt-2.x-stable line and any 12.2.x dependency-check patch at plan time)
