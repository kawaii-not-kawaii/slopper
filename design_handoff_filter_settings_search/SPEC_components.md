# Spec — new design-system primitives

Five files in
`core/designsystem/src/main/java/io/stashapp/android/core/designsystem/component/`.
Follow the conventions already in `CSlider.kt`: public composable, KDoc with a usage
block, `modifier: Modifier = Modifier` as the first optional param, accent from
`LocalAccentColors.current`, everything else from `SpineColors`.

Shared selection treatment — define it once and reuse:

```
selected:    background accent.primary.copy(alpha = 0.12f)
             border     1.dp, accent.primary.copy(alpha = 0.45f)
             text       accent.primary, FontWeight.SemiBold
unselected:  background SpineColors.Surface
             border     1.dp, SpineColors.Border
             text       SpineColors.OnSurfaceVariant, FontWeight.Normal
shape:       ShapeSmall (6.dp)
```

---

## 1 · `SpineChip.kt`

```kotlin
@Composable
fun SpineChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: @Composable (() -> Unit)? = null,
    dashed: Boolean = false,
)
```

- Padding `horizontal = 10.dp, vertical = 7.dp`; text `MetaMono.copy(fontSize = 11.sp)`.
- `leading` renders before the label with a 6dp gap — used for the flag state dot.
- `dashed = true`: no fill, 1dp dashed border at `Border` 22%, text `OnSurfaceMuted`.
  Used for `+ save current` and `manage…`. Compose has no dashed `BorderStroke` helper —
  use `BorderStroke(1.dp, SolidColor(...))` with a `PathEffect.dashPathEffect` drawn via
  `Modifier.drawBehind`, or accept a solid 22% border if that is too fussy.
- Wrap the clickable in `Modifier.heightIn(min = 44.dp)` with the visual box centred, or
  set `Modifier.semantics { role = Role.Button }` plus outer padding. Visual height stays 30dp.
- Add `Modifier.semantics { this.selected = selected }`.

```kotlin
@Composable
fun SpineTriStateChip(
    label: String,
    state: Boolean?,          // null = any, true = yes, false = no
    onChange: (Boolean?) -> Unit,
    modifier: Modifier = Modifier,
)
```

Cycles `null → true → false → null` (same order as today's `ToggleChip`).

| state | fill | border | text | leading |
|---|---|---|---|---|
| `null` | Surface | Border | OnSurfaceVariant | none |
| `true` | accent 12% | accent 45% | accent, SemiBold | 5dp accent dot |
| `false` | Error 10% | Error 35% | Error, SemiBold | 8×1.5dp Error bar |

The label text does **not** change (today it becomes "Organized: yes" — drop that; the
dot/bar carries the state). Section label above the row reads
`FLAGS — TAP TO CYCLE YES / NO / ANY`.
`contentDescription` must still say "Organized, yes" / "Organized, no" / "Organized, any".

---

## 2 · `SegmentedRail.kt`

```kotlin
@Composable
fun <T> SegmentedRail(
    options: List<Pair<String, T>>,   // label to value
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
)
```

One `Row` inside a `Surface(color = SpineColors.Surface, shape = ShapeSmall,
border = BorderStroke(1.dp, SpineColors.Border))`, `clip(ShapeSmall)`.
Each segment is `Modifier.weight(1f)`, centred, `padding(vertical = 9.dp)`,
`MetaMono.copy(fontSize = 10.5.sp)`.

- Dividers: a 1dp `SpineColors.Border` `VerticalDivider` between segments.
- Selected segment: accent 12% fill, accent-35% 1dp divider on **both** of its own edges
  (this is what makes it read as a pressed key rather than a highlight), text accent
  SemiBold. Suppress the neighbouring plain dividers so they do not double up.
- Height comes out at ~34dp; give the whole rail `Modifier.heightIn(min = 44.dp)` only if
  you can keep the segments vertically centred — otherwise leave 34dp and accept it, since
  segments are wide.
- 7 segments is the practical maximum at 412dp width (the resolution rail:
  `any 480 720 1080 1440 4K 8K` — note the mock **drops 5K and 6K**; keep those two out of
  the rail and out of the filter, or fold them into a "custom" affordance. Confirm with
  the designer before removing them from `SceneResolution`).
- Labels are terse: `1080` not `1080p`, `month` not `Last month`, `10s` not `10 seconds`.

---

## 3 · `StepperField.kt`

```kotlin
@Composable
fun StepperField(
    value: Int?,                       // null = "any"
    onChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    min: Int = 1,
    max: Int = 99,
    valueLabel: (Int) -> String = { "≥ \$it" },
    anyLabel: String = "any",
)
```

One bordered `Surface` `Row`, three cells:
- **−** 34dp wide, 1dp `Border` divider on its right, `Icons.Outlined.Remove` at 12dp.
  Tint `OnSurfaceVariant`; when `value == null` tint `OnSurfaceFaint` and disable.
- **value** `weight(1f)`, centred. `null` → `anyLabel` in `MetaMono.copy(fontSize = 11.sp)`,
  `OnSurfaceMuted`, Normal. Non-null → `valueLabel(value)` in `OnSurface`, SemiBold.
- **+** 34dp, 1dp divider on its left, `Icons.Outlined.Add`, tint `OnSurfaceVariant`.

Behaviour: `+` from `null` → `min`; `−` from `min` → `null`; clamp at `max`.
Long-press to repeat is a nice-to-have, not required.
Used for Play count (`max = 50`) and O-counter (`max = 20`) — the same ranges the current
`IntMinSlider` uses. The whole row is 44dp tall, so touch targets are fine.

---

## 4 · `StarRatingPicker.kt`

```kotlin
@Composable
fun StarRatingPicker(
    rating100: Int?,                  // domain value, 0..100, null = any
    onChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
)
```

Row inside a bordered `Surface`, `padding(horizontal = 12.dp, vertical = 11.dp)`:
- Five 17dp stars, 6dp gap. Filled = `Icons.Filled.Star` tinted accent;
  empty = `Icons.Outlined.StarBorder` tinted `OnSurfaceFaint`.
- `Spacer(weight(1f))`, then the value bubble on the right — same treatment as `CSlider`'s
  (accent 8% fill, accent 25% border, 4dp radius, `MonoSmall` SemiBold accent).
  Label `★ 3.0+` when set, `any` when null (and in the null case use `OnSurfaceVariant`
  text with a plain `Border` outline, **not** the accent bubble — see `3a`).
- Tap star *n* → `rating100 = n * 20`. Tap the star that is already the current value →
  clear to `null`. Half-steps: tapping the left half of star *n* sets `n * 20 - 10`
  (0.5 granularity, matching the 0–100 domain scale in `SceneFilter.minRating100`).
  If half-star hit-testing is awkward, ship whole stars and file a follow-up.

**Domain note:** this replaces a *range* (`minRating100` + `maxRating100`) with a
*minimum*. On apply set `minRating100 = value` and `maxRating100 = null`. `SceneFilter`
keeps both fields — do not remove `maxRating100`, other callers may set it — but the sheet
no longer edits the upper bound. Section label becomes `RATING — MINIMUM`.

---

## 5 · `SectionLabel.kt`

```kotlin
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,   // count, rule line, "clear"
)
```

`text.uppercase()`, `MetaMono.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)`,
`SpineColors.OnSurfaceMuted`, 8dp bottom padding.
This is exactly the private `SectionLabel` in `SettingsScreen.kt` today — lift it verbatim,
add `trailing`, and delete the private copy plus `FilterSheet.kt`'s `SectionTitle`
(which is 13sp Space Grotesk — the inconsistency this fixes).

In search, `trailing` carries the count and a 1dp `Border` rule filling the remaining
width: `SCENES  18 ─────────────`.
