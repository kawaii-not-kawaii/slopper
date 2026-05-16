# Feature Research — Modern Android Platform Capabilities

**Domain:** Modern Compose-based Android app (modernization pass on Slopper)
**Researched:** 2026-05-16
**Confidence:** HIGH (Android guidelines cross-checked against current `developer.android.com` references and against Slopper's manifest/`MainActivity.kt` source)

> **Reframing:** This is a brownfield modernization. The "features" researched here are **platform-mandated and platform-expected capabilities** a 2026 Compose app must expose — *not* new end-user features (those are out of scope per `PROJECT.md`). Each item is cross-referenced with Slopper's current state (`/home/yun/slopper/app/src/main/AndroidManifest.xml`, `MainActivity.kt`, `core/designsystem/theme/Theme.kt`).
>
> Slopper context: `compileSdk = 35`, `targetSdk = 35`, `minSdk = 26` (`build-logic/convention/.../KotlinAndroid.kt:21,24`). Single Activity + Compose NavHost. ExoPlayer/Media3 in `:feature:player`. Foreground-service permissions declared but no `Service` class found in source.

## Feature Landscape

### Table Stakes (Platform-Mandated or Strongly Expected)

These are non-negotiable for a 2026 Compose app. Several are enforced by the system itself once `targetSdk` lands on 35/36.

| Capability | Why Mandatory / Expected | Complexity | Slopper Status | Notes |
|---|---|---|---|---|
| **Edge-to-edge + window insets** | Enforced on `targetSdk ≥ 35` — the system forces edge-to-edge regardless of opt-out. Apps must consume `WindowInsets` correctly or content draws under status/nav bars. ([Edge-to-edge guide](https://developer.android.com/develop/ui/compose/layouts/insets)) | MEDIUM | **PARTIAL** — `enableEdgeToEdge()` called (`MainActivity.kt:87`), `:feature:player` uses `WindowInsets.systemBars` correctly. BUT `themes.xml` hardcodes `statusBarColor`/`navigationBarColor` to black and `windowLightStatusBar=false` — fights the edge-to-edge contract. Top-level `Scaffold` only forwards `bottom` padding (`MainActivity.kt:213`); each screen must own its top inset (per the comment), which is fragile across feature modules. | Audit every `feature/*Screen.kt` for `Modifier.windowInsetsPadding(WindowInsets.systemBars)` or a `Scaffold` that handles top insets. Remove the deprecated `statusBarColor`/`navigationBarColor` from `themes.xml` (no-op on SDK 35+ and creates confusion). |
| **Predictive back gesture** | Opt-in on SDK 33, opt-in default on 34, **required on `targetSdk ≥ 36`** — the framework drops the legacy `OnBackPressed` path. ([Predictive back guide](https://developer.android.com/guide/navigation/custom-back/predictive-back-gesture)) | MEDIUM | **PARTIAL** — manifest does **not** declare `android:enableOnBackInvokedCallback="true"` on `<application>`. Compose's `BackHandler` (used in `feature/player/PlayerScreen.kt:202`) only participates in predictive back when the manifest flag is on. NavHost predictive-back animations only work with `androidx.navigation.compose ≥ 2.8`. | Add manifest flag. Verify nav lib version supports predictive-back transitions. Audit every `BackHandler` for state-restore correctness (predictive back will preview the previous screen before commit). |
| **Foreground-service type** | `targetSdk ≥ 34` requires every foreground service to declare a **type** in the manifest *and* call `startForeground(id, notif, FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)`. Failing to do so throws `MissingForegroundServiceTypeException`. ([FGS types guide](https://developer.android.com/about/versions/14/changes/fgs-types-required)) | HIGH | **MISSING / RISKY** — `AndroidManifest.xml` declares `FOREGROUND_SERVICE_MEDIA_PLAYBACK` permission **but no `<service>` element**. There is no `MediaSessionService` in `:feature:player`. Media3 best-practice in 2026 is a `MediaLibraryService` so playback survives nav exit and integrates with Wear/Auto/Cast. Today, playback stops when `PlayerScreen` leaves composition. | Add a `MediaSessionService` (or `MediaLibraryService`) in `:feature:player`, declare it with `android:foregroundServiceType="mediaPlayback"`, host an `ExoPlayer` + `MediaSession` there, and have `PlayerScreen` bind via `MediaController`. This is the single biggest modernization win listed here. |
| **POST_NOTIFICATIONS permission flow** | Runtime permission required on `targetSdk ≥ 33`. Without it, a foreground-service notification can be shown only with a system-enforced 10-second delay grace period. ([POST_NOTIFICATIONS guide](https://developer.android.com/develop/ui/views/notifications/notification-permission)) | LOW | **PARTIAL** — permission declared in manifest. **No runtime request** in code; no `ActivityResultContracts.RequestPermission` usage found. | Add a one-shot `rememberLauncherForActivityResult(RequestPermission)` triggered when the user first enters `PlayerScreen` (or right before `startForeground`). Don't block app launch on it. |
| **Notification channels** | Required since Android 8 (SDK 26 = Slopper's `minSdk`). Media playback notifications must use a channel with `IMPORTANCE_LOW` (Media3 handles this when using `MediaSessionService`, but the channel ID/name must still be supplied). | LOW | **N/A until service exists** — depends on the service work above. | When adding `MediaSessionService`, register a single `playback` channel in `StashApp.onCreate()`. |
| **Splash Screen API (`androidx.core.splashscreen`)** | Required on SDK 31+ to render the system splash correctly; the legacy "blank Activity theme" pattern is broken on 12+ (you get a double splash). ([SplashScreen guide](https://developer.android.com/develop/ui/views/launch/splash-screen/migrate)) | LOW | **MISSING** — no `androidx.core:core-splashscreen` dependency in `gradle/libs.versions.toml`, no `installSplashScreen()` call in `MainActivity`, no `Theme.Stash.SplashScreen` theme. App uses base `Theme.Material.NoActionBar` so the launcher shows a blank black window until Compose paints. | Add `androidx.core:core-splashscreen`, create `Theme.Stash.SplashScreen` with `windowSplashScreenBackground` + `windowSplashScreenAnimatedIcon`, call `installSplashScreen()` before `super.onCreate()`. Keep it on screen until `RootViewModel.start` resolves (currently shows a CircularProgressIndicator placeholder — that's exactly the splash-keep-on-screen condition). |
| **Compose performance hygiene** | Strong skipping mode is the default in Compose Compiler 1.5.4+ / Kotlin 2.0+. With Kotlin 2.0 Compose Compiler plugin, stability inference is automatic for most types. Baseline profile coverage is the table-stakes startup optimization. ([Compose perf guide](https://developer.android.com/jetpack/compose/performance)) | MEDIUM | **PARTIAL** — `:baselineprofile` module already wired and `androidx.profileinstaller` is in `app/build.gradle.kts:141`. Strong skipping likely active (depends on Kotlin/Compose versions in catalog — verify). No `@Stable`/`@Immutable` annotations spotted on UiState data classes, which is fine under strong skipping IF all params are stable. | Audit ViewModels' `UiState` classes for unstable types (e.g. `List<T>` should be `ImmutableList<T>` from `kotlinx.collections.immutable`, or strong skipping handles it — verify with Compose Compiler reports). Run `./gradlew :app:generateBaselineProfile` and confirm the generated profile covers the cold-launch path + scrolling Library/Home. |
| **Configuration changes / orientation / size classes** | Apps that handle `android:configChanges` themselves must use Compose `WindowSizeClass` and `LocalConfiguration` instead of activity recreation. Required to behave on foldables, tablets, and ChromeOS. ([Adaptive layouts guide](https://developer.android.com/develop/ui/compose/layouts/adaptive)) | MEDIUM | **PARTIAL** — `MainActivity` declares `android:configChanges="orientation|screenSize|keyboardHidden|screenLayout|smallestScreenSize"` — the Activity handles config changes itself, so layout adaptation **must** be done in Compose. No `WindowSizeClass` / `currentWindowAdaptiveInfo` usage found. App likely renders phone-shaped on tablets. | Add `androidx.compose.material3.adaptive:adaptive-navigation` and switch top-level bottom nav → `NavigationSuiteScaffold` so it auto-becomes a rail/drawer on width >= 600dp. Library/Browse grids should switch column counts off `WindowSizeClass`. |
| **Accessibility: content descriptions, TalkBack labels, large fonts, contrast** | Play Store policy + WCAG. `contentDescription` on every clickable/image; never hardcode `Modifier.size` for typography (use `MaterialTheme.typography`); honor `LocalDensity` for scaling. ([A11y in Compose](https://developer.android.com/develop/ui/compose/accessibility)) | MEDIUM | **UNKNOWN — likely PARTIAL** — Coil `AsyncImage` in `SceneCard` etc. needs explicit `contentDescription`. Bottom-nav icons in `MainBottomBar` need `contentDescription` per item. No `Modifier.semantics` usages spotted in the architecture map. | Mechanical sweep: every `Icon`, `IconButton`, `AsyncImage` gets a `contentDescription` (use `null` only when decorative + already announced via parent). Add `:designsystem` lint rule or detekt custom rule. Test with TalkBack + 200% font scale + high-contrast text on a Pixel. |
| **Adaptive launcher icon with monochrome layer (themed icon)** | Required for Material You themed icons on SDK 33+. Without `<monochrome>`, the user-themed icon falls back to a generic placeholder. | LOW | **PRESENT** — `mipmap-anydpi-v26/ic_launcher.xml` declares `<monochrome android:drawable="@drawable/ic_launcher_foreground" />`. Reusing the foreground drawable is suboptimal (monochrome should be flat single-color), but the contract is satisfied. | Optional polish: ship a proper flat single-color `ic_launcher_monochrome.xml`. |

### Differentiators (Modernization Wins Users Feel)

These aren't enforced by the framework but are *strongly expected* in 2026. Implementing them upgrades the app from "works" to "modern Android citizen".

| Capability | Value Proposition | Complexity | Slopper Status | Notes |
|---|---|---|---|---|
| **Material You dynamic color** | App reskins to match the user's wallpaper on Android 12+. Single most visible "modern Android" cue. ([Dynamic color guide](https://developer.android.com/develop/ui/views/theming/dynamic-colors)) | LOW | **MISSING** — no `dynamicLightColorScheme` / `dynamicDarkColorScheme` references in `:core:designsystem`. Theme is a static `lightColorScheme` / `darkColorScheme`. | In `StashTheme`, branch on `Build.VERSION.SDK_INT >= 31` and use `dynamicDarkColorScheme(LocalContext.current)`. Add a "Use system color" toggle in settings (uses existing `UiPreferences`). Player UI should keep its dark high-contrast palette regardless. |
| **Per-app language preferences (`LocaleManager`)** | System Settings → App language picker appears automatically once `app_locales` is declared, with zero code. ([Per-app language](https://developer.android.com/guide/topics/resources/app-languages)) | LOW | **MISSING** — no `<locale-config>` XML, no `appLocaleConfig` in manifest, no `androidResources { generateLocaleConfig = true }` in `app/build.gradle.kts`. App ships English-only today, but the infrastructure is one-time setup. | Add `androidResources.generateLocaleConfig = true` (AGP 8.0+). Even with one locale, this unlocks the System Settings entry once translations land. |
| **`MediaController` / `MediaLibraryService` integration with Wear, Auto, Cast** | Once a `MediaSessionService` exists (table-stakes above), Wear OS playback controls, Auto/Automotive, system media controls, and Cast become near-free. `media3-cast` is already in the dep catalog but unused (`INTEGRATIONS.md`). | MEDIUM | **MISSING** — see foreground-service item. Cast dep present but unwired. | Same work item as the service modernization. Cast wiring is a single `CastPlayer` swap once `MediaSession` exists. |
| **In-app language / locale runtime switching** | `AppCompatDelegate.setApplicationLocales()` lets the user pick a language without restarting the device-level Settings flow. | LOW | **MISSING** — depends on per-app-language infra above. | Cheap to add once translations exist. Defer until v2. |
| **Predictive-back custom animations on detail/sheet screens** | Predictive back previews the destination during the gesture. With `androidx.activity:activity-compose:1.10+` `PredictiveBackHandler`, you can drive a custom progress animation (e.g. dismiss the FilterSheet with finger-tracking). ([PredictiveBackHandler](https://developer.android.com/reference/kotlin/androidx/activity/compose/package-summary#PredictiveBackHandler())) | MEDIUM | **MISSING** — `BackHandler` used in player; no `PredictiveBackHandler`. | Use `PredictiveBackHandler` for `FilterSheet`, `MoreSheet`, `NavCustomizeSheet` to give them physical-feeling dismissals. |
| **Stable Compose UiState (`@Immutable` / `ImmutableList`)** | Beyond strong-skipping table-stakes — explicit immutability annotations make Compose Compiler reports clean and avoid unnecessary recompositions for Hilt-injected ViewModels. | LOW | **UNKNOWN** — no spot check available; need Compose Compiler reports. | Wire `composeCompiler { reportsDestination = layout.buildDirectory.dir("compose_reports") }` in the compose convention plugin; iterate on flagged classes. |
| **Privacy-aware logging in release** | Already done — `HttpLoggingInterceptor` gated on `FLAG_DEBUGGABLE` (`NetworkModule.kt:35`). | LOW | **PRESENT** | No action; mention only because it's a guideline-aligned positive. |

### Anti-Features (Things Modernization Should NOT Do)

These are commonly suggested in modernization passes but would either contradict `PROJECT.md` constraints or create more harm than good for Slopper specifically.

| Anti-Feature | Why Tempting | Why Problematic | Do Instead |
|---|---|---|---|
| **Migrate any screen back to the View system / XML layouts** | Compose Navigation type-safety still maturing; some libraries (e.g. ExoPlayer's `PlayerView`) are View-based. | Violates `PROJECT.md` ("Migrating off Compose … keep the existing architecture"). Loses theme + insets consistency. | Wrap View-based components (`PlayerView`, `WebView` if ever needed) inside `AndroidView` composables. Already the pattern in `feature/player`. |
| **Bump `minSdk` to drop SDK 26–30 code paths** | Lots of `if (SDK_INT >= …)` would vanish; less testing matrix. | Violates `PROJECT.md` ("`minSdk` bump — must remain installable on existing devices"). | Use `androidx.core` compat helpers (`SplashScreenCompat`, `NotificationManagerCompat`, `LocaleListCompat`) — they already hide the version forks. |
| **Adopt Jetpack Glance / App Widgets** | Widgets are a "modern Android" buzz feature. | New user-facing feature; out of scope per `PROJECT.md`. | Skip. Revisit only if a *separate* widgets milestone is requested. |
| **Switch persistence to Room "because Room is modern"** | Room is in the version catalog but unused (`INTEGRATIONS.md`). | Existing storage (Apollo SQLite cache + DataStore + EncryptedSharedPrefs) is fit for purpose. Introducing Room buys nothing for current features and is a new third-party SDK direction (technically already declared, but unused). | Leave the Room catalog entries as scaffolding. If they truly are dead, **remove them** in the dependency-refresh phase to shrink surface area. |
| **Add Sentry / Crashlytics / Firebase** | "Real apps have crash reporting." | Violates `PROJECT.md` ("No new third-party SDKs"). User runs a homelab — privacy preference is implicit. | Stay with `android.util.Log` + ANR/crash diagnostics via `adb` and the on-device dropbox. |
| **Migrate to typed Navigation 2.8 `Serializable` routes mid-modernization** | Replaces stringly-typed routes with KSerializer-driven ones. | Touches every feature module, every nav action — a refactor of this size belongs in its own milestone with its own tests. The existing centralized `Routes` registry already neutralizes the worst of stringly-typed nav. | Defer. If undertaken, do it as a dedicated phase, not as part of a "guidelines sweep". |
| **Force-dynamic-color on the player** | Internal consistency. | Photo/video playback UIs need predictable dark high-contrast chrome regardless of wallpaper tint. | Gate `dynamicColor` to non-player routes (or off-by-default with opt-in). |
| **Move all `feature/*` modules to a single `:features` module to "simplify"** | Faster initial build, fewer Gradle modules. | Violates `PROJECT.md` ("Preserve the existing module layout"). Existing modularization is the *win* — preserve it. | Refactor inside modules only. |
| **Add Hilt scopes beyond `SingletonComponent`** | "Architectural rigor." | Current `SingletonComponent`-only graph is intentional (per `ARCHITECTURE.md`). No documented pain point. | Don't fix what isn't broken. Out of scope. |

## Capability Dependencies

```
Splash Screen API
    └──unblocks──> Cold-start UX polish (perceived perf win without a real perf change)

Edge-to-edge audit
    └──must precede──> Theme cleanup (removing statusBarColor/navigationBarColor)
                            └──unblocks──> Material You dynamic color (no hardcoded bar colors fighting tonal palette)

Predictive back manifest flag
    └──unblocks──> PredictiveBackHandler usage on sheets
                        └──enhances──> FilterSheet / MoreSheet / NavCustomizeSheet UX

POST_NOTIFICATIONS runtime request
    └──must precede──> MediaSessionService.startForeground()
                            └──unblocks──> Background playback survives nav exit
                                                └──unblocks──> Media3 Cast wiring (dep already present)
                                                └──unblocks──> Wear OS / Auto controls (free once session exists)

WindowSizeClass adoption
    └──unblocks──> NavigationSuiteScaffold (rail/drawer on tablets/foldables)

generateLocaleConfig = true
    └──unblocks──> System Settings per-app language picker
                        └──unblocks──> AppCompatDelegate.setApplicationLocales() (in-app picker)

Strong skipping verified (Compose Compiler reports)
    └──unblocks──> Stable UiState audit
                        └──enhances──> Baseline profile effectiveness (less recomposition under benchmark)
```

### Dependency Notes

- **POST_NOTIFICATIONS → MediaSessionService:** the service can technically `startForeground` without the permission, but the notification is silently suppressed for ~10s, which breaks the "tap-play-leave-app, still hear audio" flow. Always request first.
- **Edge-to-edge cleanup → dynamic color:** hardcoded `statusBarColor=@android:color/black` in `themes.xml` will override Material You's tonal `surfaceContainer` color on the status bar. Must be removed before dynamic color looks right.
- **`android:configChanges` → WindowSizeClass:** because Slopper opts out of recreate-on-rotate, every layout adaptation **must** go through Compose's reactive `LocalConfiguration` / `currentWindowAdaptiveInfo`. Without WindowSizeClass adoption, the app silently looks bad on tablets/foldables — there's no recreation to "save" it.

## MVP Definition (Modernization Phases)

### Phase A — Platform Compliance (Must Land Before Any "Optional" Polish)

These items resolve framework-enforced contracts; without them, `targetSdk` 35→36 bump is unsafe.

- [ ] **Edge-to-edge audit + theme cleanup** — remove deprecated `statusBarColor`/`navigationBarColor`/`windowLightStatusBar` from `themes.xml`; ensure every top-level Compose screen consumes `WindowInsets.systemBars` correctly.
- [ ] **Predictive back enablement** — add `android:enableOnBackInvokedCallback="true"` to `<application>`; verify nav-compose version supports predictive transitions; audit all `BackHandler` usages.
- [ ] **MediaSessionService + foreground-service type** — actually implement the service that the permissions already promise. Declare `android:foregroundServiceType="mediaPlayback"`. Call `startForeground` with the typed flag.
- [ ] **POST_NOTIFICATIONS runtime request** — request before `MediaSessionService.startForeground()` is first invoked.
- [ ] **Splash Screen API migration** — `installSplashScreen()` + `keepOnScreenCondition { rootViewModel.start.value == null }` replaces the current `CircularProgressIndicator` placeholder.

### Phase B — Modern Android Citizen (User-Visible Polish)

- [ ] **Material You dynamic color** with settings toggle (defaults on for non-player routes).
- [ ] **WindowSizeClass + NavigationSuiteScaffold** for tablet/foldable navigation chrome.
- [ ] **Per-app language: `generateLocaleConfig = true`** even with EN-only (free Settings entry).
- [ ] **PredictiveBackHandler** on `FilterSheet`, `MoreSheet`, `NavCustomizeSheet`.
- [ ] **Accessibility sweep** — content descriptions on every `Icon`/`IconButton`/`AsyncImage`; TalkBack pass; 200% font scale pass.

### Phase C — Performance Verification (Measurement, Not New Capability)

- [ ] **Compose Compiler reports** wired into the convention plugin; stable-UiState audit.
- [ ] **Baseline profile regeneration** covering cold start + Home rails scroll + Library paging scroll + entering Player.
- [ ] **Macrobenchmark deltas** captured per `PROJECT.md`'s perf-claim requirement.

### Defer (Out of Scope for This Modernization)

- Cast wiring (`media3-cast` already declared) — only worth it if the user actually uses Cast.
- In-app language picker — wait for translations.
- App Widgets / Glance — new feature surface.
- Wear OS / Android Auto — new platform.

## Capability Prioritization Matrix

| Capability | Compliance Risk if Missing | Implementation Cost | Priority |
|---|---|---|---|
| Edge-to-edge correctness | HIGH (forced on SDK 35) | MEDIUM | P1 |
| Predictive back manifest flag | HIGH (forced on SDK 36) | LOW | P1 |
| MediaSessionService with FGS type | HIGH (current state is misleading — perms without service) | HIGH | P1 |
| POST_NOTIFICATIONS runtime flow | MEDIUM (silent 10s suppression today) | LOW | P1 |
| Splash Screen API | LOW (works without, but looks dated) | LOW | P1 |
| Dynamic color (Material You) | NONE (pure polish) | LOW | P2 |
| WindowSizeClass / adaptive nav | LOW (tablets look phone-shaped) | MEDIUM | P2 |
| Per-app language config | NONE (until i18n exists) | LOW | P2 |
| PredictiveBackHandler on sheets | NONE (BackHandler still works) | MEDIUM | P3 |
| Stable UiState audit | NONE (perf only) | MEDIUM | P2 |
| Baseline profile regen | NONE (already wired, just stale) | LOW | P1 |
| Accessibility sweep | MEDIUM (Play Store policy if ever published; user empathy regardless) | MEDIUM | P1 |

**Priority key:**
- **P1** — Phase A; mandatory for "aligned with current Android guidelines" claim in `PROJECT.md`.
- **P2** — Phase B; visible modernization wins.
- **P3** — Phase B/C polish; valuable but not blocking.

## Slopper-Specific Cross-Reference

Single source-of-truth table for the planner — every capability with its evidence pin.

| Capability | Evidence file | Line | Conclusion |
|---|---|---|---|
| Edge-to-edge call | `app/src/main/java/io/stashapp/android/MainActivity.kt` | 87 | Called, but theme overrides bar colors |
| Hardcoded system bar colors | `app/src/main/res/values/themes.xml` | 5–9 | Must remove for SDK 35+ correctness |
| Predictive back manifest flag | `app/src/main/AndroidManifest.xml` | — | **Missing** |
| Compose `BackHandler` usages | `feature/player/.../PlayerScreen.kt` | 202 | Works; not predictive-aware |
| Foreground service perms | `app/src/main/AndroidManifest.xml` | 13–15 | Declared |
| `<service>` declaration | (anywhere) | — | **Missing** — perms without a service is misleading |
| `targetSdk` | `build-logic/.../AndroidApplicationConventionPlugin.kt` | 17 | 35 — predictive-back not yet forced, FGS-type already enforced |
| `minSdk` | `build-logic/.../KotlinAndroid.kt` | 24 | 26 — splash/notification-channels/locale-config all supported |
| Monochrome icon | `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` | 5 | Present (reuses foreground; OK) |
| Dynamic color | `core/designsystem/.../theme/Theme.kt` | — | **Missing** — no `dynamic*ColorScheme` |
| Splash Screen API | `gradle/libs.versions.toml`, `MainActivity.onCreate` | — | **Missing** — no `core-splashscreen` dep, no `installSplashScreen()` |
| Per-app language config | `app/build.gradle.kts` | — | **Missing** — no `generateLocaleConfig` |
| WindowSizeClass / adaptive | (anywhere) | — | **Missing** — no `material3-adaptive*` deps |
| Baseline profile | `app/build.gradle.kts` | 141, 144 | Wired; needs regen post-changes |
| `android:configChanges` opt-out | `app/src/main/AndroidManifest.xml` | 32 | Activity owns config changes — forces Compose-driven adaptation |

## Sources

- [Edge-to-edge enforcement on Android 15](https://developer.android.com/develop/ui/compose/layouts/insets) — `developer.android.com`
- [Predictive back gesture guide](https://developer.android.com/guide/navigation/custom-back/predictive-back-gesture) — `developer.android.com`
- [Foreground service types required on Android 14+](https://developer.android.com/about/versions/14/changes/fgs-types-required) — `developer.android.com`
- [POST_NOTIFICATIONS runtime permission](https://developer.android.com/develop/ui/views/notifications/notification-permission) — `developer.android.com`
- [SplashScreen API migration](https://developer.android.com/develop/ui/views/launch/splash-screen/migrate) — `developer.android.com`
- [Per-app language preferences](https://developer.android.com/guide/topics/resources/app-languages) — `developer.android.com`
- [Themed app icons / monochrome layer](https://developer.android.com/develop/ui/views/launch/icon_design_adaptive) — `developer.android.com`
- [Material You dynamic color](https://developer.android.com/develop/ui/views/theming/dynamic-colors) — `developer.android.com`
- [Compose performance + strong skipping](https://developer.android.com/jetpack/compose/performance/stability) — `developer.android.com`
- [Baseline profiles](https://developer.android.com/topic/performance/baselineprofiles/overview) — `developer.android.com`
- [Adaptive layouts (WindowSizeClass)](https://developer.android.com/develop/ui/compose/layouts/adaptive) — `developer.android.com`
- [Accessibility in Compose](https://developer.android.com/develop/ui/compose/accessibility) — `developer.android.com`
- [Media3 MediaSessionService](https://developer.android.com/media/media3/session/background-playback) — `developer.android.com`
- Slopper codebase: `/home/yun/slopper/app/src/main/AndroidManifest.xml`, `/home/yun/slopper/app/src/main/java/io/stashapp/android/MainActivity.kt`, `/home/yun/slopper/app/src/main/res/values/themes.xml`, `/home/yun/slopper/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`, `/home/yun/slopper/build-logic/convention/src/main/kotlin/io/stashapp/android/buildlogic/KotlinAndroid.kt`, `/home/yun/slopper/.planning/codebase/ARCHITECTURE.md`, `/home/yun/slopper/.planning/codebase/INTEGRATIONS.md`

---
*Feature research for: modern Compose Android app — Slopper modernization*
*Researched: 2026-05-16*
