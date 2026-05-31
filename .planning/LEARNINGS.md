# Learnings — v1.1 AGP-9 Toolchain Modernization

## Decisions

1. **AGP 9 enforces consumer-rules.pro existence** — Previously missing files were silently ignored; AGP 9 now fails the build. Fix: create empty consumer-rules.pro in all library modules.

2. **EdEC/BouncyCastle blocker resolved under AGP 9** — The `assembleDebug` probe passes locally. The blocker was likely AGP-version-specific, not a fundamental CI environment issue.

3. **Floating navbar requires overlay pattern** — Scaffold's bottomBar slot applies its own background color. For a truly floating pill navbar, the bar must be a Box overlay outside the Scaffold.

4. **collectAsState should be collectAsStateWithLifecycle** — collectAsState doesn't respect lifecycle, wasting resources when the app goes to background. Always use collectAsStateWithLifecycle for ViewModel flows.

5. **Forgejo CI needs parity with GitHub CI** — Cache keys, wrapper validation, concurrency cancellation, --no-daemon flags, and test report upload should be identical across both CI platforms.

## Lessons

1. **AGP major version bumps have hidden enforcement changes** — The consumer-rules.pro enforcement wasn't documented prominently. Always test assembleDebug after an AGP major bump.

2. **Scaffold's bottomBar is not truly floating** — It draws a surface-colored background behind the bar. For floating designs, use Box overlay.

3. **Proguard file presence is now enforced** — AGP 9 treats missing proguard references as build failures, not warnings.

4. **CI workflow drift is easy** — The Forgejo CI was still on agp8 cache key and missing --no-daemon flags. Regular parity checks are needed.

## Patterns

1. **Overlay pattern for floating UI** — Use Box + Alignment.BottomCenter instead of Scaffold.bottomBar for floating elements.

2. **Lifecycle-aware collection** — Always use collectAsStateWithLifecycle for ViewModel StateFlows in Compose.

3. **Non-gating CI probes** — Use continue-on-error for experimental steps that need CI runner validation.

4. **Empty proguard files** — Create consumer-rules.pro as empty files in library modules; they serve as documentation placeholders.

## Surprises

1. **assembleDebug passes locally under AGP 9** — The EdEC blocker was expected to persist. This needs CI runner confirmation.

2. **40MB classes.dex** — The APK's DEX file is large due to the full dependency tree (Hilt, Apollo, Media3, Compose). R8/ProGuard handles this for release builds.

3. **FFmpeg native libs are included** — The debug APK includes all FFmpeg .so files (avcodec, avutil, swresample, swscale) for software codec playback.

4. **14 modules needed consumer-rules.pro** — All core/* and feature/* modules needed the file, not just the ones with proguard rules.
