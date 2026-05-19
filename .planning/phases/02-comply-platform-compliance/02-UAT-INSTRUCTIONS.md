# Phase 2 — UAT Capture Instructions (Task 4 of Plan 02.2)

**Status:** Awaiting human action.
**Branch:** `phase-2/comply-platform-compliance`
**Build SHA:** `9c65e91`
**Build state:** `./gradlew --configuration-cache assembleDebug` green.

This document tells you (the human) exactly what to do to satisfy the Task 4
`checkpoint:human-verify` gate in `.planning/phases/02-comply-platform-compliance/02.2-PLAN.md`.
After you finish the capture and place the 12 PNG files on disk, re-invoke
`/gsd-execute-phase 2` (or hand back with "approved") and the executor will
run Task 5 (write SCREENSHOTS.md + 02-UAT.md + commit the artifacts).

---

## 1. APK to flash

The app's build is configured with ABI splits and **no universal APK**. The S23+
uses arm64 — flash the arm64 split:

```
app/build/outputs/apk/debug/app-arm64-v8a-debug.apk   # 32 MB — flash this on S23+
app/build/outputs/apk/debug/app-armeabi-v7a-debug.apk # 32 MB — 32-bit fallback, ignore
```

Application ID for debug variant: `io.stashapp.android.debug` (note the `.debug` suffix).

### Install via adb

With S23+ connected over USB (Developer options ON + USB debugging ON):
```bash
adb devices            # confirm device shows as "device" (not "unauthorized")
adb install -r /home/yun/slopper/app/build/outputs/apk/debug/app-arm64-v8a-debug.apk
adb shell pm path io.stashapp.android.debug   # returns install path on success
```

If your S23+ already has a previous Slopper debug build installed, `-r` (reinstall)
preserves the install. If the signing cert differs, `adb uninstall io.stashapp.android.debug`
first.

---

## 2. The 12 screenshots (locked names)

CONTEXT.md Decision 8 locks the names. **Do not rename**.

Place each PNG under `.planning/phases/02-comply-platform-compliance/screenshots/`:

| # | Filename | Theme | Screen | What to look for |
|---|----------|-------|--------|------------------|
| 1 | `home-light.png` | Light | Home | Top inset clean (no overlap with status bar text); rails visible |
| 2 | `library-light.png` | Light | Library (browse view) | Scroll position shows insets on top + bottom |
| 3 | `settings-light.png` | Light | Settings | Scrolled to show the NEW "App / Language" row near the top |
| 4 | `player-controls-visible-light.png` | Light | Player (overlay visible) | Controls overlay is wrapped in safeDrawingPadding — no buttons under nav |
| 5 | `player-controls-hidden-light.png` | Light | Player (overlay hidden) | Video full-bleed under system bars (gesture-nav scrim only) |
| 6 | `connection-light.png` | Light | Connection screen | Auth fields visible; no clipped chrome |
| 7 | `home-dark.png` | Dark | Home | Same checks as #1 in dark |
| 8 | `library-dark.png` | Dark | Library | Same checks as #2 in dark |
| 9 | `settings-dark.png` | Dark | Settings | Same checks as #3 in dark |
| 10 | `player-controls-visible-dark.png` | Dark | Player | Same checks as #4 in dark |
| 11 | `player-controls-hidden-dark.png` | Dark | Player | Same checks as #5 in dark |
| 12 | `connection-dark.png` | Dark | Connection | Same checks as #6 in dark |

Per Plan 02.2 Task 4, file size sanity check is each PNG > 50 KB.

---

## 3. Capture procedure

### Light theme batch

1. System Settings → Display → Theme → **Light**.
2. Force-stop Slopper (Settings → Apps → Slopper → Force stop) to test cold start.
3. Launch Slopper from the app drawer. **Observe:** the new splash (COMPLY-04 evidence) — no white flash.
4. Land on Home → capture `home-light.png`.
5. Tap Library → scroll a couple of rows so insets are visible → capture `library-light.png`.
6. Open Settings → scroll until the **App / Language** row is visible at top of screen → capture `settings-light.png`.
7. Tap the Language row → confirm the system per-app language dialog opens (COMPLY-06 evidence). Dialog will show "App default" only (English-only resources today — expected). Dismiss; no screenshot needed.
8. Back to Library → open a scene → enter PlayerScreen.
9. With controls visible: capture `player-controls-visible-light.png`.
10. Tap once on the video to hide controls → wait for fade → capture `player-controls-hidden-light.png`.
11. Test back swipe: a slow drag from the left edge should show an **animated predictive-back preview** (COMPLY-02 evidence). Let go to commit — back to Library.
12. Navigate to Connection screen → capture `connection-light.png`.

### Dark theme batch

13. System Settings → Display → Theme → **Dark**.
14. Repeat steps 4, 5, 6, 9, 10, 12 — capturing the dark variants:
    - `home-dark.png`, `library-dark.png`, `settings-dark.png`,
    - `player-controls-visible-dark.png`, `player-controls-hidden-dark.png`,
    - `connection-dark.png`.

### Capture mechanics (pick whichever you prefer)

Option A (adb screencap — repeatable):
```bash
adb shell screencap -p /sdcard/shot.png && adb pull /sdcard/shot.png home-light.png
mv home-light.png /home/yun/slopper/.planning/phases/02-comply-platform-compliance/screenshots/
```

Option B (S23+ hardware): Side + Volume-Down → screenshots land in DCIM/Screenshots → transfer over USB / Quick Share / KDE Connect to the screenshots/ dir and rename.

---

## 4. Reviewer checklist (you must confirm before signaling "approved")

Verify across the 12 captures:

- [ ] No clipped chrome anywhere — no buttons hiding under the nav bar; no status bar overlapping text.
- [ ] Cold launch shows the splash (the COMPLY-04 splash with `Theme.Stash.Splash`), not a white flash.
- [ ] Language row is visible in Settings → tapping it opens the system locale dialog.
- [ ] PlayerScreen back swipe shows the **animated** predictive preview (chrome scales/dims), not a snap-exit.
- [ ] Three `ModalBottomSheet`s render bottom content clear of the system nav area:
    - Library FilterSheet (open via the filter icon in the Library top bar)
    - NavCustomize sheet
    - BottomNav "More" sheet
    Note: no screenshots required of these sheets in the 12-shot pack; just a visual check.

If ANY check fails, do NOT mark approved — describe the failure (screen + regression) and the executor will open a fix commit before re-capturing.

---

## 5. After capture

Send back to Claude / re-invoke the orchestrator with:

- Confirmation all 12 PNGs are in `.planning/phases/02-comply-platform-compliance/screenshots/`.
- Your name/handle for the **Reviewer** field.
- Capture date/time (Asia/Tokyo or UTC — either, just be explicit).
- The verdict: **PASS** / **PASS-WITH-NOTES** / **FAIL** (delete as appropriate).
- If PASS-WITH-NOTES: which row, what note.

The Task 5 executor will then write:
- `.planning/phases/02-comply-platform-compliance/screenshots/SCREENSHOTS.md` (12-row inventory + your reviewer line)
- `.planning/phases/02-comply-platform-compliance/02-UAT.md` (one row per DEVICE_TESTING.md checklist item, all gesture-nav rows PASS, all 3-button-nav rows DEFERRED → COMPLY-07-3BTN, REVIEWS-C4 ACCEPT block at the bottom, your reviewer sign-off block)
- One atomic commit: `docs(comply): COMPLY-07 — S23+ gesture-nav UAT + 12-PNG screenshot pack (3-button-nav DEFERRED → COMPLY-07-3BTN)`

---

## 6. SCREENSHOTS.md index template (what Task 5 will write)

(For reference only — Task 5 fills this in from the metadata you supply.)

```markdown
# Phase 2 COMPLY — Screenshot Pack

**Captured:** <YOUR-DATETIME>
**Device:** Samsung Galaxy S23+ (SM-S916U1)
**OS:** Android 16
**Navigation:** Gesture nav (3-button-nav DEFERRED — see COMPLY-07-3BTN backlog)
**Build SHA:** 9c65e91
**Reviewer:** <YOUR-NAME-OR-HANDLE>
**Verdict:** PASS | PASS-WITH-NOTES | FAIL

## Inventory

| Screen | Light Theme | Dark Theme | Verifies |
| ... 12-row table ... |
```

## 7. DEVICE_TESTING.md row scaffolds (Task 5 will fill)

Task 5 opens `DEVICE_TESTING.md` (repo root) and creates one row per checklist
item. You don't need to write these — but if you want a preview of the format:

```markdown
| # | Checklist item (from DEVICE_TESTING.md) | Result | Notes | Evidence |
|---|---|---|---|---|
| 1 | Home loads in under 2s | PASS | Cold launch ~1.4s, splash dismisses cleanly | screenshots/home-light.png |
| ... | ... | ... | ... | ... |
| N | 3-button-nav: edge-to-edge correct on bottom bar | DEFERRED — see COMPLY-07-3BTN | Hardware unavailable | n/a |
```

---

**Quick-resume command after you finish capture:**

```
approved — S23+ gesture-nav capture complete, 12 PNGs in place, verdict PASS
```

(Or PASS-WITH-NOTES + the note; or FAIL + the regression description.)
