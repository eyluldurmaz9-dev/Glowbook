# Package purchase flow

The supported flow is catalog → detail/benefits/session count/price/validity → explicit confirmation → `POST /api/customers/{customerId}/packages/{packageId}` → Paketlerim.

Purchase persists a `CustomerPackage` with full session count, purchase price/date and `validUntil`. It does not call appointment APIs and does not consume a session. A second active copy returns 409. Customer-row locking prevents concurrent duplicate requests.

After success Flutter exposes **Bu paketle randevu al** as a separate action. Booking with a package passes `customerPackageId`; the backend validates ownership, service match, expiry and remaining sessions before consuming one session.

There is no payment-provider integration. The UI explicitly calls this a demo/reservation purchase and must not imply that money was collected. A real payment provider still requires server-side payment intent, webhook verification, idempotency key and paid/refunded states.
