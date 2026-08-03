# Spec — Search overlay (`3c`)

File: `feature/library/src/main/java/io/stashapp/android/feature/library/SearchOverlay.kt`

## Data caveat — read first
The file's own header says search data flow is deferred and callers pass empty results.
`SearchResults` already declares `scenes`, `performerNames`, `studioNames`, `tagNames`,
but only scenes and performers render, and the Studios/Tags scopes show nothing.
This spec covers **presentation**. Wiring the query (a `SearchViewModel` or new
`LibraryViewModel` methods hitting `SceneRepository` + performer/studio/tag lookups) is
separate work — build the layout against the existing `SearchResults` shape so it lights up
when the data lands, and keep the existing empty state.

## Shell
Unchanged: `AnimatedVisibility` with `fadeIn() + slideInVertically { -it / 4 }`,
full-screen `Box` on `SpineColors.Bg`. 18dp gutter throughout (currently 8/12/18 mixed).

**Top bar** — `Row`, `padding(horizontal = 18.dp, vertical = 12.dp)`, 10dp gap:
18dp back arrow, then the field filling the rest. Keep `OutlinedTextField` (it is already
themed correctly) but:
- leading `Icons.Outlined.Search` 14dp, tinted accent when focused, `OnSurfaceVariant` when not
- trailing `Icons.Outlined.Close` 12dp `OnSurfaceVariant`, visible only when the query is non-empty
- `padding(horizontal = 12.dp, vertical = 10.dp)`, `ShapeSmall`, `MetaMono` 12sp
- focused border accent-45%, unfocused `Border` — as today

**Scope rail** — replaces the `FlowRow` of five chips with one `SegmentedRail`, full width,
18dp gutter, 14dp below:
`all 24 · scn 18 · stu 1 · prf 4 · tag 2`. Counts come from `SearchResults`; render the
abbreviation and the count in one `MetaMono` 10.5sp string, count in `OnSurfaceMuted` when
unselected. Five short labels fit; the full words do not.

## Results
**TOP RESULT** — `SectionLabel` + one bordered `Surface` card,
`padding(12.dp)`, border accent-45%: 56dp square thumbnail (`ShapeSmall`), title
`SpaceGrotesk` 14sp SemiBold `OnSurface` with the query substring highlighted, subtitle
`studio · 12 scenes · added 2y ago` in `MonoSmall` `OnSurfaceVariant`, 14dp chevron.
Only render when there is a clear best match — omit the section otherwise.

**Query highlight** — wrap matched substrings in an `AnnotatedString` `SpanStyle`:
`background = accent.primary.copy(alpha = 0.25f)`, `color = accent.primary`. Case-insensitive,
all occurrences. This is the one place accent appears in the results list.

**SCENES** — `SectionLabel("SCENES", trailing = count + rule)` then rows in a
`Column(spacedBy(6.dp))`. Each `SceneResultRow` becomes a bordered `Surface`,
`padding(8.dp)`:
- 86×50dp thumbnail, `ShapeSmall` → 4dp radius inside, `SurfaceHigh` placeholder;
  bottom-left duration badge `24:10` — `MonoSmall` 8sp SemiBold `OnSurface` on
  `SurfaceTop`, 3dp radius, 2dp × 4dp padding, 4dp inset. Lift this badge from
  `core/designsystem/.../component/SceneCard.kt` rather than rewriting it.
- title `SpaceGrotesk` 13sp Medium `OnSurface`, max 2 lines, with highlight
- meta line `lantern studio · 1080p · ★4.0` in `MonoSmall` **`OnSurfaceVariant`**
  — today the studio is `accent.primary`; remove that, it competes with selection state
- real thumbnails: use the Coil `AsyncImage` path from `SceneCard`, not the placeholder box

Then a full-width `see all N scenes →` row: 1dp `Border`, `ShapeSmall`, centred
`MetaMono` 11sp accent, `padding(vertical = 9.dp)`. Tapping it applies the query to the
library and dismisses. Render only when results are truncated.

**PERFORMERS** — `FlowRow(spacedBy(6.dp))` of chips: 18dp circle placeholder + name in
`MetaMono` 10.5sp `OnSurface`, bordered `Surface`, `padding(start = 6, end = 10, vertical = 6)`.
Close to today's `PerformerChipRow`; switch `MonoSmall` → `MetaMono` 10.5sp and 6dp gaps.

**STUDIOS** — same chip shape, no avatar. Currently unrendered — add it.

**TAGS** — `SpineChip`-shaped, unselected, but text in `SpineColors.AccentCool` (#7FB6FF).
This is the only use of the cool accent in these screens and it is deliberate: tags are
colour-coded blue across the app. Currently unrendered — add it.

**RECENT** — shown when the query is blank: `FlowRow` of transparent-fill chips
(1dp `Border`, no background) with `OnSurfaceVariant` text. Needs a recent-searches store —
a `stringPreferencesKey("recent_searches")` list in `UiPreferences`, capped at ~8. If that
is out of scope for now, omit the section rather than faking it.

**Empty state** — keep exactly as today: centred `No results for "query"` in `MetaMono`
`OnSurfaceMuted`, `padding(32.dp)`.
