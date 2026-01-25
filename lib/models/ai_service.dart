// AI Service model for ColAI.
//
// Represents a single AI service (like ChatGPT, Claude, etc.) with all its configuration.
// Each service can have custom user agents, headers, and notification preferences.

import 'package:equatable/equatable.dart';

/// Represents an AI service that can be accessed through ColAI.
///
/// Each service has:
/// - Basic info: name, URL
/// - Visual: favicon URL, optional custom icon
/// - Behavior: custom user agent, headers
/// - Features: notification support, widget configuration
class AIService extends Equatable {
  final String id;
  final String name;
  final String url;
  final String faviconUrl;
  final String? iconPath; // Path to local icon asset
  final String? customUserAgent;
  final Map<String, String>? customHeaders;
  final String? widgetSessionId; // Session to open when widget is clicked
  final bool notificationsEnabled;
  final DateTime createdAt;

  const AIService({
    required this.id,
    required this.name,
    required this.url,
    required this.faviconUrl,
    this.iconPath,
    this.customUserAgent,
    this.customHeaders,
    this.widgetSessionId,
    this.notificationsEnabled = true,
    required this.createdAt,
  });

  // JSON Serialization
  Map<String, dynamic> toJson() => {
        'id': id,
        'name': name,
        'url': url,
        'faviconUrl': faviconUrl,
        'iconPath': iconPath,
        'customUserAgent': customUserAgent,
        'customHeaders': customHeaders,
        'widgetSessionId': widgetSessionId,
        'notificationsEnabled': notificationsEnabled,
        'createdAt': createdAt.toIso8601String(),
      };

  factory AIService.fromJson(Map<String, dynamic> json) => AIService(
        id: json['id'] as String,
        name: json['name'] as String,
        url: json['url'] as String,
        faviconUrl: json['faviconUrl'] as String,
        iconPath: json['iconPath'] as String?,
        customUserAgent: json['customUserAgent'] as String?,
        customHeaders: json['customHeaders'] != null
            ? Map<String, String>.from(json['customHeaders'])
            : null,
        widgetSessionId: json['widgetSessionId'] as String?,
        notificationsEnabled: json['notificationsEnabled'] as bool? ?? true,
        createdAt: DateTime.parse(json['createdAt'] as String),
      );

  AIService copyWith({
    String? id,
    String? name,
    String? url,
    String? faviconUrl,
    String? iconPath,
    String? customUserAgent,
    Map<String, String>? customHeaders,
    String? widgetSessionId,
    bool? notificationsEnabled,
    DateTime? createdAt,
  }) {
    return AIService(
      id: id ?? this.id,
      name: name ?? this.name,
      url: url ?? this.url,
      faviconUrl: faviconUrl ?? this.faviconUrl,
      iconPath: iconPath ?? this.iconPath,
      customUserAgent: customUserAgent ?? this.customUserAgent,
      customHeaders: customHeaders ?? this.customHeaders,
      widgetSessionId: widgetSessionId ?? this.widgetSessionId,
      notificationsEnabled: notificationsEnabled ?? this.notificationsEnabled,
      createdAt: createdAt ?? this.createdAt,
    );
  }

  @override
  List<Object?> get props => [
        id,
        name,
        url,
        faviconUrl,
        iconPath,
        customUserAgent,
        customHeaders,
        widgetSessionId,
        notificationsEnabled,
        createdAt,
      ];
}
