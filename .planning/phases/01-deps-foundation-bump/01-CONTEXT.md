# Phase 1 Context: DEPS (Foundation Bump)

**Created:** 2026-05-16
**Phase dir:** `.planning/phases/01-deps-foundation-bump/`

## Domain

Toolchain & dependency-floor migration: move Slopper from `AGP 8.7.3 / Gradle 8.11.1 / Kotlin 2.1.0 / Compose BOM 2024.12.01 / compileSdk 35` to `AGP 9.2.0 / Gradle 9.4.1 / Kotlin 2.2.20 / Compose BOM 2026.05.00 / compileSdk 36` with the build green at every commit and the baseline profile regenerated against the new toolchain. Pure infrastructure — no behavior changes beyond the minimum required to keep things compiling.

## Spec Lock

**SPEC.md:** `.planning/phases/01-deps-foundation-bump/01-SPEC.md` — 16 requirements LOCKED. Downstream agents MUST read this before planning.

- Goal: locked (target versions + compileSdk 36 + green build + regenerated baseline profile)
- Boundaries: locked (no minSdk bump, no module-graph change, no feature work, no MediaSessionService introduction, no Apollo 5 / Nav3 / EncryptedPrefs migration)
- Constraints: locked (stable-only, minSdk 26, JDK 17, lockstep order rules)
- Acceptance criteria: locked (per-requirement command-checkable acceptance)

This CONTEXT.md adds only the *implementation choices* — how to land what SPEC.md already specifies.

## Canonical Refs

Downstream agents (researcher, planner, executor) MUST consult these:

- `.planning/phases/01-deps-foundation-bump/01-SPEC.md` — **locked requirements (MUST read before planning)**
- `.planning/PROJECT.md` — milestone constraints, core value, locked decisions
- `.planning/REQUIREMENTS.md` — DEPS-01..14 + out-of-scope list
- `.planning/ROADMAP.md` — Phase 1 verification gate
- `.planning/research/STACK.md` — exhaustive target-version table + migration plan
- `.planning/research/PITFALLS.md` — 18 pitfalls with phase mapping; §§1, 2, 3, 4, 6, 9, 10, 11, 12, 15, 18 apply to Phase 1
- `.planning/research/SUMMARY.md` — executive synthesis + cross-cutting risks
- `.planning/codebase/STACK.md` — current-in-repo versions (source of truth for "where we're coming from")
- `.planning/codebase/CONCERNS.md` — restoration gaps (lint-baseline missing, etc.)
- `gradle/libs.versions.toml` — version catalog to edit
- `build-logic/convention/src/main/kotlin/io/stashapp/android/buildlogic/` — convention plugins (compileSdk / targetSdk live here)
- `app/proguard-rules.pro` — R8 keep rules to audit under AGP 9 + serialization 1.9
- Upstream release notes (consult during execution, not now):
  - https://developer.android.com/build/releases/agp-9-2-0-release-notes
  - https://developer.android.com/develop/ui/compose/bom/bom-mapping
  - https://kotlinlang.org/docs/compose-compiler-migration-guide.html
  - https://docs.gradle.org/current/userguide/upgrading_major_version_9.html
  - https://android-developers.googleblog.com/2025/11/configure-and-troubleshoot-r8-keep-rules.html

## Code Context

Scouted before this discussion:

- `gradle.properties` already has `org.gradle.configuration-cache=true`, `org.gradle.parallel=true`, `org.gradle.caching=true`, `ksp.incremental=true` — the build infrastructure baseline is in place; the Gradle 9 + AGP 9 bump must keep these green, not introduce them.
- `gradle.properties` also has `android.useAndroidX=true`, `android.nonTransitiveRClass=true`, and three `android.defaults.buildfeatures.*=false` toggles (buildconfig, resvalues, shaders) — preserve as-is unless AGP 9 deprecates a key.
- `build-logic/convention/src/main/kotlin/io/stashapp/android/buildlogic/KotlinAndroid.kt` holds `compileSdk = 35` and `minSdk = 26`. SDK bumps land here (compileSdk only — minSdk is frozen).
- `targetSdk = 35` is set in TWO places: `AndroidApplicationConventionPlugin.kt` AND `AndroidLibraryConventionPlugin.kt`. Both must be bumped to 36 in the same commit; consider extracting a shared constant (Claude's discretion — only if it doesn't sprawl).
- `app/proguard-rules.pro` exists with keep rules for: Apollo generated types, Hilt aggregated roots + HiltViewModel, Media3 + nextlib (reflection-loaded), kotlinx.serialization (`@Serializable Companion + $$serializer`), Compose runtime, coroutines volatile fields. The R8 audit (DEPS-04) extends this file, doesn't replace it.
- Room (`room = "2.6.1"` and its library entries) is in `gradle/libs.versions.toml` but `grep -rE 'androidx\.room' app core feature` returns zero hits — confirmed dead; safe to remove in DEPS-11.
- `app/lint-baseline.xml` does not exist — DEPS-01 generates it. It must land *before* any version bump.
- Apollo at 4.1.0 → 4.4.3 is a minor bump within the same major; no breaking changes expected for the existing schema/queries under `io.stashapp.android.graphql.**`.

## Decisions

### 1. Commit / PR shape — linear branch with atomic per-requirement commits

Single branch: `phase-1/deps-bump`. One commit per SPEC.md requirement (`DEPS-01` through `DEPS-16`). Commits follow `chore(deps): <req-id> — <short description>` format (e.g. `chore(deps): DEPS-04 — bump AGP to 9.2.0 and compileSdk/targetSdk to 36`). One PR opened at phase end against `master` once all 16 commits land and the green-build acceptance is reproducible.

Rationale:
- Atomic per-requirement commits preserve `git bisect`-ability — if a later phase regresses, we can pin the breakage to a specific bump.
- The lockstep order in SPEC.md (Requirement #1 → #16) translates directly to commit order — the diff is the audit trail.
- One PR at end is cleaner than stacked PRs for solo work; review can scroll commit-by-commit.

**Locked for planner:** plans must produce per-requirement atomic commits; the executor must not collapse multiple requirements into one commit unless explicitly justified.

### 2. Media3 1.10 — HTTP-probe Maven Central before touching the catalog

The plan step that handles DEPS-10 begins with:

```bash
curl -sf https://repo1.maven.org/maven2/io/github/anilbeesetti/nextlib-media3ext/maven-metadata.xml \
  | grep -oE '<version>1\.10[^<]*</version>' | head -3
```

- Hits → bump both `media3 = "1.10.0"` and `nextlibMedia3Ext = "1.10.0-<matching>"`; record the resolved version pair in the plan execution log.
- Empty → leave `media3 = "1.9.1"`, add a comment block to `gradle/libs.versions.toml` reading approximately:
  ```toml
  # Media3 stays on 1.9.1 — nextlib-media3ext has no 1.10.x build on Maven Central
  # as of 2026-MM-DD. Revisit next milestone (see .planning/REQUIREMENTS.md "Deferred").
  ```

The probe runs once at plan-execution time; the date written in the comment is the date of the probe.

**Locked for planner:** the Media3 plan step has two branches; both must be specified.

### 3. Failure-recovery — revert the offending bump, continue the rest of the phase, log it

If a single requirement's bump breaks the build (compile error, runtime crash on smoke launch, test failure that's not flaky):

1. Revert the offending commit on `phase-1/deps-bump`.
2. Document the failure in the phase plan log: which version was attempted, what failed (compile output / stack trace excerpt), what upstream issue (link if known).
3. Open a new requirement in `.planning/REQUIREMENTS.md` under "Deferred to Future Milestones" naming the failed bump.
4. Continue with the next requirement.

Exception — the lockstep chain in SPEC.md Constraints (lint-baseline → JDK → Gradle → AGP/SDK → Kotlin/KSP/Compose-compiler → Compose BOM): if any one of these breaks, the phase STOPS and the failure is investigated, because every later requirement assumes the chain holds. Failures in DEPS-07 onward (AndroidX sweep, Hilt, Apollo, Media3, catalog hygiene, quality gates, dependencyCheck) follow the revert-and-continue rule.

**Locked for planner:** plans must distinguish "lockstep" requirements (stop-the-phase on failure) from "independent" ones (revert-and-continue). Plan log template must include a "Failures & Reverts" section.

## Claude's Discretion

These were not user-decided; downstream agents handle them per the guidance below.

- **R8 keep-rule audit approach:** incremental. Keep existing rules in `app/proguard-rules.pro`. For each affected library group (Apollo 4.4.3, Hilt 2.57.1, kotlinx.serialization 1.9.0, Media3 1.10 if bumped, Compose BOM 2026.05.00), consult upstream release notes for net new keep rules and append with an inline comment citing the source. Smoke-test the release APK launches and reaches the first DI-injected screen.
- **Detekt / ktlint baseline regeneration policy:** re-baseline ktlint format-only churn (expected after the Kotlin 2.2 bump); fail-fast on any net new *detekt* findings — those represent real semantic changes worth a human look. Document any detekt finding that gets baselined with a reason.
- **`dependencyCheck` plugin treatment:** the plan step greps for `dependency-check` / `dependencyCheckAnalyze` in `build-logic/` and all `build.gradle.kts` files. If absent → remove from catalog. If present and the plugin still loads on AGP 9 → leave as-is. If present and broken → revert-and-continue per Decision 3, add to deferred backlog.
- **Verification cadence:** `./gradlew --configuration-cache assembleDebug` runs after every commit (the per-requirement gate). `./gradlew --configuration-cache assembleDebug assembleRelease assembleBenchmark check` runs at step-group boundaries (after DEPS-04 SDK bump, after DEPS-06 Compose BOM, after DEPS-13 last lib bump) and once more before the phase-closing baseline profile regen (DEPS-16).
- **`targetSdk` constant:** if the planner decides to extract `targetSdk = 36` into a shared constant in `KotlinAndroid.kt` (currently it's hardcoded in two places), it lands as part of DEPS-04 in the same commit — not a separate refactor commit.

## Deferred Ideas

Captured during discussion; not in this phase.

- (none surfaced this round — scope discipline held)

## Out of Scope (Reminder)

From SPEC.md and PROJECT.md, do NOT do in Phase 1:

- `minSdk` bump
- Edge-to-edge / predictive back / Splash API work (Phase 2)
- GMD wiring (Phase 3)
- Shuffle-playback bug fix (Phase 3, PERF-08)
- `PlayerScreen.kt` split (Phase 4)
- New test infrastructure (Phase 4)
- Apollo 5 / Navigation 3 / EncryptedSharedPreferences replacement (deferred milestones)
- Any source-code behavior change beyond what codegen / deprecation removal forces

## Next Steps

`/gsd-plan-phase 1` to create executable plans for DEPS — research-then-plan with the locked SPEC + this CONTEXT as input.

---

*Phase: `01-deps-foundation-bump`*
*Context created: 2026-05-16*
