---
phase: 06-settings-redesign
reviewed: 2026-05-19T06:25:00Z
depth: standard
files_reviewed: 21
files_reviewed_list:
  - app/src/main/java/io/stashapp/android/MainActivity.kt
  - core/data/src/main/java/io/stashapp/android/core/data/prefs/PlayerPreferences.kt
  - core/data/src/main/java/io/stashapp/android/core/data/prefs/UiPreferences.kt
  - core/designsystem/src/main/java/io/stashapp/android/core/designsystem/component/CSlider.kt
  - core/designsystem/src/main/java/io/stashapp/android/core/designsystem/component/DRow.kt
  - core/designsystem/src/main/java/io/stashapp/android/core/designsystem/theme/Theme.kt
  - core/domain/src/main/java/io/stashapp/android/core/domain/PlayerSettings.kt
  - core/domain/src/main/java/io/stashapp/android/core/domain/UiSettings.kt
  - core/ui/src/main/java/io/stashapp/android/core/ui/nav/Routes.kt
  - feature/settings/src/main/java/io/stashapp/android/feature/settings/SettingsAboutScreen.kt
  - feature/settings/src/main/java/io/stashapp/android/feature/settings/SettingsCodecsScreen.kt
  - feature/settings/src/main/java/io/stashapp/android/feature/settings/SettingsDisplayScreen.kt
  - feature/settings/src/main/java/io/stashapp/android/feature/settings/SettingsLibraryScreen.kt
  - feature/settings/src/main/java/io/stashapp/android/feature/settings/SettingsPlaybackScreen.kt
  - feature/settings/src/main/java/io/stashapp/android/feature/settings/SettingsScreen.kt
  - feature/settings/src/main/java/io/stashapp/android/feature/settings/SettingsSearch.kt
  - feature/settings/src/main/java/io/stashapp/android/feature/settings/SettingsServerScreen.kt
  - feature/settings/src/main/java/io/stashapp/android/feature/settings/SettingsViewModel.kt
  - feature/settings/src/test/java/io/stashapp/android/feature/settings/SettingsScreenSmokeTest.kt
  - feature/settings/src/test/java/io/stashapp/android/feature/settings/SettingsViewModelTest.kt
findings:
  critical: 5
  warning: 5
  info: 3
  total: 13
status: issues_found
---

# Phase 6: Code Review Report

**Reviewed:** 2026-05-19T06:25:00Z
**Depth:** standard
**Files Reviewed:** 21
**Status:** issues_found

## Summary

Phase 6 delivers the SETTINGS-V3 hub-and-drill-down redesign: a hub screen, six detail pages (Playback, Codecs, Display, Library, Server, About), a full-text search overlay, and 25 new preferences backed by DataStore. The overall architecture is sound. The domain-layer interface split (PlayerSettings / UiSettings) is cleanly maintained, the DataStore helpers are correct, and the navigation graph is wired properly.

Five critical issues were found — two are rendering bugs visible to any user (an invisible column divider and a wrong back-navigation icon), two are incorrect UI semantics (cache label shows configured max not actual usage; seek slider range contradicts the domain constants), and one is a concrete data-correctness gap (search index is missing a settings entry). Five warnings address ViewModel encapsulation, debug log leakage, a suppression annotation of questionable necessity, and test coverage gaps. Three info items note minor code style inconsistencies.

---

## Critical Issues

### CR-01: Invisible vertical dividers in server info grid

**File:** `feature/settings/src/main/java/io/stashapp/android/feature/settings/SettingsServerScreen.kt:301-308`

**Issue:** The four-column stats grid (Scenes / Studios / Performers / Tags) uses `Spacer(Modifier.width(1.dp).background(SpineColors.Border))` as a column divider inside a `Row`. A `Spacer` with only a `width` modifier and no `height` or `fillMaxHeight()` collapses to 0 dp tall in Compose, making all three dividers invisible at runtime. The divider lines between columns will never appear.

**Fix:**
```kotlin
// Before
Spacer(
    modifier = Modifier
        .width(1.dp)
        .background(SpineColors.Border),
)

// After
Spacer(
    modifier = Modifier
        .width(1.dp)
        .fillMaxHeight()
        .background(SpineColors.Border),
)
```

---

### CR-02: Settings hub back button uses wrong icon (ArrowForwardIos instead of ArrowBack)

**File:** `feature/settings/src/main/java/io/stashapp/android/feature/settings/SettingsScreen.kt:137-143`

**Issue:** The `navigationIcon` slot of the Settings hub `TopAppBar` renders `Icons.AutoMirrored.Outlined.ArrowForwardIos` (a right-pointing chevron) with `contentDescription = "Back"`. Every other detail screen in this feature uses `Icons.AutoMirrored.Outlined.ArrowBack`. This is functionally correct (the `onClick = onBack` is wired), but visually displays a "forward" arrow for a "back" action, directly contradicting platform conventions and the rest of the feature.

**Fix:**
```kotlin
// Before
imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
contentDescription = "Back",
modifier = Modifier.size(18.dp),

// After
imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
contentDescription = "Back",
modifier = Modifier.size(22.dp), // match other screens
```

---

### CR-03: Scrub-sensitivity slider range contradicts domain constants

**File:** `feature/settings/src/main/java/io/stashapp/android/feature/settings/SettingsPlaybackScreen.kt:164-167`

**Issue:** The "Scrub sensitivity" `CSlider` uses `valueRange = 50f..300f`. `PlayerPreferences` defines authoritative constants `SEEK_MS_PER_PX_MIN = 20f` and `SEEK_MS_PER_PX_MAX = 500f`. The UI clips [20, 50) from the low end and (300, 500] from the high end, making it impossible to set values in those ranges even though they are valid and could already be stored in DataStore (e.g., from a previous migration or a future feature). A user who previously had `seekMsPerPx = 25f` stored will see the slider pinned to 50 with no way to go lower.

**Fix:**
```kotlin
CSlider(
    value = seekMs,
    onValueChange = { viewModel.setPlayer { setSeekMsPerPx(it) } },
    valueRange = PlayerPreferences.SEEK_MS_PER_PX_MIN..PlayerPreferences.SEEK_MS_PER_PX_MAX,
    valueLabel = "${seekMs.roundToInt()} ms/px",
)
```
Use the named constants so the UI and domain layer stay in sync automatically.

---

### CR-04: Cache clear button label claims to show "used" space but shows configured maximum

**File:** `feature/settings/src/main/java/io/stashapp/android/feature/settings/SettingsLibraryScreen.kt:189`

**Issue:** The `TextButton` label reads `"Clear image cache · $cacheMb MB used"`. The variable `cacheMb` is collected from `uiPrefs.imageCacheSizeMb`, which is the **configured maximum size** of the cache, not the actual current disk usage. A user who sets the cache to 512 MB but has only cached 10 MB of thumbnails will see "Clear image cache · 512 MB used" — a factually incorrect statement that overstates disk usage by up to 50×.

**Fix:**
```kotlin
// Option A: Remove the size claim (accurate, simplest)
text = "Clear image cache"

// Option B: Clarify it's the limit, not usage
text = "Clear image cache · up to $cacheMb MB"
```
Actual disk usage would require querying the cache directory size, which is not available in the current data model.

---

### CR-05: "Resolution badge" setting is absent from the search index

**File:** `feature/settings/src/main/java/io/stashapp/android/feature/settings/SettingsSearch.kt:49-52`

**Issue:** `SettingsDisplayScreen` contains five Card chrome rows: Rating, Play count, **Resolution badge**, Resume bar, and Studio caption. The `SettingsSearchIndex` in `Display · Card chrome` contains only four entries — Rating on cards, Play count on cards, Resume bar, Studio caption — with no entry for "Resolution badge". Any user searching for "resolution" in the search overlay will get the "Match resolution" codec entry (a different setting) but will not be directed to the display card toggle. The setting is un-discoverable via search.

**Fix:** Add the missing entry to `SettingsSearchIndex` after the "Play count on cards" entry:
```kotlin
SettingsSearchEntry(
    "Resolution badge",
    "Show resolution label on scene thumbnail cards",
    "Display · Card chrome",
    Routes.SettingsDisplay,
),
```

---

## Warnings

### WR-01: Production debug logs left in navigation handlers

**File:** `app/src/main/java/io/stashapp/android/MainActivity.kt:392-403`

**Issue:** Three `android.util.Log.i("StashNav", ...)` calls are present in the `BrowseScreen` click handlers (performer, studio, tag). These log route strings including user-provided IDs to Logcat and will ship to production builds unless excluded by ProGuard/R8. Per project coding-style rules, no `console.log` / debug logging should remain in committed code.

**Fix:** Remove the three `Log.i` calls. If navigation tracing is needed in debug builds, gate them behind `if (BuildConfig.DEBUG)`.

---

### WR-02: ViewModel exposes concrete DataStore types instead of domain interfaces

**File:** `feature/settings/src/main/java/io/stashapp/android/feature/settings/SettingsViewModel.kt:27-28`

**Issue:** `SettingsViewModel` declares its injected preferences as `val playerPrefs: PlayerPreferences` and `val uiPrefs: UiPreferences` (concrete classes from `:core:data`). All six screen composables then access these fields directly (e.g., `viewModel.playerPrefs.decoderPreference`). This means every screen composable imports a concrete data-layer type, bypassing the domain interfaces (`PlayerSettings`, `UiSettings`) that exist specifically to decouple layers. Testing is also harder — fakes must implement the concrete class, not just the interface.

**Fix:**
```kotlin
// In SettingsViewModel constructor and fields:
val playerPrefs: PlayerSettings,  // not PlayerPreferences
val uiPrefs: UiSettings,          // not UiPreferences
```
The Hilt binding of `PlayerPreferences -> PlayerSettings` already exists via `@Binds`. The concrete type is only needed for the `companion object` constants referenced in `SettingsScreen.kt` — move those constants to the interface or a separate `PlayerDefaults` object.

---

### WR-03: searchQuery is publicly mutable (MutableStateFlow exposed directly)

**File:** `feature/settings/src/main/java/io/stashapp/android/feature/settings/SettingsViewModel.kt:64`

**Issue:** `val searchQuery = MutableStateFlow("")` is a `public val` on the ViewModel. Any caller can write `viewModel.searchQuery.value = "..."` directly, bypassing `updateSearchQuery()`. While no current caller does this (the screen uses `viewModel::updateSearchQuery`), the invariant is not enforced. If `updateSearchQuery` is ever extended (validation, analytics, debounce), direct mutation would silently bypass it.

**Fix:**
```kotlin
private val _searchQuery = MutableStateFlow("")
val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

fun updateSearchQuery(query: String) {
    _searchQuery.value = query
}
```

---

### WR-04: @Suppress("UNCHECKED_CAST") on UiPreferences.flow() helper is spurious

**File:** `core/data/src/main/java/io/stashapp/android/core/data/prefs/UiPreferences.kt:172`

**Issue:** The `@Suppress("UNCHECKED_CAST")` annotation on `private fun <T> flow(key: Preferences.Key<T>, default: T): Flow<T>` is not justified. The DataStore `Preferences` operator `get(key: Key<T>): T?` is generically typed — `it[key]` returns `T?` and `?: default` returns `T`. There is no unchecked cast in this function. `PlayerPreferences` has the identical helper without any suppression and compiles cleanly. The suppression may mask a real future warning if the function signature changes.

**Fix:** Remove the `@Suppress("UNCHECKED_CAST")` annotation from `UiPreferences.flow()`. Verify it compiles without it (it should — `PlayerPreferences` proves it).

---

### WR-05: Smoke tests provide no behavioural coverage and duplicate each other

**File:** `feature/settings/src/test/java/io/stashapp/android/feature/settings/SettingsScreenSmokeTest.kt` and `SettingsViewModelTest.kt`

**Issue:** Both test files contain nearly identical tests that only confirm the class is not null and its simple name is a string literal. Neither test verifies any actual logic: the filter function in `searchQuery`→`searchResults`, the correct number of search entries per section, the DataStore defaults, or the `disconnect` flow. The `SettingsScreenSmokeTest.SettingsViewModel class is accessible` test in particular is always true for any compiled class; it cannot fail in any meaningful scenario. The project coding-style rules require 80% test coverage; these tests contribute nothing toward that target.

**Fix:** At minimum add a test for the search filter logic, which is pure and testable without Hilt:
```kotlin
@Test
fun `search filter returns matching entries`() {
    val results = SettingsSearchIndex.filter { entry ->
        (entry.label + " " + entry.hint).contains("haptics", ignoreCase = true)
    }
    assertEquals(1, results.size)
    assertEquals("Haptics on seek", results.first().label)
}

@Test
fun `search filter is case-insensitive`() {
    val upper = SettingsSearchIndex.filter { (it.label + " " + it.hint).contains("HAPTICS", ignoreCase = true) }
    val lower = SettingsSearchIndex.filter { (it.label + " " + it.hint).contains("haptics", ignoreCase = true) }
    assertEquals(upper.size, lower.size)
}
```

---

## Info

### IN-01: Spacer used as horizontal gap with padding instead of width modifier

**File:** `feature/settings/src/main/java/io/stashapp/android/feature/settings/SettingsPlaybackScreen.kt:369-371`

**Issue:** In the `DetailGroup` badge row, the gap between the group title text and the badge pill is created with `Spacer(modifier = Modifier.padding(start = 8.dp))`. This produces a zero-size `Spacer` with external padding rather than an explicit 8 dp width. It renders correctly in practice (the padding expands the composable's bounding box) but is semantically incorrect and inconsistent with every other horizontal gap in the codebase (`Spacer(Modifier.width(X.dp))`).

**Fix:**
```kotlin
// Before
androidx.compose.foundation.layout.Spacer(
    modifier = Modifier.padding(start = 8.dp),
)

// After
Spacer(Modifier.width(8.dp))
```

---

### IN-02: buildType detection in SettingsAboutScreen is fragile heuristic

**File:** `feature/settings/src/main/java/io/stashapp/android/feature/settings/SettingsAboutScreen.kt:73-75`

**Issue:** Build type is inferred from the `versionName` string (`contains("alpha")` or `contains("debug")`). This fails for version names like `1.0.0-beta`, `1.0.0-rc1`, or custom CI suffixes. The label shows "release" for those, which may be misleading. The reliable source is `BuildConfig.BUILD_TYPE`, but that is not accessible in feature modules. An alternative is passing it in via the ViewModel or a system property.

**Fix (minimal):** Widen the condition to cover common non-release suffixes:
```kotlin
val buildType = when {
    versionName.contains("debug",  ignoreCase = true) -> "debug"
    versionName.contains("alpha",  ignoreCase = true) -> "alpha"
    versionName.contains("beta",   ignoreCase = true) -> "beta"
    versionName.contains("rc",     ignoreCase = true) -> "rc"
    else -> "release"
}
```

---

### IN-03: SettingsScreen hub imports unused ArrowBack icon

**File:** `feature/settings/src/main/java/io/stashapp/android/feature/settings/SettingsScreen.kt:28`

**Issue:** `import androidx.compose.material.icons.automirrored.outlined.ArrowBack` is imported at the top of `SettingsScreen.kt` but is never used (the back button at line 139 uses `ArrowForwardIos`). This is an unused import. The import would become used if CR-02 is fixed, so it should be retained once CR-02 is addressed — but as submitted, it is dead.

**Fix:** Remove `import androidx.compose.material.icons.automirrored.outlined.ArrowBack` from `SettingsScreen.kt`, or fix CR-02 (which will make the import needed again).

---

_Reviewed: 2026-05-19T06:25:00Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
