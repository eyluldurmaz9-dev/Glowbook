# Appointment History Classification

How GlowBook decides whether an appointment belongs to **Yaklaşan Randevular** or
**Geçmiş Randevular**.

## Business timezone

There is one configured business timezone, defaulting to `Europe/Istanbul`:

```properties
glowbook.business-time-zone=Europe/Istanbul
```

`BusinessTimeConfig` exposes it as two beans:

- `ZoneId businessZoneId`
- `Clock businessClock` — every time-sensitive rule injects this

Nothing in the codebase compares a naive server instant against a local
appointment string, and no `+03:00` offset is hard-coded anywhere. A container
running in UTC (Railway) classifies exactly like the salon does. Tests replace the
bean with `glowbook.support.MutableClock`, so nothing depends on the wall clock.

The clock is used by:

- slot availability (`AppointmentAlgorithmService`)
- past-date / past-time rejection
- appointment history classification (this document)
- package session accounting ([PACKAGE_SESSION_ACCOUNTING.md](PACKAGE_SESSION_ACCOUNTING.md))
- package validity (`valid_until`) checks

## The chosen rule

`Appointment` stores `appointment_date` and `appointment_time` only. **No duration
or end instant is modelled**, so the specification's documented fallback applies:

```
now      := LocalDateTime.now(businessClock)
start    := appointmentDate + appointmentTime

upcoming := status != CANCELLED  AND  start > now
past     := status == CANCELLED  OR   start <= now
```

Implemented once in `glowbook.service.AppointmentTimeClassifier` and used by both
`AppointmentService.getUpcomingCustomerAppointments` and
`getPastCustomerAppointments`.

The two buckets are **exhaustive and mutually exclusive**: every appointment lands
in exactly one of them. `AppointmentTimeClassifierTest` asserts this for every
combination of status and hour.

### Time grouping is not business status

An appointment whose stored status is still `PENDING` or `APPROVED` moves into
history the moment its start time passes. No database status change, no scheduled
job and no manual action by an admin or employee is needed — reopening or
refreshing the app is enough.

`COMPLETED` remains a meaningful business status (an employee confirming the
service happened) and is preserved, but it is not what decides the bucket.

### Worked example

| Appointment          | Business time now     | Bucket   |
|----------------------|-----------------------|----------|
| 14 Aug 2026, 09:00   | 14 Aug 2026, 11:00    | Geçmiş   |
| 14 Aug 2026, 14:00   | 14 Aug 2026, 11:00    | Yaklaşan |
| 13 Aug 2026, 14:00   | 14 Aug 2026, 11:00    | Geçmiş   |

## Cancelled appointments

| Case                     | Yaklaşan | Geçmiş | Consumes a package session |
|--------------------------|----------|--------|----------------------------|
| Cancelled, still future  | no       | yes    | no                         |
| Cancelled, already past  | no       | yes    | no                         |

A cancelled appointment is never an active upcoming appointment, but the record is
preserved in history and the Flutter list labels it **İptal edildi**.

## Booking-time rules (unchanged)

Both still read the same business clock:

- **Full hours only** — `09:00`, `10:00`, … Half-hour starts are rejected with
  `Randevu saati tam saat olmalıdır (örnek: 10:00).`
- **Past date** — `Geçmiş bir tarih için randevu oluşturamazsın.`
- **Past same-day hour** — `Bu saat artık geçmişte kaldı. Lütfen başka bir saat seç.`
  At 14:20 the slots 09:00–14:00 are neither offered nor accepted; 15:00 onward are.
- **Taken slot** — HTTP 409; the customer sees
  `Bu saat artık uygun değil. Lütfen başka bir saat seç.` and is returned to the
  time step with package, service, employee and date preserved.

## Flutter

- `customerUpcomingAppointmentsProvider` → `GET /api/appointments/customer/{id}/upcoming`
- `customerPastAppointmentsProvider` → `GET /api/appointments/customer/{id}/past`

Both are `autoDispose`, so leaving and re-entering the screen re-asks the backend
and the grouping is re-evaluated against current business time. The customer
profile renders them under **Yaklaşan Randevular** and **Geçmiş Randevular**; the
customer dashboard mirrors them in its Yaklaşan / Geçmiş tabs. The client does no
date arithmetic of its own for grouping.

## Tests

| Test                                              | Covers                                        |
|---------------------------------------------------|-----------------------------------------------|
| `AppointmentTimeClassifierTest`                   | A, B, C, D + exclusivity, fixed clock         |
| `AppointmentTimeRulesTest`                        | O, P — full-hour and past-time, fixed clock   |
| `PackageBookingLifecycleIntegrationTest`          | live move from Yaklaşan to Geçmiş as the clock advances |
| `test/profile_packages_history_test.dart`         | UI split, no appointment in both buckets      |
