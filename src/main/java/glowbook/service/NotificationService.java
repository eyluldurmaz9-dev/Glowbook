package glowbook.service;

import glowbook.entity.Appointment;
import glowbook.entity.Customer;
import glowbook.entity.Notification;
import glowbook.entity.NotificationType;
import glowbook.exception.ResourceNotFoundException;
import glowbook.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final SmsSender smsSender;

    public List<Notification> getCustomerNotifications(Integer customerId) {
        return notificationRepository.findByCustomerCustomerIdOrderByCreatedAtDesc(customerId);
    }

    public List<Notification> getUnreadCustomerNotifications(Integer customerId) {
        return notificationRepository.findByCustomerCustomerIdAndReadFalseOrderByCreatedAtDesc(customerId);
    }

    public Notification getById(Integer notificationId) {
        return notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));
    }

    public boolean appointmentNotificationExists(Integer appointmentId, NotificationType type) {
        return notificationRepository.existsByAppointmentAppointmentIdAndType(appointmentId, type);
    }

    @Transactional
    public Notification create(Customer customer, Appointment appointment, NotificationType type, String title, String message) {
        Notification notification = Notification.builder()
                .customer(customer)
                .appointment(appointment)
                .type(type)
                .title(title)
                .message(message)
                .read(false)
                .smsSent(false)
                .build();

        return notificationRepository.save(notification);
    }

    @Transactional
    public Notification createAndSendSms(Customer customer, Appointment appointment, NotificationType type, String title, String message, String phone) {
        Notification notification = create(customer, appointment, type, title, message);
        try {
            smsSender.sendSms(phone, message);
            notification.setSmsSent(true);
        } catch (RuntimeException exception) {
            LOGGER.warn("SMS could not be sent for appointment {}: {}", appointment == null ? null : appointment.getAppointmentId(), exception.getMessage());
            notification.setSmsSent(false);
        }
        return notificationRepository.save(notification);
    }

    @Transactional
    public void createAndSendSmsSafely(Customer customer, Appointment appointment, NotificationType type, String title, String message, String phone) {
        try {
            Notification notification = create(customer, appointment, type, title, message);
            try {
                smsSender.sendSms(phone, message);
                notification.setSmsSent(true);
            } catch (RuntimeException exception) {
                LOGGER.warn("SMS could not be sent for appointment {}: {}", appointment == null ? null : appointment.getAppointmentId(), exception.getMessage());
                notification.setSmsSent(false);
            }
            notificationRepository.save(notification);
        } catch (RuntimeException exception) {
            LOGGER.warn("Notification could not be saved for appointment {}: {}", appointment == null ? null : appointment.getAppointmentId(), exception.getMessage());
        }
    }

    @Transactional
    public Notification markAsRead(Integer notificationId) {
        Notification notification = getById(notificationId);
        notification.setRead(true);
        return notificationRepository.save(notification);
    }

    @Transactional
    public void sendPendingSms(LocalDateTime beforeDateTime) {
        notificationRepository.findBySmsSentFalseAndCreatedAtBefore(beforeDateTime)
                .forEach(notification -> {
                    String phone = notification.getAppointment() != null
                            ? notification.getAppointment().getPhone()
                            : notification.getCustomer() == null ? null : notification.getCustomer().getPhone();

                    if (phone != null && !phone.isBlank()) {
                        smsSender.sendSms(phone, notification.getMessage());
                        notification.setSmsSent(true);
                        notificationRepository.save(notification);
                    }
                });
    }
}
