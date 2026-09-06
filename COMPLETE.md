# COMPLETE

Newest first.

## 2026-09-06 — Player battery/CPU round, reconciled onto the droid-builder snapshot

Recovered from `origin/wip/droid-builder-20260809` and layered on top. Written
and hand-reviewed, **not compiled** — see STATUS.md.

From the Aug-9 snapshot (kept as-is):
- Renderer ordering moved to a `BuildConfig` switch (`slopper.ffmpegRendererMode`,
  `on` | `prefer`), production defaulting to `on`, so the A/B is reproducible.
- `PlaybackDiagnostics.kt` — machine-readable logcat diagnostics (`SlopperPerf`),
  including thermal status; logs no URLs or titles.
- `tools/playback-ab/` — build + capture harness for the A/B measurement.
- `retryCurrent()` and error-recovery UI so a decoder failure is not a dead end.
- `requestHighestRefreshRate()` deleted from `MainActivity` — it forced 120 Hz
  app-wide.

Two defects found in the snapshot by hand review and fixed:
- `StashStreamAuthInterceptor` had lost its `endpoint != null` guard while still
  dereferencing `endpoint.baseUrl`. `StashEndpointProvider.current()` returns
  `StashEndpoint?`, so this could not have compiled — the snapshot was captured
  mid-edit. Guard restored, which also preserves the origin-scoping that keeps
  the ApiKey off cross-origin redirects.
- `PlaybackDiagnostics` has 24 methods against detekt's 20-per-class cap, with no
  baseline entry. Added `@Suppress("TooManyFunctions")` with a rationale.

Added this session:
- Pause playback on `Lifecycle.Event.ON_STOP`. Backgrounding the app left
  ExoPlayer decoding and streaming with nothing to draw on. `ON_STOP` rather than
  `ON_PAUSE` because a PiP activity stays STARTED, so PiP is unaffected.
- Position ticker idles at 1 Hz while paused instead of a flat 250 ms.
- Codec badge reports the live decoder from
  `AnalyticsListener.onVideoDecoderInitialized` instead of classpath presence —
  it previously read "HW+FF" while every frame was on the CPU. Pure
  `decoderBadge()` classifier + `CodecCapabilitiesTest`. Works in release builds
  with no adb, which `PlaybackDiagnostics` does not.

## Earlier work

Milestone v1.1 (AGP-9 toolchain modernization, phases 7–10) is complete, along
with the post-v1.1 UI rounds through `b03933d`. That history lives in
`.planning/STATE.md`, `.planning/MILESTONES.md`, and the git log.
