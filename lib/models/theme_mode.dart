// Theme configuration for ColAI.
//
// You know how some apps have that one perfect dark mode that just *feels* right?
// We're trying to be that app. This file defines our theme modes and contrast levels.
//
// Why two contrast levels? Because not everyone's eyes work the same way.
// Some folks like it subtle and relaxed, others need that crisp high-contrast pop.
// We got you covered either way.

/// The main theme mode - because choosing between light and dark mode
/// shouldn't require a PhD in computer science.
enum AppThemeMode {
  /// Light mode for the "I actually enjoy seeing things" crowd
  light,

  /// Dark mode for the rest of us (let's be honest, it's superior)
  dark,
}

/// Contrast levels - fine-tuning the visual experience.
///
/// Think of this as the difference between a gentle sunset and
/// looking directly at the sun. We provide options!
enum ContrastLevel {
  /// Relaxed contrast - easy on the eyes, perfect for late-night browsing
  /// when your pupils are already doing their best impression of dinner plates
  relaxed,

  /// High contrast - for when you need things to POP!
  /// Great for accessibility and those with visual impairments
  high,
}

/// Extension methods because Dart enums deserve the good stuff too.
extension AppThemeModeExtension on AppThemeMode {
  /// Human-readable name for this theme mode
  String get name {
    switch (this) {
      case AppThemeMode.light:
        return 'Light';
      case AppThemeMode.dark:
        return 'Dark';
    }
  }

  /// Quick check if we're in the superior dark side
  bool get isDark => this == AppThemeMode.dark;
}

/// Extension for contrast levels because we're polite like that.
extension ContrastLevelExtension on ContrastLevel {
  /// Human-readable name for this contrast level
  String get name {
    switch (this) {
      case ContrastLevel.relaxed:
        return 'Relaxed';
      case ContrastLevel.high:
        return 'High';
    }
  }
}
