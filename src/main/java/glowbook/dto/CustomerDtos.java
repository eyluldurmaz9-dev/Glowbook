package glowbook.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.time.LocalDateTime;

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

    public record CustomerPackageResponse(
            Integer customerPackageId,
            Integer customerId,
            Integer packageId,
            String packageName,
            String serviceName,
            Integer totalSession,
            Integer remainingSession,
            Double purchasePrice,
            LocalDate purchaseDate,
            LocalDate validUntil,
            Boolean active
    ) {
    }
}
