# Phase 5: SPINE — Compose UI Redesign — Specification

**Created:** 2026-05-19
**Ambiguity score:** 0.137 (gate: ≤ 0.20)
**Requirements:** 12 locked
**UI-SPEC source:** `05-UI-SPEC.md` + `design_handoff_slopper_spine/README.md` (558 lines, high-fidelity)

## Goal

Apply the Spine visual direction to all 8 Slopper screens and 6 modal flows: replace existing design tokens (colors, typography, shapes, spacing), replace the bottom navigation bar with a floating pill component, update `SceneCard` and `PlayerScreen` with Spine-specific additions (chapter strip), and wire new screen-level compositions — all without altering data flow, navigation graph, or existing feature behavior.

## Background

**Current state (scouted 2026-05-19):**
- `core/designsystem/theme/Color.kt`: 5-tier surface tokens (`SurfaceBase`→`SurfaceHighest`), warm amber accent `#F0A037`. Spine replaces both entirely.
- `core/designsystem/theme/Type.kt`: `FontFamily.Default` (system sans-serif) throughout. Spine introduces Space Grotesk (UI text) + JetBrains Mono (metadata/monospaced) from Google Fonts.
- `core/ui/nav/BottomNav.kt`: Material 3 `NavigationBar` + `NavigationBarItem`. Spine replaces with custom floating pill bar.
- `core/designsystem/component/SceneCard.kt`: Existing component using Coil 3 `AsyncImage`. Spine updates colors, gradient, and metadata layout.
- Phase 2 COMPLY-01 is complete: `enableEdgeToEdge()` active, `themes.xml` clean, `safeDrawingPadding` at PlayerScreen overlay, all 3 `ModalBottomSheet` sites have `contentWindowInsets`. This is a **hard prerequisite** for Spine's full-bleed hero/scrim approach.

**Resolved open questions (from UI-SPEC):**
- **Q1 Palette:** Sage (`#9DC83C`) as v1 default. Ember/Signal deferred to v1.x theme picker.
- **Q2 Fonts:** Downloadable via Google Fonts API (`androidx.compose.ui:ui-text-google-fonts`). No bundled `.ttf`.
- **Q3 Nav customization:** More-sheet + Customize-nav-sheet **replaces** existing `NavCustomizeSheet`. Both are `ModalBottomSheet` wrappers.
- **Q4 Filter "Save view":** **Deferred to v1.x** — requires new Room table; out of scope for v1. Filter sheet footer shows "Reset" + "Apply" only (no "Save view").
- **Q5 Chapter strip:** Reads `PlayerUiState.current?.markers` (already available via `PlayerViewModel`). No new data fetching needed.
- **Q6 Edge-to-edge:** Resolved by Phase 2 COMPLY-01.

## Requirements

1. **Design token migration — Color (SPINE-01):** All existing `StashColors` values in `Color.kt` are replaced with Spine surface, text, accent, border, and semantic tokens.
   - Current: `SurfaceBase=#0B0E13`, `SurfaceLow=#121722`, `SurfaceMed=#1A2030`, `SurfaceHigh=#232B3D`, `SurfaceHighest=#2E3750`; `AccentPrimary=#F0A037`
   - Target: `Bg=#0A0D12`, `Surface=#11151C`, `SurfaceHigh=#1A2030`, `SurfaceTop=#232B3D` (4 tiers); `AccentPrimary=#9DC83C` (Sage), `AccentPrimaryDim=#6E9028`, `AccentOnPrimary=#0B1402`; text: `OnSurface=#EAEEF6`, `OnSurfaceVariant=#8C95A8`, `OnSurfaceMuted=#525B6E`, `OnSurfaceFaint=#2F3645`; `Border=#A0B4DC@10%`, `BorderStrong=#A0B4DC@22%`; semantic: `Warning=#FFCC44`, `Error=#FF5860`, `Success=#5DBB63`; cool accent: `AccentCool=#7FB6FF`, `AccentCoolDim=#4A75B6`
   - Acceptance: `grep -c 'AccentPrimary.*9DC83C\|0x9DC83C\|0xFF9DC83C' core/designsystem/src/main/java/.../Color.kt` → ≥ 1; `grep -c 'F0A037\|SurfaceBase\|SurfaceLow\|SurfaceMed\|SurfaceHighest' core/designsystem/src/main/java/.../Color.kt` → 0 (old tokens removed)

2. **Design token migration — Typography (SPINE-02):** Space Grotesk and JetBrains Mono loaded via Google Fonts; 10 named text styles defined.
   - Current: All styles use `FontFamily.Default`
   - Target: `SpaceGrotesk = GoogleFont("Space Grotesk")` and `JetBrainsMono = GoogleFont("JetBrains Mono")` declared in `core/designsystem/`; `Type.kt` defines at minimum: `HeadlineLarge` (28sp W600 -0.8sp), `TitleLarge` (18sp W600 -0.4sp), `TitleMedium` (14sp W600 -0.2sp), `TitleSmall` (13sp W500), `BodyMedium` (13sp W400), `LabelMedium` (11sp W500 -0.1sp), `MetaMono` (10sp JetBrainsMono W400 0.6sp), `MonoSmall` (9sp JetBrainsMono W400 0.5sp); `StashTheme` wires the updated `StashTypography`
   - Acceptance: `grep -c 'GoogleFont\|Space Grotesk\|JetBrains Mono' core/designsystem/src/main/java/.../Type.kt` → ≥ 2; `./gradlew :core:designsystem:assembleDebug` → BUILD SUCCESSFUL

3. **Floating pill bottom nav (SPINE-03):** The Material 3 `NavigationBar` in `BottomNav.kt` is replaced with a custom floating pill composable matching the Spine spec.
   - Current: `NavigationBar { NavigationBarItem(...) }` — standard M3 bar anchored to bottom
   - Target: Custom `Box` containing a centered `Row` composable; container: `padding=4dp, background=Surface@92%, blur=20dp, border=1px Border, borderRadius=16dp, shadow`; absolute position bottom 14dp; inactive tabs: transparent bg, OnSurfaceVariant icon 18dp, NO label; active tab: AccentPrimary bg, AccentOnPrimary icon 18dp + SpaceGrotesk 11sp W600 label; default 4 tabs: Home · Library · Browse · Settings; More accessed via More sheet
   - Acceptance: `grep -c 'NavigationBar\b' core/ui/src/main/java/.../BottomNav.kt` → 0 (removed); `grep -c 'BlurMaskFilter\|renderEffect\|SpineBottomNav\|floatingNav\|pill' core/ui/src/main/java/.../BottomNav.kt` → ≥ 1; `./gradlew :core:ui:assembleDebug` → BUILD SUCCESSFUL

4. **SceneCard update (SPINE-04):** `SceneCard.kt` updated with Spine colors, gradient, and metadata layout.
   - Current: Existing card with warm-amber accent, old surface colors
   - Target: 16:9 aspect ratio, `ShapeSmall(6dp)` clip; gradient from `rgba(10,13,18,0)@45%` → `rgba(10,13,18,0.92)` (Bg-based); top-left: resolution chip (MetaMono, white, black@65%+blur, 3dp radius); top-right: rating chip (≥4.0 only, Warning color); bottom: title (SpaceGrotesk 12sp W500, 2 lines) + duration left (MonoSmall white@70%) + studio right (MonoSmall AccentPrimary); resume bar: 2dp full-width at bottom, AccentPrimary fill, white@10% track
   - Acceptance: `grep -c 'ShapeSmall\|6.dp\|roundedCornerShape' core/designsystem/src/main/java/.../SceneCard.kt` → ≥ 1; `./gradlew :core:designsystem:assembleDebug` → BUILD SUCCESSFUL

5. **HomeScreen — Spine (SPINE-05):** HomeScreen redesigned with inline top bar, resume card, and Spine-styled rails.
   - Current: Existing HomeScreen with Material3 scaffold + default typography
   - Target: (a) Inline top bar Row (logo mark + "Slopper" TitleLarge + server name badge + search/refresh icon buttons, 18dp horizontal padding); (b) `SpineResumeCard` composable in `core/designsystem/component/` (130×88dp thumbnail, "RESUME" MetaMono label, progress bar); (c) Rails with TitleMedium W600 section headers + count badge; (d) `LazyRow` items at 180dp width, 10dp gap, 18dp contentPadding; (e) all text and surface colors from Spine tokens
   - Acceptance: `ls core/designsystem/src/main/java/.../component/ResumeCard.kt` → exists; `./gradlew :feature:home:assembleDebug` → BUILD SUCCESSFUL

6. **LibraryScreen — Spine (SPINE-06):** LibraryScreen updated with Spine search field, filter chips, and grid styling.
   - Current: Existing library with default search and filter components
   - Target: (a) Always-visible search field (Surface bg, Border, ShapeSmall, MonoSmall placeholder, "⌘K" hint); (b) horizontal filter chip row (active: AccentPrimary 12%+30% border; inactive: Border only; all ShapeSmall, SpaceGrotesk 11.5sp); (c) "N results · N organized" + "Grid · auto" meta row (MetaMono OnSurfaceMuted); (d) `LazyVerticalGrid(GridCells.Adaptive(180.dp))`, 10dp gaps, 18dp horizontal padding, 100dp bottom padding; filter sheet footer: Reset + Apply only (no Save view per Q4 decision)
   - Acceptance: `./gradlew :feature:library:assembleDebug` → BUILD SUCCESSFUL; filter sheet has no "Save view" button (`grep -c 'save.*view\|saveView\|SaveView' feature/library/` → 0)

7. **DetailScreen — Spine (SPINE-07):** DetailScreen updated with card-style hero, title block, 2-col metadata grid, cast and chapter rows.
   - Current: Existing detail with full-bleed hero + M3 layout
   - Target: (a) Card-style hero: 8dp 18dp padding, 16/10 aspect ratio, ShapeMedium, Border border; back button 30dp circle black@55%+blur; meta pills bottom-left (MetaMono, black@55%+blur); 44dp AccentPrimary play circle bottom-right; (b) Title block: studio MetaMono W700 AccentPrimary, title 24sp W600, meta row MetaMono; (c) Primary CTA: full-width AccentPrimary button, "Play"/"Resume · X:XX:XX left"; (d) 2-col technical metadata grid (Border as 1dp gap; key MetaMono OnSurfaceMuted, value JetBrainsMono 11sp); (e) cast rows (36dp avatar + TitleSmall + MetaMono count + heart + chevron); (f) chapter rows (64×36dp thumbnail + title + AccentPrimary tag); tags FlowRow
   - Acceptance: `./gradlew :feature:detail:assembleDebug` → BUILD SUCCESSFUL

8. **BrowseScreen — Spine (SPINE-08):** BrowseScreen redesigned with 4-tab segmented control and per-type layouts.
   - Current: Existing browse with tab selection
   - Target: (a) Segmented control: Surface bg, Border, ShapeSmall, 3dp inner padding, 4 tabs (Studios/Performers/Tags/Markers); active: SurfaceHigh bg, BorderStrong, SpaceGrotesk 11sp W500; (b) Performers list: 44dp circle avatar, TitleSmall, MetaMono "N scenes · last seen", 3 stacked mini-thumbnails; (c) Studios grid: 2-col, 4:3 aspect ShapeMedium, bottom gradient, studio name SpaceGrotesk 16sp W600; (d) Tags: AccentCool text
   - Acceptance: `./gradlew :feature:browse:assembleDebug` → BUILD SUCCESSFUL

9. **SettingsScreen — Spine (SPINE-09):** SettingsScreen updated with Spine section groups and custom Switch style.
   - Current: Existing settings with Material3 defaults
   - Target: (a) Section header: MetaMono uppercase W600 1.5sp letterSpacing, OnSurfaceMuted, 8dp top; (b) Section container: Surface bg, ShapeSmall, overflow clip, Border row dividers; (c) Setting row: 10dp 14dp padding, SpaceGrotesk 13sp key, JetBrainsMono 11sp AccentPrimary value; (d) SpineSwitch: custom `SwitchColors` — ON: AccentPrimary track, AccentOnPrimary thumb; OFF: SurfaceHigh track, Border, OnSurfaceVariant thumb
   - Acceptance: `./gradlew :feature:settings:assembleDebug` → BUILD SUCCESSFUL

10. **ConnectionScreen — Spine (SPINE-10):** ConnectionScreen updated with Spine input fields and result cards.
    - Current: Existing connection screen with Material3 defaults
    - Target: (a) Centered column, 24dp padding, centered vertically; (b) Input field: JetBrainsMono 12sp value, Surface bg, Border border, ShapeSmall, AccentPrimary border on focus; (c) Success card: AccentPrimary@8% bg, AccentPrimary@25% border, checkmark + server info + MetaMono details; (d) Error card: Error@8% bg, Error@30% border; (e) Test button: transparent + Border; Connect button: AccentPrimary fill, AccentOnPrimary text, W700
    - Acceptance: `./gradlew :feature:connection:assembleDebug` → BUILD SUCCESSFUL

11. **PlayerScreen — Spine (SPINE-11):** PlayerScreen updated with Spine scrim system, chapter strip, and Spine transport row.
    - Current: Existing player with Phase 2/3 additions (PredictiveBackHandler, safeDrawingPadding, applyVideoFrameRate LaunchedEffect)
    - Target: (a) Top scrim: gradient, 90dp tall; bottom scrim: gradient → `rgba(0,0,0,0.92)`, 160dp tall; (b) Chapter strip: proportional segments above timeline, AccentPrimary fill for played/current, white@18% for future; `Canvas`-drawn composable reading `state.current?.markers`; (c) Transport row: prev + back10 + play (52dp ShapeMedium AccentPrimary bg, AccentOnPrimary icon) + fwd10 + next; (d) Top pill chips row: HW·HEVC badge, speed, PiP, aspect, shuffle, repeat (26dp height, ShapeSmall); (e) All Phase 2/3 additions preserved (PredictiveBackHandler, safeDrawingPadding, LaunchedEffect(videoFrameRate))
    - Acceptance: `ls feature/player/src/main/java/.../PlayerTimeline.kt` → exists (chapter strip lives here or alongside); `grep -n 'PredictiveBackHandler\|safeDrawingPadding' feature/player/src/main/java/.../PlayerScreen.kt` → ≥ 2 hits each; `./gradlew :feature:player:assembleDebug` → BUILD SUCCESSFUL

12. **Modal flows — Spine (SPINE-12):** All 6 modal flows implemented with Spine styling.
    - Current: Existing `FilterSheet`, `NavCustomizeSheet`, `MoreSheet` (in `BottomNav.kt`) — all Phase 2-updated
    - Target: (a) Filter sheet: 84% height, Bg background, BorderStrong top border, active filter strip, 9 sections, range sliders, sticky footer (Reset + Apply, no Save view); (b) More sheet: 62% height, 2 groups (Browse + App), accent-colored leading icons; (c) Customize nav bar sheet: 76%, checkbox + drag handle rows, 4-tab cap, AccentPrimary checked state; (d) Marker editor sheet: 84%, mini-timeline + marker rows + "Add marker" CTA; (e) Search overlay: full-screen, scope chips + sectioned results; (f) Player settings panel: right-anchored ~40% width, blur bg, speed/audio/subtitle controls + up-next queue
    - Acceptance: `grep -c 'filterSheet\|FilterSheet\|MoreSheet\|NavCustomizeSheet\|MarkerEditor\|PlayerSettingsPanel' feature/ core/ -r --include="*.kt"` → ≥ 6 distinct composable names; `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL; `grep -c 'Save view\|saveView' feature/library/src/` → 0

## Boundaries

**In scope:**
- All color/typography/shape/spacing token replacements in `core/designsystem/`
- Floating pill bottom nav composable (replaces `NavigationBar`)
- `SceneCard.kt` color and layout update
- All 8 screens redesigned: Home, Library, Detail, Browse, Settings, Connection, PlayerScreen, and the scaffolded entry points
- 6 modal flows: filter sheet, more sheet, nav customize sheet, marker editor sheet, search overlay, player settings panel
- `SpineResumeCard.kt` and `ChapterStrip` (new composables in `core/designsystem/component/`)
- Space Grotesk + JetBrains Mono fonts via `ui-text-google-fonts`
- Sage palette as default v1 accent

**Out of scope:**
- Filter sheet "Save view" persistence (Room table `SavedView`) — deferred to v1.x
- Ember / Signal accent palette picker in Settings — deferred to v1.x theme picker
- Tablet / foldable layout — phone-only, same as existing app
- Light theme — dark-only, same as existing app
- New data sources, new GraphQL operations, new navigation routes
- Background playback / `MediaSessionService` — BG-MEDIA milestone
- `PlayerScreen.kt` further splitting beyond Phase 4's POLISH-01 split
- Any behavior change to the existing connection flow, player queue logic, or library pagination

## Constraints

- **Toolchain floor:** AGP 8.7.3 / Kotlin 2.2.20 / compileSdk 35 / Compose BOM 2026.05.00 (unchanged)
- **Phase 2 COMPLY-01 is a hard prerequisite** — edge-to-edge, safeDrawingPadding, ModalBottomSheet contentWindowInsets are already in place
- **Phase 4 POLISH-01 split is the baseline** — `PlayerControls.kt` and `PlayerTimeline.kt` are the split files; Spine work touches `PlayerTimeline.kt` (chapter strip), `PlayerScreen.kt` (scrims, transport), and `PlayerControls.kt` (top chip row)
- **Coil 3 for all images** — no change to image loading infrastructure
- **Material 3 components remain as the base** — Spine overrides via `SwitchColors`, `FilterChipDefaults.colors(...)` etc., not by removing M3
- **Google Fonts API** (`implementation(libs.androidx.compose.ui.text.google.fonts)`) — network-dependent; must gracefully fall back to system sans-serif if fonts unavailable
- **Phase 3 PERF-03 ImmutableList** — `ImmutableList<Marker>` in `PlayerTimeline.kt` must be preserved in chapter strip implementation
- **Anti-regression:** all existing navigation flows, player gestures, library pagination, filter presets must work after Spine lands

## Ambiguity Report

```
Goal Clarity:       0.91 (min 0.75) ✓
Boundary Clarity:   0.88 (min 0.70) ✓
Constraint Clarity: 0.83 (min 0.65) ✓
Acceptance Criteria:0.79 (min 0.70) ✓
Ambiguity score:    0.137 (gate: ≤ 0.20) ✓ PASS
```

All 4 dimensions above minimum. Design handoff provides pixel-level specs — planner needs no assumption.
