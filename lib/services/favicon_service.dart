import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;

class FaviconService {
  static const Duration _timeout = Duration(seconds: 5);

  /// Fetch favicon URL using multiple fallback strategies
  static Future<String> fetchFaviconUrl(String url) async {
    final domain = _extractDomain(url);
    final googleUrl =
        'https://www.google.com/s2/favicons?domain=$domain&sz=128';

    if (kIsWeb) {
      return googleUrl;
    }

    // Strategy 1: Google Favicon API
    if (await _isUrlValid(googleUrl)) {
      return googleUrl;
    }

    // Strategy 2: DuckDuckGo Icons
    final duckduckgoUrl = 'https://icons.duckduckgo.com/ip3/$domain.ico';
    if (await _isUrlValid(duckduckgoUrl)) {
      return duckduckgoUrl;
    }

    // Strategy 3: Direct favicon.ico
    final directUrl = '$url/favicon.ico';
    if (await _isUrlValid(directUrl)) {
      return directUrl;
    }

    // Strategy 4: Parse HTML for icon link
    try {
      final iconUrl = await _parseHtmlForIcon(url);
      if (iconUrl != null && await _isUrlValid(iconUrl)) {
        return iconUrl;
      }
    } catch (e) {
      // Ignore parsing errors
    }

    // Fallback: return Google API URL anyway
    return googleUrl;
  }

  static String _extractDomain(String url) {
    try {
      final uri = Uri.parse(url);
      return uri.host;
    } catch (e) {
      return url.replaceAll(RegExp(r'^https?://'), '').split('/').first;
    }
  }

  static Future<bool> _isUrlValid(String url) async {
    try {
      final response = await http.head(Uri.parse(url)).timeout(_timeout);
      return response.statusCode == 200;
    } catch (e) {
      return false;
    }
  }

  static Future<String?> _parseHtmlForIcon(String url) async {
    try {
      final response = await http.get(Uri.parse(url)).timeout(_timeout);
      if (response.statusCode != 200) return null;

      final html = response.body;

      // Look for common icon link patterns
      final patterns = [
        RegExp(
            r'''<link[^>]*rel=["'](?:icon|shortcut icon)["'][^>]*href=["'](.[^"']*)["']''',
            caseSensitive: false),
        RegExp(
            r'''<link[^>]*href=["'](.[^"']*)["'][^>]*rel=["'](?:icon|shortcut icon)["']''',
            caseSensitive: false),
      ];

      for (final pattern in patterns) {
        final match = pattern.firstMatch(html);
        if (match != null) {
          final iconPath = match.group(1);
          if (iconPath != null) {
            return _resolveUrl(url, iconPath);
          }
        }
      }

      return null;
    } catch (e) {
      return null;
    }
  }

  static String _resolveUrl(String baseUrl, String path) {
    try {
      final base = Uri.parse(baseUrl);
      return base.resolve(path).toString();
    } catch (e) {
      if (path.startsWith('http')) return path;
      return '$baseUrl/$path';
    }
  }
}
