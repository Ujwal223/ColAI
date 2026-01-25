import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_inappwebview/flutter_inappwebview.dart';
import 'package:ai_hub/models/ai_service.dart';
import 'package:ai_hub/models/session.dart';
import 'package:ai_hub/state/sessions_bloc.dart';
import 'package:ai_hub/state/theme_cubit.dart';
import 'package:ai_hub/services/system_theme_manager.dart';
import 'package:ai_hub/utils/user_agent_generator.dart';
import 'package:uuid/uuid.dart';
import 'package:path_provider/path_provider.dart';
import 'package:ai_hub/models/theme_mode.dart';
import 'dart:collection';
import 'dart:io';
import 'package:url_launcher/url_launcher.dart';
import 'package:ai_hub/services/session_privacy_service.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:gal/gal.dart';
import 'package:http/http.dart' as http;
import 'package:path/path.dart' as p;
import 'package:ai_hub/services/injection_scripts.dart';
import 'package:ai_hub/services/notification_service.dart';
import 'package:ai_hub/services/logger_service.dart';

class WebViewScreen extends StatefulWidget {
  final AIService service;
  final String? initialAction; // 'mic' or 'search'

  const WebViewScreen({super.key, required this.service, this.initialAction});

  @override
  State<WebViewScreen> createState() => _WebViewScreenState();
}

class _WebViewScreenState extends State<WebViewScreen>
    with SingleTickerProviderStateMixin {
  final Map<String, InAppWebViewController> _webViewControllers = {};
  final Map<String, Widget> _webViewWidgets = {};
  Session? _activeSession;
  List<Session> _sessions = [];
  ServiceTheme? _currentTheme;
  late AnimationController _refreshIconController;
  DateTime? _lastBackPress;
  static const _backPressDebounce = Duration(milliseconds: 300);
  double _progress = 0;

  @override
  void initState() {
    super.initState();
    _refreshIconController = AnimationController(
      vsync: this,
      duration: const Duration(seconds: 1),
    );
    _loadSessions();
  }

  @override
  void dispose() {
    // Save current session cookies before disposing
    if (_activeSession != null) {
      final controller = _webViewControllers[_activeSession!.id];
      if (controller != null) {
        SessionPrivacyService.saveCookies(controller, _activeSession!.id);
      }
    }

    // Clean up global active controllers map to prevent leaks
    for (final sessionId in _webViewControllers.keys) {
      SessionPrivacyService.activeControllers.remove(sessionId);
    }

    _refreshIconController.dispose();
    super.dispose();
  }

  // _applyInjections intentionally removed by user request

  void _loadSessions() {
    final sessionsBloc = context.read<SessionsBloc>();
    final themeState = context.read<ThemeCubit>().state;
    _currentTheme = SystemThemeManager.getKnownServiceTheme(
        widget.service.url, themeState.themeMode.isDark);

    if (sessionsBloc.state is SessionsLoaded) {
      final allSessions = (sessionsBloc.state as SessionsLoaded).sessions;
      _sessions =
          allSessions.where((s) => s.serviceId == widget.service.id).toList();

      if (_sessions.isEmpty) {
        _createSession('Primary');
      } else {
        _activeSession = _sessions.firstWhere(
          (s) => s.isDefault,
          orElse: () => _sessions.first,
        );
        setState(() {});
      }
    } else {
      sessionsBloc.add(const LoadSessions());
    }
  }

  Future<void> _createSession(String accountName) async {
    const uuid = Uuid();
    final sessionId = uuid.v4();
    String? storagePath;

    final appDir = await getApplicationDocumentsDirectory();
    final sessionDir = Directory('${appDir.path}/sessions/$sessionId');
    await sessionDir.create(recursive: true);
    storagePath = sessionDir.path;

    // Critically important: Save current session cookies before switching context
    if (_activeSession != null) {
      final currentController = _webViewControllers[_activeSession!.id];
      if (currentController != null) {
        await SessionPrivacyService.saveCookies(
            currentController, _activeSession!.id);
      }
    }

    final session = Session(
      id: sessionId,
      serviceId: widget.service.id,
      accountName: accountName,
      isDefault: _sessions.isEmpty,
      lastAccessed: DateTime.now(),
      cookieStorePath: storagePath,
    );

    if (!mounted) return;
    context.read<SessionsBloc>().add(SessionAdded(session));

    setState(() {
      _sessions.add(session);
      _activeSession = session;
    });
  }

  Future<void> _updateUserAgentIfNeeded(
      InAppWebViewController controller, String? url) async {
    if (url == null) return;

    final newUserAgent = UserAgentGenerator.getUserAgent(url: url);

    final settings = await controller.getSettings();

    if (settings != null && settings.userAgent != newUserAgent) {
      await controller.setSettings(
        settings: InAppWebViewSettings(userAgent: newUserAgent),
      );
    }
  }

  Future<void> _switchSession(Session newSession) async {
    if (_activeSession?.id == newSession.id) return;

    HapticFeedback.selectionClick();
    final oldSessionId = _activeSession?.id;
    if (oldSessionId != null) {
      final oldController = _webViewControllers[oldSessionId];
      if (oldController != null) {
        await SessionPrivacyService.saveCookies(oldController, oldSessionId);

        _webViewControllers.remove(oldSessionId);
        _webViewWidgets.remove(oldSessionId);
        SessionPrivacyService.activeControllers.remove(oldSessionId);
      }
    }

    setState(() {
      _activeSession = newSession;
    });

    if (!mounted) return;
    context.read<SessionsBloc>().add(SessionAccessed(newSession.id));

    await Future.delayed(const Duration(milliseconds: 100));
    final newController = _webViewControllers[newSession.id];
    if (newController != null) {
      await SessionPrivacyService.restoreCookies(newController, newSession.id);
      await newController.reload();

      if (!mounted) return;
      final themeMode = context.read<ThemeCubit>().state.themeMode;
      _detectAndApplyTheme(newController, themeMode);
    }
  }

  Future<void> _detectAndApplyTheme(
      InAppWebViewController controller, AppThemeMode themeMode) async {
    final detectedColor = await SystemThemeManager.detectThemeColor(controller);

    final detectedPrimary = await controller.evaluateJavascript(source: '''
      (function() {
        const primaryBtn = document.querySelector('button[type="submit"], .btn-primary, [class*="accent"], [class*="primary"]');
        if (primaryBtn) return window.getComputedStyle(primaryBtn).backgroundColor;
        return null;
      })();
    ''');

    if (detectedColor != null && mounted) {
      Color? primary;
      if (detectedPrimary != null && detectedPrimary is String) {
        final match =
            RegExp(r'rgba?\((\d+),\s*(\d+),\s*(\d+)(?:,\s*[\d\.]+)?\)')
                .firstMatch(detectedPrimary);
        if (match != null) {
          primary = Color.fromARGB(255, int.parse(match.group(1)!),
              int.parse(match.group(2)!), int.parse(match.group(3)!));
        }
      }

      setState(() {
        final bgColor = detectedColor;
        final luminance = bgColor.computeLuminance();
        final isLightBg = luminance > 0.6;

        _currentTheme = ServiceTheme(
          backgroundColor: bgColor,
          primaryColor: primary ??
              _currentTheme?.primaryColor ??
              CupertinoColors.activeBlue,
          textColor: isLightBg ? Colors.black : Colors.white,
          isDark: !isLightBg,
        );
      });
    }
  }

  void _showAddSessionDialog() {
    final controller = TextEditingController();
    showCupertinoDialog(
      context: context,
      barrierDismissible: true,
      builder: (context) => CupertinoTheme(
        data: CupertinoTheme.of(context).copyWith(
          barBackgroundColor: CupertinoColors.systemBackground,
        ),
        child: CupertinoAlertDialog(
          title: const Text('New Account Session'),
          content: Padding(
            padding: const EdgeInsets.only(top: 16),
            child: CupertinoTextField(
              controller: controller,
              placeholder: 'Account Name (e.g. Work, Personal)',
              autofocus: true,
              decoration: BoxDecoration(
                color: CupertinoColors.systemBackground.resolveFrom(context),
                borderRadius: BorderRadius.circular(8),
              ),
            ),
          ),
          actions: [
            CupertinoDialogAction(
              child: const Text('Cancel'),
              onPressed: () => Navigator.pop(context),
            ),
            CupertinoDialogAction(
              isDefaultAction: true,
              onPressed: () {
                final name = controller.text.trim();
                if (name.isNotEmpty) {
                  _createSession(name);
                  Navigator.pop(context);
                }
              },
              child: const Text('Create'),
            ),
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return BlocListener<SessionsBloc, SessionsState>(
      listener: (context, sessionsState) {
        if (sessionsState is SessionsLoaded) {
          final serviceSessions = sessionsState.sessions
              .where((s) => s.serviceId == widget.service.id)
              .toList();
          if (listEquals(_sessions, serviceSessions)) return;

          setState(() {
            _sessions = serviceSessions;
            if (_activeSession != null &&
                !_sessions.any((s) => s.id == _activeSession!.id)) {
              _activeSession = _sessions.isNotEmpty ? _sessions.first : null;
            }
            if (_sessions.isEmpty) {
              _createSession('Primary');
            }
          });
        }
      },
      child: BlocBuilder<ThemeCubit, ThemeState>(
        builder: (context, themeState) {
          return PopScope(
            canPop: false,
            onPopInvokedWithResult: (didPop, result) async {
              if (didPop) return;

              final now = DateTime.now();
              if (_lastBackPress != null &&
                  now.difference(_lastBackPress!) < _backPressDebounce) {
                return;
              }
              _lastBackPress = now;

              final navigator = Navigator.of(context);
              final controller = _webViewControllers[_activeSession?.id];

              bool canGoBack = false;
              try {
                canGoBack =
                    (controller != null && await controller.canGoBack());
              } catch (e) {
                // Ignore failures if controller is disposed or not ready
              }

              if (canGoBack) {
                await controller!.goBack();
              } else {
                if (mounted) navigator.pop();
              }
            },
            child: Scaffold(
              resizeToAvoidBottomInset: false,
              backgroundColor: _currentTheme?.backgroundColor ??
                  (themeState.themeMode.isDark
                      ? const Color(0xFF000000)
                      : const Color(0xFFF2F2F7)),
              appBar: CupertinoNavigationBar(
                middle: Text(
                  'ColAI',
                  style: TextStyle(
                    color: _currentTheme?.textColor ??
                        (themeState.themeMode.isDark
                            ? Colors.white
                            : Colors.black),
                  ),
                ),
                backgroundColor: _currentTheme?.backgroundColor ??
                    (themeState.themeMode.isDark
                        ? const Color(0xFF000000)
                        : const Color(0xFFF2F2F7)),
                border: null,
                leading: CupertinoButton(
                  padding: EdgeInsets.zero,
                  onPressed: () async {
                    final navigator = Navigator.of(context);
                    final controller = _webViewControllers[_activeSession?.id];
                    bool canGoBack = false;
                    try {
                      canGoBack =
                          (controller != null && await controller.canGoBack());
                    } catch (e) {
                      // Ignore failures if controller is disposed
                    }

                    if (canGoBack) {
                      await controller!.goBack();
                    } else {
                      if (mounted) navigator.pop();
                    }
                  },
                  child: Icon(
                    CupertinoIcons.chevron_back,
                    color: _currentTheme?.textColor ??
                        (themeState.themeMode.isDark
                            ? Colors.white
                            : Colors.black),
                  ),
                ),
                trailing: _activeSession != null
                    ? CupertinoButton(
                        padding: EdgeInsets.zero,
                        onPressed: () {
                          _refreshIconController.forward(from: 0.0);
                          _webViewControllers[_activeSession!.id]?.reload();
                        },
                        child: RotationTransition(
                          turns: _refreshIconController,
                          child: Icon(
                            CupertinoIcons.refresh,
                            size: 20,
                            color: _currentTheme?.textColor ??
                                (themeState.themeMode.isDark
                                    ? Colors.white
                                    : Colors.black),
                          ),
                        ),
                      )
                    : null,
              ) as ObstructingPreferredSizeWidget,
              body: Container(
                color: _currentTheme?.backgroundColor ??
                    (themeState.themeMode.isDark ? Colors.black : Colors.white),
                child: SafeArea(
                  bottom: false,
                  child: Column(
                    children: [
                      Expanded(
                        child: Stack(
                          children: [
                            Container(
                              color: _currentTheme?.backgroundColor,
                              child: _activeSession == null
                                  ? const Center(
                                      child: CupertinoActivityIndicator())
                                  : _buildWebView(
                                      _activeSession!,
                                      _getEffectiveThemeMode(
                                          _activeSession!, themeState)),
                            ),
                            if (_progress > 0 && _progress < 1.0)
                              Positioned(
                                top: 0,
                                left: 0,
                                right: 0,
                                child: SizedBox(
                                  height: 2.5,
                                  child: LinearProgressIndicator(
                                    value: _progress,
                                    backgroundColor: Colors.transparent,
                                    valueColor: AlwaysStoppedAnimation<Color>(
                                      _currentTheme?.primaryColor ??
                                          CupertinoColors.activeBlue,
                                    ),
                                  ),
                                ),
                              ),
                          ],
                        ),
                      ),
                      _buildSessionToolbar(themeState.themeMode),
                    ],
                  ),
                ),
              ),
            ),
          );
        },
      ),
    );
  }

  // _buildWebWarning removed

  Widget _buildWebView(Session session, AppThemeMode themeMode) {
    return InAppWebView(
      key: ValueKey(session.id), // Preserve state based on session ID
      initialSettings: InAppWebViewSettings(
        userAgent: UserAgentGenerator.getUserAgent(url: widget.service.url),
        transparentBackground: false,
        javaScriptEnabled: true,
        mixedContentMode: MixedContentMode.MIXED_CONTENT_ALWAYS_ALLOW,
        useHybridComposition: true, // Required for Samsung A15/Mali stability
        hardwareAcceleration:
            true, // Re-enable for performance, HC handles the composition
        javaScriptCanOpenWindowsAutomatically: true,
        supportMultipleWindows: true,
        cacheEnabled: true,
        domStorageEnabled: true,
        databaseEnabled: true,
        useWideViewPort: true,
        loadWithOverviewMode: true,
        saveFormData: true,
        isInspectable: kDebugMode,
        sharedCookiesEnabled: false,
      ),
      initialUserScripts: UnmodifiableListView([
        UserScript(
          source: """
            (function() {
              if (window.Notification && window.Notification.polyfill) return;
              
              const OriginalNotification = window.Notification;
              
              window.Notification = function(title, options) {
                this.title = title;
                this.options = options || {};
                
                window.flutter_inappwebview.callHandler('onWebNotification', {
                  title: title,
                  body: this.options.body || '',
                  tag: this.options.tag || ''
                });
                
                if (OriginalNotification) {
                  return new OriginalNotification(title, options);
                }
              };
              
              window.Notification.polyfill = true;
              window.Notification.permission = 'granted';
              window.Notification.requestPermission = function() {
                return Promise.resolve('granted');
              };
            })();
          """,
          injectionTime: UserScriptInjectionTime.AT_DOCUMENT_START,
        ),
      ]),
      onWebViewCreated: (controller) async {
        _webViewControllers[session.id] = controller;
        SessionPrivacyService.activeControllers[session.id] = controller;

        // Handle web notifications
        controller.addJavaScriptHandler(
          handlerName: 'onWebNotification',
          callback: (args) {
            if (args.isNotEmpty && widget.service.notificationsEnabled) {
              final data = args[0] as Map<String, dynamic>;
              NotificationService.showNotification(
                title: data['title'] ?? widget.service.name,
                body: data['body'] ?? '',
                payload: widget.service.id,
              );
            }
          },
        );

        // Core Isolation Fix: Restore cookies BEFORE loading the first page
        await SessionPrivacyService.restoreCookies(controller, session.id);

        // Controlled Load: Only start loading once the cookie vault is ready
        await controller.loadUrl(
          urlRequest: URLRequest(url: WebUri(widget.service.url)),
        );
      },
      onConsoleMessage: (controller, consoleMessage) {
        Logger.web(
            '[Console] ${consoleMessage.messageLevel}: ${consoleMessage.message}');
      },
      onLoadStart: (controller, url) async {
        Logger.web('Navigation started: $url');
        setState(() {
          _progress = 0;
        });
        _detectAndApplyTheme(controller, themeMode);
        _updateUserAgentIfNeeded(controller, url?.toString());
      },
      onLoadStop: (controller, url) async {
        Logger.web('Navigation finished: $url');
        _detectAndApplyTheme(controller, themeMode);

        // Handle initial widget actions (Mic/Search)
        if (widget.initialAction != null) {
          final script = widget.initialAction == 'mic'
              ? InjectionScripts.triggerVoice(widget.service.url)
              : InjectionScripts.focusSearch(widget.service.url);
          await controller.evaluateJavascript(source: script);
        }
        _updateUserAgentIfNeeded(controller, url?.toString());
        await SessionPrivacyService.saveCookies(controller, session.id);
      },
      onUpdateVisitedHistory: (controller, url, isReload) async {
        _detectAndApplyTheme(controller, themeMode);
        _updateUserAgentIfNeeded(controller, url?.toString());
      },
      onProgressChanged: (controller, progress) async {
        setState(() {
          _progress = progress / 100;
        });

        // Inject CSS fixes for specific providers
        if (progress == 100) {
          final css =
              InjectionScripts.getProviderSpecificCSS(widget.service.url);
          if (css.isNotEmpty) {
            await controller.injectCSSCode(source: css);
          }
        }
      },
      onPermissionRequest: (controller, request) async {
        final resources = request.resources;
        if (resources.contains(PermissionResourceType.NOTIFICATIONS)) {
          final status = await Permission.notification.status;
          bool systemAllowed = status.isGranted;
          bool serviceAllowed = widget.service.notificationsEnabled;
          bool sessionAllowed = session.notificationsEnabled;

          if (systemAllowed && serviceAllowed && sessionAllowed) {
            return PermissionResponse(
              resources: resources,
              action: PermissionResponseAction.GRANT,
            );
          } else {
            return PermissionResponse(
              resources: resources,
              action: PermissionResponseAction.DENY,
            );
          }
        }
        return PermissionResponse(
          resources: resources,
          action: PermissionResponseAction.PROMPT,
        );
      },
      onRenderProcessGone: (controller, detail) async {
        Logger.warn('WebView renderer process crashed: ${detail.didCrash}');
        // Recover from crash by reloading the WebView silently
        if (mounted) {
          await controller.reload();
        }
      },
      onReceivedServerTrustAuthRequest: (controller, challenge) async {
        return ServerTrustAuthResponse(
            action: ServerTrustAuthResponseAction.PROCEED);
      },
      onCreateWindow: (controller, createWindowAction) async {
        showCupertinoDialog(
          context: context,
          builder: (context) {
            return CupertinoPageScaffold(
              navigationBar: CupertinoNavigationBar(
                middle: const Text('Authentication'),
                leading: CupertinoButton(
                  padding: EdgeInsets.zero,
                  child: const Icon(CupertinoIcons.xmark),
                  onPressed: () => Navigator.of(context).pop(),
                ),
              ),
              child: SafeArea(
                child: InAppWebView(
                  windowId: createWindowAction.windowId,
                  initialSettings: InAppWebViewSettings(
                    userAgent: UserAgentGenerator.getUserAgent(),
                    javaScriptEnabled: true,
                    supportMultipleWindows: true,
                    javaScriptCanOpenWindowsAutomatically: true,
                    useHybridComposition:
                        true, // Consistent stability for popups
                    hardwareAcceleration: true,
                    transparentBackground: false,
                    sharedCookiesEnabled: false,
                  ),
                  onCloseWindow: (controller) {
                    Navigator.of(context).pop();
                  },
                ),
              ),
            );
          },
        );
        return true;
      },
      onDownloadStartRequest: (controller, request) async {
        final url = request.url;
        final fileName = request.suggestedFilename ??
            'download_${DateTime.now().millisecondsSinceEpoch}.png';

        try {
          if (!await Gal.hasAccess()) {
            await Gal.requestAccess();
          }

          String? filePath;
          final tempDir = await getTemporaryDirectory();
          filePath = p.join(tempDir.path, fileName);

          if (url.toString().startsWith('data:')) {
            final dataParts = url.toString().split(',');
            if (dataParts.length > 1) {
              final base64String = dataParts[1];
              final bytes = base64.decode(base64String);
              final file = File(filePath);
              await file.writeAsBytes(bytes);
            }
          } else {
            final response = await http.get(url);
            if (response.statusCode == 200) {
              final file = File(filePath);
              await file.writeAsBytes(response.bodyBytes);
            } else {
              throw Exception(
                  'Failed to download image: ${response.statusCode}');
            }
          }

          if (await File(filePath).exists()) {
            await Gal.putImage(filePath);
            if (mounted) {
              ScaffoldMessenger.of(context).showSnackBar(
                SnackBar(
                  content: Text('Saved $fileName to Gallery'),
                  backgroundColor: CupertinoColors.activeGreen,
                ),
              );
            }
          }
        } catch (e) {
          if (mounted) {
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(
                content: Text('Download failed: $e'),
                backgroundColor: CupertinoColors.destructiveRed,
              ),
            );
          }
        }
      },
      shouldOverrideUrlLoading: (controller, navigationAction) async {
        final uri = navigationAction.request.url;
        if (uri == null) return NavigationActionPolicy.ALLOW;

        final host = uri.host.toLowerCase();
        final urlString = uri.toString();

        // 1. Check for blocked patterns first
        final blockedPatterns = [
          'apps.apple.com',
          'play.google.com',
          'app-store',
          'download-app',
          'itunes.apple.com',
          'market.android.com',
          '/download',
          'install-app',
          'get-app'
        ];

        for (final pattern in blockedPatterns) {
          if (urlString.contains(pattern)) {
            Logger.info('Blocked redirect to: $urlString');
            return NavigationActionPolicy.CANCEL;
          }
        }

        // 2. Allow known Auth providers in-app (for login flows)
        final authDomains = [
          'accounts.google.com',
          'appleid.apple.com',
          'login.microsoftonline.com',
          'auth0.com',
          'auth.',
          'login.',
          'id.',
          'sso.',
          'oauth',
          'signin',
          'v2/auth'
        ];

        if (authDomains.any((d) => host.contains(d) || urlString.contains(d))) {
          return NavigationActionPolicy.ALLOW;
        }

        final googleDomains = [
          'google.com',
          'google.co',
          'gstatic.com',
          'googleapis.com',
          'googleusercontent.com',
          'youtube.com',
          'ytimg.com',
          'ggpht.com',
          'doubleclick.net',
          'google-analytics.com',
          'googletagmanager.com'
        ];

        final microsoftDomains = [
          'microsoft.com',
          'microsoftonline.com',
          'live.com',
          'bing.com',
          'azure.com',
          'vortex.data.microsoft.com'
        ];

        if (googleDomains.any((d) => host.contains(d)) ||
            microsoftDomains.any((d) => host.contains(d))) {
          return NavigationActionPolicy.ALLOW;
        }

        final serviceUri = WebUri(widget.service.url);
        final serviceHost = serviceUri.host.toLowerCase();

        final whitelistPatterns = [
          r'.*openai\\.com$',
          r'.*chatgpt\\.com$',
          r'.*anthropic\\.com$',
          r'.*claude\\.ai$',
          r'.*x\\.ai$',
          r'.*grok\\.com$',
          r'.*perplexity\\.ai$',
          r'.*meta\\.com$',
          r'.*facebook\\.com$',
          r'.*apple\\.com$',
          r'.*github\\.com$',
          r'.*deepseek\\.com$'
        ];

        bool isWhitelisted = host.contains(serviceHost) ||
            serviceHost.contains(host) ||
            host.isEmpty;

        if (!isWhitelisted) {
          for (final pattern in whitelistPatterns) {
            if (RegExp(pattern).hasMatch(host)) {
              isWhitelisted = true;
              break;
            }
          }
        }

        if (isWhitelisted) {
          return NavigationActionPolicy.ALLOW;
        }

        // 3. Launch everything else external
        if (await canLaunchUrl(uri)) {
          await launchUrl(uri, mode: LaunchMode.externalApplication);
        }
        return NavigationActionPolicy.CANCEL;
      },
    );
  }

  Widget _buildSessionToolbar(AppThemeMode themeMode) {
    return Material(
      color: Colors.transparent,
      child: Container(
        padding: EdgeInsets.only(
            bottom: MediaQuery.of(context).padding.bottom + 8, top: 4),
        decoration: BoxDecoration(
          color: _currentTheme?.backgroundColor ??
              (themeMode.isDark ? Colors.black : Colors.white),
          border: Border(
              top: BorderSide(
                  color: (themeMode.isDark ? Colors.white : Colors.black)
                      .withValues(alpha: 0.1),
                  width: 0.5)),
        ),
        child: SingleChildScrollView(
          scrollDirection: Axis.horizontal,
          padding: const EdgeInsets.symmetric(horizontal: 16),
          child: Row(
            children: [
              ..._sessions.map((s) => _buildSessionTab(s, themeMode)),
              CupertinoButton(
                padding: const EdgeInsets.only(left: 12),
                onPressed: _showAddSessionDialog,
                child: Icon(
                  CupertinoIcons.add_circled,
                  size: 28,
                  color: widget.service.id == 'chatgpt' ||
                          widget.service.id == 'grok' ||
                          widget.service.id == 'deepseek'
                      ? Colors.white
                      : (_currentTheme?.primaryColor ??
                          CupertinoColors.activeBlue),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildSessionTab(Session session, AppThemeMode themeMode) {
    bool isActive = session.id == _activeSession?.id;
    return GestureDetector(
      onTap: () => _switchSession(session),
      onLongPress: () => _showSessionOptions(session),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        margin: const EdgeInsets.only(right: 8),
        decoration: BoxDecoration(
          color: isActive
              ? (_currentTheme?.primaryColor ?? CupertinoColors.activeBlue)
              : Colors.transparent,
          borderRadius: BorderRadius.circular(12),
          border: isActive
              ? null
              : Border.all(
                  color: (_currentTheme?.textColor ??
                          (themeMode.isDark ? Colors.white : Colors.black))
                      .withValues(alpha: 0.1),
                  width: 1,
                ),
        ),
        child: Text(
          session.accountName,
          style: TextStyle(
            color: isActive
                ? (_currentTheme?.backgroundColor ??
                    (themeMode.isDark ? Colors.black : Colors.white))
                : (_currentTheme?.textColor.withValues(alpha: 0.6) ??
                    (themeMode.isDark ? Colors.white60 : Colors.black54)),
            fontWeight: isActive ? FontWeight.w700 : FontWeight.w500,
            fontSize: 14,
          ),
        ),
      ),
    );
  }

  void _showSessionOptions(Session session) {
    showCupertinoModalPopup(
      context: context,
      builder: (context) => CupertinoActionSheet(
        title: Text('Session: ${session.accountName}'),
        actions: [
          CupertinoActionSheetAction(
            isDestructiveAction: true,
            onPressed: () {
              Navigator.pop(context);
              _deleteSession(session);
            },
            child: const Text('Delete Session'),
          ),
        ],
        cancelButton: CupertinoActionSheetAction(
          onPressed: () => Navigator.pop(context),
          child: const Text('Cancel'),
        ),
      ),
    );
  }

  Future<void> _deleteSession(Session session) async {
    if (_sessions.length == 1) return;
    context.read<SessionsBloc>().add(SessionDeleted(session.id));
    setState(() {
      _sessions.removeWhere((s) => s.id == session.id);

      if (_activeSession?.id == session.id) {
        _webViewControllers.remove(session.id);
        _webViewWidgets.remove(session.id);
        SessionPrivacyService.activeControllers.remove(session.id);
        _activeSession = _sessions.first;
      }
    });
  }

  AppThemeMode _getEffectiveThemeMode(Session session, ThemeState themeState) {
    // Check if session has a custom theme override
    if (session.themeMode == 'light') return AppThemeMode.light;
    if (session.themeMode == 'dark') return AppThemeMode.dark;
    // Otherwise use the app's current theme
    return themeState.themeMode;
  }
}
