<!-- generated-by: gsd-doc-writer -->
# Slopper

> ⚠️ **Alpha software.** Slopper 0.2.0-alpha is under active development and
> **not production-ready**. The build, APIs, and behavior may change without
> notice, and releases are unstable. It currently rides pre-release build tooling
> (AGP 9.2.1, Kotlin 2.3.20 / KSP 2.3.9, detekt 2.0.0-alpha.5, and
> androidx baseline-profile 1.5.0-alpha07). Use at your own risk.

[![OpenSSF Scorecard](https://api.securityscorecards.dev/projects/github.com/kawaii-not-kawaii/slopper/badge)](https://scorecard.dev/viewer/?uri=github.com/kawaii-not-kawaii/slopper)

Native Android client for [Stash](https://github.com/stashapp/stash) — a Kotlin /
Jetpack Compose multi-module app that talks to a self-hosted Stash GraphQL
server. Connection flow, paginated library grid with search/filter, scene
detail with rating + organize + O-counter actions, and a Media3-backed player
with queue / shuffle / repeat / PiP / marker-seek / resume sync-back.

The **v1.0 modernization milestone** is complete, and phases 7–10 of the v1.1
milestone have landed. The current 0.2.0-alpha build uses Gradle 9.4.1, AGP
9.2.1, Kotlin 2.3.20, Hilt 2.59.2, Apollo 5.0.1, and Media3 1.10.0. The
milestones also delivered the platform-compliance, performance, visual-design,
and architecture work described in the repository history.

## Toolchain

| Component                  | Version                  | Source                                  |
|----------------------------|--------------------------|-----------------------------------------|
| Application                | 0.2.0-alpha              | `app/build.gradle.kts`                  |
| Gradle wrapper             | 9.4.1 (SHA-256 pinned)   | `gradle/wrapper/gradle-wrapper.properties` |
| AGP                        | 9.2.1                    | `gradle/libs.versions.toml`             |
| Kotlin / KSP               | 2.3.20 / 2.3.9           | `gradle/libs.versions.toml`             |
| JDK toolchain              | 17 (auto-download off)   | `build-logic/.../KotlinAndroid.kt`, `gradle.properties` |
| compileSdk / targetSdk     | 36 / 35                  | `build-logic/.../KotlinAndroid.kt`, convention plugins |
| minSdk                     | 26                       | `build-logic/.../KotlinAndroid.kt`      |
| Hilt                       | 2.59.2                   | `gradle/libs.versions.toml`             |
| Apollo                     | 5.0.1                    | `gradle/libs.versions.toml`             |
| Compose BOM                | 2026.06.01               | `gradle/libs.versions.toml`             |
| Media3 / nextlib extension | 1.10.0 / 1.10.0-0.12.1  | `gradle/libs.versions.toml`             |
| Coil                       | 3.4.0                    | `gradle/libs.versions.toml`             |
| detekt / ktlint plugin     | 2.0.0-alpha.5 / 14.2.0  | `gradle/libs.versions.toml`             |
| Baseline-profile plugin    | 1.5.0-alpha07            | `gradle/libs.versions.toml`             |

Single source of truth for every library version is
[`gradle/libs.versions.toml`](gradle/libs.versions.toml).

## First-time build

Five commands from a clean clone to an installed debug APK.

```bash
# 1. JDK 17 must be on PATH (Adoptium / Temurin / Zulu / Corretto — any vendor).
java -version    # should print "17.x.x"

# 2. Android SDK with platforms 35 + 36 and build-tools 35 + 36 installed,
#    exported via either ANDROID_SDK_ROOT (preferred) or ANDROID_HOME.
export ANDROID_SDK_ROOT="$HOME/Android/Sdk"

# 3. Install the Gradle wrapper (idempotent — safe to re-run).
./bootstrap.sh

# 4. Build per-ABI debug APKs.
./gradlew :app:assembleDebug

# 5. Install on a connected device with USB debugging enabled.
adb install -r app/build/outputs/apk/debug/app-arm64-v8a-debug.apk
```

For a one-step build-and-install: `./gradlew :app:installDebug`.

Notes:
- The wrapper enforces `org.gradle.java.installations.auto-download=false` —
  Gradle will refuse to download its own JDK. Install JDK 17 yourself.
- [`bootstrap.sh`](bootstrap.sh) prefers a system `gradle` if present;
  otherwise it pulls Gradle 9.4.1 into `~/.local/gradle-9.4.1/` and uses
  that to generate the wrapper.
- Build artifacts land in `app/build/outputs/apk/debug/` as per-ABI splits
  (`app-arm64-v8a-debug.apk`, `app-armeabi-v7a-debug.apk`).

For the full install-and-smoke-test checklist (connection flow, library, scene
detail, player, queue, marker seek, resume sync-back) see
[`DEVICE_TESTING.md`](DEVICE_TESTING.md).

## Module map

The build graph is driven by `settings.gradle.kts` and DRY'd up via convention
plugins in `build-logic/`.

| Module                  | Role                                                                   |
|-------------------------|------------------------------------------------------------------------|
| `:app`                  | Application module, navigation host, `Application` + `MainActivity`.   |
| `:build-logic:convention` | Gradle convention plugins (`stash.android.application`, `…library`, `…feature`, `…compose`, `…hilt`). |
| `:core:common`          | Dispatchers, `Result` wrappers — pure-Kotlin utilities.                |
| `:core:model`           | Domain models. No Android dependencies.                                |
| `:core:network`         | Apollo client, endpoint provider, auth interceptor.                    |
| `:core:domain`          | Repository interfaces.                                                 |
| `:core:data`            | Repository impls, encrypted preferences, GraphQL→domain mappers.       |
| `:core:designsystem`    | Theme, palette, reusable Compose surfaces (`SceneCard`, etc.).         |
| `:core:ui`              | Coil image loader (Stash API-key aware), shared routes.                |
| `:feature:connection`   | Server URL + API-key entry, server-info probe.                         |
| `:feature:home`         | Home / landing scaffold.                                               |
| `:feature:library`      | Paginated scene grid (Paging 3) + search + filter sheet.               |
| `:feature:browse`       | Performers / Studios / Tags pickers; selection filters the library.   |
| `:feature:detail`       | Scene detail — metadata, performers, tags, markers, rating + actions.  |
| `:feature:player`       | Media3 wrapper, queue, gesture controls, PiP, activity sync.           |
| `:feature:settings`     | Codec status, browse entrypoints, disconnect.                          |
| `:baselineprofile`      | Macrobenchmark module — generates baseline profile for `:app`.         |

## GraphQL schema

Apollo generates Kotlin types from a vendored single-file schema at
[`core/network/src/main/graphql/io/stashapp/android/graphql/schema.graphqls`](core/network/src/main/graphql/io/stashapp/android/graphql/schema.graphqls).
The file is a concatenation of `stashapp/stash@develop`'s `graphql/schema/`
(root + `types/*`) with provenance recorded in the file header.

Regenerate after a schema bump:

```bash
./gradlew :core:network:generateApolloSources
```

This task also runs automatically as part of `:app:assembleDebug`.

## Release signing

Release builds in `app/build.gradle.kts` read signing config from a
`keystore.properties` file (or the matching `STASH_KEYSTORE_*` environment
variables in CI). The live file is **never** committed —
[`.gitignore`](.gitignore) excludes `keystore.properties`, `*.keystore`, and
`release.keystore`.

To set up signing locally:

```bash
cp keystore.properties.example keystore.properties
# Edit keystore.properties — point storeFile at your .jks and set passwords.
./gradlew :app:assembleRelease
```

See [`keystore.properties.example`](keystore.properties.example) for the
expected keys. Without either source the release build falls back to the
debug keystore — fine for personal sideload, **not** for Play Store
distribution.

## Lint detector disables (transitional)

Three lint detectors are turned off in
[`build-logic/convention/src/main/kotlin/io/stashapp/android/buildlogic/KotlinAndroid.kt`](build-logic/convention/src/main/kotlin/io/stashapp/android/buildlogic/KotlinAndroid.kt):

- `NullSafeMutableLiveData` (from `androidx.lifecycle` 2.10.0)
- `FrequentlyChangingValue` (from `compose-runtime` in Compose BOM 2026.06.01)
- `RememberInComposition` (from `compose-runtime` in Compose BOM 2026.06.01)

These detectors remain disabled in the current convention plugin pending
compatibility re-evaluation. Re-enable them when the AndroidX/toolchain
combination supports them.

If you see new ignored lint warnings after rebasing, check whether this list
can be reduced.

## Repository layout

```
slopper/
├── app/                     # Application module
├── baselineprofile/         # Macrobenchmark — baseline profile generator
├── build-logic/convention/  # Gradle convention plugins
├── config/                  # detekt + lint config
├── core/                    # common, model, domain, network, data, ui, designsystem
├── feature/                 # connection, home, library, browse, detail, player, settings
├── gradle/
│   ├── libs.versions.toml   # Single source of truth for deps + versions
│   └── wrapper/             # Pinned Gradle 9.4.1 wrapper
├── tools/                   # Out-of-band tooling (e.g. ffmpeg-extension build)
├── bootstrap.sh             # One-time Gradle wrapper installer
├── DEVICE_TESTING.md        # End-to-end install + smoke test checklist
├── keystore.properties.example
├── settings.gradle.kts
└── build.gradle.kts
```

## Status & caveats

- **Automated JVM coverage exists.** The local full gate on 2026-08-01 ran 61
  passing JVM tests across 16 reports. There is still no `androidTest`
  correctness coverage, so UI and device behavior require the checklist in
  [`DEVICE_TESTING.md`](DEVICE_TESTING.md).
- **CI configuration exists.** GitHub Actions and Forgejo Actions workflows are
  present. No live remote workflow result was verified on 2026-08-01.
- **The local build and quality gate passed on 2026-08-01.**
  `./gradlew :app:assembleDebug detekt ktlintCheck test lint --no-daemon`
  completed with 896 tasks (203 executed, 693 up-to-date). APK runtime launch
  was not verified: no device was connected, and software emulators crashed
  without KVM.
- **Phase 3 (PERF) landed.** GMD (Pixel 6 API 34) declared; Compose Compiler stability reports enabled; `ImmutableList<T>` migration for home/player UiState; `applyVideoFrameRate` moved from `AndroidView.update` to `LaunchedEffect`; shuffle queue-exhaustion UX fix; `ColdStartBenchmark` + `LibraryScrollBenchmark` added. Macrobench execution remains a device-testing concern.
- **Platform compliance (Phase 2) landed.** Edge-to-edge enforced via
  `enableEdgeToEdge()` (bar-color overrides stripped from `themes.xml`); all
  three `ModalBottomSheet` sites get `contentWindowInsets`; `PlayerScreen`
  chrome wrapped in `safeDrawingPadding`; `PredictiveBackHandler` replaces
  `BackHandler`; Splash Screen API wired in `MainActivity`; per-app language
  picker via `generateLocaleConfig`; orphan `POST_NOTIFICATIONS` and
  `FOREGROUND_SERVICE_MEDIA_PLAYBACK` permissions removed.
- **FFmpeg extension is optional.** Without it, ExoPlayer falls back to the
  device's hardware decoders only (AC3/EAC3/DTS/TrueHD may fail). Build
  scripts live in `tools/ffmpeg-extension/`.

## Useful Gradle tasks

```bash
./gradlew :app:assembleDebug                       # build debug APKs
./gradlew :app:installDebug                        # build + install in one step
./gradlew :app:assembleRelease                     # build signed release (needs keystore.properties)
./gradlew :core:network:generateApolloSources      # regenerate Apollo types
./gradlew :app:generateBaselineProfile             # regenerate baseline profile (uses :baselineprofile)
./gradlew ktlintCheck                              # lint Kotlin style
./gradlew detekt                                   # static analysis
./gradlew lint                                     # Android lint
./gradlew clean                                    # nuke build outputs (use when KSP gets confused)
```
