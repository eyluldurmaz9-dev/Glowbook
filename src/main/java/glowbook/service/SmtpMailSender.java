package glowbook.service;

import glowbook.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Real SMTP delivery via Spring's own mail starter, using the same explicit
 * opt-in pattern as {@link TwilioSmsSender}: inert until an operator sets
 * {@code app.mail.provider=smtp} (MAIL_PROVIDER=smtp) and the spring.mail.*
 * connection properties (MAIL_HOST/MAIL_PORT/MAIL_USERNAME/MAIL_PASSWORD),
 * all of which are environment variables — no credentials in source.
 */
@Service
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "smtp")
public class SmtpMailSender implements MailSender {

    private final JavaMailSender javaMailSender;

    @Value("${app.mail.from:}")
    private String fromAddress;

    public SmtpMailSender(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    @Override
    public void sendMail(String to, String subject, String body) {
        if (isBlank(to)) {
            throw new BusinessException("Mail recipient is missing");
        }
        if (isBlank(fromAddress)) {
            throw new BusinessException("MAIL_FROM environment variable is missing");
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setFrom(fromAddress);
            message.setSubject(subject);
            message.setText(body);
            javaMailSender.send(message);
        } catch (RuntimeException exception) {
            throw new BusinessException("Mail could not be sent");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
