package glowbook.service;

import java.util.List;

/**
 * Channel abstraction for sending an approved WhatsApp Business template message,
 * mirroring {@link SmsSender}'s shape. {@code AppointmentService}/{@code
 * WhatsAppNotificationService} never talk to the Cloud API directly — they depend on
 * this interface, and Spring selects whichever implementation {@code
 * app.whatsapp.provider} names.
 */
public interface WhatsAppSender {

    /**
     * @param phoneE164     recipient in E.164 form, e.g. {@code +905551234567}
     * @param templateName  the Meta-approved template name (never hardcoded per call site)
     * @param languageCode  the template's approved locale, e.g. {@code tr}
     * @param bodyParameters ordered values filling the template's body placeholders
     */
    WhatsAppSendResult sendTemplate(String phoneE164, String templateName, String languageCode,
                                     List<String> bodyParameters);
}
