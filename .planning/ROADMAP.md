# Roadmap: Slopper Android Modernization

## Milestones

- ✅ **v1.0 Modernization** — Phases 1–6 (shipped 2026-05-29) — full detail in [`milestones/v1.0-ROADMAP.md`](milestones/v1.0-ROADMAP.md)
- 🔄 **v1.1 AGP-9 Toolchain Modernization (DEPS-17)** — Phases 7–10 (started 2026-05-30)

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

### 🔄 v1.1 AGP-9 Toolchain Modernization (DEPS-17)

- [x] **Phase 7: GRADLE-9 — Gradle-9 Readiness + Deprecation Sweep (AGP-8.7.3 fold-forward)** (2/2 plans, completed 2026-05-30) - Gradle 9.4.1 target PINNED (not flipped — AGP 8.7.3 hard-fails on Gradle 9), deprecations swept on the current toolchain, every plugin's Gradle-9 verdict recorded, detekt decision made; the live wrapper activation folds forward to Phase 8 commit 1
- [ ] **Phase 8: AGP-9 — Atomic Build-Logic Migration + compileSdk 36** - The indivisible core: Gradle 9.4.1 activation (commit 1) + AGP 9.2.1 + built-in Kotlin + `CommonExtension` fix + `compilerOptions` + Hilt 2.59.2 + compileSdk 36, build green across all ~14 modules
- [ ] **Phase 9: LIBS — Green-Gated Library Bumps** - Media3/nextlib 1.10.0 locked pair + activity-compose 1.13 + core-ktx 1.18, device software-codec playback verified
- [ ] **Phase 10: CI-SIGNING — Isolated Assemble/Signing Probe** - Non-gating `assembleDebug` probe on AGP-9 runners; promote a real gate or document the EdEC blocker persisting

## Phase Details

### Phase 7: GRADLE-9 — Gradle-9 Readiness + Deprecation Sweep
**Goal**: The Gradle-9 transition is fully DE-RISKED before the AGP-9 surgery: the Gradle 9.4.1 target is pinned, every Gradle-9 deprecation is enumerated and attributed on the current toolchain, every toolchain plugin has a recorded Gradle-9 verdict (incl. the one open detekt decision), and the KGP floor is confirmed — WITHOUT flipping the live wrapper. (Re-scoped 2026-05-30: research proved AGP 8.7.3 hard-fails on Gradle 9.4.1, so a green-on-Gradle-9 build is impossible until AGP 9 lands; the wrapper ACTIVATION folds forward to Phase 8 commit 1. See `07-RESEARCH.md` + `docs/adr/0001-gradle-9-readiness-and-agp8-fold-forward.md`.)
**Depends on**: Nothing (first phase of v1.1; builds on the shipped v1.0 state)
**Requirements**: AGP9-01 (PARTIALLY delivered — readiness/pin/sweep/decision here; green-on-Gradle-9 activation asserted at the Phase-8 gate per fold-forward)
**Success Criteria** (what must be TRUE):
  1. `docs/adr/0001-gradle-9-readiness-and-agp8-fold-forward.md` records the pinned Gradle 9.4.1 target (distribution URL confirmed live; `distributionSha256Sum` fetched at Phase-8 execution, never hard-coded), and the live `gradle-wrapper.properties` STILL resolves Gradle 8.11.1 (the flip is Phase-8 commit 1).
  2. `./gradlew help --warning-mode=all` was run on Gradle 8.11.1 and every Gradle-9 deprecation is enumerated + plugin-attributed in `gradle9-deprecations.log` (or an explicit no-finding is recorded); each is resolved or explicitly accepted with a documented reason.
  3. detekt, ktlint, OWASP dependency-check, and the baseline-profile plugin each have a recorded Gradle-9 verdict in the ADR (ktlint 14.2.0 ready; dependency-check 12.2.2 via `--no-configuration-cache`; baseline-profile 1.4.1 verified/not-exercised; detekt 1.23.8 = keep stable, accept warnings, empirical test deferred to Phase 8, NO alpha).
  4. Kotlin 2.2.20 / KSP 2.2.20-2.0.4 are recorded as already satisfying AGP 9's KGP ≥ 2.2.10 floor — no Kotlin/KSP bump is needed.
  5. The repo's own build scripts are confirmed Gradle-9-clean and the CI gate (`compileDebugSources detekt ktlintCheck test`) is still green on Gradle 8.11.1 — zero regression.
**Plans**: 2 plans (2/2 complete)
  - [x] 07.1-PLAN.md — Deprecation sweep + clean-script audit + no-regression baseline on Gradle 8.11.1 (produces `gradle9-deprecations.log`)
  - [x] 07.2-PLAN.md — Gradle-9 readiness ADR: pinned target, plugin verdicts, detekt decision, KGP floor, Phase-8 fold-forward hand-off (`docs/adr/0001-…md`)
**Research flag (RESOLVED)**: OWASP dependency-check 12.2.2 runs on Gradle 9 via the already-present `--no-configuration-cache` contract. The "AGP 8.7.3 on Gradle 9 for one isolating commit" hypothesis is DISPROVEN — AGP 8.7.3 hard-fails the Gradle-version check; the phase folds forward into Phase 8 as designed by the ROADMAP escape clause.

### Phase 8: AGP-9 — Atomic Build-Logic Migration + compileSdk 36
**Goal**: The atomic critical path lands as one indivisible change-set — Gradle 9.4.1 activation (commit 1, folded forward from Phase 7), AGP 9.2.1, built-in Kotlin, the `CommonExtension` generics fix, `kotlinOptions`→`compilerOptions`, Hilt 2.59.2, and compileSdk 36 — so the `build-logic/convention` choke point configures all ~14 modules and the build is green again on AGP 9.
**Depends on**: Phase 7 (Gradle 9 target pinned + deprecations swept + plugin verdicts recorded before the AGP-9 DSL surgery; the wrapper flip is Phase-8 commit 1)
**Requirements**: AGP9-02, AGP9-03, SDK-01 (+ the green-on-Gradle-9 assertion of AGP9-01 folded forward from Phase 7)
**Success Criteria** (what must be TRUE):
  1. AGP resolves to 9.2.1 and `compileDebugSources` is green across all ~14 modules with the version-isolation opt-out flags REMOVED.
  2. `org.jetbrains.kotlin.android` is removed from every application site (both convention plugins, root + baselineprofile blocks, catalog alias), `CommonExtension<*,…>` generics are gone, and `kotlinOptions{}` is migrated to `kotlin{compilerOptions{}}` — no "extension already registered" or generics compile error.
  3. Hilt/Dagger resolves to exactly 2.59.2 (never bare "2.59+"), and Hilt + Apollo KSP codegen produces a working DI graph on Kotlin 2.2.20 / KSP 2.2.20-2.0.4.
  4. `compileSdk` is 36 in all 3 touchpoints (`KotlinAndroid.kt` + `baselineprofile`) AND `targetSdk = 35` is verifiably still explicit in every site — the app does NOT silently opt into Android-16 runtime behavior.
  5. The full phase gate passes: `compileDebugSources + detekt + ktlintCheck + test` green, and the CI cache key is bumped `agp8`→`agp9`.
**Plans**: TBD
**Research flag**: Confirm the three flagged version disagreements (AGP **9.2.1 vs 9.2.0**, Gradle floor matching the chosen AGP minor, Hilt **2.59.2 vs 2.59.1**) against live Maven metadata at plan time; verify `KotlinAndroidProjectExtension` still resolves under built-in Kotlin and that the `kotlin{compilerOptions{}}` rewrite is exact. Migrate one concern per commit (for `git bisect`); validate `:core:common:compileDebugKotlin` before a full build. The Gradle 9.4.1 wrapper flip + live sha256 fetch is commit 1 (folded forward from Phase 7).

### Phase 9: LIBS — Green-Gated Library Bumps
**Goal**: The now-unblocked library cluster lands as separable green-gated bumps on top of the AGP-9 build — Media3/nextlib as a single locked pair and the two leaf libs — changing only which versions resolve, not whether the build configures.
**Depends on**: Phase 8 (compileSdk 36 must precede the Media3/leaf bumps — the AAR `minCompileSdk` consumer rule rejects SDK-35 modules consuming SDK-36 libraries)
**Requirements**: LIB-01, LIB-02
**Success Criteria** (what must be TRUE):
  1. Media3 resolves to 1.10.0 and `nextlib-media3ext` to 1.10.0-0.12.1 as a single locked pair (NOT 1.10.1 — no nextlib pairing exists), and the build is green.
  2. Software-codec (FFmpeg renderer) playback is verified working on a physical device — no `UnsatisfiedLinkError` at runtime.
  3. activity-compose resolves to 1.13 and core-ktx to 1.18, with the build green.
  4. The corresponding `androidx.activity:*` / `androidx.core:core*` entries are removed from the Dependabot ignore list.
**Plans**: TBD
**Research flag**: Lighter research — Media3/nextlib lockstep and leaf-lib bumps are well-documented mechanical bumps. The only nuances are the matching version pair (verified `1.10.0-0.12.1`) and the device smoke test (the failure surface is runtime-only).

### Phase 10: CI-SIGNING — Isolated Assemble/Signing Probe
**Goal**: The deferred CI APK-signing question is re-evaluated under the AGP-9 toolchain in full isolation — a non-gating probe whose outcome is acted on — without ever risking the green toolchain landing.
**Depends on**: Phase 8 (needs the green AGP-9 build to probe against); intentionally LAST and isolated so it can never gate the toolchain landing
**Requirements**: CI-01
**Success Criteria** (what must be TRUE):
  1. An `assembleDebug`/`validateSigningDebug` probe runs on the AGP-9 CI runners as `continue-on-error` — it cannot fail the pipeline.
  2. The probe outcome is acted on: EITHER a demonstrated green run promotes a real assemble/signing gate, OR the BouncyCastle EdEC blocker is documented as persisting and the compile-only gate is kept as the contract (with a `bcprov-jdk18on` pin if assembly is forced).
  3. The compile-only CI gate remains the binding contract throughout — `assembleDebug` is never restored "on faith".
**Plans**: TBD
**Research flag**: EdEC/bcprov spike — capture the full stack trace on an AGP-9 runner before deciding on a gate. The toolchain bump does NOT fix this classpath skew and may regress it.

## Progress

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 1. DEPS | v1.0 | 3/3 | Complete | 2026-05-17 |
| 2. COMPLY | v1.0 | 2/2 | Complete | 2026-05-18 |
| 3. PERF | v1.0 | 3/3 | Complete | 2026-05-19 |
| 4. POLISH | v1.0 | 3/3 | Complete | 2026-05-19 |
| 5. SPINE | v1.0 | 3/3 | Complete | 2026-05-19 |
| 6. SETTINGS-V3 | v1.0 | 3/3 | Complete | 2026-05-29 |
| 7. GRADLE-9 | v1.1 | 2/2 | Complete | 2026-05-30 |
| 8. AGP-9 | v1.1 | 0/? | Not started | - |
| 9. LIBS | v1.1 | 0/? | Not started | - |
| 10. CI-SIGNING | v1.1 | 0/? | Not started | - |

## Carried Tech Debt (into next milestone)

- **Macrobenchmark execution + output capture** (Phase 3) — infrastructure ready, device measurement deferred (tracked as PERF-MB-01)
- **Formal per-screen visual-fidelity screenshot audit** (Phase 5) — SPINE UI confirmed on S23+ via Phase 6 device UAT
- **COMPLY-07-3BTN** (3-button-nav re-verification), **COMPLY-02-NAV-EVENT** (NavigationBackHandler migration)
- **WR-02** (Phase 6 review): SettingsViewModel domain-interface refactor — requires moving companion constants to interfaces
- **APOLLO-CACHE-01**: Apollo 5 declarative normalized cache — only if a caching need emerges

## Out of Scope (carried)

| Item | Reason |
|------|--------|
| New end-user features | Modernization-only milestone |
| `minSdk` / `targetSdk` bump | Must stay installable + behaviorally unchanged on existing devices (Android-16 opt-ins forbidden) |
| Kotlin past 2.2.20 / KSP past 2.2.20-2.0.4 | AGP 9 needs only KGP ≥ 2.2.10; bumping re-triggers KSP/Hilt churn for no gain |
| Media3 1.10.1 | No matching `nextlib-media3ext` 1.10.1 build exists — hard cap at 1.10.0 |
| `android.enableLegacyVariantApi` / `android.newDsl=false` | No-op / doomed crutches; repo is already clean — must NOT be added |
| New third-party SDKs | No vendor additions without explicit approval |
| Module-graph restructure | Frozen — refactors stay within modules |
| Migrating off Compose / Hilt / Gradle Kotlin DSL | Existing architecture preserved |
| Real `MediaSessionService` (background playback) | Deferred to BG-MEDIA milestone |
| Apollo 5 / Nav3 migrations | Each deserves its own milestone |
