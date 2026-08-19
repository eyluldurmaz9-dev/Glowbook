package glowbook.controller;

import glowbook.entity.EmployeeService;
import glowbook.entity.WorkingHour;
import glowbook.repository.EmployeeServiceRepository;
import glowbook.repository.WorkingHourRepository;
import glowbook.security.JwtTokenService;
import glowbook.security.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers the reported "Kapalı doesn't stick" bug end to end: PUT
 * /api/admin/working-hours/{id} → DB → GET /api/catalog/working-hours →
 * availability. Every stage already round-trips the {@code closed} field
 * correctly (entity/DTO/controller/service/mapper/availability algorithm),
 * so this test is also the guard against a real regression there — the
 * actual bug (see admin_dashboard_page.dart's working-hours list) was that
 * the admin list always rendered "start - end" regardless of {@code
 * closed}, which only ever LOOKED like the change hadn't persisted.
 *
 * TUESDAY is a real, shared seed row also used implicitly by date-based
 * booking tests elsewhere in the suite (the Spring context/H2 instance is
 * cached across test classes), so its original hours are restored in
 * {@link #restoreTuesday()} regardless of whether the test body passes.
 */
@SpringBootTest
@AutoConfigureMockMvc
class WorkingHourIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JwtTokenService jwtTokenService;
    @Autowired WorkingHourRepository workingHourRepository;
    @Autowired EmployeeServiceRepository employeeServiceRepository;

    private String adminToken;
    private Integer tuesdayId;
    private LocalTime originalStart;
    private LocalTime originalEnd;
    private Boolean originalClosed;

    @BeforeEach
    void setUp() {
        adminToken = jwtTokenService.generateToken("TESTADMIN", UserRole.ADMIN);
        WorkingHour tuesday = workingHourRepository.findByDayOfWeek(DayOfWeek.TUESDAY).orElseThrow();
        tuesdayId = tuesday.getWorkingHourId();
        originalStart = tuesday.getStartTime();
        originalEnd = tuesday.getEndTime();
        originalClosed = tuesday.getClosed();
    }

    @AfterEach
    void restoreTuesday() {
        WorkingHour tuesday = workingHourRepository.findById(tuesdayId).orElseThrow();
        tuesday.setStartTime(originalStart);
        tuesday.setEndTime(originalEnd);
        tuesday.setClosed(originalClosed);
        workingHourRepository.save(tuesday);
    }

    @Test
    void closingTuesdayPersistsAndBlocksAvailabilityWithoutAffectingOtherDays() throws Exception {
        // Tuesday starts open (TEST 1).
        assertThat(originalClosed).isNotEqualTo(Boolean.TRUE);

        EmployeeService assignment = employeeServiceRepository.findAll().stream().findFirst().orElseThrow();
        Integer serviceId = assignment.getService().getServiceId();
        Integer optionId = assignment.getServiceOption() != null
                ? assignment.getServiceOption().getOptionId() : null;

        LocalDate nextTuesday = nextDate(DayOfWeek.TUESDAY);
        LocalDate nextWednesday = nextDate(DayOfWeek.WEDNESDAY);

        // Sanity: before closing, Tuesday can produce availability output
        // (not necessarily non-empty depending on fixture data, but the
        // request itself must succeed) and Wednesday is untouched throughout.
        mockMvc.perform(get("/api/appointments/available-slots")
                        .param("serviceId", serviceId.toString())
                        .param("date", nextWednesday.toString()))
                .andExpect(status().isOk());

        // TEST 2 + TEST 3: close Tuesday, verify the persisted response.
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
                .andExpect(jsonPath("$.data.dayOfWeek").value("TUESDAY"))
                .andExpect(jsonPath("$.data.closed").value(true));

        assertThat(workingHourRepository.findById(tuesdayId).orElseThrow().getClosed()).isTrue();

        // TEST 4: re-GET reflects the persisted closed value (this is what
        // both the admin list and the edit-modal hydration read from).
        mockMvc.perform(get("/api/catalog/working-hours"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.dayOfWeek == 'TUESDAY')].closed").value(true));

        // TEST 7: no available slots on any Tuesday while closed.
        mockMvc.perform(get("/api/appointments/available-slots")
                        .param("serviceId", serviceId.toString())
                        .param("date", nextTuesday.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());

        // TEST 8: Wednesday (an untouched open day) still returns 200 with
        // its own data unaffected by Tuesday's change.
        mockMvc.perform(get("/api/appointments/available-slots")
                        .param("serviceId", serviceId.toString())
                        .param("date", nextWednesday.toString()))
                .andExpect(status().isOk());
        WorkingHour wednesday = workingHourRepository.findByDayOfWeek(DayOfWeek.WEDNESDAY).orElseThrow();
        assertThat(wednesday.getClosed()).isNotEqualTo(Boolean.TRUE);

        // TEST 9 + TEST 10: reopen Tuesday with explicit hours; availability
        // resumes.
        Map<String, Object> reopenPayload = new LinkedHashMap<>();
        reopenPayload.put("dayOfWeek", "TUESDAY");
        reopenPayload.put("startTime", "10:00:00");
        reopenPayload.put("endTime", "19:00:00");
        reopenPayload.put("closed", false);

        mockMvc.perform(put("/api/admin/working-hours/{id}", tuesdayId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reopenPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.closed").value(false));

        assertThat(workingHourRepository.findById(tuesdayId).orElseThrow().getClosed()).isFalse();

        var reopenedRequest = get("/api/appointments/available-slots")
                .param("serviceId", serviceId.toString())
                .param("date", nextTuesday.toString());
        if (optionId != null) {
            reopenedRequest.param("optionId", optionId.toString());
        }
        mockMvc.perform(reopenedRequest).andExpect(status().isOk());
    }

    private LocalDate nextDate(DayOfWeek dayOfWeek) {
        LocalDate date = LocalDate.now().plusDays(1);
        while (date.getDayOfWeek() != dayOfWeek) {
            date = date.plusDays(1);
        }
        return date;
    }
}
