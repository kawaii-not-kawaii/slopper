# Technology Stack

**Analysis Date:** 2026-05-16

## Languages

**Primary:**
- Kotlin 2.1.0 — All app, library, and convention plugin source. Configured in `gradle/libs.versions.toml` (`kotlin = "2.1.0"`) and enforced via `build-logic/convention/src/main/kotlin/io/stashapp/android/buildlogic/KotlinAndroid.kt`.

**Secondary:**
- Kotlin DSL (`*.gradle.kts`) — Gradle build scripts across all modules.
- GraphQL (SDL) — Apollo operations under `core/network/src/main/graphql/io/stashapp/android/graphql/operations/` (e.g. `FindScenes.graphql`, `SceneActivity.graphql`).
- XML — Android resources / manifest, e.g. `app/src/main/AndroidManifest.xml`, `app/src/main/res/xml/network_security_config.xml`.

## Runtime

**Environment:**
- Android (target platform). `compileSdk = 35`, `minSdk = 26` set in `build-logic/convention/src/main/kotlin/io/stashapp/android/buildlogic/KotlinAndroid.kt`.
- `baselineprofile` module raises `minSdk = 28` (baseline profiles are no-ops below P) in `baselineprofile/build.gradle.kts`.
- JVM target: Java 17 (`JavaVersion.VERSION_17`, `JvmTarget.JVM_17`) — set in `KotlinAndroid.kt` and `baselineprofile/build.gradle.kts`.
- Kotlin compiler args (in `KotlinAndroid.kt`): `-Xcontext-receivers`, `-opt-in=kotlin.RequiresOptIn`, `-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi`, `-Xskip-metadata-version-check` (to tolerate nextlib built on Kotlin 2.3).

**Package Manager:**
- Gradle 8.11.1 — `gradle/wrapper/gradle-wrapper.properties` (`distributionUrl=...gradle-8.11.1-bin.zip`).
- Android Gradle Plugin (AGP) 8.7.3 — `libs.versions.toml` (`agp = "8.7.3"`).
- Lockfile: not present (no `gradle.lockfile`). Reproducibility relies on the version catalog.

## Frameworks

**Core:**
- Jetpack Compose — UI toolkit. BOM `androidx.compose:compose-bom:2024.12.01` (`composeBom` in `libs.versions.toml`). Compiler enabled via `kotlin-compose` plugin (`org.jetbrains.kotlin.plugin.compose` 2.1.0). Convention plugin: `build-logic/convention/src/main/kotlin/io/stashapp/android/buildlogic/AndroidComposeConventionPlugin.kt`.
- Material 3 — `androidx.compose.material3:material3` + `material-icons-extended`.
- AndroidX Navigation Compose 2.8.5 — `androidx.navigation:navigation-compose`.
- Hilt 2.53.1 — DI framework. `com.google.dagger:hilt-android` + `hilt-android-compiler`, plugin `com.google.dagger.hilt.android`. Convention: `AndroidHiltConventionPlugin.kt`.
- Hilt Navigation Compose 1.2.0 — `androidx.hilt:hilt-navigation-compose`.
- KSP 2.1.0-1.0.29 — Annotation processing (Hilt/Room compilers).
- Apollo Kotlin 4.1.0 — GraphQL client. Plugin `com.apollographql.apollo`. Configured in `core/network/build.gradle.kts` (`apollo { service("stash") { packageName.set("io.stashapp.android.graphql") } }`).
- AndroidX Media3 1.9.1 — ExoPlayer + HLS + DASH + UI + Session + OkHttp datasource + Cast. Used in `feature/player`. Pinned to 1.9.x ("Stay on 1.9.x until we bump AGP / compileSdk (1.10 requires compileSdk 36)" — see `libs.versions.toml` comment).
- nextlib-media3ext 1.9.1-0.11.0 — `io.github.anilbeesetti:nextlib-media3ext`. Prebuilt FFmpeg extension supplying AC3/EAC3/DTS/TrueHD audio + H.264/HEVC/VP8/VP9 software-decode fallback (see `feature/player/build.gradle.kts`).
- AndroidX Paging 3.3.5 — `androidx.paging:paging-runtime-ktx` + `paging-compose`. Used in `feature/library`, `feature/browse`.
- AndroidX DataStore (Preferences) 1.1.1 — Player/UI prefs (`core/data/src/main/java/io/stashapp/android/core/data/prefs/PlayerPreferences.kt`).
- AndroidX Security Crypto 1.1.0 — AES-GCM `EncryptedSharedPreferences` for credentials (`core/data/.../ConnectionStore.kt`).
- AndroidX Lifecycle 2.8.7 — `lifecycle-runtime-ktx`, `viewmodel-compose`, `runtime-compose`.
- AndroidX Activity Compose 1.9.3.

**Testing:**
- JUnit 4 4.13.2 — `junit:junit`.
- AndroidX Test ext-junit 1.2.1.
- AndroidX UI Automator 2.3.0 — baseline profile generation in `baselineprofile/`.
- AndroidX Benchmark Macrobenchmark 1.3.3 — `androidx.benchmark:benchmark-macro-junit4`.
- AndroidX Baseline Profile Gradle Plugin 1.3.3 — `androidx.baselineprofile`.
- AndroidX ProfileInstaller 1.4.1 — installs compiled baseline profile at install time (`app/build.gradle.kts`).

**Build/Dev:**
- Convention plugins under `build-logic/convention/src/main/kotlin/io/stashapp/android/buildlogic/`:
  - `stash.android.application` → `AndroidApplicationConventionPlugin.kt`
  - `stash.android.library` → `AndroidLibraryConventionPlugin.kt`
  - `stash.android.feature` → `AndroidFeatureConventionPlugin.kt`
  - `stash.android.compose` → `AndroidComposeConventionPlugin.kt`
  - `stash.android.hilt` → `AndroidHiltConventionPlugin.kt`
- ktlint Gradle plugin 12.1.1 (`org.jlleitschuh.gradle.ktlint`) — ktlint version 1.3.1; applied to every subproject in root `build.gradle.kts`; excludes `/build/` and `**/generated/**`.
- detekt 1.23.7 (`io.gitlab.arturbosch.detekt`) — config at `config/detekt/detekt.yml`.
- OWASP `dependencyCheck` 11.1.1 — fails on CVSS >= 7.0; suppressions at `config/owasp-suppressions.xml`. Runs with `--no-configuration-cache` (plugin incompatible with config cache).
- Android Lint — `abortOnError = true`, `warningsAsErrors = false` locally (CI overrides), baseline at `app/lint-baseline.xml`. Disables `MutableCollectionMutableState`.

## Key Dependencies

**Critical:**
- `com.apollographql.apollo:apollo-runtime` 4.1.0 — Stash server is queried exclusively over GraphQL.
- `com.apollographql.apollo:apollo-normalized-cache-sqlite` 4.1.0 — SQLite-backed normalized cache (declared in `core/network/build.gradle.kts`).
- `com.squareup.okhttp3:okhttp` 4.12.0 + `logging-interceptor` — HTTP client shared by Apollo, Media3 datasource, and Coil.
- `androidx.media3:media3-exoplayer` 1.9.1 — Video playback.
- `io.coil-kt.coil3:coil-compose` 3.0.4 + `coil-network-okhttp` — Image loading (thumbnails, posters).
- `org.jetbrains.kotlinx:kotlinx-coroutines-android` 1.9.0.
- `org.jetbrains.kotlinx:kotlinx-serialization-json` 1.7.3 — Used by `:core:model` and `:core:data` (e.g. `UiPreferences`).

**Infrastructure:**
- `javax.inject:javax.inject:1` — `@Inject` exposed transitively from `:core:common`.
- `com.google.dagger:hilt-android` 2.53.1 — DI runtime.
- `androidx.profileinstaller:profileinstaller` 1.4.1 — Baseline profile installation.

## Configuration

**Environment:**
- `local.properties` — Android SDK location (per-machine).
- `keystore.properties` (optional, gitignored) — Release signing credentials. Template at `keystore.properties.example`. Properties: `storeFile`, `storePassword`, `keyAlias`, `keyPassword`. Alternative: env vars `STASH_KEYSTORE_FILE`, `STASH_KEYSTORE_PASSWORD`, `STASH_KEY_ALIAS`, `STASH_KEY_PASSWORD`. If neither is set, `release` falls back to the debug keystore (acceptable for sideload, NOT Play Store) — see `app/build.gradle.kts` lines 28-50.
- Runtime config: active Stash server URL + API key persisted in `EncryptedSharedPreferences` via `core/data/.../prefs/ConnectionStore.kt`. No baked-in server URL — `NetworkModule` initializes Apollo with a placeholder `http://localhost/graphql` and `StashAuthInterceptor` rewrites every request to the user-selected endpoint.

**Build:**
- `gradle/libs.versions.toml` — Single source of truth for versions/libraries/plugins.
- `gradle.properties` — JVM args (`-Xmx4g`), parallel + build cache + configuration cache enabled, KSP incremental on, `android.useAndroidX=true`, `android.nonTransitiveRClass=true`, default `buildConfig`/`resValues`/`shaders` features turned off.
- `app/proguard-rules.pro` — R8 keep rules for Apollo generated classes (`io.stashapp.android.graphql.**`), Hilt `HiltViewModel`, Media3 + nextlib reflective loading, kotlinx.serialization companions.
- `app/src/main/res/xml/network_security_config.xml` — Cleartext allowed (Stash on LAN), system trust anchors only in release (no user CAs — explicit MITM mitigation), user CAs added under `debug-overrides` for Charles/mitmproxy.

## Platform Requirements

**Development:**
- JDK 17.
- Android SDK with API 35 platform (`compileSdk = 35`).
- Android SDK API 28+ for the macrobenchmark device.
- Gradle wrapper handles Gradle itself (no system Gradle required).
- `bootstrap.sh` at repo root for environment setup.

**Production:**
- Android device or emulator API 26 (Android 8.0) and above.
- App ABI splits: `arm64-v8a`, `armeabi-v7a` only (`isUniversalApk = false` in `app/build.gradle.kts`).
- Release build: R8 minify + resource shrink enabled, JNI symbols stripped (`ndk.debugSymbolLevel = "NONE"`).
- Distribution: sideload-oriented; SemVer `0.x` is pre-release, `1.0` reserved for first public/stable release (per `app/build.gradle.kts` comment). `versionCode = 2`, `versionName = "0.2.0-alpha"`.
- Build variants: `debug` (`.debug` applicationIdSuffix, no minify), `benchmark` (mirrors release + debug-signed + `profileable` for macrobenchmark), `release` (minified, shrunk, release-signed if keystore present).

---

*Stack analysis: 2026-05-16*
