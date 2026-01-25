import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:glassmorphism/glassmorphism.dart';
import 'package:ai_hub/models/ai_service.dart';
import 'package:ai_hub/widgets/ai_service_card.dart';
import 'package:ai_hub/widgets/skeleton_loaders.dart';
import 'package:ai_hub/screens/add_service_screen.dart';
import 'package:ai_hub/screens/webview_screen.dart';
import 'package:ai_hub/state/ai_services_bloc.dart';
import 'package:ai_hub/screens/settings_screen.dart';
import 'package:ai_hub/state/theme_cubit.dart';
import 'package:ai_hub/models/theme_mode.dart';
import 'package:ai_hub/services/secure_browser_launcher.dart';
import 'package:ai_hub/data/default_ai_services.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> with WidgetsBindingObserver {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      // Overlay logic handled by SecureBrowserLauncher internal cleanup if needed
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final isDark = theme.brightness == Brightness.dark;

    return CupertinoPageScaffold(
      backgroundColor: theme.scaffoldBackgroundColor,
      child: Stack(
        children: [
          CustomScrollView(
            physics: const BouncingScrollPhysics(),
            slivers: [
              CupertinoSliverNavigationBar(
                largeTitle: Text(
                  'ColAI',
                  style: TextStyle(
                    color: isDark ? Colors.white : Colors.black,
                  ),
                ),
                backgroundColor: Colors.transparent, // Fully immersive
                border: null,
                stretch: true,
                alwaysShowMiddle: false,
              ),
              CupertinoSliverRefreshControl(
                onRefresh: () async {
                  context.read<AIServicesBloc>().add(const LoadAIServices());
                },
              ),
              SliverPadding(
                padding: const EdgeInsets.fromLTRB(16, 16, 16, 120),
                sliver: BlocBuilder<AIServicesBloc, AIServicesState>(
                  builder: (context, state) {
                    if (state is AIServicesLoading) {
                      // Show skeleton loader that matches actual grid layout
                      return const AIServicesGridSkeleton(itemCount: 6);
                    }

                    if (state is AIServicesError) {
                      return SliverFillRemaining(
                        child: _buildError(context, state.message),
                      );
                    }

                    if (state is AIServicesLoaded) {
                      if (state.services.isEmpty) {
                        return SliverFillRemaining(
                          child: _buildEmptyState(context),
                        );
                      }

                      final crossAxisCount =
                          MediaQuery.of(context).size.width > 600 ? 4 : 3;

                      return SliverGrid(
                        gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
                          crossAxisCount: crossAxisCount,
                          crossAxisSpacing: 20, // More space
                          mainAxisSpacing: 20,
                          childAspectRatio: 0.72,
                        ),
                        delegate: SliverChildBuilderDelegate(
                          (context, index) {
                            final service = state.services[index];
                            return AIServiceCard(
                              service: service,
                              onTap: () => _openService(context, service),
                              onLongPress: () =>
                                  _showServiceOptions(context, service),
                            );
                          },
                          childCount: state.services.length,
                        ),
                      );
                    }

                    return const SliverToBoxAdapter(child: SizedBox.shrink());
                  },
                ),
              ),
            ],
          ),
          Positioned(
            left: 20,
            right: 20,
            bottom: 30,
            child: _buildFloatingNavBar(context, isDark),
          ),
        ],
      ),
    );
  }

  Widget _buildFloatingNavBar(BuildContext context, bool isDark) {
    return GlassmorphicContainer(
      width: double.infinity,
      height: 70,
      borderRadius: 35,
      blur: 20,
      alignment: Alignment.center,
      border: 1.5,
      linearGradient: LinearGradient(
        begin: Alignment.topLeft,
        end: Alignment.bottomRight,
        colors: [
          (isDark ? Colors.white : Colors.black).withValues(alpha: 0.1),
          (isDark ? Colors.white : Colors.black).withValues(alpha: 0.05),
        ],
      ),
      borderGradient: LinearGradient(
        begin: Alignment.topLeft,
        end: Alignment.bottomRight,
        colors: [
          (isDark ? Colors.white : Colors.black).withValues(alpha: 0.2),
          (isDark ? Colors.white : Colors.black).withValues(alpha: 0.05),
        ],
      ),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 24),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            _buildThemeToggle(context),
            _buildAddButton(context),
            _buildSettingsButton(context),
          ],
        ),
      ),
    );
  }

  Widget _buildAddButton(BuildContext context) {
    return Container(
      width: 50,
      height: 50,
      decoration: const BoxDecoration(
        color: CupertinoColors.activeBlue,
        shape: BoxShape.circle,
      ),
      child: CupertinoButton(
        padding: EdgeInsets.zero,
        onPressed: () {
          HapticFeedback.lightImpact();
          _goToAddService(context);
        },
        child: const Icon(CupertinoIcons.add, color: Colors.white, size: 28),
      ),
    );
  }

  Widget _buildSettingsButton(BuildContext context) {
    final theme = Theme.of(context);
    final isDark = theme.brightness == Brightness.dark;
    return CupertinoButton(
      padding: EdgeInsets.zero,
      onPressed: () {
        HapticFeedback.lightImpact();
        _goToSettings(context);
      },
      child: Icon(
        CupertinoIcons.settings,
        size: 24,
        color: isDark ? Colors.white : Colors.black,
      ),
    );
  }

  Widget _buildThemeToggle(BuildContext context) {
    final theme = Theme.of(context);
    final isDark = theme.brightness == Brightness.dark;
    return BlocBuilder<ThemeCubit, ThemeState>(
      builder: (context, state) {
        return CupertinoButton(
          padding: EdgeInsets.zero,
          child: Icon(
            state.themeMode == AppThemeMode.dark
                ? CupertinoIcons.moon_fill
                : CupertinoIcons.sun_max_fill,
            size: 24,
            color: isDark ? Colors.white : Colors.black,
          ),
          onPressed: () {
            HapticFeedback.mediumImpact();
            final nextMode = state.themeMode == AppThemeMode.dark
                ? AppThemeMode.light
                : AppThemeMode.dark;
            context.read<ThemeCubit>().changeTheme(nextMode);
          },
        );
      },
    );
  }

  Widget _buildError(BuildContext context, String message) {
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: GlassmorphicContainer(
          width: double.infinity,
          height: 300,
          borderRadius: 30,
          blur: 20,
          alignment: Alignment.center,
          border: 1,
          linearGradient: LinearGradient(
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
              colors: [
                (isDark ? Colors.white : Colors.black).withValues(alpha: 0.1),
                (isDark ? Colors.white : Colors.black).withValues(alpha: 0.05),
              ]),
          borderGradient: LinearGradient(
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
              colors: [
                (isDark ? Colors.white : Colors.black).withValues(alpha: 0.5),
                (isDark ? Colors.white : Colors.black).withValues(alpha: 0.2),
              ]),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Icon(
                CupertinoIcons.exclamationmark_triangle,
                size: 64,
                color: CupertinoColors.systemRed,
              ),
              const SizedBox(height: 16),
              Text(
                'Connection Issue',
                style: TextStyle(
                  fontSize: 22,
                  fontWeight: FontWeight.bold,
                  color: isDark ? Colors.white : Colors.black,
                  letterSpacing: -0.5,
                ),
              ),
              const SizedBox(height: 8),
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 24),
                child: Text(
                  message,
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    fontSize: 14,
                    color: (isDark ? Colors.white : Colors.black)
                        .withValues(alpha: 0.6),
                  ),
                ),
              ),
              const SizedBox(height: 24),
              Semantics(
                label: 'Retry loading services',
                child: CupertinoButton.filled(
                  borderRadius: BorderRadius.circular(12),
                  onPressed: () {
                    context.read<AIServicesBloc>().add(const LoadAIServices());
                  },
                  child: const Text('Try Again'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildEmptyState(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return Column(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        GlassmorphicContainer(
          width: 120,
          height: 120,
          borderRadius: 60,
          blur: 20,
          alignment: Alignment.center,
          border: 1,
          linearGradient: LinearGradient(
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
              colors: [
                (isDark ? Colors.white : Colors.black).withValues(alpha: 0.1),
                (isDark ? Colors.white : Colors.black).withValues(alpha: 0.05),
              ]),
          borderGradient: LinearGradient(
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
              colors: [
                (isDark ? Colors.white : Colors.black).withValues(alpha: 0.5),
                (isDark ? Colors.white : Colors.black).withValues(alpha: 0.2),
              ]),
          child: Icon(
            CupertinoIcons.plus,
            size: 48,
            color: isDark ? Colors.white : Colors.black,
          ),
        ),
        const SizedBox(height: 32),
        Text(
          'Your ColAI is Empty',
          style: TextStyle(
            fontSize: 24,
            fontWeight: FontWeight.bold,
            color: isDark ? Colors.white : Colors.black,
            letterSpacing: -0.8,
          ),
        ),
        const SizedBox(height: 12),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 48),
          child: Text(
            'Add your favorite AI services once and access them across all your accounts.',
            textAlign: TextAlign.center,
            style: TextStyle(
              fontSize: 15,
              color:
                  (isDark ? Colors.white : Colors.black).withValues(alpha: 0.6),
              height: 1.4,
            ),
          ),
        ),
        const SizedBox(height: 48),
        Semantics(
          label: 'Add a new AI service',
          child: CupertinoButton.filled(
            borderRadius: BorderRadius.circular(16),
            onPressed: () => _goToAddService(context),
            child: const Padding(
              padding: EdgeInsets.symmetric(horizontal: 24),
              child: Text('Get Started'),
            ),
          ),
        ),
      ],
    );
  }

  void _goToAddService(BuildContext context) {
    Navigator.of(context).push(
      CupertinoPageRoute(
        builder: (_) => const AddServiceScreen(),
      ),
    );
  }

  void _goToSettings(BuildContext context) {
    Navigator.of(context).push(
      CupertinoPageRoute(
        builder: (_) => const SettingsScreen(),
      ),
    );
  }

  void _openService(BuildContext context, AIService service) {
    final storage = context.read<ThemeCubit>().storageService;
    final ssoEnabled = storage.loadEnableSSO();

    if (ssoEnabled) {
      SecureBrowserLauncher.launch(
        context: context,
        url: service.url,
        name: service.name,
      );
      return;
    }

    Navigator.of(context).push(
      CupertinoPageRoute(
        builder: (_) => WebViewScreen(service: service),
      ),
    );
  }

  void _showServiceOptions(BuildContext context, AIService service) {
    final bool isDefault =
        DefaultAIServices.services.any((s) => s.id == service.id);

    showCupertinoModalPopup(
      context: context,
      builder: (context) => CupertinoActionSheet(
        title: Text(service.name),
        message: Text(service.url),
        actions: [
          CupertinoActionSheetAction(
            onPressed: () {
              context.read<AIServicesBloc>().add(
                    AIServiceFaviconRefreshed(service.id),
                  );
              Navigator.pop(context);
            },
            child: const Text('Refresh Favicon'),
          ),
          if (!isDefault)
            CupertinoActionSheetAction(
              isDestructiveAction: true,
              onPressed: () {
                context.read<AIServicesBloc>().add(
                      AIServiceDeleted(service.id),
                    );
                Navigator.pop(context);
              },
              child: const Text('Delete Service'),
            ),
        ],
        cancelButton: CupertinoActionSheetAction(
          onPressed: () => Navigator.pop(context),
          child: const Text('Cancel'),
        ),
      ),
    );
  }
}
