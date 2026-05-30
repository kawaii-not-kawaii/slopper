# Requirements: Slopper — v1.1 AGP-9 Toolchain Modernization (DEPS-17)

**Defined:** 2026-05-30
**Core Value:** The app continues to install and run on every device it currently supports, with the deferred AGP-9 build-toolchain cluster unblocked and landed — without changing app behavior or bumping `minSdk`.

> Pure build-toolchain upgrade. There are no end-user features; each requirement is a verifiable build/config state. Full grounding in `.planning/research/` (STACK, FEATURES, ARCHITECTURE, PITFALLS, SUMMARY). Recommended target set: AGP **9.2.1** · Gradle **9.4.1** · Hilt **2.59.2** · Kotlin/KSP **unchanged (2.2.20 / 2.2.20-2.0.4)** · compileSdk **36** · Media3 **1.10.0** + nextlib **1.10.0-0.12.1**. Three exact-patch versions are flagged in SUMMARY.md for confirmation at plan time.

## v1.1 Requirements

### Toolchain Core — the atomic critical path

- [ ] **AGP9-01**: Gradle wrapper upgraded to the AGP-9.2 floor (target 9.4.1) with `distributionSha256Sum` re-pinned; the build runs green on Gradle 9 with every plugin (detekt, ktlint, OWASP dependency-check, baseline-profile) confirmed compatible — Gradle-9 deprecations enumerated (`./gradlew help --warning-mode=all`) and resolved.
- [ ] **AGP9-02**: AGP upgraded to 9.2.1 with the `build-logic/convention` migration complete — `org.jetbrains.kotlin.android` removed from all application sites (built-in Kotlin), `CommonExtension<*,…>` generics removed, and `kotlinOptions{}` migrated to `kotlin{compilerOptions{}}`; all ~14 modules configure and `compileDebugSources` is green.
- [ ] **AGP9-03**: Hilt/Dagger upgraded to 2.59.2 (never bare "2.59+") with Kotlin 2.2.20 / KSP 2.2.20-2.0.4 confirmed satisfying AGP 9's KGP floor; Hilt + Apollo KSP codegen produces a working DI graph (no Kotlin/KSP bump).

### Compile SDK

- [ ] **SDK-01**: `compileSdk` raised 35 → 36 in all 3 touchpoints (`KotlinAndroid.kt` + `baselineprofile`), with `targetSdk = 35` preserved explicit in every site so AGP 9's `defaultTargetSdkToCompileSdkIfUnset` does not silently opt the app into Android-16 runtime behavior; build green.

### Library Bumps — separable, green-gated follow-ups

- [ ] **LIB-01**: Media3 1.9.1 → 1.10.0 and `nextlib-media3ext` 1.9.1-0.11.0 → 1.10.0-0.12.1 as a single locked pair (NOT 1.10.1 — no nextlib pairing exists); software-codec (FFmpeg renderer) playback verified on a physical device to rule out `UnsatisfiedLinkError`.
- [ ] **LIB-02**: activity-compose → 1.13 and core-ktx → 1.18 (both unblocked by compileSdk 36), with their `androidx.activity:*` / `androidx.core:core*` entries removed from the Dependabot ignore list now that the cluster is unblocked.

### CI Signing

- [ ] **CI-01**: An `assembleDebug`/`validateSigningDebug` probe runs on the AGP-9 CI runners as `continue-on-error`; the outcome is acted on — either a demonstrated green run promotes a real assemble/signing gate, or the BouncyCastle EdEC blocker is documented as persisting and the compile-only gate is kept as the contract (with a `bcprov-jdk18on` pin if assembly is forced).

## Future Requirements

Deferred — tracked but not in this milestone's roadmap.

### Background Media

- **BG-MEDIA-01**: Real `MediaSessionService` background playback (own milestone).

### Measured Performance

- **PERF-MB-01**: Execute the wired macrobenchmark suite and capture baseline-profile deltas (carried from v1.0 Phase 3 — infrastructure ready, device measurement deferred).

### Architecture Debt

- **WR-02**: Refactor `SettingsViewModel` to expose `PlayerSettings`/`UiSettings` domain interfaces instead of concrete `PlayerPreferences`/`UiPreferences` (carried from v1.0 Phase 6 review).
- **APOLLO-CACHE-01**: Adopt Apollo 5's declarative normalized cache (`com.apollographql.cache:*`) — only if a caching need emerges.

## Out of Scope

| Item | Reason |
|------|--------|
| Kotlin past 2.2.20 / KSP past 2.2.20-2.0.4 | AGP 9 needs only KGP ≥ 2.2.10; bumping re-triggers the KSP/Hilt lockstep churn for no gain |
| Media3 1.10.1 | No matching `nextlib-media3ext` 1.10.1 build exists — hard cap at 1.10.0 |
| `minSdk` / `targetSdk` bump | App must stay installable + behaviorally unchanged on existing devices (Android-16 opt-ins forbidden) |
| `android.enableLegacyVariantApi` / `android.newDsl=false` | No-op / doomed crutches; repo is already clean (zero `applicationVariants`/`buildDir`) — must NOT be added |
| New end-user features | Modernization-only milestone |
| Module-graph restructure | Frozen — changes stay within existing modules |
| Migrating off Compose / Hilt / Gradle Kotlin DSL | Existing architecture preserved |

## Traceability

Which phases cover which requirements. Filled by the roadmapper.

| Requirement | Phase | Status |
|-------------|-------|--------|
| AGP9-01 | Phase 7 | Readiness done (07.1 sweep + 07.2 ADR: pinned 9.4.1, plugin verdicts, detekt decision, KGP floor); green-on-Gradle-9 folds to Phase 8 |
| AGP9-02 | Phase 8 | Pending |
| AGP9-03 | Phase 8 | Pending |
| SDK-01 | Phase 8 | Pending |
| LIB-01 | Phase 9 | Pending |
| LIB-02 | Phase 9 | Pending |
| CI-01 | Phase 10 | Pending |
