# Stopwatch for Light Phone 3

**Package:** `com.lightphone.stopwatch` · **Version:** 2.0 (v2)

A full-capability stopwatch built natively with the Light Phone SDK
(Kotlin + Jetpack Compose), same structure as the audiobook, Bible,
and Bump apps.

## Features

- Big centered time display (MM:SS.hh, hours appear automatically past
  60 minutes) with fixed-width digit slots so the numbers never wiggle
- Bottom-bar controls that change with state:
  - Fresh: **START**
  - Running: **LAP** / **STOP**
  - Paused: **RESET** / **START**
- Full lap tracking:
  - Live "current lap" row at the top while running
  - Each recorded lap shows the lap split (main number) and the running
    total (small, dimmed), newest first
  - **BEST** and **SLOWEST** tags appear once there are 2+ laps
- **History** (new in v2):
  - **HISTORY** button top-right on the main screen
  - Each finished run is saved automatically when you press **RESET**,
    listed by date and time with its total time and lap count, newest
    first
  - Tap any run to open it and see the full lap list (splits + running
    totals, with BEST/SLOWEST tags)
  - Delete a saved run from its detail page (two-tap confirm)
- Keeps perfect time even if you leave the app or the phone reboots —
  the start moment and laps are saved to on-device storage (DataStore),
  and elapsed time is always recomputed from the clock, so nothing
  drifts or gets lost
- Fully offline, no permissions required

## How history works

A run is captured the moment you press **RESET** (as long as it logged
some time). It stores when the run started, the total time, and every
lap. Up to 200 runs are kept; older ones roll off. Everything lives in
the same on-device DataStore as the live stopwatch state, so the list is
always in sync.

## Build & install

This is the complete light-sdk project with the stopwatch in the
`tool/` module.

1. Unzip, open the folder in Android Studio
2. Let Gradle sync
3. Build → `./gradlew :tool:assembleDebug` (or Run from Android Studio)
4. Install the APK from `tool/build/outputs/apk/debug/` onto the LP3
   via adb: `adb install -r tool-debug.apk`

`lighttool.toml` is set to `serverPackage = "com.lightos"` for the real
phone. For the SDK emulator, switch it to
`com.thelightphone.sdk.emulator`.

## Notes

- Timing is anchor-based (wall clock), not tick-counting — accuracy
  doesn't depend on the app staying in the foreground
- The big display uses Akkurat with per-digit fixed slots rather than
  the SDK's generic monospace fallback, keeping the LightOS look
