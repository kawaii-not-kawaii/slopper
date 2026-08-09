# FFmpeg extension for Media3

Stash streams a wide variety of codec combinations. Android's MediaCodec covers
H.264/H.265/VP8/VP9/AV1 well but audio support is inconsistent — notably
**AC3, EAC3, DTS, TrueHD** fail on many devices.

The app uses **[nextlib](https://github.com/anilbeesetti/nextlib)** — a prebuilt
Media3 FFmpeg extension published to Maven Central — so this works out of the
box. No NDK setup, no local build step.

## How it's wired

- **Dependency** (`feature/player/build.gradle.kts`):
  ```kotlin
  implementation(libs.nextlib.media3ext)
  ```
- **Player factory** (`StashPlayerFactory.kt`):
  ```kotlin
  val renderersFactory = NextRenderersFactory(context)
      .setExtensionRendererMode(rendererMode.media3Value)
      .setEnableDecoderFallback(true)
  ```
- **Runtime detection** (`CodecCapabilities.kt`): reflectively checks
  `FfmpegLibrary.isAvailable()` so the Settings screen's codec card and the
  player's top-right badge show "HW+FF" when software decoders are active,
  "HW" otherwise.

## Supported decoders

**Audio:** Vorbis, Opus, FLAC, ALAC, PCM μ-law / A-law, MP3, AMR-NB/WB, AAC,
AC3, EAC3, DTS, TrueHD, MLP.

**Video:** H.264, HEVC, VP8, VP9. Renderer ordering determines whether these
run in software or hardware when both are available. Media3's `ON` mode puts
FFmpeg after the platform renderer (software fallback); `PREFER` puts FFmpeg
first (software preferred).

For a controlled comparison, build with
`-Pslopper.ffmpegRendererMode=prefer` or
`-Pslopper.ffmpegRendererMode=on`. The production default is `on`: Android's
hardware MediaCodec decoders are tried first and FFmpeg remains available only
as a fallback. See `tools/playback-ab/README.md` for the complete test and
capture protocol.

## Size cost

~6 MB per ABI (so ~12 MB added to the universal APK, or 6 MB if you ship per-
ABI splits — which we do). Native `.so` files are LGPL-licensed.

## Custom decoder set

If you need something nextlib doesn't ship (e.g. VC-1, MPEG-2), you can still
build a custom AAR via `android/tools/ffmpeg-extension/build.sh` and drop it
in `feature/player/libs/`. The Gradle config detects it automatically and adds
it alongside nextlib. Both factories co-exist fine.
