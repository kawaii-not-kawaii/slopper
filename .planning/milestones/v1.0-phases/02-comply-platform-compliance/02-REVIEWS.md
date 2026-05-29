---
phase: 2
reviewers: [gemini, codex, opencode]
reviewed_at: 2026-05-17T09:50:00+09:00
plans_reviewed: [02.1-PLAN.md, 02.2-PLAN.md]
self_skipped: claude (running inside Claude Code)
unavailable: [qwen, cursor, coderabbit, lm_studio]
opencode_model: opencode-go/glm-5.1
---

# Cross-AI Plan Review — Phase 2 (COMPLY)

3 external AI reviewers (Gemini, Codex, OpenCode via GLM-5.1) independently reviewed `02.1-PLAN.md` + `02.2-PLAN.md` against the full SPEC + CONTEXT + RESEARCH stack. Strong convergence on one HIGH-severity finding: **wave-1 parallelism is wrong**.

---

## Gemini Review

## Summary
The plans are thorough, highly detailed, and accurately translate the SPEC and RESEARCH constraints into actionable, verifiable tasks. They rigorously adhere to the locked toolchain floor (AGP 8.7.3, `core-splashscreen` 1.0.1, `PredictiveBackHandler`) while strictly enforcing the anti-coupling rule against premature Phase 5 Spine styling. However, a significant execution risk exists due to file modification overlap across parallel plans.

## Strengths
- **Implementation Accuracy**: The `PredictiveBackHandler` migration flawlessly implements the `CancellationException` re-throw pattern, preventing corrupted back-gesture state.
- **Splash Gate Strategy**: Pattern A (keyed on the collected flow value instead of the StateFlow object) safely circumvents strong-skipping memoization bugs.
- **Edge Case Coverage**: Correctly identifies and includes all three `ModalBottomSheet` call sites (including the easily-missed `MoreSheet` in `BottomNav.kt`) for explicit `contentWindowInsets` adjustment, preventing the 3-button-nav issue (Pitfall E1).
- **Verification Rigor**: The bash-based automated verification blocks are robust and effectively falsify each acceptance criterion in an isolated manner.

## Concerns
- **HIGH — Concurrent Modification Collisions**: Both Plan 2.1 and Plan 2.2 are marked as `wave: 1` (parallel execution), yet both modify `app/src/main/AndroidManifest.xml` and `app/build.gradle.kts`. If automated agents execute these concurrently, it will almost certainly cause file corruption, git locks, or merge conflicts.
- **HIGH — UAT Dependency Race**: Plan 2.2 contains the manual UAT screenshot checkpoint (Task 4) which explicitly requires Plan 2.1's UI changes to be present in the built APK. Running Plan 2.2 in `wave: 1` parallel to Plan 2.1 guarantees a race condition where the UAT APK might lack the edge-to-edge or splash features.
- **LOW — Import Duplication Risk**: Plan 2.1 Task 1 instructs adding several imports to `MainActivity.kt`. If an auto-formatter or IDE plugin is triggered, or if the executor uses simple text replacement, it might clash or create duplicates. The plan warns to check for duplicates, but explicit `sed` or AST tools might struggle.

## Suggestions
- **Sequence the Plans**: Change Plan 2.2 to `wave: 2` (or explicitly set `depends_on: ["plan-02.1"]` alongside `phase-1`). This entirely eliminates the file collision risk for `AndroidManifest.xml` and `build.gradle.kts`, and ensures the debug APK built for the UAT checkpoint accurately reflects all Phase 2 changes.
- **Consolidate Manifest Changes (Optional)**: If bisectability allows, moving the orphan permission removal (Plan 2.2 Task 1) into Plan 2.1 would centralize all `AndroidManifest.xml` edits into a single sequential flow.
- **Splash Timeout Robustness**: The 3000ms safety timeout in the `LaunchedEffect` is good paranoia. Ensure the `delay` import is fully qualified (`kotlinx.coroutines.delay`) or cleanly added to avoid compilation errors if it's currently missing in `MainActivity.kt`.

## Risk Assessment
**MEDIUM**. The architectural design and Compose code modifications are very low risk and meticulously researched. However, the execution orchestrator's configuration (running overlapping file edits and dependent checkpoints in parallel `wave: 1` plans) elevates the risk of a broken build or corrupted manifest. Fixing the wave sequencing reduces the overall risk to **LOW**.

---

## Codex Review

## Summary

The plans are strong on traceability and acceptance coverage, but they are not actually parallel-safe as written. Both plans touch shared files, especially `AndroidManifest.xml`, and Plan 2.1 also shares `app/build.gradle.kts` with Plan 2.2. The technical direction is mostly sound, but a few implementation snippets should be tightened before execution: predictive-back cancellation handling, splash readiness lifecycle, locale generation edge cases, and over-specific commit-count gates.

## Strengths

- Clear mapping from COMPLY-01..07 to concrete files, commands, commits, and artifacts.
- Good reframing from `targetSdk = 36` enforcement to proactive compliance on the current AGP / SDK floor.
- Correctly avoids premature Phase 5 SPINE styling. The plans stick to plumbing and current visual conventions.
- Permission cleanup is low-risk and security-positive: both orphan permissions are removed, base `FOREGROUND_SERVICE` is preserved.
- Edge-to-edge scope correctly includes the previously missed `BottomNav.MoreSheet` ModalBottomSheet.
- UAT artifact requirements are explicit and falsifiable: screenshot names, device, sign-off, deferred 3-button backlog.
- `installSplashScreen()` timing is correctly specified before `super.onCreate()`.

## Concerns

- **HIGH: Plans are not parallel-safe.**
  Both plans modify `app/src/main/AndroidManifest.xml`. Plan 2.1 changes activity theme and application back flag; Plan 2.2 removes permissions. Both also touch `app/build.gradle.kts`. Running them as independent parallel workers risks merge conflicts or lost edits.

- **HIGH: `PredictiveBackHandler` cancellation behavior should be verified against AndroidX docs.**
  The plan rethrows `CancellationException`. That preserves coroutine etiquette generally, but predictive-back examples often catch cancellation to treat it as a cancelled gesture and return without committing. Rethrow may be harmless, but the plan should not assert it as required unless verified against the exact Activity Compose version.

- **MEDIUM: Splash Pattern A may overcomplicate `MainActivity` and risks duplicate state collection.**
  If `RootViewModel.start` is already collected for navigation, adding another `collectAsState()` can duplicate bindings or subtly change composition structure. The executor should reuse the existing `start` state wherever possible.

- **MEDIUM: Safety timeout changes the strict SPEC behavior.**
  The SPEC says splash remains until `RootViewModel.start` resolves. A hard 3-second fallback means splash can dismiss before start resolution in a pathological case. That is arguably better than ANR, but it should be called out as an intentional deviation.

- **MEDIUM: Locale generation with English-only resources may need `resources.properties`.**
  The plan handles this reactively, which is fine, but acceptance should explicitly check merged manifest output, not only generated file existence. The source manifest correctly should not contain `android:localeConfig`.

## Risk Assessment

Overall risk: **MEDIUM**.

The scope is well bounded and most changes are standard Android platform plumbing, but execution risk is elevated by the false parallel-safety claim and the shared-file conflicts. The highest technical risks are predictive-back cancellation semantics and splash readiness behavior; both are manageable with small plan edits. Security risk is low and likely improves due to permission removal. Performance risk is low to medium: the splash keep condition is cheap, but the timeout behavior should be intentional and documented.

---

## OpenCode Review (GLM-5.1)

## Summary

These two plans are exceptionally thorough, well-structured, and grounded in the detailed SPEC/CONTEXT/RESEARCH artifacts. The code snippets are correct for the target Android API surface (Activity Compose 1.9.3, Compose BOM 2026.05.00, core-splashscreen 1.0.1), the COMPLY-04 splash pattern correctly places `installSplashScreen()` before `super.onCreate()` with a Pattern A AtomicBoolean gate plus ANR safety timeout, the COMPLY-02 `PredictiveBackHandler` migration correctly uses `try/catch(CancellationException)` with re-throw, and COMPLY-01's inset strategy (strip themes.xml + safeDrawingPadding overlay + 3 explicit `contentWindowInsets` on ModalBottomSheets) follows PITFALLS §7/§8 guidance. All 18 SPEC acceptance criteria are verifiably covered by grep checks or artifact existence. The anti-coupling guardrails against Spine pre-painting are consistently enforced. Two medium-severity concerns and a few low-severity items merit attention before execution.

## Strengths

- **Atomic commit hygiene** — one commit per COMPLY requirement, bisect-friendly, matches CONTEXT.md Decision 2 exactly
- **COMPLY-04 splash implementation** — Pattern A (LaunchedEffect → AtomicBoolean) avoids Hilt-before-super.onCreate, and the `delay(3000)` safety timeout addresses Pitfall E3 (ANR risk)
- **COMPLY-02 PredictiveBackHandler** — correct `try { progress.collect {}; onExit() } catch (CancellationException) { throw e }` pattern; explicitly preserves unconditionality (no `enabled=` parameter), matching PITFALLS §8
- **COMPLY-01 researcher correction A8** — all 3 ModalBottomSheet sites (FilterSheet, NavCustomizeSheet, BottomNav.MoreSheet) are covered, not just the 2 originally listed in SPEC.md
- **COMPLY-05 base FGS preservation** — explicitly keeps `FOREGROUND_SERVICE` while removing only the `_MEDIA_PLAYBACK` subtype
- **COMPLY-06 minimal surface** — `generateLocaleConfig` + system Intent only, no custom locale UI; `resources.properties` handled conditionally; Pitfall E6 (manual localeConfig in manifest) correctly prohibited
- **REVIEWS-C4 hygiene** — backlog seeding (COMPLY-07-3BTN, COMPLY-02-NAV-EVENT) follows Phase 1 DEPS-16 precedent; 3-button-nav deferral is documented, not silent
- **Pre-removal notification grep** (RESEARCH A7) — validates zero notification call sites before deleting POST_NOTIFICATIONS

## Concerns

- **MEDIUM — Wave-1 parallelism with shared AndroidManifest.xml**: Both plans have `wave: 1` and `depends_on: ["phase-1"]`, implying parallel execution. Both modify `AndroidManifest.xml` (Plan 02.1: application attribute + activity theme; Plan 02.2: permission line deletions). While these edits target different XML elements and won't produce logical conflicts, a truly parallel executor making simultaneous commits would create git conflicts. The executor must run these sequentially on `phase-2/comply-platform-compliance`. The "parallel-safe" designation refers to logical surface independence, not concurrent git operations. This should be made explicit in execution coordination.

- **MEDIUM — `LanguageRow` R class resolution**: The plan correctly flags that `feature/settings` may use a feature-module R class rather than `io.stashapp.android.R`. Since the string resources (`settings_language`, `settings_language_description`) are defined in `app/src/main/res/values/strings.xml`, the feature module's R class won't contain them unless there's a runtime dependency. In a typical multi-module Gradle setup, `feature/settings` would need to depend on `:app` for its R class, which creates a circular dependency. The likely resolution is using `io.stashapp.android.R` directly in the import, which is valid since the app module's R class merges all resources. However, this should be verified at execution time by checking how other feature modules reference app-scope resources.

- **LOW — COMPLY-01 safeDrawingPadding wrap scope**: The plan instructs "move ALL non-AndroidView UI inside this Box" but PlayerScreen.kt is 1122 lines. If there are composables that intentionally overlap the video surface (e.g., subtitle overlays, thumbnail grids), they should NOT be inside the safeDrawingPadding wrapper. The executor must read the full file and verify the wrap scope includes only chrome/controls, not video-overlay elements that should remain full-bleed.

- **LOW — `contentWindowInsets = { WindowInsets.navigationBars }` lambda vs direct value**: In Compose BOM 2026.05.00, `ModalBottomSheet`'s `contentWindowInsets` parameter is typed as `@Composable () -> WindowInsets`. The plan uses `{ WindowInsets.navigationBars }` which is a composable lambda returning a static WindowInsets value — this is correct. However, `WindowInsets.navigationBars` requires import from `androidx.compose.foundation.layout` and is only valid inside a `@Composable` context. The lambda wrapper satisfies this. The executor should verify these imports are added to each of the 3 files.

- **LOW — `collectAsState` import requirement for splash pattern**: Task 1 lists `import androidx.compose.runtime.collectAsState` and `import androidx.compose.runtime.getValue` (needed for `val start by ... collectAsState()`). If `collectAsState` is already imported in MainActivity.kt, adding a duplicate import is harmless but messy. The plan correctly says "Drop any duplicates" but the executor should verify.

- **LOW — COMPLY-06 `resources.properties` timing**: The plan checks for build warnings after `./gradlew clean assembleDebug` *after* the COMPLY-06 commit. If AGP 8.7.3 emits a warning, the plan amends the commit to add `resources.properties`. This is reasonable but means the COMPLY-06 commit SHA may change. The acceptance criteria don't account for the amended SHA in the git log message check (which uses `git log -1 --format=%s` — message stays the same with `--amend --no-edit`, so this is fine).

## Suggestions

- **Make wave-1 parallelism explicit about serial git commits**: Add a coordination note in both plan headers stating "wave: 1 means the logical work surfaces are independent and can run in any order, BUT git commits to the shared branch must be serial. The executor MUST acquire a branch-level lock or run sequentially when committing." Alternatively, change Plan 02.2 to `wave: 2` with `depends_on: ["plan-02.1"]`, which would serialize at the plan level rather than the commit level.

- **Verify `LanguageRow` R class import path during execution**: Add a Task 2 substep to grep for how `SettingsScreen.kt` currently imports app-scope resources (likely `import io.stashapp.android.R` directly). If a different pattern exists, follow it. If no app-scope resource imports exist in feature modules yet, document the new pattern.

- **Pre-emptive `WindowInsets` import check**: For COMPLY-01 Task 3, add a one-line verification step: `grep -l 'androidx.compose.foundation.layout.WindowInsets' FilterSheet.kt NavCustomizeSheet.kt BottomNav.kt`. If any file is missing the import, add it explicitly.

- **Consider splitting COMPLY-01 into two sub-commits**: Currently Task 3 bundles themes.xml strip (UI plumbing) + ModalBottomSheet insets (3 files) + PlayerScreen overlay wrap (structural) into one commit. If the PlayerScreen wrap has unexpected interactions with the existing layout (unlikely but possible given the file size), a single commit makes it harder to bisect. This is a minor suggestion — the current atomicity matches CONTEXT.md Decision 2.

- **Verify `windowSplashScreenAnimatedIcon` rendering on cold launch**: Task 1's acceptance criteria check that the splash theme exists in XML but don't verify the actual splash appearance. Pitfall E5 (adaptive icon cropping) is deferred to UAT screenshots, but an early `adb shell am start` cold-launch test between Task 1 and Task 3 could catch cropping before the full UAT cycle.

- **Document the `resources.properties` amendment path**: If the build emits a warning, the plan says to `git commit --amend --no-edit`. Consider instead a follow-up commit (`chore(comply): add resources.properties for generateLocaleConfig`), which preserves the original COMPLY-06 commit SHA for cleaner bisect history. This is a style preference and either approach works.

## Risk Assessment

**LOW** — These are well-scoped, well-researched plans for behavioral plumbing on a mature Android codebase. The code changes are surgically minimal (manifest attribute additions, theme color removals, single API call migrations, permission line deletions, a new Settings row). The two medium concerns (manifest parallelism and R class resolution) are coordination/verification issues that are straightforward to resolve during execution. The ANR safety timeout, CancellationException re-throw, and unconditional PredictiveBackHandler guardrails address the primary runtime risks. The 12-screenshot UAT checkpoint on S23+ hardware provides empirical verification of the edge-to-edge and splash changes that can't be caught by grep alone. The overall risk posture is consistent with the project's "moderate risk profile" — stable APIs, minimal surface area, and testable acceptance criteria.

---

## Consensus Summary

### Risk verdict by reviewer

| Reviewer | Verdict | Notes |
|---|---|---|
| **Gemini** | MEDIUM (drops to LOW if wave sequencing fixed) | 2× HIGH (concurrent modification + UAT dependency race), 1× LOW |
| **Codex** | MEDIUM | 2× HIGH (parallel-safety + PredictiveBackHandler cancel semantics), 3× MEDIUM |
| **OpenCode (GLM-5.1)** | LOW | 2× MEDIUM (parallelism + R class), 4× LOW |

**Convergence:** All 3 reviewers landed on MEDIUM or LOW (no FAIL_BLOCKING). The single dominant theme is parallel-safety.

### Agreed Strengths (mentioned by 2+ reviewers)

| Strength | Gemini | Codex | OpenCode |
|---|---|---|---|
| 3 ModalBottomSheet sites (incl. MoreSheet) explicitly covered | ✓ | ✓ | ✓ |
| Atomic per-COMPLY commits / bisect-friendly | — | ✓ | ✓ |
| PredictiveBackHandler signature + try/catch CancellationException pattern | ✓ | — | ✓ |
| Splash Pattern A correctly placed before super.onCreate | ✓ | ✓ | ✓ |
| `targetSdk=36` reframing on current floor (anti-coupling) | — | ✓ | ✓ |
| All 18 SPEC acceptance items verifiably covered | ✓ | ✓ | ✓ |
| Permission removal is net-positive on attack surface | — | ✓ | ✓ |
| Base FGS preserved (not deleted with orphan FGS_MEDIA_PLAYBACK) | — | ✓ | ✓ |
| REVIEWS-C4 hygiene for accepted risks + backlog seeds | — | — | ✓ (singleton) |

### Agreed Concerns (HIGH/MEDIUM raised by 2+ reviewers)

**🔴 CONVERGENT HIGH — Wave-1 parallelism is incorrect (3/3 reviewers)**

| Reviewer | Severity | Specifics |
|---|---|---|
| Gemini | HIGH (×2) | Both plans modify AndroidManifest.xml AND app/build.gradle.kts; UAT in 2.2 needs 2.1's UI changes first |
| Codex | HIGH | "Both plans touch shared files, especially AndroidManifest.xml … running as parallel workers risks merge conflicts or lost edits" |
| OpenCode | MEDIUM | "Wave-1 parallelism … logical surface independence vs concurrent git operations" |

**Recommended fix (Gemini + OpenCode propose the same path):**
- Change Plan 02.2 to `wave: 2`
- Add `depends_on: ["plan-02.1"]` to Plan 02.2 (alongside existing `phase-1`)
- This serializes the manifest edits AND ensures Plan 02.2 Task 4 (S23+ UAT) sees Plan 02.1's UI changes (splash, predictive back, edge-to-edge) in the built APK

**Alternative (Gemini optional suggestion):** consolidate Plan 02.2 Task 1 (orphan permission removal) into Plan 02.1 → single sequential manifest edit flow.

**Verdict:** This MUST be fixed before /gsd-execute-phase 2. Execution otherwise risks broken builds or — worse — UAT validating against an APK missing half the phase's changes.

---

**🟡 Splash Pattern A nuances (2/3 — Codex + OpenCode)**

| Reviewer | Concern | Position |
|---|---|---|
| Codex | MEDIUM | Risk of duplicate `collectAsState` if `RootViewModel.start` already collected elsewhere; safety timeout deviates from strict SPEC ("until `RootViewModel.start` resolves") |
| OpenCode | LOW | `collectAsState` duplicate import flagged but acceptable; safety timeout praised as ANR insurance |
| Gemini | LOW | `delay` import qualification reminder |

**Recommended fix:** Plan 02.1 Task 1 should add an explicit verification step: `grep -c 'rootViewModel.start.collectAsState()' MainActivity.kt` — if > 1, the executor refactors to share a single `collectAsState` call. Also document the 3-second timeout as "intentional SPEC deviation per RESEARCH §A4/E3 — ANR insurance" in the COMPLY-04 commit body.

---

**🟡 `LanguageRow` R class resolution (1/3 — OpenCode unique)**

OpenCode flags that string resources defined in `app/src/main/res/values/strings.xml` won't be in `feature/settings`'s R class unless there's a runtime dependency (which would create a circular dep). Likely resolution: `feature/settings` uses `io.stashapp.android.R` directly. Executor must verify the pattern by checking how other feature modules reference app-scope resources.

**Recommended fix:** Plan 02.2 Task 2 already contains some guidance for this; tighten by adding a pre-task grep: `grep -rE 'import io\.stashapp\.android\.R' feature/`. If zero hits, document the new pattern (or move strings to `feature/settings/src/main/res/values/strings.xml`, which is cleaner).

---

**🟡 PredictiveBackHandler cancellation semantics (1/3 — Codex unique)**

Codex flags that the plan rethrows `CancellationException`, but predictive-back examples often catch it to treat as cancelled gesture and return without committing. Rethrow is likely harmless but the plan asserts it as required without explicit AndroidX docs citation.

**Status:** RESEARCH.md §A2 references the developer.android.com predictive-back guide; the rethrow pattern is the canonical Kotlin coroutines etiquette. CONTEXT.md D-03 locks the rethrow approach. CONTEXT.md gesture-cancel behavior explicitly says "onExit() does NOT fire on cancel/abandoned swipe" — which is exactly the rethrow path's semantics (CancellationException unwinds the try block, skipping the `onExit()` call after `progress.collect {}` returns normally). The plan + CONTEXT are internally consistent. **No action required**, but the commit body for COMPLY-02 should cite both RESEARCH §A2 and CONTEXT D-03 to preempt future confusion.

---

### Divergent views

- **Splash safety timeout**: Codex sees it as a SPEC deviation worth calling out; Gemini and OpenCode see it as good defensive engineering. SPEC.md acceptance only requires `setKeepOnScreenCondition` is invoked once — not a specific dismiss criterion — so the timeout is technically additive, not a SPEC violation. Codex's recommendation to "document the deviation" is sound regardless.

- **COMPLY-01 commit splitting**: OpenCode suggests splitting Task 3 into themes.xml + sheets + PlayerScreen sub-commits for finer bisect granularity. Gemini and Codex don't raise this. CONTEXT.md D-02 says one commit per COMPLY-XX; the planner already aligned with this. Sticking with one commit.

- **Cold-launch splash test**: OpenCode suggests an `adb shell am start` test between Task 1 and Task 3 to catch adaptive icon cropping early. Gemini and Codex defer this to UAT screenshots. Either path works.

---

## Required Pre-Execution Actions

Based on convergence:

### MUST FIX (before /gsd-execute-phase 2)

1. **Wave sequencing** — Change Plan 02.2 `wave: 1` → `wave: 2`; add `depends_on: ["plan-02.1"]`. Optionally also move COMPLY-03 + COMPLY-05 (Plan 02.2 Task 1) into Plan 02.1 if planner judges it improves manifest-edit serialization.

### SHOULD FIX (small edits, low cost)

2. **Splash collectAsState dedup check** — Add to Plan 02.1 Task 1: `grep -c 'rootViewModel.start.collectAsState' MainActivity.kt` pre-modification; refactor if > 0 to share one call.
3. **Splash timeout commit-body documentation** — COMPLY-04 commit body cites "intentional SPEC deviation; RESEARCH §A4/E3 — ANR insurance."
4. **LanguageRow R-class verification step** — Add to Plan 02.2 Task 2 pre-task grep for app-R-import pattern in `feature/`.
5. **PredictiveBackHandler commit-body citation** — COMPLY-02 commit body cites RESEARCH §A2 + CONTEXT D-03 to document the rethrow-on-cancel semantics.

### NICE TO HAVE

6. **WindowInsets import pre-check** — Plan 02.1 Task 3 grep for `androidx.compose.foundation.layout.WindowInsets` in the 3 sheet files; add import if missing.
7. **`delay` import** — Confirm `kotlinx.coroutines.delay` is present in MainActivity.kt (Gemini).
8. **resources.properties path** — Switch from `--amend --no-edit` to a follow-up `chore(comply)` commit if needed (OpenCode style preference).

---

## Next Step

`/gsd-plan-phase 2 --reviews` will re-spawn the planner with this REVIEWS.md as input. Planner will produce revised 02.1-PLAN.md and 02.2-PLAN.md addressing the MUST FIX (wave sequencing) and SHOULD FIX items. NICE TO HAVE can be deferred to execute-phase. After replan, re-run `/spec-layer 2` to advance.

Alternatively, the wave-sequencing fix is small enough to be done by direct edit to 02.2-PLAN.md frontmatter without spawning the planner. Decision belongs to the user.
