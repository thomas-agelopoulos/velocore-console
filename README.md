# VeloCore Console

Our own software for the Bowflex VeloCore bike's console: a clean, Apple-style replacement for the
stock screens, built from the design canvas.

The console is an Android 9 tablet (1920 × 1080). This repo is a single Android app written in
Kotlin and Jetpack Compose.

## What's in the first version

- **Home**: free ride, a suggested workout, quick starts, this week's minutes and the last ride.
- **Workouts**: nine workouts with their profile, plan and knob targets.
- **Riding**: watts against target, cadence, heart rate, knob cue and the workout timeline.
- **Summary**: totals, power against target, and how each climb or effort went. Rides are saved on
  the bike.

Ride numbers come from a **simulated rider** for now (the riding screen says "Simulated"). Reading
the bike's own sensors is the next step, once the app is on the console.

## Builds

Every push and pull request is built by GitHub Actions (`.github/workflows/build.yml`): unit tests,
then a signed APK attached to the run as `velocore-console-<run>.apk`. Pushes to `main` also publish
a release with the APK.

The app is signed with `app/signing/console.jks`, a key made only for this app, so each new build
installs over the last one. It is not the console's system key.

## Installing on the bike

With ADB authorized on the console:

```
adb connect <bike-ip>:5555
adb install -r velocore-console-<run>.apk
```

## Layout

The whole UI is laid out on a fixed 1920 × 1080 canvas and scaled to the screen
(`ui/Theme.kt`, `DesignCanvas`), so sizes in the code match the design canvas pixel for pixel.

Fonts: Inter, under the SIL Open Font License 1.1 (see `app/FONTS.md`).
