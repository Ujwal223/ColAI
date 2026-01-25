// Session state management for ColAI.
//
// Handles all session-related operations including creating, updating, and deleting sessions.
// Each AI service can have multiple isolated sessions - think of them as separate browser profiles
// that never share cookies or data. Perfect for managing multiple accounts!

import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:equatable/equatable.dart';
import 'package:ai_hub/models/session.dart';
import 'package:ai_hub/services/storage_service.dart';

// Events - what actions can be performed on sessions

/// Base class for all session-related events.
abstract class SessionsEvent extends Equatable {
  const SessionsEvent();

  @override
  List<Object?> get props => [];
}

/// Loads sessions from storage, optionally filtered by service ID.
class LoadSessions extends SessionsEvent {
  final String? serviceId;

  const LoadSessions({this.serviceId});

  @override
  List<Object?> get props => [serviceId];
}

/// Adds a new session to the list.
class SessionAdded extends SessionsEvent {
  final Session session;

  const SessionAdded(this.session);

  @override
  List<Object> get props => [session];
}

/// Updates an existing session with new data.
class SessionUpdated extends SessionsEvent {
  final Session session;

  const SessionUpdated(this.session);

  @override
  List<Object> get props => [session];
}

/// Deletes a session by ID. This will clear all its cookies and data.
class SessionDeleted extends SessionsEvent {
  final String sessionId;

  const SessionDeleted(this.sessionId);

  @override
  List<Object> get props => [sessionId];
}

/// Sets a session as the default for its service.
/// Only one session per service can be default.
class DefaultSessionSet extends SessionsEvent {
  final String serviceId;
  final String sessionId;

  const DefaultSessionSet(this.serviceId, this.sessionId);

  @override
  List<Object> get props => [serviceId, sessionId];
}

/// Updates the last accessed time for a session.
/// Helps track which sessions are actively used.
class SessionAccessed extends SessionsEvent {
  final String sessionId;

  const SessionAccessed(this.sessionId);

  @override
  List<Object> get props => [sessionId];
}

/// Clears data for a specific session (cookies, cache, etc).
class SessionDataCleared extends SessionsEvent {
  final String sessionId;

  const SessionDataCleared(this.sessionId);

  @override
  List<Object> get props => [sessionId];
}

/// Clears data for all sessions without deleting them.
class AllSessionsDataCleared extends SessionsEvent {
  const AllSessionsDataCleared();
}

/// Deletes all sessions completely.
class SessionsCleared extends SessionsEvent {
  const SessionsCleared();
}

// States - possible states of the sessions data

/// Base class for all session states.
abstract class SessionsState extends Equatable {
  const SessionsState();

  @override
  List<Object> get props => [];
}

/// Initial state before any sessions are loaded.
class SessionsInitial extends SessionsState {
  const SessionsInitial();
}

/// Sessions are being loaded from storage.
class SessionsLoading extends SessionsState {
  const SessionsLoading();
}

/// Sessions have been successfully loaded.
class SessionsLoaded extends SessionsState {
  final List<Session> sessions;

  const SessionsLoaded(this.sessions);

  @override
  List<Object> get props => [sessions];
}

/// An error occurred while managing sessions.
class SessionsError extends SessionsState {
  final String message;

  const SessionsError(this.message);

  @override
  List<Object> get props => [message];
}

// BLoC - Business Logic Component for managing session state

/// Manages all session-related state and operations.
///
/// This bloc handles the complete lifecycle of sessions:
/// - Creating new isolated sessions for different accounts
/// - Updating session metadata (last accessed, preferences)
/// - Deleting sessions and their associated data
/// - Managing default sessions per service
class SessionsBloc extends Bloc<SessionsEvent, SessionsState> {
  final StorageService _storageService;
  List<Session> _sessions = [];

  SessionsBloc(this._storageService) : super(const SessionsInitial()) {
    on<LoadSessions>(_onLoad);
    on<SessionAdded>(_onAdd);
    on<SessionUpdated>(_onUpdate);
    on<SessionDeleted>(_onDelete);
    on<DefaultSessionSet>(_onSetDefault);
    on<SessionAccessed>(_onAccess);
    on<SessionDataCleared>(_onClearData);
    on<AllSessionsDataCleared>(_onClearAllData);
    on<SessionsCleared>(_onClearAll);
  }

  Future<void> _onClearAll(
      SessionsCleared event, Emitter<SessionsState> emit) async {
    try {
      _sessions.clear();
      await _storageService.saveSessions(_sessions);
      emit(const SessionsLoaded([]));
    } catch (e) {
      emit(SessionsError('Failed to clear all sessions: $e'));
    }
  }

  Future<void> _onLoad(LoadSessions event, Emitter<SessionsState> emit) async {
    emit(const SessionsLoading());

    try {
      _sessions = _storageService.loadSessions();

      // Filter by service if specified
      if (event.serviceId != null) {
        final filteredSessions =
            _sessions.where((s) => s.serviceId == event.serviceId).toList();
        emit(SessionsLoaded(filteredSessions));
      } else {
        emit(SessionsLoaded(_sessions));
      }
    } catch (e) {
      emit(SessionsError('Failed to load sessions: $e'));
    }
  }

  Future<void> _onAdd(SessionAdded event, Emitter<SessionsState> emit) async {
    try {
      _sessions.add(event.session);
      await _storageService.saveSessions(_sessions);
      emit(SessionsLoaded(List.from(_sessions)));
    } catch (e) {
      emit(SessionsError('Failed to add session: $e'));
    }
  }

  Future<void> _onUpdate(
      SessionUpdated event, Emitter<SessionsState> emit) async {
    try {
      final index = _sessions.indexWhere((s) => s.id == event.session.id);
      if (index != -1) {
        _sessions[index] = event.session;
        await _storageService.saveSessions(_sessions);
        emit(SessionsLoaded(List.from(_sessions)));
      }
    } catch (e) {
      emit(SessionsError('Failed to update session: $e'));
    }
  }

  Future<void> _onDelete(
      SessionDeleted event, Emitter<SessionsState> emit) async {
    try {
      _sessions.removeWhere((s) => s.id == event.sessionId);
      await _storageService.saveSessions(_sessions);
      emit(SessionsLoaded(List.from(_sessions)));
    } catch (e) {
      emit(SessionsError('Failed to delete session: $e'));
    }
  }

  Future<void> _onSetDefault(
      DefaultSessionSet event, Emitter<SessionsState> emit) async {
    try {
      // Remove default from all sessions of this service
      for (var i = 0; i < _sessions.length; i++) {
        if (_sessions[i].serviceId == event.serviceId &&
            _sessions[i].isDefault) {
          _sessions[i] = _sessions[i].copyWith(isDefault: false);
        }
      }

      // Set new default
      final index = _sessions.indexWhere((s) => s.id == event.sessionId);
      if (index != -1) {
        _sessions[index] = _sessions[index].copyWith(isDefault: true);
      }

      await _storageService.saveSessions(_sessions);
      emit(SessionsLoaded(List.from(_sessions)));
    } catch (e) {
      emit(SessionsError('Failed to set default session: $e'));
    }
  }

  Future<void> _onAccess(
      SessionAccessed event, Emitter<SessionsState> emit) async {
    try {
      final index = _sessions.indexWhere((s) => s.id == event.sessionId);
      if (index != -1) {
        _sessions[index] =
            _sessions[index].copyWith(lastAccessed: DateTime.now());
        await _storageService.saveSessions(_sessions);
        emit(SessionsLoaded(List.from(_sessions)));
      }
    } catch (e) {
      emit(SessionsError('Failed to update session access: $e'));
    }
  }

  Future<void> _onClearData(
      SessionDataCleared event, Emitter<SessionsState> emit) async {
    // Note: Cookie clearing happens in the UI layer where CookieManager is accessible.
    // Here we just refresh the state to ensure UI reflects any changes.
    emit(SessionsLoaded(List.from(_sessions)));
  }

  Future<void> _onClearAllData(
      AllSessionsDataCleared event, Emitter<SessionsState> emit) async {
    emit(SessionsLoaded(List.from(_sessions)));
  }

  List<Session> getSessionsForService(String serviceId) {
    return _sessions.where((s) => s.serviceId == serviceId).toList();
  }

  Session? getDefaultSession(String serviceId) {
    final serviceSessions = getSessionsForService(serviceId);
    return serviceSessions.firstWhere(
      (s) => s.isDefault,
      orElse: () => serviceSessions.isNotEmpty
          ? serviceSessions.first
          : throw Exception('No sessions'),
    );
  }
}
