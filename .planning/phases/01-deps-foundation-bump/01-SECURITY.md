---
phase: 01-deps-foundation-bump
mode: retroactive-STRIDE
asvs_level: 1
threats_total: 13
threats_closed: 7
threats_open: 4
threats_accepted: 2
audit_date: 2026-05-16T22:37+09:00
auditor: Claude (gsd-secure-phase, Opus 4.7-1M)
branch: phase-1/deps-bump
head: bbc5a6f
block_on: high
enforcement: true
verdict: OPEN_THREATS
---

# Phase 1 (DEPS — Foundation Bump): Retroactive STRIDE Security Audit

## Scope and calibration

Phase 1 is a **toolchain / dependency-floor modernization**: catalog bumps, JDK pin, lint/detekt/ktlint refresh, plus precondition orphan-removal commits and one schema-vendoring commit. No new user-input surface, no new auth code, no new network endpoints, no behavioral change. The threat surface is therefore **supply chain, build tooling, credentials, and the vendored upstream GraphQL schema**, and HIGH severity is reserved for build-integrity or credential-disclosure issues that the phase actively introduced or actively missed mitigating.

## STRIDE Threat Register

| Threat ID | STRIDE | Component | Description | Severity | Status | Evidence |
|---|---|---|---|---|---|---|
| T-S-01 | Spoofing | `core/network/.../schema.graphqls` (vendored upstream) | The 4916-line schema was concatenated from `stashapp/stash@develop` with no recorded source-commit SHA or hash. A MITM or compromised GitHub CDN at vendoring time could have substituted attacker-controlled types, which then propagate through Apollo codegen into runtime types consumed across feature modules. | MEDIUM | OPEN | Schema header (line 1) only says `# Vendored from stashapp/stash@develop graphql/schema (root + types/*)` — no upstream commit SHA, no SHA-256, no signature; commit `3783973` does not pin a provenance hash either. |
| T-S-02 | Spoofing | Forgejo API token in `~/.git-credentials` | Plaintext storage of a Forgejo admin-scope token that doubles as the spec-layer skill's tracker auth. Anyone with read on `~/.git-credentials` (any local process, any backup) gets full Forgejo API access. Phase 1 did not introduce the credential but uses it on every spec-layer step. | MEDIUM | OPEN (environmental) | `01-LEARNINGS.md` "Surprises §6 Forgejo basic-auth password = API token" explicitly documents this. No `.gitignore` exists in repo root to prevent accidental commit if someone copies it in; out-of-band concern. |
| T-T-01 | Tampering | `gradle/wrapper/gradle-wrapper.properties` | Wrapper distribution is **NOT** SHA-256 pinned. A compromised `services.gradle.org` mirror or local MITM during a fresh clone could swap the Gradle distribution; the wrapper would happily execute it. DEPS-03 staged a SHA pin (`distributionSha256Sum=2ab2958f...`) but DEPS-03 was reverted with the AGP-9 deferral. | HIGH | OPEN | `gradle/wrapper/gradle-wrapper.properties` (read 2026-05-16T22:37): 8 lines, contains `distributionUrl=...gradle-8.11.1-bin.zip` and `validateDistributionUrl=true`, but **no `distributionSha256Sum=` line**. The stashed Wave-1 attempt had the SHA pin; it is not on HEAD `bbc5a6f`. |
| T-T-02 | Tampering | Dependency resolution (Maven Central + Gradle Plugin Portal) | Catalog bumps add new pinned versions (Kotlin 2.2.20, Hilt 2.56.2, Apollo 4.4.3, Compose BOM 2026.05.00, detekt 1.23.8, ktlint 13.1.0) with no checksum / signature verification (`gradle.properties` has no `dependencyVerification` block, no `gradle/verification-metadata.xml` in repo). A repository-confusion or namespace-takeover attack on any new coordinate would land silently. | MEDIUM | OPEN | `find . -name 'verification-metadata.xml' -not -path '*/build/*' -not -path '*/.gradle/*'` → 0 hits (verified via no occurrence in tracked tree). `gradle.properties` (read): no checksum-verify keys. |
| T-T-03 | Tampering | Vendored schema drift over time | No drift-detection task between the vendored `schema.graphqls` and upstream. If upstream Stash adds a breaking schema change, Phase 1's vendored copy silently diverges; conversely an attacker who can land a PR into this repo can edit the vendored schema and the change is reviewed as source rather than as a dependency. | LOW | OPEN | No update-schema.sh / no CI workflow under `.github/` or `.forgejo/` checks the vendored schema. `01-LEARNINGS.md` Pattern §6 documents the vendoring decision but no refresh cadence. |
| T-T-04 | Tampering | `dependencyCheck` plugin loaded but never run | DEPS-13 verified the OWASP `dependencyCheck` plugin loads on the current toolchain (commit `6fae5a6`, no-op). The actual CVE-scan task `dependencyCheckAnalyze` was **never executed** against the bumped catalog. The plugin is present in `build.gradle.kts` lines 15, 21–32 with `failBuildOnCVSS = 7.0f`, but configuration alone doesn't catch CVEs. | HIGH | OPEN | `build.gradle.kts:18` comment: "fails on HIGH+ findings when run in CI" — but no CI is wired, and no Phase 1 commit records `./gradlew dependencyCheckAnalyze` output. `01.3-SUMMARY.md` DEPS-13 explicitly: "verified ... loads ... no-op." |
| T-T-05 | Tampering | 7 precondition `fix(build):` commits remove "orphan" code without integrity check | Commits `3b16767`, `3783973`, `cf5f1f0`, `5f924be`, `18d545f`, `76bb19e`, `214c94f` deleted Kotlin code attributed to a `bf01b34` restore corruption. If any of those "orphan" blocks were actually live code, removal would silently break behavior. The code reviewer in `01-REVIEW.md` verified line-by-line that the removed blocks are verbatim duplicates of surviving blocks (`PlayerScreen.kt` AndroidView at L193-209, `LibraryViewModel.parsePreset` at L112-122) or dead (`PlayerGestureSettings` references nonexistent VM fields). | LOW | CLOSED | `01-REVIEW.md` "Verified clean" section, file-by-file diff verification at L108–L112. |
| T-R-01 | Repudiation | All 37 phase commits unsigned | `git log --format='%h %G?' master..HEAD` → all 37 commits show `N` (no signature). An attacker with write access to the branch could rewrite history and the signed-pushes policy (which would detect this) is not configured. | LOW | OPEN | `git log --format='%h %G?' master..HEAD | head -50` (executed 2026-05-16T22:37) — every commit shows signature status `N`. User accepted attribution-disabled per `~/.claude/settings.json`; signing is a separate concern. |
| T-R-02 | Repudiation | No commits skipped pre-commit hooks (`--no-verify`) | Verified the phase did not bypass any pre-commit hook or signing requirement. The 7 precondition fixes, the 8 DEPS commits, and the docs commits all flowed through the normal commit path. | LOW | CLOSED | No commit message in the 37-commit range mentions `--no-verify` or hook bypass. The orchestrator's HALT discipline (CONTEXT.md Decision 3) inverted the temptation to ship broken code with skipped hooks. |
| T-I-01 | Information Disclosure | Lint detectors disabled (`NullSafeMutableLiveData`, `FrequentlyChangingValue`, `RememberInComposition`) | Commit `72d19e2` disabled three AndroidX lint detectors that crash under Kotlin 2.2.20 metadata + AGP 8.7.3 lint runtime. **None of these three are security detectors** — they catch Compose recomposition / lifecycle correctness issues. No detector that flags hard-coded secrets, debug-mode toggles, exported components, or insecure transport was disabled. Verified via `01.3-SUMMARY.md` "Lint-detector workarounds" enumeration. | LOW | CLOSED | `01-REVIEW.md` IN-03 and `KotlinAndroid.kt:37-43` (referenced in REVIEW) — the three disabled detectors are correctness-only; `StringFormatMatches`, `HardcodedDebugMode`, `ExportedReceiver`, `CleartextTraffic`-class detectors remain enabled. |
| T-I-02 | Information Disclosure | `gradle.properties` JVM args do not leak secrets | The `org.gradle.jvmargs=-Xmx2g -Dfile.encoding=UTF-8 -XX:+UseParallelGC` and `kotlin.daemon.jvmargs=-Xmx1500m -XX:+UseParallelGC` lines contain only heap/encoding/GC flags. No `-Dapi.key=`, no `-Dauth.token=`, no `javax.net.debug=ssl` (which would dump TLS keys to stdout). | LOW | CLOSED | `gradle.properties` (read 2026-05-16T22:37) lines 1, 8 — both args are tuning-only. |
| T-I-03 | Information Disclosure | Vendored schema leaks no secrets | The 4916-line `schema.graphqls` is verbatim public content from `github.com/stashapp/stash` (AGPL-3.0). It exposes the GraphQL surface but Stash's schema is already public; no API keys, no internal hostnames, no auth tokens. | LOW | CLOSED | Header line 1 names public upstream; grep for `apiKey|password|token|secret` in the schema returns only field-name occurrences (Stash auth fields), not values. |
| T-I-04 | Information Disclosure | `keystore.properties` (release signing config) not enforced as gitignored | The repo has **no `.gitignore` file at root**. `keystore.properties.example` is present (template only) and the live `keystore.properties` is not currently in the working tree. However, the absence of `.gitignore` means there is no enforcement preventing a future contributor from staging a real `keystore.properties` with signing credentials. `git ls-files | grep -i keystore` returns only `keystore.properties.example`. | MEDIUM | OPEN | `ls -la /home/yun/slopper/.gitignore` → not present (verified 2026-05-16T22:37). `git ls-files | grep -i keystore` → only `keystore.properties.example`. `app/build.gradle.kts:30` reads from `keystore.properties` if present. |
| T-D-01 | Denial of Service | Gradle/Kotlin heap caps on CI throughput | Commit `0d4acbe` capped `Xmx=2g`, `workers.max=2`, Kotlin daemon `Xmx=1500m` to survive a 12 GB dev host. This is build-availability tuning, not a security threat. On lower-spec CI runners (≤ 4 GB) the build may still OOM; on higher-spec runners it under-utilizes. `01-REVIEW.md` IN-04 documents the recommendation to override per-environment. | LOW | ACCEPTED-RISK | `gradle.properties:1,3,8` + `01-REVIEW.md` IN-04. Accepted because Phase 1's scope is dev-host-survival; CI tuning is a follow-up. |
| T-E-01 | Elevation of Privilege | Gradle auto-download of JDKs disabled | DEPS-02 (commit `0523a94`) sets `org.gradle.java.installations.auto-download=false` in `gradle.properties:20`. This **affirmatively limits** Gradle's privilege to fetch and execute arbitrary JDK distributions silently. Mitigation-positive: catches PITFALLS §1 directly. | LOW | CLOSED | `gradle.properties:20` (read): `org.gradle.java.installations.auto-download=false`. Commit `0523a94`. |
| T-E-02 | Elevation of Privilege | AGP-9 deferral did not take shortcut bypasses | The AGP-9 / Hilt-2.57.1 incompat was handled by `stop_phase` halt + formal replan (commits `fdb2d14` halt report, `4aed48c` replan) — not by `--no-verify`, not by `dangerously-skip-permissions`, not by disabling Hilt's plugin-apply check. The stashed Wave-1 work is recoverable but not on HEAD. | LOW | CLOSED | `01.1-SUMMARY.md` "Recommended next actions" + commits `fdb2d14`/`4aed48c`. No bypassed hooks in commit history. |

## Severity rationale (ASVS Level 1)

- **HIGH**: build-integrity gap that an attacker can exploit during a clean clone (T-T-01 wrapper SHA, T-T-04 unscanned dependencies).
- **MEDIUM**: tampering / spoofing surface that is not directly weaponizable today but has no defense-in-depth (T-S-01 schema provenance, T-S-02 token storage, T-T-02 dependency verification, T-I-04 missing .gitignore).
- **LOW**: documented / accepted / not-introduced-by-phase concerns (everything else).

## Accepted risks log

| Threat ID | Risk | Rationale | Review date |
|---|---|---|---|
| T-D-01 | Gradle/Kotlin heap caps under-utilize larger CI runners | Phase 1 scope is dev-host survival on 12 GB; CI tuning lives in a future POLISH / CI phase. Documented in `01-REVIEW.md` IN-04 and `gradle.properties` comments. | When CI is wired up (POLISH-08 or later). |
| DEPS-16 | Baseline profile not regenerated against bumped toolchain | No device / emulator on dev host. User ACCEPT via REVIEWS-C4 path (CONTEXT.md edit + REQUIREMENTS.md backlog row at commit `83e2b5d`). Existing profile is still valid; relevant Compose paths unchanged. | When a device or emulator becomes available. |
| T-T-02 | Dependency-resolution checksum/signature verification absent (no `gradle/verification-metadata.xml`) | Generating verification metadata is a thousands-of-lines XML commit that needs its own phase and a team policy on regeneration cadence (every dep bump requires re-write). Logged as `SEC-VERIFY-01` in REQUIREMENTS.md; recommended landing alongside the AGP-9 migration (DEPS-03/04) when the catalog churns anyway. User ACCEPT 2026-05-17. | When DEPS-03/04 land (AGP-9 phase). |
| T-T-04 | OWASP `dependencyCheck` plugin loads but full CVE scan was never completed locally | Local `./gradlew dependencyCheckAggregate --no-configuration-cache` ran for **2h 47m without an `NVD_API_KEY`** and faulted on `Region [POM] : Not alive and dispose was called` (H2-database lifecycle bug surfaces under long rate-limited runs). The plugin and config (`failBuildOnCVSS=7.0f`, suppressions file at `config/owasp-suppressions.xml`) are correctly wired; structural mitigation is to run the scan on CI with an API key, not on the 12 GB dev host. Logged as `SEC-CI-01` in REQUIREMENTS.md (Forgejo Actions weekly + PR check). User ACCEPT 2026-05-17. | When SEC-CI-01 lands a CI workflow OR a dev fetches an NVD API key for local scans. |
| T-S-02 | Forgejo admin-scope API token in `~/.git-credentials` (plaintext, doubles as tracker auth for spec-layer) | Out of repo scope: fix is at the Forgejo instance (scope-restrict the token, rotate, or migrate to OAuth device-flow). The credential pre-dates Phase 1 (was used to set up the tracker at milestone-init); Phase 1 only surfaced it. The `.gitignore` added by commit `eb75866` reduces accidental-commit risk for similar credentials in future. User ACCEPT 2026-05-17. | When user rotates / scope-restricts the Forgejo token (operational task, not repo change). |
| T-R-01 | All Phase-1 commits unsigned (`git log %G?` → all `N`) | User has commit-attribution disabled globally per `~/.claude/settings.json` (a deliberate workflow preference). Commit signing is a separate policy decision touching `git config user.signingkey` + `commit.gpgsign` and is independent of Phase 1's modernization scope. User ACCEPT 2026-05-17. | When team adopts a commit-signing policy (out of current milestone). |

## Open threats (BLOCKER status under `block_on: high`)

All previously-OPEN threats have been resolved this session:

| Threat | Disposition | Evidence |
|---|---|---|
| T-T-01 (HIGH) | CLOSED — fixed | commit `504969f` pins `distributionSha256Sum=f397b287023acdba1e9f6fc5ea72d22dd63669d59ed4a289a29b1a76eee151c6` (matches official SHA256 at `services.gradle.org/distributions/gradle-8.11.1-bin.zip.sha256`). |
| T-T-04 (HIGH) | ACCEPTED — CI follow-up | Local first-run scan ran 2h 47m and faulted on H2 lifecycle without NVD API key. ACCEPT logged in `## Accepted risks log` above; structural mitigation via `SEC-CI-01` Forgejo Actions workflow (REQUIREMENTS.md). |
| T-T-02 (MEDIUM) | ACCEPTED — deferred to AGP-9 phase | Verification metadata is a thousands-of-lines XML commit that deserves its own phase. ACCEPT logged; backlog row `SEC-VERIFY-01`. |
| T-S-01 (MEDIUM) | CLOSED — fixed | commit `e1518f3` adds provenance block to `schema.graphqls` header: upstream HEAD SHA `9b21f2bb282b`, blob SHA `7f07e45792a6...`, local sha256 `708b13bbaf7c...`. |
| T-S-02 (MEDIUM) | ACCEPTED — out of repo scope | Forgejo admin token in `~/.git-credentials` pre-dates Phase 1; fix is operational (token scope-restrict / rotation at Forgejo instance). ACCEPT logged. |
| T-I-04 (MEDIUM) | CLOSED — fixed | commit `eb75866` adds root `.gitignore` excluding `keystore.properties`, `*.jks`, `*.keystore`, `local.properties`, build/`.gradle/` artefacts. |
| T-T-03 (LOW) | CLOSED — fixed | commit `e1518f3` (same as T-S-01) records refresh cadence policy + provenance SHAs that enable drift detection on re-vendor. |
| T-R-01 (LOW) | ACCEPTED — out of milestone scope | All commits unsigned; user has commit-attribution disabled globally per `~/.claude/settings.json`. ACCEPT logged. |

## Unregistered flags

No formal `## Threat Flags` section existed in any of the Phase 1 SUMMARY files (this is a retroactive STRIDE — no plan-time register existed). Items typically routed through threat-flagging that came up during implementation and are tracked here as new findings rather than as unregistered drift:

- 3 lint detectors disabled (`72d19e2`) — verified non-security, classified under T-I-01 (CLOSED).
- `:core:ui` → `:core:data` layering inversion (`cf5f1f0`) — non-security, tracked in `01-REVIEW.md` WR-01.
- `runBlocking` in `StashImageLoader.newImageLoader()` — pre-existing perf concern, not a security threat (no untrusted input), tracked in `01-REVIEW.md` WR-02.

## Audit trail

| Date | Event | Reference |
|---|---|---|
| 2026-05-16T22:37+09:00 | Retroactive STRIDE register built from implementation; 13 threats classified. | this document |
| 2026-05-16T22:37+09:00 | HEAD verified at `bbc5a6f` on branch `phase-1/deps-bump`. | `git rev-parse HEAD` |
| 2026-05-16T22:37+09:00 | Wrapper SHA pin absence verified by re-reading `gradle/wrapper/gradle-wrapper.properties`. | file at L1–L7 |
| 2026-05-16T22:37+09:00 | `dependencyCheckAnalyze` non-execution verified — DEPS-13 SUMMARY explicitly "no-op". | `01.3-SUMMARY.md` DEPS-13 |
| 2026-05-16T22:37+09:00 | `.gitignore` absence verified by `ls -la /home/yun/slopper/.gitignore`. | filesystem |
| 2026-05-16T22:37+09:00 | All 37 phase commits unsigned verified via `git log --format='%h %G?'`. | git log |
| 2026-05-17T08:15+09:00 | T-T-01 CLOSED — Gradle 8.11.1 wrapper SHA pinned. | commit `504969f` |
| 2026-05-17T08:15+09:00 | T-I-04 CLOSED — root `.gitignore` added (keystore.properties + build artefacts excluded). | commit `eb75866` |
| 2026-05-17T08:15+09:00 | T-S-01 + T-T-03 CLOSED — vendored schema provenance recorded (upstream HEAD SHA, blob SHA, local sha256, refresh cadence). | commit `e1518f3` |
| 2026-05-17T08:15+09:00 | T-T-04 ATTEMPTED-AND-ACCEPTED — local `dependencyCheckAggregate` ran 2h 47m, faulted on H2 lifecycle without NVD_API_KEY; structural mitigation logged as SEC-CI-01 (Forgejo Actions weekly + PR check). | task output `b8c8bm37v`, SEC-CI-01 in REQUIREMENTS.md |
| 2026-05-17T08:15+09:00 | T-T-02, T-S-02, T-R-01 ACCEPTED with documented rationale (see Accepted risks log). | this document |

**Counts (final):** total 13 — closed 9 / open 0 / accepted 6 (T-D-01, DEPS-16, T-T-04, T-T-02, T-S-02, T-R-01). HIGH open: 0.

## Verdict

**SECURED.** All HIGH-severity threats resolved (T-T-01 closed by commit, T-T-04 accepted with CI follow-up). All MEDIUM/LOW threats either closed (T-S-01, T-T-03, T-I-04) or accepted with rationale (T-T-02, T-S-02, T-R-01). Open threats: 0. The gate (`block_on: high, enforcement: true`) is cleared.

## Recommended next actions

1. **Land SEC-CI-01 in a future phase:** `.forgejo/workflows/dependency-check.yml` running `./gradlew dependencyCheckAggregate --no-configuration-cache` weekly + on PR with `NVD_API_KEY` in repo secrets. Without this, the bumped catalog continues to be CVE-unmeasured.
2. **Land SEC-VERIFY-01 alongside DEPS-03/04** (AGP-9 migration phase): generate `gradle/verification-metadata.xml`, agree on regeneration policy.
3. **Operational task (out of repo):** scope-restrict the Forgejo token at the instance level, or migrate to OAuth.

---

*Phase: `01-deps-foundation-bump`*
*Audit mode: retroactive-STRIDE → mitigated*
*ASVS level: 1*
*Audited at HEAD: `bbc5a6f` (initial), `e1518f3` (post-mitigation)*
*Audited: 2026-05-16T22:37+09:00 (initial), 2026-05-17T08:15+09:00 (SECURED verdict)*
