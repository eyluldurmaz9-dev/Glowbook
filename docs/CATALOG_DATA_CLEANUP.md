# Catalog Data Cleanup — Duplicate Turkish Records

## What was wrong

Earlier Turkish-spelling fixes to `CatalogSeedConfig` changed the seed's authoritative
strings (e.g. `"Kas Alimi"` → `"Kaş Alımı"`) without changing how the seed *matched*
existing rows. Two separate bugs compounded:

1. **`createOption`/`createPackage` matched by exact name.** A row already in the
   database with the old spelling was never recognized as "the same option" as the new,
   correctly-spelled name — so instead of updating it, the seed created a **second** row
   with the correct spelling, alongside the untouched old one.
2. **A canonical-name match never overwrote the row's own name.** `createService`
   already normalized Turkish diacritics away before comparing (so it *did* recognize
   `"Kas ve Kirpik"` and `"Kaş ve Kirpik"` as the same service) — but on a match it only
   refreshed `description`/`image`/`active`, never `serviceName` itself. A row could
   therefore stay misspelled forever even without ever being duplicated.

Together, this is exactly how the production database ended up with pairs like
`Bölgesel incelme` / `Bolgesel Incelme` (two services) and single mis-titled rows that
canonical matching silently kept re-selecting without ever correcting.

## The fix

Every `createXxx` helper in `CatalogSeedConfig` now:

1. Finds **every** existing row that is the same business entity once Turkish
   diacritics/case are normalized away (`canonicalName`) — not just the first one.
2. If more than one is found, merges every foreign-key reference off every extra row
   and onto a single survivor (see below), then deletes the extras.
3. **Unconditionally overwrites** the survivor's own fields — name included — to the
   authoritative value, regardless of whether a merge happened.

Scoping matters for correctness: services are matched by name alone; options and
packages are matched by name **within their current service**, so two genuinely
different offerings that happen to share a word are never merged. Nothing whose name
does not canonically match one of the authoritative names in `seedCatalogData()` is
ever touched — an admin-created service/option/package/employee with no counterpart in
that list keeps its id, its name, and every relationship exactly as it was.

## What "merge" actually moves

Every foreign key documented as pointing at `Service`, `ServiceOption`, `ServicePackage`
or `Employee` is re-pointed from the duplicate onto the survivor before the duplicate is
deleted — appointments, employee assignments, waiting-list entries, owned customer
packages, and a package's `coveredOptions` (a many-to-many the option side does not own,
fixed up by editing each package's own set so a package that referenced both spellings
of the same option keeps exactly one reference afterwards, not two). Nothing that
already existed — an appointment, a purchased package, a customer — is ever deleted;
only the now-unreferenced duplicate catalog row is.

Employees are **never** merged: `createEmployee` matches by stable id
(`GLW001`, `ADMIN`, …), so there is no name-based ambiguity to resolve — a misspelled
display name is simply corrected in place, same as a single unduplicated service.

A separate, final pass — `deduplicateEmployeeServiceAssignments()` — collapses any
`employee_services` row left logically duplicate as a *side effect* of a service/option
merge (an employee could have been separately assigned to both a duplicate and its
survivor before they were merged into one).

## Why this is safe to run on every startup

Nothing here is a one-off migration script with its own run-once marker — it is the same
`@Order(0)` `ApplicationRunner` that always seeded the catalog, now simply thorough about
convergence. Once a business entity has exactly one row with the correct name, every
subsequent run finds nothing to merge and nothing to rename: `serviceRepository.count()`
and friends are stable across repeated `seedCatalogData()` calls
(`CatalogSeedConfigDeduplicationTest.reseedingRepeatedlyIsANoOp`). This is what makes the
fix permanent rather than a one-time cleanup that the same underlying bug (had it not
also been fixed) could have silently undone on the next deploy.

## Also fixed in the same pass

- **JDBC connection charset.** `application.properties` / `application-prod.properties` /
  `application-dev.properties` were missing `useUnicode=true&characterEncoding=UTF-8` on
  the MySQL connection URL. Without it, MySQL Connector/J can negotiate a different
  character set than the application intends, which is how a correctly-typed Turkish
  string in Java source can still come back mangled after a round trip through the
  database. This is the root cause of the double-encoded names (e.g. `Cilt BakÄ±mÄ±`)
  observed in production separately from the duplicate-row problem above.
- **Employee display names.** `"Defne Yilmaz"` → `"Defne Yılmaz"`,
  `"Selin Aydin"` → `"Selin Aydın"` — spelling drift in the seed's own string literals,
  unrelated to duplication (their employee ids never changed).
- **Customer-facing message audit.** `GlobalExceptionHandler` used to build validation
  error text from the raw Java field name plus Hibernate Validator's built-in English
  default message (e.g. `"customerName: must not be blank"`) — every `@NotBlank`/
  `@NotNull`/etc. across every request DTO now carries an explicit Turkish `message=`,
  and the handler no longer prefixes the field name. Every remaining English
  `BusinessException`/`ConflictException`/`ResourceNotFoundException` string reachable
  from a normal customer or admin action (booking, login, registration, session refresh,
  employee/holiday/working-hour management, …) was rewritten in Turkish; the handful left
  in English (Twilio SMS environment/signing failures) are genuine server-misconfiguration
  paths that a real user's own actions cannot trigger in this deployment
  (`app.sms.provider=log` by default), not normal user-facing text.
