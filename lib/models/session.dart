// Session model for ColAI.
//
// Represents an isolated browser session for an AI service.
// Think of each session as a completely separate browser profile - they never share
// cookies, cache, or any data. Perfect for managing multiple accounts on the same service!

import 'package:equatable/equatable.dart';

/// Represents an isolated session for an AI service.
///
/// Each session is like a separate browser profile:
/// - Has its own cookies stored in an isolated directory
/// - Completely independent from other sessions
/// - Tracks when it was last used
/// - Can have custom theme preferences
/// - Can be set as the default for quick access
class Session extends Equatable {
  final String id;
  final String serviceId;
  final String accountName;
  final bool isDefault;
  final DateTime lastAccessed;
  final String? cookieStorePath; // Path to isolated cookie storage
  final bool notificationsEnabled;
  final String? themeMode; // 'light', 'dark', 'system' or null
  final String? customColors; // Hex or null

  const Session({
    required this.id,
    required this.serviceId,
    required this.accountName,
    this.isDefault = false,
    required this.lastAccessed,
    this.cookieStorePath,
    this.notificationsEnabled = true,
    this.themeMode,
    this.customColors,
  });

  // JSON Serialization
  Map<String, dynamic> toJson() => {
        'id': id,
        'serviceId': serviceId,
        'accountName': accountName,
        'isDefault': isDefault,
        'lastAccessed': lastAccessed.toIso8601String(),
        'cookieStorePath': cookieStorePath,
        'notificationsEnabled': notificationsEnabled,
        'themeMode': themeMode,
        'customColors': customColors,
      };

  factory Session.fromJson(Map<String, dynamic> json) => Session(
        id: json['id'] as String,
        serviceId: json['serviceId'] as String,
        accountName: json['accountName'] as String,
        isDefault: json['isDefault'] as bool? ?? false,
        lastAccessed: DateTime.parse(json['lastAccessed'] as String),
        cookieStorePath: json['cookieStorePath'] as String?,
        notificationsEnabled: json['notificationsEnabled'] as bool? ?? true,
        themeMode: json['themeMode'] as String?,
        customColors: json['customColors'] as String?,
      );

  Session copyWith({
    String? id,
    String? serviceId,
    String? accountName,
    bool? isDefault,
    DateTime? lastAccessed,
    String? cookieStorePath,
    bool? notificationsEnabled,
    String? themeMode,
    String? customColors,
  }) {
    return Session(
      id: id ?? this.id,
      serviceId: serviceId ?? this.serviceId,
      accountName: accountName ?? this.accountName,
      isDefault: isDefault ?? this.isDefault,
      lastAccessed: lastAccessed ?? this.lastAccessed,
      cookieStorePath: cookieStorePath ?? this.cookieStorePath,
      notificationsEnabled: notificationsEnabled ?? this.notificationsEnabled,
      themeMode: themeMode ?? this.themeMode,
      customColors: customColors ?? this.customColors,
    );
  }

  @override
  List<Object?> get props => [
        id,
        serviceId,
        accountName,
        isDefault,
        lastAccessed,
        cookieStorePath,
        notificationsEnabled,
        themeMode,
        customColors,
      ];
}
