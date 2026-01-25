// AI Services state management for ColAI.
//
// Manages the list of available AI services (ChatGPT, Claude, Gemini, etc.).
// Handles adding custom services, refreshing favicons, and persisting the service list.
// Default services are pre-configured but users can add their own!

import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:equatable/equatable.dart';
import 'package:ai_hub/models/ai_service.dart';
import 'package:ai_hub/services/storage_service.dart';
import 'package:ai_hub/services/favicon_service.dart';
import 'package:ai_hub/data/default_ai_services.dart';

// Events - actions that can be performed on AI services

/// Base class for all AI service events.
abstract class AIServicesEvent extends Equatable {
  const AIServicesEvent();

  @override
  List<Object?> get props => [];
}

/// Loads AI services from storage (or defaults if first run).
class LoadAIServices extends AIServicesEvent {
  const LoadAIServices();
}

/// Adds a new custom AI service to the list.
class AIServiceAdded extends AIServicesEvent {
  final AIService service;

  const AIServiceAdded(this.service);

  @override
  List<Object> get props => [service];
}

/// Updates an existing service (e.g., favicon refresh, notification settings).
class AIServiceUpdated extends AIServicesEvent {
  final AIService service;

  const AIServiceUpdated(this.service);

  @override
  List<Object> get props => [service];
}

/// Deletes a custom AI service. Default services cannot be deleted.
class AIServiceDeleted extends AIServicesEvent {
  final String serviceId;

  const AIServiceDeleted(this.serviceId);

  @override
  List<Object> get props => [serviceId];
}

/// Refreshes the favicon for a service by fetching it again.
class AIServiceFaviconRefreshed extends AIServicesEvent {
  final String serviceId;

  const AIServiceFaviconRefreshed(this.serviceId);

  @override
  List<Object> get props => [serviceId];
}

/// Called when onboarding/setup is complete with selected services.
class AIServicesSetupComplete extends AIServicesEvent {
  final List<AIService> services;

  const AIServicesSetupComplete(this.services);

  @override
  List<Object> get props => [services];
}

// States - possible states of the AI services data

/// Base class for all AI service states.
abstract class AIServicesState extends Equatable {
  const AIServicesState();

  @override
  List<Object> get props => [];
}

/// Initial state before services are loaded.
class AIServicesInitial extends AIServicesState {
  const AIServicesInitial();
}

/// Services are being loaded from storage.
class AIServicesLoading extends AIServicesState {
  const AIServicesLoading();
}

/// Services have been successfully loaded.
class AIServicesLoaded extends AIServicesState {
  final List<AIService> services;

  const AIServicesLoaded(this.services);

  @override
  List<Object> get props => [services];
}

/// An error occurred while managing services.
class AIServicesError extends AIServicesState {
  final String message;

  const AIServicesError(this.message);

  @override
  List<Object> get props => [message];
}

// BLoC - Business Logic Component for managing AI services

/// Manages the list of available AI services.
///
/// This bloc handles:
/// - Loading services from storage (with defaults on first run)
/// - Adding custom services
/// - Updating service metadata (favicons, notifications)
/// - Deleting custom services (default ones are protected)
/// - Refreshing favicons on demand
class AIServicesBloc extends Bloc<AIServicesEvent, AIServicesState> {
  final StorageService _storageService;
  List<AIService> _services = [];

  AIServicesBloc(this._storageService) : super(const AIServicesInitial()) {
    on<LoadAIServices>(_onLoad);
    on<AIServiceAdded>(_onAdd);
    on<AIServiceUpdated>(_onUpdate);
    on<AIServiceDeleted>(_onDelete);
    on<AIServiceFaviconRefreshed>(_onRefreshFavicon);
    on<AIServicesSetupComplete>(_onSetupComplete);
  }

  Future<void> _onLoad(
      LoadAIServices event, Emitter<AIServicesState> emit) async {
    emit(const AIServicesLoading());

    try {
      _services = _storageService.loadAIServices();

      // Failsafe: On first run or if storage is corrupted, use default services
      if (_services.isEmpty) {
        _services = List<AIService>.from(DefaultAIServices.services);
        await _storageService.saveAIServices(_services);
      }

      emit(AIServicesLoaded(_services));
    } catch (e) {
      emit(AIServicesError('Failed to load AI services: $e'));
    }
  }

  Future<void> _onSetupComplete(
      AIServicesSetupComplete event, Emitter<AIServicesState> emit) async {
    try {
      _services = event.services;
      await _storageService.saveAIServices(_services);
      emit(AIServicesLoaded(List.from(_services)));
    } catch (e) {
      emit(AIServicesError('Failed to save setup: $e'));
    }
  }

  Future<void> _onAdd(
      AIServiceAdded event, Emitter<AIServicesState> emit) async {
    try {
      _services.add(event.service);
      await _storageService.saveAIServices(_services);
      emit(AIServicesLoaded(List.from(_services)));
    } catch (e) {
      emit(AIServicesError('Failed to add service: $e'));
    }
  }

  Future<void> _onUpdate(
      AIServiceUpdated event, Emitter<AIServicesState> emit) async {
    try {
      final index = _services.indexWhere((s) => s.id == event.service.id);
      if (index != -1) {
        _services[index] = event.service;
        await _storageService.saveAIServices(_services);
        emit(AIServicesLoaded(List.from(_services)));
      }
    } catch (e) {
      emit(AIServicesError('Failed to update service: $e'));
    }
  }

  Future<void> _onDelete(
      AIServiceDeleted event, Emitter<AIServicesState> emit) async {
    try {
      _services.removeWhere((s) => s.id == event.serviceId);
      await _storageService.saveAIServices(_services);
      emit(AIServicesLoaded(List.from(_services)));
    } catch (e) {
      emit(AIServicesError('Failed to delete service: $e'));
    }
  }

  Future<void> _onRefreshFavicon(
      AIServiceFaviconRefreshed event, Emitter<AIServicesState> emit) async {
    try {
      final index = _services.indexWhere((s) => s.id == event.serviceId);
      if (index != -1) {
        final service = _services[index];
        final newFaviconUrl = await FaviconService.fetchFaviconUrl(service.url);
        _services[index] = service.copyWith(faviconUrl: newFaviconUrl);
        await _storageService.saveAIServices(_services);
        emit(AIServicesLoaded(List.from(_services)));
      }
    } catch (e) {
      emit(AIServicesError('Failed to refresh favicon: $e'));
    }
  }
}
