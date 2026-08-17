package glowbook.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Integer notificationId;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "is_read")
    @Builder.Default
    private Boolean read = false;

    @Column(name = "sms_sent")
    @Builder.Default
    private Boolean smsSent = false;

    /** Which channel actually carried this notification. Existing rows predate this
     * column and default to SMS, matching what they were sent through. */
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", length = 20)
    @Builder.Default
    private NotificationChannel channel = NotificationChannel.SMS;

    /** Meta template name used for a WhatsApp send (e.g. "appointment_confirmation_tr").
     * Null for SMS notifications. */
    @Column(name = "template", length = 100)
    private String template;

    /** The WhatsApp Cloud API message id returned on send-accepted, used to correlate
     * later delivery-status webhooks back to this row. Null for SMS or before a
     * WhatsApp send has been attempted. */
    @Column(name = "provider_message_id", length = 100)
    private String providerMessageId;

    /** WhatsApp-specific lifecycle (see {@link NotificationDeliveryStatus}). Left null
     * for SMS notifications, which only ever use {@link #smsSent}. */
    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", length = 20)
    private NotificationDeliveryStatus deliveryStatus;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
