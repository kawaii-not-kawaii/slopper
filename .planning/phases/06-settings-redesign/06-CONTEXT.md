# Phase 6: SETTINGS-V3 — Hub + Drill-Down Settings Redesign - Context

**Gathered:** 2026-05-19
**Status:** Ready for planning
**Mode:** `--auto` (single-pass, recommended defaults)

<domain>
## Phase Boundary

Replace the single-scroll SettingsScreen with the v3.0 hub+drill-down architecture from `design_handoff_slopper_spine/README.md §6`. All other screens, routes, and features are untouched.
</domain>

<spec_lock>
## Requirements (locked via SPEC.md)

**11 requirements are locked.** See `06-SPEC.md` for full requirements, boundaries, and acceptance criteria.

**In scope:** Hub landing, server status card, 6 sub-routes, CSlider + DRow components, 6 detail pages, settings search overlay.

**Out of scope:** ConnectionScreen, PlayerScreen, cast/connect (stub), downloads beta (hidden).
</spec_lock>

<decisions>
## Implementation Decisions

### Decision 1 — ViewModel Architecture
**D-01:** Single `SettingsViewModel` shared across hub and all detail pages. `hiltViewModel()` scoped to `NavBackStackEntry` for each composable — each detail page gets its own VM instance but they all use the same class.

`SettingsViewModel` additions needed:
- Inject `ConnectionRepository` (already present) + `EndpointStateHolder`
- Add `val serverInfo: StateFlow<ServerInfo?>` — fetched via `connectionRepository.test(activeServer)` on init, stored in VM
- Add `val activeServer: StateFlow<StashServer?>` from `connectionRepository.activeServer()`
- Add `val accentPalette: StateFlow<String>` from `uiPrefs.accentPalette`
- Add `fun setAccentPalette(palette: String)` setter
- All existing `setPlayer` / `setUi` helpers stay

**No separate per-page VMs** — all prefs are already in `PlayerSettings`/`UiSettings` and read via `collectAsStateWithLifecycle` directly in composables. The VM is just the mutation surface.

### Decision 2 — Sub-Navigation Pattern
**D-02:** Use existing flat `composable()` entries in `AppNavHost` in `MainActivity.kt`. No nested NavHost.

Add to `Routes.kt`:
```kotlin
const val SettingsPlayback = "settings/playback"
const val SettingsCodecs   = "settings/codecs"
const val SettingsDisplay  = "settings/display"
const val SettingsLibrary  = "settings/library"
const val SettingsServer   = "settings/server"
const val SettingsAbout    = "settings/about"
```

Each detail page composable receives `onBack: () -> Unit = { navController.popBackStack() }`.

`Routes.Settings = "settings"` remains unchanged — existing nav calls continue to work.

**Back navigation gate:** `route == Routes.Settings` in `MainActivity.tabNavigate` handles the `isTopLevel` check. Detail sub-routes are NOT top-level tabs — back presses pop to hub naturally via standard `popBackStack()`.

### Decision 3 — CSlider and DRow Placement
**D-03:** Both in `core/designsystem/src/main/java/io/stashapp/android/core/designsystem/component/`:
- `CSlider.kt` — reusable across settings and any future Spine feature
- `DRow.kt` — reusable settings-row primitive

They're pure UI components with no domain knowledge, same as `SceneCard.kt`, `SpineResumeCard.kt`.

### Decision 4 — Server Status Card Data
**D-04:** `SettingsViewModel` fetches `ServerInfo` on initialization via a `viewModelScope.launch { connectionRepository.test(activeServer) }` call. Stored as `StateFlow<ServerInfo?>`, initialized to `null`. The card renders a loading/stub state when null.

Fields displayed: endpoint URL, `ServerInfo.version`, `ServerInfo.sceneCount`, latency stub ("< 10ms" — `test()` doesn't return latency; use a hardcoded placeholder for v1).

Fallback: if network unavailable when settings opens, card shows "Connected · tap to refresh" with the URL from `activeServer`.

### Decision 5 — Accent Palette Switching
**D-05:** Store selected palette in `UiPreferences` as `val accentPalette: Flow<String>` ("sage"/"ember"/"signal"), default "sage". Add setter `suspend fun setAccentPalette(name: String)`.

Runtime switching approach: `StashTheme` in `Theme.kt` accepts an optional `accentName: String` parameter. Inside `StashTheme`, derive the three accent colors from the palette name and provide them via a new `LocalAccentColors: ProvidableCompositionLocal<AccentColors>`:
```kotlin
data class AccentColors(val primary: Color, val dim: Color, val onPrimary: Color)
val LocalAccentColors = compositionLocalOf { SageAccent }
```

All components already use `SpineColors.AccentPrimary` — change them to `LocalAccentColors.current.primary`. (This is a targeted find-replace in `core/designsystem/` only; feature modules use MaterialTheme.colorScheme mappings.)

**App root** (`StashAppContent`) collects `uiPrefs.accentPalette` and passes it to `StashTheme`. Live recomposition on palette change (no restart needed).

### Decision 6 — Plan Wave Structure
**D-06:** 3 plans in a 2-wave cascade:

- **Plan 6.1** (wave 1, autonomous): Hub foundation
  - SETTINGS-01 (hub layout), SETTINGS-02 (server card), SETTINGS-03 (sub-routes), SETTINGS-04 (CSlider), SETTINGS-05 (DRow)
  - All new components + nav wiring must land before detail pages can compile

- **Plan 6.2** (wave 2, autonomous, depends_on 6.1): Detail pages
  - SETTINGS-06 (Playback), SETTINGS-07 (Codecs), SETTINGS-08 (Display + palette), SETTINGS-09 (Library), SETTINGS-10 (Server + About)

- **Plan 6.3** (wave 2, autonomous, depends_on 6.1, parallel with 6.2): Search overlay
  - SETTINGS-11 (search overlay + static index + smoke test updates)

Plans 6.2 and 6.3 are parallel-safe: 6.2 creates new files (`Settings*Screen.kt`), 6.3 adds a composable to the hub and creates a search overlay composable.

### Decision 7 — Settings Search Static Index
**D-07:** `SettingsSearchIndex` — a `List<SettingsSearchEntry>` defined as a top-level `val` in a new `SettingsSearch.kt` file inside `feature/settings/`. Each entry:
```kotlin
data class SettingsSearchEntry(
    val label: String,
    val hint: String,
    val breadcrumb: String,   // e.g. "Playback · Resume & skip"
    val route: String,        // Routes.SettingsPlayback etc.
)
```

Filtering: `query.isNotBlank() && (entry.label + entry.hint).contains(query, ignoreCase = true)`.

The `ViewModel` exposes `val searchQuery: MutableStateFlow<String>` and `val searchResults: StateFlow<List<SettingsSearchEntry>>` derived via `combine`.

### Decision 8 — Palette Picker in Display Page
**D-08:** 3-column `LazyVerticalGrid` (or just a `Row` since there are only 3 items) of swatch cards at the top of the Theme `DetailGroup`. Each card:
- `padding(10.dp)`, `ShapeSmall`, `accent@8%-bg + AccentPrimary border` for active / `Bg + Border` for inactive
- 28×28dp colored square swatch (the accent color, ShapeSmall 6dp)
- Name (SpaceGrotesk 11sp W600), description MonoSmall below
- Active: 14dp AccentPrimary check mark in top-right corner

Tap → `viewModel.setAccentPalette("ember")` → StateFlow updates → `StashTheme` recomposes with new accent.

### Decision 9 — Commit Budget
**D-09:**
- Plan 6.1: ≤ 5 commits (sub-routes, CSlider, DRow, hub layout, server card)
- Plan 6.2: ≤ 6 commits (2 pages per commit; palette picker in Display)
- Plan 6.3: ≤ 3 commits (search index, overlay composable, smoke tests)
- Total: ≤ 14 commits

### Decision 10 — Test Coverage
**D-10:** Update Phase 4 smoke tests for `SettingsScreen` — the composable signature now takes additional callbacks for detail page navigation (`onPlaybackClick: () -> Unit`, etc.). Update the `SettingsScreenSmokeTest.kt` call site to pass no-op lambdas. No new test files needed for this phase.

### Claude's Discretion
- Exact `DetailGroup` container layout (Column with Surface bg + ShapeSmall + overflow clip — same as Phase 5 SettingsScreen section containers)
- Whether `SettingsCodecsScreen` uses `LazyColumn` or `Column` inside a `ScrollableColumn` (prefer `LazyColumn` for consistency)
- Latency stub value in server card ("< 10ms" hardcoded vs omit entirely — omit for v1 to avoid false info)
- `TestBtn` styling (outlined button, Border border, SpaceGrotesk 12sp — same as secondary action buttons elsewhere)

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase 6 Design Spec
- `.planning/phases/06-settings-redesign/06-SPEC.md` — 11 locked requirements (MANDATORY)
- `design_handoff_slopper_spine/README.md` — §6 SettingsScreen v3.0 (MANDATORY — primary design source)
- `design_handoff_slopper_spine/spine-settings.jsx` — component source (NEW in v3.0)

### Prior Phase Constraints
- `.planning/phases/05-spine-compose-ui-redesign/05-CONTEXT.md` — Spine tokens, SpaceGrotesk, MetaMono, ShapeSmall/Medium/Large
- `.planning/phases/04-polish-test-pyramid/04-CONTEXT.md` — POLISH-06 PlayerSettings/UiSettings interfaces

### Key Files to Read Before Planning
- `feature/settings/src/main/java/io/stashapp/android/feature/settings/SettingsScreen.kt` — current 579-line single-screen (to be replaced)
- `core/data/src/main/java/io/stashapp/android/core/data/prefs/UiPreferences.kt` — add `accentPalette`
- `core/domain/src/main/java/io/stashapp/android/core/domain/ConnectionRepository.kt` — `activeServer()` for server card
- `core/ui/src/main/java/io/stashapp/android/core/ui/nav/Routes.kt` — add 6 new sub-routes
- `app/src/main/java/io/stashapp/android/MainActivity.kt` — register 6 new composable routes + update `tabNavigate`
- `core/designsystem/src/main/java/io/stashapp/android/core/designsystem/theme/Theme.kt` — add `LocalAccentColors` + palette param

</canonical_refs>

<code_context>
## Codebase Context

### Current SettingsScreen call site in MainActivity
```kotlin
composable(Routes.Settings) {
    SettingsScreen(
        onBack = { navController.popBackStack() },
        onDisconnected = {
            navController.navigate(Routes.Connection) { popUpTo(0) { inclusive = true } }
        },
        onBrowsePerformers = { navController.navigate(Routes.browse("performers")) },
    )
}
```
After Phase 6, `SettingsScreen` gets additional callbacks:
`onPlaybackClick`, `onCodecsClick`, `onDisplayClick`, `onLibraryClick`, `onServerClick`, `onAboutClick`

### Accent palette data classes (new)
```kotlin
data class AccentColors(val primary: Color, val dim: Color, val onPrimary: Color)
val SageAccent   = AccentColors(Color(0xFF9DC83C), Color(0xFF6E9028), Color(0xFF0B1402))
val EmberAccent  = AccentColors(Color(0xFFE5A742), Color(0xFFB07B25), Color(0xFF1A0F00))
val SignalAccent = AccentColors(Color(0xFF4FD0E6), Color(0xFF2A9DB0), Color(0xFF001218))
fun accentForName(name: String) = when(name) {
    "ember"  -> EmberAccent
    "signal" -> SignalAccent
    else     -> SageAccent  // "sage" default
}
```

### Phase 5 component pattern (follow for CSlider/DRow)
- `core/designsystem/component/SpineResumeCard.kt` — reference for new design system component structure
- `feature/player/src/main/java/.../PlayerTimeline.kt` — `ChapterStrip` Canvas pattern

### Settings currently has NO separate ViewModel file
`SettingsViewModel` is defined inside `SettingsScreen.kt` (lines ~65–95). Will be split to a separate `SettingsViewModel.kt` file in Plan 6.1 as part of the hub refactor.
</code_context>
