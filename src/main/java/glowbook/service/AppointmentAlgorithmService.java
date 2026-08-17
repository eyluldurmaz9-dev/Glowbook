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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppointmentAlgorithmService {

    private static final int SLOT_MINUTES = 60;
    private static final Set<AppointmentStatus> BLOCKING_STATUSES = Set.of(
            AppointmentStatus.PENDING,
            AppointmentStatus.APPROVED
    );

    private final AppointmentRepository appointmentRepository;
    private final EmployeeServiceAssignmentService employeeServiceAssignmentService;
    private final WorkingHourService workingHourService;
    private final EmployeeLeaveService employeeLeaveService;
    private final HolidayService holidayService;
    private final Clock businessClock;

    public List<AppointmentDtos.AvailableSlotResponse> findAvailableSlots(Integer serviceId, Integer optionId, LocalDate appointmentDate) {
        LocalDate today = LocalDate.now(businessClock);
        if (appointmentDate == null || appointmentDate.isBefore(today)) {
            return List.of();
        }
        if (holidayService.isHoliday(appointmentDate)) {
            return List.of();
        }

        WorkingHour workingHour = workingHourService.getByDay(appointmentDate.getDayOfWeek());
        if (Boolean.TRUE.equals(workingHour.getClosed())) {
            return List.of();
        }

        List<EmployeeService> qualifiedEmployees = optionId == null
                ? employeeServiceAssignmentService.getEmployeesByService(serviceId)
                : employeeServiceAssignmentService.getEmployeesByServiceOption(serviceId, optionId);
        return qualifiedEmployees
                .stream()
                .filter(employeeService -> !employeeLeaveService.isEmployeeOnLeave(
                        employeeService.getEmployee().getEmployeeId(),
                        appointmentDate
                ))
                .map(employeeService -> toAvailableSlotResponse(employeeService, appointmentDate, workingHour))
                .filter(response -> !response.availableTimes().isEmpty())
                .toList();
    }

    public void validateSlot(String employeeId, Integer serviceId, Integer optionId, LocalDate appointmentDate, LocalTime appointmentTime) {
        validateAppointmentTime(appointmentDate, appointmentTime);
        boolean slotExists = findAvailableSlots(serviceId, optionId, appointmentDate)
                .stream()
                .filter(slot -> slot.employeeId().equals(employeeId))
                .flatMap(slot -> slot.availableTimes().stream())
                .anyMatch(time -> time.equals(appointmentTime));

        if (!slotExists) {
            throw new ConflictException("Bu saat başka bir randevu tarafından dolu. Lütfen başka bir saat seç.");
        }
    }

    public void requireFullHour(LocalTime appointmentTime) {
        if (appointmentTime == null || appointmentTime.getMinute() != 0 || appointmentTime.getSecond() != 0) {
            throw new BusinessException("Randevu saati tam saat olmalıdır (örnek: 10:00).");
        }
    }

    private AppointmentDtos.AvailableSlotResponse toAvailableSlotResponse(
            EmployeeService employeeService,
            LocalDate appointmentDate,
            WorkingHour workingHour
    ) {
        String employeeId = employeeService.getEmployee().getEmployeeId();
        List<LocalTime> availableTimes = new ArrayList<>();
        LocalTime time = nextFullHour(workingHour.getStartTime());

        while (time != null && workingHour.getEndTime() != null && time.plusMinutes(SLOT_MINUTES).compareTo(workingHour.getEndTime()) <= 0) {
            if (isFutureSlot(appointmentDate, time) && !isOccupied(employeeId, appointmentDate, time)) {
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

    public void validateAppointmentTime(LocalDate appointmentDate, LocalTime appointmentTime) {
        requireFullHour(appointmentTime);
        if (appointmentDate == null) {
            throw new BusinessException("Randevu tarihi zorunludur.");
        }
        LocalDate today = LocalDate.now(businessClock);
        if (appointmentDate.isBefore(today)) {
            throw new BusinessException("Geçmiş bir tarih için randevu oluşturamazsın.");
        }
        if (appointmentDate.equals(today) && !isFutureSlot(appointmentDate, appointmentTime)) {
            throw new BusinessException("Bu saat artık geçmişte kaldı. Lütfen başka bir saat seç.");
        }
    }

    private boolean isFutureSlot(LocalDate appointmentDate, LocalTime appointmentTime) {
        return LocalDateTime.of(appointmentDate, appointmentTime)
                .isAfter(LocalDateTime.now(businessClock));
    }

    private LocalTime nextFullHour(LocalTime time) {
        if (time == null || (time.getMinute() == 0 && time.getSecond() == 0)) {
            return time;
        }
        return time.plusHours(1).withMinute(0).withSecond(0).withNano(0);
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
