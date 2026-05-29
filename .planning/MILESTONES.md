# Milestones

## v1.0 Modernization (Shipped: 2026-05-29)

**Phases completed:** 6 phases, 17 plans, 20 tasks
**Timeline:** 2026-05-16 → 2026-05-29 (13 days)
**Codebase:** ~24.7K LOC Kotlin, multi-module Compose app
**Tag:** v1.0

**Delivered:** A full modernization pass on the Slopper Android client — refreshed dependency floor, current-guideline platform compliance, measured performance infrastructure, a test pyramid + cleanup, and a complete Compose UI redesign (Spine) capped by a hub-and-drill-down Settings system.

**Key accomplishments:**

- **DEPS (Phase 1):** Dependency lockstep bump — Kotlin 2.2.20, Compose BOM 2026.05.00, Hilt 2.56.2, Apollo 4.4.3. AGP 9 / compileSdk 36 deliberately deferred (no Hilt support yet; tracked as DEPS-17).
- **COMPLY (Phase 2):** Platform compliance — edge-to-edge, predictive back, per-app language, splash ANR safety-timeout, orphan permission removal. Verbal UAT PASS on Galaxy S23+ / Android 16.
- **PERF (Phase 3):** Performance infrastructure — GMD (Pixel 6 / API 34), Compose Compiler stability reports, ImmutableList at the ViewModel boundary, 4-journey baseline profile, and the PERF-08 silent-queue-exhaustion fix (End-of-queue banner).
- **POLISH (Phase 4):** Test pyramid + cleanup — JUnit5/MockK/Turbine/Robolectric wired, 17 seed tests, PlayerScreen split 1227→480L, ConnectionResult retired, lint baseline 1001→11 lines, Forgejo CI workflow.
- **SPINE (Phase 5):** Full Compose UI redesign — SpineColors (19 tokens), Google Fonts (Space Grotesk + JetBrains Mono), pill nav replacing M3 TabRow, SceneCard, redesigned Home/Library/Detail/Browse/Player/Connection screens.
- **SETTINGS-V3 (Phase 6):** Hub + drill-down Settings — 6 detail pages, accent palette picker, 44-entry search overlay, CSlider/DRow components, 26 new DataStore prefs. 11/11 requirements + 4/4 device UAT passed on S23+.

**Known deferred items (accepted as carried tech debt into next milestone):**

- AGP 9 / compileSdk 36 / Media3 1.10 upgrade (DEPS-17) — blocked on Hilt AGP-9 support.
- Macrobenchmark execution + benchmark output capture (Phase 3) — infrastructure ready, device measurement deferred.
- Formal per-screen visual-fidelity screenshot audit (Phase 5) — SPINE UI confirmed rendering on S23+ via Phase 6 device UAT; formal screenshot pack deferred.
- COMPLY-07-3BTN (3-button-nav re-verification) and COMPLY-02-NAV-EVENT (NavigationBackHandler migration) — backlog.
- WR-02 (Phase 6 review): SettingsViewModel domain-interface refactor — requires moving companion constants to interfaces.

---
