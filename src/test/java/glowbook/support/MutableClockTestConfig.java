package glowbook.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Instant;
import java.time.ZoneId;

/**
 * Replaces the production system clock with a controllable one for integration tests.
 * Marked {@link Primary} so every {@code Clock} injection point picks it up.
 */
@TestConfiguration(proxyBeanMethods = false)
public class MutableClockTestConfig {

    /** Friday 14 August 2026, 09:00 Istanbul — a working day, before the 10:00 opening slot. */
    public static final Instant DEFAULT_INSTANT = Instant.parse("2026-08-14T06:00:00Z");

    @Bean
    @Primary
    public MutableClock testBusinessClock(ZoneId businessZoneId) {
        return new MutableClock(DEFAULT_INSTANT, businessZoneId);
    }
}
