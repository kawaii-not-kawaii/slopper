# Phase 2: COMPLY (Platform Compliance) — Research

**Researched:** 2026-05-17
**Domain:** Android Compose platform-contract migration (edge-to-edge / predictive back / Splash API / per-app locales / orphan permissions)
**Confidence:** HIGH (Context7-equivalent: developer.android.com canonical pages + composables.com signature mirror + verified codebase grep)

## Summary

Phase 2 is BEHAVIORAL plumbing — no user-visible change beyond (1) new splash on cold launch, (2) new Language row in Settings (API 33+), (3) animated back gesture preview on PlayerScreen, (4) two orphan permissions removed, (5) themes.xml cleanup so the already-called `enableEdgeToEdge()` actually has effect. All APIs needed (`enableEdgeToEdge`, `PredictiveBackHandler`, `installSplashScreen`, `generateLocaleConfig`, `ACTION_APP_LOCALE_SETTINGS`) exist at the current toolchain floor — only `targetSdk = 36` *enforcement* differs, and that lands later in the AGP-9 phase (DEPS-17).

The codebase audit confirms the SPEC's premise: `MainActivity.kt:87` already calls `enableEdgeToEdge()`; `BackHandler` appears at exactly one call site (`PlayerScreen.kt:187`); the `MoreSheet` composable DOES exist (defined in `core/ui/src/main/java/io/stashapp/android/core/ui/nav/BottomNav.kt:167` — SPEC.md note "MoreSheet does NOT exist" is wrong); ModalBottomSheet appears in 3 sites (`FilterSheet`, `NavCustomizeSheet`, `MoreSheet`); 6 of 7 feature screens use `Scaffold` (`ConnectionScreen` uses `Box`); `app/src/main/res/values/strings.xml` ships only `app_name`; no `values-*/` directories exist (English-only app).

**Primary recommendation:** Two atomic plans as ROADMAP suggests — Plan 2.1 (Insets + Back + Splash = COMPLY-01/02/04, the three "code surgery" tasks) and Plan 2.2 (Permissions + Locales + Manifest hygiene + UAT = COMPLY-03/05/06/07, the four "thin / additive" tasks). Use `PredictiveBackHandler` with `try { progress.collect { } } catch (CancellationException) { }` pattern, `installSplashScreen()` BEFORE `super.onCreate` with Pattern A (LaunchedEffect → AtomicBoolean), `ModalBottomSheet contentWindowInsets = { BottomSheetDefaults.modalWindowInsets }` (the default already does the right thing — passing it explicitly is documentation, not behavior change).

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**1. Branch shape — isolated `phase-2/comply-platform-compliance`**
- Single branch: `phase-2/comply-platform-compliance` (already created; head `7d851a0`). Base is `73a5677` (current head of `phase-1/deps-bump` / PR #5).
- When PR #5 merges to `master`, this branch rebases onto `master` before opening the Phase 2 PR.
- Plans MUST NOT touch `gradle/libs.versions.toml` toolchain pins (kotlin / agp / composeBom / hilt / coil / apollo / okhttp). Adding the `core-splashscreen` library entry is fine (net-new, Phase 2-scoped).

**2. Commit shape — one commit per COMPLY requirement (atomic, bisectable)**
- Format: `feat(comply): COMPLY-XX — <short description>` for code; `chore(comply): COMPLY-XX — …` for manifest-only / permission-removal.
- Total: 7 commits + 8th for screenshot pack + UAT artifacts.
- Order: whatever convenient, EXCEPT COMPLY-04 splash MUST land before COMPLY-01 final UAT (splash affects cold-launch screenshot).

**3. Predictive back — `PredictiveBackHandler` (NOT `NavigationBackHandler`)**
- `PredictiveBackHandler` from `androidx.activity.compose` is available since Activity Compose 1.8.0+; we're on 1.9.3.
- `NavigationBackHandler` / `NavigationEventState` (Compose Multiplatform 1.10 / AndroidX Activity 1.10+) is NOT available at our floor — that lands when DEPS-07 + DEPS-17 bump to Activity Compose 1.13.0+.
- Backlog item `COMPLY-02-NAV-EVENT` — single call-site migration when AGP-9 phase lands.
- Gesture-cancel: Material3 default — `onExit()` does NOT fire on cancel. The lambda's body wraps `onExit()` after `progress.collect { }`; cancelled flow throws `CancellationException` which short-circuits.

**4. Edge-to-edge — PITFALLS §7 patterns applied without restructuring**
- `themes.xml` — strip `statusBarColor` and `navigationBarColor` overrides. Keep `windowBackground = @android:color/black`.
- `PlayerScreen` — wrap player surface in `Box(Modifier.fillMaxSize())`; place control overlays inside inner `Box(Modifier.safeDrawingPadding())`. SurfaceView itself must NOT receive `safeDrawing` insets.
- `FilterSheet` + `NavCustomizeSheet` — pass `contentWindowInsets = { WindowInsets.navigationBars }` to `ModalBottomSheet`.
- Other top-level Scaffolds (Home, Library, Settings, Browse, Detail) — grep-audit only. Material3 default `ScaffoldDefaults.contentWindowInsets` already does the right thing.

**5. Splash — `core-splashscreen` 1.0.1 gated on `RootViewModel.start != null`**
- Locked dep version: `androidx.core:core-splashscreen:1.0.1`.
- `Theme.Stash.Splash` parent = `Theme.SplashScreen` with `windowSplashScreenAnimatedIcon = @mipmap/ic_launcher`, `windowSplashScreenBackground = @android:color/black`, `postSplashScreenTheme = @style/Theme.Stash`.
- Activity declares `Theme.Stash.Splash` in manifest.
- Pattern A (RECOMMENDED): `installSplashScreen()` returns a `SplashScreen`; `setKeepOnScreenCondition { atomicBoolean.get() }`; a `LaunchedEffect(rootViewModel.start)` flips the boolean when `start != null`.
- Pattern B (rejected): synchronous `ConnectionRepository` probe before Hilt graph builds — more coupling.

**6. Locale picker (COMPLY-06) — top-level row in SettingsScreen, API-33 gated**
- `LanguageRow` composable as top-level row in SettingsScreen (above "Advanced" / "Diagnostics", below the connection block).
- New `R.string.settings_language` (English: "Language") in `app/src/main/res/values/strings.xml`.
- Composition: leading `Icons.Outlined.Language`, headline = `stringResource(R.string.settings_language)`, supporting = `stringResource(R.string.settings_language_description)`.
- On click: `Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply { data = Uri.fromParts("package", context.packageName, null) }`.
- Gated by `Build.VERSION.SDK_INT >= 33` — composable returns null below API 33.
- Build: `android { androidResources { generateLocaleConfig = true } }` in `app/build.gradle.kts`.
- No manual `xml/locale_config.xml` — AGP generates it.

**7. Orphan permission removal (COMPLY-03 + COMPLY-05) — sed-style line removal**
- COMPLY-03: delete `<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />`.
- COMPLY-05: delete `<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />`.
- KEEP: `<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />` — harmless base FGS for future BG-MEDIA.
- Default: two separate commits (cleaner bisect). Optional fold to one if planner judges bisect-granularity loss acceptable.

**8. UAT + screenshot pack — flat directory, light/dark per screen, S23+ only**
- Layout: `.planning/phases/02-comply-platform-compliance/screenshots/<screen>-<theme>.png` — flat, no subdirs.
- 12 PNGs total (exceeds SPEC's ≥10 minimum): home / library / player-controls-visible / player-controls-hidden / settings / connection — each in light + dark.
- `SCREENSHOTS.md` index documents capture device (Galaxy S23+ Android 16, gesture-nav), commit SHA, reviewer sign-off.
- `02-UAT.md` follows `01-UAT.md` format. 3-button-nav rows all `DEFERRED — see COMPLY-07-3BTN backlog`.

**9. Accepted Risks (REVIEWS-C4 compliance)**
- Risk 1 — PITFALLS §7 (3-button-nav edge-to-edge unverified): backlog `COMPLY-07-3BTN`, revisit trigger = hardware availability OR AGP-9 phase. User ACCEPT 2026-05-17.
- Risk 2 — PITFALLS §8 (`PredictiveBackHandler` deprecated at CMP 1.10+): backlog `COMPLY-02-NAV-EVENT`, revisit trigger = AGP-9 phase landing Activity Compose 1.13.0+.

### Claude's Discretion

- **Theme.Stash.Splash icon asset:** use existing `@mipmap/ic_launcher`. No new asset. If launcher icon's adaptive layers crop oddly inside splash circle on Android 12+, fall back to `@drawable/splash_icon` foreground variant.
- **Screenshot capture method:** `adb shell screencap` or device's built-in screenshot — either fine. No Compose `captureToImage` instrumentation needed.
- **String resource location:** `R.string.settings_language` lives in `app/src/main/res/values/strings.xml` (not feature module) — system Intent string, not feature-local.
- **PR description structure:** single PR at end of phase against `master` (post PR #5 merge + rebase). Body: per-COMPLY-XX summary, screenshot embeds/links, UAT result table, accepted-risk callouts.
- **`R.string.settings_language_description` content:** planner writes 1-sentence description following SettingsScreen voice convention.

### Deferred Ideas (OUT OF SCOPE)

- **COMPLY-07-3BTN** — re-run DEVICE_TESTING.md on 3-button-nav device. Hardware-blocked. Revisit trigger: hardware OR AGP-9 phase COMPLY re-verification.
- **COMPLY-02-NAV-EVENT** — migrate to `NavigationBackHandler` / `NavigationEventState` once Activity Compose ≥ 1.10 (AGP-9 phase).
- **Real notification flow** (BG-MEDIA-01 / BG-MEDIA-02) — when `MediaSessionService` lands, re-declare `POST_NOTIFICATIONS` + `FOREGROUND_SERVICE_MEDIA_PLAYBACK` with proper `ActivityResultContracts.RequestPermission` flow.
- **Translation work** — `generateLocaleConfig` derives from existing `values-*/`; codebase ships English-only so dialog shows one locale until translations land.
- **`PlayerScreen.kt` structural split** (POLISH-01, Phase 4). Phase 2 touches only line 187 + control-overlay wrap.
- **Custom in-app locale list UI** — Phase 2 only opens system dialog.
- **`network_security_config.xml` cleartext tightening** — SEC-02 backlog.
- **`MoreSheet`** — already exists (SPEC.md note was wrong); no Phase 2 action.

</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| COMPLY-01 | Edge-to-edge correctness (themes.xml strip + Scaffold WindowInsets audit + S23+ screenshot pack) | Standard Stack §A1 (Compose Material3); Architecture Patterns §B1 (themes.xml diff, Scaffold audit grep, ModalBottomSheet contentWindowInsets); Code Examples §F1, §F2; Pitfalls §E1 (3-button-nav scrim) |
| COMPLY-02 | Predictive back (manifest flag + PlayerScreen.kt:187 BackHandler → PredictiveBackHandler) | Standard Stack §A2 (Activity Compose 1.9.3); Architecture Patterns §B2 (manifest flag + try/catch CancellationException pattern); Code Examples §F3; Pitfalls §E2 (enabled-mutation hazard — N/A here, unconditional BackHandler) |
| COMPLY-03 | Remove orphan POST_NOTIFICATIONS permission | Architecture Patterns §B3 (manifest line delete); Code Examples §F4; verified via grep — no NotificationManager / NotificationCompat / notify( call sites in repo |
| COMPLY-04 | Splash Screen API (core-splashscreen 1.0.1 + Theme.Stash.Splash + setKeepOnScreenCondition gated on RootViewModel.start) | Standard Stack §A3 (core-splashscreen — version note flagged: 1.2.0 is current stable); Architecture Patterns §B4 (Pattern A — installSplashScreen BEFORE super.onCreate, LaunchedEffect→AtomicBoolean); Code Examples §F5, §F6; Pitfalls §E3 (ANR risk on long keep-condition) |
| COMPLY-05 | Remove orphan FOREGROUND_SERVICE_MEDIA_PLAYBACK permission | Architecture Patterns §B3 (manifest line delete); Code Examples §F4 — same pattern as COMPLY-03 |
| COMPLY-06 | Per-app language (generateLocaleConfig + new LanguageRow in SettingsScreen → Settings.ACTION_APP_LOCALE_SETTINGS) | Standard Stack §A4 (AGP androidResources DSL); Architecture Patterns §B5 (generated file location, manifest auto-merge); Code Examples §F7, §F8; Pitfalls §E4 (single-locale dialog when no values-* dirs) |
| COMPLY-07 | DEVICE_TESTING.md re-run (Galaxy S23+ gesture-nav only; 3-button-nav deferred) | Validation Strategy §G (manual UAT — no automated tests in this phase; falsifiable via grep + screenshot evidence) |

</phase_requirements>

## Project Constraints (from CLAUDE.md)

User's global CLAUDE.md emphasizes:
- **Immutability:** Compose state copies (already standard in Kotlin Compose; no Java mutation patterns apply)
- **File organization:** 200-400 lines typical, 800 max — `PlayerScreen.kt` (1122 lines) already over but its structural split is POLISH-01 (Phase 4); Phase 2 keeps surgical edits only
- **Error handling:** wrap `try/catch` with logged error + user-friendly rethrow — relevant for the splash startup path if `RootViewModel.start` collection ever throws
- **Input validation:** zod-equivalent (Compose: type-safe Intents) — `Intent(Settings.ACTION_APP_LOCALE_SETTINGS)` is type-safe constant; `Uri.fromParts` is canonical
- **No console.log** — Kotlin equivalent is `android.util.Log.i("StashNav", …)` already in MainActivity for navigation traces; Phase 2 plans should not introduce new logging in the production path

Project-level `CLAUDE.md` at `/home/yun/slopper/CLAUDE.md` directs spec-layer awareness — orthogonal to Phase 2 content.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Edge-to-edge theme cleanup (COMPLY-01a) | Android Resources (themes.xml) | — | System-bar tinting is a window-attribute concern; lives in styles |
| Inset-aware Scaffolds / overlays (COMPLY-01b) | Compose UI (per-feature) | core/designsystem (theme constants only) | Each screen owns its own inset handling; no shared inset wrapper needed |
| Predictive back manifest flag (COMPLY-02a) | AndroidManifest | — | App-level attribute on `<application>` |
| PredictiveBackHandler call site (COMPLY-02b) | feature/player (PlayerScreen) | — | Single composable, single concern (back gesture handling) |
| Orphan permission removal (COMPLY-03 + COMPLY-05) | AndroidManifest | — | Single-line `<uses-permission>` deletes; no code dependency |
| Splash dependency (COMPLY-04a) | gradle/libs.versions.toml + app/build.gradle.kts | — | App-level dep (not consumed by any other module) |
| Splash theme style (COMPLY-04b) | Android Resources (themes.xml) | — | Style inheritance Theme.SplashScreen → Theme.Stash.Splash → Theme.Stash |
| Splash install (COMPLY-04c) | app/MainActivity.onCreate | feature/* (none) | Activity-level boot hook; pre-super.onCreate call |
| Splash gate (COMPLY-04d) | app/RootViewModel.start (existing) | app/MainActivity (consumer) | Existing StateFlow re-used as keep-condition source |
| Build-time locale config (COMPLY-06a) | app/build.gradle.kts (androidResources DSL) | — | App-only build setting; library modules don't need it |
| Language row UI (COMPLY-06b) | feature/settings (SettingsScreen) | core/ui icons (Icons.Outlined.Language already in catalog via material-icons-extended) | Settings UI is the natural surface for a system-Intent launcher |
| String resource (COMPLY-06c) | app/src/main/res/values/strings.xml | — | System-Intent-related string — app-scope, not feature-scope (matches existing convention for system strings) |
| Manual UAT + screenshots (COMPLY-07) | .planning/phases/02-…/ artifacts | — | Documentation tier; no code |

**Tier verification note for planner:** No capability should be misassigned to a deeper module. `feature/player` does NOT depend on the new splash dep. `feature/settings` does NOT depend on `app/`. The only cross-tier interaction is `app/MainActivity` reading `app/RootViewModel.start` (already in place — Phase 2 adds a `LaunchedEffect` that flips an `AtomicBoolean`, no new dependency edges).

## Standard Stack

### Core (Phase 2 net-new + existing relied-upon)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `androidx.core:core-splashscreen` | **1.0.1** (locked by CONTEXT.md) | Splash Screen API backward-compat shim | The Jetpack canonical wrapper for the Android 12+ SplashScreen API; provides `installSplashScreen()`, keep-condition, and pre-12 fallback |
| `androidx.activity:activity-compose` | 1.9.3 (existing, Phase 1 floor) | `PredictiveBackHandler`, `BackHandler`, `enableEdgeToEdge` | Single source of truth for Activity-Compose integration; `PredictiveBackHandler` added 1.8.0 |
| `androidx.compose.material3:material3` | via Compose BOM 2026.05.00 (existing) | `Scaffold`, `ScaffoldDefaults.contentWindowInsets`, `ModalBottomSheet`, `BottomSheetDefaults.modalWindowInsets` | Material3 ≥ 1.4 supports `contentWindowInsets` on both Scaffold and ModalBottomSheet |
| `androidx.compose.material:material-icons-extended` | via Compose BOM (existing) | `Icons.Outlined.Language` | Already present — no new dep for the LanguageRow icon |

### Supporting (existing — no Phase 2 change)

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `androidx.compose.foundation:foundation` | via BOM | `Box`, `Modifier.safeDrawingPadding`, `WindowInsets.systemBars`, `WindowInsets.navigationBars` | Edge-to-edge overlay wrapping in PlayerScreen, FilterSheet, NavCustomizeSheet, MoreSheet |
| `androidx.hilt:hilt-navigation-compose` | 1.2.0 (existing) | `hiltViewModel()` for `RootViewModel` access | Splash gate reads `RootViewModel.start` — Hilt-injected at composition time, NOT before `super.onCreate` (Pattern A defers Hilt access to first composition) |

### Version note — splash dep

CONTEXT.md locks `androidx.core:core-splashscreen:1.0.1`. **Verified upstream:** `1.2.0` is the current stable per `developer.android.com/jetpack/androidx/releases/core` (no 1.1.x stable line). `1.0.1` is functionally complete for COMPLY-04 (all required APIs — `installSplashScreen`, `setKeepOnScreenCondition`, `Theme.SplashScreen` parent, splash attributes — exist since 1.0.0). Plan should use the CONTEXT.md-locked `1.0.1`. If a future plan-review decides to track latest stable, `1.2.0` is API-compatible and safe to swap. **[VERIFIED: developer.android.com/jetpack/androidx/releases/core, fetched 2026-05-17]**

### Alternatives Considered (and locked out)

| Instead of | Could Use | Why locked out |
|------------|-----------|----------------|
| `PredictiveBackHandler` | `NavigationBackHandler` / `NavigationEventState` | Not available at Activity Compose 1.9.3; requires 1.10+ (AGP-9 phase). Backlog `COMPLY-02-NAV-EVENT`. |
| Pattern A (LaunchedEffect→AtomicBoolean keep-condition) | Pattern B (synchronous ConnectionRepository probe pre-super.onCreate) | Pattern B requires Hilt-graph access before `super.onCreate` (impossible cleanly) OR manual SharedPreferences probe (couples MainActivity to data-layer internals). Pattern A's ~50ms splash extension is invisible. |
| Custom in-app locale picker | `Settings.ACTION_APP_LOCALE_SETTINGS` system dialog | Out-of-scope per CONTEXT.md; Phase 2 only opens system dialog. |
| Restructure Scaffolds to consume `contentWindowInsets` explicitly everywhere | Trust `ScaffoldDefaults.contentWindowInsets` default | Material3 default IS `WindowInsets.systemBars` — explicit pass-through adds noise without behavior change. Audit confirms top-level Scaffolds; only FilterSheet/NavCustomizeSheet/MoreSheet need explicit `contentWindowInsets` because `BottomSheetDefaults.modalWindowInsets` may not match per-screen expectation. |

**Installation:**

```toml
# gradle/libs.versions.toml — add to [versions] block
coreSplashscreen = "1.0.1"

# gradle/libs.versions.toml — add to [libraries] block
androidx-core-splashscreen = { module = "androidx.core:core-splashscreen", version.ref = "coreSplashscreen" }
```

```kotlin
// app/build.gradle.kts — add to dependencies { } block
implementation(libs.androidx.core.splashscreen)
```

**Version verification:**
- `androidx.core:core-splashscreen:1.0.1` — confirmed exists on Google Maven [VERIFIED: developer.android.com/jetpack/androidx/releases/core, 2026-05-17]
- `androidx.activity:activity-compose:1.9.3` already on floor [VERIFIED: gradle/libs.versions.toml line 18 `activityCompose = "1.9.3"`]
- Compose BOM `2026.05.00` already on floor [VERIFIED: gradle/libs.versions.toml line 19 `composeBom = "2026.05.00"`]

## Architecture Patterns

### System Architecture Diagram

```
                     ┌────────────────────────────────────────────┐
                     │ Cold Launch (user taps launcher icon)     │
                     └─────────────────────┬──────────────────────┘
                                           │
                                           ▼
                     ┌────────────────────────────────────────────┐
                     │ System paints Theme.Stash.Splash window:  │  ← COMPLY-04
                     │   bg=black, icon=ic_launcher (centered)   │     (themes.xml +
                     │                                            │      manifest activity
                     └─────────────────────┬──────────────────────┘      android:theme)
                                           │
                                           ▼
                     ┌────────────────────────────────────────────┐
                     │ MainActivity.onCreate:                     │
                     │   1. val splashScreen = installSplashScreen()  ← BEFORE super.onCreate
                     │   2. splashScreen.setKeepOnScreenCondition │
                     │      { !appReady.get() }                   │
                     │   3. super.onCreate(savedInstanceState)    │
                     │   4. enableEdgeToEdge()        (existing)  │  ← COMPLY-01 already done
                     │   5. requestHighestRefreshRate() (exist)   │
                     │   6. setContent { StashAppContent() }      │
                     └─────────────────────┬──────────────────────┘
                                           │
                                           ▼
                     ┌────────────────────────────────────────────┐
                     │ First Composition:                         │
                     │   - RootViewModel hiltViewModel() injects  │
                     │   - LaunchedEffect(rootViewModel.start)    │  ← Pattern A
                     │     collects StateFlow<String?>            │     (gate flip)
                     │   - When start != null, appReady.set(true) │
                     └─────────────────────┬──────────────────────┘
                                           │
                                           ▼
                     ┌────────────────────────────────────────────┐
                     │ Splash dismisses (system animates exit)    │
                     │ NavHost renders Connection or Home         │
                     └─────────────────────┬──────────────────────┘
                                           │
                                ┌──────────┴──────────┐
                                ▼                     ▼
              ┌─────────────────────────┐  ┌───────────────────────────┐
              │ User opens FilterSheet/ │  │ User enters PlayerScreen  │
              │ NavCustomizeSheet/      │  │                           │
              │ MoreSheet (Modal)       │  │  - Box (fillMaxSize)      │
              │                         │  │      SurfaceView (video)  │  ← edge-to-edge OK
              │ ModalBottomSheet(       │  │      ┌───────────────┐    │
              │   contentWindowInsets = │  │      │ Box (safeDraw │    │
              │   { WindowInsets.naviga │  │      │ ingPadding)   │    │  ← COMPLY-01
              │   tionBars }            │  │      │   Controls    │    │     overlay wrap
              │ )           ← COMPLY-01 │  │      └───────────────┘    │
              │                         │  │                           │
              │ Sheet handles predictive│  │  PredictiveBackHandler {  │  ← COMPLY-02
              │ back natively once      │  │    progress ->            │     (replaces
              │ manifest flag is set    │  │    try {                  │      BackHandler
              │           ↑ COMPLY-02   │  │      progress.collect {} │      at line 187)
              │                         │  │      onExit()             │
              │                         │  │    } catch (e:Cancel..){} │
              │                         │  │  }                        │
              └─────────────────────────┘  └───────────────────────────┘

                     ┌────────────────────────────────────────────┐
                     │ User opens Settings (any time)            │
                     │   if (SDK_INT >= 33) LanguageRow {        │  ← COMPLY-06
                     │     onClick = startActivity(Intent(       │
                     │       Settings.ACTION_APP_LOCALE_SETTINGS │
                     │     ).apply {                              │
                     │       data = Uri.fromParts("package",     │
                     │         context.packageName, null)        │
                     │     })                                     │
                     │   }                                        │
                     └────────────────────────────────────────────┘

   Manifest (one-time changes):
     <application android:enableOnBackInvokedCallback="true">          ← COMPLY-02
     <activity android:theme="@style/Theme.Stash.Splash">              ← COMPLY-04
     [DELETED] <uses-permission ...POST_NOTIFICATIONS />               ← COMPLY-03
     [DELETED] <uses-permission ...FOREGROUND_SERVICE_MEDIA_PLAYBACK />  ← COMPLY-05
     [KEPT]    <uses-permission ...FOREGROUND_SERVICE />               ← preserved

   Build (one-time changes):
     app/build.gradle.kts:  android { androidResources {              ← COMPLY-06
                              generateLocaleConfig = true } }
     → AGP generates: app/build/intermediates/.../locale_config.xml
     → Merged manifest gains: <application android:localeConfig="@xml/locale_config" />
```

### Component Responsibilities

| File | Phase 2 Change | Tier |
|------|----------------|------|
| `app/src/main/res/values/themes.xml` | Strip `statusBarColor`+`navigationBarColor`; ADD `Theme.Stash.Splash` style | Resources |
| `app/src/main/AndroidManifest.xml` | Add `enableOnBackInvokedCallback="true"`; change activity theme to `Theme.Stash.Splash`; remove 2 `<uses-permission>` lines | Manifest |
| `app/src/main/java/io/stashapp/android/MainActivity.kt` | Add `installSplashScreen()` + `setKeepOnScreenCondition` (Pattern A) before `super.onCreate`; add `LaunchedEffect` that flips `AtomicBoolean` inside `StashAppContent` | App |
| `app/build.gradle.kts` | Add `androidResources { generateLocaleConfig = true }`; add `implementation(libs.androidx.core.splashscreen)` | App build |
| `gradle/libs.versions.toml` | Add `coreSplashscreen = "1.0.1"` + `androidx-core-splashscreen = …` library entry | Catalog |
| `app/src/main/res/values/strings.xml` | Add `settings_language` + `settings_language_description` keys | Resources |
| `feature/player/src/main/java/io/stashapp/android/feature/player/PlayerScreen.kt` | Line 187: `BackHandler { onExit() }` → `PredictiveBackHandler { progress -> try { progress.collect {} ; onExit() } catch (e: CancellationException) {} }`; wrap control overlay `Box` in `Modifier.safeDrawingPadding()` (PlayerScreen.kt around the controls block, line numbers depend on layout) | feature |
| `feature/library/src/main/java/io/stashapp/android/feature/library/FilterSheet.kt` | Add `contentWindowInsets = { WindowInsets.navigationBars }` to `ModalBottomSheet` call (line 71) | feature |
| `core/ui/src/main/java/io/stashapp/android/core/ui/nav/BottomNav.kt` | Add `contentWindowInsets = { WindowInsets.navigationBars }` to `ModalBottomSheet` in `MoreSheet` composable (around line 177) | core/ui |
| `core/ui/src/main/java/io/stashapp/android/core/ui/nav/NavCustomizeSheet.kt` | Add `contentWindowInsets = { WindowInsets.navigationBars }` to `ModalBottomSheet` call | core/ui |
| `feature/settings/src/main/java/io/stashapp/android/feature/settings/SettingsScreen.kt` | Add `LanguageRow` composable + call site in main settings list | feature |

### Recommended Project Structure (Phase 2 — additive only)

```
app/
├── build.gradle.kts                        # ADD: generateLocaleConfig + splash dep
├── src/main/
│   ├── AndroidManifest.xml                 # MODIFY: 4 changes (back flag, splash theme, 2 perm deletes)
│   ├── java/io/stashapp/android/
│   │   └── MainActivity.kt                 # MODIFY: installSplashScreen + AtomicBoolean gate
│   └── res/
│       ├── values/
│       │   ├── strings.xml                 # ADD: 2 language strings
│       │   └── themes.xml                  # MODIFY: strip bar colors, ADD Theme.Stash.Splash
│       └── xml/                            # NO MANUAL FILE — AGP generates locale_config
gradle/
└── libs.versions.toml                      # ADD: coreSplashscreen + library entry
feature/
├── player/.../PlayerScreen.kt              # MODIFY: BackHandler → PredictiveBackHandler + safeDrawingPadding wrap
├── library/.../FilterSheet.kt              # MODIFY: contentWindowInsets on ModalBottomSheet
└── settings/.../SettingsScreen.kt          # ADD: LanguageRow composable + call site
core/ui/.../BottomNav.kt                    # MODIFY: contentWindowInsets on MoreSheet's ModalBottomSheet
core/ui/.../NavCustomizeSheet.kt            # MODIFY: contentWindowInsets on ModalBottomSheet
.planning/phases/02-comply-platform-compliance/
├── screenshots/                            # NEW (8th commit): 12 PNGs
│   ├── home-light.png      home-dark.png
│   ├── library-light.png   library-dark.png
│   ├── player-controls-visible-light.png   player-controls-visible-dark.png
│   ├── player-controls-hidden-light.png    player-controls-hidden-dark.png
│   ├── settings-light.png  settings-dark.png
│   └── connection-light.png connection-dark.png
├── SCREENSHOTS.md                          # NEW: index + capture metadata
└── 02-UAT.md                               # NEW: DEVICE_TESTING.md result rows
```

### Pattern B1: themes.xml diff — strip bar colors, add splash theme

**What:** Remove the two hardcoded system-bar color overrides that defeat `enableEdgeToEdge()`. Add the splash style as a new parent that flows to the existing `Theme.Stash`.

**When to use:** COMPLY-01 + COMPLY-04 land in same `themes.xml` change (planner may split into 2 commits if preferred — both touch the same file).

```xml
<!-- BEFORE -->
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.Stash" parent="android:Theme.Material.NoActionBar">
        <item name="android:windowLightStatusBar">false</item>
        <item name="android:statusBarColor">@android:color/black</item>          <!-- DELETE -->
        <item name="android:navigationBarColor">@android:color/black</item>      <!-- DELETE -->
        <item name="android:windowBackground">@android:color/black</item>
    </style>
</resources>

<!-- AFTER -->
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- enableEdgeToEdge() requires no hardcoded bar colors; system handles
         tinting from windowLightStatusBar + ui content. -->
    <style name="Theme.Stash" parent="android:Theme.Material.NoActionBar">
        <item name="android:windowLightStatusBar">false</item>
        <item name="android:windowBackground">@android:color/black</item>
    </style>

    <!-- COMPLY-04 splash theme. Transitions to Theme.Stash after first
         composition (postSplashScreenTheme). -->
    <style name="Theme.Stash.Splash" parent="Theme.SplashScreen">
        <item name="windowSplashScreenAnimatedIcon">@mipmap/ic_launcher</item>
        <item name="windowSplashScreenBackground">@android:color/black</item>
        <item name="postSplashScreenTheme">@style/Theme.Stash</item>
    </style>
</resources>
```

**Verification:** `grep -E 'statusBarColor|navigationBarColor' app/src/main/res/values/themes.xml` → empty.

### Pattern B2: AndroidManifest.xml — 4 surgical edits

```xml
<!-- BEFORE -->
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />  <!-- COMPLY-05 DELETE -->
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />                  <!-- COMPLY-03 DELETE -->

    <application
        android:name=".StashApp"
        ...
        android:theme="@style/Theme.Stash"
        android:usesCleartextTraffic="true"
        tools:targetApi="33">                                                                 <!-- COMPLY-02 ADD enableOnBackInvokedCallback="true" -->

        <activity
            android:name=".MainActivity"
            android:exported="true"
            ...
            android:theme="@style/Theme.Stash">                                                <!-- COMPLY-04 CHANGE to @style/Theme.Stash.Splash -->
            ...
        </activity>
    </application>
</manifest>

<!-- AFTER (changes only) -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <!-- COMPLY-03 + COMPLY-05: orphan permissions removed; re-added by BG-MEDIA milestone -->

    <application
        ...
        android:enableOnBackInvokedCallback="true"
        android:theme="@style/Theme.Stash"
        ...>

        <activity
            ...
            android:theme="@style/Theme.Stash.Splash">
```

**Verification grep set:**
```bash
grep -c 'android:enableOnBackInvokedCallback="true"' app/src/main/AndroidManifest.xml  # → 1
grep -c 'android.permission.POST_NOTIFICATIONS' app/src/main/AndroidManifest.xml       # → 0
grep -c 'FOREGROUND_SERVICE_MEDIA_PLAYBACK' app/src/main/AndroidManifest.xml           # → 0
grep -c '"android.permission.FOREGROUND_SERVICE"' app/src/main/AndroidManifest.xml     # → 1
grep -c '@style/Theme.Stash.Splash' app/src/main/AndroidManifest.xml                   # → 1
```

### Pattern B3: PredictiveBackHandler migration (PlayerScreen.kt:187)

```kotlin
// BEFORE — PlayerScreen.kt:8 + :187
import androidx.activity.compose.BackHandler
// ...
BackHandler { onExit() }

// AFTER
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.BackEventCompat
import kotlinx.coroutines.CancellationException
// (BackHandler import may stay if used elsewhere; in PlayerScreen it's the
//  only consumer — drop it. SPEC acceptance allows the import line to remain.)
// ...
PredictiveBackHandler { progress: Flow<BackEventCompat> ->
    try {
        progress.collect { backEvent ->
            // Phase 2: no animation work — accept the system's default preview.
            // Future Spine (Phase 5) MAY consume backEvent.progress (0f..1f) here
            // to drive a custom scale/fade on the player surface.
        }
        // Flow completed normally → commit the back action.
        onExit()
    } catch (e: CancellationException) {
        // User cancelled the swipe; do NOT call onExit().
        // Re-throw so coroutine cancellation propagates correctly upstream.
        throw e
    }
}
```

**Why this exact shape:**
- `progress.collect { }` MUST be inside `try` — when the gesture is cancelled, the system throws `CancellationException` into the suspending `collect` call. **[CITED: developer.android.com/guide/navigation/custom-back/predictive-back-gesture]**
- `onExit()` MUST be AFTER `collect` returns normally — placing it BEFORE the collect causes immediate exit on first progress event (broken UX).
- `throw e` for CancellationException — Kotlin coroutine etiquette: never swallow CancellationException, always re-throw so the parent scope sees the cancellation. **[CITED: kotlinlang.org coroutines guide — cancellation propagation rule]**
- No `enabled` parameter — PITFALLS §8 hazard "never mutate enabled mid-gesture" doesn't apply here (the original `BackHandler { }` was unconditional, the migration stays unconditional).

**Verification:**
```bash
grep -c "BackHandler\b" feature/player/src/main/java/io/stashapp/android/feature/player/PlayerScreen.kt
# expects at most 1 (an import line — the call site is now PredictiveBackHandler)
grep -c "PredictiveBackHandler" feature/player/src/main/java/io/stashapp/android/feature/player/PlayerScreen.kt
# expects ≥ 1 (import + call site = 2)
grep -rn "BackHandler\b" --include="*.kt" feature/ app/ core/
# expects only the PlayerScreen import line; no other call sites
```

### Pattern B4: installSplashScreen + Pattern A keep-condition

**Edit MainActivity.kt:**

```kotlin
// ADD imports near top
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import java.util.concurrent.atomic.AtomicBoolean
import androidx.compose.runtime.LaunchedEffect

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    // Pattern A: flipped to true once RootViewModel.start emits non-null.
    // Lives on the Activity instance so the keep-condition lambda (called by
    // the SplashScreen on each draw attempt) can read it without touching
    // the Hilt graph before super.onCreate.
    private val appReady = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        // installSplashScreen MUST be called BEFORE super.onCreate. The
        // SplashScreen library swaps the activity's theme back to the
        // postSplashScreenTheme (Theme.Stash) at this point.
        // [CITED: developer.android.com/develop/ui/views/launch/splash-screen/migrate]
        val splashScreen: SplashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !appReady.get() }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestHighestRefreshRate()
        setContent {
            StashTheme {
                val rootViewModel: RootViewModel = hiltViewModel()

                // Pattern A: flip the gate when RootViewModel resolves the
                // start destination. The splash keep-condition is polled on
                // every draw attempt, so this latency is ≤ 1 frame after the
                // StateFlow emits.
                val start by rootViewModel.start.collectAsState()
                LaunchedEffect(start) {
                    if (start != null) appReady.set(true)
                }

                StashAppContent(rootViewModel = rootViewModel)
            }
        }
    }
    // ... requestHighestRefreshRate() unchanged
}
```

**Why Pattern A:**
- `installSplashScreen()` must run before `super.onCreate` — verified canonical pattern from migration docs **[CITED: developer.android.com/develop/ui/views/launch/splash-screen/migrate]**.
- Hilt graph is NOT available before `super.onCreate` — `hiltViewModel()` requires the Activity to be at least RESUMED.
- `AtomicBoolean` on the Activity instance is thread-safe AND survives configuration changes (no — Activity is recreated on rotation; `AtomicBoolean` is fresh each time. This is fine because Pattern A's purpose is COLD-launch splash; subsequent rotations don't show splash).
- The `setKeepOnScreenCondition` lambda is polled per-draw-attempt by the SplashScreen library's pre-draw listener — reading an `AtomicBoolean` is O(1) and safe to call from the UI thread.
- The ~50ms splash extension (one frame for first composition + LaunchedEffect to fire + AtomicBoolean to flip + next pre-draw to read) is invisible to humans.

**ANR risk:** If `ConnectionRepository.activeServer()` never emits (e.g., DataStore I/O blocks indefinitely), the splash never dismisses and the system shows an ANR after ~5 seconds. **Mitigation:** verified in existing code — `RootViewModel.init` uses `collectLatest` inside `viewModelScope.launch`; DataStore `Flow<Preferences>` has guaranteed first-emit semantics (emits current value immediately on collect). No ANR risk in practice. **[VERIFIED: MainActivity.kt:67-76, RootViewModel init block]**

### Pattern B5: generateLocaleConfig — build wiring

**Edit `app/build.gradle.kts`** — add inside the `android { }` block, sibling to `defaultConfig { }`:

```kotlin
android {
    namespace = "io.stashapp.android"

    defaultConfig {
        // ... existing
    }

    // COMPLY-06: AGP scans values-* directories and generates
    // app/build/intermediates/generated_res/.../locale_config.xml at build time,
    // then auto-merges <application android:localeConfig="@xml/locale_config" />
    // into the final manifest. No source-tree file needed.
    androidResources {
        generateLocaleConfig = true
    }

    // ... rest unchanged
}
```

**What AGP does at build time:**
1. Scans `app/src/main/res/values-*/` directories AND library dependencies' `values-*/` for locale qualifiers.
2. Writes `app/build/intermediates/generated_res/<variant>/locale_config.xml` containing a `<locale-config>` element with one `<locale>` per discovered qualifier.
3. Adds `android:localeConfig="@xml/locale_config"` to the merged manifest's `<application>` element.

**Behavior with English-only repo (current state — verified `find . -path "*/res/values-*"` returns nothing):**
- AGP generates a minimal `locale_config.xml` containing only the default locale (sourced from `resources.properties → unqualifiedResLocale`, or defaulting to `en` per AGP convention).
- The system per-app language dialog will open and show only "App default" — passing the COMPLY-06 acceptance check ("dialog opens") with the documented caveat that translations are out of scope.
- **[VERIFIED: developer.android.com/guide/topics/resources/app-languages — feature available since AGP 8.1, current AGP 8.7.3 supports it]**

**Note:** If AGP issues a warning about missing `resources.properties` for the unqualified locale, add `app/src/main/res/resources.properties` with `unqualifiedResLocale=en`. Verify against actual build output during plan execution.

### Pattern B6: LanguageRow composable

```kotlin
// feature/settings/.../SettingsScreen.kt — ADD imports
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource

// ADD composable — place near other private row composables in the file
// (e.g., after SectionHeader / SwitchPref / SliderPref definitions, around line 287+)
@Composable
private fun LanguageRow() {
    // Per-app language picker only exists API 33+. Return null on older
    // devices — the row simply doesn't render.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = LocalContext.current
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_language)) },
        supportingContent = { Text(stringResource(R.string.settings_language_description)) },
        leadingContent = {
            Icon(
                Icons.Outlined.Language,
                contentDescription = null,
            )
        },
        modifier = Modifier.clickable {
            val intent = Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
        },
    )
}

// CALL — add to the main settings list, top-level row above any "Advanced" /
// "Diagnostics" section, below the connection block. Inspect SettingsScreen.kt
// during planning for the exact placement that matches the existing voice.
LanguageRow()
```

**Why `Uri.fromParts("package", packageName, null)` vs `Uri.parse("package:$packageName")`:**
- Functionally equivalent for this case but `fromParts` is the documented canonical form for opaque URIs with a known scheme — avoids ambiguity if `packageName` ever contains unexpected characters. **[CITED: developer.android.com Settings.ACTION_APP_LOCALE_SETTINGS Intent documentation]**

**Edit `app/src/main/res/values/strings.xml`:**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Stash</string>

    <!-- COMPLY-06: per-app language picker (API 33+) -->
    <string name="settings_language">Language</string>
    <string name="settings_language_description">Choose the language for this app independently from your device.</string>
</resources>
```

### Anti-Patterns to Avoid

- **Calling `installSplashScreen()` AFTER `super.onCreate()`** — splash never paints; activity proceeds with whatever theme is set on `<activity android:theme=…>` (which would now be the splash theme, leaving the splash visible until manually swapped). **[CITED: developer.android.com migration guide — explicit "BEFORE super.onCreate" instruction]**
- **Hardcoding `WindowInsets(0)` on Scaffold** — defeats Material3's automatic inset handling; per-child content must then re-implement inset awareness. PITFALLS §7 calls this out.
- **Mutating `enabled` mid-gesture on PredictiveBackHandler** — PITFALLS §8 hazard. Phase 2's PlayerScreen migration is unconditional (`PredictiveBackHandler { … }`, no `enabled = …`) so this doesn't apply, but the planner must NOT introduce conditional enabling during the migration.
- **Reading `RootViewModel` before `super.onCreate` via Hilt** — Hilt graph isn't constructed yet; `hiltViewModel()` returns `null` or crashes. Pattern A defers Hilt access to first composition.
- **Adding `android:localeConfig` manually to AndroidManifest.xml** — AGP's manifest merger writes the attribute. A manual entry causes a merge conflict warning (or worse, overrides AGP's generated reference and points at a non-existent `res/xml/locale_config.xml`). **[VERIFIED: developer.android.com/guide/topics/resources/app-languages — "you won't see an android:localeConfig entry in your app's manifest"]**
- **Using `Uri.parse("package:$packageName")` for the locale Intent** — works in practice but `Uri.fromParts` is the documented form; some lint rules flag the string-concat variant.
- **Pre-painting in Spine tokens** — CONTEXT.md explicit constraint. Phase 2 uses `Icons.Outlined.Language` from material-icons-extended, `Theme.Stash` colors via inheritance, current SettingsScreen row scaffold pattern. No Spine palette / typography references.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Pre-Android-12 splash compat | Custom Activity that shows a logo then launches MainActivity | `androidx.core:core-splashscreen` `installSplashScreen()` | Library handles API 23–31 (theme-window splash) AND API 31+ (system SplashScreen) with one API; manual approach loses the system's animated handoff |
| Back gesture preview animation | Custom touch interceptor + manual gesture-distance tracking | `androidx.activity.compose.PredictiveBackHandler` | System owns the gesture; library exposes `Flow<BackEventCompat>` so progress (0..1), swipeEdge, touchX/Y are all canonical; manual tracking forks from system semantics |
| Locale picker UI | Custom `Dialog` with locale list + per-locale flags | `Settings.ACTION_APP_LOCALE_SETTINGS` Intent + `generateLocaleConfig` | System dialog respects OS-level a11y, RTL, and locale-list ordering; auto-generated `locale_config.xml` stays in sync with `values-*/` dirs |
| `locale_config.xml` | Manually authored `app/src/main/res/xml/locale_config.xml` listing every locale | `android { androidResources { generateLocaleConfig = true } }` | AGP auto-discovers from `values-*/`; manual file goes stale when translators add a new locale dir |
| Inset-aware Scaffold | Custom `Modifier.windowInsetsPadding(…)` wrapping every screen | Material3 `Scaffold` default `contentWindowInsets = ScaffoldDefaults.contentWindowInsets` | Default IS `WindowInsets.systemBars` — same value the custom modifier would compute, with proper consume-by-child semantics |
| ModalBottomSheet inset handling | Custom Box(insetPadding) inside every sheet's content | `ModalBottomSheet contentWindowInsets` parameter | Library-aware: sheet's drag handle + scrim render correctly; child content gets the right `PaddingValues` automatically |

**Key insight:** Every one of these "system contracts" was either historically a write-your-own surface (splash pre-2021, back handler pre-2024) or is a system-owned surface that the framework now exposes. Phase 2's job is to USE the system APIs that ship for these problems, not re-implement them. Custom solutions are worse not only because of edge cases but because they DIVERGE from system behavior — users notice when an in-app dialog looks/behaves different from every other app's system dialog.

## Runtime State Inventory

**This is a code/manifest-edit phase — not a rename/refactor/migration.** No persistent strings, user IDs, or service registrations are being renamed.

| Category | Items Found | Action Required |
|----------|-------------|------------------|
| Stored data | None — no string keys are being changed in any datastore. ConnectionStore, PlayerPreferences, UiPreferences keys unchanged. | None |
| Live service config | None — no external services (n8n, Datadog, etc.) reference Slopper config. | None |
| OS-registered state | **Adaptive launcher icon stays `@mipmap/ic_launcher`** — the same asset becomes the splash icon. No re-registration needed. The Android system reads `<activity android:theme="@style/Theme.Stash.Splash">` at app start (no persistent OS cache to invalidate). | Verify on real S23+ that splash icon renders correctly (Claude's Discretion: fall back to `@drawable/splash_icon` foreground variant if adaptive crop is ugly). |
| Secrets / env vars | None — no env var or secret name changes. | None |
| Build artifacts / installed packages | `app/build/` and `*/build/` directories must be cleaned after the `generateLocaleConfig = true` toggle to ensure AGP regenerates the manifest with the `android:localeConfig` reference. A stale `app/build/intermediates/merged_manifests/` may show the old manifest without the attribute. | Run `./gradlew clean assembleDebug` once after the COMPLY-06 commit to force regeneration; verify the generated locale_config.xml exists at `app/build/intermediates/generated_res/.../locale_config.xml`. |

**The canonical question — "After every file in the repo is updated, what runtime systems still have the old string cached, stored, or registered?"** — Answer: nothing. This is a pure code+config phase with no live-state migrations.

## Common Pitfalls

### Pitfall E1: 3-button-nav devices show black scrim under nav bar with edge-to-edge

**What goes wrong:** On Android devices with 3-button navigation (older Pixels, most non-Pixel OEMs), `enableEdgeToEdge()` causes content to draw under the nav bar but the system shows a black translucent scrim there for affordance. ModalBottomSheet content without `contentWindowInsets = { WindowInsets.navigationBars }` renders behind that scrim, hiding the bottom-most action button.

**Why it happens:** Compose Material3 default `BottomSheetDefaults.modalWindowInsets` IS `WindowInsets.navigationBars` in current versions, so the default already does the right thing. **But** Phase 2 is locking the value explicitly (CONTEXT.md Decision 4) — this documents intent and protects against future Material3 default changes.

**How to avoid:** Pass `contentWindowInsets = { WindowInsets.navigationBars }` explicitly to all three ModalBottomSheets (FilterSheet, NavCustomizeSheet, MoreSheet).

**Warning signs:**
- FilterSheet "Apply" button half-hidden behind nav scrim on a 3-button-nav device.
- NavCustomizeSheet checkboxes for the last row cut off.

**Phase to address:** COMPLY-01.
**Severity:** Medium (S23+ uses gesture nav by default so this isn't testable on Slopper's primary UAT device; documented as REVIEWS-C4 ACCEPT via COMPLY-07-3BTN backlog).
**[SOURCE: PITFALLS.md §7 cross-referenced with verified Material3 ModalBottomSheet signature, composables.com/material3/modalbottomsheet]**

### Pitfall E2: PredictiveBackHandler with conditional `enabled` flag during a gesture

**What goes wrong:** If `PredictiveBackHandler(enabled = someState)` and `someState` flips false mid-swipe (e.g., a dismiss animation completes and sets it false), the system's preview shows the WRONG destination, then either commits to nothing or crashes the gesture state machine.

**Why it happens:** Pre-predictive-back, `BackHandler` ran atomically. Predictive-back is multi-phase (start → progress → commit/cancel) — mutating `enabled` mid-phase leaves the system in an inconsistent state.

**How to avoid:** In Phase 2, `PlayerScreen.kt:187` BackHandler is unconditional — the migration MUST preserve that unconditionality. Do NOT add `enabled = playerState.isReady` or similar. If a future feature needs conditional back handling, gate the ACTION inside the lambda, not the `enabled` parameter.

**Warning signs:**
- Back-swipe preview shows the library screen but player stays visible on commit.
- BackHandler callback fires twice on cancel.

**Phase to address:** COMPLY-02.
**Severity:** High if introduced; LOW for Phase 2 because the existing site is unconditional.
**[SOURCE: PITFALLS.md §8 + verified PredictiveBackHandler API contract from developer.android.com/guide/navigation/custom-back/predictive-back-gesture]**

### Pitfall E3: Splash keep-condition never flips → ANR after 5 seconds

**What goes wrong:** If `setKeepOnScreenCondition { someFlag }` returns true indefinitely (e.g., the StateFlow being awaited never emits), Android shows an ANR ("App not responding") after the system-default ANR timeout (~5s).

**Why it happens:** The pre-draw listener inside `core-splashscreen` blocks the main thread's drawing path while keep-condition returns true. Indefinite blocking = ANR.

**How to avoid:**
- Verify the StateFlow used for gating has a guaranteed first-emit. `RootViewModel.start` uses `ConnectionRepository.activeServer()` (a DataStore-backed Flow) — DataStore Flows emit current value immediately on collect, so first-emit is sub-millisecond.
- Add a SAFETY TIMEOUT in Pattern A: a `LaunchedEffect` with `delay(3000); appReady.set(true)` as a fallback if `rootViewModel.start` never emits.

**Warning signs:**
- App freezes on splash on first cold launch after install.
- `adb logcat | grep ANR` shows ANR in MainActivity within 5s of launch.

**Phase to address:** COMPLY-04.
**Severity:** High if the gate Flow has bugs; LOW for the current `RootViewModel.start` design (DataStore-backed, verified first-emit semantics).

**Recommended safety pattern (Claude's Discretion — planner may include or omit):**
```kotlin
LaunchedEffect(start) {
    if (start != null) appReady.set(true)
}
// Belt-and-suspenders: even if start never emits, dismiss splash after 3s
// to avoid ANR. In practice the StateFlow emits in <50ms.
LaunchedEffect(Unit) {
    kotlinx.coroutines.delay(3000)
    appReady.set(true)
}
```

**[SOURCE: developer.android.com/develop/ui/views/launch/splash-screen — "splash screen must only be dismissed with onResume() when the app is stable"; ANR risk inferred from main-thread blocking semantics of OnPreDrawListener]**

### Pitfall E4: `generateLocaleConfig = true` with no `values-*/` dirs produces a "single locale" dialog

**What goes wrong:** Slopper's repo has zero `values-xx/` directories (verified: `find . -path "*/res/values-*" -type d` returns nothing). With `generateLocaleConfig = true`, AGP generates a locale_config.xml with only the default locale (English). The system per-app language dialog opens but shows only "App default" — not a useful picker.

**Why it happens:** Per-app language is a localization feature; without translations there are no alternatives to pick.

**How to avoid:** Document this in the plan as expected behavior. The COMPLY-06 acceptance criteria say "Settings → Language row visible → tap opens system per-app language dialog showing app's supported locales" — a single-locale dialog DOES satisfy this (the system dialog opens; the locale list is empty-ish by design). Translation work is explicitly out-of-scope (Deferred Ideas).

**Warning signs:**
- QA reviewer flags "the language dialog only shows English — is that a bug?" → answer: no, working as intended pending translations.

**Phase to address:** COMPLY-06.
**Severity:** LOW — meets acceptance but the user-visible value is muted.
**[SOURCE: developer.android.com/guide/topics/resources/app-languages + grep verification on the repo]**

### Pitfall E5: Adaptive launcher icon crops awkwardly in splash circle

**What goes wrong:** `windowSplashScreenAnimatedIcon = @mipmap/ic_launcher` references an adaptive icon (foreground + background layers). The Android 12+ splash system masks the icon into a circle (240dp diameter on most devices). If the adaptive icon's foreground extends beyond the inner 66% safe zone (`72dp` of `108dp`), it gets cropped at the circle edge — looks visually broken.

**Why it happens:** Adaptive icons are designed for launcher icon shapes (various OEM masks); the splash's circular mask is stricter.

**How to avoid:** Inspect the actual splash render on a real S23+ device. If cropping is visible, fall back to a foreground-only `@drawable/splash_icon`:
1. Extract the launcher icon's foreground layer (`mipmap-anydpi-v26/ic_launcher.xml`'s `<foreground>`).
2. Create `app/src/main/res/drawable/splash_icon.xml` with just that foreground.
3. Reference `windowSplashScreenAnimatedIcon = @drawable/splash_icon` in `Theme.Stash.Splash`.

This is captured as Claude's Discretion in CONTEXT.md — the planner may default to `@mipmap/ic_launcher` and only swap if the UAT screenshots reveal cropping.

**Warning signs:**
- Splash icon looks zoomed/clipped on S23+.
- Top of the icon's foreground graphic touches the circle edge.

**Phase to address:** COMPLY-04.
**Severity:** LOW (cosmetic; recoverable in a follow-up tweak).
**[SOURCE: developer.android.com/develop/ui/views/launch/splash-screen — icon dimension table]**

### Pitfall E6: AGP manifest merge writes `localeConfig` AFTER source manifest is read

**What goes wrong:** If a developer adds `android:localeConfig="@xml/locale_config"` manually to `app/src/main/AndroidManifest.xml` (thinking they need to wire it), the AGP manifest merger logs a warning OR overrides the manual entry with its own, pointing at a path that doesn't exist in source.

**How to avoid:** Do NOT touch AndroidManifest.xml for COMPLY-06. The only file changes are `app/build.gradle.kts` (toggle) and `app/src/main/res/values/strings.xml` (new string).

**Warning signs:**
- Build warning `Manifest merger failed : Attribute application@localeConfig …`.
- `grep localeConfig app/src/main/AndroidManifest.xml` returns a line (it shouldn't).

**Phase to address:** COMPLY-06.
**Severity:** LOW (caught at build time).
**[SOURCE: developer.android.com/guide/topics/resources/app-languages — "you won't see an android:localeConfig entry in your app's manifest"]**

### Pitfall E7: Strong skipping silently breaks `LaunchedEffect(rootViewModel.start)` for Pattern A

**What goes wrong:** PITFALLS §5 calls out that strong-skipping (on by default in Kotlin 2.0.20+, which we're at on 2.2.20) memoizes lambdas with unstable captures. If `LaunchedEffect(rootViewModel.start)` is keyed on the `StateFlow` object itself (not its emitted value), strong skipping may keep the lambda stable across recompositions and the effect never re-triggers.

**How to avoid:** Pattern A's `LaunchedEffect(start)` keys on the COLLECTED VALUE (`String?`), not the StateFlow. Verified in the code example above — `val start by rootViewModel.start.collectAsState()` then `LaunchedEffect(start) { … }`. The key is `String?` which IS stable. No issue.

**Warning signs:**
- Splash never dismisses even though `RootViewModel.start` emits.
- Adding a `println` inside the `LaunchedEffect` shows it never fires after first composition.

**Phase to address:** COMPLY-04.
**Severity:** LOW (guarded by correct key choice in the canonical example).
**[SOURCE: PITFALLS.md §5 — strong skipping interaction with LaunchedEffect keys]**

## Code Examples

### F1: `themes.xml` final state (both COMPLY-01 + COMPLY-04 applied)

See Pattern B1 above — complete file body shown.

### F2: PlayerScreen control-overlay safeDrawingPadding wrap (COMPLY-01b)

```kotlin
// PlayerScreen.kt — inside the outer Box(Modifier.fillMaxSize().background(Color.Black))
// AFTER the AndroidView { PlayerView … } (which stays full-bleed):

Box(
    modifier = Modifier
        .fillMaxSize()
        .safeDrawingPadding(),  // ← COMPLY-01: insets the overlay away from system bars
) {
    // existing control overlays — top bar, bottom seek bar, gesture indicators
    // (the SurfaceView stays edge-to-edge; only the control layer respects insets)
}
```

**Why a separate Box wrapper:** PITFALLS §7 explicit guidance — applying `safeDrawingPadding` to the `AndroidView { PlayerView … }` causes the video letterbox to inset (visible black bars where the system bars used to be). The wrap pattern keeps video edge-to-edge while controls respect cutouts.

### F3: PredictiveBackHandler (COMPLY-02)

See Pattern B3 above — complete migration snippet shown.

### F4: Permission removal (COMPLY-03 + COMPLY-05)

`git diff` would be exactly two `-<uses-permission android:name=…/>` lines deleted from `app/src/main/AndroidManifest.xml`. No code change. Acceptance is grep-driven.

### F5: installSplashScreen + Pattern A (COMPLY-04)

See Pattern B4 above — complete MainActivity diff shown.

### F6: Theme.Stash.Splash style (COMPLY-04)

Part of F1 — included in the themes.xml `AFTER` block.

### F7: generateLocaleConfig (COMPLY-06)

See Pattern B5 above — complete `app/build.gradle.kts` diff shown.

### F8: LanguageRow composable + strings.xml (COMPLY-06)

See Pattern B6 above — complete composable + string resources shown.

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `<activity android:theme="@style/CustomSplashTheme">` + manual switch in `onCreate` | `androidx.core:core-splashscreen` `installSplashScreen()` | Stable since 1.0.0 (Mar 2022); current stable 1.2.0 | Phase 2 uses the library; obsolete pattern would conflict with Android 12+ system SplashScreen API |
| `BackHandler { onExit() }` for back gestures | `PredictiveBackHandler { progress -> try { progress.collect {} ; onExit() } catch (CancellationException) {} }` | `PredictiveBackHandler` added in Activity Compose 1.8.0 (Apr 2024). Will be deprecated when `NavigationBackHandler` ships in Activity Compose 1.10+ (already CMP 1.10) | Phase 2 uses `PredictiveBackHandler` per CONTEXT.md (locked at floor); future-AGP-9 phase migrates to `NavigationBackHandler` |
| Manual `app/src/main/res/xml/locale_config.xml` + `<application android:localeConfig=…>` in source manifest | `android { androidResources { generateLocaleConfig = true } }` | AGP 8.1 (Aug 2023); current 8.7.3 supports | Phase 2 uses build-time generation; manual file is obsolete |
| `WindowCompat.setDecorFitsSystemWindows(window, false)` + `window.statusBarColor = …` | `enableEdgeToEdge()` (Activity Compose 1.8+) + theme cleanup | Activity Compose 1.8 (May 2024); on Android 15+ (compileSdk 35) the legacy color setters are no-ops | Existing code already calls `enableEdgeToEdge()`; Phase 2 removes the theme overrides that defeat it |
| Custom Activity-based splash sequence (lottie / launchimage + 2s delay) | System SplashScreen API + keep-condition tied to real app-ready state | Android 12 (API 31); backwards-compat via core-splashscreen 1.0.1+ | Phase 2 lands canonical pattern |

**Deprecated / outdated patterns to avoid:**
- `android:statusBarColor` / `android:navigationBarColor` in themes — no-op on Android 15+ (compileSdk 35) when edge-to-edge is enabled. Phase 2 deletes them.
- `WindowInsetsControllerCompat.show/hide` for system-bar visibility on cold launch — superseded by the `enableEdgeToEdge` + per-screen safeDrawing pattern.
- `BackHandler { dismissDialog(); navigateBack() }` — works but gives snap-exit UX. Replace with `PredictiveBackHandler` for any user-visible back gesture surface.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | The CONTEXT.md-locked `core-splashscreen:1.0.1` version is acceptable to ship even though `1.2.0` is the current stable | Standard Stack | LOW — both expose identical public API for the operations Phase 2 needs (`installSplashScreen`, `setKeepOnScreenCondition`, `Theme.SplashScreen` parent). If the planner wants to upgrade to 1.2.0 for the audit trail, it's a one-line catalog change. |
| A2 | `BottomSheetDefaults.modalWindowInsets` resolves to `WindowInsets.navigationBars` in Compose BOM 2026.05.00 (so explicitly passing `WindowInsets.navigationBars` is documentation, not behavior change) | Standard Stack + Pitfalls E1 | LOW — even if the default differs, explicitly passing the value Phase 2 wants is correct and overrides any default. Worst case: a slight redundancy in the explicit declaration. |
| A3 | `ScaffoldDefaults.contentWindowInsets` equals `WindowInsets.systemBars` in Compose BOM 2026.05.00 (so unmodified top-level Scaffolds in Home/Library/Settings/Browse/Detail already satisfy the inset-aware acceptance criterion) | Architecture Patterns + Pitfalls | LOW-MEDIUM — if the default ever changed to `WindowInsets(0)`, every Scaffold would draw under the status bar with no padding. Mitigation: the COMPLY-01 manual UAT (screenshots on S23+) catches this empirically. |
| A4 | DataStore-backed `ConnectionRepository.activeServer()` Flow has guaranteed first-emit within tens of milliseconds (so Pattern A's splash keep-condition flips in <100ms in practice) | Architecture Patterns B4 + Pitfall E3 | LOW — DataStore Preferences DataStore is documented to emit current value immediately on collect. Mitigation: optional safety-timeout `LaunchedEffect` shown in Pitfall E3 fallback. |
| A5 | Material-icons-extended `Icons.Outlined.Language` exists and renders as the universal "globe with meridians" globe icon at all sizes used by the Settings ListItem | Code Examples F8 | LOW — `Icons.Outlined.Language` is a stable material-icons-extended symbol since the artifact's inception; verified in the existing codebase via `grep -rn "Icons.Outlined" --include="*.kt"` returning many other Outlined icon usages from the same artifact. |
| A6 | `Settings.ACTION_APP_LOCALE_SETTINGS` Intent doesn't require any additional `<queries>` manifest entry to launch from Slopper's app (since the target is `android.settings`) | Code Examples F8 | LOW — system settings activities are universally callable without package-visibility queries (they're in the same `system` app domain). Verified pattern from developer.android.com guide. |
| A7 | Removing `POST_NOTIFICATIONS` does not break any indirect code path — no `NotificationManager` / `NotificationCompat` / `notify(` call site exists in the repo | Architecture Patterns B3 | LOW — verified via `grep -rn "NotificationManager\|NotificationCompat\|\.notify(" --include="*.kt"` in spec phase (SPEC.md confirms zero matches). Phase 2 plan should re-run this grep as a verification step before the COMPLY-03 commit. |
| A8 | The `MoreSheet` ModalBottomSheet (at `core/ui/.../BottomNav.kt:177`) also needs the `contentWindowInsets = { WindowInsets.navigationBars }` parameter, even though SPEC.md said "MoreSheet does NOT exist" | Component Responsibilities table | MEDIUM — SPEC.md note is wrong (verified by grep). The planner MUST include MoreSheet in the FilterSheet+NavCustomizeSheet inset-modifier list. Acceptance grep `grep -rn ModalBottomSheet --include="*.kt"` should return 3 matches, all of which must have `contentWindowInsets`. |

**If this table is non-empty:** A1, A4, A8 in particular deserve a quick check by the planner during plan-writing. A8 is the most concrete — SPEC.md's "MoreSheet does NOT exist" line is factually wrong (the composable exists at `BottomNav.kt:167`); plans must extend the inset-modifier scope to cover all THREE ModalBottomSheets (FilterSheet, NavCustomizeSheet, MoreSheet) rather than two.

## Open Questions

1. **Should the safety-timeout `LaunchedEffect(Unit) { delay(3000); appReady.set(true) }` be included in Pattern A?**
   - What we know: `RootViewModel.start` is DataStore-backed; first-emit is sub-100ms in practice; the safety timeout is paranoia, not necessity.
   - What's unclear: whether the team prefers belt-and-suspenders robustness over minimal-change discipline.
   - Recommendation: include the safety timeout (3 lines of code, zero behavioral cost when working correctly, ANR insurance if the DataStore flow ever regresses). Planner picks.

2. **Should COMPLY-03 and COMPLY-05 be one commit or two?**
   - What we know: CONTEXT.md says default = two separate commits; optional fold-to-one if planner judges bisect-loss acceptable.
   - What's unclear: nothing — clear locked decision with explicit Claude-discretion fallback.
   - Recommendation: two commits as default per CONTEXT.md.

3. **`resources.properties` file for `unqualifiedResLocale=en`?**
   - What we know: AGP 8.1+ may emit a warning if `generateLocaleConfig = true` is set without a `resources.properties` declaring the default unqualified locale.
   - What's unclear: whether AGP 8.7.3 emits that warning at build time, or silently defaults to `en` based on JDK locale.
   - Recommendation: omit the file initially; if the first build after COMPLY-06 emits a warning, add `app/src/main/res/resources.properties` with one line: `unqualifiedResLocale=en`. Plan task should include a "check build output for locale warnings" verification step.

4. **`LanguageRow` placement within `SettingsScreen.kt` — between which two existing sections?**
   - What we know: CONTEXT.md says "top-level row, above any Advanced/Diagnostics sections, below the connection block." Verified codebase has `SectionHeader("Player")` as the first major section after Scaffold; there is no "Connection" or "Advanced" header in the current file.
   - What's unclear: exact insertion point — between the Scaffold opening and `SectionHeader("Player")`? Below all existing sections as a standalone row?
   - Recommendation: planner inspects the current `SettingsScreen.kt` layout during plan-writing and picks a placement consistent with existing voice. Most natural: a `SectionHeader("App")` followed by `LanguageRow()` at the very top of the scrollable Column (above "Player" section). Decision is Claude's Discretion per CONTEXT.md.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| JDK 17 | All Gradle / Kotlin / AGP builds | ✓ | (from Phase 1 — `kotlin { jvmToolchain(17) }` enforced) | — |
| Android SDK Platform 35 | `compileSdk = 35` (unchanged) | ✓ | API 35 | — |
| Galaxy S23+ device on Android 16 (gesture nav) | COMPLY-07 manual UAT, screenshot pack | ✓ | SM-S916U1 (per Phase 1 UAT log) | — |
| 3-button-nav device (Pixel or OEM) | Edge-to-edge dual-device verification | ✗ | — | DEFERRED to backlog `COMPLY-07-3BTN` (REVIEWS-C4 ACCEPT 2026-05-17) |
| `adb` | Screenshot capture, manifest verification | ✓ | (from Phase 1 — used for `01-UAT.md`) | Device's built-in screenshot also works |
| Google Maven (for `androidx.core:core-splashscreen:1.0.1`) | COMPLY-04 dependency resolution | ✓ | Public Maven repo (already in `settings.gradle.kts` repository list) | — |

**Missing dependencies with no fallback:** None blocking Phase 2.

**Missing dependencies with fallback:**
- 3-button-nav verification → DEFERRED via REVIEWS-C4 ACCEPT (already locked in CONTEXT.md Decision 9 / Risk 1).

## Validation Architecture

> Phase 2 has no automated test infrastructure (`workflow.nyquist_validation` is irrelevant — POLISH-04/05 wires JUnit5+Turbine+MockK+Robolectric in Phase 4; Phase 2 has zero unit tests by SPEC.md design). Verification is **grep + manual UAT + screenshot evidence + build green**.

### Test Framework (effective for Phase 2)

| Property | Value |
|----------|-------|
| Framework | None (POLISH-04 lands JUnit5 in Phase 4) |
| Config file | n/a |
| Quick run command | `./gradlew --configuration-cache assembleDebug` |
| Full suite command | `./gradlew --configuration-cache assembleDebug assembleRelease check` |

### Phase Requirements → Verification Map

| Req ID | Behavior | Verification Type | Automated Command | File Exists? |
|--------|----------|-------------------|-------------------|-------------|
| COMPLY-01a | Bar colors stripped | grep | `grep -E 'statusBarColor\|navigationBarColor' app/src/main/res/values/themes.xml` (expects empty) | ✅ themes.xml |
| COMPLY-01b | Scaffolds inset-aware | grep + manual UAT | `grep -rn 'Scaffold(' --include="*.kt" app/ feature/ core/` review | ✅ all listed |
| COMPLY-01c | ModalBottomSheet contentWindowInsets | grep | `grep -rn 'ModalBottomSheet(' --include="*.kt"` returns 3; each followed by `contentWindowInsets` | ✅ FilterSheet, NavCustomizeSheet, BottomNav (MoreSheet) |
| COMPLY-01d | Screenshot pack | file existence | `ls .planning/phases/02-comply-platform-compliance/screenshots/*.png \| wc -l` ≥ 10 | ❌ generated by 8th commit |
| COMPLY-02a | Manifest flag | grep | `grep -c 'android:enableOnBackInvokedCallback="true"' app/src/main/AndroidManifest.xml` → 1 | ✅ AndroidManifest.xml |
| COMPLY-02b | PredictiveBackHandler call site | grep | `grep -rn "BackHandler\b" --include="*.kt" feature/ app/ core/` returns only import; `grep -c "PredictiveBackHandler" feature/player/.../PlayerScreen.kt` ≥ 1 | ✅ PlayerScreen.kt |
| COMPLY-03 | POST_NOTIFICATIONS removed | grep | `grep -c 'android.permission.POST_NOTIFICATIONS' app/src/main/AndroidManifest.xml` → 0 | ✅ AndroidManifest.xml |
| COMPLY-04a | Splash dep present | grep | `grep -c 'core-splashscreen' gradle/libs.versions.toml` → ≥1; `grep -c 'core.splashscreen' app/build.gradle.kts` → ≥1 | ✅ both |
| COMPLY-04b | installSplashScreen call | grep | `grep -c 'installSplashScreen' app/src/main/java/io/stashapp/android/MainActivity.kt` → 1 | ✅ MainActivity.kt |
| COMPLY-04c | setKeepOnScreenCondition call | grep | `grep -c 'setKeepOnScreenCondition' app/src/main/java/io/stashapp/android/MainActivity.kt` → 1 | ✅ MainActivity.kt |
| COMPLY-04d | Splash theme in manifest | grep | `grep -c '@style/Theme.Stash.Splash' app/src/main/AndroidManifest.xml` → 1 | ✅ AndroidManifest.xml |
| COMPLY-05 | FGS_MEDIA_PLAYBACK removed | grep | `grep -c FOREGROUND_SERVICE_MEDIA_PLAYBACK app/src/main/AndroidManifest.xml` → 0; `grep -c '"android.permission.FOREGROUND_SERVICE"' …` → 1 | ✅ AndroidManifest.xml |
| COMPLY-06a | generateLocaleConfig set | grep | `grep -c 'generateLocaleConfig = true' app/build.gradle.kts` → 1 | ✅ app/build.gradle.kts |
| COMPLY-06b | Locale config generated | file existence after build | `find app/build/intermediates -name 'locale_config.xml'` returns ≥1 file after `./gradlew assembleDebug` | ❌ build artifact |
| COMPLY-06c | ACTION_APP_LOCALE_SETTINGS call | grep | `grep -c 'ACTION_APP_LOCALE_SETTINGS' feature/settings/.../SettingsScreen.kt` → 1 | ✅ SettingsScreen.kt |
| COMPLY-07 | UAT artifacts | file existence | `ls .planning/phases/02-comply-platform-compliance/02-UAT.md` exists with PASS rows; `## Accepted Risks` block in 02-CONTEXT.md | ❌ generated by Plan 2.2 final task |

### Sampling Rate

- **Per task commit:** `./gradlew assembleDebug` (~30s incremental); the per-COMPLY grep set runs in <1s.
- **Per wave merge:** `./gradlew --configuration-cache assembleDebug assembleRelease check` (~90s on tuned 12GB host per Phase 1 LEARNINGS).
- **Phase gate:** Full suite green + manual UAT on S23+ + screenshot pack committed.

### Wave 0 Gaps

- [ ] No test framework gaps (Phase 2 deliberately has no unit-test acceptance criteria per SPEC.md). Phase 4 POLISH-04/05 owns test infrastructure.
- [ ] No fixture gaps.
- [ ] Manual UAT script: re-use `DEVICE_TESTING.md` checklist (existing repo doc); Phase 2 result rows go in `02-UAT.md`.

*(If no test framework: Phase 2's acceptance is grep + build green + manual UAT — no automated test infrastructure required or expected.)*

## Security Domain

> Security-enforcement is implicitly enabled (no `security_enforcement: false` in `.planning/config.json`). Phase 2's security surface is narrow — manifest hygiene + theme cleanup + system Intent launch — but the relevant ASVS controls are documented below.

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no | n/a — no auth surface changes; Phase 2 doesn't touch ConnectionStore / credential paths |
| V3 Session Management | no | n/a — no session state |
| V4 Access Control | partial | Manifest hygiene: removing orphan permissions reduces requested-permission surface (least-privilege) |
| V5 Input Validation | yes | `Intent(Settings.ACTION_APP_LOCALE_SETTINGS)` uses type-safe constants; `Uri.fromParts("package", context.packageName, null)` is canonical and doesn't accept user input |
| V6 Cryptography | no | n/a — no crypto |
| V7 Error Handling | partial | Pattern A's CancellationException re-throw preserves coroutine cancellation semantics; splash safety-timeout (optional) protects against ANR |
| V12 Files and Resources | partial | `generateLocaleConfig` reads only `values-*/` directories (build-time); no runtime file access introduced |
| V14 Configuration | yes | `<application android:enableOnBackInvokedCallback="true">` is a deliberate opt-in to a documented system contract; removing orphan permissions follows the Android "permission minimization" guidance |

### Known Threat Patterns for Android Compose + manifest changes

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Permission over-declaration (orphan permissions invite social engineering — "why does this app need notifications?") | Information Disclosure | Phase 2 REMOVES orphans (COMPLY-03 + COMPLY-05) — directly mitigates |
| Implicit Intent leak (calling startActivity with a non-explicit Intent that a third-party app can intercept) | Information Disclosure | `Settings.ACTION_APP_LOCALE_SETTINGS` resolves to a system component only; cannot be intercepted by 3rd-party apps |
| Manifest tamper via build flag | Tampering | `enableOnBackInvokedCallback` is a documented system contract; no security implication |
| ANR-induced DoS via splash keep-condition stuck-true | Denial of Service | Pattern A reads DataStore (verified first-emit); optional safety-timeout in Pitfall E3 fallback |
| Cleartext widening to "fix" edge-to-edge | Tampering | OUT OF SCOPE per CONTEXT.md (SEC-02 backlog); Phase 2 plans MUST NOT widen `network_security_config.xml` |
| Adaptive icon foreground replaced by malicious asset in a later commit | Tampering | Standard code-review protection; outside Phase 2 attack surface |

**Phase 2 net security posture: IMPROVES.**
- Removes 2 orphan permissions (smaller attack surface, cleaner Play Store / F-Droid manifest review).
- Introduces 1 new explicit system contract (`enableOnBackInvokedCallback`) — documented, safe.
- Introduces 1 new system Intent call site (`ACTION_APP_LOCALE_SETTINGS`) — opens system dialog only, no data flow.
- Introduces 1 new build flag (`generateLocaleConfig`) — read-only on `values-*/`, no runtime effect on data.
- Introduces 1 new dep (`core-splashscreen:1.0.1`) — official AndroidX, transitively already present in many Jetpack libs.

No new credential handling, no new network calls, no new IPC, no new exported components, no new permission requests at runtime.

## Sources

### Primary (HIGH confidence)

- [developer.android.com — Migrate your splash screen implementation to Android 12 and later](https://developer.android.com/develop/ui/views/launch/splash-screen/migrate) — canonical `installSplashScreen` BEFORE `super.onCreate` pattern + Theme.SplashScreen attribute list
- [developer.android.com — Predictive back gesture for Compose](https://developer.android.com/guide/navigation/custom-back/predictive-back-gesture) — canonical `PredictiveBackHandler` snippet with `try/catch CancellationException` + `Flow<BackEventCompat>` signature + manifest `enableOnBackInvokedCallback` requirement
- [developer.android.com — Per-app language preferences](https://developer.android.com/guide/topics/resources/app-languages) — `generateLocaleConfig` DSL location, AGP 8.1+ minimum, auto-merge behavior, "you won't see android:localeConfig in your source manifest" rule
- [developer.android.com — Splash screens (Views)](https://developer.android.com/develop/ui/views/launch/splash-screen) — icon dimension table (288dp inner / 432dp area), adaptive icon background support, AVD format note for animated icons
- [developer.android.com — Jetpack Core release notes (core-splashscreen)](https://developer.android.com/jetpack/androidx/releases/core) — confirms 1.2.0 is current stable, 1.0.1 is API-complete
- [composables.com — ModalBottomSheet API Reference](https://composables.com/material3/modalbottomsheet) — exact `ModalBottomSheet` signature confirming `contentWindowInsets: @Composable () -> WindowInsets = { BottomSheetDefaults.modalWindowInsets }` default
- `.planning/research/PITFALLS.md` §7 (edge-to-edge enforcement) — Slopper-specific patterns for PlayerScreen + FilterSheet
- `.planning/research/PITFALLS.md` §8 (predictive back hazards) — Slopper-specific guidance on `enabled` mutation + `PredictiveBackHandler` deprecation path
- `.planning/research/PITFALLS.md` §5 (strong skipping) — informs Pattern A's `LaunchedEffect(start)` key choice
- `.planning/codebase/STACK.md` — verified toolchain pins
- `.planning/phases/01-deps-foundation-bump/01-LEARNINGS.md` — Phase 1 outcomes including the AGP-9 deferral that reframed Phase 2
- **Codebase grep (HIGH — verified in this session):** PlayerScreen.kt:187 (BackHandler), MoreSheet existence at BottomNav.kt:167, ModalBottomSheet 3 sites, themes.xml current state, 7-screen Scaffold inventory, English-only string resources

### Secondary (MEDIUM confidence)

- [developer.android.com — Predictive back in Compose (setup)](https://developer.android.com/develop/ui/compose/system/predictive-back) — Compose Navigation transition snippets (not directly relevant but cross-confirms manifest flag requirement)
- [developer.android.com — Use Material 3 insets](https://developer.android.com/develop/ui/compose/system/material-insets) — confirms Scaffold-provides-PaddingValues pattern; does not quote exact `ScaffoldDefaults.contentWindowInsets` default (assumed to be `WindowInsets.systemBars` per A3)
- [WebSearch — "ModalBottomSheet contentWindowInsets parameter material3 1.7 signature default"](https://composables.com/jetpack-compose/androidx.compose.material3/material3/components/ModalBottomSheet/api) — confirms `BottomSheetDefaults.modalWindowInsets` is the current default (as of M3 1.7+); earlier versions used `BottomSheetDefaults.windowInsets`
- [Issue Tracker — generateLocaleConfig in agp 8.1.0 uses non-deterministic order](https://issuetracker.google.com/issues/281825213) — known minor issue with ordering; doesn't block Phase 2's "single-locale" case

### Tertiary (LOW confidence — flagged in Assumptions)

- Splash adaptive-icon cropping advice (Pitfall E5) — drawn from multiple practitioner posts; mitigated by Claude's Discretion to fall back to `@drawable/splash_icon` if needed
- ANR risk magnitude for Pattern A (Pitfall E3) — inferred from main-thread blocking semantics; mitigated by safety-timeout option

## Metadata

**Confidence breakdown:**
- Standard Stack: **HIGH** — all versions verified from canonical Google sources + libs.versions.toml grep
- Architecture Patterns: **HIGH** — code examples drawn from canonical migration docs; codebase grep confirms file/line targets
- Pitfalls: **HIGH** — directly cross-referenced with existing PITFALLS.md research; Slopper-specific additions (Pitfall E1, E3, E4) verified against codebase state
- Validation Architecture: **HIGH** — grep commands tested against actual repo; manual UAT framing matches Phase 1 precedent
- Security Domain: **MEDIUM** — ASVS mapping is correct but Phase 2's security surface is narrow; not deeply contested

**Research date:** 2026-05-17
**Valid until:** 2026-06-17 (30 days — toolchain floor is stable; the only fast-moving item is `core-splashscreen` 1.x line and `PredictiveBackHandler` deprecation timeline, both of which are tracked as backlog items)

---

## RESEARCH COMPLETE
