import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:ai_hub/state/ai_services_bloc.dart';
import 'package:ai_hub/services/logo_service.dart';
import 'package:ai_hub/data/default_ai_services.dart';
import 'package:ai_hub/models/ai_service.dart';
import 'package:ai_hub/screens/home_screen.dart';

class SetupScreen extends StatefulWidget {
  const SetupScreen({super.key});

  @override
  State<SetupScreen> createState() => _SetupScreenState();
}

class _SetupScreenState extends State<SetupScreen> {
  double _progress = 0;
  String _currentTask = 'Initializing...';

  @override
  void initState() {
    super.initState();
    _startSetup();
  }

  Future<void> _startSetup() async {
    final services = List<AIService>.from(DefaultAIServices.services);
    final total = services.length;

    if (!kIsWeb) {
      for (int i = 0; i < total; i++) {
        final service = services[i];
        setState(() {
          _currentTask = 'Downloading logo for ${service.name}...';
          _progress = i / total;
        });

        if (service.iconPath != null && service.iconPath!.startsWith('http')) {
          final localPath = await LogoService.downloadAndSaveLogo(
            service.iconPath!,
            '${service.id}_logo.png',
          );
          if (localPath != null) {
            services[i] = service.copyWith(iconPath: localPath);
          }
        }
      }
    } else {
      // On web, we skip downloading but still show the "setup" feeling for consistency
      for (int i = 0; i < total; i++) {
        await Future.delayed(const Duration(milliseconds: 150));
        setState(() {
          _currentTask = 'Configuring ${services[i].name}...';
          _progress = (i + 1) / total;
        });
      }
    }

    setState(() {
      _currentTask = 'Finishing up...';
      _progress = 1.0;
    });

    if (!mounted) return;
    context.read<AIServicesBloc>().add(AIServicesSetupComplete(services));

    // Navigate to Home
    Navigator.of(context).pushReplacement(
      CupertinoPageRoute(builder: (_) => const HomeScreen()),
    );
  }

  @override
  Widget build(BuildContext context) {
    return CupertinoPageScaffold(
      backgroundColor: const Color(0xFF000000),
      child: Material(
        color: Colors.transparent,
        child: Center(
          child: Padding(
            padding: const EdgeInsets.all(40),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                const CupertinoActivityIndicator(
                    radius: 15, color: CupertinoColors.white),
                const SizedBox(height: 32),
                const Text(
                  'Setting Things Up',
                  style: TextStyle(
                    fontSize: 24,
                    fontWeight: FontWeight.w800,
                    color: Colors.white,
                    letterSpacing: -1.0,
                  ),
                ),
                const SizedBox(height: 12),
                Text(
                  _currentTask,
                  textAlign: TextAlign.center,
                  style: const TextStyle(
                    fontSize: 15,
                    color: Color(0xFF98989E),
                    fontWeight: FontWeight.w500,
                  ),
                ),
                const SizedBox(height: 54),
                ClipRRect(
                  borderRadius: BorderRadius.circular(3),
                  child: SizedBox(
                    width: 240,
                    height: 4,
                    child: LinearProgressIndicator(
                      value: _progress,
                      backgroundColor: Colors.white.withValues(alpha: 0.1),
                      color: Colors.white,
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
