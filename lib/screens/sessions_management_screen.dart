import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:ai_hub/models/session.dart';
import 'package:ai_hub/models/ai_service.dart';
import 'package:ai_hub/state/sessions_bloc.dart';
import 'package:ai_hub/state/ai_services_bloc.dart';
import 'dart:io';

class SessionsManagementScreen extends StatelessWidget {
  const SessionsManagementScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return CupertinoPageScaffold(
      backgroundColor: CupertinoColors.systemGroupedBackground,
      navigationBar: const CupertinoNavigationBar(
        middle: Text('Manage Sessions'),
        previousPageTitle: 'Settings',
      ),
      child: SafeArea(
        bottom: false,
        child: BlocBuilder<AIServicesBloc, AIServicesState>(
          builder: (context, servicesState) {
            return BlocBuilder<SessionsBloc, SessionsState>(
              builder: (context, sessionsState) {
                if (servicesState is! AIServicesLoaded ||
                    sessionsState is! SessionsLoaded) {
                  return const Center(child: CupertinoActivityIndicator());
                }

                final services = servicesState.services;
                final sessions = sessionsState.sessions;

                if (sessions.isEmpty) {
                  return Center(
                    child: DefaultTextStyle(
                      style: CupertinoTheme.of(context).textTheme.textStyle,
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Icon(
                            CupertinoIcons.square_stack_3d_up_slash,
                            size: 64,
                            color: CupertinoColors.systemGrey
                                .withValues(alpha: 0.5),
                          ),
                          const SizedBox(height: 16),
                          Text(
                            'No Active Sessions',
                            style: TextStyle(
                              color: CupertinoColors.systemGrey
                                  .resolveFrom(context),
                              fontSize: 17,
                              fontWeight: FontWeight.w500,
                            ),
                          ),
                        ],
                      ),
                    ),
                  );
                }

                return ListView(
                  key: ValueKey('sessions_list_${sessions.length}'),
                  physics: const BouncingScrollPhysics(),
                  children: [
                    ...services.map((service) {
                      final serviceSessions = sessions
                          .where((s) => s.serviceId == service.id)
                          .toList();

                      if (serviceSessions.isEmpty) {
                        return const SizedBox.shrink();
                      }

                      return _buildServiceSection(
                        context,
                        service,
                        serviceSessions,
                      );
                    }),
                    const SizedBox(height: 32),
                    Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 16),
                      child: CupertinoButton(
                        color: CupertinoColors.destructiveRed,
                        onPressed: () => _confirmDeleteAll(context),
                        child: const Text('Delete All Sessions'),
                      ),
                    ),
                    const SizedBox(height: 48),
                  ],
                );
              },
            );
          },
        ),
      ),
    );
  }

  Widget _buildServiceSection(
    BuildContext context,
    AIService service,
    List<Session> sessions,
  ) {
    return CupertinoFormSection.insetGrouped(
      header: Text(service.name.toUpperCase()),
      children: sessions.map((session) {
        return CupertinoFormRow(
          prefix: Row(
            children: [
              Container(
                width: 32,
                height: 32,
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(8),
                  boxShadow: [
                    BoxShadow(
                      color: Colors.black.withValues(alpha: 0.05),
                      blurRadius: 4,
                      offset: const Offset(0, 2),
                    ),
                  ],
                ),
                padding: const EdgeInsets.all(4),
                child: service.iconPath != null
                    ? (service.iconPath!.startsWith('/')
                        ? Image.file(File(service.iconPath!))
                        : Image.asset(service.iconPath!))
                    : service.faviconUrl.isNotEmpty
                        ? Image.network(service.faviconUrl)
                        : const Icon(CupertinoIcons.globe, size: 20),
              ),
              const SizedBox(width: 12),
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    session.accountName,
                    style: const TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                  Text(
                    'Last used: ${_formatDate(session.lastAccessed)}',
                    style: TextStyle(
                      fontSize: 12,
                      color: CupertinoColors.systemGrey.resolveFrom(context),
                    ),
                  ),
                ],
              ),
            ],
          ),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              if (session.isDefault)
                const Padding(
                  padding: EdgeInsets.only(right: 8),
                  child: Icon(
                    CupertinoIcons.star_fill,
                    size: 16,
                    color: CupertinoColors.systemYellow,
                  ),
                ),
              CupertinoButton(
                padding: EdgeInsets.zero,
                onPressed: () => _confirmDelete(context, session),
                child: const Icon(
                  CupertinoIcons.trash,
                  size: 22,
                  color: CupertinoColors.destructiveRed,
                ),
              ),
            ],
          ),
        );
      }).toList(),
    );
  }

  void _confirmDelete(BuildContext context, Session session) {
    showCupertinoDialog(
      context: context,
      builder: (ctx) => CupertinoAlertDialog(
        title: const Text('Delete Session?'),
        content: Text(
            'This will permanently delete authentication for "${session.accountName}" on this device.'),
        actions: [
          CupertinoDialogAction(
            child: const Text('Cancel'),
            onPressed: () => Navigator.pop(ctx),
          ),
          CupertinoDialogAction(
            isDestructiveAction: true,
            onPressed: () {
              context.read<SessionsBloc>().add(SessionDeleted(session.id));
              Navigator.pop(ctx);
            },
            child: const Text('Delete'),
          ),
        ],
      ),
    );
  }

  void _confirmDeleteAll(BuildContext context) {
    showCupertinoDialog(
      context: context,
      builder: (ctx) => CupertinoAlertDialog(
        title: const Text('Clear All Data?'),
        content: const Text(
            'This will log you out of all AI services and delete all cached session data. This cannot be undone.'),
        actions: [
          CupertinoDialogAction(
            child: const Text('Cancel'),
            onPressed: () => Navigator.pop(ctx),
          ),
          CupertinoDialogAction(
            isDestructiveAction: true,
            onPressed: () {
              context.read<SessionsBloc>().add(SessionsCleared());
              Navigator.pop(ctx);
            },
            child: const Text('Delete All'),
          ),
        ],
      ),
    );
  }

  String _formatDate(DateTime date) {
    final now = DateTime.now();
    final difference = now.difference(date);

    if (difference.inMinutes < 60) {
      return '${difference.inMinutes}m ago';
    } else if (difference.inHours < 24) {
      return '${difference.inHours}h ago';
    } else {
      return '${date.month}/${date.day}';
    }
  }
}
