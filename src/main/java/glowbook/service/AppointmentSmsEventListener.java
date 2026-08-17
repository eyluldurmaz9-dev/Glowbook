package glowbook.service;

import glowbook.entity.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Routes each post-commit appointment event to exactly one channel.
 *
 * <p>A brand-new appointment ("Randevunuz oluşturuldu") is a business-initiated
 * notification and goes out over WhatsApp — see {@link WhatsAppNotificationService}.
 * Everything else this event type still covers (approve/cancel/reschedule, and the
 * reminder cron in {@link ReminderScheduler}) is unchanged and keeps using SMS via
 * {@link NotificationService}, so this is never both channels firing for the same
 * event.</p>
 */
@Component
@RequiredArgsConstructor
public class AppointmentSmsEventListener {

    private final NotificationService notificationService;
    private final WhatsAppNotificationService whatsAppNotificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void afterAppointmentCommit(AppointmentSmsEvent event) {
        if (event.type() == NotificationType.APPOINTMENT_CREATED) {
            whatsAppNotificationService.sendAppointmentConfirmation(event);
            return;
        }
        notificationService.createAndSendAfterCommit(event);
    }
}
