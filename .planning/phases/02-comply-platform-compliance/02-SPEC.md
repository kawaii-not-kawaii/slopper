# Phase 2: COMPLY — Platform Compliance — Specification

**Created:** 2026-05-17
**Ambiguity score:** 0.2015 (gate: ≤ 0.20 — boundary; all 4 dimensions cleared minimums)
**Requirements:** 7 locked

## Goal

Land the platform-mandated UI and manifest contracts (edge-to-edge, predictive back, splash API, per-app locales, orphan-permission cleanup) on the current toolchain floor (AGP 8.7.3 / compileSdk 35 / Activity Compose 1.9.3) — proactively, before `targetSdk = 36` enforcement returns in the future AGP-9 phase — without changing user-visible app behavior.

## Background

**Phase 1 outcome that reframes Phase 2:** Phase 1 (DEPS) deferred AGP 9 / compileSdk 36 / Activity Compose 1.13.0 / Lifecycle 2.10.0 (→ `DEPS-17`) because no published Hilt release supports AGP 9 yet. The original ROADMAP scoped Phase 2 against those bumped versions; this SPEC reframes Phase 2 on the actual current floor. All COMPLY APIs needed (`enableEdgeToEdge`, `PredictiveBackHandler`, `installSplashScreen`, `generateLocaleConfig`, `POST_NOTIFICATIONS`, `ACTION_APP_LOCALE_SETTINGS`) exist at the current versions — only the target-SDK *enforcement* differs.

**Current code state (scouted 2026-05-17):**
- `MainActivity.kt:87` — `enableEdgeToEdge()` already called ✓
- `app/src/main/res/values/themes.xml` — hardcodes `statusBarColor=@android:color/black` + `navigationBarColor=@android:color/black` (fights edge-to-edge)
- `AndroidManifest.xml:13` — orphan `FOREGROUND_SERVICE_MEDIA_PLAYBACK` permission (no `MediaSessionService` exists)
- `AndroidManifest.xml:15` — orphan `POST_NOTIFICATIONS` permission (no notification code exists anywhere in repo)
- `AndroidManifest.xml` — lacks `android:enableOnBackInvokedCallback="true"`
- `PlayerScreen.kt:8,187` — single `BackHandler { onExit() }` call site, unconditional; only `BackHandler` in entire repo
- `FilterSheet.kt` + `NavCustomizeSheet.kt` — only real bottom sheets; both use Material3 `ModalBottomSheet` (handles back natively); `MoreSheet` does NOT exist (roadmap text was speculative)
- `app/build.gradle.kts` — no `generateLocaleConfig = true`
- `app/src/main/res/xml/` — no `locale_config.xml`
- No `androidx.core:core-splashscreen` dependency, no `installSplashScreen()` call
- `feature/settings/.../SettingsScreen.kt` (412 lines) — zero `language`/`locale` mentions (locale picker is new UI)
- `RootViewModel.start: StateFlow<String?>` (MainActivity.kt:67–76) — natural splash gate (null until ConnectionRepository resolves)
- `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MEDIA_PLAYBACK` declared but no service exists; only the `_MEDIA_PLAYBACK` subset is the orphan target per COMPLY-05 (the base `FOREGROUND_SERVICE` declaration is harmless and stays for future use)

**Trigger:** `targetSdk = 36` enforcement (deferred via DEPS-17) will compel these changes later; landing them now means the AGP-9 phase only touches build infrastructure, not UI plumbing.

## Requirements

1. **COMPLY-01 — Edge-to-edge correctness**: Status/nav bar colors removed from theme, Scaffolds inset-aware, screenshot evidence committed.
   - **Current:** `enableEdgeToEdge()` is called but `themes.xml` overrides both `statusBarColor` and `navigationBarColor` to black, defeating system-bar tinting; per-screen Scaffold inset handling is inconsistent.
   - **Target:** (a) Hardcoded system-bar colors removed from `themes.xml`; (b) every top-level `Scaffold` uses `WindowInsets.systemBars` (or `contentWindowInsets = WindowInsets(0)` with explicit content padding); (c) PlayerScreen overlays its own translucent scrim when player controls are visible so status-bar text remains legible.
   - **Acceptance:**
     - `grep -E 'statusBarColor|navigationBarColor' app/src/main/res/values/themes.xml` → returns empty.
     - For each top-level `Scaffold` in `app/`, `feature/library/`, `feature/player/`, `feature/settings/`, `feature/connection/`, `feature/home/` (or equivalent), the `Scaffold` declaration explicitly references `WindowInsets` (audit by grep).
     - Screenshot pack committed under `.planning/phases/02-comply-platform-compliance/screenshots/` covering Home / Library / PlayerScreen (controls-visible + controls-hidden) / Settings / Connection in both light and dark mode on Galaxy S23+ Android 16 — minimum 10 PNGs.
     - Manual reviewer sign-off recorded in `02-UAT.md`: no clipped chrome, no unreadable status-bar text.

2. **COMPLY-02 — Predictive back**: Manifest flag enabled; `PlayerScreen` migrates from `BackHandler` to `PredictiveBackHandler`.
   - **Current:** Manifest has no `android:enableOnBackInvokedCallback`. `PlayerScreen.kt:187` uses `BackHandler { onExit() }` (snap-exit, no animation). Material3 `ModalBottomSheet` instances in `FilterSheet` / `NavCustomizeSheet` already use the system back path natively once the manifest flag is set — no per-sheet code change needed.
   - **Target:** (a) `android:enableOnBackInvokedCallback="true"` in `<application>` element of `AndroidManifest.xml`; (b) `PlayerScreen.kt:187` `BackHandler { onExit() }` → `PredictiveBackHandler { progress -> progress.collect { … }; onExit() }` so the back gesture renders an animated preview before `onExit()` fires.
   - **Acceptance:**
     - `grep -c 'android:enableOnBackInvokedCallback="true"' app/src/main/AndroidManifest.xml` → `1`
     - `grep -rn "BackHandler\b" --include="*.kt" feature/ app/ core/` → returns at most the `import` line in `PlayerScreen.kt` (no remaining `BackHandler { … }` *call*); `PredictiveBackHandler` reference exists in `PlayerScreen.kt`.
     - Manual UAT on S23+ Android 16: back swipe on PlayerScreen shows animated preview; FilterSheet + NavCustomizeSheet dismiss with system preview; callback fires exactly once.

3. **COMPLY-03 — Orphan POST_NOTIFICATIONS removal**: Mirror COMPLY-05 — remove the unused permission.
   - **Current:** `<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />` declared at `AndroidManifest.xml:15` with zero `NotificationManager` / `NotificationCompat` / `notify(` call sites in the repo.
   - **Target:** Permission line removed. When BG-MEDIA milestone lands and adds notification-using features, that milestone re-declares the permission with a proper `ActivityResultContracts.RequestPermission` runtime request flow.
   - **Acceptance:** `grep -c 'android.permission.POST_NOTIFICATIONS' app/src/main/AndroidManifest.xml` → `0`. Build + UAT show no notification regression (none exist to regress).

4. **COMPLY-04 — Splash Screen API migration**: Adopt `androidx.core:core-splashscreen`; gate on `RootViewModel.start`.
   - **Current:** No `core-splashscreen` dependency; `themes.xml` has no splash theme; `MainActivity.onCreate` jumps directly to `setContent` after `super.onCreate`. On cold launch, users see a white flash before `Theme.Stash`'s black `windowBackground` paints, then another transition once Compose tree mounts.
   - **Target:** (a) Add `androidx.core:core-splashscreen` dependency (catalog entry + `app/build.gradle.kts`); (b) Create `Theme.Stash.Splash` parent style in `themes.xml` extending `Theme.SplashScreen` with `postSplashScreenTheme = @style/Theme.Stash`; (c) Set `<activity android:theme="@style/Theme.Stash.Splash">` in manifest; (d) `MainActivity.onCreate` calls `val splashScreen = installSplashScreen(); splashScreen.setKeepOnScreenCondition { rootViewModel.start.value == null }` *before* `super.onCreate`.
   - **Acceptance:**
     - Dependency `androidx.core:core-splashscreen` present in `gradle/libs.versions.toml` and consumed by `app/build.gradle.kts`.
     - `grep -c installSplashScreen app/src/main/java/io/stashapp/android/MainActivity.kt` → `1`
     - `grep -c setKeepOnScreenCondition app/src/main/java/io/stashapp/android/MainActivity.kt` → `1`
     - Manual UAT on S23+: cold launch shows splash (icon centered, black background) until first composition; no white flash; splash dismisses when `Routes.Home` or `Routes.Connection` resolves.

5. **COMPLY-05 — Orphan FGS_MEDIA_PLAYBACK removal**: Remove the unused permission.
   - **Current:** `<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />` at `AndroidManifest.xml:13`. No `MediaSessionService` / `MediaBrowserService` / `Service` subclass exists. Real background playback is a BG-MEDIA-01 backlog item.
   - **Target:** Permission line removed. (Base `FOREGROUND_SERVICE` line stays — it's harmless and will be re-used by BG-MEDIA.)
   - **Acceptance:** `grep -c FOREGROUND_SERVICE_MEDIA_PLAYBACK app/src/main/AndroidManifest.xml` → `0`. `grep -c '"android.permission.FOREGROUND_SERVICE"' app/src/main/AndroidManifest.xml` → `1` (base FGS preserved). Build green.

6. **COMPLY-06 — Per-app language picker (new Settings entry)**: Enable system locale picker dialog from a new entry in `SettingsScreen`.
   - **Current:** No `generateLocaleConfig` flag; no `locale_config.xml`; `SettingsScreen.kt` (412 lines) has zero locale/language UI.
   - **Target:** (a) `app/build.gradle.kts` `androidResources { generateLocaleConfig = true }`; (b) `locale_config.xml` auto-generated under `app/build/generated/.../locale_config.xml` (no manual file needed); (c) new `LanguageRow` composable added to `SettingsScreen.kt` that, on tap, fires `context.startActivity(Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply { data = Uri.parse("package:${context.packageName}") })`; (d) the row is gated by `Build.VERSION.SDK_INT >= 33` and hidden below (per-app locale dialog is API 33+).
   - **Acceptance:**
     - `grep -c "generateLocaleConfig = true" app/build.gradle.kts` → `1`
     - Build produces `app/build/generated/res/resValues/.../locale_config.xml` referenced by merged manifest.
     - `grep -c "ACTION_APP_LOCALE_SETTINGS" feature/settings/src/main/java/io/stashapp/android/feature/settings/SettingsScreen.kt` → `1`
     - Manual UAT on S23+: Settings → Language row visible → tap opens system per-app language dialog showing app's supported locales.
   - **Open in discuss-phase:** Exact placement of the `LanguageRow` within `SettingsScreen.kt` (header section vs. an existing group); whether the row's label is `R.string.settings_language` (new) or reuses an existing key.

7. **COMPLY-07 — Dual-device DEVICE_TESTING.md (partial: gesture-nav only, 3-button deferred)**: Run full checklist on Galaxy S23+ Android 16 (gesture-nav); 3-button-nav side documented as REVIEWS-C4 ACCEPT.
   - **Current:** Phase 1 UAT covered S23+ Android 16 (gesture-nav) for a build-correctness smoke. No 3-button-nav device available; no formal DEVICE_TESTING.md re-run since Phase 1.
   - **Target:** (a) Full DEVICE_TESTING.md checklist re-run on S23+ post-merge of COMPLY-01..06; (b) `01-CONTEXT.md` (or equivalent CONTEXT.md once discuss-phase writes it) amended with REVIEWS-C4-compliant ACCEPT entry for 3-button-nav verification; (c) backlog item `COMPLY-07-3BTN` added to `REQUIREMENTS.md` Deferred section.
   - **Acceptance:**
     - `.planning/phases/02-comply-platform-compliance/02-UAT.md` exists with full DEVICE_TESTING.md result rows for S23+ (all gesture-nav rows pass; 3-button-nav rows marked `DEFERRED — hardware unavailable, see COMPLY-07-3BTN`).
     - `02-CONTEXT.md` contains an `## Accepted Risks` entry citing PITFALLS §7 (edge-to-edge 3-button-nav inset breakage) with rationale, mitigation (defer to backlog), and revisit trigger (hardware available).
     - `REQUIREMENTS.md` Deferred section gains `COMPLY-07-3BTN` entry with traceback to this SPEC.

## Boundaries

**In scope:**
- Edge-to-edge correctness: theme cleanup + Scaffold inset audit + screenshot evidence (COMPLY-01)
- Predictive-back manifest flag + single `PlayerScreen` BackHandler→PredictiveBackHandler migration (COMPLY-02)
- Removal of two orphan permissions: `POST_NOTIFICATIONS` (COMPLY-03) + `FOREGROUND_SERVICE_MEDIA_PLAYBACK` (COMPLY-05)
- Splash Screen API adoption gated by `RootViewModel.start` (COMPLY-04)
- Per-app language: `generateLocaleConfig = true` + new `LanguageRow` in `SettingsScreen` firing `ACTION_APP_LOCALE_SETTINGS` (COMPLY-06)
- Manual UAT on Galaxy S23+ Android 16 + screenshot pack committed + REVIEWS-C4 ACCEPT for 3-button-nav (COMPLY-07 partial)

**Out of scope:**
- AGP 9 / compileSdk 36 / Activity Compose 1.13.0 bump — deferred to a dedicated AGP-9 phase (`DEPS-17` in REQUIREMENTS.md); blocked on Hilt-AGP-9 support
- Real `MediaSessionService` / background playback — BG-MEDIA-01 backlog item; Phase 2 only removes the orphan permission, does not add a service
- Runtime `POST_NOTIFICATIONS` request flow — re-introduced by BG-MEDIA milestone when a notification-using feature exists
- 3-button-nav verification of edge-to-edge — `COMPLY-07-3BTN` backlog; hardware-blocked
- `PlayerScreen.kt` structural split (1122 lines into `PlayerControls`/`PlayerGestures`/`PlayerSurface`) — POLISH-01 in Phase 4; only the line:187 BackHandler is touched in this phase
- New translations / new locale resources — `generateLocaleConfig` derives from existing `values-*/` directories; no new locales added
- `network_security_config.xml` cleartext tightening — SEC-02 backlog
- Per-app language picker as custom in-app UI — Phase 2 only opens the system dialog, no custom locale list

**Out of scope (procedural):**
- Merging Phase 1 PR #5 — operational gate; Phase 2 branches from `master` post-merge (see Open Questions)

## Constraints

- **No user-visible behavior change** beyond intended compliance work: splash appears (new), language row appears in Settings (new), back gesture animates on Player (changed from snap-exit). Everything else looks identical.
- **No SDK floor change:** `minSdk = 26`, `compileSdk = 35`, `targetSdk = 35` all unchanged. COMPLY-06 `LanguageRow` is gated on `Build.VERSION.SDK_INT >= 33` so devices on API 26–32 see no row (no functional regression — system dialog doesn't exist below 33).
- **No new third-party dependencies** beyond `androidx.core:core-splashscreen` (AndroidX, already in transitive graph via other AndroidX libs).
- **Test infrastructure pre-condition:** Phase 4 POLISH-04/05 wires JUnit5/Turbine/MockK/Robolectric. Phase 2 has no unit-test acceptance criteria — verification is build gates + manual UAT + grep checks + screenshot evidence. Future regressions on COMPLY-01..06 will be caught by POLISH-05 seed tests once they exist.
- **Branch base:** Phase 2 work branches from `master` *after* Phase 1 PR #5 is merged. If Phase 1 PR is still unmerged when Phase 2 plan execution begins, this becomes a hard precondition (see Open Questions).
- **Hardware:** Galaxy S23+ Android 16 (gesture-nav). No 3-button-nav device available (PITFALLS §7 partial verification — documented risk-accept).

## Acceptance Criteria

- [ ] `grep -E 'statusBarColor|navigationBarColor' app/src/main/res/values/themes.xml` returns empty
- [ ] All top-level `Scaffold` declarations in `app/`, `feature/library/`, `feature/player/`, `feature/settings/`, `feature/connection/`, `feature/home/` (or actual equivalents) explicitly reference `WindowInsets`
- [ ] ≥10 PNG screenshots committed under `.planning/phases/02-comply-platform-compliance/screenshots/` covering Home, Library, PlayerScreen (controls-visible + controls-hidden), Settings, Connection in light + dark mode
- [ ] `grep -c 'android:enableOnBackInvokedCallback="true"' app/src/main/AndroidManifest.xml` → `1`
- [ ] `grep -rn 'BackHandler\b' --include="*.kt" feature/ app/ core/` returns at most the `import` in `PlayerScreen.kt`; `PredictiveBackHandler` reference exists in `PlayerScreen.kt`
- [ ] `grep -c 'android.permission.POST_NOTIFICATIONS' app/src/main/AndroidManifest.xml` → `0`
- [ ] `grep -c FOREGROUND_SERVICE_MEDIA_PLAYBACK app/src/main/AndroidManifest.xml` → `0`
- [ ] `grep -c '"android.permission.FOREGROUND_SERVICE"' app/src/main/AndroidManifest.xml` → `1` (base FGS preserved)
- [ ] `androidx.core:core-splashscreen` present in `gradle/libs.versions.toml` and consumed by `app/build.gradle.kts`
- [ ] `grep -c installSplashScreen app/src/main/java/io/stashapp/android/MainActivity.kt` → `1`
- [ ] `grep -c setKeepOnScreenCondition app/src/main/java/io/stashapp/android/MainActivity.kt` → `1`
- [ ] `grep -c "generateLocaleConfig = true" app/build.gradle.kts` → `1`
- [ ] Merged debug manifest references generated `locale_config.xml`
- [ ] `grep -c "ACTION_APP_LOCALE_SETTINGS" feature/settings/src/main/java/io/stashapp/android/feature/settings/SettingsScreen.kt` → `1`
- [ ] `./gradlew --configuration-cache assembleDebug assembleRelease check` green
- [ ] `02-UAT.md` committed with full DEVICE_TESTING.md result for S23+ (gesture-nav rows pass; 3-button-nav rows DEFERRED → COMPLY-07-3BTN)
- [ ] `02-CONTEXT.md` contains REVIEWS-C4-compliant `## Accepted Risks` entry citing PITFALLS §7 (3-button-nav edge-to-edge verification deferred)
- [ ] `REQUIREMENTS.md` Deferred section gains `COMPLY-07-3BTN` entry

## Ambiguity Report

| Dimension          | Score  | Min   | Status | Notes                                                                                |
|--------------------|--------|-------|--------|--------------------------------------------------------------------------------------|
| Goal Clarity       | 0.85   | 0.75  | ✓      | Premise reframed on current floor; goal = land COMPLY-01..06 + partial COMPLY-07     |
| Boundary Clarity   | 0.78   | 0.70  | ✓      | Explicit in/out lists; orphan-permission strategy locked; AGP-9 work explicitly out  |
| Constraint Clarity | 0.75   | 0.65  | ✓      | No SDK floor change; no behavior change; S23+ only; branch base = post-merge master  |
| Acceptance Criteria| 0.78   | 0.70  | ✓      | 18 falsifiable checks; 4 manual UAT items with concrete artifacts (screenshots, MD)  |
| **Ambiguity**      | 0.2015 | ≤0.20 | ⚠      | Boundary: at the threshold; residuals captured under Open Questions for discuss-phase |

Status: ✓ = met minimum, ⚠ = below minimum (planner treats as assumption)

**Note on Ambiguity overall:** 0.2015 is fractionally above the ≤0.20 gate, but all four dimensions cleared their minimums and the user (via Spec Gate prompt) explicitly chose "Write SPEC.md — flag residuals for discuss-phase." Residuals are procedural, not requirement-blocking.

## Open Questions (residuals for discuss-phase)

These do not block planning but discuss-phase should resolve them:

1. **Locale row placement** in `SettingsScreen.kt` — top-level row vs. nested under an existing "App" or "Preferences" group; new vs. reused string resource key.
2. **Branch base for Phase 2 execution** — `master` (clean, requires Phase 1 PR #5 merged first) vs. `phase-1/deps-bump` (work on top of unmerged PR, risk of base churn on Phase 1 review feedback). Recommend `master` post-merge.
3. **COMPLY-01 screenshot count + structure** — confirmed minimum 10 PNGs; discuss-phase should decide directory naming convention (`screenshots/<screen>-<theme>.png` vs. `screenshots/<theme>/<screen>.png`) and whether to commit a single `SCREENSHOTS.md` index.
4. **PredictiveBackHandler implementation detail** — gesture-cancel handling (user starts swipe then cancels) — should `onExit()` fire on the cancel path? Material3 default is no; SPEC follows that default but discuss-phase locks it.

## Interview Log

| Round | Perspective              | Question summary                                              | Decision locked                                                                                  |
|-------|--------------------------|---------------------------------------------------------------|--------------------------------------------------------------------------------------------------|
| 1     | Researcher               | Premise: AGP-9 deferral changes Phase 2 floor — how to frame? | Reframe on current floor (AGP 8.7.3 / compileSdk 35 / Activity Compose 1.9.3)                    |
| 1     | Researcher               | COMPLY-07 dual-device situation?                              | S23+ only; defer 3-button-nav via REVIEWS-C4 ACCEPT → backlog COMPLY-07-3BTN                     |
| 1     | Researcher               | COMPLY-06 locale picker — exists or new?                      | New UI; grep confirmed zero locale code in SettingsScreen.kt (412 lines)                         |
| 2     | Researcher + Simplifier  | COMPLY-03 POST_NOTIFICATIONS — runtime helper or remove?      | Remove the orphan permission (matches COMPLY-05 pattern); BG-MEDIA re-introduces with caller    |
| 2     | Researcher + Simplifier  | COMPLY-02 predictive back — sweep size?                       | Manifest flag + PlayerScreen.kt:187 BackHandler → PredictiveBackHandler; sheets inherit          |
| 2     | Researcher + Simplifier  | COMPLY-01 edge-to-edge — falsifiability of "unreadable text"? | Code-level checks (theme cleanup + Scaffold WindowInsets) + screenshot pack on S23+              |
| —     | Gate decision            | Ambiguity 0.2015 — write or one more round?                    | Write SPEC.md, flag residuals (locale placement, branch base, screenshot structure) for discuss |

---

*Phase: 02-comply-platform-compliance*
*Spec created: 2026-05-17*
*Next step: /gsd-discuss-phase 2 — implementation decisions (how to build what's specified above; resolve the 4 Open Questions)*
