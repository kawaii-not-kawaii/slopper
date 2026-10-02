# Slopper — Project Status

**Repo source of truth for phase/milestone detail:** `.planning/STATE.md` (v1.1 complete — AGP-9 toolchain).
This file tracks the current working session state on top of that.

## Current work: filter date/time pickers (2026-10-01)

- **Ask:** every typed date/time field in the filters gets a picker so nothing has to be typed.
- **Found:** exactly one typed surface exists — Stash criteria editor (`feature/library/.../StashCriteriaSection.kt`):
  `SceneFilterInput.Date` ("YYYY-MM-DD") and `SceneFilterInput.Timestamp` ("YYYY-MM-DD HH:MM"), incl. the
  second "To" field for Between/NotBetween. Everything else (release-date rail in FilterSheet, DetailScreen)
  is bucket-based or read-only.
- **Change:** `CriterionField` wraps each value field with a trailing calendar icon → M3 `DatePickerDialog`
  (Timestamps add a `TimePicker` step, 24h) writing `YYYY-MM-DD[ HH:MM]` — same format `SceneFilterMapper`
  sends to GraphQL. Typing still works alongside the picker.
- **Also fixed (pre-existing, committed master detekt failures blocking the module gate):**
  unused `accent` in FilterSheet.kt; `String.format` locale + `singleOrNull` in SearchOverlay.kt.

## Environment notes

- Android SDK was missing on this box; installed at `~/Android/Sdk` (cmdline-tools 12.0, platform android-36,
  build-tools 36.0.0). Run builds with `ANDROID_HOME=$HOME/Android/Sdk` (no `local.properties` committed).
- No device/emulator attached → picker verified by compile + detekt/ktlint + JVM tests, not visually.
