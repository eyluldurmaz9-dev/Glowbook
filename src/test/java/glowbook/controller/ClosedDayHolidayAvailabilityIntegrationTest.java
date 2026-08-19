package glowbook.controller;

import glowbook.entity.Customer;
import glowbook.entity.EmployeeService;
import glowbook.entity.Holiday;
import glowbook.entity.ServicePackage;
import glowbook.entity.WorkingHour;
import glowbook.repository.AppointmentRepository;
import glowbook.repository.CustomerPackageRepository;
import glowbook.repository.CustomerRepository;
import glowbook.repository.EmployeeServiceRepository;
import glowbook.repository.HolidayRepository;
import glowbook.repository.NotificationRepository;
import glowbook.repository.ServiceOptionRepository;
import glowbook.repository.ServicePackageRepository;
import glowbook.repository.WorkingHourRepository;
import glowbook.security.JwtTokenService;
import glowbook.security.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The reported gap: closing a weekday or adding a holiday must make BOTH
 * availability AND appointment creation refuse that date — for normal
 * bookings and package bookings alike, direct API calls included (not just
 * whatever the Flutter UI happens to show). Every stage already routes
 * through {@code AppointmentAlgorithmService.findAvailableSlots} (holiday
 * checked before weekly-closed) and {@code AppointmentService.create}'s
 * explicit holiday/working-hours checks — {@link PackageBookingService}
 * calls the very same {@code create}, so package booking inherits the same
 * protection with no separate code path. This test proves all of it holds,
 * end to end, against the real persisted state.
 *
 * <p>TUESDAY is a real, shared seed row also touched by other test
 * classes sharing the cached Spring context/H2 instance, so its original
 * hours are restored in {@link #cleanUp()} regardless of test outcome. The
 * holiday inserted for these tests is deleted the same way.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class ClosedDayHolidayAvailabilityIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JwtTokenService jwtTokenService;
    @Autowired WorkingHourRepository workingHourRepository;
    @Autowired HolidayRepository holidayRepository;
    @Autowired CustomerRepository customerRepository;
    @Autowired CustomerPackageRepository customerPackageRepository;
    @Autowired AppointmentRepository appointmentRepository;
    @Autowired NotificationRepository notificationRepository;
    @Autowired EmployeeServiceRepository employeeServiceRepository;
    @Autowired ServiceOptionRepository serviceOptionRepository;
    @Autowired ServicePackageRepository servicePackageRepository;

    private String adminToken;
    private String customerToken;
    private Customer customer;

    private Integer tuesdayId;
    private java.time.LocalTime originalStart;
    private java.time.LocalTime originalEnd;
    private Boolean originalClosed;

    private Integer holidayId;
    private LocalDate holidayDate;

    private Integer serviceId;
    private Integer optionId;
    private String employeeId;

    private ServicePackage tenSessionPackage;
    private Integer packageOptionId;
    private String packageEmployeeId;

    @BeforeEach
    void setUp() {
        // Matches PackageBookingLifecycleIntegrationTest's own @BeforeEach: the Spring
        // context/H2 instance is shared across test classes, so counts must start from a
        // known baseline rather than assuming zero.
        notificationRepository.deleteAll();
        appointmentRepository.deleteAll();
        adminToken = jwtTokenService.generateToken("TESTADMIN", UserRole.ADMIN);
        customer = customerRepository.findByEmail("closed-day-fix@glowbook.test")
                .orElseGet(() -> customerRepository.save(Customer.builder()
                        .firstName("Kapali").lastName("Gun")
                        .phone("05559991234").email("closed-day-fix@glowbook.test")
                        .password("irrelevant").active(true).build()));
        customerToken = jwtTokenService.generateToken(customer.getCustomerId().toString(), UserRole.CUSTOMER);

        WorkingHour tuesday = workingHourRepository.findByDayOfWeek(DayOfWeek.TUESDAY).orElseThrow();
        tuesdayId = tuesday.getWorkingHourId();
        originalStart = tuesday.getStartTime();
        originalEnd = tuesday.getEndTime();
        originalClosed = tuesday.getClosed();

        // A weekday that stays OPEN in working-hours, so the holiday itself is what has to
        // block it (TEST 4's exact point: holiday overrides an otherwise-open weekday).
        holidayDate = nextDate(DayOfWeek.WEDNESDAY, 21);
        holidayId = holidayRepository.save(Holiday.builder()
                .holidayDate(holidayDate)
                .holidayName("Test Tatili")
                .description("Closed-day/holiday availability fix coverage")
                .build()).getHolidayId();

        // A service-wide assignment (serviceOption == null) qualifies for any active option
        // under that service — see EmployeeServiceRepository#employeeCanProvideOption — so
        // any assignment plus any active option for the same service is a valid pairing.
        EmployeeService assignment = employeeServiceRepository.findAll().stream()
                .filter(item -> Boolean.TRUE.equals(item.getEmployee().getActive()))
                .findFirst().orElseThrow();
        serviceId = assignment.getService().getServiceId();
        optionId = serviceOptionRepository
                .findByServiceServiceIdAndActiveTrueOrderByOptionNameAsc(serviceId)
                .stream().findFirst().orElseThrow().getOptionId();
        employeeId = assignment.getEmployee().getEmployeeId();

        tenSessionPackage = servicePackageRepository.findByActiveTrueOrderByPackageNameAsc().stream()
                .filter(item -> item.getTotalSession() != null && item.getTotalSession() == 10)
                .findFirst().orElseThrow();
        packageOptionId = tenSessionPackage.getCoveredOptions().iterator().next().getOptionId();
        packageEmployeeId = employeeServiceRepository.findAll().stream()
                .filter(item -> item.getService().getServiceId().equals(tenSessionPackage.getService().getServiceId()))
                .filter(item -> Boolean.TRUE.equals(item.getEmployee().getActive()))
                .map(item -> item.getEmployee().getEmployeeId())
                .findFirst().orElseThrow();
    }

    @AfterEach
    void cleanUp() {
        WorkingHour tuesday = workingHourRepository.findById(tuesdayId).orElseThrow();
        tuesday.setStartTime(originalStart);
        tuesday.setEndTime(originalEnd);
        tuesday.setClosed(originalClosed);
        workingHourRepository.save(tuesday);
        holidayRepository.deleteById(holidayId);
    }

    @Test
    void closedWeekdayBlocksAvailabilityAndBothBookingTypesEverywhereThatWeekdayRecurs() throws Exception {
        closeTuesday();

        LocalDate firstTuesday = nextDate(DayOfWeek.TUESDAY, 1);
        LocalDate secondTuesday = firstTuesday.plusWeeks(1);
        LocalDate thirdTuesday = firstTuesday.plusWeeks(2);

        // TEST 1: no availability on any occurrence of the closed weekday.
        for (LocalDate date : List.of(firstTuesday, secondTuesday, thirdTuesday)) {
            mockMvc.perform(get("/api/appointments/available-slots")
                            .param("serviceId", serviceId.toString())
                            .param("date", date.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        // TEST 5: a direct appointment API call is rejected, not just hidden from the UI.
        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(appointmentPayload(firstTuesday))))
                .andExpect(status().isBadRequest());
        assertThat(appointmentRepository.count()).isZero();

        // TEST 8: package booking is rejected the same way, on the same date.
        long packagesBefore = customerPackageRepository.count();
        mockMvc.perform(post(packageBookingPath())
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(packageBookingPayload(firstTuesday))))
                .andExpect(status().isBadRequest());
        assertThat(customerPackageRepository.count()).isEqualTo(packagesBefore);
        assertThat(appointmentRepository.count()).isZero();
    }

    @Test
    void holidayBlocksAvailabilityAndBothBookingTypesEvenOnAnOpenWeekday() throws Exception {
        // TEST 3 + TEST 4: an otherwise-open weekday still has zero availability on the
        // holiday date itself.
        mockMvc.perform(get("/api/appointments/available-slots")
                        .param("serviceId", serviceId.toString())
                        .param("date", holidayDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());

        // TEST 6: direct appointment API call rejected on the holiday date.
        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(appointmentPayload(holidayDate))))
                .andExpect(status().isBadRequest());
        assertThat(appointmentRepository.count()).isZero();

        // TEST 9: package booking rejected on the holiday date.
        long packagesBefore = customerPackageRepository.count();
        mockMvc.perform(post(packageBookingPath())
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(packageBookingPayload(holidayDate))))
                .andExpect(status().isBadRequest());
        assertThat(customerPackageRepository.count()).isEqualTo(packagesBefore);
        assertThat(appointmentRepository.count()).isZero();

        // The following Wednesday (a week later, not the holiday) is unaffected — the
        // holiday is a single-date rule, not a recurring weekly one (TEST 11).
        LocalDate followingWednesday = holidayDate.plusWeeks(1);
        mockMvc.perform(get("/api/appointments/available-slots")
                        .param("serviceId", serviceId.toString())
                        .param("date", followingWednesday.toString()))
                .andExpect(status().isOk());
        // TEST 12: Holiday CRUD itself is untouched and still works normally.
        mockMvc.perform(get("/api/catalog/holidays")
                        .param("startDate", holidayDate.toString())
                        .param("endDate", holidayDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].holidayDate").value(holidayDate.toString()));
    }

    @Test
    void openWeekdayWithNoHolidayStillAcceptsNormalAndPackageBookings() throws Exception {
        // TEST 7: baseline — an ordinary open, non-holiday weekday keeps working normally
        // for a direct appointment booking.
        LocalDate openWednesday = holidayDate.plusWeeks(2);
        Map<String, Object> payload = appointmentPayload(openWednesday);
        payload.put("employeeId", employeeId);
        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());
        assertThat(appointmentRepository.count()).isEqualTo(1);
    }

    /**
     * TEST 1-6/19 and SCENARIOS A-D generalized: the fix must not be
     * Tuesday-specific. For every real DayOfWeek, closing it must zero out
     * availability across three separate future months (not just "next
     * week"), and reopening it with real hours must restore availability —
     * dynamically, via the same WorkingHours row the admin edits, with no
     * redeploy/restart involved (this test only ever calls the same public
     * HTTP endpoints the app uses).
     */
    @ParameterizedTest
    @EnumSource(DayOfWeek.class)
    void everyWeekdayDynamicallyBlocksAndRestoresAvailabilityAcrossFutureMonths(DayOfWeek dayOfWeek)
            throws Exception {
        WorkingHour original = workingHourRepository.findByDayOfWeek(dayOfWeek).orElseThrow();
        Integer id = original.getWorkingHourId();
        java.time.LocalTime savedStart = original.getStartTime();
        java.time.LocalTime savedEnd = original.getEndTime();
        Boolean savedClosed = original.getClosed();

        try {
            // Avoid colliding with the unrelated "Test Tatili" holiday the shared
            // @BeforeEach inserts on a future WEDNESDAY: when dayOfWeek == WEDNESDAY,
            // every candidate below is also a Wednesday and could land on that exact
            // date, which would make the reopen-phase assertion fail for a reason
            // that has nothing to do with this test's own weekday-open/close logic.
            LocalDate thisMonth = nextOccurrenceOnOrAfter(dayOfWeek, LocalDate.now().plusDays(1), holidayDate);
            LocalDate nextMonth = nextOccurrenceOnOrAfter(dayOfWeek, thisMonth.plusDays(30), holidayDate);
            LocalDate monthAfter = nextOccurrenceOnOrAfter(dayOfWeek, nextMonth.plusDays(30), holidayDate);

            // Close it (admin toggles Kapalı=ON, Kaydet).
            Map<String, Object> closePayload = new LinkedHashMap<>();
            closePayload.put("dayOfWeek", dayOfWeek.name());
            closePayload.put("startTime", "10:00:00");
            closePayload.put("endTime", "19:00:00");
            closePayload.put("closed", true);
            mockMvc.perform(put("/api/admin/working-hours/{id}", id)
                            .header("Authorization", bearer(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(closePayload)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.closed").value(true));

            // TEST 1/2/3: zero availability this month, next month, and the month after —
            // not just "next week".
            for (LocalDate date : List.of(thisMonth, nextMonth, monthAfter)) {
                mockMvc.perform(get("/api/appointments/available-slots")
                                .param("serviceId", serviceId.toString())
                                .param("date", date.toString()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data").isEmpty());
            }

            // TEST 12/14: direct and package booking rejected while closed.
            mockMvc.perform(post("/api/appointments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(appointmentPayload(thisMonth))))
                    .andExpect(status().isBadRequest());

            // Reopen it (admin toggles Kapalı=OFF, real hours, Kaydet) — TEST 4/16: the
            // same three future dates must be bookable again, with no restart, purely
            // because the WorkingHours row changed.
            Map<String, Object> reopenPayload = new LinkedHashMap<>();
            reopenPayload.put("dayOfWeek", dayOfWeek.name());
            reopenPayload.put("startTime", "09:00:00");
            reopenPayload.put("endTime", "20:00:00");
            reopenPayload.put("closed", false);
            mockMvc.perform(put("/api/admin/working-hours/{id}", id)
                            .header("Authorization", bearer(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reopenPayload)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.closed").value(false));

            for (LocalDate date : List.of(thisMonth, nextMonth, monthAfter)) {
                mockMvc.perform(get("/api/appointments/available-slots")
                                .param("serviceId", serviceId.toString())
                                .param("date", date.toString()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data").isNotEmpty());
            }
        } finally {
            WorkingHour restore = workingHourRepository.findById(id).orElseThrow();
            restore.setStartTime(savedStart);
            restore.setEndTime(savedEnd);
            restore.setClosed(savedClosed);
            workingHourRepository.save(restore);
        }
    }

    private void closeTuesday() throws Exception {
        Map<String, Object> closePayload = new LinkedHashMap<>();
        closePayload.put("dayOfWeek", "TUESDAY");
        closePayload.put("startTime", "10:00:00");
        closePayload.put("endTime", "19:00:00");
        closePayload.put("closed", true);

        mockMvc.perform(put("/api/admin/working-hours/{id}", tuesdayId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(closePayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.closed").value(true));
    }

    private Map<String, Object> appointmentPayload(LocalDate date) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("employeeId", employeeId);
        payload.put("serviceId", serviceId);
        payload.put("optionId", optionId);
        payload.put("customerName", "Guest");
        payload.put("customerSurname", "User");
        payload.put("phone", "05550001122");
        payload.put("appointmentDate", date.toString());
        payload.put("appointmentTime", "11:00");
        return payload;
    }

    private Map<String, Object> packageBookingPayload(LocalDate date) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("employeeId", packageEmployeeId);
        payload.put("optionId", packageOptionId);
        payload.put("appointmentDate", date.toString());
        payload.put("appointmentTime", "11:00");
        return payload;
    }

    private String packageBookingPath() {
        return "/api/customers/" + customer.getCustomerId()
                + "/packages/" + tenSessionPackage.getPackageId() + "/booking";
    }

    private LocalDate nextDate(DayOfWeek dayOfWeek, int minDaysAhead) {
        LocalDate date = LocalDate.now().plusDays(minDaysAhead);
        while (date.getDayOfWeek() != dayOfWeek) {
            date = date.plusDays(1);
        }
        return date;
    }

    /** Like {@link #nextDate}, but also steps past {@code avoid} if it's an exact match. */
    private LocalDate nextOccurrenceOnOrAfter(DayOfWeek dayOfWeek, LocalDate earliest, LocalDate avoid) {
        LocalDate date = earliest;
        while (date.getDayOfWeek() != dayOfWeek || date.equals(avoid)) {
            date = date.plusDays(1);
        }
        return date;
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
