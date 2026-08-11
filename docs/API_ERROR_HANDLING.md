# API error handling

Backend responses keep the `ApiResponse` envelope and use 400 validation/business rule, 403 authorization, 404 missing resource, 409 conflict and 500 unexpected failure. Unexpected exceptions are logged server-side and return no stack trace or secret.

Flutter classifies no internet, DNS, refused connection, connect timeout, receive timeout, TLS/certificate, browser CORS-like failures, HTTP 400/401/403/404/409/422/500, invalid JSON, DTO shape mismatch and missing configuration. Messages are Turkish and action-oriented.

JWTs, passwords, refresh tokens and database credentials must never be included in logs. Production fake-data fallback is disabled; developers may explicitly enable it with `--dart-define=ENABLE_DEMO_DATA=true`. Production API override uses `--dart-define=API_BASE_URL=https://...`.
