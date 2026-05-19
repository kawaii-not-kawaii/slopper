# Phase 3 PERF — Deferred UAT

**Status:** DEFERRED — awaiting end-of-milestone device testing session
**Deferred by:** D-09 (Plan 3.3 human checkpoint per CONTEXT.md)
**Acceptance gate:** PERF-06, PERF-07, PERF-08 outputs required before Phase 3 PR is merged
**Created:** 2026-05-18

---

## Infrastructure Ready (automated tasks complete)

- [x] `ColdStartBenchmark.kt` created and compiles — `baselineprofile/src/main/java/io/stashapp/android/baselineprofile/ColdStartBenchmark.kt`
- [x] `LibraryScrollBenchmark.kt` created and compiles — `baselineprofile/src/main/java/io/stashapp/android/baselineprofile/LibraryScrollBenchmark.kt`
- [x] GMD declared in `baselineprofile/build.gradle.kts` (plan-3.1 PERF-01)
- [x] Baseline profile expanded to 4 journeys (plan-3.1 PERF-05)
- [x] PERF-08 root cause identified: silent queue exhaustion with `RepeatMode.OFF` — fix committed (PlayerViewModel.kt `onSceneEnded()` now emits "End of queue" banner)
- [ ] Benchmark execution (deferred)
- [ ] Benchmark output files committed (deferred)

---

## PERF-06 — Cold-Start Macrobench

**Command:**
```bash
cd /home/yun/slopper

# With GMD (preferred — requires Pixel 6 API 34 system image download):
./gradlew :baselineprofile:pixel6Api34BenchmarkReleaseAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=io.stashapp.android.baselineprofile.ColdStartBenchmark \
  2>&1 | tee .planning/benchmarks/perf-06-cold-start.txt

# Alternative — with connected physical device + USB debugging:
./gradlew :baselineprofile:connectedBenchmarkReleaseAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=io.stashapp.android.baselineprofile.ColdStartBenchmark \
  2>&1 | tee .planning/benchmarks/perf-06-cold-start.txt
```

**Acceptance criteria:**
- `.planning/benchmarks/perf-06-cold-start.txt` exists and is non-empty (>= 5 lines)
- Contains p50 values for both `startupWithProfile` and `startupWithoutProfile`
- Ratio: `p50(startupWithProfile) / p50(startupWithoutProfile)` >= 1.05 (5% improvement)

**Device requirement:** GMD (pixel6Api34) or physical device with USB debugging enabled
**Prerequisite:** Run `:app:generateBaselineProfile` first — `startupWithProfile` uses
`BaselineProfileMode.Require` which ABORTS if the APK has no embedded profile.

**Fallback:** If Pixel 6 API 34 system image cannot download:
- Create `.planning/benchmarks/REVIEWS-C4-ACCEPT.md` documenting the failure reason
- Commit it: `git add .planning/benchmarks/REVIEWS-C4-ACCEPT.md && git commit -m "docs(perf): REVIEWS-C4-ACCEPT — GMD image download failed"`
- PERF-06 and PERF-07 are formally deferred per AR-03-01
- PERF-01 (GMD declaration) remains SATISFIED — the code change landed

---

## PERF-07 — Library Scroll Frame-Timing Macrobench

**Command:**
```bash
cd /home/yun/slopper
./gradlew :baselineprofile:pixel6Api34BenchmarkReleaseAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=io.stashapp.android.baselineprofile.LibraryScrollBenchmark \
  2>&1 | tee .planning/benchmarks/perf-07-library-scroll.txt
```

**Acceptance criteria:**
- `.planning/benchmarks/perf-07-library-scroll.txt` exists and contains p95 frame time
- >= 95% of frames on time at p95 (frame time <= 16.67ms at 60Hz / <= 8.33ms at 120Hz)

**Device requirement:** Same as PERF-06
**Fallback:** Same as PERF-06 (same REVIEWS-C4-ACCEPT.md if GMD unavailable)

---

## PERF-08 — Live Shuffle Profiling Session

**Steps:**
1. `./gradlew :app:installDebug`
2. Open the Slopper app on a device
3. Navigate to any scene → tap Play
4. In the player, enable Shuffle; ensure Repeat mode is OFF (default)
5. Let all scenes in the queue play through to the natural end (do NOT skip)
6. Observe: after the last scene ends, a **"End of queue"** banner should appear briefly
7. Optional — heap dump: open Android Studio Profiler > Memory > Record heap dump while
   player is active; search for `PlayerListener` instances — expected count: **1**
8. Save profiler screenshot or exported heap dump to:
   `.planning/benchmarks/perf-08-shuffle-profile.{png|txt}`

**Acceptance criteria:**
- `.planning/benchmarks/perf-08-shuffle-profile.{png|txt}` committed
- "End of queue" banner visible when queue exhausted with RepeatMode.OFF
- `PlayerListener` instance count = 1 in heap dump (no accumulation)
- Summary documents actual root cause with class/line reference
  (already documented in `.planning/benchmarks/perf-08-shuffle-investigation.md`)

---

## Commit After UAT

After all benchmark output files are in place:

```bash
cd /home/yun/slopper
git add .planning/benchmarks/
git commit -m "docs(perf): PERF-10 — Benchmark outputs committed (.planning/benchmarks/)"
```

---

## PERF-10 Gate

Before merging the Phase 3 PR, verify ALL of the following:

- [ ] `.planning/benchmarks/perf-06-cold-start.txt` exists (>= 5 lines) OR `REVIEWS-C4-ACCEPT.md` documents the GMD failure
- [ ] `.planning/benchmarks/perf-07-library-scroll.txt` exists with p95 data OR same REVIEWS-C4 deferral
- [ ] `.planning/benchmarks/perf-08-shuffle-profile.{png|txt}` committed
- [ ] PR description cites specific file paths for every performance claim
- [ ] No PR bullet point says "feels faster" or "should be faster" without a benchmark file reference
- [ ] `PlayerListener` instance count confirmed = 1 in heap dump
