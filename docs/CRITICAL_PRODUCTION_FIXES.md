# Critical production fixes

## Verified end-to-end contracts

| Flow | Flutter call | Backend endpoint | Persistence/result |
|---|---|---|---|
| Customer registration | `AuthController.register` → `GlowBackendService.register` | `POST /api/auth/register` | BCrypt customer row; CUSTOMER JWT/refresh token; session stored securely |
| Customer login | login page → auth provider → backend service | `POST /api/auth/login` with `role=CUSTOMER` | Phone/email lookup, BCrypt validation, CUSTOMER claims |
| Guest booking | appointment page → providers → backend service | `POST /api/appointments`, no `customerId` | Guest identity, service/option, employee, date/time and option price persist; notification shares the appointment transaction |
| Registered booking | same booking flow plus bearer token/customer ID | `POST /api/appointments` | Customer ID must match JWT (or staff role); package session is optional |
| Package purchase | package detail confirmation | `POST /api/customers/{customerId}/packages/{packageId}` | Customer package only; never creates an appointment; duplicates return 409 |
| Notifications | customer dashboard/providers | `GET /api/notifications/customer/{id}`, `PATCH /api/notifications/{id}/read` | Customer authorization, DTO list/update |
| Employee login/dashboard | role-aware login → employee route | `POST /api/auth/login`; `GET /api/appointments/employee/{id}` | EMPLOYEE JWT and schedule DTOs |
| Admin login/dashboard | role-aware login → admin route | `POST /api/auth/login`; `/api/admin/**` | ADMIN comes from persisted employee role, not a client-selected claim |

All Flutter requests use the production base URL by default for web/release, JSON content type, and `Authorization: Bearer <token>` when a token is present. A 401 triggers one refresh attempt without logging credentials.

## Root causes corrected

- Guest appointment notification used `REQUIRES_NEW` before the appointment transaction committed. The notification foreign key failed and the supposedly caught exception marked the transaction rollback-only, producing the observed 500.
- Business conflicts were flattened to 400. Slot and duplicate-purchase conflicts now return 409; missing entities remain 404 and validation remains 400.
- Registered booking accepted arbitrary `customerId` values on the public guest endpoint. A supplied customer ID now requires matching customer authorization or a staff role.
- Concurrent bookings and package purchases had check-then-write races. Pessimistic employee/customer locks serialize the relevant decision.
- Startup catalog seeding rewrote known hardcoded passwords. Catalog staff receive no usable seeded credential, and role is persisted explicitly.
- Flutter replaced production API failures with fake services, staff and slots. Demo fallback is opt-in only with `--dart-define=ENABLE_DEMO_DATA=true`.
- Package images did not inherit service imagery when `packageImage` was absent. Flutter now applies the corresponding service image.
- Shared bottom navigation assumed every host had a GoRouter page route, breaking embedded/tested dashboard flows.

## Railway/Vercel

- Port is `${PORT:8080}`.
- Production datasource accepts Railway `MYSQL*` variables or explicit `DB_*` aliases.
- Hikari defaults are bounded to 5 connections and 1 idle connection for small Railway instances.
- `/health` and `/actuator/health` are public.
- CORS must include the exact Vercel origin (wildcard is supported by the configured origin-pattern API).
- Demo users default to disabled.

Before deployment set a long random `JWT_SECRET`, production MySQL variables and `CORS_ALLOWED_ORIGINS=https://glowbook-web-navy.vercel.app`. Keep `ENABLE_DEMO_USERS=false` unless role testing is actively required.
