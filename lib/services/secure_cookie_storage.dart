// Secure cookie storage for ColAI.
//
// Provides multi-layered encrypted storage for sensitive session cookies.
// This ensures that even if someone gains physical access to the device,
// they can't extract authentication cookies or session data.
//
// Security layers:
// Layer 1: Hardware-backed encryption using platform KeyStore/Keychain (flutter_secure_storage)
// Layer 2: Software encryption with obfuscated keys (SecureCiphers)
//
// This dual-layer approach protects against:
// - Physical device theft
// - OS-level exploits
// - Root/jailbreak attacks
// - Backup extraction

import 'dart:convert';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:ai_hub/utils/secure_ciphers.dart';

/// Provides tiered encrypted storage for sensitive cookie data.
///
/// Why two layers?
/// - Layer 1 (hardware): Protection against normal threats
/// - Layer 2 (software): Protection even if device is rooted/jailbroken
///
/// Each session's cookies are stored separately to maintain isolation.
class SecureCookieStorage {
  // Default options use modern custom ciphers (recommended by flutter_secure_storage v10+)
  static const FlutterSecureStorage _storage = FlutterSecureStorage();

  static const String _sessionCookiesKeyPrefix = 'secure_session_cookies_';

  /// Saves session-specific (AI service) cookies securely.
  static Future<void> saveSessionCookies(String sessionId,
      Map<String, List<Map<String, dynamic>>> cookiesByDomain) async {
    final json = jsonEncode(cookiesByDomain);
    final encrypted = SecureCiphers.encrypt(json);
    await _storage.write(
        key: '$_sessionCookiesKeyPrefix$sessionId', value: encrypted);
  }

  /// Loads session-specific (AI service) cookies.
  static Future<Map<String, List<Map<String, dynamic>>>> loadSessionCookies(
      String sessionId) async {
    final encrypted =
        await _storage.read(key: '$_sessionCookiesKeyPrefix$sessionId');
    if (encrypted == null) return {};

    try {
      final json = SecureCiphers.decrypt(encrypted);
      if (json.isEmpty) return {};

      final decoded = jsonDecode(json) as Map<String, dynamic>;
      return decoded
          .map((k, v) => MapEntry(k, (v as List).cast<Map<String, dynamic>>()));
    } catch (e) {
      return {};
    }
  }

  /// Deletes session-specific cookies (when session is deleted).
  static Future<void> deleteSessionCookies(String sessionId) async {
    await _storage.delete(key: '$_sessionCookiesKeyPrefix$sessionId');
  }

  /// Clears all stored cookies (for privacy reset).
  static Future<void> clearAll() async {
    await _storage.deleteAll();
  }
}
