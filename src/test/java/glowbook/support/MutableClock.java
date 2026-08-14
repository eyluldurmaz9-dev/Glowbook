package glowbook.support;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Test clock whose "now" can be moved forward on demand.
 *
 * <p>Every time-sensitive rule in GlowBook reads the injected business {@link Clock},
 * so replacing it with this class makes availability, appointment history and package
 * session accounting fully deterministic — no test ever depends on the wall clock.</p>
 */
public class MutableClock extends Clock {

    private final ZoneId zone;
    private volatile Instant instant;

    public MutableClock(Instant instant, ZoneId zone) {
        this.instant = instant;
        this.zone = zone;
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId otherZone) {
        return new MutableClock(instant, otherZone);
    }

    @Override
    public Instant instant() {
        return instant;
    }

    /** Moves the business clock to the given local date/time in the business zone. */
    public void setBusinessTime(LocalDateTime businessDateTime) {
        this.instant = businessDateTime.atZone(zone).toInstant();
    }

    public LocalDateTime businessTime() {
        return LocalDateTime.ofInstant(instant, zone);
    }
}
