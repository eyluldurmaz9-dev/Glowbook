package glowbook.dto;

import glowbook.entity.AppointmentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public final class AppointmentDtos {

    private AppointmentDtos() {
    }

    public record AppointmentRequest(
            Integer customerId,
            Integer customerPackageId,
            @NotBlank(message = "Personel seçilmelidir.") String employeeId,
            @NotNull(message = "Hizmet seçilmelidir.") Integer serviceId,
            @NotNull(message = "Alt hizmet seçilmelidir.") Integer optionId,
            String customerName,
            String customerSurname,
            @Pattern(regexp = "^\\+?[0-9][0-9 ()-]{7,19}$", message = "Geçerli bir telefon numarası girilmelidir.") String phone,
            @NotNull(message = "Randevu tarihi seçilmelidir.") LocalDate appointmentDate,
            @NotNull(message = "Randevu saati seçilmelidir.") LocalTime appointmentTime
    ) {
    }

    public record AppointmentTimeUpdateRequest(
            @NotNull(message = "Randevu tarihi seçilmelidir.") LocalDate appointmentDate,
            @NotNull(message = "Randevu saati seçilmelidir.") LocalTime appointmentTime
    ) {
    }

    /** Customer-facing reschedule. A blank employeeId keeps the appointment's current employee. */
    public record AppointmentRescheduleRequest(
            String employeeId,
            @NotNull(message = "Randevu tarihi seçilmelidir.") LocalDate appointmentDate,
            @NotNull(message = "Randevu saati seçilmelidir.") LocalTime appointmentTime
    ) {
    }

    public record CancelAppointmentRequest(
            String cancellationReason
    ) {
    }

    public record AppointmentResponse(
            Integer appointmentId,
            Integer customerId,
            Integer customerPackageId,
            String employeeId,
            String employeeName,
            Integer serviceId,
            String serviceName,
            Integer optionId,
            String optionName,
            String customerName,
            String customerSurname,
            String phone,
            LocalDate appointmentDate,
            LocalTime appointmentTime,
            BigDecimal price,
            AppointmentStatus status,
            String cancellationReason,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record AvailableSlotResponse(
            String employeeId,
            String employeeName,
            LocalDate appointmentDate,
            List<LocalTime> availableTimes
    ) {
    }
}
