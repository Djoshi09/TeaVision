# TeaVision

TeaVision (also referred to as TeaInfoapp) is an Android application for tea scanning, tea recognition, and local analytics. It performs on-device processing with CameraX, ML Kit, Room, and TensorFlow Lite.

## Overview

The app is designed to help users scan tea items, identify tea types, and review scan history and analytics. It uses local assets and on-device inference so core features work without a server.

## Features

- Camera-based scanning and tea recognition
- Barcode and text lookup support
- Local tea history storage
- Analytics view for scan activity
- Material Design interface
- On-device TensorFlow Lite model loading

## Requirements

- Android Studio
- JDK 11
- Android SDK 35
- A device or emulator with camera support for scanning features

## How to Use

1. Open the project in Android Studio.
2. Let Gradle sync finish.
3. Run the `app` module on a connected device or emulator.
4. Grant camera permission when prompted.
5. Use the main screen to start scanning and review tea details.

## Build From Terminal

If you want to build without Android Studio, run this from the outer project folder:

```bash
./gradlew assembleDebug
```

On Windows, you can use:

```bat
gradlew.bat assembleDebug
```

## Important Files and Folders

- `app/` application source code, resources, and assets
- `app/src/main/java/` Kotlin source code
- `app/src/main/res/` layouts, drawables, and values
- `app/src/main/assets/` model files and local lookup data
- `gradle/` Gradle wrapper configuration
- `gradlew` and `gradlew.bat` Gradle wrapper scripts
- `build.gradle.kts` and `settings.gradle.kts` project build setup

## Assets

The app expects its model and lookup files to stay in `app/src/main/assets/`. Do not move or rename these files unless you also update the code that loads them.

## Permissions

The app requests these permissions:

- `CAMERA` for scanning
- `VIBRATE` for feedback
- `INTERNET` for network-based lookups if used

## Project Notes

- Keep generated files such as `.idea/`, `.gradle/`, `build/`, and `local.properties` out of GitHub.
- The repository is set up so only the required source files, assets, and Gradle wrapper files are tracked.

## Troubleshooting

- If Gradle cannot find the Android SDK, set the SDK path in Android Studio or create a local `local.properties` file on your machine.
- If the app fails to load the model, confirm that the files in `app/src/main/assets/` are present and named correctly.
- If scanning does not start, verify that camera permission has been granted on the device.

## Build Tip

If you make changes to the model or asset files, rebuild the app so the updated files are packaged again.
