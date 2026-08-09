# Playback thermal A/B test

This test isolates renderer ordering:

- **A / `prefer`:** FFmpeg extension renderers are inserted before Android's
  MediaCodec renderers. This preserves the app's original behavior.
- **B / `on`:** Android MediaCodec renderers come first. FFmpeg remains
  available as a fallback for formats the platform cannot decode.

Both APKs include `SlopperPerf` logcat diagnostics for actual decoder names,
input format, dropped frames, frame-processing delay, decoder release balance,
and Android thermal-status callbacks. Stream URLs and titles are not logged.

## Build

```bash
tools/playback-ab/build-apks.sh
```

The APKs and `SHA256SUMS` are written under `captures/playback-ab/apks/`.
Debug APKs are used so the test does not replace the installed release app.
Choose the APK matching `adb shell getprop ro.product.cpu.abi`.

The equivalent manual build switches are:

```bash
./gradlew :app:assembleDebug \
  -Pslopper.ffmpegRendererMode=prefer \
  -Pslopper.playbackDiagnostics=true

./gradlew :app:assembleDebug \
  -Pslopper.ffmpegRendererMode=on \
  -Pslopper.playbackDiagnostics=true
```

Without properties, production builds use hardware-first `on` behavior and
diagnostics remain disabled. Pass `prefer` explicitly to reproduce the former
software-first behavior.

## Run

Use the same phone, queue order, playback duration, network, case, room
temperature, display refresh policy, and fixed brightness for every run. Start
each run only after the phone has returned to the same baseline temperature and
Android thermal status. Do not compare one already-hot run with one cold run.

For stronger evidence, run in counterbalanced order: A1, cool down, B1, cool
down, B2, cool down, A2. Keep the phone either connected for every run or on
wireless ADB for every run; USB charging itself produces heat.

1. Install A without clearing app data:

   ```bash
   adb install -r captures/playback-ab/apks/slopper-prefer-app-arm64-v8a-debug.apk
   ```

2. Start capture before launching the freshly installed app:

   ```bash
   tools/playback-ab/capture.sh A1-prefer
   ```

3. Launch the app, navigate to the exact test queue, and play it continuously.
   After the symptom occurs—or after the same
   fixed endpoint for a healthy run—press Ctrl-C in the capture terminal.
4. Let the phone cool to the original baseline, install B with `adb install -r`,
   and repeat with label `B1-on`.

If multiple devices are connected, set `ANDROID_SERIAL` before building or
capturing. The default package is `io.stashapp.android.debug`; see
`capture.sh --help` to override it.

## Captured evidence

Each run directory contains:

- `slopper-perf.log`: actual selected decoders, formats, dropped frames,
  processing delay, thermal status, and init/release counts.
- `samples.tsv`: total system CPU load, app process CPU, RSS, battery
  temperature, configured brightness, brightness mode, and best-effort actual
  display brightness.
- `thermal-zones.log`: readable thermal sensors and per-core frequency data.
- `start-snapshot.txt` / `end-snapshot.txt`: thermal service, battery, display,
  memory, and UI frame statistics.
- `device.txt`: device/build identity and capture settings.

Decoder names beginning with `ffmpeg` prove software decoding. Names beginning
with `c2.` or `OMX.` identify platform MediaCodec implementations. A decisive
result is B selecting a platform video decoder while showing materially lower
CPU/temperature/dropped frames for the same formats and duration.

## ADB limitations

ADB can reliably record process CPU, memory, configured brightness, Android's
thermal state, battery temperature, and any readable kernel thermal zones. It
cannot guarantee a true SoC junction temperature: many production phones hide
or rename thermal sensors, and battery temperature is a slower proxy. Adaptive
brightness also means the settings value may differ from panel output, so the
sampler separately attempts to read `BrightnessInfo` from `dumpsys display`.

For a final trace, Android Studio System Trace or Perfetto can add scheduler,
CPU-frequency, and UI-thread evidence. On supported Pixel devices, Android
Studio's Power Profiler can add device power-rail estimates.
