# Backend Manual Validation Checklist

## Deployment

- [ ] Set production environment variables in Railway or Render.
- [ ] Confirm Java 25 runtime support.
- [ ] Run build command: `./mvnw verify`.
- [ ] Run start command with `$PORT`.
- [ ] Check `/actuator/health` or `/health`.

## Database

- [ ] Provision MySQL.
- [ ] Apply migration SQL or finalized migration runner.
- [ ] Verify `spring.jpa.hibernate.ddl-auto=validate` passes.
- [ ] Verify indexes and constraints for appointments, waiting list and refresh tokens.

## API

- [ ] Register customer.
- [ ] Login customer, employee and admin.
- [ ] Refresh token.
- [ ] Access protected route with correct role.
- [ ] Confirm wrong role receives safe error.
- [ ] Create appointment.
- [ ] Request available slots.
- [ ] Create/cancel/convert waiting list record.
- [ ] Mark notification read.
- [ ] Verify profile/customer updates.

## Security

- [ ] Confirm production CORS allowlist contains only deployed Flutter Web origins.
- [ ] Confirm no response leaks password hashes or tokens except auth token responses.
- [ ] Confirm logs do not include JWT, refresh token, raw password, full phone number or SMS message body.
- [ ] Rotate any test credential accidentally used in production.

## Scheduler and SMS

- [ ] Configure SMS provider credentials outside Git.
- [ ] Verify SMS sender handles provider failure without crashing scheduler.
- [ ] Verify reminder scheduler is idempotent.
