import 'package:flutter/material.dart';
import 'package:flutter_inappwebview/flutter_inappwebview.dart';
import 'package:ai_hub/models/theme_mode.dart';
import 'package:ai_hub/utils/user_agent_generator.dart';
import 'package:ai_hub/services/logger_service.dart';

class ServiceTheme {
  final Color backgroundColor;
  final Color primaryColor;
  final Color textColor;
  final bool isDark;

  const ServiceTheme({
    required this.backgroundColor,
    required this.primaryColor,
    required this.textColor,
    required this.isDark,
  });
}

class SystemThemeManager {
  static Future<void> setTheme(
      InAppWebViewController controller, AppThemeMode themeMode) async {
    // Theme forcing disabled by user request to prevent render crashes
  }

  static String getInitialThemeScript(AppThemeMode themeMode) {
    return '';
  }

  static List<ContentBlocker> getGlobalContentBlockers() {
    return [
      // Block common trackers and annoying popups at network level
      ContentBlocker(
        trigger: ContentBlockerTrigger(
          urlFilter:
              ".*branch\\.io.*|.*smartlook\\.com.*|.*appsflyer\\.com.*|.*doubleclick\\.net.*|.*google-analytics\\.com.*",
        ),
        action: ContentBlockerAction(
          type: ContentBlockerActionType.BLOCK,
        ),
      ),
      // Hide elements via CSS
      ContentBlocker(
        trigger: ContentBlockerTrigger(
          urlFilter: ".*",
          resourceType: [ContentBlockerTriggerResourceType.RAW],
        ),
        action: ContentBlockerAction(
          type: ContentBlockerActionType.CSS_DISPLAY_NONE,
          selector:
              ".apple-app-banner, .smart-app-banner, [class*='download-app'], [class*='AppBanner'], div[class*='MobileAppBanner'], div[class*='AppDownload'], button[aria-label*='download'], .cookie-banner, #consent-banner, .cmp-container, .gdpr-banner, [id*='cookie-consent']",
        ),
      ),
    ];
  }

  static InAppWebViewSettings getWebViewSettings(AppThemeMode themeMode,
      {String? url}) {
    return InAppWebViewSettings(
      userAgent: UserAgentGenerator.getUserAgent(url: url),
      javaScriptEnabled: true,
      domStorageEnabled: true,
      databaseEnabled: true,
      cacheEnabled: true,
      hardwareAcceleration:
          true, // Re-enable for performance, HC handles the composition
      useHybridComposition: true, // Required for Samsung A15/Mali stability
      sharedCookiesEnabled: false, // Extreme session isolation
      safeBrowsingEnabled: false, // Prevent cross-session hardware ID linking
      allowsLinkPreview: false, // Privacy: no pre-fetching
      allowFileAccess: true,
      allowContentAccess: true,
      contentBlockers: getGlobalContentBlockers(),
    );
  }

  static ServiceTheme getKnownServiceTheme(String url, bool isDarkMode) {
    final host = Uri.parse(url).host.toLowerCase();

    // AI Service Specific Themes
    if (host.contains('chatgpt.com')) {
      return ServiceTheme(
        backgroundColor: isDarkMode ? const Color(0xFF212121) : Colors.white,
        primaryColor: const Color(0xFF10A37F),
        textColor:
            isDarkMode ? const Color(0xFFECECEC) : const Color(0xFF2D2D2D),
        isDark: isDarkMode,
      );
    } else if (host.contains('claude.ai')) {
      return ServiceTheme(
        backgroundColor:
            isDarkMode ? const Color(0xFF1B1B1D) : const Color(0xFFF5F5F5),
        primaryColor: const Color(0xFFD97757),
        textColor: isDarkMode ? Colors.white : Colors.black,
        isDark: isDarkMode,
      );
    } else if (host.contains('deepseek.com') ||
        host.contains('chat.deepseek.com')) {
      return ServiceTheme(
        backgroundColor: Colors.black, // Official DeepSeek Black
        primaryColor: const Color(0xFF4D6BFE), // DeepSeek Blue
        textColor: Colors.white,
        isDark: true,
      );
    } else if (host.contains('grok.com')) {
      return ServiceTheme(
        backgroundColor: Colors.black, // Official Grok Black
        primaryColor: Colors.white,
        textColor: Colors.white,
        isDark: true,
      );
    } else if (host.contains('perplexity.ai')) {
      return ServiceTheme(
        backgroundColor:
            isDarkMode ? const Color(0xFF191A1A) : const Color(0xFFF3F3F3),
        primaryColor: const Color(0xFF20B2AA),
        textColor:
            isDarkMode ? const Color(0xFFE8E8E6) : const Color(0xFF131313),
        isDark: isDarkMode,
      );
    } else if (host.contains('gemini.google.com')) {
      return ServiceTheme(
        backgroundColor: isDarkMode ? const Color(0xFF131314) : Colors.white,
        primaryColor: const Color(0xFF4285F4),
        textColor: isDarkMode ? Colors.white : Colors.black,
        isDark: isDarkMode,
      );
    }

    // Default Fallback
    return ServiceTheme(
      backgroundColor: isDarkMode ? Colors.black : Colors.white,
      primaryColor: isDarkMode ? Colors.white : Colors.black,
      textColor: isDarkMode ? Colors.white : Colors.black,
      isDark: isDarkMode,
    );
  }

  static Future<Color?> detectThemeColor(
      InAppWebViewController controller) async {
    try {
      final result = await controller.evaluateJavascript(source: '''
        (function() {
          // 1. Try meta theme-color
          const meta = document.querySelector('meta[name="theme-color"]');
          if (meta && meta.content) return meta.content;
          
          // 2. Try prominent header background
          const header = document.querySelector('header, [class*="header"], [id*="header"]');
          if (header) {
            const bg = window.getComputedStyle(header).backgroundColor;
            if (bg !== 'rgba(0, 0, 0, 0)' && bg !== 'transparent') return bg;
          }

          // 3. Try primary button or accent (AI site specific)
          const primaryBtn = document.querySelector('button[type="submit"], .btn-primary, [class*="accent"], [class*="primary"]');
          if (primaryBtn) {
            const bg = window.getComputedStyle(primaryBtn).backgroundColor;
            if (bg !== 'rgba(0, 0, 0, 0)' && bg !== 'transparent') return bg;
          }
          
          // 4. Fallback to body background
          return window.getComputedStyle(document.body).backgroundColor;
        })();
      ''');

      if (result != null && result is String) {
        String res = result.trim();
        if (res.startsWith('#')) {
          return Color(int.parse(res.replaceFirst('#', '0xFF')));
        } else if (res.startsWith('rgb')) {
          // Handle rgb and rgba
          final match =
              RegExp(r'rgba?\((\d+),\s*(\d+),\s*(\d+)(?:,\s*[\d\.]+)?\)')
                  .firstMatch(res);
          if (match != null) {
            return Color.fromARGB(255, int.parse(match.group(1)!),
                int.parse(match.group(2)!), int.parse(match.group(3)!));
          }
        }
      }
    } catch (e) {
      Logger.error('Theme detection error', e);
    }
    return null;
  }
}
