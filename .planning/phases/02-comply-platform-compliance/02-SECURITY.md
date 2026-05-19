---
phase: 2
slug: comply-platform-compliance
status: verified
threats_open: 0
threats_total: 9
threats_closed: 9
asvs_level: 1
created: 2026-05-18
audited: 2026-05-18
---

# Phase 2 — COMPLY: Security Threat Verification

> Per-phase security contract: threat register, accepted risks, and audit trail.
> register_authored_at_plan_time: true (02.1-PLAN.md + 02.2-PLAN.md both contain formal <threat_model> blocks)

---

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| Activity → System SplashScreen | Activity hands a `KeepOnScreenCondition` lambda to the system; system polls it on each pre-draw frame | `AtomicBoolean` poll result (no sensitive data) |
| Activity → System back dispatcher | App opts into predictive-back via manifest flag; system delivers `BackEventCompat` progress events to the app | `BackEventCompat(touchX, touchY, progress, swipeEdge)` — in-process only |
| PlayerScreen overlay → system insets | Compose reads `WindowInsets` values from the system to inset the control layer | Inset dimension values (int) — read-only, no sensitive data |
| App → system Settings activity | `LanguageRow` fires an explicit `ACTION_APP_LOCALE_SETTINGS` Intent; resolves to system component only | `context.packageName` (constant, not user-supplied) |
| Manifest → install-time permission grant | Removing `FOREGROUND_SERVICE_MEDIA_PLAYBACK` + `POST_NOTIFICATIONS` shrinks the granted-permission set | No data crossing — permission surface reduction |
| AGP → merged manifest | `generateLocaleConfig` auto-writes `android:localeConfig` in merged manifest at build time | Build-time resource scan only; no runtime data |

---

## Threat Register

| Threat ID | Category | Component | Disposition | Mitigation | Status |
|-----------|----------|-----------|-------------|------------|--------|
| T-02-01 | Denial of Service | MainActivity splash gate (`setKeepOnScreenCondition`) | mitigate | `LaunchedEffect(start)` keys on collected `String?` value (not raw StateFlow); `LaunchedEffect(Unit) { delay(3000); appReady.set(true) }` 3s ANR safety-timeout per RESEARCH §E3. Both verified in `MainActivity.kt:182-191`. | closed |
| T-02-02 | Tampering | `AndroidManifest.xml` `enableOnBackInvokedCallback="true"` | accept | Documented system opt-in contract; no security implication. Enables system predictive-back animation; does not widen any trust boundary. | closed |
| T-02-03 | Information Disclosure | `PredictiveBackHandler` gesture data (`BackEventCompat`) | accept | `touchX/Y`, `progress`, `swipeEdge` values stay in-process; never persisted or transmitted. Phase 2 ignores them entirely (no consumption); future Spine phase may animate progress in-composable (still in-process). | closed |
| T-02-04 | Tampering | `themes.xml` stripped `statusBarColor` / `navigationBarColor` | accept | Removing these attributes is the documented path to allow `enableEdgeToEdge()` to function correctly (PITFALLS §7). Not a security boundary; system owns bar rendering after removal. | closed |
| T-02-05 | Information Disclosure | `POST_NOTIFICATIONS` permission removed | mitigate | Permission removed from `AndroidManifest.xml` (commit `1aff210`). Net-positive: shrinks app attack surface. Pre-removal grep confirmed zero `NotificationManager`/`NotificationCompat`/`.notify(` call sites in `feature/`, `app/`, `core/` — permission was truly orphaned. Verified: `grep -c POST_NOTIFICATIONS AndroidManifest.xml` → 0. | closed |
| T-02-06 | Information Disclosure | `FOREGROUND_SERVICE_MEDIA_PLAYBACK` permission removed | mitigate | Permission removed from `AndroidManifest.xml` (commit `481c303`). No `MediaSessionService` exists in repo. BG-MEDIA milestone owns re-introduction with proper service declaration. Verified: `grep -c FOREGROUND_SERVICE_MEDIA_PLAYBACK AndroidManifest.xml` → 0. Base `FOREGROUND_SERVICE` preserved. | closed |
| T-02-07 | Information Disclosure | `LanguageRow` Intent target | accept | `Settings.ACTION_APP_LOCALE_SETTINGS` resolves only to the AOSP system Settings activity (system component, not exported by any third-party app — cannot be intercepted via implicit Intent hijacking). `Uri.fromParts` uses only `context.packageName` (compile-time constant, not user input). | closed |
| T-02-08 | Tampering | `generateLocaleConfig` build flag | accept | Build-time scan of `values-*/` directories by AGP; no runtime effect. AGP's manifest merger writes the `android:localeConfig` reference in merged manifest — build artifact, not injectable at runtime. No security boundary exists. | closed |
| T-02-09 | Denial of Service | `LanguageRow` click handler (`startActivity`) | accept | Synchronous `startActivity` call with no resource exhaustion path. If system Settings activity absent (impossible on real device), `ActivityNotFoundException` surfaces — cosmetic, non-exploitable, recoverable by user. No loop, no allocation, no retry mechanism. | closed |

*Status: open · closed*
*Disposition: mitigate (implementation required) · accept (documented risk) · transfer (third-party)*

---

## Accepted Risks Log

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|-------------|------|
| AR-02-01 | T-02-02 | System opt-in manifest flag; no security implication beyond standard Android back gesture contract | Plan author (02.1-PLAN.md threat model) | 2026-05-17 |
| AR-02-02 | T-02-03 | Gesture coordinates in-process only; never persisted or transmitted; Phase 2 ignores them | Plan author (02.1-PLAN.md threat model) | 2026-05-17 |
| AR-02-03 | T-02-04 | Documented AOSP edge-to-edge migration path (PITFALLS §7); system owns bar rendering post-removal | Plan author (02.1-PLAN.md threat model) | 2026-05-17 |
| AR-02-04 | T-02-07 | System component target; implicit Intent hijacking impossible; no user input in URI | Plan author (02.2-PLAN.md threat model) | 2026-05-17 |
| AR-02-05 | T-02-08 | Build-time AGP operation; no runtime injection surface | Plan author (02.2-PLAN.md threat model) | 2026-05-17 |
| AR-02-06 | T-02-09 | Synchronous single-call; no resource exhaustion; failure mode is cosmetic ActivityNotFoundException | Plan author (02.2-PLAN.md threat model) | 2026-05-17 |

---

## Mitigation Evidence

### T-02-01 — Splash gate ANR safety-timeout (verified)

```
MainActivity.kt:182  LaunchedEffect(start) {
MainActivity.kt:183    if (start != null) appReady.set(true)
MainActivity.kt:189  LaunchedEffect(Unit) {
MainActivity.kt:190    delay(3000)
MainActivity.kt:191    appReady.set(true)
```

- `LaunchedEffect` keyed on collected `String?` value (`start`) not raw `StateFlow` → strong-skipping cannot stall the gate.
- 3-second ANR insurance timeout ensures splash is always released even if ViewModel stalls.

### T-02-05 — POST_NOTIFICATIONS removed (verified)

```
grep -c 'android.permission.POST_NOTIFICATIONS' app/src/main/AndroidManifest.xml → 0
grep -rn 'NotificationManager\|NotificationCompat\|\.notify(' feature/ app/ core/ → 0 results
```

### T-02-06 — FOREGROUND_SERVICE_MEDIA_PLAYBACK removed (verified)

```
grep -c 'FOREGROUND_SERVICE_MEDIA_PLAYBACK' app/src/main/AndroidManifest.xml → 0
grep -c '"android.permission.FOREGROUND_SERVICE"' app/src/main/AndroidManifest.xml → 1  (base preserved)
```

---

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-05-18 | 9 | 9 | 0 | Claude (gsd-secure-phase, register_authored_at_plan_time: true) |

---

## Sign-Off

- [x] All threats have a disposition (mitigate / accept / transfer)
- [x] Accepted risks documented in Accepted Risks Log (6 accepted, 3 mitigated)
- [x] `threats_open: 0` confirmed
- [x] `status: verified` set in frontmatter
