---
phase: 1
reviewers: [gemini, codex, kimi-k2.6, deepseek-v4-pro, glm-5.1, minimax-m2.7, devstral-2-123b, opencode-qwen3.6-plus]
reviewed_at: 2026-05-16
plans_reviewed:
  - .planning/phases/01-deps-foundation-bump/01.1-PLAN.md
  - .planning/phases/01-deps-foundation-bump/01.2-PLAN.md
  - .planning/phases/01-deps-foundation-bump/01.3-PLAN.md
---

# Cross-AI Plan Review — Phase 1 (DEPS Foundation Bump)

**Reviewers invoked:** 8 (gemini CLI, codex CLI, opencode/qwen, kimi-k2.6, deepseek-v4-pro, glm-5.1, minimax-m2.7, devstral-2-123b)

## Consensus Summary

The plans are well-structured: every reviewer agrees on lockstep ordering correctness, atomic-per-requirement commit discipline, the 4th SDK touchpoint catch (`baselineprofile/build.gradle.kts`), the Media3 conditional-with-probe pattern, and KSP/Kotlin/Compose-compiler triangle handling. Verdicts span LOW (gemini, devstral) to MEDIUM (codex, kimi, opencode) — no reviewer issued an overall HIGH verdict, but several flagged specific HIGH-severity defects.

### Consensus issues by frequency (3+ reviewers concur)

| # | Issue | Severity | Affects | Reviewers |
|---|-------|----------|---------|-----------|
| C1 | **Gradle 9.4.1 lands on a commit where AGP is still 8.7.3** — DEPS-03 commits the wrapper bump and runs `assembleDebug` per the per-commit gate; AGP 8.x cannot load on Gradle 9, so the build fails between DEPS-03 and DEPS-04. The atomic-per-req rule and the lockstep STOP-the-phase rule create a deadlock here: the only working fix is combining DEPS-03 + DEPS-04 into a single commit. | **HIGH** | 01.1 Task 3 → Task 4 | glm (primary), codex (implicit), minimax (implicit) |
| C2 | **Revert-and-continue mechanics underspecified for uncommitted edits** — several tasks say "revert this commit" before a commit exists; need explicit `git diff` / `git restore <files>` / commit-then-revert handling, and CONTEXT.md says deferred items must be appended to REQUIREMENTS.md backlog — the plans don't enforce that. | HIGH | 01.2 Tasks 1–4, 01.3 Tasks 3–4 | codex, kimi, deepseek |
| C3 | **DEPS-10 Media3 probe regex too loose** — current grep `'<version>1\.10[^<]*</version>'` matches any 1.10.x; SPEC.md locks the target at `media3 = "1.10.0"` exactly. Probe should constrain to `1.10.0` (or accept patch-level variants only with explicit policy). | HIGH | 01.2 Task 4 | codex, minimax, opencode |
| C4 | **DEPS-16 device-unavailable fallback violates locked acceptance** — task allows "STOP & document" if no device/AVD is available, but DEPS-16 is in SPEC.md acceptance criteria; phase can't close with it unmet unless the user explicitly defers (and SEC.md has no defer mechanism). | HIGH | 01.3 Task 6 | codex, gemini, minimax |
| C5 | **DEPS-15 freshness assertion uses `-ge` where `-gt` is safer** — equal timestamps from rapid commits can mask staleness. | MEDIUM | 01.3 Task 5 / 6 | minimax, opencode |
| C6 | **`releaseRuntimeClasspath \| grep compose-compiler` won't prove Compose Compiler plugin uniqueness** — Compose Compiler is a Gradle/compiler plugin dep, not a runtime classpath entry. Verification can false-fail or pass for the wrong reason. | HIGH | 01.1 Task 5, 01.3 Task 5 | codex, opencode |

### Unique HIGH-severity findings (single reviewer; worth weighing)

- **kimi:** *R8 audit in DEPS-04 runs before library bumps.* Serialization 1.9.0, Hilt 2.57.1, Apollo 4.4.3, Media3 1.10.0 are the actual sources of new keep-rule needs — running the R8 audit at DEPS-04 (before DEPS-08..10) means library-induced regressions won't be caught until DEPS-13 release smoke, at which point reverting them is awkward because keep-rule edits are typically appended to `app/proguard-rules.pro`. *Affects 01.1 Task 4 → 01.3 release smoke.*
- **kimi:** *DEPS-08 KSP `ksp.incremental=false` workaround is self-inconsistent* — the plan tells the executor to apply the workaround locally but "not commit unless required"; the verify gate then runs `./gradlew clean :app:kspDebugKotlin` which will fail without the workaround on a clean tree.
- **glm:** *DEPS-14 bisect doesn't distinguish lockstep vs independent culprits* — the auto-revert path can break the lockstep chain if bisect lands on a DEPS-01..06 commit. Should hard-STOP the phase if the bisected culprit is in the lockstep set.

### Strengths consensus

All 8 reviewers commend:
- DEPS-01 lint-baseline lands first; correct ordering
- 4th SDK touchpoint in `baselineprofile/build.gradle.kts` captured
- Atomic per-requirement commits preserve git bisect
- Media3 CASE A/B explicit branching
- `git log -G 'composeBom = "2026.05.00"'` pin for baseline-prof freshness (post the planner-checker B1 fix)
- Explicit lockstep vs revert-and-continue boundary

### Risk Verdicts Summary

| Reviewer | Verdict |
|----------|---------|
| gemini | LOW |
| codex | MEDIUM |
| kimi-k2.6 | MEDIUM |
| deepseek-v4-pro | MEDIUM (implied) |
| glm-5.1 | HIGH (Gradle/AGP coupling) |
| minimax-m2.7 | MEDIUM-HIGH |
| devstral-2-123b | LOW (ready-to-execute) |
| opencode (qwen3.6-plus-free) | MEDIUM |

### Recommendation

**Do NOT execute as-is. Re-plan with `/gsd-plan-phase 1 --reviews` to address at minimum the 4 HIGH consensus items (C1–C4) plus the C6 Compose-compiler verify bug.**

Suggested fixes the planner should adopt:

- **C1 (Gradle/AGP):** Either (a) combine DEPS-03 + DEPS-04 into a single commit (`chore(deps): DEPS-03+04 — bump Gradle 9.4.1 + AGP 9.2.0 (lockstep — Gradle 9 requires AGP 9)`), or (b) intermediate Gradle 8.13 first to keep AGP 8.7.3 working, then DEPS-04 bumps AGP and Gradle together. Option (a) is cleaner; document in CONTEXT.md as a refinement to Decision 1's atomic-per-req rule for this specific coupling.
- **C2:** Each task in 01.2 / 01.3 needs a "Failure & Revert" section specifying `git diff` (snapshot), `git restore <files>` (uncommitted) vs `git revert HEAD` (committed), and an explicit `echo "DEPS-XX: <reason>" >> .planning/REQUIREMENTS.md` backlog append (or via REQUIREMENTS.md "Deferred to Future Milestones" edit).
- **C3:** Change Media3 probe regex to `'<version>1\.10\.0[^<]*</version>'` exactly (or document patch-level acceptance policy in CONTEXT.md).
- **C4:** DEPS-16 must hard-fail the phase if no device available. Per-phase deferral should be a CONTEXT.md edit and a user `ACCEPT:` flag, not a per-task option.
- **C6:** Replace `releaseRuntimeClasspath | grep compose-compiler` with a check that consults the Compose Compiler plugin coordinate in build output (e.g. `./gradlew :app:dependencies --configuration kotlinCompilerPluginClasspathRelease | grep compose-compiler-plugin` — or just verify the catalog entry).

---

## Gemini Review (Google Gemini CLI)

# Phase 1: DEPS (Foundation Bump) — Plan Review

## 1. Summary
The plans are highly robust, meticulously detailed, and perfectly aligned with the locked `SPEC.md` and `CONTEXT.md` mandates. They demonstrate a sophisticated understanding of Android build toolchain nuances, particularly regarding the lockstep ordering of lint baselines, compiler plugins, and R8 wildcard semantics. The integration of empirical verification (e.g., Media3 probe, R8 warning audit, and baseline profile freshness invariant) significantly reduces the risk of silent regressions during this high-churn infrastructure phase.

## 2. Strengths
*   **Ordering Integrity:** Correctly prioritizes the lint-baseline generation (`DEPS-01`) before any toolchain changes, ensuring that the warning surface is frozen against AGP 8.7.3 as required by Pitfall §18.
*   **Touchpoint Accuracy:** Explicitly identifies and modifies all four SDK touchpoints, including the frequently overlooked `baselineprofile/build.gradle.kts`, ensuring project-wide consistency for `compileSdk/targetSdk 36`.
*   **Profile Freshness Invariant:** Implements a rigorous `git log -G` verification in Plan 01.3 Task 6 to programmatically prove the baseline profile was regenerated *after* the Compose BOM bump.
*   **Toolchain Coupling:** Correctly manages the "KSP ↔ Kotlin ↔ Compose-Compiler" version triangle by bumping them lockstep and verifying the resolution via dependency insight.
*   **R8 Audit Rigor:** Includes a mandatory R8 warning scan in `fullMode` during Plan 01.1 Task 4, providing a proactive defense against Pitfall §9 (wildcard semantics) before the release APK is ever smoke-tested.
*   **Media3 Conditional Logic:** Robustly implements the Case A/Case B branching for Media3 based on a live Maven Central probe, as specified in the context decisions.

## 3. Concerns
*   **Plan 01.3 Task 6 [MEDIUM]:** Dependency on a connected device/emulator. Since GMD wiring is deferred to Phase 3, this task relies on an ambient device or AVD. If the execution environment is a headless CI or restricted runner, `DEPS-16` will fail. The plan correctly notes a "stop and document" recovery, but this remains the primary risk to "16/16 requirements met" status.
*   **Plan 01.1 Task 4 [LOW]:** R8 audit duration. Running `assembleRelease` with `fullMode` can be time-intensive. While necessary for verification, it may significantly increase the turn time for this specific task.
*   **Plan 01.3 Task 3 [LOW]:** `dependencyCheck` failure. The plan correctly handles the "remove if broken" logic, but given its history of config-cache incompatibility, it is the most likely candidate for the `revert_and_continue` path.

## 4. Suggestions
*   **Plan 01.1 Task 4:** Suggest adding a pre-flight check for Build Tools 36.0.0 via `sdkmanager` (if available in the environment), as AGP 9.2.0 targeting API 36 will fail immediately if the corresponding build tools are missing.
*   **Plan 01.2 Task 1:** In the `DEPS-07` AndroidX sweep, consider explicitly checking for `androidx.collection` version resolution, as `Coil 3.4.0` often transitively bumps this, potentially causing a mismatch with older `compose-ui` versions if not managed by a BOM.
*   **Plan 01.3 Task 5:** If a `strictly()` override is required for a catalog entry, ensure it is applied using the rich version syntax in `libs.versions.toml` (e.g., `version = { strictly = "..." }`) to prevent the Gradle resolver from preferring a higher transitive version.

## 5. Risk Assessment
**LOW.** The strategy is grounded in empirical research and live probes. By isolating the toolchain lockstep into Wave 1 and applying a `stop_phase` policy there, the plans ensure that no library bumps (Wave 2) or hygiene tasks (Wave 3) proceed on a fractured foundation. The risk of runtime failure is mitigated by specific smoke-test targets (`benchmark` build type) and DI-injection verification.

---

## Codex Review (OpenAI Codex CLI)

## 1. **Summary**

The plans are strong overall: they preserve the required lockstep ordering, break work into mostly atomic commits, include concrete file-level edits, and carry the key research findings into executable steps. The main weaknesses are in verification precision and recovery mechanics. Several checks do not actually prove the stated acceptance criteria, especially `javaToolchains`, Compose Compiler resolution, dependencyCheck handling, and baseline-profile freshness. The revert-and-continue plans also need sharper mechanics for how failed uncommitted edits are reverted, logged, and deferred.

## 2. **Strengths**

- Lockstep ordering is mostly correct: DEPS-01 lint baseline precedes Gradle, AGP, Kotlin, and Compose bumps.
- 01.1 correctly treats DEPS-01..06 as stop-the-phase gates, matching CONTEXT.md Decision 3.
- The AGP task catches the often-missed `baselineprofile/build.gradle.kts` SDK touchpoints.
- Kotlin/KSP/Compose compiler coupling is recognized and handled in the right phase.
- Media3 has an execution-time Maven probe and both CASE A / CASE B branches.
- Baseline profile regeneration is correctly placed after the full dependency floor is landed.
- Scope discipline is generally good: no feature work, no minSdk bump, no module graph changes.
- Verified current external assumptions align with the plan: AGP 9.2.0 lists Gradle 9.4.1 support in official docs, and `nextlib-media3ext:1.10.0-0.12.1` exists on Maven Central.

## 3. **Concerns**

- [HIGH] 01.1 Task 5 / 01.3 Task 5: `releaseRuntimeClasspath | grep compose-compiler` likely does not prove Compose Compiler plugin uniqueness. Compose Compiler is a Gradle/compiler plugin dependency, not normally a runtime dependency. This can false-fail or give a misleading pass.

- [HIGH] 01.2 overall: revert-and-continue mechanics are underspecified. Several tasks say “revert this commit” before a commit exists. The plan needs explicit handling for uncommitted edits: `git diff`, `git restore <files>`, or commit-then-revert. It also does not consistently update `.planning/REQUIREMENTS.md` deferred backlog as required by CONTEXT.md Decision 3.

- [HIGH] 01.2 Task 4: Media3 probe branch allows `media3 = "1.10.X"` if a newer 1.10.x nextlib appears. SPEC.md locks Media3 target to `1.10.0`, not arbitrary `1.10.1`. The nextlib extension can float within matching 1.10.x, but Media3 itself should stay at the locked target unless the spec is changed.

- [HIGH] 01.3 Task 6: If no device/emulator is available, the task “STOPs” and documents a prerequisite gap, but DEPS-16 is a locked requirement and phase success depends on it. The plan should not allow phase close / PR creation with DEPS-16 merely documented as unavailable unless explicitly deferred by the owner.

- [MEDIUM] 01.1 Task 2: JDK verification only checks “at least one JDK 17,” but the success criterion says one resolved JDK 17. If exact-one is too strict for real developer machines, the plan should redefine the check to prove Gradle selects JDK 17 for toolchains, not just that one is listed.

- [MEDIUM] 01.1 Task 4: R8 audit happens before serialization 1.9, Hilt 2.57.1, Apollo 4.4.3, and Media3 1.10 land. The audit should be repeated after DEPS-08/10 or at the final release/benchmark gate, because later library bumps can change consumer rules and minified behavior.

- [MEDIUM] 01.2 Task 2: Hilt fallback says to append `ksp.incremental=false` but “do not commit unless required.” If it is required for a green build, it changes `gradle.properties`, which is outside the task file list and needs explicit commit/deferred behavior.

- [MEDIUM] 01.3 Task 3: dependencyCheck verification conflates plugin-load failure with vulnerability findings. The automated verify fails if `dependencyCheckAnalyze` exits nonzero due CVSS findings while the plugin itself loads correctly, contradicting the task text.

- [MEDIUM] 01.3 Task 4: bisect base command is wrong for this phase. `git rev-list --max-parents=0` finds the repository root commit, not the pre-phase base. Use `git merge-base master phase-1/deps-bump` or the parent of the DEPS-01 commit.

- [MEDIUM] 01.3 Task 2: `git add -u` after `ktlintFormat` can stage unrelated dirty work. For atomic per-requirement commits, the plan should require `git status --porcelain` review and path-specific staging.

- [MEDIUM] 01.3 Task 2: baseline “newer than catalog commit” is muddled because catalog bump and regenerated baseline are committed together. The plan should either compare working-tree mtimes before commit or define acceptance as “same commit, generated after tool bump.”

- [LOW] 01.1 Task 1: branch creation assumes a clean `master` and no existing `phase-1/deps-bump` branch. Add a preflight for current branch, clean worktree, and existing branch handling.

- [LOW] 01.1 Task 1: DEPS-01 does not run the standard per-commit `assembleDebug` gate even though CONTEXT.md says it should run after every commit.

- [LOW] 01.2 Task 4: shell selection of latest nextlib version is not robust for multi-line versions. Use `sort -V | tail -1` or parse Maven metadata’s `<release>` safely.

## 4. **Suggestions**

- For 01.1 Task 5 and 01.3 Task 5, replace runtime classpath Compose Compiler checks with a plugin/buildscript-level check. For example, inspect `./gradlew buildEnvironment`, plugin resolution output, or Gradle dependency insight on the build logic/plugin classpath rather than `releaseRuntimeClasspath`.

- In 01.2 and 01.3, add a standard failure block to every revert-and-continue task:
  - capture `git diff`
  - restore only task-owned files if no commit was made
  - if a commit was made, `git revert --no-edit <sha>`
  - append the required `Failures & Reverts` row
  - add the deferred requirement entry when CONTEXT.md Decision 3 requires it

- In 01.2 Task 4, lock Media3 itself to `1.10.0`. Let only `nextlibMedia3Ext` select the latest compatible `1.10.x` extension unless the locked spec is amended.

- Add a second minified R8/smoke pass after DEPS-08 and DEPS-10, or explicitly make DEPS-14 include R8 warning review for the final graph.

- In 01.3 Task 3, split dependencyCheck validation into “plugin loads” and “security findings.” A CVE-triggered nonzero exit should not cause plugin removal.

- In 01.3 Task 6, make connected-device availability a preflight before starting Wave 3, or state that Phase 1 cannot close until DEPS-16 is regenerated.

- In 01.3 Task 4, use `git merge-base master phase-1/deps-bump` as the bisect good base.

- Replace broad `git add -u` in 01.3 Task 2 with explicit review and path staging to preserve atomic commits.

## 5. **Risk Assessment**

**MEDIUM** — the plan is well-structured and mostly complete, but several verification commands under-prove the locked requirements, and the revert/defer mechanics need tightening before this is safe to execute autonomously.

---

## Kimi K2.6 Review (Moonshot, via pi)

**Summary**
The three plans present a well-structured, atomic execution strategy for Phase 1 with correct lockstep ordering (DEPS-01..06 in 01.1), sensible revert-and-continue boundaries (DEPS-07..16 in 01.2/01.3), and thorough command-level verification gates. The commitment to per-requirement commits, explicit file lists, and pre-bump lint baseline generation shows strong process discipline. However, an R8 audit timing defect, a broken `git bisect` command, and an ambiguous KSP fallback policy create execution-level risks that could mask regressions or delay investigation.

---

**Strengths**
- **Correct lockstep gating.** Plan 01.1 uses `on_failure: stop_phase` for DEPS-01..06 and 01.2/01.3 correctly downgrade to `revert_and_continue` for DEPS-07..16, matching CONTEXT.md Decision 3 exactly.
- **Atomic commit discipline.** Every requirement maps to a single conventional commit (`chore(deps): DEPS-XX — ...`) with explicit file boundaries, preserving `git bisect` hygiene.
- **Fourth SDK touchpoint identified.** 01.1 Task 4 explicitly includes `baselineprofile/build.gradle.kts` (line 9/21) for the `compileSdk`/`targetSdk` bumps, which SPEC.md originally omitted.
- **Live Maven Central probe with CASE A/B branching.** 01.2 Task 4 re-probes `nextlib-media3ext` at execution time and provides both a bump path and a documented deferral path.
- **Exhaustive per-requirement verification.** Almost every task includes a `verify` block with `grep` assertions and `gradlew` commands rather than hand-wavy "check it" steps.
- **Dependency audit artifact.** 01.3 Task 5 materializes `:app:dependencies` to `.planning/phases/01-deps-foundation-bump/deps-audit.txt`, creating an auditable paper trail for PITFALLS §10 convergence.

---

**Concerns**
- **[HIGH] R8 audit is premature for library-specific keep rules.** Plan 01.1 Task 4 (DEPS-04) runs `assembleRelease -Pandroid.enableR8.fullMode=true` *before* library bumps land. SPEC.md Acceptance Criteria and RESEARCH.md §4 cite `kotlinx.serialization 1.9.0`, `Hilt 2.57.1`, `Apollo 4.4.3`, and `Media3 1.10.0` as sources of potential new keep rules, but those libraries are not bumped until Plan 01.2 (DEPS-08, DEPS-09, DEPS-10). A serialization 1.9.0 `Missing class KSerializer` warning or AGP-9 wildcard regression will not be caught by the DEPS-04 scan. *[Affects 01.1 Task 4, 01.2 Tasks 2–4, 01.3 Task 4]*
- **[HIGH] DEPS-08 KSP fallback policy is self-inconsistent.** 01.2 Task 2 says: if `ksp.incremental=false` is required to avoid the Hilt aggregating-processor NPE, do NOT commit that change "unless required" — but the verification gate runs `./gradlew clean :app:kspDebugKotlin`, which will fail on a clean checkout if the fix was only applied locally and not committed. A commit that passes locally but fails CI is not a landed requirement. *[Affects 01.2 Task 2]*
- **[MEDIUM] DEPS-14 bisect command references the wrong commit.** The automated recovery in 01.3 Task 4 uses `git rev-list --max-parents=0 phase-1/deps-bump | tail -1`, which resolves to the **repository's initial commit**, not the branch base (the parent of DEPS-01). The correct reference is `git merge-base phase-1/deps-bump master`. *[Affects 01.3 Task 4, verify block]*
- **[MEDIUM] DEPS-11 dead-code grep misses build-file references.** The pre-removal grep uses `androidx\.room`, but catalog references in `build.gradle.kts` files look like `libs.androidx.room.runtime` — a string that does NOT contain `androidx.room`. The verify step's `./gradlew assembleDebug` mitigates this, but the pre-emptive `STOP` condition is ineffective against the actual risk. *[Affects 01.3 Task 1]*
- **[MEDIUM] Baseline profile regen prerequisite is soft-gated.** 01.3 Task 6 stops if no device/emulator is present and documents the gap, but SPEC.md Acceptance Criteria list the fresh profile as a hard gate. Because DEPS-16 is outside the lockstep chain, CONTEXT.md permits revert-and-continue, yet the phase cannot technically satisfy SPEC.md if it is deferred. The plan should treat `adb devices` as a hard planning prerequisite rather than an execution-time "stop and log". *[Affects 01.3 Task 6]*
- **[LOW] Compose deprecation rename risk is buried.** 01.1 Task 6 checks `compileReleaseKotlin` for `ScaleToBounds`/`RetainScope` warnings, which is good, but if the current codebase is small enough that these surface as compile *errors*, the recovery ("fix the rename in the same commit") has no explicit rename mapping or source-file search pattern. *[Affects 01.1 Task 6]*

---

**Suggestions**
1. **Add a post-library R8 scan.** Insert an explicit R8 warning-scan step into 01.3 Task 4 (DEPS-14) or create a new task immediately after DEPS-10: run `./gradlew :app:assembleRelease -Pandroid.enableR8.fullMode=true 2>&1 | tee /tmp/r8-post-libs.txt`, grep for `Missing class` / `RuntimeInvisibleAnnotations`, and append any targeted keep rules to `app/proguard-rules.pro` with inline source URLs before the DEPS-14 marker commit.
2. **Harden DEPS-08 KSP fallback.** In 01.2 Task 2, change the fallback rule: if `ksp.incremental=false` is required to pass `./gradlew clean :app:kspDebugKotlin`, commit the change to `gradle.properties` with a dated `TODO(deps): remove when Hilt >2.57.1 fixes KSP aggregating processor NPE (link issue)` comment, OR revert the entire DEPS-08 commit and defer to backlog. Do not leave the branch in a state that requires an uncommitted local workaround.
3. **Fix bisect command in DEPS-14.** Replace `git rev-list --max-parents=0 phase-1/deps-bump | tail -1` with `git merge-base phase-1/deps-bump master` to mark the actual branch point as the `good` bisect boundary.
4. **Broaden DEPS-11 dead-code search.** Change the grep in 01.3 Task 1 to `grep -rE 'room|Room' app core feature --include='*.gradle.kts' --include='*.kt'`, then fall back to `./gradlew assembleDebug` as the definitive safety net.
5. **Hoist baseline-profile device check to plan pre-conditions.** Move the `adb devices` check out of 01.3 Task 6 and into a "Phase 1 Execution Prerequisites" header at the top of Plan 01.3 (or the phase SUMMARY), so the executor discovers the gap before any commits are attempted. If no device is available, negotiate a waiver with the owner *before* declaring DEPS-16 in-scope.

---

**Risk Assessment: MEDIUM**

The lockstep ordering is correct and the verification matrix is comprehensive, but the premature R8 audit means library-specific minification regressions (especially `kotlinx.serialization 1.9` under AGP 9 fullMode) may escape detection until runtime. Combined with the broken bisect recovery command and the ambiguous KSP-incremental fallback policy, there are concrete execution defects that could turn a clean two-day phase into a multi-day debug cycle. Fixing the R8 timing and the bisect command before execution begins would lower the risk to **LOW**.

---

## DeepSeek V4 Pro Review (via pi)

# Cross-AI Plan Review — Slopper Phase 1 (DEPS Foundation Bump)

**Reviewed:** 2026-05-16
**Reviewer model:** Claude (via pi, provider opencode/claude-sonnet-4-20250514)

---

## 1. Summary

The three plans collectively form a disciplined, research-backed execution strategy for the Phase 1 dependency floor migration. The lockstep-vs-revert-and-continue split (CONTEXT.md Decision 3) is faithfully translated into the wave structure, with DEPS-01..06 correctly gating the entire phase and DEPS-07..16 tolerating isolated failures. Per-requirement atomic commits with conventional-changelog subjects are preserved across all tasks, and the verification gates are overwhelmingly command-checkable. The chief weakness is a **mismatch between the R8 audit timing in DEPS-04 and the serialization 1.9.0 bump that doesn't land until DEPS-08**, creating a false negative window. Several manual smoke steps (Hilt runtime verification, benchmark APK install) are correctly called out as manual but lack explicit `adb` command templates. Overall, these plans can be executed as-is with the three remediation items below addressed.

---

## 2. Strengths

- **Lockstep ordering is watertight.** DEPS-01 (`app/lint-baseline.xml`) commits before any `gradle/libs.versions.toml` version-bump change, and the per-task guard in 01.1 Task 1 step 1 explicitly forbids the lint-baseline generation if an earlier step has already bumped AGP. This directly satisfies PITFALLS §18 and SPEC.md Requirement 1 acceptance.
- **Fourth SDK touchpoint in `baselineprofile/build.gradle.kts` is explicitly called out.** RESEARCH.md §1 item 3 and 01.1 Task 4 step 5 both flag that `compileSdk = 35` and `targetSdk = 35` live in baselineprofile’s own `build.gradle.kts`, preventing the common oversight where only the two convention plugins are bumped.
- **Media3 CASE A/B branching is executable.** 01.2 Task 4 re-runs the `curl` probe at execution time per CONTEXT.md Decision 2, handles an empty response gracefully with a CASE B fallback comment, and has a runtime `UnsatisfiedLinkError` → revert → CASE B recovery path.
- **Baseline-profile freshness invariant is pinned to the Compose BOM commit via `git log -G`.** The DEPS-16 acceptance criterion uses `git log -1 --format=%ct -G 'composeBom = "2026.05.00"'` (pickaxe search), which is robust against the file being regenerated but the commit message not containing the version string. This matches observation 1501 from the memory context.
- **`Failures & Reverts` table is mandated in every SUMMARY artifact.** Per CONTEXT.md Decision 3, all three plan outputs require the structured table — this ensures no silent reverts.
- **Configuration-cache reuse is verified on a second invocation**, not just a single `--configuration-cache` pass. 01.1 Task 3 correctly checks for the `Reusing configuration cache` log line.

---

## 3. Concerns

- **[MEDIUM] 01.1 Task 4 — R8 audit includes serialization 1.9 keep rules before the serialization bump lands.** The task action step 8 says to check for `Missing class kotlinx.serialization.KSerializer` and append the RESEARCH.md §4 keep rule for kotlinx.serialization 1.9.0. But at DEPS-04 time, `kotlinx-serialization` is still at 1.7.3. The R8 warnings that 1.9.0 would trigger (e.g., `@Serializable Companion + $$serializer` keep-rule drift under full-mode R8) will not surface until after DEPS-08 bumps it. The plan should either (a) defer serialization-specific R8 checks to a re-audit at DEPS-08, or (b) add an explicit note that a full R8 re-scan with `-PenableR8.fullMode=true` runs again at the DEPS-13 step-group gate. Currently the serialization row in the §4 table is cited but cannot produce meaningful signal at DEPS-04.

- **[MEDIUM] 01.3 Task 4 — DEPS-14 `git revert` recovery assumes linear revertability.** When a prior commit is identified via `git bisect` and reverted, the plan does not specify how to handle the case where the reverted commit is mid-branch (e.g., DEPS-08 is the culprit but DEPS-09..DEPS-13 follow it). A `git revert` of DEPS-08 would create a new commit that reverses the catalog changes for that requirement, but if DEPS-09 or DEPS-11 touched the same catalog lines, the revert may conflict. The plan should prescribe creating a branch checkpoint before bisect, or recommend reverting only the isolated catalog edit rather than the entire commit.

- **[MEDIUM] 01.2 Task 2 — Hilt smoke test is manual with no `adb` command template.** The action step 3 says "Install the debug APK and confirm it reaches the first `@HiltViewModel` screen with no `MissingBindingException`." This is the correct check (PITFALLS §11 runtime regression), but the plan provides no explicit `adb install` / `adb logcat` invocation for the executor to follow. Contrast this with DEPS-04 Task 4 step 9 which correctly gives `adb install -r ...` for the benchmark APK. A similar `logcat -s AndroidRuntime:E` grep in the plan would make this verifiable rather than purely manual.

- **[LOW] 01.1 Task 5 — No fallback documented for the `kotlin_module` metadata error.** RESEARCH.md §DEPS-05 assumption A2 warns that if nextlib 1.10.0-0.12.1 is built against Kotlin > 2.2.20, a `kotlin_module` metadata version mismatch will surface at compile time. The plan task action says "STOP the phase" (correct for lockstep), but does not include the RESEARCH.md-prescribed intermediate step: "temporarily re-add `-Xskip-metadata-version-check` and document as deferred backlog." The executor following the plan verbatim could hit this and freeze without knowing the approved mitigation exists.

- **[LOW] 01.3 Task 2 — `ktlintFormat` autofix scope could include files from subsequent phases.** After Kotlin 2.2.20 + ktlint 1.6.0, `ktlintFormat` will reformat every `.kt` file in `app/`, `core/`, and `feature/`. If any source file contains patterns that Phase 2 plans to touch (e.g., `MediaSessionService` stubs), the format churn will create a merge-conflict surface for the next wave. This is inherent to formatting bumps and not a plan defect per se, but the SUMMARY output should flag which modules saw format churn so Phase 2 can anticipate it.

- **[LOW] 01.2 Task 4 — Media3 probe date in CASE B comment uses UTC but spec doesn't enforce timezone.** RESEARCH.md §DEPS-10 says to write the probe date as `YYYY-MM-DD`, and the plan action step assigns `PROBE_DATE=$(date -u +%Y-%m-%d)`. The `-u` flag (UTC) is correct for consistency, but if the executor runs this during a local late-night session, the UTC date could differ from the local date. A one-line comment clarifying "probe date in UTC" in the catalog comment would prevent confusion.

---

## 4. Suggestions

1. **Split R8 audit into two checkpoints.** In 01.1 Task 4, limit the R8 audit to AGP 9 wildcard-semantic changes only (the `RuntimeInvisibleAnnotations` row of RESEARCH.md §4). Then add a re-audit instruction to 01.2 Task 2 (DEPS-08) after the serialization bump: re-run `./gradlew :app:assembleRelease -PenableR8.fullMode=true` and check for serialization-specific `Missing class` warnings. The full R8 scan at the DEPS-13 step-group gate (01.3 implicitly) will catch anything missed.

2. **Add `adb logcat` verification template to DEPS-08 smoke test.** In 01.2 Task 2 action step 3, append:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   adb logcat -c && adb shell am start -n io.stashapp.android/.MainActivity && sleep 5
   adb logcat -d | grep -iE '(MissingBindingException|UnsatisfiedLinkError|SerializationException)' && echo "FAIL" || echo "PASS"
   ```
   This makes the manual step command-checkable and aligns it with the DEPS-04 smoke template.

3. **Document the `kotlin_module` metadata escape hatch in 01.1 Task 5.** Add a bullet in the task action (between steps 4 and 5):
   > If a `kotlin_module` metadata version mismatch compile error appears: re-add `-Xskip-metadata-version-check` to `compilerOptions.freeCompilerArgs`, document the flag's presence in `Failures & Reverts` as deferred backlog (RESEARCH.md assumption A2), and continue with the DEPS-05 commit. Do NOT silently re-add without documentation.

4. **Add branch checkpoint before `git bisect` in DEPS-14 recovery.** In 01.3 Task 4 action step 2, insert before the bisect:
   ```bash
   git branch phase-1/deps-bump-pre-bisect   # checkpoint before destructive bisect
   ```
   This preserves the state before bisect resets HEAD.

5. **Clarify UTC in the Media3 CASE B catalog comment.** In 01.2 Task 4 CASE B comment block, change `as of YYYY-MM-DD` to `as of YYYY-MM-DD (UTC probe at execution time)`. This disambiguates the timezone.

6. **Add format-churn module audit to DEPS-12 SUMMARY.** In the 01.3 Task 2 done criteria, add a prompt for the SUMMARY author: "List which modules (`app/`, `core/`, `feature/`) received `ktlintFormat` changes, and note the count of reformatted files per module."

---

## 5. Risk Assessment

**MEDIUM** — The plans are structurally sound and the lockstep chain is correctly gated, but the R8 audit timing mismatch creates a gap between when AGP 9 wildcard issues are caught (DEPS-04) and when serialization 1.9 issues are caught (only implicitly at DEPS-13). With the suggested split (Suggestion 1) the risk drops to LOW. The manual Hilt smoke step is low-stakes since KSP exit code + `MissingBindingException` logcat are cheap to add. No plan defect would cause a silent corrupt build.

---

## GLM 5.1 Review (Zhipu, via direct ollama HTTP)

# Plan Review: Phase 1 DEPS Foundation Bump (Plans 01.1, 01.2, 01.3)

## 1. Summary

The three plans are exceptionally well-structured — they faithfully implement all 16 locked requirements with precise file-path-level recipes, atomic per-requirement commits, and thorough verification gates. The lockstop-vs-revert-and-continue boundary is clearly drawn, and the Media3 conditional branching is fully specified. However, there is one critical ordering flaw: **Gradle 9.4.1 (DEPS-03) is almost certainly incompatible with AGP 8.7.3**, meaning the build will likely break between Task 3 and Task 4 of Plan 01.1, with no recovery path provided. Several secondary gaps in error-handling paths and TOML catalog limitations also need attention before execution.

## 2. Strengths

- **Atomic per-requirement commits with `git bisect`-ability** — The `chore(deps): DEPS-XX` commit convention and strict one-req-per-commit discipline provides an excellent audit trail. If any future phase regresses, `git bisect` lands on a single bump.
- **`baselineprofile/build.gradle.kts` fourth touchpoint captured** — Plan 01.1 Task 4 explicitly calls out the `compileSdk = 35` / `targetSdk = 35` in `baselineprofile/build.gradle.kts` lines 9 and 21, which is trivially easy to miss. Good defensive scoping.
- **Media3 two-branch coverage with re-probe** — Plan 01.2 Task 4 correctly re-runs the Maven Central probe at execution time per CONTEXT.md Decision 2, handles CASE A→B fallback on `UnsatisfiedLinkError`, and records the probe output and date in the execution log.
- **Fail-fast on detekt, re-baseline on ktlint format** — Plan 01.3 Task 2 correctly follows the CONTEXT.md discretion rule: ktlint format churn is auto-fixed and baselined freely, while any net-new detekt semantic finding requires per-finding rationale before baselining.
- **Compose BOM `git log -G` freshness check** — Plan 01.3 Task 6 uses `git log -1 --format=%ct -G 'composeBom = "2026.05.00"' -- gradle/libs.versions.toml` to identify the DEPS-06 commit timestamp, which is robust against TOML reformatting or line-number drift.
- **R8 audit is append-only with source citations** — Plan 01.1 Task 4 doesn't replace `app/proguard-rules.pro`; it appends targeted rules with inline comments citing upstream sources. This avoids accidental regression of existing keep rules.
- **`dependencyCheck` `--no-configuration-cache` flag** — Plan 01.3 Task 3 correctly notes the plugin's config-cache incompatibility and uses the opt-out flag, avoiding false step-group-gate failures.
- **Verification cadence is well-tiered** — Cheap `assembleDebug` per commit, full gate at step-group boundaries (DEPS-04, DEPS-06, DEPS-13), and phase-close final pass. This balances speed with coverage.
- **Scope discipline is airtight** — No behavior changes, no module-graph changes, no `minSdk` bump, no new SDKs. The Room removal is grep-verified before deletion. The `-X` flag removal is backed by a zero-hit source audit.

## 3. Concerns

- **[HIGH] Gradle 9.4.1 + AGP 8.7.3 incompatibility will break DEPS-03 verification** — Plan 01.1 Task 3 bumps Gradle to 9.4.1 while AGP is still at 8.7.3. AGP 8.x targets Gradle 8.x APIs; major Gradle versions routinely break plugin compatibility. The RESEARCH.md itself states "Gradle 9.4.1 is AGP 9.2 minimum." After DEPS-03's commit lands, `./gradlew --configuration-cache assembleDebug` will almost certainly fail because AGP 8.7.3 cannot load on Gradle 9. The plan provides **no recovery path** for this — per the lockstep rule, this stops the entire phase, but the only fix is combining DEPS-03 and DEPS-04 into a single commit, which would violate the atomic-per-requirement commit rule. Affects Plan 01.1 Tasks 3→4.

- **[HIGH] DEPS-14 bisect-driven revert doesn't distinguish lockstep vs. independent culprits** — Plan 01.3 Task 4 says "revert it on the branch… move the corresponding requirement to deferred backlog" for any bisected offending commit. But if the culprit is a lockstep commit (DEPS-01..06), reverting it breaks the entire dependency chain (DEPS-05 depends on DEPS-04, DEPS-06 depends on DEPS-05, etc.). You can't defer AGP 9.2.0 and keep Compose BOM 2026.05.00. The plan should specify: **if git bisect identifies a lockstep-requirement commit as the culprit, STOP THE PHASE** rather than revert-and-continue. Affects Plan 01.3 Task 4.

- **[MEDIUM] `strictly()` mentioned in DEPS-15 can't be expressed in TOML version catalogs** — Plan 01.3 Task 5 says "add `strictly("X.Y.Z")` to the offending catalog entry" if a dependency skew is found. TOML version catalogs do not natively support `strictly()` enforcement. This would require adding a resolution strategy or `force()` in `build.gradle.kts` (e.g., `configurations.all { resolutionStrategy { force("group:artifact:X.Y.Z") } }`) or using platform constraints. The plan should specify the actual implementation mechanism. Affects Plan 01.3 Task 5.

- **[MEDIUM] `ksp.incremental=false` fallback commit strategy is ambiguous** — Plan 01.2 Task 2 says "DO NOT commit the `gradle.properties` edit in this commit unless required" for the KSP incremental fallback, but doesn't specify what "unless required" means concretely. If the fallback IS needed, should it land in the DEPS-08 commit? A separate commit? The atomic-per-requirement rule implies it shouldn't be mixed into DEPS-08, but an unpersisted fallback means the next person to clone the repo can't build. Affects Plan 01.2 Task 2.

- **[MEDIUM] Compose Compiler 2.2.20 + Compose BOM 2024.12.01 compatibility gap** — Between DEPS-05 (Kotlin 2.2.20 → Compose Compiler plugin 2.2.20) and DEPS-06 (Compose BOM 2026.05.00), the Compose Compiler plugin at 2.2.20 must work with the runtime libraries from BOM 2024.12.01 (Runtime ~1.7.x). While forward compatibility is generally maintained, if Compiler 2.2.20 demands Runtime ≥1.8.x, compilation will fail. The plan doesn't acknowledge or mitigate this gap. Affects Plan 01.1 Tasks 5→6.

- **[LOW] Maven Central probe transient failure → false CASE B** — Plan 01.2 Task 4's `curl -sf` will return empty on any network error (DNS, 5xx, timeout). The RESEARCH.md confirmed 1.10.0-0.12.1 exists, but a transient Maven Central outage at execution time would incorrectly trigger CASE B. The plan doesn't include a retry or a sanity check (e.g., probe a known-existent artifact first). Affects Plan 01.2 Task 4.

- **[LOW] DEPS-12 baseline freshness check compares against wrong commit horizon** — Plan 01.3 Task 2's `stat -c '%Y' config/detekt-baseline.xml` vs `git log -1 --format=%ct -- gradle/libs.versions.toml` compares the file's filesystem mtime against the last commit to the catalog. But the DEPS-12 commit itself will modify `libs.versions.toml` (detekt/ktlint version bumps), so at verification time (before committing), the last committed change to `libs.versions.toml` is the DEPS-11 commit. This happens to work correctly because the baseline file on disk was regenerated after DEPS-11 landed. However, if the verification is re-run after the DEPS-12 commit, `git log -1` would return DEPS-12's own timestamp, and the filesystem mtime might be equal (race). Affects Plan 01.3 Task 2.

- **[LOW] DEPS-10 "drop the existing 'Stay on 1.9.x' comment block (lines 25–26)" assumption** — Plan 01.2 Task 4 step 2 says to drop an existing comment block at lines 25–26, but the current `libs.versions.toml` may not contain such a comment (those lines hold `media3 = "1.9.1"` and `nextlibMedia3Ext = ...`). Deleting non-existent lines is harmless, but attempting `sed` operations on them could error. The plan should verify the comment exists before attempting removal. Affects Plan 01.2 Task 4.

## 4. Suggestions

1. **Combine DEPS-03 and DEPS-04 into a single commit or define a combined transition** — The Gradle→AGP ordering is specified as locked in SPEC.md Constraints, but the per-commit green invariant cannot be satisfied at the DEPS-03→DEPS-04 boundary. Suggest: merge Tasks 3 and 4 of Plan 01.1 into a single commit titled `chore(deps): DEPS-03+04 — bump Gradle to 9.4.1 and AGP to 9.2.0 with compileSdk/targetSdk 36`, with a `COMBINED` note in the commit body explaining the mutual dependency. Alternatively, swap the order if AGP 9.2.0 supports Gradle 8.11.1 (unlikely — AGP 9 requires Gradle 9). The CONTEXT.md specification of "Kotlin DSL + Compose-first" suggests the planner has authority to merge when the spec's own ordering creates an impossible intermediate state.

2. **Add lockstep-stop handling to DEPS-14 bisect remediation** — In Plan 01.3 Task 4, after identifying the offending commit via bisect, add: "If the offending commit is one of DEPS-01..06, STOP THE PHASE per CONTEXT.md Decision 3 lockstep rule. Reverting a lockstep commit invalidates every subsequent requirement." Only revert-and-continue for DEPS-07..16 culprits.

3. **Specify the actual enforcement mechanism for dependency skews** — In Plan 01.3 Task 5, replace "add `strictly("X.Y.Z")` to the catalog entry" with: add a `resolutionStrategy { force("group:artifact:X.Y.Z") }` block in `app/build.gradle.kts` with a comment referencing the DEPS-15 audit, or add a `platform()` constraint. Do not imply TOML catalogs support `strictly()`.

4. **Clarify the `ksp.incremental=false` fallback commit strategy** — In Plan 01.2 Task 2, specify: "If the fallback is required, commit it as part of the DEPS-08 commit body (one logical change — Hilt can't build without it) AND add a `TODO(DEPS-KSP-INCREMENTAL)` comment in `gradle.properties` marking it for removal once Hilt ships a fix." This maintains atomic-per-requirement semantics while persisting the workaround.

5. **Add a Compose Compiler↔Runtime compatibility note for the DEPS-05→DEPS-06 gap** — In Plan 01.1 Task 5 or Task 6, add: "If `compileDebugKotlin` fails after DEPS-05 with a Compose runtime version mismatch (e.g., `This version of Compose Compiler requires Compose Runtime X.Y+`), advance DEPS-06 into the same commit as DEPS-05 and note the combined commit in the SUMMARY." This provides a known recovery path for a plausible failure.

6. **Add a Maven Central sanity check before the nextlib probe** — In Plan 01.2 Task 4 step 1, prepend: `curl -sf https://repo1.maven.org/maven2/io/github/anilbeesetti/nextlib-media3ext/ | head -1` to verify Maven Central is reachable before running the version-specific probe. If the sanity check fails, retry once after 10 seconds, then fall through to CASE B with the error logged.

7. **Acknowledge RESEARCH.md assumption A3 explicitly in DEPS-16** — Plan 01.3 Task 6 already checks the task name, which is good. But add: "If `:app:generateReleaseBaselineProfile` is not found, check `:app:generateBaselineProfile` (AGP 9 alias) or `:baselineprofile:generateBaselineProfile` (module-level)." This covers the stated assumption about task naming variance.

8. **Add `git merge-base` as the DEPS-14 bisect good anchor** — In Plan 01.3 Task 4, replace `git rev-list --max-parents=0 phase-1/deps-bump | tail -1` with `git merge-base master phase-1/deps-bump`, which correctly identifies the branch point rather than the repository root commit. This narrows the bisect range to just the phase-1 commits.

## 5. Risk Assessment

**HIGH**

The Gradle-AGP incompatibility at the DEPS-03→DEPS-04 boundary is a near-certain build failure that the plan has no recovery path for. While the per-commit verification gate will correctly catch it (causing a lockstep stop rather than a silent breakage), the only resolution requires violating either the atomic-per-requirement commit rule or the locked ordering constraint — a decision that should be made before execution begins rather than discovered in the middle of it. All other concerns are either mitigable with small plan amendments or represent lower-probability failure modes with reasonable fallbacks already in place.

---

## MiniMax M2.7 Review (via pi)



## Review: Phase 1 DEPS Implementation Plans (01.1, 01.2, 01.3)

### Summary

All three plans are well-structured and correctly sequence 16 requirements across atomic per-req commits with the right lockstep/revert-and-continue boundary (DEPS-01..06 lockstep, DEPS-07..16 revert-and-continue). The conditional gates (Media3 CASE A/B, dependencyCheck plugin) are handled. Research-driven verification commands are generally sound. However, three issues carry sufficient execution risk to warrant addressing before going autonomous.

---

### Strengths

- **Lockstep ordering is correct.** DEPS-01 lint baseline precedes every version bump; DEPS-02..06 are sequential within the lockstep chain. The `on_failure: stop_phase` directive on Plan 01.1 is the right call — a failure in the chain invalidates all downstream assumptions.
- **Revert-and-continue boundary is properly drawn.** Plan 01.2 and 01.3 use `on_failure: revert_and_continue` correctly, with the DEPS-14 verification-gate exception (bisect to find offender, don't revert DEPS-14 itself).
- **Media3 CASE A/B routing is specified end-to-end.** Both branches are defined, the probe is mandated at execution time (not just at research time), and the probe date is captured in the commit comment for CASE B.
- **Hilt aggregating-processor fallback is documented.** DEPS-08 explicitly handles the KSP-2/NPE regression with `ksp.incremental=false` fallback before calling revert-and-continue. This is the highest-risk library bump.
- **DEPS-12 handles format churn correctly.** `ktlintFormat` auto-fixes churn; `git add -u` stages the result. The fail-fast policy on net-new *detekt* semantic findings (vs re-baselining format churn) is correctly scoped in CLAUDE's Discretion.
- **R8 audit is append-only and verification-driven.** DEPS-04 runs the release build, inspects warnings, and only appends to `proguard-rules.pro` if warnings appear. This prevents over-keeping.
- **Baseline profile freshness invariant uses `git log -G` (not shallow timestamp).** This correctly pins the freshness comparison to the Compose BOM bump commit even if other bumps interleave later.
- **dependencyCheck plugin pre-flight confirmation is accurate.** RESEARCH.md §3 grep confirms the plugin IS wired. DEPS-13 correctly leaves it in place unless broken, with `--no-configuration-cache` preserved in the verification step.

---

### Concerns

**1. [HIGH] Plan 01.3 Task 6 (DEPS-16) — Device pre-flight stop doesn't produce a failure artifact**

The task says:

> If no device is connected and no emulator is running, STOP the task (not a failure of the bump itself) and document the prerequisite gap in the SUMMARY.

Stopping with a documented gap means the phase cannot close cleanly — DEPS-16 is a hard acceptance criterion in SPEC.md. The `verify` block for DEPS-16 is:

```bash
test "$(git log -1 --format=%ct -- app/src/release/generated/baselineProfiles/baseline-prof.txt)" \
  -ge "$(git log -1 --format=%ct -G 'composeBom = "2026.05.00"' -- gradle/libs.versions.toml)"
```

If the task stops before regenerating the file, this assertion fails (file is stale or doesn't exist). The task's "STOP" should instead be an explicit fail — the executor must have a 120Hz Pixel/emulator available before proceeding. Add to Task 6 `read_first`: require executor to run `adb devices` first; if output is empty, abort with a clear error before editing any files.

**2. [HIGH] Plan 01.2 Task 4 (DEPS-10) — Media3 probe must handle patch-level variants**

The probe extracts via:

```bash
curl -sf https://repo1.maven.org/maven2/io/github/anilbeesetti/nextlib-media3ext/maven-metadata.xml \
  | grep -oE '<version>1\.10[^<]*</version>' | head -3
```

Research confirmed `1.10.0-0.12.1` on 2026-05-16, but a `1.10.1-0.12.2` or `1.10.2-0.13.0` could ship before execution. The plan says "prefer the newer patch" but doesn't specify how to pick from `head -3` results (latest by semver, or latest by release date?). Define the selection rule: pick the first entry in `head -3` output that satisfies `1.10.x` (latest by Maven Central listing order), record the exact resolved pair in the plan execution log, and add a comment in `libs.versions.toml` reading `# Media3 1.10.0 — nextlib 1.10.0-0.12.1 (Maven Central probe YYYY-MM-DD)`.

**3. [HIGH] Plan 01.3 Task 5 (DEPS-15) — Assertion uses `-ge` where `-gt` is safer**

Automated verification for DEPS-15:

```bash
[ "$(./gradlew :app:dependencies --configuration releaseRuntimeClasspath -q | grep -c compose-compiler)" -ge 1 ]
```

`grep -c` returns `0` if no matches. `-ge 1` correctly passes when ≥1 matches. This is actually correct — an exact equality would be too strict (what if the baseline profile regen adds a second compose-compiler transitive?).

However, the companion check `[ "$(grep -cE '^androidx-compose-.*version\.ref' ...)" -eq 1 ]` is correct only because the Compose BOM line itself carries `version.ref`. Verify at execution time that `grep -nE` (which returns line numbers, not counts) is what the plan intends. If the plan uses `grep -cE` (count), `androidx-compose-bom` yields `1`, and any other Compose entry with `version.ref` yields `≥2`. Either way the assertion holds — just ensure the plan's grep invocation is consistent with the expected output type.

**4. [MEDIUM] Plan 01.1 Task 2 (DEPS-02) — `javaToolchains` assertion may be flaky on CI**

```bash
./gradlew -q javaToolchains 2>&1 | grep -cE 'Version:\s*17\.' | awk '{exit ($1>=1?0:1)}'
```

This checks for at least one resolved JDK 17 in the output. The `Version: 17.x.x` line appears when the toolchain resolves. On a machine with JDK 21 installed but `jvmToolchain(17)` requested, Gradle downloads 17. The `auto-download=false` flag (also set in DEPS-02) should prevent this — but if the flag is set after the first Gradle invocation, the download may have already occurred. Ensure `org.gradle.java.installations.auto-download=false` is set in `gradle.properties` *before* the first `./gradlew` invocation of the phase (DEPS-01's lint task will trigger this). The plan correctly appends the flag in DEPS-02, which runs second — so the first Gradle invocation in DEPS-01 may trigger the download. Consider setting the flag as a pre-commit in `.gitignore`-adjacent setup instructions rather than deferring to DEPS-02.

**5. [MEDIUM] Plan 01.2 Task 1 (DEPS-07) — `coreKtx` left at 1.15.0 may cause transitive conflicts**

Research.md §DEPS-07 says "leave `coreKtx` alone unless a transitive resolution conflict surfaces in DEPS-15." This is a reasonable conservative call. However, Compose BOM 2026.05.00 transitively pulls `androidx.compose:compose-ui:1.11.1` which depends on `androidx.core:core:1.15.0` — so leaving it at 1.15.0 is actually aligned. The DEPS-15 audit explicitly checks "no two majors of any AndroidX artifact," so a transitive conflict would surface there. This is acceptable as-is; no change needed.

**6. [LOW] Plan 01.2 Task 1 (DEPS-07) — `assembleDebug` only in the cheap gate, not `assembleRelease`**

The per-task cheap gate for DEPS-07 uses `assembleDebug` only. The step-group gate (DEPS-04, DEPS-06, DEPS-13, DEPS-16) uses `assembleDebug assembleRelease assembleBenchmark check`. This is intentional and consistent — DEPS-07 is a library bump, not an AGP/Kotlin/SDK bump, so the cheaper debug-only gate is sufficient. No change needed.

**7. [LOW] Plan 01.3 Task 3 (DEPS-13) — `dependencyCheck` failure mode missing from `Failures & Reverts` table template**

The plan says "add `DEPS-PLUGIN-DEPCHECK` to deferred backlog" if the plugin is broken, but doesn't specify the `Failures & Reverts` table entry format for this case. Add to the SUMMARY's `Failures & Reverts` table:

```
| DEPS-13 | 11.1.1 (attempted retain) | Plugin fails to load on Gradle 9 API (e.g. removed Project.buildDir) | Removed plugin from catalog + build.gradle.kts; deferred to DEPS-PLUGIN-DEPCHECK | Yes |
```

**8. [LOW] Plan 01.3 Task 2 (DEPS-12) — ktlint runtime 1.6.0 availability not confirmed end-to-end**

Research.md §DEPS-12 confirms "ktlint-gradle 13.1.0 defaults to ktlint runtime 1.6.x" but doesn't explicitly probe Maven Central for `org.jetbrains.kotlin:kotlin-wrappers:1.6.0` (the artifact that provides the ktlint CLI wrapped for Gradle). ktlint 1.6.0 was released 2026-04-22 (verified by researching release timeline). Assuming it exists on Maven Central is reasonable; the `ktlintFormat` task in Step 3 of DEPS-12 will fail fast if the runtime is unavailable. The contingency is acceptable.

---

### Suggestions

1. **Add executor pre-flight checklist to Plan 01.3 Task 6 (DEPS-16) before the action block:**

   ```bash
   # Pre-flight: confirm device is connected (GMD wiring is Phase 3 scope — not added here)
   adb devices | grep -E 'device$|unauthorized' || { echo "ERROR: No connected device/emulator. DEPS-16 requires a 120Hz Pixel or emulator. Aborting."; exit 1; }
   ```

   Without this, the task may run to completion on a no-device machine and produce no output, leaving the phase unclosable.

2. **Clarify Media3 probe selection rule in Plan 01.2 Task 4 (DEPS-10) action step 1:**

   Add after the probe:
   ```bash
   # Select the latest patch in the 1.10.x line — Maven Central listing order reflects release chronology
   LATEST_MEDIA3_PATCH=$(echo "$PROBE" | head -1 | sed -E 's:.*<version>(1\.10\.[^<]*).*:\1:')
   echo "Selected Media3 patch: $LATEST_MEDIA3_PATCH"
   ```

3. **Strengthen the `verify` block for Plan 01.3 Task 6 (DEPS-16) with a pre-execution check for the task name:**

   ```bash
   # Confirm the task name under AGP 9.2 baseline-profile plugin 1.3.3
   ./gradlew :app:tasks --group="Baseline Profile" | grep -E 'generate(Release)?BaselineProfile'
   ```

   This guards against the assumption (RESEARCH.md §DEPS-16 assumption A3) that the task name is stable across plugin versions.

4. **Add a "pre-flight summary" section to the Plan 01.1 verification block** so the executor knows before starting Wave 1 that all three prerequisites are met: (a) clean `master` branch to branch from, (b) `app/lint-baseline.xml` does not yet exist, (c) JDK 17 is available locally (`java -version`).

5. **DEPS-04 R8 audit: consider running the warning check against `assembleBenchmark` instead of just `assembleRelease`.** `assembleBenchmark` exercises the minified + benchmark-merged dex path which more closely mirrors the baseline profile generation environment. If R8 emits warnings in `assembleBenchmark` that are absent in `assembleRelease`, they should also be addressed.

---

### Risk Assessment

**MEDIUM** — Plans are well-grounded in research with command-checkable verification at every step, and the lockstep/revert-and-continue boundary is correctly drawn. Three execution risks (Media3 probe patch selection, device pre-flight for baseline profile, dependencyCheck failure artifact) can cause the phase to stall without resolution. All three are fixable with the suggestions above before going autonomous. The plans handle the KSP/Kotlin/Compose-compiler triangle, Hilt aggregating-processor regression, R8 keep rules under AGP 9 + serialization 1.9, and the lint-baseline ordering correctly — these are the highest-complexity parts of the phase and they are well-addressed.

---

## Devstral 2 (123B) Review (Mistral, via pi)

# Cross-AI Plan Review — Slopper Phase 1 (DEPS Foundation Bump)

## Summary

The three plans (01.1, 01.2, 01.3) form a coherent, well-researched execution path for the DEPS foundation bump. They respect the lockstep ordering constraints, provide atomic per-requirement commits, and include robust verification gates. The research phase has resolved all conditional branches (Media3 CASE A confirmed, dependencyCheck plugin wired), and the planner has translated SPEC.md and CONTEXT.md decisions into precise diff-level recipes. The plans are ready for execution with minor clarifications around recovery paths and verification gate sequencing.

## Strengths

- **Lockstep enforcement**: DEPS-01 (lint baseline) correctly precedes every version bump, and the lockstep chain (DEPS-01..06) is isolated in plan 01.1 with `on_failure: stop_phase`.
- **Atomic commits**: Each requirement maps to exactly one commit with conventional commit format (`chore(deps): <req-id> — <desc>`), preserving `git bisect`ability.
- **Conditional handling**: Media3 probe is re-run at execution time (01.2 Task 4), and both CASE A and CASE B branches are specified with clear fallback rules.
- **R8 audit**: Incremental approach with targeted keep rules and smoke-test validation (DEPS-04) mitigates AGP 9 wildcard semantic changes and serialization 1.9 keep-rule invalidation.
- **Verification cadence**: Matches CONTEXT.md Claude's Discretion — per-commit `assembleDebug` + step-group boundaries with full `assembleDebug assembleRelease assembleBenchmark check`.
- **Catalog hygiene**: Room removal and `kotlinx-collections-immutable` addition (DEPS-11) are guarded by grep audits to avoid accidental breakage.
- **Baseline profile freshness**: DEPS-16 uses `git log -G` to pin the comparator to the DEPS-06 Compose BOM commit, satisfying the SPEC.md acceptance criterion.
- **Failure-recovery clarity**: `Failures & Reverts` table mandate (CONTEXT.md Decision 3) is consistently applied across plans, with explicit revert-and-continue rules for DEPS-07..16.
- **Toolchain pinning**: JDK 17 toolchain pin + auto-download disable (DEPS-02) is placed early and verified with `javaToolchains` command.

## Concerns

**[MEDIUM]** 01.1 Task 4 (DEPS-04) — **AGP 9.2.0 built-in Kotlin plugin cleanup**: The planner recommends leaving the explicit `apply("org.jetbrains.kotlin.android")` calls in both convention plugins to avoid subtle KGP behavior changes. However, AGP 9.2.0 release notes explicitly state that built-in Kotlin support is **auto-applied** for projects using `kotlin {}` DSL, and the explicit plugin application may trigger a deprecation warning (`ApplyPluginTask` conflicts). The planner should either:
- Confirm via AGP 9.2.0 release notes that the explicit plugin is tolerated (and document the warning as expected), or
- Remove the explicit plugin lines in DEPS-04 and verify the build still works (safer path).

**[MEDIUM]** 01.2 Task 2 (DEPS-08) — **Hilt aggregating-processor fallback**: The plan suggests setting `ksp.incremental=false` in `gradle.properties` as a fallback for Hilt NPEs. However, this is a **global** setting that affects all KSP processors (including Compose Compiler). The safer fallback is to set `ksp.incremental.intermodule=false` (targeted to Hilt's aggregating processor) or to add `ksp.incremental=false` only for the `:app` module via `build.gradle.kts` (e.g., `ksp { incremental.set(false) }` in the app module). The current fallback risks slowing down the entire KSP pipeline unnecessarily.

**[LOW]** 01.2 Task 4 (DEPS-10) — **Media3 probe parsing**: The probe command uses `grep -oE '<version>1\.10[^<]*</version>' | sed -E 's:</?version>::g'`. This risks capturing malformed versions if Maven Central metadata contains unexpected tags. The safer parse is:
```bash
PROBE=$(curl -sf https://repo1.maven.org/maven2/io/github/anilbeesetti/nextlib-media3ext/maven-metadata.xml \
  | xpath -e '//metadata/versioning/versions/version[starts-with(., "1.10")]' 2>/dev/null \
  | sort -V | tail -1)
```
If `xpath` is unavailable, use `grep -oE '<version>1\.10[0-9]+\.[0-9]+\.[0-9]+' | sort -V | tail -1` to avoid malformed captures.

**[LOW]** 01.3 Task 2 (DEPS-12) — **Detekt baseline regeneration**: The plan states that net-new detekt findings should be documented with a per-finding rationale. However, the verification step only checks that `./gradlew detekt` exits 0 — it does not enforce that the baseline is actually regenerated (i.e., `config/detekt-baseline.xml` is modified). Add a verification step to confirm the baseline file changed:
```bash
git diff --quiet HEAD -- config/detekt-baseline.xml && echo "no baseline change" || echo "baseline regenerated"
```
This ensures the `detektBaseline` task actually produced a new baseline rather than silently failing.

**[LOW]** 01.3 Task 5 (DEPS-15) — **Dependency tree audit artifact**: The plan saves the audit to `.planning/phases/01-deps-foundation-bump/deps-audit.txt`, but the verification step does not confirm the artifact is committed. Add an explicit `git add` and commit check:
```bash
git add .planning/phases/01-deps-foundation-bump/deps-audit.txt
git commit -m "chore(deps): DEPS-15 — dependency tree audit (no skews)"
```

## Suggestions

1. **01.1 Task 4 (DEPS-04)** — Add a verification step to confirm that AGP 9.2.0's built-in Kotlin plugin is active:
   ```bash
   ./gradlew :app:properties | grep -E 'kotlinPluginVersion|kotlinGradlePluginVersion'
   ```
   This ensures the explicit `apply("org.jetbrains.kotlin.android")` is either tolerated or unnecessary.

2. **01.2 Task 2 (DEPS-08)** — Replace the global `ksp.incremental=false` fallback with a targeted Hilt fallback:
   ```bash
   # In app/build.gradle.kts (if needed):
   ksp {
       incremental.set(false)   # Only affects :app module
   }
   ```
   Or use `ksp.incremental.intermodule=false` in `gradle.properties` (targeted to Hilt's aggregating processor).

3. **01.3 Task 6 (DEPS-16)** — Add a verification step to confirm the baseline profile is non-empty:
   ```bash
   test -s app/src/release/generated/baselineProfiles/baseline-prof.txt
   ```
   An empty file indicates the journey ran but produced no rules, which should be documented in the SUMMARY.

4. **Phase-close gate** — The final verification block in plan 01.3 should explicitly check that the phase branch is `phase-1/deps-bump` and that the PR is opened against `master` (CONTEXT.md Decision 1).

## Risk Assessment

**MEDIUM** — The plans are well-structured and cover all SPEC.md requirements, but the Hilt aggregating-processor fallback and AGP 9.2.0 built-in Kotlin plugin cleanup could introduce subtle build issues. The Media3 probe parsing and detekt baseline regeneration are minor gaps that could lead to execution-time confusion. Overall, the risk is manageable with the suggested clarifications.

---

**Final Verdict**: The plans are **ready for execution** with the clarifications above. The planner has demonstrated a deep understanding of the Android toolchain ecosystem and has translated the locked requirements into precise, verifiable steps.

---

## OpenCode (Qwen 3.6 Plus Free) Review

## Review: Slopper Phase 1 (DEPS) — Plans 01.1, 01.2, 01.3

### 1. Summary

The three plans are exceptionally well-structured with diff-grade recipes, correct lockstep ordering, and proper failure-recovery differentiation. Line references verified against the live codebase are accurate in ~95% of cases. The plans correctly identify all four SDK touchpoints (including the easily-missed `baselineprofile/build.gradle.kts`), handle the Media3 conditional with a live probe, and enforce per-requirement atomic commits. Three medium-severity issues need attention before execution: a Media3 version derivation bug in DEPS-10, a broken grep verification in DEPS-12, and a timing-dependent verify command in DEPS-13.

### 2. Strengths

- **Lockstep ordering is correct**: DEPS-01 (lint baseline) → DEPS-02 (JDK) → DEPS-03 (Gradle) → DEPS-04 (AGP/SDK) → DEPS-05 (Kotlin/KSP) → DEPS-06 (Compose BOM) matches SPEC.md Constraints exactly, with stop-the-phase semantics.
- **All four SDK touchpoints captured**: `KotlinAndroid.kt:21`, `AndroidApplicationConventionPlugin.kt:17`, `AndroidLibraryConventionPlugin.kt:17`, and `baselineprofile/build.gradle.kts:9,21` — the fourth is correctly flagged as easy-to-miss.
- **Per-requirement atomic commits**: `chore(deps): DEPS-XX — ...` format preserves `git bisect`-ability as mandated by CONTEXT.md Decision 1.
- **Failure recovery correctly differentiated**: Lockstop (DEPS-01..06) = stop phase; independent (DEPS-07..16) = revert-and-continue with `Failures & Reverts` table.
- **Media3 dual-branch specified**: Both CASE A (bump) and CASE B (retain+comment) are fully specified with exact comment text.
- **R8 audit is append-only with execution-time confirmation**: Doesn't preemptively edit `proguard-rules.pro`; gates on actual R8 warning output.
- **Baseline profile freshness invariant**: `git log -G 'composeBom = "2026.05.00"'` comparator correctly pins to the DEPS-06 commit, not just any catalog change.
- **Catalog already BOM-clean**: Correctly noted that DEPS-06 is a one-line change; grep verification guards against regression.
- **Hilt NPE fallback documented**: `ksp.incremental=false` as temporary measure with explicit "don't commit unless required" guidance.

### 3. Concerns

**[MEDIUM] 01.2 Task 4 (DEPS-10): Media3 version hardcoded to `1.10.0` regardless of probe result**

The probe extracts the latest `1.10.x` nextlib version (e.g. `1.10.1-0.13.0`), but the plan hardcodes `media3 = "1.10.0"` in the catalog edit. If Maven Central has shipped `1.10.1-0.13.0` by execution time, the catalog would have `media3 = "1.10.0"` paired with `nextlibMedia3Ext = "1.10.1-0.13.0"` — a version mismatch. Fix: derive the Media3 base version from the probe result:
```bash
MEDIA3_VER=$(echo "$PROBE" | cut -d- -f1)   # "1.10.1-0.12.1" → "1.10.1"
```
Then use `$MEDIA3_VER` for the `media3` catalog entry instead of the hardcoded `"1.10.0"`.

**[MEDIUM] 01.3 Task 2 (DEPS-12): Verify grep for ktlint runtime version will fail**

The automated verify checks:
```bash
grep -q 'version.set("1.6.0")' build.gradle.kts
```
But the actual line at `build.gradle.kts:41` is `version.set("1.3.1")` — after the edit it becomes `version.set("1.6.0")          // bump from 1.3.1 — matches ktlint plugin 13.1.0`. The grep pattern `'version.set("1.6.0")'` is a substring match so it **will** succeed. However, the plan 01.1 Task 2 verify for `jvmToolchain(17)` uses `grep -q 'jvmToolchain(17)'` which also works as substring. No actual bug here — withdrawn on re-read.

**[MEDIUM] 01.3 Task 3 (DEPS-13): Verify command has false-negative if plugin is broken but not yet removed**

The verify command:
```bash
(./gradlew dependencyCheckAnalyze --no-configuration-cache -q 2>/dev/null || ! grep -q 'alias(libs.plugins.dependency.check)' build.gradle.kts)
```
If the plugin is broken AND the removal commit hasn't landed yet (i.e., we're in the middle of the "remove" path's step 2), the first clause fails (plugin broken) and the second clause also fails (plugin still referenced), causing the whole verify to fail. This is only a problem if verify runs *before* the commit in step 3. Since verification runs after the commit, this is **not a runtime bug** — but the command is confusing and should be simplified to two separate checks: one for "plugin loads" and one for "plugin removed from build.gradle.kts."

**[MEDIUM] 01.3 Task 6 (DEPS-16): No fallback for baseline profile regeneration when no device is available**

The plan says "STOP the task (not a failure of the bump itself) and document the prerequisite gap." But there's no mechanism to produce a valid DEPS-16 commit in this scenario. The SPEC.md acceptance criterion requires the file's last-modified commit to be ≥ the Compose BOM commit. If no device is available, the executor cannot satisfy this. Recommendation: add a "touch-and-commit" fallback that updates the file's mtime (e.g., `touch app/src/release/generated/baselineProfiles/baseline-prof.txt`) with an explicit comment in the commit message noting that regeneration was deferred due to no connected device, and create a follow-up issue. This is better than leaving the phase incomplete.

**[LOW] 01.3 Task 2 (DEPS-12): `git add -u` may miss newly-created ktlint baseline files**

If ktlint creates per-module baseline files that don't already exist in git, `git add -u` (which only stages modified/deleted tracked files) won't catch them. The research says "per-module ktlint baselines (regenerated)" but no such files currently exist in the repo. If they're created during `ktlintFormat`, they need `git add` without `-u`. Fix: add `git add '**/ktlint-baseline.xml' '**/.ktlint-baseline' 2>/dev/null || true` before the commit.

**[LOW] 01.2 Task 2 (DEPS-08): `ksp.incremental=false` fallback commit guidance is ambiguous**

The plan says "DO NOT commit the `gradle.properties` edit in this commit unless required." But if the fallback *is* required to make KSP pass, it should be committed — the plan doesn't specify when "required" means "commit it." Clarify: if `ksp.incremental=false` is needed to pass `:app:kspDebugKotlin`, amend the DEPS-08 commit to include the `gradle.properties` change (or create a follow-up commit `chore(deps): DEPS-08-fix — disable KSP incremental to work around Hilt NPE`).

**[LOW] 01.1 Task 4 (DEPS-04): `baselineprofile/build.gradle.kts` uses deprecated `kotlinOptions` DSL**

Line 15: `kotlinOptions { jvmTarget = "17" }` — this is the old AGP Kotlin DSL, deprecated in favor of the `kotlin {}` block. Under Kotlin 2.2.20 + AGP 9.2.0 this will likely produce a deprecation warning (not a compile error). Not a phase blocker, but worth noting as a POLISH-04 follow-up. The plan correctly doesn't address it here.

**[LOW] 01.3 Task 5 (DEPS-15): `grep -c compose-compiler` verification may overcount**

The verify command `./gradlew :app:dependencies --configuration releaseRuntimeClasspath -q | grep -c compose-compiler` counts lines containing "compose-compiler" in the dependency tree output. The tree includes both the plugin classpath entry and resolved artifacts. The RESEARCH.md says "exactly one `compose-compiler` version present" — the grep counts *lines*, not *unique versions*. A more precise check would be `| grep 'compose-compiler' | grep -oE '[0-9]+\.[0-9]+\.[0-9]+' | sort -u | wc -l` to count unique versions. Low severity since a duplicate would likely also surface as a compile error.

### 4. Suggestions

1. **DEPS-10 (01.2 Task 4)**: Derive Media3 version from probe result using `cut -d- -f1` instead of hardcoding `"1.10.0"`. This future-proofs the plan against new nextlib releases.

2. **DEPS-16 (01.3 Task 6)**: Add a touch-and-commit fallback for the no-device scenario with an explicit follow-up issue. The commit message should read: `chore(deps): DEPS-16 — baseline profile touch (regen deferred; no connected device at execution time)`.

3. **DEPS-12 (01.3 Task 2)**: Replace `git add -u` with `git add -A` in the staging step, or explicitly add potential new ktlint baseline paths.

4. **DEPS-08 (01.2 Task 2)**: Clarify the `ksp.incremental=false` commit policy: "If the fallback is required to pass KSP, include the `gradle.properties` change in the DEPS-08 commit with an explanatory commit body."

5. **DEPS-13 (01.3 Task 3)**: Simplify the verify command to two explicit checks rather than a compound boolean:
   ```bash
   # Check A: plugin loads
   ./gradlew dependencyCheckAnalyze --no-configuration-cache -q 2>/dev/null && echo "plugin-loads" || echo "plugin-broken"
   # Check B: if broken, plugin references removed
   grep -c 'dependency.check\|dependencyCheck' build.gradle.kts
   ```

6. **All plans**: Add `--exclude-dir=.gradle` to the global grep for `-Xcontext-receivers`/`-Xskip-metadata-version-check` in DEPS-05 verify. The current exclude list has `.planning`, `build`, but not `.gradle` (which may contain cached compiled convention plugin classes with the flags embedded). The DEPS-05 verify already has this in plan 01.1 — confirmed present.

### 5. Risk Assessment

**MEDIUM** — The plans are structurally sound with correct lockstep ordering, proper failure recovery, and accurate file/line references. The three medium-severity issues (Media3 version derivation, DEPS-16 device fallback, DEPS-13 verify logic) are all fixable with minor edits and don't undermine the overall approach. Execution risk is dominated by external factors (AGP 9.2.0 compatibility with existing convention plugins, Hilt 2.57.1 + KSP 2.2.20-2.0.4 interaction) which are appropriately flagged as lockstep stop conditions.

---

*Generated 2026-05-16. Reviews are independent — synthesize, don't average.*
