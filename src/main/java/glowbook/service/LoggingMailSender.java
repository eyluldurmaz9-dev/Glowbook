package glowbook.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Default mail sender: no real SMTP configured, so this just logs what would
 * have been sent. Same role as {@link LoggingSmsSender}/LoggingWhatsAppSender
 * — safe no-op until {@code app.mail.provider=smtp} plus real credentials are
 * configured on the deployment platform.
 */
@Service
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "log", matchIfMissing = true)
public class LoggingMailSender implements MailSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingMailSender.class);

    @Override
    public void sendMail(String to, String subject, String body) {
        LOGGER.info("Mail queued to {} - subject: \"{}\" ({} characters)",
                maskEmail(to), subject, body == null ? 0 : body.length());
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "****";
        }
        int at = email.indexOf('@');
        String local = email.substring(0, at);
        String domain = email.substring(at);
        String visible = local.length() <= 2 ? local.substring(0, 1) : local.substring(0, 2);
        return visible + "****" + domain;
    }
}
