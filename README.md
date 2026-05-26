# TeaVision

TeaVision (also referred to as TeaInfoapp) is an Android application for tea scanning, tea recognition, and local analytics. It performs on-device processing with CameraX, ML Kit, Room, and TensorFlow Lite.

## Overview

TeaVision is a mobile-first, on-device tea recognition and analytics app focused on privacy, speed, and offline capability. It lets users quickly capture a tea package or leaf sample with the camera, identifies the tea variety using a TensorFlow Lite model, and stores each scan locally for history and analytics. Key design goals are accuracy for common tea classes, minimal latency on-device inference, and a clear consumer workflow for scanning, confirming, and saving results.

Core architecture and flow:

- Camera capture: Uses CameraX to provide a stable preview and capture pipeline with automatic focus and exposure. The UI guides the user to position items within a scan frame.
- Preprocessing: Captured frames are cropped and resized to the model's expected input shape. Optional normalization and color-space conversion are applied to match training preprocessing.
- Inference: A TensorFlow Lite model (packaged in `app/src/main/assets/`) runs on-device. The model returns class probabilities which are post-processed to the top-N labels.
- Post-processing & lookup: The top prediction is enriched with label metadata and optional barcode lookup from `tea_barcodes.csv` to surface product details when available.
- Persistence: Each confirmed scan is saved locally using Room (SQLite) with timestamp, geolocation (optional), image thumbnail, predicted label(s), and confidence scores.
- Analytics: The app aggregates scan history to present usage patterns, frequent teas, and time-based charts — all computed locally and viewable in the Analytics screen.

Privacy and offline behavior:

- All core recognition, lookups, and analytics run locally; no user images or scan results are sent to a server by default.
- Network access (if enabled) is used only for optional online lookups or sync features and can be disabled in settings.

Extensibility and maintenance:

- Models and label files are stored in `assets/` so you can replace or upgrade the model without changing application logic.
- The model loading and label mapping code is modular: replace the TFLite file and the labels file and the app will use the new artifact at next build.
- The data layer (Room) is designed so additional fields (e.g., user notes, rating) can be added with a migration.

Use cases:

- Casual users who want to identify teas while shopping or at home.
- Tea vendors or researchers collecting local scan statistics for offline analysis.
- Developers experimenting with on-device ML for product or plant recognition who want a compact example app.

This README includes build instructions, model notes, and troubleshooting guidance for developers and contributors further down.

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

## Table of Contents

- Overview
- Features
- Requirements
- Quick Start
- Development Setup
- Model & Assets
- Run the App
- Testing
- Troubleshooting
- Project Structure
- Contributing
- License and Attribution

## Quick Start

1. Clone the repository:

```bash
git clone <repo-url> && cd <repo-folder>
```

2. Ensure the Android SDK and JDK are installed and `JAVA_HOME` is set (JDK 11 recommended).

3. If you don't have Android Studio, create a `local.properties` with your SDK path:

```
sdk.dir=C:\Users\<you>\AppData\Local\Android\Sdk
```

4. Build and install a debug APK:

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

On Windows use `gradlew.bat` instead of `./gradlew`.

## Development Setup

- Open the project in Android Studio (recommended) so it can download Gradle, sync dependencies, and configure the IDE.
- Use the Android SDK Manager to install required API levels (Android SDK 35) and the Android Emulator images if needed.
- Select a Java 11 JDK in Android Studio (Project Structure → SDK Location).
- Use the `gradlew` wrapper included at the repo root to ensure consistent Gradle versions across machines.

## Model & Assets

The app performs on-device inference using TensorFlow Lite models located in `app/src/main/assets/`.

- Models found in this repository (examples):
  - `teamodel.tflite`
  - `efficientnetv2s.tflite` (if present)
  - `efficientnet_v2_s.tflite`, `efficientnetv2s_tea.tflite` (root-level experimental models)
- Labels and lookups:
  - `tea_labels.txt` — class labels for the model
  - `tea_barcodes.csv` — barcode lookup table used by the app

Notes:

- When replacing or retraining models, keep the same filename or update the code that loads the model in the app.
- Large model files increase APK size — consider using Android App Bundles or model downloads at runtime if size matters.

## Run the App

- From Android Studio: Choose the `app` module and run on a connected device or emulator.
- From command line: build with `./gradlew assembleDebug` and install with `adb install -r` as shown in Quick Start.

## Testing

- Instrumentation and unit tests (if present) can be executed via Gradle:

```bash
./gradlew test         # unit tests
./gradlew connectedAndroidTest  # instrumentation tests on device/emulator
```

If no tests exist, consider adding lightweight unit tests around utility classes and instrumentation tests for the camera flow.

## Troubleshooting

- Gradle sync fails: run `./gradlew --refresh-dependencies` and check the SDK path in `local.properties`.
- Model load failures: confirm model file names in `app/src/main/assets/` and logcat output for exceptions.
- Camera permission problems: ensure runtime `CAMERA` permission is requested and granted; test on device/emulator that supports camera.
- Emulator camera not working: use a physical device or launch the emulator with camera passthrough enabled.

## Project Structure

- `app/` — Android app module
  - `src/main/java/` — Kotlin/Java source
  - `src/main/res/` — layouts, drawables, values
  - `src/main/assets/` — models and lookup data
- `gradle/`, `gradlew`, `gradlew.bat` — Gradle wrapper (keeps Gradle version consistent)
- `archive/Tea_backup/` — archived original wrapper scripts (created during repo consolidation)

## Contributing

- Please open issues for bugs or feature requests.
- For code contributions, fork the repository, create a feature branch, and open a Pull Request with a clear description and any testing steps.
- Keep changes small and focused; follow existing code style. Prefer adding tests for new logic.

## License and Attribution

This repository does not include a LICENSE file by default. If you intend to publish this project or accept external contributions, add a suitable open-source license (for example, MIT or Apache-2.0) and include attribution for any third-party models or assets used.

If you'd like, I can add a `LICENSE` file, CI configuration, or a CONTRIBUTING.md template next.
