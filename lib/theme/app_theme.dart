import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:ai_hub/models/theme_mode.dart';

class AppTheme {
  static ThemeData getDarkTheme(ContrastLevel contrast) {
    // Unified contrast values (same relative changes in light and dark)
    Color scaffoldBg = const Color(0xFF121212); // Standard: Deep Black
    Color cardBg = const Color(0xFF1C1C1E);
    Color canvasBg = const Color(0xFF121212);
    Color outlineColor = const Color(0xFF2C2C2E);

    if (contrast == ContrastLevel.relaxed) {
      // Relaxed: Softer, less harsh
      scaffoldBg = const Color(0xFF1C1C1E);
      cardBg = const Color(0xFF2C2C2E);
      canvasBg = const Color(0xFF1C1C1E);
      outlineColor = const Color(0xFF3A3A3C);
    } else if (contrast == ContrastLevel.high) {
      // High: Maximum contrast - pure black with much brighter outlines
      scaffoldBg = const Color(0xFF000000);
      cardBg = const Color(0xFF000000);
      canvasBg = const Color(0xFF000000);
      outlineColor = const Color(0xFFFFFFFF).withValues(alpha: 0.6);
    }

    return ThemeData(
      useMaterial3: true,
      brightness: Brightness.dark,
      scaffoldBackgroundColor: scaffoldBg,
      primaryColor: const Color(0xFF0A84FF),
      canvasColor: canvasBg,
      cardColor: cardBg,
      dividerColor: outlineColor,
      fontFamily: '.SF Pro Display',
      cupertinoOverrideTheme: CupertinoThemeData(
        brightness: Brightness.dark,
        primaryColor: const Color(0xFF0A84FF),
        scaffoldBackgroundColor: scaffoldBg,
        barBackgroundColor: scaffoldBg, // Fully opaque for high contrast
        textTheme: CupertinoTextThemeData(
          primaryColor: const Color(0xFF0A84FF),
          navTitleTextStyle: const TextStyle(
            inherit: false,
            color: Color(0xFFFFFFFF),
            fontSize: 17,
            fontWeight: FontWeight.w700,
            letterSpacing: -0.4,
          ),
          navLargeTitleTextStyle: const TextStyle(
            inherit: false,
            color: Color(0xFFFFFFFF),
            fontSize: 34,
            fontWeight: FontWeight.w900,
            letterSpacing: -1.0,
          ),
          textStyle: const TextStyle(
            inherit: false,
            color: Color(0xFFFFFFFF),
            fontSize: 17,
          ),
        ),
      ),
      appBarTheme: AppBarTheme(
        backgroundColor: scaffoldBg,
        elevation: 0,
        centerTitle: true,
        scrolledUnderElevation: 0,
        iconTheme: const IconThemeData(color: Color(0xFF0A84FF)),
        titleTextStyle: const TextStyle(
          inherit: false,
          color: Color(0xFFFFFFFF),
          fontSize: 17,
          fontWeight: FontWeight.w800,
          letterSpacing: -0.4,
        ),
      ),
      cardTheme: CardThemeData(
        color: cardBg,
        elevation: 0,
        margin: EdgeInsets.zero,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(24),
          side: BorderSide(
            color: outlineColor,
            width: contrast == ContrastLevel.high
                ? 2.5
                : 1.0, // Thicker border for high contrast
          ),
        ),
      ),
      textTheme: const TextTheme(
        displayLarge: TextStyle(
          inherit: false,
          fontSize: 34,
          fontWeight: FontWeight.w800,
          letterSpacing: -1.2,
          color: Color(0xFFFFFFFF),
        ),
        titleLarge: TextStyle(
          inherit: false,
          fontSize: 22,
          fontWeight: FontWeight.w700,
          letterSpacing: -0.5,
          color: Color(0xFFFFFFFF),
        ),
        bodyLarge: TextStyle(
          inherit: false,
          fontSize: 17,
          fontWeight: FontWeight.w600,
          letterSpacing: -0.4,
          color: Color(0xFFFFFFFF),
        ),
        bodyMedium: TextStyle(
          inherit: false,
          fontSize: 15,
          fontWeight: FontWeight.w400,
          letterSpacing: -0.2,
          color: Color(0xFF98989E),
        ),
      ),
      colorScheme: ColorScheme.dark(
        primary: const Color(0xFF0A84FF),
        secondary: const Color(0xFFBF5AF2),
        surface: cardBg,
        error: const Color(0xFFFF453A),
        outline: outlineColor,
      ),
    );
  }

  static ThemeData getLightTheme(ContrastLevel contrast) {
    // Unified contrast values (same relative changes as dark mode)
    Color scaffoldBg = const Color(0xFFF2F2F7); // Standard
    Color cardBg = const Color(0xFFFFFFFF);
    Color outlineColor = const Color(0xFFC6C6C8);

    if (contrast == ContrastLevel.relaxed) {
      // Relaxed: Softer, warmer
      scaffoldBg = const Color(0xFFE8E8ED);
      cardBg = const Color(0xFFF5F5FA);
      outlineColor = const Color(0xFFD1D1D6);
    } else if (contrast == ContrastLevel.high) {
      // High: Maximum contrast - pure white with dark outlines
      scaffoldBg = const Color(0xFFFFFFFF);
      cardBg = const Color(0xFFFFFFFF);
      outlineColor = const Color(0xFF000000).withValues(alpha: 0.3);
    }

    return ThemeData(
      useMaterial3: true,
      brightness: Brightness.light,
      scaffoldBackgroundColor: scaffoldBg,
      primaryColor: const Color(0xFF007AFF),
      canvasColor: cardBg,
      cardColor: cardBg,
      dividerColor: outlineColor,
      cupertinoOverrideTheme: CupertinoThemeData(
        brightness: Brightness.light,
        primaryColor: const Color(0xFF007AFF),
        scaffoldBackgroundColor: scaffoldBg,
        barBackgroundColor: scaffoldBg.withValues(alpha: 0.8),
        textTheme: const CupertinoTextThemeData(
          navTitleTextStyle: TextStyle(
            inherit: false,
            color: Color(0xFF000000),
            fontSize: 17,
            fontWeight: FontWeight.w600,
          ),
          navLargeTitleTextStyle: TextStyle(
            inherit: false,
            color: Color(0xFF000000),
            fontSize: 34,
            fontWeight: FontWeight.w700,
          ),
        ),
      ),
      appBarTheme: AppBarTheme(
        backgroundColor: scaffoldBg.withValues(alpha: 0.8),
        elevation: 0,
        centerTitle: true,
        scrolledUnderElevation: 0,
        iconTheme: const IconThemeData(color: Color(0xFF007AFF)),
        titleTextStyle: const TextStyle(
          inherit: false,
          color: Color(0xFF000000),
          fontSize: 17,
          fontWeight: FontWeight.w600,
          letterSpacing: -0.4,
        ),
      ),
      cardTheme: CardThemeData(
        color: const Color(0xFFFFFFFF),
        elevation: 0,
        margin: EdgeInsets.zero,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(28),
          side: BorderSide(color: outlineColor, width: 1.0),
        ),
      ),
      textTheme: const TextTheme(
        displayLarge: TextStyle(
          inherit: false,
          fontSize: 34,
          fontWeight: FontWeight.w700,
          letterSpacing: -1.0,
          color: Color(0xFF000000),
        ),
        titleLarge: TextStyle(
          inherit: false,
          fontSize: 22,
          fontWeight: FontWeight.w700,
          letterSpacing: -0.5,
          color: Color(0xFF000000),
        ),
        bodyLarge: TextStyle(
          inherit: false,
          fontSize: 17,
          fontWeight: FontWeight.w400,
          letterSpacing: -0.4,
          color: Color(0xFF000000),
        ),
        bodyMedium: TextStyle(
          inherit: false,
          fontSize: 15,
          fontWeight: FontWeight.w400,
          letterSpacing: -0.2,
          color: Color(0xFF8E8E93),
        ),
      ),
      colorScheme: ColorScheme.light(
        primary: const Color(0xFF007AFF),
        secondary: const Color(0xFF5856D6),
        surface: const Color(0xFFFFFFFF),
        error: const Color(0xFFFF3B30),
        outline: outlineColor,
      ),
    );
  }

  static ThemeData getTheme(AppThemeMode mode, ContrastLevel contrast) {
    switch (mode) {
      case AppThemeMode.light:
        return getLightTheme(contrast);
      case AppThemeMode.dark:
        return getDarkTheme(contrast);
    }
  }
}
