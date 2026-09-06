# TODO

## Active — verification

- [ ] **Push `perf/playback-power` and open a PR.** CI is the only available gate.
      Highest-risk items, all unverifiable locally: the `@OptIn(UnstableApi::class)`
      placement on a `by lazy` property (lint-enforced, not compiler-enforced);
      whether `@Suppress("TooManyFunctions")` satisfies detekt on
      `PlaybackDiagnostics`; and whether the new `buildConfigField` wiring
      survives the configuration cache.
- [ ] **Device UAT on the Galaxy S23+.** Play a plain H.264 scene and read the
      codec badge: it must say `HW`. `SW·FF` means the renderer ordering
      regressed. Then background the app mid-playback (must pause) and enter PiP
      (must keep playing).
- [ ] **Run the A/B harness** in `tools/playback-ab/` now that both renderer
      orders are buildable, and record real numbers rather than inferring the win.

## Deferred

- [ ] `setTunnelingEnabled(true)` was almost certainly inert while the FFmpeg
      software renderer was winning (tunneling requires MediaCodec). It is
      reachable for the first time now. Watch UAT for black frames or A/V drift;
      drop the flag if any appear.
- [ ] `PlayerScreen` collects `position` at the composable root, restarting the
      body 4x/sec during playback. Minor next to the decode fix. Revisit only if
      a trace shows it.
- [ ] Decide whether `requestHighestRefreshRate()` (deleted from `MainActivity`
      by the snapshot) should come back behind a setting — it forced 120 Hz
      app-wide, which is why it went, but it was added for a reason.
