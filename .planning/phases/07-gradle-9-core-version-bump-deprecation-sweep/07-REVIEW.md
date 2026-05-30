---
phase: 07-gradle-9-core-version-bump-deprecation-sweep
reviewed: 2026-05-30T00:00:00Z
depth: standard
files_reviewed: 1
files_reviewed_list:
  - docs/adr/0001-gradle-9-readiness-and-agp8-fold-forward.md
findings:
  critical: 0
  warning: 1
  info: 3
  total: 4
status: issues_found
---

# Phase 7: Code Review Report

**Reviewed:** 2026-05-30
**Depth:** standard
**Files Reviewed:** 1
**Status:** issues_found

## Summary

Phase 7 is a documentation/audit-only phase. The sole non-planning artifact is
`docs/adr/0001-gradle-9-readiness-and-agp8-fold-forward.md`, an Architecture Decision Record
recording the re-scoped Gradle-9 readiness work and the AGP-8.7.3 fold-forward into Phase 8.

Findings were cross-checked against the live repository: `gradle/libs.versions.toml`,
`gradle/wrapper/gradle-wrapper.properties`, `settings.gradle.kts`, `build.gradle.kts`, and the
Phase 7 planning artifacts (`07-RESEARCH.md`, `07.1-SUMMARY.md`, `07.2-SUMMARY.md`,
`gradle9-deprecations.log`).

**Security lens (build-toolchain integrity): PASS.** The ADR handles distribution-checksum
integrity correctly. It does NOT hardcode a fabricated sha256 — it uses a
`<FETCH AT PHASE-8 EXECUTION>` placeholder, explicitly mandates fetching the hash from the
official `.sha256` URL ("never hard-coded from memory"), and preserves
`validateDistributionUrl=true` and `networkTimeout=10000`. No instruction to weaken or disable
checksum validation exists. No Critical findings.

The substantive version facts I could verify against the repo are accurate: Gradle 9.4.1 target,
AGP 8.7.3 catalog value, Kotlin 2.2.20, KSP 2.2.20-2.0.4, KGP floor 2.2.10 (2.2.20 > 2.2.10 holds),
detekt 1.23.8, ktlint 14.2.0, OWASP dependency-check 12.2.2, baseline-profile 1.4.1, and
`failBuildOnCVSS = 7.0`. The Hilt 2.59.2 figure (ADR line 121) is the Phase-8 *target* change-set
value and is internally consistent with the catalog's current 2.56.2 plus the research's recorded
Phase-8 bump — not a defect.

The findings below are documentation-accuracy issues (imprecise cross-references and an
unexplained number inconsistency). None block the phase gate.

## Warnings

### WR-01: `build.gradle.kts:19` cross-reference is imprecise and overstates the "repo contract"

**File:** `docs/adr/0001-gradle-9-readiness-and-agp8-fold-forward.md:79`
**Issue:** The plugin-verdict table states the `--no-configuration-cache` requirement is
"already the repo contract at `build.gradle.kts:19`". Line 19 of `build.gradle.kts` is a comment
(`// Local: \`./gradlew dependencyCheckAnalyze --no-configuration-cache\``), not an enforced
contract. The build file only *documents* `--no-configuration-cache` as a manual local/CI
invocation; there is no programmatic suppression or task wiring that enforces it. Calling a
comment a "contract at line 19" can mislead a Phase-8 implementer into assuming an enforcement
mechanism exists when it does not — meaning the OWASP-on-Gradle-9 config-cache incompatibility
must still be handled by hand at invocation time. The underlying fact (the plugin is
config-cache-incompatible and config cache is ON) is correct; only the citation and the word
"contract" overstate it.
**Fix:** Soften the reference, e.g.:
`...already documented as the required local/CI invocation in \`build.gradle.kts\` (comment at
lines 18-20); the flag must still be passed explicitly — there is no enforced suppression in the
build.` Also confirm CI actually passes `--no-configuration-cache` for the dependencyCheck task,
since the ADR's "config cache is ON" claim depends on that being true on Gradle 9.

## Info

### IN-01: Module-count figures are inconsistent and unexplained (~19 vs ~14)

**File:** `docs/adr/0001-gradle-9-readiness-and-agp8-fold-forward.md:40, 123`
**Issue:** Line 40 says deprecations were swept "across all ~19 modules"; line 123 says "The
convention plugins fail all ~14 modules". `settings.gradle.kts` declares 16 distinct modules (17
`include` lines, but `:baselineprofile` is duplicated — see IN-03). Both ADR numbers are
approximations and ~14 plausibly refers to the subset that applies the convention plugins, but
the two different figures appear without explanation and neither matches the 16 actually declared.
**Fix:** Reconcile to the real count and explain the delta, e.g.: "all 16 declared modules
(~14 of which apply the AGP/Kotlin convention plugins)." Pick one source of truth.

### IN-02: "config cache is ON" is asserted without a cited source in the build config

**File:** `docs/adr/0001-gradle-9-readiness-and-agp8-fold-forward.md:79`
**Issue:** The verdict table asserts "config cache is ON" as the reason the OWASP plugin needs
`--no-configuration-cache`. No `org.gradle.configuration-cache=true` reference (e.g.
`gradle.properties`) is cited, unlike the precise (if imprecise) `build.gradle.kts:19` citation
elsewhere. An unsourced premise weakens the verdict's traceability.
**Fix:** Cite where config cache is enabled (e.g. `gradle.properties:<line>`), or rephrase to
"config cache is enabled repo-wide" with a reference.

### IN-03: ADR propagates a known repo defect without flagging it (duplicate `:baselineprofile`)

**File:** `docs/adr/0001-gradle-9-readiness-and-agp8-fold-forward.md:40, 123`
**Issue:** The module-count claims are derived from `settings.gradle.kts`, which contains a
duplicate `include(":baselineprofile")` (declared twice). The duplicate is a real, pre-existing
repo defect that is out of scope for this phase's diff, but since this ADR is the authoritative
Phase-8 hand-off and bases its module accounting on that file, it would be worth a one-line note
so Phase 8 does not inherit a miscount or the duplicate silently.
**Fix:** Add a parenthetical to the module count (e.g. "16 distinct modules; note
`settings.gradle.kts` duplicates `:baselineprofile` — to be de-duplicated in Phase 8") so the
hand-off carries the caveat. (The fix to `settings.gradle.kts` itself is out of this review's
scope.)

---

_Reviewed: 2026-05-30_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
