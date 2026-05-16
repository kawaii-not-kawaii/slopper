# External Integrations

**Analysis Date:** 2026-05-16

## APIs & External Services

**Stash Server (GraphQL):**
- The sole backend the app talks to is a user-hosted [Stash](https://github.com/stashapp/stash) instance — a self-hosted media organizer. There is no first-party cloud service.
- Endpoint shape: `${baseUrl}/graphql` (computed by `core/network/src/main/java/io/stashapp/android/core/network/StashEndpoint.kt`).
- Endpoint is fully runtime-configurable; the user enters URL + API key in the `:feature:connection` flow, persisted via `core/data/src/main/java/io/stashapp/android/core/data/prefs/ConnectionStore.kt`.
- SDK/Client: Apollo Kotlin 4.1.0 (`com.apollographql.apollo:apollo-runtime` + `apollo-normalized-cache-sqlite`). Configured in `core/network/build.gradle.kts` with package `io.stashapp.android.graphql` and custom-scalar passthroughs (`Time`, `Timestamp`, `Map`, `BoolMap`, `PluginConfigMap`, `Any`, `Int64`, `Upload`) all mapped to `kotlin.String`.
- Auth: per-request `ApiKey` header injected by `StashAuthInterceptor` in `core/network/src/main/java/io/stashapp/android/core/network/di/NetworkModule.kt` (lines 65-82). Key is read from `StashEndpointProvider.current()`.
- Operations (under `core/network/src/main/graphql/io/stashapp/android/graphql/operations/`):
  - `ServerInfo.graphql` — server probe / version check.
  - `FindScenes.graphql`, `FindScene.graphql` — scene paging + detail.
  - `FindEntities.graphql` — browse (performers/tags/studios/etc.).
  - `SceneActivity.graphql`, `SceneUpdate.graphql` — playback / mutation.

**Media streams (HTTP/HLS/DASH):**
- Stash serves direct video / HLS / DASH stream URLs. Consumed by AndroidX Media3 ExoPlayer in `feature/player` using `media3-exoplayer-hls`, `media3-exoplayer-dash`, and `media3-datasource-okhttp` so the shared OkHttp client (and Stash API key) is reused for streaming.

**Google Cast:**
- `androidx.media3:media3-cast` 1.9.1 is declared in `gradle/libs.versions.toml`. No live `CastPlayer` integration found in source (`grep` for `CastPlayer` / `media3.cast` in `:feature:player` returns no usages) — dependency available but unwired.

## Data Storage

**Databases:**
- Local relational DB: not present. Room artifacts (`androidx.room:room-runtime`, `room-ktx`, `room-paging`, `room-compiler`) are declared in `gradle/libs.versions.toml` but no module imports them (`grep -r "androidx.room"` in `*.kt` returns no hits). They are wired into the catalog as scaffolding for future use.
- Apollo normalized cache: `com.apollographql.apollo:apollo-normalized-cache-sqlite` 4.1.0 (declared in `core/network/build.gradle.kts`). Backs query result caching for Stash GraphQL responses.

**File Storage:**
- App-private internal storage only (Android sandbox). No external storage permissions in `app/src/main/AndroidManifest.xml`.
- DataStore preference files: `player_prefs` (see `core/data/.../PlayerPreferences.kt`) and the UI preferences DataStore in the same directory.
- `EncryptedSharedPreferences` file for credentials (`ConnectionStore.kt`).

**Caching:**
- Apollo SQLite normalized cache (above).
- OkHttp client has no `Cache` configured in `NetworkModule.kt` — relies on Apollo's cache + Coil's own disk/memory cache.
- Coil 3.0.4 — automatic in-memory + disk cache for images.

## Authentication & Identity

**Auth Provider:**
- None (no OAuth / Firebase / Supabase / etc.). Authentication is a static API key supplied by the user.
- Storage: AES-GCM `EncryptedSharedPreferences` (AndroidX Security Crypto 1.1.0) keyed by an AndroidKeyStore-backed `MasterKey` — see `core/data/src/main/java/io/stashapp/android/core/data/prefs/ConnectionStore.kt` lines 22-31 (`AES256_GCM` for values, `AES256_SIV` for keys).
- Wire format: `ApiKey: <value>` HTTP header added to every GraphQL request by `StashAuthInterceptor` (`core/network/.../di/NetworkModule.kt` line 78).

## Monitoring & Observability

**Error Tracking:**
- None. No Crashlytics, Sentry, Bugsnag, or equivalent SDK declared in `gradle/libs.versions.toml` or any module's `build.gradle.kts`.

**Logs:**
- OkHttp `HttpLoggingInterceptor` at `Level.BASIC` (method + URL only) — gated on `ApplicationInfo.FLAG_DEBUGGABLE` so release builds never log URLs / scene IDs / search terms (`NetworkModule.kt` lines 34-42).
- No structured logging library; the rest of the app uses `android.util.Log`.

## CI/CD & Deployment

**Hosting:**
- Sideload distribution only (no Play Store / F-Droid metadata in tree). Release signing config in `app/build.gradle.kts` reads either a `keystore.properties` file or `STASH_KEYSTORE_*` env vars.

**CI Pipeline:**
- No `.github/workflows/`, `.gitlab-ci.yml`, or other CI config files in the repo root. The OWASP `dependencyCheck` Gradle plugin (root `build.gradle.kts` lines 21-32) and the ktlint / detekt subproject configuration are CI-ready but not wired to an explicit pipeline.

## Environment Configuration

**Required env vars (release signing — optional):**
- `STASH_KEYSTORE_FILE` — Path to JKS keystore.
- `STASH_KEYSTORE_PASSWORD` — Keystore password.
- `STASH_KEY_ALIAS` — Signing key alias.
- `STASH_KEY_PASSWORD` — Key password.

**Other configuration files (existence noted, contents not read):**
- `local.properties` — Standard Android SDK location, per-machine.
- `keystore.properties` — Optional; same fields as the env vars above. Template at `keystore.properties.example`.
- No `.env` files present.

**Secrets location:**
- Build-time signing secrets: `keystore.properties` or env vars (never committed).
- Runtime secrets (Stash API key): user-entered, persisted only in `EncryptedSharedPreferences`.

## Webhooks & Callbacks

**Incoming:**
- None. `app/src/main/AndroidManifest.xml` declares a single `MainActivity` with only the `MAIN` / `LAUNCHER` intent filter — no deep-link `<data>` filters, no broadcast receivers, no exported services.

**Outgoing:**
- None. The app only initiates GraphQL queries / mutations and HTTP media streams against the user's configured Stash server.

## Third-Party SDKs (summary)

| SDK | Version | Module | Purpose |
|-----|---------|--------|---------|
| Apollo Kotlin | 4.1.0 | `:core:network` | GraphQL client + SQLite normalized cache |
| OkHttp | 4.12.0 | `:core:network`, `:core:ui`, `:feature:player` | Shared HTTP client + logging interceptor |
| AndroidX Media3 | 1.9.1 | `:feature:player` | ExoPlayer (HLS/DASH/Session/Cast/OkHttp datasource) |
| nextlib-media3ext | 1.9.1-0.11.0 | `:feature:player` | Prebuilt FFmpeg extension (AC3/EAC3/DTS/TrueHD + sw H.264/HEVC/VP8/VP9) — `io.github.anilbeesetti:nextlib-media3ext` |
| Coil 3 | 3.0.4 | `:core:designsystem`, `:core:ui` | Image loading (`coil-compose` + `coil-network-okhttp`) |
| Hilt | 2.53.1 | All `stash.android.hilt` consumers | DI |
| AndroidX DataStore Preferences | 1.1.1 | `:core:data` | Player / UI preferences |
| AndroidX Security Crypto | 1.1.0 | `:core:data` | Encrypted credential storage |
| AndroidX Paging | 3.3.5 | `:core:data`, `:feature:library`, `:feature:browse` | Paged scene / entity lists |
| kotlinx.serialization | 1.7.3 | `:core:model`, `:core:data` | JSON serialization for persisted models |

## Required Android Permissions

From `app/src/main/AndroidManifest.xml`:
- `android.permission.INTERNET`, `android.permission.ACCESS_NETWORK_STATE` — Stash GraphQL + media streaming.
- `android.permission.FOREGROUND_SERVICE`, `android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `android.permission.POST_NOTIFICATIONS` — Background audio playback (Media3 `MediaSession`).
- `android:usesCleartextTraffic="true"` + scoped `network_security_config.xml` — Stash on LAN commonly runs plain HTTP; user CAs are intentionally excluded from the release trust store.

---

*Integration audit: 2026-05-16*
