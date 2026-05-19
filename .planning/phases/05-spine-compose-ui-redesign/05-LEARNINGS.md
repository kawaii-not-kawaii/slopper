---
phase: 5
phase_name: "SPINE — Compose UI Redesign"
project: "slopper"
generated: "2026-05-19"
counts:
  decisions: 6
  lessons: 6
  patterns: 5
  surprises: 5
missing_artifacts: []
---

# Phase 5 Learnings: SPINE — Compose UI Redesign

## Decisions

### Hard Token Delete → Compile Failures Surface All Regressions
Deleting `StashColors` and `LocalStashColors` entirely (rather than aliasing) caused every missed call site to fail at compile time. The Step 0 grep inventory + compile gate caught 112 hits across 11 files — all surfaced as errors, none silently regressed.

**Rationale:** Aliasing old names to new names would have hidden missing migrations. Hard delete forces correctness.
**Source:** 05.1-SUMMARY.md; 05-CONTEXT.md D-07

---

### SpineResumeCard Gated Behind `if (false)` Until UiState Has Resume Field
The `SpineResumeCard` component was built and wired but is hidden behind `if (false)` in `HomeScreen.kt` because `HomeUiState` has no `resumeScene` field. Adding the data field would require a new GraphQL call or a dedicated "recently played with position > 0" filter — out of scope for v1.

**Rationale:** Build the UI component now; wire the data in a future sprint when the backend can supply it. The `if (false)` pattern keeps the code present and findable without shipping broken UI.
**Source:** 05.2-SUMMARY.md; VERIFICATION.md AR-05-03

---

### `Font` Class Import Collision — Alias the Google Fonts One
`androidx.compose.ui.text.font.Font` and `androidx.compose.ui.text.googlefonts.Font` have the same class name. Importing both causes an "ambiguous reference" compile error. Use: `import androidx.compose.ui.text.googlefonts.Font as GoogleFontLoader`.

**Rationale:** The alias is cleaner than the fully-qualified name at every call site. Confirmed by Plan 5.1 Task 2 deviation.
**Source:** 05.1-SUMMARY.md DEV (compile error fix)

---

### `RenderEffect.createBlurEffect` Requires Explicit `Shader.TileMode` Import
`android.graphics.Shader.TileMode` is needed alongside `android.graphics.RenderEffect`. The Compose `BlurEffect` DSL is not available without the full Android-side imports for API 31+ conditional blur.

**Rationale:** Necessary for the pill nav blur on API 31+. The `if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)` guard is correct; imports must match.
**Source:** 05.1-SUMMARY.md DEV (compile error fix)

---

### Segments Plan: Design System First, Screens Second (Wave 1 → Wave 2)
Running all screen work before tokens exist would cause 100+ compile failures. Wave 1 establishes the entire token namespace before any screen file touches it. This ordering is non-negotiable for any design-token migration.

**Rationale:** Plans 5.2 and 5.3 imported `SpineColors.*` from day 1 — they compile only after Plan 5.1 lands.
**Source:** 05-CONTEXT.md D-01; 05.1-SUMMARY.md

---

### FilterSheet `hasSavedDefault`/`onSaveAsDefault` Parameters Are Pre-Existing, Not "Save View"
The SPEC required "no Save view button" in FilterSheet. The filter sheet already had `onSaveAsDefault` params for saving the default filter (not a named "saved view"). These are different features — the params were kept, the UI button stayed hidden per existing logic.

**Rationale:** Distinguishing between "save as default filter" (existing) and "save named view" (deferred Room table) prevented incorrect code deletion.
**Source:** VERIFICATION.md advisory note; 05-SPEC.md SPINE-06

---

## Lessons

### SceneCard Shape Was ShapeMedium (10dp), Not ShapeSmall (6dp) — Spec Deviation
The verifier caught that SceneCard used `RoundedCornerShape(10.dp)` (ShapeMedium) instead of the design spec's `ShapeSmall (6dp)`. Corrected in a follow-up commit after VERIFICATION.md.

**Context:** This was a pre-existing shape choice that was carried forward into the Spine update without being re-examined. The SPEC acceptance criterion checked for `ShapeSmall` but the code used a different shape that had been there since Phase 1.
**Source:** VERIFICATION.md; fix commit `2f935ee`

---

### TimelineBar Is Called Inside PlayerControls.kt — Not Directly From PlayerScreen
The CONTEXT.md D-05 decision said ChapterStrip goes "above the TimelineBar call in PlayerScreen.kt" — but TimelineBar is actually inside PlayerControls, not directly in PlayerScreen. ChapterStrip ended up as a sibling of the `AnimatedVisibility { PlayerControls(...) }` block in PlayerScreen, which is the correct placement.

**Context:** The RESEARCH.md A5 section correctly identified this, but the CONTEXT.md decision description was slightly misleading. The outcome is correct; the documentation was imprecise.
**Source:** 05.3-SUMMARY.md; 05-RESEARCH.md A5

---

### Google Fonts `font_certs.xml` Must Be Manually Committed — Not Auto-Generated
The `ui-text-google-fonts` library does NOT auto-generate `font_certs.xml`. The developer must add it manually to `res/values/`. The library will fail at runtime (not compile time) if the cert arrays are missing or malformed.

**Context:** This is documented in Google's Compose Google Fonts guide but easy to miss. Confirmed in Plan 5.1 Task 2.
**Source:** 05.1-SUMMARY.md; 05-CONTEXT.md D-03

---

### `Modifier.drawBehind` for Single-Edge Borders — Never `.border()` for Partial Edges
`Modifier.border(1.dp, color, shape)` always draws all 4 edges. For a left-edge-only border (PlayerSettingsPanel), use `Modifier.drawBehind { drawLine(color, Offset(0f,0f), Offset(0f,size.height), strokeWidth) }`.

**Context:** The GLM code review caught this — the plan's code example had `.border()` with a comment saying "use drawBehind for single edge" but the code contradicted the comment. Fixed before execution.
**Source:** 05-REVIEWS.md CRITICAL-2; Plan 5.3 fix

---

### Settings/Connection/Player Were Already Spine-Styled Before Plan 5.3 Ran
Plan 5.1 migrated all `StashColors` tokens globally (112 hits, 11 files). By the time Plan 5.3 ran, the Spine token migration was already complete for Settings, Connection, and Player. Plan 5.3 only needed to add structural Spine layout changes (section groups, input field styling, scrims, etc.), not token replacements.

**Context:** The pre-migration of all call sites in Plan 5.1 (rather than deferring per-screen token work to each screen's plan) significantly reduced the scope of Plans 5.2 and 5.3.
**Source:** 05.3-SUMMARY.md

---

### `collectAsStateWithLifecycle()` Double-Call Bug in Code Examples
A code example in Plan 5.3 showed `viewModel.position.collectAsStateWithLifecycle().value.positionMs` twice in the ChapterStrip call — double-subscribing to the same StateFlow. The existing `val position by viewModel.position.collectAsStateWithLifecycle()` variable should be used directly. The GLM review caught this before execution.

**Context:** Plan code examples that show "how to call a composable" sometimes use the ViewModel directly rather than the already-collected state variable. Always prefer the existing collected variable in composable functions.
**Source:** 05-REVIEWS.md CRITICAL-1; Plan 5.3 fix

---

## Patterns

### Pre-Migration Token Inventory Pattern
Before renaming/deleting design tokens:
```bash
grep -rn 'OldColors\.\|LocalOldColors' feature/ core/ app/ --include="*.kt" | grep -v '.planning' | sort
```
Build the complete migration map from this grep. Update ALL call sites before touching the source token definition. Run compile check after each batch.

**When to use:** Any design token migration (colors, typography, shapes) where old names are deleted.
**Source:** 05.1-PLAN.md Task 1 Step 0; 05-CONTEXT.md D-07

---

### Google Fonts Setup in Convention Plugin (4-Step Checklist)
1. Add `androidx-compose-ui-text-google-fonts` to catalog (BOM-managed, no version pin)
2. Add `implementation(libs.androidx.compose.ui.text.google.fonts)` to the target module
3. Create `res/values/font_certs.xml` with GMS cert arrays (must commit manually)
4. In `Type.kt`: declare `GoogleFont.Provider` + `FontFamily` using `GoogleFont("Name")` — alias the `Font` class from googlefonts package to avoid name collision

**When to use:** Any project adding Google Fonts to a Compose Android app.
**Source:** 05.1-PLAN.md Task 2; 05-LEARNINGS.md

---

### Single-Edge Border Pattern in Compose
```kotlin
Modifier.drawBehind {
    drawLine(
        color = SpineColors.BorderStrong,
        start = Offset(0f, 0f),
        end = Offset(0f, size.height),
        strokeWidth = 1.dp.toPx(),
    )
}
```
Use `drawBehind { drawLine(...) }` for any single-edge border. `Modifier.border()` always draws all 4 edges — never use it for partial borders.

**When to use:** Side panels, bottom bars, section dividers that need one edge highlighted.
**Source:** 05.3-PLAN.md; 05-REVIEWS.md CRITICAL-2

---

### Conditional Feature Slot Pattern (`if (false)`)
When a UI component is built but its data source isn't ready:
```kotlin
if (false) {  // TODO: wire to UiState.resumeScene once field added
    item(key = "resume_card") {
        SpineResumeCard(...)
    }
}
```
The component code is present, imports are resolved, and the slot is visible to future developers — but nothing renders. Avoids shipping an empty/broken card.

**When to use:** UI components waiting for ViewModel/data changes that are out of scope for the current phase.
**Source:** 05.2-SUMMARY.md HomeScreen; VERIFICATION.md AR-05-03

---

### ChapterStrip Placement: Sibling of Controls Block, Not Inside It
When a new overlay composable needs to appear "above" an existing overlay composable, place it as a sibling in the parent `Column`/`Box` — not nested inside the existing composable. The chapter strip sits:
```kotlin
Column(Modifier.align(Alignment.BottomCenter)) {
    ChapterStrip(...)          // new — sibling
    AnimatedVisibility(...) {
        PlayerControls(...)    // existing — unchanged
    }
}
```

**When to use:** Adding new overlay layers to the player bottom scrim or any existing layered composable structure.
**Source:** 05.3-SUMMARY.md; 05-RESEARCH.md A5

---

## Surprises

### Settings/Connection/Player Were Already Fully Migrated After Plan 5.1
Expected: Plans 5.2 and 5.3 each do ~50% token migration work. Actual: Plan 5.1's global token migration (112 hits, 11 files) did ~90% of the work. Plans 5.2 and 5.3 only needed structural layout changes.

**Impact:** Wave 2 ran faster than expected. The "11 files × 112 hits" scope of Plan 5.1 felt large but it was the right approach — one sweep is cleaner than N separate sweeps.
**Source:** 05.3-SUMMARY.md

---

### SceneCard Already Had ShapeMedium Before Spine — Carried Forward
The original SceneCard predated the Spine spec and used `RoundedCornerShape(10.dp)` (ShapeMedium). The Spine update preserved this instead of changing to `ShapeSmall(6dp)`. The verifier caught this after execution.

**Impact:** A one-line fix post-verification. The automatic verification gate (checking for `ShapeSmall` in accept criteria) confirmed the deviation cleanly.
**Source:** VERIFICATION.md advisory

---

### `LocalStashColors` CompositionLocal Had 6 Call Sites Across Different Feature Modules
Expected: 2-3 call sites. Actual: 6 files used `LocalStashColors.current.X`. This means the color system was leaking into composable implementations directly (not just via MaterialTheme) across player, detail, browse, settings, and SceneCard.

**Impact:** All 6 cleaned up in Plan 5.1. Future design system changes should ensure tokens flow through MaterialTheme rather than a custom CompositionLocal.
**Source:** 05-RESEARCH.md A1; 05.1-SUMMARY.md

---

### Google Fonts Are NOT Available Offline — But Build Succeeds
The `font_certs.xml` and `GoogleFont.Provider` build correctly without network. Fonts are loaded at runtime by GMS. If the device has no GMS or no network on first launch, `FontFamily.Default` is used silently.

**Impact:** This is by design (D-03) and acceptable for v1. No test failure, no build failure — the degradation is invisible to the build system.
**Source:** 05-CONTEXT.md D-03; 05-SPEC.md Constraints

---

### ChapterStrip Double-Collect Bug In Plan Code Example
The plan showed `viewModel.position.collectAsStateWithLifecycle().value.positionMs` in a code snippet, despite a note immediately after saying to use the existing `position` variable. The GLM cross-AI review caught this before execution and it was fixed in the plan file.

**Impact:** Zero runtime impact (fixed before execution). Illustrates that code examples in plan files should always use the correct collected-state pattern, not raw ViewModel references.
**Source:** 05-REVIEWS.md CRITICAL-1
