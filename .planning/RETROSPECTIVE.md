# Project Retrospective

*A living document updated after each milestone. Lessons feed forward into future planning.*

## Milestone: v1.0 — Modernization

**Shipped:** 2026-05-29
**Phases:** 6 | **Plans:** 17 | **Timeline:** 13 days (2026-05-16 → 2026-05-29)

### What Was Built
- **DEPS:** Dependency floor bump — Kotlin 2.2.20, Compose BOM 2026.05.00, Hilt 2.56.2, Apollo 4.4.3 lockstep (AGP 9 deferred).
- **COMPLY:** Platform compliance — edge-to-edge, predictive back, per-app language, splash ANR safety, orphan permission removal.
- **PERF:** Perf infrastructure — GMD, Compose stability reports, ImmutableList at VM boundary, 4-journey baseline profile, shuffle-exhaustion fix.
- **POLISH:** Test pyramid (JUnit5/MockK/Turbine/Robolectric, 17 seed tests), PlayerScreen split 1227→480L, lint 1001→11L, ConnectionResult retired, Forgejo CI.
- **SPINE:** Full Compose redesign — SpineColors, Google Fonts, pill nav, SceneCard, all feature screens.
- **SETTINGS-V3:** Settings hub + drill-down — 6 detail pages, accent palette picker, 44-entry search overlay, CSlider/DRow, 26 new prefs.

### What Worked
- **Wave-based parallel execution with file-overlap detection** — caught the 06.1/06.3 SettingsViewModel+SettingsScreen overlap and serialized correctly instead of racing worktrees.
- **Cross-AI review gate** caught a 3/3-convergent HIGH in Phase 2 (wave-1 parallelism on shared AndroidManifest) before it shipped — replan via `--reviews` fixed it cleanly.
- **Deferring device UAT to end-of-milestone** kept the build-execute loop fast across Phases 3–5; the single S23+ pass at the end (Phase 6) confirmed the whole UI stack.
- **Continuous-branch PR strategy** (Phases 2–6 bundled into PR #6) avoided PR-per-phase churn.

### What Was Inefficient
- **AGP 9 false start** in Phase 1 — planned the bump, hit the Hilt-doesn't-support-AGP-9 wall mid-wave, had to re-scope and defer (DEPS-17). A pre-flight dependency-compatibility check would have caught this before planning.
- **`milestone.complete` accomplishment auto-extraction** grabbed stray summary lines (rule-violation bullets, bare filenames) — required a full manual rewrite of MILESTONES.md.
- **Milestone naming drift** — CLI normalized `v1.0` → `v1`; had to rename archive files and headers to match the project's established `v1.0` convention.
- **UAT status frontmatter missing** on Phases 2–5 — the deferred-UAT files lacked `status:` YAML, so the close-time audit flagged them as `[unknown]` gaps requiring a cleanup pass.

### Patterns Established
- **Deferred-accepted UAT**: device/visual checks that pass code-level verification but lack hardware confirmation get explicit `status: deferred-accepted` frontmatter + a resolution note, rather than blocking the milestone.
- **REVIEWS-C4 ACCEPT path**: device-dependent or environment-blocked requirements documented as accepted risk with structural mitigation instead of hard-failing.
- **Anti-coupling grep gate**: `grep -rn 'import.*Spine'` in pre-Phase-5 plans kept the design system out of behavioral phases.

### Key Lessons
1. **Run a dependency-compatibility pre-flight before planning a toolchain bump.** The AGP 9 / Hilt incompatibility cost a re-scope mid-execution.
2. **Give every UAT file `status:` frontmatter at creation**, even when deferred — it keeps the milestone-close audit clean.
3. **The milestone-complete CLI's auto-extracted accomplishments need a manual pass** — budget for rewriting MILESTONES.md.
4. **File-overlap detection in wave execution is load-bearing** — `depends_on` alone wasn't enough; the SettingsViewModel/SettingsScreen shared-file check forced correct serialization.

### Cost Observations
- Model mix: predominantly Sonnet for execution (gsd-executor subagents); orchestration on Opus.
- Phase 6 executed in ~3 sequential subagent runs (~54 min total) + code review + fix + verify.
- Notable: serialized 3-plan wave avoided worktree merge conflicts entirely — no post-merge test failures.

---

## Cross-Milestone Trends

### Process Evolution

| Milestone | Phases | Key Change |
|-----------|--------|------------|
| v1.0 | 6 | Established GSD 11-step pipeline + cross-AI review gate + wave-based parallel execution |

### Cumulative Quality

| Milestone | Tests | Lint Baseline | Notable |
|-----------|-------|---------------|---------|
| v1.0 | 17 seed + 4 search filter | 1001 → 11 lines | PlayerScreen 1227→480L; ConnectionResult retired |

### Top Lessons (Verified Across Milestones)

1. *(Single milestone so far — cross-validation pending v1.1.)*
