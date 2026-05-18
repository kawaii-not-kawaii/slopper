# Device testing guide

A checklist for getting the app onto a phone and verifying the end-to-end flow
works against your actual Stash server.

## Prerequisites

- **Android Studio** (Hedgehog / Koala / newer) — installs the SDK for you.
  Or install the cmdline-tools + SDK manually.
- **JDK 17** (21 works too; AGP 8.7 tolerates both).
- **ADB** on your PATH (comes with `platform-tools` in the SDK).
- A phone with **USB debugging** enabled (Developer options → USB debugging).

## First build

```bash
cd android
./bootstrap.sh           # downloads Gradle wrapper into project root
./gradlew :app:assembleDebug
```

Artifacts land in `app/build/outputs/apk/debug/` as per-ABI APKs:
- `app-arm64-v8a-debug.apk`  — most modern devices (Pixel, recent Samsung, etc.)
- `app-armeabi-v7a-debug.apk` — older 32-bit devices

## Install

```bash
# Plug in a phone with USB debugging on and unlock the screen.
adb devices                                  # confirm phone shows up
./gradlew :app:installDebug                  # build + install in one step
adb shell am start -n io.stashapp.android.debug/io.stashapp.android.MainActivity
```

## Smoke-test checklist

Work through these in order — each one depends on the previous step working.

### Connection (5 min)
- [ ] App launches on `ConnectionScreen` on first run
- [ ] Enter Stash URL (e.g. `http://192.168.1.10:9999`) + API key
- [ ] "Test" button shows loading spinner, then server version + stats
- [ ] "Connect" advances to the library grid
- [ ] Kill + relaunch app — opens directly on library (encrypted prefs restore)

### Library + filter (5 min)
- [ ] Grid loads scenes with thumbnails, duration/resolution chips, rating pills
- [ ] Scroll triggers pagination (no stalls, no duplicates)
- [ ] Search icon opens text field; typing triggers re-query
- [ ] Filter icon opens bottom sheet
- [ ] Change sort → grid re-sorts
- [ ] Toggle "Organized" → fewer results
- [ ] Set rating min → results filtered
- [ ] Badge appears on filter icon when any filter is active
- [ ] "Reset" in sheet clears everything

### Scene detail (5 min)
- [ ] Tap a scene → detail page with hero screenshot, metadata pills
- [ ] Studio name renders, date, rating stars
- [ ] Performer avatars scroll horizontally
- [ ] Tag chips wrap cleanly
- [ ] Markers list shows with timestamps
- [ ] Rating stars — tap to set; tap active star to clear
- [ ] Organize button toggles; persists across refresh
- [ ] O-counter ± buttons update counter; persists across refresh

### Player — single scene (10 min)
- [ ] Tap "Play" → fullscreen landscape player
- [ ] Video plays with audio
- [ ] Tap screen toggles controls; auto-hide after 3s
- [ ] Double-tap left = -10s, double-tap right = +10s
- [ ] Horizontal drag scrubs position
- [ ] Play/pause button works
- [ ] Tap PiP button → phone shrinks to PiP window
- [ ] Swipe PiP back into focus → restore
- [ ] Press Back → returns to detail, player released (check `adb shell dumpsys audio` — no phantom streams)
- [ ] Re-open scene — resume position is restored from the last close

### Player — queue (10 min)
- [ ] Long-press a scene card in library → player opens with current page as queue
- [ ] Skip-next advances to next scene in queue
- [ ] Skip-prev goes back
- [ ] Shuffle toggle highlights in amber; next/prev navigates the shuffled order
- [ ] Repeat cycles OFF → ALL → ONE → OFF
- [ ] With repeat-ALL, skip-next past end wraps to first
- [ ] With repeat-ONE, scene finishes → restarts (not auto-advance)

### Marker seek (2 min)
- [ ] On a scene with markers, tap a marker in detail
- [ ] Player opens and jumps to that timestamp (not the resume point)

### Resume sync-back (5 min)
- [ ] Play a scene for ~30s
- [ ] Close player
- [ ] Check Stash web UI for the same scene — `resume_time` should reflect Android position
- [ ] Play a scene from start to near-end (>85% through)
- [ ] Close player
- [ ] Check Stash web UI — `play_count` incremented by 1, `play_history` has new entry

### Browse entities (5 min)
- [ ] Settings → Performers → grid of performer avatars, search works
- [ ] Tap a performer → library opens filtered to their scenes
- [ ] Studios + Tags screens work the same way

### Settings (2 min)
- [ ] Codec status card visible ("Limited codec support" unless FFmpeg AAR dropped in)
- [ ] Disconnect → returns to ConnectionScreen, credentials cleared

## Common failure modes

| Symptom | Likely cause | Fix |
|---|---|---|
| `ApolloException: Cannot query field X on type Y` | Schema drifted from what we generated against | Regenerate: `./gradlew :core:network:generateApolloSources` |
| All images broken, 401 errors in logcat | API key not attached to image loader | Verify `StashImageLoaderFactory` — it checks URL prefix matches endpoint |
| Video shows "No decoder available" | Unsupported audio codec (AC3/EAC3/DTS/TrueHD) | Build FFmpeg extension: `tools/ffmpeg-extension/build.sh` |
| App crashes on launch with `HiltAndroidApp` error | KSP didn't regenerate | `./gradlew clean assembleDebug` |
| Grid stuck on spinner, no errors | Server reachable from browser but not from phone on LAN | Phone on different network than Stash server, or `networkSecurityConfig` blocking HTTPS with self-signed cert |
| Scene title looks right in list but blank in detail | `details` / title edit applied server-side but cache stale | Pull to refresh (not implemented yet — `viewModel.load()` works manually) |

## Logcat filters

Useful filters while developing:

```bash
adb logcat -v color -s 'StashApp:V' 'ExoPlayer:I' 'Apollo:V' 'OkHttp:I'
```

To capture a full session trace:

```bash
adb logcat > ~/stash-session.log
# ... reproduce the issue ...
# Ctrl+C
```

## Phase 2 — Platform Compliance Checks

These checks verify the COMPLY milestone changes from Phase 2. Run on a physical device with Android 13+.

### COMPLY-01 — Edge-to-edge content insets
1. Open the player screen
2. Verify UI content (controls, timeline) is not clipped behind the system navigation bar or status bar
3. In gesture navigation mode, verify the safeDrawingPadding overlay correctly insets all interactive elements

### COMPLY-02 — Predictive back gesture
1. Enable **Developer Options → Predictive back animations** on the device
2. Open the player screen, start playback
3. Swipe back (Android 14+): verify the predictive back preview animation appears before completing the gesture
4. Verify the player exits cleanly when the gesture completes

### COMPLY-04 — Language settings (Android 13+)
1. Open **Settings → App Language**
2. Verify the Language row appears and allows per-app language selection (Android 13+ only; not shown on Android 12)

### COMPLY-06 — Bottom sheet insets
1. Open Library, tap the filter icon to open the filter sheet
2. In gesture navigation mode, verify the sheet's content is not obscured by the home indicator or navigation gestures
3. Open MoreSheet (long-press on a scene card), verify same inset behavior

## Phase 3 — Performance Regression Checks

These checks verify the PERF milestone changes from Phase 3 are still working after the Phase 4 PlayerScreen split.

### PERF-04 / Shuffle fix — Queue completion
1. Add 10+ scenes to a queue
2. Enable shuffle mode
3. Play through all scenes
4. Verify the "End of queue" banner appears when the last scene finishes (regression: it previously stopped silently)

### PERF-09 — Frame rate adaptation
1. Open a high-frame-rate video (60fps or 120fps content)
2. Enable frame rate display in Developer Options
3. Verify the display refresh rate adjusts to match the video frame rate when the player opens
4. Verify the first frame appears without jank (no frozen frame on initial load)
