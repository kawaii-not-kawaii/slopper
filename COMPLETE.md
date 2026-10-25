# Slopper — Completed Work Log

One line per finished item so nothing is re-done or re-litigated.

- 2026-10-21 — Built release APK v7 (0.2.0-alpha, versionCode 7, master `1f14727` = #80 + #81 + #82;
  debug-cert v2-signed, R8-minified) → `/home/yun/Slopper-0.2.0-alpha-v7-{arm64,armv7}.apk`;
  Taildropped arm64 to oppo-find-x9-ultra.
- 2026-10-21 — GitHub PR cleanup, 18 open → 2: merged #81 (`ab249d3`); batched 14 Dependabot PRs into
  #82 (`9eead75`, incl. CodeQL init+analyze pair) and closed originals; closed #79 (gradle-wrapper, DEP-GRADLE96);
  rebased #80 onto master and merged it (`1f14727`); closed #68 (contained in #80). Zero open PRs.
- 2026-10-01 — Date/time pickers on all typed filter fields: Stash criteria Date + Timestamp inputs
  (incl. Between "To" field) now fill from an M3 DatePicker (+ TimePicker step for timestamps);
  `feature/library/.../StashCriteriaSection.kt` + `StashCriteriaSectionTest.kt`.
- 2026-10-01 — Built release APK v6 (0.2.0-alpha, arm64 + armv7, debug-cert v2-signed, R8-minified);
  Taildropped Slopper-0.2.0-alpha-v6-arm64.apk to pixel-9-pro-1.
- 2026-10-01 — Fixed 3 pre-existing committed detekt failures in feature:library (FilterSheet unused
  `accent`; SearchOverlay default-locale format + `singleOrNull`).
- 2026-10-01 — Installed local Android SDK `~/Android/Sdk` (platform 36, build-tools 36.0.0).
- 2026-08-01 — v1.1 AGP-9 toolchain milestone complete (full history: `.planning/STATE.md`).
