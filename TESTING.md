<!-- generated-by: gsd-doc-writer -->
# Testing

This document describes the **actual** state of testing in Slopper today and
the roadmap for deeper coverage. JVM tests and static gates are active, but
there is no `androidTest` coverage. UI and device behavior still require the
manual checklist in [`DEVICE_TESTING.md`](DEVICE_TESTING.md).

## Current state — what exists today

| Layer | Status |
|---|---|
| JVM unit tests | 61 passing tests across 16 reports in the 2026-08-01 local full gate |
| Instrumentation tests (`androidTest`) | No test coverage |
| CI | GitHub Actions and Forgejo Actions workflows exist; no live remote result was verified on 2026-08-01 |
| Manual device smoke test | [`DEVICE_TESTING.md`](DEVICE_TESTING.md) checklist; not run on 2026-08-01 |
| Static analysis — detekt | Active, baselined per module |
| Static analysis — ktlint | Active |
| Android Lint | Active with `app/lint-baseline.xml` + 3 detector disables |
| Macrobenchmark scaffolding | `:baselineprofile` module exists; profile is stale |
| OWASP dependency CVE scan | Plugin wired; not yet run on every build |

JVM tests are wired under `src/test/`; the repository has no `src/androidTest/`
correctness suite. The `:baselineprofile` generator is not a correctness test.

On 2026-08-01,
`./gradlew :app:assembleDebug detekt ktlintCheck test lint --no-daemon` passed
locally with 896 tasks (203 executed, 693 up-to-date). This does not establish
a live CI result or runtime UAT: no device was connected, and software
emulators crashed without KVM, so the APK was not launched.

## Static analysis

All three tools run against the whole multi-module project from the root.

### detekt

Version 2.0.0-alpha.5, applied to every subproject via the root
`build.gradle.kts` `subprojects { }` block. Config lives at
`config/detekt/detekt.yml`. Existing findings are captured in **per-module**
baseline files (`<module>/detekt-baseline.xml`) — 12 modules currently carry a
baseline.

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

Plugin `org.jlleitschuh.gradle.ktlint` 14.2.0, ktlint runtime 1.6.0. Applied to
every subproject from the root. No on-disk baseline files — the current
codebase passes clean.

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
(`build-logic/convention/src/main/kotlin/io/stashapp/android/buildlogic/KotlinAndroid.kt`):

- `NullSafeMutableLiveData` (lifecycle 2.10.0)
- `FrequentlyChangingValue` (compose-runtime, Compose BOM 2026.06.01)
- `RememberInComposition` (compose-runtime, Compose BOM 2026.06.01)

They remain disabled pending compatibility re-evaluation; reduce this list
when the AndroidX/toolchain combination supports the detectors.

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

JVM tests do not replace runtime validation: **every UI change is still gated
on the manual smoke checklist in [`DEVICE_TESTING.md`](DEVICE_TESTING.md)**.

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

This uses the declared Pixel 6 / API 34 Gradle Managed Device
(`pixel6Api34`, with `useConnectedDevices = false`) and therefore needs
working emulator virtualization/KVM. The plugin copies the output to:

```
app/src/release/generated/baselineProfiles/baseline-prof.txt
```

which `androidx.profileinstaller` (declared in `app/build.gradle.kts`) ships
inside the release APK.

**Current status**: the checked-in profile is **stale**. On 2026-08-01 the
software emulator crashed without KVM, and no connected device was available
as a runtime-testing fallback. Profile regeneration was therefore not
verified.

The `app` module also exposes a `benchmark` build type (`initWith(release)`,
non-debuggable but `profileable`) for running macrobench traces.

## Dependency CVE scan

OWASP dependency-check 12.2.2 is wired into the root `build.gradle.kts` and
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
Gradle configuration cache as of the 12.x line. Reports land in
`build/reports/dependency-check-report.{html,json}`.

This scan is **not yet run on every build**. CI integration is on the roadmap
(see SEC-CI-01 below).

## Roadmap — deeper test coverage

The shared JVM test wiring is in place. Remaining work is to deepen behavioral
coverage and add device-backed tests.

### POLISH-05 — expand test suites

- Broaden JVM unit coverage across `core/*` and `feature/*`.
- Strengthen ViewModel state-machine tests around transitions and failures.
- Add Compose and integration smoke tests under `androidTest`.

Coverage should grow around observable behavior and regressions rather than
framework plumbing.


### PERF-05 — expand baseline profile

Cover more than cold start: warm scene-detail open, player surface
construction, queue navigation. The generator script already has the scroll
+ detail drill scaffolding to build on.

### SEC-CI-01 — CI dependency-check workflow

Move `dependencyCheckAnalyze` into CI (Forgejo Actions): weekly scheduled
scan + on every PR that touches dependencies. Fail PR check on HIGH+ CVEs
not present in `config/owasp-suppressions.xml`.

## How to add a test

The `stash.android.library` convention plugin supplies JUnit5, Turbine, MockK,
and Robolectric to library modules and uses the JUnit Platform. Put JVM tests
under the module's `src/test/` source set, following the existing tests, then
run the module test task or `./gradlew test`.

There is no `androidTest` suite yet. Device-backed Compose and integration
tests belong under `src/androidTest/` when that coverage is added. Until then,
validate UI and device behavior with
[`DEVICE_TESTING.md`](DEVICE_TESTING.md) and run
`./gradlew detekt ktlintCheck test lint`.
