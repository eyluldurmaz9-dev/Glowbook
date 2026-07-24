import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../services/auth_service.dart';
import '../services/token_storage.dart';

class AuthState {
  final bool isLoading;
  final String? accessToken;
  final String? refreshToken;
  final bool isAuthenticated;

  AuthState({
    this.isLoading = false,
    this.accessToken,
    this.refreshToken,
  }) : isAuthenticated = accessToken != null && accessToken.isNotEmpty;

  AuthState copyWith(
      {bool? isLoading, String? accessToken, String? refreshToken}) {
    return AuthState(
      isLoading: isLoading ?? this.isLoading,
      accessToken: accessToken ?? this.accessToken,
      refreshToken: refreshToken ?? this.refreshToken,
    );
  }
}

class AuthStateNotifier extends StateNotifier<AuthState> {
  final AuthService _service;
  final TokenStorage _storage;
  final ValueNotifier<bool> refreshNotifier = ValueNotifier(false);

  AuthStateNotifier(this._service, this._storage) : super(AuthState()) {
    _loadFromStorage();
  }

  Future<void> _loadFromStorage() async {
    final access = await _storage.getAccessToken();
    final refresh = await _storage.getRefreshToken();
    if (access != null && access.isNotEmpty) {
      state = state.copyWith(accessToken: access, refreshToken: refresh);
      refreshNotifier.value = !refreshNotifier.value;
    }
  }

  Future<bool> login(String username, String password) async {
    state = state.copyWith(isLoading: true);
    try {
      final data = await _service.login(username, password);
      final d = data['data'] ?? data;
      final token = d['token'] as String?;
      final refresh = d['refreshToken'] as String? ?? d['refresh'] as String?;
      if (token != null) {
        await _storage.setTokens(token, refresh ?? '');
        state = state.copyWith(
            isLoading: false, accessToken: token, refreshToken: refresh);
        refreshNotifier.value = !refreshNotifier.value;
        return true;
      }
    } catch (e) {
      // ignore
    }
    state = state.copyWith(isLoading: false);
    return false;
  }

  Future<void> logout() async {
    await _storage.clear();
    state = AuthState();
    refreshNotifier.value = !refreshNotifier.value;
  }

  Future<bool> refresh() async {
    final refreshToken = state.refreshToken ?? await _storage.getRefreshToken();
    if (refreshToken == null || refreshToken.isEmpty) return false;
    try {
      final data = await _service.refresh(refreshToken);
      final d = data['data'] ?? data;
      final token = d['token'] as String?;
      final newRefresh =
          d['refreshToken'] as String? ?? d['refresh'] as String?;
      if (token != null) {
        await _storage.setTokens(token, newRefresh ?? '');
        state = state.copyWith(accessToken: token, refreshToken: newRefresh);
        refreshNotifier.value = !refreshNotifier.value;
        return true;
      }
    } catch (e) {
      // ignore
    }
    return false;
  }
}
