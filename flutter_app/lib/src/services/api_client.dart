import 'package:dio/dio.dart';
import 'token_storage.dart';

class ApiClient {
  final Dio dio;
  final TokenStorage tokenStorage;
  final String baseUrl;

  ApiClient(this.baseUrl, this.tokenStorage)
      : dio = Dio(
          BaseOptions(
            baseUrl: baseUrl,
            connectTimeout: const Duration(seconds: 5),
            receiveTimeout: const Duration(seconds: 5),
          ),
        );

  /// Initialize interceptors. Call this once after creating ApiClient.
  void init() {
    dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) async {
          final access = await tokenStorage.getAccessToken();
          if (access != null && access.isNotEmpty) {
            options.headers['Authorization'] = 'Bearer $access';
          }
          handler.next(options);
        },
        onError: (err, handler) async {
          final response = err.response;
          if (response != null && response.statusCode == 401) {
            final refresh = await tokenStorage.getRefreshToken();
            if (refresh == null || refresh.isEmpty) {
              // no refresh token -> forward error
              handler.next(err);
              return;
            }

            // Try to refresh token using a separate Dio instance to avoid interceptor loop
            try {
              final refreshDio = Dio(BaseOptions(baseUrl: baseUrl));
              final r = await refreshDio.post(
                '/api/auth/refresh',
                data: {'refreshToken': refresh},
              );
              if (r.statusCode == 200 && r.data != null) {
                final tokens = r.data['data'] ?? r.data;
                final newAccess = tokens['token'] as String?;
                final newRefresh = tokens['refreshToken'] as String? ??
                    tokens['refresh'] as String?;
                if (newAccess != null) {
                  await tokenStorage.setTokens(newAccess, newRefresh ?? '');

                  // retry original request with new access token
                  final opts = err.requestOptions;
                  opts.headers['Authorization'] = 'Bearer $newAccess';
                  final cloneReq = await dio.fetch(opts);
                  handler.resolve(cloneReq);
                  return;
                }
              }
            } catch (e) {
              // refresh failed -> clear tokens and forward error
              await tokenStorage.clear();
              handler.next(err);
              return;
            }
          }

          handler.next(err);
        },
      ),
    );
  }
}
