# Phase 6: SETTINGS-V3 — Hub + Drill-Down Settings Redesign — Research

**Researched:** 2026-05-19
**Domain:** Jetpack Compose settings architecture, sub-navigation, DataStore prefs, Material 3 theme composition locals
**Confidence:** HIGH (all findings verified from live source files)

---

## Summary

Phase 6 replaces the existing 579-line `SettingsScreen.kt` (single scroll, all settings in one view) with a hub + drill-down architecture: a landing hub, six detail pages, two new design-system components (`CSlider`, `DRow`), and a settings search overlay. Every file that must change has been read; every interface that will be consumed has been verified. No assumptions about existing APIs are needed — they are fully documented below.

The existing `SettingsViewModel` class (lines 68–90 of `SettingsScreen.kt`) must be extracted to a new `SettingsViewModel.kt` file and expanded with `ConnectionRepository` state (server info, active server) and accent palette state. `Theme.kt` must gain a `LocalAccentColors` `CompositionLocal` and an `accentName` parameter on `StashTheme`. `Routes.kt` gains 6 new constants. `MainActivity.kt` gains 6 new `composable()` registrations and an updated `isMainTabRoute` check.

**Primary recommendation:** Extract VM first (Plan 6.1 Task 1), then wire nav (Task 2), then build components (Tasks 3–5), then detail pages (Plan 6.2), then search (Plan 6.3). Every detail page can read settings directly via `collectAsStateWithLifecycle` from the injected `PlayerPreferences`/`UiPreferences` — no VM state duplication needed beyond mutations.

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

- **D-01 ViewModel Architecture:** Single `SettingsViewModel` shared across hub and all detail pages. `hiltViewModel()` scoped per `NavBackStackEntry`. Additions: inject `ConnectionRepository` + `EndpointStateHolder`; add `serverInfo: StateFlow<ServerInfo?>`, `activeServer: StateFlow<StashServer?>`, `accentPalette: StateFlow<String>`, `fun setAccentPalette(palette: String)`. Existing `setPlayer`/`setUi` helpers stay.
- **D-02 Sub-Navigation Pattern:** Flat `composable()` entries in `AppNavHost`. No nested NavHost. Six new `Routes.kt` constants: `SettingsPlayback`, `SettingsCodecs`, `SettingsDisplay`, `SettingsLibrary`, `SettingsServer`, `SettingsAbout`. `Routes.Settings = "settings"` unchanged.
- **D-03 CSlider and DRow Placement:** Both in `core/designsystem/component/`. Pure UI components with no domain knowledge.
- **D-04 Server Status Card Data:** `SettingsViewModel` fetches `ServerInfo` via `connectionRepository.test(activeServer)` on init. Stored as `StateFlow<ServerInfo?>` (null = loading/unavailable). Fields displayed: endpoint URL, version, sceneCount, latency stub omitted for v1.
- **D-05 Accent Palette Switching:** `UiPreferences` gets `val accentPalette: Flow<String>` (default "sage") + `suspend fun setAccentPalette(name: String)`. `StashTheme` gains `accentName: String` param. New `LocalAccentColors: ProvidableCompositionLocal<AccentColors>`. App root collects and passes to `StashTheme`.
- **D-06 Plan Wave Structure:** Plan 6.1 (wave 1): hub, server card, sub-routes, CSlider, DRow. Plan 6.2 (wave 2, parallel): 6 detail pages. Plan 6.3 (wave 2, parallel): search overlay.
- **D-07 Settings Search Static Index:** `SettingsSearchIndex` as top-level `val List<SettingsSearchEntry>` in new `SettingsSearch.kt`. `SettingsSearchEntry(label, hint, breadcrumb, route)`. VM exposes `searchQuery: MutableStateFlow<String>` + `searchResults: StateFlow<List<SettingsSearchEntry>>` via `combine`.
- **D-08 Palette Picker in Display Page:** 3-item `Row` of swatch cards at top of Theme `DetailGroup`. Active: accent-8%-bg + AccentPrimary border + 14dp check. Inactive: Bg + Border.
- **D-09 Commit Budget:** Plan 6.1 ≤ 5, Plan 6.2 ≤ 6, Plan 6.3 ≤ 3. Total ≤ 14.
- **D-10 Test Coverage:** Update existing `SettingsScreenSmokeTest.kt` call site with new no-op lambdas. No new test files.

### Claude's Discretion

- Exact `DetailGroup` container layout (Column with Surface bg + ShapeSmall + overflow clip)
- Whether `SettingsCodecsScreen` uses `LazyColumn` or `Column` inside `ScrollableColumn`
- Latency stub value: omit entirely for v1 (decided: omit to avoid false info)
- `TestBtn` styling (outlined button, Border border, SpaceGrotesk 12sp)

### Deferred Ideas (OUT OF SCOPE)

- ConnectionScreen changes
- PlayerScreen changes
- Cast/Connect implementation
- Downloads beta (hidden behind `if (false)`)
- Real codec test execution
- New GraphQL operations for server stats
</user_constraints>

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| SETTINGS-01 | Hub landing replaces `SettingsScreen` with `LazyColumn`, server card, search field, `HubGroup`/`HubRow` | A1: current composable structure fully documented; hub replaces lines 92–323 |
| SETTINGS-02 | Server status card reads from `ConnectionRepository`/`EndpointStateHolder` | A7: `activeServer()` returns `Flow<StashServer?>`, `test()` returns `AppResult<ServerInfo>`; `ServerInfo` fields documented |
| SETTINGS-03 | Six sub-routes added to `Routes.kt` + `AppNavHost` registrations | A5: `Routes.kt` has no sub-routes today; `tabNavigate` and `isMainTabRoute` patterns documented; exact insertion points identified |
| SETTINGS-04 | `CSlider` component in `core/designsystem/component/` | A6: `ResumeCard.kt` pattern documented; package, file structure, import style confirmed |
| SETTINGS-05 | `DRow` component in `core/designsystem/component/` | A6: same pattern as CSlider |
| SETTINGS-06 | `SettingsPlaybackScreen` at `settings/playback` | A2: all `PlayerSettings` flows and setters documented; defaults from `PlayerPreferences.companion` documented |
| SETTINGS-07 | `SettingsCodecsScreen` at `settings/codecs` | A2: `decoderPreference`, `videoBufferPreset` flows confirmed in `PlayerSettings` |
| SETTINGS-08 | `SettingsDisplayScreen` at `settings/display` with palette picker | A3: `UiPreferences` structure documented; `accentPalette` addition pattern confirmed; A4: `StashTheme` injection point confirmed |
| SETTINGS-09 | `SettingsLibraryScreen` at `settings/library` | A2/A3: `imageCacheSizeMb`, `activityTracking`, `gridColumns` etc. all confirmed |
| SETTINGS-10 | `SettingsServerScreen` + `SettingsAboutScreen` | A7: `ServerInfo` fields (`version`, `sceneCount`, `performerCount`, `studioCount`, `tagCount`) confirmed |
| SETTINGS-11 | Settings search overlay with static index | D-07: static `List<SettingsSearchEntry>` approach; VM `searchQuery`/`searchResults` flows |
</phase_requirements>

---

## A1 — Current SettingsScreen Full Structure

**File:** `feature/settings/src/main/java/io/stashapp/android/feature/settings/SettingsScreen.kt`
**Total lines:** 579 (SPEC says 579 — confirmed 580 lines including trailing newline)

### Embedded SettingsViewModel (lines 68–90)

```kotlin
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val connectionRepository: ConnectionRepository,  // already injected
    val playerPrefs: PlayerPreferences,
    val uiPrefs: UiPreferences,
) : ViewModel() {
    fun disconnect(onDone: () -> Unit) { viewModelScope.launch { connectionRepository.disconnect(); onDone() } }
    fun <T> setPlayer(setter: suspend PlayerPreferences.() -> Unit) { viewModelScope.launch { playerPrefs.setter() } }
    fun <T> setUi(setter: suspend UiPreferences.() -> Unit) { viewModelScope.launch { uiPrefs.setter() } }
}
```

**Key observations:**
- `ConnectionRepository` is already injected — no new Hilt module needed for D-01 additions
- `playerPrefs` and `uiPrefs` are `val` (public) — composables access them directly via `collectAsStateWithLifecycle`
- `setPlayer`/`setUi` are generic HOFs — existing call sites do `viewModel.setPlayer { setSeekMsPerPx(it) }`
- No `StateFlow` properties today — all state is collected in the composable

### SettingsScreen Composable Signature (lines 93–101)

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onDisconnected: () -> Unit,
    onBrowsePerformers: () -> Unit = {},
    onBrowseStudios: () -> Unit = {},
    onBrowseTags: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
)
```

**After Phase 6**, the signature gains 6 more callbacks:
`onPlaybackClick`, `onCodecsClick`, `onDisplayClick`, `onLibraryClick`, `onServerClick`, `onAboutClick`

The `onBrowsePerformers`/`onBrowseStudios`/`onBrowseTags` callbacks are **removed** — they belonged to the old flat layout which had direct browse links. The hub does not link to browse.

### All Preferences Currently Collected

| Preference | Source | Default |
|-----------|--------|---------|
| `seekMsPerPx` | `PlayerPreferences` | `120f` |
| `doubleTapSeekSeconds` | `PlayerPreferences` | `10` |
| `defaultPlaybackSpeed` | `PlayerPreferences` | `1.0f` |
| `autoPlayNext` | `PlayerPreferences` | `true` |
| `resumeThresholdSeconds` | `PlayerPreferences` | `2` |
| `completionThresholdPercent` | `PlayerPreferences` | `85` |
| `skipIntroSeconds` | `PlayerPreferences` | `0` |
| `videoBufferPreset` | `PlayerPreferences` | `"medium"` |
| `defaultAspectRatio` | `PlayerPreferences` | `"fit"` |
| `decoderPreference` | `PlayerPreferences` | `"auto"` |
| `imageCacheSizeMb` | `UiPreferences` | `256` |
| `gridColumns` | `UiPreferences` | `"auto"` |
| `amoledBlackMode` | `UiPreferences` | `false` |
| `showRatingOnCards` | `UiPreferences` | `true` |
| `showPlayCountOnCards` | `UiPreferences` | `true` |
| `showResolutionOnCards` | `UiPreferences` | `true` |
| `activityTracking` | `UiPreferences` | `true` |
| `autoRotatePlayer` | `UiPreferences` | `true` |

### Setting Groups in Current Layout

1. **App** (line 143): `SectionContainer { LanguageRow() }`
2. **Player** (line 153): 10 rows — SliderPref × 5, ChipRowPref × 4, SwitchPref × 1
3. **Cache** (line 232): `SliderPref` for `imageCacheSizeMb` + standalone `OutlinedButton` "Clear image cache"
4. **Display** (line 259): ChipRowPref (gridColumns) + SwitchPref × 4
5. **App behavior** (line 289): SwitchPref × 2 (activityTracking, autoRotate)
6. **Account** (line 303): `SettingRow` "Disconnect server" → `viewModel.disconnect(onDisconnected)`

### Private Composable Functions (lines 326–579)

| Function | Lines | Purpose |
|----------|-------|---------|
| `SectionHeader` | 328–335 | MetaMono uppercase section label |
| `SectionContainer` | 338–351 | Surface + ShapeSmall + Column wrapper |
| `SettingRow` | 354–370 | Tappable key-only row (no value) |
| `LanguageRow` | 386–424 | API 33+ per-app language intent row |
| `SwitchPref` | 427–469 | Label + subtitle + Spine-styled Switch |
| `SliderPref` | 472–506 | Title + value label + M3 Slider (the OLD dot-row slider being replaced by `CSlider`) |
| `ChipRowPref` | 509–543 | Title + scrolling chip row |
| `CodecStatusCard` | 545–579 | Reads `CodecCapabilities.ffmpegExtensionUsable/Present` — moves to Codecs detail page |

### What Moves, What Disappears

| Current element | Destination in Phase 6 |
|----------------|------------------------|
| `SectionHeader` | **Replaced** by `DetailGroup` header pattern |
| `SectionContainer` | **Replaced** by `DetailGroup` component |
| `SettingRow` | **Replaced** by `DRow` |
| `SliderPref` | **Replaced** by `CSlider` inside `DRowStacked` |
| `ChipRowPref` | **Kept** (chips stay, just inside detail pages) |
| `SwitchPref` | **Kept** as `DRow` trailing widget |
| `CodecStatusCard` | **Moves** to `SettingsCodecsScreen` as capability banner |
| `LanguageRow` | **Stays** in hub under "App" `HubGroup` or the Nav bar row |

---

## A2 — PlayerSettings / UiSettings Interfaces

### PlayerSettings (`core/domain/.../PlayerSettings.kt`) — 42 lines

**All Flow properties:**

| Property | Type | Default (from PlayerPreferences companion) |
|----------|------|--------------------------------------------|
| `seekMsPerPx` | `Flow<Float>` | `120f` (range 20–500 ms/px) |
| `doubleTapSeekSeconds` | `Flow<Int>` | `10` (range 5–60) |
| `defaultPlaybackSpeed` | `Flow<Float>` | `1.0f` |
| `autoPlayNext` | `Flow<Boolean>` | `true` |
| `resumeThresholdSeconds` | `Flow<Int>` | `2` (range 0–30) |
| `completionThresholdPercent` | `Flow<Int>` | `85` (range 50–100) |
| `skipIntroSeconds` | `Flow<Int>` | `0` (range 0–120; "Off" when 0) |
| `videoBufferPreset` | `Flow<String>` | `"medium"` (`"small"/"medium"/"large"`) |
| `defaultAspectRatio` | `Flow<String>` | `"fit"` (`"fit"/"crop"/"stretch"`) |
| `decoderPreference` | `Flow<String>` | `"auto"` (`"auto"/"prefer_hw"/"prefer_sw"`) |

**All suspend setters:** `setSeekMsPerPx`, `setDoubleTapSeekSeconds`, `setDefaultPlaybackSpeed`, `setAutoPlayNext`, `setResumeThresholdSeconds`, `setCompletionThresholdPercent`, `setSkipIntroSeconds`, `setVideoBufferPreset`, `setDefaultAspectRatio`, `setDecoderPreference`

**Companion constants in PlayerPreferences (not on the interface, but accessible via `playerPrefs.`):**

| Constant | Value |
|----------|-------|
| `SEEK_MS_PER_PX_MIN` | `20f` |
| `SEEK_MS_PER_PX_MAX` | `500f` |
| `DOUBLE_TAP_SEEK_MIN` | `5` |
| `DOUBLE_TAP_SEEK_MAX` | `60` |
| `DEFAULT_RESUME_THRESHOLD` | `2` |
| `DEFAULT_COMPLETION_THRESHOLD` | `85` |
| `DEFAULT_SKIP_INTRO` | `0` |
| `DEFAULT_BUFFER_PRESET` | `"medium"` |
| `DEFAULT_ASPECT_RATIO` | `"fit"` |
| `DEFAULT_DECODER_PREF` | `"auto"` |

**Gap for Phase 6:** `PlayerSettings` interface has **no properties** for: `showChapterThumbnails`, `lockControlsOnIdle`, `showCodecBadge`, `showQueuePosition`, `hapticsOnSeek`, `hdrPassthrough`, `matchRefreshRate`, `matchResolution`, `fallbackOnDecoderError`, `tunneling`, `preBufferOnHover`. These are all referenced in the SPEC (SETTINGS-06, SETTINGS-07). They are **not yet in the interface or `PlayerPreferences`**. Plan 6.2 must either:
  - Add them to `PlayerSettings` interface + `PlayerPreferences` implementation, **or**
  - Use a `rememberSaveable` stub that does not persist (not acceptable for a settings page)

**Decision required by planner:** Add the missing boolean prefs to `PlayerSettings` + `PlayerPreferences`. This is in scope per the SPEC (Phase 6 owns the settings pages) and does not conflict with any locked decision.

### UiSettings (`core/domain/.../UiSettings.kt`) — 36 lines

**All Flow properties:**

| Property | Type | Default |
|----------|------|---------|
| `bottomNavVisibleIds` | `Flow<List<String>>` | `["home","scenes","browse","settings"]` |
| `defaultSceneFilter` | `Flow<SceneFilter?>` | `null` |
| `imageCacheSizeMb` | `Flow<Int>` | `256` |
| `gridColumns` | `Flow<String>` | `"auto"` |
| `amoledBlackMode` | `Flow<Boolean>` | `false` |
| `showRatingOnCards` | `Flow<Boolean>` | `true` |
| `showPlayCountOnCards` | `Flow<Boolean>` | `true` |
| `showResolutionOnCards` | `Flow<Boolean>` | `true` |
| `activityTracking` | `Flow<Boolean>` | `true` |
| `autoRotatePlayer` | `Flow<Boolean>` | `true` |

**Gap for Phase 6:** `UiSettings` interface has **no properties** for: `accentPalette`, `reduceMotion`, `cardDensity`, `longPressBehavior`, `showResumeBar`, `showStudioCaption`, `showChapterStrip`, `tapToPeekInfo`, `syncRatings`, `syncOCounter`, `syncMarkers`, `cacheDuration`, `keepWatchHistory`, `historyOnHome`, `smartRails`. These are all referenced in SETTINGS-08 and SETTINGS-09. Same gap as PlayerSettings — Plan 6.2 must add them.

**`accentPalette` specifically (D-05):** Add to both `UiSettings` interface and `UiPreferences` implementation:
```kotlin
// UiSettings interface addition:
val accentPalette: Flow<String>
suspend fun setAccentPalette(name: String)

// UiPreferences companion addition:
private val KEY_ACCENT_PALETTE = stringPreferencesKey("accent_palette")
const val DEFAULT_ACCENT_PALETTE = "sage"
```

---

## A3 — UiPreferences Structure and accentPalette Addition

**File:** `core/data/src/main/java/io/stashapp/android/core/data/prefs/UiPreferences.kt`
**Total lines:** 231
**DataStore name:** `"ui_prefs"` (line 27: `by preferencesDataStore(name = "ui_prefs")`)

### Current Keys (companion object, lines 134–150)

| Constant | Key string | Type |
|----------|-----------|------|
| `KEY_NAV_VISIBLE` | `"bottom_nav_visible"` | `stringPreferencesKey` |
| `KEY_DEFAULT_FILTER` | `"default_scene_filter"` | `stringPreferencesKey` |
| `KEY_IMAGE_CACHE_MB` | `"image_cache_mb"` | `intPreferencesKey` |
| `KEY_GRID_COLUMNS` | `"grid_columns"` | `stringPreferencesKey` |
| `KEY_AMOLED` | `"amoled_black"` | `booleanPreferencesKey` |
| `KEY_SHOW_RATING` | `"card_show_rating"` | `booleanPreferencesKey` |
| `KEY_SHOW_PLAY_COUNT` | `"card_show_play_count"` | `booleanPreferencesKey` |
| `KEY_SHOW_RESOLUTION` | `"card_show_resolution"` | `booleanPreferencesKey` |
| `KEY_ACTIVITY_TRACKING` | `"activity_tracking"` | `booleanPreferencesKey` |
| `KEY_AUTO_ROTATE` | `"auto_rotate_player"` | `booleanPreferencesKey` |

**Defaults in companion:**
- `DefaultVisible = listOf("home", "scenes", "browse", "settings")`
- `DEFAULT_IMAGE_CACHE_MB = 256`
- `DEFAULT_GRID_COLUMNS = "auto"`
- `DEFAULT_AMOLED = false`

### How to Add accentPalette (D-05)

The file uses two private generic helpers (lines 121–131):
```kotlin
private fun <T> flow(key: Preferences.Key<T>, default: T): Flow<T> =
    context.uiDataStore.data.map { it[key] ?: default }

private suspend fun <T> put(key: Preferences.Key<T>, value: T) {
    context.uiDataStore.edit { it[key] = value }
}
```

Add to the `// ---- Display` block (after line 95):
```kotlin
override val accentPalette: Flow<String> = flow(KEY_ACCENT_PALETTE, DEFAULT_ACCENT_PALETTE)
override suspend fun setAccentPalette(name: String) = put(KEY_ACCENT_PALETTE, name)
```

Add to companion (after `KEY_AUTO_ROTATE`):
```kotlin
private val KEY_ACCENT_PALETTE = stringPreferencesKey("accent_palette")
const val DEFAULT_ACCENT_PALETTE = "sage"
```

Also add `accentPalette: Flow<String>` and `suspend fun setAccentPalette(name: String)` to the `UiSettings` interface in `core/domain`.

**No migration needed** — DataStore returns the default when the key is absent, so existing installs will read `"sage"` on first launch after upgrade.

---

## A4 — Theme.kt for LocalAccentColors Addition

**File:** `core/designsystem/src/main/java/io/stashapp/android/core/designsystem/theme/Theme.kt`
**Total lines:** 57

### Current Structure

```
Lines 1–9:   package + imports
Lines 11–16: Shape token vals (ShapeSmall/Medium/Large/Circle)
Lines 20–48: StashDarkColorScheme — darkColorScheme() with all SpineColors.* mappings
Lines 50–57: StashTheme(@Composable fun) → MaterialTheme(colorScheme, typography, content)
```

### StashDarkColorScheme — Relevant Accent Mappings (lines 20–48)

| M3 slot | SpineColors value |
|---------|------------------|
| `primary` | `AccentPrimary` |
| `onPrimary` | `AccentOnPrimary` |
| `primaryContainer` | `AccentPrimaryDim` |
| `onPrimaryContainer` | `AccentOnPrimary` |
| `secondary` | `AccentCool` |
| `tertiary` | `AccentCool` |

These M3 slots will need to remain in sync with the active `LocalAccentColors` palette when switching accents. The `primary`/`onPrimary`/`primaryContainer`/`onPrimaryContainer` slots are the ones that must be dynamic.

### Current StashTheme Signature (lines 51–57)

```kotlin
@Composable
fun StashTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = StashDarkColorScheme,
        typography = StashTypography,
        content = content,
    )
}
```

### How to Modify for D-05

The color scheme is currently a top-level `private val` (static). To support dynamic palettes it must become a local computation inside `StashTheme`:

```kotlin
// New data structures (add to Theme.kt or a new AccentColors.kt):
data class AccentColors(val primary: Color, val dim: Color, val onPrimary: Color)
val SageAccent   = AccentColors(Color(0xFF9DC83C), Color(0xFF6E9028), Color(0xFF0B1402))
val EmberAccent  = AccentColors(Color(0xFFE5A742), Color(0xFFB07B25), Color(0xFF1A0F00))
val SignalAccent = AccentColors(Color(0xFF4FD0E6), Color(0xFF2A9DB0), Color(0xFF001218))
fun accentForName(name: String): AccentColors = when(name) {
    "ember"  -> EmberAccent
    "signal" -> SignalAccent
    else     -> SageAccent
}
val LocalAccentColors = compositionLocalOf { SageAccent }

// Updated StashTheme signature:
@Composable
fun StashTheme(
    accentName: String = "sage",
    content: @Composable () -> Unit
) {
    val accent = accentForName(accentName)
    val colorScheme = StashDarkColorScheme.copy(
        primary = accent.primary,
        onPrimary = accent.onPrimary,
        primaryContainer = accent.dim,
        onPrimaryContainer = accent.onPrimary,
    )
    CompositionLocalProvider(LocalAccentColors provides accent) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = StashTypography,
            content = content,
        )
    }
}
```

### App Root Wiring in MainActivity (lines 105–110)

Current call site:
```kotlin
setContent {
    StashTheme {
        val rootViewModel: RootViewModel = hiltViewModel()
        StashAppContent(rootViewModel = rootViewModel, appReady = appReady)
    }
}
```

After Phase 6, `StashAppContent` must collect `uiPrefs.accentPalette` and pass it up. The cleanest approach is to have `RootViewModel` expose an `accentPalette: StateFlow<String>` (inject `UiPreferences` into `RootViewModel`, which already injects it at line 71), then collect it in `StashAppContent` before passing to `StashTheme`:

```kotlin
setContent {
    val rootViewModel: RootViewModel = hiltViewModel()
    val accentName by rootViewModel.accentPalette.collectAsState()
    StashTheme(accentName = accentName) {
        StashAppContent(rootViewModel = rootViewModel, appReady = appReady)
    }
}
```

**Note:** `hiltViewModel()` inside `setContent` but outside a `Composable` function boundary is valid in `ComponentActivity.setContent` — this is the existing pattern for `RootViewModel` at line 107.

**Components using `SpineColors.AccentPrimary` directly** (in `core/designsystem/`) will continue to work via `MaterialTheme.colorScheme.primary` if components are updated to use `LocalAccentColors.current.primary` instead. The D-05 decision specifies a targeted find-replace in `core/designsystem/` only — feature modules use `MaterialTheme.colorScheme` mappings.

---

## A5 — Routes.kt and AppNavHost Wiring

### Current Routes.kt (`core/ui/src/main/java/io/stashapp/android/core/ui/nav/Routes.kt`) — 44 lines

Current routes as `object Routes` constants:
- `Connection = "connection"`
- `Settings = "settings"`
- `Home = "home"`
- `LibraryPattern = "library?preset={preset}"` + `Library = "library"` + `fun libraryWithPreset(...)`
- `DetailPattern = "scene/{sceneId}"` + `fun sceneDetail(...)`
- `BrowsePattern = "browse/{kind}"` + `fun browse(...)`
- `PlayerPattern = "player/{sceneId}?queueIds=...&index=...&startMs=..."` + `fun player(...)`

**No sub-routes for settings exist today.** Six new constants are needed (D-02):
```kotlin
const val SettingsPlayback = "settings/playback"
const val SettingsCodecs   = "settings/codecs"
const val SettingsDisplay  = "settings/display"
const val SettingsLibrary  = "settings/library"
const val SettingsServer   = "settings/server"
const val SettingsAbout    = "settings/about"
```

Place after `const val Settings = "settings"` (line 9).

### tabNavigate Function (MainActivity.kt lines 298–311)

```kotlin
private fun NavHostController.tabNavigate(route: String, startDestination: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { inclusive = false }
    }
}
```

**Key behavior:** `popUpTo(graph.findStartDestination().id)` clears back stack to the graph root (Home or Connection). This is the correct behavior for top-level tab switches.

**Detail sub-routes must NOT use `tabNavigate`** — they should use plain `navController.navigate(Routes.SettingsPlayback)`. Detail pages pop back to hub via standard `popBackStack()`. The hub itself remains on the back stack and receives standard back presses.

### isMainTabRoute (lines 160–168)

```kotlin
private fun isMainTabRoute(route: String?): Boolean = when {
    route == null -> false
    route == Routes.Home -> true
    route.startsWith("library") -> true
    route.startsWith("browse/") -> true
    route == Routes.Settings -> true
    else -> false
}
```

**Sub-routes must NOT match `isMainTabRoute`** — the bottom nav should be hidden on detail pages. Since `"settings/playback".startsWith("settings")` would be true if we added a check for `route.startsWith("settings")`, the check must remain as `route == Routes.Settings` (exact match). No change needed to `isMainTabRoute`.

### Current Settings composable registration (lines 427–439)

```kotlin
composable(Routes.Settings) {
    SettingsScreen(
        onBack = { navController.popBackStack() },
        onDisconnected = {
            navController.navigate(Routes.Connection) { popUpTo(0) { inclusive = true } }
        },
        onBrowsePerformers = { navController.navigate(Routes.browse("performers")) },
        onBrowseStudios = { navController.navigate(Routes.browse("studios")) },
        onBrowseTags = { navController.navigate(Routes.browse("tags")) },
    )
}
```

**After Phase 6**, replace with:
```kotlin
composable(Routes.Settings) {
    SettingsScreen(
        onBack = { navController.popBackStack() },
        onDisconnected = {
            navController.navigate(Routes.Connection) { popUpTo(0) { inclusive = true } }
        },
        onPlaybackClick = { navController.navigate(Routes.SettingsPlayback) },
        onCodecsClick   = { navController.navigate(Routes.SettingsCodecs) },
        onDisplayClick  = { navController.navigate(Routes.SettingsDisplay) },
        onLibraryClick  = { navController.navigate(Routes.SettingsLibrary) },
        onServerClick   = { navController.navigate(Routes.SettingsServer) },
        onAboutClick    = { navController.navigate(Routes.SettingsAbout) },
    )
}
```

Six new `composable()` entries are added after the existing Settings registration (before the closing `}` of `NavHost`):
```kotlin
composable(Routes.SettingsPlayback) {
    SettingsPlaybackScreen(onBack = { navController.popBackStack() })
}
composable(Routes.SettingsCodecs) {
    SettingsCodecsScreen(onBack = { navController.popBackStack() })
}
composable(Routes.SettingsDisplay) {
    SettingsDisplayScreen(onBack = { navController.popBackStack() })
}
composable(Routes.SettingsLibrary) {
    SettingsLibraryScreen(onBack = { navController.popBackStack() })
}
composable(Routes.SettingsServer) {
    SettingsServerScreen(
        onBack = { navController.popBackStack() },
        onDisconnected = {
            navController.navigate(Routes.Connection) { popUpTo(0) { inclusive = true } }
        },
    )
}
composable(Routes.SettingsAbout) {
    SettingsAboutScreen(onBack = { navController.popBackStack() })
}
```

`AppNavHost` ends at line 441. Insert before the closing `}` of the `NavHost { }` block.

---

## A6 — Component Placement Pattern (ResumeCard.kt template)

**Actual file path:** `core/designsystem/src/main/java/io/stashapp/android/core/designsystem/component/ResumeCard.kt`
(Note: CONTEXT.md references it as `SpineResumeCard.kt` but the actual filename is `ResumeCard.kt`. The composable function inside is `SpineResumeCard`.)

**All designsystem component files:**
```
core/designsystem/src/main/java/io/stashapp/android/core/designsystem/component/
├── ResumeCard.kt       (174 lines — template for CSlider.kt and DRow.kt)
└── SceneCard.kt
```

### File Structure Pattern from ResumeCard.kt

```
Line 1:     package io.stashapp.android.core.designsystem.component
Lines 2–36: imports (androidx.compose.*, coil3.compose.*, io.stashapp.android.core.designsystem.theme.*)
Lines 41–46: KDoc comment explaining component purpose and usage
Lines 47–55: @Composable fun SpineResumeCard(params...) — public function, no @Preview
Lines 56–169: implementation
Lines 171–173: private extension val (local sp helper)
```

**Key observations:**
- Package: `io.stashapp.android.core.designsystem.component`
- Public composable function (no `internal` — available to all feature modules)
- No `@Preview` annotation in this file (previews can be in a separate preview file or omitted)
- Imports from `io.stashapp.android.core.designsystem.theme.*` (SpineColors, MetaMono, ShapeMedium etc.)
- No Hilt / ViewModel — pure stateless composable, takes all data as parameters
- Uses `SpineColors.*` directly (not `MaterialTheme.colorScheme.*`) for Spine token colors

### CSlider.kt Template

```kotlin
package io.stashapp.android.core.designsystem.component

// imports: androidx.compose.foundation.*, androidx.compose.material3.Slider + SliderDefaults,
//          io.stashapp.android.core.designsystem.theme.SpineColors + JetBrainsMono + ShapeSmall

/**
 * Compact slider — track (flex 1, 3dp height) + 60dp value bubble.
 * Replaces the dot-row SliderPref used in the old SettingsScreen.
 */
@Composable
fun CSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    modifier: Modifier = Modifier,
    steps: Int = 0,
)
```

### DRow.kt Template

```kotlin
package io.stashapp.android.core.designsystem.component

/**
 * Detail-page row primitive.
 * Inline mode: label + optional hint (flex 1) + trailing widget.
 * Stacked mode: label + hint on top; full-width body widget below.
 */
@Composable
fun DRow(
    label: String,
    hint: String? = null,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
)

@Composable
fun DRowStacked(
    label: String,
    hint: String? = null,
    modifier: Modifier = Modifier,
    body: @Composable () -> Unit,
)
```

---

## A7 — ConnectionRepository and ServerInfo

### ConnectionRepository (`core/domain/.../ConnectionRepository.kt`) — 18 lines

```kotlin
interface ConnectionRepository {
    fun activeServer(): Flow<StashServer?>
    suspend fun test(server: StashServer): AppResult<ServerInfo>
    suspend fun setActive(server: StashServer)
    suspend fun disconnect()
}
```

**`activeServer()` return type:** `Flow<StashServer?>` — emits `null` when no server is configured, emits the current `StashServer` otherwise. The flow is live — it updates if the active server changes.

**`test()` return type:** `suspend fun test(server: StashServer): AppResult<ServerInfo>` — `AppResult` is from `io.stashapp.android.core.common`. This is a one-shot suspend call, not a Flow. For D-04 the VM calls this once on init (or when refresh is triggered).

**`disconnect()` return type:** `suspend fun disconnect()` — already called in current `SettingsViewModel.disconnect()`.

### StashServer (`core/model/src/main/java/.../Connection.kt`) — 7 lines

```kotlin
data class StashServer(
    val baseUrl: String,    // the endpoint URL displayed in the server card
    val apiKey: String?,    // null if no API key configured
    val displayName: String, // the label shown in the hub card (TitleMedium)
)
```

### ServerInfo (`core/model/src/main/java/.../Connection.kt`) — 10 lines

```kotlin
data class ServerInfo(
    val version: String,          // "vX.X.X" — shown in MetaMono sub-line
    val buildTime: String?,       // build timestamp — shown in About page
    val sceneCount: Int,          // count grid in Server detail page
    val performerCount: Int,      // count grid
    val studioCount: Int,         // count grid
    val tagCount: Int,            // count grid
)
```

**What `ServerInfo` does NOT expose:** latency (confirmed — `test()` does not measure or return latency). Per D-04, omit latency from the server card for v1.

### VM Init Pattern for D-04

```kotlin
// In SettingsViewModel.init { }:
viewModelScope.launch {
    connectionRepository.activeServer().collectLatest { server ->
        _activeServer.value = server
        if (server != null) {
            _serverInfo.value = null  // show loading state
            connectionRepository.test(server)
                .onSuccess { _serverInfo.value = it }
                .onFailure { _serverInfo.value = null }  // card shows stub
        }
    }
}
```

`AppResult` pattern: check `core/common` for the sealed class. The `onSuccess`/`onFailure` extension methods may or may not exist — the planner should verify the `AppResult` API shape before writing the VM code. From the existing `connectionRepository.test()` usage in `ConnectionScreen.kt` (not read here), the pattern is likely `when(result) { is AppResult.Success -> ...; is AppResult.Error -> ... }`.

---

## Architecture Patterns

### Recommended File Layout After Phase 6

```
feature/settings/src/main/java/.../feature/settings/
├── SettingsScreen.kt           (hub — replaces current 579-line file)
├── SettingsViewModel.kt        (NEW — extracted + expanded from SettingsScreen.kt)
├── SettingsSearch.kt           (NEW — SettingsSearchEntry data class + static index)
├── SettingsPlaybackScreen.kt   (NEW)
├── SettingsCodecsScreen.kt     (NEW)
├── SettingsDisplayScreen.kt    (NEW)
├── SettingsLibraryScreen.kt    (NEW)
├── SettingsServerScreen.kt     (NEW)
└── SettingsAboutScreen.kt      (NEW)

core/designsystem/src/main/java/.../component/
├── ResumeCard.kt               (unchanged — template reference)
├── SceneCard.kt                (unchanged)
├── CSlider.kt                  (NEW — SETTINGS-04)
└── DRow.kt                     (NEW — SETTINGS-05)

core/domain/src/main/java/.../
├── PlayerSettings.kt           (UPDATED — new boolean prefs)
└── UiSettings.kt               (UPDATED — accentPalette + new display/library prefs)

core/data/src/main/java/.../prefs/
├── PlayerPreferences.kt        (UPDATED — implement new booleans)
└── UiPreferences.kt            (UPDATED — implement accentPalette + new prefs)

core/designsystem/src/main/java/.../theme/
└── Theme.kt                    (UPDATED — LocalAccentColors, AccentColors, accentName param)
```

### HubRow Current-Value Summary Pattern

The hub `HubRow` shows an inline JetBrainsMono 11sp AccentPrimary summary of the current settings values (e.g. "1.0× · 10s seek · HW · Fit"). This is collected in the hub composable via `collectAsStateWithLifecycle` from the PlayerSettings flows. Since `SettingsViewModel.playerPrefs` is a public `val`, the hub composable does:

```kotlin
val speed by viewModel.playerPrefs.defaultPlaybackSpeed.collectAsStateWithLifecycle(...)
val seek  by viewModel.playerPrefs.doubleTapSeekSeconds.collectAsStateWithLifecycle(...)
// etc.
val playbackSummary = "${speed}× · ${seek}s seek · ..."
```

---

## Common Pitfalls

### Pitfall 1: SettingsViewModel extract breaks Hilt injection
**What goes wrong:** Moving `SettingsViewModel` from `SettingsScreen.kt` to `SettingsViewModel.kt` while leaving `@HiltViewModel` works fine — but if the `@Inject constructor` ordering changes or the file's package path differs from the feature module's Hilt component scope, Hilt will fail with a `MissingBinding` error at runtime.
**Why it happens:** Hilt generates component code per module boundary. The VM must remain in the same Gradle module (`feature/settings`).
**How to avoid:** Keep `SettingsViewModel.kt` in `io.stashapp.android.feature.settings` — same package as the current inline class.

### Pitfall 2: Sub-routes captured by isMainTabRoute
**What goes wrong:** Adding `route.startsWith("settings")` to `isMainTabRoute` would show the bottom nav on all detail pages.
**How to avoid:** Keep the check as `route == Routes.Settings` (exact match, already confirmed in the code).

### Pitfall 3: StashDarkColorScheme is a top-level val
**What goes wrong:** `StashDarkColorScheme` is currently `private val` (static allocation). Moving it inside `StashTheme` to make it dynamic creates a new `ColorScheme` object on every recomposition.
**How to avoid:** Use `remember(accentName) { StashDarkColorScheme.copy(...) }` inside `StashTheme` to memoize per accent name. Only 3 possible values — the object is recreated at most twice per app session.

### Pitfall 4: Missing PlayerSettings/UiSettings properties for new settings
**What goes wrong:** SETTINGS-06 through SETTINGS-09 reference booleans (`showChapterThumbnails`, `lockControlsOnIdle`, `hdrPassthrough`, etc.) that do not exist in the current interfaces. Composables referencing them won't compile.
**How to avoid:** Plan 6.2 Task 0 (or Plan 6.1 Task 5) must add all missing properties to the domain interfaces and their DataStore implementations before the detail-page composables reference them.

### Pitfall 5: AppResult API shape unknown
**What goes wrong:** `connectionRepository.test()` returns `AppResult<ServerInfo>`. If the VM uses `result.onSuccess { }` but `AppResult` is a sealed class requiring `when` matching, it won't compile.
**How to avoid:** Read `core/common/src/main/java/.../AppResult.kt` before writing the VM test call. (This file was not in the A7 research scope — planner should include it as a task pre-condition.)

### Pitfall 6: onBrowsePerformers/Studios/Tags removal
**What goes wrong:** Current `MainActivity.kt` passes `onBrowsePerformers`, `onBrowseStudios`, `onBrowseTags` to `SettingsScreen`. The hub does not have browse links. Removing these parameters without updating the call site in `MainActivity.kt` causes a compile error.
**How to avoid:** Update both `SettingsScreen` signature and `MainActivity.kt` call site in the same commit.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Pref persistence | Custom file I/O or SharedPreferences | DataStore (already wired) | Existing pattern; coroutine-native; no main-thread I/O |
| Palette derivation | Manual color math | `accentForName()` data class | 3 hardcoded palettes; no computation needed |
| Nav back stack | Custom back stack tracking | `navController.popBackStack()` | Standard Compose Navigation; already used throughout |
| Slider component | Custom canvas slider | M3 `Slider` + `SliderDefaults.colors()` inside `CSlider` | M3 Slider handles touch, accessibility, RTL; wrap it, don't replace it |
| State collection | Manual coroutine collect + mutableState | `collectAsStateWithLifecycle` | Lifecycle-aware, already imported throughout |

---

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | `AppResult` has `onSuccess`/`onFailure` extension methods or can be used with `when` branching | A7 | VM `test()` call pattern must be adjusted; low risk, easy fix |
| A2 | `EndpointStateHolder` referenced in CONTEXT.md/SPEC is synonymous with `ConnectionRepository.activeServer()` Flow and no separate injection is needed | A7 | If `EndpointStateHolder` is a distinct injectable, the VM requires an additional constructor parameter |
| A3 | `SettingsScreenSmokeTest.kt` exists in the test sources and its call site will need updating per D-10 | D-10 | If the test file does not exist, D-10 has no impact — no action needed |

---

## Environment Availability

Step 2.6: SKIPPED — this is a pure Kotlin/Compose code change phase. No external tools, databases, CLI utilities, or services beyond the existing Android build toolchain are required.

---

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | Robolectric / JUnit (inferred from existing smoke tests) |
| Config file | check `feature/settings/src/test/` |
| Quick run command | `./gradlew :feature:settings:test` |
| Full suite command | `./gradlew :feature:settings:assembleDebug :feature:settings:test` |

### Phase Requirements → Test Map

Per D-10, no new test files. Update existing smoke test only.

| Req ID | Behavior | Test Type | Action |
|--------|----------|-----------|--------|
| SETTINGS-01–11 | Hub compiles + renders | Build | `./gradlew :feature:settings:assembleDebug` |
| SETTINGS-03 | Sub-routes wired | Build | `./gradlew :app:assembleDebug` |
| Smoke test | `SettingsScreenSmokeTest` call site updated | Unit | Update lambda params, re-run |

### Wave 0 Gaps

- [ ] Update `SettingsScreenSmokeTest.kt` call site — add 6 no-op lambda params for new `on*Click` callbacks
- [ ] Verify `AppResult.kt` shape before writing VM `test()` call (`core/common/src/main/java/.../AppResult.kt`)

---

## Sources

### Primary (HIGH confidence — live source files read)
- `feature/settings/.../SettingsScreen.kt` — full 580-line file read
- `core/domain/.../PlayerSettings.kt` — full file read
- `core/domain/.../UiSettings.kt` — full file read
- `core/data/prefs/UiPreferences.kt` — full file read
- `core/data/prefs/PlayerPreferences.kt` — full file read
- `core/designsystem/theme/Theme.kt` — full file read
- `core/ui/nav/Routes.kt` — full file read
- `app/MainActivity.kt` — full file read (lines 1–441)
- `core/designsystem/component/ResumeCard.kt` — full file read (actual path; CONTEXT.md has wrong filename `SpineResumeCard.kt`)
- `core/domain/ConnectionRepository.kt` — full file read
- `core/model/Connection.kt` — full file read (StashServer + ServerInfo fields)
- `.planning/phases/06-settings-redesign/06-CONTEXT.md` — full file read
- `.planning/phases/06-settings-redesign/06-SPEC.md` — full file read
- `design_handoff_slopper_spine/README.md §6` — full file read

### Notes on Filename Discrepancy
- CONTEXT.md references `SpineResumeCard.kt` — the actual file is `ResumeCard.kt`
- The composable function inside is named `SpineResumeCard` — this is the correct function name for cross-module usage

---

## Metadata

**Confidence breakdown:**
- Current SettingsScreen structure: HIGH — file read in full
- PlayerSettings/UiSettings interfaces: HIGH — files read in full
- UiPreferences keys and addition pattern: HIGH — file read in full
- Theme.kt injection point: HIGH — file read in full
- Routes.kt and nav wiring: HIGH — files read in full
- Component placement pattern: HIGH — actual ResumeCard.kt read
- ConnectionRepository / ServerInfo fields: HIGH — both files read in full
- Missing prefs (new booleans for SETTINGS-06/07/08/09): HIGH confidence these are missing — confirmed by reading the interface files

**Research date:** 2026-05-19
**Valid until:** 2026-06-19 (stable domain; DataStore and Compose Navigation APIs are stable)
