package glowbook.service;

import glowbook.entity.NotificationType;

public record AppointmentSmsEvent(
        Integer appointmentId,
        NotificationType type,
        String title,
        String message,
        String phone
) {
}
