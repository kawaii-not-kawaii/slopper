# Handoff: Filter sheet, Settings and Search — Spine restyle

## Overview
The library filter sheet, the settings screen and the search overlay currently use stock
Material 3 controls (`FilterChip`, `Slider`, `RangeSlider`, `OutlinedTextField`,
`ExposedDropdownMenuBox`) rendered on the Spine dark scheme. They read as unfinished next
to the rest of the app: M3 selected-chip state resolves to `secondaryContainer`
(#4A75B6 cool blue) which appears nowhere else in Spine, the filter sheet scrolls ~2.5
screens with no summary of what is active, and continuous sliders are used for values that
are discrete (rating stars, play counts, cache size, seek seconds).

This handoff replaces those controls with a small set of Spine-native primitives and
restructures the three screens around them. It also adds **saved filter presets**, which
the data layer does not support yet.

## About the design files
The files in this bundle are **design references created in HTML** — prototypes of the
intended look and behaviour, not production code to copy. The target codebase is an
existing **Android / Jetpack Compose + Material 3** app (Kotlin, multi-module, Hilt,
DataStore, Paging 3). Recreate the designs there using the app's own patterns: Compose
composables in the existing feature modules, tokens from `core:designsystem`, no new
third-party UI libraries.

Open `Slopper Filter and Settings.dc.html` in a browser. It is a canvas of options
grouped in turns, newest at the top. **Turn 3 (`3a`, `3b`, `3c`) plus `2a` are the
approved direction.** Turns 1 and 2 are kept for context: turn 1 is a recreation of the
current build, turn 2 the alternatives that were rejected.

| id | Screen | Status |
|----|--------|--------|
| `2a` | Filter sheet — default state | **Approved — build this** |
| `3a` | Filter sheet — custom duration + untouched controls | **Approved — build this** |
| `3b` | Settings | **Approved — build this** |
| `3c` | Search overlay | **Approved — build this** |
| `1a`/`1b`/`1c` | Current build, recreated | Reference / before |
| `2b`–`2g` | Rejected alternatives | Context only |

## Fidelity
**High fidelity.** Colours, type ramp, spacing and radii are all real Spine tokens taken
from `core/designsystem/.../theme/Color.kt` and `Type.kt`. Match them exactly — but use
the Kotlin token objects (`SpineColors.Surface`, `MetaMono`, `LocalAccentColors.current`),
never the raw hex from the HTML. Anything accent-coloured in the mock must come from
`LocalAccentColors.current.primary` so the ember/signal palettes keep working; the mock
shows the sage palette.

The HTML uses 1px borders and px sizes at a 412×892 viewport, which is 1dp ≈ 1px at
mdpi — read every px in the mock as **dp**.

## Reading order
1. `README.md` (this file) — direction, tokens, cross-cutting rules
2. `FILE_MAP.md` — every file to create or modify, with the reason
3. `SPEC_components.md` — the five new design-system primitives, in full detail
4. `SPEC_filter_sheet.md` — `2a` / `3a`
5. `SPEC_settings.md` — `3b`
6. `SPEC_search.md` — `3c`
7. `SPEC_presets.md` — saved filter presets (new data-layer work)

## The system, in one paragraph
Every control in all three screens is built from eight parts, and nothing else:
**(1)** a section label — `MetaMono`, uppercase, SemiBold, `letterSpacing = 1.sp`,
`SpineColors.OnSurfaceMuted`, 8dp below it;
**(2)** a control surface — `SpineColors.Surface`, 1dp `SpineColors.Border`,
`ShapeSmall` (6dp);
**(3)** a chip — same surface, 7dp × 10dp padding, `MetaMono` at 11sp;
**(4)** a segmented rail for ordered/enumerable choices — one bordered row, equal-weight
segments, 1dp dividers;
**(5)** a stepper for counts — −/value/+ in one bordered row;
**(6)** the value bubble from `CSlider` (accent 8% fill / 25% border / 4dp radius) for
anything continuous;
**(7)** a star row for ratings;
**(8)** sticky sheet header (title + active count + clear-all) and footer
(Reset · Save view · Apply).
Selection is always the same three things at once: accent 12% fill, accent 45% border,
accent label at SemiBold. Nothing else in these screens is accent-coloured except the
filled Apply button.

## Cross-cutting rules
- **Restrained accent.** Selection state and the single primary action only. Metadata,
  counts and hints are `OnSurfaceVariant` (#8C95A8) or `OnSurfaceMuted` (#525B6E) — never
  accent. `OnSurfaceFaint` (#2F3645) is for hairlines, inactive slider ticks and unfilled
  star outlines only; never for text.
- **Unset must not look set.** An untouched stepper reads `any` in `OnSurfaceMuted` with a
  dimmed (`OnSurfaceFaint`) minus; an unrated star row is five outline stars. See `3a`.
- **Mono for values, Space Grotesk for prose.** Every number, enum value, size, duration
  and count is `JetBrainsMono`. Row labels and titles are `SpaceGrotesk`.
- **No M3 chrome.** No `FilterChip`, `Slider` (except inside `CSlider`), `RangeSlider`,
  `ExposedDropdownMenuBox` or bare `OutlinedTextField` in these three screens after this
  change. `ModalBottomSheet`, `TopAppBar`, `Surface`, `LazyColumn` stay.
- **Touch targets.** Chips and segments render at 30–34dp tall; wrap each in a
  `Modifier.heightIn(min = 44.dp)` clickable, or add `Modifier.padding` inside the
  clickable so the ripple area reaches 44dp. Do not grow the visual box.
- **Accessibility.** Selection is currently signalled by colour + weight. Add
  `Modifier.semantics { selected = true }` (or `toggleable`) on every chip, segment and
  star so TalkBack announces state, and `contentDescription` on the −/+ stepper buttons.

## Design tokens (all already exist — do not add new ones)
Colours — `io.stashapp.android.core.designsystem.theme.SpineColors`:

| Token | Hex | Used for |
|---|---|---|
| `Bg` | #0A0D12 | screen and sheet background |
| `Surface` | #11151C | every control surface, chip, result row |
| `SurfaceHigh` | #1A2030 | thumbnail placeholders, inactive slider track |
| `SurfaceTop` | #232B3D | duration badge on thumbnails |
| `OnSurface` | #EAEEF6 | titles, row labels, entered values |
| `OnSurfaceVariant` | #8C95A8 | unselected control text, metadata, hints |
| `OnSurfaceMuted` | #525B6E | section labels, "any", secondary counts |
| `OnSurfaceFaint` | #2F3645 | hairlines, unfilled stars, disabled − |
| `AccentCool` | #7FB6FF | tag names in search only |
| `Error` | #FF5860 | danger zone, "no" flag state |
| `Success` | #5DBB63 | synced dot |
| `Border` | #A0B4DC @ 10% | every 1dp control border |
| `BorderStrong` | #A0B4DC @ 22% | sheet top edge only |

Accent (never hardcode — `LocalAccentColors.current`): `primary` = #9DC83C sage /
#E5A742 ember / #4FD0E6 signal, `onPrimary` = #0B1402.
Accent alphas used: **0.12** selected fill, **0.45** selected border, **0.08** value-bubble
fill, **0.25** value-bubble border and search-match highlight, **0.30** top-result border.

Type — `theme/Type.kt`: `MetaMono` (10sp, ls 0.6), `MonoSmall` (9sp, ls 0.5),
`bodyMedium` (Space Grotesk 13sp), `titleSmall` (13sp Medium), `titleLarge` (18sp
SemiBold). Where the mock shows 11sp/12.5sp mono, use `MetaMono.copy(fontSize = 11.sp)`.

Radii: `ShapeSmall` = 6dp for everything; 4dp for the value bubble and thumbnail
duration badge; 16dp top corners on the sheet; 999dp for the synced pill.
Spacing scale in use: 2, 4, 6, 8, 10, 12, 14, 18, 20dp. Screen gutter is **18dp**
(the current settings screen uses 14/16dp inconsistently — unify on 18dp).

## Assets
None. All icons are existing `androidx.compose.material.icons.Icons.Outlined` /
`AutoMirrored.Outlined` vectors — the SVGs in the HTML are stand-ins drawn to match
them (ArrowBack, ArrowForwardIos, KeyboardArrowDown, Check, Star/StarOutline, Search,
Close, Add, Remove, Cast, Info, Bolt, PowerOff, LockOpen). No new drawables.
Thumbnails in the mock are diagonal-hatch placeholders; the real rows use the existing
Coil `AsyncImage` path from `SceneCard`.

## Files in this bundle
- `Slopper Filter and Settings.dc.html` — the design canvas (open this first)
- `screens/` — 2× PNGs of the four approved screens plus the three "before" screens (see `screens/README.md`)
- `android-frame.jsx`, `support.js` — runtime for the HTML; not part of the design
- `FILE_MAP.md`, `SPEC_components.md`, `SPEC_filter_sheet.md`, `SPEC_settings.md`,
  `SPEC_search.md`, `SPEC_presets.md`

## Out of scope
Settings → Server (`SettingsServerScreen.kt`) and Settings → About
(`SettingsAboutScreen.kt`) are unchanged; they are reached from the App group and will
inherit the new primitives later. The player, detail, home and browse screens are
untouched.
