import 'package:flutter_test/flutter_test.dart';
import 'package:bloc_test/bloc_test.dart';
import 'package:mocktail/mocktail.dart';
import 'package:ai_hub/state/sessions_bloc.dart';
import 'package:ai_hub/models/session.dart';
import 'package:ai_hub/services/storage_service.dart';

class MockStorageService extends Mock implements StorageService {}

void main() {
  late MockStorageService mockStorageService;
  late SessionsBloc sessionsBloc;

  final testSession = Session(
    id: '1',
    serviceId: 'chatgpt',
    accountName: 'Test Session',
    isDefault: true,
    lastAccessed: DateTime(2025, 1, 1),
  );

  setUp(() {
    mockStorageService = MockStorageService();
    sessionsBloc = SessionsBloc(mockStorageService);
  });

  tearDown(() {
    sessionsBloc.close();
  });

  group('SessionsBloc Tests', () {
    test('initial state is SessionsInitial', () {
      expect(sessionsBloc.state, const SessionsInitial());
    });

    blocTest<SessionsBloc, SessionsState>(
      'emits [SessionsLoading, SessionsLoaded] when LoadSessions is successfully added',
      build: () {
        when(() => mockStorageService.loadSessions()).thenReturn([testSession]);
        return sessionsBloc;
      },
      act: (bloc) => bloc.add(const LoadSessions()),
      expect: () => [
        const SessionsLoading(),
        SessionsLoaded([testSession]),
      ],
    );

    blocTest<SessionsBloc, SessionsState>(
      'emits [SessionsLoaded] when SessionAdded is successfully added',
      build: () {
        when(() => mockStorageService.saveSessions(any()))
            .thenAnswer((_) async => true);
        return sessionsBloc;
      },
      act: (bloc) => bloc.add(SessionAdded(testSession)),
      expect: () => [
        SessionsLoaded([testSession]),
      ],
      verify: (_) {
        verify(() => mockStorageService.saveSessions([testSession])).called(1);
      },
    );

    blocTest<SessionsBloc, SessionsState>(
      'emits SessionsError when LoadSessions fails',
      build: () {
        when(() => mockStorageService.loadSessions())
            .thenThrow(Exception('Failed to load'));
        return sessionsBloc;
      },
      act: (bloc) => bloc.add(const LoadSessions()),
      expect: () => [
        const SessionsLoading(),
        isA<SessionsError>(),
      ],
    );
  });
}
