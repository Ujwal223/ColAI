# Developer Setup Guide

This guide will help you set up the development environment for ColAI on your local machine.

## Prerequisites

- **Flutter SDK**: `>=3.5.0`
- **Dart SDK**: `^3.5.0`
- **Android Studio** or **VS Code** with Flutter extensions installed.
- **Android Device or Emulator**: Running API 26 (Android 8.0) or higher.

## Installation Steps

1. **Clone the project** to your local machine.
2. **Install dependencies**:
   ```bash
   flutter pub get
   ```
3. **Run the app** on your connected device:
   ```bash
   flutter run
   ```

## Build Instructions

### Release APK
To build a production-ready universal APK:
```bash
flutter build apk --release
```

### Split APKs (Optimized for size)
To build architecture-specific APKs (recommended for distribution):
```bash
flutter build apk --split-per-abi
```

## Running Tests

We have a comprehensive test suite for all business logic and state management.

To run all tests:
```bash
flutter test
```

To run a specific test:
```bash
flutter test test/sessions_bloc_test.dart
```

## Security Note

This app uses encryption for local storage. The encryption keys are managed by `SecureCiphers`. During development, the app will generate its own keys locally.

## Contribution Policy

Please note: **External contributions (Pull Requests) are not accepted.** This project is maintained as a personal work under the **CC BY-NC 4.0** license.
