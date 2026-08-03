# Spec — Settings (`3b`)

File: `feature/settings/src/main/java/io/stashapp/android/feature/settings/SettingsScreen.kt`

## Problem being fixed
Three container languages on one screen: a 16dp-radius server card, 6dp `DetailGroup`
cards, and bordered `NavRow`s that each carry their own 32dp icon tile. Gutters alternate
between 14dp and 16dp. Two of the four sliders control values that are discrete.

## Structure
Keep `LazyColumn(contentPadding = PaddingValues(bottom = 100.dp))`. One gutter
throughout: **18dp**. Sections separated by 18dp top padding, label 8dp above its control.
No `DetailGroup` wrapper on any of the sections below.

**Header** — `TopAppBar`, `containerColor = SpineColors.Bg`, 1dp `Border` bottom rule.
Title `SETTINGS` in `MetaMono` 11sp SemiBold ls 1.2 `OnSurface` (was 18sp Space Grotesk —
this matches the filter sheet's `FILTERS`). Back arrow 18dp. Trailing: 6dp `Success` dot +
`synced 2m ago` in `MetaMono` `OnSurfaceVariant` — plain, no pill `Surface`.
Derive the relative time from the `serverInfo` fetch; `offline` in `OnSurfaceMuted` when
`serverInfo == null`.

**SERVER** — one bordered `Surface` row, `padding(12.dp)`, clickable → `onServerClick`:
`basement-nas:9999` (`MetaMono` 12.5sp Medium `OnSurface`) over
`stash v0.28.1 · 4,812 scenes · 8ms` (`MonoSmall` `OnSurfaceVariant`, 5dp gap),
trailing 14dp chevron `OnSurfaceMuted`. No 40dp accent icon tile, no 16dp radius.
Use `activeServer?.displayName`; `Not connected` when null (row not clickable, as today).

**ACCENT** — `Row(spacedBy(8.dp))`, three `weight(1f)` chips
(bordered `Surface`, `padding(horizontal = 10.dp, vertical = 9.dp)`): a 16dp rounded-4dp
swatch + the palette name in `MetaMono` 11sp. Selected uses the standard selection
treatment **in that palette's own colour** (sage/ember/signal), as today's code already
does via `SageAccent.primary` etc. Drop the 28dp tile + check-mark layout.

**GRID COLUMNS** — `SegmentedRail`: `auto · 2 · 3 · 4`, bound to
`up.gridColumns` / `setGridColumns` (string values `"auto" "2" "3" "4"`, unchanged).
Replaces the `DRowStacked` + `ChipRow`.

**SAVED FILTER PRESETS** — `FlowRow(spacedBy(6.dp))` of `SpineChip`s (label only, none
selected — this is a management surface, not a picker) plus a `dashed` `manage…` chip.
Tapping a preset chip opens rename/delete; `manage…` opens the full list. See
`SPEC_presets.md`.

**DOUBLE-TAP SEEK** — `SegmentedRail`: `5s · 10s · 15s · 30s · 60s`, bound to
`pp.doubleTapSeekSeconds` / `setDoubleTapSeekSeconds`. Replaces the 5–60 `CSlider`.
Off-scale persisted values (e.g. 22 from the old slider) should select nothing and be
snapped to the nearest option on first interaction — do not crash or silently rewrite on load.

**SCRUB SENSITIVITY** — genuinely continuous, so keep `CSlider`, but:
- move the value bubble up next to the section label, right-aligned
  (`SectionLabel(trailing = { … })`), reading `120 ms/px`
- put the track in a bordered `Surface` row with a 38dp `−` cell and a 38dp `+` cell at the
  ends (1dp `Border` dividers), each stepping by 10
- track: 2dp, `SurfaceHigh` inactive / accent active, 10dp **square** thumb (2dp radius) —
  matches the mock; `SliderDefaults.colors` with a custom `thumb` slot
- caption below: `drag the rail, or step by 10 with the ends` (`MonoSmall` `OnSurfaceVariant`)
- range stays `PlayerPreferences.SEEK_MS_PER_PX_MIN..MAX`

If the −/+ ends are more work than they are worth, ship `CSlider` with the bubble moved to
the label row and file the steppers as a follow-up; that alone fixes most of the problem.

**THUMBNAIL CACHE** — `SegmentedRail`: `64 · 128 · 256 · 384 · 512` (MB) with the value
bubble `256 MB` on the label row, bound to `up.imageCacheSizeMb` (range today is 64–512,
so these are the sensible stops). Caption below:
`118 MB in use · clear cache` — the second half is a clickable accent-free
`OnSurfaceVariant` link. **`118 MB in use` and `clear cache` do not exist yet**: read the
Coil disk-cache size from `StashImageLoader` (`core/ui/.../image/StashImageLoader.kt`) and
add a clear action, or omit the caption in v1 and file it.

**APP** — two rows, 6dp apart, each a bordered `Surface`, `padding(12.dp)`, no icon tile:
label `SpaceGrotesk` 12.5sp `OnSurface` · `Spacer(weight(1f))` · mono value
`OnSurfaceVariant` · 14dp chevron `OnSurfaceMuted`.
- `Server & connection` → `api key set` → `onServerClick`
- `About & diagnostics` → `v0.2.0-alpha` → `onAboutClick`

**DANGER ZONE** — one row, `Error`-tinted: fill `Error` 6%, border 1dp `Error` 30%,
16dp `Icons.Outlined.PowerOff` in `Error`, label `Disconnect server` in `Error`
`SpaceGrotesk` 12.5sp Medium. No chevron, no icon tile.
Calls `viewModel.disconnect(onDisconnected)` — **add a confirmation dialog first**; today
it disconnects on a single tap, which is the one destructive action on the screen.
36dp bottom padding.

## Do not delete
`SpineSwitch`, `ChipRow` and `DetailGroup` are `internal` in this file and imported by
`SettingsServerScreen.kt` and `SettingsAboutScreen.kt`. They stop being used *here* but must
stay. Only the private `SectionLabel` and `NavRow` move/change.

## State
No new ViewModel state except presets. Everything else already exists on
`SettingsViewModel`: `activeServer`, `serverInfo`, `playerPrefs.doubleTapSeekSeconds`,
`playerPrefs.seekMsPerPx`, `uiPrefs.accentPalette`, `uiPrefs.gridColumns`,
`uiPrefs.imageCacheSizeMb`, and the `setPlayer {}` / `setUi {}` helpers.
`synced 2m ago` needs a timestamp — store the `serverInfo` fetch time in the ViewModel.
