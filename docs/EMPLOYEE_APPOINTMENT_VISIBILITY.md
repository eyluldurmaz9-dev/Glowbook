# Employee Appointment Visibility

How an appointment booked by a customer reaches the assigned employee's schedule.

## One record, two views

There is exactly **one** `appointments` row per booking. The customer's
"Randevularım" and the employee's daily/weekly schedule are two queries over the
same table — there is no separate employee-side dataset to drift out of sync.

```
Customer books: Cilt Bakımı / Eylem Ceylan / 2026-08-14 14:00
        │
        └── appointments row  (employee_id = <Eylem Ceylan>)
                 ├── GET /api/appointments/customer/{customerId}/upcoming
                 └── GET /api/appointments/employee/{employeeId}
```

This holds for registered-customer bookings, guest bookings and package bookings
alike: all three go through `AppointmentService.create(...)`.

## The endpoint

```
GET /api/appointments/employee/{employeeId}?startDate=...&endDate=...
```

Backed by
`AppointmentRepository.findByEmployeeEmployeeIdAndAppointmentDateBetweenAndStatusInOrderByAppointmentDateAscAppointmentTimeAsc`,
filtered to the active statuses `PENDING` and `APPROVED`.

Each row carries what the employee needs:

| Field                                | Notes                                        |
|--------------------------------------|----------------------------------------------|
| `customerName`, `customerSurname`    | set for guests and copied from the account for members |
| `phone`                              | normalised Turkish mobile number             |
| `serviceName`, `optionName`          | what to prepare for                          |
| `appointmentDate`, `appointmentTime` | full hours only                              |
| `status`                             | PENDING / APPROVED                           |

Past appointments stay in the range result — the range is by date, so an employee
can still look back at earlier days in the week.

## Authorization

Enforced in the backend, not by Flutter filtering:

```java
// AuthorizationSupport.assertEmployeeCanAccess
ADMIN                                  -> any employee's schedule
EMPLOYEE whose token subject == id     -> own schedule only
anything else                          -> AccessDenied (403)
```

`AppointmentController.getEmployeeSchedule` is additionally guarded by
`@PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")`.

Consequences:

- an employee requesting another employee's schedule gets **403**, not a filtered list,
- a customer token gets **403**,
- an unauthenticated request gets **401**,
- even if a client tampered with its local filtering, no foreign appointment is
  ever returned.

## Employee qualification

`AppointmentService.create` refuses a booking whose employee is not assigned to the
selected service/option:

```
Seçilen personel bu hizmeti vermiyor.
```

`EmployeeService` rows with a `service_option` cover that option only; rows with a
null option cover every option of the service. `findAvailableSlots` uses the same
assignment table, so the employees a customer can pick and the employees who may be
booked are always the same set.

## Flutter

`EmployeeDashboardPage` reads the session's `employeeId` and builds
`EmployeeAppointmentsQuery(employeeId, weekStart, weekEnd)`. Tabs:

- **Bugün** — appointments on the selected day
- **Haftalık** — the seven-day grid, one card per day, each appointment showing
  service, customer display name, sub-service, date, time and a Turkish status pill
- **Çalışma Saatleri**, **Profil**

The dashboard never receives another employee's rows to filter, because the
backend scopes the query by the path employee id and rejects mismatches.

## Demo employee

`DemoUserConfig` (enabled with `ENABLE_DEMO_USERS=true`) links
`employee@glowbook.test` to employee id **DEMOEMP** and assigns it up to three
service options. That mechanism is unchanged.

## Tests

| Test                                                         | Covers                                                     |
|--------------------------------------------------------------|------------------------------------------------------------|
| `EmployeeAppointmentVisibilityIntegrationTest`               | K, L, M — DEMOEMP sees a guest booking, others get 403/empty, past bookings stay visible |
| `PackageBookingLifecycleIntegrationTest`                     | the package appointment appears in the booked employee's week and nowhere else |
| `DemoUserIntegrationTest`                                    | DEMOEMP linkage and cross-employee 403                     |
| `test/employee_appointment_visibility_test.dart`             | weekly screen shows the assigned appointment with full detail; another employee's is absent |
| `test/employee_dashboard_test.dart`                          | daily vs weekly filtering, status actions                  |

No test hard-codes production customer data.
