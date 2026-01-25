import 'package:flutter_test/flutter_test.dart';
import 'package:bloc_test/bloc_test.dart';
import 'package:mocktail/mocktail.dart';
import 'package:ai_hub/state/theme_cubit.dart';
import 'package:ai_hub/models/theme_mode.dart';
import 'package:ai_hub/services/storage_service.dart';

class MockStorageService extends Mock implements StorageService {}

void main() {
  late MockStorageService mockStorageService;

  setUp(() {
    mockStorageService = MockStorageService();
    // Default mock behavior for constructor call
    when(() => mockStorageService.loadThemeMode()).thenReturn(null);
    when(() => mockStorageService.loadContrastLevel()).thenReturn(null);
    when(() => mockStorageService.loadEnableSSO()).thenReturn(false);
  });

  group('ThemeCubit Tests', () {
    test('initial state matches storage or defaults', () {
      when(() => mockStorageService.loadThemeMode())
          .thenReturn(AppThemeMode.light.toString());
      final cubit = ThemeCubit(mockStorageService);
      expect(cubit.state.themeMode, AppThemeMode.light);
      cubit.close();
    });

    blocTest<ThemeCubit, ThemeState>(
      'emits correct state when theme is changed',
      build: () {
        when(() => mockStorageService.saveThemeMode(any()))
            .thenAnswer((_) async => true);
        return ThemeCubit(mockStorageService);
      },
      act: (cubit) => cubit.changeTheme(AppThemeMode.light),
      expect: () => [
        isA<ThemeState>()
            .having((s) => s.themeMode, 'themeMode', AppThemeMode.light),
      ],
      verify: (_) {
        verify(() =>
                mockStorageService.saveThemeMode(AppThemeMode.light.toString()))
            .called(1);
      },
    );

    blocTest<ThemeCubit, ThemeState>(
      'emits correct state when SSO is toggled',
      build: () {
        when(() => mockStorageService.saveEnableSSO(any()))
            .thenAnswer((_) async => true);
        return ThemeCubit(mockStorageService);
      },
      act: (cubit) => cubit.toggleEnableSSO(true),
      expect: () => [
        isA<ThemeState>().having((s) => s.enableSSO, 'enableSSO', true),
      ],
    );
  });
}
