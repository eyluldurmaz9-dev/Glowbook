package glowbook.entity;

/**
 * Lifecycle of an outbound WhatsApp message, as reported by the Cloud API send call
 * (PENDING/SENT/FAILED) and later by Meta's delivery webhook (DELIVERED/READ). A
 * successful send response only means Meta <b>accepted</b> the message — it must never
 * be read as "delivered" on its own.
 */
public enum NotificationDeliveryStatus {

    PENDING,
    SENT,
    DELIVERED,
    READ,
    FAILED
}
