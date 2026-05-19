# Slopper — Spine UI Redesign Handoff

**Version:** v3.0 · May 19, 2026  
**Prepared for:** Claude Code / developer implementation  
**App:** Slopper — native Android client for [Stash](https://github.com/stashapp/stash), a self-hosted media server  
**Platform:** Android (Jetpack Compose + Material 3), edge-to-edge, dark-only, phone portrait (player landscape)

> See [`CHANGELOG.md`](CHANGELOG.md) for the full revision history. The version in the prototype header badge always matches the latest entry. Bump `SLOPPER_VERSION` in `spine-app.jsx` when you ship a new revision.

---

## Overview

This package documents the **Spine** direction — a complete visual redesign of Slopper's phone UI. All screens, modal flows, and real-content states have been designed to high fidelity.

The reference files (`Slopper Spine.html` and companions) are **HTML design prototypes** — not production code. The task is to recreate these designs in the existing Jetpack Compose codebase, using its established patterns (`@Composable`, Material 3 components, the existing `core/designsystem/` module). Do not ship the HTML directly.

---

## Fidelity

**High-fidelity.** Pixel-level intent on colors, typography, spacing, radii, and component structure. Implement these with as much fidelity as Compose allows.

Where exact CSS values are given, translate them as follows:
- `dp` → `dp` (1:1)
- `px` font sizes → `sp`
- `rgba(r,g,b,a)` with `a < 1` → `Color(r,g,b).copy(alpha = a)` or `Color(0xAARRGGBB)`
- `border-radius` → `RoundedCornerShape(Xdp)`
- `background: linear-gradient(...)` → `Brush.verticalGradient(...)` in Compose

---

## Accent palette — choose one at launch

The design ships three pre-calibrated accent options. Pick one for v1; the others can live in Settings or a future theme picker.

| ID | Name | Accent | On-accent ink | Use case |
|----|------|--------|---------------|----------|
| `sage` | Sage | `#9DC83C` | `#0B1402` | Toned lime — less neon, more moss. **Recommended default.** |
| `amber` | Ember | `#E5A742` | `#1A0F00` | Warm projector amber — ties to original Slopper brand |
| `cyan` | Signal | `#4FD0E6` | `#001218` | Cool cyan — evolved from original teal |

The rest of the surface / text tokens are identical across all three.

---

## Design tokens

### Surfaces (5-tier elevation via color, no drop shadows)

```kotlin
// Replace existing SurfaceBase/Low/Med/High/Highest
val Bg           = Color(0xFF0A0D12)   // app scaffold, status-bar-flush
val Surface      = Color(0xFF11151C)   // cards, rails, bottom nav container
val SurfaceHigh  = Color(0xFF1A2030)   // elevated cards, bottom sheets
val SurfaceTop   = Color(0xFF232B3D)   // dialogs, menus, player chips
```

### Text

```kotlin
val OnSurface        = Color(0xFFEAEEF6)   // primary text
val OnSurfaceVariant = Color(0xFF8C95A8)   // secondary / dim text
val OnSurfaceMuted   = Color(0xFF525B6E)   // timestamps, captions
val OnSurfaceFaint   = Color(0xFF2F3645)   // disabled / dividers
```

### Accent (Sage — recommended)

```kotlin
val AccentPrimary    = Color(0xFF9DC83C)
val AccentPrimaryDim = Color(0xFF6E9028)
val AccentOnPrimary  = Color(0xFF0B1402)   // text ON accent backgrounds
val AccentCool       = Color(0xFF7FB6FF)   // studio names, tag labels, codec badge
val AccentCoolDim    = Color(0xFF4A75B6)
```

### Semantic

```kotlin
val Warning = Color(0xFFFFCC44)   // star ratings
val Error   = Color(0xFFFF5860)   // error states, favorite hearts
val Success = Color(0xFF5DBB63)   // codec ready indicator
```

### Borders

```kotlin
val Border       = Color(0xFFA0B4DC).copy(alpha = 0.10f)   // default divider
val BorderStrong = Color(0xFFA0B4DC).copy(alpha = 0.22f)   // sheet handles, section lines
```

### Typography

Font: **Space Grotesk** for all UI text (display, body, labels). **JetBrains Mono** for metadata, timestamps, codec labels, key-value pairs.

```kotlin
// Add to fonts.xml / downloadable font config:
// Space Grotesk: weights 300–700
// JetBrains Mono: weights 400–600

val SpaceGrotesk = FontFamily(...)
val JetBrainsMono = FontFamily(...)

// Type scale (replaces system default in Type.kt):
val HeadlineLarge  = TextStyle(fontFamily = SpaceGrotesk, fontSize = 28.sp, fontWeight = W600, letterSpacing = (-0.8).sp)
val HeadlineMedium = TextStyle(fontFamily = SpaceGrotesk, fontSize = 22.sp, fontWeight = W600, letterSpacing = (-0.5).sp)
val TitleLarge     = TextStyle(fontFamily = SpaceGrotesk, fontSize = 18.sp, fontWeight = W600, letterSpacing = (-0.4).sp)
val TitleMedium    = TextStyle(fontFamily = SpaceGrotesk, fontSize = 14.sp, fontWeight = W600, letterSpacing = (-0.2).sp)
val TitleSmall     = TextStyle(fontFamily = SpaceGrotesk, fontSize = 13.sp, fontWeight = W500, letterSpacing = (-0.1).sp)
val BodyMedium     = TextStyle(fontFamily = SpaceGrotesk, fontSize = 13.sp, fontWeight = W400)
val BodySmall      = TextStyle(fontFamily = SpaceGrotesk, fontSize = 12.sp, fontWeight = W400)
val LabelMedium    = TextStyle(fontFamily = SpaceGrotesk, fontSize = 11.sp, fontWeight = W500, letterSpacing = (-0.1).sp)
val MetaMono       = TextStyle(fontFamily = JetBrainsMono, fontSize = 10.sp, fontWeight = W400, letterSpacing = 0.6.sp)
val MonoSmall      = TextStyle(fontFamily = JetBrainsMono, fontSize = 9.sp,  fontWeight = W400, letterSpacing = 0.5.sp)
```

### Shape

```kotlin
val ShapeSmall  = RoundedCornerShape(6.dp)    // chips, filter chips, input fields
val ShapeMedium = RoundedCornerShape(10.dp)   // cards, scene cards, resume card
val ShapeLarge  = RoundedCornerShape(16.dp)   // bottom sheets (top corners only), nav pill
val ShapeCircle = CircleShape
```

### Spacing grid

Base unit 4dp. Values in use: 4 / 6 / 8 / 10 / 12 / 14 / 16 / 18 / 20 / 24 / 28 / 32 dp.  
Screen edge padding: **18 dp** horizontal.  
Card grid gutters: **10 dp** both axes.

---

## Navigation

### Bottom navigation bar — redesigned

Replace the current `NavigationBar` with a floating pill bar:

```
Position: absolute, bottom 14dp, horizontally centered (left/right 12dp)
Container: Row, padding 4dp, background rgba(17,21,28, 0.92), blur 20dp,
           border 1px Border, borderRadius 16dp, shadow 0 10px 30px rgba(0,0,0,0.4)

Each tab item:
  - Inactive: padding 9dp 12dp, borderRadius 12dp, background transparent,
              color OnSurfaceVariant, icon 18dp + NO label
  - Active:   padding 9dp 12dp, borderRadius 12dp, background AccentPrimary,
              color AccentOnPrimary, icon 18dp + label (SpaceGrotesk 11sp W600 letterSpacing -0.1sp)

Tabs (default 4): Home · Library · Browse · Settings
More tab is accessed from Browse → More sheet (not a 5th tab icon)
```

### Route structure (unchanged)

`Connection` → `Home` → `Library` (optional preset) → `Detail/{id}` → `Player/{id}`  
`Browse/{performers|studios|tags}` → `Library?preset={kind}:{id}`  
`Settings` (from any tab via More sheet)

---

## Screens

### 1. HomeScreen

**Purpose:** Dashboard of horizontal content rails.

**Top bar** (not a `TopAppBar` — inline Row):
- Left: Logo mark (22×22dp rotated diamond, accent fill + bg cutout) + "Slopper" (TitleLarge) + server name badge (MetaMono, Surface bg, Border border, 4dp radius)
- Right: Search icon button (32×32dp) + Refresh icon button (32×32dp)
- Padding: 4dp top, 18dp horizontal, 12dp bottom

**Resume card** (top of scroll, above rails):
- Container: Surface bg, ShapeMedium border, 1px Border
- Left: 130×88dp thumbnail with centered 32dp glass play button (black 60% + blur)
- Right: "RESUME" label (MetaMono, AccentPrimary, uppercase, W700, letterSpacing 1sp) + title (TitleMedium, 1 line ellipsis) + "Studio · Xm left" (MetaMono) + 3dp progress bar (accent fill, 8% white track, borderRadius 1.5dp)

**Rails:**
- Section header Row: title (TitleMedium W600 letterSpacing -0.2sp) + count badge (MetaMono, Surface bg, 3dp radius) + optional "SMART" badge (accent tinted) + chevron right
- Horizontal `LazyRow`, item width 180dp, gap 10dp, contentPadding 18dp
- Rail section spacing: 24dp between rails
- Empty rail → hidden entirely (zero height)

**Smart rail "Tonight · under 45 min":**
- Extra badge: accent text on accent-10% bg, 1px accent-25% border, 3dp radius, "SMART" uppercase

### 2. LibraryScreen

**Purpose:** Paged grid of scenes with search + filter.

**Top bar:**
- Title "Library" (TitleLarge W600 letterSpacing -0.3sp)
- Right: Search icon button + Filter icon button (filter has accent dot badge when active)

**Search field** (always visible, inactive state):
- Row: padding 8dp 12dp, Surface bg, Border border, ShapeSmall
- Left: Search icon 14dp in OnSurfaceMuted
- Text: "search · scene title, performer, tag…" (MonoSmall, OnSurfaceMuted)
- Right: "⌘K" hint (MetaMono, SurfaceHigh bg, 3dp radius) — tap to expand to full search

**Filter chip row** (horizontal scroll, no clip, below search):
- Active chip: accent-10% bg, accent-35% border, AccentPrimary text
- Inactive chip: transparent bg, Border border, OnSurface text
- Dashed "+ filter" chip: dashed border style
- All chips: padding 5dp 10dp, ShapeSmall, SpaceGrotesk 11sp W500
- Below row: "N results · N organized" left, "Grid · auto" right (MetaMono, OnSurfaceMuted)

**Grid:**
- `LazyVerticalGrid(GridCells.Adaptive(180.dp))`
- contentPadding 18dp horizontal, 10dp vertical gap and horizontal gap
- Bottom contentPadding 100dp (clears nav bar)

### 3. SceneCard (core component)

```
Container: 16:9 aspect ratio, ShapeSmall, Surface bg, clip
Thumbnail: full-bleed AsyncImage, cover crop
Gradient: 0→transparent (45%) → rgba(10,13,18,0.92) bottom-to-top

Top-left overlay: resolution chip (MetaMono, white, black 65% bg, blur 4dp, 3dp radius, 2dp 5dp padding)
Top-right overlay: rating (MetaMono, Warning color, black 65% bg) — only shown when rating ≥ 4.0

Bottom overlay:
  Title: SpaceGrotesk 12sp W500, white, maxLines 2, overflow ellipsis
  Row below: duration left (MonoSmall, white 70%) · studio right (MonoSmall, AccentPrimary)

Resume bar (when progress > 0):
  Full-width, 2dp tall, bottom of card
  Track: white 10%
  Fill: AccentPrimary
  No borderRadius
```

### 4. DetailScreen

**Purpose:** Full scene metadata, CTA, performers, chapters.

**Hero (card-style, not full-bleed):**
- Padding: 8dp 18dp
- Aspect ratio: 16/10 (slightly shorter than 16:9 to keep title visible)
- ShapeMedium, Border border
- Back button: 30×30dp circle, black 55% + blur 8dp, top-left 10dp
- Bottom-left: meta pills (duration, resolution, codec, bitrate) — MetaMono, black 55% + blur, 4dp radius
- Bottom-right: 44dp accent circle play button

**Title block** (padding 16dp 18dp, below hero):
- Studio: MetaMono uppercase W700 letterSpacing 1sp, AccentPrimary
- Title: SpaceGrotesk 24sp W600 letterSpacing -0.6sp lineHeight 1.1
- Meta row: date · star rating (Warning) · play count (MetaMono, OnSurfaceVariant)

**Primary CTA:**
- Full-width Button, AccentPrimary bg, AccentOnPrimary text
- SpaceGrotesk 13sp W700, padding 12dp
- Icon: PlayArrow 14dp
- Label: "Play" or "Resume · X:XX:XX left Y:YY"

**Technical metadata grid:**
- 2 columns, gap 1dp (creates hairline divider effect via bg color)
- Background: Border color (the 1dp gap shows through as a line)
- Each cell: Surface bg, padding 8dp 12dp
- Key: MetaMono, OnSurfaceMuted · Value: JetBrainsMono 11sp, OnSurface
- Fields: Codec, Bitrate, Resolution, Framerate, Size, Added

**Cast & crew list:**
- Section header: SpaceGrotesk 14sp W600 + count (MetaMono right)
- Each row: 36dp circle avatar + name (TitleSmall) + "N scenes" (MetaMono) + optional favorite heart (Error) + chevron
- Row container: padding 6dp, Surface bg, ShapeSmall, Border border, gap 10dp

**Chapters list:**
- Section header: "Chapters" + count right
- Each row: 64×36dp thumbnail (ShapeSmall) + title + tag label (AccentPrimary) + timestamp right (MonoSmall)
- Row container: padding 10dp, Surface bg, ShapeSmall, Border border

**Tags:**
- FlowRow of pill chips: Surface bg, Border, borderRadius 999dp, SpaceGrotesk 12sp

### 5. BrowseScreen

**Purpose:** Grid/list of studios, performers, or tags.

**Segmented control (top):**
- Row of 4 tabs: Studios / Performers / Tags / Markers
- Container: Surface bg, Border border, ShapeSmall, padding 3dp
- Active tab: SurfaceHigh bg, BorderStrong border, ShapeSmall - 1dp, OnSurface text
- Inactive tab: transparent, OnSurfaceVariant text
- SpaceGrotesk 11sp W500

**Performers (list layout):**
- Each row: 44dp circle avatar + name (TitleSmall W500) + "N scenes · last seen Xd ago" (MetaMono) + 3 stacked mini-thumbnails (30×18dp, -8dp overlap, 2dp border Bg) + chevron
- Row padding 8dp 4dp, border-bottom Border
- Favorite heart inline next to name when `favorite == true`

**Studios (grid layout):**
- 2-column LazyVerticalGrid
- Each cell: 4:3 aspect, ShapeMedium, full-bleed image
- Bottom gradient overlay → studio name (SpaceGrotesk 16sp W600, white) + count (MetaMono, AccentCool)

**Tags:**
- List of pill-shaped items or text rows (no images)
- Tag name in AccentCool

### 6. SettingsScreen

**Purpose:** User-tunable preferences.

**Layout:** Vertically scrollable, sections grouped in Surface containers.

**Section header:** MetaMono uppercase W600 letterSpacing 1.5sp, OnSurfaceMuted, 8dp top padding.

**Section container:** Surface bg, ShapeSmall, overflow clip. Each row has border-bottom Border.

**Setting row anatomy:**
- Padding 10dp 14dp
- Key: SpaceGrotesk 13sp OnSurface W500 (flex 1)
- Sub-label: MetaMono, OnSurfaceMuted, 2dp margin-top
- Value (right): JetBrainsMono 11sp AccentPrimary, or Switch, or chevron

**Switch style:**
- Track 30×18dp, borderRadius 9dp
- ON: AccentPrimary track, AccentPrimary border, AccentOnPrimary thumb 12×12dp, thumb at right
- OFF: SurfaceHigh track, Border border, OnSurfaceVariant thumb, thumb at left

### 6. SettingsScreen — hub + drill-down

**v3.0 — fully replaces the previous single-strip layout.**

The previous design used a single vertically-scrolling list of all settings, with each numeric setting rendered as a dot-row slider consuming ~80dp. Critical actions (Disconnect server) sat next to cosmetic toggles. This rework introduces hierarchy.

#### 6.1 Settings hub (landing page)

**Top bar:** "Settings" title + a small "Synced 2m ago" sync-status pill on the right (green dot + label, MetaMono, Surface bg, 999px radius).

**Server status card** (immediately below top bar, padding 14dp 18dp):
- Container: padding 14dp, Surface bg, Border, ShapeLarge (16dp)
- Left: 40×40dp rounded-rect icon container with accent-12%-bg + accent-30%-border (cast icon, AccentPrimary)
- Middle: endpoint name (TitleMedium, W600) + small green dot inline; one sub-line in MetaMono: "Stash vX.X.X · N scenes · Yms"
- Right: chevron

**Quick search field** (below status, padding 14dp 18dp):
- Same field style as Library search: Surface bg, Border, ShapeSmall, padding 9dp 12dp
- Placeholder: "search settings…" (MonoSmall, OnSurfaceMuted)

**Category groups** (padding 20dp 18dp 0):

Each group: optional uppercase MetaMono title, then a Surface-bg container with `ShapeSmall` and `Border`. Inside, a stack of `HubRow`s separated by 1dp Border.

**`HubRow` anatomy:**
- Padding 12dp 14dp, gap 12dp
- Leading: 32×32dp rounded square (SurfaceHigh bg, OnSurfaceVariant icon)
  - Danger variant: Error 8% bg, Error icon
- Middle column (flex 1):
  - Label (SpaceGrotesk 13.5sp W500, OnSurface) + optional inline badge (e.g. "OK" with green border/text)
  - **Inline current value** (JetBrainsMono 11sp, AccentPrimary) — this is the key change: "1.0× · 10s seek · HW · Fit"
  - Hint (SpaceGrotesk 11sp OnSurfaceMuted)
- Trailing: chevron right

**Group order:**
1. (Untitled) — Playback · Quality & codecs · Display · Library
2. "App" — Nav bar · Cast & Connect · About & diagnostics
3. "Danger zone" — Disconnect server (red Error styling)

#### 6.2 Detail pages — shared anatomy

Each detail page:
- **Back arrow + title** in the top bar (back arrow 18dp on left, title TitleLarge W600 next to it)
- Optional right action button (e.g. "Switch" on Server page)
- Scrollable Column of `DetailGroup`s

**`DetailGroup`:**
- Optional uppercase MetaMono title (padding 0 2dp 8dp), with optional badge ("Beta", "Power user") to its right
- Surface bg, ShapeSmall, Border, overflow clip
- Children: `DRow` instances separated by 1dp Border
- Optional footer (inside the container, separated by border-top) — usually for a wide action like "Clear image cache"

**`DRow` (two modes):**
- **Inline mode** (most common): single row — label + sub on left (flex 1), then mono value (AccentPrimary) + trailing widget (Switch / chevron / button)
- **Stacked mode**: label + sub on top, full-width body widget below (slider, chip row, palette picker)

**Compact slider** (`CSlider`) — replaces all dot-row sliders:
- Row layout: track (flex 1) + 60dp value bubble (right)
- Track: height 3dp, SurfaceHigh bg, AccentPrimary fill, 3 tick marks at 25/50/75% (1×7dp, OnSurfaceFaint)
- Thumb: 14dp circle, AccentPrimary fill, 2dp Bg border, 1dp AccentPrimary outer ring
- Value bubble: padding 4dp 8dp, accent-8%-bg, accent-25%-border, JetBrainsMono 11sp W600 AccentPrimary, 4dp radius

#### 6.3 Detail page · Playback

- **Defaults**: Default speed (chip row × 6: 0.5× / 0.75× / 1× / 1.25× / 1.5× / 2×) · Aspect ratio (chips: Fit / Crop / Stretch) · Auto-play next (switch) · Auto-rotate on play (switch)
- **Seeking**: Double-tap seek (CSlider 5–60s) · Scrub sensitivity (CSlider 50–300 ms/px) · Show chapter thumbnails (switch)
- **Resume & skip**: Resume threshold (CSlider 0–30s) · Completion threshold (CSlider 50–100%) · Skip intro (CSlider 0–120s, "Off" when 0)
- **Player chrome** (badged "Power user"): Lock controls on idle / Show codec badge / Show queue position / Haptics on seek (switches)

#### 6.4 Detail page · Quality & codecs

- **Banner (top, not a DetailGroup):** AccentPrimary 6% bg, AccentPrimary 25% border, ShapeSmall, padding 14dp. Leading 24dp rounded-square accent icon. Title "Full codec support" + MetaMono detail "HW · HEVC · AV1 · H264 · FFmpeg extension loaded · 10-bit YUV 4:2:0 OK".
- **Decoder**: Preference (chip row: Auto / Prefer HW / Prefer SW) · Fallback on failure (switch) · Tunneling (switch)
- **Buffer**: Buffer size (chip row: Small · 15s / Medium · 50s / Large · 2min) · Pre-buffer on hover (switch)
- **Display & HDR**: HDR passthrough · Match refresh rate · Match resolution (switches)
- **Diagnostics**: Test codec (footer `TestBtn` "Run codec test") · Open codec details (chev)

#### 6.5 Detail page · Display

- **Theme**: 3-column **palette swatch grid** at top of the group (padding 12dp):
  - Each card: padding 10dp, ShapeSmall, accent-8%-bg + AccentPrimary border for active / Bg + Border for inactive
  - 28×28dp swatch (the accent color, ShapeSmall 6dp)
  - Name (SpaceGrotesk 11sp W600), desc below (MonoSmall, OnSurfaceMuted)
  - Active card has a 14dp accent-bg check in top-right corner
- DivLine then: AMOLED black mode (switch) · Reduce motion (switch)
- **Library layout**: Grid columns (Auto/2/3/4) · Card density (Compact/Comfortable/Spacious) · Long-press behavior (Play queue/Quick menu/Off) — all chip rows
- **Card chrome**: Rating · Play count · Resolution badge · Resume bar · Studio caption (switches)
- **Player**: Show chapter strip · Tap to peek info (switches)

#### 6.6 Detail page · Library

- **Sync with Stash**: Activity tracking · Sync ratings · Sync O-counter · Sync markers (switches, all default on)
- **Cache** (with footer `TestBtn` "Clear image cache · 184 MB used"): Image cache (CSlider 64–512 MB) · Cache duration (chip row: 1 day / 1 week / 30 days / Forever)
- **History**: Keep watch history · History on Home · Smart rails (switches)
- **Downloads · offline** (badged "Beta"): Allow downloads (switch) · Storage limit (chip row 2/5/10 GB/No limit) · Auto-purge watched (switch)

#### 6.7 Detail page · Server

- **Top action**: "Switch" outlined button in the top bar (changes server)
- **Status panel** (padding 16dp, Surface bg, Border, ShapeLarge):
  - Row: 8dp green dot + "CONNECTED" (MetaMono W700 #5DBB63 letterSpacing 1.4sp) + "last sync 2m ago" right
  - Endpoint URL (JetBrainsMono 14sp W500, OnSurface, word-break)
  - Sub: "Stash vX · TLS off · API key configured"
  - **4-column counts grid** (Scenes · Studios · Performers · Tags) — 1dp Border gap creates hairline; each cell: Bg bg, MetaMono label + JetBrainsMono 13sp value
- **Network** (DetailGroup): Endpoint · Latency · TLS · API key (with "Replace" outlined accent button)
- **Actions**: Refresh library now · Trigger server scan · Edit connection (all chev rows)
- **Danger zone**: Disconnect server (red Error styling, chev Error color)

#### 6.8 Detail page · About & diagnostics

- **Version block** (centered, padding 28dp 0 18dp):
  - 56×56dp logo (the diamond logo + bg cutout, scaled up)
  - "Slopper" (SpaceGrotesk 22sp W700, OnSurface)
  - MetaMono sub: "v{version} · build {hash} · {build-date}"
- **Capabilities**: Codec support (value green: "Full · HW + FFmpeg") · Hardware decoder · OpenGL · HDR · Display
- **Storage**: Image cache · Downloads · Database
- **Diagnostics**: View logs · Run network test (`TestBtn`) · Send debug report (anonymized)
- **Legal**: Open-source licenses · Privacy policy · Built on (Stash · ExoPlayer · Compose)

#### 6.9 Settings search overlay

Reached by tapping the hub's search field.

- Top bar replaces with back arrow + active search field (AccentPrimary-tinted border, AccentPrimary search icon, blinking cursor accent-colored)
- Result section: uppercase MetaMono "N matches" header
- Result container: Surface bg, Border, ShapeSmall
- **`SearchHit`** anatomy per row (padding 12dp, Border between rows):
  - Breadcrumb crumb (MetaMono, OnSurfaceMuted): e.g. "Playback · Resume & skip"
  - Label with highlighted match span (accent-25%-bg + AccentPrimary on the matched substring)
  - Right-aligned current value (JetBrainsMono 11sp AccentPrimary) on same row as label
  - Hint (BodySmall, OnSurfaceMuted, line-height 1.4) below

**Behavior:** tap a result → navigate to that detail page → briefly highlight the setting row (e.g. flash the accent-tinted border for 600ms).

---

### 7. ConnectionScreen

**Purpose:** First-run server setup.

**Layout:** Centered column, padding 24dp, justify-content center.

**Logo + wordmark** at top.

**Headline:** SpaceGrotesk 26sp W600 letterSpacing -0.6sp.

**Sub-text:** SpaceGrotesk 13sp OnSurfaceVariant lineHeight 1.5, margin-bottom 24dp.

**Input fields:**
- Label above (SpaceGrotesk 11sp OnSurfaceVariant, 4dp below)
- Container: padding 10dp 12dp, Surface bg, Border border, ShapeSmall
- Value: JetBrainsMono 12sp OnSurface
- Focus: border becomes AccentPrimary

**Success card:**
- AccentPrimary 8% bg, AccentPrimary 25% border, ShapeSmall
- Checkmark icon (AccentPrimary) + "Stash vX.X.X · N scenes" body + MetaMono detail row

**Error card:**
- Error 8% bg, Error 30% border
- X icon (Error) + message + MonoSmall detail row

**Buttons row:**
- "Test": flex 1, transparent bg, Border border
- "Connect & continue": flex 2, AccentPrimary bg, AccentOnPrimary text, W700

### 8. PlayerScreen (landscape)

**Purpose:** Fullscreen Media3 video player.

**Layout:** Landscape only. All controls overlay a black PlayerView.

**Top scrim:** gradient top → transparent, 90dp tall.  
**Bottom scrim:** gradient transparent → rgba(0,0,0,0.92), 160dp tall.

**Top bar:**
- Back button: 32×32dp rounded rect (white 6% bg, white 15% border, ShapeSmall)
- Title + queue position (SpaceGrotesk 14sp W600, queue in AccentPrimary JetBrainsMono)
- Studio subtitle (JetBrainsMono 10sp, white 60%)
- Right: pill chips row (height 26dp, ShapeSmall): `HW·HEVC`, speed, PiP, aspect, camera, shuffle, repeat

**Chapter strip** (above timeline, unique to Spine):
- Proportional segments, one per chapter
- Played/current segments: AccentPrimary fill; future: white 18%
- Chapter title below each segment (JetBrainsMono 9sp), time right-aligned
- Gap 4dp between segments

**Timeline:**
- Left: JetBrainsMono 12sp AccentPrimary (current position)
- Track: height 4dp, white 18% bg; buffered white 30%; played AccentPrimary; borderRadius 2dp
- Chapter notches: 2dp wide white marks at chapter positions
- Thumb: 18×18dp white circle, shadow
- Right: JetBrainsMono 12sp white (total duration)

**Transport row:**
- Lock icon (18dp, white 70%)
- Prev + Back10 + (big play/pause, 52×52dp AccentPrimary bg, ShapeMedium) + Fwd10 + Next
- Play/pause: AccentPrimary bg, AccentOnPrimary icon, 52dp, 10dp radius

---

## Modal flows

### Filter sheet (`ModalBottomSheet`)

- Opens to **84% of screen height**
- Top drag handle: 40×4dp, BorderStrong, centered, 8dp padding above
- Sheet header: title "Filters" (TitleLarge W600) + X close button (30dp circle Surface bg)
- Background: Bg (not Surface — deepest level)
- Top border: BorderStrong

**Active filter strip** (below header, before sections):
- Horizontal FlowRow of dismissable chips
- Each chip: AccentPrimary 10% bg, AccentPrimary 30% border, AccentPrimary text, dismiss X icon

**Sections in order:** Sort by · Duration · Released · Min resolution · Orientation · Rating range · Play count · O-counter · Flags

**Section header:** SpaceGrotesk 13sp W600 OnSurface, value right in AccentPrimary JetBrainsMono.

**Chip rows:** FlowRow, chips 6dp 11dp padding, SpaceGrotesk 11.5sp W500:
- Unselected: transparent bg, Border
- Yes/active: AccentPrimary 12% bg, AccentPrimary 30% border
- No/negative: Error 10% bg, Error 30% border (tri-state flags only)

**Range slider:** Two thumbs on a single track. Track height 4dp, filled region AccentPrimary. Thumbs: 14dp circle, AccentPrimary fill, 2dp Bg border.

**Footer (sticky):**
- "Reset" ghost left
- "Save view" outlined center
- "Apply · N results" AccentPrimary filled right

### Search (expanded state)

- Full-screen overlay on Library
- Back arrow + active search field (AccentPrimary border when focused)
- Scope chips below field: All / Scenes / Studios / Performers / Tags (with counts)
- Active scope: AccentPrimary 12% bg, AccentPrimary 30% border
- "Top result" section: type-specific card (studio, performer, etc.) with AccentPrimary bg highlight on matched text
- "Scenes · N" section: list rows with thumbnail (86×50dp), title, highlighted studio name
- "Performers · N": pill chips with inline avatar

### More sheet

- Height 62%
- Two groups: Browse (Tags · Markers · History) and App (Settings · Customize nav bar · Cast)
- Each row: accent-colored leading icon + title (TitleSmall W500) + sub (MetaMono) + trailing chevron
- Group containers: Surface bg, Border, ShapeSmall

### Customize nav bar sheet

- Height 76%
- Info banner: AccentPrimary 8% bg, AccentPrimary 20% border, note text
- Each item row: leading icon (AccentPrimary when checked) + label + checkbox (18dp, 4dp radius, AccentPrimary when checked) + drag handle
- Disabled rows (at 4-tab cap): opacity 0.4
- Footer: Cancel ghost + Apply AccentPrimary

### Marker editor sheet

- Height 84%
- Mini timeline at top: full-width, 3dp track, accent fill, marker dots (10dp, Warning fill, 2dp Bg border)
- List of markers: 64×36dp thumbnail + title + tag label (AccentPrimary) + timestamp right + overflow menu
- Active marker row: AccentPrimary 6% bg, AccentPrimary 30% border
- Footer: "+ Add marker at current time (X:XX:XX)" full-width outlined button

### Player settings panel (slide-in)

- Right-anchored panel, ~40% of screen width in landscape
- Bg: `rgba(11,15,22,0.95)` + blur 20dp, left border BorderStrong
- Speed chips (6 options), Audio track radio list, Subtitle radio list, video info key/value rows, Up next mini-queue
- Up next rows: 50×28dp thumbnail + title (SpaceGrotesk 11sp W500) + studio/duration (MonoSmall)
- Active row (NEXT): AccentPrimary 10% bg, AccentPrimary 30% border, "NEXT" label AccentPrimary

---

## Real-content states

### Home · loading skeleton

- Skeleton shapes match final layout: resume card (full-width × 88dp) + 3 rail stubs
- Shimmer: gradient scan animation (200% wide bg, sliding left→right, 1.4s linear infinite)
- Thin accent progress bar top of screen (1dp, accent glow, sliding)

### Home · empty + scanning

- Illustration area: 56×56dp film icon in Surface container
- Headline "Library's empty — for now." (22sp W600)
- Scanning progress card: pulsing dot (accent, 1.5s ease-in-out) + "Scanning · N / N files" + percentage
- "Trigger scan now" outlined button

### Library · no results

- Search icon (48dp circle Surface bg)
- "Nothing matches." (18sp W600) + recovery copy
- Primary button: "Clear all filters · keep search" (AccentPrimary)
- Secondary button: "Reset to defaults" (outlined)
- "Did you mean" card: pill suggestions in AccentPrimary

### Library · server error

- Error icon (48dp, Error 10% bg, Error 30% border)
- "Can't reach the server." headline
- Error detail card: MonoSmall error code + ETIMEDOUT message
- Retry (AccentPrimary) + Server settings (outlined) buttons
- "N cached scenes available" hint below

### Connection · wrong-server error

- Error card: Error 8% bg + Error 30% border, close icon, message + MonoSmall response detail
- "Retry test" (AccentPrimary) + "Help · find my server" (outlined) buttons

### Detail · unscraped scene

- Hero area: Surface bg with centered film icon + "No thumbnail · run scrape" MetaMono
- Filename in JetBrainsMono 14sp (word-break: break-all)
- "Play raw" primary CTA, "Scrape now" + "Edit manually" secondary row
- File metadata card: codec, resolution, bitrate, duration, hash in key/value mono layout

---

## Component inventory (Compose mapping)

| Design component | Suggested Compose impl |
|---|---|
| `SBottomNav` floating pill | Custom `Row` inside `Box` with `Modifier.align(BottomCenter)`. Not `NavigationBar`. |
| `SSceneCard` | Existing `SceneCard.kt` — update colors, remove gradient, add chapter-strip variant |
| `SpineResumeCard` | New `ResumeCard.kt` composable in `core/designsystem/component/` |
| `SpineFilterChip` | `FilterChip` M3 with custom colors override |
| `SheetShell` | `ModalBottomSheet` with custom drag handle + header |
| `SpineMeta` grid | `LazyVerticalGrid` 2-col inside `Column` |
| Chapter strip | Custom `Canvas`-drawn composable, same approach as existing `TimelineBar` |
| Shimmer skeleton | Shimmer library or custom `Animatable` brush |
| `SpineSwitch` | M3 `Switch` with custom `SwitchColors` |

---

## Assets

- **No image assets** — all thumbnails/posters come from the Stash server via Coil 3 (`AsyncImage` with API-key header injection, unchanged from existing implementation).
- **Icons** — all icons are from Material Symbols (outlined variant). No custom icon assets needed.
- **Fonts** — Space Grotesk and JetBrains Mono are available as Google Fonts downloadable fonts for Android.

---

## Files in this package

| File | Purpose |
|---|---|
| `README.md` | This document — full design spec, token list, screen-by-screen anatomy |
| `CHANGELOG.md` | Version history (v1.0 → current). Bump alongside `SLOPPER_VERSION` |
| `Slopper Spine.html` | Full hi-fi prototype — all screens, flows, states, settings, palette switcher, in-page changelog |
| `Slopper Redesign.html` | Original 3-direction exploration (Reel / Spine / Cinema) for reference |
| `screens-spine.jsx` | Core screen components source |
| `spine-flows.jsx` | Modal flow components source |
| `spine-states.jsx` | Loading / error / empty state components source |
| `spine-settings.jsx` | Settings hub + 7 detail-page components (v3.0) |
| `spine-palettes.jsx` | Three accent palette definitions |
| `spine-app.jsx` | Top-level page composition + `SLOPPER_VERSION` constant + in-page changelog data |
| `theme.jsx` | Full design token object (all 3 directions) |
| `icons.jsx` | Icon component reference |
| `art.jsx` | Demo data schema (matches Stash GraphQL shape) |
| `phone-shell.jsx` | Phone bezel + status bar used by every prototype screen |

---

## Open questions for the developer

1. **Palette choice:** Which accent (Sage / Ember / Signal) should ship as the v1 default? Or expose all three in Settings → Display?
2. **Fonts:** Confirm downloadable fonts are acceptable (Google Fonts dependency). Alternative: bundle the `.ttf` files directly in `assets/`.
3. **Bottom nav customization:** The More sheet + Customize nav sheet replace the existing `NavCustomizeSheet`. Confirm the flow is correct — the 5th slot is no longer a permanent "More" icon; the More dot is always on the right.
4. **Filter sheet "Save view":** The design shows saved views as first-class Library pills. This requires a new local persistence model (e.g. `Room` table of `SavedView(name, filterState)`). Flag if this is out of scope for v1.
5. **Chapter strip in player:** The chapter strip is a new UI component sitting above the timeline. It reads `scene.markers` sorted by time. Confirm the data is already available via the existing `PlayerViewModel`.
6. **Edge-to-edge insets:** Status bar region is 38dp (Compose `WindowInsets.statusBars`). The Home top bar and Detail hero must extend behind it with a transparent status bar.

---

*End of handoff document.*
