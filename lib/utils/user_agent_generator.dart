class UserAgentGenerator {
  // Use a modern, standard Android Chrome UA.
  // We avoid "wv" and "Version/4.0" which are common WebView signatures.
  static const String androidUserAgent =
      'Mozilla/5.0 (Linux; Android 15; Pixel 9 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.6778.135 Mobile Safari/537.36';

  static const String iphoneUserAgent =
      'Mozilla/5.0 (iPhone; CPU iPhone OS 18_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.2 Mobile/15E148 Safari/604.1';

  static String getUserAgent({String? url}) {
    if (url != null && url.toLowerCase().contains('x.com/i/grok')) {
      return androidUserAgent;
    }
    return iphoneUserAgent;
  }

  static bool isAuthUrl(String? urlString) {
    if (urlString == null || urlString.isEmpty) return false;

    try {
      final uri = Uri.parse(urlString.toLowerCase());
      final host = uri.host;
      final path = uri.path;

      // 1. Explicit Auth Subdomains/Domains
      final authDomains = [
        'accounts.google.com',
        'auth.openai.com',
        'chatgpt.com/auth', // special case check later
        'auth.anthropic.com',
        'login.microsoftonline.com',
        'login.live.com',
        'account.live.com',
        'appleid.apple.com',
        'gsa.apple.com',
        'auth0.com',
        'okta.com',
        'clerk.accounts.dev',
        'firebaseapp.com',
        'accounts.youtube.com',
        'stytch.com',
      ];

      if (authDomains.any((domain) => host.contains(domain))) return true;

      // 2. Generic Auth Subdomains (e.g., auth.example.com, login.example.com)
      if (host.startsWith('auth.') ||
          host.startsWith('login.') ||
          host.startsWith('signin.') ||
          host.startsWith('accounts.') ||
          host.startsWith('sso.')) {
        return true;
      }

      // 3. Path-based Detection (Common login endpoints)
      final authPaths = [
        '/login',
        '/signin',
        '/signup',
        '/auth',
        '/oauth',
        '/authorize',
        '/authenticate',
        '/sessions',
        '/verify',
        '/accounts',
        '/callback',
      ];

      if (authPaths.any((p) => path.contains(p))) return true;

      // 4. Query Parameter Check (OAuth/OpenID)
      if (uri.queryParameters.containsKey('client_id') ||
          uri.queryParameters.containsKey('response_type') ||
          uri.queryParameters.containsKey('redirect_uri')) {
        return true;
      }

      return false;
    } catch (e) {
      // Fallback: simple string contains
      final lower = urlString.toLowerCase();
      return lower.contains('login') ||
          lower.contains('auth') ||
          lower.contains('signin') ||
          lower.contains('oauth');
    }
  }
}
