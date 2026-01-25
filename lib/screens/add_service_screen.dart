import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:ai_hub/models/ai_service.dart';
import 'package:ai_hub/state/ai_services_bloc.dart';
import 'package:uuid/uuid.dart';

import 'package:image_picker/image_picker.dart';
import 'package:ai_hub/services/logo_service.dart';
import 'package:path/path.dart' as p;
import 'package:flutter/foundation.dart';
import 'dart:io';

class AddServiceScreen extends StatefulWidget {
  const AddServiceScreen({super.key});

  @override
  State<AddServiceScreen> createState() => _AddServiceScreenState();
}

class _AddServiceScreenState extends State<AddServiceScreen> {
  final _nameController = TextEditingController();
  final _urlController = TextEditingController();
  final _logoUrlController = TextEditingController();
  final _uuid = const Uuid();
  final _picker = ImagePicker();

  int _logoSourceIndex = 0; // 0: Link, 1: Upload
  String? _localImagePath;
  bool _isDownloading = false;

  @override
  void dispose() {
    _nameController.dispose();
    _urlController.dispose();
    _logoUrlController.dispose();
    super.dispose();
  }

  Future<void> _pickImage() async {
    setState(() => _isDownloading = true);
    try {
      final XFile? image = await _picker.pickImage(
        source: ImageSource.gallery,
        imageQuality: 100,
      );

      if (image != null) {
        // Validate extension
        final ext = p.extension(image.path).toLowerCase();
        if (ext != '.png' && ext != '.jpg' && ext != '.jpeg') {
          if (!mounted) return;
          showCupertinoDialog(
            context: context,
            builder: (context) => CupertinoAlertDialog(
              title: const Text('Unsupported Format'),
              content: const Text('Please select a PNG or JPG image.'),
              actions: [
                CupertinoDialogAction(
                  child: const Text('OK'),
                  onPressed: () => Navigator.pop(context),
                ),
              ],
            ),
          );
          return;
        }

        if (kIsWeb) {
          // On web, picked path is a blob URL
          setState(() => _localImagePath = image.path);
        } else {
          final savedPath = await LogoService.saveLocalImage(
              File(image.path), 'custom_${_uuid.v4()}.png');
          setState(() => _localImagePath = savedPath);
        }
      }
    } finally {
      setState(() => _isDownloading = false);
    }
  }

  void _saveService() async {
    if (_nameController.text.trim().isEmpty ||
        _urlController.text.trim().isEmpty) {
      showCupertinoDialog(
        context: context,
        builder: (context) => CupertinoAlertDialog(
          title: const Text('Invalid Input'),
          content: const Text('Please enter both name and URL.'),
          actions: [
            CupertinoDialogAction(
              child: const Text('OK'),
              onPressed: () => Navigator.pop(context),
            ),
          ],
        ),
      );
      return;
    }

    String? finalIconPath = _localImagePath;

    if (_logoSourceIndex == 0 && _logoUrlController.text.isNotEmpty) {
      setState(() => _isDownloading = true);
      final downloadedPath = await LogoService.downloadAndSaveLogo(
        _logoUrlController.text.trim(),
        'custom_${_uuid.v4()}.png',
      );
      finalIconPath = downloadedPath;
      setState(() => _isDownloading = false);
    }

    final service = AIService(
      id: _uuid.v4(),
      name: _nameController.text.trim(),
      url: _urlController.text.trim(),
      faviconUrl: '',
      iconPath: finalIconPath,
      createdAt: DateTime.now(),
    );

    if (!mounted) return;
    context.read<AIServicesBloc>().add(AIServiceAdded(service));
    Navigator.of(context).pop();
  }

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return CupertinoPageScaffold(
      backgroundColor:
          CupertinoColors.systemGroupedBackground.resolveFrom(context),
      navigationBar: CupertinoNavigationBar(
        middle: const Text('New Service'),
        leading: CupertinoButton(
          padding: EdgeInsets.zero,
          child: const Text('Cancel'),
          onPressed: () => Navigator.pop(context),
        ),
        trailing: _isDownloading
            ? const CupertinoActivityIndicator()
            : CupertinoButton(
                padding: EdgeInsets.zero,
                onPressed: _saveService,
                child: const Text('Done',
                    style: TextStyle(fontWeight: FontWeight.bold)),
              ),
      ),
      child: Material(
        color: Colors.transparent, // Fixes underlines
        child: SafeArea(
          child: ListView(
            padding: const EdgeInsets.symmetric(vertical: 24),
            children: [
              // Icon Preview Section
              Center(
                child: Container(
                  width: 110,
                  height: 110,
                  decoration: BoxDecoration(
                    color: isDark
                        ? const Color(0xFF151517)
                        : const Color(0xFFE5E5EA),
                    borderRadius: BorderRadius.circular(40),
                    boxShadow: [
                      if (isDark)
                        BoxShadow(
                          color: Colors.black.withValues(alpha: 0.3),
                          blurRadius: 20,
                          offset: const Offset(0, 10),
                        ),
                    ],
                  ),
                  child: ClipRRect(
                    borderRadius: BorderRadius.circular(40),
                    child: Stack(
                      alignment: Alignment.center,
                      children: [
                        _buildPreviewImage(isDark),
                        if (_isDownloading)
                          Container(
                            color: Colors.black.withValues(alpha: 0.3),
                            child: const Center(
                              child: CupertinoActivityIndicator(radius: 12),
                            ),
                          ),
                      ],
                    ),
                  ),
                ),
              ),
              const SizedBox(height: 32),

              // Source Switcher
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 24),
                child: SizedBox(
                  width: double.infinity,
                  child: CupertinoSlidingSegmentedControl<int>(
                    groupValue: _logoSourceIndex,
                    children: const {
                      0: Padding(
                          padding: EdgeInsets.symmetric(horizontal: 20),
                          child: Text('Link')),
                      1: Padding(
                          padding: EdgeInsets.symmetric(horizontal: 20),
                          child: Text('Upload')),
                    },
                    onValueChanged: (val) =>
                        setState(() => _logoSourceIndex = val ?? 0),
                  ),
                ),
              ),
              const SizedBox(height: 12),

              CupertinoListSection.insetGrouped(
                header: const Text('ESSENTIALS'),
                backgroundColor: Colors.transparent,
                children: [
                  CupertinoListTile(
                    title: const Text('Name'),
                    additionalInfo: ConstrainedBox(
                      constraints: const BoxConstraints(maxWidth: 180),
                      child: CupertinoTextField(
                        controller: _nameController,
                        placeholder: 'Required',
                        textAlign: TextAlign.end,
                        decoration: null,
                        onChanged: (_) => setState(() {}),
                        style: TextStyle(
                          color: isDark ? Colors.white : Colors.black,
                        ),
                      ),
                    ),
                  ),
                  CupertinoListTile(
                    title: const Text('URL'),
                    additionalInfo: ConstrainedBox(
                      constraints: const BoxConstraints(maxWidth: 220),
                      child: CupertinoTextField(
                        controller: _urlController,
                        placeholder: 'https://...',
                        textAlign: TextAlign.end,
                        decoration: null,
                        keyboardType: TextInputType.url,
                        style: TextStyle(
                          color: isDark ? Colors.white : Colors.black,
                        ),
                      ),
                    ),
                  ),
                ],
              ),

              CupertinoListSection.insetGrouped(
                header: const Text('LOGO SOURCING'),
                backgroundColor: Colors.transparent,
                children: [
                  if (_logoSourceIndex == 0)
                    CupertinoListTile(
                      title: const Text('Image URL'),
                      additionalInfo: ConstrainedBox(
                        constraints: const BoxConstraints(maxWidth: 220),
                        child: CupertinoTextField(
                          controller: _logoUrlController,
                          placeholder: 'Paste link',
                          textAlign: TextAlign.end,
                          decoration: null,
                          onChanged: (_) => setState(() {}),
                          style: TextStyle(
                            color: isDark ? Colors.white : Colors.black,
                          ),
                        ),
                      ),
                    )
                  else
                    CupertinoListTile(
                      title: const Text('Photo Library'),
                      trailing: const Icon(CupertinoIcons.photo, size: 20),
                      onTap: _pickImage,
                      subtitle: const Text('Supported: PNG, JPG'),
                      additionalInfo: Text(
                        _localImagePath != null ? 'Selected' : 'Tap to pick',
                        style: TextStyle(
                            color: _localImagePath != null
                                ? CupertinoColors.activeBlue
                                : CupertinoColors.placeholderText),
                      ),
                    ),
                ],
              ),
              const SizedBox(height: 40),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildPreviewImage(bool isDark) {
    if (_logoSourceIndex == 1 && _localImagePath != null) {
      if (kIsWeb) {
        return Padding(
          padding: const EdgeInsets.all(24),
          child: Image.network(_localImagePath!, fit: BoxFit.contain),
        );
      }
      return Padding(
        padding: const EdgeInsets.all(24),
        child: Image.file(File(_localImagePath!), fit: BoxFit.contain),
      );
    }

    if (_logoSourceIndex == 0 && _logoUrlController.text.isNotEmpty) {
      return Padding(
        padding: const EdgeInsets.all(24),
        child: Image.network(
          _logoUrlController.text.trim(),
          fit: BoxFit.contain,
          errorBuilder: (_, __, ___) => _buildPlaceholder(isDark),
        ),
      );
    }

    return _buildPlaceholder(isDark);
  }

  Widget _buildPlaceholder(bool isDark) {
    return Center(
      child: Text(
        _nameController.text.isNotEmpty
            ? _nameController.text[0].toUpperCase()
            : '?',
        style: TextStyle(
          fontSize: 44,
          fontWeight: FontWeight.w300,
          color: isDark ? const Color(0xFF3A3A3C) : const Color(0xFFC7C7CC),
        ),
      ),
    );
  }
}
