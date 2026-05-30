# Stack Research — Slopper Modernization (2026-05)

**Domain:** Android app (Kotlin / Compose, multi-module Gradle, sideload distribution)
**Researched:** 2026-05-16
**Confidence:** HIGH (versions cross-checked against developer.android.com / JetBrains / GitHub releases as of 2026-05-16)
**Scope:** Brownfield modernization — identify the 2026-stable target versions for the existing stack, no new SDKs. Constraints: minSdk stays at 26; stable releases only; preserve module structure.

---

## Headline Recommendation

Adopt the **AGP 9.x line** (currently 9.2.0) and bump every cooperating tool to the version that 9.2 requires: Gradle **9.4.1**, Kotlin **2.2.x** (stay one minor behind 2.3 so KSP/Hilt/Compose all have stable wheels), Compose BOM **2026.05.00** (core libs 1.11.1, Material3 1.4.0), and Media3 **1.10.0** (now legal once `compileSdk` moves to 36). The big-ticket "free" wins: AGP 9 brings built-in Kotlin support, KSP2 (default since KSP 2.0+) replaces the legacy KSP1, and the existing `EncryptedSharedPreferences` path is officially deprecated in favour of DataStore+Tink (do **not** migrate this milestone — call it out as a separate phase).

JDK target stays at **17** (AGP 9.2 explicitly lists JDK 17 as both minimum and default — no need to chase JDK 21). `minSdk = 26` is preserved; the only minSdk-affected library in the upgrade set is Lifecycle 2.10 (minSdk 23, well below ours).

---

## Recommended Stack — Target Versions

### Build / Toolchain

| Tech | Current in repo | Target stable (2026-05) | Migration risk | Notes |
|------|----------------|------------------------|----------------|-------|
| AGP | 8.7.3 | **9.2.0** | **HIGH** | Major version jump. Built-in Kotlin support (no longer apply `org.jetbrains.kotlin.android` explicitly). New DSL deprecations. R8 `-keepattributes` semantics changed — wildcards no longer keep runtime-invisible annotations; audit `app/proguard-rules.pro`. Requires Gradle ≥ 9.4.1, JDK 17, SDK Build Tools 36.0.0, `compileSdk` 36+ supported up to 36.1. Source: [AGP 9.2.0 release notes](https://developer.android.com/build/releases/agp-9-2-0-release-notes). |
| Gradle wrapper | 8.11.1 | **9.4.1** (min for AGP 9.2) — can go to **9.5.1** | **HIGH** | Gradle 9 major. Configuration cache stabilization, Provider API tightening. OWASP `dependencyCheck` 11.1.1 currently runs with `--no-configuration-cache`; re-verify on 9.x. Source: [Gradle 9 upgrade guide](https://docs.gradle.org/current/userguide/upgrading_major_version_9.html). |
| JDK toolchain | 17 | **17** (keep) | None | AGP 9.2 lists JDK 17 as both minimum and default — no need to bump to 21. |
| compileSdk | 35 | **36** | Med | Required for Media3 1.10 and Compose BOM 2026.05. Behaviour changes for `Activity#onBackPressed`, foreground services, edge-to-edge default true. AGP 9.2 supports up to 36.1. |
| minSdk | 26 | **26** (frozen) | None | Per constraint. baselineprofile module keeps 28. |
| Kotlin | 2.1.0 | **2.2.20** (stable) | Med | Don't jump straight to 2.3.x — Detekt 2.0 is alpha-only on 2.3, and several third-party libs still pin against 2.2. Kotlin 2.2 stabilises `guard conditions`, `non-local break/continue`, `multi-dollar interpolation`. **Context-receivers experimental flag may need to migrate to `context parameters`** (preview in 2.2). Source: [Kotlin 2.2 release blog](https://blog.jetbrains.com/kotlin/2025/06/kotlin-2-2-0-released/). |
| KSP | 2.1.0-1.0.29 (KSP1 + Kotlin 2.1) | **2.2.20-x.y.z** (latest KSP2 for Kotlin 2.2.20) | Med | KSP2 is default since KSP 2.0+ and is no longer a compiler plugin (it's a standalone source generator on the IntelliJ/Lint shared compiler APIs). KSP1 deprecated. Verify Hilt + Room + Apollo annotation processors all run on KSP2. Source: [KSP releases](https://github.com/google/ksp/releases), [KSP2 doc](https://github.com/google/ksp/blob/main/docs/ksp2.md). |
| Compose Compiler plugin | `org.jetbrains.kotlin.plugin.compose` 2.1.0 | **bumped in lockstep with Kotlin** (2.2.20) | Low | Already on the post-2.0 split (good). Plugin version === Kotlin version. |

### Compose & AndroidX UI

| Tech | Current | Target stable | Risk | Notes |
|------|---------|---------------|------|-------|
| Compose BOM | 2024.12.01 | **2026.05.00** | Med | Core libs jump 1.7.x → 1.11.1, Material3 jumps to 1.4.0. Many stable APIs (`PullToRefresh`, predictive back, anchored draggable, modifier-node migrations). Performance gains from lazy-layout improvements. Source: [BOM mapping](https://developer.android.com/develop/ui/compose/bom/bom-mapping), [Compose April '26 blog](https://android-developers.googleblog.com/2026/04/jetpack-compose-april-2026-updates.html). |
| AndroidX Lifecycle | 2.8.7 | **2.10.0** | Low | minSdk 23 → fine for us. Adds `rememberLifecycleOwner`, scoped ViewModels in Compose, nullable `SavedStateHandle`, new `CreationExtras` Kotlin builder. No breaking API changes from 2.8.7. Source: [Lifecycle release notes](https://developer.android.com/jetpack/androidx/releases/lifecycle). |
| AndroidX Activity Compose | 1.9.3 | **1.13.0** | Low | Predictive back fully wired; `enableEdgeToEdge()` default-true behaviour aligns with target API 36. |
| AndroidX Navigation Compose | 2.8.5 | **2.9.6** | Low | Stay on Navigation 2.x — Navigation 3 is the new artifact (`androidx.navigation3:*`) but is a rewrite; do **not** migrate this milestone (anti-feature per `PROJECT.md` constraint on new third-party SDKs / no architecture change). |
| AndroidX Paging | 3.3.5 | **3.3.6+** (Paging 3 line) | Low | Verify against latest stable on Maven; minor patch bump. |
| AndroidX DataStore Preferences | 1.1.1 | **1.2.1** | Low | Stable. 1.3.0-alpha07 ships encrypted DataStore but is still alpha — **do not adopt this milestone**. |
| AndroidX Security Crypto | 1.1.0 | **1.1.0 (frozen)** + flag deprecation | None this milestone | Officially **deprecated** in favour of DataStore + Tink. We continue to use it (no replacement in scope) but `core/data/.../ConnectionStore.kt` must be marked with a TODO and a follow-up milestone. Source: [security-crypto release notes](https://developer.android.com/jetpack/androidx/releases/security). |
| AndroidX Core KTX | 1.15.0 | **1.18.0** | Low | core-ktx is now mostly an empty artifact (KTX merged into `androidx.core:core`). Keep the dependency for compatibility. |
| AndroidX Compose Material3 | (via BOM 2024.12) | **1.4.0 (via BOM 2026.05)** | Med | Material3 expressive components, new `CarouselDefaults`, predictive-back-aware `ModalBottomSheet`. Review every `Modifier.draggable` / `swipeable` deprecation. |

### DI / Async / Serialization

| Tech | Current | Target stable | Risk | Notes |
|------|---------|---------------|------|-------|
| Hilt (Dagger) | 2.53.1 | **2.57.1** | Low | Routine bump. KSP2 support fully stable since 2.56. No API breaks expected. Source: [Dagger releases](https://github.com/google/dagger/releases). |
| Hilt Navigation Compose | 1.2.0 | **1.2.0** (still current stable) | None | No new stable in this line; bumps to 1.3.x are alpha. |
| kotlinx-coroutines | 1.9.0 | **1.11.0** | Low | 1.10.x deprecated `CoroutineDispatcher` as a context key; 1.11.0 advanced deprecation levels on `kotlinx-coroutines-test` and added lint warnings for passing `Job` into builders. Audit `core/common` for any `Job()` passed to `launch`. Source: [coroutines releases](https://github.com/Kotlin/kotlinx.coroutines/releases). |
| kotlinx-serialization | 1.7.3 | **1.9.0** (requires Kotlin 2.2) | Low | Includes serializers for `kotlin.time.Instant`. No breaking changes for our scope (JSON only, used by `:core:model` and `:core:data`). Source: [serialization changelog](https://github.com/Kotlin/kotlinx.serialization/blob/master/CHANGELOG.md). |

### Networking / GraphQL

| Tech | Current | Target stable | Risk | Notes |
|------|---------|---------------|------|-------|
| Apollo Kotlin | 4.1.0 | **4.4.3** (recommended) — **NOT 5.0** | Med | Stay on 4.x this milestone. Apollo 5.0 (May 2026) moved `apollo-normalized-cache-sqlite` to a separate repo (`apollo-kotlin-normalized-cache`), promoted some deprecation-warnings to errors, and tightened Kotlin compiler requirements (KGP 2.1+ on JVM, 2.3.10+ on KMP). 4.4.3 is the highest 4.x maintenance release with new-cache + AGP 9 fixes and is the lower-risk modernization target. Source: [Apollo Kotlin releases](https://github.com/apollographql/apollo-kotlin/releases). Apollo 5 should be a separate follow-up milestone. |
| OkHttp | 4.12.0 | **4.12.0 (keep)** or **5.3.0** (consider) | Low/Med | OkHttp 5.0 was promoted to stable in 2025 (with `DnsOverHttps` and several APIs marked stable). 5.x is binary-compatible for non-`@ExperimentalOkHttpApi` callers. **Recommend keeping 4.12.0 this milestone** — Apollo 4.x, Media3 1.10, and Coil 3.4 all transit OkHttp; staying on 4.12 keeps risk to a single shared transitive. Flag 5.x as a separate, isolated upgrade. Source: [OkHttp changelog](https://square.github.io/okhttp/changelogs/changelog/). |

### Media

| Tech | Current | Target stable | Risk | Notes |
|------|---------|---------------|------|-------|
| AndroidX Media3 | 1.9.1 (pinned — "1.10 needs compileSdk 36") | **1.10.0** | Med | Now legal once we bump `compileSdk` to 36 (which we are). 1.10 adds Material3-based playback widgets, expanded format support, improved Transformer speed. `minSdk` raised to 23 across Media3 (we're at 26 — fine). **Coordinate with `nextlib-media3ext` — confirm the matching `1.10.0-x.y.z` build exists before bumping.** Source: [Media3 1.10 release blog](https://android-developers.googleblog.com/2026/03/media3-110-is-out.html). |
| nextlib-media3ext | 1.9.1-0.11.0 | **1.10.0-x.y.z** (verify on Maven Central) | Med | Hard-pinned to the Media3 minor — bump in lockstep with Media3 only after confirming the corresponding release exists. If no 1.10 build exists by phase execution time, defer the Media3 bump. |

### Image Loading

| Tech | Current | Target stable | Risk | Notes |
|------|---------|---------------|------|-------|
| Coil | 3.0.4 | **3.4.0** | Low | Stable, OkHttp-network-transport unchanged. 3.5.0 is beta. Source: [Coil releases](https://github.com/coil-kt/coil/releases). |

### Storage / Persistence

| Tech | Current | Target stable | Risk | Notes |
|------|---------|---------------|------|-------|
| Room | 2.6.1 (declared, unused) | **2.8.4** (if kept in catalog) | Low | The catalog declares Room but **no module consumes it** (per `INTEGRATIONS.md`). Two options: (a) bump to 2.8.4 to keep the catalog evergreen, (b) **remove from `libs.versions.toml`** until a real consumer appears. Recommend **remove** — unused dependencies are technical debt, and Room 2.x has entered maintenance mode (Room 3.0 announced March 2026). |
| AndroidX ProfileInstaller | 1.4.1 | **1.4.1** (current stable) | None | No new stable. |

### Test / Perf

| Tech | Current | Target stable | Risk | Notes |
|------|---------|---------------|------|-------|
| AndroidX Benchmark Macrobenchmark | 1.3.3 | **1.4.x** (1.4.0-beta01 — **stay on 1.3.x stable**) | None | 1.4 is still beta as of 2026-05. Stay on 1.3.3 or its latest patch. Constraint: "stable releases only". |
| AndroidX Baseline Profile Gradle Plugin | 1.3.3 | **1.3.x latest** | None | Same as above. |
| AndroidX UI Automator | 2.3.0 | **2.3.0** | None | Current. |
| JUnit 4 | 4.13.2 | **4.13.2** | None | Current. |
| AndroidX Test ext-junit | 1.2.1 | **1.2.1** | None | Current. |

### Quality Gates

| Tech | Current | Target stable | Risk | Notes |
|------|---------|---------------|------|-------|
| Detekt | 1.23.7 | **1.23.8** (latest 1.x patch) — **NOT 2.0** | Low | Detekt 2.0 is alpha-only (built against Kotlin 2.3.21). Stay on 1.23.x. Verify `config/detekt/detekt.yml` against 1.23.8 rule changes. Source: [Detekt releases](https://github.com/detekt/detekt/releases). |
| ktlint (Gradle plugin) | 12.1.1 | **13.1.0** | Med | Major plugin bump. Underlying ktlint engine moves to 1.6.x. Re-format pass expected. Source: [ktlint plugin on plugins.gradle.org](https://plugins.gradle.org/plugin/org.jlleitschuh.gradle.ktlint). |
| OWASP dependencyCheck | 11.1.1 | **11.x latest patch** | Low | Still incompatible with configuration cache — keep the `--no-configuration-cache` flag. |
| Android Lint | from AGP | **from AGP 9.2** | Low | Lint rules updated significantly under AGP 9 (predictive-back, edge-to-edge defaults). Expect `app/lint-baseline.xml` to need a regeneration pass — schedule for the post-bump phase. |

---

## Compatibility Constraints (Pinned)

| Constraint | Source |
|------------|--------|
| AGP 9.2 → Gradle ≥ 9.4.1, JDK 17, BuildTools 36 | [AGP 9.2 release notes](https://developer.android.com/build/releases/agp-9-2-0-release-notes) |
| Compose BOM 2026.05 (core 1.11.1) → `compileSdk` 36 | [Compose BOM mapping](https://developer.android.com/develop/ui/compose/bom/bom-mapping) |
| Media3 1.10 → `compileSdk` 36, `minSdk` 23 | [Media3 1.10 blog](https://android-developers.googleblog.com/2026/03/media3-110-is-out.html) |
| Lifecycle 2.10 → `minSdk` 23 | [Lifecycle release notes](https://developer.android.com/jetpack/androidx/releases/lifecycle) |
| KSP version === Kotlin version line (KSP2 since 2.0+) | [KSP releases](https://github.com/google/ksp/releases) |
| Kotlin Compose Compiler plugin === Kotlin version | post-2.0 split, already correctly wired in `libs.versions.toml` |
| `-Xskip-metadata-version-check` already present (for nextlib built on Kotlin 2.3) | `build-logic/.../KotlinAndroid.kt:44` — likely **no longer needed** after Kotlin 2.2 bump if nextlib's pin still matches; verify and remove. |
| AGP 9 enables built-in Kotlin → `org.jetbrains.kotlin.android` plugin no longer required | [AGP 9 migration](https://kotlinlang.org/docs/multiplatform/multiplatform-project-agp-9-migration.html) |

---

## Migration Plan (Suggested Phase Order)

1. **Foundation bump (single commit-stream, build-only):** Gradle 9.4.1 → AGP 9.2.0 → Kotlin 2.2.20 → KSP matching → Compose Compiler plugin matching. compileSdk → 36. Build must stay green; no library bumps yet.
2. **AndroidX / Compose sweep:** Compose BOM 2026.05.00, Lifecycle 2.10.0, Activity 1.13.0, Navigation 2.9.6, Paging 3.3.6, DataStore 1.2.1, Core KTX 1.18.0.
3. **Async / DI / Serialization sweep:** Hilt 2.57.1, coroutines 1.11.0, serialization 1.9.0.
4. **Networking + Media + Image:** Apollo 4.4.3, Media3 1.10.0 (lockstep with nextlib), Coil 3.4.0. Keep OkHttp 4.12.0 for transitive stability.
5. **Quality gates + cleanup:** Detekt 1.23.8, ktlint plugin 13.1.0 (expect format churn), regenerate `app/lint-baseline.xml`. Remove unused Room entries from the catalog. Remove `-Xskip-metadata-version-check` if no longer needed.

Each phase should end with `assemble + unit tests + lint + detekt + connectedCheck` green and a fresh baseline-profile / macrobenchmark capture (per `PROJECT.md` "perf must be measured" constraint).

---

## What NOT to Use (this milestone)

| Avoid | Why | Use Instead |
|-------|-----|-------------|
| Kotlin 2.3.x | Detekt 2.0 (the only Kotlin-2.3-compatible quality gate) is still alpha; KSP/Hilt 2.3-track wheels still settling | Kotlin 2.2.20 (proven, all critical tools have stable wheels) |
| Detekt 2.0 | Alpha only as of 2026-05 | Detekt 1.23.8 |
| Apollo Kotlin 5.0 | Moves normalized-cache to a separate repo; promotes warnings to errors; KGP requirement bumped. Too much in one milestone. | Apollo 4.4.3 (in-line 4.x maintenance) |
| OkHttp 5.x | Transitive risk: Apollo 4.x, Media3, Coil all share the OkHttp client. Worth a focused, isolated milestone. | OkHttp 4.12.0 |
| Navigation 3 | New artifact, rewrite, violates "no new SDKs" + "no architecture changes" | Navigation Compose 2.9.6 |
| DataStore 1.3.0 (encrypted) | Alpha; the right destination for ConnectionStore but out of scope | DataStore 1.2.1 + keep `security-crypto` 1.1.0 with TODO |
| Migrating off `androidx.security:security-crypto` | Officially deprecated, but replacement (DataStore + Tink) is a non-trivial app-level migration; schedule separately | Keep 1.1.0; flag in CONCERNS.md |
| Macrobenchmark 1.4 / Baseline Profile Plugin 1.4 | Beta only as of 2026-05 | Stay on 1.3.3 |
| Removing `-Xskip-metadata-version-check` blindly | nextlib's Kotlin pin may still be ahead of ours | Verify nextlib's `kotlin_module` metadata after the Kotlin 2.2 bump; remove only if unneeded |

---

## Open Items for the Roadmap Phase

- **nextlib 1.10 availability** — Confirm `io.github.anilbeesetti:nextlib-media3ext:1.10.x-x.y.z` exists on Maven Central before committing to the Media3 bump.
- **R8 keep-rules audit** — AGP 9 changed wildcard semantics for runtime-invisible annotations; `app/proguard-rules.pro` (Apollo / kotlinx.serialization companions) needs an explicit re-spec.
- **Configuration cache + Gradle 9** — Re-verify OWASP `dependencyCheck` and the baselineprofile plugin under Gradle 9.4.1 configuration cache.
- **Context-receivers → context parameters** — `KotlinAndroid.kt` passes `-Xcontext-receivers`; in Kotlin 2.2 the preview replacement is `-Xcontext-parameters`. Audit call sites in `core/*` for the new syntax.
- **EncryptedSharedPreferences deprecation** — Flag in `CONCERNS.md` as a separate post-modernization milestone (DataStore + Tink migration for `ConnectionStore`).

---

## Sources

- [AGP 9.2.0 release notes](https://developer.android.com/build/releases/agp-9-2-0-release-notes) — JDK 17, Gradle 9.4.1, BuildTools 36, R8 keepattributes change (HIGH)
- [AGP about page](https://developer.android.com/build/releases/about-agp) — version matrix (HIGH)
- [Kotlin 2.2.0 release blog](https://blog.jetbrains.com/kotlin/2025/06/kotlin-2-2-0-released/) + [What's new 2.2.20](https://kotlinlang.org/docs/whatsnew2220.html) (HIGH)
- [Compose BOM mapping](https://developer.android.com/develop/ui/compose/bom/bom-mapping) — BOM 2026.05.00 → 1.11.1 / Material3 1.4.0 (HIGH)
- [Compose April '26 blog](https://android-developers.googleblog.com/2026/04/jetpack-compose-april-2026-updates.html) (HIGH)
- [Lifecycle release notes](https://developer.android.com/jetpack/androidx/releases/lifecycle) — 2.10.0, minSdk 23 (HIGH)
- [Media3 1.10 blog](https://android-developers.googleblog.com/2026/03/media3-110-is-out.html) — compileSdk 36, minSdk 23 (HIGH)
- [Apollo Kotlin releases](https://github.com/apollographql/apollo-kotlin/releases) — 5.0 split / 4.4.3 maintenance (HIGH)
- [Dagger/Hilt releases](https://github.com/google/dagger/releases) — 2.57.1 (HIGH)
- [kotlinx.coroutines releases](https://github.com/Kotlin/kotlinx.coroutines/releases) — 1.11.0 stable (HIGH)
- [kotlinx.serialization changelog](https://github.com/Kotlin/kotlinx.serialization/blob/master/CHANGELOG.md) — 1.9.0 (HIGH)
- [OkHttp changelog](https://square.github.io/okhttp/changelogs/changelog/) — 5.3.0 stable, 4.12 still maintained (HIGH)
- [Coil releases](https://github.com/coil-kt/coil/releases) — 3.4.0 stable, 3.5.0-beta (HIGH)
- [KSP releases](https://github.com/google/ksp/releases) + [KSP2 doc](https://github.com/google/ksp/blob/main/docs/ksp2.md) — KSP2 default (HIGH)
- [Detekt releases](https://github.com/detekt/detekt/releases) — 2.0 alpha-only (HIGH)
- [Gradle 9 upgrade guide](https://docs.gradle.org/current/userguide/upgrading_major_version_9.html) (HIGH)
- [security-crypto release notes](https://developer.android.com/jetpack/androidx/releases/security) — deprecation confirmed (HIGH)

---

*Stack research for: Slopper Android app modernization*
*Researched: 2026-05-16*
