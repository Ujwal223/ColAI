import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';

class AboutScreen extends StatelessWidget {
  const AboutScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return CupertinoPageScaffold(
      backgroundColor: Theme.of(context).scaffoldBackgroundColor,
      navigationBar: const CupertinoNavigationBar(
        middle: Text('About ColAI'),
      ),
      child: SafeArea(
        child: Material(
          color: Colors.transparent,
          child: ListView(
            padding: const EdgeInsets.all(24),
            children: [
              const SizedBox(height: 20),
              Center(
                child: Container(
                  width: 100,
                  height: 100,
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(24),
                    boxShadow: [
                      BoxShadow(
                        color: Colors.black.withValues(alpha: 0.1),
                        blurRadius: 20,
                        spreadRadius: 5,
                      )
                    ],
                    image: const DecorationImage(
                      image: AssetImage('assets/images/app_icon.png'),
                      fit: BoxFit.cover,
                    ),
                  ),
                ),
              ),
              const SizedBox(height: 24),
              const Center(
                child: Text(
                  'ColAI',
                  style: TextStyle(
                    fontSize: 28,
                    fontWeight: FontWeight.w700,
                    decoration: TextDecoration.none,
                  ),
                ),
              ),
              Center(
                child: Text(
                  'Version 0.1.1',
                  style: TextStyle(
                    fontSize: 16,
                    color: CupertinoColors.systemGrey.resolveFrom(context),
                    decoration: TextDecoration.none,
                    fontWeight: FontWeight.normal,
                  ),
                ),
              ),
              const SizedBox(height: 48),
              _buildSectionHeader(context, 'DEVELOPER'),
              _buildInfoRow(context, 'Developed by', 'Ujwal'),
              const SizedBox(height: 32),
              _buildSectionHeader(context, 'LEGAL'),
              _buildInfoRow(context, 'License', 'Apache 2.0'),
              Padding(
                padding: const EdgeInsets.only(top: 8, bottom: 24),
                child: Text(
                  'This application is licensed under the Apache License 2.0.',
                  style: TextStyle(
                    fontSize: 13,
                    color: CupertinoColors.systemGrey.resolveFrom(context),
                    decoration: TextDecoration.none,
                    fontWeight: FontWeight.normal,
                  ),
                ),
              ),
              _buildSectionHeader(context, 'OPEN SOURCE'),
              _buildLinkRow(context, 'View Source Code',
                  'https://github.com/Ujwal223/ColAI'),
              const SizedBox(height: 12),
              Center(
                child: Text(
                  'Built with Flutter, Bloc, and love.',
                  style: TextStyle(
                    fontSize: 13,
                    color: CupertinoColors.systemGrey.resolveFrom(context),
                    decoration: TextDecoration.none,
                    fontWeight: FontWeight.normal,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildSectionHeader(BuildContext context, String title) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Text(
        title,
        style: TextStyle(
          fontSize: 13,
          fontWeight: FontWeight.w600,
          color: CupertinoColors.systemGrey.resolveFrom(context),
          decoration: TextDecoration.none,
        ),
      ),
    );
  }

  Widget _buildInfoRow(BuildContext context, String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(
            label,
            style: TextStyle(
              fontSize: 16,
              color: CupertinoColors.label.resolveFrom(context),
              decoration: TextDecoration.none,
              fontWeight: FontWeight.normal,
            ),
          ),
          Text(
            value,
            style: TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.w500,
              color: CupertinoColors.label.resolveFrom(context),
              decoration: TextDecoration.none,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildLinkRow(BuildContext context, String title, String url) {
    return GestureDetector(
      onTap: () async {
        final uri = Uri.parse(url);
        if (await canLaunchUrl(uri)) {
          await launchUrl(uri);
        }
      },
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 16),
        decoration: BoxDecoration(
          color: CupertinoColors.systemFill.resolveFrom(context),
          borderRadius: BorderRadius.circular(12),
        ),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(
              title,
              style: const TextStyle(
                fontSize: 16,
                color: CupertinoColors.activeBlue,
                decoration: TextDecoration.none,
                fontWeight: FontWeight.w500,
              ),
            ),
            const Icon(CupertinoIcons.arrow_up_right,
                size: 16, color: CupertinoColors.activeBlue),
          ],
        ),
      ),
    );
  }
}
