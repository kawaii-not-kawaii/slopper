# Verification Report — v1.1 Milestone Completion

**Date:** 2026-05-31
**Milestone:** v1.1 — AGP-9 Toolchain Modernization
**Verdict:** PASS

## Build Gate Results

| Gate | Command | Result |
|------|---------|--------|
| Compile | `compileDebugSources` | ✅ BUILD SUCCESSFUL |
| Static analysis | `detekt ktlintCheck` | ✅ BUILD SUCCESSFUL |
| Unit tests | `test` | ✅ BUILD SUCCESSFUL |
| APK assembly | `assembleDebug` | ✅ BUILD SUCCESSFUL |

**Total tasks:** 729 (all up-to-date after initial run)

## APK Verification

| Check | Result |
|-------|--------|
| classes.dex present | ✅ 40MB |
| FFmpeg native libs | ✅ libavcodec.so, libavutil.so, libswresample.so, libswscale.so |
| media3ext native lib | ✅ libmedia3ext.so |
| Graphics path lib | ✅ libandroidx.graphics.path.so |
| DataStore lib | ✅ libdatastore_shared_counter.so |
| ABI splits | ✅ arm64-v8a + armeabi-v7a (32MB each) |

## Phase 10 CI-SIGNING

**EdEC/BouncyCastle blocker:** RESOLVED under AGP 9. `assembleDebug` passes locally with BUILD SUCCESSFUL. The `continue-on-error` probe in CI will confirm whether CI runners also pass.

**Changes made:**
1. Added non-gating `assembleDebug` probe to both CI workflows (Forgejo + GitHub)
2. Updated Forgejo CI to agp9 cache key + parity with GitHub CI
3. Created missing `consumer-rules.pro` in 14 library modules (AGP 9 enforcement)

## Floating Navbar Fix

**Issue:** Scaffold's bottomBar slot applies a `surfaceContainerLow` background strip behind the floating pill navbar.

**Fix:** Moved MainBottomBar outside the Scaffold into a Box overlay. The pill now floats directly over content with no background bleed.

## Performance Fix

**Issue:** Two `collectAsState` usages in MainActivity didn't respect lifecycle.

**Fix:** Switched to `collectAsStateWithLifecycle` for proper lifecycle-aware collection.

## Commits

| Commit | Description |
|--------|-------------|
| c34a3ea | ci(signing): add non-gating assembleDebug probe + update Forgejo CI to agp9 |
| d1c6b63 | fix(build): create missing consumer-rules.pro for AGP 9 compat |
| f6c8015 | fix(ui): make bottom navbar truly floating — remove Scaffold background strip |
| 42da7d0 | perf(ui): switch collectAsState to collectAsStateWithLifecycle |
| be59251 | docs: update STATE.md + ROADMAP.md — v1.1 milestone complete |

## Code Review

Extensive code review in progress (agent running). Will update with findings.
