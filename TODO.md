# Slopper — TODO

1. Device UAT for v1.1 APK (`/home/yun/Slopper-0.2.0-alpha-arm64.apk` on Galaxy S23+) — carried from `.planning/STATE.md`.
2. Device-check merged #80 (HW video decode: codec badge should read HW for H.264/HEVC; background pause;
   PiP keeps playing) and #81 items (play duration ≈ real watch time, resume position saved on exit).
3. Optional follow-up: "custom" release-date range in FilterSheet DateSection reusing the criteria picker
   (arbitrary ranges are already reachable via the Stash Date criterion, so this is UX sugar only).
4. Release signing: generate a real release keystore, store it as GitHub secrets, and have `release.yml`
   build and sign release APKs on `v*` tags. Every install has to be uninstalled once because the key changes.
5. Optional: rename the old `v1.0` milestone tag (e.g. `milestone-v1.0`) so it doesn't sort above app tags.
6. Deferred toolchain debt: DEP-SDK37, DEP-COIL35, DEP-GRADLE96 (see `.planning/STATE.md`).
