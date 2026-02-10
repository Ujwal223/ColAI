<img width="536" height="624" alt="ChatGPT Image Feb 10, 2026, 09_24_51 AM" src="https://github.com/user-attachments/assets/554a4652-4638-481d-8761-babbe3d32f3b" />
# ColAI 🚀

[![Flutter](https://img.shields.io/badge/Flutter-3.38.7-02569B?logo=flutter)](https://flutter.dev)
[![Dart](https://img.shields.io/badge/Dart-3.5.0-0175C2?logo=dart)](https://dart.dev)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE.md)
[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android)](https://www.android.com)

**Speed and privacy with an iOS-inspired UI for Android.**

ColAI is a premium Flutter-based hub that provides unified access to leading AI services (**ChatGPT, Claude, DeepSeek, Grok, Gemini, Perplexity**) with a focus on speed, privacy, and true multi-account isolation.

---

## ✨ Why ColAI?

*   **Privacy-First Headers**: Automatically sends DNT (Do Not Track) and Sec-GPC headers to all AI providers.

---

## 📱 Features & Showcase

| **Unified Dashboard** | **Isolated Sessions** | **Premium Controls** |
| :---: | :---: | :---: |
| Clean access to all AI providers in one stunning grid. | Manage multiple accounts with complete data privacy. | iOS-style navigation with glassmorphism and haptic feedback. |

---

## 🛠️ Tech Stack & Architecture

ColAI is built with a scalable, production-grade architecture.

| Component | Technology |
| :--- | :--- |
| **Framework** | **Flutter** (Stable 3.38.7) |
| **State Management** | **BLoC / Cubit** (flutter_bloc) |
| **Browser Engine** | **InAppWebView** (Custom Hardening) |
| **Security** | **AES-256** (Dual-layer encryption) |
| **Local Storage** | **SharedPreferences + Flutter Secure Storage** |
| **Native Integration** | **Kotlin** (Android Home Widget Support) |

---

## 🚀 Key Features

*   🎭 **True Multi-Session Support**: Run unlimited independent accounts per service.
*   🔒 **Encrypted Privacy**: Your session data is locked with AES-256 encryption.
*   📱 **Home Widget Support**: Quick-launch your favorite AI directly from the home screen.
*   🌓 **Adaptive Themes**: Seamless transition between Light and Dark modes with system synchronization.
*   🔽 **Offline Persistence**: Sessions persist across app restarts—stay logged in securely.
*   🌐 **Dynamic Content Blocking**: Built-in network-level tracker and banner blocking.

---

## 🏗️ Core Architecture Overview

ColAI follows the **Clean Architecture** pattern with a reactive state layer:

1.  **UI Layer**: Cupertino-based widgets with glassmorphism decorators.
2.  **Logic Layer (BLoC)**: Decoupled business logic for services, sessions, and theme.
3.  **Service Layer**: Hardened privacy services, notification managers, and secure storage adapters.
4.  **Security Layer**: Responsible for cookie interception, encryption, and env wiping.

---

## 🏁 Getting Started

### Prerequisites

- Flutter SDK `^3.38.7`
- Android Studio / VS Code
- Android Device (API 26+)

### Installation

1.  Clone the repository.
2.  Install dependencies:
    ```bash
    flutter pub get
    ```
3.  Run the app:
    ```bash
    flutter run
    ```

For detailed instructions, see [SETUP.md](SETUP.md).

---

## 🛡️ Security Policy

ColAI implements a custom **Session Privacy Service** that:
- Captures and encrypts cookies per session.
- Wipes the browser environment during switches.
- Prevents cross-site tracking and hardware ID linking.

---

## 📝 License & Contributing

ColAI is a personal project maintained by **Ujwal**.

**License**: This project is licensed under the [Apache License 2.0](LICENSE.md).

> [!WARNING]
> **No Contributions Expected**: The author does not seek or accept external contributions at this time. Unauthorized pull requests will be closed.

---

**Motto**: *"Speed and privacy with iOS-inspired UI for Android"*
