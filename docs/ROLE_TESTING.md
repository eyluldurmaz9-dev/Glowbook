# Role testing

Demo accounts are created only when `ENABLE_DEMO_USERS=true`. Passwords are mandatory environment secrets:

- `admin@glowbook.test` / `DEMO_ADMIN_PASSWORD`
- `employee@glowbook.test` / `DEMO_EMPLOYEE_PASSWORD`
- `customer@glowbook.test` / `DEMO_CUSTOMER_PASSWORD`

No plaintext password is present in source. The employee demo account is a real active employee assigned to every active service. Disable the flag after testing; disabling does not automatically delete existing demo rows, so remove/deactivate them through an authorized operational process if they were enabled in production.

Login uses `POST /api/auth/login` with `username`, `password` and requested `role`. The JWT role comes from the stored account type/employee role, never from registration or an unchecked client request. Normal registration always returns CUSTOMER.

`DemoUserIntegrationTest` verifies all three logins, ADMIN/EMPLOYEE access to staff endpoints, CUSTOMER rejection, and the employee-service link.
