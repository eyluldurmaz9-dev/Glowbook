package glowbook.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Default WhatsApp provider: makes no external request at all. Active whenever {@code
 * app.whatsapp.provider} is unset or {@code log} (local/dev/CI default), so tests and
 * environments without Meta credentials never touch the real Cloud API.
 */
@Service
@ConditionalOnProperty(name = "app.whatsapp.provider", havingValue = "log", matchIfMissing = true)
public class LoggingWhatsAppSender implements WhatsAppSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingWhatsAppSender.class);

    @Override
    public WhatsAppSendResult sendTemplate(String phone, String templateName, String languageCode,
                                            List<String> bodyParameters) {
        LOGGER.info("WhatsApp template '{}' ({}) queued to {} with {} parameter(s)",
                templateName, languageCode, maskPhone(phone), bodyParameters.size());
        return WhatsAppSendResult.accepted("log-" + UUID.randomUUID());
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return "****";
        }
        return "****" + phone.substring(phone.length() - 4);
    }
}
