---
status: partial
phase: 06-settings-redesign
source: [06-VERIFICATION.md]
started: 2026-05-19T14:00:00+09:00
updated: 2026-05-19T14:00:00+09:00
---

## Current Test

[awaiting human testing]

## Tests

### 1. Server status card with live data
expected: ConnectedInfoPanel renders with real server statistics when connected; ConnectedStubPanel shown when not connected
result: [pending]

### 2. Accent palette switching propagation
expected: Selecting a palette swatch in Display settings immediately recolors accent elements across the whole UI; survives app restart (DataStore persists)
result: [pending]

### 3. Search overlay animation and navigation
expected: Tapping the hub search field reveals animated overlay; typing filters results; tapping a result navigates to the correct detail page
result: [pending]

### 4. CSlider persistence
expected: Moving a CSlider in Playback settings persists the value to DataStore; restoring the app shows the saved value
result: [pending]

## Summary

total: 4
passed: 0
issues: 0
pending: 4
skipped: 0
blocked: 0

## Gaps
