# Slopper

## What This Is

Slopper is an existing Android application implemented as a Kotlin/Gradle multi-module project (`app/`, `core/*`, `feature/*`, `build-logic/`, `baselineprofile/`). This milestone is a modernization pass: revise the current code state, bring it into alignment with current Android guidelines, enhance runtime performance, and refresh the dependency set — without changing what the app does for end users.

## Core Value

The app continues to install and run on every device it currently supports, but is measurably faster, leaner, and aligned with current Android platform guidance — with a dependency tree the team can keep alive.

## Current Milestone: v1.1 AGP-9 Toolchain Modernization (DEPS-17)

**Goal:** Unblock and land the deferred build-toolchain cluster — now viable since Hilt gained AGP-9 support (Dagger 2.59) — without changing app behavior or bumping `minSdk`.

**Target changes:**
- AGP 8.7.3 → 9.x and Gradle 8.11.1 → 9.1+ (the core bump the whole cluster hangs off)
- Hilt 2.56.2 → 2.59+ (the unblocker) and KSP realigned to Kotlin
- compileSdk 35 → 36 (runtime `minSdk`/`targetSdk` behavior unchanged)
- Media3 1.9.1 → 1.10+ with `nextlib-media3ext` lockstep
- Now-unblocked leaf libs: activity-compose 1.13, core-ktx 1.18
- build-logic migration: AGP 9 built-in Kotlin support (drop `org.jetbrains.kotlin.android`) and legacy-variant-API disablement (check baseline-profile / `applicationVariants`)
- Re-evaluate the CI APK-signing BouncyCastle EdEC blocker under the new toolchain (may now permit restoring a real `assembleDebug` gate)

**Key context:** Blocker cleared — AGP 9.2.0 GA (Apr 2026), Dagger 2.59+ adds Hilt AGP-9 support. Re-activates the entire Dependabot-excluded toolchain cluster. v1.0 constraints carry forward (stable releases only, build green per phase, no `minSdk` bump, module graph frozen).

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

- [x] Code aligned with current Android guidelines — Validated in Phase 2 (COMPLY): edge-to-edge, predictive back, per-app language, splash, permission cleanup
- [x] Dependency set refreshed — Validated in Phase 1 (DEPS): Kotlin 2.2.20, Compose BOM 2026.05.00, Hilt 2.56.2, Apollo 4.4.3 lockstep (AGP 9 / compileSdk 36 deferred to DEPS-17 backlog)
- [x] Anti-patterns resolved — Validated in Phase 4 (POLISH): PlayerScreen split 1227→480L, ConnectionResult retired, lint 1001→11L, 17 seed tests
- [x] Measurable performance wins — Validated in Phase 3 (PERF): ImmutableList stability, recomposition markers, queue end banner, shuffle fix (macrobench execution device-deferred)
- [x] Build/CI healthy — Validated across Phases 1–6: build green at every phase completion; Forgejo CI workflow wired
- [x] Documentation refreshed — Validated in Phases 1–6: README, ARCHITECTURE, DEVELOPMENT, CONFIGURATION, TESTING, GETTING-STARTED all updated
- [x] UI redesigned to Spine design handoff — Validated in Phase 5 (SPINE): SpineColors, Google Fonts, pill nav, SceneCard, all screens
- [x] Settings hub + drill-down redesign — Validated in Phase 6 (SETTINGS-V3): 6 detail pages, search overlay, accent palette picker, 11/11 requirements met

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

## Current State

**Shipped:** v1.0 Modernization (2026-05-29) — 6 phases, 17 plans, ~24.7K LOC Kotlin.
DEPS → COMPLY → PERF → POLISH → SPINE → SETTINGS-V3 all complete and verified.
Build green; running on Galaxy S23+ (Android 16). Device UAT passed for Phases 2 & 6.

**Carried tech debt:** macrobench execution (Phase 3), formal visual screenshot audit (Phase 5),
COMPLY-07-3BTN, COMPLY-02-NAV-EVENT, WR-02 ViewModel interface refactor.

**Active milestone:** v1.1 AGP-9 Toolchain Modernization (DEPS-17) — COMPLETE (2026-05-31).
Landed the full AGP 9 / Gradle 9 / compileSdk 36 / Media3 1.10 toolchain cluster.
Kotlin 2.3.20 + KSP 2.3.9 + Hilt 2.59.2 + detekt 2.0.0-alpha.3. assembleDebug
passes locally (EdEC blocker resolved under AGP 9). Floating navbar fix + lifecycle
perf fix applied. All 4 phases complete.

**Remaining candidate themes (future):** background playback (BG-MEDIA / MediaSessionService),
measured perf validation pass (macrobench execution), Apollo 5 declarative-cache adoption,
CI assembleDebug gate promotion (pending CI runner confirmation).

---
*Last updated: 2026-05-30 — started v1.1 AGP-9 Toolchain Modernization milestone.*
