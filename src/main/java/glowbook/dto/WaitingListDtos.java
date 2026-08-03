package glowbook.dto;

import glowbook.entity.WaitingListStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public final class WaitingListDtos {

    private WaitingListDtos() {
    }

    public record WaitingListRequest(
            Integer customerId,
            @NotNull Integer serviceId,
            @NotNull Integer optionId,
            String customerName,
            String customerSurname,
            String phone,
            @NotNull LocalDate preferredDate,
            LocalTime preferredStartTime,
            LocalTime preferredEndTime
    ) {
    }

    public record WaitingListResponse(
            Integer waitingListId,
            Integer customerId,
            Integer serviceId,
            String serviceName,
            Integer optionId,
            String optionName,
            String customerName,
            String customerSurname,
            String phone,
            LocalDate preferredDate,
            LocalTime preferredStartTime,
            LocalTime preferredEndTime,
            WaitingListStatus status,
            LocalDateTime createdAt
    ) {
    }
}
