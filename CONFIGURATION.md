<!-- generated-by: gsd-doc-writer -->
# Configuration

Single reference for every knob Slopper exposes — build-time env vars, Gradle properties,
release signing inputs, static-analysis baselines, build-logic compiler/lint settings, and
the runtime preference stores backed by DataStore and EncryptedSharedPreferences.

For dependency-version edits see [DEVELOPMENT.md](DEVELOPMENT.md) (catalog workflow).
For first-run setup see [GETTING-STARTED.md](GETTING-STARTED.md).

---

## 1. Build-time environment variables

Slopper builds are configured exclusively through the env / `gradle.properties` /
`keystore.properties` triad — no `local.properties` overrides beyond the standard
Android SDK path are read.

| Variable | Required? | Purpose | Example |
|---|---|---|---|
| `JAVA_HOME` | Yes | JDK 17 install used by Gradle daemon + Kotlin toolchain. Gradle is forbidden from auto-downloading a JDK (`org.gradle.java.installations.auto-download=false`). | `/usr/lib/jvm/temurin-17-jdk` |
| `ANDROID_SDK_ROOT` | Yes (or `ANDROID_HOME`) | Android SDK location. `bootstrap.sh` warns if neither is set. | `$HOME/Android/Sdk` |
| `ANDROID_HOME` | Alternative to `ANDROID_SDK_ROOT` | Legacy SDK path env var; either one satisfies the check. | `$HOME/Android/Sdk` |
| `STASH_KEYSTORE_FILE` | Optional (release-signing path A) | Absolute or repo-relative path to the release `.jks` keystore. CI pattern — see §3. | `/secrets/stash-release.jks` |
| `STASH_KEYSTORE_PASSWORD` | Optional (release-signing path A) | Password for the keystore file. | `redacted` |
| `STASH_KEY_ALIAS` | Optional (release-signing path A) | Key alias inside the keystore. | `stash-release` |
| `STASH_KEY_PASSWORD` | Optional (release-signing path A) | Password for the key entry. | `redacted` |
| `NVD_API_KEY` | Optional | Speeds up `./gradlew dependencyCheckAggregate` by avoiding the NVD anonymous rate limit. Not yet enforced by CI. <!-- VERIFY: NVD_API_KEY consumption is via the OWASP dependencyCheck plugin's standard `nvdApiKey` setting — confirm in CI workflow when added --> | `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx` |
| `GRADLE_VERSION` | Optional | Used **only** by `bootstrap.sh` to override the wrapper version it installs. Default `8.11.1`. Does not affect builds once the wrapper exists. | `8.11.1` |

Release signing follows a two-source pattern: `keystore.properties` values take
precedence; missing keys fall through to the matching `STASH_KEYSTORE_*` env var. See §3.

---

## 2. `gradle.properties`

Project-wide Gradle behavior. Every key from
`/home/yun/slopper/gradle.properties`:

| Key | Value | Purpose |
|---|---|---|
| `org.gradle.jvmargs` | `-Xmx2g -Dfile.encoding=UTF-8 -XX:+UseParallelGC` | Gradle daemon heap cap. Sized for ≥12 GB hosts. |
| `org.gradle.parallel` | `true` | Run independent project tasks in parallel. |
| `org.gradle.workers.max` | `2` | Cap parallel workers to 2 — keeps daemon + Kotlin daemon + workers under ~8 GB on 12 GB dev hosts. |
| `org.gradle.caching` | `true` | Local build cache enabled. |
| `org.gradle.configuration-cache` | `true` | Configuration cache enabled. **Exception:** the OWASP `dependencyCheck` plugin is not yet config-cache compatible — use `--no-configuration-cache` when running it locally. |
| `kotlin.daemon.jvmargs` | `-Xmx1500m -XX:+UseParallelGC` | Kotlin daemon heap cap (paired with the worker cap above). |
| `android.useAndroidX` | `true` | Required for the modern AndroidX stack. |
| `android.nonTransitiveRClass` | `true` | Each module gets its own R class; faster builds, smaller APKs. |
| `android.defaults.buildfeatures.buildconfig` | `false` | `BuildConfig` generation off by default (modules opt in). |
| `android.defaults.buildfeatures.resvalues` | `false` | `resValues` generation off by default. |
| `android.defaults.buildfeatures.shaders` | `false` | Shader compilation off by default. |
| `kotlin.code.style` | `official` | Aligns IDE formatting with the official Kotlin style guide. |
| `ksp.incremental` | `true` | Incremental KSP processing. |
| `org.gradle.java.installations.auto-download` | `false` | **Security:** prevents Gradle from auto-fetching a JDK if the toolchain spec isn't satisfied locally. Fail loud instead. |

Do not edit these casually — the worker-cap + daemon-heap pair is calibrated together.

---

## 3. Release signing

Two-path approach. Either source is sufficient; if both are present, the file wins.

**Path A — `keystore.properties` (local release builds):** copy
`keystore.properties.example` to `keystore.properties` at the repo root. The file is
`.gitignore`d (`keystore.properties`, `*.jks`, `*.keystore`, `release.keystore`).

```properties
storeFile=../keystore.jks
storePassword=change_me
keyAlias=stash-release
keyPassword=change_me
```

**Path B — environment variables (CI):** set `STASH_KEYSTORE_FILE`,
`STASH_KEYSTORE_PASSWORD`, `STASH_KEY_ALIAS`, `STASH_KEY_PASSWORD`. See §1.

**Fallback:** if **neither** source is present, the release build type signs with the
debug keystore. This is acceptable for personal sideload / smoke tests but is **not** a
distributable artifact. `assembleRelease` will still succeed end-to-end on a fresh clone,
which is intentional — see `app/build.gradle.kts` lines 22–48 and 78–79.

Resolution logic (verbatim from `app/build.gradle.kts`):

```kotlin
fun prop(name: String, env: String): String? =
    keystoreProps?.getProperty(name) ?: System.getenv(env)
```

---

## 4. Dependency catalog (`gradle/libs.versions.toml`)

Single source of truth for every plugin, library, and version pin used across all
modules. **Do not** add raw `implementation("group:artifact:version")` strings to module
build files — every dependency must route through the catalog.

See [DEVELOPMENT.md](DEVELOPMENT.md) for the add-an-entry workflow and policy on version
bumps.

---

## 5. Gradle wrapper pin (`gradle/wrapper/gradle-wrapper.properties`)

```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.11.1-bin.zip
distributionSha256Sum=f397b287023acdba1e9f6fc5ea72d22dd63669d59ed4a289a29b1a76eee151c6
networkTimeout=10000
validateDistributionUrl=true
```

Gradle `8.11.1` with SHA-256 pinning + URL validation. The wrapper jar (`gradlew`,
`gradlew.bat`, `gradle-wrapper.jar`) is **gitignored** — `bootstrap.sh` regenerates it
on first run.

---

## 6. Static-analysis configuration

### 6.1 Config files

| File | Purpose |
|---|---|
| `config/detekt/detekt.yml` | Project-wide detekt rule overrides (loosens `LongMethod`, `LongParameterList`, `LargeClass`, etc. for Compose idioms; disables `MagicNumber`, `LateinitUsage`; adds `ForbiddenComment` for `FIXME:` / `STOPSHIP:`). |
| `config/owasp-suppressions.xml` | OWASP dependency-check false-positive suppressions. Referenced by the root `dependencyCheck { suppressionFile = ... }` block. |

The detekt root config is wired in `build.gradle.kts` via the `subprojects {}` block —
every module inherits it automatically. Tool version (`1.23.8`) is set both there and in
`libs.versions.toml`; keep them in sync.

### 6.2 Baselines (all committed)

| Baseline | Scope | Regenerate command |
|---|---|---|
| `app/lint-baseline.xml` | Android Lint snapshot for `:app` (only `:app` runs lint). | `./gradlew :app:updateLintBaseline` |
| `app/detekt-baseline.xml` | detekt baseline for `:app`. | `./gradlew :app:detektBaseline` |
| `core/data/detekt-baseline.xml` | detekt baseline for `:core:data`. | `./gradlew :core:data:detektBaseline` |
| `core/designsystem/detekt-baseline.xml` | detekt baseline for `:core:designsystem`. | `./gradlew :core:designsystem:detektBaseline` |
| `core/ui/detekt-baseline.xml` | detekt baseline for `:core:ui`. | `./gradlew :core:ui:detektBaseline` |
| `feature/connection/detekt-baseline.xml` | detekt baseline for `:feature:connection`. | `./gradlew :feature:connection:detektBaseline` |
| `feature/detail/detekt-baseline.xml` | detekt baseline for `:feature:detail`. | `./gradlew :feature:detail:detektBaseline` |
| `feature/player/detekt-baseline.xml` | detekt baseline for `:feature:player`. | `./gradlew :feature:player:detektBaseline` |
| `feature/settings/detekt-baseline.xml` | detekt baseline for `:feature:settings`. | `./gradlew :feature:settings:detektBaseline` |

Regenerate after a major dependency bump or rule-config change, then review the diff
before committing — a baseline diff that grows in either direction is a signal.

### 6.3 OWASP dependency-check (root build.gradle.kts)

```kotlin
dependencyCheck {
    formats = listOf("HTML", "JSON")
    failBuildOnCVSS = 7.0f                        // fail on HIGH+ (CVSS ≥ 7.0)
    suppressionFile = "$rootDir/config/owasp-suppressions.xml"
    analyzers.apply {
        assemblyEnabled = false
        nuspecEnabled = false
        nodeAuditEnabled = false
        retirejs.enabled = false
    }
}
```

Run locally with `./gradlew dependencyCheckAnalyze --no-configuration-cache`.

### 6.4 ktlint (subprojects)

Pinned to ktlint engine `1.6.0`. Excludes `**/build/**` and `**/generated/**`
(Apollo-generated GraphQL sources). `ignoreFailures = false` — lint failures fail the
build. No ktlint baseline files are committed; the codebase passes cleanly.

---

## 7. Build-logic compiler & lint settings

Centralized in `build-logic/convention/src/main/kotlin/io/stashapp/android/buildlogic/KotlinAndroid.kt`.
Every Android module applied via the `stash.android.*` convention plugins inherits these.

| Setting | Value |
|---|---|
| `compileSdk` | `35` |
| `minSdk` | `26` |
| `targetCompatibility` / `sourceCompatibility` | `JavaVersion.VERSION_17` |
| `isCoreLibraryDesugaringEnabled` | `false` |
| Kotlin `jvmToolchain` | `17` |
| Kotlin `jvmTarget` | `JVM_17` |

**Free compiler args (applied to every module):**

```kotlin
freeCompilerArgs.addAll(
    "-opt-in=kotlin.RequiresOptIn",
    "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
)
```

**Lint detectors disabled at the convention-plugin level** (re-enable when DEPS-07
lands — currently deferred):

| Detector | Source | Reason disabled |
|---|---|---|
| `NullSafeMutableLiveData` | `androidx.lifecycle 2.8.7` | Detector crashes with `IncompatibleClassChangeError` under AGP 8.7.3 lint + Kotlin 2.2.20. |
| `FrequentlyChangingValue` | `androidx.compose-runtime` (Compose BOM 2026.05.00) | Same incompatibility. |
| `RememberInComposition` | `androidx.compose-runtime` (Compose BOM 2026.05.00) | Same incompatibility. |

**Additional `:app`-level lint config** (`app/build.gradle.kts` lines 89–102):

| Setting | Value |
|---|---|
| `abortOnError` | `true` |
| `warningsAsErrors` | `false` (CI overrides to `true`) |
| `checkReleaseBuilds` | `true` |
| `baseline` | `lint-baseline.xml` |
| Disabled check | `MutableCollectionMutableState` (noisy on LazyList items) |

---

## 8. App-module build types & splits (`app/build.gradle.kts`)

| Build type | Debuggable | Minified | Shrink resources | Signing |
|---|---|---|---|---|
| `debug` | yes | no | no | debug keystore; `applicationIdSuffix = ".debug"` |
| `benchmark` | no | yes | inherits from release | **debug keystore** (so macrobenchmark can drive it) — `profileable` via fallback |
| `release` | no | yes | yes | release signing config if available, else debug fallback. `ndk.debugSymbolLevel = "NONE"` strips JNI symbols. |

ABI splits enabled: `arm64-v8a`, `armeabi-v7a`. No universal APK
(`isUniversalApk = false`).

Identity: `applicationId = "io.stashapp.android"`, `versionCode = 2`,
`versionName = "0.2.0-alpha"`.

---

## 9. Runtime preferences

Three independent stores, separated by concern. Two use **DataStore-Preferences**
(plaintext under app data dir); one uses **EncryptedSharedPreferences** (AES-GCM,
AndroidKeyStore-backed) for credentials.

### 9.1 `PlayerPreferences` — DataStore file `player_prefs`

`core/data/src/main/java/io/stashapp/android/core/data/prefs/PlayerPreferences.kt`.
Defaults come from the file's `companion object`.

| Key | Type | Default | Notes |
|---|---|---|---|
| `seek_ms_per_px` | `Float` | `120f` | Drag scrub sensitivity. Clamped `[20f, 500f]`. |
| `double_tap_seek_sec` | `Int` | `10` | Double-tap seek distance. Clamped `[5, 60]`. |
| `default_speed` | `Float` | `1.0f` | Persistent default playback speed. |
| `auto_play_next` | `Boolean` | `true` | Auto-advance to next item in queue at scene end. |
| `resume_threshold_sec` | `Int` | `2` | Resume positions below this are ignored. |
| `completion_threshold_pct` | `Int` | `85` | Percent watched before a "completed play" is recorded. |
| `skip_intro_sec` | `Int` | `0` | Auto-skip N seconds at the start of every scene. 0 = off. |
| `buffer_preset` | `String` | `"medium"` | ExoPlayer min/max buffer preset. One of: `small`, `medium`, `large`. |
| `default_aspect_ratio` | `String` | `"fit"` | One of: `fit`, `crop`, `stretch`. |
| `decoder_preference` | `String` | `"auto"` | One of: `auto`, `prefer_hw`, `prefer_sw`. |

### 9.2 `UiPreferences` — DataStore file `ui_prefs`

`core/data/src/main/java/io/stashapp/android/core/data/prefs/UiPreferences.kt`.

| Key | Type | Default | Notes |
|---|---|---|---|
| `bottom_nav_visible` | `String` (CSV) | `"home,scenes,studios,performers"` | Ordered list of nav-item ids. "More" tab appended separately. |
| `default_scene_filter` | `String` (JSON) | unset (null filter) | JSON-serialized `StoredSceneFilter`. Survives schema growth via `ignoreUnknownKeys = true`. |
| `image_cache_mb` | `Int` | `256` | Coil image cache size in MB. |
| `grid_columns` | `String` | `"auto"` | One of: `auto`, `2`, `3`, `4`. |
| `amoled_black` | `Boolean` | `false` | True-black mode for AMOLED panels. |
| `card_show_rating` | `Boolean` | `true` | Show rating overlay on scene cards. |
| `card_show_play_count` | `Boolean` | `true` | Show play-count overlay on scene cards. |
| `card_show_resolution` | `Boolean` | `true` | Show resolution overlay on scene cards. |
| `activity_tracking` | `Boolean` | `true` | Send play/resume/finish events to the Stash server. |
| `auto_rotate_player` | `Boolean` | `true` | Auto-rotate player on device orientation change. |

### 9.3 `ConnectionStore` — EncryptedSharedPreferences file `stash_connection`

`core/data/src/main/java/io/stashapp/android/core/data/prefs/ConnectionStore.kt`.

Backed by `androidx.security.crypto` `EncryptedSharedPreferences` using a
`MasterKey` (`AES256_GCM` scheme, AndroidKeyStore-backed). Key encryption:
`AES256_SIV`. Value encryption: `AES256_GCM`. The API key is **never** stored in
plaintext on disk — this is intentional and is the only encrypted preference store
in the app.

| Key | Type | Default | Notes |
|---|---|---|---|
| `url` | `String` | unset | Stash server base URL. |
| `api_key` | `String` | unset | Per-server API key. Optional — some self-hosted Stash servers run without auth. |
| `name` | `String` | unset (falls back to `url`) | User-supplied display name for the server. |

---

## 10. Platform-system preferences (not app-controlled)

These behaviors are delegated to the Android OS and are not represented in the
DataStore stores above:

- **Per-app display language** — Android 13+ (API 33+). The system language
  picker is accessed via **Settings → Language row in Slopper's Settings
  screen**, which fires `Intent(Settings.ACTION_APP_LOCALE_SETTINGS)`.
  The selected language is stored by the system (not Slopper) and surfaced via
  the `LocaleManager` API. `app/build.gradle.kts` enables
  `androidResources { generateLocaleConfig = true }` so the system knows which
  locales the app supports.

## 11. What is intentionally NOT configurable

To match the project's "no surveillance, no remote control" posture:

- **No feature flags** — every behavior toggle that exists is a user-facing setting
  written to one of the DataStore files above. No remote kill-switches, no A/B framework.
- **No remote config** — the app does not phone home for configuration. The only
  outbound network call is to the user-supplied Stash server.
- **No analytics / telemetry opt-in toggle** — because there is no analytics or
  telemetry SDK to opt into. Crashes are not exfiltrated. (`activityTracking` only
  controls play/resume/finish events sent to the user's own Stash server, not anywhere
  else.)
- **No build-time secret keys** — the app ships no API tokens, no third-party SDK
  keys, no signing-time secret material beyond the release keystore.

If you add a config surface that breaks any of these properties, document the rationale
in the PR description and update this section.

---

## Provenance

Generated from direct reads of:
`gradle.properties`, `gradle/wrapper/gradle-wrapper.properties`, `keystore.properties.example`,
`.gitignore`, `bootstrap.sh`, `build.gradle.kts`, `app/build.gradle.kts`,
`build-logic/convention/src/main/kotlin/io/stashapp/android/buildlogic/KotlinAndroid.kt`,
`config/detekt/detekt.yml`, and every file under
`core/data/src/main/java/io/stashapp/android/core/data/prefs/`.
