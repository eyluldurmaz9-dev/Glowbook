package glowbook.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class BusinessTimeConfig {

    @Bean
    ZoneId businessZoneId(@Value("${glowbook.business-time-zone:Europe/Istanbul}") String zoneId) {
        return ZoneId.of(zoneId);
    }

    @Bean
    Clock businessClock(ZoneId businessZoneId) {
        return Clock.system(businessZoneId);
    }
}
