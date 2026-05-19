# Codebase Concerns

**Analysis Date:** 2026-05-16

This is a freshly-restored Android Kotlin project (Stash client) with an MVP scaffold. The codebase is reasonably well-structured (modular Gradle, Hilt DI, Apollo GraphQL, Media3), but a number of items either are explicitly stubbed, were lost in the restoration, or pose risk for the first signed release.

## Tech Debt

### No `.gitignore` at the repo root
- Issue: There is no `.gitignore` anywhere in the project. The repository will happily track Gradle's `build/`, `.gradle/`, `.idea/`, `local.properties`, `keystore.properties`, `keystore.jks`, `*.apk`, etc. on the next `git add`.
- Files: repo root (missing file)
- Impact: Very high. Accidentally committing `keystore.properties` or `keystore.jks` leaks the release signing key; accidentally committing `local.properties` leaks per-machine SDK paths (and is what's happening today — see Security).
- Fix approach: Add a standard Android `.gitignore` at the repo root covering at minimum: `**/build/`, `.gradle/`, `.idea/`, `*.iml`, `local.properties`, `keystore.properties`, `*.jks`, `*.keystore`, `*.apk`, `*.aab`, `captures/`, `.cxx/`, `app/release/`. Verify `git status` is clean after a full build before the next commit.

### Duplicate `include(":baselineprofile")` in `settings.gradle.kts`
- Issue: `:baselineprofile` is `include`d twice (lines ~33 and ~46 of `settings.gradle.kts`).
- Files: `settings.gradle.kts:33`, `settings.gradle.kts:46`
- Impact: Gradle tolerates this (idempotent), but it's a smell that suggests the file was hand-merged during restoration. Future readers will be confused.
- Fix approach: Delete one of the two `include(":baselineprofile")` lines; keep the one with the explanatory comment.

### Generic `catch (e: Throwable)` swallowing in repositories/paging
- Issue: Several call sites collapse every failure into `AppError.Network(e.message)` regardless of root cause, which loses distinction between cancellation, programmer error, parsing errors, and real network failures.
- Files: `core/data/src/main/java/io/stashapp/android/core/data/scene/DefaultSceneRepository.kt:78`, `:97`, `:163`, `:178`; `core/data/src/main/java/io/stashapp/android/core/data/scene/ScenePagingSource.kt:61`; `core/data/src/main/java/io/stashapp/android/core/data/browse/BrowsePagingSources.kt:77`, `:125`, `:173`
- Impact: Medium. `CancellationException` is caught and converted into a fake "Network error" — this breaks structured concurrency and can produce misleading UI banners when the user navigates away mid-load. Real bugs (NPEs, deserialization failures) are also reported to the user as "network errors".
- Fix approach: Catch `CancellationException` and rethrow first; then narrow to `ApolloException`/`IOException` for the network branch and let everything else propagate (or log + rethrow).

### `local.properties` is committed with an absolute path
- Issue: `local.properties` contains `sdk.dir=/home/yun/android-sdk` and is checked into git.
- Files: `local.properties:1`
- Impact: This file should be per-developer and never committed. Today it hardcodes `yun`'s home directory, which will fail for every other contributor / CI runner.
- Fix approach: Remove `local.properties` from the working tree (`git rm --cached local.properties`), add it to the new `.gitignore`, and document SDK setup in `README.md` / `bootstrap.sh` (which already warns about `ANDROID_SDK_ROOT`).

### Large player file (`PlayerScreen.kt`, 1122 lines)
- Issue: `feature/player/src/main/java/io/stashapp/android/feature/player/PlayerScreen.kt` is 1122 lines — over the 800-line "max" guideline and ~3x the next-largest screen.
- Files: `feature/player/src/main/java/io/stashapp/android/feature/player/PlayerScreen.kt`
- Impact: Hard to navigate; mixes the `AndroidView` wrapper, MX-Player-style gesture controls, top bar, bottom bar, the frame-rate side-effect, and the screenshot stub.
- Fix approach: Split out `PlayerControls`, `PlayerGestures`, and the `applyVideoFrameRate`/`enterPip`/`codecLabel` helpers into sibling files (`PlayerControls.kt`, `PlayerGestures.kt`, `PlayerSurface.kt`). The existing private-by-default visibility makes this mechanical.

### Empty `lint-baseline.xml` referenced in `app/build.gradle.kts`
- Issue: `app/build.gradle.kts` declares `lint { baseline = file("lint-baseline.xml") }` but `app/lint-baseline.xml` does not exist on disk.
- Files: `app/build.gradle.kts` (lint block), expected `app/lint-baseline.xml`
- Impact: Low — AGP will create the baseline on the next `lint` run, but until then every existing lint warning becomes a fresh "new" issue. If the missing file is unexpected (i.e., lost in restoration), the previously-accepted baseline of issues is gone.
- Fix approach: Run `./gradlew :app:updateLintBaseline` once on a clean tree and commit the generated file; or remove the `baseline = …` line if you want a clean slate.

## Known Bugs / Explicit TODOs

### Player screenshot button is a stub
- Symptoms: Tapping the screenshot button flashes "Screenshot — coming soon" banner; nothing is captured.
- Files: `feature/player/src/main/java/io/stashapp/android/feature/player/PlayerScreen.kt:411`
- Trigger: Open scene → player → tap screenshot icon.
- Workaround: None. The UI element is shipped but non-functional.
- Fix approach: Use `MediaMetadataRetriever.getFrameAtTime` (per the in-file comment) plus `MediaStore.Images` insert; declare `READ_MEDIA_IMAGES` (API 33+) / scoped storage write. The author flagged storage-permission plumbing as the deferred work.

### Filter sheet entity pickers not yet implemented
- Symptoms: README "TODO" entry — tag/performer/studio picker inside the filter sheet only round-trips pre-selected ids; you cannot pick them inside the sheet.
- Files: `feature/library/src/main/java/io/stashapp/android/feature/library/FilterSheet.kt` (531 lines), referenced by `feature/library/src/main/java/io/stashapp/android/feature/library/LibraryViewModel.kt`
- Trigger: Open filter sheet → there is no UI to add a tag/performer/studio filter.
- Workaround: None from the UI; ids can be passed in programmatically via deep-link from the Browse screens, which is the existing UX path.
- Fix approach: Reuse the Browse screen's entity grid as a picker dialog; pass back selection through a saved-state handle.

## Restoration Gaps (likely lost in session-history restore)

The single commit message ("restore Slopper Android app from Claude session history") plus the absence of files that the codebase references suggests the following items were never restored:

- **`.gitignore`** — referenced by convention; missing entirely (see Tech Debt).
- **`app/lint-baseline.xml`** — referenced by `app/build.gradle.kts`; missing (see Tech Debt).
- **`gradlew` / `gradlew.bat` / `gradle/wrapper/gradle-wrapper.jar`** — `bootstrap.sh` claims to install the wrapper but `gradle/` contents on disk are limited (`/home/yun/slopper/gradle/wrapper` exists but the wrapper jar/script are not in the repo). Until `./bootstrap.sh` is run, `./gradlew` does not exist. This is intentional per `bootstrap.sh`, but `README.md` should make it explicit on the very first build step (it does — fine).
- **No test files anywhere** — `find … -path "*test*"` returns no `src/test/` or `src/androidTest/` directories. Either tests were never written, or they were lost. The user's global rules require 80% coverage; the project is at 0% (see Test Coverage Gaps).
- **`keystore.properties`, `keystore.jks`** — intentionally never restored (and shouldn't be). The example file is present; see Security.
- **Generated Apollo sources** — symlinks in `core/network/src/main/graphql/…` point into `../../graphql/schema/`; verify that path resolves on a fresh clone (the README mentions a `graphql/schema/` dir at the repo root; not yet inspected — flag for follow-up).

## Security Considerations

### Keystore handling
- Risk: Release signing key may end up in git history if a contributor follows the doc literally without a `.gitignore`.
- Files: `keystore.properties.example`, `app/build.gradle.kts:34-49`
- Current mitigation: `keystore.properties.example` and the comments in `app/build.gradle.kts` explicitly warn "NEVER check that one in"; CI env vars `STASH_KEYSTORE_*` are the preferred path. Release falls back to the debug keystore if neither source is present, so a fresh clone won't accidentally sign with a missing-but-thought-present key.
- Recommendations:
  1. Add `keystore.properties` and `*.jks` / `*.keystore` to the root `.gitignore` *before* anyone generates a real key locally.
  2. Add a `git secrets` / `pre-commit` hook (or a CI grep) that refuses to commit `BEGIN PRIVATE KEY`, `keystore.jks`, or any non-`.example` `keystore.properties`.
  3. Document key rotation procedure in `README.md` (currently absent).
  4. Consider Google Play App Signing so the upload key can be rotated without an app reinstall.

### Cleartext HTTP is permitted app-wide
- Risk: API key is sent over plain HTTP if the user enters an `http://` URL — vulnerable to passive sniffing on hostile networks.
- Files: `app/src/main/AndroidManifest.xml` (`android:usesCleartextTraffic="true"`), `app/src/main/res/xml/network_security_config.xml`
- Current mitigation: User-installed CAs are intentionally excluded from the base config (file calls this "security review H2"), so a rogue MDM-installed root cannot MITM HTTPS. Comment acknowledges Stash on LAN commonly uses HTTP.
- Recommendations: Add a one-time warning banner on the connection screen when the URL is `http://` and not on a private RFC1918 subnet, and prefer `https://` autocomplete. Long term, scope cleartext to a configurable allowlist via `<domain-config cleartextTrafficPermitted="true">` instead of the global flag.

### Encrypted prefs
- Status: API key is stored via `EncryptedSharedPreferences` (Jetpack Security + AndroidKeyStore) per `core/data/src/main/java/io/stashapp/android/core/data/prefs/ConnectionStore.kt`. Good.
- Risk: No biometric prompt on app open (the README's "TODO" lists biometric app lock); a stolen-unlocked device exposes API access.
- Fix approach: Wire `androidx.biometric` into a launcher gate; gate decryption of the prefs key on successful auth.

### OWASP dependency-check is configured but unused
- Status: `config/owasp-suppressions.xml` exists with only commented-out example entries, indicating the plugin is wired (somewhere) but no real suppressions yet. Verify the gradle task name and that CI actually runs `dependencyCheckAnalyze`.
- Files: `config/owasp-suppressions.xml`
- Risk: Vulnerable transitive deps go undetected.
- Fix approach: Confirm a CI job invokes the plugin and fails the build on `CRITICAL`/`HIGH`.

## Performance Hotspots

The presence of a dedicated `:baselineprofile` module (`baselineprofile/src/main/java/io/stashapp/android/baselineprofile/StashBaselineProfileGenerator.kt`) and a `benchmark` build type signals deliberate attention to startup/scroll performance. Current state:

### Baseline profile journey is minimal
- Issue: `StashBaselineProfileGenerator.generate()` only does cold start → 3 grid scrolls → first-card tap → back. Detail/player/browse/settings/filter paths are not exercised, so their hot Compose paths won't be AOT-compiled.
- Files: `baselineprofile/src/main/java/io/stashapp/android/baselineprofile/StashBaselineProfileGenerator.kt`
- Impact: Player and filter sheet (the most visually heavy screens) will JIT on first use.
- Improvement path: After "tap first card → back", also tap "Play" to enter the player (1–2s probe), then back; tap the filter icon (sheet animation); navigate to a browse tab. Each adds <5s of trace and meaningfully more profile coverage.

### Baseline profile only runs on a connected device
- Issue: `useConnectedDevices = true`, no Gradle-managed-device (the file says "punted for now"). Profile regeneration is therefore manual.
- Files: `baselineprofile/build.gradle.kts`
- Improvement path: Define a GMD (`Pixel 6 API 34`) so CI can regen the profile reproducibly.

### `PlayerScreen.kt` does a `setFrameRate` on every Compose `update` pass
- Issue: `applyVideoFrameRate` is called from the `AndroidView`'s `update` block (per the file's own comment), relying on `setFrameRate` being idempotent.
- Files: `feature/player/src/main/java/io/stashapp/android/feature/player/PlayerScreen.kt:1075-1090`
- Impact: Low (system call is cheap) but not free; would benefit from a `LaunchedEffect(fps)` instead so it only fires on actual change.

### Coil image loader scope
- Status: Custom image loader in `core/ui/` injects the `ApiKey` header only for Stash-origin URLs (per README). Verify it uses Coil's disk cache + memory cache defaults sensibly; sprite thumbnails on the grid will benefit massively from a generous memory-cache size on phones with ≥6 GB RAM.

## Fragile Areas

### `PlayerScreen.kt` mixes Compose, AndroidView, and direct Activity manipulation
- Files: `feature/player/src/main/java/io/stashapp/android/feature/player/PlayerScreen.kt`
- Why fragile: `enterPip` takes an `Activity` directly, frame-rate code reflects on `videoSurfaceView`, gesture controls own a `lastInteraction` ms timestamp for chrome auto-hide. Refactors that touch any of these regions risk breaking PiP, screen-on lock, or the gesture chrome.
- Safe modification: Always run the full `DEVICE_TESTING.md` player checklist before merging — PiP enter/exit and rotation lock are not unit-testable.
- Test coverage: Zero (see below).

### No foreground service yet despite `FOREGROUND_SERVICE_MEDIA_PLAYBACK` permission
- Files: `app/src/main/AndroidManifest.xml` declares the permission; no `<service>` element registers a MediaSessionService / MediaLibraryService anywhere in the project.
- Why fragile: Audio will be killed when the app backgrounds on Android 12+. README claims "MediaSession-ready player" but the service half is missing.
- Safe modification: Add a `MediaSessionService` subclass in `feature/player` and register it in the manifest before relying on background audio in production.

### `local.properties` hardcodes the developer's username
- Files: `local.properties`
- Why fragile: Any clone by someone other than `yun` will fail until they edit this file. See Tech Debt.

### Settings restoration only sets `applicationIdSuffix = ".debug"` on debug
- Files: `app/build.gradle.kts`
- Why fragile: The README's `adb shell am start` invocation hardcodes the debug suffix; release/benchmark variants both use the bare `io.stashapp.android`. If both are installed at once they conflict only between release and benchmark — keep this in mind during baseline profile generation.

## Test Coverage Gaps

### Zero automated tests in the repository
- What's not tested: Everything. No `src/test/`, no `src/androidTest/`, no JUnit, no Robolectric, no Compose UI tests, no repository tests, no ViewModel tests.
- Files: All modules
- Risk: Very high. The user's global rules mandate 80% coverage with unit + integration + E2E layers. Current state is zero. Every refactor is "test by hand against a real Stash server" per `DEVICE_TESTING.md`.
- Priority: High. Recommended initial seeding, in order:
  1. `core/data` repository tests (mock `ApolloClient` with Apollo's `TestNetworkTransportHandler`; covers the `catch (e: Throwable)` narrowing in the same PR).
  2. `core/data/scene/SceneFilterMapper` pure-function tests.
  3. `feature/library/LibraryViewModel` state-machine tests with `Turbine`.
  4. Compose UI tests for `SceneCard`, `FilterSheet`, `ConnectionScreen` (use `ComposeTestRule`).
  5. A single end-to-end macrobenchmark journey already wired in `:baselineprofile`.

## Missing Critical Features

(Pulled from `README.md`'s explicit TODO list — these are intentionally unbuilt, listed here so planners don't accidentally count them as done.)

- Chromecast support (`Media3 Cast` extension)
- Offline downloads (`ExoPlayer DownloadService`)
- Tag/performer/studio picker inside the filter sheet (see Known Bugs)
- Scene marker creation from the player (long-press timeline)
- HDR passthrough verification matrix
- Biometric app lock (see Security)
- Pull-to-refresh on library + detail
- Android TV support (Leanback / TV Compose) — separate plan
- FFmpeg extension via local build (build tooling exists in `tools/ffmpeg-extension/` but is skipped; the app currently consumes `nextlib` from Maven Central instead — fine, but the duplicate tooling will rot)

## Dependencies at Risk

- **`nextlib` (Maven Central FFmpeg extension)** — `feature/player/FFMPEG_EXTENSION.md` notes ~6 MB per ABI of LGPL native code shipped via a third-party. Risk: upstream abandonment. Mitigation: `tools/ffmpeg-extension/` (Dockerfile + build.sh) exists as an escape hatch — but it's untested in the restored tree.
- **Gradle 8.11.1 pinned in `bootstrap.sh`** — AGP version not visible here; verify compatibility against the `gradle/libs.versions.toml` Android Gradle Plugin version on the next build.

## Scaling Limits

- **Paging 3 page size** — `core/data/src/main/java/io/stashapp/android/core/data/scene/ScenePagingSource.kt` uses Apollo `findScenes` with offset pagination. Stash servers with very large libraries (>50k scenes) may see slow tail-page queries; profile against a representative server before claiming production readiness.
- **Single ApolloClient instance** — Verify connection pooling sizes are adequate for the player concurrently fetching sprite thumbnails, sub-resources, and the resume-sync mutation.

---

*Concerns audit: 2026-05-16*
