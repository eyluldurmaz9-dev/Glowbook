package glowbook.service;

import glowbook.entity.Appointment;
import glowbook.entity.AppointmentStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests A-D of the specification: appointment history classification against a fixed clock.
 */
class AppointmentTimeClassifierTest {

    private static final ZoneId ISTANBUL = ZoneId.of("Europe/Istanbul");
    /** 14 August 2026, 11:00 Istanbul (08:00 UTC). */
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-08-14T08:00:00Z"), ISTANBUL);

    private final AppointmentTimeClassifier classifier = new AppointmentTimeClassifier(FIXED_CLOCK);

    @Test
    void futureAppointmentIsUpcomingOnly() {
        Appointment appointment = appointment(LocalDate.of(2026, 8, 20), LocalTime.of(14, 0), AppointmentStatus.APPROVED);

        assertThat(classifier.isUpcoming(appointment)).isTrue();
        assertThat(classifier.isPast(appointment)).isFalse();
    }

    @Test
    void pastDayAppointmentIsHistoryOnly() {
        Appointment appointment = appointment(LocalDate.of(2026, 8, 13), LocalTime.of(14, 0), AppointmentStatus.APPROVED);

        assertThat(classifier.isPast(appointment)).isTrue();
        assertThat(classifier.isUpcoming(appointment)).isFalse();
    }

    @Test
    void todayNineAmAtElevenAmMovesToHistoryEvenWhenStillConfirmed() {
        Appointment appointment = appointment(LocalDate.of(2026, 8, 14), LocalTime.of(9, 0), AppointmentStatus.APPROVED);

        assertThat(classifier.isPast(appointment)).isTrue();
        assertThat(classifier.isUpcoming(appointment)).isFalse();
    }

    @Test
    void todayLaterHourStaysUpcoming() {
        Appointment appointment = appointment(LocalDate.of(2026, 8, 14), LocalTime.of(14, 0), AppointmentStatus.PENDING);

        assertThat(classifier.isUpcoming(appointment)).isTrue();
        assertThat(classifier.isPast(appointment)).isFalse();
    }

    @Test
    void cancelledPastAppointmentIsHistoryAndNotCountedAsUsed() {
        Appointment appointment = appointment(LocalDate.of(2026, 8, 13), LocalTime.of(14, 0), AppointmentStatus.CANCELLED);

        assertThat(classifier.isPast(appointment)).isTrue();
        assertThat(classifier.isUpcoming(appointment)).isFalse();
        assertThat(classifier.isCancelled(appointment)).isTrue();
    }

    @Test
    void cancelledFutureAppointmentIsNeverUpcoming() {
        Appointment appointment = appointment(LocalDate.of(2026, 8, 20), LocalTime.of(14, 0), AppointmentStatus.CANCELLED);

        assertThat(classifier.isUpcoming(appointment)).isFalse();
        assertThat(classifier.isPast(appointment)).isTrue();
    }

    @Test
    void anAppointmentNeverBelongsToBothBuckets() {
        for (LocalTime time : java.util.List.of(LocalTime.of(9, 0), LocalTime.of(11, 0), LocalTime.of(14, 0))) {
            for (AppointmentStatus status : AppointmentStatus.values()) {
                Appointment appointment = appointment(LocalDate.of(2026, 8, 14), time, status);
                assertThat(classifier.isUpcoming(appointment) && classifier.isPast(appointment)).isFalse();
                assertThat(classifier.isUpcoming(appointment) || classifier.isPast(appointment)).isTrue();
            }
        }
    }

    private Appointment appointment(LocalDate date, LocalTime time, AppointmentStatus status) {
        return Appointment.builder()
                .appointmentDate(date)
                .appointmentTime(time)
                .status(status)
                .build();
    }
}
