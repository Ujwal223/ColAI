import 'dart:io';
import 'package:http/http.dart' as http;
import 'package:path_provider/path_provider.dart';
import 'package:path/path.dart' as p;
import 'package:flutter/foundation.dart';
import 'package:ai_hub/services/logger_service.dart';

class LogoService {
  static Future<String?> downloadAndSaveLogo(
      String url, String fileName) async {
    // Web: No local file system storage. Return the URL directly for Image.network.
    if (kIsWeb) return url;

    try {
      final response = await http.get(Uri.parse(url));
      if (response.statusCode == 200) {
        final appDir = await getApplicationDocumentsDirectory();
        final logoDir = Directory('${appDir.path}/logos');
        if (!await logoDir.exists()) {
          await logoDir.create(recursive: true);
        }

        final filePath = p.join(logoDir.path, fileName);
        final file = File(filePath);
        if (await file.exists()) {
          return filePath;
        }
        await file.writeAsBytes(response.bodyBytes);
        return filePath;
      }
    } catch (e) {
      Logger.error('Error downloading logo', e);
    }
    return null;
  }

  static Future<String?> saveLocalImage(File? sourceFile, String fileName,
      {String? webPath}) async {
    // Web: Source file (dart:io File) doesn't exist. Use the webPath (blob URL).
    if (kIsWeb) return webPath;

    if (sourceFile == null) return null;

    try {
      final appDir = await getApplicationDocumentsDirectory();
      final logoDir = Directory('${appDir.path}/logos');
      if (!await logoDir.exists()) {
        await logoDir.create(recursive: true);
      }

      final filePath = p.join(logoDir.path, fileName);
      await sourceFile.copy(filePath);
      return filePath;
    } catch (e) {
      Logger.error('Error saving local image', e);
    }
    return null;
  }
}
