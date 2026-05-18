---
phase: 3
slug: perf-measured-wins
status: verified
threats_open: 0
threats_total: 10
threats_closed: 10
asvs_level: 1
created: 2026-05-19
audited: 2026-05-19
register_authored_at_plan_time: true
---

# Phase 3 — PERF: Security Threat Verification

> Per-phase security contract: threat register, accepted risks, and audit trail.
> register_authored_at_plan_time: true (all 3 PLAN.md files contain formal <threat_model> blocks)

---

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| Build system → emulator download | GMD triggers a system image download from Google servers | Build tool chain only; no project data |
| Convention plugin → compiler plugin classpath | `ComposeCompilerGradlePluginExtension` resolved at build time | Build classpath; no runtime data |
| Profile generator → device under test | UiAutomator calls exercise the APK | Test APK interactions; no sensitive data |
| ViewModel → Composable | `HomeUiState` / `ImmutableList<T>` crosses the VM-Compose boundary | Domain model types (titles, IDs); no credentials |
| AndroidView.update lambda → recomposition | Each Compose recomposition triggered the AndroidView update callback | Frame rate configuration call; no sensitive data |
| Benchmark APK → installed app APK | `BaselineProfileMode.Require` requires embedded profile | Benchmark timing data; no PII |
| Human → profiling session | Shuffle profiling and macrobench execution require device access | Performance metrics; no secrets |

---

## Threat Register

| Threat ID | Category | Component | Disposition | Mitigation | Status |
|-----------|----------|-----------|-------------|------------|--------|
| T-03-01 | Tampering | GMD system image download | accept | Per AR-03-01: REVIEWS-C4 ACCEPT path if `google_apis` image unavailable; GMD declaration (PERF-01) lands regardless. Fallback: `systemImageSource = "aosp"`. | closed |
| T-03-02 | Denial of Service | `stabilityConfigurationFile` missing | mitigate | `compose_stability.conf` created at project root before `compileReleaseKotlin`. Verified: `ls compose_stability.conf` → exists (commit `38d2bd6`). | closed |
| T-03-03 | Denial of Service | Profile journey failures (no Stash server) | accept | Each journey guarded with null-safe checks; failed journey produces shorter profile, not test failure. UiAutomator `?.` patterns throughout `StashBaselineProfileGenerator.kt`. | closed |
| T-03-04 | Tampering | ImmutableList compile boundary | mitigate | Build verification (`assembleDebug`) mandatory after each migration step. Type mismatches at call sites are compile errors (not runtime). Build green at `3daa87e`. All `List<T>` UiState fields migrated. | closed |
| T-03-05 | Denial of Service | `applyVideoFrameRate` timing change | mitigate | `LaunchedEffect(state.videoFrameRate)` fires on state change — same trigger semantics as before. Null-safe `playerView?.let { }` prevents NPE on first-frame race. Build green. | closed |
| T-03-06 | Tampering | Missing `.toPersistentList()` at List→ImmutableList boundary | mitigate | PERF-03 verify block: `grep -rn 'List<SceneSummary>\|List<HomeRail>\|List<Marker>' feature/` → 0 hits. All call sites confirmed migrated. | closed |
| T-03-07 | Denial of Service | Shuffle fix regression | mitigate | Fix committed: `onSceneEnded()` emits "End of queue" banner on null advance. `./gradlew :feature:player:assembleDebug` green at `ded7167`. Runtime verification deferred to end-of-milestone (UAT-DEFERRED.md §PERF-08). | closed |
| T-03-08 | Denial of Service | `BaselineProfileMode.Require` benchmark abort if no profile | mitigate | Plans 3.2 and 3.3 both `depends_on: plan-3.1` which contains PERF-05 (profile expansion). Wave ordering enforces plan-3.1 completes before benchmarks are executed. KDoc in benchmark classes documents dependency. | closed |
| T-03-09 | Information Disclosure | Benchmark output files committed to git | accept | `.planning/benchmarks/` files contain only performance timing metrics — no PII, no secrets, no credentials. Committing is required by PERF-10 acceptance criteria (D-08). | closed |
| T-03-10 | Repudiation | Performance claims without backing files | mitigate | `03-UAT-DEFERRED.md` explicitly blocks PR merge until benchmark files exist or `REVIEWS-C4-ACCEPT.md` documents GMD failure. No "feels faster" claims in any plan or summary. PERF-10 gate enforced. | closed |

*Status: open · closed*
*Disposition: mitigate (implementation required) · accept (documented risk) · transfer (third-party)*

---

## Mitigation Evidence

### T-03-02 — `compose_stability.conf` created (verified)
```
ls /home/yun/slopper/compose_stability.conf → exists (empty file, 0 bytes)
# Note: must be empty — comment lines cause parse errors in KGP 2.2.20
# Commit 38d2bd6 created the file and confirmed KGP accepts it
```

### T-03-04/T-03-06 — ImmutableList migration complete (verified)
```
grep -rn 'List<SceneSummary>\|List<HomeRail>\|List<Marker>' feature/ → 0 hits
./gradlew :feature:home:assembleDebug :feature:player:assembleDebug → BUILD SUCCESSFUL
```

### T-03-05 — applyVideoFrameRate in LaunchedEffect (verified)
```
grep -n 'applyVideoFrameRate' feature/player/.../PlayerScreen.kt
→ line 228: inside LaunchedEffect(state.videoFrameRate) { ... }
→ NOT inside AndroidView(update = { ... }) — confirmed
```

### T-03-07 — Shuffle fix (verified)
```
grep -n 'End of queue\|queue.advance.*return' feature/player/.../PlayerViewModel.kt → hit
./gradlew :feature:player:assembleDebug → BUILD SUCCESSFUL
```

---

## Accepted Risks Log

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|-------------|------|
| AR-03-01 | T-03-01 | GMD `google_apis` system image may not download on dev host due to memory/network constraints; fallback is `aosp` or REVIEWS-C4 ACCEPT for PERF-06/07. GMD declaration code change (PERF-01) lands regardless. | Plan author (03.1-PLAN.md AR-03-01); 03-CONTEXT.md AR-03-01 | 2026-05-18 |
| AR-03-02 | T-03-03 | Profile journey failures due to missing Stash server connection are expected on CI/clean hosts; profile is best-effort. Shorter profile is better than no profile. | Plan author (03.1-PLAN.md T-03-03 accept) | 2026-05-18 |
| AR-03-03 | T-03-09 | Benchmark output files contain only timing metrics; no sensitive data. PERF-10 requires them committed as evidence. | Plan author (03.3-PLAN.md T-03-09 accept) | 2026-05-18 |

---

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-05-19 | 10 | 10 | 0 | Claude (gsd-secure-phase, register_authored_at_plan_time: true) |

---

## Sign-Off

- [x] All 10 threats have a disposition (mitigate / accept)
- [x] Accepted risks documented in Accepted Risks Log (3 accepted)
- [x] `threats_open: 0` confirmed
- [x] `status: verified` set in frontmatter
