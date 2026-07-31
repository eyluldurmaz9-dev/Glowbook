package glowbook.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LoggingSmsSender implements SmsSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingSmsSender.class);

    @Override
    public void sendSms(String phone, String message) {
        LOGGER.info("SMS queued to {}: {}", phone, message);
    }
}
