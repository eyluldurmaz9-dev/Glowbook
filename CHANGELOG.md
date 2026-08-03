# Changelog

## Release Candidate - 2026-07-31

### Added

- JWT authentication and refresh token support.
- Customer, employee, admin, catalog, package, appointment, waitlist, schedule and notification API coverage.
- Production profile with environment-based database and JWT configuration.
- CORS allowlist configuration through `CORS_ALLOWED_ORIGINS`.
- Public health endpoints at `/actuator/health` and `/health`.
- GitHub Actions workflow for backend test and build.
- Railway/Render deployment documentation.

### Changed

- SMS fallback logging now masks phone numbers and does not log message content.
- Production port can be supplied through `PORT`.
- Secure headers configured through Spring Security.

### Known Limitations

- Production schema migration files exist, but Flyway/Liquibase runtime dependency is not wired into the build.
- Real SMS provider credentials and sender implementation must be configured outside Git.
- Aggregate reporting/statistics APIs are limited.
