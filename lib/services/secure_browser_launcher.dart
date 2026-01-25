import 'package:flutter/material.dart';
import 'package:flutter_custom_tabs/flutter_custom_tabs.dart';
import 'package:ai_hub/services/logger_service.dart';

class SecureBrowserLauncher {
  /// Launch browser without any custom overlay
  static Future<void> launch({
    required BuildContext context,
    required String url,
    required String name,
  }) async {
    try {
      // Launch Custom Tab only
      await _launchCustomTab(context, url);
    } catch (e) {
      Logger.error('Error launching browser', e);
    }
  }

  static Future<void> _launchCustomTab(BuildContext context, String url) async {
    // We'll use a neutral white theme by default, or you can pass theme colors here.
    // For now, simplicity is best as per user's satisfaction.
    await launchUrl(
      Uri.parse(url),
      customTabsOptions: CustomTabsOptions(
        shareState: CustomTabsShareState.off,
        urlBarHidingEnabled: true,
        showTitle: true,
        closeButton: CustomTabsCloseButton(
          icon: CustomTabsCloseButtonIcons.back,
        ),
      ),
      safariVCOptions: const SafariViewControllerOptions(
        barCollapsingEnabled: true,
      ),
    );
  }
}
