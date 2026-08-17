package glowbook.support;

import glowbook.service.WhatsAppSendResult;
import glowbook.service.WhatsAppSender;

import java.util.ArrayList;
import java.util.List;

/**
 * Records every WhatsApp send instead of calling the real Meta Cloud API. Registered as
 * the {@code @Primary} {@link WhatsAppSender} bean by {@link WhatsAppTestConfig}, so no
 * automated test can ever reach the real network — see task requirement "tests must
 * never call the real Meta API".
 */
public class FakeWhatsAppSender implements WhatsAppSender {

    public record RecordedSend(String phone, String templateName, String languageCode, List<String> parameters) {
    }

    private final List<RecordedSend> sends = new ArrayList<>();
    private boolean nextSendFails = false;
    private String failureMessage = "Simulated WhatsApp failure";

    @Override
    public synchronized WhatsAppSendResult sendTemplate(String phoneE164, String templateName, String languageCode,
                                                          List<String> bodyParameters) {
        sends.add(new RecordedSend(phoneE164, templateName, languageCode, List.copyOf(bodyParameters)));
        if (nextSendFails) {
            nextSendFails = false;
            return WhatsAppSendResult.failed(failureMessage);
        }
        return WhatsAppSendResult.accepted("fake-message-" + sends.size());
    }

    public synchronized List<RecordedSend> sends() {
        return List.copyOf(sends);
    }

    public synchronized int sendCount() {
        return sends.size();
    }

    public synchronized void failNextSend(String message) {
        this.nextSendFails = true;
        this.failureMessage = message;
    }

    public synchronized void reset() {
        sends.clear();
        nextSendFails = false;
    }
}
