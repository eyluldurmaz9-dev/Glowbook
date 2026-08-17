package glowbook.service;

import glowbook.entity.Appointment;
import glowbook.entity.Notification;
import glowbook.entity.NotificationChannel;
import glowbook.entity.NotificationDeliveryStatus;
import glowbook.repository.AppointmentRepository;
import glowbook.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Sends the WhatsApp appointment-confirmation template after a booking has committed.
 *
 * <p>Deliberately mirrors {@link NotificationService#createAndSendAfterCommit}: same
 * {@code appointmentNotificationExists} dedup check (so a repeated event/retry can never
 * send a second confirmation for the same appointment) and the same {@code
 * REQUIRES_NEW} + try/catch shape (so a Cloud API failure can only ever mark this one
 * notification row FAILED — it can never roll back or otherwise affect the appointment
 * that already committed). Kept as its own class, rather than folded into {@code
 * NotificationService}, so the SMS path used by approve/cancel/update stays untouched.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WhatsAppNotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WhatsAppNotificationService.class);

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.of("tr", "TR"));
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final NotificationRepository notificationRepository;
    private final AppointmentRepository appointmentRepository;
    private final TurkishPhoneNumberService phoneNumberService;
    private final WhatsAppSender whatsAppSender;

    /**
     * Top-level product switch, independent of which {@link WhatsAppSender} bean would
     * be selected. Defaults to {@code false}: no real salon uses GlowBook yet, so there
     * is no Meta template to send against — the architecture and tests stay ready, but
     * nothing is attempted (not even the no-op logging provider) until this is turned on.
     */
    @Value("${app.whatsapp.enabled:false}")
    private boolean whatsAppEnabled;

    @Value("${whatsapp.template-name:appointment_confirmation_tr}")
    private String templateName;

    @Value("${whatsapp.template-language:tr}")
    private String templateLanguage;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendAppointmentConfirmation(AppointmentSmsEvent event) {
        if (!whatsAppEnabled) {
            LOGGER.info("WhatsApp notifications are disabled (WHATSAPP_ENABLED=false); skipping appointment {}",
                    event.appointmentId());
            return;
        }

        Appointment appointment = appointmentRepository.findById(event.appointmentId()).orElse(null);
        if (appointment == null
                || notificationRepository.existsByAppointmentAppointmentIdAndType(event.appointmentId(), event.type())) {
            return;
        }

        Notification notification = Notification.builder()
                .customer(appointment.getCustomer())
                .appointment(appointment)
                .type(event.type())
                .title(event.title())
                .message(event.message())
                .read(false)
                .smsSent(false)
                .channel(NotificationChannel.WHATSAPP)
                .template(templateName)
                .deliveryStatus(NotificationDeliveryStatus.PENDING)
                .build();

        try {
            String phone = phoneNumberService.normalize(event.phone());
            List<String> parameters = buildTemplateParameters(appointment);
            WhatsAppSendResult result = whatsAppSender.sendTemplate(phone, templateName, templateLanguage, parameters);
            if (result.accepted()) {
                notification.setProviderMessageId(result.providerMessageId());
                notification.setDeliveryStatus(NotificationDeliveryStatus.SENT);
                notification.setSentAt(LocalDateTime.now());
            } else {
                notification.setDeliveryStatus(NotificationDeliveryStatus.FAILED);
                notification.setFailedAt(LocalDateTime.now());
                LOGGER.warn("WhatsApp confirmation failed for appointment {}: {}",
                        event.appointmentId(), result.errorMessage());
            }
        } catch (RuntimeException exception) {
            notification.setDeliveryStatus(NotificationDeliveryStatus.FAILED);
            notification.setFailedAt(LocalDateTime.now());
            LOGGER.warn("WhatsApp confirmation could not be sent for appointment {}: {}",
                    event.appointmentId(), exception.getMessage());
        }

        notificationRepository.save(notification);
    }

    /**
     * Applies a delivery-status update reported by Meta's webhook (see {@link
     * glowbook.controller.WhatsAppWebhookController}) to the notification it belongs to.
     *
     * <p>A successful send only ever proves Meta <b>accepted</b> the message
     * ({@link NotificationDeliveryStatus#SENT}); this is the only path that can promote
     * a notification to DELIVERED or READ. Out-of-order or duplicate webhook deliveries
     * (Meta does not guarantee ordering) must not regress an already-more-advanced
     * status back to an earlier one.</p>
     */
    @Transactional
    public void recordDeliveryStatusUpdate(String providerMessageId, String rawStatus) {
        if (providerMessageId == null || providerMessageId.isBlank()) {
            return;
        }
        NotificationDeliveryStatus newStatus = mapStatus(rawStatus);
        if (newStatus == null) {
            return;
        }
        Notification notification = notificationRepository.findByProviderMessageId(providerMessageId).orElse(null);
        if (notification == null) {
            return;
        }
        if (newStatus != NotificationDeliveryStatus.FAILED
                && notification.getDeliveryStatus() != null
                && !isAdvancement(notification.getDeliveryStatus(), newStatus)) {
            return;
        }

        notification.setDeliveryStatus(newStatus);
        LocalDateTime now = LocalDateTime.now();
        switch (newStatus) {
            case SENT -> notification.setSentAt(now);
            case DELIVERED -> notification.setDeliveredAt(now);
            case READ -> notification.setReadAt(now);
            case FAILED -> notification.setFailedAt(now);
            case PENDING -> {
            }
        }
        notificationRepository.save(notification);
    }

    private static final List<NotificationDeliveryStatus> PROGRESSION = List.of(
            NotificationDeliveryStatus.PENDING,
            NotificationDeliveryStatus.SENT,
            NotificationDeliveryStatus.DELIVERED,
            NotificationDeliveryStatus.READ
    );

    private boolean isAdvancement(NotificationDeliveryStatus current, NotificationDeliveryStatus candidate) {
        int currentIndex = PROGRESSION.indexOf(current);
        int candidateIndex = PROGRESSION.indexOf(candidate);
        return currentIndex < 0 || candidateIndex < 0 || candidateIndex > currentIndex;
    }

    private NotificationDeliveryStatus mapStatus(String rawStatus) {
        if (rawStatus == null) {
            return null;
        }
        return switch (rawStatus.toLowerCase(Locale.ROOT)) {
            case "sent" -> NotificationDeliveryStatus.SENT;
            case "delivered" -> NotificationDeliveryStatus.DELIVERED;
            case "read" -> NotificationDeliveryStatus.READ;
            case "failed" -> NotificationDeliveryStatus.FAILED;
            default -> null;
        };
    }

    /** Order must match the approved template's body placeholders: date, time, service, employee. */
    private List<String> buildTemplateParameters(Appointment appointment) {
        String date = appointment.getAppointmentDate().format(DATE_FORMATTER);
        String time = appointment.getAppointmentTime().format(TIME_FORMATTER);
        String serviceName = appointment.getService() == null ? "" : appointment.getService().getServiceName();
        String employeeName = appointment.getEmployee() == null ? "" : (
                appointment.getEmployee().getFirstName() + " " + appointment.getEmployee().getLastName());
        return List.of(date, time, serviceName, employeeName);
    }
}
