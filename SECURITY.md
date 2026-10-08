# Security Policy for ColAI

ColAI is engineered with privacy and container security as primary design tenets. The application employs hardware-backed cryptographic vaults, strict Mozilla GeckoView container isolation (`contextId`), and hardened intent and network security boundaries to protect user authentication sessions and AI interactions.

---

## 1. Supported Versions

Security updates, bug fixes, and engine patches are actively maintained for the following releases:

| Version | Supported | Status |
| :--- | :--- | :--- |
| **2.0.x** | :white_check_mark: | Current Active Release |
| **1.x** | :x: | Deprecated / End of Life |

---

## 2. Reporting a Vulnerability

We appreciate the responsible security research community and welcome vulnerability disclosures.

If you believe you have discovered a vulnerability, security flaw, or privacy weakness in ColAI, please report it privately:

- **Primary Contact:** [notujwal@proton.me](mailto:notujwal@proton.me) or directly via [GitHub Security Advisories](https://github.com/Ujwal223/ColAI/security/advisories)
- **Response Time:** We aim to acknowledge receipt of all vulnerability reports within **72 hours** and provide an initial assessment within **7 business days**.

### Please Include:
1. Clear description of the vulnerability and attack scenario.
2. Steps to reproduce the issue (proof of concept, request dumps, or code sample).
3. Affected components, files, or versions.
4. Any potential remediation suggestions.

> **Note:** Please refrain from publicly disclosing the issue or publishing proofs-of-concept until a patch has been released and coordinated disclosure has completed.

---

## 3. Security Architecture & Threat Model

### A. Isolated Container Architecture
- **Context Isolation:** Each session container operates with an isolated Mozilla Gecko `contextId`. Cookies, Web Storage, IndexedDB, HTTP Auth, and cache partitions are strictly segregated between different AI provider sessions and user accounts.
- **SSO & Popup Protection:** OAuth and Single Sign-On popups are isolated to legitimate authentication workflows, preventing cross-origin session leaks or white-screen state crashes.
- **Navigation Bounds:** Links navigating off-service or external web addresses are intercepted and routed to the user's default system browser rather than executed inside the privileged container. In-session redirects are verified against allowlisted ecosystem domains.

### B. Hardware-Backed Cryptography
- **Vault Encryption:** Local application preferences and state secrets are stored in `EncryptedSharedPreferences` backed by the Android KeyStore `MasterKey` utilizing:
  - **AES-256-SIV** for deterministic key encryption
  - **AES-256-GCM** for authenticated payload encryption
- **Constant-Time Verification:** Session PIN locks and master recovery keys are verified using constant-time comparison (`MessageDigest.isEqual`) to protect against timing side-channel attacks.
- **Encrypted Backups:** Full-fidelity backup files (`colai-backup-v1`) are encrypted using **PBKDF2** key derivation (100,000 iterations with HMAC-SHA256, cryptographically random 16-byte salt, and 12-byte IV) coupled with **AES-256-GCM** authenticated encryption with 128-bit authentication tags.
- **Archive Extraction Hardening:** Archive extraction enforces strict canonical path checking with directory separator verification to guard against Zip Slip attacks.

### C. Network & Platform Security
- **Strict HTTPS / No Cleartext Traffic:** `android:usesCleartextTraffic="false"` is enforced alongside an explicit `network_security_config.xml`. All application network operations and logo fetches enforce TLS/HTTPS.
- **Content Permissions Least-Privilege:** Silently auto-granting sensitive Web permissions (such as HTML5 Geolocation) is prohibited. Non-essential content permissions are denied by default.
- **Scoped File Provider:** `FileProvider` paths are strictly scoped to designated temporary cache directories (`cache/shared/`, `cache/uploads/`), preventing exposure of root external storage or internal app data directories.
- **Safe Download Handling:** All downloaded filenames from `Content-Disposition` or URIs are sanitized, stripping path traversal sequences (`../`, null bytes, and path delimiters) before writing to the public MediaStore Downloads directory.

---

## 4. Responsible Disclosure & Bounty

While ColAI is currently an open-source community project without a dedicated commercial bug bounty fund, we publicly credit and recognize all ethical researchers who report valid vulnerabilities in our release notes and Security Hall of Fame.
