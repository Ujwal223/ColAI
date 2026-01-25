// Storage service for ColAI.
//
// Handles all data persistence using SharedPreferences.
// This is where we save everything: AI services, sessions, theme preferences, etc.
// Think of it as the app's filing cabinet - everything gets stored and retrieved through here.

import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:ai_hub/models/ai_service.dart';
import 'package:ai_hub/models/session.dart';

/// Service for persisting app data locally.
///
/// Uses SharedPreferences to store:
/// - List of AI services (with custom ones users add)
/// - All sessions and their metadata
/// - Theme and appearance preferences
/// - Onboarding completion status
class StorageService {
  static const String _aiServicesKey = 'ai_services';
  static const String _sessionsKey = 'sessions';
  static const String _themeKey = 'theme_mode';
  static const String _contrastKey = 'contrast_level';
  static const String _enableSSOKey = 'enable_sso';
  static const String _onboardingCompleteKey = 'onboarding_complete';

  final SharedPreferences _prefs;

  StorageService(this._prefs);

  static Future<StorageService> create() async {
    final prefs = await SharedPreferences.getInstance();
    return StorageService(prefs);
  }

  // AI Services
  Future<bool> saveAIServices(List<AIService> services) async {
    final jsonList = services.map((s) => s.toJson()).toList();
    return await _prefs.setString(_aiServicesKey, jsonEncode(jsonList));
  }

  List<AIService> loadAIServices() {
    final jsonString = _prefs.getString(_aiServicesKey);
    if (jsonString == null) return [];

    final jsonList = jsonDecode(jsonString) as List;
    return jsonList.map((json) => AIService.fromJson(json)).toList();
  }

  // Sessions
  Future<bool> saveSessions(List<Session> sessions) async {
    final jsonList = sessions.map((s) => s.toJson()).toList();
    return await _prefs.setString(_sessionsKey, jsonEncode(jsonList));
  }

  List<Session> loadSessions() {
    final jsonString = _prefs.getString(_sessionsKey);
    if (jsonString == null) return [];

    final jsonList = jsonDecode(jsonString) as List;
    return jsonList.map((json) => Session.fromJson(json)).toList();
  }

  List<Session> getSessionsForService(String serviceId) {
    final allSessions = loadSessions();
    return allSessions.where((s) => s.serviceId == serviceId).toList();
  }

  // Theme
  Future<bool> saveThemeMode(String themeMode) async {
    return await _prefs.setString(_themeKey, themeMode);
  }

  String? loadThemeMode() {
    return _prefs.getString(_themeKey);
  }

  Future<bool> saveContrastLevel(String contrastLevel) async {
    return await _prefs.setString(_contrastKey, contrastLevel);
  }

  String? loadContrastLevel() {
    return _prefs.getString(_contrastKey);
  }

  // SSO Settings
  Future<bool> saveEnableSSO(bool enable) async {
    return await _prefs.setBool(_enableSSOKey, enable);
  }

  bool loadEnableSSO() {
    return _prefs.getBool(_enableSSOKey) ?? false;
  }

  // Onboarding
  Future<bool> saveOnboardingComplete(bool complete) async {
    return await _prefs.setBool(_onboardingCompleteKey, complete);
  }

  bool loadOnboardingComplete() {
    return _prefs.getBool(_onboardingCompleteKey) ?? false;
  }

  // Clear all data
  Future<bool> clearAll() async {
    return await _prefs.clear();
  }
}
