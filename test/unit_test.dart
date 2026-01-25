import 'package:flutter_test/flutter_test.dart';
import 'package:ai_hub/utils/secure_ciphers.dart';
import 'package:ai_hub/models/session.dart';
import 'package:ai_hub/models/ai_service.dart';

void main() {
  group('SecureCiphers Tests', () {
    test('Encryption and Decryption consistency', () {
      const plaintext = 'SensitiveData123';
      final encrypted = SecureCiphers.encrypt(plaintext);
      expect(encrypted, isNot(equals(plaintext)));

      final decrypted = SecureCiphers.decrypt(encrypted);
      expect(decrypted, equals(plaintext));
    });

    test('Decryption with invalid data returns empty string', () {
      final decrypted = SecureCiphers.decrypt('invalid_base64_data');
      expect(decrypted, equals(''));
    });
  });

  group('Model Tests', () {
    test('Session equality and json conversion', () {
      final session = Session(
        id: 'test-id',
        serviceId: 'service-id',
        accountName: 'Test Session',
        isDefault: true,
        lastAccessed: DateTime(2025, 1, 1),
      );

      final json = session.toJson();
      final fromJson = Session.fromJson(json);

      expect(fromJson, equals(session));
      expect(fromJson.hashCode, equals(session.hashCode));
    });

    test('AIService equality and json conversion', () {
      final service = AIService(
        id: 'test-service',
        name: 'Test AI',
        url: 'https://test.ai',
        faviconUrl: 'https://test.ai/favicon.ico',
        createdAt: DateTime(2025, 1, 1),
        iconPath: 'assets/icon.png',
      );

      final json = service.toJson();
      final fromJson = AIService.fromJson(json);

      expect(fromJson, equals(service));
      expect(fromJson.hashCode, equals(service.hashCode));
    });
  });
}
