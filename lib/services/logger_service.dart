import 'package:flutter/foundation.dart';

/// Central logging service for ColAI.
///
/// This service provides a unified way to log messages across the app.
/// In production/release mode, logs can be filtered or disabled to enhance performance
/// and security.
class Logger {
  /// Log an informative message.
  static void info(String message) {
    if (kDebugMode) {
      _printLog('INFO', message);
    }
  }

  /// Log a WebView-related message.
  static void web(String message) {
    if (kDebugMode) {
      _printLog('WEB', message);
    }
  }

  /// Log a warning message.
  static void warn(String message) {
    if (kDebugMode) {
      _printLog('WARN', message);
    }
  }

  /// Log an error message.
  static void error(String message, [dynamic error, StackTrace? stackTrace]) {
    String errorMessage = message;
    if (error != null) {
      errorMessage += ' | Error: $error';
    }

    _printLog('ERROR', errorMessage);

    if (stackTrace != null && kDebugMode) {
      debugPrint(stackTrace.toString());
    }
  }

  static void _printLog(String level, String message) {
    final timestamp = DateTime.now().toIso8601String().split('T').last;
    debugPrint('[$level][$timestamp] ColAI: $message');
  }
}
