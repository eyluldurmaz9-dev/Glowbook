package glowbook.service;

import glowbook.entity.Appointment;
import glowbook.entity.AppointmentStatus;
import glowbook.entity.NotificationType;
import glowbook.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReminderScheduler {

    private final AppointmentRepository appointmentRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "${app.scheduler.reminder-cron:0 0 10 * * *}")
    @Transactional
    public void sendAppointmentReminders() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        appointmentRepository.findByAppointmentDateAndStatusInOrderByAppointmentTimeAsc(
                        tomorrow,
                        Set.of(AppointmentStatus.PENDING, AppointmentStatus.APPROVED)
                )
                .stream()
                .filter(appointment -> !notificationService.appointmentNotificationExists(
                        appointment.getAppointmentId(),
                        NotificationType.APPOINTMENT_REMINDER
                ))
                .forEach(this::sendReminder);
    }

    private void sendReminder(Appointment appointment) {
        String message = "GlowBook randevu hatırlatma: "
                + appointment.getAppointmentDate()
                + " "
                + appointment.getAppointmentTime()
                + " tarihinde "
                + appointment.getService().getServiceName()
                + " randevunuz var.";

        notificationService.createAndSendSms(
                appointment.getCustomer(),
                appointment,
                NotificationType.APPOINTMENT_REMINDER,
                "Randevu hatırlatma",
                message,
                appointment.getPhone()
        );
    }
}
