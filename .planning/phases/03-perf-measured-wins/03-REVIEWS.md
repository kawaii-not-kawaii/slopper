# Phase 3 — PERF Cross-AI Reviews

**Generated:** 2026-05-18
**Phase:** 03-perf-measured-wins
**Reviewer:** GLM-4.7 (via z.ai MCP) — 3 plan files reviewed
**Model:** glm-4.7 (sonnet tier)

---

## Review Summary

| Plan | Severity | Findings | Verdict |
|------|----------|----------|---------|
| 03.1 — Measurement Substrate | 2 MEDIUM, 3 LOW | See below | Approve with required changes |
| 03.2 — Compose Hygiene | 0 HIGH/MEDIUM, 0 blocking | Implementation verified correct | GOOD |
| 03.3 — Shuffle + Macrobench | 3 MINOR | Placeholder verification gaps | GOOD with minor suggestions |

**Overall:** No HIGH or convergent blockers. 2 MEDIUM findings in Plan 03.1 corrected before commit. Plans are ready for execution.

---

## Plan 03.1 Findings

### MEDIUM-1 — pixel6Api34 grep count incorrect (FIXED)
- **Severity:** MEDIUM
- **Location:** Plan 03.1 Task 1 verify block
- **Finding:** Grep expectation was `Expected: 3` but the source file only contains 2 occurrences (`create("pixel6Api34")` + `managedDevices += "pixel6Api34"`). The third "occurrence" (task name) is Gradle-generated at build time, not present in the source file.
- **Resolution:** Fixed — expectation changed to `Expected: 2` with note explaining Gradle generates the task name.

### MEDIUM-2 — Missing `Direction` import in Task 3 journey code (FIXED)
- **Severity:** MEDIUM
- **Location:** Plan 03.1 Task 3 journey expansions
- **Finding:** The plan's journey code uses `Direction.DOWN` and `Direction.RIGHT`, but the existing `StashBaselineProfileGenerator.kt` imports only `By` and `Until` — `Direction` is not imported. The plan incorrectly stated "No import changes needed."
- **Resolution:** Fixed — plan now includes an import check step with grep verification before adding the import.

### LOW-1 — Import duplication check missing for AndroidComposeConventionPlugin
- **Severity:** LOW
- **Location:** Plan 03.1 Task 2 Step B.1
- **Finding:** The plan adds `import org.gradle.kotlin.dsl.configure` but doesn't check if it's already present (duplicate imports cause compile errors).
- **Recommendation:** Add pre-edit check: `grep -c 'import org.gradle.kotlin.dsl.configure' AndroidComposeConventionPlugin.kt` → skip if result ≥ 1.
- **Status:** Advisory — executor should check; `assembleDebug` will fail loudly on duplicate import.

### LOW-2 — Stability reports directory existence check
- **Severity:** LOW
- **Location:** Plan 03.1 Task 2 verification
- **Finding:** The verify block searches for `*.txt` files under `build/compose-reports` without first verifying the directory exists, making failure diagnosis harder.
- **Recommendation:** Add `ls -d .../build/compose-reports` before the `find` command.
- **Status:** Advisory — not blocking execution.

### LOW-3 — Anti-coupling Spine check is Phase 2 carry-forward
- **Severity:** LOW
- **Location:** Plan 03.1 verification
- **Finding:** The Spine anti-coupling check (`grep -rn 'import .*Spine'`) is correct and valuable as a carry-forward invariant. The reviewer noted it might be "irrelevant" — this is incorrect. It IS relevant as a regression check across all phases.
- **Status:** Keep as-is. The check is intentional and was specifically mandated by Phase 2 CONTEXT.md to carry forward.

---

## Plan 03.2 Findings

### No blocking issues
The GLM reviewer validated the ImmutableList migration approach by tracing the code path:
- `HomeRail.scenes: ImmutableList<SceneSummary>` + `HomeUiState.rails: ImmutableList<HomeRail>` migration is correct
- `HomeUiState.Initial` companion `toPersistentList()` is required and documented in the plan
- Both `:feature:home` and `:feature:player` dependency additions are correct
- `toPersistentList()` at `state.current?.markers.orEmpty()` call sites is the correct approach
- PERF-09 `applyVideoFrameRate` relocation to `LaunchedEffect(state.videoFrameRate)` is correct; no hoisting needed since `playerView` is already top-level

---

## Plan 03.3 Findings

### MINOR-1 — Task 1 placeholder verification incomplete
- **Location:** Task 1 Step 5 verification
- **Finding:** The investigation artifact template contains `[X]` and `[Fill in]` placeholders, but the verify block doesn't check that these were actually filled.
- **Recommendation:** Add: `grep -c '\[X\]\|\[Fill in\]\|\[describe\]' perf-08-shuffle-investigation.md` → Expected: 0

### MINOR-2 — Fix condition ambiguity
- **Location:** Task 1 Step 3
- **Finding:** The shuffle fix code uses `queue.reset() // or equivalent method`. If `PlayerQueue.reset()` doesn't exist, the fix is unclear.
- **Recommendation:** Clarify: "If `PlayerQueue` has no `reset()` method, document as deferred — do not create the method in this plan."

### MINOR-3 — Output section timing ambiguity
- **Location:** Output section
- **Finding:** The output template should clarify that it's filled after the human checkpoint completes.
- **Status:** Advisory only.

---

## Convergent Findings

No 2/3 or 3/3 convergent findings across plans. Both MEDIUM findings above were isolated to Plan 03.1 and corrected before this REVIEWS.md was committed.

---

## Replan Decision

**No replan required.** The 2 MEDIUM fixes were surgical corrections (grep count + import note) applied directly to Plan 03.1 before commit. No plan restructuring, wave-sequencing changes, or requirement coverage gaps were identified. Plan 03.2 and Plan 03.3 are accepted without changes.

---

## should_fix addressed

- ✅ MEDIUM-1: pixel6Api34 grep count corrected in Plan 03.1
- ✅ MEDIUM-2: Direction import check added to Plan 03.1 Task 3

## nice_to_have deferred

- LOW-1: Convention plugin duplicate import check — executor responsibility (assembleDebug backstop)
- LOW-2: Stability reports directory check — advisory
- MINOR-1..3: Placeholder verification and fix condition clarity — advisory
