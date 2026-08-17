# Booking Wizard State Machine

Governs `lib/features/appointment/appointment_page.dart`'s
`_AppointmentPageState` in the Flutter app (`glowbook-flutter` repo).

## Step state

`_step` (an `int`) indexes into `_stepKinds`, a list computed fresh on every
build from `_packageContext` (see `docs/PACKAGE_BOOKING_RULES.md` for how
that list differs between normal and package booking). Every selection
(`_service`, `_option`, `_customerPackage`, `_packageEmployeeId`, `_date`,
`_time`, `_slot`) lives directly on the `State` object, not behind a
provider, so moving `_step` backward or forward never re-fetches or discards
a prior selection by itself — only an explicit, narrowly-scoped `setState`
call ever clears one (see "Selective resets" below).

## Three ways Back can happen

1. **In-page "Geri" button** (`_ActionBar.onBack`) — disabled at `_step == 0`
   (`onBack: null`), otherwise decrements `_step` and calls `_scrollToTop()`.
2. **Android hardware back / iOS swipe-back gesture** — intercepted by a
   `PopScope` wrapping the page's `Scaffold`:
   `canPop: _step == 0 && !_submitting`. While `_step > 0`,
   `onPopInvokedWithResult` decrements `_step` exactly like the in-page
   button instead of letting the system gesture pop the route and leave the
   page — otherwise a mid-wizard hardware back would jump straight out to
   whatever page preceded the wizard, skipping the
   Summary → Time → Date → Employee sequence entirely. A submission in
   flight (`_submitting`) blocks the gesture outright so an in-flight
   request is never abandoned.
3. **Browser back (web)** — a URL-level navigation handled by `go_router`,
   outside the wizard's own step state. Every package entry point encodes
   its full selection (`packageId`, `serviceId`, `optionId`,
   `coveredOptionIds`, …) into the route's query string
   (`PackageBookingContext.toQuery`/`fromQuery`), so a browser back to a
   previous `/appointment?...` URL — or a page refresh — reconstructs the
   same package context from the URL rather than depending on in-memory
   state that a fresh page load would not have.

## Why Back cannot land on a generic error

The generic error widget (`GlowError`, default title "Bir şey ters gitti")
only ever renders from an `AsyncValue.error` branch of a provider's `.when`
call — never from step navigation itself, since `_step`, `_packageContext`
and every selection field are synchronous local state, not futures. Back
therefore cannot itself throw. The one thing Back can do is *reveal* a step
whose provider is mid-flight or freshly re-subscribed
(`serviceOptionsProvider`, `employeesByServiceOptionProvider`,
`availableSlotsForDatesProvider` are all `.autoDispose.family`, so navigating
away and back can re-trigger a fetch) — that step shows its own `loading`
state while it resolves and only reaches `error` for a genuine backend
failure, which is the intended, narrow use of the generic error surface.

## Selective resets

Moving forward can invalidate a downstream choice — but Back never does:

- Picking a different **employee** (package mode) clears `_time`/`_slot`
  (the previously chosen slot belonged to the old employee) but leaves the
  package/service/option untouched.
- Picking a different **date** clears `_time`/`_slot` for the same reason.
- Picking a covered **option** on a multi-option package clears
  `_packageEmployeeId`/`_time`/`_slot` (an employee qualified for the old
  option may not be for the new one) but keeps `_packageContext` itself.

Back never triggers any of this: decrementing `_step` does not call
`_selectService`, `_selectCoveredOption`, or any other selection handler, so
it cannot cascade-clear anything. The full-flow reset
(`_resetBookingState`, which zeroes every field including `_packageContext`)
is called exactly once, only after the backend has confirmed a successful
booking — never as a side effect of navigation, and never on failure (a
failed submission leaves every selection exactly as the customer left it, so
they can simply retry).

## Stale async responses

`_submitAppointment` re-checks slot availability
(`ref.refresh(availableSlotsProvider(...).future)`) immediately before
submitting, and every `setState` after an `await` is guarded by `if
(!mounted) return;` — so a response that resolves after the customer has
already navigated away (Back included) cannot write into a disposed
`State`, and cannot resurrect a stale success or failure after the fact.
`_BookingSubmission` (`idle` / `submitting` / `success` / `failure`) is set
exactly once per submission attempt, so a success and a later conflict error
can never be shown at the same time.
