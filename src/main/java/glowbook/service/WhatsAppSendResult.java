package glowbook.service;

/**
 * Outcome of a single WhatsApp Cloud API send attempt.
 *
 * <p>{@code accepted() == true} only means Meta's Graph API accepted the message for
 * delivery and returned a message id — it is <b>not</b> proof the customer received it.
 * Actual delivery/read status arrives later through the webhook (see
 * {@link glowbook.controller.WhatsAppWebhookController}) and is recorded separately on
 * {@link glowbook.entity.Notification#getDeliveryStatus()}.</p>
 */
public record WhatsAppSendResult(boolean accepted, String providerMessageId, String errorMessage) {

    public static WhatsAppSendResult accepted(String providerMessageId) {
        return new WhatsAppSendResult(true, providerMessageId, null);
    }

    public static WhatsAppSendResult failed(String errorMessage) {
        return new WhatsAppSendResult(false, null, errorMessage);
    }
}
