package glowbook.controller;

import glowbook.dto.ApiResponse;
import glowbook.dto.DtoMapper;
import glowbook.dto.ScheduleDtos;
import glowbook.entity.Holiday;
import glowbook.entity.WorkingHour;
import glowbook.service.HolidayService;
import glowbook.service.WorkingHourService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ScheduleController {

    private final WorkingHourService workingHourService;
    private final HolidayService holidayService;

    @GetMapping("/catalog/working-hours")
    public ApiResponse<List<ScheduleDtos.WorkingHourResponse>> getWorkingHours() {
        return ApiResponse.success("Working hours listed", workingHourService.getAllWorkingHours()
                .stream()
                .map(DtoMapper::toWorkingHourResponse)
                .toList());
    }

    @PostMapping("/admin/working-hours")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public ApiResponse<ScheduleDtos.WorkingHourResponse> createWorkingHour(@Valid @RequestBody ScheduleDtos.WorkingHourRequest request) {
        return ApiResponse.success("Working hour created", DtoMapper.toWorkingHourResponse(workingHourService.create(toWorkingHour(request))));
    }

    @PutMapping("/admin/working-hours/{workingHourId}")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public ApiResponse<ScheduleDtos.WorkingHourResponse> updateWorkingHour(
            @PathVariable Integer workingHourId,
            @Valid @RequestBody ScheduleDtos.WorkingHourRequest request
    ) {
        return ApiResponse.success("Working hour updated", DtoMapper.toWorkingHourResponse(workingHourService.update(workingHourId, toWorkingHour(request))));
    }

    @GetMapping("/catalog/holidays")
    public ApiResponse<List<ScheduleDtos.HolidayResponse>> getHolidays(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate
    ) {
        return ApiResponse.success("Holidays listed", holidayService.getHolidays(startDate, endDate)
                .stream()
                .map(DtoMapper::toHolidayResponse)
                .toList());
    }

    @PostMapping("/admin/holidays")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public ApiResponse<ScheduleDtos.HolidayResponse> createHoliday(@Valid @RequestBody ScheduleDtos.HolidayRequest request) {
        Holiday holiday = Holiday.builder()
                .holidayDate(request.holidayDate())
                .holidayName(request.holidayName())
                .description(request.description())
                .build();
        return ApiResponse.success("Holiday created", DtoMapper.toHolidayResponse(holidayService.create(holiday)));
    }

    private WorkingHour toWorkingHour(ScheduleDtos.WorkingHourRequest request) {
        return WorkingHour.builder()
                .dayOfWeek(request.dayOfWeek())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .closed(request.closed() != null && request.closed())
                .build();
    }
}
