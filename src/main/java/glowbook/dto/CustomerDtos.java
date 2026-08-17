package glowbook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public final class CustomerDtos {

    private CustomerDtos() {
    }

    public record CustomerRequest(
            @NotBlank String firstName,
            @NotBlank String lastName,
            @NotBlank String phone,
            @NotBlank String password,
            String email,
            Boolean active
    ) {
    }

    public record CustomerResponse(
            Integer customerId,
            String firstName,
            String lastName,
            String phone,
            String email,
            Boolean active,
            LocalDateTime createdAt
    ) {
    }

    /**
     * {@code totalSession}/{@code usedSession}/{@code scheduledSession}/{@code remainingSession}
     * come from the single derived accounting rule; see docs/PACKAGE_SESSION_ACCOUNTING.md.
     */
    public record CustomerPackageResponse(
            Integer customerPackageId,
            Integer customerId,
            Integer packageId,
            String packageName,
            String serviceName,
            Integer serviceId,
            Integer totalSession,
            Integer usedSession,
            Integer scheduledSession,
            Integer remainingSession,
            Double purchasePrice,
            LocalDate purchaseDate,
            LocalDate validUntil,
            Boolean active,
            /** Same authoritative coverage as {@code CatalogDtos.ServicePackageResponse} —
             * exactly one entry means booking this owned package must never ask again
             * which sub-service to use. */
            List<CatalogDtos.ServiceOptionResponse> coveredOptions
    ) {
    }

    public record PackageBookingRequest(
            @NotBlank String employeeId,
            Integer optionId,
            @NotNull LocalDate appointmentDate,
            @NotNull LocalTime appointmentTime
    ) {
    }

    public record PackageBookingResponse(
            CustomerPackageResponse customerPackage,
            AppointmentDtos.AppointmentResponse appointment
    ) {
    }
}
