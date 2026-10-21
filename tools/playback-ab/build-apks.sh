#!/usr/bin/env bash
set -euo pipefail

script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
repo_root=$(cd -- "$script_dir/../.." && pwd)
output_dir=${1:-"$repo_root/captures/playback-ab/apks"}

mkdir -p "$output_dir"

build_variant() {
    local mode=$1

    "$repo_root/gradlew" \
        -p "$repo_root" \
        :app:assembleDebug \
        -Pslopper.ffmpegRendererMode="$mode" \
        -Pslopper.playbackDiagnostics=true

    local found=0
    while IFS= read -r -d '' apk; do
        found=1
        local apk_name
        apk_name=$(basename -- "$apk")
        cp -- "$apk" "$output_dir/slopper-${mode}-${apk_name}"
    done < <(find "$repo_root/app/build/outputs/apk/debug" -maxdepth 1 -type f -name '*.apk' -print0)

    if [[ $found -eq 0 ]]; then
        echo "No debug APKs were produced" >&2
        exit 1
    fi
}

# A preserves the original extension-first behavior. B makes FFmpeg a fallback.
build_variant prefer
build_variant on

(
    cd -- "$output_dir"
    sha256sum slopper-*.apk > SHA256SUMS
)

echo "A/B APKs written to $output_dir"
echo "A: slopper-prefer-* (FFmpeg first)"
echo "B: slopper-on-*     (MediaCodec first, FFmpeg fallback)"
