# PERF-08 Shuffle Playback Hang — Investigation

**Date:** 2026-05-18
**Investigator:** Claude (automated static analysis)
**Status:** Root cause identified and fix applied; live profiling deferred to end-of-milestone

---

## Hypothesis Tested

Primary: Queue exhaustion — `queue.advance()` returns null at last shuffled item,
`onSceneEnded()` returns without scheduling the next scene and without any user feedback.

---

## Findings

### Listener Accumulation Check

- `addListener` call count in `PlayerViewModel`: **1** (inside `player` lazy init block, line 101)
- `removeListener` call count in `PlayerViewModel`: **1** (`onCleared()`, line 446)
- Conclusion: **CLEARED** — no listener accumulation possible. One listener added per
  ViewModel instance, removed on clear. `PlayerViewModel` is scoped to `NavBackStackEntry`
  via `hiltViewModel()` so it is recreated on each navigation to `Routes.Player`.

### Queue Exhaustion Check

- `onSceneEnded()` null-advance handling (before fix):

  ```kotlin
  private fun onSceneEnded() {
      val next = queue.advance() ?: return   // <-- silent return when queue exhausted
      loadAndPlay(next, autoResume = false)
  }
  ```

  `PlayerViewModel.kt` line 370–373 (pre-fix).

- `PlayerQueue.advance()` behavior (`PlayerQueue.kt` lines 49–62):

  ```kotlin
  fun advance(): String? {
      if (repeatMode == RepeatMode.ONE) return activeOrder.getOrNull(currentIndex)
      val nextIdx = currentIndex + 1
      if (nextIdx > activeOrder.lastIndex) {
          return if (repeatMode == RepeatMode.ALL) {
              currentIndex = 0
              activeOrder.firstOrNull()    // wraps correctly for RepeatMode.ALL
          } else {
              null                         // returns null for RepeatMode.OFF
          }
      }
      currentIndex = nextIdx
      return activeOrder[nextIdx]
  }
  ```

- **RepeatMode.ALL wrapping**: ALREADY PRESENT in `PlayerQueue.advance()` — wrapping
  logic was correct. The queue wraps to index 0 when `RepeatMode.ALL` is active.

- **Root cause confirmed**: The default `repeatMode` is `RepeatMode.OFF`
  (`PlayerQueue.kt` line 98). When the user has not explicitly toggled repeat, the
  queue stops at the last item. `onSceneEnded()` received `null` from `advance()` and
  returned silently — **no banner, no state update, no indication to the user**.
  Result: the player appears frozen ("hang") because the last video ends and nothing
  visibly happens.

- Conclusion: **ROOT CAUSE CONFIRMED** — silent null return in `onSceneEnded()` when
  queue exhausted with `RepeatMode.OFF`.

---

## Fix Applied

**File:** `feature/player/src/main/java/io/stashapp/android/feature/player/PlayerViewModel.kt`

**Change:** `onSceneEnded()` now emits a `"End of queue"` banner when `advance()` returns
null, giving the user clear visual feedback that playback stopped intentionally. The banner
auto-clears after 2500 ms (same timing as shuffle/repeat banners).

```kotlin
// Before:
private fun onSceneEnded() {
    val next = queue.advance() ?: return
    loadAndPlay(next, autoResume = false)
}

// After:
private fun onSceneEnded() {
    val next = queue.advance()
    if (next == null) {
        // Queue exhausted with RepeatMode.OFF — emit a banner so the user knows
        // playback stopped intentionally rather than silently "hanging".
        _state.update { it.copy(banner = "End of queue") }
        clearBannerLater()
        return
    }
    loadAndPlay(next, autoResume = false)
}
```

**Build verification:** `./gradlew :feature:player:assembleDebug` → BUILD SUCCESSFUL

**Why no structural refactor:**
- `PlayerQueue.advance()` already handles `RepeatMode.ALL` wrap correctly — no change
  needed there.
- The fix is minimal and confined to `PlayerViewModel.kt`. No new methods added to
  `PlayerQueue`.
- AR-03-02 constraint (no large ViewModel restructuring in this plan) is satisfied.

---

## Deferred Items

- **Live heap profiling:** Android Studio Profiler heap dump during a 10-video shuffle
  session is deferred to the end-of-milestone human testing session.
  Artifact target: `.planning/benchmarks/perf-08-shuffle-profile.{png|txt}`

---

## Reproduction Steps (for human verification at Task 3 checkpoint)

1. Install debug build: `./gradlew :app:installDebug`
2. Open the Slopper app → navigate to a scene → tap Play
3. In the queue, enable shuffle, ensure `RepeatMode` is **OFF** (default)
4. Let all scenes in the queue play through to completion without skipping
5. Expected (with fix applied): after the last scene ends, a **"End of queue"** banner
   appears briefly — no apparent "hang"
6. Expected (RepeatMode.ALL): playback wraps to the first item automatically (no change
   needed — this was already working)
7. Optional — heap dump: open Android Studio Profiler → Memory → record heap dump
   while player is active; search for `PlayerListener` instances — expected count: **1**

---

## Evidence Files

- This file: static code analysis (2026-05-18)
- `feature/player/src/main/java/io/stashapp/android/feature/player/PlayerViewModel.kt` — fix applied
- `feature/player/src/main/java/io/stashapp/android/feature/player/PlayerQueue.kt` — read-only, no change
- Profiling artifact: to be committed by human at Task 3 checkpoint
  (`.planning/benchmarks/perf-08-shuffle-profile.{png|txt}`)
