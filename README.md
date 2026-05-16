# Stash Android

Native Android client for [Stash](https://github.com/stashapp/stash). Built with
Kotlin, Jetpack Compose, Apollo GraphQL, and Media3 (ExoPlayer + FFmpeg
extension).

## Status

**Foundation / MVP scaffolding.** The following works end-to-end in this commit:

- Connection flow (URL + API key, encrypted persistence, server info probe)
- Paginated scene grid (Plex-style dark UI, sprite thumbnails, resume bar)
- Search + filter sheet (sort, resolution, rating range, organized, has-markers, interactive flags)
- Scene detail (hero image, metadata, performer/tag/marker chips, resume action)
- Rating/organize/O-counter toggles on detail (optimistic UI, server-backed)
- Native Media3 player with queue, shuffle, repeat, next/prev, gestures, PiP
- Marker-seek: tap a marker in detail → player jumps to that timestamp
- Resume sync-back: position + play-count + play-history propagate to Stash
- Performers / Studios / Tags browse screens with search; tap filters the library
- Library interaction model: tap → detail; long-press → play-queue-from-here
- Modular Gradle + Hilt DI + Apollo generation against the Stash schema

See `TODO` at the bottom for what's intentionally stubbed.

## Project layout

```
android/
├── app/                        # Application module & navigation host
├── build-logic/                # Gradle convention plugins (keeps modules DRY)
├── gradle/libs.versions.toml   # Single source of truth for deps & versions
├── core/
│   ├── common/                 # Dispatchers, Result
│   ├── model/                  # Domain models (no Android deps)
│   ├── network/                # Apollo client, endpoint provider, auth
│   ├── domain/                 # Repository interfaces
│   ├── data/                   # Repository impls, encrypted prefs, mappers
│   ├── designsystem/           # Theme, SceneCard, reusable Compose pieces
│   └── ui/                     # Coil loader (API-key aware), routes
└── feature/
    ├── connection/             # Server setup screen
    ├── library/                # Scene browsing grid (Paging 3) + search + filter sheet
    ├── browse/                 # Performers / Studios / Tags grids (entity picker)
    ├── detail/                 # Scene detail — metadata, performers, tags, markers, actions
    ├── player/                 # Media3 wrapper, queue, gesture controls, activity sync
    └── settings/               # Codec status, browse entrypoints, disconnect
```

## Build

```bash
cd android
./bootstrap.sh                # one-time: downloads Gradle wrapper
./gradlew :app:assembleDebug  # builds per-ABI APKs into app/build/outputs/apk/debug/
```

See [`DEVICE_TESTING.md`](DEVICE_TESTING.md) for the full install-and-smoke-test
checklist — every major feature has a numbered verification step.

Requirements:
- JDK 17 (21 works too)
- Android SDK 35 (compile) / SDK 26 (min)
- For the FFmpeg extension: see `feature/player/FFMPEG_EXTENSION.md`

## GraphQL schema

`core/network/src/main/graphql/…/*.graphqls` are symlinks into
`../../graphql/schema/`. Changing the Stash server schema automatically
propagates — just re-run `:core:network:generateApolloSources`.

## Design direction

Plex-inspired dark UI tailored for Stash:
- Deep slightly-blue charcoal surfaces (avoids pure black OLED harshness)
- Warm amber accent (classic projector vibe, distinct from Plex orange)
- Teal secondary for progress/links
- Graduated surface elevation (5 tiers) for cards → modals
- Generous spacing, large tap targets, landscape-first for the player

See `core/designsystem/…/theme/Color.kt` for the palette.

## Native-Android features wired in now

- Picture-in-Picture (manifest flag + in-player button)
- MediaSession-ready player (`StashPlayerFactory` configures audio attrs + handle-becoming-noisy)
- Landscape auto-rotate in player with screen-on lock
- Encrypted API key at rest (Jetpack Security + AndroidKeyStore)
- Coil image loader injecting `ApiKey` header only for Stash-origin URLs

## TODO (next passes)

- [ ] Chromecast via Media3 Cast extension (low priority per product call)
- [ ] Offline downloads with `ExoPlayer`'s `DownloadService`
- [ ] Tag/performer/studio picker inside the filter sheet (currently only round-trips preselected ids)
- [ ] Scene marker creation from player (long-press timeline)
- [ ] HDR passthrough verification matrix
- [ ] Biometric app lock
- [ ] Pull-to-refresh on library + detail
- [ ] Android TV support (separate plan — Leanback / TV Compose)
- [ ] FFmpeg extension (build tooling is ready in `tools/ffmpeg-extension/`, skipped for now)

