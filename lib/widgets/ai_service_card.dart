// AI Service Card Widget for ColAI.
//
// This widget displays a single AI service in a glassmorphic card design.
// It shows the service logo, name, and session count. The card has a subtle
// iOS-inspired glass effect that adapts to light and dark themes.
//
// Performance note: Uses BlocBuilder with buildWhen to avoid rebuilding
// when sessions for other services change.

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:ai_hub/models/ai_service.dart';
import 'package:ai_hub/state/sessions_bloc.dart';
import 'package:flutter/foundation.dart';
import 'dart:io';
import 'dart:ui';
import 'package:ai_hub/services/logger_service.dart';

/// A glassmorphic card that displays an AI service.
///
/// Features:
/// - Adaptive glass-blur background
/// - Auto-inverts certain logos in dark mode for better visibility
/// - Shows session count for the service
/// - Handles tap and long-press gestures
class AIServiceCard extends StatelessWidget {
  final AIService service;
  final VoidCallback onTap;
  final VoidCallback? onLongPress;

  const AIServiceCard({
    super.key,
    required this.service,
    required this.onTap,
    this.onLongPress,
  });

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;

    // Optimize: Only rebuild when sessions for THIS service change
    return BlocBuilder<SessionsBloc, SessionsState>(
      buildWhen: (previous, current) {
        // Only rebuild if sessions actually changed
        if (previous is SessionsLoaded && current is SessionsLoaded) {
          final prevCount =
              previous.sessions.where((s) => s.serviceId == service.id).length;
          final currentCount =
              current.sessions.where((s) => s.serviceId == service.id).length;
          return prevCount != currentCount;
        }
        // Rebuild for loading/error states
        return true;
      },
      builder: (context, state) {
        int sessionCount = 0;
        if (state is SessionsLoaded) {
          sessionCount =
              state.sessions.where((s) => s.serviceId == service.id).length;
        }

        return Material(
          color: Colors.transparent,
          child: GestureDetector(
            onTap: () {
              HapticFeedback.lightImpact();
              onTap();
            },
            onLongPress: onLongPress,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                // Liquid Glass Logo Container - Ultra Compact
                SizedBox(
                  height: 110, // Increased ~10% for better density
                  child: Center(
                    child: Container(
                      decoration: BoxDecoration(
                        borderRadius: BorderRadius.circular(30),
                        boxShadow: [
                          BoxShadow(
                            color: Colors.black.withValues(alpha: 0.1),
                            blurRadius: 15,
                            offset: const Offset(0, 5),
                          ),
                        ],
                      ),
                      child: ClipRRect(
                        borderRadius: BorderRadius.circular(24),
                        child: BackdropFilter(
                          filter: ImageFilter.blur(sigmaX: 30, sigmaY: 30),
                          child: Container(
                            padding: const EdgeInsets.all(
                                18), // Slightly more padding
                            decoration: BoxDecoration(
                              color: isDark
                                  ? const Color(0xFF1C1C1E)
                                      .withValues(alpha: 0.6)
                                  : const Color(0xFFE5E5EA)
                                      .withValues(alpha: 0.7),
                              borderRadius: BorderRadius.circular(24),
                              border: Border.all(
                                color: isDark
                                    ? const Color(0x22FFFFFF)
                                    : const Color(0x08000000),
                                width: 0.5,
                              ),
                            ),
                            child: AspectRatio(
                              aspectRatio: 1,
                              child: Center(
                                child: ClipRRect(
                                  borderRadius: BorderRadius.circular(10),
                                  child: _applyInversion(_buildIcon(), isDark),
                                ),
                              ),
                            ),
                          ),
                        ),
                      ),
                    ),
                  ),
                ),
                const SizedBox(height: 6),
                // Name with refined hierarchy
                Text(
                  service.name,
                  style: TextStyle(
                    fontSize: 13, // Reduced from 15
                    fontWeight: FontWeight.w600,
                    color: isDark
                        ? const Color(0xFFFFFFFF)
                        : const Color(0xFF000000),
                    letterSpacing: -0.2,
                  ),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
                if (sessionCount > 0)
                  Padding(
                    padding: const EdgeInsets.only(top: 2),
                    child: Text(
                      '$sessionCount ${sessionCount == 1 ? 'session' : 'sessions'}',
                      style: const TextStyle(
                        fontSize: 10, // Reduced from 11
                        fontWeight: FontWeight.w500,
                        color: Color(0xFF8E8E93),
                      ),
                    ),
                  ),
              ],
            ),
          ),
        );
      },
    );
  }

  Widget _applyInversion(Widget child, bool isDark) {
    if (isDark && (service.id == 'chatgpt' || service.id == 'grok')) {
      return ColorFiltered(
        colorFilter: const ColorFilter.matrix([
          -1,
          0,
          0,
          0,
          255,
          0,
          -1,
          0,
          0,
          255,
          0,
          0,
          -1,
          0,
          255,
          0,
          0,
          0,
          1,
          0,
        ]),
        child: child,
      );
    }
    return child;
  }

  Widget _buildIcon() {
    // 1. Handle Web platforms (no File IO allowed)
    if (kIsWeb) {
      if (service.iconPath != null && service.iconPath!.isNotEmpty) {
        if (service.iconPath!.startsWith('http')) {
          return Image.network(
            service.iconPath!,
            fit: BoxFit.contain,
            errorBuilder: (_, __, ___) => _buildLetterAvatar(),
          );
        }
        if (service.iconPath!.startsWith('assets/')) {
          return Image.asset(service.iconPath!, fit: BoxFit.contain);
        }
      }
      return _buildLetterAvatar();
    }

    // 2. Handle Native platforms (Android/iOS)
    if (service.iconPath != null && service.iconPath!.isNotEmpty) {
      // Assets
      if (service.iconPath!.startsWith('assets/')) {
        return Image.asset(
          service.iconPath!,
          fit: BoxFit.contain,
          errorBuilder: (_, __, ___) => _buildLetterAvatar(),
        );
      }
      // Network
      if (service.iconPath!.startsWith('http')) {
        return Image.network(
          service.iconPath!,
          fit: BoxFit.contain,
          errorBuilder: (_, __, ___) => _buildLetterAvatar(),
        );
      }
      // Local Files (App Data)
      try {
        final file = File(service.iconPath!);
        if (file.existsSync()) {
          return Image.file(
            file,
            fit: BoxFit.contain,
            errorBuilder: (_, __, ___) => _buildLetterAvatar(),
          );
        }
      } catch (e) {
        Logger.error('Error loading local image file', e);
      }
    }

    return _buildLetterAvatar();
  }

  Widget _buildLetterAvatar() {
    return Center(
      child: Text(
        service.name.isNotEmpty ? service.name[0].toUpperCase() : '?',
        style: const TextStyle(
          fontSize: 32,
          fontWeight: FontWeight.w300,
          color: Color(0xFF8E8E93),
        ),
      ),
    );
  }
}
