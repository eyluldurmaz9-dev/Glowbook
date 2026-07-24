import '../services/api_client.dart';

class AuthService {
  final ApiClient api;
  AuthService(this.api);

  Future<Map<String, dynamic>> login(String username, String password) async {
    final resp = await api.dio.post(
      '/api/auth/login',
      data: {'username': username, 'password': password},
    );
    return resp.data as Map<String, dynamic>;
  }

  Future<Map<String, dynamic>> register(Map<String, dynamic> payload) async {
    final resp = await api.dio.post('/api/auth/register', data: payload);
    return resp.data as Map<String, dynamic>;
  }

  Future<Map<String, dynamic>> refresh(String refreshToken) async {
    final resp = await api.dio.post(
      '/api/auth/refresh',
      data: {'refreshToken': refreshToken},
    );
    return resp.data as Map<String, dynamic>;
  }
}
