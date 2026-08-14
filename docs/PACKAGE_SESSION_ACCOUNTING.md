# Package Session Accounting

How GlowBook decides how many sessions of a purchased package are used, reserved
and still bookable.

## The rule

Session counts are **derived**, never incrementally mutated. For a given
`CustomerPackage` we look at every `Appointment` whose `customer_package_id`
points at it and classify each one against the business clock:

| Bucket      | Condition                                                            |
|-------------|----------------------------------------------------------------------|
| `total`     | `service_packages.total_session` of the purchased package             |
| `used`      | not cancelled **and** `appointmentDateTime <= now`                    |
| `scheduled` | not cancelled **and** `appointmentDateTime > now`                     |
| `remaining` | `max(total - used - scheduled, 0)`                                    |

`now` is always `LocalDateTime.now(businessClock)` — see
[APPOINTMENT_HISTORY.md](APPOINTMENT_HISTORY.md) for the timezone contract.

Because every number is recomputed from the appointment rows, a session can never
be subtracted twice, restored twice, or left consumed by a cancelled booking.
There is no "decrement on book / increment on cancel" code path to get out of sync.

### Worked example — 10-session package

| Moment                                              | total | used | scheduled | remaining |
|-----------------------------------------------------|-------|------|-----------|-----------|
| Just purchased, one future appointment booked        | 10    | 0    | 1         | 9         |
| That appointment's time passes, not cancelled        | 10    | 1    | 0         | 9         |
| Instead: appointment cancelled before it happened    | 10    | 0    | 0         | 10        |
| Cancelled appointment whose time later passes        | 10    | 0    | 0         | 10        |

A future appointment is **reserved, not used**. It only becomes used once its
start time has passed, automatically — no admin or employee has to mark it.

## Where it lives

| Concern                                   | Type                                                  |
|-------------------------------------------|-------------------------------------------------------|
| The four numbers                          | `glowbook.service.PackageSessionAccounting` (record)  |
| The single calculation                    | `glowbook.service.PackageSessionAccountingService`    |
| Validation before booking                 | `CustomerPackageService.reserveSession(...)`          |
| Writing the derived value back to the row | `PackageSessionAccountingService.synchronize(...)`    |

`customer_packages.remaining_session` is kept as a **cache** of the derived
`remaining` value so the column stays truthful for reporting. It is rewritten by
`synchronize(...)` after any appointment create or cancel that touches a package;
nothing reads it to make a decision.

## Booking against a package

`CustomerPackageService.reserveSession(customerPackageId, customerId, serviceId)`:

1. the package must be active and owned by that customer,
2. the package's service must match the appointment's service,
3. the package must not be past `valid_until` (it is deactivated if it is),
4. `remaining > 0`.

It deliberately **does not decrement anything**. The session is consumed by the
existence of the appointment row. `AppointmentService.create(...)` calls
`synchronize(...)` immediately after saving, and `AppointmentService.cancel(...)`
calls it immediately after the status flips to `CANCELLED`.

Re-cancelling an already cancelled appointment is a no-op and cannot restore a
second session.

## Purchase + first appointment

`POST /api/customers/{customerId}/packages/{packageId}/booking`

```json
{ "employeeId": "GLW002", "optionId": 12, "appointmentDate": "2026-08-14", "appointmentTime": "14:00" }
```

Handled by `PackageBookingService.purchaseAndBookFirstAppointment(...)` inside a
single `@Transactional` boundary:

1. resolve the package → **the service is derived from it, never taken from the caller**,
2. resolve the covered sub-service (see below),
3. create the `CustomerPackage`,
4. create exactly **one** appointment through the normal `AppointmentService.create`
   path, so slot validation, full-hour rule, past-time rejection, employee
   qualification and conflict detection all still apply,
5. recompute the accounting.

If step 4 fails for any reason the package purchase is rolled back with it. There
is no state where a package exists without its first appointment.

### Covered sub-service

The package fixes the service, so the customer is never asked for it again. The
option is resolved as follows:

- caller sent `optionId` → validated against the package's service and used,
- the service has exactly one active option → picked silently,
- the service has several active options and none was sent → `Bu paket birden fazla
  seçenek kapsıyor. Lütfen birini seç.`

That is the only question the flow may still ask.

## API shape

`GET /api/customers/{customerId}/packages` returns, per package:

```json
{
  "customerPackageId": 5,
  "packageId": 201,
  "packageName": "5 Bolge Lazer Paketi",
  "serviceId": 2,
  "serviceName": "Lazer Epilasyon",
  "totalSession": 10,
  "usedSession": 1,
  "scheduledSession": 0,
  "remainingSession": 9,
  "validUntil": "2027-08-14",
  "active": true
}
```

The Flutter "Paketlerim" section renders these directly as
Toplam / Kullanılan / Planlanan / Kalan. It never computes `total - remaining`
itself, which would wrongly show a planned appointment as already used.

## Tests

| Test                                                              | Covers                                    |
|-------------------------------------------------------------------|-------------------------------------------|
| `PackageSessionAccountingServiceTest`                             | E, F, G, H, I — all four numbers, fixed clock |
| `PackageBookingLifecycleIntegrationTest`                          | full section-21 scenario, rollback, conflict |
| `test/profile_packages_history_test.dart`                         | Paketlerim rendering                       |
| `test/package_booking_flow_test.dart`                             | flow skips the service question            |
