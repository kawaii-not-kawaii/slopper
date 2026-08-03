# File map

Paths are relative to the repo root (`kawaii-not-kawaii/slopper`, branch `master`).
Ordered so the tree compiles at every step: primitives first, then screens, then data.

## 1 · New — design-system primitives
All in `core/designsystem/src/main/java/io/stashapp/android/core/designsystem/component/`.
Public composables, same style as the existing `CSlider.kt` / `DRow.kt`
(KDoc with a usage block, `LocalAccentColors.current` for accent, `SpineColors` for the rest).

| File | What it is | Replaces |
|---|---|---|
| `SpineChip.kt` | `SpineChip` (selectable) + `SpineTriStateChip` (yes/no/any) | every `FilterChip` in `FilterSheet.kt` |
| `SegmentedRail.kt` | `SegmentedRail<T>` — one bordered row of equal segments | resolution / date / orientation chip rows; grid-columns `ChipRow`; seek + cache `CSlider`s; search scope chips |
| `StepperField.kt` | `StepperField` — −/value/+ with a null "any" state | the two `IntMinSlider`s |
| `StarRatingPicker.kt` | `StarRatingPicker` — 5 tappable stars, 0.5 steps, null = any | `RatingRange` (`RangeSlider`) |
| `SectionLabel.kt` | `SectionLabel` — uppercase mono label | the two private copies (in `SettingsScreen.kt`, and the `SectionTitle` in `FilterSheet.kt`) |

Detail: `SPEC_components.md`.

## 2 · Modify — screens

### `feature/library/src/main/java/io/stashapp/android/feature/library/FilterSheet.kt`
Largest change. Rewrite the sheet body and delete the private helpers that the new
primitives replace. Spec: `SPEC_filter_sheet.md`.
- Sheet: `fillMaxHeight(0.84f)` → `0.86f`, add `shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)` and a 1dp `BorderStrong` top edge.
- Add sticky header (`FILTERS` + active-count badge + `CLEAR ALL`) and sticky footer
  (`Reset` · `Save view` · `Apply`) outside the scrolling `Column`.
- New `PRESETS` section at the top.
- Delete: `SectionTitle`, `SortDropdown`, `DateChips`, `ResolutionChips`,
  `OrientationChips`, `RatingRange`, `IntMinSlider`, `ToggleChip`.
- Keep unchanged: `DurationSection` latching logic, `DurationCustomRange` state handling,
  `currentDurationBucket`, `currentDateBucket`, `datesFor`. Only their rendering changes.
- Keep the commit-on-Apply contract exactly as-is — **no live result count** on the Apply
  button (explicit product decision).

### `feature/settings/src/main/java/io/stashapp/android/feature/settings/SettingsScreen.kt`
Spec: `SPEC_settings.md`.
- Drop the three competing container languages (16dp server card / 6dp `DetailGroup` /
  bordered `NavRow` with icon tiles) for one: `SectionLabel` + a single bordered surface.
- Replace the `DetailGroup` wrappers around Theme / Library layout / Playback / Image cache
  with bare sections at an 18dp gutter.
- Grid columns, double-tap seek and disk cache become `SegmentedRail`s
  (cache also keeps a value bubble). Scrub sensitivity keeps `CSlider` but gains −/+ ends.
- Accent palette: three full-width-thirds swatch chips, not the 28dp-tile cards.
- `NavRow`: remove the 32dp icon tile, add a right-aligned mono value.
- New `SAVED FILTER PRESETS` section (chips + `manage…`).
- Move the private `SectionLabel` out to the new shared file; keep `SpineSwitch`,
  `ChipRow` and `DetailGroup` in this file — `SettingsServerScreen.kt` and
  `SettingsAboutScreen.kt` still import them (`internal`). Do **not** delete them.

### `feature/library/src/main/java/io/stashapp/android/feature/library/SearchOverlay.kt`
Spec: `SPEC_search.md`.
- Scope `FlowRow` of chips → one `SegmentedRail` with per-scope counts
  (`all 24 / scn 18 / stu 1 / prf 4 / tag 2`).
- Search field: keep `OutlinedTextField` but move it to `SpineColors.Surface` with a
  leading search icon and trailing clear (`×`); it already themes correctly.
- `SceneResultRow`: wrap in a bordered `Surface`, add the duration badge, put
  studio · resolution · rating on one `MonoSmall` line in `OnSurfaceVariant`
  (currently the studio line is accent — remove that).
- Add: top-result card, query-match highlight, `see all N scenes →` row, Tags and Recent
  sections (`TagChipRow`, `RecentChipRow`).
- `SearchResults` already carries `studioNames` / `tagNames` but nothing renders them;
  wire both. Populating the data is still deferred — see the note in `SPEC_search.md`.

### `feature/library/src/main/java/io/stashapp/android/feature/library/LibraryScreen.kt`
- Pass the active-filter count into `FilterSheet` (or compute it there from
  `SceneFilter`); the sheet header needs it.
- Wire the new `onSavePreset` / `onApplyPreset` callbacks through to the ViewModel.

## 3 · Modify — data layer (saved presets)
Spec: `SPEC_presets.md`. Today only a **single** default filter is persisted
(`UiSettings.defaultSceneFilter`); presets need a named list.

| File | Change |
|---|---|
| `core/domain/.../UiSettings.kt` | add `savedFilterPresets: Flow<List<SavedFilterPreset>>`, `saveFilterPreset(name, filter, sort)`, `deleteFilterPreset(id)`, `renameFilterPreset(id, name)` |
| `core/domain/.../SceneRepository.kt` | add the `SavedFilterPreset` data class next to `SceneFilter` (`id`, `name`, `filter`, `sort`) |
| `core/data/.../prefs/UiPreferences.kt` | new `stringPreferencesKey("saved_filter_presets")`; serialise `List<StoredPreset>` with the existing `Json`; reuse `StoredSceneFilter` (make it `internal`, add `sort: String?`) |
| `feature/library/.../LibraryViewModel.kt` | expose `presets: StateFlow<List<SavedFilterPreset>>`; add `savePreset(name)`, `applyPreset(id)`, `deletePreset(id)` |
| `feature/settings/.../SettingsViewModel.kt` | expose `presets` for the settings list + `deletePreset` / `renamePreset` |

Keep `defaultSceneFilter` working — it is a separate concept (what the Library opens with)
and `LibraryViewModel.init` depends on it.

## 4 · Tests
Existing smoke tests should keep passing unchanged:
`feature/library/src/test/.../LibraryScreenSmokeTest.kt`,
`feature/settings/src/test/.../SettingsScreenSmokeTest.kt`.
Worth adding: a `UiPreferencesTest` round-trip for preset serialisation, and a
`SceneFilter.activeCount` unit test (new derived property — see `SPEC_filter_sheet.md`).
