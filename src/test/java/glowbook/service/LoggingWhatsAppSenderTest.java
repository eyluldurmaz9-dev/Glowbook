package glowbook.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spec item I: with the default/disabled provider, sending must make zero external
 * requests. {@link LoggingWhatsAppSender} has no {@code HttpClient} field at all — it is
 * architecturally incapable of reaching the network — so a plain unit test proves this
 * without any mocking/interception needed.
 */
class LoggingWhatsAppSenderTest {

    @Test
    void sendingMakesNoExternalRequestAndReturnsAnAcceptedResult() {
        LoggingWhatsAppSender sender = new LoggingWhatsAppSender();

        WhatsAppSendResult result = sender.sendTemplate(
                "+905551234567", "appointment_confirmation_tr", "tr",
                List.of("17 Ağustos 2026", "10:00", "Cilt Bakımı", "Eylem Ceylan"));

        assertThat(result.accepted()).isTrue();
        assertThat(result.providerMessageId()).startsWith("log-");
        assertThat(result.errorMessage()).isNull();
    }
}
