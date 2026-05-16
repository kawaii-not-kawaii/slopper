#!/usr/bin/env bash
# Build the Media3 FFmpeg decoder extension AAR and drop it into
# feature/player/libs/.
#
# What this does:
#   1. Clones (or reuses) the androidx/media repo at the version pinned in
#      android/gradle/libs.versions.toml
#   2. Clones (or reuses) FFmpeg at the version the extension expects
#   3. Cross-compiles FFmpeg for each Android ABI requested
#   4. Builds the decoder_ffmpeg AAR
#   5. Copies the resulting AAR to feature/player/libs/
#
# Usage (on host):
#   ANDROID_NDK=/path/to/android-ndk-r26d ./build.sh
#
# Usage (Docker, no host NDK required):
#   docker build -t stash-ffmpeg-builder tools/ffmpeg-extension
#   docker run --rm -v "$PWD":/work -w /work stash-ffmpeg-builder \
#     tools/ffmpeg-extension/build.sh
#
# Supported ABIs can be overridden with ABIS="arm64-v8a" for faster single-arch
# builds. Default builds the two ABIs we ship in app/build.gradle.kts.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
WORK_DIR="${WORK_DIR:-$SCRIPT_DIR/work}"
OUT_DIR="$REPO_ROOT/feature/player/libs"

# Pin to match android/gradle/libs.versions.toml :: media3
MEDIA3_VERSION="${MEDIA3_VERSION:-1.5.1}"
# FFmpeg release the extension is tested against. Update together with MEDIA3.
FFMPEG_VERSION="${FFMPEG_VERSION:-6.0}"

ABIS="${ABIS:-arm64-v8a armeabi-v7a}"

# Codecs to enable. Focused on audio formats Android MediaCodec commonly misses
# (AC3/EAC3/DTS/TrueHD) plus a handful of lossless/vintage formats users
# occasionally encounter. Video decoders are intentionally excluded — hardware
# MediaCodec is nearly always faster and more power-efficient for video.
ENABLE_DECODERS=(
  vorbis opus flac alac pcm_mulaw pcm_alaw mp3
  amrnb amrwb aac ac3 eac3 dca mlp truehd
)

if [[ -z "${ANDROID_NDK:-}" ]]; then
  echo "error: ANDROID_NDK is not set. Install Android NDK r26 and export ANDROID_NDK=/path/to/ndk" >&2
  exit 1
fi

mkdir -p "$WORK_DIR" "$OUT_DIR"

# ---- 1. Media3 source ----
MEDIA3_DIR="$WORK_DIR/media"
if [[ ! -d "$MEDIA3_DIR" ]]; then
  echo ">> Cloning androidx/media @ $MEDIA3_VERSION"
  git clone --depth=1 --branch "$MEDIA3_VERSION" \
    https://github.com/androidx/media.git "$MEDIA3_DIR"
else
  echo ">> Reusing existing media clone at $MEDIA3_DIR"
fi

EXT_JNI_DIR="$MEDIA3_DIR/libraries/decoder_ffmpeg/src/main/jni"

# ---- 2. FFmpeg source ----
FFMPEG_DIR="$EXT_JNI_DIR/ffmpeg"
if [[ ! -d "$FFMPEG_DIR" ]]; then
  echo ">> Cloning FFmpeg @ n$FFMPEG_VERSION"
  git clone --depth=1 --branch "n$FFMPEG_VERSION" \
    https://github.com/FFmpeg/FFmpeg.git "$FFMPEG_DIR"
else
  echo ">> Reusing existing FFmpeg clone at $FFMPEG_DIR"
fi

# ---- 3. Build FFmpeg for each ABI ----
cd "$EXT_JNI_DIR"
HOST_PLATFORM_TAG="$(uname -s | tr '[:upper:]' '[:lower:]')-x86_64"
export HOST_PLATFORM="$HOST_PLATFORM_TAG"
export ANDROID_ABI=21

# The upstream build_ffmpeg.sh expects an ndkPath arg + api level + decoders.
# It internally loops ABIs it recognizes; we invoke per-ABI for clarity.
for abi in $ABIS; do
  case "$abi" in
    arm64-v8a) NDK_TRIPLET=aarch64-linux-android ;;
    armeabi-v7a) NDK_TRIPLET=armv7a-linux-androideabi ;;
    x86_64) NDK_TRIPLET=x86_64-linux-android ;;
    x86) NDK_TRIPLET=i686-linux-android ;;
    *) echo "unsupported ABI: $abi" >&2; exit 1 ;;
  esac
  echo ">> Building FFmpeg for $abi ($NDK_TRIPLET)"
  NDK_PATH="$ANDROID_NDK" \
  HOST_PLATFORM="$HOST_PLATFORM_TAG" \
  ANDROID_ABI="$abi" \
  ./build_ffmpeg.sh "$EXT_JNI_DIR" "android-21" "$ANDROID_NDK" "${ENABLE_DECODERS[*]}"
done

# ---- 4. Assemble AAR ----
echo ">> Assembling decoder_ffmpeg AAR"
cd "$MEDIA3_DIR"
./gradlew :libraries:decoder_ffmpeg:assembleRelease --no-daemon

# ---- 5. Copy AAR(s) into feature/player/libs/ ----
AAR_SRC=$(find libraries/decoder_ffmpeg/build/outputs/aar -name '*.aar' | head -n1)
if [[ -z "$AAR_SRC" ]]; then
  echo "error: AAR output not found" >&2
  exit 1
fi

cp "$AAR_SRC" "$OUT_DIR/media3-decoder-ffmpeg.aar"
echo ""
echo "Done. AAR installed at:"
echo "  $OUT_DIR/media3-decoder-ffmpeg.aar"
echo ""
echo "Rebuild the app to pick it up:"
echo "  cd android && ./gradlew :app:assembleDebug"
