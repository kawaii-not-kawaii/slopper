# Slopper

## What This Is

Slopper is an existing Android application implemented as a Kotlin/Gradle multi-module project (`app/`, `core/*`, `feature/*`, `build-logic/`, `baselineprofile/`). This milestone is a modernization pass: revise the current code state, bring it into alignment with current Android guidelines, enhance runtime performance, and refresh the dependency set — without changing what the app does for end users.

## Core Value

The app continues to install and run on every device it currently supports, but is measurably faster, leaner, and aligned with current Android platform guidance — with a dependency tree the team can keep alive.

## Requirements

### Validated

<!-- Inferred from existing codebase (see .planning/codebase/). These are the capabilities the app already ships. -->

- ✓ Multi-module Android app (`app/`, `core/*`, `feature/*`) — existing
- ✓ Gradle Kotlin DSL build with `build-logic/` convention plugins — existing
- ✓ Baseline Profile generator module (`baselineprofile/`) — existing
- ✓ Detekt / lint configuration under `config/` — existing
- ✓ Release signing scaffold via `keystore.properties.example` — existing
- ✓ Device test instructions documented in `DEVICE_TESTING.md` — existing

### Active

<!-- Modernization goals for this milestone. -->

- [ ] Code aligned with current Android guidelines (Compose, lifecycle, edge-to-edge, predictive back, foreground-service rules, scoped storage, etc.) where applicable to the current feature set
- [ ] Dependency set refreshed to current stable releases (AGP, Kotlin, Compose BOM, Hilt/DI, Coroutines, libraries) — no alpha/RC unless explicitly justified
- [ ] Anti-patterns surfaced in `.planning/codebase/CONCERNS.md` resolved (without changing module structure)
- [ ] Measurable performance wins captured via baseline profile + macrobenchmark deltas (startup, scroll/jank, key flows)
- [ ] Build/CI healthy after upgrades — no new warnings, lint clean, tests green
- [ ] Documentation refreshed where dependency or guideline changes invalidate it (READMEs, DEVICE_TESTING.md, build-logic comments)

### Out of Scope

- New end-user features — this milestone is purely modernization
- `minSdk` bump — must remain installable on existing devices
- New third-party SDKs / vendor dependencies — unless replacing one already in use
- Restructuring the module graph (`app/` / `core/` / `feature/` / `build-logic/`) — refactors stay within modules
- Migrating off Compose / Hilt / Gradle Kotlin DSL — keep the existing architecture
- Performance changes without a baseline-profile or macrobenchmark delta — "feels faster" is not enough

## Context

- Codebase was recently restored from a Claude session history (commit `bf01b34 feat: restore Slopper Android app from Claude session history`) — there may be incomplete bits surfaced in `.planning/codebase/CONCERNS.md`.
- Codebase map already produced in `.planning/codebase/` (STACK, INTEGRATIONS, ARCHITECTURE, STRUCTURE, CONVENTIONS, TESTING, CONCERNS) and should be the source of truth for the existing state when planning each phase.
- Project owner runs a homelab + values clean, idiomatic builds (Kotlin DSL, Compose-first, baseline profiles already wired up).
- The presence of `baselineprofile/` and a populated `build-logic/` indicates the project is already past the "weekend prototype" bar — modernization should respect that investment.

## Constraints

- **Compatibility**: Must remain installable on all currently-supported devices — no `minSdk` bump, no removal of ABIs.
- **Architecture**: Preserve the existing module layout (`app/`, `core/`, `feature/`, `build-logic/`, `baselineprofile/`). Refactors stay inside modules.
- **Dependencies**: Stable releases only (moderate risk profile). New third-party SDKs require explicit approval.
- **Performance**: Each perf claim must be backed by a baseline-profile or macrobenchmark delta — no unmeasured "optimizations".
- **Build**: After each phase, the build must remain green (assemble + unit tests + lint + detekt).
- **Scope discipline**: No new user-facing features; modernization only.

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Balanced sweep across guidelines, performance, and deps | User wants one coherent modernization roadmap, not three separate passes | — Pending |
| Moderate risk profile (stable releases, internal refactors OK, no public API breaks) | Keep upgrade fallout manageable; don't chase alphas | — Pending |
| Keep current `minSdk` | Must stay installable on existing devices | — Pending |
| Coarse phase granularity (3–5 phases) | Modernization work clusters naturally into a few large themes (deps, guidelines, perf, polish) | — Pending |
| YOLO mode with Research + Plan Check + Verifier enabled | User wants speed but also wants each phase grounded in current Android guidance and verified against its goal | — Pending |
| Quality model profile (Opus for research/roadmap) | Modernization decisions benefit from deep analysis of Android ecosystem state | — Pending |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd-transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `/gsd-complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-05-16 after initialization*
