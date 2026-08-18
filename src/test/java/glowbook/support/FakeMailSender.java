package glowbook.support;

import glowbook.service.MailSender;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Records every mail send instead of calling real SMTP. Registered as the
 * {@code @Primary} {@link MailSender} bean by {@link MailTestConfig} — same
 * shape as {@link FakeWhatsAppSender}/{@link WhatsAppTestConfig}.
 */
public class FakeMailSender implements MailSender {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("token=([^\\s]+)");

    public record RecordedMail(String to, String subject, String body) {
    }

    private final List<RecordedMail> sends = new ArrayList<>();

    @Override
    public synchronized void sendMail(String to, String subject, String body) {
        sends.add(new RecordedMail(to, subject, body));
    }

    public synchronized List<RecordedMail> sends() {
        return List.copyOf(sends);
    }

    public synchronized int sendCount() {
        return sends.size();
    }

    /** Extracts the raw reset token from the most recently sent mail's link. */
    public synchronized String lastToken() {
        RecordedMail last = sends.get(sends.size() - 1);
        Matcher matcher = TOKEN_PATTERN.matcher(last.body());
        if (!matcher.find()) {
            throw new IllegalStateException("No reset token found in mail body: " + last.body());
        }
        return matcher.group(1);
    }

    public synchronized void reset() {
        sends.clear();
    }
}
