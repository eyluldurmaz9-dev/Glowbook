package glowbook.service;

import glowbook.entity.Employee;
import glowbook.entity.EmployeeService;
import glowbook.entity.WorkingHour;
import glowbook.exception.BusinessException;
import glowbook.repository.AppointmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AppointmentTimeRulesTest {

    private static final ZoneId ISTANBUL = ZoneId.of("Europe/Istanbul");
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-08-13T10:25:00Z"), ISTANBUL);

    private AppointmentAlgorithmService algorithm;
    private EmployeeServiceAssignmentService assignments;

    @BeforeEach
    void setUp() {
        AppointmentRepository appointments = mock(AppointmentRepository.class);
        assignments = mock(EmployeeServiceAssignmentService.class);
        WorkingHourService workingHours = mock(WorkingHourService.class);
        EmployeeLeaveService leaves = mock(EmployeeLeaveService.class);
        HolidayService holidays = mock(HolidayService.class);

        when(holidays.isHoliday(any())).thenReturn(false);
        when(leaves.isEmployeeOnLeave(any(), any())).thenReturn(false);
        when(appointments.existsByEmployeeEmployeeIdAndAppointmentDateAndAppointmentTimeAndStatusIn(
                any(), any(), any(), any())).thenReturn(false);
        when(workingHours.getByDay(any(DayOfWeek.class))).thenReturn(WorkingHour.builder()
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(19, 0))
                .closed(false)
                .build());

        algorithm = new AppointmentAlgorithmService(
                appointments, assignments, workingHours, leaves, holidays, FIXED_CLOCK);
    }

    @Test
    void rejectsYesterdayDeterministicallyInIstanbul() {
        assertThatThrownBy(() -> algorithm.validateAppointmentTime(
                LocalDate.of(2026, 8, 12), LocalTime.of(14, 0)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Geçmiş bir tarih için randevu oluşturamazsın.");
    }

    @Test
    void rejectsPastHourToday() {
        assertThatThrownBy(() -> algorithm.validateAppointmentTime(
                LocalDate.of(2026, 8, 13), LocalTime.of(13, 0)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Bu saat artık geçmişte kaldı. Lütfen başka bir saat seç.");
    }

    @Test
    void acceptsNextFutureFullHourToday() {
        algorithm.validateAppointmentTime(
                LocalDate.of(2026, 8, 13), LocalTime.of(14, 0));
    }

    @Test
    void rejectsHalfHour() {
        assertThatThrownBy(() -> algorithm.validateAppointmentTime(
                LocalDate.of(2026, 8, 14), LocalTime.of(14, 30)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("tam saat");
    }

    @Test
    void sameDaySlotsExcludePastAndCurrentHourAndPreserveEmployeeName() {
        when(assignments.getEmployeesByServiceOption(1, 11)).thenReturn(List.of(
                EmployeeService.builder()
                        .employee(Employee.builder().employeeId("EMP-1")
                                .firstName("Eylem").lastName("Ceylan").active(true).build())
                        .build()));

        var slots = algorithm.findAvailableSlots(
                1, 11, LocalDate.of(2026, 8, 13));

        assertThat(slots).hasSize(1);
        assertThat(slots.getFirst().employeeName()).isEqualTo("Eylem Ceylan");
        assertThat(slots.getFirst().availableTimes())
                .contains(LocalTime.of(14, 0), LocalTime.of(15, 0))
                .doesNotContain(LocalTime.of(13, 0), LocalTime.of(12, 0));
        assertThat(slots.getFirst().availableTimes())
                .allMatch(time -> time.getMinute() == 0 && time.getSecond() == 0);
    }

    @Test
    void futureDayKeepsMorningSlots() {
        when(assignments.getEmployeesByServiceOption(1, 11)).thenReturn(List.of(
                EmployeeService.builder()
                        .employee(Employee.builder().employeeId("EMP-2")
                                .firstName("Elifnaz").lastName("Yaraşır").active(true).build())
                        .build()));

        var slots = algorithm.findAvailableSlots(
                1, 11, LocalDate.of(2026, 8, 14));

        assertThat(slots.getFirst().availableTimes()).contains(LocalTime.of(9, 0));
    }

    @Test
    void pastDateHasNoAvailability() {
        assertThat(algorithm.findAvailableSlots(
                1, 11, LocalDate.of(2026, 8, 12))).isEmpty();
    }
}
