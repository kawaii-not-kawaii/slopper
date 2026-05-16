# Spec-Layer State

project: slopper
current_phase: 1
state: phase_1_active
brownfield: true
last_updated: 2026-05-16

tracker:
  enabled: true
  type: forgejo
  remote: https://alpine-forgejo.twin-wezen.ts.net/chibicoffeelover/slopper.git
  host: alpine-forgejo.twin-wezen.ts.net
  owner: chibicoffeelover
  repo: slopper
  milestone_number: 12
  milestone_title: v1.0
  health_status: ok
  last_health_check: 2026-05-16T23:25:00Z
  last_error: null
  issues:
    1: 1
    2: 2
    3: 3
    4: 4

advisory_notes: []
accepted_risks: []

---

## Phase 1 — DEPS (Foundation Bump)
ui: false
phase_name: "DEPS — Foundation Bump"
phase_dir: .planning/phases/01-deps-foundation-bump
state: phase_1_active
assumptions_open: 0
cc_concerns_cleared: false
advisory_notes:
  - "W2 (plan-checker): empty marker commit for DEPS-14 makes the commit non-bisectable; documented in plan, not blocking"
accepted_risks: []
issue_number: 1
issue_url: https://alpine-forgejo.twin-wezen.ts.net/chibicoffeelover/slopper/issues/1
command_manifest:
  - step: 1
    cmd: "/gsd-spec-phase 1"
    output_file: ".planning/phases/01-deps-foundation-bump/01-SPEC.md"
    gate_file: "gates/spec.md"
    status: complete
    gate_passed: true
    tracker_synced: true
    completed_at: 2026-05-16T00:00:00Z
    tracker_comment_url: https://alpine-forgejo.twin-wezen.ts.net/chibicoffeelover/slopper/issues/1#issuecomment-408
  - step: 2
    cmd: "/gsd-discuss-phase 1"
    output_file: ".planning/phases/01-deps-foundation-bump/01-CONTEXT.md"
    gate_file: "gates/discuss.md"
    status: complete
    gate_passed: true
    tracker_synced: true
    completed_at: 2026-05-16T00:00:00Z
    tracker_comment_url: https://alpine-forgejo.twin-wezen.ts.net/chibicoffeelover/slopper/issues/1#issuecomment-409
  - step: 3
    cmd: "/gsd-plan-phase 1"
    output_file_glob: ".planning/phases/01-deps-foundation-bump/01.*-PLAN.md"
    gate_file: "gates/plan.md"
    status: complete
    gate_passed: true
    tracker_synced: true
    completed_at: 2026-05-16T00:00:00Z
    tracker_comment_url: https://alpine-forgejo.twin-wezen.ts.net/chibicoffeelover/slopper/issues/1#issuecomment-410
  - step: 4
    cmd: "/gsd-review --phase 1 --all"
    output_file: ".planning/phases/01-deps-foundation-bump/REVIEWS.md"
    gate_file: "gates/review.md"
    status: complete
    gate_passed: true
    tracker_synced: true
    completed_at: 2026-05-16T00:00:00Z
    tracker_comment_url: https://alpine-forgejo.twin-wezen.ts.net/chibicoffeelover/slopper/issues/1#issuecomment-415
    unblock_history:
      blocked_at: 2026-05-16T00:00:00Z
      blocked_comment: https://alpine-forgejo.twin-wezen.ts.net/chibicoffeelover/slopper/issues/1#issuecomment-413
      blocking_concerns_resolved:
        - "C1: merged DEPS-03+04 into one atomic commit; CONTEXT.md §Decision 1 Refinement added"
        - "C2: per-task <on_failure> 3-branch revert protocol + awk REQUIREMENTS.md logger"
        - "C3: Media3 regex tightened to 1\\.10\\.0[^<]*"
        - "C4: DEPS-16 hard-fail on no device, no task-level defer path"
        - "C6: switched to kotlinCompilerPluginClasspathRelease"
      additional_fixes:
        - "kimi unique: R8 fullMode audit moved 01.1 → 01.3 Task 4"
        - "glm unique: DEPS-14 bisect hard-stops on lockstep-set culprit match"
        - "Bonus: distributionSha256Sum lookup at execution time for Gradle wrapper"
      replan_commit: 9487c9d
  - step: 5
    cmd: "/gsd-execute-phase 1"
    output_file: ".planning/phases/01-deps-foundation-bump/VERIFICATION.md"
    gate_file: "gates/execute.md"
    status: complete
    gate_passed: true
    tracker_synced: true
    completed_at: 2026-05-16T21:55:00+09:00
    tracker_comment_url: https://alpine-forgejo.twin-wezen.ts.net/chibicoffeelover/slopper/issues/1#issuecomment-417
    notes:
      - "PASS-WITH-NOTES: 12/17 reqs landed, 5/17 deferred with REVIEWS-C4-compliant hygiene (DEPS-03/04/07/10-1.10/16)"
      - "AGP 9 / compileSdk 36 / Media3 1.10 deferred via DEPS-17 (no Hilt supports AGP 9 yet)"
      - "DEPS-16 baseline profile regen deferred via REVIEWS-C4 path (CONTEXT.md edit + user ACCEPT) — no device on host"
      - "Re-plan landed at 4aed48c after Wave 1 partial halt (Hilt/AGP-9 reality check)"
      - "VERIFICATION.md commit: d7594f9"
      - "Final phase HEAD: e21fa7c on phase-1/deps-bump"
  - step: 6
    cmd: "/gsd-verify-work 1"
    output_file: ".planning/phases/01-deps-foundation-bump/01-UAT.md"
    gate_file: "gates/verify.md"
    status: complete
    gate_passed: true
    tracker_synced: true
    completed_at: 2026-05-16T22:45:00+09:00
    tracker_comment_url: https://alpine-forgejo.twin-wezen.ts.net/chibicoffeelover/slopper/issues/1#issuecomment-418
    notes:
      - "7/7 tests pass; 7-step manual smoke on Galaxy S23+ Android 16 — all green"
      - "Confirms no runtime regression from Kotlin 2.2.20 + Compose BOM 2026.05.00 + Hilt 2.56.2 + Apollo 4.4.3 lockstep"
      - "Validates precondition cleanups (orphan code blocks in app/build.gradle.kts, PlayerScreen, SettingsScreen, MainActivity) didn't lose UI surface"
  - step: 7
    cmd: "/gsd-extract-learnings 1"
    output_file: ".planning/phases/01-deps-foundation-bump/01-LEARNINGS.md"
    gate_file: "gates/learnings.md"
    status: complete
    gate_passed: true
    tracker_synced: true
    completed_at: 2026-05-16T22:55:00+09:00
    tracker_comment_url: https://alpine-forgejo.twin-wezen.ts.net/chibicoffeelover/slopper/issues/1#issuecomment-419
    notes:
      - "30 learnings extracted: 8 decisions / 9 lessons / 6 patterns / 7 surprises"
      - "Headline: AGP 9 is not a minor bump (5 distinct breakages); Hilt 2.57.1 doesn't exist; bf01b34 restore had 7 invisible corruptions; lockstep tightened from plan's DEPS-03+04 to executed DEPS-05+08"
  - step: 8
    cmd: "/gsd-secure-phase 1"
    output_file_glob: ".planning/phases/01-deps-foundation-bump/*SECURITY*"
    gate_file: "gates/security.md"
    status: complete
    gate_passed: true
    tracker_synced: true
    completed_at: 2026-05-17T08:20:00+09:00
    tracker_comment_url: https://alpine-forgejo.twin-wezen.ts.net/chibicoffeelover/slopper/issues/1#issuecomment-420
    notes:
      - "retroactive-STRIDE — no plan-time threat model existed; auditor built register from implementation"
      - "13 threats: 9 closed, 0 open, 6 accepted (gate SECURED under block_on: high)"
      - "Mitigations landed: T-T-01 wrapper SHA pin (504969f), T-I-04 .gitignore (eb75866), T-S-01+T-T-03 schema provenance (e1518f3)"
      - "T-T-04 local CVE scan failed at 2h47m without NVD_API_KEY; accepted with structural mitigation SEC-CI-01 (Forgejo Actions weekly)"
      - "T-T-02 dep verification metadata deferred to AGP-9 phase (SEC-VERIFY-01)"
  - step: 9
    cmd: "/gsd-validate-phase 1"
    output_file_glob: ".planning/phases/01-deps-foundation-bump/*VALIDATION*"
    gate_file: "gates/validation.md"
    status: complete
    gate_passed: true
    tracker_synced: true
    completed_at: 2026-05-17T08:35:00+09:00
    tracker_comment_url: https://alpine-forgejo.twin-wezen.ts.net/chibicoffeelover/slopper/issues/1#issuecomment-421
    notes:
      - "documented-stub mode: workflow.nyquist_validation=false in config; Phase 1 is infrastructure-only (no new behavior surface)"
      - "17 reqs: 12 COVERED via build/UAT/security gates, 5 DEFERRED with hygiene; 0 new tests generated"
      - "POLISH-04 + POLISH-05 backlog will land the test pyramid in a future phase"
  - step: 10
    cmd: "/gsd-docs-update 1"
    output_marker: "docs_updated_at"
    gate_file: "gates/docs.md"
    status: complete
    gate_passed: true
    tracker_synced: true
    completed_at: 2026-05-17T09:05:00+09:00
    tracker_comment_url: https://alpine-forgejo.twin-wezen.ts.net/chibicoffeelover/slopper/issues/1#issuecomment-422
    docs_updated_at: 2026-05-17T09:05:00+09:00
    notes:
      - "Full canonical pass: 6 docs via 6 parallel doc-writer subagents, 1683 lines total"
      - "Files: README (rewrite), ARCHITECTURE / GETTING-STARTED / DEVELOPMENT / TESTING / CONFIGURATION (new)"
      - "3 real bugs surfaced (settings.gradle.kts duplicate include; DEVICE_TESTING.md stale cd-android path; pre-rewrite README JDK-21 claim)"
      - "4 codebase drifts caught (ConnectionStore is EncryptedSharedPreferences; detekt baselines per-module; no ktlint baselines; :feature:settings→:feature:player anti-pattern)"
  - step: 11
    cmd: "/gsd-ship 1"
    output_marker: "pr_merged_at"
    gate_file: "gates/ship.md"
    status: pending
    gate_passed: false
    tracker_synced: false
    completed_at: null
    tracker_comment_url: null

---

## Phase 2 — COMPLY (Platform Compliance)
ui: false
phase_name: "COMPLY — Platform Compliance"
phase_dir: .planning/phases/02-comply-platform-compliance
state: pending
assumptions_open: 0
cc_concerns_cleared: false
advisory_notes: []
accepted_risks: []
issue_number: 2
issue_url: https://alpine-forgejo.twin-wezen.ts.net/chibicoffeelover/slopper/issues/2
command_manifest:
  - step: 1
    cmd: "/gsd-spec-phase 2"
    output_file: ".planning/phases/02-comply-platform-compliance/02-SPEC.md"
    gate_file: "gates/spec.md"
    status: pending
    gate_passed: false
    tracker_synced: false
    completed_at: null
    tracker_comment_url: null
  - step: 2
    cmd: "/gsd-discuss-phase 2"
    output_file: ".planning/phases/02-comply-platform-compliance/02-CONTEXT.md"
    gate_file: "gates/discuss.md"
    status: pending
    gate_passed: false
    tracker_synced: false
    completed_at: null
    tracker_comment_url: null
  - step: 3
    cmd: "/gsd-plan-phase 2"
    output_file_glob: ".planning/phases/02-comply-platform-compliance/02*-PLAN.md"
    gate_file: "gates/plan.md"
    status: pending
    gate_passed: false
    tracker_synced: false
    completed_at: null
    tracker_comment_url: null
  - step: 4
    cmd: "/gsd-review --phase 2 --all"
    output_file: ".planning/phases/02-comply-platform-compliance/REVIEWS.md"
    gate_file: "gates/review.md"
    status: pending
    gate_passed: false
    tracker_synced: false
    completed_at: null
    tracker_comment_url: null
  - step: 5
    cmd: "/gsd-execute-phase 2"
    output_file: ".planning/phases/02-comply-platform-compliance/VERIFICATION.md"
    gate_file: "gates/execute.md"
    status: pending
    gate_passed: false
    tracker_synced: false
    completed_at: null
    tracker_comment_url: null
  - step: 6
    cmd: "/gsd-verify-work 2"
    output_file: ".planning/phases/02-comply-platform-compliance/02-UAT.md"
    gate_file: "gates/verify.md"
    status: pending
    gate_passed: false
    tracker_synced: false
    completed_at: null
    tracker_comment_url: null
  - step: 7
    cmd: "/gsd-extract-learnings 2"
    output_file: ".planning/phases/02-comply-platform-compliance/LEARNINGS.md"
    gate_file: "gates/learnings.md"
    status: pending
    gate_passed: false
    tracker_synced: false
    completed_at: null
    tracker_comment_url: null
  - step: 8
    cmd: "/gsd-secure-phase 2"
    output_file_glob: ".planning/phases/02-comply-platform-compliance/*SECURITY*"
    gate_file: "gates/security.md"
    status: pending
    gate_passed: false
    tracker_synced: false
    completed_at: null
    tracker_comment_url: null
  - step: 9
    cmd: "/gsd-validate-phase 2"
    output_file_glob: ".planning/phases/02-comply-platform-compliance/*VALIDATION*"
    gate_file: "gates/validation.md"
    status: pending
    gate_passed: false
    tracker_synced: false
    completed_at: null
    tracker_comment_url: null
  - step: 10
    cmd: "/gsd-docs-update 2"
    output_marker: "docs_updated_at"
    gate_file: "gates/docs.md"
    status: pending
    gate_passed: false
    tracker_synced: false
    completed_at: null
    tracker_comment_url: null
  - step: 11
    cmd: "/gsd-ship 2"
    output_marker: "pr_merged_at"
    gate_file: "gates/ship.md"
    status: pending
    gate_passed: false
    tracker_synced: false
    completed_at: null
    tracker_comment_url: null

---

## Phase 3 — PERF (Measured Wins)
ui: false
phase_name: "PERF — Measured Wins"
phase_dir: .planning/phases/03-perf-measured-wins
state: pending
assumptions_open: 0
cc_concerns_cleared: false
advisory_notes: []
accepted_risks: []
issue_number: 3
issue_url: https://alpine-forgejo.twin-wezen.ts.net/chibicoffeelover/slopper/issues/3
command_manifest:
  - step: 1
    cmd: "/gsd-spec-phase 3"
    output_file: ".planning/phases/03-perf-measured-wins/03-SPEC.md"
    gate_file: "gates/spec.md"
    status: pending
    gate_passed: false
    tracker_synced: false
    completed_at: null
    tracker_comment_url: null
  - step: 2
    cmd: "/gsd-discuss-phase 3"
    output_file: ".planning/phases/03-perf-measured-wins/03-CONTEXT.md"
    gate_file: "gates/discuss.md"
    status: pending
    gate_passed: false
    tracker_synced: false
    completed_at: null
    tracker_comment_url: null
  - step: 3
    cmd: "/gsd-plan-phase 3"
    output_file_glob: ".planning/phases/03-perf-measured-wins/03*-PLAN.md"
    gate_file: "gates/plan.md"
    status: pending
    gate_passed: false
    tracker_synced: false
    completed_at: null
    tracker_comment_url: null
  - step: 4
    cmd: "/gsd-review --phase 3 --all"
    output_file: ".planning/phases/03-perf-measured-wins/REVIEWS.md"
    gate_file: "gates/review.md"
    status: pending
    gate_passed: false
    tracker_synced: false
    completed_at: null
    tracker_comment_url: null
  - step: 5
    cmd: "/gsd-execute-phase 3"
    output_file: ".planning/phases/03-perf-measured-wins/VERIFICATION.md"
    gate_file: "gates/execute.md"
    status: pending
    gate_passed: false
    tracker_synced: false
    completed_at: null
    tracker_comment_url: null
  - step: 6
    cmd: "/gsd-verify-work 3"
    output_file: ".planning/phases/03-perf-measured-wins/03-UAT.md"
    gate_file: "gates/verify.md"
    status: pending
    gate_passed: false
    tracker_synced: false
    completed_at: null
    tracker_comment_url: null
  - step: 7
    cmd: "/gsd-extract-learnings 3"
    output_file: ".planning/phases/03-perf-measured-wins/LEARNINGS.md"
    gate_file: "gates/learnings.md"
    status: pending
    gate_passed: false
    tracker_synced: false
    completed_at: null
    tracker_comment_url: null
  - step: 8
    cmd: "/gsd-secure-phase 3"
    output_file_glob: ".planning/phases/03-perf-measured-wins/*SECURITY*"
    gate_file: "gates/security.md"
    status: pending
    gate_passed: false
    tracker_synced: false
    completed_at: null
    tracker_comment_url: null
  - step: 9
    cmd: "/gsd-validate-phase 3"
    output_file_glob: ".planning/phases/03-perf-measured-wins/*VALIDATION*"
    gate_file: "gates/validation.md"
    status: pending
    gate_passed: false
    tracker_synced: false
    completed_at: null
    tracker_comment_url: null
  - step: 10
    cmd: "/gsd-docs-update 3"
    output_marker: "docs_updated_at"
    gate_file: "gates/docs.md"
    status: pending
    gate_passed: false
    tracker_synced: false
    completed_at: null
    tracker_comment_url: null
  - step: 11
    cmd: "/gsd-ship 3"
    output_marker: "pr_merged_at"
    gate_file: "gates/ship.md"
    status: pending
    gate_passed: false
    tracker_synced: false
    completed_at: null
    tracker_comment_url: null

---

## Phase 4 — POLISH (Test Pyramid & Cleanup)
ui: false
phase_name: "POLISH — Test Pyramid & Cleanup"
phase_dir: .planning/phases/04-polish-test-pyramid
state: pending
assumptions_open: 0
cc_concerns_cleared: false
advisory_notes: []
accepted_risks: []
issue_number: 4
issue_url: https://alpine-forgejo.twin-wezen.ts.net/chibicoffeelover/slopper/issues/4
command_manifest:
  - step: 1
    cmd: "/gsd-spec-phase 4"
    output_file: ".planning/phases/04-polish-test-pyramid/04-SPEC.md"
    gate_file: "gates/spec.md"
    status: pending
    gate_passed: false
    tracker_synced: false
    completed_at: null
    tracker_comment_url: null
  - step: 2
    cmd: "/gsd-discuss-phase 4"
    output_file: ".planning/phases/04-polish-test-pyramid/04-CONTEXT.md"
    gate_file: "gates/discuss.md"
    status: pending
    gate_passed: false
    tracker_synced: false
    completed_at: null
    tracker_comment_url: null
  - step: 3
    cmd: "/gsd-plan-phase 4"
    output_file_glob: ".planning/phases/04-polish-test-pyramid/04*-PLAN.md"
    gate_file: "gates/plan.md"
    status: pending
    gate_passed: false
    tracker_synced: false
    completed_at: null
    tracker_comment_url: null
  - step: 4
    cmd: "/gsd-review --phase 4 --all"
    output_file: ".planning/phases/04-polish-test-pyramid/REVIEWS.md"
    gate_file: "gates/review.md"
    status: pending
    gate_passed: false
    tracker_synced: false
    completed_at: null
    tracker_comment_url: null
  - step: 5
    cmd: "/gsd-execute-phase 4"
    output_file: ".planning/phases/04-polish-test-pyramid/VERIFICATION.md"
    gate_file: "gates/execute.md"
    status: pending
    gate_passed: false
    tracker_synced: false
    completed_at: null
    tracker_comment_url: null
  - step: 6
    cmd: "/gsd-verify-work 4"
    output_file: ".planning/phases/04-polish-test-pyramid/04-UAT.md"
    gate_file: "gates/verify.md"
    status: pending
    gate_passed: false
    tracker_synced: false
    completed_at: null
    tracker_comment_url: null
  - step: 7
    cmd: "/gsd-extract-learnings 4"
    output_file: ".planning/phases/04-polish-test-pyramid/LEARNINGS.md"
    gate_file: "gates/learnings.md"
    status: pending
    gate_passed: false
    tracker_synced: false
    completed_at: null
    tracker_comment_url: null
  - step: 8
    cmd: "/gsd-secure-phase 4"
    output_file_glob: ".planning/phases/04-polish-test-pyramid/*SECURITY*"
    gate_file: "gates/security.md"
    status: pending
    gate_passed: false
    tracker_synced: false
    completed_at: null
    tracker_comment_url: null
  - step: 9
    cmd: "/gsd-validate-phase 4"
    output_file_glob: ".planning/phases/04-polish-test-pyramid/*VALIDATION*"
    gate_file: "gates/validation.md"
    status: pending
    gate_passed: false
    tracker_synced: false
    completed_at: null
    tracker_comment_url: null
  - step: 10
    cmd: "/gsd-docs-update 4"
    output_marker: "docs_updated_at"
    gate_file: "gates/docs.md"
    status: pending
    gate_passed: false
    tracker_synced: false
    completed_at: null
    tracker_comment_url: null
  - step: 11
    cmd: "/gsd-ship 4"
    output_marker: "pr_merged_at"
    gate_file: "gates/ship.md"
    status: pending
    gate_passed: false
    tracker_synced: false
    completed_at: null
    tracker_comment_url: null
