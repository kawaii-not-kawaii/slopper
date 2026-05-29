# Slopper — UI Design Handoff

> A handoff document for redesigning the UI of **Slopper**, a native Android client for the
> [Stash](https://github.com/stashapp/stash) self-hosted media server. This document describes
> what currently exists, screen-by-screen, so a designer can propose a redesign without needing
> to read the codebase.

---

## 1. App identity and purpose

**Slopper** is a native Android client for [Stash](https://github.com/stashapp/stash), a
self-hosted media library/server. The Stash server stores video scenes plus rich metadata
(performers, studios, tags, markers, ratings, play counts, resume positions). Slopper is the
phone-side front-end for browsing, searching, and playing that library.

**Design influences (current):**

- **Plex** for the overall information architecture and dark aesthetic (rail-based home, big
  scene cards with 16:9 thumbnails, hero-image scene detail).
- **MX Player** for the fullscreen video player chrome (dense utility strip of toggles,
  flat play/pause, gesture-driven scrub).
- **Stash web UI** for which collections matter (Scenes, Performers, Studios, Tags) and
  which filters are first-class (resolution, duration, rating, organized flag, markers,
  interactive, captions, dates).

**Differentiators vs. Plex:**

- Warm **amber** accent (`#F0A037`) instead of Plex orange — meant to feel like a film
  projector, distinct from the Plex brand.
- Cool **teal** secondary (`#4FC8D9`) for links, progress, and "info" affordances.
- Slightly-blue charcoal surfaces (not pure black) to be gentler on OLED panels over long
  sessions.
- Bottom-bar navigation is **user-customizable** (pick which 4 tabs appear, the rest spill
  into a "More" sheet).

**Platform constraints worth knowing for the redesign:**

- Phone-only for now. Landscape is required for the player; portrait everywhere else.
- Built on Jetpack Compose + Material 3, but the Material defaults are heavily overridden —
  feel free to propose a design that diverges from Material if it serves the product.
- Edge-to-edge rendering is on (`enableEdgeToEdge()`), so designs must account for the
  status bar / nav-gesture insets.
- Highest-refresh-rate panels (90/120 Hz) are explicitly opted into — animations should
  feel fluid.

---

## 2. Design system

All references below point to `core/designsystem/src/main/java/io/stashapp/android/core/designsystem/`.

### 2.1 Color palette — `theme/Color.kt`

The entire app is dark-only. There is **no light theme**.

**Surfaces** — five graduated tiers, app background → modal:

| Token            | Hex         | Usage                                              |
|------------------|-------------|----------------------------------------------------|
| `SurfaceBase`    | `#0B0E13`   | App scaffold background, status-bar-flush regions  |
| `SurfaceLow`     | `#121722`   | Cards, rails, bottom navigation bar                |
| `SurfaceMed`     | `#1A2030`   | Elevated cards, bottom sheets                      |
| `SurfaceHigh`    | `#232B3D`   | Dialogs, menus, player chips                       |
| `SurfaceHighest` | `#2E3750`   | Highest-elevation modals (rarely used)             |

**Text**:

| Token              | Hex         | Usage                                            |
|--------------------|-------------|--------------------------------------------------|
| `OnSurface`        | `#E8EBF2`   | Primary text                                     |
| `OnSurfaceVariant` | `#A8B0C0`   | Secondary text, metadata                         |
| `OnSurfaceMuted`   | `#6C7488`   | Tertiary text (timestamps, captions)             |
| `OnSurfaceFaint`   | `#3F4656`   | Disabled / dividers                              |

**Brand / accent**:

| Token               | Hex         | Usage                                            |
|---------------------|-------------|--------------------------------------------------|
| `AccentPrimary`     | `#F0A037`   | Warm amber. Play affordances, ratings, primary CTAs, resume progress |
| `AccentPrimaryDim`  | `#C68129`   | Pressed / container state of primary             |
| `AccentOnPrimary`   | `#1A0F00`   | Text on amber surfaces                           |
| `AccentSecondary`   | `#4FC8D9`   | Cool teal. Studio names, tag chips, codec badge, queue position |
| `AccentSecondaryDim`| `#338996`   | Pressed / container state of secondary           |

**Semantic**:

| Token     | Hex       | Usage                              |
|-----------|-----------|------------------------------------|
| `Success` | `#5DBB63` | (Reserved — used sparingly)        |
| `Warning` | `#E8C547` | Rating stars, "not ready" badges   |
| `Error`   | `#E85757` | Errors, favorite hearts            |

**Overlays**:

| Token         | Alpha+Hex     | Usage                                  |
|---------------|---------------|----------------------------------------|
| `Divider`     | `#1FFFFFFF`   | Horizontal dividers (12% white)        |
| `ScrimStrong` | `#D9000000`   | Press overlays, bottom of hero images  |
| `ScrimMedium` | `#99000000`   | Chip backgrounds over images           |
| `ScrimSoft`   | `#66000000`   | Subtle dimming                         |

### 2.2 Typography — `theme/Type.kt`

System default sans-serif (`FontFamily.Default`). The redesign is welcome to propose a
custom typeface. Full Material 3 type scale is defined; the screens lean on this subset:

| Style            | Size / Weight       | Where used                                          |
|------------------|---------------------|-----------------------------------------------------|
| `headlineMedium` | 22 sp SemiBold      | Connection screen title, Detail screen scene title  |
| `titleMedium`    | 18 sp Medium        | Section headers (rails on Home, sections in Detail) |
| `titleSmall`     | 13 sp Medium        | Card titles, settings rows                          |
| `bodyMedium`     | 13 sp Regular       | Body copy, descriptions                             |
| `bodySmall`      | 12 sp Regular       | Captions, metadata under cards                      |
| `labelMedium`    | 11 sp Medium        | Chip labels, settings section headers               |
| `labelSmall`     | 10 sp Medium        | Pill/chip text on cards, timestamps                 |

Letter-spacing is slightly positive on labels (`0.2–0.5 sp`) to keep them legible at small
sizes over photographic backgrounds.

### 2.3 Spacing & shape

- **Base unit**: 4 dp grid. Spacing scale used in practice: 4 / 6 / 8 / 10 / 12 / 16 / 20 / 24 dp.
- **Screen edge padding**: 16–20 dp horizontally.
- **Grid gutters**: 12 dp horizontal, 16 dp vertical between cards.
- **Card corner radius**: 10 dp (scene cards), 8 dp (chips/buttons), 12 dp (dialogs/sheets), 6 dp (small pills).
- **Tap targets**: Icon buttons 36–48 dp, primary action buttons 56 dp circle (Detail), 44+ dp height.

### 2.4 Elevation / depth

The app does **not** rely on Material drop shadows. Depth is communicated entirely through
the five graduated surface colors above. A card sitting on `SurfaceBase` uses
`SurfaceLow`/`SurfaceMed`; a sheet over that uses `SurfaceMed`/`SurfaceHigh`. This is
deliberate — soft shadows on a dark background look muddy.

### 2.5 The SceneCard component — `component/SceneCard.kt`

This is the single most-reused composable in the app. It appears in every horizontal home
rail and fills the main library grid.

**Anatomy** (16:9 aspect ratio):

```
┌────────────────────────────────────────┐
│ [screenshot, cover-crop]      [★ 4.2]  │  ← rating pill (top-right)
│                                        │
│      [press → ▶ play icon overlay]     │
│                                        │
│ [10:42][1080p]              [▶ 7]      │  ← bottom-left: duration + resolution chips
│ ──────────────                          │      bottom-right: play-count chip (amber)
│ ████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░  │  ← resume progress bar (amber, 3 dp, bottom edge)
└────────────────────────────────────────┘
  Scene title here, max 2 lines       ← 8 dp gap, titleSmall, onSurface
```

**Behaviors:**

- **Tap** → opens scene detail.
- **Long-press** → starts the player with this card's containing list as the play queue,
  positioned at this index.
- **Press feedback** → animated dark scrim with centered 56 dp play-arrow icon.
- A vertical gradient scrim (transparent → ScrimStrong) covers the bottom 45% of the image so the
  chips and resume bar remain legible regardless of the source screenshot.
- Rating is shown in stars-out-of-5 (server returns 0–100, divided by 20).

---

## 3. Navigation

References: `app/src/main/java/io/stashapp/android/MainActivity.kt`,
`core/ui/src/main/java/io/stashapp/android/core/ui/nav/{Routes,BottomNav,NavCustomizeSheet}.kt`.

### 3.1 Top-level shape

The app is a single `Activity` hosting a Compose `NavHost`. **Two chrome modes:**

1. **Tab chrome** — bottom navigation bar visible. Routes: Home, Library (Scenes), Browse/*,
   Settings.
2. **Full-bleed** — no bottom bar. Routes: Connection (pre-auth), Scene Detail, Player.

### 3.2 Bottom navigation bar — `BottomNav.kt`

- `NavigationBar` (Material 3) with **`SurfaceLow` background**, no tonal elevation.
- Displays up to **4 user-selected tabs** plus an always-on "More" (Tune icon) on the right.
- Selected items use **filled icon + amber tint + amber-tinted pill indicator** (18% alpha).
- Unselected items use **outlined icon + `OnSurfaceVariant` tint**.
- Label style: `labelSmall` under each icon.

**Available tabs** (`MainNavItems.All`):

| ID          | Label      | Filled / outlined icon            | Route                |
|-------------|------------|-----------------------------------|----------------------|
| `home`      | Home       | `ViewModule` / `Home`             | `home`               |
| `scenes`    | Scenes     | `Movie` / `Movie`                 | `library`            |
| `studios`   | Studios    | `Storefront` / `Storefront`       | `browse/studios`     |
| `performers`| People     | `Person` / `Person`               | `browse/performers`  |
| `tags`      | Tags       | `Label` / `Label`                 | `browse/tags`        |

**Default visible**: Home, Scenes, Studios, People. Tags is in the More sheet by default.

### 3.3 More sheet — `BottomNav.kt`, `MoreSheet`

A `ModalBottomSheet` (`SurfaceLow` background) that appears when the user taps "More". Two
sections:

- **Browse** — list of nav items not currently in the visible tab set.
- **App** — two `ListItem`s: "Settings" (Tune icon) and "Customize nav bar" (ViewModule icon).

### 3.4 Customize-nav sheet — `NavCustomizeSheet.kt`

A second `ModalBottomSheet` reached from More → "Customize nav bar". A vertical list of
checkboxes (one per `MainNavItem`), capped at 4 selections (the 5th slot is the always-on
More button). When at the cap, unchecked items render in the faint disabled color and their
checkboxes are non-interactive. Footer: TextButton "Cancel" / TextButton "Apply" (the Apply
button text is amber).

### 3.5 Routes (full list) — `Routes.kt`

| Constant          | Pattern                                              |
|-------------------|------------------------------------------------------|
| `Connection`      | `connection`                                         |
| `Home`            | `home`                                               |
| `Library`         | `library?preset={preset}`  (optional filter preset)  |
| `Detail`          | `scene/{sceneId}`                                    |
| `Browse`          | `browse/{kind}`  (kind ∈ performers/studios/tags)    |
| `Player`          | `player/{sceneId}?queueIds=...&index=N&startMs=ms`   |
| `Settings`        | `settings`                                           |

The library route accepts a **preset** like `performer:42` or `studio:7` or `tag:18`; tapping
an entity in any Browse screen navigates to `library?preset=…` so the same scenes grid does
double-duty as "all scenes" and "scenes for this entity".

---

## 4. Screens

Each section: top bar, body, key components, interactions, states.

### 4.1 ConnectionScreen — `feature/connection/.../ConnectionScreen.kt`

**Purpose**: First-run server setup. Asked for URL + (optional) API key + display name.

**Top bar**: None. Full-bleed centered card.

**Body** (centered column, `widthIn(max = 420.dp)`, vertically scrollable):

1. A large `Cloud` icon (56 dp, amber tint).
2. Headline "Connect to Stash" (`headlineMedium`).
3. Subtitle: "Enter your server URL. An API key is required if you have auth enabled."
4. `OutlinedTextField` — "Server URL", placeholder `http://192.168.1.10:9999`, URI keyboard.
5. `OutlinedTextField` — "API key (optional)", password-masked by default, with an
   eye-icon trailing toggle (Visibility / VisibilityOff). Autocorrect + IME-learning
   disabled (keys must not land in keyboard's personal dictionary).
6. `OutlinedTextField` — "Display name (optional)".
7. **Conditional success card** (after a successful Test): `Surface` with `surfaceContainerHigh`,
   rounded 12 dp, containing a `VerifiedUser` icon (amber) + "Connected to Stash v{version}" and
   a `{sceneCount} scenes · {performerCount} performers · {studioCount} studios · {tagCount} tags`
   line in muted text.
8. **Conditional error card**: same shape, `Error` icon (red), error message.
9. Two equal-weight buttons in a Row: `OutlinedButton` "Test" (shows inline
   `CircularProgressIndicator` while probing) and `Button` "Connect" (disabled until server
   info has been fetched).

**States**: blank → typing → testing (spinner in Test button) → success card visible /
Connect enabled → error card → Connected (navigates away).

**Interactions**: Test probes the server and populates the success/error card without
persisting credentials. Connect saves the encrypted server record and navigates to Home,
clearing the back stack.

---

### 4.2 HomeScreen — `feature/home/.../HomeScreen.kt`

**Purpose**: Dashboard of horizontal "rails" (Plex-style), one per curated list.

**Top bar** (`TopAppBar`, `surface` color):
- Title: "Home"
- Actions: `Refresh` icon, `Settings` icon.

**Body**: A `LazyColumn` of rails, 20 dp vertical spacing.

**Each rail** (`HomeRailRow`):
- Title (e.g. "Recently added", "Continue watching") — `titleMedium`/SemiBold, 16 dp horizontal padding.
- 10 dp gap.
- A `LazyRow` of `SceneCard`s, each `Modifier.width(220.dp)`, 12 dp horizontal spacing.
- 16 dp content padding at row ends.

**Rail state behavior**:
- Loading → 140 dp tall centered `CircularProgressIndicator`.
- Error → 80 dp tall, left-aligned `bodySmall` in error color: "Couldn't load: {message}".
- Empty (and not loading, not erroring) → the entire rail is **hidden** (zero vertical
  space — important for the "Continue watching" rail when the user has nothing in
  progress).

**Interactions**:
- Tap a card → scene detail.
- Long-press a card → play with the *current rail* as the queue, positioned at that card's index.

---

### 4.3 LibraryScreen + FilterSheet — `feature/library/.../{LibraryScreen,FilterSheet}.kt`

**Purpose**: The main scene browser. A paged grid backed by Paging 3, with search + filter
sheet, optionally pre-filtered by a route preset (performer/studio/tag).

#### LibraryScreen

**Top bar** (`LibraryTopBar`):
- Title: "Library" — replaced by an inline `OutlinedTextField` ("Search scenes",
  Search-action IME) when search is expanded.
- Actions (right): Search toggle (Search / Clear icon), Filters icon with a `Badge` dot
  when any filter is active, Settings icon.

**Body** (`ScenesGrid`):
- `LazyVerticalGrid` with `GridCells.Adaptive(minSize = 180.dp)` → 2 columns on most phones,
  3 on tablets.
- 12 dp horizontal contentPadding, 16 dp vertical spacing between rows, 12 dp between columns.
- Cards: `SceneCard` (described above).
- Append-loading row: full-width `CircularProgressIndicator` spanning all columns at the end
  of the grid while paging.

**States**:
- Refreshing + empty → centered `CircularProgressIndicator`.
- Refresh error + empty → centered Column with "Can't load scenes" (`titleMedium`) +
  error.message in muted bodySmall.
- Loaded + no results → centered muted text "No scenes match your filter."
- Loaded + results → grid.

**Interactions**:
- Tap a card → scene detail (`onSceneClick` carries the *full visible id list* as queue
  context, so the player can be invoked from the detail with proper "next/previous"
  available).
- Long-press a card → directly start the player with the visible id list as the queue.

#### FilterSheet (ModalBottomSheet)

Vertically scrollable, padded 20 dp horizontal, sections separated by 20 dp spacing. Each
section's title uses `titleSmall`/SemiBold.

**Sections (in order)**:

1. **Sort by** — `ExposedDropdownMenuBox` with a `SceneSort` enum (Date desc, Date asc,
   Rating, Random, Play count, etc.).
2. **Duration** — A `FlowRow` of `FilterChip`s: Any / preset buckets (short / medium / long
   etc., from `SceneDurationBucket`) / Custom. Custom expands to two `OutlinedTextField`s
   ("Min (min)" — "Max (min)"), digit-only, separated by an em-dash.
3. **Release date** — `FlowRow` of FilterChips: Any / Last week / Last month / Last year /
   This year.
4. **Minimum resolution** — `FlowRow` of FilterChips driven by `SceneResolution` enum.
5. **Orientation** — `FlowRow` of FilterChips for landscape/portrait/square.
6. **Rating (★ 0–5)** — A `RangeSlider` 0–100 with 19 steps; a `bodySmall` label below
   shows the current "x.x – y.y" range in star units.
7. **Play count** — A `Slider` 0–50, label "Any" / "At least N".
8. **O-counter** — A `Slider` 0–20, label "Any" / "At least N".
9. **Flags** — A `FlowRow` of **tri-state** filter chips: Organized, Has markers,
   Interactive, In progress, Has captions. Tri-state cycles label as
   `"{flag}"` → `"{flag}: yes"` → `"{flag}: no"` → unset. Selected (non-null) chips use
   an amber-tinted container (25% alpha).
10. **Save / Clear default** — `TextButton`s ("Save as default" or "Update default", and
    "Clear default" when one is saved). Kept visually subtle.
11. **Action row** — `TextButton` "Reset" on the left, then "Cancel" + primary `Button`
    "Apply" on the right. Apply commits to the ViewModel and dismisses the sheet.

**Interaction model**: All filter edits happen against **local sheet state**. Nothing is
applied until the user taps "Apply". "Reset" resets local state to defaults (does not
auto-apply). Apply commits to the ViewModel which updates the paged query and (if
appropriate) refreshes the grid.

---

### 4.4 BrowseScreen — `feature/browse/.../BrowseScreen.kt`

**Purpose**: A single composable that renders one of three entity grids based on the
`{kind}` route argument: **Performers**, **Studios**, or **Tags**.

**Top bar**:
- Navigation icon: `ArrowBack`.
- Title: "Performers" / "Studios" / "Tags" (or an inline search text field when search is expanded).
- Action: Search / Clear toggle.

**Body**: A shared `PagingGrid` wrapper that handles loading/error/empty:
- `LazyVerticalGrid` with `GridCells.Adaptive(minSize = 140.dp)` → 3–4 columns.
- 12 dp gutters, 16 dp vertical spacing.

**Cell shapes** differ per kind:

**Performers** (`PerformersGrid`):
- A **circular** Surface (1:1 aspect, CircleShape) with the performer's photo cropped to fill.
- Top-right overlay: a red `Favorite` heart (only when `favorite == true`).
- Below: name (`labelMedium`, centered, max 2 lines), then "{N} scenes" in muted `labelSmall`.

**Studios** (`StudiosGrid`):
- A **16:9 rounded** Surface (8 dp radius) with the studio logo/banner cropped to fill.
- Below: name (`labelMedium`/Medium, max 2 lines, left-aligned), then "{N} scenes" in muted.

**Tags** (`TagsGrid`):
- A **text-only** Surface card (12 dp radius, `surfaceContainer` color) padded 12 dp.
- Tag name rendered in **teal** (`AccentSecondary`) in `titleSmall`, max 2 lines.
- Below: "{N} scenes" in muted `labelSmall`.
- No image — tags don't have images in Stash.

**States**:
- Loading → centered spinner.
- Error → centered red error message.
- Empty → centered muted "No results".

**Interactions**:
- Tap a cell → navigate to `library?preset={kind}:{id}` so the user lands in a pre-filtered
  Library showing only that entity's scenes.

---

### 4.5 DetailScreen — `feature/detail/.../DetailScreen.kt`

**Purpose**: A Plex-style scene detail page with hero image, metadata, primary actions,
performer/tag/marker chips, and a details body.

**Top bar**: Transparent `TopAppBar`. Only a back arrow (no title) — the title lives in
the body so it can overlap the hero image.

**Body** (vertically scrollable `Column`):

1. **Hero** — 16:9 `AsyncImage` covering full width, with:
   - A bottom-fading gradient `0.3f to Transparent, 1f to background` so the title region
     blends into the page.
   - A **72 dp amber circular play button** (`CircleShape` Surface, `AccentPrimary` 92%
     alpha) centered on the hero, containing a 40 dp PlayArrow. Tapping it invokes Play
     (resuming from `resumeTimeSeconds` if > 2 s).

2. **Title + metadata strip** (20 dp horizontal padding):
   - Title (`headlineMedium`, max 3 lines, ellipsized).
   - A horizontal row of: studio name (teal `titleSmall`) · date (muted `bodySmall`) ·
     `Star` icon (warning yellow) + star rating, separated by middle-dot characters.

3. **Tech / stats pill row** (`FlowRow`): duration / resolution / video codec / audio codec /
   bitrate (in Mbps, 1 decimal) / play count `▶ N` (amber) / O-counter `● N` (amber) /
   `Interactive` (amber). Pills are 6 dp rounded `Surface`s with `surfaceContainerHigh`
   background; amber-accent pills swap to `AccentPrimary` / `AccentOnPrimary`.

4. **Primary play button** — Full-width Material `Button`, PlayArrow + label
   "Play" or "Resume from MM:SS". This is redundant with the hero play button but useful
   after the user scrolls.

5. **Action row** (`ActionRow`):
   - **Star rating**: label "Rating", followed by five `IconButton`s. Each star is 32 dp,
     icon 24 dp. Active stars use `Star` filled + Warning yellow; inactive use `StarBorder`
     muted. Tap to set rating to that threshold; tap an already-active star to step down one
     star (clearing at 0).
   - **Organized toggle**: a pill-shaped `Surface` with `CheckCircle` / `RadioButtonUnchecked`
     icon + "Organized" or "Mark organized". Active state uses amber background + dark text.
   - **O-counter stepper**: a rounded Surface containing `Remove` button + "● N" label
     (amber when > 0, otherwise muted) + `Add` button.

6. **Performers section** — `Section` titled "Performers" with a horizontal `LazyRow` of
   72 dp circular avatars (with name labels below in `labelSmall`). Each cell is 80 dp wide,
   110 dp tall.

7. **Tags section** — `Section` titled "Tags" with a `FlowRow` of pill-shaped tag chips
   (50 dp radius — fully rounded), `surfaceContainer` background, **teal** label text.

8. **Markers section** — `Section` titled "Markers" with a vertical Column of rows. Each
   marker row is a tappable `surfaceContainer` Surface (8 dp radius) containing:
   - `Bookmark` icon (amber).
   - Title (or primary tag name fallback) in `bodyMedium`, with an optional secondary
     `primaryTagName` line in muted `labelSmall`.
   - Right-aligned timestamp (e.g. `12:34`) in muted `labelMedium`.
   - Tapping → invokes the player with `startSeconds = marker.seconds`.

9. **Details text block** (if non-blank).

**States**: Loading (centered spinner), Error ("Can't load scene: …"), Loaded (the body
above).

---

### 4.6 PlayerScreen — `feature/player/.../PlayerScreen.kt`

**Purpose**: Fullscreen Media3/ExoPlayer-backed video player. **Landscape only** (sensor
landscape; user can lock with the rotation toggle). Inspired by MX Player's information-dense
control overlay rather than a minimal Netflix/YouTube look.

**Layout shape** (overlay over a black `PlayerView` AndroidView):

```
[scrim top]
┌── system bars ────────────────────────────────────┐
│ ⌄  Scene Title           3/12   [HW][1.0x▸][PiP]  │ ← top bar (close, title+queue, status chips)
│    Studio Name (teal)                              │
│  [⤿rotate][⬚aspect][📷shot][⇄shuffle][↻repeat]   │ ← MX-style utility strip
│                                                    │
│                  [VIDEO]                           │ ← gesture surface (tap, double-tap, drag)
│                                                    │
│  0:12  ───────●─────────────────────  -10:24       │ ← timeline bar (left time, scrub track, right time/-remaining)
│  [🔓unlock][↺-10][⏮prev]  ▶/⏸  [⏭next][↻+10][⬚]│ ← transport row
└────────────────────────────────────────────────────┘
[scrim bottom]
```

**Top control block** (top scrim gradient: `0xF2000000` → `0x80000000` → transparent, 260 px tall):

- **Row 1 (main top bar)**:
  - `KeyboardArrowDown` icon (44 dp) — close player.
  - **Title column**: title (white `titleSmall`, single line, weight 1) with a queue position
    suffix ("3/12") in **teal `labelSmall`**; subtitle (studio name) in `OnSurfaceVariant`
    `labelSmall` below.
  - **Right-side chip row** (all 36 dp tall, 10 dp corner radius, `widthIn(min=52.dp)`):
    - **CodecBadge** — teal-on-tinted-teal surface with thin teal border. Shows `HW` or `HW+FF` (with FFmpeg extension).
    - **SpeedPill** — `Speed` icon + speed value (e.g. "1.0x"). Active (non-1.0x) flips to amber background; inactive is dark with a faint white border.
    - **PipChip** — `PictureInPicture` icon on a dark surface with white-border.
- **Row 2 (utility strip)** — 5 small icon buttons (36 dp, icon 22 dp), tinted **amber when
  active**, otherwise white:
  - Rotation lock (`ScreenRotation` / `ScreenLockRotation` when on).
  - Aspect ratio cycle (`AspectRatio`) — cycles Fit → Crop (zoom) → Stretch.
  - Screenshot (`PhotoCamera`) — currently shows a "coming soon" banner.
  - Shuffle (`Shuffle`).
  - Repeat (`Repeat` / `RepeatOne`).

**Bottom control block** (bottom scrim gradient, 320 px tall):

- **TimelineBar**:
  - Left label: current position in `labelSmall` — turns **amber** while dragging.
  - Center: a `Canvas`-drawn track. Three layers: faint base track (`OnSurfaceFaint`),
    buffered progress (35% white-ish), played progress (`AccentPrimary` amber).
    Animated track height: 7 dp idle → 10 dp while dragging.
  - **Marker overlays**: small dots on the track for each scene marker (amber outer
    ring + white inner dot).
  - **Thumb**: amber circle, 7 dp idle → 10 dp while dragging.
  - Right label: total duration, or `-MM:SS` remaining if the user has tapped it (state is
    toggled by tapping the right label).
- **Transport row** (`SpaceBetween`):
  - LockOpen icon → tap to enter locked mode.
  - Replay10 (back N seconds based on doubleTapSeekSec preference).
  - SkipPrevious (36 dp icon, 48 dp button, disabled when no previous).
  - **PlayPauseFlat** — large 64 dp button, 52 dp flat icon. Pause/Play swap animated via
    `AnimatedContent` (scaleIn + fadeIn / scaleOut + fadeOut, 160 ms).
  - SkipNext.
  - Forward10.
  - AspectRatio (duplicate of the top one — convenient placement).

**Gesture model** (active when *not* locked):

- **Single tap** anywhere on the video → toggle controls. Controls auto-hide after 3 s of
  inactivity while playing.
- **Double-tap left/right** → step seek backward/forward by the configured
  `doubleTapSeekSec` (default 10 s). Shows a **StepSeekCallout** — a Canvas-drawn pair of
  ripple arcs around a centered text label like `+10s` or `-30s` (accumulates when tapped
  repeatedly within 800 ms).
- **Horizontal drag** → scrub. While dragging, a **ScrubPreviewCard** appears centered: a
  `SurfaceHigh` rounded panel showing target time (`titleMedium`), "of TOTAL" subtitle, and a
  delta line in amber (`+12s`).

**Locked mode**:

- Hides all controls. A single small unlock icon appears on the left edge when the user taps
  the screen; tapping it unlocks. While locked, no swipe/double-tap gestures are routed.

**Transient/affordance UI**:

- **BannerPill** — A pill at top-center with a thin amber left bar, `SurfaceHigh`/92%-alpha
  background, amber-tinted label. Slides + fades in/out for ephemeral messages like
  "Screenshot — coming soon".
- **Loading spinner** — centered `CircularProgressIndicator` (white) when the player is
  buffering with no media surface yet.
- **Error message** — centered white text "Playback error: …".

**Other notes**:
- The video surface uses Media3's native `PlayerView` wrapped in `AndroidView`. The video's
  fps is forwarded to `Surface.setFrameRate()` so VRR panels can seamlessly switch refresh
  rates.
- PiP is wired up via `PictureInPictureParams(setAspectRatio(16:9))`.
- Back button exits the player.

---

### 4.7 SettingsScreen — `feature/settings/.../SettingsScreen.kt`

**Purpose**: All user-tunable preferences (player, cache, display, behavior) plus account
controls. Layout is a vertically scrollable Column with section headers.

**Top bar**: `TopAppBar` with title "Settings". No back button (the user gets back via the
nav system).

**Section headers** (`SectionHeader`): `titleSmall`/Bold, **amber** color, padded
top-12/bottom-4 dp. Sections separated by a thin `HorizontalDivider` (12% white).

**Reusable primitives** the screen uses:

- **`SwitchPref`** — Row with title (`bodyMedium`) + optional subtitle (`labelSmall` muted),
  trailing Material `Switch` (track turns amber when checked). The whole row is clickable
  and toggles the switch.
- **`SliderPref`** — Title row with right-aligned current value (`labelMedium` muted), then a
  Material `Slider` underneath.
- **`ChipRowPref`** — Title above a Row of selectable Surface "chips". Selected chip has
  amber background + dark text; unselected uses `surfaceContainerHigh`.

**Sections**:

1. **Player**
   - SliderPref "Scrub sensitivity" (ms/px).
   - SliderPref "Double-tap seek" (seconds, integer steps).
   - ChipRowPref "Default speed" → 0.5x / 0.75x / 1x / 1.25x / 1.5x / 2x.
   - SwitchPref "Auto-play next" (subtitle: "Advance to the next item in queue").
   - SliderPref "Resume threshold" (0–30 s).
   - SliderPref "Completion threshold" (50–100%).
   - SliderPref "Skip intro" (0–120 s, "Off" when 0).
   - ChipRowPref "Video buffer" → Small (15s) / Medium (50s) / Large (2min).
   - ChipRowPref "Default aspect ratio" → Fit / Crop / Stretch.
   - ChipRowPref "Decoder preference" → Auto / Prefer HW / Prefer SW.
   - **CodecStatusCard** — A `surfaceContainerHigh` rounded card (12 dp radius) showing
     codec readiness: `CheckCircle` amber + "Full codec support", or `Warning` red +
     "FFmpeg detected but not loaded" / "Limited codec support". Body line shows
     `CodecCapabilities.statusLabel` in muted bodySmall.

2. **Cache**
   - SliderPref "Image cache size" (64–512 MB).
   - `OutlinedButton` (full-width) "Clear image cache" with a `DeleteSweep` leading icon.

3. **Display**
   - ChipRowPref "Grid columns" → Auto / 2 / 3 / 4.
   - SwitchPref "AMOLED black mode" (subtitle: "Pure black backgrounds for OLED screens").
   - SwitchPref "Rating on cards" (no subtitle).
   - SwitchPref "Play count on cards".
   - SwitchPref "Resolution on cards".

4. **App behavior**
   - SwitchPref "Activity tracking" (subtitle: "Sync resume position + play count to Stash").
   - SwitchPref "Auto-rotate player" (subtitle: "Sensor landscape when playing").

5. **Account**
   - `OutlinedButton` (full-width) "Disconnect server" — clears credentials and routes back
     to ConnectionScreen.
   - `OutlinedButton` (full-width) "Back".

---

## 5. Cross-cutting UX patterns

- **Empty rails hide entirely** rather than render a placeholder. Same logic could apply to
  Detail's optional sections (Performers/Tags/Markers/Details are each rendered only if
  non-empty).
- **Tri-state filter chips**: null → yes → no → null. Selected chips (non-null) take an
  amber 25% tint.
- **Long-press → play queue**: This is the second-tier interaction across the app. Any
  scrollable list of scenes uses it.
- **Resume-from-position** is surfaced in three places: (1) a thin amber progress bar
  along the bottom of `SceneCard`s, (2) the primary Detail play button switches to
  "Resume from MM:SS", (3) the player's initial `startMs` defaults to the saved resume
  position when the route doesn't specify one.
- **Network-backed images use Coil 3 (`AsyncImage`)**, with a custom loader that injects
  the Stash API key as a request header **only** on Stash-origin URLs.

---

## 6. Component inventory (quick reference)

| Component               | Defined in                                          | Notes                                       |
|-------------------------|------------------------------------------------------|---------------------------------------------|
| `SceneCard`             | `core/designsystem/component/SceneCard.kt`           | 16:9 scene tile, rating + duration chips, resume bar |
| `MainBottomBar`         | `core/ui/nav/BottomNav.kt`                            | NavigationBar with amber selection         |
| `MoreSheet`             | `core/ui/nav/BottomNav.kt`                            | Overflow ModalBottomSheet                  |
| `NavCustomizeSheet`     | `core/ui/nav/NavCustomizeSheet.kt`                    | Checkbox-list ModalBottomSheet, cap=4      |
| `FilterSheet`           | `feature/library/FilterSheet.kt`                      | Scrollable filter+sort ModalBottomSheet    |
| `PlayerControls`        | `feature/player/PlayerScreen.kt`                      | MX-Player-style overlay                    |
| `TimelineBar`           | `feature/player/PlayerScreen.kt`                      | Canvas-drawn scrub track + marker dots     |
| `StepSeekCallout`       | `feature/player/PlayerScreen.kt`                      | Canvas arc rings + delta label             |
| `ScrubPreviewCard`      | `feature/player/PlayerScreen.kt`                      | Centered scrub preview                     |
| `BannerPill`            | `feature/player/PlayerScreen.kt`                      | Ephemeral top-center notification          |
| `SwitchPref` / `SliderPref` / `ChipRowPref` | `feature/settings/SettingsScreen.kt` | Settings row primitives          |
| `CodecStatusCard`       | `feature/settings/SettingsScreen.kt`                  | FFmpeg/HW status                           |

---

## 7. What we want from the redesign

> _Placeholder — fill in with specific goals, references, and constraints before
> handing off to the designer._

- Tone / direction:
- Inspiration references:
- Pain points to fix:
- Things to preserve:
- Open questions:
