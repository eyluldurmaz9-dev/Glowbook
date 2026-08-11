package glowbook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public final class CatalogDtos {

    private CatalogDtos() {
    }

    public record ServiceRequest(
            @NotBlank String serviceName,
            String description,
            String serviceImage,
            Boolean active
    ) {
    }

    public record ServiceResponse(
            Integer serviceId,
            String serviceName,
            String description,
            String serviceImage,
            Boolean active
    ) {
    }

    public record ServiceOptionRequest(
            @NotBlank String optionName,
            @NotNull @Positive Double price,
            Boolean active
    ) {
    }

    public record ServiceOptionResponse(
            Integer optionId,
            Integer serviceId,
            String optionName,
            Double price,
            Boolean active
    ) {
    }

    public record ServicePackageRequest(
            @NotBlank String packageName,
            String description,
            @NotNull @Positive Integer totalSession,
            @NotNull @Positive Double price,
            @Positive Integer validityDays,
            String packageImage,
            Boolean active
    ) {
    }

    public record ServicePackageResponse(
            Integer packageId,
            Integer serviceId,
            String serviceName,
            String packageName,
            String description,
            Integer totalSession,
            Double price,
            Integer validityDays,
            String packageImage,
            Boolean active
    ) {
    }
}
