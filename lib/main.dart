import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_inappwebview/flutter_inappwebview.dart';
import 'package:ai_hub/state/ai_services_bloc.dart';
import 'package:ai_hub/state/sessions_bloc.dart';
import 'package:ai_hub/state/theme_cubit.dart';
import 'package:ai_hub/services/storage_service.dart';
import 'package:ai_hub/theme/app_theme.dart';
import 'package:ai_hub/models/theme_mode.dart';
import 'package:flutter/cupertino.dart';
import 'package:home_widget/home_widget.dart';
import 'package:ai_hub/screens/webview_screen.dart';
import 'package:ai_hub/services/session_privacy_service.dart';
import 'package:ai_hub/screens/onboarding_screen.dart';
import 'package:ai_hub/screens/home_screen.dart';
import 'package:ai_hub/services/notification_service.dart';
import 'package:ai_hub/services/secure_browser_launcher.dart';
import 'package:ai_hub/services/logger_service.dart';

void main() async {
  try {
    WidgetsFlutterBinding.ensureInitialized();

    // Initialize notification service - Catching errors so it doesn't block startup
    try {
      await NotificationService.init();
    } catch (e) {
      Logger.error('Notification init error', e);
    }

    // Initialize WebView platform
    if (Platform.isAndroid) {
      await InAppWebViewController.setWebContentsDebuggingEnabled(true);
    }

    // Initialize storage service
    final storageService = await StorageService.create();

    // Home Screen Widget setup
    HomeWidget.setAppGroupId('group.colai');

    // Set system UI overlay style
    SystemChrome.setSystemUIOverlayStyle(
      const SystemUiOverlayStyle(
        statusBarColor: Colors.transparent,
        statusBarIconBrightness: Brightness.light,
        statusBarBrightness: Brightness.dark,
        systemNavigationBarColor: Color(
            0x02000000), // More standard way to define transparent-ish color
        systemNavigationBarDividerColor: Colors.transparent,
        systemNavigationBarIconBrightness: Brightness.light,
        systemNavigationBarContrastEnforced: false,
      ),
    );

    // Use a more stable UI mode for gesture compatibility
    SystemChrome.setEnabledSystemUIMode(
      SystemUiMode.manual,
      overlays: [SystemUiOverlay.top, SystemUiOverlay.bottom],
    );

    runApp(AIHubApp(storageService: storageService));
  } catch (e) {
    Logger.error('CRITICAL STARTUP ERROR', e);
    // Even if everything fails, try to start with a safe fallback if possible
    // but better to fix the root cause.
  }
}

class AIHubApp extends StatefulWidget {
  final StorageService storageService;

  const AIHubApp({super.key, required this.storageService});

  @override
  State<AIHubApp> createState() => _AIHubAppState();
}

class _AIHubAppState extends State<AIHubApp> with WidgetsBindingObserver {
  final GlobalKey<NavigatorState> _navigatorKey = GlobalKey<NavigatorState>();

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _handleWidgetLaunch();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.paused ||
        state == AppLifecycleState.inactive) {
      _persistAllCookies();
    }
  }

  Future<void> _persistAllCookies() async {
    // Definitive cookie save and flush of all active webview controllers
    // when the app enters the background.
    try {
      await SessionPrivacyService.saveAllCookies(
          SessionPrivacyService.activeControllers);
    } catch (e) {
      // Silence background persistence errors
    }
  }

  Future<void> _handleWidgetLaunch() async {
    // Check if launched from widget
    final uri = await HomeWidget.initiallyLaunchedFromHomeWidget();
    if (uri != null && uri.scheme == 'colai') {
      _processWidgetClick(uri);
    }

    // Listen for background clicks
    HomeWidget.widgetClicked.listen((uri) {
      if (uri != null && uri.scheme == 'colai') {
        _processWidgetClick(uri);
      }
    });
  }

  void _processWidgetClick(Uri uri) {
    // Logic to find the service and navigate
    Future.delayed(const Duration(milliseconds: 600), () {
      if (!mounted) return;
      final context = _navigatorKey.currentContext;
      if (context != null && context.mounted) {
        final servicesBloc = BlocProvider.of<AIServicesBloc>(context);
        if (servicesBloc.state is AIServicesLoaded) {
          final services = (servicesBloc.state as AIServicesLoaded).services;
          if (services.isNotEmpty) {
            // Find service with configured widget session, or first
            final service = services.firstWhere(
              (s) => s.widgetSessionId != null,
              orElse: () => services.first,
            );

            // Determine mode from URI
            String? initialAction;
            if (uri.path.contains('mic')) {
              initialAction = 'mic';
            } else if (uri.path.contains('search')) {
              initialAction = 'search';
            }

            final isSSOEnabled = widget.storageService.loadEnableSSO();

            // If SSO is on and NOT a special mode click, use SecureBrowser
            if (isSSOEnabled && initialAction == null) {
              SecureBrowserLauncher.launch(
                context: context,
                url: service.url,
                name: service.name,
              );
              return;
            }

            _navigatorKey.currentState?.push(
              CupertinoPageRoute(
                builder: (_) => WebViewScreen(
                  service: service,
                  initialAction: initialAction,
                ),
              ),
            );
          }
        }
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return MultiBlocProvider(
      providers: [
        BlocProvider(
          create: (context) => ThemeCubit(widget.storageService),
        ),
        BlocProvider(
          create: (context) =>
              AIServicesBloc(widget.storageService)..add(LoadAIServices()),
        ),
        BlocProvider(
          create: (context) =>
              SessionsBloc(widget.storageService)..add(const LoadSessions()),
        ),
      ],
      child: BlocBuilder<ThemeCubit, ThemeState>(
        builder: (context, themeState) {
          return MaterialApp(
            navigatorKey: _navigatorKey,
            title: 'ColAI',
            debugShowCheckedModeBanner: false,
            // Proper theme mode configuration
            themeMode: themeState.themeMode == AppThemeMode.light
                ? ThemeMode.light
                : ThemeMode.dark,
            theme:
                AppTheme.getTheme(AppThemeMode.light, themeState.contrastLevel),
            darkTheme:
                AppTheme.getTheme(AppThemeMode.dark, themeState.contrastLevel),
            themeAnimationDuration: const Duration(milliseconds: 350),
            themeAnimationCurve: Curves.easeInOutCubic,
            builder: (context, child) {
              final isDark = themeState.themeMode.isDark;
              // Ensure child is preserved properly
              if (child == null) return const SizedBox.shrink();

              return AnnotatedRegion<SystemUiOverlayStyle>(
                value: SystemUiOverlayStyle(
                  statusBarColor: Colors.transparent,
                  statusBarIconBrightness:
                      isDark ? Brightness.light : Brightness.dark,
                  statusBarBrightness:
                      isDark ? Brightness.dark : Brightness.light,
                  systemNavigationBarColor:
                      isDark ? Colors.black : Colors.white,
                  systemNavigationBarDividerColor: Colors.transparent,
                  systemNavigationBarIconBrightness:
                      isDark ? Brightness.light : Brightness.dark,
                  systemNavigationBarContrastEnforced: false,
                ),
                child: child,
              );
            },

            home: widget.storageService.loadOnboardingComplete()
                ? const HomeScreen()
                : const OnboardingScreen(),
          );
        },
      ),
    );
  }
}
