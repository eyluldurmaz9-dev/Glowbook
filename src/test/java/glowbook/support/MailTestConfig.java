package glowbook.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Replaces the production mail provider with {@link FakeMailSender} for
 * integration tests — same shape as {@link WhatsAppTestConfig}.
 */
@TestConfiguration(proxyBeanMethods = false)
public class MailTestConfig {

    @Bean
    @Primary
    public FakeMailSender fakeMailSender() {
        return new FakeMailSender();
    }
}
