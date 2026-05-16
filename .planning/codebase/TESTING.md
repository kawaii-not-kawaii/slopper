# Testing Patterns

**Analysis Date:** 2026-05-16

## Current State Summary

**This project has essentially no automated test suite.** Verification today is overwhelmingly **manual device testing** (see `DEVICE_TESTING.md`), with a single instrumented macrobenchmark (`StashBaselineProfileGenerator`) being the only Kotlin test class in the repo.

| Test type | Status |
|-----------|--------|
| Unit tests (`src/test/`) | **None** — no `src/test/` directory exists in any module |
| Instrumentation tests (`src/androidTest/`) | **None** — no `src/androidTest/` directory exists in any module |
| Compose UI tests | **None** |
| Macrobenchmark (baseline profile) | **Yes** — `:baselineprofile` module |
| Manual smoke tests | **Yes** — checklist in `DEVICE_TESTING.md` |
| Static analysis (treated as test gate) | detekt + ktlint + Android lint + OWASP dependency-check |

**Implication for new features:** Add tests when implementing them. The Gradle setup is wired for unit and instrumentation tests via the convention plugins; you just need to create the `src/test/` and `src/androidTest/` source roots and add deps.

## Test Frameworks Available in the Version Catalog

Declared in `gradle/libs.versions.toml`:

| Library | Version | Purpose |
|---------|---------|---------|
| `junit:junit` | `4.13.2` | JUnit 4 (used by macrobenchmark; default for unit tests) |
| `androidx.test.ext:junit` | `1.2.1` | AndroidX JUnit runner (`AndroidJUnit4`) |
| `androidx.test.uiautomator:uiautomator` | (catalog ref `uiAutomator`) | UI Automator for cross-app/system UI driving |
| `androidx.benchmark:benchmark-macro-junit4` | (catalog ref `benchmark`) | Macrobenchmark runner — used by `:baselineprofile` |

**NOT yet present in the catalog (add if introducing tests):**
- Truth / AssertK / Kotest assertions
- MockK
- Mockito
- Turbine (Flow assertions)
- `kotlinx-coroutines-test`
- `androidx.compose.ui:ui-test-junit4`, `ui-test-manifest`
- `androidx.paging:paging-testing`
- Robolectric

## Baseline Profile Module — the Only Existing Test

**Location:** `baselineprofile/src/main/java/io/stashapp/android/baselineprofile/StashBaselineProfileGenerator.kt`

**Module build file:** `baselineprofile/build.gradle.kts`
- Applies `alias(libs.plugins.android.test)` + `alias(libs.plugins.androidx.baselineprofile)`
- `compileSdk = 35`, `minSdk = 28` (baseline profiles are no-ops below P)
- `testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"`
- `targetProjectPath = ":app"` — macrobenchmark runs against the installed app APK
- `experimentalProperties["android.experimental.self-instrumenting"] = true`
- `baselineProfile { useConnectedDevices = true }` — uses whatever device is plugged in via adb
- Suppresses benchmark errors for `EMULATOR,DEBUGGABLE,UNLOCKED` so dev devices work

**Test structure (the canonical example):**
```kotlin
@RunWith(AndroidJUnit4::class)
class StashBaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(packageName = TARGET_PACKAGE) {
        pressHome()
        startActivityAndWait()
        device.wait(Until.hasObject(By.pkg(TARGET_PACKAGE).depth(0)), 5_000)
        // ... drive cold start → grid scroll → detail open/back
    }

    private companion object {
        const val TARGET_PACKAGE = "io.stashapp.android"
    }
}
```

**Run:**
```bash
./gradlew :app:generateBaselineProfile     # generates app/src/release/generated/baselineProfiles/baseline-prof.txt
```
The profile is committed and packaged into release APKs by `androidx.baselineprofile`. Regenerate before major releases / Compose bumps / large feature drops — see `baselineprofile/README.md`.

**Why not in CI:** Slow, device-specific, requires a configured Stash server on the device (per `baselineprofile/README.md` "Why not auto-run in CI?" section).

## App Build Configuration Relevant to Tests

`app/build.gradle.kts`:
- A `benchmark` build type exists (lines 60-66) — `initWith(release)`, `isMinifyEnabled = true`, `isDebuggable = false`, with `matchingFallbacks += "release"`. This is what the macrobenchmark runs against.
- The `release` `signingConfig` falls back to `debug` if no `keystore.properties` exists, so `assembleRelease` + benchmark profile generation work on a fresh clone for smoke testing.

## Recommended Test Layout (for when tests are added)

Match the Now-in-Android multi-module pattern that this codebase mirrors.

### Unit tests — `src/test/java/...`

Location per module:
- `core/<layer>/src/test/java/io/stashapp/android/core/<layer>/`
- `feature/<name>/src/test/java/io/stashapp/android/feature/<name>/`

Naming: `<ClassUnderTest>Test.kt` (e.g. `DetailViewModelTest.kt`, `SceneMapperTest.kt`, `SceneFilterTest.kt`).

Suggested deps to add to `gradle/libs.versions.toml` and apply via each module's `build.gradle.kts`:
```kotlin
testImplementation(libs.junit)
testImplementation(libs.kotlinx.coroutines.test)
testImplementation(libs.turbine)           // Flow assertions
testImplementation(libs.mockk)             // mocking
testImplementation(libs.truth)             // assertions
```

### Instrumented tests — `src/androidTest/java/...`

Location per module:
- `app/src/androidTest/java/io/stashapp/android/`
- `feature/<name>/src/androidTest/java/io/stashapp/android/feature/<name>/`

Use for:
- Compose UI tests (`createAndroidComposeRule<MainActivity>()`)
- Hilt-injected screen tests (`@HiltAndroidTest` + `HiltTestApplication`)
- DataStore / encrypted-prefs tests (`ConnectionStore`, `UiPreferences`, `PlayerPreferences` in `core/data/prefs/`)

Test runner is already implied by the convention plugins — declare in module `defaultConfig`:
```kotlin
testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
```
For Hilt: `dagger.hilt.android.testing.HiltTestApplication` via a custom `AndroidJUnitRunner` subclass.

### Compose UI tests

Recommended approach for the existing screens:
- `DetailScreenTest`, `LibraryScreenTest`, `ConnectionScreenTest`, etc.
- Inject fakes for `SceneRepository`, `BrowseRepository`, `ConnectionRepository` (already interface-based in `core/domain/`).
- Use `createAndroidComposeRule<HiltTestActivity>()` with `@HiltAndroidTest`.

## Mocking Strategy (Recommended)

The domain layer is **already interface-driven**, which makes fakes preferable to mocks for most cases:
- `SceneRepository`, `BrowseRepository`, `ConnectionRepository` (in `core/domain/`) — implement `FakeSceneRepository` in test source.
- Hilt `@Binds` swap to a test module via `@TestInstallIn(replaces = [DataModule::class], components = [SingletonComponent::class])`.

When mocking is needed (e.g. `ApolloClient`, low-level `OkHttpClient`), use **MockK** — it's the idiomatic Kotlin choice and handles `suspend` and `final` classes natively.

For Flows: **Turbine** (`flow.test { awaitItem() ... }`) is the standard. Combine with `kotlinx-coroutines-test`'s `runTest { }` and `StandardTestDispatcher`.

For Paging 3: `androidx.paging:paging-testing` provides `asSnapshot { }` for `Flow<PagingData<T>>`.

## Coroutines Test Setup

ViewModels use `viewModelScope` (Main dispatcher). For tests:
```kotlin
@Before fun setUp() = Dispatchers.setMain(StandardTestDispatcher())
@After  fun tearDown() = Dispatchers.resetMain()
```

Inject dispatchers via the existing Hilt qualifiers (`@IoDispatcher`, `@DefaultDispatcher`, `@MainDispatcher` defined in `core/common/src/main/java/io/stashapp/android/core/common/di/Dispatchers.kt`) so test modules can swap them for `UnconfinedTestDispatcher`.

## Coverage Tooling

**None currently configured.** No JaCoCo / Kover plugin in `build.gradle.kts` or any module.

To add Kotlinx **Kover** (recommended over JaCoCo for Kotlin):
1. Add `org.jetbrains.kotlinx.kover` plugin alias to `gradle/libs.versions.toml`.
2. Apply at root `build.gradle.kts` and merge per-module reports.
3. Target 80% per global standards (`~/.claude/rules/testing.md`).

## CI Test Setup

**No CI workflows exist** — `.github/` directory is absent. Test gates today live entirely in Gradle tasks invoked manually:

```bash
./gradlew ktlintCheck
./gradlew detekt
./gradlew lint
./gradlew dependencyCheckAnalyze --no-configuration-cache    # OWASP — note: incompatible with config cache
./gradlew :app:assembleDebug
./gradlew :baselineprofile:connectedBenchmarkAndroidTest     # only with a connected device
```

The `dependencyCheck` task in the root `build.gradle.kts` (lines 21-32) is the only **build-failing** security gate — fails on CVSS ≥ 7.0 (HIGH+). Suppressions live in `config/owasp-suppressions.xml`.

**Recommended CI matrix** (when adding GitHub Actions):
1. `ktlintCheck` + `detekt` (fast — ~seconds)
2. `lint` (warningsAsErrors override for CI per `app/build.gradle.kts` comment)
3. Unit tests across all modules: `./gradlew test`
4. Assemble debug: `./gradlew :app:assembleDebug`
5. Connected tests on a managed device (GMD): `./gradlew connectedDebugAndroidTest`
6. OWASP `dependencyCheckAnalyze` — gate on HIGH+ (already configured)
7. Baseline profile regen: manual / nightly only — not per-PR.

## Manual Device Testing

`DEVICE_TESTING.md` (project root) is the source of truth for pre-release verification. It defines a phased checklist:

1. **Connection** (5 min) — URL + API key, test, persistence across relaunch
2. **Library + filter** (5 min) — grid load, pagination, search, filter sheet, badge, reset
3. **Scene detail** (5 min) — hero, metadata, performers, tags, markers, rating, organize, O-counter
4. **Player single scene** (10 min) — playback, controls, gestures, PiP, resume restore
5. **Player queue** (10 min) — long-press to queue, skip, shuffle, repeat modes
6. **Marker seek** (2 min) — marker jump
7. **Resume sync-back** (5 min) — `resume_time`, `play_count`, `play_history` on server
8. **Browse entities** (5 min) — Performers / Studios / Tags screens
9. **Settings** (2 min) — codec status, disconnect

Each item is a checkbox; failure modes are catalogued in the "Common failure modes" table (Apollo schema drift, image 401, decoder missing, KSP regen, network reachability, stale cache).

**Logcat filter** for sessions:
```bash
adb logcat -v color -s 'StashApp:V' 'ExoPlayer:I' 'Apollo:V' 'OkHttp:I'
```

When adding features that touch any of the listed flows, update `DEVICE_TESTING.md` with new checkboxes — it's the closest thing this project has to an integration test suite.

## Testing Anti-Patterns to Avoid Here

1. **Do not mock the `ApolloClient` directly** in repository tests — use the `MockServerHandler` / `MockServer` from `apollo-testing-support` (already pulled in transitively via Apollo). Otherwise tests become coupled to call structure rather than GraphQL semantics.
2. **Do not test Compose layouts pixel-perfectly** — use `onNodeWithText` / `onNodeWithContentDescription` semantics, not screenshot diffing (no screenshot framework configured).
3. **Do not assert on `_state.value` directly** in VM tests — collect from the public `state: StateFlow` with Turbine so emission ordering is validated.
4. **Avoid `runBlocking`** in tests — `kotlinx-coroutines-test`'s `runTest { }` properly skips delays and integrates with `Dispatchers.setMain`.

---

*Testing analysis: 2026-05-16*
