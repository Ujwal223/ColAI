import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:ai_hub/screens/home_screen.dart';
import 'package:ai_hub/screens/setup_screen.dart';
import 'package:ai_hub/state/ai_services_bloc.dart';
import 'package:permission_handler/permission_handler.dart';

import 'package:ai_hub/state/theme_cubit.dart';

class OnboardingScreen extends StatefulWidget {
  const OnboardingScreen({super.key});

  @override
  State<OnboardingScreen> createState() => _OnboardingScreenState();
}

class _OnboardingScreenState extends State<OnboardingScreen> {
  final PageController _pageController = PageController();
  int _currentPage = 0;
  bool _notificationsGranted = false;
  // bool _storageGranted = false; // Android 13+ doesn't need explicit read/write usually for simple internal stuff, but for Gal (gallery) it might.
  // Gal usually handles its own permission request. We'll add Notification request here as it's common.

  bool _ssoEnabled = false;

  @override
  void initState() {
    super.initState();
    _checkPermissions();
    _loadSSO();
  }

  Future<void> _checkPermissions() async {
    final status = await Permission.notification.status;
    setState(() {
      _notificationsGranted = status.isGranted;
    });
  }

  Future<void> _loadSSO() async {
    final storage = context.read<ThemeCubit>().storageService;
    // Default to false for new installs
    final enabled = storage.loadEnableSSO() && storage.loadOnboardingComplete();
    if (mounted) setState(() => _ssoEnabled = enabled);
  }

  void _nextPage() {
    if (_currentPage < 2) {
      _pageController.nextPage(
        duration: const Duration(milliseconds: 300),
        curve: Curves.easeInOut,
      );
    } else {
      _finishOnboarding();
    }
  }

  Future<void> _finishOnboarding() async {
    // Save SSO setting one last time to be sure
    final storage = context.read<ThemeCubit>().storageService;
    await storage.saveOnboardingComplete(true);
    await storage.saveEnableSSO(_ssoEnabled);

    if (!mounted) return;

    final state = context.read<AIServicesBloc>().state;
    if (state is AIServicesLoaded && state.services.isNotEmpty) {
      Navigator.of(context).pushReplacement(
        CupertinoPageRoute(builder: (_) => const HomeScreen()),
      );
    } else {
      Navigator.of(context).pushReplacement(
        CupertinoPageRoute(builder: (_) => const SetupScreen()),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    // Dark theme for onboarding mainly
    return Scaffold(
      backgroundColor: Colors.black,
      body: SafeArea(
        child: Column(
          children: [
            Expanded(
              child: PageView(
                controller: _pageController,
                physics: const NeverScrollableScrollPhysics(), // Force buttons
                onPageChanged: (index) => setState(() => _currentPage = index),
                children: [
                  _buildWelcomePage(),
                  _buildPermissionsPage(),
                  _buildSSOPage(),
                ],
              ),
            ),
            _buildBottomBar(),
          ],
        ),
      ),
    );
  }

  Widget _buildWelcomePage() {
    return Padding(
      padding: const EdgeInsets.all(32),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Container(
            width: 100,
            height: 100,
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(24),
              image: const DecorationImage(
                image: AssetImage('assets/images/app_icon.png'),
                fit: BoxFit.cover,
              ),
              boxShadow: [
                BoxShadow(
                  color: Colors.blue.withValues(alpha: 0.3),
                  blurRadius: 30,
                  spreadRadius: 5,
                )
              ],
            ),
          ),
          const SizedBox(height: 40),
          const Text(
            'Welcome to ColAI',
            style: TextStyle(
              fontSize: 28,
              fontWeight: FontWeight.bold,
              color: Colors.white,
            ),
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 16),
          Text(
            'Your unified hub for all AI services.\nIsolated spaces, multiple accounts.',
            style: TextStyle(
              fontSize: 16,
              color: Colors.white.withValues(alpha: 0.7),
              height: 1.5,
            ),
            textAlign: TextAlign.center,
          ),
        ],
      ),
    );
  }

  Widget _buildPermissionsPage() {
    return Padding(
      padding: const EdgeInsets.all(32),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          const Icon(CupertinoIcons.checkmark_shield_fill,
              size: 80, color: Colors.blue),
          const SizedBox(height: 32),
          const Text(
            'Permissions',
            style: TextStyle(
              fontSize: 24,
              fontWeight: FontWeight.bold,
              color: Colors.white,
            ),
          ),
          const SizedBox(height: 16),
          Text(
            'To get the best experience, please allow notifications for AI alerts.',
            style: TextStyle(
              fontSize: 16,
              color: Colors.white.withValues(alpha: 0.7),
              height: 1.5,
            ),
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 40),
          _buildPermissionTile(
            'Notifications',
            CupertinoIcons.bell_fill,
            _notificationsGranted,
            () async {
              final status = await Permission.notification.request();
              setState(() => _notificationsGranted = status.isGranted);
            },
          ),
        ],
      ),
    );
  }

  Widget _buildPermissionTile(
      String title, IconData icon, bool granted, VoidCallback onRequest) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      decoration: BoxDecoration(
        color: Colors.white.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(16),
      ),
      child: Row(
        children: [
          Icon(icon, color: Colors.white),
          const SizedBox(width: 16),
          Expanded(
            child: Text(
              title,
              style: const TextStyle(
                  fontSize: 16,
                  color: Colors.white,
                  fontWeight: FontWeight.w500),
            ),
          ),
          CupertinoButton(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            color: granted
                ? CupertinoColors.activeGreen
                : CupertinoColors.activeBlue,
            borderRadius: BorderRadius.circular(20),
            onPressed: granted ? null : onRequest,
            child: Text(
              granted ? 'Allowed' : 'Allow',
              style: const TextStyle(color: Colors.white, fontSize: 14),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildSSOPage() {
    return Padding(
      padding: const EdgeInsets.all(32),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          const Icon(CupertinoIcons.globe, size: 80, color: Colors.orange),
          const SizedBox(height: 32),
          const Text(
            'Important Setup',
            style: TextStyle(
              fontSize: 24,
              fontWeight: FontWeight.bold,
              color: Colors.white,
            ),
          ),
          const SizedBox(height: 20),
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: Colors.orange.withValues(alpha: 0.1),
              borderRadius: BorderRadius.circular(12),
              border: Border.all(color: Colors.orange.withValues(alpha: 0.3)),
            ),
            child: Column(
              children: [
                const Row(
                  children: [
                    Icon(CupertinoIcons.exclamationmark_triangle_fill,
                        color: Colors.orange, size: 20),
                    SizedBox(width: 8),
                    Text('Read Carefully',
                        style: TextStyle(
                            color: Colors.orange, fontWeight: FontWeight.bold)),
                  ],
                ),
                const SizedBox(height: 12),
                Text(
                  'Without "Enable SSO Login", Gemini is un-loginable, and ChatGPT can only be logged in via Phone Number (No Social Logins).',
                  style: TextStyle(
                    fontSize: 14,
                    color: Colors.white.withValues(alpha: 0.9),
                    height: 1.4,
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 32),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
            decoration: BoxDecoration(
              color: Colors.white.withValues(alpha: 0.1),
              borderRadius: BorderRadius.circular(16),
            ),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                const Text(
                  'Enable SSO Login',
                  style: TextStyle(
                      fontSize: 16,
                      color: Colors.white,
                      fontWeight: FontWeight.w600),
                ),
                CupertinoSwitch(
                  value: _ssoEnabled,
                  onChanged: (val) {
                    setState(() => _ssoEnabled = val);
                  },
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildBottomBar() {
    return Padding(
      padding: const EdgeInsets.all(24),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          // Indicators
          Row(
            children: List.generate(3, (index) {
              return AnimatedContainer(
                duration: const Duration(milliseconds: 300),
                margin: const EdgeInsets.only(right: 8),
                width: _currentPage == index ? 24 : 8,
                height: 8,
                decoration: BoxDecoration(
                  color: _currentPage == index
                      ? Colors.blue
                      : Colors.white.withValues(alpha: 0.3),
                  borderRadius: BorderRadius.circular(4),
                ),
              );
            }),
          ),

          CupertinoButton(
            color: Colors.white,
            borderRadius: BorderRadius.circular(30),
            padding: const EdgeInsets.symmetric(horizontal: 32, vertical: 16),
            onPressed: _nextPage,
            child: Text(
              _currentPage == 2 ? 'Get Started' : 'Next',
              style: const TextStyle(
                color: Colors.black,
                fontWeight: FontWeight.bold,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
