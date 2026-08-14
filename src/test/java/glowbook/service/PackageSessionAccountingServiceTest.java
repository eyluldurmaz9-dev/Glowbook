package glowbook.service;

import glowbook.entity.Appointment;
import glowbook.entity.AppointmentStatus;
import glowbook.entity.CustomerPackage;
import glowbook.entity.ServicePackage;
import glowbook.repository.AppointmentRepository;
import glowbook.repository.CustomerPackageRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests E-I of the specification: package session accounting against a fixed clock.
 */
class PackageSessionAccountingServiceTest {

    private static final ZoneId ISTANBUL = ZoneId.of("Europe/Istanbul");
    private static final Integer PACKAGE_ID = 77;

    /** 14 August 2026, 11:00 Istanbul. */
    private static final Clock BEFORE_APPOINTMENT =
            Clock.fixed(Instant.parse("2026-08-14T08:00:00Z"), ISTANBUL);
    /** 14 August 2026, 15:00 Istanbul — after a 14:00 appointment. */
    private static final Clock AFTER_APPOINTMENT =
            Clock.fixed(Instant.parse("2026-08-14T12:00:00Z"), ISTANBUL);

    @Test
    void tenSessionPackageWithOneFutureBookingReportsScheduledNotUsed() {
        PackageSessionAccounting accounting = accountingAt(
                BEFORE_APPOINTMENT,
                List.of(appointment(LocalTime.of(14, 0), AppointmentStatus.PENDING)));

        assertThat(accounting.total()).isEqualTo(10);
        assertThat(accounting.used()).isZero();
        assertThat(accounting.scheduled()).isEqualTo(1);
        assertThat(accounting.remaining()).isEqualTo(9);
    }

    @Test
    void sameAppointmentBecomesUsedOnceItsTimePasses() {
        PackageSessionAccounting accounting = accountingAt(
                AFTER_APPOINTMENT,
                List.of(appointment(LocalTime.of(14, 0), AppointmentStatus.PENDING)));

        assertThat(accounting.total()).isEqualTo(10);
        assertThat(accounting.used()).isEqualTo(1);
        assertThat(accounting.scheduled()).isZero();
        assertThat(accounting.remaining()).isEqualTo(9);
    }

    @Test
    void cancelledFutureAppointmentRestoresAvailabilityExactlyOnce() {
        PackageSessionAccounting accounting = accountingAt(
                BEFORE_APPOINTMENT,
                List.of(appointment(LocalTime.of(14, 0), AppointmentStatus.CANCELLED)));

        assertThat(accounting.used()).isZero();
        assertThat(accounting.scheduled()).isZero();
        assertThat(accounting.remaining()).isEqualTo(10);
    }

    @Test
    void cancelledPastAppointmentStaysInHistoryWithoutConsumingASession() {
        PackageSessionAccounting accounting = accountingAt(
                AFTER_APPOINTMENT,
                List.of(appointment(LocalTime.of(14, 0), AppointmentStatus.CANCELLED)));

        assertThat(accounting.used()).isZero();
        assertThat(accounting.remaining()).isEqualTo(10);
    }

    @Test
    void mixedHistoryIsNeverDoubleCounted() {
        PackageSessionAccounting accounting = accountingAt(
                AFTER_APPOINTMENT,
                List.of(
                        appointment(LocalTime.of(10, 0), AppointmentStatus.COMPLETED),
                        appointment(LocalTime.of(11, 0), AppointmentStatus.APPROVED),
                        appointment(LocalTime.of(12, 0), AppointmentStatus.CANCELLED),
                        futureAppointment(AppointmentStatus.PENDING),
                        futureAppointment(AppointmentStatus.APPROVED)));

        assertThat(accounting.total()).isEqualTo(10);
        assertThat(accounting.used()).isEqualTo(2);
        assertThat(accounting.scheduled()).isEqualTo(2);
        assertThat(accounting.remaining()).isEqualTo(6);
    }

    @Test
    void aFullyBookedPackageHasNoBookableSessionLeft() {
        List<Appointment> tenBookings = java.util.stream.IntStream.range(0, 10)
                .mapToObj(index -> futureAppointment(AppointmentStatus.PENDING))
                .toList();

        PackageSessionAccounting accounting = accountingAt(BEFORE_APPOINTMENT, tenBookings);

        assertThat(accounting.scheduled()).isEqualTo(10);
        assertThat(accounting.remaining()).isZero();
        assertThat(accounting.hasBookableSession()).isFalse();
    }

    private PackageSessionAccounting accountingAt(Clock clock, List<Appointment> appointments) {
        AppointmentRepository appointmentRepository = mock(AppointmentRepository.class);
        CustomerPackageRepository customerPackageRepository = mock(CustomerPackageRepository.class);
        when(appointmentRepository.findByCustomerPackageCustomerPackageId(eq(PACKAGE_ID)))
                .thenReturn(appointments);

        PackageSessionAccountingService service = new PackageSessionAccountingService(
                appointmentRepository,
                customerPackageRepository,
                new AppointmentTimeClassifier(clock));

        return service.calculate(tenSessionPackage());
    }

    private CustomerPackage tenSessionPackage() {
        return CustomerPackage.builder()
                .customerPackageId(PACKAGE_ID)
                .servicePackage(ServicePackage.builder().packageId(5).totalSession(10).build())
                .remainingSession(10)
                .active(true)
                .build();
    }

    private Appointment appointment(LocalTime time, AppointmentStatus status) {
        return Appointment.builder()
                .customerPackage(CustomerPackage.builder().customerPackageId(PACKAGE_ID).build())
                .appointmentDate(LocalDate.of(2026, 8, 14))
                .appointmentTime(time)
                .status(status)
                .build();
    }

    private Appointment futureAppointment(AppointmentStatus status) {
        return Appointment.builder()
                .customerPackage(CustomerPackage.builder().customerPackageId(PACKAGE_ID).build())
                .appointmentDate(LocalDate.of(2026, 9, 14))
                .appointmentTime(LocalTime.of(14, 0))
                .status(status)
                .build();
    }
}
