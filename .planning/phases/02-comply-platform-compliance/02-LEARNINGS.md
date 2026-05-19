---
phase: 2
phase_name: "COMPLY — Platform Compliance"
project: "slopper"
generated: "2026-05-18"
counts:
  decisions: 8
  lessons: 9
  patterns: 6
  surprises: 7
missing_artifacts: []
---

# Phase 2 Learnings: COMPLY — Platform Compliance

## Decisions

### PredictiveBackHandler Chosen Over NavigationBackHandler
Use `PredictiveBackHandler` (androidx.activity) rather than the newer `NavigationBackHandler` API for all predictive-back migration on the current floor (Activity Compose 1.9.3).

**Rationale:** `NavigationBackHandler` requires Activity Compose ≥ 1.10. The Phase 2 floor is 1.9.3 (DEPS-17 deferred AGP-9 bump). Using the only available API avoids a floor constraint violation. Accepted risk `COMPLY-02-NAV-EVENT` tracks the eventual migration with revisit trigger: Activity Compose ≥ 1.10.
**Source:** 02-CONTEXT.md Decision 3; 02.1-PLAN.md; 02-REVIEWS.md

---

### Splash Gate — Pattern A Case B Path (Reuse Existing collectAsState)
When `MainActivity` already has a `collectAsState` binding for `rootViewModel.start` (Case B), thread the `appReady` AtomicBoolean into the existing `StashAppContent` composable as a parameter instead of creating a second independent binding.

**Rationale:** The SPEC mandates EXACTLY 1 `collectAsState` for `start` to prevent double-subscription bugs. The Step 0 dedup grep found 1 pre-existing binding at `StashAppContent:174`. Creating a second binding in `setContent { }` would have broken the dedup invariant. Threading `appReady` as a parameter is functionally equivalent and preserves the invariant.
**Source:** 02.1-SUMMARY.md §Step 0 Dedup; 02.1-DEVIATIONS.md DEV-01; VERIFICATION.md COMPLY-04 row

---

### safeDrawingPadding Wraps Only UI Chrome, Not Gesture-Detection Layers
Wrap only `PlayerControls` and the locked-overlay in `Box(Modifier.safeDrawingPadding())`; leave `pointerInput` gesture-detection layers and `StepSeekCallout` indicators at full-bleed.

**Rationale:** Gesture detection layers (tap-toggle, horizontal scrub) must capture input across the full screen including regions near system bars. Wrapping them inside `safeDrawingPadding` would shrink the touch target and break the scrub/tap-toggle UX near the edges. SPEC must_have ("SurfaceView stays edge-to-edge") is satisfied; the chrome-only wrapping is consistent with the OpenCode reviewer LOW guidance.
**Source:** 02.1-DEVIATIONS.md DEV-02; VERIFICATION.md COMPLY-01 row; 02-REVIEWS.md

---

### R-Class Co-Location in Feature Module (Module-Graph Direction)
When a feature module needs string resources for a new composable, co-locate the strings in the feature module's `src/main/res/values/strings.xml` rather than in `app/strings.xml`.

**Rationale:** The Gradle dependency direction is `app → feature/settings`, never the reverse. `feature/settings` cannot resolve `io.stashapp.android.R` because the app module's R class is not on the feature's compile classpath. Placing strings in the feature module is the correct structural fix; the build auto-resolves the same-package R class with no explicit import. Duplicate copies are kept in `app/strings.xml` per CONTEXT.md lock — AGP merges resources and deduplicates identical values.
**Source:** 02.2-DEVIATIONS.md #1; VERIFICATION.md COMPLY-06 row

---

### Verbal-Verdict UAT Accepted as Evidence Under REVIEWS-C4 Hygiene
When a human reviewer delivers a verbal "all pass" at a `checkpoint:human-verify` gate and explicitly elects to skip the screenshot pack, the verbal verdict is accepted as sufficient functional evidence provided: (a) the election is explicit and recorded verbatim, (b) an accepted-risk block with a re-open trigger is committed, and (c) the SPEC unmet criterion is declared visibly.

**Rationale:** The reviewer who would have signed the PNG pack was the same reviewer who inspected every surface live and returned "all pass". Re-shooting the pack at Phase 5 (Spine redesign) is more valuable since Phase 5 produces fresh baselines anyway. The accepted-risk + re-open trigger pattern satisfies the REVIEWS-C4 ACCEPT requirement.
**Source:** 02.2-DEVIATIONS.md DEV-05; 02-UAT.md §Accepted Risks; VERIFICATION.md §overrides

---

### Wave-2 Sequencing for Plans Sharing Manifest or Build Files
When two plans in the same phase both modify `AndroidManifest.xml` or `app/build.gradle.kts`, the second plan must run as wave 2 (not wave 1 parallel) and declare `depends_on` the first plan.

**Rationale:** Plans 02.1 and 02.2 both modified `AndroidManifest.xml`. Running them in parallel (both wave 1) creates a merge conflict risk and means 02.2's UAT would run before 02.1's UI changes are present. The cross-AI review (3/3 HIGH convergence) caught this before execution. The REVIEWS `--reviews` replan flag corrected both wave fields and added the explicit dependency.
**Source:** 02-REVIEWS.md §convergent_findings; SPEC-LAYER.md step 4 notes

---

### WindowInsets Defaults Are Acceptable — No Need for Explicit References at Every Scaffold
Material3 `Scaffold`'s default `contentWindowInsets = ScaffoldDefaults.contentWindowInsets` is equivalent to `WindowInsets.systemBars` — it is correct edge-to-edge behavior without an explicit override.

**Rationale:** The SPEC originally said "every Scaffold explicitly references WindowInsets". At verification time, all 4 top-level Scaffolds used the Material3 default, which is correct. Forcing explicit references would be redundant and noisy. CONTEXT.md Decision 4 was amended during discuss-phase to accept the default as equivalent. Recorded here so future phases don't re-audit Scaffold insets unnecessarily.
**Source:** VERIFICATION.md §Scaffold WindowInsets audit; 02-CONTEXT.md Decision 4

---

### Orphan Permission Removal as Separate Atomic Commits (Not Batch)
COMPLY-03 (POST_NOTIFICATIONS) and COMPLY-05 (FOREGROUND_SERVICE_MEDIA_PLAYBACK) were landed as two independent commits despite being one-line manifest deletions each.

**Rationale:** CONTEXT.md Decision 7 mandates each removal be independently bisect-meaningful. A single batch commit obscures which removal introduced a regression if post-permission-removal tests fail. The two commits (`481c303` + `1aff210`) preserve the invariant.
**Source:** 02-CONTEXT.md Decision 7; 02.2-SUMMARY.md Task 1

---

## Lessons

### AGP 8.7.3 `resources.properties` Is a Hard Error, Not a Warning
When `generateLocaleConfig = true` is added to `androidResources {}`, AGP 8.7.3 aborts the build with "`No resources.properties file found`" if `app/src/main/res/resources.properties` does not exist. The Android docs describe this as a warning; AGP 8.7.3 treats it as a build-blocking error.

**Context:** Plan 02.2 anticipated the warning and had the remediation (`unqualifiedResLocale=en`) in Step 4. In practice, the first `assembleDebug` after enabling the flag failed outright rather than emitting a diagnostic. The fix was identical but the expectation ("warning on first build, then fix") was wrong. Any project on AGP 8.7.x must create `resources.properties` atomically with the `generateLocaleConfig` line.
**Source:** 02.2-DEVIATIONS.md #2; 02.2-SUMMARY.md Task 2

---

### Feature Modules Cannot Import the App Module's R Class
A feature module (e.g., `:feature:settings`) that is depended on by `:app` cannot import `io.stashapp.android.R`. The Gradle dependency direction is `app → feature`, not the reverse. Attempting to import the app R class produces `Unresolved reference 'R'` at compile time.

**Context:** The original plan suggested placing strings in `app/strings.xml` and referencing them via `R.string.*` from the feature composable. This fails structurally. The correct pattern is to co-locate strings in the feature module's resources and let same-package R resolution handle them implicitly (no import needed).
**Source:** 02.2-DEVIATIONS.md #1

---

### Cross-AI Review Catches Wave-Sequencing Bugs That Individual Planning Misses
All three cross-AI reviewers (Gemini, Codex, OpenCode) independently flagged the wave-1 parallelism conflict as HIGH risk — the only time all three converged on a finding at the same severity level.

**Context:** The planner marked both plans as `wave: 1` (parallel-safe), but they both targeted `AndroidManifest.xml` and `app/build.gradle.kts`. A single human planner focused on per-plan task scope is unlikely to catch this cross-plan conflict. The `--reviews` replan flag exists exactly for this recovery path. Running cross-AI review for any phase with ≥2 plans targeting overlapping files is strongly recommended.
**Source:** 02-REVIEWS.md §convergent_findings; SPEC-LAYER.md step 4

---

### `installSplashScreen()` Must Be the First Statement Before `super.onCreate()`
The splash screen library requires `installSplashScreen()` to be called before `super.onCreate(savedInstanceState)`, not merely before `setContent {}`. Calling it after `super.onCreate()` causes the splash to be skipped silently on some devices.

**Context:** This ordering constraint is easy to misremember. The RESEARCH document (§A4) explicitly called it out, and the SPEC must_have included a grep check `installSplashScreen BEFORE super.onCreate`. Verified by line numbers in VERIFICATION.md: line 99 < line 102.
**Source:** 02.1-SUMMARY.md §COMPLY-04 verify block; VERIFICATION.md COMPLY-04 row; RESEARCH §A4

---

### MoreSheet Lives at BottomNav.kt, Not a Standalone File
The third `ModalBottomSheet` call site (`MoreSheet`) is a private composable inside `BottomNav.kt` at line 179, not a separate `MoreSheet.kt` file. The SPEC-phase scout initially listed only two `ModalBottomSheet` sites.

**Context:** Automated SPEC-phase research missed this because it searched for files with "Sheet" in the name rather than for `ModalBottomSheet(` call sites in the codebase. Any future audit of sheet-level insets must use `grep -rn 'ModalBottomSheet(' --include="*.kt"` rather than relying on file inventory. This lesson was caught mid-plan-phase (commit `26b0595` patched SPEC + CONTEXT).
**Source:** 02.1-SUMMARY.md §COMPLY-01 verify block; 02-SPEC.md §corrections commit 26b0595

---

### `PredictiveBackHandler` Cancel Semantics Require `try { } catch (CancellationException) { throw e }`
The `PredictiveBackHandler` lambda uses a coroutine-based `Flow<BackEventCompat>` for `progress`. Cancelling the back gesture (user pulls back and releases) cancels the coroutine. If `CancellationException` is not rethrown, the coroutine framework's structured concurrency breaks — subsequent back gestures may not fire, and `onExit()` may fire spuriously on cancelled gestures.

**Context:** The RESEARCH document (§A2) documented this explicitly. The pattern is: `try { progress.collect { … }; onExit() } catch (e: CancellationException) { throw e }`. Both RESEARCH and CONTEXT locked this as a must-have invariant; it was verified at COMPLY-02 grep checks.
**Source:** 02.1-SUMMARY.md §COMPLY-02 verify block; RESEARCH §A2; 02-CONTEXT.md D-03

---

### Pre-Removal Safety Grep Is Essential for Orphan Permissions
Before removing any permission declaration from `AndroidManifest.xml`, run a grep across `feature/`, `app/`, and `core/` to confirm zero call sites for the APIs gated by that permission. POST_NOTIFICATIONS gates `NotificationManager.notify()` / `NotificationCompat` / `.notify(`; removing the permission while call sites exist produces a runtime denial-of-service (silent notification failure on Android 13+).

**Context:** RESEARCH §A7 prescribed the pre-removal grep. The grep returned 0 for POST_NOTIFICATIONS at COMPLY-03 time — the app had no notification call sites, confirming the permission was truly orphaned. This pattern should be applied to any permission removal in future phases.
**Source:** 02.2-SUMMARY.md Task 1; RESEARCH §A7

---

### ANR Safety-Timeout Is Required Alongside the Splash Keep-Condition
`setKeepOnScreenCondition { !appReady.get() }` alone can hold the splash indefinitely if `RootViewModel.start` never emits (e.g., network failure before GraphQL handshake). A `LaunchedEffect(Unit) { delay(3000); appReady.set(true) }` provides a 3-second ANR insurance timeout.

**Context:** This is an "intentional SPEC deviation per RESEARCH §A4/E3" — the SPEC's strict acceptance criterion only required the gate flip on `start != null`, but the RESEARCH document explicitly called out the ANR risk. The safety timeout was added and documented in the commit body. The 3-second value matches the Android system's watchdog window for startup transitions.
**Source:** 02.1-SUMMARY.md §COMPLY-04; RESEARCH §A4, §E3

---

### Verbal Verdict UAT Requires Four Explicit Artifacts, Not Just a Sign-Off Comment
A verbal-verdict UAT path (no screenshots) is acceptable under the REVIEWS-C4 ACCEPT convention only if four artifacts are produced: (1) UAT table with per-row results and evidence column, (2) Accepted Risks block with re-open trigger, (3) Reviewer Sign-Off block with verbatim statement + device + build SHA, (4) SCREENSHOTS.md stub naming the pack-not-produced files.

**Context:** Generating all four artifacts is more work than the PNG pack itself. The value is auditability: a future reviewer can reconstruct what was tested, who tested it, on which build, and what the re-open conditions are. Skipping any of the four artifacts weakens the evidence chain under future scrutiny.
**Source:** 02.2-DEVIATIONS.md DEV-05; 02-UAT.md; screenshots/SCREENSHOTS.md

---

## Patterns

### Pattern A Splash Dedup — Step 0 Grep Before Writing Gate Code
Before writing any splash keep-condition code, run `grep -c 'rootViewModel.start.collectAsState' MainActivity.kt` to discover whether a binding already exists.
- Result **0** → Case A: add binding in `setContent { }`.
- Result **≥1** → Case B: thread `appReady` into the composable that owns the existing binding as a parameter.

**When to use:** Any time a `setKeepOnScreenCondition` gate needs to observe a ViewModel StateFlow that may already be collected elsewhere in the Activity. Prevents double-subscription bugs and satisfies the EXACTLY-1-collectAsState invariant.
**Source:** 02.1-SUMMARY.md §Step 0 Dedup; 02.1-PLAN.md Task 1 Step 0; RESEARCH §A4

---

### AGP `generateLocaleConfig` Setup Checklist (3 steps, atomic)
Enable per-app language support with exactly these three simultaneous changes — doing them in separate commits causes intermediate build failures:
1. `app/build.gradle.kts`: add `androidResources { generateLocaleConfig = true }` inside the `android {}` block.
2. `app/src/main/res/resources.properties`: create with `unqualifiedResLocale=en` (AGP 8.7.3 hard error without this).
3. `feature/settings/.../SettingsScreen.kt`: add `LanguageRow()` composable with `Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU` guard + `Intent(Settings.ACTION_APP_LOCALE_SETTINGS)` fire.

**When to use:** Any project adding per-app language support on AGP 8.7.x.
**Source:** 02.2-SUMMARY.md Task 2; 02.2-DEVIATIONS.md #2

---

### R-Class Location Decision Tree for Feature Module Strings
Before placing new strings in `app/strings.xml` for use in a feature module, check the module dependency direction:
- If `app → feature` (feature is a library consumed by app): strings must go in the feature module's `src/main/res/values/strings.xml`. The app-level R class is not visible to the feature.
- If `feature → core` (feature depends on a core module): same rule — place strings in the declaring module.
- When both app and feature need the same strings: co-locate in the feature module AND keep a copy in app (AGP deduplicates at merge time).

**When to use:** Any time a planner or implement step calls for "add strings to `app/res/values/strings.xml` and reference from `feature/X`". Check the module graph first.
**Source:** 02.2-DEVIATIONS.md #1; VERIFICATION.md COMPLY-06 row

---

### REVIEWS-C4 ACCEPT Pattern for Hardware-Blocked Verification
When a verification step requires hardware that is unavailable (e.g., 3-button-nav OEM device), accept the gap via:
1. `02-CONTEXT.md` `## Accepted Risks` section: entry with `id`, `description`, and `revisit_trigger`.
2. `REQUIREMENTS.md` Deferred section: backlog item with explicit revisit trigger.
3. UAT table: `DEFERRED` rows citing the accepted risk id.
4. SPEC-LAYER.md: accepted risks carried into the phase state block.

**When to use:** Any phase step that cannot be completed due to environmental constraints (hardware, access, CI infra). The pattern provides an auditable trail without blocking phase progress.
**Source:** 02-UAT.md §REVIEWS-C4 Sign-Off; 02-CONTEXT.md §Accepted Risks; 02-REVIEWS.md §nice_to_have_deferred

---

### Cross-Plan File Conflict Detection for Wave Scheduling
Before assigning wave numbers to parallel plans, run:
```
grep -h 'Files likely touched:' PLAN1.md PLAN2.md | sort | uniq -d
```
Any shared file is a wave conflict. Plans sharing `AndroidManifest.xml`, `app/build.gradle.kts`, or any `build.gradle.kts` must be wave-sequenced (wave N, wave N+1) rather than run in parallel.

**When to use:** At plan-phase time before finalizing wave assignments for any phase with ≥2 plans. Takes 30 seconds; the cross-AI review costs 20 minutes.
**Source:** 02-REVIEWS.md §convergent_findings HIGH finding; SPEC-LAYER.md step 4 notes

---

### Accepted-Risk Block Schema for Spec-Bullet Overrides
When a SPEC acceptance criterion is unmet by deliberate choice, produce these four elements:
```
1. VERIFICATION.md `overrides:` frontmatter block (must_have + reason + accepted_by + accepted_at)
2. UAT.md `## Accepted Risks` section (id + description + re-open trigger + responsible party)
3. SCREENSHOTS.md or equivalent stub (chain-of-custody record)
4. DEVIATIONS.md entry classifying the deviation as human-election (not Rule 1-3 auto-fix)
```

**When to use:** Any time a human reviewer at a `checkpoint:human-verify` gate elects to skip a deliverable that is explicitly named in a SPEC acceptance criterion.
**Source:** 02.2-DEVIATIONS.md DEV-05; VERIFICATION.md §overrides; 02-UAT.md §Accepted Risks

---

## Surprises

### AGP-Generated Locale Config Uses Underscore-Prefix Naming
AGP 8.7.3 generates `_generated_res_locale_config.xml` (with a leading underscore), not `locale_config.xml`. The merged manifest references it as `android:localeConfig="@xml/_generated_res_locale_config"`.

**Impact:** Any `find` or `grep` verification step that looks for `locale_config.xml` by exact name returns empty. Correct pattern: `find -name '*locale_config*'`. This naming is an AGP internal convention and does not affect runtime behavior — the system reads the merged manifest attribute, not the raw filename.
**Source:** 02.2-DEVIATIONS.md #3; VERIFICATION.md COMPLY-06 row

---

### MoreSheet Was Not a Standalone File — Spec-Phase Scout Error
The spec-phase researcher listed only 2 `ModalBottomSheet` sites (FilterSheet, NavCustomizeSheet) when there are actually 3. The third — `MoreSheet` — is a private composable inside `BottomNav.kt:179`, not a file with "Sheet" in its name.

**Impact:** If uncaught, COMPLY-01 would have missed the third `contentWindowInsets` fix and the bottom sheet would have clipped content behind the navigation bar. Caught mid-plan-phase via the plan-phase researcher's own `grep -rn 'ModalBottomSheet('` scan (commit `26b0595`). Required patching SPEC.md and CONTEXT.md before planning could proceed.
**Source:** 02-SPEC.md §correction commit 26b0595; 02.1-SUMMARY.md §COMPLY-01 verify block

---

### Cross-AI Wave-Sequencing Bug — 3/3 Convergent HIGH Finding Before a Single Line of Code Was Written
Three independent AI reviewers converged on the same HIGH-severity finding (wave-1 parallelism conflict on AndroidManifest.xml) before any execution had started. This is the workflow working as intended — a failure mode that would have caused a mid-execution manifest conflict was caught and replanned in 20 minutes.

**Impact:** If the HIGH had not been caught, Plan 02.1 and Plan 02.2 would have both tried to edit `AndroidManifest.xml` simultaneously, likely producing a merge conflict and a failed CI build. The replan cost was low (two file edits + commit); the recovery from a mid-execution conflict would have been much higher.
**Source:** 02-REVIEWS.md §convergent_findings; SPEC-LAYER.md step 4 `unblock_history`

---

### R8 Kotlin-Metadata Warnings Were Pre-Existing, Not Introduced by Phase 2
The `assembleRelease` build after Plan 02.1 emitted R8 kotlin-metadata warnings. Investigation confirmed these were pre-existing warnings from the Phase 1 toolchain state, not caused by any Phase 2 change.

**Impact:** Wasted a diagnostic detour during Plan 02.1 execution. Future phases should baseline R8 warnings at the start of a phase (store the warning count from a clean build) so any delta attributable to new code is immediately visible.
**Source:** 02.1-SUMMARY.md §Build Results final row note

---

### The 8-Commit Budget Hit Exactly — Commit Budgets Work as Planning Constraints
CONTEXT.md Decision 2 set a budget of "8 atomic COMPLY commits" for Phase 2. The actual execution landed exactly 8 (3 from Plan 02.1 + 5 from Plan 02.2). The budget forced plan authors to separate code-class commits from docs/bookkeeping commits, which also improved bisect cleanliness.

**Impact:** Counting only meaningful code/manifest/doc commits (not auxiliary bookkeeping) against the budget is the correct accounting. The 3 auxiliary commits (`73ef77e`, `06b5571`, `a75cca0`) did not count against the budget. This distinction should be standard in future phase CONTEXT.md decisions.
**Source:** 02.2-SUMMARY.md §Cumulative Phase 2 commits; SPEC-LAYER.md step 5 notes

---

### Splash Theme Must Be Declared on the Activity, Not the Application
`android:theme="@style/Theme.Stash.Splash"` must be set on the `<activity>` element in AndroidManifest.xml, not on the `<application>` element. Setting it on `<application>` causes the splash theme to apply to all activities, which is incorrect for apps with secondary activities (e.g., share targets).

**Impact:** Low for this app (single-activity), but architecturally important. Recorded as a constraint for future multi-activity phases or when a share-sheet activity is added in a future milestone.
**Source:** 02.1-SUMMARY.md §COMPLY-04 verify block; RESEARCH §A4

---

### `PredictiveBackHandler` Has No `enabled =` Parameter Unlike `BackHandler`
The standard `BackHandler` composable accepts an `enabled` boolean parameter to conditionally activate/deactivate the handler. `PredictiveBackHandler` has no such parameter — it is always active when composed.

**Impact:** Any logic that previously used `BackHandler(enabled = someCondition) { ... }` must migrate the condition to remove the handler from the composition tree entirely (via `if (someCondition) { PredictiveBackHandler { ... } }`) rather than toggling `enabled`. This is a subtle API difference that can produce always-active back capture if the migration is done naively (just replacing `BackHandler` with `PredictiveBackHandler` while keeping the `enabled` parameter).
**Source:** 02.1-SUMMARY.md §COMPLY-02 verify block ("`enabled =` param on PredictiveBackHandler: 0 ✓"); RESEARCH §A2 PITFALLS §8
