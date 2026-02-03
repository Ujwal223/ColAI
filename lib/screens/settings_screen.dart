import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:ai_hub/models/ai_service.dart';
import 'package:ai_hub/models/session.dart';
import 'package:ai_hub/state/ai_services_bloc.dart';
import 'package:ai_hub/state/sessions_bloc.dart';
import 'package:ai_hub/state/theme_cubit.dart';
import 'package:ai_hub/models/theme_mode.dart';
import 'package:ai_hub/screens/notifications_settings_screen.dart';
import 'package:ai_hub/screens/about_screen.dart';
import 'package:ai_hub/screens/sessions_management_screen.dart';

class SettingsScreen extends StatelessWidget {
  const SettingsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return CupertinoPageScaffold(
      backgroundColor: Theme.of(context).scaffoldBackgroundColor,
      navigationBar: const CupertinoNavigationBar(
        middle: Text('Settings'),
      ),
      child: SafeArea(
        child: Material(
          color: Theme.of(context).scaffoldBackgroundColor,
          child: Container(
            color: Theme.of(context).scaffoldBackgroundColor,
            child: BlocBuilder<ThemeCubit, ThemeState>(
              builder: (context, themeState) {
                return BlocBuilder<AIServicesBloc, AIServicesState>(
                  builder: (context, servicesState) {
                    return BlocBuilder<SessionsBloc, SessionsState>(
                      builder: (context, sessionsState) {
                        return ListView(
                          physics: const BouncingScrollPhysics(),
                          children: [
                            _buildAppearanceSection(context, themeState),
                            _buildBrowsingSection(context, themeState),
                            _buildManagementSection(context),
                            if (servicesState is AIServicesLoaded &&
                                sessionsState is SessionsLoaded) ...[
                              _buildWidgetConfigurationSection(
                                  context,
                                  servicesState.services,
                                  sessionsState.sessions),
                            ],
                            const SizedBox(height: 48),
                          ],
                        );
                      },
                    );
                  },
                );
              },
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildAppearanceSection(BuildContext context, ThemeState state) {
    return CupertinoFormSection.insetGrouped(
      backgroundColor: Theme.of(context).scaffoldBackgroundColor,
      header: const Text('APPEARANCE'),
      children: [
        CupertinoFormRow(
          prefix: const Text('Dark Mode'),
          child: CupertinoSwitch(
            value: state.themeMode == AppThemeMode.dark,
            onChanged: (value) {
              context
                  .read<ThemeCubit>()
                  .changeTheme(value ? AppThemeMode.dark : AppThemeMode.light);
            },
          ),
        ),
        CupertinoFormRow(
          prefix: const Text('Contrast'),
          child: CupertinoTheme(
            data: CupertinoTheme.of(context), // Ensures propagation
            child: CupertinoSlidingSegmentedControl<ContrastLevel>(
              groupValue: state.contrastLevel,
              onValueChanged: (level) {
                if (level != null) {
                  context.read<ThemeCubit>().changeContrast(level);
                }
              },
              children: {
                ContrastLevel.relaxed: Text('Relaxed',
                    style: TextStyle(
                        fontSize: 14,
                        color: CupertinoColors.label.resolveFrom(context))),
                ContrastLevel.high: Text('High',
                    style: TextStyle(
                        fontSize: 14,
                        color: CupertinoColors.label.resolveFrom(context))),
              },
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildBrowsingSection(BuildContext context, ThemeState state) {
    final storage = context.read<ThemeCubit>().storageService;

    return FutureBuilder<bool>(
      future: Future.value(storage.loadEnableSSO()),
      builder: (context, snapshot) {
        // If loading, show default false (or previous value if we had it).
        // To make it "instant", we use a StatefulBuilder to toggle visual state immediately
        // while the storage save happens in background.
        bool enabled = snapshot.data ?? false;

        return StatefulBuilder(
          builder: (context, setStateLocal) {
            return CupertinoFormSection.insetGrouped(
              backgroundColor: Theme.of(context).scaffoldBackgroundColor,
              header: const Text('BROWSING'),
              children: [
                CupertinoFormRow(
                  prefix: const Text('Enable SSO Login'),
                  helper: const Text(
                    'Opens login pages in a Custom Chrome Tab. '
                    'Enable this if you have trouble logging in.',
                    style: TextStyle(
                        fontSize: 12, color: CupertinoColors.systemGrey),
                  ),
                  child: CupertinoSwitch(
                    value: enabled,
                    onChanged: (value) async {
                      setStateLocal(() {
                        enabled = value;
                      });
                      await storage.saveEnableSSO(value);
                    },
                  ),
                ),
              ],
            );
          },
        );
      },
    );
  }

  Widget _buildManagementSection(BuildContext context) {
    return CupertinoFormSection.insetGrouped(
      backgroundColor: Theme.of(context).scaffoldBackgroundColor,
      header: const Text('MANAGEMENT'),
      children: [
        CupertinoFormRow(
          prefix: const Text('Sessions'),
          child: CupertinoButton(
            padding: EdgeInsets.zero,
            onPressed: () {
              Navigator.of(context).push(
                CupertinoPageRoute(
                    builder: (_) => const SessionsManagementScreen()),
              );
            },
            child: const Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text('Manage',
                    style: TextStyle(
                        fontSize: 15, color: CupertinoColors.activeBlue)),
                Icon(CupertinoIcons.chevron_right, size: 16),
              ],
            ),
          ),
        ),
        CupertinoFormRow(
          prefix: const Text('Notifications'),
          child: CupertinoButton(
            padding: EdgeInsets.zero,
            onPressed: () {
              Navigator.of(context).push(
                CupertinoPageRoute(
                    builder: (_) => const NotificationSettingsScreen()),
              );
            },
            child: const Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text('Configure',
                    style: TextStyle(
                        fontSize: 15, color: CupertinoColors.activeBlue)),
                Icon(CupertinoIcons.chevron_right, size: 16),
              ],
            ),
          ),
        ),
        CupertinoFormRow(
          prefix: const Text('About ColAI'),
          child: CupertinoButton(
            padding: EdgeInsets.zero,
            onPressed: () {
              Navigator.of(context).push(
                CupertinoPageRoute(builder: (_) => const AboutScreen()),
              );
            },
            child: const Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text('v1.0.1',
                    style: TextStyle(
                        fontSize: 15, color: CupertinoColors.systemGrey)),
                Icon(CupertinoIcons.chevron_right, size: 16),
              ],
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildWidgetConfigurationSection(BuildContext context,
      List<AIService> services, List<Session> allSessions) {
    if (services.isEmpty) return const SizedBox.shrink();

    AIService? widgetService;
    Session? widgetSession;

    for (var service in services) {
      if (service.widgetSessionId != null) {
        widgetService = service;
        widgetSession = allSessions.firstWhere(
          (s) => s.id == service.widgetSessionId,
          orElse: () => allSessions.firstWhere(
            (s) => s.serviceId == service.id,
            orElse: () => allSessions.first,
          ),
        );
        break;
      }
    }

    widgetService ??= services.first;
    widgetSession ??= allSessions.isNotEmpty
        ? allSessions.firstWhere(
            (s) => s.serviceId == widgetService!.id,
            orElse: () => allSessions.first,
          )
        : null;

    final displayText = widgetSession != null
        ? '${widgetService.name} (${widgetSession.accountName})'
        : '${widgetService.name} (No Sessions)';

    return CupertinoFormSection.insetGrouped(
      backgroundColor: Theme.of(context).scaffoldBackgroundColor,
      header: const Text('WIDGET CONFIGURATION'),
      children: [
        CupertinoFormRow(
          prefix: const Text('Widget Opens', style: TextStyle(fontSize: 15)),
          child: CupertinoButton(
            padding: EdgeInsets.zero,
            onPressed: () =>
                _showUnifiedWidgetPicker(context, services, allSessions),
            child: Text(
              displayText,
              style: TextStyle(
                  fontSize: 14,
                  color: CupertinoColors.label.resolveFrom(context)),
              overflow: TextOverflow.ellipsis,
            ),
          ),
        ),
      ],
    );
  }

  void _showUnifiedWidgetPicker(BuildContext context, List<AIService> services,
      List<Session> allSessions) {
    showCupertinoModalPopup(
      context: context,
      builder: (context) => CupertinoActionSheet(
        title: const Text('Select Widget Target'),
        message:
            const Text('Which service and account should the widget open?'),
        actions: services.expand((service) {
          final sessions =
              allSessions.where((s) => s.serviceId == service.id).toList();

          if (sessions.isEmpty) {
            return [
              CupertinoActionSheetAction(
                onPressed: () {},
                child: Text('${service.name} (No Sessions)',
                    style: const TextStyle(color: CupertinoColors.systemGrey)),
              ),
            ];
          }

          return sessions.map((session) {
            return CupertinoActionSheetAction(
              onPressed: () {
                for (var s in services) {
                  if (s.widgetSessionId != null) {
                    context.read<AIServicesBloc>().add(
                          AIServiceUpdated(s.copyWith(widgetSessionId: null)),
                        );
                  }
                }
                context.read<AIServicesBloc>().add(
                      AIServiceUpdated(
                          service.copyWith(widgetSessionId: session.id)),
                    );
                Navigator.pop(context);
              },
              child: Text('${service.name} - ${session.accountName}'),
            );
          });
        }).toList(),
        cancelButton: CupertinoActionSheetAction(
          onPressed: () => Navigator.pop(context),
          child: const Text('Cancel'),
        ),
      ),
    );
  }
}
