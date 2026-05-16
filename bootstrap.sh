#!/usr/bin/env bash
# One-time bootstrap: install a Gradle wrapper + verify SDK/NDK presence so
# subsequent `./gradlew :app:assembleDebug` runs work out of the box.
#
# Idempotent — safe to re-run. Skips downloads if the wrapper already exists.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

GRADLE_VERSION="${GRADLE_VERSION:-8.11.1}"

# --- 1. Ensure a Gradle wrapper exists --------------------------------------
if [[ ! -f "gradlew" ]]; then
  echo ">> Installing Gradle $GRADLE_VERSION + wrapper"

  # Try system gradle first — fastest, most common
  if command -v gradle >/dev/null 2>&1; then
    gradle wrapper --gradle-version "$GRADLE_VERSION" --distribution-type bin
  else
    # Fall back to a one-shot tarball download into ~/.local/gradle
    GRADLE_HOME="$HOME/.local/gradle-$GRADLE_VERSION"
    if [[ ! -x "$GRADLE_HOME/bin/gradle" ]]; then
      mkdir -p "$HOME/.local"
      tmp="$(mktemp -d)"
      url="https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
      echo "   downloading $url"
      curl -fsSL -o "$tmp/gradle.zip" "$url"
      # The zip already contains a top-level `gradle-$VERSION/` directory, so
      # unzipping directly into ~/.local lands us at $GRADLE_HOME.
      unzip -q "$tmp/gradle.zip" -d "$HOME/.local"
      rm -rf "$tmp"
    fi
    "$GRADLE_HOME/bin/gradle" wrapper --gradle-version "$GRADLE_VERSION" --distribution-type bin
  fi
fi

# --- 2. Verify SDK -----------------------------------------------------------
if [[ -z "${ANDROID_SDK_ROOT:-}${ANDROID_HOME:-}" ]]; then
  echo ""
  echo "warning: neither ANDROID_SDK_ROOT nor ANDROID_HOME is set."
  echo "         Install Android Studio or cmdline-tools, then export:"
  echo "         export ANDROID_SDK_ROOT=\$HOME/Android/Sdk"
  echo ""
fi

cat <<EOF
Bootstrap complete.

Build a debug APK:
  ./gradlew :app:assembleDebug

Install on a connected device:
  adb install -r app/build/outputs/apk/debug/app-arm64-v8a-debug.apk

Or, for one-step build+install to a connected device:
  ./gradlew :app:installDebug

Generate Apollo GraphQL sources (runs automatically as part of assemble):
  ./gradlew :core:network:generateApolloSources
EOF
