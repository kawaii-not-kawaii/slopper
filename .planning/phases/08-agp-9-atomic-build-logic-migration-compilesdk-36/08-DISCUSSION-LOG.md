# Phase 8: AGP-9 — Atomic Build-Logic Migration + compileSdk 36 - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-05-30
**Phase:** 8-agp-9-atomic-build-logic-migration-compilesdk-36
**Mode:** `--auto` (autonomous — Claude selected the recommended option for each area; no interactive prompts)
**Areas discussed:** Commit sequencing, Version pins, targetSdk safety guard, Built-in Kotlin removal, CI cache + gate, Forbidden crutches/scope

---

## Commit sequencing (bisectability)

| Option | Description | Selected |
|--------|-------------|----------|
| One concern per commit, choke-point smoke first | 7-step ordered sequence; build only green at the end; `:core:common:compileDebugKotlin` smoke before full build | ✓ |
| Single mega-commit | All changes in one commit | |

**Choice:** One concern per commit (D-01/D-02). **Notes:** Preserves `git bisect` over the atomic change-set; matches the ROADMAP research flag ("migrate one concern per commit").

## Version pins

| Option | Description | Selected |
|--------|-------------|----------|
| AGP 9.2.1 + Hilt 2.59.2 (confirm live Maven) | Latest stable patches; exact pins, never floating | ✓ |
| AGP 9.2.0 / Hilt 2.59.1 | Earlier patches | |

**Choice:** AGP 9.2.1, Hilt 2.59.2, Kotlin/KSP unchanged (D-03/D-04/D-04b). **Notes:** Confirm against live Maven metadata at plan time; fall back to 9.2.0 only if 9.2.1 unpublished. Never bare `2.59+`.

## targetSdk safety guard

| Option | Description | Selected |
|--------|-------------|----------|
| Keep targetSdk=35 explicit in all 3 sites + assert | Prevents AGP-9 `defaultTargetSdkToCompileSdkIfUnset` silent flip to 36 | ✓ |
| Rely on AGP defaults | Let AGP infer targetSdk | |

**Choice:** Explicit targetSdk=35 everywhere + grep verification (D-05a/D-05b). **Notes:** Highest-consequence silent failure — an inferred targetSdk=36 is a forbidden Android-16 runtime opt-in. compileSdk→36 only.

## Built-in Kotlin removal

| Option | Description | Selected |
|--------|-------------|----------|
| Remove kotlin.android from all 5 sites; keep compose/serialization | AGP 9 bundles Kotlin Android plugin | ✓ |
| Leave existing kotlin.android applies | | |

**Choice:** Remove all 5 `org.jetbrains.kotlin.android` sites (D-06/D-06b). **Notes:** Any leftover apply/alias → "extension already registered". compose + serialization plugins stay.

## CI cache + gate

| Option | Description | Selected |
|--------|-------------|----------|
| Bump agp8→agp9; full gate with isolation flags removed | Stale AGP-8 cache poisons AGP-9 config | ✓ |
| Keep cache key | | |

**Choice:** Bump cache key both sites; gate = `compileDebugSources detekt ktlintCheck test` green, opt-out flags removed (D-07/D-08). **Notes:** detekt-on-Gradle-9 is first empirically tested here per Phase-7 ADR; deviation-handle if it hard-fails (no alpha bump).

## Forbidden crutches / scope

| Option | Description | Selected |
|--------|-------------|----------|
| No crutch flags; keep change-set atomic; defer settings.gradle.kts dedupe | | ✓ |
| Add enableLegacyVariantApi / newDsl=false | | |

**Choice:** No crutch flags; keep Phase 8 atomic (D-09). **Notes:** `settings.gradle.kts` duplicate include deferred to keep bisect-pure.

## Claude's Discretion

- Exact commit messages and sub-edit grouping (within the bisect invariant).
- Expression of the `CommonExtension` generics fix (type-param list vs alias vs star-projection).
- Whether to run the full gate after the Hilt commit as well as at the end.

## Deferred Ideas

- `settings.gradle.kts` duplicate `include(":baselineprofile")` dedupe (1-line; deferred).
- Media3/nextlib + leaf libs → Phase 9.
- CI assemble/signing probe + EdEC/bcprov spike → Phase 10.
- DEPS-07 lint re-enables → only if AGP-9 changes lint behavior.
