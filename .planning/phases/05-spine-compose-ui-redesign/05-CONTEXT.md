# Phase 5: SPINE — Compose UI Redesign - Context

**Gathered:** 2026-05-19
**Status:** Ready for planning
**Mode:** `--auto` (single-pass, recommended defaults; no AskUserQuestion turns)

<domain>
## Phase Boundary

Apply the Spine visual direction to all existing Slopper screens and modals: replace design tokens, swap the bottom nav component, add the chapter strip to the player, and update every screen composable to use Spine colors/typography/shapes — without adding new routes, new data sources, or changing any business logic.

</domain>

<spec_lock>
## Requirements (locked via SPEC.md)

**12 requirements are locked.** See `05-SPEC.md` for full requirements, boundaries, and acceptance criteria.

Downstream agents MUST read `05-SPEC.md` before planning or implementing.

**In scope (from SPEC.md):**
- Color/typography/shape/spacing design token replacement in `core/designsystem/`
- Floating pill bottom nav (replaces `NavigationBar`)
- `SceneCard.kt` color/layout update + `SpineResumeCard.kt` + `ChapterStrip` (new)
- All 8 screens: Home, Library, Detail, Browse, Settings, Connection, PlayerScreen
- 6 modal flows: filter sheet, more sheet, nav customize sheet, marker editor, search overlay, player settings panel

**Out of scope (from SPEC.md):**
- Filter "Save view" persistence (deferred v1.x)
- Ember/Signal palette picker (Sage only for v1)
- Tablet/foldable layout, light theme, new routes/features

</spec_lock>

<decisions>
## Implementation Decisions

### Decision 1 — Plan Wave Structure
**D-01:** 3 plans in a 2-wave cascade:

- **Plan 5.1** (wave 1, autonomous, SPINE-01/02/03/04 + new components): Design system foundation. Must land before any screen work.
  - Tasks: (a) Color.kt → Spine tokens; (b) Type.kt → Space Grotesk + JetBrains Mono; (c) Theme.kt → updated shapes/spacing; (d) BottomNav.kt → floating pill; (e) SceneCard.kt → Spine update; (f) new `SpineResumeCard.kt` + `ChapterStrip` composable in `core/designsystem/component/`

- **Plan 5.2** (wave 2, autonomous, SPINE-05/06/07/08 + depends_on plan-5.1): Feature screens group 1.
  - Tasks: HomeScreen, LibraryScreen, DetailScreen, BrowseScreen — all updated to use Spine tokens

- **Plan 5.3** (wave 2, autonomous, SPINE-09/10/11/12 + depends_on plan-5.1, parallel with 5.2): Feature screens group 2 + player + modals.
  - Tasks: SettingsScreen, ConnectionScreen, PlayerScreen (chapter strip + scrims + transport), all 6 modal flows

**Parallelism check:** Plan 5.2 touches `feature/{home,library,detail,browse}`; Plan 5.3 touches `feature/{settings,connection,player}` + `core/ui/nav/`. No overlapping files — safe to run in parallel.

**Commit budget:**
- Plan 5.1: ≤ 5 commits
- Plan 5.2: ≤ 5 commits (group by screen or by area)
- Plan 5.3: ≤ 6 commits
- Total: ≤ 16 atomic commits

### Decision 2 — Bottom Nav Blur Implementation
**D-02:** `Modifier.blur()` requires API 31+ and minSdk is 26. Graceful degradation approach:

```kotlin
// In the floating pill composable:
val modifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    Modifier.graphicsLayer {
        renderEffect = BlurEffect(20f, 20f, TileMode.Clamp)
    }
} else {
    Modifier  // no blur on API 26-30; semi-transparent bg provides visual separation
}
```

The pill bar background is `Surface.copy(alpha = 0.92f)` (not pure transparent), so it remains visually legible on all API levels. The blur is progressive enhancement. No `accompanist-placeholder` or external shimmer library needed.

### Decision 3 — Google Fonts Implementation
**D-03:** Add `androidx.compose.ui:ui-text-google-fonts` to `gradle/libs.versions.toml`. This is part of the Compose BOM — no explicit version pin needed:

```toml
[libraries]
androidx-compose-ui-text-google-fonts = { module = "androidx.compose.ui:ui-text-google-fonts" }
```

Add `implementation(libs.androidx.compose.ui.text.google.fonts)` to `core/designsystem/build.gradle.kts`.

Font declaration in `core/designsystem/`:
```kotlin
private val provider = GoogleFontProvider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)
val SpaceGrotesk = FontFamily(
    Font(GoogleFont("Space Grotesk"), provider, weight = FontWeight.Light),
    Font(GoogleFont("Space Grotesk"), provider, weight = FontWeight.Normal),
    Font(GoogleFont("Space Grotesk"), provider, weight = FontWeight.Medium),
    Font(GoogleFont("Space Grotesk"), provider, weight = FontWeight.SemiBold),
    Font(GoogleFont("Space Grotesk"), provider, weight = FontWeight.Bold),
)
val JetBrainsMono = FontFamily(
    Font(GoogleFont("JetBrains Mono"), provider, weight = FontWeight.Normal),
    Font(GoogleFont("JetBrains Mono"), provider, weight = FontWeight.Medium),
    Font(GoogleFont("JetBrains Mono"), provider, weight = FontWeight.SemiBold),
)
```

**Fallback:** If Google Fonts unavailable at runtime, Compose silently falls back to `FontFamily.Default`. No crash, no special handling needed.

**`res/values/font_certs.xml`:** The `com_google_android_gms_fonts_certs` resource array must be declared. Use the standard `preloadedfonts.xml` from the Google Fonts documentation or include it in `core/designsystem/src/main/res/values/`.

### Decision 4 — Accent Palette Architecture
**D-04:** Replace `StashColors` object with a new `SpineColors` object containing Spine tokens. Three accent palette data classes defined as constants — Sage hardwired for v1:

```kotlin
// Color.kt — replace StashColors with SpineColors
object SpineColors {
    val Bg           = Color(0xFF0A0D12)
    val Surface      = Color(0xFF11151C)
    val SurfaceHigh  = Color(0xFF1A2030)
    val SurfaceTop   = Color(0xFF232B3D)
    // Text
    val OnSurface        = Color(0xFFEAEEF6)
    val OnSurfaceVariant = Color(0xFF8C95A8)
    val OnSurfaceMuted   = Color(0xFF525B6E)
    val OnSurfaceFaint   = Color(0xFF2F3645)
    // Accent — Sage (v1 default)
    val AccentPrimary    = Color(0xFF9DC83C)
    val AccentPrimaryDim = Color(0xFF6E9028)
    val AccentOnPrimary  = Color(0xFF0B1402)
    // Cool accent (constant across palettes)
    val AccentCool    = Color(0xFF7FB6FF)
    val AccentCoolDim = Color(0xFF4A75B6)
    // Semantic
    val Warning = Color(0xFFFFCC44)
    val Error   = Color(0xFFFF5860)
    val Success = Color(0xFF5DBB63)
    // Borders
    val Border       = Color(0xFFA0B4DC).copy(alpha = 0.10f)
    val BorderStrong = Color(0xFFA0B4DC).copy(alpha = 0.22f)
}
```

Old `StashColors` is deleted; `StashTheme` updated to use `SpineColors`. All call sites using old names (`SurfaceBase`, `SurfaceLow`, etc.) will fail to compile — this cleanly surfaces regressions.

### Decision 5 — Chapter Strip Placement in Player
**D-05:** `ChapterStrip` is a new `@Composable` added to `PlayerTimeline.kt` (logical home for timeline-related composables, aligns with Phase 4 POLISH-01 split). It uses `Canvas` drawing similar to existing `TimelineBar`.

In `PlayerScreen.kt`, chapter strip placement: inside the bottom scrim overlay `Box`, positioned **above the `TimelineBar`** call:

```kotlin
// In the bottom scrim Box (inside Box(Modifier.fillMaxSize().background(Color.Black))):
Column(
    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
    verticalArrangement = Arrangement.Bottom
) {
    ChapterStrip(
        markers = state.current?.markers.orEmpty().toPersistentList(),
        positionMs = position.positionMs,
        durationMs = position.durationMs,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp)
    )
    TimelineBar(...) // existing
}
```

`position` is from `viewModel.position.collectAsStateWithLifecycle()` — already collected in PlayerScreen.

### Decision 6 — Gradient Specifications
**D-06:** Exact `Brush` definitions for all gradients:

**SceneCard:**
```kotlin
val cardGradient = Brush.verticalGradient(
    0.45f to Color.Transparent,
    1.0f to Color(0xEB0A0D12)  // Bg at 92% opacity
)
```

**PlayerScreen top scrim:**
```kotlin
val topScrim = Brush.verticalGradient(
    colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent),
    startY = 0f,
    endY = Float.POSITIVE_INFINITY  // 90dp tall Box bounds it
)
```

**PlayerScreen bottom scrim:**
```kotlin
val bottomScrim = Brush.verticalGradient(
    colors = listOf(Color.Transparent, Color(0xEB000000)),
    startY = 0f,
    endY = Float.POSITIVE_INFINITY  // 160dp tall Box bounds it
)
```

### Decision 7 — Token Migration Approach
**D-07:** Hard migration — old token names are deleted, not aliased. Any code referencing `StashColors.SurfaceBase`, `StashColors.SurfaceLow`, etc. will fail to compile, surfacing regressions cleanly. The planner must grep all usage of old token names before migrating `Color.kt`.

Pre-migration grep: `grep -rn 'StashColors\.' feature/ core/ app/ | grep -v '\.planning'` — all hits must be updated in the same plan before the `Color.kt` rename.

### Decision 8 — Testing for UI Phase
**D-08:** Do NOT create new test files. Instead:
1. Update the 7 existing Compose smoke tests (from Phase 4 POLISH-05) to compile after design token changes.
2. If a smoke test breaks due to a missing token or changed API, fix the test.
3. `./gradlew test` must remain BUILD SUCCESSFUL throughout.

Rationale: Phase 5 is a visual redesign — correctness is verified by visual UAT (deferred), not by unit tests. The smoke tests verify "composable renders without crash" — updating them to use Spine theme is sufficient.

### Decision 9 — Shape and Spacing Tokens
**D-09:** Shape values defined as local `val` constants in `Theme.kt` (not M3 `Shapes` object, since Spine uses component-specific shapes not a global system):

```kotlin
// In Theme.kt or a new Spine.kt:
val ShapeSmall  = RoundedCornerShape(6.dp)
val ShapeMedium = RoundedCornerShape(10.dp)
val ShapeLarge  = RoundedCornerShape(16.dp)
val ShapeCircle = CircleShape
```

Spacing: no new token object — use literal `dp` values from the spec (18dp screen edge, 10dp gutters, etc.) consistently. A future `SpineSpacing` object can be added if repetition becomes excessive.

### Decision 10 — Floating Pill Nav Positioning
**D-10:** The pill bar is rendered as an overlay at the BOTTOM of the root `Box` in `StashAppContent` (i.e., the `MainBottomBar` call site). It is NOT the `bottomBar` parameter of `Scaffold` — that would create the standard anchored bar. Instead, use `Box { ... MainBottomBar(Modifier.align(Alignment.BottomCenter)) }` with the content having `Modifier.padding(bottom = 100.dp)` (approximate pill height + 14dp bottom gap + safe area) to prevent content clipping behind the pill.

**More sheet trigger:** The "More" button is accessed by the Browse tab's screen content (as a dedicated action in the Browse top bar or a dedicated floating button), NOT as a 5th nav tab. This is per the design handoff: "More is accessed from Browse → More sheet (not a 5th tab icon)."

### Decision 11 — PlayerSettings Panel Implementation
**D-11:** The Player settings panel (right-anchored slide-in, ~40% width in landscape) is a new composable in `feature/player/`. It uses `AnimatedVisibility` with a `slideInHorizontally` animation from the right edge. It overlays the player surface (not a `ModalBottomSheet` — the handoff specifies it as a "slide-in panel"). State is a local `var showSettingsPanel by remember { mutableStateOf(false) }` in `PlayerScreen.kt`.

### Decision 12 — Spine Branch Strategy
**D-12:** Continue on `phase-2/comply-platform-compliance` (same continuous branch strategy as Phases 2-4). A new `phase-5/spine-compose-ui-redesign` branch would separate the commits cleanly, but the PR chain is already established. The final PR will cover all v1.0 work. If the user wants a clean phase-5-only PR in the future, `git log --no-merges phase-4..phase-5` can filter the commits.

### Accepted Risks
**AR-05-01:** Google Fonts API requires network connectivity on first render. On devices with no network at first launch, fonts fall back to system default. This is visually acceptable for v1.
**AR-05-02:** `Modifier.blur()` unavailable on API 26-30 (affects ~3% of Android devices in 2026). Pill bar uses semi-transparent background instead. Behavior is identical; blur is cosmetic.
**AR-05-03:** Hard token migration (delete old names) will break if any feature module references StashColors directly with the old names. Compile failure surfaces this cleanly; no silent regressions.

### Claude's Discretion
- Exact `Canvas` drawing code for `ChapterStrip` (proportional segments, 2dp notches) — planner reads `TimelineBar` implementation as the reference pattern
- Whether `font_certs.xml` needs to be committed or is generated by `ui-text-google-fonts` library
- Exact padding values for inline top bar elements (handoff says 4dp top, 18dp horizontal, 12dp bottom — planner uses these)
- Order of `ThemeBar` in scrim Box — Column vs separate Box alignment layers

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase 5 Design Spec
- `.planning/phases/05-spine-compose-ui-redesign/05-SPEC.md` — 12 locked requirements (MANDATORY)
- `.planning/phases/05-spine-compose-ui-redesign/05-UI-SPEC.md` — Complete UI design contract (MANDATORY)
- `design_handoff_slopper_spine/README.md` — High-fidelity pixel-level specs for all screens (MANDATORY)

### Prior Phase Constraints
- `.planning/phases/02-comply-platform-compliance/02-CONTEXT.md` — COMPLY-01 edge-to-edge (prerequisite for Spine full-bleed)
- `.planning/phases/03-perf-measured-wins/03-CONTEXT.md` — PERF-03 ImmutableList<Marker> must survive in ChapterStrip
- `.planning/phases/04-polish-test-pyramid/04-CONTEXT.md` — POLISH-01 PlayerScreen split (PlayerTimeline.kt is where ChapterStrip lives)

### Key Implementation Files
- `core/designsystem/src/main/java/io/stashapp/android/core/designsystem/theme/Color.kt` — Replace StashColors with SpineColors
- `core/designsystem/src/main/java/io/stashapp/android/core/designsystem/theme/Type.kt` — Add Space Grotesk + JetBrains Mono
- `core/designsystem/src/main/java/io/stashapp/android/core/designsystem/theme/Theme.kt` — Wire updated tokens + shapes
- `core/designsystem/src/main/java/io/stashapp/android/core/designsystem/component/SceneCard.kt` — Update colors/gradient/metadata
- `core/ui/src/main/java/io/stashapp/android/core/ui/nav/BottomNav.kt` — Replace NavigationBar with pill composable
- `feature/player/src/main/java/io/stashapp/android/feature/player/PlayerTimeline.kt` — Add ChapterStrip composable
- `feature/player/src/main/java/io/stashapp/android/feature/player/PlayerScreen.kt` — Chapter strip + scrims + transport

</canonical_refs>

<code_context>
## Codebase Context

### Current Token Usage to Migrate
Pre-migration grep command for planner:
```bash
grep -rn 'StashColors\.' feature/ core/ app/ --include="*.kt" | grep -v '.planning' | wc -l
```
All hits must be updated before `Color.kt` is renamed.

### Phase 4 Split Baseline (PlayerTimeline.kt)
- `TimelineBar(...)` at line 44
- `ScrubPreviewCard(...)` at line 202
- `BannerPill(...)` at line 244
- `ChapterStrip` goes ABOVE `TimelineBar` in the player's bottom scrim area

### Existing Composable Signatures to Update
- `SceneCard` in `core/designsystem/component/SceneCard.kt` — already uses `AsyncImage` (Coil 3, unchanged)
- `MainBottomBar` in `core/ui/nav/BottomNav.kt` — uses `NavigationBar` (to be replaced)
- `MoreSheet` in `core/ui/nav/BottomNav.kt` — Phase 2 added; update styling but keep the ModalBottomSheet wrapper

### Google Fonts Library
Not yet in catalog. Add: `androidx-compose-ui-text-google-fonts = { module = "androidx.compose.ui:ui-text-google-fonts" }` (no version — BOM-managed). Add to `core/designsystem/build.gradle.kts`.

### Phase 4 Smoke Tests That Need Updating
When Spine tokens replace old `StashColors`, these 7 test files may need their `setContent { ... }` wrapped in `StashTheme { ... }` to compile:
- `feature/home/.../HomeScreenSmokeTest.kt`
- `feature/library/.../LibraryScreenSmokeTest.kt`
- `feature/player/.../PlayerScreenSmokeTest.kt`
- (+ 4 others)

### Anti-Coupling (from Phase 2 carry-forward)
`grep -rn 'import .*Spine' feature/ core/ app/` → 0 hits after all Plan 5.1 changes land. The Spine design tokens are named `Bg`, `Surface`, `AccentPrimary` etc. — not `Spine*` — so this grep correctly returns 0.

</code_context>
