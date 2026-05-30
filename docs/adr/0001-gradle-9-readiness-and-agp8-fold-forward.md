# ADR 0001 — Gradle-9 Readiness and the AGP-8.7.3 Fold-Forward

> This is the first Architecture Decision Record in the Slopper repository; it establishes
> the `docs/adr/` convention. Records use the standard **Status / Context / Decision /
> Consequences** structure.

## Status

**Accepted** — 2026-05-30 (Phase 7, milestone v1.1 "AGP-9 Toolchain Modernization").

This ADR records the **partial** delivery of requirement **AGP9-01**: the Gradle 9.4.1
target is pinned, the Gradle-9 deprecation surface is swept (Plan 07.1, see
`gradle9-deprecations.log`), every toolchain plugin has a recorded Gradle-9 verdict, the
KGP floor is confirmed, and the live wrapper **activation is handed off to Phase 8**. No
live build config is modified by this decision — the wrapper remains on Gradle 8.11.1.

## Context — the AGP-8.7.3 × Gradle-9 incompatibility

The optimistic "flip the wrapper to Gradle 9 as one isolating commit on the current AGP"
framing is **disproven** by the Phase 7 research (HIGH confidence, multiple official sources):

- AGP **8.7**'s official compatibility table lists Gradle **8.9 as both minimum and default**
  and lists **no Gradle-9 tier** at all.
- The Gradle compatibility matrix tests Gradle 9 **only against AGP 9.0–9.2.x**. AGP 8.7 is
  outside the tested range — there is no version overlap.
- AGP enforces a **hard Gradle-version check at configuration start**. On Gradle 9.4.1 with
  AGP 8.7.3 the build fails immediately (`Minimum supported Gradle version is …`) and no
  module configures.

**Conclusion:** a green build on Gradle 9.4.1 with AGP 8.7.3 is **impossible**. The ROADMAP
escape clause fires — "if AGP 8.7.3 is NOT Gradle-9-compatible, this phase folds forward into
Phase 8." It is not compatible, so the wrapper flip rides into Phase 8's atomic AGP-9
change-set as its first step. Phase 7 therefore delivers everything that can be done and
verified **independently of the AGP-9 DSL surgery**: target pinning, deprecation enumeration,
per-plugin Gradle-9 verdicts, the detekt decision, and the KGP-floor confirmation.

Empirical input from Plan 07.1 (`gradle9-deprecations.log`): running
`./gradlew help --warning-mode=all` and `./gradlew detekt ktlintCheck --warning-mode=all
--dry-run` on the **current Gradle 8.11.1 + AGP 8.7.3** toolchain emitted **ZERO** Gradle-9
deprecations across all ~19 modules; the repo's own build scripts use no Gradle-9-removed
APIs (only `layout.buildDirectory`); and the v1.0 CI gate
(`compileDebugSources detekt ktlintCheck test`) stayed green. The detekt tasks were
**SKIPPED** in the dry-run, so the detekt-on-Gradle-9 surface is empirically **unknown** until
AGP 9 lands and the build first actually configures on Gradle 9.

## Decision

### 1. Pinned Gradle target (recorded, NOT applied to the live wrapper this phase)

The Phase-8 wrapper edit is pinned here as:

```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-9.4.1-bin.zip
distributionSha256Sum=<FETCH AT PHASE-8 EXECUTION — see below>
# keep validateDistributionUrl=true, networkTimeout=10000 unchanged
```

- `distributionUrl` → `https://services.gradle.org/distributions/gradle-9.4.1-bin.zip`
  (confirmed live during research, HTTP 307 CDN redirect).
- `distributionSha256Sum` → **fetched at Phase-8 execution time** from
  `https://services.gradle.org/distributions/gradle-9.4.1-bin.zip.sha256` (e.g.
  `curl -sSL https://services.gradle.org/distributions/gradle-9.4.1-bin.zip.sha256`, or
  atomically via `./gradlew wrapper --gradle-version 9.4.1
  --gradle-distribution-sha256-sum <sum>`). The hash is **never hard-coded from memory** — a
  stale value fails `validateDistributionUrl=true` loudly and never ships silently
  (threat T-07.2-01).
- `validateDistributionUrl=true` and `networkTimeout=10000` are kept unchanged.
- **9.4.1**, not 9.5.x, is the target: it is the exact AGP-9.2 Gradle floor; going higher
  violates the minimal-surface + stable-only policy with no benefit.

The current live wrapper continues to read `gradle-8.11.1-bin.zip` — this ADR records the
target, it does **not** flip it.

### 2. Plugin Gradle-9 compatibility verdicts

| Plugin | Plugin id | Catalog version | Gradle-9 verdict | Action |
|--------|-----------|-----------------|------------------|--------|
| **ktlint** | `org.jlleitschuh.gradle.ktlint` | `14.2.0` | **READY ✓** — Gradle-9 support landed in 13.1.0; 14.x is current and Isolated-Projects-ready | No action. |
| **OWASP dependency-check** | `org.owasp.dependencycheck` | `12.2.2` | **RUNS on Gradle 9 only via `--no-configuration-cache`** — already the repo contract at `build.gradle.kts:19`; the plugin is config-cache-incompatible and config cache is ON | Keep the existing `--no-configuration-cache` contract; no bump. Preserve `failBuildOnCVSS=7.0` (threat T-07.2-02). |
| **baseline-profile** | `androidx.baselineprofile` | `1.4.1` | **VERIFIED for Phase 7** — not exercised in the frozen scope; its AGP-9 variant-API soft spot is a Phase-8 concern, not a Gradle-9 one | No action this phase; re-verify when AGP 9 lands in Phase 8. |
| **detekt** | `io.gitlab.arturbosch.detekt` | `1.23.8` | **THE OPEN DECISION** — officially Gradle-8.12.1-targeted; the only Gradle-9 line is 2.0.0-alpha.1 (forbidden by the stable-only policy) | See Decision §3. |

### 3. The detekt-on-Gradle-9 decision

detekt 1.23.8 is the latest **stable** detekt but is officially pinned to Gradle 8.12.1 /
Kotlin 2.0.21. The only detekt line that officially supports Gradle 9 is **2.0.0-alpha.x**,
which the **stable-only dependency policy forbids**. detekt's Gradle plugin applies Kotlin/AGP
as `compileOnly`, so it typically still *runs* on a newer Gradle with deprecation warnings —
but whether 1.23.8 merely warns or hard-fails on Gradle 9 is **empirically untestable in
Phase 7** because nothing configures on Gradle 9 until AGP 9 lands. Plan 07.1's enumeration
confirms this: the detekt tasks were SKIPPED in the dry-run and emitted zero deprecations on
Gradle 8.11.1 (`gradle9-deprecations.log`, "Gradle-9 Deprecation Attribution" section).

**Decision (recorded verbatim):**

> Keep detekt 1.23.8 (stable). Accept its anticipated Gradle-9 deprecation warnings as a
> tracked item. Defer the empirical run-vs-fail determination to Phase 8's first
> green-on-Gradle-9 build (it is UNTESTABLE in Phase 7 because nothing configures on Gradle 9
> until AGP 9 lands). Do NOT adopt detekt 2.0.0-alpha.x. Revisit a narrow stable-only-policy
> exception ONLY if detekt 1.23.8 hard-fails in Phase 8.

This upholds the stable-only policy and refuses to pull an unvetted pre-release plugin in to
"fix" a warning that has not yet been observed (threat T-07.2-03, disposition: accept).

### 4. KGP floor confirmation

AGP 9 requires only **KGP ≥ 2.2.10**. The catalog already pins:

- `kotlin = "2.2.20"` — 2.2.20 > 2.2.10 → **floor satisfied**.
- `ksp = "2.2.20-2.0.4"` — locked exactly to the Kotlin version.

Therefore **no Kotlin/KSP bump is needed**; Phase 8 carries no Kotlin/KSP change. Chasing
Kotlin 2.3.x would re-trigger KSP/Hilt churn for no gain and is explicitly out of scope.

## Consequences — Hand-off to Phase 8 (the fold-forward)

The `gradle-wrapper.properties` flip to **9.4.1** plus the **live sha256 fetch** become
**STEP 1 of Phase 8's atomic AGP-9 change-set**:

> Gradle 9.4.1 → AGP 9.2.1 → drop `kotlin.android` → `CommonExtension` fix →
> `compilerOptions` migration → Hilt 2.59.2 → compileSdk 36.

The convention plugins fail all ~14 modules until that change-set lands whole, so the wrapper
flip cannot be a standalone green commit on AGP 8.7.3 — it is green only once AGP is also 9.x.

Consequently the **"build runs green on Gradle 9"** clause of **AGP9-01** is asserted at the
**Phase 8 gate**, not the Phase 7 gate. Phase 7 delivers AGP9-01 **PARTIALLY**:

- ✓ Gradle 9.4.1 target pinned (URL + fetched-at-execution sha256 mandate).
- ✓ Gradle-9 deprecations enumerated and attributed (Plan 07.1 — empty surface on 8.11.1).
- ✓ All four toolchain plugins audited and verdicts recorded.
- ✓ The detekt-on-Gradle-9 decision made (keep stable, defer empirical test, reject alpha).
- ✓ KGP ≥ 2.2.10 floor confirmed (no Kotlin/KSP bump).
- → green-on-Gradle-9 **handed off to Phase 8**.

Phase 7's own gate is the no-regression-on-8.11.1 build plus these recorded
enumeration/decision artifacts.

## Explicitly rejected options

- **Gradle 9.5.x** as the target — rejected by the minimal-surface policy; 9.4.1 is the exact
  AGP-9.2 floor and there is no benefit to going higher.
- **detekt 2.0.0-alpha.x** — rejected by the stable-only policy; adopting a pre-release plugin
  to silence an unobserved Gradle-9 warning is forbidden (unvetted supply-chain provenance).
- **Running AGP 8.7.3 on Gradle 9 for "one isolating commit"** — rejected; disproven, AGP
  hard-fails the Gradle-version check at configuration start (no module configures).
- **`android.enableLegacyVariantApi=true`** and **`android.newDsl=false`** — rejected as
  forbidden AGP-9 crutches; they are no-op/doomed, are AGP-9 flags irrelevant to a
  Gradle-only phase, and the repo is already clean (zero `applicationVariants`/`buildDir`).
  They must not appear in the build at any point.
- **Bumping the CI cache key `agp8`→`agp9`** — rejected this phase; AGP is still 8.7.3, so the
  key stays `agp8` until Phase 8.

## References

- `gradle9-deprecations.log` — Plan 07.1's Gradle-9 deprecation inventory (the
  enumerate-on-the-old-Gradle-first evidence cited for the detekt verdict).
- `07-RESEARCH.md` — version facts, plugin verdicts, the Fold-Forward Decision (Pattern 1),
  and Open Questions.
- `07.1-SUMMARY.md` — Wave 1 findings (zero deprecations on 8.11.1, repo scripts clean,
  CI gate green).
- ROADMAP.md (milestone v1.1) — Phase 8 as the indivisible atomic AGP-9 change-set.
