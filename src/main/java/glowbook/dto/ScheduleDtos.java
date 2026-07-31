package glowbook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

public final class ScheduleDtos {

    private ScheduleDtos() {
    }

    public record WorkingHourRequest(
            @NotNull DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            Boolean closed
    ) {
    }

    public record WorkingHourResponse(
            Integer workingHourId,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            Boolean closed
    ) {
    }

    public record HolidayRequest(
            @NotNull LocalDate holidayDate,
            @NotBlank String holidayName,
            String description
    ) {
    }

    public record HolidayResponse(
            Integer holidayId,
            LocalDate holidayDate,
            String holidayName,
            String description
    ) {
    }
}
