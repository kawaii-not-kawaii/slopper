# STATUS

**Branch point:** `b03933d` (master HEAD; master has not moved)
**Working branch:** `perf/playback-power`, based on `origin/wip/droid-builder-20260809`
**Current work:** Player battery/CPU round — hardware video decode + power fixes.

## What happened this session

The Aug-9 battery/CPU round was not lost. It lives on
`origin/wip/droid-builder-20260809` (`82ba70f`), a verbatim snapshot of the
uncommitted working tree on the **droid-builder** host, taken before that host
was decommissioned. That is why it appears on no live tailnet machine
(checked: glmdev, slopcoder, and nine others have no slopper checkout at all;
birdy rejects this machine's SSH key).

That branch is master + 2 commits with zero divergence, so it applied cleanly.

## The root cause (found twice, independently)

`StashPlayerFactory` shipped `EXTENSION_RENDERER_MODE_PREFER`. Per upstream
`DefaultRenderersFactory`: `_ON` indexes extension renderers **after** the core
MediaCodec ones, `_PREFER` indexes them **before**. nextlib ships H.264/HEVC/
VP8/VP9 software decoders, so ordinary video decoded on the CPU while the
hardware decoder sat idle. Production is now `on`.

## Blocker: nothing has been compiled

No JDK, no Android SDK, no Gradle cache on this machine — `./gradlew` fails
immediately. Everything is hand-reviewed only. Verification goes through
`.github/workflows/ci.yml` (compileDebugSources, detekt, ktlintCheck, test,
assembleDebug on pull_request).

Two defects were already caught by hand review of the snapshot; see COMPLETE.md.
