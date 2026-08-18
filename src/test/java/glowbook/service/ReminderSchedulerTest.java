package glowbook.service;

import glowbook.entity.Appointment;
import glowbook.entity.AppointmentStatus;
import glowbook.entity.Customer;
import glowbook.entity.Notification;
import glowbook.repository.AppointmentRepository;
import glowbook.repository.CustomerRepository;
import glowbook.repository.EmployeeServiceRepository;
import glowbook.repository.NotificationRepository;
import glowbook.repository.ServiceOptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression guard for the reported "Randevu hatirlatma" (missing ı) defect: the
 * reminder title/message must always be generated with the correct Turkish
 * character, since {@link ReminderScheduler} writes this text into a new
 * {@link Notification} row on every run - Notification.title is never
 * recomputed after it's first persisted (see NotificationService.create), so
 * this is the one place that governs every newly created reminder's text.
 */
@SpringBootTest
class ReminderSchedulerTest {

    @Autowired ReminderScheduler reminderScheduler;
    @Autowired AppointmentRepository appointmentRepository;
    @Autowired NotificationRepository notificationRepository;
    @Autowired CustomerRepository customerRepository;
    @Autowired EmployeeServiceRepository employeeServiceRepository;
    @Autowired ServiceOptionRepository serviceOptionRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        appointmentRepository.deleteAll();
    }

    @Test
    void newReminderTitleAndMessageUseCorrectTurkishCharacters() {
        var assignment = employeeServiceRepository.findAll().stream()
                .filter(item -> "GLW001".equals(item.getEmployee().getEmployeeId()))
                .findFirst().orElseThrow();
        var option = serviceOptionRepository
                .findByServiceServiceIdAndActiveTrueOrderByOptionNameAsc(
                        assignment.getService().getServiceId())
                .getFirst();

        Customer customer = customerRepository.save(Customer.builder()
                .firstName("Reminder").lastName("Test").phone("05559991122")
                .email("reminder-turkish-check@glowbook.test")
                .password(passwordEncoder.encode("test-password")).active(true).build());

        appointmentRepository.save(Appointment.builder()
                .customer(customer)
                .employee(assignment.getEmployee())
                .service(assignment.getService())
                .serviceOption(option)
                .customerName(customer.getFirstName())
                .customerSurname(customer.getLastName())
                .phone(customer.getPhone())
                .appointmentDate(LocalDate.now().plusDays(1))
                .appointmentTime(LocalTime.of(11, 0))
                .price(BigDecimal.valueOf(option.getPrice()))
                .status(AppointmentStatus.PENDING)
                .build());

        reminderScheduler.sendAppointmentReminders();

        var notifications = notificationRepository
                .findByCustomerCustomerIdOrderByCreatedAtDesc(customer.getCustomerId());
        assertThat(notifications).hasSize(1);
        Notification reminder = notifications.getFirst();
        assertThat(reminder.getTitle()).isEqualTo("Randevu hatırlatma");
        assertThat(reminder.getMessage()).contains("hatırlatma");
        // The exact defect reported: a dotless "i" silently substituted for "ı".
        assertThat(reminder.getTitle()).doesNotContain("hatirlatma");
    }
}
