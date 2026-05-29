# Simple Pomodoro

A clean, production-ready Pomodoro timer app built with Kotlin and Jetpack Compose.

## Features

- **Work Session**: 25 minutes (customizable)
- **Short Break**: 5 minutes (customizable)  
- **Long Break**: 15 minutes (customizable, after every 4 work sessions)
- **Controls**: START, PAUSE, RESET, SKIP buttons
- **Circular Progress Ring**: Visual timer indicator
- **Auto-switch**: Automatically transitions between work/break sessions
- **Session Counter**: Shows current session (e.g., 3/4)
- **Notifications**: Sound + vibration when timer ends, works in background
- **Keep Screen On**: Toggle in settings
- **Material 3 Design**: Modern UI with light/dark mode (follows system)
- **100% Offline**: No internet permission, no ads, no analytics, no login

## Technical Details

- **Minimum SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM-like with Service for timer
- **Storage**: DataStore Preferences
- **Foreground Service**: Timer runs reliably even when app is backgrounded

## Permissions

- `VIBRATE` - For notification vibration
- `POST_NOTIFICATIONS` - For timer completion notifications (Android 13+)
- `WAKE_LOCK` - To keep timer running
- `FOREGROUND_SERVICE` - For persistent timer service

## Build Instructions

### Prerequisites

1. Android Studio Hedgehog (2023.1.1) or later
2. JDK 17

### Steps to Build APK

1. **Open Project**
   - Launch Android Studio
   - Click "Open" and select the `SimplePomodoro` folder

2. **Sync Gradle**
   - Android Studio will automatically sync the project
   - Wait for "Gradle sync finished" message

3. **Build APK**
   - Go to menu: **Build > Build APK(s)**
   - Or use keyboard shortcut: `Ctrl+Shift+A` then type "Build APK"
   - Wait for build to complete

4. **Locate APK**
   - The APK will be at: `SimplePomodoro/app/build/outputs/apk/debug/app-debug.apk`
   - You can click "locate" in the build success notification

5. **Install on Device**
   - Transfer `app-debug.apk` to your Android device
   - Open the file to install (you may need to enable "Install from unknown sources")

### Command Line Build

If you have Android SDK command-line tools:

```bash
cd SimplePomodoro
./gradlew assembleDebug
```

The APK will be at: `app/build/outputs/apk/debug/app-debug.apk`

## Project Structure

```
SimplePomodoro/
├── app/
│   ├── src/main/
│   │   ├── java/com/simplepomodoro/
│   │   │   ├── MainActivity.kt          # Main activity & UI host
│   │   │   ├── data/
│   │   │   │   └── SettingsRepository.kt # DataStore settings
│   │   │   ├── domain/
│   │   │   │   └── TimerModels.kt        # Data classes
│   │   │   ├── service/
│   │   │   │   └── TimerService.kt       # Foreground timer service
│   │   │   └── ui/
│   │   │       ├── TimerScreen.kt        # Main screen composable
│   │   │       ├── SettingsScreen.kt     # Settings screen composable
│   │   │       └── TimerComponents.kt    # Reusable UI components
│   │   ├── res/
│   │   │   ├── drawable/                 # Vector assets
│   │   │   ├── mipmap-anydpi-v26/        # App icons
│   │   │   ├── values/                   # Colors, strings, themes
│   │   │   └── values-night/             # Dark theme overrides
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts
└── settings.gradle.kts
```

## Usage

1. **Start Timer**: Tap START button or use notification action
2. **Pause**: Tap PAUSE during a running session
3. **Reset**: Tap RESET to restart current session
4. **Skip**: Tap SKIP to move to next session
5. **Change Mode**: Use preset chips (25:00 Work, 5:00 Short, 15:00 Long)
6. **Settings**: Tap gear icon to customize durations and screen-on behavior

## License

This project is provided as-is for educational purposes.
