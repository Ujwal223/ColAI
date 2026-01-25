import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:ai_hub/state/ai_services_bloc.dart';
import 'package:ai_hub/state/sessions_bloc.dart';

class NotificationSettingsScreen extends StatefulWidget {
  const NotificationSettingsScreen({super.key});

  @override
  State<NotificationSettingsScreen> createState() =>
      _NotificationSettingsScreenState();
}

class _NotificationSettingsScreenState
    extends State<NotificationSettingsScreen> {
  @override
  Widget build(BuildContext context) {
    return CupertinoPageScaffold(
      backgroundColor: Theme.of(context).scaffoldBackgroundColor,
      navigationBar: const CupertinoNavigationBar(
        middle: Text('Notifications'),
      ),
      child: SafeArea(
        child: BlocBuilder<AIServicesBloc, AIServicesState>(
          builder: (context, servicesState) {
            return BlocBuilder<SessionsBloc, SessionsState>(
              builder: (context, sessionsState) {
                if (servicesState is! AIServicesLoaded ||
                    sessionsState is! SessionsLoaded) {
                  return const Center(child: CupertinoActivityIndicator());
                }

                return ListView(
                  children: [
                    FutureBuilder<PermissionStatus>(
                      future: Permission.notification.status,
                      builder: (context, snapshot) {
                        final systemAllowed = snapshot.data?.isGranted ?? false;
                        return CupertinoFormSection.insetGrouped(
                          backgroundColor:
                              Theme.of(context).scaffoldBackgroundColor,
                          header: const Text('APP PERMISSION'),
                          footer: Text(
                            systemAllowed
                                ? 'The app has permission to send notifications.'
                                : 'The app needs system permission to send notifications.',
                          ),
                          children: [
                            CupertinoFormRow(
                              prefix: const Text('Allow Notifications'),
                              child: CupertinoButton(
                                padding: EdgeInsets.zero,
                                onPressed: () async {
                                  await Permission.notification.request();
                                  setState(() {});
                                },
                                child: Text(systemAllowed
                                    ? 'Allowed'
                                    : 'Request Access'),
                              ),
                            ),
                          ],
                        );
                      },
                    ),
                    ...servicesState.services.map((service) {
                      return CupertinoFormSection.insetGrouped(
                        backgroundColor:
                            Theme.of(context).scaffoldBackgroundColor,
                        header: Text(service.name.toUpperCase()),
                        children: [
                          CupertinoFormRow(
                            prefix: Text(
                                'Receive Notifications from ${service.name}'),
                            child: CupertinoSwitch(
                              value: service.notificationsEnabled,
                              onChanged: (value) {
                                context.read<AIServicesBloc>().add(
                                      AIServiceUpdated(service.copyWith(
                                          notificationsEnabled: value)),
                                    );
                              },
                            ),
                          ),
                        ],
                      );
                    }),
                    const SizedBox(height: 40),
                  ],
                );
              },
            );
          },
        ),
      ),
    );
  }
}
