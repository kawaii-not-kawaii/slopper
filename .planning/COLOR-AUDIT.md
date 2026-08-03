# Colour audit — filter/settings/search redesign

Audited 2026-08-03 with `rg` across `app`, `core`, and `feature` Kotlin sources. Runtime accent-coloured UI must read `LocalAccentColors.current`; palette definitions, semantic colours, and video/image contrast overlays are intentionally fixed.

## Fixed

| Finding | Location | Resolution |
|---|---|---|
| Selected bottom-nav pill stayed sage under ember/signal | `core/ui/.../nav/BottomNav.kt:126` (consumer), selected background/icon/label in the same composable | The bar now reads `LocalAccentColors.current` and uses `primary` / `onPrimary`. |
| More-sheet icons stayed sage | `core/ui/.../nav/BottomNav.kt:293` | `MoreSheetItem` now reads the current accent. |
| Customize-nav banner, checkbox, selected icon, and Apply button stayed sage | `core/ui/.../nav/NavCustomizeSheet.kt:57` and its accent consumers | The sheet now uses the current palette's `primary` / `onPrimary` throughout. |
| Scene-card gradient duplicated the background token as raw ARGB | `core/designsystem/.../component/SceneCard.kt:104` | Replaced with `SpineColors.Bg.copy(alpha = 0.92f)`. |
| Player buffered-progress colour was raw ARGB | `feature/player/.../PlayerTimeline.kt:128` | Replaced with `SpineColors.OnSurfaceVariant.copy(alpha = 0.35f)`. |
| Player settings-panel background duplicated the app background as raw ARGB | `feature/player/.../PlayerSettingsPanel.kt:61` | Replaced with `SpineColors.Bg.copy(alpha = 0.95f)`. |

## Deliberate fixed colours — unchanged

These do not represent selectable app accent and therefore must not follow sage/ember/signal.

### Design-system and semantic definitions

- `core/designsystem/.../theme/Color.kt:18-45`: the canonical Spine surface, text, semantic, cool-tag, and fallback sage tokens. This is the token source, not a consumer bypassing the theme.
- `core/designsystem/.../theme/Theme.kt:28-30`: sage, ember, and signal palette definitions must contain their own fixed swatches.
- `core/designsystem/.../theme/Theme.kt:45-48`: the static dark-scheme seed is overwritten by `StashTheme` with the selected palette before provision; it is not a runtime accent leak.
- `core/designsystem/.../theme/Theme.kt:50,52,54,67,70`: black/white contrast roles and the modal scrim are fixed Material colour-scheme roles.
- `feature/settings/.../SettingsScreen.kt` palette chips deliberately render all three palette swatches in their own colours so users can choose between them.
- `feature/library/.../SearchOverlay.kt` uses `SpineColors.AccentCool` for tag labels only, matching the established cross-app tag colour and the approved handoff.

### Media/image overlays

- `core/designsystem/.../component/SceneCard.kt:68,121,172,226,244`: pressed-video scrim, play glyph, progress track, and media badges use black/white for stable contrast over arbitrary thumbnails. The actual selected/accent states in the card already use `LocalAccentColors`.
- `core/designsystem/.../component/ResumeCard.kt:106,114,170`: frosted play control and progress overlay use black/white over remote imagery.
- `feature/detail/.../DetailScreen.kt:181,188,209,212`: hero back control and metadata pills use black/white over the scene hero.
- `feature/browse/.../BrowseScreen.kt:358,375`: studio-card bottom scrim and overlaid text use black/white over unknown artwork.
- `feature/player/.../PlayerScreen.kt:194,197,312,338`: player canvas, readability gradient, loading indicator, and playback error are fixed black/white video-layer UI.
- `feature/player/.../PlayerControls.kt:132-144,166,177,362,381,424,430,463,468,490,495,544,565`: controller gradients, icons, disabled alpha, gesture indicators, and text sit directly over video. Active states that should follow the palette already use `LocalAccentColors`.
- `feature/player/.../PlayerTimeline.kt:216,226,310`: timeline tooltip/labels and unplayed marker strokes require fixed contrast over video; played state uses the current accent.
- `feature/player/.../MarkerEditorSheet.kt:148`: translucent white separator is a neutral overlay, not selection state.

## Result

No feature or navigation consumer now uses `SpineColors.AccentPrimary` / `AccentOnPrimary` directly. Remaining references are the token definitions and the dark-scheme fallback seed described above.
