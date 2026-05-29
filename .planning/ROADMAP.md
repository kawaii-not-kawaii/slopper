# Roadmap: Slopper Android Modernization

## Milestones

- ✅ **v1.0 Modernization** — Phases 1–6 (shipped 2026-05-29) — full detail in [`milestones/v1.0-ROADMAP.md`](milestones/v1.0-ROADMAP.md)

## Phases

<details>
<summary>✅ v1.0 Modernization (Phases 1–6) — SHIPPED 2026-05-29</summary>

- [x] Phase 1: DEPS — Foundation Bump (3/3 plans) — completed 2026-05-17
- [x] Phase 2: COMPLY — Platform Compliance (2/2 plans) — completed 2026-05-18
- [x] Phase 3: PERF — Measured Wins (3/3 plans) — completed 2026-05-19
- [x] Phase 4: POLISH — Test Pyramid & Cleanup (3/3 plans) — completed 2026-05-19
- [x] Phase 5: SPINE — Compose UI Redesign (3/3 plans) — completed 2026-05-19
- [x] Phase 6: SETTINGS-V3 — Hub + Drill-Down Settings (3/3 plans) — completed 2026-05-29

Full phase detail, success criteria, and risk register archived in [`milestones/v1.0-ROADMAP.md`](milestones/v1.0-ROADMAP.md).

</details>

## Progress

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 1. DEPS | v1.0 | 3/3 | Complete | 2026-05-17 |
| 2. COMPLY | v1.0 | 2/2 | Complete | 2026-05-18 |
| 3. PERF | v1.0 | 3/3 | Complete | 2026-05-19 |
| 4. POLISH | v1.0 | 3/3 | Complete | 2026-05-19 |
| 5. SPINE | v1.0 | 3/3 | Complete | 2026-05-19 |
| 6. SETTINGS-V3 | v1.0 | 3/3 | Complete | 2026-05-29 |

## Carried Tech Debt (into next milestone)

- **AGP 9 / compileSdk 36 / Media3 1.10** upgrade (DEPS-17) — blocked on Hilt AGP-9 support
- **Macrobenchmark execution + output capture** (Phase 3) — infrastructure ready, device measurement deferred
- **Formal per-screen visual-fidelity screenshot audit** (Phase 5) — SPINE UI confirmed on S23+ via Phase 6 device UAT
- **COMPLY-07-3BTN** (3-button-nav re-verification), **COMPLY-02-NAV-EVENT** (NavigationBackHandler migration)
- **WR-02** (Phase 6 review): SettingsViewModel domain-interface refactor — requires moving companion constants to interfaces

## Out of Scope (carried from v1.0)

| Item | Reason |
|------|--------|
| New end-user features | Modernization-only milestone |
| `minSdk` bump | Must stay installable on existing devices |
| New third-party SDKs | No vendor additions without explicit approval |
| Module-graph restructure | Frozen — refactors stay within modules |
| Migrating off Compose / Hilt / Gradle Kotlin DSL | Existing architecture preserved |
| Real `MediaSessionService` (background playback) | Deferred to BG-MEDIA milestone |
| Apollo 5 / Nav3 migrations | Each deserves its own milestone |
