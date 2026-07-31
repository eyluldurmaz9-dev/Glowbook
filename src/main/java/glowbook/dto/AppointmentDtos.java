package glowbook.dto;

import glowbook.entity.AppointmentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

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
            @NotBlank String employeeId,
            @NotNull Integer serviceId,
            @NotNull Integer optionId,
            String customerName,
            String customerSurname,
            String phone,
            @NotNull LocalDate appointmentDate,
            @NotNull LocalTime appointmentTime
    ) {
    }

    public record AppointmentTimeUpdateRequest(
            @NotNull LocalDate appointmentDate,
            @NotNull LocalTime appointmentTime
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
