# Package → Service Coverage

## The rule

A `ServicePackage` covers specific `ServiceOption`s (sub-services), not a whole
`Service` category. `ServicePackage.coveredOptions` (a `@ManyToMany` to
`ServiceOption`, table `package_covered_options`) is the **sole authority**
on what a package may be booked for. The package's broad `service` field
(e.g. "Cilt Bakımı") is display/filtering metadata only — it is never used to
decide whether a specific appointment is allowed.

This exists because most services have several sibling sub-services, and a
package sold for one of them does not automatically cover the others. Before
this relationship existed, a "Hydrafacial Bakım Paketi" (which should only
ever back a Hydrafacial appointment) could be used to book "Akne Bakımı"
simply because both belong to the "Cilt Bakımı" service — the exact defect
this model closes.

## Where it is enforced

Every booking path that consumes a package funnels through one of two
checks, both against `coveredOptions`, never against `service` alone:

- **First appointment** (buy + book in one call):
  `PackageBookingService.resolveCoveredOption` — a requested option must be
  a member of `servicePackage.getCoveredOptions()`; if none is requested and
  the package covers exactly one option, that option is used silently; if it
  covers more than one, the caller must say which.
- **Later session** (book against an already-owned package):
  `CustomerPackageService.reserveSession` — checks `serviceId` first (cheap,
  early rejection) and then, authoritatively, that the requested `optionId`
  is a member of `coveredOptions`.

Both reject a mismatch with the same customer-facing message:
`"Bu hizmet seçtiğin pakete dahil değil."` — never a technical one.

`AppointmentService.create` is the single entry point every booking path
(registered customer, guest, first package appointment, later package
session) funnels through, so the option is always resolved and validated in
exactly one place before an `Appointment` row is ever built.

## Employee qualification

Coverage decides *what* may be booked; `EmployeeServiceAssignmentService`
separately decides *who* may perform it. An employee row with a `null`
`serviceOption` is qualified for every option under that service; a row with
a specific `serviceOption` is qualified for only that one
(`EmployeeServiceRepository.employeeCanProvideOption`). A package booking
still fails with `"Seçilen personel bu hizmeti vermiyor."` if the chosen
employee cannot perform the package's covered option, independent of
coverage.

## Client responsibility

`CatalogDtos.ServicePackageResponse.coveredOptions` and
`CustomerDtos.CustomerPackageResponse.coveredOptions` expose the same list to
Flutter, so the booking wizard can restrict its own UI to genuinely covered
options (defense in depth — the backend enforces regardless). See
`docs/PACKAGE_BOOKING_RULES.md` for how the wizard uses this.

## Seed data

`CatalogSeedConfig` hardcodes real coverage for every demo package,
deliberately including both single-option packages (e.g. "Hydrafacial Bakım
Paketi" → only "Hydrafacial Bakım") and multi-option ones (e.g. "Glow Cilt
Paketi" → "Klasik Cilt Bakımı" + "Leke Bakımı", excluding "Akne Bakımı" and
"Anti Aging Bakım") so both code paths above are exercised by the same data
an engineer would use to manually verify the fix.
