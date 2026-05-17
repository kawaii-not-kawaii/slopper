# Phase 2: COMPLY — Platform Compliance - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-05-17
**Phase:** 02-comply-platform-compliance
**Mode:** `--auto` (single-pass; auto-mode picked the recommended option for each gray area; no interactive AskUserQuestion turns)
**Areas auto-resolved:** Branch shape · Commit shape · PredictiveBackHandler API choice · Edge-to-edge inset patterns · Splash gate implementation · Locale picker placement · Orphan permission removal · UAT + screenshot pack structure · Accepted risks

---

## Area 1 — Branch shape

| Option | Description | Selected |
|--------|-------------|----------|
| Fresh `phase-2/comply-platform-compliance` (recommended) | Isolated branch; base `73a5677` (current PR-#5 head); rebase to `master` post-merge | ✓ |
| Continue on `phase-1/deps-bump` | Adds Phase 2 commits to PR #5 — balloons the review surface | |
| Branch from `master` immediately | Loses Phase 1 floor (Compose BOM 2026.05.00 needed as splash + inset precondition) — would force cherry-picks | |

**Auto choice:** Fresh `phase-2/comply-platform-compliance` (already created at commit `7d851a0` during spec-phase fixup).
**Notes:** SPEC commit `b5b51f0` was accidentally landed on `phase-1/deps-bump` during spec-phase and moved off via `git branch` + `git reset --hard HEAD~1` (local-only, no force-push). PR #5 unaffected.

---

## Area 2 — Commit shape

| Option | Description | Selected |
|--------|-------------|----------|
| One commit per COMPLY-XX requirement (recommended; carries forward Phase 1 Decision 1) | 7 + 1 screenshot commit = 8 total; max bisect granularity | ✓ |
| Two grouped commits (UI changes + manifest changes) | Smaller history; harder to bisect a single regression | |
| One big commit at end | Worst bisect-ability; cleanest history | |

**Auto choice:** Per-requirement atomic commits.
**Notes:** Optional fold of COMPLY-03 + COMPLY-05 into one removal commit flagged in CONTEXT.md §7 as planner discretion; default stays separate for clean bisect.

---

## Area 3 — Predictive back API choice

| Option | Description | Selected |
|--------|-------------|----------|
| `PredictiveBackHandler` (only option at current floor — recommended) | Available in Activity Compose 1.8.0+; we're at 1.9.3 ✓ | ✓ |
| `NavigationBackHandler` / `NavigationEventState` (post-deprecation API) | Requires Activity Compose ≥ 1.10; we're at 1.9.3 (deferred 1.13.0 via DEPS-07) — NOT available | |

**Auto choice:** `PredictiveBackHandler` at PlayerScreen.kt:187.
**Notes:** PITFALLS §8 flags `PredictiveBackHandler` as deprecation-path on Compose Multiplatform 1.10+. Backlog item `COMPLY-02-NAV-EVENT` added to CONTEXT.md `<deferred>` for migration once AGP-9 phase lands Activity Compose 1.13.0+.
**Gesture-cancel:** Material3 default — `onExit()` does NOT fire on cancel/abandoned swipe (matches user expectation of "peek without committing").

---

## Area 4 — Edge-to-edge inset patterns (COMPLY-01)

| Option | Description | Selected |
|--------|-------------|----------|
| Minimum-touch PITFALLS §7 patterns (recommended) | strip themes.xml system-bar colors; `Box(safeDrawingPadding())` around PlayerScreen overlays; `contentWindowInsets = navigationBars` on ModalBottomSheet | ✓ |
| Custom `WindowInsetsControllerCompat` wrapper | More control; introduces a custom abstraction layer; not justified by current scope | |
| Scaffold restructuring across all top-level screens | Higher risk; touches modules unnecessarily | |

**Auto choice:** Minimum-touch §7 patterns.
**Notes:** Other top-level Scaffolds (Home, Library, Settings, Connection) get a grep-audit; default Material3 Scaffold uses `ScaffoldDefaults.contentWindowInsets = WindowInsets.systemBars` which is correct. Planner flags anomalies as follow-ups, not blockers.

---

## Area 5 — Splash gate implementation (COMPLY-04)

| Option | Description | Selected |
|--------|-------------|----------|
| **A** — `LaunchedEffect(start) → AtomicBoolean` keep-alive flag (recommended) | Simple; ~50ms invisible splash extension; no Hilt-before-super gymnastics | ✓ |
| **B** — Direct `ConnectionRepository.activeServer()` probe into keep condition | Faster splash dismiss; more coupling; bypasses RootViewModel | |

**Auto choice:** Pattern A (LaunchedEffect-driven AtomicBoolean).
**Notes:** Planner makes the final call during planning; A is the recommended starting point. The keep-condition implementation pattern is the only sub-decision flagged for planner judgment in COMPLY-04.

---

## Area 6 — Locale picker placement (COMPLY-06)

| Option | Description | Selected |
|--------|-------------|----------|
| Top-level row in primary settings list (recommended) | Above any "Advanced" / "Diagnostics" sections, below connection block; new `R.string.settings_language` resource | ✓ |
| Nested under existing "App" / "Preferences" group | Requires the group to already exist; `SettingsScreen.kt` has 412 lines, structure to be inspected by planner | |
| Reuse existing string resource key | None present — `SettingsScreen.kt` has zero `language` / `locale` mentions; new key required regardless | |

**Auto choice:** Top-level row, new `R.string.settings_language` + `R.string.settings_language_description` resources, `Icons.Outlined.Language` leading icon, API-33 gate.

---

## Area 7 — Orphan permission removal (COMPLY-03 + COMPLY-05)

| Option | Description | Selected |
|--------|-------------|----------|
| Two separate atomic commits (recommended) | Max bisect granularity; mirrors Phase 1 pattern | ✓ |
| Single combined commit | Cleaner history; loses 1 bisect step | |

**Auto choice:** Two separate commits (planner may fold if judged worth it; default stays separate).

---

## Area 8 — UAT + screenshot pack structure

| Option | Description | Selected |
|--------|-------------|----------|
| Flat `screenshots/<screen>-<theme>.png` + `SCREENSHOTS.md` index (recommended) | 12 PNGs, predictable naming, single index for human review | ✓ |
| Nested `screenshots/<theme>/<screen>.png` | Cleaner directory listing per theme; harder to scan diff in a PR | |
| One composite contact-sheet image | Easier to embed in PR; loses per-screenshot detail | |

**Auto choice:** Flat naming + `SCREENSHOTS.md` index. 12 PNGs covers Home / Library / Player (controls visible + hidden) / Settings / Connection × light+dark.

---

## Area 9 — Accepted risks (REVIEWS-C4 compliance)

| Risk | Disposition |
|------|-------------|
| PITFALLS §7 — 3-button-nav edge-to-edge unverified | ACCEPT — backlog `COMPLY-07-3BTN`; revisit on hardware availability or AGP-9 phase re-verification |
| PITFALLS §8 — `PredictiveBackHandler` on deprecation path | ACCEPT (informational) — backlog `COMPLY-02-NAV-EVENT`; migrate to `NavigationBackHandler` once Activity Compose ≥ 1.10 |

**Notes:** Risk #1 was a direct user ACCEPT during spec-phase Q2 (2026-05-17). Risk #2 is auto-accepted because `PredictiveBackHandler` is the only API actually available at the current floor.

---

## Claude's Discretion

These were not user-decided (auto mode); the planner / executor exercises judgment with the documented preferences:

- Splash icon asset = launcher icon (no new design work); fallback to `@drawable/splash_icon` foreground variant only if adaptive layers crop awkwardly on Android 12+
- Screenshot capture method = `adb shell screencap` over USB or system screenshot — either acceptable
- String resource location = `app/src/main/res/values/strings.xml` (app-level, since `LanguageRow` calls a system Intent not a feature-local action)
- PR description structure = per-COMPLY-XX summary with screenshot embeds + UAT table + accepted-risk callouts
- `R.string.settings_language_description` content = 1-sentence description matching SettingsScreen voice convention (planner inspects existing rows on first read)

## Deferred Ideas

- `COMPLY-07-3BTN` (3-button-nav verification) — backlog; revisit on hardware availability
- `COMPLY-02-NAV-EVENT` (NavigationBackHandler migration) — backlog; revisit AGP-9 phase
- Real notification flow — BG-MEDIA-01 / BG-MEDIA-02 milestone
- Translation work — separate effort, not modernization
- `PlayerScreen.kt` structural split — POLISH-01 in Phase 4
- Custom in-app locale list UI — explicit out-of-scope per SPEC.md
- `network_security_config.xml` cleartext tightening — SEC-02 backlog

---

*Phase: 02-comply-platform-compliance*
*Discussion logged: 2026-05-17*
*Mode: --auto*
