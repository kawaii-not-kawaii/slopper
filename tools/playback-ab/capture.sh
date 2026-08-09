#!/usr/bin/env bash
set -euo pipefail

usage() {
    cat <<'EOF'
Usage: capture.sh LABEL [OUTPUT_DIR] [PACKAGE] [INTERVAL_SECONDS]

Capture phone and app metrics until Ctrl-C.

  LABEL             Run label such as A1-prefer or B1-on
  OUTPUT_DIR        Default: captures/playback-ab/<label>-<UTC timestamp>
  PACKAGE           Default: io.stashapp.android.debug
  INTERVAL_SECONDS  Default: 5

Set ANDROID_SERIAL when more than one ADB device is connected.
EOF
}

if [[ $# -lt 1 || ${1:-} == "-h" || ${1:-} == "--help" ]]; then
    usage
    [[ $# -ge 1 ]] && exit 0 || exit 2
fi

label=$1
package_name=${3:-io.stashapp.android.debug}
interval_seconds=${4:-5}

if ! [[ $interval_seconds =~ ^[1-9][0-9]*$ ]]; then
    echo "INTERVAL_SECONDS must be a positive integer" >&2
    exit 2
fi

script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
repo_root=$(cd -- "$script_dir/../.." && pwd)
utc_stamp=$(date -u +%Y%m%dT%H%M%SZ)
output_dir=${2:-"$repo_root/captures/playback-ab/${label}-${utc_stamp}"}

adb_cmd=(adb)
if [[ -n ${ANDROID_SERIAL:-} ]]; then
    adb_cmd+=(-s "$ANDROID_SERIAL")
fi

if [[ $("${adb_cmd[@]}" get-state 2>/dev/null) != "device" ]]; then
    echo "No authorized ADB device is available" >&2
    exit 1
fi

mkdir -p "$output_dir"

device_file="$output_dir/device.txt"
samples_file="$output_dir/samples.tsv"
thermal_file="$output_dir/thermal-zones.log"
logcat_file="$output_dir/slopper-perf.log"
start_snapshot="$output_dir/start-snapshot.txt"
end_snapshot="$output_dir/end-snapshot.txt"

{
    printf 'label=%s\n' "$label"
    printf 'capture_started_utc=%s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
    printf 'package=%s\n' "$package_name"
    printf 'interval_seconds=%s\n' "$interval_seconds"
    printf 'renderer_expected=%s\n' "$(case "$label" in *prefer*) echo prefer ;; *on*) echo on ;; *) echo unknown ;; esac)"
    "${adb_cmd[@]}" shell getprop ro.product.manufacturer | tr -d '\r' | sed 's/^/manufacturer=/'
    "${adb_cmd[@]}" shell getprop ro.product.model | tr -d '\r' | sed 's/^/model=/'
    "${adb_cmd[@]}" shell getprop ro.build.version.release | tr -d '\r' | sed 's/^/android_release=/'
    "${adb_cmd[@]}" shell getprop ro.build.version.sdk | tr -d '\r' | sed 's/^/sdk=/'
    "${adb_cmd[@]}" shell getprop ro.product.cpu.abi | tr -d '\r' | sed 's/^/abi=/'
    "${adb_cmd[@]}" shell wm size | tr -d '\r'
    "${adb_cmd[@]}" shell wm density | tr -d '\r'
} > "$device_file"

snapshot() {
    local phase=$1
    local target=$2
    {
        printf 'phase=%s\n' "$phase"
        printf 'utc=%s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
        printf '\n[battery]\n'
        "${adb_cmd[@]}" shell dumpsys battery | tr -d '\r'
        printf '\n[thermalservice]\n'
        "${adb_cmd[@]}" shell dumpsys thermalservice 2>&1 | tr -d '\r'
        printf '\n[display]\n'
        "${adb_cmd[@]}" shell dumpsys display 2>&1 | tr -d '\r' | grep -E \
            'DisplayDeviceInfo|supportedModes|activeMode|renderFrameRate|BrightnessInfo|mBrightnessInfo' || true
        printf '\n[brightness-settings]\n'
        printf 'mode='
        "${adb_cmd[@]}" shell settings get system screen_brightness_mode | tr -d '\r'
        printf 'value='
        "${adb_cmd[@]}" shell settings get system screen_brightness | tr -d '\r'
        printf '\n[meminfo]\n'
        "${adb_cmd[@]}" shell dumpsys meminfo "$package_name" 2>&1 | tr -d '\r'
        printf '\n[gfxinfo]\n'
        "${adb_cmd[@]}" shell dumpsys gfxinfo "$package_name" framestats 2>&1 | tr -d '\r'
    } > "$target"
}

snapshot start "$start_snapshot"
"${adb_cmd[@]}" shell dumpsys gfxinfo "$package_name" reset >/dev/null 2>&1 || true

# -T 1 starts close to capture time without clearing the device-wide log buffer.
"${adb_cmd[@]}" logcat -v epoch -T 1 'SlopperPerf:I' '*:S' > "$logcat_file" 2>&1 &
logcat_pid=$!

cleanup() {
    trap - INT TERM EXIT
    kill "$logcat_pid" 2>/dev/null || true
    wait "$logcat_pid" 2>/dev/null || true
    snapshot end "$end_snapshot"
    printf 'capture_ended_utc=%s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)" >> "$device_file"
    echo
    echo "Capture complete: $output_dir"
}
trap cleanup INT TERM EXIT

printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' \
    epoch_ms elapsed_s pid system_cpu_pct app_cpu_all_cores_pct app_cpu_one_core_pct \
    rss_kb battery_temp_c brightness_setting brightness_mode display_brightness > "$samples_file"

start_epoch=$(date +%s)
previous_process_ticks=""
previous_total_ticks=""
previous_idle_ticks=""
cpu_count=$("${adb_cmd[@]}" shell getconf _NPROCESSORS_ONLN 2>/dev/null | tr -d '\r' || true)
if ! [[ $cpu_count =~ ^[1-9][0-9]*$ ]]; then
    cpu_count=1
fi

echo "Capturing '$label' every ${interval_seconds}s. Start playback, then press Ctrl-C after the run."

while true; do
    epoch_seconds=$(date +%s)
    epoch_ms=$(date +%s%3N)
    elapsed_seconds=$((epoch_seconds - start_epoch))
    pid=$("${adb_cmd[@]}" shell pidof -s "$package_name" 2>/dev/null | tr -d '\r' || true)

    process_ticks=""
    total_ticks=""
    idle_ticks=""
    rss_kb=""
    if [[ $pid =~ ^[0-9]+$ ]]; then
        process_ticks=$("${adb_cmd[@]}" shell \
            "awk '{print \$14+\$15}' /proc/$pid/stat" 2>/dev/null | tr -d '\r' || true)
        rss_kb=$("${adb_cmd[@]}" shell \
            "awk '/^VmRSS:/ {print \$2}' /proc/$pid/status" 2>/dev/null | tr -d '\r' || true)
    fi
    cpu_tick_line=$("${adb_cmd[@]}" shell \
        "awk '/^cpu / {sum=0; for (i=2; i<=NF; i++) sum+=\$i; print sum, \$5+\$6}' /proc/stat" \
        2>/dev/null | tr -d '\r' || true)
    read -r total_ticks idle_ticks <<< "$cpu_tick_line" || true

    system_cpu=""
    cpu_all_cores=""
    cpu_one_core=""
    if [[ $process_ticks =~ ^[0-9]+$ && $total_ticks =~ ^[0-9]+$ &&
          $previous_process_ticks =~ ^[0-9]+$ && $previous_total_ticks =~ ^[0-9]+$ ]]; then
        process_delta=$((process_ticks - previous_process_ticks))
        total_delta=$((total_ticks - previous_total_ticks))
        if ((total_delta > 0 && process_delta >= 0)); then
            cpu_all_cores=$(awk -v p="$process_delta" -v t="$total_delta" \
                'BEGIN {printf "%.2f", 100 * p / t}')
            cpu_one_core=$(awk -v p="$process_delta" -v t="$total_delta" -v n="$cpu_count" \
                'BEGIN {printf "%.2f", 100 * n * p / t}')
        fi
    fi
    if [[ $total_ticks =~ ^[0-9]+$ && $idle_ticks =~ ^[0-9]+$ &&
          $previous_total_ticks =~ ^[0-9]+$ && $previous_idle_ticks =~ ^[0-9]+$ ]]; then
        total_delta=$((total_ticks - previous_total_ticks))
        idle_delta=$((idle_ticks - previous_idle_ticks))
        if ((total_delta > 0 && idle_delta >= 0)); then
            system_cpu=$(awk -v idle="$idle_delta" -v total="$total_delta" \
                'BEGIN {printf "%.2f", 100 * (total - idle) / total}')
        fi
    fi
    previous_process_ticks=$process_ticks
    previous_total_ticks=$total_ticks
    previous_idle_ticks=$idle_ticks

    battery_temp_raw=$("${adb_cmd[@]}" shell dumpsys battery 2>/dev/null | tr -d '\r' |
        awk -F: '/temperature/ {gsub(/[[:space:]]/, "", $2); print $2; exit}')
    battery_temp_c=""
    if [[ $battery_temp_raw =~ ^-?[0-9]+$ ]]; then
        battery_temp_c=$(awk -v t="$battery_temp_raw" 'BEGIN {printf "%.1f", t / 10}')
    fi

    brightness_setting=$("${adb_cmd[@]}" shell settings get system screen_brightness 2>/dev/null |
        tr -d '\r' || true)
    brightness_mode=$("${adb_cmd[@]}" shell settings get system screen_brightness_mode 2>/dev/null |
        tr -d '\r' || true)
    display_brightness=$("${adb_cmd[@]}" shell dumpsys display 2>/dev/null | tr -d '\r' |
        sed -nE 's/.*BrightnessInfo\{brightness=([^,}]+).*/\1/p' | head -n 1 || true)

    printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' \
        "$epoch_ms" "$elapsed_seconds" "$pid" "$system_cpu" "$cpu_all_cores" \
        "$cpu_one_core" "$rss_kb" "$battery_temp_c" "$brightness_setting" \
        "$brightness_mode" "$display_brightness" >> "$samples_file"

    {
        printf 'epoch_ms=%s elapsed_s=%s\n' "$epoch_ms" "$elapsed_seconds"
        "${adb_cmd[@]}" shell \
            'for zone in /sys/class/thermal/thermal_zone*; do
                if [ -r "$zone/type" ] && [ -r "$zone/temp" ]; then
                    printf "%s=" "$(cat "$zone/type")"
                    cat "$zone/temp"
                fi
            done' 2>&1 | tr -d '\r'
        "${adb_cmd[@]}" shell \
            'for cpu in /sys/devices/system/cpu/cpu[0-9]*; do
                file="$cpu/cpufreq/scaling_cur_freq"
                if [ -r "$file" ]; then printf "%s=" "${cpu##*/}"; cat "$file"; fi
            done' 2>&1 | tr -d '\r'
        printf '\n'
    } >> "$thermal_file"

    sleep "$interval_seconds"
done
