# Filter / Settings / Search redesign + blur + colour audit — implementation plan

Source of truth for the design: `design_handoff_filter_settings_search/` (in repo root).
Read in this order: `README.md` → `FILE_MAP.md` → `SPEC_components.md` → `SPEC_filter_sheet.md`
→ `SPEC_settings.md` → `SPEC_search.md` → `SPEC_presets.md`.
Approved screens are `screens/2a`, `3a`, `3b`, `3c`. Turns 1 and 2 are before/rejected — ignore them.

## The five user asks

1. Rebuild the filter sheet + settings screen to match the handoff. Everything functional, nothing mocked.
2. Make search actually work — today nothing happens when you tap it.
3. Enlarge the scenes-page search bar; it is out of proportion with the rest of the screen.
4. Find hardcoded colours that don't respect the accent/theme switch.
5. Add a thumbnail blur toggle so the app is safe to screenshot during device testing.

## Scouting results — read before planning anything

### Search is dead code, not broken code
- `feature/library/.../SearchOverlay.kt` is **referenced by nothing**. `grep -rn "SearchOverlay\|SearchResults"` returns only the file itself.
- `LibraryScreen.kt:126` — `.clickable { /* TODO: open search keyboard */ }`. That is the whole bug.
- The backend already works: `SceneQuery.searchText` → `q` in both `ScenePagingSource.kt:35` and
  `DefaultSceneRepository.kt:64`. `LibraryViewModel.setSearchText()` already exists and already
  feeds the paging flow.
- Performer/studio/tag lookup already exists: `BrowseRepository.filterOptions(FilterOptionQuery(kind, search))`
  returns `List<FilterEntityOption>` (id + label), implemented in `DefaultBrowseRepository.kt:60`
  for Performer/Studio/Tag with a search string and per_page 50.
- **No new GraphQL is needed for search.** It is wiring plus a ViewModel.
- `FilterOptionsViewModel.kt` is the debounce pattern to copy: cancel previous job, `delay(200)`,
  catch `CancellationException` / `ApolloException` / `IllegalStateException` separately.

### Presets genuinely don't exist
`UiSettings` persists exactly one unnamed `defaultSceneFilter`. Presets are a named list — new work.
`defaultSceneFilter` must keep working; `LibraryViewModel.init` depends on it.

### Accent bugs (task 4)
Real bugs — hardcoded sage that ignores the ember/signal palette:
- `core/ui/.../nav/BottomNav.kt` lines 160, 175, 188, 299
- `core/ui/.../nav/NavCustomizeSheet.kt` lines 79, 80, 87, 121, 122, 130, 170, 171

Both should read `LocalAccentColors.current` instead of `SpineColors.AccentPrimary` /
`AccentOnPrimary`. The theme is correctly wired at `MainActivity.kt:112` (`StashTheme(accentName)`),
so this is purely the two files not consuming it.

Not bugs, leave alone: `SettingsScreen.kt:243-245` uses `SageAccent`/`EmberAccent`/`SignalAccent`
deliberately — the palette swatches must show their own colour.

Raw hex with a token equivalent, worth replacing:
- `SceneCard.kt:102` `Color(0xEB0A0D12)` → `SpineColors.Bg.copy(alpha = 0.92f)`
- `PlayerTimeline.kt:128` `Color(0x59A8B0C0)`
- `PlayerSettingsPanel.kt:61` `Color(0xF20B0F16)`

~40 further `Color.White` / `Color.Black` uses in the player, detail hero and browse scrims are
**deliberate** (white-on-video overlays). Inventory them in the report; do not change them.

### Bonus bug found while scouting
`core/ui/.../image/StashImageLoader.kt:47` hardcodes `defaultCacheMb = 256` and never reads
`UiPreferences.imageCacheSizeMb`. The existing cache slider in settings is decorative. The new
THUMBNAIL CACHE rail must not ship equally fake — wire the pref.

### Environment
- minSdk 26, compileSdk 37, targetSdk 35, JDK 21, AGP 9.2.1, Kotlin 2.3.20.
- `Modifier.blur` requires **API 31+** and silently no-ops below. A privacy feature that fails
  silently is worse than none — fall back to an opaque scrim under 31.
- Test infra is plain JUnit 5 + MockK/Turbine. **No Compose UI test harness exists** — the
  `*SmokeTest.kt` files only assert on data classes. Write JVM unit tests, not UI tests.
- ktlint + detekt run on every module and are part of the gate.

## Decisions already made with the user — do not relitigate

| # | Decision |
|---|---|
| D1 | `StashCriteriaSection` ("All Stash filters", 547 lines) is **kept**, moved to the bottom of the sheet, collapsed behind an expandable `ALL STASH FILTERS` row. Not deleted — the mock just omits it. |
| D2 | MIN RESOLUTION rail ships the 7 mock stops (`any 480 720 1080 1440 4K 8K`). `SceneResolution` enum is **untouched** (all 15 entries stay). If a persisted filter holds an off-rail tier (5K/6K/VR/etc.) render it as an extra segment so it is not silently wiped. |
| D3 | Search builds **all three** extras: TOP RESULT card, RECENT searches, `see all N scenes →`. |
| D4 | Colour task: **fix** the accent bugs + raw-hex-with-a-token; **report only** the deliberate player/video overlay whites. |
| D5 | Blur: a **Settings toggle, default ON**, applied to **all remote imagery** (SceneCard, ResumeCard, search rows, detail hero, browse performer/studio/tag cards). |
| D6 | Drop the fake `⌘K` badge from the scenes search bar — desktop affordance on a phone. |

## Phases — serial, the tree compiles after each

### Phase 1 — design-system primitives (`core/designsystem/.../component/`)
Six new files, nothing else changes yet. Match the conventions in the existing `CSlider.kt` /
`DRow.kt`: public composable, KDoc with a usage block, `modifier: Modifier = Modifier` first
optional param, accent from `LocalAccentColors.current`, everything else from `SpineColors`.

- `SectionLabel.kt` — uppercase mono label, `trailing` slot for count / rule line. Lift the private
  copy out of `SettingsScreen.kt:402` verbatim and add `trailing`.
- `SpineChip.kt` — `SpineChip` (selectable, `leading`, `dashed`) + `SpineTriStateChip`
  (null→true→false→null; accent dot for yes, Error bar for no; label text never changes).
- `SegmentedRail.kt` — `SegmentedRail<T>(options: List<Pair<String, T>>, selected, onSelect)`.
  Selected segment gets accent-35% dividers on **both** its own edges; suppress the neighbouring
  plain dividers so they don't double up.
- `StepperField.kt` — −/value/+, null = "any" with a dimmed `OnSurfaceFaint` minus.
  `+` from null → min; `−` from min → null; clamp at max.
- `StarRatingPicker.kt` — 5 stars, tap star n → `n*20`, tap the current value → clear to null,
  left half of star n → `n*20-10`. Accent bubble when set, plain `any` pill when null.
- `PrivacyBlur.kt` — `Modifier.privacyBlur(enabled: Boolean)`. `Modifier.blur(20.dp)` on API 31+,
  opaque `SpineColors.SurfaceHigh` scrim below 31.

Define the 12% fill / 45% border / SemiBold selection treatment **once** in an internal helper and
reuse it across all of them.

### Phase 2 — data layer
- `core/domain/SceneRepository.kt` — add `SavedFilterPreset(id, name, filter, sort)` and
  `SceneFilter.activeCount`. Count min/max **pairs as one facet** (duration min+max is one control,
  rating min+max is one) or the badge reads 2 for a single duration bucket.
- `core/domain/UiSettings.kt` — `savedFilterPresets` / `saveFilterPreset` / `deleteFilterPreset` /
  `renameFilterPreset`; `recentSearches` / `addRecentSearch`; `blurThumbnails` / `setBlurThumbnails`.
- `core/data/prefs/UiPreferences.kt` — three new keys. Presets **reuse the existing
  `StoredSceneFilter`** (make it `internal`, add `sort: String?`) — do not write a second mapper.
  Decode defensively (`runCatching { … }.getOrNull() ?: emptyList()`), matching how
  `defaultSceneFilter` already survives corruption. Cap presets at 20, recents at 8.
- `core/ui/.../image/StashImageLoader.kt` — honour `imageCacheSizeMb`.

Tests (plain JUnit 5):
- `SceneFilterActiveCountTest` — pair-grouping is the thing that will be got wrong.
- `UiPreferencesPresetTest` — JSON round-trip, and a corrupt blob returns empty rather than crashing.

### Phase 3 — filter sheet (`feature/library/.../FilterSheet.kt`)
Sticky header / scrolling body / sticky footer, header and footer **outside** the `verticalScroll`.
Header: `FILTERS` + `N ACTIVE` badge (hidden entirely at zero) + `CLEAR ALL`.
Body order: PRESETS → SORT BY → DURATION → RELEASE DATE → MIN RESOLUTION → ORIENTATION →
RATING (MINIMUM) → PLAY COUNT / O-COUNTER side by side → FLAGS → ALL STASH FILTERS (collapsed, D1).
Footer: `Reset` · `Save view` · `Apply`, built as clickable `Surface`s at 6dp radius, not M3 pills.

Delete: `SectionTitle`, `SortDropdown`, `DateChips`, `ResolutionChips`, `OrientationChips`,
`RatingRange`, `IntMinSlider`, `ToggleChip`.

**Keep exactly as written** — this logic is correct and load-bearing: `DurationSection`'s latching
and `hasBoundsWithoutPreset` derivation, `DurationCustomRange`'s decoupled `minText`/`maxText`
string state and digit filtering, `currentDurationBucket`, `currentDateBucket`, `datesFor`,
`withoutCriterion`, `clearQuickFields`. Only their rendering changes.

Commit-on-Apply stays. No live result count on Apply — explicit product decision.
`Save view` opens a name prompt and does **not** apply or dismiss.
Rating becomes a minimum: set `minRating100`, `maxRating100 = null`. Keep the `maxRating100` field
on `SceneFilter` — other callers may set it.

### Phase 4 — settings (`feature/settings/.../SettingsScreen.kt`)
One 18dp gutter throughout. `SectionLabel` + a single bordered surface — the three competing
container languages (16dp server card / 6dp `DetailGroup` / bordered `NavRow` with 32dp icon tiles) go.

Sections: SERVER · ACCENT (three equal thirds, selected in that palette's own colour) ·
GRID COLUMNS rail · SAVED FILTER PRESETS · DOUBLE-TAP SEEK rail · SCRUB SENSITIVITY (CSlider with
−/+ ends, bubble moved up to the label row) · THUMBNAIL CACHE rail with a real `N MB in use ·
clear cache` read from Coil's disk cache · **BLUR THUMBNAILS switch (D5)** · APP · DANGER ZONE.

- Off-scale persisted seek values (e.g. 22 from the old slider) select nothing and snap on first
  interaction. Do not crash, do not silently rewrite on load.
- DANGER ZONE **gets a confirmation dialog**. Today one tap disconnects — the single destructive
  action on the screen with no guard.
- `SettingsViewModel` gains `presets`, `deletePreset`, `renamePreset`, and a `serverInfo` fetch
  timestamp so `synced 2m ago` is real.
- **Do not delete** `SpineSwitch`, `ChipRow`, `DetailGroup` — they are `internal` and still imported
  by `SettingsServerScreen.kt` and `SettingsAboutScreen.kt`. They stop being used *here* only.

### Phase 5 — search (asks 2 and 3)
New `SearchViewModel` (feature/library): scenes via `sceneRepository.scenes(SceneQuery(searchText = q), limit = 20)`,
performers/studios/tags via `browseRepository.filterOptions(kind, q)`. Copy `FilterOptionsViewModel`'s
200ms-debounce + job-cancel shape.

`SearchOverlay.kt` rewritten to `3c`: scope rail with live per-scope counts (`all 24 · scn 18 · stu 1 ·
prf 4 · tag 2`), top-result card (exact-or-prefix title match, omit the section when ambiguous),
accent-25% query highlight via `AnnotatedString` `SpanStyle` (case-insensitive, all occurrences),
bordered scene rows with **real Coil `AsyncImage`** and the duration badge lifted from `SceneCard.kt`
rather than rewritten, studios + tags sections (declared in `SearchResults` today but never rendered),
`see all N scenes →` (only when truncated; applies the query to the library and dismisses),
recent chips when the query is blank. Meta line goes `OnSurfaceVariant` — the studio is currently
accent and competes with selection state. Tags use `SpineColors.AccentCool`, deliberately.

`LibraryScreen.kt`: wire `SpineSearchBar` → the overlay → `viewModel.setSearchText`, pass the active
filter count into `FilterSheet`, wire `onSavePreset` / `onApplyPreset`. **Enlarge the bar** (ask 3):
9sp `MonoSmall` → 12sp mono, 8dp → 11dp vertical padding, 14dp → 16dp icon, matching the overlay
field. Drop the `⌘K` badge (D6). Note the current `.clickable` sits *after* `.padding`, so the hit
area is inset — fix that while you are in there.

### Phase 6 — colour audit (ask 4)
Apply the D4 fixes listed under "Accent bugs" above. Produce
`.planning/COLOR-AUDIT.md`: every finding, file:line, fixed vs. deliberate, with the reason.

### Phase 7 — verify on device
Build, install, walk all four screens including the **untouched/empty state** from `3a` (steppers
read `any` with a dimmed −, star row is five outlines, rails on `any`, no flag lit, badge hidden) —
the spec calls this out as the thing to check before declaring the screen done.
Cycle sage → ember → signal to prove the accent fix. Screenshots for each.
`ktlint` + `detekt` + `test` green before calling it done.

## Device and server

- Pixel 9 Pro (`caiman`) on network adb at `100.115.140.62:36055`. Screen 960×2142.
- Extend Unlock is on, so the device is fully scriptable with no PIN:
  ```
  adb shell input keyevent KEYCODE_WAKEUP
  adb shell input swipe 480 1900 480 200 250    # past keyguard
  adb exec-out screencap -p > shot.png
  ```
- Installed package is `io.stashapp.android.debug`, v0.2.0-alpha (versionCode 5).
- Stash server credentials are in `.secrets/stash.env` (gitignored, **never commit**).
  Verified live: HTTP 200, 1628 scenes / 1084 performers / 1617 tags — real data to test search
  against.
- Build: `./gradlew :app:assembleDebug`, APKs land in `app/build/outputs/apk/debug/`
  (`app-arm64-v8a-debug.apk` for this device).

## Constraints

- No new third-party UI dependencies. Compose + M3 + the existing design system only.
- No `FilterChip`, `Slider` (except inside `CSlider`), `RangeSlider`, `ExposedDropdownMenuBox` or
  bare `OutlinedTextField` in these three screens after the change. `ModalBottomSheet`, `TopAppBar`,
  `Surface`, `LazyColumn` stay.
- Never hardcode accent — `LocalAccentColors.current` everywhere, so ember/signal keep working.
  The mock shows sage; that is not a licence to hardcode it.
- Touch targets: chips and segments render 30–34dp tall but need a 44dp clickable. Grow the ripple
  area, not the visual box.
- Accessibility: `Modifier.semantics { selected = … }` on every chip, segment and star;
  `contentDescription` on the stepper buttons and on tri-state chips
  ("Organized, yes" / "no" / "any").
- Existing smoke tests must keep passing unchanged.
