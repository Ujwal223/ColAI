// Session privacy service for ColAI.
//
// Manages the complete isolation of browser sessions for different accounts.
// This is the secret sauce that makes multi-account management possible without
// any data leakage between sessions.
//
// How session isolation works:
// 1. Each session gets its own encrypted cookie storage
// 2. Cookies are completely wiped before switching sessions
// 3. Only that session's cookies are restored
// 4. All AI service domains are isolated per session
//
// This means you can be logged into ChatGPT with your work account in one session
// and your personal account in another, and they'll never interfere with each other.

import 'dart:io';
import 'package:flutter_inappwebview/flutter_inappwebview.dart';
import 'package:ai_hub/services/secure_cookie_storage.dart';
import 'package:ai_hub/services/logger_service.dart';

/// Service for maintaining complete privacy and isolation between sessions.
///
/// Responsibilities:
/// - Save/restore cookies securely for each session
/// - Ensure complete cookie isolation between sessions
/// - Manage cookies for all AI service domains
/// - Handle authentication domain cookies (Google, Microsoft, Apple logins)
class SessionPrivacyService {
  /// Global map of active webview controllers to trigger persistence on app lifecycle changes.
  static final Map<String, InAppWebViewController> activeControllers = {};

  // AI Service and general domains (Isolated per session)
  static const List<String> aiDomains = [
    'openai.com',
    'chatgpt.com',
    'anthropic.com',
    'claude.ai',
    'deepseek.com',
    'grok.com',
    'x.ai',
    'perplexity.ai',
    'gemini.google.com',
    'bard.google.com',
    'google.com',
    'googleusercontent.com',
    'gstatic.com',
    'youtube.com',
    'gmail.com',
    'docs.google.com',
    'drive.google.com',
    'mail.google.com',
    'bing.com',
    'microsoft.com',
    'live.com',
    'office.com',
    'msn.com',
    'copilot.microsoft.com',
    'accounts.google.com',
    'login.microsoftonline.com',
    'login.live.com',
    'appleid.apple.com',
    'apple.com',
    'icloud.com',
  ];

  /// Saves cookies from the current controller securely.
  /// [sessionId] is used to isolate AI service cookies.
  static Future<void> saveCookies(
      InAppWebViewController controller, String sessionId) async {
    final cookieManager = CookieManager.instance();

    // Collect and save Isolated AI Cookies (including auth domains per session)
    final sessionCookies = <String, List<Map<String, dynamic>>>{};
    for (final domain in aiDomains) {
      final cookies = await _getCookiesForDomain(cookieManager, domain);
      if (cookies.isNotEmpty) {
        sessionCookies[domain] = cookies;
      }
    }
    await SecureCookieStorage.saveSessionCookies(sessionId, sessionCookies);

    // Trigger native flush to ensure persistence on disk
    try {
      if (Platform.isAndroid) {
        await cookieManager.deleteCookie(
          url: WebUri('https://google.com'),
          name: 'flush_trigger',
        );
      }
    } catch (e) {
      // Ignore
    }
  }

  /// Restores cookies to the current controller from secure storage.
  static Future<void> restoreCookies(
      InAppWebViewController controller, String sessionId) async {
    final cookieManager = CookieManager.instance();

    // 1. Aggressive Clean Wipe before restoring
    await cookieManager.deleteAllCookies();

    // 2. Brief delay to allow native platform to process the wipe
    // Especially important on some Android versions where removeAllCookies is async
    Logger.web('Wiping cookies for new session setup...');
    await Future.delayed(const Duration(milliseconds: 100));

    // 3. Restore Isolated AI Cookies
    Logger.web('Restoring isolated cookie vault for session: $sessionId');
    final sessionCookies =
        await SecureCookieStorage.loadSessionCookies(sessionId);
    for (final entry in sessionCookies.entries) {
      await _applyCookiesForDomain(cookieManager, entry.key, entry.value);
    }

    // 4. Final native flush/sync
    Logger.web('Session vault restoration complete');
  }

  static Future<List<Map<String, dynamic>>> _getCookiesForDomain(
    CookieManager cookieManager,
    String domain,
  ) async {
    try {
      // We try both with and without dots for domain-wide cookies
      final urls = [
        WebUri('https://$domain'),
        WebUri('https://.$domain'),
        WebUri('http://$domain'),
      ];

      final Map<String, Map<String, dynamic>> uniqueCookies = {};

      for (final url in urls) {
        final cookies = await cookieManager.getCookies(url: url);
        for (final c in cookies) {
          final key = '${c.name}_${c.domain}_${c.path}';
          uniqueCookies[key] = {
            'name': c.name,
            'value': c.value,
            'domain': c.domain,
            'path': c.path,
            'isSecure': c.isSecure,
            'isHttpOnly': c.isHttpOnly,
            'sameSite': c.sameSite?.toValue(),
            'expiresDate': c.expiresDate,
          };
        }
      }
      return uniqueCookies.values.toList();
    } catch (e) {
      return [];
    }
  }

  static Future<void> _applyCookiesForDomain(
    CookieManager cookieManager,
    String domain,
    List<Map<String, dynamic>> cookies,
  ) async {
    for (final data in cookies) {
      try {
        final url = WebUri(
            'https://${data['domain']?.toString().replaceFirst(RegExp(r'^\.'), '') ?? domain}');
        await cookieManager.setCookie(
          url: url,
          name: data['name'],
          value: data['value'],
          domain: data['domain'] ?? '.$domain',
          path: data['path'] ?? '/',
          isSecure: data['isSecure'] ?? true,
          isHttpOnly: data['isHttpOnly'] ?? false,
          expiresDate: data['expiresDate'],
          sameSite: HTTPCookieSameSitePolicy.fromValue(data['sameSite']),
        );
      } catch (e) {
        // Silent fail
      }
    }
  }

  /// Persists cookies from multiple controllers at once.
  static Future<void> saveAllCookies(
      Map<String, InAppWebViewController> controllers) async {
    for (final entry in controllers.entries) {
      await saveCookies(entry.value, entry.key);
    }
  }
}
