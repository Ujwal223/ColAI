# Troubleshooting

Common issues and solutions when building or using ColAI on Android.

---

## Web and Session Issues

### Page stuck loading or shows blank screen
- **Check internet connectivity**: Confirm your device has an active network connection.
- **Reload the session**: Tap the reload button in the top navigation bar.
- **Anti-bot challenges**: Some services (such as Cloudflare verification or Arkose Labs captchas) can take a few seconds to initialize within GeckoView.
- **Process memory limits**: Mozilla GeckoView runs web content in a separate child process. On devices with low RAM or aggressive memory killers, an out-of-memory event may terminate the tab process; reopening the service will restart the container session.

### Google Sign-In / SSO Authentication Popups
- Platforms such as Claude, Grok, and Anthropic use popup authentication flows (`window.open`).
- ColAI opens these authentication transactions in an isolated child popup session sharing the active container's `contextId`.
- Once authentication finishes with Google or your identity provider, the popup automatically transfers tokens back to the main session and dismisses.

### Sharing Images from Android ShareSheet
- When sharing images to ColAI, select the target AI provider and session.
- An attachment pill will appear at the bottom indicating the image is ready.
- Simply tap the attach button or paste in the chat composer; ColAI will auto-confirm the attachment through `GeckoPromptDelegate`.

### How session isolation works
ColAI assigns a separate `contextId` to each session. Cookies, storage, and caches are kept in separate directories by GeckoView. Signing in on one account does not affect another session for the same website.

---

## Home Screen Widgets

### Widget does not open the selected session
1. Go to **Settings** > tap **Widget Opens**.
2. Pick the provider and session you want the widget to open.
3. This saves your preference to storage and refreshes the widget immediately.
4. Tapping the widget will now launch directly into that chosen session.

### Bento Medium (4x2) widget fails to load
- If the widget does not appear immediately after being added to your home screen, launch the app once to trigger an initial update broadcast.
- Check your device launcher's background restrictions (e.g. battery savers that prevent widgets from updating).

---

## Security and PIN Locks

### Resetting a forgotten session PIN
If you forget the 4-digit PIN for a protected session:
1. Tap **Forgot PIN?** on the PIN prompt.
2. Enter the **Master Recovery Key** provided when you set up PIN protection.
3. Once verified, the PIN lock is removed and the session will open.

*Note*: The master recovery key is stored with PBKDF2 hashing in Android's encrypted storage. Keep your recovery key saved in a password manager.

---

## Permissions

### Cannot upload images or attachments
- Make sure photo and file permissions are enabled under Android **Settings** > **Apps** > **ColAI** > **Permissions**.
- ColAI uses Android's system photo picker and document picker so you only grant access to selected files.

### Microphone input not working
- Ensure the **Microphone** permission is granted in system app settings.
- If your device runs Android 12+, check that the system microphone privacy indicator turns on when you speak.

---

## Backups and Data Management

### Transferring data to a new device
1. In ColAI, open **Settings** > **Backup & Restore** > **Create Encrypted Backup**.
2. Enter a password. The app will write an AES-256-GCM encrypted `.colai` file to your chosen folder.
3. Copy the backup file to your new device.
4. On the new phone, choose **Restore Encrypted Backup** in Settings and enter your password.

### Erasing all app data
You can reset the app by going to Android **Settings** > **Apps** > **ColAI** > **Storage** > **Clear Data**. This deletes all containers, databases, and local keys.

---

## Reporting Issues

If you find a bug:
1. Search existing issues on [GitHub Issues](https://github.com/Ujwal223/ColAI/issues).
2. If it is new, open an issue including your device model, Android version, and steps to reproduce.
