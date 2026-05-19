# Phase 6: SETTINGS-V3 — Hub + Drill-Down Settings Redesign — Specification

**Created:** 2026-05-19
**Ambiguity score:** 0.120 (gate: ≤ 0.20)
**Requirements:** 11 locked
**Design source:** `design_handoff_slopper_spine/README.md §6` (v3.0, committed 9aab2a4)

## Goal

Replace the single-scroll SettingsScreen with the v3.0 hub + drill-down architecture: a landing hub with server status card, quick search, and grouped category rows; seven detail pages (Playback, Quality & codecs, Display, Library, Server, About & diagnostics, Settings search); and three new reusable components (`CSlider`, `HubRow`, `DRow`).

## Background

**Current state (scouted 2026-05-19):**
- `feature/settings/src/main/java/.../SettingsScreen.kt`: 579-line single composable with `SectionHeader` + `SectionContainer` + `ChipRowPref` + dot-slider pattern
- Single route: `Routes.Settings = "settings"` — no sub-navigation
- All settings on one screen: Player preferences, App settings, Language row
- `SettingsViewModel` inline in `SettingsScreen.kt` (no separate file), reads `PlayerPreferences` + `UiPreferences`
- No server status info surfaced anywhere in settings
- Dot-sliders consume ~80dp each (seek sensitivity, double-tap seek, etc.)
- "Disconnect server" not currently on the settings screen (accessible from Connection flow only)

**Phase 5 SPINE** applied Spine token styling to the existing layout — this phase replaces the layout entirely.

**Resolved design questions:**
- Hub search field → navigates to `settings/search` overlay (in-page, not a new NavGraph destination)
- Server detail page → reads from existing `ConnectionStore` / `EndpointStateHolder` — no new GraphQL calls
- Codec test button → stub for v1 (navigates nowhere; shows a toast "Codec test — coming soon")
- Downloads (beta) group in Library detail → hidden behind `if (false)` for v1 (feature not implemented)
- Danger zone "Disconnect" → calls the existing `ConnectionRepository.disconnect()` / navigate to Connection screen

## Requirements

1. **Settings hub landing (SETTINGS-01):** `SettingsScreen` composable is replaced with a hub layout: server status card at top, quick search field, four grouped `HubGroup`/`HubRow` categories.
   - Current: single-scroll layout
   - Target: `LazyColumn` with server status card → search field → 4 `HubGroup`s: (untitled: Playback/Quality/Display/Library) + App (Nav bar/Cast/About) + Danger (Disconnect)
   - Each `HubRow` shows: 32dp icon container (SurfaceHigh bg), label (SpaceGrotesk 13.5sp W500), inline current-values summary (JetBrainsMono 11sp AccentPrimary), chevron
   - Acceptance: `grep -c 'HubRow\|HubGroup' feature/settings/src/main/java/.../SettingsScreen.kt` → ≥ 4; `./gradlew :feature:settings:assembleDebug` → BUILD SUCCESSFUL

2. **Server status card (SETTINGS-02):** Top of the hub shows server name, green online dot, Stash version, and scene count. (Latency omitted — `ServerInfo` has no latency field; deferred per D-04.)
   - Current: not shown in settings
   - Target: `Surface` card (ShapeLarge, Border) with: left 40dp accent icon container, center `TitleMedium` endpoint name + inline `Success` dot, `MetaMono` sub "Stash vX.X.X · N scenes"; right chevron → navigates to settings/server detail page
   - Reads from `ConnectionRepository` / `EndpointStateHolder` — no new data calls; uses existing connected-server state already available in app
   - Acceptance: server card renders when connected; tapping navigates to settings/server

3. **Settings sub-navigation (SETTINGS-03):** Six new sub-routes wired in the app's NavHost.
   - Current: single `Routes.Settings = "settings"`
   - Target: add to `Routes.kt`: `SettingsPlayback = "settings/playback"`, `SettingsCodecs = "settings/codecs"`, `SettingsDisplay = "settings/display"`, `SettingsLibrary = "settings/library"`, `SettingsServer = "settings/server"`, `SettingsAbout = "settings/about"`; all registered in `AppNavHost` in `MainActivity.kt`; back navigation from detail → hub works correctly
   - Acceptance: `grep -c 'settings/' core/ui/src/main/java/.../Routes.kt` → ≥ 6; `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL

4. **`CSlider` component (SETTINGS-04):** Compact horizontal slider with inline mono value bubble replacing all dot-row sliders.
   - Current: dot-row sliders consuming ~80dp each
   - Target: new `@Composable fun CSlider(value, onValueChange, range, steps, valueLabel, modifier)` in `core/designsystem/component/`; layout: `Row` with track (flex 1, height 3dp, SurfaceHigh bg, AccentPrimary fill, 3 tick marks at 25/50/75%) + 60dp value bubble (accent-8%-bg, accent-25%-border, JetBrainsMono 11sp W600 AccentPrimary, 4dp radius); thumb: 14dp circle AccentPrimary fill
   - Acceptance: `ls core/designsystem/src/main/java/.../component/CSlider.kt` → exists; compiles and renders

5. **`DRow` component (SETTINGS-05):** Detail-page row primitive supporting inline (key/value/switch) and stacked (key/body) modes.
   - Current: `SettingRow` (single mode only)
   - Target: new `@Composable fun DRow(label, hint, trailingContent, modifier)` + stacked variant `DRowStacked(label, hint, body)` in `core/designsystem/component/`; inline mode: label + hint (flex 1) + trailing widget; stacked mode: label + hint on top, full-width body below; 1dp Border dividers between rows inside a `DetailGroup`
   - Acceptance: `ls core/designsystem/src/main/java/.../component/DRow.kt` → exists

6. **Playback detail page (SETTINGS-06):** `SettingsPlaybackScreen` composable at route `settings/playback` with all player preferences.
   - Current: player prefs on the main settings scroll
   - Target: 4 `DetailGroup`s: Defaults (speed chip row ×6, aspect chip row ×3, auto-play switch, auto-rotate switch) · Seeking (double-tap `CSlider` 5–60s, scrub sensitivity `CSlider` 50–300 ms/px, show chapter thumbnails switch) · Resume & skip (resume threshold `CSlider` 0–30s, completion threshold `CSlider` 50–100%, skip intro `CSlider` 0–120s) · Player chrome [Power user badge] (lock controls / show codec badge / show queue position / haptics — switches)
   - All values read/write through existing `PlayerSettings` interface (Phase 4 POLISH-06 wired this)
   - Acceptance: `grep -c 'CSlider' feature/settings/src/main/java/.../SettingsPlaybackScreen.kt` → ≥ 3

7. **Quality & codecs detail page (SETTINGS-07):** `SettingsCodcsScreen` at route `settings/codecs`.
   - Target: capability banner (AccentPrimary 6% bg + 25% border, ShapeSmall, codec status line) · Decoder group (preference chip row: Auto/Prefer HW/Prefer SW, fallback switch, tunneling switch) · Buffer group (buffer size chip row: Small/Medium/Large) · Display & HDR group (HDR passthrough / match refresh rate / match resolution — switches) · Diagnostics footer (`TestBtn` "Run codec test" → toast stub for v1)
   - Acceptance: `ls feature/settings/src/main/java/.../SettingsCodecsScreen.kt` → exists; assembleDebug green

8. **Display detail page (SETTINGS-08):** `SettingsDisplayScreen` at route `settings/display`.
   - Target: Theme group with 3-column palette swatch grid (Sage/Ember/Signal — active state with accent-bg check mark; tapping switches `SpineColors.AccentPrimary` at runtime via a new `ThemePreference` in `UiPreferences`) · AMOLED / reduce motion switches · Library layout group (grid columns chip row, card density chip row) · Card chrome group (rating/play-count/resolution/resume-bar/studio-caption switches) · Player group (show chapter strip / tap-to-peek switches)
   - Acceptance: `ls feature/settings/src/main/java/.../SettingsDisplayScreen.kt` → exists

9. **Library detail page (SETTINGS-09):** `SettingsLibraryScreen` at route `settings/library`.
   - Target: Sync group (activity tracking / sync ratings / O-counter / markers — switches) · Cache group (image cache `CSlider` 64–512 MB, duration chip row, "Clear image cache · X MB used" footer button) · History group (keep watch history / history on home / smart rails — switches) · Downloads group (hidden via `if (false)` — beta, not implemented)
   - Acceptance: `ls feature/settings/src/main/java/.../SettingsLibraryScreen.kt` → exists

10. **Server + About detail pages (SETTINGS-10):** `SettingsServerScreen` and `SettingsAboutScreen` at their respective routes.
    - **Server:** status panel (green dot + CONNECTED MetaMono, endpoint URL, version sub) · 4-column counts grid (Scenes/Studios/Performers/Tags, hairline Border gaps) · Network group (endpoint, TLS, API key with "Replace" button) · Actions group (Refresh library / Trigger scan / Edit connection — chev rows) · Danger zone (Disconnect — Error styling)
    - **About:** 56dp logo + app name (SpaceGrotesk 22sp W700) + MetaMono version/build/date · Capabilities (codec support status, hardware decoder, OpenGL, HDR) · Storage (image cache size, db size) · Diagnostics (view logs, run network test stub, send debug report stub) · Legal (open-source licenses, built on Stash/ExoPlayer/Compose)
    - Acceptance: both files exist; `./gradlew :feature:settings:assembleDebug` → BUILD SUCCESSFUL

11. **Settings search overlay (SETTINGS-11):** In-hub search that matches setting names across all detail pages.
    - Current: search field is decorative only
    - Target: tapping the hub search field reveals an `AnimatedVisibility` overlay with: back arrow + active search field (AccentPrimary border, blinking cursor) · Results list with `SearchHit` rows (breadcrumb MetaMono path, label with highlighted match, current value right, hint below) · Tapping a result navigates to that detail page (and briefly flashes the matched row's border)
    - The search index is a static `List<SettingSearchEntry>` hardcoded in the ViewModel (no server call)
    - Acceptance: `grep -c 'SearchHit\|searchQuery\|searchResults' feature/settings/src/main/java/.../*.kt` → ≥ 3; typing in search field shows filtered results

## Boundaries

**In scope:**
- `SettingsScreen.kt` replaced with hub layout
- 6 new detail page composables
- `CSlider` and `DRow` new components in `core/designsystem/component/`
- 6 new sub-routes in `Routes.kt` + `AppNavHost`
- Server status card reading from existing `EndpointStateHolder`
- Palette swatch picker for Sage/Ember/Signal (requires new `accentPalette` pref in `UiPreferences`)
- Settings search (static index, no server call)

**Out of scope:**
- Any change to ConnectionScreen, PlayerScreen, or other existing screens
- Real codec test execution (stub only)
- Downloads/offline feature (hidden behind `if (false)`)
- Cast & Connect row (navigates nowhere in v1 — stub chevron)
- New GraphQL operations for server stats (counts come from existing connected-server state)
- Cast (no implementation exists)

## Constraints

- **Toolchain floor:** AGP 8.7.3 / Kotlin 2.2.20 / compileSdk 35 (unchanged)
- **POLISH-06 interfaces:** `PlayerSettings` and `UiSettings` in `:core:domain` already wired — use them, not `PlayerPreferences` directly
- **Phase 5 Spine tokens:** All new screens use `SpineColors.*`, `MetaMono`, `MonoSmall`, `ShapeSmall/Medium/Large` from Phase 5
- **Back navigation:** Detail pages pop back to the hub; the hub itself pops back to the previous screen (Home/Library etc.)
- **`SettingsViewModel` scope:** Expand to cover all 6 detail pages (or create per-page ViewModels — decided in discuss-phase)

## Ambiguity Report

```
Goal Clarity:       0.93 (min 0.75) ✓
Boundary Clarity:   0.88 (min 0.70) ✓
Constraint Clarity: 0.82 (min 0.65) ✓
Acceptance Criteria:0.85 (min 0.70) ✓
Ambiguity score:    0.120 (gate: ≤ 0.20) ✓ PASS
```
