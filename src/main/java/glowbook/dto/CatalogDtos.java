package glowbook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public final class CatalogDtos {

    private CatalogDtos() {
    }

    public record ServiceRequest(
            @NotBlank(message = "Hizmet adı boş olamaz.") String serviceName,
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
            @NotBlank(message = "Alt hizmet adı boş olamaz.") String optionName,
            @NotNull(message = "Fiyat girilmelidir.") @Positive(message = "Fiyat sıfırdan büyük olmalıdır.") Double price,
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
            @NotBlank(message = "Paket adı boş olamaz.") String packageName,
            String description,
            @NotNull(message = "Seans sayısı girilmelidir.") @Positive(message = "Seans sayısı sıfırdan büyük olmalıdır.") Integer totalSession,
            @NotNull(message = "Fiyat girilmelidir.") @Positive(message = "Fiyat sıfırdan büyük olmalıdır.") Double price,
            @Positive(message = "Geçerlilik süresi sıfırdan büyük olmalıdır.") Integer validityDays,
            String packageImage,
            Boolean active,
            /** IDs of the {@code ServiceOption}s this package may be booked for. Every id
             * must belong to the package's own service. Required to be non-empty — a
             * package with no covered option could never be booked at all. */
            List<Integer> coveredOptionIds
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
            Boolean active,
            /** Authoritative: the only sub-services this package may actually be booked
             * for. Exactly one entry means the booking flow must never ask again which
             * sub-service to use. */
            List<ServiceOptionResponse> coveredOptions
    ) {
    }
}
