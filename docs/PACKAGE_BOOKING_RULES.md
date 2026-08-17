# Package Booking Rules (Flutter wizard)

Governs `lib/features/appointment/appointment_page.dart`,
`lib/features/package/package_detail_page.dart` and
`lib/features/appointment/booking_models.dart` in the Flutter app. Backend
enforcement is authoritative regardless (see
`docs/PACKAGE_SERVICE_COVERAGE.md` in the backend repo) — everything below is
defense in depth plus the correct customer-facing flow.

## Step sequence

Normal (non-package) booking is unchanged:

```
Hizmet → Alt hizmet ve paket → Tarih → Saat → Personel → Özet
```

Package booking never asks for the service again — it always starts at
Personel, except that a package covering **more than one** sub-service still
has to ask which one:

```
[Seçenek, only if the package covers >1 option] → Personel → Tarih → Saat → Özet
```

`_AppointmentPageState._stepKinds` builds this list from
`PackageBookingContext.hasKnownOption` (`true` once an `optionId` is known —
either because the package has exactly one covered option, or because the
customer already picked one on the option step).

## Where the covered option comes from

`PackageDetailPage._continueToBooking`:

1. Reads `CatalogPackage.coveredOptions` (populated from the backend's
   `coveredOptions` field — see `CatalogDtos.ServicePackageResponse`).
2. If it has exactly one entry, that option's id is set as
   `PackageBookingContext.optionId` directly — the option step never renders.
3. If it has more than one, `optionId` is left `null` and
   `coveredOptionIds` (every covered option's id) is carried through the
   route instead — the wizard's option step (`_CoveredOptionStep`) then
   shows only those options, filtered out of the same
   `serviceOptionsProvider` result the normal flow uses, never the service's
   full catalog.

An empty `coveredOptionIds` (a stale link, or a package with no coverage
configured) renders an explicit "Paket kapsamında seçenek yok" empty state —
it never silently falls back to showing every option under the service,
since that fallback is exactly what caused the original defect.

## Ownership: first purchase vs. later session

`PackageDetailPage._continueToBooking` also looks up whether the signed-in
customer already owns a usable copy of this package
(`customerPackagesProvider` + `CustomerPackageOption.canUseFor`). If so, its
`customerPackageId` is carried into `PackageBookingContext` and the wizard
books a **later session** against the existing package instead of buying a
second copy. This makes "Paketlerim → package card → Bu paketle randevu al"
(profile_page.dart's `_MyPackagesSection`) and "Paket detayı → Bu paketle
randevu al" (a fresh, not-yet-owned package) go through the exact same
screen and the exact same coverage-restricted option step — there is only
one package-booking code path, not two.

## Employee filtering

The Personel step in package mode uses
`employeesByServiceOptionProvider(serviceId, optionId)`, never the broader
"employees for this service" list — so only employees actually qualified for
the package's resolved sub-service are offered.

## Summary step

`_SummaryStep` shows the package-derived service and option as plain text; in
package mode there is no service or (single-option) option step, so there is
no control the customer could use to change either before confirming.

## Back navigation

See `docs/BOOKING_STATE_MACHINE.md` for how Back — the in-page "Geri" button,
Android hardware back, and the iOS swipe-back gesture — is kept from ever
leaving `_packageContext` or the wizard's other selections in an inconsistent
state, and from ever surfacing a generic error as a side effect of navigating
backward.
