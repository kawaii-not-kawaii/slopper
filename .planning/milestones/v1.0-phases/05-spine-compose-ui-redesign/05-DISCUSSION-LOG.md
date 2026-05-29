# Phase 5 Discussion Log

**Phase:** 5 — SPINE (Compose UI Redesign)
**Mode:** `--auto` (single-pass autonomous)
**Date:** 2026-05-19

## Gray Areas Auto-Resolved

| Area | Question | Selected | Rationale |
|------|----------|----------|-----------|
| Plan structure | How many plans, waves? | 3 plans: 5.1 wave 1, 5.2+5.3 wave 2 parallel | Design system must land before screens; 5.2/5.3 disjoint feature sets |
| Bottom nav blur | Modifier.blur on API 26? | Graceful degradation: RenderEffect API 31+, semi-transparent fallback below | minSdk 26; blur is cosmetic; pill bar legible without it |
| Google Fonts | Downloadable or bundled? | Downloadable via ui-text-google-fonts (BOM-managed) | Standard practice; silent fallback to system font if unavailable |
| Accent palette | Replace or extend StashColors? | Hard replace with SpineColors object | Compile failures surface old-token regressions cleanly |
| Chapter strip | Where in PlayerScreen? | Inside bottom scrim Box, above TimelineBar, new ChapterStrip composable in PlayerTimeline.kt | Logical home; Phase 4 split already put timeline composables there |
| Gradients | Brush specs? | Exact rgba values from design handoff (Bg@92% for card, Black@70%+transparent for scrims) | Direct from handoff |
| Testing | New tests or update existing? | Update Phase 4 smoke tests only; no new test files | Visual phase — UAT deferred; smoke tests verify "no crash" |
| Spacing/shapes | Token object or literals? | Local vals in Theme.kt; literal dp in composables | Simple; avoids premature abstraction |
| Nav pill position | Scaffold bottomBar or overlay? | Overlay Box alignment BottomCenter | StandardScheduled NavigationBar would anchor — pill must float |
| Player settings panel | ModalBottomSheet or slide-in? | AnimatedVisibility slideInHorizontally overlay | Handoff specifies "right-anchored panel" not bottom sheet |

## Deferred Ideas

- **v1.x: Theme picker** (Ember/Signal accent) — not in scope; Sage hardwired
- **v1.x: Filter "Save view"** — Room table for SavedView deferred
- **Tablet layout** — phone-only milestone

## Claude's Discretion Items

- Exact Canvas drawing for ChapterStrip proportional segments
- font_certs.xml existence/generation
- Padding arithmetic for pill bar bottom offset
- Column vs Box for chapter strip + timeline layout
