<!-- generated-by: gsd-doc-writer -->
# Testing

This document describes the **actual** state of testing in Slopper today, plus
the roadmap for landing a real test pyramid. Be aware: this is a brownfield
project mid-modernization, and the automated test story is intentionally bare.
If you change behaviour, run through the manual checklist in
[`DEVICE_TESTING.md`](DEVICE_TESTING.md).

## Current state — what exists today

| Layer | Status |
|---|---|
| JVM unit tests | None wired |
| Instrumentation tests (`androidTest`) | None wired |
| CI | None — no `.github/workflows`, no Forgejo Actions yet |
| Manual device smoke test | [`DEVICE_TESTING.md`](DEVICE_TESTING.md) checklist |
| Static analysis — detekt | Active, baselined per module |
| Static analysis — ktlint | Active |
| Android Lint | Active with `app/lint-baseline.xml` + 3 detector disables |
| Macrobenchmark scaffolding | `:baselineprofile` module exists; profile is stale |
| OWASP dependency CVE scan | Plugin wired; not yet run on every build |

No module under `core/*` or `feature/*` has a `src/test/` or `src/androidTest/`
directory. The only test-style module is `:baselineprofile`, which is a
macrobenchmark generator, not a correctness test.

## Static analysis

All three tools run against the whole multi-module project from the root.

### detekt

Version 1.23.8, applied to every subproject via the root `build.gradle.kts`
`subprojects { }` block. Config lives at `config/detekt/detekt.yml`. Existing
findings are captured in **per-module** baseline files
(`<module>/detekt-baseline.xml`) — 9 modules currently carry a baseline.

```bash
./gradlew detekt                # run analysis (fails on new issues)
./gradlew detektFormat          # auto-fix Formatting violations
```

Failure mode: any **new** finding above the baseline fails the task
(`ignoreFailures = false`). Regenerate the baseline only when intentionally
accepting new findings:

```bash
./gradlew detektBaseline
```

### ktlint

Plugin `org.jlleitschuh.gradle.ktlint` 13.1.0, ktlint runtime 1.6.0. Applied to
every subproject from the root. No on-disk baseline files — the current codebase
passes clean.

```bash
./gradlew ktlintCheck           # check formatting
./gradlew ktlintFormat          # auto-fix
```

Generated Apollo sources and anything under `build/` are excluded via the filter
in the root `build.gradle.kts`.

### Android Lint

Run against the application module:

```bash
./gradlew :app:lintDebug
```

Three detectors are **disabled** in the convention plugin
(`build-logic/convention/src/main/kotlin/io/stashapp/android/buildlogic/KotlinAndroid.kt`)
because they crash with `IncompatibleClassChangeError` under AGP 8.7.3 lint +
Kotlin 2.2.20:

- `NullSafeMutableLiveData` (lifecycle 2.8.7)
- `FrequentlyChangingValue` (compose-runtime, Compose BOM 2026.05.00)
- `RememberInComposition` (compose-runtime, Compose BOM 2026.05.00)

These will be re-enabled when AndroidX Lifecycle / Compose are bumped further
(currently deferred under POLISH backlog).

### Lint baseline policy

`app/lint-baseline.xml` is a snapshot of pre-existing findings (~1000 lines)
captured during the Phase 1 dependency bump. The contract:

- **Floor**: the baseline is the maximum tolerated noise.
- **New issues fail the build** (`abortOnError = true`).
- **Existing issues are deferred** — they will be triaged and burned down
  under the POLISH backlog, not re-baselined silently.

Do not regenerate `lint-baseline.xml` just to make a build green. If you must,
record the reason in the commit message and call it out in PR review.

## Platform compliance checks (Phase 2)

After any change to `AndroidManifest.xml`, `themes.xml`, `MainActivity.kt`,
`PlayerScreen.kt`, or the three `ModalBottomSheet` sites, also verify:

| Check | How |
|---|---|
| Cold-launch splash visible until library loads | Install fresh, cold-launch; no white flash |
| Edge-to-edge: no clipped buttons under gesture nav | Enable gesture-nav, check bottom tabs + sheets |
| Predictive back: preview appears on swipe-right | Enable **Settings → Developer Options → Predictive back animations**; swipe back from `PlayerScreen` |
| `ModalBottomSheet` sheets (FilterSheet, NavCustomizeSheet, MoreSheet) don't clip content behind nav bar | Open each sheet in gesture-nav mode |
| Per-app language picker present (API 33+) | Settings screen → Language row fires system picker |

These checks are tracked in `.planning/phases/02-comply-platform-compliance/02-UAT.md`
(51-row verbal PASS on Galaxy S23+ Android 16, gesture-nav, 2026-05-17).
For full re-verification criteria see `02-UAT.md`.

## Manual device testing

Until the automated pyramid lands, **every UI change is gated on the manual
smoke checklist in [`DEVICE_TESTING.md`](DEVICE_TESTING.md)**.

The bottom-bar smoke covers the six top-level destinations:

- **Connection** — first-run server URL + API key entry, "Test" round-trip, encrypted-prefs restore on relaunch
- **Library** — grid load, pagination, search, filter sheet (sort / organized / rating min)
- **Browse** — Performers / Studios / Tags grids, drill-through to filtered library
- **Home** — entry surface
- **Detail** — hero, metadata pills, rating stars, organize toggle, O-counter, markers
- **Player** — single-scene playback, queue navigation, PiP, marker seek, resume sync-back to Stash

See `DEVICE_TESTING.md` for the prerequisites (Android Studio / JDK 17 / ADB +
phone with USB debugging), install commands, and the common-failure-mode table
keyed by symptom.

## Macrobenchmark + baseline profile

The `:baselineprofile` module (Android Test + `androidx.baselineprofile`
plugin) generates the baseline profile that ships in the release APK and lets
ART pre-compile hot Compose paths (NavHost transitions, `LazyVerticalGrid`
measure/layout, `AnimatedContent`).

The single generator class lives at:

```
baselineprofile/src/main/java/io/stashapp/android/baselineprofile/StashBaselineProfileGenerator.kt
```

It exercises: cold start → wait for first draw → scroll the library grid →
open + back out of detail.

### Regenerating the profile

```bash
./gradlew :app:generateBaselineProfile
```

This requires a connected device (the module is configured with
`useConnectedDevices = true` — no Gradle Managed Device yet). The plugin
copies the output to:

```
app/src/release/generated/baselineProfiles/baseline-prof.txt
```

which `androidx.profileinstaller` (declared in `app/build.gradle.kts`) ships
inside the release APK.

**Current status**: the checked-in profile is **stale**. The Phase 1
regeneration attempt was deferred because no test device was wired on the dev
host. A new profile will land once the GMD work in PERF-01 makes regeneration
repeatable in CI.

The `app` module also exposes a `benchmark` build type (`initWith(release)`,
non-debuggable but `profileable`) for running macrobench traces.

## Dependency CVE scan

OWASP dependency-check 11.1.1 is wired into the root `build.gradle.kts` and
configured to fail on CVSS ≥ 7.0 (HIGH / CRITICAL). Suppressions live at
`config/owasp-suppressions.xml`.

```bash
# Set NVD_API_KEY first — without it the NVD API rate-limits aggressively
# and the first-run download takes hours (a previous local run hit ~2h47m
# without one).
export NVD_API_KEY="<your key from https://nvd.nist.gov/developers/request-an-api-key>"

./gradlew dependencyCheckAnalyze --no-configuration-cache
```

`--no-configuration-cache` is required — the plugin is incompatible with the
Gradle configuration cache as of the 11.x line. Reports land in
`build/reports/dependency-check-report.{html,json}`.

This scan is **not yet run on every build**. CI integration is on the roadmap
(see SEC-CI-01 below).

## Roadmap — getting to a real test pyramid

These items are tracked in the project's internal requirements backlog.

### POLISH-04 — test framework wiring

Wire **JUnit5 + Turbine + MockK + Robolectric** into the
`stash.android.library` convention plugin so every `core/*` and `feature/*`
module gets the same test classpath and runner configuration by default.
Lands the `src/test/` source-set with shared test utilities.

### POLISH-05 — seed test suites

The first wave of tests once POLISH-04 is in:

- **Unit tests** for `core/common`, `core/model`, `core/domain` (pure-Kotlin layers, no Android stubs needed)
- **One ViewModel state-machine test per feature** using Turbine to assert state transitions
- **One Compose smoke test per feature** to catch composition crashes

The coverage target is intentionally modest at first — get the pyramid built,
then grow it.

### PERF-01 — Gradle Managed Device for macrobench

Wire a Pixel 6 / API 34 GMD into `:baselineprofile` so profile regeneration
(and any future macrobench) runs hands-free in CI on a deterministic device.
Unblocks fresh baseline-profile generation without a physical phone on the
dev host.

### PERF-05 — expand baseline profile

Cover more than cold start: warm scene-detail open, player surface
construction, queue navigation. The generator script already has the scroll
+ detail drill scaffolding to build on.

### SEC-CI-01 — CI dependency-check workflow

Move `dependencyCheckAnalyze` into CI (Forgejo Actions): weekly scheduled
scan + on every PR that touches dependencies. Fail PR check on HIGH+ CVEs
not present in `config/owasp-suppressions.xml`.

## How to add a test

When the test pyramid is wired (POLISH-04 / POLISH-05), this section will
document:

- The `src/test/` and `src/androidTest/` source-set layout
- JUnit5 + Turbine + MockK + Robolectric usage patterns for ViewModel / repository tests
- The Compose smoke-test pattern for screen-level coverage
- How tests run in CI and what failure modes look like locally

Until then: any UI or behaviour change must be validated by walking the
[`DEVICE_TESTING.md`](DEVICE_TESTING.md) checklist on a real device, and
new commits must pass `./gradlew detekt ktlintCheck :app:lintDebug`.
