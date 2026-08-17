package glowbook.repository;

import glowbook.entity.Notification;
import glowbook.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    List<Notification> findByCustomerCustomerIdOrderByCreatedAtDesc(Integer customerId);

    List<Notification> findByCustomerCustomerIdAndReadFalseOrderByCreatedAtDesc(Integer customerId);

    boolean existsByAppointmentAppointmentIdAndType(Integer appointmentId, NotificationType type);

    List<Notification> findBySmsSentFalseAndCreatedAtBefore(LocalDateTime beforeDateTime);

    /** Correlates an inbound WhatsApp delivery-status webhook back to the notification it belongs to. */
    Optional<Notification> findByProviderMessageId(String providerMessageId);
}
