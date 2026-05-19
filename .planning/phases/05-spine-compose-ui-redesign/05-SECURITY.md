---
phase: 5
slug: spine-compose-ui-redesign
status: verified
threats_open: 0
threats_total: 8
threats_closed: 8
asvs_level: 1
created: 2026-05-19
audited: 2026-05-19
register_authored_at_plan_time: true
---

# Phase 5 — SPINE: Security Threat Verification

## Threat Register

| Threat ID | Category | Component | Disposition | Mitigation | Status |
|-----------|----------|-----------|-------------|------------|--------|
| T-05-01 | Tampering | Token migration misses call sites | mitigate | Step 0 grep inventory; hard-delete migration; assembleDebug gate. Verified: `grep -rn 'StashColors\.' feature/ core/ app/` → 0 hits | closed |
| T-05-02 | Denial of Service | Google Fonts download fails at runtime | accept | Silent fallback to `FontFamily.Default`. Build and tests pass regardless. | closed |
| T-05-03 | Denial of Service | LocalStashColors deletion breaks 6 call sites | mitigate | All 6 files updated before Color.kt rewrite. Compile gate verified. | closed |
| T-05-04 | Tampering | Phase 3 ImmutableList types broken in screen updates | mitigate | `ImmutableList<Marker>` confirmed in 5 locations. assembleDebug green. | closed |
| T-05-05 | Tampering | Phase 2 COMPLY-01 insets broken | mitigate | `contentWindowInsets` confirmed in NavCustomizeSheet; `safeDrawingPadding` confirmed in PlayerScreen. | closed |
| T-05-06 | Tampering | PlayerScreen Phase 3 (PredictiveBackHandler, PERF-09) broken | mitigate | Both grep-confirmed in PlayerScreen.kt post-execution. | closed |
| T-05-07 | Denial of Service | ChapterStrip clips behind gesture layer (safe-area) | mitigate | `navigationBarsPadding()` applied to ChapterStrip call site. | closed |
| T-05-08 | Information Disclosure | Save view re-introduced via FilterSheet changes | mitigate | `grep -c 'Save view\|saveView' feature/library/FilterSheet.kt` → 0. | closed |

## Accepted Risks Log

| Risk ID | Threat Ref | Rationale | Date |
|---------|------------|-----------|------|
| AR-05-01 | T-05-02 | Google Fonts API runtime dependency; silent fallback acceptable for v1 | 2026-05-19 |
| AR-05-02 | N/A | Blur effect only on API 31+ (minSdk 26); cosmetic degradation on older devices | 2026-05-19 |
| AR-05-03 | N/A | SpineResumeCard gated behind `if (false)` pending `HomeUiState.resumeScene` field; v1 ships without it | 2026-05-19 |

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open |
|------------|---------------|--------|------|
| 2026-05-19 | 8 | 8 | 0 |

## Sign-Off

- [x] All 8 threats have dispositions
- [x] `threats_open: 0`
- [x] `status: verified`
