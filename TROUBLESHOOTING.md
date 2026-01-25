# Troubleshooting Guide

If you encounter issues with ColAI, please refer to this guide for common solutions.

## Common Issues

### 1. Services Stuck on Loading
- **Check Internet**: Ensure you have an active network connection.
- **Pull to Refresh**: Try swiping down on the home screen to reload the service list.
- **Clear App Cache**: Go to your Android System Settings > Apps > ColAI > Storage > Clear Cache.

### 2. Login Problems (SSO)
- **Problem**: Some services (like Google or Microsoft) might block logins within a WebView.
- **Solution**: Go to **Settings** in ColAI and enable **"SSO Login"**. This will open the login page in your system's secure browser (Chrome Custom Tabs), which is allowed by most providers.

### 3. Missing Favicons
- **Problem**: Icons for certain services don't appear.
- **Solution**: Long-press the service card on the home screen and select **"Refresh Favicon"**. ColAI will attempt to fetch a high-quality icon using multiple fallback strategies.

### 4. Webview Crashes
- **Problem**: A service page goes blank or crashes.
- **Solution**: ColAI has built-in auto-recovery for renderer crashes. If it persists, check if your system's "Android System WebView" is up to date in the Play Store.

## Privacy and Data

### Where are my cookies stored?
- Cookies are stored in an encrypted database isolated per session. They are never shared between sessions or leaked to other apps.

### How do I wipe all data?
- Go to **Settings** > **Manage Sessions** > **Clear Data**. This will securely erase all sessions, cookies, and custom services. or just **Clear Data** of app

## Support

As this is a personal project with a **no-contribution policy**, support is limited. However, you are welcome to use the code as permitted by the **Apache License 2.0**.
