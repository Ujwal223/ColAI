// Skeleton loader widgets for ColAI.
//
// These shimmering placeholders match the exact layout of the actual content,
// providing a smooth loading experience. They give users a visual preview of
// what's coming while data loads.

import 'package:flutter/material.dart';
import 'dart:ui';

/// A skeleton loader that mimics the AI service card layout.
///
/// Displays a shimmering placeholder with the same dimensions and layout
/// as the real AIServiceCard, creating a seamless transition when content loads.
class AIServiceCardSkeleton extends StatefulWidget {
  const AIServiceCardSkeleton({super.key});

  @override
  State<AIServiceCardSkeleton> createState() => _AIServiceCardSkeletonState();
}

class _AIServiceCardSkeletonState extends State<AIServiceCardSkeleton>
    with SingleTickerProviderStateMixin {
  late AnimationController _controller;
  late Animation<double> _animation;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      duration: const Duration(milliseconds: 1500),
      vsync: this,
    )..repeat(reverse: true);

    _animation = Tween<double>(begin: 0.3, end: 0.7).animate(
      CurvedAnimation(parent: _controller, curve: Curves.easeInOut),
    );
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return AnimatedBuilder(
      animation: _animation,
      builder: (context, child) {
        return Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            // Logo container skeleton - matches AIServiceCard dimensions
            SizedBox(
              height: 110,
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
                        padding: const EdgeInsets.all(18),
                        decoration: BoxDecoration(
                          color: isDark
                              ? const Color(0xFF1C1C1E).withValues(alpha: 0.6)
                              : const Color(0xFFE5E5EA).withValues(alpha: 0.7),
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
                            // Shimmering circle representing logo
                            child: Container(
                              width: 50,
                              height: 50,
                              decoration: BoxDecoration(
                                borderRadius: BorderRadius.circular(10),
                                color: (isDark ? Colors.white : Colors.black)
                                    .withValues(alpha: _animation.value * 0.3),
                              ),
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
            // Name skeleton
            Container(
              width: 70,
              height: 13,
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(4),
                color: (isDark ? Colors.white : Colors.black)
                    .withValues(alpha: _animation.value * 0.3),
              ),
            ),
            const SizedBox(height: 4),
            // Session count skeleton
            Container(
              width: 50,
              height: 10,
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(4),
                color: const Color(0xFF8E8E93)
                    .withValues(alpha: _animation.value * 0.2),
              ),
            ),
          ],
        );
      },
    );
  }
}

/// Grid of skeleton loaders matching the home screen layout.
class AIServicesGridSkeleton extends StatelessWidget {
  final int itemCount;

  const AIServicesGridSkeleton({
    super.key,
    this.itemCount = 6, // Default to 6 (typical default services count)
  });

  @override
  Widget build(BuildContext context) {
    final crossAxisCount = MediaQuery.of(context).size.width > 600 ? 4 : 3;

    return SliverGrid(
      gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: crossAxisCount,
        crossAxisSpacing: 20,
        mainAxisSpacing: 20,
        childAspectRatio: 0.72,
      ),
      delegate: SliverChildBuilderDelegate(
        (context, index) => const AIServiceCardSkeleton(),
        childCount: itemCount,
      ),
    );
  }
}
