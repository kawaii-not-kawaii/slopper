---
phase: 06-settings-redesign
fixed_at: 2026-05-19T06:47:00Z
review_path: .planning/phases/06-settings-redesign/06-REVIEW.md
iteration: 1
findings_in_scope: 10
fixed: 9
skipped: 1
status: partial
---

# Phase 6: Code Review Fix Report

**Fixed at:** 2026-05-19T06:47:00Z
**Source review:** .planning/phases/06-settings-redesign/06-REVIEW.md
**Iteration:** 1

**Summary:**
- Findings in scope: 10 (5 Critical + 5 Warning)
- Fixed: 9
- Skipped: 1

## Fixed Issues

### CR-01: Invisible vertical dividers in server info grid

**Files modified:** `feature/settings/src/main/java/io/stashapp/android/feature/settings/SettingsServerScreen.kt`
**Commit:** 3dd384b
**Applied fix:** Added `.fillMaxHeight()` to the three `Spacer` dividers inside the 4-column stats grid `Row`. Also added missing `import androidx.compose.foundation.layout.fillMaxHeight`.

---

### CR-02: Settings hub back button uses wrong icon

**Files modified:** `feature/settings/src/main/java/io/stashapp/android/feature/settings/SettingsScreen.kt`
**Commit:** 6630e3f
**Applied fix:** Changed `Icons.AutoMirrored.Outlined.ArrowForwardIos` to `Icons.AutoMirrored.Outlined.ArrowBack` in the settings hub `TopAppBar` navigation icon, and updated size from 18.dp to 22.dp to match other detail screens. The `ArrowBack` import was already present at line 28.

---

### CR-03: Scrub-sensitivity slider range contradicts domain constants

**Files modified:** `feature/settings/src/main/java/io/stashapp/android/feature/settings/SettingsPlaybackScreen.kt`
**Commit:** d3c6a59
**Applied fix:** Replaced hardcoded `50f..300f` with `PlayerPreferences.SEEK_MS_PER_PX_MIN..PlayerPreferences.SEEK_MS_PER_PX_MAX` (20f..500f). Added `import io.stashapp.android.core.data.prefs.PlayerPreferences` to the file.

---

### CR-04: Cache clear button label claims to show "used" space

**Files modified:** `feature/settings/src/main/java/io/stashapp/android/feature/settings/SettingsLibraryScreen.kt`
**Commit:** 064656d
**Applied fix:** Changed label text from `"Clear image cache · $cacheMb MB used"` to `"Clear image cache · up to $cacheMb MB"` (Option B from the review suggestion).

---

### CR-05: "Resolution badge" setting absent from the search index

**Files modified:** `feature/settings/src/main/java/io/stashapp/android/feature/settings/SettingsSearch.kt`
**Commit:** fa9d11b
**Applied fix:** Added the missing `SettingsSearchEntry("Resolution badge", "Show resolution label on scene thumbnail cards", "Display · Card chrome", Routes.SettingsDisplay)` entry between "Play count on cards" and "Resume bar", matching the order in `SettingsDisplayScreen.kt`.

---

### WR-01: Production debug logs left in navigation handlers

**Files modified:** `app/src/main/java/io/stashapp/android/MainActivity.kt`
**Commit:** a39f73c
**Applied fix:** Removed all three `android.util.Log.i("StashNav", ...)` calls from the `onPerformerClick`, `onStudioClick`, and `onTagClick` handlers in the `BrowseScreen` composable block.

---

### WR-03: searchQuery is publicly mutable

**Files modified:** `feature/settings/src/main/java/io/stashapp/android/feature/settings/SettingsViewModel.kt`
**Commit:** edc5ca9
**Applied fix:** Renamed public `val searchQuery = MutableStateFlow("")` to `private val _searchQuery = MutableStateFlow("")` and added `val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()`. Updated the `searchResults` derived flow to use `_searchQuery` and `updateSearchQuery` to write to `_searchQuery.value`.

---

### WR-04: @Suppress("UNCHECKED_CAST") on UiPreferences.flow() is spurious

**Files modified:** `core/data/src/main/java/io/stashapp/android/core/data/prefs/UiPreferences.kt`
**Commit:** eca62d1
**Applied fix:** Removed the `@Suppress("UNCHECKED_CAST")` annotation from the private `flow()` helper. Build confirms it compiles cleanly without it, consistent with the identical helper in `PlayerPreferences` which has no such suppression.

---

### WR-05: Smoke tests provide no behavioural coverage

**Files modified:** `feature/settings/src/test/java/io/stashapp/android/feature/settings/SettingsScreenSmokeTest.kt`
**Commit:** cfcd96b
**Applied fix:** Added 4 meaningful filter tests to `SettingsScreenSmokeTest`:
1. `search filter returns matching entries for haptics` — asserts exactly 1 result with label "Haptics on seek"
2. `search filter is case-insensitive` — compares UPPER vs lower query results for "haptics"
3. `search filter returns empty list for blank query` — documents the ViewModel's isBlank() gate behaviour
4. `resolution badge entry is present in search index` — verifies the CR-05 fix is discoverable via search

Removed the private `assertEquals` helper (now redundant with the static import of `org.junit.jupiter.api.Assertions.assertEquals`). Added imports for `assertEquals` and `assertTrue`.

---

## Skipped Issues

### WR-02: ViewModel exposes concrete DataStore types instead of domain interfaces

**File:** `feature/settings/src/main/java/io/stashapp/android/feature/settings/SettingsViewModel.kt:27-28`
**Reason:** Blocked by two hard coupling points that cannot be resolved without breaking changes across 6 screen files.

**Blocker detail:**

1. **`setPlayer`/`setUi` lambda receivers are typed to concrete classes:** `fun setPlayer(setter: suspend PlayerPreferences.() -> Unit)` and `fun setUi(setter: suspend UiPreferences.() -> Unit)`. Changing the field types from `PlayerPreferences`/`UiPreferences` to `PlayerSettings`/`UiSettings` would require also changing these lambda signatures, which in turn requires every call site like `viewModel.setPlayer { setSeekMsPerPx(it) }` across all 6 screen files to remain compilable. The `setSeekMsPerPx` method exists on both the interface and the concrete class, so this part could work — but the lambda receiver type change is a broader API surface change.

2. **Companion object constants referenced in `SettingsScreen.kt`:** Lines 101-111 of `SettingsScreen.kt` directly reference `PlayerPreferences.DEFAULT_SPEED`, `PlayerPreferences.DEFAULT_DOUBLE_TAP_SEEK_SEC`, `PlayerPreferences.DEFAULT_BUFFER_PRESET`, `PlayerPreferences.DEFAULT_DECODER_PREF`, `UiPreferences.DEFAULT_ACCENT_PALETTE`, `UiPreferences.DEFAULT_GRID_COLUMNS`, and `UiPreferences.DEFAULT_IMAGE_CACHE_MB`. These constants live in the `companion object` of the concrete classes, not on the `PlayerSettings` or `UiSettings` interfaces. Moving them to the interfaces or to a separate `PlayerDefaults`/`UiDefaults` object would be a companion refactor outside the scope of this fix pass. The review itself noted this concern: "The concrete type is only needed for the companion object constants referenced in SettingsScreen.kt — move those constants to the interface or a separate PlayerDefaults object." This is a design task, not a line-level fix.

**Recommendation:** Treat WR-02 as a dedicated refactoring task. Steps: (1) add the constants to `PlayerSettings`/`UiSettings` companion objects, (2) update `SettingsScreen.kt` to use the interface constants, (3) change the ViewModel field types and lambda receivers to the interface types, (4) verify all 6 screen composable files compile.

**Original issue:** SettingsViewModel declares injected fields as `val playerPrefs: PlayerPreferences` and `val uiPrefs: UiPreferences` (concrete classes), bypassing the domain interfaces.

---

## Build Verification

`./gradlew :feature:settings:assembleDebug --no-daemon` — **BUILD SUCCESSFUL** (29s)

All 9 fixed findings compiled cleanly. Only pre-existing deprecation warnings in `ConnectionStore.kt` and annotation target notes in `PlayerPreferences.kt`/`UiPreferences.kt` — none introduced by these fixes.

---

_Fixed: 2026-05-19T06:47:00Z_
_Fixer: Claude (gsd-code-fixer)_
_Iteration: 1_
