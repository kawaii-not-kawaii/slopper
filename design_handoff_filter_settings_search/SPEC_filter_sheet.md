# Spec — Filter sheet (`2a` default, `3a` custom-duration)

File: `feature/library/src/main/java/io/stashapp/android/feature/library/FilterSheet.kt`

## Purpose
Narrow the library grid. Commit-on-Apply: all edits are local state, nothing queries until
Apply. **This does not change** — no live result count, by explicit product decision.

## Shell
`ModalBottomSheet` as today (`containerColor = SpineColors.Bg`,
`contentWindowInsets = { WindowInsets.navigationBars }`), with:
- `fillMaxHeight(0.84f)` → **0.86f**
- `shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)`
- a 1dp `SpineColors.BorderStrong` top edge
- drag handle: 28×3dp, `OnSurfaceFaint`, 8dp top / 2dp bottom padding
  (override `dragHandle` — the M3 default is 32×4dp at 40% white)

Three regions, header and footer **outside** the `verticalScroll`:

### Header — 10dp top / 12dp bottom, 18dp gutter, 1dp `Border` bottom rule
`FILTERS` (`MetaMono` 11sp SemiBold, ls 1.2, `OnSurface`) ·
active-count badge · `Spacer(weight(1f))` · `CLEAR ALL` (`MetaMono` 10sp,
`OnSurfaceVariant`, resets filter + sort like today's Reset).

Badge: `3 ACTIVE`, `MonoSmall` SemiBold ls 0.6, accent text, accent-10% fill,
accent-30% 1dp border, 4dp radius, 3dp × 6dp padding. **Hide it entirely at zero** — do
not render `0 ACTIVE`.

Add to `SceneFilter` in `core/domain/.../SceneRepository.kt`, beside the existing
`isActive`:

```kotlin
val activeCount: Int
    get() = listOf(
        minResolution, minRating100, maxRating100, organized, hasMarkers, interactive,
        hasResumeTime, minDurationSeconds, maxDurationSeconds, minDate, maxDate,
        minPlayCount, maxPlayCount, minOCounter, maxOCounter, orientation, hasCaptions,
    ).count { it != null } +
        listOf(performerIds, studioIds, tagIds).count { it.isNotEmpty() }
```

Count min/max **pairs** as one where they are one control: duration min+max is one
facet, rating min+max is one. Adjust the expression to group them, or the badge will
read 2 for a single duration bucket. Unit-test this.

### Body — `verticalScroll`, 18dp gutter, 14dp top, `Arrangement.spacedBy(18.dp)`
Sections in order. Each is `SectionLabel` + control, 8dp apart.

**1 · PRESETS** — `FlowRow(spacedBy(6.dp))` of `SpineChip`s, one per saved preset,
selected when the current filter+sort equals it, then a `dashed` `+ save current` chip.
Tapping a preset replaces filter and sort wholesale. See `SPEC_presets.md`.

**2 · SORT BY** — replaces `ExposedDropdownMenuBox` + `OutlinedTextField`. A bordered
`Surface` row, `padding(horizontal = 12.dp, vertical = 11.dp)`: current label
(`MetaMono` 12sp Medium, `OnSurface`) · `Spacer(weight(1f))` ·
`Icons.Outlined.KeyboardArrowDown` 14dp, `OnSurfaceMuted`.
Tapping opens a plain `DropdownMenu` anchored to the row, listing `SceneSort.entries` by
`label`; style items `MetaMono` 11sp on `SurfaceHigh`. Nine options, so a menu is right —
do not make this a rail.

**3 · DURATION** — `FlowRow(spacedBy(6.dp))`: `Any`, then `SceneDurationBucket.entries`,
then `custom`. Labels shorten to `<5m · 5–15m · 15–30m · 30–60m · 1–2h · 2h+`
(`SceneDurationBucket.label` currently reads "Under 5m" / "Over 2h" — either change the
enum labels or map them in the sheet; changing the enum also affects any other consumer,
so prefer a local `shortLabel` mapping).

Keep `DurationSection`'s latching logic and `hasBoundsWithoutPreset` derivation **exactly
as written** — it is correct. Only the chips change to `SpineChip`.

`3a` shows the expanded custom state. `DurationCustomRange` keeps its decoupled
`minText`/`maxText` string state and digit filtering; restyle the two
`OutlinedTextField`s as bordered `Surface` boxes, `weight(1f)`, separated by a `–`
(`MetaMono` 12sp `OnSurfaceMuted`):
- stacked label `MIN · MINUTES` / `MAX · MINUTES` — `MonoSmall` 8.5sp ls 0.8, `OnSurfaceMuted`
- value below, 5dp gap, `MetaMono` 13sp Medium `OnSurface`; empty renders `no limit` in `OnSurfaceMuted`
- focused box gets the accent-45% border; unfocused `Border`
- `padding(horizontal = 11.dp, vertical = 9.dp)`, `KeyboardType.Number`, still digits-only ≤4 chars
- below the row, a `MonoSmall` `OnSurfaceVariant` line: `18m and longer` (or `18–45m`, or
  `up to 45m`) — derived from the current bounds

**4 · RELEASE DATE** — `SegmentedRail`: `any · week · month · year · 2026`
(the last label is `LocalDate.now().year`). Values map to `DateBucket` via the existing
`datesFor()`; keep `currentDateBucket` for the reverse lookup. `2a` omits this section
for space — include it, between DURATION and MIN RESOLUTION, as shown in `3a`.

**5 · MIN RESOLUTION** — `SegmentedRail`: `any · 480 · 720 · 1080 · 1440 · 4K · 8K`,
plus a `MonoSmall` `OnSurfaceVariant` caption below: `1080p and above`.
(5K/6K dropped — see the note in `SPEC_components.md`.)

**6 · ORIENTATION** — `SegmentedRail`: `any · landscape · portrait · square`. Not shown in
the mock; it exists today, keep it here in rail form.

**7 · RATING — MINIMUM** — `StarRatingPicker`.

**8 · PLAY COUNT / O-COUNTER** — a `Row(spacedBy(10.dp))` of two `weight(1f)` columns, each
`SectionLabel` + `StepperField`. Halves the vertical space of the two old sliders.

**9 · FLAGS** — `SectionLabel("FLAGS — TAP TO CYCLE YES / NO / ANY")` +
`FlowRow(spacedBy(6.dp))` of five `SpineTriStateChip`s: Organized, Has markers,
Interactive, In progress, Has captions — same `SceneFilter` fields as today.
16dp bottom padding so the last row clears the footer.

### Footer — sticky, 12dp top / 16dp bottom, 18dp gutter, 1dp `Border` top rule, `Bg` fill
`Reset` (`MetaMono` 11sp `OnSurfaceMuted`, resets to `SceneFilter()` + `SceneSort.DateDesc`)
· `Spacer(weight(1f))` · `Save view` (outlined: 1dp `Border`, `OnSurface` text, 6dp radius,
11dp × 14dp padding) · `Apply` (filled `accent.primary` / `accent.onPrimary`, 6dp radius,
11dp × 20dp, `SpaceGrotesk` 12sp SemiBold).

`Save view` opens a name prompt and calls `savePreset(name)` — it does **not** apply or
dismiss. The current code has a comment saying "No Save view per D-01 Q4"; that decision is
reversed here, so remove the comment. Note the footer buttons are 6dp radius rectangles,
not M3 pills — build them as clickable `Surface`s, not `Button`/`TextButton`.

## Empty / untouched state (`3a`)
Nothing is pre-selected and no control fakes activity: steppers read `any` with a dimmed
`−`, the star row is five outlines with a plain `any` pill, rails sit on `any`, no flag
chip is lit, and the header badge is hidden. This is the state a first-time user sees;
verify it against `3a` before calling the screen done.

## Interaction
- All local state, commit on Apply — unchanged.
- `CLEAR ALL` and `Reset` do the same thing; keep both (one in reach at the top of a long
  scroll, one in the footer).
- No animation beyond the sheet's own; the custom-duration row appears with the default
  `AnimatedVisibility` fade if you want one, ≤150ms.
