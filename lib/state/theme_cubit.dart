// Theme state management for ColAI.
//
// This cubit handles all the theme-related state and persistence.
// Because nobody wants their carefully chosen dark mode to reset every time
// they open the app. We save your preferences and restore them like magic!

import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:equatable/equatable.dart';
import 'package:ai_hub/models/theme_mode.dart';
import 'package:ai_hub/services/storage_service.dart';

// Events
abstract class ThemeEvent extends Equatable {
  const ThemeEvent();

  @override
  List<Object> get props => [];
}

class ThemeChanged extends ThemeEvent {
  final AppThemeMode themeMode;

  const ThemeChanged(this.themeMode);

  @override
  List<Object> get props => [themeMode];
}

class ThemeLoaded extends ThemeEvent {}

class ThemeState extends Equatable {
  final AppThemeMode themeMode;
  final ContrastLevel contrastLevel;
  final bool enableSSO;

  const ThemeState({
    required this.themeMode,
    required this.contrastLevel,
    this.enableSSO = false,
  });

  @override
  List<Object> get props => [themeMode, contrastLevel, enableSSO];

  ThemeState copyWith({
    AppThemeMode? themeMode,
    ContrastLevel? contrastLevel,
    bool? enableSSO,
  }) {
    return ThemeState(
      themeMode: themeMode ?? this.themeMode,
      contrastLevel: contrastLevel ?? this.contrastLevel,
      enableSSO: enableSSO ?? this.enableSSO,
    );
  }
}

// Cubit
class ThemeCubit extends Cubit<ThemeState> {
  final StorageService _storageService;
  StorageService get storageService => _storageService;

  ThemeCubit(this._storageService)
      : super(const ThemeState(
          themeMode: AppThemeMode.dark,
          contrastLevel: ContrastLevel.relaxed,
        )) {
    _loadSettings();
  }

  void _loadSettings() {
    final savedTheme = _storageService.loadThemeMode();
    final savedContrast = _storageService.loadContrastLevel();

    AppThemeMode themeMode = AppThemeMode.dark;
    if (savedTheme != null) {
      themeMode = AppThemeMode.values.firstWhere(
        (mode) => mode.toString() == savedTheme,
        orElse: () => AppThemeMode.dark,
      );
    }

    ContrastLevel contrastLevel = ContrastLevel.relaxed;
    if (savedContrast != null) {
      contrastLevel = ContrastLevel.values.firstWhere(
        (c) => c.toString() == savedContrast,
        orElse: () => ContrastLevel.relaxed,
      );
    }

    final enableSSO = _storageService.loadEnableSSO();

    emit(ThemeState(
      themeMode: themeMode,
      contrastLevel: contrastLevel,
      enableSSO: enableSSO,
    ));
  }

  Future<void> changeTheme(AppThemeMode themeMode) async {
    await _storageService.saveThemeMode(themeMode.toString());
    emit(state.copyWith(themeMode: themeMode));
  }

  Future<void> changeContrast(ContrastLevel contrastLevel) async {
    await _storageService.saveContrastLevel(contrastLevel.toString());
    emit(state.copyWith(contrastLevel: contrastLevel));
  }

  Future<void> toggleEnableSSO(bool value) async {
    await _storageService.saveEnableSSO(value);
    emit(state.copyWith(enableSSO: value));
  }

  void cycleTheme() {
    final nextMode = state.themeMode == AppThemeMode.light
        ? AppThemeMode.dark
        : AppThemeMode.light;
    changeTheme(nextMode);
  }
}
