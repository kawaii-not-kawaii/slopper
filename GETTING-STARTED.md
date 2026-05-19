<!-- generated-by: gsd-doc-writer -->
# Getting Started

First-time contributor onboarding for Slopper — a native Android Compose
client for a self-hosted [Stash](https://github.com/stashapp/stash) GraphQL
server. By the end of this doc you'll have a debug APK built from a fresh
clone and installed on a phone or emulator.

Estimated time: **15–25 min** on a warm SDK, **45+ min** on a cold one (the
first Gradle run pulls a few hundred MB of dependencies).

---

## 1. Prerequisites

Tick each box before running the build. Order matters — the build will fail
loudly on missing JDK/SDK pieces but silently on a wrong toolchain version.

### 1.1 JDK 17

The Gradle toolchain is pinned to JDK 17 in `gradle.properties`
(`org.gradle.java.installations.auto-download=false` — Gradle will **not**
download a JDK for you).

```bash
java -version
# Expected: openjdk version "17.x.x"  (any vendor — Temurin, Zulu, Corretto, Microsoft)
```

If you don't have it:

```bash
# Debian / Ubuntu
sudo apt install openjdk-17-jdk

# Fedora
sudo dnf install java-17-openjdk-devel

# macOS (Homebrew)
brew install --cask temurin@17
```

Then export `JAVA_HOME` in your shell rc:

```bash
export JAVA_HOME="$(/usr/libexec/java_home -v 17 2>/dev/null || dirname "$(dirname "$(readlink -f "$(command -v javac)")")")"
export PATH="$JAVA_HOME/bin:$PATH"
```

### 1.2 Android SDK (platforms 35 + 36, build-tools 35 + 36)

Install Android Studio (easiest — it manages the SDK for you) **or** the
standalone cmdline-tools, then export `ANDROID_SDK_ROOT`:

```bash
export ANDROID_SDK_ROOT="$HOME/Android/Sdk"     # Linux default
# export ANDROID_SDK_ROOT="$HOME/Library/Android/sdk"   # macOS default
export PATH="$ANDROID_SDK_ROOT/platform-tools:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$PATH"
```

Install the required components:

```bash
sdkmanager --install \
  "platform-tools" \
  "platforms;android-35" \
  "platforms;android-36" \
  "build-tools;35.0.0" \
  "build-tools;36.0.0"

# Accept all SDK licenses (one-time):
yes | sdkmanager --licenses
```

### 1.3 `adb` on PATH

`adb` ships inside `platform-tools`. Verify:

```bash
adb --version
# Expected: Android Debug Bridge version 1.0.41 (or newer)
```

### 1.4 A device or emulator

Either works:

- **Physical phone** — enable Developer Options → USB debugging (and, for
  wireless install, Wireless debugging).
- **Emulator** — any AVD running API 26+ (`min-sdk = 26`).

Release-signing note: if you don't supply a release keystore, the release
build type falls back to the debug key (see `app/build.gradle.kts` →
`signingConfigs`). That APK is installable for personal sideload but is
**not** Play Store distributable.

---

## 2. First-time setup

```bash
# 1. Clone (substitute your fork / mirror URL)
git clone <your-slopper-remote> slopper
cd slopper

# 2. Generate the Gradle wrapper
#    Uses system `gradle` if present, otherwise downloads Gradle 8.11.1
#    into ~/.local/gradle-8.11.1 as a one-shot.
bash bootstrap.sh

# 3. Build a debug APK (first run: 5–15 min depending on network + cache)
./gradlew :app:assembleDebug
```

After `assembleDebug` succeeds you'll have per-ABI APKs under:

```
app/build/outputs/apk/debug/
├── app-arm64-v8a-debug.apk      # most modern phones
└── app-armeabi-v7a-debug.apk    # older 32-bit devices
```

---

## 3. Install on a device

### USB

```bash
# Confirm the device is visible
adb devices

# Install (most phones from 2017+ are arm64-v8a)
adb install -r app/build/outputs/apk/debug/app-arm64-v8a-debug.apk

# Or one-step build + install:
./gradlew :app:installDebug
```

### Wireless (Android 11+)

On the phone: **Developer options → Wireless debugging → Pair device with
pairing code**. Note the pairing IP/port + the (separate) connect port.

```bash
adb pair  <phone-ip>:<pair-port>      # enter the 6-digit code when prompted
adb connect <phone-ip>:<connect-port>
adb devices                            # phone should show as "device"
adb install -r app/build/outputs/apk/debug/app-arm64-v8a-debug.apk
```

---

## 4. Connect to your Stash server

1. Launch the app — a branded splash screen is shown while the connection
   check runs. This is normal; it clears automatically (≤ 3 seconds max).
2. First run drops you on the **Connection** screen.
3. Enter your Stash URL (e.g. `http://192.168.1.10:9999`) and API key.
4. Tap **Test** — you should see the server version + scene count.
5. Tap **Connect** — the library grid loads.

Credentials are persisted in encrypted prefs (Jetpack Security +
AndroidKeyStore), so subsequent launches skip straight to the library.

---

## 5. Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| `ERROR: JAVA_HOME is not set` | Shell can't find JDK 17 | `export JAVA_HOME=...` (see §1.1) and re-source your shell rc |
| `SDK location not found. Define ANDROID_SDK_ROOT…` | SDK env var missing | `export ANDROID_SDK_ROOT=$HOME/Android/Sdk` |
| `Failed to find Platform SDK with path: platforms;android-35` | SDK platform missing | `sdkmanager "platforms;android-35" "platforms;android-36"` |
| `Could not find build-tools 35.0.0` | Build-tools missing | `sdkmanager "build-tools;35.0.0" "build-tools;36.0.0"` |
| Gradle daemon dies / `OutOfMemoryError` on a 12 GB host | Heap math | Already mitigated — `gradle.properties` caps Gradle at `-Xmx2g`, Kotlin daemon at `-Xmx1500m`, `workers.max=2`. If you bumped these, revert. |
| First build hangs for minutes on "Resolve dependencies…" | Initial download of Apollo + Compose + Media3 + dependency-check NVD feed | Let it finish — subsequent builds use the local cache |
| `Toolchain installation … auto-detect: false` errors | Gradle wants to download a JDK but auto-download is disabled by design | Install JDK 17 locally (§1.1); do **not** flip `org.gradle.java.installations.auto-download` |
| App crashes immediately on launch | KSP stale after a dep bump | `./gradlew clean :app:assembleDebug` |

See `DEVICE_TESTING.md` for the full end-to-end smoke-test checklist
(connection → library → detail → player → resume sync).

---

## 6. Static checks

Run these before opening a PR:

```bash
./gradlew detekt ktlintCheck
```

Both are configured with baselines (pre-existing issues won't block the
build — only new violations will).

---

## 7. Release signing (optional)

Personal sideload builds don't need this — the release build falls back to
the debug key. If you need a real release key:

```bash
cp keystore.properties.example keystore.properties
# Edit keystore.properties with your real storeFile path / passwords / alias.
# keystore.properties is gitignored — never commit it.
./gradlew :app:assembleRelease
```

In CI, set `STASH_KEYSTORE_FILE`, `STASH_KEYSTORE_PASSWORD`,
`STASH_KEY_ALIAS`, `STASH_KEY_PASSWORD` env vars instead (see the
`signingConfigs` block in `app/build.gradle.kts`).

---

## 8. Where to next

- **`ARCHITECTURE.md`** — module graph, layering rules, GraphQL pipeline.
- **`DEVELOPMENT.md`** — day-to-day workflow: code style, module conventions,
  how to add a feature.
- **`DEVICE_TESTING.md`** — full smoke-test checklist for every major flow,
  plus logcat filters and common runtime failure modes.
