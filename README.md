# Codroid

Codroid is a lightweight Android source-code editor. It opens and saves text files using Android's system file picker, so files can be stored in device storage or a connected cloud provider without broad storage permissions.

## Features

- Editable, multiline monospace code area with cursor position and unsaved-change indicator.
- On-screen Tab action inserts four spaces at the cursor or replaces a selection.
- Create a fresh file, open an existing file, and save to a chosen location.
- Ctrl+O and Ctrl+S keyboard shortcuts for external keyboards.
- Remembers editor contents during screen rotation.
- App label: **Codroid**.

## Build the APK

Open this directory in Android Studio with JDK 17 and Android SDK Platform 35 installed, then allow Gradle sync. You can also build from a terminal with the included Gradle wrapper:

```shell
.\gradlew.bat assembleDebug
```

In Android Studio, choose **Build > Build Bundle(s) / APK(s) > Build APK(s)**.

The debug APK is written to:

`app/build/outputs/apk/debug/app-debug.apk`

The project uses the Android Gradle Plugin 8.7.3, which requires Gradle 8.9 and JDK 17.
