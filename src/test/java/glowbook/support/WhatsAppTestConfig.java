package glowbook.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Replaces the production WhatsApp provider with {@link FakeWhatsAppSender} for
 * integration tests — same shape as {@link MutableClockTestConfig}. Import this instead
 * of ever letting a test exercise {@code MetaWhatsAppSender} against the real Graph API.
 */
@TestConfiguration(proxyBeanMethods = false)
public class WhatsAppTestConfig {

    @Bean
    @Primary
    public FakeWhatsAppSender fakeWhatsAppSender() {
        return new FakeWhatsAppSender();
    }
}
