# Stack Research — AGP-9 Toolchain Modernization (DEPS-17 / v1.1)

**Domain:** Android Compose multi-module build-toolchain upgrade (pure version bumps, no user features)
**Researched:** 2026-05-30
**Confidence:** HIGH (every target version verified against Google Maven, Maven Central `maven-metadata.xml`, or official AGP/Gradle release notes — not training data)

---

## TL;DR — The Recommended Target Set

Copy-pasteable `[versions]` deltas. **FROM → TO**:

| Key | FROM (current) | TO (target) | Why |
|-----|----------------|-------------|-----|
| `agp` | `8.7.3` | **`9.2.1`** | Latest stable AGP 9 patch (Google Maven, last-modified 2026-05-05); 9.2 GA Apr 2026 |
| Gradle wrapper | `8.11.1` | **`9.4.1`** | Exact minimum AGP 9.2 demands; latest 9.4.x patch |
| `hilt` | `2.56.2` | **`2.59.2`** | First Hilt line with AGP-9 Gradle-plugin support (2.59); .2 fixes incremental + jetifier + ComponentTreeDeps |
| `kotlin` | `2.2.20` | **`2.2.20` (NO CHANGE)** | AGP 9 needs KGP ≥ 2.2.10; 2.2.20 already satisfies it. Do NOT chase 2.3.x. |
| `ksp` | `2.2.20-2.0.4` | **`2.2.20-2.0.4` (NO CHANGE)** | Latest KSP on the Kotlin-2.2.20 line; already correct. Locked to Kotlin exactly. |
| `compileSdk` | `35` | **`36`** | Milestone target; AGP 9.2 supports up to 36.1, so 36 is well inside range |
| `media3` | `1.9.1` | **`1.10.0`** | Capped by nextlib lockstep — see warning below. NOT 1.10.1. |
| `nextlibMedia3Ext` | `1.9.1-0.11.0` | **`1.10.0-0.12.1`** | Latest published nextlib; pairs with Media3 **1.10.0** only |
| JDK | 17 compile / 21 daemon | **unchanged** | AGP 9.2 + Gradle 9.4.1 both require JDK 17 min; 21 daemon is fine |

**Plus the now-unblocked leaf libs** the milestone calls out (`activity-compose` → 1.13, `core-ktx` → 1.18) — these are not toolchain-locked, verify-and-bump independently during the leaf-cleanup step.

---

## Recommended Stack — Detailed Compatibility Matrix

### Core toolchain (the lockstep cluster)

| Technology | Version | Min dependency it imposes | Source (verified) |
|------------|---------|---------------------------|-------------------|
| **AGP** | `9.2.1` | Gradle ≥ 9.4.1, JDK ≥ 17, KGP ≥ 2.2.10, Build-Tools 36.0.0 | Google Maven metadata (9.0.0/9.0.1/9.1.0/9.1.1/9.2.0/**9.2.1**); AGP 9.2.0 release notes |
| **Gradle** | `9.4.1` | JDK 17 min (run + compile) | gradle.org/releases — 9.4.1 is the latest 9.4.x (2026-03-19); 9.5.1 also exists but 9.4.1 is the exact AGP-9.2 floor |
| **Hilt / Dagger** | `2.59.2` | AGP ≥ 9.0.0, Gradle ≥ 9.1 (subsumed by AGP 9.2's 9.4.1) | Maven Central `<release>2.59.2</release>`; google/dagger releases |
| **Kotlin (KGP)** | `2.2.20` | — (no change) | JetBrains Kotlin blog "Update your projects for AGP 9": AGP 9.0 needs KGP ≥ **2.2.10**; 2.2.20 satisfies |
| **KSP** | `2.2.20-2.0.4` | Locked to Kotlin 2.2.20 exactly | Maven Central metadata: `2.2.20-2.0.4` is the highest on the 2.2.20 line |
| **compileSdk** | `36` | Build-Tools 36.0.0 | AGP 9.2 supports up to API 36.1 (release notes) |
| **Media3** | `1.10.0` | compileSdk 36 friendly; re-enables async MediaCodec decryption on API 36+ | developer.android.com/jetpack/androidx/releases/media3 |
| **nextlib-media3ext** | `1.10.0-0.12.1` | Media3 **1.10.0** (lockstep) | Maven Central metadata — `1.10.0-0.12.1` is the newest published |

### Answering each numbered question precisely

**1. AGP 9.x — best stable patch?** → **`9.2.1`**.
Google Maven lists `9.0.0, 9.0.1, 9.1.0, 9.1.1, 9.2.0, 9.2.1`. 9.2.0 was the April-2026 GA minor; **9.2.1 is a stable patch published 2026-05-05** (verified via POM last-modified header). Take the patch. Gradle minimums per AGP minor (from official about-agp / release notes):
- AGP 9.0.x → Gradle ≥ **9.1.0**
- AGP 9.1.x → Gradle ≥ **9.3.1**
- AGP 9.2.x → Gradle ≥ **9.4.1**

**2. Gradle.** AGP 9.2.x requires Gradle **9.4.1** minimum. Use exactly `9.4.1`.
Wrapper distribution URL pattern:
```
distributionUrl=https\://services.gradle.org/distributions/gradle-9.4.1-bin.zip
```
`distributionSha256Sum` must be re-pinned. Fetch the official checksum at upgrade time:
```
curl -s https://services.gradle.org/distributions/gradle-9.4.1-bin.zip.sha256
```
(or `./gradlew wrapper --gradle-version 9.4.1 --gradle-distribution-sha256-sum <sum>`). The current pin `f397b287...151c6` is for 8.11.1 and MUST be replaced. Keep `validateDistributionUrl=true`.

**3. Hilt / Dagger.** Minimum with AGP-9 support = **2.59** (first line whose Hilt Gradle plugin supports AGP 9.0 and drops AGP 8). Current latest = **2.59.2** (no 2.59.3 / 2.60 exists — Maven Central `<latest>2.59.2</latest>`). Use **2.59.2**: it fixes the AGP-9 regressions found in 2.59 (slow incremental builds from `HiltSyncTask`, `jetifierEnabled=true` compile errors via dagger#5099, and the `android-classes` transform replaced with attribute rules per #5116). No AGP-9.2-specific caveats beyond those, which 2.59.2 already resolves.

**4. KSP ↔ Kotlin lock.** KSP is pinned to the exact Kotlin version: `2.2.20-<n>`. The highest KSP on the 2.2.20 line is **`2.2.20-2.0.4`** — already in the project, no change. KSP only advances past this if Kotlin advances (next lines are `2.2.21-2.0.5`, then `2.3.x-…`). AGP 9 / Hilt do **NOT** force a Kotlin bump (see #5), so KSP stays at `2.2.20-2.0.4`.

**5. Kotlin minimum from AGP's built-in Kotlin.** AGP 9.0 added a runtime dependency on **KGP ≥ 2.2.10** for built-in Kotlin. Our **2.2.20 satisfies it** (2.2.20 > 2.2.10). No Kotlin bump required. The build-logic change here is structural, not a version bump: AGP 9 enables built-in Kotlin by default, so the `org.jetbrains.kotlin.android` plugin must be **removed** from convention plugins (applying it under AGP 9 errors: "the plugin is no longer required for Kotlin support since AGP 9.0"). `android.builtInKotlin=false` is an escape hatch but is being removed in AGP 10 — migrate properly now.

**6. compileSdk 36.** Requires **Build-Tools 36.0.0** (AGP 9.2 default). AGP 9.2 supports up to API **36.1** (per the 9.2.0 release notes). compileSdk **36 is comfortably inside range**. (Note: the `about-agp` summary page shows "37" for 9.2's max API — this is a documentation inconsistency; the authoritative 9.2.0 release-notes compatibility table says **36.1**. Either way, **36 is supported** — no risk to this milestone. Flagged below.)

**7. Media3 + nextlib lockstep (HARD CONSTRAINT).** Latest stable Media3 is **1.10.1** (2026-05-12). BUT nextlib-media3ext's newest publish is **`1.10.0-0.12.1`**, which pairs with Media3 **1.10.0** — there is **no `1.10.1-…` nextlib build yet**. Since these are version-locked (nextlib's prefix must match the Media3 version), **target Media3 `1.10.0`, not `1.10.1`.** Going to 1.10.1 would desync the FFmpeg extension. Media3 1.10.0 (2026-03-26) is fully stable and satisfies the milestone.

**8. JDK.** AGP 9.2 and Gradle 9.4.1 both require **JDK 17 minimum** for run and compile. The existing setup (JDK 17 toolchain for compilation, JDK 21 for the Gradle daemon) is fully supported — Gradle 9.x runs happily on JDK 21, and a JDK-17 `kotlin { jvmToolchain(17) }` / `compileOptions` target is unaffected. **No JDK change needed.** Keep daemon on 21 if desired; no concerns.

---

## Integration Ordering Constraints (what must bump before what)

This is the load-bearing guidance for the roadmapper. The cluster is a near-atomic flip — but there's a forced order *within* it:

```
1. Gradle wrapper 8.11.1 → 9.4.1   (must precede AGP; AGP 9.2 refuses Gradle < 9.4.1)
2. AGP 8.7.3 → 9.2.1               (the keystone; triggers built-in-Kotlin behavior)
3. build-logic migration           (SAME change-set as AGP bump — see below)
   - remove `org.jetbrains.kotlin.android` plugin application
   - audit/disable legacy variant API (applicationVariants / baseline-profile)
4. Hilt 2.56.2 → 2.59.2            (REQUIRES AGP 9 already present — 2.59 drops AGP 8)
5. compileSdk 35 → 36 + Build-Tools 36.0.0
6. Media3 1.9.1 → 1.10.0 + nextlib 1.9.1-0.11.0 → 1.10.0-0.12.1  (single locked pair)
7. Leaf libs (activity-compose 1.13, core-ktx 1.18) — independent, last
```

**Why steps 1–4 are effectively atomic:** Gradle < 9.4.1 blocks AGP 9.2; AGP 8 blocks Hilt 2.59; Hilt 2.59 blocks AGP 8. You cannot land them piecemeal and keep the build green between each — they go in one coordinated change-set (or one tightly-sequenced phase). compileSdk/Media3/leaf libs (5–7) CAN be separate follow-up steps since they don't gate each other.

---

## What NOT to Bump (explicit guardrails)

| Do NOT touch | Why |
|--------------|-----|
| **Kotlin past 2.2.20** | AGP 9 only needs KGP 2.2.10. 2.3.x would force a new KSP line (`2.3.x-…`), re-trigger the Dependabot lockstep churn, and risk the KSP/Hilt incompatibility seen on the 2.3.0 line. Out of scope. |
| **KSP past `2.2.20-2.0.4`** | Pinned to Kotlin exactly. Bumping requires bumping Kotlin first. |
| **Media3 to 1.10.1** | nextlib has no `1.10.1-…` pairing. Would desync FFmpeg extension. Stay at 1.10.0. |
| **Gradle to 9.5.x** | 9.4.1 is the exact AGP-9.2 floor and is stable. 9.5.1 exists but adds risk for no milestone benefit (stable-only / minimal-surface policy). |
| **Hilt to a non-existent 2.60/2.59.3** | 2.59.2 is the latest. Don't speculate. |
| **minSdk / targetSdk** | Out of scope this milestone (PROJECT.md constraint). compileSdk only. |
| **Apollo / OkHttp / Coil / Compose BOM** | Already landed in DEPS (Apollo 5.0.0, OkHttp 5.3.2, Coil 3.4.0, BOM 2026.05.01). Not part of the AGP-9 cluster. Leave them. |

---

## Version Compatibility Table (cross-tool, explicit)

| Package A | Compatible With | Notes |
|-----------|-----------------|-------|
| AGP 9.2.1 | Gradle 9.4.1+ | Hard minimum; lower Gradle is rejected at sync |
| AGP 9.2.1 | JDK 17+ | run + compile |
| AGP 9.2.1 | KGP 2.2.10+ | built-in Kotlin runtime dep; 2.2.20 ✓ |
| AGP 9.2.1 | compileSdk ≤ 36.1, Build-Tools 36.0.0 | 36 ✓ |
| Hilt 2.59.2 | AGP 9.0.0+ only | drops AGP 8 entirely — cannot coexist with old AGP |
| KSP 2.2.20-2.0.4 | Kotlin 2.2.20 exactly | exact-match lock |
| Kotlin 2.2.20 | AGP 9.2.1 (built-in Kotlin) | ≥ 2.2.10 satisfied |
| Media3 1.10.0 | nextlib-media3ext 1.10.0-0.12.1 | version-prefix lockstep |
| Gradle 9.4.1 | JDK 17 (min) / 21 (daemon) | both supported |

---

## Sources (all verified 2026-05-30)

- **Google Maven metadata** — `dl.google.com/.../com/android/tools/build/gradle/maven-metadata.xml` → AGP 9.x list incl. **9.2.1**; POM last-modified 2026-05-05. (HIGH — authoritative artifact registry)
- **Maven Central metadata** — `repo1.maven.org/.../com/google/dagger/dagger/maven-metadata.xml` → `<latest>2.59.2</latest>`; `.../nextlib-media3ext/maven-metadata.xml` → newest `1.10.0-0.12.1`; `.../ksp/symbol-processing-api/maven-metadata.xml` → `2.2.20-2.0.4` latest on 2.2.20 line. (HIGH)
- **AGP 9.2.0 release notes** — developer.android.com/build/releases/agp-9-2-0-release-notes → Gradle 9.4.1 min, JDK 17, Build-Tools 36.0.0, max API 36.1. (HIGH)
- **About AGP** — developer.android.com/build/releases/about-agp → per-minor Gradle minimums (9.0→9.1.0, 9.1→9.3.1, 9.2→9.4.1). (HIGH; note max-API-level cell shows "37" — conflicts with 9.2.0 release notes' "36.1", flagged below)
- **JetBrains Kotlin blog** "Update your projects for AGP 9" (blog.jetbrains.com/kotlin/2026/01) → AGP 9.0 runtime dep on KGP ≥ 2.2.10. (HIGH)
- **Migrate to built-in Kotlin** — developer.android.com/build/migrate-to-built-in-kotlin → remove `org.jetbrains.kotlin.android`; built-in Kotlin default in 9.0; opt-out gone in AGP 10. (HIGH)
- **google/dagger releases & issues** — #5099 / #5116 (jetifier, ComponentTreeDeps, android-classes transform), 2.59→2.59.2 fix chain. (HIGH for version, MEDIUM for fix attribution from issue threads)
- **gradle.org/releases** — Gradle 9.4.1 (2026-03-19) latest 9.4.x; 9.5.1 latest overall. (HIGH)
- **Media3 releases** — developer.android.com/jetpack/androidx/releases/media3 → 1.10.0 (2026-03-26), 1.10.1 (2026-05-12). (HIGH)

---

## Flags / Where Sources Disagree

1. **compileSdk max for AGP 9.2: 36.1 vs 37.** The `about-agp` summary table renders "37" for AGP 9.2; the AGP 9.2.0 **release-notes** compatibility section says "**36.1**". WebFetch also mis-scraped the 9.1.1 page as "37.0" (likely cross-page bleed). **Resolution: trust the release notes → 36.1.** *Impact on this milestone: NONE* — we target compileSdk **36**, which every reading supports. No action needed beyond awareness.

2. **WebFetch date hallucinations.** Several WebFetch summaries stamped 2026 Dagger/KSP releases as "Feb 2025 / May 26" etc. The **version strings** were cross-checked against Maven metadata and are correct; ignore the fabricated dates in those summaries.

3. **Gradle 9.5.1 exists** (2026-05-12) but is deliberately NOT chosen — 9.4.1 is the exact AGP-9.2 floor and matches the stable-only/minimal-surface policy.

---
*Stack research for: AGP-9 toolchain modernization (DEPS-17)*
*Researched: 2026-05-30*
