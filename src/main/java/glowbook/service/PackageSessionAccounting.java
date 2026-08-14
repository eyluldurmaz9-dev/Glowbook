package glowbook.service;

/**
 * Authoritative, fully derived view of how many sessions of a customer package
 * are consumed, reserved and still bookable.
 *
 * <p>The four numbers are computed from the appointments linked to the package,
 * never from an incrementally mutated counter, so a session can never be
 * subtracted twice nor restored twice.</p>
 *
 * <ul>
 *     <li>{@code total} — sessions the purchased package contains.</li>
 *     <li>{@code used} — linked appointments whose start time has passed and that were not cancelled.</li>
 *     <li>{@code scheduled} — linked appointments still in the future and not cancelled.</li>
 *     <li>{@code remaining} — {@code total - used - scheduled}, i.e. what may still be booked.</li>
 * </ul>
 */
public record PackageSessionAccounting(int total, int used, int scheduled, int remaining) {

    public static PackageSessionAccounting of(int total, int used, int scheduled) {
        int safeTotal = Math.max(total, 0);
        int safeUsed = Math.max(used, 0);
        int safeScheduled = Math.max(scheduled, 0);
        return new PackageSessionAccounting(
                safeTotal,
                safeUsed,
                safeScheduled,
                Math.max(safeTotal - safeUsed - safeScheduled, 0)
        );
    }

    public boolean hasBookableSession() {
        return remaining > 0;
    }

    /** A package is exhausted once every session has been used or reserved. */
    public boolean isExhausted() {
        return remaining <= 0;
    }
}
