package glowbook.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.sms.provider", havingValue = "log", matchIfMissing = true)
public class LoggingSmsSender implements SmsSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingSmsSender.class);

    @Override
    public void sendSms(String phone, String message) {
        LOGGER.info("SMS queued to {} with {} characters", maskPhone(phone), message == null ? 0 : message.length());
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return "****";
        }
        String suffix = phone.substring(phone.length() - 4);
        return "****" + suffix;
    }
}
