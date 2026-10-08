# Setup Guide

This document describes how to set up your development environment and build ColAI from source.

---

## 1. Prerequisites

Before building the app, make sure you have the following installed:

- **Java Development Kit**: JDK 21 (Adoptium Temurin, Azul Zulu, or OpenJDK)
- **Android SDK**: Compile & Target SDK 35, Minimum SDK 26
- **Android NDK**: Required for GeckoView native libraries
- **IDE**: Android Studio (Ladybug or newer) or any code editor with Gradle support

### Setting up JDK and SDK

#### Linux
Depending on your package manager:

```bash
# Ubuntu / Debian
sudo apt update && sudo apt install -y openjdk-21-jdk android-sdk

# Fedora
sudo dnf install -y java-21-openjdk-devel android-tools

# Arch Linux
sudo pacman -S jdk21-openjdk android-tools
sudo archlinux-java set java-21-openjdk
```

#### macOS
Using Homebrew:

```bash
brew install openjdk@21 android-platform-tools
sudo ln -sfn $(brew --prefix openjdk@21)/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-21.jdk
```

#### Windows
1. Install JDK 21 from [Adoptium](https://adoptium.net/) or Oracle.
2. Add the JDK installation directory to your `JAVA_HOME` environment variable.
3. Append `%JAVA_HOME%\bin` to your system `Path`.
4. Install [Android Studio](https://developer.android.com/studio) to install and manage Android SDK packages.

---

## 2. Cloning the Codebase

```bash
git clone https://github.com/Ujwal223/ColAI.git
cd ColAI
```

Check your Java version before proceeding:
```bash
java -version
# Should output openjdk version "21.x.x"
```

---

## 3. Building with Gradle

The project includes the standard Gradle wrapper scripts.

### Build Debug APK
```bash
# Linux / macOS
./gradlew assembleDebug

# Windows
gradlew.bat assembleDebug
```
The output file is located at `app/build/outputs/apk/debug/app-debug.apk`.

### Installing to a Device or Emulator
Connect an Android device with USB debugging enabled (or start an emulator):
```bash
# Verify connection
adb devices

# Install directly
./gradlew installDebug
```

### Build Release APKs
```bash
# Linux / macOS
./gradlew assembleRelease

# Windows
gradlew.bat assembleRelease
```
The resulting split APKs (`arm64-v8a`, `armeabi-v7a`, `x86_64`) and universal APK are saved in `app/build/outputs/apk/release/`.

---

## 4. Running Unit Tests

Run the local unit test suite:
```bash
./gradlew testDebugUnitTest
```

To run a specific test:
```bash
./gradlew testDebugUnitTest --tests "com.ujwal.colai.core.deeplink.DeepLinkResolverTest"
```

---

## 5. Keystore Configuration

By default, debug builds sign using Android's standard debug keystore.

For signing release builds locally, specify your keystore details in `gradle.properties` or environment variables:
```properties
RELEASE_STORE_FILE=/path/to/keystore.jks
RELEASE_STORE_PASSWORD=your_store_password
RELEASE_KEY_ALIAS=your_key_alias
RELEASE_KEY_PASSWORD=your_key_password
```

See `.env.example` for optional local environment parameters. Never check keystore files or passwords into version control.

---

## 6. Project Layout

```
ColAI/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── assets/              # App images and logos
│   │   ├── kotlin/com/ujwal/colai/
│   │   │   ├── core/            # Database (Room), Engine (GeckoView), Security (AES-256)
│   │   │   ├── feature/         # Compose screens (Home, Web, Settings, About)
│   │   │   └── util/            # Resolvers, haptics, helpers
│   │   └── res/                 # Layouts, widgets, drawables, themes
│   └── build.gradle.kts         # App build configuration
├── gradle/libs.versions.toml    # Version catalog
├── scripts/                     # Update checks and utilities
└── build.gradle.kts             # Root Gradle build script
```
