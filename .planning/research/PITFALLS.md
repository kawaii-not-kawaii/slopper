# Pitfalls Research

**Domain:** Android Compose multi-module modernization (brownfield, 2026)
**Researched:** 2026-05-16
**Confidence:** HIGH (Context7-equivalent sources: Android Developers, kotlinlang.org, AndroidX release notes, kotlinx.serialization issue tracker, AGP 9.0 release notes). Project-specific risk inference is MEDIUM.

> Scope is **Slopper's** modernization: AGP 8.7.3 → current stable, Kotlin 2.1.0 → current stable, Compose BOM 2024.12.01 → current stable, Hilt 2.53.1 → current, KSP 2.1.0-1.0.29 → matching, `targetSdk` already 35, `minSdk` pinned at 26 (28 for `:baselineprofile`). All pitfalls below are filtered for this exact baseline — generic Android advice is excluded.

Phase abbreviations used throughout:
- **D** = Dependencies & Build phase (AGP/Gradle/Kotlin/KSP/Hilt/Compose BOM bumps, version catalog hygiene, CI cache)
- **G** = Guidelines phase (edge-to-edge, predictive back, foreground service, scoped storage where applicable)
- **P** = Performance phase (baseline profile journey expansion, macrobenchmark CI, R8/minify, strong skipping verification)
- **L** = Polish phase (detekt/ktlint rule drift, lint-baseline, docs, restoration leftovers)

## Critical Pitfalls

### Pitfall 1: JDK toolchain mismatch after AGP bump

**What goes wrong:**
AGP 9.x raises the **minimum JDK to run Gradle** to JDK 17, and recent Kotlin (2.1.20+) compiler tooling assumes JDK 17+. Slopper already sets `JavaVersion.VERSION_17` and `JvmTarget.JVM_17` in `KotlinAndroid.kt`, but the **Gradle daemon JDK** is implicit (whatever `JAVA_HOME` happens to be) and is not pinned via a `kotlin { jvmToolchain(17) }` block or `org.gradle.java.installations.fromEnv`. Bumping AGP without pinning the toolchain produces opaque "Unsupported class file major version 65 / 67" errors on contributor machines and CI.

**Why it happens:**
Convention plugins enforce `compileOptions.targetCompatibility = 17` but not the daemon JDK. AGP/Kotlin/Compose-compiler are each tested against a specific JDK pair (compile vs. run).

**How to avoid:**
1. Add `kotlin { jvmToolchain(17) }` to the `stash.android.application` / `stash.android.library` / `stash.android.feature` convention plugins.
2. Pin the wrapper-daemon JDK in `gradle.properties` via `org.gradle.java.installations.auto-download=false` plus a documented JDK 17 install in `bootstrap.sh`.
3. After bump, run `./gradlew -q javaToolchains` and verify a single resolved JDK.

**Warning signs:**
- `Unsupported class file major version` errors during `:buildSrc` or `:build-logic:convention` compile.
- `error: invalid source release` from KSP / Hilt processor.
- Different lint/detekt outputs on dev vs. CI for the same SHA.

**Phase to address:** **D** — first task, before any version bump.
**Severity:** High.

---

### Pitfall 2: Configuration cache breakage from OWASP `dependencyCheck`

**What goes wrong:**
`config/owasp-suppressions.xml` is wired but the plugin **is incompatible with the Gradle configuration cache** — `STACK.md` already notes "Runs with `--no-configuration-cache`". Bumping AGP/Gradle re-enables stricter config-cache validation and surfaces *other* plugins that quietly mutate task state at execution time. After a Gradle 8 → 9 jump, expect `cannot serialize Gradle script object references of type Project` from at least one of: `dependencyCheck`, `ktlint`, `detekt`.

**Why it happens:**
The current `build.gradle.kts` mixes config-cache-friendly and config-cache-hostile plugins behind a global toggle. Once Gradle hardens cache validation, the build fails wholesale instead of silently degrading.

**How to avoid:**
1. **Isolate** the OWASP task to a dedicated init script or a `--no-configuration-cache` invocation in CI only; never run it as part of `./gradlew check`.
2. Verify per-plugin compatibility tables (`org.gradle.unsafe.configuration-cache=warn` first, then `=true`).
3. Run `./gradlew assembleDebug --configuration-cache --no-build-cache --rerun-tasks` after every dep bump in this phase to catch new violators.

**Warning signs:**
- `0 problems were found storing the configuration cache` flips to `N problems were found`.
- `invocation of 'Task.project' at execution time is unsupported`.
- A previously-fast incremental build suddenly does a full re-configure.

**Phase to address:** **D**.
**Severity:** High.

---

### Pitfall 3: KSP version pin drifts away from Kotlin version

**What goes wrong:**
Slopper uses `ksp = "2.1.0-1.0.29"`. KSP version is **`<kotlin>-<ksp>` and must match the Kotlin version exactly**. A common bump mistake: update `kotlin = "2.1.20"` in `libs.versions.toml` but forget the KSP coordinate suffix. Symptom is `KSP support version X.Y.Z does not match the Kotlin compiler version A.B.C` — but worse, with `mismatch="warning"` the Hilt aggregating processor still runs and emits stale generated code, producing `MissingBindingsException` at runtime for new `@HiltViewModel`s.

**Why it happens:**
Two coordinates encode the same version; version catalogs can't enforce the link.

**How to avoid:**
1. In `libs.versions.toml`, declare `kotlin = "2.1.X"` and `ksp = "2.1.X-1.0.YY"` next to each other with a comment: `# MUST match kotlin = above`.
2. Add a `build-logic` assertion in the convention plugin: read both versions and fail the build if the prefix differs.
3. Set `ksp { arg("mismatch","fail") }` (or the equivalent in current KSP) so a mismatch hard-fails rather than warns.

**Warning signs:**
- Random `MissingBindingException` in `:app` after a clean install but not on the second run.
- `Hilt_*` generated classes time-stamped older than their source `@HiltViewModel`.
- KSP warning lines about version mismatch in `:core:data` / `:core:network` compile output.

**Phase to address:** **D**.
**Severity:** High.

---

### Pitfall 4: Compose Compiler plugin coordinate split

**What goes wrong:**
Since Kotlin 2.0 the Compose compiler ships as the `org.jetbrains.kotlin.plugin.compose` Gradle plugin (Slopper already applies this via `kotlin-compose`). When bumping Kotlin, three things must move in lockstep:
1. `kotlin = "X.Y.Z"`
2. `org.jetbrains.kotlin.plugin.compose` version (catalog `kotlin-compose` alias)
3. The Compose BOM (`composeBom`) — which controls runtime/foundation/material3/ui — is **independent** of (1) and (2). The compiler plugin version pins to Kotlin; the runtime pins to BOM.

Bumping the BOM but not the compiler plugin (or vice versa) yields the cryptic `Compose Compiler Y is not compatible with Kotlin X` or, more dangerously, **silent miscompilation of `@Composable inline fun` boundaries**.

**Why it happens:**
The Compose ecosystem now has three coupled versions where there used to be one. Pre-2.0 the compiler artifact had its own version axis pinned in `composeOptions { kotlinCompilerExtensionVersion = ... }`; that block is **dead** and copy-pasted upgrade guides still mention it.

**How to avoid:**
1. Confirm `composeOptions { kotlinCompilerExtensionVersion = ... }` is **not** present in `AndroidComposeConventionPlugin.kt` (it isn't today — keep it that way).
2. Use the Compose-to-Kotlin compatibility map ([Android Developers](https://developer.android.com/jetpack/androidx/releases/compose-kotlin)) to pick a BOM whose runtime is built against the target Kotlin's compiler.
3. After bump, run `./gradlew :app:dependencies --configuration releaseRuntimeClasspath | grep compose` — verify only one `compose-compiler` version appears (it's resolved transitively from the plugin; should not be in `dependencies {}`).

**Warning signs:**
- Build succeeds but `@Composable` lambdas behave as if non-restartable.
- `e: ... unresolved reference: ComposableTargetMarker`.
- Two different `compose-compiler` artifacts in the dependency report.

**Phase to address:** **D**.
**Severity:** High.

---

### Pitfall 5: Strong skipping changes recomposition semantics for `PlayerScreen`

**What goes wrong:**
Strong skipping is **on by default in Kotlin 2.0.20+ / Compose Compiler 1.5.4+**, so Slopper is already running with it. When jumping to a newer Compose BOM, the **runtime** behavior of strong skipping can shift — auto-remembered lambdas with unstable captures (e.g. `ViewModel`, `ExoPlayer`) now memoize, meaning a `LaunchedEffect(playerState)` keyed on an unstable type stops re-triggering. `PlayerScreen.kt` (1122 lines, already flagged) is the highest-risk surface: it captures `Activity`, `ExoPlayer`, gesture state, and a `lastInteraction` timestamp inside Compose lambdas.

**Why it happens:**
Strong skipping flips lambda memoization from "stable captures only" to "all captures". `LaunchedEffect` keys that used to *look* fresh on each recomposition (because the lambda was a new instance) now stay stable, and the effect doesn't re-launch.

**How to avoid:**
1. Audit every `LaunchedEffect`, `DisposableEffect`, `produceState` in `feature/player` and `feature/library`. Replace `LaunchedEffect(viewModel) { ... }` with `LaunchedEffect(Unit) { viewModel.events.collect { ... } }` (the lambda contents — not the lambda identity — own the work).
2. For unstable captures that should still trigger effects, key on a **stable derived value** (`LaunchedEffect(playerState.position) { ... }`, not the player itself).
3. Note: `LazyListScope` lambdas (`items { }`) are **not** auto-memoized — Slopper's library/browse grids are unaffected here, but custom `LazyColumn` content lambdas that recreate `remember { ... }` blocks each compose still need explicit `key()`.

**Warning signs:**
- PiP / frame-rate / screen-on side effects in `PlayerScreen.kt` stop firing after a Compose BOM bump.
- `applyVideoFrameRate` (currently called from `AndroidView.update`, see `CONCERNS.md`) no longer reacts to fps changes.
- Manual `DEVICE_TESTING.md` row "Resume restore" or "PiP enter/exit" regresses with no obvious code change.

**Phase to address:** **D** (verify after Compose BOM bump) + **L** (player refactor when splitting `PlayerScreen.kt`).
**Severity:** High — and not catchable by lint.

---

### Pitfall 6: Stale baseline profile is worse than no profile

**What goes wrong:**
Slopper ships `app/src/release/generated/baselineProfiles/baseline-prof.txt`. After dependency bumps (especially Compose BOM, Media3, Apollo, Coil), the AOT-compiled method handles inside the profile point at **renamed/inlined methods that no longer exist in the new bytecode**. ART silently falls back to JIT for those entries, *plus* spends startup time loading + verifying the stale profile. Net effect: startup gets **slower** than not shipping a profile at all, and the macrobenchmark "wins" you ship are negative.

**Why it happens:**
The baseline profile is a list of method descriptors. Library upgrades change inline boundaries and method signatures. The `:baselineprofile` module's `useConnectedDevices = true` (`STACK.md`) means regeneration is manual — easy to skip during a dep sweep.

**How to avoid:**
1. **Regenerate the profile in the same PR that bumps any of**: Compose BOM, Media3, Apollo, Coil, Kotlin, Hilt, Paging.
2. Configure a Gradle-managed device (`Pixel 6 API 34`) so regeneration is reproducible — `CONCERNS.md` already flags this as deferred work; do it before the perf phase, not after.
3. In CI, fail the build if `baseline-prof.txt` is older than `gradle/libs.versions.toml` (a simple `find -newer` check).
4. Validate via macrobenchmark: profile-installed vs. no-profile cold-start delta should be **≥ 15%**. If it's <5%, the profile is dead.

**Warning signs:**
- Macrobenchmark cold-start p50 *increases* after a "no logic change" dep bump.
- `adb shell cmd package compile -m verify <pkg>` plus a comparison run shows little difference (profile is already effectively unused).
- `dumpsys package <pkg>` shows `installer-profile` flag but `compilation_filter=verify` (profile not actually applied).

**Phase to address:** **P** (regenerate + GMD wiring), but **gate every D-phase PR on profile freshness**.
**Severity:** High — and stealthy; users feel it, your benchmarks don't unless you check.

---

### Pitfall 7: Edge-to-edge enforcement breaks `PlayerScreen` and `FilterSheet` insets

**What goes wrong:**
With `compileSdk = 35` and `targetSdk = 35`, edge-to-edge is **enforced** on Android 15 devices (no opt-out via `setStatusBarColor` / `setNavigationBarColor` — those calls are no-ops). The two screens at risk in Slopper:
1. **`PlayerScreen.kt`** — gesture controls and the top/bottom bars assume the system bars are inset from the surface. Edge-to-edge means the player surface now extends under the cutout/status bar, but the **touch targets for gestures** still register inside the system-bar region (causing accidental notification-shade pulls during scrub).
2. **`FilterSheet.kt`** (531 lines) — bottom-sheet content can render under the 3-button navigation bar on devices that still have it (older Pixels, most non-Pixel OEMs even on Android 15).

Calling `enableEdgeToEdge()` alone is **not enough** — every Compose screen needs `WindowInsets.safeDrawing` / `WindowInsets.systemBars` applied at the right level.

**Why it happens:**
The default `Scaffold` in Compose `material3` 1.3+ consumes insets correctly; custom layouts (the player) and `ModalBottomSheet` content (`FilterSheet`) do not.

**How to avoid:**
1. Confirm `MainActivity` calls `enableEdgeToEdge()` **before** `setContent { }`. (Required since AndroidX Activity 1.8.0.)
2. In `PlayerScreen`, wrap the player surface in `Box(Modifier.fillMaxSize())` but put control overlays inside a `Box(Modifier.safeDrawingPadding())` — never apply `safeDrawing` to the SurfaceView itself or you lose edge-to-edge video.
3. For `ModalBottomSheet`, use `contentWindowInsets = { WindowInsets.navigationBars }` (Compose 1.7+) so the sheet content avoids the nav bar.
4. Test the manual `DEVICE_TESTING.md` checklist on **both** a 3-button-nav device and a gesture-nav device; the regressions look different.

**Warning signs:**
- "Phantom" notification-shade pulls during player scrub.
- Filter sheet's "Apply" button is half-hidden behind the nav bar.
- Status-bar icons are unreadable over the player's top bar.

**Phase to address:** **G**.
**Severity:** High (player) / Medium (filter sheet).

---

### Pitfall 8: Predictive back breaks custom Compose back handlers

**What goes wrong:**
Slopper almost certainly uses `BackHandler { ... }` in `PlayerScreen` to exit fullscreen / dismiss the queue, and in `FilterSheet` to dismiss. On Android 14+ with predictive back **opted in**, the system needs to know the back action *before* the gesture completes so it can render the preview. `BackHandler { onDismiss() }` works (it uses `OnBackPressedDispatcher` internally), but a `BackHandler(enabled = isQueueOpen)` whose `enabled` flag flips during the gesture (e.g. dismiss animation starts, `enabled` goes false mid-swipe) leaves the user in a broken state — the predictive preview shows the wrong destination.

Also: as of Compose Multiplatform 1.10 / AndroidX Activity 1.10+, `PredictiveBackHandler` has been **deprecated** in favor of `NavigationBackHandler`/`NavigationEventState` ([migration guide](https://medium.com/@santimattius/goodbye-predictivebackhandler-how-to-migrate-to-the-new-navigation-event-in-compose-6e2f294f5ddb)). If Slopper hasn't adopted `PredictiveBackHandler` yet (likely — codebase was just restored), skip it entirely and go straight to the new API.

**Why it happens:**
Pre-predictive-back, `BackHandler` callbacks ran atomically on button press. Predictive-back makes back a multi-phase gesture (start → progress → commit/cancel).

**How to avoid:**
1. In `AndroidManifest.xml`, set `android:enableOnBackInvokedCallback="true"` on `<application>`. (May already be set — verify.)
2. Replace any animated dismissal driven by `BackHandler` with `NavigationBackHandler` so cancel returns to the prior state without state divergence.
3. Never mutate `enabled` mid-gesture; if dismissal is conditional, gate the *action* inside the lambda instead.
4. Test predictive back on Android 14+ with developer-options "Predictive back animations" toggled on — Pixel default behavior differs from OEM.

**Warning signs:**
- Back swipe preview shows the wrong screen.
- Player exits to a black screen instead of the library on back-from-queue.
- `BackHandler` callback fires twice on cancel.

**Phase to address:** **G**.
**Severity:** Medium.

---

### Pitfall 9: R8 keep rules invalidated by kotlinx.serialization 1.8+ / Apollo upgrade

**What goes wrong:**
`app/proguard-rules.pro` has keep rules for `io.stashapp.android.graphql.**` (Apollo) and kotlinx.serialization companions. Two upgrade-time traps:
1. **kotlinx.serialization 1.9.0+** changed its bundled R8 rules; consumers that hand-rolled keep rules (Slopper does) now get R8 warnings *about kotlinx-serialization's own rules conflicting with the consumer rules* (see [issue #3033](https://github.com/Kotlin/kotlinx.serialization/issues/3033), [issue #2392](https://github.com/Kotlin/kotlinx.serialization/issues/2392)). 1.10.0 revised the rules again.
2. **Apollo 4.x** generates Kotlin data classes with reflection-free serialization, but third-party adapters and `@JsonClass`-style annotations may still hit R8. Apollo's own consumer rules cover the generated code; rules you wrote for Apollo 3.x may now be **over-broad** keep rules that defeat shrinking.

**Why it happens:**
Hand-written keep rules outlive the libraries they were written for. R8 *warns* but does not fail by default — the broken release ships.

**How to avoid:**
1. After bumping kotlinx.serialization / Apollo / Hilt, run `./gradlew :app:assembleRelease -Pandroid.enableR8.fullMode=true` and **read every R8 warning** — do not pipe to `/dev/null`.
2. Delete keep rules for libraries that now ship their own `consumer-rules.pro` (kotlinx.serialization since 1.6, Apollo since 3.x, Hilt since 2.40). Replace with `-printconfiguration r8-final-config.txt` and inspect what R8 actually retained.
3. Add an instrumented smoke test that opens each screen on a **minified** build (`benchmark` build type is perfect for this — it's already `minifyEnabled=true` per `STACK.md`).
4. Track `app-release.apk` size delta per bump; a sudden growth means a new over-broad keep rule snuck in.

**Warning signs:**
- `Missing class kotlinx.serialization.KSerializer (referenced from: ...)` in `r8-warnings.txt`.
- `release` APK works but `benchmark` crashes with `SerializationException: Polymorphic serializer was not found`.
- APK size grows >5% on a "dep-only" bump.

**Phase to address:** **D** (bump-time check) and **P** (release-build smoke test pre-baseline-profile regen).
**Severity:** High — crashes only in release builds, which are not in your manual test loop.

---

### Pitfall 10: Dependency convergence — multiple AndroidX versions resolved

**What goes wrong:**
Compose BOM controls `androidx.compose.*`, but **`androidx.activity`, `androidx.lifecycle`, `androidx.navigation`, `androidx.paging`** are explicit versions in `libs.versions.toml` and pulled in transitively by Compose / Hilt / Media3 / Coil. Mismatch examples seen during 2025/2026 upgrades:
- Compose BOM 2025.x wants `androidx.activity:activity-compose:1.10.x`, but `libs.versions.toml` pins `1.9.3`. Gradle picks 1.10 (highest), which silently disables Slopper's lower-pinned version's behaviors.
- Hilt 2.5x transitively pulls `androidx.lifecycle:lifecycle-viewmodel:2.9.x`; Slopper pins `2.8.7`. The resolved 2.9 breaks `SavedStateHandle` keys that worked under 2.8.

**Why it happens:**
Version catalogs encode intent but Gradle resolves to the highest version in the graph. The BOM only covers its own modules.

**How to avoid:**
1. Run `./gradlew :app:dependencies --configuration releaseRuntimeClasspath > deps.txt` **before** and **after** each bump; diff for unexpected version shifts.
2. For AndroidX artifacts that have a BOM-equivalent stability story (lifecycle, activity), pin the catalog version to the BOM-implied version and use `strictly("X.Y.Z")` if the resolver insists on bumping it.
3. Add a `dependencyResolutionManagement { failOnVersionConflict() }` block in a separate "audit" build, run weekly — gates against silent drift.

**Warning signs:**
- `Cannot find @Composable function ... in androidx.compose.material3:material3:X.Y` where X.Y is the BOM-resolved version, not what you wrote.
- `SavedStateHandle.get<T>("key")` returns null where it used to return a value.
- A new `@HiltViewModel` parameter type isn't auto-injected (Hilt-Activity version skew).

**Phase to address:** **D**.
**Severity:** Medium.

---

### Pitfall 11: Hilt aggregating processor blows incremental builds

**What goes wrong:**
Hilt's annotation processor is **aggregating** — KSP must re-process every `@HiltViewModel`, `@Module`, `@AndroidEntryPoint` whenever any of them changes ([KSP issue #511](https://github.com/google/ksp/issues/511)). Bumping Hilt and KSP simultaneously can also surface [google/dagger#4063](https://github.com/google/dagger/issues/4063) — incremental compilation NPE when touching a class hierarchy under `@AndroidEntryPoint`. Slopper has Hilt in `app/`, `core/data/`, `core/network/`, and every `feature/*` — the blast radius is the entire module graph.

**Why it happens:**
Aggregating output design + KSP's incremental contract.

**How to avoid:**
1. Stay on the **latest** Hilt 2.5x stable when bumping; older versions in the 2.4x line have unfixed incremental bugs.
2. In `gradle.properties`, keep `ksp.incremental=true` and `ksp.incremental.intermodule=true` — but if NPEs appear, set both to `false` until Hilt/Dagger releases the fix.
3. Cache `build/generated/ksp/` separately in CI so a clean is cheap.
4. Run a `./gradlew :app:assembleDebug` smoke after every Hilt bump, then edit one `@HiltViewModel` and re-run — if the incremental build wedges, you've hit it.

**Warning signs:**
- `java.lang.NullPointerException at dagger.hilt.processor.internal.aggregateddeps.AggregatedDepsGenerator`.
- Incremental build times balloon (cold build was 90s, incremental edit is 60s instead of 10s).
- `Hilt_*` generated classes get rebuilt on every compile, not just on dep-graph changes.

**Phase to address:** **D**.
**Severity:** Medium.

---

### Pitfall 12: Detekt / ktlint rule drift after Kotlin bump

**What goes wrong:**
ktlint 1.3.1 (Slopper's pin) and detekt 1.23.7 were both built against Kotlin 2.0.x. Kotlin 2.1.x / 2.2.x introduces context parameters (replacing `-Xcontext-receivers`, which Slopper currently uses in `KotlinAndroid.kt`!), and detekt/ktlint pre-2.0-aware rule sets either crash (`Could not load type ...`) or false-positive on the new syntax.

**Why it happens:**
Static analyzers ship their own Kotlin compiler embedded. The embedded version lags Kotlin proper by 1–2 minors.

**How to avoid:**
1. Bump ktlint Gradle plugin to a version with the matching ktlint runtime *first* (12.3+ → ktlint 1.6+).
2. Bump detekt to a version that bundles the matching Kotlin compiler (1.23.8+ for Kotlin 2.1; 2.0.0+ for Kotlin 2.2 — verify on detekt release page at bump time).
3. **Important**: `-Xcontext-receivers` is being replaced; the compiler arg currently in `KotlinAndroid.kt` will warn (eventually error) on Kotlin 2.2. Decide: drop the arg, or migrate the few usage sites to context parameters.
4. Run `./gradlew detekt ktlintCheck` immediately after the Kotlin bump, **before** any code changes — catches drift cleanly.

**Warning signs:**
- `org.jetbrains.kotlin.com.intellij.psi.PsiInvalidElementAccessException` inside detekt output.
- ktlint reports unfixable `standard:no-multi-spaces` errors on `when`-with-context-parameters code that compiles fine.
- A clean tree fails `ktlintCheck` after dependency-only bumps.

**Phase to address:** **D** (during Kotlin bump) + **L** (re-baseline detekt config).
**Severity:** Medium.

---

### Pitfall 13: Macrobenchmark variance masks real regressions

**What goes wrong:**
Slopper's `:baselineprofile` runs with `useConnectedDevices = true` on whatever phone is plugged in. Variance sources stack up: thermal throttling, foreground services running on the test device, background ART compilation of the *just-installed* APK, screen-on power state, network jitter against the real Stash server. Without explicit variance control, p50 deltas of ±20% are normal — well above the modernization phase's actual perf wins (typically 5–15%).

**Why it happens:**
Macrobenchmark is non-deterministic by design; it measures real device behavior.

**How to avoid:**
1. Pin a Gradle-managed device (Pixel 6 API 34) for **CI** runs; keep `useConnectedDevices` only for local exploratory measurement.
2. Use `CompilationMode.Partial(BaselineProfileMode.Require)` so the test fails if the profile isn't actually used (catches Pitfall 6 too).
3. Run **5+ iterations** per measurement (default is 5; verify), and report **p50 + p95**, not the mean.
4. Disable Wi-Fi / put device in airplane mode if the benchmark journey doesn't strictly need network; mock the Apollo response for cold-start measurements.
5. See [statistically rigorous macrobenchmarks](https://blog.p-y.wtf/statistically-rigorous-android-macrobenchmarks) — accept that anything <10% delta needs ≥30 iterations to be significant.

**Warning signs:**
- Same APK, same device, two runs 30 minutes apart: p50 differs by >15%.
- "Improvement" in CI vanishes on re-run.
- `compilation_filter=verify` in `dumpsys` after a run that should have applied the profile.

**Phase to address:** **P**.
**Severity:** Medium — flaky perf data leads to wrong refactor priorities.

---

### Pitfall 14: Version catalog — BOM vs. explicit version coordinates

**What goes wrong:**
Several Compose artifacts are pulled in via BOM (no version) and some via explicit catalog versions. A common mistake during a BOM bump is to write:

```toml
compose-material3 = { module = "androidx.compose.material3:material3", version.ref = "composeBom" }
```

— pointing the `version.ref` at the BOM coordinate, which works for the BOM but is wrong for the **artifact** (whose version comes from the BOM at resolution time, not the catalog). The correct shape is:

```toml
compose-material3 = { module = "androidx.compose.material3:material3" }  # no version — BOM controls it
```

If Slopper's catalog has mixed shapes today (worth auditing), a BOM bump can fail to actually pick up new artifact versions.

**Why it happens:**
BOM semantics in version catalogs are subtle. Explicit `version.ref` overrides the BOM; omitting `version` defers to the BOM.

**How to avoid:**
1. Audit `libs.versions.toml` — any `androidx.compose.*` artifact with a `version.ref` should drop it (unless intentionally pinned higher than the BOM).
2. Add a `:app:dependencyInsight --dependency compose-ui` step in CI to verify the BOM-controlled version actually resolves.
3. Document in a comment which catalog entries are BOM-controlled.

**Warning signs:**
- BOM bump produces zero version changes in `./gradlew dependencies` output.
- A new Compose API isn't available even after the BOM is updated to a version that has it.

**Phase to address:** **D**.
**Severity:** Low — wastes a debug hour, doesn't cause runtime bugs.

---

### Pitfall 15: CI cache invalidation cascade after Gradle / JDK changes

**What goes wrong:**
Slopper has no `.github/` yet (per `TESTING.md`), so this is forward-looking — but **the moment** CI is added in the modernization milestone, Gradle's build cache and the GitHub Actions cache need explicit cache keys that include Gradle version, JDK version, AGP version, and the wrapper hash. Caching only on `libs.versions.toml` hash means a Gradle wrapper bump produces stale cache hits → cryptic `OutputPropertyAssertionError` or `Inconsistent execution result` errors that only manifest in CI.

**Why it happens:**
Gradle's task outputs are version-sensitive. Cache hits across a Gradle major version flip silently corrupt builds.

**How to avoid:**
1. Compose the GitHub Actions cache key from `${{ hashFiles('gradle/wrapper/gradle-wrapper.properties', 'gradle/libs.versions.toml', '**/*.gradle.kts') }}`.
2. Use `gradle/gradle-build-action@v3` (or current `setup-gradle`) which handles this correctly out of the box.
3. After a Gradle wrapper bump, manually invalidate the CI cache once (push an empty commit with `[skip-cache]`-aware key change).
4. Pin the runner's JDK via `actions/setup-java@v4` with `java-version: 17` and `distribution: temurin` — don't rely on runner defaults.

**Warning signs:**
- CI green, local red on the same SHA (or vice versa).
- `Configuration cache state could not be reused` on every CI run.
- Random task-output mismatches that don't reproduce locally.

**Phase to address:** **D** (CI scaffold) + **L** (tighten caching as part of polish).
**Severity:** Medium.

---

### Pitfall 16: `nextlib-media3ext` version coupling to Media3

**What goes wrong:**
`STACK.md` notes Media3 is pinned to 1.9.x because "1.10 requires compileSdk 36" and `nextlib-media3ext` is `1.9.1-0.11.0`. The nextlib artifact's version prefix **must** match Media3's exactly — its native code is built against that ABI. Bumping Media3 to 1.10.x (when compileSdk moves) without finding a matching nextlib release will silently lose AC3/EAC3/DTS playback or, worse, crash with `UnsatisfiedLinkError` in the player.

**Why it happens:**
Native interop. The `nextlib-media3ext` maintainer rebuilds shortly after each Media3 release, but there's a lag window.

**How to avoid:**
1. **Don't bump Media3 in this milestone.** `PROJECT.md` says no `minSdk` bump and no module restructure; the `compileSdk 36 → Media3 1.10` coupling means this is a future-milestone concern.
2. If Media3 must move, confirm `io.github.anilbeesetti:nextlib-media3ext:<media3-version>-<nextlib-version>` exists on Maven Central first.
3. Mitigation already documented: `tools/ffmpeg-extension/` exists as an escape hatch; flag it as "live" before bumping Media3 (see `CONCERNS.md`).

**Warning signs:**
- `UnsatisfiedLinkError: dlopen failed` on player start.
- AC3/EAC3 audio tracks vanish; only AAC plays.
- `nextlib` artifact resolution fails with `No matching variant`.

**Phase to address:** **D** — explicitly *exclude* Media3 from the bump scope.
**Severity:** High **if** attempted, Low if scope-checked.

---

### Pitfall 17: Compose API removals/renames (December '25 → April '26)

**What goes wrong:**
Compose BOM 2025.12.00 onward removed `ScaleToBounds()` (replaced by `scaleToBounds()` modifier) and renamed several `Retain*` types (e.g. `RetainScope` → `RetainedValuesStore`) per the [April '26 release notes](https://android-developers.googleblog.com/2026/04/jetpack-compose-april-2026-updates.html). Slopper currently uses BOM `2024.12.01` — a jump to current stable crosses both rename boundaries. Test-side: v1 testing APIs (`UnconfinedTestDispatcher`-backed) were superseded by v2 (`StandardTestDispatcher`-backed). Slopper has **no Compose tests today**, so the test API churn is moot — but if tests are added during modernization, write them against v2 from day one.

**Why it happens:**
Compose APIs evolve aggressively in minor BOM releases.

**How to avoid:**
1. Use the [BOM-to-library mapping](https://developer.android.com/develop/ui/compose/bom/bom-mapping) to pick the target BOM, then read the changelogs for **every Compose artifact** between 1.7 and the target.
2. After bump, `./gradlew :app:compileReleaseKotlin` and address every deprecation warning — don't suppress.
3. If introducing Compose UI tests in this milestone, target the v2 testing API directly. Do not pull in `UnconfinedTestDispatcher`-based examples from older blogs.

**Warning signs:**
- `Unresolved reference: ScaleToBounds`.
- `RetainScope is deprecated` warning that's actually a hard error in newer versions.
- New Compose tests pass locally but hang in CI (v1/v2 dispatcher mismatch).

**Phase to address:** **D** (BOM bump) and any test-writing work.
**Severity:** Medium.

---

### Pitfall 18: `lint-baseline.xml` missing — every warning becomes "new"

**What goes wrong:**
`CONCERNS.md` already flags this: `app/build.gradle.kts` references `app/lint-baseline.xml` but the file doesn't exist on disk. When AGP and lint-checks bump versions, **new** lint checks light up. Without a baseline, the warningsAsErrors-in-CI policy (per `STACK.md`) will fail the build on the first dep bump — including for warnings that have nothing to do with the bump itself.

**Why it happens:**
The baseline was lost in the restoration commit. The lint config still expects it.

**How to avoid:**
1. **Before any dependency bump**, run `./gradlew :app:updateLintBaseline` on the current code state and commit the result. This freezes the *current* warning set as known-acceptable.
2. After each dep bump, run `./gradlew :app:lint` — anything new is genuinely a new issue.
3. Plan a separate "lint cleanup" pass in the polish phase to *shrink* the baseline back toward zero.

**Warning signs:**
- First `./gradlew lint` after a dep bump reports hundreds of issues.
- CI fails with `Found N new issues` where N is a 3-digit number.

**Phase to address:** **D** (regenerate baseline first thing) + **L** (shrink it).
**Severity:** Medium.

---

## Technical Debt Patterns

| Shortcut | Immediate Benefit | Long-term Cost | When Acceptable |
|----------|-------------------|----------------|-----------------|
| Disable strong skipping with `-P plugin:androidx.compose.compiler.plugins.kotlin:strongSkipping=false` to "fix" a regression | Player effects start working again instantly | Slopper loses the very recomposition wins the perf phase needs; locks the app into a deprecated mode that will be removed | **Never** — fix the effect keys instead (Pitfall 5) |
| Add `-keep class **` blanket R8 rules to silence warnings | Release build stops crashing | APK size grows, every shrinking benefit lost, real issues hidden | **Never** — narrow to the offending package |
| Keep `-Xcontext-receivers` after Kotlin 2.2 by suppressing the warning | Build keeps compiling | Will hard-fail on Kotlin 2.3; technical debt grows under a `@Suppress` carpet | Only as a temporary unblock during the bump PR, with a tracking issue |
| Regenerate baseline profile only "before releases" | Less device wrangling per PR | Stale profiles silently regress startup between releases (Pitfall 6) | Only if a CI freshness check (Pitfall 6 fix) is in place |
| Bump Compose BOM without bumping Compose Compiler plugin | Avoids touching the convention plugin | Subtle miscompilation, silent runtime breakage (Pitfall 4) | **Never** — they're a matched pair |
| Pin `ksp.incremental=false` permanently to avoid Hilt NPEs | Builds become deterministic | Every edit triggers full KSP re-run; build time doubles | Only as a temporary workaround pending a Hilt fix release |
| Skip `lint-baseline.xml` regeneration "for now" | Less file to commit | Lint becomes useless because everything is "new"; warningsAsErrors policy backfires | **Never** — it's a 30-second task |

## Integration Gotchas

| Integration | Common Mistake | Correct Approach |
|-------------|----------------|------------------|
| Apollo Kotlin 4.x normalized cache | Bumping `apollo-normalized-cache-sqlite` independently of `apollo-runtime` | Pin both in `libs.versions.toml` with the same `version.ref`; Apollo's modules are version-locked |
| Coil 3.x with OkHttp | Sharing the app-wide `OkHttpClient` with Apollo (which adds the `StashAuthInterceptor`) leaks the `Authorization`/`ApiKey` header to non-Stash hosts | Build a separate `OkHttpClient.Builder().build()` for Coil that adds the header **only** when the request URL matches the active Stash origin — Slopper already does this per `CONCERNS.md`; **don't regress it** during a Coil bump |
| Media3 + nextlib | Bumping Media3 without checking `nextlib-media3ext` availability | See Pitfall 16 — match versions in lockstep, escape hatch is `tools/ffmpeg-extension/` |
| Hilt + Compose Navigation | Calling `hiltViewModel()` inside a `navigation()` graph nested under a non-Hilt-aware parent | Use the explicit `hiltViewModel(viewModelStoreOwner = ...)` form; Hilt 1.2+'s scoping with Navigation 2.8+ has subtle behavior at graph boundaries |
| AndroidX Activity edge-to-edge | Calling `enableEdgeToEdge()` after `setContent { }` | Always before; otherwise insets aren't dispatched correctly on Android <15 |
| EncryptedSharedPreferences | Migrating the underlying file after a security-crypto bump without exporting/re-importing keys | `androidx.security:security-crypto:1.1.0` is itself deprecated upstream — track this; future bump *will* require migration plan, but is **out of scope** for this milestone |

## Performance Traps

| Trap | Symptoms | Prevention | When It Breaks |
|------|----------|------------|----------------|
| `applyVideoFrameRate` in `AndroidView.update` (already flagged in `CONCERNS.md`) | CPU wakes for every recomposition that touches the player surface | Move into `LaunchedEffect(targetFps) { surface.setFrameRate(targetFps, ...) }` | Visible on Pixel 4a-class devices during long playback sessions |
| Sprite thumbnail memory cache too small | Library grid re-decodes thumbnails on every scroll back | Configure Coil's `MemoryCache.Builder().maxSizePercent(0.25)` for ≥6GB-RAM devices, less for low-RAM | Becomes painful at >50-scene grids |
| Baseline profile missing player/filter-sheet journeys | Player and filter paths JIT on first use, manifesting as a 1–2s hitch on first open | Expand `StashBaselineProfileGenerator` (already on the docket per `CONCERNS.md`) | First time after install / data clear |
| Compose recomposition driven by unstable Apollo data classes | Library grid recomposes whole rows on any data update | Annotate query result types with `@Stable` / `@Immutable` *or* configure Compose Compiler's stability config file pointing at Apollo's package | Visible with `LayoutInspector` recomposition counts > 1 on no-op data refresh |
| OkHttp connection pool exhaustion during player + thumbnail concurrency (flagged in `CONCERNS.md` "Single ApolloClient instance") | Thumbnails stall when scrubbing the player | Verify `OkHttpClient.Builder().connectionPool(ConnectionPool(maxIdleConnections=10, ...))` is configured for both Apollo and Coil clients | At >5 concurrent in-flight requests |

## Security Mistakes

| Mistake | Risk | Prevention |
|---------|------|------------|
| Forgetting to re-bundle `consumer-rules.pro` after a kotlinx.serialization bump | API key serialization in `ConnectionStore` could be stripped, leading to plaintext fallback or crash | Always run release smoke on the `benchmark` build type after a serialization bump (Pitfall 9) |
| Re-enabling cleartext globally after edge-to-edge migration "to fix" a redirect | API key leaked over HTTP on hostile networks | Keep cleartext narrow per `network_security_config.xml`; do **not** widen during this milestone (already flagged in `CONCERNS.md` H2 review) |
| Bumping `androidx.security-crypto` past the deprecation cliff without migration | Encrypted prefs become unreadable; user must re-enter API key | Pin at 1.1.0 for this milestone; track the official replacement (DataStore-encrypted), schedule for a future milestone |
| New Hilt-injected `@AndroidEntryPoint` exposes a previously-private receiver | Implicit intent exposure post-Android 14 | Always declare `android:exported="false"` explicitly when adding any `<activity>`/`<receiver>`/`<service>` |

## UX Pitfalls

| Pitfall | User Impact | Better Approach |
|---------|-------------|-----------------|
| Edge-to-edge applied to player but bottom seek bar overlaps gesture-nav handle | Scrubbing accidentally triggers system back / home | Inset the seek bar with `WindowInsets.systemBars.only(WindowInsetsSides.Bottom)` |
| Predictive back preview shows a stale Composition (e.g. player still visible during back-to-library swipe) | Visual stutter on Pixel 8+/Android 14+ devices | Ensure the destination Composable is composed *before* the gesture commits; use Compose Navigation 2.8+'s built-in predictive support |
| Filter sheet doesn't restore selection state across rotation after `SavedStateHandle` version skew | Users lose filter selections | Pin lifecycle / activity to BOM-resolved versions (Pitfall 10) and test rotation explicitly |
| Performance "wins" advertised in changelog with no measurable delta | Users see no improvement, trust erodes | Every perf claim **must** carry a baseline-profile or macrobenchmark delta number — per `PROJECT.md` constraints |

## "Looks Done But Isn't" Checklist

- [ ] **AGP/Gradle bump:** Often missing JDK toolchain pin — verify `kotlin { jvmToolchain(17) }` in convention plugins, `gradle --version` shows expected daemon JDK.
- [ ] **Kotlin bump:** Often missing matching KSP coordinate — grep `libs.versions.toml` for `ksp = ` and confirm prefix matches `kotlin = `.
- [ ] **Compose BOM bump:** Often missing baseline profile regen — `git log -1 --format=%cd app/src/release/generated/baselineProfiles/baseline-prof.txt` should be **after** the BOM bump commit.
- [ ] **R8 minify:** Often missing release-build smoke — verify `:app:assembleBenchmark` opens every primary screen on a real device without crashing.
- [ ] **Edge-to-edge migration:** Often missing 3-button-nav device test — apps look fine on Pixel gesture-nav, broken on OEM 3-button.
- [ ] **Predictive back:** Often missing the manifest flag — verify `android:enableOnBackInvokedCallback="true"` on `<application>`.
- [ ] **Hilt bump:** Often missing the incremental smoke — edit one `@HiltViewModel` and time the next build vs. the previous incremental build.
- [ ] **Baseline profile:** Often missing freshness gate in CI — `find app/src/release/generated/baselineProfiles/baseline-prof.txt -newer gradle/libs.versions.toml` returns the file.
- [ ] **Detekt/ktlint:** Often missing rule re-baseline — `./gradlew detekt ktlintCheck` on a clean tree before any code changes is green.
- [ ] **Macrobenchmark:** Often missing GMD pin — `./gradlew :baselineprofile:pixel6Api34BenchmarkBenchmark` exists.
- [ ] **lint baseline:** Often missing — `ls app/lint-baseline.xml` returns a file before bumping AGP.
- [ ] **Version catalog:** Often has accidental `version.ref` on BOM-controlled Compose artifacts — `grep 'version.ref = "composeBom"' gradle/libs.versions.toml` should match only the BOM line.

## Recovery Strategies

| Pitfall | Recovery Cost | Recovery Steps |
|---------|---------------|----------------|
| Stale baseline profile shipped | LOW | Regen profile, ship a patch release; ART rebuilds in the background after install |
| KSP version mismatch caused MissingBindingException | LOW | Pin coordinates correctly, `./gradlew clean`, rebuild |
| Edge-to-edge regression in player | MEDIUM | Add `safeDrawingPadding()` to overlay only; ship a patch — pure UI fix, no migration |
| R8 stripped serialization classes | MEDIUM | Restore narrower keep rules, rebuild, ship patch — but **API-key prefs may be wiped if `ConnectionStore` failed silently on first launch**; ship a "re-enter your API key" banner |
| Hilt incremental NPE | LOW | `./gradlew clean`, set `ksp.incremental=false` until the next Hilt patch release |
| Wrong Compose Compiler plugin version baked into release | HIGH | Pull release; whole-app recomposition behavior may be wrong; cannot be patched without full re-test pass against the `DEVICE_TESTING.md` checklist |
| Dependency convergence resolved unexpected version of lifecycle | LOW–MEDIUM | Pin with `strictly("X.Y.Z")` in catalog, rebuild, manual SavedStateHandle test |
| Predictive back broke navigation | MEDIUM | Migrate to `NavigationBackHandler`; intermediate fix is to set `android:enableOnBackInvokedCallback="false"` (loses preview but restores legacy behavior) |

## Pitfall-to-Phase Mapping

| Pitfall | Prevention Phase | Verification |
|---------|------------------|--------------|
| 1: JDK toolchain mismatch | **D** | `./gradlew -q javaToolchains` shows single JDK 17 |
| 2: Configuration cache breakage | **D** | `./gradlew assembleDebug --configuration-cache` is clean |
| 3: KSP / Kotlin version drift | **D** | `mismatch=fail` set; build green |
| 4: Compose Compiler plugin coordinate | **D** | `./gradlew :app:dependencies` shows one compose-compiler version |
| 5: Strong skipping regressions in player | **D** verify + **L** refactor | Manual `DEVICE_TESTING.md` player checklist green after BOM bump |
| 6: Stale baseline profile | **P** (with **D**-phase gate) | CI freshness check passes; profile-installed cold-start ≥15% faster than no-profile |
| 7: Edge-to-edge insets in player / filter | **G** | Pixel + 3-button-nav OEM device both pass `DEVICE_TESTING.md` |
| 8: Predictive back custom handlers | **G** | Back-swipe preview test on Android 14+ |
| 9: R8 / serialization keep rules | **D** bump check + **P** smoke | `benchmark` build opens every primary screen |
| 10: Dependency convergence | **D** | `./gradlew dependencies` diff reviewed; no surprise version upgrades |
| 11: Hilt aggregating processor regression | **D** | Incremental edit on one `@HiltViewModel` builds in <15s |
| 12: Detekt / ktlint rule drift | **D** + **L** | `./gradlew detekt ktlintCheck` green; baselines re-shrunk in **L** |
| 13: Macrobenchmark variance | **P** | GMD wired; p50 stable across 3 back-to-back runs |
| 14: Catalog BOM vs. explicit version | **D** | Catalog audit checklist completed |
| 15: CI cache invalidation | **D** + **L** | First CI run on a fresh cache key reproduces local result |
| 16: Media3 / nextlib coupling | **D** (scope-check) | Media3 explicitly excluded from bump scope this milestone |
| 17: Compose API removals (Dec '25 / Apr '26) | **D** | `compileReleaseKotlin` has zero deprecation warnings |
| 18: Missing `lint-baseline.xml` | **D** then **L** | File exists; **L** shrinks it |

## Sources

**Authoritative (HIGH confidence):**
- [Android Gradle plugin 9.0.1 (January 2026) release notes](https://developer.android.com/build/releases/agp-9-0-0-release-notes)
- [Compose compiler migration guide — kotlinlang.org](https://kotlinlang.org/docs/compose-compiler-migration-guide.html)
- [Update your Kotlin projects for AGP 9.0 — The Kotlin Blog](https://blog.jetbrains.com/kotlin/2026/01/update-your-projects-for-agp9/)
- [Compose-to-Kotlin compatibility map](https://developer.android.com/jetpack/androidx/releases/compose-kotlin)
- [Compose BOM to library version mapping](https://developer.android.com/develop/ui/compose/bom/bom-mapping)
- [Strong skipping mode docs — Android Developers](https://developer.android.com/develop/ui/compose/performance/stability/strongskipping)
- [Configure and troubleshoot R8 Keep Rules (Nov 2025)](https://android-developers.googleblog.com/2025/11/configure-and-troubleshoot-r8-keep-rules.html)
- [Handle edge-to-edge enforcements in Android 15 — codelab](https://developer.android.com/codelabs/edge-to-edge)
- [Predictive back gesture support — Android Developers](https://developer.android.com/guide/navigation/custom-back/predictive-back-gesture)
- [About Predictive back in Compose](https://developer.android.com/develop/ui/compose/system/predictive-back)
- [What's new in the Jetpack Compose April '26 release](https://android-developers.googleblog.com/2026/04/jetpack-compose-april-2026-updates.html)
- [Benchmark Baseline Profiles with Macrobenchmark library](https://developer.android.com/topic/performance/baselineprofiles/measure-baselineprofile)
- [Hilt Gradle setup — dagger.dev](https://dagger.dev/hilt/gradle-setup.html)
- [KSP incremental processing](https://kotlinlang.org/docs/ksp-incremental.html)

**Issue trackers / community (MEDIUM confidence, but live evidence):**
- [google/ksp#511 — incremental aggregating processor](https://github.com/google/ksp/issues/511)
- [google/dagger#4063 — Hilt + KSP incremental NPE](https://github.com/google/dagger/issues/4063)
- [Kotlin/kotlinx.serialization#2392 — Missing classes with R8](https://github.com/Kotlin/kotlinx.serialization/issues/2392)
- [Kotlin/kotlinx.serialization#3033 — R8 warning from 1.9.0 ProGuard rules](https://github.com/Kotlin/kotlinx.serialization/issues/3033)
- [dependency-check/dependency-check-gradle#401 — results vary by AGP](https://github.com/dependency-check/dependency-check-gradle/issues/401)

**Practitioner write-ups (MEDIUM confidence, cross-verified):**
- [Statistically Rigorous Android Macrobenchmarks — P-Y's blog](https://blog.p-y.wtf/statistically-rigorous-android-macrobenchmarks)
- [Strong skipping does not fix Kotlin collections in Compose — Jorge Castillo](https://newsletter.jorgecastillo.dev/p/strong-skipping-does-not-fix-kotlin-collections)
- [Goodbye PredictiveBackHandler — migration guide (Mar 2026)](https://medium.com/@santimattius/goodbye-predictivebackhandler-how-to-migrate-to-the-new-navigation-event-in-compose-6e2f294f5ddb)
- [Jetpack Compose: Strong Skipping Mode Explained — Ben Trengrove (Android Developers)](https://medium.com/androiddevelopers/jetpack-compose-strong-skipping-mode-explained-cbdb2aa4b900)
- [Insets handling tips for Android 15's edge-to-edge — Ash Nohe (Android Developers)](https://medium.com/androiddevelopers/insets-handling-tips-for-android-15-s-edge-to-edge-enforcement-872774e8839b)

**Project-specific cross-references:**
- `/home/yun/slopper/.planning/PROJECT.md` — scope and constraints
- `/home/yun/slopper/.planning/codebase/STACK.md` — current pinned versions
- `/home/yun/slopper/.planning/codebase/CONCERNS.md` — existing risks (PlayerScreen size, lint baseline gap, GMD deferral, profile journey gaps, frame-rate on update)
- `/home/yun/slopper/.planning/codebase/TESTING.md` — current zero-test baseline

---
*Pitfalls research for: Android Compose modernization (Slopper)*
*Researched: 2026-05-16*
