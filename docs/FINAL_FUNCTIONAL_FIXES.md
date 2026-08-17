# KOMUT 2 — Final Functional Fixes

Package booking flow + back navigation + state integrity repair. See
`docs/PACKAGE_SERVICE_COVERAGE.md`, `docs/PACKAGE_BOOKING_RULES.md` and
`docs/BOOKING_STATE_MACHINE.md` for the detailed mechanics; this file is the
summary of what was wrong, what changed, and how it was verified.

## Defects fixed

**A — package flow could book an uncovered service.** `ServicePackage` only
linked to a broad `Service` category, never to specific `ServiceOption`s, so
a Hydrafacial-only package could be used to book "Akne Bakımı" — same
service, different, non-purchased sub-service. Fixed by adding
`ServicePackage.coveredOptions` (authoritative `@ManyToMany` to
`ServiceOption`) and enforcing it at both places a package ever backs an
appointment: `PackageBookingService.resolveCoveredOption` (first booking) and
`CustomerPackageService.reserveSession` (later session). See
`docs/PACKAGE_SERVICE_COVERAGE.md`.

**B — package booking asked for the service again.** The Flutter wizard's
option step (`_CoveredOptionStep`) queried every option under the package's
service, unfiltered, and `PackageDetailPage` never told it which option(s)
the package actually covers. Fixed: `CatalogPackage`/`CustomerPackageOption`
now carry `coveredOptions` from the backend; a single-option package derives
its option silently (no step shown at all); a multi-option package's step
now lists only its own covered options. See `docs/PACKAGE_BOOKING_RULES.md`.

**C/D — Back navigation and state integrity.** Audited every selection field
in `_AppointmentPageState` and the three ways Back can happen (in-page
button, Android/iOS system gesture, browser back). The in-page button and the
step list were already state-safe; the concrete gap was that Android
hardware back / iOS swipe-back had no interception at all and would pop the
whole page — skipping the wizard's own step history rather than walking
backward through it. Fixed with a `PopScope` that steps `_step` down instead
of leaving the page while `_step > 0`. No generic-error path is reachable
from Back: `GlowError` only ever renders from a provider's `AsyncValue.error`
branch, never from local step-state changes. See
`docs/BOOKING_STATE_MACHINE.md`.

**E — backend authority.** All four validations required by the task
(package exists/active, belongs to the customer, sub-option is covered,
employee is qualified) already existed or were added in this pass and are
covered by `PackageServiceCoverageIntegrationTest`.

**F — customer-facing wording.** New/changed rejection messages are all
Turkish and non-technical (`"Bu hizmet seçtiğin pakete dahil değil."`,
`"Seçilen personel bu hizmeti vermiyor."`, `"Paketinde planlanabilir seans
kalmadı."`). One pre-existing English message reachable from the package
purchase path (`"Customer already owns an active copy of this package"`) was
also fixed to Turkish while in this code (`CustomerPackageService.purchase`).

## Missing entry point found and fixed

`PackageDetailPage` never set `customerPackageId`, so "book a later session
against an already-owned package" was unreachable from the UI — clicking "Bu
paketle randevu al" on an owned package would always retry a *purchase* and
hit a 409 conflict. Fixed: the page now looks up ownership via
`customerPackagesProvider` before navigating and passes `customerPackageId`
when the package is already owned, so the same button correctly serves both
a first purchase and a later session. `profile_page.dart`'s "Paketlerim"
cards were also not tappable at all; they now route into this same
`PackageDetailPage` screen, so every package entry point in the spec
(Paketlerim → package → "Bu paketle randevu al"; package purchase → first
appointment; service detail → relevant package → booking) shares one
package-scoped code path.

## Files changed

**Backend** (`C:\dev\Glowbook`):
`src/main/java/glowbook/entity/ServicePackage.java`,
`dto/CatalogDtos.java`, `dto/CustomerDtos.java`, `dto/DtoMapper.java`,
`service/ServicePackageService.java`, `service/PackageBookingService.java`,
`service/CustomerPackageService.java`, `service/AppointmentService.java`,
`controller/CatalogController.java`, `config/CatalogSeedConfig.java` (also
fixed Turkish diacritics in the real seed data, same defect class as KOMUT 1
found in the Flutter fallback data);
`test/java/glowbook/controller/PackageServiceCoverageIntegrationTest.java`
(new, 13 tests A–M);
`PackageBookingLifecycleIntegrationTest.java`,
`WhatsAppAppointmentNotificationIntegrationTest.java` (updated to derive
their option under test from the package's own `coveredOptions`, since they
previously picked an arbitrary option that the new coverage check correctly
rejects).

**Flutter** (`C:\dev\glowbook-flutter`):
`lib/features/appointment/appointment_page.dart` (`PopScope`, coverage
filter on the option step), `lib/features/appointment/booking_models.dart`
(`PackageBookingContext.coveredOptionIds`, `CustomerPackageOption
.coveredOptions`), `lib/features/catalog/catalog_models.dart`
(`CatalogPackage.coveredOptions`), `lib/features/package/package_detail_page
.dart` (derives the known option, looks up ownership), `lib/features/profile
/profile_page.dart` (Paketlerim cards now navigate);
`test/package_coverage_and_back_navigation_test.dart` (new, 5 tests);
`test/package_booking_flow_test.dart`, `test/booking_success_navigation_test
.dart` (fixtures updated to include realistic `coveredOptions`).

## Test results

Backend: `mvnw.cmd test` — 79/79 passed, 0 failures, 0 errors (66 pre-existing
+ 13 new). `mvnw.cmd clean verify` — success.

Flutter: `flutter analyze` — no issues. `flutter test` — 143/143 passed.
`flutter build web --release` — success (pre-existing, unrelated wasm
dry-run warning from `flutter_secure_storage_web`, not from this change).
Android build: skipped — this environment's Android SDK is missing the
cmdline-tools component (`flutter doctor` reports the toolchain incomplete).

## Explicitly not changed

Ordinary (non-package) booking still asks for a service and sub-service —
`_stepKinds` only special-cases the flow when `_packageContext != null`.
No visual design, imagery, colors, typography, or layout was touched; every
edit is to state/data-flow logic, provider wiring, or backend validation.
