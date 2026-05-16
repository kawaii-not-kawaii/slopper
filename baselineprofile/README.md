# :baselineprofile

Macrobenchmark module that generates a compiled [baseline profile] for the
`:app` release build. A baseline profile is a list of hot methods / Compose
call sites; ART uses it at install time so those paths are AOT-compiled on
first launch instead of interpreted, which removes the cold-start jitter.

## How to generate

Needs a connected Android 14+ device (or a managed / cloud-managed device
declared in this module's `baselineProfile { }` block). On a local phone:

```bash
cd android
./gradlew :app:generateBaselineProfile
```

This builds the `benchmark` variant of `:app`, installs it, then runs the
[`StashBaselineProfileGenerator`](src/main/java/io/stashapp/android/baselineprofile/StashBaselineProfileGenerator.kt)
macrobenchmark. The collector records every method + class ART touches during
the flow and writes the result to:

```
app/src/release/generated/baselineProfiles/baseline-prof.txt
```

That file is packaged into subsequent release APKs automatically by the
`androidx.baselineprofile` plugin.

## Journey covered

1. Cold start → first frame
2. Home screen rails populate
3. Library grid fling (3 pages)
4. Scene detail open / back

Feel free to extend the generator — more hot paths = better profile.

## Why not auto-run in CI?

Baseline profile generation is slow (tens of seconds per run), deterministic
only on a given device model, and requires a working Stash server on the
device to exercise the full app. We commit the generated `baseline-prof.txt`
and regenerate it periodically (before major releases, after Compose bumps,
after big new features) rather than on every PR.

[baseline profile]: https://developer.android.com/topic/performance/baselineprofiles/overview
