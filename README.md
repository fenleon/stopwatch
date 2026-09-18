# Stopwatch for the Light Phone III

A simple stopwatch tool for the Light Phone III. Built with the
[light-sdk](https://github.com/lightphone/light-sdk).

## What it does

- Big time display. Tap it to start or stop. Double tap it to record a lap.
- RESET, STOP and LAP buttons. You can also use the volume keys:
  volume down starts or stops, volume up records a lap. When the watch is
  stopped, volume up resets it.
- Saves every run when you reset it. Runs are kept in History.
- In History you can open a run to see each lap, or tap the X to remove it.
- Gentle haptics on every button. The screen caps at 99:59.99 and 99 laps.

## Install

Download the APK from the
[releases page](https://github.com/fenleon/stopwatch/releases) and install it
on your Light Phone III. The phone must allow external tools
(Settings → External tools → "All tools"), because the APK is not signed by
Light.

## Build

```sh
./gradlew :tool:assembleRelease
```

The APK ends up in `tool/build/outputs/apk/release/`. It signs with the
light-sdk dev keystore. The build expects the light-sdk project next to this
folder (see `settings.gradle.kts`).

## License

Same license as the project this was forked from. See LICENSE.
