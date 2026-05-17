# Phase 2: COMPLY (Platform Compliance) - Context

**Gathered:** 2026-05-17
**Status:** Ready for planning
**Mode:** `--auto` (single-pass, recommended defaults; no AskUserQuestion turns)

<domain>
## Phase Boundary

Land the framework-enforced platform contracts that `targetSdk = 36` will eventually mandate — proactively, on the current floor (AGP 8.7.3 / compileSdk 35 / Activity Compose 1.9.3 / Compose BOM 2026.05.00) — without user-visible behavior change beyond:

1. **New** splash screen on cold launch (was: white flash)
2. **New** "Language" row in Settings (API 33+ only) that opens the system per-app locale dialog
3. **Changed** Player back gesture: animated preview before exit (was: snap-exit)
4. **Cleaner** manifest: two orphan permissions removed (`POST_NOTIFICATIONS`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`)
5. **Theme cleanup** so `enableEdgeToEdge()` actually works (currently defeated by hardcoded black `statusBarColor` / `navigationBarColor` in `themes.xml`)

When the future AGP-9 phase finally bumps `compileSdk = 36`, the system will then *enforce* the contracts this phase already satisfies — that phase touches only build infrastructure, not UI plumbing.

</domain>

<spec_lock>
## Requirements (locked via SPEC.md)

**7 requirements are locked.** See `02-SPEC.md` for full requirements, boundaries, and acceptance criteria.

Downstream agents MUST read `02-SPEC.md` before planning or implementing. Requirements are not duplicated here.

**In scope (from SPEC.md):**
- Edge-to-edge correctness: theme cleanup + Scaffold inset audit + screenshot evidence (COMPLY-01)
- Predictive-back manifest flag + single `PlayerScreen` BackHandler→PredictiveBackHandler migration (COMPLY-02)
- Removal of two orphan permissions: `POST_NOTIFICATIONS` (COMPLY-03) + `FOREGROUND_SERVICE_MEDIA_PLAYBACK` (COMPLY-05)
- Splash Screen API adoption gated by `RootViewModel.start` (COMPLY-04)
- Per-app language: `generateLocaleConfig = true` + new `LanguageRow` in `SettingsScreen` firing `ACTION_APP_LOCALE_SETTINGS` (COMPLY-06)
- Manual UAT on Galaxy S23+ Android 16 + screenshot pack committed + REVIEWS-C4 ACCEPT for 3-button-nav (COMPLY-07 partial)

**Out of scope (from SPEC.md):**
- AGP 9 / compileSdk 36 / Activity Compose 1.13.0 bump — deferred to dedicated AGP-9 phase (DEPS-17)
- Real `MediaSessionService` / background playback — BG-MEDIA-01 backlog
- Runtime `POST_NOTIFICATIONS` request flow — re-introduced by BG-MEDIA milestone
- 3-button-nav verification of edge-to-edge — `COMPLY-07-3BTN` backlog (hardware-blocked)
- `PlayerScreen.kt` structural split (POLISH-01, Phase 4)
- New translations / new locale resources — `generateLocaleConfig` derives from existing `values-*/` directories
- `network_security_config.xml` cleartext tightening — SEC-02 backlog
- Custom in-app locale list UI — only the system dialog is exposed

</spec_lock>

<decisions>
## Implementation Decisions

### 1. Branch shape — isolated `phase-2/comply-platform-compliance`

Single branch: `phase-2/comply-platform-compliance` (already created; head `7d851a0`). Base is `73a5677` (current head of `phase-1/deps-bump` / PR #5). When PR #5 merges to `master`, this branch rebases onto `master` before opening the Phase 2 PR.

Rationale:
- Keeps Phase 2 commits out of PR #5 (already happened: Phase 1 SPEC commit `b5b51f0` was moved off `phase-1/deps-bump` onto this branch when it was accidentally landed there).
- Phase 1 commits are still needed at the floor (Compose BOM 2026.05.00 is a precondition for COMPLY-04 splash + COMPLY-01 inset modifiers).
- Rebasing onto a squashed `master` is cleaner than merging across two open PRs.

**Locked for planner:** plans land on `phase-2/comply-platform-compliance`. Plans must NOT touch `gradle/libs.versions.toml` toolchain pins (kotlin / agp / composeBom / hilt / coil / apollo / okhttp) — those are Phase 1 territory. Adding the `core-splashscreen` library entry is fine because it's net-new and Phase 2-scoped.

### 2. Commit shape — one commit per COMPLY requirement (atomic, bisectable)

Same pattern as Phase 1 (Decision 1 there): one commit per `COMPLY-XX`. Format: `feat(comply): COMPLY-XX — <short description>` for code changes, `chore(comply): COMPLY-XX — …` for manifest-only / permission-removal changes. Total: **7 commits** plus an 8th for the screenshot pack + UAT artifacts.

Rationale:
- Bisect-friendly: if any single COMPLY change regresses Compose render or test smoke, the bad commit is one diff away.
- The dependency order across requirements is loose (COMPLY-01..06 are mostly disjoint surfaces) so commit order = whatever the executor finds convenient, except: COMPLY-04 splash MUST land before COMPLY-01 final UAT (splash affects cold-launch screenshot).

**Locked for planner:** plans produce atomic per-requirement commits; the screenshot pack is its own commit at the end (after all 6 code changes land and rebuild green).

### 3. Predictive back — use `PredictiveBackHandler` now, migrate to `NavigationBackHandler` in the AGP-9 phase

`PredictiveBackHandler` (androidx.activity.compose) is available from Activity Compose 1.8.0+. We're on 1.9.3 — it works. `NavigationBackHandler` / `NavigationEventState` (the post-deprecation API from Compose Multiplatform 1.10 / AndroidX Activity 1.10+) is **NOT** available at Activity Compose 1.9.3 — that lands when DEPS-07 + DEPS-17 bump to Activity Compose 1.13.0+.

Therefore:
- Phase 2 implementation: `PredictiveBackHandler { progress -> progress.collect { } ; onExit() }` at `PlayerScreen.kt:187`.
- Backlog item to add (to REQUIREMENTS.md Deferred section): `COMPLY-02-NAV-EVENT` — migrate to `NavigationBackHandler` once Activity Compose ≥ 1.10 is on the floor (AGP-9 phase).

**Gesture-cancel behavior:** Material3 default — `onExit()` does NOT fire on cancel/abandoned swipe. The lambda's body wraps the `onExit()` call inside the post-collect block so a cancelled `progress` flow (user lifts finger mid-swipe) returns from the lambda without exit. This matches user expectation (you can "peek" without committing).

**Locked for planner:** plan step for COMPLY-02 references `PredictiveBackHandler` (NOT `NavigationBackHandler`). PITFALLS §8 hazard ("never mutate `enabled` mid-gesture") is satisfied because `PlayerScreen.kt:187` `BackHandler` is unconditional (no `enabled = …` argument).

### 4. Edge-to-edge — PITFALLS §7 patterns applied without restructuring

Three pattern decisions for COMPLY-01:

1. **`themes.xml`** — strip `statusBarColor` and `navigationBarColor` overrides. Keep `windowBackground = @android:color/black` (it only paints below the Compose surface; harmless and visually consistent with the player's letterbox).
2. **`PlayerScreen`** — wrap the player surface in `Box(Modifier.fillMaxSize())`, place control overlays inside an inner `Box(Modifier.safeDrawingPadding())`. The SurfaceView itself must NOT receive `safeDrawing` insets or video letterboxes incorrectly.
3. **`FilterSheet` + `NavCustomizeSheet`** — pass `contentWindowInsets = { WindowInsets.navigationBars }` to `ModalBottomSheet`. Available in Compose BOM 2026.05.00 (material3 ≥ 1.7).

These are the minimum-touch patterns from PITFALLS §7. No Scaffold restructuring; no migration to a custom `WindowInsetsControllerCompat` wrapper.

**Locked for planner:** the inset modifier sites are exhaustively listed (themes.xml + PlayerScreen + FilterSheet + NavCustomizeSheet). Other screens (Home, Library, Settings, Connection) get a grep-audit: any top-level `Scaffold` declaration is verified to either use the default `contentWindowInsets = ScaffoldDefaults.contentWindowInsets` (which is `WindowInsets.systemBars`) or an explicit `WindowInsets`. If the audit finds a Scaffold passing `contentWindowInsets = WindowInsets(0)` without explicit per-child inset handling, the planner flags it as a follow-up (not a Phase 2 blocker — Material3 default Scaffold already does the right thing).

### 5. Splash — `core-splashscreen` artifact gated on `RootViewModel.start != null`

Steps for COMPLY-04 in execution order:

1. Add `androidx.core:core-splashscreen:1.0.1` to `gradle/libs.versions.toml` (use the stable; the 1.1.x line is alpha as of 2026-05).
2. Add a `Theme.Stash.Splash` style in `themes.xml` with `parent = "Theme.SplashScreen"`, `windowSplashScreenAnimatedIcon = @mipmap/ic_launcher`, `windowSplashScreenBackground = @android:color/black`, `postSplashScreenTheme = @style/Theme.Stash`.
3. Set `<activity android:theme="@style/Theme.Stash.Splash">` in `AndroidManifest.xml` (the `<application>` theme stays `Theme.Stash`).
4. In `MainActivity.onCreate`, **before** `super.onCreate(savedInstanceState)`:
   ```kotlin
   val splashScreen = installSplashScreen()
   splashScreen.setKeepOnScreenCondition { rootViewModelStartIsNull() }
   ```
   The `rootViewModelStartIsNull()` helper reads `_start.value == null` from the Hilt-graph `RootViewModel` instance — but Hilt isn't ready before `super.onCreate`. Two implementation patterns are acceptable; planner picks one:
   - **A:** Make `installSplashScreen` keep alive until `setContent` runs and the `LaunchedEffect(rootViewModel.start)` flips a `AtomicBoolean`. Simplest; minor splash lingering risk.
   - **B:** Inject a lightweight `ConnectionRepository.activeServer()` synchronous probe directly into the keep condition before Hilt-Compose graph builds. Faster splash dismiss; more coupling.

   **Recommended for planner:** **A** — simpler, no Hilt-before-super gymnastics, and the ~50ms splash extension is invisible to humans.
5. RootViewModel.start logic stays unchanged (MainActivity.kt:67–76 already StateFlow-emits `Routes.Home` or `Routes.Connection`).

**Locked for planner:** dependency entry + theme parent + manifest activity theme + `installSplashScreen()` call site are all fixed. The keep-condition implementation pattern (A vs B) is flagged as a Plan Sub-Decision (planner picks during planning; A is recommended).

### 6. Locale picker (COMPLY-06) — top-level row in SettingsScreen, API-33 gated

UI placement:
- Add a `LanguageRow` composable in `SettingsScreen.kt` as a **top-level row** in the primary settings list (above any "Advanced" / "Diagnostics" sections, below the connection block).
- New string resource `R.string.settings_language` (English: "Language") in `app/src/main/res/values/strings.xml`. Other `values-*/` directories (if they exist) get untranslated fallback to English — translation work is out of scope.
- Row composition: leading icon `Icons.Outlined.Language`, headline = `stringResource(R.string.settings_language)`, supporting text = `stringResource(R.string.settings_language_description)` (a 1-sentence "Choose the language for this app independently from your device").
- On click: fire `Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply { data = Uri.fromParts("package", context.packageName, null) }`. Wrap in `if (Build.VERSION.SDK_INT >= 33)` — row composable returns `null` (does not render) below API 33.

Build wiring:
- `app/build.gradle.kts` — add `androidResources { generateLocaleConfig = true }` inside the `android { … }` block.
- No manual `app/src/main/res/xml/locale_config.xml` — AGP generates it from existing `values-*` directories.

**Locked for planner:** placement + string resource keys + icon + intent + API gate are all locked. If the planner finds NO `values-*/` directories exist (i.e., the app currently has only English), it flags the issue: `generateLocaleConfig` produces an empty list and the system dialog shows only "App default" — still passes the COMPLY-06 acceptance check (dialog opens), but documented in the plan as "produces a one-locale dialog until translations are added (out of scope)".

### 7. Orphan permission removal (COMPLY-03 + COMPLY-05) — sed-style line removal

Both are single-line manifest deletions. No supporting code change; no rollback risk.

- COMPLY-03: delete `<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />` (line 15 of current manifest).
- COMPLY-05: delete `<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />` (line 13 of current manifest).
- Keep: `<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />` (base FGS — harmless, BG-MEDIA milestone will re-use).

Verification per requirement: `grep -c '<permission line>' app/src/main/AndroidManifest.xml` → `0` for the removed; `1` for `FOREGROUND_SERVICE`.

**Locked for planner:** two commits, each a single-line removal. Optionally fold both into one `chore(comply): COMPLY-03+05 — remove orphan permissions (POST_NOTIFICATIONS, FGS_MEDIA_PLAYBACK)` commit IF the planner judges the bisect-granularity loss is worth the cleaner history. Default: keep separate.

### 8. UAT + screenshot pack — flat directory, light/dark per screen, S23+ only

Directory layout: `.planning/phases/02-comply-platform-compliance/screenshots/<screen>-<theme>.png`
- `home-light.png` / `home-dark.png`
- `library-light.png` / `library-dark.png`
- `player-controls-visible-light.png` / `player-controls-visible-dark.png`
- `player-controls-hidden-light.png` / `player-controls-hidden-dark.png`
- `settings-light.png` / `settings-dark.png`
- `connection-light.png` / `connection-dark.png`

= 12 PNGs (exceeds SPEC.md ≥10 minimum). Plus `SCREENSHOTS.md` index documenting capture device (Galaxy S23+ Android 16, gesture-nav), commit SHA at capture, and reviewer sign-off line.

`02-UAT.md` follows the same format as `01-UAT.md`: row per DEVICE_TESTING.md checklist item, columns for `Result` (PASS/FAIL/DEFERRED), `Notes`, `Evidence` (screenshot filename if applicable). 3-button-nav rows all `DEFERRED — see COMPLY-07-3BTN backlog`.

**Locked for planner:** screenshot capture is its own task (the 8th commit), runs after all 6 COMPLY code changes are merged green and a fresh `assembleDebug` APK is installed on S23+.

### 9. Accepted Risks (REVIEWS-C4 compliance)

This phase explicitly accepts these risks under the same hygiene pattern as Phase 1 DEPS-16:

#### Risk 1: PITFALLS §7 — 3-button-nav edge-to-edge regressions unverified

- **What:** No 3-button-nav device available for COMPLY-07 dual-device gate. Edge-to-edge regressions on older Pixels / OEM 3-button-nav devices ship unverified.
- **Mitigation (structural):** Backlog item `COMPLY-07-3BTN` added to `REQUIREMENTS.md` Deferred section. When 3-button-nav hardware (or properly-configured emulator with `persist.sys.navigation_mode 0`) becomes available, the existing `DEVICE_TESTING.md` checklist re-runs on it; any regression is filed as a follow-up bug, not a Phase 2 reopen.
- **Revisit trigger:** Hardware availability. Could be folded into the AGP-9 phase's COMPLY re-verification gate since the SDK floor moves at the same time.
- **User ACCEPT:** 2026-05-17 (explicit via spec-phase Q2: "S23+ only — defer 3-button-nav via REVIEWS C4 ACCEPT").

#### Risk 2: PITFALLS §8 — `PredictiveBackHandler` is technically deprecated at Compose Multiplatform 1.10+

- **What:** `PredictiveBackHandler` is the current API at our Activity Compose 1.9.3 floor, but it's already on the deprecation path in the Compose Multiplatform 1.10 / AndroidX Activity 1.10+ ecosystem.
- **Mitigation (structural):** Backlog item `COMPLY-02-NAV-EVENT` added — migrate to `NavigationBackHandler` once Activity Compose ≥ 1.10 is on the floor (AGP-9 phase). PlayerScreen.kt becomes a single-call-site migration, low effort.
- **Revisit trigger:** AGP-9 phase landing Activity Compose 1.13.0+.
- **User ACCEPT (implicit):** Auto-mode picked `PredictiveBackHandler` as the only API actually present on the current floor; the deprecation note is informational, not a current-build issue.

### Claude's Discretion

These are not user-decided; the planner / executor exercises judgment per the guidance below.

- **Theme.Stash.Splash icon asset:** use the existing `@mipmap/ic_launcher` (already on disk). No new icon production. If the launcher icon's foreground layer crops awkwardly inside the splash circle on Android 12+, fall back to creating a `@drawable/splash_icon` variant — small (~100 LOC asset change, one-time).
- **Screenshot capture method:** `adb shell screencap` over USB or `Settings → Power → Capture screenshot` — either is fine; capture in the order listed in §8 and rename per the schema. No Compose `captureToImage` instrumentation needed for Phase 2.
- **String resource location:** `R.string.settings_language` lives in `app/src/main/res/values/strings.xml` (not a feature-module strings file), since `LanguageRow` calls a system Intent (not a feature-local action). This is consistent with how other system-Intent strings would land.
- **PR description structure:** planner produces a single PR at end of phase against `master` (post PR #5 merge + rebase). PR body includes: per-COMPLY-XX change summary, screenshot embeds (or links to `.planning/phases/02-comply-platform-compliance/screenshots/`), UAT result table, accepted-risk callouts.
- **`R.string.settings_language_description`** content: the planner writes a 1-sentence description following the SettingsScreen voice convention. If existing rows have descriptions, follow that style; if not, add one anyway — the row is novel and a description helps users understand it's per-app, not system-wide.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase-specific
- `.planning/phases/02-comply-platform-compliance/02-SPEC.md` — **locked requirements (MUST read before planning)** — 7 requirements, 18 acceptance criteria
- `.planning/phases/01-deps-foundation-bump/01-LEARNINGS.md` — Phase 1 outcomes including AGP-9 deferral context

### Project-level
- `.planning/PROJECT.md` — milestone constraints, core value, locked decisions
- `.planning/REQUIREMENTS.md` — COMPLY-01..07 entries + Deferred section (where COMPLY-07-3BTN + COMPLY-02-NAV-EVENT will land)
- `.planning/ROADMAP.md` — Phase 2 entry (note: original SDK-36 premise is reframed — see SPEC.md "Background")
- `.planning/STATE.md` — milestone status snapshot

### Research
- `.planning/research/PITFALLS.md` §7 — edge-to-edge enforcement on PlayerScreen + FilterSheet inset patterns; **MUST read for COMPLY-01 implementation**
- `.planning/research/PITFALLS.md` §8 — predictive back hazards; `PredictiveBackHandler` vs deprecated `NavigationBackHandler`; **MUST read for COMPLY-02**

### Codebase maps
- `.planning/codebase/ARCHITECTURE.md` — module graph (`:feature:settings` → `:core:ui` boundaries relevant for `LanguageRow`)
- `.planning/codebase/CONCERNS.md` — pre-existing concerns (verify any flagged inset / back issues not already documented)
- `.planning/codebase/CONVENTIONS.md` — code style patterns (`SettingsScreen.kt` row composition convention)

### Build / source touchpoints (read during planning)
- `app/src/main/AndroidManifest.xml` — manifest changes for COMPLY-02 (manifest flag), COMPLY-03 + COMPLY-05 (permission removal), COMPLY-04 (activity theme)
- `app/src/main/res/values/themes.xml` — strip system-bar colors (COMPLY-01); add `Theme.Stash.Splash` (COMPLY-04)
- `app/src/main/res/values/strings.xml` — `R.string.settings_language` + description (COMPLY-06)
- `app/src/main/java/io/stashapp/android/MainActivity.kt` — `installSplashScreen()` call site + keep condition (COMPLY-04)
- `app/build.gradle.kts` — `androidResources { generateLocaleConfig = true }` (COMPLY-06)
- `gradle/libs.versions.toml` — `androidx-core-splashscreen` library entry (COMPLY-04 dep)
- `feature/player/src/main/java/io/stashapp/android/feature/player/PlayerScreen.kt:187` — `BackHandler` → `PredictiveBackHandler` (COMPLY-02); `Box(Modifier.safeDrawingPadding())` control overlay wrap (COMPLY-01)
- `feature/library/src/main/java/io/stashapp/android/feature/library/FilterSheet.kt` — `contentWindowInsets = { WindowInsets.navigationBars }` on `ModalBottomSheet` (COMPLY-01)
- `core/ui/src/main/java/io/stashapp/android/core/ui/nav/NavCustomizeSheet.kt` — same `ModalBottomSheet` inset pattern (COMPLY-01)
- `feature/settings/src/main/java/io/stashapp/android/feature/settings/SettingsScreen.kt` — add `LanguageRow` (COMPLY-06)

### Upstream docs (consult during planning, NOT pre-fetched)
- https://developer.android.com/develop/ui/views/launch/splash-screen — Splash Screen API guide
- https://developer.android.com/develop/ui/views/predictive-back/predictive-back — Predictive back guide
- https://developer.android.com/develop/ui/views/layout/edge-to-edge — Edge-to-edge guide
- https://developer.android.com/guide/topics/resources/app-languages — Per-app language picker guide

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- **`RootViewModel.start: StateFlow<String?>`** (MainActivity.kt:67–76): the natural splash gate. `null` until `ConnectionRepository.activeServer()` first-emits; non-null after. Perfect `setKeepOnScreenCondition` source. **Pattern A** (LaunchedEffect → AtomicBoolean) recommended over poking the Hilt graph before `super.onCreate`.
- **`enableEdgeToEdge()`** (MainActivity.kt:87): already called in `onCreate`. No code addition needed for COMPLY-01 — only theme + per-screen inset wrap work.
- **Material3 `ModalBottomSheet`** (FilterSheet.kt, NavCustomizeSheet.kt): handles predictive-back natively once `enableOnBackInvokedCallback` manifest flag is on. Zero per-sheet code change for COMPLY-02.
- **Existing `SettingsScreen.kt` row pattern**: 412 lines of composables; `LanguageRow` follows whatever row scaffold the file already uses (planner inspects on first read).

### Established Patterns
- **Convention plugins own `compileSdk` / `targetSdk`** (`build-logic/convention/src/main/kotlin/io/stashapp/android/buildlogic/`): Phase 2 does NOT touch these. The `generateLocaleConfig` flag goes in `app/build.gradle.kts` (app-only resource setting), not a convention plugin (libraries don't need locale config).
- **Atomic per-requirement commits** (Phase 1 Decision 1): carries forward — one commit per COMPLY-XX requirement, format `feat(comply): COMPLY-XX — <description>` or `chore(comply): …` for manifest-only changes.
- **REVIEWS-C4 hardware-blocked deferral pattern** (Phase 1 DEPS-16): carries forward — accepted risks go in this CONTEXT.md `## Accepted Risks` (above), with backlog item in `REQUIREMENTS.md` Deferred section, revisit trigger documented.
- **PR-at-end-of-phase** (Phase 1 PR #5 pattern): carries forward — single PR after all COMPLY commits land green, opened against `master` post-merge of PR #5.

### Integration Points
- **`MainActivity.onCreate` boot sequence** (COMPLY-04): `installSplashScreen()` call comes BEFORE `super.onCreate(savedInstanceState)`. Existing `enableEdgeToEdge()` + `requestHighestRefreshRate()` stay where they are.
- **`SettingsScreen` integration** (COMPLY-06): `LanguageRow` composable lives next to existing row composables; injected with `LocalContext.current` for the intent; uses `LaunchedEffect`-free composition (no async needed — system intent is sync).
- **Theme inheritance chain** (COMPLY-04): `Theme.SplashScreen` → `Theme.Stash.Splash` → `Theme.Stash` (via `postSplashScreenTheme`). Activity declares `Theme.Stash.Splash`; system transitions to `Theme.Stash` after first composition.
- **Manifest merge** (COMPLY-06): generated `locale_config.xml` (from `androidResources { generateLocaleConfig = true }`) is auto-merged into the application manifest under `<application android:localeConfig="@xml/locale_config" />` — no manual manifest edit needed for the merge wiring itself.

</code_context>

<specifics>
## Specific Ideas

- **Splash icon = launcher icon (no new asset).** Matches Slopper's existing visual identity; no design dependency. If the launcher icon's adaptive layers crop oddly inside the splash circle on Android 12+, a `@drawable/splash_icon` foreground variant is acceptable (Claude's discretion in §9).
- **`LanguageRow` icon = `Icons.Outlined.Language`** — Material icons extended (already in catalog), no new asset.
- **Screenshot pack capture order:** Home → Library (browse view) → Library with filter sheet open → Player (controls visible, then hidden) → Settings (scrolled to Language row) → Connection. Re-capture in dark theme after toggling system theme.
- **`02-UAT.md` cover sheet:** device, build SHA, capture timestamp, reviewer name, summary verdict (PASS / PASS-WITH-NOTES / FAIL).

</specifics>

<deferred>
## Deferred Ideas

These were touched on during spec/discuss but belong in other phases. They are NOT in Phase 2 scope but are captured here so they don't get lost.

- **COMPLY-07-3BTN** (carry-forward backlog): re-run DEVICE_TESTING.md on a 3-button-nav device (or properly-configured emulator) once hardware is available. PITFALLS §7 partial verification. To be added to `REQUIREMENTS.md` Deferred section during the first planning commit. Revisit trigger: hardware availability OR AGP-9 phase COMPLY re-verification gate.
- **COMPLY-02-NAV-EVENT** (carry-forward backlog): migrate `PredictiveBackHandler` in `PlayerScreen.kt` to `NavigationBackHandler` / `NavigationEventState` once Activity Compose ≥ 1.10 is on the floor (AGP-9 phase / DEPS-17 successor). Single call-site migration. To be added to `REQUIREMENTS.md` Deferred section during the first planning commit.
- **Real notification flow** (BG-MEDIA-01 / BG-MEDIA-02 in REQUIREMENTS.md): When `MediaSessionService` lands, re-declare `POST_NOTIFICATIONS` with proper `ActivityResultContracts.RequestPermission` flow and `FOREGROUND_SERVICE_MEDIA_PLAYBACK` permission. Phase 2 only removes the orphans; BG-MEDIA milestone owns the re-introduction.
- **Translation work** — `generateLocaleConfig` derives its locale list from existing `values-*/` directories. If Slopper currently ships English-only (planner verifies), the system dialog will show one locale. Adding translations is a separate effort, not modernization.
- **`PlayerScreen.kt` structural split** — POLISH-01 in Phase 4. Phase 2 only touches `PlayerScreen.kt:187` (BackHandler) and adds an inner `Box(Modifier.safeDrawingPadding())` wrap; no other surgery.
- **Custom in-app locale list UI** — Phase 2 only opens the system dialog. Building a custom locale picker (with our own list, our own animations, etc.) would be a separate feature and is explicitly out of scope per SPEC.md.
- **`network_security_config.xml` cleartext tightening** — SEC-02 backlog. Phase 2 stays clear of security config.

### Reviewed Todos (not folded)
- None — no pre-existing todos cross-referenced for Phase 2 scope.

</deferred>

---

*Phase: 02-comply-platform-compliance*
*Context gathered: 2026-05-17*
*Mode: --auto (recommended defaults; all 4 SPEC.md Open Questions resolved by selecting Recommended option)*
