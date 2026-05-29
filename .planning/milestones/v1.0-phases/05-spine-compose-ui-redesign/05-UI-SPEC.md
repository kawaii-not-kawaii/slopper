---
phase: 5
slug: spine-compose-ui-redesign
status: draft
preset: spine
created: 2026-05-17
source: design_handoff_slopper_spine/
spec_layer_registered: false
note: "Canonicalized from design handoff. Phase 5 spec-layer formal pipeline (issue, manifest, spec/discuss steps) deferred until Phase 5 starts in roadmap order. This file pre-locks the visual contract so earlier phases (especially Phase 2 COMPLY) know what NOT to fight."
---

# Phase 5 — Spine Compose UI Redesign — UI Design Contract

> Complete visual re-skin of Slopper's phone UI. All screens, modal flows, and real-content states are designed to high fidelity in `design_handoff_slopper_spine/`. This file is the canonical GSD UI design contract for Phase 5.

**Source of truth:** `design_handoff_slopper_spine/README.md` (558 lines, hi-fi).
**HTML prototype:** `design_handoff_slopper_spine/Slopper Spine.html` (visual reference, do not ship).
**JSX sources:** `screens-spine.jsx`, `spine-flows.jsx`, `spine-states.jsx`, `spine-palettes.jsx`, `theme.jsx`, `icons.jsx`, `art.jsx` (design-time references).

---

## Design System

| Property | Value |
|----------|-------|
| Platform | Android — Jetpack Compose + Material 3 |
| Theme mode | **Dark-only** (no light theme; matches existing app behavior) |
| Orientation | Phone portrait; PlayerScreen forces landscape |
| Component library | Material 3 (`androidx.compose.material3`) with custom Spine color/typography/shape tokens |
| Icon library | Material Symbols Outlined (no custom icon assets) |
| Font (UI text) | Space Grotesk (weights 300–700, Google Fonts downloadable) |
| Font (metadata) | JetBrains Mono (weights 400–600, Google Fonts downloadable) |
| Module home | `core/designsystem/` |

---

## Color Tokens (Spine palette — Sage default)

### Surfaces (5-tier elevation via color, no drop shadows)

| Token | Value | Usage |
|-------|-------|-------|
| `Bg` | `#0A0D12` | App scaffold, status-bar-flush |
| `Surface` | `#11151C` | Cards, rails, bottom nav container |
| `SurfaceHigh` | `#1A2030` | Elevated cards, bottom sheets |
| `SurfaceTop` | `#232B3D` | Dialogs, menus, player chips |

### Text

| Token | Value | Usage |
|-------|-------|-------|
| `OnSurface` | `#EAEEF6` | Primary text |
| `OnSurfaceVariant` | `#8C95A8` | Secondary / dim text |
| `OnSurfaceMuted` | `#525B6E` | Timestamps, captions |
| `OnSurfaceFaint` | `#2F3645` | Disabled / dividers |

### Accent palettes (choose ONE at launch)

| ID | Name | Accent | On-accent ink | Status |
|----|------|--------|---------------|--------|
| `sage` | Sage | `#9DC83C` | `#0B1402` | **Recommended default** (toned lime, moss-leaning) |
| `amber` | Ember | `#E5A742` | `#1A0F00` | Alternate (warm projector amber; original brand tie) |
| `cyan` | Signal | `#4FD0E6` | `#001218` | Alternate (cool cyan; original teal evolution) |

**Open Question:** Ship Sage as v1 default and expose Ember/Signal in Settings → Display, OR ship Sage only and reserve picker for v1.x? (See Handoff Open Question #1.)

### Cool accent (cross-palette constant)

| Token | Value | Usage |
|-------|-------|-------|
| `AccentCool` | `#7FB6FF` | Studio names, tag labels, codec badge |
| `AccentCoolDim` | `#4A75B6` | Cool accent dim variant |

### Semantic

| Token | Value | Usage |
|-------|-------|-------|
| `Warning` | `#FFCC44` | Star ratings |
| `Error` | `#FF5860` | Error states, favorite hearts |
| `Success` | `#5DBB63` | Codec ready indicator |

### Borders

| Token | Value | Usage |
|-------|-------|-------|
| `Border` | `#A0B4DC` @ 10% alpha | Default divider |
| `BorderStrong` | `#A0B4DC` @ 22% alpha | Sheet handles, section lines |

**Accent reservation:** AccentPrimary is for primary CTAs, active nav pill, focused input borders, active filter chips, progress fills, "RESUME" labels, "SMART" badges. **Never** apply AccentPrimary to all interactive elements.

---

## Typography

| Role | Family | Size | Weight | Letter-spacing | Usage |
|------|--------|------|--------|----------------|-------|
| HeadlineLarge | Space Grotesk | 28sp | W600 | -0.8sp | Page headlines |
| HeadlineMedium | Space Grotesk | 22sp | W600 | -0.5sp | Section headlines |
| TitleLarge | Space Grotesk | 18sp | W600 | -0.4sp | Top-bar titles, sheet headers |
| TitleMedium | Space Grotesk | 14sp | W600 | -0.2sp | Rail headers, resume card title |
| TitleSmall | Space Grotesk | 13sp | W500 | -0.1sp | List row titles |
| BodyMedium | Space Grotesk | 13sp | W400 | — | Body text |
| BodySmall | Space Grotesk | 12sp | W400 | — | Compact body |
| LabelMedium | Space Grotesk | 11sp | W500 | -0.1sp | Bottom nav labels, chip labels |
| MetaMono | JetBrains Mono | 10sp | W400 | 0.6sp | Metadata, badges, key labels |
| MonoSmall | JetBrains Mono | 9sp | W400 | 0.5sp | Chapter labels, timestamps |

**Open Question:** Downloadable fonts (Google Fonts API) vs bundled `.ttf` in `assets/`. (See Handoff Open Question #2.)

---

## Shape

| Token | Value | Usage |
|-------|-------|-------|
| `ShapeSmall` | `RoundedCornerShape(6.dp)` | Chips, filter chips, input fields |
| `ShapeMedium` | `RoundedCornerShape(10.dp)` | Cards, scene cards, resume card |
| `ShapeLarge` | `RoundedCornerShape(16.dp)` | Bottom sheets (top corners only), nav pill |
| `ShapeCircle` | `CircleShape` | Avatars, glass play button |

---

## Spacing Scale

Base unit **4dp**. Values in use: 4, 6, 8, 10, 12, 14, 16, 18, 20, 24, 28, 32 dp.

| Token | Value | Usage |
|-------|-------|-------|
| Screen edge padding | 18dp horizontal | All screens |
| Card grid gutter | 10dp both axes | Library grid, rails |
| Section padding (vertical) | 24dp between rails | Home rails |

Exceptions: bottom-nav padding 4dp inner; nav-pill items 9dp 12dp.

---

## Screens

Full per-screen specifications in `design_handoff_slopper_spine/README.md` §Screens. Summary list:

1. **HomeScreen** — Resume card + horizontal LazyRow rails (180dp items); smart "Tonight · under 45 min" rail
2. **LibraryScreen** — `LazyVerticalGrid(GridCells.Adaptive(180.dp))` + always-visible search field + filter chip row
3. **SceneCard** (core component) — 16:9, ShapeSmall, gradient overlay, resolution chip + rating chip + duration/studio metadata
4. **DetailScreen** — Card-style hero (16:10) + title block + primary CTA + 2-col technical metadata grid + cast/chapters/tags
5. **BrowseScreen** — Segmented control (Studios/Performers/Tags/Markers); per-type layouts
6. **SettingsScreen** — Vertically scrollable, Surface-grouped sections, MetaMono section headers, custom Switch style
7. **ConnectionScreen** — Centered column, headline + sub + input fields + success/error cards + Test/Connect buttons
8. **PlayerScreen** (landscape) — Black surface, top/bottom scrims, chapter strip (Spine-unique) above timeline, transport row with 52dp accent play/pause

---

## Modal Flows

| Flow | Type | Height | Key behaviors |
|------|------|--------|---------------|
| Filter sheet | `ModalBottomSheet` | 84% | Active filter strip + 9 sections + range slider + sticky footer (Reset/Save/Apply) |
| Search (expanded) | Full-screen overlay | 100% | Back arrow + active field + scope chips + top result + sectioned results |
| More sheet | `ModalBottomSheet` | 62% | Two groups: Browse (Tags/Markers/History) + App (Settings/Customize nav/Cast) |
| Customize nav bar sheet | `ModalBottomSheet` | 76% | Item list with leading icon + label + checkbox + drag handle; 4-tab cap |
| Marker editor sheet | `ModalBottomSheet` | 84% | Mini-timeline + markers list + add-marker CTA |
| Player settings panel | Right-anchored slide-in | ~40% width (landscape) | Speed chips, audio/subtitle radios, video info, up-next mini-queue |

---

## Navigation

### Bottom navigation bar — REDESIGNED

**Replace** the current `NavigationBar` with a floating pill bar.

- **Position:** Absolute, bottom 14dp, horizontally centered (left/right 12dp)
- **Container:** Row, padding 4dp, background `Color(0x11151C).copy(alpha = 0.92f)`, blur 20dp, border 1px Border, borderRadius 16dp, shadow 0 10dp 30dp `Color.Black.copy(alpha = 0.4f)`
- **Inactive tab:** padding 9dp 12dp, borderRadius 12dp, background transparent, color OnSurfaceVariant, icon 18dp + NO label
- **Active tab:** padding 9dp 12dp, borderRadius 12dp, background AccentPrimary, color AccentOnPrimary, icon 18dp + label (SpaceGrotesk 11sp W600)
- **Default tabs (4):** Home · Library · Browse · Settings
- **More:** accessed from Browse → More sheet (not a 5th tab icon); More dot always on the right

### Route structure (UNCHANGED)

`Connection` → `Home` → `Library` (optional preset) → `Detail/{id}` → `Player/{id}`
`Browse/{performers|studios|tags}` → `Library?preset={kind}:{id}`
`Settings` (from any tab via More sheet)

**Open Question:** Confirm More-sheet + Customize-nav-sheet replaces existing `NavCustomizeSheet`. (Handoff Open Question #3.)

---

## Component Inventory (Compose Mapping)

| Spine component | Suggested Compose impl | Reuse existing? |
|---|---|---|
| `SBottomNav` floating pill | Custom `Row` inside `Box` with `Modifier.align(BottomCenter)`. **NOT** `NavigationBar`. | New |
| `SSceneCard` | Existing `SceneCard.kt` — **update colors, remove old gradient, add chapter-strip variant** | Update |
| `SpineResumeCard` | New `ResumeCard.kt` composable in `core/designsystem/component/` | New |
| `SpineFilterChip` | `FilterChip` M3 with custom colors override | Update |
| `SheetShell` | `ModalBottomSheet` with custom drag handle + header | New (wrapper) |
| `SpineMeta` grid | `LazyVerticalGrid` 2-col inside `Column` | New |
| Chapter strip | Custom `Canvas`-drawn composable, same approach as existing `TimelineBar` | New |
| Shimmer skeleton | Shimmer library OR custom `Animatable` brush | New |
| `SpineSwitch` | M3 `Switch` with custom `SwitchColors` | Update |

---

## Real-Content States

Per-screen state specs in handoff §"Real-content states". Summary:

| Screen | States covered |
|--------|----------------|
| Home | Loading skeleton (shimmer), empty + scanning (illustration + progress) |
| Library | No results (recovery copy + "Did you mean" suggestions), server error (cached scenes hint) |
| Connection | Wrong-server error (Error card + retry/help buttons) |
| Detail | Unscraped scene (filename + raw play + scrape CTA) |

---

## Edge-to-Edge Requirements (depends on Phase 2 COMPLY-01)

**Direct cross-phase dependency.** Spine relies on Phase 2 COMPLY-01 landing edge-to-edge correctly:

> "Edge-to-edge insets: Status bar region is 38dp (Compose `WindowInsets.statusBars`). The Home top bar and Detail hero must extend behind it with a transparent status bar." — Handoff Open Question #6.

Phase 5 will assume:
- `themes.xml` has NO hardcoded `statusBarColor` / `navigationBarColor` (COMPLY-01 strips these)
- `enableEdgeToEdge()` is called in `MainActivity.onCreate` (already true at Phase 1 head; COMPLY-01 keeps it)
- Top-level `Scaffold` declarations use `WindowInsets.systemBars` (COMPLY-01 audits this)
- Player overlay scrims account for status bar insets (COMPLY-01's `Box(safeDrawingPadding())` pattern at PlayerScreen.kt:187 area carries forward; Phase 5 expands to chapter strip + transport row)
- `ModalBottomSheet` uses `contentWindowInsets = { WindowInsets.navigationBars }` (COMPLY-01 sets this on FilterSheet + NavCustomizeSheet; Phase 5 inherits and extends to new sheets)

If COMPLY-01 doesn't ship cleanly, Phase 5 inherits all the inset bugs and would have to do this work itself.

---

## Open Questions (from handoff)

1. **Palette choice:** Sage as v1 default? Or expose all three?
2. **Fonts:** Downloadable fonts (Google Fonts) vs bundled `.ttf`?
3. **Bottom nav customization:** Confirm More-sheet + Customize-nav-sheet replaces existing `NavCustomizeSheet`?
4. **Filter sheet "Save view":** Requires new local persistence (`Room` table `SavedView(name, filterState)`) — in scope for v1 or defer?
5. **Chapter strip:** Reads `scene.markers` sorted by time — confirm `PlayerViewModel` already exposes this?
6. **Edge-to-edge insets:** 38dp status bar handling — **resolved by Phase 2 COMPLY-01.**

---

## Checker Sign-Off

This UI-SPEC.md was canonicalized from the design handoff under the **lightweight** path — Phase 5 spec-layer pipeline (issue, manifest, spec/discuss/research/UI-checker formal flow) was NOT run. Sign-off is therefore informal until Phase 5 starts in roadmap order, at which point:

- [ ] Run full `/gsd-ui-phase 5` to re-generate via gsd-ui-researcher (may produce a structurally-cleaner version than this manual restructure)
- [ ] Run gsd-ui-checker against the result (6-pillar audit: Copywriting / Visuals / Color / Typography / Spacing / Registry Safety)
- [ ] Add Phase 5 issue + manifest to spec-layer
- [ ] Resolve the 5 open questions (Sage default, font delivery, nav-sheet replacement, Save view scope, Chapter strip data wiring) via `/gsd-discuss-phase 5`

**Approval:** pending (informal scaffold only; v1 contract awaits Phase 5 formal entry)

---

*Phase: 05-spine-compose-ui-redesign*
*UI-SPEC created (lightweight): 2026-05-17*
*Source: design_handoff_slopper_spine/README.md*
*Trigger: Phase 2 plan-phase needed Spine forward-compat reference; full Phase 5 spec-layer registration deferred*
