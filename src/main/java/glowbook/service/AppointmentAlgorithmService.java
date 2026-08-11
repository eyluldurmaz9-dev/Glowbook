package glowbook.service;

import glowbook.dto.AppointmentDtos;
import glowbook.entity.Appointment;
import glowbook.entity.AppointmentStatus;
import glowbook.entity.EmployeeService;
import glowbook.entity.WorkingHour;
import glowbook.exception.BusinessException;
import glowbook.exception.ConflictException;
import glowbook.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppointmentAlgorithmService {

    private static final int SLOT_MINUTES = 30;
    private static final Set<AppointmentStatus> BLOCKING_STATUSES = Set.of(
            AppointmentStatus.PENDING,
            AppointmentStatus.APPROVED
    );

    private final AppointmentRepository appointmentRepository;
    private final EmployeeServiceAssignmentService employeeServiceAssignmentService;
    private final WorkingHourService workingHourService;
    private final EmployeeLeaveService employeeLeaveService;
    private final HolidayService holidayService;

    public List<AppointmentDtos.AvailableSlotResponse> findAvailableSlots(Integer serviceId, LocalDate appointmentDate) {
        if (holidayService.isHoliday(appointmentDate)) {
            return List.of();
        }

        WorkingHour workingHour = workingHourService.getByDay(appointmentDate.getDayOfWeek());
        if (Boolean.TRUE.equals(workingHour.getClosed())) {
            return List.of();
        }

        return employeeServiceAssignmentService.getEmployeesByService(serviceId)
                .stream()
                .filter(employeeService -> !employeeLeaveService.isEmployeeOnLeave(
                        employeeService.getEmployee().getEmployeeId(),
                        appointmentDate
                ))
                .map(employeeService -> toAvailableSlotResponse(employeeService, appointmentDate, workingHour))
                .filter(response -> !response.availableTimes().isEmpty())
                .toList();
    }

    public void validateSlot(String employeeId, Integer serviceId, LocalDate appointmentDate, LocalTime appointmentTime) {
        boolean slotExists = findAvailableSlots(serviceId, appointmentDate)
                .stream()
                .filter(slot -> slot.employeeId().equals(employeeId))
                .flatMap(slot -> slot.availableTimes().stream())
                .anyMatch(time -> time.equals(appointmentTime));

        if (!slotExists) {
            throw new ConflictException("Selected slot is not available");
        }
    }

    private AppointmentDtos.AvailableSlotResponse toAvailableSlotResponse(
            EmployeeService employeeService,
            LocalDate appointmentDate,
            WorkingHour workingHour
    ) {
        String employeeId = employeeService.getEmployee().getEmployeeId();
        List<LocalTime> availableTimes = new ArrayList<>();
        LocalTime time = workingHour.getStartTime();

        while (time != null && workingHour.getEndTime() != null && time.plusMinutes(SLOT_MINUTES).compareTo(workingHour.getEndTime()) <= 0) {
            if (!isOccupied(employeeId, appointmentDate, time)) {
                availableTimes.add(time);
            }
            time = time.plusMinutes(SLOT_MINUTES);
        }

        return new AppointmentDtos.AvailableSlotResponse(
                employeeId,
                employeeService.getEmployee().getFirstName() + " " + employeeService.getEmployee().getLastName(),
                appointmentDate,
                availableTimes
        );
    }

    private boolean isOccupied(String employeeId, LocalDate appointmentDate, LocalTime appointmentTime) {
        return appointmentRepository.existsByEmployeeEmployeeIdAndAppointmentDateAndAppointmentTimeAndStatusIn(
                employeeId,
                appointmentDate,
                appointmentTime,
                BLOCKING_STATUSES
        );
    }
}
