# Phase 5 — SPINE Cross-AI Reviews

**Generated:** 2026-05-19
**Reviewer:** GLM-4.7 (Plans 5.1 + 5.3 reviewed)
**Plan 5.2:** Not separately reviewed — plan-checker Dimension 7 confirmed full CONTEXT compliance

---

## Summary

| Plan | Severity | Verdict |
|------|----------|---------|
| 05.1 — Design System | 0 HIGH, 5 advisory | Approved with suggestions |
| 05.2 — Screens Group 1 | Plan-checker verified | Approved |
| 05.3 — Player + Modals | 2 CRITICAL fixed, 4 minor | Approved after fixes |

**No replan required.** 2 critical code-example bugs in Plan 5.3 fixed before execution.

---

## Plan 05.1 Findings

### ADVISORY-1 — Progress bar height inconsistency (2dp SceneCard vs 3dp ResumeCard)
- Resume bar in SceneCard: 2dp (design spec); SpineResumeCard: 3dp (design spec)
- These ARE different components with different specs per the handoff — both correct
- **Status:** Non-issue (different components)

### ADVISORY-2 — Color `0xEB0A0D12` not a named SpineColors token
- The SceneCard gradient uses `Color(0xEB0A0D12)` (SpineColors.Bg at 92% opacity)
- It's a derived value, not a standalone token — inline usage is acceptable
- **Status:** Advisory — executor may add `val BgScrim = SpineColors.Bg.copy(0.92f)` locally

### ADVISORY-3 — BlurEffect/Build.VERSION_CODES imports not shown
- Plan code references `BlurEffect`, `TileMode`, `Build.VERSION_CODES.S` without imports
- **Status:** Advisory — executor responsibility; `import android.graphics.RenderEffect` etc.

### ADVISORY-4 — Routes package reference for MoreSheet
- Plan references `Routes.browse("tags")` without import path
- **Status:** Advisory — Routes is in `core/ui/.../Routes.kt` which BottomNav.kt already imports

### ADVISORY-5 — FilterSheet.kt in Plan 5.1 files list unnecessarily
- Plan 5.1 files_modified listed FilterSheet.kt (pre-checker fix artifact)
- **Status:** Confirmed not in Plan 5.1 files list (it's in Plan 5.2 and was removed from 5.3)

---

## Plan 05.3 Findings (2 CRITICAL — fixed)

### CRITICAL-1 — ChapterStrip double `collectAsStateWithLifecycle()` in code example (FIXED)
- **Finding:** Code example showed `viewModel.position.collectAsStateWithLifecycle().value.positionMs` twice — double-subscribing to the same StateFlow
- **Why critical:** The note immediately after said to use existing `position` variable, but the code contradicted it. An executor following code over note would introduce a performance bug.
- **Fix applied:** Code example now uses `position.positionMs` / `position.durationMs` with clarifying comment
- **Status:** FIXED in `4bb6fd9`

### CRITICAL-2 — PlayerSettingsPanel uses `.border()` for left-edge-only border (FIXED)
- **Finding:** `Modifier.border(1.dp, BorderStrong, RectangleShape)` adds all 4 sides; the comment said "left border only — use drawBehind" but the code used `.border()`
- **Why critical:** Would produce a visible 4-sided border on the slide-in panel instead of the 1-sided left edge per the handoff spec
- **Fix applied:** Replaced with `Modifier.drawBehind { drawLine(...) }` pattern
- **Status:** FIXED in `4bb6fd9`

### MINOR-1 — Verification grep included FilterSheet (Plan 5.2 ownership)
- **Fixed:** Grep updated to only check Plan 5.3 deliverables; FilterSheet has separate read-only existence check

### MINOR-2 — SearchResults type ambiguity
- The plan references `SearchResults` type without import — executor should check if it exists or define locally
- **Status:** Advisory

### MINOR-3 — Scrim alpha `0xEB` vs `0xF2`
- D-06 spec says "rgba(0,0,0,0.92)" → `0xEB000000`. `0xEB` ≈ 92% — correct per D-06
- **Status:** Non-issue (correct per spec)

### MINOR-4 — MetaMono letterSpacing 1.5sp vs base 0.6sp for section headers
- Intentional deviation: section headers use emphasized MetaMono per Spine handoff
- **Status:** Advisory — add comment in code

---

## Convergent Findings

None. The double-collect and drawBehind bugs were isolated to Plan 5.3 code examples.

---

## Replan Decision

No replan required. All findings were code-example corrections (not plan structure changes). Wave structure, requirement coverage, and file assignments remain unchanged.
