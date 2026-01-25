import 'package:flutter_test/flutter_test.dart';
import 'package:bloc_test/bloc_test.dart';
import 'package:mocktail/mocktail.dart';
import 'package:ai_hub/state/ai_services_bloc.dart';
import 'package:ai_hub/models/ai_service.dart';
import 'package:ai_hub/services/storage_service.dart';

class MockStorageService extends Mock implements StorageService {}

void main() {
  late MockStorageService mockStorageService;
  late AIServicesBloc aiServicesBloc;

  final testService = AIService(
    id: 'chatgpt',
    name: 'ChatGPT',
    url: 'https://chat.openai.com',
    faviconUrl: 'https://chat.openai.com/favicon.ico',
    createdAt: DateTime(2025, 1, 1),
  );

  setUp(() {
    mockStorageService = MockStorageService();
    aiServicesBloc = AIServicesBloc(mockStorageService);
  });

  tearDown(() {
    aiServicesBloc.close();
  });

  group('AIServicesBloc Tests', () {
    test('initial state is AIServicesInitial', () {
      expect(aiServicesBloc.state, const AIServicesInitial());
    });

    blocTest<AIServicesBloc, AIServicesState>(
      'emits [AIServicesLoading, AIServicesLoaded] when LoadAIServices is successfully added',
      build: () {
        when(() => mockStorageService.loadAIServices())
            .thenReturn([testService]);
        return aiServicesBloc;
      },
      act: (bloc) => bloc.add(const LoadAIServices()),
      expect: () => [
        const AIServicesLoading(),
        AIServicesLoaded([testService]),
      ],
    );

    blocTest<AIServicesBloc, AIServicesState>(
      'emits default services if storage is empty',
      build: () {
        when(() => mockStorageService.loadAIServices()).thenReturn([]);
        when(() => mockStorageService.saveAIServices(any()))
            .thenAnswer((_) async => true);
        return aiServicesBloc;
      },
      act: (bloc) => bloc.add(const LoadAIServices()),
      expect: () => [
        const AIServicesLoading(),
        isA<AIServicesLoaded>()
            .having((s) => s.services.length, 'length', greaterThan(0)),
      ],
      verify: (_) {
        verify(() => mockStorageService.saveAIServices(any())).called(1);
      },
    );
  });
}
