package glowbook.service;

import glowbook.entity.Appointment;
import glowbook.entity.AppointmentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Single authoritative rule for deciding whether an appointment belongs to
 * "Yaklaşan Randevular" or "Geçmiş Randevular".
 *
 * <p><b>Chosen rule.</b> {@link Appointment} stores only a start date and start
 * time; no duration or end instant is modelled, so the documented fallback from
 * the specification applies:</p>
 *
 * <pre>
 *   upcoming := status != CANCELLED AND start &gt; now
 *   past     := status == CANCELLED OR  start &lt;= now
 * </pre>
 *
 * <p>{@code now} is always read through the injected business {@link Clock}
 * (Europe/Istanbul by default), never through the container's system default
 * zone, so a server running in UTC classifies exactly like the salon does.</p>
 *
 * <p>Time grouping and business status are deliberately separate concepts: an
 * appointment whose stored status is still {@code PENDING} or {@code APPROVED}
 * moves into history the moment its start time passes. Cancelled appointments
 * are never "upcoming", but stay visible in history so the record is preserved.</p>
 */
@Component
@RequiredArgsConstructor
public class AppointmentTimeClassifier {

    private final Clock businessClock;

    public boolean isUpcoming(Appointment appointment) {
        return !isCancelled(appointment) && !hasStarted(appointment);
    }

    public boolean isPast(Appointment appointment) {
        return isCancelled(appointment) || hasStarted(appointment);
    }

    /**
     * Pure time predicate, independent of status: has the appointment's start moment passed?
     * Package accounting uses this together with {@link #isCancelled(Appointment)}.
     */
    public boolean hasStarted(Appointment appointment) {
        return !startOf(appointment).isAfter(now());
    }

    public boolean isCancelled(Appointment appointment) {
        return AppointmentStatus.CANCELLED.equals(appointment.getStatus());
    }

    public LocalDateTime now() {
        return LocalDateTime.now(businessClock);
    }

    private LocalDateTime startOf(Appointment appointment) {
        return LocalDateTime.of(appointment.getAppointmentDate(), appointment.getAppointmentTime());
    }
}
