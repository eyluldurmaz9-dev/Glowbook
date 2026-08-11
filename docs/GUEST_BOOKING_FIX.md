# Guest booking fix

`POST /api/appointments` accepts guest bookings without authentication and without `customerId`.

Required guest fields are `customerName`, `customerSurname`, `phone`, `employeeId`, `serviceId`, `optionId`, `appointmentDate` (`YYYY-MM-DD`) and `appointmentTime` (`HH:mm`). The server resolves the employee, service and sub-service, derives price from the selected option, validates assignment/holiday/leave/working hours/availability, and persists a nullable customer/package relationship.

Responses:

- 400: missing/invalid guest data or other validation rule
- 404: employee, service or option does not exist/is inactive
- 409: unavailable or duplicate slot
- 500: uncaught internal failure only

The production 500 was caused by notification persistence in an independent transaction that could not see the uncommitted appointment. Notification persistence now participates in the appointment transaction, so Hibernate inserts the appointment before its dependent notification.

Automated coverage is in `AppointmentControllerIntegrationTest`: valid guest, missing phone, invalid employee, unavailable slot, duplicate slot and registered-customer regression.
