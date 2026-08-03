package glowbook.dto;

import glowbook.entity.NotificationType;

import java.time.LocalDateTime;

public final class NotificationDtos {

    private NotificationDtos() {
    }

    public record NotificationResponse(
            Integer notificationId,
            Integer customerId,
            Integer appointmentId,
            NotificationType type,
            String title,
            String message,
            Boolean read,
            Boolean smsSent,
            LocalDateTime createdAt
    ) {
    }
}
