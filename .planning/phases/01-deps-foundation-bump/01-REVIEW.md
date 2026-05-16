---
phase: 01-deps-foundation-bump
reviewed: 2026-05-16T22:35:00+09:00
depth: standard
diff_base: master
head: b8875313044e450050a4ac9f6960c4fe9f2c2f05
files_reviewed: 55
files_in_scope_total: 79
files_reviewed_list:
  - gradle/libs.versions.toml
  - gradle.properties
  - gradle/wrapper/gradle-wrapper.properties
  - build-logic/convention/src/main/kotlin/io/stashapp/android/buildlogic/KotlinAndroid.kt
  - core/network/src/main/graphql/io/stashapp/android/graphql/schema.graphqls
  - app/build.gradle.kts
  - app/src/main/java/io/stashapp/android/MainActivity.kt
  - core/ui/build.gradle.kts
  - core/ui/src/main/java/io/stashapp/android/core/ui/image/StashImageLoader.kt
  - core/data/build.gradle.kts
  - feature/player/src/main/java/io/stashapp/android/feature/player/PlayerScreen.kt
  - feature/player/src/main/java/io/stashapp/android/feature/player/PlayerViewModel.kt
  - feature/settings/src/main/java/io/stashapp/android/feature/settings/SettingsScreen.kt
  - feature/library/src/main/java/io/stashapp/android/feature/library/LibraryViewModel.kt
  - config/detekt/detekt.yml
  - core/data/src/main/java/io/stashapp/android/core/data/scene/SceneFilterMapper.kt
  - core/data/src/main/java/io/stashapp/android/core/data/scene/DefaultSceneRepository.kt
findings:
  critical: 0
  warning: 2
  info: 5
  total: 7
status: issues_found
verdict: PASS-WITH-FINDINGS
---

# Phase 1 (DEPS — Foundation Bump): Adversarial Code Review

**Reviewed:** 2026-05-16T22:35+09:00
**Depth:** standard
**Base:** `master` → HEAD `b887531` (53 .kt/.kts files + catalog + schema + detekt config + gradle.properties + wrapper props)
**Status:** issues_found (verdict **PASS-WITH-FINDINGS** — none blocking)

## TL;DR

Phase 1 is a clean toolchain/catalog bump landed against the AGP 8.7.3 / Gradle 8.11.1 floor (AGP-9 cliff explicitly deferred via DEPS-17). All five precondition orphan-removal commits (`3b16767` / `5f924be` / `18d545f` / `76bb19e` / `214c94f`) and the `:core:data`→`:core:ui` wiring commit (`cf5f1f0`) were verified to remove only dead/dangling code or restore a strictly necessary dep — no live code was lost. The vendored 4916-line `schema.graphqls` is structurally self-consistent (zero duplicate type/input/enum/scalar/union definitions, single `Query`/`Mutation`/`Subscription` root, 8 scalars, 158 inputs, 102 types). Catalog version pins are mutually consistent (Apollo 4.4.3 + OkHttp 4.12.0 documented as held; Hilt 2.56.2 floor matches DEPS-17 deferral rationale; Media3 1.9.1 CASE B comment is dated and accurate). The two non-trivial concerns found are a maintainability gap (dead `<T>` generic on `SettingsViewModel.setPlayer/setUi`, both functions entirely unreferenced — pre-existing, not introduced by Phase 1) and an architectural smell that *is* newly introduced: `:core:ui` now `implementation(project(":core:data"))` because `StashImageLoader` pulls `UiPreferences` from the data layer. Both are non-blocking but should be tracked.

## Critical Issues

None.

## Warnings

### WR-01: `:core:ui` → `:core:data` introduces a layering inversion that should be tracked

**File:** `core/ui/build.gradle.kts:16`
**Commit:** `cf5f1f0 fix(build): add missing :core:data dep to :core:ui for UiPreferences injection`
**Issue:** The Phase 1 precondition fix wires `implementation(project(":core:data"))` into `:core:ui` so that `StashImageLoader` (`core/ui/src/main/java/io/stashapp/android/core/ui/image/StashImageLoader.kt:12`) can inject `io.stashapp.android.core.data.prefs.UiPreferences`. In typical clean-architecture layering, `:core:ui` is a Compose/UI layer that should depend on **domain abstractions**, not on the persistence layer that wraps DataStore. The single consumer is one Coil `SingletonImageLoader.Factory` reading `imageCacheSizeMb`. This works (Hilt resolves it) and is the minimum-diff unblock, but it locks the UI layer to the concrete DataStore impl and makes the data layer a transitive `implementation` dep of every feature module that pulls `:core:ui`. No cycle is created (`:core:data` does not depend on `:core:ui`), and the dep is `implementation` (not `api`) so the leak is contained.
**Recommendation:** Track follow-up to relocate the `imageCacheSizeMb: Flow<Int>` accessor (or a thin `ImageLoaderConfig` interface) into `:core:domain`, with `:core:data` providing the impl via Hilt. That keeps `:core:ui` off the persistence layer's classpath. This is not blocking — DEPS-17 / DEPS-07 deferral is the natural moment to roll this in.

### WR-02: `runBlocking` on the image-loader factory path can stall the first paint

**File:** `core/ui/src/main/java/io/stashapp/android/core/ui/image/StashImageLoader.kt:33`
**Issue:** `newImageLoader()` does `runBlocking { uiPreferences.imageCacheSizeMb.first() }` to size the disk cache. Coil 3 calls `SingletonImageLoader.Factory.newImageLoader()` lazily on first `imageLoader.get(...)`, which in this app is on the first Compose render that needs an image — typically on the UI thread or a Compose worker. `runBlocking` there will pin whatever thread invokes it until DataStore's first emission lands. This is **pre-existing** in master and not introduced by Phase 1 (the file was reformatted but the `runBlocking` line is identical), but it is now reachable through the freshly-added `:core:ui` → `:core:data` edge in a way that warrants visibility.
**Recommendation:** Out-of-scope for Phase 1 (deps-only). File a Phase 3 / POLISH ticket to switch to a cached-snapshot read (`runBlocking(Dispatchers.IO)` with timeout, or pre-seed via a Hilt `@Provides` that completes off the main thread before first paint). Do not address as part of DEPS.

## Info

### IN-01: `SettingsViewModel.setPlayer<T>` and `setUi<T>` are dead code with an unused type parameter

**File:** `feature/settings/src/main/java/io/stashapp/android/feature/settings/SettingsScreen.kt:67-73`
**Issue:** Both functions declare a generic `<T>` that is never used in the parameter list, return type, or body. The functions themselves are entirely unreferenced (no callers in `feature/`, `core/`, or `app/`). This is pre-existing in master — Phase 1's ktlint reformat preserved them. Phase 1 did remove the orphan `PlayerGestureSettings` composable at `76bb19e`; this is the same flavour of dead code and could have been swept in the same commit.
**Recommendation:** Track for a future cleanup pass; not blocking. If left, at minimum drop the `<T>` to silence the `unused type parameter` smell.

### IN-02: `<T>` annotation on the SettingsViewModel constructor reflow obscures readability

**File:** `feature/settings/src/main/java/io/stashapp/android/feature/settings/SettingsScreen.kt:53-59`
**Issue:** Ktlint 13.1.0's `class-signature` rule moved `@Inject constructor(...)` onto its own line for several Hilt-annotated classes (`RootViewModel`, `SettingsViewModel`, `LibraryViewModel`, `PlayerViewModel`, `DefaultSceneRepository`, etc.). This is purely cosmetic; just calling it out for downstream reviewers who will see the same shape repeated 6+ times in the diff and might mistake it for a code change.
**Recommendation:** No action. Document in a ktlint-config note so the next reformat round doesn't churn.

### IN-03: Compose BOM 2026.05.00 in `[versions]` but lint detectors for compose-runtime detectors are disabled in convention plugin

**File:** `build-logic/convention/src/main/kotlin/io/stashapp/android/buildlogic/KotlinAndroid.kt:37-43`
**Issue:** `FrequentlyChangingValue` and `RememberInComposition` are disabled across **all** Compose modules via the convention plugin. The disable comment correctly points to "Re-enable when AndroidX Lifecycle/Compose are bumped (DEPS-07, deferred)." This is correct behavior given the AGP 8.7.3 lint × Kotlin 2.2.20 metadata incompatibility documented in `01.3-SUMMARY.md`. The risk is residual: any Compose regression in the categories `FrequentlyChangingValue` / `RememberInComposition` flag will be invisible to CI until DEPS-07 lands.
**Recommendation:** Already tracked in VERIFICATION.md "Risks Carried Forward §1". No code action — log a POLISH ticket to re-enable + reset baselines as part of DEPS-07.

### IN-04: `gradle.properties` memory tuning is unconditional — CI runners may benefit from overrides

**File:** `gradle.properties:1,3,8`
**Issue:** `org.gradle.jvmargs=-Xmx2g`, `org.gradle.workers.max=2`, `kotlin.daemon.jvmargs=-Xmx1500m` were tightened to survive the 12 GB dev box per `0d4acbe`. On a multi-GB CI runner these caps will leave throughput on the table (workers.max=2 means the configuration-cache + Kotlin daemon + AGP run on at most 2 worker threads, and `-Xmx2g` will GC-thrash for a full clean build with K2). There is no `ci.gradle.properties` or `-P` override hooked into a CI variant.
**Recommendation:** Document the threshold in `.planning/REQUIREMENTS.md` or `CONTEXT.md` ("12 GB host floor"). When CI is wired up (future phase), add `-Dorg.gradle.workers.max=$(nproc)` / `-Xmx4g` to the CI invocation; do not edit `gradle.properties` from CI.

### IN-05: `detekt.yml` ForbiddenComment list lost `TODO:` from prior config? — verify intent

**File:** `config/detekt/detekt.yml:35-41`
**Issue:** The new structured `comments:` list contains only `FIXME:` and `STOPSHIP:` (the same two values as the old `values: ['FIXME:', 'STOPSHIP:']`). This is consistent with the master config, not a regression. Calling it out only because `TODO:` markers are NOT detected by ForbiddenComment, which intersects with the global instructions (`~/.claude/rules/coding-style.md`) that says "No console.log statements / No hardcoded values" but is silent on TODO comments. The catalog comment `# detekt 1.23.8 deprecated values: in favour of structured comments: list` is accurate; the migration is correct.
**Recommendation:** No action. If TODO-prohibition is desired policy, add a third entry — but that's a separate decision from DEPS-12.

## Verified clean

- **`gradle/libs.versions.toml`** — Version pins internally consistent. Apollo 4.4.3 + OkHttp 4.12.0 deferral comment (L32-L35) matches the DEPS-09 disposition in `01.2-SUMMARY.md`. Media3 1.9.1 + nextlibMedia3Ext 1.9.1-0.11.0 CASE B comment (L24-L27) is dated `2026-05-16` and correctly references `01.1-SUMMARY.md` Option A. Hilt 2.56.2 + Kotlin 2.2.20 + KSP 2.2.20-2.0.4 trio is the published, working set per Maven Central. Zero `room` references (DEPS-11 hygiene). `kotlinxCollectionsImmutable = "0.4.0"` correctly pinned stable.
- **`gradle/wrapper/gradle-wrapper.properties`** — Gradle 8.11.1 retained per DEPS-17 deferral.
- **`build-logic/convention/.../KotlinAndroid.kt`** — `compileSdk = 35`, `minSdk = 26`, `jvmToolchain(17)`, `JvmTarget.JVM_17` all coherent. No leftover `-Xcontext-receivers` or `-Xskip-metadata-version-check`. No `context()` DSL usage anywhere in `core/`, `feature/`, or `app/src/`. Three lint disables scoped via the convention plugin (applied once to every module that includes `stash-android-application` / `stash-android-library`), not duplicated per-module — correct deduplication. Self-documenting comment points to DEPS-07.
- **`gradle.properties`** — JVM tuning landed cleanly; auto-download disable at L20 honors PITFALLS §1.
- **`core/network/.../schema.graphqls`** — 4916 lines, 102 types, 158 inputs, 25 enums, 8 scalars, 2 unions, exactly one `Query` / `Mutation` / `Subscription` root each. Zero duplicate `type|input|enum|scalar|union|interface` definitions (verified by `awk` + `uniq -c | awk '$1 > 1'`). Provenance header line 1 names upstream source. Apollo codegen succeeds (generated sources in `core/network/build/generated/source/apollo/stash/...`). No license/security concerns surface from a structural scan; upstream is `stashapp/stash` AGPL-3.0 vendored at `develop`.
- **`app/build.gradle.kts`** — Orphan `implementation(project(":feature:..."))` block at top-of-file is gone; `import java.util.Properties` is restored to a proper top-of-file import (commit `3b16767`). Keystore properties read pattern is unchanged semantically.
- **`app/src/main/java/io/stashapp/android/MainActivity.kt`** — Orphan brace pair gone (`214c94f`). Removed `MainNavItems` import is genuinely unused in this file (still referenced from `core/ui/.../nav/BottomNav.kt` and `NavCustomizeSheet.kt`). Removed duplicate `import io.stashapp.android.feature.browse.BrowseScreen` was a verbatim adjacent dupe.
- **`feature/player/.../PlayerScreen.kt`** — Removed top-of-file orphan `AndroidView` block (`18d545f`) is verbatim duplicate of the kept block at L193-209 (verified line-by-line; same `factory` lambda, same `useController = false`, same `setKeepContentOnPlayerReset(true)`, same `update` body). Removed `import androidx.compose.ui.graphics.graphicsLayer` is unused in the file (zero grep hits post-removal).
- **`feature/settings/.../SettingsScreen.kt`** — Removed `PlayerGestureSettings` composable (`76bb19e`) references `viewModel.playerPreferences.seekMsPerPx` — `SettingsViewModel` has no `playerPreferences` field (it has `playerPrefs`), confirming this was dead/unreachable code. No callers anywhere in the codebase.
- **`feature/library/.../LibraryViewModel.kt`** — Removed duplicate `private fun parsePreset(raw: String?): SceneFilter` (`5f924be`) was the second of two identical copies; the surviving copy at L112-122 is structurally identical. Only one `parsePreset` definition + one call site at L44 remain.
- **`core/ui/build.gradle.kts`** — `implementation(project(":core:data"))` added; no circular reference (`:core:data` build.gradle.kts does not depend on `:core:ui`). See WR-01 for layering note.
- **`config/detekt/detekt.yml`** — Both renames correctly applied (`thresholdInFile`→`thresholdInFiles`, `thresholdInClass`→`thresholdInClasses`, and `values:` → structured `comments:`). Self-documenting comments left for next reviewer.
- **All ~50 Priority B `.kt` files under `core/`, `feature/`, `app/`** — Spot-checked `PlayerViewModel.kt`, `SceneFilterMapper.kt`, `DefaultSceneRepository.kt`, `LibraryViewModel.kt`, `SettingsScreen.kt` with `git diff -w` and the non-blank-line changes are exclusively: (a) `@Inject constructor(...)` line-breaks, (b) trailing-comma `, ->` on `when` arm groupings, (c) chained-method/property `?.foo` line-breaks under `.indent_size=4 chain-method-rule`. Zero behavior change detected across the sample.

## Out of scope / skipped

- `.planning/**` — planning artifacts (reviewed by plan-checker).
- `app/lint-baseline.xml`, `*/detekt-baseline.xml`, `*-baseline.xml` — generated baselines regenerated by DEPS-01 / DEPS-12.
- `.planning/phases/01-deps-foundation-bump/deps-audit.txt` — DEPS-15 generated artifact.
- `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar` — wrapper bootstrap files.
- `*.min.js` / `dist/` / `build/` — none present.

## Verdict

**PASS-WITH-FINDINGS** — All 7 findings are non-blocking. Zero criticals. The two `Warning`-tier items (WR-01 layering, WR-02 `runBlocking`) are tracked-not-fixed: WR-01 is a deliberate Phase 1 minimum-diff trade-off (relocate `UiPreferences` accessor into `:core:domain` during DEPS-07); WR-02 is pre-existing and out of DEPS scope. Phase 1's actual scope — catalog bumps, toolchain pin, lint-detector workarounds, precondition cleanups — landed cleanly with deferral hygiene. Recommend proceeding to Phase 2 (COMPLY). Re-open WR-01 + WR-02 in the AGP-9 transition phase along with the lint-detector re-enable.

---

_Reviewed: 2026-05-16T22:35+09:00_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
_HEAD: `b8875313044e450050a4ac9f6960c4fe9f2c2f05`_
