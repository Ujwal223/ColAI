import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:ai_hub/services/storage_service.dart';
import 'package:ai_hub/models/ai_service.dart';

class MockSharedPreferences extends Mock implements SharedPreferences {}

void main() {
  late MockSharedPreferences mockPrefs;
  late StorageService storageService;

  setUp(() {
    mockPrefs = MockSharedPreferences();
    storageService = StorageService(mockPrefs);
  });

  group('StorageService Tests', () {
    test('loadAIServices returns empty list when no data is saved', () {
      when(() => mockPrefs.getString(any())).thenReturn(null);
      final services = storageService.loadAIServices();
      expect(services, isEmpty);
    });

    test('saveAIServices correctly encodes and saves services', () async {
      final service = AIService(
        id: '1',
        name: 'Test AI',
        url: 'https://test.ai',
        faviconUrl: 'https://test.ai/favicon.ico',
        createdAt: DateTime(2025, 1, 1),
      );

      when(() => mockPrefs.setString(any(), any()))
          .thenAnswer((_) async => true);

      final result = await storageService.saveAIServices([service]);

      expect(result, isTrue);
      verify(() => mockPrefs.setString('ai_services', any())).called(1);
    });
  });
}
