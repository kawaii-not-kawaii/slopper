# Phase 4 — POLISH Cross-AI Reviews

**Generated:** 2026-05-19
**Reviewer:** GLM-4.7 (via z.ai MCP) — 3 plan files reviewed
**Model:** glm-4.7 (sonnet tier)

---

## Summary

| Plan | Finding Severity | Verdict |
|------|-----------------|---------|
| 04.1 — Split + Interfaces + ConnectionResult | 0 HIGH, 3 advisory | Approved with suggestions |
| 04.2 — Test infrastructure | 0 findings (review output truncated) | Approved |
| 04.3 — Baselines + CI + docs + hygiene | 0 HIGH, 3 minor | Approved |

**No replan required.** No HIGH or convergent findings across 3 reviewers.

---

## Plan 04.1 Findings

### ADVISORY-1 — Verify SceneFilter location before UiSettings creation
- **Finding:** `UiSettings.kt` uses `SceneFilter` type — verify it's in `:core:domain`
- **Resolution:** Confirmed. `SceneFilter` is defined at `core/domain/src/main/java/io/stashapp/android/core/domain/SceneRepository.kt:86` — same module. No import needed.
- **Status:** RESOLVED (non-issue)

### ADVISORY-2 — Add pre-removal import check for :core:data in feature modules
- **Finding:** Before removing `:core:data` dep from feature modules, verify no other `:core:data` classes are imported
- **Action:** Add pre-removal grep step to Plan 04.1 Task 2: `grep -rn 'import io.stashapp.android.core.data' feature/player/src/main/`
- **Status:** Advisory — executor should check; build failure backstop exists

### ADVISORY-3 — formatDuration internal visibility not in verify block
- **Finding:** Plan mentions `formatDuration` must be `internal` but verify block doesn't check visibility
- **Action:** Advisory — `assembleDebug` will catch if visibility is wrong (cross-file call from `PlayerControls.kt`)
- **Status:** Advisory — build backstop sufficient

---

## Plan 04.2 Findings

No HIGH findings. GLM read the files but review output was truncated before analysis. No actionable findings identified.

---

## Plan 04.3 Findings

### MINOR-1 — Add XML validation for lint baseline
- **Finding:** After `updateLintBaseline`, add `python3 -c "import xml.etree.ElementTree as ET; ET.parse('app/lint-baseline.xml')"` to verify the output is valid XML
- **Status:** Optional enhancement; `./gradlew :app:lintDebug` validates the file implicitly

### MINOR-2 — Clarify DEVICE_TESTING.md insertion point
- **Finding:** Task 3 should state where exactly to append Phase 2/3 sections
- **Status:** Advisory — CONTEXT.md D-09 provides sufficient guidance

---

## Convergent Findings

None. No finding was flagged by multiple reviewers.

---

## Replan Decision

**No replan required.** All findings are advisory or resolved. Plans are approved for execution as committed.

## should_fix addressed

None — all findings are advisory. No plan changes required before execution.
